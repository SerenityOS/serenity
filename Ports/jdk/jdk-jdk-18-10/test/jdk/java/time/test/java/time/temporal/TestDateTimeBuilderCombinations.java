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
package test.java.time.temporal;

import static java.time.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_MONTH;
import static java.time.temporal.ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_MONTH;
import static java.time.temporal.ChronoField.ALIGNED_WEEK_OF_YEAR;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.DAY_OF_YEAR;
import static java.time.temporal.ChronoField.EPOCH_DAY;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.PROLEPTIC_MONTH;
import static java.time.temporal.ChronoField.YEAR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test.
 */
public class TestDateTimeBuilderCombinations {

    @DataProvider(name = "combine")
    Object[][] data_combine() {
        return new Object[][] {
            {YEAR, 2012, MONTH_OF_YEAR, 6, DAY_OF_MONTH, 3, null, null, LocalDate.class, LocalDate.of(2012, 6, 3)},
            {PROLEPTIC_MONTH, 2012 * 12 + 6 - 1, DAY_OF_MONTH, 3, null, null, null, null, LocalDate.class, LocalDate.of(2012, 6, 3)},
            {YEAR, 2012, ALIGNED_WEEK_OF_YEAR, 6, DAY_OF_WEEK, 3, null, null, LocalDate.class, LocalDate.of(2012, 2, 8)},
            {YEAR, 2012, DAY_OF_YEAR, 155, null, null, null, null, LocalDate.class, LocalDate.of(2012, 6, 3)},
//            {ERA, 1, YEAR_OF_ERA, 2012, DAY_OF_YEAR, 155, null, null, LocalDate.class, LocalDate.of(2012, 6, 3)},
            {YEAR, 2012, MONTH_OF_YEAR, 6, null, null, null, null, LocalDate.class, null},
            {EPOCH_DAY, 12, null, null, null, null, null, null, LocalDate.class, LocalDate.of(1970, 1, 13)},
        };
    }

    @Test(dataProvider = "combine")
    public void test_derive(final TemporalField field1, final Number value1,
                            final TemporalField field2, final Number value2,
                            final TemporalField field3, final Number value3,
                            final TemporalField field4, final Number value4,
                            Class<?> query, Object expectedVal) {
        // mock for testing that does not fully comply with TemporalAccessor contract
        TemporalAccessor test = new TemporalAccessor() {
            @Override
            public boolean isSupported(TemporalField field) {
                return field == field1 || field == field2 || field == field3 || field == field4;
            }
            @Override
            public long getLong(TemporalField field) {
                if (field == field1) {
                    return value1.longValue();
                }
                if (field == field2) {
                    return value2.longValue();
                }
                if (field == field3) {
                    return value3.longValue();
                }
                if (field == field4) {
                    return value4.longValue();
                }
                throw new DateTimeException("Unsupported");
            }
        };
        String str = "";
        DateTimeFormatterBuilder dtfb = new DateTimeFormatterBuilder();
        dtfb.appendValue(field1).appendLiteral('-');
        str += value1 + "-";
        if (field2 != null) {
            dtfb.appendValue(field2).appendLiteral('-');
            str += value2 + "-";
        }
        if (field3 != null) {
            dtfb.appendValue(field3).appendLiteral('-');
            str += value3 + "-";
        }
        if (field4 != null) {
            dtfb.appendValue(field4).appendLiteral('-');
            str += value4 + "-";
        }
        TemporalAccessor parsed = dtfb.toFormatter().parse(str);
        if (query == LocalDate.class) {
            if (expectedVal != null) {
                assertEquals(parsed.query(LocalDate::from), expectedVal);
            } else {
                try {
                    parsed.query(LocalDate::from);
                    fail();
                } catch (DateTimeException ex) {
                    // expected
                }
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    //-----------------------------------------------------------------------
    @DataProvider(name = "normalized")
    Object[][] data_normalized() {
        return new Object[][] {
            {YEAR, 2127, YEAR, 2127},
            {MONTH_OF_YEAR, 12, MONTH_OF_YEAR, 12},
            {DAY_OF_YEAR, 127, DAY_OF_YEAR, 127},
            {DAY_OF_MONTH, 23, DAY_OF_MONTH, 23},
            {DAY_OF_WEEK, 127, DAY_OF_WEEK, 127L},
            {ALIGNED_WEEK_OF_YEAR, 23, ALIGNED_WEEK_OF_YEAR, 23},
            {ALIGNED_DAY_OF_WEEK_IN_YEAR, 4, ALIGNED_DAY_OF_WEEK_IN_YEAR, 4L},
            {ALIGNED_WEEK_OF_MONTH, 4, ALIGNED_WEEK_OF_MONTH, 4},
            {ALIGNED_DAY_OF_WEEK_IN_MONTH, 3, ALIGNED_DAY_OF_WEEK_IN_MONTH, 3},
            {PROLEPTIC_MONTH, 27, PROLEPTIC_MONTH, null},
            {PROLEPTIC_MONTH, 27, YEAR, 2},
            {PROLEPTIC_MONTH, 27, MONTH_OF_YEAR, 4},
        };
    }

    @Test(dataProvider = "normalized")
    public void test_normalized(final TemporalField field1, final Number value1, TemporalField expectedField, Number expectedVal) {
        // mock for testing that does not fully comply with TemporalAccessor contract
        TemporalAccessor test = new TemporalAccessor() {
            @Override
            public boolean isSupported(TemporalField field) {
                return field == field1;
            }
            @Override
            public long getLong(TemporalField field) {
                if (field == field1) {
                    return value1.longValue();
                }
                throw new DateTimeException("Unsupported");
            }
        };
        DateTimeFormatter f = new DateTimeFormatterBuilder().appendValue(field1).toFormatter();
        String str = value1.toString();
        TemporalAccessor temporal = f.parse(str);
        if (expectedVal != null) {
            assertEquals(temporal.getLong(expectedField), expectedVal.longValue());
        } else {
            assertEquals(temporal.isSupported(expectedField), false);
        }
    }

}
