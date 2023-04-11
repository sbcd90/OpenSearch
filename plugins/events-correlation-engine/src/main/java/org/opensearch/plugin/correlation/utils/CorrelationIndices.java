/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.correlation.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.ActionListener;
import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.client.Client;
import org.opensearch.cluster.ClusterState;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.core.xcontent.ToXContent;
import org.opensearch.plugin.correlation.events.model.Correlation;
import org.opensearch.plugin.correlation.settings.EventsCorrelationSettings;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

import static org.opensearch.plugin.correlation.rules.model.CorrelationRule.NO_ID;
import static org.opensearch.plugin.correlation.rules.model.CorrelationRule.NO_VERSION;

public class CorrelationIndices {

    private static final Logger log = LogManager.getLogger(CorrelationIndices.class);
    public static final long FIXED_HISTORICAL_INTERVAL = 24L * 60L * 60L * 20L * 1000L;

    private final Client client;

    private final ClusterService clusterService;

    private final Settings settings;

    private volatile int noOfShards;

    public CorrelationIndices(Client client, ClusterService clusterService, Settings settings) {
        this.client = client;
        this.clusterService = clusterService;
        this.settings = settings;
        this.noOfShards = EventsCorrelationSettings.CORRELATION_HISTORY_INDEX_SHARDS.get(this.settings);
        this.clusterService.getClusterSettings().addSettingsUpdateConsumer(EventsCorrelationSettings.CORRELATION_HISTORY_INDEX_SHARDS, it -> noOfShards = it);
    }

    public static String correlationMappings() throws IOException {
        return new String(Objects.requireNonNull(CorrelationIndices.class.getClassLoader().getResourceAsStream("mappings/correlation.json")).readAllBytes(), Charset.defaultCharset());
    }

    public void initCorrelationIndex(ActionListener<CreateIndexResponse> actionListener) throws IOException {
        if (!correlationIndexExists()) {
            CreateIndexRequest indexRequest = new CreateIndexRequest(Correlation.CORRELATION_HISTORY_INDEX)
                .mapping(correlationMappings())
                .settings(Settings.builder().put("index.hidden", true).put("number_of_shards", noOfShards).put("index.correlation", true).build());
            client.admin().indices().create(indexRequest, actionListener);
        }
    }

    public boolean correlationIndexExists() {
        ClusterState clusterState = clusterService.state();
        return clusterState.getRoutingTable().hasIndex(Correlation.CORRELATION_HISTORY_INDEX);
    }

    public void setupCorrelationIndex(Long setupTimestamp, ActionListener<BulkResponse> listener) throws IOException {
        long currentTimestamp = System.currentTimeMillis();

        Correlation rootRecord = new Correlation(NO_ID,
            NO_VERSION,
            true,
            0L,
            "",
            "",
            new float[]{},
            currentTimestamp,
            "",
            "",
            List.of(),
            0L);
        IndexRequest indexRequest = new IndexRequest(Correlation.CORRELATION_HISTORY_INDEX)
            .source(rootRecord.toXContent(XContentFactory.jsonBuilder(), ToXContent.EMPTY_PARAMS))
            .timeout(TimeValue.timeValueSeconds(60));

        Correlation scoreRootRecord = new Correlation(NO_ID,
            NO_VERSION,
            false,
            0L,
            "",
            "",
            new float[]{},
            0L,
            "",
            "",
            List.of(),
            setupTimestamp
        );
        IndexRequest scoreIndexRequest = new IndexRequest(Correlation.CORRELATION_HISTORY_INDEX)
            .source(scoreRootRecord.toXContent(XContentFactory.jsonBuilder(), ToXContent.EMPTY_PARAMS))
            .timeout(TimeValue.timeValueSeconds(60));

        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.add(indexRequest);
        bulkRequest.add(scoreIndexRequest);
        bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

        client.bulk(bulkRequest, listener);
    }
}
