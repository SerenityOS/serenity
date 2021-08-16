/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javax.sound.sampled.spi;

import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import static javax.sound.sampled.AudioFormat.Encoding;

/**
 * A format conversion provider provides format conversion services from one or
 * more input formats to one or more output formats. Converters include codecs,
 * which encode and/or decode audio data, as well as transcoders, etc. Format
 * converters provide methods for determining what conversions are supported and
 * for obtaining an audio stream from which converted data can be read.
 * <p>
 * The source format represents the format of the incoming audio data, which
 * will be converted.
 * <p>
 * The target format represents the format of the processed, converted audio
 * data. This is the format of the data that can be read from the stream
 * returned by one of the {@code getAudioInputStream} methods.
 *
 * @author Kara Kytle
 * @since 1.3
 */
public abstract class FormatConversionProvider {

    /**
     * Constructor for subclasses to call.
     */
    protected FormatConversionProvider() {}

    /**
     * Obtains the set of source format encodings from which format conversion
     * services are provided by this provider.
     *
     * @return array of source format encodings. If for some reason provider
     *         does not provide any conversion services, an array of length 0 is
     *         returned.
     */
    public abstract Encoding[] getSourceEncodings();

    /**
     * Obtains the set of target format encodings to which format conversion
     * services are provided by this provider.
     *
     * @return array of target format encodings. If for some reason provider
     *         does not provide any conversion services, an array of length 0 is
     *         returned.
     */
    public abstract Encoding[] getTargetEncodings();

    /**
     * Indicates whether the format converter supports conversion from the
     * specified source format encoding.
     *
     * @param  sourceEncoding the source format encoding for which support is
     *         queried
     * @return {@code true} if the encoding is supported, otherwise
     *         {@code false}
     * @throws NullPointerException if {@code sourceEncoding} is {@code null}
     */
    public boolean isSourceEncodingSupported(final Encoding sourceEncoding) {
        return Arrays.stream(getSourceEncodings())
                     .anyMatch(sourceEncoding::equals);
    }

    /**
     * Indicates whether the format converter supports conversion to the
     * specified target format encoding.
     *
     * @param  targetEncoding the target format encoding for which support is
     *         queried
     * @return {@code true} if the encoding is supported, otherwise
     *         {@code false}
     * @throws NullPointerException if {@code targetEncoding} is {@code null}
     */
    public boolean isTargetEncodingSupported(final Encoding targetEncoding) {
        return Arrays.stream(getTargetEncodings())
                     .anyMatch(targetEncoding::equals);
    }

    /**
     * Obtains the set of target format encodings supported by the format
     * converter given a particular source format. If no target format encodings
     * are supported for this source format, an array of length 0 is returned.
     *
     * @param  sourceFormat format of the incoming data
     * @return array of supported target format encodings
     * @throws NullPointerException if {@code sourceFormat} is {@code null}
     */
    public abstract Encoding[] getTargetEncodings(AudioFormat sourceFormat);

    /**
     * Indicates whether the format converter supports conversion to a
     * particular encoding from a particular format.
     *
     * @param  targetEncoding desired encoding of the outgoing data
     * @param  sourceFormat format of the incoming data
     * @return {@code true} if the conversion is supported, otherwise
     *         {@code false}
     * @throws NullPointerException if {@code targetEncoding} or
     *         {@code sourceFormat} are {@code null}
     */
    public boolean isConversionSupported(final Encoding targetEncoding,
                                         final AudioFormat sourceFormat) {
        return Arrays.stream(getTargetEncodings(sourceFormat))
                     .anyMatch(targetEncoding::equals);
    }

    /**
     * Obtains the set of target formats with the encoding specified supported
     * by the format converter. If no target formats with the specified encoding
     * are supported for this source format, an array of length 0 is returned.
     *
     * @param  targetEncoding desired encoding of the stream after processing
     * @param  sourceFormat format of the incoming data
     * @return array of supported target formats
     * @throws NullPointerException if {@code targetEncoding} or
     *         {@code sourceFormat} are {@code null}
     */
    public abstract AudioFormat[] getTargetFormats(Encoding targetEncoding,
                                                   AudioFormat sourceFormat);

    /**
     * Indicates whether the format converter supports conversion to one
     * particular format from another.
     *
     * @param  targetFormat desired format of outgoing data
     * @param  sourceFormat format of the incoming data
     * @return {@code true} if the conversion is supported, otherwise
     *         {@code false}
     * @throws NullPointerException if {@code targetFormat} or
     *         {@code sourceFormat} are {@code null}
     */
    public boolean isConversionSupported(final AudioFormat targetFormat,
                                         final AudioFormat sourceFormat) {
        final Encoding targetEncoding = targetFormat.getEncoding();
        return Arrays.stream(getTargetFormats(targetEncoding, sourceFormat))
                     .anyMatch(targetFormat::matches);
    }

    /**
     * Obtains an audio input stream with the specified encoding from the given
     * audio input stream.
     *
     * @param  targetEncoding desired encoding of the stream after processing
     * @param  sourceStream stream from which data to be processed should be
     *         read
     * @return stream from which processed data with the specified target
     *         encoding may be read
     * @throws IllegalArgumentException if the format combination supplied is
     *         not supported
     * @throws NullPointerException if {@code targetEncoding} or
     *         {@code sourceStream} are {@code null}
     */
    public abstract AudioInputStream getAudioInputStream(
            Encoding targetEncoding, AudioInputStream sourceStream);

    /**
     * Obtains an audio input stream with the specified format from the given
     * audio input stream.
     *
     * @param  targetFormat desired data format of the stream after processing
     * @param  sourceStream stream from which data to be processed should be
     *         read
     * @return stream from which processed data with the specified format may be
     *         read
     * @throws IllegalArgumentException if the format combination supplied is
     *         not supported
     * @throws NullPointerException if {@code targetFormat} or
     *         {@code sourceStream} are {@code null}
     */
    public abstract AudioInputStream getAudioInputStream(
            AudioFormat targetFormat, AudioInputStream sourceStream);
}
