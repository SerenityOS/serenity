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
 * @run main ParseHexFloatingPoint
 * @bug 4826774 8078672
 * @summary Numerical tests for hexadecimal inputs to parse{Double, Float} (use -Dseed=X to set PRNG seed)
 * @author Joseph D. Darcy
 * @key randomness
 */

import jdk.test.lib.RandomFactory;

public class ParseHexFloatingPoint {
    private ParseHexFloatingPoint(){}

    public static final double infinityD = Double.POSITIVE_INFINITY;
    public static final double NaND = Double.NaN;

    static int test(String testName, String input,
                    double result, double expected) {
        int failures =0;

        if (Double.compare(result, expected) != 0 ) {
            System.err.println("Failure for " + testName +
                               ": For input " + input +
                               " expected " + expected +
                               " got " + result + ".");
        }

        return failures;
    }

    static int testCase(String input, double expected) {
        int failures =0;


        // Try different combination of letter components
        input = input.toLowerCase(java.util.Locale.US);

        String [] suffices = {"", "f", "F", "d", "D"};
        String [] signs = {"", "-", "+"};

        for(int i = 0; i < 2; i++) {
            String s1 = input;
            if(i == 1)
                s1 = s1.replace('x', 'X');

            for(int j = 0; j < 2; j++) {
                String s2 = s1;
                if(j == 1)
                    s2 = s2.replace('p', 'P');

                for(int k = 0; k < 2; k++) {
                    String s3 = s2;
                    if(k == 1)
                        s3 = upperCaseHex(s3);


                    for(int m = 0; m < suffices.length; m++) {
                        String s4 = s3 + suffices[m];


                        for(int n = 0; n < signs.length; n++) {
                            String s5 = signs[n] + s4;

                            double result = Double.parseDouble(s5);
                            failures += test("Double.parseDouble",
                                             s5, result, (signs[n].equals("-") ?
                                                          -expected:
                                                          expected));
                        }
                    }
                }
            }
        }

        return failures;
    }

    static String upperCaseHex(String s) {
        return s.replace('a', 'A').replace('b', 'B').replace('c', 'C').
                 replace('d', 'D').replace('e','E').replace('f', 'F');
    }

