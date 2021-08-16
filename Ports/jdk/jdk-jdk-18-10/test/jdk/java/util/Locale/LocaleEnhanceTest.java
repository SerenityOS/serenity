/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Locale.Builder;
import java.util.Set;

/**
 * @test
 * @bug 6875847 6992272 7002320 7015500 7023613 7032820 7033504 7004603
 *    7044019 8008577 8176853 8255086 8263202
 * @summary test API changes to Locale
 * @library /java/text/testlib
 * @modules jdk.localedata
 * @compile LocaleEnhanceTest.java
 * @run main/othervm -Djava.locale.providers=JRE,SPI -esa LocaleEnhanceTest
 */
public class LocaleEnhanceTest extends IntlTest {

    public static void main(String[] args) throws Exception {
        List<String> argList = new ArrayList<String>();
        argList.addAll(Arrays.asList(args));
        argList.add("-nothrow");
        new LocaleEnhanceTest().run(argList.toArray(new String[argList.size()]));
    }

    public LocaleEnhanceTest() {
    }

    ///
    /// Generic sanity tests
    ///

    /** A canonical language code. */
    private static final String l = "en";

    /** A canonical script code.. */
    private static final String s = "Latn";

    /** A canonical region code. */
    private static final String c = "US";

    /** A canonical variant code. */
    private static final String v = "NewYork";

    /**
     * Ensure that Builder builds locales that have the expected
     * tag and java6 ID.  Note the odd cases for the ID.
     */
    public void testCreateLocaleCanonicalValid() {
        String[] valids = {
            "en-Latn-US-NewYork", "en_US_NewYork_#Latn",
            "en-Latn-US", "en_US_#Latn",
            "en-Latn-NewYork", "en__NewYork_#Latn", // double underscore
            "en-Latn", "en__#Latn", // double underscore
            "en-US-NewYork", "en_US_NewYork",
            "en-US", "en_US",
            "en-NewYork", "en__NewYork", // double underscore
            "en", "en",
            "und-Latn-US-NewYork", "_US_NewYork_#Latn",
            "und-Latn-US", "_US_#Latn",
            "und-Latn-NewYork", "", // variant only not supported
            "und-Latn", "",
            "und-US-NewYork", "_US_NewYork",
            "und-US", "_US",
            "und-NewYork", "", // variant only not supported
            "und", ""
        };

        Builder builder = new Builder();

        for (int i = 0; i < valids.length; i += 2) {
            String tag = valids[i];
            String id = valids[i+1];

            String idl = (i & 16) == 0 ? l : "";
            String ids = (i & 8) == 0 ? s : "";
            String idc = (i & 4) == 0 ? c : "";
            String idv = (i & 2) == 0 ? v : "";

            String msg = String.valueOf(i/2) + ": '" + tag + "' ";

            try {
                Locale l = builder
                    .setLanguage(idl)
                    .setScript(ids)
                    .setRegion(idc)
                    .setVariant(idv)
                    .build();
                assertEquals(msg + "language", idl, l.getLanguage());
                assertEquals(msg + "script", ids, l.getScript());
                assertEquals(msg + "country", idc, l.getCountry());
                assertEquals(msg + "variant", idv, l.getVariant());
                assertEquals(msg + "tag", tag, l.toLanguageTag());
                assertEquals(msg + "id", id, l.toString());
            }
            catch (IllegalArgumentException e) {
                errln(msg + e.getMessage());
            }
        }
    }

    /**
     * Test that locale construction works with 'multiple variants'.
     * <p>
     * The string "Newer__Yorker" is treated as three subtags,
     * "Newer", "", and "Yorker", and concatenated into one
     * subtag by omitting empty subtags and joining the remainer
     * with underscores.  So the resulting variant tag is "Newer_Yorker".
     * Note that 'New' and 'York' are invalid BCP47 variant subtags
     * because they are too short.
     */
    public void testCreateLocaleMultipleVariants() {

        String[] valids = {
            "en-Latn-US-Newer-Yorker",  "en_US_Newer_Yorker_#Latn",
            "en-Latn-Newer-Yorker",     "en__Newer_Yorker_#Latn",
            "en-US-Newer-Yorker",       "en_US_Newer_Yorker",
            "en-Newer-Yorker",          "en__Newer_Yorker",
            "und-Latn-US-Newer-Yorker", "_US_Newer_Yorker_#Latn",
            "und-Latn-Newer-Yorker",    "",
            "und-US-Newer-Yorker",      "_US_Newer_Yorker",
            "und-Newer-Yorker",         "",
        };

        Builder builder = new Builder(); // lenient variant

        final String idv = "Newer_Yorker";
        for (int i = 0; i < valids.length; i += 2) {
            String tag = valids[i];
            String id = valids[i+1];

            String idl = (i & 8) == 0 ? l : "";
            String ids = (i & 4) == 0 ? s : "";
            String idc = (i & 2) == 0 ? c : "";

            String msg = String.valueOf(i/2) + ": " + tag + " ";
            try {
                Locale l = builder
                    .setLanguage(idl)
                    .setScript(ids)
                    .setRegion(idc)
                    .setVariant(idv)
                    .build();

                assertEquals(msg + " language", idl, l.getLanguage());
                assertEquals(msg + " script", ids, l.getScript());
                assertEquals(msg + " country", idc, l.getCountry());
                assertEquals(msg + " variant", idv, l.getVariant());

                assertEquals(msg + "tag", tag, l.toLanguageTag());
                assertEquals(msg + "id", id, l.toString());
            }
            catch (IllegalArgumentException e) {
                errln(msg + e.getMessage());
            }
        }
    }

    /**
     * Ensure that all these invalid formats are not recognized by
     * forLanguageTag.
     */
    public void testCreateLocaleCanonicalInvalidSeparator() {
        String[] invalids = {
            // trailing separator
            "en_Latn_US_NewYork_",
            "en_Latn_US_",
            "en_Latn_",
            "en_",
            "_",

            // double separator
            "en_Latn_US__NewYork",
            "_Latn_US__NewYork",
            "en_US__NewYork",
            "_US__NewYork",

            // are these OK?
            // "en_Latn__US_NewYork", // variant is 'US_NewYork'
            // "_Latn__US_NewYork", // variant is 'US_NewYork'
            // "en__Latn_US_NewYork", // variant is 'Latn_US_NewYork'
            // "en__US_NewYork", // variant is 'US_NewYork'

            // double separator without language or script
            "__US",
            "__NewYork",

            // triple separator anywhere except within variant
            "en___NewYork",
            "en_Latn___NewYork",
            "_Latn___NewYork",
            "___NewYork",
        };

        for (int i = 0; i < invalids.length; ++i) {
            String id = invalids[i];
            Locale l = Locale.forLanguageTag(id);
            assertEquals(id, "und", l.toLanguageTag());
        }
    }

