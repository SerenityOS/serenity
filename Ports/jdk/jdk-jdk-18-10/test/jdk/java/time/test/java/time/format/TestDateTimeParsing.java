/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright (c) 2014, Stephen Colebourne & Michael Nascimento Santos
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
package test.java.time.format;

import static java.time.temporal.ChronoField.AMPM_OF_DAY;
import static java.time.temporal.ChronoField.EPOCH_DAY;
import static java.time.temporal.ChronoField.HOUR_OF_AMPM;
import static java.time.temporal.ChronoField.INSTANT_SECONDS;
import static java.time.temporal.ChronoField.MICRO_OF_SECOND;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.OFFSET_SECONDS;
import static java.time.temporal.ChronoField.SECOND_OF_DAY;
import static java.util.Locale.US;
import static org.testng.Assert.assertEquals;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @test
 * @summary Test parsing of edge cases.
 * @bug 8223773
 */
public class TestDateTimeParsing {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final ZoneOffset OFFSET_0230 = ZoneOffset.ofHoursMinutes(2, 30);

    private static final DateTimeFormatter LOCALFIELDS = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss").toFormatter();
    private static final DateTimeFormatter LOCALFIELDS_ZONEID = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss ").appendZoneId().toFormatter();
    private static final DateTimeFormatter LOCALFIELDS_OFFSETID = new DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss ").appendOffsetId().toFormatter();
    private static final DateTimeFormatter LOCALFIELDS_WITH_PARIS = LOCALFIELDS.withZone(PARIS);
    private static final DateTimeFormatter LOCALFIELDS_WITH_0230 = LOCALFIELDS.withZone(OFFSET_0230);
    private static final DateTimeFormatter INSTANT = new DateTimeFormatterBuilder()
        .appendInstant().toFormatter();
    private static final DateTimeFormatter INSTANT_WITH_PARIS = INSTANT.withZone(PARIS);
    private static final DateTimeFormatter INSTANT_WITH_0230 = INSTANT.withZone(OFFSET_0230);
    private static final DateTimeFormatter INSTANT_OFFSETID = new DateTimeFormatterBuilder()
        .appendInstant().appendLiteral(' ').appendOffsetId().toFormatter();
    private static final DateTimeFormatter INSTANT_OFFSETSECONDS = new DateTimeFormatterBuilder()
        .appendInstant().appendLiteral(' ').appendValue(OFFSET_SECONDS).toFormatter();
    private static final DateTimeFormatter INSTANTSECONDS = new DateTimeFormatterBuilder()
        .appendValue(INSTANT_SECONDS).toFormatter();
    private static final DateTimeFormatter INSTANTSECONDS_WITH_PARIS = INSTANTSECONDS.withZone(PARIS);
    private static final DateTimeFormatter INSTANTSECONDS_NOS = new DateTimeFormatterBuilder()
        .appendValue(INSTANT_SECONDS).appendLiteral('.').appendValue(NANO_OF_SECOND).toFormatter();
    private static final DateTimeFormatter INSTANTSECONDS_NOS_WITH_PARIS = INSTANTSECONDS_NOS.withZone(PARIS);
    private static final DateTimeFormatter INSTANTSECONDS_OFFSETSECONDS = new DateTimeFormatterBuilder()
        .appendValue(INSTANT_SECONDS).appendLiteral(' ').appendValue(OFFSET_SECONDS).toFormatter();

    private static final String DTPE_MESSAGE =
        "Invalid value for HourOfAmPm (valid values 0 - 11): 12";

