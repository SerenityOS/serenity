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
 *          IndexColorModel works properly for IndexColorModel unique
 *          properties.
 * @run     main IndexColorModelEqualsTest
 */

import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.math.BigInteger;

public class IndexColorModelEqualsTest {

    private static void verifyEquals(IndexColorModel m1,
                                     IndexColorModel m2) {
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
                    + " IndexColorModels");
        }
    }

    private static void testColorMapEquality() {
        // test with different cmap values.
        IndexColorModel model1 = new IndexColorModel(8, 3, new int[] {1, 2, 3},
                0, true, -1, DataBuffer.TYPE_BYTE);
        IndexColorModel model2 = new IndexColorModel(8, 3, new int[] {4, 5, 6},
                0, true, -1, DataBuffer.TYPE_BYTE);
        if (model1.equals(model2)) {
            throw new RuntimeException("equals() method is determining"
                    + " ColorMap equality improperly");
        }
        if (model2.equals(model1)) {
            throw new RuntimeException("equals() method is determining"
                    + " ColorMap equality improperly");
        }
    }

    private static void testSizeEquality() {
        // test with different size for cmap.
        IndexColorModel model1 = new IndexColorModel(8, 4,
                new int[] {1, 2, 3, 4},
                0, true, -1, DataBuffer.TYPE_BYTE);
        IndexColorModel model2 = new IndexColorModel(8, 3,
                new int[] {1, 2, 3},
                0, true, -1, DataBuffer.TYPE_BYTE);
        if (model1.equals(model2)) {
            throw new RuntimeException("equals() method is determining"
                    + " Map size equality improperly");
        }
        if (model2.equals(model1)) {
            throw new RuntimeException("equals() method is determining"
                    + " Map size equality improperly");
        }
    }

    private static void testTransparentIndexEquality() {
        // test with different values for transparent_index.
        IndexColorModel model1 = new IndexColorModel(8, 3, new int[] {1, 2, 3},
                0, true, 1, DataBuffer.TYPE_BYTE);
        IndexColorModel model2 = new IndexColorModel(8, 3, new int[] {1, 2, 3},
                0, true, 2, DataBuffer.TYPE_BYTE);
        if (model1.equals(model2)) {
            throw new RuntimeException("equals() method is determining"
                    + " TransparentIndex equality improperly");
        }
        if (model2.equals(model1)) {
            throw new RuntimeException("equals() method is determining"
                    + " TransparentIndex equality improperly");
        }
    }

    private static void testValidPixelsEquality() {
        // test with different valid pixels.
        /*
         * In setRGBs() function of IndexColorModel we override
         * transparent_index value to map to pixel value if alpha is 0x00
         * so we should have atleast minimum alpha value to verify
         * equality of validBits thats why we have color value as
         * 16777216(2 ^ 24).
         */
        int color = 16777216;
        IndexColorModel model1 = new IndexColorModel(8, 3, new int[] {color,
                color, color}, 0, DataBuffer.TYPE_BYTE, new BigInteger("1"));
        IndexColorModel model2 = new IndexColorModel(8, 3, new int[] {color,
                color, color}, 0, DataBuffer.TYPE_BYTE, new BigInteger("2"));
        if (model1.equals(model2)) {
            throw new RuntimeException("equals() method is determining"
                    + " Valid pixels equality improperly");
        }
        if (model2.equals(model1)) {
            throw new RuntimeException("equals() method is determining"
                    + " Valid pixels equality improperly");
        }
    }

    private static void testConstructor1() {
        /*
         * verify equality with constructor
         * IndexColorModel(int bits, int size, byte[] r, byte[] g, byte[] b)
         */
        IndexColorModel model1 = new IndexColorModel(8, 2,
                new byte[] {1, 2}, new byte[] {1, 2}, new byte[] {1, 2});
        IndexColorModel model2 = new IndexColorModel(8, 2,
                new byte[] {1, 2}, new byte[] {1, 2}, new byte[] {1, 2});
        verifyEquals(model1, model2);
    }

    private static void testConstructor2() {
        /*
         * verify equality with constructor
         * IndexColorModel(int bits, int size, byte[] r, byte[] g, byte[] b,
         * byte[] a)
         */
        IndexColorModel model1 = new IndexColorModel(8, 2, new byte[] {1, 2},
                new byte[] {1, 2}, new byte[] {1, 2}, new byte[] {1, 2});
        IndexColorModel model2 = new IndexColorModel(8, 2, new byte[] {1, 2},
                new byte[] {1, 2}, new byte[] {1, 2}, new byte[] {1, 2});
        verifyEquals(model1, model2);
    }

    private static void testConstructor3() {
        /*
         * verify equality with constructor
         * IndexColorModel(int bits, int size, byte[] r, byte[] g, byte[] b,
         * int trans)
         */
        IndexColorModel model1 = new IndexColorModel(8, 2, new byte[] {1, 2},
                new byte[] {1, 2}, new byte[] {1, 2}, 1);
        IndexColorModel model2 = new IndexColorModel(8, 2, new byte[] {1, 2},
                new byte[] {1, 2}, new byte[] {1, 2}, 1);
        verifyEquals(model1, model2);
    }

    private static void testConstructor4() {
        /*
         * verify equality with constructor
         * IndexColorModel(int bits, int size, byte[] cmap, int start,
         * boolean hasalpha)
         */
        IndexColorModel model1 = new IndexColorModel(8, 1,
                new byte[] {1, 2, 3, 4}, 0, true);
        IndexColorModel model2 = new IndexColorModel(8, 1,
                new byte[] {1, 2, 3, 4}, 0, true);
        verifyEquals(model1, model2);
    }

    private static void testConstructor5() {
        /*
         * verify equality with constructor
         * IndexColorModel(int bits, int size, byte[] cmap, int start,
         * boolean hasalpha, int trans)
         */
        IndexColorModel model1 = new IndexColorModel(8, 1,
                new byte[] {1, 2, 3, 4}, 0, true, 0);
        IndexColorModel model2 = new IndexColorModel(8, 1,
                new byte[] {1, 2, 3, 4}, 0, true, 0);
        verifyEquals(model1, model2);
    }

    private static void testConstructor6() {
        /*
         * verify equality with constructor
         * IndexColorModel(int bits, int size, int[] cmap, int start,
         * boolean hasalpha, int trans, int transferType)
         */
        IndexColorModel model1 = new IndexColorModel(8, 3, new int[] {1, 2, 3},
                0, true, -1, DataBuffer.TYPE_BYTE);
        IndexColorModel model2 = new IndexColorModel(8, 3, new int[] {1, 2, 3},
                0, true, -1, DataBuffer.TYPE_BYTE);
        verifyEquals(model1, model2);
    }

    private static void testConstructor7() {
        /*
         * verify equality with constructor
         * IndexColorModel(int bits, int size, int[] cmap, int start,
         * int transferType, BigInteger validBits)
         */
        /*
         * In setRGBs() function of IndexColorModel we override
         * transparent_index value to map to pixel value if alpha is 0x00
         * so we should have atleast minimum alpha value to keep
         * both model1 and model2 same.
         */
        int color = 16777216;
        IndexColorModel model1 = new IndexColorModel(8, 3, new int[] {color,
                color, color}, 0, DataBuffer.TYPE_BYTE, new BigInteger("1"));
        IndexColorModel model2 = new IndexColorModel(8, 3, new int[] {color,
                color, color}, 0, DataBuffer.TYPE_BYTE, new BigInteger("1"));
        verifyEquals(model1, model2);
    }

    private static void testSameIndexColorModel() {
        testConstructor1();
        testConstructor2();
        testConstructor3();
        testConstructor4();
        testConstructor5();
        testConstructor6();
        testConstructor7();
    }
    public static void main(String[] args) {
        /* test whether equals() method works properly for parameters
         * unique to IndexColorModel.
         */
        testColorMapEquality();
        testSizeEquality();
        testTransparentIndexEquality();
        testValidPixelsEquality();
        // verify same IndexColorModel equality using different constructors.
        testSameIndexColorModel();
    }
}

