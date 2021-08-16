/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8134384 8234347 8236548
 * @summary Tests CLDR TimeZoneNames has English names for all tzids
 * @run main/othervm -Djava.locale.providers=CLDR Bug8134384
 */

import java.text.*;
import java.time.*;
import java.util.*;

public class Bug8134384 {
    public static void main(String [] args) {
        TimeZone original = TimeZone.getDefault();

        try {
            for (String tz : TimeZone.getAvailableIDs() ) {
                TimeZone.setDefault(TimeZone.getTimeZone(tz));
                // Summer solstice
                String date1 = Date.from(Instant.parse("2015-06-21T00:00:00.00Z")).toString();
                testParse(Locale.ENGLISH, date1, tz);
                testParse(Locale.US, date1, tz);
                // Winter solstice
                String date2 = Date.from(Instant.parse("2015-12-22T00:00:00.00Z")).toString();
                testParse(Locale.ENGLISH, date2, tz);
                testParse(Locale.US, date2, tz);
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private static void testParse(Locale locale, String date, String tz) {
        try {
            new SimpleDateFormat("EEE MMM d hh:mm:ss z yyyy", locale).parse(date);
            System.out.println(String.format(Locale.ENGLISH, "OK parsing '%s' in locale '%s', tz: %s", date, locale, tz));
        } catch (ParseException pe) {
            throw new RuntimeException(String.format(Locale.ENGLISH, "ERROR parsing '%s' in locale '%s', tz: %s: %s", date, locale, tz, pe.toString()));
        }
    }
}
