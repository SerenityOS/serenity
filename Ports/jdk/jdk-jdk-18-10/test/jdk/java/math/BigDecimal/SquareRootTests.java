/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4851777 8233452
 * @summary Tests of BigDecimal.sqrt().
 */

import java.math.*;
import java.util.*;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static java.math.BigDecimal.ZERO;
import static java.math.BigDecimal.valueOf;

public class SquareRootTests {
    private static BigDecimal TWO = new BigDecimal(2);

    /**
     * The value 0.1, with a scale of 1.
     */
    private static final BigDecimal ONE_TENTH = valueOf(1L, 1);

    public static void main(String... args) {
        int failures = 0;

        failures += negativeTests();
        failures += zeroTests();
        failures += oneDigitTests();
        failures += twoDigitTests();
        failures += evenPowersOfTenTests();
        failures += squareRootTwoTests();
        failures += lowPrecisionPerfectSquares();
        failures += almostFourRoundingDown();
        failures += almostFourRoundingUp();
        failures += nearTen();
        failures += nearOne();
        failures += halfWay();

        if (failures > 0 ) {
            throw new RuntimeException("Incurred " + failures + " failures" +
                                       " testing BigDecimal.sqrt().");
        }
    }

    private static int negativeTests() {
        int failures = 0;

        for (long i = -10; i < 0; i++) {
            for (int j = -5; j < 5; j++) {
                try {
                    BigDecimal input = BigDecimal.valueOf(i, j);
                    BigDecimal result = input.sqrt(MathContext.DECIMAL64);
                    System.err.println("Unexpected sqrt of negative: (" +
                                       input + ").sqrt()  = " + result );
                    failures += 1;
                } catch (ArithmeticException e) {
                    ; // Expected
                }
            }
        }

        return failures;
    }

    private static int zeroTests() {
        int failures = 0;

        for (int i = -100; i < 100; i++) {
            BigDecimal expected = BigDecimal.valueOf(0L, i/2);
            // These results are independent of rounding mode
            failures += compare(BigDecimal.valueOf(0L, i).sqrt(MathContext.UNLIMITED),
                                expected, true, "zeros");

            failures += compare(BigDecimal.valueOf(0L, i).sqrt(MathContext.DECIMAL64),
                                expected, true, "zeros");
        }

        return failures;
    }

    /**
     * Probe inputs with one digit of precision, 1 ... 9 and those
     * values scaled by 10^-1, 0.1, ... 0.9.
     */
    private static int oneDigitTests() {
        int failures = 0;

        List<BigDecimal> oneToNine =
            List.of(ONE,        TWO,        valueOf(3),
                    valueOf(4), valueOf(5), valueOf(6),
                    valueOf(7), valueOf(8), valueOf(9));

        List<RoundingMode> modes =
            List.of(RoundingMode.UP,      RoundingMode.DOWN,
                    RoundingMode.CEILING, RoundingMode.FLOOR,
                    RoundingMode.HALF_UP, RoundingMode.HALF_DOWN, RoundingMode.HALF_EVEN);

        for (int i = 1; i < 20; i++) {
            for (RoundingMode rm : modes) {
                for (BigDecimal bd  : oneToNine) {
                    MathContext mc = new MathContext(i, rm);

                    failures += compareSqrtImplementations(bd, mc);
                    bd = bd.multiply(ONE_TENTH);
                    failures += compareSqrtImplementations(bd, mc);
                }
            }
        }

        return failures;
    }

    /**
     * Probe inputs with two digits of precision, (10 ... 99) and
     * those values scaled by 10^-1 (1, ... 9.9) and scaled by 10^-2
     * (0.1 ... 0.99).
     */
    private static int twoDigitTests() {
        int failures = 0;

        List<RoundingMode> modes =
            List.of(RoundingMode.UP,      RoundingMode.DOWN,
                    RoundingMode.CEILING, RoundingMode.FLOOR,
                    RoundingMode.HALF_UP, RoundingMode.HALF_DOWN, RoundingMode.HALF_EVEN);

        for (int i = 10; i < 100; i++) {
            BigDecimal bd0 = BigDecimal.valueOf(i);
            BigDecimal bd1 = bd0.multiply(ONE_TENTH);
            BigDecimal bd2 = bd1.multiply(ONE_TENTH);

            for (BigDecimal bd : List.of(bd0, bd1, bd2)) {
                for (int precision = 1; i < 20; i++) {
                    for (RoundingMode rm : modes) {
                        MathContext mc = new MathContext(precision, rm);
                        failures += compareSqrtImplementations(bd, mc);
                    }
                }
            }
        }

        return failures;
    }

