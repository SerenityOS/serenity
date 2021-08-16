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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoPeriod;
import java.time.chrono.ChronoZonedDateTime;
import java.time.chrono.Chronology;
import java.time.chrono.Era;
import java.time.chrono.IsoChronology;
import java.time.chrono.JapaneseDate;
import java.time.chrono.MinguoChronology;
import java.time.chrono.MinguoEra;
import java.time.chrono.MinguoDate;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.chrono.ThaiBuddhistDate;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjusters;
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
public class TCKMinguoChronology {

    private static final ZoneOffset OFFSET_PTWO = ZoneOffset.ofHours(2);
    private static final ZoneId ZONE_PARIS = ZoneId.of("Europe/Paris");
    private static final int YDIFF = 1911;
    //-----------------------------------------------------------------------
    // Chronology.ofName("Minguo")  Lookup by name
    //-----------------------------------------------------------------------
    @Test
    public void test_chrono_byName() {
        Chronology c = MinguoChronology.INSTANCE;
        Chronology test = Chronology.of("Minguo");
        Assert.assertNotNull(test, "The Minguo calendar could not be found byName");
        Assert.assertEquals(test.getId(), "Minguo", "ID mismatch");
        Assert.assertEquals(test.getCalendarType(), "roc", "Type mismatch");
        Assert.assertEquals(test, c);
    }

    //-----------------------------------------------------------------------
    // creation, toLocalDate()
    //-----------------------------------------------------------------------
    @DataProvider(name="samples")
    Object[][] data_samples() {
        return new Object[][] {
            {MinguoChronology.INSTANCE.date(1, 1, 1), LocalDate.of(1 + YDIFF, 1, 1)},
            {MinguoChronology.INSTANCE.date(1, 1, 2), LocalDate.of(1 + YDIFF, 1, 2)},
            {MinguoChronology.INSTANCE.date(1, 1, 3), LocalDate.of(1 + YDIFF, 1, 3)},

            {MinguoChronology.INSTANCE.date(2, 1, 1), LocalDate.of(2 + YDIFF, 1, 1)},
            {MinguoChronology.INSTANCE.date(3, 1, 1), LocalDate.of(3 + YDIFF, 1, 1)},
            {MinguoChronology.INSTANCE.date(3, 12, 6), LocalDate.of(3 + YDIFF, 12, 6)},
            {MinguoChronology.INSTANCE.date(4, 1, 1), LocalDate.of(4 + YDIFF, 1, 1)},
            {MinguoChronology.INSTANCE.date(4, 7, 3), LocalDate.of(4 + YDIFF, 7, 3)},
            {MinguoChronology.INSTANCE.date(4, 7, 4), LocalDate.of(4 + YDIFF, 7, 4)},
            {MinguoChronology.INSTANCE.date(5, 1, 1), LocalDate.of(5 + YDIFF, 1, 1)},
            {MinguoChronology.INSTANCE.date(100, 3, 3), LocalDate.of(100 + YDIFF, 3, 3)},
            {MinguoChronology.INSTANCE.date(101, 10, 28), LocalDate.of(101 + YDIFF, 10, 28)},
            {MinguoChronology.INSTANCE.date(101, 10, 29), LocalDate.of(101 + YDIFF, 10, 29)},

            {MinguoChronology.INSTANCE.dateYearDay(1916 - YDIFF, 60), LocalDate.of(1916, 2, 29)},
            {MinguoChronology.INSTANCE.dateYearDay(1908 - YDIFF, 60), LocalDate.of(1908, 2, 29)},
            {MinguoChronology.INSTANCE.dateYearDay(2000 - YDIFF, 60), LocalDate.of(2000, 2, 29)},
            {MinguoChronology.INSTANCE.dateYearDay(2400 - YDIFF, 60), LocalDate.of(2400, 2, 29)},

            {MinguoChronology.INSTANCE.dateYearDay(MinguoEra.ROC, 1916 - YDIFF, 60), LocalDate.of(1916, 2, 29)},
            {MinguoChronology.INSTANCE.dateYearDay(MinguoEra.BEFORE_ROC, 4, 60), LocalDate.of(1908, 2, 29)},
            {MinguoChronology.INSTANCE.dateYearDay(MinguoEra.ROC, 2000 - YDIFF, 60), LocalDate.of(2000, 2, 29)},
            {MinguoChronology.INSTANCE.dateYearDay(MinguoEra.ROC, 2400 - YDIFF, 60), LocalDate.of(2400, 2, 29)},

            {MinguoChronology.INSTANCE.date(MinguoEra.ROC, 1916 - YDIFF, 2, 29 ), LocalDate.of(1916, 2, 29)},
            {MinguoChronology.INSTANCE.date(MinguoEra.BEFORE_ROC, 4, 2, 29), LocalDate.of(1908, 2, 29)},
            {MinguoChronology.INSTANCE.date(MinguoEra.ROC, 2000 - YDIFF, 2, 29), LocalDate.of(2000, 2, 29)},
            {MinguoChronology.INSTANCE.date(MinguoEra.ROC, 2400 - YDIFF, 2, 29), LocalDate.of(2400, 2, 29)},
        };
    }

    @Test(dataProvider="samples")
    public void test_toLocalDate(MinguoDate minguo, LocalDate iso) {
        assertEquals(LocalDate.from(minguo), iso);
    }

    @Test(dataProvider="samples")
    public void test_fromCalendrical(MinguoDate minguo, LocalDate iso) {
        assertEquals(MinguoChronology.INSTANCE.date(iso), minguo);
        assertEquals(MinguoDate.from(iso), minguo);
    }

    @Test(dataProvider="samples")
    public void test_isEqual(MinguoDate minguo, LocalDate iso) {
        assertTrue(minguo.isEqual(iso));
    }

    @Test(dataProvider="samples")
    public void test_date_equals(MinguoDate minguo, LocalDate iso) {
        assertFalse(minguo.equals(iso));
        assertNotEquals(minguo.hashCode(), iso.hashCode());
    }

