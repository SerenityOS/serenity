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
 * @bug 8040211 8191404 8203872 8222980 8225435 8241082 8242010 8247432
 *      8258795
 * @summary Checks the IANA language subtag registry data update
 *          (LSR Revision: 2021-05-11) with Locale and Locale.LanguageRange
 *          class methods.
 * @run main LanguageSubtagRegistryTest
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.List;
import java.util.Locale.LanguageRange;
import java.util.Locale.FilteringMode;
import static java.util.Locale.FilteringMode.EXTENDED_FILTERING;

public class LanguageSubtagRegistryTest {

    static boolean err = false;

    private static final String ACCEPT_LANGUAGE =
        "Accept-Language: aam, adp, aog, aue, bcg, bic, blg, bpp, cey, cnp, cqu, csp, csx, dif, dmw, ehs, ema,"
        + " en-gb-oed, gti, jks, kdz, koj, kru, kwq, kxe, kzk, lii, lmm, lsb, lsn, lsv, lvi, mtm,"
        + " ngv, nns, ola, oyb, pat, phr, pnd, pub, scv, snz, sqx, suj, szy, taj, tjj, tjp, tvx,"
        + " uss, uth, ysm, wkr;q=0.9, ar-hyw;q=0.8, yug;q=0.5, gfx;q=0.4";
    private static final List<LanguageRange> EXPECTED_RANGE_LIST = List.of(
            new LanguageRange("aam", 1.0),
            new LanguageRange("aas", 1.0),
            new LanguageRange("adp", 1.0),
            new LanguageRange("dz", 1.0),
            new LanguageRange("aog", 1.0),
            new LanguageRange("myd", 1.0),
            new LanguageRange("aue", 1.0),
            new LanguageRange("ktz", 1.0),
            new LanguageRange("bcg", 1.0),
            new LanguageRange("bgm", 1.0),
            new LanguageRange("bic", 1.0),
            new LanguageRange("bir", 1.0),
            new LanguageRange("blg", 1.0),
            new LanguageRange("iba", 1.0),
            new LanguageRange("bpp", 1.0),
            new LanguageRange("nxu", 1.0),
            new LanguageRange("cey", 1.0),
            new LanguageRange("cnp", 1.0),
            new LanguageRange("zh-cnp", 1.0),
            new LanguageRange("cqu", 1.0),
            new LanguageRange("quh", 1.0),
            new LanguageRange("csp", 1.0),
            new LanguageRange("zh-csp", 1.0),
            new LanguageRange("csx", 1.0),
            new LanguageRange("sgn-csx", 1.0),
            new LanguageRange("dif", 1.0),
            new LanguageRange("dit", 1.0),
            new LanguageRange("dmw", 1.0),
            new LanguageRange("xrq", 1.0),
            new LanguageRange("ehs", 1.0),
            new LanguageRange("sgn-ehs", 1.0),
            new LanguageRange("ema", 1.0),
            new LanguageRange("uok", 1.0),
            new LanguageRange("en-gb-oed", 1.0),
            new LanguageRange("en-gb-oxendict", 1.0),
            new LanguageRange("gti", 1.0),
            new LanguageRange("nyc", 1.0),
            new LanguageRange("jks", 1.0),
            new LanguageRange("sgn-jks", 1.0),
            new LanguageRange("kdz", 1.0),
            new LanguageRange("ncp", 1.0),
            new LanguageRange("koj", 1.0),
            new LanguageRange("kwv", 1.0),
            new LanguageRange("kru", 1.0),
            new LanguageRange("kxl", 1.0),
            new LanguageRange("kwq", 1.0),
            new LanguageRange("yam", 1.0),
            new LanguageRange("kxe", 1.0),
            new LanguageRange("tvd", 1.0),
            new LanguageRange("kzk", 1.0),
            new LanguageRange("gli", 1.0),
            new LanguageRange("drr", 1.0),
            new LanguageRange("lii", 1.0),
            new LanguageRange("raq", 1.0),
            new LanguageRange("lmm", 1.0),
            new LanguageRange("rmx", 1.0),
            new LanguageRange("lsb", 1.0),
            new LanguageRange("sgn-lsb", 1.0),
            new LanguageRange("lsn", 1.0),
            new LanguageRange("sgn-lsn", 1.0),
            new LanguageRange("lsv", 1.0),
            new LanguageRange("sgn-lsv", 1.0),
            new LanguageRange("lvi", 1.0),
            new LanguageRange("mtm", 1.0),
            new LanguageRange("ymt", 1.0),
            new LanguageRange("ngv", 1.0),
            new LanguageRange("nnx", 1.0),
            new LanguageRange("nns", 1.0),
            new LanguageRange("nbr", 1.0),
            new LanguageRange("ola", 1.0),
            new LanguageRange("thw", 1.0),
            new LanguageRange("oyb", 1.0),
            new LanguageRange("thx", 1.0),
            new LanguageRange("skk", 1.0),
            new LanguageRange("jeg", 1.0),
            new LanguageRange("pat", 1.0),
            new LanguageRange("kxr", 1.0),
            new LanguageRange("phr", 1.0),
            new LanguageRange("pmu", 1.0),
            new LanguageRange("pnd", 1.0),
            new LanguageRange("pub", 1.0),
            new LanguageRange("puz", 1.0),
            new LanguageRange("scv", 1.0),
            new LanguageRange("zir", 1.0),
            new LanguageRange("snz", 1.0),
            new LanguageRange("asd", 1.0),
            new LanguageRange("sqx", 1.0),
            new LanguageRange("sgn-sqx", 1.0),
            new LanguageRange("suj", 1.0),
            new LanguageRange("szy", 1.0),
            new LanguageRange("taj", 1.0),
            new LanguageRange("tsf", 1.0),
            new LanguageRange("tjj", 1.0),
            new LanguageRange("tjp", 1.0),
            new LanguageRange("tvx", 1.0),
            new LanguageRange("uss", 1.0),
            new LanguageRange("uth", 1.0),
            new LanguageRange("ysm", 1.0),
            new LanguageRange("sgn-ysm", 1.0),
            new LanguageRange("wkr", 0.9),
            new LanguageRange("ar-hyw", 0.8),
            new LanguageRange("yug", 0.5),
            new LanguageRange("yuu", 0.5),
            new LanguageRange("gfx", 0.4),
            new LanguageRange("oun", 0.4),
            new LanguageRange("mwj", 0.4),
            new LanguageRange("vaj", 0.4)
        );