    private static int compareSqrtImplementations(BigDecimal bd, MathContext mc) {
        return equalNumerically(BigSquareRoot.sqrt(bd, mc),
                                bd.sqrt(mc), "sqrt(" + bd + ") under " + mc);
    }

    /**
     * sqrt(10^2N) is 10^N
     * Both numerical value and representation should be verified
     */
    private static int evenPowersOfTenTests() {
        int failures = 0;
        MathContext oneDigitExactly = new MathContext(1, RoundingMode.UNNECESSARY);

        for (int scale = -100; scale <= 100; scale++) {
            BigDecimal testValue               = BigDecimal.valueOf(1, 2*scale);
            BigDecimal expectedNumericalResult = BigDecimal.valueOf(1,   scale);

            BigDecimal result;

            failures += equalNumerically(expectedNumericalResult,
                                         result = testValue.sqrt(MathContext.DECIMAL64),
                                         "Even powers of 10, DECIMAL64");

            // Can round to one digit of precision exactly
            failures += equalNumerically(expectedNumericalResult,
                                         result = testValue.sqrt(oneDigitExactly),
                                         "even powers of 10, 1 digit");

            if (result.precision() > 1) {
                failures += 1;
                System.err.println("Excess precision for " + result);
            }

            // If rounding to more than one digit, do precision / scale checking...
        }

        return failures;
    }

    private static int squareRootTwoTests() {
        int failures = 0;

        // Square root of 2 truncated to 65 digits
        BigDecimal highPrecisionRoot2 =
            new BigDecimal("1.41421356237309504880168872420969807856967187537694807317667973799");

        RoundingMode[] modes = {
            RoundingMode.UP,       RoundingMode.DOWN,
            RoundingMode.CEILING, RoundingMode.FLOOR,
            RoundingMode.HALF_UP, RoundingMode.HALF_DOWN, RoundingMode.HALF_EVEN
        };


        // For each interesting rounding mode, for precisions 1 to, say,
        // 63 numerically compare TWO.sqrt(mc) to
        // highPrecisionRoot2.round(mc) and the alternative internal high-precision
        // implementation of square root.
        for (RoundingMode mode : modes) {
            for (int precision = 1; precision < 63; precision++) {
                MathContext mc = new MathContext(precision, mode);
                BigDecimal expected = highPrecisionRoot2.round(mc);
                BigDecimal computed = TWO.sqrt(mc);
                BigDecimal altComputed = BigSquareRoot.sqrt(TWO, mc);

                failures += equalNumerically(expected, computed, "sqrt(2)");
                failures += equalNumerically(computed, altComputed, "computed & altComputed");
            }
        }

        return failures;
    }

    private static int lowPrecisionPerfectSquares() {
        int failures = 0;

        // For 5^2 through 9^2, if the input is rounded to one digit
        // first before the root is computed, the wrong answer will
        // result. Verify results and scale for different rounding
        // modes and precisions.
        long[][] squaresWithOneDigitRoot = {{ 4, 2},
                                            { 9, 3},
                                            {25, 5},
                                            {36, 6},
                                            {49, 7},
                                            {64, 8},
                                            {81, 9}};

        for (long[] squareAndRoot : squaresWithOneDigitRoot) {
            BigDecimal square     = new BigDecimal(squareAndRoot[0]);
            BigDecimal expected   = new BigDecimal(squareAndRoot[1]);

            for (int scale = 0; scale <= 4; scale++) {
                BigDecimal scaledSquare = square.setScale(scale, RoundingMode.UNNECESSARY);
                int expectedScale = scale/2;
                for (int precision = 0; precision <= 5; precision++) {
                    for (RoundingMode rm : RoundingMode.values()) {
                        MathContext mc = new MathContext(precision, rm);
                        BigDecimal computedRoot = scaledSquare.sqrt(mc);
                        failures += equalNumerically(expected, computedRoot, "simple squares");
                        int computedScale = computedRoot.scale();
                        if (precision >=  expectedScale + 1 &&
                            computedScale != expectedScale) {
                            System.err.printf("%s\tprecision=%d\trm=%s%n",
                                              computedRoot.toString(), precision, rm);
                            failures++;
                            System.err.printf("\t%s does not have expected scale of %d%n.",
                                              computedRoot, expectedScale);
                        }
                    }
                }
            }
        }

        return failures;
    }

