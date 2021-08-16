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
package tck.java.time.zone;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneOffsetTransitionRule;
import java.time.zone.ZoneOffsetTransitionRule.TimeDefinition;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.testng.annotations.Test;

/**
 * Test ZoneRules.
 */
@Test
public class TCKZoneRules {

    private static final ZoneOffset OFFSET_ZERO = ZoneOffset.ofHours(0);
    private static final ZoneOffset OFFSET_PONE = ZoneOffset.ofHours(1);
    private static final ZoneOffset OFFSET_PTWO = ZoneOffset.ofHours(2);
    public static final String LATEST_TZDB = "2009b";
    private static final int OVERLAP = 2;
    private static final int GAP = 0;



    //-----------------------------------------------------------------------
    // Europe/London
    //-----------------------------------------------------------------------
    private ZoneRules europeLondon() {
        return ZoneId.of("Europe/London").getRules();
    }

    @Test
    public void test_London() {
        ZoneRules test = europeLondon();
        assertEquals(test.isFixedOffset(), false);
    }

    @Test
    public void test_London_preTimeZones() {
        ZoneRules test = europeLondon();
        ZonedDateTime old = createZDT(1800, 1, 1, ZoneOffset.UTC);
        Instant instant = old.toInstant();
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(0, -1, -15);
        assertEquals(test.getOffset(instant), offset);
        checkOffset(test, old.toLocalDateTime(), offset, 1);
        assertEquals(test.getStandardOffset(instant), offset);
        assertEquals(test.getDaylightSavings(instant), Duration.ZERO);
        assertEquals(test.isDaylightSavings(instant), false);
    }

    @Test
    public void test_London_getOffset() {
        ZoneRules test = europeLondon();
        assertEquals(test.getOffset(createInstant(2008, 1, 1, ZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 2, 1, ZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 1, ZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 4, 1, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 5, 1, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 6, 1, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 7, 1, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 8, 1, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 9, 1, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 1, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 11, 1, ZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 12, 1, ZoneOffset.UTC)), OFFSET_ZERO);
    }

    @Test
    public void test_London_getOffset_toDST() {
        ZoneRules test = europeLondon();
        assertEquals(test.getOffset(createInstant(2008, 3, 24, ZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 25, ZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 26, ZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 27, ZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 28, ZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 29, ZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 30, ZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 31, ZoneOffset.UTC)), OFFSET_PONE);
        // cutover at 01:00Z
        assertEquals(test.getOffset(createInstant(2008, 3, 30, 0, 59, 59, 999999999, ZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 3, 30, 1, 0, 0, 0, ZoneOffset.UTC)), OFFSET_PONE);
    }

    @Test
    public void test_London_getOffset_fromDST() {
        ZoneRules test = europeLondon();
        assertEquals(test.getOffset(createInstant(2008, 10, 24, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 25, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 26, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 27, ZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 10, 28, ZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 10, 29, ZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 10, 30, ZoneOffset.UTC)), OFFSET_ZERO);
        assertEquals(test.getOffset(createInstant(2008, 10, 31, ZoneOffset.UTC)), OFFSET_ZERO);
        // cutover at 01:00Z
        assertEquals(test.getOffset(createInstant(2008, 10, 26, 0, 59, 59, 999999999, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 26, 1, 0, 0, 0, ZoneOffset.UTC)), OFFSET_ZERO);
    }

