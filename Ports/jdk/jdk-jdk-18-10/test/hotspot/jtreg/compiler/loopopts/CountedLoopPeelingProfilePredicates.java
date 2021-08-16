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
 * @bug 8205515
 * @summary CountedLoopEndNode from peeled loop body is not candidate for profile loop predication
 *
 * @run main/othervm -XX:-TieredCompilation -XX:-BackgroundCompilation -XX:-UseOnStackReplacement CountedLoopPeelingProfilePredicates
 *
 */

import java.util.Arrays;

public class CountedLoopPeelingProfilePredicates {
    public static void main(String[] args) {
        int stop = 2;
        boolean[] flags1 = new boolean[stop];
        flags1[stop-1] = true;
        boolean[] flags2 = new boolean[stop];
        flags2[0] = true;
        boolean[] flags3 = new boolean[100];
        Arrays.fill(flags3, true);
        flags3[0] = false;

        for (int i = 0; i < 20_000; i++) {
            test_helper(stop, flags1, false);
            test_helper(stop, flags2, false);
            test_helper(stop, flags2, false);
        }
        for (int i = 0; i < 20_000; i++) {
            test(stop, flags1, false, flags3, 1);
            test(stop, flags2, false, flags3, 1);
            test(stop, flags2, false, flags3, 1);
        }
    }



    private static void test(int stop, boolean[] flags1, boolean flag2, boolean[] flags3, int inc) {
        for (int j = 0; j < 100; j+=inc) {
            if (flags3[j]) {
                test_helper(stop, flags1, flag2);
            }
        }
    }

    private static void test_helper(int stop, boolean[] flags1, boolean flag2) {
        for (int i = 0; i < stop; i++) {
            if (flags1[i]) {
                return;
            }
            if (flag2) {
            }
        }
    }
}
