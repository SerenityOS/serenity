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
 * @test 1.1 02/09/11
 * @bug 4176141 4655819
 * @summary Regression tests for Japanese Collation
 * @modules jdk.localedata
 */

import java.text.*;
import java.util.*;

public class JapaneseTest {

    // NOTE:
    //   Golden data in this test case is locale data dependent and
    //   may need to be changed if the Japanese locale collation rules
    //   are changed.

    /*
     *                    | NO_DECOMP(default) | CANONICAL_DECOMP | FULL_DECOMP
     * -------------------+--------------------+------------------+-------------
     *  PRIMARY           | s1 < s2 (-1)       | s1 < s2 (-1)     | s1 < s2 (-1)
     *  SECONDARY         | s1 < s2 (-1)       | s1 < s2 (-1)     | s1 < s2 (-1)
     *  TERTIARY(default) | S1 < s2 (-1)       | s1 < s2 (-1)     | s1 < s2 (-1)
     */
    static final int[][] results1 = {
        { -1, -1, -1},
        { -1, -1, -1},
        { -1, -1, -1},
    };
    static final String[][] compData1 = {
        /*
         * Data to verify '<' relationship in LocaleElements_ja.java
         */
        {"\u3084", "\u30E6",
         "Hiragana \"YA\"(0x3084) <---> Katakana \"YU\"(0x30E6)"},
        {"\u30E6", "\u3088",
         "Katakana \"YU\"(0x30E6) <---> Hiragana \"YO\"(0x3088)"},
        {"\u00B1", "\u2260",
         "Plus-Minus Sign(0x00B1) <---> Not Equal To(0x2260)"},
        {"\u3011", "\u2260",
         "Right Black Lenticular Bracket(0x3011) <---> Not Equal To(0x2260)"},
        {"\u2260", "\u2103",
         "Not Equal To(0x2260) <---> Degree Celsius(0x2103)"},
        {"\u2260", "\u2606",
         "Not Equal To(0x2260) <---> White Star(0x2606)"},
        {"\u30FD", "\u309E",
         "Katakana Iteration Mark(0x30FD) <---> Hiragana Voiced Iteration Mark(0x309E)"},
        {"\u3059\u309D", "\u3059\u309E",
         "Hiragana \"SU\"(0x3059)Hiragana Iteration Mark(0x309D) <---> Hiragana \"SU\"(0x3059)Hiragana Voiced Iteration Mark(0x309E)"},
        {"\u821E", "\u798F",
         "CJK Unified Ideograph(0x821E) <---> CJK Unified Ideograph(0x798F)"},

        /*
         * Data to verify normalization
         */
        {"\u2260", "\u225F",
         "Not Equal To(0x2260) <---> Questioned Equal To(0x225F)"},
        {"\u226E", "\u2260",
         "Not Less-than(0x226E) <---> Not Equal To(0x2260)"},
        {"\u226E", "\u226D",
         "Not Less-than(0x226E) <---> Not Equivalent To(0x226D)"},
    };

    /*
     *                    | NO_DECOMP(default) | CANONICAL_DECOMP | FULL_DECOMP
     * -------------------+--------------------+------------------+-------------
     *  PRIMARY           | s1 = s2 (0)        | s1 = s2 (0)      | s1 = s2 (0)
     *  SECONDARY         | s1 < s2 (-1)       | s1 < s2 (-1)     | s1 < s2 (-1)
     *  TERTIARY(default) | S1 < s2 (-1)       | s1 < s2 (-1)     | s1 < s2 (-1)
     */
    static final int[][] results2 = {
        {  0,  0,  0},
        { -1, -1, -1},
        { -1, -1, -1},
    };
    static final String[][] compData2 = {
        /*
         * Data to verify ';' relationship in LocaleElements_ja.java
         */
        {"\u3099", "\u309A",
         "Combining Katakana-Hiragana Voiced Sound Mark(0x3099) <---> Combining Katakana-Hiragana Semi-voiced Sound Mark(0x309A)"},
        {"\u3053\u3046\u3068\u3046", "\u3053\u3046\u3068\u3099\u3046",
         "Hiragana \"KOUTOU\"(0x3053 0x3046 0x3068 0x3046) <---> Hiragana \"KOUTO\"(0x3053 0x3046 0x3068)Combining Katakana-Hiragana Voiced Sound Mark(0X3099)\"U\"(0x3046)"},
        {"\u3053\u3046\u3068\u3046", "\u3053\u3046\u3069\u3046",
         "Hiragana \"KOUTOU\"(0x3053 0x3046 0x3068 0x3046) <---> Hiragana \"KOUDOU\"(0x3053 0x3046 0x3069 0x3046)"},
        {"\u3053\u3046\u3069\u3046", "\u3054\u3046\u3068\u3046",
         "Hiragana \"KOUTOU\"(0x3053 0x3046 0x3069 0x3046) <---> Hiragana \"GOUTOU\"(0x3054 0x3046 0x3068 0x3046)"},
        {"\u3054\u3046\u3068\u3046", "\u3054\u3046\u3069\u3046",
         "Hiragana \"GOUTOU\"(0x3054 0x3046 0x3068 0x3046) <---> Hiragana \"GOUDOU\"(0x3054 0x3046 0x3069 0x3046)"},
    };

