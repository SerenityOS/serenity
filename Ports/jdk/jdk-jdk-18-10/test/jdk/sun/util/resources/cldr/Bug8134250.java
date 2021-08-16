/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8134250 8134520
 * @modules jdk.localedata
 * @summary Tests CLDR/LDML features are correctly reflected in JDK.
 * @run main/othervm -Djava.locale.providers=CLDR Bug8134250
 */

// Note this test highly depends on a particular version of CLDR. Results
// may vary in the future.

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.Locale;

public class Bug8134250 {
    public static void main(String [] args) {
        LocalDate d = LocalDate.of(1980, Month.JANUARY, 1);

        // en-GB inherits from en-001 where its short tz name for
        // America/Los_Angeles is "non-inheritance marker". Thus the
        // resulting formatted text should be a custom ID.
        DateTimeFormatterBuilder dtfb = new DateTimeFormatterBuilder();
        dtfb.appendZoneText(TextStyle.SHORT);
        DateTimeFormatter dtf = dtfb.toFormatter(Locale.UK)
            .withZone(ZoneId.of("America/Los_Angeles"));
        String result = dtf.format(d);
        System.out.println(result);
        if (!"GMT-08:00".equals(result)) {
            throw new RuntimeException("short time zone name for America/Los_Angeles in en_GB is incorrect. Got: " + result + ", expected: GMT-08:00");
        }

        // Islamic Um-Alqura calendar is an alias to Islamic calendar.
        // In Islamic calendar data, only month names are localized, also
        // date/time format for FULL style should be inherited from "generic"
        // calendar, where it includes ERA field.
        Locale locale = Locale.forLanguageTag("en-US-u-ca-islamic-umalqura");
        Chronology chrono = Chronology.ofLocale(locale);
        dtf = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.FULL)
            .withLocale(locale)
            .withChronology(chrono);
        result = dtf.format(d);
        System.out.println(dtf.format(d));
        if (!"Tuesday, Safar 12, 1400 AH".equals(result)) {
            throw new RuntimeException("FULL date format of Islamic Um-Alqura calendar in en_US is incorrect. Got: " + result + ", expected: Tuesday, Safar 12, 1400 AH");
        }
    }
}
