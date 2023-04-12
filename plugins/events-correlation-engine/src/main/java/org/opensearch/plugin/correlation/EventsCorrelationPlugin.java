/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.correlation;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionResponse;
import org.opensearch.client.Client;
import org.opensearch.cluster.metadata.IndexNameExpressionResolver;
import org.opensearch.cluster.node.DiscoveryNodes;
import org.opensearch.cluster.service.ClusterService;
import org.opensearch.common.io.stream.NamedWriteableRegistry;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.common.settings.IndexScopedSettings;
import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.settings.SettingsFilter;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.env.Environment;
import org.opensearch.env.NodeEnvironment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.codec.CodecServiceFactory;
import org.opensearch.index.mapper.Mapper;
import org.opensearch.plugin.correlation.core.index.codec.CorrelationCodecService;
import org.opensearch.plugin.correlation.core.index.mapper.VectorFieldMapper;
import org.opensearch.plugin.correlation.core.index.query.CorrelationQueryBuilder;
import org.opensearch.plugin.correlation.events.action.IndexCorrelationAction;
import org.opensearch.plugin.correlation.events.action.SearchCorrelatedEventsAction;
import org.opensearch.plugin.correlation.events.action.StoreCorrelationAction;
import org.opensearch.plugin.correlation.events.transport.TransportSearchCorrelatedEventsAction;
import org.opensearch.plugin.correlation.events.transport.TransportStoreCorrelationAction;
import org.opensearch.plugin.correlation.rules.action.IndexCorrelationRuleAction;
import org.opensearch.plugin.correlation.rules.resthandler.RestIndexCorrelationRuleAction;
import org.opensearch.plugin.correlation.events.transport.TransportIndexCorrelationAction;
import org.opensearch.plugin.correlation.rules.transport.TransportIndexCorrelationRuleAction;
import org.opensearch.plugin.correlation.settings.EventsCorrelationSettings;
import org.opensearch.plugin.correlation.utils.CorrelationIndices;
import org.opensearch.plugin.correlation.utils.CorrelationRuleIndices;
import org.opensearch.plugins.ActionPlugin;
import org.opensearch.plugins.EnginePlugin;
import org.opensearch.plugins.MapperPlugin;
import org.opensearch.plugins.Plugin;
import org.opensearch.plugins.SearchPlugin;
import org.opensearch.repositories.RepositoriesService;
import org.opensearch.rest.RestController;
import org.opensearch.rest.RestHandler;
import org.opensearch.script.ScriptService;
import org.opensearch.threadpool.ThreadPool;
import org.opensearch.watcher.ResourceWatcherService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class EventsCorrelationPlugin extends Plugin implements ActionPlugin, MapperPlugin, SearchPlugin, EnginePlugin {

    public static final String PLUGINS_BASE_URI = "/_plugins/_correlation";
    public static final String CORRELATION_RULES_BASE_URI = PLUGINS_BASE_URI + "/rules";

    private CorrelationRuleIndices correlationRuleIndices;

    private CorrelationIndices correlationIndices;

    @Override
    public Collection<Object> createComponents(
        Client client,
        ClusterService clusterService,
        ThreadPool threadPool,
        ResourceWatcherService resourceWatcherService,
        ScriptService scriptService,
        NamedXContentRegistry xContentRegistry,
        Environment environment,
        NodeEnvironment nodeEnvironment,
        NamedWriteableRegistry namedWriteableRegistry,
        IndexNameExpressionResolver indexNameExpressionResolver,
        Supplier<RepositoriesService> repositoriesServiceSupplier
    ) {
        correlationRuleIndices = new CorrelationRuleIndices(client, clusterService);
        correlationIndices = new CorrelationIndices(client, clusterService, clusterService.getSettings());
        return List.of(correlationRuleIndices, correlationIndices);
    }

    @Override
    public List<RestHandler> getRestHandlers(
        Settings settings,
        RestController restController,
        ClusterSettings clusterSettings,
        IndexScopedSettings indexScopedSettings,
        SettingsFilter settingsFilter,
        IndexNameExpressionResolver indexNameExpressionResolver,
        Supplier<DiscoveryNodes> nodesInCluster
    ) {
        return List.of(new RestIndexCorrelationRuleAction());
    }

    @Override
    public Map<String, Mapper.TypeParser> getMappers() {
        return Collections.singletonMap(
            VectorFieldMapper.CONTENT_TYPE,
            new VectorFieldMapper.TypeParser()
        );
    }

    @Override
    public Optional<CodecServiceFactory> getCustomCodecServiceFactory(IndexSettings indexSettings) {
        if (indexSettings.getValue(EventsCorrelationSettings.IS_CORRELATION_INDEX_SETTING)) {
            return Optional.of(CorrelationCodecService::new);
        }
        return Optional.empty();
    }

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        return List.of(
            new ActionPlugin.ActionHandler<>(IndexCorrelationRuleAction.INSTANCE, TransportIndexCorrelationRuleAction.class),
            new ActionPlugin.ActionHandler<>(IndexCorrelationAction.INSTANCE, TransportIndexCorrelationAction.class),
            new ActionPlugin.ActionHandler<>(StoreCorrelationAction.INSTANCE, TransportStoreCorrelationAction.class),
            new ActionPlugin.ActionHandler<>(SearchCorrelatedEventsAction.INSTANCE, TransportSearchCorrelatedEventsAction.class));
    }

    @Override
    public List<QuerySpec<?>> getQueries() {
        return Collections.singletonList(new QuerySpec<>(CorrelationQueryBuilder.NAME, CorrelationQueryBuilder::new, CorrelationQueryBuilder::fromXContent));
    }

    @Override
    public List<Setting<?>> getSettings() {
        return List.of(
            EventsCorrelationSettings.IS_CORRELATION_INDEX_SETTING,
            EventsCorrelationSettings.CORRELATION_HISTORY_INDEX_SHARDS,
            EventsCorrelationSettings.CORRELATION_TIME_WINDOW
        );
    }
}
