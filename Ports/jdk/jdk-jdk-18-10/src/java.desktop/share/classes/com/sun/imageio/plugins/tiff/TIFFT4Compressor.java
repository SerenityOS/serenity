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

import javax.imageio.plugins.tiff.BaselineTIFFTagSet;
import javax.imageio.plugins.tiff.TIFFField;
import javax.imageio.plugins.tiff.TIFFTag;
import java.io.IOException;
import javax.imageio.IIOException;
import javax.imageio.metadata.IIOMetadata;

public class TIFFT4Compressor extends TIFFFaxCompressor {

    private boolean is1DMode = false;
    private boolean isEOLAligned = false;

    public TIFFT4Compressor() {
        super("CCITT T.4", BaselineTIFFTagSet.COMPRESSION_CCITT_T_4, true);
    }

    /**
     * Sets the value of the {@code metadata} field.
     *
     * <p> The implementation in this class also sets local options
     * from the T4_OPTIONS field if it exists, and if it doesn't, adds
     * it with default values.</p>
     *
     * @param metadata the {@code IIOMetadata} object for the
     * image being written.
     *
     * @see #getMetadata()
     */
    public void setMetadata(IIOMetadata metadata) {
        super.setMetadata(metadata);

        if (metadata instanceof TIFFImageMetadata) {
            TIFFImageMetadata tim = (TIFFImageMetadata)metadata;
            TIFFField f = tim.getTIFFField(BaselineTIFFTagSet.TAG_T4_OPTIONS);
            if (f != null) {
                int options = f.getAsInt(0);
                is1DMode = (options & 0x1) == 0;
                isEOLAligned = (options & 0x4) == 0x4;
            } else {
                long[] oarray = new long[1];
                oarray[0] = (isEOLAligned ? 0x4 : 0x0) |
                    (is1DMode ? 0x0 : 0x1);

                BaselineTIFFTagSet base = BaselineTIFFTagSet.getInstance();
                TIFFField T4Options =
                  new TIFFField(base.getTag(BaselineTIFFTagSet.TAG_T4_OPTIONS),
                                TIFFTag.TIFF_LONG,
                                1,
                                oarray);
                tim.rootIFD.addTIFFField(T4Options);
            }
        }
    }

