/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import sun.java2d.marlin.FloatMath;

/*
 * @test
 * @summary Check for correct implementation of FloatMath.ceil/floor
 * @run main CeilAndFloorTests
 * @modules java.desktop/sun.java2d.marlin
 */
public class CeilAndFloorTests {

    public static String toHexString(float f) {
        if (!Float.isNaN(f))
            return Float.toHexString(f);
        else
            return "NaN(0x" + Integer.toHexString(Float.floatToRawIntBits(f)) + ")";
    }

    public static int test(String testName, float input,
                           float result, float expected) {
        if (Float.compare(expected, result) != 0) {
            System.err.println("Failure for " + testName + ":\n" +
                               "\tFor input " + input    + "\t(" + toHexString(input) + ")\n" +
                               "\texpected  " + expected + "\t(" + toHexString(expected) + ")\n" +
                               "\tgot       " + result   + "\t(" + toHexString(result) + ").");
            return 1;
        }
        else
            return 0;
    }

    public static int test_skip_0(String testName, float input,
                           float result, float expected)
    {
        // floor_int does not distinguish +0f and -0f
        // but it is not critical for Marlin
        if (Float.compare(expected, result) != 0 && (expected != 0f))
        {
            System.err.println("Failure for " + testName + ":\n" +
                               "\tFor input " + input    + "\t(" + toHexString(input) + ")\n" +
                               "\texpected  " + expected + "\t(" + toHexString(expected) + ")\n" +
                               "\tgot       " + result   + "\t(" + toHexString(result) + ").");
            return 1;
        }
        else
            return 0;
    }

    private static int testCeilCase(float input, float expected) {
        int failures = 0;
        // float result:
        failures += test("FloatMath.ceil_f", input, FloatMath.ceil_f(input), expected);
        // int result:
        failures += test("FloatMath.ceil_int", input, FloatMath.ceil_int(input), (int)expected);
        failures += test("FloatMath.ceil_f (int)", input, (int)FloatMath.ceil_f(input), (int)expected);
        return failures;
    }

    private static int testFloorCase(float input, float expected) {
        int failures = 0;
        // float result:
        failures += test       ("FloatMath.floor_f", input, FloatMath.floor_f(input), expected);
        // ignore difference between +0f and -0f:
        failures += test_skip_0("FloatMath.floor_int", input, FloatMath.floor_int(input), (int)expected);
        failures += test_skip_0("FloatMath.floor_f (int)", input, (int)FloatMath.floor_f(input), (int)expected);
        return failures;
    }

    private static int nearIntegerTests() {
        int failures = 0;

        float [] fixedPoints = {
            -0.0f,
             0.0f,
            -1.0f,
             1.0f,
            -0x1.0p52f,
             0x1.0p52f,
            -Float.MAX_VALUE,
             Float.MAX_VALUE,
             Float.NEGATIVE_INFINITY,
             Float.POSITIVE_INFINITY,
             Float.NaN,
        };

        for(float fixedPoint : fixedPoints) {
            failures += testCeilCase(fixedPoint, fixedPoint);
            failures += testFloorCase(fixedPoint, fixedPoint);
        }

        for(int i = Float.MIN_EXPONENT; i <= Float.MAX_EXPONENT; i++) {
            float powerOfTwo   = Math.scalb(1.0f, i);
            float neighborDown = Math.nextDown(powerOfTwo);
            float neighborUp   = Math.nextUp(powerOfTwo);

            if (i < 0) {
                failures += testCeilCase( powerOfTwo,  1.0f);
                failures += testCeilCase(-powerOfTwo, -0.0f);

                failures += testFloorCase( powerOfTwo,  0.0f);
                failures += testFloorCase(-powerOfTwo, -1.0f);

                failures += testCeilCase( neighborDown, 1.0f);
                failures += testCeilCase(-neighborDown, -0.0f);

                failures += testFloorCase( neighborUp,  0.0f);
                failures += testFloorCase(-neighborUp, -1.0f);
            } else {
                failures += testCeilCase(powerOfTwo, powerOfTwo);
                failures += testFloorCase(powerOfTwo, powerOfTwo);

                if (neighborDown==Math.rint(neighborDown)) {
                    failures += testCeilCase( neighborDown,  neighborDown);
                    failures += testCeilCase(-neighborDown, -neighborDown);

                    failures += testFloorCase( neighborDown, neighborDown);
                    failures += testFloorCase(-neighborDown,-neighborDown);
                } else {
                    failures += testCeilCase( neighborDown, powerOfTwo);
                    failures += testFloorCase(-neighborDown, -powerOfTwo);
                }

                if (neighborUp==Math.rint(neighborUp)) {
                    failures += testCeilCase(neighborUp, neighborUp);
                    failures += testCeilCase(-neighborUp, -neighborUp);

                    failures += testFloorCase(neighborUp, neighborUp);
                    failures += testFloorCase(-neighborUp, -neighborUp);
                } else {
                    failures += testFloorCase(neighborUp, powerOfTwo);
                    failures += testCeilCase(-neighborUp, -powerOfTwo);
                }
            }
        }

        for(int i = -(0x10000); i <= 0x10000; i++) {
            float f = (float) i;
            float neighborDown = Math.nextDown(f);
            float neighborUp   = Math.nextUp(f);

            failures += testCeilCase( f, f);
            failures += testCeilCase(-f, -f);

            failures += testFloorCase( f, f);
            failures += testFloorCase(-f, -f);

            if (Math.abs(f) > 1.0) {
                failures += testCeilCase( neighborDown, f);
                failures += testCeilCase(-neighborDown, -f+1);

                failures += testFloorCase( neighborUp, f);
                failures += testFloorCase(-neighborUp, -f-1);
            }
        }

        return failures;
    }

