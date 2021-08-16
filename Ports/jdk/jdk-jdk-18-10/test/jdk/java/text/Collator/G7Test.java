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
 * @test
 * @library /java/text/testlib
 * @summary test G7 Collation
 * @modules jdk.localedata
 */
/*
 *
 *
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996, 1997 - All Rights Reserved
 *
 * Portions copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
*/

/**
 * G7 Test cases
 *
 * @author     Helena Shih
 */

import java.text.Collator;
import java.text.RuleBasedCollator;
import java.util.Locale;

// G7 test program for printing out test results

public class G7Test extends CollatorTest {

    public static void main(String[] args) throws Exception {
        new G7Test().run(args);
    }

    private static final String testCases[] = {
        "black-birds",              // 0
        "Pat",                      // 1
        "p\u00E9ch\u00E9",          // 2
        "p\u00EAche",               // 3
        "p\u00E9cher",              // 4
        "p\u00EAcher",              // 5
        "Tod",                      // 6
        "T\u00F6ne",                // 7
        "Tofu",                     // 8
        "blackbirds",               // 9
        "Ton",                      // 10
        "PAT",                      // 11
        "blackbird",                // 12
        "black-bird",               // 13
        "pat",                      // 14
        // Additional tests
        "czar",                     // 15
        "churo",                    // 16
        "cat",                      // 17
        "darn",                     // 18
        "?",                        // 19
        "quick",                    // 20
        "#",                        // 21
        "&",                        // 22
        "aardvark",                 // 23
        "a-rdvark",                 // 24
        "abbot",                    // 25
        "coop",                     // 26
        "co-p",                     // 27
        "cop",                      // 28
        "zebra"                     // 29
    };

