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
 * @bug 8271272
 * @summary C2: assert(!had_error) failed: bad dominance
 *
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileOnly=TestMainNeverExecuted TestMainNeverExecuted
 *
 */

public class TestMainNeverExecuted {
    public static long y;
    static int iArrFld[] = new int[400];
    static long x = 0;

    public static void main(String[] strArr) {
        for (int i1 = 0; i1 < 100; i1++)
            vMeth(3, 5);
    }
    static void vMeth(int f, int g) {
        int i3 = 23;
        int i11 = 2, i12 = 12, i13 = 32901, i14 = 43741;
        for (i11 = 7; i11 < 325; ++i11) {
            i13 = 1;
            while ((i13 += 3) < 5) {
                iArrFld[i13 - 1] = 906;
                for (i14 = i13; i14 < 5; i14 += 2) {
                    y += i14;
                    i3 += i14;
                }
            }
        }
        x += i11 + i12 + i13 + i14;
    }
}
