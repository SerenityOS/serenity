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
 * @bug 4018937 8008577
 * @summary Confirm that methods which are newly added to support BigDecimal and BigInteger work as expected.
 * @library /java/text/testlib
 * @run main/othervm -Djava.locale.providers=COMPAT,SPI BigDecimalParse
 */

import java.math.BigDecimal;
import java.text.*;
import java.util.*;

public class BigDecimalParse extends IntlTest {

    public static void main(String[] args) throws Exception {
        Locale loc = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            new BigDecimalParse().run(args);
        } finally {
            // restore the reserved locale
            Locale.setDefault(loc);
        }
    }

    static final String nonsep_int =
        "123456789012345678901234567890123456789012345678901234567890" +
        "123456789012345678901234567890123456789012345678901234567890" +
        "123456789012345678901234567890123456789012345678901234567890" +
        "123456789012345678901234567890123456789012345678901234567890" +
        "123456789012345678901234567890123456789012345678901234567890" +
        "123456789012345678901234567890123456789012345678901234567890";

    static final String sep_int =
        "123,456,789,012,345,678,901,234,567,890," +
        "123,456,789,012,345,678,901,234,567,890," +
        "123,456,789,012,345,678,901,234,567,890," +
        "123,456,789,012,345,678,901,234,567,890," +
        "123,456,789,012,345,678,901,234,567,890," +
        "123,456,789,012,345,678,901,234,567,890," +
        "123,456,789,012,345,678,901,234,567,890," +
        "123,456,789,012,345,678,901,234,567,890," +
        "123,456,789,012,345,678,901,234,567,890," +
        "123,456,789,012,345,678,901,234,567,890," +
        "123,456,789,012,345,678,901,234,567,890," +
        "123,456,789,012,345,678,901,234,567,890";

    static final String nonsep_zero =
        "000000000000000000000000000000000000000000000000000000000000" +
        "000000000000000000000000000000000000000000000000000000000000" +
        "000000000000000000000000000000000000000000000000000000000000" +
        "000000000000000000000000000000000000000000000000000000000000" +
        "000000000000000000000000000000000000000000000000000000000000" +
        "000000000000000000000000000000000000000000000000000000000000";

    static final String sep_zero =
        "000,000,000,000,000,000,000,000,000,000," +
        "000,000,000,000,000,000,000,000,000,000," +
        "000,000,000,000,000,000,000,000,000,000," +
        "000,000,000,000,000,000,000,000,000,000," +
        "000,000,000,000,000,000,000,000,000,000," +
        "000,000,000,000,000,000,000,000,000,000," +
        "000,000,000,000,000,000,000,000,000,000," +
        "000,000,000,000,000,000,000,000,000,000," +
        "000,000,000,000,000,000,000,000,000,000," +
        "000,000,000,000,000,000,000,000,000,000," +
        "000,000,000,000,000,000,000,000,000,000," +
        "000,000,000,000,000,000,000,000,000,000";

    static final String fra =
        "012345678901234567890123456789012345678901234567890123456789" +
        "012345678901234567890123456789012345678901234567890123456789" +
        "012345678901234567890123456789012345678901234567890123456789" +
        "012345678901234567890123456789012345678901234567890123456789" +
        "012345678901234567890123456789012345678901234567890123456789" +
        "012345678901234567890123456789012345678901234567890123456789";


    Number parsed = null;
    ParsePosition pp;
    boolean exceptionOccurred;
    String msg;
    DecimalFormat df;

    /**
     * Test for normal big numbers which have the fraction part
     */
    void test_Parse_in_DecimalFormat_BigDecimal() {
        df = new DecimalFormat();
        df.setParseBigDecimal(true);

        // From: 1234...7890.012...789
        // To:   BigDecimal 1234...7890.012...789
        check(nonsep_int + "." + fra, new BigDecimal(nonsep_int + "." + fra));

        // From: -1,234...7,890.012...789
        // To:   BigDecimal -1234...7890.012...789
        check("-" + sep_int    + "." + fra,
              new BigDecimal("-" + nonsep_int + "." + fra));

        // From: 000...0000.0...0
        // To:   BigDecimal 0E-360
        check(nonsep_zero + "." + nonsep_zero,
              new BigDecimal(nonsep_zero + "." + nonsep_zero));

        // From: 0.000...0000123...789E370
        // To:   BigDecimal 0.0123...789
        check("0.0000000000" + nonsep_zero + fra + "E370",
              new BigDecimal("0.0000000000" + nonsep_zero + fra + "E370"));

        // From: 0.1123...890E-360
        // To:   BigDecimal 1.123...890E-361
        check("0.1" + nonsep_int + "E-360",
              new BigDecimal("0.1" + nonsep_int + "E-360"));

        // From: 000...0000.0...0123...7890
        // To:   BigDecimal 1.234...890E-361
        check(nonsep_zero + "." + nonsep_zero + nonsep_int,
              new BigDecimal(nonsep_zero + "." + nonsep_zero + nonsep_int));

        // From: 0.123...890E360
        // To:   BigDecimal 123...890
        check("0." + nonsep_int + "E360",
              new BigDecimal("0." + nonsep_int + "E360"));
    }

    /**
     * Test for normal big numbers which have the fraction part with multiplier
     */
    void test_Parse_in_DecimalFormat_BigDecimal_usingMultiplier() {
        df = new DecimalFormat();
        df.setParseBigDecimal(true);

        // From: 250,0...0,000.000...000
        // To:   1000...0000.000...000
        df.setMultiplier(250000000);
        check("250,000,000," + sep_zero + "." + nonsep_zero,
              new BigDecimal("1" + nonsep_zero + "." + nonsep_zero));

        // From: -250,0...0,000.000...000
        // To:   -1000...0000.000...000
        check("-250,000,000," + sep_zero + "." + nonsep_zero,
              new BigDecimal("-1" + nonsep_zero + "." + nonsep_zero));

        // From: 250,0...0,000.000...000
        // To:   -1000...0000.000...000
        df.setMultiplier(-250000000);
        check("250,000,000," + sep_zero + "." + nonsep_zero,
              new BigDecimal("-1" + nonsep_zero + "." + nonsep_zero));

        // From: -250,0...0,000.000...000
        // To:   1000...0000.000...000
        check("-250,000,000," + sep_zero + "." + nonsep_zero,
              new BigDecimal("1" + nonsep_zero + "." + nonsep_zero));

        // Confirm that ArithmeticException is handled properly
        // From: 1000.000
        // To:   333.333
        df.setMultiplier(3);
        check("1000.000", new BigDecimal("333.333"));

        // Confirm that ArithmeticException is handled properly
        // From: 10000.0000
        // To:   303.0303
        df.setMultiplier(33);
        check("10000.0000", new BigDecimal("303.0303"));
    }

    /**
     * Test for division by zero (BigDecimal)
     */
    void test_Parse_in_DecimalFormat_BigDecimal_DivisionByZero() {
        df = new DecimalFormat();
        df.setParseBigDecimal(true);
        df.setMultiplier(0);

        // From: 1000.000
        // To:   Double.POSITIVE_INFINITY
        check("1000.000", Double.POSITIVE_INFINITY);

        // From: -1000
        // To:   Double.NEGATIVE_INFINITY
        check("-1000", Double.NEGATIVE_INFINITY);

        // From: -0.00
        // To:   Double.NaN
        check("-0.00", Double.NaN);
    }

    /**
     * Test for division by zero (Double)
     */
    void test_Parse_in_DecimalFormat_Double_DivisionByZero() {
        df = new DecimalFormat();
        df.setParseBigDecimal(false);
        df.setMultiplier(0);

        // From: 1000.000
        // To:   Double.POSITIVE_INFINITY
        check("1000.000", Double.POSITIVE_INFINITY);

        // From: -1000.000
        // To:   Double.NEGATIVE_INFINITY
        check("-1000.000", Double.NEGATIVE_INFINITY);

        // From: 0.0
        // To:   Double.NaN
        check("0.0", Double.NaN);

        // From: -0.0 (Double)
        // To:   Double.NaN
        check("-0.0", Double.NaN);

        // From: Double.NaN
        // To:   Double.NaN
        check("\ufffd", Double.NaN);

        // From: Double.POSITIVE_INFINITY
        // To:   Double.NaN
        check("\u221e", Double.POSITIVE_INFINITY);

        // From: Double.NEGATIVE_INFINITY
        // To:   Double.NaN
        check("-\u221e", Double.NEGATIVE_INFINITY);
    }

    /**
     * Test for division by zero (Long)
     */
    void test_Parse_in_DecimalFormat_Long_DivisionByZero() {
        df = new DecimalFormat();
        df.setParseBigDecimal(false);
        df.setMultiplier(0);

        // From: 1000
        // To:   Double.POSITIVE_INFINITY
        check("1000", Double.POSITIVE_INFINITY);

        // From: -1000
        // To:   Double.NEGATIVE_INFINITY
        check("-1000", Double.NEGATIVE_INFINITY);

        // From: -000 (Long)
        // To:   Double.NaN
        check("-000", Double.NaN);
    }

    /**
     * Test for normal big numbers which don't have the fraction part
     */
    void test_Parse_in_DecimalFormat_BigInteger() {
        df = new DecimalFormat();
        df.setParseBigDecimal(true);

        // From: 123...890
        // To:   BigDecimal 123...890
        check(nonsep_int + nonsep_int, new BigDecimal(nonsep_int + nonsep_int));

        // From: 123,4...7,890
        // To:   BigDecimal 1234...7890
        check(sep_int + "," + sep_int, new BigDecimal(nonsep_int + nonsep_int));

        // From: -000...000123...890
        // To:   BigDecimal -123...890
        check("-" + nonsep_zero + nonsep_int, new BigDecimal("-" + nonsep_int));

        // From: -000,0...0,000,123,4...7,890
        // To:   BigDecimal -123...890
        check("-" + sep_zero + "," + sep_int, new BigDecimal("-" + nonsep_int));
    }

    /**
     * Test for normal big numbers which don't have the fraction part with
     * multiplier
     */
    void test_Parse_in_DecimalFormat_BigInteger_usingMultiplier() {
        df = new DecimalFormat();
        df.setParseBigDecimal(true);

        // From: 250,0...0,000
        // To:   1000...0000
        df.setMultiplier(250000000);
        check("250,000,000," + sep_zero, new BigDecimal("1" + nonsep_zero));

        // From: -250,0...0,000
        // To:   -1000...0000
        check("-250,000,000," + sep_zero, new BigDecimal("-1" + nonsep_zero));

        // From: 250,0...0,000
        // To:   -1000...0000
        df.setMultiplier(-250000000);
        check("250,000,000," + sep_zero, new BigDecimal("-1" + nonsep_zero));

        // From: -250,0...0,000
        // To:   1000...0000
        check("-250,000,000," + sep_zero, new BigDecimal("1" + nonsep_zero));

        // From: 250,0...0,000E-360
        // To:   -1000...0000.000...000
        check("250,000,000," + sep_zero + "," + sep_zero + "E-360",
              new BigDecimal("-1" + nonsep_zero + "." + nonsep_zero));

        // Confirm that a division which results in a irrational number is done
        // properly
        // From: 1000
        // To:   333
        df.setMultiplier(3);
        check("1000", new BigDecimal("333"));
    }

    /**
     * Test for special numbers
     *    Double.NaN
     *    Double.POSITIVE_INFINITY
     *    Double.NEGATIVE_INFINITY
     */
    void test_Parse_in_DecimalFormat_SpecialNumber() {
        df = new DecimalFormat();
        df.setParseBigDecimal(true);

        String[] numbers = {
            "0", "0.0", "25", "25.0", "25.5", "\u221e", "\ufffd",
            "-0", "-0.0", "-25", "-25.0", "-25.5", "-\u221e",
        };
        int multipliers[] = {5, -5};
        Number[][] expected = {
            {
                new BigDecimal("0"), new BigDecimal("0.0"), new BigDecimal("5"),
                new BigDecimal("5.0"), new BigDecimal("5.1"),
                Double.POSITIVE_INFINITY, Double.NaN,
                new BigDecimal("0"), new BigDecimal("0.0"),
                new BigDecimal("-5"), new BigDecimal("-5.0"),
                new BigDecimal("-5.1"),
                Double.NEGATIVE_INFINITY, Double.NaN,
            },
            {
                new BigDecimal("0"), new BigDecimal("0.0"),
                new BigDecimal("-5"), new BigDecimal("-5.0"),
                new BigDecimal("-5.1"),
                Double.NEGATIVE_INFINITY, Double.NaN,
                new BigDecimal("0"), new BigDecimal("0.0"), new BigDecimal("5"),
                new BigDecimal("5.0"), new BigDecimal("5.1"),
                Double.POSITIVE_INFINITY,
            },
        };

        for (int i = 0; i < multipliers.length; i++) {
            df.setMultiplier(multipliers[i]);
            for (int j = 0; j < numbers.length; j++) {
                check(String.valueOf(numbers[j]), expected[i][j]);
            }
        }
    }

    /**
     * Test for special numbers
     */
    void test_Parse_in_DecimalFormat_Other() {
        df = new DecimalFormat();
        df.setParseBigDecimal(true);

        String[] numbers = {
            "-9223372036854775808",     // Long.MIN_VALUE
        };
        int multipliers[] = {1, -1};
        String[][] expected = {
            {"-9223372036854775808"},   // Long.MIN_VALUE
            {"9223372036854775808"},    // Long.MAX_VALUE+1 = abs(MIN_VALUE)
        };

        for (int i = 0; i < multipliers.length; i++) {
            df.setMultiplier(multipliers[i]);
            for (int j = 0; j < numbers.length; j++) {
                check(String.valueOf(numbers[j]),
                      new BigDecimal(expected[i][j]));
            }
        }
    }

    static final String[] patterns = {
        "  {0, number}  ",
        "  {0, number}  ",
        "  {0, number, currency}  ",
        "  {0, number, currency}  ",
        "  {0, number, percent}  ",
        "  {0, number, percent}  ",
        "  {0, number,#,##0.###E0}  ",
        "  {0, number,#,##0.###E0}  ",

        "  {0, number}  ",
        "  {0, number}  ",
        "  {0, number, integer}  ",
        "  {0, number, integer}  ",
        "  {0, number, currency}  ",
        "  {0, number, currency}  ",
        "  {0, number, percent}  ",
        "  {0, number, percent}  ",
        "  {0, number,#,##0.###E0}  ",
        "  {0, number,#,##0.###E0}  ",
    };
    static final String[] from = {
        "  12,345,678,901,234,567,890.98765432109876543210987654321  ",
        "  -12,345,678,901,234,567,890.98765432109876543210987654321  ",
        "  $12,345,678,901,234,567,890.98765432109876543210987654321  ",
        "  ($12,345,678,901,234,567,890.98765432109876543210987654321)  ",
        "  1,234,567,890,123,456,789,098.76543210987654321098765432100%  ",
        "  -1,234,567,890,123,456,789,098.76543210987654321098765432100%  ",
        "  12,345,678,901,234,567,890.98765432109876543210987654321E-20  ",
        "  -12,345,678,901,234,567,890.98765432109876543210987654321E-20  ",

        "  9,876,543,210,987,654,321,098,765,432,109,876,543,210  ",
        "  -9,876,543,210,987,654,321,098,765,432,109,876,543,210  ",
        "  9,876,543,210,987,654,321,098,765,432,109,876,543,210E5  ",
        "  -9,876,543,210,987,654,321,098,765,432,109,876,543,210E-5  ",
        "  $9,876,543,210,987,654,321,098,765,432,109,876,543,210.00  ",
        "  ($9,876,543,210,987,654,321,098,765,432,109,876,543,210.00)  ",
        "  987,654,321,098,765,432,109,876,543,210,987,654,321,012%  ",
        "  -987,654,321,098,765,432,109,876,543,210,987,654,321,012%  ",
        "  98,765,432,109,876,543,210.98765432109876543210E20  ",
        "  -987,654,321,098,765,432,109,876,543,210,987,654,321,000,000,000,000,000,000,000E-20  ",
    };

    static final String[] expected1 = { // isParseIntegerOnly() == false
        "12345678901234567890.98765432109876543210987654321",
        "-12345678901234567890.98765432109876543210987654321",
        "12345678901234567890.98765432109876543210987654321",
        "-12345678901234567890.98765432109876543210987654321",
        "12345678901234567890.98765432109876543210987654321",
        "-12345678901234567890.98765432109876543210987654321",
        "0.1234567890123456789098765432109876543210987654321",
        "-0.1234567890123456789098765432109876543210987654321",

        "9876543210987654321098765432109876543210",
        "-9876543210987654321098765432109876543210",
        "9.876543210987654321098765432109876543210E44",
        "-98765432109876543210987654321098765.43210",
        "9876543210987654321098765432109876543210.00",
        "-9876543210987654321098765432109876543210.00",
        "9876543210987654321098765432109876543210.12",
        "-9876543210987654321098765432109876543210.12",
        "9876543210987654321098765432109876543210",
        "-9876543210987654321098765432109876543210.00000000000000000000",
    };
    static final int[] parsePosition1 = {
        60, 61, 61, 63, 64, 65, 64, 65,
        57, 58, 59, 61, 61, 63, 60, 61, 54, 88,
    };

    /**
     * Test for MessageFormat: setParseIntegerOnly(false)
     */
    void test_Parse_in_MessageFormat_NotParseIntegerOnly() {
        for (int i=0; i < patterns.length; i++) {
            pp = new ParsePosition(0);
            Object[] parsed = null;

            try {
                MessageFormat mf = new MessageFormat(patterns[i]);
                Format[] formats = mf.getFormats();
                for (int j=0; j < formats.length; j++) {
                    ((DecimalFormat)formats[j]).setParseBigDecimal(true);
                }

                parsed = mf.parse(from[i], pp);

                if (pp.getErrorIndex() != -1) {
                    errln("Case" + (i+1) +
                          ": getErrorIndex() returns wrong value. expected:-1, got:"+
                          pp.getErrorIndex() + " for " + from[i]);
                }
                if (pp.getIndex() != parsePosition1[i]) {
                    errln("Case" + (i+1) +
                          ": getIndex() returns wrong value. expected:" +
                          parsePosition1[i] + ", got:"+ pp.getIndex() +
                          " for " + from[i]);
                }
            }
            catch(Exception e) {
                errln("Unexpected exception: " + e.getMessage());
            }

            checkType(from[i], getType(new BigDecimal(expected1[i])),
                      getType((Number)parsed[0]));
            checkParse(from[i], new BigDecimal(expected1[i]),
                       (Number)parsed[0]);
        }
    }

    static final String[] expected2 = { // isParseIntegerOnly() == true
        "12345678901234567890",
        "-12345678901234567890",
        "12345678901234567890",
        "-12345678901234567890",
        "12345678901234567890",
        "-12345678901234567890",
        "0",
        "0",

        "9876543210987654321098765432109876543210",
        "-9876543210987654321098765432109876543210",
        "9.876543210987654321098765432109876543210E44",
        "-98765432109876543210987654321098765.43210",
        "9876543210987654321098765432109876543210",
        "-9876543210987654321098765432109876543210",
        "9876543210987654321098765432109876543210.12",
        "-9876543210987654321098765432109876543210.12",
        "9876543210987654321098765432109876543210",
        "-9876543210987654321098765432109876543210.00000000000000000000",
    };
    static final int[][] parsePosition2 = {     // {errorIndex, index}
        /*
         * Should keep in mind that the expected result is different from
         * DecimalFormat.parse() for some cases.
         */
        {28, 0},        // parsing stopped at '.'
        {29, 0},        // parsing stopped at '.'
        {29, 0},        // parsing stopped at '.'
        {2, 0},         // parsing stopped at '(' because cannot find ')'
        {2, 0},         // parsing stopped at the first numeric
                        // because cannot find '%'
        {2, 0},         // parsing stopped at the first numeric
                        // because cannot find '%'
        {28, 0},        // parsing stopped at '.'
        {29, 0},        // parsing stopped at '.'

        {-1, 57}, {-1, 58}, {-1, 59}, {-1, 61},
        {56, 0},        // parsing stopped at '.'
                        // because cannot find '%'
        {2, 0},         // parsing stopped at '(' because cannot find ')'
        {-1, 60}, {-1, 61},
        {28, 0},        // parsing stopped at '.'
        {-1, 88},
    };

    /**
     * Test for MessageFormat: setParseIntegerOnly(true)
     */
    void test_Parse_in_MessageFormat_ParseIntegerOnly() {
        for (int i=0; i < patterns.length; i++) {
            pp = new ParsePosition(0);
            Object[] parsed = null;

            try {
                MessageFormat mf = new MessageFormat(patterns[i]);
                Format[] formats = mf.getFormats();
                for (int j=0; j < formats.length; j++) {
                    ((DecimalFormat)formats[j]).setParseBigDecimal(true);
                    ((DecimalFormat)formats[j]).setParseIntegerOnly(true);
                }

                parsed = mf.parse(from[i], pp);

                if (pp.getErrorIndex() != parsePosition2[i][0]) {
                    errln("Case" + (i+1) +
                          ": getErrorIndex() returns wrong value. expected:" +
                          parsePosition2[i][0] + ", got:"+ pp.getErrorIndex() +
                          " for " + from[i]);
                }
                if (pp.getIndex() != parsePosition2[i][1]) {
                    errln("Case" + (i+1) +
                          ": getIndex() returns wrong value. expected:" +
                          parsePosition2[i][1] + ", got:"+ pp.getIndex() +
                          " for " + from[i]);
                }
            }
            catch(Exception e) {
                errln("Unexpected exception: " + e.getMessage());
            }

            if (parsePosition2[i][0] == -1) {
                checkType(from[i], getType(new BigDecimal(expected2[i])),
                          getType((Number)parsed[0]));
                checkParse(from[i], new BigDecimal(expected2[i]),
                           (Number)parsed[0]);
            }
        }
    }

    static final String[] from3 = {
        "12,345,678,901,234,567,890.98765432109876543210987654321",
        "-12,345,678,901,234,567,890.98765432109876543210987654321",
        "9,876,543,210,987,654,321,098,765,432,109,876,543,210",
        "-9,876,543,210,987,654,321,098,765,432,109,876,543,210",
        "1234556790000E-8",
    };
    static final String[] expected3 = {
        "12345678901234567890",
        "-12345678901234567890",
        "9876543210987654321098765432109876543210",
        "-9876543210987654321098765432109876543210",
        "12345.56790000",
    };
    static final int[][] parsePosition3 = {     // {errorIndex, index}
        {-1, 26},
        {-1, 27},
        {-1, 53},
        {-1, 54},
        {-1, 16},
    };

    /**
     * Test for DecimalFormat: setParseIntegerOnly(true)
     */
    void test_Parse_in_DecimalFormat_ParseIntegerOnly() {
        DecimalFormat df = (DecimalFormat)NumberFormat.getIntegerInstance();
        df.setParseBigDecimal(true);

        for (int i=0; i < from3.length; i++) {
            pp = new ParsePosition(0);
            Number parsed = null;

            try {
                parsed = df.parse(from3[i], pp);

                if (pp.getErrorIndex() != parsePosition3[i][0]) {
                    errln("Case" + (i+1) +
                          ": getErrorIndex() returns wrong value. expected:" +
                          parsePosition3[i][0] + ", got:"+ pp.getErrorIndex() +
                          " for " + from3[i]);
                }
                if (pp.getIndex() != parsePosition3[i][1]) {
                    errln("Case" + (i+1) +
                          ": getIndex() returns wrong value. expected:" +
                          parsePosition3[i][1] + ", got:"+ pp.getIndex() +
                          " for " + from3[i]);
                }
            }
            catch(Exception e) {
                errln("Unexpected exception: " + e.getMessage());
            }

            if (parsePosition3[i][0] == -1) {
                checkType(from3[i], getType(new BigDecimal(expected3[i])),
                          getType(parsed));
                checkParse(from3[i], new BigDecimal(expected3[i]), parsed);
            }
        }
    }

    protected void check(String from, Number to) {
        pp = new ParsePosition(0);
        try {
            parsed = df.parse(from, pp);
        }
        catch(Exception e) {
            exceptionOccurred = true;
            errln(e.getMessage());
        }
        if (!exceptionOccurred) {
            checkParse(from, to, parsed);
            checkType(from, getType(to), getType(parsed));
            checkParsePosition(from, from.length(), pp.getIndex());
        }
    }

    private void checkParse(String orig, Number expected, Number got) {
        if (!expected.equals(got)) {
            errln("Parsing... failed." +
                  "\n   original: " + orig +
                  "\n   parsed:   " + got +
                  "\n   expected: " + expected + "\n");
        }
    }

    private void checkType(String orig, String expected, String got) {
        if (!expected.equals(got)) {
            errln("Parsing... unexpected Class returned." +
                  "\n   original: " + orig +
                  "\n   got:      " + got +
                  "\n   expected: " + expected + "\n");
        }
    }

    private void checkParsePosition(String orig, int expected, int got) {
        if (expected != got) {
            errln("Parsing... wrong ParsePosition returned." +
                  "\n   original: " + orig +
                  "\n   got:      " + got +
                  "\n   expected: " + expected + "\n");
        }
    }

    private String getType(Number number) {
        return number.getClass().getName();
    }
}