    /**
     * Ensure that all current locale ids parse.  Use DateFormat as a proxy
     * for all current locale ids.
     */
    public void testCurrentLocales() {
        Locale[] locales = java.text.DateFormat.getAvailableLocales();
        Builder builder = new Builder();

        for (Locale target : locales) {
            String tag = target.toLanguageTag();

            // the tag recreates the original locale,
            // except no_NO_NY
            Locale tagResult = Locale.forLanguageTag(tag);
            if (!target.getVariant().equals("NY")) {
                assertEquals("tagResult", target, tagResult);
            }

            // the builder also recreates the original locale,
            // except ja_JP_JP, th_TH_TH and no_NO_NY
            Locale builderResult = builder.setLocale(target).build();
            if (target.getVariant().length() != 2) {
                assertEquals("builderResult", target, builderResult);
            }
        }
    }

    /**
     * Ensure that all icu locale ids parse.
     */
    public void testIcuLocales() throws Exception {
        BufferedReader br = new BufferedReader(
            new InputStreamReader(
                LocaleEnhanceTest.class.getResourceAsStream("icuLocales.txt"),
                "UTF-8"));
        String id = null;
        while (null != (id = br.readLine())) {
            Locale result = Locale.forLanguageTag(id);
            assertEquals("ulocale", id, result.toLanguageTag());
        }
    }

    ///
    /// Compatibility tests
    ///

    public void testConstructor() {
        // all the old weirdness still holds, no new weirdness
        String[][] tests = {
            // language to lower case, region to upper, variant unchanged
            // short
            { "X", "y", "z", "x", "Y" },
            // long
            { "xXxXxXxXxXxX", "yYyYyYyYyYyYyYyY", "zZzZzZzZzZzZzZzZ",
              "xxxxxxxxxxxx", "YYYYYYYYYYYYYYYY" },
            // mapped language ids
            { "he", "IL", "", "he" },
            { "iw", "IL", "", "he" },
            { "yi", "DE", "", "yi" },
            { "ji", "DE", "", "yi" },
            { "id", "ID", "", "id" },
            { "in", "ID", "", "id" },
            // special variants
            { "ja", "JP", "JP" },
            { "th", "TH", "TH" },
            { "no", "NO", "NY" },
            { "no", "NO", "NY" },
            // no canonicalization of 3-letter language codes
            { "eng", "US", "" }
        };
        for (int i = 0; i < tests.length; ++ i) {
            String[] test = tests[i];
            String id = String.valueOf(i);
            Locale locale = new Locale(test[0], test[1], test[2]);
            assertEquals(id + " lang", test.length > 3 ? test[3] : test[0], locale.getLanguage());
            assertEquals(id + " region", test.length > 4 ? test[4] : test[1], locale.getCountry());
            assertEquals(id + " variant", test.length > 5 ? test[5] : test[2], locale.getVariant());
        }
    }

    ///
    /// Locale API tests.
    ///

    public void testGetScript() {
        // forLanguageTag normalizes case
        Locale locale = Locale.forLanguageTag("und-latn");
        assertEquals("forLanguageTag", "Latn", locale.getScript());

        // Builder normalizes case
        locale = new Builder().setScript("LATN").build();
        assertEquals("builder", "Latn", locale.getScript());

        // empty string is returned, not null, if there is no script
        locale = Locale.forLanguageTag("und");
        assertEquals("script is empty string", "", locale.getScript());
    }

    public void testGetExtension() {
        // forLanguageTag does NOT normalize to hyphen
        Locale locale = Locale.forLanguageTag("und-a-some_ex-tension");
        assertEquals("some_ex-tension", null, locale.getExtension('a'));

        // regular extension
        locale = new Builder().setExtension('a', "some-ex-tension").build();
        assertEquals("builder", "some-ex-tension", locale.getExtension('a'));

        // returns null if extension is not present
        assertEquals("empty b", null, locale.getExtension('b'));

        // throws exception if extension tag is illegal
        new ExpectIAE() { public void call() { Locale.forLanguageTag("").getExtension('\uD800'); }};

        // 'x' is not an extension, it's a private use tag, but it's accessed through this API
        locale = Locale.forLanguageTag("x-y-z-blork");
        assertEquals("x", "y-z-blork", locale.getExtension('x'));
    }

    public void testGetExtensionKeys() {
        Locale locale = Locale.forLanguageTag("und-a-xx-yy-b-zz-ww");
        Set<Character> result = locale.getExtensionKeys();
        assertEquals("result size", 2, result.size());
        assertTrue("'a','b'", result.contains('a') && result.contains('b'));

        // result is not mutable
        try {
            result.add('x');
            errln("expected exception on add to extension key set");
        }
        catch (UnsupportedOperationException e) {
            // ok
        }

        // returns empty set if no extensions
        locale = Locale.forLanguageTag("und");
        assertTrue("empty result", locale.getExtensionKeys().isEmpty());
    }

    public void testGetUnicodeLocaleAttributes() {
        Locale locale = Locale.forLanguageTag("en-US-u-abc-def");
        Set<String> attributes = locale.getUnicodeLocaleAttributes();
        assertEquals("number of attributes", 2, attributes.size());
        assertTrue("attribute abc", attributes.contains("abc"));
        assertTrue("attribute def", attributes.contains("def"));

        locale = Locale.forLanguageTag("en-US-u-ca-gregory");
        attributes = locale.getUnicodeLocaleAttributes();
        assertTrue("empty attributes", attributes.isEmpty());
    }

