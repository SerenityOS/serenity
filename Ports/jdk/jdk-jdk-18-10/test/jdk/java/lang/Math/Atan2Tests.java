/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4984407
 * @summary Tests for {Math, StrictMath}.atan2
 * @author Joseph D. Darcy
 */

public class Atan2Tests {
    private Atan2Tests(){}

    static int testAtan2Case(double input1, double input2, double expected) {
        int failures = 0;
        failures += Tests.test("StrictMath.atan2(double, double)", input1, input2,
                               StrictMath.atan2(input1, input2), expected);
        failures += Tests.test("Math.atan2(double, double)", input1, input2,
                               Math.atan2(input1, input2), expected);

        return failures;
    }

    static int testAtan2() {
        int failures = 0;

        double [][] testCases = {
            {-3.0,      Double.POSITIVE_INFINITY,       -0.0},
        };

        for (double[] testCase : testCases) {
            failures+=testAtan2Case(testCase[0], testCase[1], testCase[2]);
        }

        return failures;
    }

    public static void main(String [] argv) {
        int failures = 0;

        failures += testAtan2();

        if (failures > 0) {
            System.err.println("Testing atan2 incurred "
                               + failures + " failures.");
            throw new RuntimeException();
        }
    }
}
