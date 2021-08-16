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
 * @summary test Dummy Collation
 */

import java.text.Collator;
import java.text.RuleBasedCollator;

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

public class DummyTest extends CollatorTest {

    public static void main(String[] args) throws Exception {
        new DummyTest().run(args);
    }

    private static final String DEFAULTRULES =
        "='\u200B'=\u200C=\u200D=\u200E=\u200F"
        // Control Characters
        + "=\u0000 =\u0001 =\u0002 =\u0003 =\u0004" //null, .. eot
        + "=\u0005 =\u0006 =\u0007 =\u0008 ='\u0009'" //enq, ...
        + "='\u000b' =\u000e" //vt,, so
        + "=\u000f ='\u0010' =\u0011 =\u0012 =\u0013" //si, dle, dc1, dc2, dc3
        + "=\u0014 =\u0015 =\u0016 =\u0017 =\u0018" //dc4, nak, syn, etb, can
        + "=\u0019 =\u001a =\u001b =\u001c =\u001d" //em, sub, esc, fs, gs
        + "=\u001e =\u001f =\u007f"                   //rs, us, del
        //....then the C1 Latin 1 reserved control codes
        + "=\u0080 =\u0081 =\u0082 =\u0083 =\u0084 =\u0085"
        + "=\u0086 =\u0087 =\u0088 =\u0089 =\u008a =\u008b"
        + "=\u008c =\u008d =\u008e =\u008f =\u0090 =\u0091"
        + "=\u0092 =\u0093 =\u0094 =\u0095 =\u0096 =\u0097"
        + "=\u0098 =\u0099 =\u009a =\u009b =\u009c =\u009d"
        + "=\u009e =\u009f"
        // IGNORE except for secondary, tertiary difference
        // Spaces
        + ";'\u0020';'\u00A0'"                  // spaces
        + ";'\u2000';'\u2001';'\u2002';'\u2003';'\u2004'"  // spaces
        + ";'\u2005';'\u2006';'\u2007';'\u2008';'\u2009'"  // spaces
        + ";'\u200A';'\u3000';'\uFEFF'"                // spaces
        + ";'\r' ;'\t' ;'\n';'\f';'\u000b'"  // whitespace

        // Non-spacing accents

        + ";\u0301"          // non-spacing acute accent
        + ";\u0300"          // non-spacing grave accent
        + ";\u0306"          // non-spacing breve accent
        + ";\u0302"          // non-spacing circumflex accent
        + ";\u030c"          // non-spacing caron/hacek accent
        + ";\u030a"          // non-spacing ring above accent
        + ";\u030d"          // non-spacing vertical line above
        + ";\u0308"          // non-spacing diaeresis accent
        + ";\u030b"          // non-spacing double acute accent
        + ";\u0303"          // non-spacing tilde accent
        + ";\u0307"          // non-spacing dot above/overdot accent
        + ";\u0304"          // non-spacing macron accent
        + ";\u0337"          // non-spacing short slash overlay (overstruck diacritic)
        + ";\u0327"          // non-spacing cedilla accent
        + ";\u0328"          // non-spacing ogonek accent
        + ";\u0323"          // non-spacing dot-below/underdot accent
        + ";\u0332"          // non-spacing underscore/underline accent
        // with the rest of the general diacritical marks in binary order
        + ";\u0305"          // non-spacing overscore/overline
        + ";\u0309"          // non-spacing hook above
        + ";\u030e"          // non-spacing double vertical line above
        + ";\u030f"          // non-spacing double grave
        + ";\u0310"          // non-spacing chandrabindu
        + ";\u0311"          // non-spacing inverted breve
        + ";\u0312"          // non-spacing turned comma above/cedilla above
        + ";\u0313"          // non-spacing comma above
        + ";\u0314"          // non-spacing reversed comma above
        + ";\u0315"          // non-spacing comma above right
        + ";\u0316"          // non-spacing grave below
        + ";\u0317"          // non-spacing acute below
        + ";\u0318"          // non-spacing left tack below
        + ";\u0319"          // non-spacing tack below
        + ";\u031a"          // non-spacing left angle above
        + ";\u031b"          // non-spacing horn
        + ";\u031c"          // non-spacing left half ring below
        + ";\u031d"          // non-spacing up tack below
        + ";\u031e"          // non-spacing down tack below
        + ";\u031f"          // non-spacing plus sign below
        + ";\u0320"          // non-spacing minus sign below
        + ";\u0321"          // non-spacing palatalized hook below
        + ";\u0322"          // non-spacing retroflex hook below
        + ";\u0324"          // non-spacing double dot below
        + ";\u0325"          // non-spacing ring below
        + ";\u0326"          // non-spacing comma below
        + ";\u0329"          // non-spacing vertical line below
        + ";\u032a"          // non-spacing bridge below
        + ";\u032b"          // non-spacing inverted double arch below
        + ";\u032c"          // non-spacing hacek below
        + ";\u032d"          // non-spacing circumflex below
        + ";\u032e"          // non-spacing breve below
        + ";\u032f"          // non-spacing inverted breve below
        + ";\u0330"          // non-spacing tilde below
        + ";\u0331"          // non-spacing macron below
        + ";\u0333"          // non-spacing double underscore
        + ";\u0334"          // non-spacing tilde overlay
        + ";\u0335"          // non-spacing short bar overlay
        + ";\u0336"          // non-spacing long bar overlay
        + ";\u0338"          // non-spacing long slash overlay
        + ";\u0339"          // non-spacing right half ring below
        + ";\u033a"          // non-spacing inverted bridge below
        + ";\u033b"          // non-spacing square below
        + ";\u033c"          // non-spacing seagull below
        + ";\u033d"          // non-spacing x above
        + ";\u033e"          // non-spacing vertical tilde
        + ";\u033f"          // non-spacing double overscore
        + ";\u0340"          // non-spacing grave tone mark
        + ";\u0341"          // non-spacing acute tone mark
        + ";\u0342;\u0343;\u0344;\u0345;\u0360;\u0361"    // newer
        + ";\u0483;\u0484;\u0485;\u0486"    // Cyrillic accents

