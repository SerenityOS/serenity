/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8152077
 * @summary Make sure that roll() with HOUR/HOUR_OF_DAY works around standard/daylight
 *          time transitions
 */

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import static java.util.Calendar.*;

public class Bug8152077 {
    private static final TimeZone LA = TimeZone.getTimeZone("America/Los_Angeles");
    private static final TimeZone BR = TimeZone.getTimeZone("America/Sao_Paulo");

    private static final int[] ALLDAY_HOURS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
        12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23
    };
    private static final int[] AM_HOURS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
    };
    private static final int[] PM_HOURS = { // in 24-hour clock
        12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23
    };

    private static int errors;

    public static void main(String[] args) {
        TimeZone initialTz = TimeZone.getDefault();
        try {
            testRoll(LA, new int[] { 2016, MARCH, 13 },
                     new int[] { 0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11,
                         12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 });
            testRoll(LA, new int[] { 2016, MARCH, 13 },
                     new int[] { 0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11 });
            testRoll(LA, new int[] { 2016, MARCH, 13 }, PM_HOURS);

            testRoll(LA, new int[] { 2016, NOVEMBER, 6 }, ALLDAY_HOURS);
            testRoll(LA, new int[] { 2016, NOVEMBER, 6 }, AM_HOURS);
            testRoll(LA, new int[] { 2016, NOVEMBER, 6 }, PM_HOURS);

            testRoll(BR, new int[] { 2016, FEBRUARY, 21 }, ALLDAY_HOURS);
            testRoll(BR, new int[] { 2016, FEBRUARY, 21 }, AM_HOURS);
            testRoll(BR, new int[] { 2016, FEBRUARY, 21 }, PM_HOURS);

            testRoll(BR, new int[] { 2016, OCTOBER, 15 }, ALLDAY_HOURS);
            testRoll(BR, new int[] { 2016, OCTOBER, 15 }, PM_HOURS);
            testRoll(BR, new int[] { 2016, OCTOBER, 16 },
                     new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
                         12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23 });
            testRoll(BR, new int[] { 2016, OCTOBER, 16 },
                     new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 });
            testRoll(BR, new int[] { 2016, OCTOBER, 16 }, PM_HOURS);
        } finally {
            TimeZone.setDefault(initialTz);
        }
        if (errors > 0) {
            throw new RuntimeException("Test failed");
        }
    }

    private static void testRoll(TimeZone tz, int[] params, int[] sequence) {
        TimeZone.setDefault(tz);
        for (int i = 0; i < sequence.length; i++) {
            testRoll(+1, params, sequence, i);
            testRoll(-1, params, sequence, i);
        }
    }

    // amount must be 1 or -1.
    private static void testRoll(int amount, int[] params, int[] sequence, int startIndex) {
        int year = params[0];
        int month = params[1];
        int dayOfMonth = params[2];
        int hourOfDay = sequence[startIndex];
        Calendar cal = new GregorianCalendar(year, month, dayOfMonth,
                                             hourOfDay, 0, 0);
        int ampm = cal.get(AM_PM);

        int length = sequence.length;
        int count = length * 2;
        int field = (length > 12) ? HOUR_OF_DAY : HOUR;

        System.out.printf("roll(%s, %2d) starting from %s%n",
                          (field == HOUR) ? "HOUR" : "HOUR_OF_DAY", amount, cal.getTime());
        for (int i = 0; i < count; i++) {
            int index = (amount > 0) ? (startIndex + i + 1) % length
                                     : Math.floorMod(startIndex - i - 1, length);
            int expected = sequence[index];
            if (field == HOUR) {
                expected %= 12;
            }
            cal.roll(field, amount);
            int value = cal.get(field);
            if (value != expected) {
                System.out.println("Unexpected field value: got=" + value
                                    + ", expected=" + expected);
                errors++;
            }
            if (cal.get(DAY_OF_MONTH) != dayOfMonth) {
                System.out.println("DAY_OF_MONTH changed: " + dayOfMonth
                                    + " to " + cal.get(DAY_OF_MONTH));
            }
            if (field == HOUR && cal.get(AM_PM) != ampm) {
                System.out.println("AM_PM changed: " + ampm + " to " + cal.get(AM_PM));
                errors++;
            }
        }
    }
}
