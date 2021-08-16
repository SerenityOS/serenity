/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import static org.testng.Assert.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import static org.testng.Assert.assertEquals;

/**
 * @test
 * @bug 8212970
 * @summary Test whether the savings are positive in time zones that have
 *      negative savings in the source TZ files.
 * @run testng NegativeDSTTest
 */
@Test
public class NegativeDSTTest {

    private static final TimeZone DUBLIN = TimeZone.getTimeZone("Europe/Dublin");
    private static final TimeZone PRAGUE = TimeZone.getTimeZone("Europe/Prague");
    private static final TimeZone WINDHOEK = TimeZone.getTimeZone("Africa/Windhoek");
    private static final TimeZone CASABLANCA = TimeZone.getTimeZone("Africa/Casablanca");
    private static final int ONE_HOUR = 3600_000;

    @DataProvider
    private Object[][] negativeDST () {
        return new Object[][] {
            // TimeZone, localDate, offset, isDaylightSavings
            // Europe/Dublin for the Rule "Eire"
            {DUBLIN, LocalDate.of(1970, 6, 23), ONE_HOUR, true},
            {DUBLIN, LocalDate.of(1971, 6, 23), ONE_HOUR, true},
            {DUBLIN, LocalDate.of(1971, 11, 1), 0, false},
            {DUBLIN, LocalDate.of(2019, 6, 23), ONE_HOUR, true},
            {DUBLIN, LocalDate.of(2019, 12, 23), 0, false},

            // Europe/Prague which contains fixed negative savings (not a named Rule)
            {PRAGUE, LocalDate.of(1946, 9, 30), 2 * ONE_HOUR, true},
            {PRAGUE, LocalDate.of(1946, 10, 10), ONE_HOUR, false},
            {PRAGUE, LocalDate.of(1946, 12, 3), 0, false},
            {PRAGUE, LocalDate.of(1947, 2, 25), ONE_HOUR, false},
            {PRAGUE, LocalDate.of(1947, 4, 30), 2 * ONE_HOUR, true},

            // Africa/Windhoek for the Rule "Namibia"
            {WINDHOEK, LocalDate.of(1994, 3, 23), ONE_HOUR, false},
            {WINDHOEK, LocalDate.of(2016, 9, 23), 2 * ONE_HOUR, true},

            // Africa/Casablanca for the Rule "Morocco" Defines negative DST till 2037 as of 2019a.
            {CASABLANCA, LocalDate.of(1939, 9, 13), ONE_HOUR, true},
            {CASABLANCA, LocalDate.of(1939, 11, 20), 0, false},
            {CASABLANCA, LocalDate.of(2018, 6, 18), ONE_HOUR, true},
            {CASABLANCA, LocalDate.of(2019, 1, 1), ONE_HOUR, true},
            {CASABLANCA, LocalDate.of(2019, 5, 6), 0, false},
            {CASABLANCA, LocalDate.of(2037, 10, 5), 0, false},
            {CASABLANCA, LocalDate.of(2037, 11, 16), ONE_HOUR, true},
            {CASABLANCA, LocalDate.of(2038, 11, 1), ONE_HOUR, true},
        };
    }

    @Test(dataProvider="negativeDST")
    public void test_NegativeDST(TimeZone tz, LocalDate ld, int offset, boolean isDST) {
        Date d = Date.from(Instant.from(ZonedDateTime.of(ld, LocalTime.MIN, tz.toZoneId())));
        assertEquals(tz.getOffset(d.getTime()), offset);
        assertEquals(tz.inDaylightTime(d), isDST);
    }
}
