/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.Chronology;
import java.util.Locale;

public class HijrahConfigCheck {
    private static final String CALTYPE = "islamic-test";

    public static void main(String... args) {
        // Availability test
        if (Chronology.getAvailableChronologies().stream()
                .filter(c -> c.getCalendarType().equals(CALTYPE))
                .count() != 1) {
            throw new RuntimeException(CALTYPE + " chronology was not found, or " +
                    "appeared more than once in Chronology.getAvailableChronologies()");
        }

        // Instantiation tests
        Chronology c1 = Chronology.of(CALTYPE);
        Chronology c2 = Chronology.ofLocale(Locale.forLanguageTag("und-u-ca-" + CALTYPE ));
        if (!c1.equals(c2)) {
            throw new RuntimeException(CALTYPE + " chronologies differ. c1: " + c1 +
                                        ", c2: " + c2);
        }

        // Date test
        // 2020-01-10 is AH 1000-01-10 in islamic-test config
        LocalDateTime iso = LocalDateTime.of(LocalDate.of(2020, 1, 10), LocalTime.MIN);
        ChronoLocalDateTime hijrah = c1.date(1000, 1, 10).atTime(LocalTime.MIN);
        if (!iso.toInstant(ZoneOffset.UTC).equals(hijrah.toInstant(ZoneOffset.UTC))) {
            throw new RuntimeException("test Hijrah date is incorrect. LocalDate: " +
                    iso + ", test date: " + hijrah);
        }
    }
}
