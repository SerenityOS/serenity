/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6362557 8200698
 * @summary Some tests of add(BigDecimal, mc)
 * @author Joseph D. Darcy
 */

import java.math.*;
import static java.math.BigDecimal.*;
import java.util.Set;
import java.util.EnumSet;

public class AddTests {

    private static Set<RoundingMode> nonExactRoundingModes =
        EnumSet.complementOf(EnumSet.of(RoundingMode.UNNECESSARY));

    /**
     * Test for some simple additions, particularly, it will test
     * the overflow case.
     */
    private static int simpleTests() {
        int failures = 0;

        BigDecimal[] bd1 = {
            new BigDecimal(new BigInteger("7812404666936930160"), 11),
            new BigDecimal(new BigInteger("7812404666936930160"), 12),
            new BigDecimal(new BigInteger("7812404666936930160"), 13),
        };
        BigDecimal bd2 = new BigDecimal(new BigInteger("2790000"), 1);
        BigDecimal[] expectedResult = {
            new BigDecimal("78403046.66936930160"),
            new BigDecimal("8091404.666936930160"),
            new BigDecimal("1060240.4666936930160"),
        };
        for (int i = 0; i < bd1.length; i++) {
            if (!bd1[i].add(bd2).equals(expectedResult[i]))
                failures++;
        }
        return failures;
    }

    /**
     * Test for extreme value of scale and rounding precision that
     * could cause integer overflow in right-shift-into-sticky-bit
     * computations.
     */
    private static int extremaTests() {
        int failures = 0;

        failures += addWithoutException(valueOf(1, -Integer.MAX_VALUE),
                                        valueOf(2, Integer.MAX_VALUE), null);
        failures += addWithoutException(valueOf(1, -Integer.MAX_VALUE),
                                        valueOf(-2, Integer.MAX_VALUE), null);
        return failures;
    }

    /**
     * Print sum of b1 and b2; correct result will not throw an
     * exception.
     */
    private static int addWithoutException(BigDecimal b1, BigDecimal b2, MathContext mc) {
        if (mc == null)
            mc = new MathContext(2, RoundingMode.DOWN);

        try {
            BigDecimal sum = b1.add(b2, mc);
            printAddition(b1, b2, sum.toString());
            return 0;
        } catch(ArithmeticException ae) {
            printAddition(b1, b2, "Exception!");
            return 1;
        }
    }

    /**
     * Test combinations of operands that may meet the condensation
     * criteria when rounded to different precisions.
     */
    private static int roundingGradationTests() {
        int failures = 0;

        failures += roundAway(new BigDecimal("1234e100"),
                              new BigDecimal(   "1234e97"));

        failures += roundAway(new BigDecimal("1234e100"),
                              new BigDecimal(    "1234e96"));

        failures += roundAway(new BigDecimal("1234e100"),
                              new BigDecimal(     "1234e95"));

        failures += roundAway(new BigDecimal("1234e100"),
                              new BigDecimal(      "1234e94"));

        failures += roundAway(new BigDecimal("1234e100"),
                              new BigDecimal(       "1234e93"));

        failures += roundAway(new BigDecimal("1234e100"),
                              new BigDecimal(        "1234e92"));

        failures += roundAway(new BigDecimal("1234e100"),
                              new BigDecimal("1234e50"));


        failures += roundAway(new BigDecimal("1000e100"),
                              new BigDecimal(   "1234e97"));

        failures += roundAway(new BigDecimal("1000e100"),
                              new BigDecimal(    "1234e96"));

        failures += roundAway(new BigDecimal("1000e100"),
                              new BigDecimal(     "1234e95"));

        failures += roundAway(new BigDecimal("1000e100"),
                              new BigDecimal(      "1234e94"));

        failures += roundAway(new BigDecimal("1000e100"),
                              new BigDecimal(       "1234e93"));

        failures += roundAway(new BigDecimal("1000e100"),
                              new BigDecimal(        "1234e92"));

        failures += roundAway(new BigDecimal("1000e100"),
                              new BigDecimal("1234e50"));



        failures += roundAway(new BigDecimal("1999e100"),
                              new BigDecimal(   "1234e97"));

        failures += roundAway(new BigDecimal("1999e100"),
                              new BigDecimal(    "1234e96"));

        failures += roundAway(new BigDecimal("1999e100"),
                              new BigDecimal(     "1234e95"));

        failures += roundAway(new BigDecimal("1999e100"),
                              new BigDecimal(      "1234e94"));

        failures += roundAway(new BigDecimal("1999e100"),
                              new BigDecimal(       "1234e93"));

        failures += roundAway(new BigDecimal("1999e100"),
                              new BigDecimal(        "1234e92"));

        failures += roundAway(new BigDecimal("1999e100"),
                              new BigDecimal("1234e50"));



        failures += roundAway(new BigDecimal("9999e100"),
                              new BigDecimal(   "1234e97"));

        failures += roundAway(new BigDecimal("9999e100"),
                              new BigDecimal(    "1234e96"));

        failures += roundAway(new BigDecimal("9999e100"),
                              new BigDecimal(     "1234e95"));

        failures += roundAway(new BigDecimal("9999e100"),
                              new BigDecimal(      "1234e94"));

        failures += roundAway(new BigDecimal("9999e100"),
                              new BigDecimal(       "1234e93"));

        failures += roundAway(new BigDecimal("9999e100"),
                              new BigDecimal(        "1234e92"));

        failures += roundAway(new BigDecimal("9999e100"),
                              new BigDecimal("1234e50"));

        return failures;
    }

