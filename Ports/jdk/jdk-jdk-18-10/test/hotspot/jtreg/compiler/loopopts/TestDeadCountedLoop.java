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
 * @bug 8267399
 * @summary C2: java/text/Normalizer/ConformanceTest.java test failed with assertion
 *
 * @run main/othervm -XX:-TieredCompilation -XX:-BackgroundCompilation TestDeadCountedLoop
 *
 */

public class TestDeadCountedLoop {
    public static void main(String[] args) {
        for (int i = 0; i < 20_000; i++) {
            test(true, new int[10], false, 0, 1);
            test(false, new int[10], false, 0, 1);
        }
    }

    private static int test(boolean flag, int[] array2, boolean flag2, int start, int stop) {
        if (array2 == null) {
        }
        int[] array;
        if (flag) {
            array = new int[1];
        } else {
            array = new int[2];
        }
        int len = array.length;
        int v = 1;
        for (int j = start; j < stop; j++) {
            for (int i = 0; i < len; i++) {
                if (i > 0) {
                    if (flag2) {
                        break;
                    }
                    v *= array2[i + j];
                }
            }
        }

        return v;
    }
}
