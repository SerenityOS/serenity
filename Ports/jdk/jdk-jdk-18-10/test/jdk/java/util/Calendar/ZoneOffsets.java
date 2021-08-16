/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6231602
 * @summary Make sure that ZONE_OFFSET and/or DST_OFFSET setting is
 * taken into account for time calculations.
 */

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import static java.util.GregorianCalendar.*;

public class ZoneOffsets {

    // This TimeZone always returns the dstOffset value.
    @SuppressWarnings("serial")
    private static class TestTimeZone extends TimeZone {

        private int gmtOffset;
        private int dstOffset;

        TestTimeZone(int gmtOffset, String id, int dstOffset) {
            this.gmtOffset = gmtOffset;
            setID(id);
            this.dstOffset = dstOffset;
        }

        public int getOffset(int era, int year, int month, int day,
                int dayOfWeek, int milliseconds) {
            return gmtOffset + dstOffset;
        }

        public int getOffset(long date) {
            return gmtOffset + dstOffset;
        }

        public void setRawOffset(int offsetMillis) {
            gmtOffset = offsetMillis;
        }

        public int getRawOffset() {
            return gmtOffset;
        }

        public int getDSTSavings() {
            return dstOffset;
        }

        public boolean useDaylightTime() {
            return dstOffset != 0;
        }

        public boolean inDaylightTime(Date date) {
            return dstOffset != 0;
        }

        public String toString() {
            return "TestTimeZone[" + getID() + ", " + gmtOffset + ", " + dstOffset + "]";
        }
    }

    private static Locale[] locales = {
        Locale.getDefault(),
        new Locale("th", "TH"),
        new Locale("ja", "JP", "JP")};

    private static final int HOUR = 60 * 60 * 1000;

    private static int[][] offsets = {
        {0, 0},
        {0, HOUR},
        {0, 2 * HOUR},
        {-8 * HOUR, 0},
        {-8 * HOUR, HOUR},
        {-8 * HOUR, 2 * HOUR},
        {9 * HOUR, 0},
        {9 * HOUR, HOUR},
        {9 * HOUR, 2 * HOUR}};

    public static void main(String[] args) {
        for (int l = 0; l < locales.length; l++) {
            Locale loc = locales[l];
            for (int i = 0; i < offsets.length; i++) {
                test(loc, offsets[i][0], offsets[i][1]);
            }
        }

        // The test case in the bug report.
        GregorianCalendar cal = new GregorianCalendar();
        cal.setLenient(false);
        cal.setGregorianChange(new Date(Long.MIN_VALUE));
        cal.clear();
        cal.set(ZONE_OFFSET, 0);
        cal.set(DST_OFFSET, 0);
        cal.set(ERA, AD);
        cal.set(2004, FEBRUARY, 3, 0, 0, 0);
        cal.set(MILLISECOND, 0);
        // The following line should not throw an IllegalArgumentException.
        cal.getTime();
    }

