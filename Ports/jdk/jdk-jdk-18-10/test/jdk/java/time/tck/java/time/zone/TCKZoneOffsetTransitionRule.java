/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright (c) 2010-2012, Stephen Colebourne & Michael Nascimento Santos
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
package tck.java.time.zone;

import static org.testng.Assert.assertEquals;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneOffsetTransitionRule;
import java.time.zone.ZoneOffsetTransitionRule.TimeDefinition;

import org.testng.annotations.Test;
import tck.java.time.AbstractTCKTest;

/**
 * Test ZoneOffsetTransitionRule.
 */
@Test
public class TCKZoneOffsetTransitionRule extends AbstractTCKTest {

    private static final LocalTime TIME_0100 = LocalTime.of(1, 0);
    private static final ZoneOffset OFFSET_0200 = ZoneOffset.ofHours(2);
    private static final ZoneOffset OFFSET_0300 = ZoneOffset.ofHours(3);

    //-----------------------------------------------------------------------
    // factory
    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void test_factory_nullMonth() {
        ZoneOffsetTransitionRule.of(
                null, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_factory_nullTime() {
        ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, null, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_factory_nullTimeDefinition() {
        ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, null,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_factory_nullStandardOffset() {
        ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                null, OFFSET_0200, OFFSET_0300);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_factory_nullOffsetBefore() {
        ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, null, OFFSET_0300);
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_factory_nullOffsetAfter() {
        ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, null);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_factory_invalidDayOfMonthIndicator_tooSmall() {
        ZoneOffsetTransitionRule.of(
                Month.MARCH, -29, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_factory_invalidDayOfMonthIndicator_zero() {
        ZoneOffsetTransitionRule.of(
                Month.MARCH, 0, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_factory_invalidDayOfMonthIndicator_tooLarge() {
        ZoneOffsetTransitionRule.of(
                Month.MARCH, 32, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_factory_invalidMidnightFlag() {
        ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, true, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
    }

    @Test(expectedExceptions=IllegalArgumentException.class)
    public void test_factory_nonZeroTimeNanos() {
        ZoneOffsetTransitionRule.of(
            Month.MARCH, 20, DayOfWeek.SUNDAY, LocalTime.of(1, 2, 3, 400_000_000),
            false, TimeDefinition.WALL, OFFSET_0200, OFFSET_0200, OFFSET_0300);
    }

    //-----------------------------------------------------------------------
    // getters
    //-----------------------------------------------------------------------
    @Test
    public void test_getters_floatingWeek() throws Exception {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(test.getMonth(), Month.MARCH);
        assertEquals(test.getDayOfMonthIndicator(), 20);
        assertEquals(test.getDayOfWeek(), DayOfWeek.SUNDAY);
        assertEquals(test.getLocalTime(), TIME_0100);
        assertEquals(test.isMidnightEndOfDay(), false);
        assertEquals(test.getTimeDefinition(), TimeDefinition.WALL);
        assertEquals(test.getStandardOffset(), OFFSET_0200);
        assertEquals(test.getOffsetBefore(), OFFSET_0200);
        assertEquals(test.getOffsetAfter(), OFFSET_0300);
    }

    @Test
    public void test_getters_floatingWeekBackwards() throws Exception {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, -1, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(test.getMonth(), Month.MARCH);
        assertEquals(test.getDayOfMonthIndicator(), -1);
        assertEquals(test.getDayOfWeek(), DayOfWeek.SUNDAY);
        assertEquals(test.getLocalTime(), TIME_0100);
        assertEquals(test.isMidnightEndOfDay(), false);
        assertEquals(test.getTimeDefinition(), TimeDefinition.WALL);
        assertEquals(test.getStandardOffset(), OFFSET_0200);
        assertEquals(test.getOffsetBefore(), OFFSET_0200);
        assertEquals(test.getOffsetAfter(), OFFSET_0300);
    }

    @Test
    public void test_getters_fixedDate() throws Exception {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, null, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(test.getMonth(), Month.MARCH);
        assertEquals(test.getDayOfMonthIndicator(), 20);
        assertEquals(test.getDayOfWeek(), null);
        assertEquals(test.getLocalTime(), TIME_0100);
        assertEquals(test.isMidnightEndOfDay(), false);
        assertEquals(test.getTimeDefinition(), TimeDefinition.WALL);
        assertEquals(test.getStandardOffset(), OFFSET_0200);
        assertEquals(test.getOffsetBefore(), OFFSET_0200);
        assertEquals(test.getOffsetAfter(), OFFSET_0300);
    }


    //-----------------------------------------------------------------------
    // createTransition()
    //-----------------------------------------------------------------------
    @Test
    public void test_createTransition_floatingWeek_gap_notEndOfDay() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransition trans = ZoneOffsetTransition.of(
                LocalDateTime.of(2000, Month.MARCH, 26, 1, 0), OFFSET_0200, OFFSET_0300);
        assertEquals(test.createTransition(2000), trans);
    }

    @Test
    public void test_createTransition_floatingWeek_overlap_endOfDay() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, LocalTime.MIDNIGHT, true, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0300, OFFSET_0200);
        ZoneOffsetTransition trans = ZoneOffsetTransition.of(
                LocalDateTime.of(2000, Month.MARCH, 27, 0, 0), OFFSET_0300, OFFSET_0200);
        assertEquals(test.createTransition(2000), trans);
    }

    @Test
    public void test_createTransition_floatingWeekBackwards_last() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, -1, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransition trans = ZoneOffsetTransition.of(
                LocalDateTime.of(2000, Month.MARCH, 26, 1, 0), OFFSET_0200, OFFSET_0300);
        assertEquals(test.createTransition(2000), trans);
    }

    @Test
    public void test_createTransition_floatingWeekBackwards_seventhLast() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, -7, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransition trans = ZoneOffsetTransition.of(
                LocalDateTime.of(2000, Month.MARCH, 19, 1, 0), OFFSET_0200, OFFSET_0300);
        assertEquals(test.createTransition(2000), trans);
    }

    @Test
    public void test_createTransition_floatingWeekBackwards_secondLast() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, -2, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransition trans = ZoneOffsetTransition.of(
                LocalDateTime.of(2000, Month.MARCH, 26, 1, 0), OFFSET_0200, OFFSET_0300);
        assertEquals(test.createTransition(2000), trans);
    }

    @Test
    public void test_createTransition_fixedDate() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, null, TIME_0100, false, TimeDefinition.STANDARD,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransition trans = ZoneOffsetTransition.of(
                LocalDateTime.of(2000, Month.MARCH, 20, 1, 0), OFFSET_0200, OFFSET_0300);
        assertEquals(test.createTransition(2000), trans);
    }

    //-----------------------------------------------------------------------
    // equals()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals_monthDifferent() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.APRIL, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(b.equals(b), true);
    }

    @Test
    public void test_equals_dayOfMonthDifferent() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 21, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(b.equals(b), true);
    }

    @Test
    public void test_equals_dayOfWeekDifferent() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SATURDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(b.equals(b), true);
    }

