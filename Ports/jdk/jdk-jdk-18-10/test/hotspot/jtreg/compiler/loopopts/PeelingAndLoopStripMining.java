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

/*
 * @test
 * @bug 8229994
 * @summary Tests dominance in PhaseIdealLoop::get_early_ctrl_for_expensive() if an expensive SqrtD node is peeled.
 *
 * @run main/othervm -Xbatch -XX:-TieredCompilation
 *                   -XX:CompileCommand=compileonly,compiler.loopopts.PeelingAndLoopStripMining::*
 *                   compiler.loopopts.PeelingAndLoopStripMining
 */

package compiler.loopopts;

public class PeelingAndLoopStripMining {

    static double dFld = 3.3;

    public void test(int k) {
        for (int i = 1; i < 100; i++) {
            // Loop invariant check triggers loop peeling
            if (k == 300) {
                return;
            }

            /*
             * sqrtAdd() is inlined which contains an expensive node SqrtD with a control input from the above IfTrue node 'ifT'
             * (flipped in IR, i.e. when not returning). PhaseIdealLoop::peeled_dom_test_elim() will change the control input
             * of SqrtD to the peeled IfTrue node 'pifT' of 'ifT' and test if 'pifT' is on the idom chain starting from 'ifT'.
             * This failed previously because the new loop entry from the peeled iteration was always set as idom of the
             * CountedLoop node and not of the OuterStripMinedLoop node if present. Thus, 'pifT' was not found on the idom chain
             * starting from 'ifT' resulting in a bad graph assertion failure in PhaseIdealLoop::get_early_ctrl_for_expensive().
             */
            dFld = sqrtAdd(i);
        }
    }

    public static void main(String[] strArr) {
        PeelingAndLoopStripMining _instance = new SubClass();
        for (int i = 0; i < 10000; i++ ) {
            _instance.test(12);
        }

        _instance = new PeelingAndLoopStripMining();
        for (int i = 0; i < 10000; i++ ) {
            _instance.test(300);
        }
        for (int i = 0; i < 10000; i++ ) {
            _instance.test(45);
        }
    }

    public double sqrtAdd(int i) {
        return 1.0 + Math.sqrt(i);
    }
}

class SubClass extends PeelingAndLoopStripMining {
    public double sqrtAdd(int i) {
        return 2.0 + Math.sqrt(i);
    }
}

