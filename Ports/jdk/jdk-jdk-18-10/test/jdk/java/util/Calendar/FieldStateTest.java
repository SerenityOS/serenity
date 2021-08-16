/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4860664 4916815 4867075
 * @library /java/text/testlib
 * @build Koyomi
 * @run main FieldStateTest
 * @summary Unit tests for internal fields states.
 */

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static java.util.Calendar.*;

public class FieldStateTest extends IntlTest {

    public static void main(String[] args) throws Exception {
        Locale reservedLocale = Locale.getDefault();
        TimeZone reservedTimeZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
            Locale.setDefault(Locale.US);

            new FieldStateTest().run(args);
        } finally {
            // restore the reserved locale and time zone
            Locale.setDefault(reservedLocale);
            TimeZone.setDefault(reservedTimeZone);
        }
    }

    public void TestFieldState() {
        Koyomi cal = new Koyomi();
        logln("Right after instantialtion:");
        if (!cal.checkAllSet()) {
            errln(cal.getMessage());
        }

        logln("Set date to 2003/10/31 after the instantiation:");
        cal.set(2003, OCTOBER, 31);
        // let cal calculate the time
        cal.getTime();
        // At this point, all fields have to be recalculated and
        // happen to have the set-state from the instantiation. The
        // three fields should have "externally set" and the rest of
        // the fields have "computed". But we can't distinguish them
        // outside the package.
        if (!cal.checkAllSet()) {
            errln(cal.getMessage());
        }
        // Make sure that the correct date was produced.
        if (!cal.checkInternalDate(2003, OCTOBER, 31, FRIDAY)) {
            errln(cal.getMessage());
        }

        logln("Change to Monday of the week, which is 2003/10/27:");
        cal.set(DAY_OF_WEEK, MONDAY);
        cal.getTime();
        if (!cal.checkDate(2003, OCTOBER, 27)) {
            errln(cal.getMessage());
        }

        // The same operation didn't work after calling clear() before
        // 1.5 because the set-state was just depends on its previous
        // operations. After the instantiation, all the fields are set
        // to "computed". But after calling clear(), the state becomes
        // "unset".
        logln("Set to 2003/10/31 after clear():");
        cal.clear();
        cal.set(2003, OCTOBER, 31);
        cal.getTime();
        cal.set(DAY_OF_WEEK, MONDAY);
        if (!cal.checkDate(2003, OCTOBER, 27, MONDAY)) {
            errln(cal.getMessage());
        }

        logln("Set to 2003/10/31 after clear(), then to the 51st week of year (12/19):");
        cal.clear();
        cal.set(2003, OCTOBER, 31);
        cal.getTime();
        cal.set(WEEK_OF_YEAR, 51);
        if (!cal.checkFieldValue(WEEK_OF_YEAR, 51)) {
            errln(cal.getMessage());
        }
        if (!cal.checkDate(2003, DECEMBER, 19, FRIDAY)) {
            errln(cal.getMessage());
        }

        logln("Set to 2003/10 Mon of 4th week (10/20: 43rd week of year, 293rd day):");
        cal.clear();
        cal.set(YEAR, 2003);
        cal.set(MONTH, OCTOBER);
        cal.set(DAY_OF_WEEK, MONDAY);
        cal.set(WEEK_OF_MONTH, 4);
        cal.getTime();
        if (!cal.checkFieldValue(DAY_OF_MONTH, 20)) {
            errln(cal.getMessage());
        }
        if (!cal.checkFieldValue(DAY_OF_YEAR, 293)) {
            errln(cal.getMessage());
        }
        if (!cal.checkFieldValue(WEEK_OF_YEAR, 43)) {
            errln(cal.getMessage());
        }

        logln("Set to 2003/10 Mon of 43rd week of year (10/20: 4th week of month, 293rd day):");
        cal.clear();
        cal.set(YEAR, 2003);
        cal.set(DAY_OF_WEEK, MONDAY);
        cal.set(WEEK_OF_YEAR, 43);
        cal.getTime();
        if (!cal.checkDate(2003, OCTOBER, 20, MONDAY)) {
            errln(cal.getMessage());
        }
        if (!cal.checkFieldValue(WEEK_OF_MONTH, 4)) {
            errln(cal.getMessage());
        }
        if (!cal.checkFieldValue(DAY_OF_YEAR, 293)) {
            errln(cal.getMessage());
        }

        logln("Set day of week to SUNDAY and date to 2003/10/31. "
                + "Then, getTime and set week of year to 43.");

        @SuppressWarnings("deprecation")
        Date d = new Date(2003 - 1900, OCTOBER, 31);
        cal.setTime(d);
        cal.set(DAY_OF_WEEK, SUNDAY);
        cal.set(2003, OCTOBER, 31); // 2003/10/31 is Friday.
        cal.set(ZONE_OFFSET, 0);
        cal.set(DST_OFFSET, 0);

        // This call should change the day of week to FRIDAY since the
        // selected field combination should be YEAR, MONTH and
        // DAY_OF_MONTH. The other calendar fields must be normalized
        // with the selected date.
        cal.getTime();
        cal.set(WEEK_OF_YEAR, 43);
        if (!cal.checkDate(2003, OCTOBER, 24, FRIDAY)) {
            errln(cal.getMessage());
        }
    }

    /*
     * 4916815: REGRESSION: Problem with java.util.Calendar VM 1.4.2-b28
     */
    public void Test4916815() {
        logln("Set date to 2003/9/26 (Fri). Roll to Aug and back to Sep. "
                + "Set dayofweek to Sunday which should be 2003/9/21.");
        Koyomi cal = new Koyomi();
        cal.clear();
        // 2003/9/26 (Fri)
        cal.set(2003, SEPTEMBER, 26);
        // Go to August then back to September
        cal.roll(MONTH, -1);
        cal.roll(MONTH, +1);
        Koyomi cal2 = (Koyomi) cal.clone();
        cal2.getTime();
        // Sunday of the week should be 2003/9/21.
        cal2.set(DAY_OF_WEEK, SUNDAY);
        if (!cal2.checkDate(2003, SEPTEMBER, 21, SUNDAY)) {
            errln(cal2.getMessage());
        }
    }

    /*
     * 4867075: GregorianCalendar get() calls complete() internally, should getTime() too?
     */
    public void Test4867075() {
        Koyomi cal = new Koyomi(Locale.US);
        cal.clear();
        cal.set(YEAR, 2004);
        cal.set(WEEK_OF_YEAR, 1);
        checkDate(cal, SUNDAY, 2003, DECEMBER, 28);
        checkDate(cal, MONDAY, 2003, DECEMBER, 29);
        checkDate(cal, TUESDAY, 2003, DECEMBER, 30);
        checkDate(cal, WEDNESDAY, 2003, DECEMBER, 31);
        checkDate(cal, THURSDAY, 2004, JANUARY, 1);
        checkDate(cal, FRIDAY, 2004, JANUARY, 2);
        checkDate(cal, SATURDAY, 2004, JANUARY, 3);
    }

    private void checkDate(Koyomi cal, int dayOfWeek,
            int expectedYear, int expectedMonth, int expectedDayOfMonth) {
        cal.set(DAY_OF_WEEK, dayOfWeek);
        cal.getTime();
        if (!cal.checkInternalDate(expectedYear, expectedMonth, expectedDayOfMonth, dayOfWeek)) {
            errln(cal.getMessage());
        }
    }

    static String toHexString(int x) {
        return Integer.toHexString(x);
    }
}
