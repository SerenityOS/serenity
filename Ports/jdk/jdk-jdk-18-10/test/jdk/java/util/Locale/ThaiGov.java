/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4474409
 * @author John O'Conner
 * @modules jdk.localedata
 * @run main/othervm -Djava.locale.providers=COMPAT ThaiGov
 */

import java.util.*;
import java.text.*;

public class ThaiGov {

    ThaiGov() {
        System.out.println("ThaiGov locale test...");

    }

    void numberTest() throws RuntimeException {
        final String strExpected = "\u0E51\u0E52\u002C\u0E53\u0E54\u0E55\u002C\u0E56\u0E57\u0E58\u002E\u0E52\u0E53\u0E54";
        final double value =  12345678.234;

        Locale locTH = new Locale("th", "TH", "TH");

        // th_TH_TH test
        NumberFormat nf = NumberFormat.getInstance(locTH);
        String str = nf.format(value);

        if (!strExpected.equals(str)) {
            throw new RuntimeException();
        }

    }

    void currencyTest() throws RuntimeException {
        final String strExpected = "\u0E3F\u0E51\u0E52\u002C\u0E53\u0E54\u0E55\u002C\u0E56\u0E57\u0E58\u002E\u0E52\u0E53";
        final double value =  12345678.234;

        Locale locTH = new Locale("th", "TH", "TH");

        // th_TH_TH test
        NumberFormat nf = NumberFormat.getCurrencyInstance(locTH);
        String str = nf.format(value);

        if (!strExpected.equals(str)) {
            throw new RuntimeException();
        }

    }

    void dateTest() throws RuntimeException {
        Locale locTH = new Locale("th", "TH", "TH");
        TimeZone tz = TimeZone.getTimeZone("PST");

        Calendar calGregorian = Calendar.getInstance(tz, Locale.US);
        calGregorian.clear();
        calGregorian.set(2002, 4, 1, 8, 30);
        final Date date = calGregorian.getTime();
        Calendar cal = Calendar.getInstance(tz, locTH);
        cal.clear();
        cal.setTime(date);


        final String strExpected = "\u0E27\u0E31\u0E19\u0E1E\u0E38\u0E18\u0E17\u0E35\u0E48\u0020\u0E51\u0020\u0E1E\u0E24\u0E29\u0E20\u0E32\u0E04\u0E21\u0020\u0E1E\u002E\u0E28\u002E\u0020\u0E52\u0E55\u0E54\u0E55\u002C\u0020\u0E58\u0020\u0E19\u0E32\u0E2C\u0E34\u0E01\u0E32\u0020\u0E53\u0E50\u0020\u0E19\u0E32\u0E17\u0E35\u0020\u0E50\u0E50\u0020\u0E27\u0E34\u0E19\u0E32\u0E17\u0E35";
        Date value =  cal.getTime();

        // th_TH_TH test
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, locTH);
        df.setTimeZone(tz);
        String str = df.format(value);

        if (!strExpected.equals(str)) {
            throw new RuntimeException();
        }

    }

    public static void main(String[] args) {

        ThaiGov app = new ThaiGov();
        System.out.print("Running numberTest...");
        app.numberTest();
        System.out.print("Finished\n");
        System.out.print("Running currencyTest...");
        app.currencyTest();
        System.out.print("Finished\n");
        System.out.print("Running dateTest...");
        app.dateTest();
        System.out.print("Finished\n");

        System.out.println("PASSED");
    }


}
