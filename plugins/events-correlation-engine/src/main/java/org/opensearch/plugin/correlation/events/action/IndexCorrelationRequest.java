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

public class IndexCorrelationRequest extends ActionRequest {

    private String index;

    private String event;

    private Boolean store;

    public IndexCorrelationRequest(String index, String event, Boolean store) {
        super();
        this.index = index;
        this.event = event;
        this.store = store;
    }

    public IndexCorrelationRequest(StreamInput sin) throws IOException {
        this(sin.readString(), sin.readString(), sin.readBoolean());
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(index);
        out.writeString(event);
        out.writeBoolean(store);
    }

    public String getIndex() {
        return index;
    }

    public String getEvent() {
        return event;
    }

    public Boolean getStore() {
        return store;
    }
}