    public void testGetUnicodeLocaleType() {
        Locale locale = Locale.forLanguageTag("und-u-co-japanese-nu-thai");
        assertEquals("collation", "japanese", locale.getUnicodeLocaleType("co"));
        assertEquals("numbers", "thai", locale.getUnicodeLocaleType("nu"));

        // Unicode locale extension key is case insensitive
        assertEquals("key case", "japanese", locale.getUnicodeLocaleType("Co"));

        // if keyword is not present, returns null
        assertEquals("locale keyword not present", null, locale.getUnicodeLocaleType("xx"));

        // if no locale extension is set, returns null
        locale = Locale.forLanguageTag("und");
        assertEquals("locale extension not present", null, locale.getUnicodeLocaleType("co"));

        // typeless keyword
        locale = Locale.forLanguageTag("und-u-kn");
        assertEquals("typeless keyword", "", locale.getUnicodeLocaleType("kn"));

        // invalid keys throw exception
        new ExpectIAE() { public void call() { Locale.forLanguageTag("").getUnicodeLocaleType("q"); }};
        new ExpectIAE() { public void call() { Locale.forLanguageTag("").getUnicodeLocaleType("abcdefghi"); }};

        // null argument throws exception
        new ExpectNPE() { public void call() { Locale.forLanguageTag("").getUnicodeLocaleType(null); }};
    }

    public void testGetUnicodeLocaleKeys() {
        Locale locale = Locale.forLanguageTag("und-u-co-japanese-nu-thai");
        Set<String> result = locale.getUnicodeLocaleKeys();
        assertEquals("two keys", 2, result.size());
        assertTrue("co and nu", result.contains("co") && result.contains("nu"));

        // result is not modifiable
        try {
            result.add("frobozz");
            errln("expected exception when add to locale key set");
        }
        catch (UnsupportedOperationException e) {
            // ok
        }
    }

    public void testPrivateUseExtension() {
        Locale locale = Locale.forLanguageTag("x-y-x-blork-");
        assertEquals("blork", "y-x-blork", locale.getExtension(Locale.PRIVATE_USE_EXTENSION));

        locale = Locale.forLanguageTag("und");
        assertEquals("no privateuse", null, locale.getExtension(Locale.PRIVATE_USE_EXTENSION));
    }

    public void testToLanguageTag() {
        // lots of normalization to test here
        // test locales created using the constructor
        String[][] tests = {
            // empty locale canonicalizes to 'und'
            { "", "", "", "und" },
            // variant alone is not a valid Locale, but has a valid language tag
            { "", "", "NewYork", "und-NewYork" },
            // standard valid locales
            { "", "Us", "", "und-US" },
            { "", "US", "NewYork", "und-US-NewYork" },
            { "EN", "", "", "en" },
            { "EN", "", "NewYork", "en-NewYork" },
            { "EN", "US", "", "en-US" },
            { "EN", "US", "NewYork", "en-US-NewYork" },
            // underscore in variant will be emitted as multiple variant subtags
            { "en", "US", "Newer_Yorker", "en-US-Newer-Yorker" },
            // invalid variant subtags are appended as private use
            { "en", "US", "new_yorker", "en-US-x-lvariant-new-yorker" },
            // the first invalid variant subtags and following variant subtags are appended as private use
            { "en", "US", "Windows_XP_Home", "en-US-Windows-x-lvariant-XP-Home" },
            // too long variant and following variant subtags disappear
            { "en", "US", "WindowsVista_SP2", "en-US" },
            // invalid region subtag disappears
            { "en", "USA", "", "en" },
            // invalid language tag disappears
            { "e", "US", "", "und-US" },
            // three-letter language tags are not canonicalized
            { "Eng", "", "", "eng" },
            // legacy languages canonicalize to modern equivalents
            { "he", "IL", "", "he-IL" },
            { "iw", "IL", "", "he-IL" },
            { "yi", "DE", "", "yi-DE" },
            { "ji", "DE", "", "yi-DE" },
            { "id", "ID", "", "id-ID" },
            { "in", "ID", "", "id-ID" },
            // special values are converted on output
            { "ja", "JP", "JP", "ja-JP-u-ca-japanese-x-lvariant-JP" },
            { "th", "TH", "TH", "th-TH-u-nu-thai-x-lvariant-TH" },
            { "no", "NO", "NY", "nn-NO" }
        };
        for (int i = 0; i < tests.length; ++i) {
            String[] test = tests[i];
            Locale locale = new Locale(test[0], test[1], test[2]);
            assertEquals("case " + i, test[3], locale.toLanguageTag());
        }

        // test locales created from forLanguageTag
        String[][] tests1 = {
            // case is normalized during the round trip
            { "EN-us", "en-US" },
            { "en-Latn-US", "en-Latn-US" },
            // reordering Unicode locale extensions
            { "de-u-co-phonebk-ca-gregory", "de-u-ca-gregory-co-phonebk" },
            // private use only language tag is preserved (no extra "und")
            { "x-elmer", "x-elmer" },
            { "x-lvariant-JP", "x-lvariant-JP" },
        };
        for (String[] test : tests1) {
            Locale locale = Locale.forLanguageTag(test[0]);
            assertEquals("case " + test[0], test[1], locale.toLanguageTag());
        }

    }

