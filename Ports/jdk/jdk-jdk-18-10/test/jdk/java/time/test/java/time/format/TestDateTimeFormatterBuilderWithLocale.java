/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @modules jdk.localedata
 */
package test.java.time.format;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.JapaneseEra;
import java.time.chrono.MinguoChronology;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.format.ResolverStyle;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test DateTimeFormatterBuilder.
 */
@Test
public class TestDateTimeFormatterBuilderWithLocale {

    private DateTimeFormatterBuilder builder;

    @BeforeMethod
    public void setUp() {
        builder = new DateTimeFormatterBuilder();
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

    //-----------------------------------------------------------------------
    @DataProvider(name="mapTextLookup")
    Object[][] data_mapTextLookup() {
        return new Object[][] {
            {IsoChronology.INSTANCE.date(1, 1, 1), Locale.ENGLISH},
            {JapaneseChronology.INSTANCE.date(JapaneseEra.HEISEI, 1, 1, 8), Locale.ENGLISH},
            {MinguoChronology.INSTANCE.date(1, 1, 1), Locale.ENGLISH},
            {ThaiBuddhistChronology.INSTANCE.date(1, 1, 1), Locale.ENGLISH},
        };
    }

    @Test(dataProvider="mapTextLookup")
    public void test_appendText_mapTextLookup(ChronoLocalDate date, Locale locale) {
        final String firstYear = "firstYear";
        final String firstMonth = "firstMonth";
        final String firstYearMonth = firstYear + firstMonth;
        final long first = 1L;

        DateTimeFormatter formatter = builder
            .appendText(ChronoField.YEAR_OF_ERA, Map.of(first, firstYear))
            .appendText(ChronoField.MONTH_OF_YEAR, Map.of(first, firstMonth))
            .toFormatter(locale)
            .withResolverStyle(ResolverStyle.STRICT);

        assertEquals(date.format(formatter), firstYearMonth);

        TemporalAccessor ta = formatter.parse(firstYearMonth);
        assertEquals(ta.getLong(ChronoField.YEAR_OF_ERA), first);
        assertEquals(ta.getLong(ChronoField.MONTH_OF_YEAR), first);
    }


    //-----------------------------------------------------------------------
    @DataProvider(name="localePatterns")
    Object[][] localizedDateTimePatterns() {
        return new Object[][] {
            // French Locale and ISO Chronology
            {FormatStyle.FULL, FormatStyle.FULL, IsoChronology.INSTANCE, Locale.FRENCH, "EEEE d MMMM y '\u00e0' HH:mm:ss zzzz"},
            {FormatStyle.LONG, FormatStyle.LONG, IsoChronology.INSTANCE, Locale.FRENCH, "d MMMM y '\u00e0' HH:mm:ss z"},
            {FormatStyle.MEDIUM, FormatStyle.MEDIUM, IsoChronology.INSTANCE, Locale.FRENCH, "d MMM y, HH:mm:ss"},
            {FormatStyle.SHORT, FormatStyle.SHORT, IsoChronology.INSTANCE, Locale.FRENCH, "dd/MM/y HH:mm"},
            {FormatStyle.FULL, null, IsoChronology.INSTANCE, Locale.FRENCH, "EEEE d MMMM y"},
            {FormatStyle.LONG, null, IsoChronology.INSTANCE, Locale.FRENCH, "d MMMM y"},
            {FormatStyle.MEDIUM, null, IsoChronology.INSTANCE, Locale.FRENCH, "d MMM y"},
            {FormatStyle.SHORT, null, IsoChronology.INSTANCE, Locale.FRENCH, "dd/MM/y"},
            {null, FormatStyle.FULL, IsoChronology.INSTANCE, Locale.FRENCH, "HH:mm:ss zzzz"},
            {null, FormatStyle.LONG, IsoChronology.INSTANCE, Locale.FRENCH, "HH:mm:ss z"},
            {null, FormatStyle.MEDIUM, IsoChronology.INSTANCE, Locale.FRENCH, "HH:mm:ss"},
            {null, FormatStyle.SHORT, IsoChronology.INSTANCE, Locale.FRENCH, "HH:mm"},

            // Japanese Locale and JapaneseChronology
            {FormatStyle.FULL, FormatStyle.FULL, JapaneseChronology.INSTANCE, Locale.JAPANESE, "Gy\u5e74M\u6708d\u65e5EEEE H\u6642mm\u5206ss\u79d2 zzzz"},
            {FormatStyle.LONG, FormatStyle.LONG, JapaneseChronology.INSTANCE, Locale.JAPANESE, "Gy\u5e74M\u6708d\u65e5 H:mm:ss z"},
            {FormatStyle.MEDIUM, FormatStyle.MEDIUM, JapaneseChronology.INSTANCE, Locale.JAPANESE, "Gy\u5e74M\u6708d\u65e5 H:mm:ss"},
            {FormatStyle.SHORT, FormatStyle.SHORT, JapaneseChronology.INSTANCE, Locale.JAPANESE, "GGGGGy/M/d H:mm"},
            {FormatStyle.FULL, null, JapaneseChronology.INSTANCE, Locale.JAPANESE, "Gy\u5e74M\u6708d\u65e5EEEE"},
            {FormatStyle.LONG, null, JapaneseChronology.INSTANCE, Locale.JAPANESE, "Gy\u5e74M\u6708d\u65e5"},
            {FormatStyle.MEDIUM, null, JapaneseChronology.INSTANCE, Locale.JAPANESE, "Gy\u5e74M\u6708d\u65e5"},
            {FormatStyle.SHORT, null, JapaneseChronology.INSTANCE, Locale.JAPANESE, "GGGGGy/M/d"},
            {null, FormatStyle.FULL, JapaneseChronology.INSTANCE, Locale.JAPANESE, "H\u6642mm\u5206ss\u79d2 zzzz"},
            {null, FormatStyle.LONG, JapaneseChronology.INSTANCE, Locale.JAPANESE, "H:mm:ss z"},
            {null, FormatStyle.MEDIUM, JapaneseChronology.INSTANCE, Locale.JAPANESE, "H:mm:ss"},
            {null, FormatStyle.SHORT, JapaneseChronology.INSTANCE, Locale.JAPANESE, "H:mm"},

            // Chinese Local and Chronology
            {FormatStyle.FULL, FormatStyle.FULL, MinguoChronology.INSTANCE, Locale.CHINESE, "Gy\u5e74M\u6708d\u65e5EEEE zzzz ah:mm:ss"},
            {FormatStyle.LONG, FormatStyle.LONG, MinguoChronology.INSTANCE, Locale.CHINESE, "Gy\u5e74M\u6708d\u65e5 z ah:mm:ss"},
            {FormatStyle.MEDIUM, FormatStyle.MEDIUM, MinguoChronology.INSTANCE, Locale.CHINESE, "Gy\u5e74M\u6708d\u65e5 ah:mm:ss"},
            {FormatStyle.SHORT, FormatStyle.SHORT, MinguoChronology.INSTANCE, Locale.CHINESE, "Gy/M/d ah:mm"},
            {FormatStyle.FULL, null, MinguoChronology.INSTANCE, Locale.CHINESE, "Gy\u5e74M\u6708d\u65e5EEEE"},
            {FormatStyle.LONG, null, MinguoChronology.INSTANCE, Locale.CHINESE, "Gy\u5e74M\u6708d\u65e5"},
            {FormatStyle.MEDIUM, null, MinguoChronology.INSTANCE, Locale.CHINESE, "Gy\u5e74M\u6708d\u65e5"},
            {FormatStyle.SHORT, null, MinguoChronology.INSTANCE, Locale.CHINESE, "Gy/M/d"},
            {null, FormatStyle.FULL, MinguoChronology.INSTANCE, Locale.CHINESE, "zzzz ah:mm:ss"},
            {null, FormatStyle.LONG, MinguoChronology.INSTANCE, Locale.CHINESE, "z ah:mm:ss"},
            {null, FormatStyle.MEDIUM, MinguoChronology.INSTANCE, Locale.CHINESE, "ah:mm:ss"},
            {null, FormatStyle.SHORT, MinguoChronology.INSTANCE, Locale.CHINESE, "ah:mm"},
        };
    }

    @Test(dataProvider="localePatterns")
    public void test_getLocalizedDateTimePattern(FormatStyle dateStyle, FormatStyle timeStyle,
            Chronology chrono, Locale locale, String expected) {
        String actual = DateTimeFormatterBuilder.getLocalizedDateTimePattern(dateStyle, timeStyle, chrono, locale);
        assertEquals(actual, expected, "Pattern " + convertNonAscii(actual));
    }

    /**
     * Returns a string that includes non-ascii characters after expanding
     * the non-ascii characters to their Java language \\uxxxx form.
     * @param input an input string
     * @return the encoded string.
     */
    private String convertNonAscii(String input) {
        StringBuilder sb = new StringBuilder(input.length() * 6);
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch < 255) {
                sb.append(ch);
            } else {
                sb.append("\\u");
                sb.append(Integer.toHexString(ch));
            }
        }
        return sb.toString();
    }

    private static Temporal date(int y, int m, int d) {
        return LocalDate.of(y, m, d);
    }
}
