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
 *
 */

/*
 * @test
 * @key stress randomness
 * @bug 8257822
 * @summary Verify that zero check is executed before division/modulo operation.
 * @requires vm.compiler2.enabled
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileOnly=compiler/loopopts/TestDivZeroWithSplitIf::test
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+StressGCM -XX:StressSeed=873732072 compiler.loopopts.TestDivZeroWithSplitIf
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileOnly=compiler/loopopts/TestDivZeroWithSplitIf::test
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+StressGCM compiler.loopopts.TestDivZeroWithSplitIf
 */

package compiler.loopopts;

public class TestDivZeroWithSplitIf {
    public static int iArrFld[] = new int[10];

    public static void test() {
        int x = 20;
        int y = 0;
        int z = 10;
        for (int i = 9; i < 99; i += 2) {
            for (int j = 3; j < 100; j++) {
                for (int k = 1; k < 2; k++) {
                    try {
                        x = (-65229 / y); // Division by zero
                        z = (iArrFld[5] / 8); // RangeCheckNode
                    } catch (ArithmeticException a_e) {}
                    try {
                        y = (-38077 / y);
                        z = (y / 9);
                    } catch (ArithmeticException a_e) {}
                    y = 8;
                    z += k;
                }
            }
        }
    }
    public static void main(String[] strArr) {
        for (int i = 0; i < 10; i++) {
            test();
        }
    }
}

