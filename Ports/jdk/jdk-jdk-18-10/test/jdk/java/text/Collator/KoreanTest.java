/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test 1.1 02/09/12
 * @bug 4176141 4655819
 * @summary Regression tests for Korean Collation
 * @modules jdk.localedata
 */

import java.text.*;
import java.util.*;

public class KoreanTest {

    // NOTE:
    //   Golden data in this test case is locale data dependent and
    //   may need to be changed if the Korean locale collation rules
    //   are changed.

    // And, CollationDecomp has been set to 0(NO_DECOMPOSITION) in
    // LocaleElements_ko.java.
    // This is very important to consider what is correct behavior in
    // Korean Collator. Sometimes different from other locales.

    /*
     * TERTIARY(default): s1 < s2,  SECONDARY: s1 < s2,  PRIMARY: s1 < s2
     */
    static final String[][] compData1 = {
        /*
         * Data to verify '<' relationship in LocaleElements_ja.java
         */
        {"\uACE0\uC591\uC774", "\u732B",
         "Hangul \"Cat\"(0xACE0 0xC591 0xC774) <---> Chinese Kanji \"Cat\"(0x732B)"},
        {"\u30FB", "\u2025",
         "Katakana middle dot(0x30FB) <---> Two dot leader(0x2025)"},

        {"\u00B1", "\u2260",
         "Plus-Minus Sign(0x00B1) <---> Not Equal To(0x2260)"},
        {"\u3011", "\u2260",
         "Right Black Lenticular Bracket(0x3011) <---> Not Equal To(0x2260)"},
        {"\u2260", "\u2103",
         "Not Equal To(0x2260) <---> Degree Celsius(0x2103)"},
        {"\u2260", "\u2606",
         "Not Equal To(0x2260) <---> White Star(0x2606)"},

        // Unlike other locales' Collator, compare() returns -1 because of
        // NO_DECOMPOSITION.
        /* above "assumption" is no longer true, now we do normalize ("decomposition")
           for the pattern in ko locale, but exclude those hangul syllables, so the
           test case below need to be excluded from tiger/1.5
        {"\u003D\u0338", "\u2260",
         "Equal(0x003D)Combining Long Solidus Overlay(0x0338) <---> Not Equal To(0x2260)"},
         */
    };

    /*
     * TERTIARY(default): s1 = s2,  SECONDARY: s1 = s2,  PRIMARY: s1 = s2
     */
    static final String[][] compData2 = {
        // Verify a character which has been added since Unicode 2.1.X.
        {"\u798F", "\uFA1B",
         "CJK Unified Ideograph \"FUKU\"(0x798F) <---> CJK Compatibility Ideograph \"FUKU\"(0xFA1B)"},

    };

    Collator col = Collator.getInstance(Locale.KOREA);
    int result = 0;

    public static void main(String[] args) throws Exception {
        new KoreanTest().run();
    }

    public void run() {
        //
        // Test for TERTIARY(default)
        //
        doCompare(compData1);
        doEquals(compData2);

        //
        // Test for SECONDARY
        //
        col.setStrength(Collator.SECONDARY);
        doCompare(compData1);
        doEquals(compData2);

        //
        // Test for PRIMARY
        //
        col.setStrength(Collator.PRIMARY);
        doCompare(compData1);
        doEquals(compData2);

        if (result !=0) {
            throw new RuntimeException("Unexpected results on Korean collation.");
        }
    }

    /* compare() should return -1 for each combination. */
    void doCompare(String[][] s) {
        int value;
        for (int i=0; i < s.length; i++) {
            if ((value = col.compare(s[i][0], s[i][1])) > -1) {
                result++;
                System.err.println("TERTIARY: The first string should be less than the second string:  " +
                    s[i][2] + "  compare() returned " + value + ".");
            }
        }
    }

    /* equals() should return true for each combination. */
    void doEquals(String[][] s) {
        for (int i=0; i < s.length; i++) {
            if (!col.equals(s[i][0], s[i][1])) {
                result++;
                System.err.println("TERTIARY: The first string should be equals to the second string:  " +
                    s[i][2] + "  compare() returned " +
                    col.compare(s[i][0], s[i][1] + "."));
            }
        }
    }
}
