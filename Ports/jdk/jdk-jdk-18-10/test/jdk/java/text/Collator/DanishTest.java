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
 * @bug 4930708 4174436 5008498
 * @library /java/text/testlib
 * @summary test Danish Collation
 * @modules jdk.localedata
 */
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

import java.util.Locale;
import java.text.Collator;

// Quick dummy program for printing out test results
public class DanishTest extends CollatorTest {

    public static void main(String[] args) throws Exception {
        new DanishTest().run(args);
    }

    /*
     * Data for TestPrimary()
     */
    private static final String[] primarySourceData = {
        "Lvi",
        "L\u00E4vi",
        "L\u00FCbeck",
        "ANDR\u00C9",
        "ANDRE",
        "ANNONCERER"
    };

    private static final String[] primaryTargetData = {
            "Lwi",
            "L\u00F6wi",
            "Lybeck",
            "ANDR\u00E9",
            "ANDR\u00C9",
            "ANN\u00D3NCERER"
    };

    private static final int[] primaryResults = {
            -1, -1, 0, 0, 0, 0
    };

    /*
     * Data for TestTertiary()
     */
    private static final String[] tertiarySourceData = {
            "Luc",
            "luck",
            "L\u00FCbeck",
            "L\u00E4vi",
            "L\u00F6ww"
    };

    private static final String[] tertiaryTargetData = {
            "luck",
            "L\u00FCbeck",
            "lybeck",
            "L\u00F6we",
            "mast"
    };

    private static final int[] tertiaryResults = {
            -1, -1,  1, -1, -1
    };

    /*
     * Data for TestExtra()
     */
    private static final String[] testData = {
            "A/S",
            "ANDRE",
            "ANDR\u00C9", // E-acute
            "ANDR\u00C8", // E-grave
            "ANDR\u00E9", // e-acute
            "ANDR\u00EA", // e-circ
            "Andre",
            "Andr\u00E9", // e-acute
            "\u00C1NDRE", // A-acute
            "\u00C0NDRE", // A-grave
            "andre",
            "\u00E1ndre", // a-acute
            "\u00E0ndre", // a-grave
            "ANDREAS",
            "ANNONCERER",
            "ANN\u00D3NCERER", // O-acute
            "annoncerer",
            "ann\u00F3ncerer", // o-acute
            "AS",
            "A\u00e6RO", // ae-ligature
            "CA",
            "\u00C7A", // C-cedilla
            "CB",
            "\u00C7C", // C-cedilla
            "D.S.B.",
            "DA",
            "DB",
            "\u00D0ORA", // capital eth
            "DSB",
            "\u00D0SB", // capital eth
            "DSC",
            "EKSTRA_ARBEJDE",
            "EKSTRABUD",
            "H\u00D8ST",  // could the 0x00D8 be 0x2205?
            "HAAG",
            "H\u00C5NDBOG", // A-ring
            "HAANDV\u00C6RKSBANKEN", // AE-ligature
            "INTERNETFORBINDELSE",
            "Internetforbindelse",
            "\u00CDNTERNETFORBINDELSE", // I-acute
            "internetforbindelse",
            "\u00EDnternetforbindelse", // i-acute
            "Karl",
            "karl",
            "NIELSEN",
            "NIELS J\u00D8RGEN", // O-slash
            "NIELS-J\u00D8RGEN", // O-slash
            "OERVAL",
            "\u0152RVAL", // OE-ligature
            "\u0153RVAL", // oe-ligature
            "R\u00C9E, A", // E-acute
            "REE, B",
            "R\u00C9E, L", // E-acute
            "REE, V",
            "SCHYTT, B",
            "SCHYTT, H",
            "SCH\u00DCTT, H", // U-diaeresis
            "SCHYTT, L",
            "SCH\u00DCTT, M", // U-diaeresis
            "SS",
            "ss",
            "\u00DF", // sharp S
            "SSA",
            "\u00DFA", // sharp S
            "STOREK\u00C6R", // AE-ligature
            "STORE VILDMOSE",
            "STORMLY",
            "STORM PETERSEN",
            "THORVALD",
            "THORVARDUR",
            "\u00DEORVAR\u0110UR", //  capital thorn, capital d-stroke(like eth) (sami)
            "THYGESEN",
            "VESTERG\u00C5RD, A",
            "VESTERGAARD, A",
            "VESTERG\u00C5RD, B",                // 50
            "Westmalle",
            "YALLE",
            "Yderligere",
            "\u00DDderligere", // Y-acute
            "\u00DCderligere", // U-diaeresis
            "\u00FDderligere", // y-acute
            "\u00FCderligere", // u-diaeresis
            "U\u0308ruk-hai",
            "ZORO",
            "\u00C6BLE",  // AE-ligature
            "\u00E6BLE",  // ae-ligature
            "\u00C4BLE",  // A-diaeresis
            "\u00E4BLE",  // a-diaeresis
            "\u00D8BERG", // O-stroke
            "\u00F8BERG", // o-stroke
            "\u00D6BERG", // O-diaeresis
            "\u00F6BERG"  // o-diaeresis
    };

    public void TestPrimary() {
        doTest(myCollation, Collator.PRIMARY,
               primarySourceData, primaryTargetData, primaryResults);
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

    private final Collator myCollation = Collator.getInstance(new Locale("da", "", ""));
}
