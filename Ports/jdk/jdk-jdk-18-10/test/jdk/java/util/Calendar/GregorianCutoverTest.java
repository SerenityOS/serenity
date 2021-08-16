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
 * @bug 4359204 4928615 4743587 4956232 6459836 6549953
 * @library /java/text/testlib
 * @build Koyomi
 * @run main GregorianCutoverTest
 * @summary Unit tests related to the Gregorian cutover support.
 */

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static java.util.GregorianCalendar.*;

public class GregorianCutoverTest extends IntlTest {

    public static void main(String[] args) throws Exception {
        TimeZone tz = TimeZone.getDefault();
        Locale lc = Locale.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
            Locale.setDefault(Locale.US);

            new GregorianCutoverTest().run(args);
        } finally {
            TimeZone.setDefault(tz);
            Locale.setDefault(lc);
        }
    }

    /**
     * 4359204: GregorianCalendar.get(cal.DAY_OF_YEAR) is inconsistent for year 1582
     */
    public void Test4359204() {
        Koyomi cal = new Koyomi();

        cal.set(1582, JANUARY, 1);
        checkContinuity(cal, DAY_OF_YEAR);
        checkContinuity(cal, WEEK_OF_YEAR);
        cal.set(1582, OCTOBER, 1);
        checkContinuity(cal, WEEK_OF_MONTH);

        // JCK tests the cutover date 1970-1-1 (Epoch)
        cal.setGregorianChange(new Date(0));
        cal.set(1969, JANUARY, 1);
        checkContinuity(cal, DAY_OF_YEAR);
        checkContinuity(cal, WEEK_OF_YEAR);
        cal.set(1969, DECEMBER, 1);
        checkContinuity(cal, WEEK_OF_MONTH);
        cal.set(1970, JANUARY, 1);
        checkContinuity(cal, DAY_OF_YEAR);
        checkContinuity(cal, WEEK_OF_YEAR);

        // Use large date (year >= 50000)
        @SuppressWarnings("deprecation")
        Date d = new Date(50000 - 1900, JANUARY, 20);
        cal.setGregorianChange(d);
        cal.set(49998, JANUARY, 1);
        checkContinuity(cal, DAY_OF_YEAR);
        checkContinuity(cal, WEEK_OF_YEAR);
        cal.set(49999, JANUARY, 1);
        checkContinuity(cal, DAY_OF_YEAR);
        checkContinuity(cal, WEEK_OF_YEAR);
        cal.set(50000, JANUARY, 20);
        checkContinuity(cal, DAY_OF_YEAR);
        checkContinuity(cal, WEEK_OF_YEAR);

        // Handling of "overlapping" dates may still be incorrect as
        // of 1.5. Also, there's no way to disambiguate "overlapping"
        // dates.
        // millis=-112033929600000: date=-1581-10-15T00:00:00.000Z
        cal.setGregorianChange(new Date(-112033929600000L));
        cal.set(ERA, AD);
        cal.set(-1581, JANUARY, 1);
        // The year should have 379 days.
        checkContinuity(cal, DAY_OF_YEAR);
        checkContinuity(cal, WEEK_OF_YEAR);

        logln("Default cutover");
        cal = new Koyomi();
        cal.set(1582, OCTOBER, 1);
        logln("  roll --DAY_OF_MONTH from 1582/10/01");
        cal.roll(DAY_OF_MONTH, -1);
        if (!cal.checkDate(1582, OCTOBER, 31)) {
            errln(cal.getMessage());
        }
        logln("  roll DAY_OF_MONTH+10 from 1582/10/31");
        cal.roll(DAY_OF_MONTH, +10);
        if (!cal.checkDate(1582, OCTOBER, 20)) {
            errln(cal.getMessage());
        }
        logln("  roll DAY_OF_MONTH-10 from 1582/10/20");
        cal.roll(DAY_OF_MONTH, -10);
        if (!cal.checkDate(1582, OCTOBER, 31)) {
            errln(cal.getMessage());
        }
        logln("  roll back one day further");
        cal.roll(DAY_OF_MONTH, +1);
        if (!cal.checkDate(1582, OCTOBER, 1)) {
            errln(cal.getMessage());
        }

        // should handle the gap between 1969/12/22 (Julian) to 1970/1/5 (Gregorian)
        logln("Cutover date is 1970/1/5");
        @SuppressWarnings("deprecation")
        Date d1 = new Date(1970 - 1900, JANUARY, 5);
        cal.setGregorianChange(d1);
        cal.set(ERA, AD);
        cal.set(YEAR, 1970);
        logln("  Set DAY_OF_YEAR to the 28th day of 1970");
        cal.set(DAY_OF_YEAR, 28);
        if (!cal.checkDate(1970, FEBRUARY, 1)) {
            errln(cal.getMessage());
        }
        if (!cal.checkFieldValue(WEEK_OF_YEAR, 5)) {
            errln(cal.getMessage());
        }
        logln("  1969/12/22 should be the 356th day of the year.");
        cal.set(1969, DECEMBER, 22);
        if (!cal.checkFieldValue(DAY_OF_YEAR, 356)) {
            errln(cal.getMessage());
        }
        logln("  Set DAY_OF_YEAR to autual maximum.");
        int actualMaxDayOfYear = cal.getActualMaximum(DAY_OF_YEAR);
        if (actualMaxDayOfYear != 356) {
            errln("actual maximum of DAY_OF_YEAR: got " + actualMaxDayOfYear + ", expected 356");
        }
        cal.set(DAY_OF_YEAR, actualMaxDayOfYear);
        if (!cal.checkDate(1969, DECEMBER, 22)) {
            errln(cal.getMessage());
        }
        cal.set(1969, DECEMBER, 22);
        cal.roll(DAY_OF_YEAR, +1);
        logln("  Set to 1969/12/22 and roll DAY_OF_YEAR++");
        if (!cal.checkDate(1969, JANUARY, 1)) {
            errln(cal.getMessage());
        }
        logln("  1970/1/5 should be the first day of the year.");
        cal.set(1970, JANUARY, 5);
        if (!cal.checkFieldValue(DAY_OF_YEAR, 1)) {
            errln(cal.getMessage());
        }
        logln("  roll --DAY_OF_MONTH from 1970/1/5");
        cal.roll(DAY_OF_MONTH, -1);
        if (!cal.checkDate(1970, JANUARY, 31)) {
            errln(cal.getMessage());
        }
        logln("  roll back one day of month");
        cal.roll(DAY_OF_MONTH, +1);
        if (!cal.checkDate(1970, JANUARY, 5)) {
            errln(cal.getMessage());
        }

        // Test "missing" dates in non-lenient.
        cal = new Koyomi(); // new instance for the default cutover
        cal.setLenient(false);
        try {
            // the next day of 1582/10/4 (Julian) is 1582/10/15 (Gregorian)
            logln("1582/10/10 doesn't exit with the default cutover.");
            cal.set(1582, OCTOBER, 10);
            cal.getTime();
            errln("    Didn't throw IllegalArgumentException in non-lenient.");
        } catch (IllegalArgumentException e) {
        }
    }

    private void checkContinuity(Koyomi cal, int field) {
        cal.getTime();
        logln(Koyomi.getFieldName(field) + " starting on " + cal.toDateString());
        int max = cal.getActualMaximum(field);
        for (int i = 1; i <= max; i++) {
            logln(i + "    " + cal.toDateString());
            if (!cal.checkFieldValue(field, i)) {
                errln("    " + cal.toDateString() + ":\t" + cal.getMessage());
            }
            cal.add(field, +1);
        }
    }

    /**
     * 4928615: GregorianCalendar returns wrong dates after setGregorianChange
     */
    public void Test4928615() {
        Koyomi cal = new Koyomi();
        logln("Today is 2003/10/1 Gregorian.");
        @SuppressWarnings("deprecation")
        Date x = new Date(2003 - 1900, 10 - 1, 1);
        cal.setTime(x);

        logln("  Changing the cutover date to yesterday...");
        cal.setGregorianChange(new Date(x.getTime() - (24 * 3600 * 1000)));
        if (!cal.checkDate(2003, OCTOBER, 1)) {
            errln("    " + cal.getMessage());
        }
        logln("  Changing the cutover date to tomorrow...");
        cal.setGregorianChange(new Date(x.getTime() + (24 * 3600 * 1000)));
        if (!cal.checkDate(2003, SEPTEMBER, 18)) {
            errln("    " + cal.getMessage());
        }
    }

    /**
     * 4743587: GregorianCalendar.getLeastMaximum() returns wrong values
     */
    public void Test4743587() {
        Koyomi cal = new Koyomi();
        Koyomi cal2 = (Koyomi) cal.clone();
        logln("getLeastMaximum should handle cutover year.\n"
                + "  default cutover date");
        if (!cal.checkLeastMaximum(DAY_OF_YEAR, 365 - 10)) {
            errln("    " + cal.getMessage());
        }
        if (!cal.checkLeastMaximum(WEEK_OF_YEAR, 52 - ((10 + 6) / 7))) {
            errln("    " + cal.getMessage());
        }
        // Corrected for 4956232
        if (!cal.checkLeastMaximum(DAY_OF_MONTH, 28)) {
            errln("    " + cal.getMessage());
        }
        if (!cal.checkLeastMaximum(WEEK_OF_MONTH, 3)) {
            errln("    " + cal.getMessage());
        }
        if (!cal.checkLeastMaximum(DAY_OF_WEEK_IN_MONTH, 3)) {
            errln("    " + cal.getMessage());
        }
        // make sure that getLeastMaximum calls didn't affect the date
        if (!cal.equals(cal2)) {
            errln("    getLeastMaximum calls modified the object.");
        }
        if (!cal.checkGreatestMinimum(DAY_OF_MONTH, 1)) {
            errln("    " + cal.getMessage());
        }

        logln("  changing the date to 1582/10/20 for actual min/max tests");
        cal.set(1582, OCTOBER, 20);
        if (!cal.checkActualMinimum(DAY_OF_MONTH, 1)) {
            errln("    " + cal.getMessage());
        }
        if (!cal.checkActualMaximum(DAY_OF_MONTH, 31)) {
            errln("    " + cal.getMessage());
        }

        cal = new Koyomi();
        logln("Change the cutover date to 1970/1/5.");
        @SuppressWarnings("deprecation")
        Date d = new Date(1970 - 1900, 0, 5);
        cal.setGregorianChange(d);
        if (!cal.checkLeastMaximum(DAY_OF_YEAR, 356)) {
            errln("    " + cal.getMessage());
        }
        if (!cal.checkLeastMaximum(DAY_OF_MONTH, 22)) {
            errln("    " + cal.getMessage());
        }
        if (!cal.checkGreatestMinimum(DAY_OF_MONTH, 5)) {
            errln("    " + cal.getMessage());
        }
        cal.set(1970, JANUARY, 10);
        if (!cal.checkActualMinimum(DAY_OF_MONTH, 5)) {
            errln("    " + cal.getMessage());
        }
        if (!cal.checkActualMaximum(DAY_OF_MONTH, 31)) {
            errln("    " + cal.getMessage());
        }
    }

    /**
     * 6459836: (cal) GregorianCalendar set method provides wrong result
     */
    public void Test6459836() {
        int hour = 13865672;
        Koyomi gc1 = new Koyomi();
        gc1.clear();
        gc1.set(1, JANUARY, 1, 0, 0, 0);
        gc1.set(HOUR_OF_DAY, hour);
        if (!gc1.checkDate(1582, OCTOBER, 4)) {
            errln("test case 1: " + gc1.getMessage());
        }
        gc1.clear();
        gc1.set(1, JANUARY, 1, 0, 0, 0);
        gc1.set(HOUR_OF_DAY, hour + 24);
        if (!gc1.checkDate(1582, OCTOBER, 15)) {
            errln("test case 2: " + gc1.getMessage());
        }
    }

    /**
     * 6549953 (cal) WEEK_OF_YEAR and DAY_OF_YEAR calculation problems around Gregorian cutover
     */
    public void Test6549953() {
        Koyomi cal = new Koyomi();

        cal.set(YEAR, 1582);
        cal.set(WEEK_OF_YEAR, 42);
        cal.set(DAY_OF_WEEK, FRIDAY);
        cal.checkFieldValue(WEEK_OF_YEAR, 42);
        cal.checkFieldValue(DAY_OF_WEEK, FRIDAY);
        if (!cal.checkDate(1582, OCTOBER, 29)) {
            errln(cal.getMessage());
        }
        cal.clear();
        cal.set(1582, OCTOBER, 1);
        cal.set(DAY_OF_YEAR, 292);
        if (!cal.checkDate(1582, OCTOBER, 29)) {
            errln(cal.getMessage());
        }
    }
}
