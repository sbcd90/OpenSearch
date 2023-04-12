/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.correlation.events.model;

import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.common.io.stream.Writeable;
import org.opensearch.common.xcontent.XContentParserUtils;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EventWithScore implements Writeable, ToXContentObject {

    private static final String INDEX_FIELD = "index";
    private static final String EVENT_FIELD = "event";
    private static final String SCORE_FIELD = "score";
    private static final String TAGS_FIELD = "tags";

    private String index;

    private String event;

    private Double score;

    private List<String> tags;

    public EventWithScore(String index, String event, Double score, List<String> tags) {
        this.index = index;
        this.event = event;
        this.score = score;
        this.tags = tags;
    }

    public EventWithScore(StreamInput sin) throws IOException {
        this(
            sin.readString(),
            sin.readString(),
            sin.readDouble(),
            sin.readStringList()
        );
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(index);
        out.writeString(event);
        out.writeDouble(score);
        out.writeStringCollection(tags);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject()
            .field(INDEX_FIELD, index)
            .field(EVENT_FIELD, event)
            .field(SCORE_FIELD, score)
            .field(TAGS_FIELD, tags);
        return builder.endObject();
    }

    public static EventWithScore parse(XContentParser xcp) throws IOException {
        String index = null;
        String event = null;
        Double score = null;
        List<String> tags = new ArrayList<>();

        XContentParserUtils.ensureExpectedToken(XContentParser.Token.START_OBJECT, xcp.currentToken(), xcp);
        while (xcp.nextToken() != XContentParser.Token.END_OBJECT) {
            String fieldName = xcp.currentName();
            xcp.nextToken();

            switch (fieldName) {
                case INDEX_FIELD:
                    index = xcp.text();
                    break;
                case EVENT_FIELD:
                    event = xcp.text();
                    break;
                case SCORE_FIELD:
                    score = xcp.doubleValue();
                    break;
                case TAGS_FIELD:
                    XContentParserUtils.ensureExpectedToken(XContentParser.Token.START_ARRAY, xcp.currentToken(), xcp);
                    while (xcp.nextToken() != XContentParser.Token.END_ARRAY) {
                        String tag = xcp.text();
                        tags.add(tag);
                    }
                    break;
                default:
                    xcp.skipChildren();
            }
        }
        return new EventWithScore(index, event, score, tags);
    }
}
