/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.correlation.core;

public class CorrelationConstants {
    /**
     * the hyper-parameters for constructing HNSW graphs.
     * https://lucene.apache.org/core/9_4_0/core/org/apache/lucene/util/hnsw/HnswGraph.html
     */
    public static final String METHOD_PARAMETER_M = "m";
    public static final String METHOD_PARAMETER_EF_CONSTRUCTION = "ef_construction";
    /**
     * dimension of the correlation vectors
     */
    public static final String DIMENSION = "dimension";
    public static final String CORRELATION_CONTEXT = "correlation_ctx";
}
