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
package test.java.time.format;

import static org.testng.Assert.assertEquals;

import java.time.DateTimeException;
import java.time.ZoneOffset;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test ZoneOffsetPrinterParser.
 */
@Test
public class TestZoneOffsetPrinter extends AbstractTestPrinterParser {

    private static final ZoneOffset OFFSET_0130 = ZoneOffset.of("+01:30");

    //-----------------------------------------------------------------------
    @DataProvider(name="offsets")
    Object[][] provider_offsets() {
        return new Object[][] {
            {"+HH", "NO-OFFSET", ZoneOffset.UTC},
            {"+HH", "+01", ZoneOffset.ofHours(1)},
            {"+HH", "-01", ZoneOffset.ofHours(-1)},

            {"+HHMM", "NO-OFFSET", ZoneOffset.UTC},
            {"+HHMM", "+0102", ZoneOffset.ofHoursMinutes(1, 2)},
            {"+HHMM", "-0102", ZoneOffset.ofHoursMinutes(-1, -2)},

            {"+HH:MM", "NO-OFFSET", ZoneOffset.UTC},
            {"+HH:MM", "+01:02", ZoneOffset.ofHoursMinutes(1, 2)},
            {"+HH:MM", "-01:02", ZoneOffset.ofHoursMinutes(-1, -2)},

            {"+HHMMss", "NO-OFFSET", ZoneOffset.UTC},
            {"+HHMMss", "+0100", ZoneOffset.ofHoursMinutesSeconds(1, 0, 0)},
            {"+HHMMss", "+0102", ZoneOffset.ofHoursMinutesSeconds(1, 2, 0)},
            {"+HHMMss", "+0159", ZoneOffset.ofHoursMinutesSeconds(1, 59, 0)},
            {"+HHMMss", "+0200", ZoneOffset.ofHoursMinutesSeconds(2, 0, 0)},
            {"+HHMMss", "+1800", ZoneOffset.ofHoursMinutesSeconds(18, 0, 0)},
            {"+HHMMss", "+010215", ZoneOffset.ofHoursMinutesSeconds(1, 2, 15)},
            {"+HHMMss", "-0100", ZoneOffset.ofHoursMinutesSeconds(-1, 0, 0)},
            {"+HHMMss", "-0200", ZoneOffset.ofHoursMinutesSeconds(-2, 0, 0)},
            {"+HHMMss", "-1800", ZoneOffset.ofHoursMinutesSeconds(-18, 0, 0)},

            {"+HHMMss", "NO-OFFSET", ZoneOffset.UTC},
            {"+HHMMss", "+0100", ZoneOffset.ofHoursMinutesSeconds(1, 0, 0)},
            {"+HHMMss", "+010203", ZoneOffset.ofHoursMinutesSeconds(1, 2, 3)},
            {"+HHMMss", "+015959", ZoneOffset.ofHoursMinutesSeconds(1, 59, 59)},
            {"+HHMMss", "+0200", ZoneOffset.ofHoursMinutesSeconds(2, 0, 0)},
            {"+HHMMss", "+1800", ZoneOffset.ofHoursMinutesSeconds(18, 0, 0)},
            {"+HHMMss", "-0100", ZoneOffset.ofHoursMinutesSeconds(-1, 0, 0)},
            {"+HHMMss", "-0200", ZoneOffset.ofHoursMinutesSeconds(-2, 0, 0)},
            {"+HHMMss", "-1800", ZoneOffset.ofHoursMinutesSeconds(-18, 0, 0)},

            {"+HH:MM:ss", "NO-OFFSET", ZoneOffset.UTC},
            {"+HH:MM:ss", "+01:00", ZoneOffset.ofHoursMinutesSeconds(1, 0, 0)},
            {"+HH:MM:ss", "+01:02", ZoneOffset.ofHoursMinutesSeconds(1, 2, 0)},
            {"+HH:MM:ss", "+01:59", ZoneOffset.ofHoursMinutesSeconds(1, 59, 0)},
            {"+HH:MM:ss", "+02:00", ZoneOffset.ofHoursMinutesSeconds(2, 0, 0)},
            {"+HH:MM:ss", "+18:00", ZoneOffset.ofHoursMinutesSeconds(18, 0, 0)},
            {"+HH:MM:ss", "+01:02:15", ZoneOffset.ofHoursMinutesSeconds(1, 2, 15)},
            {"+HH:MM:ss", "-01:00", ZoneOffset.ofHoursMinutesSeconds(-1, 0, 0)},
            {"+HH:MM:ss", "-02:00", ZoneOffset.ofHoursMinutesSeconds(-2, 0, 0)},
            {"+HH:MM:ss", "-18:00", ZoneOffset.ofHoursMinutesSeconds(-18, 0, 0)},

            {"+HH:MM:ss", "NO-OFFSET", ZoneOffset.UTC},
            {"+HH:MM:ss", "+01:00", ZoneOffset.ofHoursMinutesSeconds(1, 0, 0)},
            {"+HH:MM:ss", "+01:02:03", ZoneOffset.ofHoursMinutesSeconds(1, 2, 3)},
            {"+HH:MM:ss", "+01:59:59", ZoneOffset.ofHoursMinutesSeconds(1, 59, 59)},
            {"+HH:MM:ss", "+02:00", ZoneOffset.ofHoursMinutesSeconds(2, 0, 0)},
            {"+HH:MM:ss", "+18:00", ZoneOffset.ofHoursMinutesSeconds(18, 0, 0)},
            {"+HH:MM:ss", "-01:00", ZoneOffset.ofHoursMinutesSeconds(-1, 0, 0)},
            {"+HH:MM:ss", "-02:00", ZoneOffset.ofHoursMinutesSeconds(-2, 0, 0)},
            {"+HH:MM:ss", "-18:00", ZoneOffset.ofHoursMinutesSeconds(-18, 0, 0)},

            {"+HHMMSS", "NO-OFFSET", ZoneOffset.UTC},
            {"+HHMMSS", "+010203", ZoneOffset.ofHoursMinutesSeconds(1, 2, 3)},
            {"+HHMMSS", "-010203", ZoneOffset.ofHoursMinutesSeconds(-1, -2, -3)},
            {"+HHMMSS", "+010200", ZoneOffset.ofHoursMinutesSeconds(1, 2, 0)},
            {"+HHMMSS", "-010200", ZoneOffset.ofHoursMinutesSeconds(-1, -2, 0)},

            {"+HH:MM:SS", "NO-OFFSET", ZoneOffset.UTC},
            {"+HH:MM:SS", "+01:02:03", ZoneOffset.ofHoursMinutesSeconds(1, 2, 3)},
            {"+HH:MM:SS", "-01:02:03", ZoneOffset.ofHoursMinutesSeconds(-1, -2, -3)},
            {"+HH:MM:SS", "+01:02:00", ZoneOffset.ofHoursMinutesSeconds(1, 2, 0)},
            {"+HH:MM:SS", "-01:02:00", ZoneOffset.ofHoursMinutesSeconds(-1, -2, 0)},
        };
    }

    @Test(dataProvider="offsets")
    public void test_format(String pattern, String expected, ZoneOffset offset) throws Exception {
        buf.append("EXISTING");
        getFormatter(pattern, "NO-OFFSET").formatTo(offset, buf);
        assertEquals(buf.toString(), "EXISTING" + expected);
    }

    @Test(dataProvider="offsets")
    public void test_toString(String pattern, String expected, ZoneOffset offset) throws Exception {
        assertEquals(getFormatter(pattern, "NO-OFFSET").toString(), "Offset(" + pattern + ",'NO-OFFSET')");
    }

    //-----------------------------------------------------------------------
    @Test(expectedExceptions=DateTimeException.class)
    public void test_print_emptyCalendrical() throws Exception {
        getFormatter("+HH:MM:ss", "Z").formatTo(EMPTY_DTA, buf);
    }

    public void test_print_emptyAppendable() throws Exception {
        getFormatter("+HH:MM:ss", "Z").formatTo(OFFSET_0130, buf);
        assertEquals(buf.toString(), "+01:30");
    }

}
