/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.correlation.core.index.serializer;

import org.opensearch.ExceptionsHelper;
import org.opensearch.common.io.stream.BytesStreamInput;
import org.opensearch.common.io.stream.BytesStreamOutput;

import java.io.IOException;

/**
 * CorrelationVectorSerializer class to do serde operation of converting float vectors to byte array and vice versa.
 *
 * @opensearch.internal
 */
public class CorrelationVectorSerializer {

    /**
     * converts float array based vector to byte array.
     * @param input float array
     * @return byte array
     */
    public static byte[] floatToByteArray(float[] input) {
        byte[] bytes;
        try (BytesStreamOutput objectStream = new BytesStreamOutput()) {
            objectStream.writeFloatArray(input);
            bytes = objectStream.bytes().toBytesRef().bytes;
        } catch (IOException ex) {
            throw ExceptionsHelper.convertToOpenSearchException(ex);
        }
        return bytes;
    }

    /**
     * converts byte array to float array
     * @param byteStream byte array input stream
     * @return float array
     */
    public static float[] byteToFloatArray(byte[] byteStream) {
        try (BytesStreamInput objectStream = new BytesStreamInput(byteStream)) {
            return objectStream.readFloatArray();
        } catch (IOException ex) {
            throw ExceptionsHelper.convertToOpenSearchException(ex);
        }
    }
}
