/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

package test.java.time.zone;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.Year;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneOffsetTransitionRule;
import java.time.zone.ZoneRules;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @summary Tests for ZoneRules class.
 *
 * @bug 8212970 8236903 8239836
 */
@Test
public class TestZoneRules {

    private static final ZoneId DUBLIN = ZoneId.of("Europe/Dublin");
    private static final ZoneId PRAGUE = ZoneId.of("Europe/Prague");
    private static final ZoneId WINDHOEK = ZoneId.of("Africa/Windhoek");
    private static final ZoneId CASABLANCA = ZoneId.of("Africa/Casablanca");

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");
    private static final LocalTime ONE_AM = LocalTime.of(1, 0);

    private static final ZoneOffset OFF_0 = ZoneOffset.ofHours(0);
    private static final ZoneOffset OFF_1 = ZoneOffset.ofHours(1);
    private static final ZoneOffset OFF_2 = ZoneOffset.ofHours(2);
    private static final List EL = Collections.emptyList();
    private static final ZoneOffsetTransition ZOT = ZoneId.of("America/Los_Angeles").getRules().getTransitions().get(0);
    private static final ZoneOffsetTransitionRule ZOTR = ZoneId.of("America/Los_Angeles").getRules().getTransitionRules().get(0);

    @DataProvider
    private Object[][] negativeDST () {
        return new Object[][] {
            // ZoneId, localDate, offset, standard offset, isDaylightSavings
            // Europe/Dublin for the Rule "Eire"
            {DUBLIN, LocalDate.of(1970, 6, 23), OFF_1, OFF_0, true},
            {DUBLIN, LocalDate.of(1971, 6, 23), OFF_1, OFF_0, true},
            {DUBLIN, LocalDate.of(1971, 11, 1), OFF_0, OFF_0, false},
            {DUBLIN, LocalDate.of(2019, 6, 23), OFF_1, OFF_0, true},
            {DUBLIN, LocalDate.of(2019, 12, 23), OFF_0, OFF_0, false},

            // Europe/Prague which contains fixed negative savings (not a named Rule)
            {PRAGUE, LocalDate.of(1946, 9, 30), OFF_2, OFF_1, true},
            {PRAGUE, LocalDate.of(1946, 10, 10), OFF_1, OFF_1, false},
            {PRAGUE, LocalDate.of(1946, 12, 3), OFF_0, OFF_0, false},
            {PRAGUE, LocalDate.of(1947, 2, 25), OFF_1, OFF_1, false},
            {PRAGUE, LocalDate.of(1947, 4, 30), OFF_2, OFF_1, true},

            // Africa/Windhoek for the Rule "Namibia"
            {WINDHOEK, LocalDate.of(1994, 3, 23), OFF_1, OFF_1, false},
            {WINDHOEK, LocalDate.of(2016, 9, 23), OFF_2, OFF_1, true},

            // Africa/Casablanca for the Rule "Morocco" Defines negative DST till 2037 as of 2019a.
            {CASABLANCA, LocalDate.of(1939, 9, 13), OFF_1, OFF_0, true},
            {CASABLANCA, LocalDate.of(1939, 11, 20), OFF_0, OFF_0, false},
            {CASABLANCA, LocalDate.of(2018, 6, 18), OFF_1, OFF_0, true},
            {CASABLANCA, LocalDate.of(2019, 1, 1), OFF_1, OFF_0, true},
            {CASABLANCA, LocalDate.of(2019, 5, 6), OFF_0, OFF_0, false},
            {CASABLANCA, LocalDate.of(2037, 10, 5), OFF_0, OFF_0, false},
            {CASABLANCA, LocalDate.of(2037, 11, 16), OFF_1, OFF_0, true},
            {CASABLANCA, LocalDate.of(2038, 11, 8), OFF_1, OFF_0, true},
        };
    }

    @DataProvider
    private Object[][] transitionBeyondDay() {
        return new Object[][] {
            // ZoneId, LocalDateTime, beforeOffset, afterOffset

            // Asserts that the rule:
            // Rule Japan   1948    1951    -   Sep Sat>=8  25:00   0   S
            // translates to the next day.
            {TOKYO, LocalDateTime.of(LocalDate.of(1948, 9, 12), ONE_AM), ZoneOffset.ofHours(10), ZoneOffset.ofHours(9)},
            {TOKYO, LocalDateTime.of(LocalDate.of(1949, 9, 11), ONE_AM), ZoneOffset.ofHours(10), ZoneOffset.ofHours(9)},
            {TOKYO, LocalDateTime.of(LocalDate.of(1950, 9, 10), ONE_AM), ZoneOffset.ofHours(10), ZoneOffset.ofHours(9)},
            {TOKYO, LocalDateTime.of(LocalDate.of(1951, 9, 9), ONE_AM), ZoneOffset.ofHours(10), ZoneOffset.ofHours(9)},
        };
    }

