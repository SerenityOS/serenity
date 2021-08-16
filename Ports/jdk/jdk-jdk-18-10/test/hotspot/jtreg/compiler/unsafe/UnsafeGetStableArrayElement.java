/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary tests on constant folding of unsafe get operations from stable arrays
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @requires vm.flavor == "server" & !vm.emulatedClient
 *
 * @modules java.base/jdk.internal.vm.annotation
 *          java.base/jdk.internal.misc
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions
 *                   -Xbatch -XX:-TieredCompilation
 *                   -XX:+FoldStableValues
 *                   -XX:+WhiteBoxAPI
 *                   -XX:CompileCommand=dontinline,*Test::test*
 *                   compiler.unsafe.UnsafeGetStableArrayElement
 */

package compiler.unsafe;

import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.Stable;
import jdk.test.lib.Platform;

import java.util.concurrent.Callable;

import static jdk.internal.misc.Unsafe.*;
import static jdk.test.lib.Asserts.assertEQ;
import static jdk.test.lib.Asserts.assertNE;

import sun.hotspot.code.Compiler;

public class UnsafeGetStableArrayElement {
    @Stable static final boolean[] STABLE_BOOLEAN_ARRAY = new boolean[16];
    @Stable static final    byte[]    STABLE_BYTE_ARRAY = new    byte[16];
    @Stable static final   short[]   STABLE_SHORT_ARRAY = new   short[8];
    @Stable static final    char[]    STABLE_CHAR_ARRAY = new    char[8];
    @Stable static final     int[]     STABLE_INT_ARRAY = new     int[4];
    @Stable static final    long[]    STABLE_LONG_ARRAY = new    long[2];
    @Stable static final   float[]   STABLE_FLOAT_ARRAY = new   float[4];
    @Stable static final  double[]  STABLE_DOUBLE_ARRAY = new  double[2];
    @Stable static final  Object[]  STABLE_OBJECT_ARRAY = new  Object[4];

    static {
        Setter.reset();
    }
    static final Unsafe U = Unsafe.getUnsafe();

    static class Setter {
        private static void setZ(boolean defaultVal) { STABLE_BOOLEAN_ARRAY[0] = defaultVal ? false :                true; }
        private static void setB(boolean defaultVal) { STABLE_BYTE_ARRAY[0]    = defaultVal ?     0 :      Byte.MAX_VALUE; }
        private static void setS(boolean defaultVal) { STABLE_SHORT_ARRAY[0]   = defaultVal ?     0 :     Short.MAX_VALUE; }
        private static void setC(boolean defaultVal) { STABLE_CHAR_ARRAY[0]    = defaultVal ?     0 : Character.MAX_VALUE; }
        private static void setI(boolean defaultVal) { STABLE_INT_ARRAY[0]     = defaultVal ?     0 :   Integer.MAX_VALUE; }
        private static void setJ(boolean defaultVal) { STABLE_LONG_ARRAY[0]    = defaultVal ?     0 :      Long.MAX_VALUE; }
        private static void setF(boolean defaultVal) { STABLE_FLOAT_ARRAY[0]   = defaultVal ?     0 :     Float.MAX_VALUE; }
        private static void setD(boolean defaultVal) { STABLE_DOUBLE_ARRAY[0]  = defaultVal ?     0 :    Double.MAX_VALUE; }
        private static void setL(boolean defaultVal) { STABLE_OBJECT_ARRAY[0]  = defaultVal ?  null :        new Object(); }

        static void reset() {
            setZ(false);
            setB(false);
            setS(false);
            setC(false);
            setI(false);
            setJ(false);
            setF(false);
            setD(false);
            setL(false);
        }
    }

    static class Test {
        static void changeZ() { Setter.setZ(true); }
        static void changeB() { Setter.setB(true); }
        static void changeS() { Setter.setS(true); }
        static void changeC() { Setter.setC(true); }
        static void changeI() { Setter.setI(true); }
        static void changeJ() { Setter.setJ(true); }
        static void changeF() { Setter.setF(true); }
        static void changeD() { Setter.setD(true); }
        static void changeL() { Setter.setL(true); }

