/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4904082 4917089 6337226 6378503
 * @summary Tests that integral division and related methods return the proper result and scale.
 * @author Joseph D. Darcy
 */
import java.math.*;
public class IntegralDivisionTests {

    static int dividetoIntegralValueTests() {
        int failures = 0;

        // Exact integer quotient should have the same results from
        // the exact divide and dividetoIntegralValue


        // Rounded results
        BigDecimal [][] moreTestCases = {
            {new BigDecimal("11003"),   new BigDecimal("10"),   new BigDecimal("1100")},
            {new BigDecimal("11003"),   new BigDecimal("1e1"),  new BigDecimal("1100.0")},
            {new BigDecimal("1e9"),     new BigDecimal("1"),    new BigDecimal("1e9")},
            {new BigDecimal("1e9"),     new BigDecimal("1.00"), new BigDecimal("1e9")},
            {new BigDecimal("1e9"),     new BigDecimal("0.1"),  new BigDecimal("1e10")},
            {new BigDecimal("10e8"),    new BigDecimal("0.1"),  new BigDecimal("10e9")},
            {new BigDecimal("400e1"),   new BigDecimal("5"),    new BigDecimal("80e1")},
            {new BigDecimal("400e1"),   new BigDecimal("4.999999999"),  new BigDecimal("8e2")},
            {new BigDecimal("40e2"),    new BigDecimal("5"),    new BigDecimal("8e2")},
            {BigDecimal.valueOf(1, Integer.MIN_VALUE),
                BigDecimal.valueOf(1, -(Integer.MAX_VALUE & 0x7fffff00)),
                BigDecimal.valueOf(1, -256)},
        };

        for(BigDecimal [] testCase: moreTestCases) {
            BigDecimal quotient;
            if (! (quotient=testCase[0].divideToIntegralValue(testCase[1])).equals(testCase[2]) ){
                failures++;
                // BigDecimal exact = testCase[0].divide(testCase[1]);
                System.err.println();
                System.err.println("dividend  = " + testCase[0] + " scale = " + testCase[0].scale());
                System.err.println("divisor   = " + testCase[1] + " scale = " + testCase[1].scale());
                System.err.println("quotient  = " + quotient    + " scale = " + quotient.scale());
                System.err.println("expected  = " + testCase[2] + " scale = " + testCase[2].scale());
                // System.err.println("exact     = " + exact       + " scale = " + exact.scale());
            }
        }

        return failures;
    }

    static int dividetoIntegralValueRoundedTests() {
        int failures = 0;

        BigDecimal dividend = new BigDecimal("11003");
        BigDecimal divisor = new BigDecimal("10");
        BigDecimal [] quotients = {     // Expected results with precision =
            new BigDecimal("1100"),     // 0
            null,                       // 1
            new BigDecimal("11e2"),     // 2
            new BigDecimal("110e1"),    // 3
            new BigDecimal("1100"),     // 4
        };
        failures += divideContextTestPrecs(dividend, divisor, quotients);

        dividend = new BigDecimal("11003");
        divisor = new BigDecimal("1e1");
        BigDecimal [] quotients2 = {    // Expected results with precision =
            new BigDecimal("1100.0"),   // 0
            null,                       // 1
            new BigDecimal("11e2"),     // 2
            new BigDecimal("110e1"),    // 3
            new BigDecimal("1100"),     // 4
            new BigDecimal("1100.0"),   // 5
        };
        failures += divideContextTestPrecs(dividend, divisor, quotients2);

        dividend = new BigDecimal("1230000");
        divisor = new BigDecimal("100");
        BigDecimal [] quotients3 = {    // Expected results with precision =
            new BigDecimal("12300"),    // 0
            null,                       // 1
            null,                       // 2
            new BigDecimal("123e2"),    // 3
            new BigDecimal("1230e1"),   // 4
            new BigDecimal("12300"),    // 5
        };
        failures += divideContextTestPrecs(dividend, divisor, quotients3);

        dividend = new BigDecimal("33");
        divisor  = new BigDecimal("3");
        BigDecimal [] quotients4 = {    // Expected results with precision =
            new BigDecimal("11"),       // 0
            null,                       // 1
            new BigDecimal("11"),       // 2
            new BigDecimal("11"),       // 3
        };
        failures += divideContextTestPrecs(dividend, divisor, quotients4);

        dividend = new BigDecimal("34");
        divisor  = new BigDecimal("3");
        BigDecimal [] quotients5 = {    // Expected results with precision =
            new BigDecimal("11"),       // 0
            null,                       // 1
            new BigDecimal("11"),       // 2
            new BigDecimal("11"),       // 3
        };
        failures += divideContextTestPrecs(dividend, divisor, quotients5);

        return failures;
    }

