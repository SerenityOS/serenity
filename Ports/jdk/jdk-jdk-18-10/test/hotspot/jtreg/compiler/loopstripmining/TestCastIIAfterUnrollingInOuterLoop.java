/*
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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
 * @bug 8240335
 * @summary C2: assert(found_sfpt) failed: no node in loop that's not input to safepoint
 *
 * @run main/othervm -Xcomp -XX:CompileOnly=TestCastIIAfterUnrollingInOuterLoop TestCastIIAfterUnrollingInOuterLoop
 *
 */

public class TestCastIIAfterUnrollingInOuterLoop {
    public static final int N = 400;

    public static long instanceCount=727275458L;
    public static int iFld=-10;
    public static volatile short sFld=-2966;
    public static float fFld=1.682F;
    public static int iArrFld[]=new int[N];

    public static void vMeth1(int i1) {
        int i3=4;
        long lArr[]=new long[N], lArr1[]=new long[N];

        boolean b = (Integer.reverseBytes(i1 << 5) < (instanceCount++));
        for (int i2 = 1; i2 < 146; i2++) {
            iFld >>= (++i3);
        }
        if (b) {
            for (int i4 = 4; i4 < 218; ++i4) {
                instanceCount = iArrFld[i4 - 1];
                int i10 = 1;
                while (++i10 < 8) {
                    lArr1[i4] += 61384L;
                }
                lArr[i4 + 1] = i4;
                i3 += sFld;
            }
        }
    }

    public void mainTest(String[] strArr1) {
        vMeth1(iFld);
        for (int i19 = 2; i19 < 190; i19++) {
            int i20 = (int)instanceCount;
            instanceCount += (((i19 * i20) + i20) - fFld);
        }
    }
    public static void main(String[] strArr) {
        TestCastIIAfterUnrollingInOuterLoop _instance = new TestCastIIAfterUnrollingInOuterLoop();
        for (int i = 0; i < 10; i++) {
            _instance.mainTest(strArr);
        }
    }
}
