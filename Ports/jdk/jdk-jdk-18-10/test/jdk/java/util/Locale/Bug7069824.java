/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7069824 8042360 8032842 8175539 8210443 8242010
 * @summary Verify implementation for Locale matching.
 * @run testng/othervm Bug7069824
 */

import java.util.*;
import java.util.Locale.*;
import static java.util.Locale.FilteringMode.*;
import static java.util.Locale.LanguageRange.*;
import static org.testng.Assert.*;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class Bug7069824 {

    @DataProvider(name = "LRConstructorData")
    Object[][] LRConstructorData() {
        return new Object[][] {
                // Range, Weight
                {"elvish", MAX_WEIGHT},
                {"de-DE", MAX_WEIGHT},
                {"de-Latn-DE-1996", MAX_WEIGHT},
                {"zh-Hant-CN-x-private1-private2", MAX_WEIGHT},
                {"ar", 0.8},
                {"en-US", 0.5},
                {"sr-Latn-BA", 0},
                {"ja", 1},
        };
    }

    @DataProvider(name = "LRConstructorNPEData")
    Object[][] LRConstructorNPEData() {
        return new Object[][] {
                // Range, Weight
                {null, MAX_WEIGHT},
                {null, 0.8},
        };
    }

    @DataProvider(name = "LRConstructorIAEData")
    Object[][] LRConstructorIAEData() {
        return new Object[][] {
                // Range, Weight
                {"ja", -0.8},
                {"Elvish", 3.0},
                {"-ja", MAX_WEIGHT},
                {"ja--JP", MAX_WEIGHT},
                {"en-US-", MAX_WEIGHT},
                {"a4r", MAX_WEIGHT},
                {"ar*", MAX_WEIGHT},
                {"ar-*EG", MAX_WEIGHT},
                {"abcdefghijklmn", MAX_WEIGHT},
                {"ja-J=", MAX_WEIGHT},
                {"ja-opqrstuvwxyz", MAX_WEIGHT},
                {"zh_CN", MAX_WEIGHT},
                {"1996-de-Latn", MAX_WEIGHT},
                // Testcase for 8042360
                {"en-Latn-1234567890", MAX_WEIGHT},
        };
    }

    @DataProvider(name = "LRParseData")
    Object[][] LRParseData() {
        return new Object[][] {
                // Ranges, Expected result
                {"Accept-Language: fr-FX, de-DE;q=0.5, fr-tp-x-FOO;q=0.1, "
                        + "en-X-tp;q=0.6, en-FR;q=0.7, de-de;q=0.8, iw;q=0.4, "
                        + "he;q=0.4, de-de;q=0.5, ja, in-tpp, in-tp;q=0.2",
                        List.of(new LanguageRange("fr-fx", 1.0),
                                new LanguageRange("fr-fr", 1.0),
                                new LanguageRange("ja", 1.0),
                                new LanguageRange("in-tpp", 1.0),
                                new LanguageRange("id-tpp", 1.0),
                                new LanguageRange("en-fr", 0.7),
                                new LanguageRange("en-fx", 0.7),
                                new LanguageRange("en-x-tp", 0.6),
                                new LanguageRange("de-de", 0.5),
                                new LanguageRange("de-dd", 0.5),
                                new LanguageRange("iw", 0.4),
                                new LanguageRange("he", 0.4),
                                new LanguageRange("in-tp", 0.2),
                                new LanguageRange("id-tl", 0.2),
                                new LanguageRange("id-tp", 0.2),
                                new LanguageRange("in-tl", 0.2),
                                new LanguageRange("fr-tp-x-foo", 0.1),
                                new LanguageRange("fr-tl-x-foo", 0.1))},
                {"Accept-Language: hak-CN;q=0.8, no-bok-NO;q=0.9, no-nyn, cmn-CN;q=0.1",
                        List.of(new LanguageRange("no-nyn", 1.0),
                                new LanguageRange("nn", 1.0),
                                new LanguageRange("no-bok-no", 0.9),
                                new LanguageRange("nb-no", 0.9),
                                new LanguageRange("hak-CN", 0.8),
                                new LanguageRange("zh-hakka-CN", 0.8),
                                new LanguageRange("i-hak-CN", 0.8),
                                new LanguageRange("zh-hak-CN", 0.8),
                                new LanguageRange("cmn-CN", 0.1),
                                new LanguageRange("zh-guoyu-CN", 0.1),
                                new LanguageRange("zh-cmn-CN", 0.1))},
                {"Accept-Language: rki;q=0.4, no-bok-NO;q=0.9, ccq;q=0.5",
                        List.of(new LanguageRange("no-bok-no", 0.9),
                                new LanguageRange("nb-no", 0.9),
                                new LanguageRange("rki", 0.4),
                                new LanguageRange("ybd", 0.4),
                                new LanguageRange("ccq", 0.4))},
        };
    }

    @DataProvider(name = "LRParseIAEData")
    Object[][] LRParseIAEData() {
        return new Object[][] {
                // Ranges
                {""},
                {"ja;q=3"},
        };
    }

    @DataProvider(name = "LRMapEquivalentsData")
    Object[][] LRMapEquivalentsData() {
        return new Object[][] {
                // Ranges, Map, Expected result
                {LanguageRange.parse("zh, zh-TW;q=0.8, ar;q=0.9, EN, zh-HK, ja-JP;q=0.2, es;q=0.4"),
                        new HashMap<>(),
                        LanguageRange.parse("zh, zh-TW;q=0.8, ar;q=0.9, EN, zh-HK, ja-JP;q=0.2, es;q=0.4")},
                {LanguageRange.parse("zh, zh-TW;q=0.8, ar;q=0.9, EN, zh-HK, ja-JP;q=0.2, es;q=0.4"),
                        null,
                        LanguageRange.parse("zh, zh-TW;q=0.8, ar;q=0.9, EN, zh-HK, ja-JP;q=0.2, es;q=0.4")},
                {LanguageRange.parse("zh, zh-TW;q=0.8, ar;q=0.9, EN, zh-HK, ja-JP;q=0.2, es;q=0.4"),
                        new LinkedHashMap<String, List<String>>() {
                            {
                                put("ja", List.of("ja", "ja-Hira"));
                                put("zh", List.of("zh-Hans", "zh-Hans-CN", "zh-CN"));
                                put("zh-TW", List.of("zh-TW", "zh-Hant"));
                                put("es", null);
                                put("en", List.of());
                                put("zh-HK", List.of("de"));
                            }
                        },
                        List.of(new LanguageRange("zh-hans", 1.0),
                                new LanguageRange("zh-hans-cn", 1.0),
                                new LanguageRange("zh-cn", 1.0),
                                new LanguageRange("de", 1.0),
                                new LanguageRange("ar", 0.9),
                                new LanguageRange("zh-tw", 0.8),
                                new LanguageRange("zh-hant", 0.8),
                                new LanguageRange("ja-jp", 0.2),
                                new LanguageRange("ja-hira-jp", 0.2))},
        };
    }

    @DataProvider(name = "LFilterData")
    Object[][] LFilterData() {
        return new Object[][] {
                // Range, LanguageTags, FilteringMode, Expected locales
                {"ja-JP, fr-FR", "de-DE, en, ja-JP-hepburn, fr, he, ja-Latn-JP",
                        EXTENDED_FILTERING, "ja-JP-hepburn, ja-Latn-JP"},
                {"ja-*-JP, fr-FR", "de-DE, en, ja-JP-hepburn, fr, he, ja-Latn-JP",
                        EXTENDED_FILTERING, "ja-JP-hepburn, ja-Latn-JP"},
                {"ja-*-JP, fr-FR, de-de;q=0.2", "de-DE, en, ja-JP-hepburn, de-de, fr, he, ja-Latn-JP",
                        AUTOSELECT_FILTERING, "ja-JP-hepburn, ja-Latn-JP, de-DE"},
                {"ja-JP, fr-FR, de-de;q=0.2", "de-DE, en, ja-JP-hepburn, de-de, fr, he, ja-Latn-JP",
                        AUTOSELECT_FILTERING, "ja-JP-hepburn, de-DE"},
                {"en;q=0.2, ja-*-JP, fr-JP", "de-DE, en, ja-JP-hepburn, fr, he, ja-Latn-JP",
                        IGNORE_EXTENDED_RANGES, "en"},
                {"en;q=0.2, ja-*-JP, fr-JP", "de-DE, en, ja-JP-hepburn, fr, he, ja-Latn-JP",
                        MAP_EXTENDED_RANGES, "ja-JP-hepburn, en"},
                {"en;q=0.2, ja-JP, fr-JP", "de-DE, en, ja-JP-hepburn, fr, he, ja-Latn-JP",
                        REJECT_EXTENDED_RANGES, "ja-JP-hepburn, en"},
                {"en;q=0.2, ja-*-JP, fr-JP", "", REJECT_EXTENDED_RANGES, ""},
        };
    }

    @DataProvider(name = "LFilterNPEData")
    Object[][] LFilterNPEData() {
        return new Object[][] {
                // Range, LanguageTags, FilteringMode
                {"en;q=0.2, ja-*-JP, fr-JP", null, REJECT_EXTENDED_RANGES},
                {null, "de-DE, en, ja-JP-hepburn, fr, he, ja-Latn-JP", REJECT_EXTENDED_RANGES},
        };
    }

    @DataProvider(name = "LFilterTagsData")
    Object[][] LFilterTagsData() {
        return new Object[][] {
                // Range, LanguageTags, FilteringMode, Expected language tags
                {"en;q=0.2, *;q=0.6, ja", "de-DE, en, ja-JP-hepburn, fr-JP, he",
                        null, "de-DE, en, ja-JP-hepburn, fr-JP, he"},
                {"en;q=0.2, ja-JP, fr-JP", "de-DE, en, ja-JP-hepburn, fr, he",
                        null, "ja-JP-hepburn, en"},
                {"en;q=0.2, ja-JP, fr-JP, iw", "de-DE, he, en, ja-JP-hepburn, fr, he-IL",
                        null, "ja-JP-hepburn, he, he-IL, en"},
                {"en;q=0.2, ja-JP, fr-JP, he", "de-DE, en, ja-JP-hepburn, fr, iw-IL",
                        null, "ja-JP-hepburn, iw-IL, en"},
                {"de-DE", "de-DE, de-de, de-Latn-DE, de-Latf-DE, de-DE-x-goethe, "
                        + "de-Latn-DE-1996, de-Deva-DE, de, de-x-DE, de-Deva",
                        MAP_EXTENDED_RANGES, "de-DE, de-DE-x-goethe"},
                {"de-DE", "de-DE, de-de, de-Latn-DE, de-Latf-DE, de-DE-x-goethe, "
                        + "de-Latn-DE-1996, de-Deva-DE, de, de-x-DE, de-Deva",
                        EXTENDED_FILTERING,
                        "de-DE, de-Latn-DE, de-Latf-DE, de-DE-x-goethe, "
                                + "de-Latn-DE-1996, de-Deva-DE"},
                {"de-*-DE", "de-DE, de-de, de-Latn-DE, de-Latf-DE, de-DE-x-goethe, "
                        + "de-Latn-DE-1996, de-Deva-DE, de, de-x-DE, de-Deva",
                        EXTENDED_FILTERING,
                        "de-DE, de-Latn-DE, de-Latf-DE, de-DE-x-goethe, "
                                + "de-Latn-DE-1996, de-Deva-DE"},
        };
    }

    @DataProvider(name = "LLookupData")
    Object[][] LLookupData() {
        return new Object[][] {
                // Range, LanguageTags, Expected locale
                {"en;q=0.2, *-JP;q=0.6, iw", "de-DE, en, ja-JP-hepburn, fr-JP, he", "he"},
                {"en;q=0.2, *-JP;q=0.6, iw", "de-DE, he-IL, en, iw", "he"},
                {"en;q=0.2, ja-*-JP-x-foo;q=0.6, iw", "de-DE, fr, en, ja-Latn-JP", "ja-Latn-JP"},
        };
    }

    @DataProvider(name = "LLookupTagData")
    Object[][] LLookupTagData() {
        return new Object[][] {
                // Range, LanguageTags, Expected language tag
                {"en, *", "es, de, ja-JP", null},
                {"en;q=0.2, *-JP", "de-DE, en, ja-JP-hepburn, fr-JP, en-JP", "fr-JP"},
                {"en;q=0.2, ar-MO, iw", "de-DE, he, fr-JP", "he"},
                {"en;q=0.2, ar-MO, he", "de-DE, iw, fr-JP", "iw"},
                {"de-DE-1996;q=0.8, en;q=0.2, iw;q=0.9, zh-Hans-CN;q=0.7", "de-DE, zh-CN, he, iw, fr-JP", "iw"},
                {"de-DE-1996;q=0.8, en;q=0.2, he;q=0.9, zh-Hans-CN;q=0.7", "de-DE, zh-CN, he, iw, fr-JP", "he"},
        };
    }

    @Test
    public void testLRConstants() {
        assertEquals(MIN_WEIGHT, 0.0, "    MIN_WEIGHT should be 0.0 but got "
                + MIN_WEIGHT);
        assertEquals(MAX_WEIGHT, 1.0, "    MAX_WEIGHT should be 1.0 but got "
                + MAX_WEIGHT);
    }

    @Test(dataProvider = "LRConstructorData")
    public void testLRConstructors(String range, double weight) {
        LanguageRange lr;
        if (weight == MAX_WEIGHT) {
            lr = new LanguageRange(range);
        } else {
            lr = new LanguageRange(range, weight);
        }
        assertEquals(lr.getRange(), range.toLowerCase(Locale.ROOT),
                "    LR.getRange() returned unexpected value. Expected: "
                        + range.toLowerCase(Locale.ROOT) + ", got: " + lr.getRange());
        assertEquals(lr.getWeight(), weight,
                "    LR.getWeight() returned unexpected value. Expected: "
                        + weight + ", got: " + lr.getWeight());
    }

    @Test(dataProvider = "LRConstructorNPEData", expectedExceptions = NullPointerException.class)
    public void testLRConstructorNPE(String range, double weight) {
        if (weight == MAX_WEIGHT) {
            new LanguageRange(range);
        } else {
            new LanguageRange(range, weight);
        }
    }

    @Test(dataProvider = "LRConstructorIAEData", expectedExceptions = IllegalArgumentException.class)
    public void testLRConstructorIAE(String range, double weight) {
        if (weight == MAX_WEIGHT) {
            new LanguageRange(range);
        } else {
            new LanguageRange(range, weight);
        }
    }

    @Test
    public void testLREquals() {
        LanguageRange lr1 = new LanguageRange("ja", 1.0);
        LanguageRange lr2 = new LanguageRange("ja");
        LanguageRange lr3 = new LanguageRange("ja", 0.1);
        LanguageRange lr4 = new LanguageRange("en", 1.0);

        assertEquals(lr1, lr2, "    LR(ja, 1.0).equals(LR(ja)) should return true.");
        assertNotEquals(lr1, lr3, "    LR(ja, 1.0).equals(LR(ja, 0.1)) should return false.");
        assertNotEquals(lr1, lr4, "    LR(ja, 1.0).equals(LR(en, 1.0)) should return false.");
        assertNotNull(lr1, "    LR(ja, 1.0).equals(null) should return false.");
        assertNotEquals(lr1, "", "    LR(ja, 1.0).equals(\"\") should return false.");
    }

    @Test(dataProvider = "LRParseData")
    public void testLRParse(String ranges, List<LanguageRange> expected) {
        assertEquals(LanguageRange.parse(ranges), expected,
                "    LR.parse(" + ranges + ") test failed.");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testLRParseNPE() {
        LanguageRange.parse(null);
    }

    @Test(dataProvider = "LRParseIAEData", expectedExceptions = IllegalArgumentException.class)
    public void testLRParseIAE(String ranges) {
        LanguageRange.parse(ranges);
    }

    @Test(dataProvider = "LRMapEquivalentsData")
    public void testLRMapEquivalents(List<Locale.LanguageRange> priorityList,
            Map<String,List<String>> map, List<LanguageRange> expected) {
        assertEquals(LanguageRange.mapEquivalents(priorityList, map), expected,
                "    LR.mapEquivalents() test failed.");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testLRMapEquivalentsNPE() {
        LanguageRange.mapEquivalents(null, Map.of("ja", List.of("ja", "ja-Hira")));
    }

    @Test(dataProvider = "LFilterData")
    public void testLFilter(String ranges, String tags, FilteringMode mode, String expectedLocales) {
        List<LanguageRange> priorityList = LanguageRange.parse(ranges);
        List<Locale> tagList = generateLocales(tags);
        String actualLocales =
                showLocales(Locale.filter(priorityList, tagList, mode));
        assertEquals(actualLocales, expectedLocales, showErrorMessage("    L.Filter(" + mode + ")",
                ranges, tags, expectedLocales, actualLocales));
    }

    @Test(dataProvider = "LFilterNPEData", expectedExceptions = NullPointerException.class)
    public void testLFilterNPE(String ranges, String tags, FilteringMode mode) {
        List<LanguageRange> priorityList = LanguageRange.parse(ranges);
        List<Locale> tagList = generateLocales(tags);
        showLocales(Locale.filter(priorityList, tagList, mode));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testLFilterIAE() {
        String ranges = "en;q=0.2, ja-*-JP, fr-JP";
        String tags = "de-DE, en, ja-JP-hepburn, fr, he, ja-Latn-JP";
        List<LanguageRange> priorityList = LanguageRange.parse(ranges);
        List<Locale> tagList = generateLocales(tags);
        showLocales(Locale.filter(priorityList, tagList, REJECT_EXTENDED_RANGES));
    }

    @Test(dataProvider = "LFilterTagsData")
    public void testLFilterTags(String ranges, String tags, FilteringMode mode, String expectedTags) {
        List<LanguageRange> priorityList = LanguageRange.parse(ranges);
        List<String> tagList = generateLanguageTags(tags);
        String actualTags;
        if (mode == null) {
            actualTags = showLanguageTags(Locale.filterTags(priorityList, tagList));
        } else {
            actualTags = showLanguageTags(Locale.filterTags(priorityList, tagList, mode));
        }
        assertEquals(actualTags, expectedTags,
                showErrorMessage("    L.FilterTags(" + (mode != null ? mode : "") + ")",
                        ranges, tags, expectedTags, actualTags));
    }

    @Test(dataProvider = "LLookupData")
    public void testLLookup(String ranges, String tags, String expectedLocale) {
        List<LanguageRange> priorityList = LanguageRange.parse(ranges);
        List<Locale> localeList = generateLocales(tags);
        String actualLocale =
                Locale.lookup(priorityList, localeList).toLanguageTag();
        assertEquals(actualLocale, expectedLocale, showErrorMessage("    L.Lookup()",
                ranges, tags, expectedLocale, actualLocale));
    }

    @Test(dataProvider = "LLookupTagData")
    public void testLLookupTag(String ranges, String tags, String expectedTag) {
        List<LanguageRange> priorityList = LanguageRange.parse(ranges);
        List<String> tagList = generateLanguageTags(tags);
        String actualTag = Locale.lookupTag(priorityList, tagList);
        assertEquals(actualTag, expectedTag, showErrorMessage("    L.LookupTag()",
                ranges, tags, expectedTag, actualTag));
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

    private static List<String> generateLanguageTags(String tags) {
        List<String> tagList = new ArrayList<>();
        String[] t = tags.split(", ");
        for (String tag : t) {
            tagList.add(tag);
        }
        return tagList;
    }

    private static String showLanguageTags(List<String> tags) {
        StringBuilder sb = new StringBuilder();

        Iterator<String> itr = tags.iterator();
        if (itr.hasNext()) {
            sb.append(itr.next());
        }
        while (itr.hasNext()) {
            sb.append(", ");
            sb.append(itr.next());
        }

        return sb.toString().trim();
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

    private static String showErrorMessage(String methodName,
            String priorityList,
            String tags,
            String expectedTags,
            String actualTags) {
        return "Incorrect " + methodName + " result."
                + "  Priority list  :  " + priorityList
                + "  Language tags  :  " + tags
                + "  Expected value : " + expectedTags
                + "  Actual value   : " + actualTags;
    }
}
