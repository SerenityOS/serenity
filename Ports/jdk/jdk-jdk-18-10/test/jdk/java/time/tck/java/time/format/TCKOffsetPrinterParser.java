/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright (c) 2010-2012, Stephen Colebourne & Michael Nascimento Santos
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
package tck.java.time.format;

import static org.testng.Assert.assertEquals;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.util.Locale;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test DateTimeFormatterBuilder.appendOffset().
 */
@Test
public class TCKOffsetPrinterParser {

    private static final ZoneOffset OFFSET_UTC = ZoneOffset.UTC;
    private static final ZoneOffset OFFSET_P0100 = ZoneOffset.ofHours(1);
    private static final ZoneOffset OFFSET_P0123 = ZoneOffset.ofHoursMinutes(1, 23);
    private static final ZoneOffset OFFSET_P0023 = ZoneOffset.ofHoursMinutes(0, 23);
    private static final ZoneOffset OFFSET_P012345 = ZoneOffset.ofHoursMinutesSeconds(1, 23, 45);
    private static final ZoneOffset OFFSET_P000045 = ZoneOffset.ofHoursMinutesSeconds(0, 0, 45);
    private static final ZoneOffset OFFSET_M0100 = ZoneOffset.ofHours(-1);
    private static final ZoneOffset OFFSET_M0123 = ZoneOffset.ofHoursMinutes(-1, -23);
    private static final ZoneOffset OFFSET_M0023 = ZoneOffset.ofHoursMinutes(0, -23);
    private static final ZoneOffset OFFSET_M012345 = ZoneOffset.ofHoursMinutesSeconds(-1, -23, -45);
    private static final ZoneOffset OFFSET_M000045 = ZoneOffset.ofHoursMinutesSeconds(0, 0, -45);
    private static final LocalDateTime DT_2012_06_30_12_30_40 = LocalDateTime.of(2012, 6, 30, 12, 30, 40);

    private static final ZoneOffset OFFSET_P1100 = ZoneOffset.ofHours(11);
    private static final ZoneOffset OFFSET_P1123 = ZoneOffset.ofHoursMinutes(11, 23);
    private static final ZoneOffset OFFSET_P1023 = ZoneOffset.ofHoursMinutes(10, 23);
    private static final ZoneOffset OFFSET_P112345 = ZoneOffset.ofHoursMinutesSeconds(11, 23, 45);
    private static final ZoneOffset OFFSET_P100045 = ZoneOffset.ofHoursMinutesSeconds(10, 0, 45);
    private static final ZoneOffset OFFSET_M1100 = ZoneOffset.ofHours(-11);
    private static final ZoneOffset OFFSET_M1123 = ZoneOffset.ofHoursMinutes(-11, -23);
    private static final ZoneOffset OFFSET_M112345 = ZoneOffset.ofHoursMinutesSeconds(-11, -23, -45);
    private DateTimeFormatterBuilder builder;

