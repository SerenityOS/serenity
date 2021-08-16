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
 * @bug 4322313 4833268 6302990 6304305
 * @library /java/text/testlib
 * @summary Make sure that new implementation for
 * SimpleDateFormat.parse('z' or 'Z') and format('z' or 'Z') work correctly.
 */

import java.io.*;
import java.text.*;
import java.util.*;

public class Bug4322313 extends IntlTest {

    public void Test4322313() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedTimeZone = TimeZone.getDefault();
        boolean err = false;
        long mpm = 60 * 1000;   /* Milliseconds per a minute */

        Locale[] locs = {Locale.US, Locale.JAPAN, Locale.UK, new Locale("ar")};

        String[] formats = {
            "z",
            "Z",
        };

        Object[][] valids = {
          /* given ID      offset                format('z'), ('Z')    index */
            {"GMT+03:04",  -184L * mpm, "GMT+03:04", "+0304", 9},
            {"GMT+13:42",  -822L * mpm, "GMT+13:42", "+1342", 9},
            {"GMT+00:00",   0L,         "GMT+00:00", "+0000", 9},
            {"GMT+1:11",   -71L * mpm,  "GMT+01:11", "+0111", 8},
            {"GMT +13:42",  0L,         "GMT",       "+0000", 3},
            {" GMT",        0L,         "GMT",       "+0000", 4},
            {"+0304",      -184L * mpm, "GMT+03:04", "+0304", 5},
            {"+1342",      -822L * mpm, "GMT+13:42", "+1342", 5},
            {"+0000",       0L,         "GMT+00:00", "+0000", 5},
            {" +1342",     -822L * mpm, "GMT+13:42", "+1342", 6},
            /* ISO-LATIN-1 digits */
            {"GMT+\u0030\u0031:\u0032\u0033", -83L * mpm, "GMT+01:23", "+0123", 9},

           /* In fact, this test case is skipped because TimeZone class can't
            * recognize TimeZone IDs like "+00234" or "-00234".
            */
            {"+00234",     -23L * mpm, "GMT+00:23", "+0023", 5},

            {"GMT-03:04",  184L * mpm, "GMT-03:04", "-0304", 9},
            {"GMT-13:42",  822L * mpm, "GMT-13:42", "-1342", 9},
            {"GMT-00:00",  0L,         "GMT+00:00", "+0000", 9},
            {"GMT-1:11",   71L * mpm,  "GMT-01:11", "-0111", 8},
            {"GMT -13:42", 0L,         "GMT",       "+0000", 3},
            {"-0304",      184L * mpm, "GMT-03:04", "-0304", 5},
            {"-1342",      822L * mpm, "GMT-13:42", "-1342", 5},
            {" -1342",     822L * mpm, "GMT-13:42", "-1342", 6},
            /* ISO-LATIN-1 digits */
            {"GMT-\u0030\u0031:\u0032\u0033", 83L * mpm, "GMT-01:23", "-0123", 9},
           /* In fact, this test case is skipped because TimeZone class can't
            * recognize TimeZone IDs like "+00234" or "-00234".
            */
            {"-00234",     23L * mpm,  "GMT+00:23", "-0023", 5},
        };