    // loop to TOTALTESTSET
    private static final int[][] G7Results = {
        { 12, 13,  9,  0, 14,  1, 11,  2,  3,  4,
           5,  6,  8, 10,  7, 31, 31, 31, 31, 31,
          31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, // en_US
        { 12, 13,  9,  0, 14,  1, 11,  2,  3,  4,
           5,  6,  8, 10,  7, 31, 31, 31, 31, 31,
          31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, // en_GB
        { 12, 13,  9,  0, 14,  1, 11,  2,  3,  4,
           5,  6,  8, 10,  7, 31, 31, 31, 31, 31,
          31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, // en_CA
        { 12, 13,  9,  0, 14,  1, 11,  3,  2,  4,
           5,  6,  8, 10,  7, 31, 31, 31, 31, 31,
          31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, // fr_FR
        { 12, 13,  9,  0, 14,  1, 11,  3,  2,  4,
           5,  6,  8, 10,  7, 31, 31, 31, 31, 31,
          31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, // fr_CA
        { 12, 13,  9,  0, 14,  1, 11,  2,  3,  4,
           5,  6,  8, 10,  7, 31, 31, 31, 31, 31,
          31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, // de_DE
        { 12, 13,  9,  0, 14,  1, 11,  2,  3,  4,
           5,  6,  8, 10,  7, 31, 31, 31, 31, 31,
          31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, // it_IT
        { 12, 13,  9,  0, 14,  1, 11,  2,  3,  4,
           5,  6,  8, 10,  7, 31, 31, 31, 31, 31,
          31, 31, 31, 31, 31, 31, 31, 31, 31, 31 }, // ja_JP
    };

    // new table collation with rules "& Z < p, P"
    // loop to FIXEDTESTSET
    private static final int[] Test1Results = {
        12, 13,  9,  0,  6,  8, 10,  7, 14,  1,
        11,  2,  3,  4,  5, 31, 31, 31, 31, 31,
        31, 31, 31, 31, 31, 31, 31, 31, 31, 31
    };

    // new table collation with rules "& C < ch , cH, Ch, CH "
    // loop to TOTALTESTSET
    private static final int[] Test2Results = {
        19, 22, 21, 23, 25, 24, 12, 13,  9,  0,
        17, 26, 28, 27, 15, 16, 18, 14,  1, 11,
         2,  3,  4,  5, 20,  6,  8, 10,  7, 29
    };

    // new table collation with rules
    //     "& Question-mark ; ? & Hash-mark ; # & Ampersand ; '&'  "
    // loop to TOTALTESTSET
    private static final int[] Test3Results = {
        23, 25, 22, 24, 12, 13,  9,  0, 17, 16,
        26, 28, 27, 15, 18, 21, 14,  1, 11,  2,
         3,  4,  5, 19, 20,  6,  8, 10,  7, 29
    };

    // analogous to Japanese rules
    //     " & aa ; a- & ee ; e- & ii ; i- & oo ; o- & uu ; u- "
    // loop to TOTALTESTSET
    private static final int[] Test4Results = {
        19, 22, 21, 23, 24, 25, 12, 13,  9,  0,
        17, 16, 26, 27, 28, 15, 18, 14,  1, 11,
         2,  3,  4,  5, 20,  6,  8, 10,  7, 29
    };

    public void TestG7Data() {
        for (int i = 0; i < locales.length; i++) {
            Collator myCollation= null;
            RuleBasedCollator tblColl1 = null;

            try {
                myCollation = Collator.getInstance(locales[i]);
                tblColl1 = new RuleBasedCollator(((RuleBasedCollator)myCollation).getRules());
            } catch (Exception foo) {
                errln("Exception: " + foo.getMessage() +
                      " Locale : " + locales[i].getDisplayName() +
                      " getRules failed\n");
                continue;
            }
            for (int j = 0; j < FIXEDTESTSET; j++) {
                for (int n = j+1; n < FIXEDTESTSET; n++) {
                    doTest(tblColl1, testCases[G7Results[i][j]],
                           testCases[G7Results[i][n]], -1);
                }
            }
            myCollation = null;
        }
    }

    /*
     * Demo Test 1 : Create a new table collation with rules "& Z < p, P"
     */
    public void TestDemoTest1() {
        int j = 0;
        final Collator myCollation = Collator.getInstance(Locale.US);
        final String defRules = ((RuleBasedCollator)myCollation).getRules();
        RuleBasedCollator tblColl = null;
        String newRules = defRules + " & Z < p, P";

        try {
            tblColl = new RuleBasedCollator(newRules);
            for (j = 0; j < FIXEDTESTSET; j++) {
                for (int n = j+1; n < FIXEDTESTSET; n++) {
                    doTest(tblColl, testCases[Test1Results[j]],
                           testCases[Test1Results[n]], -1);
                }
            }
            tblColl = null;
        } catch (Exception foo) {
            errln("Exception: " + foo.getMessage() +
                  "\nDemo Test 1 Table Collation object creation failed.");
        }
    }

    /*
     * Demo Test 2 : Create a new table collation with rules
     *     "& C < ch , cH, Ch, CH"
     */
    public void TestDemoTest2() {
        final Collator myCollation = Collator.getInstance(Locale.US);
        final String defRules = ((RuleBasedCollator)myCollation).getRules();
        String newRules = defRules + "& C < ch , cH, Ch, CH";

        try {
            RuleBasedCollator tblColl = new RuleBasedCollator(newRules);
            for (int j = 0; j < TOTALTESTSET; j++) {
                for (int n = j+1; n < TOTALTESTSET; n++) {
                    doTest(tblColl, testCases[Test2Results[j]],
                           testCases[Test2Results[n]], -1);
                }
            }
        } catch (Exception foo) {
            errln("Exception: " + foo.getMessage() +
                  "\nDemo Test 2 Table Collation object creation failed.\n");
        }
    }

    /*
     * Demo Test 3 : Create a new table collation with rules
     *     "& Question'-'mark ; '?' & Hash'-'mark ; '#' & Ampersand ; '&'"
     */
    public void TestDemoTest3() {
        final Collator myCollation = Collator.getInstance(Locale.US);
        final String defRules = ((RuleBasedCollator)myCollation).getRules();
        RuleBasedCollator tblColl = null;
        String newRules = defRules + "& Question'-'mark ; '?' & Hash'-'mark ; '#' & Ampersand ; '&";

        try {
            tblColl = new RuleBasedCollator(newRules);
            for (int j = 0; j < TOTALTESTSET; j++) {
                for (int n = j+1; n < TOTALTESTSET; n++) {
                    doTest(tblColl, testCases[Test3Results[j]],
                           testCases[Test3Results[n]], -1);
                }
            }
        } catch (Exception foo) {
            errln("Exception: " + foo.getMessage() +
                  "\nDemo Test 3 Table Collation object creation failed.");
        }
    }

    /*
     * Demo Test 4 : Create a new table collation with rules
     *     " & aa ; a'-' & ee ; e'-' & ii ; i'-' & oo ; o'-' & uu ; u'-' "
     */
    public void TestDemoTest4() {
        final Collator myCollation = Collator.getInstance(Locale.US);
        final String defRules = ((RuleBasedCollator)myCollation).getRules();
        RuleBasedCollator tblColl = null;
        String newRules = defRules + " & aa ; a'-' & ee ; e'-' & ii ; i'-' & oo ; o'-' & uu ; u'-' ";

        try {
            tblColl = new RuleBasedCollator(newRules);
            for (int j = 0; j < TOTALTESTSET; j++) {
                for (int n = j+1; n < TOTALTESTSET; n++) {
                    doTest(tblColl, testCases[Test4Results[j]],
                           testCases[Test4Results[n]], -1);
                }
            }
        } catch (Exception foo) {
            errln("Exception: " + foo.getMessage() +
                  "\nDemo Test 4 Table Collation object creation failed.");
        }
        tblColl = null;
    }

    private static final int FIXEDTESTSET = 15;
    private static final int TOTALTESTSET = 30;

    private static final Locale locales[] = {
        Locale.US,
        Locale.UK,
        Locale.CANADA,
        Locale.FRANCE,
        Locale.CANADA_FRENCH,
        Locale.GERMANY,
        Locale.JAPAN,
        Locale.ITALY
    };
}
