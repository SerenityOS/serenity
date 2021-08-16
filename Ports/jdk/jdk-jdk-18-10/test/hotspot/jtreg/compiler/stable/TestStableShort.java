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
 * @test TestStableShort
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
 *                                 compiler.stable.TestStableShort
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:-TieredCompilation
 *                                 -XX:+FoldStableValues
 *                                 compiler.stable.TestStableShort
 *
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:-TieredCompilation
 *                                 -XX:+FoldStableValues
 *                                 compiler.stable.TestStableShort
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:-TieredCompilation
 *                                 -XX:+FoldStableValues
 *                                 compiler.stable.TestStableShort
 */

package compiler.stable;

import jdk.internal.vm.annotation.Stable;

import java.lang.reflect.InvocationTargetException;

public class TestStableShort {
    static final boolean isStableEnabled    = StableConfiguration.isStableEnabled;

    public static void main(String[] args) throws Exception {
        run(DefaultValue.class);
        run(ShortStable.class);
        run(DefaultStaticValue.class);
        run(StaticShortStable.class);
        run(VolatileShortStable.class);

        // @Stable arrays: Dim 1-4
        run(ShortArrayDim1.class);
        run(ShortArrayDim2.class);
        run(ShortArrayDim3.class);
        run(ShortArrayDim4.class);

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
        public @Stable short v;

        public static final DefaultValue c = new DefaultValue();
        public static short get() { return c.v; }
        public static void test() throws Exception {
                     short val1 = get();
            c.v = 1; short val2 = get();
            assertEquals(val1, 0);
            assertEquals(val2, 1);
        }
    }

    /* ==================================================== */

    static class ShortStable {
        public @Stable short v;

        public static final ShortStable c = new ShortStable();
        public static short get() { return c.v; }
        public static void test() throws Exception {
            c.v = 1;     short val1 = get();
            c.v = 32767; short val2 = get();
            assertEquals(val1, 1);
            assertEquals(val2, (isStableEnabled ? 1 : 32767));
        }
    }

    /* ==================================================== */

    static class DefaultStaticValue {
        public static @Stable short v;

        public static final DefaultStaticValue c = new DefaultStaticValue();
        public static short get() { return c.v; }
        public static void test() throws Exception {
                     short val1 = get();
            c.v = 1; short val2 = get();
            assertEquals(val1, 0);
            assertEquals(val2, 1);
        }
    }

    /* ==================================================== */

    static class StaticShortStable {
        public static @Stable short v;

        public static final StaticShortStable c = new StaticShortStable();
        public static short get() { return c.v; }
        public static void test() throws Exception {
            c.v = 1;     short val1 = get();
            c.v = 32767; short val2 = get();
            assertEquals(val1, 1);
            assertEquals(val2, (isStableEnabled ? 1 : 32767));
        }
    }

    /* ==================================================== */

    static class VolatileShortStable {
        public @Stable volatile short v;

        public static final VolatileShortStable c = new VolatileShortStable();
        public static short get() { return c.v; }
        public static void test() throws Exception {
            c.v = 1;     short val1 = get();
            c.v = 32767; short val2 = get();
            assertEquals(val1, 1);
            assertEquals(val2, (isStableEnabled ? 1 : 32767));
        }
    }

    /* ==================================================== */
    // @Stable array == field && all components are stable

    static class ShortArrayDim1 {
        public @Stable short[] v;

        public static final ShortArrayDim1 c = new ShortArrayDim1();
        public static short get() { return c.v[0]; }
        public static short get1() { return c.v[10]; }
        public static short[] get2() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new short[1]; c.v[0] = 1; short val1 = get();
                                    c.v[0] = 2; short val2 = get();
                assertEquals(val1, 1);
                assertEquals(val2, (isStableEnabled ? 1 : 2));

