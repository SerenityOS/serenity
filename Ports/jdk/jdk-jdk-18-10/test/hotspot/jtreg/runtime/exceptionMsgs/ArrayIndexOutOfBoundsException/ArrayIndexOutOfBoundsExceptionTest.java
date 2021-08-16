/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, 2019 SAP SE. All rights reserved.
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
 * @summary Test extended ArrayIndexOutOfBoundsException message. The
 *   message lists information about the array and the indexes involved.
 * @comment This will run in 'normal' mode when Graal is not enabled, else
 *   Graal mode.
 * @run testng ArrayIndexOutOfBoundsExceptionTest
 */
/**
 * @test
 * @requires !vm.graal.enabled
 * @comment These test C1 and C2 so make no sense when Graal is enabled.
 * @run testng/othervm -Xcomp -XX:-TieredCompilation  ArrayIndexOutOfBoundsExceptionTest
 * @run testng/othervm -Xcomp -XX:TieredStopAtLevel=1 ArrayIndexOutOfBoundsExceptionTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Tests the detailed messages of the ArrayIndexOutOfBoundsException.
 */
public class ArrayIndexOutOfBoundsExceptionTest {

    static {
        System.loadLibrary("ArrayIndexOutOfBoundsExceptionTest");
    }

    // Some fields used in the test.
    static int[] staticArray = new int[0];
    static long[][] staticLongArray = new long[0][0];
    ArrayList<String> names = new ArrayList<>();
    ArrayList<String> curr;

    public static void main(String[] args) {
        ArrayIndexOutOfBoundsExceptionTest t = new ArrayIndexOutOfBoundsExceptionTest();
        try {
            t.testAIOOBMessages();
        } catch (Exception e) {}
    }

    /**
     *
     */
    public static class ArrayGenerator {

        /**
         * @param dummy1
         * @return Object Array
         */
        public static Object[] arrayReturner(boolean dummy1) {
            return new Object[0];
        }

        /**
         * @param dummy1
         * @param dummy2
         * @param dummy3
         * @return Object Array
         */
        public Object[] returnMyArray(double dummy1, long dummy2, short dummy3) {
            return new Object[0];
        }
    }

    static native void  doNativeArrayStore(Object[] dst, Object element, int index);
    static native Object doNativeArrayLoad(Object[] src, int index);

    static native void doNativeBooleanArrayRegionLoad (boolean[] source, int start, int len);
    static native void doNativeBooleanArrayRegionStore(boolean[] source, int start, int len);
    static native void doNativeByteArrayRegionLoad    (byte[] source,    int start, int len);
    static native void doNativeByteArrayRegionStore   (byte[] source,    int start, int len);
    static native void doNativeShortArrayRegionLoad   (short[] source,   int start, int len);
    static native void doNativeShortArrayRegionStore  (short[] source,   int start, int len);
    static native void doNativeCharArrayRegionLoad    (char[] source,    int start, int len);
    static native void doNativeCharArrayRegionStore   (char[] source,    int start, int len);
    static native void doNativeIntArrayRegionLoad     (int[] source,     int start, int len);
    static native void doNativeIntArrayRegionStore    (int[] source,     int start, int len);
    static native void doNativeLongArrayRegionLoad    (long[] source,    int start, int len);
    static native void doNativeLongArrayRegionStore   (long[] source,    int start, int len);
    static native void doNativeFloatArrayRegionLoad   (float[] source,   int start, int len);
    static native void doNativeFloatArrayRegionStore  (float[] source,   int start, int len);
    static native void doNativeDoubleArrayRegionLoad  (double[] source,  int start, int len);
    static native void doNativeDoubleArrayRegionStore (double[] source,  int start, int len);

