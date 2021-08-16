/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8255368 8240632
 * @summary Tests corner cases of Math.exp
 */

public class ExpCornerCaseTests {
    private ExpCornerCaseTests(){}

    public static void main(String... args) {
        int failures = 0;

        failures += testExpCornerCases();

        if (failures > 0) {
            System.err.printf("Testing exp corner cases incurred %d failures.%n", failures);
            throw new RuntimeException();
        }
    }

    private static int testExpCornerCases() {
        int failures = 0;
        double [][] testCases = {
           {+0x4.0p8,                  Double.POSITIVE_INFINITY},
           {+0x2.71p12,                Double.POSITIVE_INFINITY},

           // Identified special cases in IEEE 754 exp operation
           {+0.0,                      1.0},
           {-0.0,                      1.0},
         };

        for (double[] testCase : testCases) {
            failures += testExp(testCase[0], testCase[1]);
        }

        return failures;
    }

    private static int testExp(double input, double expected) {
        int failures = 0;
        failures += Tests.test("StrictMath.exp", input, StrictMath.exp(input), expected);
        failures += Tests.test("Math.exp", input, Math.exp(input), expected);
        return failures;
    }
}
