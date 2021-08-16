/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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
 * @bug 8263303
 * @summary C2 compilation fails with assert(found_sfpt) failed: no node in loop that's not input to safepoint
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:LoopUnrollLimit=0 TestPinnedUseInOuterLSMUnusedBySfpt
 *
 */

public class TestPinnedUseInOuterLSMUnusedBySfpt {
    public static void main(String[] args) {
        int[] array = new int[10000];
        for (int i = 0; i < 20_000; i++) {
            test(100, array, 42);
            test(100, array, 0);
        }
    }

    private static float test(int stop, int[] array, int val) {
        if (array == null) {
        }
        int j;
        for (j = 0; j < 10; j++) {

        }
        int i;
        int v = 0;
        float f = 1;
        for (i = 0; i < 10000; i++) {
            if ((j - 10) * i + val == 42) {
                f *= 2;
            } else {
                f *= 3;
            }
            v = (j - 10) * array[i];
            if (i % 10001 != i) {
                return v;
            }
        }
        return v + array[i-1] + f;
    }
}
