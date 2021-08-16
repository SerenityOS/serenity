/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6452848
 * @summary Make sure that the resolution of (WEKK_OF_MONTH +
 * DAY_OF_WEEK) and (DAY_OF_WEEK_IN_MONTH + DAY_OF_WEEK) works as
 * specified in the API.
 * @key randomness
 */

import java.util.*;
import static java.util.Calendar.*;

public class ResolutionTest {
    static Random rand = new Random();

    public static void main(String[] args) {
        for (int year = 1995; year < 2011; year++) {
            for (int month = JANUARY; month <= DECEMBER; month++) {
                for (int dow = SUNDAY; dow <= SATURDAY; dow++) {
                    test(year, month, dow);
                }
            }
        }
    }

    static void test(int year, int month, int dow) {
        Calendar cal = new GregorianCalendar(year, month, 1);
        int max = cal.getActualMaximum(DAY_OF_MONTH);
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int d = 1; d <= max; d++) {
            cal.clear();
            cal.set(year, month, d);
            if (cal.get(DAY_OF_WEEK) == dow) {
                list.add(d);
            }
        }
        for (int i = 0; i < 100; i++) {
            int nth = rand.nextInt(list.size()); // 0-based
            int day = list.get(nth);
            nth++; // 1-based
            testDayOfWeekInMonth(year, month, nth, dow, day);
        }

        // Put WEEK_OF_MONTH-DAY_OF_MONTH pairs
        list = new ArrayList<Integer>();
        for (int d = 1; d <= max; d++) {
            cal.clear();
            cal.set(year, month, d);
            if (cal.get(DAY_OF_WEEK) == dow) {
                list.add(cal.get(WEEK_OF_MONTH));
                list.add(d);
            }
        }
        for (int i = 0; i < list.size(); i++) {
            int nth = list.get(i++);
            int day = list.get(i);
            testWeekOfMonth(year, month, nth, dow, day);
        }
    }

    static Koyomi cal = new Koyomi();

    static void testDayOfWeekInMonth(int year, int month, int nth, int dow, int expected) {
        // don't call clear() here
        cal.set(YEAR, year);
        cal.set(MONTH, month);
        // Set DAY_OF_WEEK_IN_MONTH before DAY_OF_WEEK
        cal.set(DAY_OF_WEEK_IN_MONTH, nth);
        cal.set(DAY_OF_WEEK, dow);
        if (!cal.checkDate(year, month, expected)) {
            throw new RuntimeException(String.format("DOWIM: year=%d, month=%d, nth=%d, dow=%d:%s%n",
                                                     year, month+1, nth, dow, cal.getMessage()));
        }
    }

    static void testWeekOfMonth(int year, int month, int nth, int dow, int expected) {
        // don't call clear() here
        cal.set(YEAR, year);
        cal.set(MONTH, month);
        // Set WEEK_OF_MONTH before DAY_OF_WEEK
        cal.set(WEEK_OF_MONTH, nth);
        cal.set(DAY_OF_WEEK, dow);
        if (!cal.checkDate(year, month, expected)) {
            throw new RuntimeException(String.format("WOM: year=%d, month=%d, nth=%d, dow=%d:%s%n",
                                                     year, month+1, nth, dow, cal.getMessage()));
        }
    }
}
