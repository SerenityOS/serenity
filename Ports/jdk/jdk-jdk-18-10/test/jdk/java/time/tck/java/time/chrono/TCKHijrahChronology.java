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
package tck.java.time.chrono;

import java.time.Clock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.Era;
import java.time.chrono.HijrahChronology;
import java.time.chrono.HijrahDate;
import java.time.chrono.HijrahEra;
import java.time.chrono.IsoChronology;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class TCKHijrahChronology {

    //-----------------------------------------------------------------------
    // Chronology.ofName("Hijrah")  Lookup by name
    //-----------------------------------------------------------------------
    @Test
    public void test_chrono_byName() {
        Chronology c = HijrahChronology.INSTANCE;
        Chronology test = Chronology.of("Hijrah-umalqura");
        Assert.assertNotNull(test, "The Hijrah-umalqura calendar could not be found by name");
        Assert.assertEquals(test.getId(), "Hijrah-umalqura", "ID mismatch");
        Assert.assertEquals(test.getCalendarType(), "islamic-umalqura", "Type mismatch");
        Assert.assertEquals(test, c);
    }

    // Tests for dateNow() method
    @Test
    public void test_dateNow(){
        assertEquals(HijrahChronology.INSTANCE.dateNow(), HijrahDate.now()) ;
        assertEquals(HijrahChronology.INSTANCE.dateNow(), HijrahDate.now(ZoneId.systemDefault())) ;
        assertEquals(HijrahChronology.INSTANCE.dateNow(), HijrahDate.now(Clock.systemDefaultZone())) ;
        assertEquals(HijrahChronology.INSTANCE.dateNow(), HijrahDate.now(Clock.systemDefaultZone().getZone())) ;

        assertEquals(HijrahChronology.INSTANCE.dateNow(), HijrahChronology.INSTANCE.dateNow(ZoneId.systemDefault())) ;
        assertEquals(HijrahChronology.INSTANCE.dateNow(), HijrahChronology.INSTANCE.dateNow(Clock.systemDefaultZone())) ;
        assertEquals(HijrahChronology.INSTANCE.dateNow(), HijrahChronology.INSTANCE.dateNow(Clock.systemDefaultZone().getZone())) ;

        ZoneId zoneId = ZoneId.of("Europe/Paris");
        assertEquals(HijrahChronology.INSTANCE.dateNow(zoneId), HijrahChronology.INSTANCE.dateNow(Clock.system(zoneId))) ;
        assertEquals(HijrahChronology.INSTANCE.dateNow(zoneId), HijrahChronology.INSTANCE.dateNow(Clock.system(zoneId).getZone())) ;
        assertEquals(HijrahChronology.INSTANCE.dateNow(zoneId), HijrahDate.now(Clock.system(zoneId))) ;
        assertEquals(HijrahChronology.INSTANCE.dateNow(zoneId), HijrahDate.now(Clock.system(zoneId).getZone())) ;

        assertEquals(HijrahChronology.INSTANCE.dateNow(ZoneId.of(ZoneOffset.UTC.getId())), HijrahChronology.INSTANCE.dateNow(Clock.systemUTC())) ;
    }

    // Sample invalid dates
    @DataProvider(name="badDates")
    Object[][] data_badDates() {
        return new Object[][] {
            {1299, 12, 29},
            {1320, 1, 29 + 1},
            {1320, 12, 29 + 1},
            {1434, -1, 1},
            {1605, 1, 29},
            {1434, 0, 1},
            {1434, 14, 1},
            {1434, 15, 1},
            {1434, 1, -1},
            {1434, 1, 0},
            {1434, 1, 32},
            {1434, 12, -1},
            {1434, 12, 0},
            {1434, 12, 32},
        };
    }

    // This is a negative test to verify if the API throws exception if an invalid date is provided
    @Test(dataProvider="badDates", expectedExceptions=DateTimeException.class)
    public void test_badDates(int year, int month, int dom) {
        HijrahChronology.INSTANCE.date(year, month, dom);
    }

    // Negative test or dateYearDay with day too large
    @Test(expectedExceptions=java.time.DateTimeException.class)
    public void test_ofYearDayTooLarge() {
        int year = 1435;
        int lengthOfYear = HijrahChronology.INSTANCE.dateYearDay(year, 1).lengthOfYear();
        HijrahDate hd = HijrahChronology.INSTANCE.dateYearDay(year, lengthOfYear + 1);
    }

    //-----------------------------------------------------------------------
    // Bad Era for Chronology.date(era,...) and Chronology.prolepticYear(Era,...)
    //-----------------------------------------------------------------------
    @Test
    public void test_InvalidEras() {
        // Verify that the eras from every other Chronology are invalid
        for (Chronology chrono : Chronology.getAvailableChronologies()) {
            if (chrono instanceof HijrahChronology) {
                continue;
            }
            List<Era> eras = chrono.eras();
            for (Era era : eras) {
                try {
                    ChronoLocalDate date = HijrahChronology.INSTANCE.date(era, 1, 1, 1);
                    fail("HijrahChronology.date did not throw ClassCastException for Era: " + era);
                } catch (ClassCastException cex) {
                    ; // ignore expected exception
                }

                /* TODO: Test for checking HijrahDate.of(Era, y, m, d) method if it is added.
                try {
                    @SuppressWarnings("unused")
                    HijrahDate jdate = HijrahDate.of(era, 1, 1, 1);
                    fail("HijrahDate.of did not throw ClassCastException for Era: " + era);
                } catch (ClassCastException cex) {
                    ; // ignore expected exception
                }
                */

                try {
                    @SuppressWarnings("unused")
                    int year = HijrahChronology.INSTANCE.prolepticYear(era, 1);
                    fail("HijrahChronology.prolepticYear did not throw ClassCastException for Era: " + era);
                } catch (ClassCastException cex) {
                    ; // ignore expected exception
                }
            }
        }
    }
    //-----------------------------------------------------------------------
    // Tests for HijrahChronology resolve
    //-----------------------------------------------------------------------
    @DataProvider(name = "resolve_styleByEra")
    Object[][] data_resolve_styleByEra() {
        Object[][] result = new Object[ResolverStyle.values().length * HijrahEra.values().length][];
        int i = 0;
        for (ResolverStyle style : ResolverStyle.values()) {
            for (HijrahEra era : HijrahEra.values()) {
                result[i++] = new Object[] {style, era};
            }
        }
        return result;
    }

    @Test(dataProvider = "resolve_styleByEra")
    public void test_resolve_yearOfEra_eraOnly_valid(ResolverStyle style, HijrahEra era) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.ERA, (long) era.getValue());
        HijrahDate date = HijrahChronology.INSTANCE.resolveDate(fieldValues, style);
        assertEquals(date, null);
        assertEquals(fieldValues.get(ChronoField.ERA), (Long) (long) era.getValue());
        assertEquals(fieldValues.size(), 1);
    }

    @Test(dataProvider = "resolve_styleByEra")
    public void test_resolve_yearOfEra_eraAndYearOfEraOnly_valid(ResolverStyle style, HijrahEra era) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.ERA, (long) era.getValue());
        fieldValues.put(ChronoField.YEAR_OF_ERA, 1343L);
        HijrahDate date = HijrahChronology.INSTANCE.resolveDate(fieldValues, style);
        assertEquals(date, null);
        assertEquals(fieldValues.get(ChronoField.ERA), null);
        assertEquals(fieldValues.get(ChronoField.YEAR_OF_ERA), null);
        assertEquals(fieldValues.get(ChronoField.YEAR), (Long) 1343L);
        assertEquals(fieldValues.size(), 1);
    }

    @Test(dataProvider = "resolve_styleByEra")
    public void test_resolve_yearOfEra_eraAndYearOnly_valid(ResolverStyle style, HijrahEra era) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.ERA, (long) era.getValue());
        fieldValues.put(ChronoField.YEAR, 1343L);
        HijrahDate date = HijrahChronology.INSTANCE.resolveDate(fieldValues, style);
        assertEquals(date, null);
        assertEquals(fieldValues.get(ChronoField.ERA), (Long) (long) era.getValue());
        assertEquals(fieldValues.get(ChronoField.YEAR), (Long) 1343L);
        assertEquals(fieldValues.size(), 2);
    }

    @DataProvider(name = "resolve_styles")
    Object[][] data_resolve_styles() {
        Object[][] result = new Object[ResolverStyle.values().length][];
        int i = 0;
        for (ResolverStyle style : ResolverStyle.values()) {
            result[i++] = new Object[] {style};
        }
        return result;
    }

    @Test(dataProvider = "resolve_styles")
    public void test_resolve_yearOfEra_yearOfEraOnly_valid(ResolverStyle style) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR_OF_ERA, 1343L);
        HijrahDate date = HijrahChronology.INSTANCE.resolveDate(fieldValues, style);
        assertEquals(date, null);
        assertEquals(fieldValues.get(ChronoField.YEAR_OF_ERA), (style != ResolverStyle.STRICT) ? null : (Long) 1343L);
        assertEquals(fieldValues.get(ChronoField.YEAR), (style == ResolverStyle.STRICT) ? null : (Long) 1343L);
        assertEquals(fieldValues.size(), 1);
    }

    @Test(dataProvider = "resolve_styles")
    public void test_resolve_yearOfEra_yearOfEraAndYearOnly_valid(ResolverStyle style) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR_OF_ERA, 1343L);
        fieldValues.put(ChronoField.YEAR, 1343L);
        HijrahDate date = HijrahChronology.INSTANCE.resolveDate(fieldValues, style);
        assertEquals(date, null);
        assertEquals(fieldValues.get(ChronoField.YEAR_OF_ERA), null);
        assertEquals(fieldValues.get(ChronoField.YEAR), (Long) 1343L);
        assertEquals(fieldValues.size(), 1);
    }

    //-----------------------------------------------------------------------
    // Sample Hijrah Calendar data; official data is in lib/hijrah-ummalqura.properties
    // 1432=29 30 30 30 29 30 29 30 29 30 29 29  total = 354
    // 1433=30 29 30 30 29 30 30 29 30 29 30 29  total = 355
    // 1434=29 30 29 30 29 30 30 29 30 30 29 29  total = 354
    // 1435=30 29 30 29 30 29 30 29 30 30 29 30  total = 355
    //-----------------------------------------------------------------------
    @DataProvider(name = "resolve_ymd")
    Object[][] data_resolve_ymd() {
        // Compute the number of days in various month and years so that test cases
        // are not dependent on specific calendar data
        // Month numbers are always 1..12 so they can be used literally
        final int year = 1434;
        final int yearP1 = year + 1;
        final int yearP2 = year + 2;
        final int yearM1 = year - 1;
        final int yearM2 = year - 2;
        final int lastDayInYear = dateYearDay(year, 1).lengthOfYear();
        final int lastDayInYearP1 = dateYearDay(yearP1, 1).lengthOfYear();
        final int lastDayInYearM1 = dateYearDay(yearM1, 1).lengthOfYear();
        final int lastDayInYearM2 = dateYearDay(yearM2, 1).lengthOfYear();
        final int lastDayInMonthM1 = date(yearM1, 12, 1).lengthOfMonth();
        final int lastDayInMonthM2 = date(yearM1, 11, 1).lengthOfMonth();
        final int lastDayInMonthM11 = date(yearM1, 2, 1).lengthOfMonth();

        final int lastDayInMonth1 = date(year, 1, 1).lengthOfMonth();
        final int lastDayInMonth2 = date(year, 2, 1).lengthOfMonth();
        final int lastDayInMonth4 = date(year, 4, 1).lengthOfMonth();
        final int lastDayInMonth5 = date(year, 5, 1).lengthOfMonth();
        final int lastDayInMonth6 = date(year, 6, 1).lengthOfMonth();
        final int lastDayInMonth7 = date(year, 7, 1).lengthOfMonth();

        return new Object[][] {
                {year, 1, -lastDayInYearM1, dateYearDay(yearM2, lastDayInYearM2), false, false},
                {year, 1, -lastDayInYearM1 + 1, date(yearM1, 1, 1), false, false},
                {year, 1, -lastDayInMonthM1, date(yearM1, 11, lastDayInMonthM2), false, false},
                {year, 1, -lastDayInMonthM1 + 1, date(yearM1, 12, 1), false, false},
                {year, 1, -12, date(yearM1, 12, lastDayInMonthM1 - 12), false, false},
                {year, 1, 1, date(year, 1, 1), true, true},
                {year, 1, lastDayInMonth1 + lastDayInMonth2 - 1, date(year, 2, lastDayInMonth2 - 1), false, false},
                {year, 1, lastDayInMonth1 + lastDayInMonth2, date(year, 2, lastDayInMonth2), false, false},
                {year, 1, lastDayInMonth1 + lastDayInMonth2 + 1 , date(year, 3, 1), false, false},
                {year, 1, lastDayInYear, dateYearDay(year, lastDayInYear), false, false},
                {year, 1, lastDayInYear + 1, date(1435, 1, 1), false, false},
                {year, 1, lastDayInYear + lastDayInYearP1, dateYearDay(yearP1, lastDayInYearP1), false, false},
                {year, 1, lastDayInYear + lastDayInYearP1 + 1, date(yearP2, 1, 1), false, false},

                {year, 2, 1, date(year, 2, 1), true, true},
                {year, 2, lastDayInMonth2 - 2, date(year, 2, lastDayInMonth2 - 2), true, true},
                {year, 2, lastDayInMonth2 - 1, date(year, 2, lastDayInMonth2 - 1), true, true},
                {year, 2, lastDayInMonth2, date(year, 2, lastDayInMonth2), date(year, 2, lastDayInMonth2), true},
                {year, 2, lastDayInMonth2 + 1, date(year, 3, 1), false, false},

                {year, -12, 1, date(yearM2, 12, 1), false, false},
                {year, -11, 1, date(yearM1, 1, 1), false, false},
                {year, -1, 1, date(yearM1, 11, 1), false, false},
                {year, 0, 1, date(yearM1, 12, 1), false, false},
                {year, 1, 1, date(year, 1, 1), true, true},
                {year, 12, 1, date(year, 12, 1), true, true},
                {year, 13, 1, date(yearP1, 1, 1), false, false},
                {year, 24, 1, date(yearP1, 12, 1), false, false},
                {year, 25, 1, date(yearP2, 1, 1), false, false},

                {year, 6, -lastDayInMonth5, date(year, 4, lastDayInMonth4), false, false},
                {year, 6, -lastDayInMonth5 + 1, date(year, 5, 1), false, false},
                {year, 6, -1, date(year, 5, lastDayInMonth5 - 1), false, false},
                {year, 6, 0, date(year, 5, lastDayInMonth5), false, false},
                {year, 6, 1, date(year, 6, 1), true, true},
                {year, 6, lastDayInMonth6 - 1 , date(year, 6, lastDayInMonth6 - 1), true, true},
                {year, 6, lastDayInMonth6, date(year, 6, lastDayInMonth6), date(year, 6, lastDayInMonth6), true},
                {year, 6, lastDayInMonth6 + 1, date(year, 7, 1), false, false},
                {year, 6, lastDayInMonth6 + lastDayInMonth7 , date(year, 7, lastDayInMonth7), false, false},
                {year, 6, lastDayInMonth6 + lastDayInMonth7 + 1, date(year, 8, 1), false, false},

                {yearM1, 2, 1, date(yearM1, 2, 1), true, true},
                {yearM1, 2, lastDayInMonthM11 - 1, date(yearM1, 2, lastDayInMonthM11 - 1), true, true},
                {yearM1, 2, lastDayInMonthM11, date(yearM1, 2, lastDayInMonthM11), true, true},
                {yearM1, 2, lastDayInMonthM11 + 1, date(yearM1, 3, 1), date(yearM1, 2, lastDayInMonthM11), false},
                {yearM1, 2, lastDayInMonthM11 + 2, date(yearM1, 3, 2), false, false},
                // Bad dates
                {1299, 12, 1, null, false, false},
                {1601, 1, 1, null, false, false},

        };
    }

    @Test(dataProvider = "resolve_ymd")
    public void test_resolve_ymd_lenient(int y, int m, int d, HijrahDate expected, Object smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.MONTH_OF_YEAR, (long) m);
        fieldValues.put(ChronoField.DAY_OF_MONTH, (long) d);

        if (expected != null) {
            HijrahDate date = HijrahChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.LENIENT);
            assertEquals(date, expected);
            assertEquals(fieldValues.size(), 0);
        } else {
            try {
                HijrahDate date = HijrahChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.LENIENT);
                fail("Should have failed, returned: " + date);
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    @Test(dataProvider = "resolve_ymd")
    public void test_resolve_ymd_smart(int y, int m, int d, HijrahDate expected, Object smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.MONTH_OF_YEAR, (long) m);
        fieldValues.put(ChronoField.DAY_OF_MONTH, (long) d);
        if (Boolean.TRUE.equals(smart)) {
            HijrahDate date = HijrahChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
            assertEquals(date, expected);
            assertEquals(fieldValues.size(), 0);
        } else if (smart instanceof HijrahDate) {
            HijrahDate date = HijrahChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
            assertEquals(date, smart);
        } else {
            try {
                HijrahDate date = HijrahChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
                fail("Should have failed, returned: " + date);
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    @Test(dataProvider = "resolve_ymd")
    public void test_resolve_ymd_strict(int y, int m, int d, HijrahDate expected, Object smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.MONTH_OF_YEAR, (long) m);
        fieldValues.put(ChronoField.DAY_OF_MONTH, (long) d);
        if (strict) {
            HijrahDate date = HijrahChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.STRICT);
            assertEquals(date, expected, "Resolved to incorrect date");
            assertEquals(fieldValues.size(), 0);
        } else {
            try {
                HijrahDate date = HijrahChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.STRICT);
                fail("Should have failed, returned: " + date);
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    //-----------------------------------------------------------------------
    @DataProvider(name = "resolve_yd")
    Object[][] data_resolve_yd() {
        // Compute the number of days in various month and years so that test cases
        // are not dependent on specific calendar data
        // Month numbers are always 1..12 so they can be used literally
        final int year = 1343;
        final int yearP1 = year + 1;
        final int yearP2 = year + 2;
        final int yearM1 = year - 1;
        final int yearM2 = year - 2;
        final int lastDayInYear = dateYearDay(year, 1).lengthOfYear();
        final int lastDayInYearP1 = dateYearDay(yearP1, 1).lengthOfYear();
        final int lastDayInYearM1 = dateYearDay(yearM1, 1).lengthOfYear();
        final int lastDayInYearM2 = dateYearDay(yearM2, 1).lengthOfYear();

        final int lastDayInMonthM1 = date(yearM1, 12, 1).lengthOfMonth();
        final int lastDayInMonthM2 = date(yearM1, 11, 1).lengthOfMonth();
        final int lastDayInMonthM11 = date(yearM1, 2, 1).lengthOfMonth();

        final int lastDayInMonth1 = date(year, 1, 1).lengthOfMonth();
        final int lastDayInMonth2 = date(year, 2, 1).lengthOfMonth();
        final int lastDayInMonth4 = date(year, 4, 1).lengthOfMonth();
        final int lastDayInMonth5 = date(year, 5, 1).lengthOfMonth();
        final int lastDayInMonth6 = date(year, 6, 1).lengthOfMonth();
        final int lastDayInMonth7 = date(year, 7, 1).lengthOfMonth();

        return new Object[][] {
                {year, -lastDayInYearM1, dateYearDay(yearM2, lastDayInYearM2), false, false},
                {year, -lastDayInYearM1 + 1, date(yearM1, 1, 1), false, false},
                {year, -lastDayInMonthM1, date(yearM1, 11, lastDayInMonthM2), false, false},
                {year, -lastDayInMonthM1 + 1, date(yearM1, 12, 1), false, false},
                {year, -12, date(yearM1, 12, lastDayInMonthM1 - 12), false, false},
                {year, -1, date(yearM1, 12, lastDayInMonthM1 - 1), false, false},
                {year, 0, date(yearM1, 12, lastDayInMonthM1), false, false},
                {year, 1, date(year, 1, 1), true, true},
                {year, 2, date(year, 1, 2), true, true},
                {year, lastDayInMonth1, date(year, 1, lastDayInMonth1), true, true},
                {year, lastDayInMonth1 + 1, date(year, 2, 1), true, true},
                {year, lastDayInMonth1 + lastDayInMonth2 - 1, date(year, 2, lastDayInMonth2 - 1), true, true},
                {year, lastDayInMonth1 + lastDayInMonth2, date(year, 2, lastDayInMonth2), true, true},
                {year, lastDayInMonth1 + lastDayInMonth2 + 1, date(year, 3, 1), true, true},
                {year, lastDayInYear - 1, dateYearDay(year, lastDayInYear - 1), true, true},
                {year, lastDayInYear, dateYearDay(year, lastDayInYear), true, true},
                {year, lastDayInYear + 1, date(yearP1, 1, 1), false, false},
                {year, lastDayInYear + lastDayInYearP1, dateYearDay(yearP1, lastDayInYearP1), false, false},
                {year, lastDayInYear + lastDayInYearP1 + 1, date(yearP2, 1, 1), false, false},
        };
    }

    @Test(dataProvider = "resolve_yd")
    public void test_resolve_yd_lenient(int y, int d, HijrahDate expected, boolean smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.DAY_OF_YEAR, (long) d);
        HijrahDate date = HijrahChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.LENIENT);
        assertEquals(date, expected);
        assertEquals(fieldValues.size(), 0);
    }

    @Test(dataProvider = "resolve_yd")
    public void test_resolve_yd_smart(int y, int d, HijrahDate expected, boolean smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.DAY_OF_YEAR, (long) d);
        if (smart) {
            HijrahDate date = HijrahChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
            assertEquals(date, expected);
            assertEquals(fieldValues.size(), 0);
        } else {
            try {
                HijrahDate date = HijrahChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
                fail("Should have failed, returned date: " + date);
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    @Test(dataProvider = "resolve_yd")
    public void test_resolve_yd_strict(int y, int d, HijrahDate expected, boolean smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.DAY_OF_YEAR, (long) d);
        if (strict) {
            HijrahDate date = HijrahChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.STRICT);
            assertEquals(date, expected);
            assertEquals(fieldValues.size(), 0);
        } else {
            try {
                HijrahDate date = HijrahChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.STRICT);
                fail("Should have failed, returned date: " + date);
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    //-----------------------------------------------------------------------
    private static HijrahDate date(int y, int m, int d) {
        return HijrahDate.of(y, m, d);
    }

    private static HijrahDate dateYearDay(int y, int doy) {
        return HijrahChronology.INSTANCE.dateYearDay(y, doy);
    }

    //-----------------------------------------------------------------------
    // equals()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals_true() {
        assertTrue(HijrahChronology.INSTANCE.equals(HijrahChronology.INSTANCE));
    }

    @Test
    public void test_equals_false() {
        assertFalse(HijrahChronology.INSTANCE.equals(IsoChronology.INSTANCE));
    }
}
