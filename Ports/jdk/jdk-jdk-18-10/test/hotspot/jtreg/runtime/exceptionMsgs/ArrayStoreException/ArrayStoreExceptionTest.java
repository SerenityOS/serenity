/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 SAP SE. All rights reserved.
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

/**
 * @test
 * @summary Test ArrayStoreException message. The message lists
 *   information about the array types involved.
 * @library /test/lib
 * @run main ArrayStoreExceptionTest
 */

import java.util.Date;
import jdk.test.lib.Asserts;

/**
 * Tests the detailed messages of the ArrayStoreException.
 */
public class ArrayStoreExceptionTest {

    static {
        System.loadLibrary("ArrayStoreExceptionTest");
    }

    static void testASMessages(Object from, Object to, String message) throws Exception {
        try {
            System.arraycopy(from, 1, to, 3, 2);
            Asserts.fail("Expected ArrayStoreException not thrown");
        } catch (ArrayStoreException e) {
            Asserts.assertEquals(e.getMessage(), message);
        }
    }

    static native void doNativeArrayStore(Object[] src, Object dst, int index);

    static void testNativeASMessages(Object[] array, Object elem, int index, String message)
        throws Exception {
        try {
            doNativeArrayStore(array, elem, index);
            Asserts.fail("Expected ArrayStoreException not thrown");
        } catch (ArrayStoreException e) {
            Asserts.assertEquals(e.getMessage(), message);
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            boolean[]    za1 = new boolean[3];
            byte[]       ba1 = new byte[3];
            short[]      sa1 = new short[3];
            char[]       ca1 = new char[3];
            int[]        ia1 = new int[3];
            long[]       la1 = new long[3];
            float[]      fa1 = new float[3];
            double[]     da1 = new double[3];
            Object[]     oa1 = new Object[3];

            boolean[]    za2 = new boolean[9];
            byte[]       ba2 = new byte[9];
            short[]      sa2 = new short[9];
            char[]       ca2 = new char[9];
            int[]        ia2 = new int[9];
            long[]       la2 = new long[9];
            float[]      fa2 = new float[9];
            double[]     da2 = new double[9];
            Object[]     oa2 = new Object[9];

            boolean[][]  za3 = new boolean[9][9];
            byte[][]     ba3 = new byte[9][9];
            short[][]    sa3 = new short[9][9];
            char[][]     ca3 = new char[9][9];
            int[][]      ia3 = new int[9][9];
            long[][]     la3 = new long[9][9];
            float[][]    fa3 = new float[9][9];
            double[][]   da3 = new double[9][9];
            Object[][]   oa3 = new Object[9][9];

            int[][][]    ia4 = new int[9][9][9];
            Object[][][] oa4 = new Object[9][9][9];


            testASMessages(za1, ba2, "arraycopy: type mismatch: can not copy boolean[] into byte[]");
            testASMessages(ba1, sa2, "arraycopy: type mismatch: can not copy byte[] into short[]");
            testASMessages(sa1, ca2, "arraycopy: type mismatch: can not copy short[] into char[]");
            testASMessages(ca1, ia2, "arraycopy: type mismatch: can not copy char[] into int[]");
            testASMessages(ia1, la2, "arraycopy: type mismatch: can not copy int[] into long[]");
            testASMessages(la1, fa2, "arraycopy: type mismatch: can not copy long[] into float[]");
            testASMessages(fa1, da2, "arraycopy: type mismatch: can not copy float[] into double[]");
            testASMessages(da1, oa2, "arraycopy: type mismatch: can not copy double[] into object array[]");
            testASMessages(oa1, za2, "arraycopy: type mismatch: can not copy object array[] into boolean[]");

            testASMessages(za1, oa2, "arraycopy: type mismatch: can not copy boolean[] into object array[]");
            testASMessages(ba1, za2, "arraycopy: type mismatch: can not copy byte[] into boolean[]");
            testASMessages(sa1, ba2, "arraycopy: type mismatch: can not copy short[] into byte[]");
            testASMessages(ca1, sa2, "arraycopy: type mismatch: can not copy char[] into short[]");
            testASMessages(ia1, ca2, "arraycopy: type mismatch: can not copy int[] into char[]");
            testASMessages(la1, ia2, "arraycopy: type mismatch: can not copy long[] into int[]");
            testASMessages(fa1, la2, "arraycopy: type mismatch: can not copy float[] into long[]");
            testASMessages(da1, fa2, "arraycopy: type mismatch: can not copy double[] into float[]");
            testASMessages(oa1, da2, "arraycopy: type mismatch: can not copy object array[] into double[]");

            testASMessages(za3, ba2, "arraycopy: type mismatch: can not copy object array[] into byte[]");
            testASMessages(ba3, sa2, "arraycopy: type mismatch: can not copy object array[] into short[]");
            testASMessages(sa3, ca2, "arraycopy: type mismatch: can not copy object array[] into char[]");
            testASMessages(ca3, ia2, "arraycopy: type mismatch: can not copy object array[] into int[]");
            testASMessages(ia3, la2, "arraycopy: type mismatch: can not copy object array[] into long[]");
            testASMessages(la3, fa2, "arraycopy: type mismatch: can not copy object array[] into float[]");
            testASMessages(fa3, da2, "arraycopy: type mismatch: can not copy object array[] into double[]");
            testASMessages(oa3, za2, "arraycopy: type mismatch: can not copy object array[] into boolean[]");

            testASMessages(za1, oa3, "arraycopy: type mismatch: can not copy boolean[] into object array[]");
            testASMessages(ba1, za3, "arraycopy: type mismatch: can not copy byte[] into object array[]");
            testASMessages(sa1, ba3, "arraycopy: type mismatch: can not copy short[] into object array[]");
            testASMessages(ca1, sa3, "arraycopy: type mismatch: can not copy char[] into object array[]");
            testASMessages(ia1, ca3, "arraycopy: type mismatch: can not copy int[] into object array[]");
            testASMessages(la1, ia3, "arraycopy: type mismatch: can not copy long[] into object array[]");
            testASMessages(fa1, la3, "arraycopy: type mismatch: can not copy float[] into object array[]");
            testASMessages(da1, fa3, "arraycopy: type mismatch: can not copy double[] into object array[]");

            //testASMessages(null, ba2,  "arraycopy: type mismatch: can not copy boolean[] into byte[]"); NPE
            //testASMessages(za1,  null, "arraycopy: type mismatch: can not copy boolean[] into byte[]"); NPE
            testASMessages("This is not an array", ia2, "arraycopy: source type java.lang.String is not an array");
            testASMessages(la1, "This is not an array", "arraycopy: destination type java.lang.String is not an array");

            //testASMessages(null, oa2,  "arraycopy: type mismatch: can not copy boolean[] into byte[]"); NPE
            //testASMessages(oa1,  null, "arraycopy: type mismatch: can not copy boolean[] into byte[]"); NPE
            testASMessages("This is not an array", oa2, "arraycopy: source type java.lang.String is not an array");
            testASMessages(oa1, "This is not an array", "arraycopy: destination type java.lang.String is not an array");

            String[] Sa1 = new String[3];
            Date[]   Da1 = new Date[3];
            String[] Sa2 = new String[9];
            Date[]   Da2 = new Date[9];

            for (int i = 0; i < 3; i++) {
                Sa1[i] = "" + i;
                oa1[i] = "" + i;
            }
            testASMessages(Sa1, Da2,
                           "arraycopy: type mismatch: can not copy java.lang.String[] " +
                           "into java.util.Date[]");
            testASMessages(oa1, Da2,
                           "arraycopy: element type mismatch: can not cast one of the " +
                           "elements of java.lang.Object[] to the type of the destination " +
                           "array, java.util.Date");

            // These should succeed.
            doNativeArrayStore(Sa1, "This is a string", 0);
            doNativeArrayStore(oa1, "This is a string", 2);

            testNativeASMessages(Da1, "This is not a date", 0,
                                 "type mismatch: can not store java.lang.String to java.util.Date[0]");
            testNativeASMessages(Da1, "This is not a date", 2,
                                 "type mismatch: can not store java.lang.String to java.util.Date[2]");
            testNativeASMessages(oa3, "This is not a date", 2,
                                 "type mismatch: can not store java.lang.String to java.lang.Object[2][]");
            testNativeASMessages(oa4, "This is not a date", 1,
                                 "type mismatch: can not store java.lang.String to java.lang.Object[1][][]");
            testNativeASMessages(ia3, "This is not a date", 1,
                                 "type mismatch: can not store java.lang.String to int[1][]");
            testNativeASMessages(ia4, "This is not a date", 2,
                                 "type mismatch: can not store java.lang.String to int[2][][]");

        } catch (java.lang.RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            Asserts.fail("Wrong exception thrown: " + e);
        }
    }
}
