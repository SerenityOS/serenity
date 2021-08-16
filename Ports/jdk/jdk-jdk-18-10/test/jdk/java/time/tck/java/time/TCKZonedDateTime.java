/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static java.time.Month.JANUARY;
import static java.time.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static java.time.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_MONTH;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR;
import static java.time.temporal.ChronoField.AMPM_OF_DAY;
import static java.time.temporal.ChronoField.CLOCK_HOUR_OF_AMPM;
import static java.time.temporal.ChronoField.CLOCK_HOUR_OF_DAY;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.time.temporal.ChronoField.EPOCH_DAY;
import static java.time.temporal.ChronoField.ERA;
import static java.time.temporal.ChronoField.HOUR_OF_AMPM;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.INSTANT_SECONDS;
import static java.time.temporal.ChronoField.MICRO_OF_DAY;
import static java.time.temporal.ChronoField.MICRO_OF_SECOND;
import static java.time.temporal.ChronoField.MILLI_OF_DAY;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.NANO_OF_DAY;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.OFFSET_SECONDS;
import static java.time.temporal.ChronoField.PROLEPTIC_MONTH;
import static java.time.temporal.ChronoField.SECOND_OF_DAY;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;
import static java.time.temporal.ChronoField.YEAR_OF_ERA;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.JulianFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test ZonedDateTime.
 */
@Test
public class TCKZonedDateTime extends AbstractDateTimeTest {

    private static final ZoneOffset OFFSET_0100 = ZoneOffset.ofHours(1);
    private static final ZoneOffset OFFSET_0200 = ZoneOffset.ofHours(2);
    private static final ZoneOffset OFFSET_0130 = ZoneOffset.of("+01:30");
    private static final ZoneOffset OFFSET_MAX = ZoneOffset.MAX;
    private static final ZoneOffset OFFSET_MIN = ZoneOffset.MIN;

    private static final ZoneId ZONE_0100 = OFFSET_0100;
    private static final ZoneId ZONE_0200 = OFFSET_0200;
    private static final ZoneId ZONE_M0100 = ZoneOffset.ofHours(-1);
    private static final ZoneId ZONE_LONDON = ZoneId.of("Europe/London");
    private static final ZoneId ZONE_PARIS = ZoneId.of("Europe/Paris");
    private LocalDateTime TEST_PARIS_GAP_2008_03_30_02_30;
    private LocalDateTime TEST_PARIS_OVERLAP_2008_10_26_02_30;
    private LocalDateTime TEST_LOCAL_2008_06_30_11_30_59_500;
    private ZonedDateTime TEST_DATE_TIME;
    private ZonedDateTime TEST_DATE_TIME_PARIS;

