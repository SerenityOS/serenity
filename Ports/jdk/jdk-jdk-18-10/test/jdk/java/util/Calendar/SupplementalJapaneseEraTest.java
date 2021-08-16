/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.text.SimpleDateFormat;
import java.time.chrono.JapaneseChronology;
import java.time.chrono.JapaneseDate;
import java.time.chrono.JapaneseEra;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import static java.util.GregorianCalendar.*;
import java.util.Locale;

/*
 * Usage:
 *   java -Djdk.calendar.japanese.supplemental.era=... SupplementalJapaneseEraTest <flag>
 *      -t   executes tests with a valid property value
 *      -b <eraname>
 *           executes tests with an invalid property value
 */

public class SupplementalJapaneseEraTest {
    private static final Locale WAREKI_LOCALE = Locale.forLanguageTag("ja-JP-u-ca-japanese");
    private static final String SUP_ERA_NAME = "SupEra";
    private static final String SUP_ERA_ABBR = "S.E.";
    private static int errors = 0;

    public static void main(String[] args) {
        // args[0] is a flag.
        switch (args[0]) {
        case "-t":
            // test with a valid property value
            testProperty();
            break;

        case "-b":
            // test with an invalid property value
            // args[1] is the current era name.
            testValidation(args[1].replace("\r", "")); // remove any CR for Cygwin
            break;
        }
        if (errors != 0) {
            throw new RuntimeException("test failed");
        }
    }

    private static void testProperty() {
        Calendar jcal = new Calendar.Builder()
            .setCalendarType("japanese")
            .setFields(ERA, 6, YEAR, 1, DAY_OF_YEAR, 1)
            .build();
        Date firstDayOfEra = jcal.getTime();
        jcal.set(ERA, jcal.get(ERA) - 1); // previous era
        jcal.set(YEAR, 1);
        jcal.set(DAY_OF_YEAR, 1);
        Calendar cal = new GregorianCalendar();
        cal.setTimeInMillis(jcal.getTimeInMillis());
        cal.add(YEAR, 199);
        int year = cal.get(YEAR);

        SimpleDateFormat sdf;
        String expected, got;

        // test long era name
        sdf = new SimpleDateFormat("GGGG y-MM-dd", WAREKI_LOCALE);
        got = sdf.format(firstDayOfEra);
        expected = SUP_ERA_NAME + " 1-02-11";
        if (!expected.equals(got)) {
            System.err.printf("GGGG y-MM-dd: got=\"%s\", expected=\"%s\"%n", got, expected);
            errors++;
        }

        // test era abbreviation
        sdf = new SimpleDateFormat("G y-MM-dd", WAREKI_LOCALE);
        got = sdf.format(firstDayOfEra);
        expected = SUP_ERA_ABBR + " 1-02-11";
        if (!expected.equals(got)) {
            System.err.printf("G y-MM-dd: got=\"%s\", expected=\"%s\"%n", got, expected);
            errors++;
        }

        // confirm the gregorian year
        sdf = new SimpleDateFormat("y", Locale.US);
        int y = Integer.parseInt(sdf.format(firstDayOfEra));
        if (y != year) {
            System.err.printf("Gregorian year: got=%d, expected=%d%n", y, year);
            errors++;
        }

        // test java.time.chrono.JapaneseEra
        JapaneseDate jdate = JapaneseDate.of(year, 2, 11);
        got = jdate.toString();
        expected = "Japanese " + SUP_ERA_NAME + " 1-02-11";
        if (!expected.equals(got)) {
            System.err.printf("JapaneseDate: got=\"%s\", expected=\"%s\"%n", got, expected);
            errors++;
        }
        JapaneseEra jera = jdate.getEra();
        got = jera.getDisplayName(TextStyle.FULL, Locale.US);
        if (!SUP_ERA_NAME.equals(got)) {
            System.err.printf("JapaneseEra (FULL): got=\"%s\", expected=\"%s\"%n", got, SUP_ERA_NAME);
            errors++;
        }
        got = jera.getDisplayName(TextStyle.SHORT, Locale.US);
        if (!SUP_ERA_NAME.equals(got)) {
            System.err.printf("JapaneseEra (SHORT): got=\"%s\", expected=\"%s\"%n", got, SUP_ERA_NAME);
            errors++;
        }
        got = jera.getDisplayName(TextStyle.NARROW, Locale.US);
        if (!SUP_ERA_ABBR.equals(got)) {
            System.err.printf("JapaneseEra (NARROW): got=\"%s\", expected=\"%s\"%n", got, SUP_ERA_ABBR);
            errors++;
        }
        got = jera.getDisplayName(TextStyle.NARROW_STANDALONE, Locale.US);
        if (!SUP_ERA_ABBR.equals(got)) {
            System.err.printf("JapaneseEra (NARROW_STANDALONE): got=\"%s\", expected=\"%s\"%n", got, SUP_ERA_ABBR);
            errors++;
        }

        // test full/short/narrow names with java.time.format
        got = DateTimeFormatter
            .ofPattern("GGGG G GGGGG")
            .withLocale(Locale.US)
            .withChronology(JapaneseChronology.INSTANCE)
            .format(jdate);
        expected = SUP_ERA_NAME + " " + SUP_ERA_NAME + " " + SUP_ERA_ABBR;
        if (!expected.equals(got)) {
            System.err.printf("java.time formatter full/short/narrow names: got=\"%s\", expected=\"%s\"%n", got, expected);
            errors++;
        }
    }

    private static void testValidation(String eraName) {
        Calendar jcal = new Calendar.Builder()
            .setCalendarType("japanese")
            .setFields(YEAR, 1, DAY_OF_YEAR, 1)
            .build();
        if (!jcal.getDisplayName(ERA, LONG, Locale.US).equals(eraName)) {
            errors++;
            String prop = System.getProperty("jdk.calendar.japanese.supplemental.era");
            System.err.println("Era changed with invalid property: " + prop);
        }
    }
}
