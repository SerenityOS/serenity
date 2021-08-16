/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     7107905
 * @summary Test verifies whether equals() and hashCode() methods in
 *          PackedColorModel works properly.
 * @run     main PackedColorModelEqualsTest
 */

import java.awt.color.ColorSpace;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;

public class PackedColorModelEqualsTest {

    private static void verifyEquals(DirectColorModel m1,
                                     DirectColorModel m2) {
        if (m1.equals(null)) {
            throw new RuntimeException("equals(null) returns true");
        }
        if (!(m1.equals(m2))) {
            throw new RuntimeException("equals() method is not working"
                    + " properly");
        }
        if (!(m2.equals(m1))) {
            throw new RuntimeException("equals() method is not working"
                    + " properly");
        }
        if (m1.hashCode() != m2.hashCode()) {
            throw new RuntimeException("HashCode is not same for same"
                    + " PackedColorModels");
        }
    }

    private static void testMaskArrayEquality() {
        /*
         * Test with different maskArray values, since PackedColorModel
         * is abstract we use subclass DirectColorModel.
         */
        DirectColorModel model1 =
            new DirectColorModel(24, 0x00FF0000, 0x0000FF00, 0x000000FF);
        DirectColorModel model2 =
            new DirectColorModel(24, 0x000000FF, 0x0000FF00, 0x00FF0000);
        if (model1.equals(model2)) {
            throw new RuntimeException("equals() method is determining"
                    + " ColorMap equality improperly");
        }
        if (model2.equals(model1)) {
            throw new RuntimeException("equals() method is determining"
                    + " ColorMap equality improperly");
        }
    }

    private static void testConstructor1() {
        /*
         * verify equality with constructor
         * DirectColorModel(int bits, int rmask, int gmask, int bmask,
         *    int amask)
         */
        DirectColorModel model1 =
            new DirectColorModel(32, 0xFF000000, 0x00FF0000,
                    0x0000FF00, 0x000000FF);
        DirectColorModel model2 =
            new DirectColorModel(32, 0xFF000000, 0x00FF0000,
                    0x0000FF00, 0x000000FF);
        verifyEquals(model1, model2);
    }

    private static void testConstructor2() {
        /*
         * verify equality with constructor
         * DirectColorModel(ColorSpace space, int bits, int rmask, int gmask,
         * int bmask, int amask, boolean isAlphaPremultiplied, int transferType)
         */
        DirectColorModel model1 =
            new DirectColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                    32, 0xFF000000, 0x00FF0000, 0x0000FF00, 0x000000FF,
                    false, DataBuffer.TYPE_BYTE);
        DirectColorModel model2 =
            new DirectColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                    32, 0xFF000000, 0x00FF0000, 0x0000FF00, 0x000000FF,
                    false, DataBuffer.TYPE_BYTE);
        verifyEquals(model1, model2);
    }

    private static void testSamePackedColorModel() {
        testConstructor1();
        testConstructor2();
    }
    public static void main(String[] args) {
        // test with different mask array.
        testMaskArrayEquality();
        // verify PackedColorModel equality using different constructors.
        testSamePackedColorModel();
    }
}