    @Test
    public void test_dateNow(){
        assertEquals(MinguoChronology.INSTANCE.dateNow(), MinguoDate.now()) ;
        assertEquals(MinguoChronology.INSTANCE.dateNow(), MinguoDate.now(ZoneId.systemDefault())) ;
        assertEquals(MinguoChronology.INSTANCE.dateNow(), MinguoDate.now(Clock.systemDefaultZone())) ;
        assertEquals(MinguoChronology.INSTANCE.dateNow(), MinguoDate.now(Clock.systemDefaultZone().getZone())) ;

        assertEquals(MinguoChronology.INSTANCE.dateNow(), MinguoChronology.INSTANCE.dateNow(ZoneId.systemDefault())) ;
        assertEquals(MinguoChronology.INSTANCE.dateNow(), MinguoChronology.INSTANCE.dateNow(Clock.systemDefaultZone())) ;
        assertEquals(MinguoChronology.INSTANCE.dateNow(), MinguoChronology.INSTANCE.dateNow(Clock.systemDefaultZone().getZone())) ;

        ZoneId zoneId = ZoneId.of("Europe/Paris");
        assertEquals(MinguoChronology.INSTANCE.dateNow(zoneId), MinguoChronology.INSTANCE.dateNow(Clock.system(zoneId))) ;
        assertEquals(MinguoChronology.INSTANCE.dateNow(zoneId), MinguoChronology.INSTANCE.dateNow(Clock.system(zoneId).getZone())) ;
        assertEquals(MinguoChronology.INSTANCE.dateNow(zoneId), MinguoDate.now(Clock.system(zoneId))) ;
        assertEquals(MinguoChronology.INSTANCE.dateNow(zoneId), MinguoDate.now(Clock.system(zoneId).getZone())) ;

        assertEquals(MinguoChronology.INSTANCE.dateNow(ZoneId.of(ZoneOffset.UTC.getId())), MinguoChronology.INSTANCE.dateNow(Clock.systemUTC())) ;
    }

    @SuppressWarnings("unused")
    @Test(dataProvider="samples")
    public void test_MinguoDate(MinguoDate minguoDate, LocalDate iso) {
        MinguoDate hd = minguoDate;
        ChronoLocalDateTime<MinguoDate> hdt = hd.atTime(LocalTime.NOON);
        ZoneOffset zo = ZoneOffset.ofHours(1);
        ChronoZonedDateTime<MinguoDate> hzdt = hdt.atZone(zo);
        hdt = hdt.plus(1, ChronoUnit.YEARS);
        hdt = hdt.plus(1, ChronoUnit.MONTHS);
        hdt = hdt.plus(1, ChronoUnit.DAYS);
        hdt = hdt.plus(1, ChronoUnit.HOURS);
        hdt = hdt.plus(1, ChronoUnit.MINUTES);
        hdt = hdt.plus(1, ChronoUnit.SECONDS);
        hdt = hdt.plus(1, ChronoUnit.NANOS);
        ChronoLocalDateTime<MinguoDate> a2 = hzdt.toLocalDateTime();
        MinguoDate a3 = a2.toLocalDate();
        MinguoDate a5 = hzdt.toLocalDate();
        //System.out.printf(" d: %s, dt: %s; odt: %s; zodt: %s; a4: %s%n", date, hdt, hodt, hzdt, a5);
    }

    @Test()
    public void test_MinguoChrono() {
        MinguoDate h1 = MinguoChronology.INSTANCE.date(MinguoEra.ROC, 1, 2, 3);
        MinguoDate h2 = h1;
        ChronoLocalDateTime<MinguoDate> h3 = h2.atTime(LocalTime.NOON);
        @SuppressWarnings("unused")
        ChronoZonedDateTime<MinguoDate> h4 = h3.atZone(ZoneOffset.UTC);
    }

    @DataProvider(name="badDates")
    Object[][] data_badDates() {
        return new Object[][] {
            {1912, 0, 0},

            {1912, -1, 1},
            {1912, 0, 1},
            {1912, 14, 1},
            {1912, 15, 1},

            {1912, 1, -1},
            {1912, 1, 0},
            {1912, 1, 32},
            {1912, 2, 29},
            {1912, 2, 30},

            {1912, 12, -1},
            {1912, 12, 0},
            {1912, 12, 32},

            {1907 - YDIFF, 2, 29},
            {100 - YDIFF, 2, 29},
            {2100 - YDIFF, 2, 29},
            {2101 - YDIFF, 2, 29},
            };
    }

    @Test(dataProvider="badDates", expectedExceptions=DateTimeException.class)
    public void test_badDates(int year, int month, int dom) {
        MinguoChronology.INSTANCE.date(year, month, dom);
    }

    //-----------------------------------------------------------------------
    // prolepticYear() and is LeapYear()
    //-----------------------------------------------------------------------
    @DataProvider(name="prolepticYear")
    Object[][] data_prolepticYear() {
        return new Object[][] {
            {1, MinguoEra.ROC, 1912 - YDIFF, 1912 - YDIFF, true},
            {1, MinguoEra.ROC, 1916 - YDIFF, 1916 - YDIFF, true},
            {1, MinguoEra.ROC, 1914 - YDIFF, 1914 - YDIFF, false},
            {1, MinguoEra.ROC, 2000 - YDIFF, 2000 - YDIFF, true},
            {1, MinguoEra.ROC, 2100 - YDIFF, 2100 - YDIFF, false},
            {1, MinguoEra.ROC, 0, 0, false},
            {1, MinguoEra.ROC, 1908 - YDIFF, 1908 - YDIFF, true},
            {1, MinguoEra.ROC, 1900 - YDIFF, 1900 - YDIFF, false},
            {1, MinguoEra.ROC, 1600 - YDIFF, 1600 - YDIFF, true},

            {0, MinguoEra.BEFORE_ROC, YDIFF - 1911, 1912 - YDIFF, true},
            {0, MinguoEra.BEFORE_ROC, YDIFF - 1915, 1916 - YDIFF, true},
            {0, MinguoEra.BEFORE_ROC, YDIFF - 1913, 1914 - YDIFF, false},
            {0, MinguoEra.BEFORE_ROC, YDIFF - 1999, 2000 - YDIFF, true},
            {0, MinguoEra.BEFORE_ROC, YDIFF - 2099, 2100 - YDIFF, false},
            {0, MinguoEra.BEFORE_ROC, 1, 0, false},
            {0, MinguoEra.BEFORE_ROC, YDIFF - 1907, 1908 - YDIFF, true},
            {0, MinguoEra.BEFORE_ROC, YDIFF - 1899, 1900 - YDIFF, false},
            {0, MinguoEra.BEFORE_ROC, YDIFF - 1599, 1600 - YDIFF, true},

        };
    }

