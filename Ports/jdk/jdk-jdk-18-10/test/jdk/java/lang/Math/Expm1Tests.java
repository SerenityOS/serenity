/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4851638 4900189 4939441
 * @summary Tests for {Math, StrictMath}.expm1
 * @author Joseph D. Darcy
 */

/*
 * The Taylor expansion of expxm1(x) = exp(x) -1 is
 *
 * 1 + x/1! + x^2/2! + x^3/3| + ... -1 =
 *
 * x + x^2/2! + x^3/3 + ...
 *
 * Therefore, for small values of x, expxm1 ~= x.
 *
 * For large values of x, expxm1(x) ~= exp(x)
 *
 * For large negative x, expxm1(x) ~= -1.
 */

public class Expm1Tests {

    private Expm1Tests(){}

    static final double infinityD = Double.POSITIVE_INFINITY;
    static final double NaNd = Double.NaN;

    static int testExpm1() {
        int failures = 0;

        double [][] testCases = {
            {Double.NaN,                NaNd},
            {Double.longBitsToDouble(0x7FF0000000000001L),      NaNd},
            {Double.longBitsToDouble(0xFFF0000000000001L),      NaNd},
            {Double.longBitsToDouble(0x7FF8555555555555L),      NaNd},
            {Double.longBitsToDouble(0xFFF8555555555555L),      NaNd},
            {Double.longBitsToDouble(0x7FFFFFFFFFFFFFFFL),      NaNd},
            {Double.longBitsToDouble(0xFFFFFFFFFFFFFFFFL),      NaNd},
            {Double.longBitsToDouble(0x7FFDeadBeef00000L),      NaNd},
            {Double.longBitsToDouble(0xFFFDeadBeef00000L),      NaNd},
            {Double.longBitsToDouble(0x7FFCafeBabe00000L),      NaNd},
            {Double.longBitsToDouble(0xFFFCafeBabe00000L),      NaNd},
            {infinityD,                 infinityD},
            {-infinityD,                -1.0},
            {-0.0,                      -0.0},
            {+0.0,                      +0.0},
        };

        // Test special cases
        for(int i = 0; i < testCases.length; i++) {
            failures += testExpm1CaseWithUlpDiff(testCases[i][0],
                                                 testCases[i][1], 0, null);
        }


        // For |x| < 2^-54 expm1(x) ~= x
        for(int i = DoubleConsts.MIN_SUB_EXPONENT; i <= -54; i++) {
            double d = Math.scalb(2, i);
            failures += testExpm1Case(d, d);
            failures += testExpm1Case(-d, -d);
        }


        // For values of y where exp(y) > 2^54, expm1(x) ~= exp(x).
        // The least such y is ln(2^54) ~= 37.42994775023705; exp(x)
        // overflows for x > ~= 709.8

        // Use a 2-ulp error threshold to account for errors in the
        // exp implementation; the increments of d in the loop will be
        // exact.
        for(double d = 37.5; d <= 709.5; d += 1.0) {
            failures += testExpm1CaseWithUlpDiff(d, StrictMath.exp(d), 2, null);
        }

        // For x > 710, expm1(x) should be infinity
        for(int i = 10; i <= Double.MAX_EXPONENT; i++) {
            double d = Math.scalb(2, i);
            failures += testExpm1Case(d, infinityD);
        }

        // By monotonicity, once the limit is reached, the
        // implemenation should return the limit for all smaller
        // values.
        boolean reachedLimit [] = {false, false};

        // Once exp(y) < 0.5 * ulp(1), expm1(y) ~= -1.0;
        // The greatest such y is ln(2^-53) ~= -36.7368005696771.
        for(double d = -36.75; d >= -127.75; d -= 1.0) {
            failures += testExpm1CaseWithUlpDiff(d, -1.0, 1,
                                                 reachedLimit);
        }

        for(int i = 7; i <= Double.MAX_EXPONENT; i++) {
            double d = -Math.scalb(2, i);
            failures += testExpm1CaseWithUlpDiff(d, -1.0, 1, reachedLimit);
        }

        // Test for monotonicity failures near multiples of log(2).
        // Test two numbers before and two numbers after each chosen
        // value; i.e.
        //
        // pcNeighbors[] =
        // {nextDown(nextDown(pc)),
        // nextDown(pc),
        // pc,
        // nextUp(pc),
        // nextUp(nextUp(pc))}
        //
        // and we test that expm1(pcNeighbors[i]) <= expm1(pcNeighbors[i+1])
        {
            double pcNeighbors[] = new double[5];
            double pcNeighborsExpm1[] = new double[5];
            double pcNeighborsStrictExpm1[] = new double[5];

            for(int i = -50; i <= 50; i++) {
                double pc = StrictMath.log(2)*i;

                pcNeighbors[2] = pc;
                pcNeighbors[1] = Math.nextDown(pc);
                pcNeighbors[0] = Math.nextDown(pcNeighbors[1]);
                pcNeighbors[3] = Math.nextUp(pc);
                pcNeighbors[4] = Math.nextUp(pcNeighbors[3]);

                for(int j = 0; j < pcNeighbors.length; j++) {
                    pcNeighborsExpm1[j]       =       Math.expm1(pcNeighbors[j]);
                    pcNeighborsStrictExpm1[j] = StrictMath.expm1(pcNeighbors[j]);
                }

                for(int j = 0; j < pcNeighborsExpm1.length-1; j++) {
                    if(pcNeighborsExpm1[j] >  pcNeighborsExpm1[j+1] ) {
                        failures++;
                        System.err.println("Monotonicity failure for Math.expm1 on " +
                                          pcNeighbors[j] + " and "  +
                                          pcNeighbors[j+1] + "\n\treturned " +
                                          pcNeighborsExpm1[j] + " and " +
                                          pcNeighborsExpm1[j+1] );
                    }

                    if(pcNeighborsStrictExpm1[j] >  pcNeighborsStrictExpm1[j+1] ) {
                        failures++;
                        System.err.println("Monotonicity failure for StrictMath.expm1 on " +
                                          pcNeighbors[j] + " and "  +
                                          pcNeighbors[j+1] + "\n\treturned " +
                                          pcNeighborsStrictExpm1[j] + " and " +
                                          pcNeighborsStrictExpm1[j+1] );
                    }


                }

            }
        }

        return failures;
    }

