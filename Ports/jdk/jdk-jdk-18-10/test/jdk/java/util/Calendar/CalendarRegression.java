/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4031502 4035301 4040996 4051765 4059654 4061476 4070502 4071197 4071385
 * 4073929 4083167 4086724 4092362 4095407 4096231 4096539 4100311 4103271
 * 4106136 4108764 4114578 4118384 4125881 4125892 4136399 4141665 4142933
 * 4145158 4145983 4147269 4149677 4162587 4165343 4166109 4167060 4173516
 * 4174361 4177484 4197699 4209071 4288792 4328747 4413980 4546637 4623997
 * 4685354 4655637 4683492 4080631 4080631 4167995 4340146 4639407
 * 4652815 4652830 4740554 4936355 4738710 4633646 4846659 4822110 4960642
 * 4973919 4980088 4965624 5013094 5006864 8152077
 * @library /java/text/testlib
 * @run main CalendarRegression
 */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import static java.util.Calendar.*;

public class CalendarRegression extends IntlTest {

    public static void main(String[] args) throws Exception {
        new CalendarRegression().run(args);
    }

    /*
    Synopsis: java.sql.Timestamp constructor works wrong on Windows 95

    ==== Here is the test ====
    public static void main (String args[]) {
    java.sql.Timestamp t= new java.sql.Timestamp(0,15,5,5,8,13,123456700);
    logln("expected=1901-04-05 05:08:13.1234567");
    logln(" result="+t);
    }

    ==== Here is the output of the test on Solaris or NT ====
    expected=1901-04-05 05:08:13.1234567
    result=1901-04-05 05:08:13.1234567

    ==== Here is the output of the test on Windows95 ====
    expected=1901-04-05 05:08:13.1234567
    result=1901-04-05 06:08:13.1234567
     */
    public void Test4031502() {
        // This bug actually occurs on Windows NT as well, and doesn't
        // require the host zone to be set; it can be set in Java.
        String[] ids = TimeZone.getAvailableIDs();
        boolean bad = false;
        for (int i = 0; i < ids.length; ++i) {
            TimeZone zone = TimeZone.getTimeZone(ids[i]);
            GregorianCalendar cal = new GregorianCalendar(zone);
            cal.clear();
            cal.set(1900, 15, 5, 5, 8, 13);
            if (cal.get(HOUR) != 5) {
                logln(zone.getID() + " "
                        + //zone.useDaylightTime() + " "
                        + cal.get(DST_OFFSET) / (60 * 60 * 1000) + " "
                        + zone.getRawOffset() / (60 * 60 * 1000)
                        + ": HOUR = " + cal.get(HOUR));
                bad = true;
            }
        }
        if (bad) {
            errln("TimeZone problems with GC");
        }
    }

    public void Test4035301() {
        GregorianCalendar c = new GregorianCalendar(98, 8, 7);
        GregorianCalendar d = new GregorianCalendar(98, 8, 7);
        if (c.after(d)
                || c.after(c)
                || c.before(d)
                || c.before(c)
                || !c.equals(c)
                || !c.equals(d)) {
            errln("Fail");
        }
    }

    public void Test4040996() {
        String[] ids = TimeZone.getAvailableIDs(-8 * 60 * 60 * 1000);
        SimpleTimeZone pdt = new SimpleTimeZone(-8 * 60 * 60 * 1000, ids[0]);
        pdt.setStartRule(APRIL, 1, SUNDAY, 2 * 60 * 60 * 1000);
        pdt.setEndRule(OCTOBER, -1, SUNDAY, 2 * 60 * 60 * 1000);
        Calendar calendar = new GregorianCalendar(pdt);

        calendar.set(MONTH, 3);
        calendar.set(DAY_OF_MONTH, 18);
        calendar.set(SECOND, 30);

        logln("MONTH: " + calendar.get(MONTH));
        logln("DAY_OF_MONTH: "
                + calendar.get(DAY_OF_MONTH));
        logln("MINUTE: " + calendar.get(MINUTE));
        logln("SECOND: " + calendar.get(SECOND));

        calendar.add(SECOND, 6);
        //This will print out todays date for MONTH and DAY_OF_MONTH
        //instead of the date it was set to.
        //This happens when adding MILLISECOND or MINUTE also
        logln("MONTH: " + calendar.get(MONTH));
        logln("DAY_OF_MONTH: "
                + calendar.get(DAY_OF_MONTH));
        logln("MINUTE: " + calendar.get(MINUTE));
        logln("SECOND: " + calendar.get(SECOND));
        if (calendar.get(MONTH) != 3
                || calendar.get(DAY_OF_MONTH) != 18
                || calendar.get(SECOND) != 36) {
            errln("Fail: Calendar.add misbehaves");
        }
    }

    public void Test4051765() {
        Calendar cal = Calendar.getInstance();
        cal.setLenient(false);
        cal.set(DAY_OF_WEEK, 0);
        try {
            cal.getTime();
            errln("Fail: DAY_OF_WEEK 0 should be disallowed");
        } catch (IllegalArgumentException e) {
            return;
        }
    }

    /* User error - no bug here
    public void Test4059524() {
        // Create calendar for April 10, 1997
        GregorianCalendar calendar  = new GregorianCalendar();
        // print out a bunch of interesting things
        logln("ERA: " + calendar.get(calendar.ERA));
        logln("YEAR: " + calendar.get(calendar.YEAR));
        logln("MONTH: " + calendar.get(calendar.MONTH));
        logln("WEEK_OF_YEAR: " +
                           calendar.get(calendar.WEEK_OF_YEAR));
        logln("WEEK_OF_MONTH: " +
                           calendar.get(calendar.WEEK_OF_MONTH));
        logln("DATE: " + calendar.get(calendar.DATE));
        logln("DAY_OF_MONTH: " +
                           calendar.get(calendar.DAY_OF_MONTH));
        logln("DAY_OF_YEAR: " + calendar.get(calendar.DAY_OF_YEAR));
        logln("DAY_OF_WEEK: " + calendar.get(calendar.DAY_OF_WEEK));
        logln("DAY_OF_WEEK_IN_MONTH: " +
                           calendar.get(calendar.DAY_OF_WEEK_IN_MONTH));
        logln("AM_PM: " + calendar.get(calendar.AM_PM));
        logln("HOUR: " + calendar.get(calendar.HOUR));
        logln("HOUR_OF_DAY: " + calendar.get(calendar.HOUR_OF_DAY));
        logln("MINUTE: " + calendar.get(calendar.MINUTE));
        logln("SECOND: " + calendar.get(calendar.SECOND));
        logln("MILLISECOND: " + calendar.get(calendar.MILLISECOND));
        logln("ZONE_OFFSET: "
                           + (calendar.get(calendar.ZONE_OFFSET)/(60*60*1000)));
        logln("DST_OFFSET: "
                           + (calendar.get(calendar.DST_OFFSET)/(60*60*1000)));
        calendar  = new GregorianCalendar(1997,3,10);
        calendar.getTime();
        logln("April 10, 1997");
        logln("ERA: " + calendar.get(calendar.ERA));
        logln("YEAR: " + calendar.get(calendar.YEAR));
        logln("MONTH: " + calendar.get(calendar.MONTH));
        logln("WEEK_OF_YEAR: " +
                           calendar.get(calendar.WEEK_OF_YEAR));
        logln("WEEK_OF_MONTH: " +
                           calendar.get(calendar.WEEK_OF_MONTH));
        logln("DATE: " + calendar.get(calendar.DATE));
        logln("DAY_OF_MONTH: " +
                           calendar.get(calendar.DAY_OF_MONTH));
        logln("DAY_OF_YEAR: " + calendar.get(calendar.DAY_OF_YEAR));
        logln("DAY_OF_WEEK: " + calendar.get(calendar.DAY_OF_WEEK));
        logln("DAY_OF_WEEK_IN_MONTH: " + calendar.get(calendar.DAY_OF_WEEK_IN_MONTH));
        logln("AM_PM: " + calendar.get(calendar.AM_PM));
        logln("HOUR: " + calendar.get(calendar.HOUR));
        logln("HOUR_OF_DAY: " + calendar.get(calendar.HOUR_OF_DAY));
        logln("MINUTE: " + calendar.get(calendar.MINUTE));
        logln("SECOND: " + calendar.get(calendar.SECOND));
        logln("MILLISECOND: " + calendar.get(calendar.MILLISECOND));
        logln("ZONE_OFFSET: "
                           + (calendar.get(calendar.ZONE_OFFSET)/(60*60*1000))); // in hours
        logln("DST_OFFSET: "
                           + (calendar.get(calendar.DST_OFFSET)/(60*60*1000))); // in hours
    }
     */
    public void Test4059654() {
        GregorianCalendar gc = new GregorianCalendar();

        gc.set(1997, 3, 1, 15, 16, 17); // April 1, 1997

        gc.set(HOUR, 0);
        gc.set(AM_PM, AM);
        gc.set(MINUTE, 0);
        gc.set(SECOND, 0);
        gc.set(MILLISECOND, 0);

        Date cd = gc.getTime();
        @SuppressWarnings("deprecation")
        Date exp = new Date(97, 3, 1, 0, 0, 0);
        if (!cd.equals(exp)) {
            errln("Fail: Calendar.set broken. Got " + cd + " Want " + exp);
        }
    }

