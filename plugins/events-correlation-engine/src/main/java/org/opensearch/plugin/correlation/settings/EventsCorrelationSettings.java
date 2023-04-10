/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.correlation.settings;

import org.opensearch.common.settings.Setting;
import org.opensearch.common.unit.TimeValue;

import java.util.concurrent.TimeUnit;

import static org.opensearch.common.settings.Setting.Property.IndexScope;

public class EventsCorrelationSettings {
    public static final String CORRELATION_INDEX = "index.correlation";
    public static final Setting<Boolean> IS_CORRELATION_INDEX_SETTING = Setting.boolSetting(CORRELATION_INDEX, false, IndexScope);

    public static final Setting<TimeValue> CORRELATION_TIME_WINDOW = Setting.positiveTimeSetting(
        "plugins.security_analytics.correlation_time_window",
        new TimeValue(5, TimeUnit.MINUTES),
        Setting.Property.NodeScope, Setting.Property.Dynamic
    );
}
