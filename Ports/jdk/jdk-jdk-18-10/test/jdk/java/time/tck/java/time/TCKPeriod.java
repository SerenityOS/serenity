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
package tck.java.time;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.YEARS;
import static org.testng.Assert.assertEquals;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.chrono.ThaiBuddhistChronology;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test Period.
 */
@Test
public class TCKPeriod extends AbstractTCKTest {

    //-----------------------------------------------------------------------
    // ofYears(int)
    //-----------------------------------------------------------------------
    @Test
    public void factory_ofYears_int() {
        assertPeriod(Period.ofYears(0), 0, 0, 0);
        assertPeriod(Period.ofYears(1), 1, 0, 0);
        assertPeriod(Period.ofYears(234), 234, 0, 0);
        assertPeriod(Period.ofYears(-100), -100, 0, 0);
        assertPeriod(Period.ofYears(Integer.MAX_VALUE), Integer.MAX_VALUE, 0, 0);
        assertPeriod(Period.ofYears(Integer.MIN_VALUE), Integer.MIN_VALUE, 0, 0);
    }

    //-----------------------------------------------------------------------
    // ofMonths(int)
    //-----------------------------------------------------------------------
    @Test
    public void factory_ofMonths_int() {
        assertPeriod(Period.ofMonths(0), 0, 0, 0);
        assertPeriod(Period.ofMonths(1), 0, 1, 0);
        assertPeriod(Period.ofMonths(234), 0, 234, 0);
        assertPeriod(Period.ofMonths(-100), 0, -100, 0);
        assertPeriod(Period.ofMonths(Integer.MAX_VALUE), 0, Integer.MAX_VALUE, 0);
        assertPeriod(Period.ofMonths(Integer.MIN_VALUE), 0, Integer.MIN_VALUE, 0);
    }

    //-----------------------------------------------------------------------
    // ofWeeks(int)
    //-----------------------------------------------------------------------
    @Test
    public void factory_ofWeeks_int() {
        assertPeriod(Period.ofWeeks(0), 0, 0, 0);
        assertPeriod(Period.ofWeeks(1), 0, 0, 7);
        assertPeriod(Period.ofWeeks(234), 0, 0, 234 * 7);
        assertPeriod(Period.ofWeeks(-100), 0, 0, -100 * 7);
        assertPeriod(Period.ofWeeks(Integer.MAX_VALUE / 7), 0, 0, (Integer.MAX_VALUE / 7) * 7);
        assertPeriod(Period.ofWeeks(Integer.MIN_VALUE / 7), 0, 0, (Integer.MIN_VALUE / 7) * 7);
    }

    //-----------------------------------------------------------------------
    // ofDays(int)
    //-----------------------------------------------------------------------
    @Test
    public void factory_ofDays_int() {
        assertPeriod(Period.ofDays(0), 0, 0, 0);
        assertPeriod(Period.ofDays(1), 0, 0, 1);
        assertPeriod(Period.ofDays(234), 0, 0, 234);
        assertPeriod(Period.ofDays(-100), 0, 0, -100);
        assertPeriod(Period.ofDays(Integer.MAX_VALUE), 0, 0, Integer.MAX_VALUE);
        assertPeriod(Period.ofDays(Integer.MIN_VALUE), 0, 0, Integer.MIN_VALUE);
    }

    //-----------------------------------------------------------------------
    // of(int3)
    //-----------------------------------------------------------------------
    @Test
    public void factory_of_ints() {
        assertPeriod(Period.of(1, 2, 3), 1, 2, 3);
        assertPeriod(Period.of(0, 2, 3), 0, 2, 3);
        assertPeriod(Period.of(1, 0, 0), 1, 0, 0);
        assertPeriod(Period.of(0, 0, 0), 0, 0, 0);
        assertPeriod(Period.of(-1, -2, -3), -1, -2, -3);
    }

    //-----------------------------------------------------------------------
    // from(TemporalAmount)
    //-----------------------------------------------------------------------
    @Test
    public void factory_from_TemporalAmount_Period() {
        TemporalAmount amount = Period.of(1, 2, 3);
        assertPeriod(Period.from(amount), 1, 2, 3);
    }

    @Test
    public void factory_from_TemporalAmount_YearsDays() {
        TemporalAmount amount = new TemporalAmount() {
            @Override
            public long get(TemporalUnit unit) {
                if (unit == YEARS) {
                    return 23;
                } else {
                    return 45;
                }
            }
            @Override
            public List<TemporalUnit> getUnits() {
                List<TemporalUnit> list = new ArrayList<>();
                list.add(YEARS);
                list.add(DAYS);
                return list;
            }
            @Override
            public Temporal addTo(Temporal temporal) {
                throw new UnsupportedOperationException();
            }
            @Override
            public Temporal subtractFrom(Temporal temporal) {
                throw new UnsupportedOperationException();
            }
        };
        assertPeriod(Period.from(amount), 23, 0, 45);
    }

