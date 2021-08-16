/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Confirm that the negative multiplier works as expected.
 * @bug 4833877
 */

import java.math.*;
import java.text.*;
import java.util.*;

public class Bug4833877 {

    static DecimalFormat df;

    static boolean err = false;

    public static void main(String[] args) throws Exception {

        Locale defaultLoc = Locale.getDefault();
        Locale.setDefault(Locale.US);

        /* ================================================================ */

        df = new DecimalFormat();
        df.setMaximumFractionDigits(50);
        df.setMultiplier(4);

        /*
         * Test for double/Double
         */
        checkFormat(252.5252525252525, "1,010.10101010101");
        checkParse("-1,010.10101010101", -252.5252525252525);

        checkFormat(-2222.2222, "-8,888.8888");
        checkParse("8888.8888", 2222.2222);

        /*
         * Test for long/Long
         */
        checkFormat(1000L, "4,000");
        checkParse("-4,000", -1000L);

        checkFormat(-250L, "-1,000");
        checkParse("1000", 250L);

        /* ---------------------------------------------------------------- */

        df.setParseBigDecimal(true);

        /*
         * Test for BigDecimal
         */
        checkFormat(new BigDecimal("22222.222222222222222222222"),
                    "88,888.888888888888888888888");
        checkParse("-88,888.888888888888888888888",
                    new BigDecimal("-22222.222222222222222222222"));

        checkFormat(new BigDecimal("-1111111111111111111.111111111111111111"),
                    "-4,444,444,444,444,444,444.444444444444444444");
        checkParse("4444444444444444444.444444444444444444",
                    new BigDecimal("1111111111111111111.111111111111111111"));

        /*
         * Test for BigInteger
         */
        checkFormat(new BigInteger("22222222222222222222222222"),
                    "88,888,888,888,888,888,888,888,888");
        checkParse("-88,888,888,888,888,888,888,888,888",
                    new BigDecimal("-22222222222222222222222222"));

        checkFormat(new BigInteger("-1111111111111111111111111"),
                    "-4,444,444,444,444,444,444,444,444");
        checkParse("4444444444444444444444444",
                    new BigDecimal("1111111111111111111111111"));

        /* ---------------------------------------------------------------- */

        df.setParseBigDecimal(false);
        df.setMultiplier(-4);

        /*
         * Test for double/Double
         */
        checkFormat(252.5252525252525, "-1,010.10101010101");
        checkParse("-1,010.10101010101", 252.5252525252525);

        checkFormat(-2222.2222, "8,888.8888");
        checkParse("8888.8888", -2222.2222);

        /*
         * Test for long/Long
         */
        checkFormat(1000L, "-4,000");
        checkParse("-4,000", 1000L);

        checkFormat(-250L, "1,000");
        checkParse("1000", -250L);

        /* ---------------------------------------------------------------- */

        df.setParseBigDecimal(true);

        /*
         * Test for BigDecimal
         */
        checkFormat(new BigDecimal("22222.222222222222222222222"),
                    "-88,888.888888888888888888888");
        checkParse("-88,888.888888888888888888888",
                    new BigDecimal("22222.222222222222222222222"));

        checkFormat(new BigDecimal("-1111111111111111111.111111111111111111"),
                   "4,444,444,444,444,444,444.444444444444444444");
        checkParse("4444444444444444444.444444444444444444",
                    new BigDecimal("-1111111111111111111.111111111111111111"));

        /*
         * Test for BigInteger
         */
        checkFormat(new BigInteger("22222222222222222222222222"),
                    "-88,888,888,888,888,888,888,888,888");
        checkParse("-88,888,888,888,888,888,888,888,888",
                    new BigDecimal("22222222222222222222222222"));

        checkFormat(new BigInteger("-1111111111111111111111111"),
                   "4,444,444,444,444,444,444,444,444");
        checkParse("4444444444444444444444444",
                    new BigDecimal("-1111111111111111111111111"));

        /* ---------------------------------------------------------------- */

        df.setParseBigDecimal(false);
        df.setMultiplier(-3);

        /*
         * Test for double/Double
         */
        checkFormat(3333.3333333, "-9,999.9999999");
        checkParse("-10,000.00000000000", 3333.3333333333335);// rounding error

        df.setParseIntegerOnly(true);
        checkFormat(-3333.3333333, "9,999.9999999");
        checkParse("10,000.00000000000", -3333L);
        df.setParseIntegerOnly(false);
        checkFormat(-3333.3333333, "9,999.9999999");
        checkParse("10,000.00000000000", -3333.3333333333335);// rounding error

        /*
         * Test for long/Long
         */
        checkFormat(3333L, "-9,999");
        df.setParseIntegerOnly(true);
        checkParse("-10,000", 3333L);
        df.setParseIntegerOnly(false);
        checkParse("-10000", 3333.3333333333335);// rounding error

        checkFormat(-3333L, "9,999");
        df.setParseIntegerOnly(true);
        checkParse("10,000", -3333L);
        df.setParseIntegerOnly(false);
        checkParse("10000", -3333.3333333333335);// rounding error

        /* ---------------------------------------------------------------- */

        df.setParseBigDecimal(true);

        /*
         * Test for BigDecimal
         */
        checkFormat(new BigDecimal("33333.333333333333333333333"),
                    "-99,999.999999999999999999999");
        checkParse("-100,000.000000000000000000000",
                    new BigDecimal("33333.333333333333333333333"));

        checkFormat(new BigDecimal("-33333.333333333333333333333"),
                    "99,999.999999999999999999999");
        checkParse("100,000.000000000000000000000",
                    new BigDecimal("-33333.333333333333333333333"));

        /*
         * Test for BigInteger
         */
        checkFormat(new BigInteger("33333333333333333333333333"),
                    "-99,999,999,999,999,999,999,999,999");
        checkParse("-100,000,000,000,000,000,000,000,000",
                    new BigDecimal("33333333333333333333333333"));

        checkFormat(new BigInteger("-33333333333333333333333333"),
                    "99,999,999,999,999,999,999,999,999");
        df.setParseIntegerOnly(true);
        checkParse("100,000,000,000,000,000,000,000,000.000",
                    new BigDecimal("-33333333333333333333333333"));
        df.setParseIntegerOnly(false);
        checkParse("100,000,000,000,000,000,000,000,000.000",
                    new BigDecimal("-33333333333333333333333333.333"));

        /* ================================================================ */

        df = new DecimalFormat("0.#E0;-0.#E0");
        df.setMaximumFractionDigits(50);
        df.setMultiplier(4);

        /*
         * Test for double/Double
         */
        checkFormat(252.5252525252525, "1.01010101010101E3");
        checkParse("-1.01010101010101E3", -2.525252525252525E2);

        checkFormat(-2222.2222, "-8.8888888E3");
        checkParse("8888.8888", 2.2222222E3);

        /*
         * Test for long/Long
         */
        checkFormat(1000L, "4E3");
        checkParse("-4E3", -1000L);

        checkFormat(-250L, "-1E3");
        checkParse("1000", 250L);

        /* ---------------------------------------------------------------- */

        df.setParseBigDecimal(true);

        /*
         * Test for BigDecimal
         */

        checkFormat(new BigDecimal("22222.222222222222222222222"),
                    "8.8888888888888888888888888E4");
        checkParse("-8.8888888888888888888888888E4",
                    new BigDecimal("-2.2222222222222222222222222E4"));

        checkFormat(new BigDecimal("-1111111111111111111.111111111111111111"),
                    "-4.444444444444444444444444444444444444E18");
        checkParse("4444444444444444444.444444444444444444",
                    new BigDecimal("1111111111111111111.111111111111111111"));

        /*
         * Test for BigInteger
         */
        checkFormat(new BigInteger("22222222222222222222222222"),
                    "8.8888888888888888888888888E25");
        checkParse("-8.8888888888888888888888888E25",
                    new BigDecimal("-22222222222222222222222222"));

        checkFormat(new BigInteger("-1111111111111111111111111"),
                    "-4.444444444444444444444444E24");
        checkParse("4444444444444444444444444",
                    new BigDecimal("1111111111111111111111111"));

        /* ---------------------------------------------------------------- */

        df.setParseBigDecimal(false);
        df.setMultiplier(-4);

        /*
         * Test for double/Double
         */
        checkFormat(252.5252525252525, "-1.01010101010101E3");
        checkParse("-1.01010101010101E3", 2.525252525252525E2);

        checkFormat(-2222.2222, "8.8888888E3");
        checkParse("8888.8888", -2.2222222E3);

        /*
         * Test for long/Long
         */
        checkFormat(1000L, "-4E3");
        checkParse("-4E3", 1000L);

        checkFormat(-250L, "1E3");
        checkParse("1000", -250L);

        /* ---------------------------------------------------------------- */

        df.setParseBigDecimal(true);

        /*
         * Test for BigDecimal
         */

        checkFormat(new BigDecimal("22222.222222222222222222222"),
                   "-8.8888888888888888888888888E4");
        checkParse("-8.8888888888888888888888888E4",
                    new BigDecimal("2.2222222222222222222222222E4"));

        checkFormat(new BigDecimal("-1111111111111111111.111111111111111111"),
                    "4.444444444444444444444444444444444444E18");
        checkParse("4444444444444444444.444444444444444444",
                    new BigDecimal("-1111111111111111111.111111111111111111"));

        /*
         * Test for BigInteger
         */
        checkFormat(new BigInteger("22222222222222222222222222"),
                   "-8.8888888888888888888888888E25");
        checkParse("-8.8888888888888888888888888E25",
                    new BigDecimal("22222222222222222222222222"));

        checkFormat(new BigInteger("-1111111111111111111111111"),
                    "4.444444444444444444444444E24");
        checkParse("4444444444444444444444444",
                   new BigDecimal("-1111111111111111111111111"));

        /* ---------------------------------------------------------------- */

        df.setParseBigDecimal(false);
        df.setMultiplier(-3);

        /*
         * Test for double/Double
         */
        checkFormat(3333.3333333, "-9.9999999999E3");
        checkParse("-1.00000000000000E3", 3.33333333333333333E2);

        df.setParseIntegerOnly(true);
        checkFormat(-3333.3333333, "9.9999999999E3");
        checkParse("10.00000000000000E3",-3L);
        df.setParseIntegerOnly(false);
        checkFormat(-3333.3333333, "9.9999999999E3");
        checkParse("10.00000000000000E3", -3.33333333333333333E3);

        /*
         * Test for long/Long
         */
        checkFormat(3333L, "-9.999E3");
        df.setParseIntegerOnly(true);
        checkParse("-1.0E4", 0L);
        df.setParseIntegerOnly(false);
        checkParse("-1.0E4", 3333.3333333333335);

        checkFormat(-3333L, "9.999E3");
        df.setParseIntegerOnly(true);
        checkParse("10.0E4", -3L);
        df.setParseIntegerOnly(false);
        checkParse("10.0E4", -33333.3333333333336);

        /* ---------------------------------------------------------------- */

        df.setParseBigDecimal(true);

        /*
         * Test for BigDecimal
         */

        checkFormat(new BigDecimal("333.333333333333333333333333"),
                   "-9.99999999999999999999999999E2");
        checkParse("-1.0000000000000000000000000E3",
                    new BigDecimal("3.333333333333333333333333E2"));

        df.setParseIntegerOnly(true);
        checkFormat(new BigDecimal("-333.333333333333333333333333"),
                   "9.99999999999999999999999999E2");
        checkParse("10.0000000000000000000000000E3",
                    new BigDecimal("-3"));
        df.setParseIntegerOnly(false);
        checkFormat(new BigDecimal("-333.333333333333333333333333"),
                   "9.99999999999999999999999999E2");
        checkParse("1.0000000000000000000000000E3",
                    new BigDecimal("-3.333333333333333333333333E2"));

        /*
         * Test for BigInteger
         */
        checkFormat(new BigInteger("33333333333333333333333333"),
                    "-9.9999999999999999999999999E25");
        checkParse("-100000000000000000000000000",
                    new BigDecimal("33333333333333333333333333"));

        checkFormat(new BigInteger("-33333333333333333333333333"),
                    "9.9999999999999999999999999E25");
        df.setParseIntegerOnly(true);
        checkParse("100000000000000000000000000000",
                    new BigDecimal("-33333333333333333333333333333"));
        df.setParseIntegerOnly(false);
        checkParse("100000000000000000000000000.000",
                    new BigDecimal("-33333333333333333333333333.333"));

        /* ================================================================ */

        Locale.setDefault(defaultLoc);

        if (err) {
            throw new RuntimeException("Wrong format/parse with DecimalFormat");
        }
    }

