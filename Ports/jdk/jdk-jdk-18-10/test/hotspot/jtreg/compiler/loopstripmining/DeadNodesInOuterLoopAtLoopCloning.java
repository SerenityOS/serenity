/*
 * Copyright (c) 2019, Red Hat, Inc. All rights reserved.
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
 * @bug 8230061
 * @summary loop unrolling breaks when outer strip mined loop contains dead node
 *
 * @run main/othervm -Xmx1G DeadNodesInOuterLoopAtLoopCloning
 *
 */

public class DeadNodesInOuterLoopAtLoopCloning {

    public static final int N = 400;

    public static long instanceCount=-2288355609708559532L;

    public static double checkSum(double[] a) {
        double sum = 0;
        for (int j = 0; j < a.length; j++) {
            sum += (a[j] / (j + 1) + a[j] % (j + 1));
        }
        return sum;
    }

    public static int iMeth(double d1) {

        int i4=6022, i5=-211, i6=-15841, iArr[]=new int[N];
        double d2=-8.78129, dArr[]=new double[N];

        i5 = 1;
        do {
            i6 = 1;
            while (++i6 < 5) {
                i4 = -933;
                i4 *= i4;
                dArr[i5 + 1] = i4;
                i4 -= i4;
                d2 = 1;
                do {
                    iArr[(int)(d2 + 1)] += (int)instanceCount;
                    try {
                        i4 = (i4 % -51430);
                        i4 = (iArr[i6] % 31311);
                        iArr[i6 + 1] = (24197 / i5);
                    } catch (ArithmeticException a_e) {}
                    i4 -= (int)instanceCount;
                    i4 <<= i5;
                    i4 &= 12;
                } while (++d2 < 1);
            }
        } while (++i5 < 320);
        long meth_res = Double.doubleToLongBits(checkSum(dArr));
        return (int)meth_res;
    }

    public static void main(String[] strArr) {
        DeadNodesInOuterLoopAtLoopCloning _instance = new DeadNodesInOuterLoopAtLoopCloning();
        for (int i = 0; i < 10 * 320; i++ ) {
            _instance.iMeth(0.8522);
        }
    }
}