    /*
     * Test easy and tricky double rounding cases.
     */
    static int doubleTests() {

        /*
         * A String, double pair
         */
        class PairSD {
            public String s;
            public double d;
            PairSD(String s, double d) {
                this.s = s;
                this.d = d;
            }
        }
        int failures = 0;



        // Hex strings that convert to three; test basic functionality
        // of significand and exponent shift adjusts along with the
        // no-op of adding leading zeros.  These cases don't exercise
        // the rounding code.
        String leadingZeros = "0x0000000000000000000";
        String [] threeTests = {
            "0x.003p12",
            "0x.006p11",
            "0x.00cp10",
            "0x.018p9",

            "0x.3p4",
            "0x.6p3",
            "0x.cp2",
            "0x1.8p1",

            "0x3p0",
            "0x6.0p-1",
            "0xc.0p-2",
            "0x18.0p-3",

            "0x3000000p-24",
            "0x3.0p0",
            "0x3.000000p0",
        };
        for(int i=0; i < threeTests.length; i++) {
            String input = threeTests[i];
            failures += testCase(input, 3.0);

            input.replaceFirst("^0x", leadingZeros);
            failures += testCase(input, 3.0);
        }

        long bigExponents [] = {
            2*Double.MAX_EXPONENT,
            2*Double.MIN_EXPONENT,

            (long)Integer.MAX_VALUE-1,
            (long)Integer.MAX_VALUE,
            (long)Integer.MAX_VALUE+1,

            (long)Integer.MIN_VALUE-1,
            (long)Integer.MIN_VALUE,
            (long)Integer.MIN_VALUE+1,

            Long.MAX_VALUE-1,
            Long.MAX_VALUE,

            Long.MIN_VALUE+1,
            Long.MIN_VALUE,
        };

        // Test zero significand with large exponents.
        for(int i = 0; i < bigExponents.length; i++) {
            failures += testCase("0x0.0p"+Long.toString(bigExponents[i]) , 0.0);
        }

        // Test nonzero significand with large exponents.
        for(int i = 0; i < bigExponents.length; i++) {
            long exponent = bigExponents[i];
            failures += testCase("0x10000.0p"+Long.toString(exponent) ,
                                 (exponent <0?0.0:infinityD));
        }

        // Test significands with different lengths and bit patterns.
        {
            long signif = 0;
                for(int i = 1; i <= 0xe; i++) {
                    signif = (signif <<4) | (long)i;
                    failures += testCase("0x"+Long.toHexString(signif)+"p0", signif);
                }
        }

        PairSD [] testCases = {
            new PairSD("0x0.0p0",               0.0/16.0),
            new PairSD("0x0.1p0",               1.0/16.0),
            new PairSD("0x0.2p0",               2.0/16.0),
            new PairSD("0x0.3p0",               3.0/16.0),
            new PairSD("0x0.4p0",               4.0/16.0),
            new PairSD("0x0.5p0",               5.0/16.0),
            new PairSD("0x0.6p0",               6.0/16.0),
            new PairSD("0x0.7p0",               7.0/16.0),
            new PairSD("0x0.8p0",               8.0/16.0),
            new PairSD("0x0.9p0",               9.0/16.0),
            new PairSD("0x0.ap0",               10.0/16.0),
            new PairSD("0x0.bp0",               11.0/16.0),
            new PairSD("0x0.cp0",               12.0/16.0),
            new PairSD("0x0.dp0",               13.0/16.0),
            new PairSD("0x0.ep0",               14.0/16.0),
            new PairSD("0x0.fp0",               15.0/16.0),

            // Half-way case between zero and MIN_VALUE rounds down to
            // zero
            new PairSD("0x1.0p-1075",           0.0),

            // Slighly more than half-way case between zero and
            // MIN_VALUES rounds up to zero.
            new PairSD("0x1.1p-1075",                   Double.MIN_VALUE),
            new PairSD("0x1.000000000001p-1075",        Double.MIN_VALUE),
            new PairSD("0x1.000000000000001p-1075",     Double.MIN_VALUE),

            // More subnormal rounding tests
            new PairSD("0x0.fffffffffffff7fffffp-1022", Math.nextDown(Double.MIN_NORMAL)),
            new PairSD("0x0.fffffffffffff8p-1022",      Double.MIN_NORMAL),
            new PairSD("0x0.fffffffffffff800000001p-1022",Double.MIN_NORMAL),
            new PairSD("0x0.fffffffffffff80000000000000001p-1022",Double.MIN_NORMAL),
            new PairSD("0x1.0p-1022",                   Double.MIN_NORMAL),


            // Large value and overflow rounding tests
            new PairSD("0x1.fffffffffffffp1023",        Double.MAX_VALUE),
            new PairSD("0x1.fffffffffffff0000000p1023", Double.MAX_VALUE),
            new PairSD("0x1.fffffffffffff4p1023",       Double.MAX_VALUE),
            new PairSD("0x1.fffffffffffff7fffffp1023",  Double.MAX_VALUE),
            new PairSD("0x1.fffffffffffff8p1023",       infinityD),
            new PairSD("0x1.fffffffffffff8000001p1023", infinityD),

            new PairSD("0x1.ffffffffffffep1023",        Math.nextDown(Double.MAX_VALUE)),
            new PairSD("0x1.ffffffffffffe0000p1023",    Math.nextDown(Double.MAX_VALUE)),
            new PairSD("0x1.ffffffffffffe8p1023",       Math.nextDown(Double.MAX_VALUE)),
            new PairSD("0x1.ffffffffffffe7p1023",       Math.nextDown(Double.MAX_VALUE)),
            new PairSD("0x1.ffffffffffffeffffffp1023",  Double.MAX_VALUE),
            new PairSD("0x1.ffffffffffffe8000001p1023", Double.MAX_VALUE),
        };

        for (int i = 0; i < testCases.length; i++) {
            failures += testCase(testCases[i].s,testCases[i].d);
        }

        failures += significandAlignmentTests();

        {
            java.util.Random rand = RandomFactory.getRandom();
            // Consistency check; double => hexadecimal => double
            // preserves the original value.
            for(int i = 0; i < 1000; i++) {
                double d = rand.nextDouble();
                failures += testCase(Double.toHexString(d), d);
            }
        }

        return failures;
    }

    /*
     * Verify rounding works the same regardless of how the
     * significand is aligned on input.  A useful extension could be
     * to have this sort of test for strings near the overflow
     * threshold.
     */
    static int significandAlignmentTests() {
        int failures = 0;
                // baseSignif * 2^baseExp = nextDown(2.0)
        long [] baseSignifs = {
            0x1ffffffffffffe00L,
            0x1fffffffffffff00L
        };

        double [] answers = {
            Math.nextDown(Math.nextDown(2.0)),
            Math.nextDown(2.0),
            2.0
        };

        int baseExp = -60;
        int count = 0;
        for(int i = 0; i < 2; i++) {
            for(long j = 0; j <= 0xfL; j++) {
                for(long k = 0; k <= 8; k+= 4) { // k = {0, 4, 8}
                    long base = baseSignifs[i];
                    long testValue = base | (j<<4) | k;

                    int offset = 0;
                    // Calculate when significand should be incremented
                    // see table 4.7 in Koren book

                    if ((base & 0x100L) == 0L ) { // lsb is 0
                        if ( (j >= 8L) &&         // round is 1
                             ((j & 0x7L) != 0 || k != 0 ) ) // sticky is 1
                            offset = 1;
                    }
                    else {                        // lsb is 1
                        if (j >= 8L)              // round is 1
                            offset = 1;
                    }

                    double expected = answers[i+offset];

                    for(int m = -2; m <= 3; m++) {
                        count ++;

                        // Form equal value string and evaluate it
                        String s = "0x" +
                            Long.toHexString((m >=0) ?(testValue<<m):(testValue>>(-m))) +
                            "p" + (baseExp - m);

                        failures += testCase(s, expected);
                    }
                }
            }
        }

        return failures;
    }


