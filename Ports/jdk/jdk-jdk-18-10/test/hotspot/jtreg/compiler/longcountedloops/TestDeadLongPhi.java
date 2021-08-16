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
 * @bug 8263189
 * @summary C2: assert(!had_error) failed: bad dominance
 *
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileOnly=TestDeadLongPhi TestDeadLongPhi
 *
 */

public class TestDeadLongPhi {
    static int N = 400;
    static long instanceCount;

    public static void main(String[] strArr) {
        for (int i = 0; i < 10 ; i++) {
            mainTest();
        }
    }

    static void mainTest() {
        vMeth();
    }

    static void vMeth() {
        int i17 = 89, i18, i19 = 44, i20 = 2, iArr3[] = new int[N];
        long l2, lArr[] = new long[N];
        byte by = 22;
        init(iArr3, 131);
        for (i18 = 2; i18 < 350; ++i18) {
            i17 /= 8;
            for (l2 = 5; l2 > i18; l2 -= 2) {
                switch ((i18 % 3)) {
                    case 8:
                        iArr3[i18] |= i17;
                        break;
                    case 9:
                        i20 += (((l2) + by) - i19);
                        i19 += instanceCount;
                    case 10:
                }
            }
        }
    }

    public static void init(int[] a, int seed) {
        for (int j = 0; j < a.length; j++) {
            a[j] = (j % 2 == 0) ? seed + j : seed - j;
        }
    }

}