    @BeforeMethod
    public void setUp() {
        builder = new DateTimeFormatterBuilder();
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="print")
    Object[][] data_print() {
        return new Object[][] {
                {"+HH", "Z", DT_2012_06_30_12_30_40, OFFSET_UTC, "Z"},
                {"+HH", "Z", DT_2012_06_30_12_30_40, OFFSET_P0100, "+01"},
                {"+HH", "Z", DT_2012_06_30_12_30_40, OFFSET_P0123, "+01"},
                {"+HH", "Z", DT_2012_06_30_12_30_40, OFFSET_P0023, "Z"},
                {"+HH", "Z", DT_2012_06_30_12_30_40, OFFSET_P012345, "+01"},
                {"+HH", "Z", DT_2012_06_30_12_30_40, OFFSET_P000045, "Z"},
                {"+HH", "Z", DT_2012_06_30_12_30_40, OFFSET_M0100, "-01"},
                {"+HH", "Z", DT_2012_06_30_12_30_40, OFFSET_M0123, "-01"},
                {"+HH", "Z", DT_2012_06_30_12_30_40, OFFSET_M0023, "Z"},
                {"+HH", "Z", DT_2012_06_30_12_30_40, OFFSET_M012345, "-01"},
                {"+HH", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "Z"},

                {"+HHmm", "Z", DT_2012_06_30_12_30_40, OFFSET_UTC, "Z"},
                {"+HHmm", "Z", DT_2012_06_30_12_30_40, OFFSET_P0100, "+01"},
                {"+HHmm", "Z", DT_2012_06_30_12_30_40, OFFSET_P0123, "+0123"},
                {"+HHmm", "Z", DT_2012_06_30_12_30_40, OFFSET_P0023, "+0023"},
                {"+HHmm", "Z", DT_2012_06_30_12_30_40, OFFSET_P012345, "+0123"},
                {"+HHmm", "Z", DT_2012_06_30_12_30_40, OFFSET_P000045, "Z"},
                {"+HHmm", "Z", DT_2012_06_30_12_30_40, OFFSET_M0100, "-01"},
                {"+HHmm", "Z", DT_2012_06_30_12_30_40, OFFSET_M0123, "-0123"},
                {"+HHmm", "Z", DT_2012_06_30_12_30_40, OFFSET_M0023, "-0023"},
                {"+HHmm", "Z", DT_2012_06_30_12_30_40, OFFSET_M012345, "-0123"},
                {"+HHmm", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "Z"},

                {"+HHMM", "Z", DT_2012_06_30_12_30_40, OFFSET_UTC, "Z"},
                {"+HHMM", "Z", DT_2012_06_30_12_30_40, OFFSET_P0100, "+0100"},
                {"+HHMM", "Z", DT_2012_06_30_12_30_40, OFFSET_P0123, "+0123"},
                {"+HHMM", "Z", DT_2012_06_30_12_30_40, OFFSET_P0023, "+0023"},
                {"+HHMM", "Z", DT_2012_06_30_12_30_40, OFFSET_P012345, "+0123"},
                {"+HHMM", "Z", DT_2012_06_30_12_30_40, OFFSET_P000045, "Z"},
                {"+HHMM", "Z", DT_2012_06_30_12_30_40, OFFSET_M0100, "-0100"},
                {"+HHMM", "Z", DT_2012_06_30_12_30_40, OFFSET_M0123, "-0123"},
                {"+HHMM", "Z", DT_2012_06_30_12_30_40, OFFSET_M0023, "-0023"},
                {"+HHMM", "Z", DT_2012_06_30_12_30_40, OFFSET_M012345, "-0123"},
                {"+HHMM", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "Z"},

                {"+HH:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_UTC, "Z"},
                {"+HH:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_P0100, "+01:00"},
                {"+HH:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_P0123, "+01:23"},
                {"+HH:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_P0023, "+00:23"},
                {"+HH:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_P012345, "+01:23"},
                {"+HH:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_P000045, "Z"},
                {"+HH:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_M0100, "-01:00"},
                {"+HH:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_M0123, "-01:23"},
                {"+HH:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_M0023, "-00:23"},
                {"+HH:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_M012345, "-01:23"},
                {"+HH:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "Z"},

                {"+HHMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_UTC, "Z"},
                {"+HHMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0100, "+0100"},
                {"+HHMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0123, "+0123"},
                {"+HHMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0023, "+0023"},
                {"+HHMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_P012345, "+012345"},
                {"+HHMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_P000045, "+000045"},
                {"+HHMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0100, "-0100"},
                {"+HHMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0123, "-0123"},
                {"+HHMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0023, "-0023"},
                {"+HHMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_M012345, "-012345"},
                {"+HHMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "-000045"},

                {"+HH:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_UTC, "Z"},
                {"+HH:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0100, "+01:00"},
                {"+HH:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0123, "+01:23"},
                {"+HH:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0023, "+00:23"},
                {"+HH:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P012345, "+01:23:45"},
                {"+HH:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "-00:00:45"},
                {"+HH:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0100, "-01:00"},
                {"+HH:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0123, "-01:23"},
                {"+HH:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0023, "-00:23"},
                {"+HH:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M012345, "-01:23:45"},
                {"+HH:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "-00:00:45"},

                {"+HHMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_UTC, "Z"},
                {"+HHMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_P0100, "+010000"},
                {"+HHMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_P0123, "+012300"},
                {"+HHMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_P0023, "+002300"},
                {"+HHMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_P012345, "+012345"},
                {"+HHMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "-000045"},
                {"+HHMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_M0100, "-010000"},
                {"+HHMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_M0123, "-012300"},
                {"+HHMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_M0023, "-002300"},
                {"+HHMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_M012345, "-012345"},
                {"+HHMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "-000045"},

                {"+HH:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_UTC, "Z"},
                {"+HH:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_P0100, "+01:00:00"},
                {"+HH:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_P0123, "+01:23:00"},
                {"+HH:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_P0023, "+00:23:00"},
                {"+HH:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_P012345, "+01:23:45"},
                {"+HH:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "-00:00:45"},
                {"+HH:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_M0100, "-01:00:00"},
                {"+HH:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_M0123, "-01:23:00"},
                {"+HH:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_M0023, "-00:23:00"},
                {"+HH:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_M012345, "-01:23:45"},
                {"+HH:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "-00:00:45"},

                {"+HH:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_UTC, "Z"},
                {"+HH:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0100, "+01"},
                {"+HH:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0123, "+01:23"},
                {"+HH:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0023, "+00:23"},
                {"+HH:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P012345, "+01:23:45"},
                {"+HH:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "-00:00:45"},
                {"+HH:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0100, "-01"},
                {"+HH:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0123, "-01:23"},
                {"+HH:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0023, "-00:23"},
                {"+HH:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M012345, "-01:23:45"},
                {"+HH:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "-00:00:45"},

                {"+HHmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_UTC, "Z"},
                {"+HHmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0100, "+01"},
                {"+HHmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0123, "+0123"},
                {"+HHmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0023, "+0023"},
                {"+HHmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_P012345, "+012345"},
                {"+HHmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_P000045, "+000045"},
                {"+HHmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0100, "-01"},
                {"+HHmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0123, "-0123"},
                {"+HHmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0023, "-0023"},
                {"+HHmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_M012345, "-012345"},
                {"+HHmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "-000045"},

                {"+H", "Z", DT_2012_06_30_12_30_40, OFFSET_UTC, "Z"},
                {"+H", "Z", DT_2012_06_30_12_30_40, OFFSET_P0100, "+1"},
                {"+H", "Z", DT_2012_06_30_12_30_40, OFFSET_P0123, "+1"},
                {"+H", "Z", DT_2012_06_30_12_30_40, OFFSET_P0023, "Z"},
                {"+H", "Z", DT_2012_06_30_12_30_40, OFFSET_P012345, "+1"},
                {"+H", "Z", DT_2012_06_30_12_30_40, OFFSET_P000045, "Z"},
                {"+H", "Z", DT_2012_06_30_12_30_40, OFFSET_M0100, "-1"},
                {"+H", "Z", DT_2012_06_30_12_30_40, OFFSET_M0123, "-1"},
                {"+H", "Z", DT_2012_06_30_12_30_40, OFFSET_M0023, "Z"},
                {"+H", "Z", DT_2012_06_30_12_30_40, OFFSET_M012345, "-1"},
                {"+H", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "Z"},

                {"+Hmm", "Z", DT_2012_06_30_12_30_40, OFFSET_UTC, "Z"},
                {"+Hmm", "Z", DT_2012_06_30_12_30_40, OFFSET_P0100, "+1"},
                {"+Hmm", "Z", DT_2012_06_30_12_30_40, OFFSET_P0123, "+123"},
                {"+Hmm", "Z", DT_2012_06_30_12_30_40, OFFSET_P0023, "+023"},
                {"+Hmm", "Z", DT_2012_06_30_12_30_40, OFFSET_P012345, "+123"},
                {"+Hmm", "Z", DT_2012_06_30_12_30_40, OFFSET_P000045, "Z"},
                {"+Hmm", "Z", DT_2012_06_30_12_30_40, OFFSET_M0100, "-1"},
                {"+Hmm", "Z", DT_2012_06_30_12_30_40, OFFSET_M0123, "-123"},
                {"+Hmm", "Z", DT_2012_06_30_12_30_40, OFFSET_M0023, "-023"},
                {"+Hmm", "Z", DT_2012_06_30_12_30_40, OFFSET_M012345, "-123"},
                {"+Hmm", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "Z"},

                {"+HMM", "Z", DT_2012_06_30_12_30_40, OFFSET_UTC, "Z"},
                {"+HMM", "Z", DT_2012_06_30_12_30_40, OFFSET_P0100, "+100"},
                {"+HMM", "Z", DT_2012_06_30_12_30_40, OFFSET_P0123, "+123"},
                {"+HMM", "Z", DT_2012_06_30_12_30_40, OFFSET_P0023, "+023"},
                {"+HMM", "Z", DT_2012_06_30_12_30_40, OFFSET_P012345, "+123"},
                {"+HMM", "Z", DT_2012_06_30_12_30_40, OFFSET_P000045, "Z"},
                {"+HMM", "Z", DT_2012_06_30_12_30_40, OFFSET_M0100, "-100"},
                {"+HMM", "Z", DT_2012_06_30_12_30_40, OFFSET_M0123, "-123"},
                {"+HMM", "Z", DT_2012_06_30_12_30_40, OFFSET_M0023, "-023"},
                {"+HMM", "Z", DT_2012_06_30_12_30_40, OFFSET_M012345, "-123"},
                {"+HMM", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "Z"},

                {"+H:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_UTC, "Z"},
                {"+H:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_P0100, "+1:00"},
                {"+H:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_P0123, "+1:23"},
                {"+H:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_P0023, "+0:23"},
                {"+H:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_P012345, "+1:23"},
                {"+H:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_P000045, "Z"},
                {"+H:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_M0100, "-1:00"},
                {"+H:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_M0123, "-1:23"},
                {"+H:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_M0023, "-0:23"},
                {"+H:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_M012345, "-1:23"},
                {"+H:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "Z"},

                {"+HMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_UTC, "Z"},
                {"+HMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0100, "+100"},
                {"+HMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0123, "+123"},
                {"+HMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0023, "+023"},
                {"+HMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_P012345, "+12345"},
                {"+HMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_P000045, "+00045"},
                {"+HMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0100, "-100"},
                {"+HMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0123, "-123"},
                {"+HMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0023, "-023"},
                {"+HMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_M012345, "-12345"},
                {"+HMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "-00045"},

                {"+H:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_UTC, "Z"},
                {"+H:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0100, "+1:00"},
                {"+H:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0123, "+1:23"},
                {"+H:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0023, "+0:23"},
                {"+H:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P012345, "+1:23:45"},
                {"+H:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "-0:00:45"},
                {"+H:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0100, "-1:00"},
                {"+H:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0123, "-1:23"},
                {"+H:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0023, "-0:23"},
                {"+H:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M012345, "-1:23:45"},
                {"+H:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "-0:00:45"},

                {"+HMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_UTC, "Z"},
                {"+HMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_P0100, "+10000"},
                {"+HMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_P0123, "+12300"},
                {"+HMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_P0023, "+02300"},
                {"+HMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_P012345, "+12345"},
                {"+HMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "-00045"},
                {"+HMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_M0100, "-10000"},
                {"+HMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_M0123, "-12300"},
                {"+HMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_M0023, "-02300"},
                {"+HMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_M012345, "-12345"},
                {"+HMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "-00045"},

                {"+H:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_UTC, "Z"},
                {"+H:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_P0100, "+1:00:00"},
                {"+H:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_P0123, "+1:23:00"},
                {"+H:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_P0023, "+0:23:00"},
                {"+H:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_P012345, "+1:23:45"},
                {"+H:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "-0:00:45"},
                {"+H:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_M0100, "-1:00:00"},
                {"+H:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_M0123, "-1:23:00"},
                {"+H:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_M0023, "-0:23:00"},
                {"+H:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_M012345, "-1:23:45"},
                {"+H:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "-0:00:45"},

                {"+H:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_UTC, "Z"},
                {"+H:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0100, "+1"},
                {"+H:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0123, "+1:23"},
                {"+H:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0023, "+0:23"},
                {"+H:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P012345, "+1:23:45"},
                {"+H:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "-0:00:45"},
                {"+H:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0100, "-1"},
                {"+H:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0123, "-1:23"},
                {"+H:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0023, "-0:23"},
                {"+H:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M012345, "-1:23:45"},
                {"+H:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "-0:00:45"},

                {"+Hmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_UTC, "Z"},
                {"+Hmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0100, "+1"},
                {"+Hmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0123, "+123"},
                {"+Hmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_P0023, "+023"},
                {"+Hmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_P012345, "+12345"},
                {"+Hmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_P000045, "+00045"},
                {"+Hmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0100, "-1"},
                {"+Hmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0123, "-123"},
                {"+Hmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_M0023, "-023"},
                {"+Hmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_M012345, "-12345"},
                {"+Hmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_M000045, "-00045"},

                {"+H", "Z", DT_2012_06_30_12_30_40, OFFSET_P1100, "+11"},
                {"+H", "Z", DT_2012_06_30_12_30_40, OFFSET_P1123, "+11"},
                {"+H", "Z", DT_2012_06_30_12_30_40, OFFSET_P1023, "+10"},
                {"+H", "Z", DT_2012_06_30_12_30_40, OFFSET_P112345, "+11"},
                {"+H", "Z", DT_2012_06_30_12_30_40, OFFSET_P100045, "+10"},
                {"+H", "Z", DT_2012_06_30_12_30_40, OFFSET_M1100, "-11"},
                {"+H", "Z", DT_2012_06_30_12_30_40, OFFSET_M1123, "-11"},
                {"+H", "Z", DT_2012_06_30_12_30_40, OFFSET_M112345, "-11"},

                {"+Hmm", "Z", DT_2012_06_30_12_30_40, OFFSET_P1100, "+11"},
                {"+Hmm", "Z", DT_2012_06_30_12_30_40, OFFSET_P1123, "+1123"},
                {"+Hmm", "Z", DT_2012_06_30_12_30_40, OFFSET_P1023, "+1023"},
                {"+Hmm", "Z", DT_2012_06_30_12_30_40, OFFSET_P112345, "+1123"},
                {"+Hmm", "Z", DT_2012_06_30_12_30_40, OFFSET_P100045, "+10"},
                {"+Hmm", "Z", DT_2012_06_30_12_30_40, OFFSET_M1100, "-11"},
                {"+Hmm", "Z", DT_2012_06_30_12_30_40, OFFSET_M1123, "-1123"},
                {"+Hmm", "Z", DT_2012_06_30_12_30_40, OFFSET_M112345, "-1123"},

                {"+HMM", "Z", DT_2012_06_30_12_30_40, OFFSET_P1100, "+1100"},
                {"+HMM", "Z", DT_2012_06_30_12_30_40, OFFSET_P1123, "+1123"},
                {"+HMM", "Z", DT_2012_06_30_12_30_40, OFFSET_P1023, "+1023"},
                {"+HMM", "Z", DT_2012_06_30_12_30_40, OFFSET_P112345, "+1123"},
                {"+HMM", "Z", DT_2012_06_30_12_30_40, OFFSET_P100045, "+1000"},
                {"+HMM", "Z", DT_2012_06_30_12_30_40, OFFSET_M1100, "-1100"},
                {"+HMM", "Z", DT_2012_06_30_12_30_40, OFFSET_M1123, "-1123"},
                {"+HMM", "Z", DT_2012_06_30_12_30_40, OFFSET_M112345, "-1123"},

                {"+H:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_P1100, "+11:00"},
                {"+H:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_P1123, "+11:23"},
                {"+H:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_P1023, "+10:23"},
                {"+H:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_P112345, "+11:23"},
                {"+H:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_P100045, "+10:00"},
                {"+H:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_M1100, "-11:00"},
                {"+H:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_M1123, "-11:23"},
                {"+H:MM", "Z", DT_2012_06_30_12_30_40, OFFSET_M112345, "-11:23"},

                {"+HMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_P1100, "+1100"},
                {"+HMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_P1123, "+1123"},
                {"+HMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_P1023, "+1023"},
                {"+HMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_P112345, "+112345"},
                {"+HMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_P100045, "+100045"},
                {"+HMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_M1100, "-1100"},
                {"+HMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_M1123, "-1123"},
                {"+HMMss", "Z", DT_2012_06_30_12_30_40, OFFSET_M112345, "-112345"},

                {"+H:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P1100, "+11:00"},
                {"+H:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P1123, "+11:23"},
                {"+H:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P1023, "+10:23"},
                {"+H:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P112345, "+11:23:45"},
                {"+H:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M1100, "-11:00"},
                {"+H:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M1123, "-11:23"},
                {"+H:MM:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M112345, "-11:23:45"},

                {"+HMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_P1100, "+110000"},
                {"+HMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_P1123, "+112300"},
                {"+HMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_P1023, "+102300"},
                {"+HMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_P112345, "+112345"},
                {"+HMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_M1100, "-110000"},
                {"+HMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_M1123, "-112300"},
                {"+HMMSS", "Z", DT_2012_06_30_12_30_40, OFFSET_M112345, "-112345"},

                {"+H:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_P1100, "+11:00:00"},
                {"+H:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_P1123, "+11:23:00"},
                {"+H:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_P1023, "+10:23:00"},
                {"+H:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_P112345, "+11:23:45"},
                {"+H:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_M1100, "-11:00:00"},
                {"+H:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_M1123, "-11:23:00"},
                {"+H:MM:SS", "Z", DT_2012_06_30_12_30_40, OFFSET_M112345, "-11:23:45"},

                {"+H:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P1100, "+11"},
                {"+H:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P1123, "+11:23"},
                {"+H:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P1023, "+10:23"},
                {"+H:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_P112345, "+11:23:45"},
                {"+H:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M1100, "-11"},
                {"+H:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M1123, "-11:23"},
                {"+H:mm:ss", "Z", DT_2012_06_30_12_30_40, OFFSET_M112345, "-11:23:45"},

                {"+Hmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_P1100, "+11"},
                {"+Hmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_P1123, "+1123"},
                {"+Hmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_P1023, "+1023"},
                {"+Hmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_P112345, "+112345"},
                {"+Hmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_P100045, "+100045"},
                {"+Hmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_M1100, "-11"},
                {"+Hmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_M1123, "-1123"},
                {"+Hmmss", "Z", DT_2012_06_30_12_30_40, OFFSET_M112345, "-112345"},
        };
    }

    @DataProvider(name="print_localized")
    Object[][] data_print_localized() {
        return new Object[][] {
                {TextStyle.FULL, DT_2012_06_30_12_30_40, OFFSET_UTC, "GMT"},
                {TextStyle.FULL, DT_2012_06_30_12_30_40, OFFSET_P0100, "GMT+01:00"},
                {TextStyle.FULL, DT_2012_06_30_12_30_40, OFFSET_P0123, "GMT+01:23"},
                {TextStyle.FULL, DT_2012_06_30_12_30_40, OFFSET_P0023, "GMT+00:23"},
                {TextStyle.FULL, DT_2012_06_30_12_30_40, OFFSET_P012345, "GMT+01:23:45"},
                {TextStyle.FULL, DT_2012_06_30_12_30_40, OFFSET_M000045, "GMT-00:00:45"},
                {TextStyle.FULL, DT_2012_06_30_12_30_40, OFFSET_M0100, "GMT-01:00"},
                {TextStyle.FULL, DT_2012_06_30_12_30_40, OFFSET_M0123, "GMT-01:23"},
                {TextStyle.FULL, DT_2012_06_30_12_30_40, OFFSET_M0023, "GMT-00:23"},
                {TextStyle.FULL, DT_2012_06_30_12_30_40, OFFSET_M012345, "GMT-01:23:45"},
                {TextStyle.FULL, DT_2012_06_30_12_30_40, OFFSET_M000045, "GMT-00:00:45"},
                {TextStyle.SHORT, DT_2012_06_30_12_30_40, OFFSET_UTC, "GMT"},
                {TextStyle.SHORT, DT_2012_06_30_12_30_40, OFFSET_P0100, "GMT+1"},
                {TextStyle.SHORT, DT_2012_06_30_12_30_40, OFFSET_P0123, "GMT+1:23"},
                {TextStyle.SHORT, DT_2012_06_30_12_30_40, OFFSET_P0023, "GMT+0:23"},
                {TextStyle.SHORT, DT_2012_06_30_12_30_40, OFFSET_P012345, "GMT+1:23:45"},
                {TextStyle.SHORT, DT_2012_06_30_12_30_40, OFFSET_M000045, "GMT-0:00:45"},
                {TextStyle.SHORT, DT_2012_06_30_12_30_40, OFFSET_M0100, "GMT-1"},
                {TextStyle.SHORT, DT_2012_06_30_12_30_40, OFFSET_M0123, "GMT-1:23"},
                {TextStyle.SHORT, DT_2012_06_30_12_30_40, OFFSET_M0023, "GMT-0:23"},
                {TextStyle.SHORT, DT_2012_06_30_12_30_40, OFFSET_M012345, "GMT-1:23:45"},
                {TextStyle.SHORT, DT_2012_06_30_12_30_40, OFFSET_M000045, "GMT-0:00:45"},
        };
    }

    @Test(dataProvider="print")
    public void test_print(String offsetPattern, String noOffset, LocalDateTime ldt, ZoneId zone, String expected) {
        ZonedDateTime zdt = ldt.atZone(zone);
        builder.appendOffset(offsetPattern, noOffset);
        String output = builder.toFormatter().format(zdt);
        assertEquals(output, expected);
    }

    //-----------------------------------------------------------------------
    @Test(dataProvider="print")
    public void test_print_pattern_X(String offsetPattern, String noOffset, LocalDateTime ldt, ZoneId zone, String expected) {
        String pattern = null;
        if (offsetPattern.equals("+HHmm") && noOffset.equals("Z")) {
            pattern = "X";
        } else if (offsetPattern.equals("+HHMM") && noOffset.equals("Z")) {
            pattern = "XX";
        } else if (offsetPattern.equals("+HH:MM") && noOffset.equals("Z")) {
            pattern = "XXX";
        } else if (offsetPattern.equals("+HHMMss") && noOffset.equals("Z")) {
            pattern = "XXXX";
        } else if (offsetPattern.equals("+HH:MM:ss") && noOffset.equals("Z")) {
            pattern = "XXXXX";
        }
        if (pattern != null) {
            ZonedDateTime zdt = ldt.atZone(zone);
            builder.appendPattern(pattern);
            String output = builder.toFormatter().format(zdt);
            assertEquals(output, expected);
        }
    }

    @Test(dataProvider="print")
    public void test_print_pattern_x(String offsetPattern, String noOffset, LocalDateTime ldt, ZoneId zone, String expected) {
        String pattern = null;
        String zero = null;
        if (offsetPattern.equals("+HHmm") && noOffset.equals("Z")) {
            pattern = "x";
            zero = "+00";
        } else if (offsetPattern.equals("+HHMM") && noOffset.equals("Z")) {
            pattern = "xx";
            zero = "+0000";
        } else if (offsetPattern.equals("+HH:MM") && noOffset.equals("Z")) {
            pattern = "xxx";
            zero = "+00:00";
        } else if (offsetPattern.equals("+HHMMss") && noOffset.equals("Z")) {
            pattern = "xxxx";
            zero = "+0000";
        } else if (offsetPattern.equals("+HH:MM:ss") && noOffset.equals("Z")) {
            pattern = "xxxxx";
            zero = "+00:00";
        }
        if (pattern != null) {
            ZonedDateTime zdt = ldt.atZone(zone);
            builder.appendPattern(pattern);
            String output = builder.toFormatter().format(zdt);
            assertEquals(output, (expected.equals("Z") ? zero : expected));
        }
    }

    @Test(dataProvider="print")
    public void test_print_pattern_Z(String offsetPattern, String noOffset, LocalDateTime ldt, ZoneId zone, String expected) {
        String pattern = null;
        if (offsetPattern.equals("+HHMM") && noOffset.equals("Z")) {
            ZonedDateTime zdt = ldt.atZone(zone);
            DateTimeFormatter f1 = new DateTimeFormatterBuilder().appendPattern("Z").toFormatter();
            String output1 = f1.format(zdt);
            assertEquals(output1, (expected.equals("Z") ? "+0000" : expected));

            DateTimeFormatter f2 = new DateTimeFormatterBuilder().appendPattern("ZZ").toFormatter();
            String output2 = f2.format(zdt);
            assertEquals(output2, (expected.equals("Z") ? "+0000" : expected));

            DateTimeFormatter f3 = new DateTimeFormatterBuilder().appendPattern("ZZZ").toFormatter();
            String output3 = f3.format(zdt);
            assertEquals(output3, (expected.equals("Z") ? "+0000" : expected));
        } else if (offsetPattern.equals("+HH:MM:ss") && noOffset.equals("Z")) {
            ZonedDateTime zdt = ldt.atZone(zone);
            DateTimeFormatter f = new DateTimeFormatterBuilder().appendPattern("ZZZZZ").toFormatter();
            String output = f.format(zdt);
            assertEquals(output, expected);
        }
    }

    @Test(dataProvider="print_localized")
    public void test_print_localized(TextStyle style, LocalDateTime ldt, ZoneOffset offset, String expected) {
        OffsetDateTime odt = OffsetDateTime.of(ldt, offset);
        ZonedDateTime zdt = ldt.atZone(offset);

        DateTimeFormatter f = new DateTimeFormatterBuilder().appendLocalizedOffset(style)
                                                            .toFormatter(Locale.US);
        assertEquals(f.format(odt), expected);
        assertEquals(f.format(zdt), expected);
        assertEquals(f.parse(expected, ZoneOffset::from), offset);

        if (style == TextStyle.FULL) {
            f = new DateTimeFormatterBuilder().appendPattern("ZZZZ")
                                              .toFormatter(Locale.US);
            assertEquals(f.format(odt), expected);
            assertEquals(f.format(zdt), expected);
            assertEquals(f.parse(expected, ZoneOffset::from), offset);

            f = new DateTimeFormatterBuilder().appendPattern("OOOO")
                                              .toFormatter(Locale.US);
            assertEquals(f.format(odt), expected);
            assertEquals(f.format(zdt), expected);
            assertEquals(f.parse(expected, ZoneOffset::from), offset);
        }

        if (style == TextStyle.SHORT) {
            f = new DateTimeFormatterBuilder().appendPattern("O")
                                              .toFormatter(Locale.US);
            assertEquals(f.format(odt), expected);
            assertEquals(f.format(zdt), expected);
            assertEquals(f.parse(expected, ZoneOffset::from), offset);
        }
    }

    //-----------------------------------------------------------------------
    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_print_pattern_X6rejected() {
        builder.appendPattern("XXXXXX");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_print_pattern_x6rejected() {
        builder.appendPattern("xxxxxx");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_print_pattern_Z6rejected() {
        builder.appendPattern("ZZZZZZ");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_print_pattern_O2rejected() {
        builder.appendPattern("OO");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_print_pattern_O3rejected() {
        builder.appendPattern("OOO");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_print_pattern_O5rejected() {
        builder.appendPattern("OOOOO");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_print_pattern_localzed_full_standline() {
        builder.appendLocalizedOffset(TextStyle.FULL_STANDALONE);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_print_pattern_localzed_short_standalone() {
        builder.appendLocalizedOffset(TextStyle.SHORT_STANDALONE);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_print_pattern_localzed_narrow() {
        builder.appendLocalizedOffset(TextStyle.NARROW);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_print_pattern_localzed_narrow_standalone() {
        builder.appendLocalizedOffset(TextStyle.NARROW_STANDALONE);
    }

}
