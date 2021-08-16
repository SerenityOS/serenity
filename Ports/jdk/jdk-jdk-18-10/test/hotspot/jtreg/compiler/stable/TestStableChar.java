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
 * @test TestStableChar
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
 *                                 compiler.stable.TestStableChar
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:-TieredCompilation
 *                                 -XX:-FoldStableValues
 *                                 compiler.stable.TestStableChar
 *
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                                 -XX:+FoldStableValues
 *                                 compiler.stable.TestStableChar
 * @run main/bootclasspath/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:CompileOnly=::get,::get1,::get2,::get3,::get4
 *                                 -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                                 -XX:-FoldStableValues
 *                                 compiler.stable.TestStableChar
 */

package compiler.stable;

import jdk.internal.vm.annotation.Stable;

import java.lang.reflect.InvocationTargetException;

public class TestStableChar {
    static final boolean isStableEnabled    = StableConfiguration.isStableEnabled;

    public static void main(String[] args) throws Exception {
        run(DefaultValue.class);
        run(CharStable.class);
        run(DefaultStaticValue.class);
        run(StaticCharStable.class);
        run(VolatileCharStable.class);

        // @Stable arrays: Dim 1-4
        run(CharArrayDim1.class);
        run(CharArrayDim2.class);
        run(CharArrayDim3.class);
        run(CharArrayDim4.class);

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
        public @Stable char v;

        public static final DefaultValue c = new DefaultValue();
        public static char get() { return c.v; }
        public static void test() throws Exception {
                       char val1 = get();
            c.v = 'a'; char val2 = get();
            assertEquals(val1, 0);
            assertEquals(val2, 'a');
        }
    }

    /* ==================================================== */

    static class CharStable {
        public @Stable char v;

        public static final CharStable c = new CharStable();
        public static char get() { return c.v; }
        public static void test() throws Exception {
            c.v = 'a'; char val1 = get();
            c.v = 'b'; char val2 = get();
            assertEquals(val1, 'a');
            assertEquals(val2, (isStableEnabled ? 'a' : 'b'));
        }
    }

    /* ==================================================== */

    static class DefaultStaticValue {
        public static @Stable char v;

        public static final DefaultStaticValue c = new DefaultStaticValue();
        public static char get() { return c.v; }
        public static void test() throws Exception {
                       char val1 = get();
            c.v = 'a'; char val2 = get();
            assertEquals(val1, 0);
            assertEquals(val2, 'a');
        }
    }

    /* ==================================================== */

    static class StaticCharStable {
        public @Stable char v;

        public static final StaticCharStable c = new StaticCharStable();
        public static char get() { return c.v; }
        public static void test() throws Exception {
            c.v = 'a'; char val1 = get();
            c.v = 'b'; char val2 = get();
            assertEquals(val1, 'a');
            assertEquals(val2, (isStableEnabled ? 'a' : 'b'));
        }
    }

    /* ==================================================== */

    static class VolatileCharStable {
        public @Stable volatile char v;

        public static final VolatileCharStable c = new VolatileCharStable();
        public static char get() { return c.v; }
        public static void test() throws Exception {
            c.v = 'a'; char val1 = get();
            c.v = 'b'; char val2 = get();
            assertEquals(val1, 'a');
            assertEquals(val2, (isStableEnabled ? 'a' : 'b'));
        }
    }

    /* ==================================================== */
    // @Stable array == field && all components are stable

    static class CharArrayDim1 {
        public @Stable char[] v;

        public static final CharArrayDim1 c = new CharArrayDim1();
        public static char get() { return c.v[0]; }
        public static char get1() { return c.v[10]; }
        public static char[] get2() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new char[1]; c.v[0] = 'a'; char val1 = get();
                                   c.v[0] = 'b'; char val2 = get();
                assertEquals(val1, 'a');
                assertEquals(val2, (isStableEnabled ? 'a' : 'b'));