    @BeforeMethod
    public void setUp() {
        TEST_LOCAL_2008_06_30_11_30_59_500 = LocalDateTime.of(2008, 6, 30, 11, 30, 59, 500);
        TEST_DATE_TIME = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        TEST_DATE_TIME_PARIS = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_PARIS);
        TEST_PARIS_OVERLAP_2008_10_26_02_30 = LocalDateTime.of(2008, 10, 26, 2, 30);
        TEST_PARIS_GAP_2008_03_30_02_30 = LocalDateTime.of(2008, 3, 30, 2, 30);
    }

    //-----------------------------------------------------------------------
    @Override
    protected List<TemporalAccessor> samples() {
        TemporalAccessor[] array = {TEST_DATE_TIME, };
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> validFields() {
        TemporalField[] array = {
            NANO_OF_SECOND,
            NANO_OF_DAY,
            MICRO_OF_SECOND,
            MICRO_OF_DAY,
            MILLI_OF_SECOND,
            MILLI_OF_DAY,
            SECOND_OF_MINUTE,
            SECOND_OF_DAY,
            MINUTE_OF_HOUR,
            MINUTE_OF_DAY,
            CLOCK_HOUR_OF_AMPM,
            HOUR_OF_AMPM,
            CLOCK_HOUR_OF_DAY,
            HOUR_OF_DAY,
            AMPM_OF_DAY,
            DAY_OF_WEEK,
            ALIGNED_DAY_OF_WEEK_IN_MONTH,
            ALIGNED_DAY_OF_WEEK_IN_YEAR,
            DAY_OF_MONTH,
            DAY_OF_YEAR,
            EPOCH_DAY,
            ALIGNED_WEEK_OF_MONTH,
            ALIGNED_WEEK_OF_YEAR,
            MONTH_OF_YEAR,
            PROLEPTIC_MONTH,
            YEAR_OF_ERA,
            YEAR,
            ERA,
            OFFSET_SECONDS,
            INSTANT_SECONDS,
            JulianFields.JULIAN_DAY,
            JulianFields.MODIFIED_JULIAN_DAY,
            JulianFields.RATA_DIE,
        };
        return Arrays.asList(array);
    }

    @Override
    protected List<TemporalField> invalidFields() {
        List<TemporalField> list = new ArrayList<>(Arrays.<TemporalField>asList(ChronoField.values()));
        list.removeAll(validFields());
        return list;
    }

    //-----------------------------------------------------------------------
    // now()
    //-----------------------------------------------------------------------
    @Test
    public void now() {
        ZonedDateTime expected = ZonedDateTime.now(Clock.systemDefaultZone());
        ZonedDateTime test = ZonedDateTime.now();
        long diff = Math.abs(test.toLocalTime().toNanoOfDay() - expected.toLocalTime().toNanoOfDay());
        if (diff >= 100000000) {
            // may be date change
            expected = ZonedDateTime.now(Clock.systemDefaultZone());
            test = ZonedDateTime.now();
            diff = Math.abs(test.toLocalTime().toNanoOfDay() - expected.toLocalTime().toNanoOfDay());
        }
        assertTrue(diff < 100000000);  // less than 0.1 secs
    }

    //-----------------------------------------------------------------------
    // now(ZoneId)
    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void now_ZoneId_nullZoneId() {
        ZonedDateTime.now((ZoneId) null);
    }

    @Test
    public void now_ZoneId() {
        ZoneId zone = ZoneId.of("UTC+01:02:03");
        ZonedDateTime expected = ZonedDateTime.now(Clock.system(zone));
        ZonedDateTime test = ZonedDateTime.now(zone);
        assertEquals(Duration.between(expected, test).truncatedTo(ChronoUnit.SECONDS),
                Duration.ZERO);
    }

    //-----------------------------------------------------------------------
    // now(Clock)
    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void now_Clock_nullClock() {
        ZonedDateTime.now((Clock) null);
    }

    @Test
    public void now_Clock_allSecsInDay_utc() {
        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            Instant instant = Instant.ofEpochSecond(i).plusNanos(123456789L);
            Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
            ZonedDateTime test = ZonedDateTime.now(clock);
            assertEquals(test.getYear(), 1970);
            assertEquals(test.getMonth(), Month.JANUARY);
            assertEquals(test.getDayOfMonth(), (i < 24 * 60 * 60 ? 1 : 2));
            assertEquals(test.getHour(), (i / (60 * 60)) % 24);
            assertEquals(test.getMinute(), (i / 60) % 60);
            assertEquals(test.getSecond(), i % 60);
            assertEquals(test.getNano(), 123456789);
            assertEquals(test.getOffset(), ZoneOffset.UTC);
            assertEquals(test.getZone(), ZoneOffset.UTC);
        }
    }

    @Test
    public void now_Clock_allSecsInDay_zone() {
        ZoneId zone = ZoneId.of("Europe/London");
        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            Instant instant = Instant.ofEpochSecond(i).plusNanos(123456789L);
            ZonedDateTime expected = ZonedDateTime.ofInstant(instant, zone);
            Clock clock = Clock.fixed(expected.toInstant(), zone);
            ZonedDateTime test = ZonedDateTime.now(clock);
            assertEquals(test, expected);
        }
    }

    @Test
    public void now_Clock_allSecsInDay_beforeEpoch() {
        LocalTime expected = LocalTime.MIDNIGHT.plusNanos(123456789L);
        for (int i =-1; i >= -(24 * 60 * 60); i--) {
            Instant instant = Instant.ofEpochSecond(i).plusNanos(123456789L);
            Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
            ZonedDateTime test = ZonedDateTime.now(clock);
            assertEquals(test.getYear(), 1969);
            assertEquals(test.getMonth(), Month.DECEMBER);
            assertEquals(test.getDayOfMonth(), 31);
            expected = expected.minusSeconds(1);
            assertEquals(test.toLocalTime(), expected);
            assertEquals(test.getOffset(), ZoneOffset.UTC);
            assertEquals(test.getZone(), ZoneOffset.UTC);
        }
    }

    @Test
    public void now_Clock_offsets() {
        ZonedDateTime base = ZonedDateTime.of(LocalDateTime.of(1970, 1, 1, 12, 0), ZoneOffset.UTC);
        for (int i = -9; i < 15; i++) {
            ZoneOffset offset = ZoneOffset.ofHours(i);
            Clock clock = Clock.fixed(base.toInstant(), offset);
            ZonedDateTime test = ZonedDateTime.now(clock);
            assertEquals(test.getHour(), (12 + i) % 24);
            assertEquals(test.getMinute(), 0);
            assertEquals(test.getSecond(), 0);
            assertEquals(test.getNano(), 0);
            assertEquals(test.getOffset(), offset);
            assertEquals(test.getZone(), offset);
        }
    }

    //-----------------------------------------------------------------------
    // dateTime factories
    //-----------------------------------------------------------------------
    void check(ZonedDateTime test, int y, int m, int d, int h, int min, int s, int n, ZoneOffset offset, ZoneId zone) {
        assertEquals(test.getYear(), y);
        assertEquals(test.getMonth().getValue(), m);
        assertEquals(test.getDayOfMonth(), d);
        assertEquals(test.getHour(), h);
        assertEquals(test.getMinute(), min);
        assertEquals(test.getSecond(), s);
        assertEquals(test.getNano(), n);
        assertEquals(test.getOffset(), offset);
        assertEquals(test.getZone(), zone);
    }

    //-----------------------------------------------------------------------
    // of(LocalDate, LocalTime, ZoneId)
    //-----------------------------------------------------------------------
    @Test
    public void factory_of_LocalDateLocalTime() {
        ZonedDateTime test = ZonedDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 10, 500), ZONE_PARIS);
        check(test, 2008, 6, 30, 11, 30, 10, 500, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void factory_of_LocalDateLocalTime_inGap() {
        ZonedDateTime test = ZonedDateTime.of(TEST_PARIS_GAP_2008_03_30_02_30.toLocalDate(), TEST_PARIS_GAP_2008_03_30_02_30.toLocalTime(), ZONE_PARIS);
        check(test, 2008, 3, 30, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS);  // one hour later in summer offset
    }

    @Test
    public void factory_of_LocalDateLocalTime_inOverlap() {
        ZonedDateTime test = ZonedDateTime.of(TEST_PARIS_OVERLAP_2008_10_26_02_30.toLocalDate(), TEST_PARIS_OVERLAP_2008_10_26_02_30.toLocalTime(), ZONE_PARIS);
        check(test, 2008, 10, 26, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS);  // same time in summer offset
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_of_LocalDateLocalTime_nullDate() {
        ZonedDateTime.of((LocalDate) null, LocalTime.of(11, 30, 10, 500), ZONE_PARIS);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_of_LocalDateLocalTime_nullTime() {
        ZonedDateTime.of(LocalDate.of(2008, 6, 30), (LocalTime) null, ZONE_PARIS);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_of_LocalDateLocalTime_nullZone() {
        ZonedDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 10, 500), null);
    }

    //-----------------------------------------------------------------------
    // of(LocalDateTime, ZoneId)
    //-----------------------------------------------------------------------
    @Test
    public void factory_of_LocalDateTime() {
        LocalDateTime base = LocalDateTime.of(2008, 6, 30, 11, 30, 10, 500);
        ZonedDateTime test = ZonedDateTime.of(base, ZONE_PARIS);
        check(test, 2008, 6, 30, 11, 30, 10, 500, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void factory_of_LocalDateTime_inGap() {
        ZonedDateTime test = ZonedDateTime.of(TEST_PARIS_GAP_2008_03_30_02_30, ZONE_PARIS);
        check(test, 2008, 3, 30, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS);  // one hour later in summer offset
    }

    @Test
    public void factory_of_LocalDateTime_inOverlap() {
        ZonedDateTime test = ZonedDateTime.of(TEST_PARIS_OVERLAP_2008_10_26_02_30, ZONE_PARIS);
        check(test, 2008, 10, 26, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS);  // same time in summer offset
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_of_LocalDateTime_nullDateTime() {
        ZonedDateTime.of((LocalDateTime) null, ZONE_PARIS);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_of_LocalDateTime_nullZone() {
        LocalDateTime base = LocalDateTime.of(2008, 6, 30, 11, 30, 10, 500);
        ZonedDateTime.of(base, null);
    }

    //-----------------------------------------------------------------------
    // of(int..., ZoneId)
    //-----------------------------------------------------------------------
    @Test
    public void factory_of_ints() {
        ZonedDateTime test = ZonedDateTime.of(2008, 6, 30, 11, 30, 10, 500, ZONE_PARIS);
        check(test, 2008, 6, 30, 11, 30, 10, 500, OFFSET_0200, ZONE_PARIS);
    }

    //-----------------------------------------------------------------------
    // ofInstant(Instant, ZoneId)
    //-----------------------------------------------------------------------
    @Test
    public void factory_ofInstant_Instant_ZR() {
        Instant instant = LocalDateTime.of(2008, 6, 30, 11, 30, 10, 35).toInstant(OFFSET_0200);
        ZonedDateTime test = ZonedDateTime.ofInstant(instant, ZONE_PARIS);
        check(test, 2008, 6, 30, 11, 30, 10, 35, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void factory_ofInstant_Instant_ZO() {
        Instant instant = LocalDateTime.of(2008, 6, 30, 11, 30, 10, 45).toInstant(OFFSET_0200);
        ZonedDateTime test = ZonedDateTime.ofInstant(instant, OFFSET_0200);
        check(test, 2008, 6, 30, 11, 30, 10, 45, OFFSET_0200, OFFSET_0200);
    }

    @Test
    public void factory_ofInstant_Instant_inGap() {
        Instant instant = TEST_PARIS_GAP_2008_03_30_02_30.toInstant(OFFSET_0100);
        ZonedDateTime test = ZonedDateTime.ofInstant(instant, ZONE_PARIS);
        check(test, 2008, 3, 30, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS);  // one hour later in summer offset
    }

    @Test
    public void factory_ofInstant_Instant_inOverlap_earlier() {
        Instant instant = TEST_PARIS_OVERLAP_2008_10_26_02_30.toInstant(OFFSET_0200);
        ZonedDateTime test = ZonedDateTime.ofInstant(instant, ZONE_PARIS);
        check(test, 2008, 10, 26, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS);  // same time and offset
    }

    @Test
    public void factory_ofInstant_Instant_inOverlap_later() {
        Instant instant = TEST_PARIS_OVERLAP_2008_10_26_02_30.toInstant(OFFSET_0100);
        ZonedDateTime test = ZonedDateTime.ofInstant(instant, ZONE_PARIS);
        check(test, 2008, 10, 26, 2, 30, 0, 0, OFFSET_0100, ZONE_PARIS);  // same time and offset
    }

    @Test
    public void factory_ofInstant_Instant_invalidOffset() {
        Instant instant = LocalDateTime.of(2008, 6, 30, 11, 30, 10, 500).toInstant(OFFSET_0130);
        ZonedDateTime test = ZonedDateTime.ofInstant(instant, ZONE_PARIS);
        check(test, 2008, 6, 30, 12, 0, 10, 500, OFFSET_0200, ZONE_PARIS);  // corrected offset, thus altered time
    }

    @Test
    public void factory_ofInstant_allSecsInDay() {
        for (int i = 0; i < (24 * 60 * 60); i++) {
            Instant instant = Instant.ofEpochSecond(i);
            ZonedDateTime test = ZonedDateTime.ofInstant(instant, OFFSET_0100);
            assertEquals(test.getYear(), 1970);
            assertEquals(test.getMonth(), Month.JANUARY);
            assertEquals(test.getDayOfMonth(), 1 + (i >= 23 * 60 * 60 ? 1 : 0));
            assertEquals(test.getHour(), ((i / (60 * 60)) + 1) % 24);
            assertEquals(test.getMinute(), (i / 60) % 60);
            assertEquals(test.getSecond(), i % 60);
        }
    }

    @Test
    public void factory_ofInstant_allDaysInCycle() {
        // sanity check using different algorithm
        ZonedDateTime expected = LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0).atZone(ZoneOffset.UTC);
        for (long i = 0; i < 146097; i++) {
            Instant instant = Instant.ofEpochSecond(i * 24L * 60L * 60L);
            ZonedDateTime test = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
            assertEquals(test, expected);
            expected = expected.plusDays(1);
        }
    }

    @Test
    public void factory_ofInstant_minWithMinOffset() {
        long days_0000_to_1970 = (146097 * 5) - (30 * 365 + 7);
        int year = Year.MIN_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days_0000_to_1970;
        Instant instant = Instant.ofEpochSecond(days * 24L * 60L * 60L - OFFSET_MIN.getTotalSeconds());
        ZonedDateTime test = ZonedDateTime.ofInstant(instant, OFFSET_MIN);
        assertEquals(test.getYear(), Year.MIN_VALUE);
        assertEquals(test.getMonth().getValue(), 1);
        assertEquals(test.getDayOfMonth(), 1);
        assertEquals(test.getOffset(), OFFSET_MIN);
        assertEquals(test.getHour(), 0);
        assertEquals(test.getMinute(), 0);
        assertEquals(test.getSecond(), 0);
        assertEquals(test.getNano(), 0);
    }

    @Test
    public void factory_ofInstant_minWithMaxOffset() {
        long days_0000_to_1970 = (146097 * 5) - (30 * 365 + 7);
        int year = Year.MIN_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days_0000_to_1970;
        Instant instant = Instant.ofEpochSecond(days * 24L * 60L * 60L - OFFSET_MAX.getTotalSeconds());
        ZonedDateTime test = ZonedDateTime.ofInstant(instant, OFFSET_MAX);
        assertEquals(test.getYear(), Year.MIN_VALUE);
        assertEquals(test.getMonth().getValue(), 1);
        assertEquals(test.getDayOfMonth(), 1);
        assertEquals(test.getOffset(), OFFSET_MAX);
        assertEquals(test.getHour(), 0);
        assertEquals(test.getMinute(), 0);
        assertEquals(test.getSecond(), 0);
        assertEquals(test.getNano(), 0);
    }

    @Test
    public void factory_ofInstant_maxWithMinOffset() {
        long days_0000_to_1970 = (146097 * 5) - (30 * 365 + 7);
        int year = Year.MAX_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) + 365 - days_0000_to_1970;
        Instant instant = Instant.ofEpochSecond((days + 1) * 24L * 60L * 60L - 1 - OFFSET_MIN.getTotalSeconds());
        ZonedDateTime test = ZonedDateTime.ofInstant(instant, OFFSET_MIN);
        assertEquals(test.getYear(), Year.MAX_VALUE);
        assertEquals(test.getMonth().getValue(), 12);
        assertEquals(test.getDayOfMonth(), 31);
        assertEquals(test.getOffset(), OFFSET_MIN);
        assertEquals(test.getHour(), 23);
        assertEquals(test.getMinute(), 59);
        assertEquals(test.getSecond(), 59);
        assertEquals(test.getNano(), 0);
    }

    @Test
    public void factory_ofInstant_maxWithMaxOffset() {
        long days_0000_to_1970 = (146097 * 5) - (30 * 365 + 7);
        int year = Year.MAX_VALUE;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) + 365 - days_0000_to_1970;
        Instant instant = Instant.ofEpochSecond((days + 1) * 24L * 60L * 60L - 1 - OFFSET_MAX.getTotalSeconds());
        ZonedDateTime test = ZonedDateTime.ofInstant(instant, OFFSET_MAX);
        assertEquals(test.getYear(), Year.MAX_VALUE);
        assertEquals(test.getMonth().getValue(), 12);
        assertEquals(test.getDayOfMonth(), 31);
        assertEquals(test.getOffset(), OFFSET_MAX);
        assertEquals(test.getHour(), 23);
        assertEquals(test.getMinute(), 59);
        assertEquals(test.getSecond(), 59);
        assertEquals(test.getNano(), 0);
    }

    //-----------------------------------------------------------------------
    @Test(expectedExceptions=DateTimeException.class)
    public void factory_ofInstant_maxInstantWithMaxOffset() {
        Instant instant = Instant.ofEpochSecond(Long.MAX_VALUE);
        ZonedDateTime.ofInstant(instant, OFFSET_MAX);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_ofInstant_maxInstantWithMinOffset() {
        Instant instant = Instant.ofEpochSecond(Long.MAX_VALUE);
        ZonedDateTime.ofInstant(instant, OFFSET_MIN);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_ofInstant_tooBig() {
        long days_0000_to_1970 = (146097 * 5) - (30 * 365 + 7);
        long year = Year.MAX_VALUE + 1L;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days_0000_to_1970;
        Instant instant = Instant.ofEpochSecond(days * 24L * 60L * 60L);
        ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_ofInstant_tooLow() {
        long days_0000_to_1970 = (146097 * 5) - (30 * 365 + 7);
        int year = Year.MIN_VALUE - 1;
        long days = (year * 365L + (year / 4 - year / 100 + year / 400)) - days_0000_to_1970;
        Instant instant = Instant.ofEpochSecond(days * 24L * 60L * 60L);
        ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_ofInstant_Instant_nullInstant() {
        ZonedDateTime.ofInstant((Instant) null, ZONE_0100);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_ofInstant_Instant_nullZone() {
        ZonedDateTime.ofInstant(Instant.EPOCH, null);
    }

    //-----------------------------------------------------------------------
    // ofStrict(LocalDateTime, ZoneId, ZoneOffset)
    //-----------------------------------------------------------------------
    @Test
    public void factory_ofStrict_LDT_ZI_ZO() {
        LocalDateTime normal = LocalDateTime.of(2008, 6, 30, 11, 30, 10, 500);
        ZonedDateTime test = ZonedDateTime.ofStrict(normal, OFFSET_0200, ZONE_PARIS);
        check(test, 2008, 6, 30, 11, 30, 10, 500, OFFSET_0200, ZONE_PARIS);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_ofStrict_LDT_ZI_ZO_inGap() {
        try {
            ZonedDateTime.ofStrict(TEST_PARIS_GAP_2008_03_30_02_30, OFFSET_0100, ZONE_PARIS);
        } catch (DateTimeException ex) {
            assertEquals(ex.getMessage().contains(" gap"), true);
            throw ex;
        }
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_ofStrict_LDT_ZI_ZO_inOverlap_invalidOfset() {
        try {
            ZonedDateTime.ofStrict(TEST_PARIS_OVERLAP_2008_10_26_02_30, OFFSET_0130, ZONE_PARIS);
        } catch (DateTimeException ex) {
            assertEquals(ex.getMessage().contains(" is not valid for "), true);
            throw ex;
        }
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_ofStrict_LDT_ZI_ZO_invalidOffset() {
        try {
            ZonedDateTime.ofStrict(TEST_LOCAL_2008_06_30_11_30_59_500, OFFSET_0130, ZONE_PARIS);
        } catch (DateTimeException ex) {
            assertEquals(ex.getMessage().contains(" is not valid for "), true);
            throw ex;
        }
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_ofStrict_LDT_ZI_ZO_nullLDT() {
        ZonedDateTime.ofStrict((LocalDateTime) null, OFFSET_0100, ZONE_PARIS);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_ofStrict_LDT_ZI_ZO_nullZO() {
        ZonedDateTime.ofStrict(TEST_LOCAL_2008_06_30_11_30_59_500, null, ZONE_PARIS);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_ofStrict_LDT_ZI_ZO_nullZI() {
        ZonedDateTime.ofStrict(TEST_LOCAL_2008_06_30_11_30_59_500, OFFSET_0100, null);
    }

    //-----------------------------------------------------------------------
    // from(TemporalAccessor)
    //-----------------------------------------------------------------------
    @Test
    public void factory_from_TemporalAccessor_ZDT() {
        assertEquals(ZonedDateTime.from(TEST_DATE_TIME_PARIS), TEST_DATE_TIME_PARIS);
    }

    @Test
    public void factory_from_TemporalAccessor_LDT_ZoneId() {
        assertEquals(ZonedDateTime.from(new TemporalAccessor() {
            @Override
            public boolean isSupported(TemporalField field) {
                return TEST_DATE_TIME_PARIS.toLocalDateTime().isSupported(field);
            }

            @Override
            public long getLong(TemporalField field) {
                return TEST_DATE_TIME_PARIS.toLocalDateTime().getLong(field);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <R> R query(TemporalQuery<R> query) {
                if (query == TemporalQueries.zoneId()) {
                    return (R) TEST_DATE_TIME_PARIS.getZone();
                }
                return TemporalAccessor.super.query(query);
            }
        }), TEST_DATE_TIME_PARIS);
    }

    @Test
    public void factory_from_TemporalAccessor_Instant_ZoneId() {
        assertEquals(ZonedDateTime.from(new TemporalAccessor() {
            @Override
            public boolean isSupported(TemporalField field) {
                return field == INSTANT_SECONDS || field == NANO_OF_SECOND;
            }

            @Override
            public long getLong(TemporalField field) {
                return TEST_DATE_TIME_PARIS.toInstant().getLong(field);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <R> R query(TemporalQuery<R> query) {
                if (query == TemporalQueries.zoneId()) {
                    return (R) TEST_DATE_TIME_PARIS.getZone();
                }
                return TemporalAccessor.super.query(query);
            }
        }), TEST_DATE_TIME_PARIS);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_from_TemporalAccessor_invalid_noDerive() {
        ZonedDateTime.from(LocalTime.of(12, 30));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_from_TemporalAccessor_null() {
        ZonedDateTime.from((TemporalAccessor) null);
    }

    //-----------------------------------------------------------------------
    // parse()
    //-----------------------------------------------------------------------
    @Test(dataProvider="sampleToString")
    public void test_parse(int y, int month, int d, int h, int m, int s, int n, String zoneId, String text) {
        ZonedDateTime t = ZonedDateTime.parse(text);
        assertEquals(t.getYear(), y);
        assertEquals(t.getMonth().getValue(), month);
        assertEquals(t.getDayOfMonth(), d);
        assertEquals(t.getHour(), h);
        assertEquals(t.getMinute(), m);
        assertEquals(t.getSecond(), s);
        assertEquals(t.getNano(), n);
        assertEquals(t.getZone().getId(), zoneId);
    }

    @DataProvider(name="parseAdditional")
    Object[][] data_parseAdditional() {
        return new Object[][] {
                {"2012-06-30T12:30:40Z[GMT]", 2012, 6, 30, 12, 30, 40, 0, "GMT"},
                {"2012-06-30T12:30:40Z[UT]", 2012, 6, 30, 12, 30, 40, 0, "UT"},
                {"2012-06-30T12:30:40Z[UTC]", 2012, 6, 30, 12, 30, 40, 0, "UTC"},
                {"2012-06-30T12:30:40+01:00[Z]", 2012, 6, 30, 11, 30, 40, 0, "Z"},
                {"2012-06-30T12:30:40+01:00[+01:00]", 2012, 6, 30, 12, 30, 40, 0, "+01:00"},
                {"2012-06-30T12:30:40+01:00[GMT+01:00]", 2012, 6, 30, 12, 30, 40, 0, "GMT+01:00"},
                {"2012-06-30T12:30:40+01:00[UT+01:00]", 2012, 6, 30, 12, 30, 40, 0, "UT+01:00"},
                {"2012-06-30T12:30:40+01:00[UTC+01:00]", 2012, 6, 30, 12, 30, 40, 0, "UTC+01:00"},
                {"2012-06-30T12:30:40-01:00[-01:00]", 2012, 6, 30, 12, 30, 40, 0, "-01:00"},
                {"2012-06-30T12:30:40-01:00[GMT-01:00]", 2012, 6, 30, 12, 30, 40, 0, "GMT-01:00"},
                {"2012-06-30T12:30:40-01:00[UT-01:00]", 2012, 6, 30, 12, 30, 40, 0, "UT-01:00"},
                {"2012-06-30T12:30:40-01:00[UTC-01:00]", 2012, 6, 30, 12, 30, 40, 0, "UTC-01:00"},
                {"2012-06-30T12:30:40+01:00[Europe/London]", 2012, 6, 30, 12, 30, 40, 0, "Europe/London"},
                {"2012-06-30T12:30:40+01", 2012, 6, 30, 12, 30, 40, 0, "+01:00"},
        };
    }

    @Test(dataProvider="parseAdditional")
    public void test_parseAdditional(String text, int y, int month, int d, int h, int m, int s, int n, String zoneId) {
        ZonedDateTime t = ZonedDateTime.parse(text);
        assertEquals(t.getYear(), y);
        assertEquals(t.getMonth().getValue(), month);
        assertEquals(t.getDayOfMonth(), d);
        assertEquals(t.getHour(), h);
        assertEquals(t.getMinute(), m);
        assertEquals(t.getSecond(), s);
        assertEquals(t.getNano(), n);
        assertEquals(t.getZone().getId(), zoneId);
    }

    @Test(expectedExceptions=DateTimeParseException.class)
    public void factory_parse_illegalValue() {
        ZonedDateTime.parse("2008-06-32T11:15+01:00[Europe/Paris]");
    }

    @Test(expectedExceptions=DateTimeParseException.class)
    public void factory_parse_invalidValue() {
        ZonedDateTime.parse("2008-06-31T11:15+01:00[Europe/Paris]");
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_nullText() {
        ZonedDateTime.parse((String) null);
    }

    //-----------------------------------------------------------------------
    // parse(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void factory_parse_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("y M d H m s VV");
        ZonedDateTime test = ZonedDateTime.parse("2010 12 3 11 30 0 Europe/London", f);
        assertEquals(test, ZonedDateTime.of(LocalDateTime.of(2010, 12, 3, 11, 30), ZoneId.of("Europe/London")));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_formatter_nullText() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("y M d H m s");
        ZonedDateTime.parse((String) null, f);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_formatter_nullFormatter() {
        ZonedDateTime.parse("ANY", null);
    }

    //-----------------------------------------------------------------------
    // basics
    //-----------------------------------------------------------------------
    @DataProvider(name="sampleTimes")
    Object[][] provider_sampleTimes() {
        return new Object[][] {
            {2008, 6, 30, 11, 30, 20, 500, ZONE_0100},
            {2008, 6, 30, 11, 0, 0, 0, ZONE_0100},
            {2008, 6, 30, 11, 30, 20, 500, ZONE_PARIS},
            {2008, 6, 30, 11, 0, 0, 0, ZONE_PARIS},
            {2008, 6, 30, 23, 59, 59, 999999999, ZONE_0100},
            {-1, 1, 1, 0, 0, 0, 0, ZONE_0100},
        };
    }

    @Test(dataProvider="sampleTimes")
    public void test_get(int y, int o, int d, int h, int m, int s, int n, ZoneId zone) {
        LocalDate localDate = LocalDate.of(y, o, d);
        LocalTime localTime = LocalTime.of(h, m, s, n);
        LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);
        ZoneOffset offset = zone.getRules().getOffset(localDateTime);
        ZonedDateTime a = ZonedDateTime.of(localDateTime, zone);

        assertEquals(a.getYear(), localDate.getYear());
        assertEquals(a.getMonth(), localDate.getMonth());
        assertEquals(a.getDayOfMonth(), localDate.getDayOfMonth());
        assertEquals(a.getDayOfYear(), localDate.getDayOfYear());
        assertEquals(a.getDayOfWeek(), localDate.getDayOfWeek());

        assertEquals(a.getHour(), localTime.getHour());
        assertEquals(a.getMinute(), localTime.getMinute());
        assertEquals(a.getSecond(), localTime.getSecond());
        assertEquals(a.getNano(), localTime.getNano());

        assertEquals(a.toLocalDate(), localDate);
        assertEquals(a.toLocalTime(), localTime);
        assertEquals(a.toLocalDateTime(), localDateTime);
        if (zone instanceof ZoneOffset) {
            assertEquals(a.toString(), localDateTime.toString() + offset.toString());
        } else {
            assertEquals(a.toString(), localDateTime.toString() + offset.toString() + "[" + zone.toString() + "]");
        }
    }

    //-----------------------------------------------------------------------
    // isSupported(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_isSupported_TemporalField() {
        assertEquals(TEST_DATE_TIME.isSupported((TemporalField) null), false);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.NANO_OF_SECOND), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.NANO_OF_DAY), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.MICRO_OF_SECOND), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.MICRO_OF_DAY), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.MILLI_OF_SECOND), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.MILLI_OF_DAY), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.SECOND_OF_MINUTE), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.SECOND_OF_DAY), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.MINUTE_OF_HOUR), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.MINUTE_OF_DAY), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.HOUR_OF_AMPM), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.CLOCK_HOUR_OF_AMPM), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.HOUR_OF_DAY), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.CLOCK_HOUR_OF_DAY), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.AMPM_OF_DAY), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.DAY_OF_WEEK), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.DAY_OF_MONTH), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.DAY_OF_YEAR), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.EPOCH_DAY), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.ALIGNED_WEEK_OF_MONTH), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.ALIGNED_WEEK_OF_YEAR), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.MONTH_OF_YEAR), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.PROLEPTIC_MONTH), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.YEAR), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.YEAR_OF_ERA), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.ERA), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.INSTANT_SECONDS), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoField.OFFSET_SECONDS), true);
    }

    //-----------------------------------------------------------------------
    // isSupported(TemporalUnit)
    //-----------------------------------------------------------------------
    @Test
    public void test_isSupported_TemporalUnit() {
        assertEquals(TEST_DATE_TIME.isSupported((TemporalUnit) null), false);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoUnit.NANOS), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoUnit.MICROS), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoUnit.MILLIS), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoUnit.SECONDS), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoUnit.MINUTES), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoUnit.HOURS), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoUnit.HALF_DAYS), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoUnit.DAYS), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoUnit.WEEKS), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoUnit.MONTHS), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoUnit.YEARS), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoUnit.DECADES), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoUnit.CENTURIES), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoUnit.MILLENNIA), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoUnit.ERAS), true);
        assertEquals(TEST_DATE_TIME.isSupported(ChronoUnit.FOREVER), false);
    }

    //-----------------------------------------------------------------------
    // get(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_TemporalField() {
        ZonedDateTime test = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, 12, 30, 40, 987654321), ZONE_0100);
        assertEquals(test.get(ChronoField.YEAR), 2008);
        assertEquals(test.get(ChronoField.MONTH_OF_YEAR), 6);
        assertEquals(test.get(ChronoField.DAY_OF_MONTH), 30);
        assertEquals(test.get(ChronoField.DAY_OF_WEEK), 1);
        assertEquals(test.get(ChronoField.DAY_OF_YEAR), 182);

        assertEquals(test.get(ChronoField.HOUR_OF_DAY), 12);
        assertEquals(test.get(ChronoField.MINUTE_OF_HOUR), 30);
        assertEquals(test.get(ChronoField.SECOND_OF_MINUTE), 40);
        assertEquals(test.get(ChronoField.NANO_OF_SECOND), 987654321);
        assertEquals(test.get(ChronoField.HOUR_OF_AMPM), 0);
        assertEquals(test.get(ChronoField.AMPM_OF_DAY), 1);

        assertEquals(test.get(ChronoField.OFFSET_SECONDS), 3600);
    }

    @Test
    public void test_getLong_TemporalField() {
        ZonedDateTime test = ZonedDateTime.of(LocalDateTime.of(2008, 6, 30, 12, 30, 40, 987654321), ZONE_0100);
        assertEquals(test.getLong(ChronoField.YEAR), 2008);
        assertEquals(test.getLong(ChronoField.MONTH_OF_YEAR), 6);
        assertEquals(test.getLong(ChronoField.DAY_OF_MONTH), 30);
        assertEquals(test.getLong(ChronoField.DAY_OF_WEEK), 1);
        assertEquals(test.getLong(ChronoField.DAY_OF_YEAR), 182);

        assertEquals(test.getLong(ChronoField.HOUR_OF_DAY), 12);
        assertEquals(test.getLong(ChronoField.MINUTE_OF_HOUR), 30);
        assertEquals(test.getLong(ChronoField.SECOND_OF_MINUTE), 40);
        assertEquals(test.getLong(ChronoField.NANO_OF_SECOND), 987654321);
        assertEquals(test.getLong(ChronoField.HOUR_OF_AMPM), 0);
        assertEquals(test.getLong(ChronoField.AMPM_OF_DAY), 1);

        assertEquals(test.getLong(ChronoField.OFFSET_SECONDS), 3600);
        assertEquals(test.getLong(ChronoField.INSTANT_SECONDS), test.toEpochSecond());
    }

    //-----------------------------------------------------------------------
    // query(TemporalQuery)
    //-----------------------------------------------------------------------
    @Test
    public void test_query_chrono() {
        assertEquals(TEST_DATE_TIME.query(TemporalQueries.chronology()), IsoChronology.INSTANCE);
        assertEquals(TemporalQueries.chronology().queryFrom(TEST_DATE_TIME), IsoChronology.INSTANCE);
    }

    @Test
    public void test_query_zoneId() {
        assertEquals(TEST_DATE_TIME.query(TemporalQueries.zoneId()), TEST_DATE_TIME.getZone());
        assertEquals(TemporalQueries.zoneId().queryFrom(TEST_DATE_TIME), TEST_DATE_TIME.getZone());
    }

    @Test
    public void test_query_precision() {
        assertEquals(TEST_DATE_TIME.query(TemporalQueries.precision()), NANOS);
        assertEquals(TemporalQueries.precision().queryFrom(TEST_DATE_TIME), NANOS);
    }

    @Test
    public void test_query_offset() {
        assertEquals(TEST_DATE_TIME.query(TemporalQueries.offset()), TEST_DATE_TIME.getOffset());
        assertEquals(TemporalQueries.offset().queryFrom(TEST_DATE_TIME), TEST_DATE_TIME.getOffset());
    }

    @Test
    public void test_query_zone() {
        assertEquals(TEST_DATE_TIME.query(TemporalQueries.zone()), TEST_DATE_TIME.getZone());
        assertEquals(TemporalQueries.zone().queryFrom(TEST_DATE_TIME), TEST_DATE_TIME.getZone());
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_query_null() {
        TEST_DATE_TIME.query(null);
    }

    //-----------------------------------------------------------------------
    // withEarlierOffsetAtOverlap()
    //-----------------------------------------------------------------------
    @Test
    public void test_withEarlierOffsetAtOverlap_notAtOverlap() {
        ZonedDateTime base = ZonedDateTime.ofStrict(TEST_LOCAL_2008_06_30_11_30_59_500, OFFSET_0200, ZONE_PARIS);
        ZonedDateTime test = base.withEarlierOffsetAtOverlap();
        assertEquals(test, base);  // not changed
    }

    @Test
    public void test_withEarlierOffsetAtOverlap_atOverlap() {
        ZonedDateTime base = ZonedDateTime.ofStrict(TEST_PARIS_OVERLAP_2008_10_26_02_30, OFFSET_0100, ZONE_PARIS);
        ZonedDateTime test = base.withEarlierOffsetAtOverlap();
        assertEquals(test.getOffset(), OFFSET_0200);  // offset changed to earlier
        assertEquals(test.toLocalDateTime(), base.toLocalDateTime());  // date-time not changed
    }

    @Test
    public void test_withEarlierOffsetAtOverlap_atOverlap_noChange() {
        ZonedDateTime base = ZonedDateTime.ofStrict(TEST_PARIS_OVERLAP_2008_10_26_02_30, OFFSET_0200, ZONE_PARIS);
        ZonedDateTime test = base.withEarlierOffsetAtOverlap();
        assertEquals(test, base);  // not changed
    }

    //-----------------------------------------------------------------------
    // withLaterOffsetAtOverlap()
    //-----------------------------------------------------------------------
    @Test
    public void test_withLaterOffsetAtOverlap_notAtOverlap() {
        ZonedDateTime base = ZonedDateTime.ofStrict(TEST_LOCAL_2008_06_30_11_30_59_500, OFFSET_0200, ZONE_PARIS);
        ZonedDateTime test = base.withLaterOffsetAtOverlap();
        assertEquals(test, base);  // not changed
    }

    @Test
    public void test_withLaterOffsetAtOverlap_atOverlap() {
        ZonedDateTime base = ZonedDateTime.ofStrict(TEST_PARIS_OVERLAP_2008_10_26_02_30, OFFSET_0200, ZONE_PARIS);
        ZonedDateTime test = base.withLaterOffsetAtOverlap();
        assertEquals(test.getOffset(), OFFSET_0100);  // offset changed to later
        assertEquals(test.toLocalDateTime(), base.toLocalDateTime());  // date-time not changed
    }

    @Test
    public void test_withLaterOffsetAtOverlap_atOverlap_noChange() {
        ZonedDateTime base = ZonedDateTime.ofStrict(TEST_PARIS_OVERLAP_2008_10_26_02_30, OFFSET_0100, ZONE_PARIS);
        ZonedDateTime test = base.withLaterOffsetAtOverlap();
        assertEquals(test, base);  // not changed
    }

    //-----------------------------------------------------------------------
    // withZoneSameLocal(ZoneId)
    //-----------------------------------------------------------------------
    @Test
    public void test_withZoneSameLocal() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.withZoneSameLocal(ZONE_0200);
        assertEquals(test.toLocalDateTime(), base.toLocalDateTime());
    }

    @Test
    public void test_withZoneSameLocal_noChange() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.withZoneSameLocal(ZONE_0100);
        assertEquals(test, base);
    }

    @Test
    public void test_withZoneSameLocal_retainOffset1() {
        LocalDateTime ldt = LocalDateTime.of(2008, 11, 2, 1, 30, 59, 0);  // overlap
        ZonedDateTime base = ZonedDateTime.of(ldt, ZoneId.of("UTC-04:00") );
        ZonedDateTime test = base.withZoneSameLocal(ZoneId.of("America/New_York"));
        assertEquals(base.getOffset(), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(), ZoneOffset.ofHours(-4));
    }

    @Test
    public void test_withZoneSameLocal_retainOffset2() {
        LocalDateTime ldt = LocalDateTime.of(2008, 11, 2, 1, 30, 59, 0);  // overlap
        ZonedDateTime base = ZonedDateTime.of(ldt, ZoneId.of("UTC-05:00") );
        ZonedDateTime test = base.withZoneSameLocal(ZoneId.of("America/New_York"));
        assertEquals(base.getOffset(), ZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(), ZoneOffset.ofHours(-5));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_withZoneSameLocal_null() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        base.withZoneSameLocal(null);
    }

    //-----------------------------------------------------------------------
    // withZoneSameInstant()
    //-----------------------------------------------------------------------
    @Test
    public void test_withZoneSameInstant() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        ZonedDateTime test = base.withZoneSameInstant(ZONE_0200);
        ZonedDateTime expected = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500.plusHours(1), ZONE_0200);
        assertEquals(test, expected);
    }

    @Test
    public void test_withZoneSameInstant_noChange() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        ZonedDateTime test = base.withZoneSameInstant(ZONE_0100);
        assertEquals(test, base);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_withZoneSameInstant_null() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        base.withZoneSameInstant(null);
    }

    //-----------------------------------------------------------------------
    // withFixedOffsetZone()
    //-----------------------------------------------------------------------
    @Test
    public void test_withZoneLocked() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_PARIS);
        ZonedDateTime test = base.withFixedOffsetZone();
        ZonedDateTime expected = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0200);
        assertEquals(test, expected);
    }

    //-----------------------------------------------------------------------
    // with(TemporalAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_with_adjuster_LocalDateTime_sameOffset() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_PARIS);
        ZonedDateTime test = base.with(LocalDateTime.of(2012, 7, 15, 14, 30));
        check(test, 2012, 7, 15, 14, 30, 0, 0, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void test_with_adjuster_LocalDateTime_adjustedOffset() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_PARIS);
        ZonedDateTime test = base.with(LocalDateTime.of(2012, 1, 15, 14, 30));
        check(test, 2012, 1, 15, 14, 30, 0, 0, OFFSET_0100, ZONE_PARIS);
    }

    @Test
    public void test_with_adjuster_LocalDate() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_PARIS);
        ZonedDateTime test = base.with(LocalDate.of(2012, 7, 28));
        check(test, 2012, 7, 28, 11, 30, 59, 500, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void test_with_adjuster_LocalTime() {
        ZonedDateTime base = ZonedDateTime.of(TEST_PARIS_OVERLAP_2008_10_26_02_30, ZONE_PARIS);
        ZonedDateTime test = base.with(LocalTime.of(2, 29));
        check(test, 2008, 10, 26, 2, 29, 0, 0, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void test_with_adjuster_Year() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.with(Year.of(2007));
        assertEquals(test, ZonedDateTime.of(ldt.withYear(2007), ZONE_0100));
    }

    @Test
    public void test_with_adjuster_Month_adjustedDayOfMonth() {
        ZonedDateTime base = ZonedDateTime.of(LocalDateTime.of(2012, 7, 31, 0, 0), ZONE_PARIS);
        ZonedDateTime test = base.with(Month.JUNE);
        check(test, 2012, 6, 30, 0, 0, 0, 0, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void test_with_adjuster_Offset_same() {
        ZonedDateTime base = ZonedDateTime.of(LocalDateTime.of(2012, 7, 31, 0, 0), ZONE_PARIS);
        ZonedDateTime test = base.with(ZoneOffset.ofHours(2));
        check(test, 2012, 7, 31, 0, 0, 0, 0, OFFSET_0200, ZONE_PARIS);
    }

    @Test
    public void test_with_adjuster_Offset_timeAdjust() {
        ZonedDateTime base = ZonedDateTime.of(LocalDateTime.of(2012, 7, 31, 0, 0), ZONE_PARIS);
        ZonedDateTime test = base.with(ZoneOffset.ofHours(1));
        check(test, 2012, 7, 31, 0, 0, 0, 0, OFFSET_0200, ZONE_PARIS);  // invalid offset ignored
    }

    @Test
    public void test_with_adjuster_LocalDate_retainOffset1() {
        ZoneId newYork = ZoneId.of("America/New_York");
        LocalDateTime ldt = LocalDateTime.of(2008, 11, 1, 1, 30);
        ZonedDateTime base = ZonedDateTime.of(ldt, newYork);
        assertEquals(base.getOffset(), ZoneOffset.ofHours(-4));
        ZonedDateTime test = base.with(LocalDate.of(2008, 11, 2));
        assertEquals(test.getOffset(), ZoneOffset.ofHours(-4));
    }

    @Test
    public void test_with_adjuster_LocalDate_retainOffset2() {
        ZoneId newYork = ZoneId.of("America/New_York");
        LocalDateTime ldt = LocalDateTime.of(2008, 11, 3, 1, 30);
        ZonedDateTime base = ZonedDateTime.of(ldt, newYork);
        assertEquals(base.getOffset(), ZoneOffset.ofHours(-5));
        ZonedDateTime test = base.with(LocalDate.of(2008, 11, 2));
        assertEquals(test.getOffset(), ZoneOffset.ofHours(-5));
    }

    @Test
    public void test_with_adjuster_OffsetDateTime_validOffsetNotInOverlap() {
        // ODT will be a valid ZDT for the zone, so must be retained exactly
        OffsetDateTime odt = TEST_LOCAL_2008_06_30_11_30_59_500.atOffset(OFFSET_0200);
        ZonedDateTime zdt = TEST_PARIS_OVERLAP_2008_10_26_02_30.atZone(ZONE_PARIS);
        ZonedDateTime test = zdt.with(odt);
        assertEquals(test.toOffsetDateTime(), odt);
    }

    @Test
    public void test_with_adjuster_OffsetDateTime_invalidOffsetIgnored() {
        // ODT has invalid offset for ZDT, so only LDT is set
        OffsetDateTime odt = TEST_LOCAL_2008_06_30_11_30_59_500.atOffset(OFFSET_0130);
        ZonedDateTime zdt = TEST_PARIS_OVERLAP_2008_10_26_02_30.atZone(ZONE_PARIS);
        ZonedDateTime test = zdt.with(odt);
        assertEquals(test.toLocalDateTime(), TEST_LOCAL_2008_06_30_11_30_59_500);
        assertEquals(test.getOffset(), zdt.getOffset());
    }

    @Test
    public void test_with_adjuster_OffsetDateTime_retainOffsetInOverlap1() {
        // ODT will be a valid ZDT for the zone, so must be retained exactly
        OffsetDateTime odt = TEST_PARIS_OVERLAP_2008_10_26_02_30.atOffset(OFFSET_0100);
        ZonedDateTime zdt = TEST_LOCAL_2008_06_30_11_30_59_500.atZone(ZONE_PARIS);
        ZonedDateTime test = zdt.with(odt);
        assertEquals(test.toOffsetDateTime(), odt);
    }

    @Test
    public void test_with_adjuster_OffsetDateTime_retainOffsetInOverlap2() {
        // ODT will be a valid ZDT for the zone, so must be retained exactly
        OffsetDateTime odt = TEST_PARIS_OVERLAP_2008_10_26_02_30.atOffset(OFFSET_0200);
        ZonedDateTime zdt = TEST_LOCAL_2008_06_30_11_30_59_500.atZone(ZONE_PARIS);
        ZonedDateTime test = zdt.with(odt);
        assertEquals(test.toOffsetDateTime(), odt);
    }

    @Test
    public void test_with_adjuster_OffsetTime_validOffsetNotInOverlap() {
        // OT has valid offset for resulting time
        OffsetTime ot = OffsetTime.of(15, 50, 30, 40, OFFSET_0100);
        ZonedDateTime zdt = TEST_PARIS_OVERLAP_2008_10_26_02_30.atZone(ZONE_PARIS);
        ZonedDateTime test = zdt.with(ot);
        assertEquals(test.toLocalDateTime(), dateTime(2008, 10, 26, 15, 50, 30, 40));
        assertEquals(test.getOffset(), OFFSET_0100);
    }

    @Test
    public void test_with_adjuster_OffsetTime_invalidOffsetIgnored1() {
        // OT has invalid offset for ZDT, so only LT is set
        OffsetTime ot = OffsetTime.of(0, 50, 30, 40, OFFSET_0130);
        ZonedDateTime zdt = dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS);  // earlier part of overlap
        ZonedDateTime test = zdt.with(ot);
        assertEquals(test.toLocalDateTime(), dateTime(2008, 10, 26, 0, 50, 30, 40));
        assertEquals(test.getOffset(), OFFSET_0200);  // offset not adjusted
    }

    @Test
    public void test_with_adjuster_OffsetTime_invalidOffsetIgnored2() {
        // OT has invalid offset for ZDT, so only LT is set
        OffsetTime ot = OffsetTime.of(15, 50, 30, 40, OFFSET_0130);
        ZonedDateTime zdt = dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS);  // earlier part of overlap
        ZonedDateTime test = zdt.with(ot);
        assertEquals(test.toLocalDateTime(), dateTime(2008, 10, 26, 15, 50, 30, 40));
        assertEquals(test.getOffset(), OFFSET_0100);  // offset adjusted because of time change
    }

    @Test
    public void test_with_adjuster_OffsetTime_validOffsetIntoOverlap1() {
        // OT has valid offset for resulting time
        OffsetTime ot = OffsetTime.of(2, 30, 30, 40, OFFSET_0100);  // valid offset in overlap
        ZonedDateTime zdt = dateTime(2008, 10, 26, 0, 0, 0, 0, OFFSET_0200, ZONE_PARIS);  // just before overlap
        ZonedDateTime test = zdt.with(ot);
        assertEquals(test.toLocalDateTime(), dateTime(2008, 10, 26, 2, 30, 30, 40));
        assertEquals(test.getOffset(), OFFSET_0100);
    }

    @Test
    public void test_with_adjuster_OffsetTime_validOffsetIntoOverlap2() {
        // OT has valid offset for resulting time
        OffsetTime ot = OffsetTime.of(2, 30, 30, 40, OFFSET_0200);  // valid offset in overlap
        ZonedDateTime zdt = dateTime(2008, 10, 26, 0, 0, 0, 0, OFFSET_0200, ZONE_PARIS);  // just before overlap
        ZonedDateTime test = zdt.with(ot);
        assertEquals(test.toLocalDateTime(), dateTime(2008, 10, 26, 2, 30, 30, 40));
        assertEquals(test.getOffset(), OFFSET_0200);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_with_adjuster_null() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        base.with((TemporalAdjuster) null);
    }

    //-----------------------------------------------------------------------
    // with(long,TemporalUnit)
    //-----------------------------------------------------------------------
    @DataProvider(name = "withFieldLong")
    Object[][] data_withFieldLong() {
        return new Object[][] {
                // set simple fields
                {TEST_LOCAL_2008_06_30_11_30_59_500.atZone(ZONE_PARIS), YEAR, 2009,
                        dateTime(2009, 6, 30, 11, 30, 59, 500, OFFSET_0200, ZONE_PARIS)},
                {TEST_LOCAL_2008_06_30_11_30_59_500.atZone(ZONE_PARIS), MONTH_OF_YEAR, 7,
                        dateTime(2008, 7, 30, 11, 30, 59, 500, OFFSET_0200, ZONE_PARIS)},
                {TEST_LOCAL_2008_06_30_11_30_59_500.atZone(ZONE_PARIS), DAY_OF_MONTH, 15,
                        dateTime(2008, 6, 15, 11, 30, 59, 500, OFFSET_0200, ZONE_PARIS)},
                {TEST_LOCAL_2008_06_30_11_30_59_500.atZone(ZONE_PARIS), HOUR_OF_DAY, 14,
                        dateTime(2008, 6, 30, 14, 30, 59, 500, OFFSET_0200, ZONE_PARIS)},

                // set around overlap
                {TEST_PARIS_OVERLAP_2008_10_26_02_30.atZone(ZONE_PARIS).withEarlierOffsetAtOverlap(), HOUR_OF_DAY, 0,
                        dateTime(2008, 10, 26, 0, 30, 0, 0, OFFSET_0200, ZONE_PARIS)},
                {TEST_PARIS_OVERLAP_2008_10_26_02_30.atZone(ZONE_PARIS).withLaterOffsetAtOverlap(), HOUR_OF_DAY, 0,
                        dateTime(2008, 10, 26, 0, 30, 0, 0, OFFSET_0200, ZONE_PARIS)},
                {TEST_PARIS_OVERLAP_2008_10_26_02_30.atZone(ZONE_PARIS).withEarlierOffsetAtOverlap(), MINUTE_OF_HOUR, 20,
                        dateTime(2008, 10, 26, 2, 20, 0, 0, OFFSET_0200, ZONE_PARIS)},  // offset unchanged
                {TEST_PARIS_OVERLAP_2008_10_26_02_30.atZone(ZONE_PARIS).withLaterOffsetAtOverlap(), MINUTE_OF_HOUR, 20,
                        dateTime(2008, 10, 26, 2, 20, 0, 0, OFFSET_0100, ZONE_PARIS)},  // offset unchanged
                {TEST_PARIS_OVERLAP_2008_10_26_02_30.atZone(ZONE_PARIS).withEarlierOffsetAtOverlap(), HOUR_OF_DAY, 3,
                        dateTime(2008, 10, 26, 3, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},
                {TEST_PARIS_OVERLAP_2008_10_26_02_30.atZone(ZONE_PARIS).withLaterOffsetAtOverlap(), HOUR_OF_DAY, 3,
                        dateTime(2008, 10, 26, 3, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},

                // set offset
                {TEST_LOCAL_2008_06_30_11_30_59_500.atZone(ZONE_PARIS), OFFSET_SECONDS, 7200,
                        dateTime(2008, 6, 30, 11, 30, 59, 500, OFFSET_0200, ZONE_PARIS)},  // offset unchanged
                {TEST_LOCAL_2008_06_30_11_30_59_500.atZone(ZONE_PARIS), OFFSET_SECONDS, 3600,
                        dateTime(2008, 6, 30, 11, 30, 59, 500, OFFSET_0200, ZONE_PARIS)},  // invalid offset ignored
                {TEST_PARIS_OVERLAP_2008_10_26_02_30.atZone(ZONE_PARIS).withEarlierOffsetAtOverlap(), OFFSET_SECONDS, 3600,
                        dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},
                {TEST_PARIS_OVERLAP_2008_10_26_02_30.atZone(ZONE_PARIS).withLaterOffsetAtOverlap(), OFFSET_SECONDS, 3600,
                        dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},
                {TEST_PARIS_OVERLAP_2008_10_26_02_30.atZone(ZONE_PARIS).withEarlierOffsetAtOverlap(), OFFSET_SECONDS, 7200,
                        dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS)},
                {TEST_PARIS_OVERLAP_2008_10_26_02_30.atZone(ZONE_PARIS).withLaterOffsetAtOverlap(), OFFSET_SECONDS, 7200,
                        dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS)},
        };
    };

    @Test(dataProvider = "withFieldLong")
    public void test_with_fieldLong(ZonedDateTime base, TemporalField setField, int setValue, ZonedDateTime expected) {
        assertEquals(base.with(setField, setValue), expected);
    }

    @Test(dataProvider = "withFieldLong")
    public void test_with_adjuster_ensureZoneOffsetConsistent(ZonedDateTime base, TemporalField setField, int setValue, ZonedDateTime expected) {
        if (setField == OFFSET_SECONDS) {
            assertEquals(base.with(ZoneOffset.ofTotalSeconds(setValue)), expected);
        }
    }

    @Test(dataProvider = "withFieldLong")
    public void test_with_adjuster_ensureOffsetDateTimeConsistent(ZonedDateTime base, TemporalField setField, int setValue, ZonedDateTime expected) {
        if (setField == OFFSET_SECONDS) {
            OffsetDateTime odt = base.toOffsetDateTime().with(setField, setValue);
            assertEquals(base.with(odt), expected);
        }
    }

    //-----------------------------------------------------------------------
    // withYear()
    //-----------------------------------------------------------------------
    @Test
    public void test_withYear_normal() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        ZonedDateTime test = base.withYear(2007);
        assertEquals(test, ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500.withYear(2007), ZONE_0100));
    }

    @Test
    public void test_withYear_noChange() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        ZonedDateTime test = base.withYear(2008);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // with(Month)
    //-----------------------------------------------------------------------
    @Test
    public void test_withMonth_Month_normal() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        ZonedDateTime test = base.with(JANUARY);
        assertEquals(test, ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500.withMonth(1), ZONE_0100));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_withMonth_Month_null() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        base.with((Month) null);
    }

    //-----------------------------------------------------------------------
    // withMonth()
    //-----------------------------------------------------------------------
    @Test
    public void test_withMonth_normal() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        ZonedDateTime test = base.withMonth(1);
        assertEquals(test, ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500.withMonth(1), ZONE_0100));
    }

    @Test
    public void test_withMonth_noChange() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        ZonedDateTime test = base.withMonth(6);
        assertEquals(test, base);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_withMonth_tooBig() {
        TEST_DATE_TIME.withMonth(13);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_withMonth_tooSmall() {
        TEST_DATE_TIME.withMonth(0);
    }

    //-----------------------------------------------------------------------
    // withDayOfMonth()
    //-----------------------------------------------------------------------
    @Test
    public void test_withDayOfMonth_normal() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        ZonedDateTime test = base.withDayOfMonth(15);
        assertEquals(test, ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500.withDayOfMonth(15), ZONE_0100));
    }

    @Test
    public void test_withDayOfMonth_noChange() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        ZonedDateTime test = base.withDayOfMonth(30);
        assertEquals(test, base);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_withDayOfMonth_tooBig() {
        LocalDateTime.of(2007, 7, 2, 11, 30).atZone(ZONE_PARIS).withDayOfMonth(32);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_withDayOfMonth_tooSmall() {
        TEST_DATE_TIME.withDayOfMonth(0);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_withDayOfMonth_invalid31() {
        LocalDateTime.of(2007, 6, 2, 11, 30).atZone(ZONE_PARIS).withDayOfMonth(31);
    }

    //-----------------------------------------------------------------------
    // withDayOfYear()
    //-----------------------------------------------------------------------
    @Test
    public void test_withDayOfYear_normal() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        ZonedDateTime test = base.withDayOfYear(33);
        assertEquals(test, ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500.withDayOfYear(33), ZONE_0100));
    }

    @Test
    public void test_withDayOfYear_noChange() {
        LocalDateTime ldt = LocalDateTime.of(2008, 2, 5, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.withDayOfYear(36);
        assertEquals(test, base);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_withDayOfYear_tooBig() {
        TEST_DATE_TIME.withDayOfYear(367);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_withDayOfYear_tooSmall() {
        TEST_DATE_TIME.withDayOfYear(0);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_withDayOfYear_invalid366() {
        LocalDateTime.of(2007, 2, 2, 11, 30).atZone(ZONE_PARIS).withDayOfYear(366);
    }

    //-----------------------------------------------------------------------
    // withHour()
    //-----------------------------------------------------------------------
    @Test
    public void test_withHour_normal() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        ZonedDateTime test = base.withHour(15);
        assertEquals(test, ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500.withHour(15), ZONE_0100));
    }

    @Test
    public void test_withHour_noChange() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        ZonedDateTime test = base.withHour(11);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // withMinute()
    //-----------------------------------------------------------------------
    @Test
    public void test_withMinute_normal() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        ZonedDateTime test = base.withMinute(15);
        assertEquals(test, ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500.withMinute(15), ZONE_0100));
    }

    @Test
    public void test_withMinute_noChange() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        ZonedDateTime test = base.withMinute(30);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // withSecond()
    //-----------------------------------------------------------------------
    @Test
    public void test_withSecond_normal() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        ZonedDateTime test = base.withSecond(12);
        assertEquals(test, ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500.withSecond(12), ZONE_0100));
    }

    @Test
    public void test_withSecond_noChange() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        ZonedDateTime test = base.withSecond(59);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // withNano()
    //-----------------------------------------------------------------------
    @Test
    public void test_withNanoOfSecond_normal() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        ZonedDateTime test = base.withNano(15);
        assertEquals(test, ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500.withNano(15), ZONE_0100));
    }

    @Test
    public void test_withNanoOfSecond_noChange() {
        ZonedDateTime base = ZonedDateTime.of(TEST_LOCAL_2008_06_30_11_30_59_500, ZONE_0100);
        ZonedDateTime test = base.withNano(500);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // truncatedTo(TemporalUnit)
    //-----------------------------------------------------------------------
    @Test
    public void test_truncatedTo_normal() {
        assertEquals(TEST_DATE_TIME.truncatedTo(NANOS), TEST_DATE_TIME);
        assertEquals(TEST_DATE_TIME.truncatedTo(SECONDS), TEST_DATE_TIME.withNano(0));
        assertEquals(TEST_DATE_TIME.truncatedTo(DAYS), TEST_DATE_TIME.with(LocalTime.MIDNIGHT));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_truncatedTo_null() {
        TEST_DATE_TIME.truncatedTo(null);
    }

    //-----------------------------------------------------------------------
    // plus/minus
    //-----------------------------------------------------------------------
    @DataProvider(name="plusDays")
    Object[][] data_plusDays() {
        return new Object[][] {
            // normal
            {dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100), 0, dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100)},
            {dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100), 1, dateTime(2008, 7, 1, 23, 30, 59, 0, OFFSET_0100, ZONE_0100)},
            {dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100), -1, dateTime(2008, 6, 29, 23, 30, 59, 0, OFFSET_0100, ZONE_0100)},
            // skip over gap
            {dateTime(2008, 3, 30, 1, 30, 0, 0, OFFSET_0100, ZONE_PARIS), 1, dateTime(2008, 3, 31, 1, 30, 0, 0, OFFSET_0200, ZONE_PARIS)},
            {dateTime(2008, 3, 30, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS), -1, dateTime(2008, 3, 29, 3, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},
            // land in gap
            {dateTime(2008, 3, 29, 2, 30, 0, 0, OFFSET_0100, ZONE_PARIS), 1, dateTime(2008, 3, 30, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS)},
            {dateTime(2008, 3, 31, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS), -1, dateTime(2008, 3, 30, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS)},
            // skip over overlap
            {dateTime(2008, 10, 26, 1, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 1, dateTime(2008, 10, 27, 1, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},
            {dateTime(2008, 10, 25, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 1, dateTime(2008, 10, 26, 3, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},
            // land in overlap
            {dateTime(2008, 10, 25, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 1, dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS)},
            {dateTime(2008, 10, 27, 2, 30, 0, 0, OFFSET_0100, ZONE_PARIS), -1, dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},
        };
    }

    @DataProvider(name="plusTime")
    Object[][] data_plusTime() {
        return new Object[][] {
            // normal
            {dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100), 0, dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100)},
            {dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100), 1, dateTime(2008, 7, 1, 0, 30, 59, 0, OFFSET_0100, ZONE_0100)},
            {dateTime(2008, 6, 30, 23, 30, 59, 0, OFFSET_0100, ZONE_0100), -1, dateTime(2008, 6, 30, 22, 30, 59, 0, OFFSET_0100, ZONE_0100)},
            // gap
            {dateTime(2008, 3, 30, 1, 30, 0, 0, OFFSET_0100, ZONE_PARIS), 1, dateTime(2008, 3, 30, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS)},
            {dateTime(2008, 3, 30, 3, 30, 0, 0, OFFSET_0200, ZONE_PARIS), -1, dateTime(2008, 3, 30, 1, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},
            // overlap
            {dateTime(2008, 10, 26, 1, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 1, dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS)},
            {dateTime(2008, 10, 26, 1, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 2, dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},
            {dateTime(2008, 10, 26, 1, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 3, dateTime(2008, 10, 26, 3, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},
            {dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 1, dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},
            {dateTime(2008, 10, 26, 2, 30, 0, 0, OFFSET_0200, ZONE_PARIS), 2, dateTime(2008, 10, 26, 3, 30, 0, 0, OFFSET_0100, ZONE_PARIS)},
        };
    }

    //-----------------------------------------------------------------------
    // plus(TemporalAmount)
    //-----------------------------------------------------------------------
    @Test(dataProvider="plusDays")
    public void test_plus_TemporalAmount_Period_days(ZonedDateTime base, int amount, ZonedDateTime expected) {
        assertEquals(base.plus(Period.ofDays(amount)), expected);
    }

    @Test(dataProvider="plusTime")
    public void test_plus_TemporalAmount_Period_hours(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(base.plus(MockSimplePeriod.of(amount, HOURS)), expected);
    }

    @Test(dataProvider="plusTime")
    public void test_plus_TemporalAmount_Duration_hours(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(base.plus(Duration.ofHours(amount)), expected);
    }

    @Test
    public void test_plus_TemporalAmount() {
        MockSimplePeriod period = MockSimplePeriod.of(7, ChronoUnit.MONTHS);
        ZonedDateTime t = ZonedDateTime.of(LocalDateTime.of(2008, 6, 1, 12, 30, 59, 500), ZONE_0100);
        ZonedDateTime expected = ZonedDateTime.of(LocalDateTime.of(2009, 1, 1, 12, 30, 59, 500), ZONE_0100);
        assertEquals(t.plus(period), expected);
    }

    @Test
    public void test_plus_TemporalAmount_Duration() {
        Duration duration = Duration.ofSeconds(4L * 60 * 60 + 5L * 60 + 6L);
        ZonedDateTime t = ZonedDateTime.of(LocalDateTime.of(2008, 6, 1, 12, 30, 59, 500), ZONE_0100);
        ZonedDateTime expected = ZonedDateTime.of(LocalDateTime.of(2008, 6, 1, 16, 36, 5, 500), ZONE_0100);
        assertEquals(t.plus(duration), expected);
    }

    @Test
    public void test_plus_TemporalAmount_Period_zero() {
        ZonedDateTime t = TEST_DATE_TIME.plus(MockSimplePeriod.ZERO_DAYS);
        assertEquals(t, TEST_DATE_TIME);
    }

    @Test
    public void test_plus_TemporalAmount_Duration_zero() {
        ZonedDateTime t = TEST_DATE_TIME.plus(Duration.ZERO);
        assertEquals(t, TEST_DATE_TIME);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_plus_TemporalAmount_null() {
        TEST_DATE_TIME.plus((TemporalAmount) null);
    }

    //-----------------------------------------------------------------------
    // plus(long,TemporalUnit)
    //-----------------------------------------------------------------------
    @Test(dataProvider="plusDays")
    public void test_plus_longUnit_days(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(base.plus(amount, DAYS), expected);
    }

    @Test(dataProvider="plusTime")
    public void test_plus_longUnit_hours(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(base.plus(amount, HOURS), expected);
    }

    @Test(dataProvider="plusTime")
    public void test_plus_longUnit_minutes(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(base.plus(amount * 60, MINUTES), expected);
    }

    @Test(dataProvider="plusTime")
    public void test_plus_longUnit_seconds(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(base.plus(amount * 3600, SECONDS), expected);
    }

    @Test(dataProvider="plusTime")
    public void test_plus_longUnit_nanos(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(base.plus(amount * 3600_000_000_000L, NANOS), expected);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_plus_longUnit_null() {
        TEST_DATE_TIME_PARIS.plus(0, null);
    }

    //-----------------------------------------------------------------------
    // plusYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusYears() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.plusYears(1);
        assertEquals(test, ZonedDateTime.of(ldt.plusYears(1), ZONE_0100));
    }

    @Test
    public void test_plusYears_zero() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.plusYears(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // plusMonths()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusMonths() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.plusMonths(1);
        assertEquals(test, ZonedDateTime.of(ldt.plusMonths(1), ZONE_0100));
    }

    @Test
    public void test_plusMonths_zero() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.plusMonths(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // plusWeeks()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusWeeks() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.plusWeeks(1);
        assertEquals(test, ZonedDateTime.of(ldt.plusWeeks(1), ZONE_0100));
    }

    @Test
    public void test_plusWeeks_zero() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.plusWeeks(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // plusDays()
    //-----------------------------------------------------------------------
    @Test(dataProvider="plusDays")
    public void test_plusDays(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(base.plusDays(amount), expected);
    }

    //-----------------------------------------------------------------------
    // plusHours()
    //-----------------------------------------------------------------------
    @Test(dataProvider="plusTime")
    public void test_plusHours(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(base.plusHours(amount), expected);
    }

    //-----------------------------------------------------------------------
    // plusMinutes()
    //-----------------------------------------------------------------------
    @Test(dataProvider="plusTime")
    public void test_plusMinutes(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(base.plusMinutes(amount * 60), expected);
    }

    @Test
    public void test_plusMinutes_minutes() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.plusMinutes(30);
        assertEquals(test, ZonedDateTime.of(ldt.plusMinutes(30), ZONE_0100));
    }

    //-----------------------------------------------------------------------
    // plusSeconds()
    //-----------------------------------------------------------------------
    @Test(dataProvider="plusTime")
    public void test_plusSeconds(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(base.plusSeconds(amount * 3600), expected);
    }

    @Test
    public void test_plusSeconds_seconds() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.plusSeconds(1);
        assertEquals(test, ZonedDateTime.of(ldt.plusSeconds(1), ZONE_0100));
    }

    //-----------------------------------------------------------------------
    // plusNanos()
    //-----------------------------------------------------------------------
    @Test(dataProvider="plusTime")
    public void test_plusNanos(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(base.plusNanos(amount * 3600_000_000_000L), expected);
    }

    @Test
    public void test_plusNanos_nanos() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.plusNanos(1);
        assertEquals(test, ZonedDateTime.of(ldt.plusNanos(1), ZONE_0100));
    }

    //-----------------------------------------------------------------------
    // minus(TemporalAmount)
    //-----------------------------------------------------------------------
    @Test(dataProvider="plusDays")
    public void test_minus_TemporalAmount_Period_days(ZonedDateTime base, int amount, ZonedDateTime expected) {
        assertEquals(base.minus(Period.ofDays(-amount)), expected);
    }

    @Test(dataProvider="plusTime")
    public void test_minus_TemporalAmount_Period_hours(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(base.minus(MockSimplePeriod.of(-amount, HOURS)), expected);
    }

    @Test(dataProvider="plusTime")
    public void test_minus_TemporalAmount_Duration_hours(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(base.minus(Duration.ofHours(-amount)), expected);
    }

    @Test
    public void test_minus_TemporalAmount() {
        MockSimplePeriod period = MockSimplePeriod.of(7, ChronoUnit.MONTHS);
        ZonedDateTime t = ZonedDateTime.of(LocalDateTime.of(2008, 6, 1, 12, 30, 59, 500), ZONE_0100);
        ZonedDateTime expected = ZonedDateTime.of(LocalDateTime.of(2007, 11, 1, 12, 30, 59, 500), ZONE_0100);
        assertEquals(t.minus(period), expected);
    }

    @Test
    public void test_minus_TemporalAmount_Duration() {
        Duration duration = Duration.ofSeconds(4L * 60 * 60 + 5L * 60 + 6L);
        ZonedDateTime t = ZonedDateTime.of(LocalDateTime.of(2008, 6, 1, 12, 30, 59, 500), ZONE_0100);
        ZonedDateTime expected = ZonedDateTime.of(LocalDateTime.of(2008, 6, 1, 8, 25, 53, 500), ZONE_0100);
        assertEquals(t.minus(duration), expected);
    }

    @Test
    public void test_minus_TemporalAmount_Period_zero() {
        ZonedDateTime t = TEST_DATE_TIME.minus(MockSimplePeriod.ZERO_DAYS);
        assertEquals(t, TEST_DATE_TIME);
    }

    @Test
    public void test_minus_TemporalAmount_Duration_zero() {
        ZonedDateTime t = TEST_DATE_TIME.minus(Duration.ZERO);
        assertEquals(t, TEST_DATE_TIME);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_minus_TemporalAmount_null() {
        TEST_DATE_TIME.minus((TemporalAmount) null);
    }

    //-----------------------------------------------------------------------
    // minusYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusYears() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.minusYears(1);
        assertEquals(test, ZonedDateTime.of(ldt.minusYears(1), ZONE_0100));
    }

    @Test
    public void test_minusYears_zero() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.minusYears(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // minusMonths()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusMonths() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.minusMonths(1);
        assertEquals(test, ZonedDateTime.of(ldt.minusMonths(1), ZONE_0100));
    }

    @Test
    public void test_minusMonths_zero() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.minusMonths(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // minusWeeks()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusWeeks() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.minusWeeks(1);
        assertEquals(test, ZonedDateTime.of(ldt.minusWeeks(1), ZONE_0100));
    }

    @Test
    public void test_minusWeeks_zero() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.minusWeeks(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // minusDays()
    //-----------------------------------------------------------------------
    @Test(dataProvider="plusDays")
    public void test_minusDays(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(base.minusDays(-amount), expected);
    }

    //-----------------------------------------------------------------------
    // minusHours()
    //-----------------------------------------------------------------------
    @Test(dataProvider="plusTime")
    public void test_minusHours(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(base.minusHours(-amount), expected);
    }

    //-----------------------------------------------------------------------
    // minusMinutes()
    //-----------------------------------------------------------------------
    @Test(dataProvider="plusTime")
    public void test_minusMinutes(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(base.minusMinutes(-amount * 60), expected);
    }

    @Test
    public void test_minusMinutes_minutes() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.minusMinutes(30);
        assertEquals(test, ZonedDateTime.of(ldt.minusMinutes(30), ZONE_0100));
    }

    //-----------------------------------------------------------------------
    // minusSeconds()
    //-----------------------------------------------------------------------
    @Test(dataProvider="plusTime")
    public void test_minusSeconds(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(base.minusSeconds(-amount * 3600), expected);
    }

    @Test
    public void test_minusSeconds_seconds() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.minusSeconds(1);
        assertEquals(test, ZonedDateTime.of(ldt.minusSeconds(1), ZONE_0100));
    }

    //-----------------------------------------------------------------------
    // minusNanos()
    //-----------------------------------------------------------------------
    @Test(dataProvider="plusTime")
    public void test_minusNanos(ZonedDateTime base, long amount, ZonedDateTime expected) {
        assertEquals(base.minusNanos(-amount * 3600_000_000_000L), expected);
    }

    @Test
    public void test_minusNanos_nanos() {
        LocalDateTime ldt = LocalDateTime.of(2008, 6, 30, 23, 30, 59, 0);
        ZonedDateTime base = ZonedDateTime.of(ldt, ZONE_0100);
        ZonedDateTime test = base.minusNanos(1);
        assertEquals(test, ZonedDateTime.of(ldt.minusNanos(1), ZONE_0100));
    }

    //-----------------------------------------------------------------------
    // until(Temporal,TemporalUnit)
    //-----------------------------------------------------------------------
    // TODO: more tests for period between two different zones
    // compare results to OffsetDateTime.until, especially wrt dates

    @Test(dataProvider="plusDays")
    public void test_until_days(ZonedDateTime base, long expected, ZonedDateTime end) {
        if (base.toLocalTime().equals(end.toLocalTime()) == false) {
            return;  // avoid DST gap input values
        }
        assertEquals(base.until(end, DAYS), expected);
    }

    @Test(dataProvider="plusTime")
    public void test_until_hours(ZonedDateTime base, long expected, ZonedDateTime end) {
        assertEquals(base.until(end, HOURS), expected);
    }

    @Test(dataProvider="plusTime")
    public void test_until_minutes(ZonedDateTime base, long expected, ZonedDateTime end) {
        assertEquals(base.until(end, MINUTES), expected * 60);
    }

    @Test(dataProvider="plusTime")
    public void test_until_seconds(ZonedDateTime base, long expected, ZonedDateTime end) {
        assertEquals(base.until(end, SECONDS), expected * 3600);
    }

    @Test(dataProvider="plusTime")
    public void test_until_nanos(ZonedDateTime base, long expected, ZonedDateTime end) {
        assertEquals(base.until(end, NANOS), expected * 3600_000_000_000L);
    }

    @Test
    public void test_until_parisLondon() {
        ZonedDateTime midnightLondon = LocalDate.of(2012, 6, 28).atStartOfDay(ZONE_LONDON);
        ZonedDateTime midnightParis1 = LocalDate.of(2012, 6, 29).atStartOfDay(ZONE_PARIS);
        ZonedDateTime oneAm1 = LocalDateTime.of(2012, 6, 29, 1, 0).atZone(ZONE_PARIS);
        ZonedDateTime midnightParis2 = LocalDate.of(2012, 6, 30).atStartOfDay(ZONE_PARIS);

        assertEquals(midnightLondon.until(midnightParis1, HOURS), 23);
        assertEquals(midnightLondon.until(oneAm1, HOURS), 24);
        assertEquals(midnightLondon.until(midnightParis2, HOURS), 23 + 24);

        assertEquals(midnightLondon.until(midnightParis1, DAYS), 0);
        assertEquals(midnightLondon.until(oneAm1, DAYS), 1);
        assertEquals(midnightLondon.until(midnightParis2, DAYS), 1);
    }

    @Test
    public void test_until_gap() {
        ZonedDateTime before = TEST_PARIS_GAP_2008_03_30_02_30.withHour(0).withMinute(0).atZone(ZONE_PARIS);
        ZonedDateTime after = TEST_PARIS_GAP_2008_03_30_02_30.withHour(0).withMinute(0).plusDays(1).atZone(ZONE_PARIS);

        assertEquals(before.until(after, HOURS), 23);
        assertEquals(before.until(after, DAYS), 1);
    }

    @Test
    public void test_until_overlap() {
        ZonedDateTime before = TEST_PARIS_OVERLAP_2008_10_26_02_30.withHour(0).withMinute(0).atZone(ZONE_PARIS);
        ZonedDateTime after = TEST_PARIS_OVERLAP_2008_10_26_02_30.withHour(0).withMinute(0).plusDays(1).atZone(ZONE_PARIS);

        assertEquals(before.until(after, HOURS), 25);
        assertEquals(before.until(after, DAYS), 1);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_until_differentType() {
        TEST_DATE_TIME_PARIS.until(TEST_LOCAL_2008_06_30_11_30_59_500, DAYS);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_until_nullTemporal() {
        TEST_DATE_TIME_PARIS.until(null, DAYS);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_until_nullUnit() {
        TEST_DATE_TIME_PARIS.until(TEST_DATE_TIME_PARIS, null);
    }

    //-----------------------------------------------------------------------
    // format(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void test_format_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("y M d H m s");
        String t = ZonedDateTime.of(dateTime(2010, 12, 3, 11, 30), ZONE_PARIS).format(f);
        assertEquals(t, "2010 12 3 11 30 0");
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_format_formatter_null() {
        ZonedDateTime.of(dateTime(2010, 12, 3, 11, 30), ZONE_PARIS).format(null);
    }

    //-----------------------------------------------------------------------
    // toOffsetDateTime()
    //-----------------------------------------------------------------------
    @Test
    public void test_toOffsetDateTime() {
        assertEquals(TEST_DATE_TIME.toOffsetDateTime(), OffsetDateTime.of(TEST_DATE_TIME.toLocalDateTime(), TEST_DATE_TIME.getOffset()));
    }

    //-----------------------------------------------------------------------
    // toInstant()
    //-----------------------------------------------------------------------
    @DataProvider(name="toInstant")
    Object[][] data_toInstant() {
        return new Object[][] {
            {LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0), 0L, 0},
            {LocalDateTime.of(1970, 1, 1, 0, 0, 0, 1), 0L, 1},
            {LocalDateTime.of(1970, 1, 1, 0, 0, 0, 999_999_999), 0L, 999_999_999},
            {LocalDateTime.of(1970, 1, 1, 0, 0, 1, 0), 1L, 0},
            {LocalDateTime.of(1970, 1, 1, 0, 0, 1, 1), 1L, 1},
            {LocalDateTime.of(1969, 12, 31, 23, 59, 59, 999999999), -1L, 999_999_999},
            {LocalDateTime.of(1970, 1, 2, 0, 0), 24L * 60L * 60L, 0},
            {LocalDateTime.of(1969, 12, 31, 0, 0), -24L * 60L * 60L, 0},
        };
    }

    @Test(dataProvider="toInstant")
    public void test_toInstant_UTC(LocalDateTime ldt, long expectedEpSec, int expectedNos) {
        ZonedDateTime dt = ldt.atZone(ZoneOffset.UTC);
        Instant test = dt.toInstant();
        assertEquals(test.getEpochSecond(), expectedEpSec);
        assertEquals(test.getNano(), expectedNos);
    }

    @Test(dataProvider="toInstant")
    public void test_toInstant_P0100(LocalDateTime ldt, long expectedEpSec, int expectedNos) {
        ZonedDateTime dt = ldt.atZone(ZONE_0100);
        Instant test = dt.toInstant();
        assertEquals(test.getEpochSecond(), expectedEpSec - 3600);
        assertEquals(test.getNano(), expectedNos);
    }

    @Test(dataProvider="toInstant")
    public void test_toInstant_M0100(LocalDateTime ldt, long expectedEpSec, int expectedNos) {
        ZonedDateTime dt = ldt.atZone(ZONE_M0100);
        Instant test = dt.toInstant();
        assertEquals(test.getEpochSecond(), expectedEpSec + 3600);
        assertEquals(test.getNano(), expectedNos);
    }

    //-----------------------------------------------------------------------
    // toEpochSecond()
    //-----------------------------------------------------------------------
    @Test
    public void test_toEpochSecond_afterEpoch() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0).plusHours(1);
        for (int i = 0; i < 100000; i++) {
            ZonedDateTime a = ZonedDateTime.of(ldt, ZONE_PARIS);
            assertEquals(a.toEpochSecond(), i);
            ldt = ldt.plusSeconds(1);
        }
    }

    @Test
    public void test_toEpochSecond_beforeEpoch() {
        LocalDateTime ldt = LocalDateTime.of(1970, 1, 1, 0, 0).plusHours(1);
        for (int i = 0; i < 100000; i++) {
            ZonedDateTime a = ZonedDateTime.of(ldt, ZONE_PARIS);
            assertEquals(a.toEpochSecond(), -i);
            ldt = ldt.minusSeconds(1);
        }
    }

    @Test(dataProvider="toInstant")
    public void test_toEpochSecond_UTC(LocalDateTime ldt, long expectedEpSec, int expectedNos) {
        ZonedDateTime dt = ldt.atZone(ZoneOffset.UTC);
        assertEquals(dt.toEpochSecond(), expectedEpSec);
    }

    @Test(dataProvider="toInstant")
    public void test_toEpochSecond_P0100(LocalDateTime ldt, long expectedEpSec, int expectedNos) {
        ZonedDateTime dt = ldt.atZone(ZONE_0100);
        assertEquals(dt.toEpochSecond(), expectedEpSec - 3600);
    }

    @Test(dataProvider="toInstant")
    public void test_toEpochSecond_M0100(LocalDateTime ldt, long expectedEpSec, int expectedNos) {
        ZonedDateTime dt = ldt.atZone(ZONE_M0100);
        assertEquals(dt.toEpochSecond(), expectedEpSec + 3600);
    }

    //-----------------------------------------------------------------------
    // compareTo()
    //-----------------------------------------------------------------------
    @Test
    public void test_compareTo_time1() {
        ZonedDateTime a = ZonedDateTime.of(2008, 6, 30, 11, 30, 39, 0, ZONE_0100);
        ZonedDateTime b = ZonedDateTime.of(2008, 6, 30, 11, 30, 41, 0, ZONE_0100);  // a is before b due to time
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
    }

    @Test
    public void test_compareTo_time2() {
        ZonedDateTime a = ZonedDateTime.of(2008, 6, 30, 11, 30, 40, 4, ZONE_0100);
        ZonedDateTime b = ZonedDateTime.of(2008, 6, 30, 11, 30, 40, 5, ZONE_0100);  // a is before b due to time
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
    }

    @Test
    public void test_compareTo_offset1() {
        ZonedDateTime a = ZonedDateTime.of(2008, 6, 30, 11, 30, 41, 0, ZONE_0200);
        ZonedDateTime b = ZonedDateTime.of(2008, 6, 30, 11, 30, 39, 0, ZONE_0100);  // a is before b due to offset
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
    }

    @Test
    public void test_compareTo_offset2() {
        ZonedDateTime a = ZonedDateTime.of(2008, 6, 30, 11, 30, 40, 5, ZoneId.of("UTC+01:01"));
        ZonedDateTime b = ZonedDateTime.of(2008, 6, 30, 11, 30, 40, 4, ZONE_0100);  // a is before b due to offset
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
    }

    @Test
    public void test_compareTo_both() {
        ZonedDateTime a = ZonedDateTime.of(2008, 6, 30, 11, 50, 0, 0, ZONE_0200);
        ZonedDateTime b = ZonedDateTime.of(2008, 6, 30, 11, 20, 0, 0, ZONE_0100);  // a is before b on instant scale
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
    }

    @Test
    public void test_compareTo_bothNanos() {
        ZonedDateTime a = ZonedDateTime.of(2008, 6, 30, 11, 20, 40, 5, ZONE_0200);
        ZonedDateTime b = ZonedDateTime.of(2008, 6, 30, 10, 20, 40, 6, ZONE_0100);  // a is before b on instant scale
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
    }

    @Test
    public void test_compareTo_hourDifference() {
        ZonedDateTime a = ZonedDateTime.of(2008, 6, 30, 10, 0, 0, 0, ZONE_0100);
        ZonedDateTime b = ZonedDateTime.of(2008, 6, 30, 11, 0, 0, 0, ZONE_0200);  // a is before b despite being same time-line time
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_compareTo_null() {
        ZonedDateTime a = ZonedDateTime.of(2008, 6, 30, 23, 30, 59, 0, ZONE_0100);
        a.compareTo(null);
    }

    //-----------------------------------------------------------------------
    // isBefore()
    //-----------------------------------------------------------------------
    @DataProvider(name="IsBefore")
    Object[][] data_isBefore() {
        return new Object[][] {
            {11, 30, ZONE_0100, 11, 31, ZONE_0100, true}, // a is before b due to time
            {11, 30, ZONE_0200, 11, 30, ZONE_0100, true}, // a is before b due to offset
            {11, 30, ZONE_0200, 10, 30, ZONE_0100, false}, // a is equal b due to same instant
        };
    }

    @Test(dataProvider="IsBefore")
    public void test_isBefore(int hour1, int minute1, ZoneId zone1, int hour2, int minute2, ZoneId zone2, boolean expected) {
        ZonedDateTime a = ZonedDateTime.of(2008, 6, 30, hour1, minute1, 0, 0, zone1);
        ZonedDateTime b = ZonedDateTime.of(2008, 6, 30, hour2, minute2, 0, 0, zone2);
        assertEquals(a.isBefore(b), expected);
        assertEquals(b.isBefore(a), false);
        assertEquals(a.isBefore(a), false);
        assertEquals(b.isBefore(b), false);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_isBefore_null() {
        ZonedDateTime a = ZonedDateTime.of(2008, 6, 30, 23, 30, 59, 0, ZONE_0100);
        a.isBefore(null);
    }

    //-----------------------------------------------------------------------
    // isAfter()
    //-----------------------------------------------------------------------
    @DataProvider(name="IsAfter")
    Object[][] data_isAfter() {
        return new Object[][] {
            {11, 31, ZONE_0100, 11, 30, ZONE_0100, true}, // a is after b due to time
            {11, 30, ZONE_0100, 11, 30, ZONE_0200, true}, // a is after b due to offset
            {11, 30, ZONE_0200, 10, 30, ZONE_0100, false}, // a is equal b due to same instant
        };
    }

    @Test(dataProvider="IsAfter")
    public void test_isAfter(int hour1, int minute1, ZoneId zone1, int hour2, int minute2, ZoneId zone2, boolean expected) {
        ZonedDateTime a = ZonedDateTime.of(2008, 6, 30, hour1, minute1, 0, 0, zone1);
        ZonedDateTime b = ZonedDateTime.of(2008, 6, 30, hour2, minute2, 0, 0, zone2);
        assertEquals(a.isAfter(b), expected);
        assertEquals(b.isAfter(a), false);
        assertEquals(a.isAfter(a), false);
        assertEquals(b.isAfter(b), false);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_isAfter_null() {
        ZonedDateTime a = ZonedDateTime.of(2008, 6, 30, 23, 30, 59, 0, ZONE_0100);
        a.isAfter(null);
    }

    //-----------------------------------------------------------------------
    // equals() / hashCode()
    //-----------------------------------------------------------------------
    @Test(dataProvider="sampleTimes")
    public void test_equals_true(int y, int o, int d, int h, int m, int s, int n, ZoneId ignored) {
        ZonedDateTime a = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
        ZonedDateTime b = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
        assertEquals(a.equals(b), true);
        assertEquals(a.hashCode() == b.hashCode(), true);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_year_differs(int y, int o, int d, int h, int m, int s, int n, ZoneId ignored) {
        ZonedDateTime a = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
        ZonedDateTime b = ZonedDateTime.of(dateTime(y + 1, o, d, h, m, s, n), ZONE_0100);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_hour_differs(int y, int o, int d, int h, int m, int s, int n, ZoneId ignored) {
        h = (h == 23 ? 22 : h);
        ZonedDateTime a = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
        ZonedDateTime b = ZonedDateTime.of(dateTime(y, o, d, h + 1, m, s, n), ZONE_0100);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_minute_differs(int y, int o, int d, int h, int m, int s, int n, ZoneId ignored) {
        m = (m == 59 ? 58 : m);
        ZonedDateTime a = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
        ZonedDateTime b = ZonedDateTime.of(dateTime(y, o, d, h, m + 1, s, n), ZONE_0100);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_second_differs(int y, int o, int d, int h, int m, int s, int n, ZoneId ignored) {
        s = (s == 59 ? 58 : s);
        ZonedDateTime a = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
        ZonedDateTime b = ZonedDateTime.of(dateTime(y, o, d, h, m, s + 1, n), ZONE_0100);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_nano_differs(int y, int o, int d, int h, int m, int s, int n, ZoneId ignored) {
        n = (n == 999999999 ? 999999998 : n);
        ZonedDateTime a = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
        ZonedDateTime b = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n + 1), ZONE_0100);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_offset_differs(int y, int o, int d, int h, int m, int s, int n, ZoneId ignored) {
        ZonedDateTime a = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0100);
        ZonedDateTime b = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZONE_0200);
        assertEquals(a.equals(b), false);
    }

    @Test
    public void test_equals_itself_true() {
        assertEquals(TEST_DATE_TIME.equals(TEST_DATE_TIME), true);
    }

    @Test
    public void test_equals_string_false() {
        assertEquals(TEST_DATE_TIME.equals("2007-07-15"), false);
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @DataProvider(name="sampleToString")
    Object[][] provider_sampleToString() {
        return new Object[][] {
            {2008, 6, 30, 11, 30, 59, 0, "Z", "2008-06-30T11:30:59Z"},
            {2008, 6, 30, 11, 30, 59, 0, "+01:00", "2008-06-30T11:30:59+01:00"},
            {2008, 6, 30, 11, 30, 59, 999000000, "Z", "2008-06-30T11:30:59.999Z"},
            {2008, 6, 30, 11, 30, 59, 999000000, "+01:00", "2008-06-30T11:30:59.999+01:00"},
            {2008, 6, 30, 11, 30, 59, 999000, "Z", "2008-06-30T11:30:59.000999Z"},
            {2008, 6, 30, 11, 30, 59, 999000, "+01:00", "2008-06-30T11:30:59.000999+01:00"},
            {2008, 6, 30, 11, 30, 59, 999, "Z", "2008-06-30T11:30:59.000000999Z"},
            {2008, 6, 30, 11, 30, 59, 999, "+01:00", "2008-06-30T11:30:59.000000999+01:00"},

            {2008, 6, 30, 11, 30, 59, 999, "Europe/London", "2008-06-30T11:30:59.000000999+01:00[Europe/London]"},
            {2008, 6, 30, 11, 30, 59, 999, "Europe/Paris", "2008-06-30T11:30:59.000000999+02:00[Europe/Paris]"},
        };
    }

    @Test(dataProvider="sampleToString")
    public void test_toString(int y, int o, int d, int h, int m, int s, int n, String zoneId, String expected) {
        ZonedDateTime t = ZonedDateTime.of(dateTime(y, o, d, h, m, s, n), ZoneId.of(zoneId));
        String str = t.toString();
        assertEquals(str, expected);
    }

    //-------------------------------------------------------------------------
    private static LocalDateTime dateTime(
            int year, int month, int dayOfMonth,
            int hour, int minute) {
        return LocalDateTime.of(year, month, dayOfMonth, hour, minute);
    }

    private static LocalDateTime dateTime(
                    int year, int month, int dayOfMonth,
                    int hour, int minute, int second, int nanoOfSecond) {
                return LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond);
            }

    private static ZonedDateTime dateTime(
            int year, int month, int dayOfMonth,
            int hour, int minute, int second, int nanoOfSecond, ZoneOffset offset, ZoneId zoneId) {
        return ZonedDateTime.ofStrict(LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond), offset, zoneId);
    }

}