                c.v = new short[1]; c.v[0] = 3; short val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 3));
            }

            {
                c.v = new short[20]; c.v[10] = 1; short val1 = get1();
                                     c.v[10] = 2; short val2 = get1();
                assertEquals(val1, 1);
                assertEquals(val2, (isStableEnabled ? 1 : 2));

                c.v = new short[20]; c.v[10] = 3; short val3 = get1();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 3));
            }

            {
                c.v = new short[1]; short[] val1 = get2();
                c.v = new short[1]; short[] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class ShortArrayDim2 {
        public @Stable short[][] v;

        public static final ShortArrayDim2 c = new ShortArrayDim2();
        public static short get() { return c.v[0][0]; }
        public static short[] get1() { return c.v[0]; }
        public static short[][] get2() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new short[1][1]; c.v[0][0] = 1; short val1 = get();
                                       c.v[0][0] = 2; short val2 = get();
                assertEquals(val1, 1);
                assertEquals(val2, (isStableEnabled ? 1 : 2));

                c.v = new short[1][1]; c.v[0][0] = 3; short val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 3));

                c.v[0] = new short[1]; c.v[0][0] = 4; short val4 = get();
                assertEquals(val4, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 4));
            }

            {
                c.v = new short[1][1]; short[] val1 = get1();
                c.v[0] = new short[1]; short[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new short[1][1]; short[][] val1 = get2();
                c.v = new short[1][1]; short[][] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class ShortArrayDim3 {
        public @Stable short[][][] v;

        public static final ShortArrayDim3 c = new ShortArrayDim3();
        public static short get() { return c.v[0][0][0]; }
        public static short[] get1() { return c.v[0][0]; }
        public static short[][] get2() { return c.v[0]; }
        public static short[][][] get3() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new short[1][1][1]; c.v[0][0][0] = 1; short val1 = get();
                                          c.v[0][0][0] = 2; short val2 = get();
                assertEquals(val1, 1);
                assertEquals(val2, (isStableEnabled ? 1 : 2));

                c.v = new short[1][1][1]; c.v[0][0][0] = 3; short val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 3));

                c.v[0] = new short[1][1]; c.v[0][0][0] = 4; short val4 = get();
                assertEquals(val4, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 4));

                c.v[0][0] = new short[1]; c.v[0][0][0] = 5; short val5 = get();
                assertEquals(val5, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 5));
            }

            {
                c.v = new short[1][1][1]; short[] val1 = get1();
                c.v[0][0] = new short[1]; short[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new short[1][1][1]; short[][] val1 = get2();
                c.v[0] = new short[1][1]; short[][] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new short[1][1][1]; short[][][] val1 = get3();
                c.v = new short[1][1][1]; short[][][] val2 = get3();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class ShortArrayDim4 {
        public @Stable short[][][][] v;

        public static final ShortArrayDim4 c = new ShortArrayDim4();
        public static short get() { return c.v[0][0][0][0]; }
        public static short[] get1() { return c.v[0][0][0]; }
        public static short[][] get2() { return c.v[0][0]; }
        public static short[][][] get3() { return c.v[0]; }
        public static short[][][][] get4() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new short[1][1][1][1]; c.v[0][0][0][0] = 1; short val1 = get();
                                             c.v[0][0][0][0] = 2; short val2 = get();
                assertEquals(val1, 1);
                assertEquals(val2, (isStableEnabled ? 1 : 2));

                c.v = new short[1][1][1][1]; c.v[0][0][0][0] = 3; short val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 3));

                c.v[0] = new short[1][1][1]; c.v[0][0][0][0] = 4; short val4 = get();
                assertEquals(val4, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 4));

                c.v[0][0] = new short[1][1]; c.v[0][0][0][0] = 5; short val5 = get();
                assertEquals(val5, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 5));

                c.v[0][0][0] = new short[1]; c.v[0][0][0][0] = 6; short val6 = get();
                assertEquals(val6, (isStableEnabled ? (isStableEnabled ? 1 : 2)
                                                    : 6));
            }

            {
                c.v = new short[1][1][1][1]; short[] val1 = get1();
                c.v[0][0][0] = new short[1]; short[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new short[1][1][1][1]; short[][] val1 = get2();
                c.v[0][0] = new short[1][1]; short[][] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new short[1][1][1][1]; short[][][] val1 = get3();
                c.v[0] = new short[1][1][1]; short[][][] val2 = get3();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new short[1][1][1][1]; short[][][][] val1 = get4();
                c.v = new short[1][1][1][1]; short[][][][] val2 = get4();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */
    // Dynamic Dim is higher than static
    static class ObjectArrayLowerDim0 {
        public @Stable Object v;

        public static final ObjectArrayLowerDim0 c = new ObjectArrayLowerDim0();
        public static short get() { return ((short[])c.v)[0]; }
        public static short[] get1() { return (short[])c.v; }

        public static void test() throws Exception {
            {
                c.v = new short[1]; ((short[])c.v)[0] = 1; short val1 = get();
                                    ((short[])c.v)[0] = 2; short val2 = get();

                assertEquals(val1, 1);
                assertEquals(val2, 2);
            }

            {
                c.v = new short[1]; short[] val1 = get1();
                c.v = new short[1]; short[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class ObjectArrayLowerDim1 {
        public @Stable Object[] v;

        public static final ObjectArrayLowerDim1 c = new ObjectArrayLowerDim1();
        public static short get() { return ((short[][])c.v)[0][0]; }
        public static short[] get1() { return (short[])(c.v[0]); }
        public static Object[] get2() { return c.v; }

        public static void test() throws Exception {
            {
                c.v = new short[1][1]; ((short[][])c.v)[0][0] = 1; short val1 = get();
                                       ((short[][])c.v)[0][0] = 2; short val2 = get();

                assertEquals(val1, 1);
                assertEquals(val2, 2);
            }

            {
                c.v = new short[1][1]; c.v[0] = new short[0]; short[] val1 = get1();
                                       c.v[0] = new short[0]; short[] val2 = get1();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new short[0][0]; Object[] val1 = get2();
                c.v = new short[0][0]; Object[] val2 = get2();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class ObjectArrayLowerDim2 {
        public @Stable Object[][] v;

        public static final ObjectArrayLowerDim2 c = new ObjectArrayLowerDim2();
        public static short get() { return ((short[][][])c.v)[0][0][0]; }
        public static short[] get1() { return (short[])(c.v[0][0]); }
        public static short[][] get2() { return (short[][])(c.v[0]); }
        public static Object[][] get3() { return c.v; }

        public static void test() throws Exception {
            {
                c.v = new short[1][1][1]; ((short[][][])c.v)[0][0][0] = 1; short val1 = get();
                                          ((short[][][])c.v)[0][0][0] = 2; short val2 = get();

                assertEquals(val1, 1);
                assertEquals(val2, 2);
            }

            {
                c.v = new short[1][1][1]; c.v[0][0] = new short[0]; short[] val1 = get1();
                                          c.v[0][0] = new short[0]; short[] val2 = get1();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new short[1][1][1]; c.v[0] = new short[0][0]; short[][] val1 = get2();
                                          c.v[0] = new short[0][0]; short[][] val2 = get2();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new short[0][0][0]; Object[][] val1 = get3();
                c.v = new short[0][0][0]; Object[][] val2 = get3();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class NestedStableField {
        static class A {
            public @Stable short a;

        }
        public @Stable A v;

        public static final NestedStableField c = new NestedStableField();
        public static A get() { return c.v; }
        public static short get1() { return get().a; }

        public static void test() throws Exception {
            {
                c.v = new A(); c.v.a = 1; A val1 = get();
                               c.v.a = 2; A val2 = get();

                assertEquals(val1.a, 2);
                assertEquals(val2.a, 2);
            }

            {
                c.v = new A(); c.v.a = 1; short val1 = get1();
                               c.v.a = 2; short val2 = get1();
                c.v = new A(); c.v.a = 3; short val3 = get1();

                assertEquals(val1, 1);
                assertEquals(val2, (isStableEnabled ? 1 : 2));
                assertEquals(val3, (isStableEnabled ? 1 : 3));
            }
        }
    }

    /* ==================================================== */

    static class NestedStableField1 {
        static class A {
            public @Stable short a;
            public @Stable A next;
        }
        public @Stable A v;

        public static final NestedStableField1 c = new NestedStableField1();
        public static A get() { return c.v.next.next.next.next.next.next.next; }
        public static short get1() { return get().a; }

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
                               c.v.a = 1; short val1 = get1();
                               c.v.a = 2; short val2 = get1();
                c.v = new A(); c.v.next = c.v;
                               c.v.a = 3; short val3 = get1();

                assertEquals(val1, 1);
                assertEquals(val2, (isStableEnabled ? 1 : 2));
                assertEquals(val3, (isStableEnabled ? 1 : 3));
            }
        }
    }
   /* ==================================================== */

    static class NestedStableField2 {
        static class A {
            public @Stable short a;
            public @Stable A left;
            public         A right;
        }

        public @Stable A v;

        public static final NestedStableField2 c = new NestedStableField2();
        public static short get() { return c.v.left.left.left.a; }
        public static short get1() { return c.v.left.left.right.left.a; }

        public static void test() throws Exception {
            {
                c.v = new A(); c.v.left = c.v.right = c.v;
                               c.v.a = 1; short val1 = get(); short val2 = get1();
                               c.v.a = 2; short val3 = get(); short val4 = get1();

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
            public @Stable short a;
            public @Stable A[] left;
            public         A[] right;
        }

        public @Stable A[] v;

        public static final NestedStableField3 c = new NestedStableField3();
        public static short get() { return c.v[0].left[1].left[0].left[1].a; }
        public static short get1() { return c.v[1].left[0].left[1].right[0].left[1].a; }

        public static void test() throws Exception {
            {
                A elem = new A();
                c.v = new A[] { elem, elem }; c.v[0].left = c.v[0].right = c.v;
                               elem.a = 1; short val1 = get(); short val2 = get1();
                               elem.a = 2; short val3 = get(); short val4 = get1();

                assertEquals(val1, 1);
                assertEquals(val3, (isStableEnabled ? 1 : 2));

                assertEquals(val2, 1);
                assertEquals(val4, 2);
            }
        }
    }

    /* ==================================================== */
    // Auxiliary methods
    static void assertEquals(int i, int j) { if (i != j)  throw new AssertionError(i + " != " + j); }
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
