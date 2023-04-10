/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.plugin.correlation.core.index.codec.correlation950;

import org.apache.lucene.codecs.lucene95.Lucene95HnswVectorsFormat;
import org.opensearch.index.mapper.MapperService;
import org.opensearch.plugin.correlation.core.index.codec.BasePerFieldCorrelationVectorsFormat;

import java.util.Optional;

public class PerFieldCorrelationVectorsFormat extends BasePerFieldCorrelationVectorsFormat {

    public PerFieldCorrelationVectorsFormat(final Optional<MapperService> mapperService) {
        super(
            mapperService,
            Lucene95HnswVectorsFormat.DEFAULT_MAX_CONN,
            Lucene95HnswVectorsFormat.DEFAULT_BEAM_WIDTH,
            () -> new Lucene95HnswVectorsFormat(),
            (maxConn, beamWidth) -> new Lucene95HnswVectorsFormat(maxConn, beamWidth)
        );
    }
}
