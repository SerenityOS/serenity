/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4851776 4907265 6177836 6876282 8066842
 * @summary Some tests for the divide methods.
 * @author Joseph D. Darcy
 */

import java.math.*;
import static java.math.BigDecimal.*;

public class DivideTests {

    // Preliminary exact divide method; could be used for comparison
    // purposes.
    BigDecimal anotherDivide(BigDecimal dividend, BigDecimal divisor) {
        /*
         * Handle zero cases first.
         */
        if (divisor.signum() == 0) {   // x/0
            if (dividend.signum() == 0)    // 0/0
                throw new ArithmeticException("Division undefined");  // NaN
            throw new ArithmeticException("Division by zero");
        }
        if (dividend.signum() == 0)        // 0/y
            return BigDecimal.ZERO;
        else {
            /*
             * Determine if there is a result with a terminating
             * decimal expansion.  Putting aside overflow and
             * underflow considerations, the existance of an exact
             * result only depends on the ratio of the intVal's of the
             * dividend (i.e. this) and and divisor since the scales
             * of the argument just affect where the decimal point
             * lies.
             *
             * For the ratio of (a = this.intVal) and (b =
             * divisor.intVal) to have a finite decimal expansion,
             * once a/b is put in lowest terms, b must be equal to
             * (2^i)*(5^j) for some integer i,j >= 0.  Therefore, we
             * first compute to see if b_prime =(b/gcd(a,b)) is equal
             * to (2^i)*(5^j).
             */
            BigInteger TWO  = BigInteger.valueOf(2);
            BigInteger FIVE = BigInteger.valueOf(5);
            BigInteger TEN  = BigInteger.valueOf(10);

            BigInteger divisorIntvalue  = divisor.scaleByPowerOfTen(divisor.scale()).toBigInteger().abs();
            BigInteger dividendIntvalue = dividend.scaleByPowerOfTen(dividend.scale()).toBigInteger().abs();

            BigInteger b_prime = divisorIntvalue.divide(dividendIntvalue.gcd(divisorIntvalue));

            boolean goodDivisor = false;
            int i=0, j=0;

            badDivisor: {
                while(! b_prime.equals(BigInteger.ONE) ) {
                    int b_primeModTen = b_prime.mod(TEN).intValue() ;

                    switch(b_primeModTen) {
                    case 0:
                        // b_prime divisible by 10=2*5, increment i and j
                        i++;
                        j++;
                        b_prime = b_prime.divide(TEN);
                        break;

                    case 5:
                        // b_prime divisible by 5, increment j
                        j++;
                        b_prime = b_prime.divide(FIVE);
                        break;

                    case 2:
                    case 4:
                    case 6:
                    case 8:
                        // b_prime divisible by 2, increment i
                        i++;
                        b_prime = b_prime.divide(TWO);
                        break;

                    default: // hit something we shouldn't have
                        b_prime = BigInteger.ONE; // terminate loop
                        break badDivisor;
                    }
                }

                goodDivisor = true;
            }

            if( ! goodDivisor ) {
                throw new ArithmeticException("Non terminating decimal expansion");
            }
            else {
                // What is a rule for determining how many digits are
                // needed?  Once that is determined, cons up a new
                // MathContext object and pass it on to the divide(bd,
                // mc) method; precision == ?, roundingMode is unnecessary.

                // Are we sure this is the right scale to use?  Should
                // also determine a precision-based method.
                MathContext mc = new MathContext(dividend.precision() +
                                                 (int)Math.ceil(
                                                      10.0*divisor.precision()/3.0),
                                                 RoundingMode.UNNECESSARY);
                // Should do some more work here to rescale, etc.
                return dividend.divide(divisor, mc);
            }
        }
    }

