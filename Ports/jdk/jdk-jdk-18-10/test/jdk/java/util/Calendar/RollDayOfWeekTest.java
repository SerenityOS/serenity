/*
 * Copyright (c) 2004, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5090555 5091805
 * @summary Make sure that rolling DAY_OF_WEEK stays in the same week
 * around year boundaries.
 * @run main/othervm RollDayOfWeekTest 5 5
 */

import java.util.*;

import static java.util.Calendar.*;

// Usage: java RollDayOfWeekTest [pastYears futureYears]
public class RollDayOfWeekTest {
    public static void main(String[] args) {
        int pastYears = 5, futureYears = 23;
        if (args.length == 2) {
            pastYears = Integer.parseInt(args[0]);
            pastYears = Math.max(1, Math.min(pastYears, 5));
            futureYears = Integer.parseInt(args[1]);
            futureYears = Math.max(1, Math.min(futureYears, 28));
        }

        System.out.printf("Test [%d .. %+d] year range.%n", -pastYears, futureYears);
        Calendar cal = new GregorianCalendar();
        int year = cal.get(YEAR) - pastYears;

        // Use the all combinations of firstDayOfWeek and
        // minimalDaysInFirstWeek values in the year range current
        // year - pastYears to current year + futureYears.
        for (int fdw = SUNDAY; fdw <= SATURDAY; fdw++) {
            for (int mdifw = 1; mdifw <= 7; mdifw++) {
                cal.clear();
                cal.setFirstDayOfWeek(fdw);
                cal.setMinimalDaysInFirstWeek(mdifw);
                cal.set(year, JANUARY, 1);
                checkRoll(cal, futureYears);
            }
        }

        // testing roll from BCE to CE
        year = -1;
        for (int fdw = SUNDAY; fdw <= SATURDAY; fdw++) {
            for (int mdifw = 1; mdifw <= 7; mdifw++) {
                cal.clear();
                cal.setFirstDayOfWeek(fdw);
                cal.setMinimalDaysInFirstWeek(mdifw);
                cal.set(year, JANUARY, 1);
                checkRoll(cal, 4);
            }
        }
    }


    static void checkRoll(Calendar cal, int years) {
        Calendar cal2 = null, cal3 = null, prev = null;
        // Check 28 years
        for (int x = 0; x < (int)(365.2425*years); x++) {
            cal2 = (Calendar) cal.clone();
            cal3 = (Calendar) cal.clone();

            // roll foreword
            for (int i = 0; i < 10; i++) {
                prev = (Calendar) cal2.clone();
                cal2.roll(Calendar.DAY_OF_WEEK, +1);
                roll(cal3, +1);
                long t2 = cal2.getTimeInMillis();
                long t3 = cal3.getTimeInMillis();
                if (t2 != t3) {
                    System.err.println("prev: " + prev.getTime() + "\n" + prev);
                    System.err.println("cal2: " + cal2.getTime() + "\n" + cal2);
                    System.err.println("cal3: " + cal3.getTime() + "\n" + cal3);
                    throw new RuntimeException("+1: t2=" + t2 + ", t3=" + t3);
                }
            }

            // roll backward
            for (int i = 0; i < 10; i++) {
                prev = (Calendar) cal2.clone();
                cal2.roll(Calendar.DAY_OF_WEEK, -1);
                roll(cal3, -1);
                long t2 = cal2.getTimeInMillis();
                long t3 = cal3.getTimeInMillis();
                if (t2 != t3) {
                    System.err.println("prev: " + prev.getTime() + "\n" + prev);
                    System.err.println("cal2: " + cal2.getTime() + "\n" + cal2);
                    System.err.println("cal3: " + cal3.getTime() + "\n" + cal3);
                    throw new RuntimeException("-1: t2=" + t2 + ", t3=" + t3);
                }
            }
            cal.add(DAY_OF_YEAR, +1);
        }
    }

    // Another way to roll within the same week.
    static void roll(Calendar cal, int n) {
        int doy = cal.get(DAY_OF_YEAR);
        int diff = cal.get(DAY_OF_WEEK) - cal.getFirstDayOfWeek();
        if (diff < 0) {
            diff += 7;
        }

        // dow1: first day of the week
        int dow1 = doy - diff;
        n %= 7;
        doy += n;
        if (doy < dow1) {
            doy += 7;
        } else if (doy >= dow1 + 7) {
            doy -= 7;
        }
        cal.set(DAY_OF_YEAR, doy);
    }
}
