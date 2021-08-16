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

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoPeriod;
import java.time.chrono.ChronoZonedDateTime;
import java.time.chrono.Chronology;
import java.time.chrono.HijrahChronology;
import java.time.chrono.HijrahDate;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.JapaneseDate;
import java.time.chrono.MinguoChronology;
import java.time.chrono.MinguoDate;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.chrono.ThaiBuddhistDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.ValueRange;
import java.time.temporal.WeekFields;
import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests for the Umm alQura chronology and data.
 * Note: The dates used for testing are just a sample of calendar data.
 * @bug 8067800
 */
@Test
public class TestUmmAlQuraChronology {

    private static final ZoneOffset OFFSET_PTWO = ZoneOffset.ofHours(2);
    private static final ZoneId ZONE_RIYADH = ZoneId.of("Asia/Riyadh");

    // Test for HijrahChronology Aliases
    @Test
    public void test_aliases() {
        HijrahChronology hc = (HijrahChronology) Chronology.of("Hijrah");
        assertEquals(hc, HijrahChronology.INSTANCE, "Alias for Hijrah-umalqura");
        hc = (HijrahChronology) Chronology.of("islamic");
        assertEquals(hc, HijrahChronology.INSTANCE, "Alias for Hijrah-umalqura");
    }

    // Test to check if the exception is thrown for an incorrect chronology id
    @Test(expectedExceptions=DateTimeException.class)
    public void test_badChronology() {
        Chronology test = Chronology.of("Hijrah-ummalqura");
    }

    //--------------------------------------------------------------------------
    // regular data factory for Umm alQura dates and the corresponding ISO dates
    //--------------------------------------------------------------------------
    @DataProvider(name = "UmmAlQuraVsISODates")
    Object[][] data_UmmAlQuraVsISODates() {
        return new Object[][] {
            {HijrahDate.of(1318, 1, 1), LocalDate.of(1900, 04, 30)},
            {HijrahDate.of(1318, 12, 29), LocalDate.of(1901, 04, 19)},
            {HijrahDate.of(1319, 01, 01), LocalDate.of(1901, 04, 20)},
            {HijrahDate.of(1433, 12, 29), LocalDate.of(2012, 11, 14)},
            {HijrahDate.of(1434, 01, 01), LocalDate.of(2012, 11, 15)},
            {HijrahDate.of(1434, 02, 18), LocalDate.of(2012, 12, 31)},
            {HijrahDate.of(1502, 12, 29), LocalDate.of(2079, 10, 25)},
        };
    }

    // Test to verify the epoch days for given Hijrah & ISO date instances
    @Test(dataProvider="UmmAlQuraVsISODates")
        public void Test_UmmAlQuraVsISODates(HijrahDate hd, LocalDate ld) {
        assertEquals(hd.toEpochDay(), ld.toEpochDay(), "Umm alQura date and ISO date should have same epochDay");
    }

    // UmmAlQura chronology ranges for year, month and days for the HijrahChronology
    @Test
    public void Test_UmmAlQuraChronoRange() {
        HijrahChronology chrono = HijrahChronology.INSTANCE;
        ValueRange year = chrono.range(YEAR);
        assertEquals(year.getMinimum(), 1300, "Minimum year");
        assertEquals(year.getLargestMinimum(), 1300, "Largest minimum year");
        assertEquals(year.getMaximum(), 1600, "Largest year");
        assertEquals(year.getSmallestMaximum(), 1600, "Smallest Maximum year");

        ValueRange month = chrono.range(MONTH_OF_YEAR);
        assertEquals(month.getMinimum(), 1, "Minimum month");
        assertEquals(month.getLargestMinimum(), 1, "Largest minimum month");
        assertEquals(month.getMaximum(), 12, "Largest month");
        assertEquals(month.getSmallestMaximum(), 12, "Smallest Maximum month");

        ValueRange day = chrono.range(DAY_OF_MONTH);
        assertEquals(day.getMinimum(), 1, "Minimum day");
        assertEquals(day.getLargestMinimum(), 1, "Largest minimum day");
        assertEquals(day.getMaximum(), 30, "Largest day");
        assertEquals(day.getSmallestMaximum(), 29, "Smallest Maximum day");
    }

    //-----------------------------------------------------------------------
    // regular data factory for dates and the corresponding range values
    //-----------------------------------------------------------------------
    @DataProvider(name = "dates")
    Object[][] data_dates() {
        return new Object[][]{
            {HijrahDate.of(1300, 5, 1), 1300, 1600, 1, 12, 1, 30, 30},
            {HijrahDate.of(1300, 6, 1), 1300, 1600, 1, 12, 1, 29, 30},
            {HijrahDate.of(1434, 12, 1), 1300, 1600, 1, 12, 1, 29, 30},
            {HijrahDate.of(1500, 4, 1), 1300, 1600, 1, 12, 1, 30, 30},
            {HijrahDate.of(1600, 6, 1), 1300, 1600, 1, 12, 1, 29, 30},
        };
    }