    public void testForLanguageTag() {
        // forLanguageTag implements the 'Language-Tag' production of
        // BCP47, so it handles private use and legacy language tags,
        // unlike locale builder.  Tags listed below (except for the
        // sample private use tags) come from 4646bis Feb 29, 2009.

        String[][] tests = {
            // private use tags only
            { "x-abc", "x-abc" },
            { "x-a-b-c", "x-a-b-c" },
            { "x-a-12345678", "x-a-12345678" },

            // legacy language tags with preferred mappings
            { "i-ami", "ami" },
            { "i-bnn", "bnn" },
            { "i-hak", "hak" },
            { "i-klingon", "tlh" },
            { "i-lux", "lb" }, // two-letter tag
            { "i-navajo", "nv" }, // two-letter tag
            { "i-pwn", "pwn" },
            { "i-tao", "tao" },
            { "i-tay", "tay" },
            { "i-tsu", "tsu" },
            { "art-lojban", "jbo" },
            { "no-bok", "nb" },
            { "no-nyn", "nn" },
            { "sgn-BE-FR", "sfb" },
            { "sgn-BE-NL", "vgt" },
            { "sgn-CH-DE", "sgg" },
            { "zh-guoyu", "cmn" },
            { "zh-hakka", "hak" },
            { "zh-min-nan", "nan" },
            { "zh-xiang", "hsn" },

            // irregular legacy language tags, no preferred mappings, drop illegal fields
            // from end.  If no subtag is mappable, fallback to 'und'
            { "i-default", "en-x-i-default" },
            { "i-enochian", "x-i-enochian" },
            { "i-mingo", "see-x-i-mingo" },
            { "en-GB-oed", "en-GB-x-oed" },
            { "zh-min", "nan-x-zh-min" },
            { "cel-gaulish", "xtg-x-cel-gaulish" },
        };
        for (int i = 0; i < tests.length; ++i) {
            String[] test = tests[i];
            Locale locale = Locale.forLanguageTag(test[0]);
            assertEquals("legacy language tag case " + i, test[1], locale.toLanguageTag());
        }

        // forLanguageTag ignores everything past the first place it encounters
        // a syntax error
        tests = new String[][] {
            { "valid",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-def-x-y-12345678-z",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-def-x-y-12345678-z" },
            { "segment of private use tag too long",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-def-x-y-123456789-z",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-def-x-y" },
            { "segment of private use tag is empty",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-def-x-y--12345678-z",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-def-x-y" },
            { "first segment of private use tag is empty",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-def-x--y-12345678-z",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-def" },
            { "illegal extension tag",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-def-\uD800-y-12345678-z",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-def" },
            { "locale subtag with no value",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-x-y-12345678-z",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-bb-x-y-12345678-z" },
            { "locale key subtag invalid",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc-123456789-def-x-y-12345678-z",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-aa-abc" },
            // locale key subtag invalid in earlier position, all following subtags
            // dropped (and so the locale extension dropped as well)
            { "locale key subtag invalid in earlier position",
              "en-US-Newer-Yorker-a-bb-cc-dd-u-123456789-abc-bb-def-x-y-12345678-z",
              "en-US-Newer-Yorker-a-bb-cc-dd" },
        };
        for (int i = 0; i < tests.length; ++i) {
            String[] test = tests[i];
            String msg = "syntax error case " + i + " " + test[0];
            try {
                Locale locale = Locale.forLanguageTag(test[1]);
                assertEquals(msg, test[2], locale.toLanguageTag());
            }
            catch (IllegalArgumentException e) {
                errln(msg + " caught exception: " + e);
            }
        }

        // duplicated extension are just ignored
        Locale locale = Locale.forLanguageTag("und-d-aa-00-bb-01-D-AA-10-cc-11-c-1234");
        assertEquals("extension", "aa-00-bb-01", locale.getExtension('d'));
        assertEquals("extension c", "1234", locale.getExtension('c'));

        locale = Locale.forLanguageTag("und-U-ca-gregory-u-ca-japanese");
        assertEquals("Unicode extension", "ca-gregory", locale.getExtension(Locale.UNICODE_LOCALE_EXTENSION));

        // redundant Unicode locale keys in an extension are ignored
        locale = Locale.forLanguageTag("und-u-aa-000-bb-001-bB-002-cc-003-c-1234");
        assertEquals("Unicode keywords", "aa-000-bb-001-cc-003", locale.getExtension(Locale.UNICODE_LOCALE_EXTENSION));
        assertEquals("Duplicated Unicode locake key followed by an extension", "1234", locale.getExtension('c'));
    }

    public void testGetDisplayScript() {
        Locale latnLocale = Locale.forLanguageTag("und-latn");
        Locale hansLocale = Locale.forLanguageTag("und-hans");

        Locale oldLocale = Locale.getDefault();

        Locale.setDefault(Locale.US);
        assertEquals("latn US", "Latin", latnLocale.getDisplayScript());
        assertEquals("hans US", "Simplified", hansLocale.getDisplayScript());

        Locale.setDefault(Locale.GERMANY);
        assertEquals("latn DE", "Lateinisch", latnLocale.getDisplayScript());
        assertEquals("hans DE", "Vereinfachte Chinesische Schrift", hansLocale.getDisplayScript());

        Locale.setDefault(oldLocale);
    }

    public void testGetDisplayScriptWithLocale() {
        Locale latnLocale = Locale.forLanguageTag("und-latn");
        Locale hansLocale = Locale.forLanguageTag("und-hans");

        assertEquals("latn US", "Latin", latnLocale.getDisplayScript(Locale.US));
        assertEquals("hans US", "Simplified", hansLocale.getDisplayScript(Locale.US));

        assertEquals("latn DE", "Lateinisch", latnLocale.getDisplayScript(Locale.GERMANY));
        assertEquals("hans DE", "Vereinfachte Chinesische Schrift", hansLocale.getDisplayScript(Locale.GERMANY));
    }

    public void testGetDisplayName() {
        final Locale[] testLocales = {
                Locale.ROOT,
                new Locale("en"),
                new Locale("en", "US"),
                new Locale("", "US"),
                new Locale("no", "NO", "NY"),
                new Locale("", "", "NY"),
                Locale.forLanguageTag("zh-Hans"),
                Locale.forLanguageTag("zh-Hant"),
                Locale.forLanguageTag("zh-Hans-CN"),
                Locale.forLanguageTag("und-Hans"),
        };

        final String[] displayNameEnglish = {
                "",
                "English",
                "English (United States)",
                "United States",
                "Norwegian (Norway,Nynorsk)",
                "Nynorsk",
                "Chinese (Simplified)",
                "Chinese (Traditional)",
                "Chinese (Simplified,China)",
                "Simplified",
        };

        final String[] displayNameSimplifiedChinese = {
                "",
                "\u82f1\u6587",
                "\u82f1\u6587 (\u7f8e\u56fd)",
                "\u7f8e\u56fd",
                "\u632a\u5a01\u6587 (\u632a\u5a01,Nynorsk)",
                "Nynorsk",
                "\u4e2d\u6587 (\u7b80\u4f53\u4e2d\u6587)",
                "\u4e2d\u6587 (\u7e41\u4f53\u4e2d\u6587)",
                "\u4e2d\u6587 (\u7b80\u4f53\u4e2d\u6587,\u4e2d\u56fd)",
                "\u7b80\u4f53\u4e2d\u6587",
        };

        for (int i = 0; i < testLocales.length; i++) {
            Locale loc = testLocales[i];
            assertEquals("English display name for " + loc.toLanguageTag(),
                    displayNameEnglish[i], loc.getDisplayName(Locale.ENGLISH));
            assertEquals("Simplified Chinese display name for " + loc.toLanguageTag(),
                    displayNameSimplifiedChinese[i], loc.getDisplayName(Locale.CHINA));
        }
    }

