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
 * @bug 8200282
 * @summary arraycopy converted as a series of loads/stores uses wrong slice for loads
 *
 * @run main/othervm -XX:-TieredCompilation -XX:-BackgroundCompilation -XX:-UseOnStackReplacement -XX:CompileCommand=dontinline,ACasLoadsStoresBadMem::not_inlined  ACasLoadsStoresBadMem
 *
 */

public class ACasLoadsStoresBadMem {
    public static void main(String[] args) {
        int[] dst = new int[5];
        for (int i = 0; i < 20_000; i++) {
            test1(dst, 1);
            for (int j = 1; j < 5; j++) {
                if (dst[j] != j) {
                    throw new RuntimeException("Bad copy ");
                }
            }
        }
    }

    private static void test1(int[] dst, int dstPos) {
        int[] src = new int[4];
        not_inlined();
        src[0] = 1;
        src[1] = 2;
        src[2] = 3;
        src[3] = 4;
        System.arraycopy(src, 0, dst, dstPos, 4);
    }

    private static void not_inlined() {
    }
}
