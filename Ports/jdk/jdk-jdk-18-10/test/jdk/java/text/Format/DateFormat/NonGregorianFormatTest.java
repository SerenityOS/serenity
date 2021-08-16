/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4833268 6253991 8008577
 * @summary Test formatting and parsing with non-Gregorian calendars
 * @modules jdk.localedata
 * @run main/othervm -Djava.locale.providers=COMPAT,SPI NonGregorianFormatTest
 */

import java.util.*;
import java.text.*;
import static java.util.Calendar.*;

public class NonGregorianFormatTest {
    static int errors;

    @SuppressWarnings("deprecation")
    static final Object[][] JAPANESE_EN = {
        { "GGGG yyyy MMMM d", "Showa 1 December 31", new Date(1926-1900, DECEMBER, 31) },
        { "GGGG yyyy MMMM d", "Showa 64 January 6", new Date(1989-1900, JANUARY, 6) },
        { "GGGG yyyy MMMM d", "Heisei 1 August 9", new Date(1989-1900, AUGUST, 9) },
        { "GGGG yyyy MMMM d", "Heisei 17 June 10", new Date(2005-1900, JUNE, 10) },
        { "Gy.MM.dd", "S1.12.31", new Date(1926-1900, DECEMBER, 31) },
        { "Gy.MM.dd", "S64.01.06", new Date(1989-1900, JANUARY, 6) },
        { "Gyy.MM.dd", "H01.08.09", new Date(1989-1900, AUGUST, 9) },
        { "Gy.M.d", "H1.8.9", new Date(1989-1900, AUGUST, 9) },
        { "Gy.MM.dd", "H17.06.10", new Date(2005-1900, JUNE, 10) },
    };

    // Invalid dates for parse exception tests
    static final Object[][] EXCEPTION_JAPANESE_EN = {
        { "GGGG yyyy MMMM d", "Showa 1 December 10" },
        { "GGGG yyyy MMMM d", "Showa 64 January 16" },
        { "GGGG yyyy MMMM d", "Heisei 1 January 1" },
        { "Gy.MM.dd", "S1.12.10" },
        { "Gy.MM.dd", "S64.01.16" },
        { "Gyy.MM.dd", "H01.01.01" },
    };

    @SuppressWarnings("deprecation")
    static final Object[][] BUDDHIST_EN = {
        { "GGGG yyyy MMMM d", "B.E. 2469 December 31", new Date(1926-1900, DECEMBER, 31) },
        { "GGGG yyyy MMMM d", "B.E. 2532 January 6", new Date(1989-1900, JANUARY, 6) },
        { "GGGG yyyy MMMM d", "B.E. 2532 August 8", new Date(1989-1900, AUGUST, 8) },
        { "GGGG yyyy MMMM d", "B.E. 2548 June 10", new Date(2005-1900, JUNE, 10) },
        { "Gyyyy/MM/dd", "B.E.2469/12/31", new Date(1926-1900, DECEMBER, 31) },
        { "Gyyyy/MM/dd", "B.E.2532/01/06", new Date(1989-1900, JANUARY, 6) },
        { "Gyyyy/MM/dd", "B.E.2532/08/09", new Date(1989-1900, AUGUST, 9) },
        { "Gyyyy/MM/dd", "B.E.2548/06/10", new Date(2005-1900, JUNE, 10) },
    };

    static final String FULL_DATE_FORMAT_JA = "GGGGyyyy'\u5e74'M'\u6708'd'\u65e5'";

    @SuppressWarnings("deprecation")
    static final Object[][] JAPANESE_JA = {
        { FULL_DATE_FORMAT_JA, "\u662d\u548c\u5143\u5e7412\u670831\u65e5", new Date(1926-1900, DECEMBER, 31) },
        { FULL_DATE_FORMAT_JA, "\u662d\u548c64\u5e741\u67086\u65e5", new Date(1989-1900, JANUARY, 6) },
        { FULL_DATE_FORMAT_JA, "\u5e73\u6210\u5143\u5e748\u67089\u65e5", new Date(1989-1900, AUGUST, 9) },
        { FULL_DATE_FORMAT_JA, "\u5e73\u621017\u5e746\u670810\u65e5", new Date(2005-1900, JUNE, 10) },
        { "Gyy.MM.dd", "S01.12.31", new Date(1926-1900, DECEMBER, 31) },
        { "Gyy.MM.dd", "S64.01.06", new Date(1989-1900, JANUARY, 6) },
        { "Gyy.MM.dd", "H01.08.09", new Date(1989-1900, AUGUST, 9) },
        { "Gy.M.d", "H1.8.9", new Date(1989-1900, AUGUST, 9) },
        { "Gyy.MM.dd", "H17.06.10", new Date(2005-1900, JUNE, 10) },
    };

    // Invalid dates for parse exception tests
    static final Object[][] EXCEPTION_JAPANESE_JA = {
        { FULL_DATE_FORMAT_JA, "\u662d\u548c\u5143\u5e7412\u670810\u65e5" },
        { FULL_DATE_FORMAT_JA, "\u662d\u548c64\u5e741\u670816\u65e5" },
        { FULL_DATE_FORMAT_JA, "\u5e73\u6210\u5143\u5e741\u67081\u65e5" },
        { "Gyy.MM.dd", "S01.12.10" },
        { "Gyy.MM.dd", "S64.01.16" },
        { "Gyy.MM.dd", "H01.01.01" },
    };

