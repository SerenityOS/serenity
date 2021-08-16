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

import static org.testng.Assert.assertEquals;

import java.text.ParsePosition;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.time.temporal.TemporalQuery;
import java.util.Locale;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test formatter chrono.
 */
@Test
public class TCKChronoPrinterParser {
    // this test assumes ISO, ThaiBuddhist and Japanese are available

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
        builder.appendChronologyId().toFormatter().parseUnresolved("ISO", new ParsePosition(-1));
    }

    @Test(expectedExceptions=IndexOutOfBoundsException.class)
    public void test_parse_offEndPosition() {
        builder.appendChronologyId().toFormatter().parseUnresolved("ISO", new ParsePosition(4));
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="parseValid")
    Object[][] data_parseValid() {
        return new Object[][] {
                {"ISO", IsoChronology.INSTANCE},
                {"ThaiBuddhist", ThaiBuddhistChronology.INSTANCE},
                {"Japanese", JapaneseChronology.INSTANCE},

                {"ISO2012", IsoChronology.INSTANCE},
                {"ThaiBuddhistXXX", ThaiBuddhistChronology.INSTANCE},
                {"JapaneseXXX", JapaneseChronology.INSTANCE},
        };
    }

    @Test(dataProvider="parseValid")
    public void test_parseValid_caseSensitive(String text, Chronology expected) {
        builder.appendChronologyId();
        TemporalAccessor parsed = builder.toFormatter().parseUnresolved(text, pos);
        assertEquals(pos.getIndex(), expected.getId().length());
        assertEquals(pos.getErrorIndex(), -1);
        assertEquals(parsed.query(TemporalQueries.chronology()), expected);
    }

    @Test(dataProvider="parseValid")
    public void test_parseValid_caseSensitive_lowercaseRejected(String text, Chronology expected) {
        builder.appendChronologyId();
        TemporalAccessor parsed = builder.toFormatter().parseUnresolved(text.toLowerCase(Locale.ENGLISH), pos);
        assertEquals(pos.getIndex(), 0);
        assertEquals(pos.getErrorIndex(), 0);
        assertEquals(parsed, null);
    }

    @Test(dataProvider="parseValid")
    public void test_parseValid_caseInsensitive(String text, Chronology expected) {
        builder.parseCaseInsensitive().appendChronologyId();
        TemporalAccessor parsed = builder.toFormatter().parseUnresolved(text.toLowerCase(Locale.ENGLISH), pos);
        assertEquals(pos.getIndex(), expected.getId().length());
        assertEquals(pos.getErrorIndex(), -1);
        assertEquals(parsed.query(TemporalQueries.chronology()), expected);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="parseInvalid")
    Object[][] data_parseInvalid() {
        return new Object[][] {
                {"Rubbish"},
                {"IS"},
                {"Thai"},
                {"Japan"},
        };
    }

    @Test(dataProvider="parseInvalid")
    public void test_parseInvalid(String text) {
        builder.appendChronologyId();
        TemporalAccessor parsed = builder.toFormatter().parseUnresolved(text, pos);
        assertEquals(pos.getIndex(), 0);
        assertEquals(pos.getErrorIndex(), 0);
        assertEquals(parsed, null);
    }

}
