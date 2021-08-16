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
package test.java.time.format;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;
import static java.time.temporal.ChronoField.YEAR_OF_ERA;
import static java.time.temporal.ChronoUnit.YEARS;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;

import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.HijrahChronology;
import java.time.chrono.IsoChronology;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.MinguoChronology;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.chrono.ThaiBuddhistDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test ReducedPrinterParser.
 */
@Test
public class TestReducedParser extends AbstractTestPrinterParser {
    private static final boolean STRICT = true;
    private static final boolean LENIENT = false;

    private DateTimeFormatter getFormatter0(TemporalField field, int width, int baseValue) {
        return builder.appendValueReduced(field, width, width, baseValue).toFormatter(locale).withDecimalStyle(decimalStyle);
    }

    private DateTimeFormatter getFormatter0(TemporalField field, int minWidth, int maxWidth, int baseValue) {
        return builder.appendValueReduced(field, minWidth, maxWidth, baseValue).toFormatter(locale).withDecimalStyle(decimalStyle);
    }

    private DateTimeFormatter getFormatterBaseDate(TemporalField field, int minWidth, int maxWidth, int baseValue) {
        return builder.appendValueReduced(field, minWidth, maxWidth, LocalDate.of(baseValue, 1, 1)).toFormatter(locale).withDecimalStyle(decimalStyle);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="error")
    Object[][] data_error() {
        return new Object[][] {
            {YEAR, 2, 2010, "12", -1, IndexOutOfBoundsException.class},
            {YEAR, 2, 2010, "12", 3, IndexOutOfBoundsException.class},
        };
    }

    @Test(dataProvider="error")
    public void test_parse_error(TemporalField field, int width, int baseValue, String text, int pos, Class<?> expected) {
        try {
            getFormatter0(field, width, baseValue).parseUnresolved(text, new ParsePosition(pos));
        } catch (RuntimeException ex) {
            assertTrue(expected.isInstance(ex));
        }
    }

    //-----------------------------------------------------------------------
    public void test_parse_fieldRangeIgnored() throws Exception {
        ParsePosition pos = new ParsePosition(0);
        TemporalAccessor parsed = getFormatter0(DAY_OF_YEAR, 3, 10).parseUnresolved("456", pos);
        assertEquals(pos.getIndex(), 3);
        assertParsed(parsed, DAY_OF_YEAR, 456L);  // parsed dayOfYear=456
    }

    //-----------------------------------------------------------------------
    // Parse data and values that are consistent whether strict or lenient
    // The data is the ChronoField, width, baseValue, text, startPos, endPos, value
    //-----------------------------------------------------------------------
    @DataProvider(name="ParseAll")
    Object[][] provider_parseAll() {
        return new Object[][] {
             // negative zero
            {YEAR, 1, 2010, "-0", 0, 0, null},

            // general
            {YEAR, 2, 2010, "Xxx12Xxx", 3, 5, 2012},
            {YEAR, 2, 2010, "12-45", 0, 2, 2012},

            // other junk
            {YEAR, 2, 2010, "A0", 0, 0, null},
            {YEAR, 2, 2010, "  1", 0, 0, null},
            {YEAR, 2, 2010, "-1", 0, 0, null},
            {YEAR, 2, 2010, "-10", 0, 0, null},
            {YEAR, 2, 2000, " 1", 0, 0, null},

            // parse OK 1
            {YEAR, 1, 2010, "1", 0, 1, 2011},
            {YEAR, 1, 2010, "3", 1, 1, null},
            {YEAR, 1, 2010, "9", 0, 1, 2019},

            {YEAR, 1, 2005, "0", 0, 1, 2010},
            {YEAR, 1, 2005, "4", 0, 1, 2014},
            {YEAR, 1, 2005, "5", 0, 1, 2005},
            {YEAR, 1, 2005, "9", 0, 1, 2009},
            {YEAR, 1, 2010, "1-2", 0, 1, 2011},

            // parse OK 2
            {YEAR, 2, 2010, "00", 0, 2, 2100},
            {YEAR, 2, 2010, "09", 0, 2, 2109},
            {YEAR, 2, 2010, "10", 0, 2, 2010},
            {YEAR, 2, 2010, "99", 0, 2, 2099},

            // parse OK 2
            {YEAR, 2, -2005, "05", 0, 2, -2005},
            {YEAR, 2, -2005, "00", 0, 2, -2000},
            {YEAR, 2, -2005, "99", 0, 2, -1999},
            {YEAR, 2, -2005, "06", 0, 2, -1906},

            {YEAR, 2, -2005, "43", 0, 2, -1943},
        };
    }

