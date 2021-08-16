/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8141243
 * @summary Make sure that SimpleDateFormat parses "UTC" as the UTC time zone.
 * @run main Bug8141243
 * @run main/othervm -Djava.locale.providers=COMPAT Bug8141243
 */

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import static java.util.TimeZone.*;

public class Bug8141243 {
    public static void main(String[] args) {
        TimeZone UTC = TimeZone.getTimeZone("UTC");
        TimeZone initTz = TimeZone.getDefault();

        List<String> errors = new ArrayList<>();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
            for (Locale locale : DateFormat.getAvailableLocales()) {
                // exclude any locales which localize "UTC".
                String utc = UTC.getDisplayName(false, SHORT, locale);
                if (!"UTC".equals(utc)) {
                    System.out.println("Skipping " + locale + " due to localized UTC name: " + utc);
                    continue;
                }
                SimpleDateFormat fmt = new SimpleDateFormat("z", locale);
                try {
                    Date date = fmt.parse("UTC");
                    // Parsed one may not exactly be UTC. Universal, UCT, etc. are equivalents.
                    if (!fmt.getTimeZone().getID().matches("(Etc/)?(UTC|Universal|UCT|Zulu)")) {
                        errors.add("timezone: " + fmt.getTimeZone().getID()
                                   + ", locale: " + locale);
                    }
                } catch (ParseException e) {
                    errors.add("parse exception: " + e + ", locale: " + locale);
                }
            }
        } finally {
            // Restore the default time zone
            TimeZone.setDefault(initTz);
        }

        if (!errors.isEmpty()) {
            System.out.println("Got unexpected results:");
            for (String s : errors) {
                System.out.println("    " + s);
            }
            throw new RuntimeException("Test failed.");
        } else {
            System.out.println("Test passed.");
        }
    }
}
