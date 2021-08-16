/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4514831
 * @summary Confirm that GregorianCalendar.roll() works properly during transition from Daylight Saving Time to Standard Time.
 */

import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import static java.util.GregorianCalendar.*;


public class bug4514831 {

    public static void main(String[] args) {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedTimeZone = TimeZone.getDefault();
        boolean err = false;

        String golden_data1 = "27-28 28-29 29-30 30-31 31-1 1-2 2-3 ";
        String golden_data2 = "27-28 28-29 29-30 30-31 31-25 25-26 26-27 ";
        String golden_data3 = "1-8 8-15 15-22 22-29 29-1 1-8 8-15 ";

        try {
            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("US/Pacific"));

            String test_roll = "";
            GregorianCalendar c_roll = new GregorianCalendar(2001, OCTOBER, 27);
            for (int i = 0; i < 7; i++) {
                test_roll += c_roll.get(DAY_OF_MONTH) + "-";
                c_roll.roll(DAY_OF_YEAR, true);
                test_roll += c_roll.get(DAY_OF_MONTH) + " ";
            }
            if (!test_roll.equals(golden_data1)) {
                err = true;
                System.err.println("Wrong roll(DAY_OF_YEAR) transition: got "
                        + test_roll + "expected " + golden_data1);
            }

            test_roll = "";
            c_roll = new GregorianCalendar(2001, OCTOBER, 27);
            c_roll.setFirstDayOfWeek(THURSDAY);
            for (int i = 0; i < 7; i++) {
                test_roll += c_roll.get(DAY_OF_MONTH) + "-";
                c_roll.roll(DAY_OF_WEEK, true);
                test_roll += c_roll.get(DAY_OF_MONTH) + " ";
            }
            if (!test_roll.equals(golden_data2)) {
                err = true;
                System.err.println("Wrong roll(DAY_OF_WEEK) transition: got "
                        + test_roll + "expected " + golden_data2);
            }

            test_roll = "";
            c_roll = new GregorianCalendar(2001, OCTOBER, 1);
            for (int i = 0; i < 7; i++) {
                test_roll += c_roll.get(DAY_OF_MONTH) + "-";
                c_roll.roll(DAY_OF_WEEK_IN_MONTH, true);
                test_roll += c_roll.get(DAY_OF_MONTH) + " ";
            }
            if (!test_roll.equals(golden_data3)) {
                err = true;
                System.err.println("Wrong roll(DAY_OF_WEEK_IN_MONTH) transition: got "
                        + test_roll + "expected " + golden_data3);
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedTimeZone);
        }

        if (err) {
            throw new RuntimeException("Wrong roll() transition");
        }
    }
}
