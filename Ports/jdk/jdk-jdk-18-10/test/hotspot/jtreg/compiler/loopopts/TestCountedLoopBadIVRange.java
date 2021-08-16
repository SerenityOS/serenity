/*
 * Copyright (c) 2017, Red Hat, Inc. All rights reserved.
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
 * @bug 8190375
 * @summary Bad range for IV phi when exit condition is a not equal test
 * @run main/othervm -XX:-TieredCompilation TestCountedLoopBadIVRange
 *
 */


public class TestCountedLoopBadIVRange {

    static int test1(int[] arr) {
        int j = 0;
        int res = 0;
        for (int i = 0; i < 2; i++) {
            // When entered with j == 10, exit condition never
            // succeeds so range of values for j can't be computed
            // from exit condition
            for (; j != 5; j++) {
                if (j >= 20) {
                    break;
                }
                res += arr[j];
            }
            j = 10;
        }
        return res;
    }

    static int test2(int[] arr) {
        int j = 10;
        int res = 0;
        for (int i = 0; i < 2; i++) {
            // Same as above but loop variable is decreasing
            for (; j != 5; j--) {
                if (j < 0) {
                    break;
                }
                res += arr[j];
            }
            j = 1;
        }
        return res;
    }

    public static void main(String[] args) {
        int[] arr = new int[20];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = i;
        }
        for (int i = 0; i < 20_000; i++) {
            int res = test1(arr);
            if (res != 155) {
                throw new RuntimeException("Incorrect result " + res);
            }
            res = test2(arr);
            if (res != 41) {
                throw new RuntimeException("Incorrect result " + res);
            }
        }
    }
}
