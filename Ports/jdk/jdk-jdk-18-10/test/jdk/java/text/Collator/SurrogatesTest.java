/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test Supplementary Character Collation
 */

import java.text.Collator;
import java.text.RuleBasedCollator;

// Quick dummy program for printing out test results
public class SurrogatesTest extends CollatorTest {

    public static void main(String[] args) throws Exception {
        new SurrogatesTest().run(args);
    }

    /*
     * Data for TestPrimary()
     */
    private static final String[] primarySourceData = {
        "A\ud800\udc04BCD"
    };

    private static final String[] primaryTargetData = {
        "A\ud800\udc05BCD"
    };

    private static final int[] primaryResults = {
         0
    };

    /*
     * Data for TestTertiary()
     */
    private static final String[] tertiarySourceData = {
        "ABCD",
        "ABCD",
        "A\ud800\udc00CD",
        "WXYZ",
        "WXYZ",
        "AFEM",
        "FGM",
        "BB",
        "BB"
    };

    private static final String[] tertiaryTargetData = {
        "A\ud800\udc00CD",
        "AB\ud800\udc00D",
        "A\ud800\udc01CD",
        "W\ud800\udc0aYZ",
        "W\ud800\udc0bYZ",
        "A\ud800\udc08M",
        "\ud800\udc08M",
        "\ud800\udc04\ud800\udc02",
        "\ud800\udc04\ud800\udc05"
    };

    private static final int[] tertiaryResults = {
        -1,  1,  1,  1, -1, -1, -1, -1,  1
    };

    public void TestPrimary() {
        doTest(myCollation, Collator.PRIMARY,
               primarySourceData, primaryTargetData, primaryResults);
    }

    public void TestTertiary() {
        doTest(myCollation, Collator.TERTIARY,
               tertiarySourceData, tertiaryTargetData, tertiaryResults);
    }

    private Collator getCollator() {
        RuleBasedCollator base = (RuleBasedCollator)Collator.getInstance();
        String rule = base.getRules();
        try {
            return new RuleBasedCollator(rule
                                     + "&B < \ud800\udc01 < \ud800\udc00"
                                     + ", \ud800\udc02, \ud800\udc03"
                                     + "; \ud800\udc04, \ud800\udc05"
                                     + "< \ud800\udc06 < \ud800\udc07"
                                     + "&FE < \ud800\udc08"
                                     + "&PE, \ud800\udc09"
                                     + "&Z < \ud800\udc0a < \ud800\udc0b < \ud800\udc0c"
                                     + "&\ud800\udc0a < x, X"
                                     + "&A < \ud800\udc04\ud800\udc05");
        } catch (Exception e) {
            errln("Failed to create new RulebasedCollator object");
            return null;
        }
    }

    private Collator myCollation = getCollator();
}
