/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8001562
 * @summary Verify that getAvailableLocales() in locale sensitive services
 *     classes return compatible set of locales as in JDK7.
 * @modules jdk.localedata
 * @run main Bug8001562
 */

import java.text.BreakIterator;
import java.text.Collator;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class Bug8001562 {

    static final List<String> jdk7availTags = List.of(
            "ar", "ar-AE", "ar-BH", "ar-DZ", "ar-EG", "ar-IQ", "ar-JO", "ar-KW",
            "ar-LB", "ar-LY", "ar-MA", "ar-OM", "ar-QA", "ar-SA", "ar-SD", "ar-SY",
            "ar-TN", "ar-YE", "be", "be-BY", "bg", "bg-BG", "ca", "ca-ES", "cs",
            "cs-CZ", "da", "da-DK", "de", "de-AT", "de-CH", "de-DE", "de-LU", "el",
            "el-CY", "el-GR", "en", "en-AU", "en-CA", "en-GB", "en-IE", "en-IN",
            "en-MT", "en-NZ", "en-PH", "en-SG", "en-US", "en-ZA", "es", "es-AR",
            "es-BO", "es-CL", "es-CO", "es-CR", "es-DO", "es-EC", "es-ES", "es-GT",
            "es-HN", "es-MX", "es-NI", "es-PA", "es-PE", "es-PR", "es-PY", "es-SV",
            "es-US", "es-UY", "es-VE", "et", "et-EE", "fi", "fi-FI", "fr", "fr-BE",
            "fr-CA", "fr-CH", "fr-FR", "fr-LU", "ga", "ga-IE", "he", "he-IL",
            "hi-IN", "hr", "hr-HR", "hu", "hu-HU", "id", "id-ID", "is", "is-IS",
            "it", "it-CH", "it-IT", "ja", "ja-JP",
            "ja-JP-u-ca-japanese-x-lvariant-JP", "ko", "ko-KR", "lt", "lt-LT", "lv",
            "lv-LV", "mk", "mk-MK", "ms", "ms-MY", "mt", "mt-MT", "nl", "nl-BE",
            "nl-NL", "no", "no-NO", "no-NO-x-lvariant-NY", "pl", "pl-PL", "pt",
            "pt-BR", "pt-PT", "ro", "ro-RO", "ru", "ru-RU", "sk", "sk-SK", "sl",
            "sl-SI", "sq", "sq-AL", "sr", "sr-BA", "sr-CS", "sr-Latn", "sr-Latn-BA",
            "sr-Latn-ME", "sr-Latn-RS", "sr-ME", "sr-RS", "sv", "sv-SE", "th",
            "th-TH", "th-TH-u-nu-thai-x-lvariant-TH", "tr", "tr-TR", "uk", "uk-UA",
            "vi", "vi-VN", "zh", "zh-CN", "zh-HK", "zh-SG", "zh-TW");
    static List<Locale> jdk7availLocs;

    static {
        jdk7availLocs = jdk7availTags.stream()
                .map(Locale::forLanguageTag)
                .collect(Collectors.toList());
    }

    public static void main(String[] args) {
        List<Locale> avail = Arrays.asList(BreakIterator.getAvailableLocales());
        diffLocale(BreakIterator.class, avail);

        avail = Arrays.asList(Collator.getAvailableLocales());
        diffLocale(Collator.class, avail);

        avail = Arrays.asList(DateFormat.getAvailableLocales());
        diffLocale(DateFormat.class, avail);

        avail = Arrays.asList(DateFormatSymbols.getAvailableLocales());
        diffLocale(DateFormatSymbols.class, avail);

        avail = Arrays.asList(DecimalFormatSymbols.getAvailableLocales());
        diffLocale(DecimalFormatSymbols.class, avail);

        avail = Arrays.asList(NumberFormat.getAvailableLocales());
        diffLocale(NumberFormat.class, avail);

        avail = Arrays.asList(Locale.getAvailableLocales());
        diffLocale(Locale.class, avail);
    }

    static void diffLocale(Class<?> c, List<Locale> locs) {
        String diff = "";

        System.out.printf("Only in target locales (%s.getAvailableLocales()): ", c.getSimpleName());
        for (Locale l : locs) {
            if (!jdk7availLocs.contains(l)) {
                diff += "\"" + l.toLanguageTag() + "\", ";
            }
        }
        System.out.println(diff);
        diff = "";

        System.out.printf("Only in JDK7 (%s.getAvailableLocales()): ", c.getSimpleName());
        for (Locale l : jdk7availLocs) {
            if (!locs.contains(l)) {
                diff += "\"" + l.toLanguageTag() + "\", ";
            }
        }
        System.out.println(diff);

        if (diff.length() > 0) {
            throw new RuntimeException("Above locale(s) were not included in the target available locales");
        }
    }
}