    // Test to verify the min/max field ranges for given dates
    @Test(dataProvider="dates")
    public void Test_UmmAlQuraRanges(HijrahDate date,
                        int minYear, int maxYear,
                        int minMonth, int maxMonth,
                        int minDay, int maxDay, int maxChronoDay) {
        // Check the chronology ranges
        HijrahChronology chrono = date.getChronology();
        ValueRange yearRange = chrono.range(YEAR);
        assertEquals(yearRange.getMinimum(), minYear, "Minimum year for Hijrah chronology");
        assertEquals(yearRange.getLargestMinimum(), minYear, "Largest minimum year for Hijrah chronology");
        assertEquals(yearRange.getMaximum(), maxYear, "Maximum year for Hijrah chronology");
        assertEquals(yearRange.getSmallestMaximum(), maxYear, "Smallest Maximum year for Hijrah chronology");

        ValueRange monthRange = chrono.range(MONTH_OF_YEAR);
        assertEquals(monthRange.getMinimum(), minMonth, "Minimum month for Hijrah chronology");
        assertEquals(monthRange.getMaximum(), maxMonth, "Maximum month for Hijrah chronology");

        ValueRange daysRange = chrono.range(DAY_OF_MONTH);
        assertEquals(daysRange.getMinimum(), minDay, "Minimum day for chronology");
        assertEquals(daysRange.getMaximum(), maxChronoDay, "Maximum day for Hijrah chronology");

        // Check the date ranges
        yearRange = date.range(YEAR);
        assertEquals(yearRange.getMinimum(), minYear, "Minimum year for Hijrah date");
        assertEquals(yearRange.getLargestMinimum(), minYear, "Largest minimum  year for Hijrah date");
        assertEquals(yearRange.getMaximum(), maxYear, "Maximum year for Hijrah date");
        assertEquals(yearRange.getSmallestMaximum(), maxYear, "Smallest maximum year for Hijrah date");

        monthRange = date.range(MONTH_OF_YEAR);
        assertEquals(monthRange.getMinimum(), minMonth, "Minimum month for HijrahDate");
        assertEquals(monthRange.getMaximum(), maxMonth, "Maximum month for HijrahDate");

        daysRange = date.range(DAY_OF_MONTH);
        assertEquals(daysRange.getMinimum(), minDay, "Minimum day for HijrahDate");
        assertEquals(daysRange.getMaximum(), maxDay, "Maximum day for HijrahDate");

    }

    // Check the date limits
    @Test
    public void test_hijrahDateLimits() {
        HijrahChronology chrono = HijrahChronology.INSTANCE;
        ValueRange yearRange = chrono.range(YEAR);
        ValueRange monthRange = chrono.range(MONTH_OF_YEAR);
        ValueRange dayRange = chrono.range(DAY_OF_MONTH);

        HijrahDate xx = chrono.date(1434, 1, 1);
        HijrahDate minDate = chrono.date((int)yearRange.getLargestMinimum(),
                (int)monthRange.getMinimum(), (int)dayRange.getMinimum());
        try {
            HijrahDate before = minDate.minus(1, ChronoUnit.DAYS);
            fail("Exception did not occur, minDate: " + minDate + ".minus(1, DAYS) = " + before);

        } catch (DateTimeException ex) {
            // ignore, this exception was expected
        }

        HijrahDate maxDate = chrono.date((int)yearRange.getSmallestMaximum(),
                (int)monthRange.getMaximum(), 1);
        int monthLen = maxDate.lengthOfMonth();
        maxDate = maxDate.with(DAY_OF_MONTH, monthLen);
        try {
            HijrahDate after = maxDate.plus(1, ChronoUnit.DAYS);
            fail("Exception did not occur, maxDate: " + maxDate + ".plus(1, DAYS) = " + after);
        } catch (DateTimeException ex) {
            // ignore, this exception was expected
        }
    }

    // Data provider to verify the dateYearDay() method
    @DataProvider(name="dateYearDay")
    Object[][] data_dateYearDay() {
        return new Object[][] {
            {HijrahChronology.INSTANCE.dateYearDay(1434, 42), HijrahChronology.INSTANCE.date(1434, 02, 13)},
            {HijrahChronology.INSTANCE.dateYearDay(1330, 354), HijrahChronology.INSTANCE.date(1330, 12, 29)},
            {HijrahChronology.INSTANCE.dateYearDay(1600, 1), HijrahChronology.INSTANCE.date(1600, 1, 1)},
            {HijrahChronology.INSTANCE.dateYearDay(1400, 175), HijrahChronology.INSTANCE.date(1400, 6, 28)},
            {HijrahChronology.INSTANCE.dateYearDay(1520, 190), HijrahChronology.INSTANCE.date(1520, 7, 13)},
            {HijrahChronology.INSTANCE.dateYearDay(1521, 112), HijrahChronology.INSTANCE.date(1521, 4, 25)},
        };
    }

    // Test to verify the dateYearDay() method
    @Test(dataProvider="dateYearDay")
    public void test_DateYearDay(ChronoLocalDate date1,   ChronoLocalDate date2) {
       assertEquals(date1, date2);
    }

    //-----------------------------------------------------------------------
    // HijrahDate.with(DAY_OF_YEAR, n)
    //-----------------------------------------------------------------------
    @Test
    public void test_getDayOfYear() {
        HijrahDate hd1 = HijrahChronology.INSTANCE.dateYearDay(1434, 1);
        for (int i = 1; i <= hd1.lengthOfYear(); i++) {
            HijrahDate hd = HijrahChronology.INSTANCE.dateYearDay(1434, i);
            int doy = hd.get(DAY_OF_YEAR);
            assertEquals(doy, i, "get(DAY_OF_YEAR) incorrect for " + i);
        }
    }

