/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8229499
 * @summary Node estimate for loop unrolling is not correct/sufficient:
 *          assert(delta <= 2 * required) failed: Bad node estimate ...
 *
 * @requires !vm.graal.enabled
 *
 * @run main/othervm -XX:-TieredCompilation -XX:-BackgroundCompilation
 *                   LoopUnrollBadNodeBudget
 *
 */

public class LoopUnrollBadNodeBudget {

    int a;
    long b;
    int c;
    int d(long e, short f, int g) {
        int h, j = 2, k, l[][] = new int[a][];
        for (h = 8; h < 58; ++h)
            for (k = 1; 7 > k; ++k)
                switch (h % 9 * 5 + 43) {
                    case 70:
                    case 65:
                    case 86:
                    case 81:
                    case 62:
                    case 69:
                    case 74:
                        g = j;
                }
        long m = u(l);
        return (int)m;
    }
    void n(int p, int o) { d(b, (short)0, p); }
    void r(String[] q) {
        int i = 4;
        n(i, c);
    }
    long u(int[][] a) {
        long sum = 0;
        return sum;
    }
    public static void main(String[] t) {
        try {
            LoopUnrollBadNodeBudget s = new LoopUnrollBadNodeBudget();
            for (int i = 5000; i > 0; i--)
                s.r(t);
        } catch (Exception ex) {
        }
    }
}
