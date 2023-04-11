/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.correlation.events.model;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.opensearch.plugin.correlation.rules.model.CorrelationRule.NO_ID;
import static org.opensearch.plugin.correlation.rules.model.CorrelationRule.NO_VERSION;

public class Correlation implements Writeable, ToXContentObject {

    private static final Logger log = LogManager.getLogger(Correlation.class);

    public static final String CORRELATION_HISTORY_INDEX = ".opensearch-correlation-history";

    private static final String ROOT_FIELD = "root";
    private static final String LEVEL_FIELD = "level";
    private static final String EVENT1_FIELD = "event1";
    private static final String EVENT2_FIELD = "event2";
    private static final String CORRELATION_VECTOR_FIELD = "corr_vector";
    private static final String TIMESTAMP_FIELD = "timestamp";
    private static final String INDEX1_FIELD = "index1";
    private static final String INDEX2_FIELD = "index2";
    private static final String TAGS_FIELD = "tags";
    private static final String SCORE_TIMESTAMP_FIELD = "score_timestamp";

    private String id;

    private Long version;

    private Boolean isRoot;

    private Long level;

    private String event1;

    private String event2;

    private float[] correlationVector;

    private Long timestamp;

    private String index1;

    private String index2;

    private List<String> tags;

    private Long scoreTimestamp;

    public Correlation(String id,
                       Long version,
                       Boolean isRoot,
                       Long level,
                       String event1,
                       String event2,
                       float[] correlationVector,
                       Long timestamp,
                       String index1,
                       String index2,
                       List<String> tags,
                       Long scoreTimestamp) {
        this.id = id != null ? id : NO_ID;
        this.version = version != null ? version : NO_VERSION;
        this.isRoot = isRoot;
        this.level = level;
        this.event1 = event1;
        this.event2 = event2;
        this.correlationVector = correlationVector;
        this.timestamp = timestamp;
        this.index1 = index1;
        this.index2 = index2;
        this.tags = tags;
        this.scoreTimestamp = scoreTimestamp;
    }

    public Correlation(StreamInput sin) throws IOException {
        this(sin.readString(),
            sin.readLong(),
            sin.readBoolean(),
            sin.readLong(),
            sin.readString(),
            sin.readString(),
            sin.readFloatArray(),
            sin.readLong(),
            sin.readString(),
            sin.readString(),
            sin.readStringList(),
            sin.readLong());
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(ROOT_FIELD, isRoot);
        builder.field(LEVEL_FIELD, level);
        builder.field(EVENT1_FIELD, event1);
        builder.field(EVENT2_FIELD, event2);
        builder.field(CORRELATION_VECTOR_FIELD, correlationVector);
        builder.field(TIMESTAMP_FIELD, timestamp);
        builder.field(INDEX1_FIELD, index1);
        builder.field(INDEX2_FIELD, index2);
        builder.field(TAGS_FIELD, tags);
        builder.field(SCORE_TIMESTAMP_FIELD, scoreTimestamp);
        return builder.endObject();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(id);
        out.writeLong(version);
        out.writeBoolean(isRoot);
        out.writeLong(level);
        out.writeString(event1);
        out.writeString(event2);
        out.writeFloatArray(correlationVector);
        out.writeLong(timestamp);
        out.writeString(index1);
        out.writeString(index2);
        out.writeStringCollection(tags);
        out.writeLong(scoreTimestamp);
    }

    public static Correlation parse(XContentParser xcp, String id, Long version) throws IOException {
        if (id == null) {
            id = NO_ID;
        }
        if (version == null) {
            version = NO_VERSION;
        }

        Boolean isRoot = null;
        Long level = null;
        String event1 = null;
        String event2 = null;
        float[] correlationVector = null;
        Long timestamp = null;
        String index1 = null;
        String index2 = null;
        List<String> tags = new ArrayList<>();
        Long scoreTimestamp = null;

        XContentParserUtils.ensureExpectedToken(XContentParser.Token.START_OBJECT, xcp.nextToken(), xcp);
        while (xcp.nextToken() != XContentParser.Token.END_OBJECT) {
            String fieldName = xcp.currentName();
            xcp.nextToken();

            switch (fieldName) {
                case ROOT_FIELD:
                    isRoot = xcp.booleanValue();
                    break;
                case LEVEL_FIELD:
                    level = xcp.longValue();
                    break;
                case EVENT1_FIELD:
                    event1 = xcp.text();
                    break;
                case EVENT2_FIELD:
                    event2 = xcp.text();
                    break;
                case CORRELATION_VECTOR_FIELD:
                    List<Float> correlationVectorList = new ArrayList<>();
                    XContentParserUtils.ensureExpectedToken(XContentParser.Token.START_ARRAY, xcp.currentToken(), xcp);
                    while (xcp.nextToken() != XContentParser.Token.END_ARRAY) {
                        correlationVectorList.add(xcp.floatValue());
                    }

                    correlationVector = new float[correlationVectorList.size()];
                    for (int idx = 0; idx < correlationVectorList.size(); ++idx) {
                        correlationVector[idx] = correlationVectorList.get(idx);
                    }
                    break;
                case TIMESTAMP_FIELD:
                    timestamp = xcp.longValue();
                    break;
                case INDEX1_FIELD:
                    index1 = xcp.text();
                    break;
                case INDEX2_FIELD:
                    index2 = xcp.text();
                    break;
                case TAGS_FIELD:
                    XContentParserUtils.ensureExpectedToken(XContentParser.Token.START_ARRAY, xcp.currentToken(), xcp);
                    while (xcp.nextToken() != XContentParser.Token.END_ARRAY) {
                        tags.add(xcp.text());
                    }
                    break;
                case SCORE_TIMESTAMP_FIELD:
                    scoreTimestamp = xcp.longValue();
                    break;
                default:
                    xcp.skipChildren();
            }
        }

        return new Correlation(id,
            version,
            isRoot,
            level,
            event1,
            event2,
            correlationVector,
            timestamp,
            index1,
            index2,
            tags,
            scoreTimestamp);
    }

    public static Correlation readFrom(StreamInput sin) throws IOException {
        return new Correlation(sin);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Correlation that = (Correlation) o;
        return id.equals(that.id) && version.equals(that.version) && isRoot.equals(that.isRoot) && level.equals(that.level) && event1.equals(that.event1) && event2.equals(that.event2) && Arrays.equals(correlationVector, that.correlationVector) && timestamp.equals(that.timestamp) && index1.equals(that.index1) && index2.equals(that.index2) && tags.equals(that.tags) && scoreTimestamp.equals(that.scoreTimestamp);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, version, isRoot, level, event1, event2, timestamp, index1, index2, tags, scoreTimestamp);
        result = 31 * result + Arrays.hashCode(correlationVector);
        return result;
    }
}