    /**
     * Test around 3.9999 that the sqrt doesn't improperly round-up to
     * a numerical value of 2.
     */
    private static int almostFourRoundingDown() {
        int failures = 0;
        BigDecimal nearFour = new BigDecimal("3.999999999999999999999999999999");

        // Sqrt is 1.9999...

        for (int i = 1; i < 64; i++) {
            MathContext mc = new MathContext(i, RoundingMode.FLOOR);
            BigDecimal result = nearFour.sqrt(mc);
            BigDecimal expected = BigSquareRoot.sqrt(nearFour, mc);
            failures += equalNumerically(expected, result, "near four rounding down");
            failures += (result.compareTo(TWO) < 0) ? 0  : 1 ;
        }

        return failures;
    }

    /**
     * Test around 4.000...1 that the sqrt doesn't improperly
     * round-down to a numerical value of 2.
     */
    private static int almostFourRoundingUp() {
        int failures = 0;
        BigDecimal nearFour = new BigDecimal("4.000000000000000000000000000001");

        // Sqrt is 2.0000....<non-zero digits>

        for (int i = 1; i < 64; i++) {
            MathContext mc = new MathContext(i, RoundingMode.CEILING);
            BigDecimal result = nearFour.sqrt(mc);
            BigDecimal expected = BigSquareRoot.sqrt(nearFour, mc);
            failures += equalNumerically(expected, result, "near four rounding up");
            failures += (result.compareTo(TWO) > 0) ? 0  : 1 ;
        }

        return failures;
    }

    private static int nearTen() {
        int failures = 0;

         BigDecimal near10 = new BigDecimal("9.99999999999999999999");

         BigDecimal near10sq = near10.multiply(near10);

         BigDecimal near10sq_ulp = near10sq.add(near10sq.ulp());

        for (int i = 10; i < 23; i++) {
            MathContext mc = new MathContext(i, RoundingMode.HALF_EVEN);

            failures += equalNumerically(BigSquareRoot.sqrt(near10sq_ulp, mc),
                                         near10sq_ulp.sqrt(mc),
                                         "near 10 rounding half even");
        }

        return failures;
    }


    /*
     * Probe for rounding failures near a power of ten, 1 = 10^0,
     * where an ulp has a different size above and below the value.
     */
    private static int nearOne() {
        int failures = 0;

         BigDecimal near1 = new BigDecimal(".999999999999999999999");
         BigDecimal near1sq = near1.multiply(near1);
         BigDecimal near1sq_ulp = near1sq.add(near1sq.ulp());

         for (int i = 10; i < 23; i++) {
             for (RoundingMode rm : List.of(RoundingMode.HALF_EVEN,
                                            RoundingMode.UP,
                                            RoundingMode.DOWN )) {
                 MathContext mc = new MathContext(i, rm);
                 failures += equalNumerically(BigSquareRoot.sqrt(near1sq_ulp, mc),
                                              near1sq_ulp.sqrt(mc),
                                              mc.toString());
             }
         }

         return failures;
    }


    private static int halfWay() {
        int failures = 0;

        /*
         * Use enough digits that the exact result cannot be computed
         * from the sqrt of a double.
         */
        BigDecimal[] halfWayCases = {
            // Odd next digit, truncate on HALF_EVEN
            new BigDecimal("123456789123456789.5"),

             // Even next digit, round up on HALF_EVEN
            new BigDecimal("123456789123456788.5"),
        };

        for (BigDecimal halfWayCase : halfWayCases) {
            // Round result to next-to-last place
            int precision = halfWayCase.precision() - 1;
            BigDecimal square = halfWayCase.multiply(halfWayCase);

            for (RoundingMode rm : List.of(RoundingMode.HALF_EVEN,
                                           RoundingMode.HALF_UP,
                                           RoundingMode.HALF_DOWN)) {
                MathContext mc = new MathContext(precision, rm);

                System.out.println("\nRounding mode " + rm);
                System.out.println("\t" + halfWayCase.round(mc) + "\t" + halfWayCase);
                System.out.println("\t" + BigSquareRoot.sqrt(square, mc));

                failures += equalNumerically(/*square.sqrt(mc),*/
                                             BigSquareRoot.sqrt(square, mc),
                                             halfWayCase.round(mc),
                                             "Rounding halway " + rm);
            }
        }

        return failures;
    }