    ///
    /// Builder tests
    ///

    public void testBuilderSetLocale() {
        Builder builder = new Builder();
        Builder lenientBuilder = new Builder();

        String languageTag = "en-Latn-US-NewYork-a-bb-ccc-u-co-japanese-x-y-z";
        String target = "en-Latn-US-NewYork-a-bb-ccc-u-co-japanese-x-y-z";

        Locale locale = Locale.forLanguageTag(languageTag);
        Locale result = lenientBuilder
            .setLocale(locale)
            .build();
        assertEquals("long tag", target, result.toLanguageTag());
        assertEquals("long tag", locale, result);

        // null is illegal
        new BuilderNPE("locale") {
            public void call() { b.setLocale(null); }
        };

        // builder canonicalizes the three legacy locales:
        // ja_JP_JP, th_TH_TH, no_NY_NO.
        locale = builder.setLocale(new Locale("ja", "JP", "JP")).build();
        assertEquals("ja_JP_JP languagetag", "ja-JP-u-ca-japanese", locale.toLanguageTag());
        assertEquals("ja_JP_JP variant", "", locale.getVariant());

        locale = builder.setLocale(new Locale("th", "TH", "TH")).build();
        assertEquals("th_TH_TH languagetag", "th-TH-u-nu-thai", locale.toLanguageTag());
        assertEquals("th_TH_TH variant", "", locale.getVariant());

        locale = builder.setLocale(new Locale("no", "NO", "NY")).build();
        assertEquals("no_NO_NY languagetag", "nn-NO", locale.toLanguageTag());
        assertEquals("no_NO_NY language", "nn", locale.getLanguage());
        assertEquals("no_NO_NY variant", "", locale.getVariant());

        // non-canonical, non-legacy locales are invalid
        new BuilderILE("123_4567_89") {
            public void call() {
                b.setLocale(new Locale("123", "4567", "89"));
            }
        };
    }

    public void testBuilderSetLanguageTag() {
        String source = "eN-LaTn-Us-NewYork-A-Xx-B-Yy-X-1-2-3";
        String target = "en-Latn-US-NewYork-a-xx-b-yy-x-1-2-3";
        Builder builder = new Builder();
        String result = builder
            .setLanguageTag(source)
            .build()
            .toLanguageTag();
        assertEquals("language", target, result);

        // redundant extensions cause a failure
        new BuilderILE() { public void call() { b.setLanguageTag("und-a-xx-yy-b-ww-A-00-11-c-vv"); }};

        // redundant Unicode locale extension keys within an Unicode locale extension cause a failure
        new BuilderILE() { public void call() { b.setLanguageTag("und-u-nu-thai-NU-chinese-xx-1234"); }};
    }

    public void testBuilderSetLanguage() {
        // language is normalized to lower case
        String source = "eN";
        String target = "en";
        String defaulted = "";
        Builder builder = new Builder();
        String result = builder
            .setLanguage(source)
            .build()
            .getLanguage();
        assertEquals("en", target, result);

        // setting with empty resets
        result = builder
            .setLanguage(target)
            .setLanguage("")
            .build()
            .getLanguage();
        assertEquals("empty", defaulted, result);

        // setting with null resets too
        result = builder
                .setLanguage(target)
                .setLanguage(null)
                .build()
                .getLanguage();
        assertEquals("null", defaulted, result);

        // language codes must be 2-8 alpha
        // for forwards compatibility, 4-alpha and 5-8 alpha (registered)
        // languages are accepted syntax
        new BuilderILE("q", "abcdefghi", "13") { public void call() { b.setLanguage(arg); }};

        // language code validation is NOT performed, any 2-8-alpha passes
        assertNotNull("2alpha", builder.setLanguage("zz").build());
        assertNotNull("8alpha", builder.setLanguage("abcdefgh").build());

        // three-letter language codes are NOT canonicalized to two-letter
        result = builder
            .setLanguage("eng")
            .build()
            .getLanguage();
        assertEquals("eng", "eng", result);
    }

    public void testBuilderSetScript() {
        // script is normalized to title case
        String source = "lAtN";
        String target = "Latn";
        String defaulted = "";
        Builder builder = new Builder();
        String result = builder
            .setScript(source)
            .build()
            .getScript();
        assertEquals("script", target, result);

        // setting with empty resets
        result = builder
            .setScript(target)
            .setScript("")
            .build()
            .getScript();
        assertEquals("empty", defaulted, result);

        // settting with null also resets
        result = builder
                .setScript(target)
                .setScript(null)
                .build()
                .getScript();
        assertEquals("null", defaulted, result);

        // ill-formed script codes throw IAE
        // must be 4alpha
        new BuilderILE("abc", "abcde", "l3tn") { public void call() { b.setScript(arg); }};

        // script code validation is NOT performed, any 4-alpha passes
        assertEquals("4alpha", "Wxyz", builder.setScript("wxyz").build().getScript());
    }

    public void testBuilderSetRegion() {
        // region is normalized to upper case
        String source = "uS";
        String target = "US";
        String defaulted = "";
        Builder builder = new Builder();
        String result = builder
            .setRegion(source)
            .build()
            .getCountry();
        assertEquals("us", target, result);

        // setting with empty resets
        result = builder
            .setRegion(target)
            .setRegion("")
            .build()
            .getCountry();
        assertEquals("empty", defaulted, result);

        // setting with null also resets
        result = builder
                .setRegion(target)
                .setRegion(null)
                .build()
                .getCountry();
        assertEquals("null", defaulted, result);

        // ill-formed region codes throw IAE
        // 2 alpha or 3 numeric
        new BuilderILE("q", "abc", "12", "1234", "a3", "12a") { public void call() { b.setRegion(arg); }};

        // region code validation is NOT performed, any 2-alpha or 3-digit passes
        assertEquals("2alpha", "ZZ", builder.setRegion("ZZ").build().getCountry());
        assertEquals("3digit", "000", builder.setRegion("000").build().getCountry());
    }