    @Test
    public void test_equals_dayOfWeekDifferentNull() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, null, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(b.equals(b), true);
    }

    @Test
    public void test_equals_localTimeDifferent() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, LocalTime.MIDNIGHT, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(b.equals(b), true);
    }

    @Test
    public void test_equals_endOfDayDifferent() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, LocalTime.MIDNIGHT, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, LocalTime.MIDNIGHT, true, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(b.equals(b), true);
    }

    @Test
    public void test_equals_timeDefinitionDifferent() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.STANDARD,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(b.equals(b), true);
    }

    @Test
    public void test_equals_standardOffsetDifferent() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0300, OFFSET_0200, OFFSET_0300);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(b.equals(b), true);
    }

    @Test
    public void test_equals_offsetBeforeDifferent() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0300, OFFSET_0300);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(b.equals(b), true);
    }

    @Test
    public void test_equals_offsetAfterDifferent() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0200);
        assertEquals(a.equals(a), true);
        assertEquals(a.equals(b), false);
        assertEquals(b.equals(a), false);
        assertEquals(b.equals(b), true);
    }

    @Test
    public void test_equals_string_false() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.equals("TZDB"), false);
    }

    @Test
    public void test_equals_null_false() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.equals(null), false);
    }

    //-----------------------------------------------------------------------
    // hashCode()
    //-----------------------------------------------------------------------
    @Test
    public void test_hashCode_floatingWeek_gap_notEndOfDay() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void test_hashCode_floatingWeek_overlap_endOfDay_nullDayOfWeek() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.OCTOBER, 20, null, LocalTime.MIDNIGHT, true, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0300, OFFSET_0200);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.OCTOBER, 20, null, LocalTime.MIDNIGHT, true, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0300, OFFSET_0200);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void test_hashCode_floatingWeekBackwards() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, -1, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, -1, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void test_hashCode_fixedDate() {
        ZoneOffsetTransitionRule a = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, null, TIME_0100, false, TimeDefinition.STANDARD,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        ZoneOffsetTransitionRule b = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, null, TIME_0100, false, TimeDefinition.STANDARD,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(a.hashCode(), b.hashCode());
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @Test
    public void test_toString_floatingWeek_gap_notEndOfDay() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(test.toString(), "TransitionRule[Gap +02:00 to +03:00, SUNDAY on or after MARCH 20 at 01:00 WALL, standard offset +02:00]");
    }

    @Test
    public void test_toString_floatingWeek_overlap_endOfDay() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.OCTOBER, 20, DayOfWeek.SUNDAY, LocalTime.MIDNIGHT, true, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0300, OFFSET_0200);
        assertEquals(test.toString(), "TransitionRule[Overlap +03:00 to +02:00, SUNDAY on or after OCTOBER 20 at 24:00 WALL, standard offset +02:00]");
    }

    @Test
    public void test_toString_floatingWeekBackwards_last() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, -1, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(test.toString(), "TransitionRule[Gap +02:00 to +03:00, SUNDAY on or before last day of MARCH at 01:00 WALL, standard offset +02:00]");
    }

    @Test
    public void test_toString_floatingWeekBackwards_secondLast() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, -2, DayOfWeek.SUNDAY, TIME_0100, false, TimeDefinition.WALL,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(test.toString(), "TransitionRule[Gap +02:00 to +03:00, SUNDAY on or before last day minus 1 of MARCH at 01:00 WALL, standard offset +02:00]");
    }

    @Test
    public void test_toString_fixedDate() {
        ZoneOffsetTransitionRule test = ZoneOffsetTransitionRule.of(
                Month.MARCH, 20, null, TIME_0100, false, TimeDefinition.STANDARD,
                OFFSET_0200, OFFSET_0200, OFFSET_0300);
        assertEquals(test.toString(), "TransitionRule[Gap +02:00 to +03:00, MARCH 20 at 01:00 STANDARD, standard offset +02:00]");
    }

}
