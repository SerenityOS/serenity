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
 * @test TestStableLong
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
 *                                 compiler.stable.TestStableLong
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:-TieredCompilation
 *                                 -XX:-FoldStableValues
 *                                 compiler.stable.TestStableLong
 *
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                                 -XX:+FoldStableValues
 *                                 compiler.stable.TestStableLong
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                                 -XX:-FoldStableValues
 *                                 compiler.stable.TestStableLong
 */

package compiler.stable;

import jdk.internal.vm.annotation.Stable;

import java.lang.reflect.InvocationTargetException;

public class TestStableLong {
    static final boolean isStableEnabled    = StableConfiguration.isStableEnabled;

    public static void main(String[] args) throws Exception {
        run(DefaultValue.class);
        run(LongStable.class);
        run(DefaultStaticValue.class);
        run(StaticLongStable.class);
        run(VolatileLongStable.class);

        // @Stable arrays: Dim 1-4
        run(LongArrayDim1.class);
        run(LongArrayDim2.class);
        run(LongArrayDim3.class);
        run(LongArrayDim4.class);

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
        public @Stable long v;

        public static final DefaultValue c = new DefaultValue();
        public static long get() { return c.v; }
        public static void test() throws Exception {
                      long val1 = get();
            c.v = 1L; long val2 = get();
            assertEquals(val1, 0);
            assertEquals(val2, 1L);
        }
    }

    /* ==================================================== */

    static class LongStable {
        public @Stable long v;

        public static final LongStable c = new LongStable();
        public static long get() { return c.v; }
        public static void test() throws Exception {
            c.v = 5;              long val1 = get();
            c.v = Long.MAX_VALUE; long val2 = get();
            assertEquals(val1, 5);
            assertEquals(val2, (isStableEnabled ? 5 : Long.MAX_VALUE));
        }
    }

    /* ==================================================== */

    static class DefaultStaticValue {
        public static @Stable long v;

        public static final DefaultStaticValue c = new DefaultStaticValue();
        public static long get() { return c.v; }
        public static void test() throws Exception {
                      long val1 = get();
            c.v = 1L; long val2 = get();
            assertEquals(val1, 0);
            assertEquals(val2, 1L);
        }
    }

    /* ==================================================== */

    static class StaticLongStable {
        public static @Stable long v;

        public static final StaticLongStable c = new StaticLongStable();
        public static long get() { return c.v; }
        public static void test() throws Exception {
            c.v = 5;              long val1 = get();
            c.v = Long.MAX_VALUE; long val2 = get();
            assertEquals(val1, 5);
            assertEquals(val2, (isStableEnabled ? 5 : Long.MAX_VALUE));
        }
    }

    /* ==================================================== */

    static class VolatileLongStable {
        public @Stable volatile long v;

        public static final VolatileLongStable c = new VolatileLongStable();
        public static long get() { return c.v; }
        public static void test() throws Exception {
            c.v = 5;              long val1 = get();
            c.v = Long.MAX_VALUE; long val2 = get();
            assertEquals(val1, 5);
            assertEquals(val2, (isStableEnabled ? 5 : Long.MAX_VALUE));
        }
    }

    /* ==================================================== */
    // @Stable array == field && all components are stable

    static class LongArrayDim1 {
        public @Stable long[] v;

        public static final LongArrayDim1 c = new LongArrayDim1();
        public static long get() { return c.v[0]; }
        public static long get1() { return c.v[10]; }
        public static long[] get2() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new long[1]; c.v[0] = 1; long val1 = get();
                                   c.v[0] = 2; long val2 = get();
                assertEquals(val1, 1);
                assertEquals(val2, (isStableEnabled ? 1 : 2));

