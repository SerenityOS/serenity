/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
package test.java.time.temporal;

import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.chrono.ThaiBuddhistDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalField;
import java.time.temporal.ValueRange;
import java.time.temporal.WeekFields;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class TestIsoWeekFields {

    @DataProvider(name = "fields")
    Object[][] data_Fields() {
        return new Object[][] {
                {IsoFields.WEEK_OF_WEEK_BASED_YEAR, IsoFields.WEEK_BASED_YEAR},
                {WeekFields.ISO.weekOfWeekBasedYear(), WeekFields.ISO.weekBasedYear()},
        };
    }

    //-----------------------------------------------------------------------
    // WEEK_OF_WEEK_BASED_YEAR
    //-----------------------------------------------------------------------
    @Test(dataProvider = "fields")
    public void test_WOWBY_basics(TemporalField weekField, TemporalField yearField) {
        assertEquals(weekField.isDateBased(), true);
        assertEquals(weekField.isTimeBased(), false);
        assertEquals(weekField.getBaseUnit(), ChronoUnit.WEEKS);
        assertEquals(weekField.getRangeUnit(), IsoFields.WEEK_BASED_YEARS);
    }

    @Test(dataProvider = "fields")
    public void test_WOWBY_isSupportedBy(TemporalField weekField, TemporalField yearField) {
        assertEquals(weekField.isSupportedBy(LocalTime.NOON), false);
        assertEquals(weekField.isSupportedBy(MonthDay.of(2, 1)), false);
        assertEquals(weekField.isSupportedBy(LocalDate.MIN), true);
        assertEquals(weekField.isSupportedBy(OffsetDateTime.MAX), true);
    }

    @Test
    public void test_WOWBY_isSupportedBy_fieldsDiffer() {
        assertEquals(IsoFields.WEEK_OF_WEEK_BASED_YEAR.isSupportedBy(ThaiBuddhistDate.now()), false);
        assertEquals(WeekFields.ISO.weekOfWeekBasedYear().isSupportedBy(ThaiBuddhistDate.now()), true);
    }

    @Test(dataProvider = "fields")
    public void test_WOWBY_range(TemporalField weekField, TemporalField yearField) {
        assertEquals(weekField.range(), ValueRange.of(1, 52, 53));
    }

    @Test(dataProvider = "fields")
    public void test_WOWBY_rangeRefinedBy(TemporalField weekField, TemporalField yearField) {
        assertEquals(weekField.rangeRefinedBy(LocalDate.of(2012, 12, 31)), ValueRange.of(1, 52));
        assertEquals(weekField.rangeRefinedBy(LocalDate.of(2013, 12, 29)), ValueRange.of(1, 52));
        assertEquals(weekField.rangeRefinedBy(LocalDate.of(2013, 12, 30)), ValueRange.of(1, 52));
        assertEquals(weekField.rangeRefinedBy(LocalDate.of(2014, 12, 28)), ValueRange.of(1, 52));
        assertEquals(weekField.rangeRefinedBy(LocalDate.of(2014, 12, 29)), ValueRange.of(1, 53));
        assertEquals(weekField.rangeRefinedBy(LocalDate.of(2016, 1, 3)), ValueRange.of(1, 53));
        assertEquals(weekField.rangeRefinedBy(LocalDate.of(2016, 1, 4)), ValueRange.of(1, 52));
    }

    //-----------------------------------------------------------------------
    // WEEK_BASED_YEAR
    //-----------------------------------------------------------------------
    @Test(dataProvider = "fields")
    public void test_WBY_basics(TemporalField weekField, TemporalField yearField) {
        assertEquals(yearField.isDateBased(), true);
        assertEquals(yearField.isTimeBased(), false);
        assertEquals(yearField.getBaseUnit(), IsoFields.WEEK_BASED_YEARS);
        assertEquals(yearField.getRangeUnit(), ChronoUnit.FOREVER);
    }

    @Test(dataProvider = "fields")
    public void test_WBY_isSupportedBy(TemporalField weekField, TemporalField yearField) {
        assertEquals(yearField.isSupportedBy(LocalTime.NOON), false);
        assertEquals(yearField.isSupportedBy(MonthDay.of(2, 1)), false);
        assertEquals(yearField.isSupportedBy(LocalDate.MIN), true);
        assertEquals(yearField.isSupportedBy(OffsetDateTime.MAX), true);
    }

    @Test
    public void test_WBY_isSupportedBy_ISO() {
        assertEquals(IsoFields.WEEK_BASED_YEAR.isSupportedBy(ThaiBuddhistDate.now()), false);
    }

    @Test
    public void test_Unit_isSupportedBy_ISO() {
        assertEquals(IsoFields.WEEK_BASED_YEARS.isSupportedBy(LocalDate.now()),true);
        assertEquals(IsoFields.WEEK_BASED_YEARS.isSupportedBy(ThaiBuddhistDate.now()),false);
        assertEquals(IsoFields.QUARTER_YEARS.isSupportedBy(LocalDate.now()),true);
        assertEquals(IsoFields.QUARTER_YEARS.isSupportedBy(ThaiBuddhistDate.now()),false);
    }

    @Test(dataProvider = "fields")
    public void test_WBY_range(TemporalField weekField, TemporalField yearField) {
        assertEquals(yearField.range(), ValueRange.of(Year.MIN_VALUE, Year.MAX_VALUE));
    }

    @Test(dataProvider = "fields")
    public void test_WBY_rangeRefinedBy(TemporalField weekField, TemporalField yearField) {
        assertEquals(yearField.rangeRefinedBy(LocalDate.of(2012, 12, 31)), ValueRange.of(Year.MIN_VALUE, Year.MAX_VALUE));
    }

    //-----------------------------------------------------------------------
    @Test(dataProvider = "fields")
    public void test_getFrom(TemporalField weekField, TemporalField yearField) {
        // tests every day from 2011 to 2016 inclusive
        LocalDate date = LocalDate.of(2011, 1, 3);
        int wby = 2011;
        int week = 1;
        int dow = 1;
        for (int i = 1; i <= ((52 + 52 + 52 + 52 + 53 + 52) * 7); i++) {
            assertEquals(yearField.getFrom(date), wby);
            assertEquals(weekField.getFrom(date), week);
            assertEquals(DAY_OF_WEEK.getFrom(date), dow);
            if (dow == 7) {
                dow = 1;
                week++;
            } else {
                dow++;
            }
            if (week > wbyLen(wby)) {
                week = 1;
                wby++;
            }
            date = date.plusDays(1);
        }
        assertEquals(yearField.getFrom(date), 2017);
        assertEquals(weekField.getFrom(date), 1);
        assertEquals(DAY_OF_WEEK.getFrom(date), 1);
    }

    @Test(dataProvider = "fields")
    public void test_adjustInto_dow(TemporalField weekField, TemporalField yearField) {
        // tests every day from 2012 to 2016 inclusive
        LocalDate date = LocalDate.of(2012, 1, 2);
        int wby = 2012;
        int week = 1;
        int dow = 1;
        for (int i = 1; i <= ((52 + 52 + 52 + 53 + 52) * 7); i++) {
            for (int j = 1; j <= 7; j++) {
                LocalDate adjusted = DAY_OF_WEEK.adjustInto(date, j);
                assertEquals(adjusted.get(DAY_OF_WEEK), j);
                assertEquals(adjusted.get(weekField), week);
                assertEquals(adjusted.get(yearField), wby);
            }
            if (dow == 7) {
                dow = 1;
                week++;
            } else {
                dow++;
            }
            if (week > wbyLen(wby)) {
                week = 1;
                wby++;
            }
            date = date.plusDays(1);
        }
    }

    @Test(dataProvider = "fields")
    public void test_adjustInto_week(TemporalField weekField, TemporalField yearField) {
        // tests every day from 2012 to 2016 inclusive
        LocalDate date = LocalDate.of(2012, 1, 2);
        int wby = 2012;
        int week = 1;
        int dow = 1;
        for (int i = 1; i <= ((52 + 52 + 52 + 53 + 52) * 7); i++) {
            int weeksInYear = (wby == 2015 ? 53 : 52);
            for (int j = 1; j <= weeksInYear; j++) {
                LocalDate adjusted = weekField.adjustInto(date, j);
                assertEquals(adjusted.get(weekField), j);
                assertEquals(adjusted.get(DAY_OF_WEEK), dow);
                assertEquals(adjusted.get(yearField), wby);
            }
            if (dow == 7) {
                dow = 1;
                week++;
            } else {
                dow++;
            }
            if (week > wbyLen(wby)) {
                week = 1;
                wby++;
            }
            date = date.plusDays(1);
        }
    }

    @Test(dataProvider = "fields")
    public void test_adjustInto_wby(TemporalField weekField, TemporalField yearField) {
        // tests every day from 2012 to 2016 inclusive
        LocalDate date = LocalDate.of(2012, 1, 2);
        int wby = 2012;
        int week = 1;
        int dow = 1;
        for (int i = 1; i <= ((52 + 52 + 52 + 53 + 52) * 7); i++) {
            for (int j = 2004; j <= 2015; j++) {
                LocalDate adjusted = yearField.adjustInto(date, j);
                assertEquals(adjusted.get(yearField), j);
                assertEquals(adjusted.get(DAY_OF_WEEK), dow);
                assertEquals(adjusted.get(weekField), (week == 53 && wbyLen(j) == 52 ? 52 : week), "" + date + " " + adjusted);
            }
            if (dow == 7) {
                dow = 1;
                week++;
            } else {
                dow++;
            }
            if (week > wbyLen(wby)) {
                week = 1;
                wby++;
            }
            date = date.plusDays(1);
        }
    }

    @Test(dataProvider = "fields")
    public void test_addTo_weekBasedYears(TemporalField weekField, TemporalField yearField) {
        // tests every day from 2012 to 2016 inclusive
        LocalDate date = LocalDate.of(2012, 1, 2);
        int wby = 2012;
        int week = 1;
        int dow = 1;
        for (int i = 1; i <= ((52 + 52 + 52 + 53 + 52) * 7); i++) {
            for (int j = -5; j <= 5; j++) {
                LocalDate adjusted = IsoFields.WEEK_BASED_YEARS.addTo(date, j);
                assertEquals(adjusted.get(yearField), wby + j);
                assertEquals(adjusted.get(DAY_OF_WEEK), dow);
                assertEquals(adjusted.get(weekField), (week == 53 && wbyLen(wby + j) == 52 ? 52 : week), "" + date + " " + adjusted);
            }
            if (dow == 7) {
                dow = 1;
                week++;
            } else {
                dow++;
            }
            if (week > wbyLen(wby)) {
                week = 1;
                wby++;
            }
            date = date.plusDays(1);
        }
    }

    @Test
    public void test_ISOSingleton() {
        assertTrue(WeekFields.ISO == WeekFields.of(DayOfWeek.MONDAY, 4));
    }

    private int wbyLen(int wby) {
        return (wby == 2004 || wby == 2009 || wby == 2015 || wby == 2020 ? 53 : 52);
    }

}
