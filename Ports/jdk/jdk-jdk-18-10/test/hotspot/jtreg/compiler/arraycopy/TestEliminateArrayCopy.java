/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8076188 8246153 8248226
 * @summary arraycopy to non escaping destination may be eliminated
 * @library /
 *
 * @run main/othervm -ea -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   -XX:CompileCommand=dontinline,compiler.arraycopy.TestEliminateArrayCopy*::m*
 *                   compiler.arraycopy.TestEliminateArrayCopy
 * @run main/othervm -ea -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:+StressReflectiveCode
 *                   -XX:CompileCommand=dontinline,compiler.arraycopy.TestEliminateArrayCopy*::m*
 *                   compiler.arraycopy.TestEliminateArrayCopy
 * @run main/othervm -ea -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:+StressReflectiveCode
 *                   -XX:-ReduceInitialCardMarks -XX:-ReduceBulkZeroing
 *                   -XX:CompileCommand=dontinline,compiler.arraycopy.TestEliminateArrayCopy*::m*
 *                   compiler.arraycopy.TestEliminateArrayCopy
 */

package compiler.arraycopy;

public class TestEliminateArrayCopy {

    static class CloneTests extends TestInstanceCloneUtils {
        // object allocation and ArrayCopyNode should be eliminated
        static void m1(E src) throws CloneNotSupportedException {
            src.clone();
        }

        // both object allocations and ArrayCopyNode should be eliminated
        static void m2(Object dummy) throws CloneNotSupportedException {
            E src = new E(false);
            src.clone();
        }

        // object allocation and ArrayCopyNode should be eliminated. Fields should be loaded from src.
        static int m3(E src) throws CloneNotSupportedException {
            E dest = (E)src.clone();
            return dest.i1 + dest.i2 + dest.i3 + dest.i4 + dest.i5 +
                dest.i6 + dest.i7 + dest.i8 + dest.i9;
        }
    }

    static class ArrayCopyTests extends TestArrayCopyUtils {

        // object allocation and ArrayCopyNode should be eliminated.
        @Args(src=ArraySrc.LARGE)
        static int m1() throws CloneNotSupportedException {
            int[] array_clone = (int[])large_int_src.clone();
            return array_clone[0] + array_clone[1] + array_clone[2] +
                array_clone[3] + array_clone[4] + array_clone[5] +
                array_clone[6] + array_clone[7] + array_clone[8] +
                array_clone[9];
        }

        // object allocation and ArrayCopyNode should be eliminated.
        @Args(src=ArraySrc.LARGE)
        static int m2() {
            int[] dest = new int[10];
            System.arraycopy(large_int_src, 0, dest, 0, 10);
            return dest[0] + dest[1] + dest[2] + dest[3] + dest[4] +
                dest[5] + dest[6] + dest[7] + dest[8] + dest[9];
        }

        // object allocations and ArrayCopyNodes should be eliminated.
        @Args(src=ArraySrc.LARGE)
        static int m3() {
            int[] dest1 = new int[10];
            System.arraycopy(large_int_src, 0, dest1, 0, 10);

            int[] dest2 = new int[10];
            System.arraycopy(dest1, 0, dest2, 0, 10);

            return dest2[0] + dest2[1] + dest2[2] + dest2[3] + dest2[4] +
                dest2[5] + dest2[6] + dest2[7] + dest2[8] + dest2[9];
        }

        static class m4_class {
            Object f;
        }

        static void m4_helper() {}

        // allocations eliminated and arraycopy optimized out
        @Args(src=ArraySrc.LARGE)
        static int m4() {
            int[] dest = new int[10];
            m4_class o = new m4_class();
            o.f = dest;
            m4_helper();
            System.arraycopy(large_int_src, 0, o.f, 0, 10);
            return dest[0] + dest[1] + dest[2] + dest[3] + dest[4] +
                dest[5] + dest[6] + dest[7] + dest[8] + dest[9];
        }

        static void m5_helper() {}

        // Small copy cannot be converted to loads/stores because
        // allocation is not close enough to arraycopy but arraycopy
        // itself can be eliminated
        @Args(src=ArraySrc.SMALL, dst=ArrayDst.NEW)
        static void m5(A[] src, A[] dest) {
            A[] temp = new A[5];
            m5_helper();
            System.arraycopy(src, 0, temp, 0, 5);
            dest[0] = temp[0];
            dest[1] = temp[1];
            dest[2] = temp[2];
            dest[3] = temp[3];
            dest[4] = temp[4];
        }

        // object allocation and ArrayCopyNode should be eliminated.
        @Args(src=ArraySrc.LARGE)
        static int m6(int [] src) {
            int res = src[0] + src[1] + src[2] + src[3] + src[4] +
                src[5] + src[6] + src[7] + src[8] + src[9];

            int[] dest = new int[10];

            System.arraycopy(src, 0, dest, 0, 10);

            res += dest[0] + dest[1] + dest[2] + dest[3] + dest[4] +
                dest[5] + dest[6] + dest[7] + dest[8] + dest[9];
            return res/2;
        }

        @Args(src=ArraySrc.LARGE)
        static int m7() {
            int[] dest = new int[10];
            dest[0] = large_int_src[8];
            dest[1] = large_int_src[9];
            System.arraycopy(large_int_src, 0, dest, 2, 8);
            return dest[0] + dest[1] + dest[2] + dest[3] + dest[4] +
                dest[5] + dest[6] + dest[7] + dest[8] + dest[9];
        }
    }

    // test that OptimizePtrCompare still works
    static final Object[] m1_array = new Object[10];
    static boolean m1_array_null_element = false;
    static void m1(int i) {
        Object[] array_clone = (Object[])m1_array.clone();
        if (array_clone[i] == null) {
            m1_array_null_element = true;
        }
    }

    static public void main(String[] args) throws Exception {
        CloneTests clone_tests = new CloneTests();

        clone_tests.doTest(clone_tests.e, "m1");
        clone_tests.doTest(null, "m2");
        clone_tests.doTest(clone_tests.e, "m3");

        ArrayCopyTests ac_tests = new ArrayCopyTests();

        ac_tests.doTest("m1");
        ac_tests.doTest("m2");
        ac_tests.doTest("m3");
        ac_tests.doTest("m4");
        ac_tests.doTest("m5");
        ac_tests.doTest("m6");
        ac_tests.doTest("m7");

        if (!clone_tests.success || !ac_tests.success) {
            throw new RuntimeException("some tests failed");
        }

        // Make sure both branches of the if in m1() appear taken
        for (int i = 0; i < 7000; i++) {
            m1(0);
        }
        m1_array[0] = new Object();
        for (int i = 0; i < 20000; i++) {
            m1(0);
        }
        m1_array_null_element = false;
        m1(0);
        if (m1_array_null_element) {
            throw new RuntimeException("OptimizePtrCompare test failed");
        }
    }
}
