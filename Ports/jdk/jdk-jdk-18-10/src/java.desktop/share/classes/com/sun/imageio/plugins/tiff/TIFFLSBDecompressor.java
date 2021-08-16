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

import java.io.IOException;

public class TIFFLSBDecompressor extends TIFFDecompressor {

    /**
     * Table for flipping bytes from LSB-to-MSB to MSB-to-LSB.
     */
    private static final byte[] flipTable = TIFFFaxDecompressor.flipTable;

    public TIFFLSBDecompressor() {}

    public void decodeRaw(byte[] b,
                          int dstOffset,
                          int bitsPerPixel,
                          int scanlineStride) throws IOException {
        stream.seek(offset);

        int bytesPerRow = (srcWidth*bitsPerPixel + 7)/8;
        if(bytesPerRow == scanlineStride) {
            int numBytes = bytesPerRow*srcHeight;
            stream.readFully(b, dstOffset, numBytes);
            int xMax = dstOffset + numBytes;
            for (int x = dstOffset; x < xMax; x++) {
                b[x] = flipTable[b[x]&0xff];
            }
        } else {
            for (int y = 0; y < srcHeight; y++) {
                stream.readFully(b, dstOffset, bytesPerRow);
                int xMax = dstOffset + bytesPerRow;
                for (int x = dstOffset; x < xMax; x++) {
                    b[x] = flipTable[b[x]&0xff];
                }
                dstOffset += scanlineStride;
            }
        }
    }
}