        static boolean testZ_Z() { return U.getBoolean(STABLE_BOOLEAN_ARRAY, ARRAY_BOOLEAN_BASE_OFFSET); }
        static byte    testZ_B() { return U.getByte(   STABLE_BOOLEAN_ARRAY, ARRAY_BOOLEAN_BASE_OFFSET); }
        static short   testZ_S() { return U.getShort(  STABLE_BOOLEAN_ARRAY, ARRAY_BOOLEAN_BASE_OFFSET); }
        static char    testZ_C() { return U.getChar(   STABLE_BOOLEAN_ARRAY, ARRAY_BOOLEAN_BASE_OFFSET); }
        static int     testZ_I() { return U.getInt(    STABLE_BOOLEAN_ARRAY, ARRAY_BOOLEAN_BASE_OFFSET); }
        static long    testZ_J() { return U.getLong(   STABLE_BOOLEAN_ARRAY, ARRAY_BOOLEAN_BASE_OFFSET); }
        static float   testZ_F() { return U.getFloat(  STABLE_BOOLEAN_ARRAY, ARRAY_BOOLEAN_BASE_OFFSET); }
        static double  testZ_D() { return U.getDouble( STABLE_BOOLEAN_ARRAY, ARRAY_BOOLEAN_BASE_OFFSET); }

        static boolean testB_Z() { return U.getBoolean(STABLE_BYTE_ARRAY, ARRAY_BYTE_BASE_OFFSET); }
        static byte    testB_B() { return U.getByte(   STABLE_BYTE_ARRAY, ARRAY_BYTE_BASE_OFFSET); }
        static short   testB_S() { return U.getShort(  STABLE_BYTE_ARRAY, ARRAY_BYTE_BASE_OFFSET); }
        static char    testB_C() { return U.getChar(   STABLE_BYTE_ARRAY, ARRAY_BYTE_BASE_OFFSET); }
        static int     testB_I() { return U.getInt(    STABLE_BYTE_ARRAY, ARRAY_BYTE_BASE_OFFSET); }
        static long    testB_J() { return U.getLong(   STABLE_BYTE_ARRAY, ARRAY_BYTE_BASE_OFFSET); }
        static float   testB_F() { return U.getFloat(  STABLE_BYTE_ARRAY, ARRAY_BYTE_BASE_OFFSET); }
        static double  testB_D() { return U.getDouble( STABLE_BYTE_ARRAY, ARRAY_BYTE_BASE_OFFSET); }

        static boolean testS_Z() { return U.getBoolean(STABLE_SHORT_ARRAY, ARRAY_SHORT_BASE_OFFSET); }
        static byte    testS_B() { return U.getByte(   STABLE_SHORT_ARRAY, ARRAY_SHORT_BASE_OFFSET); }
        static short   testS_S() { return U.getShort(  STABLE_SHORT_ARRAY, ARRAY_SHORT_BASE_OFFSET); }
        static char    testS_C() { return U.getChar(   STABLE_SHORT_ARRAY, ARRAY_SHORT_BASE_OFFSET); }
        static int     testS_I() { return U.getInt(    STABLE_SHORT_ARRAY, ARRAY_SHORT_BASE_OFFSET); }
        static long    testS_J() { return U.getLong(   STABLE_SHORT_ARRAY, ARRAY_SHORT_BASE_OFFSET); }
        static float   testS_F() { return U.getFloat(  STABLE_SHORT_ARRAY, ARRAY_SHORT_BASE_OFFSET); }
        static double  testS_D() { return U.getDouble( STABLE_SHORT_ARRAY, ARRAY_SHORT_BASE_OFFSET); }

        static boolean testC_Z() { return U.getBoolean(STABLE_CHAR_ARRAY, ARRAY_CHAR_BASE_OFFSET); }
        static byte    testC_B() { return U.getByte(   STABLE_CHAR_ARRAY, ARRAY_CHAR_BASE_OFFSET); }
        static short   testC_S() { return U.getShort(  STABLE_CHAR_ARRAY, ARRAY_CHAR_BASE_OFFSET); }
        static char    testC_C() { return U.getChar(   STABLE_CHAR_ARRAY, ARRAY_CHAR_BASE_OFFSET); }
        static int     testC_I() { return U.getInt(    STABLE_CHAR_ARRAY, ARRAY_CHAR_BASE_OFFSET); }
        static long    testC_J() { return U.getLong(   STABLE_CHAR_ARRAY, ARRAY_CHAR_BASE_OFFSET); }
        static float   testC_F() { return U.getFloat(  STABLE_CHAR_ARRAY, ARRAY_CHAR_BASE_OFFSET); }
        static double  testC_D() { return U.getDouble( STABLE_CHAR_ARRAY, ARRAY_CHAR_BASE_OFFSET); }

