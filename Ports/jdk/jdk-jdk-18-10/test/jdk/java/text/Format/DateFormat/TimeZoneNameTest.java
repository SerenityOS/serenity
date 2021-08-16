/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4348864 4112924 4425386 4495052 4836940 4851113 8008577
 * @summary test time zone display names in en_US locale
 * @library /java/text/testlib
 * @run main/othervm -Djava.locale.providers=COMPAT,SPI TimeZoneNameTest
 */

import java.util.*;
import java.text.*;

public class TimeZoneNameTest extends IntlTest
{
    static final String[] data = {
        // Added to verify the fix for 4836940
        "N", "Antarctica/Rothera", "ROTT", "Rothera Time", "ROTT", "Rothera Time",
        "N", "Asia/Tehran", "IRST", "Iran Standard Time", "IRDT", "Iran Daylight Time",
        "N", "Iran", "IRST", "Iran Standard Time", "IRDT", "Iran Daylight Time",

        // Added to verify the fix for 4851113
        "N", "America/Rankin_Inlet", "CST", "Central Standard Time", "CDT", "Central Daylight Time",
        "N", "Asia/Samarkand", "UZT", "Uzbekistan Time", "UZT", "Uzbekistan Time",
        "N", "Asia/Tashkent", "UZT", "Uzbekistan Time", "UZT", "Uzbekistan Time",
        "N", "Atlantic/Jan_Mayen", "CET", "Central European Time", "CEST", "Central European Summer Time",
        "N", "Europe/Oslo", "CET", "Central European Time", "CEST", "Central European Summer Time",

        "N", "Pacific/Honolulu", "HST", "Hawaii Standard Time", "HST", "Hawaii Standard Time",
        "N", "America/Los_Angeles", "PST", "Pacific Standard Time", "PDT", "Pacific Daylight Time",
        "N", "US/Pacific", "PST", "Pacific Standard Time", "PDT", "Pacific Daylight Time",
        "N", "America/Phoenix", "MST", "Mountain Standard Time", "MST", "Mountain Standard Time",
        "N", "America/Denver", "MST", "Mountain Standard Time", "MDT", "Mountain Daylight Time",
        "N", "America/Chicago", "CST", "Central Standard Time", "CDT", "Central Daylight Time",
        "N", "America/Indianapolis", "EST", "Eastern Standard Time", "EST", "Eastern Standard Time",
        "N", "America/Montreal", "EST", "Eastern Standard Time", "EDT", "Eastern Daylight Time",
        "N", "America/Toronto", "EST", "Eastern Standard Time", "EDT", "Eastern Daylight Time",
        "N", "America/New_York", "EST", "Eastern Standard Time", "EDT", "Eastern Daylight Time",
        "S", "America/Manaus", "AMT", "Amazon Time", "AMT", "Amazon Time",
        "S", "America/Campo_Grande", "AMT", "Amazon Time", "AMST", "Amazon Summer Time",
        "S", "America/Bahia", "BRT", "Brasilia Time", "BRST", "Brasilia Summer Time",
        "N", "America/Halifax", "AST", "Atlantic Standard Time", "ADT", "Atlantic Daylight Time",
        "N", "GMT", "GMT", "Greenwich Mean Time", "GMT", "Greenwich Mean Time",
        "N", "Europe/London", "GMT", "Greenwich Mean Time", "BST", "British Summer Time",
        "N", "Europe/Paris", "CET", "Central European Time", "CEST", "Central European Summer Time",
        "N", "WET", "WET", "Western European Time", "WEST", "Western European Summer Time",
        "N", "Europe/Berlin", "CET", "Central European Time", "CEST", "Central European Summer Time",
        "N", "Asia/Jerusalem", "IST", "Israel Standard Time", "IDT", "Israel Daylight Time",
        "N", "Europe/Helsinki", "EET", "Eastern European Time", "EEST", "Eastern European Summer Time",
        "N", "Africa/Cairo", "EET", "Eastern European Time", "EEST", "Eastern European Summer Time",
        "N", "Europe/Moscow", "MSK", "Moscow Standard Time", "MSD", "Moscow Daylight Time",
        "N", "Asia/Omsk", "OMST", "Omsk Time", "OMSST", "Omsk Summer Time",
        "N", "Asia/Shanghai", "CST", "China Standard Time", "CST", "China Standard Time",
        "N", "Asia/Tokyo", "JST", "Japan Standard Time", "JST", "Japan Standard Time",
        "N", "Japan", "JST", "Japan Standard Time", "JST", "Japan Standard Time",
        "N", "Asia/Seoul", "KST", "Korea Standard Time", "KST", "Korea Standard Time",
        "N", "ROK", "KST", "Korea Standard Time", "KST", "Korea Standard Time",
        "S", "Australia/Darwin", "ACST", "Australian Central Standard Time (Northern Territory)",
                                 "ACST", "Australian Central Standard Time (Northern Territory)",
        "S", "Australia/Adelaide", "ACST", "Australian Central Standard Time (South Australia)",
                                   "ACDT", "Australian Central Daylight Time (South Australia)",
        "S", "Australia/Broken_Hill", "ACST", "Australian Central Standard Time (South Australia/New South Wales)",
                                      "ACDT", "Australian Central Daylight Time (South Australia/New South Wales)",
        "S", "Australia/Hobart", "AEST", "Australian Eastern Standard Time (Tasmania)",
                                 "AEDT", "Australian Eastern Daylight Time (Tasmania)",
        "S", "Australia/Brisbane", "AEST", "Australian Eastern Standard Time (Queensland)",
                                   "AEST", "Australian Eastern Standard Time (Queensland)",
        "S", "Australia/Sydney", "AEST", "Australian Eastern Standard Time (New South Wales)",
                                 "AEDT", "Australian Eastern Daylight Time (New South Wales)",
        "N", "Pacific/Guam", "ChST", "Chamorro Standard Time",
                             "ChST", "Chamorro Standard Time",
        "N", "Pacific/Saipan", "ChST", "Chamorro Standard Time",
                               "ChST", "Chamorro Standard Time",
    };

