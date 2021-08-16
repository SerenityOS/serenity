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
 * @test TestStableUByte
 * @summary tests on stable fields and arrays
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 * @modules java.base/jdk.internal.vm.annotation
 * @build sun.hotspot.WhiteBox
 *
 * @run main/bootclasspath/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+AlwaysIncrementalInline
 *                                 -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:-TieredCompilation
 *                                 -XX:+FoldStableValues
 *                                 -XX:CompileOnly=::get,::get1
 *                                 compiler.stable.TestStableUByte
 * @run main/bootclasspath/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+AlwaysIncrementalInline
 *                                 -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:-TieredCompilation
 *                                 -XX:-FoldStableValues
 *                                 -XX:CompileOnly=::get,::get1
 *                                 compiler.stable.TestStableUByte
 *
 * @run main/bootclasspath/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+AlwaysIncrementalInline
 *                                 -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                                 -XX:+FoldStableValues
 *                                 -XX:CompileOnly=::get,::get1
 *                                 compiler.stable.TestStableUByte
 * @run main/bootclasspath/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+AlwaysIncrementalInline
 *                                 -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xcomp
 *                                 -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                                 -XX:-FoldStableValues
 *                                 -XX:CompileOnly=::get,::get1
 *                                 compiler.stable.TestStableUByte
 */

package compiler.stable;

import jdk.internal.vm.annotation.Stable;

import java.lang.reflect.InvocationTargetException;

public class TestStableUByte {
    static final boolean isStableEnabled = StableConfiguration.isStableEnabled;

    public static void main(String[] args) throws Exception {
        run(UByteStable.class);
        run(UByteArrayDim1.class);

        if (failed) {
            throw new Error("TEST FAILED");
        }
    }

    /* ==================================================== */

    static class UByteStable {
        public @Stable byte v;

        public static final UByteStable c = new UByteStable();

        public static int get() { return c.v & 0xFF; }

        public static void test() throws Exception {
            byte v1 = -1, v2 = 1;

            c.v = v1; int r1 = get();
            c.v = v2; int r2 = get();

            assertEquals(r1, v1 & 0xFF);
            assertEquals(r2, (isStableEnabled ? v1 : v2) & 0xFF);
        }
    }

    /* ==================================================== */

    static class UByteArrayDim1 {
        public @Stable byte[] v;

        public static final UByteArrayDim1 c = new UByteArrayDim1();

        public static byte[] get()  { return c.v; }
        public static int    get1() { return get()[0] & 0xFF; }

        public static void test() throws Exception {
            byte v1 = -1, v2 = 1;

            c.v = new byte[1];
            c.v[0] = v1; int r1 = get1();
            c.v[0] = v2; int r2 = get1();

            assertEquals(r1, v1 & 0xFF);
            assertEquals(r2, (isStableEnabled ? v1 : v2) & 0xFF);
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
