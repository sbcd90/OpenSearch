/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.correlation.events.action;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class StoreCorrelationRequest extends ActionRequest {

    private String index;

    private String event;

    private Long timestamp;

    private Map<String, List<String>> eventsAdjacencyList;

    private List<String> tags;

    public StoreCorrelationRequest(String index, String event, Long timestamp, Map<String, List<String>> eventsAdjacencyList, List<String> tags) {
        super();
        this.index = index;
        this.event = event;
        this.timestamp = timestamp;
        this.eventsAdjacencyList = eventsAdjacencyList;
        this.tags = tags;
    }

    public StoreCorrelationRequest(StreamInput sin) throws IOException {
        this(
            sin.readString(),
            sin.readString(),
            sin.readLong(),
            sin.readMap(StreamInput::readString, StreamInput::readStringList),
            sin.readStringList()
        );
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(index);
        out.writeString(event);
        out.writeLong(timestamp);
        out.writeMap(eventsAdjacencyList, StreamOutput::writeString, StreamOutput::writeStringCollection);
        out.writeStringCollection(tags);
    }

    public String getEvent() {
        return event;
    }

    public String getIndex() {
        return index;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public List<String> getTags() {
        return tags;
    }

    public Map<String, List<String>> getEventsAdjacencyList() {
        return eventsAdjacencyList;
    }
}
