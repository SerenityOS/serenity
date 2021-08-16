/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4919632
 * @summary Unit test for ISO8601 time zone format support
 */

import java.text.*;
import java.util.*;

public class ISO8601ZoneTest {
    static final Date TIMESTAMP = new Date(1283758039020L);

    static final String[][] formatData = {
        // time zone name, expected output at TIMESTAMP
        { "America/Los_Angeles", "2010-09-06T00:27:19.020-07", },
        { "America/Los_Angeles", "2010-09-06T00:27:19.020-0700", },
        { "America/Los_Angeles", "2010-09-06T00:27:19.020-07:00", },
        { "Australia/Sydney", "2010-09-06T17:27:19.020+10", },
        { "Australia/Sydney", "2010-09-06T17:27:19.020+1000", },
        { "Australia/Sydney", "2010-09-06T17:27:19.020+10:00", },
        { "GMT-07:00", "2010-09-06T00:27:19.020-07", },
        { "GMT-07:00", "2010-09-06T00:27:19.020-0700", },
        { "GMT-07:00", "2010-09-06T00:27:19.020-07:00", },
        { "UTC", "2010-09-06T07:27:19.020Z", },
        { "UTC", "2010-09-06T07:27:19.020Z", },
        { "UTC", "2010-09-06T07:27:19.020Z", },
    };

    static final String[] zones = {
        "America/Los_Angeles", "Australia/Sydney", "GMT-07:00",
        "UTC", "GMT+05:30", "GMT-01:23",
    };

    static final String[] isoZoneFormats = {
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
    };

    // badData[][0] - format
    // badData[][1] - (bad) text to be parsed
    // badData[][2] - subtext at the end of which a parse error is detected
    static final String[][] badData = {
        { "X", "1", "1" },
        { "X", "+1", "+1" },
        { "X", "-2", "-2" },
        { "X", "-24", "-2" },
        { "X", "+24", "+2" },

        { "XX", "9", "9" },
        { "XX", "23", "2" },
        { "XX", "234", "2" },
        { "XX", "3456", "3" },
        { "XX", "23456", "2" },
        { "XX", "+1", "+1" },
        { "XX", "-12", "-12" },
        { "XX", "+123", "+123" },
        { "XX", "-12:34", "-12" },
        { "XX", "+12:34", "+12" },
        { "XX", "-2423", "-2" },
        { "XX", "+2423", "+2" },
        { "XX", "-1260", "-126" },
        { "XX", "+1260", "+126" },

        { "XXX", "9", "9" },
        { "XXX", "23", "2" },
        { "XXX", "234", "2" },
        { "XXX", "3456", "3" },
        { "XXX", "23456", "2" },
        { "XXX", "2:34", "2" },
        { "XXX", "12:4", "1" },
        { "XXX", "12:34", "1" },
        { "XXX", "-1", "-1" },
        { "XXX", "+1", "+1" },
        { "XXX", "-12", "-12" },
        { "XXX", "+12", "+12" },
        { "XXX", "-123", "-12" },
        { "XXX", "+123", "+12" },
        { "XXX", "-1234", "-12" },
        { "XXX", "+1234", "+12" },
        { "XXX", "+24:23", "+2" },
        { "XXX", "+12:60", "+12:6" },
        { "XXX", "+1:23", "+1" },
        { "XXX", "+12:3", "+12:3" },
    };

    static String[] badFormats = {
        "XXXX", "XXXXX", "XXXXXX",
    };

    public static void main(String[] args) throws Exception {
        TimeZone tz = TimeZone.getDefault();
        Locale loc = Locale.getDefault();
        Locale.setDefault(Locale.US);

        try {
            for (int i = 0; i < formatData.length; i++) {
                TimeZone.setDefault(TimeZone.getTimeZone(formatData[i][0]));
                formatTest(isoZoneFormats[i % isoZoneFormats.length],
                           formatData[i][1]);
            }

            for (String zone : zones) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                for (String fmt : isoZoneFormats) {
                    roundtripTest(fmt);
                    SimpleDateFormat f = new SimpleDateFormat(fmt);
                }

            }

            for (String[] d : badData) {
                badDataParsing(d[0], d[1], d[2].length());
            }

            for (String fmt : badFormats) {
                badFormat(fmt);
            }
        } finally {
            TimeZone.setDefault(tz);
            Locale.setDefault(loc);
        }

    }

    static void formatTest(String fmt, String expected) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(fmt);
        String s = sdf.format(TIMESTAMP);
        if (!expected.equals(s)) {
            throw new RuntimeException("formatTest: got " + s
                                       + ", expected " + expected);
        }

        Date d = sdf.parse(s);
        if (d.getTime() != TIMESTAMP.getTime()) {
            throw new RuntimeException("formatTest: parse(" + s
                                       + "), got " + d.getTime()
                                       + ", expected " + TIMESTAMP.getTime());
        }

        ParsePosition pos = new ParsePosition(0);
        d = sdf.parse(s + "123", pos);
        if (d.getTime() != TIMESTAMP.getTime()) {
            throw new RuntimeException("formatTest: parse(" + s
                                       + "), got " + d.getTime()
                                       + ", expected " + TIMESTAMP.getTime());
        }
        if (pos.getIndex() != s.length()) {
            throw new RuntimeException("formatTest: wrong resulting parse position: "
                                       + pos.getIndex() + ", expected " + s.length());
        }
    }

    static void roundtripTest(String fmt) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(fmt);
        Date date = new Date();

        int fractionalHour = sdf.getTimeZone().getOffset(date.getTime());
        fractionalHour %= 3600000; // fraction of hour

        String s = sdf.format(date);
        Date pd = sdf.parse(s);
        long diffsInMillis = pd.getTime() - date.getTime();
        if (diffsInMillis != 0) {
            if (diffsInMillis != fractionalHour) {
                throw new RuntimeException("fmt= " + fmt
                                           + ", diff="+diffsInMillis
                                           + ", fraction=" + fractionalHour);
            }
        }
    }


    static void badDataParsing(String fmt, String text, int expectedErrorIndex) {
        SimpleDateFormat sdf = new SimpleDateFormat(fmt);
        try {
            sdf.parse(text);
            throw new RuntimeException("didn't throw an exception: fmt=" + fmt
                                       + ", text=" + text);
        } catch (ParseException e) {
            // OK
        }

        ParsePosition pos = new ParsePosition(0);
        Date d = sdf.parse(text, pos);
        int errorIndex = pos.getErrorIndex();
        if (d != null || errorIndex != expectedErrorIndex) {
            throw new RuntimeException("Bad error index=" + errorIndex
                                       + ", expected=" + expectedErrorIndex
                                       + ", fmt=" + fmt + ", text=" + text);
        }
    }

    static void badFormat(String fmt) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(fmt);
            throw new RuntimeException("Constructor didn't throw an exception: fmt=" + fmt);
        } catch (IllegalArgumentException e) {
            // OK
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat();
            sdf.applyPattern(fmt);
            throw new RuntimeException("applyPattern didn't throw an exception: fmt=" + fmt);
        } catch (IllegalArgumentException e) {
            // OK
        }
    }
}
