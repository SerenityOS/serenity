/*
 o Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.time.temporal.ChronoField.EPOCH_DAY;
import static java.time.temporal.ChronoField.ERA;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;
import static java.time.temporal.ChronoField.YEAR_OF_ERA;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Period;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoPeriod;
import java.time.chrono.Chronology;
import java.time.chrono.Era;
import java.time.chrono.IsoChronology;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.JapaneseDate;
import java.time.chrono.JapaneseEra;
import java.time.chrono.MinguoChronology;
import java.time.chrono.MinguoDate;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.ValueRange;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class TCKJapaneseChronology {

    // Year differences from Gregorian years.
    private static final int YDIFF_REIWA = 2018;
    private static final int YDIFF_HEISEI = 1988;
    private static final int YDIFF_MEIJI = 1867;
    private static final int YDIFF_SHOWA = 1925;
    private static final int YDIFF_TAISHO = 1911;

    //-----------------------------------------------------------------------
    // Chronology.of(String)
    //-----------------------------------------------------------------------
    @Test
    public void test_chrono_byName() {
        Chronology c = JapaneseChronology.INSTANCE;
        Chronology test = Chronology.of("Japanese");
        Assert.assertNotNull(test, "The Japanese calendar could not be found byName");
        Assert.assertEquals(test.getId(), "Japanese", "ID mismatch");
        Assert.assertEquals(test.getCalendarType(), "japanese", "Type mismatch");
        Assert.assertEquals(test, c);
    }

    //-----------------------------------------------------------------------
    // Chronology.ofLocale(Locale)
    //-----------------------------------------------------------------------
    @Test
    public void test_chrono_byLocale_fullTag_japaneseCalendarFromJapan() {
        Chronology test = Chronology.ofLocale(Locale.forLanguageTag("ja-JP-u-ca-japanese"));
        Assert.assertEquals(test.getId(), "Japanese");
        Assert.assertEquals(test, JapaneseChronology.INSTANCE);
    }

    @Test
    public void test_chrono_byLocale_fullTag_japaneseCalendarFromElsewhere() {
        Chronology test = Chronology.ofLocale(Locale.forLanguageTag("en-US-u-ca-japanese"));
        Assert.assertEquals(test.getId(), "Japanese");
        Assert.assertEquals(test, JapaneseChronology.INSTANCE);
    }

    @Test
    public void test_chrono_byLocale_oldJP_noVariant() {
        Chronology test = Chronology.ofLocale(new Locale("ja", "JP"));
        Assert.assertEquals(test.getId(), "ISO");
        Assert.assertEquals(test, IsoChronology.INSTANCE);
    }

    @Test
    public void test_chrono_byLocale_oldJP_variant() {
        Chronology test = Chronology.ofLocale(new Locale("ja", "JP", "JP"));
        Assert.assertEquals(test.getId(), "Japanese");
        Assert.assertEquals(test, JapaneseChronology.INSTANCE);
    }

    @Test
    public void test_chrono_byLocale_iso() {
        Assert.assertEquals(Chronology.ofLocale(new Locale("ja", "JP")).getId(), "ISO");
        Assert.assertEquals(Chronology.ofLocale(Locale.forLanguageTag("ja-JP")).getId(), "ISO");
        Assert.assertEquals(Chronology.ofLocale(Locale.forLanguageTag("ja-JP-JP")).getId(), "ISO");
    }

    //-----------------------------------------------------------------------
    // creation and cross-checks
    //-----------------------------------------------------------------------
    @DataProvider(name="createByEra")
    Object[][] data_createByEra() {
        return new Object[][] {
                {JapaneseEra.REIWA, 2020 - YDIFF_REIWA, 2, 29, 60, LocalDate.of(2020, 2, 29)},
                {JapaneseEra.HEISEI, 1996 - YDIFF_HEISEI, 2, 29, 60, LocalDate.of(1996, 2, 29)},
                {JapaneseEra.HEISEI, 2000 - YDIFF_HEISEI, 2, 29, 60, LocalDate.of(2000, 2, 29)},
                {JapaneseEra.MEIJI, 1874 - YDIFF_MEIJI, 2, 28, 59, LocalDate.of(1874, 2, 28)},
                {JapaneseEra.SHOWA, 1928 - YDIFF_SHOWA, 12, 25, 360, LocalDate.of(1928, 12, 25)},
                {JapaneseEra.TAISHO, 1916 - YDIFF_TAISHO, 7, 30, 212, LocalDate.of(1916, 7, 30)},
                {JapaneseEra.MEIJI, 6, 1, 1, 1, LocalDate.of(1873, 1, 1)},
                {JapaneseEra.MEIJI, 45, 7, 29, 211, LocalDate.of(1912, 7, 29)},
                {JapaneseEra.TAISHO, 1, 7, 30, 1, LocalDate.of(1912, 7, 30)},
                {JapaneseEra.TAISHO, 15, 12, 24, 358, LocalDate.of(1926, 12, 24)},
                {JapaneseEra.SHOWA, 1, 12, 25, 1, LocalDate.of(1926, 12, 25)},
                {JapaneseEra.SHOWA, 64, 1, 7, 7, LocalDate.of(1989, 1, 7)},
                {JapaneseEra.HEISEI, 1, 1, 8, 1, LocalDate.of(1989, 1, 8)},
        };
    }

    @Test(dataProvider="createByEra")
    public void test_createEymd(JapaneseEra era, int yoe, int moy, int dom, int doy, LocalDate iso) {
        JapaneseDate dateByChronoFactory = JapaneseChronology.INSTANCE.date(era, yoe, moy, dom);
        JapaneseDate dateByDateFactory = JapaneseDate.of(era, yoe, moy, dom);
        assertEquals(dateByChronoFactory, dateByDateFactory);
        assertEquals(dateByChronoFactory.hashCode(), dateByDateFactory.hashCode());
    }

    @Test(dataProvider="createByEra")
    public void test_createEyd(JapaneseEra era, int yoe, int moy, int dom, int doy, LocalDate iso) {
        JapaneseDate dateByChronoFactory = JapaneseChronology.INSTANCE.dateYearDay(era, yoe, doy);
        JapaneseDate dateByDateFactory = JapaneseDate.of(era, yoe, moy, dom);
        assertEquals(dateByChronoFactory, dateByDateFactory);
        assertEquals(dateByChronoFactory.hashCode(), dateByDateFactory.hashCode());
    }

    @Test(dataProvider="createByEra")
    public void test_createByEra_isEqual(JapaneseEra era, int yoe, int moy, int dom, int doy, LocalDate iso) {
        JapaneseDate test = JapaneseDate.of(era, yoe, moy, dom);
        assertEquals(test.isEqual(iso), true);
        assertEquals(iso.isEqual(test), true);
    }

    @Test(dataProvider="createByEra")
    public void test_createByEra_chronologyTemporalFactory(JapaneseEra era, int yoe, int moy, int dom, int doy, LocalDate iso) {
        JapaneseDate test = JapaneseDate.of(era, yoe, moy, dom);
        assertEquals(IsoChronology.INSTANCE.date(test), iso);
        assertEquals(JapaneseChronology.INSTANCE.date(iso), test);
    }

    @Test(dataProvider="createByEra")
    public void test_createByEra_dateFrom(JapaneseEra era, int yoe, int moy, int dom, int doy, LocalDate iso) {
        JapaneseDate test = JapaneseDate.of(era, yoe, moy, dom);
        assertEquals(LocalDate.from(test), iso);
        assertEquals(JapaneseDate.from(iso), test);
    }

    @Test(dataProvider="createByEra")
    public void test_createByEra_query(JapaneseEra era, int yoe, int moy, int dom, int doy, LocalDate iso) {
        JapaneseDate test = JapaneseDate.of(era, yoe, moy, dom);
        assertEquals(test.query(TemporalQueries.localDate()), iso);
    }

    @Test(dataProvider="createByEra")
    public void test_createByEra_epochDay(JapaneseEra era, int yoe, int moy, int dom, int doy, LocalDate iso) {
        JapaneseDate test = JapaneseDate.of(era, yoe, moy, dom);
        assertEquals(test.getLong(EPOCH_DAY), iso.getLong(EPOCH_DAY));
        assertEquals(test.toEpochDay(), iso.toEpochDay());
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="createByProleptic")
    Object[][] data_createByProleptic() {
        return new Object[][] {
                {1928, 2, 28, 59, LocalDate.of(1928, 2, 28)},
                {1928, 2, 29, 60, LocalDate.of(1928, 2, 29)},

                {1873, 9, 7, 250, LocalDate.of(1873, 9, 7)},
                {1873, 9, 8, 251, LocalDate.of(1873, 9, 8)},
                {1912, 7, 29, 211, LocalDate.of(1912, 7, 29)},
                {1912, 7, 30, 212, LocalDate.of(1912, 7, 30)},
                {1926, 12, 24, 358, LocalDate.of(1926, 12, 24)},
                {1926, 12, 25, 359, LocalDate.of(1926, 12, 25)},
                {1989, 1, 7, 7, LocalDate.of(1989, 1, 7)},
                {1989, 1, 8, 8, LocalDate.of(1989, 1, 8)},
        };
    }

    @Test(dataProvider="createByProleptic")
    public void test_createYmd(int y, int moy, int dom, int doy, LocalDate iso) {
        JapaneseDate dateByChronoFactory = JapaneseChronology.INSTANCE.date(y, moy, dom);
        JapaneseDate dateByDateFactory = JapaneseDate.of(y, moy, dom);
        assertEquals(dateByChronoFactory, dateByDateFactory);
        assertEquals(dateByChronoFactory.hashCode(), dateByDateFactory.hashCode());
    }

    @Test(dataProvider="createByProleptic")
    public void test_createYd(int y, int moy, int dom, int doy, LocalDate iso) {
        JapaneseDate dateByChronoFactory = JapaneseChronology.INSTANCE.dateYearDay(y, doy);
        JapaneseDate dateByDateFactory = JapaneseDate.of(y, moy, dom);
        assertEquals(dateByChronoFactory, dateByDateFactory);
        assertEquals(dateByChronoFactory.hashCode(), dateByDateFactory.hashCode());
    }

    @Test(dataProvider="createByProleptic")
    public void test_createByProleptic_isEqual(int y, int moy, int dom, int doy, LocalDate iso) {
        JapaneseDate test = JapaneseDate.of(y, moy, dom);
        assertEquals(test.isEqual(iso), true);
        assertEquals(iso.isEqual(test), true);
    }

    @Test(dataProvider="createByProleptic")
    public void test_createByProleptic_chronologyTemporalFactory(int y, int moy, int dom, int doy, LocalDate iso) {
        JapaneseDate test = JapaneseDate.of(y, moy, dom);
        assertEquals(IsoChronology.INSTANCE.date(test), iso);
        assertEquals(JapaneseChronology.INSTANCE.date(iso), test);
    }

    @Test(dataProvider="createByProleptic")
    public void test_createByProleptic_dateFrom(int y, int moy, int dom, int doy, LocalDate iso) {
        JapaneseDate test = JapaneseDate.of(y, moy, dom);
        assertEquals(LocalDate.from(test), iso);
        assertEquals(JapaneseDate.from(iso), test);
    }

    @Test(dataProvider="createByProleptic")
    public void test_createByProleptic_query(int y, int moy, int dom, int doy, LocalDate iso) {
        JapaneseDate test = JapaneseDate.of(y, moy, dom);
        assertEquals(test.query(TemporalQueries.localDate()), iso);
    }

    @Test(dataProvider="createByProleptic")
    public void test_createByProleptic_epochDay(int y, int moy, int dom, int doy, LocalDate iso) {
        JapaneseDate test = JapaneseDate.of(y, moy, dom);
        assertEquals(test.getLong(EPOCH_DAY), iso.getLong(EPOCH_DAY));
        assertEquals(test.toEpochDay(), iso.toEpochDay());
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_dateNow(){
        assertEquals(JapaneseChronology.INSTANCE.dateNow(), JapaneseDate.now()) ;
        assertEquals(JapaneseChronology.INSTANCE.dateNow(), JapaneseDate.now(ZoneId.systemDefault())) ;
        assertEquals(JapaneseChronology.INSTANCE.dateNow(), JapaneseDate.now(Clock.systemDefaultZone())) ;
        assertEquals(JapaneseChronology.INSTANCE.dateNow(), JapaneseDate.now(Clock.systemDefaultZone().getZone())) ;

        assertEquals(JapaneseChronology.INSTANCE.dateNow(), JapaneseChronology.INSTANCE.dateNow(ZoneId.systemDefault())) ;
        assertEquals(JapaneseChronology.INSTANCE.dateNow(), JapaneseChronology.INSTANCE.dateNow(Clock.systemDefaultZone())) ;
        assertEquals(JapaneseChronology.INSTANCE.dateNow(), JapaneseChronology.INSTANCE.dateNow(Clock.systemDefaultZone().getZone())) ;

        ZoneId zoneId = ZoneId.of("Europe/Paris");
        assertEquals(JapaneseChronology.INSTANCE.dateNow(zoneId), JapaneseChronology.INSTANCE.dateNow(Clock.system(zoneId))) ;
        assertEquals(JapaneseChronology.INSTANCE.dateNow(zoneId), JapaneseChronology.INSTANCE.dateNow(Clock.system(zoneId).getZone())) ;
        assertEquals(JapaneseChronology.INSTANCE.dateNow(zoneId), JapaneseDate.now(Clock.system(zoneId))) ;
        assertEquals(JapaneseChronology.INSTANCE.dateNow(zoneId), JapaneseDate.now(Clock.system(zoneId).getZone())) ;

        assertEquals(JapaneseChronology.INSTANCE.dateNow(ZoneId.of(ZoneOffset.UTC.getId())), JapaneseChronology.INSTANCE.dateNow(Clock.systemUTC())) ;
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="badDates")
    Object[][] data_badDates() {
        return new Object[][] {
            {1928, 0, 0},

            {1928, -1, 1},
            {1928, 0, 1},
            {1928, 14, 1},
            {1928, 15, 1},

            {1928, 1, -1},
            {1928, 1, 0},
            {1928, 1, 32},

            {1928, 12, -1},
            {1928, 12, 0},
            {1928, 12, 32},

            {1725, 2, 29},
            {500, 2, 29},
            {2100, 2, 29},

            {1872, 12, 31},     // Last day of MEIJI 5
        };
    }

    @Test(dataProvider="badDates", expectedExceptions=DateTimeException.class)
    public void test_badDates(int year, int month, int dom) {
        JapaneseChronology.INSTANCE.date(year, month, dom);
    }

    //-----------------------------------------------------------------------
    // prolepticYear() and is LeapYear()
    //-----------------------------------------------------------------------
    @DataProvider(name="prolepticYear")
    Object[][] data_prolepticYear() {
        return new Object[][] {
                {3, JapaneseEra.REIWA, 1, 1 + YDIFF_REIWA, false},
                {3, JapaneseEra.REIWA, 102, 102 + YDIFF_REIWA, true},

                {2, JapaneseEra.HEISEI, 1, 1 + YDIFF_HEISEI, false},
                {2, JapaneseEra.HEISEI, 4, 4 + YDIFF_HEISEI, true},

                {-1, JapaneseEra.MEIJI, 9, 9 + YDIFF_MEIJI, true},
                {-1, JapaneseEra.MEIJI, 10, 10 + YDIFF_MEIJI, false},

                {1, JapaneseEra.SHOWA, 1, 1 + YDIFF_SHOWA, false},
                {1, JapaneseEra.SHOWA, 7, 7 + YDIFF_SHOWA, true},

                {0, JapaneseEra.TAISHO, 1, 1 + YDIFF_TAISHO, true},
                {0, JapaneseEra.TAISHO, 4, 4 + YDIFF_TAISHO, false},
        };
    }

    @Test(dataProvider="prolepticYear")
    public void test_prolepticYear(int eraValue, Era  era, int yearOfEra, int expectedProlepticYear, boolean isLeapYear) {
        Era eraObj = JapaneseChronology.INSTANCE.eraOf(eraValue);
        assertTrue(JapaneseChronology.INSTANCE.eras().contains(eraObj));
        assertEquals(eraObj, era);
        assertEquals(JapaneseChronology.INSTANCE.prolepticYear(era, yearOfEra), expectedProlepticYear);
    }

    @Test(dataProvider="prolepticYear")
    public void test_isLeapYear(int eraValue, Era  era, int yearOfEra, int expectedProlepticYear, boolean isLeapYear) {
        assertEquals(JapaneseChronology.INSTANCE.isLeapYear(expectedProlepticYear), isLeapYear);
        assertEquals(JapaneseChronology.INSTANCE.isLeapYear(expectedProlepticYear), Year.of(expectedProlepticYear).isLeap());

        JapaneseDate jdate = JapaneseDate.now();
        jdate = jdate.with(ChronoField.YEAR, expectedProlepticYear).with(ChronoField.MONTH_OF_YEAR, 2);
        if (isLeapYear) {
            assertEquals(jdate.lengthOfMonth(), 29);
        } else {
            assertEquals(jdate.lengthOfMonth(), 28);
        }
    }

    @DataProvider(name="prolepticYearError")
    Object[][] data_prolepticYearError() {
        return new Object[][] {
                {JapaneseEra.MEIJI, 100},
                {JapaneseEra.MEIJI, 0},
                {JapaneseEra.MEIJI, -10},

                {JapaneseEra.SHOWA, 100},
                {JapaneseEra.SHOWA, 0},
                {JapaneseEra.SHOWA, -10},

                {JapaneseEra.TAISHO, 100},
                {JapaneseEra.TAISHO, 0},
                {JapaneseEra.TAISHO, -10},
        };
    }

    @Test(dataProvider="prolepticYearError", expectedExceptions=DateTimeException.class)
    public void test_prolepticYearError(Era era, int yearOfEra) {
        JapaneseChronology.INSTANCE.prolepticYear(era, yearOfEra);
    }

    //-----------------------------------------------------------------------
    // Bad Era for Chronology.date(era,...) and Chronology.prolepticYear(Era,...)
    //-----------------------------------------------------------------------
    @Test
    public void test_InvalidEras() {
        // Verify that the eras from every other Chronology are invalid
        for (Chronology chrono : Chronology.getAvailableChronologies()) {
            if (chrono instanceof JapaneseChronology) {
                continue;
            }
            List<Era> eras = chrono.eras();
            for (Era era : eras) {
                try {
                    ChronoLocalDate date = JapaneseChronology.INSTANCE.date(era, 1, 1, 1);
                    fail("JapaneseChronology.date did not throw ClassCastException for Era: " + era);
                } catch (ClassCastException cex) {
                    ; // ignore expected exception
                }
                try {
                    @SuppressWarnings("unused")
                    int year = JapaneseChronology.INSTANCE.prolepticYear(era, 1);
                    fail("JapaneseChronology.prolepticYear did not throw ClassCastException for Era: " + era);
                } catch (ClassCastException cex) {
                    ; // ignore expected exception
                }

            }
        }
    }

    //-----------------------------------------------------------------------
    // get(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_getLong() {
        JapaneseDate base = JapaneseChronology.INSTANCE.date(JapaneseEra.SHOWA, 63, 6, 30);
        assertEquals(base.getLong(ERA), JapaneseEra.SHOWA.getValue());
        assertEquals(base.getLong(YEAR), 1988L);
        assertEquals(base.getLong(YEAR_OF_ERA), 63L);
        assertEquals(base.getLong(MONTH_OF_YEAR), 6L);
        assertEquals(base.getLong(DAY_OF_MONTH), 30L);
    }

    //-----------------------------------------------------------------------
    // with(TemporalField, long)
    //-----------------------------------------------------------------------
    @Test
    public void test_with_TemporalField_long() {
        JapaneseDate base = JapaneseChronology.INSTANCE.date(JapaneseEra.SHOWA, 63, 6, 30);
        JapaneseDate test = base.with(YEAR, 1987);
        assertEquals(test, JapaneseChronology.INSTANCE.date(JapaneseEra.SHOWA, 62, 6, 30));

        test = test.with(YEAR_OF_ERA, 2);
        assertEquals(test, JapaneseChronology.INSTANCE.date(JapaneseEra.SHOWA, 2, 6, 30));

        test = test.with(ERA, JapaneseEra.HEISEI.getValue());
        assertEquals(test, JapaneseChronology.INSTANCE.date(JapaneseEra.HEISEI, 2, 6, 30));

        test = test.with(MONTH_OF_YEAR, 3);
        assertEquals(test, JapaneseChronology.INSTANCE.date(JapaneseEra.HEISEI, 2, 3, 30));

        test = test.with(DAY_OF_MONTH, 4);
        assertEquals(test, JapaneseChronology.INSTANCE.date(JapaneseEra.HEISEI, 2, 3, 4));
    }

    //-----------------------------------------------------------------------
    // with(WithAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_adjust1() {
        JapaneseDate base = JapaneseChronology.INSTANCE.date(1928, 10, 29);
        JapaneseDate test = base.with(TemporalAdjusters.lastDayOfMonth());
        assertEquals(test, JapaneseChronology.INSTANCE.date(1928, 10, 31));
    }

    @Test
    public void test_adjust2() {
        JapaneseDate base = JapaneseChronology.INSTANCE.date(1928, 12, 2);
        JapaneseDate test = base.with(TemporalAdjusters.lastDayOfMonth());
        assertEquals(test, JapaneseChronology.INSTANCE.date(1928, 12, 31));
    }

    //-----------------------------------------------------------------------
    // JapaneseDate.with(Local*)
    //-----------------------------------------------------------------------
    @Test
    public void test_adjust_toLocalDate() {
        JapaneseDate jdate = JapaneseChronology.INSTANCE.date(1926, 1, 4);
        JapaneseDate test = jdate.with(LocalDate.of(2012, 7, 6));
        assertEquals(test, JapaneseChronology.INSTANCE.date(2012, 7, 6));
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_adjust_toMonth() {
        JapaneseDate jdate = JapaneseChronology.INSTANCE.date(1926, 1, 4);
        jdate.with(Month.APRIL);
    }

    //-----------------------------------------------------------------------
    // LocalDate.with(JapaneseDate)
    //-----------------------------------------------------------------------
    @Test
    public void test_LocalDate_adjustToJapaneseDate() {
        JapaneseDate jdate = JapaneseChronology.INSTANCE.date(1928, 10, 29);
        LocalDate test = LocalDate.MIN.with(jdate);
        assertEquals(test, LocalDate.of(1928, 10, 29));
    }

    @Test
    public void test_LocalDateTime_adjustToJapaneseDate() {
        JapaneseDate jdate = JapaneseChronology.INSTANCE.date(1928, 10, 29);
        LocalDateTime test = LocalDateTime.MIN.with(jdate);
        assertEquals(test, LocalDateTime.of(1928, 10, 29, 0, 0));
    }

    //-----------------------------------------------------------------------
    // Check Japanese Eras
    //-----------------------------------------------------------------------
    @DataProvider(name="japaneseEras")
    Object[][] data_japanseseEras() {
        return new Object[][] {
            { JapaneseEra.MEIJI, -1, "Meiji"},
            { JapaneseEra.TAISHO, 0, "Taisho"},
            { JapaneseEra.SHOWA, 1, "Showa"},
            { JapaneseEra.HEISEI, 2, "Heisei"},
            { JapaneseEra.REIWA, 3, "Reiwa"},
        };
    }

    @Test(dataProvider="japaneseEras")
    public void test_Japanese_Eras(Era era, int eraValue, String name) {
        assertEquals(era.getValue(), eraValue, "EraValue");
        assertEquals(era.toString(), name, "Era Name");
        assertEquals(era, JapaneseChronology.INSTANCE.eraOf(eraValue), "JapaneseChronology.eraOf()");
        List<Era> eras = JapaneseChronology.INSTANCE.eras();
        assertTrue(eras.contains(era), "Era is not present in JapaneseChronology.INSTANCE.eras()");
    }

    @Test
    public void test_Japanese_badEras() {
        int badEras[] = {-1000, -998, -997, -2, 4, 5, 1000};
        for (int badEra : badEras) {
            try {
                Era era = JapaneseChronology.INSTANCE.eraOf(badEra);
                fail("JapaneseChronology.eraOf returned " + era + " + for invalid eraValue " + badEra);
            } catch (DateTimeException ex) {
                // ignore expected exception
            }
        }
    }

    @Test(dataProvider="japaneseEras")
    public void test_JapaneseEra_singletons(Era expectedEra, int eraValue, String name) {
        JapaneseEra actualEra = JapaneseEra.valueOf(name);
        assertEquals(actualEra, expectedEra, "JapaneseEra.valueOf(name)");

        actualEra = JapaneseEra.of(eraValue);
        assertEquals(actualEra, expectedEra, "JapaneseEra.of(value)");

        String string = actualEra.toString();
        assertEquals(string, name, "JapaneseEra.toString()");
    }

    @Test
    public void test_JapaneseEra_values() {
        JapaneseEra[] actualEras = JapaneseEra.values();
        Object[][] erasInfo = data_japanseseEras();
        assertEquals(actualEras.length, erasInfo.length, "Wrong number of Eras");

        for (int i = 0; i < erasInfo.length; i++) {
            Object[] eraInfo = erasInfo[i];
            assertEquals(actualEras[i], eraInfo[0], "Singleton mismatch");
        }
    }

    @Test
    public void test_JapaneseChronology_eras() {
        List<Era> actualEras = JapaneseChronology.INSTANCE.eras();
        Object[][] erasInfo = data_japanseseEras();
        assertEquals(actualEras.size(), erasInfo.length, "Wrong number of Eras");

        for (int i = 0; i < erasInfo.length; i++) {
            Object[] eraInfo = erasInfo[i];
            assertEquals(actualEras.get(i), eraInfo[0], "Singleton mismatch");
        }
    }

    //-----------------------------------------------------------------------
    // PeriodUntil()
    //-----------------------------------------------------------------------
    @Test
    public void test_periodUntilDate() {
        JapaneseDate mdate1 = JapaneseDate.of(1970, 1, 1);
        JapaneseDate mdate2 = JapaneseDate.of(1971, 2, 2);
        ChronoPeriod period = mdate1.until(mdate2);
        assertEquals(period, JapaneseChronology.INSTANCE.period(1, 1, 1));
    }

    @Test
    public void test_periodUntilUnit() {
        JapaneseDate mdate1 = JapaneseDate.of(1970, 1, 1);
        JapaneseDate mdate2 = JapaneseDate.of(1971, 2, 2);
        long months = mdate1.until(mdate2, ChronoUnit.MONTHS);
        assertEquals(months, 13);
    }

    @Test
    public void test_periodUntilDiffChrono() {
        JapaneseDate mdate1 = JapaneseDate.of(1970, 1, 1);
        JapaneseDate mdate2 = JapaneseDate.of(1971, 2, 2);
        MinguoDate ldate2 = MinguoChronology.INSTANCE.date(mdate2);
        ChronoPeriod period = mdate1.until(ldate2);
        assertEquals(period, JapaneseChronology.INSTANCE.period(1, 1, 1));
    }

    //-----------------------------------------------------------------------
    // JapaneseChronology.dateYearDay, getDayOfYear
    //-----------------------------------------------------------------------
    @Test
    public void test_getDayOfYear() {
        // Test all the Eras
        for (JapaneseEra era : JapaneseEra.values()) {
            int firstYear = (era == JapaneseEra.MEIJI) ? 6 : 1;  // Until Era supports range(YEAR_OF_ERA)
            JapaneseDate hd1 = JapaneseChronology.INSTANCE.dateYearDay(era, firstYear, 1);
            ValueRange range = hd1.range(DAY_OF_YEAR);
            assertEquals(range.getMaximum(), hd1.lengthOfYear(), "lengthOfYear should match range.getMaximum()");

            for (int i = 1; i <= hd1.lengthOfYear(); i++) {
                JapaneseDate hd = JapaneseChronology.INSTANCE.dateYearDay(era, firstYear, i);
                int doy = hd.get(DAY_OF_YEAR);
                assertEquals(doy, i, "get(DAY_OF_YEAR) incorrect for " + i + ", of date: " + hd);
            }
        }
    }

    @Test
    public void test_withDayOfYear() {
        JapaneseDate hd = JapaneseChronology.INSTANCE.dateYearDay(1990, 1);
        for (int i = 1; i <= hd.lengthOfYear(); i++) {
            JapaneseDate hd2 = hd.with(DAY_OF_YEAR, i);
            int doy = hd2.get(DAY_OF_YEAR);
            assertEquals(doy, i, "with(DAY_OF_YEAR) incorrect for " + i + " " + hd2);
        }
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @DataProvider(name="toString")
    Object[][] data_toString() {
        return new Object[][] {
            {JapaneseChronology.INSTANCE.date(1873, 12,  5), "Japanese Meiji 6-12-05"},
            {JapaneseChronology.INSTANCE.date(1873, 12,  6), "Japanese Meiji 6-12-06"},
            {JapaneseChronology.INSTANCE.date(1873,  9,  8), "Japanese Meiji 6-09-08"},
            {JapaneseChronology.INSTANCE.date(1912,  7, 29), "Japanese Meiji 45-07-29"},
            {JapaneseChronology.INSTANCE.date(1912,  7, 30), "Japanese Taisho 1-07-30"},
            {JapaneseChronology.INSTANCE.date(1926, 12, 24), "Japanese Taisho 15-12-24"},
            {JapaneseChronology.INSTANCE.date(1926, 12, 25), "Japanese Showa 1-12-25"},
            {JapaneseChronology.INSTANCE.date(1989,  1,  7), "Japanese Showa 64-01-07"},
            {JapaneseChronology.INSTANCE.date(1989,  1,  8), "Japanese Heisei 1-01-08"},
            {JapaneseChronology.INSTANCE.date(2012, 12,  6), "Japanese Heisei 24-12-06"},
            {JapaneseChronology.INSTANCE.date(2020,  1,  6), "Japanese Reiwa 2-01-06"},
        };
    }

    @Test(dataProvider="toString")
    public void test_toString(JapaneseDate jdate, String expected) {
        assertEquals(jdate.toString(), expected);
    }

    //-----------------------------------------------------------------------
    // equals()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals_true() {
        assertTrue(JapaneseChronology.INSTANCE.equals(JapaneseChronology.INSTANCE));
    }

    @Test
    public void test_equals_false() {
        assertFalse(JapaneseChronology.INSTANCE.equals(IsoChronology.INSTANCE));
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name = "resolve_styleByEra")
    Object[][] data_resolve_styleByEra() {
        Object[][] result = new Object[ResolverStyle.values().length * JapaneseEra.values().length][];
        int i = 0;
        for (ResolverStyle style : ResolverStyle.values()) {
            for (JapaneseEra era : JapaneseEra.values()) {
                result[i++] = new Object[] {style, era};
            }
        }
        return result;
    }

    @Test(dataProvider = "resolve_styleByEra")
    public void test_resolve_yearOfEra_eraOnly_valid(ResolverStyle style, JapaneseEra era) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.ERA, (long) era.getValue());
        JapaneseDate date = JapaneseChronology.INSTANCE.resolveDate(fieldValues, style);
        assertEquals(date, null);
        assertEquals(fieldValues.get(ChronoField.ERA), (Long) (long) era.getValue());
        assertEquals(fieldValues.size(), 1);
    }

    @Test(dataProvider = "resolve_styleByEra")
    public void test_resolve_yearOfEra_eraAndYearOfEraOnly_valid(ResolverStyle style, JapaneseEra era) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.ERA, (long) era.getValue());
        fieldValues.put(ChronoField.YEAR_OF_ERA, 1L);
        JapaneseDate date = JapaneseChronology.INSTANCE.resolveDate(fieldValues, style);
        assertEquals(date, null);
        assertEquals(fieldValues.get(ChronoField.ERA), (Long) (long) era.getValue());
        assertEquals(fieldValues.get(ChronoField.YEAR_OF_ERA), (Long) 1L);
        assertEquals(fieldValues.size(), 2);
    }

    @Test(dataProvider = "resolve_styleByEra")
    public void test_resolve_yearOfEra_eraAndYearOnly_valid(ResolverStyle style, JapaneseEra era) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.ERA, (long) era.getValue());
        fieldValues.put(ChronoField.YEAR, 1L);
        JapaneseDate date = JapaneseChronology.INSTANCE.resolveDate(fieldValues, style);
        assertEquals(date, null);
        assertEquals(fieldValues.get(ChronoField.ERA), (Long) (long) era.getValue());
        assertEquals(fieldValues.get(ChronoField.YEAR), (Long) 1L);
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
        fieldValues.put(ChronoField.YEAR_OF_ERA, 1L);
        JapaneseDate date = JapaneseChronology.INSTANCE.resolveDate(fieldValues, style);
        assertEquals(date, null);
        assertEquals(fieldValues.get(ChronoField.YEAR_OF_ERA), (Long) 1L);
        assertEquals(fieldValues.size(), 1);
    }

    @Test(dataProvider = "resolve_styles")
    public void test_resolve_yearOfEra_yearOfEraAndYearOnly_valid(ResolverStyle style) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR_OF_ERA, 1L);
        fieldValues.put(ChronoField.YEAR, 2012L);
        JapaneseDate date = JapaneseChronology.INSTANCE.resolveDate(fieldValues, style);
        assertEquals(date, null);
        assertEquals(fieldValues.get(ChronoField.YEAR_OF_ERA), (Long) 1L);
        assertEquals(fieldValues.get(ChronoField.YEAR), (Long) 2012L);
        assertEquals(fieldValues.size(), 2);
    }

    public void test_resolve_yearOfEra_eraOnly_invalidTooSmall() {
        for (ResolverStyle style : ResolverStyle.values()) {
            Map<TemporalField, Long> fieldValues = new HashMap<>();
            fieldValues.put(ChronoField.ERA, JapaneseEra.MEIJI.getValue() - 1L);
            try {
                JapaneseChronology.INSTANCE.resolveDate(fieldValues, style);
                fail("Should have failed: " + style);
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    public void test_resolve_yearOfEra_eraOnly_invalidTooLarge() {
        for (ResolverStyle style : ResolverStyle.values()) {
            Map<TemporalField, Long> fieldValues = new HashMap<>();
            fieldValues.put(ChronoField.ERA, JapaneseEra.values()[JapaneseEra.values().length - 1].getValue() + 1L);
            try {
                JapaneseChronology.INSTANCE.resolveDate(fieldValues, style);
                fail("Should have failed: " + style);
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name = "resolve_ymd")
    Object[][] data_resolve_ymd() {
        return new Object[][] {
                {2012, 1, -365, date(2010, 12, 31), false, false},
                {2012, 1, -364, date(2011, 1, 1), false, false},
                {2012, 1, -31, date(2011, 11, 30), false, false},
                {2012, 1, -30, date(2011, 12, 1), false, false},
                {2012, 1, -12, date(2011, 12, 19), false, false},
                {2012, 1, 1, date(2012, 1, 1), true, true},
                {2012, 1, 59, date(2012, 2, 28), false, false},
                {2012, 1, 60, date(2012, 2, 29), false, false},
                {2012, 1, 61, date(2012, 3, 1), false, false},
                {2012, 1, 365, date(2012, 12, 30), false, false},
                {2012, 1, 366, date(2012, 12, 31), false, false},
                {2012, 1, 367, date(2013, 1, 1), false, false},
                {2012, 1, 367 + 364, date(2013, 12, 31), false, false},
                {2012, 1, 367 + 365, date(2014, 1, 1), false, false},

                {2012, 2, 1, date(2012, 2, 1), true, true},
                {2012, 2, 28, date(2012, 2, 28), true, true},
                {2012, 2, 29, date(2012, 2, 29), true, true},
                {2012, 2, 30, date(2012, 3, 1), date(2012, 2, 29), false},
                {2012, 2, 31, date(2012, 3, 2), date(2012, 2, 29), false},
                {2012, 2, 32, date(2012, 3, 3), false, false},

                {2012, -12, 1, date(2010, 12, 1), false, false},
                {2012, -11, 1, date(2011, 1, 1), false, false},
                {2012, -1, 1, date(2011, 11, 1), false, false},
                {2012, 0, 1, date(2011, 12, 1), false, false},
                {2012, 1, 1, date(2012, 1, 1), true, true},
                {2012, 12, 1, date(2012, 12, 1), true, true},
                {2012, 13, 1, date(2013, 1, 1), false, false},
                {2012, 24, 1, date(2013, 12, 1), false, false},
                {2012, 25, 1, date(2014, 1, 1), false, false},

                {2012, 6, -31, date(2012, 4, 30), false, false},
                {2012, 6, -30, date(2012, 5, 1), false, false},
                {2012, 6, -1, date(2012, 5, 30), false, false},
                {2012, 6, 0, date(2012, 5, 31), false, false},
                {2012, 6, 1, date(2012, 6, 1), true, true},
                {2012, 6, 30, date(2012, 6, 30), true, true},
                {2012, 6, 31, date(2012, 7, 1), date(2012, 6, 30), false},
                {2012, 6, 61, date(2012, 7, 31), false, false},
                {2012, 6, 62, date(2012, 8, 1), false, false},

                {2011, 2, 1, date(2011, 2, 1), true, true},
                {2011, 2, 28, date(2011, 2, 28), true, true},
                {2011, 2, 29, date(2011, 3, 1), date(2011, 2, 28), false},
                {2011, 2, 30, date(2011, 3, 2), date(2011, 2, 28), false},
                {2011, 2, 31, date(2011, 3, 3), date(2011, 2, 28), false},
                {2011, 2, 32, date(2011, 3, 4), false, false},
        };
    }

    @Test(dataProvider = "resolve_ymd")
    public void test_resolve_ymd_lenient(int y, int m, int d, JapaneseDate expected, Object smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.MONTH_OF_YEAR, (long) m);
        fieldValues.put(ChronoField.DAY_OF_MONTH, (long) d);
        JapaneseDate date = JapaneseChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.LENIENT);
        assertEquals(date, expected);
        assertEquals(fieldValues.size(), 0);
    }

    @Test(dataProvider = "resolve_ymd")
    public void test_resolve_ymd_smart(int y, int m, int d, JapaneseDate expected, Object smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.MONTH_OF_YEAR, (long) m);
        fieldValues.put(ChronoField.DAY_OF_MONTH, (long) d);
        if (Boolean.TRUE.equals(smart)) {
            JapaneseDate date = JapaneseChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
            assertEquals(date, expected);
            assertEquals(fieldValues.size(), 0);
        } else if (smart instanceof JapaneseDate) {
            JapaneseDate date = JapaneseChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
            assertEquals(date, smart);
        } else {
            try {
                JapaneseChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
                fail("Should have failed");
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    @Test(dataProvider = "resolve_ymd")
    public void test_resolve_ymd_strict(int y, int m, int d, JapaneseDate expected, Object smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.MONTH_OF_YEAR, (long) m);
        fieldValues.put(ChronoField.DAY_OF_MONTH, (long) d);
        if (strict) {
            JapaneseDate date = JapaneseChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.STRICT);
            assertEquals(date, expected);
            assertEquals(fieldValues.size(), 0);
        } else {
            try {
                JapaneseChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.STRICT);
                fail("Should have failed");
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name = "resolve_yd")
    Object[][] data_resolve_yd() {
        return new Object[][] {
                {2012, -365, date(2010, 12, 31), false, false},
                {2012, -364, date(2011, 1, 1), false, false},
                {2012, -31, date(2011, 11, 30), false, false},
                {2012, -30, date(2011, 12, 1), false, false},
                {2012, -12, date(2011, 12, 19), false, false},
                {2012, -1, date(2011, 12, 30), false, false},
                {2012, 0, date(2011, 12, 31), false, false},
                {2012, 1, date(2012, 1, 1), true, true},
                {2012, 2, date(2012, 1, 2), true, true},
                {2012, 31, date(2012, 1, 31), true, true},
                {2012, 32, date(2012, 2, 1), true, true},
                {2012, 59, date(2012, 2, 28), true, true},
                {2012, 60, date(2012, 2, 29), true, true},
                {2012, 61, date(2012, 3, 1), true, true},
                {2012, 365, date(2012, 12, 30), true, true},
                {2012, 366, date(2012, 12, 31), true, true},
                {2012, 367, date(2013, 1, 1), false, false},
                {2012, 367 + 364, date(2013, 12, 31), false, false},
                {2012, 367 + 365, date(2014, 1, 1), false, false},

                {2011, 59, date(2011, 2, 28), true, true},
                {2011, 60, date(2011, 3, 1), true, true},
        };
    }

    @Test(dataProvider = "resolve_yd")
    public void test_resolve_yd_lenient(int y, int d, JapaneseDate expected, boolean smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.DAY_OF_YEAR, (long) d);
        JapaneseDate date = JapaneseChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.LENIENT);
        assertEquals(date, expected);
        assertEquals(fieldValues.size(), 0);
    }

    @Test(dataProvider = "resolve_yd")
    public void test_resolve_yd_smart(int y, int d, JapaneseDate expected, boolean smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.DAY_OF_YEAR, (long) d);
        if (smart) {
            JapaneseDate date = JapaneseChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
            assertEquals(date, expected);
            assertEquals(fieldValues.size(), 0);
        } else {
            try {
                JapaneseChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
                fail("Should have failed");
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    @Test(dataProvider = "resolve_yd")
    public void test_resolve_yd_strict(int y, int d, JapaneseDate expected, boolean smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.DAY_OF_YEAR, (long) d);
        if (strict) {
            JapaneseDate date = JapaneseChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.STRICT);
            assertEquals(date, expected);
            assertEquals(fieldValues.size(), 0);
        } else {
            try {
                JapaneseChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.STRICT);
                fail("Should have failed");
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name = "resolve_eymd")
    Object[][] data_resolve_eymd() {
        return new Object[][] {
                // lenient
                {ResolverStyle.LENIENT, JapaneseEra.HEISEI, 1, 1, 1, date(1989, 1, 1)},  // SHOWA, not HEISEI
                {ResolverStyle.LENIENT, JapaneseEra.HEISEI, 1, 1, 7, date(1989, 1, 7)},  // SHOWA, not HEISEI
                {ResolverStyle.LENIENT, JapaneseEra.HEISEI, 1, 1, 8, date(1989, 1, 8)},
                {ResolverStyle.LENIENT, JapaneseEra.HEISEI, 1, 12, 31, date(1989, 12, 31)},
                {ResolverStyle.LENIENT, JapaneseEra.HEISEI, 2, 1, 1, date(1990, 1, 1)},

                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 1, 1, date(1989, 1, 1)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 1, 7, date(1989, 1, 7)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 1, 8, date(1989, 1, 8)},  // HEISEI, not SHOWA
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 12, 31, date(1989, 12, 31)},  // HEISEI, not SHOWA
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 65, 1, 1, date(1990, 1, 1)},  // HEISEI, not SHOWA

                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 1, -366, date(1987, 12, 31)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 1, -365, date(1988, 1, 1)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 1, -31, date(1988, 11, 30)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 1, -30, date(1988, 12, 1)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 1, 0, date(1988, 12, 31)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 1, 1, date(1989, 1, 1)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 1, 27, date(1989, 1, 27)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 1, 28, date(1989, 1, 28)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 1, 29, date(1989, 1, 29)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 1, 30, date(1989, 1, 30)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 1, 31, date(1989, 1, 31)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 1, 32, date(1989, 2, 1)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 1, 58, date(1989, 2, 27)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 1, 59, date(1989, 2, 28)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 1, 60, date(1989, 3, 1)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 1, 365, date(1989, 12, 31)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 1, 366, date(1990, 1, 1)},

                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 63, 1, 1, date(1988, 1, 1)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 63, 1, 31, date(1988, 1, 31)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 63, 1, 32, date(1988, 2, 1)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 63, 1, 58, date(1988, 2, 27)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 63, 1, 59, date(1988, 2, 28)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 63, 1, 60, date(1988, 2, 29)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 63, 1, 61, date(1988, 3, 1)},

                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 2, 1, date(1989, 2, 1)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 2, 28, date(1989, 2, 28)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 64, 2, 29, date(1989, 3, 1)},

                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 63, 2, 1, date(1988, 2, 1)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 63, 2, 28, date(1988, 2, 28)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 63, 2, 29, date(1988, 2, 29)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 63, 2, 30, date(1988, 3, 1)},

                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 62, -11, 1, date(1986, 1, 1)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 62, -1, 1, date(1986, 11, 1)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 62, 0, 1, date(1986, 12, 1)},
                {ResolverStyle.LENIENT, JapaneseEra.SHOWA, 62, 13, 1, date(1988, 1, 1)},

                // smart
                {ResolverStyle.SMART, JapaneseEra.HEISEI, 0, 1, 1, null},
                {ResolverStyle.SMART, JapaneseEra.HEISEI, 1, 1, 1, date(1989, 1, 1)},  // SHOWA, not HEISEI
                {ResolverStyle.SMART, JapaneseEra.HEISEI, 1, 1, 7, date(1989, 1, 7)},  // SHOWA, not HEISEI
                {ResolverStyle.SMART, JapaneseEra.HEISEI, 1, 1, 8, date(1989, 1, 8)},
                {ResolverStyle.SMART, JapaneseEra.HEISEI, 1, 12, 31, date(1989, 12, 31)},
                {ResolverStyle.SMART, JapaneseEra.HEISEI, 2, 1, 1, date(1990, 1, 1)},

                {ResolverStyle.SMART, JapaneseEra.SHOWA, 64, 1, 1, date(1989, 1, 1)},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 64, 1, 7, date(1989, 1, 7)},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 64, 1, 8, date(1989, 1, 8)},  // HEISEI, not SHOWA
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 64, 12, 31, date(1989, 12, 31)},  // HEISEI, not SHOWA
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 65, 1, 1, null},  // HEISEI, not SHOWA

                {ResolverStyle.SMART, JapaneseEra.SHOWA, 62, 1, 0, null},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 62, 1, 1, date(1987, 1, 1)},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 62, 1, 27, date(1987, 1, 27)},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 62, 1, 28, date(1987, 1, 28)},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 62, 1, 29, date(1987, 1, 29)},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 62, 1, 30, date(1987, 1, 30)},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 62, 1, 31, date(1987, 1, 31)},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 62, 1, 32, null},

                {ResolverStyle.SMART, JapaneseEra.SHOWA, 62, 2, 0, null},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 62, 2, 1, date(1987, 2, 1)},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 62, 2, 27, date(1987, 2, 27)},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 62, 2, 28, date(1987, 2, 28)},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 62, 2, 29, date(1987, 2, 28)},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 62, 2, 30, date(1987, 2, 28)},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 62, 2, 31, date(1987, 2, 28)},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 62, 2, 32, null},

                {ResolverStyle.SMART, JapaneseEra.SHOWA, 63, 2, 0, null},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 63, 2, 1, date(1988, 2, 1)},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 63, 2, 27, date(1988, 2, 27)},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 63, 2, 28, date(1988, 2, 28)},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 63, 2, 29, date(1988, 2, 29)},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 63, 2, 30, date(1988, 2, 29)},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 63, 2, 31, date(1988, 2, 29)},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 63, 2, 32, null},

                {ResolverStyle.SMART, JapaneseEra.SHOWA, 62, -12, 1, null},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 62, -1, 1, null},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 62, 0, 1, null},
                {ResolverStyle.SMART, JapaneseEra.SHOWA, 62, 13, 1, null},

                // strict
                {ResolverStyle.STRICT, JapaneseEra.HEISEI, 0, 1, 1, null},
                {ResolverStyle.STRICT, JapaneseEra.HEISEI, 1, 1, 1, null},  // SHOWA, not HEISEI
                {ResolverStyle.STRICT, JapaneseEra.HEISEI, 1, 1, 7, null},  // SHOWA, not HEISEI
                {ResolverStyle.STRICT, JapaneseEra.HEISEI, 1, 1, 8, date(1989, 1, 8)},
                {ResolverStyle.STRICT, JapaneseEra.HEISEI, 1, 12, 31, date(1989, 12, 31)},
                {ResolverStyle.STRICT, JapaneseEra.HEISEI, 2, 1, 1, date(1990, 1, 1)},

                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 64, 1, 1, date(1989, 1, 1)},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 64, 1, 7, date(1989, 1, 7)},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 64, 1, 8, null},  // HEISEI, not SHOWA
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 64, 12, 31, null},  // HEISEI, not SHOWA
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 65, 1, 1, null},  // HEISEI, not SHOWA

                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 62, 1, 0, null},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 62, 1, 1, date(1987, 1, 1)},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 62, 1, 27, date(1987, 1, 27)},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 62, 1, 28, date(1987, 1, 28)},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 62, 1, 29, date(1987, 1, 29)},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 62, 1, 30, date(1987, 1, 30)},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 62, 1, 31, date(1987, 1, 31)},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 62, 1, 32, null},

                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 62, 2, 0, null},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 62, 2, 1, date(1987, 2, 1)},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 62, 2, 27, date(1987, 2, 27)},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 62, 2, 28, date(1987, 2, 28)},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 62, 2, 29, null},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 62, 2, 30, null},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 62, 2, 31, null},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 62, 2, 32, null},

                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 63, 2, 0, null},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 63, 2, 1, date(1988, 2, 1)},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 63, 2, 27, date(1988, 2, 27)},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 63, 2, 28, date(1988, 2, 28)},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 63, 2, 29, date(1988, 2, 29)},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 63, 2, 30, null},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 63, 2, 31, null},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 63, 2, 32, null},

                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 62, -12, 1, null},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 62, -1, 1, null},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 62, 0, 1, null},
                {ResolverStyle.STRICT, JapaneseEra.SHOWA, 62, 13, 1, null},
        };
    }

    @Test(dataProvider = "resolve_eymd")
    public void test_resolve_eymd(ResolverStyle style, JapaneseEra era, int yoe, int m, int d, JapaneseDate expected) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.ERA, (long) era.getValue());
        fieldValues.put(ChronoField.YEAR_OF_ERA, (long) yoe);
        fieldValues.put(ChronoField.MONTH_OF_YEAR, (long) m);
        fieldValues.put(ChronoField.DAY_OF_MONTH, (long) d);
        if (expected != null) {
            JapaneseDate date = JapaneseChronology.INSTANCE.resolveDate(fieldValues, style);
            assertEquals(date, expected);
            assertEquals(fieldValues.size(), 0);
        } else {
            try {
                JapaneseChronology.INSTANCE.resolveDate(fieldValues, style);
                fail("Should have failed");
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    //-----------------------------------------------------------------------
    private static JapaneseDate date(int y, int m, int d) {
        return JapaneseDate.of(y, m, d);
    }

}
