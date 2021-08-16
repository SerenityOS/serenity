/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8177552
 * @summary Checks CNF.parse() when parseBigDecimal is set to true
 * @modules jdk.localedata
 * @run testng/othervm TestParseBigDecimal
 */

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.text.CompactNumberFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class TestParseBigDecimal {

    private static final CompactNumberFormat FORMAT_DZ_LONG = (CompactNumberFormat) NumberFormat
            .getCompactNumberInstance(new Locale("dz"), NumberFormat.Style.LONG);

    private static final CompactNumberFormat FORMAT_EN_US_SHORT = (CompactNumberFormat) NumberFormat
            .getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT);

    private static final CompactNumberFormat FORMAT_EN_LONG = (CompactNumberFormat) NumberFormat
            .getCompactNumberInstance(new Locale("en"), NumberFormat.Style.LONG);

    private static final CompactNumberFormat FORMAT_HI_IN_LONG = (CompactNumberFormat) NumberFormat
            .getCompactNumberInstance(new Locale("hi", "IN"), NumberFormat.Style.LONG);

    private static final CompactNumberFormat FORMAT_JA_JP_SHORT = (CompactNumberFormat) NumberFormat
            .getCompactNumberInstance(Locale.JAPAN, NumberFormat.Style.SHORT);

    private static final CompactNumberFormat FORMAT_IT_SHORT = (CompactNumberFormat) NumberFormat
            .getCompactNumberInstance(new Locale("it"), NumberFormat.Style.SHORT);

    private static final CompactNumberFormat FORMAT_SW_LONG = (CompactNumberFormat) NumberFormat
            .getCompactNumberInstance(new Locale("sw"), NumberFormat.Style.LONG);

    private static final CompactNumberFormat FORMAT_SE_SHORT = (CompactNumberFormat) NumberFormat
            .getCompactNumberInstance(new Locale("se"), NumberFormat.Style.SHORT);

    @BeforeTest
    public void mutateInstances() {
        FORMAT_DZ_LONG.setParseBigDecimal(true);
        FORMAT_EN_US_SHORT.setParseBigDecimal(true);
        FORMAT_EN_LONG.setParseBigDecimal(true);
        FORMAT_HI_IN_LONG.setParseBigDecimal(true);
        FORMAT_JA_JP_SHORT.setParseBigDecimal(true);
        FORMAT_IT_SHORT.setParseBigDecimal(true);
        FORMAT_SW_LONG.setParseBigDecimal(true);
        FORMAT_SE_SHORT.setParseBigDecimal(true);
    }

    @DataProvider(name = "parse")
    Object[][] compactParseData() {
        return new Object[][]{
            // compact number format instance, string to parse, parsed number
            {FORMAT_DZ_LONG, "\u0F66\u0F9F\u0F7C\u0F44\u0F0B\u0F55\u0FB2"
                + "\u0F42 \u0F21", new BigDecimal("1000")},
            {FORMAT_DZ_LONG, "-\u0F66\u0F9F\u0F7C\u0F44\u0F0B\u0F55\u0FB2"
                + "\u0F42 \u0F23", new BigDecimal("-3000")},
            {FORMAT_DZ_LONG, "\u0F51\u0F74\u0F44\u0F0B\u0F55\u0FB1\u0F74\u0F62"
                + "\u0F0B\u0F66\u0F0B\u0F61\u0F0B \u0F21"
                + "\u0F22\u0F23\u0F24\u0F25\u0F27", new BigDecimal("12345700000000000000")},
            {FORMAT_DZ_LONG, "-\u0F51\u0F74\u0F44\u0F0B\u0F55\u0FB1\u0F74\u0F62"
                + "\u0F0B\u0F66\u0F0B\u0F61\u0F0B \u0F21"
                + "\u0F22\u0F23\u0F24\u0F25\u0F27", new BigDecimal("-12345700000000000000")},
            {FORMAT_EN_US_SHORT, "-0.0", new BigDecimal("-0.0")},
            {FORMAT_EN_US_SHORT, "0", new BigDecimal("0")},
            {FORMAT_EN_US_SHORT, "499", new BigDecimal("499")},
            {FORMAT_EN_US_SHORT, "-499", new BigDecimal("-499")},
            {FORMAT_EN_US_SHORT, "499.89", new BigDecimal("499.89")},
            {FORMAT_EN_US_SHORT, "-499.89", new BigDecimal("-499.89")},
            {FORMAT_EN_US_SHORT, "1K", new BigDecimal("1000")},
            {FORMAT_EN_US_SHORT, "-1K", new BigDecimal("-1000")},
            {FORMAT_EN_US_SHORT, "3K", new BigDecimal("3000")},
            {FORMAT_EN_US_SHORT, "-3K", new BigDecimal("-3000")},
            {FORMAT_EN_US_SHORT, "17K", new BigDecimal("17000")},
            {FORMAT_EN_US_SHORT, "-17K", new BigDecimal("-17000")},
            {FORMAT_EN_US_SHORT, "12345678901234567890",
                new BigDecimal("12345678901234567890")},
            {FORMAT_EN_US_SHORT, "12345679T", new BigDecimal("12345679000000000000")},
            {FORMAT_EN_US_SHORT, "-12345679T", new BigDecimal("-12345679000000000000")},
            {FORMAT_EN_US_SHORT, "599.01K", new BigDecimal("599010.00")},
            {FORMAT_EN_US_SHORT, "-599.01K", new BigDecimal("-599010.00")},
            {FORMAT_EN_US_SHORT, "599444444.90T", new BigDecimal("599444444900000000000.00")},
            {FORMAT_EN_US_SHORT, "-599444444.90T", new BigDecimal("-599444444900000000000.00")},
            {FORMAT_EN_US_SHORT, "123456789012345.5678K",
                new BigDecimal("123456789012345567.8000")},
            {FORMAT_EN_US_SHORT, "17.000K", new BigDecimal("17000.000")},
            {FORMAT_EN_US_SHORT, "123.56678K", new BigDecimal("123566.78000")},
            {FORMAT_EN_US_SHORT, "-123.56678K", new BigDecimal("-123566.78000")},
            {FORMAT_EN_LONG, "999", new BigDecimal("999")},
            {FORMAT_EN_LONG, "1 thousand", new BigDecimal("1000")},
            {FORMAT_EN_LONG, "3 thousand", new BigDecimal("3000")},
            {FORMAT_EN_LONG, "12345679 trillion", new BigDecimal("12345679000000000000")},
            {FORMAT_HI_IN_LONG, "999", new BigDecimal("999")},
            {FORMAT_HI_IN_LONG, "-999", new BigDecimal("-999")},
            {FORMAT_HI_IN_LONG, "1 \u0939\u091C\u093C\u093E\u0930", new BigDecimal("1000")},
            {FORMAT_HI_IN_LONG, "-1 \u0939\u091C\u093C\u093E\u0930", new BigDecimal("-1000")},
            {FORMAT_HI_IN_LONG, "3 \u0939\u091C\u093C\u093E\u0930", new BigDecimal("3000")},
            {FORMAT_HI_IN_LONG, "12345679 \u0916\u0930\u092C", new BigDecimal("1234567900000000000")},
            {FORMAT_HI_IN_LONG, "-12345679 \u0916\u0930\u092C", new BigDecimal("-1234567900000000000")},
            {FORMAT_JA_JP_SHORT, "-99", new BigDecimal("-99")},
            {FORMAT_JA_JP_SHORT, "1\u4E07", new BigDecimal("10000")},
            {FORMAT_JA_JP_SHORT, "30\u4E07", new BigDecimal("300000")},
            {FORMAT_JA_JP_SHORT, "-30\u4E07", new BigDecimal("-300000")},
            {FORMAT_JA_JP_SHORT, "12345679\u5146", new BigDecimal("12345679000000000000")},
            {FORMAT_JA_JP_SHORT, "-12345679\u5146", new BigDecimal("-12345679000000000000")},
            {FORMAT_IT_SHORT, "-99", new BigDecimal("-99")},
            {FORMAT_IT_SHORT, "1\u00a0Mln", new BigDecimal("1000000")},
            {FORMAT_IT_SHORT, "30\u00a0Mln", new BigDecimal("30000000")},
            {FORMAT_IT_SHORT, "-30\u00a0Mln", new BigDecimal("-30000000")},
            {FORMAT_IT_SHORT, "12345679\u00a0Bln", new BigDecimal("12345679000000000000")},
            {FORMAT_IT_SHORT, "-12345679\u00a0Bln", new BigDecimal("-12345679000000000000")},
            {FORMAT_SW_LONG, "-0.0", new BigDecimal("-0.0")},
            {FORMAT_SW_LONG, "499", new BigDecimal("499")},
            {FORMAT_SW_LONG, "elfu 1", new BigDecimal("1000")},
            {FORMAT_SW_LONG, "elfu 3", new BigDecimal("3000")},
            {FORMAT_SW_LONG, "elfu 17", new BigDecimal("17000")},
            {FORMAT_SW_LONG, "elfu -3", new BigDecimal("-3000")},
            {FORMAT_SW_LONG, "-499", new BigDecimal("-499")},
            {FORMAT_SW_LONG, "elfu 1", new BigDecimal("1000")},
            {FORMAT_SW_LONG, "elfu 3", new BigDecimal("3000")},
            {FORMAT_SW_LONG, "elfu -3", new BigDecimal("-3000")},
            {FORMAT_SW_LONG, "elfu 17", new BigDecimal("17000")},
            {FORMAT_SW_LONG, "trilioni 12345679", new BigDecimal("12345679000000000000")},
            {FORMAT_SW_LONG, "trilioni -12345679", new BigDecimal("-12345679000000000000")},
            {FORMAT_SW_LONG, "elfu 599.01", new BigDecimal("599010.00")},
            {FORMAT_SW_LONG, "elfu -599.01", new BigDecimal("-599010.00")},
            {FORMAT_SE_SHORT, "999", new BigDecimal("999")},
            {FORMAT_SE_SHORT, "8\u00a0mn", new BigDecimal("8000000")},
            {FORMAT_SE_SHORT, "8\u00a0dt", new BigDecimal("8000")},
            {FORMAT_SE_SHORT, "12345679\u00a0bn", new BigDecimal("12345679000000000000")},
            {FORMAT_SE_SHORT, "12345679,89\u00a0bn", new BigDecimal("12345679890000000000.00")},
            {FORMAT_SE_SHORT, "\u2212999", new BigDecimal("-999")},
            {FORMAT_SE_SHORT, "\u22128\u00a0mn", new BigDecimal("-8000000")},
            {FORMAT_SE_SHORT, "\u22128\u00a0dt", new BigDecimal("-8000")},
            {FORMAT_SE_SHORT, "\u221212345679\u00a0bn", new BigDecimal("-12345679000000000000")},
            {FORMAT_SE_SHORT, "\u221212345679,89\u00a0bn", new BigDecimal("-12345679890000000000.00")},};
    }

    @Test(dataProvider = "parse")
    public void testParse(NumberFormat cnf, String parseString,
            Number expected) throws ParseException {
        CompactFormatAndParseHelper.testParse(cnf, parseString, expected, null, BigDecimal.class);
    }
}