    public static int testExpm1Case(double input,
                                    double expected) {
        return testExpm1CaseWithUlpDiff(input, expected, 1, null);
    }

    public static int testExpm1CaseWithUlpDiff(double input,
                                               double expected,
                                               double ulps,
                                               boolean [] reachedLimit) {
        int failures = 0;
        double mathUlps = ulps, strictUlps = ulps;
        double mathOutput;
        double strictOutput;

        if (reachedLimit != null) {
            if (reachedLimit[0])
                mathUlps = 0;

            if (reachedLimit[1])
                strictUlps = 0;
        }

        failures += Tests.testUlpDiffWithLowerBound("Math.expm1(double)",
                                                    input, mathOutput=Math.expm1(input),
                                                    expected, mathUlps, -1.0);
        failures += Tests.testUlpDiffWithLowerBound("StrictMath.expm1(double)",
                                                    input, strictOutput=StrictMath.expm1(input),
                                                    expected, strictUlps, -1.0);
        if (reachedLimit != null) {
            reachedLimit[0] |= (mathOutput   == -1.0);
            reachedLimit[1] |= (strictOutput == -1.0);
        }

        return failures;
    }

    public static void main(String argv[]) {
        int failures = 0;

        failures += testExpm1();

        if (failures > 0) {
            System.err.println("Testing expm1 incurred "
                               + failures + " failures.");
            throw new RuntimeException();
        }
    }
}
