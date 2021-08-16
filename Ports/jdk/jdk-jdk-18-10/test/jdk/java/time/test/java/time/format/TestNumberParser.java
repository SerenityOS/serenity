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

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.text.ParsePosition;
import java.time.format.DateTimeFormatter;
import java.time.format.SignStyle;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalQueries;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test NumberPrinterParser.
 */
@Test
public class TestNumberParser extends AbstractTestPrinterParser {

    //-----------------------------------------------------------------------
    @DataProvider(name="error")
    Object[][] data_error() {
        return new Object[][] {
            {DAY_OF_MONTH, 1, 2, SignStyle.NEVER, "12", -1, IndexOutOfBoundsException.class},
            {DAY_OF_MONTH, 1, 2, SignStyle.NEVER, "12", 3, IndexOutOfBoundsException.class},
        };
    }

    @Test(dataProvider="error")
    public void test_parse_error(TemporalField field, int min, int max, SignStyle style, String text, int pos, Class<?> expected) {
        try {
            getFormatter(field, min, max, style).parseUnresolved(text, new ParsePosition(pos));
            fail();
        } catch (RuntimeException ex) {
            assertTrue(expected.isInstance(ex));
        }
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="parseData")
    Object[][] provider_parseData() {
        return new Object[][] {
            // normal
            {1, 2, SignStyle.NEVER, 0, "12", 0, 2, 12L},       // normal
            {1, 2, SignStyle.NEVER, 0, "Xxx12Xxx", 3, 5, 12L}, // parse in middle
            {1, 2, SignStyle.NEVER, 0, "99912999", 3, 5, 12L}, // parse in middle
            {2, 4, SignStyle.NEVER, 0, "12345", 0, 4, 1234L},  // stops at max width
            {2, 4, SignStyle.NEVER, 0, "12-45", 0, 2, 12L},    // stops at dash
            {2, 4, SignStyle.NEVER, 0, "123-5", 0, 3, 123L},   // stops at dash
            {1, 10, SignStyle.NORMAL, 0, "2147483647", 0, 10, Integer.MAX_VALUE},
            {1, 10, SignStyle.NORMAL, 0, "-2147483648", 0, 11, Integer.MIN_VALUE},
            {1, 10, SignStyle.NORMAL, 0, "2147483648", 0, 10, 2147483648L},
            {1, 10, SignStyle.NORMAL, 0, "-2147483649", 0, 11, -2147483649L},
            {1, 10, SignStyle.NORMAL, 0, "987659876598765", 0, 10, 9876598765L},
            {1, 19, SignStyle.NORMAL, 0, "999999999999999999", 0, 18, 999999999999999999L},
            {1, 19, SignStyle.NORMAL, 0, "-999999999999999999", 0, 19, -999999999999999999L},
            {1, 19, SignStyle.NORMAL, 0, "1000000000000000000", 0, 19, 1000000000000000000L},
            {1, 19, SignStyle.NORMAL, 0, "-1000000000000000000", 0, 20, -1000000000000000000L},
            {1, 19, SignStyle.NORMAL, 0, "000000000000000000", 0, 18, 0L},
            {1, 19, SignStyle.NORMAL, 0, "0000000000000000000", 0, 19, 0L},
            {1, 19, SignStyle.NORMAL, 0, "9223372036854775807", 0, 19, Long.MAX_VALUE},
            {1, 19, SignStyle.NORMAL, 0, "-9223372036854775808", 0, 20, Long.MIN_VALUE},
            {1, 19, SignStyle.NORMAL, 0, "9223372036854775808", 0, 18, 922337203685477580L},  // last digit not parsed
            {1, 19, SignStyle.NORMAL, 0, "-9223372036854775809", 0, 19, -922337203685477580L}, // last digit not parsed
            // no match
            {1, 2, SignStyle.NEVER, 1, "A1", 0, 0, 0},
            {1, 2, SignStyle.NEVER, 1, " 1", 0, 0, 0},
            {1, 2, SignStyle.NEVER, 1, "  1", 1, 1, 0},
            {2, 2, SignStyle.NEVER, 1, "1", 0, 0, 0},
            {2, 2, SignStyle.NEVER, 1, "Xxx1", 0, 0, 0},
            {2, 2, SignStyle.NEVER, 1, "1", 1, 1, 0},
            {2, 2, SignStyle.NEVER, 1, "Xxx1", 4, 4, 0},
            {2, 2, SignStyle.NEVER, 1, "1-2", 0, 0, 0},
            {1, 19, SignStyle.NORMAL, 0, "-000000000000000000", 0, 0, 0},
            {1, 19, SignStyle.NORMAL, 0, "-0000000000000000000", 0, 0, 0},
            // parse reserving space 1 (adjacent-parsing)
            {1, 1, SignStyle.NEVER, 1, "12", 0, 1, 1L},
            {1, 19, SignStyle.NEVER, 1, "12", 0, 1, 1L},
            {1, 19, SignStyle.NEVER, 1, "12345", 0, 4, 1234L},
            {1, 19, SignStyle.NEVER, 1, "12345678901", 0, 10, 1234567890L},
            {1, 19, SignStyle.NEVER, 1, "123456789012345678901234567890", 0, 19, 1234567890123456789L},
            {1, 19, SignStyle.NEVER, 1, "1", 0, 1, 1L},  // error from next field
            {2, 2, SignStyle.NEVER, 1, "12", 0, 2, 12L},  // error from next field
            {2, 19, SignStyle.NEVER, 1, "1", 0, 0, 0},
            // parse reserving space 2 (adjacent-parsing)
            {1, 1, SignStyle.NEVER, 2, "123", 0, 1, 1L},
            {1, 19, SignStyle.NEVER, 2, "123", 0, 1, 1L},
            {1, 19, SignStyle.NEVER, 2, "12345", 0, 3, 123L},
            {1, 19, SignStyle.NEVER, 2, "12345678901", 0, 9, 123456789L},
            {1, 19, SignStyle.NEVER, 2, "123456789012345678901234567890", 0, 19, 1234567890123456789L},
            {1, 19, SignStyle.NEVER, 2, "1", 0, 1, 1L},  // error from next field
            {1, 19, SignStyle.NEVER, 2, "12", 0, 1, 1L},  // error from next field
            {2, 2, SignStyle.NEVER, 2, "12", 0, 2, 12L},  // error from next field
            {2, 19, SignStyle.NEVER, 2, "1", 0, 0, 0},
            {2, 19, SignStyle.NEVER, 2, "1AAAAABBBBBCCCCC", 0, 0, 0},
        };
    }