    private static void test(Locale loc, int gmtOffset, int dstOffset) {
        TimeZone tz1 = new TestTimeZone(gmtOffset,
                "GMT" + (gmtOffset / HOUR) + "." + (dstOffset / HOUR),
                dstOffset);
        int someDifferentOffset = gmtOffset + 2 * HOUR;
        TimeZone tz2 = new TestTimeZone(someDifferentOffset,
                "GMT" + (someDifferentOffset / HOUR) + "." + (dstOffset / HOUR),
                dstOffset);

        int someDifferentDSTOffset = dstOffset == 2 * HOUR ? HOUR : dstOffset + HOUR;
        TimeZone tz3 = new TestTimeZone(gmtOffset,
                "GMT" + (gmtOffset / HOUR) + "." + (someDifferentDSTOffset / HOUR),
                someDifferentDSTOffset);

        // cal1 is the base line.
        Calendar cal1 = Calendar.getInstance(tz1, loc);
        cal1.clear();
        cal1.set(2005, MARCH, 11);
        long t1 = cal1.getTime().getTime();
        int gmt = cal1.get(ZONE_OFFSET);
        int dst = cal1.get(DST_OFFSET);

        // Test 8 cases with cal2.
        Calendar cal2 = Calendar.getInstance(tz2, loc);
        cal2.clear();
        cal2.set(2005, MARCH, 11);
        // test1: set only ZONE_OFFSET
        cal2.set(ZONE_OFFSET, gmtOffset);
        if (t1 != cal2.getTime().getTime() || dst != cal2.get(DST_OFFSET)) {
            error("Test1", loc, cal2, gmtOffset, dstOffset, t1);
        }

        cal2.setTimeZone(tz3);
        cal2.clear();
        cal2.set(2005, MARCH, 11);
        // test2: set only DST_OFFSET
        cal2.set(DST_OFFSET, dstOffset);
        if (t1 != cal2.getTime().getTime() || gmt != cal2.get(ZONE_OFFSET)) {
            error("Test2", loc, cal2, gmtOffset, dstOffset, t1);
        }

        cal2.setTimeZone(tz2);
        cal2.clear();
        cal2.set(2005, MARCH, 11);
        // test3: set both ZONE_OFFSET and DST_OFFSET
        cal2.set(ZONE_OFFSET, gmtOffset);
        cal2.set(DST_OFFSET, dstOffset);
        if (t1 != cal2.getTime().getTime()) {
            error("Test3", loc, cal2, gmtOffset, dstOffset, t1);
        }

        cal2.setTimeZone(tz3);
        cal2.clear();
        cal2.set(2005, MARCH, 11);
        // test4: set both ZONE_OFFSET and DST_OFFSET
        cal2.set(ZONE_OFFSET, gmtOffset);
        cal2.set(DST_OFFSET, dstOffset);
        if (t1 != cal2.getTime().getTime()) {
            error("Test4", loc, cal2, gmtOffset, dstOffset, t1);
        }

        // Test the same thing in non-lenient
        cal2.setLenient(false);

        cal2.setTimeZone(tz2);
        cal2.clear();
        cal2.set(2005, MARCH, 11);
        adjustJapaneseEra(cal2);
        // test5: set only ZONE_OFFSET in non-lenient
        cal2.set(ZONE_OFFSET, gmtOffset);
        if (t1 != cal2.getTime().getTime() || dst != cal2.get(DST_OFFSET)) {
            error("Test5", loc, cal2, gmtOffset, dstOffset, t1);
        }

        cal2.setTimeZone(tz3);
        cal2.clear();
        cal2.set(2005, MARCH, 11);
        adjustJapaneseEra(cal2);
        // test6: set only DST_OFFSET in non-lenient
        cal2.set(DST_OFFSET, dstOffset);
        if (t1 != cal2.getTime().getTime() || gmt != cal2.get(ZONE_OFFSET)) {
            error("Test6", loc, cal2, gmtOffset, dstOffset, t1);
        }

        cal2.setTimeZone(tz2);
        cal2.clear();
        cal2.set(2005, MARCH, 11);
        adjustJapaneseEra(cal2);
        // test7: set both ZONE_OFFSET and DST_OFFSET in non-lenient
        cal2.set(ZONE_OFFSET, gmtOffset);
        cal2.set(DST_OFFSET, dstOffset);
        if (t1 != cal2.getTime().getTime()) {
            error("Test7", loc, cal2, gmtOffset, dstOffset, t1);
        }

        cal2.setTimeZone(tz3);
        cal2.clear();
        cal2.set(2005, MARCH, 11);
        adjustJapaneseEra(cal2);
        // test8: set both ZONE_OFFSET and DST_OFFSET in non-lenient
        cal2.set(ZONE_OFFSET, gmtOffset);
        cal2.set(DST_OFFSET, dstOffset);
        if (t1 != cal2.getTime().getTime()) {
            error("Test8", loc, cal2, gmtOffset, dstOffset, t1);
        }
    }

    private static void error(String msg, Locale loc, Calendar cal2, int gmtOffset, int dstOffset, long t1) {
        System.err.println(cal2);
        throw new RuntimeException(msg + ": Locale=" + loc
                + ", gmtOffset=" + gmtOffset + ", dstOffset=" + dstOffset
                + ", cal1 time=" + t1 + ", cal2 time=" + cal2.getTime().getTime());
    }

    private static void adjustJapaneseEra(Calendar cal) {
        // In case of Japanese calendar, explicitly set the last era; REIWA so that
        // year 2005 won't throw exception
        if (!cal.isLenient() &&
                cal.getCalendarType().equals("japanese") &&
                System.currentTimeMillis() < 1556668800000L) { // Current time not in REIWA
            cal.set(Calendar.ERA, 5);
            cal.add(Calendar.YEAR, -30); // -30: Subtract year-length of HEISEI era
        }
        return;
    }
}