    public void Test4061476() {
        SimpleDateFormat fmt = new SimpleDateFormat("ddMMMyy", Locale.UK);
        Calendar cal = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"),
                Locale.UK);
        fmt.setCalendar(cal);
        try {
            Date date = fmt.parse("29MAY97");
            cal.setTime(date);
        } catch (Exception e) {
        }
        cal.set(HOUR_OF_DAY, 13);
        logln("Hour: " + cal.get(HOUR_OF_DAY));
        cal.add(HOUR_OF_DAY, 6);
        logln("Hour: " + cal.get(HOUR_OF_DAY));
        if (cal.get(HOUR_OF_DAY) != 19) {
            errln("Fail: Want 19 Got " + cal.get(HOUR_OF_DAY));
        }
    }

    public void Test4070502() {
        @SuppressWarnings("deprecation")
        Date d = getAssociatedDate(new Date(98, 0, 30));
        Calendar cal = new GregorianCalendar();
        cal.setTime(d);
        if (cal.get(DAY_OF_WEEK) == SATURDAY
                || cal.get(DAY_OF_WEEK) == SUNDAY) {
            errln("Fail: Want weekday Got " + d);
        }
    }

    /**
     * Get the associated date starting from a specified date
     * NOTE: the unnecessary "getTime()'s" below are a work-around for a
     * bug in jdk 1.1.3 (and probably earlier versions also)
     * <p>
     * @param date The date to start from
     */
    public static Date getAssociatedDate(Date d) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(d);
        //cal.add(field, amount); //<-- PROBLEM SEEN WITH field = DATE,MONTH
        // cal.getTime();  // <--- REMOVE THIS TO SEE BUG
        while (true) {
            int wd = cal.get(DAY_OF_WEEK);
            if (wd == SATURDAY || wd == SUNDAY) {
                cal.add(DATE, 1);
                // cal.getTime();
            } else {
                break;
            }
        }
        return cal.getTime();
    }

    public void Test4071197() {
        dowTest(false);
        dowTest(true);
    }

    void dowTest(boolean lenient) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(1997, AUGUST, 12); // Wednesday
        // cal.getTime(); // Force update
        cal.setLenient(lenient);
        cal.set(1996, DECEMBER, 1); // Set the date to be December 1, 1996
        int dow = cal.get(DAY_OF_WEEK);
        int min = cal.getMinimum(DAY_OF_WEEK);
        int max = cal.getMaximum(DAY_OF_WEEK);
        logln(cal.getTime().toString());
        if (min != SUNDAY || max != SATURDAY) {
            errln("FAIL: Min/max bad");
        }
        if (dow < min || dow > max) {
            errln("FAIL: Day of week " + dow + " out of range");
        }
        if (dow != SUNDAY) {
            errln("FAIL: Day of week should be SUNDAY Got " + dow);
        }
    }

    @SuppressWarnings("deprecation")
    public void Test4071385() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(98, JUNE, 24));
        cal.set(MONTH, NOVEMBER); // change a field
        logln(cal.getTime().toString());
        if (!cal.getTime().equals(new Date(98, NOVEMBER, 24))) {
            errln("Fail");
        }
    }

    public void Test4073929() {
        GregorianCalendar foo1 = new GregorianCalendar(1997, 8, 27);
        foo1.add(DAY_OF_MONTH, +1);
        int testyear = foo1.get(YEAR);
        int testmonth = foo1.get(MONTH);
        int testday = foo1.get(DAY_OF_MONTH);
        if (testyear != 1997
                || testmonth != 8
                || testday != 28) {
            errln("Fail: Calendar not initialized");
        }
    }

    public void Test4083167() {
        TimeZone saveZone = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            Date firstDate = new Date();
            Calendar cal = new GregorianCalendar();
            cal.setTime(firstDate);
            long firstMillisInDay = cal.get(HOUR_OF_DAY) * 3600000L
                    + cal.get(MINUTE) * 60000L
                    + cal.get(SECOND) * 1000L
                    + cal.get(MILLISECOND);

            logln("Current time: " + firstDate.toString());

            for (int validity = 0; validity < 30; validity++) {
                Date lastDate = new Date(firstDate.getTime()
                        + (long) validity * 1000 * 24 * 60 * 60);
                cal.setTime(lastDate);
                long millisInDay = cal.get(HOUR_OF_DAY) * 3600000L
                        + cal.get(MINUTE) * 60000L
                        + cal.get(SECOND) * 1000L
                        + cal.get(MILLISECOND);
                if (firstMillisInDay != millisInDay) {
                    errln("Day has shifted " + lastDate);
                }
            }
        } finally {
            TimeZone.setDefault(saveZone);
        }
    }

    public void Test4086724() {
        SimpleDateFormat date;
        TimeZone saveZone = TimeZone.getDefault();
        Locale saveLocale = Locale.getDefault();

        String summerTime = "British Summer Time";
        String standardTime = "Greenwich Mean Time";
        try {
            Locale.setDefault(Locale.UK);
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"));
            date = new SimpleDateFormat("zzzz");

            Calendar cal = Calendar.getInstance();
            cal.set(1997, SEPTEMBER, 30);
            Date now = cal.getTime();
            String formattedDate = date.format(now);
            if (!formattedDate.equals(summerTime)) {
                errln("Wrong display name \"" + formattedDate
                        + "\" for <" + now + ">");
            }
            int weekOfYear = cal.get(WEEK_OF_YEAR);
            if (weekOfYear != 40) {
                errln("Wrong week-of-year " + weekOfYear
                        + " for <" + now + ">");
            }

            cal.set(1996, DECEMBER, 31);
            now = cal.getTime();
            formattedDate = date.format(now);
            if (!formattedDate.equals(standardTime)) {
                errln("Wrong display name \"" + formattedDate
                        + "\" for <" + now + ">");
            }
            weekOfYear = cal.get(WEEK_OF_YEAR);
            if (weekOfYear != 1) {
                errln("Wrong week-of-year " + weekOfYear
                        + " for <" + now + ">");
            }

            cal.set(1997, JANUARY, 1);
            now = cal.getTime();
            formattedDate = date.format(now);
            if (!formattedDate.equals(standardTime)) {
                errln("Wrong display name \"" + formattedDate
                        + "\" for <" + now + ">");
            }
            weekOfYear = cal.get(WEEK_OF_YEAR);
            if (weekOfYear != 1) {
                errln("Wrong week-of-year " + weekOfYear
                        + " for <" + now + ">");
            }

            cal.set(1997, JANUARY, 8);
            now = cal.getTime();
            formattedDate = date.format(now);
            if (!formattedDate.equals(standardTime)) {
                errln("Wrong display name \"" + formattedDate
                        + "\" for <" + now + ">");
            }
            weekOfYear = cal.get(WEEK_OF_YEAR);
            if (weekOfYear != 2) {
                errln("Wrong week-of-year " + weekOfYear
                        + " for <" + now + ">");
            }

        } finally {
            Locale.setDefault(saveLocale);
            TimeZone.setDefault(saveZone);
        }
    }

    public void Test4092362() {
        GregorianCalendar cal1 = new GregorianCalendar(1997, 10, 11, 10, 20, 40);
        /*cal1.set( Calendar.YEAR, 1997 );
        cal1.set( Calendar.MONTH, 10 );
        cal1.set( Calendar.DATE, 11 );
        cal1.set( Calendar.HOUR, 10 );
        cal1.set( Calendar.MINUTE, 20 );
        cal1.set( Calendar.SECOND, 40 ); */

        logln(" Cal1 = " + cal1.getTime().getTime());
        logln(" Cal1 time in ms = " + cal1.get(MILLISECOND));
        for (int k = 0; k < 100; k++);

        GregorianCalendar cal2 = new GregorianCalendar(1997, 10, 11, 10, 20, 40);
        /*cal2.set( Calendar.YEAR, 1997 );
        cal2.set( Calendar.MONTH, 10 );
        cal2.set( Calendar.DATE, 11 );
        cal2.set( Calendar.HOUR, 10 );
        cal2.set( Calendar.MINUTE, 20 );
        cal2.set( Calendar.SECOND, 40 ); */

        logln(" Cal2 = " + cal2.getTime().getTime());
        logln(" Cal2 time in ms = " + cal2.get(MILLISECOND));
        if (!cal1.equals(cal2)) {
            errln("Fail: Milliseconds randomized");
        }
    }

    public void Test4095407() {
        GregorianCalendar a = new GregorianCalendar(1997, NOVEMBER, 13);
        int dow = a.get(DAY_OF_WEEK);
        if (dow != THURSDAY) {
            errln("Fail: Want THURSDAY Got " + dow);
        }
    }

    public void Test4096231() {
        TimeZone GMT = TimeZone.getTimeZone("GMT");
        TimeZone PST = TimeZone.getTimeZone("PST");
        int sec = 0, min = 0, hr = 0, day = 1, month = 10, year = 1997;

        Calendar cal1 = new GregorianCalendar(PST);
        cal1.setTime(new Date(880698639000L));
        int p;
        logln("PST 1 is: " + (p = cal1.get(HOUR_OF_DAY)));
        cal1.setTimeZone(GMT);
        // Issue 1: Changing the timezone doesn't change the
        //          represented time.
        int h1, h2;
        logln("GMT 1 is: " + (h1 = cal1.get(HOUR_OF_DAY)));
        cal1.setTime(new Date(880698639000L));
        logln("GMT 2 is: " + (h2 = cal1.get(HOUR_OF_DAY)));
        // Note: This test had a bug in it.  It wanted h1!=h2, when
        // what was meant was h1!=p.  Fixed this concurrent with fix
        // to 4177484.
        if (p == h1 || h1 != h2) {
            errln("Fail: Hour same in different zones");
        }

        Calendar cal2 = new GregorianCalendar(GMT);
        Calendar cal3 = new GregorianCalendar(PST);
        cal2.set(MILLISECOND, 0);
        cal3.set(MILLISECOND, 0);

        cal2.set(cal1.get(YEAR),
                cal1.get(MONTH),
                cal1.get(DAY_OF_MONTH),
                cal1.get(HOUR_OF_DAY),
                cal1.get(MINUTE),
                cal1.get(SECOND));

        long t1, t2, t3, t4;
        logln("RGMT 1 is: " + (t1 = cal2.getTime().getTime()));
        cal3.set(year, month, day, hr, min, sec);
        logln("RPST 1 is: " + (t2 = cal3.getTime().getTime()));
        cal3.setTimeZone(GMT);
        logln("RGMT 2 is: " + (t3 = cal3.getTime().getTime()));
        cal3.set(cal1.get(YEAR),
                cal1.get(MONTH),
                cal1.get(DAY_OF_MONTH),
                cal1.get(HOUR_OF_DAY),
                cal1.get(MINUTE),
                cal1.get(SECOND));
        // Issue 2: Calendar continues to use the timezone in its
        //          constructor for set() conversions, regardless
        //          of calls to setTimeZone()
        logln("RGMT 3 is: " + (t4 = cal3.getTime().getTime()));
        if (t1 == t2
                || t1 != t4
                || t2 != t3) {
            errln("Fail: Calendar zone behavior faulty");
        }
    }

    public void Test4096539() {
        int[] y = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

        for (int x = 0; x < 12; x++) {
            GregorianCalendar gc = new GregorianCalendar(1997, x, y[x]);
            int m1, m2;
            log((m1 = gc.get(MONTH) + 1) + "/"
                    + gc.get(DATE) + "/" + gc.get(YEAR)
                    + " + 1mo = ");

            gc.add(MONTH, 1);
            logln((m2 = gc.get(MONTH) + 1) + "/"
                    + gc.get(DATE) + "/" + gc.get(YEAR)
            );
            int m = (m1 % 12) + 1;
            if (m2 != m) {
                errln("Fail: Want " + m + " Got " + m2);
            }
        }

    }

    public void Test4100311() {
        Locale locale = Locale.getDefault();
        if (!TestUtils.usesGregorianCalendar(locale)) {
            logln("Skipping this test because locale is " + locale);
            return;
        }

        GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance();
        cal.set(YEAR, 1997);
        cal.set(DAY_OF_YEAR, 1);
        Date d = cal.getTime();             // Should be Jan 1
        logln(d.toString());
        if (cal.get(DAY_OF_YEAR) != 1) {
            errln("Fail: DAY_OF_YEAR not set");
        }
    }

    public void Test4103271() {
        Locale locale = Locale.getDefault();
        if (!TestUtils.usesGregorianCalendar(locale)) {
            logln("Skipping this test because locale is " + locale);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat();
        int numYears = 40, startYear = 1997, numDays = 15;
        String output, testDesc;
        GregorianCalendar testCal = (GregorianCalendar) Calendar.getInstance();
        testCal.clear();
        sdf.setCalendar(testCal);
        sdf.applyPattern("d MMM yyyy");
        boolean fail = false;
        for (int firstDay = 1; firstDay <= 2; firstDay++) {
            for (int minDays = 1; minDays <= 7; minDays++) {
                testCal.setMinimalDaysInFirstWeek(minDays);
                testCal.setFirstDayOfWeek(firstDay);
                testDesc = ("Test" + String.valueOf(firstDay) + String.valueOf(minDays));
                logln(testDesc + " => 1st day of week="
                        + String.valueOf(firstDay)
                        + ", minimum days in first week="
                        + String.valueOf(minDays));
                for (int j = startYear; j <= startYear + numYears; j++) {
                    testCal.set(j, 11, 25);
                    for (int i = 0; i < numDays; i++) {
                        testCal.add(DATE, 1);
                        String calWOY;
                        int actWOY = testCal.get(WEEK_OF_YEAR);
                        if (actWOY < 1 || actWOY > 53) {
                            Date d = testCal.getTime();
                            calWOY = String.valueOf(actWOY);
                            output = testDesc + " - " + sdf.format(d) + "\t";
                            output = output + "\t" + calWOY;
                            logln(output);
                            fail = true;
                        }
                    }
                }
            }
        }

        int[] DATA = {
            3, 52, 52, 52, 52, 52, 52, 52,
            1, 1, 1, 1, 1, 1, 1,
            2, 2, 2, 2, 2, 2, 2,
            4, 52, 52, 52, 52, 52, 52, 52,
            53, 53, 53, 53, 53, 53, 53,
            1, 1, 1, 1, 1, 1, 1};
        testCal.setFirstDayOfWeek(SUNDAY);
        for (int j = 0; j < DATA.length; j += 22) {
            logln("Minimal days in first week = " + DATA[j]
                    + "  Week starts on Sunday");
            testCal.setMinimalDaysInFirstWeek(DATA[j]);
            testCal.set(1997, DECEMBER, 21);
            for (int i = 0; i < 21; ++i) {
                int woy = testCal.get(WEEK_OF_YEAR);
                log("\t" + testCal.getTime() + " " + woy);
                if (woy != DATA[j + 1 + i]) {
                    log(" ERROR");
                    fail = true;
                } else {
                    logln(" OK");
                }

                // Now compute the time from the fields, and make sure we
                // get the same answer back.  This is a round-trip test.
                Date save = testCal.getTime();
                testCal.clear();
                testCal.set(YEAR, DATA[j + 1 + i] < 25 ? 1998 : 1997);
                testCal.set(WEEK_OF_YEAR, DATA[j + 1 + i]);
                testCal.set(DAY_OF_WEEK, (i % 7) + SUNDAY);
                if (!testCal.getTime().equals(save)) {
                    logln("  Parse failed: " + testCal.getTime());
                    fail = true;
                } else {
                    logln("  Passed");
                }

                testCal.setTime(save);
                testCal.add(DAY_OF_MONTH, 1);
            }
        }

        // Test field disambiguation with a few special hard-coded cases.
        // This shouldn't fail if the above cases aren't failing.
        @SuppressWarnings("deprecation")
        Object[] DISAM = {
            1998, 1, SUNDAY,
            new Date(97, DECEMBER, 28),
            1998, 2, SATURDAY,
            new Date(98, JANUARY, 10),
            1998, 53, THURSDAY,
            new Date(98, DECEMBER, 31),
            1998, 53, FRIDAY,
            new Date(99, JANUARY, 1)};
        testCal.setMinimalDaysInFirstWeek(3);
        testCal.setFirstDayOfWeek(SUNDAY);
        for (int i = 0; i < DISAM.length; i += 4) {
            int y = (Integer) DISAM[i];
            int woy = (Integer) DISAM[i + 1];
            int dow = (Integer) DISAM[i + 2];
            Date exp = (Date) DISAM[i + 3];
            testCal.clear();
            testCal.set(YEAR, y);
            testCal.set(WEEK_OF_YEAR, woy);
            testCal.set(DAY_OF_WEEK, dow);
            log(y + "-W" + woy + "-DOW" + dow);
            if (!testCal.getTime().equals(exp)) {
                logln("  FAILED expect: " + exp + "\n            got: " + testCal.getTime());
                fail = true;
            } else {
                logln("  OK");
            }
        }

        // Now try adding and rolling
        Object ADD = new Object();
        Object ROLL = new Object();
        @SuppressWarnings("deprecation")
        Object[] ADDROLL = {
            ADD, 1, new Date(98, DECEMBER, 25), new Date(99, JANUARY, 1),
            ADD, 1, new Date(97, DECEMBER, 28), new Date(98, JANUARY, 4),
            ROLL, 1, new Date(98, DECEMBER, 27), new Date(98, JANUARY, 4),
            ROLL, 1, new Date(99, DECEMBER, 24), new Date(99, DECEMBER, 31),
            ROLL, 1, new Date(99, DECEMBER, 25), new Date(99, JANUARY, 9)};
        testCal.setMinimalDaysInFirstWeek(3);
        testCal.setFirstDayOfWeek(SUNDAY);
        for (int i = 0; i < ADDROLL.length; i += 4) {
            int amount = (Integer) ADDROLL[i + 1];
            Date before = (Date) ADDROLL[i + 2];
            Date after = (Date) ADDROLL[i + 3];

            testCal.setTime(before);
            if (ADDROLL[i] == ADD) {
                testCal.add(WEEK_OF_YEAR, amount);
            } else {
                testCal.roll(WEEK_OF_YEAR, amount);
            }
            log((ADDROLL[i] == ADD ? "add(WOY," : "roll(WOY,")
                    + amount + ")\t     " + before
                    + "\n\t\t  => " + testCal.getTime());
            if (!after.equals(testCal.getTime())) {
                logln("\tFAIL\n\t\texp: " + after);
                fail = true;
            } else {
                logln("  OK");
            }

            testCal.setTime(after);
            if (ADDROLL[i] == ADD) {
                testCal.add(WEEK_OF_YEAR, -amount);
            } else {
                testCal.roll(WEEK_OF_YEAR, -amount);
            }
            log((ADDROLL[i] == ADD ? "add(WOY," : "roll(WOY,")
                    + (-amount) + ")     " + after
                    + "\n\t\t  => " + testCal.getTime());
            if (!before.equals(testCal.getTime())) {
                logln("\tFAIL\n\t\texp: " + before);
                fail = true;
            } else {
                logln("\tOK");
            }
        }

        if (fail) {
            errln("Fail: Week of year misbehaving");
        }
    }

    public void Test4106136() {
        Locale saveLocale = Locale.getDefault();
        try {
            Locale[] locales = {Locale.CHINESE, Locale.CHINA};
            for (int i = 0; i < locales.length; ++i) {
                Locale.setDefault(locales[i]);
                int[] n = {
                    getAvailableLocales().length,
                    DateFormat.getAvailableLocales().length,
                    NumberFormat.getAvailableLocales().length};
                for (int j = 0; j < n.length; ++j) {
                    if (n[j] == 0) {
                        errln("Fail: No locales for " + locales[i]);
                    }
                }
            }
        } finally {
            Locale.setDefault(saveLocale);
        }
    }

    @SuppressWarnings("deprecation")
    public void Test4108764() {
        Date d00 = new Date(97, MARCH, 15, 12, 00, 00);
        Date d01 = new Date(97, MARCH, 15, 12, 00, 56);
        Date d10 = new Date(97, MARCH, 15, 12, 34, 00);
        Date d11 = new Date(97, MARCH, 15, 12, 34, 56);
        Date epoch = new Date(70, JANUARY, 1);

        Calendar cal = Calendar.getInstance();
        cal.setTime(d11);

        cal.clear(MINUTE);
        logln(cal.getTime().toString());
        if (!cal.getTime().equals(d01)) {
            errln("Fail: clear(MINUTE) broken");
        }

        cal.set(SECOND, 0);
        logln(cal.getTime().toString());
        if (!cal.getTime().equals(d00)) {
            errln("Fail: set(SECOND, 0) broken");
        }

        cal.setTime(d11);
        cal.set(SECOND, 0);
        logln(cal.getTime().toString());
        if (!cal.getTime().equals(d10)) {
            errln("Fail: set(SECOND, 0) broken #2");
        }

        cal.clear(MINUTE);
        logln(cal.getTime().toString());
        if (!cal.getTime().equals(d00)) {
            errln("Fail: clear(MINUTE) broken #2");
        }

        cal.clear();
        logln(cal.getTime().toString());
        if (!cal.getTime().equals(epoch)) {
            errln("Fail: clear() broken Want " + epoch);
        }
    }

    @SuppressWarnings("deprecation")
    public void Test4114578() {
        Locale locale = Locale.getDefault();
        if (!TestUtils.usesGregorianCalendar(locale)) {
            logln("Skipping this test because locale is " + locale);
            return;
        }

        int ONE_HOUR = 60 * 60 * 1000;
        TimeZone saveZone = TimeZone.getDefault();
        boolean fail = false;
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("PST"));
            Calendar cal = Calendar.getInstance();
            long onset = new Date(98, APRIL, 5, 1, 0).getTime() + ONE_HOUR;
            long cease = new Date(98, OCTOBER, 25, 0, 0).getTime() + 2 * ONE_HOUR;

            final int ADD = 1;
            final int ROLL = 2;

            long[] DATA = {
                // Start            Action   Amt    Expected_change
                onset - ONE_HOUR,   ADD,      1,     ONE_HOUR,
                onset,              ADD,     -1,    -ONE_HOUR,
                onset - ONE_HOUR,   ROLL,     1,     ONE_HOUR,
                onset,              ROLL,    -1,    -ONE_HOUR,
                cease - ONE_HOUR,   ADD,      1,     ONE_HOUR,
                cease,              ADD,     -1,    -ONE_HOUR,
                // roll() was changed to support wall-clock-based roll (JDK-8152077). The
                // time value may jump 2 hours by skipping non-existent wall-clock time.
                // Note that JDK-4114578 was a problem of add(), not roll().
                cease - ONE_HOUR,   ROLL,     1,     ONE_HOUR * 2,
                cease,              ROLL,    -1,    -ONE_HOUR * 2};

            for (int i = 0; i < DATA.length; i += 4) {
                Date date = new Date(DATA[i]);
                int amt = (int) DATA[i + 2];
                long expectedChange = DATA[i + 3];

                log(date.toString());
                cal.setTime(date);

                switch ((int) DATA[i + 1]) {
                    case ADD:
                        log(" add (HOUR," + (amt < 0 ? "" : "+") + amt + ")= ");
                        cal.add(HOUR, amt);
                        break;
                    case ROLL:
                        log(" roll(HOUR," + (amt < 0 ? "" : "+") + amt + ")= ");
                        cal.roll(HOUR, amt);
                        break;
                }

                log(cal.getTime().toString());

                long change = cal.getTime().getTime() - date.getTime();
                if (change != expectedChange) {
                    fail = true;
                    logln(" FAIL");
                } else {
                    logln(" OK");
                }
            }
        } finally {
            TimeZone.setDefault(saveZone);
        }

        if (fail) {
            errln("Fail: roll/add misbehaves around DST onset/cease");
        }
    }

    /**
     * Make sure maximum for HOUR field is 11, not 12.
     */
    public void Test4118384() {
        Calendar cal = Calendar.getInstance();
        if (cal.getMaximum(HOUR) != 11
                || cal.getLeastMaximum(HOUR) != 11
                || cal.getActualMaximum(HOUR) != 11) {
            errln("Fail: maximum of HOUR field should be 11");
        }
    }

    /**
     * Check isLeapYear for BC years.
     */
    public void Test4125881() {
        Locale locale = Locale.getDefault();
        if (!TestUtils.usesGregorianCalendar(locale)) {
            logln("Skipping this test because locale is " + locale);
            return;
        }

        GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance();
        DateFormat fmt = new SimpleDateFormat("MMMM d, yyyy G");
        cal.clear();
        for (int y = -20; y <= 10; ++y) {
            cal.set(ERA, y < 1 ? GregorianCalendar.BC : GregorianCalendar.AD);
            cal.set(YEAR, y < 1 ? 1 - y : y);
            logln(y + " = " + fmt.format(cal.getTime()) + " "
                    + cal.isLeapYear(y));
            if (cal.isLeapYear(y) != ((y + 40) % 4 == 0)) {
                errln("Leap years broken");
            }
        }
    }

    /**
     * Prove that GregorianCalendar is proleptic (it used to cut off
     * at 45 BC, and not have leap years before then).
     */
    public void Test4125892() {
        Locale locale = Locale.getDefault();
        if (!TestUtils.usesGregorianCalendar(locale)) {
            logln("Skipping this test because locale is " + locale);
            return;
        }

        GregorianCalendar cal = (GregorianCalendar) Calendar.getInstance();
        DateFormat fmt = new SimpleDateFormat("MMMM d, yyyy G");
        cal.clear();
        cal.set(ERA, GregorianCalendar.BC);
        cal.set(YEAR, 81); // 81 BC is a leap year (proleptically)
        cal.set(MONTH, FEBRUARY);
        cal.set(DATE, 28);
        cal.add(DATE, 1);
        if (cal.get(DATE) != 29
                || !cal.isLeapYear(-80)) { // -80 == 81 BC
            errln("Calendar not proleptic");
        }
    }

    /**
     * Calendar and GregorianCalendar hashCode() methods need improvement.
     * Calendar needs a good implementation that subclasses can override,
     * and GregorianCalendar should use that implementation.
     */
    public void Test4136399() {
        /* Note: This test is actually more strict than it has to be.
        * Technically, there is no requirement that unequal objects have
        * unequal hashes.  We only require equal objects to have equal hashes.
        * It is desirable for unequal objects to have distributed hashes, but
        * there is no hard requirement here.
        *
        * In this test we make assumptions about certain attributes of calendar
        * objects getting represented in the hash, which need not always be the
        * case (although it does work currently with the given test). */
        Calendar a = Calendar.getInstance();
        Calendar b = (Calendar) a.clone();
        if (a.hashCode() != b.hashCode()) {
            errln("Calendar hash code unequal for cloned objects");
        }

        b.setMinimalDaysInFirstWeek(7 - a.getMinimalDaysInFirstWeek());
        if (a.hashCode() == b.hashCode()) {
            errln("Calendar hash code ignores minimal days in first week");
        }
        b.setMinimalDaysInFirstWeek(a.getMinimalDaysInFirstWeek());

        b.setFirstDayOfWeek((a.getFirstDayOfWeek() % 7) + 1); // Next day
        if (a.hashCode() == b.hashCode()) {
            errln("Calendar hash code ignores first day of week");
        }
        b.setFirstDayOfWeek(a.getFirstDayOfWeek());

        b.setLenient(!a.isLenient());
        if (a.hashCode() == b.hashCode()) {
            errln("Calendar hash code ignores lenient setting");
        }
        b.setLenient(a.isLenient());

        // Assume getTimeZone() returns a reference, not a clone
        // of a reference -- this is true as of this writing
        b.getTimeZone().setRawOffset(a.getTimeZone().getRawOffset() + 60 * 60 * 1000);
        if (a.hashCode() == b.hashCode()) {
            errln("Calendar hash code ignores zone");
        }
        b.getTimeZone().setRawOffset(a.getTimeZone().getRawOffset());

        GregorianCalendar c = new GregorianCalendar();
        GregorianCalendar d = (GregorianCalendar) c.clone();
        if (c.hashCode() != d.hashCode()) {
            errln("GregorianCalendar hash code unequal for clones objects");
        }
        Date cutover = c.getGregorianChange();
        d.setGregorianChange(new Date(cutover.getTime() + 24 * 60 * 60 * 1000));
        if (c.hashCode() == d.hashCode()) {
            errln("GregorianCalendar hash code ignores cutover");
        }
    }

    /**
     * GregorianCalendar.equals() ignores cutover date
     */
    public void Test4141665() {
        GregorianCalendar cal = new GregorianCalendar();
        GregorianCalendar cal2 = (GregorianCalendar) cal.clone();
        Date cut = cal.getGregorianChange();
        Date cut2 = new Date(cut.getTime() + 100 * 24 * 60 * 60 * 1000L); // 100 days later
        if (!cal.equals(cal2)) {
            errln("Cloned GregorianCalendars not equal");
        }
        cal2.setGregorianChange(cut2);
        if (cal.equals(cal2)) {
            errln("GregorianCalendar.equals() ignores cutover");
        }
    }

    /**
     * Bug states that ArrayIndexOutOfBoundsException is thrown by GregorianCalendar.roll()
     * when IllegalArgumentException should be.
     */
    public void Test4142933() {
        GregorianCalendar calendar = new GregorianCalendar();
        try {
            calendar.roll(-1, true);
            errln("Test failed, no exception trown");
        } catch (IllegalArgumentException e) {
            // OK: Do nothing
            // logln("Test passed");
        } catch (Exception e) {
            errln("Test failed. Unexpected exception is thrown: " + e);
            e.printStackTrace();
        }
    }

    /**
     * GregorianCalendar handling of Dates Long.MIN_VALUE and Long.MAX_VALUE is
     * confusing; unless the time zone has a raw offset of zero, one or the
     * other of these will wrap.  We've modified the test given in the bug
     * report to therefore only check the behavior of a calendar with a zero raw
     * offset zone.
     */
    public void Test4145158() {
        GregorianCalendar calendar = new GregorianCalendar();

        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));

        calendar.setTime(new Date(Long.MIN_VALUE));
        int year1 = calendar.get(YEAR);
        int era1 = calendar.get(ERA);

        calendar.setTime(new Date(Long.MAX_VALUE));
        int year2 = calendar.get(YEAR);
        int era2 = calendar.get(ERA);

        if (year1 == year2 && era1 == era2) {
            errln("Fail: Long.MIN_VALUE or Long.MAX_VALUE wrapping around");
        }
    }

    /**
     * Maximum value for YEAR field wrong.
     */
    public void Test4145983() {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date[] DATES = {new Date(Long.MAX_VALUE), new Date(Long.MIN_VALUE)};
        for (int i = 0; i < DATES.length; ++i) {
            calendar.setTime(DATES[i]);
            int year = calendar.get(YEAR);
            int maxYear = calendar.getMaximum(YEAR);
            if (year > maxYear) {
                errln("Failed for " + DATES[i].getTime() + " ms: year="
                        + year + ", maxYear=" + maxYear);
            }
        }
    }

    /**
     * This is a bug in the validation code of GregorianCalendar.  As reported,
     * the bug seems worse than it really is, due to a bug in the way the bug
     * report test was written.  In reality the bug is restricted to the DAY_OF_YEAR
     * field. - liu 6/29/98
     */
    public void Test4147269() {
        final String[] fieldName = {
            "ERA",
            "YEAR",
            "MONTH",
            "WEEK_OF_YEAR",
            "WEEK_OF_MONTH",
            "DAY_OF_MONTH",
            "DAY_OF_YEAR",
            "DAY_OF_WEEK",
            "DAY_OF_WEEK_IN_MONTH",
            "AM_PM",
            "HOUR",
            "HOUR_OF_DAY",
            "MINUTE",
            "SECOND",
            "MILLISECOND",
            "ZONE_OFFSET",
            "DST_OFFSET"};
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setLenient(false);
        @SuppressWarnings("deprecation")
        Date date = new Date(1996 - 1900, JANUARY, 3); // Arbitrary date
        for (int field = 0; field < FIELD_COUNT; field++) {
            calendar.setTime(date);
            // Note: In the bug report, getActualMaximum() was called instead
            // of getMaximum() -- this was an error.  The validation code doesn't
            // use getActualMaximum(), since that's too costly.
            int max = calendar.getMaximum(field);
            int value = max + 1;
            calendar.set(field, value);
            try {
                calendar.getTime(); // Force time computation
                // We expect an exception to be thrown. If we fall through
                // to the next line, then we have a bug.
                errln("Test failed with field " + fieldName[field]
                        + ", date before: " + date
                        + ", date after: " + calendar.getTime()
                        + ", value: " + value + " (max = " + max + ")");
            } catch (IllegalArgumentException e) {
            }
        }
    }

    /**
     * Reported bug is that a GregorianCalendar with a cutover of Date(Long.MAX_VALUE)
     * doesn't behave as a pure Julian calendar.
     * CANNOT REPRODUCE THIS BUG
     */
    public void Test4149677() {
        TimeZone[] zones = {TimeZone.getTimeZone("GMT"),
            TimeZone.getTimeZone("PST"),
            TimeZone.getTimeZone("EAT")};
        for (int i = 0; i < zones.length; ++i) {
            GregorianCalendar calendar = new GregorianCalendar(zones[i]);

            // Make sure extreme values don't wrap around
            calendar.setTime(new Date(Long.MIN_VALUE));
            if (calendar.get(ERA) != GregorianCalendar.BC) {
                errln("Fail: Date(Long.MIN_VALUE) has an AD year in " + zones[i]);
            }
            calendar.setTime(new Date(Long.MAX_VALUE));
            if (calendar.get(ERA) != GregorianCalendar.AD) {
                errln("Fail: Date(Long.MAX_VALUE) has a BC year in " + zones[i]);
            }

            calendar.setGregorianChange(new Date(Long.MAX_VALUE));
            // to obtain a pure Julian calendar

            boolean is100Leap = calendar.isLeapYear(100);
            if (!is100Leap) {
                errln("test failed with zone " + zones[i].getID());
                errln(" cutover date is Date(Long.MAX_VALUE)");
                errln(" isLeapYear(100) returns: " + is100Leap);
            }
        }
    }

    /**
     * Calendar and Date HOUR broken.  If HOUR is out-of-range, Calendar
     * and Date classes will misbehave.
     */
    public void Test4162587() {
        TimeZone savedTz = TimeZone.getDefault();
        TimeZone tz = TimeZone.getTimeZone("PST");
        TimeZone.setDefault(tz);
        GregorianCalendar cal = new GregorianCalendar(tz);
        Date d;

        try {
            for (int i = 0; i < 5; ++i) {
                if (i > 0) {
                    logln("---");
                }

                cal.clear();
                cal.set(1998, APRIL, 5, i, 0);
                d = cal.getTime();
                String s0 = d.toString();
                logln("0 " + i + ": " + s0);

                cal.clear();
                cal.set(1998, APRIL, 4, i + 24, 0);
                d = cal.getTime();
                String sPlus = d.toString();
                logln("+ " + i + ": " + sPlus);

                cal.clear();
                cal.set(1998, APRIL, 6, i - 24, 0);
                d = cal.getTime();
                String sMinus = d.toString();
                logln("- " + i + ": " + sMinus);

                if (!s0.equals(sPlus) || !s0.equals(sMinus)) {
                    errln("Fail: All three lines must match");
                }
            }
        } finally {
            TimeZone.setDefault(savedTz);
        }
    }

    /**
     * Adding 12 months behaves differently from adding 1 year
     */
    public void Test4165343() {
        GregorianCalendar calendar = new GregorianCalendar(1996, FEBRUARY, 29);
        Date start = calendar.getTime();
        logln("init date: " + start);
        calendar.add(MONTH, 12);
        Date date1 = calendar.getTime();
        logln("after adding 12 months: " + date1);
        calendar.setTime(start);
        calendar.add(YEAR, 1);
        Date date2 = calendar.getTime();
        logln("after adding one year : " + date2);
        if (date1.equals(date2)) {
            logln("Test passed");
        } else {
            errln("Test failed");
        }
    }

    /**
     * GregorianCalendar.getActualMaximum() does not account for first day of week.
     */
    public void Test4166109() {
        /* Test month:
        *
        *      March 1998
        * Su Mo Tu We Th Fr Sa
        *  1  2  3  4  5  6  7
        *  8  9 10 11 12 13 14
        * 15 16 17 18 19 20 21
        * 22 23 24 25 26 27 28
        * 29 30 31
         */
        boolean passed = true;
        int field = WEEK_OF_MONTH;

        GregorianCalendar calendar = new GregorianCalendar(Locale.US);
        calendar.set(1998, MARCH, 1);
        calendar.setMinimalDaysInFirstWeek(1);
        logln("Date:  " + calendar.getTime());

        int firstInMonth = calendar.get(DAY_OF_MONTH);

        for (int firstInWeek = SUNDAY; firstInWeek <= SATURDAY; firstInWeek++) {
            calendar.setFirstDayOfWeek(firstInWeek);
            int returned = calendar.getActualMaximum(field);
            int expected = (31 + ((firstInMonth - firstInWeek + 7) % 7) + 6) / 7;

            logln("First day of week = " + firstInWeek
                    + "  getActualMaximum(WEEK_OF_MONTH) = " + returned
                    + "  expected = " + expected
                    + ((returned == expected) ? "  ok" : "  FAIL"));

            if (returned != expected) {
                passed = false;
            }
        }
        if (!passed) {
            errln("Test failed");
        }
    }

    /**
     * Calendar.getActualMaximum(YEAR) works wrong.
     *
     * Note: Before 1.5, this test case assumed that
     * setGregorianChange didn't change object's date. But it was
     * changed. See 4928615.
     */
    public void Test4167060() {
        int field = YEAR;
        DateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy G",
                Locale.US);

        int[][] dates = {
            // year, month, day of month
            {100, NOVEMBER, 1},
            {-99 /*100BC*/, JANUARY, 1},
            {1996, FEBRUARY, 29}};

        String[] id = {"Hybrid", "Gregorian", "Julian"};

        for (int k = 0; k < 3; ++k) {
            logln("--- " + id[k] + " ---");

            for (int j = 0; j < dates.length; ++j) {
                GregorianCalendar calendar = new GregorianCalendar();
                if (k == 1) {
                    calendar.setGregorianChange(new Date(Long.MIN_VALUE));
                } else if (k == 2) {
                    calendar.setGregorianChange(new Date(Long.MAX_VALUE));
                }
                calendar.set(dates[j][0], dates[j][1], dates[j][2]);
                format.setCalendar((Calendar) calendar.clone());

                Date dateBefore = calendar.getTime();

                int maxYear = calendar.getActualMaximum(field);
                logln("maxYear: " + maxYear + " for " + format.format(calendar.getTime()));
                logln("date before: " + format.format(dateBefore));

                int[] years = {2000, maxYear - 1, maxYear, maxYear + 1};

                for (int i = 0; i < years.length; i++) {
                    boolean valid = years[i] <= maxYear;
                    calendar.set(field, years[i]);
                    Date dateAfter = calendar.getTime();
                    int newYear = calendar.get(field);
                    calendar.setTime(dateBefore); // restore calendar for next use

                    logln(" Year " + years[i] + (valid ? " ok " : " bad")
                            + " => " + format.format(dateAfter));
                    if (valid && newYear != years[i]) {
                        errln("  FAIL: " + newYear + " should be valid; date, month and time shouldn't change");
                    } else if (!valid && newYear == years[i]) {
                        errln("  FAIL: " + newYear + " should be invalid");
                    }
                }
            }
        }
    }

    /**
     * Calendar.roll broken
     * This bug relies on the TimeZone bug 4173604 to also be fixed.
     */
    public void Test4173516() {
        Locale locale = Locale.getDefault();
        if (!TestUtils.usesGregorianCalendar(locale)) {
            logln("Skipping this test because locale is " + locale);
            return;
        }

        int[][] fieldsList = {
            {1997, FEBRUARY, 1, 10, 45, 15, 900},
            {1999, DECEMBER, 22, 23, 59, 59, 999},
            // test case for 4960642 with default cutover
            {1582, OCTOBER, 4, 23, 59, 59, 999}};
        String[] fieldNames = {
            "ERA", "YEAR", "MONTH", "WEEK_OF_YEAR", "WEEK_OF_MONTH",
            "DAY_OF_MONTH", "DAY_OF_YEAR", "DAY_OF_WEEK", "DAY_OF_WEEK_IN_MONTH",
            "AM_PM", "HOUR", "HOUR_OF_DAY", "MINUTE", "SECOND", "MILLISECOND",
            "ZONE_OFFSET", "DST_OFFSET"};

        Locale savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        int limit = 40;

        try {
            GregorianCalendar cal = new GregorianCalendar();

            cal.setTime(new Date(0));
            cal.roll(HOUR, 0x7F000000);
            cal.roll(HOUR, -0x7F000000);
            if (cal.getTime().getTime() != 0) {
                errln("Hour rolling broken. expected 0, got " + cal.getTime().getTime());
            }

            for (int op = 0; op < 2; ++op) {
                logln("Testing GregorianCalendar " + (op == 0 ? "add" : "roll"));

                for (int field = 0; field < FIELD_COUNT; ++field) {
                    if (field != ZONE_OFFSET
                            && field != DST_OFFSET) {
                        for (int j = 0; j < fieldsList.length; ++j) {
                            int[] fields = fieldsList[j];
                            cal.clear();
                            cal.set(fields[0], fields[1], fields[2],
                                    fields[3], fields[4], fields[5]);
                            cal.set(MILLISECOND, fields[6]);
                            for (int i = 0; i < 2 * limit; i++) {
                                if (op == 0) {
                                    cal.add(field, i < limit ? 1 : -1);
                                } else {
                                    cal.roll(field, i < limit ? 1 : -1);
                                }
                            }

                            if (cal.get(YEAR) != fields[0]
                                    || cal.get(MONTH) != fields[1]
                                    || cal.get(DATE) != fields[2]
                                    || cal.get(HOUR_OF_DAY) != fields[3]
                                    || cal.get(MINUTE) != fields[4]
                                    || cal.get(SECOND) != fields[5]
                                    || cal.get(MILLISECOND) != fields[6]) {
                                errln("Field " + field
                                        + " (" + fieldNames[field]
                                        + ") FAIL, expected "
                                        + fields[0]
                                        + "/" + (fields[1] + 1)
                                        + "/" + fields[2]
                                        + " " + fields[3]
                                        + ":" + fields[4]
                                        + ":" + fields[5]
                                        + "." + fields[6]
                                        + ", got " + cal.get(YEAR)
                                        + "/" + (cal.get(MONTH) + 1)
                                        + "/" + cal.get(DATE)
                                        + " " + cal.get(HOUR_OF_DAY)
                                        + ":" + cal.get(MINUTE)
                                        + ":" + cal.get(SECOND)
                                        + "." + cal.get(MILLISECOND));

                                cal.clear();
                                cal.set(fields[0], fields[1], fields[2],
                                        fields[3], fields[4], fields[5]);
                                cal.set(MILLISECOND, fields[6]);
                                errln(cal.get(YEAR)
                                        + "/" + (cal.get(MONTH) + 1)
                                        + "/" + cal.get(DATE)
                                        + " " + cal.get(HOUR_OF_DAY)
                                        + ":" + cal.get(MINUTE)
                                        + ":" + cal.get(SECOND)
                                        + "." + cal.get(MILLISECOND));

                                long prev = cal.getTime().getTime();
                                for (int i = 0; i < 2 * limit; i++) {
                                    if (op == 0) {
                                        cal.add(field, i < limit ? 1 : -1);
                                    } else {
                                        cal.roll(field, i < limit ? 1 : -1);
                                    }
                                    long t = cal.getTime().getTime();
                                    long delta = t - prev;
                                    prev = t;
                                    errln((op == 0 ? "add(" : "roll(")
                                            + fieldNames[field] + ", "
                                            + (i < limit ? "+" : "-") + "1) => "
                                            + cal.get(YEAR)
                                            + "/" + (cal.get(MONTH) + 1)
                                            + "/" + cal.get(DATE)
                                            + " " + cal.get(HOUR_OF_DAY)
                                            + ":" + cal.get(MINUTE)
                                            + ":" + cal.get(SECOND)
                                            + "." + cal.get(MILLISECOND)
                                            + " d=" + delta);
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            Locale.setDefault(savedLocale);
        }
    }

    public void Test4174361() {
        GregorianCalendar calendar = new GregorianCalendar(1996, 1, 29);

        calendar.add(MONTH, 10);
        Date date1 = calendar.getTime();
        int d1 = calendar.get(DAY_OF_MONTH);

        calendar = new GregorianCalendar(1996, 1, 29);
        calendar.add(MONTH, 11);
        Date date2 = calendar.getTime();
        int d2 = calendar.get(DAY_OF_MONTH);

        if (d1 != d2) {
            errln("adding months to Feb 29 broken");
        }
    }

    /**
     * Calendar does not update field values when setTimeZone is called.
     */
    public void Test4177484() {
        TimeZone PST = TimeZone.getTimeZone("PST");
        TimeZone EST = TimeZone.getTimeZone("EST");

        Calendar cal = Calendar.getInstance(PST, Locale.US);
        cal.clear();
        cal.set(1999, 3, 21, 15, 5, 0); // Arbitrary
        int h1 = cal.get(HOUR_OF_DAY);
        cal.setTimeZone(EST);
        int h2 = cal.get(HOUR_OF_DAY);
        if (h1 == h2) {
            errln("FAIL: Fields not updated after setTimeZone");
        }

        // getTime() must NOT change when time zone is changed.
        // getTime() returns zone-independent time in ms.
        cal.clear();
        cal.setTimeZone(PST);
        cal.set(HOUR_OF_DAY, 10);
        Date pst10 = cal.getTime();
        cal.setTimeZone(EST);
        Date est10 = cal.getTime();
        if (!pst10.equals(est10)) {
            errln("FAIL: setTimeZone changed time");
        }
    }

    /**
     * Week of year is wrong at the start and end of the year.
     */
    public void Test4197699() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setFirstDayOfWeek(MONDAY);
        cal.setMinimalDaysInFirstWeek(4);
        DateFormat fmt = new SimpleDateFormat("E dd MMM yyyy  'DOY='D 'WOY='w");
        fmt.setCalendar(cal);

        int[] DATA = {
            2000, JANUARY, 1, 52,
            2001, DECEMBER, 31, 1};

        for (int i = 0; i < DATA.length;) {
            cal.set(DATA[i++], DATA[i++], DATA[i++]);
            int expWOY = DATA[i++];
            int actWOY = cal.get(WEEK_OF_YEAR);
            if (expWOY == actWOY) {
                logln("Ok: " + fmt.format(cal.getTime()));
            } else {
                errln("FAIL: " + fmt.format(cal.getTime())
                        + ", expected WOY=" + expWOY);
                cal.add(DATE, -8);
                for (int j = 0; j < 14; ++j) {
                    cal.add(DATE, 1);
                    logln(fmt.format(cal.getTime()));
                }
            }
        }
    }

    /**
     * Calendar DAY_OF_WEEK_IN_MONTH fields->time broken.  The problem
     * is in the field disambiguation code in GregorianCalendar.  This
     * code is supposed to choose the most recent set of fields
     * among the following:
     *
     *   MONTH + DAY_OF_MONTH
     *   MONTH + WEEK_OF_MONTH + DAY_OF_WEEK
     *   MONTH + DAY_OF_WEEK_IN_MONTH + DAY_OF_WEEK
     *   DAY_OF_YEAR
     *   WEEK_OF_YEAR + DAY_OF_WEEK
     */
    @SuppressWarnings("deprecation")
    public void Test4209071() {
        Calendar cal = Calendar.getInstance(Locale.US);

        // General field setting test
        int Y = 1995 - 1900;

        Object[] FIELD_DATA = {
            // Add new test cases as needed.

            // 0
            new int[]{}, new Date(Y, JANUARY, 1),
            // 1
            new int[]{MONTH, MARCH},
            new Date(Y, MARCH, 1),
            // 2
            new int[]{DAY_OF_WEEK, WEDNESDAY},
            new Date(Y, JANUARY, 4),
            // 3
            new int[]{DAY_OF_WEEK, THURSDAY,
                DAY_OF_MONTH, 18,},
            new Date(Y, JANUARY, 18),
            // 4
            new int[]{DAY_OF_MONTH, 18,
                DAY_OF_WEEK, THURSDAY,},
            new Date(Y, JANUARY, 18),
            // 5  (WOM -1 is in previous month)
            new int[]{DAY_OF_MONTH, 18,
                WEEK_OF_MONTH, -1,
                DAY_OF_WEEK, THURSDAY,},
            new Date(Y - 1, DECEMBER, 22),
            // 6
            new int[]{DAY_OF_MONTH, 18,
                WEEK_OF_MONTH, 4,
                DAY_OF_WEEK, THURSDAY,},
            new Date(Y, JANUARY, 26),
            // 7  (DIM -1 is in same month)
            new int[]{DAY_OF_MONTH, 18,
                DAY_OF_WEEK_IN_MONTH, -1,
                DAY_OF_WEEK, THURSDAY,},
            new Date(Y, JANUARY, 26),
            // 8
            new int[]{WEEK_OF_YEAR, 9,
                DAY_OF_WEEK, WEDNESDAY,},
            new Date(Y, MARCH, 1),
            // 9
            new int[]{MONTH, OCTOBER,
                DAY_OF_WEEK_IN_MONTH, 1,
                DAY_OF_WEEK, FRIDAY,},
            new Date(Y, OCTOBER, 6),
            // 10
            new int[]{MONTH, OCTOBER,
                WEEK_OF_MONTH, 2,
                DAY_OF_WEEK, FRIDAY,},
            new Date(Y, OCTOBER, 13),
            // 11
            new int[]{MONTH, OCTOBER,
                DAY_OF_MONTH, 15,
                DAY_OF_YEAR, 222,},
            new Date(Y, AUGUST, 10),
            // 12
            new int[]{DAY_OF_WEEK, THURSDAY,
                MONTH, DECEMBER,},
            new Date(Y, DECEMBER, 7)};

        for (int i = 0; i < FIELD_DATA.length; i += 2) {
            int[] fields = (int[]) FIELD_DATA[i];
            Date exp = (Date) FIELD_DATA[i + 1];

            cal.clear();
            cal.set(YEAR, Y + 1900);
            for (int j = 0; j < fields.length; j += 2) {
                cal.set(fields[j], fields[j + 1]);
            }

            Date act = cal.getTime();
            if (!act.equals(exp)) {
                errln("FAIL: Test " + (i / 2) + " got " + act
                        + ", want " + exp
                        + " (see test/java/util/Calendar/CalendarRegression.java");
            }
        }

        // Test specific failure reported in bug
        @SuppressWarnings("deprecation")
        Object[] DATA = {
            1, new Date(1997 - 1900, JANUARY, 5),
            4, new Date(1997 - 1900, JANUARY, 26),
            8, new Date(1997 - 1900, FEBRUARY, 23),
            -1, new Date(1997 - 1900, JANUARY, 26),
            -4, new Date(1997 - 1900, JANUARY, 5),
            -8, new Date(1996 - 1900, DECEMBER, 8)};
        for (int i = 0; i < DATA.length; i += 2) {
            cal.clear();
            cal.set(DAY_OF_WEEK_IN_MONTH,
                    ((Number) DATA[i]).intValue());
            cal.set(DAY_OF_WEEK, SUNDAY);
            cal.set(MONTH, JANUARY);
            cal.set(YEAR, 1997);
            Date actual = cal.getTime();
            if (!actual.equals(DATA[i + 1])) {
                errln("FAIL: Sunday " + DATA[i]
                        + " of Jan 1997 -> " + actual
                        + ", want " + DATA[i + 1]);
            }
        }
    }

    public void Test4288792() throws Exception {
        TimeZone savedTZ = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        GregorianCalendar cal = new GregorianCalendar();
        try {
            for (int i = 1900; i < 2100; i++) {
                for (int j1 = 1; j1 <= 7; j1++) {
                    // Loop for MinimalDaysInFirstWeek: 1..7
                    for (int j = SUNDAY; j <= SATURDAY; j++) {
                        // Loop for FirstDayOfWeek: SUNDAY..SATURDAY
                        cal.clear();
                        cal.setMinimalDaysInFirstWeek(j1);
                        cal.setFirstDayOfWeek(j);
                        cal.set(YEAR, i);
                        int maxWeek = cal.getActualMaximum(WEEK_OF_YEAR);
                        cal.set(WEEK_OF_YEAR, maxWeek);
                        cal.set(DAY_OF_WEEK, j);

                        for (int k = 1; k < 7; k++) {
                            cal.add(DATE, 1);
                            int WOY = cal.get(WEEK_OF_YEAR);
                            if (WOY != maxWeek) {
                                errln(cal.getTime() + ",got=" + WOY
                                        + ",expected=" + maxWeek
                                        + ",min=" + j1 + ",first=" + j);
                            }
                        }

                        cal.add(DATE, 1);
                        int WOY = cal.get(WEEK_OF_YEAR);
                        if (WOY != 1) {
                            errln(cal.getTime() + ",got=" + WOY
                                    + ",expected=1,min=" + j1 + ",first" + j);
                        }
                    }
                }
            }
        } finally {
            TimeZone.setDefault(savedTZ);
        }
    }

    public void Test4328747() throws Exception {
        Calendar c = Calendar.getInstance(Locale.US);
        c.clear();
        c.set(1966, 0, 1); // 1 jan 1966

        // serialize
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream s = new ObjectOutputStream(out);
        s.writeObject(c);
        s.flush();

        // deserialize
        ObjectInputStream t = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()));
        Calendar result = (Calendar) t.readObject();

        // let recalculate fields with the same UTC time
        result.setTime(result.getTime());
        // Bug gives 1965 11 19
        if ((result.get(YEAR) != 1966) || (result.get(MONTH) != 0)
                || (result.get(DATE) != 1)) {
            errln("deserialized Calendar returned wrong date field(s): "
                    + result.get(YEAR) + "/" + result.get(MONTH) + "/" + result.get(DATE)
                    + ", expected 1966/0/1");
        }
    }

    /**
     * Test whether Calendar can be serialized/deserialized correctly
     * even if invalid/customized TimeZone is used.
     */
    public void Test4413980() {
        TimeZone savedTimeZone = TimeZone.getDefault();
        try {
            boolean pass = true;
            String[] IDs = new String[]{"Undefined", "PST", "US/Pacific",
                "GMT+3:00", "GMT-01:30"};
            for (int i = 0; i < IDs.length; i++) {
                TimeZone tz = TimeZone.getTimeZone(IDs[i]);
                TimeZone.setDefault(tz);

                Calendar c = Calendar.getInstance();

                // serialize
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ObjectOutputStream s = new ObjectOutputStream(out);
                s.writeObject(c);
                s.flush();

                // deserialize
                ObjectInputStream t = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()));

                if (!c.equals(t.readObject())) {
                    pass = false;
                    logln("Calendar instance which uses TimeZone <"
                            + IDs[i] + "> is incorrectly serialized/deserialized.");
                } else {
                    logln("Calendar instance which uses TimeZone <"
                            + IDs[i] + "> is correctly serialized/deserialized.");
                }
            }
            if (!pass) {
                errln("Fail: Calendar serialization/equality bug");
            }
        } catch (IOException | ClassNotFoundException e) {
            errln("Fail: " + e);
            e.printStackTrace();
        } finally {
            TimeZone.setDefault(savedTimeZone);
        }
    }

    /**
     * 4546637: Incorrect WEEK_OF_MONTH after changing First Day Of Week
     */
    public void Test4546637() {
        GregorianCalendar day = new GregorianCalendar(2001, NOVEMBER, 04);
        day.setMinimalDaysInFirstWeek(1);
        int wom = day.get(WEEK_OF_MONTH);

        day.setFirstDayOfWeek(MONDAY);
        if (day.get(WEEK_OF_MONTH) != 1) {
            errln("Fail: 2001/11/4 must be the first week of the month.");
        }
    }

    /**
     * 4623997: GregorianCalendar returns bad WEEK_OF_YEAR
     */
    public void Test4623997() {
        GregorianCalendar cal = new GregorianCalendar(2000, JANUARY, 1);

        int dow = cal.get(DAY_OF_WEEK);

        cal.setFirstDayOfWeek(MONDAY);
        cal.setMinimalDaysInFirstWeek(4);

        if (cal.get(WEEK_OF_YEAR) != 52) {
            errln("Fail: 2000/1/1 must be the 52nd week of the year.");
        }
    }

    /**
     * 4685354: Handling of Calendar fields setting state is broken
     *
     * <p>Need to use SimpleDateFormat to test because a call to
     * get(int) changes internal states of a Calendar.
     */
    public void Test4685354() {
        Locale locale = Locale.getDefault();
        if (!TestUtils.usesAsciiDigits(locale)
                || !TestUtils.usesGregorianCalendar(locale)) {
            logln("Skipping this test because locale is " + locale);
            return;
        }

        Calendar calendar = Calendar.getInstance(Locale.US);
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
        String expected = "1999/12/31";
        Date t;
        String s;

        try {
            calendar.setTime(df.parse(expected));
        } catch (Exception e) {
            throw new RuntimeException("Unexpected parse exception", e);
        }

        t = calendar.getTime();
        calendar.set(DAY_OF_MONTH, 33);
        t = calendar.getTime();
        calendar.set(DAY_OF_MONTH, 0);
        s = df.format(calendar.getTime());
        if (!expected.equals(s)) {
            errln("DAY_OF_MONTH w/o ZONE_OFFSET: expected: " + expected + ", got: " + s);
        }

        // The same thing must work with ZONE_OFFSET set
        try {
            calendar.setTime(df.parse(expected));
        } catch (Exception e) {
            throw new RuntimeException("Unexpected parse exception", e);
        }
        t = calendar.getTime();
        calendar.set(ZONE_OFFSET, calendar.get(ZONE_OFFSET));
        calendar.set(DAY_OF_MONTH, 33);
        t = calendar.getTime();
        calendar.set(DAY_OF_MONTH, 0);
        s = df.format(calendar.getTime());
        if (!expected.equals(s)) {
            errln("DAY_OF_MONTH: expected: " + expected + ", got: " + s);
        }

        expected = "1999/12/24"; // 0th week of 2000
        calendar.clear();
        Date initialDate = null;
        try {
            initialDate = df.parse(expected);
            calendar.setTime(initialDate);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected parse exception", e);
        }
        t = calendar.getTime();
        calendar.set(ZONE_OFFSET, calendar.get(ZONE_OFFSET));
        // jump to the next year
        calendar.set(WEEK_OF_YEAR, 100);
        t = calendar.getTime();
        calendar.set(WEEK_OF_YEAR, 0);
        s = df.format(calendar.getTime());
        if (!expected.equals(s)) {
            errln("WEEK_OF_YEAR: expected: " + expected + ", got: " + s);
        }
        // change the state back
        calendar.clear();
        calendar.setTime(initialDate);
        calendar.set(ZONE_OFFSET, calendar.get(ZONE_OFFSET));
        // jump to next month
        calendar.set(WEEK_OF_MONTH, 7);
        t = calendar.getTime();
        calendar.set(WEEK_OF_MONTH, 0);
        s = df.format(calendar.getTime());
        if (!expected.equals(s)) {
            errln("WEEK_OF_MONTH: expected: " + expected + ", got: " + s);
        }

        // Make sure the time fields work correctly.
        calendar.clear();
        df = new SimpleDateFormat("HH:mm:ss");
        TimeZone tz = TimeZone.getTimeZone("GMT");
        df.setTimeZone(tz);
        calendar.setTimeZone(tz);
        expected = "22:59:59";
        try {
            calendar.setTime(df.parse(expected));
        } catch (Exception e) {
            throw new RuntimeException("Unexpected parse exception", e);
        }
        t = calendar.getTime();
        // time should be 22:59:59.
        calendar.set(MINUTE, 61);
        // time should be 23:01:59.
        t = calendar.getTime();
        calendar.set(MINUTE, -1);
        // time should be back to 22:59:59.
        s = df.format(calendar.getTime());
        if (!expected.equals(s)) {
            errln("MINUTE: expected: " + expected + ", got: " + s);
        }
    }

    /**
     * 4655637: Calendar.set() for DAY_OF_WEEK does not return the right value
     *
     * <p>Need to use SimpleDateFormat to test because a call to
     * get(int) changes internal states of a Calendar.
     */
    public void Test4655637() {
        Locale locale = Locale.getDefault();
        if (!TestUtils.usesGregorianCalendar(locale)) {
            logln("Skipping this test because locale is " + locale);
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(1029814211523L));
        cal.set(YEAR, 2001);
        Date t = cal.getTime();
        cal.set(MONTH, JANUARY);
        t = cal.getTime();

        cal.set(DAY_OF_MONTH, 8);
        t = cal.getTime();

        cal.set(DAY_OF_WEEK, MONDAY);
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
        String expected = "2001/01/08";
        String s = df.format(cal.getTime());
        if (!expected.equals(s)) {
            errln("expected: " + expected + ", got: " + s);
        }
    }

    /**
     * 4683492: Invalid value for MONTH in GregorianCalendar causes exception in getTime().
     *
     * <p>Need to use SimpleDateFormat to test because a call to
     * get(int) changes internal states of a Calendar.
     *
     * <p>This test case throws ArrayIndexOutOfBoundsException without the fix.
     */
    public void Test4683492() {
        Calendar cal = new GregorianCalendar(2002, 3, 29, 10, 0, 0);
        cal.set(DAY_OF_WEEK, FRIDAY);
        cal.set(DAY_OF_WEEK_IN_MONTH, -1);
        cal.set(MONTH, 12);
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd", Locale.US);
        String expected = "2003/01/31";
        String s = df.format(cal.getTime());
        if (!expected.equals(s)) {
            errln("expected: " + expected + ", got: " + s);
        }
    }

    /**
     * 4080631: Calendar.hashCode is amazingly bad
     */
    public void Test4080631() {
        Calendar cal = Calendar.getInstance();
        int h1 = cal.hashCode();
        cal.add(SECOND, +1);
        int h2 = cal.hashCode();
        Calendar cal2 = (Calendar) cal.clone();
        cal.add(MILLISECOND, +1);
        int h3 = cal.hashCode();
        logln("hash code: h1=" + h1 + ", h2=" + h2 + ", h3=" + h3);
        if (h1 == h2 || h1 == h3 || h2 == h3) {
            errln("hash code is poor: hashCode=" + h1);
        }
        h2 = cal2.hashCode();
        cal.add(MILLISECOND, -1);
        int h4 = cal.hashCode();
        logln("hash code: h2=" + h2 + ", h4=" + h4);
        if (cal.equals(cal2) && h2 != h4) {
            errln("broken hash code: h2=" + h2 + ", h4=" + h4);
        }
        int x = cal.getFirstDayOfWeek() + 3;
        if (x > SATURDAY) {
            x -= 7;
        }
        cal.setFirstDayOfWeek(x);
        int h5 = cal.hashCode();
        logln("hash code: h4=" + h4 + ", h5=" + h5);
        if (h4 == h5) {
            errln("has code is poor with first day of week param: hashCode=" + h4);
        }
    }

    /**
     * 4125161: RFE: GregorianCalendar needs more era names (BCE and CE)
     */
    /*
    public void Test4125161() throws Exception {
        Class gc = GregorianCalendar.class;
        Field f;
        int mod;
        f = gc.getDeclaredField("BCE");
        mod = f.getModifiers();
        if (!Modifier.isStatic(mod) || !Modifier.isFinal(mod)) {
            errln("BCE: wrong modifiers: " + mod);
        }
        f = gc.getDeclaredField("CE");
        mod = f.getModifiers();
        if (!Modifier.isStatic(mod) || !Modifier.isFinal(mod)) {
            errln("CE: wrong modifiers: " + mod);
        }
        if (GregorianCalendar.BCE != GregorianCalendar.BC
            || GregorianCalendar.CE != GregorianCalendar.AD) {
            errln("Wrong BCE and/or CE values");
        }
    }
     */
    /**
     * 4167995: GregorianCalendar.setGregorianChange() not to spec
     */
    public void Test4167995() {
        Koyomi gc = new Koyomi(TimeZone.getTimeZone("GMT"));
        logln("Hybrid: min date");
        gc.setTime(new Date(Long.MIN_VALUE));
        if (!gc.checkDate(292269055, DECEMBER, 2, SUNDAY)
                || !gc.checkFieldValue(ERA, GregorianCalendar.BC)) {
            errln(gc.getMessage());
        }
        logln("Hybrid: max date");
        gc.setTime(new Date(Long.MAX_VALUE));
        if (!gc.checkDate(292278994, AUGUST, 17, SUNDAY)
                || !gc.checkFieldValue(ERA, GregorianCalendar.AD)) {
            errln(gc.getMessage());
        }

        gc.setGregorianChange(new Date(Long.MIN_VALUE));
        logln("Gregorian: min date");
        gc.setTime(new Date(Long.MIN_VALUE));
        if (!gc.checkDate(292275056, MAY, 16, SUNDAY)
                || !gc.checkFieldValue(ERA, GregorianCalendar.BC)) {
            errln(gc.getMessage());
        }
        logln("Gregorian: max date");
        gc.setTime(new Date(Long.MAX_VALUE));
        if (!gc.checkDate(292278994, AUGUST, 17, SUNDAY)
                || !gc.checkFieldValue(ERA, GregorianCalendar.AD)) {
            errln(gc.getMessage());
        }

        gc.setGregorianChange(new Date(Long.MAX_VALUE));
        logln("Julian: min date");
        gc.setTime(new Date(Long.MIN_VALUE));
        if (!gc.checkDate(292269055, DECEMBER, 2, SUNDAY)
                || !gc.checkFieldValue(ERA, GregorianCalendar.BC)) {
            errln(gc.getMessage());
        }
        logln("Julian: max date");
        gc.setTime(new Date(Long.MAX_VALUE));
        if (!gc.checkDate(292272993, JANUARY, 4, SUNDAY)
                || !gc.checkFieldValue(ERA, GregorianCalendar.AD)) {
            errln(gc.getMessage());
        }
    }

    /**
     * 4340146: Calendar.equals modifies state
     */
    public void Test4340146() {
        Koyomi cal = new Koyomi();
        cal.clear();
        cal.set(2003, OCTOBER, 32);
        cal.equals(new Koyomi());
        if (!cal.checkInternalDate(2003, OCTOBER, 32)) {
            errln(cal.getMessage());
        }
        new Koyomi().equals(cal);
        if (!cal.checkInternalDate(2003, OCTOBER, 32)) {
            errln(cal.getMessage());
        }
    }

    /**
     * 4639407: GregorianCalendar doesn't work in non-lenient due to timezone bounds checking
     */
    public void Test4639407() {
        // The following operations in non-lenient mode shouldn't
        // throw IllegalArgumentException.
        Koyomi cal = new Koyomi(TimeZone.getTimeZone("Pacific/Kiritimati"));
        cal.setLenient(false);
        cal.set(2003, OCTOBER, 10);
        cal.getTime();
        cal.setTimeZone(TimeZone.getTimeZone("Pacific/Tongatapu"));
        cal.set(2003, OCTOBER, 10);
        cal.getTime();
    }

    /**
     * 4652815: rolling week-of-year back hundreds of weeks changes year
     */
    public void Test4652815() {
        Koyomi cal = new Koyomi(Locale.US);
        testRoll(cal, 2003, SEPTEMBER, 29);
        testRoll(cal, 2003, DECEMBER, 24);
        testRoll(cal, 1582, DECEMBER, 19);
        testRoll(cal, 1582, DECEMBER, 20);
    }

    private void testRoll(Koyomi cal, int year, int month, int dayOfMonth) {
        cal.clear();
        cal.set(year, month, dayOfMonth);
        cal.getTime(); // normalize fields
        logln("Roll backwards from " + cal.toDateString());
        for (int i = 0; i < 1000; i++) {
            cal.roll(WEEK_OF_YEAR, -i);
            if (!cal.checkFieldValue(YEAR, year)) {
                errln(cal.getMessage());
            }
        }
        logln("Roll forewards from " + cal.toDateString());
        for (int i = 0; i < 1000; i++) {
            cal.roll(WEEK_OF_YEAR, +i);
            if (!cal.checkFieldValue(YEAR, year)) {
                errln(cal.getMessage());
            }
        }
    }

    /**
     * 4652830: GregorianCalendar roll behaves unexpectedly for dates in BC era
     */
    public void Test4652830() {
        Koyomi cal = new Koyomi(Locale.US);
        cal.clear();
        logln("BCE 9-2-28 (leap year) roll DAY_OF_MONTH++ twice");
        cal.set(ERA, GregorianCalendar.BC);
        cal.set(9, FEBRUARY, 28);
        if (cal.getActualMaximum(DAY_OF_YEAR) != 366) {
            errln("    wrong actual max of DAY_OF_YEAR: got "
                    + cal.getActualMaximum(DAY_OF_YEAR) + " expected " + 366);
        }
        cal.roll(DAY_OF_MONTH, +1);
        if (!cal.checkFieldValue(ERA, GregorianCalendar.BC)
                || !cal.checkDate(9, FEBRUARY, 29)) {
            errln(cal.getMessage());
        }
        cal.roll(DAY_OF_MONTH, +1);
        if (!cal.checkFieldValue(ERA, GregorianCalendar.BC)
                || !cal.checkDate(9, FEBRUARY, 1)) {
            errln(cal.getMessage());
        }
    }

    /**
     * 4740554: GregorianCalendar.getActualMaximum is inconsistent with normalization
     */
    public void Test4740554() {
        logln("1999/(Feb+12)/1 should be normalized to 2000/Feb/1 for getActualMaximum");
        Koyomi cal = new Koyomi(Locale.US);
        cal.clear();
        cal.set(1999, FEBRUARY + 12, 1);
        if (!cal.checkActualMaximum(DAY_OF_YEAR, 366)) {
            errln(cal.getMessage());
        }
        if (!cal.checkActualMaximum(DAY_OF_MONTH, 29)) {
            errln(cal.getMessage());
        }
    }

    /**
     * 4936355: GregorianCalendar causes overflow/underflow with time of day calculation
     */
    public void Test4936355() {
        Koyomi cal = new Koyomi(TimeZone.getTimeZone("GMT"));
        cal.clear();
        cal.set(1970, JANUARY, 1);
        checkTimeCalculation(cal, HOUR_OF_DAY, Integer.MAX_VALUE,
                (long) Integer.MAX_VALUE * 60 * 60 * 1000);

        cal.clear();
        cal.set(1970, JANUARY, 1);
        checkTimeCalculation(cal, HOUR, Integer.MAX_VALUE,
                (long) Integer.MAX_VALUE * 60 * 60 * 1000);

        cal.clear();
        cal.set(1970, JANUARY, 1);
        checkTimeCalculation(cal, MINUTE, Integer.MAX_VALUE,
                (long) Integer.MAX_VALUE * 60 * 1000);

        cal.clear();
        // Make sure to use Gregorian dates (before and after the
        // set() call) for testing
        cal.set(250000, JANUARY, 1);
        checkTimeCalculation(cal, HOUR_OF_DAY, Integer.MIN_VALUE,
                (long) Integer.MIN_VALUE * 60 * 60 * 1000);

        cal.clear();
        cal.set(250000, JANUARY, 1);
        checkTimeCalculation(cal, HOUR, Integer.MIN_VALUE,
                (long) Integer.MIN_VALUE * 60 * 60 * 1000);

        cal.clear();
        cal.set(250000, JANUARY, 1);
        checkTimeCalculation(cal, MINUTE, Integer.MIN_VALUE,
                (long) Integer.MIN_VALUE * 60 * 1000);
    }

    private void checkTimeCalculation(Koyomi cal, int field, int value, long expectedDelta) {
        long time = cal.getTimeInMillis();
        cal.set(field, value);
        long time2 = cal.getTimeInMillis();
        if ((time + expectedDelta) != time2) {
            String s = value == Integer.MAX_VALUE ? "Integer.MAX_VALUE" : "Integer.MIN_VALUE";
            errln("set(" + Koyomi.getFieldName(field) + ", " + s + ") failed." + " got " + time2
                    + ", expected " + (time + expectedDelta));
        }
    }

    /**
     * 4722650: Calendar.equals can throw an exception in non-lenient
     * (piggy-back tests for compareTo() which is new in 1.5)
     */
    public void Test4722650() {
        Calendar cal1 = new GregorianCalendar();
        cal1.clear();
        Calendar cal2 = new GregorianCalendar();
        cal2.clear();
        cal2.setLenient(false);

        cal1.set(2003, OCTOBER, 31);
        cal2.set(2003, OCTOBER, 31);
        try {
            if (cal1.equals(cal2)) {
                errln("lenient and non-lenient shouldn't be equal. (2003/10/31)");
            }
            if (cal1.compareTo(cal2) != 0) {
                errln("cal1 and cal2 should represent the same time. (2003/10/31)");
            }
        } catch (IllegalArgumentException e) {
            errln("equals threw IllegalArugumentException with non-lenient");
        }

        cal1.set(2003, OCTOBER, 32);
        cal2.set(2003, OCTOBER, 32);
        try {
            if (cal1.equals(cal2)) {
                errln("lenient and non-lenient shouldn't be equal. (2003/10/32)");
            }
            if (cal1.compareTo(cal2) != 0) {
                errln("cal1 and cal2 should represent the same time. (2003/10/32)");
            }
        } catch (IllegalArgumentException e) {
            errln("equals threw IllegalArugumentException with non-lenient");
        }

        cal1 = Calendar.getInstance(new Locale("th", "TH"));
        cal1.setTimeInMillis(0L);
        cal2 = Calendar.getInstance(Locale.US);
        cal2.setTimeInMillis(0L);
        if (cal1.equals(cal2)) {
            errln("Buddhist.equals(Gregorian) shouldn't be true. (millis=0)");
        }
        if (cal1.compareTo(cal2) != 0) {
            errln("cal1 (Buddhist) and cal2 (Gregorian) should represent the same time. (millis=0)");
        }
    }

    /**
     * 4738710: API: Calendar comparison methods should be improved
     */
    public void Test4738710() {
        Calendar cal0 = new GregorianCalendar(2003, SEPTEMBER, 30);
        Comparable<Calendar> cal1 = new GregorianCalendar(2003, OCTOBER, 1);
        Calendar cal2 = new GregorianCalendar(2003, OCTOBER, 2);
        if (!(cal1.compareTo(cal0) > 0)) {
            errln("!(cal1 > cal0)");
        }
        if (!(cal1.compareTo(cal2) < 0)) {
            errln("!(cal1 < cal2)");
        }
        if (cal1.compareTo(new GregorianCalendar(2003, OCTOBER, 1)) != 0) {
            errln("cal1 != new GregorianCalendar(2003, OCTOBER, 1)");
        }

        if (cal0.after(cal2)) {
            errln("cal0 shouldn't be after cal2");
        }
        if (cal2.before(cal0)) {
            errln("cal2 shouldn't be before cal0");
        }

        if (cal0.after(0)) {
            errln("cal0.after() returned true with an Integer.");
        }
        if (cal0.before(0)) {
            errln("cal0.before() returned true with an Integer.");
        }
        if (cal0.after(null)) {
            errln("cal0.after() returned true with null.");
        }
        if (cal0.before(null)) {
            errln("cal0.before() returned true with null.");
        }
    }

    /**
     * 4633646: Setting WEEK_OF_MONTH to 1 results in incorrect date
     */
    @SuppressWarnings("deprecation")
    public void Test4633646() {
        Koyomi cal = new Koyomi(Locale.US);
        cal.setTime(new Date(2002 - 1900, 1 - 1, 28));
        sub4633646(cal);

        cal.setLenient(false);
        cal.setTime(new Date(2002 - 1900, 1 - 1, 28));
        sub4633646(cal);

        cal = new Koyomi(Locale.US);
        cal.clear();
        cal.set(2002, JANUARY, 28);
        sub4633646(cal);

        cal.clear();
        cal.setLenient(false);
        cal.set(2002, JANUARY, 28);
        sub4633646(cal);
    }

    void sub4633646(Koyomi cal) {
        cal.getTime();
        cal.set(WEEK_OF_MONTH, 1);
        if (cal.isLenient()) {
            if (!cal.checkDate(2001, DECEMBER, 31)) {
                errln(cal.getMessage());
            }
            if (!cal.checkFieldValue(WEEK_OF_MONTH, 6)) {
                errln(cal.getMessage());
            }
        } else {
            try {
                Date d = cal.getTime();
                errln("didn't throw IllegalArgumentException in non-lenient");
            } catch (IllegalArgumentException e) {
            }
        }
    }

    /**
     * 4846659: Calendar: Both set() and roll() don't work for AM_PM time field
     * (Partially fixed only roll as of 1.5)
     */
    public void Test4846659() {
        Koyomi cal = new Koyomi();
        cal.clear();
        cal.set(2003, OCTOBER, 31, 10, 30, 30);
        cal.getTime();
        // Test roll()
        cal.roll(AM_PM, +1); // should turn to PM
        if (!cal.checkFieldValue(HOUR_OF_DAY, 10 + 12)) {
            errln("roll: AM_PM didn't change to PM");
        }

        cal.clear();
        cal.set(2003, OCTOBER, 31, 10, 30, 30);
        cal.getTime();
        // Test set()
        cal.set(AM_PM, PM); // should turn to PM
        if (!cal.checkFieldValue(HOUR_OF_DAY, 10 + 12)) {
            errln("set: AM_PM didn't change to PM");
        }

        cal.clear();
        cal.set(2003, OCTOBER, 31, 10, 30, 30);
        cal.getTime();
        cal.set(AM_PM, PM);
        cal.set(HOUR, 9);
        if (!cal.checkFieldValue(HOUR_OF_DAY, 9 + 12)) {
            errln("set: both AM_PM and HOUT didn't change to PM");
        }
    }

    /**
     * 4822110: GregorianCalendar.get() returns an incorrect date after setFirstDayOfWeek()
     */
    public void Test4822110() {
        Koyomi cal = new Koyomi(Locale.US);
        //    June 2003
        //  S  M Tu  W Th  F  S
        //  1  2  3  4  5  6  7
        //  8  9 10 11 12 13 14
        // 15 16 17 18 19 20 21
        // 22 23 24 25 26 27 28
        // 29 30
        cal.clear();
        // 6/1 to 6/7 should be the 1st week of June.
        cal.set(2003, JUNE, 2);
        cal.getTime();                  // Let cal calculate time.
        cal.setFirstDayOfWeek(MONDAY);
        // Now 6/2 to 6/8 should be the 2nd week of June. Sunday of
        // that week is 6/8.
        logln("1: " + cal.get(WEEK_OF_MONTH) + ", " + cal.get(DAY_OF_MONTH));
        cal.set(DAY_OF_WEEK, SUNDAY);
        logln("1st Sunday of June 2003 with FirstDayOfWeek=MONDAY");
        if (!cal.checkDate(2003, JUNE, 8)) {
            errln(cal.getMessage());
        }
    }

    /**
     * 4973919: Inconsistent GregorianCalendar hashCode before and after serialization
     */
    public void Test4966499() throws Exception {
        GregorianCalendar date1 = new GregorianCalendar(2004, JANUARY, 7);

        // Serialize date1
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(date1);

        byte[] buffer = baos.toByteArray();

        // Deserialize it
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        ObjectInputStream ois = new ObjectInputStream(bais);
        GregorianCalendar date2 = (GregorianCalendar) ois.readObject();

        if (!date1.equals(date2)) {
            errln("date1.equals(date2) != true");
        }
        if (date1.hashCode() != date2.hashCode()) {
            errln("inconsistent hashCode() value (before=0x"
                    + Integer.toHexString(date1.hashCode())
                    + ", after=0x" + Integer.toHexString(date2.hashCode()) + ")");
        }
    }

    /**
     * 4980088: GregorianCalendar.getActualMaximum doesn't throw exception
     */
    public void Test4980088() {
        GregorianCalendar cal = new GregorianCalendar();
        try {
            int x = cal.getMaximum(100);
            errln("getMaximum(100) didn't throw an exception.");
        } catch (IndexOutOfBoundsException e) {
            logln("getMaximum: " + e.getClass().getName() + ": " + e.getMessage());
        }

        try {
            int x = cal.getLeastMaximum(100);
            errln("getLeastMaximum(100) didn't throw an exception.");
        } catch (IndexOutOfBoundsException e) {
            logln("getLeastMaximum: " + e.getClass().getName() + ": " + e.getMessage());
        }

        try {
            int x = cal.getActualMaximum(100);
            errln("getActualMaximum(100) didn't throw an exception.");
        } catch (IndexOutOfBoundsException e) {
            logln("getActualMaximum: " + e.getClass().getName() + ": " + e.getMessage());
        }

        try {
            int x = cal.getMinimum(100);
            errln("getMinimum(100) didn't throw an exception.");
        } catch (IndexOutOfBoundsException e) {
            logln("getMinimum: " + e.getClass().getName() + ": " + e.getMessage());
        }

        try {
            int x = cal.getGreatestMinimum(100);
            errln("getGreatestMinimum(100) didn't throw an exception.");
        } catch (IndexOutOfBoundsException e) {
            logln("getGreatestMinimum: " + e.getClass().getName() + ": " + e.getMessage());
        }

        try {
            int x = cal.getActualMinimum(100);
            errln("getActualMinimum(100) didn't throw an exception.");
        } catch (IndexOutOfBoundsException e) {
            logln("getActualMinimum: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    /**
     * 4965624: GregorianCalendar.isLeapYear(1000) returns incorrect value
     */
    public void Test4965624() {
        // 5013094: This test case needs to use "GMT" to specify
        // Gregorian cutover dates.
        TimeZone savedZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        try {
            Map<Date, Boolean> data = new HashMap<>();
            data.put(getGregorianDate(999, OCTOBER, 1), Boolean.FALSE);
            data.put(getGregorianDate(1000, JANUARY, 1), Boolean.FALSE);
            data.put(getGregorianDate(1000, FEBRUARY, 1), Boolean.FALSE);
            data.put(getGregorianDate(1000, FEBRUARY, 28), Boolean.FALSE);
            data.put(getGregorianDate(1000, MARCH, 1), Boolean.TRUE);
            data.put(getGregorianDate(1001, JANUARY, 1), Boolean.TRUE);
            data.put(getGregorianDate(1001, JANUARY, 6), Boolean.TRUE);
            data.put(getGregorianDate(1001, MARCH, 1), Boolean.TRUE);

            data.keySet().forEach(d -> {
                boolean expected = data.get(d);
                GregorianCalendar cal = new GregorianCalendar();
                cal.setGregorianChange(d);
                if (cal.isLeapYear(1000) != expected) {
                    errln("isLeapYear(1000) returned " + cal.isLeapYear(1000)
                            + " with cutover date (Julian) " + d);
                }
            });
        } finally {
            TimeZone.setDefault(savedZone);
        }
    }

    // Note that we can't use Date to produce Gregorian calendar dates
    // before the default cutover date.
    static Date getGregorianDate(int year, int month, int dayOfMonth) {
        GregorianCalendar g = new GregorianCalendar();
        // Make g a pure Gregorian calendar
        g.setGregorianChange(new Date(Long.MIN_VALUE));
        g.clear();
        g.set(year, month, dayOfMonth);
        return g.getTime();
    }

    /**
     * 5006864: Define the minimum value of DAY_OF_WEEK_IN_MONTH as 1
     */
    public void Test5006864() {
        GregorianCalendar cal = new GregorianCalendar();
        int min = cal.getMinimum(DAY_OF_WEEK_IN_MONTH);
        if (min != 1) {
            errln("GregorianCalendar.getMinimum(DAY_OF_WEEK_IN_MONTH) returned "
                    + min + ", expected 1.");
        }
        min = cal.getGreatestMinimum(DAY_OF_WEEK_IN_MONTH);
        if (min != 1) {
            errln("GregorianCalendar.getGreatestMinimum(DAY_OF_WEEK_IN_MONTH) returned "
                    + min + ", expected 1.");
        }
    }
}
