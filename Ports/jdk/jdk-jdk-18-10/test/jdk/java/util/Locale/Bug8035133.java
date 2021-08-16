/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8035133
 * @summary Checks that the tags matching the range with quality weight q=0
 *          e.g. en;q=0 must be elimited and must not be the part of output
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;


public class Bug8035133 {

    private static boolean err = false;

    public static void main(String[] args) {

        // checking Locale.lookup with de-ch;q=0
        checkLookup("en;q=0.1, *-ch;q=0.5, de-ch;q=0",
                "de-ch, en, fr-ch", "fr-CH");

        /* checking Locale.lookup with *;q=0 '*' should be ignored
         * in lookup
         */
        checkLookup("en;q=0.1, *-ch;q=0.5, *;q=0",
                "de-ch, en, fr-ch", "de-CH");

        // checking Locale.filter with fr-ch;q=0 in BASIC_FILTERING
        checkFilter("en;q=0.1, fr-ch;q=0.0, de-ch;q=0.5",
                "de-ch, en, fr-ch", "de-CH, en");

        // checking Locale.filter with *;q=0 in BASIC_FILTERING
        checkFilter("de-ch;q=0.6, *;q=0", "de-ch, fr-ch", "");

        // checking Locale.filter with *;q=0 in BASIC_FILTERING
        checkFilter("de-ch;q=0.6, de;q=0", "de-ch", "");

        // checking Locale.filter with *;q=0.6, en;q=0 in BASIC_FILTERING
        checkFilter("*;q=0.6, en;q=0", "de-ch, hi-in, en", "de-CH, hi-IN");

        // checking Locale.filter with de-ch;q=0 in EXTENDED_FILTERING
        checkFilter("en;q=0.1, *-ch;q=0.5, de-ch;q=0",
                "de-ch, en, fr-ch", "fr-CH, en");

        /* checking Locale.filter with *-ch;q=0 in EXTENDED_FILTERING which
         * must make filter to return "" empty or no match
         */
        checkFilter("de-ch;q=0.5, *-ch;q=0", "de-ch, fr-ch", "");

        /* checking Locale.filter with *;q=0 in EXTENDED_FILTERING which
         * must make filter to return "" empty or no match
         */
        checkFilter("*-ch;q=0.5, *;q=0", "de-ch, fr-ch", "");

        /* checking Locale.filter with *;q=0.6, *-Latn;q=0 in
         * EXTENDED_FILTERING
         */
        checkFilter("*;q=0.6, *-Latn;q=0", "de-ch, hi-in, en-Latn",
                "de-CH, hi-IN");

        if (err) {
            throw new RuntimeException("[LocaleMatcher method(s) failed]");
        }

    }

    private static void checkLookup(String ranges, String tags,
            String expectedLocale) {

        List<Locale.LanguageRange> priorityList = Locale.LanguageRange
                .parse(ranges);
        List<Locale> localeList = generateLocales(tags);
        Locale loc = Locale.lookup(priorityList, localeList);
        String actualLocale
                = loc.toLanguageTag();

        if (!actualLocale.equals(expectedLocale)) {
            System.err.println("Locale.lookup failed with ranges: " + ranges
                    + " Expected: " + expectedLocale
                    + " Actual: " + actualLocale);
            err = true;
        }

    }

    private static void checkFilter(String ranges, String tags,
            String expectedLocales) {

        List<Locale.LanguageRange> priorityList = Locale.LanguageRange
                .parse(ranges);
        List<Locale> localeList = generateLocales(tags);
        String actualLocales = getLocalesAsString(
                Locale.filter(priorityList, localeList));

        if (!actualLocales.equals(expectedLocales)) {
            System.err.println("Locale.filter failed with ranges: " + ranges
                    + " Expected: " + expectedLocales
                    + " Actual: " + actualLocales);
            err = true;
        }

    }

    private static List<Locale> generateLocales(String tags) {
        if (tags == null) {
            return null;
        }

        List<Locale> localeList = new ArrayList<>();
        if (tags.equals("")) {
            return localeList;
        }
        String[] t = tags.split(", ");
        for (String tag : t) {
            localeList.add(Locale.forLanguageTag(tag));
        }
        return localeList;
    }

    private static String getLocalesAsString(List<Locale> locales) {
        StringBuilder sb = new StringBuilder();

        Iterator<Locale> itr = locales.iterator();
        if (itr.hasNext()) {
            sb.append(itr.next().toLanguageTag());
        }
        while (itr.hasNext()) {
            sb.append(", ");
            sb.append(itr.next().toLanguageTag());
        }

        return sb.toString().trim();
    }

}
