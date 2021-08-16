/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package compiler.intrinsics;

/*
 * @test
 * @requires vm.opt.final.UseVectorizedMismatchIntrinsic == true
 * @modules java.base/jdk.internal.misc
 *          java.base/jdk.internal.util
 *
 *  @run main/othervm -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,*::test*
 *                    -Xbatch -XX:-TieredCompilation
 *                    -XX:UseAVX=3
 *                     compiler.intrinsics.VectorizedMismatchTest
 *
 *  @run main/othervm -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,*::test*
 *                    -Xbatch -XX:-TieredCompilation
 *                    -XX:+UnlockDiagnosticVMOptions -XX:UseAVX=3 -XX:AVX3Threshold=0
 *                     compiler.intrinsics.VectorizedMismatchTest
 */

import jdk.internal.misc.Unsafe;
import jdk.internal.util.ArraysSupport;

public class VectorizedMismatchTest {
    private boolean[] boolean_a = new boolean[128];
    private boolean[] boolean_b = new boolean[128];

    int testBooleanConstantLength(int length) {
        boolean[] obja = boolean_a;
        boolean[] objb = boolean_b;
        long offset = Unsafe.ARRAY_BOOLEAN_BASE_OFFSET;
        int scale = ArraysSupport.LOG2_ARRAY_BOOLEAN_INDEX_SCALE;
        return ArraysSupport.vectorizedMismatch(obja, offset, objb, offset, length, scale);
    }

    int testBooleanConstantLength0()   { return testBooleanConstantLength(0);   }
    int testBooleanConstantLength1()   { return testBooleanConstantLength(1);   }
    int testBooleanConstantLength64()  { return testBooleanConstantLength(64);  }
    int testBooleanConstantLength128() { return testBooleanConstantLength(128); }

    /* ==================================================================================== */

    private byte[] byte_a = new byte[128];
    private byte[] byte_b = new byte[128];

    int testByteConstantLength(int length) {
        byte[] obja = byte_a;
        byte[] objb = byte_b;
        long offset = Unsafe.ARRAY_BYTE_BASE_OFFSET;
        int scale = ArraysSupport.LOG2_ARRAY_BYTE_INDEX_SCALE;
        return ArraysSupport.vectorizedMismatch(obja, offset, objb, offset, length, scale);
    }

    int testByteConstantLength0()   { return testByteConstantLength(0);   }
    int testByteConstantLength1()   { return testByteConstantLength(1);   }
    int testByteConstantLength64()  { return testByteConstantLength(64);  }
    int testByteConstantLength128() { return testByteConstantLength(128); }

    /* ==================================================================================== */

    private short[] short_a = new short[64];
    private short[] short_b = new short[64];

    int testShortConstantLength(int length) {
        short[] obja = short_a;
        short[] objb = short_b;
        long offset = Unsafe.ARRAY_SHORT_BASE_OFFSET;
        int scale = ArraysSupport.LOG2_ARRAY_SHORT_INDEX_SCALE;
        return ArraysSupport.vectorizedMismatch(obja, offset, objb, offset, length, scale);
    }

    int testShortConstantLength0()  { return testShortConstantLength(0);  }
    int testShortConstantLength1()  { return testShortConstantLength(1);  }
    int testShortConstantLength32() { return testShortConstantLength(32); }
    int testShortConstantLength64() { return testShortConstantLength(64); }

    /* ==================================================================================== */

    private char[] char_a = new char[64];
    private char[] char_b = new char[64];

    int testCharConstantLength(int length) {
        char[] obja = char_a;
        char[] objb = char_b;
        long offset = Unsafe.ARRAY_CHAR_BASE_OFFSET;
        int scale = ArraysSupport.LOG2_ARRAY_CHAR_INDEX_SCALE;
        return ArraysSupport.vectorizedMismatch(obja, offset, objb, offset, length, scale);
    }

    int testCharConstantLength0()  { return testCharConstantLength(0);  }
    int testCharConstantLength1()  { return testCharConstantLength(1);  }
    int testCharConstantLength32() { return testCharConstantLength(32); }
    int testCharConstantLength64() { return testCharConstantLength(64); }

    /* ==================================================================================== */

    private int[] int_a = new int[32];
    private int[] int_b = new int[32];

    int testIntConstantLength(int length) {
        int[] obja = int_a;
        int[] objb = int_b;
        long offset = Unsafe.ARRAY_INT_BASE_OFFSET;
        int scale = ArraysSupport.LOG2_ARRAY_INT_INDEX_SCALE;
        return ArraysSupport.vectorizedMismatch(obja, offset, objb, offset, length, scale);
    }

    int testIntConstantLength0()  { return testIntConstantLength(0);  }
    int testIntConstantLength1()  { return testIntConstantLength(1);  }
    int testIntConstantLength16() { return testIntConstantLength(16); }
    int testIntConstantLength32() { return testIntConstantLength(32); }

    /* ==================================================================================== */

    private float[] float_a = new float[32];
    private float[] float_b = new float[32];

    int testFloatConstantLength(int length) {
        float[] obja = float_a;
        float[] objb = float_b;
        long offset = Unsafe.ARRAY_FLOAT_BASE_OFFSET;
        int scale = ArraysSupport.LOG2_ARRAY_FLOAT_INDEX_SCALE;
        return ArraysSupport.vectorizedMismatch(obja, offset, objb, offset, length, scale);
    }

