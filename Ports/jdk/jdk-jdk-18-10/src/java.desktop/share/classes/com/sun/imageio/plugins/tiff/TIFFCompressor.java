/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.imageio.plugins.tiff;

import java.io.IOException;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;

/**
 * An abstract superclass for pluggable TIFF compressors.
 */
public abstract class TIFFCompressor {

    /**
     * The {@code ImageWriter} calling this
     * {@code TIFFCompressor}.
     */
    protected ImageWriter writer;

    /**
     * The {@code IIOMetadata} object containing metadata for the
     * current image.
     */
    protected IIOMetadata metadata;

    /**
     * The name of the compression type supported by this compressor.
     */
    protected String compressionType;

    /**
     * The value to be assigned to the TIFF <i>Compression</i> tag in the
     * TIFF image metadata.
     */
    protected int compressionTagValue;

    /**
     * Whether the compression is lossless.
     */
    protected boolean isCompressionLossless;

    /**
     * The {@code ImageOutputStream} to be written.
     */
    protected ImageOutputStream stream;

    /**
     * Creates a compressor object for use in compressing TIFF data.
     *
     * <p>The parameters {@code compressionTagValue} and
     * {@code isCompressionLossless} are provided to accomodate
     * compression types which are unknown. A compression type is
     * "known" if it is either among those already supported by the
     * TIFF writer (see {@link TIFFImageWriteParam}), or is listed in
     * the TIFF 6.0 specification but not supported. If the compression
     * type is unknown, the {@code compressionTagValue} and
     * {@code isCompressionLossless} parameters are ignored.</p>
     *
     * @param compressionType The name of the compression type.
     * @param compressionTagValue The value to be assigned to the TIFF
     * <i>Compression</i> tag in the TIFF image metadata; ignored if
     * {@code compressionType} is a known type.
     * @param isCompressionLossless Whether the compression is lossless;
     * ignored if {@code compressionType} is a known type.
     *
     * @throws NullPointerException if {@code compressionType} is
     * {@code null}.
     * @throws IllegalArgumentException if {@code compressionTagValue} is
     * less {@code 1}.
     */
    public TIFFCompressor(String compressionType,
                          int compressionTagValue,
                          boolean isCompressionLossless) {
        if(compressionType == null) {
            throw new NullPointerException("compressionType == null");
        } else if(compressionTagValue < 1) {
            throw new IllegalArgumentException("compressionTagValue < 1");
        }

        // Set the compression type.
        this.compressionType = compressionType;

        // Determine whether this type is either defined in the TIFF 6.0
        // specification or is already supported.
        int compressionIndex = -1;
        String[] compressionTypes = TIFFImageWriter.compressionTypes;
        int len = compressionTypes.length;
        for(int i = 0; i < len; i++) {
            if(compressionTypes[i].equals(compressionType)) {
                // Save the index of the supported type.
                compressionIndex = i;
                break;
            }
        }

        if(compressionIndex != -1) {
            // Known compression type.
            this.compressionTagValue =
                TIFFImageWriter.compressionNumbers[compressionIndex];
            this.isCompressionLossless =
                TIFFImageWriter.isCompressionLossless[compressionIndex];
        } else {
            // Unknown compression type.
            this.compressionTagValue = compressionTagValue;
            this.isCompressionLossless = isCompressionLossless;
        }
    }

    /**
     * Retrieve the name of the compression type supported by this compressor.
     *
     * @return The compression type name.
     */
    public String getCompressionType() {
        return compressionType;
    }

    /**
     * Retrieve the value to be assigned to the TIFF <i>Compression</i> tag
     * in the TIFF image metadata.
     *
     * @return The <i>Compression</i> tag value.
     */
    public int getCompressionTagValue() {
        return compressionTagValue;
    }

    /**
     * Retrieves a value indicating whether the compression is lossless.
     *
     * @return Whether the compression is lossless.
     */
    public boolean isCompressionLossless() {
        return isCompressionLossless;
    }

    /**
     * Sets the {@code ImageOutputStream} to be written.
     *
     * @param stream an {@code ImageOutputStream} to be written.
     *
     * @see #getStream
     */
    public void setStream(ImageOutputStream stream) {
        this.stream = stream;
    }

    /**
     * Returns the {@code ImageOutputStream} that will be written.
     *
     * @return an {@code ImageOutputStream}.
     *
     * @see #setStream(ImageOutputStream)
     */
    public ImageOutputStream getStream() {
        return stream;
    }

    /**
     * Sets the value of the {@code writer} field.
     *
     * @param writer the current {@code ImageWriter}.
     *
     * @see #getWriter()
     */
    public void setWriter(ImageWriter writer) {
        this.writer = writer;
    }

    /**
     * Returns the current {@code ImageWriter}.
     *
     * @return an {@code ImageWriter}.
     *
     * @see #setWriter(ImageWriter)
     */
    public ImageWriter getWriter() {
        return this.writer;
    }

    /**
     * Sets the value of the {@code metadata} field.
     *
     * @param metadata the {@code IIOMetadata} object for the
     * image being written.
     *
     * @see #getMetadata()
     */
    public void setMetadata(IIOMetadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Returns the current {@code IIOMetadata} object.
     *
     * @return the {@code IIOMetadata} object for the image being
     * written.
     *
     * @see #setMetadata(IIOMetadata)
     */
    public IIOMetadata getMetadata() {
        return this.metadata;
    }

    /**
     * Encodes the supplied image data, writing to the currently set
     * {@code ImageOutputStream}.
     *
     * @param b an array of {@code byte}s containing the packed
     * but uncompressed image data.
     * @param off the starting offset of the data to be written in the
     * array {@code b}.
     * @param width the width of the rectangle of pixels to be written.
     * @param height the height of the rectangle of pixels to be written.
     * @param bitsPerSample an array of {@code int}s indicting
     * the number of bits used to represent each image sample within
     * a pixel.
     * @param scanlineStride the number of bytes separating each
     * row of the input data.
     *
     * @return the number of bytes written.
     *
     * @throws IOException if the supplied data cannot be encoded by
     * this {@code TIFFCompressor}, or if any I/O error occurs
     * during writing.
     */
    public abstract int encode(byte[] b, int off,
                               int width, int height,
                               int[] bitsPerSample,
                               int scanlineStride) throws IOException;

}