    @Test
    public void test_withDayOfYear() {
        HijrahDate hd = HijrahChronology.INSTANCE.dateYearDay(1434, 1);
        for (int i = 1; i <= hd.lengthOfYear(); i++) {
            HijrahDate hd2 = hd.with(DAY_OF_YEAR, i);
            int doy = hd2.get(DAY_OF_YEAR);
            assertEquals(doy, i, "with(DAY_OF_YEAR) incorrect for " + i + " " + hd2);
        }
    }

    @Test(expectedExceptions=java.time.DateTimeException.class)
    public void test_withDayOfYearTooSmall() {
        HijrahDate hd = HijrahChronology.INSTANCE.dateYearDay(1435, 1);
        HijrahDate hd2 = hd.with(DAY_OF_YEAR, 0);
    }

    @Test(expectedExceptions=java.time.DateTimeException.class)
    public void test_withDayOfYearTooLarge() {
        HijrahDate hd = HijrahChronology.INSTANCE.dateYearDay(1435, 1);
        HijrahDate hd2 = hd.with(DAY_OF_YEAR, hd.lengthOfYear() + 1);
    }

    // Test to verify the with() method with ChronoField is set to DAY_OF_WEEK
    @Test
    public void test_adjustWithDayOfWeek() {
        assertEquals(HijrahChronology.INSTANCE.date(1320, 1, 15).with(ChronoField.DAY_OF_WEEK, 4), HijrahDate.of(1320, 1, 15));
        assertEquals(HijrahChronology.INSTANCE.date(1421, 11, 15).with(ChronoField.DAY_OF_WEEK, 1), HijrahDate.of(1421, 11, 11));
        assertEquals(HijrahChronology.INSTANCE.date(1529, 7, 18).with(ChronoField.DAY_OF_WEEK, 6), HijrahDate.of(1529, 7, 20));
        assertEquals(HijrahChronology.INSTANCE.date(1534, 2, 10).with(ChronoField.DAY_OF_WEEK, 5), HijrahDate.of(1534, 2, 12));
        assertEquals(HijrahChronology.INSTANCE.date(1552, 4, 1).with(ChronoField.DAY_OF_WEEK, 2), HijrahDate.of(1552, 3, 26));
    }

    // Test to verify the with() method with ChronoField is set to DAY_OF_MONTH
    @Test
    public void test_adjustWithDayOfMonth() {
        assertEquals(HijrahChronology.INSTANCE.date(1320, 1, 15).with(ChronoField.DAY_OF_MONTH, 2), HijrahDate.of(1320, 1, 2));
        assertEquals(HijrahChronology.INSTANCE.date(1421, 11, 15).with(ChronoField.DAY_OF_MONTH, 9), HijrahDate.of(1421, 11, 9));
        assertEquals(HijrahChronology.INSTANCE.date(1529, 7, 18).with(ChronoField.DAY_OF_MONTH, 13), HijrahDate.of(1529, 7, 13));
        assertEquals(HijrahChronology.INSTANCE.date(1534, 12, 10).with(ChronoField.DAY_OF_MONTH, 29), HijrahDate.of(1534, 12, 29));
        assertEquals(HijrahChronology.INSTANCE.date(1552, 4, 1).with(ChronoField.DAY_OF_MONTH, 6), HijrahDate.of(1552, 4, 6));
    }

    // Test to verify the with() method with ChronoField is set to DAY_OF_YEAR
    @Test
    public void test_adjustWithDayOfYear() {
        assertEquals(HijrahChronology.INSTANCE.date(1320, 1, 15).with(ChronoField.DAY_OF_YEAR, 24), HijrahDate.of(1320, 1, 24));
        assertEquals(HijrahChronology.INSTANCE.date(1421, 11, 15).with(ChronoField.DAY_OF_YEAR, 135), HijrahDate.of(1421, 5, 18));
        assertEquals(HijrahChronology.INSTANCE.date(1529, 7, 18).with(ChronoField.DAY_OF_YEAR, 64), HijrahDate.of(1529, 3, 5));
        assertEquals(HijrahChronology.INSTANCE.date(1534, 2, 10).with(ChronoField.DAY_OF_YEAR, 354), HijrahDate.of(1534, 12, 29));
        assertEquals(HijrahChronology.INSTANCE.date(1552, 4, 1).with(ChronoField.DAY_OF_YEAR, 291), HijrahDate.of(1552, 10, 26));
    }

    // Data provider to get the difference between two dates in terms of days, months and years
    @DataProvider(name="datesForDiff")
    Object[][] data_datesForDiffs() {
        return new Object[][] {
            {HijrahDate.of(1350, 5, 15), HijrahDate.of(1351, 12, 29), 574, 19, 1},
            {HijrahDate.of(1434, 5, 1), HijrahDate.of(1434,6, 12), 40, 1, 0},
            {HijrahDate.of(1436, 1, 1), HijrahDate.of(1475, 12, 29), 14173, 479, 39},
            {HijrahDate.of(1500, 6, 12), HijrahDate.of(1551, 7, 12), 18102, 613, 51},
            {HijrahDate.of(1550, 3, 11), HijrahDate.of(1551, 4, 11), 384, 13, 1},
        };
    }

    // Test to verify the difference between two given dates in terms of days, months and years
    @Test(dataProvider="datesForDiff")
    public void test_diffBetweenDates(ChronoLocalDate from, ChronoLocalDate to, long days, long months, long years) {
          assertEquals(from.until(to, ChronoUnit.DAYS), days);
          assertEquals(from.until(to, ChronoUnit.MONTHS), months);
          assertEquals(from.until(to, ChronoUnit.YEARS), years);
    }

