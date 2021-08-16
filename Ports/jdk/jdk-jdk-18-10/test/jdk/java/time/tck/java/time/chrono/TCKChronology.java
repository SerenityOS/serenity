/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright (c) 2012, Stephen Colebourne & Michael Nascimento Santos
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.Era;
import java.time.chrono.HijrahChronology;
import java.time.chrono.HijrahEra;
import java.time.chrono.IsoChronology;
import java.time.chrono.IsoEra;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.JapaneseEra;
import java.time.chrono.MinguoChronology;
import java.time.chrono.MinguoEra;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.chrono.ThaiBuddhistEra;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.Set;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test Chronology class.
 */
@Test
public class TCKChronology {

    private static final ZoneOffset OFFSET_P0100 = ZoneOffset.ofHours(1);
    private static final ZoneOffset OFFSET_M0100 = ZoneOffset.ofHours(-1);

    private static final int YDIFF_MEIJI = 1867;
    private static final int YDIFF_SHOWA = 1925;
    private static final int YDIFF_HEISEI = 1988;
    private static final int YDIFF_MINGUO = 1911;
    private static final int YDIFF_THAIBUDDHIST = 543;
    //-----------------------------------------------------------------------
    // regular data factory for ID and calendarType of available calendars
    //-----------------------------------------------------------------------
    @DataProvider(name = "calendarNameAndType")
    Object[][] data_of_calendars() {
        return new Object[][] {
                    {"Hijrah-umalqura", "islamic-umalqura"},
                    {"ISO", "iso8601"},
                    {"Japanese", "japanese"},
                    {"Minguo", "roc"},
                    {"ThaiBuddhist", "buddhist"},
                };
    }

    @Test(dataProvider = "calendarNameAndType")
    public void test_getters(String chronoId, String calendarSystemType) {
        Chronology chrono = Chronology.of(chronoId);
        assertNotNull(chrono, "Required calendar not found by ID: " + chronoId);
        assertEquals(chrono.getId(), chronoId);
        assertEquals(chrono.getCalendarType(), calendarSystemType);
    }

    @Test(dataProvider = "calendarNameAndType")
    public void test_required_calendars(String chronoId, String calendarSystemType) {
        Chronology chrono = Chronology.of(chronoId);
        assertNotNull(chrono, "Required calendar not found by ID: " + chronoId);
        chrono = Chronology.of(calendarSystemType);
        assertNotNull(chrono, "Required calendar not found by type: " + chronoId);
        Set<Chronology> cals = Chronology.getAvailableChronologies();
        assertTrue(cals.contains(chrono), "Required calendar not found in set of available calendars");
    }

    @Test
    public void test_calendar_list() {
        Set<Chronology> chronos = Chronology.getAvailableChronologies();
        assertNotNull(chronos, "Required list of calendars must be non-null");
        for (Chronology chrono : chronos) {
            Chronology lookup = Chronology.of(chrono.getId());
            assertNotNull(lookup, "Required calendar not found: " + chrono);
        }
        assertEquals(chronos.size() >= data_of_calendars().length, true, "Chronology.getAvailableChronologies().size = " + chronos.size()
                + ", expected >= " + data_of_calendars().length);
    }

    //-----------------------------------------------------------------------
    // getDisplayName()
    //-----------------------------------------------------------------------
    @DataProvider(name = "calendarDisplayName")
    Object[][] data_of_calendarDisplayNames() {
        return new Object[][] {
                    {"Hijrah", "Islamic Calendar (Umm al-Qura)"},
                    {"ISO", "ISO"},
                    {"Japanese", "Japanese Calendar"},
                    {"Minguo", "Minguo Calendar"},
                    {"ThaiBuddhist", "Buddhist Calendar"},
                };
    }

    @Test(dataProvider = "calendarDisplayName")
    public void test_getDisplayName(String chronoId, String calendarDisplayName) {
        Chronology chrono = Chronology.of(chronoId);
        assertEquals(chrono.getDisplayName(TextStyle.FULL, Locale.ENGLISH), calendarDisplayName);
    }