    static int divideContextTestPrecs(BigDecimal dividend,
                                      BigDecimal divisor,
                                      BigDecimal[] quotients)
    {
        int failures = 0;
        for(int i = 0; i < quotients.length; i++) {
            BigDecimal result = null;
            BigDecimal quotient = quotients[i];

            try {
                result = dividend.divideToIntegralValue(divisor,
                                                        new MathContext(i, RoundingMode.DOWN));
            } catch (ArithmeticException e) {
                if (quotient != null) {
                    failures++;
                    System.err.println();
                    System.err.println("Unexpected exception:");
                    System.err.println("dividend  = " + dividend     + " scale = " + dividend.scale());
                    System.err.println("divisor   = " + divisor      + " scale = " + divisor.scale());
                    System.err.println("expected  = " + quotient     + " scale = " + quotient.scale());
                }
            }

            if (quotient != null) {
                if (! result.equals(quotient)) {
                    failures++;
                    System.err.println();
                    System.err.println("Unexpected result:");
                    System.err.println("dividend  = " + dividend     + " scale = " + dividend.scale());
                    System.err.println("divisor   = " + divisor      + " scale = " + divisor.scale());
                    System.err.println("quotient  = " + result       + " scale = " + result.scale());
                    System.err.println("expected  = " + quotient     + " scale = " + quotient.scale());
                    System.err.println("precision = " + i);
                }
            } else {
                if (result != null) {
                    failures++;
                    System.err.println();
                    System.err.println("Unexpected unexceptional result:");
                    System.err.println("dividend  = " + dividend     + " scale = " + dividend.scale());
                    System.err.println("divisor   = " + divisor      + " scale = " + divisor.scale());
                    System.err.println("quotient  = " + result       + " scale = " + result.scale());
                    System.err.println("precision = " + i);
                }
            }

        }
        return failures;
    }


    static int divideContextTests(BigDecimal dividend,
                                  BigDecimal divisor,
                                  BigDecimal expected,
                                  MathContext mc) {
        int failures = 0;

        failures += divideContextTest(dividend,              divisor,          expected,                mc);
        failures += divideContextTest(dividend.negate(),     divisor.negate(), expected,                mc);

        if (expected != null) {
            failures += divideContextTest(dividend.negate(), divisor,          expected.negate(),       mc);
            failures += divideContextTest(dividend,          divisor.negate(), expected.negate(),       mc);
        }

        return failures;
    }


    static int divideContextTest(BigDecimal dividend,
                                 BigDecimal divisor,
                                 BigDecimal expected,
                                 MathContext mc)
    {
        int failures = 0;

        BigDecimal result = null;

        try {
            result = dividend.divideToIntegralValue(divisor, mc);
        } catch (ArithmeticException e) {
            if (expected != null) {
                failures++;
                System.err.println();
                System.err.println("Unexpected exception:");
                System.err.println("dividend  = " + dividend     + " scale = " + dividend.scale());
                System.err.println("divisor   = " + divisor      + " scale = " + divisor.scale());
                System.err.println("expected  = " + expected     + " scale = " + expected.scale());
                System.err.println("MathContext  = " + mc);
            }
        }

        if (expected != null) {
            if (! result.equals(expected)) {
                failures++;
                System.err.println();
                System.err.println("Unexpected result:");
                System.err.println("dividend  = " + dividend     + " scale = " + dividend.scale());
                System.err.println("divisor   = " + divisor      + " scale = " + divisor.scale());
                System.err.println("expected  = " + expected     + " scale = " + expected.scale());
                System.err.println("result    = " + result       + " scale = " + result.scale());
                System.err.println("MathContext  = " + mc);
            }
        } else {
            if (result != null) {
                failures++;
                System.err.println();
                System.err.println("Unexpected unexceptional result:");
                System.err.println("dividend  = " + dividend     + " scale = " + dividend.scale());
                System.err.println("divisor   = " + divisor      + " scale = " + divisor.scale());
                System.err.println("quotient  = " + result       + " scale = " + result.scale());
                System.err.println("MathConext = " + mc);
                }
        }

        return failures;
    }