        static boolean testI_Z() { return U.getBoolean(STABLE_INT_ARRAY, ARRAY_INT_BASE_OFFSET); }
        static byte    testI_B() { return U.getByte(   STABLE_INT_ARRAY, ARRAY_INT_BASE_OFFSET); }
        static short   testI_S() { return U.getShort(  STABLE_INT_ARRAY, ARRAY_INT_BASE_OFFSET); }
        static char    testI_C() { return U.getChar(   STABLE_INT_ARRAY, ARRAY_INT_BASE_OFFSET); }
        static int     testI_I() { return U.getInt(    STABLE_INT_ARRAY, ARRAY_INT_BASE_OFFSET); }
        static long    testI_J() { return U.getLong(   STABLE_INT_ARRAY, ARRAY_INT_BASE_OFFSET); }
        static float   testI_F() { return U.getFloat(  STABLE_INT_ARRAY, ARRAY_INT_BASE_OFFSET); }
        static double  testI_D() { return U.getDouble( STABLE_INT_ARRAY, ARRAY_INT_BASE_OFFSET); }

        static boolean testJ_Z() { return U.getBoolean(STABLE_LONG_ARRAY, ARRAY_LONG_BASE_OFFSET); }
        static byte    testJ_B() { return U.getByte(   STABLE_LONG_ARRAY, ARRAY_LONG_BASE_OFFSET); }
        static short   testJ_S() { return U.getShort(  STABLE_LONG_ARRAY, ARRAY_LONG_BASE_OFFSET); }
        static char    testJ_C() { return U.getChar(   STABLE_LONG_ARRAY, ARRAY_LONG_BASE_OFFSET); }
        static int     testJ_I() { return U.getInt(    STABLE_LONG_ARRAY, ARRAY_LONG_BASE_OFFSET); }
        static long    testJ_J() { return U.getLong(   STABLE_LONG_ARRAY, ARRAY_LONG_BASE_OFFSET); }
        static float   testJ_F() { return U.getFloat(  STABLE_LONG_ARRAY, ARRAY_LONG_BASE_OFFSET); }
        static double  testJ_D() { return U.getDouble( STABLE_LONG_ARRAY, ARRAY_LONG_BASE_OFFSET); }

        static boolean testF_Z() { return U.getBoolean(STABLE_FLOAT_ARRAY, ARRAY_FLOAT_BASE_OFFSET); }
        static byte    testF_B() { return U.getByte(   STABLE_FLOAT_ARRAY, ARRAY_FLOAT_BASE_OFFSET); }
        static short   testF_S() { return U.getShort(  STABLE_FLOAT_ARRAY, ARRAY_FLOAT_BASE_OFFSET); }
        static char    testF_C() { return U.getChar(   STABLE_FLOAT_ARRAY, ARRAY_FLOAT_BASE_OFFSET); }
        static int     testF_I() { return U.getInt(    STABLE_FLOAT_ARRAY, ARRAY_FLOAT_BASE_OFFSET); }
        static long    testF_J() { return U.getLong(   STABLE_FLOAT_ARRAY, ARRAY_FLOAT_BASE_OFFSET); }
        static float   testF_F() { return U.getFloat(  STABLE_FLOAT_ARRAY, ARRAY_FLOAT_BASE_OFFSET); }
        static double  testF_D() { return U.getDouble( STABLE_FLOAT_ARRAY, ARRAY_FLOAT_BASE_OFFSET); }

        static boolean testD_Z() { return U.getBoolean(STABLE_DOUBLE_ARRAY, ARRAY_DOUBLE_BASE_OFFSET); }
        static byte    testD_B() { return U.getByte(   STABLE_DOUBLE_ARRAY, ARRAY_DOUBLE_BASE_OFFSET); }
        static short   testD_S() { return U.getShort(  STABLE_DOUBLE_ARRAY, ARRAY_DOUBLE_BASE_OFFSET); }
        static char    testD_C() { return U.getChar(   STABLE_DOUBLE_ARRAY, ARRAY_DOUBLE_BASE_OFFSET); }
        static int     testD_I() { return U.getInt(    STABLE_DOUBLE_ARRAY, ARRAY_DOUBLE_BASE_OFFSET); }
        static long    testD_J() { return U.getLong(   STABLE_DOUBLE_ARRAY, ARRAY_DOUBLE_BASE_OFFSET); }
        static float   testD_F() { return U.getFloat(  STABLE_DOUBLE_ARRAY, ARRAY_DOUBLE_BASE_OFFSET); }
        static double  testD_D() { return U.getDouble( STABLE_DOUBLE_ARRAY, ARRAY_DOUBLE_BASE_OFFSET); }