    public void testBuilderSetVariant() {
        // Variant case is not normalized in lenient variant mode
        String source = "NewYork";
        String target = source;
        String defaulted = "";
        Builder builder = new Builder();
        String result = builder
            .setVariant(source)
            .build()
            .getVariant();
        assertEquals("NewYork", target, result);

        result = builder
            .setVariant("NeWeR_YoRkEr")
            .build()
            .toLanguageTag();
        assertEquals("newer yorker", "und-NeWeR-YoRkEr", result);

        // subtags of variant are NOT reordered
        result = builder
            .setVariant("zzzzz_yyyyy_xxxxx")
            .build()
            .getVariant();
        assertEquals("zyx", "zzzzz_yyyyy_xxxxx", result);

        // setting to empty resets
        result = builder
            .setVariant(target)
            .setVariant("")
            .build()
            .getVariant();
        assertEquals("empty", defaulted, result);

        // setting to null also resets
        result = builder
                .setVariant(target)
                .setVariant(null)
                .build()
                .getVariant();
        assertEquals("null", defaulted, result);

        // ill-formed variants throw IAE
        // digit followed by 3-7 characters, or alpha followed by 4-8 characters.
        new BuilderILE("abcd", "abcdefghi", "1ab", "1abcdefgh") { public void call() { b.setVariant(arg); }};

        // 4 characters is ok as long as the first is a digit
        assertEquals("digit+3alpha", "1abc", builder.setVariant("1abc").build().getVariant());

        // all subfields must conform
        new BuilderILE("abcde-fg") { public void call() { b.setVariant(arg); }};
    }

    public void testBuilderSetExtension() {
        // upper case characters are normalized to lower case
        final char sourceKey = 'a';
        final String sourceValue = "aB-aBcdefgh-12-12345678";
        String target = "ab-abcdefgh-12-12345678";
        Builder builder = new Builder();
        String result = builder
            .setExtension(sourceKey, sourceValue)
            .build()
            .getExtension(sourceKey);
        assertEquals("extension", target, result);

        // setting with empty resets
        result = builder
            .setExtension(sourceKey, sourceValue)
            .setExtension(sourceKey, "")
            .build()
            .getExtension(sourceKey);
        assertEquals("empty", null, result);

        // setting with null also resets
        result = builder
                .setExtension(sourceKey, sourceValue)
                .setExtension(sourceKey, null)
                .build()
                .getExtension(sourceKey);
        assertEquals("null", null, result);

        // ill-formed extension keys throw IAE
        // must be in [0-9a-ZA-Z]
        new BuilderILE("$") { public void call() { b.setExtension('$', sourceValue); }};

        // each segment of value must be 2-8 alphanum
        new BuilderILE("ab-cd-123456789") { public void call() { b.setExtension(sourceKey, arg); }};

        // no multiple hyphens.
        new BuilderILE("ab--cd") { public void call() { b.setExtension(sourceKey, arg); }};

        // locale extension key has special handling
        Locale locale = builder
            .setExtension('u', "co-japanese")
            .build();
        assertEquals("locale extension", "japanese", locale.getUnicodeLocaleType("co"));

        // locale extension has same behavior with set locale keyword
        Locale locale2 = builder
            .setUnicodeLocaleKeyword("co", "japanese")
            .build();
        assertEquals("locales with extension", locale, locale2);

        // setting locale extension overrides all previous calls to setLocaleKeyword
        Locale locale3 = builder
            .setExtension('u', "xxx-nu-thai")
            .build();
        assertEquals("remove co", null, locale3.getUnicodeLocaleType("co"));
        assertEquals("override thai", "thai", locale3.getUnicodeLocaleType("nu"));
        assertEquals("override attribute", 1, locale3.getUnicodeLocaleAttributes().size());

        // setting locale keyword extends values already set by the locale extension
        Locale locale4 = builder
            .setUnicodeLocaleKeyword("co", "japanese")
            .build();
        assertEquals("extend", "japanese", locale4.getUnicodeLocaleType("co"));
        assertEquals("extend", "thai", locale4.getUnicodeLocaleType("nu"));

        // locale extension subtags are reordered
        result = builder
            .clear()
            .setExtension('u', "456-123-zz-123-yy-456-xx-789")
            .build()
            .toLanguageTag();
        assertEquals("reorder", "und-u-123-456-xx-789-yy-456-zz-123", result);

        // multiple keyword types
        result = builder
            .clear()
            .setExtension('u', "nu-thai-foobar")
            .build()
            .getUnicodeLocaleType("nu");
        assertEquals("multiple types", "thai-foobar", result);

        // redundant locale extensions are ignored
        result = builder
            .clear()
            .setExtension('u', "nu-thai-NU-chinese-xx-1234")
            .build()
            .toLanguageTag();
        assertEquals("duplicate keys", "und-u-nu-thai-xx-1234", result);
    }

    public void testBuilderAddUnicodeLocaleAttribute() {
        Builder builder = new Builder();
        Locale locale = builder
            .addUnicodeLocaleAttribute("def")
            .addUnicodeLocaleAttribute("abc")
            .build();

        Set<String> uattrs = locale.getUnicodeLocaleAttributes();
        assertEquals("number of attributes", 2, uattrs.size());
        assertTrue("attribute abc", uattrs.contains("abc"));
        assertTrue("attribute def", uattrs.contains("def"));

        // remove attribute
        locale = builder.removeUnicodeLocaleAttribute("xxx")
            .build();

        assertEquals("remove bogus", 2, uattrs.size());

        // add duplicate
        locale = builder.addUnicodeLocaleAttribute("abc")
            .build();
        assertEquals("add duplicate", 2, uattrs.size());

        // null attribute throws NPE
        new BuilderNPE("null attribute") { public void call() { b.addUnicodeLocaleAttribute(null); }};
        new BuilderNPE("null attribute removal") { public void call() { b.removeUnicodeLocaleAttribute(null); }};

        // illformed attribute throws IllformedLocaleException
        new BuilderILE("invalid attribute") { public void call() { b.addUnicodeLocaleAttribute("ca"); }};
    }

