/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8005471 8008577 8129881 8130845 8136518 8181157 8210490 8220037
 *      8234347 8236548
 * @modules jdk.localedata
 * @run main/othervm -Djava.locale.providers=CLDR CLDRDisplayNamesTest
 * @summary Make sure that localized time zone names of CLDR are used
 * if specified.
 */

import java.text.*;
import java.util.*;
import static java.util.TimeZone.*;

public class CLDRDisplayNamesTest {
    /*
     * The first element is a language tag. The rest are localized
     * display names of America/Los_Angeles copied from the CLDR
     * resources data. If data change in CLDR, test data below will
     * need to be changed accordingly.
     *
     * Generic names are NOT tested (until they are supported by API).
     */
    static final String[][] CLDR_DATA = {
        {
            "ja-JP",
            "\u30a2\u30e1\u30ea\u30ab\u592a\u5e73\u6d0b\u6a19\u6e96\u6642",
            "GMT-08:00",
            "\u30a2\u30e1\u30ea\u30ab\u592a\u5e73\u6d0b\u590f\u6642\u9593",
            "GMT-07:00",
            //"\u30a2\u30e1\u30ea\u30ab\u592a\u5e73\u6d0b\u6642\u9593",
        //"PT"
        },
        {
            "zh-CN",
            "\u5317\u7f8e\u592a\u5e73\u6d0b\u6807\u51c6\u65f6\u95f4",
            "GMT-08:00",
            "\u5317\u7f8e\u592a\u5e73\u6d0b\u590f\u4ee4\u65f6\u95f4",
            "GMT-07:00",
            //"\u5317\u7f8e\u592a\u5e73\u6d0b\u65f6\u95f4",
        //"PT",
        },
        {
            "de-DE",
            "Nordamerikanische Westk\u00fcsten-Normalzeit",
            "GMT-08:00",
            "Nordamerikanische Westk\u00fcsten-Sommerzeit",
            "GMT-07:00",
            //"Nordamerikanische Westk\u00fcstenzeit",
        //"PT",
        },
    };

    private static final String NO_INHERITANCE_MARKER = "\u2205\u2205\u2205";

    public static void main(String[] args) {
        // Make sure that localized time zone names of CLDR are used
        // if specified.
        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
        int errors = 0;
        for (String[] data : CLDR_DATA) {
            Locale locale = Locale.forLanguageTag(data[0]);
            for (int i = 1; i < data.length; i++) {
                int style = ((i % 2) == 1) ? LONG : SHORT;
                boolean daylight = (i == 3 || i == 4);
                String name = tz.getDisplayName(daylight, style, locale);
                if (!data[i].equals(name)) {
                    System.err.printf("error: got '%s' expected '%s' (style=%d, daylight=%s, locale=%s)%n",
                            name, data[i], style, daylight, locale);
                    errors++;
                }
            }
        }

        // for 8129881
        /* 8234347: CLDR Converter will not pre-fill short display names from COMPAT anymore.
        tz = TimeZone.getTimeZone("Europe/Vienna");
        String name = tz.getDisplayName(false, SHORT, Locale.ENGLISH);
        if (!"CET".equals(name)) {
            System.err.printf("error: got '%s' expected 'CET' %n", name);
            errors++;
        }
        */

        // for 8130845
        SimpleDateFormat fmtROOT = new SimpleDateFormat("EEE MMM d hh:mm:ss z yyyy", Locale.ROOT);
        SimpleDateFormat fmtUS = new SimpleDateFormat("EEE MMM d hh:mm:ss z yyyy", Locale.US);
        SimpleDateFormat fmtUK = new SimpleDateFormat("EEE MMM d hh:mm:ss z yyyy", Locale.UK);
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ROOT);
            fmtROOT.parse("Thu Nov 13 04:35:51 GMT-09:00 2008");
            fmtUS.parse("Thu Nov 13 04:35:51 AKST 2008");
            fmtUK.parse("Thu Nov 13 04:35:51 GMT-09:00 2008");
        } catch (ParseException pe) {
            System.err.println(pe);
            errors++;
        } finally {
            Locale.setDefault(originalLocale);
        }

        // for 8210490
        // Check that TimeZone.getDisplayName should honor passed locale parameter,
        // even if default locale is set to some other locale.
        Locale.setDefault(Locale.forLanguageTag("ar-PK"));
        TimeZone zi = TimeZone.getTimeZone("Etc/GMT-5");
        String displayName = zi.getDisplayName(false, TimeZone.SHORT, Locale.US);
        Locale.setDefault(originalLocale);
        if (!displayName.equals("GMT+05:00")) {
            System.err.printf("Wrong display name for timezone Etc/GMT-5 : expected GMT+05:00,  Actual " + displayName);
            errors++;
        }

        // 8217366: No "no inheritance marker" should be left in the returned array
        // from DateFormatSymbols.getZoneStrings()
        errors += List.of(Locale.ROOT,
                Locale.CHINA,
                Locale.GERMANY,
                Locale.JAPAN,
                Locale.UK,
                Locale.US,
                Locale.forLanguageTag("hi-IN"),
                Locale.forLanguageTag("es-419")).stream()
            .peek(System.out::println)
            .map(l -> DateFormatSymbols.getInstance(l).getZoneStrings())
            .flatMap(zoneStrings -> Arrays.stream(zoneStrings))
            .filter(namesArray -> Arrays.stream(namesArray)
                .anyMatch(aName -> aName.equals(NO_INHERITANCE_MARKER)))
            .peek(marker -> {
                 System.err.println("No-inheritance-marker is detected with tzid: "
                                                + marker[0]);
            })
            .count();

        // 8220037: Make sure CLDRConverter uniquely produces bundles, regardless of the
        // source file enumeration order.
        /* 8234347: CLDR Converter will not pre-fill short display names from COMPAT anymore.
        tz = TimeZone.getTimeZone("America/Argentina/La_Rioja");
        if (!"ARST".equals(tz.getDisplayName(true, TimeZone.SHORT,
                                new Locale.Builder()
                                    .setLanguage("en")
                                    .setRegion("CA")
                                    .build()))) {
            System.err.println("Short display name of \"" + tz.getID() + "\" was not \"ARST\"");
            errors++;
        }
        */

        if (errors > 0) {
            throw new RuntimeException("test failed");
        }
    }
}
