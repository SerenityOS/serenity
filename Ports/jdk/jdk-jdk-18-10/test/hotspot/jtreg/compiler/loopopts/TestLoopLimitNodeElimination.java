/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8260370
 * @summary C2: LoopLimit node is not eliminated
 *
 * @run main/othervm
 *      -Xcomp
 *      -XX:CompileOnly=compiler/loopopts/TestLoopLimitNodeElimination
 *      compiler.loopopts.TestLoopLimitNodeElimination
 */

package compiler.loopopts;

// the test code is derived from a randomly generated test
public class TestLoopLimitNodeElimination {
    private static class MyException extends RuntimeException { }
    private static final int ITERATIONS = 100000;
    private static final int SIZE = 400;

    private static int counter = 0;

    int[] array1 = new int[SIZE];

    void test() {
        int[] array2 = new int[SIZE];
        array1[2] = 0;
        array1 = array1;
        for (long i = 301; i > 2; i -= 2) {
            int j = 1;
            do {
               for (int k = (int) i; k < 1; k++) { }
            } while (++j < 4);
        }

        counter++;
        if (counter == ITERATIONS) {
            throw new MyException();
        }
    }

    public static void main(String[] args) {
        try {
            var test = new TestLoopLimitNodeElimination();
            while (true) {
                test.test();
            }
        } catch (MyException e) {
            // expected
        }
    }
}