    @Test
    public void test_London_getOffsetInfo() {
        ZoneRules test = europeLondon();
        checkOffset(test, createLDT(2008, 1, 1), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 2, 1), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 1), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 4, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 5, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 6, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 7, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 8, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 9, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 10, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 11, 1), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 12, 1), OFFSET_ZERO, 1);
    }

    @Test
    public void test_London_getOffsetInfo_toDST() {
        ZoneRules test = europeLondon();
        checkOffset(test, createLDT(2008, 3, 24), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 25), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 26), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 27), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 28), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 29), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 30), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 3, 31), OFFSET_PONE, 1);
        // cutover at 01:00Z
        checkOffset(test, LocalDateTime.of(2008, 3, 30, 0, 59, 59, 999999999), OFFSET_ZERO, 1);
        checkOffset(test, LocalDateTime.of(2008, 3, 30, 2, 0, 0, 0), OFFSET_PONE, 1);
    }

    @Test
    public void test_London_getOffsetInfo_fromDST() {
        ZoneRules test = europeLondon();
        checkOffset(test, createLDT(2008, 10, 24), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 10, 25), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 10, 26), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 10, 27), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 10, 28), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 10, 29), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 10, 30), OFFSET_ZERO, 1);
        checkOffset(test, createLDT(2008, 10, 31), OFFSET_ZERO, 1);
        // cutover at 01:00Z
        checkOffset(test, LocalDateTime.of(2008, 10, 26, 0, 59, 59, 999999999), OFFSET_PONE, 1);
        checkOffset(test, LocalDateTime.of(2008, 10, 26, 2, 0, 0, 0), OFFSET_ZERO, 1);
    }

    @Test
    public void test_London_getOffsetInfo_gap() {
        ZoneRules test = europeLondon();
        final LocalDateTime dateTime = LocalDateTime.of(2008, 3, 30, 1, 0, 0, 0);
        ZoneOffsetTransition trans = checkOffset(test, dateTime, OFFSET_ZERO, GAP);
        assertEquals(trans.isGap(), true);
        assertEquals(trans.isOverlap(), false);
        assertEquals(trans.getOffsetBefore(), OFFSET_ZERO);
        assertEquals(trans.getOffsetAfter(), OFFSET_PONE);
        assertEquals(trans.getInstant(), createInstant(2008, 3, 30, 1, 0, ZoneOffset.UTC));
        assertEquals(trans.getDateTimeBefore(), LocalDateTime.of(2008, 3, 30, 1, 0));
        assertEquals(trans.getDateTimeAfter(), LocalDateTime.of(2008, 3, 30, 2, 0));
        assertEquals(trans.isValidOffset(OFFSET_ZERO), false);
        assertEquals(trans.isValidOffset(OFFSET_PONE), false);
        assertEquals(trans.isValidOffset(OFFSET_PTWO), false);
        assertEquals(trans.toString(), "Transition[Gap at 2008-03-30T01:00Z to +01:00]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(OFFSET_ZERO));
        assertTrue(trans.equals(trans));

        final ZoneOffsetTransition otherTrans = test.getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));
        assertEquals(trans.hashCode(), otherTrans.hashCode());
    }

    @Test
    public void test_London_getOffsetInfo_overlap() {
        ZoneRules test = europeLondon();
        final LocalDateTime dateTime = LocalDateTime.of(2008, 10, 26, 1, 0, 0, 0);
        ZoneOffsetTransition trans = checkOffset(test, dateTime, OFFSET_PONE, OVERLAP);
        assertEquals(trans.isGap(), false);
        assertEquals(trans.isOverlap(), true);
        assertEquals(trans.getOffsetBefore(), OFFSET_PONE);
        assertEquals(trans.getOffsetAfter(), OFFSET_ZERO);
        assertEquals(trans.getInstant(), createInstant(2008, 10, 26, 1, 0, ZoneOffset.UTC));
        assertEquals(trans.getDateTimeBefore(), LocalDateTime.of(2008, 10, 26, 2, 0));
        assertEquals(trans.getDateTimeAfter(), LocalDateTime.of(2008, 10, 26, 1, 0));
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(-1)), false);
        assertEquals(trans.isValidOffset(OFFSET_ZERO), true);
        assertEquals(trans.isValidOffset(OFFSET_PONE), true);
        assertEquals(trans.isValidOffset(OFFSET_PTWO), false);
        assertEquals(trans.toString(), "Transition[Overlap at 2008-10-26T02:00+01:00 to Z]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(OFFSET_PONE));
        assertTrue(trans.equals(trans));

        final ZoneOffsetTransition otherTrans = test.getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));
        assertEquals(trans.hashCode(), otherTrans.hashCode());
    }

    @Test
    public void test_London_getStandardOffset() {
        ZoneRules test = europeLondon();
        ZonedDateTime zdt = createZDT(1840, 1, 1, ZoneOffset.UTC);
        while (zdt.getYear() < 2010) {
            Instant instant = zdt.toInstant();
            if (zdt.getYear() < 1848) {
                assertEquals(test.getStandardOffset(instant), ZoneOffset.ofHoursMinutesSeconds(0, -1, -15));
            } else if (zdt.getYear() >= 1969 && zdt.getYear() < 1972) {
                assertEquals(test.getStandardOffset(instant), OFFSET_PONE);
            } else {
                assertEquals(test.getStandardOffset(instant), OFFSET_ZERO);
            }
            zdt = zdt.plusMonths(6);
        }
    }

    @Test
    public void test_London_getTransitions() {
        ZoneRules test = europeLondon();
        List<ZoneOffsetTransition> trans = test.getTransitions();

        ZoneOffsetTransition first = trans.get(0);
        assertEquals(first.getDateTimeBefore(), LocalDateTime.of(1847, 12, 1, 0, 0));
        assertEquals(first.getOffsetBefore(), ZoneOffset.ofHoursMinutesSeconds(0, -1, -15));
        assertEquals(first.getOffsetAfter(), OFFSET_ZERO);

        ZoneOffsetTransition spring1916 = trans.get(1);
        assertEquals(spring1916.getDateTimeBefore(), LocalDateTime.of(1916, 5, 21, 2, 0));
        assertEquals(spring1916.getOffsetBefore(), OFFSET_ZERO);
        assertEquals(spring1916.getOffsetAfter(), OFFSET_PONE);

        ZoneOffsetTransition autumn1916 = trans.get(2);
        assertEquals(autumn1916.getDateTimeBefore(), LocalDateTime.of(1916, 10, 1, 3, 0));
        assertEquals(autumn1916.getOffsetBefore(), OFFSET_PONE);
        assertEquals(autumn1916.getOffsetAfter(), OFFSET_ZERO);

        ZoneOffsetTransition zot = null;
        Iterator<ZoneOffsetTransition> it = trans.iterator();
        while (it.hasNext()) {
            zot = it.next();
            if (zot.getDateTimeBefore().getYear() == 1990) {
                break;
            }
        }
        assertEquals(zot.getDateTimeBefore(), LocalDateTime.of(1990, 3, 25, 1, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_ZERO);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), LocalDateTime.of(1990, 10, 28, 2, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_PONE);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), LocalDateTime.of(1991, 3, 31, 1, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_ZERO);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), LocalDateTime.of(1991, 10, 27, 2, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_PONE);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), LocalDateTime.of(1992, 3, 29, 1, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_ZERO);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), LocalDateTime.of(1992, 10, 25, 2, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_PONE);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), LocalDateTime.of(1993, 3, 28, 1, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_ZERO);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), LocalDateTime.of(1993, 10, 24, 2, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_PONE);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), LocalDateTime.of(1994, 3, 27, 1, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_ZERO);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), LocalDateTime.of(1994, 10, 23, 2, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_PONE);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), LocalDateTime.of(1995, 3, 26, 1, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_ZERO);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), LocalDateTime.of(1995, 10, 22, 2, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_PONE);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), LocalDateTime.of(1996, 3, 31, 1, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_ZERO);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), LocalDateTime.of(1996, 10, 27, 2, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_PONE);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), LocalDateTime.of(1997, 3, 30, 1, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_ZERO);
        zot = it.next();
        assertEquals(zot.getDateTimeBefore(), LocalDateTime.of(1997, 10, 26, 2, 0));
        assertEquals(zot.getOffsetBefore(), OFFSET_PONE);
        assertEquals(it.hasNext(), false);
    }

    @Test
    public void test_London_getTransitionRules() {
        ZoneRules test = europeLondon();
        List<ZoneOffsetTransitionRule> rules = test.getTransitionRules();
        assertEquals(rules.size(), 2);

        ZoneOffsetTransitionRule in = rules.get(0);
        assertEquals(in.getMonth(), Month.MARCH);
        assertEquals(in.getDayOfMonthIndicator(), 25);  // optimized from -1
        assertEquals(in.getDayOfWeek(), DayOfWeek.SUNDAY);
        assertEquals(in.getLocalTime(), LocalTime.of(1, 0));
        assertEquals(in.getTimeDefinition(), TimeDefinition.UTC);
        assertEquals(in.getStandardOffset(), OFFSET_ZERO);
        assertEquals(in.getOffsetBefore(), OFFSET_ZERO);
        assertEquals(in.getOffsetAfter(), OFFSET_PONE);

        ZoneOffsetTransitionRule out = rules.get(1);
        assertEquals(out.getMonth(), Month.OCTOBER);
        assertEquals(out.getDayOfMonthIndicator(), 25);  // optimized from -1
        assertEquals(out.getDayOfWeek(), DayOfWeek.SUNDAY);
        assertEquals(out.getLocalTime(), LocalTime.of(1, 0));
        assertEquals(out.getTimeDefinition(), TimeDefinition.UTC);
        assertEquals(out.getStandardOffset(), OFFSET_ZERO);
        assertEquals(out.getOffsetBefore(), OFFSET_PONE);
        assertEquals(out.getOffsetAfter(), OFFSET_ZERO);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_London_nextTransition_historic() {
        ZoneRules test = europeLondon();
        List<ZoneOffsetTransition> trans = test.getTransitions();

        ZoneOffsetTransition first = trans.get(0);
        assertEquals(test.nextTransition(first.getInstant().minusNanos(1)), first);

        for (int i = 0; i < trans.size() - 1; i++) {
            ZoneOffsetTransition cur = trans.get(i);
            ZoneOffsetTransition next = trans.get(i + 1);

            assertEquals(test.nextTransition(cur.getInstant()), next);
            assertEquals(test.nextTransition(next.getInstant().minusNanos(1)), next);
        }
    }

    @Test
    public void test_London_nextTransition_rulesBased() {
        ZoneRules test = europeLondon();
        List<ZoneOffsetTransitionRule> rules = test.getTransitionRules();
        List<ZoneOffsetTransition> trans = test.getTransitions();

        ZoneOffsetTransition last = trans.get(trans.size() - 1);
        assertEquals(test.nextTransition(last.getInstant()), rules.get(0).createTransition(1998));

        for (int year = 1998; year < 2010; year++) {
            ZoneOffsetTransition a = rules.get(0).createTransition(year);
            ZoneOffsetTransition b = rules.get(1).createTransition(year);
            ZoneOffsetTransition c = rules.get(0).createTransition(year + 1);

            assertEquals(test.nextTransition(a.getInstant()), b);
            assertEquals(test.nextTransition(b.getInstant().minusNanos(1)), b);

            assertEquals(test.nextTransition(b.getInstant()), c);
            assertEquals(test.nextTransition(c.getInstant().minusNanos(1)), c);
        }
    }

    @Test
    public void test_London_nextTransition_lastYear() {
        ZoneRules test = europeLondon();
        List<ZoneOffsetTransitionRule> rules = test.getTransitionRules();
        ZoneOffsetTransition zot = rules.get(1).createTransition(Year.MAX_VALUE);
        assertEquals(test.nextTransition(zot.getInstant()), null);
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_London_previousTransition_historic() {
        ZoneRules test = europeLondon();
        List<ZoneOffsetTransition> trans = test.getTransitions();

        ZoneOffsetTransition first = trans.get(0);
        assertEquals(test.previousTransition(first.getInstant()), null);
        assertEquals(test.previousTransition(first.getInstant().minusNanos(1)), null);

        for (int i = 0; i < trans.size() - 1; i++) {
            ZoneOffsetTransition prev = trans.get(i);
            ZoneOffsetTransition cur = trans.get(i + 1);

            assertEquals(test.previousTransition(cur.getInstant()), prev);
            assertEquals(test.previousTransition(prev.getInstant().plusSeconds(1)), prev);
            assertEquals(test.previousTransition(prev.getInstant().plusNanos(1)), prev);
        }
    }

    @Test
    public void test_London_previousTransition_rulesBased() {
        ZoneRules test = europeLondon();
        List<ZoneOffsetTransitionRule> rules = test.getTransitionRules();
        List<ZoneOffsetTransition> trans = test.getTransitions();

        ZoneOffsetTransition last = trans.get(trans.size() - 1);
        assertEquals(test.previousTransition(last.getInstant().plusSeconds(1)), last);
        assertEquals(test.previousTransition(last.getInstant().plusNanos(1)), last);

        // Jan 1st of year between transitions and rules
        ZonedDateTime odt = ZonedDateTime.ofInstant(last.getInstant(), last.getOffsetAfter());
        odt = odt.withDayOfYear(1).plusYears(1).with(LocalTime.MIDNIGHT);
        assertEquals(test.previousTransition(odt.toInstant()), last);

        // later years
        for (int year = 1998; year < 2010; year++) {
            ZoneOffsetTransition a = rules.get(0).createTransition(year);
            ZoneOffsetTransition b = rules.get(1).createTransition(year);
            ZoneOffsetTransition c = rules.get(0).createTransition(year + 1);

            assertEquals(test.previousTransition(c.getInstant()), b);
            assertEquals(test.previousTransition(b.getInstant().plusSeconds(1)), b);
            assertEquals(test.previousTransition(b.getInstant().plusNanos(1)), b);

            assertEquals(test.previousTransition(b.getInstant()), a);
            assertEquals(test.previousTransition(a.getInstant().plusSeconds(1)), a);
            assertEquals(test.previousTransition(a.getInstant().plusNanos(1)), a);
        }
    }

    //-----------------------------------------------------------------------
    // Europe/Paris
    //-----------------------------------------------------------------------
    private ZoneRules europeParis() {
        return ZoneId.of("Europe/Paris").getRules();
    }

    @Test
    public void test_Paris() {
        ZoneRules test = europeParis();
        assertEquals(test.isFixedOffset(), false);
    }

    @Test
    public void test_Paris_preTimeZones() {
        ZoneRules test = europeParis();
        ZonedDateTime old = createZDT(1800, 1, 1, ZoneOffset.UTC);
        Instant instant = old.toInstant();
        ZoneOffset offset = ZoneOffset.ofHoursMinutesSeconds(0, 9, 21);
        assertEquals(test.getOffset(instant), offset);
        checkOffset(test, old.toLocalDateTime(), offset, 1);
        assertEquals(test.getStandardOffset(instant), offset);
        assertEquals(test.getDaylightSavings(instant), Duration.ZERO);
        assertEquals(test.isDaylightSavings(instant), false);
    }

    @Test
    public void test_Paris_getOffset() {
        ZoneRules test = europeParis();
        assertEquals(test.getOffset(createInstant(2008, 1, 1, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 2, 1, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 3, 1, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 4, 1, ZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 5, 1, ZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 6, 1, ZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 7, 1, ZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 8, 1, ZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 9, 1, ZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 10, 1, ZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 11, 1, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 12, 1, ZoneOffset.UTC)), OFFSET_PONE);
    }

    @Test
    public void test_Paris_getOffset_toDST() {
        ZoneRules test = europeParis();
        assertEquals(test.getOffset(createInstant(2008, 3, 24, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 3, 25, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 3, 26, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 3, 27, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 3, 28, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 3, 29, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 3, 30, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 3, 31, ZoneOffset.UTC)), OFFSET_PTWO);
        // cutover at 01:00Z
        assertEquals(test.getOffset(createInstant(2008, 3, 30, 0, 59, 59, 999999999, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 3, 30, 1, 0, 0, 0, ZoneOffset.UTC)), OFFSET_PTWO);
    }

    @Test
    public void test_Paris_getOffset_fromDST() {
        ZoneRules test = europeParis();
        assertEquals(test.getOffset(createInstant(2008, 10, 24, ZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 10, 25, ZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 10, 26, ZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 10, 27, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 28, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 29, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 30, ZoneOffset.UTC)), OFFSET_PONE);
        assertEquals(test.getOffset(createInstant(2008, 10, 31, ZoneOffset.UTC)), OFFSET_PONE);
        // cutover at 01:00Z
        assertEquals(test.getOffset(createInstant(2008, 10, 26, 0, 59, 59, 999999999, ZoneOffset.UTC)), OFFSET_PTWO);
        assertEquals(test.getOffset(createInstant(2008, 10, 26, 1, 0, 0, 0, ZoneOffset.UTC)), OFFSET_PONE);
    }

    @Test
    public void test_Paris_getOffsetInfo() {
        ZoneRules test = europeParis();
        checkOffset(test, createLDT(2008, 1, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 2, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 3, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 4, 1), OFFSET_PTWO, 1);
        checkOffset(test, createLDT(2008, 5, 1), OFFSET_PTWO, 1);
        checkOffset(test, createLDT(2008, 6, 1), OFFSET_PTWO, 1);
        checkOffset(test, createLDT(2008, 7, 1), OFFSET_PTWO, 1);
        checkOffset(test, createLDT(2008, 8, 1), OFFSET_PTWO, 1);
        checkOffset(test, createLDT(2008, 9, 1), OFFSET_PTWO, 1);
        checkOffset(test, createLDT(2008, 10, 1), OFFSET_PTWO, 1);
        checkOffset(test, createLDT(2008, 11, 1), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 12, 1), OFFSET_PONE, 1);
    }

    @Test
    public void test_Paris_getOffsetInfo_toDST() {
        ZoneRules test = europeParis();
        checkOffset(test, createLDT(2008, 3, 24), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 3, 25), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 3, 26), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 3, 27), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 3, 28), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 3, 29), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 3, 30), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 3, 31), OFFSET_PTWO, 1);
        // cutover at 01:00Z which is 02:00+01:00(local Paris time)
        checkOffset(test, LocalDateTime.of(2008, 3, 30, 1, 59, 59, 999999999), OFFSET_PONE, 1);
        checkOffset(test, LocalDateTime.of(2008, 3, 30, 3, 0, 0, 0), OFFSET_PTWO, 1);
    }

    @Test
    public void test_Paris_getOffsetInfo_fromDST() {
        ZoneRules test = europeParis();
        checkOffset(test, createLDT(2008, 10, 24), OFFSET_PTWO, 1);
        checkOffset(test, createLDT(2008, 10, 25), OFFSET_PTWO, 1);
        checkOffset(test, createLDT(2008, 10, 26), OFFSET_PTWO, 1);
        checkOffset(test, createLDT(2008, 10, 27), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 10, 28), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 10, 29), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 10, 30), OFFSET_PONE, 1);
        checkOffset(test, createLDT(2008, 10, 31), OFFSET_PONE, 1);
        // cutover at 01:00Z which is 02:00+01:00(local Paris time)
        checkOffset(test, LocalDateTime.of(2008, 10, 26, 1, 59, 59, 999999999), OFFSET_PTWO, 1);
        checkOffset(test, LocalDateTime.of(2008, 10, 26, 3, 0, 0, 0), OFFSET_PONE, 1);
    }

    @Test
    public void test_Paris_getOffsetInfo_gap() {
        ZoneRules test = europeParis();
        final LocalDateTime dateTime = LocalDateTime.of(2008, 3, 30, 2, 0, 0, 0);
        ZoneOffsetTransition trans = checkOffset(test, dateTime, OFFSET_PONE, GAP);
        assertEquals(trans.isGap(), true);
        assertEquals(trans.isOverlap(), false);
        assertEquals(trans.getOffsetBefore(), OFFSET_PONE);
        assertEquals(trans.getOffsetAfter(), OFFSET_PTWO);
        assertEquals(trans.getInstant(), createInstant(2008, 3, 30, 1, 0, ZoneOffset.UTC));
        assertEquals(trans.isValidOffset(OFFSET_ZERO), false);
        assertEquals(trans.isValidOffset(OFFSET_PONE), false);
        assertEquals(trans.isValidOffset(OFFSET_PTWO), false);
        assertEquals(trans.toString(), "Transition[Gap at 2008-03-30T02:00+01:00 to +02:00]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(OFFSET_PONE));
        assertTrue(trans.equals(trans));

        final ZoneOffsetTransition otherTrans = test.getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));
        assertEquals(trans.hashCode(), otherTrans.hashCode());
    }

    @Test
    public void test_Paris_getOffsetInfo_overlap() {
        ZoneRules test = europeParis();
        final LocalDateTime dateTime = LocalDateTime.of(2008, 10, 26, 2, 0, 0, 0);
        ZoneOffsetTransition trans = checkOffset(test, dateTime, OFFSET_PTWO, OVERLAP);
        assertEquals(trans.isGap(), false);
        assertEquals(trans.isOverlap(), true);
        assertEquals(trans.getOffsetBefore(), OFFSET_PTWO);
        assertEquals(trans.getOffsetAfter(), OFFSET_PONE);
        assertEquals(trans.getInstant(), createInstant(2008, 10, 26, 1, 0, ZoneOffset.UTC));
        assertEquals(trans.isValidOffset(OFFSET_ZERO), false);
        assertEquals(trans.isValidOffset(OFFSET_PONE), true);
        assertEquals(trans.isValidOffset(OFFSET_PTWO), true);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(3)), false);
        assertEquals(trans.toString(), "Transition[Overlap at 2008-10-26T03:00+02:00 to +01:00]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(OFFSET_PTWO));
        assertTrue(trans.equals(trans));

        final ZoneOffsetTransition otherTrans = test.getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));
        assertEquals(trans.hashCode(), otherTrans.hashCode());
    }

    @Test
    public void test_Paris_getStandardOffset() {
        ZoneRules test = europeParis();
        ZonedDateTime zdt = createZDT(1840, 1, 1, ZoneOffset.UTC);
        while (zdt.getYear() < 2010) {
            Instant instant = zdt.toInstant();
            if (zdt.toLocalDate().isBefore(LocalDate.of(1911, 3, 11))) {
                assertEquals(test.getStandardOffset(instant), ZoneOffset.ofHoursMinutesSeconds(0, 9, 21));
            } else if (zdt.toLocalDate().isBefore(LocalDate.of(1940, 6, 14))) {
                assertEquals(test.getStandardOffset(instant), OFFSET_ZERO);
            } else if (zdt.toLocalDate().isBefore(LocalDate.of(1944, 8, 25))) {
                assertEquals(test.getStandardOffset(instant), OFFSET_PONE);
            } else if (zdt.toLocalDate().isBefore(LocalDate.of(1945, 9, 16))) {
                assertEquals(test.getStandardOffset(instant), OFFSET_ZERO);
            } else {
                assertEquals(test.getStandardOffset(instant), OFFSET_PONE);
            }
            zdt = zdt.plusMonths(6);
        }
    }

    //-----------------------------------------------------------------------
    // America/New_York
    //-----------------------------------------------------------------------
    private ZoneRules americaNewYork() {
        return ZoneId.of("America/New_York").getRules();
    }

    @Test
    public void test_NewYork() {
        ZoneRules test = americaNewYork();
        assertEquals(test.isFixedOffset(), false);
    }

    @Test
    public void test_NewYork_preTimeZones() {
        ZoneRules test = americaNewYork();
        ZonedDateTime old = createZDT(1800, 1, 1, ZoneOffset.UTC);
        Instant instant = old.toInstant();
        ZoneOffset offset = ZoneOffset.of("-04:56:02");
        assertEquals(test.getOffset(instant), offset);
        checkOffset(test, old.toLocalDateTime(), offset, 1);
        assertEquals(test.getStandardOffset(instant), offset);
        assertEquals(test.getDaylightSavings(instant), Duration.ZERO);
        assertEquals(test.isDaylightSavings(instant), false);
    }

    @Test
    public void test_NewYork_getOffset() {
        ZoneRules test = americaNewYork();
        ZoneOffset offset = ZoneOffset.ofHours(-5);
        assertEquals(test.getOffset(createInstant(2008, 1, 1, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 2, 1, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 3, 1, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 4, 1, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 5, 1, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 6, 1, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 7, 1, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 8, 1, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 9, 1, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 10, 1, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 11, 1, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 12, 1, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 1, 28, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 2, 28, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 3, 28, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 4, 28, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 5, 28, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 6, 28, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 7, 28, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 8, 28, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 9, 28, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 10, 28, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 11, 28, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 12, 28, offset)), ZoneOffset.ofHours(-5));
    }

    @Test
    public void test_NewYork_getOffset_toDST() {
        ZoneRules test = americaNewYork();
        ZoneOffset offset = ZoneOffset.ofHours(-5);
        assertEquals(test.getOffset(createInstant(2008, 3, 8, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 3, 9, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 3, 10, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 3, 11, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 3, 12, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 3, 13, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 3, 14, offset)), ZoneOffset.ofHours(-4));
        // cutover at 02:00 local
        assertEquals(test.getOffset(createInstant(2008, 3, 9, 1, 59, 59, 999999999, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 3, 9, 2, 0, 0, 0, offset)), ZoneOffset.ofHours(-4));
    }

    @Test
    public void test_NewYork_getOffset_fromDST() {
        ZoneRules test = americaNewYork();
        ZoneOffset offset = ZoneOffset.ofHours(-4);
        assertEquals(test.getOffset(createInstant(2008, 11, 1, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 11, 2, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 11, 3, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 11, 4, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 11, 5, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 11, 6, offset)), ZoneOffset.ofHours(-5));
        assertEquals(test.getOffset(createInstant(2008, 11, 7, offset)), ZoneOffset.ofHours(-5));
        // cutover at 02:00 local
        assertEquals(test.getOffset(createInstant(2008, 11, 2, 1, 59, 59, 999999999, offset)), ZoneOffset.ofHours(-4));
        assertEquals(test.getOffset(createInstant(2008, 11, 2, 2, 0, 0, 0, offset)), ZoneOffset.ofHours(-5));
    }

    @Test
    public void test_NewYork_getOffsetInfo() {
        ZoneRules test = americaNewYork();
        checkOffset(test, createLDT(2008, 1, 1), ZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 2, 1), ZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 3, 1), ZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 4, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 5, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 6, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 7, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 8, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 9, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 10, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 11, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 12, 1), ZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 1, 28), ZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 2, 28), ZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 3, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 4, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 5, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 6, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 7, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 8, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 9, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 10, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 11, 28), ZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 12, 28), ZoneOffset.ofHours(-5), 1);
    }

    @Test
    public void test_NewYork_getOffsetInfo_toDST() {
        ZoneRules test = americaNewYork();
        checkOffset(test, createLDT(2008, 3, 8), ZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 3, 9), ZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 3, 10), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 3, 11), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 3, 12), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 3, 13), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 3, 14), ZoneOffset.ofHours(-4), 1);
        // cutover at 02:00 local
        checkOffset(test, LocalDateTime.of(2008, 3, 9, 1, 59, 59, 999999999), ZoneOffset.ofHours(-5), 1);
        checkOffset(test, LocalDateTime.of(2008, 3, 9, 3, 0, 0, 0), ZoneOffset.ofHours(-4), 1);
    }

    @Test
    public void test_NewYork_getOffsetInfo_fromDST() {
        ZoneRules test = americaNewYork();
        checkOffset(test, createLDT(2008, 11, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 11, 2), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, createLDT(2008, 11, 3), ZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 11, 4), ZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 11, 5), ZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 11, 6), ZoneOffset.ofHours(-5), 1);
        checkOffset(test, createLDT(2008, 11, 7), ZoneOffset.ofHours(-5), 1);
        // cutover at 02:00 local
        checkOffset(test, LocalDateTime.of(2008, 11, 2, 0, 59, 59, 999999999), ZoneOffset.ofHours(-4), 1);
        checkOffset(test, LocalDateTime.of(2008, 11, 2, 2, 0, 0, 0), ZoneOffset.ofHours(-5), 1);
    }

    @Test
    public void test_NewYork_getOffsetInfo_gap() {
        ZoneRules test = americaNewYork();
        final LocalDateTime dateTime = LocalDateTime.of(2008, 3, 9, 2, 0, 0, 0);
        ZoneOffsetTransition trans = checkOffset(test, dateTime, ZoneOffset.ofHours(-5), GAP);
        assertEquals(trans.isGap(), true);
        assertEquals(trans.isOverlap(), false);
        assertEquals(trans.getOffsetBefore(), ZoneOffset.ofHours(-5));
        assertEquals(trans.getOffsetAfter(), ZoneOffset.ofHours(-4));
        assertEquals(trans.getInstant(), createInstant(2008, 3, 9, 2, 0, ZoneOffset.ofHours(-5)));
        assertEquals(trans.isValidOffset(OFFSET_PTWO), false);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(-5)), false);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(-4)), false);
        assertEquals(trans.toString(), "Transition[Gap at 2008-03-09T02:00-05:00 to -04:00]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(ZoneOffset.ofHours(-5)));
        assertTrue(trans.equals(trans));

        final ZoneOffsetTransition otherTrans = test.getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));
        assertEquals(trans.hashCode(), otherTrans.hashCode());
    }

    @Test
    public void test_NewYork_getOffsetInfo_overlap() {
        ZoneRules test = americaNewYork();
        final LocalDateTime dateTime = LocalDateTime.of(2008, 11, 2, 1, 0, 0, 0);
        ZoneOffsetTransition trans = checkOffset(test, dateTime, ZoneOffset.ofHours(-4), OVERLAP);
        assertEquals(trans.isGap(), false);
        assertEquals(trans.isOverlap(), true);
        assertEquals(trans.getOffsetBefore(), ZoneOffset.ofHours(-4));
        assertEquals(trans.getOffsetAfter(), ZoneOffset.ofHours(-5));
        assertEquals(trans.getInstant(), createInstant(2008, 11, 2, 2, 0, ZoneOffset.ofHours(-4)));
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(-1)), false);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(-5)), true);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(-4)), true);
        assertEquals(trans.isValidOffset(OFFSET_PTWO), false);
        assertEquals(trans.toString(), "Transition[Overlap at 2008-11-02T02:00-04:00 to -05:00]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(ZoneOffset.ofHours(-4)));
        assertTrue(trans.equals(trans));

        final ZoneOffsetTransition otherTrans = test.getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));
        assertEquals(trans.hashCode(), otherTrans.hashCode());
    }

    @Test
    public void test_NewYork_getStandardOffset() {
        ZoneRules test = americaNewYork();
        ZonedDateTime dateTime = createZDT(1860, 1, 1, ZoneOffset.UTC);
        while (dateTime.getYear() < 2010) {
            Instant instant = dateTime.toInstant();
            if (dateTime.toLocalDate().isBefore(LocalDate.of(1883, 11, 18))) {
                assertEquals(test.getStandardOffset(instant), ZoneOffset.of("-04:56:02"));
            } else {
                assertEquals(test.getStandardOffset(instant), ZoneOffset.ofHours(-5));
            }
            dateTime = dateTime.plusMonths(6);
        }
    }

    //-----------------------------------------------------------------------
    // Kathmandu
    //-----------------------------------------------------------------------
    private ZoneRules asiaKathmandu() {
        return ZoneId.of("Asia/Kathmandu").getRules();
    }

    @Test
    public void test_Kathmandu_nextTransition_historic() {
        ZoneRules test = asiaKathmandu();
        List<ZoneOffsetTransition> trans = test.getTransitions();

        ZoneOffsetTransition first = trans.get(0);
        assertEquals(test.nextTransition(first.getInstant().minusNanos(1)), first);

        for (int i = 0; i < trans.size() - 1; i++) {
            ZoneOffsetTransition cur = trans.get(i);
            ZoneOffsetTransition next = trans.get(i + 1);

            assertEquals(test.nextTransition(cur.getInstant()), next);
            assertEquals(test.nextTransition(next.getInstant().minusNanos(1)), next);
        }
    }

    @Test
    public void test_Kathmandu_nextTransition_noRules() {
        ZoneRules test = asiaKathmandu();
        List<ZoneOffsetTransition> trans = test.getTransitions();

        ZoneOffsetTransition last = trans.get(trans.size() - 1);
        assertEquals(test.nextTransition(last.getInstant()), null);
    }

    //-----------------------------------------------------------------------
    // Apia
    //-----------------------------------------------------------------------
    private ZoneRules pacificApia() {
        return ZoneId.of("Pacific/Apia").getRules();
    }

    @Test
    public void test_Apia_nextTransition_historic() {
        ZoneRules test = pacificApia();
        List<ZoneOffsetTransition> trans = test.getTransitions();

        ZoneOffsetTransition first = trans.get(0);
        assertEquals(test.nextTransition(first.getInstant().minusNanos(1)), first);

        for (int i = 0; i < trans.size() - 1; i++) {
            ZoneOffsetTransition cur = trans.get(i);
            ZoneOffsetTransition next = trans.get(i + 1);

            assertEquals(test.nextTransition(cur.getInstant()), next);
            assertEquals(test.nextTransition(next.getInstant().minusNanos(1)), next);
        }
    }

    @Test
    public void test_Apia_jumpOverInternationalDateLine_M10_to_P14() {
        // transition occurred at 2011-12-30T00:00-10:00
        ZoneRules test = pacificApia();
        Instant instantBefore = LocalDate.of(2011, 12, 27).atStartOfDay(ZoneOffset.UTC).toInstant();
        ZoneOffsetTransition trans = test.nextTransition(instantBefore);
        assertEquals(trans.getDateTimeBefore(), LocalDateTime.of(2011, 12, 30, 0, 0));
        assertEquals(trans.getDateTimeAfter(), LocalDateTime.of(2011, 12, 31, 0, 0));
        assertEquals(trans.isGap(), true);
        assertEquals(trans.isOverlap(), false);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(-10)), false);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHours(+14)), false);
        assertEquals(trans.getDuration(), Duration.ofHours(24));
        assertEquals(trans.getInstant(), LocalDateTime.of(2011, 12, 31, 0, 0).toInstant(ZoneOffset.ofHours(+14)));

        ZonedDateTime zdt = ZonedDateTime.of(2011, 12, 29, 23, 0, 0, 0, ZoneId.of("Pacific/Apia"));
        assertEquals(zdt.plusHours(2).toLocalDateTime(), LocalDateTime.of(2011, 12, 31, 1, 0));
    }

    @Test
    public void test_Apia_jumpForwardOverInternationalDateLine_P12_to_M12() {
        // transition occurred at 1892-07-04T00:00+12:33:04
        ZoneRules test = pacificApia();
        Instant instantBefore = LocalDate.of(1892, 7, 2).atStartOfDay(ZoneOffset.UTC).toInstant();
        ZoneOffsetTransition trans = test.nextTransition(instantBefore);
        assertEquals(trans.getDateTimeBefore(), LocalDateTime.of(1892, 7, 5, 0, 0));
        assertEquals(trans.getDateTimeAfter(), LocalDateTime.of(1892, 7, 4, 0, 0));
        assertEquals(trans.isGap(), false);
        assertEquals(trans.isOverlap(), true);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHoursMinutesSeconds(+12, 33, 4)), true);
        assertEquals(trans.isValidOffset(ZoneOffset.ofHoursMinutesSeconds(-11, -26, -56)), true);
        assertEquals(trans.getDuration(), Duration.ofHours(-24));
        assertEquals(trans.getInstant(), LocalDateTime.of(1892, 7, 4, 0, 0).toInstant(ZoneOffset.ofHoursMinutesSeconds(-11, -26, -56)));

        ZonedDateTime zdt = ZonedDateTime.of(1892, 7, 4, 23, 0, 0, 0, ZoneId.of("Pacific/Apia"));
        assertEquals(zdt.plusHours(2).toLocalDateTime(), LocalDateTime.of(1892, 7, 4, 1, 0, 0));
    }

    //-------------------------------------------------------------------------
    @Test(expectedExceptions=UnsupportedOperationException.class)
    public void test_getTransitions_immutable() {
        ZoneRules test = europeParis();
        test.getTransitions().clear();
    }

    @Test(expectedExceptions=UnsupportedOperationException.class)
    public void test_getTransitionRules_immutable() {
        ZoneRules test = europeParis();
        test.getTransitionRules().clear();
    }

    //-----------------------------------------------------------------------
    // of()
    //-----------------------------------------------------------------------
    @Test
    public void test_of(){
        //used for standard offset
        ZoneOffset stdOffset1 = ZoneOffset.UTC;
        ZoneOffset stdOffset2 = ZoneOffset.ofHours(1);
        LocalDateTime time_of_stdOffsetTransition1 = LocalDateTime.of(2013, 1, 5, 1, 0);
        ZoneOffsetTransition  stdOffsetTransition1 = ZoneOffsetTransition.of(time_of_stdOffsetTransition1, stdOffset1, stdOffset2);
        List<ZoneOffsetTransition> stdOffsetTransition_list = new ArrayList<ZoneOffsetTransition>();
        stdOffsetTransition_list.add(stdOffsetTransition1);

        //used for wall offset
        ZoneOffset wallOffset1 = ZoneOffset.ofHours(2);
        ZoneOffset wallOffset2 = ZoneOffset.ofHours(4);
        ZoneOffset wallOffset3 = ZoneOffset.ofHours(7);

        LocalDateTime time_of_wallOffsetTransition1 = LocalDateTime.of(2013, 2, 5, 1, 0);
        LocalDateTime time_of_wallOffsetTransition2 = LocalDateTime.of(2013, 3, 5, 1, 0);
        LocalDateTime time_of_wallOffsetTransition3 = LocalDateTime.of(2013, 10, 5, 1, 0);

        ZoneOffsetTransition  wallOffsetTransition1 = ZoneOffsetTransition.of(time_of_wallOffsetTransition1, wallOffset1, wallOffset2);
        ZoneOffsetTransition  wallOffsetTransition2 = ZoneOffsetTransition.of(time_of_wallOffsetTransition2, wallOffset2, wallOffset3);
        ZoneOffsetTransition  wallOffsetTransition3 = ZoneOffsetTransition.of(time_of_wallOffsetTransition3, wallOffset3, wallOffset1);

        List<ZoneOffsetTransition> wallOffsetTransition_list = new ArrayList<ZoneOffsetTransition>();
        wallOffsetTransition_list.add(wallOffsetTransition1);
        wallOffsetTransition_list.add(wallOffsetTransition2);
        wallOffsetTransition_list.add(wallOffsetTransition3);

        //used for ZoneOffsetTransitionRule
        ZoneOffset ruleOffset = ZoneOffset.ofHours(3);
        ZoneOffsetTransitionRule.TimeDefinition timeDefinition = ZoneOffsetTransitionRule.TimeDefinition.valueOf("WALL");
        ZoneOffsetTransitionRule rule1 = ZoneOffsetTransitionRule.of(Month.FEBRUARY,
                                                                     2,
                                                                     DayOfWeek.MONDAY,
                                                                     LocalTime.of(1, 0),
                                                                     false,
                                                                     timeDefinition,
                                                                     ZoneOffset.UTC,
                                                                     ZoneOffset.UTC,
                                                                     ruleOffset
                                                                     );
         List<ZoneOffsetTransitionRule> rule_list = new ArrayList<ZoneOffsetTransitionRule>();
         rule_list.add(rule1);

         //Begin verification
         ZoneRules zoneRule = ZoneRules.of(stdOffset1,
                                           wallOffset1,
                                           stdOffsetTransition_list,
                                           wallOffsetTransition_list,
                                           rule_list
                                           );

         OffsetDateTime before_time_of_stdOffsetTransition1 = OffsetDateTime.of(time_of_stdOffsetTransition1, stdOffset1).minusSeconds(1);
         OffsetDateTime after_time_of_stdOffsetTransition1 = OffsetDateTime.of(time_of_stdOffsetTransition1, stdOffset1).plusSeconds(1);;
         assertEquals(zoneRule.getStandardOffset(before_time_of_stdOffsetTransition1.toInstant()), stdOffset1);
         assertEquals(zoneRule.getStandardOffset(after_time_of_stdOffsetTransition1.toInstant()), stdOffset2);

         OffsetDateTime  before_time_of_wallOffsetTransition1 = OffsetDateTime.of(time_of_wallOffsetTransition1, wallOffset1).minusSeconds(1);
         OffsetDateTime  after_time_of_wallOffsetTransition1 = OffsetDateTime.of(time_of_wallOffsetTransition1, wallOffset1).plusSeconds(1);
         assertEquals(zoneRule.nextTransition(before_time_of_wallOffsetTransition1.toInstant()), wallOffsetTransition1);
         assertEquals(zoneRule.nextTransition(after_time_of_wallOffsetTransition1.toInstant()), wallOffsetTransition2);

         OffsetDateTime  before_time_of_wallOffsetTransition2 = OffsetDateTime.of(time_of_wallOffsetTransition2, wallOffset2).minusSeconds(1);
         OffsetDateTime  after_time_of_wallOffsetTransition2 = OffsetDateTime.of(time_of_wallOffsetTransition2, wallOffset2).plusSeconds(1);
         assertEquals(zoneRule.nextTransition(before_time_of_wallOffsetTransition2.toInstant()), wallOffsetTransition2);
         assertEquals(zoneRule.nextTransition(after_time_of_wallOffsetTransition2.toInstant()), wallOffsetTransition3);

         OffsetDateTime  before_time_of_wallOffsetTransition3 = OffsetDateTime.of(time_of_wallOffsetTransition3, wallOffset3).minusSeconds(1);
         OffsetDateTime  after_time_of_wallOffsetTransition3 = OffsetDateTime.of(time_of_wallOffsetTransition3, wallOffset3).plusSeconds(1);
         assertEquals(zoneRule.nextTransition(before_time_of_wallOffsetTransition3.toInstant()), wallOffsetTransition3);
         assertEquals(zoneRule.nextTransition(after_time_of_wallOffsetTransition3.toInstant()), rule1.createTransition(2014));
    }

    //-----------------------------------------------------------------------
    // equals() / hashCode()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals() {
        ZoneRules test1 = europeLondon();
        ZoneRules test2 = europeParis();
        ZoneRules test2b = europeParis();
        assertEquals(test1.equals(test2), false);
        assertEquals(test2.equals(test1), false);

        assertEquals(test1.equals(test1), true);
        assertEquals(test2.equals(test2), true);
        assertEquals(test2.equals(test2b), true);

        assertEquals(test1.hashCode() == test1.hashCode(), true);
        assertEquals(test2.hashCode() == test2.hashCode(), true);
        assertEquals(test2.hashCode() == test2b.hashCode(), true);
    }

    @Test
    public void test_equals_null() {
        assertEquals(europeLondon().equals(null), false);
    }

    @Test
    public void test_equals_notZoneRules() {
        assertEquals(europeLondon().equals("Europe/London"), false);
    }

    @Test
    public void test_toString() {
        assertEquals(europeLondon().toString().contains("ZoneRules"), true);
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    private Instant createInstant(int year, int month, int day, ZoneOffset offset) {
        return LocalDateTime.of(year, month, day, 0, 0).toInstant(offset);
    }

    private Instant createInstant(int year, int month, int day, int hour, int min, ZoneOffset offset) {
        return LocalDateTime.of(year, month, day, hour, min).toInstant(offset);
    }

    private Instant createInstant(int year, int month, int day, int hour, int min, int sec, int nano, ZoneOffset offset) {
        return LocalDateTime.of(year, month, day, hour, min, sec, nano).toInstant(offset);
    }

    private ZonedDateTime createZDT(int year, int month, int day, ZoneId zone) {
        return LocalDateTime.of(year, month, day, 0, 0).atZone(zone);
    }

    private LocalDateTime createLDT(int year, int month, int day) {
        return LocalDateTime.of(year, month, day, 0, 0);
    }

    private ZoneOffsetTransition checkOffset(ZoneRules rules, LocalDateTime dateTime, ZoneOffset offset, int type) {
        List<ZoneOffset> validOffsets = rules.getValidOffsets(dateTime);
        assertEquals(validOffsets.size(), type);
        assertEquals(rules.getOffset(dateTime), offset);
        if (type == 1) {
            assertEquals(validOffsets.get(0), offset);
            return null;
        } else {
            ZoneOffsetTransition zot = rules.getTransition(dateTime);
            assertNotNull(zot);
            assertEquals(zot.isOverlap(), type == 2);
            assertEquals(zot.isGap(), type == 0);
            assertEquals(zot.isValidOffset(offset), type == 2);
            return zot;
        }
    }

}