    @Test(expectedExceptions = ArithmeticException.class)
    public void factory_from_TemporalAmount_Years_tooBig() {
        TemporalAmount amount = new TemporalAmount() {
            @Override
            public long get(TemporalUnit unit) {
                return ((long) (Integer.MAX_VALUE)) + 1;
            }
            @Override
            public List<TemporalUnit> getUnits() {
                return Collections.<TemporalUnit>singletonList(YEARS);
            }
            @Override
            public Temporal addTo(Temporal temporal) {
                throw new UnsupportedOperationException();
            }
            @Override
            public Temporal subtractFrom(Temporal temporal) {
                throw new UnsupportedOperationException();
            }
        };
        Period.from(amount);
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void factory_from_TemporalAmount_DaysHours() {
        TemporalAmount amount = new TemporalAmount() {
            @Override
            public long get(TemporalUnit unit) {
                if (unit == DAYS) {
                    return 1;
                } else {
                    return 2;
                }
            }
            @Override
            public List<TemporalUnit> getUnits() {
                List<TemporalUnit> list = new ArrayList<>();
                list.add(DAYS);
                list.add(HOURS);
                return list;
            }
            @Override
            public Temporal addTo(Temporal temporal) {
                throw new UnsupportedOperationException();
            }
            @Override
            public Temporal subtractFrom(Temporal temporal) {
                throw new UnsupportedOperationException();
            }
        };
        Period.from(amount);
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void factory_from_TemporalAmount_NonISO() {
        Period.from(ThaiBuddhistChronology.INSTANCE.period(1, 1, 1));
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void factory_from_TemporalAmount_Duration() {
        Period.from(Duration.ZERO);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void factory_from_TemporalAmount_null() {
        Period.from(null);
    }

    //-----------------------------------------------------------------------
    // parse(String)
    //-----------------------------------------------------------------------
    @DataProvider(name="parseSuccess")
    Object[][] data_factory_parseSuccess() {
        return new Object[][] {
                {"P1Y", Period.ofYears(1)},
                {"P12Y", Period.ofYears(12)},
                {"P987654321Y", Period.ofYears(987654321)},
                {"P+1Y", Period.ofYears(1)},
                {"P+12Y", Period.ofYears(12)},
                {"P+987654321Y", Period.ofYears(987654321)},
                {"P+0Y", Period.ofYears(0)},
                {"P0Y", Period.ofYears(0)},
                {"P-0Y", Period.ofYears(0)},
                {"P-25Y", Period.ofYears(-25)},
                {"P-987654321Y", Period.ofYears(-987654321)},
                {"P" + Integer.MAX_VALUE + "Y", Period.ofYears(Integer.MAX_VALUE)},
                {"P" + Integer.MIN_VALUE + "Y", Period.ofYears(Integer.MIN_VALUE)},

                {"P1M", Period.ofMonths(1)},
                {"P12M", Period.ofMonths(12)},
                {"P987654321M", Period.ofMonths(987654321)},
                {"P+1M", Period.ofMonths(1)},
                {"P+12M", Period.ofMonths(12)},
                {"P+987654321M", Period.ofMonths(987654321)},
                {"P+0M", Period.ofMonths(0)},
                {"P0M", Period.ofMonths(0)},
                {"P-0M", Period.ofMonths(0)},
                {"P-25M", Period.ofMonths(-25)},
                {"P-987654321M", Period.ofMonths(-987654321)},
                {"P" + Integer.MAX_VALUE + "M", Period.ofMonths(Integer.MAX_VALUE)},
                {"P" + Integer.MIN_VALUE + "M", Period.ofMonths(Integer.MIN_VALUE)},

                {"P1W", Period.ofDays(1 * 7)},
                {"P12W", Period.ofDays(12 * 7)},
                {"P7654321W", Period.ofDays(7654321 * 7)},
                {"P+1W", Period.ofDays(1 * 7)},
                {"P+12W", Period.ofDays(12 * 7)},
                {"P+7654321W", Period.ofDays(7654321 * 7)},
                {"P+0W", Period.ofDays(0)},
                {"P0W", Period.ofDays(0)},
                {"P-0W", Period.ofDays(0)},
                {"P-25W", Period.ofDays(-25 * 7)},
                {"P-7654321W", Period.ofDays(-7654321 * 7)},

                {"P1D", Period.ofDays(1)},
                {"P12D", Period.ofDays(12)},
                {"P987654321D", Period.ofDays(987654321)},
                {"P+1D", Period.ofDays(1)},
                {"P+12D", Period.ofDays(12)},
                {"P+987654321D", Period.ofDays(987654321)},
                {"P+0D", Period.ofDays(0)},
                {"P0D", Period.ofDays(0)},
                {"P-0D", Period.ofDays(0)},
                {"P-25D", Period.ofDays(-25)},
                {"P-987654321D", Period.ofDays(-987654321)},
                {"P" + Integer.MAX_VALUE + "D", Period.ofDays(Integer.MAX_VALUE)},
                {"P" + Integer.MIN_VALUE + "D", Period.ofDays(Integer.MIN_VALUE)},

                {"P0Y0M0D", Period.of(0, 0, 0)},
                {"P2Y0M0D", Period.of(2, 0, 0)},
                {"P0Y3M0D", Period.of(0, 3, 0)},
                {"P0Y0M4D", Period.of(0, 0, 4)},
                {"P2Y3M25D", Period.of(2, 3, 25)},
                {"P-2Y3M25D", Period.of(-2, 3, 25)},
                {"P2Y-3M25D", Period.of(2, -3, 25)},
                {"P2Y3M-25D", Period.of(2, 3, -25)},
                {"P-2Y-3M-25D", Period.of(-2, -3, -25)},

                {"P0Y0M0W0D", Period.of(0, 0, 0)},
                {"P2Y3M4W25D", Period.of(2, 3, 4 * 7 + 25)},
                {"P-2Y-3M-4W-25D", Period.of(-2, -3, -4 * 7 - 25)},
        };
    }

    @Test(dataProvider="parseSuccess")
    public void factory_parse(String text, Period expected) {
        Period p = Period.parse(text);
        assertEquals(p, expected);
    }

    @Test(dataProvider="parseSuccess")
    public void factory_parse_plus(String text, Period expected) {
        Period p = Period.parse("+" + text);
        assertEquals(p, expected);
    }

    @Test(dataProvider="parseSuccess")
    public void factory_parse_minus(String text, Period expected) {
        Period p = null;
        try {
            p = Period.parse("-" + text);
        } catch (DateTimeParseException ex) {
            assertEquals(expected.getYears() == Integer.MIN_VALUE ||
                    expected.getMonths() == Integer.MIN_VALUE ||
                    expected.getDays() == Integer.MIN_VALUE, true);
            return;
        }
        // not inside try/catch or it breaks test
        assertEquals(p, expected.negated());
    }

    @Test(dataProvider="parseSuccess")
    public void factory_parse_lowerCase(String text, Period expected) {
        Period p = Period.parse(text.toLowerCase(Locale.ENGLISH));
        assertEquals(p, expected);
    }

    @DataProvider(name="parseFailure")
    Object[][] data_parseFailure() {
        return new Object[][] {
                {""},
                {"PTD"},
                {"AT0D"},
                {"PA0D"},
                {"PT0A"},

                {"PT+D"},
                {"PT-D"},
                {"PT.D"},
                {"PTAD"},

                {"PT+0D"},
                {"PT-0D"},
                {"PT+1D"},
                {"PT-.D"},

                {"P1Y1MT1D"},
                {"P1YMD"},
                {"P1Y2Y"},
                {"PT1M+3S"},

                {"P1M2Y"},
                {"P1W2Y"},
                {"P1D2Y"},
                {"P1W2M"},
                {"P1D2M"},
                {"P1D2W"},

                {"PT1S1"},
                {"PT1S."},
                {"PT1SA"},
                {"PT1M1"},
                {"PT1M."},
                {"PT1MA"},

                {"P"+ (((long) Integer.MAX_VALUE) + 1) + "Y"},
                {"P"+ (((long) Integer.MAX_VALUE) + 1) + "M"},
                {"P"+ (((long) Integer.MAX_VALUE) + 1) + "D"},
                {"P"+ (((long) Integer.MIN_VALUE) - 1) + "Y"},
                {"P"+ (((long) Integer.MIN_VALUE) - 1) + "M"},
                {"P"+ (((long) Integer.MIN_VALUE) - 1) + "D"},

                {"Rubbish"},
        };
    }

    @Test(dataProvider="parseFailure", expectedExceptions=DateTimeParseException.class)
    public void factory_parseFailures(String text) {
        try {
            Period.parse(text);
        } catch (DateTimeParseException ex) {
            assertEquals(ex.getParsedString(), text);
            throw ex;
        }
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_parse_null() {
        Period.parse(null);
    }

    //-----------------------------------------------------------------------
    // between(LocalDate,LocalDate)
    //-----------------------------------------------------------------------
    @DataProvider(name="between")
    Object[][] data_between() {
        return new Object[][] {
                {2010, 1, 1, 2010, 1, 1, 0, 0, 0},
                {2010, 1, 1, 2010, 1, 2, 0, 0, 1},
                {2010, 1, 1, 2010, 1, 31, 0, 0, 30},
                {2010, 1, 1, 2010, 2, 1, 0, 1, 0},
                {2010, 1, 1, 2010, 2, 28, 0, 1, 27},
                {2010, 1, 1, 2010, 3, 1, 0, 2, 0},
                {2010, 1, 1, 2010, 12, 31, 0, 11, 30},
                {2010, 1, 1, 2011, 1, 1, 1, 0, 0},
                {2010, 1, 1, 2011, 12, 31, 1, 11, 30},
                {2010, 1, 1, 2012, 1, 1, 2, 0, 0},

                {2010, 1, 10, 2010, 1, 1, 0, 0, -9},
                {2010, 1, 10, 2010, 1, 2, 0, 0, -8},
                {2010, 1, 10, 2010, 1, 9, 0, 0, -1},
                {2010, 1, 10, 2010, 1, 10, 0, 0, 0},
                {2010, 1, 10, 2010, 1, 11, 0, 0, 1},
                {2010, 1, 10, 2010, 1, 31, 0, 0, 21},
                {2010, 1, 10, 2010, 2, 1, 0, 0, 22},
                {2010, 1, 10, 2010, 2, 9, 0, 0, 30},
                {2010, 1, 10, 2010, 2, 10, 0, 1, 0},
                {2010, 1, 10, 2010, 2, 28, 0, 1, 18},
                {2010, 1, 10, 2010, 3, 1, 0, 1, 19},
                {2010, 1, 10, 2010, 3, 9, 0, 1, 27},
                {2010, 1, 10, 2010, 3, 10, 0, 2, 0},
                {2010, 1, 10, 2010, 12, 31, 0, 11, 21},
                {2010, 1, 10, 2011, 1, 1, 0, 11, 22},
                {2010, 1, 10, 2011, 1, 9, 0, 11, 30},
                {2010, 1, 10, 2011, 1, 10, 1, 0, 0},

                {2010, 3, 30, 2011, 5, 1, 1, 1, 1},
                {2010, 4, 30, 2011, 5, 1, 1, 0, 1},

                {2010, 2, 28, 2012, 2, 27, 1, 11, 30},
                {2010, 2, 28, 2012, 2, 28, 2, 0, 0},
                {2010, 2, 28, 2012, 2, 29, 2, 0, 1},

                {2012, 2, 28, 2014, 2, 27, 1, 11, 30},
                {2012, 2, 28, 2014, 2, 28, 2, 0, 0},
                {2012, 2, 28, 2014, 3, 1, 2, 0, 1},

                {2012, 2, 29, 2014, 2, 28, 1, 11, 30},
                {2012, 2, 29, 2014, 3, 1, 2, 0, 1},
                {2012, 2, 29, 2014, 3, 2, 2, 0, 2},

                {2012, 2, 29, 2016, 2, 28, 3, 11, 30},
                {2012, 2, 29, 2016, 2, 29, 4, 0, 0},
                {2012, 2, 29, 2016, 3, 1, 4, 0, 1},

                {2010, 1, 1, 2009, 12, 31, 0, 0, -1},
                {2010, 1, 1, 2009, 12, 30, 0, 0, -2},
                {2010, 1, 1, 2009, 12, 2, 0, 0, -30},
                {2010, 1, 1, 2009, 12, 1, 0, -1, 0},
                {2010, 1, 1, 2009, 11, 30, 0, -1, -1},
                {2010, 1, 1, 2009, 11, 2, 0, -1, -29},
                {2010, 1, 1, 2009, 11, 1, 0, -2, 0},
                {2010, 1, 1, 2009, 1, 2, 0, -11, -30},
                {2010, 1, 1, 2009, 1, 1, -1, 0, 0},

                {2010, 1, 15, 2010, 1, 15, 0, 0, 0},
                {2010, 1, 15, 2010, 1, 14, 0, 0, -1},
                {2010, 1, 15, 2010, 1, 1, 0, 0, -14},
                {2010, 1, 15, 2009, 12, 31, 0, 0, -15},
                {2010, 1, 15, 2009, 12, 16, 0, 0, -30},
                {2010, 1, 15, 2009, 12, 15, 0, -1, 0},
                {2010, 1, 15, 2009, 12, 14, 0, -1, -1},

                {2010, 2, 28, 2009, 3, 1, 0, -11, -27},
                {2010, 2, 28, 2009, 2, 28, -1, 0, 0},
                {2010, 2, 28, 2009, 2, 27, -1, 0, -1},

                {2010, 2, 28, 2008, 2, 29, -1, -11, -28},
                {2010, 2, 28, 2008, 2, 28, -2, 0, 0},
                {2010, 2, 28, 2008, 2, 27, -2, 0, -1},

                {2012, 2, 29, 2009, 3, 1, -2, -11, -28},
                {2012, 2, 29, 2009, 2, 28, -3, 0, -1},
                {2012, 2, 29, 2009, 2, 27, -3, 0, -2},

                {2012, 2, 29, 2008, 3, 1, -3, -11, -28},
                {2012, 2, 29, 2008, 2, 29, -4, 0, 0},
                {2012, 2, 29, 2008, 2, 28, -4, 0, -1},
        };
    }

    @Test(dataProvider="between")
    public void factory_between_LocalDate(int y1, int m1, int d1, int y2, int m2, int d2, int ye, int me, int de) {
        LocalDate start = LocalDate.of(y1, m1, d1);
        LocalDate end = LocalDate.of(y2, m2, d2);
        Period test = Period.between(start, end);
        assertPeriod(test, ye, me, de);
        //assertEquals(start.plus(test), end);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_between_LocalDate_nullFirst() {
        Period.between((LocalDate) null, LocalDate.of(2010, 1, 1));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void factory_between_LocalDate_nullSecond() {
        Period.between(LocalDate.of(2010, 1, 1), (LocalDate) null);
    }

    //-----------------------------------------------------------------------
    // isZero()
    //-----------------------------------------------------------------------
    @Test
    public void test_isZero() {
        assertEquals(Period.of(0, 0, 0).isZero(), true);
        assertEquals(Period.of(1, 2, 3).isZero(), false);
        assertEquals(Period.of(1, 0, 0).isZero(), false);
        assertEquals(Period.of(0, 2, 0).isZero(), false);
        assertEquals(Period.of(0, 0, 3).isZero(), false);
    }

    //-----------------------------------------------------------------------
    // isNegative()
    //-----------------------------------------------------------------------
    @Test
    public void test_isPositive() {
        assertEquals(Period.of(0, 0, 0).isNegative(), false);
        assertEquals(Period.of(1, 2, 3).isNegative(), false);
        assertEquals(Period.of(1, 0, 0).isNegative(), false);
        assertEquals(Period.of(0, 2, 0).isNegative(), false);
        assertEquals(Period.of(0, 0, 3).isNegative(), false);

        assertEquals(Period.of(-1, -2, -3).isNegative(), true);
        assertEquals(Period.of(-1, -2, 3).isNegative(), true);
        assertEquals(Period.of(1, -2, -3).isNegative(), true);
        assertEquals(Period.of(-1, 2, -3).isNegative(), true);
        assertEquals(Period.of(-1, 2, 3).isNegative(), true);
        assertEquals(Period.of(1, -2, 3).isNegative(), true);
        assertEquals(Period.of(1, 2, -3).isNegative(), true);
    }

    //-----------------------------------------------------------------------
    // withYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_withYears() {
        assertPeriod(Period.of(1, 2, 3).withYears(1), 1, 2, 3);
        assertPeriod(Period.of(1, 2, 3).withYears(10), 10, 2, 3);
        assertPeriod(Period.of(1, 2, 3).withYears(-10), -10, 2, 3);
        assertPeriod(Period.of(-1, -2, -3).withYears(10), 10, -2, -3);
        assertPeriod(Period.of(1, 2, 3).withYears(0), 0, 2, 3);
    }

    //-----------------------------------------------------------------------
    // withMonths()
    //-----------------------------------------------------------------------
    @Test
    public void test_withMonths() {
        assertPeriod(Period.of(1, 2, 3).withMonths(2), 1, 2, 3);
        assertPeriod(Period.of(1, 2, 3).withMonths(10), 1, 10, 3);
        assertPeriod(Period.of(1, 2, 3).withMonths(-10), 1, -10, 3);
        assertPeriod(Period.of(-1, -2, -3).withMonths(10), -1, 10, -3);
        assertPeriod(Period.of(1, 2, 3).withMonths(0), 1, 0, 3);
    }

    //-----------------------------------------------------------------------
    // withDays()
    //-----------------------------------------------------------------------
    @Test
    public void test_withDays() {
        assertPeriod(Period.of(1, 2, 3).withDays(3), 1, 2, 3);
        assertPeriod(Period.of(1, 2, 3).withDays(10), 1, 2, 10);
        assertPeriod(Period.of(1, 2, 3).withDays(-10), 1, 2, -10);
        assertPeriod(Period.of(-1, -2, -3).withDays(10), -1, -2, 10);
        assertPeriod(Period.of(1, 2, 3).withDays(0), 1, 2, 0);
    }

    //-----------------------------------------------------------------------
    // plus(Period)
    //-----------------------------------------------------------------------
    @DataProvider(name="plus")
    Object[][] data_plus() {
        return new Object[][] {
                {pymd(0, 0, 0), pymd(0, 0, 0), pymd(0, 0, 0)},
                {pymd(0, 0, 0), pymd(5, 0, 0), pymd(5, 0, 0)},
                {pymd(0, 0, 0), pymd(-5, 0, 0), pymd(-5, 0, 0)},
                {pymd(0, 0, 0), pymd(0, 5, 0), pymd(0, 5, 0)},
                {pymd(0, 0, 0), pymd(0, -5, 0), pymd(0, -5, 0)},
                {pymd(0, 0, 0), pymd(0, 0, 5), pymd(0, 0, 5)},
                {pymd(0, 0, 0), pymd(0, 0, -5), pymd(0, 0, -5)},
                {pymd(0, 0, 0), pymd(2, 3, 4), pymd(2, 3, 4)},
                {pymd(0, 0, 0), pymd(-2, -3, -4), pymd(-2, -3, -4)},

                {pymd(4, 5, 6), pymd(2, 3, 4), pymd(6, 8, 10)},
                {pymd(4, 5, 6), pymd(-2, -3, -4), pymd(2, 2, 2)},
        };
    }

    @Test(dataProvider="plus")
    public void test_plus_TemporalAmount(Period base, Period add, Period expected) {
        assertEquals(base.plus(add), expected);
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void test_plus_TemporalAmount_nonISO() {
        pymd(4, 5, 6).plus(ThaiBuddhistChronology.INSTANCE.period(1, 0, 0));
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void test_plus_TemporalAmount_DaysHours() {
        TemporalAmount amount = new TemporalAmount() {
            @Override
            public long get(TemporalUnit unit) {
                if (unit == DAYS) {
                    return 1;
                } else {
                    return 2;
                }
            }
            @Override
            public List<TemporalUnit> getUnits() {
                List<TemporalUnit> list = new ArrayList<>();
                list.add(DAYS);
                list.add(HOURS);
                return list;
            }
            @Override
            public Temporal addTo(Temporal temporal) {
                throw new UnsupportedOperationException();
            }
            @Override
            public Temporal subtractFrom(Temporal temporal) {
                throw new UnsupportedOperationException();
            }
        };
        pymd(4, 5, 6).plus(amount);
    }

    //-----------------------------------------------------------------------
    // plusYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusYears() {
        assertPeriod(Period.of(1, 2, 3).plusYears(0), 1, 2, 3);
        assertPeriod(Period.of(1, 2, 3).plusYears(10), 11, 2, 3);
        assertPeriod(Period.of(1, 2, 3).plusYears(-10), -9, 2, 3);
        assertPeriod(Period.of(1, 2, 3).plusYears(-1), 0, 2, 3);

        assertPeriod(Period.of(1, 2, 3).plus(Period.ofYears(0)), 1, 2, 3);
        assertPeriod(Period.of(1, 2, 3).plus(Period.ofYears(10)), 11, 2, 3);
        assertPeriod(Period.of(1, 2, 3).plus(Period.ofYears(-10)), -9, 2, 3);
        assertPeriod(Period.of(1, 2, 3).plus(Period.ofYears(-1)), 0, 2, 3);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_plusYears_overflowTooBig() {
        Period test = Period.ofYears(Integer.MAX_VALUE);
        test.plusYears(1);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_plusYears_overflowTooSmall() {
        Period test = Period.ofYears(Integer.MIN_VALUE);
        test.plusYears(-1);
    }

    //-----------------------------------------------------------------------
    // plusMonths()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusMonths() {
        assertPeriod(Period.of(1, 2, 3).plusMonths(0), 1, 2, 3);
        assertPeriod(Period.of(1, 2, 3).plusMonths(10), 1, 12, 3);
        assertPeriod(Period.of(1, 2, 3).plusMonths(-10), 1, -8, 3);
        assertPeriod(Period.of(1, 2, 3).plusMonths(-2), 1, 0, 3);

        assertPeriod(Period.of(1, 2, 3).plus(Period.ofMonths(0)), 1, 2, 3);
        assertPeriod(Period.of(1, 2, 3).plus(Period.ofMonths(10)), 1, 12, 3);
        assertPeriod(Period.of(1, 2, 3).plus(Period.ofMonths(-10)), 1, -8, 3);
        assertPeriod(Period.of(1, 2, 3).plus(Period.ofMonths(-2)), 1, 0, 3);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_plusMonths_overflowTooBig() {
        Period test = Period.ofMonths(Integer.MAX_VALUE);
        test.plusMonths(1);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_plusMonths_overflowTooSmall() {
        Period test = Period.ofMonths(Integer.MIN_VALUE);
        test.plusMonths(-1);
    }

    //-----------------------------------------------------------------------
    // plusDays()
    //-----------------------------------------------------------------------
    @Test
    public void test_plusDays() {
        assertPeriod(Period.of(1, 2, 3).plusDays(0), 1, 2, 3);
        assertPeriod(Period.of(1, 2, 3).plusDays(10), 1, 2, 13);
        assertPeriod(Period.of(1, 2, 3).plusDays(-10), 1, 2, -7);
        assertPeriod(Period.of(1, 2, 3).plusDays(-3), 1, 2, 0);

        assertPeriod(Period.of(1, 2, 3).plus(Period.ofDays(0)), 1, 2, 3);
        assertPeriod(Period.of(1, 2, 3).plus(Period.ofDays(10)), 1, 2, 13);
        assertPeriod(Period.of(1, 2, 3).plus(Period.ofDays(-10)), 1, 2, -7);
        assertPeriod(Period.of(1, 2, 3).plus(Period.ofDays(-3)), 1, 2, 0);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_plusDays_overflowTooBig() {
        Period test = Period.ofDays(Integer.MAX_VALUE);
        test.plusDays(1);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_plusDays_overflowTooSmall() {
        Period test = Period.ofDays(Integer.MIN_VALUE);
        test.plusDays(-1);
    }

    //-----------------------------------------------------------------------
    // minus(Period)
    //-----------------------------------------------------------------------
    @DataProvider(name="minus")
    Object[][] data_minus() {
        return new Object[][] {
                {pymd(0, 0, 0), pymd(0, 0, 0), pymd(0, 0, 0)},
                {pymd(0, 0, 0), pymd(5, 0, 0), pymd(-5, 0, 0)},
                {pymd(0, 0, 0), pymd(-5, 0, 0), pymd(5, 0, 0)},
                {pymd(0, 0, 0), pymd(0, 5, 0), pymd(0, -5, 0)},
                {pymd(0, 0, 0), pymd(0, -5, 0), pymd(0, 5, 0)},
                {pymd(0, 0, 0), pymd(0, 0, 5), pymd(0, 0, -5)},
                {pymd(0, 0, 0), pymd(0, 0, -5), pymd(0, 0, 5)},
                {pymd(0, 0, 0), pymd(2, 3, 4), pymd(-2, -3, -4)},
                {pymd(0, 0, 0), pymd(-2, -3, -4), pymd(2, 3, 4)},

                {pymd(4, 5, 6), pymd(2, 3, 4), pymd(2, 2, 2)},
                {pymd(4, 5, 6), pymd(-2, -3, -4), pymd(6, 8, 10)},
        };
    }

    @Test(dataProvider="minus")
    public void test_minus_TemporalAmount(Period base, Period subtract, Period expected) {
        assertEquals(base.minus(subtract), expected);
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void test_minus_TemporalAmount_nonISO() {
        pymd(4, 5, 6).minus(ThaiBuddhistChronology.INSTANCE.period(1, 0, 0));
    }

    @Test(expectedExceptions = DateTimeException.class)
    public void test_minus_TemporalAmount_DaysHours() {
        TemporalAmount amount = new TemporalAmount() {
            @Override
            public long get(TemporalUnit unit) {
                if (unit == DAYS) {
                    return 1;
                } else {
                    return 2;
                }
            }
            @Override
            public List<TemporalUnit> getUnits() {
                List<TemporalUnit> list = new ArrayList<>();
                list.add(DAYS);
                list.add(HOURS);
                return list;
            }
            @Override
            public Temporal addTo(Temporal temporal) {
                throw new UnsupportedOperationException();
            }
            @Override
            public Temporal subtractFrom(Temporal temporal) {
                throw new UnsupportedOperationException();
            }
        };
        pymd(4, 5, 6).minus(amount);
    }

    //-----------------------------------------------------------------------
    // minusYears()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusYears() {
        assertPeriod(Period.of(1, 2, 3).minusYears(0), 1, 2, 3);
        assertPeriod(Period.of(1, 2, 3).minusYears(10), -9, 2, 3);
        assertPeriod(Period.of(1, 2, 3).minusYears(-10), 11, 2, 3);
        assertPeriod(Period.of(1, 2, 3).minusYears(-1), 2, 2, 3);

        assertPeriod(Period.of(1, 2, 3).minus(Period.ofYears(0)), 1, 2, 3);
        assertPeriod(Period.of(1, 2, 3).minus(Period.ofYears(10)), -9, 2, 3);
        assertPeriod(Period.of(1, 2, 3).minus(Period.ofYears(-10)), 11, 2, 3);
        assertPeriod(Period.of(1, 2, 3).minus(Period.ofYears(-1)), 2, 2, 3);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_minusYears_overflowTooBig() {
        Period test = Period.ofYears(Integer.MAX_VALUE);
        test.minusYears(-1);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_minusYears_overflowTooSmall() {
        Period test = Period.ofYears(Integer.MIN_VALUE);
        test.minusYears(1);
    }

    //-----------------------------------------------------------------------
    // minusMonths()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusMonths() {
        assertPeriod(Period.of(1, 2, 3).minusMonths(0), 1, 2, 3);
        assertPeriod(Period.of(1, 2, 3).minusMonths(10), 1, -8, 3);
        assertPeriod(Period.of(1, 2, 3).minusMonths(-10), 1, 12, 3);
        assertPeriod(Period.of(1, 2, 3).minusMonths(-2), 1, 4, 3);

        assertPeriod(Period.of(1, 2, 3).minus(Period.ofMonths(0)), 1, 2, 3);
        assertPeriod(Period.of(1, 2, 3).minus(Period.ofMonths(10)), 1, -8, 3);
        assertPeriod(Period.of(1, 2, 3).minus(Period.ofMonths(-10)), 1, 12, 3);
        assertPeriod(Period.of(1, 2, 3).minus(Period.ofMonths(-2)), 1, 4, 3);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_minusMonths_overflowTooBig() {
        Period test = Period.ofMonths(Integer.MAX_VALUE);
        test.minusMonths(-1);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_minusMonths_overflowTooSmall() {
        Period test = Period.ofMonths(Integer.MIN_VALUE);
        test.minusMonths(1);
    }

    //-----------------------------------------------------------------------
    // minusDays()
    //-----------------------------------------------------------------------
    @Test
    public void test_minusDays() {
        assertPeriod(Period.of(1, 2, 3).minusDays(0), 1, 2, 3);
        assertPeriod(Period.of(1, 2, 3).minusDays(10), 1, 2, -7);
        assertPeriod(Period.of(1, 2, 3).minusDays(-10), 1, 2, 13);
        assertPeriod(Period.of(1, 2, 3).minusDays(-3), 1, 2, 6);

        assertPeriod(Period.of(1, 2, 3).minus(Period.ofDays(0)), 1, 2, 3);
        assertPeriod(Period.of(1, 2, 3).minus(Period.ofDays(10)), 1, 2, -7);
        assertPeriod(Period.of(1, 2, 3).minus(Period.ofDays(-10)), 1, 2, 13);
        assertPeriod(Period.of(1, 2, 3).minus(Period.ofDays(-3)), 1, 2, 6);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_minusDays_overflowTooBig() {
        Period test = Period.ofDays(Integer.MAX_VALUE);
        test.minusDays(-1);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_minusDays_overflowTooSmall() {
        Period test = Period.ofDays(Integer.MIN_VALUE);
        test.minusDays(1);
    }

    //-----------------------------------------------------------------------
    // multipliedBy()
    //-----------------------------------------------------------------------
    @Test
    public void test_multipliedBy() {
        Period test = Period.of(1, 2, 3);
        assertPeriod(test.multipliedBy(0), 0, 0, 0);
        assertPeriod(test.multipliedBy(1), 1, 2, 3);
        assertPeriod(test.multipliedBy(2), 2, 4, 6);
        assertPeriod(test.multipliedBy(-3), -3, -6, -9);
    }

    @Test
    public void test_multipliedBy_zeroBase() {
        assertPeriod(Period.ZERO.multipliedBy(2), 0, 0, 0);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_multipliedBy_overflowTooBig() {
        Period test = Period.ofYears(Integer.MAX_VALUE / 2 + 1);
        test.multipliedBy(2);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_multipliedBy_overflowTooSmall() {
        Period test = Period.ofYears(Integer.MIN_VALUE / 2 - 1);
        test.multipliedBy(2);
    }

    //-----------------------------------------------------------------------
    // negated()
    //-----------------------------------------------------------------------
    @Test
    public void test_negated() {
        assertPeriod(Period.of(0, 0, 0).negated(), 0 ,0, 0);
        assertPeriod(Period.of(1, 2, 3).negated(), -1, -2, -3);
        assertPeriod(Period.of(-1, -2, -3).negated(), 1, 2, 3);
        assertPeriod(Period.of(-1, 2, -3).negated(), 1, -2, 3);
        assertPeriod(Period.of(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE).negated(),
                -Integer.MAX_VALUE, -Integer.MAX_VALUE, -Integer.MAX_VALUE);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_negated_overflow_years() {
        Period.ofYears(Integer.MIN_VALUE).negated();
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_negated_overflow_months() {
        Period.ofMonths(Integer.MIN_VALUE).negated();
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_negated_overflow_days() {
        Period.ofDays(Integer.MIN_VALUE).negated();
    }

    //-----------------------------------------------------------------------
    // normalized()
    //-----------------------------------------------------------------------
    @DataProvider(name="normalized")
    Object[][] data_normalized() {
        return new Object[][] {
                {0, 0,  0, 0},
                {1, 0,  1, 0},
                {-1, 0,  -1, 0},

                {1, 1,  1, 1},
                {1, 2,  1, 2},
                {1, 11,  1, 11},
                {1, 12,  2, 0},
                {1, 13,  2, 1},
                {1, 23,  2, 11},
                {1, 24,  3, 0},
                {1, 25,  3, 1},

                {1, -1,  0, 11},
                {1, -2,  0, 10},
                {1, -11,  0, 1},
                {1, -12,  0, 0},
                {1, -13,  0, -1},
                {1, -23,  0, -11},
                {1, -24,  -1, 0},
                {1, -25,  -1, -1},
                {1, -35,  -1, -11},
                {1, -36,  -2, 0},
                {1, -37,  -2, -1},

                {-1, 1,  0, -11},
                {-1, 11,  0, -1},
                {-1, 12,  0, 0},
                {-1, 13,  0, 1},
                {-1, 23,  0, 11},
                {-1, 24,  1, 0},
                {-1, 25,  1, 1},

                {-1, -1,  -1, -1},
                {-1, -11,  -1, -11},
                {-1, -12,  -2, 0},
                {-1, -13,  -2, -1},
        };
    }

    @Test(dataProvider="normalized")
    public void test_normalized(int inputYears, int inputMonths, int expectedYears, int expectedMonths) {
        assertPeriod(Period.of(inputYears, inputMonths, 0).normalized(), expectedYears, expectedMonths, 0);
    }

    @Test(dataProvider="normalized")
    public void test_normalized_daysUnaffected(int inputYears, int inputMonths, int expectedYears, int expectedMonths) {
        assertPeriod(Period.of(inputYears, inputMonths, 5).normalized(), expectedYears, expectedMonths, 5);
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_normalized_min() {
        Period base = Period.of(Integer.MIN_VALUE, -12, 0);
        base.normalized();
    }

    @Test(expectedExceptions=ArithmeticException.class)
    public void test_normalized_max() {
        Period base = Period.of(Integer.MAX_VALUE, 12, 0);
        base.normalized();
    }

    //-----------------------------------------------------------------------
    // addTo()
    //-----------------------------------------------------------------------
    @DataProvider(name="addTo")
    Object[][] data_addTo() {
        return new Object[][] {
                {pymd(0, 0, 0),  date(2012, 6, 30), date(2012, 6, 30)},

                {pymd(1, 0, 0),  date(2012, 6, 10), date(2013, 6, 10)},
                {pymd(0, 1, 0),  date(2012, 6, 10), date(2012, 7, 10)},
                {pymd(0, 0, 1),  date(2012, 6, 10), date(2012, 6, 11)},

                {pymd(-1, 0, 0),  date(2012, 6, 10), date(2011, 6, 10)},
                {pymd(0, -1, 0),  date(2012, 6, 10), date(2012, 5, 10)},
                {pymd(0, 0, -1),  date(2012, 6, 10), date(2012, 6, 9)},

                {pymd(1, 2, 3),  date(2012, 6, 27), date(2013, 8, 30)},
                {pymd(1, 2, 3),  date(2012, 6, 28), date(2013, 8, 31)},
                {pymd(1, 2, 3),  date(2012, 6, 29), date(2013, 9, 1)},
                {pymd(1, 2, 3),  date(2012, 6, 30), date(2013, 9, 2)},
                {pymd(1, 2, 3),  date(2012, 7, 1), date(2013, 9, 4)},

                {pymd(1, 0, 0),  date(2011, 2, 28), date(2012, 2, 28)},
                {pymd(4, 0, 0),  date(2011, 2, 28), date(2015, 2, 28)},
                {pymd(1, 0, 0),  date(2012, 2, 29), date(2013, 2, 28)},
                {pymd(4, 0, 0),  date(2012, 2, 29), date(2016, 2, 29)},

                {pymd(1, 1, 0),  date(2011, 1, 29), date(2012, 2, 29)},
                {pymd(1, 2, 0),  date(2012, 2, 29), date(2013, 4, 29)},
        };
    }

    @Test(dataProvider="addTo")
    public void test_addTo(Period period, LocalDate baseDate, LocalDate expected) {
        assertEquals(period.addTo(baseDate), expected);
    }

    @Test(dataProvider="addTo")
    public void test_addTo_usingLocalDatePlus(Period period, LocalDate baseDate, LocalDate expected) {
        assertEquals(baseDate.plus(period), expected);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_addTo_nullZero() {
        Period.ZERO.addTo(null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_addTo_nullNonZero() {
        Period.ofDays(2).addTo(null);
    }

    //-----------------------------------------------------------------------
    // subtractFrom()
    //-----------------------------------------------------------------------
    @DataProvider(name="subtractFrom")
    Object[][] data_subtractFrom() {
        return new Object[][] {
                {pymd(0, 0, 0), date(2012, 6, 30), date(2012, 6, 30)},

                {pymd(1, 0, 0), date(2012, 6, 10), date(2011, 6, 10)},
                {pymd(0, 1, 0), date(2012, 6, 10), date(2012, 5, 10)},
                {pymd(0, 0, 1), date(2012, 6, 10), date(2012, 6, 9)},

                {pymd(-1, 0, 0), date(2012, 6, 10), date(2013, 6, 10)},
                {pymd(0, -1, 0), date(2012, 6, 10), date(2012, 7, 10)},
                {pymd(0, 0, -1), date(2012, 6, 10), date(2012, 6, 11)},

                {pymd(1, 2, 3), date(2012, 8, 30), date(2011, 6, 27)},
                {pymd(1, 2, 3), date(2012, 8, 31), date(2011, 6, 27)},
                {pymd(1, 2, 3), date(2012, 9, 1), date(2011, 6, 28)},
                {pymd(1, 2, 3), date(2012, 9, 2), date(2011, 6, 29)},
                {pymd(1, 2, 3), date(2012, 9, 3), date(2011, 6, 30)},
                {pymd(1, 2, 3), date(2012, 9, 4), date(2011, 7, 1)},

                {pymd(1, 0, 0), date(2011, 2, 28), date(2010, 2, 28)},
                {pymd(4, 0, 0), date(2011, 2, 28), date(2007, 2, 28)},
                {pymd(1, 0, 0), date(2012, 2, 29), date(2011, 2, 28)},
                {pymd(4, 0, 0), date(2012, 2, 29), date(2008, 2, 29)},

                {pymd(1, 1, 0), date(2013, 3, 29), date(2012, 2, 29)},
                {pymd(1, 2, 0), date(2012, 2, 29), date(2010, 12, 29)},
        };
    }

    @Test(dataProvider="subtractFrom")
    public void test_subtractFrom(Period period, LocalDate baseDate, LocalDate expected) {
        assertEquals(period.subtractFrom(baseDate), expected);
    }

    @Test(dataProvider="subtractFrom")
    public void test_subtractFrom_usingLocalDateMinus(Period period, LocalDate baseDate, LocalDate expected) {
        assertEquals(baseDate.minus(period), expected);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_subtractFrom_nullZero() {
        Period.ZERO.subtractFrom(null);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_subtractFrom_nullNonZero() {
        Period.ofDays(2).subtractFrom(null);
    }

    //-----------------------------------------------------------------------
    // get units
    //-----------------------------------------------------------------------
    @Test
    public void test_Period_getUnits() {
        Period period = Period.of(2012, 1, 1);
        List<TemporalUnit> units = period.getUnits();
        assertEquals(units.size(), 3, "Period.getUnits should return 3 units");
        assertEquals(units.get(0), ChronoUnit.YEARS, "Period.getUnits contains ChronoUnit.YEARS");
        assertEquals(units.get(1), ChronoUnit.MONTHS, "Period.getUnits contains ChronoUnit.MONTHS");
        assertEquals(units.get(2), ChronoUnit.DAYS, "Period.getUnits contains ChronoUnit.DAYS");
    }


    @DataProvider(name="GoodTemporalUnit")
    Object[][] data_goodTemporalUnit() {
        return new Object[][] {
            {2, ChronoUnit.DAYS},
            {2, ChronoUnit.MONTHS},
            {2, ChronoUnit.YEARS},
        };
    }

    @Test(dataProvider="GoodTemporalUnit")
    public void test_good_getUnit(long amount, TemporalUnit unit) {
        Period period = Period.of(2, 2, 2);
        long actual = period.get(unit);
        assertEquals(actual, amount, "Value of unit: " + unit);
    }

    @DataProvider(name="BadTemporalUnit")
    Object[][] data_badTemporalUnit() {
        return new Object[][] {
            {ChronoUnit.MICROS},
            {ChronoUnit.MILLIS},
            {ChronoUnit.HALF_DAYS},
            {ChronoUnit.DECADES},
            {ChronoUnit.CENTURIES},
            {ChronoUnit.MILLENNIA},
        };
    }

    @Test(dataProvider="BadTemporalUnit", expectedExceptions=DateTimeException.class)
    public void test_bad_getUnit(TemporalUnit unit) {
        Period period = Period.of(2, 2, 2);
        period.get(unit);
    }

    //-----------------------------------------------------------------------
    // equals() / hashCode()
    //-----------------------------------------------------------------------
    public void test_equals() {
        assertEquals(Period.of(1, 0, 0).equals(Period.ofYears(1)), true);
        assertEquals(Period.of(0, 1, 0).equals(Period.ofMonths(1)), true);
        assertEquals(Period.of(0, 0, 1).equals(Period.ofDays(1)), true);
        assertEquals(Period.of(1, 2, 3).equals(Period.of(1, 2, 3)), true);

        assertEquals(Period.ofYears(1).equals(Period.ofYears(1)), true);
        assertEquals(Period.ofYears(1).equals(Period.ofYears(2)), false);

        assertEquals(Period.ofMonths(1).equals(Period.ofMonths(1)), true);
        assertEquals(Period.ofMonths(1).equals(Period.ofMonths(2)), false);

        assertEquals(Period.ofDays(1).equals(Period.ofDays(1)), true);
        assertEquals(Period.ofDays(1).equals(Period.ofDays(2)), false);

        assertEquals(Period.of(1, 2, 3).equals(Period.of(0, 2, 3)), false);
        assertEquals(Period.of(1, 2, 3).equals(Period.of(1, 0, 3)), false);
        assertEquals(Period.of(1, 2, 3).equals(Period.of(1, 2, 0)), false);
    }

    public void test_equals_self() {
        Period test = Period.of(1, 2, 3);
        assertEquals(test.equals(test), true);
    }

    public void test_equals_null() {
        Period test = Period.of(1, 2, 3);
        assertEquals(test.equals(null), false);
    }

    public void test_equals_otherClass() {
        Period test = Period.of(1, 2, 3);
        assertEquals(test.equals(""), false);
    }

    //-----------------------------------------------------------------------
    public void test_hashCode() {
        Period test5 = Period.ofDays(5);
        Period test6 = Period.ofDays(6);
        Period test5M = Period.ofMonths(5);
        Period test5Y = Period.ofYears(5);
        assertEquals(test5.hashCode() == test5.hashCode(), true);
        assertEquals(test5.hashCode() == test6.hashCode(), false);
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @DataProvider(name="toStringAndParse")
    Object[][] data_toString() {
        return new Object[][] {
                {Period.ZERO, "P0D"},
                {Period.ofDays(0), "P0D"},
                {Period.ofYears(1), "P1Y"},
                {Period.ofMonths(1), "P1M"},
                {Period.ofDays(1), "P1D"},
                {Period.of(1, 2, 0), "P1Y2M"},
                {Period.of(0, 2, 3), "P2M3D"},
                {Period.of(1, 2, 3), "P1Y2M3D"},
        };
    }

    @Test(dataProvider="toStringAndParse")
    public void test_toString(Period input, String expected) {
        assertEquals(input.toString(), expected);
    }

    @Test(dataProvider="toStringAndParse")
    public void test_parse(Period test, String expected) {
        assertEquals(Period.parse(expected), test);
    }

    //-----------------------------------------------------------------------
    private void assertPeriod(Period test, int y, int m, int d) {
        assertEquals(test.getYears(), y, "years");
        assertEquals(test.getMonths(), m, "months");
        assertEquals(test.getDays(), d, "days");
        assertEquals(test.toTotalMonths(), y * 12L + m, "totalMonths");
    }

    private static Period pymd(int y, int m, int d) {
        return Period.of(y, m, d);
    }

    private static LocalDate date(int y, int m, int d) {
        return LocalDate.of(y, m, d);
    }

}
