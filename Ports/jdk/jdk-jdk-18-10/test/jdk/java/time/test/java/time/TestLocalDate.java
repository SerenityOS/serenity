/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright (c) 2007-2012, Stephen Colebourne & Michael Nascimento Santos
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
package test.java.time;

import static java.time.temporal.ChronoField.YEAR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test LocalDate.
 */
@Test
public class TestLocalDate extends AbstractTest {

    private LocalDate TEST_2007_07_15;

    @BeforeMethod
    public void setUp() {
        TEST_2007_07_15 = LocalDate.of(2007, 7, 15);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_immutable() {
        assertImmutable(LocalDate.class);
    }

    //-----------------------------------------------------------------------
    // Since plusDays/minusDays actually depends on MJDays, it cannot be used for testing
    private LocalDate next(LocalDate date) {
        int newDayOfMonth = date.getDayOfMonth() + 1;
        if (newDayOfMonth <= date.getMonth().length(isIsoLeap(date.getYear()))) {
            return date.withDayOfMonth(newDayOfMonth);
        }
        date = date.withDayOfMonth(1);
        if (date.getMonth() == Month.DECEMBER) {
            date = date.withYear(date.getYear() + 1);
        }
        return date.with(date.getMonth().plus(1));
    }

    private LocalDate previous(LocalDate date) {
        int newDayOfMonth = date.getDayOfMonth() - 1;
        if (newDayOfMonth > 0) {
            return date.withDayOfMonth(newDayOfMonth);
        }
        date = date.with(date.getMonth().minus(1));
        if (date.getMonth() == Month.DECEMBER) {
            date = date.withYear(date.getYear() - 1);
        }
        return date.withDayOfMonth(date.getMonth().length(isIsoLeap(date.getYear())));
    }

    @Test
    public void test_with_DateTimeField_long_noChange_same() {
        LocalDate t = TEST_2007_07_15.with(YEAR, 2007);
        assertSame(t, TEST_2007_07_15);
    }

    @Test
    public void test_withYear_int_noChange_same() {
        LocalDate t = TEST_2007_07_15.withYear(2007);
        assertSame(t, TEST_2007_07_15);
    }

    @Test
    public void test_withMonth_int_noChange_same() {
        LocalDate t = TEST_2007_07_15.withMonth(7);
        assertSame(t, TEST_2007_07_15);
    }

    @Test
    public void test_withDayOfMonth_noChange_same() {
        LocalDate t = TEST_2007_07_15.withDayOfMonth(15);
        assertSame(t, TEST_2007_07_15);
    }

    @Test
    public void test_withDayOfYear_noChange_same() {
        LocalDate t = TEST_2007_07_15.withDayOfYear(31 + 28 + 31 + 30 + 31 + 30 + 15);
        assertSame(t, TEST_2007_07_15);
    }

    @Test
    public void test_plus_Period_zero() {
        LocalDate t = TEST_2007_07_15.plus(MockSimplePeriod.ZERO_DAYS);
        assertSame(t, TEST_2007_07_15);
    }

    @Test
    public void test_plus_longPeriodUnit_zero() {
        LocalDate t = TEST_2007_07_15.plus(0, ChronoUnit.DAYS);
        assertSame(t, TEST_2007_07_15);
    }

    @Test
    public void test_plusYears_long_noChange_same() {
        LocalDate t = TEST_2007_07_15.plusYears(0);
        assertSame(t, TEST_2007_07_15);
    }

    @Test
    public void test_plusMonths_long_noChange_same() {
        LocalDate t = TEST_2007_07_15.plusMonths(0);
        assertSame(t, TEST_2007_07_15);
    }

    //-----------------------------------------------------------------------
    // plusWeeks()
    //-----------------------------------------------------------------------
    @DataProvider(name="samplePlusWeeksSymmetry")
    Object[][] provider_samplePlusWeeksSymmetry() {
        return new Object[][] {
            {LocalDate.of(-1, 1, 1)},
            {LocalDate.of(-1, 2, 28)},
            {LocalDate.of(-1, 3, 1)},
            {LocalDate.of(-1, 12, 31)},
            {LocalDate.of(0, 1, 1)},
            {LocalDate.of(0, 2, 28)},
            {LocalDate.of(0, 2, 29)},
            {LocalDate.of(0, 3, 1)},
            {LocalDate.of(0, 12, 31)},
            {LocalDate.of(2007, 1, 1)},
            {LocalDate.of(2007, 2, 28)},
            {LocalDate.of(2007, 3, 1)},
            {LocalDate.of(2007, 12, 31)},
            {LocalDate.of(2008, 1, 1)},
            {LocalDate.of(2008, 2, 28)},
            {LocalDate.of(2008, 2, 29)},
            {LocalDate.of(2008, 3, 1)},
            {LocalDate.of(2008, 12, 31)},
            {LocalDate.of(2099, 1, 1)},
            {LocalDate.of(2099, 2, 28)},
            {LocalDate.of(2099, 3, 1)},
            {LocalDate.of(2099, 12, 31)},
            {LocalDate.of(2100, 1, 1)},
            {LocalDate.of(2100, 2, 28)},
            {LocalDate.of(2100, 3, 1)},
            {LocalDate.of(2100, 12, 31)},
        };
    }

    @Test(dataProvider="samplePlusWeeksSymmetry")
    public void test_plusWeeks_symmetry(LocalDate reference) {
        for (int weeks = 0; weeks < 365 * 8; weeks++) {
            LocalDate t = reference.plusWeeks(weeks).plusWeeks(-weeks);
            assertEquals(t, reference);

            t = reference.plusWeeks(-weeks).plusWeeks(weeks);
            assertEquals(t, reference);
        }
    }

    @Test
    public void test_plusWeeks_noChange_same() {
        LocalDate t = TEST_2007_07_15.plusWeeks(0);
        assertSame(t, TEST_2007_07_15);
    }

    //-----------------------------------------------------------------------
    // plusDays()
    //-----------------------------------------------------------------------
    @DataProvider(name="samplePlusDaysSymmetry")
    Object[][] provider_samplePlusDaysSymmetry() {
        return new Object[][] {
            {LocalDate.of(-1, 1, 1)},
            {LocalDate.of(-1, 2, 28)},
            {LocalDate.of(-1, 3, 1)},
            {LocalDate.of(-1, 12, 31)},
            {LocalDate.of(0, 1, 1)},
            {LocalDate.of(0, 2, 28)},
            {LocalDate.of(0, 2, 29)},
            {LocalDate.of(0, 3, 1)},
            {LocalDate.of(0, 12, 31)},
            {LocalDate.of(2007, 1, 1)},
            {LocalDate.of(2007, 2, 28)},
            {LocalDate.of(2007, 3, 1)},
            {LocalDate.of(2007, 12, 31)},
            {LocalDate.of(2008, 1, 1)},
            {LocalDate.of(2008, 2, 28)},
            {LocalDate.of(2008, 2, 29)},
            {LocalDate.of(2008, 3, 1)},
            {LocalDate.of(2008, 12, 31)},
            {LocalDate.of(2099, 1, 1)},
            {LocalDate.of(2099, 2, 28)},
            {LocalDate.of(2099, 3, 1)},
            {LocalDate.of(2099, 12, 31)},
            {LocalDate.of(2100, 1, 1)},
            {LocalDate.of(2100, 2, 28)},
            {LocalDate.of(2100, 3, 1)},
            {LocalDate.of(2100, 12, 31)},
        };
    }

    @Test(dataProvider="samplePlusDaysSymmetry")
    public void test_plusDays_symmetry(LocalDate reference) {
        for (int days = 0; days < 365 * 8; days++) {
            LocalDate t = reference.plusDays(days).plusDays(-days);
            assertEquals(t, reference);

            t = reference.plusDays(-days).plusDays(days);
            assertEquals(t, reference);
        }
    }

    @Test
    public void test_plusDays_noChange_same() {
        LocalDate t = TEST_2007_07_15.plusDays(0);
        assertSame(t, TEST_2007_07_15);
    }

    @Test
    public void test_minus_Period_zero() {
        LocalDate t = TEST_2007_07_15.minus(MockSimplePeriod.ZERO_DAYS);
        assertSame(t, TEST_2007_07_15);
    }

    @Test
    public void test_minus_longPeriodUnit_zero() {
        LocalDate t = TEST_2007_07_15.minus(0, ChronoUnit.DAYS);
        assertSame(t, TEST_2007_07_15);
    }

    @Test
    public void test_minusYears_long_noChange_same() {
        LocalDate t = TEST_2007_07_15.minusYears(0);
        assertSame(t, TEST_2007_07_15);
    }

    @Test
    public void test_minusMonths_long_noChange_same() {
        LocalDate t = TEST_2007_07_15.minusMonths(0);
        assertSame(t, TEST_2007_07_15);
    }

    //-----------------------------------------------------------------------
    // minusWeeks()
    //-----------------------------------------------------------------------
    @DataProvider(name="sampleMinusWeeksSymmetry")
    Object[][] provider_sampleMinusWeeksSymmetry() {
        return new Object[][] {
            {LocalDate.of(-1, 1, 1)},
            {LocalDate.of(-1, 2, 28)},
            {LocalDate.of(-1, 3, 1)},
            {LocalDate.of(-1, 12, 31)},
            {LocalDate.of(0, 1, 1)},
            {LocalDate.of(0, 2, 28)},
            {LocalDate.of(0, 2, 29)},
            {LocalDate.of(0, 3, 1)},
            {LocalDate.of(0, 12, 31)},
            {LocalDate.of(2007, 1, 1)},
            {LocalDate.of(2007, 2, 28)},
            {LocalDate.of(2007, 3, 1)},
            {LocalDate.of(2007, 12, 31)},
            {LocalDate.of(2008, 1, 1)},
            {LocalDate.of(2008, 2, 28)},
            {LocalDate.of(2008, 2, 29)},
            {LocalDate.of(2008, 3, 1)},
            {LocalDate.of(2008, 12, 31)},
            {LocalDate.of(2099, 1, 1)},
            {LocalDate.of(2099, 2, 28)},
            {LocalDate.of(2099, 3, 1)},
            {LocalDate.of(2099, 12, 31)},
            {LocalDate.of(2100, 1, 1)},
            {LocalDate.of(2100, 2, 28)},
            {LocalDate.of(2100, 3, 1)},
            {LocalDate.of(2100, 12, 31)},
        };
    }

    @Test(dataProvider="sampleMinusWeeksSymmetry")
    public void test_minusWeeks_symmetry(LocalDate reference) {
        for (int weeks = 0; weeks < 365 * 8; weeks++) {
            LocalDate t = reference.minusWeeks(weeks).minusWeeks(-weeks);
            assertEquals(t, reference);

            t = reference.minusWeeks(-weeks).minusWeeks(weeks);
            assertEquals(t, reference);
        }
    }

    @Test
    public void test_minusWeeks_noChange_same() {
        LocalDate t = TEST_2007_07_15.minusWeeks(0);
        assertSame(t, TEST_2007_07_15);
    }

    //-----------------------------------------------------------------------
    // minusDays()
    //-----------------------------------------------------------------------
    @DataProvider(name="sampleMinusDaysSymmetry")
    Object[][] provider_sampleMinusDaysSymmetry() {
        return new Object[][] {
            {LocalDate.of(-1, 1, 1)},
            {LocalDate.of(-1, 2, 28)},
            {LocalDate.of(-1, 3, 1)},
            {LocalDate.of(-1, 12, 31)},
            {LocalDate.of(0, 1, 1)},
            {LocalDate.of(0, 2, 28)},
            {LocalDate.of(0, 2, 29)},
            {LocalDate.of(0, 3, 1)},
            {LocalDate.of(0, 12, 31)},
            {LocalDate.of(2007, 1, 1)},
            {LocalDate.of(2007, 2, 28)},
            {LocalDate.of(2007, 3, 1)},
            {LocalDate.of(2007, 12, 31)},
            {LocalDate.of(2008, 1, 1)},
            {LocalDate.of(2008, 2, 28)},
            {LocalDate.of(2008, 2, 29)},
            {LocalDate.of(2008, 3, 1)},
            {LocalDate.of(2008, 12, 31)},
            {LocalDate.of(2099, 1, 1)},
            {LocalDate.of(2099, 2, 28)},
            {LocalDate.of(2099, 3, 1)},
            {LocalDate.of(2099, 12, 31)},
            {LocalDate.of(2100, 1, 1)},
            {LocalDate.of(2100, 2, 28)},
            {LocalDate.of(2100, 3, 1)},
            {LocalDate.of(2100, 12, 31)},
        };
    }

    @Test(dataProvider="sampleMinusDaysSymmetry")
    public void test_minusDays_symmetry(LocalDate reference) {
        for (int days = 0; days < 365 * 8; days++) {
            LocalDate t = reference.minusDays(days).minusDays(-days);
            assertEquals(t, reference);

            t = reference.minusDays(-days).minusDays(days);
            assertEquals(t, reference);
        }
    }

    @Test
    public void test_minusDays_noChange_same() {
        LocalDate t = TEST_2007_07_15.minusDays(0);
        assertSame(t, TEST_2007_07_15);
    }

    @Test
    public void test_toEpochDay_fromMJDays_symmetry() {
        long date_0000_01_01 = -678941 - 40587;

        LocalDate test = LocalDate.of(0, 1, 1);
        for (long i = date_0000_01_01; i < 700000; i++) {
            assertEquals(LocalDate.ofEpochDay(test.toEpochDay()), test);
            test = next(test);
        }
        test = LocalDate.of(0, 1, 1);
        for (long i = date_0000_01_01; i > -2000000; i--) {
            assertEquals(LocalDate.ofEpochDay(test.toEpochDay()), test);
            test = previous(test);
        }
    }

    void doTest_comparisons_LocalDate(LocalDate... localDates) {
        for (int i = 0; i < localDates.length; i++) {
            LocalDate a = localDates[i];
            for (int j = 0; j < localDates.length; j++) {
                LocalDate b = localDates[j];
                if (i < j) {
                    assertTrue(a.compareTo(b) < 0, a + " <=> " + b);
                    assertEquals(a.isBefore(b), true, a + " <=> " + b);
                    assertEquals(a.isAfter(b), false, a + " <=> " + b);
                    assertEquals(a.equals(b), false, a + " <=> " + b);
                } else if (i > j) {
                    assertTrue(a.compareTo(b) > 0, a + " <=> " + b);
                    assertEquals(a.isBefore(b), false, a + " <=> " + b);
                    assertEquals(a.isAfter(b), true, a + " <=> " + b);
                    assertEquals(a.equals(b), false, a + " <=> " + b);
                } else {
                    assertEquals(a.compareTo(b), 0, a + " <=> " + b);
                    assertEquals(a.isBefore(b), false, a + " <=> " + b);
                    assertEquals(a.isAfter(b), false, a + " <=> " + b);
                    assertEquals(a.equals(b), true, a + " <=> " + b);
                }
            }
        }
    }

    @DataProvider(name="quarterYearsToAdd")
    Object[][] provider_quarterYearsToAdd() {
        return new Object[][] {
            {Long.valueOf(-1000000000)},
            {Long.valueOf(-256)},
            {Long.valueOf(-255)},
            {Long.valueOf(-1)},
            {Long.valueOf(0)},
            {Long.valueOf(1)},
            {Long.valueOf(255)},
            {Long.valueOf(256)},
            {Long.valueOf(1000000000)},
        };
    }

    @Test(dataProvider="quarterYearsToAdd")
    public void test_plus_QuarterYears(long quarterYears) {
        LocalDate t0 = TEST_2007_07_15
                .plus(quarterYears, IsoFields.QUARTER_YEARS);
        LocalDate t1 = TEST_2007_07_15
                .plus(quarterYears, ChronoUnit.MONTHS)
                .plus(quarterYears, ChronoUnit.MONTHS)
                .plus(quarterYears, ChronoUnit.MONTHS);
        assertEquals(t0, t1);
    }

    @Test(dataProvider="quarterYearsToAdd")
    public void test_minus_QuarterYears(long quarterYears) {
        LocalDate t0 = TEST_2007_07_15
                .minus(quarterYears, IsoFields.QUARTER_YEARS);
        LocalDate t1 = TEST_2007_07_15
                .minus(quarterYears, ChronoUnit.MONTHS)
                .minus(quarterYears, ChronoUnit.MONTHS)
                .minus(quarterYears, ChronoUnit.MONTHS);
        assertEquals(t0, t1);
    }
}
