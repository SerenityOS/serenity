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
 * @summary test English Collation
 */

import java.util.Locale;
import java.text.Collator;

/*
(C) Copyright Taligent, Inc. 1996 - All Rights Reserved
(C) Copyright IBM Corp. 1996 - All Rights Reserved

  The original version of this source code and documentation is copyrighted and
owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These materials are
provided under terms of a License Agreement between Taligent and Sun. This
technology is protected by multiple US and International patents. This notice and
attribution to Taligent may not be removed.
  Taligent is a registered trademark of Taligent, Inc.
*/

public class EnglishTest extends CollatorTest {

    public static void main(String[] args) throws Exception {
        new EnglishTest().run(args);
    }

    /*
     * Data for TestPrimary()
     */
    private static final String[] primarySourceData = {
        "p\u00EAche",
        "abc",
        "abc",
        "abc",
        "a\u00E6c"
    };

    private static final String[] primaryTargetData = {
        "p\u00E9ch\u00E9",
        "aBC",
        "abd",
        "\u00E4bc",
        "a\u00C6c"
    };

    private static final int[] primaryResults = {
         0,  0, -1,  0,  0,
    };

    /*
     * Data for TestSecondary()
     */
    private static final String[] secondarySourceData = {
        "abc",
        "abc",
        "a\u00E6c",
        "abc",
        "abc",
        "p\u00e9ch\u00e9"
    };

    private static final String[] secondaryTargetData = {
        "aBd",
        "\u00E4bc",
        "a\u00C6c",
        "aBd",
        "\u00E4bc",
        "p\u00eache"
    };

    private static final int[] secondaryResults = {
        -1, -1,  0, -1, -1, -1
    };

    /*
     * Data for TestTertiary() {
     */
    private static final String[] tertiarySourceData = {
        "ab",
        "black-bird",
        "black bird",
        "black-bird",
        "Hello",
        "ABC",
        "abc",
        "blackbird",
        "black-bird",
        "black-bird",
        "p\u00EAche",
        "p\u00E9ch\u00E9",
        "\u00C4B\u0308C\u0308",
        "a\u0308bc",
        "p\u00E9cher",
        "roles",
        "abc",
        "A",
        "A",
        "ab",
        "tcompareplain",
        "ab",
        "a#b",
        "a#b",
        "abc",
        "Abcda",
        "abcda",
        "abcda",
        "\u00E6bcda",
        "\u00E4bcda",
        "abc",
        "abc",
        "abc",
        "abc",
        "abc",
        "acHc",
        "a\u0308bc",
        "thi\u0302s"
    };

    private static final String[] tertiaryTargetData = {
        "abc",
        "blackbird",
        "black-bird",
        "black",
        "hello",
        "ABC",
        "ABC",
        "blackbirds",
        "blackbirds",
        "blackbird",
        "p\u00E9ch\u00E9",
        "p\u00E9cher",
        "\u00C4B\u0308C\u0308",
        "A\u0308bc",
        "p\u00E9che",
        "ro\u0302le",
        "A\u00E1cd",
        "A\u00E1cd",
        "abc",
        "abc",
        "TComparePlain",
        "aBc",
        "a#B",
        "a&b",
        "a#c",
        "abcda",
        "\u00C4bcda",
        "\u00E4bcda",
        "\u00C4bcda",
        "\u00C4bcda",
        "ab#c",
        "abc",
        "ab=c",
        "abd",
        "\u00E4bc",
        "aCHc",
        "\u00E4bc",
        "th\u00EEs"
    };

    private static final int[] tertiaryResults = {
        -1,  1, -1,  1,  1,  0, -1, -1, -1,  1,
         1, -1,  0, -1,  1,  1,  1, -1, -1, -1,
        -1, -1, -1,  1,  1,  1, -1, -1,  1, -1,
         1,  0,  1, -1, -1, -1,  0,  0
    };

    private static final String testData[] = {
        "a",
        "A",
        "e",
        "E",
        "\u00e9",
        "\u00e8",
        "\u00ea",
        "\u00eb",
        "ea",
        "x"
    };

    public void TestPrimary() {
        doTest(myCollation, Collator.PRIMARY,
               primarySourceData, primaryTargetData, primaryResults);
    }

    public void TestSecondary() {
        doTest(myCollation, Collator.SECONDARY,
               secondarySourceData, secondaryTargetData, secondaryResults);
    }

    public void TestTertiary() {
        doTest(myCollation, Collator.TERTIARY,
               tertiarySourceData, tertiaryTargetData, tertiaryResults);

        for (int i = 0; i < testData.length-1; i++) {
            for (int j = i+1; j < testData.length; j++) {
                doTest(myCollation, testData[i], testData[j], -1);
            }
        }
    }

    private final Collator myCollation = Collator.getInstance(Locale.ENGLISH);
}