    @Test(dataProvider="ParseAll")
    public void test_parseAllStrict(TemporalField field, int width, int baseValue, String input, int pos, int parseLen, Integer parseVal) {
        ParsePosition ppos = new ParsePosition(pos);
        setStrict(true);
        TemporalAccessor parsed = getFormatter0(field, width, baseValue).parseUnresolved(input, ppos);
        if (ppos.getErrorIndex() != -1) {
            assertEquals(ppos.getErrorIndex(), parseLen, "error case parse position");
            assertEquals(parsed, parseVal, "unexpected parse result");
        } else {
            assertEquals(ppos.getIndex(), parseLen, "parse position");
            assertParsed(parsed, YEAR, parseVal != null ? (long) parseVal : null);
        }
    }

   @Test(dataProvider="ParseAll")
    public void test_parseAllLenient(TemporalField field, int width, int baseValue, String input, int pos, int parseLen, Integer parseVal) {
        ParsePosition ppos = new ParsePosition(pos);
        setStrict(false);
        TemporalAccessor parsed = getFormatter0(field, width, baseValue).parseUnresolved(input, ppos);
        if (ppos.getErrorIndex() != -1) {
            assertEquals(ppos.getErrorIndex(), parseLen, "error case parse position");
            assertEquals(parsed, parseVal, "unexpected parse result");
        } else {
            assertEquals(ppos.getIndex(), parseLen, "parse position");
            assertParsed(parsed, YEAR, parseVal != null ? (long) parseVal : null);
        }
    }

    //-----------------------------------------------------------------------
    // Parse data and values in strict and lenient modes.
    // The data is the ChronoField, minWidth, maxWidth, baseValue, text, startPos,
    // Strict Pair(endPos, value), Lenient Pair(endPos, value)
    //-----------------------------------------------------------------------
    @DataProvider(name="ParseLenientSensitive")
    Object[][] provider_parseLenientSensitive() {
        return new Object[][] {
            // few digits supplied
            {YEAR, 2, 2, 2010, "3", 0, strict(0, null), lenient(1, 3)},
            {YEAR, 2, 2, 2010, "4", 0, strict(0, null), lenient(1, 4)},
            {YEAR, 2, 2, 2010, "5", 1, strict(1, null), lenient(1, null)},
            {YEAR, 2, 2, 2010, "6-2", 0, strict(0, null), lenient(1, 6)},
            {YEAR, 2, 2, 2010, "9", 0, strict(0, null), lenient(1, 9)},

            // other junk
            {YEAR, 1, 4, 2000, "7A", 0, strict(1, 2007), lenient(1, 2007)},
            {YEAR, 2, 2, 2010, "8A", 0, strict(0, null), lenient(1, 8)},

            // Negative sign cases
            {YEAR, 2, 4, 2000, "-1", 0, strict(0, null), lenient(2, -1)},
            {YEAR, 2, 4, 2000, "-10", 0, strict(0, null), lenient(3, -10)},

            // Positive sign cases
            {YEAR, 2, 4, 2000, "+1", 0, strict(0, null), lenient(2, 1)},
            {YEAR, 2, 4, 2000, "+10", 0, strict(0, null), lenient(3, 2010)},

            // No sign cases
            {YEAR, 1, 1, 2005, "21", 0, strict(1, 2012), lenient(2, 21)},
            {YEAR, 1, 2, 2010, "12", 0, strict(2, 12), lenient(2, 12)},
            {YEAR, 1, 4, 2000, "87", 0, strict(2, 87), lenient(2, 87)},
            {YEAR, 1, 4, 2000, "9876", 0, strict(4, 9876), lenient(4, 9876)},
            {YEAR, 2, 2, 2010, "321", 0, strict(2, 2032), lenient(3, 321)},
            {YEAR, 2, 4, 2010, "2", 0, strict(0, null), lenient(1, 2)},
            {YEAR, 2, 4, 2010, "21", 0, strict(2, 2021), lenient(2, 2021)},
            {YEAR, 2, 4, 2010, "321", 0, strict(3, 321), lenient(3, 321)},
            {YEAR, 2, 4, 2010, "4321", 0, strict(4, 4321), lenient(4, 4321)},
            {YEAR, 2, 4, 2010, "54321", 0, strict(4, 5432), lenient(5, 54321)},
            {YEAR, 2, 8, 2010, "87654321", 3, strict(8, 54321), lenient(8, 54321)},
            {YEAR, 2, 9, 2010, "987654321", 0, strict(9, 987654321), lenient(9, 987654321)},
            {YEAR, 3, 3, 2010, "765", 0, strict(3, 2765), lenient(3, 2765)},
            {YEAR, 3, 4, 2010, "76", 0, strict(0, null), lenient(2, 76)},
            {YEAR, 3, 4, 2010, "765", 0, strict(3, 2765), lenient(3, 2765)},
            {YEAR, 3, 4, 2010, "7654", 0, strict(4, 7654), lenient(4, 7654)},
            {YEAR, 3, 4, 2010, "76543", 0, strict(4, 7654), lenient(5, 76543)},

            // Negative baseValue
            {YEAR, 2, 4, -2005, "123", 0, strict(3, 123), lenient(3, 123)},

            // Basics
            {YEAR, 2, 4, 2010, "10", 0, strict(2, 2010), lenient(2, 2010)},
            {YEAR, 2, 4, 2010, "09", 0, strict(2, 2109), lenient(2, 2109)},
        };
    }

