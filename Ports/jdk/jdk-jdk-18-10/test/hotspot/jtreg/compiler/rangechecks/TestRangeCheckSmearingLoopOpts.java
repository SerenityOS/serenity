/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8048170
 * @summary Following range check smearing, range check cannot be replaced by dominating identical test.
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   compiler.rangechecks.TestRangeCheckSmearingLoopOpts
 */

package compiler.rangechecks;

public class TestRangeCheckSmearingLoopOpts {

    static int dummy;

    static int m1(int[] array, int i) {
        for (;;) {
            for (;;) {
                if (array[i] < 0) { // range check (i+0) dominates equivalent check below
                    break;
                }
                i++;
            }

            // A control flow that stops IfNode::up_one_dom()
            if ((i % 2)== 0) {
                if ((array[i] % 2) == 0) {
                    dummy = i;
                }
            }

            // IfNode::Ideal will rewrite some range checks if Compile::allow_range_check_smearing
            if (array[i-1] == 9) {    // range check (i-1) unchanged
                int res = array[i-3]; // range check (i-3) unchanged
                res += array[i];      // range check (i+0) unchanged
                res += array[i-2];    // removed redundant range check
                // the previous access might be hoisted by
                // PhaseIdealLoop::split_if_with_blocks_post because
                // it appears to have the same guard, but it also
                // depends on the previous guards
                return res;
            }
            i++;
        }
    }

    static public void main(String[] args) {
        int[] array = { 0, 1, 2, -3, 4, 5, -2, 7, 8, 9, -1 };
        for (int i = 0; i < 20000; i++) {
            m1(array, 0);
        }
        array[0] = -1;
        try {
            m1(array, 0);
        } catch(ArrayIndexOutOfBoundsException aioobe) {}
    }
}
