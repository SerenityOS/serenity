/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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

import static java.time.temporal.ChronoField.AMPM_OF_DAY;
import static java.time.temporal.ChronoField.CLOCK_HOUR_OF_AMPM;
import static java.time.temporal.ChronoField.CLOCK_HOUR_OF_DAY;
import static java.time.temporal.ChronoField.HOUR_OF_AMPM;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MICRO_OF_DAY;
import static java.time.temporal.ChronoField.MICRO_OF_SECOND;
import static java.time.temporal.ChronoField.MILLI_OF_DAY;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_DAY;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.OFFSET_SECONDS;
import static java.time.temporal.ChronoField.SECOND_OF_DAY;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.MICROS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.HALF_DAYS;
import static java.time.temporal.ChronoUnit.NANOS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.JulianFields;
import java.time.temporal.Temporal;
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
import test.java.time.MockSimplePeriod;

/**
 * Test OffsetTime.
 */
@Test
public class TCKOffsetTime extends AbstractDateTimeTest {

    private static final ZoneId ZONE_GAZA = ZoneId.of("Asia/Gaza");
    private static final ZoneOffset OFFSET_PONE = ZoneOffset.ofHours(1);
    private static final ZoneOffset OFFSET_PTWO = ZoneOffset.ofHours(2);
    private static final ZoneOffset OFFSET_MTWO = ZoneOffset.ofHours(-2);
    private static final LocalDate DATE = LocalDate.of(2008, 12, 3);
    private OffsetTime TEST_11_30_59_500_PONE;

