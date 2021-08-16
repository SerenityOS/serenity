/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4267450
 * @summary Unit test for week date support
 */

import java.text.*;
import java.util.*;
import static java.util.GregorianCalendar.*;

public class WeekDateTest {
    static SimpleDateFormat ymdFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    static SimpleDateFormat ywdFormat = new SimpleDateFormat("YYYY-'W'ww-u", Locale.US);
    static {
        ymdFormat.setCalendar(newCalendar());
        ywdFormat.setCalendar(newCalendar());
    }

    // Round-trip Data
    static final String[][] roundTripData = {
        { "2005-01-01", "2004-W53-6" },
        { "2005-01-02", "2004-W53-7" },
        { "2005-12-31", "2005-W52-6" },
        { "2007-01-01", "2007-W01-1" },
        { "2007-12-30", "2007-W52-7" },
        { "2007-12-31", "2008-W01-1" },
        { "2008-01-01", "2008-W01-2" },
        { "2008-12-29", "2009-W01-1" },
        { "2008-12-31", "2009-W01-3" },
        { "2009-01-01", "2009-W01-4" },
        { "2009-12-31", "2009-W53-4" },
        { "2010-01-03", "2009-W53-7" },
        { "2009-12-31", "2009-W53-4" },
        { "2010-01-01", "2009-W53-5" },
        { "2010-01-02", "2009-W53-6" },
        { "2010-01-03", "2009-W53-7" },
        { "2008-12-28", "2008-W52-7" },
        { "2008-12-29", "2009-W01-1" },
        { "2008-12-30", "2009-W01-2" },
        { "2008-12-31", "2009-W01-3" },
        { "2009-01-01", "2009-W01-4" },
        { "2009-01-01", "2009-W01-4" },
    };

    // Data for leniency test
    static final String[][] leniencyData = {
        { "2008-12-28", "2009-W01-0" },
        { "2010-01-04", "2009-W53-8" },
        { "2008-12-29", "2008-W53-1" },
    };

    static final String[] invalidData = {
        "2010-W00-1",
        "2010-W55-1",
        "2010-W03-0",
        "2010-W04-8",
        "2010-W04-19"
    };

    public static void main(String[] args) throws Exception {
        formatTest(roundTripData);
        parseTest(roundTripData);
        parseTest(leniencyData);
        nonLenientTest(invalidData);
        noWeekDateSupport();
    }

    private static void formatTest(String[][] data) throws Exception {
        for (String[] dates : data) {
            String regularDate = dates[0];
            String weekDate = dates[1];
            Date date = null;
            date = ymdFormat.parse(regularDate);
            String s = ywdFormat.format(date);
            if (!s.equals(weekDate)) {
                throw new RuntimeException("format: got="+s+", expecetd="+weekDate);
            }
        }
    }

    private static void parseTest(String[][] data) throws Exception {
        for (String[] dates : data) {
            String regularDate = dates[0];
            String weekDate = dates[1];
            Date date1 = null, date2 = null;
            date1 = ymdFormat.parse(regularDate);
            date2 = ywdFormat.parse(weekDate);
            if (!date1.equals(date2)) {
                System.err.println(regularDate + ": date1 = " + date1);
                System.err.println(weekDate + ": date2 = " + date2);
                throw new RuntimeException("parse: date1 != date2");
            }
        }
    }


    // Non-lenient mode test
    private static void nonLenientTest(String[] data) {
        ywdFormat.setLenient(false);
        for (String date : data) {
            try {
                Date d = ywdFormat.parse(date);
                throw new RuntimeException("No ParseException thrown with " + date);
            } catch (ParseException e) {
                // OK
            }
        }
        ywdFormat.setLenient(true);
    }


    private static void noWeekDateSupport() throws Exception {
        // Tests with Japanese Imperial Calendar that doesn't support week dates.
        Calendar jcal = Calendar.getInstance(TimeZone.getTimeZone("GMT"),
                                             new Locale("ja", "JP", "JP"));

        String format = "2-W01-2"; // 2019-12-31 == R1-12-31
        int expectedYear = 2019;
        // Check the current era, Heisei or Reiwa
        if (System.currentTimeMillis() < 1556668800000L) {
            format = "21-W01-3"; // 2008-12-31 == H20-12-31
            expectedYear = 2008;
        }
        jcal.setFirstDayOfWeek(MONDAY);
        jcal.setMinimalDaysInFirstWeek(4);
        SimpleDateFormat sdf = new SimpleDateFormat("Y-'W'ww-u");
        sdf.setCalendar(jcal);
        Date d = sdf.parse(format);
        GregorianCalendar gcal = newCalendar();
        gcal.setTime(d);
        if (gcal.get(YEAR) != expectedYear
            || gcal.get(MONTH) != DECEMBER
            || gcal.get(DAY_OF_MONTH) != 31) {
            String s = String.format("noWeekDateSupport: got %04d-%02d-%02d, expected %4d-12-31%n",
                                     gcal.get(YEAR),
                                     gcal.get(MONTH)+1,
                                     gcal.get(DAY_OF_MONTH),
                                     expectedYear);
            throw new RuntimeException(s);
        }
    }

    private static GregorianCalendar newCalendar() {
        // Use GMT to avoid any surprises related DST transitions.
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        // Setup the ISO 8601 compatible parameters
        cal.setFirstDayOfWeek(MONDAY);
        cal.setMinimalDaysInFirstWeek(4);
        return cal;
    }
}
