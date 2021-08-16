/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @test TestStableFloat
 * @summary tests on stable fields and arrays
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @modules java.base/jdk.internal.vm.annotation
 * @build sun.hotspot.WhiteBox
 *
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:-TieredCompilation
 *                                 -XX:+FoldStableValues
 *                                 compiler.stable.TestStableFloat
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:-TieredCompilation
 *                                 -XX:-FoldStableValues
 *                                 compiler.stable.TestStableFloat
 *
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                                 -XX:+FoldStableValues
 *                                 compiler.stable.TestStableFloat
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                                 -XX:-FoldStableValues
 *                                 compiler.stable.TestStableFloat
 */

package compiler.stable;

import jdk.internal.vm.annotation.Stable;

import java.lang.reflect.InvocationTargetException;

public class TestStableFloat {
    static final boolean isStableEnabled    = StableConfiguration.isStableEnabled;

    public static void main(String[] args) throws Exception {
        run(DefaultValue.class);
        run(FloatStable.class);
        run(DefaultStaticValue.class);
        run(StaticFloatStable.class);
        run(VolatileFloatStable.class);

        // @Stable arrays: Dim 1-4
        run(FloatArrayDim1.class);
        run(FloatArrayDim2.class);
        run(FloatArrayDim3.class);
        run(FloatArrayDim4.class);

        // @Stable Object field: dynamic arrays
        run(ObjectArrayLowerDim0.class);
        run(ObjectArrayLowerDim1.class);
        run(ObjectArrayLowerDim2.class);

        // Nested @Stable fields
        run(NestedStableField.class);
        run(NestedStableField1.class);
        run(NestedStableField2.class);
        run(NestedStableField3.class);

        if (failed) {
            throw new Error("TEST FAILED");
        }
    }

    /* ==================================================== */

    static class DefaultValue {
        public @Stable float v;

        public static final DefaultValue c = new DefaultValue();
        public static float get() { return c.v; }
        public static void test() throws Exception {
                        float val1 = get();
            c.v = 1.0F; float val2 = get();
            assertEquals(val1, 0F);
            assertEquals(val2, 1.0F);
        }
    }

    /* ==================================================== */

    static class FloatStable {
        public @Stable float v;

        public static final FloatStable c = new FloatStable();
        public static float get() { return c.v; }
        public static void test() throws Exception {
            c.v = 1.0F; float val1 = get();
            c.v = 2.0F; float val2 = get();
            assertEquals(val1, 1.0F);
            assertEquals(val2, (isStableEnabled ? 1.0F : 2.0F));
        }
    }

    /* ==================================================== */

    static class DefaultStaticValue {
        public static @Stable float v;

        public static final DefaultStaticValue c = new DefaultStaticValue();
        public static float get() { return c.v; }
        public static void test() throws Exception {
            float val1 = get();
            c.v = 1.0F; float val2 = get();
            assertEquals(val1, 0F);
            assertEquals(val2, 1.0F);
        }
    }

    /* ==================================================== */

    static class StaticFloatStable {
        public static @Stable float v;

        public static final StaticFloatStable c = new StaticFloatStable();
        public static float get() { return c.v; }
        public static void test() throws Exception {
            c.v = 1.0F; float val1 = get();
            c.v = 2.0F; float val2 = get();
            assertEquals(val1, 1.0F);
            assertEquals(val2, (isStableEnabled ? 1.0F : 2.0F));
        }
    }

    /* ==================================================== */

    static class VolatileFloatStable {
        public @Stable volatile float v;

        public static final VolatileFloatStable c = new VolatileFloatStable();
        public static float get() { return c.v; }
        public static void test() throws Exception {
            c.v = 1.0F; float val1 = get();
            c.v = 2.0F; float val2 = get();
            assertEquals(val1, 1.0F);
            assertEquals(val2, (isStableEnabled ? 1.0F : 2.0F));
        }
    }

    /* ==================================================== */
    // @Stable array == field && all components are stable

    static class FloatArrayDim1 {
        public @Stable float[] v;

        public static final FloatArrayDim1 c = new FloatArrayDim1();
        public static float get() { return c.v[0]; }
        public static float get1() { return c.v[10]; }
        public static float[] get2() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new float[1]; c.v[0] = 1.0F; float val1 = get();
                                    c.v[0] = 2.0F; float val2 = get();
                assertEquals(val1, 1.0F);
                assertEquals(val2, (isStableEnabled ? 1.0F : 2.0F));

