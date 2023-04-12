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

public class SearchCorrelatedEventsRequest extends ActionRequest {

    private String index;

    private String event;

    private String timestampField;

    private Long timeWindow;

    private Integer nearbyEvents;

    public SearchCorrelatedEventsRequest(String index, String event, String timestampField, Long timeWindow, Integer nearbyEvents) {
        super();
        this.index = index;
        this.event = event;
        this.timestampField = timestampField;
        this.timeWindow = timeWindow;
        this.nearbyEvents = nearbyEvents;
    }

    public SearchCorrelatedEventsRequest(StreamInput sin) throws IOException {
        this(
            sin.readString(),
            sin.readString(),
            sin.readString(),
            sin.readLong(),
            sin.readInt()
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
        out.writeString(timestampField);
        out.writeLong(timeWindow);
        out.writeInt(nearbyEvents);
    }

    public String getIndex() {
        return index;
    }

    public String getEvent() {
        return event;
    }

    public String getTimestampField() {
        return timestampField;
    }

    public Long getTimeWindow() {
        return timeWindow;
    }

    public Integer getNearbyEvents() {
        return nearbyEvents;
    }
}
