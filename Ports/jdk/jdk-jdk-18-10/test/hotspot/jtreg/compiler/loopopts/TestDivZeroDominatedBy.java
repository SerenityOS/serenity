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
 * @bug 8259227
 * @summary Verify that zero check is executed before division/modulo operation.
 * @requires vm.compiler2.enabled
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileOnly=compiler/loopopts/TestDivZeroDominatedBy::test
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+StressGCM -XX:StressSeed=917280111 compiler.loopopts.TestDivZeroDominatedBy
 * @run main/othervm -Xcomp -XX:-TieredCompilation -XX:CompileOnly=compiler/loopopts/TestDivZeroDominatedBy::test
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+StressGCM compiler.loopopts.TestDivZeroDominatedBy
 */

package compiler.loopopts;

public class TestDivZeroDominatedBy {

    public static int iFld = 1;
    public static int iFld1 = 2;
    public static int iFld2 = 3;
    public static int iArrFld[] = new int[10];
    public static double dFld = 1.0;

    public static void test() {
        int x = 1;
        int y = 2;
        int z = 3;

        iFld = y;
        iArrFld[5] += iFld1;
        int i = 1;
        do {
            for (int j = 0; j < 10; j++) {
                iFld2 += iFld2;
                iFld1 = iFld2;
                int k = 1;
                do {
                    iArrFld[k] = y;
                    z = iFld2;
                    dFld = x;
                    try {
                        y = iArrFld[k];
                        iArrFld[8] = 5;
                        iFld = (100 / z);
                    } catch (ArithmeticException a_e) {}
                } while (++k < 2);
            }
        } while (++i < 10);
    }

    public static void main(String[] strArr) {
        test();
    }
}