    // Data provider to get the difference between two dates as a period
    @DataProvider(name="datesForPeriod")
    Object[][] data_Period() {
        return new Object[][] {
            {HijrahDate.of(1350, 5, 15), HijrahDate.of(1434, 7, 20), HijrahChronology.INSTANCE.period(84, 2, 5)},
            {HijrahDate.of(1403, 5, 28), HijrahDate.of(1434, 7, 20), HijrahChronology.INSTANCE.period(31, 1, 22)},
            {HijrahDate.of(1434, 7, 20), HijrahDate.of(1484, 2, 15), HijrahChronology.INSTANCE.period(49, 6, 24)},
            {HijrahDate.of(1500, 6, 12), HijrahDate.of(1450, 4, 21), HijrahChronology.INSTANCE.period(-50, -1, -20)},
            {HijrahDate.of(1549, 3, 11), HijrahDate.of(1550, 3, 10), HijrahChronology.INSTANCE.period(0, 11, 28)},
        };
    }

    // Test to get the Period between two given dates
    @Test(dataProvider="datesForPeriod")
    public void test_until(HijrahDate h1, HijrahDate h2, ChronoPeriod p) {
        ChronoPeriod period = h1.until(h2);
        assertEquals(period, p);
    }

    // Test to get the Period between dates in different chronologies
    @Test(dataProvider="datesForPeriod")
    public void test_periodUntilDiffChrono(HijrahDate h1, HijrahDate h2, ChronoPeriod p) {
        MinguoDate m = MinguoChronology.INSTANCE.date(h2);
        ChronoPeriod period = h1.until(m);
        assertEquals(period, p);
    }

    // Test to get the adjusted date from a given date using TemporalAdjuster methods
    @Test
    public void test_temporalDayAdjustments() {
        HijrahDate date = HijrahDate.of(1554, 7, 21);
        assertEquals(date.with(TemporalAdjusters.firstDayOfMonth()), HijrahDate.of(1554, 7, 1));
        assertEquals(date.with(TemporalAdjusters.lastDayOfMonth()), HijrahDate.of(1554, 7, 29));
        assertEquals(date.with(TemporalAdjusters.firstDayOfNextMonth()), HijrahDate.of(1554, 8, 1));
        assertEquals(date.with(TemporalAdjusters.firstDayOfNextYear()), HijrahDate.of(1555, 1, 1));
        assertEquals(date.with(TemporalAdjusters.firstDayOfYear()), HijrahDate.of(1554, 1, 1));
        assertEquals(date.with(TemporalAdjusters.lastDayOfYear()), HijrahDate.of(1554, 12, 30));
    }

    // Data provider for string representation of the date instances
    @DataProvider(name="toString")
    Object[][] data_toString() {
        return new Object[][] {
            {HijrahChronology.INSTANCE.date(1320, 1, 1), "Hijrah-umalqura AH 1320-01-01"},
            {HijrahChronology.INSTANCE.date(1500, 10, 28), "Hijrah-umalqura AH 1500-10-28"},
            {HijrahChronology.INSTANCE.date(1500, 10, 29), "Hijrah-umalqura AH 1500-10-29"},
            {HijrahChronology.INSTANCE.date(1434, 12, 5), "Hijrah-umalqura AH 1434-12-05"},
            {HijrahChronology.INSTANCE.date(1434, 12, 6), "Hijrah-umalqura AH 1434-12-06"},
        };
    }

    // Test to verify the returned string value of a given date instance
    @Test(dataProvider="toString")
    public void test_toString(ChronoLocalDate hijrahDate, String expected) {
        assertEquals(hijrahDate.toString(), expected);
    }

    // Data provider for maximum number of days
    @DataProvider(name="monthDays")
    Object[][] data_monthDays() {
        return new Object[][] {
            {1432, 1, 29},
            {1432, 4, 30},
            {1433, 12, 29},
            {1434, 1, 29},
            {1435, 8, 29},
            {1435, 9, 30},
        };
    }

    // Test to verify the maximum number of days by adding one month to a given date
    @Test (dataProvider="monthDays")
    public void test_valueRange_monthDays(int year, int month, int maxlength) {
        ChronoLocalDate date = HijrahChronology.INSTANCE.date(year, month, 1);
        ValueRange range = null;
        for (int i=1; i<=12; i++) {
            range = date.range(ChronoField.DAY_OF_MONTH);
            date = date.plus(1, ChronoUnit.MONTHS);
            assertEquals(range.getMaximum(), month, maxlength);
        }
    }

    // Test to get the last day of the month by adjusting the date with lastDayOfMonth() method
    @Test(dataProvider="monthDays")
    public void test_lastDayOfMonth(int year, int month, int numDays) {
        HijrahDate hDate = HijrahChronology.INSTANCE.date(year, month, 1);
        hDate = hDate.with(TemporalAdjusters.lastDayOfMonth());
        assertEquals(hDate.get(ChronoField.DAY_OF_MONTH), numDays);
    }

