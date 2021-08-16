/*
 * Copyright (c) 2005, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 */

/*
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - 1999 - All Rights Reserved
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 */

package sun.text.resources.ext;

import java.util.ListResourceBundle;

public class CollationData_ja extends ListResourceBundle {

    protected final Object[][] getContents() {
        return new Object[][] {
            { "Rule",
                // Japanese JIS order
                // Ignorables
                "&\u0486;\u30fc"                         // L. MARK
                + ";\u3099"                                // V. MARK
                + ";\u309A"                                // SV. MARK
                + ";\u30fb"                                // Kana MIDDLE DOT
                + ",\u303F"                                // IDEOGRAPHIC HALF FILL SPACE

                // Symbols
                + "&\u2212 <\u309b"                       // Kana-Gana V. SOUND MARK
                + "<\u309c"                                // Kana-Gana SV. SOUND MARK
                + "<\u309d"                                // Gana ITERATION MARK
                + ",\u30fd"                                // Kana ITERATION MARK
                + "<\u309e"                                // Gana V. ITERATION MARK
                + ",\u30fe"                                // Kana V. ITERATION MARK

                // Hiragana/Kanakana Letters
                // Remember that decomposition automatically turns
                // <ga> into <ka><length>, so <ga>,etc. don't need separate rules

                + "& Z <\u3042"                            // Gana A
                + ",\u3041"                                // Gana SMALL A
                + ",\u30a2"                                // Kana A
                + ",\u30a1"                                // Kana SMALL A

                + "<\u3044"                                // Gana I
                + ",\u3043"                                // Gana SMALL I
                + ",\u30a4"                                // Kana I
                + ",\u30a3"                                // Kana SMALL I

                + "<\u3046"                                // Gana U
                + ",\u3045"                                // Gana SMALL U
                + ",\u30a6"                                // Kana U
                + ",\u30a5"                                // Kana SMALL U

                + "<\u3048"                                // Gana E
                + ",\u3047"                                // Gana SMALL E
                + ",\u30a8"                                // Kana E
                + ",\u30a7"                                // Kana SMALL E

                + "<\u304a"                                // Gana O
                + ",\u3049"                                // Gana SMALL O
                + ",\u30aa"                                // Kana O
                + ",\u30a9"                                // Kana SMALL O

                + "<\u304b"                                // Gana KA
                + ",\u30ab"                                // Kana KA
                + ",\u30f5"                                // Kana SMALL KA
                + "<\u304d"                                // Gana KI
                + ",\u30ad"                                // Kana KI
                + "<\u304f"     // Gana KU
                + ",\u30af"     // Kana KU
                + "<\u3051"     // Gana KE
                + ",\u30b1"     // Kana KE
                + ",\u30f6"     // Kana SMALL KE
                + "<\u3053"     // Gana KO
                + ",\u30b3"     // Kana KO

                + "<\u3055"     // Gana SA
                + ",\u30b5"     // Kana SA
                + "<\u3057"     // Gana SI
                + ",\u30b7"     // Kana SI
                + "<\u3059"     // Gana SU
                + ",\u30b9"     // Kana SU
                + "<\u305b"     // Gana SE
                + ",\u30bb"     // Kana SE
                + "<\u305d"     // Gana SO
                + ",\u30bd"     // Kana SO

                + "<\u305f"     // Gana TA
                + ",\u30bf"     // Kana TA
                + "<\u3061"     // Gana TI
                + ",\u30c1"     // Kana TI
                + "<\u3064"     // Gana TU
                + ",\u3063"     // Gana SMALL TU
                + ",\u30c4"     // Kana TU
                + ",\u30c3"     // Kana SMALL TU
                + "<\u3066"     // Gana TE
                + ",\u30c6"     // Kana TE
                + "<\u3068"     // Gana TO
                + ",\u30c8"     // Kana TO

                + "<\u306a"     // Gana NA
                + ",\u30ca"     // Kana NA
                + "<\u306b"     // Gana NI
                + ",\u30cb"     // Kana NI
                + "<\u306c"     // Gana NU
                + ",\u30cc"     // Kana NU
                + "<\u306d"     // Gana NE
                + ",\u30cd"     // Kana NE
                + "<\u306e"     // Gana NO
                + ",\u30ce"     // Kana NO

                + "<\u306f"     // Gana HA
                + ",\u30cf"     // Kana HA
                + "<\u3072"     // Gana HI
                + ",\u30d2"     // Kana HI
                + "<\u3075"     // Gana HU
                + ",\u30d5"     // Kana HU
                + "<\u3078"     // Gana HE
                + ",\u30d8"     // Kana HE
                + "<\u307b"     // Gana HO
                + ",\u30db"     // Kana HO

                + "<\u307e"     // Gana MA
                + ",\u30de"     // Kana MA
                + "<\u307f"     // Gana MI
                + ",\u30df"     // Kana MI
                + "<\u3080"     // Gana MU
                + ",\u30e0"     // Kana MU
                + "<\u3081"     // Gana ME
                + ",\u30e1"     // Kana ME
                + "<\u3082"     // Gana MO
                + ",\u30e2"     // Kana MO

                + "<\u3084"     // Gana YA
                + ",\u3083"     // Gana SMALL YA
                + ",\u30e4"     // Kana YA
                + ",\u30e3"     // Kana SMALL YA
                + "<\u3086"     // Gana YU
                + ",\u3085"     // Gana SMALL YU
                + ",\u30e6"     // Kana YU
                + ",\u30e5"     // Kana SMALL YU
                + "<\u3088"     // Gana YO
                + ",\u3087"     // Gana SMALL YO
                + ",\u30e8"     // Kana YO
                + ",\u30e7"     // Kana SMALL YO

                + "<\u3089"     // Gana RA
                + ",\u30e9"     // Kana RA
                + "<\u308a"     // Gana RI
                + ",\u30ea"     // Kana RI
                + "<\u308b"     // Gana RU
                + ",\u30eb"     // Kana RU
                + "<\u308c"     // Gana RE
                + ",\u30ec"     // Kana RE
                + "<\u308d"     // Gana RO
                + ",\u30ed"     // Kana RO

                + "<\u308f"     // Gana WA
                + ",\u308e"     // Gana SMALL WA
                + ",\u30ef"     // Kana WA
                + ",\u30ee"     // Kana SMALL WA
                + "<\u3090"     // Gana WI
                + ",\u30f0"     // Kana WI
                + "<\u3091"     // Gana WE
                + ",\u30f1"     // Kana WE
                + "<\u3092"     // Gana WO
                + ",\u30f2"     // Kana WO

                + "<\u3093"     // Gana N
                + ",\u30f3"     // Kana N

                // START Handle Length mark.
                // This is optional: some dictionaries just ignore length mark
                // If you want to do that, comment out down to "END Handle Length mark."
                // These rules handle matching the length mark to the right vowel.
                // <ka><length> == <ka><a>, <ki><length> == <ki><i>, etc.
                + "&\u30a2\u30a2 ,\u30a2\u30fc"     // Kana A
                + "&\u30a1\u30a2 ,\u30a1\u30fc"     // Kana SMALL A
                + "&\u30a4\u30a4 ,\u30a4\u30fc"     // Kana I
                + "&\u30a3\u30a4 ,\u30a3\u30fc"     // Kana SMALL I
                + "&\u30a6\u30a6 ,\u30a6\u30fc"     // Kana U
                + "&\u30a5\u30a6 ,\u30a5\u30fc"     // Kana SMALL U
                + "&\u30a8\u30a8 ,\u30a8\u30fc"     // Kana E
                + "&\u30a7\u30a8 ,\u30a7\u30fc"     // Kana SMALL E
                + "&\u30aa\u30aa ,\u30aa\u30fc"     // Kana O
                + "&\u30a9\u30aa ,\u30a9\u30fc"     // Kana SMALL O

                + "&\u30ab\u30a2 ,\u30ab\u30fc"     // Kana KA
                + "&\u30f5\u30a2 ,\u30f5\u30fc"     // Kana SMALL KA
                + "&\u30ad\u30a4 ,\u30ad\u30fc"     // Kana KI
                + "&\u30af\u30a6 ,\u30af\u30fc"     // Kana KU
                + "&\u30b1\u30a8 ,\u30b1\u30fc"     // Kana KE
                + "&\u30f6\u30a8 ,\u30f6\u30fc"     // Kana SMALL KE
                + "&\u30b3\u30aa ,\u30b3\u30fc"     // Kana KO

                + "&\u30b5\u30a2 ,\u30b5\u30fc"     // Kana SA
                + "&\u30b7\u30a4 ,\u30b7\u30fc"     // Kana SI
                + "&\u30b9\u30a6 ,\u30b9\u30fc"     // Kana SU
                + "&\u30bb\u30a8 ,\u30bb\u30fc"     // Kana SE
                + "&\u30bd\u30aa ,\u30bd\u30fc"     // Kana SO

                + "&\u30bf\u30a2 ,\u30bf\u30fc"     // Kana TA
                + "&\u30c1\u30a4 ,\u30c1\u30fc"     // Kana TI
                + "&\u30c4\u30a6 ,\u30c4\u30fc"     // Kana TU
                + "&\u30c3\u30a6 ,\u30c3\u30fc"     // Kana SMALL TU
                + "&\u30c6\u30a8 ,\u30c6\u30fc"     // Kana TE
                + "&\u30c8\u30aa ,\u30c8\u30fc"     // Kana TO

                + "&\u30ca\u30a2 ,\u30ca\u30fc"     // Kana NA
                + "&\u30cb\u30a4 ,\u30cb\u30fc"     // Kana NI
                + "&\u30cc\u30a6 ,\u30cc\u30fc"     // Kana NU
                + "&\u30cd\u30a8 ,\u30cd\u30fc"     // Kana NE
                + "&\u30ce\u30aa ,\u30ce\u30fc"     // Kana NO

                + "&\u30cf\u30a2 ,\u30cf\u30fc"     // Kana HA
                + "&\u30d2\u30a4 ,\u30d2\u30fc"     // Kana HI
                + "&\u30d5\u30a6 ,\u30d5\u30fc"     // Kana HU
                + "&\u30d8\u30a8 ,\u30d8\u30fc"     // Kana HE
                + "&\u30db\u30aa ,\u30db\u30fc"     // Kana HO

                + "&\u30de\u30a2 ,\u30de\u30fc"     // Kana MA
                + "&\u30df\u30a4 ,\u30df\u30fc"     // Kana MI
                + "&\u30e0\u30a6 ,\u30e0\u30fc"     // Kana MU
                + "&\u30e1\u30a8 ,\u30e1\u30fc"     // Kana ME
                + "&\u30e2\u30aa ,\u30e2\u30fc"     // Kana MO

                + "&\u30e4\u30a2 ,\u30e4\u30fc"     // Kana YA
                + "&\u30e3\u30a2 ,\u30e3\u30fc"     // Kana SMALL YAMARK
                + "&\u30e6\u30a6 ,\u30e6\u30fc"     // Kana YU
                + "&\u30e5\u30a6 ,\u30e5\u30fc"     // Kana SMALL YUMARK
                + "&\u30e8\u30aa ,\u30e8\u30fc"     // Kana YO
                + "&\u30e7\u30aa ,\u30e7\u30fc"     // Kana SMALL YOMARK

                + "&\u30e9\u30a2 ,\u30e9\u30fc"     // Kana RA
                + "&\u30ea\u30a4 ,\u30ea\u30fc"     // Kana RI
                + "&\u30eb\u30a6 ,\u30eb\u30fc"     // Kana RU
                + "&\u30ec\u30a8 ,\u30ec\u30fc"     // Kana RE
                + "&\u30ed\u30aa ,\u30ed\u30fc"     // Kana RO

                + "&\u30ef\u30a2 ,\u30ef\u30fc"     // Kana WA
                + "&\u30ee\u30a2 ,\u30ee\u30fc"     // Kana SMALL WA
                + "&\u30f0\u30a4 ,\u30f0\u30fc"     // Kana WI
                + "&\u30f1\u30a8 ,\u30f1\u30fc"     // Kana WE
                + "&\u30f2\u30aa ,\u30f2\u30fc"     // Kana WO

                // And now, with voice marks
                // <ga><length> == <ka><voice><length> == <ka><voice>><a>

                + "&\u30ab\u309b\u30a2 ,\u30ab\u309b\u30fc"     // Kana KA + voice = GA
                + "&\u30ad\u309b\u30a4 ,\u30ad\u309b\u30fc"     // Kana KI
                + "&\u30af\u309b\u30a6 ,\u30af\u309b\u30fc"     // Kana KU
                + "&\u30b1\u309b\u30a8 ,\u30b1\u309b\u30fc"     // Kana KE
                + "&\u30b3\u309b\u30aa ,\u30b3\u309b\u30fc"     // Kana KO

                + "&\u30b5\u309b\u30a2 ,\u30b5\u309b\u30fc"     // Kana SA
                + "&\u30b7\u309b\u30a4 ,\u30b7\u309b\u30fc"     // Kana SI
                + "&\u30b9\u309b\u30a6 ,\u30b9\u309b\u30fc"     // Kana SU
                + "&\u30bb\u309b\u30a8 ,\u30bb\u309b\u30fc"     // Kana SE
                + "&\u30bd\u309b\u30aa ,\u30bd\u309b\u30fc"     // Kana SO

                + "&\u30bf\u309b\u30a2 ,\u30bf\u309b\u30fc"     // Kana TA
                + "&\u30c1\u309b\u30a4 ,\u30c1\u309b\u30fc"     // Kana TI
                + "&\u30c4\u309b\u30a6 ,\u30c4\u309b\u30fc"     // Kana TU
                + "&\u30c6\u309b\u30a8 ,\u30c6\u309b\u30fc"     // Kana TE
                + "&\u30c8\u309b\u30aa ,\u30c8\u309b\u30fc"     // Kana TO

                + "&\u30cf\u309b\u30a2 ,\u30cf\u309b\u30fc"     // Kana HA
                + "&\u30d2\u309b\u30a4 ,\u30d2\u309b\u30fc"     // Kana HI
                + "&\u30d5\u309b\u30a6 ,\u30d5\u309b\u30fc"     // Kana HU
                + "&\u30d8\u309b\u30a8 ,\u30d8\u309b\u30fc"     // Kana HE
                + "&\u30db\u309b\u30aa ,\u30db\u309b\u30fc"     // Kana HO

                + "&\u30a6\u309b\u30a6 ,\u30a6\u309b\u30fc"     // Kana U
                + "&\u30f0\u309b\u30a4 ,\u30f0\u309b\u30fc"     // Kana WI
                + "&\u30f1\u309b\u30a8 ,\u30f1\u309b\u30fc"     // Kana WE
                + "&\u30f2\u309b\u30aa ,\u30f2\u309b\u30fc"     // Kana WO

                // And now, with semi-voice marks

                + "&\u30cf\u309c\u30a2 ,\u30cf\u309c\u30fc"     // Kana HA + semi-voice = PA
                + "&\u30d2\u309c\u30a4 ,\u30d2\u309c\u30fc"     // Kana HI
                + "&\u30d5\u309c\u30a6 ,\u30d5\u309c\u30fc"     // Kana HU
                + "&\u30d8\u309c\u30a8 ,\u30d8\u309c\u30fc"     // Kana HE
                + "&\u30db\u309c\u30aa ,\u30db\u309c\u30fc"     // Kana HO

                // END Handle Length mark.

                + "& \u30f3" +   // RESET TO END AFTER &'s
                " <\u3001" + // IDEOGRAPHIC COMMA,Po
                " <\u3002" + // IDEOGRAPHIC FULL STOP,Po
                " <\u30fb" + // Kana MIDDLE DOT,Po

                " <\u3003" + // DITTO MARK,Po
                " <\u4edd" + // [CJK Unified Ideographs],
                " <\u3005" + // IDEOGRAPHIC ITERATION MARK,Lm
                " <\u3006" + // IDEOGRAPHIC CLOSING MARK,Po
                " <\u3007" + // IDEOGRAPHIC NUMBER ZERO,Nl
                " <\u30fc" + // Kana-Gana PROLONGED SOUND MARK,Lm
                " <\u2014" + // EM DASH,Pd
                " <\u2010" + // HYPHEN,Pd
                " <\u301c" + // WAVE DASH,Pd
                " <\u2016" + // DOUBLE VERTICAL LINE,Po
                " <\u2026" + // HORIZONTAL ELLIPSIS,Po
                " <\u2025" + // TWO DOT LEADER,Po
                " <\u2018" + // LEFT SINGLE QUOTATION MARK,Ps
                " <\u2019" + // RIGHT SINGLE QUOTATION MARK,Pe
                " <\u201c" + // LEFT DOUBLE QUOTATION MARK,Ps
                " <\u201d" + // RIGHT DOUBLE QUOTATION MARK,Pe
                " <\u3014" + // LEFT TORTOISE SHELL BRACKET,Ps
                " <\u3015" + // RIGHT TORTOISE SHELL BRACKET,Pe
                " <\u3008" + // LEFT ANGLE BRACKET,Ps
                " <\u3009" + // RIGHT ANGLE BRACKET,Pe
                " <\u300a" + // LEFT DOUBLE ANGLE BRACKET,Ps
                " <\u300b" + // RIGHT DOUBLE ANGLE BRACKET,Pe
                " <\u300c" + // LEFT CORNER BRACKET,Ps
                " <\u300d" + // RIGHT CORNER BRACKET,Pe
                " <\u300e" + // LEFT WHITE CORNER BRACKET,Ps
                " <\u300f" + // RIGHT WHITE CORNER BRACKET,Pe
                " <\u3010" + // LEFT BLACK LENTICULAR BRACKET,Ps
                " <\u3011" + // RIGHT BLACK LENTICULAR BRACKET,Pe
                " <\u2212" + // MINUS SIGN,Sm
                " <\u00b1" + // PLUS-MINUS SIGN,Sm
                " <\u00d7" + // MULTIPLICATION SIGN,Sm
                " <\u00f7" + // DIVISION SIGN,Sm
                " <\u2260" + // NOT EQUAL TO,Sm
                " <\u2266" + // LESS-THAN OVER EQUAL TO,Sm
                " <\u2267" + // GREATER-THAN OVER EQUAL TO,Sm
                " <\u221e" + // INFINITY,Sm
                " <\u2234" + // THEREFORE,Sm
                " <\u2642" + // MALE SIGN,So
                " <\u2640" + // FEMALE SIGN,So
                " <\u00b0" + // DEGREE SIGN,So
                " <\u2032" + // PRIME,Po
                " <\u2033" + // DOUBLE PRIME,Po
                " <\u2103" + // DEGREE CELSIUS,So
                " <\u00a7" + // SECTION SIGN,So
                " <\u2606" + // WHITE STAR,So
                " <\u2605" + // BLACK STAR,So
                " <\u25cb" + // WHITE CIRCLE,So
                " <\u25cf" + // BLACK CIRCLE,So
                " <\u25ce" + // BULLSEYE,So
                " <\u25c7" + // WHITE DIAMOND,So
                " <\u25c6" + // BLACK DIAMOND,So
                " <\u25a1" + // WHITE SQUARE,So
                " <\u25a0" + // BLACK SQUARE,So
                " <\u25b3" + // WHITE UP-POINTING TRIANGLE,So
                " <\u25b2" + // BLACK UP-POINTING TRIANGLE,So
                " <\u25bd" + // WHITE DOWN-POINTING TRIANGLE,So
                " <\u25bc" + // BLACK DOWN-POINTING TRIANGLE,So
                " <\u203b" + // REFERENCE MARK,Po
                " <\u3012" + // POSTAL MARK,So
                " <\u2192" + // RIGHTWARDS ARROW,Sm
                " <\u2190" + // LEFTWARDS ARROW,Sm
                " <\u2191" + // UPWARDS ARROW,Sm
                " <\u2193" + // DOWNWARDS ARROW,Sm
                " <\u3013" + // GETA MARK,So
                " <\u2208" + // ELEMENT OF,Sm
                " <\u220b" + // CONTAINS AS MEMBER,Sm
                " <\u2286" + // SUBSET OF OR EQUAL TO,Sm
                " <\u2287" + // SUPERSET OF OR EQUAL TO,Sm
                " <\u2282" + // SUBSET OF,Sm
                " <\u2283" + // SUPERSET OF,Sm
                " <\u222a" + // UNION,Sm
                " <\u2229" + // INTERSECTION,Sm
                " <\u2227" + // LOGICAL AND,Sm
                " <\u2228" + // LOGICAL OR,Sm
                " <\u00ac" + // FULLWIDTH NOT SIGN,Sm
                " <\u21d2" + // RIGHTWARDS DOUBLE ARROW,Sm
                " <\u21d4" + // LEFT RIGHT DOUBLE ARROW,Sm
                " <\u2200" + // FOR ALL,Sm
                " <\u2203" + // THERE EXISTS,Sm
                " <\u2220" + // ANGLE,Sm
                " <\u22a5" + // UP TACK,Sm
                " <\u2312" + // ARC,So
                " <\u2202" + // PARTIAL DIFFERENTIAL,Sm
                " <\u2207" + // NABLA,Sm
                " <\u2261" + // IDENTICAL TO,Sm
                " <\u2252" + // APPROXIMATELY EQUAL TO OR THE IMAGE OF,Sm
                " <\u226a" + // MUCH LESS-THAN,Sm
                " <\u226b" + // MUCH GREATER-THAN,Sm
                " <\u221a" + // SQUARE ROOT,Sm
                " <\u223d" + // REVERSED TILDE,Sm
                " <\u221d" + // PROPORTIONAL TO,Sm
                " <\u2235" + // BECAUSE,Sm
                " <\u222b" + // INTEGRAL,Sm
                " <\u222c" + // DOUBLE INTEGRAL,Sm
                " <\u212b" + // ANGSTROM SIGN,Lu
                " <\u2030" + // PER MILLE SIGN,Po
                " <\u266f" + // MUSIC SHARP SIGN,So
                " <\u266d" + // MUSIC FLAT SIGN,So
                " <\u266a" + // EIGHTH NOTE,So
                " <\u2020" + // DAGGER,Po
                " <\u2021" + // DOUBLE DAGGER,Po
                " <\u00b6" + // PILCROW SIGN,So
                " <\u25ef" + // LARGE CIRCLE,So

                " <\u03b1" + // GREEK SMALL LETTER ALPHA,Ll
                " ,\u0391" + // GREEK CAPITAL LETTER ALPHA,Lu
                " <\u03b2" + // GREEK SMALL LETTER BETA,Ll
                " ,\u0392" + // GREEK CAPITAL LETTER BETA,Lu
                " <\u03b3" + // GREEK SMALL LETTER GAMMA,Ll
                " ,\u0393" + // GREEK CAPITAL LETTER GAMMA,Lu
                " <\u03b4" + // GREEK SMALL LETTER DELTA,Ll
                " ,\u0394" + // GREEK CAPITAL LETTER DELTA,Lu
                " <\u03b5" + // GREEK SMALL LETTER EPSILON,Ll
                " ,\u0395" + // GREEK CAPITAL LETTER EPSILON,Lu
                " <\u03b6" + // GREEK SMALL LETTER ZETA,Ll
                " ,\u0396" + // GREEK CAPITAL LETTER ZETA,Lu
                " <\u03b7" + // GREEK SMALL LETTER ETA,Ll
                " ,\u0397" + // GREEK CAPITAL LETTER ETA,Lu
                " <\u03b8" + // GREEK SMALL LETTER THETA,Ll
                " ,\u0398" + // GREEK CAPITAL LETTER THETA,Lu
                " <\u03b9" + // GREEK SMALL LETTER IOTA,Ll
                " ,\u0399" + // GREEK CAPITAL LETTER IOTA,Lu
                " <\u03ba" + // GREEK SMALL LETTER KAPPA,Ll
                " ,\u039a" + // GREEK CAPITAL LETTER KAPPA,Lu
                " <\u03bb" + // GREEK SMALL LETTER LAMDA,Ll
                " ,\u039b" + // GREEK CAPITAL LETTER LAMDA,Lu
                " <\u03bc" + // GREEK SMALL LETTER MU,Ll
                " ,\u039c" + // GREEK CAPITAL LETTER MU,Lu
                " <\u03bd" + // GREEK SMALL LETTER NU,Ll
                " ,\u039d" + // GREEK CAPITAL LETTER NU,Lu
                " <\u03be" + // GREEK SMALL LETTER XI,Ll
                " ,\u039e" + // GREEK CAPITAL LETTER XI,Lu
                " <\u03bf" + // GREEK SMALL LETTER OMICRON,Ll
                " ,\u039f" + // GREEK CAPITAL LETTER OMICRON,Lu
                " <\u03c0" + // GREEK SMALL LETTER PI,Ll
                " ,\u03a0" + // GREEK CAPITAL LETTER PI,Lu
                " <\u03c1" + // GREEK SMALL LETTER RHO,Ll
                " ,\u03a1" + // GREEK CAPITAL LETTER RHO,Lu
                " <\u03c3" + // GREEK SMALL LETTER SIGMA,Ll
                " ,\u03a3" + // GREEK CAPITAL LETTER SIGMA,Lu
                " <\u03c4" + // GREEK SMALL LETTER TAU,Ll
                " ,\u03a4" + // GREEK CAPITAL LETTER TAU,Lu
                " <\u03c5" + // GREEK SMALL LETTER UPSILON,Ll
                " ,\u03a5" + // GREEK CAPITAL LETTER UPSILON,Lu
                " <\u03c6" + // GREEK SMALL LETTER PHI,Ll
                " ,\u03a6" + // GREEK CAPITAL LETTER PHI,Lu
                " <\u03c7" + // GREEK SMALL LETTER CHI,Ll
                " ,\u03a7" + // GREEK CAPITAL LETTER CHI,Lu
                " <\u03c8" + // GREEK SMALL LETTER PSI,Ll
                " ,\u03a8" + // GREEK CAPITAL LETTER PSI,Lu
                " <\u03c9" + // GREEK SMALL LETTER OMEGA,Ll
                " ,\u03a9" + // GREEK CAPITAL LETTER OMEGA,Lu


                " <\u0430" + // CYRILLIC SMALL LETTER A,Ll
                " ,\u0410" + // CYRILLIC CAPITAL LETTER A,Lu
                " <\u0431" + // CYRILLIC SMALL LETTER BE,Ll
                " ,\u0411" + // CYRILLIC CAPITAL LETTER BE,Lu
                " <\u0432" + // CYRILLIC SMALL LETTER VE,Ll
                " ,\u0412" + // CYRILLIC CAPITAL LETTER VE,Lu
                " <\u0433" + // CYRILLIC SMALL LETTER GHE,Ll
                " ,\u0413" + // CYRILLIC CAPITAL LETTER GHE,Lu
                " <\u0434" + // CYRILLIC SMALL LETTER DE,Ll
                " ,\u0414" + // CYRILLIC CAPITAL LETTER DE,Lu
                " <\u0435" + // CYRILLIC SMALL LETTER IE,Ll
                " ,\u0415" + // CYRILLIC CAPITAL LETTER IE,Lu
                " <\u0451" + // CYRILLIC SMALL LETTER IO,Ll
                " ,\u0401" + // CYRILLIC CAPITAL LETTER IO,Lu
                " <\u0436" + // CYRILLIC SMALL LETTER ZHE,Ll
                " ,\u0416" + // CYRILLIC CAPITAL LETTER ZHE,Lu
                " <\u0437" + // CYRILLIC SMALL LETTER ZE,Ll
                " ,\u0417" + // CYRILLIC CAPITAL LETTER ZE,Lu
                " <\u0438" + // CYRILLIC SMALL LETTER I,Ll
                " ,\u0418" + // CYRILLIC CAPITAL LETTER I,Lu
                " <\u0439" + // CYRILLIC SMALL LETTER SHORT I,Ll
                " ,\u0419" + // CYRILLIC CAPITAL LETTER SHORT I,Lu
                " <\u043a" + // CYRILLIC SMALL LETTER KA,Ll
                " ,\u041a" + // CYRILLIC CAPITAL LETTER KA,Lu
                " <\u043b" + // CYRILLIC SMALL LETTER EL,Ll
                " ,\u041b" + // CYRILLIC CAPITAL LETTER EL,Lu
                " <\u043c" + // CYRILLIC SMALL LETTER EM,Ll
                " ,\u041c" + // CYRILLIC CAPITAL LETTER EM,Lu
                " <\u043d" + // CYRILLIC SMALL LETTER EN,Ll
                " ,\u041d" + // CYRILLIC CAPITAL LETTER EN,Lu
                " <\u043e" + // CYRILLIC SMALL LETTER O,Ll
                " ,\u041e" + // CYRILLIC CAPITAL LETTER O,Lu
                " <\u043f" + // CYRILLIC SMALL LETTER PE,Ll
                " ,\u041f" + // CYRILLIC CAPITAL LETTER PE,Lu
                " <\u0440" + // CYRILLIC SMALL LETTER ER,Ll
                " ,\u0420" + // CYRILLIC CAPITAL LETTER ER,Lu
                " <\u0441" + // CYRILLIC SMALL LETTER ES,Ll
                " ,\u0421" + // CYRILLIC CAPITAL LETTER ES,Lu
                " <\u0442" + // CYRILLIC SMALL LETTER TE,Ll
                " ,\u0422" + // CYRILLIC CAPITAL LETTER TE,Lu
                " <\u0443" + // CYRILLIC SMALL LETTER U,Ll
                " ,\u0423" + // CYRILLIC CAPITAL LETTER U,Lu
                " <\u0444" + // CYRILLIC SMALL LETTER EF,Ll
                " ,\u0424" + // CYRILLIC CAPITAL LETTER EF,Lu
                " <\u0445" + // CYRILLIC SMALL LETTER HA,Ll
                " ,\u0425" + // CYRILLIC CAPITAL LETTER HA,Lu
                " <\u0446" + // CYRILLIC SMALL LETTER TSE,Ll
                " ,\u0426" + // CYRILLIC CAPITAL LETTER TSE,Lu
                " <\u0447" + // CYRILLIC SMALL LETTER CHE,Ll
                " ,\u0427" + // CYRILLIC CAPITAL LETTER CHE,Lu
                " <\u0448" + // CYRILLIC SMALL LETTER SHA,Ll
                " ,\u0428" + // CYRILLIC CAPITAL LETTER SHA,Lu
                " <\u0449" + // CYRILLIC SMALL LETTER SHCHA,Ll
                " ,\u0429" + // CYRILLIC CAPITAL LETTER SHCHA,Lu
                " <\u044a" + // CYRILLIC SMALL LETTER HARD SIGN,Ll
                " ,\u042a" + // CYRILLIC CAPITAL LETTER HARD SIGN,Lu
                " <\u044b" + // CYRILLIC SMALL LETTER YERU,Ll
                " ,\u042b" + // CYRILLIC CAPITAL LETTER YERU,Lu
                " <\u044c" + // CYRILLIC SMALL LETTER SOFT SIGN,Ll
                " ,\u042c" + // CYRILLIC CAPITAL LETTER SOFT SIGN,Lu
                " <\u044d" + // CYRILLIC SMALL LETTER E,Ll
                " ,\u042d" + // CYRILLIC CAPITAL LETTER E,Lu
                " <\u044e" + // CYRILLIC SMALL LETTER YU,Ll
                " ,\u042e" + // CYRILLIC CAPITAL LETTER YU,Lu
                " <\u044f" + // CYRILLIC SMALL LETTER YA,Ll
                " ,\u042f" + // CYRILLIC CAPITAL LETTER YA,Lu


                " <\u2500" + // BOX DRAWINGS LIGHT HORIZONTAL,So
                " <\u2502" + // BOX DRAWINGS LIGHT VERTICAL,So
                " <\u250c" + // BOX DRAWINGS LIGHT DOWN AND RIGHT,So
                " <\u2510" + // BOX DRAWINGS LIGHT DOWN AND LEFT,So
                " <\u2518" + // BOX DRAWINGS LIGHT UP AND LEFT,So
                " <\u2514" + // BOX DRAWINGS LIGHT UP AND RIGHT,So
                " <\u251c" + // BOX DRAWINGS LIGHT VERTICAL AND RIGHT,So
                " <\u252c" + // BOX DRAWINGS LIGHT DOWN AND HORIZONTAL,So
                " <\u2524" + // BOX DRAWINGS LIGHT VERTICAL AND LEFT,So
                " <\u2534" + // BOX DRAWINGS LIGHT UP AND HORIZONTAL,So
                " <\u253c" + // BOX DRAWINGS LIGHT VERTICAL AND HORIZONTAL,So
                " <\u2501" + // BOX DRAWINGS HEAVY HORIZONTAL,So
                " <\u2503" + // BOX DRAWINGS HEAVY VERTICAL,So
                " <\u250f" + // BOX DRAWINGS HEAVY DOWN AND RIGHT,So
                " <\u2513" + // BOX DRAWINGS HEAVY DOWN AND LEFT,So
                " <\u251b" + // BOX DRAWINGS HEAVY UP AND LEFT,So
                " <\u2517" + // BOX DRAWINGS HEAVY UP AND RIGHT,So
                " <\u2523" + // BOX DRAWINGS HEAVY VERTICAL AND RIGHT,So
                " <\u2533" + // BOX DRAWINGS HEAVY DOWN AND HORIZONTAL,So
                " <\u252b" + // BOX DRAWINGS HEAVY VERTICAL AND LEFT,So
                " <\u253b" + // BOX DRAWINGS HEAVY UP AND HORIZONTAL,So
                " <\u254b" + // BOX DRAWINGS HEAVY VERTICAL AND HORIZONTAL,So
                " <\u2520" + // BOX DRAWINGS VERTICAL HEAVY AND RIGHT LIGHT,So
                " <\u252f" + // BOX DRAWINGS DOWN LIGHT AND HORIZONTAL HEAVY,So
                " <\u2528" + // BOX DRAWINGS VERTICAL HEAVY AND LEFT LIGHT,So
                " <\u2537" + // BOX DRAWINGS UP LIGHT AND HORIZONTAL HEAVY,So
                " <\u253f" + // BOX DRAWINGS VERTICAL LIGHT AND HORIZONTAL HEAVY,So
                " <\u251d" + // BOX DRAWINGS VERTICAL LIGHT AND RIGHT HEAVY,So
                " <\u2530" + // BOX DRAWINGS DOWN HEAVY AND HORIZONTAL LIGHT,So
                " <\u2525" + // BOX DRAWINGS VERTICAL LIGHT AND LEFT HEAVY,So
                " <\u2538" + // BOX DRAWINGS UP HEAVY AND HORIZONTAL LIGHT,So
                " <\u2542" + // BOX DRAWINGS VERTICAL HEAVY AND HORIZONTAL LIGHT,So

                // JIS0208 Ideographs are in JIS order.
                "<\u4e9c<\u5516<\u5a03<\u963f<\u54c0" +
                "<\u611b<\u6328<\u59f6<\u9022<\u8475<\u831c<\u7a50<\u60aa" +
                "<\u63e1<\u6e25<\u65ed<\u8466<\u82a6<\u9bf5<\u6893<\u5727" +
                "<\u65a1<\u6271<\u5b9b<\u59d0<\u867b<\u98f4<\u7d62<\u7dbe" +
                "<\u9b8e<\u6216<\u7c9f<\u88b7<\u5b89<\u5eb5<\u6309<\u6697" +
                "<\u6848<\u95c7<\u978d<\u674f<\u4ee5<\u4f0a<\u4f4d<\u4f9d" +
                "<\u5049<\u56f2<\u5937<\u59d4<\u5a01<\u5c09<\u60df<\u610f" +
                "<\u6170<\u6613<\u6905<\u70ba<\u754f<\u7570<\u79fb<\u7dad" +
                "<\u7def<\u80c3<\u840e<\u8863<\u8b02<\u9055<\u907a<\u533b" +
                "<\u4e95<\u4ea5<\u57df<\u80b2<\u90c1<\u78ef<\u4e00<\u58f1" +
                "<\u6ea2<\u9038<\u7a32<\u8328<\u828b<\u9c2f<\u5141<\u5370" +
                "<\u54bd<\u54e1<\u56e0<\u59fb<\u5f15<\u98f2<\u6deb<\u80e4" +
                "<\u852d<\u9662<\u9670<\u96a0<\u97fb<\u540b<\u53f3<\u5b87" +
                "<\u70cf<\u7fbd<\u8fc2<\u96e8<\u536f<\u9d5c<\u7aba<\u4e11" +
                "<\u7893<\u81fc<\u6e26<\u5618<\u5504<\u6b1d<\u851a<\u9c3b" +
                "<\u59e5<\u53a9<\u6d66<\u74dc<\u958f<\u5642<\u4e91<\u904b" +
                "<\u96f2<\u834f<\u990c<\u53e1<\u55b6<\u5b30<\u5f71<\u6620" +
                "<\u66f3<\u6804<\u6c38<\u6cf3<\u6d29<\u745b<\u76c8<\u7a4e" +
                "<\u9834<\u82f1<\u885b<\u8a60<\u92ed<\u6db2<\u75ab<\u76ca" +
                "<\u99c5<\u60a6<\u8b01<\u8d8a<\u95b2<\u698e<\u53ad<\u5186" +
                "<\u5712<\u5830<\u5944<\u5bb4<\u5ef6<\u6028<\u63a9<\u63f4" +
                "<\u6cbf<\u6f14<\u708e<\u7114<\u7159<\u71d5<\u733f<\u7e01" +
                "<\u8276<\u82d1<\u8597<\u9060<\u925b<\u9d1b<\u5869<\u65bc" +
                "<\u6c5a<\u7525<\u51f9<\u592e<\u5965<\u5f80<\u5fdc<\u62bc" +
                "<\u65fa<\u6a2a<\u6b27<\u6bb4<\u738b<\u7fc1<\u8956<\u9d2c" +
                "<\u9d0e<\u9ec4<\u5ca1<\u6c96<\u837b<\u5104<\u5c4b<\u61b6" +
                "<\u81c6<\u6876<\u7261<\u4e59<\u4ffa<\u5378<\u6069<\u6e29" +
                "<\u7a4f<\u97f3<\u4e0b<\u5316<\u4eee<\u4f55<\u4f3d<\u4fa1" +
                "<\u4f73<\u52a0<\u53ef<\u5609<\u590f<\u5ac1<\u5bb6<\u5be1" +
                "<\u79d1<\u6687<\u679c<\u67b6<\u6b4c<\u6cb3<\u706b<\u73c2" +
                "<\u798d<\u79be<\u7a3c<\u7b87<\u82b1<\u82db<\u8304<\u8377" +
                "<\u83ef<\u83d3<\u8766<\u8ab2<\u5629<\u8ca8<\u8fe6<\u904e" +
                "<\u971e<\u868a<\u4fc4<\u5ce8<\u6211<\u7259<\u753b<\u81e5" +
                "<\u82bd<\u86fe<\u8cc0<\u96c5<\u9913<\u99d5<\u4ecb<\u4f1a" +
                "<\u89e3<\u56de<\u584a<\u58ca<\u5efb<\u5feb<\u602a<\u6094" +
                "<\u6062<\u61d0<\u6212<\u62d0<\u6539<\u9b41<\u6666<\u68b0" +
                "<\u6d77<\u7070<\u754c<\u7686<\u7d75<\u82a5<\u87f9<\u958b" +
                "<\u968e<\u8c9d<\u51f1<\u52be<\u5916<\u54b3<\u5bb3<\u5d16" +
                "<\u6168<\u6982<\u6daf<\u788d<\u84cb<\u8857<\u8a72<\u93a7" +
                "<\u9ab8<\u6d6c<\u99a8<\u86d9<\u57a3<\u67ff<\u86ce<\u920e" +
                "<\u5283<\u5687<\u5404<\u5ed3<\u62e1<\u64b9<\u683c<\u6838" +
                "<\u6bbb<\u7372<\u78ba<\u7a6b<\u899a<\u89d2<\u8d6b<\u8f03" +
                "<\u90ed<\u95a3<\u9694<\u9769<\u5b66<\u5cb3<\u697d<\u984d" +
                "<\u984e<\u639b<\u7b20<\u6a2b<\u6a7f<\u68b6<\u9c0d<\u6f5f" +
                "<\u5272<\u559d<\u6070<\u62ec<\u6d3b<\u6e07<\u6ed1<\u845b" +
                "<\u8910<\u8f44<\u4e14<\u9c39<\u53f6<\u691b<\u6a3a<\u9784" +
                "<\u682a<\u515c<\u7ac3<\u84b2<\u91dc<\u938c<\u565b<\u9d28" +
                "<\u6822<\u8305<\u8431<\u7ca5<\u5208<\u82c5<\u74e6<\u4e7e" +
                "<\u4f83<\u51a0<\u5bd2<\u520a<\u52d8<\u52e7<\u5dfb<\u559a" +
                "<\u582a<\u59e6<\u5b8c<\u5b98<\u5bdb<\u5e72<\u5e79<\u60a3" +
                "<\u611f<\u6163<\u61be<\u63db<\u6562<\u67d1<\u6853<\u68fa" +
                "<\u6b3e<\u6b53<\u6c57<\u6f22<\u6f97<\u6f45<\u74b0<\u7518" +
                "<\u76e3<\u770b<\u7aff<\u7ba1<\u7c21<\u7de9<\u7f36<\u7ff0" +
                "<\u809d<\u8266<\u839e<\u89b3<\u8acc<\u8cab<\u9084<\u9451" +
                "<\u9593<\u9591<\u95a2<\u9665<\u97d3<\u9928<\u8218<\u4e38" +
                "<\u542b<\u5cb8<\u5dcc<\u73a9<\u764c<\u773c<\u5ca9<\u7feb" +
                "<\u8d0b<\u96c1<\u9811<\u9854<\u9858<\u4f01<\u4f0e<\u5371" +
                "<\u559c<\u5668<\u57fa<\u5947<\u5b09<\u5bc4<\u5c90<\u5e0c" +
                "<\u5e7e<\u5fcc<\u63ee<\u673a<\u65d7<\u65e2<\u671f<\u68cb" +
                "<\u68c4<\u6a5f<\u5e30<\u6bc5<\u6c17<\u6c7d<\u757f<\u7948" +
                "<\u5b63<\u7a00<\u7d00<\u5fbd<\u898f<\u8a18<\u8cb4<\u8d77" +
                "<\u8ecc<\u8f1d<\u98e2<\u9a0e<\u9b3c<\u4e80<\u507d<\u5100" +
                "<\u5993<\u5b9c<\u622f<\u6280<\u64ec<\u6b3a<\u72a0<\u7591" +
                "<\u7947<\u7fa9<\u87fb<\u8abc<\u8b70<\u63ac<\u83ca<\u97a0" +
                "<\u5409<\u5403<\u55ab<\u6854<\u6a58<\u8a70<\u7827<\u6775" +
                "<\u9ecd<\u5374<\u5ba2<\u811a<\u8650<\u9006<\u4e18<\u4e45" +
                "<\u4ec7<\u4f11<\u53ca<\u5438<\u5bae<\u5f13<\u6025<\u6551" +
                "<\u673d<\u6c42<\u6c72<\u6ce3<\u7078<\u7403<\u7a76<\u7aae" +
                "<\u7b08<\u7d1a<\u7cfe<\u7d66<\u65e7<\u725b<\u53bb<\u5c45" +
                "<\u5de8<\u62d2<\u62e0<\u6319<\u6e20<\u865a<\u8a31<\u8ddd" +
                "<\u92f8<\u6f01<\u79a6<\u9b5a<\u4ea8<\u4eab<\u4eac<\u4f9b" +
                "<\u4fa0<\u50d1<\u5147<\u7af6<\u5171<\u51f6<\u5354<\u5321" +
                "<\u537f<\u53eb<\u55ac<\u5883<\u5ce1<\u5f37<\u5f4a<\u602f" +
                "<\u6050<\u606d<\u631f<\u6559<\u6a4b<\u6cc1<\u72c2<\u72ed" +
                "<\u77ef<\u80f8<\u8105<\u8208<\u854e<\u90f7<\u93e1<\u97ff" +
                "<\u9957<\u9a5a<\u4ef0<\u51dd<\u5c2d<\u6681<\u696d<\u5c40" +
                "<\u66f2<\u6975<\u7389<\u6850<\u7c81<\u50c5<\u52e4<\u5747" +
                "<\u5dfe<\u9326<\u65a4<\u6b23<\u6b3d<\u7434<\u7981<\u79bd" +
                "<\u7b4b<\u7dca<\u82b9<\u83cc<\u887f<\u895f<\u8b39<\u8fd1" +
                "<\u91d1<\u541f<\u9280<\u4e5d<\u5036<\u53e5<\u533a<\u72d7" +
                "<\u7396<\u77e9<\u82e6<\u8eaf<\u99c6<\u99c8<\u99d2<\u5177" +
                "<\u611a<\u865e<\u55b0<\u7a7a<\u5076<\u5bd3<\u9047<\u9685" +
                "<\u4e32<\u6adb<\u91e7<\u5c51<\u5c48<\u6398<\u7a9f<\u6c93" +
                "<\u9774<\u8f61<\u7aaa<\u718a<\u9688<\u7c82<\u6817<\u7e70" +
                "<\u6851<\u936c<\u52f2<\u541b<\u85ab<\u8a13<\u7fa4<\u8ecd" +
                "<\u90e1<\u5366<\u8888<\u7941<\u4fc2<\u50be<\u5211<\u5144" +
                "<\u5553<\u572d<\u73ea<\u578b<\u5951<\u5f62<\u5f84<\u6075" +
                "<\u6176<\u6167<\u61a9<\u63b2<\u643a<\u656c<\u666f<\u6842" +
                "<\u6e13<\u7566<\u7a3d<\u7cfb<\u7d4c<\u7d99<\u7e4b<\u7f6b" +
                "<\u830e<\u834a<\u86cd<\u8a08<\u8a63<\u8b66<\u8efd<\u981a" +
                "<\u9d8f<\u82b8<\u8fce<\u9be8<\u5287<\u621f<\u6483<\u6fc0" +
                "<\u9699<\u6841<\u5091<\u6b20<\u6c7a<\u6f54<\u7a74<\u7d50" +
                "<\u8840<\u8a23<\u6708<\u4ef6<\u5039<\u5026<\u5065<\u517c" +
                "<\u5238<\u5263<\u55a7<\u570f<\u5805<\u5acc<\u5efa<\u61b2" +
                "<\u61f8<\u62f3<\u6372<\u691c<\u6a29<\u727d<\u72ac<\u732e" +
                "<\u7814<\u786f<\u7d79<\u770c<\u80a9<\u898b<\u8b19<\u8ce2" +
                "<\u8ed2<\u9063<\u9375<\u967a<\u9855<\u9a13<\u9e78<\u5143" +
                "<\u539f<\u53b3<\u5e7b<\u5f26<\u6e1b<\u6e90<\u7384<\u73fe" +
                "<\u7d43<\u8237<\u8a00<\u8afa<\u9650<\u4e4e<\u500b<\u53e4" +
                "<\u547c<\u56fa<\u59d1<\u5b64<\u5df1<\u5eab<\u5f27<\u6238" +
                "<\u6545<\u67af<\u6e56<\u72d0<\u7cca<\u88b4<\u80a1<\u80e1" +
                "<\u83f0<\u864e<\u8a87<\u8de8<\u9237<\u96c7<\u9867<\u9f13" +
                "<\u4e94<\u4e92<\u4f0d<\u5348<\u5449<\u543e<\u5a2f<\u5f8c" +
                "<\u5fa1<\u609f<\u68a7<\u6a8e<\u745a<\u7881<\u8a9e<\u8aa4" +
                "<\u8b77<\u9190<\u4e5e<\u9bc9<\u4ea4<\u4f7c<\u4faf<\u5019" +
                "<\u5016<\u5149<\u516c<\u529f<\u52b9<\u52fe<\u539a<\u53e3" +
                "<\u5411<\u540e<\u5589<\u5751<\u57a2<\u597d<\u5b54<\u5b5d" +
                "<\u5b8f<\u5de5<\u5de7<\u5df7<\u5e78<\u5e83<\u5e9a<\u5eb7" +
                "<\u5f18<\u6052<\u614c<\u6297<\u62d8<\u63a7<\u653b<\u6602" +
                "<\u6643<\u66f4<\u676d<\u6821<\u6897<\u69cb<\u6c5f<\u6d2a" +
                "<\u6d69<\u6e2f<\u6e9d<\u7532<\u7687<\u786c<\u7a3f<\u7ce0" +
                "<\u7d05<\u7d18<\u7d5e<\u7db1<\u8015<\u8003<\u80af<\u80b1" +
                "<\u8154<\u818f<\u822a<\u8352<\u884c<\u8861<\u8b1b<\u8ca2" +
                "<\u8cfc<\u90ca<\u9175<\u9271<\u783f<\u92fc<\u95a4<\u964d" +
                "<\u9805<\u9999<\u9ad8<\u9d3b<\u525b<\u52ab<\u53f7<\u5408" +
                "<\u58d5<\u62f7<\u6fe0<\u8c6a<\u8f5f<\u9eb9<\u514b<\u523b" +
                "<\u544a<\u56fd<\u7a40<\u9177<\u9d60<\u9ed2<\u7344<\u6f09" +
                "<\u8170<\u7511<\u5ffd<\u60da<\u9aa8<\u72db<\u8fbc<\u6b64" +
                "<\u9803<\u4eca<\u56f0<\u5764<\u58be<\u5a5a<\u6068<\u61c7" +
                "<\u660f<\u6606<\u6839<\u68b1<\u6df7<\u75d5<\u7d3a<\u826e" +
                "<\u9b42<\u4e9b<\u4f50<\u53c9<\u5506<\u5d6f<\u5de6<\u5dee" +
                "<\u67fb<\u6c99<\u7473<\u7802<\u8a50<\u9396<\u88df<\u5750" +
                "<\u5ea7<\u632b<\u50b5<\u50ac<\u518d<\u6700<\u54c9<\u585e" +
                "<\u59bb<\u5bb0<\u5f69<\u624d<\u63a1<\u683d<\u6b73<\u6e08" +
                "<\u707d<\u91c7<\u7280<\u7815<\u7826<\u796d<\u658e<\u7d30" +
                "<\u83dc<\u88c1<\u8f09<\u969b<\u5264<\u5728<\u6750<\u7f6a" +
                "<\u8ca1<\u51b4<\u5742<\u962a<\u583a<\u698a<\u80b4<\u54b2" +
                "<\u5d0e<\u57fc<\u7895<\u9dfa<\u4f5c<\u524a<\u548b<\u643e" +
                "<\u6628<\u6714<\u67f5<\u7a84<\u7b56<\u7d22<\u932f<\u685c" +
                "<\u9bad<\u7b39<\u5319<\u518a<\u5237<\u5bdf<\u62f6<\u64ae" +
                "<\u64e6<\u672d<\u6bba<\u85a9<\u96d1<\u7690<\u9bd6<\u634c" +
                "<\u9306<\u9bab<\u76bf<\u6652<\u4e09<\u5098<\u53c2<\u5c71" +
                "<\u60e8<\u6492<\u6563<\u685f<\u71e6<\u73ca<\u7523<\u7b97" +
                "<\u7e82<\u8695<\u8b83<\u8cdb<\u9178<\u9910<\u65ac<\u66ab" +
                "<\u6b8b<\u4ed5<\u4ed4<\u4f3a<\u4f7f<\u523a<\u53f8<\u53f2" +
                "<\u55e3<\u56db<\u58eb<\u59cb<\u59c9<\u59ff<\u5b50<\u5c4d" +
                "<\u5e02<\u5e2b<\u5fd7<\u601d<\u6307<\u652f<\u5b5c<\u65af" +
                "<\u65bd<\u65e8<\u679d<\u6b62<\u6b7b<\u6c0f<\u7345<\u7949" +
                "<\u79c1<\u7cf8<\u7d19<\u7d2b<\u80a2<\u8102<\u81f3<\u8996" +
                "<\u8a5e<\u8a69<\u8a66<\u8a8c<\u8aee<\u8cc7<\u8cdc<\u96cc" +
                "<\u98fc<\u6b6f<\u4e8b<\u4f3c<\u4f8d<\u5150<\u5b57<\u5bfa" +
                "<\u6148<\u6301<\u6642<\u6b21<\u6ecb<\u6cbb<\u723e<\u74bd" +
                "<\u75d4<\u78c1<\u793a<\u800c<\u8033<\u81ea<\u8494<\u8f9e" +
                "<\u6c50<\u9e7f<\u5f0f<\u8b58<\u9d2b<\u7afa<\u8ef8<\u5b8d" +
                "<\u96eb<\u4e03<\u53f1<\u57f7<\u5931<\u5ac9<\u5ba4<\u6089" +
                "<\u6e7f<\u6f06<\u75be<\u8cea<\u5b9f<\u8500<\u7be0<\u5072" +
                "<\u67f4<\u829d<\u5c61<\u854a<\u7e1e<\u820e<\u5199<\u5c04" +
                "<\u6368<\u8d66<\u659c<\u716e<\u793e<\u7d17<\u8005<\u8b1d" +
                "<\u8eca<\u906e<\u86c7<\u90aa<\u501f<\u52fa<\u5c3a<\u6753" +
                "<\u707c<\u7235<\u914c<\u91c8<\u932b<\u82e5<\u5bc2<\u5f31" +
                "<\u60f9<\u4e3b<\u53d6<\u5b88<\u624b<\u6731<\u6b8a<\u72e9" +
                "<\u73e0<\u7a2e<\u816b<\u8da3<\u9152<\u9996<\u5112<\u53d7" +
                "<\u546a<\u5bff<\u6388<\u6a39<\u7dac<\u9700<\u56da<\u53ce" +
                "<\u5468<\u5b97<\u5c31<\u5dde<\u4fee<\u6101<\u62fe<\u6d32" +
                "<\u79c0<\u79cb<\u7d42<\u7e4d<\u7fd2<\u81ed<\u821f<\u8490" +
                "<\u8846<\u8972<\u8b90<\u8e74<\u8f2f<\u9031<\u914b<\u916c" +
                "<\u96c6<\u919c<\u4ec0<\u4f4f<\u5145<\u5341<\u5f93<\u620e" +
                "<\u67d4<\u6c41<\u6e0b<\u7363<\u7e26<\u91cd<\u9283<\u53d4" +
                "<\u5919<\u5bbf<\u6dd1<\u795d<\u7e2e<\u7c9b<\u587e<\u719f" +
                "<\u51fa<\u8853<\u8ff0<\u4fca<\u5cfb<\u6625<\u77ac<\u7ae3" +
                "<\u821c<\u99ff<\u51c6<\u5faa<\u65ec<\u696f<\u6b89<\u6df3" +
                "<\u6e96<\u6f64<\u76fe<\u7d14<\u5de1<\u9075<\u9187<\u9806" +
                "<\u51e6<\u521d<\u6240<\u6691<\u66d9<\u6e1a<\u5eb6<\u7dd2" +
                "<\u7f72<\u66f8<\u85af<\u85f7<\u8af8<\u52a9<\u53d9<\u5973" +
                "<\u5e8f<\u5f90<\u6055<\u92e4<\u9664<\u50b7<\u511f<\u52dd" +
                "<\u5320<\u5347<\u53ec<\u54e8<\u5546<\u5531<\u5617<\u5968" +
                "<\u59be<\u5a3c<\u5bb5<\u5c06<\u5c0f<\u5c11<\u5c1a<\u5e84" +
                "<\u5e8a<\u5ee0<\u5f70<\u627f<\u6284<\u62db<\u638c<\u6377" +
                "<\u6607<\u660c<\u662d<\u6676<\u677e<\u68a2<\u6a1f<\u6a35" +
                "<\u6cbc<\u6d88<\u6e09<\u6e58<\u713c<\u7126<\u7167<\u75c7" +
                "<\u7701<\u785d<\u7901<\u7965<\u79f0<\u7ae0<\u7b11<\u7ca7" +
                "<\u7d39<\u8096<\u83d6<\u848b<\u8549<\u885d<\u88f3<\u8a1f" +
                "<\u8a3c<\u8a54<\u8a73<\u8c61<\u8cde<\u91a4<\u9266<\u937e" +
                "<\u9418<\u969c<\u9798<\u4e0a<\u4e08<\u4e1e<\u4e57<\u5197" +
                "<\u5270<\u57ce<\u5834<\u58cc<\u5b22<\u5e38<\u60c5<\u64fe" +
                "<\u6761<\u6756<\u6d44<\u72b6<\u7573<\u7a63<\u84b8<\u8b72" +
                "<\u91b8<\u9320<\u5631<\u57f4<\u98fe<\u62ed<\u690d<\u6b96" +
                "<\u71ed<\u7e54<\u8077<\u8272<\u89e6<\u98df<\u8755<\u8fb1" +
                "<\u5c3b<\u4f38<\u4fe1<\u4fb5<\u5507<\u5a20<\u5bdd<\u5be9" +
                "<\u5fc3<\u614e<\u632f<\u65b0<\u664b<\u68ee<\u699b<\u6d78" +
                "<\u6df1<\u7533<\u75b9<\u771f<\u795e<\u79e6<\u7d33<\u81e3" +
                "<\u82af<\u85aa<\u89aa<\u8a3a<\u8eab<\u8f9b<\u9032<\u91dd" +
                "<\u9707<\u4eba<\u4ec1<\u5203<\u5875<\u58ec<\u5c0b<\u751a" +
                "<\u5c3d<\u814e<\u8a0a<\u8fc5<\u9663<\u976d<\u7b25<\u8acf" +
                "<\u9808<\u9162<\u56f3<\u53a8<\u9017<\u5439<\u5782<\u5e25" +
                "<\u63a8<\u6c34<\u708a<\u7761<\u7c8b<\u7fe0<\u8870<\u9042" +
                "<\u9154<\u9310<\u9318<\u968f<\u745e<\u9ac4<\u5d07<\u5d69" +
                "<\u6570<\u67a2<\u8da8<\u96db<\u636e<\u6749<\u6919<\u83c5" +
                "<\u9817<\u96c0<\u88fe<\u6f84<\u647a<\u5bf8<\u4e16<\u702c" +
                "<\u755d<\u662f<\u51c4<\u5236<\u52e2<\u59d3<\u5f81<\u6027" +
                "<\u6210<\u653f<\u6574<\u661f<\u6674<\u68f2<\u6816<\u6b63" +
                "<\u6e05<\u7272<\u751f<\u76db<\u7cbe<\u8056<\u58f0<\u88fd" +
                "<\u897f<\u8aa0<\u8a93<\u8acb<\u901d<\u9192<\u9752<\u9759" +
                "<\u6589<\u7a0e<\u8106<\u96bb<\u5e2d<\u60dc<\u621a<\u65a5" +
                "<\u6614<\u6790<\u77f3<\u7a4d<\u7c4d<\u7e3e<\u810a<\u8cac" +
                "<\u8d64<\u8de1<\u8e5f<\u78a9<\u5207<\u62d9<\u63a5<\u6442" +
                "<\u6298<\u8a2d<\u7a83<\u7bc0<\u8aac<\u96ea<\u7d76<\u820c" +
                "<\u8749<\u4ed9<\u5148<\u5343<\u5360<\u5ba3<\u5c02<\u5c16" +
                "<\u5ddd<\u6226<\u6247<\u64b0<\u6813<\u6834<\u6cc9<\u6d45" +
                "<\u6d17<\u67d3<\u6f5c<\u714e<\u717d<\u65cb<\u7a7f<\u7bad" +
                "<\u7dda<\u7e4a<\u7fa8<\u817a<\u821b<\u8239<\u85a6<\u8a6e" +
                "<\u8cce<\u8df5<\u9078<\u9077<\u92ad<\u9291<\u9583<\u9bae" +
                "<\u524d<\u5584<\u6f38<\u7136<\u5168<\u7985<\u7e55<\u81b3" +
                "<\u7cce<\u564c<\u5851<\u5ca8<\u63aa<\u66fe<\u66fd<\u695a" +
                "<\u72d9<\u758f<\u758e<\u790e<\u7956<\u79df<\u7c97<\u7d20" +
                "<\u7d44<\u8607<\u8a34<\u963b<\u9061<\u9f20<\u50e7<\u5275" +
                "<\u53cc<\u53e2<\u5009<\u55aa<\u58ee<\u594f<\u723d<\u5b8b" +
                "<\u5c64<\u531d<\u60e3<\u60f3<\u635c<\u6383<\u633f<\u63bb" +
                "<\u64cd<\u65e9<\u66f9<\u5de3<\u69cd<\u69fd<\u6f15<\u71e5" +
                "<\u4e89<\u75e9<\u76f8<\u7a93<\u7cdf<\u7dcf<\u7d9c<\u8061" +
                "<\u8349<\u8358<\u846c<\u84bc<\u85fb<\u88c5<\u8d70<\u9001" +
                "<\u906d<\u9397<\u971c<\u9a12<\u50cf<\u5897<\u618e<\u81d3" +
                "<\u8535<\u8d08<\u9020<\u4fc3<\u5074<\u5247<\u5373<\u606f" +
                "<\u6349<\u675f<\u6e2c<\u8db3<\u901f<\u4fd7<\u5c5e<\u8cca" +
                "<\u65cf<\u7d9a<\u5352<\u8896<\u5176<\u63c3<\u5b58<\u5b6b" +
                "<\u5c0a<\u640d<\u6751<\u905c<\u4ed6<\u591a<\u592a<\u6c70" +
                "<\u8a51<\u553e<\u5815<\u59a5<\u60f0<\u6253<\u67c1<\u8235" +
                "<\u6955<\u9640<\u99c4<\u9a28<\u4f53<\u5806<\u5bfe<\u8010" +
                "<\u5cb1<\u5e2f<\u5f85<\u6020<\u614b<\u6234<\u66ff<\u6cf0" +
                "<\u6ede<\u80ce<\u817f<\u82d4<\u888b<\u8cb8<\u9000<\u902e" +
                "<\u968a<\u9edb<\u9bdb<\u4ee3<\u53f0<\u5927<\u7b2c<\u918d" +
                "<\u984c<\u9df9<\u6edd<\u7027<\u5353<\u5544<\u5b85<\u6258" +
                "<\u629e<\u62d3<\u6ca2<\u6fef<\u7422<\u8a17<\u9438<\u6fc1" +
                "<\u8afe<\u8338<\u51e7<\u86f8<\u53ea<\u53e9<\u4f46<\u9054" +
                "<\u8fb0<\u596a<\u8131<\u5dfd<\u7aea<\u8fbf<\u68da<\u8c37" +
                "<\u72f8<\u9c48<\u6a3d<\u8ab0<\u4e39<\u5358<\u5606<\u5766" +
                "<\u62c5<\u63a2<\u65e6<\u6b4e<\u6de1<\u6e5b<\u70ad<\u77ed" +
                "<\u7aef<\u7baa<\u7dbb<\u803d<\u80c6<\u86cb<\u8a95<\u935b" +
                "<\u56e3<\u58c7<\u5f3e<\u65ad<\u6696<\u6a80<\u6bb5<\u7537" +
                "<\u8ac7<\u5024<\u77e5<\u5730<\u5f1b<\u6065<\u667a<\u6c60" +
                "<\u75f4<\u7a1a<\u7f6e<\u81f4<\u8718<\u9045<\u99b3<\u7bc9" +
                "<\u755c<\u7af9<\u7b51<\u84c4<\u9010<\u79e9<\u7a92<\u8336" +
                "<\u5ae1<\u7740<\u4e2d<\u4ef2<\u5b99<\u5fe0<\u62bd<\u663c" +
                "<\u67f1<\u6ce8<\u866b<\u8877<\u8a3b<\u914e<\u92f3<\u99d0" +
                "<\u6a17<\u7026<\u732a<\u82e7<\u8457<\u8caf<\u4e01<\u5146" +
                "<\u51cb<\u558b<\u5bf5<\u5e16<\u5e33<\u5e81<\u5f14<\u5f35" +
                "<\u5f6b<\u5fb4<\u61f2<\u6311<\u66a2<\u671d<\u6f6e<\u7252" +
                "<\u753a<\u773a<\u8074<\u8139<\u8178<\u8776<\u8abf<\u8adc" +
                "<\u8d85<\u8df3<\u929a<\u9577<\u9802<\u9ce5<\u52c5<\u6357" +
                "<\u76f4<\u6715<\u6c88<\u73cd<\u8cc3<\u93ae<\u9673<\u6d25" +
                "<\u589c<\u690e<\u69cc<\u8ffd<\u939a<\u75db<\u901a<\u585a" +
                "<\u6802<\u63b4<\u69fb<\u4f43<\u6f2c<\u67d8<\u8fbb<\u8526" +
                "<\u7db4<\u9354<\u693f<\u6f70<\u576a<\u58f7<\u5b2c<\u7d2c" +
                "<\u722a<\u540a<\u91e3<\u9db4<\u4ead<\u4f4e<\u505c<\u5075" +
                "<\u5243<\u8c9e<\u5448<\u5824<\u5b9a<\u5e1d<\u5e95<\u5ead" +
                "<\u5ef7<\u5f1f<\u608c<\u62b5<\u633a<\u63d0<\u68af<\u6c40" +
                "<\u7887<\u798e<\u7a0b<\u7de0<\u8247<\u8a02<\u8ae6<\u8e44" +
                "<\u9013<\u90b8<\u912d<\u91d8<\u9f0e<\u6ce5<\u6458<\u64e2" +
                "<\u6575<\u6ef4<\u7684<\u7b1b<\u9069<\u93d1<\u6eba<\u54f2" +
                "<\u5fb9<\u64a4<\u8f4d<\u8fed<\u9244<\u5178<\u586b<\u5929" +
                "<\u5c55<\u5e97<\u6dfb<\u7e8f<\u751c<\u8cbc<\u8ee2<\u985b" +
                "<\u70b9<\u4f1d<\u6bbf<\u6fb1<\u7530<\u96fb<\u514e<\u5410" +
                "<\u5835<\u5857<\u59ac<\u5c60<\u5f92<\u6597<\u675c<\u6e21" +
                "<\u767b<\u83df<\u8ced<\u9014<\u90fd<\u934d<\u7825<\u783a" +
                "<\u52aa<\u5ea6<\u571f<\u5974<\u6012<\u5012<\u515a<\u51ac" +
                "<\u51cd<\u5200<\u5510<\u5854<\u5858<\u5957<\u5b95<\u5cf6" +
                "<\u5d8b<\u60bc<\u6295<\u642d<\u6771<\u6843<\u68bc<\u68df" +
                "<\u76d7<\u6dd8<\u6e6f<\u6d9b<\u706f<\u71c8<\u5f53<\u75d8" +
                "<\u7977<\u7b49<\u7b54<\u7b52<\u7cd6<\u7d71<\u5230<\u8463" +
                "<\u8569<\u85e4<\u8a0e<\u8b04<\u8c46<\u8e0f<\u9003<\u900f" +
                "<\u9419<\u9676<\u982d<\u9a30<\u95d8<\u50cd<\u52d5<\u540c" +
                "<\u5802<\u5c0e<\u61a7<\u649e<\u6d1e<\u77b3<\u7ae5<\u80f4" +
                "<\u8404<\u9053<\u9285<\u5ce0<\u9d07<\u533f<\u5f97<\u5fb3" +
                "<\u6d9c<\u7279<\u7763<\u79bf<\u7be4<\u6bd2<\u72ec<\u8aad" +
                "<\u6803<\u6a61<\u51f8<\u7a81<\u6934<\u5c4a<\u9cf6<\u82eb" +
                "<\u5bc5<\u9149<\u701e<\u5678<\u5c6f<\u60c7<\u6566<\u6c8c" +
                "<\u8c5a<\u9041<\u9813<\u5451<\u66c7<\u920d<\u5948<\u90a3" +
                "<\u5185<\u4e4d<\u51ea<\u8599<\u8b0e<\u7058<\u637a<\u934b" +
                "<\u6962<\u99b4<\u7e04<\u7577<\u5357<\u6960<\u8edf<\u96e3" +
                "<\u6c5d<\u4e8c<\u5c3c<\u5f10<\u8fe9<\u5302<\u8cd1<\u8089" +
                "<\u8679<\u5eff<\u65e5<\u4e73<\u5165<\u5982<\u5c3f<\u97ee" +
                "<\u4efb<\u598a<\u5fcd<\u8a8d<\u6fe1<\u79b0<\u7962<\u5be7" +
                "<\u8471<\u732b<\u71b1<\u5e74<\u5ff5<\u637b<\u649a<\u71c3" +
                "<\u7c98<\u4e43<\u5efc<\u4e4b<\u57dc<\u56a2<\u60a9<\u6fc3" +
                "<\u7d0d<\u80fd<\u8133<\u81bf<\u8fb2<\u8997<\u86a4<\u5df4" +
                "<\u628a<\u64ad<\u8987<\u6777<\u6ce2<\u6d3e<\u7436<\u7834" +
                "<\u5a46<\u7f75<\u82ad<\u99ac<\u4ff3<\u5ec3<\u62dd<\u6392" +
                "<\u6557<\u676f<\u76c3<\u724c<\u80cc<\u80ba<\u8f29<\u914d" +
                "<\u500d<\u57f9<\u5a92<\u6885<\u6973<\u7164<\u72fd<\u8cb7" +
                "<\u58f2<\u8ce0<\u966a<\u9019<\u877f<\u79e4<\u77e7<\u8429" +
                "<\u4f2f<\u5265<\u535a<\u62cd<\u67cf<\u6cca<\u767d<\u7b94" +
                "<\u7c95<\u8236<\u8584<\u8feb<\u66dd<\u6f20<\u7206<\u7e1b" +
                "<\u83ab<\u99c1<\u9ea6<\u51fd<\u7bb1<\u7872<\u7bb8<\u8087" +
                "<\u7b48<\u6ae8<\u5e61<\u808c<\u7551<\u7560<\u516b<\u9262" +
                "<\u6e8c<\u767a<\u9197<\u9aea<\u4f10<\u7f70<\u629c<\u7b4f" +
                "<\u95a5<\u9ce9<\u567a<\u5859<\u86e4<\u96bc<\u4f34<\u5224" +
                "<\u534a<\u53cd<\u53db<\u5e06<\u642c<\u6591<\u677f<\u6c3e" +
                "<\u6c4e<\u7248<\u72af<\u73ed<\u7554<\u7e41<\u822c<\u85e9" +
                "<\u8ca9<\u7bc4<\u91c6<\u7169<\u9812<\u98ef<\u633d<\u6669" +
                "<\u756a<\u76e4<\u78d0<\u8543<\u86ee<\u532a<\u5351<\u5426" +
                "<\u5983<\u5e87<\u5f7c<\u60b2<\u6249<\u6279<\u62ab<\u6590" +
                "<\u6bd4<\u6ccc<\u75b2<\u76ae<\u7891<\u79d8<\u7dcb<\u7f77" +
                "<\u80a5<\u88ab<\u8ab9<\u8cbb<\u907f<\u975e<\u98db<\u6a0b" +
                "<\u7c38<\u5099<\u5c3e<\u5fae<\u6787<\u6bd8<\u7435<\u7709" +
                "<\u7f8e<\u9f3b<\u67ca<\u7a17<\u5339<\u758b<\u9aed<\u5f66" +
                "<\u819d<\u83f1<\u8098<\u5f3c<\u5fc5<\u7562<\u7b46<\u903c" +
                "<\u6867<\u59eb<\u5a9b<\u7d10<\u767e<\u8b2c<\u4ff5<\u5f6a" +
                "<\u6a19<\u6c37<\u6f02<\u74e2<\u7968<\u8868<\u8a55<\u8c79" +
                "<\u5edf<\u63cf<\u75c5<\u79d2<\u82d7<\u9328<\u92f2<\u849c" +
                "<\u86ed<\u9c2d<\u54c1<\u5f6c<\u658c<\u6d5c<\u7015<\u8ca7" +
                "<\u8cd3<\u983b<\u654f<\u74f6<\u4e0d<\u4ed8<\u57e0<\u592b" +
                "<\u5a66<\u5bcc<\u51a8<\u5e03<\u5e9c<\u6016<\u6276<\u6577" +
                "<\u65a7<\u666e<\u6d6e<\u7236<\u7b26<\u8150<\u819a<\u8299" +
                "<\u8b5c<\u8ca0<\u8ce6<\u8d74<\u961c<\u9644<\u4fae<\u64ab" +
                "<\u6b66<\u821e<\u8461<\u856a<\u90e8<\u5c01<\u6953<\u98a8" +
                "<\u847a<\u8557<\u4f0f<\u526f<\u5fa9<\u5e45<\u670d<\u798f" +
                "<\u8179<\u8907<\u8986<\u6df5<\u5f17<\u6255<\u6cb8<\u4ecf" +
                "<\u7269<\u9b92<\u5206<\u543b<\u5674<\u58b3<\u61a4<\u626e" +
                "<\u711a<\u596e<\u7c89<\u7cde<\u7d1b<\u96f0<\u6587<\u805e" +
                "<\u4e19<\u4f75<\u5175<\u5840<\u5e63<\u5e73<\u5f0a<\u67c4" +
                "<\u4e26<\u853d<\u9589<\u965b<\u7c73<\u9801<\u50fb<\u58c1" +
                "<\u7656<\u78a7<\u5225<\u77a5<\u8511<\u7b86<\u504f<\u5909" +
                "<\u7247<\u7bc7<\u7de8<\u8fba<\u8fd4<\u904d<\u4fbf<\u52c9" +
                "<\u5a29<\u5f01<\u97ad<\u4fdd<\u8217<\u92ea<\u5703<\u6355" +
                "<\u6b69<\u752b<\u88dc<\u8f14<\u7a42<\u52df<\u5893<\u6155" +
                "<\u620a<\u66ae<\u6bcd<\u7c3f<\u83e9<\u5023<\u4ff8<\u5305" +
                "<\u5446<\u5831<\u5949<\u5b9d<\u5cf0<\u5cef<\u5d29<\u5e96" +
                "<\u62b1<\u6367<\u653e<\u65b9<\u670b<\u6cd5<\u6ce1<\u70f9" +
                "<\u7832<\u7e2b<\u80de<\u82b3<\u840c<\u84ec<\u8702<\u8912" +
                "<\u8a2a<\u8c4a<\u90a6<\u92d2<\u98fd<\u9cf3<\u9d6c<\u4e4f" +
                "<\u4ea1<\u508d<\u5256<\u574a<\u59a8<\u5e3d<\u5fd8<\u5fd9" +
                "<\u623f<\u66b4<\u671b<\u67d0<\u68d2<\u5192<\u7d21<\u80aa" +
                "<\u81a8<\u8b00<\u8c8c<\u8cbf<\u927e<\u9632<\u5420<\u982c" +
                "<\u5317<\u50d5<\u535c<\u58a8<\u64b2<\u6734<\u7267<\u7766" +
                "<\u7a46<\u91e6<\u52c3<\u6ca1<\u6b86<\u5800<\u5e4c<\u5954" +
                "<\u672c<\u7ffb<\u51e1<\u76c6<\u6469<\u78e8<\u9b54<\u9ebb" +
                "<\u57cb<\u59b9<\u6627<\u679a<\u6bce<\u54e9<\u69d9<\u5e55" +
                "<\u819c<\u6795<\u9baa<\u67fe<\u9c52<\u685d<\u4ea6<\u4fe3" +
                "<\u53c8<\u62b9<\u672b<\u6cab<\u8fc4<\u4fad<\u7e6d<\u9ebf" +
                "<\u4e07<\u6162<\u6e80<\u6f2b<\u8513<\u5473<\u672a<\u9b45" +
                "<\u5df3<\u7b95<\u5cac<\u5bc6<\u871c<\u6e4a<\u84d1<\u7a14" +
                "<\u8108<\u5999<\u7c8d<\u6c11<\u7720<\u52d9<\u5922<\u7121" +
                "<\u725f<\u77db<\u9727<\u9d61<\u690b<\u5a7f<\u5a18<\u51a5" +
                "<\u540d<\u547d<\u660e<\u76df<\u8ff7<\u9298<\u9cf4<\u59ea" +
                "<\u725d<\u6ec5<\u514d<\u68c9<\u7dbf<\u7dec<\u9762<\u9eba" +
                "<\u6478<\u6a21<\u8302<\u5984<\u5b5f<\u6bdb<\u731b<\u76f2" +
                "<\u7db2<\u8017<\u8499<\u5132<\u6728<\u9ed9<\u76ee<\u6762" +
                "<\u52ff<\u9905<\u5c24<\u623b<\u7c7e<\u8cb0<\u554f<\u60b6" +
                "<\u7d0b<\u9580<\u5301<\u4e5f<\u51b6<\u591c<\u723a<\u8036" +
                "<\u91ce<\u5f25<\u77e2<\u5384<\u5f79<\u7d04<\u85ac<\u8a33" +
                "<\u8e8d<\u9756<\u67f3<\u85ae<\u9453<\u6109<\u6108<\u6cb9" +
                "<\u7652<\u8aed<\u8f38<\u552f<\u4f51<\u512a<\u52c7<\u53cb" +
                "<\u5ba5<\u5e7d<\u60a0<\u6182<\u63d6<\u6709<\u67da<\u6e67" +
                "<\u6d8c<\u7336<\u7337<\u7531<\u7950<\u88d5<\u8a98<\u904a" +
                "<\u9091<\u90f5<\u96c4<\u878d<\u5915<\u4e88<\u4f59<\u4e0e" +
                "<\u8a89<\u8f3f<\u9810<\u50ad<\u5e7c<\u5996<\u5bb9<\u5eb8" +
                "<\u63da<\u63fa<\u64c1<\u66dc<\u694a<\u69d8<\u6d0b<\u6eb6" +
                "<\u7194<\u7528<\u7aaf<\u7f8a<\u8000<\u8449<\u84c9<\u8981" +
                "<\u8b21<\u8e0a<\u9065<\u967d<\u990a<\u617e<\u6291<\u6b32" +
                "<\u6c83<\u6d74<\u7fcc<\u7ffc<\u6dc0<\u7f85<\u87ba<\u88f8" +
                "<\u6765<\u83b1<\u983c<\u96f7<\u6d1b<\u7d61<\u843d<\u916a" +
                "<\u4e71<\u5375<\u5d50<\u6b04<\u6feb<\u85cd<\u862d<\u89a7" +
                "<\u5229<\u540f<\u5c65<\u674e<\u68a8<\u7406<\u7483<\u75e2" +
                "<\u88cf<\u88e1<\u91cc<\u96e2<\u9678<\u5f8b<\u7387<\u7acb" +
                "<\u844e<\u63a0<\u7565<\u5289<\u6d41<\u6e9c<\u7409<\u7559" +
                "<\u786b<\u7c92<\u9686<\u7adc<\u9f8d<\u4fb6<\u616e<\u65c5" +
                "<\u865c<\u4e86<\u4eae<\u50da<\u4e21<\u51cc<\u5bee<\u6599" +
                "<\u6881<\u6dbc<\u731f<\u7642<\u77ad<\u7a1c<\u7ce7<\u826f" +
                "<\u8ad2<\u907c<\u91cf<\u9675<\u9818<\u529b<\u7dd1<\u502b" +
                "<\u5398<\u6797<\u6dcb<\u71d0<\u7433<\u81e8<\u8f2a<\u96a3" +
                "<\u9c57<\u9e9f<\u7460<\u5841<\u6d99<\u7d2f<\u985e<\u4ee4" +
                "<\u4f36<\u4f8b<\u51b7<\u52b1<\u5dba<\u601c<\u73b2<\u793c" +
                "<\u82d3<\u9234<\u96b7<\u96f6<\u970a<\u9e97<\u9f62<\u66a6" +
                "<\u6b74<\u5217<\u52a3<\u70c8<\u88c2<\u5ec9<\u604b<\u6190" +
                "<\u6f23<\u7149<\u7c3e<\u7df4<\u806f<\u84ee<\u9023<\u932c" +
                "<\u5442<\u9b6f<\u6ad3<\u7089<\u8cc2<\u8def<\u9732<\u52b4" +
                "<\u5a41<\u5eca<\u5f04<\u6717<\u697c<\u6994<\u6d6a<\u6f0f" +
                "<\u7262<\u72fc<\u7bed<\u8001<\u807e<\u874b<\u90ce<\u516d" +
                "<\u9e93<\u7984<\u808b<\u9332<\u8ad6<\u502d<\u548c<\u8a71" +
                "<\u6b6a<\u8cc4<\u8107<\u60d1<\u67a0<\u9df2<\u4e99<\u4e98" +
                "<\u9c10<\u8a6b<\u85c1<\u8568<\u6900<\u6e7e<\u7897<\u8155" +
                "<\u5f0c<\u4e10<\u4e15<\u4e2a<\u4e31<\u4e36<\u4e3c<\u4e3f" +
                "<\u4e42<\u4e56<\u4e58<\u4e82<\u4e85<\u8c6b<\u4e8a<\u8212" +
                "<\u5f0d<\u4e8e<\u4e9e<\u4e9f<\u4ea0<\u4ea2<\u4eb0<\u4eb3" +
                "<\u4eb6<\u4ece<\u4ecd<\u4ec4<\u4ec6<\u4ec2<\u4ed7<\u4ede" +
                "<\u4eed<\u4edf<\u4ef7<\u4f09<\u4f5a<\u4f30<\u4f5b<\u4f5d" +
                "<\u4f57<\u4f47<\u4f76<\u4f88<\u4f8f<\u4f98<\u4f7b<\u4f69" +
                "<\u4f70<\u4f91<\u4f6f<\u4f86<\u4f96<\u5118<\u4fd4<\u4fdf" +
                "<\u4fce<\u4fd8<\u4fdb<\u4fd1<\u4fda<\u4fd0<\u4fe4<\u4fe5" +
                "<\u501a<\u5028<\u5014<\u502a<\u5025<\u5005<\u4f1c<\u4ff6" +
                "<\u5021<\u5029<\u502c<\u4ffe<\u4fef<\u5011<\u5006<\u5043" +
                "<\u5047<\u6703<\u5055<\u5050<\u5048<\u505a<\u5056<\u506c" +
                "<\u5078<\u5080<\u509a<\u5085<\u50b4<\u50b2<\u50c9<\u50ca" +
                "<\u50b3<\u50c2<\u50d6<\u50de<\u50e5<\u50ed<\u50e3<\u50ee" +
                "<\u50f9<\u50f5<\u5109<\u5101<\u5102<\u5116<\u5115<\u5114" +
                "<\u511a<\u5121<\u513a<\u5137<\u513c<\u513b<\u513f<\u5140" +
                "<\u5152<\u514c<\u5154<\u5162<\u7af8<\u5169<\u516a<\u516e" +
                "<\u5180<\u5182<\u56d8<\u518c<\u5189<\u518f<\u5191<\u5193" +
                "<\u5195<\u5196<\u51a4<\u51a6<\u51a2<\u51a9<\u51aa<\u51ab" +
                "<\u51b3<\u51b1<\u51b2<\u51b0<\u51b5<\u51bd<\u51c5<\u51c9" +
                "<\u51db<\u51e0<\u8655<\u51e9<\u51ed<\u51f0<\u51f5<\u51fe" +
                "<\u5204<\u520b<\u5214<\u520e<\u5227<\u522a<\u522e<\u5233" +
                "<\u5239<\u524f<\u5244<\u524b<\u524c<\u525e<\u5254<\u526a" +
                "<\u5274<\u5269<\u5273<\u527f<\u527d<\u528d<\u5294<\u5292" +
                "<\u5271<\u5288<\u5291<\u8fa8<\u8fa7<\u52ac<\u52ad<\u52bc" +
                "<\u52b5<\u52c1<\u52cd<\u52d7<\u52de<\u52e3<\u52e6<\u98ed" +
                "<\u52e0<\u52f3<\u52f5<\u52f8<\u52f9<\u5306<\u5308<\u7538" +
                "<\u530d<\u5310<\u530f<\u5315<\u531a<\u5323<\u532f<\u5331" +
                "<\u5333<\u5338<\u5340<\u5346<\u5345<\u4e17<\u5349<\u534d" +
                "<\u51d6<\u535e<\u5369<\u536e<\u5918<\u537b<\u5377<\u5382" +
                "<\u5396<\u53a0<\u53a6<\u53a5<\u53ae<\u53b0<\u53b6<\u53c3" +
                "<\u7c12<\u96d9<\u53df<\u66fc<\u71ee<\u53ee<\u53e8<\u53ed" +
                "<\u53fa<\u5401<\u543d<\u5440<\u542c<\u542d<\u543c<\u542e" +
                "<\u5436<\u5429<\u541d<\u544e<\u548f<\u5475<\u548e<\u545f" +
                "<\u5471<\u5477<\u5470<\u5492<\u547b<\u5480<\u5476<\u5484" +
                "<\u5490<\u5486<\u54c7<\u54a2<\u54b8<\u54a5<\u54ac<\u54c4" +
                "<\u54c8<\u54a8<\u54ab<\u54c2<\u54a4<\u54be<\u54bc<\u54d8" +
                "<\u54e5<\u54e6<\u550f<\u5514<\u54fd<\u54ee<\u54ed<\u54fa" +
                "<\u54e2<\u5539<\u5540<\u5563<\u554c<\u552e<\u555c<\u5545" +
                "<\u5556<\u5557<\u5538<\u5533<\u555d<\u5599<\u5580<\u54af" +
                "<\u558a<\u559f<\u557b<\u557e<\u5598<\u559e<\u55ae<\u557c" +
                "<\u5583<\u55a9<\u5587<\u55a8<\u55da<\u55c5<\u55df<\u55c4" +
                "<\u55dc<\u55e4<\u55d4<\u5614<\u55f7<\u5616<\u55fe<\u55fd" +
                "<\u561b<\u55f9<\u564e<\u5650<\u71df<\u5634<\u5636<\u5632" +
                "<\u5638<\u566b<\u5664<\u562f<\u566c<\u566a<\u5686<\u5680" +
                "<\u568a<\u56a0<\u5694<\u568f<\u56a5<\u56ae<\u56b6<\u56b4" +
                "<\u56c2<\u56bc<\u56c1<\u56c3<\u56c0<\u56c8<\u56ce<\u56d1" +
                "<\u56d3<\u56d7<\u56ee<\u56f9<\u5700<\u56ff<\u5704<\u5709" +
                "<\u5708<\u570b<\u570d<\u5713<\u5718<\u5716<\u55c7<\u571c" +
                "<\u5726<\u5737<\u5738<\u574e<\u573b<\u5740<\u574f<\u5769" +
                "<\u57c0<\u5788<\u5761<\u577f<\u5789<\u5793<\u57a0<\u57b3" +
                "<\u57a4<\u57aa<\u57b0<\u57c3<\u57c6<\u57d4<\u57d2<\u57d3" +
                "<\u580a<\u57d6<\u57e3<\u580b<\u5819<\u581d<\u5872<\u5821" +
                "<\u5862<\u584b<\u5870<\u6bc0<\u5852<\u583d<\u5879<\u5885" +
                "<\u58b9<\u589f<\u58ab<\u58ba<\u58de<\u58bb<\u58b8<\u58ae" +
                "<\u58c5<\u58d3<\u58d1<\u58d7<\u58d9<\u58d8<\u58e5<\u58dc" +
                "<\u58e4<\u58df<\u58ef<\u58fa<\u58f9<\u58fb<\u58fc<\u58fd" +
                "<\u5902<\u590a<\u5910<\u591b<\u68a6<\u5925<\u592c<\u592d" +
                "<\u5932<\u5938<\u593e<\u7ad2<\u5955<\u5950<\u594e<\u595a" +
                "<\u5958<\u5962<\u5960<\u5967<\u596c<\u5969<\u5978<\u5981" +
                "<\u599d<\u4f5e<\u4fab<\u59a3<\u59b2<\u59c6<\u59e8<\u59dc" +
                "<\u598d<\u59d9<\u59da<\u5a25<\u5a1f<\u5a11<\u5a1c<\u5a09" +
                "<\u5a1a<\u5a40<\u5a6c<\u5a49<\u5a35<\u5a36<\u5a62<\u5a6a" +
                "<\u5a9a<\u5abc<\u5abe<\u5acb<\u5ac2<\u5abd<\u5ae3<\u5ad7" +
                "<\u5ae6<\u5ae9<\u5ad6<\u5afa<\u5afb<\u5b0c<\u5b0b<\u5b16" +
                "<\u5b32<\u5ad0<\u5b2a<\u5b36<\u5b3e<\u5b43<\u5b45<\u5b40" +
                "<\u5b51<\u5b55<\u5b5a<\u5b5b<\u5b65<\u5b69<\u5b70<\u5b73" +
                "<\u5b75<\u5b78<\u6588<\u5b7a<\u5b80<\u5b83<\u5ba6<\u5bb8" +
                "<\u5bc3<\u5bc7<\u5bc9<\u5bd4<\u5bd0<\u5be4<\u5be6<\u5be2" +
                "<\u5bde<\u5be5<\u5beb<\u5bf0<\u5bf6<\u5bf3<\u5c05<\u5c07" +
                "<\u5c08<\u5c0d<\u5c13<\u5c20<\u5c22<\u5c28<\u5c38<\u5c39" +
                "<\u5c41<\u5c46<\u5c4e<\u5c53<\u5c50<\u5c4f<\u5b71<\u5c6c" +
                "<\u5c6e<\u4e62<\u5c76<\u5c79<\u5c8c<\u5c91<\u5c94<\u599b" +
                "<\u5cab<\u5cbb<\u5cb6<\u5cbc<\u5cb7<\u5cc5<\u5cbe<\u5cc7" +
                "<\u5cd9<\u5ce9<\u5cfd<\u5cfa<\u5ced<\u5d8c<\u5cea<\u5d0b" +
                "<\u5d15<\u5d17<\u5d5c<\u5d1f<\u5d1b<\u5d11<\u5d14<\u5d22" +
                "<\u5d1a<\u5d19<\u5d18<\u5d4c<\u5d52<\u5d4e<\u5d4b<\u5d6c" +
                "<\u5d73<\u5d76<\u5d87<\u5d84<\u5d82<\u5da2<\u5d9d<\u5dac" +
                "<\u5dae<\u5dbd<\u5d90<\u5db7<\u5dbc<\u5dc9<\u5dcd<\u5dd3" +
                "<\u5dd2<\u5dd6<\u5ddb<\u5deb<\u5df2<\u5df5<\u5e0b<\u5e1a" +
                "<\u5e19<\u5e11<\u5e1b<\u5e36<\u5e37<\u5e44<\u5e43<\u5e40" +
                "<\u5e4e<\u5e57<\u5e54<\u5e5f<\u5e62<\u5e64<\u5e47<\u5e75" +
                "<\u5e76<\u5e7a<\u9ebc<\u5e7f<\u5ea0<\u5ec1<\u5ec2<\u5ec8" +
                "<\u5ed0<\u5ecf<\u5ed6<\u5ee3<\u5edd<\u5eda<\u5edb<\u5ee2" +
                "<\u5ee1<\u5ee8<\u5ee9<\u5eec<\u5ef1<\u5ef3<\u5ef0<\u5ef4" +
                "<\u5ef8<\u5efe<\u5f03<\u5f09<\u5f5d<\u5f5c<\u5f0b<\u5f11" +
                "<\u5f16<\u5f29<\u5f2d<\u5f38<\u5f41<\u5f48<\u5f4c<\u5f4e" +
                "<\u5f2f<\u5f51<\u5f56<\u5f57<\u5f59<\u5f61<\u5f6d<\u5f73" +
                "<\u5f77<\u5f83<\u5f82<\u5f7f<\u5f8a<\u5f88<\u5f91<\u5f87" +
                "<\u5f9e<\u5f99<\u5f98<\u5fa0<\u5fa8<\u5fad<\u5fbc<\u5fd6" +
                "<\u5ffb<\u5fe4<\u5ff8<\u5ff1<\u5fdd<\u60b3<\u5fff<\u6021" +
                "<\u6060<\u6019<\u6010<\u6029<\u600e<\u6031<\u601b<\u6015" +
                "<\u602b<\u6026<\u600f<\u603a<\u605a<\u6041<\u606a<\u6077" +
                "<\u605f<\u604a<\u6046<\u604d<\u6063<\u6043<\u6064<\u6042" +
                "<\u606c<\u606b<\u6059<\u6081<\u608d<\u60e7<\u6083<\u609a" +
                "<\u6084<\u609b<\u6096<\u6097<\u6092<\u60a7<\u608b<\u60e1" +
                "<\u60b8<\u60e0<\u60d3<\u60b4<\u5ff0<\u60bd<\u60c6<\u60b5" +
                "<\u60d8<\u614d<\u6115<\u6106<\u60f6<\u60f7<\u6100<\u60f4" +
                "<\u60fa<\u6103<\u6121<\u60fb<\u60f1<\u610d<\u610e<\u6147" +
                "<\u613e<\u6128<\u6127<\u614a<\u613f<\u613c<\u612c<\u6134" +
                "<\u613d<\u6142<\u6144<\u6173<\u6177<\u6158<\u6159<\u615a" +
                "<\u616b<\u6174<\u616f<\u6165<\u6171<\u615f<\u615d<\u6153" +
                "<\u6175<\u6199<\u6196<\u6187<\u61ac<\u6194<\u619a<\u618a" +
                "<\u6191<\u61ab<\u61ae<\u61cc<\u61ca<\u61c9<\u61f7<\u61c8" +
                "<\u61c3<\u61c6<\u61ba<\u61cb<\u7f79<\u61cd<\u61e6<\u61e3" +
                "<\u61f6<\u61fa<\u61f4<\u61ff<\u61fd<\u61fc<\u61fe<\u6200" +
                "<\u6208<\u6209<\u620d<\u620c<\u6214<\u621b<\u621e<\u6221" +
                "<\u622a<\u622e<\u6230<\u6232<\u6233<\u6241<\u624e<\u625e" +
                "<\u6263<\u625b<\u6260<\u6268<\u627c<\u6282<\u6289<\u627e" +
                "<\u6292<\u6293<\u6296<\u62d4<\u6283<\u6294<\u62d7<\u62d1" +
                "<\u62bb<\u62cf<\u62ff<\u62c6<\u64d4<\u62c8<\u62dc<\u62cc" +
                "<\u62ca<\u62c2<\u62c7<\u629b<\u62c9<\u630c<\u62ee<\u62f1" +
                "<\u6327<\u6302<\u6308<\u62ef<\u62f5<\u6350<\u633e<\u634d" +
                "<\u641c<\u634f<\u6396<\u638e<\u6380<\u63ab<\u6376<\u63a3" +
                "<\u638f<\u6389<\u639f<\u63b5<\u636b<\u6369<\u63be<\u63e9" +
                "<\u63c0<\u63c6<\u63e3<\u63c9<\u63d2<\u63f6<\u63c4<\u6416" +
                "<\u6434<\u6406<\u6413<\u6426<\u6436<\u651d<\u6417<\u6428" +
                "<\u640f<\u6467<\u646f<\u6476<\u644e<\u652a<\u6495<\u6493" +
                "<\u64a5<\u64a9<\u6488<\u64bc<\u64da<\u64d2<\u64c5<\u64c7" +
                "<\u64bb<\u64d8<\u64c2<\u64f1<\u64e7<\u8209<\u64e0<\u64e1" +
                "<\u62ac<\u64e3<\u64ef<\u652c<\u64f6<\u64f4<\u64f2<\u64fa" +
                "<\u6500<\u64fd<\u6518<\u651c<\u6505<\u6524<\u6523<\u652b" +
                "<\u6534<\u6535<\u6537<\u6536<\u6538<\u754b<\u6548<\u6556" +
                "<\u6555<\u654d<\u6558<\u655e<\u655d<\u6572<\u6578<\u6582" +
                "<\u6583<\u8b8a<\u659b<\u659f<\u65ab<\u65b7<\u65c3<\u65c6" +
                "<\u65c1<\u65c4<\u65cc<\u65d2<\u65db<\u65d9<\u65e0<\u65e1" +
                "<\u65f1<\u6772<\u660a<\u6603<\u65fb<\u6773<\u6635<\u6636" +
                "<\u6634<\u661c<\u664f<\u6644<\u6649<\u6641<\u665e<\u665d" +
                "<\u6664<\u6667<\u6668<\u665f<\u6662<\u6670<\u6683<\u6688" +
                "<\u668e<\u6689<\u6684<\u6698<\u669d<\u66c1<\u66b9<\u66c9" +
                "<\u66be<\u66bc<\u66c4<\u66b8<\u66d6<\u66da<\u66e0<\u663f" +
                "<\u66e6<\u66e9<\u66f0<\u66f5<\u66f7<\u670f<\u6716<\u671e" +
                "<\u6726<\u6727<\u9738<\u672e<\u673f<\u6736<\u6741<\u6738" +
                "<\u6737<\u6746<\u675e<\u6760<\u6759<\u6763<\u6764<\u6789" +
                "<\u6770<\u67a9<\u677c<\u676a<\u678c<\u678b<\u67a6<\u67a1" +
                "<\u6785<\u67b7<\u67ef<\u67b4<\u67ec<\u67b3<\u67e9<\u67b8" +
                "<\u67e4<\u67de<\u67dd<\u67e2<\u67ee<\u67b9<\u67ce<\u67c6" +
                "<\u67e7<\u6a9c<\u681e<\u6846<\u6829<\u6840<\u684d<\u6832" +
                "<\u684e<\u68b3<\u682b<\u6859<\u6863<\u6877<\u687f<\u689f" +
                "<\u688f<\u68ad<\u6894<\u689d<\u689b<\u6883<\u6aae<\u68b9" +
                "<\u6874<\u68b5<\u68a0<\u68ba<\u690f<\u688d<\u687e<\u6901" +
                "<\u68ca<\u6908<\u68d8<\u6922<\u6926<\u68e1<\u690c<\u68cd" +
                "<\u68d4<\u68e7<\u68d5<\u6936<\u6912<\u6904<\u68d7<\u68e3" +
                "<\u6925<\u68f9<\u68e0<\u68ef<\u6928<\u692a<\u691a<\u6923" +
                "<\u6921<\u68c6<\u6979<\u6977<\u695c<\u6978<\u696b<\u6954" +
                "<\u697e<\u696e<\u6939<\u6974<\u693d<\u6959<\u6930<\u6961" +
                "<\u695e<\u695d<\u6981<\u696a<\u69b2<\u69ae<\u69d0<\u69bf" +
                "<\u69c1<\u69d3<\u69be<\u69ce<\u5be8<\u69ca<\u69dd<\u69bb" +
                "<\u69c3<\u69a7<\u6a2e<\u6991<\u69a0<\u699c<\u6995<\u69b4" +
                "<\u69de<\u69e8<\u6a02<\u6a1b<\u69ff<\u6b0a<\u69f9<\u69f2" +
                "<\u69e7<\u6a05<\u69b1<\u6a1e<\u69ed<\u6a14<\u69eb<\u6a0a" +
                "<\u6a12<\u6ac1<\u6a23<\u6a13<\u6a44<\u6a0c<\u6a72<\u6a36" +
                "<\u6a78<\u6a47<\u6a62<\u6a59<\u6a66<\u6a48<\u6a38<\u6a22" +
                "<\u6a90<\u6a8d<\u6aa0<\u6a84<\u6aa2<\u6aa3<\u6a97<\u8617" +
                "<\u6abb<\u6ac3<\u6ac2<\u6ab8<\u6ab3<\u6aac<\u6ade<\u6ad1" +
                "<\u6adf<\u6aaa<\u6ada<\u6aea<\u6afb<\u6b05<\u8616<\u6afa" +
                "<\u6b12<\u6b16<\u9b31<\u6b1f<\u6b38<\u6b37<\u76dc<\u6b39" +
                "<\u98ee<\u6b47<\u6b43<\u6b49<\u6b50<\u6b59<\u6b54<\u6b5b" +
                "<\u6b5f<\u6b61<\u6b78<\u6b79<\u6b7f<\u6b80<\u6b84<\u6b83" +
                "<\u6b8d<\u6b98<\u6b95<\u6b9e<\u6ba4<\u6baa<\u6bab<\u6baf" +
                "<\u6bb2<\u6bb1<\u6bb3<\u6bb7<\u6bbc<\u6bc6<\u6bcb<\u6bd3" +
                "<\u6bdf<\u6bec<\u6beb<\u6bf3<\u6bef<\u9ebe<\u6c08<\u6c13" +
                "<\u6c14<\u6c1b<\u6c24<\u6c23<\u6c5e<\u6c55<\u6c62<\u6c6a" +
                "<\u6c82<\u6c8d<\u6c9a<\u6c81<\u6c9b<\u6c7e<\u6c68<\u6c73" +
                "<\u6c92<\u6c90<\u6cc4<\u6cf1<\u6cd3<\u6cbd<\u6cd7<\u6cc5" +
                "<\u6cdd<\u6cae<\u6cb1<\u6cbe<\u6cba<\u6cdb<\u6cef<\u6cd9" +
                "<\u6cea<\u6d1f<\u884d<\u6d36<\u6d2b<\u6d3d<\u6d38<\u6d19" +
                "<\u6d35<\u6d33<\u6d12<\u6d0c<\u6d63<\u6d93<\u6d64<\u6d5a" +
                "<\u6d79<\u6d59<\u6d8e<\u6d95<\u6fe4<\u6d85<\u6df9<\u6e15" +
                "<\u6e0a<\u6db5<\u6dc7<\u6de6<\u6db8<\u6dc6<\u6dec<\u6dde" +
                "<\u6dcc<\u6de8<\u6dd2<\u6dc5<\u6dfa<\u6dd9<\u6de4<\u6dd5" +
                "<\u6dea<\u6dee<\u6e2d<\u6e6e<\u6e2e<\u6e19<\u6e72<\u6e5f" +
                "<\u6e3e<\u6e23<\u6e6b<\u6e2b<\u6e76<\u6e4d<\u6e1f<\u6e43" +
                "<\u6e3a<\u6e4e<\u6e24<\u6eff<\u6e1d<\u6e38<\u6e82<\u6eaa" +
                "<\u6e98<\u6ec9<\u6eb7<\u6ed3<\u6ebd<\u6eaf<\u6ec4<\u6eb2" +
                "<\u6ed4<\u6ed5<\u6e8f<\u6ea5<\u6ec2<\u6e9f<\u6f41<\u6f11" +
                "<\u704c<\u6eec<\u6ef8<\u6efe<\u6f3f<\u6ef2<\u6f31<\u6eef" +
                "<\u6f32<\u6ecc<\u6f3e<\u6f13<\u6ef7<\u6f86<\u6f7a<\u6f78" +
                "<\u6f81<\u6f80<\u6f6f<\u6f5b<\u6ff3<\u6f6d<\u6f82<\u6f7c" +
                "<\u6f58<\u6f8e<\u6f91<\u6fc2<\u6f66<\u6fb3<\u6fa3<\u6fa1" +
                "<\u6fa4<\u6fb9<\u6fc6<\u6faa<\u6fdf<\u6fd5<\u6fec<\u6fd4" +
                "<\u6fd8<\u6ff1<\u6fee<\u6fdb<\u7009<\u700b<\u6ffa<\u7011" +
                "<\u7001<\u700f<\u6ffe<\u701b<\u701a<\u6f74<\u701d<\u7018" +
                "<\u701f<\u7030<\u703e<\u7032<\u7051<\u7063<\u7099<\u7092" +
                "<\u70af<\u70f1<\u70ac<\u70b8<\u70b3<\u70ae<\u70df<\u70cb" +
                "<\u70dd<\u70d9<\u7109<\u70fd<\u711c<\u7119<\u7165<\u7155" +
                "<\u7188<\u7166<\u7162<\u714c<\u7156<\u716c<\u718f<\u71fb" +
                "<\u7184<\u7195<\u71a8<\u71ac<\u71d7<\u71b9<\u71be<\u71d2" +
                "<\u71c9<\u71d4<\u71ce<\u71e0<\u71ec<\u71e7<\u71f5<\u71fc" +
                "<\u71f9<\u71ff<\u720d<\u7210<\u721b<\u7228<\u722d<\u722c" +
                "<\u7230<\u7232<\u723b<\u723c<\u723f<\u7240<\u7246<\u724b" +
                "<\u7258<\u7274<\u727e<\u7282<\u7281<\u7287<\u7292<\u7296" +
                "<\u72a2<\u72a7<\u72b9<\u72b2<\u72c3<\u72c6<\u72c4<\u72ce" +
                "<\u72d2<\u72e2<\u72e0<\u72e1<\u72f9<\u72f7<\u500f<\u7317" +
                "<\u730a<\u731c<\u7316<\u731d<\u7334<\u732f<\u7329<\u7325" +
                "<\u733e<\u734e<\u734f<\u9ed8<\u7357<\u736a<\u7368<\u7370" +
                "<\u7378<\u7375<\u737b<\u737a<\u73c8<\u73b3<\u73ce<\u73bb" +
                "<\u73c0<\u73e5<\u73ee<\u73de<\u74a2<\u7405<\u746f<\u7425" +
                "<\u73f8<\u7432<\u743a<\u7455<\u743f<\u745f<\u7459<\u7441" +
                "<\u745c<\u7469<\u7470<\u7463<\u746a<\u7476<\u747e<\u748b" +
                "<\u749e<\u74a7<\u74ca<\u74cf<\u74d4<\u73f1<\u74e0<\u74e3" +
                "<\u74e7<\u74e9<\u74ee<\u74f2<\u74f0<\u74f1<\u74f8<\u74f7" +
                "<\u7504<\u7503<\u7505<\u750c<\u750e<\u750d<\u7515<\u7513" +
                "<\u751e<\u7526<\u752c<\u753c<\u7544<\u754d<\u754a<\u7549" +
                "<\u755b<\u7546<\u755a<\u7569<\u7564<\u7567<\u756b<\u756d" +
                "<\u7578<\u7576<\u7586<\u7587<\u7574<\u758a<\u7589<\u7582" +
                "<\u7594<\u759a<\u759d<\u75a5<\u75a3<\u75c2<\u75b3<\u75c3" +
                "<\u75b5<\u75bd<\u75b8<\u75bc<\u75b1<\u75cd<\u75ca<\u75d2" +
                "<\u75d9<\u75e3<\u75de<\u75fe<\u75ff<\u75fc<\u7601<\u75f0" +
                "<\u75fa<\u75f2<\u75f3<\u760b<\u760d<\u7609<\u761f<\u7627" +
                "<\u7620<\u7621<\u7622<\u7624<\u7634<\u7630<\u763b<\u7647" +
                "<\u7648<\u7646<\u765c<\u7658<\u7661<\u7662<\u7668<\u7669" +
                "<\u766a<\u7667<\u766c<\u7670<\u7672<\u7676<\u7678<\u767c" +
                "<\u7680<\u7683<\u7688<\u768b<\u768e<\u7696<\u7693<\u7699" +
                "<\u769a<\u76b0<\u76b4<\u76b8<\u76b9<\u76ba<\u76c2<\u76cd" +
                "<\u76d6<\u76d2<\u76de<\u76e1<\u76e5<\u76e7<\u76ea<\u862f" +
                "<\u76fb<\u7708<\u7707<\u7704<\u7729<\u7724<\u771e<\u7725" +
                "<\u7726<\u771b<\u7737<\u7738<\u7747<\u775a<\u7768<\u776b" +
                "<\u775b<\u7765<\u777f<\u777e<\u7779<\u778e<\u778b<\u7791" +
                "<\u77a0<\u779e<\u77b0<\u77b6<\u77b9<\u77bf<\u77bc<\u77bd" +
                "<\u77bb<\u77c7<\u77cd<\u77d7<\u77da<\u77dc<\u77e3<\u77ee" +
                "<\u77fc<\u780c<\u7812<\u7926<\u7820<\u792a<\u7845<\u788e" +
                "<\u7874<\u7886<\u787c<\u789a<\u788c<\u78a3<\u78b5<\u78aa" +
                "<\u78af<\u78d1<\u78c6<\u78cb<\u78d4<\u78be<\u78bc<\u78c5" +
                "<\u78ca<\u78ec<\u78e7<\u78da<\u78fd<\u78f4<\u7907<\u7912" +
                "<\u7911<\u7919<\u792c<\u792b<\u7940<\u7960<\u7957<\u795f" +
                "<\u795a<\u7955<\u7953<\u797a<\u797f<\u798a<\u799d<\u79a7" +
                "<\u9f4b<\u79aa<\u79ae<\u79b3<\u79b9<\u79ba<\u79c9<\u79d5" +
                "<\u79e7<\u79ec<\u79e1<\u79e3<\u7a08<\u7a0d<\u7a18<\u7a19" +
                "<\u7a20<\u7a1f<\u7980<\u7a31<\u7a3b<\u7a3e<\u7a37<\u7a43" +
                "<\u7a57<\u7a49<\u7a61<\u7a62<\u7a69<\u9f9d<\u7a70<\u7a79" +
                "<\u7a7d<\u7a88<\u7a97<\u7a95<\u7a98<\u7a96<\u7aa9<\u7ac8" +
                "<\u7ab0<\u7ab6<\u7ac5<\u7ac4<\u7abf<\u9083<\u7ac7<\u7aca" +
                "<\u7acd<\u7acf<\u7ad5<\u7ad3<\u7ad9<\u7ada<\u7add<\u7ae1" +
                "<\u7ae2<\u7ae6<\u7aed<\u7af0<\u7b02<\u7b0f<\u7b0a<\u7b06" +
                "<\u7b33<\u7b18<\u7b19<\u7b1e<\u7b35<\u7b28<\u7b36<\u7b50" +
                "<\u7b7a<\u7b04<\u7b4d<\u7b0b<\u7b4c<\u7b45<\u7b75<\u7b65" +
                "<\u7b74<\u7b67<\u7b70<\u7b71<\u7b6c<\u7b6e<\u7b9d<\u7b98" +
                "<\u7b9f<\u7b8d<\u7b9c<\u7b9a<\u7b8b<\u7b92<\u7b8f<\u7b5d" +
                "<\u7b99<\u7bcb<\u7bc1<\u7bcc<\u7bcf<\u7bb4<\u7bc6<\u7bdd" +
                "<\u7be9<\u7c11<\u7c14<\u7be6<\u7be5<\u7c60<\u7c00<\u7c07" +
                "<\u7c13<\u7bf3<\u7bf7<\u7c17<\u7c0d<\u7bf6<\u7c23<\u7c27" +
                "<\u7c2a<\u7c1f<\u7c37<\u7c2b<\u7c3d<\u7c4c<\u7c43<\u7c54" +
                "<\u7c4f<\u7c40<\u7c50<\u7c58<\u7c5f<\u7c64<\u7c56<\u7c65" +
                "<\u7c6c<\u7c75<\u7c83<\u7c90<\u7ca4<\u7cad<\u7ca2<\u7cab" +
                "<\u7ca1<\u7ca8<\u7cb3<\u7cb2<\u7cb1<\u7cae<\u7cb9<\u7cbd" +
                "<\u7cc0<\u7cc5<\u7cc2<\u7cd8<\u7cd2<\u7cdc<\u7ce2<\u9b3b" +
                "<\u7cef<\u7cf2<\u7cf4<\u7cf6<\u7cfa<\u7d06<\u7d02<\u7d1c" +
                "<\u7d15<\u7d0a<\u7d45<\u7d4b<\u7d2e<\u7d32<\u7d3f<\u7d35" +
                "<\u7d46<\u7d73<\u7d56<\u7d4e<\u7d72<\u7d68<\u7d6e<\u7d4f" +
                "<\u7d63<\u7d93<\u7d89<\u7d5b<\u7d8f<\u7d7d<\u7d9b<\u7dba" +
                "<\u7dae<\u7da3<\u7db5<\u7dc7<\u7dbd<\u7dab<\u7e3d<\u7da2" +
                "<\u7daf<\u7ddc<\u7db8<\u7d9f<\u7db0<\u7dd8<\u7ddd<\u7de4" +
                "<\u7dde<\u7dfb<\u7df2<\u7de1<\u7e05<\u7e0a<\u7e23<\u7e21" +
                "<\u7e12<\u7e31<\u7e1f<\u7e09<\u7e0b<\u7e22<\u7e46<\u7e66" +
                "<\u7e3b<\u7e35<\u7e39<\u7e43<\u7e37<\u7e32<\u7e3a<\u7e67" +
                "<\u7e5d<\u7e56<\u7e5e<\u7e59<\u7e5a<\u7e79<\u7e6a<\u7e69" +
                "<\u7e7c<\u7e7b<\u7e83<\u7dd5<\u7e7d<\u8fae<\u7e7f<\u7e88" +
                "<\u7e89<\u7e8c<\u7e92<\u7e90<\u7e93<\u7e94<\u7e96<\u7e8e" +
                "<\u7e9b<\u7e9c<\u7f38<\u7f3a<\u7f45<\u7f4c<\u7f4d<\u7f4e" +
                "<\u7f50<\u7f51<\u7f55<\u7f54<\u7f58<\u7f5f<\u7f60<\u7f68" +
                "<\u7f69<\u7f67<\u7f78<\u7f82<\u7f86<\u7f83<\u7f88<\u7f87" +
                "<\u7f8c<\u7f94<\u7f9e<\u7f9d<\u7f9a<\u7fa3<\u7faf<\u7fb2" +
                "<\u7fb9<\u7fae<\u7fb6<\u7fb8<\u8b71<\u7fc5<\u7fc6<\u7fca" +
                "<\u7fd5<\u7fd4<\u7fe1<\u7fe6<\u7fe9<\u7ff3<\u7ff9<\u98dc" +
                "<\u8006<\u8004<\u800b<\u8012<\u8018<\u8019<\u801c<\u8021" +
                "<\u8028<\u803f<\u803b<\u804a<\u8046<\u8052<\u8058<\u805a" +
                "<\u805f<\u8062<\u8068<\u8073<\u8072<\u8070<\u8076<\u8079" +
                "<\u807d<\u807f<\u8084<\u8086<\u8085<\u809b<\u8093<\u809a" +
                "<\u80ad<\u5190<\u80ac<\u80db<\u80e5<\u80d9<\u80dd<\u80c4" +
                "<\u80da<\u80d6<\u8109<\u80ef<\u80f1<\u811b<\u8129<\u8123" +
                "<\u812f<\u814b<\u968b<\u8146<\u813e<\u8153<\u8151<\u80fc" +
                "<\u8171<\u816e<\u8165<\u8166<\u8174<\u8183<\u8188<\u818a" +
                "<\u8180<\u8182<\u81a0<\u8195<\u81a4<\u81a3<\u815f<\u8193" +
                "<\u81a9<\u81b0<\u81b5<\u81be<\u81b8<\u81bd<\u81c0<\u81c2" +
                "<\u81ba<\u81c9<\u81cd<\u81d1<\u81d9<\u81d8<\u81c8<\u81da" +
                "<\u81df<\u81e0<\u81e7<\u81fa<\u81fb<\u81fe<\u8201<\u8202" +
                "<\u8205<\u8207<\u820a<\u820d<\u8210<\u8216<\u8229<\u822b" +
                "<\u8238<\u8233<\u8240<\u8259<\u8258<\u825d<\u825a<\u825f" +
                "<\u8264<\u8262<\u8268<\u826a<\u826b<\u822e<\u8271<\u8277" +
                "<\u8278<\u827e<\u828d<\u8292<\u82ab<\u829f<\u82bb<\u82ac" +
                "<\u82e1<\u82e3<\u82df<\u82d2<\u82f4<\u82f3<\u82fa<\u8393" +
                "<\u8303<\u82fb<\u82f9<\u82de<\u8306<\u82dc<\u8309<\u82d9" +
                "<\u8335<\u8334<\u8316<\u8332<\u8331<\u8340<\u8339<\u8350" +
                "<\u8345<\u832f<\u832b<\u8317<\u8318<\u8385<\u839a<\u83aa" +
                "<\u839f<\u83a2<\u8396<\u8323<\u838e<\u8387<\u838a<\u837c" +
                "<\u83b5<\u8373<\u8375<\u83a0<\u8389<\u83a8<\u83f4<\u8413" +
                "<\u83eb<\u83ce<\u83fd<\u8403<\u83d8<\u840b<\u83c1<\u83f7" +
                "<\u8407<\u83e0<\u83f2<\u840d<\u8422<\u8420<\u83bd<\u8438" +
                "<\u8506<\u83fb<\u846d<\u842a<\u843c<\u855a<\u8484<\u8477" +
                "<\u846b<\u84ad<\u846e<\u8482<\u8469<\u8446<\u842c<\u846f" +
                "<\u8479<\u8435<\u84ca<\u8462<\u84b9<\u84bf<\u849f<\u84d9" +
                "<\u84cd<\u84bb<\u84da<\u84d0<\u84c1<\u84c6<\u84d6<\u84a1" +
                "<\u8521<\u84ff<\u84f4<\u8517<\u8518<\u852c<\u851f<\u8515" +
                "<\u8514<\u84fc<\u8540<\u8563<\u8558<\u8548<\u8541<\u8602" +
                "<\u854b<\u8555<\u8580<\u85a4<\u8588<\u8591<\u858a<\u85a8" +
                "<\u856d<\u8594<\u859b<\u85ea<\u8587<\u859c<\u8577<\u857e" +
                "<\u8590<\u85c9<\u85ba<\u85cf<\u85b9<\u85d0<\u85d5<\u85dd" +
                "<\u85e5<\u85dc<\u85f9<\u860a<\u8613<\u860b<\u85fe<\u85fa" +
                "<\u8606<\u8622<\u861a<\u8630<\u863f<\u864d<\u4e55<\u8654" +
                "<\u865f<\u8667<\u8671<\u8693<\u86a3<\u86a9<\u86aa<\u868b" +
                "<\u868c<\u86b6<\u86af<\u86c4<\u86c6<\u86b0<\u86c9<\u8823" +
                "<\u86ab<\u86d4<\u86de<\u86e9<\u86ec<\u86df<\u86db<\u86ef" +
                "<\u8712<\u8706<\u8708<\u8700<\u8703<\u86fb<\u8711<\u8709" +
                "<\u870d<\u86f9<\u870a<\u8734<\u873f<\u8737<\u873b<\u8725" +
                "<\u8729<\u871a<\u8760<\u875f<\u8778<\u874c<\u874e<\u8774" +
                "<\u8757<\u8768<\u876e<\u8759<\u8753<\u8763<\u876a<\u8805" +
                "<\u87a2<\u879f<\u8782<\u87af<\u87cb<\u87bd<\u87c0<\u87d0" +
                "<\u96d6<\u87ab<\u87c4<\u87b3<\u87c7<\u87c6<\u87bb<\u87ef" +
                "<\u87f2<\u87e0<\u880f<\u880d<\u87fe<\u87f6<\u87f7<\u880e" +
                "<\u87d2<\u8811<\u8816<\u8815<\u8822<\u8821<\u8831<\u8836" +
                "<\u8839<\u8827<\u883b<\u8844<\u8842<\u8852<\u8859<\u885e" +
                "<\u8862<\u886b<\u8881<\u887e<\u889e<\u8875<\u887d<\u88b5" +
                "<\u8872<\u8882<\u8897<\u8892<\u88ae<\u8899<\u88a2<\u888d" +
                "<\u88a4<\u88b0<\u88bf<\u88b1<\u88c3<\u88c4<\u88d4<\u88d8" +
                "<\u88d9<\u88dd<\u88f9<\u8902<\u88fc<\u88f4<\u88e8<\u88f2" +
                "<\u8904<\u890c<\u890a<\u8913<\u8943<\u891e<\u8925<\u892a" +
                "<\u892b<\u8941<\u8944<\u893b<\u8936<\u8938<\u894c<\u891d" +
                "<\u8960<\u895e<\u8966<\u8964<\u896d<\u896a<\u896f<\u8974" +
                "<\u8977<\u897e<\u8983<\u8988<\u898a<\u8993<\u8998<\u89a1" +
                "<\u89a9<\u89a6<\u89ac<\u89af<\u89b2<\u89ba<\u89bd<\u89bf" +
                "<\u89c0<\u89da<\u89dc<\u89dd<\u89e7<\u89f4<\u89f8<\u8a03" +
                "<\u8a16<\u8a10<\u8a0c<\u8a1b<\u8a1d<\u8a25<\u8a36<\u8a41" +
                "<\u8a5b<\u8a52<\u8a46<\u8a48<\u8a7c<\u8a6d<\u8a6c<\u8a62" +
                "<\u8a85<\u8a82<\u8a84<\u8aa8<\u8aa1<\u8a91<\u8aa5<\u8aa6" +
                "<\u8a9a<\u8aa3<\u8ac4<\u8acd<\u8ac2<\u8ada<\u8aeb<\u8af3" +
                "<\u8ae7<\u8ae4<\u8af1<\u8b14<\u8ae0<\u8ae2<\u8af7<\u8ade" +
                "<\u8adb<\u8b0c<\u8b07<\u8b1a<\u8ae1<\u8b16<\u8b10<\u8b17" +
                "<\u8b20<\u8b33<\u97ab<\u8b26<\u8b2b<\u8b3e<\u8b28<\u8b41" +
                "<\u8b4c<\u8b4f<\u8b4e<\u8b49<\u8b56<\u8b5b<\u8b5a<\u8b6b" +
                "<\u8b5f<\u8b6c<\u8b6f<\u8b74<\u8b7d<\u8b80<\u8b8c<\u8b8e" +
                "<\u8b92<\u8b93<\u8b96<\u8b99<\u8b9a<\u8c3a<\u8c41<\u8c3f" +
                "<\u8c48<\u8c4c<\u8c4e<\u8c50<\u8c55<\u8c62<\u8c6c<\u8c78" +
                "<\u8c7a<\u8c82<\u8c89<\u8c85<\u8c8a<\u8c8d<\u8c8e<\u8c94" +
                "<\u8c7c<\u8c98<\u621d<\u8cad<\u8caa<\u8cbd<\u8cb2<\u8cb3" +
                "<\u8cae<\u8cb6<\u8cc8<\u8cc1<\u8ce4<\u8ce3<\u8cda<\u8cfd" +
                "<\u8cfa<\u8cfb<\u8d04<\u8d05<\u8d0a<\u8d07<\u8d0f<\u8d0d" +
                "<\u8d10<\u9f4e<\u8d13<\u8ccd<\u8d14<\u8d16<\u8d67<\u8d6d" +
                "<\u8d71<\u8d73<\u8d81<\u8d99<\u8dc2<\u8dbe<\u8dba<\u8dcf" +
                "<\u8dda<\u8dd6<\u8dcc<\u8ddb<\u8dcb<\u8dea<\u8deb<\u8ddf" +
                "<\u8de3<\u8dfc<\u8e08<\u8e09<\u8dff<\u8e1d<\u8e1e<\u8e10" +
                "<\u8e1f<\u8e42<\u8e35<\u8e30<\u8e34<\u8e4a<\u8e47<\u8e49" +
                "<\u8e4c<\u8e50<\u8e48<\u8e59<\u8e64<\u8e60<\u8e2a<\u8e63" +
                "<\u8e55<\u8e76<\u8e72<\u8e7c<\u8e81<\u8e87<\u8e85<\u8e84" +
                "<\u8e8b<\u8e8a<\u8e93<\u8e91<\u8e94<\u8e99<\u8eaa<\u8ea1" +
                "<\u8eac<\u8eb0<\u8ec6<\u8eb1<\u8ebe<\u8ec5<\u8ec8<\u8ecb" +
                "<\u8edb<\u8ee3<\u8efc<\u8efb<\u8eeb<\u8efe<\u8f0a<\u8f05" +
                "<\u8f15<\u8f12<\u8f19<\u8f13<\u8f1c<\u8f1f<\u8f1b<\u8f0c" +
                "<\u8f26<\u8f33<\u8f3b<\u8f39<\u8f45<\u8f42<\u8f3e<\u8f4c" +
                "<\u8f49<\u8f46<\u8f4e<\u8f57<\u8f5c<\u8f62<\u8f63<\u8f64" +
                "<\u8f9c<\u8f9f<\u8fa3<\u8fad<\u8faf<\u8fb7<\u8fda<\u8fe5" +
                "<\u8fe2<\u8fea<\u8fef<\u9087<\u8ff4<\u9005<\u8ff9<\u8ffa" +
                "<\u9011<\u9015<\u9021<\u900d<\u901e<\u9016<\u900b<\u9027" +
                "<\u9036<\u9035<\u9039<\u8ff8<\u904f<\u9050<\u9051<\u9052" +
                "<\u900e<\u9049<\u903e<\u9056<\u9058<\u905e<\u9068<\u906f" +
                "<\u9076<\u96a8<\u9072<\u9082<\u907d<\u9081<\u9080<\u908a" +
                "<\u9089<\u908f<\u90a8<\u90af<\u90b1<\u90b5<\u90e2<\u90e4" +
                "<\u6248<\u90db<\u9102<\u9112<\u9119<\u9132<\u9130<\u914a" +
                "<\u9156<\u9158<\u9163<\u9165<\u9169<\u9173<\u9172<\u918b" +
                "<\u9189<\u9182<\u91a2<\u91ab<\u91af<\u91aa<\u91b5<\u91b4" +
                "<\u91ba<\u91c0<\u91c1<\u91c9<\u91cb<\u91d0<\u91d6<\u91df" +
                "<\u91e1<\u91db<\u91fc<\u91f5<\u91f6<\u921e<\u91ff<\u9214" +
                "<\u922c<\u9215<\u9211<\u925e<\u9257<\u9245<\u9249<\u9264" +
                "<\u9248<\u9295<\u923f<\u924b<\u9250<\u929c<\u9296<\u9293" +
                "<\u929b<\u925a<\u92cf<\u92b9<\u92b7<\u92e9<\u930f<\u92fa" +
                "<\u9344<\u932e<\u9319<\u9322<\u931a<\u9323<\u933a<\u9335" +
                "<\u933b<\u935c<\u9360<\u937c<\u936e<\u9356<\u93b0<\u93ac" +
                "<\u93ad<\u9394<\u93b9<\u93d6<\u93d7<\u93e8<\u93e5<\u93d8" +
                "<\u93c3<\u93dd<\u93d0<\u93c8<\u93e4<\u941a<\u9414<\u9413" +
                "<\u9403<\u9407<\u9410<\u9436<\u942b<\u9435<\u9421<\u943a" +
                "<\u9441<\u9452<\u9444<\u945b<\u9460<\u9462<\u945e<\u946a" +
                "<\u9229<\u9470<\u9475<\u9477<\u947d<\u945a<\u947c<\u947e" +
                "<\u9481<\u947f<\u9582<\u9587<\u958a<\u9594<\u9596<\u9598" +
                "<\u9599<\u95a0<\u95a8<\u95a7<\u95ad<\u95bc<\u95bb<\u95b9" +
                "<\u95be<\u95ca<\u6ff6<\u95c3<\u95cd<\u95cc<\u95d5<\u95d4" +
                "<\u95d6<\u95dc<\u95e1<\u95e5<\u95e2<\u9621<\u9628<\u962e" +
                "<\u962f<\u9642<\u964c<\u964f<\u964b<\u9677<\u965c<\u965e" +
                "<\u965d<\u965f<\u9666<\u9672<\u966c<\u968d<\u9698<\u9695" +
                "<\u9697<\u96aa<\u96a7<\u96b1<\u96b2<\u96b0<\u96b4<\u96b6" +
                "<\u96b8<\u96b9<\u96ce<\u96cb<\u96c9<\u96cd<\u894d<\u96dc" +
                "<\u970d<\u96d5<\u96f9<\u9704<\u9706<\u9708<\u9713<\u970e" +
                "<\u9711<\u970f<\u9716<\u9719<\u9724<\u972a<\u9730<\u9739" +
                "<\u973d<\u973e<\u9744<\u9746<\u9748<\u9742<\u9749<\u975c" +
                "<\u9760<\u9764<\u9766<\u9768<\u52d2<\u976b<\u9771<\u9779" +
                "<\u9785<\u977c<\u9781<\u977a<\u9786<\u978b<\u978f<\u9790" +
                "<\u979c<\u97a8<\u97a6<\u97a3<\u97b3<\u97b4<\u97c3<\u97c6" +
                "<\u97c8<\u97cb<\u97dc<\u97ed<\u9f4f<\u97f2<\u7adf<\u97f6" +
                "<\u97f5<\u980f<\u980c<\u9838<\u9824<\u9821<\u9837<\u983d" +
                "<\u9846<\u984f<\u984b<\u986b<\u986f<\u9870<\u9871<\u9874" +
                "<\u9873<\u98aa<\u98af<\u98b1<\u98b6<\u98c4<\u98c3<\u98c6" +
                "<\u98e9<\u98eb<\u9903<\u9909<\u9912<\u9914<\u9918<\u9921" +
                "<\u991d<\u991e<\u9924<\u9920<\u992c<\u992e<\u993d<\u993e" +
                "<\u9942<\u9949<\u9945<\u9950<\u994b<\u9951<\u9952<\u994c" +
                "<\u9955<\u9997<\u9998<\u99a5<\u99ad<\u99ae<\u99bc<\u99df" +
                "<\u99db<\u99dd<\u99d8<\u99d1<\u99ed<\u99ee<\u99f1<\u99f2" +
                "<\u99fb<\u99f8<\u9a01<\u9a0f<\u9a05<\u99e2<\u9a19<\u9a2b" +
                "<\u9a37<\u9a45<\u9a42<\u9a40<\u9a43<\u9a3e<\u9a55<\u9a4d" +
                "<\u9a5b<\u9a57<\u9a5f<\u9a62<\u9a65<\u9a64<\u9a69<\u9a6b" +
                "<\u9a6a<\u9aad<\u9ab0<\u9abc<\u9ac0<\u9acf<\u9ad1<\u9ad3" +
                "<\u9ad4<\u9ade<\u9adf<\u9ae2<\u9ae3<\u9ae6<\u9aef<\u9aeb" +
                "<\u9aee<\u9af4<\u9af1<\u9af7<\u9afb<\u9b06<\u9b18<\u9b1a" +
                "<\u9b1f<\u9b22<\u9b23<\u9b25<\u9b27<\u9b28<\u9b29<\u9b2a" +
                "<\u9b2e<\u9b2f<\u9b32<\u9b44<\u9b43<\u9b4f<\u9b4d<\u9b4e" +
                "<\u9b51<\u9b58<\u9b74<\u9b93<\u9b83<\u9b91<\u9b96<\u9b97" +
                "<\u9b9f<\u9ba0<\u9ba8<\u9bb4<\u9bc0<\u9bca<\u9bb9<\u9bc6" +
                "<\u9bcf<\u9bd1<\u9bd2<\u9be3<\u9be2<\u9be4<\u9bd4<\u9be1" +
                "<\u9c3a<\u9bf2<\u9bf1<\u9bf0<\u9c15<\u9c14<\u9c09<\u9c13" +
                "<\u9c0c<\u9c06<\u9c08<\u9c12<\u9c0a<\u9c04<\u9c2e<\u9c1b" +
                "<\u9c25<\u9c24<\u9c21<\u9c30<\u9c47<\u9c32<\u9c46<\u9c3e" +
                "<\u9c5a<\u9c60<\u9c67<\u9c76<\u9c78<\u9ce7<\u9cec<\u9cf0" +
                "<\u9d09<\u9d08<\u9ceb<\u9d03<\u9d06<\u9d2a<\u9d26<\u9daf" +
                "<\u9d23<\u9d1f<\u9d44<\u9d15<\u9d12<\u9d41<\u9d3f<\u9d3e" +
                "<\u9d46<\u9d48<\u9d5d<\u9d5e<\u9d64<\u9d51<\u9d50<\u9d59" +
                "<\u9d72<\u9d89<\u9d87<\u9dab<\u9d6f<\u9d7a<\u9d9a<\u9da4" +
                "<\u9da9<\u9db2<\u9dc4<\u9dc1<\u9dbb<\u9db8<\u9dba<\u9dc6" +
                "<\u9dcf<\u9dc2<\u9dd9<\u9dd3<\u9df8<\u9de6<\u9ded<\u9def" +
                "<\u9dfd<\u9e1a<\u9e1b<\u9e1e<\u9e75<\u9e79<\u9e7d<\u9e81" +
                "<\u9e88<\u9e8b<\u9e8c<\u9e92<\u9e95<\u9e91<\u9e9d<\u9ea5" +
                "<\u9ea9<\u9eb8<\u9eaa<\u9ead<\u9761<\u9ecc<\u9ece<\u9ecf" +
                "<\u9ed0<\u9ed4<\u9edc<\u9ede<\u9edd<\u9ee0<\u9ee5<\u9ee8" +
                "<\u9eef<\u9ef4<\u9ef6<\u9ef7<\u9ef9<\u9efb<\u9efc<\u9efd" +
                "<\u9f07<\u9f08<\u76b7<\u9f15<\u9f21<\u9f2c<\u9f3e<\u9f4a" +
                "<\u9f52<\u9f54<\u9f63<\u9f5f<\u9f60<\u9f61<\u9f66<\u9f67" +
                "<\u9f6c<\u9f6a<\u9f77<\u9f72<\u9f76<\u9f95<\u9f9c<\u9fa0" +
                "<\u582f<\u69c7<\u9059<\u7464<\u51dc<\u7199" +
                //
                //jisx0213 ideographs
                //  (1)Level 3 and 4 kanji characters are sorted after jisx0208
                //     kanji (Level 1 and 2 kanji)
                //  (2)In each level, characters are sorted by JIS order
                //  (3)"JIS X 0213 compatibility additions" characters
                //     0xfa30-0xfa6a have been purposely removed from the rules,
                //     they will be added later during rule table initialization
                //     by using the order value of their corresponding canonical
                //     equivalent.
                //
                // jisx0213/level 3 kanji
                "<\ud840\udc0b<\u3402<\u4e28<\u4e2f<\u4e30<\u4e8d<\u4ee1" +
                "<\u4efd<\u4eff<\u4f03<\u4f0b<\u4f60<\u4f48<\u4f49<\u4f56" +
                "<\u4f5f<\u4f6a<\u4f6c<\u4f7e<\u4f8a<\u4f94<\u4f97<\u4fc9" +
                "<\u4fe0<\u5001<\u5002<\u500e<\u5018<\u5027<\u502e<\u5040" +
                "<\u503b<\u5041<\u5094<\u50cc<\u50f2<\u50d0<\u50e6<\u5106" +
                "<\u5103<\u510b<\u511e<\u5135<\u514a<\u5155<\u5157<\u34B5" +
                "<\u519d<\u51C3<\u51CA<\u51de<\u51e2<\u51ee<\u5201<\u34DB" +
                "<\u5213<\u5215<\u5249<\u5257<\u5261<\u5293<\u52c8<\u52cc" +
                "<\u52D0<\u52d6<\u52db<\u52f0<\u52FB<\u5300<\u5307<\u531c" +
                "<\u5361<\u5363<\u537D<\u5393<\u539d<\u53b2<\u5412<\u5427" +
                "<\u544d<\u549c<\u546b<\u5474<\u547f<\u5488<\u5496<\u54a1" +
                "<\u54a9<\u54c6<\u54ff<\u550e<\u552b<\u5535<\u5550<\u555e" +
                "<\u5581<\u5586<\u558e<\u55ad<\u55ce<\u5608<\u560e<\u563b" +
                "<\u5649<\u5676<\u5666<\u566f<\u5671<\u5672<\u5699<\u569e" +
                "<\u56a9<\u56ac<\u56b3<\u56c9<\u56ca<\u570a<\ud844\ude3d" +
                "<\u5721<\u572f<\u5733<\u5734<\u5770<\u5777<\u577c<\u579c" +
                "<\ud844\udf1b<\u57b8<\u57c7<\u57c8<\u57cf<\u57e4<\u57ed" +
                "<\u57f5<\u57f6<\u57ff<\u5809<\u5861<\u5864<\u587c<\u5889" +
                "<\u589e<\u58a9<\ud845\udc6e<\u58d2<\u58ce<\u58d4<\u58da" +
                "<\u58E0<\u58e9<\u590c<\u8641<\u595d<\u596d<\u598b<\u5992" +
                "<\u59a4<\u59c3<\u59d2<\u59dd<\u5a13<\u5a23<\u5a67<\u5a6d" +
                "<\u5a77<\u5a7e<\u5A84<\u5a9e<\u5aa7<\u5ac4<\ud846\udcbd" +
                "<\u5b19<\u5b25<\u5b41<\u5b56<\u5b7d<\u5b93<\u5bd8<\u5bec" +
                "<\u5C12<\u5c1e<\u5c23<\u5c2b<\u378D<\u5c62<\ud845\udeb4" +
                "<\u5c7a<\u5c8f<\u5c9f<\u5ca3<\u5caa<\u5cba<\u5ccb<\u5CD0" +
                "<\u5cd2<\u5cf4<\ud847\ude34<\u37E2<\u5d0d<\u5d27<\u5d46" +
                "<\u5D47<\u5d53<\u5d4a<\u5d6d<\u5d81<\u5da0<\u5DA4<\u5da7" +
                "<\u5db8<\u5dcb<\u5DE2<\u5e14<\u5e18<\u5e58<\u5e5e<\u5ebe" +
                "<\u5ecb<\u5EF9<\u5F00<\u5f02<\u5f07<\u5f1d<\u5f23<\u5f34" +
                "<\u5f36<\u5f3d<\u5f40<\u5f45<\u5f54<\u5f58<\u5f64<\u5f67" +
                "<\u5f7d<\u5f89<\u5f9c<\u5fa7<\u5faf<\u5FB5<\u5fb7<\u5fc9" +
                "<\u5fde<\u5fe1<\u5fe9<\u600d<\u6014<\u6018<\u6033<\u6035" +
                "<\u6047<\u609d<\u609e<\u60cb<\u60d4<\u60d5<\u60dd<\u60f8" +
                "<\u611c<\u612b<\u6130<\u6137<\u618d<\u61bc<\u61b9<\u6222" +
                "<\u623E<\u6243<\u6256<\u625a<\u626f<\u6285<\u62c4<\u62d6" +
                "<\u62fc<\u630a<\u6318<\u6339<\u6343<\u6365<\u637c<\u63e5" +
                "<\u63ED<\u63f5<\u6410<\u6414<\u6422<\u6479<\u6451<\u6460" +
                "<\u646d<\u64ce<\u64be<\u64bf<\u64c4<\u64ca<\u64d0<\u64f7" +
                "<\u64fb<\u6522<\u6529<\u6567<\u659d<\u6600<\u6609<\u6615" +
                "<\u661e<\u663A<\u6622<\u6624<\u662b<\u6630<\u6631<\u6633" +
                "<\u66fb<\u6648<\u664c<\ud84c\uddc4<\u6659<\u665A<\u6661" +
                "<\u6665<\u6673<\u6677<\u6678<\u668d<\u66a0<\u66b2<\u66bb" +
                "<\u66C6<\u66c8<\u3B22<\u66db<\u66e8<\u66fa<\u6713<\u6733" +
                "<\u6766<\u6747<\u6748<\u677b<\u6781<\u6793<\u6798<\u679b" +
                "<\u67bb<\u67f9<\u67c0<\u67d7<\u67FC<\u6801<\u6852<\u681d" +
                "<\u682c<\u6831<\u685b<\u6872<\u6875<\u68a3<\u68a5<\u68b2" +
                "<\u68c8<\u68d0<\u68e8<\u68ed<\u68f0<\u68f1<\u68fc<\u690a" +
                "<\u6949<\ud84d\uddc4<\u6935<\u6942<\u6957<\u6963<\u6964" +
                "<\u6968<\u6980<\u69a5<\u69ad<\u69CF<\u3BB6<\u3BC3<\u69e2" +
                "<\u69E9<\u69f5<\u69F6<\u6a0f<\u6a15<\ud84d\udf3f<\u6a3b" +
                "<\u6a3e<\u6a45<\u6a50<\u6a56<\u6a5b<\u6a6b<\u6a73<\ud84d\udf63" +
                "<\u6a89<\u6A94<\u6a9d<\u6a9e<\u6aa5<\u6ae4<\u6ae7<\u3C0F" +
                "<\u6b1b<\u6b1e<\u6b2c<\u6b35<\u6b46<\u6b56<\u6b60<\u6B65" +
                "<\u6b67<\u6B77<\u6b82<\u6ba9<\u6bad<\u6BCF<\u6bd6<\u6BD7" +
                "<\u6bff<\u6c05<\u6c10<\u6c33<\u6c59<\u6c5c<\u6CAA<\u6c74" +
                "<\u6c76<\u6c85<\u6c86<\u6c98<\u6c9c<\u6CFB<\u6cc6<\u6cd4" +
                "<\u6ce0<\u6ceb<\u6cee<\ud84f\udcfe<\u6d04<\u6d0e<\u6d2e" +
                "<\u6d31<\u6d39<\u6d3f<\u6D58<\u6d65<\u6d82<\u6d87<\u6D89" +
                "<\u6d94<\u6daa<\u6dac<\u6dbf<\u6dc4<\u6dd6<\u6DDA<\u6ddb" +
                "<\u6ddd<\u6dfc<\u6E34<\u6e44<\u6e5c<\u6e5e<\u6EAB<\u6eb1" +
                "<\u6ec1<\u6ec7<\u6ece<\u6F10<\u6f1a<\u6f2a<\u6f2f<\u6f33" +
                "<\u6f51<\u6f59<\u6f5e<\u6f61<\u6f62<\u6f7e<\u6f88<\u6f8c" +
                "<\u6f8d<\u6f94<\u6fa0<\u6fa7<\u6fb6<\u6fbc<\u6fc7<\u6fca" +
                "<\u6ff9<\u6ff0<\u6ff5<\u7005<\u7006<\u7028<\u704a<\u705d" +
                "<\u705e<\u704e<\u7064<\u7075<\u7085<\u70a4<\u70ab<\u70b7" +
                "<\u70d4<\u70d8<\u70e4<\u710f<\u712b<\u711e<\u7120<\u712E" +
                "<\u7130<\u7146<\u7147<\u7151<\u7152<\u715c<\u7160<\u7168" +
                "<\u7185<\u7187<\u7192<\u71c1<\u71ba<\u71c4<\u71fe<\u7200" +
                "<\u7215<\u7255<\u7256<\u3E3F<\u728d<\u729b<\u72be<\u72C0" +
                "<\u72fb<\ud851\udff1<\u7327<\u7328<\u7350<\u7366<\u737c" +
                "<\u7395<\u739f<\u73a0<\u73a2<\u73a6<\u73ab<\u73c9<\u73cf" +
                "<\u73d6<\u73d9<\u73e3<\u73e9<\u7407<\u740a<\u741a<\u741b" +
                "<\u7426<\u7428<\u742a<\u742b<\u742c<\u742e<\u742f<\u7430" +
                "<\u7444<\u7446<\u7447<\u744b<\u7457<\u7462<\u746b<\u746d" +
                "<\u7486<\u7487<\u7489<\u7498<\u749c<\u749f<\u74a3<\u7490" +
                "<\u74a6<\u74a8<\u74a9<\u74b5<\u74bf<\u74c8<\u74c9<\u74da" +
                "<\u74ff<\u7501<\u7517<\u752f<\u756f<\u7579<\u7592<\u3F72" +
                "<\u75ce<\u75e4<\u7600<\u7602<\u7608<\u7615<\u7616<\u7619" +
                "<\u761e<\u762d<\u7635<\u7643<\u764b<\u7664<\u7665<\u766d" +
                "<\u766f<\u7671<\u7681<\u769b<\u769d<\u769e<\u76a6<\u76aa" +
                "<\u76B6<\u76c5<\u76cc<\u76ce<\u76d4<\u76e6<\u76f1<\u76fc" +
                "<\u770a<\u7719<\u7734<\u7736<\u7746<\u774d<\u774e<\u775c" +
                "<\u775f<\u7762<\u777a<\u7780<\u7794<\u77aa<\u77e0<\u782d" +
                "<\ud855\udc8e<\u7843<\u784e<\u784F<\u7851<\u7868<\u786e" +
                "<\u78b0<\ud855\udd0e<\u78ad<\u78e4<\u78f2<\u7900<\u78f7" +
                "<\u791c<\u792E<\u7931<\u7934<\u7945<\u7946<\u795c<\u7979" +
                "<\u7998<\u79b1<\u79b8<\u79c8<\u79ca<\ud855\udf71<\u79d4" +
                "<\u79de<\u79eb<\u79ed<\u7a03<\u7a39<\u7a5d<\u7a6d<\u7a85" +
                "<\u7aa0<\ud856\uddc4<\u7ab3<\u7abb<\u7ace<\u7aeb<\u7afd" +
                "<\u7B12<\u7b2d<\u7B3B<\u7b47<\u7b4e<\u7b60<\u7b6d<\u7b6f" +
                "<\u7b72<\u7b9e<\u7bd7<\u7bd9<\u7c01<\u7c31<\u7C1E<\u7c20" +
                "<\u7c33<\u7c36<\u4264<\ud857\udda1<\u7c59<\u7c6d<\u7c79" +
                "<\u7c8f<\u7c94<\u7ca0<\u7cbc<\u7cd5<\u7cd9<\u7cdd<\u7d07" +
                "<\u7d08<\u7d13<\u7d1d<\u7d23<\u7d31<\u7d41<\u7d48<\u7d53" +
                "<\u7d5c<\u7d7a<\u7d83<\u7d8b<\u7da0<\u7da6<\u7dc2<\u7dcc" +
                "<\u7dd6<\u7DE3<\u7e28<\u7e08<\u7e11<\u7e15<\u7e47<\u7e52" +
                "<\u7e61<\u7e8a<\u7e8d<\u7f47<\u7f91<\u7f97<\u7fbf<\u7fce" +
                "<\u7fdb<\u7fdf<\u7fec<\u7fee<\u7ffa<\u8014<\u8026<\u8035" +
                "<\u8037<\u803c<\u80CA<\u80d7<\u80e0<\u80f3<\u8118<\u814a" +
                "<\u8160<\u8167<\u8168<\u816d<\u81bb<\u81ca<\u81cf<\u81d7" +
                "<\u4453<\u445B<\u8260<\u8274<\ud85a\udeff<\u828e<\u82a1" +
                "<\u82a3<\u82a4<\u82a9<\u82ae<\u82b7<\u82be<\u82bf<\u82c6" +
                "<\u82d5<\u82fd<\u82fe<\u8300<\u8301<\u8362<\u8322<\u832d" +
                "<\u833a<\u8343<\u8347<\u8351<\u8355<\u837d<\u8386<\u8392" +
                "<\u8398<\u83a7<\u83a9<\u83bf<\u83c0<\u83c7<\u83cf<\u83d1" +
                "<\u83E1<\u83ea<\u8401<\u8406<\u840a<\u8448<\u845F<\u8470" +
                "<\u8473<\u8485<\u849e<\u84af<\u84b4<\u84ba<\u84c0<\u84c2" +
                "<\ud85b\ude40<\u8532<\u851e<\u8523<\u852f<\u8559<\u8564" +
                "<\u85ad<\u857a<\u858c<\u858f<\u85a2<\u85b0<\u85cb<\u85ce" +
                "<\u85ed<\u8612<\u85ff<\u8604<\u8605<\u8610<\ud85c\udcf4" +
                "<\u8618<\u8629<\u8638<\u8657<\u865B<\u8662<\u459D<\u866c" +
                "<\u8675<\u8698<\u86b8<\u86fa<\u86fc<\u86fd<\u870b<\u8771" +
                "<\u8787<\u8788<\u87ac<\u87ad<\u87b5<\u45EA<\u87d6<\u87EC" +
                "<\u8806<\u880a<\u8810<\u8814<\u881f<\u8898<\u88aa<\u88ca" +
                "<\u88ce<\ud85d\ude84<\u88f5<\u891c<\u8918<\u8919<\u891a" +
                "<\u8927<\u8930<\u8932<\u8939<\u8940<\u8994<\u89d4<\u89e5" +
                "<\u89f6<\u8a12<\u8a15<\u8a22<\u8a37<\u8a47<\u8a4e<\u8a5d" +
                "<\u8a61<\u8a75<\u8a79<\u8aa7<\u8AD0<\u8adf<\u8af4<\u8af6" +
                "<\u8b46<\u8b54<\u8b59<\u8B69<\u8B9D<\u8c49<\u8c68<\u8ce1" +
                "<\u8cf4<\u8cf8<\u8cfe<\u8d12<\u8d1b<\u8daf<\u8dce<\u8dd1" +
                "<\u8dd7<\u8e20<\u8e23<\u8e3d<\u8e70<\u8e7b<\ud860\ude77" +
                "<\u8ec0<\u4844<\u8efa<\u8f1e<\u8f2d<\u8f36<\u8f54<\ud860\udfcd" +
                "<\u8fa6<\u8fb5<\u8fe4<\u8fe8<\u8fee<\u9008<\u902d<\u9088" +
                "<\u9095<\u9097<\u9099<\u909b<\u90a2<\u90b3<\u90be<\u90c4" +
                "<\u90c5<\u90c7<\u90d7<\u90dd<\u90de<\u90ef<\u90f4<\u9114" +
                "<\u9115<\u9116<\u9122<\u9123<\u9127<\u912f<\u9131<\u9134" +
                "<\u913d<\u9148<\u915b<\u9183<\u919e<\u91ac<\u91b1<\u91bc" +
                "<\u91d7<\u91fb<\u91e4<\u91e5<\u91ed<\u91f1<\u9207<\u9210" +
                "<\u9238<\u9239<\u923a<\u923c<\u9240<\u9243<\u924f<\u9278" +
                "<\u9288<\u92c2<\u92cb<\u92cc<\u92d3<\u92e0<\u92ff<\u9304" +
                "<\u931f<\u9321<\u9325<\u9348<\u9349<\u934A<\u9364<\u9365" +
                "<\u936a<\u9370<\u939b<\u93a3<\u93ba<\u93c6<\u93de<\u93df" +
                "<\u9404<\u93fd<\u9433<\u944a<\u9463<\u946b<\u9471<\u9472" +
                "<\u958e<\u959f<\u95a6<\u95a9<\u95ac<\u95b6<\u95bd<\u95cb" +
                "<\u95d0<\u95d3<\u49B0<\u95da<\u95de<\u9658<\u9684<\u969d" +
                "<\u96a4<\u96a5<\u96d2<\u96de<\u96e9<\u96ef<\u9733<\u973b" +
                "<\u974d<\u974e<\u974f<\u975a<\u976e<\u9773<\u9795<\u97ae" +
                "<\u97ba<\u97c1<\u97c9<\u97de<\u97db<\u97f4<\u980a<\u981e" +
                "<\u982b<\u9830<\u9852<\u9853<\u9856<\u9857<\u9859<\u985a" +
                "<\u9865<\u986c<\u98ba<\u98c8<\u98e7<\u9958<\u999e<\u9a02" +
                "<\u9a03<\u9a24<\u9a2d<\u9a2e<\u9a38<\u9a4a<\u9a4e<\u9A52" +
                "<\u9ab6<\u9ac1<\u9ac3<\u9ace<\u9ad6<\u9af9<\u9b02<\u9b08" +
                "<\u9b20<\u4C17<\u9b2d<\u9b5e<\u9b79<\u9b66<\u9b72<\u9b75" +
                "<\u9b84<\u9b8a<\u9b8f<\u9b9e<\u9ba7<\u9bc1<\u9bce<\u9be5" +
                "<\u9bf8<\u9bfd<\u9c00<\u9c23<\u9c41<\u9c4f<\u9c50<\u9c53" +
                "<\u9c63<\u9C65<\u9c77<\u9d1d<\u9d1e<\u9d43<\u9d47<\u9D52" +
                "<\u9d63<\u9d70<\u9d7c<\u9d8a<\u9d96<\u9DC0<\u9dac<\u9dbc" +
                "<\u9dd7<\ud868\udd90<\u9de7<\u9e07<\u9e15<\u9e7c<\u9e9e" +
                "<\u9ea4<\u9eac<\u9eaf<\u9eb4<\u9eb5<\u9EC3<\u9ed1<\u9f10" +
                "<\u9f39<\u9f57<\u9f90<\u9f94<\u9f97<\u9fa2" +
                // jisx0213/level 4 kanji
                "<\ud840\udc89<\u4e02<\u4E0F<\u4e12<\u4E29<\u4e2b<\u4e2e" +
                "<\u4e40<\u4e47<\u4E48<\ud840\udca2<\u4e51<\u3406<\ud840\udca4" +
                "<\u4e5a<\u4e69<\u4e9d<\u342C<\u342E<\u4eb9<\u4EBB<\ud840\udda2" +
                "<\u4EBC<\u4ec3<\u4EC8<\u4ed0<\u4EEB<\u4eda<\u4ef1<\u4ef5" +
                "<\u4f00<\u4f16<\u4F64<\u4f37<\u4f3e<\u4f54<\u4f58<\ud840\ude13" +
                "<\u4f77<\u4f78<\u4f7a<\u4f7d<\u4f82<\u4f85<\u4f92<\u4f9a" +
                "<\u4FE6<\u4fb2<\u4fbe<\u4fc5<\u4fcb<\u4fcf<\u4fd2<\u346A" +
                "<\u4ff2<\u5000<\u5010<\u5013<\u501c<\u501e<\u5022<\u3468" +
                "<\u5042<\u5046<\u504e<\u5053<\u5057<\u5063<\u5066<\u506a" +
                "<\u5070<\u50A3<\u5088<\u5092<\u5093<\u5095<\u5096<\u509c" +
                "<\u50aa<\ud840\udf2b<\u50B1<\u50ba<\u50BB<\u50c4<\u50c7" +
                "<\u50F3<\ud840\udf81<\u50ce<\ud840\udf71<\u50d4<\u50D9" +
                "<\u50E1<\u50e9<\u3492<\u5108<\ud840\udff9<\u5117<\u511b" +
                "<\ud841\udc4a<\u5160<\ud841\udd09<\u5173<\u5183<\u518b" +
                "<\u34BC<\u5198<\u51a3<\u51ad<\u34C7<\u51bc<\ud841\uddd6" +
                "<\ud841\ude28<\u51f3<\u51f4<\u5202<\u5212<\u5216<\ud841\udf4f" +
                "<\u5255<\u525c<\u526C<\u5277<\u5284<\u5282<\ud842\udc07" +
                "<\u5298<\ud842\udc3a<\u52a4<\u52a6<\u52af<\u52ba<\u52bb" +
                "<\u52CA<\u351F<\u52d1<\ud842\udcb9<\u52f7<\u530a<\u530b" +
                "<\u5324<\u5335<\u533e<\u5342<\ud842\udd7c<\ud842\udd9d" +
                "<\u5367<\u536c<\u537A<\u53a4<\u53b4<\ud842\uded3<\u53b7" +
                "<\u53c0<\ud842\udf1d<\u355D<\u355E<\u53d5<\u53da<\u3563" +
                "<\u53F4<\u53f5<\u5455<\u5424<\u5428<\u356E<\u5443<\u5462" +
                "<\u5466<\u546C<\u548a<\u548d<\u5495<\u54A0<\u54a6<\u54ad" +
                "<\u54ae<\u54b7<\u54ba<\u54bf<\u54C3<\ud843\udd45<\u54ec" +
                "<\u54ef<\u54F1<\u54F3<\u5500<\u5501<\u5509<\u553c<\u5541" +
                "<\u35A6<\u5547<\u554a<\u35A8<\u5560<\u5561<\u5564<\ud843\udde1" +
                "<\u557D<\u5582<\u5588<\u5591<\u35C5<\u55d2<\ud843\ude95" +
                "<\ud843\ude6d<\u55bf<\u55c9<\u55cc<\u55d1<\u55DD<\u35DA" +
                "<\u55e2<\ud843\ude64<\u55e9<\u5628<\ud843\udf5f<\u5607" +
                "<\u5610<\u5630<\u5637<\u35F4<\u563d<\u563f<\u5640<\u5647" +
                "<\u565e<\u5660<\u566d<\u3605<\u5688<\u568c<\u5695<\u569a" +
                "<\u569d<\u56a8<\u56ad<\u56B2<\u56c5<\u56cd<\u56df<\u56e8" +
                "<\u56f6<\u56f7<\ud844\ude01<\u5715<\u5723<\ud844\ude55" +
                "<\u5729<\ud844\ude7b<\u5745<\u5746<\u574c<\u574d<\ud844\ude74" +
                "<\u5768<\u576f<\u5773<\u5774<\u5775<\u577b<\ud844\udee4" +
                "<\ud844\uded7<\u57ac<\u579a<\u579d<\u579e<\u57a8<\u57D7" +
                "<\ud844\udefd<\u57cc<\ud844\udf36<\ud844\udf44<\u57de" +
                "<\u57e6<\u57f0<\u364A<\u57f8<\u57FB<\u57fd<\u5804<\u581e" +
                "<\u5820<\u5827<\u5832<\u5839<\ud844\udfc4<\u5849<\u584c" +
                "<\u5867<\u588a<\u588B<\u588d<\u588f<\u5890<\u5894<\u589d" +
                "<\u58AA<\u58b1<\ud845\udc6d<\u58C3<\u58cd<\u58e2<\u58f3" +
                "<\u58F4<\u5905<\u5906<\u590b<\u590D<\u5914<\u5924<\ud845\uddd7" +
                "<\u3691<\u593D<\u3699<\u5946<\u3696<\ud85b\udc29<\u595b" +
                "<\u595f<\ud845\ude47<\u5975<\u5976<\u597c<\u599f<\u59ae" +
                "<\u59bc<\u59c8<\u59cd<\u59de<\u59e3<\u59e4<\u59e7<\u59ee" +
                "<\ud845\udf06<\ud845\udf42<\u36CF<\u5a0c<\u5a0d<\u5A17" +
                "<\u5a27<\u5a2d<\u5a55<\u5a65<\u5a7a<\u5a8b<\u5a9c<\u5a9f" +
                "<\u5aa0<\u5aa2<\u5ab1<\u5ab3<\u5ab5<\u5aba<\u5abf<\u5ada" +
                "<\u5adc<\u5ae0<\u5ae5<\u5AF0<\u5aee<\u5af5<\u5b00<\u5b08" +
                "<\u5b17<\u5b34<\u5b2d<\u5b4c<\u5b52<\u5b68<\u5b6f<\u5b7c" +
                "<\u5b7f<\u5b81<\u5b84<\ud846\uddc3<\u5b96<\u5bac<\u3761" +
                "<\u5bc0<\u3762<\u5BCE<\u5bd6<\u376C<\u376B<\u5bf1<\u5bfd" +
                "<\u3775<\u5C03<\u5c29<\u5c30<\ud847\udc56<\u5C5F<\u5c63" +
                "<\u5c67<\u5c68<\u5c69<\u5c70<\ud847\udd2d<\ud847\udd45" +
                "<\u5c7c<\ud847\udd78<\ud847\udd62<\u5c88<\u5c8a<\u37C1" +
                "<\ud847\udda1<\ud847\udd9c<\u5ca0<\u5ca2<\u5ca6<\u5CA7" +
                "<\ud847\udd92<\u5CAD<\u5cb5<\ud847\uddb7<\u5cc9<\ud847\udde0" +
                "<\ud847\ude33<\u5d06<\u5D10<\u5d2b<\u5D1D<\u5D20<\u5d24" +
                "<\u5d26<\u5d31<\u5d39<\u5d42<\u37E8<\u5d61<\u5d6a<\u37F4" +
                "<\u5d70<\ud847\udf1e<\u37FD<\u5d88<\u3800<\u5d92<\u5d94" +
                "<\u5D97<\u5d99<\u5db0<\u5db2<\u5db4<\ud847\udf76<\u5db9" +
                "<\u5DD1<\u5DD7<\u5dd8<\u5de0<\ud847\udffa<\u5de4<\u5de9" +
                "<\u382F<\u5e00<\u3836<\u5e12<\u5e15<\u3840<\u5e1f<\u5e2e" +
                "<\u5e3e<\u5e49<\u385C<\u5e56<\u3861<\u5e6b<\u5e6c<\u5e6d" +
                "<\u5e6e<\ud848\udd7b<\u5ea5<\u5eaa<\u5eac<\u5EB9<\u5ebf" +
                "<\u5ec6<\u5ed2<\u5ED9<\ud848\udf1e<\u5EFD<\u5f08<\u5f0e" +
                "<\u5f1c<\ud848\udfad<\u5F1E<\u5f47<\u5f63<\u5f72<\u5f7e" +
                "<\u5f8f<\u5fa2<\u5fa4<\u5fb8<\u5fc4<\u38FA<\u5fc7<\u5fcb" +
                "<\u5fd2<\u5fd3<\u5fd4<\u5fe2<\u5fee<\u5fef<\u5ff3<\u5ffc" +
                "<\u3917<\u6017<\u6022<\u6024<\u391A<\u604c<\u607f<\u608a" +
                "<\u6095<\u60a8<\ud849\udef3<\u60b0<\u60b1<\u60be<\u60c8" +
                "<\u60d9<\u60db<\u60EE<\u60f2<\u60f5<\u6110<\u6112<\u6113" +
                "<\u6119<\u611e<\u613A<\u396F<\u6141<\u6146<\u6160<\u617c" +
                "<\ud84a\udc5b<\u6192<\u6193<\u6197<\u6198<\u61a5<\u61a8" +
                "<\u61ad<\ud84a\udcab<\u61d5<\u61dd<\u61df<\u61F5<\ud84a\udd8f" +
                "<\u6215<\u6223<\u6229<\u6246<\u624c<\u6251<\u6252<\u6261" +
                "<\u6264<\u627B<\u626d<\u6273<\u6299<\u62a6<\u62d5<\ud84a\udeb8" +
                "<\u62fd<\u6303<\u630d<\u6310<\ud84a\udf4f<\ud84a\udf50" +
                "<\u6332<\u6335<\u633B<\u633c<\u6341<\u6344<\u634e<\ud84a\udf46" +
                "<\u6359<\ud84b\udc1d<\ud84a\udfa6<\u636c<\u6384<\u6399" +
                "<\ud84b\udc24<\u6394<\u63bd<\u63F7<\u63d4<\u63d5<\u63dc" +
                "<\u63e0<\u63EB<\u63ec<\u63f2<\u6409<\u641e<\u6425<\u6429" +
                "<\u642f<\u645a<\u645b<\u645d<\u6473<\u647d<\u6487<\u6491" +
                "<\u649d<\u649f<\u64cb<\u64cc<\u64d5<\u64d7<\ud84b\udde1" +
                "<\u64e4<\u64e5<\u64ff<\u6504<\u3A6E<\u650f<\u6514<\u6516" +
                "<\u3A73<\u651e<\u6532<\u6544<\u6554<\u656b<\u657a<\u6581" +
                "<\u6584<\u6585<\u658a<\u65b2<\u65B5<\u65B8<\u65bf<\u65c2" +
                "<\u65c9<\u65d4<\u3AD6<\u65f2<\u65f9<\u65FC<\u6604<\u6608" +
                "<\u6621<\u662a<\u6645<\u6651<\u664e<\u3AEA<\ud84c\uddc3" +
                "<\u6657<\u665b<\u6663<\ud84c\uddf5<\ud84c\uddb6<\u666a" +
                "<\u666b<\u666c<\u666D<\u667b<\u6680<\u6690<\u6692<\u6699" +
                "<\u3B0E<\u66ad<\u66b1<\u66b5<\u3B1A<\u66bf<\u3B1C<\u66ec" +
                "<\u3AD7<\u6701<\u6705<\u6712<\ud84c\udf72<\u6719<\ud84c\udfd3" +
                "<\ud84c\udfd2<\u674c<\u674D<\u6754<\u675d<\ud84c\udfd0" +
                "<\ud84c\udfe4<\ud84c\udfd5<\u6774<\u6776<\ud84c\udfda" +
                "<\u6792<\ud84c\udfdf<\u8363<\u6810<\u67b0<\u67b2<\u67c3" +
                "<\u67c8<\u67d2<\u67d9<\u67DB<\u67f0<\u67f7<\ud84d\udc4a" +
                "<\ud84d\udc51<\ud84d\udc4b<\u6818<\u681f<\u682d<\ud84d\udc65" +
                "<\u6833<\u683b<\u683E<\u6844<\u6845<\u6849<\u684c<\u6855" +
                "<\u6857<\u3B77<\u686b<\u686e<\u687a<\u687c<\u6882<\u6890" +
                "<\u6896<\u3B6D<\u6898<\u6899<\u689a<\u689c<\u68aa<\u68AB" +
                "<\u68B4<\u68bb<\u68fb<\ud84d\udce4<\ud84d\udd5a<\u68C3" +
                "<\u68c5<\u68cc<\u68cf<\u68d6<\u68d9<\u68E4<\u68e5<\u68ec" +
                "<\u68F7<\u6903<\u6907<\u3B87<\u3B88<\ud84d\udd94<\u693b" +
                "<\u3B8D<\u6946<\u6969<\u696c<\u6972<\u697a<\u697f<\u6992" +
                "<\u3BA4<\u6996<\u6998<\u69a6<\u69B0<\u69b7<\u69ba<\u69bc" +
                "<\u69C0<\u69d1<\u69d6<\ud84d\ude39<\ud84d\ude47<\u6a30" +
                "<\ud84d\ude38<\ud84d\ude3a<\u69E3<\u69ee<\u69ef<\u69f3" +
                "<\u3BCD<\u69F4<\u69fe<\u6a11<\u6a1a<\u6a1d<\ud84d\udf1c" +
                "<\u6a32<\u6A33<\u6a34<\u6a3f<\u6a46<\u6a49<\u6A7A<\u6a4e" +
                "<\u6a52<\u6a64<\ud84d\udf0c<\u6a7e<\u6a83<\u6a8b<\u3BF0" +
                "<\u6a91<\u6a9f<\u6AA1<\ud84d\udf64<\u6aab<\u6abd<\u6ac6" +
                "<\u6ad4<\u6ad0<\u6adc<\u6add<\ud84d\udfff<\ud84d\udfe7" +
                "<\u6aec<\u6af1<\u6af2<\u6AF3<\u6afd<\ud84e\udc24<\u6B0B" +
                "<\u6b0f<\u6b10<\u6b11<\ud84e\udc3d<\u6b17<\u3C26<\u6b2f" +
                "<\u6b4a<\u6b58<\u6B6C<\u6b75<\u6B7A<\u6B81<\u6b9b<\u6bae" +
                "<\ud84e\ude98<\u6bbd<\u6bbe<\u6BC7<\u6BC8<\u6bc9<\u6bda" +
                "<\u6be6<\u6be7<\u6bee<\u6bf1<\u6c02<\u6C0A<\u6c0e<\u6c35" +
                "<\u6c36<\u6c3a<\ud84f\udc7f<\u6c3f<\u6c4d<\u6c5b<\u6c6d" +
                "<\u6C84<\u6c89<\u3CC3<\u6c94<\u6c95<\u6c97<\u6CAD<\u6cc2" +
                "<\u6cd0<\u3CD2<\u6cd6<\u6cda<\u6cdc<\u6ce9<\u6cec<\u6CED" +
                "<\ud84f\udd00<\u6D00<\u6d0a<\u6D24<\u6d26<\u6d27<\u6c67" +
                "<\u6d2f<\u6d3c<\u6D5B<\u6d5e<\u6D60<\u6d70<\u6D80<\u6D81" +
                "<\u6D8A<\u6D8D<\u6d91<\u6d98<\ud84f\udd40<\u6E17<\ud84f\uddfa" +
                "<\ud84f\uddf9<\ud84f\uddd3<\u6DAB<\u6DAE<\u6db4<\u6DC2" +
                "<\u6D34<\u6dc8<\u6dce<\u6dcf<\u6DD0<\u6ddf<\u6de9<\u6df6" +
                "<\u6e36<\u6e1e<\u6e22<\u6e27<\u3D11<\u6e32<\u6e3c<\u6e48" +
                "<\u6e49<\u6e4b<\u6E4C<\u6e4f<\u6e51<\u6e53<\u6e54<\u6e57" +
                "<\u6e63<\u3D1E<\u6e93<\u6ea7<\u6EB4<\u6ebf<\u6ec3<\u6eca" +
                "<\u6ED9<\u6F35<\u6eeb<\u6ef9<\u6efb<\u6f0a<\u6f0c<\u6f18" +
                "<\u6F25<\u6f36<\u6f3c<\ud84f\udf7e<\u6f52<\u6f57<\u6f5a" +
                "<\u6F60<\u6f68<\u6F98<\u6f7d<\u6f90<\u6f96<\u6FBE<\u6f9f" +
                "<\u6fa5<\u6faf<\u3D64<\u6fb5<\u6fc8<\u6FC9<\u6fda<\u6fde" +
                "<\u6fe9<\ud850\udc96<\u6ffc<\u7000<\u7007<\u700A<\u7023" +
                "<\ud850\udd03<\u7039<\u703A<\u703c<\u7043<\u7047<\u704b" +
                "<\u3D9A<\u7054<\u7065<\u7069<\u706c<\u706e<\u7076<\u707e" +
                "<\u7081<\u7086<\u7095<\u7097<\u70bb<\ud850\uddc6<\u709F" +
                "<\u70b1<\ud850\uddfe<\u70EC<\u70ca<\u70d1<\u70d3<\u70dc" +
                "<\u7103<\u7104<\u7106<\u7107<\u7108<\u710c<\u3DC0<\u712f" +
                "<\u7131<\u7150<\u714a<\u7153<\u715e<\u3DD4<\u7196<\u7180" +
                "<\u719b<\u71a0<\u71a2<\u71AE<\u71af<\u71b3<\ud850\udfbc" +
                "<\u71cb<\u71d3<\u71d9<\u71dc<\u7207<\u3E05<\u722b<\u7234" +
                "<\u7238<\u7239<\u4E2C<\u7242<\u7253<\u7257<\u7263<\ud851\ude29" +
                "<\u726e<\u726f<\u7278<\u727f<\u728e<\ud851\udea5<\u72ad" +
                "<\u72ae<\u72B0<\u72b1<\u72c1<\u3E60<\u72cc<\u3E66<\u3E68" +
                "<\u72f3<\u72fa<\u7307<\u7312<\u7318<\u7319<\u3E83<\u7339" +
                "<\u732c<\u7331<\u7333<\u733d<\u7352<\u3E94<\u736b<\u736c" +
                "<\ud852\udc96<\u736e<\u736f<\u7371<\u7377<\u7381<\u7385" +
                "<\u738A<\u7394<\u7398<\u739c<\u739e<\u73a5<\u73A8<\u73b5" +
                "<\u73b7<\u73b9<\u73bc<\u73bf<\u73c5<\u73cb<\u73e1<\u73e7" +
                "<\u73f9<\u7413<\u73fa<\u7401<\u7424<\u7431<\u7439<\u7453" +
                "<\u7440<\u7443<\u744d<\u7452<\u745d<\u7471<\u7481<\u7485" +
                "<\u7488<\ud852\ude4d<\u7492<\u7497<\u7499<\u74a0<\u74a1" +
                "<\u74A5<\u74aa<\u74ab<\u74b9<\u74bb<\u74BA<\u74D6<\u74d8" +
                "<\u74de<\u74ef<\u74eb<\ud852\udf56<\u74fa<\ud852\udf6f" +
                "<\u7520<\u7524<\u752a<\u3F57<\ud853\udc16<\u753d<\u753e" +
                "<\u7540<\u7548<\u754e<\u7550<\u7552<\u756C<\u7572<\u7571" +
                "<\u757a<\u757d<\u757e<\u7581<\ud853\udd14<\u758C<\u3F75" +
                "<\u75a2<\u3F77<\u75B0<\u75B7<\u75bf<\u75c0<\u75c6<\u75cf" +
                "<\u75D3<\u75DD<\u75df<\u75e0<\u75e7<\u75ec<\u75ee<\u75f1" +
                "<\u75f9<\u7603<\u7618<\u7607<\u760f<\u3FAE<\ud853\ude0e" +
                "<\u7613<\u761b<\u761c<\ud853\ude37<\u7625<\u7628<\u763c" +
                "<\u7633<\ud853\ude6a<\u3FC9<\u7641<\ud853\ude8b<\u7649" +
                "<\u7655<\u3FD7<\u766e<\u7695<\u769c<\u76A1<\u76a0<\u76a7" +
                "<\u76a8<\u76AF<\ud854\udc4a<\u76c9<\ud854\udc55<\u76e8" +
                "<\u76ec<\ud854\udd22<\u7717<\u771a<\u772d<\u7735<\ud854\udda9" +
                "<\u4039<\ud854\udde5<\ud854\uddcd<\u7758<\u7760<\u776a" +
                "<\ud854\ude1e<\u7772<\u777C<\u777d<\ud854\ude4c<\u4058" +
                "<\u779a<\u779f<\u77a2<\u77A4<\u77A9<\u77de<\u77df<\u77e4" +
                "<\u77e6<\u77ea<\u77ec<\u4093<\u77f0<\u77f4<\u77fb<\ud855\udc2e" +
                "<\u7805<\u7806<\u7809<\u780d<\u7819<\u7821<\u782C<\u7847" +
                "<\u7864<\u786a<\ud855\udcd9<\u788a<\u7894<\u78a4<\u789d" +
                "<\u789e<\u789f<\u78bb<\u78c8<\u78cc<\u78ce<\u78d5<\u78e0" +
                "<\u78e1<\u78e6<\u78F9<\u78fa<\u78fb<\u78FE<\ud855\udda7" +
                "<\u7910<\u791B<\u7930<\u7925<\u793b<\u794a<\u7958<\u795b" +
                "<\u4105<\u7967<\u7972<\u7994<\u7995<\u7996<\u799b<\u79a1" +
                "<\u79a9<\u79b4<\u79bb<\u79c2<\u79c7<\u79CC<\u79CD<\u79d6" +
                "<\u4148<\ud855\udfa9<\ud855\udfb4<\u414F<\u7a0a<\u7a11" +
                "<\u7a15<\u7a1b<\u7a1e<\u4163<\u7a2d<\u7a38<\u7a47<\u7a4c" +
                "<\u7a56<\u7a59<\u7a5c<\u7a5f<\u7a60<\u7a67<\u7a6a<\u7a75" +
                "<\u7a78<\u7a82<\u7a8a<\u7a90<\u7aa3<\u7aac<\ud856\uddd4" +
                "<\u41B4<\u7ab9<\u7abc<\u7ABE<\u41BF<\u7acc<\u7ad1<\u7ae7" +
                "<\u7ae8<\u7af4<\ud856\udee4<\ud856\udee3<\u7b07<\ud856\udef1" +
                "<\u7b3d<\u7b27<\u7b2a<\u7b2e<\u7b2f<\u7b31<\u41E6<\u41F3" +
                "<\u7B7F<\u7b41<\u41EE<\u7b55<\u7B79<\u7b64<\u7b66<\u7b69" +
                "<\u7b73<\ud856\udfb2<\u4207<\u7b90<\u7b91<\u7b9b<\u420E" +
                "<\u7baf<\u7bb5<\u7bbc<\u7bc5<\u7bca<\ud857\udc4b<\ud857\udc64" +
                "<\u7bd4<\u7bd6<\u7bda<\u7bea<\u7BF0<\u7c03<\u7c0b<\u7c0e" +
                "<\u7c0f<\u7c26<\u7C45<\u7c4a<\u7c51<\u7C57<\u7c5e<\u7c61" +
                "<\u7c69<\u7c6e<\u7C6F<\u7c70<\ud857\ude2e<\ud857\ude56" +
                "<\ud857\ude65<\u7ca6<\ud857\ude62<\u7cb6<\u7cb7<\u7cbf" +
                "<\ud857\uded8<\u7cc4<\ud857\udec2<\u7cc8<\u7ccd<\ud857\udee8" +
                "<\u7cd7<\ud857\udf23<\u7ce6<\u7ceb<\ud857\udf5c<\u7cf5" +
                "<\u7d03<\u7d09<\u42C6<\u7d12<\u7d1e<\ud857\udfe0<\ud857\udfd4" +
                "<\u7d3d<\u7d3e<\u7d40<\u7d47<\ud858\udc0c<\ud857\udffb" +
                "<\u42D6<\u7d59<\u7d5a<\u7d6a<\u7d70<\u42DD<\u7d7f<\ud858\udc17" +
                "<\u7d86<\u7d88<\u7d8c<\u7d97<\ud858\udc60<\u7d9d<\u7da7" +
                "<\u7daa<\u7db6<\u7db7<\u7DC0<\u7dd7<\u7dd9<\u7de6<\u7df1" +
                "<\u7df9<\u4302<\ud858\udced<\u7e10<\u7e17<\u7e1d<\u7e20" +
                "<\u7e27<\u7e2c<\u7e45<\u7e73<\u7E75<\u7e7e<\u7e86<\u7e87" +
                "<\u432B<\u7e91<\u7e98<\u7e9a<\u4343<\u7f3c<\u7f3b<\u7f3e" +
                "<\u7f43<\u7f44<\u7f4f<\u34C1<\ud858\ude70<\u7f52<\ud858\ude86" +
                "<\u7f61<\u7f63<\u7f64<\u7f6d<\u7f7d<\u7f7e<\ud858\udf4c" +
                "<\u7f90<\u517B<\ud84f\udd0e<\u7f96<\u7f9c<\u7fad<\ud859\udc02" +
                "<\u7fc3<\u7fcf<\u7fe3<\u7fe5<\u7fef<\u7ff2<\u8002<\u800a" +
                "<\u8008<\u800e<\u8011<\u8016<\u8024<\u802c<\u8030<\u8043" +
                "<\u8066<\u8071<\u8075<\u807B<\u8099<\u809c<\u80A4<\u80a7" +
                "<\u80b8<\ud859\ude7e<\u80C5<\u80d5<\u80d8<\u80E6<\ud859\udeb0" +
                "<\u810D<\u80F5<\u80FB<\u43EE<\u8135<\u8116<\u811e<\u43F0" +
                "<\u8124<\u8127<\u812c<\ud859\udf1d<\u813D<\u4408<\u8169" +
                "<\u4417<\u8181<\u441C<\u8184<\u8185<\u4422<\u8198<\u81b2" +
                "<\u81C1<\u81c3<\u81D6<\u81db<\ud85a\udcdd<\u81e4<\ud85a\udcea" +
                "<\u81ec<\ud85a\udd51<\u81fd<\u81ff<\ud85a\udd6f<\u8204" +
                "<\ud85a\udddd<\u8219<\u8221<\u8222<\ud85a\ude1e<\u8232" +
                "<\u8234<\u823C<\u8246<\u8249<\u8245<\ud85a\ude58<\u824b" +
                "<\u4476<\u824f<\u447A<\u8257<\ud85a\ude8c<\u825c<\u8263" +
                "<\ud85a\udeb7<\u8279<\u4491<\u827d<\u827f<\u8283<\u828a" +
                "<\u8293<\u82a7<\u82a8<\u82b2<\u82b4<\u82ba<\u82bc<\u82e2" +
                "<\u82e8<\u82f7<\u8307<\u8308<\u830C<\u8354<\u831b<\u831d" +
                "<\u8330<\u833c<\u8344<\u8357<\u44BE<\u837f<\u44D4<\u44B3" +
                "<\u838d<\u8394<\u8395<\u839b<\u839d<\u83c9<\u83d0<\u83d4" +
                "<\u83dd<\u83E5<\u83f9<\u840f<\u8411<\u8415<\ud85b\udc73" +
                "<\u8417<\u8439<\u844a<\u844f<\u8451<\u8452<\u8459<\u845a" +
                "<\u845c<\ud85b\udcdd<\u8465<\u8476<\u8478<\u847c<\u8481" +
                "<\u450D<\u84dc<\u8497<\u84a6<\u84be<\u4508<\u84CE<\u84cf" +
                "<\u84d3<\ud85b\ude65<\u84e7<\u84ea<\u84ef<\u84f0<\u84f1" +
                "<\u84fa<\u84fd<\u850c<\u851B<\u8524<\u8525<\u852b<\u8534" +
                "<\u854f<\u856f<\u4525<\u4543<\u853E<\u8551<\u8553<\u855e" +
                "<\u8561<\u8562<\ud85b\udf94<\u857b<\u857d<\u857f<\u8581" +
                "<\u8586<\u8593<\u859d<\u859f<\ud85b\udff8<\ud85b\udff6" +
                "<\ud85b\udff7<\u85b7<\u85bc<\u85c7<\u85ca<\u85d8<\u85D9" +
                "<\u85df<\u85E1<\u85e6<\u85f6<\u8600<\u8611<\u861e<\u8621" +
                "<\u8624<\u8627<\ud85c\udd0d<\u8639<\u863c<\ud85c\udd39" +
                "<\u8640<\u8653<\u8656<\u866f<\u8677<\u867a<\u8687<\u8689" +
                "<\u868d<\u8691<\u869c<\u869D<\u86a8<\u86b1<\u86b3<\u86c1" +
                "<\u86c3<\u86d1<\u86d5<\u86d7<\u86e3<\u86E6<\u45B8<\u8705" +
                "<\u8707<\u870e<\u8710<\u8713<\u8719<\u871f<\u8721<\u8723" +
                "<\u8731<\u873a<\u873e<\u8740<\u8743<\u8751<\u8758<\u8764" +
                "<\u8765<\u8772<\u877C<\ud85c\udfdb<\ud85c\udfda<\u87a7" +
                "<\u8789<\u878b<\u8793<\u87a0<\ud85c\udffe<\u45E5<\u87be" +
                "<\ud85d\udc10<\u87c1<\u87ce<\u87F5<\u87df<\ud85d\udc49" +
                "<\u87e3<\u87E5<\u87E6<\u87ea<\u87eb<\u87ed<\u8801<\u8803" +
                "<\u880b<\u8813<\u8828<\u882e<\u8832<\u883c<\u460F<\u884a" +
                "<\u8858<\u885f<\u8864<\ud85d\ude15<\ud85d\ude14<\u8869" +
                "<\ud85d\ude31<\u886F<\u88a0<\u88BC<\u88bd<\u88be<\u88c0" +
                "<\u88d2<\ud85d\ude93<\u88d1<\u88d3<\u88db<\u88f0<\u88f1" +
                "<\u4641<\u8901<\ud85d\udf0e<\u8937<\ud85d\udf23<\u8942" +
                "<\u8945<\u8949<\ud85d\udf52<\u4665<\u8962<\u8980<\u8989" +
                "<\u8990<\u899f<\u89b0<\u89b7<\u89d6<\u89d8<\u89eb<\u46A1" +
                "<\u89f1<\u89f3<\u89fd<\u89ff<\u46AF<\u8a11<\u8a14<\ud85e\udd85" +
                "<\u8A21<\u8a35<\u8a3e<\u8a45<\u8a4d<\u8a58<\u8aae<\u8a90" +
                "<\u8ab7<\u8abe<\u8ad7<\u8afc<\ud85e\ude84<\u8b0a<\u8b05" +
                "<\u8B0D<\u8b1c<\u8b1f<\u8b2d<\u8b43<\u470C<\u8B51<\u8b5e" +
                "<\u8b76<\u8b7f<\u8b81<\u8b8b<\u8b94<\u8b95<\u8b9c<\u8b9e" +
                "<\u8c39<\ud85e\udfb3<\u8c3d<\ud85e\udfbe<\ud85e\udfc7" +
                "<\u8c45<\u8c47<\u8c4f<\u8c54<\u8c57<\u8c69<\u8c6d<\u8c73" +
                "<\ud85f\udcb8<\u8c93<\u8c92<\u8c99<\u4764<\u8c9b<\u8ca4" +
                "<\u8cd6<\u8cd5<\u8cd9<\ud85f\udda0<\u8cf0<\u8CF1<\ud85f\ude10" +
                "<\u8d09<\u8D0E<\u8d6c<\u8d84<\u8d95<\u8da6<\ud85f\udfb7" +
                "<\u8dc6<\u8dc8<\u8dd9<\u8dec<\u8E0C<\u47FD<\u8dfd<\u8e06" +
                "<\ud860\udc8a<\u8e14<\u8e16<\u8e21<\u8e22<\u8e27<\ud860\udcbb" +
                "<\u4816<\u8e36<\u8e39<\u8e4b<\u8e54<\u8e62<\u8e6c<\u8e6d" +
                "<\u8e6f<\u8E98<\u8e9e<\u8eae<\u8eb3<\u8eb5<\u8EB6<\u8ebb" +
                "<\ud860\ude82<\u8ed1<\u8ed4<\u484E<\u8ef9<\ud860\udef3" +
                "<\u8f00<\u8f08<\u8f17<\u8F2B<\u8f40<\u8F4A<\u8f58<\ud861\udc0c" +
                "<\u8fa4<\u8FB4<\u8fb6<\ud861\udc55<\u8fc1<\u8fc6<\u8fca" +
                "<\u8fcd<\u8fd3<\u8fd5<\u8fe0<\u8ff1<\u8ff5<\u8ffb<\u9002" +
                "<\u900c<\u9037<\ud861\udd6b<\u9043<\u9044<\u905d<\ud861\uddc8" +
                "<\ud861\uddc9<\u9085<\u908c<\u9090<\u961d<\u90a1<\u48B5" +
                "<\u90b0<\u90b6<\u90c3<\u90c8<\ud861\uded7<\u90dc<\u90df" +
                "<\ud861\udefa<\u90f6<\u90F2<\u9100<\u90eb<\u90fe<\u90ff" +
                "<\u9104<\u9106<\u9118<\u911c<\u911e<\u9137<\u9139<\u913a" +
                "<\u9146<\u9147<\u9157<\u9159<\u9161<\u9164<\u9174<\u9179" +
                "<\u9185<\u918e<\u91a8<\u91ae<\u91b3<\u91b6<\u91c3<\u91C4" +
                "<\u91da<\ud862\udd49<\ud862\udd46<\u91ec<\u91ee<\u9201" +
                "<\u920a<\u9216<\u9217<\ud862\udd6b<\u9233<\u9242<\u9247" +
                "<\u924a<\u924e<\u9251<\u9256<\u9259<\u9260<\u9261<\u9265" +
                "<\u9267<\u9268<\ud862\udd87<\ud862\udd88<\u927c<\u927d" +
                "<\u927f<\u9289<\u928d<\u9297<\u9299<\u929f<\u92a7<\u92ab" +
                "<\ud862\uddba<\ud862\uddbb<\u92b2<\u92bf<\u92c0<\u92c6" +
                "<\u92ce<\u92d0<\u92d7<\u92d9<\u92e5<\u92e7<\u9311<\ud862\ude1e" +
                "<\ud862\ude29<\u92F7<\u92f9<\u92fb<\u9302<\u930d<\u9315" +
                "<\u931d<\u931e<\u9327<\u9329<\ud862\ude71<\ud862\ude43" +
                "<\u9347<\u9351<\u9357<\u935a<\u936B<\u9371<\u9373<\u93a1" +
                "<\ud862\ude99<\ud862\udecd<\u9388<\u938b<\u938f<\u939e" +
                "<\u93F5<\ud862\udee4<\ud862\udedd<\u93F1<\u93c1<\u93c7" +
                "<\u93dc<\u93e2<\u93e7<\u9409<\u940f<\u9416<\u9417<\u93fb" +
                "<\u9432<\u9434<\u943b<\u9445<\ud862\udfc1<\ud862\udfef" +
                "<\u946d<\u946f<\u9578<\u9579<\u9586<\u958c<\u958d<\ud863\udd10" +
                "<\u95ab<\u95b4<\ud863\udd71<\u95c8<\ud863\uddfb<\ud863\ude1f" +
                "<\u962c<\u9633<\u9634<\ud863\ude36<\u963c<\u9641<\u9661" +
                "<\ud863\ude89<\u9682<\ud863\udeeb<\u969a<\ud863\udf32" +
                "<\u49E7<\u96a9<\u96af<\u96b3<\u96ba<\u96BD<\u49FA<\ud863\udff8" +
                "<\u96d8<\u96da<\u96dd<\u4A04<\u9714<\u9723<\u4A29<\u9736" +
                "<\u9741<\u9747<\u9755<\u9757<\u975b<\u976a<\ud864\udea0" +
                "<\ud864\udeb1<\u9796<\u979a<\u979e<\u97a2<\u97b1<\u97b2" +
                "<\u97be<\u97cc<\u97d1<\u97d4<\u97d8<\u97d9<\u97e1<\u97f1" +
                "<\u9804<\u980d<\u980e<\u9814<\u9816<\u4ABC<\ud865\udc90" +
                "<\u9823<\u9832<\u9833<\u9825<\u9847<\u9866<\u98ab<\u98ad" +
                "<\u98b0<\ud865\uddcf<\u98b7<\u98b8<\u98bb<\u98BC<\u98bf" +
                "<\u98c2<\u98C7<\u98CB<\u98E0<\ud865\ude7f<\u98e1<\u98e3" +
                "<\u98e5<\u98ea<\u98F0<\u98F1<\u98f3<\u9908<\u4B3B<\ud865\udef0" +
                "<\u9916<\u9917<\ud865\udf19<\u991a<\u991b<\u991c<\ud865\udf50" +
                "<\u9931<\u9932<\u9933<\u993a<\u993b<\u993c<\u9940<\u9941" +
                "<\u9946<\u994d<\u994e<\u995c<\u995f<\u9960<\u99A3<\u99a6" +
                "<\u99b9<\u99bd<\u99bf<\u99c3<\u99c9<\u99d4<\u99d9<\u99de" +
                "<\ud866\udcc6<\u99f0<\u99f9<\u99FC<\u9A0A<\u9a11<\u9a16" +
                "<\u9A1A<\u9a20<\u9A31<\u9a36<\u9a44<\u9a4c<\u9A58<\u4BC2" +
                "<\u9aaf<\u4BCA<\u9AB7<\u4BD2<\u9ab9<\ud866\ude72<\u9ac6" +
                "<\u9ad0<\u9ad2<\u9ad5<\u4BE8<\u9adc<\u9ae0<\u9ae5<\u9ae9" +
                "<\u9b03<\u9b0c<\u9b10<\u9b12<\u9b16<\u9b1c<\u9b2b<\u9b33" +
                "<\u9b3d<\u4C20<\u9b4b<\u9b63<\u9b65<\u9b6b<\u9b6c<\u9b73" +
                "<\u9B76<\u9b77<\u9ba6<\u9bac<\u9bb1<\ud867\udddb<\ud867\ude3d" +
                "<\u9bb2<\u9bb8<\u9bbe<\u9bc7<\u9bf3<\u9bd8<\u9bdd<\u9be7" +
                "<\u9bea<\u9beb<\u9bef<\u9BEE<\ud867\ude15<\u9bfa<\ud867\ude8a" +
                "<\u9bf7<\ud867\ude49<\u9c16<\u9c18<\u9c19<\u9c1a<\u9C1D" +
                "<\u9c22<\u9c27<\u9c29<\u9c2a<\ud867\udec4<\u9c31<\u9c36" +
                "<\u9c37<\u9c45<\u9c5c<\ud867\udee9<\u9c49<\u9c4a<\ud867\udedb" +
                "<\u9c54<\u9c58<\u9c5b<\u9c5d<\u9c5f<\u9c69<\u9c6a<\u9c6b" +
                "<\u9C6D<\u9c6e<\u9c70<\u9c72<\u9c75<\u9C7A<\u9ce6<\u9cf2" +
                "<\u9d0b<\u9d02<\ud867\udfce<\u9d11<\u9d17<\u9d18<\ud868\udc2f" +
                "<\u4CC4<\ud868\udc1a<\u9d32<\u4CD1<\u9d42<\u9d4a<\u9d5f" +
                "<\u9d62<\ud868\udcf9<\u9d69<\u9d6b<\ud868\udc82<\u9D73" +
                "<\u9d76<\u9d77<\u9d7e<\u9d84<\u9d8d<\u9D99<\u9da1<\u9dbf" +
                "<\u9db5<\u9db9<\u9DBD<\u9dc3<\u9dc7<\u9dc9<\u9dd6<\u9dda" +
                "<\u9ddf<\u9de0<\u9DE3<\u9df4<\u4D07<\u9e0a<\u9e02<\u9E0D" +
                "<\u9e19<\u9e1c<\u9e1d<\u9e7b<\ud848\ude18<\u9e80<\u9e85" +
                "<\u9e9b<\u9ea8<\ud868\udf8c<\u9EBD<\ud869\udc37<\u9edf" +
                "<\u9ee7<\u9eee<\u9eff<\u9f02<\u4D77<\u9f03<\u9f17<\u9f19" +
                "<\u9f2f<\u9f37<\u9f3a<\u9f3d<\u9f41<\u9f45<\u9f46<\u9f53" +
                "<\u9f55<\u9f58<\ud869\uddf1<\u9f5d<\ud869\ude02<\u9f69" +
                "<\ud869\ude1a<\u9f6d<\u9f70<\u9f75<\ud869\udeb2"
            }
        };
    }
}
