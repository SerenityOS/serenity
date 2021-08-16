/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.imageio.plugins.common.LZWCompressor;
import java.io.IOException;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;

/**
 * LZW Compressor.
 */
public class TIFFLZWCompressor extends TIFFCompressor {

    private final int predictor;

    public TIFFLZWCompressor(int predictorValue) {
        super("LZW", BaselineTIFFTagSet.COMPRESSION_LZW, true);
        this.predictor = predictorValue;
    }

    public void setStream(ImageOutputStream stream) {
        super.setStream(stream);
    }

    public int encode(byte[] b, int off,
                      int width, int height,
                      int[] bitsPerSample,
                      int scanlineStride) throws IOException {

        LZWCompressor lzwCompressor = new LZWCompressor(stream, 8, true);

        int samplesPerPixel = bitsPerSample.length;
        int bitsPerPixel = 0;
        for (int i = 0; i < samplesPerPixel; i++) {
            bitsPerPixel += bitsPerSample[i];
        }
        int bytesPerRow = (bitsPerPixel*width + 7)/8;

        long initialStreamPosition = stream.getStreamPosition();

        boolean usePredictor =
            predictor == BaselineTIFFTagSet.PREDICTOR_HORIZONTAL_DIFFERENCING;

        if(bytesPerRow == scanlineStride && !usePredictor) {
            lzwCompressor.compress(b, off, bytesPerRow*height);
        } else {
            byte[] rowBuf = usePredictor ? new byte[bytesPerRow] : null;
            for(int i = 0; i < height; i++) {
                if(usePredictor) {
                    // Cannot modify b[] in place as it might be a data
                    // array from the image being written so make a copy.
                    System.arraycopy(b, off, rowBuf, 0, bytesPerRow);
                    for(int j = bytesPerRow - 1; j >= samplesPerPixel; j--) {
                        rowBuf[j] -= rowBuf[j - samplesPerPixel];
                    }
                    lzwCompressor.compress(rowBuf, 0, bytesPerRow);
                } else {
                    lzwCompressor.compress(b, off, bytesPerRow);
                }
                off += scanlineStride;
            }
        }

        lzwCompressor.flush();

        int bytesWritten =
            (int)(stream.getStreamPosition() - initialStreamPosition);

        return bytesWritten;
    }
}
