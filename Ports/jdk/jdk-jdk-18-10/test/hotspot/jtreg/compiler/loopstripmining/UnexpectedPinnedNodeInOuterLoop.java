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
 * @bug 8202950
 * @summary C2: assert(found_sfpt) failed: no node in loop that's not input to safepoint
 *
 * @run main/othervm -Xcomp -Xbatch -XX:CompileOnly=UnexpectedPinnedNodeInOuterLoop -XX:-TieredCompilation UnexpectedPinnedNodeInOuterLoop
 *
 */

public class UnexpectedPinnedNodeInOuterLoop {

    public static final int N = 400;

    public static volatile float fFld=0.488F;
    public static volatile int iFld=143;

    public static void lMeth(int i2) {
        int i20=95, i21=-163, i22=-11, iArr[]=new int[N], iArr2[]=new int[N];
        byte by1=-97;

        for (i20 = 15; 253 > i20; ++i20) {
            iFld += i21;
            for (i22 = 1; 7 > i22; i22++) {
                iArr[i20 + 1] >>= i20;
            }
            fFld = i2;
            iArr2[i20] -= (int)2.302F;
        }
    }

    public static void main(String[] strArr) {
        lMeth(0);
    }
}
