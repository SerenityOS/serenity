/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
import javax.imageio.IIOException;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;

class TIFFLZWDecompressor extends TIFFDecompressor {

    private static final int CLEAR_CODE = 256;
    private static final int EOI_CODE   = 257;
    private static final int FIRST_CODE = 258;

    private static final int[] andTable = {
        511,
        1023,
        2047,
        4095
    };

    private int predictor;

    // whether to reverse the bits in each byte of the input data, i.e.,
    // convert right-to-left fill order (lsb) to left-to-right (msb).
    private boolean flipBits;

    private byte[] srcData;
    private byte[] dstData;

    private int srcIndex;
    private int dstIndex;

    private byte[][] stringTable;
    private int tableIndex, bitsToGet = 9;

    private int nextData = 0;
    private int nextBits = 0;

    public TIFFLZWDecompressor(int predictor, int fillOrder)
        throws IIOException {
        super();

        if (predictor != BaselineTIFFTagSet.PREDICTOR_NONE &&
            predictor !=
            BaselineTIFFTagSet.PREDICTOR_HORIZONTAL_DIFFERENCING) {
            throw new IIOException("Illegal value for Predictor in " +
                                   "TIFF file");
        }

        this.predictor = predictor;

        flipBits = fillOrder == BaselineTIFFTagSet.FILL_ORDER_RIGHT_TO_LEFT;
    }

    public void decodeRaw(byte[] b,
                          int dstOffset,
                          int bitsPerPixel,
                          int scanlineStride) throws IOException {

        // Check bitsPerSample.
        if (predictor ==
            BaselineTIFFTagSet.PREDICTOR_HORIZONTAL_DIFFERENCING) {
            int len = bitsPerSample.length;
            for(int i = 0; i < len; i++) {
                if(bitsPerSample[i] != 8) {
                    throw new IIOException
                        (bitsPerSample[i] + "-bit samples "+
                         "are not supported for Horizontal "+
                         "differencing Predictor");
                }
            }
        }

        stream.seek(offset);

        byte[] sdata = new byte[byteCount];
        stream.readFully(sdata);

        if (flipBits) {
            for (int i = 0; i < byteCount; i++) {
                sdata[i] = TIFFFaxDecompressor.flipTable[sdata[i] & 0xff];
            }
        }

        int bytesPerRow = (srcWidth*bitsPerPixel + 7)/8;
        byte[] buf;
        int bufOffset;
        if(bytesPerRow == scanlineStride) {
            buf = b;
            bufOffset = dstOffset;
        } else {
            buf = new byte[bytesPerRow*srcHeight];
            bufOffset = 0;
        }

        int numBytesDecoded = decode(sdata, 0, buf, bufOffset);

        if(bytesPerRow != scanlineStride) {
            int off = 0;
            for (int y = 0; y < srcHeight; y++) {
                System.arraycopy(buf, off, b, dstOffset, bytesPerRow);
                off += bytesPerRow;
                dstOffset += scanlineStride;
            }
        }
    }

    public int decode(byte[] sdata, int srcOffset,
                      byte[] ddata, int dstOffset)
        throws IOException {
        if (sdata[0] == (byte)0x00 && sdata[1] == (byte)0x01) {
            throw new IIOException
                ("TIFF 5.0-style LZW compression is not supported!");
        }

        this.srcData = sdata;
        this.dstData = ddata;

        this.srcIndex = srcOffset;
        this.dstIndex = dstOffset;

        this.nextData = 0;
        this.nextBits = 0;

        initializeStringTable();

        int code, oldCode = 0;
        byte[] string;

        while ((code = getNextCode()) != EOI_CODE) {
            if (code == CLEAR_CODE) {
                initializeStringTable();
                code = getNextCode();
                if (code == EOI_CODE) {
                    break;
                }

                writeString(stringTable[code]);
                oldCode = code;
            } else {
                if (code < tableIndex) {
                    string = stringTable[code];

                    writeString(string);
                    addStringToTable(stringTable[oldCode], string[0]);
                    oldCode = code;
                } else {
                    string = stringTable[oldCode];
                    string = composeString(string, string[0]);
                    writeString(string);
                    addStringToTable(string);
                    oldCode = code;
                }
            }
        }

        if (predictor ==
            BaselineTIFFTagSet.PREDICTOR_HORIZONTAL_DIFFERENCING) {
            int step = planar || samplesPerPixel == 1 ? 1 : samplesPerPixel;

            int samplesPerRow = step * srcWidth;

            int off = dstOffset + step;
            for (int j = 0; j < srcHeight; j++) {
                int count = off;
                for (int i = step; i < samplesPerRow; i++) {
                    dstData[count] += dstData[count - step];
                    count++;
                }
                off += samplesPerRow;
            }
        }

        return dstIndex - dstOffset;
    }

    /**
     * Initialize the string table.
     */
    public void initializeStringTable() {
        stringTable = new byte[4096][];

        for (int i = 0; i < CLEAR_CODE; i++) {
            stringTable[i] = new byte[1];
            stringTable[i][0] = (byte)i;
        }

        tableIndex = FIRST_CODE;
        bitsToGet = 9;
    }

    /**
     * Write out the string just uncompressed.
     */
    public void writeString(byte[] string) {
        if(dstIndex < dstData.length) {
            int maxIndex = Math.min(string.length,
                                    dstData.length - dstIndex);

            for (int i=0; i < maxIndex; i++) {
                dstData[dstIndex++] = string[i];
            }
        }
    }

    /**
     * Add a new string to the string table.
     */
    public void addStringToTable(byte[] oldString, byte newString) {
        int length = oldString.length;
        byte[] string = new byte[length + 1];
        System.arraycopy(oldString, 0, string, 0, length);
        string[length] = newString;

        // Add this new String to the table
        stringTable[tableIndex++] = string;

        if (tableIndex == 511) {
            bitsToGet = 10;
        } else if (tableIndex == 1023) {
            bitsToGet = 11;
        } else if (tableIndex == 2047) {
            bitsToGet = 12;
        }
    }

    /**
     * Add a new string to the string table.
     */
    public void addStringToTable(byte[] string) {
        // Add this new String to the table
        stringTable[tableIndex++] = string;

        if (tableIndex == 511) {
            bitsToGet = 10;
        } else if (tableIndex == 1023) {
            bitsToGet = 11;
        } else if (tableIndex == 2047) {
            bitsToGet = 12;
        }
    }

    /**
     * Append {@code newString} to the end of {@code oldString}.
     */
    public byte[] composeString(byte[] oldString, byte newString) {
        int length = oldString.length;
        byte[] string = new byte[length + 1];
        System.arraycopy(oldString, 0, string, 0, length);
        string[length] = newString;

        return string;
    }

    // Returns the next 9, 10, 11 or 12 bits
    public int getNextCode() {
        // Attempt to get the next code. The exception is caught to make
        // this robust to cases wherein the EndOfInformation code has been
        // omitted from a strip. Examples of such cases have been observed
        // in practice.

        try {
            nextData = (nextData << 8) | (srcData[srcIndex++] & 0xff);
            nextBits += 8;

            if (nextBits < bitsToGet) {
                nextData = (nextData << 8) | (srcData[srcIndex++] & 0xff);
                nextBits += 8;
            }

            int code =
                (nextData >> (nextBits - bitsToGet)) & andTable[bitsToGet - 9];
            nextBits -= bitsToGet;

            return code;
        } catch (ArrayIndexOutOfBoundsException e) {
            // Strip not terminated as expected: return EndOfInformation code.
            return EOI_CODE;
        }
    }
}
