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

public class TIFFT6Compressor extends TIFFFaxCompressor {

    public TIFFT6Compressor() {
        super("CCITT T.6", BaselineTIFFTagSet.COMPRESSION_CCITT_T_6, true);
    }

    /**
     * Encode a buffer of data using CCITT T.6 Compression also known as
     * Group 4 facsimile compression.
     *
     * @param data        The row of data to compress.
     * @param lineStride  Byte step between the same sample in different rows.
     * @param colOffset   Bit offset within first {@code data[rowOffset]}.
     * @param width       Number of bits in the row.
     * @param height      Number of rows in the buffer.
     * @param compData    The compressed data.
     *
     * @return The number of bytes saved in the compressed data array.
     */
    public synchronized int encodeT6(byte[] data,
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
        byte[] refData = null;
        int refAddr  = 0;
        int lineAddr = 0;
        int  outIndex = 0;

        initBitBuf();

        //
        // Iterate over all lines
        //
        while(height-- != 0) {
            int a0   = colOffset;
            int last = a0 + width;

            int testbit =
                ((data[lineAddr + (a0>>>3)]&0xff) >>>
                 (7-(a0 & 0x7))) & 0x1;
            int a1 = testbit != 0 ?
                a0 : nextState(data, lineAddr, a0, last);

            testbit = refData == null ?
                0: ((refData[refAddr + (a0>>>3)]&0xff) >>>
                       (7-(a0 & 0x7))) & 0x1;
            int b1 = testbit != 0 ?
                a0 : nextState(refData, refAddr, a0, last);

            //
            // The current color is set to WHITE at line start
            //
            int color = WHITE;

            while(true) {
                int b2 = nextState(refData, refAddr, b1, last);
                if(b2 < a1) {          // pass mode
                    outIndex += add2DBits(compData, outIndex, pass, 0);
                    a0 = b2;
                } else {
                    int tmp = b1 - a1 + 3;
                    if((tmp <= 6) && (tmp >= 0)) { // vertical mode
                        outIndex += add2DBits(compData, outIndex, vert, tmp);
                        a0 = a1;
                    } else {            // horizontal mode
                        int a2 = nextState(data, lineAddr, a1, last);
                        outIndex += add2DBits(compData, outIndex, horz, 0);
                        outIndex += add1DBits(compData, outIndex, a1-a0, color);
                        outIndex += add1DBits(compData, outIndex, a2-a1, color^1);
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
                testbit = refData == null ?
                    0: ((refData[refAddr + (b1>>>3)]&0xff) >>>
                           (7-(b1 & 0x7))) & 0x1;
                if(testbit == color) {
                    b1 = nextState(refData, refAddr, b1, last);
                }
            }

            refData = data;
            refAddr = lineAddr;
            lineAddr += lineStride;

        } // End while(height--)

        //
        // append eofb
        //
        outIndex += addEOFB(compData, outIndex);

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
                             "Bits per sample must be 1 for T6 compression!");
        }


        if (metadata instanceof TIFFImageMetadata) {
            TIFFImageMetadata tim = (TIFFImageMetadata)metadata;

            long[] options = new long[1];
            options[0] = 0;

            BaselineTIFFTagSet base = BaselineTIFFTagSet.getInstance();
            TIFFField T6Options =
                new TIFFField(base.getTag(BaselineTIFFTagSet.TAG_T6_OPTIONS),
                              TIFFTag.TIFF_LONG,
                              1,
                              options);
            tim.rootIFD.addTIFFField(T6Options);
        }

        // See comment in TIFFT4Compressor
        int maxBits = 9*((width + 1)/2) + 2;
        int bufSize = (maxBits + 7)/8;
        bufSize = height*(bufSize + 2) + 12;

        byte[] compData = new byte[bufSize];
        int bytes = encodeT6(b, scanlineStride, 8*off, width, height,
                             compData);
        stream.write(compData, 0, bytes);
        return bytes;
    }
}
