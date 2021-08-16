/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4955000
 * @summary Make sure that a Date and a GregorianCalendar produce the
 * same date/time. Both are new implementations in 1.5.
 */

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static java.util.GregorianCalendar.*;

@SuppressWarnings("deprecation")
public class Bug4955000 {

    // Tests for Date.UTC(), derived from JCK
    // Date.miscTests.Date1025 and Date2015
    public static void main(String[] args) {
        TimeZone defaultTZ = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("NST"));
            GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            // Date1025
            int[] years1 = {
                Integer.MIN_VALUE,
                Integer.MIN_VALUE + 1,
                gc.getMinimum(YEAR) - 1,
                gc.getMaximum(YEAR) + 1,
                Integer.MAX_VALUE - 1,
                Integer.MAX_VALUE
            };
            for (int i = 0; i < years1.length; i++) {
                gc.clear();
                gc.set(years1[i], JANUARY, 1);
                long t = gc.getTimeInMillis();
                long utc = Date.UTC(years1[i] - 1900, 1 - 1, 1,
                        0, 0, 0); // Jan 1 00:00:00
                if (t != utc) {
                    throw new RuntimeException("t (" + t + ") != utc (" + utc + ")");
                }
            }

            // Date2015
            int[] years = {
                gc.getGreatestMinimum(YEAR),
                gc.getGreatestMinimum(YEAR) + 1,
                -1,
                0,
                1,
                gc.getLeastMaximum(YEAR) - 1,
                gc.getLeastMaximum(YEAR)
            };

            int[] months = {
                gc.getMinimum(MONTH),
                gc.getMinimum(MONTH) + 1,
                gc.getMaximum(MONTH) - 1,
                gc.getMaximum(MONTH)
            };

            int[] dates = {
                gc.getMinimum(DAY_OF_MONTH),
                gc.getMinimum(DAY_OF_MONTH) + 1,
                gc.getMaximum(DAY_OF_MONTH) - 1,
                gc.getMaximum(DAY_OF_MONTH)
            };

            int[] hs = {
                gc.getMinimum(HOUR),
                gc.getMinimum(HOUR) + 1,
                gc.getMaximum(HOUR) - 1,
                gc.getMaximum(HOUR)
            };

            int[] ms = {
                gc.getMinimum(MINUTE),
                gc.getMinimum(MINUTE) + 1,
                gc.getMaximum(MINUTE) - 1,
                gc.getMaximum(MINUTE)
            };

            int[] ss = {
                gc.getMinimum(SECOND),
                gc.getMinimum(SECOND) + 1,
                gc.getMaximum(SECOND) - 1,
                gc.getMaximum(SECOND)
            };

            for (int i = 0; i < years.length; i++) {
                for (int j = 0; j < months.length; j++) {
                    for (int k = 0; k < dates.length; k++) {
                        for (int m = 0; m < hs.length; m++) {
                            for (int n = 0; n < ms.length; n++) {
                                for (int p = 0; p < ss.length; p++) {
                                    int year = years[i] - 1900;
                                    int month = months[j];
                                    int date = dates[k];
                                    int hours = hs[m];
                                    int minutes = ms[n];
                                    int seconds = ss[p];

                                    long result = Date.UTC(year, month, date,
                                            hours, minutes, seconds);

                                    gc.clear();
                                    gc.set(year + 1900, month, date, hours, minutes, seconds);

                                    long expected = gc.getTime().getTime();

                                    if (expected != result) {
                                        throw new RuntimeException("expected (" + expected
                                                + ") != result (" + result + ")");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            TimeZone.setDefault(defaultTZ);
        }
    }
}
