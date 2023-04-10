/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.correlation.core.index.serializer;

import org.opensearch.ExceptionsHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class CorrelationVectorSerializer {

    public byte[] floatToByteArray(float[] input) {
        byte[] bytes;
        try(
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
        ) {
            objectStream.writeObject(input);
            bytes = byteStream.toByteArray();
        } catch (IOException ex) {
            throw ExceptionsHelper.convertToOpenSearchException(ex);
        }
        return bytes;
    }

    public float[] byteToFloatArray(ByteArrayInputStream byteStream) {
        try {
            ObjectInputStream objectStream  = new ObjectInputStream(byteStream);
            return (float[]) objectStream.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            throw ExceptionsHelper.convertToOpenSearchException(ex);
        }
    }
}
