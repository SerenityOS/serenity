/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @summary Checks the functioning of compact number format by changing the
 *          formatting parameters. For example, min fraction digits, grouping
 *          size etc.
 * @modules jdk.localedata
 * @run testng/othervm TestMutatingInstance
 */
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.CompactNumberFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestMutatingInstance {

    private static final NumberFormat FORMAT_FRACTION = NumberFormat
            .getCompactNumberInstance(new Locale("en"), NumberFormat.Style.LONG);

    private static final CompactNumberFormat FORMAT_GROUPING = (CompactNumberFormat) NumberFormat
            .getCompactNumberInstance(new Locale("en"), NumberFormat.Style.LONG);

    private static final NumberFormat FORMAT_MININTEGER = NumberFormat
            .getCompactNumberInstance(new Locale("en"), NumberFormat.Style.LONG);

    private static final NumberFormat FORMAT_PARSEINTONLY = NumberFormat
            .getCompactNumberInstance(new Locale("en"), NumberFormat.Style.LONG);

    // No compact patterns are specified for this instance except at index 4.
    // This is to test how the behaviour differs between compact number formatting
    // and general number formatting
    private static final NumberFormat FORMAT_NO_PATTERNS = new CompactNumberFormat(
            "#,##0.0#", DecimalFormatSymbols.getInstance(Locale.US),
            new String[]{"", "", "", "", "00K", "", "", "", "", "", "", "", "", "", ""});

    @BeforeTest
    public void mutateInstances() {
        FORMAT_FRACTION.setMinimumFractionDigits(2);
        FORMAT_GROUPING.setGroupingSize(3);
        FORMAT_GROUPING.setGroupingUsed(true);
        FORMAT_MININTEGER.setMinimumIntegerDigits(5);
        FORMAT_PARSEINTONLY.setParseIntegerOnly(true);
        FORMAT_PARSEINTONLY.setGroupingUsed(true);
        // Setting min fraction digits and other fields does not effect
        // the general number formatting behaviour, when no compact number
        // patterns are specified
        FORMAT_NO_PATTERNS.setMinimumFractionDigits(2);
    }

    @DataProvider(name = "format")
    Object[][] compactFormatData() {
        return new Object[][]{
            {FORMAT_FRACTION, 1900, "1.90 thousand"},
            {FORMAT_FRACTION, 1000, "1.00 thousand"},
            {FORMAT_FRACTION, 9090.99, "9.09 thousand"},
            {FORMAT_FRACTION, new BigDecimal(12346567890987654.32),
                "12346.57 trillion"},
            {FORMAT_FRACTION, new BigInteger("12346567890987654"),
                "12346.57 trillion"},
            {FORMAT_GROUPING, new BigDecimal(12346567890987654.32),
                "12,347 trillion"},
            {FORMAT_GROUPING, 100000, "100 thousand"},
            {FORMAT_MININTEGER, 10000, "00010 thousand"},
            {FORMAT_NO_PATTERNS, 100000, "100,000"},
            {FORMAT_NO_PATTERNS, 1000.998, "1,001"},
            {FORMAT_NO_PATTERNS, 10900, "10.90K"},
            {FORMAT_NO_PATTERNS, new BigDecimal(12346567890987654.32), "12,346,567,890,987,654"},};
    }

    @DataProvider(name = "parse")
    Object[][] compactParseData() {
        return new Object[][]{
            {FORMAT_FRACTION, "190 thousand", 190000L},
            {FORMAT_FRACTION, "19.9 thousand", 19900L},
            {FORMAT_GROUPING, "12,346 thousand", 12346000L},
            {FORMAT_PARSEINTONLY, "12345 thousand", 12345000L},
            {FORMAT_PARSEINTONLY, "12,345 thousand", 12345000L},
            {FORMAT_PARSEINTONLY, "12.345 thousand", 12000L},};
    }

    @Test(dataProvider = "format")
    public void formatCompactNumber(NumberFormat nf,
            Object number, String expected) {
        CompactFormatAndParseHelper.testFormat(nf, number, expected);
    }

    @Test(dataProvider = "parse")
    public void parseCompactNumber(NumberFormat nf,
            String parseString, Number expected) throws ParseException {
        CompactFormatAndParseHelper.testParse(nf, parseString, expected, null, null);
    }

}