                c.v = new long[1]; c.v[0] = 3; long val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 3));
            }

            {
                c.v = new long[20]; c.v[10] = 1; long val1 = get1();
                                    c.v[10] = 2; long val2 = get1();
                assertEquals(val1, 1);
                assertEquals(val2, (isStableEnabled ? 1 : 2));

                c.v = new long[20]; c.v[10] = 3; long val3 = get1();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 3));
            }

            {
                c.v = new long[1]; long[] val1 = get2();
                c.v = new long[1]; long[] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class LongArrayDim2 {
        public @Stable long[][] v;

        public static final LongArrayDim2 c = new LongArrayDim2();
        public static long get() { return c.v[0][0]; }
        public static long[] get1() { return c.v[0]; }
        public static long[][] get2() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new long[1][1]; c.v[0][0] = 1; long val1 = get();
                                      c.v[0][0] = 2; long val2 = get();
                assertEquals(val1, 1);
                assertEquals(val2, (isStableEnabled ? 1 : 2));

                c.v = new long[1][1]; c.v[0][0] = 3; long val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 3));

                c.v[0] = new long[1]; c.v[0][0] = 4; long val4 = get();
                assertEquals(val4, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 4));
            }

            {
                c.v = new long[1][1]; long[] val1 = get1();
                c.v[0] = new long[1]; long[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new long[1][1]; long[][] val1 = get2();
                c.v = new long[1][1]; long[][] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class LongArrayDim3 {
        public @Stable long[][][] v;

        public static final LongArrayDim3 c = new LongArrayDim3();
        public static long get() { return c.v[0][0][0]; }
        public static long[] get1() { return c.v[0][0]; }
        public static long[][] get2() { return c.v[0]; }
        public static long[][][] get3() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new long[1][1][1]; c.v[0][0][0] = 1; long val1 = get();
                                         c.v[0][0][0] = 2; long val2 = get();
                assertEquals(val1, 1);
                assertEquals(val2, (isStableEnabled ? 1 : 2));

                c.v = new long[1][1][1]; c.v[0][0][0] = 3; long val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 3));

                c.v[0] = new long[1][1]; c.v[0][0][0] = 4; long val4 = get();
                assertEquals(val4, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 4));

                c.v[0][0] = new long[1]; c.v[0][0][0] = 5; long val5 = get();
                assertEquals(val5, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 5));
            }

            {
                c.v = new long[1][1][1]; long[] val1 = get1();
                c.v[0][0] = new long[1]; long[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new long[1][1][1]; long[][] val1 = get2();
                c.v[0] = new long[1][1]; long[][] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new long[1][1][1]; long[][][] val1 = get3();
                c.v = new long[1][1][1]; long[][][] val2 = get3();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class LongArrayDim4 {
        public @Stable long[][][][] v;

        public static final LongArrayDim4 c = new LongArrayDim4();
        public static long get() { return c.v[0][0][0][0]; }
        public static long[] get1() { return c.v[0][0][0]; }
        public static long[][] get2() { return c.v[0][0]; }
        public static long[][][] get3() { return c.v[0]; }
        public static long[][][][] get4() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new long[1][1][1][1]; c.v[0][0][0][0] = 1; long val1 = get();
                                            c.v[0][0][0][0] = 2; long val2 = get();
                assertEquals(val1, 1);
                assertEquals(val2, (isStableEnabled ? 1 : 2));

                c.v = new long[1][1][1][1]; c.v[0][0][0][0] = 3; long val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 3));

                c.v[0] = new long[1][1][1]; c.v[0][0][0][0] = 4; long val4 = get();
                assertEquals(val4, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 4));

                c.v[0][0] = new long[1][1]; c.v[0][0][0][0] = 5; long val5 = get();
                assertEquals(val5, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 5));

                c.v[0][0][0] = new long[1]; c.v[0][0][0][0] = 6; long val6 = get();
                assertEquals(val6, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 6));
            }

            {
                c.v = new long[1][1][1][1]; long[] val1 = get1();
                c.v[0][0][0] = new long[1]; long[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new long[1][1][1][1]; long[][] val1 = get2();
                c.v[0][0] = new long[1][1]; long[][] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new long[1][1][1][1]; long[][][] val1 = get3();
                c.v[0] = new long[1][1][1]; long[][][] val2 = get3();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new long[1][1][1][1]; long[][][][] val1 = get4();
                c.v = new long[1][1][1][1]; long[][][][] val2 = get4();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */
    // Dynamic Dim is higher than static
    static class ObjectArrayLowerDim0 {
        public @Stable Object v;

        public static final ObjectArrayLowerDim0 c = new ObjectArrayLowerDim0();
        public static long get() { return ((long[])c.v)[0]; }
        public static long[] get1() { return (long[])c.v; }

        public static void test() throws Exception {
            {
                c.v = new long[1]; ((long[])c.v)[0] = 1; long val1 = get();
                                   ((long[])c.v)[0] = 2; long val2 = get();

                assertEquals(val1, 1);
                assertEquals(val2, 2);
            }

            {
                c.v = new long[1]; long[] val1 = get1();
                c.v = new long[1]; long[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class ObjectArrayLowerDim1 {
        public @Stable Object[] v;

        public static final ObjectArrayLowerDim1 c = new ObjectArrayLowerDim1();
        public static long get() { return ((long[][])c.v)[0][0]; }
        public static long[] get1() { return (long[])(c.v[0]); }
        public static Object[] get2() { return c.v; }

        public static void test() throws Exception {
            {
                c.v = new long[1][1]; ((long[][])c.v)[0][0] = 1; long val1 = get();
                                      ((long[][])c.v)[0][0] = 2; long val2 = get();

                assertEquals(val1, 1);
                assertEquals(val2, 2);
            }

            {
                c.v = new long[1][1]; c.v[0] = new long[0]; long[] val1 = get1();
                                      c.v[0] = new long[0]; long[] val2 = get1();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new long[0][0]; Object[] val1 = get2();
                c.v = new long[0][0]; Object[] val2 = get2();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class ObjectArrayLowerDim2 {
        public @Stable Object[][] v;

        public static final ObjectArrayLowerDim2 c = new ObjectArrayLowerDim2();
        public static long get() { return ((long[][][])c.v)[0][0][0]; }
        public static long[] get1() { return (long[])(c.v[0][0]); }
        public static long[][] get2() { return (long[][])(c.v[0]); }
        public static Object[][] get3() { return c.v; }

        public static void test() throws Exception {
            {
                c.v = new long[1][1][1]; ((long[][][])c.v)[0][0][0] = 1L; long val1 = get();
                                         ((long[][][])c.v)[0][0][0] = 2L; long val2 = get();

                assertEquals(val1, 1L);
                assertEquals(val2, 2L);
            }

            {
                c.v = new long[1][1][1]; c.v[0][0] = new long[0]; long[] val1 = get1();
                                         c.v[0][0] = new long[0]; long[] val2 = get1();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new long[1][1][1]; c.v[0] = new long[0][0]; long[][] val1 = get2();
                                         c.v[0] = new long[0][0]; long[][] val2 = get2();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new long[0][0][0]; Object[][] val1 = get3();
                c.v = new long[0][0][0]; Object[][] val2 = get3();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class NestedStableField {
        static class A {
            public @Stable long a;

        }
        public @Stable A v;

        public static final NestedStableField c = new NestedStableField();
        public static A get() { return c.v; }
        public static long get1() { return get().a; }

        public static void test() throws Exception {
            {
                c.v = new A(); c.v.a = 1; A val1 = get();
                               c.v.a = 2; A val2 = get();

                assertEquals(val1.a, 2);
                assertEquals(val2.a, 2);
            }

            {
                c.v = new A(); c.v.a = 1; long val1 = get1();
                               c.v.a = 2; long val2 = get1();
                c.v = new A(); c.v.a = 3; long val3 = get1();

                assertEquals(val1, 1);
                assertEquals(val2, (isStableEnabled ? 1 : 2));
                assertEquals(val3, (isStableEnabled ? 1 : 3));
            }
        }
    }

    /* ==================================================== */

    static class NestedStableField1 {
        static class A {
            public @Stable long a;
            public @Stable A next;
        }
        public @Stable A v;

        public static final NestedStableField1 c = new NestedStableField1();
        public static A get() { return c.v.next.next.next.next.next.next.next; }
        public static long get1() { return get().a; }

        public static void test() throws Exception {
            {
                c.v = new A(); c.v.next = new A();   c.v.next.next  = c.v;
                               c.v.a = 1; c.v.next.a = 1; A val1 = get();
                               c.v.a = 2; c.v.next.a = 2; A val2 = get();

                assertEquals(val1.a, 2);
                assertEquals(val2.a, 2);
            }

            {
                c.v = new A(); c.v.next = c.v;
                               c.v.a = 1; long val1 = get1();
                               c.v.a = 2; long val2 = get1();
                c.v = new A(); c.v.next = c.v;
                               c.v.a = 3; long val3 = get1();

                assertEquals(val1, 1);
                assertEquals(val2, (isStableEnabled ? 1 : 2));
                assertEquals(val3, (isStableEnabled ? 1 : 3));
            }
        }
    }
   /* ==================================================== */

    static class NestedStableField2 {
        static class A {
            public @Stable long a;
            public @Stable A left;
            public         A right;
        }

        public @Stable A v;

        public static final NestedStableField2 c = new NestedStableField2();
        public static long get() { return c.v.left.left.left.a; }
        public static long get1() { return c.v.left.left.right.left.a; }

        public static void test() throws Exception {
            {
                c.v = new A(); c.v.left = c.v.right = c.v;
                               c.v.a = 1; long val1 = get(); long val2 = get1();
                               c.v.a = 2; long val3 = get(); long val4 = get1();

                assertEquals(val1, 1);
                assertEquals(val3, (isStableEnabled ? 1 : 2));

                assertEquals(val2, 1);
                assertEquals(val4, 2);
            }
        }
    }

    /* ==================================================== */

    static class NestedStableField3 {
        static class A {
            public @Stable long a;
            public @Stable A[] left;
            public         A[] right;
        }

        public @Stable A[] v;

        public static final NestedStableField3 c = new NestedStableField3();
        public static long get() { return c.v[0].left[1].left[0].left[1].a; }
        public static long get1() { return c.v[1].left[0].left[1].right[0].left[1].a; }

        public static void test() throws Exception {
            {
                A elem = new A();
                c.v = new A[] { elem, elem }; c.v[0].left = c.v[0].right = c.v;
                               elem.a = 1; long val1 = get(); long val2 = get1();
                               elem.a = 2; long val3 = get(); long val4 = get1();

                assertEquals(val1, 1);
                assertEquals(val3, (isStableEnabled ? 1 : 2));

                assertEquals(val2, 1);
                assertEquals(val4, 2);
            }
        }
    }

    /* ==================================================== */
    // Auxiliary methods
    static void assertEquals(long i, long j) { if (i != j)  throw new AssertionError(i + " != " + j); }
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