    /**
     * Compute the number of days from the Epoch and compute the date from the number of days.
     */
    @Test(dataProvider = "calendarNameAndType")
    public void test_epoch(String name, String alias) {
        Chronology chrono = Chronology.of(name); // a chronology. In practice this is rarely hardcoded
        ChronoLocalDate date1 = chrono.dateNow();
        long epoch1 = date1.getLong(ChronoField.EPOCH_DAY);
        ChronoLocalDate date2 = date1.with(ChronoField.EPOCH_DAY, epoch1);
        assertEquals(date1, date2, "Date from epoch day is not same date: " + date1 + " != " + date2);
        long epoch2 = date1.getLong(ChronoField.EPOCH_DAY);
        assertEquals(epoch1, epoch2, "Epoch day not the same: " + epoch1 + " != " + epoch2);
    }

    @Test(dataProvider = "calendarNameAndType")
    public void test_dateEpochDay(String name, String alias) {
        Chronology chrono = Chronology.of(name);
        ChronoLocalDate date = chrono.dateNow();
        long epochDay = date.getLong(ChronoField.EPOCH_DAY);
        ChronoLocalDate test = chrono.dateEpochDay(epochDay);
        assertEquals(test, date);
    }

    //-----------------------------------------------------------------------
    // locale based lookup
    //-----------------------------------------------------------------------
    @DataProvider(name = "calendarsystemtype")
    Object[][] data_CalendarType() {
        return new Object[][] {
            {HijrahChronology.INSTANCE, "islamic-umalqura"},
            {IsoChronology.INSTANCE, "iso8601"},
            {JapaneseChronology.INSTANCE, "japanese"},
            {MinguoChronology.INSTANCE, "roc"},
            {ThaiBuddhistChronology.INSTANCE, "buddhist"},
        };
    }

    @Test(dataProvider = "calendarsystemtype")
    public void test_getCalendarType(Chronology chrono, String calendarType) {
        String type = calendarType;
        assertEquals(chrono.getCalendarType(), type);
    }

    @Test(dataProvider = "calendarsystemtype")
    public void test_lookupLocale(Chronology chrono, String calendarType) {
        Locale.Builder builder = new Locale.Builder().setLanguage("en").setRegion("CA");
        builder.setUnicodeLocaleKeyword("ca", calendarType);
        Locale locale = builder.build();
        assertEquals(Chronology.ofLocale(locale), chrono);
    }

    //-----------------------------------------------------------------------
    // dateNow()
    //-----------------------------------------------------------------------
    @Test
    public void test_MinguoChronology_dateNow() {
        ZoneId zoneId_paris = ZoneId.of("Europe/Paris");
        Clock clock = Clock.system(zoneId_paris);

        Chronology chrono = Chronology.of("Minguo");
        assertEquals(chrono.dateNow(), MinguoChronology.INSTANCE.dateNow());
        assertEquals(chrono.dateNow(zoneId_paris), MinguoChronology.INSTANCE.dateNow(zoneId_paris));
        assertEquals(chrono.dateNow(clock), MinguoChronology.INSTANCE.dateNow(clock));
    }

    @Test
    public void test_IsoChronology_dateNow() {
        ZoneId zoneId_paris = ZoneId.of("Europe/Paris");
        Clock clock = Clock.system(zoneId_paris);

        Chronology chrono = Chronology.of("ISO");
        assertEquals(chrono.dateNow(), IsoChronology.INSTANCE.dateNow());
        assertEquals(chrono.dateNow(zoneId_paris), IsoChronology.INSTANCE.dateNow(zoneId_paris));
        assertEquals(chrono.dateNow(clock), IsoChronology.INSTANCE.dateNow(clock));
    }

