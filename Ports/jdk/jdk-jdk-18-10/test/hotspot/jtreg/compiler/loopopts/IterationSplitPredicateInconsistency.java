/*
 * Copyright (c) 2018, Red Hat, Inc. All rights reserved.
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

/**
 * @test
 * @bug 8193130 8203915
 * @summary Bad graph when unrolled loop bounds conflicts with range checks
 * @requires vm.flavor == "server"
 *
 * @run main/othervm IterationSplitPredicateInconsistency
 * @run main/othervm -XX:-UseLoopPredicate IterationSplitPredicateInconsistency
 * @run main/othervm -XX:LoopStripMiningIter=0 IterationSplitPredicateInconsistency
 *
 */

public class IterationSplitPredicateInconsistency {
    static volatile int barrier;

    // Pollute profile so loop appears to run for a large number of iterations
    static boolean test1_helper(int start, int stop, double[] array1, double[] array2, int exit) {
        for (int i = start; i < stop; i++) {
            array1[i] = array2[i];
            if (i == exit) {
                return true;
            }
            barrier = 0x42;
        }
        return false;
    }

    static double[] test1(int start, double[] array2, int exit) {
        double[] array1 = new double[10];
        // Predication moves range checks out of loop and
        // pre/main/post loops are created. The main loop is unrolled
        // several times to the point where it's never executed but
        // compiler can't tell from the loop bounds alone. The lower
        // bound of the loop is negative and would cause range checks
        // (that were removed from the loop body) to fail.
        if (test1_helper(start, 5, array1, array2, exit)) {
            return null;
        }
        return array1;
    }

    // Same as above with other combinations of increasing/decreasing
    // loops, positive/negative stride
    static boolean test2_helper(int start, int stop, double[] array1, double[] array2, int exit) {
        for (int i = start-1; i >= stop; i--) {
            array1[i] = array2[i];
            if (i == exit) {
                return true;
            }
            barrier = 0x42;
        }
        return false;
    }

    static double[] test2(int start, double[] array2, int exit) {
        double[] array1 = new double[10];
        if (test2_helper(start, 0, array1, array2, exit)) {
            return null;
        }
        return array1;
    }

    static boolean test3_helper(int start, int stop, double[] array1, double[] array2, int exit) {
        for (int i = start; i < stop; i++) {
            array1[stop-i-1] = array2[stop-i-1];
            if (i == exit) {
                return true;
            }
            barrier = 0x42;
        }
        return false;
    }

    static double[] test3(int start, double[] array2, int exit) {
        double[] array1 = new double[5];
        if (test3_helper(start, 5, array1, array2, exit)) {
            return null;
        }
        return array1;
    }

    static boolean test4_helper(int start, int stop, int from, double[] array1, double[] array2, int exit) {
        for (int i = start-1; i >= stop; i--) {
            array1[from-i-1] = array2[from-i-1];
            if (i == exit) {
                return true;
            }
            barrier = 0x42;
        }
        return false;
    }

    static double[] test4(int start, double[] array2, int exit) {
        double[] array1 = new double[5];
        if (test4_helper(start, 0, 5, array1, array2, exit)) {
            return null;
        }
        return array1;
    }

    public static void main(String[] args) {
        double[] array2 = new double[10];
        double[] array3 = new double[1000];
        for (int i = 0; i < 20_000; i++) {
            test1_helper(0, 1000, array3, array3, 998);
            test1(0, array2, 999);
            test1(0, array2, 4);
            test2_helper(1000, 0, array3, array3, 1);
            test2(5, array2, 999);
            test2(5, array2, 1);
            test3_helper(0, 1000, array3, array3, 998);
            test3(0, array2, 999);
            test3(0, array2, 4);
            test4_helper(1000, 0, 1000, array3, array3, 1);
            test4(5, array2, 999);
            test4(5, array2, 1);
        }
    }
}
