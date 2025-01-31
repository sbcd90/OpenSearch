/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.search.aggregations.metrics;

import org.apache.lucene.index.LeafReaderContext;
import org.opensearch.common.geo.GeoPoint;
import org.opensearch.common.util.BigArrays;
import org.opensearch.common.util.DoubleArray;
import org.opensearch.common.util.LongArray;
import org.opensearch.core.common.lease.Releasables;
import org.opensearch.index.fielddata.MultiGeoPointValues;
import org.opensearch.search.aggregations.Aggregator;
import org.opensearch.search.aggregations.InternalAggregation;
import org.opensearch.search.aggregations.LeafBucketCollector;
import org.opensearch.search.aggregations.LeafBucketCollectorBase;
import org.opensearch.search.aggregations.support.ValuesSource;
import org.opensearch.search.aggregations.support.ValuesSourceConfig;
import org.opensearch.search.internal.SearchContext;

import java.io.IOException;
import java.util.Map;

/**
 * A geo metric aggregator that computes a geo-centroid from a {@code geo_point} type field
 *
 * @opensearch.internal
 */
final class GeoCentroidAggregator extends MetricsAggregator {
    private final ValuesSource.GeoPoint valuesSource;
    private DoubleArray lonSum, lonCompensations, latSum, latCompensations;
    private LongArray counts;

    GeoCentroidAggregator(
        String name,
        ValuesSourceConfig valuesSourceConfig,
        SearchContext context,
        Aggregator parent,
        Map<String, Object> metadata
    ) throws IOException {
        super(name, context, parent, metadata);
        // TODO: Stop expecting nulls here
        this.valuesSource = valuesSourceConfig.hasValues() ? (ValuesSource.GeoPoint) valuesSourceConfig.getValuesSource() : null;
        if (valuesSource != null) {
            final BigArrays bigArrays = context.bigArrays();
            lonSum = bigArrays.newDoubleArray(1, true);
            lonCompensations = bigArrays.newDoubleArray(1, true);
            latSum = bigArrays.newDoubleArray(1, true);
            latCompensations = bigArrays.newDoubleArray(1, true);
            counts = bigArrays.newLongArray(1, true);
        }
    }

    @Override
    public LeafBucketCollector getLeafCollector(LeafReaderContext ctx, LeafBucketCollector sub) throws IOException {
        if (valuesSource == null) {
            return LeafBucketCollector.NO_OP_COLLECTOR;
        }
        final BigArrays bigArrays = context.bigArrays();
        final MultiGeoPointValues values = valuesSource.geoPointValues(ctx);
        final CompensatedSum compensatedSumLat = new CompensatedSum(0, 0);
        final CompensatedSum compensatedSumLon = new CompensatedSum(0, 0);

        return new LeafBucketCollectorBase(sub, values) {
            @Override
            public void collect(int doc, long bucket) throws IOException {
                latSum = bigArrays.grow(latSum, bucket + 1);
                lonSum = bigArrays.grow(lonSum, bucket + 1);
                lonCompensations = bigArrays.grow(lonCompensations, bucket + 1);
                latCompensations = bigArrays.grow(latCompensations, bucket + 1);
                counts = bigArrays.grow(counts, bucket + 1);

                if (values.advanceExact(doc)) {
                    final int valueCount = values.docValueCount();
                    // increment by the number of points for this document
                    counts.increment(bucket, valueCount);
                    // Compute the sum of double values with Kahan summation algorithm which is more
                    // accurate than naive summation.
                    double sumLat = latSum.get(bucket);
                    double compensationLat = latCompensations.get(bucket);
                    double sumLon = lonSum.get(bucket);
                    double compensationLon = lonCompensations.get(bucket);

                    compensatedSumLat.reset(sumLat, compensationLat);
                    compensatedSumLon.reset(sumLon, compensationLon);

                    // update the sum
                    for (int i = 0; i < valueCount; ++i) {
                        GeoPoint value = values.nextValue();
                        // latitude
                        compensatedSumLat.add(value.getLat());
                        // longitude
                        compensatedSumLon.add(value.getLon());
                    }
                    lonSum.set(bucket, compensatedSumLon.value());
                    lonCompensations.set(bucket, compensatedSumLon.delta());
                    latSum.set(bucket, compensatedSumLat.value());
                    latCompensations.set(bucket, compensatedSumLat.delta());
                }
            }
        };
    }

    @Override
    public InternalAggregation buildAggregation(long bucket) {
        if (valuesSource == null || bucket >= counts.size()) {
            return buildEmptyAggregation();
        }
        final long bucketCount = counts.get(bucket);
        final GeoPoint bucketCentroid = (bucketCount > 0)
            ? new GeoPoint(latSum.get(bucket) / bucketCount, lonSum.get(bucket) / bucketCount)
            : null;
        return new InternalGeoCentroid(name, bucketCentroid, bucketCount, metadata());
    }

    @Override
    public InternalAggregation buildEmptyAggregation() {
        return new InternalGeoCentroid(name, null, 0L, metadata());
    }

    @Override
    public void doClose() {
        Releasables.close(latSum, latCompensations, lonSum, lonCompensations, counts);
    }
}
