/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6683975 8008577
 * @summary Make sure that date is formatted correctlyin th locale.
 * @modules jdk.localedata
 * @run main/othervm -Djava.locale.providers=JRE,SPI Bug6683975
 */
import java.text.*;
import java.util.*;

public class Bug6683975 {

    private static boolean err = false;

    private static Locale th = new Locale("th", "");
    private static Locale th_TH = new Locale("th", "TH");
    private static String expected_th[] = {
        "\u0e27\u0e31\u0e19\u0e2d\u0e31\u0e07\u0e04\u0e32\u0e23\u0e17\u0e35\u0e48 30 \u0e01\u0e31\u0e19\u0e22\u0e32\u0e22\u0e19 \u0e04.\u0e28. 2008, 8 \u0e19\u0e32\u0e2c\u0e34\u0e01\u0e32 0 \u0e19\u0e32\u0e17\u0e35 00 \u0e27\u0e34\u0e19\u0e32\u0e17\u0e35",  // 0: FULL
        "30 \u0e01\u0e31\u0e19\u0e22\u0e32\u0e22\u0e19 2008, 8 \u0e19\u0e32\u0e2c\u0e34\u0e01\u0e32 0 \u0e19\u0e32\u0e17\u0e35",  // 1: LONG
        "30 \u0e01.\u0e22. 2008, 8:00:00",  // 2: MEDIUM
        "30/9/2008, 8:00 \u0e19.",  // 3: SHORT
    };
    private static String expected_th_TH[] = {
        "\u0e27\u0e31\u0e19\u0e2d\u0e31\u0e07\u0e04\u0e32\u0e23\u0e17\u0e35\u0e48 30 \u0e01\u0e31\u0e19\u0e22\u0e32\u0e22\u0e19 \u0e1e.\u0e28. 2551, 8 \u0e19\u0e32\u0e2c\u0e34\u0e01\u0e32 0 \u0e19\u0e32\u0e17\u0e35 00 \u0e27\u0e34\u0e19\u0e32\u0e17\u0e35",  // 0: FULL
        "30 \u0e01\u0e31\u0e19\u0e22\u0e32\u0e22\u0e19 2551, 8 \u0e19\u0e32\u0e2c\u0e34\u0e01\u0e32 0 \u0e19\u0e32\u0e17\u0e35",  // 1: LONG
        "30 \u0e01.\u0e22. 2551, 8:00:00",  // 2: MEDIUM
        "30/9/2551, 8:00 \u0e19."  // 3: SHORT
    };
    private static String stylePattern[] =  {
        "FULL", "LONG", "MEDIUM", "SHORT"
    };

    private static void test(int style) {
        DateFormat df_th = DateFormat.getDateTimeInstance(style, style, th);
        DateFormat df_th_TH = DateFormat.getDateTimeInstance(style, style, th_TH);

        String str_th = ((SimpleDateFormat)df_th).toPattern();
        String str_th_TH = ((SimpleDateFormat)df_th_TH).toPattern();
        if (!str_th.equals(str_th_TH)) {
            err = true;
            System.err.println("Error: Pattern for th locale should be the same as pattern for th_TH locale. (" + stylePattern[style] + ")");
            System.err.println("\tth: " + str_th);
            System.err.println("\tth_TH: " + str_th_TH);
        }

        @SuppressWarnings("deprecation")
        Date date = new Date(2008-1900, Calendar.SEPTEMBER, 30, 8, 0, 0);
        str_th = df_th.format(date);
        if (!expected_th[style].equals(str_th)) {
            err = true;
            System.err.println("Error: Formatted date in th locale is incorrect in " +  stylePattern[style] + " pattern.");
            System.err.println("\tExpected: " + expected_th[style]);
            System.err.println("\tGot: " + str_th);
        }

        str_th_TH = df_th_TH.format(date);
        if (!expected_th_TH[style].equals(str_th_TH)) {
            err = true;
            System.err.println("Error: Formatted date in th_TH locale is incorrect in " +  stylePattern[style] + " pattern.");
            System.err.println("\tExpected: " + expected_th_TH[style]);
            System.err.println("\tGot: " + str_th_TH);
        }
    }

    public static void main(String[] args) {
        TimeZone timezone = TimeZone.getDefault();
        Locale locale = Locale.getDefault();

        TimeZone.setDefault(TimeZone.getTimeZone("US/Pacific"));
        Locale.setDefault(Locale.US);

        try {
            test(DateFormat.FULL);
            test(DateFormat.LONG);
            test(DateFormat.MEDIUM);
            test(DateFormat.SHORT);
        }
        catch (Exception e) {
            err = true;
            System.err.println("Unexpected exception was thrown: " + e);
        }
        finally {
            TimeZone.setDefault(timezone);
            Locale.setDefault(locale);

            if (err) {
                throw new RuntimeException("Failed.");
            } else {
                System.out.println("Passed.");
            }
        }
    }

}
