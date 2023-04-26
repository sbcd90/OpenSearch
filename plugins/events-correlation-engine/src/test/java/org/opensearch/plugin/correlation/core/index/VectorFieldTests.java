/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.correlation.core.index;

import org.apache.lucene.document.FieldType;
import org.junit.Assert;
import org.opensearch.test.OpenSearchTestCase;

/**
 * Unit tests for VectorField
 */
public class VectorFieldTests extends OpenSearchTestCase {

    /**
     * test VectorField ctor
     */
    public void testVectorField_ctor() {
        VectorField field = new VectorField("test-field", new float[] { 1.0f, 1.0f }, new FieldType());
        Assert.assertEquals("test-field", field.name());
    }
}
