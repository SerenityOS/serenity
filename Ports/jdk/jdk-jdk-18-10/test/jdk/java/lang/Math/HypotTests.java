/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run main HypotTests
 * @bug 4851638 4939441 8078672 8240632
 * @summary Tests for {Math, StrictMath}.hypot (use -Dseed=X to set PRNG seed)
 * @author Joseph D. Darcy
 * @key randomness
 */

import jdk.test.lib.RandomFactory;

public class HypotTests {
    private HypotTests(){}

    static final double infinityD = Double.POSITIVE_INFINITY;
    static final double NaNd      = Double.NaN;

    /**
     * Given integers m and n, assuming m < n, the triple (n^2 - m^2,
     * 2mn, and n^2 + m^2) is a Pythagorean triple with a^2 + b^2 =
     * c^2.  This methods returns a long array holding the Pythagorean
     * triple corresponding to the inputs.
     */
    static long [] pythagoreanTriple(int m, int n) {
        long M = m;
        long N = n;
        long result[] = new long[3];


        result[0] = Math.abs(M*M - N*N);
        result[1] = Math.abs(2*M*N);
        result[2] = Math.abs(M*M + N*N);

        return result;
    }

    static int testHypot() {
        int failures = 0;

        double [][] testCases = {
            // Special cases
            {infinityD,         infinityD,              infinityD},
            {infinityD,         0.0,                    infinityD},
            {infinityD,         1.0,                    infinityD},
            {infinityD,         NaNd,                   infinityD},
            {NaNd,              NaNd,                   NaNd},
            {0.0,               NaNd,                   NaNd},
            {1.0,               NaNd,                   NaNd},
            {Double.longBitsToDouble(0x7FF0000000000001L),      1.0,    NaNd},
            {Double.longBitsToDouble(0xFFF0000000000001L),      1.0,    NaNd},
            {Double.longBitsToDouble(0x7FF8555555555555L),      1.0,    NaNd},
            {Double.longBitsToDouble(0xFFF8555555555555L),      1.0,    NaNd},
            {Double.longBitsToDouble(0x7FFFFFFFFFFFFFFFL),      1.0,    NaNd},
            {Double.longBitsToDouble(0xFFFFFFFFFFFFFFFFL),      1.0,    NaNd},
            {Double.longBitsToDouble(0x7FFDeadBeef00000L),      1.0,    NaNd},
            {Double.longBitsToDouble(0xFFFDeadBeef00000L),      1.0,    NaNd},
            {Double.longBitsToDouble(0x7FFCafeBabe00000L),      1.0,    NaNd},
            {Double.longBitsToDouble(0xFFFCafeBabe00000L),      1.0,    NaNd},
        };

        for(int i = 0; i < testCases.length; i++) {
            failures += testHypotCase(testCases[i][0], testCases[i][1],
                                      testCases[i][2]);
        }

        // Verify hypot(x, 0.0) is close to x over the entire exponent
        // range.
        for(int i = DoubleConsts.MIN_SUB_EXPONENT;
            i <= Double.MAX_EXPONENT;
            i++) {
            double input = Math.scalb(2, i);
            failures += testHypotCase(input, 0.0, input);
        }


        // Test Pythagorean triples

        // Small ones
        for(int m = 1; m < 10; m++) {
            for(int n = m+1; n < 11; n++) {
                long [] result = pythagoreanTriple(m, n);
                failures += testHypotCase(result[0], result[1], result[2]);
            }
        }

        // Big ones
        for(int m = 100000; m < 100100; m++) {
            for(int n = m+100000; n < 200200; n++) {
                long [] result = pythagoreanTriple(m, n);
                failures += testHypotCase(result[0], result[1], result[2]);
            }
        }

        // Approaching overflow tests

        /*
         * Create a random value r with an large-ish exponent.  The
         * result of hypot(3*r, 4*r) should be approximately 5*r. (The
         * computation of 4*r is exact since it just changes the
         * exponent).  While the exponent of r is less than or equal
         * to (MAX_EXPONENT - 3), the computation should not overflow.
         */
        java.util.Random rand = RandomFactory.getRandom();
        for(int i = 0; i < 1000; i++) {
            double d = rand.nextDouble();
            // Scale d to have an exponent equal to MAX_EXPONENT -15
            d = Math.scalb(d, Double.MAX_EXPONENT
                                 -15 - Tests.ilogb(d));
            for(int j = 0; j <= 13; j += 1) {
                failures += testHypotCase(3*d, 4*d, 5*d, 2.5);
                d *= 2.0; // increase exponent by 1
            }
        }

        // Test for monotonicity failures.  Fix one argument and test
        // two numbers before and two numbers after each chosen value;
        // i.e.
        //
        // pcNeighbors[] =
        // {nextDown(nextDown(pc)),
        // nextDown(pc),
        // pc,
        // nextUp(pc),
        // nextUp(nextUp(pc))}
        //
        // and we test that hypot(pcNeighbors[i]) <= hypot(pcNeighbors[i+1])
        {
            double pcNeighbors[] = new double[5];
            double pcNeighborsHypot[] = new double[5];
            double pcNeighborsStrictHypot[] = new double[5];


            for(int i = -18; i <= 18; i++) {
                double pc = Math.scalb(1.0, i);

                pcNeighbors[2] = pc;
                pcNeighbors[1] = Math.nextDown(pc);
                pcNeighbors[0] = Math.nextDown(pcNeighbors[1]);
                pcNeighbors[3] = Math.nextUp(pc);
                pcNeighbors[4] = Math.nextUp(pcNeighbors[3]);

                for(int j = 0; j < pcNeighbors.length; j++) {
                    pcNeighborsHypot[j]       =       Math.hypot(2.0, pcNeighbors[j]);
                    pcNeighborsStrictHypot[j] = StrictMath.hypot(2.0, pcNeighbors[j]);
                }

                for(int j = 0; j < pcNeighborsHypot.length-1; j++) {
                    if(pcNeighborsHypot[j] >  pcNeighborsHypot[j+1] ) {
                        failures++;
                        System.err.println("Monotonicity failure for Math.hypot on " +
                                          pcNeighbors[j] + " and "  +
                                          pcNeighbors[j+1] + "\n\treturned " +
                                          pcNeighborsHypot[j] + " and " +
                                          pcNeighborsHypot[j+1] );
                    }

                    if(pcNeighborsStrictHypot[j] >  pcNeighborsStrictHypot[j+1] ) {
                        failures++;
                        System.err.println("Monotonicity failure for StrictMath.hypot on " +
                                          pcNeighbors[j] + " and "  +
                                          pcNeighbors[j+1] + "\n\treturned " +
                                          pcNeighborsStrictHypot[j] + " and " +
                                          pcNeighborsStrictHypot[j+1] );
                    }


                }

            }
        }


        return failures;
    }