    @SuppressWarnings("deprecation")
    static final Object[][] BUDDHIST_JA = {
        { FULL_DATE_FORMAT_JA, "\u4ecf\u66a62469\u5e7412\u670831\u65e5", new Date(1926-1900, DECEMBER, 31) },
        { FULL_DATE_FORMAT_JA, "\u4ecf\u66a62532\u5e741\u67086\u65e5", new Date(1989-1900, JANUARY, 6) },
        { FULL_DATE_FORMAT_JA, "\u4ecf\u66a62532\u5e748\u67089\u65e5", new Date(1989-1900, AUGUST, 9) },
        { FULL_DATE_FORMAT_JA, "\u4ecf\u66a62548\u5e746\u670810\u65e5", new Date(2005-1900, JUNE, 10) },
        { "Gyyyy/MM/dd", "B.E.2469/12/31", new Date(1926-1900, DECEMBER, 31) },
        { "Gyyyy/MM/dd", "B.E.2532/01/06", new Date(1989-1900, JANUARY, 6) },
        { "Gyyyy/MM/dd", "B.E.2532/08/09", new Date(1989-1900, AUGUST, 9) },
        { "Gyyyy/MM/dd", "B.E.2548/06/10", new Date(2005-1900, JUNE, 10) },
    };

    public static void main(String[] args) throws ParseException {
        Locale defaultLocale = Locale.getDefault();
        Locale[] locales = { Locale.ENGLISH, Locale.JAPAN };
        try {
            for (Locale locale : locales) {
                test(locale);
            }
        } finally {
            Locale.setDefault(defaultLocale);
        }
        if (errors > 0) {
            throw new RuntimeException("FAILED: " + errors + " error(s)");
        }
    }

    private static void test(Locale locale) {
        Locale.setDefault(locale);

        // Tests with the Japanese imperial calendar
        Locale calendarLocale = new Locale("ja", "JP", "JP");
        testRoundTrip(calendarLocale);
        testRoundTripSimple(calendarLocale,
                            locale == Locale.ENGLISH ? JAPANESE_EN : JAPANESE_JA);
        testParseExceptions(calendarLocale,
                            locale == Locale.ENGLISH ? EXCEPTION_JAPANESE_EN : EXCEPTION_JAPANESE_JA);

        // Tests with the Thai Buddhist calendar
        calendarLocale = new Locale("th", "TH");
        testRoundTrip(calendarLocale);
        testRoundTripSimple(calendarLocale,
                            locale == Locale.ENGLISH ? BUDDHIST_EN : BUDDHIST_JA);
    }

    @SuppressWarnings("deprecation")
    private static void testRoundTrip(Locale calendarLocale) {
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL,
                                                       DateFormat.FULL,
                                                       calendarLocale);

        long t = System.currentTimeMillis();
        t = (t / 1000) * 1000; // discard milliseconds
        testRoundTrip(df, new Date(t));

        // H1.8.9
        testRoundTrip(df, new Date(1989-1900, AUGUST, 9));

        // H17.6.13
        testRoundTrip(df, new Date(2005-1900, JUNE, 13));
    }

    private static void testRoundTrip(DateFormat df, Date orig) {
        try {
            String s = df.format(orig);
            Date parsed = df.parse(s);
            if (!orig.equals(parsed)) {
                error("testRoundTrip: bad date: origianl: '%s', parsed '%s'%n", orig, parsed);
            }
        } catch (Exception e) {
            error("Unexpected exception: %s%n", e);
        }
    }

    private static void testRoundTripSimple(Locale calendarLocale, Object[][] data) {
        try {
            for (Object[] item : data) {
                String pattern = (String) item[0];
                String str = (String) item[1];
                Date date = (Date) item[2];
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                Calendar cal = Calendar.getInstance(calendarLocale);
                sdf.setCalendar(cal);
                String s = sdf.format(date);
                if (!s.equals(str)) {
                    error("testRoundTripSimple: Got '%s', expected '%s'%n", s, str);
                }
                Date d = sdf.parse(str);
                if (!d.equals(date)) {
                    error("testRoundTripSimple: Got '%s', expected '%s'%n", d, date);
                }
            }
        } catch (Exception e) {
            error("Unexpected exception: %s%n", e);
        }
    }

    private static void testParseExceptions(Locale calendarLocale, Object[][] data) {
        for (Object[] item : data) {
            String pattern = (String) item[0];
            String str = (String) item[1];
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            Calendar cal = Calendar.getInstance(calendarLocale);
            sdf.setCalendar(cal);
            sdf.setLenient(false);
            try {
                Date d = sdf.parse(str);
                error("testParseExceptions: parsing '%s' doesn't throw a ParseException.%n", str);
            } catch (ParseException e) {
                // OK
            }
        }
    }

    private static void error(String msg) {
        System.out.println(msg);
        errors++;
    }

    private static void error(String fmt, Object... args) {
        System.out.printf(fmt, args);
        errors++;
    }
}
