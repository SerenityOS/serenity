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
 * @bug 8247763
 * @summary assert(outer->outcnt() == 2) failed: 'only phis' failure in LoopNode::verify_strip_mined()
 *
 * @run main/othervm -Xcomp -XX:CompileOnly=TestStoreSunkToOuterLoop TestStoreSunkToOuterLoop
 *
 */

public class TestStoreSunkToOuterLoop {

    public static final int N = 400;
    public static long instanceCount=-62761L;
    public static boolean bFld=false;
    public static int iArrFld[]=new int[N];

    public void mainTest() {

        int i15=226, i16=54621, i19=780;
        float f3=0.671F, f4=-101.846F;

        i15 = 1;
        do {
            if (bFld) continue;
            for (i16 = 1; i16 < 101; ++i16) {
                iArrFld[i16 - 1] = i15;
                instanceCount = i16;
            }
        } while (++i15 < 248);
        f3 += -2061721519L;
        for (f4 = 324; f4 > 3; f4--) {
            for (i19 = 4; i19 < 78; ++i19) {
                f3 -= -11;
            }
        }

        System.out.println(instanceCount);
    }

    public static void main(String[] strArr) {
        TestStoreSunkToOuterLoop _instance = new TestStoreSunkToOuterLoop();
        for (int i = 0; i < 10; i++ ) {
            _instance.mainTest();
        }
    }
}