    //-----------------------------------------------------------------------
    @Test(dataProvider="parseData")
    public void test_parse_fresh(int minWidth, int maxWidth, SignStyle signStyle, int subsequentWidth, String text, int pos, int expectedPos, long expectedValue) {
        ParsePosition ppos = new ParsePosition(pos);
        DateTimeFormatter dtf = getFormatter(DAY_OF_MONTH, minWidth, maxWidth, signStyle);
        if (subsequentWidth > 0) {
            // hacky, to reserve space
            dtf = builder.appendValue(DAY_OF_YEAR, subsequentWidth).toFormatter(locale).withDecimalStyle(decimalStyle);
        }
        TemporalAccessor parsed = dtf.parseUnresolved(text, ppos);
        if (ppos.getErrorIndex() != -1) {
            assertEquals(ppos.getErrorIndex(), expectedPos);
        } else {
            assertTrue(subsequentWidth >= 0);
            assertEquals(ppos.getIndex(), expectedPos + subsequentWidth);
            assertEquals(parsed.getLong(DAY_OF_MONTH), expectedValue);
            assertEquals(parsed.query(TemporalQueries.chronology()), null);
            assertEquals(parsed.query(TemporalQueries.zoneId()), null);
        }
    }

    @Test(dataProvider="parseData")
    public void test_parse_textField(int minWidth, int maxWidth, SignStyle signStyle, int subsequentWidth, String text, int pos, int expectedPos, long expectedValue) {
        ParsePosition ppos = new ParsePosition(pos);
        DateTimeFormatter dtf = getFormatter(DAY_OF_WEEK, minWidth, maxWidth, signStyle);
        if (subsequentWidth > 0) {
            // hacky, to reserve space
            dtf = builder.appendValue(DAY_OF_YEAR, subsequentWidth).toFormatter(locale).withDecimalStyle(decimalStyle);
        }
        TemporalAccessor parsed = dtf.parseUnresolved(text, ppos);
        if (ppos.getErrorIndex() != -1) {
            assertEquals(ppos.getErrorIndex(), expectedPos);
        } else {
            assertTrue(subsequentWidth >= 0);
            assertEquals(ppos.getIndex(), expectedPos + subsequentWidth);
            assertEquals(parsed.getLong(DAY_OF_WEEK), expectedValue);
            assertEquals(parsed.query(TemporalQueries.chronology()), null);
            assertEquals(parsed.query(TemporalQueries.zoneId()), null);
        }
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="parseSignsStrict")
    Object[][] provider_parseSignsStrict() {
        return new Object[][] {
            // basics
            {"0", 1, 2, SignStyle.NEVER, 1, 0},
            {"1", 1, 2, SignStyle.NEVER, 1, 1},
            {"2", 1, 2, SignStyle.NEVER, 1, 2},
            {"3", 1, 2, SignStyle.NEVER, 1, 3},
            {"4", 1, 2, SignStyle.NEVER, 1, 4},
            {"5", 1, 2, SignStyle.NEVER, 1, 5},
            {"6", 1, 2, SignStyle.NEVER, 1, 6},
            {"7", 1, 2, SignStyle.NEVER, 1, 7},
            {"8", 1, 2, SignStyle.NEVER, 1, 8},
            {"9", 1, 2, SignStyle.NEVER, 1, 9},
            {"10", 1, 2, SignStyle.NEVER, 2, 10},
            {"100", 1, 2, SignStyle.NEVER, 2, 10},
            {"100", 1, 3, SignStyle.NEVER, 3, 100},

            // never
            {"0", 1, 2, SignStyle.NEVER, 1, 0},
            {"5", 1, 2, SignStyle.NEVER, 1, 5},
            {"50", 1, 2, SignStyle.NEVER, 2, 50},
            {"500", 1, 2, SignStyle.NEVER, 2, 50},
            {"-0", 1, 2, SignStyle.NEVER, 0, null},
            {"-5", 1, 2, SignStyle.NEVER, 0, null},
            {"-50", 1, 2, SignStyle.NEVER, 0, null},
            {"-500", 1, 2, SignStyle.NEVER, 0, null},
            {"-AAA", 1, 2, SignStyle.NEVER, 0, null},
            {"+0", 1, 2, SignStyle.NEVER, 0, null},
            {"+5", 1, 2, SignStyle.NEVER, 0, null},
            {"+50", 1, 2, SignStyle.NEVER, 0, null},
            {"+500", 1, 2, SignStyle.NEVER, 0, null},
            {"+AAA", 1, 2, SignStyle.NEVER, 0, null},

            // not negative
            {"0", 1, 2, SignStyle.NOT_NEGATIVE, 1, 0},
            {"5", 1, 2, SignStyle.NOT_NEGATIVE, 1, 5},
            {"50", 1, 2, SignStyle.NOT_NEGATIVE, 2, 50},
            {"500", 1, 2, SignStyle.NOT_NEGATIVE, 2, 50},
            {"-0", 1, 2, SignStyle.NOT_NEGATIVE, 0, null},
            {"-5", 1, 2, SignStyle.NOT_NEGATIVE, 0, null},
            {"-50", 1, 2, SignStyle.NOT_NEGATIVE, 0, null},
            {"-500", 1, 2, SignStyle.NOT_NEGATIVE, 0, null},
            {"-AAA", 1, 2, SignStyle.NOT_NEGATIVE, 0, null},
            {"+0", 1, 2, SignStyle.NOT_NEGATIVE, 0, null},
            {"+5", 1, 2, SignStyle.NOT_NEGATIVE, 0, null},
            {"+50", 1, 2, SignStyle.NOT_NEGATIVE, 0, null},
            {"+500", 1, 2, SignStyle.NOT_NEGATIVE, 0, null},
            {"+AAA", 1, 2, SignStyle.NOT_NEGATIVE, 0, null},

            // normal
            {"0", 1, 2, SignStyle.NORMAL, 1, 0},
            {"5", 1, 2, SignStyle.NORMAL, 1, 5},
            {"50", 1, 2, SignStyle.NORMAL, 2, 50},
            {"500", 1, 2, SignStyle.NORMAL, 2, 50},
            {"-0", 1, 2, SignStyle.NORMAL, 0, null},
            {"-5", 1, 2, SignStyle.NORMAL, 2, -5},
            {"-50", 1, 2, SignStyle.NORMAL, 3, -50},
            {"-500", 1, 2, SignStyle.NORMAL, 3, -50},
            {"-AAA", 1, 2, SignStyle.NORMAL, 1, null},
            {"+0", 1, 2, SignStyle.NORMAL, 0, null},
            {"+5", 1, 2, SignStyle.NORMAL, 0, null},
            {"+50", 1, 2, SignStyle.NORMAL, 0, null},
            {"+500", 1, 2, SignStyle.NORMAL, 0, null},
            {"+AAA", 1, 2, SignStyle.NORMAL, 0, null},

            // always
            {"0", 1, 2, SignStyle.ALWAYS, 0, null},
            {"5", 1, 2, SignStyle.ALWAYS, 0, null},
            {"50", 1, 2, SignStyle.ALWAYS, 0, null},
            {"500", 1, 2, SignStyle.ALWAYS, 0, null},
            {"-0", 1, 2, SignStyle.ALWAYS, 0, null},
            {"-5", 1, 2, SignStyle.ALWAYS, 2, -5},
            {"-50", 1, 2, SignStyle.ALWAYS, 3, -50},
            {"-500", 1, 2, SignStyle.ALWAYS, 3, -50},
            {"-AAA", 1, 2, SignStyle.ALWAYS, 1, null},
            {"+0", 1, 2, SignStyle.ALWAYS, 2, 0},
            {"+5", 1, 2, SignStyle.ALWAYS, 2, 5},
            {"+50", 1, 2, SignStyle.ALWAYS, 3, 50},
            {"+500", 1, 2, SignStyle.ALWAYS, 3, 50},
            {"+AAA", 1, 2, SignStyle.ALWAYS, 1, null},

            // exceeds pad
            {"0", 1, 2, SignStyle.EXCEEDS_PAD, 1, 0},
            {"5", 1, 2, SignStyle.EXCEEDS_PAD, 1, 5},
            {"50", 1, 2, SignStyle.EXCEEDS_PAD, 0, null},
            {"500", 1, 2, SignStyle.EXCEEDS_PAD, 0, null},
            {"-0", 1, 2, SignStyle.EXCEEDS_PAD, 0, null},
            {"-5", 1, 2, SignStyle.EXCEEDS_PAD, 2, -5},
            {"-50", 1, 2, SignStyle.EXCEEDS_PAD, 3, -50},
            {"-500", 1, 2, SignStyle.EXCEEDS_PAD, 3, -50},
            {"-AAA", 1, 2, SignStyle.EXCEEDS_PAD, 1, null},
            {"+0", 1, 2, SignStyle.EXCEEDS_PAD, 0, null},
            {"+5", 1, 2, SignStyle.EXCEEDS_PAD, 0, null},
            {"+50", 1, 2, SignStyle.EXCEEDS_PAD, 3, 50},
            {"+500", 1, 2, SignStyle.EXCEEDS_PAD, 3, 50},
            {"+AAA", 1, 2, SignStyle.EXCEEDS_PAD, 1, null},
       };
    }

