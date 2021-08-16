/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright (c) 2010-2013, Stephen Colebourne & Michael Nascimento Santos
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
import static org.testng.Assert.fail;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.TemporalAccessor;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8166138
 */

/**
 * Test DateTimeFormatterBuilder.appendInstant().
 */
@Test
public class TCKInstantPrinterParser {

    @DataProvider(name="printGrouped")
    Object[][] data_printGrouped() {
        return new Object[][] {
                {0, 0, "1970-01-01T00:00:00Z"},

                {-1, 0, "1969-12-31T23:59:59Z"},
                {1, 0, "1970-01-01T00:00:01Z"},
                {60, 0, "1970-01-01T00:01:00Z"},
                {3600, 0, "1970-01-01T01:00:00Z"},
                {86400, 0, "1970-01-02T00:00:00Z"},

                {182, 2, "1970-01-01T00:03:02.000000002Z"},
                {182, 20, "1970-01-01T00:03:02.000000020Z"},
                {182, 200, "1970-01-01T00:03:02.000000200Z"},
                {182, 2000, "1970-01-01T00:03:02.000002Z"},
                {182, 20000, "1970-01-01T00:03:02.000020Z"},
                {182, 200000, "1970-01-01T00:03:02.000200Z"},
                {182, 2000000, "1970-01-01T00:03:02.002Z"},
                {182, 20000000, "1970-01-01T00:03:02.020Z"},
                {182, 200000000, "1970-01-01T00:03:02.200Z"},

                {Instant.MAX.getEpochSecond(), 999999999, "+1000000000-12-31T23:59:59.999999999Z"},
                {Instant.MIN.getEpochSecond(), 0, "-1000000000-01-01T00:00:00Z"},
        };
    }

    @Test(dataProvider="printGrouped")
    public void test_print_grouped(long instantSecs, int nano, String expected) {
        Instant instant = Instant.ofEpochSecond(instantSecs, nano);
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendInstant().toFormatter();
        assertEquals(f.format(instant), expected);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="printDigits")
    Object[][] data_printDigits() {
        return new Object[][] {
                {-1, 0, 0, "1970-01-01T00:00:00Z"},
                {0, 0, 0, "1970-01-01T00:00:00Z"},
                {1, 0, 0, "1970-01-01T00:00:00.0Z"},
                {3, 0, 0, "1970-01-01T00:00:00.000Z"},
                {9, 0, 0, "1970-01-01T00:00:00.000000000Z"},

                {-1, -1, 0, "1969-12-31T23:59:59Z"},
                {-1, 1, 0, "1970-01-01T00:00:01Z"},
                {-1, 60, 0, "1970-01-01T00:01:00Z"},
                {-1, 3600, 0, "1970-01-01T01:00:00Z"},
                {-1, 86400, 0, "1970-01-02T00:00:00Z"},

                {-1, 182, 2, "1970-01-01T00:03:02.000000002Z"},
                {-1, 182, 20, "1970-01-01T00:03:02.00000002Z"},
                {-1, 182, 200, "1970-01-01T00:03:02.0000002Z"},
                {-1, 182, 2000, "1970-01-01T00:03:02.000002Z"},
                {-1, 182, 20000, "1970-01-01T00:03:02.00002Z"},
                {-1, 182, 200000, "1970-01-01T00:03:02.0002Z"},
                {-1, 182, 2000000, "1970-01-01T00:03:02.002Z"},
                {-1, 182, 20000000, "1970-01-01T00:03:02.02Z"},
                {-1, 182, 200000000, "1970-01-01T00:03:02.2Z"},

                {0, 182, 2, "1970-01-01T00:03:02Z"},
                {0, 182, 20, "1970-01-01T00:03:02Z"},
                {0, 182, 200, "1970-01-01T00:03:02Z"},
                {0, 182, 2000, "1970-01-01T00:03:02Z"},
                {0, 182, 20000, "1970-01-01T00:03:02Z"},
                {0, 182, 200000, "1970-01-01T00:03:02Z"},
                {0, 182, 2000000, "1970-01-01T00:03:02Z"},
                {0, 182, 20000000, "1970-01-01T00:03:02Z"},
                {0, 182, 200000000, "1970-01-01T00:03:02Z"},

                {1, 182, 2, "1970-01-01T00:03:02.0Z"},
                {1, 182, 20, "1970-01-01T00:03:02.0Z"},
                {1, 182, 200, "1970-01-01T00:03:02.0Z"},
                {1, 182, 2000, "1970-01-01T00:03:02.0Z"},
                {1, 182, 20000, "1970-01-01T00:03:02.0Z"},
                {1, 182, 200000, "1970-01-01T00:03:02.0Z"},
                {1, 182, 2000000, "1970-01-01T00:03:02.0Z"},
                {1, 182, 20000000, "1970-01-01T00:03:02.0Z"},
                {1, 182, 200000000, "1970-01-01T00:03:02.2Z"},

                {3, 182, 2, "1970-01-01T00:03:02.000Z"},
                {3, 182, 20, "1970-01-01T00:03:02.000Z"},
                {3, 182, 200, "1970-01-01T00:03:02.000Z"},
                {3, 182, 2000, "1970-01-01T00:03:02.000Z"},
                {3, 182, 20000, "1970-01-01T00:03:02.000Z"},
                {3, 182, 200000, "1970-01-01T00:03:02.000Z"},
                {3, 182, 2000000, "1970-01-01T00:03:02.002Z"},
                {3, 182, 20000000, "1970-01-01T00:03:02.020Z"},
                {3, 182, 200000000, "1970-01-01T00:03:02.200Z"},

                {9, 182, 2, "1970-01-01T00:03:02.000000002Z"},
                {9, 182, 20, "1970-01-01T00:03:02.000000020Z"},
                {9, 182, 200, "1970-01-01T00:03:02.000000200Z"},
                {9, 182, 2000, "1970-01-01T00:03:02.000002000Z"},
                {9, 182, 20000, "1970-01-01T00:03:02.000020000Z"},
                {9, 182, 200000, "1970-01-01T00:03:02.000200000Z"},
                {9, 182, 2000000, "1970-01-01T00:03:02.002000000Z"},
                {9, 182, 20000000, "1970-01-01T00:03:02.020000000Z"},
                {9, 182, 200000000, "1970-01-01T00:03:02.200000000Z"},

                {9, Instant.MAX.getEpochSecond(), 999999999, "+1000000000-12-31T23:59:59.999999999Z"},
                {9, Instant.MIN.getEpochSecond(), 0, "-1000000000-01-01T00:00:00.000000000Z"},
        };
    }

