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
 * @test
 * @bug 8159016 8202949 8203915
 * @summary Tests correct dominator information after over-unrolling a loop.
 * @requires vm.gc == "Parallel" | vm.gc == "null"
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -Xcomp -XX:-TieredCompilation -XX:-UseSwitchProfiling
 *                   -XX:-UseCountedLoopSafepoints -XX:LoopUnrollLimit=250
 *                   -XX:-UseG1GC -XX:+UseParallelGC compiler.loopopts.TestOverunrolling
 */

package compiler.loopopts;

public class TestOverunrolling {

    public static Object test1(int arg) {
        Object arr[] = new Object[3];
        int lim = (arg & 3);
        // The pre loop is executed for one iteration, initializing p[0].
        // The main loop is unrolled twice, initializing p[1], p[2], p[3] and p[4].
        // The p[3] and p[4] stores are always out of bounds and removed. However,
        // C2 is unable to remove the "over-unrolled", dead main loop. As a result,
        // there is a control path from the main loop to the post loop without a
        // memory path (because the last store was replaced by TOP). We crash
        // because we use a memory edge from a non-dominating region.
        for (int i = 0; i < lim; ++i) {
            arr[i] = new Object();
        }
        // Avoid EA
        return arr;
    }

    public static long lFld = 0;
    public static volatile double dFld = 0;

    public static void test2() {
        int iArr[] = new int[10];
        // The inner for-loop is overunrolled because we fail to determine
        // the constant lower and upper bound (6,8]. After unrolling multiple times,
        // the range check dependent CastII/ConvI2L emitted for the iArr access become
        // TOP because index 'j' is out of bounds. As a result, the memory graph is
        // corrupted with memory consuming nodes still being reachable because the dead
        // loop is not (yet) removed (Opaque1 nodes are still guarding the bounds).
        for (int i = 6; i < 10; i++) {
            for (int j = 8; j > i; j--) {
                int k = 1;
                do {
                    iArr[j] = 0;
                    switch (k) {
                    case 1:
                        lFld = 0;
                        break;
                    case 10:
                        dFld = 0;
                        break;
                    }
                } while (++k < 1);
            }
        }
    }

    // Similar to test2 but we cannot statically determine the upper bound of
    // the inner for loop and can therefore not prevent over-unrolling.
    public static void test3(int[] array) {
        int[] iArr = new int[8];
        for (int i = 0; i < array.length; i++) {
            for (int j = 5; j < i; j++) {
                int k = 1;
                do {
                    iArr[j] = 0;
                    switch (k) {
                    case 1:
                        lFld = 0;
                        break;
                    case 10:
                        dFld = 0;
                        break;
                    }
                } while (++k < 1);
            }
        }
    }

    // Similar to test3 but with negative stride and constant outer loop limit
    public static void test4(int[] array, boolean store) {
        int[] iArr = new int[8];
        for (int i = -8; i < 8; i++) {
            for (int j = 5; j > i; j--) {
                int k = 1;
                do {
                    if (store) {
                        iArr[j] = 0;
                    }
                    switch (k) {
                    case 1:
                        lFld = 0;
                        break;
                    case 10:
                        dFld = 0;
                        break;
                    }
                } while (++k < 1);
            }
        }
    }

    // The inner for-loop is over-unrolled and vectorized resulting in
    // a crash in the matcher because the memory input to a vector is top.
    public static int test5(int[] array) {
        int result = 0;
        int[] iArr = new int[8];
        for (int i = 0; i < array.length; i++) {
            for (int j = 5; j < i; j++) {
                iArr[j] += array[j];
                result += array[j];
            }
        }
        return result;
    }

    public static void main(String args[]) {
        for (int i = 0; i < 42; ++i) {
            test1(i);
        }
        test2();
        int[] array = new int[8];
        test3(array);
        test4(array, false);
        test5(array);
    }
}