    public static int powersOf2and5() {
        int failures = 0;

        for(int i = 0; i < 6; i++) {
            int powerOf2 = (int)StrictMath.pow(2.0, i);

            for(int j = 0; j < 6; j++) {
                int powerOf5 = (int)StrictMath.pow(5.0, j);
                int product;

                BigDecimal bd;

                try {
                    bd = BigDecimal.ONE.divide(new BigDecimal(product=powerOf2*powerOf5));
                } catch (ArithmeticException e) {
                    failures++;
                    System.err.println((new BigDecimal(powerOf2)).toString() + " / " +
                                       (new BigDecimal(powerOf5)).toString() + " threw an exception.");
                    e.printStackTrace();
                }

                try {
                    bd = new BigDecimal(powerOf2).divide(new BigDecimal(powerOf5));
                } catch (ArithmeticException e) {
                    failures++;
                    System.err.println((new BigDecimal(powerOf2)).toString() + " / " +
                                       (new BigDecimal(powerOf5)).toString() + " threw an exception.");
                    e.printStackTrace();
                }

                try {
                    bd = new BigDecimal(powerOf5).divide(new BigDecimal(powerOf2));
                } catch (ArithmeticException e) {
                    failures++;
                    System.err.println((new BigDecimal(powerOf5)).toString() + " / " +
                                       (new BigDecimal(powerOf2)).toString() + " threw an exception.");

                    e.printStackTrace();
                }

            }
        }
        return failures;
    }

    public static int nonTerminating() {
        int failures = 0;
        int[] primes = {1, 3, 7, 13, 17};

        // For each pair of prime products, verify the ratio of
        // non-equal products has a non-terminating expansion.

        for(int i = 0; i < primes.length; i++) {
            for(int j = i+1; j < primes.length; j++) {

                for(int m = 0; m < primes.length; m++) {
                    for(int n = m+1; n < primes.length; n++) {
                        int dividend = primes[i] * primes[j];
                        int divisor  = primes[m] * primes[n];

                        if ( ((dividend/divisor) * divisor) != dividend ) {
                            try {
                                BigDecimal quotient = (new BigDecimal(dividend).
                                                       divide(new BigDecimal(divisor)));
                                failures++;
                                System.err.println("Exact quotient " + quotient.toString() +
                                                   " returned for non-terminating fraction " +
                                                   dividend + " / " + divisor + ".");
                            }
                            catch (ArithmeticException e) {
                                ; // Correct result
                            }
                        }

                    }
                }
            }
        }

        return failures;
    }

    public static int properScaleTests(){
        int failures = 0;

        BigDecimal[][] testCases = {
            {new BigDecimal("1"),       new BigDecimal("5"),            new BigDecimal("2e-1")},
            {new BigDecimal("1"),       new BigDecimal("50e-1"),        new BigDecimal("2e-1")},
            {new BigDecimal("10e-1"),   new BigDecimal("5"),            new BigDecimal("2e-1")},
            {new BigDecimal("1"),       new BigDecimal("500e-2"),       new BigDecimal("2e-1")},
            {new BigDecimal("100e-2"),  new BigDecimal("5"),            new BigDecimal("20e-2")},
            {new BigDecimal("1"),       new BigDecimal("32"),           new BigDecimal("3125e-5")},
            {new BigDecimal("1"),       new BigDecimal("64"),           new BigDecimal("15625e-6")},
            {new BigDecimal("1.0000000"),       new BigDecimal("64"),   new BigDecimal("156250e-7")},
        };


        for(BigDecimal[] tc : testCases) {
            BigDecimal quotient;
            if (! (quotient = tc[0].divide(tc[1])).equals(tc[2]) ) {
                failures++;
                System.err.println("Unexpected quotient from " + tc[0] + " / " + tc[1] +
                                   "; expected " + tc[2] + " got " + quotient);
            }
        }

        return failures;
    }