    /**
     *
     */
    @Test
    public void testAIOOBMessages() {
        boolean[] za1 = new boolean[0];
        byte[]    ba1 = new byte[0];
        short[]   sa1 = new short[0];
        char[]    ca1 = new char[0];
        int[]     ia1 = new int[0];
        long[]    la1 = new long[0];
        float[]   fa1 = new float[0];
        double[]  da1 = new double[0];
        Object[]  oa1 = new Object[10];
        Object[]  oa2 = new Object[5];

        boolean[] za2 = new boolean[10];
        boolean[] za3 = new boolean[5];
        byte[]    ba2 = new byte[10];
        byte[]    ba3 = new byte[5];
        short[]   sa2 = new short[10];
        short[]   sa3 = new short[5];
        char[]    ca2 = new char[10];
        char[]    ca3 = new char[5];
        int[]     ia2 = new int[10];
        int[]     ia3 = new int[5];
        long[]    la2 = new long[10];
        long[]    la3 = new long[5];
        float[]   fa2 = new float[10];
        float[]   fa3 = new float[5];
        double[]  da2 = new double[10];
        double[]  da3 = new double[5];

        try {
            System.out.println(za1[-5]);
            fail();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index -5 out of bounds for length 0");
        }

        try {
            System.out.println(ba1[0]);
            fail();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index 0 out of bounds for length 0");
        }

        try {
            System.out.println(sa1[0]);
            fail();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index 0 out of bounds for length 0");
        }

        try {
            System.out.println(ca1[0]);
            fail();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index 0 out of bounds for length 0");
        }

        try {
            System.out.println(ia1[0]);
            fail();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index 0 out of bounds for length 0");
        }

        try {
            System.out.println(la1[0]);
            fail();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index 0 out of bounds for length 0");
        }

        try {
            System.out.println(fa1[0]);
            fail();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index 0 out of bounds for length 0");
        }

        try {
            System.out.println(da1[0]);
            fail();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index 0 out of bounds for length 0");
        }

        try {
            System.out.println(oa1[12]);
            fail();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index 12 out of bounds for length 10");
        }

        try {
            System.out.println(za1[0] = false);
            fail();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index 0 out of bounds for length 0");
        }

        try {
            System.out.println(ba1[0] = 0);
            fail();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index 0 out of bounds for length 0");
        }

        try {
            System.out.println(sa1[0] = 0);
            fail();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index 0 out of bounds for length 0");
        }

        try {
            System.out.println(ca1[0] = 0);
            fail();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index 0 out of bounds for length 0");
        }

        try {
            System.out.println(ia1[0] = 0);
            fail();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index 0 out of bounds for length 0");
        }

        try {
            System.out.println(la1[0] = 0);
            fail();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index 0 out of bounds for length 0");
        }

        try {
            System.out.println(fa1[0] = 0);
            fail();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index 0 out of bounds for length 0");
        }

        try {
            System.out.println(da1[0] = 0);
            fail();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index 0 out of bounds for length 0");
        }

        try {
            System.out.println(oa1[-2] = null);
            fail();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index -2 out of bounds for length 10");
        }

        try {
            assertTrue((ArrayGenerator.arrayReturner(false))[0] == null);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index 0 out of bounds for length 0");
        }
        try {
            staticArray[0] = 2;
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index 0 out of bounds for length 0");
        }

        // Test all five possible messages of arraycopy exceptions thrown in ObjArrayKlass::copy_array().

        try {
            System.arraycopy(oa1, -17, oa2, 0, 5);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "arraycopy: source index -17 out of bounds for object array[10]");
        }

