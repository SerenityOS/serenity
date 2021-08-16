/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8038436 8158504 8065555 8167143 8167273 8189272
 * @summary Test for changes in 8038436
 * @modules java.base/sun.util.locale.provider
 *          java.base/sun.util.spi
 *          jdk.localedata
 * @compile -XDignore.symbol.file Bug8038436.java
 * @run main/othervm  -Djava.locale.providers=COMPAT Bug8038436  availlocs
 */

import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import sun.util.locale.provider.LocaleProviderAdapter;

public class Bug8038436 {
    public static void main(String[] args) {

        switch (args[0]) {

        case "availlocs":
            availableLocalesTests();
            break;
        default:
            throw new RuntimeException("no test was specified.");
        }

    }


    static final String[] bipLocs = ("ar, ar-JO, ar-LB, ar-SY, be, be-BY, bg, " +
        "bg-BG, ca, ca-ES, cs, cs-CZ, da, da-DK, de, de-AT, de-CH, de-DE, " +
        "de-LU, el, el-CY, el-GR, en, en-AU, en-CA, en-GB, en-IE, en-IN, " +
        "en-MT, en-NZ, en-PH, en-SG, en-US, en-ZA, es, es-AR, es-BO, es-CL, " +
        "es-CO, es-CR, es-DO, es-EC, es-ES, es-GT, es-HN, es-MX, es-NI, " +
        "es-PA, es-PE, es-PR, es-PY, es-SV, es-US, es-UY, es-VE, et, et-EE, " +
        "fi, fi-FI, fr, fr-BE, fr-CA, fr-CH, fr-FR, ga, ga-IE, he, he-IL, " +
        "hi-IN, hr, hr-HR, hu, hu-HU, id, id-ID, is, is-IS, it, it-CH, it-IT, " +
        "ja, ja-JP, ko, ko-KR, lt, lt-LT, lv, lv-LV, mk, mk-MK, ms, ms-MY, mt, " +
        "mt-MT, nb, nb-NO, nl, nl-BE, nl-NL, nn-NO, no, no-NO, no-NO, pl, pl-PL, pt, pt-BR, " +
        "pt-PT, ro, ro-RO, ru, ru-RU, sk, sk-SK, sl, sl-SI, sq, sq-AL, sr, " +
        "sr-BA, sr-CS, sr-Latn, sr-Latn-ME, sr-ME, sr-RS, sv, sv-SE, th, th-TH, " +
        "tr, tr-TR, uk, uk-UA, und, vi, vi-VN, zh, zh-CN, zh-HK, zh-Hans-CN, " +
        "zh-Hans-SG, zh-Hant-HK, zh-Hant-TW, zh-SG, zh-TW, ").split(",\\s*");
    static final String[] dfpLocs = bipLocs;
    static final String[] datefspLocs = bipLocs;
    static final String[] decimalfspLocs = bipLocs;
    static final String[] calnpLocs = bipLocs;
    static final String[] cpLocs = ("ar, be, bg, ca, cs, da, el, es, et, fi, " +
        "fr, he, hi, hr, hu, is, ja, ko, lt, lv, mk, nb, nb-NO, nn-NO, no, pl, ro, ru, sk, sl, " +
        "sq, sr, sr-Latn, sv, th, tr, uk, und, vi, zh, zh-HK, zh-Hant-HK, " +
        "zh-Hant-TW, zh-TW, ").split(",\\s*");
    static final String[] nfpLocs = ("ar, ar-AE, ar-BH, ar-DZ, ar-EG, ar-IQ, " +
        "ar-JO, ar-KW, ar-LB, ar-LY, ar-MA, ar-OM, ar-QA, ar-SA, ar-SD, ar-SY, " +
        "ar-TN, ar-YE, be, be-BY, bg, bg-BG, ca, ca-ES, cs, cs-CZ, da, da-DK, " +
        "de, de-AT, de-CH, de-DE, de-LU, el, el-CY, el-GR, en, en-AU, " +
        "en-CA, en-GB, en-IE, en-IN, en-MT, en-NZ, en-PH, en-SG, en-US, en-ZA, " +
        "es, es-AR, es-BO, es-CL, es-CO, es-CR, es-CU, es-DO, es-EC, es-ES, " +
        "es-GT, es-HN, es-MX, es-NI, es-PA, es-PE, es-PR, es-PY, es-SV, es-US, " +
        "es-UY, es-VE, et, et-EE, fi, fi-FI, fr, fr-BE, fr-CA, fr-CH, fr-FR, " +
        "fr-LU, ga, ga-IE, he, he-IL, hi, hi-IN, hr, hr-HR, hu, hu-HU, id, " +
        "id-ID, is, is-IS, it, it-CH, it-IT, ja, ja-JP, " +
        "ja-JP-u-ca-japanese-x-lvariant-JP, ko, ko-KR, lt, lt-LT, lv, lv-LV, " +
        "mk, mk-MK, ms, ms-MY, mt, mt-MT, nb, nb-NO, nl, nl-BE, nl-NL, nn-NO, " +
        "nn-NO, no, no-NO, pl, pl-PL, pt, pt-BR, pt-PT, ro, ro-RO, ru, ru-RU, " +
        "sk, sk-SK, sl, sl-SI, sq, sq-AL, sr, sr-BA, sr-CS, sr-Latn, " +
        "sr-Latn-BA, sr-Latn-ME, sr-Latn-RS, sr-ME, sr-RS, sv, sv-SE, th, " +
        "th-TH, th-TH-u-nu-thai-x-lvariant-TH, tr, tr-TR, uk, uk-UA, und, vi, " +
        "vi-VN, zh, zh-CN, zh-HK, zh-Hans-CN, zh-Hans-SG, zh-Hant-HK, " +
        "zh-Hant-TW, zh-SG, zh-TW, ").split(",\\s*");
    static final String[] currencynpLocs = ("ar-AE, ar-BH, ar-DZ, ar-EG, ar-IQ, " +
        "ar-JO, ar-KW, ar-LB, ar-LY, ar-MA, ar-OM, ar-QA, ar-SA, ar-SD, ar-SY, " +
        "ar-TN, ar-YE, be-BY, bg-BG, ca-ES, cs-CZ, da-DK, de, de-AT, de-CH, " +
        "de-DE, de-LU, el-CY, el-GR, en-AU, en-CA, en-GB, en-IE, en-IN, " +
        "en-MT, en-NZ, en-PH, en-SG, en-US, en-ZA, es, es-AR, es-BO, es-CL, " +
        "es-CO, es-CR, es-CU, es-DO, es-EC, es-ES, es-GT, es-HN, es-MX, es-NI, " +
        "es-PA, es-PE, es-PR, es-PY, es-SV, es-US, es-UY, es-VE, et-EE, fi-FI, " +
        "fr, fr-BE, fr-CA, fr-CH, fr-FR, fr-LU, ga-IE, he-IL, hi-IN, hr-HR, " +
        "hu-HU, id-ID, is-IS, it, it-CH, it-IT, ja, ja-JP, ko, ko-KR, lt-LT, " +
        "lv-LV, mk-MK, ms-MY, mt-MT, nb,  nb-NO, nl-BE, nl-NL, nn-NO, no-NO, pl-PL, pt, pt-BR, " +
        "pt-PT, ro-RO, ru-RU, sk-SK, sl-SI, sq-AL, sr-BA, sr-CS, sr-Latn-BA, " +
        "sr-Latn-ME, sr-Latn-RS, sr-ME, sr-RS, sv, sv-SE, th-TH, tr-TR, uk-UA, " +
        "und, vi-VN, zh-CN, zh-HK, zh-Hans-CN, zh-Hans-SG, zh-Hant-HK, " +
        "zh-Hant-TW, zh-SG, zh-TW, ").split(",\\s*");
    static final String[] lnpLocs = ("ar, be, bg, ca, cs, da, de, el, el-CY, " +
        "en, en-MT, en-PH, en-SG, es, es-US, et, fi, fr, ga, he, hi, hr, hu, " +
        "id, is, it, ja, ko, lt, lv, mk, ms, mt, nb, nb-NO, nl, nn-NO, no, no-NO, pl, pt, pt-BR, " +
        "pt-PT, ro, ru, sk, sl, sq, sr, sr-Latn, sv, th, tr, uk, und, vi, zh, " +
        "zh-HK, zh-Hans-SG, zh-Hant-HK, zh-Hant-TW, zh-SG, zh-TW, ").split(",\\s*");
    static final String[] tznpLocs = ("de, en, en-CA, en-GB, en-IE, es, fr, hi, " +
        "it, ja, ko, nb,  nb-NO, nn-NO, pt-BR, sv, und, zh-CN, zh-HK, zh-Hans-CN, zh-Hant-HK, " +
        "zh-Hant-TW, zh-TW, ").split(",\\s*");
    static final String[] caldpLocs = ("ar, be, bg, ca, cs, da, de, el, el-CY, " +
        "en, en-GB, en-IE, en-MT, es, es-ES, es-US, et, fi, fr, fr-CA, he, hi, " +
        "hr, hu, id-ID, is, it, ja, ko, lt, lv, mk, ms-MY, mt, mt-MT, nb, nb-NO, nl, nn-NO, no, " +
        "pl, pt, pt-BR, pt-PT, ro, ru, sk, sl, sq, sr, sr-Latn-BA, sr-Latn-ME, " +
        "sr-Latn-RS, sv, th, tr, uk, und, vi, zh, ").split(",\\s*");
    static final String[] calpLocs = caldpLocs;

