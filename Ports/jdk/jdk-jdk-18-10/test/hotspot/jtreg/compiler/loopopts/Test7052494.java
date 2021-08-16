/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7052494
 * @summary Eclipse test fails on JDK 7 b142
 *
 * @run main/othervm -Xbatch compiler.loopopts.Test7052494
 */

package compiler.loopopts;

public class Test7052494 {

    static int test1(int i, int limit) {
        int result = 0;
        while (i++ != 0) {
            if (result >= limit)
                break;
            result = i * 2;
        }
        return result;
    }

    static int test2(int i, int limit) {
        int result = 0;
        while (i-- != 0) {
            if (result <= limit)
                break;
            result = i * 2;
        }
        return result;
    }

    static void test3(int i, int limit, int arr[]) {
        while (i++ != 0) {
            if (arr[i - 1] >= limit)
                break;
            arr[i] = i * 2;
        }
    }

    static void test4(int i, int limit, int arr[]) {
        while (i-- != 0) {
            if (arr[arr.length + i + 1] <= limit)
                break;
            arr[arr.length + i] = i * 2;
        }
    }

    // Empty loop rolls through MAXINT if i > 0

    static final int limit5 = Integer.MIN_VALUE + 10000;

    static int test5(int i) {
        int result = 0;
        while (i++ != limit5) {
            result = i * 2;
        }
        return result;
    }

    // Empty loop rolls through MININT if i < 0

    static final int limit6 = Integer.MAX_VALUE - 10000;

    static int test6(int i) {
        int result = 0;
        while (i-- != limit6) {
            result = i * 2;
        }
        return result;
    }

    public static void main(String[] args) {
        boolean failed = false;
        int[] arr = new int[8];
        int[] ar3 = {0, 0, 4, 6, 8, 10, 0, 0};
        int[] ar4 = {0, 0, 0, -10, -8, -6, -4, 0};
        System.out.println("test1");
        for (int i = 0; i < 11000; i++) {
            int k = test1(1, 10);
            if (k != 10) {
                System.out.println("FAILED: " + k + " != 10");
                failed = true;
                break;
            }
        }
        System.out.println("test2");
        for (int i = 0; i < 11000; i++) {
            int k = test2(-1, -10);
            if (k != -10) {
                System.out.println("FAILED: " + k + " != -10");
                failed = true;
                break;
            }
        }
        System.out.println("test3");
        for (int i = 0; i < 11000; i++) {
            java.util.Arrays.fill(arr, 0);
            test3(1, 10, arr);
            if (!java.util.Arrays.equals(arr, ar3)) {
                System.out.println("FAILED: arr = { " + arr[0] + ", "
                        + arr[1] + ", "
                        + arr[2] + ", "
                        + arr[3] + ", "
                        + arr[4] + ", "
                        + arr[5] + ", "
                        + arr[6] + ", "
                        + arr[7] + " }");
                failed = true;
                break;
            }
        }
        System.out.println("test4");
        for (int i = 0; i < 11000; i++) {
            java.util.Arrays.fill(arr, 0);
            test4(-1, -10, arr);
            if (!java.util.Arrays.equals(arr, ar4)) {
                System.out.println("FAILED: arr = { " + arr[0] + ", "
                        + arr[1] + ", "
                        + arr[2] + ", "
                        + arr[3] + ", "
                        + arr[4] + ", "
                        + arr[5] + ", "
                        + arr[6] + ", "
                        + arr[7] + " }");
                failed = true;
                break;
            }
        }
        System.out.println("test5");
        for (int i = 0; i < 11000; i++) {
            int k = test5(limit6);
            if (k != limit5 * 2) {
                System.out.println("FAILED: " + k + " != " + limit5 * 2);
                failed = true;
                break;
            }
        }
        System.out.println("test6");
        for (int i = 0; i < 11000; i++) {
            int k = test6(limit5);
            if (k != limit6 * 2) {
                System.out.println("FAILED: " + k + " != " + limit6 * 2);
                failed = true;
                break;
            }
        }
        System.out.println("finish");
        if (failed) {
            System.exit(97);
        }
    }
}
