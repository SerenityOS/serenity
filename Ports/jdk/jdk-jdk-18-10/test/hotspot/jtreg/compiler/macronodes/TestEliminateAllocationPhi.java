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
 * @bug 8046698
 * @summary PhiNode inserted between AllocateNode and Initialization node confuses allocation elimination
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   compiler.macronodes.TestEliminateAllocationPhi
 */

package compiler.macronodes;

public class TestEliminateAllocationPhi {

    // This will return I when called from m(0 and once optimized will
    // go away but this will confuse escape analysis in m(): it will
    // find I as non escaping but non scalar replaceable. In its own
    // method so that we can make the profile of the if() branch look
    // like it's taken sometimes.
    static Integer m2(Integer I, int i) {
        for (; i < 10; i=(i+2)*(i+2)) {
        }
        if (i == 121) {
            return II;
        }
        return I;
    }

    static Integer II = new Integer(42);

    static int m(int[] integers, boolean flag) {
        int j = 0;
        while(true) {
            try {
                int k = integers[j++];
                // A branch that will cause loop unswitching
                if (flag) {
                    k += 42;
                }
                if (k < 1000) {
                    throw new Exception();
                }
                // Because of the try/catch the Allocate node for this
                // new will be in the loop while the Initialization
                // node will be outside the loop. When loop
                // unswitching happens, the Allocate node will be
                // cloned and the results of both will be inputs to a
                // Phi that will be between the Allocate nodes and the
                // Initialization nodes.
                Integer I = new Integer(k);

                I = m2(I, 0);

                int i = I.intValue();
                return i;
            } catch(Exception e) {
            }
        }
    }

    static public void main(String[] args) {
        for (int i = 0; i < 5000; i++) {
            m2(null, 1);
        }

        int[] integers = { 2000 };
        for (int i = 0; i < 6000; i++) {
            m(integers, (i%2) == 0);
        }
        int[] integers2 = { 1, 2, 3, 4, 5, 2000 };
        for (int i = 0; i < 10000; i++) {
            m(integers2, (i%2) == 0);
        }
    }
}