    //-----------------------------------------------------------------------
    // Parsing tests for strict mode
    //-----------------------------------------------------------------------
    @Test(dataProvider="ParseLenientSensitive")
    public void test_parseStrict(TemporalField field, int minWidth, int maxWidth, int baseValue, String input, int pos,
        Pair strict, Pair lenient) {
        ParsePosition ppos = new ParsePosition(pos);
        setStrict(true);
        TemporalAccessor parsed = getFormatter0(field, minWidth, maxWidth, baseValue).parseUnresolved(input, ppos);
        if (ppos.getErrorIndex() != -1) {
            assertEquals(ppos.getErrorIndex(), strict.parseLen, "error case parse position");
            assertEquals(parsed, strict.parseVal, "unexpected parse result");
        } else {
            assertEquals(ppos.getIndex(), strict.parseLen, "parse position");
            assertParsed(parsed, YEAR, strict.parseVal != null ? (long) strict.parseVal : null);
        }
    }

    @Test(dataProvider="ParseLenientSensitive")
    public void test_parseStrict_baseDate(TemporalField field, int minWidth, int maxWidth, int baseValue, String input, int pos,
                                 Pair strict, Pair lenient) {
        ParsePosition ppos = new ParsePosition(pos);
        setStrict(true);
        TemporalAccessor parsed = getFormatterBaseDate(field, minWidth, maxWidth, baseValue).parseUnresolved(input, ppos);
        if (ppos.getErrorIndex() != -1) {
            assertEquals(ppos.getErrorIndex(), strict.parseLen, "error case parse position");
            assertEquals(parsed, strict.parseVal, "unexpected parse result");
        } else {
            assertEquals(ppos.getIndex(), strict.parseLen, "parse position");
            assertParsed(parsed, YEAR, strict.parseVal != null ? (long) strict.parseVal : null);
        }
    }

    //-----------------------------------------------------------------------
    // Parsing tests for lenient mode
    //-----------------------------------------------------------------------
    @Test(dataProvider="ParseLenientSensitive")
    public void test_parseLenient(TemporalField field, int minWidth, int maxWidth, int baseValue, String input, int pos,
        Pair strict, Pair lenient) {
        ParsePosition ppos = new ParsePosition(pos);
        setStrict(false);
        TemporalAccessor parsed = getFormatter0(field, minWidth, maxWidth, baseValue).parseUnresolved(input, ppos);
        if (ppos.getErrorIndex() != -1) {
            assertEquals(ppos.getErrorIndex(), lenient.parseLen, "error case parse position");
            assertEquals(parsed, lenient.parseVal, "unexpected parse result");
        } else {
            assertEquals(ppos.getIndex(), lenient.parseLen, "parse position");
            assertParsed(parsed, YEAR, lenient.parseVal != null ? (long) lenient.parseVal : null);
        }
    }

    @Test(dataProvider="ParseLenientSensitive")
    public void test_parseLenient_baseDate(TemporalField field, int minWidth, int maxWidth, int baseValue, String input, int pos,
                                  Pair strict, Pair lenient) {
        ParsePosition ppos = new ParsePosition(pos);
        setStrict(false);
        TemporalAccessor parsed = getFormatterBaseDate(field, minWidth, maxWidth, baseValue).parseUnresolved(input, ppos);
        if (ppos.getErrorIndex() != -1) {
            assertEquals(ppos.getErrorIndex(), lenient.parseLen, "error case parse position");
            assertEquals(parsed, lenient.parseVal, "unexpected parse result");
        } else {
            assertEquals(ppos.getIndex(), lenient.parseLen, "parse position");
            assertParsed(parsed, YEAR, lenient.parseVal != null ? (long) lenient.parseVal : null);
        }
    }

