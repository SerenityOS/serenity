/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

package test.java.time.chrono;

import java.time.*;
import java.time.chrono.*;
import java.time.temporal.*;
import java.util.List;
import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Tests for the Japanese chronology
 */
@Test
public class TestJapaneseChronology {
    private static final JapaneseChronology JAPANESE = JapaneseChronology.INSTANCE;
    private static final Locale jaJPJP = Locale.forLanguageTag("ja-JP-u-ca-japanese");

    @DataProvider(name="transitions")
    Object[][] transitionData() {
        return new Object[][] {
            // Japanese era, yearOfEra, month, dayOfMonth, gregorianYear
            { JapaneseEra.MEIJI,      6,  1,  1, 1873 },
            // Meiji-Taisho transition isn't accurate. 1912-07-30 is the last day of Meiji
            // and the first day of Taisho.
            { JapaneseEra.MEIJI,     45,  7, 29, 1912 },
            { JapaneseEra.TAISHO,     1,  7, 30, 1912 },
            // Same for Taisho-Showa transition. 1926-12-25 is the last day of Taisho
            // and the first day of Showa.
            { JapaneseEra.TAISHO,    15, 12, 24, 1926 },
            { JapaneseEra.SHOWA,      1, 12, 25, 1926 },
            { JapaneseEra.SHOWA,     64,  1,  7, 1989 },
            { JapaneseEra.HEISEI,     1,  1,  8, 1989 },
            { JapaneseEra.HEISEI,    31,  4, 30, 2019 },
            { JapaneseEra.REIWA,      1,  5,  1, 2019 },
        };
    }

    @DataProvider(name="day_year_data")
    Object[][] dayYearData() {
        return new Object[][] {
            // Japanese era, yearOfEra, dayOfYear, month, dayOfMonth
            { JapaneseEra.MEIJI,  45,  211,  7, 29 },
            { JapaneseEra.TAISHO,  1,    1,  7, 30 },
            { JapaneseEra.TAISHO,  2,   60,  3,  1 },
            { JapaneseEra.TAISHO, 15,  358, 12, 24 },
            { JapaneseEra.SHOWA,   1,    1, 12, 25 },
            { JapaneseEra.SHOWA,   2,    8,  1,  8 },
            { JapaneseEra.SHOWA,  64,    7,  1,  7 },
            { JapaneseEra.HEISEI,  1,    1,  1,  8 },
            { JapaneseEra.HEISEI,  2,    8,  1,  8 },
            { JapaneseEra.HEISEI, 31,  120,  4, 30 },
            { JapaneseEra.REIWA,   1,    1,  5,  1 },
        };
    }

    @DataProvider(name="range_data")
    Object[][] rangeData() {
        return new Object[][] {
            // field, minSmallest, minLargest, maxSmallest, maxLargest
            { ChronoField.ERA,         -1, -1, 3, 3},
            { ChronoField.YEAR_OF_ERA, 1, 1, 15, 999999999-2019}, // depends on the current era
            { ChronoField.DAY_OF_YEAR, 1, 1, 7, 366},
            { ChronoField.YEAR, 1873, 1873, 999999999, 999999999},
        };
    }

    @DataProvider(name="invalid_dates")
    Object[][] invalidDatesData() {
        return new Object[][] {
            // Japanese era, yearOfEra, month, dayOfMonth
            { JapaneseEra.MEIJI,      6,  2, 29 },
            { JapaneseEra.MEIJI,     45,  7, 30 },
            { JapaneseEra.MEIJI,     46,  1,  1 },
            { JapaneseEra.TAISHO,     1,  7, 29 },
            { JapaneseEra.TAISHO,     2,  2, 29 },
            { JapaneseEra.TAISHO,    15, 12, 25 },
            { JapaneseEra.TAISHO,    16,  1,  1 },
            { JapaneseEra.SHOWA,      1, 12, 24 },
            { JapaneseEra.SHOWA,      2,  2, 29 },
            { JapaneseEra.SHOWA,     64,  1,  8 },
            { JapaneseEra.SHOWA,     65,  1,  1 },
            { JapaneseEra.HEISEI,     1,  1,  7 },
            { JapaneseEra.HEISEI,     1,  2, 29 },
            { JapaneseEra.HEISEI,    31,  5,  1 },
            { JapaneseEra.REIWA,      1,  4, 30 },
            { JapaneseEra.REIWA, Year.MAX_VALUE,  12, 31 },
        };
    }

    @DataProvider(name="invalid_eraYear")
    Object[][] invalidEraYearData() {
        return new Object[][] {
            // Japanese era, yearOfEra
            { JapaneseEra.MEIJI,     -1 },
            { JapaneseEra.MEIJI,      0 },
            { JapaneseEra.MEIJI,     46 },
            { JapaneseEra.TAISHO,    -1 },
            { JapaneseEra.TAISHO,     0 },
            { JapaneseEra.TAISHO,    16 },
            { JapaneseEra.SHOWA,     -1 },
            { JapaneseEra.SHOWA,      0 },
            { JapaneseEra.SHOWA,     65 },
            { JapaneseEra.HEISEI,    -1 },
            { JapaneseEra.HEISEI,     0 },
            { JapaneseEra.HEISEI,    32 },
            { JapaneseEra.REIWA,     -1 },
            { JapaneseEra.REIWA,      0 },
            { JapaneseEra.REIWA, Year.MAX_VALUE },
        };
    }