        Object[][] invalids = {
          /* given ID       error index   */
            {"GMT+8",       5},
            {"GMT+18",      6},
            {"GMT+208",     6},
            {"GMT+0304",    6},
            {"GMT+42195",   5},
            {"GMT+5:8",     7},
            {"GMT+23:60",   8},
            {"GMT+11:1",    8},
            {"GMT+24:13",   5},
            {"GMT+421:950", 5},
            {"GMT+0a:0A",   5},
            {"GMT+ 13:42",  4},
            {"GMT+13 :42",  6},
            {"GMT+13: 42",  7},
            {"GMT+-13:42",  4},
            {"G M T",       0},
            {"+8",          2},
            {"+18",         3},
            {"+208",        4},
            {"+2360",       4},
            {"+2413",       2},
            {"+42195",      2},
            {"+0AbC",       2},
            {"+ 1342",      1},
            {"+-1342",      1},
            {"1342",        0},
          /* Arabic-Indic digits */
            {"GMT+\u0660\u0661:\u0662\u0663", 4},
          /* Extended Arabic-Indic digits */
            {"GMT+\u06f0\u06f1:\u06f2\u06f3", 4},
          /* Devanagari digits */
            {"GMT+\u0966\u0967:\u0968\u0969", 4},
          /* Fullwidth digits */
            {"GMT+\uFF10\uFF11:\uFF12\uFF13", 4},

            {"GMT-8",       5},
            {"GMT-18",      6},
            {"GMT-208",     6},
            {"GMT-0304",    6},
            {"GMT-42195",   5},
            {"GMT-5:8",     7},
            {"GMT-23:60",   8},
            {"GMT-11:1",    8},
            {"GMT-24:13",   5},
            {"GMT-421:950", 5},
            {"GMT-0a:0A",   5},
            {"GMT- 13:42",  4},
            {"GMT-13 :42",  6},
            {"GMT-13: 42",  7},
            {"GMT-+13:42",  4},
            {"-8",          2},
            {"-18",         3},
            {"-208",        4},
            {"-2360",       4},
            {"-2413",       2},
            {"-42195",      2},
            {"-0AbC",       2},
            {"- 1342",      1},
            {"--1342",      1},
            {"-802",        2},
          /* Arabic-Indic digits */
            {"GMT-\u0660\u0661:\u0662\u0663", 4},
          /* Extended Arabic-Indic digits */
            {"GMT-\u06f0\u06f1:\u06f2\u06f3", 4},
          /* Devanagari digits */
            {"GMT-\u0966\u0967:\u0968\u0969", 4},
          /* Fullwidth digits */
            {"GMT-\uFF10\uFF11:\uFF12\uFF13", 4},
        };

