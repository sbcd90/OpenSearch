/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.correlation.events.action;

import org.opensearch.action.ActionType;

public class StoreCorrelationAction extends ActionType<StoreCorrelationResponse> {

    public static final StoreCorrelationAction INSTANCE = new StoreCorrelationAction();
    public static final String NAME = "cluster:admin/store/correlation/events";

    private StoreCorrelationAction() {
        super(NAME, StoreCorrelationResponse::new);
    }
}
