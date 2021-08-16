/*
 * Copyright (c) 2001, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4407042
 * @summary Make sure that cloned SimpleDateFormat objects work
 * independently in multiple threads.
 * @library /java/text/testlib
 * @run main Bug4407042 10
 */

import java.io.*;
import java.text.*;
import java.util.*;

// Usage: java Bug4407042 [duration]
public class Bug4407042 {

    static final String TIME_STRING = "2000/11/18 00:01:00";
    static final long UTC_LONG = 974534460000L;
    static SimpleDateFormat masterFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    static boolean runrun = true;
    static int duration = 100;

    void test() {
        Locale locale = Locale.getDefault();
        if (!TestUtils.usesAsciiDigits(locale)
                || !TestUtils.usesGregorianCalendar(locale)) {
            System.out.println("Skipping this test because locale is " + locale);
            return;
        }

        masterFormat.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        DateParseThread d1 = new DateParseThread();
        DateFormatThread d2 = new DateFormatThread();
        d1.start();
        d2.start();
        int n = Thread.activeCount();
        boolean failed = false;

        for (int i = 0; i < duration; i++) {
            try {
                Thread.sleep(1000);
                if (Thread.activeCount() != n) {
                    failed = true;
                    break;
                }
            } catch (InterruptedException e) {
            }
        }
        runrun = false;
        try {
            d1.join();
            d2.join();
        } catch (InterruptedException e) {
        }
        if (failed) {
            throw new RuntimeException("Failed");
        }
    }

    synchronized static SimpleDateFormat getFormatter() {
        return (SimpleDateFormat) masterFormat.clone();
    }

    static class DateParseThread extends Thread {
        public void run() {
            SimpleDateFormat sdf = getFormatter();
            Calendar cal = null;

            try {
                int i = 0;
                while (runrun) {
                    Date date =sdf.parse(TIME_STRING);
                    long t = date.getTime();
                    i++;
                    if (t != UTC_LONG) {
                        throw new RuntimeException("Parse Error: " + i +
                                                   " (" + sdf.format(date) + ") " + t +
                                                   " != " + UTC_LONG);
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
                throw new RuntimeException("Parse Error");
            }
        }
    }

    static class DateFormatThread extends Thread {
        public  void run () {
            SimpleDateFormat sdf = getFormatter();
            Calendar cal = null;

            int i = 0;
            while (runrun) {
                i++;
                String s = sdf.format(new Date(UTC_LONG));
                if (!s.equals(TIME_STRING)) {
                    throw new RuntimeException("Format Error: " + i + " " +
                                               s + " != " + TIME_STRING);
                }
            }
        }
    }

    public static void main (String[] args) {
        if (args.length == 1) {
            duration = Math.max(10, Integer.parseInt(args[0]));
        }
        new Bug4407042().test();
    }
}
