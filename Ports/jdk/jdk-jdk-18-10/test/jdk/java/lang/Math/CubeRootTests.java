/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @run main CubeRootTests
 * @bug 4347132 4939441 8078672
 * @summary Tests for {Math, StrictMath}.cbrt (use -Dseed=X to set PRNG seed)
 * @author Joseph D. Darcy
 * @key randomness
 */

import jdk.test.lib.RandomFactory;

public class CubeRootTests {
    private CubeRootTests(){}

    static final double infinityD = Double.POSITIVE_INFINITY;
    static final double NaNd = Double.NaN;

    // Initialize shared random number generator
    static java.util.Random rand = RandomFactory.getRandom();

    static int testCubeRootCase(double input, double expected) {
        int failures=0;

        double minus_input = -input;
        double minus_expected = -expected;

        failures+=Tests.test("Math.cbrt(double)", input,
                             Math.cbrt(input), expected);
        failures+=Tests.test("Math.cbrt(double)", minus_input,
                             Math.cbrt(minus_input), minus_expected);
        failures+=Tests.test("StrictMath.cbrt(double)", input,
                             StrictMath.cbrt(input), expected);
        failures+=Tests.test("StrictMath.cbrt(double)", minus_input,
                             StrictMath.cbrt(minus_input), minus_expected);

        return failures;
    }