    @BeforeMethod
    public void setUp() {
        TEST_11_30_59_500_PONE = OffsetTime.of(11, 30, 59, 500, OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Override
    protected List<TemporalAccessor> samples() {
        TemporalAccessor[] array = {TEST_11_30_59_500_PONE, OffsetTime.MIN, OffsetTime.MAX};
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
            OFFSET_SECONDS,
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
    // constants
    //-----------------------------------------------------------------------
    @Test
    public void constant_MIN() {
        check(OffsetTime.MIN, 0, 0, 0, 0, ZoneOffset.MAX);
    }

    @Test
    public void constant_MAX() {
        check(OffsetTime.MAX, 23, 59, 59, 999999999, ZoneOffset.MIN);
    }

    //-----------------------------------------------------------------------
    // now()
    //-----------------------------------------------------------------------
    @Test
    public void now() {
        final long DELTA = 20_000_000_000L;    // 20 seconds of nanos leeway
        ZonedDateTime nowDT = ZonedDateTime.now();

        OffsetTime expected = OffsetTime.now(Clock.systemDefaultZone());
        OffsetTime test = OffsetTime.now();
        long diff = Math.abs(test.toLocalTime().toNanoOfDay() - expected.toLocalTime().toNanoOfDay());
        if (diff >= DELTA) {
            // may be date change
            expected = OffsetTime.now(Clock.systemDefaultZone());
            test = OffsetTime.now();
            diff = Math.abs(test.toLocalTime().toNanoOfDay() - expected.toLocalTime().toNanoOfDay());
        }
        assertTrue(diff < DELTA);
        assertEquals(test.getOffset(), nowDT.getOffset());
    }

    //-----------------------------------------------------------------------
    // now(Clock)
    //-----------------------------------------------------------------------
    @Test
    public void now_Clock_allSecsInDay() {
        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            Instant instant = Instant.ofEpochSecond(i, 8);
            Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
            OffsetTime test = OffsetTime.now(clock);
            assertEquals(test.getHour(), (i / (60 * 60)) % 24);
            assertEquals(test.getMinute(), (i / 60) % 60);
            assertEquals(test.getSecond(), i % 60);
            assertEquals(test.getNano(), 8);
            assertEquals(test.getOffset(), ZoneOffset.UTC);
        }
    }

    @Test
    public void now_Clock_beforeEpoch() {
        for (int i =-1; i >= -(24 * 60 * 60); i--) {
            Instant instant = Instant.ofEpochSecond(i, 8);
            Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
            OffsetTime test = OffsetTime.now(clock);
            assertEquals(test.getHour(), ((i + 24 * 60 * 60) / (60 * 60)) % 24);
            assertEquals(test.getMinute(), ((i + 24 * 60 * 60) / 60) % 60);
            assertEquals(test.getSecond(), (i + 24 * 60 * 60) % 60);
            assertEquals(test.getNano(), 8);
            assertEquals(test.getOffset(), ZoneOffset.UTC);
        }
    }

    @Test
    public void now_Clock_offsets() {
        Instant base = LocalDateTime.of(1970, 1, 1, 12, 0).toInstant(ZoneOffset.UTC);
        for (int i = -9; i < 15; i++) {
            ZoneOffset offset = ZoneOffset.ofHours(i);
            Clock clock = Clock.fixed(base, offset);
            OffsetTime test = OffsetTime.now(clock);
            assertEquals(test.getHour(), (12 + i) % 24);
            assertEquals(test.getMinute(), 0);
            assertEquals(test.getSecond(), 0);
            assertEquals(test.getNano(), 0);
            assertEquals(test.getOffset(), offset);
        }
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void now_Clock_nullZoneId() {
        OffsetTime.now((ZoneId) null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void now_Clock_nullClock() {
        OffsetTime.now((Clock) null);
    }

    //-----------------------------------------------------------------------
    // factories
    //-----------------------------------------------------------------------
    private void check(OffsetTime test, int h, int m, int s, int n, ZoneOffset offset) {
        assertEquals(test.toLocalTime(), LocalTime.of(h, m, s, n));
        assertEquals(test.getOffset(), offset);

        assertEquals(test.getHour(), h);
        assertEquals(test.getMinute(), m);
        assertEquals(test.getSecond(), s);
        assertEquals(test.getNano(), n);

        assertEquals(test, test);
        assertEquals(test.hashCode(), test.hashCode());
        assertEquals(OffsetTime.of(LocalTime.of(h, m, s, n), offset), test);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_intsHMSN() {
        OffsetTime test = OffsetTime.of(11, 30, 10, 500, OFFSET_PONE);
        check(test, 11, 30, 10, 500, OFFSET_PONE);
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_LocalTimeZoneOffset() {
        LocalTime localTime = LocalTime.of(11, 30, 10, 500);
        OffsetTime test = OffsetTime.of(localTime, OFFSET_PONE);
        check(test, 11, 30, 10, 500, OFFSET_PONE);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_LocalTimeZoneOffset_nullTime() {
        OffsetTime.of((LocalTime) null, OFFSET_PONE);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_LocalTimeZoneOffset_nullOffset() {
        LocalTime localTime = LocalTime.of(11, 30, 10, 500);
        OffsetTime.of(localTime, (ZoneOffset) null);
    }

    //-----------------------------------------------------------------------
    // ofInstant()
    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void factory_ofInstant_nullInstant() {
        OffsetTime.ofInstant((Instant) null, ZoneOffset.UTC);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_ofInstant_nullOffset() {
        Instant instant = Instant.ofEpochSecond(0L);
        OffsetTime.ofInstant(instant, (ZoneOffset) null);
    }

    @Test
    public void factory_ofInstant_allSecsInDay() {
        for (int i = 0; i < (2 * 24 * 60 * 60); i++) {
            Instant instant = Instant.ofEpochSecond(i, 8);
            OffsetTime test = OffsetTime.ofInstant(instant, ZoneOffset.UTC);
            assertEquals(test.getHour(), (i / (60 * 60)) % 24);
            assertEquals(test.getMinute(), (i / 60) % 60);
            assertEquals(test.getSecond(), i % 60);
            assertEquals(test.getNano(), 8);
        }
    }

    @Test
    public void factory_ofInstant_beforeEpoch() {
        for (int i =-1; i >= -(24 * 60 * 60); i--) {
            Instant instant = Instant.ofEpochSecond(i, 8);
            OffsetTime test = OffsetTime.ofInstant(instant, ZoneOffset.UTC);
            assertEquals(test.getHour(), ((i + 24 * 60 * 60) / (60 * 60)) % 24);
            assertEquals(test.getMinute(), ((i + 24 * 60 * 60) / 60) % 60);
            assertEquals(test.getSecond(), (i + 24 * 60 * 60) % 60);
            assertEquals(test.getNano(), 8);
        }
    }

    //-----------------------------------------------------------------------
    @Test
    public void factory_ofInstant_maxYear() {
        OffsetTime test = OffsetTime.ofInstant(Instant.MAX, ZoneOffset.UTC);
        assertEquals(test.getHour(), 23);
        assertEquals(test.getMinute(), 59);
        assertEquals(test.getSecond(), 59);
        assertEquals(test.getNano(), 999_999_999);
    }

    @Test
    public void factory_ofInstant_minYear() {
        OffsetTime test = OffsetTime.ofInstant(Instant.MIN, ZoneOffset.UTC);
        assertEquals(test.getHour(), 0);
        assertEquals(test.getMinute(), 0);
        assertEquals(test.getSecond(), 0);
        assertEquals(test.getNano(), 0);
    }

    //-----------------------------------------------------------------------
    // from(TemporalAccessor)
    //-----------------------------------------------------------------------
    @Test
    public void factory_from_TemporalAccessor_OT() {
        assertEquals(OffsetTime.from(OffsetTime.of(17, 30, 0, 0, OFFSET_PONE)), OffsetTime.of(17, 30, 0, 0, OFFSET_PONE));
    }

    @Test
    public void test_from_TemporalAccessor_ZDT() {
        ZonedDateTime base = LocalDateTime.of(2007, 7, 15, 11, 30, 59, 500).atZone(OFFSET_PONE);
        assertEquals(OffsetTime.from(base), TEST_11_30_59_500_PONE);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void factory_from_TemporalAccessor_invalid_noDerive() {
        OffsetTime.from(LocalDate.of(2007, 7, 15));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_from_TemporalAccessor_null() {
        OffsetTime.from((TemporalAccessor) null);
    }

    //-----------------------------------------------------------------------
    // parse()
    //-----------------------------------------------------------------------
    @Test(dataProvider = "sampleToString")
    public void factory_parse_validText(int h, int m, int s, int n, String offsetId, String parsable) {
        OffsetTime t = OffsetTime.parse(parsable);
        assertNotNull(t, parsable);
        check(t, h, m, s, n, ZoneOffset.of(offsetId));
    }

    @DataProvider(name="sampleBadParse")
    Object[][] provider_sampleBadParse() {
        return new Object[][]{
                {"00;00"},
                {"12-00"},
                {"-01:00"},
                {"00:00:00-09"},
                {"00:00:00,09"},
                {"00:00:abs"},
                {"11"},
                {"11:30"},
                {"11:30+01:00[Europe/Paris]"},
        };
    }

    @Test(dataProvider = "sampleBadParse", expectedExceptions={DateTimeParseException.class})
    public void factory_parse_invalidText(String unparsable) {
        OffsetTime.parse(unparsable);
    }

    //-----------------------------------------------------------------------s
    @Test(expectedExceptions={DateTimeParseException.class})
    public void factory_parse_illegalHour() {
        OffsetTime.parse("25:00+01:00");
    }

    @Test(expectedExceptions={DateTimeParseException.class})
    public void factory_parse_illegalMinute() {
        OffsetTime.parse("12:60+01:00");
    }

    @Test(expectedExceptions={DateTimeParseException.class})
    public void factory_parse_illegalSecond() {
        OffsetTime.parse("12:12:60+01:00");
    }

    //-----------------------------------------------------------------------
    // parse(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void factory_parse_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("H m s XXX");
        OffsetTime test = OffsetTime.parse("11 30 0 +01:00", f);
        assertEquals(test, OffsetTime.of(11, 30, 0, 0, ZoneOffset.ofHours(1)));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_formatter_nullText() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("y M d H m s");
        OffsetTime.parse((String) null, f);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_formatter_nullFormatter() {
        OffsetTime.parse("ANY", null);
    }

    //-----------------------------------------------------------------------
    // constructor via factory
    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void constructor_nullTime() throws Throwable  {
        OffsetTime.of(null, OFFSET_PONE);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void constructor_nullOffset() throws Throwable  {
       OffsetTime.of(LocalTime.of(11, 30, 0, 0), null);
    }

    //-----------------------------------------------------------------------
    // basics
    //-----------------------------------------------------------------------
    @DataProvider(name="sampleTimes")
    Object[][] provider_sampleTimes() {
        return new Object[][] {
            {11, 30, 20, 500, OFFSET_PONE},
            {11, 0, 0, 0, OFFSET_PONE},
            {23, 59, 59, 999999999, OFFSET_PONE},
        };
    }

    @Test(dataProvider="sampleTimes")
    public void test_get(int h, int m, int s, int n, ZoneOffset offset) {
        LocalTime localTime = LocalTime.of(h, m, s, n);
        OffsetTime a = OffsetTime.of(localTime, offset);

        assertEquals(a.toLocalTime(), localTime);
        assertEquals(a.getOffset(), offset);
        assertEquals(a.toString(), localTime.toString() + offset.toString());
        assertEquals(a.getHour(), localTime.getHour());
        assertEquals(a.getMinute(), localTime.getMinute());
        assertEquals(a.getSecond(), localTime.getSecond());
        assertEquals(a.getNano(), localTime.getNano());
    }

    //-----------------------------------------------------------------------
    // isSupported(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_isSupported_TemporalField() {
        assertEquals(TEST_11_30_59_500_PONE.isSupported((TemporalField) null), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.NANO_OF_SECOND), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.NANO_OF_DAY), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.MICRO_OF_SECOND), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.MICRO_OF_DAY), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.MILLI_OF_SECOND), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.MILLI_OF_DAY), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.SECOND_OF_MINUTE), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.SECOND_OF_DAY), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.MINUTE_OF_HOUR), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.MINUTE_OF_DAY), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.HOUR_OF_AMPM), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.CLOCK_HOUR_OF_AMPM), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.HOUR_OF_DAY), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.CLOCK_HOUR_OF_DAY), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.AMPM_OF_DAY), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.DAY_OF_WEEK), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.DAY_OF_MONTH), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.DAY_OF_YEAR), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.EPOCH_DAY), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.ALIGNED_WEEK_OF_MONTH), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.ALIGNED_WEEK_OF_YEAR), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.MONTH_OF_YEAR), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.PROLEPTIC_MONTH), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.YEAR), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.YEAR_OF_ERA), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.ERA), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.INSTANT_SECONDS), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoField.OFFSET_SECONDS), true);
    }

    //-----------------------------------------------------------------------
    // isSupported(TemporalUnit)
    //-----------------------------------------------------------------------
    @Test
    public void test_isSupported_TemporalUnit() {
        assertEquals(TEST_11_30_59_500_PONE.isSupported((TemporalUnit) null), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoUnit.NANOS), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoUnit.MICROS), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoUnit.MILLIS), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoUnit.SECONDS), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoUnit.MINUTES), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoUnit.HOURS), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoUnit.HALF_DAYS), true);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoUnit.DAYS), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoUnit.WEEKS), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoUnit.MONTHS), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoUnit.YEARS), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoUnit.DECADES), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoUnit.CENTURIES), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoUnit.MILLENNIA), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoUnit.ERAS), false);
        assertEquals(TEST_11_30_59_500_PONE.isSupported(ChronoUnit.FOREVER), false);
    }

    //-----------------------------------------------------------------------
    // get(TemporalField)
    //-----------------------------------------------------------------------
    @Test
    public void test_get_TemporalField() {
        OffsetTime test = OffsetTime.of(12, 30, 40, 987654321, OFFSET_PONE);
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
        OffsetTime test = OffsetTime.of(12, 30, 40, 987654321, OFFSET_PONE);
        assertEquals(test.getLong(ChronoField.HOUR_OF_DAY), 12);
        assertEquals(test.getLong(ChronoField.MINUTE_OF_HOUR), 30);
        assertEquals(test.getLong(ChronoField.SECOND_OF_MINUTE), 40);
        assertEquals(test.getLong(ChronoField.NANO_OF_SECOND), 987654321);
        assertEquals(test.getLong(ChronoField.HOUR_OF_AMPM), 0);
        assertEquals(test.getLong(ChronoField.AMPM_OF_DAY), 1);

        assertEquals(test.getLong(ChronoField.OFFSET_SECONDS), 3600);
    }

    //-----------------------------------------------------------------------
    // query(TemporalQuery)
    //-----------------------------------------------------------------------
    @DataProvider(name="query")
    Object[][] data_query() {
        return new Object[][] {
                {TEST_11_30_59_500_PONE, TemporalQueries.chronology(), null},
                {TEST_11_30_59_500_PONE, TemporalQueries.zoneId(), null},
                {TEST_11_30_59_500_PONE, TemporalQueries.precision(), ChronoUnit.NANOS},
                {TEST_11_30_59_500_PONE, TemporalQueries.zone(), OFFSET_PONE},
                {TEST_11_30_59_500_PONE, TemporalQueries.offset(), OFFSET_PONE},
                {TEST_11_30_59_500_PONE, TemporalQueries.localDate(), null},
                {TEST_11_30_59_500_PONE, TemporalQueries.localTime(), LocalTime.of(11, 30, 59, 500)},
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
        TEST_11_30_59_500_PONE.query(null);
    }

    //-----------------------------------------------------------------------
    // withOffsetSameLocal()
    //-----------------------------------------------------------------------
    @Test
    public void test_withOffsetSameLocal() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.withOffsetSameLocal(OFFSET_PTWO);
        assertEquals(test.toLocalTime(), base.toLocalTime());
        assertEquals(test.getOffset(), OFFSET_PTWO);
    }

    @Test
    public void test_withOffsetSameLocal_noChange() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.withOffsetSameLocal(OFFSET_PONE);
        assertEquals(test, base);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_withOffsetSameLocal_null() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        base.withOffsetSameLocal(null);
    }

    //-----------------------------------------------------------------------
    // withOffsetSameInstant()
    //-----------------------------------------------------------------------
    @Test
    public void test_withOffsetSameInstant() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.withOffsetSameInstant(OFFSET_PTWO);
        OffsetTime expected = OffsetTime.of(12, 30, 59, 0, OFFSET_PTWO);
        assertEquals(test, expected);
    }

    @Test
    public void test_withOffsetSameInstant_noChange() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.withOffsetSameInstant(OFFSET_PONE);
        assertEquals(test, base);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_withOffsetSameInstant_null() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        base.withOffsetSameInstant(null);
    }

    //-----------------------------------------------------------------------
    // adjustInto(Temporal)
    //-----------------------------------------------------------------------
    @DataProvider(name="adjustInto")
    Object[][] data_adjustInto() {
        return new Object[][]{
                {OffsetTime.of(LocalTime.of(23, 5), OFFSET_PONE), OffsetTime.of(LocalTime.of(1, 1, 1, 100), ZoneOffset.UTC), OffsetTime.of(LocalTime.of(23, 5), OFFSET_PONE), null},
                {OffsetTime.of(LocalTime.of(23, 5), OFFSET_PONE), OffsetTime.MAX, OffsetTime.of(LocalTime.of(23, 5), OFFSET_PONE), null},
                {OffsetTime.of(LocalTime.of(23, 5), OFFSET_PONE), OffsetTime.MIN, OffsetTime.of(LocalTime.of(23 , 5), OFFSET_PONE), null},
                {OffsetTime.MAX, OffsetTime.of(LocalTime.of(23, 5), OFFSET_PONE), OffsetTime.of(OffsetTime.MAX.toLocalTime(), ZoneOffset.ofHours(-18)), null},
                {OffsetTime.MIN, OffsetTime.of(LocalTime.of(23, 5), OFFSET_PONE), OffsetTime.of(OffsetTime.MIN.toLocalTime(), ZoneOffset.ofHours(18)), null},


                {OffsetTime.of(LocalTime.of(23, 5), OFFSET_PONE), ZonedDateTime.of(LocalDateTime.of(2012, 3, 4, 1, 1, 1, 100), ZONE_GAZA), ZonedDateTime.of(LocalDateTime.of(2012, 3, 4, 23, 5), ZONE_GAZA), null},
                {OffsetTime.of(LocalTime.of(23, 5), OFFSET_PONE), OffsetDateTime.of(LocalDateTime.of(2012, 3, 4, 1, 1, 1, 100), ZoneOffset.UTC), OffsetDateTime.of(LocalDateTime.of(2012, 3, 4, 23, 5), OFFSET_PONE), null},

                {OffsetTime.of(LocalTime.of(23, 5), OFFSET_PONE), LocalDateTime.of(2012, 3, 4, 1, 1, 1, 100), null, DateTimeException.class},
                {OffsetTime.of(LocalTime.of(23, 5), OFFSET_PONE), LocalDate.of(2210, 2, 2), null, DateTimeException.class},
                {OffsetTime.of(LocalTime.of(23, 5), OFFSET_PONE), LocalTime.of(22, 3, 0), null, DateTimeException.class},
                {OffsetTime.of(LocalTime.of(23, 5), OFFSET_PONE), null, null, NullPointerException.class},

        };
    }

    @Test(dataProvider="adjustInto")
    public void test_adjustInto(OffsetTime test, Temporal temporal, Temporal expected, Class<?> expectedEx) {
        if (expectedEx == null) {
            Temporal result = test.adjustInto(temporal);
            assertEquals(result, expected);
        } else {
            try {
                Temporal result = test.adjustInto(temporal);
                fail();
            } catch (Exception ex) {
                assertTrue(expectedEx.isInstance(ex));
            }
        }
    }

    //-----------------------------------------------------------------------
    // with(WithAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_with_adjustment() {
        final OffsetTime sample = OffsetTime.of(23, 5, 0, 0, OFFSET_PONE);
        TemporalAdjuster adjuster = new TemporalAdjuster() {
            @Override
            public Temporal adjustInto(Temporal dateTime) {
                return sample;
            }
        };
        assertEquals(TEST_11_30_59_500_PONE.with(adjuster), sample);
    }

    @Test
    public void test_with_adjustment_LocalTime() {
        OffsetTime test = TEST_11_30_59_500_PONE.with(LocalTime.of(13, 30));
        assertEquals(test, OffsetTime.of(13, 30, 0, 0, OFFSET_PONE));
    }

    @Test
    public void test_with_adjustment_OffsetTime() {
        OffsetTime test = TEST_11_30_59_500_PONE.with(OffsetTime.of(13, 35, 0, 0, OFFSET_PTWO));
        assertEquals(test, OffsetTime.of(13, 35, 0, 0, OFFSET_PTWO));
    }

    @Test
    public void test_with_adjustment_ZoneOffset() {
        OffsetTime test = TEST_11_30_59_500_PONE.with(OFFSET_PTWO);
        assertEquals(test, OffsetTime.of(11, 30, 59, 500, OFFSET_PTWO));
    }

    @Test
    public void test_with_adjustment_AmPm() {
        OffsetTime test = TEST_11_30_59_500_PONE.with(new TemporalAdjuster() {
            @Override
            public Temporal adjustInto(Temporal dateTime) {
                return dateTime.with(HOUR_OF_DAY, 23);
            }
        });
        assertEquals(test, OffsetTime.of(23, 30, 59, 500, OFFSET_PONE));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_with_adjustment_null() {
        TEST_11_30_59_500_PONE.with((TemporalAdjuster) null);
    }

    //-----------------------------------------------------------------------
    // with(TemporalField, long)
    //-----------------------------------------------------------------------
    @Test
    public void test_with_TemporalField() {
        OffsetTime test = OffsetTime.of(12, 30, 40, 987654321, OFFSET_PONE);
        assertEquals(test.with(ChronoField.HOUR_OF_DAY, 15), OffsetTime.of(15, 30, 40, 987654321, OFFSET_PONE));
        assertEquals(test.with(ChronoField.MINUTE_OF_HOUR, 50), OffsetTime.of(12, 50, 40, 987654321, OFFSET_PONE));
        assertEquals(test.with(ChronoField.SECOND_OF_MINUTE, 50), OffsetTime.of(12, 30, 50, 987654321, OFFSET_PONE));
        assertEquals(test.with(ChronoField.NANO_OF_SECOND, 12345), OffsetTime.of(12, 30, 40, 12345, OFFSET_PONE));
        assertEquals(test.with(ChronoField.HOUR_OF_AMPM, 6), OffsetTime.of(18, 30, 40, 987654321, OFFSET_PONE));
        assertEquals(test.with(ChronoField.AMPM_OF_DAY, 0), OffsetTime.of(0, 30, 40, 987654321, OFFSET_PONE));

        assertEquals(test.with(ChronoField.OFFSET_SECONDS, 7205), OffsetTime.of(12, 30, 40, 987654321, ZoneOffset.ofHoursMinutesSeconds(2, 0, 5)));
    }

    @Test(expectedExceptions=NullPointerException.class )
    public void test_with_TemporalField_null() {
        TEST_11_30_59_500_PONE.with((TemporalField) null, 0);
    }

    @Test(expectedExceptions=DateTimeException.class )
    public void test_with_TemporalField_invalidField() {
        TEST_11_30_59_500_PONE.with(ChronoField.YEAR, 0);
    }

    //-----------------------------------------------------------------------
    // withHour()
    //-----------------------------------------------------------------------
    @Test
    public void test_withHour_normal() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.withHour(15);
        assertEquals(test, OffsetTime.of(15, 30, 59, 0, OFFSET_PONE));
    }

    @Test
    public void test_withHour_noChange() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.withHour(11);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // withMinute()
    //-----------------------------------------------------------------------
    @Test
    public void test_withMinute_normal() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.withMinute(15);
        assertEquals(test, OffsetTime.of(11, 15, 59, 0, OFFSET_PONE));
    }

    @Test
    public void test_withMinute_noChange() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.withMinute(30);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // withSecond()
    //-----------------------------------------------------------------------
    @Test
    public void test_withSecond_normal() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.withSecond(15);
        assertEquals(test, OffsetTime.of(11, 30, 15, 0, OFFSET_PONE));
    }

    @Test
    public void test_withSecond_noChange() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.withSecond(59);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // withNano()
    //-----------------------------------------------------------------------
    @Test
    public void test_withNanoOfSecond_normal() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 1, OFFSET_PONE);
        OffsetTime test = base.withNano(15);
        assertEquals(test, OffsetTime.of(11, 30, 59, 15, OFFSET_PONE));
    }

    @Test
    public void test_withNanoOfSecond_noChange() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 1, OFFSET_PONE);
        OffsetTime test = base.withNano(1);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // truncatedTo(TemporalUnit)
    //-----------------------------------------------------------------------
    @Test
    public void test_truncatedTo_normal() {
        assertEquals(TEST_11_30_59_500_PONE.truncatedTo(NANOS), TEST_11_30_59_500_PONE);
        assertEquals(TEST_11_30_59_500_PONE.truncatedTo(SECONDS), TEST_11_30_59_500_PONE.withNano(0));
        assertEquals(TEST_11_30_59_500_PONE.truncatedTo(DAYS), TEST_11_30_59_500_PONE.with(LocalTime.MIDNIGHT));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_truncatedTo_null() {
        TEST_11_30_59_500_PONE.truncatedTo(null);
    }

    //-----------------------------------------------------------------------
    // plus(PlusAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_plus_PlusAdjuster() {
        MockSimplePeriod period = MockSimplePeriod.of(7, ChronoUnit.MINUTES);
        OffsetTime t = TEST_11_30_59_500_PONE.plus(period);
        assertEquals(t, OffsetTime.of(11, 37, 59, 500, OFFSET_PONE));
    }

    @Test
    public void test_plus_PlusAdjuster_noChange() {
        OffsetTime t = TEST_11_30_59_500_PONE.plus(MockSimplePeriod.of(0, SECONDS));
        assertEquals(t, TEST_11_30_59_500_PONE);
    }

    @Test
    public void test_plus_PlusAdjuster_zero() {
        OffsetTime t = TEST_11_30_59_500_PONE.plus(Period.ZERO);
        assertEquals(t, TEST_11_30_59_500_PONE);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_plus_PlusAdjuster_null() {
        TEST_11_30_59_500_PONE.plus((TemporalAmount) null);
    }

    //-----------------------------------------------------------------------
    // plusHours()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusHours() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.plusHours(13);
        assertEquals(test, OffsetTime.of(0, 30, 59, 0, OFFSET_PONE));
    }

    @Test
    public void test_plusHours_zero() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.plusHours(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // plusMinutes()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusMinutes() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.plusMinutes(30);
        assertEquals(test, OffsetTime.of(12, 0, 59, 0, OFFSET_PONE));
    }

    @Test
    public void test_plusMinutes_zero() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.plusMinutes(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // plusSeconds()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusSeconds() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.plusSeconds(1);
        assertEquals(test, OffsetTime.of(11, 31, 0, 0, OFFSET_PONE));
    }

    @Test
    public void test_plusSeconds_zero() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.plusSeconds(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // plusNanos()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusNanos() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.plusNanos(1);
        assertEquals(test, OffsetTime.of(11, 30, 59, 1, OFFSET_PONE));
    }

    @Test
    public void test_plusNanos_zero() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.plusNanos(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // minus(MinusAdjuster)
    //-----------------------------------------------------------------------
    @Test
    public void test_minus_MinusAdjuster() {
        MockSimplePeriod period = MockSimplePeriod.of(7, ChronoUnit.MINUTES);
        OffsetTime t = TEST_11_30_59_500_PONE.minus(period);
        assertEquals(t, OffsetTime.of(11, 23, 59, 500, OFFSET_PONE));
    }

    @Test
    public void test_minus_MinusAdjuster_noChange() {
        OffsetTime t = TEST_11_30_59_500_PONE.minus(MockSimplePeriod.of(0, SECONDS));
        assertEquals(t, TEST_11_30_59_500_PONE);
    }

    @Test
    public void test_minus_MinusAdjuster_zero() {
        OffsetTime t = TEST_11_30_59_500_PONE.minus(Period.ZERO);
        assertEquals(t, TEST_11_30_59_500_PONE);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_minus_MinusAdjuster_null() {
        TEST_11_30_59_500_PONE.minus((TemporalAmount) null);
    }

    //-----------------------------------------------------------------------
    // minusHours()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusHours() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.minusHours(-13);
        assertEquals(test, OffsetTime.of(0, 30, 59, 0, OFFSET_PONE));
    }

    @Test
    public void test_minusHours_zero() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.minusHours(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // minusMinutes()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusMinutes() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.minusMinutes(50);
        assertEquals(test, OffsetTime.of(10, 40, 59, 0, OFFSET_PONE));
    }

    @Test
    public void test_minusMinutes_zero() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.minusMinutes(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // minusSeconds()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusSeconds() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.minusSeconds(60);
        assertEquals(test, OffsetTime.of(11, 29, 59, 0, OFFSET_PONE));
    }

    @Test
    public void test_minusSeconds_zero() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.minusSeconds(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // minusNanos()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusNanos() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.minusNanos(1);
        assertEquals(test, OffsetTime.of(11, 30, 58, 999999999, OFFSET_PONE));
    }

    @Test
    public void test_minusNanos_zero() {
        OffsetTime base = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        OffsetTime test = base.minusNanos(0);
        assertEquals(test, base);
    }

    //-----------------------------------------------------------------------
    // until(Temporal, TemporalUnit)
    //-----------------------------------------------------------------------
    @DataProvider(name="periodUntilUnit")
    Object[][] data_untilUnit() {
        return new Object[][] {
                {OffsetTime.of(1, 1, 1, 0, OFFSET_PONE), OffsetTime.of(13, 1, 1, 0, OFFSET_PONE), HALF_DAYS, 1},
                {OffsetTime.of(1, 1, 1, 0, OFFSET_PONE), OffsetTime.of(2, 1, 1, 0, OFFSET_PONE), HOURS, 1},
                {OffsetTime.of(1, 1, 1, 0, OFFSET_PONE), OffsetTime.of(2, 1, 1, 0, OFFSET_PONE), MINUTES, 60},
                {OffsetTime.of(1, 1, 1, 0, OFFSET_PONE), OffsetTime.of(2, 1, 1, 0, OFFSET_PONE), SECONDS, 3600},
                {OffsetTime.of(1, 1, 1, 0, OFFSET_PONE), OffsetTime.of(2, 1, 1, 0, OFFSET_PONE), MILLIS, 3600*1000},
                {OffsetTime.of(1, 1, 1, 0, OFFSET_PONE), OffsetTime.of(2, 1, 1, 0, OFFSET_PONE), MICROS, 3600*1000*1000L},
                {OffsetTime.of(1, 1, 1, 0, OFFSET_PONE), OffsetTime.of(2, 1, 1, 0, OFFSET_PONE), NANOS, 3600*1000*1000L*1000},

                {OffsetTime.of(1, 1, 1, 0, OFFSET_PONE), OffsetTime.of(14, 1, 1, 0, OFFSET_PTWO), HALF_DAYS, 1},
                {OffsetTime.of(1, 1, 1, 0, OFFSET_PONE), OffsetTime.of(3, 1, 1, 0, OFFSET_PTWO), HOURS, 1},
                {OffsetTime.of(1, 1, 1, 0, OFFSET_PONE), OffsetTime.of(3, 1, 1, 0, OFFSET_PTWO), MINUTES, 60},
                {OffsetTime.of(1, 1, 1, 0, OFFSET_PONE), OffsetTime.of(3, 1, 1, 0, OFFSET_PTWO), SECONDS, 3600},
                {OffsetTime.of(1, 1, 1, 0, OFFSET_PONE), OffsetTime.of(3, 1, 1, 0, OFFSET_PTWO), MILLIS, 3600*1000},
                {OffsetTime.of(1, 1, 1, 0, OFFSET_PONE), OffsetTime.of(3, 1, 1, 0, OFFSET_PTWO), MICROS, 3600*1000*1000L},
                {OffsetTime.of(1, 1, 1, 0, OFFSET_PONE), OffsetTime.of(3, 1, 1, 0, OFFSET_PTWO), NANOS, 3600*1000*1000L*1000},
        };
    }

    @Test(dataProvider="periodUntilUnit")
    public void test_until_TemporalUnit(OffsetTime offsetTime1, OffsetTime offsetTime2, TemporalUnit unit, long expected) {
        long amount = offsetTime1.until(offsetTime2, unit);
        assertEquals(amount, expected);
    }

    @Test(dataProvider="periodUntilUnit")
    public void test_until_TemporalUnit_negated(OffsetTime offsetTime1, OffsetTime offsetTime2, TemporalUnit unit, long expected) {
        long amount = offsetTime2.until(offsetTime1, unit);
        assertEquals(amount, -expected);
    }

    @Test(dataProvider="periodUntilUnit")
    public void test_until_TemporalUnit_between(OffsetTime offsetTime1, OffsetTime offsetTime2, TemporalUnit unit, long expected) {
        long amount = unit.between(offsetTime1, offsetTime2);
        assertEquals(amount, expected);
    }

    @Test
    public void test_until_convertedType() {
        OffsetTime offsetTime = OffsetTime.of(1, 1, 1, 0, OFFSET_PONE);
        OffsetDateTime offsetDateTime = offsetTime.plusSeconds(3).atDate(LocalDate.of(1980, 2, 10));
        assertEquals(offsetTime.until(offsetDateTime, SECONDS), 3);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_until_invalidType() {
        OffsetTime offsetTime = OffsetTime.of(1, 1, 1, 0, OFFSET_PONE);
        offsetTime.until(LocalDate.of(1980, 2, 10), SECONDS);
    }

    @Test(expectedExceptions=DateTimeException.class)
    public void test_until_invalidTemporalUnit() {
        OffsetTime offsetTime1 = OffsetTime.of(1, 1, 1, 0, OFFSET_PONE);
        OffsetTime offsetTime2 = OffsetTime.of(2, 1, 1, 0, OFFSET_PONE);
        offsetTime1.until(offsetTime2, MONTHS);
    }

    //-----------------------------------------------------------------------
    // format(DateTimeFormatter)
    //-----------------------------------------------------------------------
    @Test
    public void test_format_formatter() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("H m s");
        String t = OffsetTime.of(11, 30, 0, 0, OFFSET_PONE).format(f);
        assertEquals(t, "11 30 0");
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_format_formatter_null() {
        OffsetTime.of(11, 30, 0, 0, OFFSET_PONE).format(null);
    }

    //-----------------------------------------------------------------------
    // toEpochSecond()
    //-----------------------------------------------------------------------
    @DataProvider(name="epochSecond")
    Object[][] provider_toEpochSecond() {
        return new Object[][] {
        {OffsetTime.of(0, 0, 0, 0, OFFSET_PTWO).toEpochSecond(LocalDate.of(1970, 1, 1)), -7200L},
        {OffsetTime.of(11, 30, 0, 0, OFFSET_MTWO).toEpochSecond(LocalDate.of(1995, 9, 27)), 812208600L},
        {OffsetTime.of(0, 0, 0, 0, OFFSET_PONE).toEpochSecond(LocalDate.of(1970, 1, 1)),
                Instant.ofEpochSecond(-3600).getEpochSecond()},
        {OffsetTime.of(11, 30, 0, 0, OFFSET_PTWO).toEpochSecond(LocalDate.of(1965, 12, 31)),
                Instant.ofEpochSecond(-126282600L).getEpochSecond()},
        {OffsetTime.of(11, 30, 0, 0, OFFSET_MTWO).toEpochSecond(LocalDate.of(1970, 1, 1)),
                OffsetDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.of(11, 30), OFFSET_MTWO)
                              .toEpochSecond()},
        };
    }

    @Test(dataProvider="epochSecond")
    public void test_toEpochSecond(long actual, long expected) {
        assertEquals(actual, expected);
    }

    //-----------------------------------------------------------------------
    // compareTo()
    //-----------------------------------------------------------------------
    @Test
    public void test_compareTo_time() {
        OffsetTime a = OffsetTime.of(11, 29, 0, 0, OFFSET_PONE);
        OffsetTime b = OffsetTime.of(11, 30, 0, 0, OFFSET_PONE);  // a is before b due to time
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(convertInstant(a).compareTo(convertInstant(b)) < 0, true);
    }

    @Test
    public void test_compareTo_offset() {
        OffsetTime a = OffsetTime.of(11, 30, 0, 0, OFFSET_PTWO);
        OffsetTime b = OffsetTime.of(11, 30, 0, 0, OFFSET_PONE);  // a is before b due to offset
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(convertInstant(a).compareTo(convertInstant(b)) < 0, true);
    }

    @Test
    public void test_compareTo_both() {
        OffsetTime a = OffsetTime.of(11, 50, 0, 0, OFFSET_PTWO);
        OffsetTime b = OffsetTime.of(11, 20, 0, 0, OFFSET_PONE);  // a is before b on instant scale
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(convertInstant(a).compareTo(convertInstant(b)) < 0, true);
    }

    @Test
    public void test_compareTo_bothNearStartOfDay() {
        OffsetTime a = OffsetTime.of(0, 10, 0, 0, OFFSET_PONE);
        OffsetTime b = OffsetTime.of(2, 30, 0, 0, OFFSET_PTWO);  // a is before b on instant scale
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(convertInstant(a).compareTo(convertInstant(b)) < 0, true);
    }

    @Test
    public void test_compareTo_hourDifference() {
        OffsetTime a = OffsetTime.of(10, 0, 0, 0, OFFSET_PONE);
        OffsetTime b = OffsetTime.of(11, 0, 0, 0, OFFSET_PTWO);  // a is before b despite being same time-line time
        assertEquals(a.compareTo(b) < 0, true);
        assertEquals(b.compareTo(a) > 0, true);
        assertEquals(a.compareTo(a) == 0, true);
        assertEquals(b.compareTo(b) == 0, true);
        assertEquals(convertInstant(a).compareTo(convertInstant(b)) == 0, true);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_compareTo_null() {
        OffsetTime a = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        a.compareTo(null);
    }

    @Test(expectedExceptions=ClassCastException.class)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void compareToNonOffsetTime() {
       Comparable c = TEST_11_30_59_500_PONE;
       c.compareTo(new Object());
    }

    private Instant convertInstant(OffsetTime ot) {
        return DATE.atTime(ot.toLocalTime()).toInstant(ot.getOffset());
    }

    //-----------------------------------------------------------------------
    // isAfter() / isBefore() / isEqual()
    //-----------------------------------------------------------------------
    @Test
    public void test_isBeforeIsAfterIsEqual1() {
        OffsetTime a = OffsetTime.of(11, 30, 58, 0, OFFSET_PONE);
        OffsetTime b = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);  // a is before b due to time
        assertEquals(a.isBefore(b), true);
        assertEquals(a.isEqual(b), false);
        assertEquals(a.isAfter(b), false);

        assertEquals(b.isBefore(a), false);
        assertEquals(b.isEqual(a), false);
        assertEquals(b.isAfter(a), true);

        assertEquals(a.isBefore(a), false);
        assertEquals(b.isBefore(b), false);

        assertEquals(a.isEqual(a), true);
        assertEquals(b.isEqual(b), true);

        assertEquals(a.isAfter(a), false);
        assertEquals(b.isAfter(b), false);
    }

    @Test
    public void test_isBeforeIsAfterIsEqual1nanos() {
        OffsetTime a = OffsetTime.of(11, 30, 59, 3, OFFSET_PONE);
        OffsetTime b = OffsetTime.of(11, 30, 59, 4, OFFSET_PONE);  // a is before b due to time
        assertEquals(a.isBefore(b), true);
        assertEquals(a.isEqual(b), false);
        assertEquals(a.isAfter(b), false);

        assertEquals(b.isBefore(a), false);
        assertEquals(b.isEqual(a), false);
        assertEquals(b.isAfter(a), true);

        assertEquals(a.isBefore(a), false);
        assertEquals(b.isBefore(b), false);

        assertEquals(a.isEqual(a), true);
        assertEquals(b.isEqual(b), true);

        assertEquals(a.isAfter(a), false);
        assertEquals(b.isAfter(b), false);
    }

    @Test
    public void test_isBeforeIsAfterIsEqual2() {
        OffsetTime a = OffsetTime.of(11, 30, 59, 0, OFFSET_PTWO);
        OffsetTime b = OffsetTime.of(11, 30, 58, 0, OFFSET_PONE);  // a is before b due to offset
        assertEquals(a.isBefore(b), true);
        assertEquals(a.isEqual(b), false);
        assertEquals(a.isAfter(b), false);

        assertEquals(b.isBefore(a), false);
        assertEquals(b.isEqual(a), false);
        assertEquals(b.isAfter(a), true);

        assertEquals(a.isBefore(a), false);
        assertEquals(b.isBefore(b), false);

        assertEquals(a.isEqual(a), true);
        assertEquals(b.isEqual(b), true);

        assertEquals(a.isAfter(a), false);
        assertEquals(b.isAfter(b), false);
    }

    @Test
    public void test_isBeforeIsAfterIsEqual2nanos() {
        OffsetTime a = OffsetTime.of(11, 30, 59, 4, ZoneOffset.ofTotalSeconds(OFFSET_PONE.getTotalSeconds() + 1));
        OffsetTime b = OffsetTime.of(11, 30, 59, 3, OFFSET_PONE);  // a is before b due to offset
        assertEquals(a.isBefore(b), true);
        assertEquals(a.isEqual(b), false);
        assertEquals(a.isAfter(b), false);

        assertEquals(b.isBefore(a), false);
        assertEquals(b.isEqual(a), false);
        assertEquals(b.isAfter(a), true);

        assertEquals(a.isBefore(a), false);
        assertEquals(b.isBefore(b), false);

        assertEquals(a.isEqual(a), true);
        assertEquals(b.isEqual(b), true);

        assertEquals(a.isAfter(a), false);
        assertEquals(b.isAfter(b), false);
    }

    @Test
    public void test_isBeforeIsAfterIsEqual_instantComparison() {
        OffsetTime a = OffsetTime.of(11, 30, 59, 0, OFFSET_PTWO);
        OffsetTime b = OffsetTime.of(10, 30, 59, 0, OFFSET_PONE);  // a is same instant as b
        assertEquals(a.isBefore(b), false);
        assertEquals(a.isEqual(b), true);
        assertEquals(a.isAfter(b), false);

        assertEquals(b.isBefore(a), false);
        assertEquals(b.isEqual(a), true);
        assertEquals(b.isAfter(a), false);

        assertEquals(a.isBefore(a), false);
        assertEquals(b.isBefore(b), false);

        assertEquals(a.isEqual(a), true);
        assertEquals(b.isEqual(b), true);

        assertEquals(a.isAfter(a), false);
        assertEquals(b.isAfter(b), false);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_isBefore_null() {
        OffsetTime a = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        a.isBefore(null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_isAfter_null() {
        OffsetTime a = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        a.isAfter(null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_isEqual_null() {
        OffsetTime a = OffsetTime.of(11, 30, 59, 0, OFFSET_PONE);
        a.isEqual(null);
    }

    //-----------------------------------------------------------------------
    // equals() / hashCode()
    //-----------------------------------------------------------------------
    @Test(dataProvider="sampleTimes")
    public void test_equals_true(int h, int m, int s, int n, ZoneOffset ignored) {
        OffsetTime a = OffsetTime.of(h, m, s, n, OFFSET_PONE);
        OffsetTime b = OffsetTime.of(h, m, s, n, OFFSET_PONE);
        assertEquals(a.equals(b), true);
        assertEquals(a.hashCode() == b.hashCode(), true);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_hour_differs(int h, int m, int s, int n, ZoneOffset ignored) {
        h = (h == 23 ? 22 : h);
        OffsetTime a = OffsetTime.of(h, m, s, n, OFFSET_PONE);
        OffsetTime b = OffsetTime.of(h + 1, m, s, n, OFFSET_PONE);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_minute_differs(int h, int m, int s, int n, ZoneOffset ignored) {
        m = (m == 59 ? 58 : m);
        OffsetTime a = OffsetTime.of(h, m, s, n, OFFSET_PONE);
        OffsetTime b = OffsetTime.of(h, m + 1, s, n, OFFSET_PONE);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_second_differs(int h, int m, int s, int n, ZoneOffset ignored) {
        s = (s == 59 ? 58 : s);
        OffsetTime a = OffsetTime.of(h, m, s, n, OFFSET_PONE);
        OffsetTime b = OffsetTime.of(h, m, s + 1, n, OFFSET_PONE);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_nano_differs(int h, int m, int s, int n, ZoneOffset ignored) {
        n = (n == 999999999 ? 999999998 : n);
        OffsetTime a = OffsetTime.of(h, m, s, n, OFFSET_PONE);
        OffsetTime b = OffsetTime.of(h, m, s, n + 1, OFFSET_PONE);
        assertEquals(a.equals(b), false);
    }
    @Test(dataProvider="sampleTimes")
    public void test_equals_false_offset_differs(int h, int m, int s, int n, ZoneOffset ignored) {
        OffsetTime a = OffsetTime.of(h, m, s, n, OFFSET_PONE);
        OffsetTime b = OffsetTime.of(h, m, s, n, OFFSET_PTWO);
        assertEquals(a.equals(b), false);
    }

    @Test
    public void test_equals_itself_true() {
        assertEquals(TEST_11_30_59_500_PONE.equals(TEST_11_30_59_500_PONE), true);
    }

    @Test
    public void test_equals_string_false() {
        assertEquals(TEST_11_30_59_500_PONE.equals("2007-07-15"), false);
    }

    @Test
    public void test_equals_null_false() {
        assertEquals(TEST_11_30_59_500_PONE.equals(null), false);
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @DataProvider(name="sampleToString")
    Object[][] provider_sampleToString() {
        return new Object[][] {
            {11, 30, 59, 0, "Z", "11:30:59Z"},
            {11, 30, 59, 0, "+01:00", "11:30:59+01:00"},
            {11, 30, 59, 999000000, "Z", "11:30:59.999Z"},
            {11, 30, 59, 999000000, "+01:00", "11:30:59.999+01:00"},
            {11, 30, 59, 999000, "Z", "11:30:59.000999Z"},
            {11, 30, 59, 999000, "+01:00", "11:30:59.000999+01:00"},
            {11, 30, 59, 999, "Z", "11:30:59.000000999Z"},
            {11, 30, 59, 999, "+01:00", "11:30:59.000000999+01:00"},
        };
    }

    @Test(dataProvider="sampleToString")
    public void test_toString(int h, int m, int s, int n, String offsetId, String expected) {
        OffsetTime t = OffsetTime.of(h, m, s, n, ZoneOffset.of(offsetId));
        String str = t.toString();
        assertEquals(str, expected);
    }

}