    @DataProvider(name = "instantZones")
    Object[][] data_instantZones() {
        return new Object[][] {
            {LOCALFIELDS_ZONEID, "2014-06-30 01:02:03 Europe/Paris", ZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, PARIS)},
            {LOCALFIELDS_ZONEID, "2014-06-30 01:02:03 +02:30", ZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, OFFSET_0230)},
            {LOCALFIELDS_OFFSETID, "2014-06-30 01:02:03 +02:30", ZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, OFFSET_0230)},
            {LOCALFIELDS_WITH_PARIS, "2014-06-30 01:02:03", ZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, PARIS)},
            {LOCALFIELDS_WITH_0230, "2014-06-30 01:02:03", ZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, OFFSET_0230)},
            {INSTANT_WITH_PARIS, "2014-06-30T01:02:03Z", ZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, ZoneOffset.UTC).withZoneSameInstant(PARIS)},
            {INSTANT_WITH_0230, "2014-06-30T01:02:03Z", ZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, ZoneOffset.UTC).withZoneSameInstant(OFFSET_0230)},
            {INSTANT_OFFSETID, "2014-06-30T01:02:03Z +02:30", ZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, ZoneOffset.UTC).withZoneSameInstant(OFFSET_0230)},
            {INSTANT_OFFSETSECONDS, "2014-06-30T01:02:03Z 9000", ZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, ZoneOffset.UTC).withZoneSameInstant(OFFSET_0230)},
            {INSTANTSECONDS_WITH_PARIS, "86402", Instant.ofEpochSecond(86402).atZone(PARIS)},
            {INSTANTSECONDS_NOS_WITH_PARIS, "86402.123456789", Instant.ofEpochSecond(86402, 123456789).atZone(PARIS)},
            {INSTANTSECONDS_OFFSETSECONDS, "86402 9000", Instant.ofEpochSecond(86402).atZone(OFFSET_0230)},
        };
    }

    @Test(dataProvider = "instantZones")
    public void test_parse_instantZones_ZDT(DateTimeFormatter formatter, String text, ZonedDateTime expected) {
        TemporalAccessor actual = formatter.parse(text);
        assertEquals(ZonedDateTime.from(actual), expected);
    }

    @Test(dataProvider = "instantZones")
    public void test_parse_instantZones_LDT(DateTimeFormatter formatter, String text, ZonedDateTime expected) {
        TemporalAccessor actual = formatter.parse(text);
        assertEquals(LocalDateTime.from(actual), expected.toLocalDateTime());
    }

    @Test(dataProvider = "instantZones")
    public void test_parse_instantZones_Instant(DateTimeFormatter formatter, String text, ZonedDateTime expected) {
        TemporalAccessor actual = formatter.parse(text);
        assertEquals(Instant.from(actual), expected.toInstant());
    }

    @Test(dataProvider = "instantZones")
    public void test_parse_instantZones_supported(DateTimeFormatter formatter, String text, ZonedDateTime expected) {
        TemporalAccessor actual = formatter.parse(text);
        assertEquals(actual.isSupported(INSTANT_SECONDS), true);
        assertEquals(actual.isSupported(EPOCH_DAY), true);
        assertEquals(actual.isSupported(SECOND_OF_DAY), true);
        assertEquals(actual.isSupported(NANO_OF_SECOND), true);
        assertEquals(actual.isSupported(MICRO_OF_SECOND), true);
        assertEquals(actual.isSupported(MILLI_OF_SECOND), true);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name = "instantNoZone")
    Object[][] data_instantNoZone() {
        return new Object[][] {
            {INSTANT, "2014-06-30T01:02:03Z", ZonedDateTime.of(2014, 6, 30, 1, 2, 3, 0, ZoneOffset.UTC).toInstant()},
            {INSTANTSECONDS, "86402", Instant.ofEpochSecond(86402)},
            {INSTANTSECONDS_NOS, "86402.123456789", Instant.ofEpochSecond(86402, 123456789)},
        };
    }

    @Test(dataProvider = "instantNoZone", expectedExceptions = DateTimeException.class)
    public void test_parse_instantNoZone_ZDT(DateTimeFormatter formatter, String text, Instant expected) {
        TemporalAccessor actual = formatter.parse(text);
        ZonedDateTime.from(actual);
    }

    @Test(dataProvider = "instantNoZone", expectedExceptions = DateTimeException.class)
    public void test_parse_instantNoZone_LDT(DateTimeFormatter formatter, String text, Instant expected) {
        TemporalAccessor actual = formatter.parse(text);
        LocalDateTime.from(actual);
    }

    @Test(dataProvider = "instantNoZone")
    public void test_parse_instantNoZone_Instant(DateTimeFormatter formatter, String text, Instant expected) {
        TemporalAccessor actual = formatter.parse(text);
        assertEquals(Instant.from(actual), expected);
    }

    @Test(dataProvider = "instantNoZone")
    public void test_parse_instantNoZone_supported(DateTimeFormatter formatter, String text, Instant expected) {
        TemporalAccessor actual = formatter.parse(text);
        assertEquals(actual.isSupported(INSTANT_SECONDS), true);
        assertEquals(actual.isSupported(EPOCH_DAY), false);
        assertEquals(actual.isSupported(SECOND_OF_DAY), false);
        assertEquals(actual.isSupported(NANO_OF_SECOND), true);
        assertEquals(actual.isSupported(MICRO_OF_SECOND), true);
        assertEquals(actual.isSupported(MILLI_OF_SECOND), true);
    }

    // Bug 8223773: validation check for the range of HourOfAmPm in SMART mode.
    // Should throw a DateTimeParseException, as 12 is out of range for HourOfAmPm.
    @Test(expectedExceptions = DateTimeParseException.class)
    public void test_validateHourOfAmPm() {
        try {
            new DateTimeFormatterBuilder()
                .appendValue(HOUR_OF_AMPM,2)
                .appendText(AMPM_OF_DAY)
                .toFormatter(US)
                .parse("12PM");
        } catch (DateTimeParseException e) {
            Throwable cause = e.getCause();
            if (cause == null ||
                !DTPE_MESSAGE.equals(cause.getMessage())) {
                throw new RuntimeException(
                    "DateTimeParseException was thrown with different reason: " + e);
            } else {
                throw e;
            }
        }
    }
}
