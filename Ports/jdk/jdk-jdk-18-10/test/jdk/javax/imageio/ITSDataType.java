/*
 * Copyright (c) 2002, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
 * @test
 * @bug 4506450
 * @summary Tests whether ImageTypeSpecifier.createBanded() and
 *          ImageTypeSpecifier.createInterleaved() can accept all supported
 *          DataBuffer types
 */

import java.awt.color.ColorSpace;
import java.awt.image.DataBuffer;

import javax.imageio.ImageTypeSpecifier;

public class ITSDataType {

    public static final int[] dataTypes = new int[] {
        DataBuffer.TYPE_BYTE,
        DataBuffer.TYPE_SHORT,
        DataBuffer.TYPE_USHORT,
        DataBuffer.TYPE_INT,
        DataBuffer.TYPE_FLOAT,
        DataBuffer.TYPE_DOUBLE,
    };

    public static void main(String[] args) {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        int[] bankIndices = new int[] { 1 };
        int[] bandOffsets = new int[] { 0 };

        // test createBanded()
        for (int i = 0; i < dataTypes.length; i++) {
            int dataType = dataTypes[i];

            try {
                ImageTypeSpecifier.createBanded(cs, bankIndices, bandOffsets,
                                                dataType, false, false);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("createBanded() test failed for " +
                                           "dataType = " + dataType);
            }
        }

        // test createInterleaved()
        for (int i = 0; i < dataTypes.length; i++) {
            int dataType = dataTypes[i];

            try {
                ImageTypeSpecifier.createInterleaved(cs, bandOffsets,
                                                     dataType, false, false);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("createInterleaved() test failed " +
                                           "for dataType = " + dataType);
            }
        }
    }
}