        + ";\u20D0;\u20D1;\u20D2"           // symbol accents
        + ";\u20D3;\u20D4;\u20D5"           // symbol accents
        + ";\u20D6;\u20D7;\u20D8"           // symbol accents
        + ";\u20D9;\u20DA;\u20DB"           // symbol accents
        + ";\u20DC;\u20DD;\u20DE"           // symbol accents
        + ";\u20DF;\u20E0;\u20E1"           // symbol accents

        + ",'\u002D';\u00AD"                // dashes
        + ";\u2010;\u2011;\u2012"           // dashes
        + ";\u2013;\u2014;\u2015"           // dashes
        + ";\u2212"                         // dashes

        // other punctuation

        + "<'\u005f'"        // underline/underscore (spacing)
        + "<\u00af"          // overline or macron (spacing)
//        + "<\u00ad"        // syllable hyphen (SHY) or soft hyphen
        + "<'\u002c'"        // comma (spacing)
        + "<'\u003b'"        // semicolon
        + "<'\u003a'"        // colon
        + "<'\u0021'"        // exclamation point
        + "<\u00a1"          // inverted exclamation point
        + "<'\u003f'"        // question mark
        + "<\u00bf"          // inverted question mark
        + "<'\u002f'"        // slash
        + "<'\u002e'"        // period/full stop
        + "<\u00b4"          // acute accent (spacing)
        + "<'\u0060'"        // grave accent (spacing)
        + "<'\u005e'"        // circumflex accent (spacing)
        + "<\u00a8"          // diaresis/umlaut accent (spacing)
        + "<'\u007e'"        // tilde accent (spacing)
        + "<\u00b7"          // middle dot (spacing)
        + "<\u00b8"          // cedilla accent (spacing)
        + "<'\u0027'"        // apostrophe
        + "<'\"'"            // quotation marks
        + "<\u00ab"          // left angle quotes
        + "<\u00bb"          // right angle quotes
        + "<'\u0028'"        // left parenthesis
        + "<'\u0029'"        // right parenthesis
        + "<'\u005b'"        // left bracket
        + "<'\u005d'"        // right bracket
        + "<'\u007b'"        // left brace
        + "<'\u007d'"        // right brace
        + "<\u00a7"          // section symbol
        + "<\u00b6"          // paragraph symbol
        + "<\u00a9"          // copyright symbol
        + "<\u00ae"          // registered trademark symbol
        + "<'\u0040'"          // at sign
        + "<\u00a4"          // international currency symbol
        + "<\u00a2"          // cent sign
        + "<'\u0024'"        // dollar sign
        + "<\u00a3"          // pound-sterling sign
        + "<\u00a5"          // yen sign
        + "<'\u002a'"        // asterisk
        + "<'\\u005c'"       // backslash
        + "<'\u0026'"        // ampersand
        + "<'\u0023'"        // number sign
        + "<'\u0025'"        // percent sign
        + "<'\u002b'"        // plus sign
//        + "<\u002d"        // hyphen or minus sign
        + "<\u00b1"          // plus-or-minus sign
        + "<\u00f7"          // divide sign
        + "<\u00d7"          // multiply sign
        + "<'\u003c'"        // less-than sign
        + "<'\u003d'"        // equal sign
        + "<'\u003e'"        // greater-than sign
        + "<\u00ac"          // end of line symbol/logical NOT symbol
        + "<'\u007c'"          // vertical line/logical OR symbol
        + "<\u00a6"          // broken vertical line
        + "<\u00b0"          // degree symbol
        + "<\u00b5"          // micro symbol