    /**
     * Verify +0.0 is returned if both arguments are zero.
     */
    private static int testHypotZeros() {
        return testHypotCase(0.0, 0.0, +0.0, 0.0);
    }

    static int testHypotCase(double input1, double input2, double expected) {
        return testHypotCase(input1,input2, expected, 1);
    }

    static int testHypotCase(double input1, double input2, double expected,
                             double ulps) {
        int failures = 0;
        if (expected < 0.0) {
            throw new AssertionError("Result of hypot must be greater than " +
                                     "or equal to zero");
        }

        // Test Math and StrictMath methods with no inputs negated,
        // each input negated singly, and both inputs negated.  Also
        // test inputs in reversed order.

        for(int i = -1; i <= 1; i+=2) {
            for(int j = -1; j <= 1; j+=2) {
                double x = i * input1;
                double y = j * input2;
                failures += Tests.testUlpDiff("Math.hypot", x, y,
                                              Math.hypot(x, y), expected, ulps);
                failures += Tests.testUlpDiff("Math.hypot", y, x,
                                              Math.hypot(y, x ), expected, ulps);

                failures += Tests.testUlpDiff("StrictMath.hypot", x, y,
                                              StrictMath.hypot(x, y), expected, ulps);
                failures += Tests.testUlpDiff("StrictMath.hypot", y, x,
                                              StrictMath.hypot(y, x), expected, ulps);
            }
        }

        return failures;
    }

    public static void main(String argv[]) {
        int failures = 0;

        failures += testHypot();
        failures += testHypotZeros();

        if (failures > 0) {
            System.err.println("Testing the hypot incurred "
                               + failures + " failures.");
            throw new RuntimeException();
        }
    }

}
