/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test 1.1 06/01/24
 * @bug 6335238
 * @summary Make sure that both the original and cloned SimpleDateFormat coexistindependently and don't cut off each other.
 * @run main Bug6335238 10
 */

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

// Usage: java Bug6335238 [duration]
public class Bug6335238 {

    static final long UTC_LONG = 974534460000L;
    static final String TIME_STRING = "2000/11/18 00:01:00";
    static SimpleDateFormat masterSdf;
    static int duration = 180;
    static boolean stopped = false;
    static boolean err = false;

    public static void main(String[] args) {
        if (args.length == 1) {
            duration = Math.max(10, Integer.parseInt(args[0]));
        }
        Locale savedLocale = Locale.getDefault();
        TimeZone savedTimeZone = TimeZone.getDefault();

        TimeZone.setDefault(TimeZone.getTimeZone("US/Pacific"));
        Locale.setDefault(Locale.US);

        masterSdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        try {
            // Once it is used, DecimalFormat becomes not thread-safe.
            Date d = masterSdf.parse(TIME_STRING);

            new Bug6335238();
        } catch (Exception e) {
            System.err.println(e);
            err = true;
        } finally {
            TimeZone.setDefault(savedTimeZone);
            Locale.setDefault(savedLocale);

            if (err) {
                throw new RuntimeException("Failed: Multiple DateFormat instances didn't work correctly.");
            } else {
                System.out.println("Passed.");
            }
        }
    }

    public Bug6335238() {
        stopped = false;

        DateParseThread d1 = new DateParseThread();
        DateFormatThread d2 = new DateFormatThread();
        DateParseThread d3 = new DateParseThread();
        DateFormatThread d4 = new DateFormatThread();

        d1.start();
        d2.start();
        d3.start();
        d4.start();

        try {
            Thread.sleep(duration * 1000);
        }
        catch (Exception e) {
            System.err.println(e);
            err = true;
        }

        stopped = true;
    }

    class DateFormatThread extends Thread {

        public void run() {
            int i = 0;

            while (!stopped) {
                SimpleDateFormat sdf;
                synchronized (masterSdf) {
                    sdf = (SimpleDateFormat)masterSdf.clone();
                }

                i++;
                String s = sdf.format(new Date(UTC_LONG));

                if (!s.equals(TIME_STRING)) {
                    stopped = true;
                    err = true;

                    throw new RuntimeException("Formatting Date Error: counter=" +
                        i + ", Got<" + s + "> != Expected<" + TIME_STRING + ">");
                }
            }
        }
    }

    class DateParseThread extends Thread {

        public void run() {
            int i = 0;

            while (!stopped) {
                SimpleDateFormat sdf;
                synchronized (masterSdf) {
                    sdf = (SimpleDateFormat)masterSdf.clone();
                }

                i++;
                Date date;
                try {
                    date = sdf.parse(TIME_STRING);
                    long t = date.getTime();

                    if (t != UTC_LONG) {
                        stopped = true;
                        err = true;

                        throw new RuntimeException("Parsing Date Error: counter=" +
                            i + " Got:" + t + "<" + sdf.format(date) +
                            "> != " + UTC_LONG);
                    }
                }
                catch (ParseException e) {
                    stopped = true;
                    err = true;

                    throw new RuntimeException(e);
                }
            }
        }
    }
}
