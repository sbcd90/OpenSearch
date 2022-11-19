/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.index.search;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.opensearch.index.query.QueryShardContext;

import java.util.HashMap;
import java.util.Map;

public class QueryStringQueryParserExt extends QueryStringQueryParser {

    private Map<String, Object> fields;

    public QueryStringQueryParserExt(QueryShardContext context, boolean lenient) {
        super(context, lenient);
        this.fields = new HashMap<>();
    }

    @Override
    public Query getFieldQuery(String field, String queryText, boolean quoted) throws ParseException {
        if (!field.equals("*")) {
            fields.put(field, queryText);
        }
        return super.getFieldQuery(field, queryText, quoted);
    }

    public Map<String, Object> getFields() {
        return fields;
    }
}
