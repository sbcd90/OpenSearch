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

public class VectorField extends Field {

    public VectorField(String name, float[] value, IndexableFieldType type) {
        super(name, new BytesRef(), type);
        try {
            final CorrelationVectorSerializer vectorSerializer = new CorrelationVectorSerializer();
            final byte[] floatToByte = vectorSerializer.floatToByteArray(value);
            this.setBytesValue(floatToByte);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