        try {
            for (int i=0; i < locs.length; i++) {
                Locale locale = locs[i];
                Locale.setDefault(locale);

                for (int j=0; j < formats.length; j++) {
                    TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
                    SimpleDateFormat sdf = new SimpleDateFormat(formats[j]);
                    Date date;

                    /* Okay case */
                    for (int k=0; k < valids.length; k++) {
                        ParsePosition pos = new ParsePosition(0);
                        try {
                            date = sdf.parse((String)valids[k][0], pos);
                        }
                        catch (Exception e) {
                            err = true;
                            System.err.println("\tParse  Error [Locale=" +
                                locale + ", " + formats[j] +
                                "/\"" + valids[k][0] +
                                "\"] Unexpected Exception occurred: " + e);
                            continue;
                        }

                        int offset = pos.getIndex();
                        if (offset != ((Integer)valids[k][4]).intValue()) {
                            err = true;
                            System.err.println("\tParse  Error [Locale=" +
                                locale + ", " + formats[j] +
                                "/\"" + valids[k][0] +
                                "\"] invalid index: expected:" + valids[k][4] +
                                ", got:" + offset);
                        }

                        if (date.getTime() != ((Long)valids[k][1]).longValue()) {
                            err = true;
                            System.err.println("\tParse  Error [Locale=" +
                                locale + ", " + formats[j] +
                                "/\"" + valids[k][0] +
                                "\"] expected:" + valids[k][1] +
                                ", got:" + date.getTime() + ", " + date);
                        } else {
/*
                            logln("\tParse  Okay  [Locale=" +
                                locale) + ", " + formats[j] +
                                "/\"" + valids[k][0] +
                                "\"] expected:" + valids[k][1] +
                                ", got:" + date.getTime() + ", " + date);
*/

                            try {
                                date = sdf.parse((String)valids[k][0]);
                            }
                            catch (Exception e) {
                                err = true;
                                System.err.println("\tParse  Error [Locale=" +
                                    locale + ", " + formats[j] +
                                    "/\"" + valids[k][0] +
                                    "\"] Unexpected Exception occurred: " + e);
                                continue;
                            }

                            /* Since TimeZone.getTimeZone() don't treat
                             * "+00234" or "-00234" as a valid ID, skips.
                             */
                            if (((String)valids[k][0]).length() == 6) {
                                continue;
                            }

                            /* Since TimeZone.getTimeZone() don't recognize
                             * +hhmm/-hhmm format, add "GMT" as prefix.
                             */
                            sdf.setTimeZone(TimeZone.getTimeZone(
                                (((((String)valids[k][0]).charAt(0) != 'G') ?
                                "GMT" : "") + valids[k][0])));
                            StringBuffer s = new StringBuffer();
                            sdf.format(date, s, new FieldPosition(0));
                            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

                            String got = s.toString();
                            String expected = (String)valids[k][2+j];
                            if (!got.equals(expected) &&
                                // special case to allow the difference between
                                // DateFormatSymbols.getZoneStrings() and
                                // TimeZone.getDisplayName() for "GMT+-00:00"
                                !(got.equals("GMT-00:00") &&
                                  expected.equals("GMT+00:00"))) {
                                err = true;
                                System.err.println("\tFormat Error [Locale=" +
                                    locale + ", " +
                                    formats[j] + "/\"" + valids[k][0] +
                                    "\"] expected:" + valids[k][2+j] +
                                    ", got:" + s + ", " + date);
                            } else {
/*
                                logln("\tFormat Okay  [Locale=" +
                                    locale + ", " +
                                    formats[j] + "/\"" + valids[k][0] +
                                    "\"] expected:" + valids[k][2+j] +
                                    ", got:" + s + ", " + date);
*/
                            }
                        }
                    }

                    /* Error case 1
                     *   using SimpleDateFormat.parse(String, ParsePosition)
                     */
                    for (int k=0; k < invalids.length; k++) {
                        ParsePosition pos = new ParsePosition(0);
                        try {
                            date = sdf.parse((String)invalids[k][0], pos);
                            if (date != null) {
                                err = true;
                                System.err.println("\tParse  Error [Locale=" +
                                    locale + ", " + formats[j] +
                                    "/\"" + invalids[k][0] +
                                    "\"] expected:null , got:" + date);
                            }
                            int offset = pos.getErrorIndex();
                            if (offset != ((Integer)invalids[k][1]).intValue()) {
                                err = true;
                                System.err.println("\tParse  Error [Locale=" +
                                    locale + ", " + formats[j] +
                                    "/\"" + invalids[k][0] +
                                    "\"] incorrect offset. expected:" +
                                    invalids[k][1] + ", got: " + offset);
                            } else {
/*
                                logln("\tParse  Okay  [Locale=" +
                                    locale + ", " + formats[j] +
                                    "/\"" + invalids[k][0] +
                                    "\"] correct offset: " + offset);
*/
                            }
                        }
                        catch (Exception e) {
                            err = true;
                            System.err.println("\tParse  Error [Locale=" +
                                locale + ", " + formats[j] +
                                "/\"" + invalids[k][0] +
                                "\"] Unexpected Exception occurred: " + e);
                        }
                    }

                    /* Error case 2
                     *   using DateFormat.parse(String)
                     */
                    boolean correctParseException = false;
                    for (int k=0; k < invalids.length; k++) {
                        try {
                            date = sdf.parse((String)invalids[k][0]);
                        }
                        catch (ParseException e) {
                            correctParseException = true;
                            int offset = e.getErrorOffset();
                            if (offset != ((Integer)invalids[k][1]).intValue()) {
                                err = true;
                                System.err.println("\tParse  Error [Locale=" +
                                    locale + ", " + formats[j] +
                                    "/\"" + invalids[k][0] +
                                    "\"] Expected exception occurred with an incorrect offset. expected:" +
                                    invalids[k][1] + ", got: " + offset);
                            } else {
/*
                                logln("\tParse  Okay  [Locale=" +
                                    locale + ", " + formats[j] +
                                    "/\"" + invalids[k][0] +
                                    "\"] Expected exception occurred with an correct offset: "
                                    + offset);
*/
                            }
                        }
                        catch (Exception e) {
                            err = true;
                            System.err.println("\tParse  Error [Locale=" +
                                locale + ", " + formats[j] +
                                "/\"" + invalids[k][0] +
                                "\"] Invalid exception occurred: " + e);
                        }
                        finally {
                            if (!correctParseException) {
                                err = true;
                                System.err.println("\tParse  Error: [Locale=" +
                                    locale + ", " + formats[j] +
                                    "/\"" + invalids[k][0] +
                                    "\"] Expected exception didn't occur.");
                            }
                        }
                    }
                }
            }
        }
        finally {
            Locale.setDefault(savedLocale);
            TimeZone.setDefault(savedTimeZone);
            if (err) {
                errln("SimpleDateFormat.parse()/format() test failed");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new Bug4322313().run(args);
    }
}
