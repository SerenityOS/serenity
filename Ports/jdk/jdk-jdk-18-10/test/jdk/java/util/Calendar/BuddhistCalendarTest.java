/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4817812 4847186 4956227 4956479
 * @summary Confirm that BuddhistCalendar's add(), roll() and toString() work correctly with Buddhist Era years.
 */

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import static java.util.Calendar.*;

public class BuddhistCalendarTest {

    private static final Locale THAI_LOCALE = new Locale("th", "TH");

    public static void main(String[] args) {
        testAddRoll();
        testToString();
        testException();
        testLeastMax();
    }

    /**
     * 4817812
     */
    static void testAddRoll() {
        Calendar cal;
        int base, year;

        /*
         * Test: BuddhistCalendar.add(YEAR)
         */
        cal = getBuddhistCalendar();
        base = cal.get(YEAR);
        cal.add(YEAR, 1);
        year = cal.get(YEAR);
        check(year, base+1, "add(+YEAR)");

        cal = getBuddhistCalendar();
        base = cal.get(YEAR);
        cal.add(YEAR, -3);
        year = cal.get(YEAR);
        check(year, base-3, "add(-YEAR)");

        /*
         * Test BuddhistCalendar.add(MONTH)
         */
        cal = getBuddhistCalendar();
        base = cal.get(YEAR);
        cal.set(MONTH, DECEMBER);
        cal.add(MONTH, 2);
        year = cal.get(YEAR);
        check(year, base+1, "add(+MONTH)");

        cal = getBuddhistCalendar();
        base = cal.get(YEAR);
        cal.set(MONTH, FEBRUARY);
        cal.add(MONTH, -4);
        year = cal.get(YEAR);
        check(year, base-1, "add(-MONTH)");

        /*
         * Test BuddhistCalendar.roll(YEAR)
         */
        cal = getBuddhistCalendar();
        base = cal.get(YEAR);
        cal.roll(YEAR, 2);
        year = cal.get(YEAR);
        check(year, base+2, "roll(+YEAR)");

        cal = getBuddhistCalendar();
        base = cal.get(YEAR);
        cal.roll(YEAR, -4);
        year = cal.get(YEAR);
        check(year, base-4, "roll(-YEAR)");

        /*
         * Test BuddhistCalendar.roll(WEEK_OF_YEAR)
         */
        cal = getBuddhistCalendar();
        cal.set(YEAR, 2543);   // A.D.2000
        cal.set(MONTH, DECEMBER);
        cal.set(DATE, 31);
        base = cal.get(YEAR);
        check(base, 2543, "roll(+WEEK_OF_YEAR)");
        cal.roll(WEEK_OF_YEAR, 10);
        year = cal.get(YEAR);
        check(year, base, "roll(+WEEK_OF_YEAR)");

        cal = getBuddhistCalendar();
        cal.set(YEAR, 2543);   // A.D.2000
        cal.set(MONTH, JANUARY);
        cal.set(DATE, 1);
        base = cal.get(YEAR);
        check(base, 2543, "roll(+WEEK_OF_YEAR)");
        cal.roll(WEEK_OF_YEAR, -10);
        year = cal.get(YEAR);
        check(year, base, "roll(-WEEK_OF_YEAR)");

        /*
         * Test Calendar.set(year, month, date)
         */
        cal = getBuddhistCalendar();
        base = cal.get(YEAR);
        cal.set(3001, APRIL, 10);
        year = cal.get(YEAR);
        check(year, 3001, "set(year, month, date)");

        /*
         * Test Calendar.set(year, month, date, hour, minute)
         */
        cal = getBuddhistCalendar();
        base = cal.get(YEAR);
        cal.set(3020, MAY, 20, 9, 10);
        year = cal.get(YEAR);
        check(year, 3020, "set(year, month, date, hour, minute)");

        /*
         * Test Calendar.set(year, month, date, hour, minute, second)
         */
        cal = getBuddhistCalendar();
        base = cal.get(YEAR);
        cal.set(3120, MAY, 20, 9, 10, 52);
        year = cal.get(YEAR);
        check(year, 3120, "set(year, month, date, hour, minute, second)");

        /*
         * Test BuddhistCalendar.getActualMaximum(YEAR);
         *    set(YEAR)/get(YEAR) in this method doesn't affect the real
         *    YEAR value because a clone is used with set()&get().
         */
        cal = getBuddhistCalendar();
        base = cal.get(YEAR);
        int limit = cal.getActualMaximum(YEAR);
        year = cal.get(YEAR);
        check(year, base, "BuddhistCalendar.getActualMaximum(YEAR)");

        /*
         * Test BuddhistCalendar.getActualMinimum(YEAR);
         *   This doesn't call set(YEAR) nor get(YEAR), though.
         */
        cal = getBuddhistCalendar();
        base = cal.get(YEAR);
        limit = cal.getActualMinimum(YEAR);
        year = cal.get(YEAR);
        check(year, base, "BuddhistCalendar.getActualMinimum(YEAR)");
    }

    /**
     * 4847186: BuddhistCalendar: toString() returns Gregorian year
     */
    static void testToString() {
        Calendar cal = getBuddhistCalendar();
        int year = cal.get(YEAR);
        String s = cal.toString();
        String y = s.replaceAll(".+,YEAR=(\\d+),.+", "$1");
        if (Integer.parseInt(y) != year) {
            throw new RuntimeException("toString(): wrong year value: got " + y
                                       + ", expected " + year);
        }
    }

    /**
     * 4956479: BuddhistCalendar methods may return wrong values after exception
     */
    static void testException() {
        Calendar cal = getBuddhistCalendar();
        int year = cal.get(YEAR);
        boolean exceptionOccurred = false;
        try {
            cal.add(100, +1); // cause exception
        } catch (Exception e) {
            exceptionOccurred = true;
        }
        if (!exceptionOccurred) {
            throw new RuntimeException("testException: test case failed: no exception thrown");
        }
        int year2 = cal.get(YEAR);
        if (year2 != year) {
            throw new RuntimeException("wrong year value after exception: got " + year2
                                       + ", expected " + year);
        }
    }

    /**
     * 4956227: getLeastMaximum(WEEK_OF_MONTH) return diff. val. for Greg. and Buddhist Calendar
     */
    static void testLeastMax() {
        Calendar bc = getBuddhistCalendar();
        // Specify THAI_LOCALE to get the same params for WEEK
        // calculations (6904680).
        Calendar gc = new GregorianCalendar(THAI_LOCALE);
        for (int f = 0; f < Calendar.FIELD_COUNT; f++) {
            if (f == ERA || f == YEAR) {
                continue;
            }
            int bn = bc.getLeastMaximum(f);
            int gn = gc.getLeastMaximum(f);
            if (bn != gn) {
                throw new RuntimeException("inconsistent Least Max value for " + Koyomi.getFieldName(f)
                                           + ": Buddhist=" + bn
                                           + ": Gregorian=" + gn);
            }
        }
    }

    /**
     * @return a BuddhistCalendar
     */
    static Calendar getBuddhistCalendar() {
        return Calendar.getInstance(THAI_LOCALE);
    }

    static void check(int got, int expected, String s) {
        if (got != expected) {
            throw new RuntimeException("Failed: " +
                s + ": got:" + got + ", expected:" + expected);
        }
    }
}
