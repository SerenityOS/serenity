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
 * @bug 8179071 8202537 8231273 8251317
 * @summary Test that language aliases of CLDR supplemental metadata are handled correctly.
 * @modules jdk.localedata
 * @run main/othervm -Djava.locale.providers=CLDR Bug8179071
 */

/**
 * This fix is dependent on a particular version of CLDR data.
 */

import java.time.Month;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Bug8179071 {

    // Deprecated and Legacy tags.
    // As of CLDR 38, language aliases for some of the legacy tags have been removed.
    private static final Set<String> LegacyAliases = Set.of(
            "zh-guoyu", "zh-min-nan", "i-klingon", "i-tsu",
            "sgn-CH-DE", "mo", "i-tay", "scc",
            "i-hak", "sgn-BE-FR", "i-lux", "tl", "zh-hakka", "i-ami", "aa-SAAHO",
            "zh-xiang", "i-pwn", "sgn-BE-NL", "jw", "sh", "i-bnn");
    // expected month format data for  locales after language aliases replacement.
    private static final Map<String, String> shortJanuaryNames = Map.of( "pa-PK", "\u0a1c\u0a28",
                                                          "uz-AF" , "yan",
                                                          "sr-ME", "\u0458\u0430\u043d",
                                                          "scc", "\u0458\u0430\u043d",
                                                          "sh", "jan",
                                                          "ha-Latn-NE", "Jan",
                                                          "i-lux", "Jan.");


    private static void test(String tag, String expected) {
        Locale target = Locale.forLanguageTag(tag);
        Month day = Month.JANUARY;
        TextStyle style = TextStyle.SHORT;
        String actual = day.getDisplayName(style, target);
        if (!actual.equals(expected)) {
            throw new RuntimeException("failed for locale  " + tag + " actual output " + actual +"  does not match with  " + expected);
        }
    }

    /**
     * getAvailableLocales() should not contain any deprecated or Legacy language tags
     */
    private static void checkInvalidTags() {
        Set<String> invalidTags = new HashSet<>();
        Arrays.asList(Locale.getAvailableLocales()).stream()
                .map(loc -> loc.toLanguageTag())
                .forEach( tag -> {if(LegacyAliases.contains(tag)) {invalidTags.add(tag);}});
        if (!invalidTags.isEmpty()) {
          throw new RuntimeException("failed: Deprecated and Legacy tags found  " + invalidTags  + " in AvailableLocales ");
        }
    }

    public static void main(String[] args) {
        shortJanuaryNames.forEach(Bug8179071::test);
        checkInvalidTags();
    }
}
