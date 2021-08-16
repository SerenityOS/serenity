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
 * @bug 8210390
 * @summary C2 still crashes with "assert(mode == ControlAroundStripMined && use == sfpt) failed: missed a node"
 *
 * @run main/othervm -Xcomp StripMinedLoopReorgOffsets
 *
 */

public class StripMinedLoopReorgOffsets {

    public static final int N = 400;

    public static long instanceCount=-4622920139809038929L;

    public void mainTest(String[] strArr1) {

        int i1=-211, i20=54720, i21=205, i22=2184, i23=58, i24=-50110, iArr3[]=new int[N];

        for (i20 = 16; 331 > i20; ++i20) {
            i1 = i20;
            i21 += i1;
            iArr3[i20] <<= (int)StripMinedLoopReorgOffsets.instanceCount;
            for (i22 = 4; i22 < 80; i22++) {
                i21 = i23;
                i24 = 1;
                while (++i24 < 2) {
                    try {
                        iArr3[i22] = (i23 / i1);
                    } catch (ArithmeticException a_e) {}
                }
            }
        }

        System.out.println("i1 i20 = " + i1 + "," + i20);
        System.out.println("i21 i22 i23 = " + i21 + "," + i22 + "," + i23);
        System.out.println("i24 = " + i24);
    }
    public static void main(String[] strArr) {
        StripMinedLoopReorgOffsets _instance = new StripMinedLoopReorgOffsets();
        for (int i = 0; i < 10; i++ ) {
            _instance.mainTest(strArr);
        }
    }
}
