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
 * @bug 4023247 4027685 4032037 4072029 4073003 4118010 4120606 4133833 4136916 6274757 6314387
 * @library /java/text/testlib
 */

import java.util.*;

@SuppressWarnings("deprecation")
public class DateRegression extends IntlTest {

    public static void main(String[] args) throws Exception {
        new DateRegression().run(args);
    }

    /**
     * @bug 4023247
     */
    public void Test4023247() {
        Date d1 = new Date(0);
        Date d2 = new Date(0);

        d1.setYear(96);
        d1.setMonth(11);
        d1.setDate(22);
        d1.setHours(0);
        d1.setMinutes(0);
        d1.setSeconds(0);

        d2.setYear(96);
        d2.setMonth(11);
        d2.setDate(22);
        d2.setHours(0);
        d2.setMinutes(0);
        d2.setSeconds(0);

        if (d1.hashCode() != d2.hashCode())
            errln("Fail: Date hashCode misbehaves");
    }

    /**
     * @bug 4027685
     */
    public void Test4027685() {
        // Should be 01/16/97 00:00:00
        Date nite = new Date("16-JAN-97 12:00 AM");
        // Should be 01/16/97 12:00:00
        Date noon = new Date("16-JAN-97 12:00 PM");

        logln("Midnight = " + nite + ", Noon = " + noon);
        if (!nite.equals(new Date(97, Calendar.JANUARY, 16, 0, 0)) ||
            !noon.equals(new Date(97, Calendar.JANUARY, 16, 12, 0)))
            errln("Fail: Nite/Noon confused");
    }

    /**
     * @bug 4032037
     */
    public void Test4032037() {
        Date ref = new Date(97, 1, 10);
        Date d = new Date(Date.parse("2/10/97"));
        logln("Date.parse(2/10/97) => " + d);
        if (!d.equals(ref)) errln("Fail: Want " + ref + " Got " + d);
        d = new Date(Date.parse("10 feb 1997"));
        logln("Date.parse(10 feb 1997) => " + d);
        if (!d.equals(ref)) errln("Fail: Want " + ref + " Got " + d);
        d = new Date("2/10/97");
        logln("Date(2/10/97) => " + d);
        if (!d.equals(ref)) errln("Fail: Want " + ref + " Got " + d);
        d = new Date("10 feb 1997");
        logln("Date(10 feb 1997) => " + d);
        if (!d.equals(ref)) errln("Fail: Want " + ref + " Got " + d);
    }

    /**
     * @bug 4072029
     */
    public void Test4072029() {
        TimeZone saveZone = TimeZone.getDefault();

        try {
            TimeZone.setDefault(TimeZone.getTimeZone("PST"));
            Date now = new Date();
            String s = now.toString();
            Date now2 = new Date(now.toString());
            String s2 = now2.toString(); // An hour's difference

            if (!s.equals(s2) ||
                Math.abs(now.getTime() - now2.getTime()) > 60000 /*one min*/) {
                errln("Fail: Roundtrip toString/parse");
            }
        }
        finally {
            TimeZone.setDefault(saveZone);
        }
    }

    /**
     * @bug 4073003
     */
    public void Test4073003() {
        Date d = new Date(Date.parse("01/02/1984"));
        if (!d.equals(new Date(84, 0, 2)))
            errln("Fail: Want 1/2/1984 Got " + d);
        d = new Date(Date.parse("02/03/2012"));
        if (!d.equals(new Date(112, 1, 3)))
            errln("Fail: Want 2/3/2012 Got " + d);
        d = new Date(Date.parse("03/04/15"));
        if (!d.equals(new Date(115, 2, 4)))
            errln("Fail: Want 3/4/2015 Got " + d);
    }

    /**
     * @bug 4118010
     * Regress bug:
     * Feb. 2000 has 29 days, but Date(2000, 1, 29) returns March 01, 2000
     * NOTE: This turned out to be a user error (passing in 2000 instead
     * of 2000-1900 to the Date constructor).
     */
    public void Test4118010() {
        Date d=new java.util.Date(2000-1900, Calendar.FEBRUARY, 29);
        int m=d.getMonth();
        int date=d.getDate();
        if (m != Calendar.FEBRUARY ||
            date != 29)
            errln("Fail: Want Feb 29, got " + d);
    }

    /**
     * @bug 4120606
     * Date objects share state after cloning.
     */
    public void Test4120606() {
        Date d = new Date(98, Calendar.JUNE, 24);
        d.setMonth(Calendar.MAY);
        Date e = (Date)d.clone();
        d.setMonth(Calendar.FEBRUARY);
        if (e.getMonth() != Calendar.MAY) {
            errln("Cloned Date objects share state");
        }
    }

    /**
     * @bug 4133833
     * Date constructor crashes with parameters out of range, when it should
     * normalize.
     */
    public void Test4133833() {
        Date date = new java.util.Date(12,15,19);
        Date exp  = new Date(1913-1900, Calendar.APRIL, 19);
        if (!date.equals(exp))
            errln("Fail: Want " + exp +
                                "; got " + date);
    }

    /**
     * @bug 4136916
     * Date.toString() throws exception in 1.2b4-E
     * CANNOT REPRODUCE this bug
     */
    public void Test4136916() {
        Date time = new Date();
        logln(time.toString());
    }

    /**
     * @bug 6274757
     * Date getTime and toString interaction for some time values
     */
    public void Test6274757() {
        TimeZone savedTz = TimeZone.getDefault();
        try {
            // Use a time zone west of GMT.
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
            TimeZone jdkGMT = TimeZone.getTimeZone("GMT");
            Calendar jdkCal = Calendar.getInstance(jdkGMT);
            jdkCal.clear();
            jdkCal.set(1582, Calendar.OCTOBER, 15);
            logln("JDK time: " + jdkCal.getTime().getTime() );
            logln("JDK time (str): " + jdkCal.getTime() );
            logln("Day of month: " + jdkCal.get(Calendar.DAY_OF_MONTH));
            Date co = jdkCal.getTime();
            logln("Change over (Oct 15 1582) = " + co + " (" +
                  co.getTime() + ")");
            long a = jdkCal.getTime().getTime();
            Date c = jdkCal.getTime();
            c.toString();
            long b = c.getTime();

            if (a != b) {
                errln("ERROR: " + a + " != " + b);
            } else {
                logln(a + " = " + b);
            }
        } finally {
            TimeZone.setDefault(savedTz);
        }
    }

    /**
     * @bug 6314387
     * JCK6.0: api/java_util/Date/index.html#misc fails, mustang
     */
    public void Test6314387() {
        Date d = new Date(Long.MAX_VALUE);
        int y = d.getYear();
        if (y != 292277094) {
            errln("yesr: got " + y + ", expected 292277094");
        }
        d = new Date(Long.MIN_VALUE);
        y = d.getYear();
        if (y != 292267155) {
            errln("yesr: got " + y + ", expected 292267155");
        }
    }
}

//eof