    /*
     *                    | NO_DECOMP(default) | CANONICAL_DECOMP | FULL_DECOMP
     * -------------------+--------------------+------------------+-------------
     *  PRIMARY           | s1 = s2 (0)        | s1 = s2 (0)      | s1 = s2 (0)
     *  SECONDARY         | s1 = s2 (0)        | s1 = s2 (0)      | s1 = s2 (0)
     *  TERTIARY(default) | S1 < s2 (-1)       | s1 < s2 (-1)     | s1 < s2 (-1)
     */
    static final int[][] results3 = {
        {  0,  0,  0},
        {  0,  0,  0},
        { -1, -1, -1},
    };
    static final String[][] compData3 = {
        /*
         * Data to verify ',' relationship in LocaleElements_ja.java
         */
        {"\u3042", "\u3041",
         "Hiragana \"A\"(0x3042) <---> Hiragana \"a\"(0x3041)"},
        {"\u3041", "\u30A2",
         "Hiragana \"a\"(0x3041) <---> Katakana \"A\"(0x30A2)"},
        {"\u30A2", "\u30A1",
         "Katakana \"A\"(0x30A2) <---> Katakana \"a\"(0x30A1)"},
        {"\u3094", "\u30F4",
         "Hiragana \"VU\"(0x3094) <---> Katakana \"VU\"(0x30F4)"},
        {"\u3094", "\u30A6\u3099",
         "Hiragana \"VU\"(0x3094) <---> Katakana \"U\"(0x30A6)Combining Katakana-Hiragana Voiced Sound Mark(0x3099)"},
        {"\u3046\u3099", "\u30F4",
         "Hiragana \"U\"(0x3046)Combining Katakana-Hiragana Voiced Sound Mark(0x3099) <---> Katakana \"VU\"(0x30F4)"},
        {"\u3046\u3099", "\u30A6\u3099",
         "Hiragana \"U\"(0x3046)Combining Katakana-Hiragana Voiced Sound Mark(0x3099) <---> Katakana \"U\"(0x30A6)Combining Katakana-Hiragana Voiced Sound Mark(0x3099)"},
        {"\u30AB\u30A2", "\u30AB\u30FC",
         "Katakana \"KAA\"(0x30AB 0x30A2) <---> Katakana \"KA-\"(0x30AB 0x30FC)"},
        {"\u30CB\u30A1\u30A2", "\u30CB\u30A1\u30FC",
         "Katakana \"NyaA\"(0x30CB 0x30A1 0x30A2) <---> Katakana \"Nya-\"(0x30CB 0x30A1 0x30FC)"},
        {"\u30B3\u30AA\u30D2\u30A4", "\u30B3\u30FC\u30D2\u30FC",
         "Katakana \"KOOHII\"(0x30B3 0x30AA 0x30D2 0x30A4) <---> Katakana \"KO-HI-\"(0x30B3 0x30FC 0x30D2 0x30FC)"},
        {"\u308A\u3088\u3046", "\u308A\u3087\u3046",
         "Hiragana \"RIYOU\"(0x308A 0x3088 0x3046) <---> Hiragana \"Ryou\"(0x308A 0x3087 0x3046)"},
        {"\u3081\u3064\u304D", "\u3081\u3063\u304D",
         "Hiragana \"METSUKI\"(0x3081 0x3064 0x304D) <---> Hiragana \"MEKKI\"(0x3081 0x3063 0x304D)"},
        {"\u3075\u3042\u3093", "\u30D5\u30A1\u30F3",
         "Hiragana \"FUAN\"(0x3075 0x3042 0x3093) <---> Katakana \"FUaN\"(0x30D5 0x30A1 0x30F3)"},
        {"\u3075\u3041\u3093", "\u30D5\u30A2\u30F3",
         "Hiragana \"FUaN\"(0x3075 0x3041 0x3093) <---> Katakana \"FUAN\"(0x30D5 0x30A2 0x30F3)"},
        {"\u30D5\u30A2\u30F3", "\u30D5\u30A1\u30F3",
         "Katakana \"FUAN\"(0x30D5 0x30A2 0x30F3) <---> Katakana \"FUaN\"(0x30D5 0x30A1 0x30F3)"},
    };