    // Data provider for the 12 islamic month names in a formatted date
    @DataProvider(name="patternMonthNames")
    Object[][] data_patternMonthNames() {
        return new Object[][] {
            {1434, 1, 1, "01 AH Thu Muharram 1434"},
            {1434, 2, 1, "01 AH Fri Safar 1434"},
            {1434, 3, 1, "01 AH Sun Rabi\u02bb I 1434"},//the actual month name is Rabi Al-Awwal, but the locale data contains short form.
            {1434, 4, 1, "01 AH Mon Rabi\u02bb II 1434"},//the actual month name is Rabi Al-Akhar, but the locale data contains short form.
            {1434, 5, 1, "01 AH Wed Jumada I 1434"},//the actual month name is Jumada Al-Awwal, but the locale data contains short form.
            {1434, 6, 1, "01 AH Thu Jumada II 1434"},//the actual month name is Jumada Al-Akhar, but the locale data contains short form.
            {1434, 7, 1, "01 AH Sat Rajab 1434"},
            {1434, 8, 1, "01 AH Mon Sha\u02bbban 1434"},
            {1434, 9, 1, "01 AH Tue Ramadan 1434"},
            {1434, 10, 1, "01 AH Thu Shawwal 1434"},
            {1434, 11, 1, "01 AH Sat Dhu\u02bbl-Qi\u02bbdah 1434"},
            {1434, 12, 1, "01 AH Sun Dhu\u02bbl-Hijjah 1434"},
        };
    }

    // Test to verify the formatted dates
    @Test(dataProvider="patternMonthNames")
    public void test_ofPattern(int year, int month, int day, String expected) {
        DateTimeFormatter test = DateTimeFormatter.ofPattern("dd G E MMMM yyyy", Locale.US);
        assertEquals(test.format(HijrahDate.of(year, month, day)), expected);
    }

    // Data provider for localized dates
    @DataProvider(name="chronoDateTimes")
   Object[][] data_chronodatetimes() {
        return new Object[][] {
            {1432, 12, 29, "Safar 1, 1434 AH"},
            {1433, 1, 30, "Safar 30, 1434 AH"},
            {1434, 6, 30, "Rajab 30, 1435 AH"},
        };
    }

    // Test to verify the localized dates using ofLocalizedDate() method
    @Test(dataProvider="chronoDateTimes")
    public void test_formatterOfLocalizedDate(int year, int month, int day, String expected) {
        HijrahDate hd = HijrahChronology.INSTANCE.date(year, month, day);
        ChronoLocalDateTime<HijrahDate> hdt = hd.atTime(LocalTime.NOON);
        hdt = hdt.plus(1, ChronoUnit.YEARS);
        hdt = hdt.plus(1, ChronoUnit.MONTHS);
        hdt = hdt.plus(1, ChronoUnit.DAYS);
        hdt = hdt.plus(1, ChronoUnit.HOURS);
        hdt = hdt.plus(1, ChronoUnit.MINUTES);
        hdt = hdt.plus(1, ChronoUnit.SECONDS);
        DateTimeFormatter df = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withChronology(Chronology.of("Hijrah-umalqura")).withLocale(Locale.US);
        assertEquals(df.format(hdt), expected);
    }

    // Data provider to get the day of the week in a given date
    // The day of the week varies if the week starts with a saturday or sunday
    @DataProvider(name="dayOfWeek")
    Object[][] data_dayOfweek() {
        return new Object[][] {
            {HijrahDate.of(1434, 6, 24), 1, 7},
            {HijrahDate.of(1432, 9, 3), 5, 4},
            {HijrahDate.of(1334, 12, 29), 7, 6},
            {HijrahDate.of(1354, 5, 24), 1, 7},
            {HijrahDate.of(1465, 10, 2), 2, 1},
        };
    }

    // Test to get the day of the week based on a Saturday/Sunday as the first day of the week
    @Test(dataProvider="dayOfWeek")
    public void test_dayOfWeek(HijrahDate date, int satStart, int sunStart) {
        assertEquals(date.get(WeekFields.of(DayOfWeek.SATURDAY, 7).dayOfWeek()), satStart);
        assertEquals(date.get(WeekFields.of(DayOfWeek.SUNDAY, 7).dayOfWeek()), sunStart);
    }

    // Data sample to get the epoch days of a date instance
    @DataProvider(name="epochDays")
    Object[][] data_epochdays() {
        return new Object[][] {
            {1332, -20486},
            {1334, -19777},
            {1336, -19068},
            {1432, 14950},
            {1434, 15659},
            {1534, 51096},
            {1535, 51450},
        };
    }

    // Test to verify the number of epoch days of a date instance
    @Test(dataProvider="epochDays")
    public void test_epochDays(int y, long epoch) {
        HijrahDate date = HijrahDate.of(y, 1, 1);
        assertEquals(date.toEpochDay(), epoch);
    }

    // Data provider to verify whether a given hijrah year is a leap year or not
    @DataProvider(name="leapYears")
    Object[][] data_leapyears() {
        return new Object[][] {
            {1302, true},
            {1305, false},
            {1315, false},
            {1534, false},
            {1411, true},
            {1429, false},
            {1433, true},
            {1443, true},
        };
    }

    // Test to verify whether a given hijrah year is a leap year or not
    @Test(dataProvider="leapYears")
    public void test_leapYears(int y, boolean leapyear) {
        HijrahDate date = HijrahDate.of(y, 1, 1);
        assertEquals(date.isLeapYear(), leapyear);
    }

