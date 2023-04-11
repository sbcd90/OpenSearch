/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.correlation.events.transport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.OpenSearchStatusException;
import org.opensearch.action.ActionListener;
import org.opensearch.action.search.MultiSearchRequest;
import org.opensearch.action.search.MultiSearchResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.support.ActionFilters;
import org.opensearch.action.support.HandledTransportAction;
import org.opensearch.client.Client;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.inject.Inject;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.LoggingDeprecationHandler;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.NestedQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.plugin.correlation.events.action.IndexCorrelationAction;
import org.opensearch.plugin.correlation.events.action.IndexCorrelationRequest;
import org.opensearch.plugin.correlation.events.action.IndexCorrelationResponse;
import org.opensearch.plugin.correlation.rules.model.CorrelationQuery;
import org.opensearch.plugin.correlation.rules.model.CorrelationRule;
import org.opensearch.plugin.correlation.settings.EventsCorrelationSettings;
import org.opensearch.rest.RestStatus;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.tasks.Task;
import org.opensearch.transport.TransportService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TransportIndexCorrelationAction extends HandledTransportAction<IndexCorrelationRequest, IndexCorrelationResponse> {

    private static final Logger log = LogManager.getLogger(TransportIndexCorrelationAction.class);

    private final Client client;

    private final NamedXContentRegistry xContentRegistry;

    private final Settings settings;

    private final ClusterService clusterService;

    private volatile long correlationTimeWindow;

    @Inject
    public TransportIndexCorrelationAction(
        TransportService transportService,
        Client client,
        NamedXContentRegistry xContentRegistry,
        Settings settings,
        ActionFilters actionFilters,
        ClusterService clusterService) {
        super(IndexCorrelationAction.NAME, transportService, actionFilters, IndexCorrelationRequest::new);
        this.client = client;
        this.xContentRegistry = xContentRegistry;
        this.settings = settings;
        this.clusterService = clusterService;
        this.correlationTimeWindow = EventsCorrelationSettings.CORRELATION_TIME_WINDOW.get(this.settings).getMillis();

        this.clusterService.getClusterSettings().addSettingsUpdateConsumer(EventsCorrelationSettings.CORRELATION_TIME_WINDOW, it -> correlationTimeWindow = it.getMillis());
    }

    @Override
    protected void doExecute(Task task, IndexCorrelationRequest request, ActionListener<IndexCorrelationResponse> listener) {
        AsyncIndexCorrelationAction asyncAction = new AsyncIndexCorrelationAction(request, listener);
        asyncAction.start();
    }

    class AsyncIndexCorrelationAction {
        private final IndexCorrelationRequest request;

        private final ActionListener<IndexCorrelationResponse> listener;

        AsyncIndexCorrelationAction(IndexCorrelationRequest request, ActionListener<IndexCorrelationResponse> listener) {
            this.request = request;
            this.listener = listener;
        }

        void start() {
            String inputIndex = request.getIndex();
            String event = request.getEvent();

            NestedQueryBuilder queryBuilder = QueryBuilders.nestedQuery(
                "correlate",
                QueryBuilders.matchQuery("correlate.index", inputIndex),
                ScoreMode.None
            );
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(queryBuilder);
            searchSourceBuilder.fetchSource(true);

            SearchRequest searchRequest = new SearchRequest();
            searchRequest.indices(CorrelationRule.CORRELATION_RULE_INDEX);
            searchRequest.source(searchSourceBuilder);

            client.search(searchRequest, new ActionListener<>() {
                @Override
                public void onResponse(SearchResponse response) {
                    if (response.isTimedOut()) {
                        onFailures(new OpenSearchStatusException(response.toString(), RestStatus.REQUEST_TIMEOUT));
                    }

                    Iterator<SearchHit> hits = response.getHits().iterator();
                    List<CorrelationRule> correlationRules = new ArrayList<>();
                    while (hits.hasNext()) {
                        try {
                            SearchHit hit = hits.next();

                            XContentParser xcp = XContentType.JSON.xContent().createParser(
                                xContentRegistry,
                                LoggingDeprecationHandler.INSTANCE, hit.getSourceAsString()
                            );

                            CorrelationRule rule = CorrelationRule.parse(xcp, hit.getId(), hit.getVersion());
                            correlationRules.add(rule);
                        } catch (IOException e) {
                            onFailures(e);
                        }
                    }

                    prepRulesForCorrelatedEventsGeneration(inputIndex, event, correlationRules);
                }

                @Override
                public void onFailure(Exception e) {
                    onFailures(e);
                }
            });
        }

        private void prepRulesForCorrelatedEventsGeneration(String index, String event, List<CorrelationRule> correlationRules) {
            MultiSearchRequest mSearchRequest = new MultiSearchRequest();

            for (CorrelationRule rule: correlationRules) {
                // assuming no index duplication in a rule.
                Optional<CorrelationQuery> query = rule.getCorrelationQueries().stream()
                    .filter(correlationQuery -> correlationQuery.getIndex().equals(index)).findFirst();

                if (query.isPresent()) {
                    BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("_id", event))
                        .must(QueryBuilders.queryStringQuery(query.get().getQuery()));

                    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                    searchSourceBuilder.query(queryBuilder);
                    searchSourceBuilder.fetchSource(false);

                    // assuming all queries belonging to an index use the same timestamp field.
                    searchSourceBuilder.fetchField(query.get().getTimestampField());

                    SearchRequest searchRequest = new SearchRequest();
                    searchRequest.indices(index);
                    searchRequest.source(searchSourceBuilder);
                    mSearchRequest.add(searchRequest);
                }
            }

            if (!mSearchRequest.requests().isEmpty()) {

                client.multiSearch(mSearchRequest, new ActionListener<>() {
                    @Override
                    public void onResponse(MultiSearchResponse items) {
                        MultiSearchResponse.Item[] responses = items.getResponses();
                        Map<String, List<CorrelationQuery>> indexQueriesMap = new HashMap<>();
                        Long timestamp = null;

                        int idx = 0;
                        for (MultiSearchResponse.Item response: responses) {
                            if (response.isFailure()) {
                                // suppress exception
                                continue;
                            }

                            SearchHits searchHits = response.getResponse().getHits();
                            if (searchHits.getTotalHits().value == 1) {
                                for (CorrelationQuery query: correlationRules.get(idx).getCorrelationQueries()) {
                                    List<CorrelationQuery> queries;
                                    if (indexQueriesMap.containsKey(query.getIndex())) {
                                        queries = indexQueriesMap.get(query.getIndex());
                                    } else {
                                        queries = new ArrayList<>();
                                    }
                                    queries.add(query);
                                    indexQueriesMap.put(query.getIndex(), queries);

                                    if (query.getIndex().equals(index)) {
                                        // assuming all queries belonging to an index use the same timestamp field.
                                        timestamp = searchHits.getAt(0).getFields().get(query.getTimestampField()).<Long>getValue();
                                    }
                                }
                            }
                            ++idx;
                        }
                        generateCorrelatedEvents(index, event, timestamp, indexQueriesMap);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        onFailures(e);
                    }
                });
            } else {
                // orphan event
                onOperation(true, new HashMap<>());
            }
        }

        private void generateCorrelatedEvents(String inputIndex, String event, Long timestamp, Map<String, List<CorrelationQuery>> indexQueriesMap) {
            MultiSearchRequest mSearchRequest = new MultiSearchRequest();

            for (Map.Entry<String, List<CorrelationQuery>> indexQueriesEntry: indexQueriesMap.entrySet()) {
                String index = indexQueriesEntry.getKey();
                List<CorrelationQuery> correlationQueries = indexQueriesEntry.getValue();

                // assuming all queries belonging to an index use the same timestamp field.
                String timestampField = correlationQueries.get(0).getTimestampField();

                BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.rangeQuery(timestampField)
                        .gte(timestamp - correlationTimeWindow)
                        .lte(timestamp + correlationTimeWindow));

                if (index.equals(inputIndex)) {
                    queryBuilder = queryBuilder.mustNot(QueryBuilders.matchQuery("_id", event));
                }

                for (CorrelationQuery query: correlationQueries) {
                    queryBuilder = queryBuilder.should(QueryBuilders.queryStringQuery(query.getQuery()));
                }

                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                searchSourceBuilder.query(queryBuilder);
                searchSourceBuilder.fetchSource(false);

                SearchRequest searchRequest = new SearchRequest();
                searchRequest.indices(index);
                searchRequest.source(searchSourceBuilder);

                mSearchRequest.add(searchRequest);
            }

            if (!mSearchRequest.requests().isEmpty()) {
                client.multiSearch(mSearchRequest, new ActionListener<>() {
                    @Override
                    public void onResponse(MultiSearchResponse items) {
                        MultiSearchResponse.Item[] responses = items.getResponses();
                        Map<String, Set<String>> eventsAdjacencyList = new HashMap<>();

                        for (MultiSearchResponse.Item response: responses) {
                            if (response.isFailure()) {
                                // suppress exception
                                continue;
                            }

                            Iterator<SearchHit> searchHits = response.getResponse().getHits().iterator();

                            while (searchHits.hasNext()) {
                                SearchHit hit = searchHits.next();

                                String index = hit.getIndex();
                                String id = hit.getId();

                                Set<String> neighborEvents;
                                if (eventsAdjacencyList.containsKey(index)) {
                                    neighborEvents = eventsAdjacencyList.get(index);
                                } else {
                                    neighborEvents = new HashSet<>();
                                }
                                neighborEvents.add(id);
                                eventsAdjacencyList.put(index, neighborEvents);
                            }
                        }

                        Map<String, List<String>> neighborEvents = new HashMap<>();
                        for (Map.Entry<String, Set<String>> neighborEvent: eventsAdjacencyList.entrySet()) {
                            neighborEvents.put(neighborEvent.getKey(), new ArrayList<>(neighborEvent.getValue()));
                        }
                        onOperation(false, neighborEvents);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        onFailures(e);
                    }
                });
            } else {
                // orphan event
                onOperation(true, new HashMap<>());
            }
        }

        private void onOperation(Boolean isOrphan, Map<String, List<String>> neighborEvents) {
            finishHim(isOrphan, neighborEvents, null);
        }

        private void onFailures(Exception t) {
            finishHim(null, null, t);
        }

        private void finishHim(Boolean isOrphan, Map<String, List<String>> neighborEvents, Exception t) {
            if (t != null) {
                listener.onFailure(t);
            } else {
                listener.onResponse(new IndexCorrelationResponse(isOrphan, neighborEvents, RestStatus.OK));
            }
        }
    }
}
