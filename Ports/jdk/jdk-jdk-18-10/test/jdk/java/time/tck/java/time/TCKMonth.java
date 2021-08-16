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

import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static org.testng.Assert.assertEquals;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.chrono.IsoChronology;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.JulianFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test Month.
 */
@Test
public class TCKMonth extends AbstractDateTimeTest {

    private static final int MAX_LENGTH = 12;

    //-----------------------------------------------------------------------
    @Override
    protected List<TemporalAccessor> samples() {
        TemporalAccessor[] array = {Month.JANUARY, Month.JUNE, Month.DECEMBER, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> validFields() {
        TemporalField[] array = {
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
    @Test
    public void test_factory_int_singleton() {
        for (int i = 1; i <= MAX_LENGTH; i++) {
            Month test = Month.of(i);
            assertEquals(test.getValue(), i);
        }
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_factory_int_tooLow() {
        Month.of(0);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_factory_int_tooHigh() {
        Month.of(13);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_factory_CalendricalObject() {
        assertEquals(Month.from(LocalDate.of(2011, 6, 6)), Month.JUNE);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_factory_CalendricalObject_invalid_noDerive() {
        Month.from(LocalTime.of(12, 30));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_factory_CalendricalObject_null() {
        Month.from((TemporalAccessor) null);
    }

    //-----------------------------------------------------------------------
    // isSupported(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_isSupported_TemporalField() {
        assertEquals(Month.AUGUST.isSupported((TemporalField) null), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.NANO_OF_SECOND), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.NANO_OF_DAY), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.MICRO_OF_SECOND), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.MICRO_OF_DAY), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.MILLI_OF_SECOND), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.MILLI_OF_DAY), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.SECOND_OF_MINUTE), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.SECOND_OF_DAY), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.MINUTE_OF_HOUR), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.MINUTE_OF_DAY), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.HOUR_OF_AMPM), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.CLOCK_HOUR_OF_AMPM), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.HOUR_OF_DAY), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.CLOCK_HOUR_OF_DAY), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.AMPM_OF_DAY), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.DAY_OF_WEEK), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.DAY_OF_MONTH), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.DAY_OF_YEAR), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.EPOCH_DAY), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.ALIGNED_WEEK_OF_MONTH), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.ALIGNED_WEEK_OF_YEAR), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.MONTH_OF_YEAR), true);
        assertEquals(Month.AUGUST.isSupported(ChronoField.PROLEPTIC_MONTH), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.YEAR), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.YEAR_OF_ERA), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.ERA), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.INSTANT_SECONDS), false);
        assertEquals(Month.AUGUST.isSupported(ChronoField.OFFSET_SECONDS), false);
    }

    //-----------------------------------------------------------------------
    // get(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_TemporalField() {
        assertEquals(Month.JULY.get(ChronoField.MONTH_OF_YEAR), 7);
    }

    @Test
    public void test_getLong_TemporalField() {
        assertEquals(Month.JULY.getLong(ChronoField.MONTH_OF_YEAR), 7);
    }

    //-----------------------------------------------------------------------
    // query(TemporalQuery)
    //-----------------------------------------------------------------------
    @DataProvider(name="query")
    Object[][] data_query() {
        return new Object[][] {
                {Month.JUNE, TemporalQueries.chronology(), IsoChronology.INSTANCE},
                {Month.JUNE, TemporalQueries.zoneId(), null},
                {Month.JUNE, TemporalQueries.precision(), ChronoUnit.MONTHS},
                {Month.JUNE, TemporalQueries.zone(), null},
                {Month.JUNE, TemporalQueries.offset(), null},
                {Month.JUNE, TemporalQueries.localDate(), null},
                {Month.JUNE, TemporalQueries.localTime(), null},
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
        Month.JUNE.query(null);
    }

    //-----------------------------------------------------------------------
    // getText()
    //-----------------------------------------------------------------------
    @Test
    public void test_getText() {
        assertEquals(Month.JANUARY.getDisplayName(TextStyle.SHORT, Locale.US), "Jan");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_getText_nullStyle() {
        Month.JANUARY.getDisplayName(null, Locale.US);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_getText_nullLocale() {
        Month.JANUARY.getDisplayName(TextStyle.FULL, null);
    }

    //-----------------------------------------------------------------------
    // plus(long), plus(long,unit)
    //-----------------------------------------------------------------------
    @DataProvider(name="plus")
    Object[][] data_plus() {
        return new Object[][] {
            {1, -13, 12},
            {1, -12, 1},
            {1, -11, 2},
            {1, -10, 3},
            {1, -9, 4},
            {1, -8, 5},
            {1, -7, 6},
            {1, -6, 7},
            {1, -5, 8},
            {1, -4, 9},
            {1, -3, 10},
            {1, -2, 11},
            {1, -1, 12},
            {1, 0, 1},
            {1, 1, 2},
            {1, 2, 3},
            {1, 3, 4},
            {1, 4, 5},
            {1, 5, 6},
            {1, 6, 7},
            {1, 7, 8},
            {1, 8, 9},
            {1, 9, 10},
            {1, 10, 11},
            {1, 11, 12},
            {1, 12, 1},
            {1, 13, 2},

            {1, 1, 2},
            {2, 1, 3},
            {3, 1, 4},
            {4, 1, 5},
            {5, 1, 6},
            {6, 1, 7},
            {7, 1, 8},
            {8, 1, 9},
            {9, 1, 10},
            {10, 1, 11},
            {11, 1, 12},
            {12, 1, 1},

            {1, -1, 12},
            {2, -1, 1},
            {3, -1, 2},
            {4, -1, 3},
            {5, -1, 4},
            {6, -1, 5},
            {7, -1, 6},
            {8, -1, 7},
            {9, -1, 8},
            {10, -1, 9},
            {11, -1, 10},
            {12, -1, 11},
        };
    }

    @Test(dataProvider="plus")
    public void test_plus_long(int base, long amount, int expected) {
        assertEquals(Month.of(base).plus(amount), Month.of(expected));
    }

    //-----------------------------------------------------------------------
    // minus(long), minus(long,unit)
    //-----------------------------------------------------------------------
    @DataProvider(name="minus")
    Object[][] data_minus() {
        return new Object[][] {
            {1, -13, 2},
            {1, -12, 1},
            {1, -11, 12},
            {1, -10, 11},
            {1, -9, 10},
            {1, -8, 9},
            {1, -7, 8},
            {1, -6, 7},
            {1, -5, 6},
            {1, -4, 5},
            {1, -3, 4},
            {1, -2, 3},
            {1, -1, 2},
            {1, 0, 1},
            {1, 1, 12},
            {1, 2, 11},
            {1, 3, 10},
            {1, 4, 9},
            {1, 5, 8},
            {1, 6, 7},
            {1, 7, 6},
            {1, 8, 5},
            {1, 9, 4},
            {1, 10, 3},
            {1, 11, 2},
            {1, 12, 1},
            {1, 13, 12},
        };
    }

    @Test(dataProvider="minus")
    public void test_minus_long(int base, long amount, int expected) {
        assertEquals(Month.of(base).minus(amount), Month.of(expected));
    }

    //-----------------------------------------------------------------------
    // length(boolean)
    //-----------------------------------------------------------------------
    @Test
    public void test_length_boolean_notLeapYear() {
        assertEquals(Month.JANUARY.length(false), 31);
        assertEquals(Month.FEBRUARY.length(false), 28);
        assertEquals(Month.MARCH.length(false), 31);
        assertEquals(Month.APRIL.length(false), 30);
        assertEquals(Month.MAY.length(false), 31);
        assertEquals(Month.JUNE.length(false), 30);
        assertEquals(Month.JULY.length(false), 31);
        assertEquals(Month.AUGUST.length(false), 31);
        assertEquals(Month.SEPTEMBER.length(false), 30);
        assertEquals(Month.OCTOBER.length(false), 31);
        assertEquals(Month.NOVEMBER.length(false), 30);
        assertEquals(Month.DECEMBER.length(false), 31);
    }

    @Test
    public void test_length_boolean_leapYear() {
        assertEquals(Month.JANUARY.length(true), 31);
        assertEquals(Month.FEBRUARY.length(true), 29);
        assertEquals(Month.MARCH.length(true), 31);
        assertEquals(Month.APRIL.length(true), 30);
        assertEquals(Month.MAY.length(true), 31);
        assertEquals(Month.JUNE.length(true), 30);
        assertEquals(Month.JULY.length(true), 31);
        assertEquals(Month.AUGUST.length(true), 31);
        assertEquals(Month.SEPTEMBER.length(true), 30);
        assertEquals(Month.OCTOBER.length(true), 31);
        assertEquals(Month.NOVEMBER.length(true), 30);
        assertEquals(Month.DECEMBER.length(true), 31);
    }

    //-----------------------------------------------------------------------
    // minLength()
    //-----------------------------------------------------------------------
    @Test
    public void test_minLength() {
        assertEquals(Month.JANUARY.minLength(), 31);
        assertEquals(Month.FEBRUARY.minLength(), 28);
        assertEquals(Month.MARCH.minLength(), 31);
        assertEquals(Month.APRIL.minLength(), 30);
        assertEquals(Month.MAY.minLength(), 31);
        assertEquals(Month.JUNE.minLength(), 30);
        assertEquals(Month.JULY.minLength(), 31);
        assertEquals(Month.AUGUST.minLength(), 31);
        assertEquals(Month.SEPTEMBER.minLength(), 30);
        assertEquals(Month.OCTOBER.minLength(), 31);
        assertEquals(Month.NOVEMBER.minLength(), 30);
        assertEquals(Month.DECEMBER.minLength(), 31);
    }

    //-----------------------------------------------------------------------
    // maxLength()
    //-----------------------------------------------------------------------
    @Test
    public void test_maxLength() {
        assertEquals(Month.JANUARY.maxLength(), 31);
        assertEquals(Month.FEBRUARY.maxLength(), 29);
        assertEquals(Month.MARCH.maxLength(), 31);
        assertEquals(Month.APRIL.maxLength(), 30);
        assertEquals(Month.MAY.maxLength(), 31);
        assertEquals(Month.JUNE.maxLength(), 30);
        assertEquals(Month.JULY.maxLength(), 31);
        assertEquals(Month.AUGUST.maxLength(), 31);
        assertEquals(Month.SEPTEMBER.maxLength(), 30);
        assertEquals(Month.OCTOBER.maxLength(), 31);
        assertEquals(Month.NOVEMBER.maxLength(), 30);
        assertEquals(Month.DECEMBER.maxLength(), 31);
    }

    //-----------------------------------------------------------------------
    // firstDayOfYear(boolean)
    //-----------------------------------------------------------------------
    @Test
    public void test_firstDayOfYear_notLeapYear() {
        assertEquals(Month.JANUARY.firstDayOfYear(false), 1);
        assertEquals(Month.FEBRUARY.firstDayOfYear(false), 1 + 31);
        assertEquals(Month.MARCH.firstDayOfYear(false), 1 + 31 + 28);
        assertEquals(Month.APRIL.firstDayOfYear(false), 1 + 31 + 28 + 31);
        assertEquals(Month.MAY.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30);
        assertEquals(Month.JUNE.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30 + 31);
        assertEquals(Month.JULY.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30 + 31 + 30);
        assertEquals(Month.AUGUST.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30 + 31 + 30 + 31);
        assertEquals(Month.SEPTEMBER.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30 + 31 + 30 + 31 + 31);
        assertEquals(Month.OCTOBER.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30 + 31 + 30 + 31 + 31 + 30);
        assertEquals(Month.NOVEMBER.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31);
        assertEquals(Month.DECEMBER.firstDayOfYear(false), 1 + 31 + 28 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31 + 30);
    }

    @Test
    public void test_firstDayOfYear_leapYear() {
        assertEquals(Month.JANUARY.firstDayOfYear(true), 1);
        assertEquals(Month.FEBRUARY.firstDayOfYear(true), 1 + 31);
        assertEquals(Month.MARCH.firstDayOfYear(true), 1 + 31 + 29);
        assertEquals(Month.APRIL.firstDayOfYear(true), 1 + 31 + 29 + 31);
        assertEquals(Month.MAY.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30);
        assertEquals(Month.JUNE.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30 + 31);
        assertEquals(Month.JULY.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30 + 31 + 30);
        assertEquals(Month.AUGUST.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30 + 31 + 30 + 31);
        assertEquals(Month.SEPTEMBER.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30 + 31 + 30 + 31 + 31);
        assertEquals(Month.OCTOBER.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30 + 31 + 30 + 31 + 31 + 30);
        assertEquals(Month.NOVEMBER.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31);
        assertEquals(Month.DECEMBER.firstDayOfYear(true), 1 + 31 + 29 + 31 + 30 + 31 + 30 + 31 + 31 + 30 + 31 + 30);
    }

    //-----------------------------------------------------------------------
    // firstMonthOfQuarter()
    //-----------------------------------------------------------------------
    @Test
    public void test_firstMonthOfQuarter() {
        assertEquals(Month.JANUARY.firstMonthOfQuarter(), Month.JANUARY);
        assertEquals(Month.FEBRUARY.firstMonthOfQuarter(), Month.JANUARY);
        assertEquals(Month.MARCH.firstMonthOfQuarter(), Month.JANUARY);
        assertEquals(Month.APRIL.firstMonthOfQuarter(), Month.APRIL);
        assertEquals(Month.MAY.firstMonthOfQuarter(), Month.APRIL);
        assertEquals(Month.JUNE.firstMonthOfQuarter(), Month.APRIL);
        assertEquals(Month.JULY.firstMonthOfQuarter(), Month.JULY);
        assertEquals(Month.AUGUST.firstMonthOfQuarter(), Month.JULY);
        assertEquals(Month.SEPTEMBER.firstMonthOfQuarter(), Month.JULY);
        assertEquals(Month.OCTOBER.firstMonthOfQuarter(), Month.OCTOBER);
        assertEquals(Month.NOVEMBER.firstMonthOfQuarter(), Month.OCTOBER);
        assertEquals(Month.DECEMBER.firstMonthOfQuarter(), Month.OCTOBER);
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @Test
    public void test_toString() {
        assertEquals(Month.JANUARY.toString(), "JANUARY");
        assertEquals(Month.FEBRUARY.toString(), "FEBRUARY");
        assertEquals(Month.MARCH.toString(), "MARCH");
        assertEquals(Month.APRIL.toString(), "APRIL");
        assertEquals(Month.MAY.toString(), "MAY");
        assertEquals(Month.JUNE.toString(), "JUNE");
        assertEquals(Month.JULY.toString(), "JULY");
        assertEquals(Month.AUGUST.toString(), "AUGUST");
        assertEquals(Month.SEPTEMBER.toString(), "SEPTEMBER");
        assertEquals(Month.OCTOBER.toString(), "OCTOBER");
        assertEquals(Month.NOVEMBER.toString(), "NOVEMBER");
        assertEquals(Month.DECEMBER.toString(), "DECEMBER");
    }

    //-----------------------------------------------------------------------
    // generated methods
    //-----------------------------------------------------------------------
    @Test
    public void test_enum() {
        assertEquals(Month.valueOf("JANUARY"), Month.JANUARY);
        assertEquals(Month.values()[0], Month.JANUARY);
    }

}
