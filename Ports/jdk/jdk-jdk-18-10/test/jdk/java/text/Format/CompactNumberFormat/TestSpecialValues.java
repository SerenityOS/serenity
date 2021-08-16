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
 * @summary Checks the formatting and parsing of special values
 * @modules jdk.localedata
 * @run testng/othervm TestSpecialValues
 */
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestSpecialValues {

    private static final NumberFormat FORMAT = NumberFormat
            .getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT);

    @DataProvider(name = "formatSpecialValues")
    Object[][] formatSpecialValues() {
        return new Object[][]{
            // number , formatted ouput
            {+0.0, "0"},
            {-0.0, "-0"},
            {Double.MIN_VALUE, "0"},
            {Double.MIN_NORMAL, "0"},
            {Double.NaN, "NaN"},
            {Double.POSITIVE_INFINITY, "\u221E"},
            {Double.NEGATIVE_INFINITY, "-\u221E"},
            {Long.MIN_VALUE, "-9223372T"},
            {Long.MAX_VALUE, "9223372T"},};
    }

    @DataProvider(name = "parseSpecialValues")
    Object[][] parseSpecialValues() {
        return new Object[][]{
            // parse string, parsed number
            {"-0.0", -0.0},
            {"" + Long.MIN_VALUE, Long.MIN_VALUE},
            {"" + Long.MAX_VALUE, Long.MAX_VALUE},
            {"NaN", Double.NaN},
            {"\u221E", Double.POSITIVE_INFINITY},
            {"-\u221E", Double.NEGATIVE_INFINITY},};
    }

    @Test(dataProvider = "formatSpecialValues")
    public void testFormatSpecialValues(Object number, String expected) {
        CompactFormatAndParseHelper.testFormat(FORMAT, number, expected);
    }

    @Test(dataProvider = "parseSpecialValues")
    public void testParseSpecialValues(String parseString, Number expected)
            throws ParseException {
        CompactFormatAndParseHelper.testParse(FORMAT, parseString, expected, null, null);
    }
}