    // Data provider to verify that a given hijrah year is outside the range of supported years
    // The values are dependent on the currently configured UmmAlQura calendar data
    @DataProvider(name="OutOfRangeLeapYears")
    Object[][] data_invalid_leapyears() {
        return new Object[][] {
                {1299},
                {1601},
                {Integer.MAX_VALUE},
                {Integer.MIN_VALUE},
        };
    }

    @Test(dataProvider="OutOfRangeLeapYears")
    public void test_notLeapYears(int y) {
        assertFalse(HijrahChronology.INSTANCE.isLeapYear(y), "Out of range leap year");
    }


    // Date samples to convert HijrahDate to LocalDate and vice versa
    @DataProvider(name="samples")
    Object[][] data_samples() {
        return new Object[][] {
            {HijrahChronology.INSTANCE.date(1319, 12, 30), LocalDate.of(1902, 4, 9)},
            {HijrahChronology.INSTANCE.date(1320, 1, 1), LocalDate.of(1902, 4, 10)},
            {HijrahChronology.INSTANCE.date(1321, 12, 30), LocalDate.of(1904, 3, 18)},
            {HijrahChronology.INSTANCE.date(1433, 7, 29), LocalDate.of(2012, 6, 19)},
            {HijrahChronology.INSTANCE.date(1434, 10, 12), LocalDate.of(2013, 8, 19)},
            {HijrahChronology.INSTANCE.date(1500, 3, 3), LocalDate.of(2077, 1, 28)},
        };
    }

    // Test to get LocalDate instance from a given HijrahDate
    @Test(dataProvider="samples")
    public void test_toLocalDate(ChronoLocalDate hijrahDate, LocalDate iso) {
        assertEquals(LocalDate.from(hijrahDate), iso);
    }

    // Test to adjust HijrahDate with a given LocalDate
    @Test(dataProvider="samples")
    public void test_adjust_toLocalDate(ChronoLocalDate hijrahDate, LocalDate iso) {
        assertEquals(hijrahDate.with(iso), hijrahDate);
    }

    // Test to get a HijrahDate from a calendrical
    @Test(dataProvider="samples")
    public void test_fromCalendrical(ChronoLocalDate hijrahDate, LocalDate iso) {
        assertEquals(HijrahChronology.INSTANCE.date(iso), hijrahDate);
    }

    // Test to verify the day of week of a given HijrahDate and LocalDate
    @Test(dataProvider="samples")
    public void test_dayOfWeekEqualIsoDayOfWeek(ChronoLocalDate hijrahDate, LocalDate iso) {
        assertEquals(hijrahDate.get(ChronoField.DAY_OF_WEEK), iso.get(ChronoField.DAY_OF_WEEK), "Hijrah day of week should be same as ISO day of week");
    }

    // Test to get the local date by applying the MIN adjustment with hijrah date
    @Test(dataProvider="samples")
    public void test_LocalDate_adjustToHijrahDate(ChronoLocalDate hijrahDate, LocalDate localDate) {
        LocalDate test = LocalDate.MIN.with(hijrahDate);
        assertEquals(test, localDate);
    }

    // Test to get the local date time by applying the MIN adjustment with hijrah date
    @Test(dataProvider="samples")
    public void test_LocalDateTime_adjustToHijrahDate(ChronoLocalDate hijrahDate, LocalDate localDate) {
        LocalDateTime test = LocalDateTime.MIN.with(hijrahDate);
        assertEquals(test, LocalDateTime.of(localDate, LocalTime.MIDNIGHT));
    }

    // Sample dates for comparison
    @DataProvider(name="datesForComparison")
    Object[][] data_datesForComparison() {
        return new Object[][] {
            {HijrahChronology.INSTANCE.date(1434, 6, 26), LocalDate.of(2013, 5, 5), -1, 1},
            {HijrahChronology.INSTANCE.date(1433, 4, 15), LocalDate.of(2012, 3, 15), 1, -1},
            {HijrahChronology.INSTANCE.date(1432, 5, 21), LocalDate.of(2011, 4, 22), -1, 1},
            {HijrahChronology.INSTANCE.date(1433, 7, 29), LocalDate.of(2012, 6, 2), -1, 1},
            {HijrahChronology.INSTANCE.date(1434, 10, 12), LocalDate.of(2013, 8, 2), -1, 1},
        };
    }

    // Test to compare dates in both forward and reverse order
    @Test(dataProvider="datesForComparison")
    public void test_compareDates(HijrahDate hdate, LocalDate ldate, int result1, int result2) {
        assertEquals(ldate.compareTo(hdate), result1);
        assertEquals(hdate.compareTo(ldate), result2);
    }

    // Test to verify the values of various chrono fields for a given hijrah date instance
    @Test
    public void test_chronoFields() {
        ChronoLocalDate hdate = HijrahChronology.INSTANCE.date(1434, 6, 28);
        assertEquals(hdate.get(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH), 7);
        assertEquals(hdate.get(ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR), 7);
        assertEquals(hdate.get(ChronoField.ALIGNED_WEEK_OF_MONTH), 4);
        assertEquals(hdate.get(ChronoField.ALIGNED_WEEK_OF_YEAR), 25);
        assertEquals(hdate.get(ChronoField.ERA), 1);
        assertEquals(hdate.get(ChronoField.YEAR_OF_ERA), 1434);
        assertEquals(hdate.get(ChronoField.MONTH_OF_YEAR), 6);
        assertEquals(hdate.get(ChronoField.DAY_OF_MONTH), 28);
        assertEquals(hdate.get(ChronoField.DAY_OF_WEEK), 3);
        assertEquals(hdate.get(ChronoField.DAY_OF_YEAR), 175);
    }

