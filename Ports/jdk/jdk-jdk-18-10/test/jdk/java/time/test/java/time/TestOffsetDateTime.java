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
package test.java.time;

import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertEquals;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test OffsetDateTime.
 *
 * @bug 8211990
 */
@Test
public class TestOffsetDateTime extends AbstractTest {

    private static final ZoneOffset OFFSET_PONE = ZoneOffset.ofHours(1);
    private static final ZoneOffset OFFSET_PTWO = ZoneOffset.ofHours(2);
    private OffsetDateTime TEST_2008_6_30_11_30_59_000000500;

    @BeforeMethod
    public void setUp() {
        TEST_2008_6_30_11_30_59_000000500 = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59, 500), OFFSET_PONE);
    }

    @Test
    public void test_immutable() {
        assertImmutable(OffsetDateTime.class);
    }

    //-----------------------------------------------------------------------
    // basics
    //-----------------------------------------------------------------------
    @DataProvider(name="sampleTimes")
    Object[][] provider_sampleTimes() {
        return new Object[][] {
            {2008, 6, 30, 11, 30, 20, 500, OFFSET_PONE},
            {2008, 6, 30, 11, 0, 0, 0, OFFSET_PONE},
            {2008, 6, 30, 23, 59, 59, 999999999, OFFSET_PONE},
            {-1, 1, 1, 0, 0, 0, 0, OFFSET_PONE},
        };
    }

    @Test(dataProvider="sampleTimes")
    public void test_get_same(int y, int o, int d, int h, int m, int s, int n, ZoneOffset offset) {
        LocalDate localDate = LocalDate.of(y, o, d);
        LocalTime localTime = LocalTime.of(h, m, s, n);
        LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime);
        OffsetDateTime a = OffsetDateTime.of(localDateTime, offset);

        assertSame(a.getOffset(), offset);
        assertSame(a.toLocalDate(), localDate);
        assertSame(a.toLocalTime(), localTime);
        assertSame(a.toLocalDateTime(), localDateTime);
    }

    //-----------------------------------------------------------------------
    // withOffsetSameLocal()
    //-----------------------------------------------------------------------
    @Test
    public void test_withOffsetSameLocal() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withOffsetSameLocal(OFFSET_PTWO);
        assertSame(test.toLocalDateTime(), base.toLocalDateTime());
        assertSame(test.getOffset(), OFFSET_PTWO);
    }

    @Test
    public void test_withOffsetSameLocal_noChange() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withOffsetSameLocal(OFFSET_PONE);
        assertSame(test, base);
    }

    @Test
    public void test_withOffsetSameInstant_noChange() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withOffsetSameInstant(OFFSET_PONE);
        assertSame(test, base);
    }

    @Test
    public void test_withYear_noChange() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withYear(2008);
        assertSame(test, base);
    }

    @Test
    public void test_withMonth_noChange() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withMonth(6);
        assertSame(test, base);
    }

    @Test
    public void test_withDayOfMonth_noChange() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withDayOfMonth(30);
        assertSame(test, base);
    }

    @Test
    public void test_withDayOfYear_noChange() {
        OffsetDateTime t = TEST_2008_6_30_11_30_59_000000500.withDayOfYear(31 + 29 + 31 + 30 + 31 + 30);
        assertSame(t, TEST_2008_6_30_11_30_59_000000500);
    }

    @Test
    public void test_withHour_noChange() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withHour(11);
        assertSame(test, base);
    }

    @Test
    public void test_withMinute_noChange() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withMinute(30);
        assertSame(test, base);
    }

    @Test
    public void test_withSecond_noChange() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.withSecond(59);
        assertSame(test, base);
    }

    @Test
    public void test_withNanoOfSecond_noChange() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59, 1), OFFSET_PONE);
        OffsetDateTime test = base.withNano(1);
        assertSame(test, base);
    }

    @Test
    public void test_plus_Period_zero() {
        OffsetDateTime t = TEST_2008_6_30_11_30_59_000000500.plus(MockSimplePeriod.ZERO_DAYS);
        assertSame(t, TEST_2008_6_30_11_30_59_000000500);
    }

    @Test
    public void test_plusYears_zero() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusYears(0);
        assertSame(test, base);
    }

    @Test
    public void test_plusMonths_zero() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusMonths(0);
        assertSame(test, base);
    }

    @Test
    public void test_plusWeeks_zero() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusWeeks(0);
        assertSame(test, base);
    }

    @Test
    public void test_plusDays_zero() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusDays(0);
        assertSame(test, base);
    }

    @Test
    public void test_plusHours_zero() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusHours(0);
        assertSame(test, base);
    }

    @Test
    public void test_plusMinutes_zero() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusMinutes(0);
        assertSame(test, base);
    }

    @Test
    public void test_plusSeconds_zero() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusSeconds(0);
        assertSame(test, base);
    }

    @Test
    public void test_plusNanos_zero() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.plusNanos(0);
    }

    @Test
    public void test_minus_Period_zero() {
        OffsetDateTime t = TEST_2008_6_30_11_30_59_000000500.minus(MockSimplePeriod.ZERO_DAYS);
        assertSame(t, TEST_2008_6_30_11_30_59_000000500);
    }

    @Test
    public void test_minusYears_zero() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2007, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusYears(0);
        assertSame(test, base);
    }

    @Test
    public void test_minusMonths_zero() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusMonths(0);
        assertSame(test, base);
    }

    @Test
    public void test_minusWeeks_zero() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusWeeks(0);
        assertSame(test, base);
    }

    @Test
    public void test_minusDays_zero() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusDays(0);
        assertSame(test, base);
    }

    @Test
    public void test_minusHours_zero() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusHours(0);
        assertSame(test, base);
    }

    @Test
    public void test_minusMinutes_zero() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusMinutes(0);
        assertSame(test, base);
    }

    @Test
    public void test_minusSeconds_zero() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusSeconds(0);
        assertSame(test, base);
    }

    @Test
    public void test_minusNanos_zero() {
        OffsetDateTime base = OffsetDateTime.of(LocalDate.of(2008, 6, 30), LocalTime.of(11, 30, 59), OFFSET_PONE);
        OffsetDateTime test = base.minusNanos(0);
        assertSame(test, base);
    }

    @Test
    public void test_duration() {
        OffsetDateTime start = OffsetDateTime.MAX
                                .withOffsetSameLocal(ZoneOffset.ofHours(-17));
        OffsetDateTime end = OffsetDateTime.MAX;
        assertEquals(Duration.between(start, end), Duration.ofHours(1));
    }
}
