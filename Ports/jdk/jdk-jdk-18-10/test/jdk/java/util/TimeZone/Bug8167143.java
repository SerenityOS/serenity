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
 * @bug 8167143
 * @summary Test
 * Timezone parsing works for all locales for default providers prefernce
 * as well as when  prefernce list is [COMPAT, CLDR],
 * CLDR implict locales are correctly reflected,
 * th_TH bundle is not wrongly cached in DateFormatSymbols,
 * correct candidate locale list is retrieved for
 * zh_Hant and zh_Hans and
 * Implict COMPAT Locales nn-NO, nb-NO are reflected in available locales
 * for all Providers for COMPAT.
 * @modules java.base/sun.util.locale.provider
 *          java.base/sun.util.spi
 *          jdk.localedata
 * @run main/othervm -Djava.locale.providers=COMPAT,CLDR Bug8167143 testTimeZone
 * @run main/othervm  Bug8167143 testTimeZone
 * @run main/othervm -Djava.locale.providers=CLDR Bug8167143 testCldr
 * @run main/othervm  Bug8167143 testCache
 * @run main/othervm  Bug8167143 testCandidateLocales
 * @run main/othervm  -Djava.locale.providers=COMPAT Bug8167143 testCompat
 */
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;

import sun.util.locale.provider.LocaleProviderAdapter;
import sun.util.locale.provider.LocaleProviderAdapter.Type;

public class Bug8167143 {

    private static final TimeZone REYKJAVIK = TimeZone.getTimeZone("Atlantic/Reykjavik");
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    private static final List<Locale> CLDR_IMPLICIT_LOCS = List.of(Locale.forLanguageTag("zh-Hans-CN"),
            Locale.forLanguageTag("zh-Hans-SG"),
            Locale.forLanguageTag("zh-Hant-HK"),
            Locale.forLanguageTag("zh-Hant-TW"),
            Locale.forLanguageTag("zh-Hant-MO"));

    private static final List<Locale> COMPAT_IMPLICIT_LOCS = List.of(Locale.forLanguageTag("nn-NO"),
            Locale.forLanguageTag("nb-NO"));
    /**
     * List of candidate locales for zh_Hant
     */
    private static final List<Locale> ZH_HANT_CANDLOCS = List.of(
            Locale.forLanguageTag("zh-Hant"),
            Locale.forLanguageTag("zh-TW"),
            Locale.forLanguageTag("zh"),
            Locale.ROOT);
    /**
     * List of candidate locales for zh_Hans
     */
    private static final List<Locale> ZH_HANS_CANDLOCS = List.of(
            Locale.forLanguageTag("zh-Hans"),
            Locale.forLanguageTag("zh-CN"),
            Locale.forLanguageTag("zh"),
            Locale.ROOT);

    public static void main(String[] args) {
        switch (args[0]) {
            case "testTimeZone":
                testTimeZoneParsing();
                break;
            case "testCldr":
                testImplicitCldrLocales();
                break;
            case "testCache":
                testDateFormatSymbolsCache();
                break;
            case "testCandidateLocales":
                testCandidateLocales();
                break;
            case "testCompat":
                testImplicitCompatLocales();
                break;
            default:
                throw new RuntimeException("no test was specified.");
        }
    }

    /**
     * Check that if Locale Provider Preference list is Default, or if Locale
     * Provider Preference List is COMPAT,CLDR SimplDateFormat parsing works for
     * all Available Locales.
     */
    private static void testTimeZoneParsing() {
        Set<Locale> locales = Set.of(Locale.forLanguageTag("zh-hant"), new Locale("no", "NO", "NY"));
        // Set<Locale> locales = Set.of(Locale.getAvailableLocales());
        locales.forEach((locale) -> {
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd z", locale);
            for (final TimeZone tz : new TimeZone[]{REYKJAVIK, GMT, NEW_YORK}) {
                try {
                    sdf.parse("2000/02/10 " + tz.getDisplayName(locale));
                } catch (ParseException e) {
                    throw new RuntimeException("TimeZone Parsing failed with Locale "
                            + locale + " for TimeZone  " + tz.getDisplayName(), e);
                }
            }
        });
    }

    /**
     * Check that locales implicitly supported from CLDR are reflected in output
     * from getAvailbleLocales() for each bundle.
     *
     */
    private static void testImplicitCldrLocales() {
        LocaleProviderAdapter cldr = LocaleProviderAdapter.forType(Type.CLDR);
        checkPresenceCldr("CurrencyNameProvider",
                cldr.getCurrencyNameProvider().getAvailableLocales());
        checkPresenceCldr("LocaleNameProvider",
                cldr.getLocaleNameProvider().getAvailableLocales());
        checkPresenceCldr("TimeZoneNameProvider",
                cldr.getTimeZoneNameProvider().getAvailableLocales());
        checkPresenceCldr("CalendarDataProvider",
                cldr.getCalendarDataProvider().getAvailableLocales());
        checkPresenceCldr("CalendarNameProvider",
                cldr.getCalendarProvider().getAvailableLocales());
    }

