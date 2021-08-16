/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @test
 * @bug 8176841 8202537
 * @summary Tests the display names for BCP 47 U extensions
 * @modules jdk.localedata
 * @run testng/othervm -Djava.locale.providers=CLDR DisplayNameTests
 */

import static org.testng.Assert.assertEquals;

import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test Locale.getDisplayName() with BCP47 U extensions. Note that the
 * result may change depending on the CLDR releases.
 */
@Test
public class DisplayNameTests {
    private static final Locale loc1 = Locale.forLanguageTag("en-Latn-US-u" +
                                                             "-ca-japanese" +
                                                             "-cf-account" +
                                                             "-co-pinyin" +
                                                             "-cu-jpy" +
                                                             "-em-emoji" +
                                                             "-fw-wed" +
                                                             "-hc-h23" +
                                                             "-lb-loose" +
                                                             "-lw-breakall" +
                                                             "-ms-uksystem" +
                                                             "-nu-roman" +
                                                             "-rg-gbzzzz" +
                                                             "-sd-gbsct" +
                                                             "-ss-standard" +
                                                             "-tz-jptyo" +
                                                             "-va-posix");
    private static final Locale loc2 = new Locale("ja", "JP", "JP");
    private static final Locale loc3 = new Locale.Builder()
                                            .setRegion("US")
                                            .setScript("Latn")
                                            .setUnicodeLocaleKeyword("ca", "japanese")
                                            .build();
    private static final Locale loc4 = new Locale.Builder()
                                            .setRegion("US")
                                            .setUnicodeLocaleKeyword("ca", "japanese")
                                            .build();
    private static final Locale loc5 = new Locale.Builder()
                                            .setUnicodeLocaleKeyword("ca", "japanese")
                                            .build();
    private static final Locale loc6 = Locale.forLanguageTag( "zh-CN-u-ca-dddd-nu-ddd-cu-ddd-fw-moq-tz-unknown-rg-twzz");

    @DataProvider(name="locales")
    Object[][] tz() {
        return new Object[][] {
            // Locale for display, Test Locale, Expected output,
            {Locale.US, loc1,
            "English (Latin, United States, Japanese Calendar, Accounting Currency Format, Pinyin Sort Order, Currency: Japanese Yen, Prefer Emoji Presentation For Emoji Characters, First Day of Week Is Wednesday, 24 Hour System (0\u201323), Loose Line Break Style, Allow Line Breaks In All Words, Imperial Measurement System, Roman Numerals, Region For Supplemental Data: United Kingdom, Region Subdivision: gbsct, Suppress Sentence Breaks After Standard Abbreviations, Time Zone: Japan Time, POSIX Compliant Locale)"},
            {Locale.JAPAN, loc1,
            "\u82f1\u8a9e (\u30e9\u30c6\u30f3\u6587\u5b57\u3001\u30a2\u30e1\u30ea\u30ab\u5408\u8846\u56fd\u3001\u548c\u66a6\u3001\u4f1a\u8a08\u901a\u8ca8\u30d5\u30a9\u30fc\u30de\u30c3\u30c8\u3001\u30d4\u30f3\u30a4\u30f3\u9806\u3001\u901a\u8ca8: \u65e5\u672c\u5186\u3001em: emoji\u3001fw: wed\u300124\u6642\u9593\u5236(0\u301c23)\u3001\u7981\u5247\u51e6\u7406(\u5f31)\u3001lw: breakall\u3001\u30e4\u30fc\u30c9\u30fb\u30dd\u30f3\u30c9\u6cd5\u3001\u30ed\u30fc\u30de\u6570\u5b57\u3001rg: \u30a4\u30ae\u30ea\u30b9\u3001sd: gbsct\u3001ss: standard\u3001\u30bf\u30a4\u30e0\u30be\u30fc\u30f3: \u65e5\u672c\u6642\u9593\u3001\u30ed\u30b1\u30fc\u30eb\u306e\u30d0\u30ea\u30a2\u30f3\u30c8: posix)"},
            {Locale.forLanguageTag("hi-IN"), loc1,
            "\u0905\u0902\u0917\u094d\u0930\u0947\u091c\u093c\u0940 (\u0932\u0948\u091f\u093f\u0928, \u0938\u0902\u092f\u0941\u0915\u094d\u0924 \u0930\u093e\u091c\u094d\u092f, \u091c\u093e\u092a\u093e\u0928\u0940 \u092a\u0902\u091a\u093e\u0902\u0917, \u0932\u0947\u0916\u093e\u0902\u0915\u0928 \u092e\u0941\u0926\u094d\u0930\u093e \u092a\u094d\u0930\u093e\u0930\u0942\u092a, \u092a\u093f\u0928\u092f\u0940\u0928 \u0935\u0930\u094d\u0917\u0940\u0915\u0930\u0923, \u092e\u0941\u0926\u094d\u0930\u093e: \u091c\u093e\u092a\u093e\u0928\u0940 \u092f\u0947\u0928, em: emoji, fw: wed, 24 \u0918\u0902\u091f\u094b\u0902 \u0915\u0940 \u092a\u094d\u0930\u0923\u093e\u0932\u0940 (0\u201323), \u0922\u0940\u0932\u0940 \u092a\u0902\u0915\u094d\u0924\u093f \u0935\u093f\u091a\u094d\u091b\u0947\u0926 \u0936\u0948\u0932\u0940, lw: breakall, \u0907\u092e\u094d\u092a\u0940\u0930\u093f\u092f\u0932 \u092e\u093e\u092a\u0928 \u092a\u094d\u0930\u0923\u093e\u0932\u0940, \u0930\u094b\u092e\u0928 \u0938\u0902\u0916\u094d\u092f\u093e\u090f\u0901, rg: \u092f\u0942\u0928\u093e\u0907\u091f\u0947\u0921 \u0915\u093f\u0902\u0917\u0921\u092e, sd: gbsct, ss: standard, \u0938\u092e\u092f \u0915\u094d\u0937\u0947\u0924\u094d\u0930: \u091c\u093e\u092a\u093e\u0928 \u0938\u092e\u092f, \u0938\u094d\u0925\u093e\u0928\u0940\u092f \u092a\u094d\u0930\u0915\u093e\u0930: posix)"},

            // cases where no localized types are available. fall back to "key: type"
            {Locale.US, Locale.forLanguageTag("en-u-ca-unknown"), "English (Calendar: unknown)"},

            // cases with variant, w/o language, script
            {Locale.US, loc2, "Japanese (Japan, JP, Japanese Calendar)"},
            {Locale.US, loc3, "Latin (United States, Japanese Calendar)"},
            {Locale.US, loc4, "United States (Japanese Calendar)"},
            {Locale.US, loc5, ""},

            // invalid cases
            {loc6, loc6, "\u4e2d\u6587 (\u4e2d\u56fd\uff0c\u65e5\u5386\uff1adddd\uff0c\u8d27\u5e01\uff1addd\uff0cfw\uff1amoq\uff0c\u6570\u5b57\uff1addd\uff0crg\uff1atwzz\uff0c\u65f6\u533a\uff1aunknown)"},
            {Locale.US, loc6, "Chinese (China, Calendar: dddd, Currency: ddd, First day of week: moq, Numbers: ddd, Region For Supplemental Data: twzz, Time Zone: unknown)"},
        };
    }

    @Test(dataProvider="locales")
    public void test_locales(Locale inLocale, Locale testLocale, String expected) {
        String result = testLocale.getDisplayName(inLocale);
        assertEquals(result, expected);
    }
}
