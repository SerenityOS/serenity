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
 * @bug 4074599 4939441
 * @summary Tests for {Math, StrictMath}.log10
 * @author Joseph D. Darcy
 */

public class Log10Tests {
    private Log10Tests(){}

    static final double infinityD = Double.POSITIVE_INFINITY;
    static final double NaNd = Double.NaN;
    static final double LN_10 = StrictMath.log(10.0);

    // Initialize shared random number generator
    static java.util.Random rand = new java.util.Random(0L);

    static int testLog10Case(double input, double expected) {
        int failures=0;

        failures+=Tests.test("Math.log10(double)", input,
                             Math.log10(input), expected);

        failures+=Tests.test("StrictMath.log10(double)", input,
                             StrictMath.log10(input), expected);

        return failures;
    }

    static int testLog10() {
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
            {Double.NEGATIVE_INFINITY,  NaNd},
            {-8.0,                      NaNd},
            {-1.0,                      NaNd},
            {-Double.MIN_NORMAL,        NaNd},
            {-Double.MIN_VALUE,         NaNd},
            {-0.0,                      -infinityD},
            {+0.0,                      -infinityD},
            {+1.0,                      0.0},
            {Double.POSITIVE_INFINITY,  infinityD},
        };

        // Test special cases
        for(int i = 0; i < testCases.length; i++) {
            failures += testLog10Case(testCases[i][0],
                                          testCases[i][1]);
        }

        // Test log10(10^n) == n for integer n; 10^n, n < 0 is not
        // exactly representable as a floating-point value -- up to
        // 10^22 can be represented exactly
        double testCase = 1.0;
        for(int i = 0; i < 23; i++) {
            failures += testLog10Case(testCase, i);
            testCase *= 10.0;
        }

        // Test for gross inaccuracy by comparing to log; should be
        // within a few ulps of log(x)/log(10)
        for(int i = 0; i < 10000; i++) {
            double input = Double.longBitsToDouble(rand.nextLong());
            if(! Double.isFinite(input))
                continue; // avoid testing NaN and infinite values
            else {
                input = Math.abs(input);

                double expected = StrictMath.log(input)/LN_10;
                if( ! Double.isFinite(expected))
                    continue; // if log(input) overflowed, try again
                else {
                    double result;

                    if( Math.abs(((result=Math.log10(input)) - expected)/Math.ulp(expected)) > 3) {
                        failures++;
                        System.err.println("For input " + input +
                                           ", Math.log10 was more than 3 ulps different from " +
                                           "log(input)/log(10): log10(input) = " + result +
                                           "\tlog(input)/log(10) = " + expected);
                    }

                    if( Math.abs(((result=StrictMath.log10(input)) - expected)/Math.ulp(expected)) > 3) {
                        failures++;
                        System.err.println("For input " + input +
                                           ", StrictMath.log10 was more than 3 ulps different from " +
                                           "log(input)/log(10): log10(input) = " + result +
                                           "\tlog(input)/log(10) = " + expected);
                    }


                }
            }
        }

        // Test for accuracy and monotonicity near log10(1.0).  From
        // the Taylor expansion of log,
        // log10(1+z) ~= (z -(z^2)/2)/LN_10;
        {
            double neighbors[] =        new double[40];
            double neighborsStrict[] =  new double[40];
            double z = Double.NaN;

            // Test inputs greater than 1.0.
            neighbors[0] =              Math.log10(1.0);
            neighborsStrict[0] =        StrictMath.log10(1.0);

            double input[] =  new double[40];
            int half = input.length/2;


            // Initialize input to the 40 consecutive double values
            // "centered" at 1.0.
            double up = Double.NaN;
            double down = Double.NaN;
            for(int i = 0; i < half; i++) {
                if (i == 0) {
                    input[half] = 1.0;
                    up   = Math.nextUp(1.0);
                    down = Math.nextDown(1.0);
                } else {
                    input[half + i] = up;
                    input[half - i] = down;
                    up   = Math.nextUp(up);
                    down = Math.nextDown(down);
                }
            }
            input[0] = Math.nextDown(input[1]);

            for(int i = 0; i < neighbors.length; i++) {
                neighbors[i] =          Math.log10(input[i]);
                neighborsStrict[i] =    StrictMath.log10(input[i]);

                // Test accuracy.
                z = input[i] - 1.0;
                double expected = (z - (z*z)*0.5)/LN_10;
                if ( Math.abs(neighbors[i] - expected ) > 3*Math.ulp(expected) ) {
                    failures++;
                    System.err.println("For input near 1.0 " + input[i] +
                                       ", Math.log10(1+z) was more than 3 ulps different from " +
                                       "(z-(z^2)/2)/ln(10): log10(input) = " + neighbors[i] +
                                       "\texpected about = " + expected);
                }

                if ( Math.abs(neighborsStrict[i] - expected ) > 3*Math.ulp(expected) ) {
                    failures++;
                    System.err.println("For input near 1.0 " + input[i] +
                                       ", StrictMath.log10(1+z) was more than 3 ulps different from " +
                                       "(z-(z^2)/2)/ln(10): log10(input) = " + neighborsStrict[i] +
                                       "\texpected about = " + expected);
                }

                // Test monotonicity
                if( i > 0) {
                    if( neighbors[i-1] > neighbors[i] ) {
                        failures++;
                        System.err.println("Monotonicity failure for Math.log10  at " + input[i] +
                                           " and prior value.");
                    }

                    if( neighborsStrict[i-1] > neighborsStrict[i] ) {
                        failures++;
                        System.err.println("Monotonicity failure for StrictMath.log10  at " + input[i] +
                                           " and prior value.");
                    }
                }
            }

        }

        return failures;
    }

    public static void main(String argv[]) {
        int failures = 0;

        failures += testLog10();

        if (failures > 0) {
            System.err.println("Testing log10 incurred "
                               + failures + " failures.");
            throw new RuntimeException();
        }
    }

}