    private static int compare(BigDecimal a, BigDecimal b, boolean expected, String prefix) {
        boolean result = a.equals(b);
        int failed = (result==expected) ? 0 : 1;
        if (failed == 1) {
            System.err.println("Testing " + prefix +
                               "(" + a + ").compareTo(" + b + ") => " + result +
                               "\n\tExpected " + expected);
        }
        return failed;
    }

    private static int equalNumerically(BigDecimal a, BigDecimal b,
                                        String prefix) {
        return compareNumerically(a, b, 0, prefix);
    }


    private static int compareNumerically(BigDecimal a, BigDecimal b,
                                          int expected, String prefix) {
        int result = a.compareTo(b);
        int failed = (result==expected) ? 0 : 1;
        if (failed == 1) {
            System.err.println("Testing " + prefix +
                               "(" + a + ").compareTo(" + b + ") => " + result +
                               "\n\tExpected " + expected);
        }
        return failed;
    }

    /**
     * Alternative implementation of BigDecimal square root which uses
     * higher-precision for a simpler set of termination conditions
     * for the Newton iteration.
     */
    private static class BigSquareRoot {

        /**
         * The value 0.5, with a scale of 1.
         */
        private static final BigDecimal ONE_HALF = valueOf(5L, 1);

        public static boolean isPowerOfTen(BigDecimal bd) {
            return BigInteger.ONE.equals(bd.unscaledValue());
        }

        public static BigDecimal square(BigDecimal bd) {
            return bd.multiply(bd);
        }

