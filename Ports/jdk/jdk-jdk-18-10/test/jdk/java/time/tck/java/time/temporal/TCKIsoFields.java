/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.YEAR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.chrono.ThaiBuddhistDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.IsoFields;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class TCKIsoFields {

    @DataProvider(name="quarter")
    Object[][] data_quarter() {
        return new Object[][] {
                {LocalDate.of(1969, 12, 29), 90, 4},
                {LocalDate.of(1969, 12, 30), 91, 4},
                {LocalDate.of(1969, 12, 31), 92, 4},

                {LocalDate.of(1970, 1, 1), 1, 1},
                {LocalDate.of(1970, 1, 2), 2, 1},
                {LocalDate.of(1970, 2, 28), 59, 1},
                {LocalDate.of(1970, 3, 1), 60, 1},
                {LocalDate.of(1970, 3, 31), 90, 1},

                {LocalDate.of(1970, 4, 1), 1, 2},
                {LocalDate.of(1970, 6, 30), 91, 2},

                {LocalDate.of(1970, 7, 1), 1, 3},
                {LocalDate.of(1970, 9, 30), 92, 3},

                {LocalDate.of(1970, 10, 1), 1, 4},
                {LocalDate.of(1970, 12, 31), 92, 4},

                {LocalDate.of(1972, 2, 28), 59, 1},
                {LocalDate.of(1972, 2, 29), 60, 1},
                {LocalDate.of(1972, 3, 1), 61, 1},
                {LocalDate.of(1972, 3, 31), 91, 1},
        };
    }

    //-----------------------------------------------------------------------
    // DAY_OF_QUARTER
    //-----------------------------------------------------------------------
    @Test(dataProvider = "quarter")
    public void test_DOQ(LocalDate date, int doq, int qoy) {
        assertEquals(IsoFields.DAY_OF_QUARTER.getFrom(date), doq);
        assertEquals(date.get(IsoFields.DAY_OF_QUARTER), doq);
    }

    public void test_DOQ_basics() {
        assertEquals(IsoFields.DAY_OF_QUARTER.isDateBased(), true);
        assertEquals(IsoFields.DAY_OF_QUARTER.isTimeBased(), false);
    }

    //-----------------------------------------------------------------------
    // QUARTER_OF_YEAR
    //-----------------------------------------------------------------------
    @Test(dataProvider = "quarter")
    public void test_QOY(LocalDate date, int doq, int qoy) {
        assertEquals(IsoFields.QUARTER_OF_YEAR.getFrom(date), qoy);
        assertEquals(date.get(IsoFields.QUARTER_OF_YEAR), qoy);
    }

    public void test_QOY_basics() {
        assertEquals(IsoFields.QUARTER_OF_YEAR.isDateBased(), true);
        assertEquals(IsoFields.QUARTER_OF_YEAR.isTimeBased(), false);
    }

    //-----------------------------------------------------------------------
    // parse quarters
    //-----------------------------------------------------------------------
    @Test(dataProvider = "quarter")
    public void test_parse_quarters(LocalDate date, int doq, int qoy) {
        DateTimeFormatter f = new DateTimeFormatterBuilder()
                .appendValue(YEAR).appendLiteral('-')
                .appendValue(IsoFields.QUARTER_OF_YEAR).appendLiteral('-')
                .appendValue(IsoFields.DAY_OF_QUARTER)
                .toFormatter().withResolverStyle(ResolverStyle.STRICT);
        LocalDate parsed = LocalDate.parse(date.getYear() + "-" + qoy + "-" + doq, f);
        assertEquals(parsed, date);
    }

    @Test(dataProvider = "quarter")
    public void test_parse_quarters_SMART(LocalDate date, int doq, int qoy) {
        DateTimeFormatter f = new DateTimeFormatterBuilder()
                .appendValue(YEAR).appendLiteral('-')
                .appendValue(IsoFields.QUARTER_OF_YEAR).appendLiteral('-')
                .appendValue(IsoFields.DAY_OF_QUARTER)
                .toFormatter().withResolverStyle(ResolverStyle.SMART);
        LocalDate parsed = LocalDate.parse(date.getYear() + "-" + qoy + "-" + doq, f);
        assertEquals(parsed, date);
    }

    @Test(dataProvider = "quarter")
    public void test_parse_quarters_LENIENT(LocalDate date, int doq, int qoy) {
        DateTimeFormatter f = new DateTimeFormatterBuilder()
                .appendValue(YEAR).appendLiteral('-')
                .appendValue(IsoFields.QUARTER_OF_YEAR).appendLiteral('-')
                .appendValue(IsoFields.DAY_OF_QUARTER)
                .toFormatter().withResolverStyle(ResolverStyle.LENIENT);
        LocalDate parsed = LocalDate.parse(date.getYear() + "-" + qoy + "-" + doq, f);
        assertEquals(parsed, date);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="parseLenientQuarter")
    Object[][] data_parseLenientQuarter() {
        return new Object[][] {
                {"2012:0:1", LocalDate.of(2011, 10, 1), false},
                {"2012:5:1", LocalDate.of(2013, 1, 1), false},

                {"2012:1:-1", LocalDate.of(2011, 12, 30), false},
                {"2012:1:0", LocalDate.of(2011, 12, 31), false},
                {"2012:0:0", LocalDate.of(2011, 9, 30), false},

                {"2012:1:92", LocalDate.of(2012, 4, 1), true},
                {"2012:2:92", LocalDate.of(2012, 7, 1), true},
                {"2012:2:93", LocalDate.of(2012, 7, 2), false},
                {"2012:3:93", LocalDate.of(2012, 10, 1), false},
                {"2012:4:93", LocalDate.of(2013, 1, 1), false},
                {"2012:4:182", LocalDate.of(2013, 3, 31), false},
                {"2012:4:183", LocalDate.of(2013, 4, 1), false},

                {"2011:1:91", LocalDate.of(2011, 4, 1), true},
                {"2011:1:92", LocalDate.of(2011, 4, 2), true},
        };
    }

    @Test(dataProvider = "parseLenientQuarter", expectedExceptions = DateTimeParseException.class)
    public void test_parse_parseLenientQuarter_STRICT(String str, LocalDate expected, boolean smart) {
        DateTimeFormatter f = new DateTimeFormatterBuilder()
                .appendValue(YEAR).appendLiteral(':')
                .appendValue(IsoFields.QUARTER_OF_YEAR).appendLiteral(':')
                .appendValue(IsoFields.DAY_OF_QUARTER)
                .toFormatter().withResolverStyle(ResolverStyle.STRICT);
        LocalDate.parse(str, f);
    }

    @Test(dataProvider = "parseLenientQuarter")
    public void test_parse_parseLenientQuarter_SMART(String str, LocalDate expected, boolean smart) {
        DateTimeFormatter f = new DateTimeFormatterBuilder()
                .appendValue(YEAR).appendLiteral(':')
                .appendValue(IsoFields.QUARTER_OF_YEAR).appendLiteral(':')
                .appendValue(IsoFields.DAY_OF_QUARTER)
                .toFormatter().withResolverStyle(ResolverStyle.SMART);
        if (smart) {
            LocalDate parsed = LocalDate.parse(str, f);
            assertEquals(parsed, expected);
        } else {
            try {
                LocalDate.parse(str, f);
                fail("Should have failed");
            } catch (DateTimeParseException ex) {
                // expected
            }
        }
    }

    @Test(dataProvider = "parseLenientQuarter")
    public void test_parse_parseLenientQuarter_LENIENT(String str, LocalDate expected, boolean smart) {
        DateTimeFormatter f = new DateTimeFormatterBuilder()
                .appendValue(YEAR).appendLiteral(':')
                .appendValue(IsoFields.QUARTER_OF_YEAR).appendLiteral(':')
                .appendValue(IsoFields.DAY_OF_QUARTER)
                .toFormatter().withResolverStyle(ResolverStyle.LENIENT);
        LocalDate parsed = LocalDate.parse(str, f);
        assertEquals(parsed, expected);
    }

    //-----------------------------------------------------------------------
    // quarters between
    //-----------------------------------------------------------------------
    @DataProvider(name="quartersBetween")
    Object[][] data_quartersBetween() {
        return new Object[][] {
                {LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 1), 0},
                {LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 2), 0},
                {LocalDate.of(2000, 1, 1), LocalDate.of(2000, 2, 1), 0},
                {LocalDate.of(2000, 1, 1), LocalDate.of(2000, 3, 1), 0},
                {LocalDate.of(2000, 1, 1), LocalDate.of(2000, 3, 31), 0},
                {LocalDate.of(2000, 1, 1), LocalDate.of(2000, 4, 1), 1},
                {LocalDate.of(2000, 1, 1), LocalDate.of(2000, 4, 2), 1},
                {LocalDate.of(2000, 1, 1), LocalDate.of(2000, 6, 30), 1},
                {LocalDate.of(2000, 1, 1), LocalDate.of(2000, 7, 1), 2},
                {LocalDate.of(2000, 1, 1), LocalDate.of(2000, 10, 1), 3},
                {LocalDate.of(2000, 1, 1), LocalDate.of(2000, 12, 31), 3},
                {LocalDate.of(2000, 1, 1), LocalDate.of(2001, 1, 1), 4},
                {LocalDate.of(2000, 1, 1), LocalDate.of(2002, 1, 1), 8},

                {LocalDate.of(2000, 1, 1), LocalDate.of(1999, 12, 31), 0},
                {LocalDate.of(2000, 1, 1), LocalDate.of(1999, 10, 2), 0},
                {LocalDate.of(2000, 1, 1), LocalDate.of(1999, 10, 1), -1},
                {LocalDate.of(2000, 1, 1), LocalDate.of(1999, 7, 2), -1},
                {LocalDate.of(2000, 1, 1), LocalDate.of(1999, 7, 1), -2},
                {LocalDate.of(2000, 1, 1), LocalDate.of(1999, 4, 2), -2},
                {LocalDate.of(2000, 1, 1), LocalDate.of(1999, 4, 1), -3},
                {LocalDate.of(2000, 1, 1), LocalDate.of(1999, 1, 2), -3},
                {LocalDate.of(2000, 1, 1), LocalDate.of(1999, 1, 1), -4},
                {LocalDate.of(2000, 1, 1), LocalDate.of(1998, 12, 31), -4},
                {LocalDate.of(2000, 1, 1), LocalDate.of(1998, 10, 2), -4},
                {LocalDate.of(2000, 1, 1), LocalDate.of(1998, 10, 1), -5},

                {LocalDate.of(2000, 1, 1), LocalDateTime.of(2001, 4, 5, 0, 0), 5},
        };
    }

    @Test(dataProvider="quartersBetween")
    public void test_quarters_between(LocalDate start, Temporal end, long expected) {
        assertEquals(IsoFields.QUARTER_YEARS.between(start, end), expected);
    }

    @Test(dataProvider="quartersBetween")
    public void test_quarters_between_until(LocalDate start, Temporal end, long expected) {
        assertEquals(start.until(end, IsoFields.QUARTER_YEARS), expected);
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name="week")
    Object[][] data_week() {
        return new Object[][] {
                {LocalDate.of(1969, 12, 29), MONDAY, 1, 1970},
                {LocalDate.of(2012, 12, 23), SUNDAY, 51, 2012},
                {LocalDate.of(2012, 12, 24), MONDAY, 52, 2012},
                {LocalDate.of(2012, 12, 27), THURSDAY, 52, 2012},
                {LocalDate.of(2012, 12, 28), FRIDAY, 52, 2012},
                {LocalDate.of(2012, 12, 29), SATURDAY, 52, 2012},
                {LocalDate.of(2012, 12, 30), SUNDAY, 52, 2012},
                {LocalDate.of(2012, 12, 31), MONDAY, 1, 2013},
                {LocalDate.of(2013, 1, 1), TUESDAY, 1, 2013},
                {LocalDate.of(2013, 1, 2), WEDNESDAY, 1, 2013},
                {LocalDate.of(2013, 1, 6), SUNDAY, 1, 2013},
                {LocalDate.of(2013, 1, 7), MONDAY, 2, 2013},
        };
    }

    //-----------------------------------------------------------------------
    // WEEK_OF_WEEK_BASED_YEAR
    //-----------------------------------------------------------------------
    @Test(dataProvider="week")
    public void test_WOWBY(LocalDate date, DayOfWeek dow, int week, int wby) {
        assertEquals(date.getDayOfWeek(), dow);
        assertEquals(IsoFields.WEEK_OF_WEEK_BASED_YEAR.getFrom(date), week);
        assertEquals(date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), week);
    }

    public void test_WOWBY_basics() {
        assertEquals(IsoFields.WEEK_OF_WEEK_BASED_YEAR.isDateBased(), true);
        assertEquals(IsoFields.WEEK_OF_WEEK_BASED_YEAR.isTimeBased(), false);
    }

    //-----------------------------------------------------------------------
    // WEEK_BASED_YEAR
    //-----------------------------------------------------------------------
    @Test(dataProvider="week")
    public void test_WBY(LocalDate date, DayOfWeek dow, int week, int wby) {
        assertEquals(date.getDayOfWeek(), dow);
        assertEquals(IsoFields.WEEK_BASED_YEAR.getFrom(date), wby);
        assertEquals(date.get(IsoFields.WEEK_BASED_YEAR), wby);
    }

    public void test_WBY_basics() {
        assertEquals(IsoFields.WEEK_BASED_YEAR.isDateBased(), true);
        assertEquals(IsoFields.WEEK_BASED_YEAR.isTimeBased(), false);
    }

    //-----------------------------------------------------------------------
    // parse weeks
    //-----------------------------------------------------------------------
    @Test(dataProvider="week")
    public void test_parse_weeks_STRICT(LocalDate date, DayOfWeek dow, int week, int wby) {
        DateTimeFormatter f = new DateTimeFormatterBuilder()
                .appendValue(IsoFields.WEEK_BASED_YEAR).appendLiteral('-')
                .appendValue(IsoFields.WEEK_OF_WEEK_BASED_YEAR).appendLiteral('-')
                .appendValue(DAY_OF_WEEK)
                .toFormatter().withResolverStyle(ResolverStyle.STRICT);
        LocalDate parsed = LocalDate.parse(wby + "-" + week + "-" + dow.getValue(), f);
        assertEquals(parsed, date);
    }

    @Test(dataProvider="week")
    public void test_parse_weeks_SMART(LocalDate date, DayOfWeek dow, int week, int wby) {
        DateTimeFormatter f = new DateTimeFormatterBuilder()
                .appendValue(IsoFields.WEEK_BASED_YEAR).appendLiteral('-')
                .appendValue(IsoFields.WEEK_OF_WEEK_BASED_YEAR).appendLiteral('-')
                .appendValue(DAY_OF_WEEK)
                .toFormatter().withResolverStyle(ResolverStyle.SMART);
        LocalDate parsed = LocalDate.parse(wby + "-" + week + "-" + dow.getValue(), f);
        assertEquals(parsed, date);
    }

    @Test(dataProvider="week")
    public void test_parse_weeks_LENIENT(LocalDate date, DayOfWeek dow, int week, int wby) {
        DateTimeFormatter f = new DateTimeFormatterBuilder()
                .appendValue(IsoFields.WEEK_BASED_YEAR).appendLiteral('-')
                .appendValue(IsoFields.WEEK_OF_WEEK_BASED_YEAR).appendLiteral('-')
                .appendValue(DAY_OF_WEEK)
                .toFormatter().withResolverStyle(ResolverStyle.LENIENT);
        LocalDate parsed = LocalDate.parse(wby + "-" + week + "-" + dow.getValue(), f);
        assertEquals(parsed, date);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="parseLenientWeek")
    Object[][] data_parseLenientWeek() {
        return new Object[][] {
                {"2012:52:-1", LocalDate.of(2012, 12, 22), false},
                {"2012:52:0", LocalDate.of(2012, 12, 23), false},
                {"2012:52:8", LocalDate.of(2012, 12, 31), false},
                {"2012:52:9", LocalDate.of(2013, 1, 1), false},

                {"2012:53:1", LocalDate.of(2012, 12, 31), true},
                {"2012:54:1", LocalDate.of(2013, 1, 7), false},

                {"2013:0:1", LocalDate.of(2012, 12, 24), false},
                {"2013:0:0", LocalDate.of(2012, 12, 23), false},
        };
    }

    @Test(dataProvider = "parseLenientWeek", expectedExceptions = DateTimeParseException.class)
    public void test_parse_parseLenientWeek_STRICT(String str, LocalDate expected, boolean smart) {
        DateTimeFormatter f = new DateTimeFormatterBuilder()
                .appendValue(IsoFields.WEEK_BASED_YEAR).appendLiteral(':')
                .appendValue(IsoFields.WEEK_OF_WEEK_BASED_YEAR).appendLiteral(':')
                .appendValue(DAY_OF_WEEK)
                .toFormatter().withResolverStyle(ResolverStyle.STRICT);
        LocalDate.parse(str, f);
    }

    @Test(dataProvider = "parseLenientWeek")
    public void test_parse_parseLenientWeek_SMART(String str, LocalDate expected, boolean smart) {
        DateTimeFormatter f = new DateTimeFormatterBuilder()
                .appendValue(IsoFields.WEEK_BASED_YEAR).appendLiteral(':')
                .appendValue(IsoFields.WEEK_OF_WEEK_BASED_YEAR).appendLiteral(':')
                .appendValue(DAY_OF_WEEK)
                .toFormatter().withResolverStyle(ResolverStyle.SMART);
        if (smart) {
            LocalDate parsed = LocalDate.parse(str, f);
            assertEquals(parsed, expected);
        } else {
            try {
                LocalDate.parse(str, f);
                fail("Should have failed");
            } catch (DateTimeParseException ex) {
                // expected
            }
        }
    }

    @Test(dataProvider = "parseLenientWeek")
    public void test_parse_parseLenientWeek_LENIENT(String str, LocalDate expected, boolean smart) {
        DateTimeFormatter f = new DateTimeFormatterBuilder()
                .appendValue(IsoFields.WEEK_BASED_YEAR).appendLiteral(':')
                .appendValue(IsoFields.WEEK_OF_WEEK_BASED_YEAR).appendLiteral(':')
                .appendValue(DAY_OF_WEEK)
                .toFormatter().withResolverStyle(ResolverStyle.LENIENT);
        LocalDate parsed = LocalDate.parse(str, f);
        assertEquals(parsed, expected);
    }

    //-----------------------------------------------------------------------
    // rangeRefinedBy
    //-----------------------------------------------------------------------
    @DataProvider(name="isofields")
    Object[][] data_isofields() {
        return new Object[][] {
               {IsoFields.DAY_OF_QUARTER, 49, ValueRange.of(1, 91)},
               {IsoFields.QUARTER_OF_YEAR, 2, ValueRange.of(1, 4)},
               {IsoFields.WEEK_OF_WEEK_BASED_YEAR, 20, ValueRange.of(1, 52)},
               {IsoFields.WEEK_BASED_YEAR, 2016, ValueRange.of(LocalDate.MIN.getYear(),
                                                               LocalDate.MAX.getYear())},
        };
    }

    @Test(dataProvider = "isofields")
    public void test_isofields_rangerefinedby(TemporalField field, int value, ValueRange valueRange) {
        LocalDate date = LocalDate.of(2016, 5, 19);
        assertEquals(field.rangeRefinedBy(date), valueRange);
    }

    @Test(dataProvider = "isofields", expectedExceptions = UnsupportedTemporalTypeException.class)
    public void test_nonisofields_rangerefinedby(TemporalField field, int value, ValueRange valueRange) {
        field.rangeRefinedBy(ThaiBuddhistDate.now());
    }

    //-----------------------------------------------------------------------
    // getFrom
    //-----------------------------------------------------------------------
    @Test(dataProvider = "isofields")
    public void test_isofields_getFrom(TemporalField field, int value, ValueRange valueRange) {
        LocalDate date = LocalDate.of(2016, 5, 19);
        assertEquals(field.getFrom(date), value);
    }

    @Test(dataProvider = "isofields", expectedExceptions = UnsupportedTemporalTypeException.class)
    public void test_nonisofields_getFrom(TemporalField field, int value, ValueRange valueRange) {
        field.getFrom(ThaiBuddhistDate.now());
    }

    //-----------------------------------------------------------------------
    public void test_loop() {
        // loop round at least one 400 year cycle, including before 1970
        LocalDate date = LocalDate.of(1960, 1, 5);  // Tuseday of week 1 1960
        int year = 1960;
        int wby = 1960;
        int weekLen = 52;
        int week = 1;
        while (date.getYear() < 2400) {
            DayOfWeek loopDow = date.getDayOfWeek();
            if (date.getYear() != year) {
                year = date.getYear();
            }
            if (loopDow == MONDAY) {
                week++;
                if ((week == 53 && weekLen == 52) || week == 54) {
                    week = 1;
                    LocalDate firstDayOfWeekBasedYear = date.plusDays(14).withDayOfYear(1);
                    DayOfWeek firstDay = firstDayOfWeekBasedYear.getDayOfWeek();
                    weekLen = (firstDay == THURSDAY || (firstDay == WEDNESDAY && firstDayOfWeekBasedYear.isLeapYear()) ? 53 : 52);
                    wby++;
                }
            }
            assertEquals(IsoFields.WEEK_OF_WEEK_BASED_YEAR.rangeRefinedBy(date), ValueRange.of(1, weekLen), "Failed on " + date + " " + date.getDayOfWeek());
            assertEquals(IsoFields.WEEK_OF_WEEK_BASED_YEAR.getFrom(date), week, "Failed on " + date + " " + date.getDayOfWeek());
            assertEquals(date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), week, "Failed on " + date + " " + date.getDayOfWeek());
            assertEquals(IsoFields.WEEK_BASED_YEAR.getFrom(date), wby, "Failed on " + date + " " + date.getDayOfWeek());
            assertEquals(date.get(IsoFields.WEEK_BASED_YEAR), wby, "Failed on " + date + " " + date.getDayOfWeek());
            date = date.plusDays(1);
        }
    }

    // TODO: more tests
}