    public static void main(String[] args) throws Exception {
        Locale reservedLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            new TimeZoneNameTest().run(args);
        } finally {
            // restore the reserved locale
            Locale.setDefault(reservedLocale);
        }
    }

    public void Test4112924() {
        SimpleDateFormat lfmt = new SimpleDateFormat("zzzz");
        SimpleDateFormat sfmt = new SimpleDateFormat("z");

        GregorianCalendar june = new GregorianCalendar(2000, Calendar.JUNE, 21);
        GregorianCalendar december = new GregorianCalendar(2000, Calendar.DECEMBER, 21);

        int count = data.length;
        for (int i = 0; i < count; i++) {
            GregorianCalendar sol1, sol2;

            // check hemisphere
            if ("N".equals(data[i++])) {
                sol1 = december;
                sol2 = june;
            } else {
                sol1 = june;
                sol2 = december;
            }

            TimeZone tz = TimeZone.getTimeZone(data[i++]);
            lfmt.setTimeZone(tz);
            sfmt.setTimeZone(tz);

            logln(tz.getID() + ": " + sfmt.format(sol1.getTime()) + ", " + lfmt.format(sol1.getTime()));
            logln(tz.getID() + ": " + sfmt.format(sol2.getTime()) + ", " + lfmt.format(sol2.getTime()));
            String s = sfmt.format(sol1.getTime());
            if (!data[i].equals(s)) {
                errln(tz.getID() + ": wrong short name: \"" + s + "\" (expected \"" + data[i] + "\")");
            }
            s = lfmt.format(sol1.getTime());
            if (!data[++i].equals(s)) {
                errln(tz.getID() + ": wrong long name: \"" + s + "\" (expected \"" + data[i] + "\")");
            }
            s = sfmt.format(sol2.getTime());
            if (!data[++i].equals(s)) {
                errln(tz.getID() + ": wrong short name: \"" + s + "\" (expected \"" + data[i] + "\")");
            }
            s = lfmt.format(sol2.getTime());
            if (!data[++i].equals(s)) {
                errln(tz.getID() + ": wrong long name: \"" + s + "\" (expected \"" + data[i] + "\")");
            }
        }
    }
}
