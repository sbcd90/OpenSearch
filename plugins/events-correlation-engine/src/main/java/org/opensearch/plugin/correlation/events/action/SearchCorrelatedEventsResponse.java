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
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.plugin.correlation.events.model.EventWithScore;
import org.opensearch.rest.RestStatus;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class SearchCorrelatedEventsResponse extends ActionResponse implements ToXContentObject {

    private List<EventWithScore> events;

    private RestStatus status;

    private static final String EVENTS = "events";

    public SearchCorrelatedEventsResponse(List<EventWithScore> events, RestStatus status) {
        super();
        this.events = events;
        this.status = status;
    }

    public SearchCorrelatedEventsResponse(StreamInput sin) throws IOException {
        this(
            Collections.unmodifiableList(sin.readList(EventWithScore::new)),
            sin.readEnum(RestStatus.class)
        );
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject()
            .field(EVENTS, events)
            .endObject();
        return builder.endObject();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeCollection(events);
        out.writeEnum(status);
    }
}
