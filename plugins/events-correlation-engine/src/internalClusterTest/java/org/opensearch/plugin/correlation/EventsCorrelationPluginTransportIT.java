/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.correlation;

import org.apache.lucene.search.join.ScoreMode;
import org.junit.Assert;
import org.opensearch.action.admin.cluster.node.info.NodeInfo;
import org.opensearch.action.admin.cluster.node.info.NodesInfoRequest;
import org.opensearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.opensearch.action.admin.cluster.node.info.PluginsAndModules;
import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.index.query.NestedQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.plugin.correlation.events.action.IndexCorrelationAction;
import org.opensearch.plugin.correlation.events.action.IndexCorrelationRequest;
import org.opensearch.plugin.correlation.events.action.IndexCorrelationResponse;
import org.opensearch.plugin.correlation.rules.action.IndexCorrelationRuleAction;
import org.opensearch.plugin.correlation.rules.action.IndexCorrelationRuleRequest;
import org.opensearch.plugin.correlation.rules.action.IndexCorrelationRuleResponse;
import org.opensearch.plugin.correlation.rules.model.CorrelationQuery;
import org.opensearch.plugin.correlation.rules.model.CorrelationRule;
import org.opensearch.plugins.Plugin;
import org.opensearch.plugins.PluginInfo;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestStatus;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.test.OpenSearchIntegTestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EventsCorrelationPluginTransportIT extends OpenSearchIntegTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Arrays.asList(EventsCorrelationPlugin.class);
    }

    public void testPluginsAreInstalled() {
        NodesInfoRequest nodesInfoRequest = new NodesInfoRequest();
        nodesInfoRequest.addMetric(NodesInfoRequest.Metric.PLUGINS.metricName());
        NodesInfoResponse nodesInfoResponse = OpenSearchIntegTestCase.client().admin().cluster().nodesInfo(nodesInfoRequest).actionGet();
        List<PluginInfo> pluginInfos = nodesInfoResponse.getNodes()
            .stream()
            .flatMap(
                (Function<NodeInfo, Stream<PluginInfo>>) nodeInfo -> nodeInfo.getInfo(PluginsAndModules.class).getPluginInfos().stream()
            )
            .collect(Collectors.toList());
        Assert.assertTrue(
            pluginInfos.stream()
                .anyMatch(pluginInfo -> pluginInfo.getName().equals("org.opensearch.plugin.correlation.EventsCorrelationPlugin"))
        );
    }

    public void testCreatingACorrelationRule() throws ExecutionException, InterruptedException {
        List<CorrelationQuery> correlationQueries = Arrays.asList(
            new CorrelationQuery("s3_access_logs", "aws.cloudtrail.eventName:ReplicateObject", "@timestamp"),
            new CorrelationQuery("app_logs", "keywords:PermissionDenied", "@timestamp")
        );
        CorrelationRule correlationRule = new CorrelationRule(CorrelationRule.NO_ID, CorrelationRule.NO_VERSION, "s3 to app logs", correlationQueries);
        IndexCorrelationRuleRequest request = new IndexCorrelationRuleRequest(CorrelationRule.NO_ID, correlationRule, RestRequest.Method.POST);

        IndexCorrelationRuleResponse response = client().execute(IndexCorrelationRuleAction.INSTANCE, request).get();
        Assert.assertEquals(RestStatus.CREATED, response.getStatus());

        NestedQueryBuilder queryBuilder = QueryBuilders.nestedQuery(
            "correlate",
            QueryBuilders.matchQuery("correlate.index", "s3_access_logs"),
            ScoreMode.None
        );
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        searchSourceBuilder.fetchSource(true);

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(CorrelationRule.CORRELATION_RULE_INDEX);
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client().search(searchRequest).get();
        Assert.assertEquals(1L, searchResponse.getHits().getTotalHits().value);
    }

    public void testFilteringCorrelationRules() throws ExecutionException, InterruptedException {
        List<CorrelationQuery> correlationQueries1 = Arrays.asList(
            new CorrelationQuery("s3_access_logs", "aws.cloudtrail.eventName:ReplicateObject", "@timestamp"),
            new CorrelationQuery("app_logs", "keywords:PermissionDenied", "@timestamp")
        );
        CorrelationRule correlationRule1 = new CorrelationRule(CorrelationRule.NO_ID, CorrelationRule.NO_VERSION, "s3 to app logs", correlationQueries1);
        IndexCorrelationRuleRequest request1 = new IndexCorrelationRuleRequest(CorrelationRule.NO_ID, correlationRule1, RestRequest.Method.POST);
        client().execute(IndexCorrelationRuleAction.INSTANCE, request1).get();

        List<CorrelationQuery> correlationQueries2 = Arrays.asList(
            new CorrelationQuery("windows", "host.hostname:EC2AMAZ*", "@timestamp"),
            new CorrelationQuery("app_logs", "endpoint:/customer_records.txt", "@timestamp")
        );
        CorrelationRule correlationRule2 = new CorrelationRule(CorrelationRule.NO_ID, CorrelationRule.NO_VERSION, "windows to app logs", correlationQueries2);
        IndexCorrelationRuleRequest request2 = new IndexCorrelationRuleRequest(CorrelationRule.NO_ID, correlationRule2, RestRequest.Method.POST);
        client().execute(IndexCorrelationRuleAction.INSTANCE, request2).get();

        NestedQueryBuilder queryBuilder = QueryBuilders.nestedQuery(
            "correlate",
            QueryBuilders.matchQuery("correlate.index", "s3_access_logs"),
            ScoreMode.None
        );
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        searchSourceBuilder.fetchSource(true);

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices(CorrelationRule.CORRELATION_RULE_INDEX);
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client().search(searchRequest).get();
        Assert.assertEquals(1L, searchResponse.getHits().getTotalHits().value);
    }

    public void testEventOnIndexWithNoRules() throws ExecutionException, InterruptedException {
        String windowsIndex = "windows";
        CreateIndexRequest windowsRequest = new CreateIndexRequest(windowsIndex)
            .mapping(windowsMappings()).settings(Settings.EMPTY);

        client().admin().indices().create(windowsRequest).get();

        String appLogsIndex = "app_logs";
        CreateIndexRequest appLogsRequest = new CreateIndexRequest(appLogsIndex)
            .mapping(appLogsMappings()).settings(Settings.EMPTY);

        client().admin().indices().create(appLogsRequest).get();

        List<CorrelationQuery> correlationQueries = List.of(
            new CorrelationQuery("app_logs", "endpoint:\\/customer_records.txt", "timestamp")
        );
        CorrelationRule correlationRule = new CorrelationRule(CorrelationRule.NO_ID, CorrelationRule.NO_VERSION, "windows to app logs", correlationQueries);
        IndexCorrelationRuleRequest request = new IndexCorrelationRuleRequest(CorrelationRule.NO_ID, correlationRule, RestRequest.Method.POST);
        client().execute(IndexCorrelationRuleAction.INSTANCE, request).get();

        IndexRequest indexRequestWindows = new IndexRequest("windows")
            .source(sampleWindowsEvent(), XContentType.JSON);
        IndexResponse response = client().index(indexRequestWindows).get();
        String eventId = response.getId();

        IndexCorrelationRequest correlationRequest = new IndexCorrelationRequest("windows", eventId);
        IndexCorrelationResponse correlationResponse = client().execute(IndexCorrelationAction.INSTANCE, correlationRequest).get();

        Assert.assertEquals(200, correlationResponse.getStatus().getStatus());
        Assert.assertTrue(correlationResponse.getOrphan());
        Assert.assertEquals(0, correlationResponse.getNeighborEvents().size());
    }

    public void testEventOnIndexWithNoMatchingRules() throws ExecutionException, InterruptedException {
        String windowsIndex = "windows";
        CreateIndexRequest windowsRequest = new CreateIndexRequest(windowsIndex)
            .mapping(windowsMappings()).settings(Settings.EMPTY);

        client().admin().indices().create(windowsRequest).get();

        String appLogsIndex = "app_logs";
        CreateIndexRequest appLogsRequest = new CreateIndexRequest(appLogsIndex)
            .mapping(appLogsMappings()).settings(Settings.EMPTY);

        client().admin().indices().create(appLogsRequest).get();

        List<CorrelationQuery> correlationQueries = Arrays.asList(
            new CorrelationQuery("windows", "host.hostname:EC2BMAZ*", "winlog.timestamp")
        );
        CorrelationRule correlationRule = new CorrelationRule(CorrelationRule.NO_ID, CorrelationRule.NO_VERSION, "windows to app logs", correlationQueries);
        IndexCorrelationRuleRequest request = new IndexCorrelationRuleRequest(CorrelationRule.NO_ID, correlationRule, RestRequest.Method.POST);
        client().execute(IndexCorrelationRuleAction.INSTANCE, request).get();

        IndexRequest indexRequestWindows = new IndexRequest("windows")
            .source(sampleWindowsEvent(), XContentType.JSON);
        IndexResponse response = client().index(indexRequestWindows).get();
        String eventId = response.getId();

        IndexCorrelationRequest correlationRequest = new IndexCorrelationRequest("windows", eventId);
        IndexCorrelationResponse correlationResponse = client().execute(IndexCorrelationAction.INSTANCE, correlationRequest).get();

        Assert.assertEquals(200, correlationResponse.getStatus().getStatus());
        Assert.assertTrue(correlationResponse.getOrphan());
        Assert.assertEquals(0, correlationResponse.getNeighborEvents().size());
    }

    public void testCorrelationWithSingleRule() throws ExecutionException, InterruptedException {
        String windowsIndex = "windows";
        CreateIndexRequest windowsRequest = new CreateIndexRequest(windowsIndex)
            .mapping(windowsMappings()).settings(Settings.EMPTY);

        client().admin().indices().create(windowsRequest).get();

        String appLogsIndex = "app_logs";
        CreateIndexRequest appLogsRequest = new CreateIndexRequest(appLogsIndex)
            .mapping(appLogsMappings()).settings(Settings.EMPTY);

        client().admin().indices().create(appLogsRequest).get();

        List<CorrelationQuery> correlationQueries = Arrays.asList(
            new CorrelationQuery("windows", "host.hostname:EC2AMAZ*", "winlog.timestamp"),
            new CorrelationQuery("app_logs", "endpoint:\\/customer_records.txt", "timestamp")
        );
        CorrelationRule correlationRule = new CorrelationRule(CorrelationRule.NO_ID, CorrelationRule.NO_VERSION, "windows to app logs", correlationQueries);
        IndexCorrelationRuleRequest request = new IndexCorrelationRuleRequest(CorrelationRule.NO_ID, correlationRule, RestRequest.Method.POST);
        client().execute(IndexCorrelationRuleAction.INSTANCE, request).get();

        IndexRequest indexRequestWindows = new IndexRequest("windows")
            .source(sampleWindowsEvent(), XContentType.JSON);
        client().index(indexRequestWindows).get();

        IndexRequest indexRequestAppLogs = new IndexRequest("app_logs")
            .source(sampleAppLogsEvent(), XContentType.JSON);
        IndexResponse response = client().index(indexRequestAppLogs).get();
        String eventId = response.getId();

        // sleep needed to ensure both events are inserted into their respective indices.
        Thread.sleep(10000);
        IndexCorrelationRequest correlationRequest = new IndexCorrelationRequest("app_logs", eventId);
        IndexCorrelationResponse correlationResponse = client().execute(IndexCorrelationAction.INSTANCE, correlationRequest).get();

        Assert.assertEquals(200, correlationResponse.getStatus().getStatus());
        Assert.assertEquals(1, correlationResponse.getNeighborEvents().size());
        Assert.assertFalse(correlationResponse.getOrphan());
    }

    public void testCorrelationWithMultipleRule() throws ExecutionException, InterruptedException {
        String windowsIndex = "windows";
        CreateIndexRequest windowsRequest = new CreateIndexRequest(windowsIndex)
            .mapping(windowsMappings()).settings(Settings.EMPTY);

        client().admin().indices().create(windowsRequest).get();

        String appLogsIndex = "app_logs";
        CreateIndexRequest appLogsRequest = new CreateIndexRequest(appLogsIndex)
            .mapping(appLogsMappings()).settings(Settings.EMPTY);

        client().admin().indices().create(appLogsRequest).get();

        List<CorrelationQuery> correlationQueries1 = Arrays.asList(
            new CorrelationQuery("windows", "host.hostname:EC2AMAZ*", "winlog.timestamp"),
            new CorrelationQuery("app_logs", "endpoint:\\/customer_records.txt", "timestamp")
        );
        CorrelationRule correlationRule1 = new CorrelationRule(CorrelationRule.NO_ID, CorrelationRule.NO_VERSION, "windows to app logs", correlationQueries1);
        IndexCorrelationRuleRequest request1 = new IndexCorrelationRuleRequest(CorrelationRule.NO_ID, correlationRule1, RestRequest.Method.POST);
        client().execute(IndexCorrelationRuleAction.INSTANCE, request1).get();

        List<CorrelationQuery> correlationQueries2 = Arrays.asList(
            new CorrelationQuery("windows", "host.hostname:EC2BMAZ*", "winlog.timestamp"),
            new CorrelationQuery("app_logs", "endpoint:\\/customer_records1.txt", "timestamp")
        );
        CorrelationRule correlationRule2 = new CorrelationRule(CorrelationRule.NO_ID, CorrelationRule.NO_VERSION, "windows to app logs", correlationQueries2);
        IndexCorrelationRuleRequest request2 = new IndexCorrelationRuleRequest(CorrelationRule.NO_ID, correlationRule2, RestRequest.Method.POST);
        client().execute(IndexCorrelationRuleAction.INSTANCE, request2).get();

        IndexRequest indexRequestWindows = new IndexRequest("windows")
            .source(sampleWindowsEvent(), XContentType.JSON);
        client().index(indexRequestWindows).get();

        IndexRequest indexRequestAppLogs = new IndexRequest("app_logs")
            .source(sampleAppLogsEvent(), XContentType.JSON);
        IndexResponse response = client().index(indexRequestAppLogs).get();
        String eventId = response.getId();

        // sleep needed to ensure both events are inserted into their respective indices.
        Thread.sleep(10000);
        IndexCorrelationRequest correlationRequest = new IndexCorrelationRequest("app_logs", eventId);
        IndexCorrelationResponse correlationResponse = client().execute(IndexCorrelationAction.INSTANCE, correlationRequest).get();

        Assert.assertEquals(200, correlationResponse.getStatus().getStatus());
        Assert.assertFalse(correlationResponse.getOrphan());
        Assert.assertEquals(1, correlationResponse.getNeighborEvents().size());
    }

    private String windowsMappings() {
        return "    \"properties\": {\n" +
            "      \"server.user.hash\": {\n" +
            "        \"type\": \"text\"\n" +
            "      },\n" +
            "      \"winlog.event_id\": {\n" +
            "        \"type\": \"integer\"\n" +
            "      },\n" +
            "      \"host.hostname\": {\n" +
            "        \"type\": \"text\"\n" +
            "      },\n" +
            "      \"windows.message\": {\n" +
            "        \"type\": \"text\"\n" +
            "      },\n" +
            "      \"winlog.provider_name\": {\n" +
            "        \"type\": \"text\"\n" +
            "      },\n" +
            "      \"winlog.event_data.ServiceName\": {\n" +
            "        \"type\": \"text\"\n" +
            "      },\n" +
            "      \"winlog.timestamp\": {\n" +
            "        \"type\": \"long\"\n" +
            "      }\n" +
            "    }\n";
    }

    private String sampleWindowsEvent() {
        return "{\n" +
            "  \"EventTime\": \"2020-02-04T14:59:39.343541+00:00\",\n" +
            "  \"host.hostname\": \"EC2AMAZ-EPO7HKA\",\n" +
            "  \"Keywords\": \"9223372036854775808\",\n" +
            "  \"SeverityValue\": 2,\n" +
            "  \"Severity\": \"INFO\",\n" +
            "  \"winlog.event_id\": 22,\n" +
            "  \"SourceName\": \"Microsoft-Windows-Sysmon\",\n" +
            "  \"ProviderGuid\": \"{5770385F-C22A-43E0-BF4C-06F5698FFBD9}\",\n" +
            "  \"Version\": 5,\n" +
            "  \"TaskValue\": 22,\n" +
            "  \"OpcodeValue\": 0,\n" +
            "  \"RecordNumber\": 9532,\n" +
            "  \"ExecutionProcessID\": 1996,\n" +
            "  \"ExecutionThreadID\": 2616,\n" +
            "  \"Channel\": \"Microsoft-Windows-Sysmon/Operational\",\n" +
            "  \"winlog.event_data.SubjectDomainName\": \"NTAUTHORITY\",\n" +
            "  \"AccountName\": \"SYSTEM\",\n" +
            "  \"UserID\": \"S-1-5-18\",\n" +
            "  \"AccountType\": \"User\",\n" +
            "  \"windows.message\": \"Dns query:\\r\\nRuleName: \\r\\nUtcTime: 2020-02-04 14:59:38.349\\r\\nProcessGuid: {b3c285a4-3cda-5dc0-0000-001077270b00}\\r\\nProcessId: 1904\\r\\nQueryName: EC2AMAZ-EPO7HKA\\r\\nQueryStatus: 0\\r\\nQueryResults: 172.31.46.38;\\r\\nImage: C:\\\\Program Files\\\\nxlog\\\\nxlog.exe\",\n" +
            "  \"Category\": \"Dns query (rule: DnsQuery)\",\n" +
            "  \"Opcode\": \"Info\",\n" +
            "  \"UtcTime\": \"2020-02-04 14:59:38.349\",\n" +
            "  \"ProcessGuid\": \"{b3c285a4-3cda-5dc0-0000-001077270b00}\",\n" +
            "  \"ProcessId\": \"1904\",\n" +
            "  \"QueryName\": \"EC2AMAZ-EPO7HKA\",\n" +
            "  \"QueryStatus\": \"0\",\n" +
            "  \"QueryResults\": \"172.31.46.38;\",\n" +
            "  \"Image\": \"C:\\\\Program Files\\\\nxlog\\\\regsvr32.exe\",\n" +
            "  \"EventReceivedTime\": \"2020-02-04T14:59:40.780905+00:00\",\n" +
            "  \"SourceModuleName\": \"in\",\n" +
            "  \"SourceModuleType\": \"im_msvistalog\",\n" +
            "  \"CommandLine\": \"eachtest\",\n" +
            "  \"Initiated\": \"true\",\n" +
            " \"winlog.timestamp\": " + System.currentTimeMillis() + "\n" +
            "}";
    }

    private String appLogsMappings() {
        return "    \"properties\": {\n" +
            "      \"http_method\": {\n" +
            "        \"type\": \"text\"\n" +
            "      },\n" +
            "      \"endpoint\": {\n" +
            "        \"type\": \"text\"\n" +
            "      },\n" +
            "      \"keywords\": {\n" +
            "        \"type\": \"text\"\n" +
            "      },\n" +
            "      \"timestamp\": {\n" +
            "        \"type\": \"long\"\n" +
            "      }\n" +
            "    }\n";
    }

    private String sampleAppLogsEvent() {
        return "{\n" +
            "  \"endpoint\": \"/customer_records.txt\",\n" +
            "  \"http_method\": \"POST\",\n" +
            "  \"keywords\": \"PermissionDenied\",\n" +
            "  \"timestamp\": " + System.currentTimeMillis() + "\n" +
            "}";
    }
}