        static Object  testL_L() { return U.getReference( STABLE_OBJECT_ARRAY, ARRAY_OBJECT_BASE_OFFSET); }
        static boolean testL_Z() { return U.getBoolean(STABLE_OBJECT_ARRAY, ARRAY_OBJECT_BASE_OFFSET);    }
        static byte    testL_B() { return U.getByte(   STABLE_OBJECT_ARRAY, ARRAY_OBJECT_BASE_OFFSET);    }
        static short   testL_S() { return U.getShort(  STABLE_OBJECT_ARRAY, ARRAY_OBJECT_BASE_OFFSET);    }
        static char    testL_C() { return U.getChar(   STABLE_OBJECT_ARRAY, ARRAY_OBJECT_BASE_OFFSET);    }
        static int     testL_I() { return U.getInt(    STABLE_OBJECT_ARRAY, ARRAY_OBJECT_BASE_OFFSET);    }
        static long    testL_J() { return U.getLong(   STABLE_OBJECT_ARRAY, ARRAY_OBJECT_BASE_OFFSET);    }
        static float   testL_F() { return U.getFloat(  STABLE_OBJECT_ARRAY, ARRAY_OBJECT_BASE_OFFSET);    }
        static double  testL_D() { return U.getDouble( STABLE_OBJECT_ARRAY, ARRAY_OBJECT_BASE_OFFSET);    }

        static short   testS_U() { return U.getShortUnaligned(STABLE_SHORT_ARRAY, ARRAY_SHORT_BASE_OFFSET + 1); }
        static char    testC_U() { return U.getCharUnaligned(  STABLE_CHAR_ARRAY,  ARRAY_CHAR_BASE_OFFSET + 1); }
        static int     testI_U() { return U.getIntUnaligned(    STABLE_INT_ARRAY,   ARRAY_INT_BASE_OFFSET + 1); }
        static long    testJ_U() { return U.getLongUnaligned(  STABLE_LONG_ARRAY,  ARRAY_LONG_BASE_OFFSET + 1); }
    }

    static void run(Callable<?> c) throws Exception {
        run(c, null, null);
    }

    static void run(Callable<?> c, Runnable sameResultAction, Runnable changeResultAction) throws Exception {
        Object first = c.call();

        // Trigger compilation.
        for (int i = 0; i < 20_000; i++) {
            // Don't compare results here, since most of Test::testL_* results vary across iterations (due to GC).
            c.call();
        }

        if (sameResultAction != null) {
            sameResultAction.run();
            assertEQ(first, c.call());
        }

        if (changeResultAction != null) {
            changeResultAction.run();
            assertNE(first, c.call());
            assertEQ(c.call(), c.call());
        }
    }

    static void testMatched(Callable<?> c, Runnable setDefaultAction) throws Exception {
        run(c, setDefaultAction, null);
        Setter.reset();
    }

    static void testMismatched(Callable<?> c, Runnable setDefaultAction) throws Exception {
        testMismatched(c, setDefaultAction, false);
    }

    static void testMismatched(Callable<?> c, Runnable setDefaultAction, boolean objectArray) throws Exception {
        if (Compiler.isGraalEnabled() && !objectArray) {
            // Graal will constant fold mismatched reads from primitive stable arrays
            run(c, setDefaultAction, null);
        } else {
            run(c, null, setDefaultAction);
        }
        Setter.reset();
    }