    public void testBuildersetUnicodeLocaleKeyword() {
        // Note: most behavior is tested in testBuilderSetExtension
        Builder builder = new Builder();
        Locale locale = builder
            .setUnicodeLocaleKeyword("co", "japanese")
            .setUnicodeLocaleKeyword("nu", "thai")
            .build();
        assertEquals("co", "japanese", locale.getUnicodeLocaleType("co"));
        assertEquals("nu", "thai", locale.getUnicodeLocaleType("nu"));
        assertEquals("keys", 2, locale.getUnicodeLocaleKeys().size());

        // can clear a keyword by setting to null, others remain
        String result = builder
            .setUnicodeLocaleKeyword("co", null)
            .build()
            .toLanguageTag();
        assertEquals("empty co", "und-u-nu-thai", result);

        // locale keyword extension goes when all keywords are gone
        result = builder
            .setUnicodeLocaleKeyword("nu", null)
            .build()
            .toLanguageTag();
        assertEquals("empty nu", "und", result);

        // locale keywords are ordered independent of order of addition
        result = builder
            .setUnicodeLocaleKeyword("zz", "012")
            .setUnicodeLocaleKeyword("aa", "345")
            .build()
            .toLanguageTag();
        assertEquals("reordered", "und-u-aa-345-zz-012", result);

        // null keyword throws NPE
        new BuilderNPE("keyword") { public void call() { b.setUnicodeLocaleKeyword(null, "thai"); }};

        // well-formed keywords are two alphanum
        new BuilderILE("a", "abc") { public void call() { b.setUnicodeLocaleKeyword(arg, "value"); }};

        // well-formed values are 3-8 alphanum
        new BuilderILE("ab", "abcdefghi") { public void call() { b.setUnicodeLocaleKeyword("ab", arg); }};
    }

    public void testBuilderPrivateUseExtension() {
        // normalizes hyphens to underscore, case to lower
        String source = "c-B-a";
        String target = "c-b-a";
        Builder builder = new Builder();
        String result = builder
            .setExtension(Locale.PRIVATE_USE_EXTENSION, source)
            .build()
            .getExtension(Locale.PRIVATE_USE_EXTENSION);
        assertEquals("abc", target, result);

        // multiple hyphens are ill-formed
        new BuilderILE("a--b") { public void call() { b.setExtension(Locale.PRIVATE_USE_EXTENSION, arg); }};
    }

    public void testBuilderClear() {
        String monster = "en-latn-US-NewYork-a-bb-cc-u-co-japanese-x-z-y-x-x";
        Builder builder = new Builder();
        Locale locale = Locale.forLanguageTag(monster);
        String result = builder
            .setLocale(locale)
            .clear()
            .build()
            .toLanguageTag();
        assertEquals("clear", "und", result);
    }

    public void testBuilderRemoveUnicodeAttribute() {
        // tested in testBuilderAddUnicodeAttribute
    }

    public void testBuilderBuild() {
        // tested in other test methods
    }

    public void testSerialize() {
        final Locale[] testLocales = {
            Locale.ROOT,
            new Locale("en"),
            new Locale("en", "US"),
            new Locale("en", "US", "Win"),
            new Locale("en", "US", "Win_XP"),
            new Locale("ja", "JP"),
            new Locale("ja", "JP", "JP"),
            new Locale("th", "TH"),
            new Locale("th", "TH", "TH"),
            new Locale("no", "NO"),
            new Locale("nb", "NO"),
            new Locale("nn", "NO"),
            new Locale("no", "NO", "NY"),
            new Locale("nn", "NO", "NY"),
            new Locale("he", "IL"),
            new Locale("he", "IL", "var"),
            new Locale("Language", "Country", "Variant"),
            new Locale("", "US"),
            new Locale("", "", "Java"),
            Locale.forLanguageTag("en-Latn-US"),
            Locale.forLanguageTag("zh-Hans"),
            Locale.forLanguageTag("zh-Hant-TW"),
            Locale.forLanguageTag("ja-JP-u-ca-japanese"),
            Locale.forLanguageTag("und-Hant"),
            Locale.forLanguageTag("und-a-123-456"),
            Locale.forLanguageTag("en-x-java"),
            Locale.forLanguageTag("th-TH-u-ca-buddist-nu-thai-x-lvariant-TH"),
        };

        for (Locale locale : testLocales) {
            try {
                // write
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(locale);

                // read
                ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                ObjectInputStream ois = new ObjectInputStream(bis);
                Object o = ois.readObject();

                assertEquals("roundtrip " + locale, locale, o);
            } catch (Exception e) {
                errln(locale + " encountered exception:" + e.getLocalizedMessage());
            }
        }
    }

    public void testDeserialize6() {
        final String TESTFILEPREFIX = "java6locale_";

        File dataDir = null;
        String dataDirName = System.getProperty("serialized.data.dir");
        if (dataDirName == null) {
            URL resdirUrl = getClass().getClassLoader().getResource("serialized");
            if (resdirUrl != null) {
                try {
                    dataDir = new File(resdirUrl.toURI());
                } catch (URISyntaxException urie) {
                }
            }
        } else {
            dataDir = new File(dataDirName);
        }

        if (dataDir == null) {
            errln("'dataDir' is null. serialized.data.dir Property value is "+dataDirName);
            return;
        } else if (!dataDir.isDirectory()) {
            errln("'dataDir' is not a directory. dataDir: "+dataDir.toString());
            return;
        }

        File[] files = dataDir.listFiles();
        for (File testfile : files) {
            if (testfile.isDirectory()) {
                continue;
            }
            String name = testfile.getName();
            if (!name.startsWith(TESTFILEPREFIX)) {
                continue;
            }
            Locale locale;
            String locStr = name.substring(TESTFILEPREFIX.length());
            if (locStr.equals("ROOT")) {
                locale = Locale.ROOT;
            } else {
                String[] fields = locStr.split("_", 3);
                String lang = fields[0];
                String country = (fields.length >= 2) ? fields[1] : "";
                String variant = (fields.length == 3) ? fields[2] : "";
                locale = new Locale(lang, country, variant);
            }

            // deserialize
            try (FileInputStream fis = new FileInputStream(testfile);
                 ObjectInputStream ois = new ObjectInputStream(fis))
            {
                Object o = ois.readObject();
                assertEquals("Deserialize Java 6 Locale " + locale, o, locale);
            } catch (Exception e) {
                errln("Exception while reading " + testfile.getAbsolutePath() + " - " + e.getMessage());
            }
        }
    }