    public static int trailingZeroTests() {
        int failures = 0;

        MathContext mc = new MathContext(3, RoundingMode.FLOOR);
        BigDecimal[][] testCases = {
            {new BigDecimal("19"),      new BigDecimal("100"),          new BigDecimal("0.19")},
            {new BigDecimal("21"),      new BigDecimal("110"),          new BigDecimal("0.190")},
        };

        for(BigDecimal[] tc : testCases) {
            BigDecimal quotient;
            if (! (quotient = tc[0].divide(tc[1], mc)).equals(tc[2]) ) {
                failures++;
                System.err.println("Unexpected quotient from " + tc[0] + " / " + tc[1] +
                                   "; expected " + tc[2] + " got " + quotient);
            }
        }

        return failures;
    }

    public static int scaledRoundedDivideTests() {
        int failures = 0;
        // Tests of the traditional scaled divide under different
        // rounding modes.

        // Encode rounding mode and scale for the divide in a
        // BigDecimal with the significand equal to the rounding mode
        // and the scale equal to the number's scale.

        // {dividend, dividisor, rounding, quotient}
        BigDecimal a = new BigDecimal("31415");
        BigDecimal a_minus = a.negate();
        BigDecimal b = new BigDecimal("10000");

        BigDecimal c = new BigDecimal("31425");
        BigDecimal c_minus = c.negate();

         // Ad hoc tests
        BigDecimal d = new BigDecimal(new BigInteger("-37361671119238118911893939591735"), 10);
        BigDecimal e = new BigDecimal(new BigInteger("74723342238476237823787879183470"), 15);

        BigDecimal[][] testCases = {
            {a,         b,      BigDecimal.valueOf(ROUND_UP, 3),        new BigDecimal("3.142")},
            {a_minus,   b,      BigDecimal.valueOf(ROUND_UP, 3),        new BigDecimal("-3.142")},

            {a,         b,      BigDecimal.valueOf(ROUND_DOWN, 3),      new BigDecimal("3.141")},
            {a_minus,   b,      BigDecimal.valueOf(ROUND_DOWN, 3),      new BigDecimal("-3.141")},

            {a,         b,      BigDecimal.valueOf(ROUND_CEILING, 3),   new BigDecimal("3.142")},
            {a_minus,   b,      BigDecimal.valueOf(ROUND_CEILING, 3),   new BigDecimal("-3.141")},

            {a,         b,      BigDecimal.valueOf(ROUND_FLOOR, 3),     new BigDecimal("3.141")},
            {a_minus,   b,      BigDecimal.valueOf(ROUND_FLOOR, 3),     new BigDecimal("-3.142")},

            {a,         b,      BigDecimal.valueOf(ROUND_HALF_UP, 3),   new BigDecimal("3.142")},
            {a_minus,   b,      BigDecimal.valueOf(ROUND_HALF_UP, 3),   new BigDecimal("-3.142")},

            {a,         b,      BigDecimal.valueOf(ROUND_DOWN, 3),      new BigDecimal("3.141")},
            {a_minus,   b,      BigDecimal.valueOf(ROUND_DOWN, 3),      new BigDecimal("-3.141")},

            {a,         b,      BigDecimal.valueOf(ROUND_HALF_EVEN, 3), new BigDecimal("3.142")},
            {a_minus,   b,      BigDecimal.valueOf(ROUND_HALF_EVEN, 3), new BigDecimal("-3.142")},

            {c,         b,      BigDecimal.valueOf(ROUND_HALF_EVEN, 3), new BigDecimal("3.142")},
            {c_minus,   b,      BigDecimal.valueOf(ROUND_HALF_EVEN, 3), new BigDecimal("-3.142")},

            {d,         e,      BigDecimal.valueOf(ROUND_HALF_UP, -5),   BigDecimal.valueOf(-1, -5)},
            {d,         e,      BigDecimal.valueOf(ROUND_HALF_DOWN, -5), BigDecimal.valueOf(0, -5)},
            {d,         e,      BigDecimal.valueOf(ROUND_HALF_EVEN, -5), BigDecimal.valueOf(0, -5)},
        };

        for(BigDecimal tc[] : testCases) {
            int scale = tc[2].scale();
            int rm = tc[2].unscaledValue().intValue();

            BigDecimal quotient = tc[0].divide(tc[1], scale, rm);
            if (!quotient.equals(tc[3])) {
                failures++;
                System.err.println("Unexpected quotient from " + tc[0] + " / " + tc[1] +
                                   " scale " + scale + " rounding mode " + RoundingMode.valueOf(rm) +
                                   "; expected " + tc[3] + " got " + quotient);
            }
        }

        // 6876282
        BigDecimal[][] testCases2 = {
            // { dividend, divisor, expected quotient }
            { new BigDecimal(3090), new BigDecimal(7), new BigDecimal(441) },
            { new BigDecimal("309000000000000000000000"), new BigDecimal("700000000000000000000"),
              new BigDecimal(441) },
            { new BigDecimal("962.430000000000"), new BigDecimal("8346463.460000000000"),
              new BigDecimal("0.000115309916") },
            { new BigDecimal("18446744073709551631"), new BigDecimal("4611686018427387909"),
              new BigDecimal(4) },
            { new BigDecimal("18446744073709551630"), new BigDecimal("4611686018427387909"),
              new BigDecimal(4) },
            { new BigDecimal("23058430092136939523"), new BigDecimal("4611686018427387905"),
              new BigDecimal(5) },
            { new BigDecimal("-18446744073709551661"), new BigDecimal("-4611686018427387919"),
              new BigDecimal(4) },
            { new BigDecimal("-18446744073709551660"), new BigDecimal("-4611686018427387919"),
              new BigDecimal(4) },
        };

        for (BigDecimal test[] : testCases2) {
            BigDecimal quo = test[0].divide(test[1], RoundingMode.HALF_UP);
            if (!quo.equals(test[2])) {
                failures++;
                System.err.println("Unexpected quotient from " + test[0] + " / " + test[1] +
                                   " rounding mode HALF_UP" +
                                   "; expected " + test[2] + " got " + quo);
            }
        }
        return failures;
    }

