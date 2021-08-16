/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7130335 7130335
 * @summary Make sure that round-trip conversion (format/parse) works
 *          with old timestamps in Europe/Moscow and with multiple time zone letters.
 */
import java.text.*;
import java.util.*;
import static java.util.GregorianCalendar.*;

public class Bug7130335 {
    private static final TimeZone MOSCOW = TimeZone.getTimeZone("Europe/Moscow");
    private static final TimeZone LONDON = TimeZone.getTimeZone("Europe/London");
    private static final TimeZone LA = TimeZone.getTimeZone("America/Los_Angeles");
    private static final TimeZone[] ZONES = {
        MOSCOW, LONDON, LA
    };

    public static void main(String[] args) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z", Locale.US);
        sdf.setTimeZone(MOSCOW);
        Calendar cal = new GregorianCalendar(MOSCOW, Locale.US);
        cal.clear();
        // Try both +03:00 and +02:00
        cal.set(1922, SEPTEMBER, 30);
        test(sdf, cal);
        cal.add(DAY_OF_YEAR, 1);
        test(sdf, cal);
        cal.set(1991, MARCH, 31);
        // in daylight saving time
        test(sdf, cal);
        cal.add(DAY_OF_YEAR, 1);
        test(sdf, cal);
        // Try the current timestamp
        cal.setTimeInMillis(System.currentTimeMillis());
        test(sdf, cal);

        // tests for multiple time zone letters (8000529)
        test8000529("yyyy-MM-dd HH:mm:ss.SSS Z (z)");
        test8000529("yyyy-MM-dd HH:mm:ss.SSS Z (zzzz)");
        test8000529("yyyy-MM-dd HH:mm:ss.SSS z (Z)");
        test8000529("yyyy-MM-dd HH:mm:ss.SSS zzzz (Z)");

    }

    private static void test(SimpleDateFormat sdf, Calendar cal) throws Exception {
        Date d = cal.getTime();
        String f = sdf.format(d);
        System.out.println(f);
        Date pd = sdf.parse(f);
        String p = sdf.format(pd);
        if (!d.equals(pd) || !f.equals(p)) {
            throw new RuntimeException("format: " + f + ", parse: " + p);
        }
    }

    private static void test8000529(String fmt) throws Exception {
        for (TimeZone tz : ZONES) {
            SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.US);
            sdf.setTimeZone(tz);
            Calendar cal = new GregorianCalendar(tz, Locale.US);
            cal.clear();
            cal.set(2012, JUNE, 22);
            test(sdf, cal);
            cal.set(2012, DECEMBER, 22);
            test(sdf, cal);
            cal.setTimeInMillis(System.currentTimeMillis());
            test(sdf, cal);
        }
    }
}
