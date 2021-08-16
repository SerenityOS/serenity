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
package tck.java.time.format;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.text.ParsePosition;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.TemporalAccessor;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test padding behavior of formatter.
 */
@Test
public class TCKPadPrinterParser {

    private DateTimeFormatterBuilder builder;
    private ParsePosition pos;

    @BeforeMethod
    public void setUp() {
        builder = new DateTimeFormatterBuilder();
        pos = new ParsePosition(0);
    }

    //-----------------------------------------------------------------------
    @Test(expectedExceptions=IndexOutOfBoundsException.class)
    public void test_parse_negativePosition() {
        builder.padNext(3, '-').appendLiteral('Z');
        builder.toFormatter().parseUnresolved("--Z", new ParsePosition(-1));
    }

    @Test(expectedExceptions=IndexOutOfBoundsException.class)
    public void test_parse_offEndPosition() {
        builder.padNext(3, '-').appendLiteral('Z');
        builder.toFormatter().parseUnresolved("--Z", new ParsePosition(4));
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="parseStrict")
    Object[][] data_parseStrict() {
        return new Object[][] {
                {"222", 3, -1, 222},
                {"222X", 3, -1, 222},
                {"#22", 3, -1, 22},
                {"#22X", 3, -1, 22},
                {"##2", 3, -1, 2},
                {"##2X", 3, -1, 2},
                {"##22", 3, -1, 2},
                {"-22", 3, -1, -22},
                {"#-2", 3, -1, -2},
                {"3", 0, 0, null},
                {"3X", 0, 0, null},
                {"#3", 0, 0, null},
                {"#3X", 0, 1, null},
                {"##A", 0, 2, null},
                {"  3", 0, 0, null},
                {"##", 0, 0, null},
                {"#", 0, 0, null},
                {"", 0, 0, null},
        };
    }

    @Test(dataProvider="parseStrict")
    public void test_parseStrict(String text, int expectedIndex, int expectedErrorIndex, Number expectedMonth) {
        builder.padNext(3, '#').appendValue(MONTH_OF_YEAR, 1, 3, SignStyle.NORMAL);
        TemporalAccessor parsed = builder.toFormatter().parseUnresolved(text, pos);
        assertEquals(pos.getIndex(), expectedIndex);
        assertEquals(pos.getErrorIndex(), expectedErrorIndex);
        if (expectedMonth != null) {
            assertEquals(parsed.isSupported(MONTH_OF_YEAR), true);
            assertEquals(parsed.getLong(MONTH_OF_YEAR), expectedMonth.longValue());
        } else {
            assertEquals(parsed, null);
        }
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="parseLenient")
    Object[][] data_parseLenient() {
        return new Object[][] {
                {"222", 3, -1, 222},
                {"222X", 3, -1, 222},
                {"#22", 3, -1, 22},
                {"#22X", 3, -1, 22},
                {"##2", 3, -1, 2},
                {"##2X", 3, -1, 2},
                {"##22", 3, -1, 2},
                {"-22", 3, -1, -22},
                {"#-2", 3, -1, -2},
                {"3", 1, -1, 3},
                {"3X", 1, -1, 3},
                {"33", 2, -1, 33},
                {"33X", 2, -1, 33},
                {"#3", 2, -1, 3},
                {"#3X", 2, -1, 3},
                {"##A", 0, 2, null},
                {"  1", 0, 0, null},
                {"##", 0, 2, null},
                {"#", 0, 1, null},
                {"", 0, 0, null},
        };
    }

    @Test(dataProvider="parseLenient")
    public void test_parseLenient(String text, int expectedIndex, int expectedErrorIndex, Number expectedMonth) {
        builder.parseLenient().padNext(3, '#').appendValue(MONTH_OF_YEAR, 1, 3, SignStyle.NORMAL);
        TemporalAccessor parsed = builder.toFormatter().parseUnresolved(text, pos);
        assertEquals(pos.getIndex(), expectedIndex);
        assertEquals(pos.getErrorIndex(), expectedErrorIndex);
        if (expectedMonth != null) {
            assertEquals(parsed.isSupported(MONTH_OF_YEAR), true);
            assertEquals(parsed.getLong(MONTH_OF_YEAR), expectedMonth.longValue());
        } else {
            assertEquals(parsed, null);
        }
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parse_decoratedStartsWithPad() {
        builder.padNext(8, '-').appendLiteral("-HELLO-");
        TemporalAccessor parsed = builder.toFormatter().parseUnresolved("--HELLO-", pos);
        assertEquals(pos.getIndex(), 0);
        assertEquals(pos.getErrorIndex(), 2);
        assertEquals(parsed, null);
    }

    @Test
    public void test_parse_decoratedStartsWithPad_number() {
        builder.padNext(3, '-').appendValue(MONTH_OF_YEAR, 1, 2, SignStyle.NORMAL);
        TemporalAccessor parsed = builder.toFormatter().parseUnresolved("--2", pos);
        assertEquals(pos.getIndex(), 3);
        assertEquals(pos.getErrorIndex(), -1);
        assertEquals(parsed.isSupported(MONTH_OF_YEAR), true);
        assertEquals(parsed.getLong(MONTH_OF_YEAR), 2L);  // +2, not -2
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parse_decoratedEmpty_strict() {
        builder.padNext(4, '-').optionalStart().appendValue(DAY_OF_MONTH).optionalEnd();
        TemporalAccessor parsed = builder.toFormatter().parseUnresolved("----", pos);
        assertEquals(pos.getIndex(), 4);
        assertEquals(pos.getErrorIndex(), -1);
        assertNotNull(parsed);
    }

    @Test
    public void test_parse_decoratedEmpty_lenient() {
        builder.parseLenient().padNext(4, '-').optionalStart().appendValue(DAY_OF_MONTH).optionalEnd();
        TemporalAccessor parsed = builder.toFormatter().parseUnresolved("----", pos);
        assertEquals(pos.getIndex(), 4);
        assertEquals(pos.getErrorIndex(), -1);
        assertNotNull(parsed);
    }

}
