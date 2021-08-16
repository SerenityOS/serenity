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
 * @bug 4932968 5015215
 * @library /java/text/testlib
 * @summary test Vietnamese Collation
 * @modules jdk.localedata
 */

/*
 *******************************************************************************
 * (C) Copyright IBM Corp. 1996-2003 - All Rights Reserved                     *
 *                                                                             *
 * The original version of this source code and documentation is copyrighted   *
 * and owned by IBM, These materials are provided under terms of a License     *
 * Agreement between IBM and Sun. This technology is protected by multiple     *
 * US and International patents. This notice and attribution to IBM may not    *
 * to removed.                                                                 *
 *******************************************************************************
 */

import java.util.Locale;
import java.text.Collator;

// Quick dummy program for printing out test results
public class VietnameseTest extends CollatorTest {

    public static void main(String[] args) throws Exception {
        new VietnameseTest().run(args);
    }

    private final static String testPS[] = {
        "a",
        "a",
        "\u00c2",
        "Cz",
        "d",
        "e",
        "e",
        "\u1ec7",
        "gz",
        "i",
        "kz",
        "nz",
        "nh",
        "o",
        "o",
        "\u01a0",
        "pz",
        "tz",
        "tr",
        "u",
        "u",
        "y"
    };

    private final static String testPT[] = {
        "\u00e0",
        "\u0102",
        "\u0102",
        "Ch",
        "\u0110",
        "\u0111",
        "\u1eb9",
        "\u1eb9",
        "gi",
        "\u0128",
        "kh",
        "ng",
        "ng",
        "\u00f2",
        "\u00f4",
        "\u00f4",
        "ph",
        "th",
        "th",
        "\u1ee5",
        "\u01b0",
        "\u1ef4"
    };

    private final static int testPR[] = {
        0,
        -1,
        1,
        1,
        -1,
        1,
        0,
        1,
        1,
        0,
        1,
        1,
        1,
        0,
        -1,
        1,
        1,
        1,
        1,
        0,
        -1,
        0
    };

    private final static String testT[] = {
        "a",
        "A",
        "\u00e0",
        "\u00c0",
        "\u1ea3",
        "\u1ea2",
        "\u00e3",
        "\u00c3",
        "\u00e1",
        "\u00c1",
        "\u1ea1",
        "\u1ea0",
        "\u0103",
        "\u0102",
        "\u1eb1",
        "\u1eb0",
        "\u1eb3",
        "\u1eb2",
        "\u1eb5",
        "\u1eb4",
        "\u1eaf",
        "\u1eae",
        "\u1eb7",
        "\u1eb6",
        "\u00e2",
        "\u00c2",
        "\u1ea7",
        "\u1ea6",
        "\u1ea9",
        "\u1ea8",
        "\u1eab",
        "\u1eaa",
        "\u1ea5",
        "\u1ea4",
        "\u1ead",
        "\u1eac",
        "b",
        "B",
        "c",
        "C",
        "ch",
        "Ch",
        "CH",
        "d",
        "D",
        "\u0111",
        "\u0110",
        "e",
        "E",
        "\u00e8",
        "\u00c8",
        "\u1ebb",
        "\u1eba",
        "\u1ebd",
        "\u1ebc",
        "\u00e9",
        "\u00c9",
        "\u1eb9",
        "\u1eb8",
        "\u00ea",
        "\u00ca",
        "\u1ec1",
        "\u1ec0",
        "\u1ec3",
        "\u1ec2",
        "\u1ec5",
        "\u1ec4",
        "\u1ebf",
        "\u1ebe",
        "\u1ec7",
        "\u1ec6",
        "f",
        "F",
        "g",
        "G",
        "gi",
        "Gi",
        "GI",
        "gz",
        "h",
        "H",
        "i",
        "I",
        "\u00ec",
        "\u00cc",
        "\u1ec9",
        "\u1ec8",
        "\u0129",
        "\u0128",
        "\u00ed",
        "\u00cd",
        "\u1ecb",
        "\u1eca",
        "j",
        "J",
        "k",
        "K",
        "kh",
        "Kh",
        "KH",
        "kz",
        "l",
        "L",
        "m",
        "M",
        "n",
        "N",
        "ng",
        "Ng",
        "NG",
        "ngz",
        "nh",
        "Nh",
        "NH",
        "nz",
        "o",
        "O",
        "\u00f2",
        "\u00d2",
        "\u1ecf",
        "\u1ece",
        "\u00f5",
        "\u00d5",
        "\u00f3",
        "\u00d3",
        "\u1ecd",
        "\u1ecc",
        "\u00f4",
        "\u00d4",
        "\u1ed3",
        "\u1ed2",
        "\u1ed5",
        "\u1ed4",
        "\u1ed7",
        "\u1ed6",
        "\u1ed1",
        "\u1ed0",
        "\u1ed9",
        "\u1ed8",
        "\u01a1",
        "\u01a0",
        "\u1edd",
        "\u1edc",
        "\u1edf",
        "\u1ede",
        "\u1ee1",
        "\u1ee0",
        "\u1edb",
        "\u1eda",
        "\u1ee3",
        "\u1ee2",
        "p",
        "P",
        "ph",
        "Ph",
        "PH",
        "pz",
        "q",
        "Q",
        "r",
        "R",
        "s",
        "S",
        "t",
        "T",
        "th",
        "Th",
        "TH",
        "thz",
        "tr",
        "Tr",
        "TR",
        "tz",
        "u",
        "U",
        "\u00f9",
        "\u00d9",
        "\u1ee7",
        "\u1ee6",
        "\u0169",
        "\u0168",
        "\u00fa",
        "\u00da",
        "\u1ee5",
        "\u1ee4",
        "\u01b0",
        "\u01af",
        "\u1eeb",
        "\u1eea",
        "\u1eed",
        "\u1eec",
        "\u1eef",
        "\u1eee",
        "\u1ee9",
        "\u1ee8",
        "\u1ef1",
        "\u1ef0",
        "v",
        "V",
        "w",
        "W",
        "x",
        "X",
        "y",
        "Y",
        "\u1ef3",
        "\u1ef2",
        "\u1ef7",
        "\u1ef6",
        "\u1ef9",
        "\u1ef8",
        "\u00fd",
        "\u00dd",
        "\u1ef5",
        "\u1ef4",
        "z",
        "Z"
    };

    public void TestPrimary() {
        doTest(myCollation, Collator.PRIMARY, testPS, testPT, testPR);
    }

    public void TestTertiary() {
        int testLength = testT.length;

        myCollation.setStrength(Collator.TERTIARY);
        for (int i = 0; i < testLength - 1; i++) {
            for (int j = i+1; j < testLength; j++) {
                doTest(myCollation, testT[i], testT[j], -1);
            }
        }
    }

    private final Collator myCollation = Collator.getInstance(new Locale("vi", "VN"));
}