    @Test(dataProvider="printDigits")
    public void test_print_digits(int fractionalDigits, long instantSecs, int nano, String expected) {
        Instant instant = Instant.ofEpochSecond(instantSecs, nano);
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendInstant(fractionalDigits).toFormatter();
        assertEquals(f.format(instant), expected);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="parseDigits")
    Object[][] data_parse_digits() {
        return new Object[][] {
                {0, 0, "1970-01-01T00:00:00Z"},
                {0, 0, "1970-01-01T00:00:00Z"},
                {0, 0, "1970-01-01T00:00:00.0Z"},
                {0, 0, "1970-01-01T00:00:00.000Z"},

                {0, 0, "1970-01-01T00:00:00+00:00"},
                {0, 0, "1970-01-01T05:30:00+05:30"},
                {0, 0, "1970-01-01T01:00:00.0+01:00"},

                {-1, 0, "1969-12-31T23:59:59Z"},
                {1, 0, "1970-01-01T00:00:01Z"},
                {60, 0, "1970-01-01T00:01:00Z"},
                {3600, 0, "1970-01-01T01:00:00Z"},
                {86400, 0, "1970-01-02T00:00:00Z"},

                {-1, 0, "1969-12-31T23:59:59+00:00"},
                {1, 0, "1970-01-01T05:30:01+05:30"},
                {60, 0, "1969-12-31T19:01:00-05:00"},
                {3600, 0, "1970-01-01T06:30:00+05:30"},
                {86400, 0, "1970-01-01T19:00:00-05:00"},

                {182, 234000000, "1970-01-01T00:03:02.234Z"},
                {182, 234000000, "1970-01-01T00:03:02.2340Z"},
                {182, 234000000, "1970-01-01T00:03:02.23400Z"},
                {182, 234000000, "1970-01-01T00:03:02.234000Z"},

                {182, 234000000, "1970-01-01T00:03:02.234+00:00"},
                {182, 234000000, "1970-01-01T05:33:02.2340+05:30"},
                {182, 234000000, "1969-12-31T19:03:02.23400-05:00"},
                {182, 234000000, "1970-01-01T00:03:02.234000+00:00"},

        };
    }

