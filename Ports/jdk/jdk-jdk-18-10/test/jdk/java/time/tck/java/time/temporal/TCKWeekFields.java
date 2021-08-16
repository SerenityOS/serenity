/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright (c) 2008-2012, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tck.java.time.temporal;

import static java.time.format.ResolverStyle.LENIENT;
import static java.time.format.ResolverStyle.SMART;
import static java.time.format.ResolverStyle.STRICT;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.time.temporal.ValueRange;
import java.time.temporal.WeekFields;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import tck.java.time.AbstractTCKTest;

/**
 * Test WeekFields.
 */
@Test
public class TCKWeekFields extends AbstractTCKTest {

    @Test(dataProvider="weekFields")
    public void test_of_DayOfWeek_int_singleton(DayOfWeek firstDayOfWeek, int minDays) {
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        assertEquals(week.getFirstDayOfWeek(), firstDayOfWeek, "Incorrect firstDayOfWeek");
        assertEquals(week.getMinimalDaysInFirstWeek(), minDays, "Incorrect MinimalDaysInFirstWeek");
        assertSame(WeekFields.of(firstDayOfWeek, minDays), week);
    }

    //-----------------------------------------------------------------------
    @Test(dataProvider="weekFields")
    public void test_basics(DayOfWeek firstDayOfWeek, int minDays) {
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        assertEquals(week.dayOfWeek().isDateBased(), true);
        assertEquals(week.dayOfWeek().isTimeBased(), false);

        assertEquals(week.weekOfMonth().isDateBased(), true);
        assertEquals(week.weekOfMonth().isTimeBased(), false);

        assertEquals(week.weekOfYear().isDateBased(), true);
        assertEquals(week.weekOfYear().isTimeBased(), false);

        assertEquals(week.weekOfWeekBasedYear().isDateBased(), true);
        assertEquals(week.weekOfWeekBasedYear().isTimeBased(), false);

        assertEquals(week.weekBasedYear().isDateBased(), true);
        assertEquals(week.weekBasedYear().isTimeBased(), false);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_dayOfWeekField_simpleGet() {
        LocalDate date = LocalDate.of(2000, 1, 10);  // Known to be ISO Monday
        assertEquals(date.get(WeekFields.ISO.dayOfWeek()), 1);
        assertEquals(date.get(WeekFields.of(DayOfWeek.MONDAY, 1).dayOfWeek()), 1);
        assertEquals(date.get(WeekFields.of(DayOfWeek.MONDAY, 7).dayOfWeek()), 1);
        assertEquals(date.get(WeekFields.SUNDAY_START.dayOfWeek()), 2);
        assertEquals(date.get(WeekFields.of(DayOfWeek.SUNDAY, 1).dayOfWeek()), 2);
        assertEquals(date.get(WeekFields.of(DayOfWeek.SUNDAY, 7).dayOfWeek()), 2);
        assertEquals(date.get(WeekFields.of(DayOfWeek.SATURDAY, 1).dayOfWeek()), 3);
        assertEquals(date.get(WeekFields.of(DayOfWeek.FRIDAY, 1).dayOfWeek()), 4);
        assertEquals(date.get(WeekFields.of(DayOfWeek.TUESDAY, 1).dayOfWeek()), 7);
    }

    @Test
    public void test_dayOfWeekField_simpleSet() {
        LocalDate date = LocalDate.of(2000, 1, 10);  // Known to be ISO Monday
        assertEquals(date.with(WeekFields.ISO.dayOfWeek(), 2), LocalDate.of(2000, 1, 11));
        assertEquals(date.with(WeekFields.ISO.dayOfWeek(), 7), LocalDate.of(2000, 1, 16));

        assertEquals(date.with(WeekFields.SUNDAY_START.dayOfWeek(), 3), LocalDate.of(2000, 1, 11));
        assertEquals(date.with(WeekFields.SUNDAY_START.dayOfWeek(), 7), LocalDate.of(2000, 1, 15));

        assertEquals(date.with(WeekFields.of(DayOfWeek.SATURDAY, 1).dayOfWeek(), 4), LocalDate.of(2000, 1, 11));
        assertEquals(date.with(WeekFields.of(DayOfWeek.TUESDAY, 1).dayOfWeek(), 1), LocalDate.of(2000, 1, 4));
    }

    @Test(dataProvider="weekFields")
    public void test_dayOfWeekField(DayOfWeek firstDayOfWeek, int minDays) {
        LocalDate day = LocalDate.of(2000, 1, 10);  // Known to be ISO Monday
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField f = week.dayOfWeek();

        for (int i = 1; i <= 7; i++) {
            assertEquals(day.get(f), (7 + day.getDayOfWeek().getValue() - firstDayOfWeek.getValue()) % 7 + 1);
            day = day.plusDays(1);
        }
    }

    @Test(dataProvider="weekFields")
    public void test_weekOfMonthField(DayOfWeek firstDayOfWeek, int minDays) {
        LocalDate day = LocalDate.of(2012, 12, 31);  // Known to be ISO Monday
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField dowField = week.dayOfWeek();
        TemporalField womField = week.weekOfMonth();

        for (int i = 1; i <= 15; i++) {
            int actualDOW = day.get(dowField);
            int actualWOM = day.get(womField);

            // Verify that the combination of day of week and week of month can be used
            // to reconstruct the same date.
            LocalDate day1 = day.withDayOfMonth(1);
            int offset = - (day1.get(dowField) - 1);

            int week1 = day1.get(womField);
            if (week1 == 0) {
                // week of the 1st is partial; start with first full week
                offset += 7;
            }

            offset += actualDOW - 1;
            offset += (actualWOM - 1) * 7;
            LocalDate result = day1.plusDays(offset);

            assertEquals(result, day, "Incorrect dayOfWeek or weekOfMonth: "
                    + String.format("%s, ISO Dow: %s, offset: %s, actualDOW: %s, actualWOM: %s, expected: %s, result: %s%n",
                    week, day.getDayOfWeek(), offset, actualDOW, actualWOM, day, result));
            day = day.plusDays(1);
        }
    }

    @Test(dataProvider="weekFields")
    public void test_weekOfYearField(DayOfWeek firstDayOfWeek, int minDays) {
        LocalDate day = LocalDate.of(2012, 12, 31);  // Known to be ISO Monday
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField dowField = week.dayOfWeek();
        TemporalField woyField = week.weekOfYear();

        for (int i = 1; i <= 15; i++) {
            int actualDOW = day.get(dowField);
            int actualWOY = day.get(woyField);

            // Verify that the combination of day of week and week of month can be used
            // to reconstruct the same date.
            LocalDate day1 = day.withDayOfYear(1);
            int offset = - (day1.get(dowField) - 1);
            int week1 = day1.get(woyField);
            if (week1 == 0) {
                // week of the 1st is partial; start with first full week
                offset += 7;
            }
            offset += actualDOW - 1;
            offset += (actualWOY - 1) * 7;
            LocalDate result = day1.plusDays(offset);

            assertEquals(result, day, "Incorrect dayOfWeek or weekOfYear "
                    + String.format("%s, ISO Dow: %s, offset: %s, actualDOW: %s, actualWOM: %s, expected: %s, result: %s%n",
                    week, day.getDayOfWeek(), offset, actualDOW, actualWOY, day, result));
            day = day.plusDays(1);
        }
    }

    /**
     * Verify that the date can be reconstructed from the DOW, WeekOfWeekBasedYear,
     * and WeekBasedYear for every combination of start of week
     * and minimal days in week.
     * @param firstDayOfWeek the first day of the week
     * @param minDays the minimum number of days in the week
     */
    @Test(dataProvider="weekFields")
    public void test_weekOfWeekBasedYearField(DayOfWeek firstDayOfWeek, int minDays) {
        LocalDate day = LocalDate.of(2012, 12, 31);  // Known to be ISO Monday
        WeekFields weekDef = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField dowField = weekDef.dayOfWeek();
        TemporalField wowbyField = weekDef.weekOfWeekBasedYear();
        TemporalField yowbyField = weekDef.weekBasedYear();

        for (int i = 1; i <= 15; i++) {
            int actualDOW = day.get(dowField);
            int actualWOWBY = day.get(wowbyField);
            int actualYOWBY = day.get(yowbyField);

            // Verify that the combination of day of week and week of month can be used
            // to reconstruct the same date.
            LocalDate day1 = LocalDate.of(actualYOWBY, 1, 1);
            DayOfWeek isoDOW = day1.getDayOfWeek();
            int dow = (7 + isoDOW.getValue() - firstDayOfWeek.getValue()) % 7 + 1;

            int weekStart = Math.floorMod(1 - dow, 7);
            if (weekStart + 1 > weekDef.getMinimalDaysInFirstWeek()) {
                // The previous week has the minimum days in the current month to be a 'week'
                weekStart -= 7;
            }
            weekStart += actualDOW - 1;
            weekStart += (actualWOWBY - 1) * 7;
            LocalDate result = day1.plusDays(weekStart);

            assertEquals(result, day, "Incorrect dayOfWeek or weekOfYear "
                    + String.format("%s, ISO Dow: %s, weekStart: %s, actualDOW: %s, actualWOWBY: %s, YearOfWBY: %d, expected day: %s, result: %s%n",
                    weekDef, day.getDayOfWeek(), weekStart, actualDOW, actualWOWBY, actualYOWBY, day, result));
            day = day.plusDays(1);
        }
    }

    @Test(dataProvider="weekFields")
    public void test_fieldRanges(DayOfWeek firstDayOfWeek, int minDays) {
        WeekFields weekDef = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField womField = weekDef.weekOfMonth();
        TemporalField woyField = weekDef.weekOfYear();

        LocalDate day = LocalDate.of(2012, 11, 30);
        LocalDate endDay = LocalDate.of(2013, 1, 2);
        while (day.isBefore(endDay)) {
            LocalDate last = day.with(DAY_OF_MONTH, day.lengthOfMonth());
            int lastWOM = last.get(womField);
            LocalDate first = day.with(DAY_OF_MONTH, 1);
            int firstWOM = first.get(womField);
            ValueRange rangeWOM = day.range(womField);
            assertEquals(rangeWOM.getMinimum(), firstWOM,
                    "Range min should be same as WeekOfMonth for first day of month: "
                    + first + ", " + weekDef);
            assertEquals(rangeWOM.getMaximum(), lastWOM,
                    "Range max should be same as WeekOfMonth for last day of month: "
                    + last + ", " + weekDef);

            last = day.with(DAY_OF_YEAR, day.lengthOfYear());
            int lastWOY = last.get(woyField);
            first = day.with(DAY_OF_YEAR, 1);
            int firstWOY = first.get(woyField);
            ValueRange rangeWOY = day.range(woyField);
            assertEquals(rangeWOY.getMinimum(), firstWOY,
                    "Range min should be same as WeekOfYear for first day of Year: "
                    + day + ", " + weekDef);
            assertEquals(rangeWOY.getMaximum(), lastWOY,
                    "Range max should be same as WeekOfYear for last day of Year: "
                    + day + ", " + weekDef);

            day = day.plusDays(1);
        }
    }

    //-----------------------------------------------------------------------
    // withDayOfWeek()
    //-----------------------------------------------------------------------
    @Test(dataProvider="weekFields")
    public void test_withDayOfWeek(DayOfWeek firstDayOfWeek, int minDays) {
        LocalDate day = LocalDate.of(2012, 12, 15);  // Safely in the middle of a month
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField dowField = week.dayOfWeek();
        TemporalField womField = week.weekOfMonth();
        TemporalField woyField = week.weekOfYear();

        int wom = day.get(womField);
        int woy = day.get(woyField);
        for (int dow = 1; dow <= 7; dow++) {
            LocalDate result = day.with(dowField, dow);
            assertEquals(result.get(dowField), dow, String.format("Incorrect new Day of week: %s", result));
            assertEquals(result.get(womField), wom, "Week of Month should not change");
            assertEquals(result.get(woyField), woy, "Week of Year should not change");
        }
    }

    @Test(dataProvider="weekFields")
    public void test_rangeWeekOfWeekBasedYear(DayOfWeek firstDayOfWeek, int minDays) {
        WeekFields weekFields = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField dowField = weekFields.dayOfWeek();
        TemporalField wowByField = weekFields.weekOfWeekBasedYear();

        LocalDate day1 = LocalDate.of(2012, 1, weekFields.getMinimalDaysInFirstWeek());
        day1 = day1.with(wowByField, 1).with(dowField, 1);

        LocalDate day2 = LocalDate.of(2013, 1, weekFields.getMinimalDaysInFirstWeek());
        day2 = day2.with(wowByField, 1).with(dowField, 1);

        int expectedWeeks = (int)ChronoUnit.DAYS.between(day1, day2) / 7;

        ValueRange range = day1.range(wowByField);
        assertEquals(range.getMaximum(), expectedWeeks, "Range incorrect");
    }

    @Test(dataProvider="weekFields")
    public void test_withWeekOfWeekBasedYear(DayOfWeek firstDayOfWeek, int minDays) {
        LocalDate day = LocalDate.of(2012, 12, 31);
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField dowField = week.dayOfWeek();
        TemporalField wowbyField = week.weekOfWeekBasedYear();
        TemporalField yowbyField = week.weekBasedYear();

        int dowExpected = (day.get(dowField) - 1) % 7 + 1;
        LocalDate dowDate = day.with(dowField, dowExpected);
        int dowResult = dowDate.get(dowField);
        assertEquals(dowResult, dowExpected, "Localized DayOfWeek not correct; " + day + " -->" + dowDate);

        int weekExpected = day.get(wowbyField) + 1;
        ValueRange range = day.range(wowbyField);
        weekExpected = ((weekExpected - 1) % (int)range.getMaximum()) + 1;
        LocalDate weekDate = day.with(wowbyField, weekExpected);
        int weekResult = weekDate.get(wowbyField);
        assertEquals(weekResult, weekExpected, "Localized WeekOfWeekBasedYear not correct; " + day + " -->" + weekDate);

        int yearExpected = day.get(yowbyField) + 1;

        LocalDate yearDate = day.with(yowbyField, yearExpected);
        int yearResult = yearDate.get(yowbyField);
        assertEquals(yearResult, yearExpected, "Localized WeekBasedYear not correct; " + day  + " --> " + yearDate);

        range = yearDate.range(wowbyField);
        weekExpected = Math.min(day.get(wowbyField), (int)range.getMaximum());

        int weekActual = yearDate.get(wowbyField);
        assertEquals(weekActual, weekExpected, "Localized WeekOfWeekBasedYear week should not change; " + day + " --> " + yearDate + ", actual: " + weekActual + ", weekExpected: " + weekExpected);
    }

    //-----------------------------------------------------------------------
    @Test(dataProvider="weekFields")
    public void test_parse_resolve_localizedWom(DayOfWeek firstDayOfWeek, int minDays) {
        LocalDate date = LocalDate.of(2012, 12, 15);
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField womField = week.weekOfMonth();

        for (int i = 1; i <= 60; i++) {
            DateTimeFormatter f = new DateTimeFormatterBuilder()
                    .appendValue(YEAR).appendLiteral(':')
                    .appendValue(MONTH_OF_YEAR).appendLiteral(':')
                    .appendValue(womField).appendLiteral(':')
                    .appendValue(DAY_OF_WEEK).toFormatter().withResolverStyle(SMART);
            String str = date.getYear() + ":" + date.getMonthValue() + ":" +
                    date.get(womField) + ":" + date.get(DAY_OF_WEEK);
            LocalDate parsed = LocalDate.parse(str, f);
            assertEquals(parsed, date, " ::" + str + "::" + i);

            date = date.plusDays(1);
        }
    }

    @Test(dataProvider="weekFields")
    public void test_parse_resolve_localizedWom_lenient(DayOfWeek firstDayOfWeek, int minDays) {
        LocalDate date = LocalDate.of(2012, 12, 15);
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField womField = week.weekOfMonth();

        for (int i = 1; i <= 60; i++) {
            DateTimeFormatter f = new DateTimeFormatterBuilder()
                    .appendValue(YEAR).appendLiteral(':')
                    .appendValue(MONTH_OF_YEAR).appendLiteral(':')
                    .appendValue(womField).appendLiteral(':')
                    .appendValue(DAY_OF_WEEK).toFormatter().withResolverStyle(LENIENT);
            int wom = date.get(womField);
            int dow = date.get(DAY_OF_WEEK);
            for (int j = wom - 10; j < wom + 10; j++) {
                String str = date.getYear() + ":" + date.getMonthValue() + ":" + j + ":" + dow;
                LocalDate parsed = LocalDate.parse(str, f);
                assertEquals(parsed, date.plusWeeks(j - wom), " ::" + str + ": :" + i + "::" + j);
            }

            date = date.plusDays(1);
        }
    }

    @Test(dataProvider="weekFields")
    public void test_parse_resolve_localizedWom_strict(DayOfWeek firstDayOfWeek, int minDays) {
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField womField = week.weekOfMonth();
        DateTimeFormatter f = new DateTimeFormatterBuilder()
                .appendValue(YEAR).appendLiteral(':')
                .appendValue(MONTH_OF_YEAR).appendLiteral(':')
                .appendValue(womField).appendLiteral(':')
                .appendValue(DAY_OF_WEEK).toFormatter().withResolverStyle(STRICT);
        String str = "2012:1:0:1";
        try {
            LocalDate date = LocalDate.parse(str, f);
            assertEquals(date.getYear(), 2012);
            assertEquals(date.getMonthValue(), 1);
            assertEquals(date.get(womField), 0);
            assertEquals(date.get(DAY_OF_WEEK), 1);
        } catch (DateTimeException ex) {
            // expected
        }
    }

    //-----------------------------------------------------------------------
    @Test(dataProvider="weekFields")
    public void test_parse_resolve_localizedWomDow(DayOfWeek firstDayOfWeek, int minDays) {
        LocalDate date = LocalDate.of(2012, 12, 15);
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField dowField = week.dayOfWeek();
        TemporalField womField = week.weekOfMonth();

        for (int i = 1; i <= 15; i++) {
            DateTimeFormatter f = new DateTimeFormatterBuilder()
                    .appendValue(YEAR).appendLiteral(':')
                    .appendValue(MONTH_OF_YEAR).appendLiteral(':')
                    .appendValue(womField).appendLiteral(':')
                    .appendValue(dowField).toFormatter();
            String str = date.getYear() + ":" + date.getMonthValue() + ":" +
                    date.get(womField) + ":" + date.get(dowField);
            LocalDate parsed = LocalDate.parse(str, f);
            assertEquals(parsed, date, " :: " + str + " " + i);

            date = date.plusDays(1);
        }
    }

    @Test(dataProvider="weekFields")
    public void test_parse_resolve_localizedWomDow_lenient(DayOfWeek firstDayOfWeek, int minDays) {
        LocalDate date = LocalDate.of(2012, 12, 15);
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField dowField = week.dayOfWeek();
        TemporalField womField = week.weekOfMonth();

        for (int i = 1; i <= 60; i++) {
            DateTimeFormatter f = new DateTimeFormatterBuilder()
                    .appendValue(YEAR).appendLiteral(':')
                    .appendValue(MONTH_OF_YEAR).appendLiteral(':')
                    .appendValue(womField).appendLiteral(':')
                    .appendValue(dowField).toFormatter().withResolverStyle(LENIENT);
            int wom = date.get(womField);
            int dow = date.get(dowField);
            for (int j = wom - 10; j < wom + 10; j++) {
                String str = date.getYear() + ":" + date.getMonthValue() + ":" + j + ":" + dow;
                LocalDate parsed = LocalDate.parse(str, f);
                assertEquals(parsed, date.plusWeeks(j - wom), " ::" + str + ": :" + i + "::" + j);
            }

            date = date.plusDays(1);
        }
    }

    //-----------------------------------------------------------------------
    @Test(dataProvider="weekFields")
    public void test_parse_resolve_localizedWoy(DayOfWeek firstDayOfWeek, int minDays) {
        LocalDate date = LocalDate.of(2012, 12, 15);
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField woyField = week.weekOfYear();

        for (int i = 1; i <= 60; i++) {
            DateTimeFormatter f = new DateTimeFormatterBuilder()
                    .appendValue(YEAR).appendLiteral(':')
                    .appendValue(woyField).appendLiteral(':')
                    .appendValue(DAY_OF_WEEK).toFormatter();
            String str = date.getYear() + ":" +
                    date.get(woyField) + ":" + date.get(DAY_OF_WEEK);
            LocalDate parsed = LocalDate.parse(str, f);
            assertEquals(parsed, date, " :: " + str + " " + i);

            date = date.plusDays(1);
        }
    }

    @Test(dataProvider="weekFields")
    public void test_parse_resolve_localizedWoy_lenient(DayOfWeek firstDayOfWeek, int minDays) {
        LocalDate date = LocalDate.of(2012, 12, 15);
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField woyField = week.weekOfYear();

        for (int i = 1; i <= 60; i++) {
            DateTimeFormatter f = new DateTimeFormatterBuilder()
                    .appendValue(YEAR).appendLiteral(':')
                    .appendValue(woyField).appendLiteral(':')
                    .appendValue(DAY_OF_WEEK).toFormatter().withResolverStyle(LENIENT);
            int woy = date.get(woyField);
            int dow = date.get(DAY_OF_WEEK);
            for (int j = woy - 60; j < woy + 60; j++) {
                String str = date.getYear() + ":" + j + ":" + dow;
                LocalDate parsed = LocalDate.parse(str, f);
                assertEquals(parsed, date.plusWeeks(j - woy), " ::" + str + ": :" + i + "::" + j);
            }

            date = date.plusDays(1);
        }
    }

    @Test(dataProvider="weekFields")
    public void test_parse_resolve_localizedWoy_strict(DayOfWeek firstDayOfWeek, int minDays) {
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField woyField = week.weekOfYear();
        DateTimeFormatter f = new DateTimeFormatterBuilder()
                .appendValue(YEAR).appendLiteral(':')
                .appendValue(woyField).appendLiteral(':')
                .appendValue(DAY_OF_WEEK).toFormatter().withResolverStyle(STRICT);
        String str = "2012:0:1";
        try {
            LocalDate date = LocalDate.parse(str, f);
            assertEquals(date.getYear(), 2012);
            assertEquals(date.get(woyField), 0);
            assertEquals(date.get(DAY_OF_WEEK), 1);
        } catch (DateTimeException ex) {
            // expected
        }
    }

    //-----------------------------------------------------------------------
    @Test(dataProvider="weekFields")
    public void test_parse_resolve_localizedWoyDow(DayOfWeek firstDayOfWeek, int minDays) {
        LocalDate date = LocalDate.of(2012, 12, 15);
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField dowField = week.dayOfWeek();
        TemporalField woyField = week.weekOfYear();

        for (int i = 1; i <= 60; i++) {
            DateTimeFormatter f = new DateTimeFormatterBuilder()
                    .appendValue(YEAR).appendLiteral(':')
                    .appendValue(MONTH_OF_YEAR).appendLiteral(':')
                    .appendValue(woyField).appendLiteral(':')
                    .appendValue(dowField).toFormatter();
            String str = date.getYear() + ":" + date.getMonthValue() + ":" +
                    date.get(woyField) + ":" + date.get(dowField);
            LocalDate parsed = LocalDate.parse(str, f);
            assertEquals(parsed, date, " :: " + str + " " + i);

            date = date.plusDays(1);
        }
    }

    @Test(dataProvider="weekFields")
    public void test_parse_resolve_localizedWoyDow_lenient(DayOfWeek firstDayOfWeek, int minDays) {
        LocalDate date = LocalDate.of(2012, 12, 15);
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField dowField = week.dayOfWeek();
        TemporalField woyField = week.weekOfYear();

        for (int i = 1; i <= 60; i++) {
            DateTimeFormatter f = new DateTimeFormatterBuilder()
                    .appendValue(YEAR).appendLiteral(':')
                    .appendValue(woyField).appendLiteral(':')
                    .appendValue(dowField).toFormatter().withResolverStyle(LENIENT);
            int woy = date.get(woyField);
            int dow = date.get(dowField);
            for (int j = woy - 60; j < woy + 60; j++) {
                String str = date.getYear() + ":" + j + ":" + dow;
                LocalDate parsed = LocalDate.parse(str, f);
                assertEquals(parsed, date.plusWeeks(j - woy), " ::" + str + ": :" + i + "::" + j);
            }

            date = date.plusDays(1);
        }
    }

    //-----------------------------------------------------------------------
    @Test(dataProvider="weekFields")
    public void test_parse_resolve_localizedWoWBY(DayOfWeek firstDayOfWeek, int minDays) {
        LocalDate date = LocalDate.of(2012, 12, 31);
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField wowbyField = week.weekOfWeekBasedYear();
        TemporalField yowbyField = week.weekBasedYear();

        for (int i = 1; i <= 60; i++) {
            DateTimeFormatter f = new DateTimeFormatterBuilder()
                    .appendValue(yowbyField).appendLiteral(':')
                    .appendValue(wowbyField).appendLiteral(':')
                    .appendValue(DAY_OF_WEEK).toFormatter();
            String str = date.get(yowbyField) + ":" + date.get(wowbyField) + ":" +
                    date.get(DAY_OF_WEEK);
            LocalDate parsed = LocalDate.parse(str, f);
            assertEquals(parsed, date, " :: " + str + " " + i);

            date = date.plusDays(1);
        }
    }

    @Test(dataProvider="weekFields")
    public void test_parse_resolve_localizedWoWBY_lenient(DayOfWeek firstDayOfWeek, int minDays) {
        LocalDate date = LocalDate.of(2012, 12, 31);
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField wowbyField = week.weekOfWeekBasedYear();
        TemporalField yowbyField = week.weekBasedYear();

        for (int i = 1; i <= 60; i++) {
            DateTimeFormatter f = new DateTimeFormatterBuilder()
                    .appendValue(yowbyField).appendLiteral(':')
                    .appendValue(wowbyField).appendLiteral(':')
                    .appendValue(DAY_OF_WEEK).toFormatter().withResolverStyle(LENIENT);
            int wowby = date.get(wowbyField);
            int dow = date.get(DAY_OF_WEEK);
            for (int j = wowby - 60; j < wowby + 60; j++) {
                String str = date.get(yowbyField) + ":" + j + ":" + dow;
                LocalDate parsed = LocalDate.parse(str, f);
                assertEquals(parsed, date.plusWeeks(j - wowby), " ::" + str + ": :" + i + "::" + j);
            }

            date = date.plusDays(1);
        }
    }

    @Test(dataProvider="weekFields")
    public void test_parse_resolve_localizedWoWBY_strict(DayOfWeek firstDayOfWeek, int minDays) {
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField wowbyField = week.weekOfWeekBasedYear();
        TemporalField yowbyField = week.weekBasedYear();
        DateTimeFormatter f = new DateTimeFormatterBuilder()
                .appendValue(yowbyField).appendLiteral(':')
                .appendValue(wowbyField).appendLiteral(':')
                .appendValue(DAY_OF_WEEK).toFormatter().withResolverStyle(STRICT);
        String str = "2012:0:1";
        try {
            LocalDate date = LocalDate.parse(str, f);
            assertEquals(date.get(yowbyField), 2012);
            assertEquals(date.get(wowbyField), 0);
            assertEquals(date.get(DAY_OF_WEEK), 1);
        } catch (DateTimeException ex) {
            // expected
        }
    }

    //-----------------------------------------------------------------------
    @Test(dataProvider="weekFields")
    public void test_parse_resolve_localizedWoWBYDow(DayOfWeek firstDayOfWeek, int minDays) {
        LocalDate date = LocalDate.of(2012, 12, 31);
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField dowField = week.dayOfWeek();
        TemporalField wowbyField = week.weekOfWeekBasedYear();
        TemporalField yowbyField = week.weekBasedYear();

        for (int i = 1; i <= 60; i++) {
            DateTimeFormatter f = new DateTimeFormatterBuilder()
                    .appendValue(yowbyField).appendLiteral(':')
                    .appendValue(wowbyField).appendLiteral(':')
                    .appendValue(dowField).toFormatter();
            String str = date.get(yowbyField) + ":" + date.get(wowbyField) + ":" +
                    date.get(dowField);
            LocalDate parsed = LocalDate.parse(str, f);
            assertEquals(parsed, date, " :: " + str + " " + i);

            date = date.plusDays(1);
        }
    }

    @Test(dataProvider="weekFields")
    public void test_parse_resolve_localizedWoWBYDow_lenient(DayOfWeek firstDayOfWeek, int minDays) {
        LocalDate date = LocalDate.of(2012, 12, 31);
        WeekFields week = WeekFields.of(firstDayOfWeek, minDays);
        TemporalField dowField = week.dayOfWeek();
        TemporalField wowbyField = week.weekOfWeekBasedYear();
        TemporalField yowbyField = week.weekBasedYear();

        for (int i = 1; i <= 60; i++) {
            DateTimeFormatter f = new DateTimeFormatterBuilder()
                    .appendValue(yowbyField).appendLiteral(':')
                    .appendValue(wowbyField).appendLiteral(':')
                    .appendValue(dowField).toFormatter().withResolverStyle(LENIENT);
            int wowby = date.get(wowbyField);
            int dow = date.get(dowField);
            for (int j = wowby - 60; j < wowby + 60; j++) {
                String str = date.get(yowbyField) + ":" + j + ":" + dow;
                LocalDate parsed = LocalDate.parse(str, f);
                assertEquals(parsed, date.plusWeeks(j - wowby), " ::" + str + ": :" + i + "::" + j);
            }

            date = date.plusDays(1);
        }
    }


    //-----------------------------------------------------------------------
    @DataProvider(name="weekFields")
    Object[][] data_weekFields() {
        Object[][] objects = new Object[49][];
        int i = 0;
        for (DayOfWeek firstDayOfWeek : DayOfWeek.values()) {
            for (int minDays = 1; minDays <= 7; minDays++) {
                objects[i++] = new Object[] {firstDayOfWeek, minDays};
            }
        }
        return objects;
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="WeekBasedYearData")
    Object[][] provider_WeekBasedYearData() {
        return new Object[][] {
            {WeekFields.of(DayOfWeek.SUNDAY, 1),  2008, 52, 7, LocalDate.of(2008, 12, 27)},
            {WeekFields.of(DayOfWeek.SUNDAY, 1),  2009,  1, 1, LocalDate.of(2008, 12, 28)},
            {WeekFields.of(DayOfWeek.SUNDAY, 1),  2009,  1, 2, LocalDate.of(2008, 12, 29)},
            {WeekFields.of(DayOfWeek.SUNDAY, 1),  2009,  1, 3, LocalDate.of(2008, 12, 30)},
            {WeekFields.of(DayOfWeek.SUNDAY, 1),  2009,  1, 4, LocalDate.of(2008, 12, 31)},
            {WeekFields.of(DayOfWeek.SUNDAY, 1),  2009,  1, 5, LocalDate.of(2009, 1, 1)},
            {WeekFields.of(DayOfWeek.SUNDAY, 1),  2009,  2, 1, LocalDate.of(2009, 1, 4)},
            {WeekFields.of(DayOfWeek.SUNDAY, 1),  2009,  2, 2, LocalDate.of(2009, 1, 5)},
            {WeekFields.of(DayOfWeek.SUNDAY, 1),  2009,  2, 3, LocalDate.of(2009, 1, 6)},
       };
    }

    @Test(dataProvider="WeekBasedYearData")
    public void test_weekBasedYears(WeekFields weekDef, int weekBasedYear,
            int weekOfWeekBasedYear, int dayOfWeek, LocalDate date) {
        TemporalField dowField = weekDef.dayOfWeek();
        TemporalField wowbyField = weekDef.weekOfWeekBasedYear();
        TemporalField yowbyField = weekDef.weekBasedYear();
        assertEquals(date.get(dowField), dayOfWeek, "DayOfWeek mismatch");
        assertEquals(date.get(wowbyField), weekOfWeekBasedYear, "Week of WeekBasedYear mismatch");
        assertEquals(date.get(yowbyField), weekBasedYear, "Year of WeekBasedYear mismatch");
    }


    //-----------------------------------------------------------------------
    @DataProvider(name="IsoWeekData")
    Object[][] data_week() {
        return new Object[][] {
                {LocalDate.of(1969, 12, 29), DayOfWeek.MONDAY, 1, 1970},
                {LocalDate.of(2012, 12, 23), DayOfWeek.SUNDAY, 51, 2012},
                {LocalDate.of(2012, 12, 24), DayOfWeek.MONDAY, 52, 2012},
                {LocalDate.of(2012, 12, 27), DayOfWeek.THURSDAY, 52, 2012},
                {LocalDate.of(2012, 12, 28), DayOfWeek.FRIDAY, 52, 2012},
                {LocalDate.of(2012, 12, 29), DayOfWeek.SATURDAY, 52, 2012},
                {LocalDate.of(2012, 12, 30), DayOfWeek.SUNDAY, 52, 2012},
                {LocalDate.of(2012, 12, 31), DayOfWeek.MONDAY, 1, 2013},
                {LocalDate.of(2013, 1, 1), DayOfWeek.TUESDAY, 1, 2013},
                {LocalDate.of(2013, 1, 2), DayOfWeek.WEDNESDAY, 1, 2013},
                {LocalDate.of(2013, 1, 6), DayOfWeek.SUNDAY, 1, 2013},
                {LocalDate.of(2013, 1, 7), DayOfWeek.MONDAY, 2, 2013},
        };
    }

    //-----------------------------------------------------------------------
    // WEEK_OF_WEEK_BASED_YEAR
    // Validate with the same data used by IsoFields.
    //-----------------------------------------------------------------------
    @Test(dataProvider="IsoWeekData")
    public void test_WOWBY(LocalDate date, DayOfWeek dow, int week, int wby) {
        WeekFields weekDef = WeekFields.ISO;
        TemporalField dowField = weekDef.dayOfWeek();
        TemporalField wowbyField = weekDef.weekOfWeekBasedYear();
        TemporalField yowbyField = weekDef.weekBasedYear();

        assertEquals(date.get(dowField), dow.getValue());
        assertEquals(date.get(wowbyField), week);
        assertEquals(date.get(yowbyField), wby);
    }

    //-----------------------------------------------------------------------
    // equals() and hashCode().
    //-----------------------------------------------------------------------
    @Test
    public void test_equals() {
        WeekFields weekDef_iso = WeekFields.ISO;
        WeekFields weekDef_sundayStart = WeekFields.SUNDAY_START;

        assertTrue(weekDef_iso.equals(WeekFields.of(DayOfWeek.MONDAY, 4)));
        assertTrue(weekDef_sundayStart.equals(WeekFields.of(DayOfWeek.SUNDAY, 1)));
        assertEquals(weekDef_iso.hashCode(), WeekFields.of(DayOfWeek.MONDAY, 4).hashCode());
        assertEquals(weekDef_sundayStart.hashCode(), WeekFields.of(DayOfWeek.SUNDAY, 1).hashCode());

        assertFalse(weekDef_iso.equals(weekDef_sundayStart));
        assertNotEquals(weekDef_iso.hashCode(), weekDef_sundayStart.hashCode());
    }

}
