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
 * @test TestStableBoolean
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
 *                                 compiler.stable.TestStableBoolean
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:-TieredCompilation
 *                                 -XX:-FoldStableValues
 *                                 compiler.stable.TestStableBoolean
 *
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                                 -XX:+FoldStableValues
 *                                 compiler.stable.TestStableBoolean
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                                 -XX:-FoldStableValues
 *                                 compiler.stable.TestStableBoolean
 */

package compiler.stable;

import jdk.internal.vm.annotation.Stable;

import java.lang.reflect.InvocationTargetException;

public class TestStableBoolean {
    static final boolean isStableEnabled = StableConfiguration.isStableEnabled;

    public static void main(String[] args) throws Exception {
        run(DefaultValue.class);
        run(BooleanStable.class);
        run(DefaultStaticValue.class);
        run(StaticBooleanStable.class);
        run(VolatileBooleanStable.class);

        // @Stable arrays: Dim 1-4
        run(BooleanArrayDim1.class);
        run(BooleanArrayDim2.class);
        run(BooleanArrayDim3.class);
        run(BooleanArrayDim4.class);

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
        public @Stable boolean v;

        public static final DefaultValue c = new DefaultValue();
        public static boolean get() { return c.v; }
        public static void test() throws Exception {
                        boolean val1 = get();
            c.v = true; boolean val2 = get();
            assertEquals(val1, false);
            assertEquals(val2, true);
        }
    }

    /* ==================================================== */

    static class BooleanStable {
        public @Stable boolean v;

        public static final BooleanStable c = new BooleanStable();
        public static boolean get() { return c.v; }
        public static void test() throws Exception {
            c.v = true; boolean val1 = get();
            c.v = false; boolean val2 = get();
            assertEquals(val1, true);
            assertEquals(val2, (isStableEnabled ? true : false));
        }
    }

    /* ==================================================== */

    static class DefaultStaticValue {
        public static @Stable boolean v;

        public static final DefaultStaticValue c = new DefaultStaticValue();
        public static boolean get() { return c.v; }
        public static void test() throws Exception {
                        boolean val1 = get();
            c.v = true; boolean val2 = get();
            assertEquals(val1, false);
            assertEquals(val2, true);
        }
    }

    /* ==================================================== */

    static class StaticBooleanStable {
        public static @Stable boolean v;

        public static final StaticBooleanStable c = new StaticBooleanStable();
        public static boolean get() { return c.v; }
        public static void test() throws Exception {
            c.v = true; boolean val1 = get();
            c.v = false; boolean val2 = get();
            assertEquals(val1, true);
            assertEquals(val2, (isStableEnabled ? true : false));
        }
    }

    /* ==================================================== */

    static class VolatileBooleanStable {
        public @Stable volatile boolean v;

        public static final VolatileBooleanStable c = new VolatileBooleanStable();
        public static boolean get() { return c.v; }
        public static void test() throws Exception {
            c.v = true; boolean val1 = get();
            c.v = false; boolean val2 = get();
            assertEquals(val1, true);
            assertEquals(val2, (isStableEnabled ? true : false));
        }
    }

    /* ==================================================== */
    // @Stable array == field && all components are stable

    static class BooleanArrayDim1 {
        public @Stable boolean[] v;

        public static final BooleanArrayDim1 c = new BooleanArrayDim1();
        public static boolean get() { return c.v[0]; }
        public static boolean get1() { return c.v[10]; }
        public static boolean[] get2() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new boolean[1]; c.v[0] = true;  boolean val1 = get();
                                      c.v[0] = false; boolean val2 = get();
                assertEquals(val1, true);
                assertEquals(val2, (isStableEnabled ? true : false));
            }

            {
                c.v = new boolean[20]; c.v[10] = true;  boolean val1 = get1();
                                       c.v[10] = false; boolean val2 = get1();
                assertEquals(val1, true);
                assertEquals(val2, (isStableEnabled ? true : false));
            }