    @Test(dataProvider="parseDigits")
    public void test_parse_digitsMinusOne(long instantSecs, int nano, String input) {
        Instant expected = Instant.ofEpochSecond(instantSecs, nano);
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendInstant(-1).toFormatter();
        assertEquals(f.parse(input, Instant::from), expected);
        assertEquals(f.parse(input).query(DateTimeFormatter.parsedExcessDays()), Period.ZERO);
        assertEquals(f.parse(input).query(DateTimeFormatter.parsedLeapSecond()), Boolean.FALSE);
    }

    @DataProvider(name="parseNineDigits")
    Object[][] data_parse_ninedigits() {
        return new Object[][] {
                {0, 0, "1970-01-01T00:00:00.000000000Z"},
                {0, 0, "1970-01-01T05:30:00.000000000+05:30"},

                {182, 234000000, "1970-01-01T00:03:02.234000000Z"},
                {182, 234000000, "1970-01-01T01:03:02.234000000+01:00"},

                {((23 * 60) + 59) * 60 + 59, 123456789, "1970-01-01T23:59:59.123456789Z"},
                {((23 * 60) + 59) * 60 + 59, 123456789, "1970-01-02T05:29:59.123456789+05:30"},

                {Instant.MAX.getEpochSecond(), 999999999, "+1000000000-12-31T23:59:59.999999999Z"},
                {Instant.MIN.getEpochSecond(), 0, "-1000000000-01-01T00:00:00.000000000Z"},
                {Instant.MAX.getEpochSecond(), 999999999, "+1000000000-12-31T23:59:59.999999999+00:00"},
                {Instant.MIN.getEpochSecond(), 0, "-1000000000-01-01T00:00:00.000000000+00:00"},
        };
    }

    @Test(dataProvider="parseNineDigits")
    public void test_parse_digitsNine(long instantSecs, int nano, String input) {
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendInstant(9).toFormatter();
        Instant expected = Instant.ofEpochSecond(instantSecs, nano);
        assertEquals(f.parse(input, Instant::from), expected);
        assertEquals(f.parse(input).query(DateTimeFormatter.parsedExcessDays()), Period.ZERO);
        assertEquals(f.parse(input).query(DateTimeFormatter.parsedLeapSecond()), Boolean.FALSE);
    }

    @DataProvider(name="parseMaxMinInstant")
    Object[][] data_parse_MaxMinInstant() {
        return new Object[][] {
                {"+1000000000-12-31T23:59:59.999999999-01:00"},
                {"-1000000000-01-01T00:00:00.000000000+01:00"}
        };
    }

    @Test(dataProvider="parseMaxMinInstant", expectedExceptions=DateTimeParseException.class)
    public void test_invalid_Instant(String input) {
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendInstant(-1).toFormatter();
        f.parse(input, Instant::from);
    }

    @Test
    public void test_parse_endOfDay() {
        Instant expected = OffsetDateTime.of(1970, 2, 4, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendInstant(-1).toFormatter();
        for (ResolverStyle style : ResolverStyle.values()) {
            TemporalAccessor parsed = f.withResolverStyle(style).parse("1970-02-03T24:00:00Z");
            assertEquals(parsed.query(Instant::from), expected);
            assertEquals(parsed.query(DateTimeFormatter.parsedExcessDays()), Period.ZERO);
            assertEquals(parsed.query(DateTimeFormatter.parsedLeapSecond()), Boolean.FALSE);
        }
    }

    @Test
    public void test_parse_leapSecond() {
        Instant expected = OffsetDateTime.of(1970, 2, 3, 23, 59, 59, 123456789, ZoneOffset.UTC).toInstant();
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendInstant(-1).toFormatter();
        for (ResolverStyle style : ResolverStyle.values()) {
            TemporalAccessor parsed = f.withResolverStyle(style).parse("1970-02-03T23:59:60.123456789Z");
            assertEquals(parsed.query(Instant::from), expected);
            assertEquals(parsed.query(DateTimeFormatter.parsedExcessDays()), Period.ZERO);
            assertEquals(parsed.query(DateTimeFormatter.parsedLeapSecond()), Boolean.TRUE);
        }
    }

    //-----------------------------------------------------------------------
    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendInstant_tooSmall() {
        new DateTimeFormatterBuilder().appendInstant(-2);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_appendInstant_tooBig() {
        new DateTimeFormatterBuilder().appendInstant(10);
    }

    //------------------------------------------------------------------------
    @Test
    public void test_equality() {
        Instant instant1 = Instant.parse("2018-09-12T22:15:51+05:30");
        Instant instant2 = Instant.parse("2018-09-12T16:45:51Z");
        assertEquals(instant2, instant1);
    }

}
