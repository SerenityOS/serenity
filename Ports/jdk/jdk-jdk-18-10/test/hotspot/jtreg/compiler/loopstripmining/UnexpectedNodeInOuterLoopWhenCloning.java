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
 * @bug 8202747
 * @summary C2: assert(mode == ControlAroundStripMined && use == sfpt) failed: missed a node
 *
 * @run main/othervm -Xcomp -Xbatch -XX:CompileOnly=UnexpectedNodeInOuterLoopWhenCloning -XX:-TieredCompilation UnexpectedNodeInOuterLoopWhenCloning
 *
 */

public class UnexpectedNodeInOuterLoopWhenCloning {

    public static final int N = 400;

    public static double dFld=0.37026;
    public static int iArrFld[]=new int[N];

    public static void vMeth() {

        int i5=6, i6=-42538, i7=-209, i8=163, i10=-4, i11=191;
        boolean b=true;
        double dArr[]=new double[N];

        for (i5 = 3; i5 < 245; i5++) {
            i7 = 7;
            while (--i7 > 0) {
                iArrFld[i7] = -11995;
                if (b) continue;
            }
            for (i8 = 1; i8 < 7; ++i8) {
                for (i10 = 1; i10 < 2; i10++) {
                    dFld -= i6;
                    i11 += i7;
                }
            }
        }
    }

    public static void main(String[] strArr) {
        UnexpectedNodeInOuterLoopWhenCloning _instance = new UnexpectedNodeInOuterLoopWhenCloning();
        _instance.vMeth();
    }
}
