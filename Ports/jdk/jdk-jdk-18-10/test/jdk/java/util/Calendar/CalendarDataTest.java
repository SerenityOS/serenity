/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8190918 8202537 8221432 8231273
 * @summary Tests for region dependent calendar data, i.e.,
 *      firstDayOfWeek and minimalDaysInFirstWeek.
 * @modules jdk.localedata
 * @run main CalendarDataTest
 */

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.IntStream;

public class CalendarDataTest {

    // golden data from CLDR
    private static final List<List<String>> FIRSTDAYDATA = List.of(
        List.of("1", "AG AS AU BD BR BS BT BW BZ CA CN CO DM DO ET GT " +
            "GU HK HN ID IL IN JM JP KE KH KR LA MH MM MO MT MX MZ " +
            "NI NP PA PE PH PK PR PT PY SA SG SV TH TT TW UM US VE " +
            "VI WS YE ZA ZW"),
        List.of("2",   "001 AD AI AL AM AN AR AT AX AZ BA BE BG BM BN BY " +
            "CH CL CM CR CY CZ DE DK EC EE ES FI FJ FO FR GB GE GF GP " +
            "GR HR HU IE IS IT KG KZ LB LI LK LT LU LV MC MD ME MK MN MQ " +
            "MY NL NO NZ PL RE RO RS RU SE SI SK SM TJ TM TR UA UY UZ " +
            "VA VN XK"),
        List.of("6", "MV"),
        List.of("7", "AE AF BH DJ DZ EG IQ IR JO KW LY OM QA SD SY"));

    private static final List<List<String>> MINDAYSDATA = List.of(
        List.of("1", "001 GU UM US VI"),
        List.of("4", "AD AN AT AX BE BG CH CZ DE DK EE ES FI FJ FO FR " +
            "GB GF GG GI GP GR HU IE IM IS IT JE LI LT LU MC MQ NL NO " +
            "PL PT RE RU SE SJ SK SM VA"));

    public static void main(String... args) throws Exception {
        // world
        Calendar cal = Calendar.getInstance(new Locale("", "001"));
        checkResult("001",
            cal.getFirstDayOfWeek(),
            cal.getMinimalDaysInFirstWeek());

        // two letter country codes
        IntStream.range(0x41, 0x5b)
            .forEach(c1 -> {
                IntStream.range(0x41, 0x5b)
                    .mapToObj(c2 -> String.valueOf((char)c1) + String.valueOf((char)c2))
                    .forEach(region -> {
                        Calendar c = Calendar.getInstance(new Locale("", region));
                        checkResult(region,
                                c.getFirstDayOfWeek(),
                                c.getMinimalDaysInFirstWeek());
                    });
            });
    }

    private static void checkResult(String region, int firstDay, int minDays) {
        // first day of week
        int expected = Integer.parseInt(findEntry(region, FIRSTDAYDATA)
            .orElse(findEntry("001", FIRSTDAYDATA).orElse(List.of("1")))
            .get(0));
        if (firstDay != expected) {
            throw new RuntimeException("firstDayOfWeek is incorrect for the region: " +
                    region + ". Returned: " + firstDay + ", Expected: " + expected);
        }

        // minimal days in first week
        expected = Integer.parseInt(findEntry(region, MINDAYSDATA)
            .orElse(findEntry("001", MINDAYSDATA).orElse(List.of("1")))
            .get(0));
        if (minDays != expected) {
            throw new RuntimeException("minimalDaysInFirstWeek is incorrect for the region: " +
                    region + ". Returned: " + minDays + ", Expected: " + expected);
        }
    }

    private static Optional<List<String>> findEntry(String region, List<List<String>> data) {
        return data.stream()
            .filter(l -> l.get(1).contains(region))
            .findAny();
    }
}

