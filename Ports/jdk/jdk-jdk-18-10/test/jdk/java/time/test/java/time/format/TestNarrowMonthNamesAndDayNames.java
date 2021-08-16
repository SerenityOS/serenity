/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @modules jdk.localedata
 * @bug 8146750
 * @summary Test Narrow and NarrowStandalone month names are retrieved correctly.
 */
package test.java.time.format;

import static org.testng.Assert.assertEquals;

import java.time.DayOfWeek;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestNarrowMonthNamesAndDayNames {

    static {
        System.setProperty("java.locale.providers", "COMPAT");
    }

    private static final List<Month> MONTHVALUES = Arrays.asList(Month.values());
    private static final List<DayOfWeek> DAYVALUES = Arrays.asList(DayOfWeek.values());
    private static final List<TextStyle> TEXTSTYLELIST = Arrays.asList(TextStyle.NARROW,
            TextStyle.NARROW_STANDALONE);
    private static final List<Locale> LOCARR = Arrays.asList(Locale.US,
            Locale.GERMANY,
            Locale.FRANCE,
            new Locale("no", "NO"));

    /**
     * Locale en_US, de_DE, fr_FR, no_NO will have same Narrow and
     * Narrow_Standalone month Names for COMPAT Provider.
     */
    @DataProvider(name = "MonthNarrows")
    public Object[][] monthNameData() {
        return new Object[][]{{new String[]{
            "J",
            "F",
            "M",
            "A",
            "M",
            "J",
            "J",
            "A",
            "S",
            "O",
            "N",
            "D"
        }},};
    }

    //-----------------------------------------------------------------------
    // Check Narrow and Narrow_standalone month name values
    //-----------------------------------------------------------------------
    @Test(dataProvider = "MonthNarrows")
    public void compareMonthNarrowValues(String[] monthNarrowExpected) {
        LOCARR.forEach((loc) -> {
            TEXTSTYLELIST.forEach((style) -> {
                MONTHVALUES.forEach((value) -> {
                    String result = value.getDisplayName(style, loc);
                    int index = value.ordinal();
                    assertEquals(result, monthNarrowExpected[index], "Test failed"
                            + " for COMPAT Provider for locale "
                            + loc + " for style " + style.name()
                            + " with Month value " + value.name());
                });
            });
        });
    }

    /**
     * Locale en_US, de_DE, fr_FR, no_NO will have different Narrow and
     * Narrow_Standalone Day Names for COMPAT Provider.
     */
    @DataProvider(name = "DayNarrows")
    public Object[][] dayNameData() {
        return new Object[][]{
            {Locale.US, new String[]{"M", "T", "W", "T", "F", "S", "S"}},
            {Locale.GERMANY, new String[]{"M", "D", "M", "D", "F", "S", "S"}},
            {Locale.FRANCE, new String[]{"L", "M", "M", "J", "V", "S", "D"}},
            {new Locale("no", "NO"), new String[]{"M", "T", "O", "T", "F", "L", "S"}},};
    }

    //-----------------------------------------------------------------------
    // Check Narrow and Narrow_standalone Day name values
    //-----------------------------------------------------------------------
    @Test(dataProvider = "DayNarrows")
    public void compareDayNarrowValues(Locale locale, String[] dayNarrowExpected) {
        TEXTSTYLELIST.forEach((style) -> {
            DAYVALUES.forEach((value) -> {
                String result = value.getDisplayName(style, locale);
                int index = value.ordinal();
                assertEquals(result, dayNarrowExpected[index], "Test failed"
                        + " for COMPAT Provider for locale "
                        + locale + " for style " + style.name()
                        + " with Day value " + value.name());
            });
        });
    }
}
