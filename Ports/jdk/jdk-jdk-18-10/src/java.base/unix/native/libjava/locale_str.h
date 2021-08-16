/*
 * Copyright (c) 1996, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Mappings from partial locale names to full locale names
 */
 static char *locale_aliases[] = {
    "ar", "ar_EG",
    "be", "be_BY",
    "bg", "bg_BG",
    "br", "br_FR",
    "ca", "ca_ES",
    "cs", "cs_CZ",
    "cz", "cs_CZ",
    "da", "da_DK",
    "de", "de_DE",
    "el", "el_GR",
    "en", "en_US",
    "eo", "eo",    /* no country for Esperanto */
    "es", "es_ES",
    "et", "et_EE",
    "eu", "eu_ES",
    "fi", "fi_FI",
    "fr", "fr_FR",
    "ga", "ga_IE",
    "gl", "gl_ES",
    "he", "iw_IL",
    "hr", "hr_HR",
#ifdef __linux__
    "hs", "en_US", // used on Linux, not clear what it stands for
#endif
    "hu", "hu_HU",
    "id", "in_ID",
    "in", "in_ID",
    "is", "is_IS",
    "it", "it_IT",
    "iw", "iw_IL",
    "ja", "ja_JP",
    "kl", "kl_GL",
    "ko", "ko_KR",
    "lt", "lt_LT",
    "lv", "lv_LV",
    "mk", "mk_MK",
    "nl", "nl_NL",
    "no", "no_NO",
    "pl", "pl_PL",
    "pt", "pt_PT",
    "ro", "ro_RO",
    "ru", "ru_RU",
    "se", "se_NO",
    "sk", "sk_SK",
    "sl", "sl_SI",
    "sq", "sq_AL",
    "sr", "sr_CS",
    "su", "fi_FI",
    "sv", "sv_SE",
    "th", "th_TH",
    "tr", "tr_TR",
#ifdef __linux__
    "ua", "en_US", // used on Linux, not clear what it stands for
#endif
    "uk", "uk_UA",
    "vi", "vi_VN",
    "wa", "wa_BE",
    "zh", "zh_CN",
#ifdef __linux__
    "bokmal", "nb_NO",
    "bokm\xE5l", "nb_NO",
    "catalan", "ca_ES",
    "croatian", "hr_HR",
    "czech", "cs_CZ",
    "danish", "da_DK",
    "dansk", "da_DK",
    "deutsch", "de_DE",
    "dutch", "nl_NL",
    "eesti", "et_EE",
    "estonian", "et_EE",
    "finnish", "fi_FI",
    "fran\xE7\x61is", "fr_FR",
    "french", "fr_FR",
    "galego", "gl_ES",
    "galician", "gl_ES",
    "german", "de_DE",
    "greek", "el_GR",
    "hebrew", "iw_IL",
    "hrvatski", "hr_HR",
    "hungarian", "hu_HU",
    "icelandic", "is_IS",
    "italian", "it_IT",
    "japanese", "ja_JP",
    "korean", "ko_KR",
    "lithuanian", "lt_LT",
    "norwegian", "no_NO",
    "nynorsk", "nn_NO",
    "polish", "pl_PL",
    "portuguese", "pt_PT",
    "romanian", "ro_RO",
    "russian", "ru_RU",
    "slovak", "sk_SK",
    "slovene", "sl_SI",
    "slovenian", "sl_SI",
    "spanish", "es_ES",
    "swedish", "sv_SE",
    "thai", "th_TH",
    "turkish", "tr_TR",
#else
    "big5", "zh_TW.Big5",
    "chinese", "zh_CN",
    "iso_8859_1", "en_US.ISO8859-1",
    "iso_8859_15", "en_US.ISO8859-15",
    "japanese", "ja_JP",
    "no_NY", "no_NO@nynorsk",
    "sr_SP", "sr_YU",
    "tchinese", "zh_TW",
#endif
    "", "",
 };

/*
 * Linux/Solaris language string to ISO639 string mapping table.
 */
static char *language_names[] = {
    "C", "en",
    "POSIX", "en",
    "cz", "cs",
    "he", "iw",
#ifdef __linux__
    "hs", "en", // used on Linux, not clear what it stands for
#endif
    "id", "in",
    "sh", "sr", // sh is deprecated
    "su", "fi",
#ifdef __linux__
    "ua", "en", // used on Linux, not clear what it stands for
    "catalan", "ca",
    "croatian", "hr",
    "czech", "cs",
    "danish", "da",
    "dansk", "da",
    "deutsch", "de",
    "dutch", "nl",
    "finnish", "fi",
    "fran\xE7\x61is", "fr",
    "french", "fr",
    "german", "de",
    "greek", "el",
    "hebrew", "he",
    "hrvatski", "hr",
    "hungarian", "hu",
    "icelandic", "is",
    "italian", "it",
    "japanese", "ja",
    "norwegian", "no",
    "polish", "pl",
    "portuguese", "pt",
    "romanian", "ro",
    "russian", "ru",
    "slovak", "sk",
    "slovene", "sl",
    "slovenian", "sl",
    "spanish", "es",
    "swedish", "sv",
    "turkish", "tr",
#else
    "chinese", "zh",
    "japanese", "ja",
    "korean", "ko",
#endif
    "", "",
};

/*
 * Linux/Solaris script string to Java script name mapping table.
 */
static char *script_names[] = {
#ifdef __linux__
    "cyrillic", "Cyrl",
    "devanagari", "Deva",
    "iqtelif", "Latn",
    "latin", "Latn",
#endif
    "Arab", "Arab",
    "Cyrl", "Cyrl",
    "Deva", "Deva",
    "Ethi", "Ethi",
    "Hans", "Hans",
    "Hant", "Hant",
    "Latn", "Latn",
    "Sund", "Sund",
    "Syrc", "Syrc",
    "Tfng", "Tfng",
    "", "",
};

/*
 * Linux/Solaris country string to ISO3166 string mapping table.
 */
static char *country_names[] = {
#ifdef __linux__
    "RN", "US", // used on Linux, not clear what it stands for
#endif
    "YU", "CS", // YU has been removed from ISO 3166
    "", "",
};

/*
 * Linux/Solaris variant string to Java variant name mapping table.
 */
static char *variant_names[] = {
    "nynorsk", "NY",
    "", "",
};
