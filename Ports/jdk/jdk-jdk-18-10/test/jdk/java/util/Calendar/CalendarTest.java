/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4064654 4374886 4984320 4984574 4944795 8210142
 * @summary test for Calendar
 * @library /java/text/testlib
 * @modules java.base/java.util:+open
 * @run main CalendarTest
 * @key randomness
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import static java.util.Calendar.*;

public class CalendarTest extends IntlTest {

    static final int ONE_DAY = 24 * 60 * 60 * 1000;
    static final int EPOCH_JULIAN = 2440588;

    public static void main(String argv[]) throws Exception {
        new CalendarTest().run(argv);
    }

    /**
     * Test the behavior of the GregorianCalendar around the changeover.
     */
    public void TestGregorianChangeover() {
        TimeZone savedZone = TimeZone.getDefault();
        /*
          Changeover -7 days: 1582/9/28 dow=6
          Changeover -6 days: 1582/9/29 dow=7
          Changeover -5 days: 1582/9/30 dow=1
          Changeover -4 days: 1582/10/1 dow=2
          Changeover -3 days: 1582/10/2 dow=3
          Changeover -2 days: 1582/10/3 dow=4
          Changeover -1 days: 1582/10/4 dow=5
          Changeover +0 days: 1582/10/15 dow=6
          Changeover +1 days: 1582/10/16 dow=7
          Changeover +2 days: 1582/10/17 dow=1
          Changeover +3 days: 1582/10/18 dow=2
          Changeover +4 days: 1582/10/19 dow=3
          Changeover +5 days: 1582/10/20 dow=4
          Changeover +6 days: 1582/10/21 dow=5
          Changeover +7 days: 1582/10/22 dow=6
          */
        int[] MON = {  9,  9,  9,10,10,10,10, 10, 10, 10, 10, 10, 10, 10, 10 };
        int[] DOM = { 28, 29, 30, 1, 2, 3, 4, 15, 16, 17, 18, 19, 20, 21, 22 };
        int[] DOW = {  6,  7,  1, 2, 3, 4, 5,  6,  7,  1,  2,  3,  4,  5,  6 };
        //                                     ^ <-Changeover Fri Oct 15 1582
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
            @SuppressWarnings("deprecation")
            Date co = new Date(1582 - 1900, OCTOBER, 15);
            GregorianCalendar cal = new GregorianCalendar();
            int j = 0;
            for (int i = -7; i <= 7; ++i, ++j) {
                Date d = new Date(co.getTime() + i * ONE_DAY);
                cal.setTime(d);
                int y = cal.get(YEAR);
                int mon = cal.get(MONTH) + 1 - JANUARY;
                int dom = cal.get(DATE);
                int dow = cal.get(DAY_OF_WEEK);

                logln("Changeover " + (i >= 0 ? "+" : "") + i
                        + " days: " + y + "/" + mon + "/" + dom + " dow=" + dow);
                if (y != 1582 || mon != MON[j] || dom != DOM[j] || dow != DOW[j]) {
                    errln(" Fail: Above line is wrong");
                }
            }
        } finally {
            TimeZone.setDefault(savedZone);
        }
    }

    /**
     * Test the mapping between millis and fields.  For the purposes
     * of this test, we don't care about timezones and week data
     * (first day of week, minimal days in first week).
     */
    @SuppressWarnings("deprecation")
    public void TestMapping() {
        TimeZone saveZone = TimeZone.getDefault();
        int[] DATA = {
            // Julian#   Year      Month    DOM   JULIAN:Year  Month,   DOM
            2440588,     1970,    JANUARY,   1,    1969,     DECEMBER,  19,
            2415080,     1900,      MARCH,   1,    1900,     FEBRUARY,  17,
            2451604,     2000,   FEBRUARY,  29,    2000,     FEBRUARY,  16,
            2452269,     2001,   DECEMBER,  25,    2001,     DECEMBER,  12,
            2416526,     1904,   FEBRUARY,  15,    1904,     FEBRUARY,   2,
            2416656,     1904,       JUNE,  24,    1904,         JUNE,  11,
            1721426,        1,    JANUARY,   1,       1,      JANUARY,   3,
            2000000,      763,  SEPTEMBER,  18,     763,    SEPTEMBER,  14,
            4000000,     6239,       JULY,  12,    6239,          MAY,  28,
            8000000,    17191,   FEBRUARY,  26,   17190,      OCTOBER,  22,
           10000000,    22666,   DECEMBER,  20,   22666,         JULY,   5};

        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            Date PURE_GREGORIAN = new Date(Long.MIN_VALUE);
            Date PURE_JULIAN = new Date(Long.MAX_VALUE);
            GregorianCalendar cal = new GregorianCalendar();
            for (int i = 0; i < DATA.length; i += 7) {
                int julian = DATA[i];
                int year = DATA[i + 1];
                int month = DATA[i + 2];
                int dom = DATA[i + 3];
                int year2, month2, dom2;
                long millis = ((long) julian - EPOCH_JULIAN) * ONE_DAY;
                String s;

                // Test Gregorian computation
                cal.setGregorianChange(PURE_GREGORIAN);
                cal.clear();
                cal.set(year, month, dom);
                long calMillis = cal.getTime().getTime();
                long delta = calMillis - millis;
                cal.setTime(new Date(millis));
                year2 = cal.get(YEAR);
                month2 = cal.get(MONTH);
                dom2 = cal.get(DAY_OF_MONTH);
                s = "G " + year + "-" + (month + 1 - JANUARY) + "-" + dom
                        + " => " + calMillis
                        + " (" + ((float) delta / ONE_DAY) + " day delta) => "
                        + year2 + "-" + (month2 + 1 - JANUARY) + "-" + dom2;
                if (delta != 0 || year != year2 || month != month2
                        || dom != dom2) {
                    errln(s + " FAIL");
                } else {
                    logln(s);
                }

                // Test Julian computation
                year = DATA[i + 4];
                month = DATA[i + 5];
                dom = DATA[i + 6];
                cal.setGregorianChange(PURE_JULIAN);
                cal.clear();
                cal.set(year, month, dom);
                calMillis = cal.getTime().getTime();
                delta = calMillis - millis;
                cal.setTime(new Date(millis));
                year2 = cal.get(YEAR);
                month2 = cal.get(MONTH);
                dom2 = cal.get(DAY_OF_MONTH);
                s = "J " + year + "-" + (month + 1 - JANUARY) + "-" + dom
                        + " => " + calMillis
                        + " (" + ((float) delta / ONE_DAY) + " day delta) => "
                        + year2 + "-" + (month2 + 1 - JANUARY) + "-" + dom2;
                if (delta != 0 || year != year2 || month != month2
                        || dom != dom2) {
                    errln(s + " FAIL");
                } else {
                    logln(s);
                }
            }

            cal.setGregorianChange(new Date(1582 - 1900, OCTOBER, 15));
            auxMapping(cal, 1582, OCTOBER, 4);
            auxMapping(cal, 1582, OCTOBER, 15);
            auxMapping(cal, 1582, OCTOBER, 16);
            for (int y = 800; y < 3000; y += 1 + 100 * Math.random()) {
                for (int m = JANUARY; m <= DECEMBER; ++m) {
                    auxMapping(cal, y, m, 15);
                }
            }
        } finally {
            TimeZone.setDefault(saveZone);
        }
    }
    private void auxMapping(Calendar cal, int y, int m, int d) {
        cal.clear();
        cal.set(y, m, d);
        long millis = cal.getTime().getTime();
        cal.setTime(new Date(millis));
        int year2 = cal.get(YEAR);
        int month2 = cal.get(MONTH);
        int dom2 = cal.get(DAY_OF_MONTH);
        if (y != year2 || m != month2 || dom2 != d) {
            errln("Round-trip failure: " + y + "-" + (m + 1) + "-" + d + " =>ms=> "
                    + year2 + "-" + (month2 + 1) + "-" + dom2);
        }
    }

    @SuppressWarnings("deprecation")
    public void TestGenericAPI() {
        Locale locale = Locale.getDefault();
        if (!TestUtils.usesGregorianCalendar(locale)) {
            logln("Skipping this test because locale is " + locale);
            return;
        }

        String str;
        Date when = new Date(90, APRIL, 15);

        String tzid = "TestZone";
        int tzoffset = 123400;

        SimpleTimeZone zone = new SimpleTimeZone(tzoffset, tzid);
        Calendar cal = Calendar.getInstance((SimpleTimeZone) zone.clone());

        if (!zone.equals(cal.getTimeZone())) {
            errln("FAIL: Calendar.getTimeZone failed");
        }

        Calendar cal2 = Calendar.getInstance(cal.getTimeZone());

        cal.setTime(when);
        cal2.setTime(when);

        if (!(cal.equals(cal2))) {
            errln("FAIL: Calendar.operator== failed");
        }
        // if ((*cal != *cal2))  errln("FAIL: Calendar.operator!= failed");
        if (!cal.equals(cal2)
                || cal.before(cal2)
                || cal.after(cal2)) {
            errln("FAIL: equals/before/after failed");
        }

        cal2.setTime(new Date(when.getTime() + 1000));
        if (cal.equals(cal2)
                || cal2.before(cal)
                || cal.after(cal2)) {
            errln("FAIL: equals/before/after failed");
        }

        cal.roll(SECOND, true);
        if (!cal.equals(cal2)
                || cal.before(cal2)
                || cal.after(cal2)) {
            errln("FAIL: equals/before/after failed");
        }

        // Roll back to January
        cal.roll(MONTH, 1 + DECEMBER - cal.get(MONTH));
        if (cal.equals(cal2)
                || cal2.before(cal)
                || cal.after(cal2)) {
            errln("FAIL: equals/before/after failed");
        }

        // C++ only
        /* TimeZone z = cal.orphanTimeZone();
        if (z.getID(str) != tzid ||
        z.getRawOffset() != tzoffset)
        errln("FAIL: orphanTimeZone failed");
        */
        for (int i = 0; i < 2; ++i) {
            boolean lenient = (i > 0);
            cal.setLenient(lenient);
            if (lenient != cal.isLenient()) {
                errln("FAIL: setLenient/isLenient failed");
            }
            // Later: Check for lenient behavior
        }

        int i;
        for (i = SUNDAY; i <= SATURDAY; ++i) {
            cal.setFirstDayOfWeek(i);
            if (cal.getFirstDayOfWeek() != i) {
                errln("FAIL: set/getFirstDayOfWeek failed");
            }
        }

        for (i = 0; i <= 7; ++i) {
            cal.setMinimalDaysInFirstWeek(i);
            if (cal.getMinimalDaysInFirstWeek() != i) {
                errln("FAIL: set/getFirstDayOfWeek failed");
            }
        }

        for (i = 0; i < FIELD_COUNT; ++i) {
            if (cal.getMinimum(i) != cal.getGreatestMinimum(i)) {
                errln("FAIL: getMinimum doesn't match getGreatestMinimum for field " + i);
            }
            if (cal.getLeastMaximum(i) > cal.getMaximum(i)) {
                errln("FAIL: getLeastMaximum larger than getMaximum for field " + i);
            }
            if (cal.getMinimum(i) >= cal.getMaximum(i)) {
                errln("FAIL: getMinimum not less than getMaximum for field " + i);
            }
        }

        cal.setTimeZone(TimeZone.getDefault());
        cal.clear();
        cal.set(1984, 5, 24);
        if (cal.getTime().getTime() != new Date(84, 5, 24).getTime()) {
            errln("FAIL: Calendar.set(3 args) failed");
            logln(" Got: " + cal.getTime() + "  Expected: " + new Date(84, 5, 24));
        }

        cal.clear();
        cal.set(1985, 3, 2, 11, 49);
        if (cal.getTime().getTime() != new Date(85, 3, 2, 11, 49).getTime()) {
            errln("FAIL: Calendar.set(5 args) failed");
            logln(" Got: " + cal.getTime() + "  Expected: " + new Date(85, 3, 2, 11, 49));
        }

        cal.clear();
        cal.set(1995, 9, 12, 1, 39, 55);
        if (cal.getTime().getTime() != new Date(95, 9, 12, 1, 39, 55).getTime()) {
            errln("FAIL: Calendar.set(6 args) failed");
            logln(" Got: " + cal.getTime() + "  Expected: " + new Date(95, 9, 12, 1, 39, 55));
        }

        cal.getTime();
        for (i = 0; i < FIELD_COUNT; ++i) {
            switch (i) {
                case YEAR:
                case MONTH:
                case DATE:
                case HOUR_OF_DAY:
                case MINUTE:
                case SECOND:
                    if (!cal.isSet(i)) {
                        errln("FAIL: !Calendar.isSet test failed: " + calendarFieldNames[i]);
                    }
                    break;
                default:
                    if (cal.isSet(i)) {
                        errln("FAIL: Calendar.isSet test failed: " + calendarFieldNames[i]);
                    }
            }
            cal.clear(i);
            if (cal.isSet(i)) {
                errln("FAIL: Calendar.clear/isSet failed");
            }
        }

        // delete cal;
        // delete cal2;
        Locale[] loc = Calendar.getAvailableLocales();
        long count = loc.length;
        if (count < 1 || loc == null) {
            errln("FAIL: getAvailableLocales failed");
        } else {
            for (i = 0; i < count; ++i) {
                cal = Calendar.getInstance(loc[i]);
                // delete cal;
            }
        }

        cal = Calendar.getInstance(TimeZone.getDefault(), Locale.ENGLISH);
        // delete cal;

        cal = Calendar.getInstance(zone, Locale.ENGLISH);
        // delete cal;

        GregorianCalendar gc = new GregorianCalendar(zone);
        // delete gc;

        gc = new GregorianCalendar(Locale.ENGLISH);
        // delete gc;

        gc = new GregorianCalendar(Locale.ENGLISH);
        // delete gc;

        gc = new GregorianCalendar(zone, Locale.ENGLISH);
        // delete gc;

        gc = new GregorianCalendar(zone);
        // delete gc;

        gc = new GregorianCalendar(1998, 10, 14, 21, 43);
        if (gc.getTime().getTime() != new Date(98, 10, 14, 21, 43).getTime()) {
            errln("FAIL: new GregorianCalendar(ymdhm) failed");
        }
        // delete gc;

        gc = new GregorianCalendar(1998, 10, 14, 21, 43, 55);
        if (gc.getTime().getTime() != new Date(98, 10, 14, 21, 43, 55).getTime()) {
            errln("FAIL: new GregorianCalendar(ymdhms) failed");
        }

        // C++ only:
        // GregorianCalendar gc2 = new GregorianCalendar(Locale.ENGLISH);
        // gc2 = gc;
        // if (gc2 != gc || !(gc2 == gc)) errln("FAIL: GregorianCalendar assignment/operator==/operator!= failed");
        // delete gc;
        // delete z;
    }

    // Verify Roger Webster's bug
    public void TestRog() {
        GregorianCalendar gc = new GregorianCalendar();

        int year = 1997, month = APRIL, date = 1;
        gc.set(year, month, date); // April 1, 1997

        gc.set(HOUR_OF_DAY, 23);
        gc.set(MINUTE, 0);
        gc.set(SECOND, 0);
        gc.set(MILLISECOND, 0);

        for (int i = 0; i < 9; i++, gc.add(DATE, 1)) {
            if (gc.get(YEAR) != year
                    || gc.get(MONTH) != month
                    || gc.get(DATE) != (date + i)) {
                errln("FAIL: Date " + gc.getTime() + " wrong");
            }
        }
    }

    // Verify DAY_OF_WEEK
    public void TestDOW943() {
        dowTest(false);
        dowTest(true);
    }

    void dowTest(boolean lenient) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(1997, AUGUST, 12); // Wednesday
        cal.getTime(); // Force update
        cal.setLenient(lenient);
        cal.set(1996, DECEMBER, 1); // Set the date to be December 1, 1996
        int dow = cal.get(DAY_OF_WEEK);
        int min = cal.getMinimum(DAY_OF_WEEK);
        int max = cal.getMaximum(DAY_OF_WEEK);
        if (dow < min || dow > max) {
            errln("FAIL: Day of week " + dow + " out of range");
        }
        if (dow != SUNDAY) {
            errln("FAIL2: Day of week should be SUNDAY; is " + dow + ": " + cal.getTime());
        }
        if (min != SUNDAY || max != SATURDAY) {
            errln("FAIL: Min/max bad");
        }
    }

    // Verify that the clone method produces distinct objects with no
    // unintentionally shared fields.
    public void TestClonesUnique908() {
        Calendar c = Calendar.getInstance();
        Calendar d = (Calendar) c.clone();
        c.set(MILLISECOND, 123);
        d.set(MILLISECOND, 456);
        if (c.get(MILLISECOND) != 123
                || d.get(MILLISECOND) != 456) {
            errln("FAIL: Clones share fields");
        }
    }

    // Verify effect of Gregorian cutoff value
    @SuppressWarnings("deprecation")
    public void TestGregorianChange768() {
        boolean b;
        GregorianCalendar c = new GregorianCalendar();
        logln("With cutoff " + c.getGregorianChange());
        logln(" isLeapYear(1800) = " + (b = c.isLeapYear(1800)));
        logln(" (should be FALSE)");
        if (b != false) {
            errln("FAIL");
        }
        c.setGregorianChange(new Date(0, 0, 1)); // Jan 1 1900
        logln("With cutoff " + c.getGregorianChange());
        logln(" isLeapYear(1800) = " + (b = c.isLeapYear(1800)));
        logln(" (should be TRUE)");
        if (b != true) {
            errln("FAIL");
        }
    }

    // Test the correct behavior of the disambiguation algorithm.
    public void TestDisambiguation765() throws Exception {
        Locale savedLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            Calendar c = Calendar.getInstance();
            c.setLenient(false);

            c.clear();
            c.set(YEAR, 1997);
            c.set(MONTH, JUNE);
            c.set(DATE, 3);

            verify765("1997 third day of June = ", c, 1997, JUNE, 3);

            c.clear();
            c.set(YEAR, 1997);
            c.set(DAY_OF_WEEK, TUESDAY);
            c.set(MONTH, JUNE);
            c.set(DAY_OF_WEEK_IN_MONTH, 1);
            verify765("1997 first Tuesday in June = ", c, 1997, JUNE, 3);

            c.setLenient(true); // for 4944795
            c.clear();
            c.set(YEAR, 1997);
            c.set(DAY_OF_WEEK, TUESDAY);
            c.set(MONTH, JUNE);
            c.set(DAY_OF_WEEK_IN_MONTH, -1);
            verify765("1997 last Tuesday in June = ", c, 1997, JUNE, 24);

            c.setLenient(false);
            IllegalArgumentException e = null;
            try {
                c.clear();
                c.set(YEAR, 1997);
                c.set(DAY_OF_WEEK, TUESDAY);
                c.set(MONTH, JUNE);
                c.set(DAY_OF_WEEK_IN_MONTH, 0);
                c.getTime();
            } catch (IllegalArgumentException ex) {
                e = ex;
            }
            verify765("1997 zero-th Tuesday in June = ", e);

            c.clear();
            c.set(YEAR, 1997);
            c.set(DAY_OF_WEEK, TUESDAY);
            c.set(MONTH, JUNE);
            c.set(WEEK_OF_MONTH, 1);
            verify765("1997 Tuesday in week 1 of June = ", c, 1997, JUNE, 3);

            c.clear();
            c.set(YEAR, 1997);
            c.set(DAY_OF_WEEK, TUESDAY);
            c.set(MONTH, JUNE);
            c.set(WEEK_OF_MONTH, 4);
            verify765("1997 Tuesday in week 4 of June = ", c, 1997, JUNE, 24);

            try {
                c.clear();
                c.set(YEAR, 1997);
                c.set(DAY_OF_WEEK, TUESDAY);
                c.set(MONTH, JUNE);
                c.set(WEEK_OF_MONTH, 1);
                verify765("1997 Tuesday in week 0 of June = ", c, 1997, JUNE, 3);
            } catch (IllegalArgumentException ex) {
                errln("FAIL: Exception seen: " + ex.getMessage());
                // ex.printStackTrace(log);
            }

            c.clear();
            c.set(YEAR, 1997);
            c.set(DAY_OF_WEEK, TUESDAY);
            c.set(WEEK_OF_YEAR, 2);
            verify765("1997 Tuesday in week 2 of year = ", c, 1997, JANUARY, 7);

            c.clear();
            c.set(YEAR, 1997);
            c.set(DAY_OF_WEEK, TUESDAY);
            c.set(WEEK_OF_YEAR, 10);
            verify765("1997 Tuesday in week 10 of year = ", c, 1997, MARCH, 4);

            try {
                c.clear();
                c.set(YEAR, 1997);
                c.set(DAY_OF_WEEK, TUESDAY);
                c.set(WEEK_OF_YEAR, 0);
                verify765("1997 Tuesday in week 0 of year = ", c, 1996, DECEMBER, 24);
                throw new Exception("Fail: WEEK_OF_YEAR 0 should be illegal");
            } catch (IllegalArgumentException ex) {
            }
        } finally {
            Locale.setDefault(savedLocale);
        }
    }

    void verify765(String msg, Calendar c, int year, int month, int day) {
        if (c.get(YEAR) == year
                && c.get(MONTH) == month
                && c.get(DATE) == day) {
            logln("PASS: " + msg + c.getTime());
        } else {
            errln("FAIL: " + msg + c.getTime()
                    + "; expected "
                    + year + "/" + (month + 1) + "/" + day);
        }
    }

    // Called when e expected to be non-null
    void verify765(String msg, IllegalArgumentException e) {
        if (e == null) {
            errln("FAIL: No IllegalArgumentException for " + msg);
        } else {
            logln("PASS: " + msg + "IllegalArgument as expected");
        }
    }

    // Test the behavior of GMT vs. local time
    public void TestGMTvsLocal4064654() {
        Locale locale = Locale.getDefault();
        if (!TestUtils.usesGregorianCalendar(locale)) {
            logln("Skipping this test because locale is " + locale);
            return;
        }

        // Sample output 1:
        // % /usr/local/java/jdk1.1.3/solaris/bin/java test 1997 1 1 12 0 0
        // date = Wed Jan 01 04:00:00 PST 1997
        // offset for Wed Jan 01 04:00:00 PST 1997= -8hr
        test4064654(1997, 1, 1, 12, 0, 0);

        // Sample output 2:
        // % /usr/local/java/jdk1.1.3/solaris/bin/java test 1997 4 16 18 30 0
        // date = Wed Apr 16 10:30:00 PDT 1997
        // offset for Wed Apr 16 10:30:00 PDT 1997= -7hr

        // Note that in sample output 2 according to the offset, the gmt time
        // of the result would be 1997 4 16 17 30 0 which is different from the
        // input of 1997 4 16 18 30 0.
        test4064654(1997, 4, 16, 18, 30, 0);
    }
    void test4064654(int yr, int mo, int dt, int hr, int mn, int sc) {
        Date date;
        Calendar gmtcal = Calendar.getInstance();
        gmtcal.setTimeZone(TimeZone.getTimeZone("Africa/Casablanca"));
        gmtcal.set(yr, mo - 1, dt, hr, mn, sc);
        gmtcal.set(MILLISECOND, 0);

        date = gmtcal.getTime();
        logln("date = " + date);

        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        cal.setTime(date);

        int offset = cal.getTimeZone().getOffset(cal.get(ERA),
                cal.get(YEAR),
                cal.get(MONTH),
                cal.get(DATE),
                cal.get(DAY_OF_WEEK),
                cal.get(MILLISECOND));

        logln("offset for " + date + "= " + (offset / 1000 / 60 / 60.0) + "hr");

        int utc = ((cal.get(HOUR_OF_DAY) * 60
                + cal.get(MINUTE)) * 60
                + cal.get(SECOND)) * 1000
                + cal.get(MILLISECOND) - offset;

        int expected = ((hr * 60 + mn) * 60 + sc) * 1000;

        if (utc != expected) {
            errln("FAIL: Discrepancy of "
                    + (utc - expected) + " millis = "
                    + ((utc - expected) / 1000 / 60 / 60.0) + " hr");
        }
    }

    // Verify that add and set work regardless of the order in which
    // they are called.
    public void TestAddSetOrder621() {
        @SuppressWarnings("deprecation")
        Date d = new Date(97, 4, 14, 13, 23, 45);

        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.add(DATE, -5);
        cal.set(HOUR_OF_DAY, 0);
        cal.set(MINUTE, 0);
        cal.set(SECOND, 0);
        // ma feb 03 00:00:00 GMT+00:00 1997
        String s = cal.getTime().toString();

        cal = Calendar.getInstance();
        cal.setTime(d);
        cal.set(HOUR_OF_DAY, 0);
        cal.set(MINUTE, 0);
        cal.set(SECOND, 0);
        cal.add(DATE, -5);
        // ma feb 03 13:11:06 GMT+00:00 1997
        String s2 = cal.getTime().toString();

        if (s.equals(s2)) {
            logln("Pass: " + s + " == " + s2);
        } else {
            errln("FAIL: " + s + " != " + s2);
        }
    }

    // Verify that add works.
    public void TestAdd520() {
        int y = 1997, m = FEBRUARY, d = 1;
        GregorianCalendar temp = new GregorianCalendar(y, m, d);
        check520(temp, y, m, d);

        temp.add(YEAR, 1);
        y++;
        check520(temp, y, m, d);

        temp.add(MONTH, 1);
        m++;
        check520(temp, y, m, d);

        temp.add(DATE, 1);
        d++;
        check520(temp, y, m, d);

        temp.add(DATE, 2);
        d += 2;
        check520(temp, y, m, d);

        temp.add(DATE, 28);
        d = 1;
        ++m;
        check520(temp, y, m, d);
    }

    void check520(Calendar c, int y, int m, int d) {
        if (c.get(YEAR) != y
                || c.get(MONTH) != m
                || c.get(DATE) != d) {
            errln("FAILURE: Expected YEAR/MONTH/DATE of "
                    + y + "/" + (m + 1) + "/" + d
                    + "; got "
                    + c.get(YEAR) + "/"
                    + (c.get(MONTH) + 1) + "/"
                    + c.get(DATE));
        } else {
            logln("Confirmed: "
                    + y + "/" + (m + 1) + "/" + d);
        }
    }

    // Verify that setting fields works.  This test fails when an exception is thrown.
    public void TestFieldSet4781() {
        try {
            GregorianCalendar g = new GregorianCalendar();
            GregorianCalendar g2 = new GregorianCalendar();
            // At this point UTC value is set, various fields are not.
            // Now set to noon.
            g2.set(HOUR, 12);
            g2.set(MINUTE, 0);
            g2.set(SECOND, 0);
            // At this point the object thinks UTC is NOT set, but fields are set.
            // The following line will result in IllegalArgumentException because
            // it thinks the YEAR is set and it is NOT.
            if (g2.equals(g)) {
                logln("Same");
            } else {
                logln("Different");
            }
        } catch (IllegalArgumentException e) {
            errln("Unexpected exception seen: " + e);
        }
    }

    // Test serialization of a Calendar object
    public void TestSerialize337() {
        Calendar cal = Calendar.getInstance();

        boolean ok = false;

        try {
            FileOutputStream f = new FileOutputStream(FILENAME);
            ObjectOutput s = new ObjectOutputStream(f);
            s.writeObject(PREFIX);
            s.writeObject(cal);
            s.writeObject(POSTFIX);
            f.close();

            FileInputStream in = new FileInputStream(FILENAME);
            ObjectInputStream t = new ObjectInputStream(in);
            String pre = (String) t.readObject();
            Calendar c = (Calendar) t.readObject();
            String post = (String) t.readObject();
            in.close();

            ok = pre.equals(PREFIX)
                    && post.equals(POSTFIX)
                    && cal.equals(c);

            File fl = new File(FILENAME);
            fl.delete();
        } catch (IOException e) {
            errln("FAIL: Exception received:");
            // e.printStackTrace(log);
        } catch (ClassNotFoundException e) {
            errln("FAIL: Exception received:");
            // e.printStackTrace(log);
        }

        if (!ok) {
            errln("Serialization of Calendar object failed.");
        }
    }
    static final String PREFIX = "abc";
    static final String POSTFIX = "def";
    static final String FILENAME = "tmp337.bin";

    // Try to zero out the seconds field
    public void TestSecondsZero121() {
        Calendar cal = new GregorianCalendar();
        // Initialize with current date/time
        cal.setTime(new Date());
        // Round down to minute
        cal.set(SECOND, 0);
        Date d = cal.getTime();
        String s = d.toString();
        if (s.indexOf(":00 ") < 0) {
            errln("Expected to see :00 in " + s);
        }
    }

    // Try various sequences of add, set, and get method calls.
    public void TestAddSetGet0610() {
        //
        // Error case 1:
        // - Upon initialization calendar fields, millis = System.currentTime
        // - After set is called fields are initialized, time is not
        // - Addition uses millis which are still *now*
        //
        {
            Calendar calendar = new GregorianCalendar();
            calendar.set(1993, JANUARY, 4);
            logln("1A) " + value(calendar));
            calendar.add(DATE, 1);
            String v = value(calendar);
            logln("1B) " + v);
            logln("--) 1993/0/5");
            if (!v.equals(EXPECTED_0610)) {
                errln("Expected " + EXPECTED_0610
                        + "; saw " + v);
            }
        }

        //
        // Error case 2:
        // - Upon initialization calendar fields set, millis = 0
        // - Addition uses millis which are still 1970, 0, 1
        //
        {
            Calendar calendar = new GregorianCalendar(1993, JANUARY, 4);
            logln("2A) " + value(calendar));
            calendar.add(DATE, 1);
            String v = value(calendar);
            logln("2B) " + v);
            logln("--) 1993/0/5");
            if (!v.equals(EXPECTED_0610)) {
                errln("Expected " + EXPECTED_0610
                        + "; saw " + v);
            }
        }

        //
        // Error case 3:
        // - Upon initialization calendar fields, millis = 0
        // - getTime( ) is called which forces the millis to be set
        // - Addition uses millis which are correct
        //
        {
            Calendar calendar = new GregorianCalendar(1993, JANUARY, 4);
            logln("3A) " + value(calendar));
            calendar.getTime();
            calendar.add(DATE, 1);
            String v = value(calendar);
            logln("3B) " + v);
            logln("--) 1993/0/5");
            if (!v.equals(EXPECTED_0610)) {
                errln("Expected " + EXPECTED_0610
                        + "; saw " + v);
            }
        }
    }
    static String value(Calendar calendar) {
        return (calendar.get(YEAR) + "/"
                + calendar.get(MONTH) + "/"
                + calendar.get(DATE));
    }
    static String EXPECTED_0610 = "1993/0/5";

    // Test that certain fields on a certain date are as expected.
    public void TestFields060() {
        int year = 1997;
        int month = OCTOBER;  //october
        int dDate = 22;   //DAYOFWEEK should return 3 for Wednesday
        GregorianCalendar calendar = null;

        calendar = new GregorianCalendar(year, month, dDate);
        for (int i = 0; i < EXPECTED_FIELDS.length;) {
            int field = EXPECTED_FIELDS[i++];
            int expected = EXPECTED_FIELDS[i++];
            if (calendar.get(field) != expected) {
                errln("Expected field " + field + " to have value " + expected
                        + "; received " + calendar.get(field) + " instead");
            }
        }
    }
    static int[] EXPECTED_FIELDS = {
        YEAR, 1997,
        MONTH, OCTOBER,
        DAY_OF_MONTH, 22,
        DAY_OF_WEEK, WEDNESDAY,
        DAY_OF_WEEK_IN_MONTH, 4,
        DAY_OF_YEAR, 295};

    static final String[] calendarFieldNames = {
        /*  0 */ "ERA",
        /*  1 */ "YEAR",
        /*  2 */ "MONTH",
        /*  3 */ "WEEK_OF_YEAR",
        /*  4 */ "WEEK_OF_MONTH",
        /*  5 */ "DAY_OF_MONTH",
        /*  6 */ "DAY_OF_YEAR",
        /*  7 */ "DAY_OF_WEEK",
        /*  8 */ "DAY_OF_WEEK_IN_MONTH",
        /*  9 */ "AM_PM",
        /* 10 */ "HOUR",
        /* 11 */ "HOUR_OF_DAY",
        /* 12 */ "MINUTE",
        /* 13 */ "SECOND",
        /* 14 */ "MILLISECOND",
        /* 15 */ "ZONE_OFFSET",
        /* 16 */ "DST_OFFSET"};

    // Verify that the fields are as expected (mostly zero) at the epoch start.
    // Note that we adjust for the default timezone to get most things to zero.
    public void TestEpochStartFields() {
        String[][] lt = {
            {"en", "US", "US/Pacific"},        /* First day = 1, Minimum day = 1 */
            {"en", "US", "America/Anchorage"}, /* First day = 1, Minimum day = 1 */
            {"en", "TO", "Pacific/Tongatapu"}, /* First day = 1, Minimum day = 1 */
            {"en", "MH", "Pacific/Majuro"},    /* First day = 1, Minimum day = 1 */
            {"ja", "JP", "Asia/Tokyo"},        /* First day = 1, Minimum day = 1 */
            {"iw", "IL", "Asia/Jerusalem"},    /* First day = 1, Minimum day = 1 */
            {"hi", "IN", "Asia/Jakarta"},      /* First day = 1, Minimum day = 1 */
            {"en", "GB", "Europe/London"},     /* First day = 2, Minimum day = 1 */
            {"en", "GB", "GMT"},               /* First day = 2, Minimum day = 1 */
            {"de", "DE", "Europe/Berlin"},     /* First day = 2, Minimum day = 4 */
            {"ar", "EG", "Africa/Cairo"}};     /* First day = 7, Minimum day = 1 */

        int[][] goldenData = {
            {1, 1970, 0, 1, 1, 1, 1, 5, 1, 0, 0, 0, 0, 0, 0, -28800000, 0},
            {1, 1969, 11, 1, 5, 31, 365, 4, 5, 1, 11, 23, 0, 0, 0, -36000000, 0},
            {1, 1970, 0, 1, 1, 1, 1, 5, 1, 0, 0, 0, 0, 0, 0, 46800000, 0},
            {1, 1970, 0, 1, 1, 1, 1, 5, 1, 0, 0, 0, 0, 0, 0, 43200000, 0},
            {1, 1970, 0, 1, 1, 1, 1, 5, 1, 0, 0, 0, 0, 0, 0, 32400000, 0},
            {1, 1970, 0, 1, 1, 1, 1, 5, 1, 0, 0, 0, 0, 0, 0, 7200000, 0},
            {1, 1970, 0, 1, 1, 1, 1, 5, 1, 0, 0, 0, 0, 0, 0, 25200000, 0},
            {1, 1970, 0, 1, 1, 1, 1, 5, 1, 0, 1, 1, 0, 0, 0, 3600000, 0},
            {1, 1970, 0, 1, 1, 1, 1, 5, 1, 0, 0, 0, 0, 0, 0, 0, 0},
            {1, 1970, 0, 1, 1, 1, 1, 5, 1, 0, 0, 0, 0, 0, 0, 3600000, 0},
            {1, 1970, 0, 1, 1, 1, 1, 5, 1, 0, 0, 0, 0, 0, 0, 7200000, 0}};

        Locale savedLocale = Locale.getDefault();
        TimeZone savedTimeZone = TimeZone.getDefault();

        try {
            for (int j = 0; j < lt.length; j++) {
                Locale l = new Locale(lt[j][0], lt[j][1]);
                TimeZone z = TimeZone.getTimeZone(lt[j][2]);
                Locale.setDefault(l);
                TimeZone.setDefault(z);
                Calendar c = Calendar.getInstance();
                Date d = new Date(-z.getRawOffset());

                int val;
                int[] EPOCH_FIELDS = goldenData[j];
                c.setTime(d);

                boolean err = false;
                for (int i = 0; i < calendarFieldNames.length; ++i) {
                    if ((val = c.get(i)) != EPOCH_FIELDS[i]) {
                        errln("Wrong value: " + val
                                + " for field(" + calendarFieldNames[i]
                                + "), expected: " + EPOCH_FIELDS[i]);
                        err = true;
                    }
                }
                if (err) {
                    errln("Failed: \n\tDate=" + d + "\n\tTimeZone=" + z
                            + "\n\tLocale=" + l + "\n\tCalendar=" + c);
                }
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedTimeZone);
        }
    }

    // Verify that as you add days to the calendar (e.g., 24 day periods),
    // the day of the week shifts in the expected pattern.
    public void TestDOWProgression() {
        Calendar cal
                = new GregorianCalendar(1972, OCTOBER, 26);
        marchByDelta(cal, 24); // Last parameter must be != 0 modulo 7
    }

    // Supply a delta which is not a multiple of 7.
    void marchByDelta(Calendar cal, int delta) {
        Calendar cur = (Calendar) cal.clone();
        int initialDOW = cur.get(DAY_OF_WEEK);
        int DOW, newDOW = initialDOW;
        do {
            DOW = newDOW;
            logln("DOW = " + DOW + "  " + cur.getTime());

            cur.add(DAY_OF_WEEK, delta);
            newDOW = cur.get(DAY_OF_WEEK);
            int expectedDOW = 1 + (DOW + delta - 1) % 7;
            if (newDOW != expectedDOW) {
                errln("Day of week should be " + expectedDOW
                        + " instead of " + newDOW + " on " + cur.getTime());
                return;
            }
        } while (newDOW != initialDOW);
    }

    public void TestActualMinMax() {
        Calendar cal = new GregorianCalendar(1967, MARCH, 10);
        cal.setFirstDayOfWeek(SUNDAY);
        cal.setMinimalDaysInFirstWeek(3);

        if (cal.getActualMinimum(DAY_OF_MONTH) != 1) {
            errln("Actual minimum date for 3/10/1967 should have been 1; got "
                    + cal.getActualMinimum(DAY_OF_MONTH));
        }
        if (cal.getActualMaximum(DAY_OF_MONTH) != 31) {
            errln("Actual maximum date for 3/10/1967 should have been 31; got "
                    + cal.getActualMaximum(DAY_OF_MONTH));
        }

        cal.set(MONTH, FEBRUARY);
        if (cal.getActualMaximum(DAY_OF_MONTH) != 28) {
            errln("Actual maximum date for 2/10/1967 should have been 28; got "
                    + cal.getActualMaximum(DAY_OF_MONTH));
        }
        if (cal.getActualMaximum(DAY_OF_YEAR) != 365) {
            errln("Number of days in 1967 should have been 365; got "
                    + cal.getActualMaximum(DAY_OF_YEAR));
        }

        cal.set(YEAR, 1968);
        if (cal.getActualMaximum(DAY_OF_MONTH) != 29) {
            errln("Actual maximum date for 2/10/1968 should have been 29; got "
                    + cal.getActualMaximum(DAY_OF_MONTH));
        }
        if (cal.getActualMaximum(DAY_OF_YEAR) != 366) {
            errln("Number of days in 1968 should have been 366; got "
                    + cal.getActualMaximum(DAY_OF_YEAR));
        }
        // Using week settings of SUNDAY/3 (see above)
        if (cal.getActualMaximum(WEEK_OF_YEAR) != 52) {
            errln("Number of weeks in 1968 should have been 52; got "
                    + cal.getActualMaximum(WEEK_OF_YEAR));
        }

        cal.set(YEAR, 1976);
        // Using week settings of SUNDAY/3 (see above)
        if (cal.getActualMaximum(WEEK_OF_YEAR) != 53) {
            errln("Number of weeks in 1976 should have been 53; got "
                    + cal.getActualMaximum(WEEK_OF_YEAR));
        }
    }

    public void TestRoll() {
        Calendar cal = new GregorianCalendar(1997, JANUARY, 31);

        int[] dayValues = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31, 31};

        for (int i = 0; i < dayValues.length; i++) {
            Calendar cal2 = (Calendar) cal.clone();
            cal2.roll(MONTH, i);
            if (cal2.get(DAY_OF_MONTH) != dayValues[i]) {
                errln("Rolling the month in 1/31/1997 up by " + i + " should have yielded "
                        + ((i + 1) % 12) + "/" + dayValues[i] + "/1997, but actually yielded "
                        + ((i + 1) % 12) + "/" + cal2.get(DAY_OF_MONTH) + "/1997.");
            }
        }

        cal.set(1996, FEBRUARY, 29);

        int[] monthValues = {1, 2, 2, 2, 1, 2, 2, 2, 1, 2};
        int[] dayValues2 = {29, 1, 1, 1, 29, 1, 1, 1, 29, 1};

        for (int i = 0; i < dayValues2.length; i++) {
            Calendar cal2 = (Calendar) cal.clone();
            cal2.roll(YEAR, i);
            if (cal2.get(DAY_OF_MONTH) != dayValues2[i] || cal2.get(MONTH)
                    != monthValues[i]) {
                errln("Rolling the year in 2/29/1996 up by " + i + " should have yielded "
                        + (monthValues[i] + 1) + "/" + dayValues2[i] + "/"
                        + (1996 + i) + ", but actually yielded "
                        + (cal2.get(MONTH) + 1) + "/"
                        + cal2.get(DAY_OF_MONTH) + "/" + (1996 + i) + ".");
            }
        }

        // Test rolling hour of day
        cal.set(HOUR_OF_DAY, 0);
        cal.roll(HOUR_OF_DAY, -2);
        int f = cal.get(HOUR_OF_DAY);
        if (f != 22) {
            errln("Rolling HOUR_OF_DAY=0 delta=-2 gave " + f + " Wanted 22");
        }
        cal.roll(HOUR_OF_DAY, 5);
        f = cal.get(HOUR_OF_DAY);
        if (f != 3) {
            errln("Rolling HOUR_OF_DAY=22 delta=5 gave " + f + " Wanted 3");
        }
        cal.roll(HOUR_OF_DAY, 21);
        f = cal.get(HOUR_OF_DAY);
        if (f != 0) {
            errln("Rolling HOUR_OF_DAY=3 delta=21 gave " + f + " Wanted 0");
        }

        // Test rolling hour
        cal.set(HOUR_OF_DAY, 0);
        cal.roll(HOUR, -2);
        f = cal.get(HOUR);
        if (f != 10) {
            errln("Rolling HOUR=0 delta=-2 gave " + f + " Wanted 10");
        }
        cal.roll(HOUR, 5);
        f = cal.get(HOUR);
        if (f != 3) {
            errln("Rolling HOUR=10 delta=5 gave " + f + " Wanted 3");
        }
        cal.roll(HOUR, 9);
        f = cal.get(HOUR);
        if (f != 0) {
            errln("Rolling HOUR=3 delta=9 gave " + f + " Wanted 0");
        }
    }

    /*
     * Confirm that multiple calls to Calendar.set() works correctly.
     */
    public void Test4374886() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedTimeZone = TimeZone.getDefault();

        try {
            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("PST"));

            Calendar cal = Calendar.getInstance();
            cal.set(YEAR, 2001);
            cal.set(MONTH, OCTOBER);
            cal.set(WEEK_OF_YEAR, 4);
            cal.set(DAY_OF_WEEK, 2);

            if (cal.get(YEAR) != 2001
                    || cal.get(MONTH) != JANUARY
                    || cal.get(DATE) != 22
                    || cal.get(DAY_OF_WEEK) != MONDAY) {
                errln("Failed : got " + cal.getTime() + ", expected Mon Jan 22, 2001");
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedTimeZone);
        }
    }

    public void TestClonedSharedZones() throws NoSuchFieldException, IllegalAccessException {
        Field zone = Calendar.class.getDeclaredField("zone");
        zone.setAccessible(true);
        Field sharedZone = Calendar.class.getDeclaredField("sharedZone");
        sharedZone.setAccessible(true);

        // create a new calendar with any date, and clone it.
        Calendar c1 = new GregorianCalendar();
        Calendar c2 = (Calendar) c1.clone();

        // c1 should have a shared zone
        if (!sharedZone.getBoolean(c1)) {
            errln("Failed : c1.sharedZone == false");
        } else {
            // c2 should have a shared zone too
            if (!sharedZone.getBoolean(c2)) {
                errln("Failed : c2.sharedZone == false");
            } else if (zone.get(c1) != zone.get(c2)) {
                errln("Failed : c1.zone != c2.zone");
            }
        }
    }
}

//eof
