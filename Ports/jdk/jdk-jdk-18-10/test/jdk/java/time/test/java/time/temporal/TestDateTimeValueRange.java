/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
package test.java.time.temporal;

import static org.testng.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.DateTimeException;
import java.time.temporal.ChronoField;
import java.time.temporal.ValueRange;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import test.java.time.AbstractTest;

/**
 * Test.
 * @bug 8239520
 */
@Test
public class TestDateTimeValueRange extends AbstractTest {

    //-----------------------------------------------------------------------
    // Basics
    //-----------------------------------------------------------------------
    @Test
    public void test_immutable() {
        assertImmutable(ValueRange.class);
    }

    //-----------------------------------------------------------------------
    // of(long,long)
    //-----------------------------------------------------------------------
    public void test_of_longlong() {
        ValueRange test = ValueRange.of(1, 12);
        assertEquals(test.getMinimum(), 1);
        assertEquals(test.getLargestMinimum(), 1);
        assertEquals(test.getSmallestMaximum(), 12);
        assertEquals(test.getMaximum(), 12);
        assertEquals(test.isFixed(), true);
        assertEquals(test.isIntValue(), true);
    }

    public void test_of_longlong_big() {
        ValueRange test = ValueRange.of(1, 123456789012345L);
        assertEquals(test.getMinimum(), 1);
        assertEquals(test.getLargestMinimum(), 1);
        assertEquals(test.getSmallestMaximum(), 123456789012345L);
        assertEquals(test.getMaximum(), 123456789012345L);
        assertEquals(test.isFixed(), true);
        assertEquals(test.isIntValue(), false);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_of_longlong_minGtMax() {
        ValueRange.of(12, 1);
    }

    //-----------------------------------------------------------------------
    // of(long,long,long)
    //-----------------------------------------------------------------------
    public void test_of_longlonglong() {
        ValueRange test = ValueRange.of(1, 28, 31);
        assertEquals(test.getMinimum(), 1);
        assertEquals(test.getLargestMinimum(), 1);
        assertEquals(test.getSmallestMaximum(), 28);
        assertEquals(test.getMaximum(), 31);
        assertEquals(test.isFixed(), false);
        assertEquals(test.isIntValue(), true);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_of_longlonglong_minGtMax() {
        ValueRange.of(12, 1, 2);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_of_longlonglong_smallestmaxminGtMax() {
        ValueRange.of(1, 31, 28);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test_of_longlonglong_minGtSmallestMax() {
        ValueRange.of(5, 2, 10);
    }

    //-----------------------------------------------------------------------
    // of(long,long,long,long)
    //-----------------------------------------------------------------------
    @DataProvider(name="valid")
    Object[][] data_valid() {
        return new Object[][] {
                {1, 1, 1, 1},
                {1, 1, 1, 2},
                {1, 1, 2, 2},
                {1, 2, 3, 4},
                {1, 1, 28, 31},
                {1, 3, 31, 31},
                {-5, -4, -3, -2},
                {-5, -4, 3, 4},
                {1, 20, 10, 31},
        };
    }

    @Test(dataProvider="valid")
    public void test_of_longlonglonglong(long sMin, long lMin, long sMax, long lMax) {
        ValueRange test = ValueRange.of(sMin, lMin, sMax, lMax);
        assertEquals(test.getMinimum(), sMin);
        assertEquals(test.getLargestMinimum(), lMin);
        assertEquals(test.getSmallestMaximum(), sMax);
        assertEquals(test.getMaximum(), lMax);
        assertEquals(test.isFixed(), sMin == lMin && sMax == lMax);
        assertEquals(test.isIntValue(), true);
    }

    @DataProvider(name="invalid")
    Object[][] data_invalid() {
        return new Object[][] {
                {1, 2, 31, 28},
                {1, 31, 2, 28},
                {31, 2, 1, 28},
                {31, 2, 3, 28},

                {2, 1, 28, 31},
                {2, 1, 31, 28},
                {12, 13, 1, 2},

                {10, 11, 0, 12}, // smallest minimum is greater than the smallest maximum
                {0, 1, 11, 10}, // smallest maximum is greater than the largest maximum
                {0, 11, 1, 10}, // largest minimum is greater than the largest maximum
                {1, 0, 10, 11}, // smallest minimum is greater than the largest minimum
        };
    }

    @Test(dataProvider="invalid", expectedExceptions=IllegalArgumentException.class)
    public void test_of_longlonglonglong_invalid(long sMin, long lMin, long sMax, long lMax) {
        ValueRange.of(sMin, lMin, sMax, lMax);
    }

    //-----------------------------------------------------------------------
    // isValidValue(long)
    //-----------------------------------------------------------------------
    public void test_isValidValue_long() {
        ValueRange test = ValueRange.of(1, 28, 31);
        assertEquals(test.isValidValue(0), false);
        assertEquals(test.isValidValue(1), true);
        assertEquals(test.isValidValue(2), true);
        assertEquals(test.isValidValue(30), true);
        assertEquals(test.isValidValue(31), true);
        assertEquals(test.isValidValue(32), false);
    }

    //-----------------------------------------------------------------------
    // isValidIntValue(long)
    //-----------------------------------------------------------------------
    public void test_isValidValue_long_int() {
        ValueRange test = ValueRange.of(1, 28, 31);
        assertEquals(test.isValidValue(0), false);
        assertEquals(test.isValidValue(1), true);
        assertEquals(test.isValidValue(31), true);
        assertEquals(test.isValidValue(32), false);
    }

    public void test_isValidValue_long_long() {
        ValueRange test = ValueRange.of(1, 28, Integer.MAX_VALUE + 1L);
        assertEquals(test.isValidIntValue(0), false);
        assertEquals(test.isValidIntValue(1), false);
        assertEquals(test.isValidIntValue(31), false);
        assertEquals(test.isValidIntValue(32), false);
    }

    //-----------------------------------------------------------------------
    // checkValidValue
    //-----------------------------------------------------------------------
    @Test(dataProvider="valid")
    public void test_of_checkValidValue(long sMin, long lMin, long sMax, long lMax) {
        ValueRange test = ValueRange.of(sMin, lMin, sMax, lMax);
        assertEquals(test.checkValidIntValue(sMin, null), sMin);
        assertEquals(test.checkValidIntValue(lMin, null), lMin);
        assertEquals(test.checkValidIntValue(sMax, null), sMax);
        assertEquals(test.checkValidIntValue(lMax, null), lMax);
    }

    @Test(dataProvider="valid", expectedExceptions = DateTimeException.class)
    public void test_of_checkValidValueMinException(long sMin, long lMin, long sMax, long lMax) {
        ValueRange test = ValueRange.of(sMin, lMin, sMax, lMax);
        test.checkValidIntValue(sMin-1, null);
    }

    @Test(dataProvider="valid", expectedExceptions = DateTimeException.class)
    public void test_of_checkValidValueMaxException(long sMin, long lMin, long sMax, long lMax) {
        ValueRange test = ValueRange.of(sMin, lMin, sMax, lMax);
        test.checkValidIntValue(lMax+1, null);
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void test_checkValidValueUnsupported_long_long() {
        ValueRange test = ValueRange.of(1, 28, Integer.MAX_VALUE + 1L);
        test.checkValidIntValue(0, (ChronoField)null);
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void test_checkValidValueInvalid_long_long() {
        ValueRange test = ValueRange.of(1, 28, Integer.MAX_VALUE + 1L);
        test.checkValidIntValue(Integer.MAX_VALUE + 2L, (ChronoField)null);
    }

    //-----------------------------------------------------------------------
    // equals() / hashCode()
    //-----------------------------------------------------------------------
    public void test_equals1() {
        ValueRange a = ValueRange.of(1, 2, 3, 4);
        ValueRange b = ValueRange.of(1, 2, 3, 4);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), true);
        assertEquals(b.equals(a), true);
        assertEquals(b.equals(b), true);
        assertEquals(a.hashCode() == b.hashCode(), true);
    }

    public void test_equals2() {
        ValueRange a = ValueRange.of(1, 2, 3, 4);
        assertEquals(a.equals(ValueRange.of(0, 2, 3, 4)), false);
        assertEquals(a.equals(ValueRange.of(1, 3, 3, 4)), false);
        assertEquals(a.equals(ValueRange.of(1, 2, 4, 4)), false);
        assertEquals(a.equals(ValueRange.of(1, 2, 3, 5)), false);
    }

    public void test_equals_otherType() {
        ValueRange a = ValueRange.of(1, 12);
        assertEquals(a.equals("Rubbish"), false);
    }

    public void test_equals_null() {
        ValueRange a = ValueRange.of(1, 12);
        assertEquals(a.equals(null), false);
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    public void test_toString() {
        assertEquals(ValueRange.of(1, 1, 4, 4).toString(), "1 - 4");
        assertEquals(ValueRange.of(1, 1, 3, 4).toString(), "1 - 3/4");
        assertEquals(ValueRange.of(1, 2, 3, 4).toString(), "1/2 - 3/4");
        assertEquals(ValueRange.of(1, 2, 4, 4).toString(), "1/2 - 4");
    }

}
