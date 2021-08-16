/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
/**
 *******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
// CHANGELOG
//      2005-05-19 Edward Wang
//          - copy this file from icu4jsrc_3_2/src/com/ibm/icu/lang/UCharacterEnums.java
//          - move from package com.ibm.icu.lang to package sun.net.idn
//
//      2011-09-06 Kurchi Subhra Hazra
//          - Added @Deprecated tag to the following:
//            - class UCharacterEnums
//            - interfaces ECharacterCategory, ECharacterDirection
//            - fields INITIAL_QUOTE_PUNCTUATION, FINAL_QUOTE_PUNCTUATION,
//              DIRECTIONALITY_LEFT_TO_RIGHT, DIRECTIONALITY_RIGHT_TO_LEFT,
//              DIRECTIONALITY_EUROPEAN_NUMBER, DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR
//              DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR, DIRECTIONALITY_ARABIC_NUMBER,
//              DIRECTIONALITY_COMMON_NUMBER_SEPARATOR, DIRECTIONALITY_PARAGRAPH_SEPARATOR,
//              DIRECTIONALITY_SEGMENT_SEPARATOR, DIRECTIONALITY_WHITESPACE,
//              DIRECTIONALITY_OTHER_NEUTRALS, DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING,
//              DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE, DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC,
//              DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING, DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE,
//              DIRECTIONALITY_POP_DIRECTIONAL_FORMAT, DIRECTIONALITY_NON_SPACING_MARK,
//              DIRECTIONALITY_BOUNDARY_NEUTRAL, DIRECTIONALITY_UNDEFINED
//

package jdk.internal.icu.lang;

/**
 * A container for the different 'enumerated types' used by UCharacter.
 * @draft ICU 3.0
 * @deprecated This is a draft API and might change in a future release of ICU.
 */

@Deprecated
class UCharacterEnums {

    /** This is just a namespace, it is not instantiatable. */
    private UCharacterEnums() {};

    /**
     * 'Enum' for the CharacterCategory constants.  These constants are
     * compatible in name <b>but not in value</b> with those defined in
     * <code>java.lang.Character</code>.
     * @see UCharacterCategory
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    @Deprecated
    public static interface ECharacterCategory {
        /**
         * Unassigned character type
         * @stable ICU 2.1
         */
        public static final int UNASSIGNED              = 0;

        /**
         * Character type Cn
         * Not Assigned (no characters in [UnicodeData.txt] have this property)
         * @stable ICU 2.6
         */
        public static final int GENERAL_OTHER_TYPES     = 0;

        /**
         * Character type Lu
         * @stable ICU 2.1
         */
        public static final int UPPERCASE_LETTER        = 1;

        /**
         * Character type Ll
         * @stable ICU 2.1
         */
        public static final int LOWERCASE_LETTER        = 2;

        /**
         * Character type Lt
         * @stable ICU 2.1
         */

        public static final int TITLECASE_LETTER        = 3;

        /**
         * Character type Lm
         * @stable ICU 2.1
         */
        public static final int MODIFIER_LETTER         = 4;

        /**
         * Character type Lo
         * @stable ICU 2.1
         */
        public static final int OTHER_LETTER            = 5;

        /**
         * Character type Mn
         * @stable ICU 2.1
         */
        public static final int NON_SPACING_MARK        = 6;

        /**
         * Character type Me
         * @stable ICU 2.1
         */
        public static final int ENCLOSING_MARK          = 7;

        /**
         * Character type Mc
         * @stable ICU 2.1
         */
        public static final int COMBINING_SPACING_MARK  = 8;

        /**
         * Character type Nd
         * @stable ICU 2.1
         */
        public static final int DECIMAL_DIGIT_NUMBER    = 9;

        /**
         * Character type Nl
         * @stable ICU 2.1
         */
        public static final int LETTER_NUMBER           = 10;

        /**
         * Character type No
         * @stable ICU 2.1
         */
        public static final int OTHER_NUMBER            = 11;

        /**
         * Character type Zs
         * @stable ICU 2.1
         */
        public static final int SPACE_SEPARATOR         = 12;

        /**
         * Character type Zl
         * @stable ICU 2.1
         */
        public static final int LINE_SEPARATOR          = 13;

        /**
         * Character type Zp
         * @stable ICU 2.1
         */
        public static final int PARAGRAPH_SEPARATOR     = 14;

        /**
         * Character type Cc
         * @stable ICU 2.1
         */
        public static final int CONTROL                 = 15;

        /**
         * Character type Cf
         * @stable ICU 2.1
         */
        public static final int FORMAT                  = 16;

        /**
         * Character type Co
         * @stable ICU 2.1
         */
        public static final int PRIVATE_USE             = 17;