    /*
     * Test tricky float rounding cases.  The code which
     * reads in a hex string converts the string to a double value.
     * If a float value is needed, the double value is cast to float.
     * However, the cast be itself not always guaranteed to return the
     * right result since:
     *
     * 1. hex string => double can discard a sticky bit which would
     * influence a direct hex string => float conversion.
     *
     * 2. hex string => double => float can have a rounding to double
     * precision which results in a larger float value while a direct
     * hex string => float conversion would not round up.
     *
     * This method includes tests of the latter two possibilities.
     */
    static int floatTests(){
        int failures = 0;

        /*
         * A String, float pair
         */
        class PairSD {
            public String s;
            public float f;
            PairSD(String s, float f) {
                this.s = s;
                this.f = f;
            }
        }

        String [][] roundingTestCases = {
            // Target float value       hard rouding version

            {"0x1.000000p0",    "0x1.0000000000001p0"},

            // Try some values that should round up to nextUp(1.0f)
            {"0x1.000002p0",    "0x1.0000010000001p0"},
            {"0x1.000002p0",    "0x1.00000100000008p0"},
            {"0x1.000002p0",    "0x1.0000010000000fp0"},
            {"0x1.000002p0",    "0x1.00000100000001p0"},
            {"0x1.000002p0",    "0x1.00000100000000000000000000000000000000001p0"},
            {"0x1.000002p0",    "0x1.0000010000000fp0"},

            // Potential double rounding cases
            {"0x1.000002p0",    "0x1.000002fffffffp0"},
            {"0x1.000002p0",    "0x1.000002fffffff8p0"},
            {"0x1.000002p0",    "0x1.000002ffffffffp0"},

            {"0x1.000002p0",    "0x1.000002ffff0ffp0"},
            {"0x1.000002p0",    "0x1.000002ffff0ff8p0"},
            {"0x1.000002p0",    "0x1.000002ffff0fffp0"},


            {"0x1.000000p0",    "0x1.000000fffffffp0"},
            {"0x1.000000p0",    "0x1.000000fffffff8p0"},
            {"0x1.000000p0",    "0x1.000000ffffffffp0"},

            {"0x1.000000p0",    "0x1.000000ffffffep0"},
            {"0x1.000000p0",    "0x1.000000ffffffe8p0"},
            {"0x1.000000p0",    "0x1.000000ffffffefp0"},

            // Float subnormal cases
            {"0x0.000002p-126", "0x0.0000010000001p-126"},
            {"0x0.000002p-126", "0x0.00000100000000000001p-126"},

            {"0x0.000006p-126", "0x0.0000050000001p-126"},
            {"0x0.000006p-126", "0x0.00000500000000000001p-126"},

            {"0x0.0p-149",      "0x0.7ffffffffffffffp-149"},
            {"0x1.0p-148",      "0x1.3ffffffffffffffp-148"},
            {"0x1.cp-147",      "0x1.bffffffffffffffp-147"},

            {"0x1.fffffcp-127", "0x1.fffffdffffffffp-127"},
        };

        String [] signs = {"", "-"};

        for(int i = 0; i < roundingTestCases.length; i++) {
            for(int j = 0; j < signs.length; j++) {
                String expectedIn = signs[j]+roundingTestCases[i][0];
                String resultIn   = signs[j]+roundingTestCases[i][1];

                float expected =  Float.parseFloat(expectedIn);
                float result   =  Float.parseFloat(resultIn);

                if( Float.compare(expected, result) != 0) {
                    failures += 1;
                    System.err.println("" + (i+1));
                    System.err.println("Expected = " + Float.toHexString(expected));
                    System.err.println("Rounded  = " + Float.toHexString(result));
                    System.err.println("Double   = " + Double.toHexString(Double.parseDouble(resultIn)));
                    System.err.println("Input    = " + resultIn);
                    System.err.println("");
                }
            }
        }

        return failures;
    }

    public static void main(String argv[]) {
        int failures = 0;

        failures += doubleTests();
        failures += floatTests();

        if (failures != 0) {
            throw new RuntimeException("" + failures + " failures while " +
                                       "testing hexadecimal floating-point " +
                                       "parsing.");
        }
    }

}
