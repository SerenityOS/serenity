/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8240632
 * @summary Test special cases of IEEE 754 recommended ops not otherwise tested
 * @build Tests
 * @build Ieee754SpecialCaseTests
 * @run main Ieee754SpecialCaseTests
 */

public class Ieee754SpecialCaseTests {
    private Ieee754SpecialCaseTests() {throw new AssertionError("No instances for you.");}

    public static void main(String... args) {
        int failures = 0;

        failures += testSpecialCos();
        failures += testSpecialAcos();
        failures += testSpecialAtan();
        failures += testSpecialLog();

        if (failures > 0) {
            System.err.printf("Testing special cases incurred %d failures.%n", failures);
            throw new RuntimeException();
        }
    }
    private static int testSpecialCos() {
        int failures = 0;
        double [][] testCases = {
            {+0.0,    1.0},
            {-0.0,    1.0},
        };

        for(double[] testCase: testCases) {
            failures += testCosCase(testCase[0], testCase[1]);
        }

        return failures;
    }

    private static int testCosCase(double input, double expected) {
        int failures = 0;
        failures += Tests.test("Math.cos",       input, Math.cos(input),       expected);
        failures += Tests.test("StrictMath.cos", input, StrictMath.cos(input), expected);
        return failures;
    }

    private static int testSpecialAcos() {
        int failures = 0;
        double [][] testCases = {
            {1.0,    0.0},
        };

        for(double[] testCase: testCases) {
            failures += testAcosCase(testCase[0], testCase[1]);
        }

        return failures;
    }

    private static int testAcosCase(double input, double expected) {
        int failures = 0;
        failures += Tests.test("Math.acos",       input, Math.acos(input),       expected);
        failures += Tests.test("StrictMath.acos", input, StrictMath.acos(input), expected);
        return failures;
    }

    private static int testSpecialAtan() {
        int failures = 0;
        double [][] testCases = {
            {Double.POSITIVE_INFINITY,    +Math.PI/2.0},
            {Double.NEGATIVE_INFINITY,    -Math.PI/2.0},
        };

        for(double[] testCase: testCases) {
            failures += testAtanCase(testCase[0], testCase[1]);
        }

        return failures;
    }

    private static int testAtanCase(double input, double expected) {
        int failures = 0;
        failures += Tests.test("Math.atan",       input, Math.atan(input),       expected);
        failures += Tests.test("StrictMath.atan", input, StrictMath.atan(input), expected);
        return failures;
    }

    private static int testSpecialLog() {
        int failures = 0;
        double [][] testCases = {
            {1.0,    +0.0},
        };

        for(double[] testCase: testCases) {
            failures += testLogCase(testCase[0], testCase[1]);
        }

        return failures;
    }

    private static int testLogCase(double input, double expected) {
        int failures = 0;
        failures += Tests.test("Math.log",       input, Math.log(input),       expected);
        failures += Tests.test("StrictMath.log", input, StrictMath.log(input), expected);
        return failures;
    }
}
