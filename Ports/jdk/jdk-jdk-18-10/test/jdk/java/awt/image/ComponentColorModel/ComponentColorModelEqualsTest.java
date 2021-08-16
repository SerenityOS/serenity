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
 *          ComponentColorModel works properly.
 * @run     main ComponentColorModelEqualsTest
 */

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;

public class ComponentColorModelEqualsTest {

    private static void verifyEquals(ComponentColorModel m1,
                                     ComponentColorModel m2) {
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
                    + " ComponentColorModels");
        }
    }

    private static void testConstructor1() {
        /*
         * verify equality with constructor
         * ComponentColorModel(ColorSpace colorSpace,
         *                  int[] bits,
         *                  boolean hasAlpha,
         *                  boolean isAlphaPremultiplied,
         *                  int transparency,
         *                  int transferType)
         */
        ComponentColorModel model1 =
            new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                    new int[] {8, 8, 8},
                                    false,
                                    false,
                                    Transparency.OPAQUE,
                                    DataBuffer.TYPE_BYTE);
        ComponentColorModel model2 =
            new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                    new int[] {8, 8, 8},
                                    false,
                                    false,
                                    Transparency.OPAQUE,
                                    DataBuffer.TYPE_BYTE);
        verifyEquals(model1, model2);
    }

    private static void testConstructor2() {
        /*
         * verify equality with constructor
         * ComponentColorModel(ColorSpace colorSpace,
         *                  boolean hasAlpha,
         *                  boolean isAlphaPremultiplied,
         *                  int transparency,
         *                  int transferType)
         */
        ComponentColorModel model1 =
            new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                    false,
                                    false,
                                    Transparency.OPAQUE,
                                    DataBuffer.TYPE_BYTE);
        ComponentColorModel model2 =
            new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                    false,
                                    false,
                                    Transparency.OPAQUE,
                                    DataBuffer.TYPE_BYTE);
        verifyEquals(model1, model2);
    }

    private static void testSameComponentColorModel() {
        testConstructor1();
        testConstructor2();
    }
    public static void main(String[] args) {
        // verify ComponentColorModel equality using different constructors.
        testSameComponentColorModel();
    }
}

