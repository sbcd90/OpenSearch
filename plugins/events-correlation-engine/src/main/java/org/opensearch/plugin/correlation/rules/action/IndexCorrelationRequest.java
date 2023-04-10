/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.correlation.rules.action;

import org.opensearch.action.ActionRequest;
import org.opensearch.action.ActionRequestValidationException;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;

import java.io.IOException;

public class IndexCorrelationRequest extends ActionRequest {

    private String index;

    private String event;

    public IndexCorrelationRequest(String index, String event) {
        super();
        this.index = index;
        this.event = event;
    }

    public IndexCorrelationRequest(StreamInput sin) throws IOException {
        this(sin.readString(), sin.readString());
    }

    @Override
    public ActionRequestValidationException validate() {
        return null;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(index);
        out.writeString(event);
    }

    public String getIndex() {
        return index;
    }

    public String getEvent() {
        return event;
    }
}
