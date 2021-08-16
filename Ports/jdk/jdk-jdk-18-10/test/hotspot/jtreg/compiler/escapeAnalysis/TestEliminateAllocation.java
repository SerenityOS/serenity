/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8231412
 * @summary The enhancement eliminates all allocations in the loop body of test() due to an improved field zeroing elimination dominance check.
 * @run main/othervm -XX:-TieredCompilation -XX:CompileCommand=compileonly,compiler.escapeAnalysis.TestEliminateAllocation::test
 *                   compiler.escapeAnalysis.TestEliminateAllocation
 */

package compiler.escapeAnalysis;

public class TestEliminateAllocation {

    public static int a = 20;
    public static int b = 30;
    public static int c = 40;

    public void test() {
        int i = 0;

        /*
         * The resulting IR for the loop body contains 2 allocations, one Wrapper and an int array
         * The array field store in the Wrapper object 'wrapper.arr = arr' cannot be capturued due to an early bail out.
         * Therefore, the initial value of wrapper.arr is null.
         * As a result, the escape analysis marks the array allocation as not scalar replaceable:
         * 'wrapper.arr' which is null is merged with the int array object in the assignment 'wrapper.arr = arr'.
         * Both null and the int array are treated as different objects. As a result the array allocation cannot be eliminated.
         *
         * The new enhancement does not bail out early anymore and therefore escape analysis does not mark it as
         * not scalar replaceable. This results in elimination of all allocations in this method.
         */
        do {
            int[] arr = new int[] { a / b / c };
            Wrapper wrapper = new Wrapper();
            wrapper.setArr(arr);
            i++;
        }
        while (i < 10);
    }

    public static void main(String[] strArr) {
        TestEliminateAllocation _instance = new TestEliminateAllocation();
        for (int i = 0; i < 10_000; i++ ) {
            _instance.test();
        }
    }
}

class Wrapper {
    int[] arr;
    void setArr(int... many) { arr = many; }
}