                c.v = new char[1]; c.v[0] = 'c'; char val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 'a' : 'b')
                                                    : 'c'));
            }

            {
                c.v = new char[20]; c.v[10] = 'a'; char val1 = get1();
                                    c.v[10] = 'b'; char val2 = get1();
                assertEquals(val1, 'a');
                assertEquals(val2, (isStableEnabled ? 'a' : 'b'));

                c.v = new char[20]; c.v[10] = 'c'; char val3 = get1();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 'a' : 'b')
                                                    : 'c'));
            }

            {
                c.v = new char[1]; char[] val1 = get2();
                c.v = new char[1]; char[] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class CharArrayDim2 {
        public @Stable char[][] v;

        public static final CharArrayDim2 c = new CharArrayDim2();
        public static char get() { return c.v[0][0]; }
        public static char[] get1() { return c.v[0]; }
        public static char[][] get2() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new char[1][1]; c.v[0][0] = 'a'; char val1 = get();
                                      c.v[0][0] = 'b'; char val2 = get();
                assertEquals(val1, 'a');
                assertEquals(val2, (isStableEnabled ? 'a' : 'b'));

                c.v = new char[1][1]; c.v[0][0] = 'c'; char val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 'a' : 'b')
                                                    : 'c'));

                c.v[0] = new char[1]; c.v[0][0] = 'd'; char val4 = get();
                assertEquals(val4, (isStableEnabled ? (isStableEnabled ? 'a' : 'b')
                                                    : 'd'));
            }

            {
                c.v = new char[1][1]; char[] val1 = get1();
                c.v[0] = new char[1]; char[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new char[1][1]; char[][] val1 = get2();
                c.v = new char[1][1]; char[][] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class CharArrayDim3 {
        public @Stable char[][][] v;

        public static final CharArrayDim3 c = new CharArrayDim3();
        public static char get() { return c.v[0][0][0]; }
        public static char[] get1() { return c.v[0][0]; }
        public static char[][] get2() { return c.v[0]; }
        public static char[][][] get3() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new char[1][1][1]; c.v[0][0][0] = 'a'; char val1 = get();
                                         c.v[0][0][0] = 'b'; char val2 = get();
                assertEquals(val1, 'a');
                assertEquals(val2, (isStableEnabled ? 'a' : 'b'));

                c.v = new char[1][1][1]; c.v[0][0][0] = 'c'; char val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 'a' : 'b')
                                                    : 'c'));

                c.v[0] = new char[1][1]; c.v[0][0][0] = 'd'; char val4 = get();
                assertEquals(val4, (isStableEnabled ? (isStableEnabled ? 'a' : 'b')
                                                    : 'd'));

                c.v[0][0] = new char[1]; c.v[0][0][0] = 'e'; char val5 = get();
                assertEquals(val5, (isStableEnabled ? (isStableEnabled ? 'a' : 'b')
                                                    : 'e'));
            }

            {
                c.v = new char[1][1][1]; char[] val1 = get1();
                c.v[0][0] = new char[1]; char[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new char[1][1][1]; char[][] val1 = get2();
                c.v[0] = new char[1][1]; char[][] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new char[1][1][1]; char[][][] val1 = get3();
                c.v = new char[1][1][1]; char[][][] val2 = get3();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class CharArrayDim4 {
        public @Stable char[][][][] v;

        public static final CharArrayDim4 c = new CharArrayDim4();
        public static char get() { return c.v[0][0][0][0]; }
        public static char[] get1() { return c.v[0][0][0]; }
        public static char[][] get2() { return c.v[0][0]; }
        public static char[][][] get3() { return c.v[0]; }
        public static char[][][][] get4() { return c.v; }
        public static void test() throws Exception {
            {
                c.v = new char[1][1][1][1]; c.v[0][0][0][0] = 'a'; char val1 = get();
                                            c.v[0][0][0][0] = 'b'; char val2 = get();
                assertEquals(val1, 'a');
                assertEquals(val2, (isStableEnabled ? 'a' : 'b'));

                c.v = new char[1][1][1][1]; c.v[0][0][0][0] = 'c'; char val3 = get();
                assertEquals(val3, (isStableEnabled ? (isStableEnabled ? 'a' : 'b')
                                                    : 'c'));

                c.v[0] = new char[1][1][1]; c.v[0][0][0][0] = 'd'; char val4 = get();
                assertEquals(val4, (isStableEnabled ? (isStableEnabled ? 'a' : 'b')
                                                    : 'd'));

                c.v[0][0] = new char[1][1]; c.v[0][0][0][0] = 'e'; char val5 = get();
                assertEquals(val5, (isStableEnabled ? (isStableEnabled ? 'a' : 'b')
                                                    : 'e'));

                c.v[0][0][0] = new char[1]; c.v[0][0][0][0] = 'f'; char val6 = get();
                assertEquals(val6, (isStableEnabled ? (isStableEnabled ? 'a' : 'b')
                                                    : 'f'));
            }

            {
                c.v = new char[1][1][1][1]; char[] val1 = get1();
                c.v[0][0][0] = new char[1]; char[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new char[1][1][1][1]; char[][] val1 = get2();
                c.v[0][0] = new char[1][1]; char[][] val2 = get2();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new char[1][1][1][1]; char[][][] val1 = get3();
                c.v[0] = new char[1][1][1]; char[][][] val2 = get3();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new char[1][1][1][1]; char[][][][] val1 = get4();
                c.v = new char[1][1][1][1]; char[][][][] val2 = get4();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */
    // Dynamic Dim is higher than static
    static class ObjectArrayLowerDim0 {
        public @Stable Object v;

        public static final ObjectArrayLowerDim0 c = new ObjectArrayLowerDim0();
        public static char get() { return ((char[])c.v)[0]; }
        public static char[] get1() { return (char[])c.v; }

        public static void test() throws Exception {
            {
                c.v = new char[1]; ((char[])c.v)[0] = 'a'; char val1 = get();
                                   ((char[])c.v)[0] = 'b'; char val2 = get();

                assertEquals(val1, 'a');
                assertEquals(val2, 'b');
            }

            {
                c.v = new char[1]; char[] val1 = get1();
                c.v = new char[1]; char[] val2 = get1();
                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class ObjectArrayLowerDim1 {
        public @Stable Object[] v;

        public static final ObjectArrayLowerDim1 c = new ObjectArrayLowerDim1();
        public static char get() { return ((char[][])c.v)[0][0]; }
        public static char[] get1() { return (char[])(c.v[0]); }
        public static Object[] get2() { return c.v; }

        public static void test() throws Exception {
            {
                c.v = new char[1][1]; ((char[][])c.v)[0][0] = 'a'; char val1 = get();
                                      ((char[][])c.v)[0][0] = 'b'; char val2 = get();

                assertEquals(val1, 'a');
                assertEquals(val2, 'b');
            }

            {
                c.v = new char[1][1]; c.v[0] = new char[0]; char[] val1 = get1();
                                      c.v[0] = new char[0]; char[] val2 = get1();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new char[0][0]; Object[] val1 = get2();
                c.v = new char[0][0]; Object[] val2 = get2();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class ObjectArrayLowerDim2 {
        public @Stable Object[][] v;

        public static final ObjectArrayLowerDim2 c = new ObjectArrayLowerDim2();
        public static char get() { return ((char[][][])c.v)[0][0][0]; }
        public static char[] get1() { return (char[])(c.v[0][0]); }
        public static char[][] get2() { return (char[][])(c.v[0]); }
        public static Object[][] get3() { return c.v; }

        public static void test() throws Exception {
            {
                c.v = new char[1][1][1]; ((char[][][])c.v)[0][0][0] = 'a'; char val1 = get();
                                         ((char[][][])c.v)[0][0][0] = 'b'; char val2 = get();

                assertEquals(val1, 'a');
                assertEquals(val2, 'b');
            }

            {
                c.v = new char[1][1][1]; c.v[0][0] = new char[0]; char[] val1 = get1();
                                         c.v[0][0] = new char[0]; char[] val2 = get1();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new char[1][1][1]; c.v[0] = new char[0][0]; char[][] val1 = get2();
                                         c.v[0] = new char[0][0]; char[][] val2 = get2();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }

            {
                c.v = new char[0][0][0]; Object[][] val1 = get3();
                c.v = new char[0][0][0]; Object[][] val2 = get3();

                assertTrue((isStableEnabled ? (val1 == val2) : (val1 != val2)));
            }
        }
    }

    /* ==================================================== */

    static class NestedStableField {
        static class A {
            public @Stable char a;

        }
        public @Stable A v;

        public static final NestedStableField c = new NestedStableField();
        public static A get() { return c.v; }
        public static char get1() { return get().a; }

        public static void test() throws Exception {
            {
                c.v = new A(); c.v.a = 'a'; A val1 = get();
                               c.v.a = 'b'; A val2 = get();

                assertEquals(val1.a, 'b');
                assertEquals(val2.a, 'b');
            }

            {
                c.v = new A(); c.v.a = 'a'; char val1 = get1();
                               c.v.a = 'b'; char val2 = get1();
                c.v = new A(); c.v.a = 'c'; char val3 = get1();

                assertEquals(val1, 'a');
                assertEquals(val2, (isStableEnabled ? 'a' : 'b'));
                assertEquals(val3, (isStableEnabled ? 'a' : 'c'));
            }
        }
    }

    /* ==================================================== */

    static class NestedStableField1 {
        static class A {
            public @Stable char a;
            public @Stable A next;
        }
        public @Stable A v;

        public static final NestedStableField1 c = new NestedStableField1();
        public static A get() { return c.v.next.next.next.next.next.next.next; }
        public static char get1() { return get().a; }

        public static void test() throws Exception {
            {
                c.v = new A(); c.v.next = new A(); c.v.next.next  = c.v;
                               c.v.a = 'a'; c.v.next.a = 'a'; A val1 = get();
                               c.v.a = 'b'; c.v.next.a = 'b'; A val2 = get();

                assertEquals(val1.a, 'b');
                assertEquals(val2.a, 'b');
            }

            {
                c.v = new A(); c.v.next = c.v;
                               c.v.a = 'a'; char val1 = get1();
                               c.v.a = 'b'; char val2 = get1();
                c.v = new A(); c.v.next = c.v;
                               c.v.a = 'c'; char val3 = get1();

                assertEquals(val1, 'a');
                assertEquals(val2, (isStableEnabled ? 'a' : 'b'));
                assertEquals(val3, (isStableEnabled ? 'a' : 'c'));
            }
        }
    }
   /* ==================================================== */

    static class NestedStableField2 {
        static class A {
            public @Stable char a;
            public @Stable A left;
            public         A right;
        }

        public @Stable A v;

        public static final NestedStableField2 c = new NestedStableField2();
        public static char get() { return c.v.left.left.left.a; }
        public static char get1() { return c.v.left.left.right.left.a; }

        public static void test() throws Exception {
            {
                c.v = new A(); c.v.left = c.v.right = c.v;
                               c.v.a = 'a'; char val1 = get(); char val2 = get1();
                               c.v.a = 'b'; char val3 = get(); char val4 = get1();

                assertEquals(val1, 'a');
                assertEquals(val3, (isStableEnabled ? 'a' : 'b'));

                assertEquals(val2, 'a');
                assertEquals(val4, 'b');
            }
        }
    }

    /* ==================================================== */

    static class NestedStableField3 {
        static class A {
            public @Stable char a;
            public @Stable A[] left;
            public         A[] right;
        }

        public @Stable A[] v;

        public static final NestedStableField3 c = new NestedStableField3();
        public static char get() { return c.v[0].left[1].left[0].left[1].a; }
        public static char get1() { return c.v[1].left[0].left[1].right[0].left[1].a; }

        public static void test() throws Exception {
            {
                A elem = new A();
                c.v = new A[] { elem, elem }; c.v[0].left = c.v[0].right = c.v;
                               elem.a = 'a'; char val1 = get(); char val2 = get1();
                               elem.a = 'b'; char val3 = get(); char val4 = get1();

                assertEquals(val1, 'a');
                assertEquals(val3, (isStableEnabled ? 'a' : 'b'));

                assertEquals(val2, 'a');
                assertEquals(val4, 'b');
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