    // Test to verify the returned hijrah date after adjusting the day of week as Saturday
    @Test
    public void test_adjustInto() {
        assertEquals(DayOfWeek.SATURDAY.adjustInto(HijrahDate.of(1434, 6, 28)), HijrahDate.of(1434, 7, 1));
        assertEquals(DayOfWeek.SATURDAY.adjustInto(HijrahDate.of(1432, 4, 13)), HijrahDate.of(1432, 4, 14));
        assertEquals(DayOfWeek.SATURDAY.adjustInto(HijrahDate.of(1433, 11, 29)), HijrahDate.of(1433, 12, 4));
        assertEquals(DayOfWeek.SATURDAY.adjustInto(HijrahDate.of(1434, 5, 10)), HijrahDate.of(1434, 5, 11));
        assertEquals(DayOfWeek.SATURDAY.adjustInto(HijrahDate.of(1434, 9, 11)), HijrahDate.of(1434, 9, 12));
    }

    //-----------------------------------------------------------------------
    // zonedDateTime(TemporalAccessor)
    //-----------------------------------------------------------------------
    @DataProvider(name="zonedDateTime")
    Object[][] data_zonedDateTime() {
        return new Object[][] {
            {ZonedDateTime.of(2012, 2, 29, 2, 7, 1, 1, ZONE_RIYADH), HijrahChronology.INSTANCE.date(1433, 4, 7), LocalTime.of(2, 7, 1, 1), null},
            {OffsetDateTime.of(2012, 2, 29, 2, 7, 1, 1, OFFSET_PTWO), HijrahChronology.INSTANCE.date(1433, 4, 7), LocalTime.of(2, 7, 1, 1), null},
            {LocalDateTime.of(2012, 2, 29, 2, 7), null, null, DateTimeException.class},
            {JapaneseDate.of(2012, 2, 29), null, null, DateTimeException.class},
            {ThaiBuddhistDate.of(2012 + 543, 2, 29), null, null, DateTimeException.class},
            {LocalDate.of(2012, 2, 29), null, null, DateTimeException.class},
            {LocalTime.of(20, 30, 29, 0), null, null, DateTimeException.class},
        };
    }

    // Test to check the zoned date times
    @Test(dataProvider="zonedDateTime")
    public void test_zonedDateTime(TemporalAccessor accessor,  HijrahDate expectedDate, LocalTime expectedTime, Class<?> expectedEx) {
        if (expectedEx == null) {
            ChronoZonedDateTime<HijrahDate> result = HijrahChronology.INSTANCE.zonedDateTime(accessor);
            assertEquals(result.toLocalDate(), expectedDate);
            assertEquals(HijrahDate.from(accessor), expectedDate);
            assertEquals(result.toLocalTime(), expectedTime);

        } else {
            try {
                ChronoZonedDateTime<HijrahDate> result = HijrahChronology.INSTANCE.zonedDateTime(accessor);
                fail();
            } catch (Exception ex) {
                assertTrue(expectedEx.isInstance(ex));
            }
        }
    }

    //-----------------------------------------------------------------------
    // zonedDateTime(Instant, ZoneId )
    //-----------------------------------------------------------------------
    @Test
    public void test_Instant_zonedDateTime() {
        OffsetDateTime offsetDateTime = OffsetDateTime.of(2012, 2, 29, 2, 7, 1, 1, OFFSET_PTWO);
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2012, 2, 29, 2, 7, 1, 1, ZONE_RIYADH);

        ChronoZonedDateTime<HijrahDate> result = HijrahChronology.INSTANCE.zonedDateTime(offsetDateTime.toInstant(), offsetDateTime.getOffset());
        assertEquals(result.toLocalDate(), HijrahChronology.INSTANCE.date(1433, 4, 7));
        assertEquals(result.toLocalTime(), LocalTime.of(2, 7, 1, 1));

