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
 * Copyright (c) 2011-2012, Stephen Colebourne & Michael Nascimento Santos
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
 * @bug 8231273
 */

package test.java.time.format;

import static java.time.temporal.ChronoField.AMPM_OF_DAY;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static org.testng.Assert.assertEquals;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalField;
import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test SimpleDateTimeTextProviderWithLocale.
 */
@Test
public class TestDateTimeTextProviderWithLocale extends AbstractTestPrinterParser {

    Locale enUS = new Locale("en", "US");
    Locale ptBR = new Locale("pt", "BR");

    //-----------------------------------------------------------------------
    @DataProvider(name = "Text")
    Object[][] data_text() {
        return new Object[][] {
            {DAY_OF_WEEK, 1, TextStyle.SHORT, ptBR, "seg."},
            {DAY_OF_WEEK, 2, TextStyle.SHORT, ptBR, "ter."},
            {DAY_OF_WEEK, 3, TextStyle.SHORT, ptBR, "qua."},
            {DAY_OF_WEEK, 4, TextStyle.SHORT, ptBR, "qui."},
            {DAY_OF_WEEK, 5, TextStyle.SHORT, ptBR, "sex."},
            {DAY_OF_WEEK, 6, TextStyle.SHORT, ptBR, "s\u00E1b."},
            {DAY_OF_WEEK, 7, TextStyle.SHORT, ptBR, "dom."},

            {DAY_OF_WEEK, 1, TextStyle.FULL, ptBR, "segunda-feira"},
            {DAY_OF_WEEK, 2, TextStyle.FULL, ptBR, "ter\u00E7a-feira"},
            {DAY_OF_WEEK, 3, TextStyle.FULL, ptBR, "quarta-feira"},
            {DAY_OF_WEEK, 4, TextStyle.FULL, ptBR, "quinta-feira"},
            {DAY_OF_WEEK, 5, TextStyle.FULL, ptBR, "sexta-feira"},
            {DAY_OF_WEEK, 6, TextStyle.FULL, ptBR, "s\u00E1bado"},
            {DAY_OF_WEEK, 7, TextStyle.FULL, ptBR, "domingo"},

            {MONTH_OF_YEAR, 1, TextStyle.SHORT, ptBR, "jan."},
            {MONTH_OF_YEAR, 2, TextStyle.SHORT, ptBR, "fev."},
            {MONTH_OF_YEAR, 3, TextStyle.SHORT, ptBR, "mar."},
            {MONTH_OF_YEAR, 4, TextStyle.SHORT, ptBR, "abr."},
            {MONTH_OF_YEAR, 5, TextStyle.SHORT, ptBR, "mai."},
            {MONTH_OF_YEAR, 6, TextStyle.SHORT, ptBR, "jun."},
            {MONTH_OF_YEAR, 7, TextStyle.SHORT, ptBR, "jul."},
            {MONTH_OF_YEAR, 8, TextStyle.SHORT, ptBR, "ago."},
            {MONTH_OF_YEAR, 9, TextStyle.SHORT, ptBR, "set."},
            {MONTH_OF_YEAR, 10, TextStyle.SHORT, ptBR, "out."},
            {MONTH_OF_YEAR, 11, TextStyle.SHORT, ptBR, "nov."},
            {MONTH_OF_YEAR, 12, TextStyle.SHORT, ptBR, "dez."},

            {MONTH_OF_YEAR, 1, TextStyle.FULL, ptBR, "janeiro"},
            {MONTH_OF_YEAR, 2, TextStyle.FULL, ptBR, "fevereiro"},
            {MONTH_OF_YEAR, 3, TextStyle.FULL, ptBR, "mar\u00E7o"},
            {MONTH_OF_YEAR, 4, TextStyle.FULL, ptBR, "abril"},
            {MONTH_OF_YEAR, 5, TextStyle.FULL, ptBR, "maio"},
            {MONTH_OF_YEAR, 6, TextStyle.FULL, ptBR, "junho"},
            {MONTH_OF_YEAR, 7, TextStyle.FULL, ptBR, "julho"},
            {MONTH_OF_YEAR, 8, TextStyle.FULL, ptBR, "agosto"},
            {MONTH_OF_YEAR, 9, TextStyle.FULL, ptBR, "setembro"},
            {MONTH_OF_YEAR, 10, TextStyle.FULL, ptBR, "outubro"},
            {MONTH_OF_YEAR, 11, TextStyle.FULL, ptBR, "novembro"},
            {MONTH_OF_YEAR, 12, TextStyle.FULL, ptBR, "dezembro"},

        };
    }

    @Test(dataProvider = "Text")
    public void test_getText(TemporalField field, Number value, TextStyle style, Locale locale, String expected) {
          DateTimeFormatter fmt = getFormatter(field, style).withLocale(locale);
          assertEquals(fmt.format(ZonedDateTime.now().with(field, value.longValue())), expected);
    }

}
