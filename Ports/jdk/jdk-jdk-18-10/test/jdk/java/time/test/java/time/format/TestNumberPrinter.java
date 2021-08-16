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
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.SignStyle;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import test.java.time.temporal.MockFieldValue;

/**
 * Test SimpleNumberPrinterParser.
 */
@Test
public class TestNumberPrinter extends AbstractTestPrinterParser {

    //-----------------------------------------------------------------------
    @Test(expectedExceptions=DateTimeException.class)
    public void test_print_emptyCalendrical() throws Exception {
        getFormatter(DAY_OF_MONTH, 1, 2, SignStyle.NEVER).formatTo(EMPTY_DTA, buf);
    }

    public void test_print_append() throws Exception {
        buf.append("EXISTING");
        getFormatter(DAY_OF_MONTH, 1, 2, SignStyle.NEVER).formatTo(LocalDate.of(2012, 1, 3), buf);
        assertEquals(buf.toString(), "EXISTING3");
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="Pad")
    Object[][] provider_pad() {
        return new Object[][] {
            {1, 1, -10, null},
            {1, 1, -9, "9"},
            {1, 1, -1, "1"},
            {1, 1, 0, "0"},
            {1, 1, 3, "3"},
            {1, 1, 9, "9"},
            {1, 1, 10, null},

            {1, 2, -100, null},
            {1, 2, -99, "99"},
            {1, 2, -10, "10"},
            {1, 2, -9, "9"},
            {1, 2, -1, "1"},
            {1, 2, 0, "0"},
            {1, 2, 3, "3"},
            {1, 2, 9, "9"},
            {1, 2, 10, "10"},
            {1, 2, 99, "99"},
            {1, 2, 100, null},

            {2, 2, -100, null},
            {2, 2, -99, "99"},
            {2, 2, -10, "10"},
            {2, 2, -9, "09"},
            {2, 2, -1, "01"},
            {2, 2, 0, "00"},
            {2, 2, 3, "03"},
            {2, 2, 9, "09"},
            {2, 2, 10, "10"},
            {2, 2, 99, "99"},
            {2, 2, 100, null},

            {1, 3, -1000, null},
            {1, 3, -999, "999"},
            {1, 3, -100, "100"},
            {1, 3, -99, "99"},
            {1, 3, -10, "10"},
            {1, 3, -9, "9"},
            {1, 3, -1, "1"},
            {1, 3, 0, "0"},
            {1, 3, 3, "3"},
            {1, 3, 9, "9"},
            {1, 3, 10, "10"},
            {1, 3, 99, "99"},
            {1, 3, 100, "100"},
            {1, 3, 999, "999"},
            {1, 3, 1000, null},

            {2, 3, -1000, null},
            {2, 3, -999, "999"},
            {2, 3, -100, "100"},
            {2, 3, -99, "99"},
            {2, 3, -10, "10"},
            {2, 3, -9, "09"},
            {2, 3, -1, "01"},
            {2, 3, 0, "00"},
            {2, 3, 3, "03"},
            {2, 3, 9, "09"},
            {2, 3, 10, "10"},
            {2, 3, 99, "99"},
            {2, 3, 100, "100"},
            {2, 3, 999, "999"},
            {2, 3, 1000, null},

            {3, 3, -1000, null},
            {3, 3, -999, "999"},
            {3, 3, -100, "100"},
            {3, 3, -99, "099"},
            {3, 3, -10, "010"},
            {3, 3, -9, "009"},
            {3, 3, -1, "001"},
            {3, 3, 0, "000"},
            {3, 3, 3, "003"},
            {3, 3, 9, "009"},
            {3, 3, 10, "010"},
            {3, 3, 99, "099"},
            {3, 3, 100, "100"},
            {3, 3, 999, "999"},
            {3, 3, 1000, null},

            {1, 10, Integer.MAX_VALUE - 1, "2147483646"},
            {1, 10, Integer.MAX_VALUE, "2147483647"},
            {1, 10, Integer.MIN_VALUE + 1, "2147483647"},
            {1, 10, Integer.MIN_VALUE, "2147483648"},
       };
    }

