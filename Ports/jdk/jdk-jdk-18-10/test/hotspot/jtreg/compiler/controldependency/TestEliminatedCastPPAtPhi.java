/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @key stress randomness
 * @bug 8139771
 * @summary Eliminating CastPP nodes at Phis when they all come from a unique input may cause crash
 * @requires vm.gc=="Serial" | vm.gc=="Parallel"
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *      -XX:+UnlockDiagnosticVMOptions -XX:+IgnoreUnrecognizedVMOptions -XX:+StressGCM
 *      compiler.controldependency.TestEliminatedCastPPAtPhi
 *
 */

package compiler.controldependency;

public class TestEliminatedCastPPAtPhi {

    static TestEliminatedCastPPAtPhi saved;
    static TestEliminatedCastPPAtPhi saved_not_null;

    int f;

    static int test(TestEliminatedCastPPAtPhi obj, int[] array, boolean flag) {
        int ret = array[0] + array[20];
        saved = obj;
        if (obj == null) {
            return ret;
        }
        saved_not_null = obj;

        // empty loop to be optimized out. Delays range check smearing
        // for the array access below until the if diamond is
        // optimized out
        int i = 0;
        for (; i < 10; i++);

        ret += array[i];

        TestEliminatedCastPPAtPhi res;
        if (flag) {
            // load is optimized out and res is obj here
            res = saved;
        } else {
            // load is optimized out and res is non null CastPP of res here
            res = saved_not_null;
        }
        // null check + CastPP here for res field load below

        // 1) null check is pushed in the branches of the if above by
        // split through phi because res is non null in the second
        // branch and the null check can be optimized out in that
        // branch. The Castpp stays here.

        // 2) null check in the first branch is also optimized out
        // because a dominating null check is found (the explicit null
        // check at the beggining of the test)

        // 3) the Phi for the if above merges a CastPP'ed value and
        // the same value so it's optimized out and replaced by the
        // uncasted value: obj

        // 4) the if above has 2 empty branches so it's optimized
        // out. The control of the CastPP that is still here is now
        // the success branch of the range check for the array access
        // above

        // 5) the loop above is optimized out, i = 10, the range check
        // for the array access above is optimized out and all its
        // uses are replaced by the range check for the array accesses
        // at the beginning of the method. The castPP here is one of
        // the uses and so its control is now the range check at the
        // beginning of the method: the control of the CastPP bypasses
        // the explicit null check

        return ret + res.f;
    }

    static public void main(String[] args) {
        int[] array = new int[100];
        TestEliminatedCastPPAtPhi obj = new TestEliminatedCastPPAtPhi();
        for (int i = 0; i < 20000; i++) {
            test(obj, array, (i%2) == 0);
        }
        test(null, array, true);
    }

}
