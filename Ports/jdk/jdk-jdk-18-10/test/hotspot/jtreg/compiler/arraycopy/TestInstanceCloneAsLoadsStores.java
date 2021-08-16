/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6700100 8156760 8248226
 * @summary small instance clone as loads/stores
 * @library /
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   -XX:CompileCommand=dontinline,compiler.arraycopy.TestInstanceCloneAsLoadsStores::m*
 *                   compiler.arraycopy.TestInstanceCloneAsLoadsStores
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   -XX:CompileCommand=dontinline,compiler.arraycopy.TestInstanceCloneAsLoadsStores::m*
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:+StressArrayCopyMacroNode
 *                   compiler.arraycopy.TestInstanceCloneAsLoadsStores
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   -XX:CompileCommand=dontinline,compiler.arraycopy.TestInstanceCloneAsLoadsStores::m*
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:-ReduceInitialCardMarks
 *                   compiler.arraycopy.TestInstanceCloneAsLoadsStores
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   -XX:CompileCommand=dontinline,compiler.arraycopy.TestInstanceCloneAsLoadsStores::m*
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:-ReduceInitialCardMarks -XX:-ReduceBulkZeroing
 *                   compiler.arraycopy.TestInstanceCloneAsLoadsStores
 */

package compiler.arraycopy;

public class TestInstanceCloneAsLoadsStores extends TestInstanceCloneUtils {

    // Should be compiled as loads/stores
    static Object m1(D src) throws CloneNotSupportedException {
        return src.clone();
    }

    // Should be compiled as adds of src (dest allocation eliminated)
    static int m2(D src) throws CloneNotSupportedException {
        D dest = (D)src.clone();
        return dest.i1 + dest.i2 + ((int)dest.i3) + dest.i4 + dest.i5;
    }

    // Should be compiled as arraycopy stub call (object too large)
    static int m3(E src) throws CloneNotSupportedException {
        E dest = (E)src.clone();
        return dest.i1 + dest.i2 + dest.i3 + dest.i4 + dest.i5 +
            dest.i6 + dest.i7 + dest.i8 + dest.i9;
    }

    // Need profiling on src's type to be able to know number of
    // fields. Cannot clone as loads/stores if compile doesn't use it.
    static Object m4(A src) throws CloneNotSupportedException {
        return src.clone();
    }

    // Same as above but should optimize out dest allocation
    static int m5(A src) throws CloneNotSupportedException {
        A dest = (A)src.clone();
        return dest.i1 + dest.i2 + dest.i3 + dest.i4 + dest.i5;
    }

    // Check that if we have no fields to clone we do fine
    static Object m6(F src) throws CloneNotSupportedException {
        return src.clone();
    }

    // With virtual call to clone: clone inlined from profling which
    // gives us exact type of src so we can clone it with
    // loads/stores.
    static G m7(G src) throws CloneNotSupportedException {
        return (G)src.myclone();
    }

    // Virtual call to clone but single target: exact type unknown,
    // clone intrinsic uses profiling to determine exact type and
    // clone with loads/stores.
    static J m8(J src) throws CloneNotSupportedException {
        return (J)src.myclone();
    }

    public static void main(String[] args) throws Exception {

        TestInstanceCloneAsLoadsStores test = new TestInstanceCloneAsLoadsStores();

        test.doTest(d, "m1");
        test.doTest(d, "m2");
        test.doTest(e, "m3");
        test.doTest(a, "m4");
        test.doTest(a, "m5");
        test.doTest(f, "m6");
        test.doTest(g, "m7");
        test.doTest(k, "m8");

        if (!test.success) {
            throw new RuntimeException("some tests failed");
        }

    }
}