    /*
     *                    | NO_DECOMP(default) | CANONICAL_DECOMP | FULL_DECOMP
     * -------------------+--------------------+------------------+-------------
     *  PRIMARY           | s1 = s2 (0)        | s1 = s2 (0)      | s1 = s2 (0)
     *  SECONDARY         | s1 = s2 (0)        | s1 = s2 (0)      | s1 = s2 (0)
     *  TERTIARY(default) | S1 = s2 (0)        | s1 = s2 (0)      | s1 = s2 (0)
     */
    static final int[][] results4 = {
        {  0,  0,  0},
        {  0,  0,  0},
        {  0,  0,  0},
    };
    static final String[][] compData4 = {
        /*
         * Data to verify Japanese normalization
         */
        {"\u309E", "\u309D\u3099",
         "Hiragana Voiced Iteration Mark(0x309E) <---> Hiragana Iteration Mark(0x309D)Combining Katakana-Hiragana Voiced Sound Mark(0x3099)"},
        {"\u30FE", "\u30FD\u3099",
         "Katakana Voiced Iteration Mark(0x30FE) <---> Katakana iteration mark(0x30FD)Combining Katakana-Hiragana Voiced Sound Mark(0x3099)"},
        {"\u306F\u3099", "\u3070",
         "Hiragana \"HA\"(0x306F)Combining Katakana-Hiragana Voiced Sound Mark(0x3099) <---> Hiragana \"BA\"(0x3070)"},
        {"\u306F\u309A", "\u3071",
         "Hiragana \"HA\"(0x306F)Combining Katakana-Hiragana Semi-voiced Sound Mark(0x309A) <---> Hiragana \"PA\"(0x3071)"},
        {"\u30EF\u3099", "\u30F7",
         "Katakana \"WA\"(0x306F)Combining Katakana-Hiragana Voiced Sound Mark(0x3099) <---> Katakana \"VA\"(0x30F7)"},
        {"\u30F0\u3099", "\u30F8",
         "Katakana \"WI\"(0x30F0)Combining Katakana-Hiragana Voiced Sound Mark(0x3099) <---> Katakana \"VI\"(0x30F8)"},
        {"\u30F1\u3099", "\u30F9",
         "Katakana \"WE\"(0x30F1)Combining Katakana-Hiragana Voiced Sound Mark(0x3099) <---> Katakana \"VE\"(0x30F9)"},
        {"\u30F2\u3099", "\u30FA",
         "Katakana \"WO\"(0x30F2)Combining Katakana-Hiragana Voiced Sound Mark(0x3099) <---> Katakana \"VO\"(0x30FA)"},
        {"\u3046\u3099", "\u3094",
         "Hiragana \"U\"(0x3046)Combining Katakana-Hiragana Voiced Sound Mark(0x3099) <---> Hiragana \"VU\"(0x3094)"},
        {"\u30A6\u3099", "\u30F4",
         "Katakana \"U\"(0x30A6)Combining Katakana-Hiragana Voiced Sound Mark(0x3099) <---> Katakana \"VU\"(0x30F4)"},

        // verify normalization
        {"\u2260", "\u003D\u0338",
         "Not Equal To(0x2260) <---> Equal(0x003D)Combining Long Solidus Overlay(0x0338)"},
        {"\u2262", "\u2261\u0338",
         "Not Identical To(0x2262) <---> Identical To(0x2261)Combining Long Solidus Overlay(0x0338)"},
        {"\u226E", "\u003C\u0338",
         "Not Less-than(0x226E) <---> Less-than Sign(0x003C)Combining Long Solidus Overlay(0x0338)"},

        // Verify a character which has been added since Unicode 2.1.X.
        {"\u798F", "\uFA1B",
         "CJK Unified Ideograph \"FUKU\"(0x798F) <---> CJK Compatibility Ideograph \"FUKU\"(0xFA1B)"},
    };

