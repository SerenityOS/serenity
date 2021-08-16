/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4851642 8253409
 * @summary Tests for Math.fusedMac and StrictMath.fusedMac.
 * @build Tests
 * @build FusedMultiplyAddTests
 * @run main FusedMultiplyAddTests
 * @run main/othervm -XX:-UseFMA FusedMultiplyAddTests
 */

/**
 * The specifications for both Math.fusedMac and StrictMath.fusedMac
 * are the same and both are exactly specified. Therefore, both
 * methods are tested in this file.
 */

public class FusedMultiplyAddTests {
    private FusedMultiplyAddTests(){}

    private static final double Infinity =  Double.POSITIVE_INFINITY;
    private static final float  InfinityF = Float.POSITIVE_INFINITY;
    private static final double NaN  = Double.NaN;
    private static final float  NaNf = Float.NaN;

    public static void main(String... args) {
        int failures = 0;

        failures += testNonFiniteD();
        failures += testZeroesD();
        failures += testSimpleD();

        failures += testNonFiniteF();
        failures += testZeroesF();
        failures += testSimpleF();

        if (failures > 0) {
            System.err.println("Testing fma incurred "
                               + failures + " failures.");
            throw new RuntimeException();
        }
    }

    private static int testNonFiniteD() {
        int failures = 0;

        double [][] testCases = {
            {Infinity,       Infinity,  Infinity,
            Infinity,
            },

            {-Infinity,       Infinity,  -Infinity,
            -Infinity,
            },

            {-Infinity,       Infinity,  Infinity,
            NaN,
            },

            {Infinity,       Infinity,  -Infinity,
            NaN,
            },

            {1.0,       Infinity,  2.0,
            Infinity,
            },

            {1.0,       2.0,       Infinity,
             Infinity,
            },

            {Infinity,  1.0,       Infinity,
             Infinity,
            },

            {Double.MAX_VALUE, 2.0, -Infinity,
             -Infinity},

            {Infinity,  1.0,       -Infinity,
             NaN,
            },

            {-Infinity, 1.0,       Infinity,
             NaN,
            },

            {1.0,       NaN,       2.0,
             NaN,
            },

            {1.0,       2.0,       NaN,
             NaN,
            },

            {Infinity,  2.0,       NaN,
             NaN,
            },

            {NaN,       2.0,       Infinity,
             NaN,
            },
        };

        for (double[] testCase: testCases)
            failures += testFusedMacCase(testCase[0], testCase[1], testCase[2], testCase[3]);

        return failures;
    }

    private static int testZeroesD() {
        int failures = 0;

        double [][] testCases = {
            {+0.0, +0.0, +0.0,
             +0.0,
            },

            {-0.0, +0.0, +0.0,
             +0.0,
            },

            {+0.0, +0.0, -0.0,
             +0.0,
            },

            {+0.0, +0.0, -0.0,
             +0.0,
            },

            {-0.0, +0.0, -0.0,
             -0.0,
            },

            {-0.0, -0.0, -0.0,
             +0.0,
            },

            {-1.0, +0.0, -0.0,
             -0.0,
            },

            {-1.0, +0.0, +0.0,
             +0.0,
            },

            {-2.0, +0.0, -0.0,
             -0.0,
            },

            {-2.0, +0.0, +0.0,
             +0.0,
            },
        };

        for (double[] testCase: testCases)
            failures += testFusedMacCase(testCase[0], testCase[1], testCase[2], testCase[3]);

        return failures;
    }

    private static int testSimpleD() {
        int failures = 0;

        double [][] testCases = {
            {1.0, 2.0, 3.0,
             5.0,},

            {1.0, 2.0, -2.0,
             0.0,},

            {5.0, 5.0, -25.0,
             0.0,},

            {Double.MAX_VALUE, 2.0, -Double.MAX_VALUE,
             Double.MAX_VALUE},

            {Double.MAX_VALUE, 2.0, 1.0,
             Infinity},

            {Double.MIN_VALUE, -Double.MIN_VALUE, +0.0,
             -0.0},

            {Double.MIN_VALUE, -Double.MIN_VALUE, -0.0,
             -0.0},

            {Double.MIN_VALUE, Double.MIN_VALUE, +0.0,
             +0.0},

            {Double.MIN_VALUE, Double.MIN_VALUE, -0.0,
             +0.0},

            {Double.MIN_VALUE, +0.0, -0.0,
             +0.0},

            {Double.MIN_VALUE, -0.0, -0.0,
             -0.0},

            {Double.MIN_VALUE, +0.0, +0.0,
             +0.0},

            {Double.MIN_VALUE, -0.0, +0.0,
             +0.0},

            {1.0+Math.ulp(1.0), 1.0+Math.ulp(1.0), -1.0-2.0*Math.ulp(1.0),
             Math.ulp(1.0)*Math.ulp(1.0)},
        };

        for (double[] testCase: testCases)
            failures += testFusedMacCase(testCase[0], testCase[1], testCase[2], testCase[3]);

        return failures;
    }

