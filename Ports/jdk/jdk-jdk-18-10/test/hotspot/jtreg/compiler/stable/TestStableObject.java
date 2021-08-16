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
 * @test TestStableObject
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
 *                                 compiler.stable.TestStableObject
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:-TieredCompilation
 *                                 -XX:-FoldStableValues
 *                                 compiler.stable.TestStableObject
 *
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                                 -XX:+FoldStableValues
 *                                 compiler.stable.TestStableObject
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                                 -XX:-FoldStableValues
 *                                 compiler.stable.TestStableObject
 */

package compiler.stable;

import jdk.internal.vm.annotation.Stable;

import java.lang.reflect.InvocationTargetException;

public class TestStableObject {
    static final boolean isStableEnabled    = StableConfiguration.isStableEnabled;

    public static void main(String[] args) throws Exception {
        run(DefaultValue.class);
        run(ObjectStable.class);
        run(DefaultStaticValue.class);
        run(StaticObjectStable.class);
        run(VolatileObjectStable.class);

        // @Stable arrays: Dim 1-4
        run(ObjectArrayDim1.class);
        run(ObjectArrayDim2.class);
        run(ObjectArrayDim3.class);
        run(ObjectArrayDim4.class);

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

    enum Values {A, B, C, D, E, F}

    static class DefaultValue {
        public @Stable Object v;

        public static final DefaultValue c = new DefaultValue();
        public static Object get() { return c.v; }
        public static void test() throws Exception {
                            Object val1 = get();
            c.v = Values.A; Object val2 = get();
            assertEquals(val1, null);
            assertEquals(val2, Values.A);
        }
    }

    /* ==================================================== */

    static class ObjectStable {
        public @Stable Values v;

        public static final ObjectStable c = new ObjectStable ();
        public static Values get() { return c.v; }
        public static void test() throws Exception {
            c.v = Values.A; Values val1 = get();
            c.v = Values.B; Values val2 = get();
            assertEquals(val1, Values.A);
            assertEquals(val2, (isStableEnabled ? Values.A : Values.B));
        }
    }

    /* ==================================================== */

    static class DefaultStaticValue {
        public static @Stable Object v;

        public static final DefaultStaticValue c = new DefaultStaticValue();
        public static Object get() { return c.v; }
        public static void test() throws Exception {
                            Object val1 = get();
            c.v = Values.A; Object val2 = get();
            assertEquals(val1, null);
            assertEquals(val2, Values.A);
        }
    }

    /* ==================================================== */

    static class StaticObjectStable {
        public static @Stable Values v;

        public static final ObjectStable c = new ObjectStable ();
        public static Values get() { return c.v; }
        public static void test() throws Exception {
            c.v = Values.A; Values val1 = get();
            c.v = Values.B; Values val2 = get();
            assertEquals(val1, Values.A);
            assertEquals(val2, (isStableEnabled ? Values.A : Values.B));
        }
    }

    /* ==================================================== */

    static class VolatileObjectStable {
        public @Stable volatile Values v;

        public static final VolatileObjectStable c = new VolatileObjectStable ();
        public static Values get() { return c.v; }
        public static void test() throws Exception {
            c.v = Values.A; Values val1 = get();
            c.v = Values.B; Values val2 = get();
            assertEquals(val1, Values.A);
            assertEquals(val2, (isStableEnabled ? Values.A : Values.B));
        }
    }

    /* ==================================================== */
    // @Stable array == field && all components are stable

    static class ObjectArrayDim1 {
        public @Stable Object[] v;

        public static final ObjectArrayDim1 c = new ObjectArrayDim1();
        public static Object get() { return c.v[0]; }
        public static Object get1() { return c.v[10]; }
        public static Object[] get2() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new Object[1]; c.v[0] = Values.A; Object val1 = get();
                                     c.v[0] = Values.B; Object val2 = get();
                assertEquals(val1, Values.A);
                assertEquals(val2, (isStableEnabled ? Values.A : Values.B));

                c.v = new Object[1]; c.v[0] = Values.C; Object val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? Values.A : Values.B)
                                                    : Values.C));
            }

            {
                c.v = new Object[20]; c.v[10] = Values.A; Object val1 = get1();
                                      c.v[10] = Values.B; Object val2 = get1();
                assertEquals(val1, Values.A);
                assertEquals(val2, (isStableEnabled ? Values.A : Values.B));

                c.v = new Object[20]; c.v[10] = Values.C; Object val3 = get1();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? Values.A : Values.B)
                                                    : Values.C));
            }

            {
                c.v = new Object[1]; Object[] val1 = get2();
                c.v = new Object[1]; Object[] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class ObjectArrayDim2 {
        public @Stable Object[][] v;

        public static final ObjectArrayDim2 c = new ObjectArrayDim2();
        public static Object get() { return c.v[0][0]; }
        public static Object[] get1() { return c.v[0]; }
        public static Object[][] get2() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new Object[1][1]; c.v[0][0] = Values.A; Object val1 = get();
                                        c.v[0][0] = Values.B; Object val2 = get();
                assertEquals(val1, Values.A);
                assertEquals(val2, (isStableEnabled ? Values.A : Values.B));

                c.v = new Object[1][1]; c.v[0][0] = Values.C; Object val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? Values.A : Values.B)
                                                    : Values.C));

                c.v[0] = new Object[1]; c.v[0][0] = Values.D; Object val4 = get();
                assertEquals(val4, (isStableEnabled ? (isStableEnabled ? Values.A : Values.B)
                                                    : Values.D));
            }

            {
                c.v = new Object[1][1]; Object[] val1 = get1();
                c.v[0] = new Object[1]; Object[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new Object[1][1]; Object[][] val1 = get2();
                c.v = new Object[1][1]; Object[][] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class ObjectArrayDim3 {
        public @Stable Object[][][] v;

        public static final ObjectArrayDim3 c = new ObjectArrayDim3();
        public static Object get() { return c.v[0][0][0]; }
        public static Object[] get1() { return c.v[0][0]; }
        public static Object[][] get2() { return c.v[0]; }
        public static Object[][][] get3() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new Object[1][1][1]; c.v[0][0][0] = Values.A; Object val1 = get();
                                           c.v[0][0][0] = Values.B; Object val2 = get();
                assertEquals(val1, Values.A);
                assertEquals(val2, (isStableEnabled ? Values.A : Values.B));

                c.v = new Object[1][1][1]; c.v[0][0][0] = Values.C; Object val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? Values.A : Values.B)
                                                    : Values.C));

                c.v[0] = new Object[1][1]; c.v[0][0][0] = Values.D; Object val4 = get();
                assertEquals(val4, (isStableEnabled ? (isStableEnabled ? Values.A : Values.B)
                                                    : Values.D));

                c.v[0][0] = new Object[1]; c.v[0][0][0] = Values.E; Object val5 = get();
                assertEquals(val5, (isStableEnabled ? (isStableEnabled ? Values.A : Values.B)
                                                    : Values.E));
            }

            {
                c.v = new Object[1][1][1]; Object[] val1 = get1();
                c.v[0][0] = new Object[1]; Object[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new Object[1][1][1]; Object[][] val1 = get2();
                c.v[0] = new Object[1][1]; Object[][] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new Object[1][1][1]; Object[][][] val1 = get3();
                c.v = new Object[1][1][1]; Object[][][] val2 = get3();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class ObjectArrayDim4 {
        public @Stable Object[][][][] v;

        public static final ObjectArrayDim4 c = new ObjectArrayDim4();
        public static Object get() { return c.v[0][0][0][0]; }
        public static Object[] get1() { return c.v[0][0][0]; }
        public static Object[][] get2() { return c.v[0][0]; }
        public static Object[][][] get3() { return c.v[0]; }
        public static Object[][][][] get4() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new Object[1][1][1][1]; c.v[0][0][0][0] = Values.A; Object val1 = get();
                                              c.v[0][0][0][0] = Values.B; Object val2 = get();
                assertEquals(val1, Values.A);
                assertEquals(val2, (isStableEnabled ? Values.A : Values.B));

                c.v = new Object[1][1][1][1]; c.v[0][0][0][0] = Values.C; Object val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? Values.A : Values.B)
                                                    : Values.C));

                c.v[0] = new Object[1][1][1]; c.v[0][0][0][0] = Values.D; Object val4 = get();
                assertEquals(val4, (isStableEnabled ? (isStableEnabled ? Values.A : Values.B)
                                                    : Values.D));

                c.v[0][0] = new Object[1][1]; c.v[0][0][0][0] = Values.E; Object val5 = get();
                assertEquals(val5, (isStableEnabled ? (isStableEnabled ? Values.A : Values.B)
                                                    : Values.E));

                c.v[0][0][0] = new Object[1]; c.v[0][0][0][0] = Values.F; Object val6 = get();
                assertEquals(val6, (isStableEnabled ? (isStableEnabled ? Values.A : Values.B)
                                                    : Values.F));
            }

            {
                c.v = new Object[1][1][1][1]; Object[] val1 = get1();
                c.v[0][0][0] = new Object[1]; Object[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new Object[1][1][1][1]; Object[][] val1 = get2();
                c.v[0][0] = new Object[1][1]; Object[][] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new Object[1][1][1][1]; Object[][][] val1 = get3();
                c.v[0] = new Object[1][1][1]; Object[][][] val2 = get3();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new Object[1][1][1][1]; Object[][][][] val1 = get4();
                c.v = new Object[1][1][1][1]; Object[][][][] val2 = get4();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */
    // Dynamic Dim is higher than static
    static class ObjectArrayLowerDim0 {
        public @Stable Object v;

        public static final ObjectArrayLowerDim0 c = new ObjectArrayLowerDim0();
        public static Object get() { return ((Object[])c.v)[0]; }
        public static Object[] get1() { return (Object[])c.v; }

        public static void test() throws Exception {
            {
                c.v = new Object[1]; ((Object[])c.v)[0] = Values.A; Object val1 = get();
                                     ((Object[])c.v)[0] = Values.B; Object val2 = get();

                assertEquals(val1, Values.A);
                assertEquals(val2, Values.B);
            }

            {
                c.v = new Object[1]; Object[] val1 = get1();
                c.v = new Object[1]; Object[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class ObjectArrayLowerDim1 {
        public @Stable Object[] v;

        public static final ObjectArrayLowerDim1 c = new ObjectArrayLowerDim1();
        public static Object get() { return ((Object[][])c.v)[0][0]; }
        public static Object[] get1() { return (Object[])(c.v[0]); }
        public static Object[] get2() { return c.v; }

        public static void test() throws Exception {
            {
                c.v = new Object[1][1]; ((Object[][])c.v)[0][0] = Values.A; Object val1 = get();
                                        ((Object[][])c.v)[0][0] = Values.B; Object val2 = get();

                assertEquals(val1, Values.A);
                assertEquals(val2, Values.B);
            }

            {
                c.v = new Object[1][1]; c.v[0] = new Object[0]; Object[] val1 = get1();
                                        c.v[0] = new Object[0]; Object[] val2 = get1();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new Object[0][0]; Object[] val1 = get2();
                c.v = new Object[0][0]; Object[] val2 = get2();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class ObjectArrayLowerDim2 {
        public @Stable Object[][] v;

        public static final ObjectArrayLowerDim2 c = new ObjectArrayLowerDim2();
        public static Object get() { return ((Object[][][])c.v)[0][0][0]; }
        public static Object[] get1() { return (Object[])(c.v[0][0]); }
        public static Object[][] get2() { return (Object[][])(c.v[0]); }
        public static Object[][] get3() { return c.v; }

        public static void test() throws Exception {
            {
                c.v = new Object[1][1][1]; ((Object[][][])c.v)[0][0][0] = Values.A; Object val1 = get();
                                           ((Object[][][])c.v)[0][0][0] = Values.B; Object val2 = get();

                assertEquals(val1, Values.A);
                assertEquals(val2, Values.B);
            }

            {
                c.v = new Object[1][1][1]; c.v[0][0] = new Object[0]; Object[] val1 = get1();
                                           c.v[0][0] = new Object[0]; Object[] val2 = get1();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new Object[1][1][1]; c.v[0] = new Object[0][0]; Object[][] val1 = get2();
                                           c.v[0] = new Object[0][0]; Object[][] val2 = get2();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new Object[0][0][0]; Object[][] val1 = get3();
                c.v = new Object[0][0][0]; Object[][] val2 = get3();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class NestedStableField {
        static class A {
            public @Stable Object a;

        }
        public @Stable A v;

        public static final NestedStableField c = new NestedStableField();
        public static A get() { return c.v; }
        public static Object get1() { return get().a; }

        public static void test() throws Exception {
            {
                c.v = new A(); c.v.a = Values.A; A val1 = get();
                               c.v.a = Values.B; A val2 = get();

                assertEquals(val1.a, Values.B);
                assertEquals(val2.a, Values.B);
            }

            {
                c.v = new A(); c.v.a = Values.A; Object val1 = get1();
                               c.v.a = Values.B; Object val2 = get1();
                c.v = new A(); c.v.a = Values.C; Object val3 = get1();

                assertEquals(val1, Values.A);
                assertEquals(val2, (isStableEnabled ? Values.A : Values.B));
                assertEquals(val3, (isStableEnabled ? Values.A : Values.C));
            }
        }
    }

    /* ==================================================== */

    static class NestedStableField1 {
        static class A {
            public @Stable Object a;
            public @Stable A next;
        }
        public @Stable A v;

        public static final NestedStableField1 c = new NestedStableField1();
        public static A get() { return c.v.next.next.next.next.next.next.next; }
        public static Object get1() { return get().a; }

        public static void test() throws Exception {
            {
                c.v = new A(); c.v.next = new A();   c.v.next.next  = c.v;
                               c.v.a = Values.A; c.v.next.a = Values.A; A val1 = get();
                               c.v.a = Values.B; c.v.next.a = Values.B; A val2 = get();

                assertEquals(val1.a, Values.B);
                assertEquals(val2.a, Values.B);
            }

            {
                c.v = new A(); c.v.next = c.v;
                               c.v.a = Values.A; Object val1 = get1();
                               c.v.a = Values.B; Object val2 = get1();
                c.v = new A(); c.v.next = c.v;
                               c.v.a = Values.C; Object val3 = get1();

                assertEquals(val1, Values.A);
                assertEquals(val2, (isStableEnabled ? Values.A : Values.B));
                assertEquals(val3, (isStableEnabled ? Values.A : Values.C));
            }
        }
    }
   /* ==================================================== */

    static class NestedStableField2 {
        static class A {
            public @Stable Object a;
            public @Stable A left;
            public         A right;
        }

        public @Stable A v;

        public static final NestedStableField2 c = new NestedStableField2();
        public static Object get() { return c.v.left.left.left.a; }
        public static Object get1() { return c.v.left.left.right.left.a; }

        public static void test() throws Exception {
            {
                c.v = new A(); c.v.left = c.v.right = c.v;
                               c.v.a = Values.A; Object val1 = get(); Object val2 = get1();
                               c.v.a = Values.B; Object val3 = get(); Object val4 = get1();

                assertEquals(val1, Values.A);
                assertEquals(val3, (isStableEnabled ? Values.A : Values.B));

                assertEquals(val2, Values.A);
                assertEquals(val4, Values.B);
            }
        }
    }

    /* ==================================================== */

    static class NestedStableField3 {
        static class A {
            public @Stable Object a;
            public @Stable A[] left;
            public         A[] right;
        }

        public @Stable A[] v;

        public static final NestedStableField3 c = new NestedStableField3();
        public static Object get() { return c.v[0].left[1].left[0].left[1].a; }
        public static Object get1() { return c.v[1].left[0].left[1].right[0].left[1].a; }

        public static void test() throws Exception {
            {
                A elem = new A();
                c.v = new A[] { elem, elem }; c.v[0].left = c.v[0].right = c.v;
                               elem.a = Values.A; Object val1 = get(); Object val2 = get1();
                               elem.a = Values.B; Object val3 = get(); Object val4 = get1();

                assertEquals(val1, Values.A);
                assertEquals(val3, (isStableEnabled ? Values.A : Values.B));

                assertEquals(val2, Values.A);
                assertEquals(val4, Values.B);
            }
        }
    }

    /* ==================================================== */
    // Auxiliary methods
    static void assertEquals(Object i, Object j) { if (i != j)  throw new AssertionError(i + " != " + j); }
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