    int testFloatConstantLength0()  { return testFloatConstantLength(0);  }
    int testFloatConstantLength1()  { return testFloatConstantLength(1);  }
    int testFloatConstantLength16() { return testFloatConstantLength(16); }
    int testFloatConstantLength32() { return testFloatConstantLength(32); }

    /* ==================================================================================== */

    private long[] long_a = new long[16];
    private long[] long_b = new long[16];

    int testLongConstantLength(int length) {
        long[] obja = long_a;
        long[] objb = long_b;
        long offset = Unsafe.ARRAY_LONG_BASE_OFFSET;
        int scale = ArraysSupport.LOG2_ARRAY_LONG_INDEX_SCALE;
        return ArraysSupport.vectorizedMismatch(obja, offset, objb, offset, length, scale);
    }

    int testLongConstantLength0()  { return testLongConstantLength(0);  }
    int testLongConstantLength1()  { return testLongConstantLength(1);  }
    int testLongConstantLength8()  { return testLongConstantLength(8);  }
    int testLongConstantLength16() { return testLongConstantLength(16); }

    /* ==================================================================================== */

    private double[] double_a = new double[16];
    private double[] double_b = new double[16];

    int testDoubleConstantLength(int length) {
        double[] obja = double_a;
        double[] objb = double_b;
        long offset = Unsafe.ARRAY_DOUBLE_BASE_OFFSET;
        int  scale  = ArraysSupport.LOG2_ARRAY_DOUBLE_INDEX_SCALE;
        return ArraysSupport.vectorizedMismatch(obja, offset, objb, offset, length, scale);
    }

    int testDoubleConstantLength0()  { return testDoubleConstantLength(0);  }
    int testDoubleConstantLength1()  { return testDoubleConstantLength(1);  }
    int testDoubleConstantLength8()  { return testDoubleConstantLength(8);  }
    int testDoubleConstantLength16() { return testDoubleConstantLength(16); }

    /* ==================================================================================== */

    static class ClassInitTest {
        static final int LENGTH = 64;
        static final int RESULT;
        static {
            byte[] arr1 = new byte[LENGTH];
            byte[] arr2 = new byte[LENGTH];
            for (int i = 0; i < 20_000; i++) {
                test(arr1, arr2);
            }
            RESULT = test(arr1, arr2);
        }

        static int test(byte[] obja, byte[] objb) {
            long offset = Unsafe.ARRAY_BYTE_BASE_OFFSET;
            int  scale  = ArraysSupport.LOG2_ARRAY_BYTE_INDEX_SCALE;
            return ArraysSupport.vectorizedMismatch(obja, offset, objb, offset, LENGTH, scale); // LENGTH is not considered a constant
        }
    }

    int testConstantBeingInitialized() {
        return ClassInitTest.RESULT; // trigger class initialization
    }

    /* ==================================================================================== */

    int testLoopUnswitch(int length) {
        long offset = Unsafe.ARRAY_BYTE_BASE_OFFSET;
        int  scale  = ArraysSupport.LOG2_ARRAY_BYTE_INDEX_SCALE;

        int acc = 0;
        for (int i = 0; i < 32; i++) {
            acc += ArraysSupport.vectorizedMismatch(byte_a, offset, byte_b, offset, length, scale);
        }
        return acc;
    }

    int testLoopHoist(int length, int stride) {
        long offset = Unsafe.ARRAY_BYTE_BASE_OFFSET;
        int  scale  = ArraysSupport.LOG2_ARRAY_BYTE_INDEX_SCALE;

        int acc = 0;

        for (int i = 0; i < 32; i += stride) {
            acc += ArraysSupport.vectorizedMismatch(byte_a, offset, byte_b, offset, length, scale);
        }
        return acc;
    }

    /* ==================================================================================== */

    public static void main(String[] args) {
        VectorizedMismatchTest t = new VectorizedMismatchTest();
        for (int i = 0; i < 20_000; i++) {
            t.testBooleanConstantLength0();
            t.testBooleanConstantLength1();
            t.testBooleanConstantLength64();
            t.testBooleanConstantLength128();

            t.testByteConstantLength0();
            t.testByteConstantLength1();
            t.testByteConstantLength64();
            t.testByteConstantLength128();

            t.testShortConstantLength0();
            t.testShortConstantLength1();
            t.testShortConstantLength32();
            t.testShortConstantLength64();

            t.testCharConstantLength0();
            t.testCharConstantLength1();
            t.testCharConstantLength32();
            t.testCharConstantLength64();

            t.testIntConstantLength0();
            t.testIntConstantLength1();
            t.testIntConstantLength16();
            t.testIntConstantLength32();

            t.testFloatConstantLength0();
            t.testFloatConstantLength1();
            t.testFloatConstantLength16();
            t.testFloatConstantLength32();

            t.testLongConstantLength0();
            t.testLongConstantLength1();
            t.testLongConstantLength8();
            t.testLongConstantLength16();

            t.testDoubleConstantLength0();
            t.testDoubleConstantLength1();
            t.testDoubleConstantLength8();
            t.testDoubleConstantLength16();

            t.testLoopUnswitch(32);
            t.testLoopHoist(128, 2);
        }
    }
}