    @DataProvider
    private Object[][] emptyTransitionList() {
        return new Object[][] {
            // days, offset, std offset, savings, isDST
            {7, 1, 2, -1, true},
            {-7, 1, 1, 0, false},
        };
    }

    @DataProvider
    private Object[][] isFixedOffset() {
        return new Object[][] {
            // ZoneRules, expected
            {ZoneRules.of(OFF_0), true},
            {ZoneRules.of(OFF_0, OFF_0, EL, EL, EL), true},
            {ZoneRules.of(OFF_0, OFF_1, EL, EL, EL), false},
            {ZoneRules.of(OFF_0, OFF_0, Collections.singletonList(ZOT), EL, EL), false},
            {ZoneRules.of(OFF_0, OFF_0, EL, Collections.singletonList(ZOT), EL), false},
            {ZoneRules.of(OFF_0, OFF_0, EL, EL, Collections.singletonList(ZOTR)), false},
        };
    }

    /**
     * Test ZoneRules whether the savings are positive in time zones that have
     * negative savings in the source TZ files.
     * @bug 8212970
     */
    @Test(dataProvider="negativeDST")
    public void test_NegativeDST(ZoneId zid, LocalDate ld, ZoneOffset offset, ZoneOffset stdOffset, boolean isDST) {
        Instant i = Instant.from(ZonedDateTime.of(ld, LocalTime.MIN, zid));
        ZoneRules zr = zid.getRules();
        assertEquals(zr.getOffset(i), offset);
        assertEquals(zr.getStandardOffset(i), stdOffset);
        assertEquals(zr.isDaylightSavings(i), isDST);
    }

    /**
     * Check the transition cutover time beyond 24:00, which should translate into the next day.
     * @bug 8212970
     */
    @Test(dataProvider="transitionBeyondDay")
    public void test_TransitionBeyondDay(ZoneId zid, LocalDateTime ldt, ZoneOffset before, ZoneOffset after) {
        ZoneOffsetTransition zot = ZoneOffsetTransition.of(ldt, before, after);
        ZoneRules zr = zid.getRules();
        assertTrue(zr.getTransitions().contains(zot));
    }

    /**
     * Make sure ZoneRules.findYear() won't throw out-of-range DateTimeException for
     * year calculation.
     * @bug 8236903
     */
    @Test
    public void test_TransitionLastRuleYear() {
        Instant maxLocalDateTime = LocalDateTime.of(Year.MAX_VALUE,
                12,
                31,
                23,
                59,
                59,
                999999999).toInstant(ZoneOffset.UTC);
        ZoneRules zoneRulesA = ZoneRules.of(OFF_1);
        ZoneOffsetTransition transition = ZoneOffsetTransition.of(LocalDateTime.ofEpochSecond(0, 0, OFF_0),
                OFF_0,
                OFF_1);
        ZoneOffsetTransitionRule transitionRule = ZoneOffsetTransitionRule.of(Month.JANUARY,
                1,
                DayOfWeek.SUNDAY,
                LocalTime.MIDNIGHT,
                true,
                ZoneOffsetTransitionRule.TimeDefinition.STANDARD,
                OFF_0,
                OFF_0,
                OFF_1);
        ZoneRules zoneRulesB = ZoneRules.of(OFF_0,
                OFF_0,
                Collections.singletonList(transition),
                Collections.singletonList(transition),
                Collections.singletonList(transitionRule));
        ZoneOffset offsetA = zoneRulesA.getOffset(maxLocalDateTime);
        ZoneOffset offsetB = zoneRulesB.getOffset(maxLocalDateTime);
        assertEquals(offsetA, offsetB);
    }

    /**
     * Tests whether empty "transitionList" is correctly interpreted.
     * @bug 8239836
     */
    @Test(dataProvider="emptyTransitionList")
    public void test_EmptyTransitionList(int days, int offset, int stdOffset, int savings, boolean isDST) {
        LocalDateTime transitionDay = LocalDateTime.of(2020, 1, 1, 2, 0);
        Instant testDay = transitionDay.plusDays(days).toInstant(ZoneOffset.UTC);
        ZoneOffsetTransition trans = ZoneOffsetTransition.of(
            transitionDay,
            OFF_1,
            OFF_2);
        ZoneRules rules = ZoneRules.of(OFF_1, OFF_1,
            Collections.singletonList(trans),
            Collections.emptyList(), Collections.emptyList());

        assertEquals(rules.getOffset(testDay), ZoneOffset.ofHours(offset));
        assertEquals(rules.getStandardOffset(testDay), ZoneOffset.ofHours(stdOffset));
        assertEquals(rules.getDaylightSavings(testDay), Duration.ofHours(savings));
        assertEquals(rules.isDaylightSavings(testDay), isDST);
    }

    /**
     * Tests whether isFixedOffset() is working correctly
     * @bug 8239836
     */
    @Test(dataProvider="isFixedOffset")
    public void test_IsFixedOffset(ZoneRules zr, boolean expected) {
        assertEquals(zr.isFixedOffset(), expected);
    }
}