    private void assertParsed(TemporalAccessor parsed, TemporalField field, Long value) {
        if (value == null) {
            assertEquals(parsed, null, "Parsed Value");
        } else {
            assertEquals(parsed.isSupported(field), true, "isSupported: " + field);
            assertEquals(parsed.getLong(field), (long) value, "Temporal.getLong: " + field);
        }
    }


    //-----------------------------------------------------------------------
    // Cases and values in adjacent parsing mode
    //-----------------------------------------------------------------------
    @DataProvider(name="ParseAdjacent")
    Object[][] provider_parseAdjacent() {
        return new Object[][] {
            // general
            {"yyMMdd", "19990703",  LENIENT, 0, 8, 1999, 7, 3},
            {"yyMMdd", "19990703",  STRICT, 0, 6, 2019, 99, 7},
            {"yyMMdd", "990703",    LENIENT, 0, 6, 2099, 7, 3},
            {"yyMMdd", "990703",    STRICT, 0, 6, 2099, 7, 3},
            {"yyMMdd", "200703",    LENIENT, 0, 6, 2020, 7, 3},
            {"yyMMdd", "200703",    STRICT, 0, 6, 2020, 7, 3},
            {"ddMMyy", "230714",    LENIENT, 0, 6, 2014, 7, 23},
            {"ddMMyy", "230714",    STRICT, 0, 6, 2014, 7, 23},
            {"ddMMyy", "25062001",  LENIENT, 0, 8, 2001, 6, 25},
            {"ddMMyy", "25062001",  STRICT, 0, 6, 2020, 6, 25},
            {"ddMMy",  "27052002",  LENIENT, 0, 8, 2002, 5, 27},
            {"ddMMy",  "27052002",  STRICT, 0, 8, 2002, 5, 27},
        };
    }

    @Test(dataProvider="ParseAdjacent")
    public void test_parseAdjacent(String pattern, String input, boolean strict, int pos, int parseLen, int year, int month, int day) {
        ParsePosition ppos = new ParsePosition(0);
        builder = new DateTimeFormatterBuilder();
        setStrict(strict);
        builder.appendPattern(pattern);
        DateTimeFormatter dtf = builder.toFormatter();

        TemporalAccessor parsed = dtf.parseUnresolved(input, ppos);
        assertNotNull(parsed, String.format("parse failed: ppos: %s, formatter: %s%n", ppos.toString(), dtf));
        if (ppos.getErrorIndex() != -1) {
            assertEquals(ppos.getErrorIndex(), parseLen, "error case parse position");
        } else {
            assertEquals(ppos.getIndex(), parseLen, "parse position");
            assertParsed(parsed, YEAR_OF_ERA, Long.valueOf(year));
            assertParsed(parsed, MONTH_OF_YEAR, Long.valueOf(month));
            assertParsed(parsed, DAY_OF_MONTH, Long.valueOf(day));
        }
    }

    //-----------------------------------------------------------------------
    // Cases and values in reduced value parsing mode
    //-----------------------------------------------------------------------
    @DataProvider(name="ReducedWithChrono")
    Object[][] provider_reducedWithChrono() {
        LocalDate baseYear = LocalDate.of(2000, 1, 1);
        return new Object[][] {
            {IsoChronology.INSTANCE.date(baseYear)},
            {IsoChronology.INSTANCE.date(baseYear).plus(1, YEARS)},
            {IsoChronology.INSTANCE.date(baseYear).plus(99, YEARS)},
            {HijrahChronology.INSTANCE.date(baseYear)},
            {HijrahChronology.INSTANCE.date(baseYear).plus(1, YEARS)},
            {HijrahChronology.INSTANCE.date(baseYear).plus(99, YEARS)},
            {JapaneseChronology.INSTANCE.date(baseYear)},
            {JapaneseChronology.INSTANCE.date(baseYear).plus(1, YEARS)},
            {JapaneseChronology.INSTANCE.date(baseYear).plus(99, YEARS)},
            {MinguoChronology.INSTANCE.date(baseYear)},
            {MinguoChronology.INSTANCE.date(baseYear).plus(1, YEARS)},
            {MinguoChronology.INSTANCE.date(baseYear).plus(99, YEARS)},
            {ThaiBuddhistChronology.INSTANCE.date(baseYear)},
            {ThaiBuddhistChronology.INSTANCE.date(baseYear).plus(1, YEARS)},
            {ThaiBuddhistChronology.INSTANCE.date(baseYear).plus(99, YEARS)},
        };
    }

