/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4160406 4705734 4707389 6358355 7032154
 * @summary Tests for Float.parseFloat method
 */

import java.math.BigDecimal;
import java.math.BigInteger;

public class ParseFloat {

    private static final BigDecimal HALF = BigDecimal.valueOf(0.5);

    private static void fail(String val, float n) {
        throw new RuntimeException("Float.parseFloat failed. String:" +
                                                val + " Result:" + n);
    }

    private static void check(String val) {
        float n = Float.parseFloat(val);
        boolean isNegativeN = n < 0 || n == 0 && 1/n < 0;
        float na = Math.abs(n);
        String s = val.trim().toLowerCase();
        switch (s.charAt(s.length() - 1)) {
            case 'd':
            case 'f':
                s = s.substring(0, s.length() - 1);
                break;
        }
        boolean isNegative = false;
        if (s.charAt(0) == '+') {
            s = s.substring(1);
        } else if (s.charAt(0) == '-') {
            s = s.substring(1);
            isNegative = true;
        }
        if (s.equals("nan")) {
            if (!Float.isNaN(n)) {
                fail(val, n);
            }
            return;
        }
        if (Float.isNaN(n)) {
            fail(val, n);
        }
        if (isNegativeN != isNegative)
            fail(val, n);
        if (s.equals("infinity")) {
            if (na != Float.POSITIVE_INFINITY) {
                fail(val, n);
            }
            return;
        }
        BigDecimal bd;
        if (s.startsWith("0x")) {
            s = s.substring(2);
            int indP = s.indexOf('p');
            long exp = Long.parseLong(s.substring(indP + 1));
            int indD = s.indexOf('.');
            String significand;
            if (indD >= 0) {
                significand = s.substring(0, indD) + s.substring(indD + 1, indP);
                exp -= 4*(indP - indD - 1);
            } else {
                significand = s.substring(0, indP);
            }
            bd = new BigDecimal(new BigInteger(significand, 16));
            if (exp >= 0) {
                bd = bd.multiply(BigDecimal.valueOf(2).pow((int)exp));
            } else {
                bd = bd.divide(BigDecimal.valueOf(2).pow((int)-exp));
            }
        } else {
            bd = new BigDecimal(s);
        }
        BigDecimal l, u;
        if (Float.isInfinite(na)) {
            l = new BigDecimal(Float.MAX_VALUE).add(new BigDecimal(Math.ulp(Float.MAX_VALUE)).multiply(HALF));
            u = null;
        } else {
            l = new BigDecimal(na).subtract(new BigDecimal(Math.ulp(-Math.nextUp(-na))).multiply(HALF));
            u = new BigDecimal(na).add(new BigDecimal(Math.ulp(n)).multiply(HALF));
        }
        int cmpL = bd.compareTo(l);
        int cmpU = u != null ? bd.compareTo(u) : -1;
        if ((Float.floatToIntBits(n) & 1) != 0) {
            if (cmpL <= 0 || cmpU >= 0) {
                fail(val, n);
            }
        } else {
            if (cmpL < 0 || cmpU > 0) {
                fail(val, n);
            }
        }
    }

    private static void check(String val, float expected) {
        float n = Float.parseFloat(val);
        if (n != expected)
            fail(val, n);
        check(val);
    }

    private static void rudimentaryTest() {
        check(new String(""+Float.MIN_VALUE), Float.MIN_VALUE);
        check(new String(""+Float.MAX_VALUE), Float.MAX_VALUE);

        check("10",     (float)  10.0);
        check("10.0",   (float)  10.0);
        check("10.01",  (float)  10.01);

        check("-10",    (float) -10.0);
        check("-10.00", (float) -10.0);
        check("-10.01", (float) -10.01);

        // bug 6358355
        check("144115196665790480", 0x1.000002p57f);
        check("144115196665790481", 0x1.000002p57f);
        check("0.050000002607703203", 0.05f);
        check("0.050000002607703204", 0.05f);
        check("0.050000002607703205", 0.05f);
        check("0.050000002607703206", 0.05f);
        check("0.050000002607703207", 0.05f);
        check("0.050000002607703208", 0.05f);
        check("0.050000002607703209", 0.050000004f);
    }

