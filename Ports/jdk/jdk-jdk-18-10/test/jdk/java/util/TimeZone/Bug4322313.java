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

/*
 * @test
 * @bug 4322313
 * @summary Make sure that new implementation of custom time zone
 * support for TimeZone.getTimeZone() works correctly and the
 * getDisplayName() methods are locale independent.
 * @library /java/text/testlib
 */

import java.io.*;
import java.text.*;
import java.util.*;

public class Bug4322313 extends IntlTest {
    private static final int MPM = 60 * 1000;    /* Milliseconds per minute */
    private static final Object[][] VALIDS = {
        /* given ID    rawOffset   normalized ID */
        {"GMT+00:00",  0,          "GMT+00:00"},
        {"GMT+3:04",   184 * MPM,  "GMT+03:04"},
        {"GMT+03:04",  184 * MPM,  "GMT+03:04"},
        {"GMT+13:42",  822 * MPM,  "GMT+13:42"},
        /* ISO-LATIN-1 digits */
        {"GMT+\u0030\u0031:\u0032\u0033", 83 * MPM, "GMT+01:23"},

        {"GMT+0",      0,          "GMT+00:00"},
        {"GMT+3",      180 * MPM,  "GMT+03:00"},
        {"GMT+13",     780 * MPM,  "GMT+13:00"},
        {"GMT+034",    34 * MPM,   "GMT+00:34"},
        {"GMT+1034",   634 * MPM,  "GMT+10:34"},

        {"GMT-00:00",  0,          "GMT-00:00"},
        {"GMT-3:04",   -184 * MPM, "GMT-03:04"},
        {"GMT-03:04",  -184 * MPM, "GMT-03:04"},
        {"GMT-13:42",  -822 * MPM, "GMT-13:42"},
        /* ISO-LATIN-1 digits */
        {"GMT-\u0030\u0031:\u0032\u0033", -83 * MPM, "GMT-01:23"},

        {"GMT-0",      0,          "GMT-00:00"},
        {"GMT-3",      -180 * MPM, "GMT-03:00"},
        {"GMT-13",     -780 * MPM, "GMT-13:00"},
        {"GMT-034",    -34 * MPM,  "GMT-00:34"},
        {"GMT-1034",   -634 * MPM, "GMT-10:34"},
    };

    private static final String[] INVALIDS = {
        "GMT+5:8",    "GMT+11:1",    "GMT+23:60",  "GMT+24:13",
        "GMT+0a:0A",  "GMT +13:42",  "GMT+ 13:42", "GMT+13 :42",
        "GMT+13: 42", "GMT+421:950", "GMT+-13:42", "GMT+!13:42",
        "GMT+a",      "GMT+24",      "GMT+060",    "GMT+3003",
        "GMT+42195",  "GMT+-1",      "GMT+-15",    " GMT",

        "GMT-5:8",    "GMT-11:1",    "GMT-23:60",  "GMT-24:13",
        "GMT-0a:0A",  "GMT -13:42",  "GMT- 13:42", "GMT-13 :42",
        "GMT-13: 42", "GMT-421:950", "GMT-+13:42", "GMT-#13:42",
        "GMT-a",      "GMT-24",      "GMT-060",    "GMT-2403",
        "GMT-42195",  "GMT-+1",      "GMT-+15",    "G M T",
        "GMT+09:00 ",
    };

    void Test4322313() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedTimeZone = TimeZone.getDefault();
        boolean err = false;

        Locale[] locs = Locale.getAvailableLocales();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

