/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @test
 * @bug 8176841 8194148
 * @summary Tests *Format class deals with Unicode extensions
 *      correctly.
 * @modules jdk.localedata
 * @run testng/othervm -Djava.locale.providers=CLDR FormatTests
 */

import static org.testng.Assert.assertEquals;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test *Format classes with BCP47 U extensions
 */
@Test
public class FormatTests {
    private static TimeZone defaultTZ;

    private static final TimeZone ASIATOKYO = TimeZone.getTimeZone("Asia/Tokyo");
    private static final TimeZone AMLA = TimeZone.getTimeZone("America/Los_Angeles");

    private static final Locale JPTYO = Locale.forLanguageTag("en-u-tz-jptyo");
    private static final Locale JCAL = Locale.forLanguageTag("en-u-ca-japanese");
    private static final Locale USLAX = Locale.forLanguageTag("en-u-tz-uslax");

    private static final Locale RG_GB = Locale.forLanguageTag("en-US-u-rg-gbzzzz");
    private static final Locale RG_DE = Locale.forLanguageTag("en-US-u-rg-dezzzz");

    private static final Locale NU_DEVA = Locale.forLanguageTag("en-US-u-nu-deva");
    private static final Locale NU_SINH = Locale.forLanguageTag("en-US-u-nu-sinh");
    private static final Locale NU_ZZZZ = Locale.forLanguageTag("en-US-u-nu-zzzz");

    private static final double testNum = 12345.6789;
    private static final String NUM_US = "12,345.6789";
    private static final String NUM_DE = "12.345,6789";
    private static final String NUM_DEVA = "\u0967\u0968,\u0969\u096a\u096b.\u096c\u096d\u096e\u096f";
    private static final String NUM_SINH = "\u0de7\u0de8,\u0de9\u0dea\u0deb.\u0dec\u0ded\u0dee\u0def";

    private static final Date testDate = new Calendar.Builder()
                                        .setCalendarType("gregory")
                                        .setDate(2017, 7, 10)
                                        .setTimeOfDay(15, 15, 0)
                                        .setTimeZone(AMLA)
                                        .build()
                                        .getTime();

    @BeforeTest
    public void beforeTest() {
        defaultTZ = TimeZone.getDefault();
        TimeZone.setDefault(AMLA);
    }

    @AfterTest
    public void afterTest() {
        TimeZone.setDefault(defaultTZ);
    }

    @DataProvider(name="dateFormatData")
    Object[][] dateFormatData() {
        return new Object[][] {
            // Locale, Expected calendar, Expected timezone, Expected formatted string

            // -ca
            {JCAL, "java.util.JapaneseImperialCalendar", null,
            "Thursday, August 10, 29 Heisei at 3:15:00 PM Pacific Daylight Time"
            },

            // -tz
            {JPTYO, null, ASIATOKYO,
            "Friday, August 11, 2017 at 7:15:00 AM Japan Standard Time"
            },
            {USLAX, null, AMLA,
            "Thursday, August 10, 2017 at 3:15:00 PM Pacific Daylight Time"
            },

            // -rg
            {RG_GB, null, null,
            "Thursday, 10 August 2017 at 15:15:00 Pacific Daylight Time"
            },
        };
    }

    @DataProvider(name="numberFormatData")
    Object[][] numberFormatData() {
        return new Object[][] {
            // Locale, number, expected format

            // -nu
            {NU_DEVA, testNum, NUM_DEVA},
            {NU_SINH, testNum, NUM_SINH},
            {NU_ZZZZ, testNum, NUM_US},

            // -rg
            {RG_DE, testNum, NUM_DE},

            // -nu & -rg, valid & invalid
            {Locale.forLanguageTag("en-US-u-nu-deva-rg-dezzzz"), testNum, NUM_DEVA},
            {Locale.forLanguageTag("en-US-u-nu-zzzz-rg-dezzzz"), testNum, NUM_US},
            {Locale.forLanguageTag("en-US-u-nu-zzzz-rg-zzzz"), testNum, NUM_US},
        };
    }

    @Test(dataProvider="dateFormatData")
    public void test_DateFormat(Locale locale, String calClass, TimeZone tz,
                                String formatExpected) throws Exception {
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, locale);
        if (calClass != null) {
            try {
                Class expected = Class.forName(calClass);
            assertEquals(df.getCalendar().getClass(), expected);
            } catch (Exception e) {
                throw e;
            }
        }
        if (tz != null) {
            assertEquals(df.getTimeZone(), tz);
        }
        String formatted = df.format(testDate);
        assertEquals(formatted, formatExpected);
        assertEquals(df.parse(formatted), testDate);
    }

    @Test(dataProvider="numberFormatData")
    public void test_NumberFormat(Locale locale, double num,
                                String formatExpected) throws Exception {
        NumberFormat nf = NumberFormat.getNumberInstance(locale);
        nf.setMaximumFractionDigits(4);
        String formatted = nf.format(num);
        assertEquals(nf.format(num), formatExpected);
        assertEquals(nf.parse(formatted), num);
    }
}
