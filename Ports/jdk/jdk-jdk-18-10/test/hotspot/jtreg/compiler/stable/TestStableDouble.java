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
 * @test TestStableDouble
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
 *                                 compiler.stable.TestStableDouble
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:-TieredCompilation
 *                                 -XX:-FoldStableValues
 *                                 compiler.stable.TestStableDouble
 *
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                                 -XX:+FoldStableValues
 *                                 compiler.stable.TestStableDouble
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                                 -XX:-FoldStableValues
 *                                 compiler.stable.TestStableDouble
 */

package compiler.stable;

import jdk.internal.vm.annotation.Stable;

import java.lang.reflect.InvocationTargetException;

public class TestStableDouble {
    static final boolean isStableEnabled    = StableConfiguration.isStableEnabled;

    public static void main(String[] args) throws Exception {
        run(DefaultValue.class);
        run(DoubleStable.class);
        run(DefaultStaticValue.class);
        run(StaticDoubleStable.class);
        run(VolatileDoubleStable.class);

        // @Stable arrays: Dim 1-4
        run(DoubleArrayDim1.class);
        run(DoubleArrayDim2.class);
        run(DoubleArrayDim3.class);
        run(DoubleArrayDim4.class);

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
        public @Stable double v;

        public static final DefaultValue c = new DefaultValue();
        public static double get() { return c.v; }
        public static void test() throws Exception {
                       double val1 = get();
            c.v = 1.0; double val2 = get();
            assertEquals(val1, 0);
            assertEquals(val2, 1.0);
        }
    }

    /* ==================================================== */

    static class DoubleStable {
        public @Stable double v;

        public static final DoubleStable c = new DoubleStable();
        public static double get() { return c.v; }
        public static void test() throws Exception {
            c.v = 1.0; double val1 = get();
            c.v = Double.MAX_VALUE; double val2 = get();
            assertEquals(val1, 1.0);
            assertEquals(val2, (isStableEnabled ? 1.0 : Double.MAX_VALUE));
        }
    }

    /* ==================================================== */

    static class DefaultStaticValue {
        public static @Stable double v;

        public static final DefaultStaticValue c = new DefaultStaticValue();
        public static double get() { return c.v; }
        public static void test() throws Exception {
                       double val1 = get();
            c.v = 1.0; double val2 = get();
            assertEquals(val1, 0);
            assertEquals(val2, 1.0);
        }
    }

    /* ==================================================== */

    static class StaticDoubleStable {
        public static @Stable double v;

        public static final StaticDoubleStable c = new StaticDoubleStable();
        public static double get() { return c.v; }
        public static void test() throws Exception {
            c.v = 1.0; double val1 = get();
            c.v = Double.MAX_VALUE; double val2 = get();
            assertEquals(val1, 1.0);
            assertEquals(val2, (isStableEnabled ? 1.0 : Double.MAX_VALUE));
        }
    }

    /* ==================================================== */

    static class VolatileDoubleStable {
        public @Stable double v;

        public static final VolatileDoubleStable c = new VolatileDoubleStable();
        public static double get() { return c.v; }
        public static void test() throws Exception {
            c.v = 1.0; double val1 = get();
            c.v = Double.MAX_VALUE; double val2 = get();
            assertEquals(val1, 1.0);
            assertEquals(val2, (isStableEnabled ? 1.0 : Double.MAX_VALUE));
        }
    }

    /* ==================================================== */
    // @Stable array == field && all components are stable

    static class DoubleArrayDim1 {
        public @Stable double[] v;

        public static final DoubleArrayDim1 c = new DoubleArrayDim1();
        public static double get() { return c.v[0]; }
        public static double get1() { return c.v[10]; }
        public static double[] get2() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new double[1]; c.v[0] = 1.0; double val1 = get();
                                     c.v[0] = 2.0; double val2 = get();
                assertEquals(val1, 1.0);
                assertEquals(val2, (isStableEnabled ? 1.0 : 2.0));

