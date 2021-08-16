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
 * @bug 4278609 4761696
 * @library /java/text/testlib
 * @summary Make sure to handle DST transition ending at 0:00 January 1.
 */

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

public class TransitionTest extends IntlTest {

    public static void main(String[] args) throws Exception {
        new TransitionTest().run(args);
    }

    public void Test4278609() {
        SimpleTimeZone tz = new SimpleTimeZone(0, "MyTimeZone",
                               /* DST start day: August, 1, 0:00 */
                               Calendar.AUGUST, 1, 0, 0,
                               /* DST end day: January, 1, 0:00 (wall-clock)*/
                               Calendar.JANUARY, 1, 0, 0,
                               60 * 60 * 1000);

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));

        // setting a date using GMT zone just after the end rule of tz zone
        cal.clear();
        cal.set(Calendar.ERA, GregorianCalendar.AD);
        cal.set(1998, Calendar.DECEMBER, 31, 23, 01, 00);

        Date date = cal.getTime();

        int millis = cal.get(Calendar.HOUR_OF_DAY) * 3600000
                     + cal.get(Calendar.MINUTE) * 60000
                     + cal.get(Calendar.SECOND) * 1000
                     + cal.get(Calendar.MILLISECOND);
        /* we must use standard local time */
        millis += tz.getRawOffset();

        int offset = tz.getOffset(cal.get(Calendar.ERA),
                                  cal.get(Calendar.YEAR),
                                  cal.get(Calendar.MONTH),
                                  cal.get(Calendar.DATE),
                                  cal.get(Calendar.DAY_OF_WEEK),
                                  millis);

        if (offset != 0) {
            SimpleDateFormat format = new SimpleDateFormat("dd MMM HH:mm:ss zzz",
                                                           Locale.US);
            format.setTimeZone(tz);
            errln("Wrong DST transition: " + tz
                  + "\na date just after DST = " + format.format(date)
                  + "\ngetOffset = " + offset);
        }
    }

    /*
     * 4761696: Rewrite SimpleTimeZone to support correct DST transitions
     *
     * Derived from JCK test cases some of which specify wrong day of week values.
     */
    public void Test4761696() {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));

        // test#1
        int rawOffset = -43200000;
        int saving = 1800000;
        int timeOfDay = 84600001;
        SimpleTimeZone tz = new SimpleTimeZone(rawOffset, "stz",
                                Calendar.JULY, 1, 0, 0,
                                Calendar.JANUARY, 1, 0, 0,
                                saving);
        int year = Integer.MIN_VALUE;
        tz.setStartYear(year);
        int offset = tz.getOffset(GregorianCalendar.AD,
                              year,
                              Calendar.DECEMBER,
                              31,
                              1, // should be SATURDAY
                              timeOfDay);
        int y = (int) mod((long)year, 28L); // 28-year cycle
        cal.clear();
        cal.set(cal.ERA, cal.AD);
        cal.set(y, Calendar.DECEMBER, 31);
        cal.set(cal.MILLISECOND, timeOfDay);
        long localtime = cal.getTimeInMillis() + rawOffset; // local standard time

        cal.clear();
        cal.set(cal.ERA, cal.AD);
        cal.set(y + 1, Calendar.JANUARY, 1);
        cal.set(cal.MILLISECOND, -saving);
        long endTime = cal.getTimeInMillis() + rawOffset;
        long expectedOffset = (localtime < endTime) ? rawOffset + saving : rawOffset;
        if (offset != expectedOffset) {
            errln("test#1: wrong offset: got "+offset+", expected="+expectedOffset);
        }

        // test#2
        saving = 1;
        timeOfDay = 0;
        tz = new SimpleTimeZone(rawOffset, "stz",
                                Calendar.JULY, 1, 0, 0,
                                Calendar.JANUARY, 1, 0, 0,
                                saving);
        tz.setStartYear(year);
        offset = tz.getOffset(GregorianCalendar.AD,
                              year,
                              Calendar.AUGUST,
                              15,
                              1, // should be MONDAY
                              timeOfDay);
        y = (int) mod((long)year, 28L); // 28-year cycle
        cal.clear();
        cal.set(y, Calendar.AUGUST, 15);
        cal.set(cal.MILLISECOND, timeOfDay);
        localtime = cal.getTimeInMillis() + rawOffset; // local standard time

        cal.clear();
        cal.set(y + 1, Calendar.JANUARY, 1);
        cal.set(cal.MILLISECOND, -saving);
        endTime = cal.getTimeInMillis() + rawOffset;
        expectedOffset = (localtime < endTime) ? rawOffset + saving : rawOffset;
        if (offset != expectedOffset) {
            errln("Wrong offset: got "+offset+", expected="+expectedOffset);
        }

        rawOffset = 43200000;
        saving = 1;
        timeOfDay = 3599998;
        tz = new SimpleTimeZone(rawOffset, "stz",
                                Calendar.JULY, 1, 0, 3600000,
                                Calendar.JANUARY, 1, 0, 3600000,
                                saving);
        tz.setStartYear(year);
        offset = tz.getOffset(GregorianCalendar.AD,
                              year,
                              Calendar.JANUARY,
                              1,
                              1,
                              timeOfDay);
        y = (int) mod((long)year, 28L); // 28-year cycle
        cal.clear();
        cal.set(y, Calendar.JANUARY, 1);
        cal.set(cal.MILLISECOND, timeOfDay);
        localtime = cal.getTimeInMillis() + rawOffset; // local standard time

        cal.clear();
        cal.set(y + 1, Calendar.JANUARY, 1);
        cal.set(cal.MILLISECOND, 3600000-saving);
        endTime = cal.getTimeInMillis() + rawOffset;
        expectedOffset = (localtime < endTime) ? rawOffset + saving : rawOffset;
        if (offset != expectedOffset) {
            errln("test#2: wrong offset: got "+offset+", expected="+expectedOffset);
        }

        // test#3
        rawOffset = -43200000;
        saving = 1800000;
        timeOfDay = 84600001;
        tz = new SimpleTimeZone(rawOffset, "stz",
                                Calendar.SEPTEMBER, 1, 0, 0,
                                Calendar.MARCH, 1, 0, 0,
                                saving);
        tz.setStartYear(year);
        offset = tz.getOffset(GregorianCalendar.AD,
                              year,
                              Calendar.FEBRUARY,
                              28,
                              1,
                              timeOfDay);
        y = (int) mod((long)year, 28L); // 28-year cycle
        cal.clear();
        cal.set(y, Calendar.FEBRUARY, 28);
        cal.set(cal.MILLISECOND, timeOfDay);
        localtime = cal.getTimeInMillis() + rawOffset; // local standard time

        cal.clear();
        cal.set(y, Calendar.MARCH, 1);
        cal.set(cal.MILLISECOND, -saving);
        endTime = cal.getTimeInMillis() + rawOffset;
        expectedOffset = (localtime < endTime) ? rawOffset + saving : rawOffset;
        if (offset != expectedOffset) {
            errln("test#3: wrong offset: got "+offset+", expected="+expectedOffset);
        }

        // test#4
        rawOffset = -43200000;
        saving = 1;
        timeOfDay = 0;
        tz = new SimpleTimeZone(rawOffset, "stz",
                                Calendar.JANUARY, -4, 1, 3600000,
                                Calendar.JULY, -4, 1, 3600000,
                                saving);
        tz.setStartYear(year);
        offset = tz.getOffset(GregorianCalendar.AD,
                              year,
                              Calendar.JANUARY,
                              10,
                              2, // should be 1 (SUNDAY)
                              timeOfDay);
        y = (int) mod((long)year, 28L); // 28-year cycle
        cal.clear();
        cal.set(y, Calendar.JANUARY, 10);
        cal.set(cal.MILLISECOND, timeOfDay);
        localtime = cal.getTimeInMillis() + rawOffset; // local standard time

        cal.clear();
        cal.set(cal.YEAR, y);
        cal.set(cal.MONTH, Calendar.JANUARY);
        cal.set(cal.DAY_OF_MONTH, 8);
        cal.set(cal.WEEK_OF_MONTH, cal.getActualMaximum(cal.WEEK_OF_MONTH)-4+1);
        cal.set(cal.DAY_OF_WEEK, 1);
        cal.set(cal.MILLISECOND, 3600000-saving);
        long startTime = cal.getTimeInMillis() + rawOffset;
        expectedOffset = (localtime >= startTime) ? rawOffset + saving : rawOffset;
        if (offset != expectedOffset) {
            errln("test#4: wrong offset: got "+offset+", expected="+expectedOffset);
        }

        // test#5
        rawOffset = 0;
        saving = 3600000;
        timeOfDay = 7200000;
        year = 1982;
        tz = new SimpleTimeZone(rawOffset, "stz",
                                Calendar.APRIL, 1, 0, 7200000,
                                Calendar.OCTOBER, 10, 0, 7200000,
                                saving);
        offset = tz.getOffset(GregorianCalendar.AD,
                              year,
                              Calendar.OCTOBER,
                              10,
                              1,
                              timeOfDay);
        cal.clear();
        cal.set(year, Calendar.OCTOBER, 10);
        cal.set(cal.MILLISECOND, timeOfDay);
        localtime = cal.getTimeInMillis() + rawOffset; // local standard time

        cal.clear();
        cal.set(year, Calendar.OCTOBER, 10);
        cal.set(cal.MILLISECOND, 7200000-saving);
        endTime = cal.getTimeInMillis() + rawOffset;
        expectedOffset = (localtime < endTime) ? rawOffset + saving : rawOffset;
        if (offset != expectedOffset) {
            errln("test#5: wrong offset: got "+offset+", expected="+expectedOffset);
        }
    }

    public static final long floorDivide(long n, long d) {
        return ((n >= 0) ?
                (n / d) : (((n + 1L) / d) - 1L));
    }

    public static final long mod(long x, long y) {
        return (x - y * floorDivide(x, y));
    }
}
