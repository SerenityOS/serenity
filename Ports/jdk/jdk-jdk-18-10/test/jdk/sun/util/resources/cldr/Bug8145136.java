/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8145136 8202537 8221432 8251317 8258794
 * @modules jdk.localedata
 * @summary Tests LikelySubtags is correctly reflected in JDK.
 * @run main/othervm -Djava.locale.providers=CLDR Bug8145136
 */
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class Bug8145136 {

    public static void main(String[] args) {
        /* This golden data is names of all *.xml files which contain locale
         * specific data in CLDR 39. With LikelySubtags,
         * these locales should be present in output of getAvailableLocales()
         * method.
         *
         * Note that language tags listed here do not follow legacy language
         * codes, i.e., he/yi/id. They are mapped to iw/ji/in, respectively,
         * on Locale instantiation.
         */
        List<String> likelySubtagsLocales = List.of("", "af-NA", "af", "af-ZA",
            "agq-CM", "agq", "ak-GH", "ak", "am-ET", "am", "ar-001", "ar-AE",
            "ar-BH", "ar-DJ", "ar-DZ", "ar-EG", "ar-EH", "ar-ER", "ar-IL",
            "ar-IQ", "ar-JO", "ar-KM", "ar-KW", "ar-LB", "ar-LY", "ar-MA",
            "ar-MR", "ar-OM", "ar-PS", "ar-QA", "ar-SA", "ar-SD", "ar-SO",
            "ar-SS", "ar-SY", "ar-TD", "ar-TN", "ar", "ar-YE", "asa-TZ",
            "asa", "as-IN", "ast-ES", "ast", "as", "az-Cyrl-AZ", "az-Cyrl",
            "az-Latn-AZ", "az-Latn", "az", "bas-CM", "bas", "be-BY", "bem",
            "bem-ZM", "be", "bez-TZ", "bez", "bg-BG", "bg", "bm-ML", "bm",
            "bn-BD", "bn-IN", "bn", "bo-CN", "bo-IN", "bo", "br-FR", "brx-IN",
            "br", "brx", "bs-Cyrl-BA", "bs-Cyrl", "bs-Latn-BA", "bs-Latn",
            "bs", "ca-AD", "ca-ES-VALENCIA", "ca-ES", "ca-FR", "ca-IT", "ca",
            "ccp-BD", "ccp-IN", "ccp", "ceb-PH", "ceb",
            "ce-RU", "ce", "cgg-UG", "cgg", "chr-US", "chr", "ckb-IQ", "ckb-IR",
            "ckb", "cs-CZ", "cs", "cy-GB", "cy", "da-DK",
            "da-GL", "dav-KE", "dav", "da", "de-AT", "de-BE", "de-CH", "de-DE", "de-IT",
            "de-LI", "de-LU", "de", "dje-NE", "dje", "doi-IN", "doi", "dsb-DE", "dsb", "dua-CM",
            "dua", "dyo-SN", "dyo", "dz-BT", "dz", "ebu-KE", "ebu", "ee-GH",
            "ee-TG", "ee", "el-CY", "el-GR", "el", "en-001", "en-150", "en-AE", "en-AG",
            "en-AI", "en-AS", "en-AT", "en-AU", "en-BB", "en-BE", "en-BI",
            "en-BM", "en-BS", "en-BW", "en-BZ", "en-CA", "en-CC", "en-CH",
            "en-CK", "en-CM", "en-CX", "en-CY", "en-DE", "en-DG", "en-DK",
            "en-DM", "en-ER", "en-FI", "en-FJ", "en-FK", "en-FM", "en-GB",
            "en-GD", "en-GG", "en-GH", "en-GI", "en-GM", "en-GU", "en-GY",
            "en-HK", "en-IE", "en-IL", "en-IM", "en-IN", "en-IO", "en-JE",
            "en-JM", "en-KE", "en-KI", "en-KN", "en-KY", "en-LC", "en-LR",
            "en-LS", "en-MG", "en-MH", "en-MO", "en-MP", "en-MS", "en-MT",
            "en-MU", "en-MW", "en-MY", "en-NA", "en-NF", "en-NG", "en-NL",
            "en-NR", "en-NU", "en-NZ", "en-PG", "en-PH", "en-PK", "en-PN",
            "en-PR", "en-PW", "en-RW", "en-SB", "en-SC", "en-SD", "en-SE",
            "en-SG", "en-SH", "en-SI", "en-SL", "en-SS", "en-SX", "en-SZ",
            "en-TC", "en-TK", "en-TO", "en-TT", "en-TV", "en-TZ", "en-UG",
            "en-UM", "en-US-POSIX", "en-US", "en-VC", "en-VG", "en-VI",
            "en-VU", "en-WS", "en", "en-ZA", "en-ZM", "en-ZW", "eo-001",
            "eo", "es-419", "es-AR", "es-BO", "es-BR", "es-BZ", "es-CL", "es-CO",
            "es-CR", "es-CU", "es-DO", "es-EA", "es-EC", "es-ES", "es-GQ",
            "es-GT", "es-HN", "es-IC", "es-MX", "es-NI", "es-PA", "es-PE",
            "es-PH", "es-PR", "es-PY", "es-SV", "es-US", "es-UY", "es-VE",
            "es", "et-EE", "et", "eu-ES", "eu", "ewo-CM", "ewo", "fa-AF",
            "fa-IR", "fa", "ff-Latn-BF", "ff-Latn-CM", "ff-Latn-GH", "ff-Latn-GM",
            "ff-Latn-GN", "ff-Latn-GW", "ff-Latn-LR", "ff-Latn-MR", "ff-Latn-NE",
            "ff-Latn-NG", "ff-Latn-SL", "ff-Latn-SN", "ff-Latn", "ff", "fi-FI",
            "fil-PH", "fil", "fi", "fo-DK", "fo-FO", "fo", "fr-BE", "fr-BF",
            "fr-BI", "fr-BJ", "fr-BL", "fr-CA", "fr-CD", "fr-CF", "fr-CG",
            "fr-CH", "fr-CI", "fr-CM", "fr-DJ", "fr-DZ", "fr-FR", "fr-GA",
            "fr-GF", "fr-GN", "fr-GP", "fr-GQ", "fr-HT", "fr-KM", "fr-LU",
            "fr-MA", "fr-MC", "fr-MF", "fr-MG", "fr-ML", "fr-MQ", "fr-MR",
            "fr-MU", "fr-NC", "fr-NE", "fr-PF", "fr-PM", "fr-RE", "fr-RW",
            "fr-SC", "fr-SN", "fr-SY", "fr-TD", "fr-TG", "fr-TN", "fr-VU",
            "fr-WF", "fr", "fr-YT", "fur-IT", "fur", "fy-NL", "fy", "ga-IE",
            "ga", "gd-GB", "gd", "gl-ES", "gl", "gsw-CH", "gsw-FR", "gsw-LI",
            "gsw", "gu-IN", "gu", "guz-KE", "guz", "gv-IM", "gv", "ha-GH", "ha-NE",
            "ha-NG", "haw-US", "haw", "ha", "he-IL", "he", "hi-IN", "hi", "hr-BA",
            "hr-HR", "hr", "hsb-DE", "hsb", "hu-HU", "hu", "hy-AM", "hy", "ia-001", "ia", "id-ID",
            "id", "ig-NG", "ig", "ii-CN", "ii", "is-IS", "is", "it-CH", "it-IT",
            "it-SM", "it-VA", "it", "ja-JP", "ja", "jgo-CM", "jgo", "jmc-TZ", "jmc", "jv-ID", "jv", "kab-DZ",
            "kab", "ka-GE", "kam-KE", "kam", "ka", "kde-TZ", "kde", "kea-CV", "kea",
            "khq-ML", "khq", "ki-KE", "ki", "kkj-CM", "kkj", "kk-KZ", "kk", "kl-GL",
            "kln-KE", "kln", "kl", "km-KH", "km", "kn-IN", "kn", "kok-IN", "ko-KP",
            "ko-KR", "kok", "ko", "ksb-TZ", "ksb", "ksf-CM", "ksf", "ksh-DE", "ksh",
            "ks-IN", "ks", "ku-TR", "ku", "kw-GB", "kw", "ky-KG", "ky", "lag-TZ", "lag", "lb-LU",
            "lb", "lg-UG", "lg", "lkt-US", "lkt", "ln-AO", "ln-CD", "ln-CF", "ln-CG",
            "ln", "lo-LA", "lo", "lrc-IQ", "lrc-IR", "lrc", "lt-LT", "lt", "lu-CD",
            "luo-KE", "luo", "lu", "luy-KE", "luy", "lv-LV", "lv", "mas-KE", "mas-TZ",
            "mas", "mer-KE", "mer", "mfe-MU", "mfe", "mgh-MZ", "mgh", "mg-MG", "mgo-CM",
            "mgo", "mg", "mi-NZ", "mi", "mk-MK", "mk", "ml-IN", "ml", "mn-MN", "mn", "mr-IN", "mr",
            "ms-BN", "ms-MY", "ms-SG", "ms", "mt-MT", "mt", "mua-CM", "mua", "my-MM",
            "my", "mzn-IR", "mzn", "naq-NA", "naq", "no-NO", "no", "nds-DE", "nds-NL", "nds", "nd",
            "nd-ZW", "ne-IN", "ne-NP", "ne", "nl-AW", "nl-BE", "nl-BQ", "nl-CW",
            "nl-NL", "nl-SR", "nl-SX", "nl", "nmg-CM", "nmg", "nnh-CM", "nnh",
            "nn-NO", "nn", "nus-SS", "nus", "nyn-UG", "nyn", "om-ET", "om-KE",
            "om", "or-IN", "or", "os-GE", "os-RU", "os", "pa-Arab-PK", "pa-Arab",
            "pa-Guru-IN", "pa-Guru", "pa", "pl-PL", "pl", "ps-AF", "ps-PK",
            "ps", "pt-AO", "pt-BR", "pt-CH", "pt-CV", "pt-GQ", "pt-GW", "pt-LU",
            "pt-MO", "pt-MZ", "pt-PT", "pt-ST", "pt-TL", "pt", "qu-BO", "qu-EC",
            "qu-PE", "qu", "rm-CH", "rm", "rn-BI", "rn", "rof-TZ", "rof", "ro-MD",
            "ro-RO", "ro", "ru-BY", "ru-KG", "ru-KZ", "ru-MD", "ru-RU", "ru-UA",
            "ru", "rwk-TZ", "rwk", "rw-RW", "rw", "sa-IN", "sa", "sah-RU", "sah", "saq-KE",
            "saq", "sbp-TZ", "sbp", "sd-PK", "sd", "se-FI", "seh-MZ", "seh", "se-NO", "se-SE",
            "ses-ML", "ses", "se", "sg-CF", "sg", "shi-Latn-MA", "shi-Latn",
            "shi-Tfng-MA", "shi-Tfng", "shi", "si-LK", "si", "sk-SK", "sk",
            "sl-SI", "sl", "smn-FI", "smn", "sn", "sn-ZW", "so-DJ", "so-ET",
            "so-KE", "so-SO", "so", "sq-AL", "sq-MK", "sq-XK", "sq", "sr-Cyrl-BA",
            "sr-Cyrl-ME", "sr-Cyrl-RS", "sr-Cyrl-XK", "sr-Cyrl", "sr-Latn-BA",
            "sr-Latn-ME", "sr-Latn-RS", "sr-Latn-XK", "sr-Latn", "sr", "sv-AX",
            "sv-FI", "sv-SE", "sv", "sw-CD", "sw-KE", "sw-TZ", "sw-UG", "sw",
            "ta-IN", "ta-LK", "ta-MY", "ta-SG", "ta", "te-IN", "teo-KE", "teo-UG",
            "teo", "te", "tg-TJ", "tg", "th-TH", "th", "ti-ER", "ti-ET", "ti", "tk-TM", "tk",
            "to-TO", "to", "tr-CY", "tr-TR", "tr", "tt-RU", "tt", "twq-NE", "twq", "tzm-MA", "tzm",
            "ug-CN", "ug", "uk-UA", "uk", "ur-IN", "ur-PK", "ur", "uz-Arab-AF",
            "uz-Arab", "uz-Cyrl-UZ", "uz-Cyrl", "uz-Latn-UZ", "uz-Latn", "uz",
            "vai-Latn-LR", "vai-Latn", "vai-Vaii-LR", "vai-Vaii", "vai", "vi-VN",
            "vi", "vun-TZ", "vun", "wae-CH", "wae", "wo-SN", "wo", "xh", "xh-ZA", "xog-UG",
            "xog", "yav-CM", "yav", "yi-001", "yi", "yo-BJ", "yo-NG", "yo", "yue-Hans-CN", "yue-Hans", "yue-Hant-HK", "yue-Hant",
            "yue", "zgh-MA", "zgh", "zh-Hans-CN", "zh-Hans-HK",
            "zh-Hans-MO", "zh-Hans-SG", "zh-Hans", "zh-Hant-HK", "zh-Hant-MO",
            "zh-Hant-TW", "zh-Hant", "zh", "zu", "zu-ZA");

        List<Locale> availableLocales = Arrays.asList(Locale.getAvailableLocales());

        List<Locale> localesNotFound = likelySubtagsLocales.stream()
                .map(Locale::forLanguageTag)
                .filter(l -> !availableLocales.contains(l))
                .collect(Collectors.toList());

        if (localesNotFound.size() > 0) {
            throw new RuntimeException("Locales " + localesNotFound
                    + " not found in Available Locales list");
        }
    }
}