    @Test(dataProvider="parseSignsStrict")
    public void test_parseSignsStrict(String input, int min, int max, SignStyle style, int parseLen, Integer parseVal) throws Exception {
        ParsePosition pos = new ParsePosition(0);
        TemporalAccessor parsed = getFormatter(DAY_OF_MONTH, min, max, style).parseUnresolved(input, pos);
        if (pos.getErrorIndex() != -1) {
            assertEquals(pos.getErrorIndex(), parseLen);
        } else {
            assertEquals(pos.getIndex(), parseLen);
            assertEquals(parsed.getLong(DAY_OF_MONTH), (long)parseVal);
            assertEquals(parsed.query(TemporalQueries.chronology()), null);
            assertEquals(parsed.query(TemporalQueries.zoneId()), null);
        }
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="parseSignsLenient")
    Object[][] provider_parseSignsLenient() {
        return new Object[][] {
            // never
            {"0", 1, 2, SignStyle.NEVER, 1, 0},
            {"5", 1, 2, SignStyle.NEVER, 1, 5},
            {"50", 1, 2, SignStyle.NEVER, 2, 50},
            {"500", 1, 2, SignStyle.NEVER, 3, 500},
            {"-0", 1, 2, SignStyle.NEVER, 2, 0},
            {"-5", 1, 2, SignStyle.NEVER, 2, -5},
            {"-50", 1, 2, SignStyle.NEVER, 3, -50},
            {"-500", 1, 2, SignStyle.NEVER, 4, -500},
            {"-AAA", 1, 2, SignStyle.NEVER, 1, null},
            {"+0", 1, 2, SignStyle.NEVER, 2, 0},
            {"+5", 1, 2, SignStyle.NEVER, 2, 5},
            {"+50", 1, 2, SignStyle.NEVER, 3, 50},
            {"+500", 1, 2, SignStyle.NEVER, 4, 500},
            {"+AAA", 1, 2, SignStyle.NEVER, 1, null},
            {"50", 2, 2, SignStyle.NEVER, 2, 50},
            {"-50", 2, 2, SignStyle.NEVER, 0, null},
            {"+50", 2, 2, SignStyle.NEVER, 0, null},

            // not negative
            {"0", 1, 2, SignStyle.NOT_NEGATIVE, 1, 0},
            {"5", 1, 2, SignStyle.NOT_NEGATIVE, 1, 5},
            {"50", 1, 2, SignStyle.NOT_NEGATIVE, 2, 50},
            {"500", 1, 2, SignStyle.NOT_NEGATIVE, 3, 500},
            {"-0", 1, 2, SignStyle.NOT_NEGATIVE, 2, 0},
            {"-5", 1, 2, SignStyle.NOT_NEGATIVE, 2, -5},
            {"-50", 1, 2, SignStyle.NOT_NEGATIVE, 3, -50},
            {"-500", 1, 2, SignStyle.NOT_NEGATIVE, 4, -500},
            {"-AAA", 1, 2, SignStyle.NOT_NEGATIVE, 1, null},
            {"+0", 1, 2, SignStyle.NOT_NEGATIVE, 2, 0},
            {"+5", 1, 2, SignStyle.NOT_NEGATIVE, 2, 5},
            {"+50", 1, 2, SignStyle.NOT_NEGATIVE, 3, 50},
            {"+500", 1, 2, SignStyle.NOT_NEGATIVE, 4, 500},
            {"+AAA", 1, 2, SignStyle.NOT_NEGATIVE, 1, null},
            {"50", 2, 2, SignStyle.NOT_NEGATIVE, 2, 50},
            {"-50", 2, 2, SignStyle.NOT_NEGATIVE, 0, null},
            {"+50", 2, 2, SignStyle.NOT_NEGATIVE, 0, null},

            // normal
            {"0", 1, 2, SignStyle.NORMAL, 1, 0},
            {"5", 1, 2, SignStyle.NORMAL, 1, 5},
            {"50", 1, 2, SignStyle.NORMAL, 2, 50},
            {"500", 1, 2, SignStyle.NORMAL, 3, 500},
            {"-0", 1, 2, SignStyle.NORMAL, 2, 0},
            {"-5", 1, 2, SignStyle.NORMAL, 2, -5},
            {"-50", 1, 2, SignStyle.NORMAL, 3, -50},
            {"-500", 1, 2, SignStyle.NORMAL, 4, -500},
            {"-AAA", 1, 2, SignStyle.NORMAL, 1, null},
            {"+0", 1, 2, SignStyle.NORMAL, 2, 0},
            {"+5", 1, 2, SignStyle.NORMAL, 2, 5},
            {"+50", 1, 2, SignStyle.NORMAL, 3, 50},
            {"+500", 1, 2, SignStyle.NORMAL, 4, 500},
            {"+AAA", 1, 2, SignStyle.NORMAL, 1, null},
            {"50", 2, 2, SignStyle.NORMAL, 2, 50},
            {"-50", 2, 2, SignStyle.NORMAL, 3, -50},
            {"+50", 2, 2, SignStyle.NORMAL, 3, 50},

            // always
            {"0", 1, 2, SignStyle.ALWAYS, 1, 0},
            {"5", 1, 2, SignStyle.ALWAYS, 1, 5},
            {"50", 1, 2, SignStyle.ALWAYS, 2, 50},
            {"500", 1, 2, SignStyle.ALWAYS, 3, 500},
            {"-0", 1, 2, SignStyle.ALWAYS, 2, 0},
            {"-5", 1, 2, SignStyle.ALWAYS, 2, -5},
            {"-50", 1, 2, SignStyle.ALWAYS, 3, -50},
            {"-500", 1, 2, SignStyle.ALWAYS, 4, -500},
            {"-AAA", 1, 2, SignStyle.ALWAYS, 1, null},
            {"+0", 1, 2, SignStyle.ALWAYS, 2, 0},
            {"+5", 1, 2, SignStyle.ALWAYS, 2, 5},
            {"+50", 1, 2, SignStyle.ALWAYS, 3, 50},
            {"+500", 1, 2, SignStyle.ALWAYS, 4, 500},
            {"+AAA", 1, 2, SignStyle.ALWAYS, 1, null},

            // exceeds pad
            {"0", 1, 2, SignStyle.EXCEEDS_PAD, 1, 0},
            {"5", 1, 2, SignStyle.EXCEEDS_PAD, 1, 5},
            {"50", 1, 2, SignStyle.EXCEEDS_PAD, 2, 50},
            {"500", 1, 2, SignStyle.EXCEEDS_PAD, 3, 500},
            {"-0", 1, 2, SignStyle.EXCEEDS_PAD, 2, 0},
            {"-5", 1, 2, SignStyle.EXCEEDS_PAD, 2, -5},
            {"-50", 1, 2, SignStyle.EXCEEDS_PAD, 3, -50},
            {"-500", 1, 2, SignStyle.EXCEEDS_PAD, 4, -500},
            {"-AAA", 1, 2, SignStyle.EXCEEDS_PAD, 1, null},
            {"+0", 1, 2, SignStyle.EXCEEDS_PAD, 2, 0},
            {"+5", 1, 2, SignStyle.EXCEEDS_PAD, 2, 5},
            {"+50", 1, 2, SignStyle.EXCEEDS_PAD, 3, 50},
            {"+500", 1, 2, SignStyle.EXCEEDS_PAD, 4, 500},
            {"+AAA", 1, 2, SignStyle.EXCEEDS_PAD, 1, null},
       };
    }