        result = HijrahChronology.INSTANCE.zonedDateTime(zonedDateTime.toInstant(), zonedDateTime.getOffset());
        assertEquals(result.toLocalDate(), HijrahChronology.INSTANCE.date(1433, 4, 7));
        assertEquals(result.toLocalTime(), LocalTime.of(2, 7, 1, 1));
    }

    //-----------------------------------------------------------------------
    // localDateTime()
    //-----------------------------------------------------------------------
    @DataProvider(name="localDateTime")
    Object[][] data_localDateTime() {
        return new Object[][] {
            {LocalDateTime.of(2012, 2, 29, 2, 7), HijrahChronology.INSTANCE.date(1433, 4, 7), LocalTime.of(2, 7), null},
            {ZonedDateTime.of(2012, 2, 29, 2, 7, 1, 1, ZONE_RIYADH), HijrahChronology.INSTANCE.date(1433, 4, 7), LocalTime.of(2, 7, 1, 1), null},
            {OffsetDateTime.of(2012, 2, 29, 2, 7, 1, 1, OFFSET_PTWO), HijrahChronology.INSTANCE.date(1433, 4, 7), LocalTime.of(2, 7, 1, 1), null},
            {JapaneseDate.of(2012, 2, 29), null, null, DateTimeException.class},
            {ThaiBuddhistDate.of(2012 + 543, 2, 29), null, null, DateTimeException.class},
            {LocalDate.of(2012, 2, 29), null, null, DateTimeException.class},
            {LocalTime.of(20, 30, 29, 0), null, null, DateTimeException.class},
        };
    }

    // Test to verify local date time values from various date instances defined in the localDateTime data provider
    @Test(dataProvider="localDateTime")
    public void test_localDateTime(TemporalAccessor accessor,  HijrahDate expectedDate, LocalTime expectedTime, Class<?> expectedEx) {
        if (expectedEx == null) {
            ChronoLocalDateTime<HijrahDate> result = HijrahChronology.INSTANCE.localDateTime(accessor);
            assertEquals(result.toLocalDate(), expectedDate);
            assertEquals(HijrahDate.from(accessor), expectedDate);
            assertEquals(result.toLocalTime(), expectedTime);
        } else {
            try {
                ChronoLocalDateTime<HijrahDate> result = HijrahChronology.INSTANCE.localDateTime(accessor);
                fail();
            } catch (Exception ex) {
                assertTrue(expectedEx.isInstance(ex));
            }
        }
    }

    // Sample Hijrah & Minguo Dates
    @DataProvider(name="hijrahToMinguo")
    Object[][] data_hijrahToMinguo() {
        return new Object[][] {
            {HijrahDate.of(1350,5,15), MinguoDate.of(20,9,28)},
            {HijrahDate.of(1434,5,1), MinguoDate.of(102,3,13)},
            {HijrahDate.of(1436,1,1), MinguoDate.of(103,10,25)},
            {HijrahDate.of(1500,6,12), MinguoDate.of(166,5,5)},
            {HijrahDate.of(1550,3,11), MinguoDate.of(214,8,11)},
        };
    }

    // Test to verify the date conversion from Hijrah to Minguo chronology
    @Test(dataProvider="hijrahToMinguo")
    public void test_hijrahToMinguo(HijrahDate hijrah, MinguoDate minguo) {
        assertEquals(MinguoChronology.INSTANCE.date(hijrah), minguo);
    }

    // Sample Hijrah & Thai Dates
    @DataProvider(name="hijrahToThai")
    Object[][] data_hijrahToThai() {
        return new Object[][] {
            {HijrahDate.of(1350,5,15), ThaiBuddhistDate.of(2474,9,28)},
            {HijrahDate.of(1434,5,1), ThaiBuddhistDate.of(2556,3,13)},
            {HijrahDate.of(1436,1,1), ThaiBuddhistDate.of(2557,10,25)},
            {HijrahDate.of(1500,6,12), ThaiBuddhistDate.of(2620,5,5)},
            {HijrahDate.of(1550,3,11), ThaiBuddhistDate.of(2668,8,11)},
        };
    }

    // Test to verify the date conversion from Hijrah to Thai chronology
    @Test(dataProvider="hijrahToThai")
    public void test_hijrahToThai(HijrahDate hijrah, ThaiBuddhistDate thai) {
        assertEquals(ThaiBuddhistChronology.INSTANCE.date(hijrah), thai);
    }

    // Sample Hijrah & Japanese Dates
    @DataProvider(name="hijrahToJapanese")
    Object[][] data_hijrahToJapanese() {
        return new Object[][] {
            {HijrahDate.of(1350,5,15), "Japanese Showa 6-09-28"},
            {HijrahDate.of(1434,5,1), "Japanese Heisei 25-03-13"},
            {HijrahDate.of(1436,1,1), "Japanese Heisei 26-10-25"},
            {HijrahDate.of(1440,8,25), "Japanese Heisei 31-04-30"},
            {HijrahDate.of(1440,8,26), "Japanese Reiwa 1-05-01"},
            {HijrahDate.of(1500,6,12), "Japanese Reiwa 59-05-05"},
            {HijrahDate.of(1550,3,11), "Japanese Reiwa 107-08-11"},
        };
    }

    // Test to verify the date conversion from Hijrah to Japanese chronology
    @Test(dataProvider="hijrahToJapanese")
      public void test_hijrahToJapanese(HijrahDate hijrah, String japanese) {
          assertEquals(JapaneseChronology.INSTANCE.date(hijrah).toString(), japanese);
    }

    @DataProvider(name="alignedDayOfWeekInMonthTestDates")
    Object[][] data_alignedDayOfWeekInMonth() {
        return new Object[][] {
            {1437, 9, 1, 1, 1},
            {1437, 10, 1, 1, 1},
            {1437, 10, 11, 2, 4},
            {1437, 10, 29, 5, 1},
        };
    }

    //-----------------------------------------------------------------------
    // Test for aligned-week-of-month calculation based on the day-of-month
    //-----------------------------------------------------------------------
    @Test(dataProvider="alignedDayOfWeekInMonthTestDates")
    public void test_alignedWeekOfMonth(int year, int month, int dom, int wom, int dowm) {
        HijrahDate date = HijrahChronology.INSTANCE.date(year, month, dom);
        assertEquals(date.getLong(ChronoField.ALIGNED_WEEK_OF_MONTH), wom);
    }

    //-----------------------------------------------------------------------
    // Test for aligned-day-of-week calculation based on the day-of-month
    //-----------------------------------------------------------------------
    @Test(dataProvider="alignedDayOfWeekInMonthTestDates")
    public void test_alignedDayOfWeekInMonth(int year, int month, int dom, int wom, int dowm) {
        HijrahDate date = HijrahChronology.INSTANCE.date(year, month, dom);
        assertEquals(date.getLong(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH), dowm);
    }
}
