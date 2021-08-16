/*
 * Copyright (c) 2019, Red Hat, Inc. All rights reserved.
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
 * @bug 8229483
 * @summary Sinking load out of loop may trigger: assert(found_sfpt) failed: no node in loop that's not input to safepoint
 *
 * @run main/othervm -XX:-TieredCompilation -XX:-BackgroundCompilation -XX:-UseOnStackReplacement -XX:LoopMaxUnroll=0 AntiDependentLoadInOuterStripMinedLoop
 *
 */

public class AntiDependentLoadInOuterStripMinedLoop {
    private static int field;
    private static volatile int barrier;

    public static void main(String[] args) {
        int[] array = new int[1];
        A a = new A();
        for (int i = 0; i < 20_000; i++) {
            test1(array);
            test2(a, array);
            test2_helper(array, 0, 0);
        }
    }

    private static int test1(int[] array) {
        int res = 1;

        for (int i = 0; i < 10; i++) {
            barrier = 1;
            // field load doesn't float higher than here

            for (int j = 0; j < 2000; j++) {
                array[0] = j;  // seen as anti dependence by C2 during loop opts, sunk out of loop
                res *= j;
            }
        }

        return field + res + field * 2;
    }

    private static int test2(A a, int[] array) {
        int ignore = a.field;
        int res = 1;

        int k = 0;
        for (k = 0; k < 2; k++) {
        }

        for (int i = 0; i < 10; i++) {
            barrier = 1;

            for (int j = 0; j < 2000; j++) {
                test2_helper(array, k, j);
                res *= j;
            }
        }

        return a.field + res + a.field * 2;
    }

    private static void test2_helper(int[] array, int k, int j) {
        if (k == 2) {
            array[0] = j;
        }
    }

    private static class A {
        public int field;
    }
}