    @Test(dataProvider="parseSignsLenient")
    public void test_parseSignsLenient(String input, int min, int max, SignStyle style, int parseLen, Integer parseVal) throws Exception {
        setStrict(false);
        ParsePosition pos = new ParsePosition(0);
        TemporalAccessor parsed = getFormatter(DAY_OF_MONTH, min, max, style).parseUnresolved(input, pos);
        if (pos.getErrorIndex() != -1) {
            assertEquals(pos.getErrorIndex(), parseLen);
        } else {
            assertEquals(pos.getIndex(), parseLen);
            assertEquals(parsed.getLong(DAY_OF_MONTH), (long)parseVal);
            assertEquals(parsed.query(TemporalQueries.chronology()), null);
            assertEquals(parsed.query(TemporalQueries.zoneId()), null);
        }
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="parseDigitsLenient")
    Object[][] provider_parseDigitsLenient() {
        return new Object[][] {
                // never
                {"5", 1, 2, SignStyle.NEVER, 1, 5},
                {"5", 2, 2, SignStyle.NEVER, 1, 5},
                {"54", 1, 3, SignStyle.NEVER, 2, 54},
                {"54", 2, 3, SignStyle.NEVER, 2, 54},
                {"54", 3, 3, SignStyle.NEVER, 2, 54},
                {"543", 1, 3, SignStyle.NEVER, 3, 543},
                {"543", 2, 3, SignStyle.NEVER, 3, 543},
                {"543", 3, 3, SignStyle.NEVER, 3, 543},
                {"5432", 1, 3, SignStyle.NEVER, 4, 5432},
                {"5432", 2, 3, SignStyle.NEVER, 4, 5432},
                {"5432", 3, 3, SignStyle.NEVER, 4, 5432},
                {"5AAA", 2, 3, SignStyle.NEVER, 1, 5},

                // not negative
                {"5", 1, 2, SignStyle.NOT_NEGATIVE, 1, 5},
                {"5", 2, 2, SignStyle.NOT_NEGATIVE, 1, 5},
                {"54", 1, 3, SignStyle.NOT_NEGATIVE, 2, 54},
                {"54", 2, 3, SignStyle.NOT_NEGATIVE, 2, 54},
                {"54", 3, 3, SignStyle.NOT_NEGATIVE, 2, 54},
                {"543", 1, 3, SignStyle.NOT_NEGATIVE, 3, 543},
                {"543", 2, 3, SignStyle.NOT_NEGATIVE, 3, 543},
                {"543", 3, 3, SignStyle.NOT_NEGATIVE, 3, 543},
                {"5432", 1, 3, SignStyle.NOT_NEGATIVE, 4, 5432},
                {"5432", 2, 3, SignStyle.NOT_NEGATIVE, 4, 5432},
                {"5432", 3, 3, SignStyle.NOT_NEGATIVE, 4, 5432},
                {"5AAA", 2, 3, SignStyle.NOT_NEGATIVE, 1, 5},

                // normal
                {"5", 1, 2, SignStyle.NORMAL, 1, 5},
                {"5", 2, 2, SignStyle.NORMAL, 1, 5},
                {"54", 1, 3, SignStyle.NORMAL, 2, 54},
                {"54", 2, 3, SignStyle.NORMAL, 2, 54},
                {"54", 3, 3, SignStyle.NORMAL, 2, 54},
                {"543", 1, 3, SignStyle.NORMAL, 3, 543},
                {"543", 2, 3, SignStyle.NORMAL, 3, 543},
                {"543", 3, 3, SignStyle.NORMAL, 3, 543},
                {"5432", 1, 3, SignStyle.NORMAL, 4, 5432},
                {"5432", 2, 3, SignStyle.NORMAL, 4, 5432},
                {"5432", 3, 3, SignStyle.NORMAL, 4, 5432},
                {"5AAA", 2, 3, SignStyle.NORMAL, 1, 5},

                // always
                {"5", 1, 2, SignStyle.ALWAYS, 1, 5},
                {"5", 2, 2, SignStyle.ALWAYS, 1, 5},
                {"54", 1, 3, SignStyle.ALWAYS, 2, 54},
                {"54", 2, 3, SignStyle.ALWAYS, 2, 54},
                {"54", 3, 3, SignStyle.ALWAYS, 2, 54},
                {"543", 1, 3, SignStyle.ALWAYS, 3, 543},
                {"543", 2, 3, SignStyle.ALWAYS, 3, 543},
                {"543", 3, 3, SignStyle.ALWAYS, 3, 543},
                {"5432", 1, 3, SignStyle.ALWAYS, 4, 5432},
                {"5432", 2, 3, SignStyle.ALWAYS, 4, 5432},
                {"5432", 3, 3, SignStyle.ALWAYS, 4, 5432},
                {"5AAA", 2, 3, SignStyle.ALWAYS, 1, 5},

                // exceeds pad
                {"5", 1, 2, SignStyle.EXCEEDS_PAD, 1, 5},
                {"5", 2, 2, SignStyle.EXCEEDS_PAD, 1, 5},
                {"54", 1, 3, SignStyle.EXCEEDS_PAD, 2, 54},
                {"54", 2, 3, SignStyle.EXCEEDS_PAD, 2, 54},
                {"54", 3, 3, SignStyle.EXCEEDS_PAD, 2, 54},
                {"543", 1, 3, SignStyle.EXCEEDS_PAD, 3, 543},
                {"543", 2, 3, SignStyle.EXCEEDS_PAD, 3, 543},
                {"543", 3, 3, SignStyle.EXCEEDS_PAD, 3, 543},
                {"5432", 1, 3, SignStyle.EXCEEDS_PAD, 4, 5432},
                {"5432", 2, 3, SignStyle.EXCEEDS_PAD, 4, 5432},
                {"5432", 3, 3, SignStyle.EXCEEDS_PAD, 4, 5432},
                {"5AAA", 2, 3, SignStyle.EXCEEDS_PAD, 1, 5},
        };
    }

