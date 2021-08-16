/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @key stress randomness
 * @requires vm.compiler2.enabled
 * @bug 8269795
 * @summary PhaseIdealLoop::peeled_dom_test_elim wrongly moves a non-dominated test out of a loop together with control dependent data nodes.
 *          This results in a crash due to an out of bounds read of an array.
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -Xcomp -XX:-TieredCompilation -XX:+StressGCM
 *                   -XX:CompileCommand=compileonly,compiler.loopopts.TestPeelingRemoveDominatedTest compiler.loopopts.TestPeelingRemoveDominatedTest
 */

package compiler.loopopts;

public class TestPeelingRemoveDominatedTest {
    public static int N = 400;
    static boolean bFld = true;
    static int iArrFld[] = new int[N];

    public static void main(String[] strArr) {
        TestPeelingRemoveDominatedTest _instance = new TestPeelingRemoveDominatedTest();
        for (int i = 0; i < 10; i++) {
            _instance.mainTest();
        }
    }

    public void mainTest() {
        vMeth();
    }


    static void vMeth() {
        iArrFld[1] = 2;
        int i6 = 2;
        while (--i6 > 0) {
            try {
                int i3 = (iArrFld[i6 - 1] / 56);
                iArrFld[1] = (-139 % i3);
            } catch (ArithmeticException a_e) {
            }
            if (bFld) {
            }
        }
    }
}
