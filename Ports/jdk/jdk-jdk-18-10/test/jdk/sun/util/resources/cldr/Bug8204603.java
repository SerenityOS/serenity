/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8204603 8251317
 * @summary Test that correct data is retrieved for zh_CN and zh_TW locales
 * and CLDR provider supports all locales for which aliases exist.
 * @modules java.base/sun.util.locale.provider
 *          jdk.localedata
 * @run main Bug8204603
 */

import java.text.DateFormatSymbols;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import sun.util.locale.provider.LocaleProviderAdapter;

/**
 * This test is dependent on a particular version of CLDR data.
 */
public class Bug8204603 {

    /**
     * List of sample locales for which CLDR provides alias Mappings. e.g alias of
     * zh-HK is zh-Hant-HK
     */
    private static final List<Locale> ALIAS_LOCALES = List.of(
        Locale.forLanguageTag("az-AZ"), Locale.forLanguageTag("bs-BA"),
        Locale.forLanguageTag("ha-Latn-NG"), Locale.forLanguageTag("i-lux"),
        Locale.forLanguageTag("kk-Cyrl-KZ"), Locale.forLanguageTag("ks-Arab-IN"),
        Locale.forLanguageTag("ky-Cyrl-KG"), Locale.forLanguageTag("lb"),
        Locale.forLanguageTag("lb"), Locale.forLanguageTag("mn-Cyrl-MN"),
        Locale.forLanguageTag("mo"),
        Locale.forLanguageTag("ms-Latn-MY"),
        Locale.forLanguageTag("pa-IN"), Locale.forLanguageTag("pa-PK"),
        Locale.forLanguageTag("scc"), Locale.forLanguageTag("scr"),
        Locale.forLanguageTag("sh"), Locale.forLanguageTag("shi-MA"),
        Locale.forLanguageTag("sr-RS"),
        Locale.forLanguageTag("tl"),
        Locale.forLanguageTag("tzm-Latn-MA"), Locale.forLanguageTag("ug-Arab-CN"),
        Locale.forLanguageTag("uz-AF"), Locale.forLanguageTag("uz-UZ"),
        Locale.forLanguageTag("vai-LR"), Locale.forLanguageTag("vai-LR"),
        Locale.forLanguageTag("yue-CN"), Locale.forLanguageTag("yue-HK"),
        Locale.forLanguageTag("zh-CN"), Locale.forLanguageTag("zh-HK"),
        Locale.forLanguageTag("zh-MO"),
        Locale.forLanguageTag("zh-TW"));
    private static final Map<Locale, String> CALENDAR_DATA_MAP = Map.of(
        Locale.forLanguageTag("zh-CN"), "\u5468\u65E5",
        Locale.forLanguageTag("zh-TW"), "\u9031\u65E5");
    private static final Map<Locale, String> NAN_DATA_MAP = Map.of(
        Locale.forLanguageTag("zh-CN"), "NaN",
        Locale.forLanguageTag("zh-TW"), "\u975E\u6578\u503C");

    public static void main(String[] args) {
        testCldrSupportedLocales();
        CALENDAR_DATA_MAP.forEach(Bug8204603::testCalendarData);
        NAN_DATA_MAP.forEach(Bug8204603::testNanData);
    }

    /**
     * tests that CLDR provider should return true for alias locales.
     */
    private static void testCldrSupportedLocales() {
        LocaleProviderAdapter cldr = LocaleProviderAdapter.forType(LocaleProviderAdapter.Type.CLDR);
        Set<String> langtags = Arrays.stream(cldr.getAvailableLocales())
            .map(Locale::toLanguageTag)
            .collect(Collectors.toSet());
        ALIAS_LOCALES.stream()
            .filter(loc -> !cldr.isSupportedProviderLocale(loc, langtags))
            .findAny()
            .ifPresent(l -> {
                throw new RuntimeException("Locale " + l
                        + "  is not supported by CLDR locale provider");
            });
    }

    private static void testCalendarData(Locale loc, String expected) {
        DateFormatSymbols dfs = DateFormatSymbols.getInstance(loc);
        String[] shortDays = dfs.getShortWeekdays();
        String actual = shortDays[Calendar.SUNDAY];
        if (!actual.equals(expected)) {
            throw new RuntimeException("Calendar data mismatch for locale: "
                    + loc + ", expected  is: " + expected + ", actual is: " + actual);
        }
    }

    private static void testNanData(Locale loc, String expected) {
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(loc);
        String actual = dfs.getNaN();
        if (!actual.equals(expected)) {
            throw new RuntimeException("NaN mismatch for locale: "
                    + loc + ", expected  is: " + expected + ", actual is: " + actual);
        }
    }
}
