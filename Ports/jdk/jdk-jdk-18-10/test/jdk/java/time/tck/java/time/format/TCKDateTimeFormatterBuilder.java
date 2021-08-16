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
 * Copyright (c) 2009-2012, Stephen Colebourne & Michael Nascimento Santos
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

import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.OFFSET_SECONDS;
import static java.time.temporal.ChronoField.YEAR;
import static org.testng.Assert.assertEquals;

import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.time.format.TextStyle;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test DateTimeFormatterBuilder.
 */
@Test
public class TCKDateTimeFormatterBuilder {

    private DateTimeFormatterBuilder builder;

    @BeforeMethod
    public void setUp() {
        builder = new DateTimeFormatterBuilder();
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_toFormatter_empty() throws Exception {
        DateTimeFormatter f = builder.toFormatter();
        assertEquals(f.format(LocalDate.of(2012, 6, 30)), "");
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parseDefaulting_entireDate() {
        DateTimeFormatter f = builder
            .parseDefaulting(YEAR, 2012).parseDefaulting(MONTH_OF_YEAR, 6)
            .parseDefaulting(DAY_OF_MONTH, 30).toFormatter();
        LocalDate parsed = f.parse("", LocalDate::from);  // blank string can be parsed
        assertEquals(parsed, LocalDate.of(2012, 6, 30));
    }

    @Test
    public void test_parseDefaulting_yearOptionalMonthOptionalDay() {
        DateTimeFormatter f = builder
                .appendValue(YEAR)
                .optionalStart().appendLiteral('-').appendValue(MONTH_OF_YEAR)
                .optionalStart().appendLiteral('-').appendValue(DAY_OF_MONTH)
                .optionalEnd().optionalEnd()
                .parseDefaulting(MONTH_OF_YEAR, 1)
                .parseDefaulting(DAY_OF_MONTH, 1).toFormatter();
        assertEquals(f.parse("2012", LocalDate::from), LocalDate.of(2012, 1, 1));
        assertEquals(f.parse("2012-6", LocalDate::from), LocalDate.of(2012, 6, 1));
        assertEquals(f.parse("2012-6-30", LocalDate::from), LocalDate.of(2012, 6, 30));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void test_parseDefaulting_null() {
        builder.parseDefaulting(null, 1);
    }

    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void test_appendValue_1arg_null() throws Exception {
        builder.appendValue(null);
    }

    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void test_appendValue_2arg_null() throws Exception {
        builder.appendValue(null, 3);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendValue_2arg_widthTooSmall() throws Exception {
        builder.appendValue(DAY_OF_MONTH, 0);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendValue_2arg_widthTooBig() throws Exception {
        builder.appendValue(DAY_OF_MONTH, 20);
    }

    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void test_appendValue_3arg_nullField() throws Exception {
        builder.appendValue(null, 2, 3, SignStyle.NORMAL);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendValue_3arg_minWidthTooSmall() throws Exception {
        builder.appendValue(DAY_OF_MONTH, 0, 2, SignStyle.NORMAL);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendValue_3arg_minWidthTooBig() throws Exception {
        builder.appendValue(DAY_OF_MONTH, 20, 2, SignStyle.NORMAL);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendValue_3arg_maxWidthTooSmall() throws Exception {
        builder.appendValue(DAY_OF_MONTH, 2, 0, SignStyle.NORMAL);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendValue_3arg_maxWidthTooBig() throws Exception {
        builder.appendValue(DAY_OF_MONTH, 2, 20, SignStyle.NORMAL);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendValue_3arg_maxWidthMinWidth() throws Exception {
        builder.appendValue(DAY_OF_MONTH, 4, 2, SignStyle.NORMAL);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_appendValue_3arg_nullSignStyle() throws Exception {
        builder.appendValue(DAY_OF_MONTH, 2, 3, null);
    }

    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void test_appendValueReduced_int_nullField() throws Exception {
        builder.appendValueReduced(null, 2, 2, 2000);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendValueReduced_int_minWidthTooSmall() throws Exception {
        builder.appendValueReduced(YEAR, 0, 2, 2000);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendValueReduced_int_minWidthTooBig() throws Exception {
        builder.appendValueReduced(YEAR, 11, 2, 2000);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendValueReduced_int_maxWidthTooSmall() throws Exception {
        builder.appendValueReduced(YEAR, 2, 0, 2000);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendValueReduced_int_maxWidthTooBig() throws Exception {
        builder.appendValueReduced(YEAR, 2, 11, 2000);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendValueReduced_int_maxWidthLessThanMin() throws Exception {
        builder.appendValueReduced(YEAR, 2, 1, 2000);
    }

    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void test_appendValueReduced_date_nullField() throws Exception {
        builder.appendValueReduced(null, 2, 2, LocalDate.of(2000, 1, 1));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_appendValueReduced_date_nullDate() throws Exception {
        builder.appendValueReduced(YEAR, 2, 2, null);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendValueReduced_date_minWidthTooSmall() throws Exception {
        builder.appendValueReduced(YEAR, 0, 2, LocalDate.of(2000, 1, 1));
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendValueReduced_date_minWidthTooBig() throws Exception {
        builder.appendValueReduced(YEAR, 11, 2, LocalDate.of(2000, 1, 1));
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendValueReduced_date_maxWidthTooSmall() throws Exception {
        builder.appendValueReduced(YEAR, 2, 0, LocalDate.of(2000, 1, 1));
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendValueReduced_date_maxWidthTooBig() throws Exception {
        builder.appendValueReduced(YEAR, 2, 11, LocalDate.of(2000, 1, 1));
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendValueReduced_date_maxWidthLessThanMin() throws Exception {
        builder.appendValueReduced(YEAR, 2, 1, LocalDate.of(2000, 1, 1));
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void test_appendFraction_4arg_nullRule() throws Exception {
        builder.appendFraction(null, 1, 9, false);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendFraction_4arg_invalidRuleNotFixedSet() throws Exception {
        builder.appendFraction(DAY_OF_MONTH, 1, 9, false);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendFraction_4arg_minTooSmall() throws Exception {
        builder.appendFraction(MINUTE_OF_HOUR, -1, 9, false);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendFraction_4arg_minTooBig() throws Exception {
        builder.appendFraction(MINUTE_OF_HOUR, 10, 9, false);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendFraction_4arg_maxTooSmall() throws Exception {
        builder.appendFraction(MINUTE_OF_HOUR, 0, -1, false);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendFraction_4arg_maxTooBig() throws Exception {
        builder.appendFraction(MINUTE_OF_HOUR, 1, 10, false);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendFraction_4arg_maxWidthMinWidth() throws Exception {
        builder.appendFraction(MINUTE_OF_HOUR, 9, 3, false);
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void test_appendText_1arg_null() throws Exception {
        builder.appendText(null);
    }

    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void test_appendText_2arg_nullRule() throws Exception {
        builder.appendText(null, TextStyle.SHORT);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_appendText_2arg_nullStyle() throws Exception {
        builder.appendText(MONTH_OF_YEAR, (TextStyle) null);
    }

    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void test_appendTextMap_nullRule() throws Exception {
        builder.appendText(null, new HashMap<>());
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_appendTextMap_nullStyle() throws Exception {
        builder.appendText(MONTH_OF_YEAR, (Map<Long, String>) null);
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name="offsetPatterns")
    Object[][] data_offsetPatterns() {
        return new Object[][] {
                {"+HH", 2, 0, 0, "+02"},
                {"+HH", -2, 0, 0, "-02"},
                {"+HH", 2, 30, 0, "+02"},
                {"+HH", 2, 0, 45, "+02"},
                {"+HH", 2, 30, 45, "+02"},

                {"+HHmm", 2, 0, 0, "+02"},
                {"+HHmm", -2, 0, 0, "-02"},
                {"+HHmm", 2, 30, 0, "+0230"},
                {"+HHmm", 2, 0, 45, "+02"},
                {"+HHmm", 2, 30, 45, "+0230"},

                {"+HH:mm", 2, 0, 0, "+02"},
                {"+HH:mm", -2, 0, 0, "-02"},
                {"+HH:mm", 2, 30, 0, "+02:30"},
                {"+HH:mm", 2, 0, 45, "+02"},
                {"+HH:mm", 2, 30, 45, "+02:30"},

                {"+HHMM", 2, 0, 0, "+0200"},
                {"+HHMM", -2, 0, 0, "-0200"},
                {"+HHMM", 2, 30, 0, "+0230"},
                {"+HHMM", 2, 0, 45, "+0200"},
                {"+HHMM", 2, 30, 45, "+0230"},

                {"+HH:MM", 2, 0, 0, "+02:00"},
                {"+HH:MM", -2, 0, 0, "-02:00"},
                {"+HH:MM", 2, 30, 0, "+02:30"},
                {"+HH:MM", 2, 0, 45, "+02:00"},
                {"+HH:MM", 2, 30, 45, "+02:30"},

                {"+HHMMss", 2, 0, 0, "+0200"},
                {"+HHMMss", -2, 0, 0, "-0200"},
                {"+HHMMss", 2, 30, 0, "+0230"},
                {"+HHMMss", 2, 0, 45, "+020045"},
                {"+HHMMss", 2, 30, 45, "+023045"},

                {"+HH:MM:ss", 2, 0, 0, "+02:00"},
                {"+HH:MM:ss", -2, 0, 0, "-02:00"},
                {"+HH:MM:ss", 2, 30, 0, "+02:30"},
                {"+HH:MM:ss", 2, 0, 45, "+02:00:45"},
                {"+HH:MM:ss", 2, 30, 45, "+02:30:45"},

                {"+HHMMSS", 2, 0, 0, "+020000"},
                {"+HHMMSS", -2, 0, 0, "-020000"},
                {"+HHMMSS", 2, 30, 0, "+023000"},
                {"+HHMMSS", 2, 0, 45, "+020045"},
                {"+HHMMSS", 2, 30, 45, "+023045"},

                {"+HH:MM:SS", 2, 0, 0, "+02:00:00"},
                {"+HH:MM:SS", -2, 0, 0, "-02:00:00"},
                {"+HH:MM:SS", 2, 30, 0, "+02:30:00"},
                {"+HH:MM:SS", 2, 0, 45, "+02:00:45"},
                {"+HH:MM:SS", 2, 30, 45, "+02:30:45"},

                {"+HHmmss", 2, 0, 0, "+02"},
                {"+HHmmss", -2, 0, 0, "-02"},
                {"+HHmmss", 2, 30, 0, "+0230"},
                {"+HHmmss", 2, 0, 45, "+020045"},
                {"+HHmmss", 2, 30, 45, "+023045"},

                {"+HH:mm:ss", 2, 0, 0, "+02"},
                {"+HH:mm:ss", -2, 0, 0, "-02"},
                {"+HH:mm:ss", 2, 30, 0, "+02:30"},
                {"+HH:mm:ss", 2, 0, 45, "+02:00:45"},
                {"+HH:mm:ss", 2, 30, 45, "+02:30:45"},

                {"+H", 2, 0, 0, "+2"},
                {"+H", -2, 0, 0, "-2"},
                {"+H", 2, 30, 0, "+2"},
                {"+H", 2, 0, 45, "+2"},
                {"+H", 2, 30, 45, "+2"},
                {"+H", 12, 0, 0, "+12"},
                {"+H", -12, 0, 0, "-12"},
                {"+H", 12, 30, 0, "+12"},
                {"+H", 12, 0, 45, "+12"},
                {"+H", 12, 30, 45, "+12"},

                {"+Hmm", 2, 0, 0, "+2"},
                {"+Hmm", -2, 0, 0, "-2"},
                {"+Hmm", 2, 30, 0, "+230"},
                {"+Hmm", 2, 0, 45, "+2"},
                {"+Hmm", 2, 30, 45, "+230"},
                {"+Hmm", 12, 0, 0, "+12"},
                {"+Hmm", -12, 0, 0, "-12"},
                {"+Hmm", 12, 30, 0, "+1230"},
                {"+Hmm", 12, 0, 45, "+12"},
                {"+Hmm", 12, 30, 45, "+1230"},

                {"+H:mm", 2, 0, 0, "+2"},
                {"+H:mm", -2, 0, 0, "-2"},
                {"+H:mm", 2, 30, 0, "+2:30"},
                {"+H:mm", 2, 0, 45, "+2"},
                {"+H:mm", 2, 30, 45, "+2:30"},
                {"+H:mm", 12, 0, 0, "+12"},
                {"+H:mm", -12, 0, 0, "-12"},
                {"+H:mm", 12, 30, 0, "+12:30"},
                {"+H:mm", 12, 0, 45, "+12"},
                {"+H:mm", 12, 30, 45, "+12:30"},

                {"+HMM", 2, 0, 0, "+200"},
                {"+HMM", -2, 0, 0, "-200"},
                {"+HMM", 2, 30, 0, "+230"},
                {"+HMM", 2, 0, 45, "+200"},
                {"+HMM", 2, 30, 45, "+230"},
                {"+HMM", 12, 0, 0, "+1200"},
                {"+HMM", -12, 0, 0, "-1200"},
                {"+HMM", 12, 30, 0, "+1230"},
                {"+HMM", 12, 0, 45, "+1200"},
                {"+HMM", 12, 30, 45, "+1230"},

                {"+H:MM", 2, 0, 0, "+2:00"},
                {"+H:MM", -2, 0, 0, "-2:00"},
                {"+H:MM", 2, 30, 0, "+2:30"},
                {"+H:MM", 2, 0, 45, "+2:00"},
                {"+H:MM", 2, 30, 45, "+2:30"},
                {"+H:MM", 12, 0, 0, "+12:00"},
                {"+H:MM", -12, 0, 0, "-12:00"},
                {"+H:MM", 12, 30, 0, "+12:30"},
                {"+H:MM", 12, 0, 45, "+12:00"},
                {"+H:MM", 12, 30, 45, "+12:30"},

                {"+HMMss", 2, 0, 0, "+200"},
                {"+HMMss", -2, 0, 0, "-200"},
                {"+HMMss", 2, 30, 0, "+230"},
                {"+HMMss", 2, 0, 45, "+20045"},
                {"+HMMss", 2, 30, 45, "+23045"},
                {"+HMMss", 12, 0, 0, "+1200"},
                {"+HMMss", -12, 0, 0, "-1200"},
                {"+HMMss", 12, 30, 0, "+1230"},
                {"+HMMss", 12, 0, 45, "+120045"},
                {"+HMMss", 12, 30, 45, "+123045"},

                {"+H:MM:ss", 2, 0, 0, "+2:00"},
                {"+H:MM:ss", -2, 0, 0, "-2:00"},
                {"+H:MM:ss", 2, 30, 0, "+2:30"},
                {"+H:MM:ss", 2, 0, 45, "+2:00:45"},
                {"+H:MM:ss", 2, 30, 45, "+2:30:45"},
                {"+H:MM:ss", 12, 0, 0, "+12:00"},
                {"+H:MM:ss", -12, 0, 0, "-12:00"},
                {"+H:MM:ss", 12, 30, 0, "+12:30"},
                {"+H:MM:ss", 12, 0, 45, "+12:00:45"},
                {"+H:MM:ss", 12, 30, 45, "+12:30:45"},

                {"+HMMSS", 2, 0, 0, "+20000"},
                {"+HMMSS", -2, 0, 0, "-20000"},
                {"+HMMSS", 2, 30, 0, "+23000"},
                {"+HMMSS", 2, 0, 45, "+20045"},
                {"+HMMSS", 2, 30, 45, "+23045"},
                {"+HMMSS", 12, 0, 0, "+120000"},
                {"+HMMSS", -12, 0, 0, "-120000"},
                {"+HMMSS", 12, 30, 0, "+123000"},
                {"+HMMSS", 12, 0, 45, "+120045"},
                {"+HMMSS", 12, 30, 45, "+123045"},

                {"+H:MM:SS", 2, 0, 0, "+2:00:00"},
                {"+H:MM:SS", -2, 0, 0, "-2:00:00"},
                {"+H:MM:SS", 2, 30, 0, "+2:30:00"},
                {"+H:MM:SS", 2, 0, 45, "+2:00:45"},
                {"+H:MM:SS", 2, 30, 45, "+2:30:45"},
                {"+H:MM:SS", 12, 0, 0, "+12:00:00"},
                {"+H:MM:SS", -12, 0, 0, "-12:00:00"},
                {"+H:MM:SS", 12, 30, 0, "+12:30:00"},
                {"+H:MM:SS", 12, 0, 45, "+12:00:45"},
                {"+H:MM:SS", 12, 30, 45, "+12:30:45"},

                {"+Hmmss", 2, 0, 0, "+2"},
                {"+Hmmss", -2, 0, 0, "-2"},
                {"+Hmmss", 2, 30, 0, "+230"},
                {"+Hmmss", 2, 0, 45, "+20045"},
                {"+Hmmss", 2, 30, 45, "+23045"},
                {"+Hmmss", 12, 0, 0, "+12"},
                {"+Hmmss", -12, 0, 0, "-12"},
                {"+Hmmss", 12, 30, 0, "+1230"},
                {"+Hmmss", 12, 0, 45, "+120045"},
                {"+Hmmss", 12, 30, 45, "+123045"},

                {"+H:mm:ss", 2, 0, 0, "+2"},
                {"+H:mm:ss", -2, 0, 0, "-2"},
                {"+H:mm:ss", 2, 30, 0, "+2:30"},
                {"+H:mm:ss", 2, 0, 45, "+2:00:45"},
                {"+H:mm:ss", 2, 30, 45, "+2:30:45"},
                {"+H:mm:ss", 12, 0, 0, "+12"},
                {"+H:mm:ss", -12, 0, 0, "-12"},
                {"+H:mm:ss", 12, 30, 0, "+12:30"},
                {"+H:mm:ss", 12, 0, 45, "+12:00:45"},
                {"+H:mm:ss", 12, 30, 45, "+12:30:45"},


        };
    }

    @Test(dataProvider="offsetPatterns")
    public void test_appendOffset_format(String pattern, int h, int m, int s, String expected) throws Exception {
        builder.appendOffset(pattern, "Z");
        DateTimeFormatter f = builder.toFormatter();
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(h, m, s);
        assertEquals(f.format(offset), expected);
    }

    @Test(dataProvider="offsetPatterns")
    public void test_appendOffset_parse(String pattern, int h, int m, int s, String expected) throws Exception {
        builder.appendOffset(pattern, "Z");
        DateTimeFormatter f = builder.toFormatter();
        ZoneOffset parsed = f.parse(expected, ZoneOffset::from);
        assertEquals(f.format(parsed), expected);
    }

    @DataProvider(name="badOffsetPatterns")
    Object[][] data_badOffsetPatterns() {
        return new Object[][] {
            {"HH"},
            {"HHMM"},
            {"HH:MM"},
            {"HHMMss"},
            {"HH:MM:ss"},
            {"HHMMSS"},
            {"HH:MM:SS"},
            {"+HHM"},
            {"+A"},
        };
    }

    @Test(dataProvider="badOffsetPatterns", expectedExceptions=IllegalArgumentException.class)
    public void test_appendOffset_badPattern(String pattern) throws Exception {
        builder.appendOffset(pattern, "Z");
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_appendOffset_3arg_nullText() throws Exception {
        builder.appendOffset("+HH:MM", null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_appendOffset_3arg_nullPattern() throws Exception {
        builder.appendOffset(null, "Z");
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name = "formatGenericTimeZonePatterns")
    Object[][] data_formatGenericNonLocationPatterns() {
        return new Object[][] {
                {"v", "America/Los_Angeles", "PT"},
                {"vvvv", "America/Los_Angeles", "Pacific Time"},
                {"v", "America/New_York", "ET"},
                {"vvvv", "America/New_York", "Eastern Time"},
        };
    }

    @Test(dataProvider = "formatGenericTimeZonePatterns")
    public void test_appendZoneText_formatGenericTimeZonePatterns(String pattern, String input, String expected) {
        ZonedDateTime zdt = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of(input));
        DateTimeFormatter df = DateTimeFormatter.ofPattern(pattern, Locale.US);
        assertEquals(zdt.format(df), expected);
    }

    @DataProvider(name = "parseGenericTimeZonePatterns")
    Object[][]  data_parseGenericTimeZonePatterns() {
        return new Object[][] {
                {"yyyy DDD HH mm v", LocalDateTime.of(2015, Month.MARCH, 10, 12, 13), ZoneId.of("America/Los_Angeles"),
                 "2015 069 12 13 PT"},
                {"yyyy DDD HH mm vvvv", LocalDateTime.of(2015, Month.MARCH, 10, 12, 13), ZoneId.of("America/Los_Angeles"),
                 "2015 069 12 13 Pacific Time"},
                {"yyyy DDD HH mm v", LocalDateTime.of(2015, Month.NOVEMBER, 10, 12, 13), ZoneId.of("America/Los_Angeles"),
                 "2015 314 12 13 PT"},
                {"yyyy DDD HH mm vvvv", LocalDateTime.of(2015, Month.NOVEMBER, 10, 12, 13), ZoneId.of("America/Los_Angeles"),
                 "2015 314 12 13 Pacific Time"},
        };
    }

    @Test(dataProvider = "parseGenericTimeZonePatterns")
    public void test_appendZoneText_parseGenericTimeZonePatterns(String pattern, LocalDateTime ldt, ZoneId zId, String input) {
        DateTimeFormatter df = new DateTimeFormatterBuilder().appendPattern(pattern).toFormatter(Locale.US);
        ZonedDateTime expected = ZonedDateTime.parse(input, df);
        ZonedDateTime actual = ZonedDateTime.of(ldt, zId);
        assertEquals(actual, expected);
    }

    @DataProvider(name = "formatNonGenericTimeZonePatterns_1")
    Object[][]  data_formatNonGenericTimeZonePatterns_1() {
        return new Object[][] {
                {"yyyy-MM-dd HH:mm:ss z", LocalDateTime.of(2015, Month.NOVEMBER, 1, 0, 30),
                 "2015-11-01 00:30:00 PDT"},
                {"yyyy-MM-dd HH:mm:ss z", LocalDateTime.of(2015, Month.NOVEMBER, 1, 1, 30),
                 "2015-11-01 01:30:00 PDT"},
                {"yyyy-MM-dd HH:mm:ss z", LocalDateTime.of(2015, Month.NOVEMBER, 1, 2, 30),
                 "2015-11-01 02:30:00 PST"},
                {"yyyy-MM-dd HH:mm:ss zzzz", LocalDateTime.of(2015, Month.NOVEMBER, 1, 0, 30),
                 "2015-11-01 00:30:00 Pacific Daylight Time"},
                {"yyyy-MM-dd HH:mm:ss zzzz", LocalDateTime.of(2015, Month.NOVEMBER, 1, 1, 30),
                 "2015-11-01 01:30:00 Pacific Daylight Time"},
                {"yyyy-MM-dd HH:mm:ss zzzz", LocalDateTime.of(2015, Month.NOVEMBER, 1, 2, 30),
                 "2015-11-01 02:30:00 Pacific Standard Time"},
        };
    }

    @Test(dataProvider = "formatNonGenericTimeZonePatterns_1")
    public void test_appendZoneText_parseNonGenricTimeZonePatterns_1(String pattern, LocalDateTime ldt, String expected) {
        ZoneId  zId = ZoneId.of("America/Los_Angeles");
        DateTimeFormatter df = new DateTimeFormatterBuilder().appendPattern(pattern).toFormatter(Locale.US);
        ZonedDateTime zdt = ZonedDateTime.of(ldt, zId);
        String actual = df.format(zdt);
        assertEquals(actual, expected);
    }

    @DataProvider(name = "formatNonGenericTimeZonePatterns_2")
    Object[][]  data_formatNonGenericTimeZonePatterns_2() {
        return new Object[][] {
                {"yyyy-MM-dd HH:mm:ss z", LocalDateTime.of(2015, Month.NOVEMBER, 1, 0, 30),
                 "2015-11-01 00:30:00 PDT"},
                {"yyyy-MM-dd HH:mm:ss z", LocalDateTime.of(2015, Month.NOVEMBER, 1, 1, 30),
                 "2015-11-01 01:30:00 PT"},
                {"yyyy-MM-dd HH:mm:ss z", LocalDateTime.of(2015, Month.NOVEMBER, 1, 2, 30),
                 "2015-11-01 02:30:00 PST"},
                {"yyyy-MM-dd HH:mm:ss zzzz", LocalDateTime.of(2015, Month.NOVEMBER, 1, 0, 30),
                 "2015-11-01 00:30:00 Pacific Daylight Time"},
                {"yyyy-MM-dd HH:mm:ss zzzz", LocalDateTime.of(2015, Month.NOVEMBER, 1, 1, 30),
                 "2015-11-01 01:30:00 Pacific Time"},
                {"yyyy-MM-dd HH:mm:ss zzzz", LocalDateTime.of(2015, Month.NOVEMBER, 1, 2, 30),
                 "2015-11-01 02:30:00 Pacific Standard Time"},
        };
    }

    @Test(dataProvider = "formatNonGenericTimeZonePatterns_2")
    public void test_appendZoneText_parseNonGenricTimeZonePatterns_2(String pattern, LocalDateTime ldt, String expected) {
        ZoneId  zId = ZoneId.of("America/Los_Angeles");
        DateTimeFormatter df = DateTimeFormatter.ofPattern(pattern, Locale.US).withZone(zId);
        String actual = df.format(ldt);
        assertEquals(actual, expected);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_appendZoneText_1arg_nullText() throws Exception {
        builder.appendZoneText(null);
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @Test
    public void test_padNext_1arg() {
        builder.appendValue(MONTH_OF_YEAR).appendLiteral(':').padNext(2).appendValue(DAY_OF_MONTH);
        assertEquals(builder.toFormatter().format(LocalDate.of(2013, 2, 1)), "2: 1");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_padNext_1arg_invalidWidth() throws Exception {
        builder.padNext(0);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_padNext_2arg_dash() throws Exception {
        builder.appendValue(MONTH_OF_YEAR).appendLiteral(':').padNext(2, '-').appendValue(DAY_OF_MONTH);
        assertEquals(builder.toFormatter().format(LocalDate.of(2013, 2, 1)), "2:-1");
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_padNext_2arg_invalidWidth() throws Exception {
        builder.padNext(0, '-');
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_padOptional() throws Exception {
        builder.appendValue(MONTH_OF_YEAR).appendLiteral(':')
                .padNext(5).optionalStart().appendValue(DAY_OF_MONTH).optionalEnd()
                .appendLiteral(':').appendValue(YEAR);
        assertEquals(builder.toFormatter().format(LocalDate.of(2013, 2, 1)), "2:    1:2013");
        assertEquals(builder.toFormatter().format(YearMonth.of(2013, 2)), "2:     :2013");
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @Test(expectedExceptions=IllegalStateException.class)
    public void test_optionalEnd_noStart() throws Exception {
        builder.optionalEnd();
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    @DataProvider(name="validPatterns")
    Object[][] dataValid() {
        return new Object[][] {
            {"'a'"},
            {"''"},
            {"'!'"},
            {"!"},
            {"'#'"},

            {"'hello_people,][)('"},
            {"'hi'"},
            {"'yyyy'"},
            {"''''"},
            {"'o''clock'"},

            {"G"},
            {"GG"},
            {"GGG"},
            {"GGGG"},
            {"GGGGG"},

            {"y"},
            {"yy"},
            {"yyy"},
            {"yyyy"},
            {"yyyyy"},

            {"M"},
            {"MM"},
            {"MMM"},
            {"MMMM"},
            {"MMMMM"},

            {"L"},
            {"LL"},
            {"LLL"},
            {"LLLL"},
            {"LLLLL"},

            {"D"},
            {"DD"},
            {"DDD"},

            {"d"},
            {"dd"},

            {"F"},

            {"Q"},
            {"QQ"},
            {"QQQ"},
            {"QQQQ"},
            {"QQQQQ"},

            {"q"},
            {"qq"},
            {"qqq"},
            {"qqqq"},
            {"qqqqq"},

            {"E"},
            {"EE"},
            {"EEE"},
            {"EEEE"},
            {"EEEEE"},

            {"e"},
            {"ee"},
            {"eee"},
            {"eeee"},
            {"eeeee"},

            {"c"},
            {"ccc"},
            {"cccc"},
            {"ccccc"},

            {"a"},

            {"H"},
            {"HH"},

            {"K"},
            {"KK"},

            {"k"},
            {"kk"},

            {"h"},
            {"hh"},

            {"m"},
            {"mm"},

            {"s"},
            {"ss"},

            {"S"},
            {"SS"},
            {"SSS"},
            {"SSSSSSSSS"},

            {"A"},
            {"AA"},
            {"AAA"},

            {"n"},
            {"nn"},
            {"nnn"},

            {"N"},
            {"NN"},
            {"NNN"},

            {"z"},
            {"zz"},
            {"zzz"},
            {"zzzz"},

            {"VV"},

            {"Z"},
            {"ZZ"},
            {"ZZZ"},

            {"X"},
            {"XX"},
            {"XXX"},
            {"XXXX"},
            {"XXXXX"},

            {"x"},
            {"xx"},
            {"xxx"},
            {"xxxx"},
            {"xxxxx"},

            {"ppH"},
            {"pppDD"},

            {"yyyy[-MM[-dd"},
            {"yyyy[-MM[-dd]]"},
            {"yyyy[-MM[]-dd]"},

            {"yyyy-MM-dd'T'HH:mm:ss.SSS"},

            {"e"},
            {"w"},
            {"ww"},
            {"W"},
            {"W"},

            {"g"},
            {"ggggg"},
        };
    }

    @Test(dataProvider="validPatterns")
    public void test_appendPattern_valid(String input) throws Exception {
        builder.appendPattern(input);  // test is for no error here
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="invalidPatterns")
    Object[][] dataInvalid() {
        return new Object[][] {
            {"'"},
            {"'hello"},
            {"'hel''lo"},
            {"'hello''"},
            {"{"},
            {"}"},
            {"{}"},
            {"#"},
            {"]"},
            {"yyyy]"},
            {"yyyy]MM"},
            {"yyyy[MM]]"},

            {"aa"},
            {"aaa"},
            {"aaaa"},
            {"aaaaa"},
            {"aaaaaa"},
            {"MMMMMM"},
            {"QQQQQQ"},
            {"qqqqqq"},
            {"EEEEEE"},
            {"eeeeee"},
            {"cc"},
            {"cccccc"},
            {"ddd"},
            {"DDDD"},
            {"FF"},
            {"FFF"},
            {"hhh"},
            {"HHH"},
            {"kkk"},
            {"KKK"},
            {"mmm"},
            {"sss"},
            {"OO"},
            {"OOO"},
            {"OOOOO"},
            {"XXXXXX"},
            {"zzzzz"},
            {"ZZZZZZ"},

            {"RO"},

            {"p"},
            {"pp"},
            {"p:"},

            {"f"},
            {"ff"},
            {"f:"},
            {"fy"},
            {"fa"},
            {"fM"},

            {"www"},
            {"WW"},

            {"vv"},
            {"vvv"},
        };
    }

    @Test(dataProvider="invalidPatterns", expectedExceptions=IllegalArgumentException.class)
    public void test_appendPattern_invalid(String input) throws Exception {
        builder.appendPattern(input);  // test is for error here
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="patternPrint")
    Object[][] data_patternPrint() {
        return new Object[][] {
            {"Q", date(2012, 2, 10), "1"},
            {"QQ", date(2012, 2, 10), "01"},
            {"QQQ", date(2012, 2, 10), "Q1"},
            {"QQQQ", date(2012, 2, 10), "1st quarter"},
            {"QQQQQ", date(2012, 2, 10), "1"},
        };
    }

    @Test(dataProvider="patternPrint")
    public void test_appendPattern_patternPrint(String input, Temporal temporal, String expected) throws Exception {
        DateTimeFormatter f = builder.appendPattern(input).toFormatter(Locale.UK);
        String test = f.format(temporal);
        assertEquals(test, expected);
    }

    private static Temporal date(int y, int m, int d) {
        return LocalDate.of(y, m, d);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="modJulianFieldPattern")
    Object[][] data_modJuilanFieldPattern() {
        return new Object[][] {
            {"g", "1"},
            {"g", "123456"},
            {"gggggg", "123456"},
        };
    }

    @Test(dataProvider="modJulianFieldPattern")
    public void test_modJulianFieldPattern(String pattern, String input) throws Exception {
        DateTimeFormatter.ofPattern(pattern).parse(input);
    }

    @DataProvider(name="modJulianFieldValues")
    Object[][] data_modJuilanFieldValues() {
        return new Object[][] {
            {1970, 1, 1, "40587"},
            {1858, 11, 17, "0"},
            {1858, 11, 16, "-1"},
        };
    }

    @Test(dataProvider="modJulianFieldValues")
    public void test_modJulianFieldValues(int y, int m, int d, String expected) throws Exception {
        DateTimeFormatter df = new DateTimeFormatterBuilder().appendPattern("g").toFormatter();
         assertEquals(LocalDate.of(y, m, d).format(df), expected);
    }
    //----------------------------------------------------------------------
    @DataProvider(name="dayOfYearFieldValues")
    Object[][] data_dayOfYearFieldValues() {
        return new Object[][] {
                {2016, 1, 1, "D", "1"},
                {2016, 1, 31, "D", "31"},
                {2016, 1, 1, "DD", "01"},
                {2016, 1, 31, "DD", "31"},
                {2016, 4, 9, "DD", "100"},
                {2016, 1, 1, "DDD", "001"},
                {2016, 1, 31, "DDD", "031"},
                {2016, 4, 9, "DDD", "100"},
        };
    }

    @Test(dataProvider="dayOfYearFieldValues")
    public void test_dayOfYearFieldValues(int y, int m, int d, String pattern, String expected) throws Exception {
        DateTimeFormatter df = new DateTimeFormatterBuilder().appendPattern(pattern).toFormatter();
        assertEquals(LocalDate.of(y, m, d).format(df), expected);
    }

    @DataProvider(name="dayOfYearFieldAdjacentParsingValues")
    Object[][] data_dayOfYearFieldAdjacentParsingValues() {
        return new Object[][] {
            {"20160281015", LocalDateTime.of(2016, 1, 28, 10, 15)},
            {"20161001015", LocalDateTime.of(2016, 4, 9, 10, 15)},
        };
    }

    @Test(dataProvider="dayOfYearFieldAdjacentParsingValues")
    public void test_dayOfYearFieldAdjacentValueParsing(String input, LocalDateTime expected) {
        DateTimeFormatter df = new DateTimeFormatterBuilder().appendPattern("yyyyDDDHHmm").toFormatter();
        LocalDateTime actual = LocalDateTime.parse(input, df);
        assertEquals(actual, expected);
    }

    @Test(expectedExceptions = DateTimeParseException.class)
    public void test_dayOfYearFieldInvalidValue() {
        DateTimeFormatter.ofPattern("DDD").parse("1234");
    }

    @Test(expectedExceptions = DateTimeParseException.class)
    public void test_dayOfYearFieldInvalidAdacentValueParsingPattern() {
        // patterns D and DD will not take part in adjacent value parsing
        DateTimeFormatter.ofPattern("yyyyDDHHmmss").parse("201610123456");
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="secondsPattern")
    Object[][] data_secondsPattern() {
        return new Object[][] {
                {"A", "1", LocalTime.ofNanoOfDay(1_000_000)},
                {"A", "100000", LocalTime.ofSecondOfDay(100)},
                {"AA", "01", LocalTime.ofNanoOfDay(1_000_000)},
                {"AA", "100000", LocalTime.ofSecondOfDay(100)},
                {"AAAAAA", "100000", LocalTime.ofSecondOfDay(100)},
                {"HHmmssn", "0000001", LocalTime.ofNanoOfDay(1)},
                {"HHmmssn", "000000111", LocalTime.ofNanoOfDay(111)},
                {"HHmmssnn", "00000001", LocalTime.ofNanoOfDay(1)},
                {"HHmmssnn", "0000001111", LocalTime.ofNanoOfDay(1111)},
                {"HHmmssnnnnnn", "000000111111", LocalTime.ofNanoOfDay(111_111)},
                {"N", "1", LocalTime.ofNanoOfDay(1)},
                {"N", "100000", LocalTime.ofNanoOfDay(100_000)},
                {"NN", "01", LocalTime.ofNanoOfDay(1)},
                {"NN", "100000", LocalTime.ofNanoOfDay(100_000)},
                {"NNNNNN", "100000", LocalTime.ofNanoOfDay(100_000)},
        };
    }

    @Test(dataProvider="secondsPattern")
    public void test_secondsPattern(String pattern, String input, LocalTime expected) throws Exception {
        DateTimeFormatter df = new DateTimeFormatterBuilder().appendPattern(pattern).toFormatter();
        assertEquals(LocalTime.parse(input, df), expected);
    }

    @DataProvider(name="secondsValues")
    Object[][] data_secondsValues() {
        return new Object[][] {
                {"A", 1, "1000"},
                {"n", 1, "0"},
                {"N", 1, "1000000000"},
        };
    }

    @Test(dataProvider="secondsValues")
    public void test_secondsValues(String pattern, int seconds , String expected) throws Exception {
        DateTimeFormatter df = new DateTimeFormatterBuilder().appendPattern(pattern).toFormatter();
        assertEquals(LocalTime.ofSecondOfDay(seconds).format(df), expected);
    }

    @Test(expectedExceptions = DateTimeParseException.class)
    public void test_secondsPatternInvalidAdacentValueParsingPattern() {
        // patterns A*, N*, n* will not take part in adjacent value parsing
        DateTimeFormatter.ofPattern("yyyyAA").parse("201610");
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_adjacent_strict_firstFixedWidth() throws Exception {
        // succeeds because both number elements are fixed width
        DateTimeFormatter f = builder.appendValue(HOUR_OF_DAY, 2).appendValue(MINUTE_OF_HOUR, 2).appendLiteral('9').toFormatter(Locale.UK);
        ParsePosition pp = new ParsePosition(0);
        TemporalAccessor parsed = f.parseUnresolved("12309", pp);
        assertEquals(pp.getErrorIndex(), -1);
        assertEquals(pp.getIndex(), 5);
        assertEquals(parsed.getLong(HOUR_OF_DAY), 12L);
        assertEquals(parsed.getLong(MINUTE_OF_HOUR), 30L);
    }

    @Test
    public void test_adjacent_strict_firstVariableWidth_success() throws Exception {
        // succeeds greedily parsing variable width, then fixed width, to non-numeric Z
        DateTimeFormatter f = builder.appendValue(HOUR_OF_DAY).appendValue(MINUTE_OF_HOUR, 2).appendLiteral('Z').toFormatter(Locale.UK);
        ParsePosition pp = new ParsePosition(0);
        TemporalAccessor parsed = f.parseUnresolved("12309Z", pp);
        assertEquals(pp.getErrorIndex(), -1);
        assertEquals(pp.getIndex(), 6);
        assertEquals(parsed.getLong(HOUR_OF_DAY), 123L);
        assertEquals(parsed.getLong(MINUTE_OF_HOUR), 9L);
    }

    @Test
    public void test_adjacent_strict_firstVariableWidth_fails() throws Exception {
        // fails because literal is a number and variable width parse greedily absorbs it
        DateTimeFormatter f = builder.appendValue(HOUR_OF_DAY).appendValue(MINUTE_OF_HOUR, 2).appendLiteral('9').toFormatter(Locale.UK);
        ParsePosition pp = new ParsePosition(0);
        TemporalAccessor parsed = f.parseUnresolved("12309", pp);
        assertEquals(pp.getErrorIndex(), 5);
        assertEquals(parsed, null);
    }

    @Test
    public void test_adjacent_lenient() throws Exception {
        // succeeds because both number elements are fixed width even in lenient mode
        DateTimeFormatter f = builder.parseLenient().appendValue(HOUR_OF_DAY, 2).appendValue(MINUTE_OF_HOUR, 2).appendLiteral('9').toFormatter(Locale.UK);
        ParsePosition pp = new ParsePosition(0);
        TemporalAccessor parsed = f.parseUnresolved("12309", pp);
        assertEquals(pp.getErrorIndex(), -1);
        assertEquals(pp.getIndex(), 5);
        assertEquals(parsed.getLong(HOUR_OF_DAY), 12L);
        assertEquals(parsed.getLong(MINUTE_OF_HOUR), 30L);
    }

    @Test
    public void test_adjacent_lenient_firstVariableWidth_success() throws Exception {
        // succeeds greedily parsing variable width, then fixed width, to non-numeric Z
        DateTimeFormatter f = builder.parseLenient().appendValue(HOUR_OF_DAY).appendValue(MINUTE_OF_HOUR, 2).appendLiteral('Z').toFormatter(Locale.UK);
        ParsePosition pp = new ParsePosition(0);
        TemporalAccessor parsed = f.parseUnresolved("12309Z", pp);
        assertEquals(pp.getErrorIndex(), -1);
        assertEquals(pp.getIndex(), 6);
        assertEquals(parsed.getLong(HOUR_OF_DAY), 123L);
        assertEquals(parsed.getLong(MINUTE_OF_HOUR), 9L);
    }

    @Test
    public void test_adjacent_lenient_firstVariableWidth_fails() throws Exception {
        // fails because literal is a number and variable width parse greedily absorbs it
        DateTimeFormatter f = builder.parseLenient().appendValue(HOUR_OF_DAY).appendValue(MINUTE_OF_HOUR, 2).appendLiteral('9').toFormatter(Locale.UK);
        ParsePosition pp = new ParsePosition(0);
        TemporalAccessor parsed = f.parseUnresolved("12309", pp);
        assertEquals(pp.getErrorIndex(), 5);
        assertEquals(parsed, null);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_adjacent_strict_fractionFollows() throws Exception {
        // succeeds because hour/min are fixed width
        DateTimeFormatter f = builder.appendValue(HOUR_OF_DAY, 2).appendValue(MINUTE_OF_HOUR, 2).appendFraction(NANO_OF_SECOND, 0, 3, false).toFormatter(Locale.UK);
        ParsePosition pp = new ParsePosition(0);
        TemporalAccessor parsed = f.parseUnresolved("1230567", pp);
        assertEquals(pp.getErrorIndex(), -1);
        assertEquals(pp.getIndex(), 7);
        assertEquals(parsed.getLong(HOUR_OF_DAY), 12L);
        assertEquals(parsed.getLong(MINUTE_OF_HOUR), 30L);
        assertEquals(parsed.getLong(NANO_OF_SECOND), 567_000_000L);
    }

    @Test
    public void test_adjacent_strict_fractionFollows_2digit() throws Exception {
        // succeeds because hour/min are fixed width
        DateTimeFormatter f = builder.appendValue(HOUR_OF_DAY, 2).appendValue(MINUTE_OF_HOUR, 2).appendFraction(NANO_OF_SECOND, 0, 3, false).toFormatter(Locale.UK);
        ParsePosition pp = new ParsePosition(0);
        TemporalAccessor parsed = f.parseUnresolved("123056", pp);
        assertEquals(pp.getErrorIndex(), -1);
        assertEquals(pp.getIndex(), 6);
        assertEquals(parsed.getLong(HOUR_OF_DAY), 12L);
        assertEquals(parsed.getLong(MINUTE_OF_HOUR), 30L);
        assertEquals(parsed.getLong(NANO_OF_SECOND), 560_000_000L);
    }

    @Test
    public void test_adjacent_strict_fractionFollows_0digit() throws Exception {
        // succeeds because hour/min are fixed width
        DateTimeFormatter f = builder.appendValue(HOUR_OF_DAY, 2).appendValue(MINUTE_OF_HOUR, 2).appendFraction(NANO_OF_SECOND, 0, 3, false).toFormatter(Locale.UK);
        ParsePosition pp = new ParsePosition(0);
        TemporalAccessor parsed = f.parseUnresolved("1230", pp);
        assertEquals(pp.getErrorIndex(), -1);
        assertEquals(pp.getIndex(), 4);
        assertEquals(parsed.getLong(HOUR_OF_DAY), 12L);
        assertEquals(parsed.getLong(MINUTE_OF_HOUR), 30L);
    }

    @Test
    public void test_adjacent_lenient_fractionFollows() throws Exception {
        // succeeds because hour/min are fixed width
        DateTimeFormatter f = builder.parseLenient().appendValue(HOUR_OF_DAY, 2).appendValue(MINUTE_OF_HOUR, 2).appendFraction(NANO_OF_SECOND, 3, 3, false).toFormatter(Locale.UK);
        ParsePosition pp = new ParsePosition(0);
        TemporalAccessor parsed = f.parseUnresolved("1230567", pp);
        assertEquals(pp.getErrorIndex(), -1);
        assertEquals(pp.getIndex(), 7);
        assertEquals(parsed.getLong(HOUR_OF_DAY), 12L);
        assertEquals(parsed.getLong(MINUTE_OF_HOUR), 30L);
        assertEquals(parsed.getLong(NANO_OF_SECOND), 567_000_000L);
    }

    @Test
    public void test_adjacent_lenient_fractionFollows_2digit() throws Exception {
        // succeeds because hour/min are fixed width
        DateTimeFormatter f = builder.parseLenient().appendValue(HOUR_OF_DAY, 2).appendValue(MINUTE_OF_HOUR, 2).appendFraction(NANO_OF_SECOND, 3, 3, false).toFormatter(Locale.UK);
        ParsePosition pp = new ParsePosition(0);
        TemporalAccessor parsed = f.parseUnresolved("123056", pp);
        assertEquals(pp.getErrorIndex(), -1);
        assertEquals(pp.getIndex(), 6);
        assertEquals(parsed.getLong(HOUR_OF_DAY), 12L);
        assertEquals(parsed.getLong(MINUTE_OF_HOUR), 30L);
        assertEquals(parsed.getLong(NANO_OF_SECOND), 560_000_000L);
    }

    @Test
    public void test_adjacent_lenient_fractionFollows_0digit() throws Exception {
        // succeeds because hour, min and fraction of seconds are fixed width
        DateTimeFormatter f = builder.parseLenient().appendValue(HOUR_OF_DAY, 2).appendValue(MINUTE_OF_HOUR, 2).appendFraction(NANO_OF_SECOND, 3, 3, false).toFormatter(Locale.UK);
        ParsePosition pp = new ParsePosition(0);
        TemporalAccessor parsed = f.parseUnresolved("1230", pp);
        assertEquals(pp.getErrorIndex(), -1);
        assertEquals(pp.getIndex(), 4);
        assertEquals(parsed.getLong(HOUR_OF_DAY), 12L);
        assertEquals(parsed.getLong(MINUTE_OF_HOUR), 30L);
    }

    @DataProvider(name="adjacentFractionParseData")
    Object[][] data_adjacent_fraction_parse() {
        return new Object[][] {
            {"20130812214600025", "yyyyMMddHHmmssSSS", LocalDateTime.of(2013, 8, 12, 21, 46, 00, 25000000)},
            {"201308122146000256", "yyyyMMddHHmmssSSSS", LocalDateTime.of(2013, 8, 12, 21, 46, 00, 25600000)},
        };
    }

    @Test(dataProvider = "adjacentFractionParseData")
    public void test_adjacent_fraction(String input, String pattern, LocalDateTime expected) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern);
        LocalDateTime actual = LocalDateTime.parse(input, dtf);
        assertEquals(actual, expected);
    }

    @DataProvider(name="lenientOffsetParseData")
    Object[][] data_lenient_offset_parse() {
        return new Object[][] {
            {"+HH", "+01", 3600},
            {"+HH", "+0101", 3660},
            {"+HH", "+010101", 3661},
            {"+HH", "+01", 3600},
            {"+HH", "+01:01", 3660},
            {"+HH", "+01:01:01", 3661},
            {"+HHmm", "+01", 3600},
            {"+HHmm", "+0101", 3660},
            {"+HHmm", "+010101", 3661},
            {"+HH:mm", "+01", 3600},
            {"+HH:mm", "+01:01", 3660},
            {"+HH:mm", "+01:01:01", 3661},
            {"+HHMM", "+01", 3600},
            {"+HHMM", "+0101", 3660},
            {"+HHMM", "+010101", 3661},
            {"+HH:MM", "+01", 3600},
            {"+HH:MM", "+01:01", 3660},
            {"+HH:MM", "+01:01:01", 3661},
            {"+HHMMss", "+01", 3600},
            {"+HHMMss", "+0101", 3660},
            {"+HHMMss", "+010101", 3661},
            {"+HH:MM:ss", "+01", 3600},
            {"+HH:MM:ss", "+01:01", 3660},
            {"+HH:MM:ss", "+01:01:01", 3661},
            {"+HHMMSS", "+01", 3600},
            {"+HHMMSS", "+0101", 3660},
            {"+HHMMSS", "+010101", 3661},
            {"+HH:MM:SS", "+01", 3600},
            {"+HH:MM:SS", "+01:01", 3660},
            {"+HH:MM:SS", "+01:01:01", 3661},
            {"+HHmmss", "+01", 3600},
            {"+HHmmss", "+0101", 3660},
            {"+HHmmss", "+010101", 3661},
            {"+HH:mm:ss", "+01", 3600},
            {"+HH:mm:ss", "+01:01", 3660},
            {"+HH:mm:ss", "+01:01:01", 3661},

            {"+H", "+1", 3600},
            {"+H", "+101", 3660},
            {"+H", "+10101", 3661},
            {"+H", "+1:01", 3660},
            {"+H", "+1:01:01", 3661},
            {"+H", "+01", 3600},
            {"+H", "+0101", 3660},
            {"+H", "+010101", 3661},
            {"+H", "+01:01", 3660},
            {"+H", "+01:01:01", 3661},
            {"+Hmm", "+1", 3600},
            {"+Hmm", "+101", 3660},
            {"+Hmm", "+10101", 3661},
            {"+Hmm", "+01", 3600},
            {"+Hmm", "+0101", 3660},
            {"+Hmm", "+010101", 3661},
            {"+H:mm", "+1", 3600},
            {"+H:mm", "+1:01", 3660},
            {"+H:mm", "+1:01:01", 3661},
            {"+H:mm", "+01", 3600},
            {"+H:mm", "+01:01", 3660},
            {"+H:mm", "+01:01:01", 3661},
            {"+HMM", "+1", 3600},
            {"+HMM", "+101", 3660},
            {"+HMM", "+10101", 3661},
            {"+HMM", "+01", 3600},
            {"+HMM", "+0101", 3660},
            {"+HMM", "+010101", 3661},
            {"+H:MM", "+1", 3600},
            {"+H:MM", "+1:01", 3660},
            {"+H:MM", "+1:01:01", 3661},
            {"+H:MM", "+01", 3600},
            {"+H:MM", "+01:01", 3660},
            {"+H:MM", "+01:01:01", 3661},
            {"+HMMss", "+1", 3600},
            {"+HMMss", "+101", 3660},
            {"+HMMss", "+10101", 3661},
            {"+HMMss", "+01", 3600},
            {"+HMMss", "+0101", 3660},
            {"+HMMss", "+010101", 3661},
            {"+H:MM:ss", "+1", 3600},
            {"+H:MM:ss", "+1:01", 3660},
            {"+H:MM:ss", "+1:01:01", 3661},
            {"+H:MM:ss", "+01", 3600},
            {"+H:MM:ss", "+01:01", 3660},
            {"+H:MM:ss", "+01:01:01", 3661},
            {"+HMMSS", "+1", 3600},
            {"+HMMSS", "+101", 3660},
            {"+HMMSS", "+10101", 3661},
            {"+HMMSS", "+01", 3600},
            {"+HMMSS", "+0101", 3660},
            {"+HMMSS", "+010101", 3661},
            {"+H:MM:SS", "+1", 3600},
            {"+H:MM:SS", "+1:01", 3660},
            {"+H:MM:SS", "+1:01:01", 3661},
            {"+H:MM:SS", "+01", 3600},
            {"+H:MM:SS", "+01:01", 3660},
            {"+H:MM:SS", "+01:01:01", 3661},
            {"+Hmmss", "+1", 3600},
            {"+Hmmss", "+101", 3660},
            {"+Hmmss", "+10101", 3661},
            {"+Hmmss", "+01", 3600},
            {"+Hmmss", "+0101", 3660},
            {"+Hmmss", "+010101", 3661},
            {"+H:mm:ss", "+1", 3600},
            {"+H:mm:ss", "+1:01", 3660},
            {"+H:mm:ss", "+1:01:01", 3661},
            {"+H:mm:ss", "+01", 3600},
            {"+H:mm:ss", "+01:01", 3660},
            {"+H:mm:ss", "+01:01:01", 3661},
        };
    }

    @DataProvider(name="strictDoubleDigitHourOffsetParseData")
    Object[][] data_strictDoubleDigitHour_offset_parse() {
        return new Object[][] {
            {"+HH", "+01", 3600},
            {"+HHmm", "+01", 3600},
            {"+HHmm", "+0101", 3660},
            {"+HH:mm", "+01", 3600},
            {"+HH:mm", "+01:01", 3660},
            {"+HHMM", "+0101", 3660},
            {"+HH:MM", "+01:01", 3660},
            {"+HHMMss", "+0101", 3660},
            {"+HHMMss", "+010101", 3661},
            {"+HH:MM:ss", "+01:01", 3660},
            {"+HH:MM:ss", "+01:01:01", 3661},
            {"+HHMMSS", "+010101", 3661},
            {"+HH:MM:SS", "+01:01:01", 3661},
            {"+HHmmss", "+01", 3600},
            {"+HHmmss", "+0101", 3660},
            {"+HHmmss", "+010101", 3661},
            {"+HH:mm:ss", "+01", 3600},
            {"+HH:mm:ss", "+01:01", 3660},
            {"+HH:mm:ss", "+01:01:01", 3661},
        };
    }

    @DataProvider(name="strictSingleDigitHourOffsetParseData")
    Object[][] data_strictSingleDigitHour_offset_parse() {
        return new Object[][] {
            {"+H", "+01", 3600},
            {"+H", "+1", 3600},
            {"+Hmm", "+01", 3600},
            {"+Hmm", "+0101", 3660},
            {"+Hmm", "+1", 3600},
            {"+Hmm", "+101", 3660},
            {"+H:mm", "+01", 3600},
            {"+H:mm", "+01:01", 3660},
            {"+H:mm", "+1", 3600},
            {"+H:mm", "+1:01", 3660},
            {"+HMM", "+0101", 3660},
            {"+HMM", "+101", 3660},
            {"+H:MM", "+01:01", 3660},
            {"+H:MM", "+1:01", 3660},
            {"+HMMss", "+0101", 3660},
            {"+HMMss", "+010101", 3661},
            {"+HMMss", "+101", 3660},
            {"+HMMss", "+10101", 3661},
            {"+H:MM:ss", "+01:01", 3660},
            {"+H:MM:ss", "+01:01:01", 3661},
            {"+H:MM:ss", "+1:01", 3660},
            {"+H:MM:ss", "+1:01:01", 3661},
            {"+HMMSS", "+010101", 3661},
            {"+HMMSS", "+10101", 3661},
            {"+H:MM:SS", "+01:01:01", 3661},
            {"+H:MM:SS", "+1:01:01", 3661},
            {"+Hmmss", "+01", 3600},
            {"+Hmmss", "+0101", 3660},
            {"+Hmmss", "+010101", 3661},
            {"+Hmmss", "+1", 3600},
            {"+Hmmss", "+101", 3660},
            {"+Hmmss", "+10101", 3661},
            {"+H:mm:ss", "+01", 3600},
            {"+H:mm:ss", "+01:01", 3660},
            {"+H:mm:ss", "+01:01:01", 3661},
            {"+H:mm:ss", "+1", 3600},
            {"+H:mm:ss", "+1:01", 3660},
            {"+H:mm:ss", "+1:01:01", 3661},
        };
    }

    @Test(dataProvider="lenientOffsetParseData")
    public void test_lenient_offset_parse_1(String pattern, String offset, int offsetSeconds) {
        assertEquals(new DateTimeFormatterBuilder().parseLenient().appendOffset(pattern, "Z").toFormatter().parse(offset).get(OFFSET_SECONDS),
                     offsetSeconds);
    }

    @Test
    public void test_lenient_offset_parse_2() {
        assertEquals(new DateTimeFormatterBuilder().parseLenient().appendOffsetId().toFormatter().parse("+01").get(OFFSET_SECONDS),
                     3600);
    }

    @Test(dataProvider="strictDoubleDigitHourOffsetParseData")
    public void test_strictDoubleDigitHour_offset_parse_1(String pattern, String offset, int offsetSeconds) {
        assertEquals(new DateTimeFormatterBuilder().appendOffset(pattern, "Z").toFormatter()
                .parse(offset).get(OFFSET_SECONDS), offsetSeconds);
    }

    @Test(dataProvider="strictDoubleDigitHourOffsetParseData")
    public void test_strictDoubleDigitHour_offset_parse_2(String pattern, String offset, int offsetSeconds) {
        assertEquals(new DateTimeFormatterBuilder().appendOffset(pattern, "Z")
                .appendLiteral("text").toFormatter().parse(offset + "text").get(OFFSET_SECONDS), offsetSeconds);
    }

    @Test(dataProvider="strictSingleDigitHourOffsetParseData")
    public void test_strictSingleDigitHour_offset_parse_1(String pattern, String offset, int offsetSeconds) {
        assertEquals(new DateTimeFormatterBuilder().appendOffset(pattern, "Z").toFormatter()
                .parse(offset).get(OFFSET_SECONDS), offsetSeconds);
    }

    @Test(dataProvider="strictSingleDigitHourOffsetParseData")
    public void test_strictSingleDigitHour_offset_parse_2(String pattern, String offset, int offsetSeconds) {
        assertEquals(new DateTimeFormatterBuilder().appendOffset(pattern, "Z")
                .appendLiteral("text").toFormatter().parse(offset + "text").get(OFFSET_SECONDS), offsetSeconds);
    }

    @DataProvider(name="strictOffsetAdjacentParseValidPatternData")
    Object[][] data_strict_offset_adjacentParse_validPattern() {
        return new Object[][] {
            {"+HH", "+01", 3600},
            {"+HHmm", "+0101", 3660},
            {"+HH:mm", "+01", 3600},
            {"+HH:mm", "+01:01", 3660},
            {"+HHMM", "+0101", 3660},
            {"+HH:MM", "+01:01", 3660},
            {"+HHMMss", "+010101", 3661},
            {"+HH:MM:ss", "+01:01", 3660},
            {"+HH:MM:ss", "+01:01:01", 3661},
            {"+HHMMSS", "+010101", 3661},
            {"+HH:MM:SS", "+01:01:01", 3661},
            {"+HHmmss", "+010101", 3661},
            {"+HH:mm:ss", "+01", 3600},
            {"+HH:mm:ss", "+01:01", 3660},
            {"+HH:mm:ss", "+01:01:01", 3661},

            {"+H", "+01", 3600},
            {"+Hmm", "+0101", 3660},
            {"+H:mm", "+01", 3600},
            {"+H:mm", "+01:01", 3660},
            {"+H:mm", "+1:01", 3660},
            {"+HMM", "+0101", 3660},
            {"+H:MM", "+01:01", 3660},
            {"+H:MM", "+1:01", 3660},
            {"+HMMss", "+010101", 3661},
            {"+H:MM:ss", "+01:01", 3660},
            {"+H:MM:ss", "+01:01:01", 3661},
            {"+H:MM:ss", "+1:01", 3660},
            {"+H:MM:ss", "+1:01:01", 3661},
            {"+HMMSS", "+010101", 3661},
            {"+H:MM:SS", "+01:01:01", 3661},
            {"+H:MM:SS", "+1:01:01", 3661},
            {"+Hmmss", "+010101", 3661},
            {"+H:mm:ss", "+01", 3600},
            {"+H:mm:ss", "+01:01", 3660},
            {"+H:mm:ss", "+01:01:01", 3661},
            {"+H:mm:ss", "+1:01", 3660},
            {"+H:mm:ss", "+1:01:01", 3661},
        };
    }

    @Test(dataProvider="strictOffsetAdjacentParseValidPatternData")
    public void test_strict_offset_adjacentValidPattern_parse(String pattern, String offset, int offsetSeconds) {
        TemporalAccessor tmp = new DateTimeFormatterBuilder().appendOffset(pattern, "Z")
                .appendValue(HOUR_OF_DAY, 2).toFormatter().parse(offset + "12");
        assertEquals(tmp.get(OFFSET_SECONDS), offsetSeconds);
        assertEquals(tmp.get(HOUR_OF_DAY), 12);
    }

    @DataProvider(name="strictOffsetAdjacentParseInvalidPatternData")
    Object[][] data_strict_offset_adjacentParse_invalidPattern() {
        return new Object[][] {
            {"+HHmm", "+01", 3600},
            {"+HHMMss", "+0101", 3660},
            {"+HHmmss", "+01", 3600},
            {"+HHmmss", "+0101", 3660},
            {"+H", "+1", 3600},
            {"+Hmm", "+01", 3600},
            {"+H:mm", "+1", 3600},
            {"+Hmm", "+1", 3600},
            {"+Hmm", "+101", 3660},
            {"+HMM", "+101", 3660},
            {"+HMMss", "+0101", 3660},
            {"+HMMss", "+101", 3660},
            {"+HMMss", "+10101", 3661},
            {"+HMMSS", "+10101", 3661},
            {"+Hmmss", "+01", 3600},
            {"+Hmmss", "+0101", 3660},
            {"+Hmmss", "+1", 3600},
            {"+Hmmss", "+101", 3660},
            {"+Hmmss", "+10101", 3661},
            {"+H:mm:ss", "+1", 3600},
        };
    }

    @Test(dataProvider="strictOffsetAdjacentParseInvalidPatternData", expectedExceptions=DateTimeParseException.class)
    public void test_strict_offset_adjacentInvalidPattern_parse(String pattern, String offset, int offsetSeconds) {
       new DateTimeFormatterBuilder().appendOffset(pattern, "Z").appendValue(HOUR_OF_DAY, 2)
               .toFormatter().parse(offset + "12");
    }

    @DataProvider(name="lenientOffsetAdjacentParseValidPatternData")
    Object[][] data_lenient_offset_adjacentParse_validPattern() {
        return new Object[][] {
            {"+HH:mm", "+01", 3600},
            {"+HH:mm", "+01:01", 3660},
            {"+HH:MM", "+01:01", 3660},
            {"+HH:MM:ss", "+01:01", 3660},
            {"+HH:MM:ss", "+01:01:01", 3661},
            {"+HHMMSS", "+010101", 3661},
            {"+HH:MM:SS", "+01:01:01", 3661},
            {"+HHmmss", "+010101", 3661},
            {"+HH:mm:ss", "+01", 3600},
            {"+HH:mm:ss", "+01:01", 3660},
            {"+HH:mm:ss", "+01:01:01", 3661},
            {"+H:mm", "+01", 3600},
            {"+H:mm", "+01:01", 3660},
            {"+H:mm", "+1:01", 3660},
            {"+H:MM", "+01:01", 3660},
            {"+H:MM", "+1:01", 3660},
            {"+HMMss", "+010101", 3661},
            {"+H:MM:ss", "+01:01", 3660},
            {"+H:MM:ss", "+01:01:01", 3661},
            {"+H:MM:ss", "+1:01", 3660},
            {"+H:MM:ss", "+1:01:01", 3661},
            {"+HMMSS", "+010101", 3661},
            {"+H:MM:SS", "+01:01:01", 3661},
            {"+H:MM:SS", "+1:01:01", 3661},
            {"+Hmmss", "+010101", 3661},
            {"+H:mm:ss", "+01", 3600},
            {"+H:mm:ss", "+01:01", 3660},
            {"+H:mm:ss", "+01:01:01", 3661},
            {"+H:mm:ss", "+1:01", 3660},
            {"+H:mm:ss", "+1:01:01", 3661},
        };
    }

    @Test(dataProvider="lenientOffsetAdjacentParseValidPatternData")
    public void test_lenient_offset_adjacentValidPattern_parse(String pattern, String offset, int offsetSeconds) {
        TemporalAccessor tmp = new DateTimeFormatterBuilder().parseLenient()
                .appendOffset(pattern, "Z").appendValue(HOUR_OF_DAY, 2).toFormatter().parse(offset + "12");
        assertEquals(tmp.get(OFFSET_SECONDS), offsetSeconds);
        assertEquals(tmp.get(HOUR_OF_DAY), 12);
    }

    @Test
    public void test_lenient_offset_adjacentValidPattern_parse1() {
        TemporalAccessor tmp = new DateTimeFormatterBuilder().parseLenient()
                .appendOffset("+HMMSS", "Z").appendValue(HOUR_OF_DAY, 2).toFormatter().parse("+10101" + "12");
        //Equivalent to +101011. In lenient mode, offset will parse upto 6 digit if possible.
        //It will take 1 digit from HOUR_OF_DAY.
        assertEquals(tmp.get(OFFSET_SECONDS), 36611);
        assertEquals(tmp.get(HOUR_OF_DAY), 2);
    }

  @DataProvider(name="lenientOffsetAdjacentParseInvalidPatternData")
    Object[][] data_lenient_offset_adjacentParse_invalidPattern() {
        return new Object[][] {
            {"+HH", "+01", 3600},
            {"+HHmm", "+0101", 3660},
            {"+HHMM", "+0101", 3660},
            {"+H", "+01", 3600},
            {"+Hmm", "+0101", 3660},
            {"+HMM", "+0101", 3660},
        };
    }

    @Test(dataProvider="lenientOffsetAdjacentParseInvalidPatternData", expectedExceptions=DateTimeParseException.class)
    public void test_lenient_offset_adjacentInvalidPattern_parse(String pattern, String offset, int offsetSeconds) {
       new DateTimeFormatterBuilder().parseLenient().appendOffset(pattern, "Z")
               .appendValue(HOUR_OF_DAY, 2).toFormatter().parse(offset + "12");
    }

    @DataProvider(name="badValues")
    Object[][] data_badOffsetValues() {
        return new Object[][] {
            {"+HH", "+24"},
            {"+HHMM", "-1361"},
            {"+HH:MM:ss", "+13:12:66"},
            {"+HH:MM:SS", "+24:60:60"},
            {"+HHMMSS", "369999"},
            {"+H:MM", "+28:12"},
        };
    }

    @Test(dataProvider="badValues", expectedExceptions=DateTimeParseException.class)
    public void test_badOffset_parse(String pattern, String offset) {
        new DateTimeFormatterBuilder().appendOffset(pattern, "Z").toFormatter().parse(offset);
    }

    @Test(expectedExceptions=DateTimeParseException.class)
    public void test_strict_appendOffsetId() {
        new DateTimeFormatterBuilder().appendOffsetId().toFormatter().parse("+01");
    }

    @Test(expectedExceptions=DateTimeParseException.class)
    public void test_strict_appendOffset_1() {
        new DateTimeFormatterBuilder().appendOffset("+HH:MM:ss", "Z").toFormatter().parse("+01");
    }

    @Test(expectedExceptions=DateTimeParseException.class)
    public void test_strict_appendOffset_2() {
        new DateTimeFormatterBuilder().appendOffset("+HHMMss", "Z").toFormatter().parse("+01");
    }

    @Test(expectedExceptions=DateTimeParseException.class)
    public void test_strict_appendOffset_3() {
        new DateTimeFormatterBuilder().appendOffset("+H:MM:ss", "Z").toFormatter().parse("+1");
    }

    @Test(expectedExceptions=DateTimeParseException.class)
    public void test_strict_appendOffset_4() {
        new DateTimeFormatterBuilder().appendOffset("+HMMss", "Z").toFormatter().parse("+1");
    }

    @Test
    public void test_basic_iso_date() {
        assertEquals(BASIC_ISO_DATE.parse("20021231+01").get(OFFSET_SECONDS), 3600);
        assertEquals(BASIC_ISO_DATE.parse("20021231+0101").get(OFFSET_SECONDS), 3660);
    }

}