        try {
            System.arraycopy(oa1, 2, oa2, -18, 5);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "arraycopy: destination index -18 out of bounds for object array[5]");
        }

        try {
            System.arraycopy(oa1, 2, oa2, 0, -19);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "arraycopy: length -19 is negative");
        }

        try {
            System.arraycopy(oa1, 8, oa2, 0, 5);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "arraycopy: last source index 13 out of bounds for object array[10]");
        }

        try {
            System.arraycopy(oa1, 1, oa2, 0, 7);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "arraycopy: last destination index 7 out of bounds for object array[5]");
        }

        // Test all five possible messages of arraycopy exceptions thrown in TypeArrayKlass::copy_array().

        try {
            System.arraycopy(da2, -17, da3, 0, 5);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "arraycopy: source index -17 out of bounds for double[10]");
        }

        try {
            System.arraycopy(da2, 2, da3, -18, 5);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "arraycopy: destination index -18 out of bounds for double[5]");
        }

        try {
            System.arraycopy(da2, 2, da3, 0, -19);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "arraycopy: length -19 is negative");
        }

        try {
            System.arraycopy(da2, 8, da3, 0, 5);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "arraycopy: last source index 13 out of bounds for double[10]");
        }

        try {
            System.arraycopy(da2, 1, da3, 0, 7);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "arraycopy: last destination index 7 out of bounds for double[5]");
        }

        // Test all possible basic types in the messages of arraycopy exceptions thrown in TypeArrayKlass::copy_array().

        try {
            System.arraycopy(za2, -17, za3, 0, 5);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "arraycopy: source index -17 out of bounds for boolean[10]");
        }

        try {
            System.arraycopy(ba2, 2, ba3, -18, 5);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "arraycopy: destination index -18 out of bounds for byte[5]");
        }

        try {
            System.arraycopy(sa2, 2, sa3, 0, -19);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "arraycopy: length -19 is negative");
        }

        try {
            System.arraycopy(ca2, 8, ca3, 0, 5);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "arraycopy: last source index 13 out of bounds for char[10]");
        }

        try {
            System.arraycopy(ia2, 2, ia3, 0, -19);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "arraycopy: length -19 is negative");
        }

        try {
            System.arraycopy(la2, 1, la3, 0, 7);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "arraycopy: last destination index 7 out of bounds for long[5]");
        }

        try {
            System.arraycopy(fa2, 1, fa3, 0, 7);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "arraycopy: last destination index 7 out of bounds for float[5]");
        }


        // Test native array access.


        try {
            System.out.println(doNativeArrayLoad(oa2, 77));
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index 77 out of bounds for length 5");
        }
        try {
            System.out.println(doNativeArrayLoad(oa1, -1));
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index -1 out of bounds for length 10");
        }

        try {
            doNativeArrayStore(oa1, "Some String", Integer.MIN_VALUE);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index -2147483648 out of bounds for length 10");
        }
        try {
            doNativeArrayStore(oa1, "Some String", 13);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Index 13 out of bounds for length 10");
        }

        // Boolean

        // Native array region loads.
        // Boolean, len negative.
        try {
            doNativeBooleanArrayRegionLoad(za2, 3, -77);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Length -77 is negative");
        }
        // Boolean, index negative.
        try {
            doNativeBooleanArrayRegionLoad(za2, -3, 3);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region -3..0 out of bounds for length 10");
        }
        // Boolean, index+len too big.
        try {
            doNativeBooleanArrayRegionLoad(za2, 3, Integer.MAX_VALUE-1);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region 3..2147483649 out of bounds for length 10");
        }
        // Native array region stores
        // Boolean, len negative.
        try {
            doNativeBooleanArrayRegionStore(za2, 3, -77);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Length -77 is negative");
        }
        // Boolean, index negative.
        try {
            doNativeBooleanArrayRegionStore(za2, -3, 3);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region -3..0 out of bounds for length 10");
        }
        // Boolean, index+len too big.
        try {
            doNativeBooleanArrayRegionStore(za2, 3, Integer.MAX_VALUE);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region 3..2147483650 out of bounds for length 10");
        }

        // Byte

        // Native array region loads.
        // Byte, len negative.
        try {
            doNativeByteArrayRegionLoad(ba1, 3, -77);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Length -77 is negative");
        }
        // Byte, index negative.
        try {
            doNativeByteArrayRegionLoad(ba1, -3, 3);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region -3..0 out of bounds for length 0");
        }
        // Byte, index+len too big.
        try {
            doNativeByteArrayRegionLoad(ba2, 3, Integer.MAX_VALUE-1);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region 3..2147483649 out of bounds for length 10");
        }
        // Native array region stores
        // Byte, len negative.
        try {
            doNativeByteArrayRegionStore(ba2, 3, -77);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Length -77 is negative");
        }
        // Byte, index negative.
        try {
            doNativeByteArrayRegionStore(ba2, -3, 3);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region -3..0 out of bounds for length 10");
        }
        // Byte, index+len too big.
        try {
            doNativeByteArrayRegionStore(ba2, 3, Integer.MAX_VALUE);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region 3..2147483650 out of bounds for length 10");
        }

        // Short

        // Native array region loads.
        // Short, len negative.
        try {
            doNativeShortArrayRegionLoad(sa2, 3, -77);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Length -77 is negative");
        }
        // Short, index negative.
        try {
            doNativeShortArrayRegionLoad(sa2, -3, 3);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region -3..0 out of bounds for length 10");
        }
        // Short, index+len too big.
        try {
            doNativeShortArrayRegionLoad(sa2, 3, Integer.MAX_VALUE-1);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region 3..2147483649 out of bounds for length 10");
        }
        // Native array region stores
        // Short, len negative.
        try {
            doNativeShortArrayRegionStore(sa2, 3, -77);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Length -77 is negative");
        }
        // Short, index negative.
        try {
            doNativeShortArrayRegionStore(sa2, -3, 3);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region -3..0 out of bounds for length 10");
        }
        // Short, index+len too big.
        try {
            doNativeShortArrayRegionStore(sa2, 3, Integer.MAX_VALUE);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region 3..2147483650 out of bounds for length 10");
        }

        // Char

        // Native array region loads.
        // Char, len negative.
        try {
            doNativeCharArrayRegionLoad(ca2, 3, -77);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Length -77 is negative");
        }
        // Char, index negative.
        try {
            doNativeCharArrayRegionLoad(ca2, -3, 3);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region -3..0 out of bounds for length 10");
        }
        // Char, index+len too big.
        try {
            doNativeCharArrayRegionLoad(ca2, 3, Integer.MAX_VALUE-1);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region 3..2147483649 out of bounds for length 10");
        }
        // Native array region stores
        // Char, len negative.
        try {
            doNativeCharArrayRegionStore(ca2, 3, -77);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Length -77 is negative");
        }
        // Char, index negative.
        try {
            doNativeCharArrayRegionStore(ca2, -3, 3);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region -3..0 out of bounds for length 10");
        }
        // Char, index+len too big.
        try {
            doNativeCharArrayRegionStore(ca2, 3, Integer.MAX_VALUE);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region 3..2147483650 out of bounds for length 10");
        }

        // Int

        // Native array region loads.
        // Int, len negative.
        try {
            doNativeIntArrayRegionLoad(ia2, 3, -77);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Length -77 is negative");
        }
        // Int, index negative.
        try {
            doNativeIntArrayRegionLoad(ia2, -3, 3);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region -3..0 out of bounds for length 10");
        }
        // Int, index+len too big.
        try {
            doNativeIntArrayRegionLoad(ia2, 3, Integer.MAX_VALUE-1);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region 3..2147483649 out of bounds for length 10");
        }
        // Native array region stores
        // Int, len negative.
        try {
            doNativeIntArrayRegionStore(ia2, 3, -77);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Length -77 is negative");
        }
        // Int, index negative.
        try {
            doNativeIntArrayRegionStore(ia2, -3, 3);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region -3..0 out of bounds for length 10");
        }
        // Int, index+len too big.
        try {
            doNativeIntArrayRegionStore(ia2, 3, Integer.MAX_VALUE);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region 3..2147483650 out of bounds for length 10");
        }

        // Long

        // Native array region loads.
        // Long, len negative.
        try {
            doNativeLongArrayRegionLoad(la2, 3, -77);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Length -77 is negative");
        }
        // Long, index negative.
        try {
            doNativeLongArrayRegionLoad(la2, -3, 3);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region -3..0 out of bounds for length 10");
        }
        // Long, index+len too big.
        try {
            doNativeLongArrayRegionLoad(la2, 3, Integer.MAX_VALUE-1);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region 3..2147483649 out of bounds for length 10");
        }
        // Native array region stores
        // Long, len negative.
        try {
            doNativeLongArrayRegionStore(la2, 3, -77);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Length -77 is negative");
        }
        // Long, index negative.
        try {
            doNativeLongArrayRegionStore(la2, -3, 3);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region -3..0 out of bounds for length 10");
        }
        // Long, index+len too big.
        try {
            doNativeLongArrayRegionStore(la2, 3, Integer.MAX_VALUE);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region 3..2147483650 out of bounds for length 10");
        }

        // Float

        // Native array region loads.
        // Float, len negative.
        try {
            doNativeFloatArrayRegionLoad(fa2, 3, -77);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Length -77 is negative");
        }
        // Float, index negative.
        try {
            doNativeFloatArrayRegionLoad(fa2, -3, 3);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region -3..0 out of bounds for length 10");
        }
        // Float, index+len too big.
        try {
            doNativeFloatArrayRegionLoad(fa2, 3, Integer.MAX_VALUE-1);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region 3..2147483649 out of bounds for length 10");
        }
        // Native array region stores
        // Float, len negative.
        try {
            doNativeFloatArrayRegionStore(fa2, 3, -77);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Length -77 is negative");
        }
        // Float, index negative.
        try {
            doNativeFloatArrayRegionStore(fa2, -3, 3);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region -3..0 out of bounds for length 10");
        }
        // Float, index+len too big.
        try {
            doNativeFloatArrayRegionStore(fa2, 3, Integer.MAX_VALUE);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region 3..2147483650 out of bounds for length 10");
        }

        // Double

        // Native array region loads.
        // Double, len negative.
        try {
            doNativeDoubleArrayRegionLoad(da2, 3, -77);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Length -77 is negative");
        }
        // Double, index negative.
        try {
            doNativeDoubleArrayRegionLoad(da2, -3, 3);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region -3..0 out of bounds for length 10");
        }
        // Double, index+len too big.
        try {
            doNativeDoubleArrayRegionLoad(da2, 3, Integer.MAX_VALUE-1);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region 3..2147483649 out of bounds for length 10");
        }
        // Native array region stores
        // Double, len negative.
        try {
            doNativeDoubleArrayRegionStore(da2, 3, -77);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Length -77 is negative");
        }
        // Double, index negative.
        try {
            doNativeDoubleArrayRegionStore(da2, -3, 3);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region -3..0 out of bounds for length 10");
        }
        // Double, index+len too big.
        try {
            doNativeDoubleArrayRegionStore(da2, 3, Integer.MAX_VALUE);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(e.getMessage(),
                "Array region 3..2147483650 out of bounds for length 10");
        }
    }
}