    @Test(dataProvider="Pad")
    public void test_pad_NOT_NEGATIVE(int minPad, int maxPad, long value, String result) throws Exception {
        try {
            getFormatter(DAY_OF_MONTH, minPad, maxPad, SignStyle.NOT_NEGATIVE).formatTo(new MockFieldValue(DAY_OF_MONTH, value), buf);
            if (result == null || value < 0) {
                fail("Expected exception");
            }
            assertEquals(buf.toString(), result);
        } catch (DateTimeException ex) {
            if (result == null || value < 0) {
                assertEquals(ex.getMessage().contains(DAY_OF_MONTH.toString()), true);
            } else {
                throw ex;
            }
        }
    }

    @Test(dataProvider="Pad")
    public void test_pad_NEVER(int minPad, int maxPad, long value, String result) throws Exception {
        try {
            getFormatter(DAY_OF_MONTH, minPad, maxPad, SignStyle.NEVER).formatTo(new MockFieldValue(DAY_OF_MONTH, value), buf);
            if (result == null) {
                fail("Expected exception");
            }
            assertEquals(buf.toString(), result);
        } catch (DateTimeException ex) {
            if (result != null) {
                throw ex;
            }
            assertEquals(ex.getMessage().contains(DAY_OF_MONTH.toString()), true);
        }
    }

    @Test(dataProvider="Pad")
    public void test_pad_NORMAL(int minPad, int maxPad, long value, String result) throws Exception {
        try {
            getFormatter(DAY_OF_MONTH, minPad, maxPad, SignStyle.NORMAL).formatTo(new MockFieldValue(DAY_OF_MONTH, value), buf);
            if (result == null) {
                fail("Expected exception");
            }
            assertEquals(buf.toString(), (value < 0 ? "-" + result : result));
        } catch (DateTimeException ex) {
            if (result != null) {
                throw ex;
            }
            assertEquals(ex.getMessage().contains(DAY_OF_MONTH.toString()), true);
        }
    }

    @Test(dataProvider="Pad")
    public void test_pad_ALWAYS(int minPad, int maxPad, long value, String result) throws Exception {
        try {
            getFormatter(DAY_OF_MONTH, minPad, maxPad, SignStyle.ALWAYS).formatTo(new MockFieldValue(DAY_OF_MONTH, value), buf);
            if (result == null) {
                fail("Expected exception");
            }
            assertEquals(buf.toString(), (value < 0 ? "-" + result : "+" + result));
        } catch (DateTimeException ex) {
            if (result != null) {
                throw ex;
            }
            assertEquals(ex.getMessage().contains(DAY_OF_MONTH.toString()), true);
        }
    }

    @Test(dataProvider="Pad")
    public void test_pad_EXCEEDS_PAD(int minPad, int maxPad, long value, String result) throws Exception {
        try {
            getFormatter(DAY_OF_MONTH, minPad, maxPad, SignStyle.EXCEEDS_PAD).formatTo(new MockFieldValue(DAY_OF_MONTH, value), buf);
            if (result == null) {
                fail("Expected exception");
                return;  // unreachable
            }
            if (result.length() > minPad || value < 0) {
                result = (value < 0 ? "-" + result : "+" + result);
            }
            assertEquals(buf.toString(), result);
        } catch (DateTimeException ex) {
            if (result != null) {
                throw ex;
            }
            assertEquals(ex.getMessage().contains(DAY_OF_MONTH.toString()), true);
        }
    }

    //-----------------------------------------------------------------------
    public void test_toString1() throws Exception {
        assertEquals(getFormatter(HOUR_OF_DAY, 1, 19, SignStyle.NORMAL).toString(), "Value(HourOfDay)");
    }

    public void test_toString2() throws Exception {
        assertEquals(getFormatter(HOUR_OF_DAY, 2, 2, SignStyle.NOT_NEGATIVE).toString(), "Value(HourOfDay,2)");
    }

    public void test_toString3() throws Exception {
        assertEquals(getFormatter(HOUR_OF_DAY, 1, 2, SignStyle.NOT_NEGATIVE).toString(), "Value(HourOfDay,1,2,NOT_NEGATIVE)");
    }

}
