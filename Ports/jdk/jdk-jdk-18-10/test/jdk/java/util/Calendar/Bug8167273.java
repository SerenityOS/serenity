/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8167273 8251317 8258794
 * @summary Test
 * Era names retrieved from Calendar and DateFormatSymbols class
 * should match for default providers preference
 * as well as when  preference list is [COMPAT, CLDR],
 * Empty era names are not retrieved from DateformatSymbols class.
 * Equivalent locales specified for [zh-HK, no-NO, no] for
 * CLDR Provider works correctly.
 * Implict COMPAT Locale nb is reflected in available locales
 * for all Providers for COMPAT.
 * @modules java.base/sun.util.locale.provider
 *          java.base/sun.util.spi
 *          jdk.localedata
 * @run main/othervm -Djava.locale.providers=COMPAT,CLDR Bug8167273 testEraName
 * @run main/othervm  Bug8167273 testEraName
 * @run main/othervm -Djava.locale.providers=CLDR Bug8167273 testCldr
 * @run main/othervm  Bug8167273 testEmptyEraNames
 * @run main/othervm  -Djava.locale.providers=COMPAT Bug8167273 testCompat
 */
import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.LocaleProviderAdapter.Type;

public class Bug8167273 {

    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "testEraName":
                testEraName();
                break;
            case "testEmptyEraNames":
                testEmptyEraNames();
                break;
            case "testCldr":
                testCldrSupportedLocales();
                break;
            case "testCompat":
                testCompatSupportedLocale();
                break;
            default:
                throw new RuntimeException("no test was specified.");
        }
    }

    /**
     * tests that era names retrieved from Calendar.getDisplayNames map should
     * match with that of Era names retrieved from DateFormatSymbols.getEras()
     * method for all Gregorian Calendar locales .
     */
    public static void testEraName() {
        Set<Locale> allLocales = Set.of(Locale.getAvailableLocales());
        Set<Locale> JpThlocales = Set.of(
                new Locale("th", "TH"), Locale.forLanguageTag("th-Thai-TH"),
                new Locale("ja", "JP", "JP"), new Locale("th", "TH", "TH")
        );
        Set<Locale> allLocs = new HashSet<>(allLocales);
        // Removing Japanese and Thai Locales to check  Gregorian Calendar Locales
        allLocs.removeAll(JpThlocales);
        allLocs.forEach((locale) -> {
            Calendar cal = Calendar.getInstance(locale);
            Map<String, Integer> names = cal.getDisplayNames(Calendar.ERA, Calendar.ALL_STYLES, locale);
            DateFormatSymbols symbols = new DateFormatSymbols(locale);
            String[] eras = symbols.getEras();
            for (String era : eras) {
                if (!names.containsKey(era)) {
                    reportMismatch(names.keySet(), eras, locale);
                }
            }
        });
    }

    private static void reportMismatch(Set<String> CalendarEras, String[] dfsEras, Locale locale) {
        System.out.println("For Locale  " + locale + "era names in calendar map are  " + CalendarEras);
        for (String era : dfsEras) {
            System.out.println("For Locale  " + locale + " era names in DateFormatSymbols era array are  " + era);
        }
        throw new RuntimeException(" Era name retrieved from Calendar class do not match with"
                + " retrieved from DateFormatSymbols  for Locale   " + locale);

    }

    /**
     * tests that Eras names returned from DateFormatSymbols.getEras()
     * and Calendar.getDisplayNames() should not be empty for any Locale.
     */
    private static void testEmptyEraNames() {
        Set<Locale> allLocales = Set.of(Locale.getAvailableLocales());
        allLocales.forEach((loc) -> {
            DateFormatSymbols dfs = new DateFormatSymbols(loc);
            Calendar cal = Calendar.getInstance(loc);
            Map<String, Integer> names = cal.getDisplayNames(Calendar.ERA, Calendar.ALL_STYLES, loc);
            Set<String> CalendarEraNames = names.keySet();
            String[] eras = dfs.getEras();
            for (String era : eras) {
                if (era.isEmpty()) {
                    throw new RuntimeException("Empty era names retrieved for DateFomatSymbols.getEras"
                            + " for locale " + loc);
                }
            }
            CalendarEraNames.stream().filter((erakey) -> (erakey.isEmpty())).forEachOrdered((l) -> {
                throw new RuntimeException("Empty era names retrieved for Calendar.getDisplayName"
                        + " for locale " + loc);
            });
        });

    }

    /**
     * tests that CLDR provider should return true for locale zh_HK, no-NO and
     * no.
     */
    private static void testCldrSupportedLocales() {
        Set<Locale> locales = Set.of(Locale.forLanguageTag("zh-HK"),
                Locale.forLanguageTag("no-NO"),
                Locale.forLanguageTag("no"));
        LocaleProviderAdapter cldr = LocaleProviderAdapter.forType(Type.CLDR);
        Set<Locale> availableLocs = Set.of(cldr.getAvailableLocales());
        Set<String> langtags = new HashSet<>();
        availableLocs.forEach((loc) -> {
            langtags.add(loc.toLanguageTag());
        });

        locales.stream().filter((loc) -> (!cldr.isSupportedProviderLocale(loc, langtags))).forEachOrdered((loc) -> {
            throw new RuntimeException("Locale " + loc + "  is not supported by CLDR Locale Provider");
        });
    }

    /**
     * Tests that locale nb should be supported by JRELocaleProvider .
     */
    private static void testCompatSupportedLocale() {
        LocaleProviderAdapter jre = LocaleProviderAdapter.forJRE();
        checkPresenceCompat("BreakIteratorProvider",
                jre.getBreakIteratorProvider().getAvailableLocales());
        checkPresenceCompat("CollatorProvider",
                jre.getCollatorProvider().getAvailableLocales());
        checkPresenceCompat("DateFormatProvider",
                jre.getDateFormatProvider().getAvailableLocales());
        checkPresenceCompat("DateFormatSymbolsProvider",
                jre.getDateFormatSymbolsProvider().getAvailableLocales());
        checkPresenceCompat("DecimalFormatSymbolsProvider",
                jre.getDecimalFormatSymbolsProvider().getAvailableLocales());
        checkPresenceCompat("NumberFormatProvider",
                jre.getNumberFormatProvider().getAvailableLocales());
        checkPresenceCompat("CurrencyNameProvider",
                jre.getCurrencyNameProvider().getAvailableLocales());
        checkPresenceCompat("LocaleNameProvider",
                jre.getLocaleNameProvider().getAvailableLocales());
        checkPresenceCompat("TimeZoneNameProvider",
                jre.getTimeZoneNameProvider().getAvailableLocales());
        checkPresenceCompat("CalendarDataProvider",
                jre.getCalendarDataProvider().getAvailableLocales());
        checkPresenceCompat("CalendarNameProvider",
                jre.getCalendarNameProvider().getAvailableLocales());
        checkPresenceCompat("CalendarProvider",
                jre.getCalendarProvider().getAvailableLocales());
    }

    private static void checkPresenceCompat(String testName, Locale[] got) {
        List<Locale> gotLocalesList = Arrays.asList(got);
        Locale nb = Locale.forLanguageTag("nb");
        if (!gotLocalesList.contains(nb)) {
            throw new RuntimeException("Locale nb not supported by JREProvider for "
                    + testName + " test ");
        }
    }
}