    @Test(dataProvider="parseDigitsLenient")
    public void test_parseDigitsLenient(String input, int min, int max, SignStyle style, int parseLen, Integer parseVal) throws Exception {
        setStrict(false);
        ParsePosition pos = new ParsePosition(0);
        TemporalAccessor parsed = getFormatter(DAY_OF_MONTH, min, max, style).parseUnresolved(input, pos);
        if (pos.getErrorIndex() != -1) {
            assertEquals(pos.getErrorIndex(), parseLen);
        } else {
            assertEquals(pos.getIndex(), parseLen);
            assertEquals(parsed.getLong(DAY_OF_MONTH), (long)parseVal);
            assertEquals(parsed.query(TemporalQueries.chronology()), null);
            assertEquals(parsed.query(TemporalQueries.zoneId()), null);
        }
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="parseDigitsAdjacentLenient")
    Object[][] provider_parseDigitsAdjacentLenient() {
        return new Object[][] {
                // never
                {"5", 1, null, null},
                {"54", 1, null, null},

                {"543", 3, 5, 43},
                {"543A", 3, 5, 43},

                {"5432", 4, 54, 32},
                {"5432A", 4, 54, 32},

                {"54321", 5, 543, 21},
                {"54321A", 5, 543, 21},
        };
    }

    @Test(dataProvider="parseDigitsAdjacentLenient")
    public void test_parseDigitsAdjacentLenient(String input, int parseLen, Integer parseMonth, Integer parsedDay) throws Exception {
        setStrict(false);
        ParsePosition pos = new ParsePosition(0);
        DateTimeFormatter f = builder
                .appendValue(MONTH_OF_YEAR, 1, 2, SignStyle.NORMAL)
                .appendValue(DAY_OF_MONTH, 2).toFormatter(locale).withDecimalStyle(decimalStyle);
        TemporalAccessor parsed = f.parseUnresolved(input, pos);
        if (pos.getErrorIndex() != -1) {
            assertEquals(pos.getErrorIndex(), parseLen);
        } else {
            assertEquals(pos.getIndex(), parseLen);
            assertEquals(parsed.getLong(MONTH_OF_YEAR), (long) parseMonth);
            assertEquals(parsed.getLong(DAY_OF_MONTH), (long) parsedDay);
            assertEquals(parsed.query(TemporalQueries.chronology()), null);
            assertEquals(parsed.query(TemporalQueries.zoneId()), null);
        }
    }

}
