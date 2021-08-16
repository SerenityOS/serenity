/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6530336 6537997 8008577
 * @library /java/text/testlib
 * @run main/othervm -Djava.locale.providers=COMPAT,SPI Bug6530336
 */

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Bug6530336 {

    public static void main(String[] args) throws Exception {
        Locale defaultLocale = Locale.getDefault();
        TimeZone defaultTimeZone = TimeZone.getDefault();

        boolean err = false;

        try {
            Locale locales[] = Locale.getAvailableLocales();
            TimeZone timezone_LA = TimeZone.getTimeZone("America/Los_Angeles");
            TimeZone.setDefault(timezone_LA);

            TimeZone timezones[] = {
                TimeZone.getTimeZone("America/New_York"),
                TimeZone.getTimeZone("America/Denver"),
            };

            String[] expected = {
                "Sun Jul 15 12:00:00 PDT 2007",
                "Sun Jul 15 14:00:00 PDT 2007",
            };

            Date[] dates = new Date[2];

            for (int i = 0; i < locales.length; i++) {
                Locale locale = locales[i];
                if (!TestUtils.usesGregorianCalendar(locale)) {
                    continue;
                }

                Locale.setDefault(locale);

                for (int j = 0; j < timezones.length; j++) {
                    Calendar cal = Calendar.getInstance(timezones[j]);
                    cal.set(2007, 6, 15, 15, 0, 0);
                    dates[j] = cal.getTime();
                }

                SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");

                for (int j = 0; j < timezones.length; j++) {
                    sdf.setTimeZone(timezones[j]);
                    String date = sdf.format(dates[j]);
                    sdf.setTimeZone(timezone_LA);
                    String date_LA = sdf.parse(date).toString();

                    if (!expected[j].equals(date_LA)) {
                        System.err.println("Got wrong Pacific time (" +
                            date_LA + ") for (" + date + ") in " + locale +
                            " in " + timezones[j] +
                            ".\nExpected=" + expected[j]);
                        err = true;
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            err = true;
        }
        finally {
            Locale.setDefault(defaultLocale);
            TimeZone.setDefault(defaultTimeZone);

            if (err) {
                throw new RuntimeException("Failed.");
            }
        }
    }

}
