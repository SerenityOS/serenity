/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2019 SAP SE. All rights reserved.
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
 * @key randomness
 * @summary Test extended NullPointerException message for
 *   classfiles generated with debug information. In this case the name
 *   of the variable containing the array is printed.
 * @bug 8218628 8248476
 * @modules java.base/java.lang:open
 *          java.base/jdk.internal.org.objectweb.asm
 * @library /test/lib
 * @compile -g NullPointerExceptionTest.java
 * @run main/othervm -XX:MaxJavaStackTraceDepth=1 -XX:+ShowCodeDetailsInExceptionMessages NullPointerExceptionTest hasDebugInfo
 */
/**
 * @test
 * @key randomness
 * @summary Test extended NullPointerException message for class
 *   files generated without debugging information. The message lists
 *   detailed information about the entity that is null.
 * @bug 8218628 8248476
 * @modules java.base/java.lang:open
 *          java.base/jdk.internal.org.objectweb.asm
 * @library /test/lib
 * @compile NullPointerExceptionTest.java
 * @run main/othervm -XX:MaxJavaStackTraceDepth=1 -XX:+ShowCodeDetailsInExceptionMessages NullPointerExceptionTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.ArrayList;
import java.util.Random;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;

import static java.lang.invoke.MethodHandles.lookup;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_SUPER;
import static jdk.internal.org.objectweb.asm.Opcodes.ACONST_NULL;
import static jdk.internal.org.objectweb.asm.Opcodes.ALOAD;
import static jdk.internal.org.objectweb.asm.Opcodes.ASTORE;
import static jdk.internal.org.objectweb.asm.Opcodes.GETFIELD;
import static jdk.internal.org.objectweb.asm.Opcodes.GETSTATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ICONST_1;
import static jdk.internal.org.objectweb.asm.Opcodes.ICONST_2;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static jdk.internal.org.objectweb.asm.Opcodes.ARETURN;
import static jdk.internal.org.objectweb.asm.Opcodes.IRETURN;
import static jdk.internal.org.objectweb.asm.Opcodes.RETURN;

/**
 * Tests NullPointerExceptions
 */
public class NullPointerExceptionTest {

    // Some fields used in the test.
    static Object nullStaticField;
    NullPointerExceptionTest nullInstanceField;
    static int[][][][] staticArray;
    static long[][] staticLongArray = new long[1000][];
    DoubleArrayGen dag;
    ArrayList<String> names = new ArrayList<>();
    ArrayList<String> curr;
    static boolean hasDebugInfo = false;
    static final Random rng = Utils.getRandomInstance();

    static {
        staticArray       = new int[1][][][];
        staticArray[0]    = new int[1][][];
        staticArray[0][0] = new int[1][];
    }

    public static void checkMessage(Throwable t, String expression,
                                    String obtainedMsg, String expectedMsg) {
        System.out.println("\nSource code:\n  " + expression + "\n\nOutput:");
        t.printStackTrace(System.out);
        if (expectedMsg != null && obtainedMsg == null) {
            Asserts.fail("Got null but expected this message: \"" + expectedMsg + "\".");
        }
        if (obtainedMsg != expectedMsg && // E.g. both are null.
            !obtainedMsg.equals(expectedMsg)) {
            System.out.println("expected msg: " + expectedMsg);
            Asserts.assertEquals(expectedMsg, obtainedMsg);
        }
        System.out.println("\n----");
    }

    public static void main(String[] args) throws Exception {
        NullPointerExceptionTest t = new NullPointerExceptionTest();
        if (args.length > 0) {
            hasDebugInfo = true;
        }

        System.out.println("Tests for the first part of the message:");
        System.out.println("========================================\n");

        // Test the message printed for the failed action.
        t.testFailedAction();

        System.out.println("Tests for the second part of the message:");
        System.out.println("=========================================\n");
        // Test the method printed for the null entity.
        t.testNullEntity();

        System.out.println("Further tests:");
        System.out.println("==============\n");

        // Test if parameters are used in the code.
        // This is relevant if there is no debug information.
        t.testParameters();

        // Test that no message is printed for exceptions
        // allocated explicitly.
        t.testCreation();

        // Test that no message is printed for exceptions
        // thrown in native methods.
        t.testNative();

        // Test that two calls to getMessage() return the same
        // message.
        // It is a design decision that it returns two different
        // String objects.
        t.testSameMessage();

        // Test serialization.
        // It is a design decision that after serialization the
        // the message is lost.
        t.testSerialization();

        // Test that messages are printed for code generated
        // on-the-fly.
        t.testGeneratedCode();

        // Some more interesting complex messages.
        t.testComplexMessages();
    }

    // Helper method to cause test case.
    private double callWithTypes(String[][] dummy1, int[][][] dummy2, float dummy3, long dummy4, short dummy5,
                                 boolean dummy6, byte dummy7, double dummy8, char dummy9) {
        return 0.0;
    }