        public static BigDecimal sqrt(BigDecimal bd, MathContext mc) {
            int signum = bd.signum();
            if (signum == 1) {
                /*
                 * The following code draws on the algorithm presented in
                 * "Properly Rounded Variable Precision Square Root," Hull and
                 * Abrham, ACM Transactions on Mathematical Software, Vol 11,
                 * No. 3, September 1985, Pages 229-237.
                 *
                 * The BigDecimal computational model differs from the one
                 * presented in the paper in several ways: first BigDecimal
                 * numbers aren't necessarily normalized, second many more
                 * rounding modes are supported, including UNNECESSARY, and
                 * exact results can be requested.
                 *
                 * The main steps of the algorithm below are as follows,
                 * first argument reduce the value to the numerical range
                 * [1, 10) using the following relations:
                 *
                 * x = y * 10 ^ exp
                 * sqrt(x) = sqrt(y) * 10^(exp / 2) if exp is even
                 * sqrt(x) = sqrt(y/10) * 10 ^((exp+1)/2) is exp is odd
                 *
                 * Then use Newton's iteration on the reduced value to compute
                 * the numerical digits of the desired result.
                 *
                 * Finally, scale back to the desired exponent range and
                 * perform any adjustment to get the preferred scale in the
                 * representation.
                 */

                // The code below favors relative simplicity over checking
                // for special cases that could run faster.

                int preferredScale = bd.scale()/2;
                BigDecimal zeroWithFinalPreferredScale =
                    BigDecimal.valueOf(0L, preferredScale);

                // First phase of numerical normalization, strip trailing
                // zeros and check for even powers of 10.
                BigDecimal stripped = bd.stripTrailingZeros();
                int strippedScale = stripped.scale();

                // Numerically sqrt(10^2N) = 10^N
                if (isPowerOfTen(stripped) &&
                    strippedScale % 2 == 0) {
                    BigDecimal result = BigDecimal.valueOf(1L, strippedScale/2);
                    if (result.scale() != preferredScale) {
                        // Adjust to requested precision and preferred
                        // scale as appropriate.
                        result = result.add(zeroWithFinalPreferredScale, mc);
                    }
                    return result;
                }

                // After stripTrailingZeros, the representation is normalized as
                //
                // unscaledValue * 10^(-scale)
                //
                // where unscaledValue is an integer with the mimimum
                // precision for the cohort of the numerical value. To
                // allow binary floating-point hardware to be used to get
                // approximately a 15 digit approximation to the square
                // root, it is helpful to instead normalize this so that
                // the significand portion is to right of the decimal
                // point by roughly (scale() - precision() + 1).

                // Now the precision / scale adjustment
                int scaleAdjust = 0;
                int scale = stripped.scale() - stripped.precision() + 1;
                if (scale % 2 == 0) {
                    scaleAdjust = scale;
                } else {
                    scaleAdjust = scale - 1;
                }

                BigDecimal working = stripped.scaleByPowerOfTen(scaleAdjust);

                assert  // Verify 0.1 <= working < 10
                    ONE_TENTH.compareTo(working) <= 0 && working.compareTo(TEN) < 0;

                // Use good ole' Math.sqrt to get the initial guess for
                // the Newton iteration, good to at least 15 decimal
                // digits. This approach does incur the cost of a
                //
                // BigDecimal -> double -> BigDecimal
                //
                // conversion cycle, but it avoids the need for several
                // Newton iterations in BigDecimal arithmetic to get the
                // working answer to 15 digits of precision. If many fewer
                // than 15 digits were needed, it might be faster to do
                // the loop entirely in BigDecimal arithmetic.
                //
                // (A double value might have as much many as 17 decimal
                // digits of precision; it depends on the relative density
                // of binary and decimal numbers at different regions of
                // the number line.)
                //
                // (It would be possible to check for certain special
                // cases to avoid doing any Newton iterations. For
                // example, if the BigDecimal -> double conversion was
                // known to be exact and the rounding mode had a
                // low-enough precision, the post-Newton rounding logic
                // could be applied directly.)

                BigDecimal guess = new BigDecimal(Math.sqrt(working.doubleValue()));
                int guessPrecision = 15;
                int originalPrecision = mc.getPrecision();
                int targetPrecision;

                // If an exact value is requested, it must only need
                // about half of the input digits to represent since
                // multiplying an N digit number by itself yield a (2N
                // - 1) digit or 2N digit result.
                if (originalPrecision == 0) {
                    targetPrecision = stripped.precision()/2 + 1;
                } else {
                    targetPrecision = originalPrecision;
                }

                // When setting the precision to use inside the Newton
                // iteration loop, take care to avoid the case where the
                // precision of the input exceeds the requested precision
                // and rounding the input value too soon.
                BigDecimal approx = guess;
                int workingPrecision = working.precision();
                // Use "2p + 2" property to guarantee enough
                // intermediate precision so that a double-rounding
                // error does not occur when rounded to the final
                // destination precision.
                int loopPrecision =
                    Math.max(2 * Math.max(targetPrecision, workingPrecision) + 2,
                             34); // Force at least two Netwon
                                  // iterations on the Math.sqrt
                                  // result.
                do {
                    MathContext mcTmp = new MathContext(loopPrecision, RoundingMode.HALF_EVEN);
                    // approx = 0.5 * (approx + fraction / approx)
                    approx = ONE_HALF.multiply(approx.add(working.divide(approx, mcTmp), mcTmp));
                    guessPrecision *= 2;
                } while (guessPrecision < loopPrecision);

                BigDecimal result;
                RoundingMode targetRm = mc.getRoundingMode();
                if (targetRm == RoundingMode.UNNECESSARY || originalPrecision == 0) {
                    RoundingMode tmpRm =
                        (targetRm == RoundingMode.UNNECESSARY) ? RoundingMode.DOWN : targetRm;
                    MathContext mcTmp = new MathContext(targetPrecision, tmpRm);
                    result = approx.scaleByPowerOfTen(-scaleAdjust/2).round(mcTmp);

                    // If result*result != this numerically, the square
                    // root isn't exact
                    if (bd.subtract(square(result)).compareTo(ZERO) != 0) {
                        throw new ArithmeticException("Computed square root not exact.");
                    }
                } else {
                    result = approx.scaleByPowerOfTen(-scaleAdjust/2).round(mc);
                }

                assert squareRootResultAssertions(bd, result, mc);
                if (result.scale() != preferredScale) {
                    // The preferred scale of an add is
                    // max(addend.scale(), augend.scale()). Therefore, if
                    // the scale of the result is first minimized using
                    // stripTrailingZeros(), adding a zero of the
                    // preferred scale rounding the correct precision will
                    // perform the proper scale vs precision tradeoffs.
                    result = result.stripTrailingZeros().
                        add(zeroWithFinalPreferredScale,
                            new MathContext(originalPrecision, RoundingMode.UNNECESSARY));
                }
                return result;
            } else {
                switch (signum) {
                case -1:
                    throw new ArithmeticException("Attempted square root " +
                                                  "of negative BigDecimal");
                case 0:
                    return valueOf(0L, bd.scale()/2);

                default:
                    throw new AssertionError("Bad value from signum");
                }
            }
        }