    /*
     * Validate whether JRE's *Providers return supported locales list based on
     * their actual resource bundle exsistence. The above golden data
     * are manually extracted, so they need to be updated if new locale
     * data resource bundle were added.
     */
    private static void availableLocalesTests() {
        LocaleProviderAdapter jre = LocaleProviderAdapter.forJRE();

        checkAvailableLocales("BreakIteratorProvider",
            jre.getBreakIteratorProvider().getAvailableLocales(), bipLocs);
        checkAvailableLocales("CollatorProvider",
            jre.getCollatorProvider().getAvailableLocales(), cpLocs);
        checkAvailableLocales("DateFormatProvider",
            jre.getDateFormatProvider().getAvailableLocales(), dfpLocs);
        checkAvailableLocales("DateFormatSymbolsProvider",
            jre.getDateFormatSymbolsProvider().getAvailableLocales(), datefspLocs);
        checkAvailableLocales("DecimalFormatSymbolsProvider",
            jre.getDecimalFormatSymbolsProvider().getAvailableLocales(), decimalfspLocs);
        checkAvailableLocales("NumberFormatProvider",
            jre.getNumberFormatProvider().getAvailableLocales(), nfpLocs);
        checkAvailableLocales("CurrencyNameProvider",
            jre.getCurrencyNameProvider().getAvailableLocales(), currencynpLocs);
        checkAvailableLocales("LocaleNameProvider",
            jre.getLocaleNameProvider().getAvailableLocales(), lnpLocs);
        checkAvailableLocales("TimeZoneNameProvider",
            jre.getTimeZoneNameProvider().getAvailableLocales(), tznpLocs);
        checkAvailableLocales("CalendarDataProvider",
            jre.getCalendarDataProvider().getAvailableLocales(), caldpLocs);
        checkAvailableLocales("CalendarNameProvider",
            jre.getCalendarNameProvider().getAvailableLocales(), calnpLocs);
        checkAvailableLocales("CalendarProvider",
            jre.getCalendarProvider().getAvailableLocales(), calpLocs);
    }

    private static void checkAvailableLocales(String testName, Locale[] got, String[] expected) {
        System.out.println("Testing available locales for " + testName);
        List<Locale> gotList = Arrays.asList(got).stream()
            .map(Locale::toLanguageTag)
            .sorted()
            .map(Locale::forLanguageTag)
            .collect(Collectors.toList());
        List<Locale> expectedList = Arrays.asList(expected).stream()
            .map(Locale::forLanguageTag)
            .collect(Collectors.toList());

        if (!gotList.equals(expectedList)) {
            throw new RuntimeException("\n" + gotList.toString() + "\n is not equal to \n" +
                                       expectedList.toString());
        }
    }
}