                c.v = new float[1]; c.v[0] = 3.0F; float val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 1.0F : 2.0F)
                                                    : 3.0F));
            }

            {
                c.v = new float[20]; c.v[10] = 1.0F; float val1 = get1();
                                     c.v[10] = 2.0F; float val2 = get1();
                assertEquals(val1, 1.0F);
                assertEquals(val2, (isStableEnabled ? 1.0F : 2.0F));

                c.v = new float[20]; c.v[10] = 3.0F; float val3 = get1();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 1.0F : 2.0F)
                                                    : 3.0F));
            }

            {
                c.v = new float[1]; float[] val1 = get2();
                c.v = new float[1]; float[] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class FloatArrayDim2 {
        public @Stable float[][] v;

        public static final FloatArrayDim2 c = new FloatArrayDim2();
        public static float get() { return c.v[0][0]; }
        public static float[] get1() { return c.v[0]; }
        public static float[][] get2() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new float[1][1]; c.v[0][0] = 1.0F; float val1 = get();
                                       c.v[0][0] = 2.0F; float val2 = get();
                assertEquals(val1, 1.0F);
                assertEquals(val2, (isStableEnabled ? 1.0F : 2.0F));

                c.v = new float[1][1]; c.v[0][0] = 3.0F; float val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 1.0F : 2.0F)
                                                    : 3.0F));

                c.v[0] = new float[1]; c.v[0][0] = 4.0F; float val4 = get();
                assertEquals(val4, (isStableEnabled ? (isStableEnabled ? 1.0F : 2.0F)
                                                    : 4.0F));
            }

            {
                c.v = new float[1][1]; float[] val1 = get1();
                c.v[0] = new float[1]; float[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new float[1][1]; float[][] val1 = get2();
                c.v = new float[1][1]; float[][] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class FloatArrayDim3 {
        public @Stable float[][][] v;

        public static final FloatArrayDim3 c = new FloatArrayDim3();
        public static float get() { return c.v[0][0][0]; }
        public static float[] get1() { return c.v[0][0]; }
        public static float[][] get2() { return c.v[0]; }
        public static float[][][] get3() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new float[1][1][1]; c.v[0][0][0] = 1.0F; float val1 = get();
                                          c.v[0][0][0] = 2.0F; float val2 = get();
                assertEquals(val1, 1.0F);
                assertEquals(val2, (isStableEnabled ? 1.0F : 2.0F));

                c.v = new float[1][1][1]; c.v[0][0][0] = 3.0F; float val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 1.0F : 2.0F)
                                                    : 3.0F));

                c.v[0] = new float[1][1]; c.v[0][0][0] = 4.0F; float val4 = get();
                assertEquals(val4, (isStableEnabled ? (isStableEnabled ? 1.0F : 2.0F)
                                                    : 4.0F));

                c.v[0][0] = new float[1]; c.v[0][0][0] = 5.0F; float val5 = get();
                assertEquals(val5, (isStableEnabled ? (isStableEnabled ? 1.0F : 2.0F)
                                                    : 5.0F));
            }

            {
                c.v = new float[1][1][1]; float[] val1 = get1();
                c.v[0][0] = new float[1]; float[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new float[1][1][1]; float[][] val1 = get2();
                c.v[0] = new float[1][1]; float[][] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new float[1][1][1]; float[][][] val1 = get3();
                c.v = new float[1][1][1]; float[][][] val2 = get3();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class FloatArrayDim4 {
        public @Stable float[][][][] v;

        public static final FloatArrayDim4 c = new FloatArrayDim4();
        public static float get() { return c.v[0][0][0][0]; }
        public static float[] get1() { return c.v[0][0][0]; }
        public static float[][] get2() { return c.v[0][0]; }
        public static float[][][] get3() { return c.v[0]; }
        public static float[][][][] get4() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new float[1][1][1][1]; c.v[0][0][0][0] = 1.0F; float val1 = get();
                                             c.v[0][0][0][0] = 2.0F; float val2 = get();
                assertEquals(val1, 1.0F);
                assertEquals(val2, (isStableEnabled ? 1.0F : 2.0F));

                c.v = new float[1][1][1][1]; c.v[0][0][0][0] = 3.0F; float val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 1.0F : 2.0F)
                                                    : 3.0F));

                c.v[0] = new float[1][1][1]; c.v[0][0][0][0] = 4.0F; float val4 = get();
                assertEquals(val4, (isStableEnabled ? (isStableEnabled ? 1.0F : 2.0F)
                                                    : 4.0F));

                c.v[0][0] = new float[1][1]; c.v[0][0][0][0] = 5.0F; float val5 = get();
                assertEquals(val5, (isStableEnabled ? (isStableEnabled ? 1.0F : 2.0F)
                                                    : 5.0F));

                c.v[0][0][0] = new float[1]; c.v[0][0][0][0] = 6.0F; float val6 = get();
                assertEquals(val6, (isStableEnabled ? (isStableEnabled ? 1.0F : 2.0F)
                                                    : 6.0F));
            }

            {
                c.v = new float[1][1][1][1]; float[] val1 = get1();
                c.v[0][0][0] = new float[1]; float[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new float[1][1][1][1]; float[][] val1 = get2();
                c.v[0][0] = new float[1][1]; float[][] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new float[1][1][1][1]; float[][][] val1 = get3();
                c.v[0] = new float[1][1][1]; float[][][] val2 = get3();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new float[1][1][1][1]; float[][][][] val1 = get4();
                c.v = new float[1][1][1][1]; float[][][][] val2 = get4();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */
    // Dynamic Dim is higher than static
    static class ObjectArrayLowerDim0 {
        public @Stable Object v;

        public static final ObjectArrayLowerDim0 c = new ObjectArrayLowerDim0();
        public static float get() { return ((float[])c.v)[0]; }
        public static float[] get1() { return (float[])c.v; }

        public static void test() throws Exception {
            {
                c.v = new float[1]; ((float[])c.v)[0] = 1.0F; float val1 = get();
                                    ((float[])c.v)[0] = 2.0F; float val2 = get();

                assertEquals(val1, 1.0F);
                assertEquals(val2, 2.0F);
            }

            {
                c.v = new float[1]; float[] val1 = get1();
                c.v = new float[1]; float[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class ObjectArrayLowerDim1 {
        public @Stable Object[] v;

        public static final ObjectArrayLowerDim1 c = new ObjectArrayLowerDim1();
        public static float get() { return ((float[][])c.v)[0][0]; }
        public static float[] get1() { return (float[])(c.v[0]); }
        public static Object[] get2() { return c.v; }

        public static void test() throws Exception {
            {
                c.v = new float[1][1]; ((float[][])c.v)[0][0] = 1.0F; float val1 = get();
                                       ((float[][])c.v)[0][0] = 2.0F; float val2 = get();

                assertEquals(val1, 1.0F);
                assertEquals(val2, 2.0F);
            }

            {
                c.v = new float[1][1]; c.v[0] = new float[0]; float[] val1 = get1();
                                       c.v[0] = new float[0]; float[] val2 = get1();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new float[0][0]; Object[] val1 = get2();
                c.v = new float[0][0]; Object[] val2 = get2();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class ObjectArrayLowerDim2 {
        public @Stable Object[][] v;

        public static final ObjectArrayLowerDim2 c = new ObjectArrayLowerDim2();
        public static float get() { return ((float[][][])c.v)[0][0][0]; }
        public static float[] get1() { return (float[])(c.v[0][0]); }
        public static float[][] get2() { return (float[][])(c.v[0]); }
        public static Object[][] get3() { return c.v; }

        public static void test() throws Exception {
            {
                c.v = new float[1][1][1]; ((float[][][])c.v)[0][0][0] = 1.0F; float val1 = get();
                                          ((float[][][])c.v)[0][0][0] = 2.0F; float val2 = get();

                assertEquals(val1, 1.0F);
                assertEquals(val2, 2.0F);
            }

            {
                c.v = new float[1][1][1]; c.v[0][0] = new float[0]; float[] val1 = get1();
                                          c.v[0][0] = new float[0]; float[] val2 = get1();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new float[1][1][1]; c.v[0] = new float[0][0]; float[][] val1 = get2();
                                          c.v[0] = new float[0][0]; float[][] val2 = get2();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new float[0][0][0]; Object[][] val1 = get3();
                c.v = new float[0][0][0]; Object[][] val2 = get3();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class NestedStableField {
        static class A {
            public @Stable float a;

        }
        public @Stable A v;

        public static final NestedStableField c = new NestedStableField();
        public static A get() { return c.v; }
        public static float get1() { return get().a; }

        public static void test() throws Exception {
            {
                c.v = new A(); c.v.a = 1.0F; A val1 = get();
                               c.v.a = 2.0F; A val2 = get();

                assertEquals(val1.a, 2.0F);
                assertEquals(val2.a, 2.0F);
            }

            {
                c.v = new A(); c.v.a = 1.0F; float val1 = get1();
                               c.v.a = 2.0F; float val2 = get1();
                c.v = new A(); c.v.a = 3.0F; float val3 = get1();

                assertEquals(val1, 1.0F);
                assertEquals(val2, (isStableEnabled ? 1.0F : 2.0F));
                assertEquals(val3, (isStableEnabled ? 1.0F : 3.0F));
            }
        }
    }

    /* ==================================================== */

    static class NestedStableField1 {
        static class A {
            public @Stable float a;
            public @Stable A next;
        }
        public @Stable A v;

        public static final NestedStableField1 c = new NestedStableField1();
        public static A get() { return c.v.next.next.next.next.next.next.next; }
        public static float get1() { return get().a; }

        public static void test() throws Exception {
            {
                c.v = new A(); c.v.next = new A();   c.v.next.next  = c.v;
                               c.v.a = 1.0F; c.v.next.a = 1.0F; A val1 = get();
                               c.v.a = 2.0F; c.v.next.a = 2.0F; A val2 = get();

                assertEquals(val1.a, 2.0F);
                assertEquals(val2.a, 2.0F);
            }

            {
                c.v = new A(); c.v.next = c.v;
                               c.v.a = 1.0F; float val1 = get1();
                               c.v.a = 2.0F; float val2 = get1();
                c.v = new A(); c.v.next = c.v;
                               c.v.a = 3.0F; float val3 = get1();

                assertEquals(val1, 1.0F);
                assertEquals(val2, (isStableEnabled ? 1.0F : 2.0F));
                assertEquals(val3, (isStableEnabled ? 1.0F : 3.0F));
            }
        }
    }
   /* ==================================================== */

    static class NestedStableField2 {
        static class A {
            public @Stable float a;
            public @Stable A left;
            public         A right;
        }

        public @Stable A v;

        public static final NestedStableField2 c = new NestedStableField2();
        public static float get() { return c.v.left.left.left.a; }
        public static float get1() { return c.v.left.left.right.left.a; }

        public static void test() throws Exception {
            {
                c.v = new A(); c.v.left = c.v.right = c.v;
                               c.v.a = 1.0F; float val1 = get(); float val2 = get1();
                               c.v.a = 2.0F; float val3 = get(); float val4 = get1();

                assertEquals(val1, 1.0F);
                assertEquals(val3, (isStableEnabled ? 1.0F : 2.0F));

                assertEquals(val2, 1.0F);
                assertEquals(val4, 2.0F);
            }
        }
    }

    /* ==================================================== */

    static class NestedStableField3 {
        static class A {
            public @Stable float a;
            public @Stable A[] left;
            public         A[] right;
        }

        public @Stable A[] v;

        public static final NestedStableField3 c = new NestedStableField3();
        public static float get() { return c.v[0].left[1].left[0].left[1].a; }
        public static float get1() { return c.v[1].left[0].left[1].right[0].left[1].a; }

        public static void test() throws Exception {
            {
                A elem = new A();
                c.v = new A[] { elem, elem }; c.v[0].left = c.v[0].right = c.v;
                               elem.a = 1.0F; float val1 = get(); float val2 = get1();
                               elem.a = 2.0F; float val3 = get(); float val4 = get1();

                assertEquals(val1, 1.0F);
                assertEquals(val3, (isStableEnabled ? 1.0F : 2.0F));

                assertEquals(val2, 1.0F);
                assertEquals(val4, 2.0F);
            }
        }
    }

    /* ==================================================== */
    // Auxiliary methods
    static void assertEquals(float i, float j) { if (i != j)  throw new AssertionError(i + " != " + j); }
    static void assertTrue(boolean b) { if (!b)  throw new AssertionError(); }

    static boolean failed = false;

    public static void run(Class<?> test) {
        Throwable ex = null;
        System.out.print(test.getName()+": ");
        try {
            test.getMethod("test").invoke(null);
        } catch (InvocationTargetException e) {
            ex = e.getCause();
        } catch (Throwable e) {
            ex = e;
        } finally {
            if (ex == null) {
                System.out.println("PASSED");
            } else {
                failed = true;
                System.out.println("FAILED");
                ex.printStackTrace(System.out);
            }
        }
    }
}
