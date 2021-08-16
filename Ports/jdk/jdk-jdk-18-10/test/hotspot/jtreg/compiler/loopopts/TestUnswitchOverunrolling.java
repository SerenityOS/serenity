/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8240227
 * @summary Test different cases of overunrolling the main loop of unswitched loops (pre/main/post) which are then not entered.
 * @run main/othervm -XX:CompileCommand=compileonly,compiler.loopopts.TestUnswitchOverunrolling::test*
 *                   -Xcomp -Xbatch -XX:-TieredCompilation compiler.loopopts.TestUnswitchOverunrolling
 * @run main/othervm -XX:CompileCommand=compileonly,compiler.loopopts.TestUnswitchOverunrolling::*
 *                   -Xcomp -Xbatch -XX:-TieredCompilation compiler.loopopts.TestUnswitchOverunrolling
 */

package compiler.loopopts;

public class TestUnswitchOverunrolling {

    public static int v = 1;
    public static int w = 2;
    public static int x = 3;
    public static int y = 4;
    public static int z = 5;

    // The inner for-loop is unswitched and pre-main-post loops are created for both unswitched loop versions.
    // Afterwards, both main loops are over-unrolled and vectorized resulting in a crash in the matcher because
    // the memory input to a vector is top.
    public static int test(int[] array) {
        int result = 0;
        int[] iArr = new int[8];
        for (int i = 0; i < array.length; i++) {
            for (int j = 5; j < i; j++) {
                if (x == 42) {
                    y = 34;
                }
                iArr[j] += array[j];
                result += array[j];
            }
        }
        return result;
    }

    // Test with additional null check predicates for objects
    public static int test2(int[] array, A a, A a2) {
        int result = 0;
        int[] iArr = new int[8];
        for (int i = 0; i < array.length; i++) {
            for (int j = 5; j < i; j++) {
                y = a2.iFld;
                if (x == 42) {
                    y = 34;
                }
                z = a.iFld;
                iArr[j] += array[j];
                result += array[j];
            }
        }
        return result;
    }

    // Test with two conditions (unswitch twice) and additional null check predicates for objects
    public static int testUnswitchTwice(int[] array, A a, A a2, A a3) {
        int result = 0;
        int[] iArr = new int[8];
        for (int i = 0; i < array.length; i++) {
            for (int j = 5; j < i; j++) {
                y = a2.iFld;
                if (x == 42) {
                    y = 34;
                }
                z = a.iFld;
                if (w == 22) {
                    y = 55;
                }
                v = a3.iFld;
                iArr[j] += array[j];
                result += array[j];
            }
        }
        return result;
    }

    // Test with three conditions (unswitch three times) and additional null check predicates for objects
    public static int testUnswitchThreeTimes(int[] array, A a, A a2, A a3) {
        int result = 0;
        int[] iArr = new int[8];
        for (int i = 0; i < array.length; i++) {
            for (int j = 5; j < i; j++) {
                y += a2.iFld;
                if (x == 42) {
                    y = 34;
                }
                if (w == 22) {
                    y = 55;
                }
                v = a3.iFld;
                if (z == 75) {
                    y = 66;
                }
                y += a.iFld + a2.iFld + a3.iFld;
                iArr[j] += array[j];
                result += array[j];
            }
        }
        return result;
    }

    public static void main(String args[]) {
        int[] array = new int[8];
        for (int i = 0; i < array.length; i++) {
            array[i] = i + 1;
        }

        check(test(array), 1, 2, 3, 4, 5);
        check(test2(array, new A(), new A()), 1, 2, 3, 6, 6);
        check(testUnswitchTwice(array, new A(), new A(), new A()), 6, 2, 3, 6, 6);
        check(testUnswitchThreeTimes(array, new A(), new A(), new A()), 6, 2, 3, 78, 6);

        for (int i = 0; i < array.length; i++) {
            if (array[i] != i + 1) {
                throw new RuntimeException("array values should not change");
            }
        }
    }

    public static void check(int result, int expV, int expW, int expX, int expY, int expZ) {
        if (result != 19 || expV != v || expW != w || expX != x || expY != y || expZ != z) {
            throw new RuntimeException("wrong execution");
        }
    }
}

class A {
    int iFld = 6;
}