    public static void main(String[] args) {
        testLanguageRange();
        testLocale();

        if (err) {
            throw new RuntimeException("Failed.");
        }
    }

    private static void testLanguageRange() {
        System.out.println("Test LanguageRange class parse method...");
        test_parse();
    }

    private static void testLocale() {
        System.out.println("Test Locale class methods...");
        test_filter();
        test_filterTags();
        test_lookup();
        test_lookupTag();
    }

    private static void test_parse() {
        boolean error = false;
        List<LanguageRange> got = LanguageRange.parse(ACCEPT_LANGUAGE);
        if (!areEqual(EXPECTED_RANGE_LIST, got)) {
            error = true;
            System.err.println("    language parse() test failed.");
        }

        if (error) {
            err = true;
            System.out.println("  test_parse() failed.");
        } else {
            System.out.println("  test_parse() passed.");
        }

    }

    private static boolean areEqual(List<LanguageRange> expected,
            List<LanguageRange> got) {
        boolean error = false;

        int expectedSize = expected.size();
        int actualSize = got.size();

        if (expectedSize != actualSize) {
            error = true;

            System.err.println("  Expected size=" + expectedSize);
            for (LanguageRange lr : expected) {
                System.err.println("    range=" + lr.getRange()
                        + ", weight=" + lr.getWeight());
            }

            System.err.println("  Actual size=" + actualSize);
            for (LanguageRange lr : got) {
                System.err.println("    range=" + lr.getRange()
                        + ", weight=" + lr.getWeight());
            }
        } else {
            for (int i = 0; i < expectedSize; i++) {
                LanguageRange lr1 = expected.get(i);
                LanguageRange lr2 = got.get(i);

                if (!lr1.getRange().equals(lr2.getRange())
                        || lr1.getWeight() != lr2.getWeight()) {
                    error = true;
                    System.err.println("  " + i + ": Expected: range=" + lr1.getRange()
                            + ", weight=" + lr1.getWeight());
                    System.err.println("  " + i + ": Actual:   range=" + lr2.getRange()
                            + ", weight=" + lr2.getWeight());
                }
            }
        }

        return !error;
    }

