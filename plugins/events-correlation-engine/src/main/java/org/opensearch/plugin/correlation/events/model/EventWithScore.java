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
import org.opensearch.core.ParseField;
import org.opensearch.core.xcontent.ObjectParser;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.core.xcontent.XContentParser;

import java.io.IOException;
import java.util.List;

/**
 * Event with score output
 * {
 *   "index": "app_logs",
 *   "event": "EYT06YgBpmAY3ZcggCts",
 *   "score": 1,
 *   "tags": []
 * }
 *
 * @opensearch.api
 * @opensearch.experimental
 */
public class EventWithScore implements Writeable, ToXContentObject {

    private static final ParseField INDEX_FIELD = new ParseField("index");
    private static final ParseField EVENT_FIELD = new ParseField("event");
    private static final ParseField SCORE_FIELD = new ParseField("score");
    private static final ParseField TAGS_FIELD = new ParseField("tags");
    private static final ObjectParser<EventWithScore, Void> PARSER = new ObjectParser<EventWithScore, Void>(
        "EventWithScore",
        EventWithScore::new
    );

    private String index;

    private String event;

    private Double score;

    private List<String> tags;

    static {
        PARSER.declareString(EventWithScore::setIndex, INDEX_FIELD);
        PARSER.declareString(EventWithScore::setEvent, EVENT_FIELD);
        PARSER.declareDouble(EventWithScore::setScore, SCORE_FIELD);
        PARSER.declareStringArray(EventWithScore::setTags, TAGS_FIELD);
    }

    private EventWithScore() {}

    /**
     * Parameterized ctor of Event with score object
     * @param index index of correlated event
     * @param event correlated event
     * @param score score of correlation
     * @param tags tags of correlated event
     */
    public EventWithScore(String index, String event, Double score, List<String> tags) {
        this.index = index;
        this.event = event;
        this.score = score;
        this.tags = tags;
    }

    /**
     * StreamInput ctor of Event with score object
     * @param sin StreamInput
     * @throws IOException IOException
     */
    public EventWithScore(StreamInput sin) throws IOException {
        this(sin.readString(), sin.readString(), sin.readDouble(), sin.readStringList());
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
            .field(INDEX_FIELD.getPreferredName(), index)
            .field(EVENT_FIELD.getPreferredName(), event)
            .field(SCORE_FIELD.getPreferredName(), score)
            .field(TAGS_FIELD.getPreferredName(), tags);
        return builder.endObject();
    }

    /**
     * Parse into EventWithScore
     * @param xcp XContentParser
     * @return EventWithScore
     * @throws IOException IOException
     */
    public static EventWithScore parse(XContentParser xcp) throws IOException {
        return PARSER.apply(xcp, null);
    }

    /**
     * set index of correlated event
     * @param index index of correlated event
     */
    public void setIndex(String index) {
        this.index = index;
    }

    /**
     * set tags of correlated event
     * @param tags tags of correlated event
     */
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    /**
     * set correlated event
     * @param event correlated event
     */
    public void setEvent(String event) {
        this.event = event;
    }

    /**
     * set score of correlation
     * @param score score of correlation
     */
    public void setScore(Double score) {
        this.score = score;
    }
}
