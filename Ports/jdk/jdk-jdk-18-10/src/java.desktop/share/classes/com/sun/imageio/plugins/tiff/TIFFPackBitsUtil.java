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

public class TIFFPackBitsUtil {

    byte[] dstData = new byte[8192];
    int dstIndex = 0;

    public TIFFPackBitsUtil() {
    }

    private void ensureCapacity(int bytesToAdd) {
        if (dstIndex + bytesToAdd > dstData.length) {
            byte[] newDstData = new byte[Math.max((int)(dstData.length*1.2f),
                                                  dstIndex + bytesToAdd)];
            System.arraycopy(dstData, 0, newDstData, 0, dstData.length);
            dstData = newDstData;
        }
    }

    public byte[] decode(byte[] srcData) throws IOException {
        int inIndex = 0;
        while (inIndex < srcData.length) {
            byte b = srcData[inIndex++];

            if (b >= 0 && b <= 127) {
                // Literal run packet

                ensureCapacity(b + 1);
                for (int i = 0; i < b + 1; i++) {
                    dstData[dstIndex++] = srcData[inIndex++];
                }
            } else if (b <= -1 && b >= -127) {
                // 2-byte encoded run packet
                byte repeat = srcData[inIndex++];
                ensureCapacity(-b + 1);
                for (int i = 0; i < (-b + 1); i++) {
                    dstData[dstIndex++] = repeat;
                }
            } else {
                // No-op packet, do nothing
                ++inIndex;
            }
        }

        byte[] newDstData = new byte[dstIndex];
        System.arraycopy(dstData, 0, newDstData, 0, dstIndex);
        return newDstData;
    }
}
