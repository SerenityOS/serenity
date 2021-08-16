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

import java.io.EOFException;
import java.io.IOException;

public class TIFFNullDecompressor extends TIFFDecompressor {

    /**
     * Whether to read the active source region only.
     */
    private boolean isReadActiveOnly = false;

    /** The original value of {@code srcMinX}. */
    private int originalSrcMinX;

    /** The original value of {@code srcMinY}. */
    private int originalSrcMinY;

    /** The original value of {@code srcWidth}. */
    private int originalSrcWidth;

    /** The original value of {@code srcHeight}. */
    private int originalSrcHeight;

    public TIFFNullDecompressor() {}

    //
    // This approach to reading the active region is a not the best
    // as the original values of the entire source region are stored,
    // overwritten, and then restored. It would probably be better to
    // revise TIFFDecompressor such that this were not necessary, i.e.,
    // change beginDecoding() and decode() to use the active region values
    // when random access is easy and the entire region values otherwise.
    //
    public void beginDecoding() {
        // Determine number of bits per pixel.
        int bitsPerPixel = 0;
        for(int i = 0; i < bitsPerSample.length; i++) {
            bitsPerPixel += bitsPerSample[i];
        }

        // Can read active region only if row starts on a byte boundary.
        if((activeSrcMinX != srcMinX || activeSrcMinY != srcMinY ||
            activeSrcWidth != srcWidth || activeSrcHeight != srcHeight) &&
           ((activeSrcMinX - srcMinX)*bitsPerPixel) % 8 == 0) {
            // Set flag.
            isReadActiveOnly = true;

            // Cache original region.
            originalSrcMinX = srcMinX;
            originalSrcMinY = srcMinY;
            originalSrcWidth = srcWidth;
            originalSrcHeight = srcHeight;

            // Replace region with active region.
            srcMinX = activeSrcMinX;
            srcMinY = activeSrcMinY;
            srcWidth = activeSrcWidth;
            srcHeight = activeSrcHeight;
        } else {
            // Clear flag.
            isReadActiveOnly = false;
        }

        super.beginDecoding();
    }

    public void decode() throws IOException {
        super.decode();

        // Reset state.
        if(isReadActiveOnly) {
            // Restore original source region values.
            srcMinX = originalSrcMinX;
            srcMinY = originalSrcMinY;
            srcWidth = originalSrcWidth;
            srcHeight = originalSrcHeight;

            // Unset flag.
            isReadActiveOnly = false;
        }
    }

    public void decodeRaw(byte[] b,
                          int dstOffset,
                          int bitsPerPixel,
                          int scanlineStride) throws IOException {
        if(isReadActiveOnly) {
            // Read the active source region only.

            int activeBytesPerRow = (activeSrcWidth*bitsPerPixel + 7)/8;
            int totalBytesPerRow = (originalSrcWidth*bitsPerPixel + 7)/8;
            int bytesToSkipPerRow = totalBytesPerRow - activeBytesPerRow;

            //
            // Seek to the start of the active region:
            //
            // active offset = original offset +
            //                 number of bytes to start of first active row +
            //                 number of bytes to first active pixel within row
            //
            // Since the condition for reading from the active region only is
            //
            //     ((activeSrcMinX - srcMinX)*bitsPerPixel) % 8 == 0
            //
            // the bit offset to the first active pixel within the first
            // active row is a multiple of 8.
            //
            stream.seek(offset +
                        (activeSrcMinY - originalSrcMinY)*totalBytesPerRow +
                        ((activeSrcMinX - originalSrcMinX)*bitsPerPixel)/8);

            int lastRow = activeSrcHeight - 1;
            for (int y = 0; y < activeSrcHeight; y++) {
                int bytesRead = stream.read(b, dstOffset, activeBytesPerRow);
                if (bytesRead < 0) {
                    throw new EOFException();
                } else if (bytesRead != activeBytesPerRow) {
                    break;
                }
                dstOffset += scanlineStride;

                // Skip unneeded bytes (row suffix + row prefix).
                if(y != lastRow) {
                    stream.skipBytes(bytesToSkipPerRow);
                }
            }
        } else {
            // Read the entire source region.
            stream.seek(offset);
            int bytesPerRow = (srcWidth*bitsPerPixel + 7)/8;
            if(bytesPerRow == scanlineStride) {
                if (stream.read(b, dstOffset, bytesPerRow*srcHeight) < 0) {
                    throw new EOFException();
                }
            } else {
                for (int y = 0; y < srcHeight; y++) {
                    int bytesRead = stream.read(b, dstOffset, bytesPerRow);
                    if (bytesRead < 0) {
                        throw new EOFException();
                    } else if (bytesRead != bytesPerRow) {
                        break;
                    }
                    dstOffset += scanlineStride;
                }
            }
        }
    }
}
