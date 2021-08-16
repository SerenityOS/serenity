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

/**
 * @test
 * @bug 8230062
 * @summary The IR of this test contains a reduction chain which corresponds to a pack in which the 2nd last element has a usage outside of the optimized loop.
 *
 * @run main/othervm -Xbatch -XX:CompileCommand=compileonly,compiler.loopopts.superword.SuperWordIntermediateUse::test
 *      compiler.loopopts.superword.SuperWordIntermediateUse
 */

package compiler.loopopts.superword;

public class SuperWordIntermediateUse {

    private int iFld;
    private int[] iArr = new int[1024];

    public void test() {
        int local = 4;

        /**
         * Before unrolling this loop:
         * iFld:   AddI 1 = -85 + Phi 1
         * local:  MulI 2 = Phi 1 * LoadI 3
         *
         * This loop is now unrolled. 'local' is a reduction. The loop is first copied:
         * iFldCopy:  AddI C1 = -85 + Phi C1
         * localCopy: MulI C2 = Phi C1 * LoadI C3
         *
         * iFld:   AddI 1 = -85 + Phi 1
         * local:  MulI 2 = Phi 1 * LoadI 3
         *
         * Then, the unnecessary nodes like phis are removed from the original loop by igvn:
         * (iFldCopy:  AddI C1 = -85 + Phi C1) field store optimized away
         * localCopy: MulI C2 = Phi C1 * LoadI C3
         *
         * iFld:   AddI 1 = -85 + MulI C2
         * local:  MulI 2 = MulI C2 * LoadI 3 -> Input into Phi C1
         *
         *  As a result AddI 1 has an input from MulI C2 which isn't the last operation in the reduction chain
         *  Phi C1 -> MulI C2 -> MulI 2 -> Phi C1 and therefore not the last element in a pack.
         *  Additionally, AddI 1 does not belong to the loop being optimized: The store node for iFld is put outside of the loop being optimized.
         *  This triggers the assertion bug when unrolled at least 4 times which creates packs of at least size 4.
         */
        for (int i = 0; i < 1024; i++) {
            iFld = -85;
            iFld = iFld + local;
            local = local * iArr[i];
            iArr[i] = 3; // Just used to trigger SuperWord optimization
        }
    }

    public static void main(String[] strArr) {
        SuperWordIntermediateUse instance = new SuperWordIntermediateUse();
        for (int i = 0; i < 1000; i++) {
            instance.test();
        }
    }
}