    private static void printAddition(BigDecimal b1, BigDecimal b2, String s) {
        System.out.println("" + b1+ "\t+\t" + b2 + "\t=\t" + s);
    }

    private static int roundAway(BigDecimal b1, BigDecimal b2) {
        int failures = 0;

        b1.precision();
        b2.precision();

        BigDecimal b1_negate = b1.negate();
        BigDecimal b2_negate = b2.negate();

        b1_negate.precision();
        b2_negate.precision();

        failures += roundAway1(b1,        b2);
        failures += roundAway1(b1,        b2_negate);
        failures += roundAway1(b1_negate, b2);
        failures += roundAway1(b1_negate, b2_negate);

        return failures;
    }

    private static int roundAway1(BigDecimal b1, BigDecimal b2) {
        int failures = 0;
        failures += roundAway0(b1, b2);
        failures += roundAway0(b2, b1);
        return failures;
    }

    /**
     * Compare b1.add(b2, mc) with b1.add(b2).round(mc) for a variety
     * of MathContexts.
     */
    private static int roundAway0(BigDecimal b1, BigDecimal b2) {
        int failures = 0;
        BigDecimal exactSum = b1.add(b2);

        for(int precision = 1 ; precision < exactSum.precision()+2; precision++) {
            for(RoundingMode rm : nonExactRoundingModes) {
                MathContext mc = new MathContext(precision, rm);
                BigDecimal roundedExactSum = exactSum.round(mc);

                try {
                    BigDecimal sum = b1.add(b2, mc);

                    if (!roundedExactSum.equals(sum) ) {
                        failures++;
                        System.out.println("Exact sum " + exactSum +
                                           "\trounded by " + mc +
                                           "\texpected: " + roundedExactSum + " got: ");
                        printAddition(b1, b2, sum.toString());
                    }
//                  else {
//                      System.out.print(mc + "\t");
//                      printAddition(b1, b2, sum.toString());
//                  }

                } catch (ArithmeticException ae) {
                    printAddition(b1, b2, "Exception!");
                    failures++;
                }
            }
        }

        return failures;
    }

    /**
     * Verify calling the precision method should not change the
     * computed result.
     */
    private static int precisionConsistencyTest() {
        int failures = 0;
        MathContext mc = new MathContext(1,RoundingMode.DOWN);
        BigDecimal a = BigDecimal.valueOf(1999, -1); //value is equivalent to 19990

        BigDecimal sum1 = a.add(BigDecimal.ONE, mc);
        a.precision();
        BigDecimal sum2 = a.add(BigDecimal.ONE, mc);

        if (!sum1.equals(sum2)) {
            failures ++;
            System.out.println("Unequal sums after calling precision!");
            System.out.print("Before:\t");
            printAddition(a, BigDecimal.ONE, sum1.toString());

            System.out.print("After:\t");
            printAddition(a, BigDecimal.ONE, sum2.toString());
        }

        return failures;
    }

    private static int arithmeticExceptionTest() {
        int failures = 0;
        BigDecimal x;
        try {
            //
            // The string representation "1e2147483647", which is equivalent
            // to 10^Integer.MAX_VALUE, is used to create an augend with an
            // unscaled value of 1 and a scale of -Integer.MAX_VALUE. The
            // addend "1" has an unscaled value of 1 with a scale of 0. The
            // addition is performed exactly and is specified to have a
            // preferred scale of max(-Integer.MAX_VALUE, 0). As the scale
            // of the result is 0, a value with Integer.MAX_VALUE + 1 digits
            // would need to be created. Therefore the next statement is
            // expected to overflow with an ArithmeticException.
            //
            x = new BigDecimal("1e2147483647").add(new BigDecimal(1));
            failures++;
        } catch (ArithmeticException ae) {
        }
        return failures;
    }

    public static void main(String argv[]) {
        int failures = 0;

        failures += extremaTests();
        failures += roundingGradationTests();
        failures += precisionConsistencyTest();
        failures += arithmeticExceptionTest();

        if (failures > 0) {
            throw new RuntimeException("Incurred " + failures +
                                       " failures while testing rounding add.");
        }
    }
}
