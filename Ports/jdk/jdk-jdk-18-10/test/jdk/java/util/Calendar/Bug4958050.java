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
 * @bug 4958050
 * @summary Make sure that add() and roll() handle time zone offset changes (both raw and DST) correctly.
 */

import java.util.Locale;
import java.util.TimeZone;

import static java.util.Calendar.*;

public class Bug4958050 {

    static int errorCount = 0;

    public static void main(String[] args) {
        // All the test cases depend on historical GMT offset changes
        // of Asia/Novosibirsk.
        Koyomi cal = new Koyomi(TimeZone.getTimeZone("Asia/Novosibirsk"), Locale.US);
        System.out.println("Time zone = " + cal.getTimeZone().getID());

        // Test the week fields
        int[] weekFields = {WEEK_OF_YEAR, WEEK_OF_MONTH, DAY_OF_WEEK_IN_MONTH};
        for (int i = 0; i < weekFields.length; i++) {
            int field = weekFields[i];
            // add()
            cal.clear();
            cal.set(1919, DECEMBER, 14 - 7, 23, 50, 00);
            cal.add(weekFields[i], +1);
            if (!cal.checkDate(1919, DECEMBER, 14)) {
                error("1919/12/07: add(" + Koyomi.getFieldName(weekFields[i]) + ", +1)\n"
                        + cal.getMessage() + " " + cal.toDateTimeString());
            }
            cal.clear();
            cal.set(1930, JUNE, 21 - 7);
            cal.add(weekFields[i], +1);
            if (!cal.checkDateTime(1930, JUNE, 21, 01, 00, 00, 000)) {
                error("1930/6/14: add(" + Koyomi.getFieldName(weekFields[i]) + ", +1)\n"
                        + cal.getMessage() + " " + cal.toDateTimeString());
            }

            // roll()
            cal.clear();
            cal.set(1919, DECEMBER, 14 - 7, 23, 50, 00);
            cal.roll(weekFields[i], +1);
            if (!cal.checkDate(1919, DECEMBER, 14)) {
                error("1919/12/07: roll(" + Koyomi.getFieldName(weekFields[i]) + ", +1)\n"
                        + cal.getMessage() + " " + cal.toDateTimeString());
            }
            cal.clear();
            cal.set(1930, JUNE, 21 - 7);
            cal.roll(weekFields[i], +1);
            if (!cal.checkDateTime(1930, JUNE, 21, 01, 00, 00, 000)) {
                error("1930/6/14: roll(" + Koyomi.getFieldName(weekFields[i]) + ", +1)\n"
                        + cal.getMessage() + " " + cal.toDateTimeString());
            }
        }

        // Test the day fields
        int[] dayFields = {DAY_OF_MONTH, DAY_OF_YEAR, DAY_OF_WEEK};
        for (int i = 0; i < dayFields.length; i++) {
            int field = dayFields[i];
            // add()
            cal.clear();
            cal.set(1919, DECEMBER, 14 - 1, 23, 50, 00);
            cal.add(field, +1);
            if (!cal.checkDate(1919, DECEMBER, 14)) {
                error("1919/12/13: add(" + Koyomi.getFieldName(field) + ", +1)\n"
                        + cal.getMessage() + " " + cal.toDateTimeString());
            }
            cal.clear();
            cal.set(1919, DECEMBER, 14, 00, 00, 00);
            cal.add(field, -1);
            if (!cal.checkDate(1919, DECEMBER, 13)) {
                error("1919/12/14: add(" + Koyomi.getFieldName(field) + ", -1)\n"
                        + cal.getMessage() + " " + cal.toDateTimeString());
            }
            cal.clear();
            cal.set(1930, JUNE, 21 - 1);
            cal.add(field, +1);
            if (!cal.checkDateTime(1930, JUNE, 21, 01, 00, 00, 000)) {
                error("1930/6/20: add(" + Koyomi.getFieldName(field) + ", +1)\n"
                        + cal.getMessage() + cal.toDateTimeString());
            }
            cal.clear();
            cal.set(1930, JUNE, 21, 01, 00, 00);
            cal.add(field, -1);
            if (!cal.checkDateTime(1930, JUNE, 20, 01, 00, 00, 000)) {
                error("1930/6/21: add(" + Koyomi.getFieldName(field) + ", -1)\n"
                        + cal.getMessage() + " " + cal.toDateTimeString());
            }

            // roll()
            cal.clear();
            cal.set(1930, JUNE, 21 - 1);
            int amount = +1;
            if (field == DAY_OF_WEEK) {
                amount += 700;
            }
            cal.roll(field, amount);
            if (!cal.checkDateTime(1930, JUNE, 21, 01, 00, 00, 000)) {
                error("1930/6/20: roll(" + Koyomi.getFieldName(field) + ", +" + amount + ")\n"
                        + cal.getMessage() + " " + cal.toDateTimeString());
            }
            cal.clear();
            cal.set(1930, JUNE, 21, 01, 00, 00);
            amount = -1;
            if (field == DAY_OF_WEEK) {
                amount -= 700;
            }
            cal.roll(field, amount);
            if (!cal.checkDateTime(1930, JUNE, 20, 01, 00, 00, 000)) {
                error("1930/6/21: roll(" + Koyomi.getFieldName(field) + ", " + amount + ")\n"
                        + cal.getMessage() + " " + cal.toDateTimeString());
            }
        }

        // Test the AM_PM field
        // add()
        cal.clear();
        cal.set(1919, DECEMBER, 14 - 1, 23, 50, 00);
        cal.add(AM_PM, +1);
        if (!cal.checkDate(1919, DECEMBER, 14)
                || !cal.checkFieldValue(AM_PM, AM)) {
            error("1919/12/13: add(AM_PM, +1)\n"
                    + cal.getMessage() + " " + cal.toDateTimeString());
        }

        cal.clear();
        cal.set(1930, JUNE, 21 - 1, 12, 00, 00);
        cal.add(AM_PM, +1);
        if (!cal.checkDate(1930, JUNE, 21)
                || !cal.checkFieldValue(AM_PM, AM)) {
            error("1930/6/20: add(AM_PM, +1)\n"
                    + cal.getMessage() + " " + cal.toDateTimeString());
        }

        cal.clear();
        cal.set(1930, JUNE, 21 - 2, 12, 00, 00);
        cal.add(AM_PM, +3);
        if (!cal.checkDate(1930, JUNE, 21)
                || !cal.checkFieldValue(AM_PM, AM)) {
            error("1930/6/10: add(AM_PM, +3)\n"
                    + cal.getMessage() + " " + cal.toDateTimeString());
        }

        cal.clear();
        cal.set(1919, DECEMBER, 14, 11, 50, 00);
        cal.add(AM_PM, -1);
        if (!cal.checkDateTime(1919, DECEMBER, 14 - 1, 23, 50, 00, 000)
                || !cal.checkFieldValue(AM_PM, PM)) {
            error("1919/12/14 11:50:00: add(AM_PM, -1)\n"
                    + cal.getMessage() + " " + cal.toDateTimeString());
        }

        cal.clear();
        cal.set(1930, JUNE, 21, 01, 00, 00);
        cal.add(AM_PM, -1);
        if (!cal.checkDateTime(1930, JUNE, 21 - 1, 01 + 12, 00, 00, 000)
                || !cal.checkFieldValue(AM_PM, PM)) {
            error("1930/6/20: add(AM_PM, -1)\n"
                    + cal.getMessage() + " " + cal.toDateTimeString());
        }

        cal.clear();
        cal.set(1930, JUNE, 21, 01, 00, 00);
        cal.add(AM_PM, -3);
        if (!cal.checkDateTime(1930, JUNE, 21 - 2, 01 + 12, 00, 00, 000)
                || !cal.checkFieldValue(AM_PM, PM)) {
            error("1930/6/10: add(AM_PM, -3)\n"
                    + cal.getMessage() + " " + cal.toDateTimeString());
        }

        // roll() (should NOT change the date)
        cal.clear();
        cal.set(1919, DECEMBER, 14 - 1, 23, 50, 00);
        cal.roll(AM_PM, +1);
        if (!cal.checkDateTime(1919, DECEMBER, 14 - 1, 23 - 12, 50, 00, 000)
                || !cal.checkFieldValue(AM_PM, AM)) {
            error("1919/12/13: roll(AM_PM, +1)\n"
                    + cal.getMessage() + " " + cal.toDateTimeString());
        }

        cal.clear();
        cal.set(1930, JUNE, 21 - 1, 12, 00, 00);
        cal.roll(AM_PM, +1);
        if (!cal.checkDateTime(1930, JUNE, 21 - 1, 12 - 12, 00, 00, 000)
                || !cal.checkFieldValue(AM_PM, AM)) {
            error("1930/6/20: roll(AM_PM, +1)\n"
                    + cal.getMessage() + " " + cal.toDateTimeString());
        }

        cal.clear();
        cal.set(1930, JUNE, 21 - 2, 12, 00, 00);
        cal.roll(AM_PM, +3);
        if (!cal.checkDateTime(1930, JUNE, 21 - 2, 12 - 12, 00, 00, 000)
                || !cal.checkFieldValue(AM_PM, AM)) {
            error("1930/6/10: roll(AM_PM, +3)\n"
                    + cal.getMessage() + " " + cal.toDateTimeString());
        }

        // Test the HOUR_OF_DAY field
        // add()
        cal.clear();
        cal.set(1930, JUNE, 20, 23, 00, 00);
        cal.add(HOUR_OF_DAY, +1);
        if (!cal.checkDateTime(1930, JUNE, 21, 01, 00, 00, 000)) {
            error("1930/6/20 23:00:00: add(HOUR_OF_DAY, +1)\n"
                    + cal.getMessage() + " " + cal.toDateTimeString());
        }

        // roll() (should NOT change the date)
        cal.clear();
        cal.set(1930, JUNE, 20, 23, 00, 00);
        cal.roll(HOUR_OF_DAY, +1);
        if (!cal.checkDateTime(1930, JUNE, 20, 00, 00, 00, 000)) {
            error("1930/6/20 23:00:00: roll(HOUR_OF_DAY, +1)\n"
                    + cal.getMessage() + " " + cal.toDateTimeString());
        }

        checkErrors();
    }

    static void error(String s) {
        System.out.println(s);
        errorCount++;
    }

    static void checkErrors() {
        if (errorCount > 0) {
            throw new RuntimeException("Failed: " + errorCount + " error(s)");
        }
    }
}