    @Test
    public void test_JapaneseChronology_dateNow() {
        ZoneId zoneId_paris = ZoneId.of("Europe/Paris");
        Clock clock = Clock.system(zoneId_paris);

        Chronology chrono = Chronology.of("Japanese");
        assertEquals(chrono.dateNow(), JapaneseChronology.INSTANCE.dateNow());
        assertEquals(chrono.dateNow(zoneId_paris), JapaneseChronology.INSTANCE.dateNow(zoneId_paris));
        assertEquals(chrono.dateNow(clock), JapaneseChronology.INSTANCE.dateNow(clock));
    }

    @Test
    public void test_ThaiBuddhistChronology_dateNow() {
        ZoneId zoneId_paris = ZoneId.of("Europe/Paris");
        Clock clock = Clock.system(zoneId_paris);

        Chronology chrono = Chronology.of("ThaiBuddhist");
        assertEquals(chrono.dateNow(), ThaiBuddhistChronology.INSTANCE.dateNow());
        assertEquals(chrono.dateNow(zoneId_paris), ThaiBuddhistChronology.INSTANCE.dateNow(zoneId_paris));
        assertEquals(chrono.dateNow(clock), ThaiBuddhistChronology.INSTANCE.dateNow(clock));
    }

    //-----------------------------------------------------------------------
    // dateYearDay() and date()
    //-----------------------------------------------------------------------
    @Test
    public void test_HijrahChronology_dateYearDay() {
        Chronology chrono = Chronology.of("Hijrah");
        ChronoLocalDate date1 = chrono.dateYearDay(HijrahEra.AH, 1434, 178);
        ChronoLocalDate date2 = chrono.date(HijrahEra.AH, 1434, 7, 1);
        assertEquals(date1, HijrahChronology.INSTANCE.dateYearDay(HijrahEra.AH, 1434, 178));
        assertEquals(date2, HijrahChronology.INSTANCE.dateYearDay(HijrahEra.AH, 1434, 178));
    }

    @Test
    public void test_MinguoChronology_dateYearDay() {
        Chronology chrono = Chronology.of("Minguo");
        ChronoLocalDate date1 = chrono.dateYearDay(MinguoEra.ROC, 5, 60);
        ChronoLocalDate date2 = chrono.date(MinguoEra.ROC, 5, 2, 29);
        assertEquals(date1, MinguoChronology.INSTANCE.dateYearDay(MinguoEra.ROC, 5, 60));
        assertEquals(date2, MinguoChronology.INSTANCE.dateYearDay(MinguoEra.ROC, 5, 60));
    }

    @Test
    public void test_IsoChronology_dateYearDay() {
        Chronology chrono = Chronology.of("ISO");
        ChronoLocalDate date1 = chrono.dateYearDay(IsoEra.CE, 5, 60);
        ChronoLocalDate date2 = chrono.date(IsoEra.CE, 5, 3, 1);
        assertEquals(date1, IsoChronology.INSTANCE.dateYearDay(IsoEra.CE, 5, 60));
        assertEquals(date2, IsoChronology.INSTANCE.dateYearDay(IsoEra.CE, 5, 60));
    }

    @Test
    public void test_JapaneseChronology_dateYearDay() {
        Chronology chrono = Chronology.of("Japanese");
        ChronoLocalDate date1 = chrono.dateYearDay(JapaneseEra.HEISEI, 8, 60);
        ChronoLocalDate date2 = chrono.date(JapaneseEra.HEISEI, 8, 2, 29);
        assertEquals(date1, JapaneseChronology.INSTANCE.dateYearDay(JapaneseEra.HEISEI, 8, 60));
        assertEquals(date2, JapaneseChronology.INSTANCE.dateYearDay(JapaneseEra.HEISEI, 8, 60));
    }

