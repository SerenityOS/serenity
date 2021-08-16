/*
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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
 * @bug 8252292
 * @summary 8240795 may cause anti-dependency to be missed
 * @requires vm.gc.Parallel
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:+UseParallelGC TestMissingAntiDependency
 *
 */

public class TestMissingAntiDependency {
    public static void main(String[] args) {
        int[] src = new int[10];
        for (int i = 0; i < 20_000; i++) {
            test(src, true, true, 10);
            test(src, true, false, 10);
            test(src, false, false, 10);
        }
        src[9] = 42;
        final int v = test(src, true, true, 1);
        if (v != 42) {
            throw new RuntimeException("Incorrect return value " + v);
        }
    }

    private static int test(int[] src, boolean flag1, boolean flag2, int stop) {
        int v = 0;
        int j = 1;
        for (; j < 10; j++) {
            for (int i = 0; i < 2; i++) {

            }
        }
        int[] dst = new int[10];
        for (int i = 0; i < stop; i ++) {
            if (flag1) {
                System.arraycopy(src, 0, dst, 0, j);
                v = dst[9];
                if (flag2) {
                    src[9] = 0x42;
                }
            }
        }
        return v;
    }
}
