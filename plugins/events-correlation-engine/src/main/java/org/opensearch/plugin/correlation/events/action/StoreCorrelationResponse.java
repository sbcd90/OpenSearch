/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.correlation.events.action;

import org.opensearch.action.ActionResponse;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.rest.RestStatus;

import java.io.IOException;

public class StoreCorrelationResponse extends ActionResponse {

    private RestStatus status;

    public StoreCorrelationResponse(RestStatus status) {
        super();
        this.status = status;
    }

    public StoreCorrelationResponse(StreamInput sin) throws IOException {
        this(
            sin.readEnum(RestStatus.class)
        );
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeEnum(status);
    }
}
