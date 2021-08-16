/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 6772689
 * @summary Test for standard-to-daylight transitions at midnight:
 * date stays on the given day.
 */

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import static java.util.GregorianCalendar.*;

public class Bug6772689 {
    private static final int BEGIN_YEAR = 2035;
    private static final int END_YEAR = BEGIN_YEAR + 28;

    public static void main(String[] args) {
        TimeZone defaultTimeZone = TimeZone.getDefault();
        int errors = 0;

        Calendar cal = new GregorianCalendar(BEGIN_YEAR, MARCH, 1);
        String[] tzids = TimeZone.getAvailableIDs();
        try {
            for (String id : tzids) {
                TimeZone tz = TimeZone.getTimeZone(id);
                if (!tz.useDaylightTime()) {
                    continue;
                }
                TimeZone.setDefault(tz);

              dateloop:
                // Use future dates because sun.util.calendar.ZoneInfo
                // delegates offset transition calculations to a SimpleTimeZone
                // (after 2038 as of JDK7).
                for (int year = BEGIN_YEAR; year < END_YEAR; year++) {
                    for (int month = MARCH; month <= NOVEMBER; month++) {
                        cal.set(year, month, 1, 15, 0, 0);
                        int maxDom = cal.getActualMaximum(DAY_OF_MONTH);
                        for (int dom = 1; dom <= maxDom; dom++) {
                            Date date = new Date(year - 1900, month, dom);
                            if (date.getYear()+1900 != year
                                || date.getMonth() != month
                                || date.getDate() != dom) {
                                System.err.printf("%s: got %04d-%02d-%02d, expected %04d-%02d-%02d%n",
                                                  id,
                                                  date.getYear() + 1900,
                                                  date.getMonth() + 1,
                                                  date.getDate(),
                                                  year,
                                                  month + 1,
                                                  dom);
                                errors++;
                                break dateloop;
                            }
                        }
                    }
                }
            }
        } finally {
            // Restore the default TimeZone.
            TimeZone.setDefault(defaultTimeZone);
        }
        if (errors > 0) {
            throw new RuntimeException("Transition test failed");
        }
    }
}