    /*
     *                    | NO_DECOMP(default) | CANONICAL_DECOMP | FULL_DECOMP
     * -------------------+--------------------+------------------+-------------
     *  PRIMARY           | s1 > s2 (1)        | s1 = s2 (0)      | s1 = s2 (0)
     *  SECONDARY         | s1 > s2 (1)        | s1 = s2 (0)      | s1 = s2 (0)
     *  TERTIARY(default) | S1 > s2 (1)        | s1 = s2 (0)      | s1 = s2 (0)
     */
    static final int[][] results5 = {
        {  1,  0,  0},
        {  1,  0,  0},
        {  1,  0,  0},
    };
    static final String[][] compData5 = {
        /*
         * Data to verify normalization
         */
        {"\u226D", "\u224D\u0338",
         "Not Equivalent To(0x226D) <---> Equivalent To(0x224D)Combining Long Solidus Overlay(0x0338)"},
    };

    static final int[][] results6 = {
        {  1, -1, -1},
        {  1, -1, -1},
        {  1, -1, -1},
    };
    static final String[][] compData6 = {
        /*
         * Data to verify normalization
         */
        {"\u226D", "\u226C",
         "Not Equivalent To(0x226D) <---> Between(0x226C)"},
        {"\u226D", "\u225F",
         "Not Equivalent To(0x226D) <---> Questioned Equal To(0x225F)"},
    };


    /*
     * The following data isn't used at the moment because iteration marks
     * aren't supported now.
     */
    static final String[][] compData0 = {
        {"\u307F\u307F", "\u307F\u309D",
         "Hiragana \"MIMI\"(0x307F 0x307F) <---> Hiragana \"MI\"(0x307F)Hiragana Iteration Mark(0x309D)"},
        {"\u3044\u3059\u305A", "\u3044\u3059\u309E",
         "Hiragana \"ISUZU\"(0x3044 0x3059 0x305A) <---> Hiragana \"ISU\"(0x3044 0x3059)Hiragana Voiced Iteration Mark(0x309E)"},
        {"\u30DF\u30DF", "\u30DF\u30FD",
         "Katakana \"MIMI\"(0x30DF 0x30DF) <---> Katakana \"MI\"(0x30DF)Katakana Iteration Mark(0x30FD)"},
        {"\u30A4\u30B9\u30BA", "\u30A4\u30B9\u30FE",
         "Katakana \"ISUZU\"(0x30A4 0x30B9 0x30BA) <---> Katakana \"ISU\"(0x30A4 0x30B9)Katakana Voiced Iteration Mark(0x30FE)"},
    };


    static final String[] decomp_name = {
        "NO_DECOMP", "CANONICAL_DECOMP", "FULL_DECOMP"
    };

    static final String[] strength_name = {
        "PRIMARY", "SECONDARY", "TERTIARY"
    };


    Collator col = Collator.getInstance(Locale.JAPAN);
    int result = 0;

    public static void main(String[] args) throws Exception {
        new JapaneseTest().run();
    }

    public void run() {
        // Use all available localse on the initial testing....
        // Locale[] locales = Locale.getAvailableLocales();
        Locale[] locales = { Locale.getDefault() };

        for (int l = 0; l < locales.length; l++) {
            Locale.setDefault(locales[l]);

            for (int decomp = 0; decomp < 3; decomp++) {// See decomp_name.
                col.setDecomposition(decomp);

                for (int strength = 0; strength < 3; strength++) {// See strength_name.
//                  System.err.println("\n" + locales[l] + ": " + strength_name[strength] + " --- " + decomp_name[decomp]);

                    col.setStrength(strength);
                    doCompare(compData1, results1[strength][decomp], strength, decomp);
                    doCompare(compData2, results2[strength][decomp], strength, decomp);
                    doCompare(compData3, results3[strength][decomp], strength, decomp);
                    doCompare(compData4, results4[strength][decomp], strength, decomp);
                    doCompare(compData5, results5[strength][decomp], strength, decomp);
                    doCompare(compData6, results6[strength][decomp], strength, decomp);
                }
            }
        }

        /* Check result */
        if (result !=0) {
            throw new RuntimeException("Unexpected results on Japanese collation.");
        }
    }

    void doCompare(String[][] s, int expectedValue, int strength, int decomp) {
        int value;
        for (int i=0; i < s.length; i++) {
            if ((value = col.compare(s[i][0], s[i][1])) != expectedValue) {
                result++;
                System.err.println(strength_name[strength] +
                                   ": compare() returned unexpected value.(" +
                                   value + ") on " + decomp_name[decomp] +
                                   "     Expected(" + expectedValue +
                                   ") for " + s[i][2]);
            }
        }
    }
}