    @DataProvider(name="invalid_day_year_data")
    Object[][] invalidDayYearData() {
        return new Object[][] {
            // Japanese era, yearOfEra, dayOfYear
            { JapaneseEra.MEIJI,  45, 240 },
            { JapaneseEra.TAISHO,  1, 365 },
            { JapaneseEra.TAISHO,  2, 366 },
            { JapaneseEra.TAISHO, 15, 359 },
            { JapaneseEra.SHOWA,   1,   8 },
            { JapaneseEra.SHOWA,   2, 366 },
            { JapaneseEra.SHOWA,  64,   8 },
            { JapaneseEra.HEISEI,  1, 360 },
            { JapaneseEra.HEISEI,  2, 366 },
            { JapaneseEra.HEISEI, 31, 121 },
            { JapaneseEra.REIWA,   1, 246 },
            { JapaneseEra.REIWA,   2, 367 },
        };
    }

    @DataProvider
    Object[][] eraNameData() {
        return new Object[][] {
            // Japanese era, name, exception
            { "Meiji",  JapaneseEra.MEIJI,      null },
            { "Taisho", JapaneseEra.TAISHO,     null },
            { "Showa",  JapaneseEra.SHOWA,      null },
            { "Heisei", JapaneseEra.HEISEI,     null },
            { "Reiwa", JapaneseEra.REIWA,       null },
            { "NewEra", null,                   IllegalArgumentException.class},
        };
    }

    @Test
    public void test_ofLocale() {
        // must be a singleton
        assertEquals(Chronology.ofLocale(jaJPJP) == JAPANESE, true);
    }

    @Test(dataProvider="transitions")
    public void test_transitions(JapaneseEra era, int yearOfEra, int month, int dayOfMonth, int gregorianYear) {
        assertEquals(JAPANESE.prolepticYear(era, yearOfEra), gregorianYear);

        JapaneseDate jdate1 = JapaneseDate.of(era, yearOfEra, month, dayOfMonth);
        JapaneseDate jdate2 = JapaneseDate.of(gregorianYear, month, dayOfMonth);
        assertEquals(jdate1, jdate2);
    }

    @Test(dataProvider="range_data")
    public void test_range(ChronoField field, int minSmallest, int minLargest, int maxSmallest, int maxLargest) {
        ValueRange range = JAPANESE.range(field);
        assertEquals(range.getMinimum(), minSmallest);
        assertEquals(range.getLargestMinimum(), minLargest);
        assertEquals(range.getSmallestMaximum(), maxSmallest);
        assertEquals(range.getMaximum(), maxLargest);
    }

    @Test(dataProvider="day_year_data")
    public void test_firstDayOfEra(JapaneseEra era, int yearOfEra, int dayOfYear, int month, int dayOfMonth) {
        JapaneseDate date1 = JAPANESE.dateYearDay(era, yearOfEra, dayOfYear);
        JapaneseDate date2 = JAPANESE.date(era, yearOfEra, month, dayOfMonth);
        assertEquals(date1, date2);
    }

    @Test(dataProvider="invalid_dates", expectedExceptions=DateTimeException.class)
    public void test_invalidDate(JapaneseEra era, int yearOfEra, int month, int dayOfMonth) {
        JapaneseDate jdate = JapaneseDate.of(era, yearOfEra, month, dayOfMonth);
        System.out.printf("No DateTimeException with %s %d.%02d.%02d%n", era, yearOfEra, month, dayOfMonth);
    }

    @Test(dataProvider="invalid_eraYear", expectedExceptions=DateTimeException.class)
    public void test_invalidEraYear(JapaneseEra era, int yearOfEra) {
        int year = JAPANESE.prolepticYear(era, yearOfEra);
        System.out.printf("No DateTimeException with era=%s, year=%d%n", era, yearOfEra);
    }

    @Test(dataProvider="invalid_day_year_data", expectedExceptions=DateTimeException.class)
    public void test_invalidDayYear(JapaneseEra era, int yearOfEra, int dayOfYear) {
        JapaneseDate date = JAPANESE.dateYearDay(era, yearOfEra, dayOfYear);
        System.out.printf("No DateTimeException with era=%s, year=%d, dayOfYear=%d%n", era, yearOfEra, dayOfYear);
    }

    @Test(dataProvider="eraNameData")
    public void test_eraName(String eraName, JapaneseEra era, Class expectedEx) {
        try {
            assertEquals(JapaneseEra.valueOf(eraName), era);
        } catch (Exception ex) {
            assertTrue(expectedEx.isInstance(ex));
        }
    }
}
