/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
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
package tck.java.time;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.JulianFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test MonthDay.
 */
@Test
public class TCKMonthDay extends AbstractDateTimeTest {

    private MonthDay TEST_07_15;

    @BeforeMethod
    public void setUp() {
        TEST_07_15 = MonthDay.of(7, 15);
    }

    //-----------------------------------------------------------------------
    @Override
    protected List<TemporalAccessor> samples() {
        TemporalAccessor[] array = {TEST_07_15, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> validFields() {
        TemporalField[] array = {
            DAY_OF_MONTH,
            MONTH_OF_YEAR,
        };
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> invalidFields() {
        List<TemporalField> list = new ArrayList<>(Arrays.<TemporalField>asList(ChronoField.values()));
        list.removeAll(validFields());
        list.add(JulianFields.JULIAN_DAY);
        list.add(JulianFields.MODIFIED_JULIAN_DAY);
        list.add(JulianFields.RATA_DIE);
        return list;
    }

    //-----------------------------------------------------------------------
    void check(MonthDay test, int m, int d) {
        assertEquals(test.getMonth().getValue(), m);
        assertEquals(test.getDayOfMonth(), d);
    }

    //-----------------------------------------------------------------------
    // now()
    //-----------------------------------------------------------------------
    @Test
    public void now() {
        MonthDay expected = MonthDay.now(Clock.systemDefaultZone());
        MonthDay test = MonthDay.now();
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = MonthDay.now(Clock.systemDefaultZone());
            test = MonthDay.now();
        }
        assertEquals(test, expected);
    }

    //-----------------------------------------------------------------------
    // now(ZoneId)
    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void now_ZoneId_nullZoneId() {
        MonthDay.now((ZoneId) null);
    }

    @Test
    public void now_ZoneId() {
        ZoneId zone = ZoneId.of("UTC+01:02:03");
        MonthDay expected = MonthDay.now(Clock.system(zone));
        MonthDay test = MonthDay.now(zone);
        for (int i = 0; i < 100; i++) {
            if (expected.equals(test)) {
                return;
            }
            expected = MonthDay.now(Clock.system(zone));
            test = MonthDay.now(zone);
        }
        assertEquals(test, expected);
    }

    //-----------------------------------------------------------------------
    // now(Clock)
    //-----------------------------------------------------------------------
    @Test
    public void now_Clock() {
        Instant instant = LocalDateTime.of(2010, 12, 31, 0, 0).toInstant(ZoneOffset.UTC);
        Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
        MonthDay test = MonthDay.now(clock);
        assertEquals(test.getMonth(), Month.DECEMBER);
        assertEquals(test.getDayOfMonth(), 31);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void now_Clock_nullClock() {
        MonthDay.now((Clock) null);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_intMonth() {
        assertEquals(TEST_07_15, MonthDay.of(Month.JULY, 15));
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_factory_intMonth_dayTooLow() {
        MonthDay.of(Month.JANUARY, 0);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_factory_intMonth_dayTooHigh() {
        MonthDay.of(Month.JANUARY, 32);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_intMonth_nullMonth() {
        MonthDay.of(null, 15);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_ints() {
        check(TEST_07_15, 7, 15);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_factory_ints_dayTooLow() {
        MonthDay.of(1, 0);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_factory_ints_dayTooHigh() {
        MonthDay.of(1, 32);
    }


    @Test(expectedExceptions=DateTimeException.class)
    public void test_factory_ints_monthTooLow() {
        MonthDay.of(0, 1);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_factory_ints_monthTooHigh() {
        MonthDay.of(13, 1);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_CalendricalObject() {
        assertEquals(MonthDay.from(LocalDate.of(2007, 7, 15)), TEST_07_15);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_factory_CalendricalObject_invalid_noDerive() {
        MonthDay.from(LocalTime.of(12, 30));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_factory_CalendricalObject_null() {
        MonthDay.from((TemporalAccessor) null);
    }

    //-----------------------------------------------------------------------
    // parse()
    //-----------------------------------------------------------------------
    @DataProvider(name="goodParseData")
    Object[][] provider_goodParseData() {
        return new Object[][] {
                {"--01-01", MonthDay.of(1, 1)},
                {"--01-31", MonthDay.of(1, 31)},
                {"--02-01", MonthDay.of(2, 1)},
                {"--02-29", MonthDay.of(2, 29)},
                {"--03-01", MonthDay.of(3, 1)},
                {"--03-31", MonthDay.of(3, 31)},
                {"--04-01", MonthDay.of(4, 1)},
                {"--04-30", MonthDay.of(4, 30)},
                {"--05-01", MonthDay.of(5, 1)},
                {"--05-31", MonthDay.of(5, 31)},
                {"--06-01", MonthDay.of(6, 1)},
                {"--06-30", MonthDay.of(6, 30)},
                {"--07-01", MonthDay.of(7, 1)},
                {"--07-31", MonthDay.of(7, 31)},
                {"--08-01", MonthDay.of(8, 1)},
                {"--08-31", MonthDay.of(8, 31)},
                {"--09-01", MonthDay.of(9, 1)},
                {"--09-30", MonthDay.of(9, 30)},
                {"--10-01", MonthDay.of(10, 1)},
                {"--10-31", MonthDay.of(10, 31)},
                {"--11-01", MonthDay.of(11, 1)},
                {"--11-30", MonthDay.of(11, 30)},
                {"--12-01", MonthDay.of(12, 1)},
                {"--12-31", MonthDay.of(12, 31)},
        };
    }

    @Test(dataProvider="goodParseData")
    public void factory_parse_success(String text, MonthDay expected) {
        MonthDay monthDay = MonthDay.parse(text);
        assertEquals(monthDay, expected);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="badParseData")
    Object[][] provider_badParseData() {
        return new Object[][] {
                {"", 0},
                {"-00", 0},
                {"--FEB-23", 2},
                {"--01-0", 5},
                {"--01-3A", 5},
        };
    }

    @Test(dataProvider="badParseData", expectedExceptions=DateTimeParseException.class)
    public void factory_parse_fail(String text, int pos) {
        try {
            MonthDay.parse(text);
            fail(String.format("Parse should have failed for %s at position %d", text, pos));
        }
        catch (DateTimeParseException ex) {
            assertEquals(ex.getParsedString(), text);
            assertEquals(ex.getErrorIndex(), pos);
            throw ex;
        }
    }

    //-----------------------------------------------------------------------
    @Test(expectedExceptions=DateTimeParseException.class)
    public void factory_parse_illegalValue_Day() {
        MonthDay.parse("--06-32");
    }

    @Test(expectedExceptions=DateTimeParseException.class)
    public void factory_parse_invalidValue_Day() {
        MonthDay.parse("--06-31");
    }

    @Test(expectedExceptions=DateTimeParseException.class)
    public void factory_parse_illegalValue_Month() {
        MonthDay.parse("--13-25");
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_nullText() {
        MonthDay.parse(null);
    }

    //-----------------------------------------------------------------------
    // parse(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void factory_parse_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("M d");
        MonthDay test = MonthDay.parse("12 3", f);
        assertEquals(test, MonthDay.of(12, 3));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_formatter_nullText() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("M d");
        MonthDay.parse((String) null, f);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_formatter_nullFormatter() {
        MonthDay.parse("ANY", null);
    }

    //-----------------------------------------------------------------------
    // isSupported(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_isSupported_TemporalField() {
        assertEquals(TEST_07_15.isSupported((TemporalField) null), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.NANO_OF_SECOND), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.NANO_OF_DAY), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.MICRO_OF_SECOND), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.MICRO_OF_DAY), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.MILLI_OF_SECOND), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.MILLI_OF_DAY), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.SECOND_OF_MINUTE), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.SECOND_OF_DAY), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.MINUTE_OF_HOUR), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.MINUTE_OF_DAY), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.HOUR_OF_AMPM), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.CLOCK_HOUR_OF_AMPM), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.HOUR_OF_DAY), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.CLOCK_HOUR_OF_DAY), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.AMPM_OF_DAY), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.DAY_OF_WEEK), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.DAY_OF_MONTH), true);
        assertEquals(TEST_07_15.isSupported(ChronoField.DAY_OF_YEAR), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.EPOCH_DAY), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.ALIGNED_WEEK_OF_MONTH), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.ALIGNED_WEEK_OF_YEAR), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.MONTH_OF_YEAR), true);
        assertEquals(TEST_07_15.isSupported(ChronoField.PROLEPTIC_MONTH), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.YEAR), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.YEAR_OF_ERA), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.ERA), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.INSTANT_SECONDS), false);
        assertEquals(TEST_07_15.isSupported(ChronoField.OFFSET_SECONDS), false);
    }

    //-----------------------------------------------------------------------
    // get(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_TemporalField() {
        assertEquals(TEST_07_15.get(ChronoField.DAY_OF_MONTH), 15);
        assertEquals(TEST_07_15.get(ChronoField.MONTH_OF_YEAR), 7);
    }

    @Test
    public void test_getLong_TemporalField() {
        assertEquals(TEST_07_15.getLong(ChronoField.DAY_OF_MONTH), 15);
        assertEquals(TEST_07_15.getLong(ChronoField.MONTH_OF_YEAR), 7);
    }

    //-----------------------------------------------------------------------
    // query(TemporalQuery)
    //-----------------------------------------------------------------------
    @DataProvider(name="query")
    Object[][] data_query() {
        return new Object[][] {
                {TEST_07_15, TemporalQueries.chronology(), IsoChronology.INSTANCE},
                {TEST_07_15, TemporalQueries.zoneId(), null},
                {TEST_07_15, TemporalQueries.precision(), null},
                {TEST_07_15, TemporalQueries.zone(), null},
                {TEST_07_15, TemporalQueries.offset(), null},
                {TEST_07_15, TemporalQueries.localDate(), null},
                {TEST_07_15, TemporalQueries.localTime(), null},
        };
    }

    @Test(dataProvider="query")
    public <T> void test_query(TemporalAccessor temporal, TemporalQuery<T> query, T expected) {
        assertEquals(temporal.query(query), expected);
    }

    @Test(dataProvider="query")
    public <T> void test_queryFrom(TemporalAccessor temporal, TemporalQuery<T> query, T expected) {
        assertEquals(query.queryFrom(temporal), expected);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_query_null() {
        TEST_07_15.query(null);
    }

    //-----------------------------------------------------------------------
    // get*()
    //-----------------------------------------------------------------------
    @DataProvider(name="sampleDates")
    Object[][] provider_sampleDates() {
        return new Object[][] {
            {1, 1},
            {1, 31},
            {2, 1},
            {2, 28},
            {2, 29},
            {7, 4},
            {7, 5},
        };
    }

    @Test(dataProvider="sampleDates")
    public void test_get(int m, int d) {
        MonthDay a = MonthDay.of(m, d);
        assertEquals(a.getMonth(), Month.of(m));
        assertEquals(a.getMonthValue(), m);
        assertEquals(a.getDayOfMonth(), d);
    }

    //-----------------------------------------------------------------------
    // with(Month)
    //-----------------------------------------------------------------------
    @Test
    public void test_with_Month() {
        assertEquals(MonthDay.of(6, 30).with(Month.JANUARY), MonthDay.of(1, 30));
    }

    @Test
    public void test_with_Month_adjustToValid() {
        assertEquals(MonthDay.of(7, 31).with(Month.JUNE), MonthDay.of(6, 30));
    }

    @Test
    public void test_with_Month_adjustToValidFeb() {
        assertEquals(MonthDay.of(7, 31).with(Month.FEBRUARY), MonthDay.of(2, 29));
    }

    @Test
    public void test_with_Month_noChangeEqual() {
        MonthDay test = MonthDay.of(6, 30);
        assertEquals(test.with(Month.JUNE), test);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_with_Month_null() {
        MonthDay.of(6, 30).with((Month) null);
    }

    //-----------------------------------------------------------------------
    // withMonth()
    //-----------------------------------------------------------------------
    @Test
    public void test_withMonth() {
        assertEquals(MonthDay.of(6, 30).withMonth(1), MonthDay.of(1, 30));
    }

    @Test
    public void test_withMonth_adjustToValid() {
        assertEquals(MonthDay.of(7, 31).withMonth(6), MonthDay.of(6, 30));
    }

    @Test
    public void test_withMonth_adjustToValidFeb() {
        assertEquals(MonthDay.of(7, 31).withMonth(2), MonthDay.of(2, 29));
    }

    @Test
    public void test_withMonth_int_noChangeEqual() {
        MonthDay test = MonthDay.of(6, 30);
        assertEquals(test.withMonth(6), test);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_withMonth_tooLow() {
        MonthDay.of(6, 30).withMonth(0);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_withMonth_tooHigh() {
        MonthDay.of(6, 30).withMonth(13);
    }

    //-----------------------------------------------------------------------
    // withDayOfMonth()
    //-----------------------------------------------------------------------
    @Test
    public void test_withDayOfMonth() {
        assertEquals(MonthDay.of(6, 30).withDayOfMonth(1), MonthDay.of(6, 1));
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_withDayOfMonth_invalid() {
        MonthDay.of(6, 30).withDayOfMonth(31);
    }

    @Test
    public void test_withDayOfMonth_adjustToValidFeb() {
        assertEquals(MonthDay.of(2, 1).withDayOfMonth(29), MonthDay.of(2, 29));
    }

    @Test
    public void test_withDayOfMonth_noChangeEqual() {
        MonthDay test = MonthDay.of(6, 30);
        assertEquals(test.withDayOfMonth(30), test);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_withDayOfMonth_tooLow() {
        MonthDay.of(6, 30).withDayOfMonth(0);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_withDayOfMonth_tooHigh() {
        MonthDay.of(6, 30).withDayOfMonth(32);
    }

    //-----------------------------------------------------------------------
    // adjustInto()
    //-----------------------------------------------------------------------
    @Test
    public void test_adjustDate() {
        MonthDay test = MonthDay.of(6, 30);
        LocalDate date = LocalDate.of(2007, 1, 1);
        assertEquals(test.adjustInto(date), LocalDate.of(2007, 6, 30));
    }

    @Test
    public void test_adjustDate_resolve() {
        MonthDay test = MonthDay.of(2, 29);
        LocalDate date = LocalDate.of(2007, 6, 30);
        assertEquals(test.adjustInto(date), LocalDate.of(2007, 2, 28));
    }

    @Test
    public void test_adjustDate_equal() {
        MonthDay test = MonthDay.of(6, 30);
        LocalDate date = LocalDate.of(2007, 6, 30);
        assertEquals(test.adjustInto(date), date);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_adjustDate_null() {
        TEST_07_15.adjustInto((LocalDate) null);
    }

    //-----------------------------------------------------------------------
    // isValidYear(int)
    //-----------------------------------------------------------------------
    @Test
    public void test_isValidYear_june() {
        MonthDay test = MonthDay.of(6, 30);
        assertEquals(test.isValidYear(2007), true);
    }

    @Test
    public void test_isValidYear_febNonLeap() {
        MonthDay test = MonthDay.of(2, 29);
        assertEquals(test.isValidYear(2007), false);
    }

    @Test
    public void test_isValidYear_febLeap() {
        MonthDay test = MonthDay.of(2, 29);
        assertEquals(test.isValidYear(2008), true);
    }

    //-----------------------------------------------------------------------
    // format(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void test_format_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("M d");
        String t = MonthDay.of(12, 3).format(f);
        assertEquals(t, "12 3");
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_format_formatter_null() {
        MonthDay.of(12, 3).format(null);
    }

    //-----------------------------------------------------------------------
    // atYear(int)
    //-----------------------------------------------------------------------
    @Test
    public void test_atYear_int() {
        MonthDay test = MonthDay.of(6, 30);
        assertEquals(test.atYear(2008), LocalDate.of(2008, 6, 30));
    }

    @Test
    public void test_atYear_int_leapYearAdjust() {
        MonthDay test = MonthDay.of(2, 29);
        assertEquals(test.atYear(2005), LocalDate.of(2005, 2, 28));
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_atYear_int_invalidYear() {
        MonthDay test = MonthDay.of(6, 30);
        test.atYear(Integer.MIN_VALUE);
    }

    //-----------------------------------------------------------------------
    // compareTo()
    //-----------------------------------------------------------------------
    @Test
    public void test_comparisons() {
        doTest_comparisons_MonthDay(
            MonthDay.of(1, 1),
            MonthDay.of(1, 31),
            MonthDay.of(2, 1),
            MonthDay.of(2, 29),
            MonthDay.of(3, 1),
            MonthDay.of(12, 31)
        );
    }

    void doTest_comparisons_MonthDay(MonthDay... localDates) {
        for (int i = 0; i < localDates.length; i++) {
            MonthDay a = localDates[i];
            for (int j = 0; j < localDates.length; j++) {
                MonthDay b = localDates[j];
                if (i < j) {
                    assertTrue(a.compareTo(b) < 0, a + " <=> " + b);
                    assertEquals(a.isBefore(b), true, a + " <=> " + b);
                    assertEquals(a.isAfter(b), false, a + " <=> " + b);
                    assertEquals(a.equals(b), false, a + " <=> " + b);
                } else if (i > j) {
                    assertTrue(a.compareTo(b) > 0, a + " <=> " + b);
                    assertEquals(a.isBefore(b), false, a + " <=> " + b);
                    assertEquals(a.isAfter(b), true, a + " <=> " + b);
                    assertEquals(a.equals(b), false, a + " <=> " + b);
                } else {
                    assertEquals(a.compareTo(b), 0, a + " <=> " + b);
                    assertEquals(a.isBefore(b), false, a + " <=> " + b);
                    assertEquals(a.isAfter(b), false, a + " <=> " + b);
                    assertEquals(a.equals(b), true, a + " <=> " + b);
                }
            }
        }
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_compareTo_ObjectNull() {
        TEST_07_15.compareTo(null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_isBefore_ObjectNull() {
        TEST_07_15.isBefore(null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_isAfter_ObjectNull() {
        TEST_07_15.isAfter(null);
    }

    //-----------------------------------------------------------------------
    // equals()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals() {
        MonthDay a = MonthDay.of(1, 1);
        MonthDay b = MonthDay.of(1, 1);
        MonthDay c = MonthDay.of(2, 1);
        MonthDay d = MonthDay.of(1, 2);

        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), true);
        assertEquals(a.equals(c), false);
        assertEquals(a.equals(d), false);

        assertEquals(b.equals(a), true);
        assertEquals(b.equals(b), true);
        assertEquals(b.equals(c), false);
        assertEquals(b.equals(d), false);

        assertEquals(c.equals(a), false);
        assertEquals(c.equals(b), false);
        assertEquals(c.equals(c), true);
        assertEquals(c.equals(d), false);

        assertEquals(d.equals(a), false);
        assertEquals(d.equals(b), false);
        assertEquals(d.equals(c), false);
        assertEquals(d.equals(d), true);
    }

    @Test
    public void test_equals_itself_true() {
        assertEquals(TEST_07_15.equals(TEST_07_15), true);
    }

    @Test
    public void test_equals_string_false() {
        assertEquals(TEST_07_15.equals("2007-07-15"), false);
    }

    @Test
    public void test_equals_null_false() {
        assertEquals(TEST_07_15.equals(null), false);
    }

    //-----------------------------------------------------------------------
    // hashCode()
    //-----------------------------------------------------------------------
    @Test(dataProvider="sampleDates")
    public void test_hashCode(int m, int d) {
        MonthDay a = MonthDay.of(m, d);
        assertEquals(a.hashCode(), a.hashCode());
        MonthDay b = MonthDay.of(m, d);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void test_hashCode_unique() {
        int leapYear = 2008;
        Set<Integer> uniques = new HashSet<Integer>(366);
        for (int i = 1; i <= 12; i++) {
            for (int j = 1; j <= 31; j++) {
                if (YearMonth.of(leapYear, i).isValidDay(j)) {
                    assertTrue(uniques.add(MonthDay.of(i, j).hashCode()));
                }
            }
        }
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @DataProvider(name="sampleToString")
    Object[][] provider_sampleToString() {
        return new Object[][] {
            {7, 5, "--07-05"},
            {12, 31, "--12-31"},
            {1, 2, "--01-02"},
        };
    }

    @Test(dataProvider="sampleToString")
    public void test_toString(int m, int d, String expected) {
        MonthDay test = MonthDay.of(m, d);
        String str = test.toString();
        assertEquals(str, expected);
    }

}