    private static void checkPresenceCldr(String testName, Locale[] got) {
        List<Locale> gotLocalesList = Arrays.asList(got);
        List<Locale> gotList = new ArrayList<>(gotLocalesList);
        if (!testName.equals("TimeZoneNameProvider")) {
            if (!gotList.removeAll(CLDR_IMPLICIT_LOCS)) {
                // check which locale are not present in retrievedLocales List.
                List<Locale> expectedLocales = new ArrayList<>(CLDR_IMPLICIT_LOCS);
                expectedLocales.removeAll(gotList);
                throw new RuntimeException("Locales those not correctly reflected are "
                        + expectedLocales + " for test " + testName);
            }
        } else {
            // check one extra locale zh_HK for TimeZoneNameProvider
            Locale zh_HK = Locale.forLanguageTag("zh-HK");
            if (!gotList.removeAll(CLDR_IMPLICIT_LOCS) && gotList.remove(zh_HK)) {
                //check which locale are not present in retrievedLocales List
                List<Locale> expectedLocales = new ArrayList<>(CLDR_IMPLICIT_LOCS);
                expectedLocales.add(zh_HK);
                expectedLocales.removeAll(gotList);
                throw new RuntimeException("Locales those not correctly reflected are "
                        + expectedLocales + " for test " + testName);
            }
        }
    }

    /**
     * Check that if Locale Provider Preference list is default and if
     * SimpleDateFormat instance for th-TH-TH is created first, then JRE bundle
     * for th-TH should not be cached in cache of DateFormatSymbols class.
     */
    private static void testDateFormatSymbolsCache() {
        Locale th_TH_TH = new Locale("th", "TH", "TH");
        Locale th_TH = new Locale("th", "TH");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd z", th_TH_TH);
        String[][] thTHTHZoneStrings = sdf.getDateFormatSymbols().getZoneStrings();
        String[][] thTHZoneStrings = sdf.getDateFormatSymbols().getZoneStrings();
        if (Arrays.equals(thTHTHZoneStrings, thTHZoneStrings)) {
            throw new RuntimeException("th_TH bundle still cached with DateFormatSymbols"
                    + "cache for locale  " + th_TH
            );
        }
    }

    /**
     * Check that candidate locales list retrieved for zh__Hant and for zh__Hans
     * do not have first candidate locale as zh_TW_Hant and zh_CN_Hans
     * respectively.
     */
    private static void testCandidateLocales() {
        ResourceBundle.Control Control = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_DEFAULT);
        Locale zh_Hant = Locale.forLanguageTag("zh-Hant");
        Locale zh_Hans = Locale.forLanguageTag("zh-Hans");
        List<Locale> zhHantCandidateLocs = Control.getCandidateLocales("", zh_Hant);
        List<Locale> zhHansCandidateLocs = Control.getCandidateLocales("", zh_Hans);
        if (!zhHantCandidateLocs.equals(ZH_HANT_CANDLOCS)) {
            reportDifference(zhHantCandidateLocs, ZH_HANT_CANDLOCS, "zh_Hant");

        }
        if (!zhHansCandidateLocs.equals(ZH_HANS_CANDLOCS)) {
            reportDifference(zhHansCandidateLocs, ZH_HANS_CANDLOCS, "zh_Hans");

        }
    }

    private static void reportDifference(List<Locale> got, List<Locale> expected, String locale) {
        List<Locale> retrievedList = new ArrayList<>(got);
        List<Locale> expectedList = new ArrayList<>(expected);
        retrievedList.removeAll(expectedList);
        expectedList.removeAll(retrievedList);
        if ((retrievedList.size() > 0) && (expectedList.size() > 0)) {
            throw new RuntimeException(" retrievedList contain extra candidate locales " + retrievedList
                    + " and missing candidate locales " + expectedList
                    + "for locale " + locale);
        }
        if ((retrievedList.size() > 0)) {
            throw new RuntimeException(" retrievedList contain extra candidate locales " + retrievedList
                    + "for locale " + locale);
        }
        if ((expectedList.size() > 0)) {
            throw new RuntimeException(" retrievedList contain extra candidate locales " + expectedList
                    + "for locale " + locale);
        }
    }

    /**
     * checks that locales nn-NO  and nb-NO should be present in list of supported locales for
     * all Providers for COMPAT.
     */
    private static void testImplicitCompatLocales() {
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
        List<Locale> gotList = new ArrayList<>(gotLocalesList);
            if (!gotList.removeAll(COMPAT_IMPLICIT_LOCS)) {
                // check which Implicit locale are not present in retrievedLocales List.
                List<Locale> implicitLocales = new ArrayList<>(COMPAT_IMPLICIT_LOCS);
                implicitLocales.removeAll(gotList);
                throw new RuntimeException("Locales those not correctly reflected are "
                        + implicitLocales + " for test " + testName);
            }
    }
}
