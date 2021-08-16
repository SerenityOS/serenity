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
package test.java.time.format;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DecimalStyle;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.Locale;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Abstract PrinterParser test.
 */
@Test
public class AbstractTestPrinterParser {

    protected StringBuilder buf;
    protected DateTimeFormatterBuilder builder;
    protected TemporalAccessor dta;
    protected Locale locale;
    protected DecimalStyle decimalStyle;


    @BeforeMethod
    public void setUp() {
        buf = new StringBuilder();
        builder = new DateTimeFormatterBuilder();
        dta = ZonedDateTime.of(LocalDateTime.of(2011, 6, 30, 12, 30, 40, 0), ZoneId.of("Europe/Paris"));
        locale = Locale.ENGLISH;
        decimalStyle = DecimalStyle.STANDARD;
    }

    protected void setCaseSensitive(boolean caseSensitive) {
        if (caseSensitive) {
            builder.parseCaseSensitive();
        } else {
            builder.parseCaseInsensitive();
        }
    }

    protected void setStrict(boolean strict) {
        if (strict) {
            builder.parseStrict();
        } else {
            builder.parseLenient();
        }
    }

    protected DateTimeFormatter getFormatter() {
        return builder.toFormatter(locale).withDecimalStyle(decimalStyle);
    }

    protected DateTimeFormatter getFormatter(char c) {
        return builder.appendLiteral(c).toFormatter(locale).withDecimalStyle(decimalStyle);
    }

    protected DateTimeFormatter getFormatter(String s) {
        return builder.appendLiteral(s).toFormatter(locale).withDecimalStyle(decimalStyle);
    }

    protected DateTimeFormatter getFormatter(TemporalField field) {
        return builder.appendText(field).toFormatter(locale).withDecimalStyle(decimalStyle);
    }

    protected DateTimeFormatter getFormatter(TemporalField field, TextStyle style) {
        return builder.appendText(field, style).toFormatter(locale).withDecimalStyle(decimalStyle);
    }

    protected DateTimeFormatter getFormatter(TemporalField field, int minWidth, int maxWidth, SignStyle signStyle) {
        return builder.appendValue(field, minWidth, maxWidth, signStyle).toFormatter(locale).withDecimalStyle(decimalStyle);
    }

    protected DateTimeFormatter getFormatter(String pattern, String noOffsetText) {
        return builder.appendOffset(pattern, noOffsetText).toFormatter(locale).withDecimalStyle(decimalStyle);
    }

    protected DateTimeFormatter getPatternFormatter(String pattern) {
        return builder.appendPattern(pattern).toFormatter(locale).withDecimalStyle(decimalStyle);
    }

    protected static final TemporalAccessor EMPTY_DTA = new TemporalAccessor() {
        public boolean isSupported(TemporalField field) {
            return true;
        }
        @Override
        public long getLong(TemporalField field) {
            throw new DateTimeException("Mock");
        }
    };
}