    private static int divideByOneTests() {
        int failures = 0;

        //problematic divisor: one with scale 17
        BigDecimal one = BigDecimal.ONE.setScale(17);
        RoundingMode rounding = RoundingMode.UNNECESSARY;

        long[][] unscaledAndScale = new long[][] {
            { Long.MAX_VALUE,  17},
            {-Long.MAX_VALUE,  17},
            { Long.MAX_VALUE,   0},
            {-Long.MAX_VALUE,   0},
            { Long.MAX_VALUE, 100},
            {-Long.MAX_VALUE, 100}
        };

        for (long[] uas : unscaledAndScale) {
            long unscaled = uas[0];
            int scale = (int)uas[1];

            BigDecimal noRound = null;
            try {
                noRound = BigDecimal.valueOf(unscaled, scale).
                    divide(one, RoundingMode.UNNECESSARY);
            } catch (ArithmeticException e) {
                failures++;
                System.err.println("ArithmeticException for value " + unscaled
                    + " and scale " + scale + " without rounding");
            }

            BigDecimal roundDown = null;
            try {
                roundDown = BigDecimal.valueOf(unscaled, scale).
                        divide(one, RoundingMode.DOWN);
            } catch (ArithmeticException e) {
                failures++;
                System.err.println("ArithmeticException for value " + unscaled
                    + " and scale " + scale + " with rounding down");
            }

            if (noRound != null && roundDown != null
                && noRound.compareTo(roundDown) != 0) {
                failures++;
                System.err.println("Equality failure for value " + unscaled
                        + " and scale " + scale);
            }
        }

        return failures;
    }

    public static void main(String argv[]) {
        int failures = 0;

        failures += powersOf2and5();
        failures += nonTerminating();
        failures += properScaleTests();
        failures += trailingZeroTests();
        failures += scaledRoundedDivideTests();
        failures += divideByOneTests();

        if (failures > 0) {
            throw new RuntimeException("Incurred " + failures +
                                       " failures while testing division.");
        }
    }
}
