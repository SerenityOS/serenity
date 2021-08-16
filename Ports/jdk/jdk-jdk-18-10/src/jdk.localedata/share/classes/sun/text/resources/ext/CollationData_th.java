/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright IBM Corp. 1996 - 1998 - All Rights Reserved
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

public class CollationData_th extends ListResourceBundle {

    protected final Object[][] getContents() {
        return new Object[][] {
            { "Rule",
                "! "                            // First turn on the SE Asian Vowel/Consonant
                                                // swapping rule
                + "& Z "                        // Put in all of the consonants, after Z
                + "< \u0E01 "                   //  KO KAI
                + "< \u0E02 "                   //  KHO KHAI
                + "< \u0E03 "                   //  KHO KHUAT
                + "< \u0E04 "                   //  KHO KHWAI
                + "< \u0E05 "                   //  KHO KHON
                + "< \u0E06 "                   //  KHO RAKHANG
                + "< \u0E07 "                   //  NGO NGU
                + "< \u0E08 "                   //  CHO CHAN
                + "< \u0E09 "                   //  CHO CHING
                + "< \u0E0A "                   //  CHO CHANG
                + "< \u0E0B "                   //  SO SO
                + "< \u0E0C "                   //  CHO CHOE
                + "< \u0E0D "                   //  YO YING
                + "< \u0E0E "                   //  DO CHADA
                + "< \u0E0F "                   //  TO PATAK
                + "< \u0E10 "                   //  THO THAN
                + "< \u0E11 "                   //  THO NANGMONTHO
                + "< \u0E12 "                   //  THO PHUTHAO
                + "< \u0E13 "                   //  NO NEN
                + "< \u0E14 "                   //  DO DEK
                + "< \u0E15 "                   //  TO TAO
                + "< \u0E16 "                   //  THO THUNG
                + "< \u0E17 "                   //  THO THAHAN
                + "< \u0E18 "                   //  THO THONG
                + "< \u0E19 "                   //  NO NU
                + "< \u0E1A "                   //  BO BAIMAI
                + "< \u0E1B "                   //  PO PLA
                + "< \u0E1C "                   //  PHO PHUNG
                + "< \u0E1D "                   //  FO FA
                + "< \u0E1E "                   //  PHO PHAN
                + "< \u0E1F "                   //  FO FAN
                + "< \u0E20 "                   //  PHO SAMPHAO
                + "< \u0E21 "                   //  MO MA
                + "< \u0E22 "                   //  YO YAK
                + "< \u0E23 "                   //  RO RUA
                + "< \u0E24 "                   //  RU
                + "< \u0E25 "                   //  LO LING
                + "< \u0E26 "                   //  LU
                + "< \u0E27 "                   //  WO WAEN
                + "< \u0E28 "                   //  SO SALA
                + "< \u0E29 "                   //  SO RUSI
                + "< \u0E2A "                   //  SO SUA
                + "< \u0E2B "                   //  HO HIP
                + "< \u0E2C "                   //  LO CHULA
                + "< \u0E2D "                   //  O ANG
                + "< \u0E2E "                   //  HO NOKHUK

                //
                // Normal vowels
                //
                + "< \u0E4D "                   //  NIKHAHIT
                + "< \u0E30 "                   //  SARA A
                + "< \u0E31 "                   //  MAI HAN-AKAT
                + "< \u0E32 "                   //  SARA AA

                // Normalizer will decompose this character to \u0e4d\u0e32.
                + "< \u0E33 = \u0E4D\u0E32 "                   //  SARA AM

                + "< \u0E34 "                   //  SARA I

                + "< \u0E35 "                   //  SARA II
                + "< \u0E36 "                   //  SARA UE
                + "< \u0E37 "                   //  SARA UEE
                + "< \u0E38 "                   //  SARA U
                + "< \u0E39 "                   //  SARA UU

                //
                // Preceding vowels
                //
                + "< \u0E40 "                   //  SARA E
                + "< \u0E41 "                   //  SARA AE
                + "< \u0E42 "                   //  SARA O
                + "< \u0E43 "                   //  SARA AI MAIMUAN
                + "< \u0E44 "                   //  SARA AI MAIMALAI


                //according to CLDR, it's after 0e44
                + "< \u0E3A "                   //  PHINTHU



                // This rare symbol comes after all characters.
                + "< \u0E45 "                   //  LAKKHANGYAO
                + "& \u0E32 , \0E45 "           // According to CLDR, 0E45 is after 0E32 in tertiary level




                // Below are thai puntuation marks and Tonal(Accent) marks. According to CLDR 1.9 and
                // ISO/IEC 14651, Annex C, C.2.1 Thai ordering principles, 0E2F to 0E5B are punctuaion marks that need to be ignored
                // in the first three leveles.  0E4E to 0E4B are tonal marks to be compared in secondary level.
                // In real implementation, set punctuation marks in tertiary as there is no fourth level in Java.
                // Set all these special marks after \u0301, the accute accent.
                + "& \u0301 "   // acute accent

                //punctuation marks
                + ", \u0E2F "                   //  PAIYANNOI      (ellipsis, abbreviation)
                + ", \u0E46 "                   //  MAIYAMOK
                + ", \u0E4F "                   //  FONGMAN
                + ", \u0E5A "                   //  ANGKHANKHU
                + ", \u0E5B "                   //  KHOMUT

                //tonal marks
                + "; \u0E4E "                   //  YAMAKKAN
                + "; \u0E4C "                   //  THANTHAKHAT
                + "; \u0E47 "                   //  MAITAIKHU
                + "; \u0E48 "                   //  MAI EK
                + "; \u0E49 "                   //  MAI THO
                + "; \u0E4A "                   //  MAI TRI
                + "; \u0E4B "                   //  MAI CHATTAWA

                //
                // Digits are equal to their corresponding Arabic digits in the first level
                //
                + "& 0 = \u0E50 "                   //  DIGIT ZERO
                + "& 1 = \u0E51 "                   //  DIGIT ONE
                + "& 2 = \u0E52 "                   //  DIGIT TWO
                + "& 3 = \u0E53 "                   //  DIGIT THREE
                + "& 4 = \u0E54 "                   //  DIGIT FOUR
                + "& 5 = \u0E55 "                   //  DIGIT FIVE
                + "& 6 = \u0E56 "                   //  DIGIT SIX
                + "& 7 = \u0E57 "                   //  DIGIT SEVEN
                + "& 8 = \u0E58 "                   //  DIGIT EIGHT
                + "& 9 = \u0E59 "                   //  DIGIT NINE


            }
        };
    }
}