    @Test
    public void test_ThaiBuddhistChronology_dateYearDay() {
        Chronology chrono = Chronology.of("ThaiBuddhist");
        ChronoLocalDate date1 = chrono.dateYearDay(ThaiBuddhistEra.BE, 2459, 60);
        ChronoLocalDate date2 = chrono.date(ThaiBuddhistEra.BE, 2459, 2, 29);
        assertEquals(date1, ThaiBuddhistChronology.INSTANCE.dateYearDay(ThaiBuddhistEra.BE, 2459, 60));
        assertEquals(date2, ThaiBuddhistChronology.INSTANCE.dateYearDay(ThaiBuddhistEra.BE, 2459, 60));
    }

    /**
     * Test lookup by calendarType of each chronology.
     * Verify that the calendar can be found by {@link java.time.chrono.Chronology#ofLocale}.
     */
    @Test
    public void test_ofLocaleByType() {
        // Test that all available chronologies can be successfully found using ofLocale
        Set<Chronology> chronos = Chronology.getAvailableChronologies();
        for (Chronology chrono : chronos) {
            Locale.Builder builder = new Locale.Builder().setLanguage("en").setRegion("CA");
            builder.setUnicodeLocaleKeyword("ca", chrono.getCalendarType());
            Locale locale = builder.build();
            assertEquals(Chronology.ofLocale(locale), chrono, "Lookup by type");
        }
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void test_lookupLocale() {
        Locale.Builder builder = new Locale.Builder().setLanguage("en").setRegion("CA");
        builder.setUnicodeLocaleKeyword("ca", "xxx");

        Locale locale = builder.build();
        Chronology.ofLocale(locale);
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void test_noChrono() {
        Chronology chrono = Chronology.of("FooFoo");
    }

    @DataProvider(name = "epochSecond_dataProvider")
    Object[][]  data_epochSecond() {
        return new Object[][] {
                {JapaneseChronology.INSTANCE, 1873, 9, 7, 1, 2, 2, OFFSET_P0100},
                {JapaneseChronology.INSTANCE, 1928, 2, 28, 1, 2, 2, OFFSET_M0100},
                {JapaneseChronology.INSTANCE, 1989, 1, 8, 1, 2, 2, OFFSET_P0100},
                {HijrahChronology.INSTANCE, 1434, 9, 7, 1, 2, 2, OFFSET_P0100},
                {MinguoChronology.INSTANCE, 1873, 9, 7, 1, 2, 2, OFFSET_P0100},
                {MinguoChronology.INSTANCE, 1928, 2, 28, 1, 2, 2, OFFSET_M0100},
                {MinguoChronology.INSTANCE, 1989, 1, 8, 1, 2, 2, OFFSET_P0100},
                {ThaiBuddhistChronology.INSTANCE, 1873, 9, 7, 1, 2, 2, OFFSET_P0100},
                {ThaiBuddhistChronology.INSTANCE, 1928, 2, 28, 1, 2, 2, OFFSET_M0100},
                {ThaiBuddhistChronology.INSTANCE, 1989, 1, 8, 1, 2, 2, OFFSET_P0100},
                {IsoChronology.INSTANCE, 1873, 9, 7, 1, 2, 2, OFFSET_P0100},
                {IsoChronology.INSTANCE, 1928, 2, 28, 1, 2, 2, OFFSET_M0100},
                {IsoChronology.INSTANCE, 1989, 1, 8, 1, 2, 2, OFFSET_P0100},

        };
    }

    @Test(dataProvider = "epochSecond_dataProvider")
    public void test_epochSecond(Chronology chrono, int y, int m, int d, int h, int min, int s, ZoneOffset offset) {
        ChronoLocalDate chronoLd = chrono.date(y, m, d);
        assertEquals(chrono.epochSecond(y, m, d, h, min, s, offset),
                     OffsetDateTime.of(LocalDate.from(chronoLd), LocalTime.of(h, min, s), offset)
                                   .toEpochSecond());
    }

    @DataProvider(name = "era_epochSecond_dataProvider")
    Object[][]  data_era_epochSecond() {
        return new Object[][] {
                {JapaneseChronology.INSTANCE, JapaneseEra.MEIJI, 1873 - YDIFF_MEIJI, 9, 7, 1, 2, 2, OFFSET_P0100},
                {JapaneseChronology.INSTANCE, JapaneseEra.SHOWA, 1928 - YDIFF_SHOWA, 2, 28, 1, 2, 2, OFFSET_M0100},
                {JapaneseChronology.INSTANCE, JapaneseEra.HEISEI, 1989 - YDIFF_HEISEI, 1, 8, 1, 2, 2, OFFSET_P0100},
                {HijrahChronology.INSTANCE, HijrahEra.AH, 1434, 9, 7, 1, 2, 2, OFFSET_P0100},
                {MinguoChronology.INSTANCE, MinguoEra.BEFORE_ROC, 1873 - YDIFF_MINGUO, 9, 7, 1, 2, 2, OFFSET_P0100},
                {MinguoChronology.INSTANCE, MinguoEra.ROC, 1928 - YDIFF_MINGUO, 2, 28, 1, 2, 2, OFFSET_M0100},
                {MinguoChronology.INSTANCE, MinguoEra.ROC, 1989 - YDIFF_MINGUO, 1, 8, 1, 2, 2, OFFSET_P0100},
                {ThaiBuddhistChronology.INSTANCE, ThaiBuddhistEra.BE, 1873 + YDIFF_THAIBUDDHIST, 9, 7, 1, 2, 2, OFFSET_P0100},
                {ThaiBuddhistChronology.INSTANCE, ThaiBuddhistEra.BE, 1928 + YDIFF_THAIBUDDHIST, 2, 28, 1, 2, 2, OFFSET_M0100},
                {ThaiBuddhistChronology.INSTANCE, ThaiBuddhistEra.BE, 1989 + YDIFF_THAIBUDDHIST, 1, 8, 1, 2, 2, OFFSET_P0100},
                {IsoChronology.INSTANCE, IsoEra.CE, 1873, 9, 7, 1, 2, 2, OFFSET_P0100},
                {IsoChronology.INSTANCE, IsoEra.CE, 1928, 2, 28, 1, 2, 2, OFFSET_M0100},
                {IsoChronology.INSTANCE, IsoEra.CE, 1989, 1, 8, 1, 2, 2, OFFSET_P0100},

        };
    }

    @Test(dataProvider = "era_epochSecond_dataProvider")
    public void test_epochSecond(Chronology chrono, Era era, int y, int m, int d, int h, int min, int s, ZoneOffset offset) {
        ChronoLocalDate chronoLd = chrono.date(era, y, m, d);
        assertEquals(chrono.epochSecond(era, y, m, d, h, min, s, offset),
                     OffsetDateTime.of(LocalDate.from(chronoLd), LocalTime.of(h, min, s), offset)
                                   .toEpochSecond());
    }

    @DataProvider(name = "bad_epochSecond_dataProvider")
    Object[][]  bad_data_epochSecond() {
        return new Object[][] {
                {JapaneseChronology.INSTANCE, 1873, 13, 7, 1, 2, 2, OFFSET_P0100},
                {HijrahChronology.INSTANCE, 1434, 9, 32, 1, 2, 2, OFFSET_P0100},
                {MinguoChronology.INSTANCE, 1873, 9, 7, 31, 2, 2, OFFSET_P0100},
                {ThaiBuddhistChronology.INSTANCE, 1928, 2, 28, -1, 2, 2, OFFSET_M0100},
                {IsoChronology.INSTANCE, 1928, 2, 28, 1, 60, 2, OFFSET_M0100},
                {IsoChronology.INSTANCE, 1989, 1, 8, 1, 2, -2, OFFSET_P0100},

        };
    }

    @Test(dataProvider = "bad_epochSecond_dataProvider", expectedExceptions = DateTimeException.class)
    public void test_bad_epochSecond(Chronology chrono, int y, int m, int d, int h, int min, int s, ZoneOffset offset) {
        chrono.epochSecond(y, m, d, h, min, s, offset);
    }

}