                c.v = new double[1]; c.v[0] = 3.0; double val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 1.0 : 2.0)
                                                    : 3.0));
            }

            {
                c.v = new double[20]; c.v[10] = 1.0; double val1 = get1();
                                      c.v[10] = 2.0; double val2 = get1();
                assertEquals(val1, 1.0);
                assertEquals(val2, (isStableEnabled ? 1.0 : 2.0));

                c.v = new double[20]; c.v[10] = 3.0; double val3 = get1();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 1.0 : 2.0)
                                                    : 3.0));
            }

            {
                c.v = new double[1]; double[] val1 = get2();
                c.v = new double[1]; double[] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class DoubleArrayDim2 {
        public @Stable double[][] v;

        public static final DoubleArrayDim2 c = new DoubleArrayDim2();
        public static double get() { return c.v[0][0]; }
        public static double[] get1() { return c.v[0]; }
        public static double[][] get2() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new double[1][1]; c.v[0][0] = 1.0; double val1 = get();
                                        c.v[0][0] = 2.0; double val2 = get();
                assertEquals(val1, 1.0);
                assertEquals(val2, (isStableEnabled ? 1.0 : 2.0));

                c.v = new double[1][1]; c.v[0][0] = 3.0; double val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 1.0 : 2.0)
                                                    : 3.0));

                c.v[0] = new double[1]; c.v[0][0] = 4.0; double val4 = get();
                assertEquals(val4, (isStableEnabled ? (isStableEnabled ? 1.0 : 2.0)
                                                    : 4.0));
            }

            {
                c.v = new double[1][1]; double[] val1 = get1();
                c.v[0] = new double[1]; double[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new double[1][1]; double[][] val1 = get2();
                c.v = new double[1][1]; double[][] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class DoubleArrayDim3 {
        public @Stable double[][][] v;

        public static final DoubleArrayDim3 c = new DoubleArrayDim3();
        public static double get() { return c.v[0][0][0]; }
        public static double[] get1() { return c.v[0][0]; }
        public static double[][] get2() { return c.v[0]; }
        public static double[][][] get3() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new double[1][1][1]; c.v[0][0][0] = 1.0; double val1 = get();
                                           c.v[0][0][0] = 2.0; double val2 = get();
                assertEquals(val1, 1.0);
                assertEquals(val2, (isStableEnabled ? 1.0 : 2.0));

                c.v = new double[1][1][1]; c.v[0][0][0] = 3.0; double val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 1.0 : 2.0)
                                                    : 3.0));

                c.v[0] = new double[1][1]; c.v[0][0][0] = 4.0; double val4 = get();
                assertEquals(val4, (isStableEnabled ? (isStableEnabled ? 1.0 : 2.0)
                                                    : 4.0));

                c.v[0][0] = new double[1]; c.v[0][0][0] = 5.0; double val5 = get();
                assertEquals(val5, (isStableEnabled ? (isStableEnabled ? 1.0 : 2.0)
                                                    : 5.0));
            }

            {
                c.v = new double[1][1][1]; double[] val1 = get1();
                c.v[0][0] = new double[1]; double[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new double[1][1][1]; double[][] val1 = get2();
                c.v[0] = new double[1][1]; double[][] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new double[1][1][1]; double[][][] val1 = get3();
                c.v = new double[1][1][1]; double[][][] val2 = get3();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class DoubleArrayDim4 {
        public @Stable double[][][][] v;

        public static final DoubleArrayDim4 c = new DoubleArrayDim4();
        public static double get() { return c.v[0][0][0][0]; }
        public static double[] get1() { return c.v[0][0][0]; }
        public static double[][] get2() { return c.v[0][0]; }
        public static double[][][] get3() { return c.v[0]; }
        public static double[][][][] get4() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new double[1][1][1][1]; c.v[0][0][0][0] = 1.0; double val1 = get();
                                              c.v[0][0][0][0] = 2.0; double val2 = get();
                assertEquals(val1, 1.0);
                assertEquals(val2, (isStableEnabled ? 1.0 : 2.0));

                c.v = new double[1][1][1][1]; c.v[0][0][0][0] = 3.0; double val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 1.0 : 2.0)
                                                    : 3.0));

                c.v[0] = new double[1][1][1]; c.v[0][0][0][0] = 4.0; double val4 = get();
                assertEquals(val4, (isStableEnabled ? (isStableEnabled ? 1.0 : 2.0)
                                                    : 4.0));

                c.v[0][0] = new double[1][1]; c.v[0][0][0][0] = 5.0; double val5 = get();
                assertEquals(val5, (isStableEnabled ? (isStableEnabled ? 1.0 : 2.0)
                                                    : 5.0));

                c.v[0][0][0] = new double[1]; c.v[0][0][0][0] = 6.0; double val6 = get();
                assertEquals(val6, (isStableEnabled ? (isStableEnabled ? 1.0 : 2.0)
                                                    : 6.0));
            }

            {
                c.v = new double[1][1][1][1]; double[] val1 = get1();
                c.v[0][0][0] = new double[1]; double[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new double[1][1][1][1]; double[][] val1 = get2();
                c.v[0][0] = new double[1][1]; double[][] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new double[1][1][1][1]; double[][][] val1 = get3();
                c.v[0] = new double[1][1][1]; double[][][] val2 = get3();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new double[1][1][1][1]; double[][][][] val1 = get4();
                c.v = new double[1][1][1][1]; double[][][][] val2 = get4();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */
    // Dynamic Dim is higher than static
    static class ObjectArrayLowerDim0 {
        public @Stable Object v;

        public static final ObjectArrayLowerDim0 c = new ObjectArrayLowerDim0();
        public static double get() { return ((double[])c.v)[0]; }
        public static double[] get1() { return (double[])c.v; }

        public static void test() throws Exception {
            {
                c.v = new double[1]; ((double[])c.v)[0] = 1.0; double val1 = get();
                                     ((double[])c.v)[0] = 2.0; double val2 = get();

                assertEquals(val1, 1.0);
                assertEquals(val2, 2.0);
            }

            {
                c.v = new double[1]; double[] val1 = get1();
                c.v = new double[1]; double[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class ObjectArrayLowerDim1 {
        public @Stable Object[] v;

        public static final ObjectArrayLowerDim1 c = new ObjectArrayLowerDim1();
        public static double get() { return ((double[][])c.v)[0][0]; }
        public static double[] get1() { return (double[])(c.v[0]); }
        public static Object[] get2() { return c.v; }

        public static void test() throws Exception {
            {
                c.v = new double[1][1]; ((double[][])c.v)[0][0] = 1.0; double val1 = get();
                                        ((double[][])c.v)[0][0] = 2.0; double val2 = get();

                assertEquals(val1, 1.0);
                assertEquals(val2, 2.0);
            }

            {
                c.v = new double[1][1]; c.v[0] = new double[0]; double[] val1 = get1();
                                        c.v[0] = new double[0]; double[] val2 = get1();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new double[0][0]; Object[] val1 = get2();
                c.v = new double[0][0]; Object[] val2 = get2();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class ObjectArrayLowerDim2 {
        public @Stable Object[][] v;

        public static final ObjectArrayLowerDim2 c = new ObjectArrayLowerDim2();
        public static double get() { return ((double[][][])c.v)[0][0][0]; }
        public static double[] get1() { return (double[])(c.v[0][0]); }
        public static double[][] get2() { return (double[][])(c.v[0]); }
        public static Object[][] get3() { return c.v; }

        public static void test() throws Exception {
            {
                c.v = new double[1][1][1]; ((double[][][])c.v)[0][0][0] = 1.0; double val1 = get();
                                           ((double[][][])c.v)[0][0][0] = 2.0; double val2 = get();

                assertEquals(val1, 1.0);
                assertEquals(val2, 2.0);
            }

            {
                c.v = new double[1][1][1]; c.v[0][0] = new double[0]; double[] val1 = get1();
                                           c.v[0][0] = new double[0]; double[] val2 = get1();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new double[1][1][1]; c.v[0] = new double[0][0]; double[][] val1 = get2();
                                           c.v[0] = new double[0][0]; double[][] val2 = get2();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new double[0][0][0]; Object[][] val1 = get3();
                c.v = new double[0][0][0]; Object[][] val2 = get3();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class NestedStableField {
        static class A {
            public @Stable double a;

        }
        public @Stable A v;

        public static final NestedStableField c = new NestedStableField();
        public static A get() { return c.v; }
        public static double get1() { return get().a; }

        public static void test() throws Exception {
            {
                c.v = new A(); c.v.a = 1.0; A val1 = get();
                               c.v.a = 2.0; A val2 = get();

                assertEquals(val1.a, 2.0);
                assertEquals(val2.a, 2.0);
            }

            {
                c.v = new A(); c.v.a = 1.0; double val1 = get1();
                               c.v.a = 2.0; double val2 = get1();
                c.v = new A(); c.v.a = 3.0; double val3 = get1();

                assertEquals(val1, 1.0);
                assertEquals(val2, (isStableEnabled ? 1.0 : 2.0));
                assertEquals(val3, (isStableEnabled ? 1.0 : 3.0));
            }
        }
    }

    /* ==================================================== */

    static class NestedStableField1 {
        static class A {
            public @Stable double a;
            public @Stable A next;
        }
        public @Stable A v;

        public static final NestedStableField1 c = new NestedStableField1();
        public static A get() { return c.v.next.next.next.next.next.next.next; }
        public static double get1() { return get().a; }

        public static void test() throws Exception {
            {
                c.v = new A(); c.v.next = new A();   c.v.next.next  = c.v;
                               c.v.a = 1.0; c.v.next.a = 1.0; A val1 = get();
                               c.v.a = 2.0; c.v.next.a = 2.0; A val2 = get();

                assertEquals(val1.a, 2.0);
                assertEquals(val2.a, 2.0);
            }

            {
                c.v = new A(); c.v.next = c.v;
                               c.v.a = 1.0; double val1 = get1();
                               c.v.a = 2.0; double val2 = get1();
                c.v = new A(); c.v.next = c.v;
                               c.v.a = 3.0; double val3 = get1();

                assertEquals(val1, 1.0);
                assertEquals(val2, (isStableEnabled ? 1.0 : 2.0));
                assertEquals(val3, (isStableEnabled ? 1.0 : 3.0));
            }
        }
    }
   /* ==================================================== */

    static class NestedStableField2 {
        static class A {
            public @Stable double a;
            public @Stable A left;
            public         A right;
        }

        public @Stable A v;

        public static final NestedStableField2 c = new NestedStableField2();
        public static double get() { return c.v.left.left.left.a; }
        public static double get1() { return c.v.left.left.right.left.a; }

        public static void test() throws Exception {
            {
                c.v = new A(); c.v.left = c.v.right = c.v;
                               c.v.a = 1.0; double val1 = get(); double val2 = get1();
                               c.v.a = 2.0; double val3 = get(); double val4 = get1();

                assertEquals(val1, 1.0);
                assertEquals(val3, (isStableEnabled ? 1.0 : 2.0));

                assertEquals(val2, 1.0);
                assertEquals(val4, 2.0);
            }
        }
    }

    /* ==================================================== */

    static class NestedStableField3 {
        static class A {
            public @Stable double a;
            public @Stable A[] left;
            public         A[] right;
        }

        public @Stable A[] v;

        public static final NestedStableField3 c = new NestedStableField3();
        public static double get() { return c.v[0].left[1].left[0].left[1].a; }
        public static double get1() { return c.v[1].left[0].left[1].right[0].left[1].a; }

        public static void test() throws Exception {
            {
                A elem = new A();
                c.v = new A[] { elem, elem }; c.v[0].left = c.v[0].right = c.v;
                               elem.a = 1.0; double val1 = get(); double val2 = get1();
                               elem.a = 2.0; double val3 = get(); double val4 = get1();

                assertEquals(val1, 1.0);
                assertEquals(val3, (isStableEnabled ? 1.0 : 2.0));

                assertEquals(val2, 1.0);
                assertEquals(val4, 2.0);
            }
        }
    }

    /* ==================================================== */
    // Auxiliary methods
    static void assertEquals(double i, double j) { if (i != j)  throw new AssertionError(i + " != " + j); }
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
