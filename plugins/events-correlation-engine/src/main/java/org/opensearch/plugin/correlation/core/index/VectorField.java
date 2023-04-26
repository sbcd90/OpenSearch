/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.correlation.core.index;

import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.util.BytesRef;
import org.opensearch.plugin.correlation.core.index.serializer.CorrelationVectorSerializer;

/**
 * Generic Vector Field defining a correlation vector name, float array.
 *
 * @opensearch.internal
 */
public class VectorField extends Field {

    /**
     * Parameterized ctor for VectorField
     * @param name name of the field
     * @param value float array value for the field
     * @param type type of the field
     */
    public VectorField(String name, float[] value, IndexableFieldType type) {
        super(name, new BytesRef(), type);
        try {
            final byte[] floatToByte = CorrelationVectorSerializer.floatToByteArray(value);
            this.setBytesValue(floatToByte);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