    private static int testNonFiniteF() {
        int failures = 0;

        float [][] testCases = {
            {1.0f,       InfinityF,  2.0f,
             InfinityF,
            },

            {1.0f,       2.0f,       InfinityF,
             InfinityF,
            },

            {InfinityF,  1.0f,       InfinityF,
             InfinityF,
            },

            {Float.MAX_VALUE, 2.0f, -InfinityF,
             -InfinityF},

            {InfinityF,  1.0f,      -InfinityF,
             NaNf,
            },

            {-InfinityF, 1.0f,       InfinityF,
             NaNf,
            },

            {1.0f,       NaNf,       2.0f,
             NaNf,
            },

            {1.0f,       2.0f,       NaNf,
             NaNf,
            },

            {InfinityF,  2.0f,       NaNf,
             NaNf,
            },

            {NaNf,       2.0f,       InfinityF,
             NaNf,
            },
        };

        for (float[] testCase: testCases)
            failures += testFusedMacCase(testCase[0], testCase[1], testCase[2], testCase[3]);

        return failures;
    }

    private static int testZeroesF() {
        int failures = 0;

        float [][] testCases = {
            {+0.0f, +0.0f, +0.0f,
             +0.0f,
            },

            {-0.0f, +0.0f, +0.0f,
             +0.0f,
            },

            {+0.0f, +0.0f, -0.0f,
             +0.0f,
            },

            {+0.0f, +0.0f, -0.0f,
             +0.0f,
            },

            {-0.0f, +0.0f, -0.0f,
             -0.0f,
            },

            {-0.0f, -0.0f, -0.0f,
             +0.0f,
            },

            {-1.0f, +0.0f, -0.0f,
             -0.0f,
            },

            {-1.0f, +0.0f, +0.0f,
             +0.0f,
            },

            {-2.0f, +0.0f, -0.0f,
             -0.0f,
            },
        };

        for (float[] testCase: testCases)
            failures += testFusedMacCase(testCase[0], testCase[1], testCase[2], testCase[3]);

        return failures;
    }

    private static int testSimpleF() {
        int failures = 0;

        float [][] testCases = {
            {1.0f, 2.0f, 3.0f,
             5.0f,},

            {1.0f, 2.0f, -2.0f,
             0.0f,},

            {5.0f, 5.0f, -25.0f,
             0.0f,},

            {Float.MAX_VALUE, 2.0f, -Float.MAX_VALUE,
             Float.MAX_VALUE},

            {Float.MAX_VALUE, 2.0f, 1.0f,
             InfinityF},

            {1.0f+Math.ulp(1.0f), 1.0f+Math.ulp(1.0f), -1.0f-2.0f*Math.ulp(1.0f),
             Math.ulp(1.0f)*Math.ulp(1.0f)},

            // Double-rounding if done in double precision
            {0x1.fffffep23f, 0x1.000004p28f, 0x1.fep5f, 0x1.000002p52f}
        };

        for (float[] testCase: testCases)
            failures += testFusedMacCase(testCase[0], testCase[1], testCase[2], testCase[3]);

        return failures;
    }


    private static int testFusedMacCase(double input1, double input2, double input3, double expected) {
        int failures = 0;
        failures += Tests.test("Math.fma(double)", input1, input2, input3,
                               Math.fma(input1, input2, input3), expected);
        failures += Tests.test("StrictMath.fma(double)", input1, input2, input3,
                               StrictMath.fma(input1, input2, input3), expected);

        // Permute first two inputs
        failures += Tests.test("Math.fma(double)", input2, input1, input3,
                               Math.fma(input2, input1, input3), expected);
        failures += Tests.test("StrictMath.fma(double)", input2, input1, input3,
                               StrictMath.fma(input2, input1, input3), expected);
        return failures;
    }

    private static int testFusedMacCase(float input1, float input2, float input3, float expected) {
        int failures = 0;
        failures += Tests.test("Math.fma(float)", input1, input2, input3,
                               Math.fma(input1, input2, input3), expected);
        failures += Tests.test("StrictMath.fma(float)", input1, input2, input3,
                               StrictMath.fma(input1, input2, input3), expected);

        // Permute first two inputs
        failures += Tests.test("Math.fma(float)", input2, input1, input3,
                               Math.fma(input2, input1, input3), expected);
        failures += Tests.test("StrictMath.fma(float)", input2, input1, input3,
                               StrictMath.fma(input2, input1, input3), expected);
        return failures;
    }
}