    static int testCubeRoot() {
        int failures = 0;
        double [][] testCases = {
            {NaNd,                      NaNd},
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
            {Double.POSITIVE_INFINITY,  Double.POSITIVE_INFINITY},
            {Double.NEGATIVE_INFINITY,  Double.NEGATIVE_INFINITY},
            {+0.0,                      +0.0},
            {-0.0,                      -0.0},
            {+1.0,                      +1.0},
            {-1.0,                      -1.0},
            {+8.0,                      +2.0},
            {-8.0,                      -2.0}
        };

        for(int i = 0; i < testCases.length; i++) {
            failures += testCubeRootCase(testCases[i][0],
                                         testCases[i][1]);
        }

        // Test integer perfect cubes less than 2^53.
        for(int i = 0; i <= 208063; i++) {
            double d = i;
            failures += testCubeRootCase(d*d*d, (double)i);
        }

        // Test cbrt(2^(3n)) = 2^n.
        for(int i = 18; i <= Double.MAX_EXPONENT/3; i++) {
            failures += testCubeRootCase(Math.scalb(1.0, 3*i),
                                         Math.scalb(1.0, i) );
        }

        // Test cbrt(2^(-3n)) = 2^-n.
        for(int i = -1; i >= DoubleConsts.MIN_SUB_EXPONENT/3; i--) {
            failures += testCubeRootCase(Math.scalb(1.0, 3*i),
                                         Math.scalb(1.0, i) );
        }

        // Test random perfect cubes.  Create double values with
        // modest exponents but only have at most the 17 most
        // significant bits in the significand set; 17*3 = 51, which
        // is less than the number of bits in a double's significand.
        long exponentBits1 =
            Double.doubleToLongBits(Math.scalb(1.0, 55)) &
            DoubleConsts.EXP_BIT_MASK;
        long exponentBits2=
            Double.doubleToLongBits(Math.scalb(1.0, -55)) &
            DoubleConsts.EXP_BIT_MASK;
        for(int i = 0; i < 100; i++) {
            // Take 16 bits since the 17th bit is implicit in the
            // exponent
           double input1 =
               Double.longBitsToDouble(exponentBits1 |
                                       // Significand bits
                                       ((long) (rand.nextInt() & 0xFFFF))<<
                                       (DoubleConsts.SIGNIFICAND_WIDTH-1-16));
           failures += testCubeRootCase(input1*input1*input1, input1);

           double input2 =
               Double.longBitsToDouble(exponentBits2 |
                                       // Significand bits
                                       ((long) (rand.nextInt() & 0xFFFF))<<
                                       (DoubleConsts.SIGNIFICAND_WIDTH-1-16));
           failures += testCubeRootCase(input2*input2*input2, input2);
        }

        // Directly test quality of implementation properties of cbrt
        // for values that aren't perfect cubes.  Verify returned
        // result meets the 1 ulp test.  That is, we want to verify
        // that for positive x > 1,
        // y = cbrt(x),
        //
        // if (err1=x - y^3 ) < 0, abs((y_pp^3 -x )) < err1
        // if (err1=x - y^3 ) > 0, abs((y_mm^3 -x )) < err1
        //
        // where y_mm and y_pp are the next smaller and next larger
        // floating-point value to y.  In other words, if y^3 is too
        // big, making y larger does not improve the result; likewise,
        // if y^3 is too small, making y smaller does not improve the
        // result.
        //
        // ...-----|--?--|--?--|-----... Where is the true result?
        //         y_mm  y     y_pp
        //
        // The returned value y should be one of the floating-point
        // values braketing the true result.  However, given y, a
        // priori we don't know if the true result falls in [y_mm, y]
        // or [y, y_pp].  The above test looks at the error in x-y^3
        // to determine which region the true result is in; e.g. if
        // y^3 is smaller than x, the true result should be in [y,
        // y_pp].  Therefore, it would be an error for y_mm to be a
        // closer approximation to x^(1/3).  In this case, it is
        // permissible, although not ideal, for y_pp^3 to be a closer
        // approximation to x^(1/3) than y^3.
        //
        // We will use pow(y,3) to compute y^3.  Although pow is not
        // correctly rounded, StrictMath.pow should have at most 1 ulp
        // error.  For y > 1, pow(y_mm,3) and pow(y_pp,3) will differ
        // from pow(y,3) by more than one ulp so the comparision of
        // errors should still be valid.

        for(int i = 0; i < 1000; i++) {
            double d = 1.0 + rand.nextDouble();
            double err, err_adjacent;

            double y1 = Math.cbrt(d);
            double y2 = StrictMath.cbrt(d);

            err = d - StrictMath.pow(y1, 3);
            if (err != 0.0) {
                if(Double.isNaN(err)) {
                    failures++;
                    System.err.println("Encountered unexpected NaN value: d = " + d +
                                       "\tcbrt(d) = " + y1);
                } else {
                    if (err < 0.0) {
                        err_adjacent = StrictMath.pow(Math.nextUp(y1), 3) - d;
                    }
                    else  { // (err > 0.0)
                        err_adjacent = StrictMath.pow(Math.nextAfter(y1,0.0), 3) - d;
                    }

                    if (Math.abs(err) > Math.abs(err_adjacent)) {
                        failures++;
                        System.err.println("For Math.cbrt(" + d + "), returned result " +
                                           y1 + "is not as good as adjacent value.");
                    }
                }
            }


            err = d - StrictMath.pow(y2, 3);
            if (err != 0.0) {
                if(Double.isNaN(err)) {
                    failures++;
                    System.err.println("Encountered unexpected NaN value: d = " + d +
                                       "\tcbrt(d) = " + y2);
                } else {
                    if (err < 0.0) {
                        err_adjacent = StrictMath.pow(Math.nextUp(y2), 3) - d;
                    }
                    else  { // (err > 0.0)
                        err_adjacent = StrictMath.pow(Math.nextAfter(y2,0.0), 3) - d;
                    }

                    if (Math.abs(err) > Math.abs(err_adjacent)) {
                        failures++;
                        System.err.println("For StrictMath.cbrt(" + d + "), returned result " +
                                           y2 + "is not as good as adjacent value.");
                    }
                }
            }


        }

        // Test monotonicity properites near perfect cubes; test two
        // numbers before and two numbers after; i.e. for
        //
        // pcNeighbors[] =
        // {nextDown(nextDown(pc)),
        // nextDown(pc),
        // pc,
        // nextUp(pc),
        // nextUp(nextUp(pc))}
        //
        // test that cbrt(pcNeighbors[i]) <= cbrt(pcNeighbors[i+1])
        {

            double pcNeighbors[] = new double[5];
            double pcNeighborsCbrt[] = new double[5];
            double pcNeighborsStrictCbrt[] = new double[5];

            // Test near cbrt(2^(3n)) = 2^n.
            for(int i = 18; i <= Double.MAX_EXPONENT/3; i++) {
                double pc = Math.scalb(1.0, 3*i);

                pcNeighbors[2] = pc;
                pcNeighbors[1] = Math.nextDown(pc);
                pcNeighbors[0] = Math.nextDown(pcNeighbors[1]);
                pcNeighbors[3] = Math.nextUp(pc);
                pcNeighbors[4] = Math.nextUp(pcNeighbors[3]);

                for(int j = 0; j < pcNeighbors.length; j++) {
                    pcNeighborsCbrt[j] =           Math.cbrt(pcNeighbors[j]);
                    pcNeighborsStrictCbrt[j] = StrictMath.cbrt(pcNeighbors[j]);
                }

                for(int j = 0; j < pcNeighborsCbrt.length-1; j++) {
                    if(pcNeighborsCbrt[j] >  pcNeighborsCbrt[j+1] ) {
                        failures++;
                        System.err.println("Monotonicity failure for Math.cbrt on " +
                                          pcNeighbors[j] + " and "  +
                                          pcNeighbors[j+1] + "\n\treturned " +
                                          pcNeighborsCbrt[j] + " and " +
                                          pcNeighborsCbrt[j+1] );
                    }

                    if(pcNeighborsStrictCbrt[j] >  pcNeighborsStrictCbrt[j+1] ) {
                        failures++;
                        System.err.println("Monotonicity failure for StrictMath.cbrt on " +
                                          pcNeighbors[j] + " and "  +
                                          pcNeighbors[j+1] + "\n\treturned " +
                                          pcNeighborsStrictCbrt[j] + " and " +
                                          pcNeighborsStrictCbrt[j+1] );
                    }


                }

            }

            // Test near cbrt(2^(-3n)) = 2^-n.
            for(int i = -1; i >= DoubleConsts.MIN_SUB_EXPONENT/3; i--) {
                double pc = Math.scalb(1.0, 3*i);

                pcNeighbors[2] = pc;
                pcNeighbors[1] = Math.nextDown(pc);
                pcNeighbors[0] = Math.nextDown(pcNeighbors[1]);
                pcNeighbors[3] = Math.nextUp(pc);
                pcNeighbors[4] = Math.nextUp(pcNeighbors[3]);

                for(int j = 0; j < pcNeighbors.length; j++) {
                    pcNeighborsCbrt[j] =           Math.cbrt(pcNeighbors[j]);
                    pcNeighborsStrictCbrt[j] = StrictMath.cbrt(pcNeighbors[j]);
                }

                for(int j = 0; j < pcNeighborsCbrt.length-1; j++) {
                    if(pcNeighborsCbrt[j] >  pcNeighborsCbrt[j+1] ) {
                        failures++;
                        System.err.println("Monotonicity failure for Math.cbrt on " +
                                          pcNeighbors[j] + " and "  +
                                          pcNeighbors[j+1] + "\n\treturned " +
                                          pcNeighborsCbrt[j] + " and " +
                                          pcNeighborsCbrt[j+1] );
                    }

                    if(pcNeighborsStrictCbrt[j] >  pcNeighborsStrictCbrt[j+1] ) {
                        failures++;
                        System.err.println("Monotonicity failure for StrictMath.cbrt on " +
                                          pcNeighbors[j] + " and "  +
                                          pcNeighbors[j+1] + "\n\treturned " +
                                          pcNeighborsStrictCbrt[j] + " and " +
                                          pcNeighborsStrictCbrt[j+1] );
                    }


                }
            }
        }

        return failures;
    }

    public static void main(String argv[]) {
        int failures = 0;

        failures += testCubeRoot();

        if (failures > 0) {
            System.err.println("Testing cbrt incurred "
                               + failures + " failures.");
            throw new RuntimeException();
        }
    }

}
