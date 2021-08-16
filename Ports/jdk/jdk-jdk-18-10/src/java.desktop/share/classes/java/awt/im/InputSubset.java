/*
 * Copyright (c) 1998, 1999, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.im;


/**
 * Defines additional Unicode subsets for use by input methods.  Unlike the
 * UnicodeBlock subsets defined in the {@link
 * java.lang.Character.UnicodeBlock} class, these constants do not
 * directly correspond to Unicode code blocks.
 *
 * @since   1.2
 */

public final class InputSubset extends Character.Subset {

    private InputSubset(String name) {
        super(name);
    }

    /**
     * Constant for all Latin characters, including the characters
     * in the BASIC_LATIN, LATIN_1_SUPPLEMENT, LATIN_EXTENDED_A,
     * LATIN_EXTENDED_B Unicode character blocks.
     */
    public static final InputSubset LATIN
        = new InputSubset("LATIN");

    /**
     * Constant for the digits included in the BASIC_LATIN Unicode character
     * block.
     */
    public static final InputSubset LATIN_DIGITS
        = new InputSubset("LATIN_DIGITS");

    /**
     * Constant for all Han characters used in writing Traditional Chinese,
     * including a subset of the CJK unified ideographs as well as Traditional
     * Chinese Han characters that may be defined as surrogate characters.
     */
    public static final InputSubset TRADITIONAL_HANZI
        = new InputSubset("TRADITIONAL_HANZI");

    /**
     * Constant for all Han characters used in writing Simplified Chinese,
     * including a subset of the CJK unified ideographs as well as Simplified
     * Chinese Han characters that may be defined as surrogate characters.
     */
    public static final InputSubset SIMPLIFIED_HANZI
        = new InputSubset("SIMPLIFIED_HANZI");

    /**
     * Constant for all Han characters used in writing Japanese, including a
     * subset of the CJK unified ideographs as well as Japanese Han characters
     * that may be defined as surrogate characters.
     */
    public static final InputSubset KANJI
        = new InputSubset("KANJI");

    /**
     * Constant for all Han characters used in writing Korean, including a
     * subset of the CJK unified ideographs as well as Korean Han characters
     * that may be defined as surrogate characters.
     */
    public static final InputSubset HANJA
        = new InputSubset("HANJA");

    /**
     * Constant for the halfwidth katakana subset of the Unicode halfwidth and
     * fullwidth forms character block.
     */
    public static final InputSubset HALFWIDTH_KATAKANA
        = new InputSubset("HALFWIDTH_KATAKANA");

    /**
     * Constant for the fullwidth ASCII variants subset of the Unicode halfwidth and
     * fullwidth forms character block.
     * @since 1.3
     */
    public static final InputSubset FULLWIDTH_LATIN
        = new InputSubset("FULLWIDTH_LATIN");

    /**
     * Constant for the fullwidth digits included in the Unicode halfwidth and
     * fullwidth forms character block.
     * @since 1.3
     */
    public static final InputSubset FULLWIDTH_DIGITS
        = new InputSubset("FULLWIDTH_DIGITS");

}