    private static void test_filter() {
        boolean error = false;

        String ranges = "mtm-RU, en-gb-oed, coy, ar-HY";
        String tags = "de-DE, en, mtm-RU, ymt-RU, en-gb-oxendict, ja-JP, pij, nts, ar-arevela";
        FilteringMode mode = EXTENDED_FILTERING;

        List<LanguageRange> priorityList = LanguageRange.parse(ranges);
        List<Locale> tagList = generateLocales(tags);
        String actualLocales
                = showLocales(Locale.filter(priorityList, tagList, mode));
        String expectedLocales = "mtm-RU, ymt-RU, en-GB-oxendict, nts, pij";

        if (!expectedLocales.equals(actualLocales)) {
            error = true;
            showErrorMessage("#1 filter(" + mode + ")",
                    ranges, tags, expectedLocales, actualLocales);
        }

        ranges = "phr-*-IN, ja-JP";
        tags = "en, pmu-Guru-IN, ja-Latn-JP, iw";
        mode = EXTENDED_FILTERING;

        priorityList = LanguageRange.parse(ranges);
        tagList = generateLocales(tags);
        actualLocales = showLocales(Locale.filter(priorityList, tagList, mode));
        expectedLocales = "pmu-Guru-IN, ja-Latn-JP";

        if (!expectedLocales.equals(actualLocales)) {
            error = true;
            showErrorMessage("#2 filter(" + mode + ")",
                    ranges, tags, expectedLocales, actualLocales);
        }

        if (error) {
            err = true;
            System.out.println("  test_filter() failed.");
        } else {
            System.out.println("  test_filter() passed.");
        }
    }

    private static void test_filterTags() {
        boolean error = false;

        String ranges = "gti;q=0.2, gfx, kzj";
        String tags = "de-DE, gti, he, nyc, mwj, vaj, ktr, dtp";

        List<LanguageRange> priorityList = LanguageRange.parse(ranges);
        List<String> tagList = generateLanguageTags(tags);
        String actualTags
                = showLanguageTags(Locale.filterTags(priorityList, tagList));
        String expectedTags = "mwj, vaj, ktr, dtp, gti, nyc";

        if (!expectedTags.equals(actualTags)) {
            error = true;
            showErrorMessage("filterTags()",
                    ranges, tags, expectedTags, actualTags);
        }

        if (error) {
            err = true;
            System.out.println("  test_filterTags() failed.");
        } else {
            System.out.println("  test_filterTags() passed.");
        }
    }

    private static void test_lookup() {
        boolean error = false;

        String ranges = "en;q=0.2, yam, rmx;q=0.9";
        String tags = "de-DE, en, kwq, lmm";
        List<LanguageRange> priorityList = LanguageRange.parse(ranges);
        List<Locale> localeList = generateLocales(tags);
        String actualLocale
                = Locale.lookup(priorityList, localeList).toLanguageTag();
        String expectedLocale = "kwq";

        if (!expectedLocale.equals(actualLocale)) {
            error = true;
            showErrorMessage("lookup()", ranges, tags, expectedLocale, actualLocale);
        }

        if (error) {
            err = true;
            System.out.println("  test_lookup() failed.");
        } else {
            System.out.println("  test_lookup() passed.");
        }
    }

    private static void test_lookupTag() {
        boolean error = false;

        String ranges = "en, tsf;q=0.2";
        String tags = "es, ja-JP, taj";
        List<LanguageRange> priorityList = LanguageRange.parse(ranges);
        List<String> tagList = generateLanguageTags(tags);
        String actualTag = Locale.lookupTag(priorityList, tagList);
        String expectedTag = "taj";

        if (!expectedTag.equals(actualTag)) {
            error = true;
            showErrorMessage("lookupTag()", ranges, tags, expectedTag, actualTag);
        }

        if (error) {
            err = true;
            System.out.println("  test_lookupTag() failed.");
        } else {
            System.out.println("  test_lookupTag() passed.");
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

        java.util.Iterator<Locale> itr = locales.iterator();
        if (itr.hasNext()) {
            sb.append(itr.next().toLanguageTag());
        }
        while (itr.hasNext()) {
            sb.append(", ");
            sb.append(itr.next().toLanguageTag());
        }

        return sb.toString().trim();
    }

    private static void showErrorMessage(String methodName,
            String priorityList,
            String tags,
            String expectedTags,
            String actualTags) {
        System.err.println("\nIncorrect " + methodName + " result.");
        System.err.println("  Priority list  :  " + priorityList);
        System.err.println("  Language tags  :  " + tags);
        System.err.println("  Expected value : " + expectedTags);
        System.err.println("  Actual value   : " + actualTags);
    }

}

