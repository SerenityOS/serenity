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
package tck.java.time.temporal;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.JulianFields;
import java.time.temporal.TemporalField;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import tck.java.time.AbstractTCKTest;

/**
 * Test Julian Fields.
 */
@Test
public class TCKJulianFields extends AbstractTCKTest {

    private static final LocalDate JAN01_1970 = LocalDate.of(1970, 1, 1);
    private static final LocalDate DEC31_1969 = LocalDate.of(1969, 12, 31);
    private static final LocalDate NOV12_1945 = LocalDate.of(1945, 11, 12);
    private static final LocalDate JAN01_0001 = LocalDate.of(1, 1, 1);

    @DataProvider(name="samples")
    Object[][] data_samples() {
        return new Object[][] {
            {ChronoField.EPOCH_DAY, JAN01_1970, 0L},
            {JulianFields.JULIAN_DAY, JAN01_1970, 2400001L + 40587L},
            {JulianFields.MODIFIED_JULIAN_DAY, JAN01_1970, 40587L},
            {JulianFields.RATA_DIE, JAN01_1970, 710347L + (40587L - 31771L)},

            {ChronoField.EPOCH_DAY, DEC31_1969, -1L},
            {JulianFields.JULIAN_DAY, DEC31_1969, 2400001L + 40586L},
            {JulianFields.MODIFIED_JULIAN_DAY, DEC31_1969, 40586L},
            {JulianFields.RATA_DIE, DEC31_1969, 710347L + (40586L - 31771L)},

            {ChronoField.EPOCH_DAY, NOV12_1945, (-24 * 365 - 6) - 31 - 30 + 11},
            {JulianFields.JULIAN_DAY, NOV12_1945, 2431772L},
            {JulianFields.MODIFIED_JULIAN_DAY, NOV12_1945, 31771L},
            {JulianFields.RATA_DIE, NOV12_1945, 710347L},

            {ChronoField.EPOCH_DAY, JAN01_0001, (-24 * 365 - 6) - 31 - 30 + 11 - 710346L},
            {JulianFields.JULIAN_DAY, JAN01_0001, 2431772L - 710346L},
            {JulianFields.MODIFIED_JULIAN_DAY, JAN01_0001, 31771L - 710346L},
            {JulianFields.RATA_DIE, JAN01_0001, 1},
        };
    }

    //-----------------------------------------------------------------------
    public void test_basics() {
        assertEquals(JulianFields.JULIAN_DAY.isDateBased(), true);
        assertEquals(JulianFields.JULIAN_DAY.isTimeBased(), false);

        assertEquals(JulianFields.MODIFIED_JULIAN_DAY.isDateBased(), true);
        assertEquals(JulianFields.MODIFIED_JULIAN_DAY.isTimeBased(), false);

        assertEquals(JulianFields.RATA_DIE.isDateBased(), true);
        assertEquals(JulianFields.RATA_DIE.isTimeBased(), false);
    }

    //-----------------------------------------------------------------------
    @Test(dataProvider="samples")
    public void test_samples_get(TemporalField field, LocalDate date, long expected) {
        assertEquals(date.getLong(field), expected);
    }

    @Test(dataProvider="samples")
    public void test_samples_set(TemporalField field, LocalDate date, long value) {
        assertEquals(field.adjustInto(LocalDate.MAX, value), date);
        assertEquals(field.adjustInto(LocalDate.MIN, value), date);
        assertEquals(field.adjustInto(JAN01_1970, value), date);
        assertEquals(field.adjustInto(DEC31_1969, value), date);
        assertEquals(field.adjustInto(NOV12_1945, value), date);
    }

    //-----------------------------------------------------------------------
    @Test(dataProvider="samples")
    public void test_samples_parse_STRICT(TemporalField field, LocalDate date, long value) {
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendValue(field)
                .toFormatter().withResolverStyle(ResolverStyle.STRICT);
        LocalDate parsed = LocalDate.parse(Long.toString(value), f);
        assertEquals(parsed, date);
    }

    @Test(dataProvider="samples")
    public void test_samples_parse_SMART(TemporalField field, LocalDate date, long value) {
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendValue(field)
                .toFormatter().withResolverStyle(ResolverStyle.SMART);
        LocalDate parsed = LocalDate.parse(Long.toString(value), f);
        assertEquals(parsed, date);
    }

    @Test(dataProvider="samples")
    public void test_samples_parse_LENIENT(TemporalField field, LocalDate date, long value) {
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendValue(field)
                .toFormatter().withResolverStyle(ResolverStyle.LENIENT);
        LocalDate parsed = LocalDate.parse(Long.toString(value), f);
        assertEquals(parsed, date);
    }

}