    static int dividetoIntegralValueScalingTests() {
        int failures = 0;

        BigDecimal dividend = new BigDecimal("123456789000");
        BigDecimal divisor = BigDecimal.ONE;
        BigDecimal expected = new BigDecimal("123456789e3");
        MathContext mc = new MathContext(9,RoundingMode.DOWN);
        failures += divideContextTests(dividend, divisor, expected, mc);


        // 100/3 = 33 remainder 1
        int [] precisions = {0, 2, 3, 4};
        dividend = new BigDecimal(100);
        divisor  = new BigDecimal(3);
        expected = new BigDecimal(33);

        for(RoundingMode rm: RoundingMode.values())
            for(int precision: precisions) {
                failures += divideContextTests(dividend, divisor, expected,
                                               new MathContext(precision, rm));
            }

        // 123000/10 = 12300 remainder 0
        dividend = new BigDecimal(123000);
        divisor  = new BigDecimal(10);
        int[] precisions1 = {0, 1, 2, 3, 4, 5};
        BigDecimal[] expected1 = {
            new BigDecimal("12300"),
            null,
            null,
            new BigDecimal("123e2"),
            new BigDecimal("1230e1"),
            new BigDecimal("12300"),
        };

        for(RoundingMode rm: RoundingMode.values())
            for(int i = 0; i < precisions1.length; i++) {
                failures += divideContextTests(dividend, divisor,
                                               expected1[i],
                                               new MathContext(precisions1[i], rm));
            }

        // 123e3/10 = 123e2 remainder 0
        dividend = new BigDecimal("123e3");
        divisor  = new BigDecimal(10);
        int[] precisions2 = {0, 1, 2, 3, 4, 5};
        BigDecimal[] expected2 = {
            new BigDecimal("123e2"),
            null,
            null,
            new BigDecimal("123e2"),
            new BigDecimal("123e2"),
            new BigDecimal("123e2"),
        };

        for(RoundingMode rm: RoundingMode.values())
            for(int i = 0; i < precisions2.length; i++) {
                failures += divideContextTests(dividend, divisor,
                                               expected2[i],
                                               new MathContext(precisions2[i], rm));
            }


        // 123000/1e1 = 12300.0 remainder 0
        dividend = new BigDecimal("123000");
        divisor  = new BigDecimal("1e1");
        int[] precisions3 = {0, 1, 2, 3, 4, 5, 6};
        BigDecimal[] expected3 = {
            new BigDecimal("12300.0"),
            null,
            null,
            new BigDecimal("123e2"),
            new BigDecimal("1230e1"),
            new BigDecimal("12300"),
            new BigDecimal("12300.0"),
        };

        for(RoundingMode rm: RoundingMode.values())
            for(int i = 0; i < precisions3.length; i++) {
                failures += divideContextTests(dividend, divisor,
                                               expected3[i],
                                               new MathContext(precisions3[i], rm));
            }



        return failures;
    }

    public static void main(String argv[]) {
        int failures = 0;

        failures += dividetoIntegralValueTests();
        failures += dividetoIntegralValueRoundedTests();
        failures += dividetoIntegralValueScalingTests();

        if (failures > 0) {
            System.err.println("Encountered " + failures +
                               " failures while testing integral division.");
            throw new RuntimeException();
        }
    }
}
