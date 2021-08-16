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

class TIFFLZWUtil {
    public TIFFLZWUtil() {
    }

    byte[] srcData;
    int srcIndex;

    byte[] dstData;
    int dstIndex = 0;

    byte[][] stringTable;
    int tableIndex, bitsToGet = 9;

    int nextData = 0;
    int nextBits = 0;

    private static final int[] andTable = {
        511,
        1023,
        2047,
        4095
    };

    public byte[] decode(byte[] data, int predictor, int samplesPerPixel,
                         int width, int height) throws IOException {
        if (data[0] == (byte)0x00 && data[1] == (byte)0x01) {
            throw new IIOException("TIFF 5.0-style LZW compression is not supported!");
        }

        this.srcData = data;
        this.srcIndex = 0;
        this.nextData = 0;
        this.nextBits = 0;

        this.dstData = new byte[8192];
        this.dstIndex = 0;

        initializeStringTable();

        int code, oldCode = 0;
        byte[] string;

        while ((code = getNextCode()) != 257) {
            if (code == 256) {
                initializeStringTable();
                code = getNextCode();
                if (code == 257) {
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

        if (predictor == 2) {

            int count;
            for (int j = 0; j < height; j++) {

                count = samplesPerPixel * (j * width + 1);

                for (int i = samplesPerPixel; i < width * samplesPerPixel; i++) {

                    dstData[count] += dstData[count - samplesPerPixel];
                    count++;
                }
            }
        }

        byte[] newDstData = new byte[dstIndex];
        System.arraycopy(dstData, 0, newDstData, 0, dstIndex);
        return newDstData;
    }

    /**
     * Initialize the string table.
     */
    public void initializeStringTable() {
        stringTable = new byte[4096][];

        for (int i = 0; i < 256; i++) {
            stringTable[i] = new byte[1];
            stringTable[i][0] = (byte)i;
        }

        tableIndex = 258;
        bitsToGet = 9;
    }

    private void ensureCapacity(int bytesToAdd) {
        if (dstIndex + bytesToAdd > dstData.length) {
            byte[] newDstData = new byte[Math.max((int)(dstData.length*1.2f),
                                                  dstIndex + bytesToAdd)];
            System.arraycopy(dstData, 0, newDstData, 0, dstData.length);
            dstData = newDstData;
        }
    }

    /**
     * Write out the string just uncompressed.
     */
    public void writeString(byte[] string) {
        ensureCapacity(string.length);
        for (int i = 0; i < string.length; i++) {
            dstData[dstIndex++] = string[i];
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
            return 257;
        }
    }
}