    static void testUnsafeAccess() throws Exception {
        // boolean[], aligned accesses
        testMatched(   Test::testZ_Z, Test::changeZ);
        testMismatched(Test::testZ_B, Test::changeZ);
        testMismatched(Test::testZ_S, Test::changeZ);
        testMismatched(Test::testZ_C, Test::changeZ);
        testMismatched(Test::testZ_I, Test::changeZ);
        testMismatched(Test::testZ_J, Test::changeZ);
        testMismatched(Test::testZ_F, Test::changeZ);
        testMismatched(Test::testZ_D, Test::changeZ);

        // byte[], aligned accesses
        testMismatched(Test::testB_Z, Test::changeB);
        testMatched(   Test::testB_B, Test::changeB);
        testMismatched(Test::testB_S, Test::changeB);
        testMismatched(Test::testB_C, Test::changeB);
        testMismatched(Test::testB_I, Test::changeB);
        testMismatched(Test::testB_J, Test::changeB);
        testMismatched(Test::testB_F, Test::changeB);
        testMismatched(Test::testB_D, Test::changeB);

        // short[], aligned accesses
        testMismatched(Test::testS_Z, Test::changeS);
        testMismatched(Test::testS_B, Test::changeS);
        testMatched(   Test::testS_S, Test::changeS);
        testMismatched(Test::testS_C, Test::changeS);
        testMismatched(Test::testS_I, Test::changeS);
        testMismatched(Test::testS_J, Test::changeS);
        testMismatched(Test::testS_F, Test::changeS);
        testMismatched(Test::testS_D, Test::changeS);

        // char[], aligned accesses
        testMismatched(Test::testC_Z, Test::changeC);
        testMismatched(Test::testC_B, Test::changeC);
        testMismatched(Test::testC_S, Test::changeC);
        testMatched(   Test::testC_C, Test::changeC);
        testMismatched(Test::testC_I, Test::changeC);
        testMismatched(Test::testC_J, Test::changeC);
        testMismatched(Test::testC_F, Test::changeC);
        testMismatched(Test::testC_D, Test::changeC);

        // int[], aligned accesses
        testMismatched(Test::testI_Z, Test::changeI);
        testMismatched(Test::testI_B, Test::changeI);
        testMismatched(Test::testI_S, Test::changeI);
        testMismatched(Test::testI_C, Test::changeI);
        testMatched(   Test::testI_I, Test::changeI);
        testMismatched(Test::testI_J, Test::changeI);
        testMismatched(Test::testI_F, Test::changeI);
        testMismatched(Test::testI_D, Test::changeI);

        // long[], aligned accesses
        testMismatched(Test::testJ_Z, Test::changeJ);
        testMismatched(Test::testJ_B, Test::changeJ);
        testMismatched(Test::testJ_S, Test::changeJ);
        testMismatched(Test::testJ_C, Test::changeJ);
        testMismatched(Test::testJ_I, Test::changeJ);
        testMatched(   Test::testJ_J, Test::changeJ);
        testMismatched(Test::testJ_F, Test::changeJ);
        testMismatched(Test::testJ_D, Test::changeJ);

        // float[], aligned accesses
        testMismatched(Test::testF_Z, Test::changeF);
        testMismatched(Test::testF_B, Test::changeF);
        testMismatched(Test::testF_S, Test::changeF);
        testMismatched(Test::testF_C, Test::changeF);
        testMismatched(Test::testF_I, Test::changeF);
        testMismatched(Test::testF_J, Test::changeF);
        testMatched(   Test::testF_F, Test::changeF);
        testMismatched(Test::testF_D, Test::changeF);

        // double[], aligned accesses
        testMismatched(Test::testD_Z, Test::changeD);
        testMismatched(Test::testD_B, Test::changeD);
        testMismatched(Test::testD_S, Test::changeD);
        testMismatched(Test::testD_C, Test::changeD);
        testMismatched(Test::testD_I, Test::changeD);
        testMismatched(Test::testD_J, Test::changeD);
        testMismatched(Test::testD_F, Test::changeD);
        testMatched(   Test::testD_D, Test::changeD);

        // Object[], aligned accesses
        testMismatched(Test::testL_J, Test::changeL, true); // long & double are always as large as an OOP
        testMismatched(Test::testL_D, Test::changeL, true);
        testMatched(   Test::testL_L, Test::changeL);

        // Unaligned accesses
        testMismatched(Test::testS_U, Test::changeS);
        testMismatched(Test::testC_U, Test::changeC);
        testMismatched(Test::testI_U, Test::changeI);
        testMismatched(Test::testJ_U, Test::changeJ);

        // No way to reliably check the expected behavior:
        //   (1) OOPs change during GC;
        //   (2) there's no way to reliably change some part of an OOP (e.g., when reading a byte from it).
        //
        // Just trigger the compilation hoping to catch any problems with asserts.
        run(Test::testL_B);
        run(Test::testL_Z);
        run(Test::testL_S);
        run(Test::testL_C);
        run(Test::testL_I);
        run(Test::testL_F);
    }

    public static void main(String[] args) throws Exception {
        if (!Platform.isServer() || Platform.isEmulatedClient()) {
            throw new Error("TESTBUG: Not server mode");
        }
        testUnsafeAccess();
        System.out.println("TEST PASSED");
    }
}