    static void checkFormat(Number num, String expected) {
        String got = df.format(num);
        if (!got.equals(expected)) {
            err = true;
            System.err.println("    DecimalFormat format(" +
                               num.getClass().getName() +
                               ") error:" +
                               "\n\tnumber:     " + num +
                               "\n\tpattern:    " + df.toPattern() +
                               "\n\tmultiplier: " + df.getMultiplier() +
                               "\n\tgot:        " + got +
                               "\n\texpected:   " + expected);
        }
    }

    static void checkParse(String text, Double expected) {
        Double got = (Double)df.parse(text, new ParsePosition(0));
        if (!got.equals(expected)) {
            err = true;
            System.err.println("    DecimalFormat parse(double) error:" +
                               "\n\ttext:       " + text +
                               "\n\tpattern:    " + df.toPattern() +
                               "\n\tmultiplier: " + df.getMultiplier() +
                               "\n\tgot:        " + got +
                               "\n\texpected:   " + expected);
        }
    }

    static void checkParse(String text, Long expected) {
        Long got = (Long)df.parse(text, new ParsePosition(0));
        if (!got.equals(expected)) {
            err = true;
            System.err.println("    DecimalFormat parse(long) error:" +
                               "\n\ttext:       " + text +
                               "\n\tpattern:    " + df.toPattern() +
                               "\n\tmultiplier: " + df.getMultiplier() +
                               "\n\tgot:        " + got +
                               "\n\texpected:   " + expected);
        }
    }

    static void checkParse(String text, BigDecimal expected) {
        BigDecimal got = (BigDecimal)df.parse(text, new ParsePosition(0));
        if (!got.equals(expected)) {
            err = true;
            System.err.println("    DecimalFormat parse(BigDecimal) error:" +
                               "\n\ttext:       " + text +
                               "\n\tpattern:    " + df.toPattern() +
                               "\n\tmultiplier: " + df.getMultiplier() +
                               "\n\tgot:        " + got +
                               "\n\texpected:   " + expected);
        }
    }
}