    public static int roundingTests() {
        int failures = 0;
        float [][] testCases = {
            { Float.MIN_VALUE,                           1.0f},
            {-Float.MIN_VALUE,                          -0.0f},
            { Math.nextDown(Float.MIN_NORMAL),           1.0f},
            {-Math.nextDown(Float.MIN_NORMAL),          -0.0f},
            { Float.MIN_NORMAL,                          1.0f},
            {-Float.MIN_NORMAL,                         -0.0f},

            { 0.1f,                                        1.0f},
            {-0.1f,                                       -0.0f},

            { 0.5f,                                        1.0f},
            {-0.5f,                                       -0.0f},

            { 1.5f,                                        2.0f},
            {-1.5f,                                       -1.0f},

            { 2.5f,                                        3.0f},
            {-2.5f,                                       -2.0f},

            { 12.3456789f,                                13.0f},
            {-12.3456789f,                               -12.0f},

            { Math.nextDown(1.0f),                         1.0f},
            { Math.nextDown(-1.0f),                       -1.0f},

            { Math.nextUp(1.0f),                           2.0f},
            { Math.nextUp(-1.0f),                         -0.0f},

            { 0x1.0p22f,                                 0x1.0p22f},
            {-0x1.0p22f,                                -0x1.0p22f},

            { Math.nextDown(0x1.0p22f),                  0x1.0p22f},
            {-Math.nextUp(0x1.0p22f),                   -0x1.0p22f},

            { Math.nextUp(0x1.0p22f),                    0x1.0p22f+1f},
            {-Math.nextDown(0x1.0p22f),                 -0x1.0p22f+1f},

            { Math.nextDown(0x1.0p23f),                  0x1.0p23f},
            {-Math.nextUp(0x1.0p23f),                   -0x1.0p23f-1f},

            { Math.nextUp(0x1.0p23f),                    0x1.0p23f+1f},
            {-Math.nextDown(0x1.0p23f),                 -0x1.0p23f+1f},
        };

        for(float[] testCase : testCases) {
            failures += testCeilCase(testCase[0], testCase[1]);
            failures += testFloorCase(-testCase[0], -testCase[1]);
        }
        return failures;
    }

    public static void main(String... args) {
        int failures = 0;

        System.out.println("nearIntegerTests");
        failures += nearIntegerTests();

        System.out.println("roundingTests");
        failures += roundingTests();

        if (failures > 0) {
            System.err.println("Testing {FloatMath}.ceil/floor incurred "
                               + failures + " failures.");
            throw new RuntimeException();
        }
    }
}