    @Test(dataProvider="prolepticYear")
    public void test_prolepticYear(int eraValue, Era  era, int yearOfEra, int expectedProlepticYear, boolean isLeapYear) {
        Era eraObj = MinguoChronology.INSTANCE.eraOf(eraValue);
        assertTrue(MinguoChronology.INSTANCE.eras().contains(eraObj));
        assertEquals(eraObj, era);
        assertEquals(MinguoChronology.INSTANCE.prolepticYear(era, yearOfEra), expectedProlepticYear);
    }

    @Test(dataProvider="prolepticYear")
    public void test_isLeapYear(int eraValue, Era  era, int yearOfEra, int expectedProlepticYear, boolean isLeapYear) {
        assertEquals(MinguoChronology.INSTANCE.isLeapYear(expectedProlepticYear), isLeapYear);
        assertEquals(MinguoChronology.INSTANCE.isLeapYear(expectedProlepticYear), Year.of(expectedProlepticYear + YDIFF).isLeap());

        MinguoDate minguo = MinguoDate.now();
        minguo = minguo.with(ChronoField.YEAR, expectedProlepticYear).with(ChronoField.MONTH_OF_YEAR, 2);
        if (isLeapYear) {
            assertEquals(minguo.lengthOfMonth(), 29);
        } else {
            assertEquals(minguo.lengthOfMonth(), 28);
        }
    }

    //-----------------------------------------------------------------------
    // Bad Era for Chronology.date(era,...) and Chronology.prolepticYear(Era,...)
    //-----------------------------------------------------------------------
    @Test
    public void test_InvalidEras() {
        // Verify that the eras from every other Chronology are invalid
        for (Chronology chrono : Chronology.getAvailableChronologies()) {
            if (chrono instanceof MinguoChronology) {
                continue;
            }
            List<Era> eras = chrono.eras();
            for (Era era : eras) {
                try {
                    ChronoLocalDate date = MinguoChronology.INSTANCE.date(era, 1, 1, 1);
                    fail("MinguoChronology.date did not throw ClassCastException for Era: " + era);
                } catch (ClassCastException cex) {
                    ; // ignore expected exception
                }

                /*  Test for missing MinguoDate.of(Era, y, m, d) method.
                try {
                    @SuppressWarnings("unused")
                    MinguoDate jdate = MinguoDate.of(era, 1, 1, 1);
                    fail("MinguoDate.of did not throw ClassCastException for Era: " + era);
                } catch (ClassCastException cex) {
                    ; // ignore expected exception
                }
                */

                try {
                    @SuppressWarnings("unused")
                    int year = MinguoChronology.INSTANCE.prolepticYear(era, 1);
                    fail("MinguoChronology.prolepticYear did not throw ClassCastException for Era: " + era);
                } catch (ClassCastException cex) {
                    ; // ignore expected exception
                }
            }
        }
    }