        /**
         * Character type Cs
         * @stable ICU 2.1
         */
        public static final int SURROGATE               = 18;

        /**
         * Character type Pd
         * @stable ICU 2.1
         */
        public static final int DASH_PUNCTUATION        = 19;

        /**
         * Character type Ps
         * @stable ICU 2.1
         */
        public static final int START_PUNCTUATION       = 20;

        /**
         * Character type Pe
         * @stable ICU 2.1
         */
        public static final int END_PUNCTUATION         = 21;

        /**
         * Character type Pc
         * @stable ICU 2.1
         */
        public static final int CONNECTOR_PUNCTUATION   = 22;

        /**
         * Character type Po
         * @stable ICU 2.1
         */
        public static final int OTHER_PUNCTUATION       = 23;

        /**
         * Character type Sm
         * @stable ICU 2.1
         */
        public static final int MATH_SYMBOL             = 24;

        /**
         * Character type Sc
         * @stable ICU 2.1
         */
        public static final int CURRENCY_SYMBOL         = 25;

        /**
         * Character type Sk
         * @stable ICU 2.1
         */
        public static final int MODIFIER_SYMBOL         = 26;

        /**
         * Character type So
         * @stable ICU 2.1
         */
        public static final int OTHER_SYMBOL            = 27;

        /**
         * Character type Pi
         * @see #INITIAL_QUOTE_PUNCTUATION
         * @stable ICU 2.1
         */
        public static final int INITIAL_PUNCTUATION     = 28;

        /**
         * Character type Pi
         * This name is compatible with java.lang.Character's name for this type.
         * @see #INITIAL_PUNCTUATION
         * @draft ICU 2.8
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final int INITIAL_QUOTE_PUNCTUATION = 28;

        /**
         * Character type Pf
         * @see #FINAL_QUOTE_PUNCTUATION
         * @stable ICU 2.1
         */
        public static final int FINAL_PUNCTUATION       = 29;