        // NUMERICS

        + "<0<1<2<3<4<5<6<7<8<9"
        + "<\u00bc<\u00bd<\u00be"   // 1/4,1/2,3/4 fractions

        // NON-IGNORABLES
        + "<a,A"
        + "<b,B"
        + "<c,C"
        + "<d,D"
        + "<\u00F0,\u00D0"                  // eth
        + "<e,E"
        + "<f,F"
        + "<g,G"
        + "<h,H"
        + "<i,I"
        + "<j,J"
        + "<k,K"
        + "<l,L"
        + "<m,M"
        + "<n,N"
        + "<o,O"
        + "<p,P"
        + "<q,Q"
        + "<r,R"
        + "<s, S & SS,\u00DF"             // s-zet
        + "<t,T"
        + "&th, \u00FE & TH, \u00DE"           // thorn
        + "<u,U"
        + "<v,V"
        + "<w,W"
        + "<x,X"
        + "<y,Y"
        + "<z,Z"
        + "&AE,\u00C6"                    // ae & AE ligature
        + "&AE,\u00E6"
        + "&OE,\u0152"                    // oe & OE ligature
        + "&OE,\u0153";

    /*
     * Data for TestPrimary()
     */
    private static final String[] primarySourceData = {
        "p\u00EAche",
        "abc",
        "abc",
        "abc",
        "abc",
        "abc",
        "a\u00E6c",
        "acHc",
        "black"
    };

    private static final String[] primaryTargetData = {
        "p\u00E9ch\u00E9",
        "abc",
        "aBC",
        "abch",
        "abd",
        "\u00E4bc",
        "a\u00C6c",
        "aCHc",
        "black-bird"
    };

    private static final int[] primaryResults = {
         0,  0,  0, -1, -1,  0,  0,  0, -1
    };

    /*
     * Data for TestSecondary()
     */
    private static final String[] secondarySourceData = {
        "four",
        "five",
        "1",
        "abc",
        "abc",
        "abcH",
        "abc",
        "acHc"
    };

    private static final String[] secondaryTargetData = {

        "4",
        "5",
        "one",
        "abc",
        "aBc",
        "abch",
        "abd",
        "aCHc"
    };

    private static final int[] secondaryResults = {
         0,  0,  0,  0,  0,  0, -1,  0
    };

    /*
     * Data for TestTertiary()
     */
    private static final String[] tertiarySourceData = {
        "ab'c",
        "co-op",
        "ab",
        "ampersad",
        "all",
        "four",
        "five",
        "1",
        "1",
        "1",
        "2",
        "2",
        "Hello",
        "a<b",
        "a<b",
        "acc",
        "acHc"
    };

    private static final String[] tertiaryTargetData = {
        "abc",
        "COOP",
        "abc",
        "&",
        "&",
        "4",
        "5",
        "one",
        "nne",
        "pne",
        "two",
        "uwo",
        "hellO",
        "a<=b",
        "abc",
        "aCHc",
        "aCHc"
    };

    private static final int[] tertiaryResults = {
        -1,  1, -1, -1, -1, -1, -1,  1,  1, -1,
         1, -1,  1,  1, -1, -1, -1
    };


    private static final String[] testData = {
        "a",
        "A",
        "\u00e4",
        "\u00c4",
        "ae",
        "aE",
        "Ae",
        "AE",
        "\u00e6",
        "\u00c6",
        "b",
        "c",
        "z"
    };

    public void TestPrimary() {
        doTest(getCollator(), Collator.PRIMARY,
               primarySourceData, primaryTargetData, primaryResults);
    }

    public void TestSecondary() {
        doTest(getCollator(), Collator.SECONDARY,
               secondarySourceData, secondaryTargetData, secondaryResults);
    }

    public void TestTertiary() {
        Collator col = getCollator();

        doTest(col, Collator.TERTIARY,
               tertiarySourceData, tertiaryTargetData, tertiaryResults);

        for (int i = 0; i < testData.length-1; i++) {
            for (int j = i+1; j < testData.length; j++) {
                doTest(col, testData[i], testData[j], -1);
            }
        }
    }

    private RuleBasedCollator myCollation = null;
    private Collator getCollator() {
        if (myCollation == null) {
            try {
                myCollation = new RuleBasedCollator
                    (DEFAULTRULES + "& C < ch, cH, Ch, CH & Five, 5 & Four, 4 & one, 1 & Ampersand; '&' & Two, 2 ");
            } catch (Exception foo) {
                errln("Collator creation failed.");
                myCollation = (RuleBasedCollator)Collator.getInstance();
            }
        }
        return myCollation;
    }
}