    //-----------------------------------------------------------------------
    // with(DateTimeAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_adjust1() {
        MinguoDate base = MinguoChronology.INSTANCE.date(2012, 10, 29);
        MinguoDate test = base.with(TemporalAdjusters.lastDayOfMonth());
        assertEquals(test, MinguoChronology.INSTANCE.date(2012, 10, 31));
    }

    @Test
    public void test_adjust2() {
        MinguoDate base = MinguoChronology.INSTANCE.date(1728, 12, 2);
        MinguoDate test = base.with(TemporalAdjusters.lastDayOfMonth());
        assertEquals(test, MinguoChronology.INSTANCE.date(1728, 12, 31));
    }

    //-----------------------------------------------------------------------
    // MinguoDate.with(Local*)
    //-----------------------------------------------------------------------
    @Test
    public void test_adjust_toLocalDate() {
        MinguoDate minguo = MinguoChronology.INSTANCE.date(99, 1, 4);
        MinguoDate test = minguo.with(LocalDate.of(2012, 7, 6));
        assertEquals(test, MinguoChronology.INSTANCE.date(101, 7, 6));
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_adjust_toMonth() {
        MinguoDate minguo = MinguoChronology.INSTANCE.date(1726, 1, 4);
        minguo.with(Month.APRIL);
    }

    //-----------------------------------------------------------------------
    // LocalDate.with(MinguoDate)
    //-----------------------------------------------------------------------
    @Test
    public void test_LocalDate_adjustToMinguoDate() {
        MinguoDate minguo = MinguoChronology.INSTANCE.date(101, 10, 29);
        LocalDate test = LocalDate.MIN.with(minguo);
        assertEquals(test, LocalDate.of(2012, 10, 29));
    }

    @Test
    public void test_LocalDateTime_adjustToMinguoDate() {
        MinguoDate minguo = MinguoChronology.INSTANCE.date(101, 10, 29);
        LocalDateTime test = LocalDateTime.MIN.with(minguo);
        assertEquals(test, LocalDateTime.of(2012, 10, 29, 0, 0));
    }

    //-----------------------------------------------------------------------
    // localDateTime()
    //-----------------------------------------------------------------------
    @DataProvider(name="localDateTime")
    Object[][] data_localDateTime() {
        return new Object[][] {
            {LocalDateTime.of(2012, 2, 29, 2, 7), MinguoChronology.INSTANCE.date(MinguoEra.ROC, 2012 - YDIFF, 2, 29), LocalTime.of(2, 7), null},
            {ZonedDateTime.of(2012, 2, 29, 2, 7, 1, 1, ZONE_PARIS), MinguoChronology.INSTANCE.date(MinguoEra.ROC, 2012 - YDIFF, 2, 29), LocalTime.of(2, 7, 1, 1), null},
            {OffsetDateTime.of(2012, 2, 29, 2, 7, 1, 1, OFFSET_PTWO), MinguoChronology.INSTANCE.date(MinguoEra.ROC, 2012 - YDIFF, 2, 29), LocalTime.of(2, 7, 1, 1), null},

            {JapaneseDate.of(2012, 2, 29), null, null, DateTimeException.class},
            {ThaiBuddhistDate.of(2012 + 543, 2, 29), null, null, DateTimeException.class},
            {LocalDate.of(2012, 2, 29), null, null, DateTimeException.class},
            {LocalTime.of(20, 30, 29, 0), null, null, DateTimeException.class},
        };
    }

    @Test(dataProvider="localDateTime")
    public void test_localDateTime(TemporalAccessor accessor,  MinguoDate expectedDate, LocalTime expectedTime, Class<?> expectedEx) {
        if (expectedEx == null) {
            ChronoLocalDateTime<MinguoDate> result = MinguoChronology.INSTANCE.localDateTime(accessor);
            assertEquals(result.toLocalDate(), expectedDate);
            assertEquals(MinguoDate.from(accessor), expectedDate);
            assertEquals(result.toLocalTime(), expectedTime);
        } else {
            try {
                ChronoLocalDateTime<MinguoDate> result = MinguoChronology.INSTANCE.localDateTime(accessor);
                fail();
            } catch (Exception ex) {
                assertTrue(expectedEx.isInstance(ex));
            }
        }
    }

    //-----------------------------------------------------------------------
    // zonedDateTime(TemporalAccessor)
    //-----------------------------------------------------------------------
    @DataProvider(name="zonedDateTime")
    Object[][] data_zonedDateTime() {
        return new Object[][] {
            {ZonedDateTime.of(2012, 2, 29, 2, 7, 1, 1, ZONE_PARIS), MinguoChronology.INSTANCE.date(MinguoEra.ROC, 2012 - YDIFF, 2, 29), LocalTime.of(2, 7, 1, 1), null},
            {OffsetDateTime.of(2012, 2, 29, 2, 7, 1, 1, OFFSET_PTWO), MinguoChronology.INSTANCE.date(MinguoEra.ROC, 2012 - YDIFF, 2, 29), LocalTime.of(2, 7, 1, 1), null},

            {LocalDateTime.of(2012, 2, 29, 2, 7), null, null, DateTimeException.class},
            {JapaneseDate.of(2012, 2, 29), null, null, DateTimeException.class},
            {ThaiBuddhistDate.of(2012 + 543, 2, 29), null, null, DateTimeException.class},
            {LocalDate.of(2012, 2, 29), null, null, DateTimeException.class},
            {LocalTime.of(20, 30, 29, 0), null, null, DateTimeException.class},
        };
    }

    @Test(dataProvider="zonedDateTime")
    public void test_zonedDateTime(TemporalAccessor accessor,  MinguoDate expectedDate, LocalTime expectedTime, Class<?> expectedEx) {
        if (expectedEx == null) {
            ChronoZonedDateTime<MinguoDate> result = MinguoChronology.INSTANCE.zonedDateTime(accessor);
            assertEquals(result.toLocalDate(), expectedDate);
            assertEquals(MinguoDate.from(accessor), expectedDate);
            assertEquals(result.toLocalTime(), expectedTime);

        } else {
            try {
                ChronoZonedDateTime<MinguoDate> result = MinguoChronology.INSTANCE.zonedDateTime(accessor);
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
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2012, 2, 29, 2, 7, 1, 1, ZONE_PARIS);

        ChronoZonedDateTime<MinguoDate> result = MinguoChronology.INSTANCE.zonedDateTime(offsetDateTime.toInstant(), offsetDateTime.getOffset());
        assertEquals(result.toLocalDate(), MinguoChronology.INSTANCE.date(MinguoEra.ROC, 2012 - YDIFF, 2, 29));
        assertEquals(result.toLocalTime(), LocalTime.of(2, 7, 1, 1));

        result = MinguoChronology.INSTANCE.zonedDateTime(zonedDateTime.toInstant(), zonedDateTime.getOffset());
        assertEquals(result.toLocalDate(), MinguoChronology.INSTANCE.date(MinguoEra.ROC, 2012 - YDIFF, 2, 29));
        assertEquals(result.toLocalTime(), LocalTime.of(2, 7, 1, 1));
    }

    //-----------------------------------------------------------------------
    // PeriodUntil()
    //-----------------------------------------------------------------------
    @Test
    public void test_periodUntilDate() {
        MinguoDate mdate1 = MinguoDate.of(1970, 1, 1);
        MinguoDate mdate2 = MinguoDate.of(1971, 2, 2);
        ChronoPeriod period = mdate1.until(mdate2);
        assertEquals(period, MinguoChronology.INSTANCE.period(1, 1, 1));
    }

    @Test
    public void test_periodUntilUnit() {
        MinguoDate mdate1 = MinguoDate.of(1970, 1, 1);
        MinguoDate mdate2 = MinguoDate.of(1971, 2, 2);
        long months = mdate1.until(mdate2, ChronoUnit.MONTHS);
        assertEquals(months, 13);
    }

    @Test
    public void test_periodUntilDiffChrono() {
        MinguoDate mdate1 = MinguoDate.of(1970, 1, 1);
        MinguoDate mdate2 = MinguoDate.of(1971, 2, 2);
        ThaiBuddhistDate ldate2 = ThaiBuddhistChronology.INSTANCE.date(mdate2);
        ChronoPeriod period = mdate1.until(ldate2);
        assertEquals(period, MinguoChronology.INSTANCE.period(1, 1, 1));
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @DataProvider(name="toString")
    Object[][] data_toString() {
        return new Object[][] {
            {MinguoChronology.INSTANCE.date(1, 1, 1), "Minguo ROC 1-01-01"},
            {MinguoChronology.INSTANCE.date(1728, 10, 28), "Minguo ROC 1728-10-28"},
            {MinguoChronology.INSTANCE.date(1728, 10, 29), "Minguo ROC 1728-10-29"},
            {MinguoChronology.INSTANCE.date(1727, 12, 5), "Minguo ROC 1727-12-05"},
            {MinguoChronology.INSTANCE.date(1727, 12, 6), "Minguo ROC 1727-12-06"},
        };
    }

    @Test(dataProvider="toString")
    public void test_toString(MinguoDate minguo, String expected) {
        assertEquals(minguo.toString(), expected);
    }

    //-----------------------------------------------------------------------
    // equals()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals_true() {
        assertTrue(MinguoChronology.INSTANCE.equals(MinguoChronology.INSTANCE));
    }

    @Test
    public void test_equals_false() {
        assertFalse(MinguoChronology.INSTANCE.equals(IsoChronology.INSTANCE));
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name = "resolve_yearOfEra")
    Object[][] data_resolve_yearOfEra() {
        return new Object[][] {
                // era only
                {ResolverStyle.STRICT, -1, null, null, null, null},
                {ResolverStyle.SMART, -1, null, null, null, null},
                {ResolverStyle.LENIENT, -1, null, null, null, null},

                {ResolverStyle.STRICT, 0, null, null, ChronoField.ERA, 0},
                {ResolverStyle.SMART, 0, null, null, ChronoField.ERA, 0},
                {ResolverStyle.LENIENT, 0, null, null, ChronoField.ERA, 0},

                {ResolverStyle.STRICT, 1, null, null, ChronoField.ERA, 1},
                {ResolverStyle.SMART, 1, null, null, ChronoField.ERA, 1},
                {ResolverStyle.LENIENT, 1, null, null, ChronoField.ERA, 1},

                {ResolverStyle.STRICT, 2, null, null, null, null},
                {ResolverStyle.SMART, 2, null, null, null, null},
                {ResolverStyle.LENIENT, 2, null, null, null, null},

                // era and year-of-era
                {ResolverStyle.STRICT, -1, 2012, null, null, null},
                {ResolverStyle.SMART, -1, 2012, null, null, null},
                {ResolverStyle.LENIENT, -1, 2012, null, null, null},

                {ResolverStyle.STRICT, 0, 2012, null, ChronoField.YEAR, -2011},
                {ResolverStyle.SMART, 0, 2012, null, ChronoField.YEAR, -2011},
                {ResolverStyle.LENIENT, 0, 2012, null, ChronoField.YEAR, -2011},

                {ResolverStyle.STRICT, 1, 2012, null, ChronoField.YEAR, 2012},
                {ResolverStyle.SMART, 1, 2012, null, ChronoField.YEAR, 2012},
                {ResolverStyle.LENIENT, 1, 2012, null, ChronoField.YEAR, 2012},

                {ResolverStyle.STRICT, 2, 2012, null, null, null},
                {ResolverStyle.SMART, 2, 2012, null, null, null},
                {ResolverStyle.LENIENT, 2, 2012, null, null, null},

                // year-of-era only
                {ResolverStyle.STRICT, null, 2012, null, ChronoField.YEAR_OF_ERA, 2012},
                {ResolverStyle.SMART, null, 2012, null, ChronoField.YEAR, 2012},
                {ResolverStyle.LENIENT, null, 2012, null, ChronoField.YEAR, 2012},

                {ResolverStyle.STRICT, null, Integer.MAX_VALUE, null, null, null},
                {ResolverStyle.SMART, null, Integer.MAX_VALUE, null, null, null},
                {ResolverStyle.LENIENT, null, Integer.MAX_VALUE, null, ChronoField.YEAR, Integer.MAX_VALUE},

                // year-of-era and year
                {ResolverStyle.STRICT, null, 2012, 2012, ChronoField.YEAR, 2012},
                {ResolverStyle.SMART, null, 2012, 2012, ChronoField.YEAR, 2012},
                {ResolverStyle.LENIENT, null, 2012, 2012, ChronoField.YEAR, 2012},

                {ResolverStyle.STRICT, null, 2012, -2011, ChronoField.YEAR, -2011},
                {ResolverStyle.SMART, null, 2012, -2011, ChronoField.YEAR, -2011},
                {ResolverStyle.LENIENT, null, 2012, -2011, ChronoField.YEAR, -2011},

                {ResolverStyle.STRICT, null, 2012, 2013, null, null},
                {ResolverStyle.SMART, null, 2012, 2013, null, null},
                {ResolverStyle.LENIENT, null, 2012, 2013, null, null},

                {ResolverStyle.STRICT, null, 2012, -2013, null, null},
                {ResolverStyle.SMART, null, 2012, -2013, null, null},
                {ResolverStyle.LENIENT, null, 2012, -2013, null, null},
        };
    }

    @Test(dataProvider = "resolve_yearOfEra")
    public void test_resolve_yearOfEra(ResolverStyle style, Integer e, Integer yoe, Integer y, ChronoField field, Integer expected) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        if (e != null) {
            fieldValues.put(ChronoField.ERA, (long) e);
        }
        if (yoe != null) {
            fieldValues.put(ChronoField.YEAR_OF_ERA, (long) yoe);
        }
        if (y != null) {
            fieldValues.put(ChronoField.YEAR, (long) y);
        }
        if (field != null) {
            MinguoDate date = MinguoChronology.INSTANCE.resolveDate(fieldValues, style);
            assertEquals(date, null);
            assertEquals(fieldValues.get(field), (Long) expected.longValue());
            assertEquals(fieldValues.size(), 1);
        } else {
            try {
                MinguoChronology.INSTANCE.resolveDate(fieldValues, style);
                fail("Should have failed");
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
                {2012 - YDIFF, 1, -365, date(2010 - YDIFF, 12, 31), false, false},
                {2012 - YDIFF, 1, -364, date(2011 - YDIFF, 1, 1), false, false},
                {2012 - YDIFF, 1, -31, date(2011 - YDIFF, 11, 30), false, false},
                {2012 - YDIFF, 1, -30, date(2011 - YDIFF, 12, 1), false, false},
                {2012 - YDIFF, 1, -12, date(2011 - YDIFF, 12, 19), false, false},
                {2012 - YDIFF, 1, 1, date(2012 - YDIFF, 1, 1), true, true},
                {2012 - YDIFF, 1, 27, date(2012 - YDIFF, 1, 27), true, true},
                {2012 - YDIFF, 1, 28, date(2012 - YDIFF, 1, 28), true, true},
                {2012 - YDIFF, 1, 29, date(2012 - YDIFF, 1, 29), true, true},
                {2012 - YDIFF, 1, 30, date(2012 - YDIFF, 1, 30), true, true},
                {2012 - YDIFF, 1, 31, date(2012 - YDIFF, 1, 31), true, true},
                {2012 - YDIFF, 1, 59, date(2012 - YDIFF, 2, 28), false, false},
                {2012 - YDIFF, 1, 60, date(2012 - YDIFF, 2, 29), false, false},
                {2012 - YDIFF, 1, 61, date(2012 - YDIFF, 3, 1), false, false},
                {2012 - YDIFF, 1, 365, date(2012 - YDIFF, 12, 30), false, false},
                {2012 - YDIFF, 1, 366, date(2012 - YDIFF, 12, 31), false, false},
                {2012 - YDIFF, 1, 367, date(2013 - YDIFF, 1, 1), false, false},
                {2012 - YDIFF, 1, 367 + 364, date(2013 - YDIFF, 12, 31), false, false},
                {2012 - YDIFF, 1, 367 + 365, date(2014 - YDIFF, 1, 1), false, false},

                {2012 - YDIFF, 2, 1, date(2012 - YDIFF, 2, 1), true, true},
                {2012 - YDIFF, 2, 28, date(2012 - YDIFF, 2, 28), true, true},
                {2012 - YDIFF, 2, 29, date(2012 - YDIFF, 2, 29), true, true},
                {2012 - YDIFF, 2, 30, date(2012 - YDIFF, 3, 1), date(2012 - YDIFF, 2, 29), false},
                {2012 - YDIFF, 2, 31, date(2012 - YDIFF, 3, 2), date(2012 - YDIFF, 2, 29), false},
                {2012 - YDIFF, 2, 32, date(2012 - YDIFF, 3, 3), false, false},

                {2012 - YDIFF, -12, 1, date(2010 - YDIFF, 12, 1), false, false},
                {2012 - YDIFF, -11, 1, date(2011 - YDIFF, 1, 1), false, false},
                {2012 - YDIFF, -1, 1, date(2011 - YDIFF, 11, 1), false, false},
                {2012 - YDIFF, 0, 1, date(2011 - YDIFF, 12, 1), false, false},
                {2012 - YDIFF, 1, 1, date(2012 - YDIFF, 1, 1), true, true},
                {2012 - YDIFF, 12, 1, date(2012 - YDIFF, 12, 1), true, true},
                {2012 - YDIFF, 13, 1, date(2013 - YDIFF, 1, 1), false, false},
                {2012 - YDIFF, 24, 1, date(2013 - YDIFF, 12, 1), false, false},
                {2012 - YDIFF, 25, 1, date(2014 - YDIFF, 1, 1), false, false},

                {2012 - YDIFF, 6, -31, date(2012 - YDIFF, 4, 30), false, false},
                {2012 - YDIFF, 6, -30, date(2012 - YDIFF, 5, 1), false, false},
                {2012 - YDIFF, 6, -1, date(2012 - YDIFF, 5, 30), false, false},
                {2012 - YDIFF, 6, 0, date(2012 - YDIFF, 5, 31), false, false},
                {2012 - YDIFF, 6, 1, date(2012 - YDIFF, 6, 1), true, true},
                {2012 - YDIFF, 6, 30, date(2012 - YDIFF, 6, 30), true, true},
                {2012 - YDIFF, 6, 31, date(2012 - YDIFF, 7, 1), date(2012 - YDIFF, 6, 30), false},
                {2012 - YDIFF, 6, 61, date(2012 - YDIFF, 7, 31), false, false},
                {2012 - YDIFF, 6, 62, date(2012 - YDIFF, 8, 1), false, false},

                {2011 - YDIFF, 2, 1, date(2011 - YDIFF, 2, 1), true, true},
                {2011 - YDIFF, 2, 28, date(2011 - YDIFF, 2, 28), true, true},
                {2011 - YDIFF, 2, 29, date(2011 - YDIFF, 3, 1), date(2011 - YDIFF, 2, 28), false},
                {2011 - YDIFF, 2, 30, date(2011 - YDIFF, 3, 2), date(2011 - YDIFF, 2, 28), false},
                {2011 - YDIFF, 2, 31, date(2011 - YDIFF, 3, 3), date(2011 - YDIFF, 2, 28), false},
                {2011 - YDIFF, 2, 32, date(2011 - YDIFF, 3, 4), false, false},
        };
    }

    @Test(dataProvider = "resolve_ymd")
    public void test_resolve_ymd_lenient(int y, int m, int d, MinguoDate expected, Object smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.MONTH_OF_YEAR, (long) m);
        fieldValues.put(ChronoField.DAY_OF_MONTH, (long) d);
        MinguoDate date = MinguoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.LENIENT);
        assertEquals(date, expected);
        assertEquals(fieldValues.size(), 0);
    }

    @Test(dataProvider = "resolve_ymd")
    public void test_resolve_ymd_smart(int y, int m, int d, MinguoDate expected, Object smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.MONTH_OF_YEAR, (long) m);
        fieldValues.put(ChronoField.DAY_OF_MONTH, (long) d);
        if (Boolean.TRUE.equals(smart)) {
            MinguoDate date = MinguoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
            assertEquals(date, expected);
            assertEquals(fieldValues.size(), 0);
        } else if (smart instanceof MinguoDate) {
            MinguoDate date = MinguoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
            assertEquals(date, smart);
        } else {
            try {
                MinguoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
                fail("Should have failed");
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    @Test(dataProvider = "resolve_ymd")
    public void test_resolve_ymd_strict(int y, int m, int d, MinguoDate expected, Object smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.MONTH_OF_YEAR, (long) m);
        fieldValues.put(ChronoField.DAY_OF_MONTH, (long) d);
        if (strict) {
            MinguoDate date = MinguoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.STRICT);
            assertEquals(date, expected);
            assertEquals(fieldValues.size(), 0);
        } else {
            try {
                MinguoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.STRICT);
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
                {2012 - YDIFF, -365, date(2010 - YDIFF, 12, 31), false, false},
                {2012 - YDIFF, -364, date(2011 - YDIFF, 1, 1), false, false},
                {2012 - YDIFF, -31, date(2011 - YDIFF, 11, 30), false, false},
                {2012 - YDIFF, -30, date(2011 - YDIFF, 12, 1), false, false},
                {2012 - YDIFF, -12, date(2011 - YDIFF, 12, 19), false, false},
                {2012 - YDIFF, -1, date(2011 - YDIFF, 12, 30), false, false},
                {2012 - YDIFF, 0, date(2011 - YDIFF, 12, 31), false, false},
                {2012 - YDIFF, 1, date(2012 - YDIFF, 1, 1), true, true},
                {2012 - YDIFF, 2, date(2012 - YDIFF, 1, 2), true, true},
                {2012 - YDIFF, 31, date(2012 - YDIFF, 1, 31), true, true},
                {2012 - YDIFF, 32, date(2012 - YDIFF, 2, 1), true, true},
                {2012 - YDIFF, 59, date(2012 - YDIFF, 2, 28), true, true},
                {2012 - YDIFF, 60, date(2012 - YDIFF, 2, 29), true, true},
                {2012 - YDIFF, 61, date(2012 - YDIFF, 3, 1), true, true},
                {2012 - YDIFF, 365, date(2012 - YDIFF, 12, 30), true, true},
                {2012 - YDIFF, 366, date(2012 - YDIFF, 12, 31), true, true},
                {2012 - YDIFF, 367, date(2013 - YDIFF, 1, 1), false, false},
                {2012 - YDIFF, 367 + 364, date(2013 - YDIFF, 12, 31), false, false},
                {2012 - YDIFF, 367 + 365, date(2014 - YDIFF, 1, 1), false, false},

                {2011 - YDIFF, 59, date(2011 - YDIFF, 2, 28), true, true},
                {2011 - YDIFF, 60, date(2011 - YDIFF, 3, 1), true, true},
        };
    }

    @Test(dataProvider = "resolve_yd")
    public void test_resolve_yd_lenient(int y, int d, MinguoDate expected, boolean smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.DAY_OF_YEAR, (long) d);
        MinguoDate date = MinguoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.LENIENT);
        assertEquals(date, expected);
        assertEquals(fieldValues.size(), 0);
    }

    @Test(dataProvider = "resolve_yd")
    public void test_resolve_yd_smart(int y, int d, MinguoDate expected, boolean smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.DAY_OF_YEAR, (long) d);
        if (smart) {
            MinguoDate date = MinguoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
            assertEquals(date, expected);
            assertEquals(fieldValues.size(), 0);
        } else {
            try {
                MinguoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
                fail("Should have failed");
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    @Test(dataProvider = "resolve_yd")
    public void test_resolve_yd_strict(int y, int d, MinguoDate expected, boolean smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.DAY_OF_YEAR, (long) d);
        if (strict) {
            MinguoDate date = MinguoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.STRICT);
            assertEquals(date, expected);
            assertEquals(fieldValues.size(), 0);
        } else {
            try {
                MinguoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.STRICT);
                fail("Should have failed");
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name = "resolve_ymaa")
    Object[][] data_resolve_ymaa() {
        return new Object[][] {
                {2012 - YDIFF, 1, 1, -365, date(2010 - YDIFF, 12, 31), false, false},
                {2012 - YDIFF, 1, 1, -364, date(2011 - YDIFF, 1, 1), false, false},
                {2012 - YDIFF, 1, 1, -31, date(2011 - YDIFF, 11, 30), false, false},
                {2012 - YDIFF, 1, 1, -30, date(2011 - YDIFF, 12, 1), false, false},
                {2012 - YDIFF, 1, 1, -12, date(2011 - YDIFF, 12, 19), false, false},
                {2012 - YDIFF, 1, 1, 1, date(2012 - YDIFF, 1, 1), true, true},
                {2012 - YDIFF, 1, 1, 59, date(2012 - YDIFF, 2, 28), false, false},
                {2012 - YDIFF, 1, 1, 60, date(2012 - YDIFF, 2, 29), false, false},
                {2012 - YDIFF, 1, 1, 61, date(2012 - YDIFF, 3, 1), false, false},
                {2012 - YDIFF, 1, 1, 365, date(2012 - YDIFF, 12, 30), false, false},
                {2012 - YDIFF, 1, 1, 366, date(2012 - YDIFF, 12, 31), false, false},
                {2012 - YDIFF, 1, 1, 367, date(2013 - YDIFF, 1, 1), false, false},
                {2012 - YDIFF, 1, 1, 367 + 364, date(2013 - YDIFF, 12, 31), false, false},
                {2012 - YDIFF, 1, 1, 367 + 365, date(2014 - YDIFF, 1, 1), false, false},

                {2012 - YDIFF, 2, 0, 1, date(2012 - YDIFF, 1, 25), false, false},
                {2012 - YDIFF, 2, 0, 7, date(2012 - YDIFF, 1, 31), false, false},
                {2012 - YDIFF, 2, 1, 1, date(2012 - YDIFF, 2, 1), true, true},
                {2012 - YDIFF, 2, 1, 7, date(2012 - YDIFF, 2, 7), true, true},
                {2012 - YDIFF, 2, 2, 1, date(2012 - YDIFF, 2, 8), true, true},
                {2012 - YDIFF, 2, 2, 7, date(2012 - YDIFF, 2, 14), true, true},
                {2012 - YDIFF, 2, 3, 1, date(2012 - YDIFF, 2, 15), true, true},
                {2012 - YDIFF, 2, 3, 7, date(2012 - YDIFF, 2, 21), true, true},
                {2012 - YDIFF, 2, 4, 1, date(2012 - YDIFF, 2, 22), true, true},
                {2012 - YDIFF, 2, 4, 7, date(2012 - YDIFF, 2, 28), true, true},
                {2012 - YDIFF, 2, 5, 1, date(2012 - YDIFF, 2, 29), true, true},
                {2012 - YDIFF, 2, 5, 2, date(2012 - YDIFF, 3, 1), true, false},
                {2012 - YDIFF, 2, 5, 7, date(2012 - YDIFF, 3, 6), true, false},
                {2012 - YDIFF, 2, 6, 1, date(2012 - YDIFF, 3, 7), false, false},
                {2012 - YDIFF, 2, 6, 7, date(2012 - YDIFF, 3, 13), false, false},

                {2012 - YDIFF, 12, 1, 1, date(2012 - YDIFF, 12, 1), true, true},
                {2012 - YDIFF, 12, 5, 1, date(2012 - YDIFF, 12, 29), true, true},
                {2012 - YDIFF, 12, 5, 2, date(2012 - YDIFF, 12, 30), true, true},
                {2012 - YDIFF, 12, 5, 3, date(2012 - YDIFF, 12, 31), true, true},
                {2012 - YDIFF, 12, 5, 4, date(2013 - YDIFF, 1, 1), true, false},
                {2012 - YDIFF, 12, 5, 7, date(2013 - YDIFF, 1, 4), true, false},

                {2012 - YDIFF, -12, 1, 1, date(2010 - YDIFF, 12, 1), false, false},
                {2012 - YDIFF, -11, 1, 1, date(2011 - YDIFF, 1, 1), false, false},
                {2012 - YDIFF, -1, 1, 1, date(2011 - YDIFF, 11, 1), false, false},
                {2012 - YDIFF, 0, 1, 1, date(2011 - YDIFF, 12, 1), false, false},
                {2012 - YDIFF, 1, 1, 1, date(2012 - YDIFF, 1, 1), true, true},
                {2012 - YDIFF, 12, 1, 1, date(2012 - YDIFF, 12, 1), true, true},
                {2012 - YDIFF, 13, 1, 1, date(2013 - YDIFF, 1, 1), false, false},
                {2012 - YDIFF, 24, 1, 1, date(2013 - YDIFF, 12, 1), false, false},
                {2012 - YDIFF, 25, 1, 1, date(2014 - YDIFF, 1, 1), false, false},

                {2011 - YDIFF, 2, 1, 1, date(2011 - YDIFF, 2, 1), true, true},
                {2011 - YDIFF, 2, 4, 7, date(2011 - YDIFF, 2, 28), true, true},
                {2011 - YDIFF, 2, 5, 1, date(2011 - YDIFF, 3, 1), true, false},
        };
    }

    @Test(dataProvider = "resolve_ymaa")
    public void test_resolve_ymaa_lenient(int y, int m, int w, int d, MinguoDate expected, boolean smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.MONTH_OF_YEAR, (long) m);
        fieldValues.put(ChronoField.ALIGNED_WEEK_OF_MONTH, (long) w);
        fieldValues.put(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH, (long) d);
        MinguoDate date = MinguoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.LENIENT);
        assertEquals(date, expected);
        assertEquals(fieldValues.size(), 0);
    }

    @Test(dataProvider = "resolve_ymaa")
    public void test_resolve_ymaa_smart(int y, int m, int w, int d, MinguoDate expected, boolean smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.MONTH_OF_YEAR, (long) m);
        fieldValues.put(ChronoField.ALIGNED_WEEK_OF_MONTH, (long) w);
        fieldValues.put(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH, (long) d);
        if (smart) {
            MinguoDate date = MinguoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
            assertEquals(date, expected);
            assertEquals(fieldValues.size(), 0);
        } else {
            try {
                MinguoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.SMART);
                fail("Should have failed");
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    @Test(dataProvider = "resolve_ymaa")
    public void test_resolve_ymaa_strict(int y, int m, int w, int d, MinguoDate expected, boolean smart, boolean strict) {
        Map<TemporalField, Long> fieldValues = new HashMap<>();
        fieldValues.put(ChronoField.YEAR, (long) y);
        fieldValues.put(ChronoField.MONTH_OF_YEAR, (long) m);
        fieldValues.put(ChronoField.ALIGNED_WEEK_OF_MONTH, (long) w);
        fieldValues.put(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH, (long) d);
        if (strict) {
            MinguoDate date = MinguoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.STRICT);
            assertEquals(date, expected);
            assertEquals(fieldValues.size(), 0);
        } else {
            try {
                MinguoChronology.INSTANCE.resolveDate(fieldValues, ResolverStyle.STRICT);
                fail("Should have failed");
            } catch (DateTimeException ex) {
                // expected
            }
        }
    }

    //-----------------------------------------------------------------------
    private static MinguoDate date(int y, int m, int d) {
        return MinguoDate.of(y, m, d);
    }

}