    static  String badStrings[] = {
        "",
        "+",
        "-",
        "+e",
        "-e",
        "+e170",
        "-e170",

        // Make sure intermediate white space is not deleted.
        "1234   e10",
        "-1234   e10",

        // Control characters in the interior of a string are not legal
        "1\u0007e1",
        "1e\u00071",

        // NaN and infinity can't have trailing type suffices or exponents
        "NaNf",
        "NaNF",
        "NaNd",
        "NaND",
        "-NaNf",
        "-NaNF",
        "-NaNd",
        "-NaND",
        "+NaNf",
        "+NaNF",
        "+NaNd",
        "+NaND",
        "Infinityf",
        "InfinityF",
        "Infinityd",
        "InfinityD",
        "-Infinityf",
        "-InfinityF",
        "-Infinityd",
        "-InfinityD",
        "+Infinityf",
        "+InfinityF",
        "+Infinityd",
        "+InfinityD",

        "NaNe10",
        "-NaNe10",
        "+NaNe10",
        "Infinitye10",
        "-Infinitye10",
        "+Infinitye10",

        // Non-ASCII digits are not recognized
        "\u0661e\u0661", // 1e1 in Arabic-Indic digits
        "\u06F1e\u06F1", // 1e1 in Extended Arabic-Indic digits
        "\u0967e\u0967" // 1e1 in Devanagari digits
    };

    static String goodStrings[] = {
        "NaN",
        "+NaN",
        "-NaN",
        "Infinity",
        "+Infinity",
        "-Infinity",
        "1.1e-23f",
        ".1e-23f",
        "1e-23",
        "1f",
        "1",
        "2",
        "1234",
        "-1234",
        "+1234",
        "2147483647",   // Integer.MAX_VALUE
        "2147483648",
        "-2147483648",  // Integer.MIN_VALUE
        "-2147483649",

        "16777215",
        "16777216",     // 2^24
        "16777217",

        "-16777215",
        "-16777216",    // -2^24
        "-16777217",

        "9007199254740991",
        "9007199254740992",     // 2^53
        "9007199254740993",

        "-9007199254740991",
        "-9007199254740992",    // -2^53
        "-9007199254740993",

        "9223372036854775807",
        "9223372036854775808",  // Long.MAX_VALUE
        "9223372036854775809",

        "-9223372036854775808",
        "-9223372036854775809", // Long.MIN_VALUE
        "-9223372036854775810"
    };

    static String paddedBadStrings[];
    static String paddedGoodStrings[];
    static {
        String pad = " \t\n\r\f\u0001\u000b\u001f";
        paddedBadStrings = new String[badStrings.length];
        for(int i = 0 ; i <  badStrings.length; i++)
            paddedBadStrings[i] = pad + badStrings[i] + pad;

        paddedGoodStrings = new String[goodStrings.length];
        for(int i = 0 ; i <  goodStrings.length; i++)
            paddedGoodStrings[i] = pad + goodStrings[i] + pad;

    }

    /*
     * Throws an exception if <code>Input</code> is
     * <code>exceptionalInput</code> and {@link Float.parseFloat
     * parseFloat} does <em>not</em> throw an exception or if
     * <code>Input</code> is not <code>exceptionalInput</code> and
     * <code>parseFloat</code> throws an exception.  This method does
     * not attempt to test whether the string is converted to the
     * proper value; just whether the input is accepted appropriately
     * or not.
     */
    private static void testParsing(String [] input,
                                    boolean exceptionalInput) {
        for(int i = 0; i < input.length; i++) {
            double d;

            try {
                d = Float.parseFloat(input[i]);
                check(input[i]);
            }
            catch (NumberFormatException e) {
                if (! exceptionalInput) {
                    throw new RuntimeException("Float.parseFloat rejected " +
                                               "good string `" + input[i] +
                                               "'.");
                }
                break;
            }
            if (exceptionalInput) {
                throw new RuntimeException("Float.parseFloat accepted " +
                                           "bad string `" + input[i] +
                                           "'.");
            }
        }
    }

    /**
     * For each power of two, test at boundaries of
     * region that should convert to that value.
     */
    private static void testPowers() {
        for(int i = -149; i <= +127; i++) {
            float f = Math.scalb(1.0f, i);
            BigDecimal f_BD = new BigDecimal(f);

            BigDecimal lowerBound = f_BD.subtract(new BigDecimal(Math.ulp(-Math.nextUp(-f))).multiply(HALF));
            BigDecimal upperBound = f_BD.add(new BigDecimal(Math.ulp(f)).multiply(HALF));

            check(lowerBound.toString());
            check(upperBound.toString());
        }
        check(new BigDecimal(Float.MAX_VALUE).add(new BigDecimal(Math.ulp(Float.MAX_VALUE)).multiply(HALF)).toString());
    }

    public static void main(String[] args) throws Exception {
        rudimentaryTest();

        testParsing(goodStrings, false);
        testParsing(paddedGoodStrings, false);
        testParsing(badStrings, true);
        testParsing(paddedBadStrings, true);

        testPowers();
    }
}
