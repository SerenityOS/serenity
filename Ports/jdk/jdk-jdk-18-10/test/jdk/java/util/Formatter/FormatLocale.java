/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8146156 8159548 8060094
 * @modules jdk.localedata
 * @summary test whether uppercasing follows Locale.Category.FORMAT locale.
 *          Also test whether the uppercasing uses the locale specified to the
 *          Formatter API.
 *
 * @run main/othervm FormatLocale
 */

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.Month;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.IntStream;

public class FormatLocale {

    static final Locale TURKISH = new Locale("tr");

    static final List<String> conversions = List.of(
        "%S",
        "%S",
        "%TB",
        "%G",
        "%C");

    static final List<Object> src = List.of(
        "Turkish",
        "Turkish",
        LocalDate.of(2016, Month.APRIL, 1),
        Float.valueOf(100_000_000),
        'i');

    static final List<Locale> defaultLocale = List.of(
            Locale.ENGLISH,
            TURKISH,
            TURKISH,
            Locale.FRANCE,
            TURKISH);

    static final List<Locale> formatLocale = List.of(
            TURKISH,
            Locale.ENGLISH,
            Locale.FRANCE,
            Locale.ENGLISH,
            Locale.ENGLISH);

    static final List<String> expectedWithDefaultLocale = List.of(
            "TURKISH",
            "TURK\u0130SH",
            "N\u0130SAN",
            "1,00000E+08",
            "\u0130");

    static final List<String> expectedWithFormatLocale = List.of(
            "TURK\u0130SH",
            "TURKISH",
            "AVRIL",
            "1.00000E+08",
            "I");

    static void formatLocaleTest() {
        StringBuilder sb = new StringBuilder();

        // checks whether upper casing follows Locale.Category.FORMAT locale
        IntStream.range(0, src.size()).forEach(i -> {
            sb.setLength(0);
            Locale.setDefault(Locale.Category.FORMAT, defaultLocale.get(i));
            new Formatter(sb).format(conversions.get(i), src.get(i));
            if (!sb.toString().equals(expectedWithDefaultLocale.get(i))) {
                throw new RuntimeException(
                        "Wrong uppercasing while using Formatter.format(" +
                                "\"" + conversions.get(i) + "\"" +
                                ") with the default locale: '"
                                + defaultLocale.get(i) +
                                "'. Expected: " + expectedWithDefaultLocale.get(i) +
                                " Returned: " + sb.toString());
            }
        });

        // checks whether upper casing uses the locale set during creation of
        // Formatter instance, instead of the default locale
        IntStream.range(0, src.size()).forEach(i -> {
            sb.setLength(0);
            Locale.setDefault(Locale.Category.FORMAT, defaultLocale.get(i));
            new Formatter(sb, formatLocale.get(i)).format(conversions.get(i),
                    src.get(i));
            if (!sb.toString().equals(expectedWithFormatLocale.get(i))) {
                throw new RuntimeException(
                        "Wrong uppercasing while using Formatter.format(" +
                                "\"" + conversions.get(i) + "\"" +
                                ") with the locale specified during instance" +
                                " creation: '" + formatLocale.get(i) +
                                "'. Expected: " + expectedWithFormatLocale.get(i) +
                                " Returned: " + sb.toString());
            }
        });

    }

    static void nullLocaleTest() {
        String fmt = "%1$ta %1$tA %1$th %1$tB %1tZ";
        String expected = "Fri Friday Jan January PST";
        StringBuilder sb = new StringBuilder();
        Locale orig = Locale.getDefault();

        try {
            Locale.setDefault(Locale.JAPAN);
            Formatter f = new Formatter(sb, (Locale)null);
            ZoneId zid = ZoneId.of("America/Los_Angeles");
            Calendar c = new GregorianCalendar(TimeZone.getTimeZone(zid), Locale.US);
            c.set(2016, 0, 1, 0, 0, 0);
            f.format(fmt, c);
            if (!sb.toString().equals(expected)) {
                throw new RuntimeException(
                    "Localized text returned with null locale.\n" +
                    "    expected: " + expected + "\n" +
                    "    returned: " + sb.toString());
            }

            sb.setLength(0);
            ZonedDateTime zdt = ZonedDateTime.of(2016, 1, 1, 0, 0, 0, 0, zid);
            f.format(fmt, zdt);

            if (!sb.toString().equals(expected)) {
                throw new RuntimeException(
                    "Localized text returned with null locale.\n" +
                    "    expected: " + expected + "\n" +
                    "    returned: " + sb.toString());
            }
        } finally {
            Locale.setDefault(orig);
        }
    }

    public static void main(String [] args) {
        formatLocaleTest();
        nullLocaleTest();
    }
}
