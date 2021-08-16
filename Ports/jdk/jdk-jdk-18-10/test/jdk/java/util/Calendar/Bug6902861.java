/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6902861
 * @summary Test for a workaround when WEEK_OF_YEAR and YEAR are out of sync.
 * @modules jdk.localedata
 */

import java.util.*;
import static java.util.GregorianCalendar.*;

public class Bug6902861 {
    static int errors = 0;

    public static void main(String [] args) {
        Locale loc = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            test(2010, JANUARY, 1, +1, 1);
            test(2010, JANUARY, 1, +2, 2);
            test(2010, JANUARY, 1, -1, 52);
            test(2010, JANUARY, 1, -2, 51);
            test(2008, DECEMBER, 31, +1, 1);
            test(2008, DECEMBER, 31, +2, 2);
            test(2008, DECEMBER, 31, -1, 52);
            test(2008, DECEMBER, 31, -2, 51);
            if (errors > 0) {
                throw new RuntimeException("Failed");
            }
        } finally {
            Locale.setDefault(loc);
        }
    }

    static void test(int year, int month, int dayOfMonth, int amount, int expected) {
        Calendar calendar = new GregorianCalendar(year, month, dayOfMonth);
        int week = calendar.get(WEEK_OF_YEAR); // fix the date
        calendar.roll(WEEK_OF_YEAR, amount);
        int got = calendar.get(WEEK_OF_YEAR);
        int y = calendar.get(YEAR);
        if (got != expected || y != year) {
            String date = String.format("%04d-%02d-%02d", year, month+1, dayOfMonth);
            System.err.printf("%s: roll %+d: got: %d,%2d; expected: %d,%2d%n",
                              date, amount, y, got, year, expected);
            errors++;
        }
    }
}