            {
                c.v = new boolean[1]; boolean[] val1 = get2();
                c.v = new boolean[1]; boolean[] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class BooleanArrayDim2 {
        public @Stable boolean[][] v;

        public static final BooleanArrayDim2 c = new BooleanArrayDim2();
        public static boolean get() { return c.v[0][0]; }
        public static boolean[] get1() { return c.v[0]; }
        public static boolean[][] get2() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new boolean[1][1]; c.v[0][0] = true;  boolean val1 = get();
                                         c.v[0][0] = false; boolean val2 = get();
                assertEquals(val1, true);
                assertEquals(val2, (isStableEnabled ? true : false));

                c.v = new boolean[1][1]; c.v[0][0] = false; boolean val3 = get();
                assertEquals(val3, (isStableEnabled ? true : false));

                c.v[0] = new boolean[1]; c.v[0][0] = false; boolean val4 = get();
                assertEquals(val4, (isStableEnabled ? true : false));
            }

            {
                c.v = new boolean[1][1]; boolean[] val1 = get1();
                c.v[0] = new boolean[1]; boolean[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new boolean[1][1]; boolean[][] val1 = get2();
                c.v = new boolean[1][1]; boolean[][] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class BooleanArrayDim3 {
        public @Stable boolean[][][] v;

        public static final BooleanArrayDim3 c = new BooleanArrayDim3();
        public static boolean get() { return c.v[0][0][0]; }
        public static boolean[] get1() { return c.v[0][0]; }
        public static boolean[][] get2() { return c.v[0]; }
        public static boolean[][][] get3() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new boolean[1][1][1]; c.v[0][0][0] = true;  boolean val1 = get();
                                            c.v[0][0][0] = false; boolean val2 = get();
                assertEquals(val1, true);
                assertEquals(val2, (isStableEnabled ? true : false));

                c.v = new boolean[1][1][1]; c.v[0][0][0] = false; boolean val3 = get();
                assertEquals(val3, (isStableEnabled ? true : false));

                c.v[0] = new boolean[1][1]; c.v[0][0][0] = false; boolean val4 = get();
                assertEquals(val4, (isStableEnabled ? true : false));

                c.v[0][0] = new boolean[1]; c.v[0][0][0] = false; boolean val5 = get();
                assertEquals(val5, (isStableEnabled ? true : false));
            }

            {
                c.v = new boolean[1][1][1]; boolean[] val1 = get1();
                c.v[0][0] = new boolean[1]; boolean[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new boolean[1][1][1]; boolean[][] val1 = get2();
                c.v[0] = new boolean[1][1]; boolean[][] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new boolean[1][1][1]; boolean[][][] val1 = get3();
                c.v = new boolean[1][1][1]; boolean[][][] val2 = get3();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class BooleanArrayDim4 {
        public @Stable boolean[][][][] v;

        public static final BooleanArrayDim4 c = new BooleanArrayDim4();
        public static boolean get() { return c.v[0][0][0][0]; }
        public static boolean[] get1() { return c.v[0][0][0]; }
        public static boolean[][] get2() { return c.v[0][0]; }
        public static boolean[][][] get3() { return c.v[0]; }
        public static boolean[][][][] get4() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new boolean[1][1][1][1]; c.v[0][0][0][0] = true;  boolean val1 = get();
                                               c.v[0][0][0][0] = false; boolean val2 = get();
                assertEquals(val1, true);
                assertEquals(val2, (isStableEnabled ? true : false));

                c.v = new boolean[1][1][1][1]; c.v[0][0][0][0] = false; boolean val3 = get();
                assertEquals(val3, (isStableEnabled ? true : false));

                c.v[0] = new boolean[1][1][1]; c.v[0][0][0][0] = false; boolean val4 = get();
                assertEquals(val4, (isStableEnabled ? true : false));

                c.v[0][0] = new boolean[1][1]; c.v[0][0][0][0] = false; boolean val5 = get();
                assertEquals(val5, (isStableEnabled ? true : false));

                c.v[0][0][0] = new boolean[1]; c.v[0][0][0][0] = false; boolean val6 = get();
                assertEquals(val6, (isStableEnabled ? true : false));
            }

            {
                c.v = new boolean[1][1][1][1]; boolean[] val1 = get1();
                c.v[0][0][0] = new boolean[1]; boolean[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new boolean[1][1][1][1]; boolean[][] val1 = get2();
                c.v[0][0] = new boolean[1][1]; boolean[][] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new boolean[1][1][1][1]; boolean[][][] val1 = get3();
                c.v[0] = new boolean[1][1][1]; boolean[][][] val2 = get3();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new boolean[1][1][1][1]; boolean[][][][] val1 = get4();
                c.v = new boolean[1][1][1][1]; boolean[][][][] val2 = get4();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

        }
    }

    /* ==================================================== */
    // Dynamic Dim is higher than static

    static class ObjectArrayLowerDim0 {
        public @Stable Object v;

        public static final ObjectArrayLowerDim0 c = new ObjectArrayLowerDim0();
        public static boolean get() { return ((boolean[])c.v)[0]; }
        public static boolean[] get1() { return (boolean[])c.v; }
        public static boolean[] get2() { return (boolean[])c.v; }

        public static void test() throws Exception {
            {
                c.v = new boolean[1]; ((boolean[])c.v)[0] = true;  boolean val1 = get();
                                      ((boolean[])c.v)[0] = false; boolean val2 = get();

                assertEquals(val1, true);
                assertEquals(val2, false);
            }

            {
                c.v = new boolean[1]; boolean[] val1 = get1();
                c.v = new boolean[1]; boolean[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class ObjectArrayLowerDim1 {
        public @Stable Object[] v;

        public static final ObjectArrayLowerDim1 c = new ObjectArrayLowerDim1();
        public static boolean get() { return ((boolean[][])c.v)[0][0]; }
        public static boolean[] get1() { return (boolean[])(c.v[0]); }
        public static Object[] get2() { return c.v; }

        public static void test() throws Exception {
            {
                c.v = new boolean[1][1]; ((boolean[][])c.v)[0][0] = true;  boolean val1 = get();
                                         ((boolean[][])c.v)[0][0] = false; boolean val2 = get();

                assertEquals(val1, true);
                assertEquals(val2, false);
            }

            {
                c.v = new boolean[1][1]; c.v[0] = new boolean[0]; boolean[] val1 = get1();
                                         c.v[0] = new boolean[0]; boolean[] val2 = get1();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new boolean[0][0]; Object[] val1 = get2();
                c.v = new boolean[0][0]; Object[] val2 = get2();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class ObjectArrayLowerDim2 {
        public @Stable Object[][] v;

        public static final ObjectArrayLowerDim2 c = new ObjectArrayLowerDim2();
        public static boolean get() { return ((boolean[][][])c.v)[0][0][0]; }
        public static boolean[] get1() { return (boolean[])(c.v[0][0]); }
        public static boolean[][] get2() { return (boolean[][])(c.v[0]); }
        public static Object[][] get3() { return c.v; }

        public static void test() throws Exception {
            {
                c.v = new boolean[1][1][1]; ((boolean[][][])c.v)[0][0][0] = true;  boolean val1 = get();
                                            ((boolean[][][])c.v)[0][0][0] = false; boolean val2 = get();

                assertEquals(val1, true);
                assertEquals(val2, false);
            }

            {
                c.v = new boolean[1][1][1]; c.v[0][0] = new boolean[0]; boolean[] val1 = get1();
                                            c.v[0][0] = new boolean[0]; boolean[] val2 = get1();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new boolean[1][1][1]; c.v[0] = new boolean[0][0]; boolean[][] val1 = get2();
                                            c.v[0] = new boolean[0][0]; boolean[][] val2 = get2();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new boolean[0][0][0]; Object[][] val1 = get3();
                c.v = new boolean[0][0][0]; Object[][] val2 = get3();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class NestedStableField {
        static class A {
            public @Stable boolean a;

        }
        public @Stable A v;

        public static final NestedStableField c = new NestedStableField();
        public static A get() { return c.v; }
        public static boolean get1() { return get().a; }

        public static void test() throws Exception {
            {
                c.v = new A(); c.v.a = true;  A val1 = get();
                               c.v.a = false; A val2 = get();

                assertEquals(val1.a, false);
                assertEquals(val2.a, false);
            }

            {
                c.v = new A(); c.v.a = true;  boolean val1 = get1();
                               c.v.a = false; boolean val2 = get1();
                c.v = new A(); c.v.a = false; boolean val3 = get1();

                assertEquals(val1, true);
                assertEquals(val2, (isStableEnabled ? true : false));
                assertEquals(val3, (isStableEnabled ? true : false));
            }
        }
    }

    /* ==================================================== */

    static class NestedStableField1 {
        static class A {
            public @Stable boolean a;
            public @Stable A next;
        }
        public @Stable A v;

        public static final NestedStableField1 c = new NestedStableField1();
        public static A get() { return c.v.next.next.next.next.next.next.next; }
        public static boolean get1() { return get().a; }

        public static void test() throws Exception {
            {
                c.v = new A(); c.v.next = new A(); c.v.next.next  = c.v;
                               c.v.a = true;  c.v.next.a = true;  A val1 = get();
                               c.v.a = false; c.v.next.a = false; A val2 = get();

                assertEquals(val1.a, false);
                assertEquals(val2.a, false);
            }

            {
                c.v = new A(); c.v.next = c.v;
                               c.v.a = true;  boolean val1 = get1();
                               c.v.a = false; boolean val2 = get1();
                c.v = new A(); c.v.next = c.v;
                               c.v.a = false; boolean val3 = get1();

                assertEquals(val1, true);
                assertEquals(val2, (isStableEnabled ? true : false));
                assertEquals(val3, (isStableEnabled ? true : false));
            }
        }
    }
   /* ==================================================== */

    static class NestedStableField2 {
        static class A {
            public @Stable boolean a;
            public @Stable A left;
            public         A right;
        }

        public @Stable A v;

        public static final NestedStableField2 c = new NestedStableField2();
        public static boolean get() { return c.v.left.left.left.a; }
        public static boolean get1() { return c.v.left.left.right.left.a; }

        public static void test() throws Exception {
            {
                c.v = new A(); c.v.left = c.v.right = c.v;
                               c.v.a = true;  boolean val1 = get(); boolean val2 = get1();
                               c.v.a = false; boolean val3 = get(); boolean val4 = get1();

                assertEquals(val1, true);
                assertEquals(val3, (isStableEnabled ? true : false));

                assertEquals(val2, true);
                assertEquals(val4, false);
            }
        }
    }

    /* ==================================================== */

    static class NestedStableField3 {
        static class A {
            public @Stable boolean a;
            public @Stable A[] left;
            public         A[] right;
        }

        public @Stable A[] v;

        public static final NestedStableField3 c = new NestedStableField3();
        public static boolean get() { return c.v[0].left[1].left[0].left[1].a; }
        public static boolean get1() { return c.v[1].left[0].left[1].right[0].left[1].a; }

        public static void test() throws Exception {
            {
                A elem = new A();
                c.v = new A[] { elem, elem }; c.v[0].left = c.v[0].right = c.v;
                               elem.a = true;  boolean val1 = get(); boolean val2 = get1();
                               elem.a = false; boolean val3 = get(); boolean val4 = get1();

                assertEquals(val1, true);
                assertEquals(val3, (isStableEnabled ? true : false));

                assertEquals(val2, true);
                assertEquals(val4, false);
            }
        }
    }

    /* ==================================================== */
    // Auxiliary methods
    static void assertEquals(boolean i, boolean j) { if (i != j)  throw new AssertionError(i + " != " + j); }
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