        /**
         * For nonzero values, check numerical correctness properties of
         * the computed result for the chosen rounding mode.
         *
         * For the directed roundings, for DOWN and FLOOR, result^2 must
         * be {@code <=} the input and (result+ulp)^2 must be {@code >} the
         * input. Conversely, for UP and CEIL, result^2 must be {@code >=} the
         * input and (result-ulp)^2 must be {@code <} the input.
         */
        private static boolean squareRootResultAssertions(BigDecimal input, BigDecimal result, MathContext mc) {
            if (result.signum() == 0) {
                return squareRootZeroResultAssertions(input, result, mc);
            } else {
                RoundingMode rm = mc.getRoundingMode();
                BigDecimal ulp = result.ulp();
                BigDecimal neighborUp   = result.add(ulp);
                // Make neighbor down accurate even for powers of ten
                if (isPowerOfTen(result)) {
                    ulp = ulp.divide(TEN);
                }
                BigDecimal neighborDown = result.subtract(ulp);

                // Both the starting value and result should be nonzero and positive.
                if (result.signum() != 1 ||
                    input.signum() != 1) {
                    return false;
                }

                switch (rm) {
                case DOWN:
                case FLOOR:
                    assert
                        square(result).compareTo(input)    <= 0 &&
                        square(neighborUp).compareTo(input) > 0:
                    "Square of result out for bounds rounding " + rm;
                    return true;

                case UP:
                case CEILING:
                    assert
                        square(result).compareTo(input) >= 0 :
                    "Square of result too small rounding " + rm;

                    assert
                        square(neighborDown).compareTo(input) < 0 :
                    "Square of down neighbor too large rounding  " + rm + "\n" +
                        "\t input: " + input + "\t neighborDown: " +  neighborDown +"\t sqrt: " + result +
                        "\t" + mc;
                    return true;


                case HALF_DOWN:
                case HALF_EVEN:
                case HALF_UP:
                    BigDecimal err = square(result).subtract(input).abs();
                    BigDecimal errUp = square(neighborUp).subtract(input);
                    BigDecimal errDown =  input.subtract(square(neighborDown));
                    // All error values should be positive so don't need to
                    // compare absolute values.

                    int err_comp_errUp = err.compareTo(errUp);
                    int err_comp_errDown = err.compareTo(errDown);

                    assert
                        errUp.signum()   == 1 &&
                        errDown.signum() == 1 :
                    "Errors of neighbors squared don't have correct signs";

                    // At least one of these must be true, but not both
//                     assert
//                         err_comp_errUp   <= 0 : "Upper neighbor is closer than result: " + rm +
//                         "\t" + input + "\t result" + result;
//                     assert
//                         err_comp_errDown <= 0 : "Lower neighbor is closer than result: " + rm +
//                         "\t" + input + "\t result " + result + "\t lower neighbor: " + neighborDown;

                    assert
                        ((err_comp_errUp   == 0 ) ? err_comp_errDown < 0 : true) &&
                        ((err_comp_errDown == 0 ) ? err_comp_errUp   < 0 : true) :
                            "Incorrect error relationships";
                        // && could check for digit conditions for ties too
                        return true;

                default: // Definition of UNNECESSARY already verified.
                    return true;
                }
            }
        }

        private static boolean squareRootZeroResultAssertions(BigDecimal input,
                                                              BigDecimal result,
                                                              MathContext mc) {
            return input.compareTo(ZERO) == 0;
        }
    }
}

