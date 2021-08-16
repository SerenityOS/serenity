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
 * @bug 8159420
 * @summary Checks the proper execution of LanguageRange.parse() and
 *          other LocaleMatcher methods when used in the locales like
 *          Turkish, because the toLowerCase() method is invoked in the
 *          parse() and other LocaleMatcher methods.
 *          e.g. "HI-Deva".toLowerCase() in the Turkish locale returns
 *          "hı-deva", where 'ı' is the LATIN SMALL LETTER DOTLESS I character
 *          which is not allowed in the language ranges/tags.
 * @compile -encoding utf-8 Bug8159420.java
 * @run main Bug8159420
 */

import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;
import java.util.Locale.FilteringMode;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import static java.util.Locale.FilteringMode.EXTENDED_FILTERING;
import static java.util.Locale.FilteringMode.AUTOSELECT_FILTERING;

public class Bug8159420 {

    static boolean err = false;

    public static void main(String[] args) {

        Locale origLocale = null;
        try {

            origLocale = Locale.getDefault();
            Locale.setDefault(new Locale("tr", "TR"));
            testParse();
            testFilter(EXTENDED_FILTERING);
            testFilter(AUTOSELECT_FILTERING);
            testLookup();
            testMapEquivalents();

            if (err) {
                throw new RuntimeException("[LocaleMatcher method(s) in turkish"
                        + " locale failed]");
            }

        } finally {
            Locale.setDefault(origLocale);
        }

    }

    /* Before the fix, the testParse() method was throwing
     * IllegalArgumentException in Turkish Locale
     */
    private static void testParse() {
        String ranges = "HI-Deva, ja-hIrA-JP, RKI";
        try {
            LanguageRange.parse(ranges);
        } catch (Exception ex) {
            System.err.println("[testParse() failed on range string: "
                    + ranges + "] due to "+ex);
            err = true;
        }
    }

    /* Before the fix, the testFilter() method was returning empty list in
     * Turkish Locale
     */
    private static void testFilter(FilteringMode mode) {

        String ranges = "hi-IN, itc-Ital";
        String tags = "hi-IN, itc-Ital";
        List<LanguageRange> priorityList = LanguageRange.parse(ranges);
        List<Locale> tagList = generateLocales(tags);
        String actualLocales = showLocales(Locale.filter(priorityList, tagList, mode));
        String expectedLocales = "hi-IN, itc-Ital";

        if (!expectedLocales.equals(actualLocales)) {
            System.err.println("testFilter(" + mode + ") failed on language ranges:"
                    + " [" + ranges + "] and language tags: [" + tags + "]");
            err = true;
        }
    }

    /* Before the fix, the testLookup() method was returning null in Turkish
     * Locale
     */
    private static void testLookup() {
        boolean error = false;
        String ranges = "hi-IN, itc-Ital";
        String tags = "hi-IN, itc-Ital";
        List<LanguageRange> priorityList = LanguageRange.parse(ranges);
        List<Locale> localeList = generateLocales(tags);
        Locale actualLocale
                = Locale.lookup(priorityList, localeList);
        String actualLocaleString = "";

        if (actualLocale != null) {
            actualLocaleString = actualLocale.toLanguageTag();
        } else {
            error = true;
        }

        String expectedLocale = "hi-IN";

        if (!expectedLocale.equals(actualLocaleString)) {
            error = true;
        }

        if (error) {
            System.err.println("testLookup() failed on language ranges:"
                    + " [" + ranges + "] and language tags: [" + tags + "]");
            err = true;
        }

    }

    /* Before the fix, testMapEquivalents() method was returning only "hi-in"
     * in Turkish Locale
     */
    private static void testMapEquivalents() {

        String ranges = "HI-IN";
        List<LanguageRange> priorityList = LanguageRange.parse(ranges);
        HashMap<String, List<String>> map = new LinkedHashMap<>();
        List<String> equivalentList = new ArrayList<>();
        equivalentList.add("HI");
        equivalentList.add("HI-Deva");
        map.put("HI", equivalentList);

        List<LanguageRange> expected = new ArrayList<>();
        expected.add(new LanguageRange("hi-in"));
        expected.add(new LanguageRange("hi-deva-in"));
        List<LanguageRange> got
                = LanguageRange.mapEquivalents(priorityList, map);

        if (!areEqual(expected, got)) {
            System.err.println("testMapEquivalents() failed");
            err = true;
        }

    }

    private static boolean areEqual(List<LanguageRange> expected,
            List<LanguageRange> got) {

        boolean error = false;
        if (expected.equals(got)) {
            return !error;
        }

        List<LanguageRange> cloneExpected = new ArrayList<>(expected);
        cloneExpected.removeAll(got);
        if (!cloneExpected.isEmpty()) {
            error = true;
            System.err.println("Found missing range(s): " + cloneExpected);
        }

        // not creating the 'got' clone as the list will not be used after this
        got.removeAll(expected);
        if (!got.isEmpty()) {
            error = true;
            System.err.println("Found extra range(s): " + got);
        }
        return !error;
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

    private static String showLocales(List<Locale> locales) {
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