        /**
         * Character type Pf
         * This name is compatible with java.lang.Character's name for this type.
         * @see #FINAL_PUNCTUATION
         * @draft ICU 2.8
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final int FINAL_QUOTE_PUNCTUATION   = 29;

        /**
         * Character type count
         * @stable ICU 2.1
         */
        public static final int CHAR_CATEGORY_COUNT     = 30;
    }

    /**
     * 'Enum' for the CharacterDirection constants.  There are two sets
     * of names, those used in ICU, and those used in the JDK.  The
     * JDK constants are compatible in name <b>but not in value</b>
     * with those defined in <code>java.lang.Character</code>.
     * @see UCharacterDirection
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */

    @Deprecated
    public static interface ECharacterDirection {
        /**
         * Directional type L
         * @stable ICU 2.1
         */
        public static final int LEFT_TO_RIGHT              = 0;

        /**
         * JDK-compatible synonum for LEFT_TO_RIGHT.
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final byte DIRECTIONALITY_LEFT_TO_RIGHT = (byte)LEFT_TO_RIGHT;

        /**
         * Directional type R
         * @stable ICU 2.1
         */
        public static final int RIGHT_TO_LEFT              = 1;

        /**
         * JDK-compatible synonum for RIGHT_TO_LEFT.
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final byte DIRECTIONALITY_RIGHT_TO_LEFT = (byte)RIGHT_TO_LEFT;

        /**
         * Directional type EN
         * @stable ICU 2.1
         */
        public static final int EUROPEAN_NUMBER            = 2;

        /**
         * JDK-compatible synonum for EUROPEAN_NUMBER.
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final byte DIRECTIONALITY_EUROPEAN_NUMBER = (byte)EUROPEAN_NUMBER;

        /**
         * Directional type ES
         * @stable ICU 2.1
         */
        public static final int EUROPEAN_NUMBER_SEPARATOR  = 3;

        /**
         * JDK-compatible synonum for EUROPEAN_NUMBER_SEPARATOR.
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR = (byte)EUROPEAN_NUMBER_SEPARATOR;

        /**
         * Directional type ET
         * @stable ICU 2.1
         */
        public static final int EUROPEAN_NUMBER_TERMINATOR = 4;

        /**
         * JDK-compatible synonum for EUROPEAN_NUMBER_TERMINATOR.
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR = (byte)EUROPEAN_NUMBER_TERMINATOR;

        /**
         * Directional type AN
         * @stable ICU 2.1
         */
        public static final int ARABIC_NUMBER              = 5;

        /**
         * JDK-compatible synonum for ARABIC_NUMBER.
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final byte DIRECTIONALITY_ARABIC_NUMBER = (byte)ARABIC_NUMBER;

        /**
         * Directional type CS
         * @stable ICU 2.1
         */
        public static final int COMMON_NUMBER_SEPARATOR    = 6;

        /**
         * JDK-compatible synonum for COMMON_NUMBER_SEPARATOR.
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final byte DIRECTIONALITY_COMMON_NUMBER_SEPARATOR = (byte)COMMON_NUMBER_SEPARATOR;

        /**
         * Directional type B
         * @stable ICU 2.1
         */
        public static final int BLOCK_SEPARATOR            = 7;

        /**
         * JDK-compatible synonum for BLOCK_SEPARATOR.
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final byte DIRECTIONALITY_PARAGRAPH_SEPARATOR = (byte)BLOCK_SEPARATOR;

        /**
         * Directional type S
         * @stable ICU 2.1
         */
        public static final int SEGMENT_SEPARATOR          = 8;

        /**
         * JDK-compatible synonum for SEGMENT_SEPARATOR.
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final byte DIRECTIONALITY_SEGMENT_SEPARATOR = (byte)SEGMENT_SEPARATOR;

        /**
         * Directional type WS
         * @stable ICU 2.1
         */
        public static final int WHITE_SPACE_NEUTRAL        = 9;

        /**
         * JDK-compatible synonum for WHITE_SPACE_NEUTRAL.
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final byte DIRECTIONALITY_WHITESPACE = (byte)WHITE_SPACE_NEUTRAL;

        /**
         * Directional type ON
         * @stable ICU 2.1
         */
        public static final int OTHER_NEUTRAL              = 10;

        /**
         * JDK-compatible synonum for OTHER_NEUTRAL.
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final byte DIRECTIONALITY_OTHER_NEUTRALS = (byte)OTHER_NEUTRAL;

        /**
         * Directional type LRE
         * @stable ICU 2.1
         */
        public static final int LEFT_TO_RIGHT_EMBEDDING    = 11;

        /**
         * JDK-compatible synonum for LEFT_TO_RIGHT_EMBEDDING.
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING = (byte)LEFT_TO_RIGHT_EMBEDDING;

        /**
         * Directional type LRO
         * @stable ICU 2.1
         */
        public static final int LEFT_TO_RIGHT_OVERRIDE     = 12;

        /**
         * JDK-compatible synonum for LEFT_TO_RIGHT_OVERRIDE.
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE = (byte)LEFT_TO_RIGHT_OVERRIDE;

        /**
         * Directional type AL
         * @stable ICU 2.1
         */
        public static final int RIGHT_TO_LEFT_ARABIC       = 13;

        /**
         * JDK-compatible synonum for RIGHT_TO_LEFT_ARABIC.
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC = (byte)RIGHT_TO_LEFT_ARABIC;

        /**
         * Directional type RLE
         * @stable ICU 2.1
         */
        public static final int RIGHT_TO_LEFT_EMBEDDING    = 14;

        /**
         * JDK-compatible synonum for RIGHT_TO_LEFT_EMBEDDING.
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING = (byte)RIGHT_TO_LEFT_EMBEDDING;

        /**
         * Directional type RLO
         * @stable ICU 2.1
         */
        public static final int RIGHT_TO_LEFT_OVERRIDE     = 15;

        /**
         * JDK-compatible synonum for RIGHT_TO_LEFT_OVERRIDE.
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE = (byte)RIGHT_TO_LEFT_OVERRIDE;

        /**
         * Directional type PDF
         * @stable ICU 2.1
         */
        public static final int POP_DIRECTIONAL_FORMAT     = 16;

        /**
         * JDK-compatible synonum for POP_DIRECTIONAL_FORMAT.
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final byte DIRECTIONALITY_POP_DIRECTIONAL_FORMAT = (byte)POP_DIRECTIONAL_FORMAT;

        /**
         * Directional type NSM
         * @stable ICU 2.1
         */
        public static final int DIR_NON_SPACING_MARK       = 17;

        /**
         * JDK-compatible synonum for DIR_NON_SPACING_MARK.
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final byte DIRECTIONALITY_NON_SPACING_MARK = (byte)DIR_NON_SPACING_MARK;

        /**
         * Directional type BN
         * @stable ICU 2.1
         */
        public static final int BOUNDARY_NEUTRAL           = 18;

        /**
         * JDK-compatible synonum for BOUNDARY_NEUTRAL.
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final byte DIRECTIONALITY_BOUNDARY_NEUTRAL = (byte)BOUNDARY_NEUTRAL;

        /**
         * Number of directional types
         * @stable ICU 2.1
         */
        public static final int CHAR_DIRECTION_COUNT       = 19;

        /**
         * Undefined bidirectional character type. Undefined <code>char</code>
         * values have undefined directionality in the Unicode specification.
         * @draft ICU 3.0
         * @deprecated This is a draft API and might change in a future release of ICU.
         */
        @Deprecated
        public static final byte DIRECTIONALITY_UNDEFINED = -1;
    }
}