            for (int i = 0; i < locs.length; i++) {
                Locale locale = locs[i];
                Locale.setDefault(locale);


                /* Okay case */
                for (int k = 0; k < VALIDS.length; k++) {
                    TimeZone tz = TimeZone.getTimeZone((String)VALIDS[k][0]);
                    int offset;

                    if (!tz.getID().equals(VALIDS[k][2])) {
                        err = true;
                        System.err.println("\tFailed [Locale=" +
                                           locale + ", \"" + VALIDS[k][0] +
                                           "\"] Invalid TimeZone ID, expected:" +
                                           VALIDS[k][2] + ", got:" + tz.getID() + ", " + tz);
                    } else {
                        logln("\tPassed [Locale=" +
                              locale + ", \"" + VALIDS[k][0] +
                              "\"] Valid TimeZone ID, got:" + VALIDS[k][2]);
                    }

                    offset = tz.getRawOffset();
                    if (offset != (int)VALIDS[k][1]) {
                        err = true;
                        System.err.println("\tFailed [Locale=" +
                                           locale + ", \"" + VALIDS[k][0] +
                                           "\"] Invalid RawOffset, expected:" + VALIDS[k][1] +
                                           ", got:" + offset + ", " + tz);
                    } else {
                        logln("\tPassed [Locale=" +
                              locale + ", \"" + VALIDS[k][0] +
                              "\"] Vaild RawOffset, got:" + offset);
                    }

                    offset = tz.getDSTSavings();
                    if (offset != 0) {
                        err = true;
                        System.err.println("\tFailed [Locale=" +
                                           locale + ", \"" + VALIDS[k][0] +
                                           "\"] DSTSavings should be zero, got:" + offset +
                                           ", " + tz);
                    } else {
                        logln("\tPassed [Locale=" +
                              locale + ", \"" + VALIDS[k][0] +
                              "\"] DSTSavings is zero.");
                    }
                }

                /* Error case */
                for (int k=0; k < INVALIDS.length; k++) {
                    TimeZone tz = TimeZone.getTimeZone(INVALIDS[k]);
                    int offset;

                    if (!tz.getID().equals("GMT")) {
                        err = true;
                        System.err.println("\tFailed [Locale=" +
                                           locale + ", \"" + INVALIDS[k] +
                                           "\"] Invalid TimeZone ID, expected:GMT, got:" +
                                           tz.getID() + ", " + tz);
                    } else {
                        logln("\tPassed [Locale=" +
                              locale + ", \"" + INVALIDS[k] +
                              "\"] Valid TimeZone ID, got:" + tz.getID());
                    }

                    offset = tz.getRawOffset();
                    if (offset != 0) {
                        err = true;
                        System.err.println("\tFailed [Locale=" +
                                           locale + ", \"" + INVALIDS[k] +
                                           "\"] RawOffset should be zero, got:" + offset +
                                           ", " + tz);
                    } else {
                        logln("\tPassed [Locale=" +
                              locale + ", \"" + INVALIDS[k] +
                              "\"] RawOffset is zero.");
                    }

                    offset = tz.getDSTSavings();
                    if (offset != 0) {
                        err = true;
                        System.err.println("\tFailed [Locale=" +
                                           locale + ", \"" + INVALIDS[k] +
                                           "\"] DSTSavings should be zero, got:" + offset +
                                           ", " + tz);
                    } else {
                        logln("\tPassed [Locale=" +
                              locale + ", \"" + INVALIDS[k] +
                              "\"] DSTSavings is zero.");
                    }
                }

                // getDisplayName() tests
                {
                    String normalizedID = "GMT-08:00";
                    TimeZone tz = TimeZone.getTimeZone("GMT-8");
                    String s;
                    s = tz.getDisplayName(true, tz.LONG);
                    if (!normalizedID.equals(s)) {
                        err = true;
                        System.err.println("getDisplayName returned unexpected name: " + s +
                                           " in " + locale);
                    }
                    s = tz.getDisplayName(true, tz.SHORT);
                    if (!normalizedID.equals(s)) {
                        err = true;
                        System.err.println("getDisplayName returned unexpected name: " + s +
                                           " in " + locale);
                    }
                    s = tz.getDisplayName(false, tz.LONG);
                    if (!normalizedID.equals(s)) {
                        err = true;
                        System.err.println("getDisplayName returned unexpected name: " + s +
                                           " in " + locale);
                    }
                    s = tz.getDisplayName(false, tz.SHORT);
                    if (!normalizedID.equals(s)) {
                        err = true;
                        System.err.println("getDisplayName returned unexpected name: " + s +
                                           " in " + locale);
                    }
                }
            }
        } finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedTimeZone);
        }
        if (err) {
            errln("TimeZone.getTimeZone() test failed");
        } else {
            logln("TimeZone.getTimeZone() test passed");
        }
    }

    public static void main (String[] args) throws Exception {
        new Bug4322313().run(args);
    }
}
