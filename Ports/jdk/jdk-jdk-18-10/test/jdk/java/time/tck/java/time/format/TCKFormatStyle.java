/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package tck.java.time.format;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.temporal.Temporal;
import java.util.Locale;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class TCKFormatStyle {

    private static final ZoneId ZONEID_PARIS = ZoneId.of("Europe/Paris");
    private static final ZoneId OFFSET_PTWO = ZoneOffset.of("+02:00");

    //-----------------------------------------------------------------------
    // valueOf()
    //-----------------------------------------------------------------------
    @Test
    public void test_valueOf() {
        for (FormatStyle style : FormatStyle.values()) {
            assertEquals(FormatStyle.valueOf(style.name()), style);
        }
    }

    @DataProvider(name="formatStyle")
    Object[][] data_formatStyle() {
        return new Object[][] {
                {ZonedDateTime.of(LocalDateTime.of(2001, 10, 2, 1, 2, 3), ZONEID_PARIS), FormatStyle.FULL, "Tuesday, October 2, 2001 at 1:02:03 AM Central European Summer Time Europe/Paris"},
                {ZonedDateTime.of(LocalDateTime.of(2001, 10, 2, 1, 2, 3), ZONEID_PARIS), FormatStyle.LONG, "October 2, 2001 at 1:02:03 AM CEST Europe/Paris"},
                {ZonedDateTime.of(LocalDateTime.of(2001, 10, 2, 1, 2, 3), ZONEID_PARIS), FormatStyle.MEDIUM, "Oct 2, 2001, 1:02:03 AM Europe/Paris"},
                {ZonedDateTime.of(LocalDateTime.of(2001, 10, 2, 1, 2, 3), ZONEID_PARIS), FormatStyle.SHORT, "10/2/01, 1:02 AM Europe/Paris"},

                {ZonedDateTime.of(LocalDateTime.of(2001, 10, 2, 1, 2, 3), OFFSET_PTWO), FormatStyle.FULL, "Tuesday, October 2, 2001 at 1:02:03 AM +02:00 +02:00"},
                {ZonedDateTime.of(LocalDateTime.of(2001, 10, 2, 1, 2, 3), OFFSET_PTWO), FormatStyle.LONG, "October 2, 2001 at 1:02:03 AM +02:00 +02:00"},
                {ZonedDateTime.of(LocalDateTime.of(2001, 10, 2, 1, 2, 3), OFFSET_PTWO), FormatStyle.MEDIUM, "Oct 2, 2001, 1:02:03 AM +02:00"},
                {ZonedDateTime.of(LocalDateTime.of(2001, 10, 2, 1, 2, 3), OFFSET_PTWO), FormatStyle.SHORT, "10/2/01, 1:02 AM +02:00"},
        };
    }

    @Test(dataProvider = "formatStyle")
    public void test_formatStyle(Temporal temporal, FormatStyle style, String formattedStr) {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        DateTimeFormatter formatter = builder.appendLocalized(style, style).appendLiteral(" ").appendZoneOrOffsetId().toFormatter();
        formatter = formatter.withLocale(Locale.US);
        assertEquals(formatter.format(temporal), formattedStr);
    }
}
