/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.correlation.rules.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.common.xcontent.XContentParserUtils;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentParser;

import java.io.IOException;

public class CorrelationQuery implements Writeable, ToXContentObject {

    private static final Logger log = LogManager.getLogger(CorrelationQuery.class);
    private static final String INDEX = "index";
    private static final String QUERY = "query";
    private static final String TIMESTAMP_FIELD = "timestampField";

    private String index;

    private String query;

    private String timestampField;

    public CorrelationQuery(String index, String query, String timestampField) {
        this.index = index;
        this.query = query;
        this.timestampField = timestampField;
    }

    public CorrelationQuery(StreamInput sin) throws IOException {
        this(sin.readString(), sin.readString(), sin.readString());
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(index);
        out.writeString(query);
        out.writeString(timestampField);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(INDEX, index).field(QUERY, query).field(TIMESTAMP_FIELD, timestampField);
        return builder.endObject();
    }

    public static CorrelationQuery parse(XContentParser xcp) throws IOException {
        String index = null;
        String query = null;
        String timestampField = null;

        XContentParserUtils.ensureExpectedToken(XContentParser.Token.START_OBJECT, xcp.currentToken(), xcp);
        while (xcp.nextToken() != XContentParser.Token.END_OBJECT) {
            String fieldName = xcp.currentName();
            xcp.nextToken();

            switch (fieldName) {
                case INDEX:
                    index = xcp.text();
                    break;
                case QUERY:
                    query = xcp.text();
                    break;
                case TIMESTAMP_FIELD:
                    timestampField = xcp.text();
                    break;
                default:
                    xcp.skipChildren();
            }
        }
        return new CorrelationQuery(index, query, timestampField);
    }

    public static CorrelationQuery readFrom(StreamInput sin) throws IOException {
        return new CorrelationQuery(sin);
    }

    public String getIndex() {
        return index;
    }

    public String getQuery() {
        return query;
    }

    public String getTimestampField() {
        return timestampField;
    }
}