    /**
     * Encode a buffer of data using CCITT T.4 Compression also known as
     * Group 3 facsimile compression.
     *
     * @param is1DMode     Whether to perform one-dimensional encoding.
     * @param isEOLAligned Whether EOL bit sequences should be padded.
     * @param data         The row of data to compress.
     * @param lineStride   Byte step between the same sample in different rows.
     * @param colOffset    Bit offset within first {@code data[rowOffset]}.
     * @param width        Number of bits in the row.
     * @param height       Number of rows in the buffer.
     * @param compData     The compressed data.
     *
     * @return The number of bytes saved in the compressed data array.
     */
    public int encodeT4(boolean is1DMode,
                        boolean isEOLAligned,
                        byte[] data,
                        int lineStride,
                        int colOffset,
                        int width,
                        int height,
                        byte[] compData)
    {
        //
        // ao, a1, a2 are bit indices in the current line
        // b1 and b2  are bit indices in the reference line (line above)
        // color is the current color (WHITE or BLACK)
        //
        byte[] refData = data;
        int lineAddr = 0;
        int outIndex = 0;

        initBitBuf();

        int KParameter = 2;
        for(int numRows = 0; numRows < height; numRows++) {
            if(is1DMode || (numRows % KParameter) == 0) { // 1D encoding
                // Write EOL+1
                outIndex += addEOL(is1DMode, isEOLAligned, true,
                                   compData, outIndex);

                // Encode row
                outIndex += encode1D(data, lineAddr, colOffset, width,
                                      compData, outIndex);
            } else { // 2D encoding.
                // Write EOL+0
                outIndex += addEOL(is1DMode, isEOLAligned, false,
                                   compData, outIndex);

                // Set reference to previous line
                int refAddr = lineAddr - lineStride;

                // Encode row
                int a0   = colOffset;
                int last = a0 + width;

                int testbit =
                    ((data[lineAddr + (a0>>>3)]&0xff) >>>
                     (7-(a0 & 0x7))) & 0x1;
                int a1 = testbit != 0 ?
                    a0 : nextState(data, lineAddr, a0, last);

                testbit = ((refData[refAddr + (a0>>>3)]&0xff) >>>
                           (7-(a0 & 0x7))) & 0x1;
                int b1 = testbit != 0 ?
                    a0 : nextState(refData, refAddr, a0, last);

                // The current color is set to WHITE at line start
                int color = WHITE;

                while(true) {
                    int b2 = nextState(refData, refAddr, b1, last);
                    if(b2 < a1) {          // pass mode
                        outIndex += add2DBits(compData, outIndex, pass, 0);
                        a0 = b2;
                    } else {
                        int tmp = b1 - a1 + 3;
                        if((tmp <= 6) && (tmp >= 0)) { // vertical mode
                            outIndex +=
                                add2DBits(compData, outIndex, vert, tmp);
                            a0 = a1;
                        } else {            // horizontal mode
                            int a2 = nextState(data, lineAddr, a1, last);
                            outIndex +=
                                add2DBits(compData, outIndex, horz, 0);
                            outIndex +=
                                add1DBits(compData, outIndex, a1-a0, color);
                            outIndex +=
                                add1DBits(compData, outIndex, a2-a1, color^1);
                            a0 = a2;
                        }
                    }
                    if(a0 >= last) {
                        break;
                    }
                    color = ((data[lineAddr + (a0>>>3)]&0xff) >>>
                             (7-(a0 & 0x7))) & 0x1;
                    a1 = nextState(data, lineAddr, a0, last);
                    b1 = nextState(refData, refAddr, a0, last);
                    testbit = ((refData[refAddr + (b1>>>3)]&0xff) >>>
                               (7-(b1 & 0x7))) & 0x1;
                    if(testbit == color) {
                        b1 = nextState(refData, refAddr, b1, last);
                    }
                }
            }

            // Skip to next line.
            lineAddr += lineStride;
        }

        for(int i = 0; i < 6; i++) {
            outIndex += addEOL(is1DMode, isEOLAligned, true,
                               compData, outIndex);
        }

        //
        // flush all pending bits
        //
        while(ndex > 0) {
            compData[outIndex++] = (byte)(bits >>> 24);
            bits <<= 8;
            ndex -= 8;
        }

        // Flip the bytes if inverse fill was requested.
        if(inverseFill) {
            for(int i = 0; i < outIndex; i++) {
                compData[i] = TIFFFaxDecompressor.flipTable[compData[i]&0xff];
            }
        }

        return outIndex;
    }

    public int encode(byte[] b, int off,
                      int width, int height,
                      int[] bitsPerSample,
                      int scanlineStride) throws IOException {
        if (bitsPerSample.length != 1 || bitsPerSample[0] != 1) {
            throw new IIOException(
                             "Bits per sample must be 1 for T4 compression!");
        }

        // This initial buffer size is based on an alternating 1-0
        // pattern generating the most bits when converted to code
        // words: 9 bits out for each pair of bits in. So the number
        // of bit pairs is determined, multiplied by 9, converted to
        // bytes, and a ceil() is taken to account for fill bits at the
        // end of each line.  The "2" addend accounts for the case
        // of the pattern beginning with black.  The buffer is intended
        // to hold only a single row.

        int maxBits = 9*((width + 1)/2) + 2;
        int bufSize = (maxBits + 7)/8;

        // Calculate the maximum row as the G3-1D size plus the EOL,
        // multiply this by the number of rows in the tile, and add
        // 6 EOLs for the RTC (return to control).
        bufSize = height*(bufSize + 2) + 12;

        byte[] compData = new byte[bufSize];

        int bytes = encodeT4(is1DMode,
                             isEOLAligned,
                             b, scanlineStride, 8*off,
                             width, height,
                             compData);

        stream.write(compData, 0, bytes);
        return bytes;
    }
}