    @SuppressWarnings("null")
    public void testFailedAction() {
        int[]     ia1 = null;
        float[]   fa1 = null;
        Object[]  oa1 = null;
        boolean[] za1 = null;
        byte[]    ba1 = null;
        char[]    ca1 = null;
        short[]   sa1 = null;
        long[]    la1 = null;
        double[]  da1 = null;

        // iaload
        try {
            int val = ia1[0];
            System.out.println(val);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "int val = ia1[0];", e.getMessage(),
                         "Cannot load from int array because " +
                         (hasDebugInfo ? "\"ia1\"" : "\"<local1>\"") + " is null");
        }
        // faload
        try {
            float val = fa1[0];
            System.out.println(val);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "float val = fa1[0];", e.getMessage(),
                         "Cannot load from float array because " +
                         (hasDebugInfo ? "\"fa1\"" : "\"<local2>\"") + " is null");
        }
        // aaload
        try {
            Object val = oa1[0];
            System.out.println(val);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "Object val = oa1[0];", e.getMessage(),
                         "Cannot load from object array because " +
                         (hasDebugInfo ? "\"oa1\"" : "\"<local3>\"") + " is null");
        }
        // baload (boolean)
        try {
            boolean val = za1[0];
            System.out.println(val);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "boolean val = za1[0];", e.getMessage(),
                         "Cannot load from byte/boolean array because " +
                         (hasDebugInfo ? "\"za1\"" : "\"<local4>\"") + " is null");
        }
        // baload (byte)
        try {
            byte val = ba1[0];
            System.out.println(val);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "byte val = ba1[0];", e.getMessage(),
                         "Cannot load from byte/boolean array because " +
                         (hasDebugInfo ? "\"ba1\"" : "\"<local5>\"") + " is null");
        }
        // caload
        try {
            char val = ca1[0];
            System.out.println(val);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "char val = ca1[0];", e.getMessage(),
                         "Cannot load from char array because " +
                         (hasDebugInfo ? "\"ca1\"" : "\"<local6>\"") + " is null");
        }
        // saload
        try {
            short val = sa1[0];
            System.out.println(val);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "short val = sa1[0];", e.getMessage(),
                         "Cannot load from short array because " +
                         (hasDebugInfo ? "\"sa1\"" : "\"<local7>\"") + " is null");
        }
        // laload
        try {
            long val = la1[0];
            System.out.println(val);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "long val = la1[0];", e.getMessage(),
                         "Cannot load from long array because " +
                         (hasDebugInfo ? "\"la1\"" : "\"<local8>\"") + " is null");
        }
        // daload
        try {
            double val = da1[0];
            System.out.println(val);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "double val = da1[0];", e.getMessage(),
                         "Cannot load from double array because " +
                         (hasDebugInfo ? "\"da1\"" : "\"<local9>\"") + " is null");
        }

        // iastore
        try {
            ia1[0] = 0;
            System.out.println(ia1[0]);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "ia1[0] = 0;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"ia1\"" : "\"<local1>\"") + " is null");
        }
        // fastore
        try {
            fa1[0] = 0.7f;
            System.out.println(fa1[0]);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "fa1[0] = 0.7f;", e.getMessage(),
                         "Cannot store to float array because " +
                         (hasDebugInfo ? "\"fa1\"" : "\"<local2>\"") + " is null");
        }
        // aastore
        try {
            oa1[0] = new Object();
            System.out.println(oa1[0]);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "oa1[0] = new Object();", e.getMessage(),
                         "Cannot store to object array because " +
                         (hasDebugInfo ? "\"oa1\"" : "\"<local3>\"") + " is null");
        }
        // bastore (boolean)
        try {
            za1[0] = false;
            System.out.println(za1[0]);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "za1[0] = false;", e.getMessage(),
                         "Cannot store to byte/boolean array because " +
                         (hasDebugInfo ? "\"za1\"" : "\"<local4>\"") + " is null");
        }
        // bastore (byte)
        try {
            ba1[0] = 0;
            System.out.println(ba1[0]);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "ba1[0] = 0;", e.getMessage(),
                         "Cannot store to byte/boolean array because " +
                         (hasDebugInfo ? "\"ba1\"" : "\"<local5>\"") + " is null");
        }
        // castore
        try {
            ca1[0] = 0;
            System.out.println(ca1[0]);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "ca1[0] = 0;", e.getMessage(),
                         "Cannot store to char array because " +
                         (hasDebugInfo ? "\"ca1\"" : "\"<local6>\"") + " is null");
        }
        // sastore
        try {
            sa1[0] = 0;
            System.out.println(sa1[0]);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "sa1[0] = 0;", e.getMessage(),
                         "Cannot store to short array because " +
                         (hasDebugInfo ? "\"sa1\"" : "\"<local7>\"") + " is null");
        }
        // lastore
        try {
            la1[0] = 0;
            System.out.println(la1[0]);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "la1[0] = 0;", e.getMessage(),
                         "Cannot store to long array because " +
                         (hasDebugInfo ? "\"la1\"" : "\"<local8>\"") + " is null");
        }
        // dastore
        try {
            da1[0] = 0;
            System.out.println(da1[0]);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "da1[0] = 0;", e.getMessage(),
                         "Cannot store to double array because " +
                         (hasDebugInfo ? "\"da1\"" : "\"<local9>\"") + " is null");
        }

        // arraylength
        try {
            int val = za1.length;
            System.out.println(val);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, " int val = za1.length;", e.getMessage(),
                         "Cannot read the array length because " +
                         (hasDebugInfo ? "\"za1\"" : "\"<local4>\"") + " is null");
        }
        // athrow
        try {
            RuntimeException exc = null;
            throw exc;
        } catch (NullPointerException e) {
            checkMessage(e, "throw exc;", e.getMessage(),
                         "Cannot throw exception because " +
                         (hasDebugInfo ? "\"exc\"" : "\"<local10>\"") + " is null");
        }
        // monitorenter
        try {
            synchronized (nullInstanceField) {
                // desired
            }
        } catch (NullPointerException e) {
            checkMessage(e, "synchronized (nullInstanceField) { ... }", e.getMessage(),
                         "Cannot enter synchronized block because " +
                         "\"this.nullInstanceField\" is null");
        }
        // monitorexit
        // No test available

        // getfield
        try {
            Object val = nullInstanceField.nullInstanceField;
            System.out.println(val);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "Object val = nullInstanceField.nullInstanceField;", e.getMessage(),
                         "Cannot read field \"nullInstanceField\" because " +
                         "\"this.nullInstanceField\" is null");
        }
        // putfield
        try {
            nullInstanceField.nullInstanceField = new NullPointerExceptionTest();
            System.out.println(nullInstanceField.nullInstanceField);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "nullInstanceField.nullInstanceField = new NullPointerExceptionTest();", e.getMessage(),
                         "Cannot assign field \"nullInstanceField\" because " +
                         "\"this.nullInstanceField\" is null");
        }
        // invokevirtual
        try {
            String val = nullInstanceField.toString();
            System.out.println(val);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "String val = nullInstanceField.toString();", e.getMessage(),
                         "Cannot invoke \"Object.toString()\" because " +
                         "\"this.nullInstanceField\" is null");
        }
        // invokeinterface
        try {
            NullPointerExceptionTest obj = this;
            Object val = obj.dag.getArray();
            Asserts.assertNull(val);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "Object val = obj.dag.getArray();", e.getMessage(),
                         "Cannot invoke \"NullPointerExceptionTest$DoubleArrayGen.getArray()\" because " +
                         (hasDebugInfo ? "\"obj" : "\"<local10>") + ".dag\" is null");
        }
        // invokespecial
        G g = null;
        try {
            byte[] classBytes = G.generateSub2GTestClass();
            Lookup lookup = lookup();
            Class<?> clazz = lookup.defineClass(classBytes);
            g = (G) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            Asserts.fail("Generating class Sub2G failed.");
        }
        try {
            g.m2("Beginning");
        } catch (NullPointerException e) {
            checkMessage(e, "return super.m2(x).substring(2); // ... where super is null by bytecode manipulation.", e.getMessage(),
                         "Cannot invoke \"G.m2(String)\" because \"null\" is null");
        }
        // Test parameter and return types
        try {
            boolean val = (nullInstanceField.callWithTypes(null, null, 0.0f, 0L, (short)0, false, (byte)0, 0.0, 'x') == 0.0);
            Asserts.assertTrue(val);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "boolean val = (nullInstanceField.callWithTypes(null, null, 0.0f, 0L, (short)0, false, (byte)0, 0.0, 'x') == 0.0);", e.getMessage(),
                         "Cannot invoke \"NullPointerExceptionTest.callWithTypes(String[][], int[][][], float, long, short, boolean, byte, double, char)\" because " +
                         "\"this.nullInstanceField\" is null");
        }
    }

    static void test_iload() {
        int i0 = 0;
        int i1 = 1;
        int i2 = 2;
        int i3 = 3;
        @SuppressWarnings("unused")
        int i4 = 4;
        int i5 = 5;

        int[][] a = new int[6][];

        // iload_0
        try {
            a[i0][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[i0][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[i0]\"" : "\"<local6>[<local0>]\"") + " is null");
        }
        // iload_1
        try {
            a[i1][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[i1][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[i1]\"" : "\"<local6>[<local1>]\"") + " is null");
        }
        // iload_2
        try {
            a[i2][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[i2][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[i2]\"" : "\"<local6>[<local2>]\"") + " is null");
        }
        // iload_3
        try {
            a[i3][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[i3][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[i3]\"" : "\"<local6>[<local3>]\"") + " is null");
        }
        // iload
        try {
            a[i5][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[i5][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[i5]\"" : "\"<local6>[<local5>]\"") + " is null");
        }
    }

    // Other datatyes than int are not needed.
    // If we implement l2d and similar bytecodes, we can print
    // long expressions as array indexes. Then these here could
    // be used.
    static void test_lload() {
        long long0 = 0L;  // l0 looks like 10. Therefore long0.
        long long1 = 1L;
        long long2 = 2L;
        long long3 = 3L;
        @SuppressWarnings("unused")
        long long4 = 4L;
        long long5 = 5L;

        int[][] a = new int[6][];

        // lload_0
        try {
            a[(int)long0][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[(int)long0][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[...]\"" : "\"<local12>[...]\"") + " is null");
        }
        // lload_1
        try {
            a[(int)long1][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[(int)long1][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[...]\"" : "\"<local12>[...]\"") + " is null");
        }
        // lload_2
        try {
            a[(int)long2][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[(int)long2][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[...]\"" : "\"<local12>[...]\"") + " is null");
        }
        // lload_3
        try {
            a[(int)long3][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[(int)long3][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[...]\"" : "\"<local12>[...]\"") + " is null");
        }
        // lload
        try {
            a[(int)long5][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[(int)long5][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[...]\"" : "\"<local12>[...]\"") + " is null");
        }
    }

    static void test_fload() {
        float f0 = 0.0f;
        float f1 = 1.0f;
        float f2 = 2.0f;
        float f3 = 3.0f;
        @SuppressWarnings("unused")
        float f4 = 4.0f;
        float f5 = 5.0f;

        int[][] a = new int[6][];

        // fload_0
        try {
            a[(int)f0][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[(int)f0][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[...]\"" : "\"<local6>[...]\"") + " is null");
        }
        // fload_1
        try {
            a[(int)f1][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[(int)f1][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[...]\"" : "\"<local6>[...]\"") + " is null");
        }
        // fload_2
        try {
            a[(int)f2][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[(int)f2][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[...]\"" : "\"<local6>[...]\"") + " is null");
        }
        // fload_3
        try {
            a[(int)f3][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[(int)f3][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[...]\"" : "\"<local6>[...]\"") + " is null");
        }
        // fload
        try {
            a[(int)f5][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[(int)f5][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[...]\"" : "\"<local6>[...]\"") + " is null");
        }
    }

    @SuppressWarnings("null")
    static void test_aload() {
        F f0 = null;
        F f1 = null;
        F f2 = null;
        F f3 = null;
        @SuppressWarnings("unused")
        F f4 = null;
        F f5 = null;

        // aload_0
        try {
            f0.i = 33;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "f0.i = 33;", e.getMessage(),
                         "Cannot assign field \"i\" because " +
                         (hasDebugInfo ? "\"f0\"" : "\"<local0>\"") + " is null");
        }
        // aload_1
        try {
            f1.i = 33;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "f1.i = 33;", e.getMessage(),
                         "Cannot assign field \"i\" because " +
                         (hasDebugInfo ? "\"f1\"" : "\"<local1>\"") + " is null");
        }
        // aload_2
        try {
            f2.i = 33;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "f2.i = 33;", e.getMessage(),
                         "Cannot assign field \"i\" because " +
                         (hasDebugInfo ? "\"f2\"" : "\"<local2>\"") + " is null");
        }
        // aload_3
        try {
            f3.i = 33;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "f3.i = 33;", e.getMessage(),
                         "Cannot assign field \"i\" because " +
                         (hasDebugInfo ? "\"f3\"" : "\"<local3>\"") + " is null");
        }
        // aload
        try {
            f5.i = 33;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "f5.i = 33;", e.getMessage(),
                         "Cannot assign field \"i\" because " +
                         (hasDebugInfo ? "\"f5\"" : "\"<local5>\"") + " is null");
        }
    }

    // Helper class for test cases.
    class A {
        public B to_b;
        public B getB() { return to_b; }
    }

    // Helper class for test cases.
    class B {
        public C to_c;
        public B to_b;
        public C getC() { return to_c; }
        public B getBfromB() { return to_b; }
    }

    // Helper class for test cases.
    class C {
        public D to_d;
        public D getD() { return to_d; }
    }

    // Helper class for test cases.
    class D {
        public int num;
        public int[][] ar;
    }


    @SuppressWarnings("null")
    public void testArrayChasing() {
        int[][][][][][] a = null;
        try {
            a[0][0][0][0][0][0] = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[0][0][0][0][0] = 99; // a is null", e.getMessage(),
                         "Cannot load from object array because " +
                         (hasDebugInfo ? "\"a\"" : "\"<local1>\"") + " is null");
        }
        a = new int[1][][][][][];
        try {
            a[0][0][0][0][0][0] = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[0][0][0][0][0] = 99; // a[0] is null", e.getMessage(),
                         "Cannot load from object array because " +
                         (hasDebugInfo ? "\"a[0]\"" : "\"<local1>[0]\"") + " is null");
        }
        a[0] = new int[1][][][][];
        try {
            a[0][0][0][0][0][0] = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[0][0][0][0][0] = 99; // a[0][0] is null", e.getMessage(),
                         "Cannot load from object array because " +
                         (hasDebugInfo ? "\"a[0][0]\"" : "\"<local1>[0][0]\"") + " is null");
        }
        a[0][0] = new int[1][][][];
        try {
            a[0][0][0][0][0][0] = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[0][0][0][0][0] = 99; // a[0][0][0] is null", e.getMessage(),
                         "Cannot load from object array because " +
                         (hasDebugInfo ? "\"a[0][0][0]\"" : "\"<local1>[0][0][0]\"") + " is null");
        }
        try {
            System.out.println(a[0][0][0].length);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[0][0][0].length; // a[0][0][0] is null", e.getMessage(),
                         "Cannot read the array length because " +
                         (hasDebugInfo ? "\"a[0][0][0]\"" : "\"<local1>[0][0][0]\"") + " is null");
        }
        a[0][0][0] = new int[1][][];
        try {
            a[0][0][0][0][0][0] = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[0][0][0][0][0] = 99; // a[0][0][0][0] is null", e.getMessage(),
                         "Cannot load from object array because " +
                         (hasDebugInfo ? "\"a[0][0][0][0]\"" : "\"<local1>[0][0][0][0]\"") + " is null");
        }
        a[0][0][0][0] = new int[1][];
        // Reaching max recursion depth. Prints <array>.
        try {
            a[0][0][0][0][0][0] = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[0][0][0][0][0] = 99; // a[0][0][0][0][0] is null", e.getMessage(),
                         "Cannot store to int array because " +
                         "\"<array>[0][0][0][0][0]\" is null");
        }
        a[0][0][0][0][0] = new int[1];
        try {
            a[0][0][0][0][0][0] = 99;
        } catch (NullPointerException e) {
            Asserts.fail();
        }
    }

    @SuppressWarnings("null")
    public void testPointerChasing() {
        A a = null;
        try {
            a.to_b.to_c.to_d.num = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a.to_b.to_c.to_d.num = 99; // a is null", e.getMessage(),
                         "Cannot read field \"to_b\" because " +
                         (hasDebugInfo ? "\"a\"" : "\"<local1>\"") + " is null");
        }
        a = new A();
        try {
            a.to_b.to_c.to_d.num = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a.to_b.to_c.to_d.num = 99; // a.to_b is null", e.getMessage(),
                         "Cannot read field \"to_c\" because " +
                         (hasDebugInfo ? "\"a.to_b\"" : "\"<local1>.to_b\"") + " is null");
        }
        a.to_b = new B();
        try {
            a.to_b.to_c.to_d.num = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a.to_b.to_c.to_d.num = 99; // a.to_b.to_c is null", e.getMessage(),
                         "Cannot read field \"to_d\" because " +
                         (hasDebugInfo ? "\"a.to_b.to_c\"" : "\"<local1>.to_b.to_c\"") + " is null");
        }
        a.to_b.to_c = new C();
        try {
            a.to_b.to_c.to_d.num = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a.to_b.to_c.to_d.num = 99; // a.to_b.to_c.to_d is null", e.getMessage(),
                         "Cannot assign field \"num\" because " +
                         (hasDebugInfo ? "\"a.to_b.to_c.to_d\"" : "\"<local1>.to_b.to_c.to_d\"") + " is null");
        }
    }

    @SuppressWarnings("null")
    public void testMethodChasing() {
        A a = null;
        try {
            a.getB().getBfromB().getC().getD().num = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a.getB().getBfromB().getC().getD().num = 99; // a is null", e.getMessage(),
                         "Cannot invoke \"NullPointerExceptionTest$A.getB()\" because " +
                         (hasDebugInfo ? "\"a" : "\"<local1>") + "\" is null");
        }
        a = new A();
        try {
            a.getB().getBfromB().getC().getD().num = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a.getB().getBfromB().getC().getD().num = 99; // a.getB() is null", e.getMessage(),
                         "Cannot invoke \"NullPointerExceptionTest$B.getBfromB()\" because " +
                         "the return value of \"NullPointerExceptionTest$A.getB()\" is null");
        }
        a.to_b = new B();
        try {
            a.getB().getBfromB().getC().getD().num = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a.getB().getBfromB().getC().getD().num = 99; // a.getB().getBfromB() is null", e.getMessage(),
                         "Cannot invoke \"NullPointerExceptionTest$B.getC()\" because " +
                         "the return value of \"NullPointerExceptionTest$B.getBfromB()\" is null");
        }
        a.to_b.to_b = new B();
        try {
            a.getB().getBfromB().getC().getD().num = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a.getB().getBfromB().getC().getD().num = 99; // a.getB().getBfromB().getC() is null", e.getMessage(),
                         "Cannot invoke \"NullPointerExceptionTest$C.getD()\" because " +
                         "the return value of \"NullPointerExceptionTest$B.getC()\" is null");
        }
        a.to_b.to_b.to_c = new C();
        try {
            a.getB().getBfromB().getC().getD().num = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a.getB().getBfromB().getC().getD().num = 99; // a.getB().getBfromB().getC().getD() is null", e.getMessage(),
                         "Cannot assign field \"num\" because " +
                         "the return value of \"NullPointerExceptionTest$C.getD()\" is null");
        }
    }

    @SuppressWarnings("null")
    public void testMixedChasing() {
        A a = null;
        try {
            a.getB().getBfromB().to_c.to_d.ar[0][0] = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a.getB().getBfromB().to_c.to_d.ar[0][0] = 99; // a is null", e.getMessage(),
                         "Cannot invoke \"NullPointerExceptionTest$A.getB()\" because " +
                         (hasDebugInfo ? "\"a\"" : "\"<local1>\"") + " is null");
        }
        a = new A();
        try {
            a.getB().getBfromB().to_c.to_d.ar[0][0] = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a.getB().getBfromB().to_c.to_d.ar[0][0] = 99; // a.getB() is null", e.getMessage(),
                         "Cannot invoke \"NullPointerExceptionTest$B.getBfromB()\" because " +
                         "the return value of \"NullPointerExceptionTest$A.getB()\" is null");
        }
        a.to_b = new B();
        try {
            a.getB().getBfromB().to_c.to_d.ar[0][0] = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a.getB().getBfromB().to_c.to_d.ar[0][0] = 99; // a.getB().getBfromB() is null", e.getMessage(),
                         "Cannot read field \"to_c\" because " +
                         "the return value of \"NullPointerExceptionTest$B.getBfromB()\" is null");
        }
        a.to_b.to_b = new B();
        try {
            a.getB().getBfromB().to_c.to_d.ar[0][0] = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a.getB().getBfromB().to_c.to_d.ar[0][0] = 99; // a.getB().getBfromB().to_c is null", e.getMessage(),
                         "Cannot read field \"to_d\" because " +
                         "\"NullPointerExceptionTest$B.getBfromB().to_c\" is null");
        }
        a.to_b.to_b.to_c = new C();
        try {
            a.getB().getBfromB().to_c.to_d.ar[0][0] = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a.getB().getBfromB().to_c.to_d.ar[0][0] = 99; // a.getB().getBfromB().to_c.to_d is null", e.getMessage(),
                         "Cannot read field \"ar\" because " +
                         "\"NullPointerExceptionTest$B.getBfromB().to_c.to_d\" is null");
        }
        a.to_b.to_b.to_c.to_d = new D();
        try {
            a.getB().getBfromB().to_c.to_d.ar[0][0] = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a.getB().getBfromB().to_c.to_d.ar[0][0] = 99; // a.getB().getBfromB().to_c.to_d.ar is null", e.getMessage(),
                         "Cannot load from object array because " +
                         "\"NullPointerExceptionTest$B.getBfromB().to_c.to_d.ar\" is null");
        }
        try {
            a.getB().getBfromB().getC().getD().ar[0][0] = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a.getB().getBfromB().getC().getD().ar[0][0] = 99; // a.getB().getBfromB().getC().getD().ar is null", e.getMessage(),
                         "Cannot load from object array because " +
                         "\"NullPointerExceptionTest$C.getD().ar\" is null");
        }
        a.to_b.to_b.to_c.to_d.ar = new int[1][];
        try {
            a.getB().getBfromB().to_c.to_d.ar[0][0] = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a.getB().getBfromB().to_c.to_d.ar[0][0] = 99; // a.getB().getBfromB().to_c.to_d.ar[0] is null", e.getMessage(),
                         "Cannot store to int array because " +
                         "\"NullPointerExceptionTest$B.getBfromB().to_c.to_d.ar[0]\" is null");
        }
        try {
            a.getB().getBfromB().getC().getD().ar[0][0] = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a.getB().getBfromB().getC().getD().ar[0][0] = 99; // a.getB().getBfromB().getC().getD().ar[0] is null", e.getMessage(),
                         "Cannot store to int array because " +
                         "\"NullPointerExceptionTest$C.getD().ar[0]\" is null");
        }
    }

    // Helper method to cause test case.
    private Object returnNull(String[][] dummy1, int[][][] dummy2, float dummy3) {
        return null;
    }

    // Helper method to cause test case.
    private NullPointerExceptionTest returnMeAsNull(Throwable dummy1, int dummy2, char dummy3) {
        return null;
    }

    // Helper interface for test cases.
    static interface DoubleArrayGen {
        public double[] getArray();
    }

    // Helper class for test cases.
    static class DoubleArrayGenImpl implements DoubleArrayGen {
        @Override
        public double[] getArray() {
            return null;
        }
    }

    // Helper class for test cases.
    static class NullPointerGenerator {
        public static Object nullReturner(boolean dummy1) {
            return null;
        }

        public Object returnMyNull(double dummy1, long dummy2, short dummy3) {
            return null;
        }
    }

    // Helper method to cause test case.
    public void ImplTestLoadedFromMethod(DoubleArrayGen gen) {
        try {
            (gen.getArray())[0] = 1.0;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "(gen.getArray())[0] = 1.0;", e.getMessage(),
                         "Cannot store to double array because " +
                         "the return value of \"NullPointerExceptionTest$DoubleArrayGen.getArray()\" is null");
        }
    }

    public void testNullEntity() {
        int[][] a = new int[820][];

        test_iload();
        test_lload();
        test_fload();
        // test_dload();
        test_aload();
        // aload_0: 'this'
        try {
            this.nullInstanceField.nullInstanceField = new NullPointerExceptionTest();
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "this.nullInstanceField.nullInstanceField = new NullPointerExceptionTest();", e.getMessage(),
                         "Cannot assign field \"nullInstanceField\" because \"this.nullInstanceField\" is null");
        }

        // aconst_null
        try {
            throw null;
        } catch (NullPointerException e) {
            checkMessage(e, "throw null;", e.getMessage(),
                         "Cannot throw exception because \"null\" is null");
        }
        // iconst_0
        try {
            a[0][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[0][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[0]\"" : "\"<local1>[0]\"") + " is null");
        }
        // iconst_1
        try {
            a[1][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[1][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[1]\"" : "\"<local1>[1]\"") + " is null");
        }
        // iconst_2
        try {
            a[2][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[2][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[2]\"" : "\"<local1>[2]\"") + " is null");
        }
        // iconst_3
        try {
            a[3][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[3][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[3]\"" : "\"<local1>[3]\"") + " is null");
        }
        // iconst_4
        try {
            a[4][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[4][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[4]\"" : "\"<local1>[4]\"") + " is null");
        }
        // iconst_5
        try {
            a[5][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[5][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[5]\"" : "\"<local1>[5]\"") + " is null");
        }
        // long --> iconst
        try {
            a[(int)0L][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[(int)0L][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[0]\"" : "\"<local1>[0]\"") + " is null");
        }
        // bipush
        try {
            a[139 /*0x77*/][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[139][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[139]\"" : "\"<local1>[139]\"") + " is null");
        }
        // sipush
        try {
            a[819 /*0x333*/][0] = 77;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a[819][0] = 77;", e.getMessage(),
                         "Cannot store to int array because " +
                         (hasDebugInfo ? "\"a[819]\"" : "\"<local1>[819]\"") + " is null");
        }

        // aaload, with recursive descend.
        testArrayChasing();

        // getstatic
        try {
            boolean val = (((float[]) nullStaticField)[0] == 1.0f);
            Asserts.assertTrue(val);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "boolean val = (((float[]) nullStaticField)[0] == 1.0f);", e.getMessage(),
                         "Cannot load from float array because \"NullPointerExceptionTest.nullStaticField\" is null");
        }

        // getfield, with recursive descend.
        testPointerChasing();

        // invokestatic
        try {
            char val = ((char[]) NullPointerGenerator.nullReturner(false))[0];
            System.out.println(val);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "char val = ((char[]) NullPointerGenerator.nullReturner(false))[0];", e.getMessage(),
                         "Cannot load from char array because " +
                         "the return value of \"NullPointerExceptionTest$NullPointerGenerator.nullReturner(boolean)\" is null");
        }
        // invokevirtual
        try {
            char val = ((char[]) (new NullPointerGenerator().returnMyNull(1, 1, (short) 1)))[0];
            System.out.println(val);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "char val = ((char[]) (new NullPointerGenerator().returnMyNull(1, 1, (short) 1)))[0];", e.getMessage(),
                         "Cannot load from char array because " +
                         "the return value of \"NullPointerExceptionTest$NullPointerGenerator.returnMyNull(double, long, short)\" is null");
        }
        // Call with array arguments.
        try {
            double val = ((double[]) returnNull(null, null, 1f))[0];
            System.out.println(val);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "double val = ((double[]) returnNull(null, null, 1f))[0];", e.getMessage(),
                         "Cannot load from double array because " +
                         "the return value of \"NullPointerExceptionTest.returnNull(String[][], int[][][], float)\" is null");
        }
        // invokespecial
        try {
            SubG g = new SubG();
            g.m2("Beginning");
        } catch (NullPointerException e) {
            checkMessage(e, "return super.m2(x).substring(2);", e.getMessage(),
                         "Cannot invoke \"String.substring(int)\" because " +
                         "the return value of \"G.m2(String)\" is null");
        }
        // invokeinterface
        ImplTestLoadedFromMethod(new DoubleArrayGenImpl());
        try {
            returnMeAsNull(null, 1, 'A').dag = new DoubleArrayGenImpl();
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "returnMeAsNull(null, 1, 'A').dag = new DoubleArrayGenImpl();", e.getMessage(),
                         "Cannot assign field \"dag\" because " +
                         "the return value of \"NullPointerExceptionTest.returnMeAsNull(java.lang.Throwable, int, char)\" is null");
        }
        testMethodChasing();

        // Mixed recursive descend.
        testMixedChasing();
    }

    // Assure 64 parameters are printed as 'parameteri'.
    public String manyParameters(
        int  i1, int  i2, int  i3, int  i4, int  i5, int  i6, int  i7, int  i8, int  i9, int i10,
        int i11, int i12, int i13, int i14, int i15, int i16, int i17, int i18, int i19, int i20,
        int i21, int i22, int i23, int i24, int i25, int i26, int i27, int i28, int i29, int i30,
        int i31, int i32, int i33, int i34, int i35, int i36, int i37, int i38, int i39, int i40,
        int i41, int i42, int i43, int i44, int i45, int i46, int i47, int i48, int i49, int i50,
        int i51, int i52, int i53, int i54, int i55, int i56, int i57, int i58, int i59, int i60,
        int i61, int i62, int i63, int i64, int i65, int i66, int i67, int i68, int i69, int i70) {
        String[][][][] ar5 = new String[1][1][1][1];
        int[][][] idx3 = new int[1][1][1];
        int[][]   idx2 = new int[1][1];
        return ar5[i70]
                  [idx2[i65][i64]]
                  [idx3[i63][i62][i47]]
                  [idx3[idx2[i33][i32]][i31][i17]]
                  .substring(2);
    }

    // The double placeholder takes two slots on the stack.
    public void testParametersTestMethod(A a, double placeholder, B b, Integer i) throws Exception {
        try {
            a.to_b.to_c.to_d.num = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "a.to_b.to_c.to_d.num = 99; // to_c is null, a is a parameter.", e.getMessage(),
                         "Cannot read field \"to_d\" because \"" +
                         (hasDebugInfo ? "a" : "<parameter1>") + ".to_b.to_c\" is null");
        }

        try {
            b.to_c.to_d.num = 99;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "b.to_c.to_d.num = 99; // b is null and b is a parameter.", e.getMessage(),
                         "Cannot read field \"to_c\" because " +
                         // We expect number '3' for the parameter.
                         (hasDebugInfo ? "\"b\"" : "\"<parameter3>\"") + " is null");
        }


        try {
            @SuppressWarnings("unused")
            int my_i = i;
        }  catch (NullPointerException e) {
            checkMessage(e, "int my_i = i; // i is a parameter of type Integer.",  e.getMessage(),
                         "Cannot invoke \"java.lang.Integer.intValue()\" because " +
                         (hasDebugInfo ? "\"i\"" : "\"<parameter4>\"") + " is null");
        }

        // If no debug information is available, only 64 parameters (this and i1 through i63)
        // will be reported in the message as 'parameteri'. Others will be reported as 'locali'.
        try {
            manyParameters(0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                           0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            } catch (NullPointerException e) {
                checkMessage(e, "return ar5[i70][idx2[i65][i64]][idx3[i63][i62][i47]][idx3[idx2[i33][i32]][i31][i17]].substring(2);", e.getMessage(),
                             "Cannot invoke \"String.substring(int)\" because " +
                             (hasDebugInfo ?
                               "\"ar5[i70][idx2[i65][i64]][idx3[i63][i62][i47]][idx3[idx2[i33][i32]][i31][i17]]\"" :
                               "\"<local71>[<local70>][<local73>[<local65>][<local64>]][<local72>[<parameter63>][<parameter62>]" +
                                  "[<parameter47>]][<local72>[<local73>[<parameter33>][<parameter32>]][<parameter31>][<parameter17>]]\"") +
                             " is null");
            }
    }


    public void testParameters() throws Exception {
        A a = new A();
        a.to_b = new B();
        testParametersTestMethod(a, 0.0, null, null);
    }


    public void testCreation() throws Exception {
        // If allocated with new, the message should not be generated.
        Asserts.assertNull(new NullPointerException().getMessage());
        String msg = new String("A pointless message");
        Asserts.assertTrue(new NullPointerException(msg).getMessage() == msg);

        // If fillInStackTrace is called a second time (first call is in
        // the constructor), the information to compute the extended
        // message is lost. Check we deal with this correctly.

        // On explicit construction no message should be computed, also
        // after fillInStackTrace().
        Asserts.assertNull(new NullPointerException().fillInStackTrace().getMessage());
        Asserts.assertTrue(new NullPointerException(msg).fillInStackTrace().getMessage() == msg);

        // Similar if the exception is assigned to a local.
        NullPointerException ex = new NullPointerException();
        Throwable t = ex.fillInStackTrace();
        Asserts.assertNull(t.getMessage());

        ex = new NullPointerException(msg);
        t = ex.fillInStackTrace();
        Asserts.assertTrue(t.getMessage() == msg);

        // An implicit exception should have the right message.
        F f = null;
        String expectedMessage =
            "Cannot assign field \"i\" because " +
            (hasDebugInfo ? "\"f\"" : "\"<local4>\"") + " is null";
        try {
            f.i = 17;
        } catch (NullPointerException e) {
            checkMessage(e, "f.i = 17;", e.getMessage(), expectedMessage);
            t = e.fillInStackTrace();
        }
        checkMessage(t, "e.fillInStackTrace()", t.getMessage(), expectedMessage);

        // Make sure a new exception thrown while calling fillInStackTrace()
        // gets the correct message.
        ex = null;
        try {
            ex.fillInStackTrace();
        } catch (NullPointerException e) {
            checkMessage(e, "ex.fillInStackTrace()", e.getMessage(),
                         "Cannot invoke \"java.lang.NullPointerException.fillInStackTrace()\" because " +
                         (hasDebugInfo ? "\"ex\"" : "\"<local2>\"") + " is null");
        }

        // setStackTrace does not affect computing the message.
        // Message and stack trace won't match, though.
        F f1 = null;
        F f2 = null;
        NullPointerException e1 = null;
        NullPointerException e2 = null;
        try {
            f1.i = 18;
        } catch (NullPointerException e) {
            checkMessage(e, "f1.i = 18;", e.getMessage(),
                         "Cannot assign field \"i\" because " +
                         (hasDebugInfo ? "\"f1\"" : "\"<local6>\"") + " is null");
            e1 = e;
        }
        try {
            f2.i = 19;
        } catch (NullPointerException e) {
            checkMessage(e, "f2.i = 19;", e.getMessage(),
                         "Cannot assign field \"i\" because " +
                         (hasDebugInfo ? "\"f2\"" : "\"<local7>\"") + " is null");
            e2 = e;
        }
        e1.setStackTrace(e2.getStackTrace());
        checkMessage(e1, "f1.i = 18;", e1.getMessage(),
                     "Cannot assign field \"i\" because " +
                     (hasDebugInfo ? "\"f1\"" : "\"<local6>\"") + " is null");
        checkMessage(e2, "f1.i = 18;", e2.getMessage(),
                     "Cannot assign field \"i\" because " +
                     (hasDebugInfo ? "\"f2\"" : "\"<local7>\"") + " is null");

        // If created via reflection, the message should not be generated.
        ex = NullPointerException.class.getDeclaredConstructor().newInstance();
        Asserts.assertNull(ex.getMessage());
    }

    public void testNative() throws Exception {
        // If NPE is thrown in a native method, the message should
        // not be generated.
        try {
            Class.forName(null);
            Asserts.fail();
        } catch (NullPointerException e) {
            Asserts.assertNull(e.getMessage());
        }
    }

    // Test we get the same message calling npe.getMessage() twice.
    @SuppressWarnings("null")
    public void testSameMessage() throws Exception {
        Object null_o = null;
        String expectedMsg =
            "Cannot invoke \"Object.hashCode()\" because " +
            (hasDebugInfo ? "\"null_o" : "\"<local1>") + "\" is null";

        try {
            null_o.hashCode();
            Asserts.fail();
        } catch (NullPointerException e) {
            String msg1 = e.getMessage();
            checkMessage(e, "null_o.hashCode()", msg1, expectedMsg);
            String msg2 = e.getMessage();
            Asserts.assertTrue(msg1.equals(msg2));
            Asserts.assertTrue(msg1 == msg2);
        }
    }

    @SuppressWarnings("null")
    public void testSerialization() throws Exception {
        // NPE without message.
        Object o1 = new NullPointerException();
        ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
        ObjectOutputStream oos1 = new ObjectOutputStream(bos1);
        oos1.writeObject(o1);
        ByteArrayInputStream bis1 = new ByteArrayInputStream(bos1.toByteArray());
        ObjectInputStream ois1 = new ObjectInputStream(bis1);
        Exception ex1 = (Exception) ois1.readObject();
        Asserts.assertNull(ex1.getMessage());

        // NPE with custom message.
        String msg2 = "A useless message";
        Object o2 = new NullPointerException(msg2);
        ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
        ObjectOutputStream oos2 = new ObjectOutputStream(bos2);
        oos2.writeObject(o2);
        ByteArrayInputStream bis2 = new ByteArrayInputStream(bos2.toByteArray());
        ObjectInputStream ois2 = new ObjectInputStream(bis2);
        Exception ex2 = (Exception) ois2.readObject();
        Asserts.assertEquals(ex2.getMessage(), msg2);

        // NPE with generated message.
        Object null_o3 = null;
        Object o3 = null;
        String msg3 = null;
        try {
            int hc = null_o3.hashCode();
            System.out.println(hc);
            Asserts.fail();
        } catch (NullPointerException npe3) {
            o3 = npe3;
            msg3 = npe3.getMessage();
            checkMessage(npe3, "int hc = null_o3.hashCode();", msg3,
                         "Cannot invoke \"Object.hashCode()\" because " +
                         (hasDebugInfo ? "\"null_o3\"" : "\"<local14>\"") + " is null");
        }
        ByteArrayOutputStream bos3 = new ByteArrayOutputStream();
        ObjectOutputStream oos3 = new ObjectOutputStream(bos3);
        oos3.writeObject(o3);
        ByteArrayInputStream bis3 = new ByteArrayInputStream(bos3.toByteArray());
        ObjectInputStream ois3 = new ObjectInputStream(bis3);
        Exception ex3 = (Exception) ois3.readObject();
        // It was decided that getMessage should not store the
        // message in Throwable.detailMessage or implement writeReplace(),
        // thus it can not be recovered by serialization.
        //Asserts.assertEquals(ex3.getMessage(), msg3);
        Asserts.assertEquals(ex3.getMessage(), null);
    }

    static int index17 = 17;
    int getIndex17() { return 17; };

    @SuppressWarnings({ "unused", "null" })
    public void testComplexMessages() {
        try {
            staticLongArray[0][0] = 2L;
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "staticLongArray[0][0] = 2L;", e.getMessage(),
                         "Cannot store to long array because " +
                         "\"NullPointerExceptionTest.staticLongArray[0]\" is null");
        }

        try {
            NullPointerExceptionTest obj = this;
            Object val = obj.dag.getArray().clone();
            Asserts.assertNull(val);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "Object val = obj.dag.getArray().clone();", e.getMessage(),
                         "Cannot invoke \"NullPointerExceptionTest$DoubleArrayGen.getArray()\" because " +
                         (hasDebugInfo ? "\"obj" : "\"<local1>") + ".dag\" is null");
        }
        try {
            int indexes[] = new int[1];
            NullPointerExceptionTest[] objs = new NullPointerExceptionTest[] {this};
            Object val = objs[indexes[0]].nullInstanceField.returnNull(null, null, 1f);
            Asserts.assertNull(val);
            Asserts.fail();
        } catch (NullPointerException e) {
            checkMessage(e, "Object val = objs[indexes[0]].nullInstanceField.returnNull(null, null, 1f);", e.getMessage(),
                         "Cannot invoke \"NullPointerExceptionTest.returnNull(String[][], int[][][], float)\" because " +
                         (hasDebugInfo ? "\"objs[indexes" : "\"<local2>[<local1>") + "[0]].nullInstanceField\" is null");
        }

        try {
            int indexes[] = new int[1];
            NullPointerExceptionTest[][] objs =
                new NullPointerExceptionTest[][] {new NullPointerExceptionTest[] {this}};
            synchronized (objs[indexes[0]][0].nullInstanceField) {
                Asserts.fail();
            }
        } catch (NullPointerException e) {
            checkMessage(e, "synchronized (objs[indexes[0]][0].nullInstanceField) { ... }", e.getMessage(),
                         "Cannot enter synchronized block because " +
                         (hasDebugInfo ? "\"objs[indexes" : "\"<local2>[<local1>" ) + "[0]][0].nullInstanceField\" is null");
        }

        try {
            // If we can get the value from more than one bci, we cannot know which one
            // is null. Make sure we don't print the wrong value.
            String s = null;
            @SuppressWarnings("unused")
            byte[] val = (rng.nextDouble() < 0.5 ? s : (new String[1])[0]).getBytes();
        } catch (NullPointerException e) {
            checkMessage(e, "byte[] val = (rng.nextDouble() < 0.5 ? s : (new String[1])[0]).getBytes();", e.getMessage(),
                         "Cannot invoke \"String.getBytes()\"");
        }

        try {
            // If we can get the value from more than one bci, we cannot know which one
            // is null. Make sure we don't print the wrong value.  Also make sure if
            // we don't print the failed action we don't print a string at all.
            int[][] a = new int[1][];
            int[][] b = new int[2][];
            long index = 0;
            @SuppressWarnings("unused")
            int val = (rng.nextDouble() < 0.5 ? a[(int)index] : b[(int)index])[13];
        } catch (NullPointerException e) {
            checkMessage(e, "int val = (rng.nextDouble() < 0.5 ? a[(int)index] : b[(int)index])[13]", e.getMessage(),
                         "Cannot load from int array");
        }

        try {
            // If we can get the value from more than one bci, we cannot know which one
            // is null. Make sure we don't print the wrong value.  Also make sure if
            // we don't print the failed action we don't print a string at all.
            int[][] a = new int[1][];
            int[][] b = new int[2][];
            long index = 0;
            int val = (rng.nextDouble() < 0.5 ? a : b)[(int)index][13];
        } catch (NullPointerException e) {
            checkMessage(e, "int val = (rng.nextDouble() < 0.5 ? a : b)[(int)index][13]", e.getMessage(),
                         "Cannot load from int array because \"<array>[...]\" is null");
        }

        try {
            C c1 = new C();
            C c2 = new C();
            (rng.nextDouble() < 0.5 ? c1 : c2).to_d.num = 77;
        } catch (NullPointerException e) {
            checkMessage(e, "(rng.nextDouble() < 0.5 ? c1 : c2).to_d.num = 77;", e.getMessage(),
                         "Cannot assign field \"num\" because \"to_d\" is null");
        }

        // Static variable as array index.
        try {
            staticLongArray[index17][0] = 2L;
        }  catch (NullPointerException e) {
            checkMessage(e, "staticLongArray[index17][0] = 2L;",  e.getMessage(),
                         "Cannot store to long array because " +
                         "\"NullPointerExceptionTest.staticLongArray[NullPointerExceptionTest.index17]\" is null");
        }

        // Method call as array index.
        try {
            staticLongArray[getIndex17()][0] = 2L;
        }  catch (NullPointerException e) {
            checkMessage(e, "staticLongArray[getIndex17()][0] = 2L;",  e.getMessage(),
                         "Cannot store to long array because " +
                         "\"NullPointerExceptionTest.staticLongArray[NullPointerExceptionTest.getIndex17()]\" is null");
        }

        // Unboxing.
        Integer a = null;
        try {
            int b = a;
        }  catch (NullPointerException e) {
            checkMessage(e, "Integer a = null; int b = a;",  e.getMessage(),
                         "Cannot invoke \"java.lang.Integer.intValue()\" because " +
                         (hasDebugInfo ? "\"a\"" : "\"<local1>\"") + " is null");
        }

        // Unboxing by hand. Has the same message as above.
        try {
            int b = a.intValue();
        }  catch (NullPointerException e) {
            checkMessage(e, "Integer a = null; int b = a.intValue();",  e.getMessage(),
                         "Cannot invoke \"java.lang.Integer.intValue()\" because " +
                         (hasDebugInfo ? "\"a\"" : "\"<local1>\"") + " is null");
        }
    }

    // Generates:
    // class E implements E0 {
    //     public int throwNPE(F f) {
    //         return f.i;
    //     }
    //     public void throwNPE_reuseStackSlot1(String s1) {
    //         System.out.println(s1.substring(1));
    //         String s1_2 = null; // Reuses slot 1.
    //         System.out.println(s1_2.substring(1));
    //     }
    //     public void throwNPE_reuseStackSlot4(String s1, String s2, String s3, String s4) {
    //         System.out.println(s4.substring(1));
    //         String s4_2 = null;  // Reuses slot 4.
    //         System.out.println(s4_2.substring(1));
    //     }
    // }
    //
    // This code was adapted from output of
    //   java jdk.internal.org.objectweb.asm.util.ASMifier E0.class
    static byte[] generateTestClass() {
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        cw.visit(50, ACC_SUPER, "E", null, "java/lang/Object", new String[] { "E0" });

        {
            mv = cw.visitMethod(0, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        {
            mv = cw.visitMethod(ACC_PUBLIC, "throwNPE", "(LF;)I", null, null);
            mv.visitCode();
            Label label0 = new Label();
            mv.visitLabel(label0);
            mv.visitLineNumber(118, label0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(GETFIELD, "F", "i", "I");
            mv.visitInsn(IRETURN);
            Label label1 = new Label();
            mv.visitLabel(label1);
            mv.visitLocalVariable("this", "LE;", null, label0, label1, 0);
            mv.visitLocalVariable("f", "LE;", null, label0, label1, 1);
            mv.visitMaxs(1, 2);
            mv.visitEnd();
        }

        {
            mv = cw.visitMethod(ACC_PUBLIC, "throwNPE_reuseStackSlot1", "(Ljava/lang/String;)V", null, null);
            mv.visitCode();
            Label label0 = new Label();
            mv.visitLabel(label0);
            mv.visitLineNumber(7, label0);
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitInsn(ICONST_1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "substring", "(I)Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            Label label1 = new Label();
            mv.visitLabel(label1);
            mv.visitLineNumber(8, label1);
            mv.visitInsn(ACONST_NULL);
            mv.visitVarInsn(ASTORE, 1);
            Label label2 = new Label();
            mv.visitLabel(label2);
            mv.visitLineNumber(9, label2);
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitVarInsn(ALOAD, 1);
            mv.visitInsn(ICONST_1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "substring", "(I)Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            Label label3 = new Label();
            mv.visitLabel(label3);
            mv.visitLineNumber(10, label3);
            mv.visitInsn(RETURN);
            mv.visitMaxs(3, 3);
            mv.visitEnd();
        }

        {
            mv = cw.visitMethod(ACC_PUBLIC, "throwNPE_reuseStackSlot4", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", null, null);
            mv.visitCode();
            Label label0 = new Label();
            mv.visitLabel(label0);
            mv.visitLineNumber(12, label0);
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitVarInsn(ALOAD, 4);
            mv.visitInsn(ICONST_1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "substring", "(I)Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            Label label1 = new Label();
            mv.visitLabel(label1);
            mv.visitLineNumber(13, label1);
            mv.visitInsn(ACONST_NULL);
            mv.visitVarInsn(ASTORE, 4);
            Label label2 = new Label();
            mv.visitLabel(label2);
            mv.visitLineNumber(14, label2);
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitVarInsn(ALOAD, 4);
            mv.visitInsn(ICONST_1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "substring", "(I)Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            Label label3 = new Label();
            mv.visitLabel(label3);
            mv.visitLineNumber(15, label3);
            mv.visitInsn(RETURN);
            mv.visitMaxs(3, 6);
            mv.visitEnd();
        }

        cw.visitEnd();

        return cw.toByteArray();
    }

    // Assign to a parameter.
    // Without debug information, this will print "parameter1" if a NPE
    // is raised in the first line because null was passed to the method.
    // It will print "local1" if a NPE is raised in line three.
    public void assign_to_parameter(String s1) {
        System.out.println(s1.substring(1));
        s1 = null;
        System.out.println(s1.substring(2));
    }

    // Tests that a class generated on the fly is handled properly.
    public void testGeneratedCode() throws Exception {
        byte[] classBytes = generateTestClass();
        Lookup lookup = lookup();
        Class<?> clazz = lookup.defineClass(classBytes);
        E0 e = (E0) clazz.getDeclaredConstructor().newInstance();
        try {
            e.throwNPE(null);
        } catch (NullPointerException ex) {
            checkMessage(ex, "return f.i;",
                         ex.getMessage(),
                         "Cannot read field \"i\" because \"f\" is null");
        }

        // Optimized bytecode can reuse local variable slots for several
        // local variables.
        // If there is no variable name information, we print 'parameteri'
        // if a parameter maps to a local slot. Once a local slot has been
        // written, we don't know any more whether it was written as the
        // corresponding parameter, or whether another local has been
        // mapped to the slot. So we don't want to print 'parameteri' any
        // more, but 'locali'. Similary for 'this'.

        // Expect message saying "parameter0".
        try {
            e.throwNPE_reuseStackSlot1(null);
        } catch (NullPointerException ex) {
            checkMessage(ex, "s1.substring(1)",
                         ex.getMessage(),
                         "Cannot invoke \"String.substring(int)\" because \"<parameter1>\" is null");
        }
        // Expect message saying "local0".
        try {
            e.throwNPE_reuseStackSlot1("aa");
        } catch (NullPointerException ex) {
            checkMessage(ex, "s1_2.substring(1)",
                         ex.getMessage(),
                         "Cannot invoke \"String.substring(int)\" because \"<local1>\" is null");
        }
        // Expect message saying "parameter4".
        try {
            e.throwNPE_reuseStackSlot4("aa", "bb", "cc", null);
        } catch (NullPointerException ex) {
            checkMessage(ex, "s4.substring(1)",
                         ex.getMessage(),
                         "Cannot invoke \"String.substring(int)\" because \"<parameter4>\" is null");
        }
        // Expect message saying "local4".
        try {
            e.throwNPE_reuseStackSlot4("aa", "bb", "cc", "dd");
        } catch (NullPointerException ex) {
            checkMessage(ex, "s4_2.substring(1)",
                         ex.getMessage(),
                         "Cannot invoke \"String.substring(int)\" because \"<local4>\" is null");
        }

        // Unfortunately, with the fix for optimized code as described above
        // we don't write 'parameteri' any more after the parameter variable
        // has been assigned.

        if (!hasDebugInfo) {
            // Expect message saying "parameter1".
            try {
                assign_to_parameter(null);
            } catch (NullPointerException ex) {
                checkMessage(ex, "s1.substring(1)",
                             ex.getMessage(),
                             "Cannot invoke \"String.substring(int)\" because \"<parameter1>\" is null");
            }
            // The message says "local1" although "parameter1" would be correct.
            try {
                assign_to_parameter("aaa");
            } catch (NullPointerException ex) {
                checkMessage(ex, "s1.substring(2)",
                             ex.getMessage(),
                             "Cannot invoke \"String.substring(int)\" because \"<local1>\" is null");
            }
        }
    }
}

// Helper interface for test cases needed for generateTestClass().
interface E0 {
    public int  throwNPE(F f);
    public void throwNPE_reuseStackSlot1(String s1);
    public void throwNPE_reuseStackSlot4(String s1, String s2, String s3, String s4);
}

// Helper class for test cases needed for generateTestClass().
class F {
    int i;
}

// For invokespecial test cases.
class G {
    public String m2(String x) {
        return null;
    }

    // This generates the following class:
    //
    // class Sub2G extends G {
    //   public String m2(String x) {
    //       super = null;  // Possible in raw bytecode.
    //       return super.m2(x).substring(2);  // Uses invokespecial.
    //   }
    // }
    //
    // This code was adapted from output of
    //   java jdk.internal.org.objectweb.asm.util.ASMifier Sub2G.class
    static byte[] generateSub2GTestClass() {
        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        cw.visit(50, ACC_SUPER, "Sub2G", null, "G", null);

        {
            mv = cw.visitMethod(0, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "G", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "m2", "(Ljava/lang/String;)Ljava/lang/String;", null, null);
            mv.visitCode();
            mv.visitInsn(ACONST_NULL);   // Will cause NPE.
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, "G", "m2", "(Ljava/lang/String;)Ljava/lang/String;", false);
            mv.visitInsn(ICONST_2);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "substring", "(I)Ljava/lang/String;", false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }

        cw.visitEnd();

        return cw.toByteArray();
    }
}
class SubG extends G {
    public String m2(String x) {
        return super.m2(x).substring(2);
    }
}
