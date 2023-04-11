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
import java.util.List;
import java.util.Map;

public class IndexCorrelationResponse extends ActionResponse {

    private Boolean isOrphan;

    private Map<String, List<String>> neighborEvents;

    private RestStatus status;

    public IndexCorrelationResponse(Boolean isOrphan, Map<String, List<String>> neighborEvents, RestStatus status) {
        super();
        this.isOrphan = isOrphan;
        this.neighborEvents = neighborEvents;
        this.status = status;
    }

    public IndexCorrelationResponse(StreamInput sin) throws IOException {
        this(
            sin.readBoolean(),
            sin.readMap(StreamInput::readString, StreamInput::readStringList),
            sin.readEnum(RestStatus.class)
        );
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeBoolean(isOrphan);
        out.writeMap(neighborEvents, StreamOutput::writeString, StreamOutput::writeStringCollection);
        out.writeEnum(status);
    }

    public RestStatus getStatus() {
        return status;
    }

    public Boolean getOrphan() {
        return isOrphan;
    }

    public Map<String, List<String>> getNeighborEvents() {
        return neighborEvents;
    }
}