    public void testBug7002320() {
        // forLanguageTag() and Builder.setLanguageTag(String)
        // should add a location extension for following two cases.
        //
        // 1. language/country are "ja"/"JP" and the resolved variant (x-lvariant-*)
        //    is exactly "JP" and no BCP 47 extensions are available, then add
        //    a Unicode locale extension "ca-japanese".
        // 2. language/country are "th"/"TH" and the resolved variant is exactly
        //    "TH" and no BCP 47 extensions are available, then add a Unicode locale
        //    extension "nu-thai".
        //
        String[][] testdata = {
            {"ja-JP-x-lvariant-JP", "ja-JP-u-ca-japanese-x-lvariant-JP"},   // special case 1
            {"ja-JP-x-lvariant-JP-XXX"},
            {"ja-JP-u-ca-japanese-x-lvariant-JP"},
            {"ja-JP-u-ca-gregory-x-lvariant-JP"},
            {"ja-JP-u-cu-jpy-x-lvariant-JP"},
            {"ja-x-lvariant-JP"},
            {"th-TH-x-lvariant-TH", "th-TH-u-nu-thai-x-lvariant-TH"},   // special case 2
            {"th-TH-u-nu-thai-x-lvariant-TH"},
            {"en-US-x-lvariant-JP"},
        };

        Builder bldr = new Builder();

        for (String[] data : testdata) {
            String in = data[0];
            String expected = (data.length == 1) ? data[0] : data[1];

            // forLanguageTag
            Locale loc = Locale.forLanguageTag(in);
            String out = loc.toLanguageTag();
            assertEquals("Language tag roundtrip by forLanguageTag with input: " + in, expected, out);

            // setLanguageTag
            bldr.clear();
            bldr.setLanguageTag(in);
            loc = bldr.build();
            out = loc.toLanguageTag();
            assertEquals("Language tag roundtrip by Builder.setLanguageTag with input: " + in, expected, out);
        }
    }

    public void testBug7023613() {
        String[][] testdata = {
            {"en-Latn", "en__#Latn"},
            {"en-u-ca-japanese", "en__#u-ca-japanese"},
        };

        for (String[] data : testdata) {
            String in = data[0];
            String expected = (data.length == 1) ? data[0] : data[1];

            Locale loc = Locale.forLanguageTag(in);
            String out = loc.toString();
            assertEquals("Empty country field with non-empty script/extension with input: " + in, expected, out);
        }
    }

    /*
     * 7033504: (lc) incompatible behavior change for ja_JP_JP and th_TH_TH locales
     */
    public void testBug7033504() {
        checkCalendar(new Locale("ja", "JP", "jp"), "java.util.GregorianCalendar");
        checkCalendar(new Locale("ja", "jp", "jp"), "java.util.GregorianCalendar");
        checkCalendar(new Locale("ja", "JP", "JP"), "java.util.JapaneseImperialCalendar");
        checkCalendar(new Locale("ja", "jp", "JP"), "java.util.JapaneseImperialCalendar");
        checkCalendar(Locale.forLanguageTag("en-u-ca-japanese"),
                      "java.util.JapaneseImperialCalendar");

        checkDigit(new Locale("th", "TH", "th"), '0');
        checkDigit(new Locale("th", "th", "th"), '0');
        checkDigit(new Locale("th", "TH", "TH"), '\u0e50');
        checkDigit(new Locale("th", "TH", "TH"), '\u0e50');
        checkDigit(Locale.forLanguageTag("en-u-nu-thai"), '\u0e50');
    }

    private void checkCalendar(Locale loc, String expected) {
        Calendar cal = Calendar.getInstance(loc);
        assertEquals("Wrong calendar", expected, cal.getClass().getName());
    }

    private void checkDigit(Locale loc, Character expected) {
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(loc);
        Character zero = dfs.getZeroDigit();
        assertEquals("Wrong digit zero char", expected, zero);
    }

    ///
    /// utility asserts
    ///

    private void assertTrue(String msg, boolean v) {
        if (!v) {
            errln(msg + ": expected true");
        }
    }

    private void assertFalse(String msg, boolean v) {
        if (v) {
            errln(msg + ": expected false");
        }
    }

    private void assertEquals(String msg, Object e, Object v) {
        if (e == null ? v != null : !e.equals(v)) {
            if (e != null) {
                e = "'" + e + "'";
            }
            if (v != null) {
                v = "'" + v + "'";
            }
            errln(msg + ": expected " + e + " but got " + v);
        }
    }

    private void assertNotEquals(String msg, Object e, Object v) {
        if (e == null ? v == null : e.equals(v)) {
            if (e != null) {
                e = "'" + e + "'";
            }
            errln(msg + ": expected not equal " + e);
        }
    }

    private void assertNull(String msg, Object o) {
        if (o != null) {
            errln(msg + ": expected null but got '" + o + "'");
        }
    }

    private void assertNotNull(String msg, Object o) {
        if (o == null) {
            errln(msg + ": expected non null");
        }
    }

    // not currently used, might get rid of exceptions from the API
    private abstract class ExceptionTest {
        private final Class<? extends Exception> exceptionClass;

        ExceptionTest(Class<? extends Exception> exceptionClass) {
            this.exceptionClass = exceptionClass;
        }

        public void run() {
            String failMsg = null;
            try {
                call();
                failMsg = "expected " + exceptionClass.getName() + "  but no exception thrown.";
            }
            catch (Exception e) {
                if (!exceptionClass.isAssignableFrom(e.getClass())) {
                    failMsg = "expected " + exceptionClass.getName() + " but caught " + e;
                }
            }
            if (failMsg != null) {
                String msg = message();
                msg = msg == null ? "" : msg + " ";
                errln(msg + failMsg);
            }
        }

        public String message() {
            return null;
        }

        public abstract void call();
    }

    private abstract class ExpectNPE extends ExceptionTest {
        ExpectNPE() {
            super(NullPointerException.class);
            run();
        }
    }

    private abstract class BuilderNPE extends ExceptionTest {
        protected final String msg;
        protected final Builder b = new Builder();

        BuilderNPE(String msg) {
            super(NullPointerException.class);

            this.msg = msg;

            run();
        }

        public String message() {
            return msg;
        }
    }

    private abstract class ExpectIAE extends ExceptionTest {
        ExpectIAE() {
            super(IllegalArgumentException.class);
            run();
        }
    }

    private abstract class BuilderILE extends ExceptionTest {
        protected final String[] args;
        protected final Builder b = new Builder();

        protected String arg; // mutates during call

        BuilderILE(String... args) {
            super(IllformedLocaleException.class);

            this.args = args;

            run();
        }

        public void run() {
            for (String arg : args) {
                this.arg = arg;
                super.run();
            }
        }

        public String message() {
            return "arg: '" + arg + "'";
        }
    }
}
