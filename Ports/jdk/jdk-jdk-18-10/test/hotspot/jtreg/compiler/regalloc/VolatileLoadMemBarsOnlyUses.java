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
 * @bug 8210389
 * @summary C2: assert(n->outcnt() != 0 || C->top() == n || n->is_Proj()) failed: No dead instructions after post-alloc
 *
 * @run main/othervm -Xcomp -XX:CompileOnly=VolatileLoadMemBarsOnlyUses VolatileLoadMemBarsOnlyUses
 *
 */

public class VolatileLoadMemBarsOnlyUses {

    public static final int N = 400;
    public static long instanceCount=-94L;
    public static volatile byte byFld=-108;

    public int mainTest(String[] strArr1) {

        int i17=9, i19=1, i20=63, i21=-32916, i22=0, iArr[]=new int[N];
        boolean b1=false;
        double d3=76.18241;

        for (int i : iArr) {
            for (i17 = 2; i17 < 63; i17++) {
                if (b1) break;
                byFld += (byte)(0.131F + (i17 * i17));
            }
            for (i19 = 1; 63 > i19; ++i19) {
                for (i21 = 1; i21 < 2; i21++) {
                    d3 = i22;
                    if (b1) continue;
                    i20 = i21;
                }
                d3 -= byFld;
                instanceCount = 46725L;
            }
            switch ((((i22 >>> 1) % 4) * 5) + 91) {
            case 98:
                break;
            case 110:
                break;
            case 105:
                break;
            case 103:
                break;
            default:
            }
        }

        return i20;
    }
    public static void main(String[] strArr) {
        VolatileLoadMemBarsOnlyUses _instance = new VolatileLoadMemBarsOnlyUses();
        for (int i = 0; i < 10; i++ ) {
            _instance.mainTest(strArr);
        }
    }
}
