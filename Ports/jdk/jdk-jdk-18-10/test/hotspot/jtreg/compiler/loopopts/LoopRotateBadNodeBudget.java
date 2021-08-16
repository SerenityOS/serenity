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
 * @bug 8231565
 * @summary Node estimate for loop rotate is not correct/sufficient:
 *          assert(delta <= 2 * required) failed: Bad node estimate ...
 *
 * @requires vm.compiler2.enabled & !vm.graal.enabled
 *
 * @run main/othervm -XX:PartialPeelNewPhiDelta=5 LoopRotateBadNodeBudget
 * @run main/othervm -Xbatch -XX:PartialPeelNewPhiDelta=5 LoopRotateBadNodeBudget
 *
 * @run main/othervm LoopRotateBadNodeBudget
 * @run main/othervm -Xbatch LoopRotateBadNodeBudget
 *
 * NOTE: Test-case seldom manifesting the problem on fast machines.
 */

public class LoopRotateBadNodeBudget {

    int h;
    float j(int a, int b) {
        double d = 0.19881;
        int c, e[] = new int[9];
        c = 1;
        while (++c < 12)
            switch ((c % 7 * 5) + 122) {
                case 156:
                case 46128:
                case 135:
                case 148:
                case 127:
                    break;
                default:
            }
        while ((d += 2) < 62)
            ;
        long k = l(e);
        return k;
    }
    long l(int[] a) {
        long m = 0;
        for (int i = 0; i < a.length; i++)
            m = a[i];
        return m;
    }
    void f(String[] g) {
        int i = 2;
        for (; i < 20000; ++i)
            j(3, h);
    }
    public static void main(String[] o) {
        try {
            LoopRotateBadNodeBudget n = new LoopRotateBadNodeBudget();
            n.f(o);
        } catch (Exception ex) {
        }
    }
}
