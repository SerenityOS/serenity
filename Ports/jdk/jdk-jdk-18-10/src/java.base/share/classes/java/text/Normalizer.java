/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *******************************************************************************
 * (C) Copyright IBM Corp. 1996-2005 - All Rights Reserved                     *
 *                                                                             *
 * The original version of this source code and documentation is copyrighted   *
 * and owned by IBM, These materials are provided under terms of a License     *
 * Agreement between IBM and Sun. This technology is protected by multiple     *
 * US and International patents. This notice and attribution to IBM may not    *
 * to removed.                                                                 *
 *******************************************************************************
 */

package java.text;

import jdk.internal.icu.text.NormalizerBase;

/**
 * This class provides the method {@code normalize} which transforms Unicode
 * text into an equivalent composed or decomposed form, allowing for easier
 * sorting and searching of text.
 * The {@code normalize} method supports the standard normalization forms
 * described in
 * <a href="https://www.unicode.org/reports/tr15/">
 * Unicode Standard Annex #15 &mdash; Unicode Normalization Forms</a>.
 * <p>
 * Characters with accents or other adornments can be encoded in
 * several different ways in Unicode.  For example, take the character A-acute.
 * In Unicode, this can be encoded as a single character (the "composed" form):
 *
 * <pre>
 *      U+00C1    LATIN CAPITAL LETTER A WITH ACUTE</pre>
 *
 * or as two separate characters (the "decomposed" form):
 *
 * <pre>
 *      U+0041    LATIN CAPITAL LETTER A
 *      U+0301    COMBINING ACUTE ACCENT</pre>
 *
 * To a user of your program, however, both of these sequences should be
 * treated as the same "user-level" character "A with acute accent".  When you
 * are searching or comparing text, you must ensure that these two sequences are
 * treated as equivalent.  In addition, you must handle characters with more than
 * one accent. Sometimes the order of a character's combining accents is
 * significant, while in other cases accent sequences in different orders are
 * really equivalent.
 * <p>
 * Similarly, the string "ffi" can be encoded as three separate letters:
 *
 * <pre>
 *      U+0066    LATIN SMALL LETTER F
 *      U+0066    LATIN SMALL LETTER F
 *      U+0069    LATIN SMALL LETTER I</pre>
 *
 * or as the single character
 *
 * <pre>
 *      U+FB03    LATIN SMALL LIGATURE FFI</pre>
 *
 * The ffi ligature is not a distinct semantic character, and strictly speaking
 * it shouldn't be in Unicode at all, but it was included for compatibility
 * with existing character sets that already provided it.  The Unicode standard
 * identifies such characters by giving them "compatibility" decompositions
 * into the corresponding semantic characters.  When sorting and searching, you
 * will often want to use these mappings.
 * <p>
 * The {@code normalize} method helps solve these problems by transforming
 * text into the canonical composed and decomposed forms as shown in the first
 * example above. In addition, you can have it perform compatibility
 * decompositions so that you can treat compatibility characters the same as
 * their equivalents.
 * Finally, the {@code normalize} method rearranges accents into the
 * proper canonical order, so that you do not have to worry about accent
 * rearrangement on your own.
 * <p>
 * The W3C generally recommends to exchange texts in NFC.
 * Note also that most legacy character encodings use only precomposed forms and
 * often do not encode any combining marks by themselves. For conversion to such
 * character encodings the Unicode text needs to be normalized to NFC.
 * For more usage examples, see the Unicode Standard Annex.
 *
 * @since 1.6
 */
public final class Normalizer {

   private Normalizer() {};

    /**
     * This enum provides constants of the four Unicode normalization forms
     * that are described in
     * <a href="https://www.unicode.org/reports/tr15/">
     * Unicode Standard Annex #15 &mdash; Unicode Normalization Forms</a>
     * and two methods to access them.
     *
     * @since 1.6
     */
    public static enum Form {

        /**
         * Canonical decomposition.
         */
        NFD,

        /**
         * Canonical decomposition, followed by canonical composition.
         */
        NFC,

        /**
         * Compatibility decomposition.
         */
        NFKD,

        /**
         * Compatibility decomposition, followed by canonical composition.
         */
        NFKC
    }

    /**
     * Normalize a sequence of char values.
     * The sequence will be normalized according to the specified normalization
     * form.
     * @param src        The sequence of char values to normalize.
     * @param form       The normalization form; one of
     *                   {@link java.text.Normalizer.Form#NFC},
     *                   {@link java.text.Normalizer.Form#NFD},
     *                   {@link java.text.Normalizer.Form#NFKC},
     *                   {@link java.text.Normalizer.Form#NFKD}
     * @return The normalized String
     * @throws NullPointerException If {@code src} or {@code form}
     * is null.
     */
    public static String normalize(CharSequence src, Form form) {
        return NormalizerBase.normalize(src.toString(), form);
    }

    /**
     * Determines if the given sequence of char values is normalized.
     * @param src        The sequence of char values to be checked.
     * @param form       The normalization form; one of
     *                   {@link java.text.Normalizer.Form#NFC},
     *                   {@link java.text.Normalizer.Form#NFD},
     *                   {@link java.text.Normalizer.Form#NFKC},
     *                   {@link java.text.Normalizer.Form#NFKD}
     * @return true if the sequence of char values is normalized;
     * false otherwise.
     * @throws NullPointerException If {@code src} or {@code form}
     * is null.
     */
    public static boolean isNormalized(CharSequence src, Form form) {
        return NormalizerBase.isNormalized(src.toString(), form);
    }
}
