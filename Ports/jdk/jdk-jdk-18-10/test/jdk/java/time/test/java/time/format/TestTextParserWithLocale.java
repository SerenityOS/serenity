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

/*
 * @test
 * @modules jdk.localedata
 */

package test.java.time.format;

import java.text.ParsePosition;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.HijrahDate;
import java.time.chrono.JapaneseDate;
import java.time.chrono.MinguoDate;
import java.time.chrono.ThaiBuddhistDate;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static org.testng.Assert.assertEquals;

/**
 * Test TextPrinterParser.
 */
@Test
public class TestTextParserWithLocale extends AbstractTestPrinterParser {
    static final Locale RUSSIAN = new Locale("ru");
    static final Locale FINNISH = new Locale("fi");

    @DataProvider(name="parseDayOfWeekText")
    Object[][] providerDayOfWeekData() {
        return new Object[][] {
            // Locale, pattern, input text, expected DayOfWeek
            {Locale.US, "e",  "1",  DayOfWeek.SUNDAY},
            {Locale.US, "ee", "01", DayOfWeek.SUNDAY},
            {Locale.US, "c",  "1",  DayOfWeek.SUNDAY},

            {Locale.UK, "e",  "1",  DayOfWeek.MONDAY},
            {Locale.UK, "ee", "01", DayOfWeek.MONDAY},
            {Locale.UK, "c",  "1",  DayOfWeek.MONDAY},
        };
    }

    @Test(dataProvider="parseDayOfWeekText")
    public void test_parseDayOfWeekText(Locale locale, String pattern, String input, DayOfWeek expected) {
        DateTimeFormatter formatter = getPatternFormatter(pattern).withLocale(locale);
        ParsePosition pos = new ParsePosition(0);
        assertEquals(DayOfWeek.from(formatter.parse(input, pos)), expected);
        assertEquals(pos.getIndex(), input.length());
    }

    //--------------------------------------------------------------------
    // Test data is dependent on localized resources.
    @DataProvider(name="parseStandaloneText")
    Object[][] providerStandaloneText() {
        // Locale, TemporalField, TextStyle, expected value, input text
        return new Object[][] {
            {RUSSIAN, MONTH_OF_YEAR, TextStyle.FULL_STANDALONE,   1, "\u044f\u043d\u0432\u0430\u0440\u044c"},
            {RUSSIAN, MONTH_OF_YEAR, TextStyle.FULL_STANDALONE,  12, "\u0434\u0435\u043a\u0430\u0431\u0440\u044c"},
            {RUSSIAN, MONTH_OF_YEAR, TextStyle.SHORT_STANDALONE,  1, "\u044f\u043d\u0432."},
            {RUSSIAN, MONTH_OF_YEAR, TextStyle.SHORT_STANDALONE, 12, "\u0434\u0435\u043a."},
            {FINNISH, DAY_OF_WEEK,   TextStyle.FULL_STANDALONE,   2, "tiistai"},
            {FINNISH, DAY_OF_WEEK,   TextStyle.SHORT_STANDALONE,  2, "ti"},
        };
    }

    // Test data is dependent on localized resources.
    @DataProvider(name="parseLenientText")
    Object[][] providerLenientText() {
        // Locale, TemporalField, expected value, input text
        return new Object[][] {
            {RUSSIAN, MONTH_OF_YEAR, 1, "\u044f\u043d\u0432\u0430\u0440\u044f"}, // full format
            {RUSSIAN, MONTH_OF_YEAR, 1, "\u044f\u043d\u0432\u0430\u0440\u044c"}, // full standalone
            {RUSSIAN, MONTH_OF_YEAR, 1, "\u044f\u043d\u0432."}, // short format
            {RUSSIAN, MONTH_OF_YEAR, 1, "\u044f\u043d\u0432."}, // short standalone
        };
    }

    @Test(dataProvider="parseStandaloneText")
    public void test_parseStandaloneText(Locale locale, TemporalField field, TextStyle style, int expectedValue, String input) {
        DateTimeFormatter formatter = getFormatter(field, style).withLocale(locale);
        ParsePosition pos = new ParsePosition(0);
        assertEquals(formatter.parseUnresolved(input, pos).getLong(field), (long) expectedValue);
        assertEquals(pos.getIndex(), input.length());
    }

    //-----------------------------------------------------------------------
    public void test_parse_french_short_strict_full_noMatch() throws Exception {
        setStrict(true);
        ParsePosition pos = new ParsePosition(0);
        getFormatter(MONTH_OF_YEAR, TextStyle.SHORT).withLocale(Locale.FRENCH)
                                                    .parseUnresolved("janvier", pos);
        assertEquals(pos.getErrorIndex(), 0);
    }

    public void test_parse_french_short_strict_short_match() throws Exception {
        setStrict(true);
        ParsePosition pos = new ParsePosition(0);
        assertEquals(getFormatter(MONTH_OF_YEAR, TextStyle.SHORT).withLocale(Locale.FRENCH)
                                                                 .parseUnresolved("janv.", pos)
                                                                 .getLong(MONTH_OF_YEAR),
                     1L);
        assertEquals(pos.getIndex(), 5);
    }

    //-----------------------------------------------------------------------

    @Test(dataProvider="parseLenientText")
    public void test_parseLenientText(Locale locale, TemporalField field, int expectedValue, String input) {
        setStrict(false);
        ParsePosition pos = new ParsePosition(0);
        DateTimeFormatter formatter = getFormatter(field).withLocale(locale);
        assertEquals(formatter.parseUnresolved(input, pos).getLong(field), (long) expectedValue);
        assertEquals(pos.getIndex(), input.length());
    }


    //-----------------------------------------------------------------------
    @DataProvider(name="parseChronoLocalDate")
    Object[][] provider_chronoLocalDate() {
        return new Object[][] {
            { HijrahDate.now() },
            { JapaneseDate.now() },
            { MinguoDate.now() },
            { ThaiBuddhistDate.now() }};
    }

    private static final DateTimeFormatter fmt_chrono =
        new DateTimeFormatterBuilder()
            .optionalStart()
            .appendChronologyId()
            .appendLiteral(' ')
            .optionalEnd()
            .optionalStart()
            .appendText(ChronoField.ERA, TextStyle.SHORT)
            .appendLiteral(' ')
            .optionalEnd()
            .appendValue(ChronoField.YEAR_OF_ERA, 1, 9, SignStyle.NORMAL)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 1, 2, SignStyle.NEVER)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NEVER)
            .toFormatter();

    @Test(dataProvider="parseChronoLocalDate")
    public void test_chronoLocalDate(ChronoLocalDate date) throws Exception {
        System.out.printf(" %s, [fmt=%s]%n", date, fmt_chrono.format(date));
        assertEquals(date, fmt_chrono.parse(fmt_chrono.format(date), ChronoLocalDate::from));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("[GGG ]yyy-MM-dd")
                                                 .withChronology(date.getChronology());
        System.out.printf(" %s, [fmt=%s]%n", date.toString(), fmt.format(date));
        assertEquals(date, fmt.parse(fmt.format(date), ChronoLocalDate::from));
    }
}
