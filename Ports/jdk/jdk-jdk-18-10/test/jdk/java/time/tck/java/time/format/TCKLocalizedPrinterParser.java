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

import java.text.DateFormat;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test localized behavior of formatter.
 */
@Test
public class TCKLocalizedPrinterParser {

    private DateTimeFormatterBuilder builder;
    private ParsePosition pos;

    @BeforeMethod
    public void setUp() {
        builder = new DateTimeFormatterBuilder();
        pos = new ParsePosition(0);
    }

    //-----------------------------------------------------------------------
    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_parse_negativePosition() {
        builder.appendLocalized(null, null);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="date")
    Object[][] data_date() {
        return new Object[][] {
                {LocalDate.of(2012, 6, 30), FormatStyle.SHORT, DateFormat.SHORT, Locale.UK},
                {LocalDate.of(2012, 6, 30), FormatStyle.SHORT, DateFormat.SHORT, Locale.US},
                {LocalDate.of(2012, 6, 30), FormatStyle.SHORT, DateFormat.SHORT, Locale.FRANCE},
                {LocalDate.of(2012, 6, 30), FormatStyle.SHORT, DateFormat.SHORT, Locale.JAPAN},

                {LocalDate.of(2012, 6, 30), FormatStyle.MEDIUM, DateFormat.MEDIUM, Locale.UK},
                {LocalDate.of(2012, 6, 30), FormatStyle.MEDIUM, DateFormat.MEDIUM, Locale.US},
                {LocalDate.of(2012, 6, 30), FormatStyle.MEDIUM, DateFormat.MEDIUM, Locale.FRANCE},
                {LocalDate.of(2012, 6, 30), FormatStyle.MEDIUM, DateFormat.MEDIUM, Locale.JAPAN},

                {LocalDate.of(2012, 6, 30), FormatStyle.LONG, DateFormat.LONG, Locale.UK},
                {LocalDate.of(2012, 6, 30), FormatStyle.LONG, DateFormat.LONG, Locale.US},
                {LocalDate.of(2012, 6, 30), FormatStyle.LONG, DateFormat.LONG, Locale.FRANCE},
                {LocalDate.of(2012, 6, 30), FormatStyle.LONG, DateFormat.LONG, Locale.JAPAN},

                {LocalDate.of(2012, 6, 30), FormatStyle.FULL, DateFormat.FULL, Locale.UK},
                {LocalDate.of(2012, 6, 30), FormatStyle.FULL, DateFormat.FULL, Locale.US},
                {LocalDate.of(2012, 6, 30), FormatStyle.FULL, DateFormat.FULL, Locale.FRANCE},
                {LocalDate.of(2012, 6, 30), FormatStyle.FULL, DateFormat.FULL, Locale.JAPAN},
        };
    }

    @SuppressWarnings("deprecation")
    @Test(dataProvider="date")
    public void test_date_print(LocalDate date, FormatStyle dateStyle, int dateStyleOld, Locale locale) {
        DateFormat old = DateFormat.getDateInstance(dateStyleOld, locale);
        Date oldDate = new Date(date.getYear() - 1900, date.getMonthValue() - 1, date.getDayOfMonth());
        String text = old.format(oldDate);

        DateTimeFormatter f = builder.appendLocalized(dateStyle, null).toFormatter(locale);
        String formatted = f.format(date);
        assertEquals(formatted, text);
    }

    @SuppressWarnings("deprecation")
    @Test(dataProvider="date")
    public void test_date_parse(LocalDate date, FormatStyle dateStyle, int dateStyleOld, Locale locale) {
        DateFormat old = DateFormat.getDateInstance(dateStyleOld, locale);
        Date oldDate = new Date(date.getYear() - 1900, date.getMonthValue() - 1, date.getDayOfMonth());
        String text = old.format(oldDate);

        DateTimeFormatter f = builder.appendLocalized(dateStyle, null).toFormatter(locale);
        TemporalAccessor parsed = f.parse(text, pos);
        assertEquals(pos.getIndex(), text.length());
        assertEquals(pos.getErrorIndex(), -1);
        assertEquals(LocalDate.from(parsed), date);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="time")
    Object[][] data_time() {
        return new Object[][] {
                {LocalTime.of(11, 30), FormatStyle.SHORT, DateFormat.SHORT, Locale.UK},
                {LocalTime.of(11, 30), FormatStyle.SHORT, DateFormat.SHORT, Locale.US},
                {LocalTime.of(11, 30), FormatStyle.SHORT, DateFormat.SHORT, Locale.FRANCE},
                {LocalTime.of(11, 30), FormatStyle.SHORT, DateFormat.SHORT, Locale.JAPAN},

                {LocalTime.of(11, 30), FormatStyle.MEDIUM, DateFormat.MEDIUM, Locale.UK},
                {LocalTime.of(11, 30), FormatStyle.MEDIUM, DateFormat.MEDIUM, Locale.US},
                {LocalTime.of(11, 30), FormatStyle.MEDIUM, DateFormat.MEDIUM, Locale.FRANCE},
                {LocalTime.of(11, 30), FormatStyle.MEDIUM, DateFormat.MEDIUM, Locale.JAPAN},

                // these localized patterns include "z" which isn't available from LocalTime
//                {LocalTime.of(11, 30), FormatStyle.LONG, DateFormat.LONG, Locale.UK},
//                {LocalTime.of(11, 30), FormatStyle.LONG, DateFormat.LONG, Locale.US},
//                {LocalTime.of(11, 30), FormatStyle.LONG, DateFormat.LONG, Locale.FRANCE},
//                {LocalTime.of(11, 30), FormatStyle.LONG, DateFormat.LONG, Locale.JAPAN},
//
//                {LocalTime.of(11, 30), FormatStyle.FULL, DateFormat.FULL, Locale.UK},
//                {LocalTime.of(11, 30), FormatStyle.FULL, DateFormat.FULL, Locale.US},
//                {LocalTime.of(11, 30), FormatStyle.FULL, DateFormat.FULL, Locale.FRANCE},
//                {LocalTime.of(11, 30), FormatStyle.FULL, DateFormat.FULL, Locale.JAPAN},
        };
    }

    @SuppressWarnings("deprecation")
    @Test(dataProvider="time")
    public void test_time_print(LocalTime time, FormatStyle timeStyle, int timeStyleOld, Locale locale) {
        DateFormat old = DateFormat.getTimeInstance(timeStyleOld, locale);
        Date oldDate = new Date(1970 - 1900, 0, 0, time.getHour(), time.getMinute(), time.getSecond());
        String text = old.format(oldDate);

        DateTimeFormatter f = builder.appendLocalized(null, timeStyle).toFormatter(locale);
        String formatted = f.format(time);
        assertEquals(formatted, text);
    }

    @SuppressWarnings("deprecation")
    @Test(dataProvider="time")
    public void test_time_parse(LocalTime time, FormatStyle timeStyle, int timeStyleOld, Locale locale) {
        DateFormat old = DateFormat.getTimeInstance(timeStyleOld, locale);
        Date oldDate = new Date(1970 - 1900, 0, 0, time.getHour(), time.getMinute(), time.getSecond());
        String text = old.format(oldDate);

        DateTimeFormatter f = builder.appendLocalized(null, timeStyle).toFormatter(locale);
        TemporalAccessor parsed = f.parse(text, pos);
        assertEquals(pos.getIndex(), text.length());
        assertEquals(pos.getErrorIndex(), -1);
        assertEquals(LocalTime.from(parsed), time);
    }

}
