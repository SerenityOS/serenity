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
 * @summary Checks the compact number format with COMPAT provider. Since the
 *          compact number resources are only provided by CLDR, using COMPAT
 *          as a provider should always use the default patterns added in the
 *          FormatData.java resource bundle
 * @modules jdk.localedata
 * @run testng/othervm -Djava.locale.providers=COMPAT TestWithCompatProvider
 */
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestWithCompatProvider {

    private static final NumberFormat FORMAT_DZ_SHORT = NumberFormat
            .getCompactNumberInstance(new Locale("dz"), NumberFormat.Style.SHORT);

    private static final NumberFormat FORMAT_EN_US_SHORT = NumberFormat
            .getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT);

    @DataProvider(name = "format")
    Object[][] compactFormatData() {
        return new Object[][]{
            {FORMAT_DZ_SHORT, 1000.09, "1K"},
            {FORMAT_DZ_SHORT, -999.99, "-1K"},
            {FORMAT_DZ_SHORT, -0.0, "-0"},
            {FORMAT_DZ_SHORT, new BigInteger("12345678901234567890"), "12345679T"},
            {FORMAT_DZ_SHORT, new BigDecimal("12345678901234567890.89"), "12345679T"},
            {FORMAT_EN_US_SHORT, -999.99, "-1K"},
            {FORMAT_EN_US_SHORT, 9999, "10K"},
            {FORMAT_EN_US_SHORT, 3000.90, "3K"},
            {FORMAT_EN_US_SHORT, new BigInteger("12345678901234567890"), "12345679T"},
            {FORMAT_EN_US_SHORT, new BigDecimal("12345678901234567890.89"), "12345679T"},};
    }

    @DataProvider(name = "parse")
    Object[][] compactParseData() {
        return new Object[][]{
            {FORMAT_DZ_SHORT, "1K", 1000L},
            {FORMAT_DZ_SHORT, "-3K", -3000L},
            {FORMAT_DZ_SHORT, "12345700T", 1.23457E19},
            {FORMAT_EN_US_SHORT, "-99", -99L},
            {FORMAT_EN_US_SHORT, "10K", 10000L},
            {FORMAT_EN_US_SHORT, "12345679T", 1.2345679E19},};
    }

    @Test(dataProvider = "format")
    public void testFormat(NumberFormat cnf, Object number,
            String expected) {
        CompactFormatAndParseHelper.testFormat(cnf, number, expected);
    }

    @Test(dataProvider = "parse")
    public void testParse(NumberFormat cnf, String parseString,
            Number expected) throws ParseException {
        CompactFormatAndParseHelper.testParse(cnf, parseString, expected, null, null);
    }

}