    @Test(dataProvider="ReducedWithChrono")
    public void test_reducedWithChronoYear(ChronoLocalDate date) {
        Chronology chrono = date.getChronology();
        DateTimeFormatter df
                = new DateTimeFormatterBuilder().appendValueReduced(YEAR, 2, 2, LocalDate.of(2000, 1, 1))
                .toFormatter()
                .withChronology(chrono);
        int expected = date.get(YEAR);
        String input = df.format(date);

        ParsePosition pos = new ParsePosition(0);
        TemporalAccessor parsed = df.parseUnresolved(input, pos);
        int actual = parsed.get(YEAR);
        assertEquals(actual, expected,
                String.format("Wrong date parsed, chrono: %s, input: %s",
                chrono, input));

    }
    @Test(dataProvider="ReducedWithChrono")
    public void test_reducedWithChronoYearOfEra(ChronoLocalDate date) {
        Chronology chrono = date.getChronology();
        DateTimeFormatter df
                = new DateTimeFormatterBuilder().appendValueReduced(YEAR_OF_ERA, 2, 2, LocalDate.of(2000, 1, 1))
                .toFormatter()
                .withChronology(chrono);
        int expected = date.get(YEAR_OF_ERA);
        String input = df.format(date);

        ParsePosition pos = new ParsePosition(0);
        TemporalAccessor parsed = df.parseUnresolved(input, pos);
        int actual = parsed.get(YEAR_OF_ERA);
        assertEquals(actual, expected,
                String.format("Wrong date parsed, chrono: %s, input: %s",
                chrono, input));

    }

    @Test
    public void test_reducedWithLateChronoChange() {
        ThaiBuddhistDate date = ThaiBuddhistDate.of(2543, 1, 1);
        DateTimeFormatter df
                = new DateTimeFormatterBuilder()
                        .appendValueReduced(YEAR, 2, 2, LocalDate.of(2000, 1, 1))
                        .appendLiteral(" ")
                        .appendChronologyId()
                .toFormatter();
        int expected = date.get(YEAR);
        String input = df.format(date);

        ParsePosition pos = new ParsePosition(0);
        TemporalAccessor parsed = df.parseUnresolved(input, pos);
        assertEquals(pos.getIndex(), input.length(), "Input not parsed completely");
        assertEquals(pos.getErrorIndex(), -1, "Error index should be -1 (no-error)");
        int actual = parsed.get(YEAR);
        assertEquals(actual, expected,
                String.format("Wrong date parsed, chrono: %s, input: %s",
                parsed.query(TemporalQueries.chronology()), input));

    }

    @Test
    public void test_reducedWithLateChronoChangeTwice() {
        DateTimeFormatter df
                = new DateTimeFormatterBuilder()
                        .appendValueReduced(YEAR, 2, 2, LocalDate.of(2000, 1, 1))
                        .appendLiteral(" ")
                        .appendChronologyId()
                        .appendLiteral(" ")
                        .appendChronologyId()
                .toFormatter();
        int expected = 2044;
        String input = "44 ThaiBuddhist ISO";
        ParsePosition pos = new ParsePosition(0);
        TemporalAccessor parsed = df.parseUnresolved(input, pos);
        assertEquals(pos.getIndex(), input.length(), "Input not parsed completely: " + pos);
        assertEquals(pos.getErrorIndex(), -1, "Error index should be -1 (no-error)");
        int actual = parsed.get(YEAR);
        assertEquals(actual, expected,
                String.format("Wrong date parsed, chrono: %s, input: %s",
                parsed.query(TemporalQueries.chronology()), input));

    }

    //-----------------------------------------------------------------------
    // Class to structure the test data
    //-----------------------------------------------------------------------

    private static Pair strict(int parseLen, Integer parseVal) {
        return new Pair(parseLen, parseVal, STRICT);
    }
    private static Pair lenient(int parseLen, Integer parseVal) {
        return new Pair(parseLen, parseVal, LENIENT);
    }

    private static class Pair {
        public final int parseLen;
        public final Integer parseVal;
        private final boolean strict;
        public Pair(int parseLen, Integer parseVal, boolean strict) {
            this.parseLen = parseLen;
            this.parseVal = parseVal;
            this.strict = strict;
        }
        public String toString() {
            return (strict ? "strict" : "lenient") + "(" + parseLen + "," + parseVal + ")";
        }
    }

}
