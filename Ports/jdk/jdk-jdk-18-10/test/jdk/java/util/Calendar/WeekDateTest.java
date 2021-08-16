/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4267450
 * @summary Unit test for week date support
 */

import java.text.*;
import java.util.*;
import static java.util.GregorianCalendar.*;

public class WeekDateTest {

    // Week dates are in the ISO numbering for day-of-week.
    static int[][][] data = {
        // Calendar year-date, Week year-date
        {{ 2005, 01, 01}, { 2004, 53, 6}},
        {{ 2005, 01, 02}, { 2004, 53, 7}},
        {{ 2005, 12, 31}, { 2005, 52, 6}},
        {{ 2007, 01, 01}, { 2007, 01, 1}},
        {{ 2007, 12, 30}, { 2007, 52, 7}},
        {{ 2007, 12, 31}, { 2008, 01, 1}},
        {{ 2008, 01, 01}, { 2008, 01, 2}},
        {{ 2008, 12, 29}, { 2009, 01, 1}},
        {{ 2008, 12, 31}, { 2009, 01, 3}},
        {{ 2009, 01, 01}, { 2009, 01, 4}},
        {{ 2009, 12, 31}, { 2009, 53, 4}},
        {{ 2010, 01, 03}, { 2009, 53, 7}},
        {{ 2009, 12, 31}, { 2009, 53, 4}},
        {{ 2010, 01, 01}, { 2009, 53, 5}},
        {{ 2010, 01, 02}, { 2009, 53, 6}},
        {{ 2010, 01, 03}, { 2009, 53, 7}},
        {{ 2008, 12, 28}, { 2008, 52, 7}},
        {{ 2008, 12, 29}, { 2009, 01, 1}},
        {{ 2008, 12, 30}, { 2009, 01, 2}},
        {{ 2008, 12, 31}, { 2009, 01, 3}},
        {{ 2009, 01, 01}, { 2009, 01, 4}}
    };

    // Data for leniency test
    static final int[][][] leniencyData = {
        {{ 2008, 12, 28}, { 2009,  0, 7}},
        {{ 2008, 12, 21}, { 2009, -1, 7}},
        {{ 2009,  1,  4}, { 2008, 53, 7}},
    };

    static final int[][] invalidData = {
        { 2010, -1,  1},
        { 2010, 00,  1},
        { 2010, 55,  1},
        { 2010, 03,  0},
        { 2010, 04,  8},
        { 2010, 04, 19},
        { 2010, 05, -1},
    };

    public static void main(String[] args) {
        GregorianCalendar cal = newCalendar();
        for (int[][] dates : data) {
            int[] expected = dates[0];
            int[] weekDate = dates[1];
            // Convert ISO 8601 day-of-week to Calendar.DAY_OF_WEEK.
            int dayOfWeek = getCalendarDayOfWeek(weekDate[2]);

            cal.clear();
            cal.setWeekDate(weekDate[0], weekDate[1], dayOfWeek);
            if (cal.get(YEAR) != expected[0]
                || cal.get(MONTH)+1 != expected[1]
                || cal.get(DAY_OF_MONTH) != expected[2]) {
                String s = String.format("got=%4d-%02d-%02d, expected=%4d-%02d-%02d",
                               cal.get(YEAR), cal.get(MONTH)+1, cal.get(DAY_OF_MONTH),
                               expected[0], expected[1], expected[2]);
                throw new RuntimeException(s);
            }
            if (cal.getWeekYear() != weekDate[0]
                || cal.get(WEEK_OF_YEAR) != weekDate[1]
                || cal.get(DAY_OF_WEEK) != dayOfWeek) {
                String s = String.format(
                    "got=%4d-W%02d-%d, expected=%4d-W%02d-%d (not ISO day-of-week)",
                    cal.getWeekYear(), cal.get(WEEK_OF_YEAR), cal.get(DAY_OF_WEEK),
                    weekDate[0], weekDate[1], dayOfWeek);
                throw new RuntimeException(s);
            }
        }

        // Test getWeeksInWeekYear().
        // If we avoid the first week of January and the last week of
        // December, getWeeksInWeekYear() and
        // getActualMaximum(WEEK_OF_YEAR) values should be the same.
        for (int year = 2000; year <= 2100; year++) {
            cal.clear();
            cal.set(year, JUNE, 1);
            int n = cal.getWeeksInWeekYear();
            if (n != cal.getActualMaximum(WEEK_OF_YEAR)) {
                String s = String.format("getWeeksInWeekYear() = %d, "
                                         + "getActualMaximum(WEEK_OF_YEAR) = %d%n",
                                         n, cal.getActualMaximum(WEEK_OF_YEAR));
                throw new RuntimeException(s);
            }

            cal.setWeekDate(cal.getWeekYear(), 1, MONDAY);
            if (cal.getWeeksInWeekYear() != n) {
                String s = String.format("first day: got %d, expected %d%n",
                                         cal.getWeeksInWeekYear(), n);
                throw new RuntimeException(s);
            }

            cal.setWeekDate(cal.getWeekYear(), n, SUNDAY);
            if (cal.getWeeksInWeekYear() != n) {
                String s = String.format("last day: got %d, expected %d%n",
                                         cal.getWeeksInWeekYear(), n);
                throw new RuntimeException(s);
            }
        }

        // Test lenient mode with out of range values.
        for (int[][] dates : leniencyData) {
            int[] expected = dates[0];
            int[] weekDate = dates[1];
            // Convert ISO 8601 day-of-week to Calendar.DAY_OF_WEEK.
            int dayOfWeek = getCalendarDayOfWeek(weekDate[2]);

            cal.clear();
            cal.setWeekDate(weekDate[0], weekDate[1], dayOfWeek);
            if (cal.get(YEAR) != expected[0]
                || cal.get(MONTH)+1 != expected[1]
                || cal.get(DAY_OF_MONTH) != expected[2]) {
                String s = String.format("got=%4d-%02d-%02d, expected=%4d-%02d-%02d",
                               cal.get(YEAR), cal.get(MONTH)+1, cal.get(DAY_OF_MONTH),
                               expected[0], expected[1], expected[2]);
                throw new RuntimeException(s);
            }
        }

        // Test non-lenient mode
        cal.setLenient(false);
        for (int[] date : invalidData) {
            cal.clear();
            try {
                // Use the raw dayOfWeek value as invalid data
                cal.setWeekDate(date[0], date[1], date[2]);
                String s = String.format("didn't throw an IllegalArgumentException with"
                                         + " %d, %d, %d",date[0], date[1], date[2]);
                throw new RuntimeException(s);
            } catch (IllegalArgumentException e) {
                // OK
            }
        }
    }

    private static GregorianCalendar newCalendar() {
        // Use GMT to avoid any surprises related DST transitions.
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        if (!cal.isWeekDateSupported()) {
            throw new RuntimeException("Week dates not supported");
        }
        // Setup the ISO 8601 compatible parameters
        cal.setFirstDayOfWeek(MONDAY);
        cal.setMinimalDaysInFirstWeek(4);
        return cal;
    }

    private static int getCalendarDayOfWeek(int isoDayOfWeek) {
        return (isoDayOfWeek == 7) ? SUNDAY : isoDayOfWeek + 1;
    }
}
