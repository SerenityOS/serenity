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
 * Copyright (c) 2012, Stephen Colebourne & Michael Nascimento Santos
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

import static java.time.Month.AUGUST;
import static java.time.Month.FEBRUARY;
import static java.time.Month.JULY;
import static java.time.Month.JUNE;
import static java.time.Month.MARCH;
import static java.time.Month.OCTOBER;
import static java.time.Month.SEPTEMBER;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.WEEKS;
import static java.time.temporal.ChronoUnit.YEARS;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneOffset;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class TestChronoUnit {

    //-----------------------------------------------------------------------
    @DataProvider(name = "yearsBetween")
    Object[][] data_yearsBetween() {
        return new Object[][] {
            {date(1939, SEPTEMBER, 2), date(1939, SEPTEMBER, 1), 0},
            {date(1939, SEPTEMBER, 2), date(1939, SEPTEMBER, 2), 0},
            {date(1939, SEPTEMBER, 2), date(1939, SEPTEMBER, 3), 0},

            {date(1939, SEPTEMBER, 2), date(1940, SEPTEMBER, 1), 0},
            {date(1939, SEPTEMBER, 2), date(1940, SEPTEMBER, 2), 1},
            {date(1939, SEPTEMBER, 2), date(1940, SEPTEMBER, 3), 1},

            {date(1939, SEPTEMBER, 2), date(1938, SEPTEMBER, 1), -1},
            {date(1939, SEPTEMBER, 2), date(1938, SEPTEMBER, 2), -1},
            {date(1939, SEPTEMBER, 2), date(1938, SEPTEMBER, 3), 0},

            {date(1939, SEPTEMBER, 2), date(1945, SEPTEMBER, 3), 6},
            {date(1939, SEPTEMBER, 2), date(1945, OCTOBER, 3), 6},
            {date(1939, SEPTEMBER, 2), date(1945, AUGUST, 3), 5},
        };
    }

    @Test(dataProvider = "yearsBetween")
    public void test_yearsBetween(LocalDate start, LocalDate end, long expected) {
        assertEquals(YEARS.between(start, end), expected);
    }

    @Test(dataProvider = "yearsBetween")
    public void test_yearsBetweenReversed(LocalDate start, LocalDate end, long expected) {
        assertEquals(YEARS.between(end, start), -expected);
    }

    @Test(dataProvider = "yearsBetween")
    public void test_yearsBetween_LocalDateTimeSameTime(LocalDate start, LocalDate end, long expected) {
        assertEquals(YEARS.between(start.atTime(12, 30), end.atTime(12, 30)), expected);
    }

    @Test(dataProvider = "yearsBetween")
    public void test_yearsBetween_LocalDateTimeLaterTime(LocalDate start, LocalDate end, long expected) {
        if (expected >= 0) {
            assertEquals(YEARS.between(start.atTime(12, 30), end.atTime(12, 31)), expected);
        } else {
            assertEquals(YEARS.between(start.atTime(12, 30), end.atTime(12, 29)), expected);
        }
    }

    @Test(dataProvider = "yearsBetween")
    public void test_yearsBetween_ZonedDateSameOffset(LocalDate start, LocalDate end, long expected) {
        assertEquals(YEARS.between(start.atStartOfDay(ZoneOffset.ofHours(2)), end.atStartOfDay(ZoneOffset.ofHours(2))), expected);
    }

    @Test(dataProvider = "yearsBetween")
    public void test_yearsBetween_ZonedDateLaterOffset(LocalDate start, LocalDate end, long expected) {
        // +01:00 is later than +02:00
        if (expected >= 0) {
            assertEquals(YEARS.between(start.atStartOfDay(ZoneOffset.ofHours(2)), end.atStartOfDay(ZoneOffset.ofHours(1))), expected);
        } else {
            assertEquals(YEARS.between(start.atStartOfDay(ZoneOffset.ofHours(1)), end.atStartOfDay(ZoneOffset.ofHours(2))), expected);
        }
    }

    //-----------------------------------------------------------------------
    @DataProvider(name = "monthsBetween")
    Object[][] data_monthsBetween() {
        return new Object[][] {
            {date(2012, JULY, 2), date(2012, JULY, 1), 0},
            {date(2012, JULY, 2), date(2012, JULY, 2), 0},
            {date(2012, JULY, 2), date(2012, JULY, 3), 0},

            {date(2012, JULY, 2), date(2012, AUGUST, 1), 0},
            {date(2012, JULY, 2), date(2012, AUGUST, 2), 1},
            {date(2012, JULY, 2), date(2012, AUGUST, 3), 1},

            {date(2012, JULY, 2), date(2012, SEPTEMBER, 1), 1},
            {date(2012, JULY, 2), date(2012, SEPTEMBER, 2), 2},
            {date(2012, JULY, 2), date(2012, SEPTEMBER, 3), 2},

            {date(2012, JULY, 2), date(2012, JUNE, 1), -1},
            {date(2012, JULY, 2), date(2012, JUNE, 2), -1},
            {date(2012, JULY, 2), date(2012, JUNE, 3), 0},

            {date(2012, FEBRUARY, 27), date(2012, MARCH, 26), 0},
            {date(2012, FEBRUARY, 27), date(2012, MARCH, 27), 1},
            {date(2012, FEBRUARY, 27), date(2012, MARCH, 28), 1},

            {date(2012, FEBRUARY, 28), date(2012, MARCH, 27), 0},
            {date(2012, FEBRUARY, 28), date(2012, MARCH, 28), 1},
            {date(2012, FEBRUARY, 28), date(2012, MARCH, 29), 1},

            {date(2012, FEBRUARY, 29), date(2012, MARCH, 28), 0},
            {date(2012, FEBRUARY, 29), date(2012, MARCH, 29), 1},
            {date(2012, FEBRUARY, 29), date(2012, MARCH, 30), 1},
        };
    }

    @Test(dataProvider = "monthsBetween")
    public void test_monthsBetween(LocalDate start, LocalDate end, long expected) {
        assertEquals(MONTHS.between(start, end), expected);
    }

    @Test(dataProvider = "monthsBetween")
    public void test_monthsBetweenReversed(LocalDate start, LocalDate end, long expected) {
        assertEquals(MONTHS.between(end, start), -expected);
    }

    @Test(dataProvider = "monthsBetween")
    public void test_monthsBetween_LocalDateTimeSameTime(LocalDate start, LocalDate end, long expected) {
        assertEquals(MONTHS.between(start.atTime(12, 30), end.atTime(12, 30)), expected);
    }

    @Test(dataProvider = "monthsBetween")
    public void test_monthsBetween_LocalDateTimeLaterTime(LocalDate start, LocalDate end, long expected) {
        if (expected >= 0) {
            assertEquals(MONTHS.between(start.atTime(12, 30), end.atTime(12, 31)), expected);
        } else {
            assertEquals(MONTHS.between(start.atTime(12, 30), end.atTime(12, 29)), expected);
        }
    }

    @Test(dataProvider = "monthsBetween")
    public void test_monthsBetween_ZonedDateSameOffset(LocalDate start, LocalDate end, long expected) {
        assertEquals(MONTHS.between(start.atStartOfDay(ZoneOffset.ofHours(2)), end.atStartOfDay(ZoneOffset.ofHours(2))), expected);
    }

    @Test(dataProvider = "monthsBetween")
    public void test_monthsBetween_ZonedDateLaterOffset(LocalDate start, LocalDate end, long expected) {
        // +01:00 is later than +02:00
        if (expected >= 0) {
            assertEquals(MONTHS.between(start.atStartOfDay(ZoneOffset.ofHours(2)), end.atStartOfDay(ZoneOffset.ofHours(1))), expected);
        } else {
            assertEquals(MONTHS.between(start.atStartOfDay(ZoneOffset.ofHours(1)), end.atStartOfDay(ZoneOffset.ofHours(2))), expected);
        }
    }

    //-----------------------------------------------------------------------
    @DataProvider(name = "weeksBetween")
    Object[][] data_weeksBetween() {
        return new Object[][] {
            {date(2012, JULY, 2), date(2012, JUNE, 25), -1},
            {date(2012, JULY, 2), date(2012, JUNE, 26), 0},
            {date(2012, JULY, 2), date(2012, JULY, 2), 0},
            {date(2012, JULY, 2), date(2012, JULY, 8), 0},
            {date(2012, JULY, 2), date(2012, JULY, 9), 1},

            {date(2012, FEBRUARY, 28), date(2012, FEBRUARY, 21), -1},
            {date(2012, FEBRUARY, 28), date(2012, FEBRUARY, 22), 0},
            {date(2012, FEBRUARY, 28), date(2012, FEBRUARY, 28), 0},
            {date(2012, FEBRUARY, 28), date(2012, FEBRUARY, 29), 0},
            {date(2012, FEBRUARY, 28), date(2012, MARCH, 1), 0},
            {date(2012, FEBRUARY, 28), date(2012, MARCH, 5), 0},
            {date(2012, FEBRUARY, 28), date(2012, MARCH, 6), 1},

            {date(2012, FEBRUARY, 29), date(2012, FEBRUARY, 22), -1},
            {date(2012, FEBRUARY, 29), date(2012, FEBRUARY, 23), 0},
            {date(2012, FEBRUARY, 29), date(2012, FEBRUARY, 28), 0},
            {date(2012, FEBRUARY, 29), date(2012, FEBRUARY, 29), 0},
            {date(2012, FEBRUARY, 29), date(2012, MARCH, 1), 0},
            {date(2012, FEBRUARY, 29), date(2012, MARCH, 6), 0},
            {date(2012, FEBRUARY, 29), date(2012, MARCH, 7), 1},
        };
    }

    @Test(dataProvider = "weeksBetween")
    public void test_weeksBetween(LocalDate start, LocalDate end, long expected) {
        assertEquals(WEEKS.between(start, end), expected);
    }

    @Test(dataProvider = "weeksBetween")
    public void test_weeksBetweenReversed(LocalDate start, LocalDate end, long expected) {
        assertEquals(WEEKS.between(end, start), -expected);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name = "daysBetween")
    Object[][] data_daysBetween() {
        return new Object[][] {
            {date(2012, JULY, 2), date(2012, JULY, 1), -1},
            {date(2012, JULY, 2), date(2012, JULY, 2), 0},
            {date(2012, JULY, 2), date(2012, JULY, 3), 1},

            {date(2012, FEBRUARY, 28), date(2012, FEBRUARY, 27), -1},
            {date(2012, FEBRUARY, 28), date(2012, FEBRUARY, 28), 0},
            {date(2012, FEBRUARY, 28), date(2012, FEBRUARY, 29), 1},
            {date(2012, FEBRUARY, 28), date(2012, MARCH, 1), 2},

            {date(2012, FEBRUARY, 29), date(2012, FEBRUARY, 27), -2},
            {date(2012, FEBRUARY, 29), date(2012, FEBRUARY, 28), -1},
            {date(2012, FEBRUARY, 29), date(2012, FEBRUARY, 29), 0},
            {date(2012, FEBRUARY, 29), date(2012, MARCH, 1), 1},

            {date(2012, MARCH, 1), date(2012, FEBRUARY, 27), -3},
            {date(2012, MARCH, 1), date(2012, FEBRUARY, 28), -2},
            {date(2012, MARCH, 1), date(2012, FEBRUARY, 29), -1},
            {date(2012, MARCH, 1), date(2012, MARCH, 1), 0},
            {date(2012, MARCH, 1), date(2012, MARCH, 2), 1},

            {date(2012, MARCH, 1), date(2013, FEBRUARY, 28), 364},
            {date(2012, MARCH, 1), date(2013, MARCH, 1), 365},

            {date(2011, MARCH, 1), date(2012, FEBRUARY, 28), 364},
            {date(2011, MARCH, 1), date(2012, FEBRUARY, 29), 365},
            {date(2011, MARCH, 1), date(2012, MARCH, 1), 366},
        };
    }

    @Test(dataProvider = "daysBetween")
    public void test_daysBetween(LocalDate start, LocalDate end, long expected) {
        assertEquals(DAYS.between(start, end), expected);
    }

    @Test(dataProvider = "daysBetween")
    public void test_daysBetweenReversed(LocalDate start, LocalDate end, long expected) {
        assertEquals(DAYS.between(end, start), -expected);
    }

    @Test(dataProvider = "daysBetween")
    public void test_daysBetween_LocalDateTimeSameTime(LocalDate start, LocalDate end, long expected) {
        assertEquals(DAYS.between(start.atTime(12, 30), end.atTime(12, 30)), expected);
    }

    @Test(dataProvider = "daysBetween")
    public void test_daysBetween_LocalDateTimeLaterTime(LocalDate start, LocalDate end, long expected) {
        if (expected >= 0) {
            assertEquals(DAYS.between(start.atTime(12, 30), end.atTime(12, 31)), expected);
        } else {
            assertEquals(DAYS.between(start.atTime(12, 30), end.atTime(12, 29)), expected);
        }
    }

    @Test(dataProvider = "daysBetween")
    public void test_daysBetween_ZonedDateSameOffset(LocalDate start, LocalDate end, long expected) {
        assertEquals(DAYS.between(start.atStartOfDay(ZoneOffset.ofHours(2)), end.atStartOfDay(ZoneOffset.ofHours(2))), expected);
    }

    @Test(dataProvider = "daysBetween")
    public void test_daysBetween_ZonedDateLaterOffset(LocalDate start, LocalDate end, long expected) {
        // +01:00 is later than +02:00
        if (expected >= 0) {
            assertEquals(DAYS.between(start.atStartOfDay(ZoneOffset.ofHours(2)), end.atStartOfDay(ZoneOffset.ofHours(1))), expected);
        } else {
            assertEquals(DAYS.between(start.atStartOfDay(ZoneOffset.ofHours(1)), end.atStartOfDay(ZoneOffset.ofHours(2))), expected);
        }
    }

    //-----------------------------------------------------------------------
    private static LocalDate date(int year, Month month, int dom) {
        return LocalDate.of(year, month, dom);
    }

}
