/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *   Copyright (C) 2009-2014, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *******************************************************************************
 */

package jdk.internal.icu.text;

import jdk.internal.icu.impl.Norm2AllModes;

/**
 * Unicode normalization functionality for standard Unicode normalization or
 * for using custom mapping tables.
 * All instances of this class are unmodifiable/immutable.
 * The Normalizer2 class is not intended for public subclassing.
 * <p>
 * The primary functions are to produce a normalized string and to detect whether
 * a string is already normalized.
 * The most commonly used normalization forms are those defined in
 * <a href="http://www.unicode.org/reports/tr15/">Unicode Standard Annex #15:
 * Unicode Normalization Forms</a>.
 * However, this API supports additional normalization forms for specialized purposes.
 * For example, NFKC_Casefold is provided via getInstance("nfkc_cf", COMPOSE)
 * and can be used in implementations of UTS #46.
 * <p>
 * Not only are the standard compose and decompose modes supplied,
 * but additional modes are provided as documented in the Mode enum.
 * <p>
 * Some of the functions in this class identify normalization boundaries.
 * At a normalization boundary, the portions of the string
 * before it and starting from it do not interact and can be handled independently.
 * <p>
 * The spanQuickCheckYes() stops at a normalization boundary.
 * When the goal is a normalized string, then the text before the boundary
 * can be copied, and the remainder can be processed with normalizeSecondAndAppend().
 * <p>
 * The hasBoundaryBefore(), hasBoundaryAfter() and isInert() functions test whether
 * a character is guaranteed to be at a normalization boundary,
 * regardless of context.
 * This is used for moving from one normalization boundary to the next
 * or preceding boundary, and for performing iterative normalization.
 * <p>
 * Iterative normalization is useful when only a small portion of a
 * longer string needs to be processed.
 * For example, in ICU, iterative normalization is used by the NormalizationTransliterator
 * (to avoid replacing already-normalized text) and ucol_nextSortKeyPart()
 * (to process only the substring for which sort key bytes are computed).
 * <p>
 * The set of normalization boundaries returned by these functions may not be
 * complete: There may be more boundaries that could be returned.
 * Different functions may return different boundaries.
 * @stable ICU 4.4
 * @author Markus W. Scherer
 */
public abstract class Normalizer2 {

    /**
     * Returns a Normalizer2 instance for Unicode NFC normalization.
     * Same as getInstance(null, "nfc", Mode.COMPOSE).
     * Returns an unmodifiable singleton instance.
     * @return the requested Normalizer2, if successful
     * @stable ICU 49
     */
    public static Normalizer2 getNFCInstance() {
        return Norm2AllModes.getNFCInstance().comp;
    }

    /**
     * Returns a Normalizer2 instance for Unicode NFD normalization.
     * Same as getInstance(null, "nfc", Mode.DECOMPOSE).
     * Returns an unmodifiable singleton instance.
     * @return the requested Normalizer2, if successful
     * @stable ICU 49
     */
    public static Normalizer2 getNFDInstance() {
        return Norm2AllModes.getNFCInstance().decomp;
    }

    /**
     * Returns a Normalizer2 instance for Unicode NFKC normalization.
     * Same as getInstance(null, "nfkc", Mode.COMPOSE).
     * Returns an unmodifiable singleton instance.
     * @return the requested Normalizer2, if successful
     * @stable ICU 49
     */
    public static Normalizer2 getNFKCInstance() {
        return Norm2AllModes.getNFKCInstance().comp;
    }

    /**
     * Returns a Normalizer2 instance for Unicode NFKD normalization.
     * Same as getInstance(null, "nfkc", Mode.DECOMPOSE).
     * Returns an unmodifiable singleton instance.
     * @return the requested Normalizer2, if successful
     * @stable ICU 49
     */
    public static Normalizer2 getNFKDInstance() {
        return Norm2AllModes.getNFKCInstance().decomp;
    }

    /**
     * Returns the normalized form of the source string.
     * @param src source string
     * @return normalized src
     * @stable ICU 4.4
     */
    public String normalize(CharSequence src) {
        if(src instanceof String) {
            // Fastpath: Do not construct a new String if the src is a String
            // and is already normalized.
            int spanLength=spanQuickCheckYes(src);
            if(spanLength==src.length()) {
                return (String)src;
            }
            if (spanLength != 0) {
                StringBuilder sb=new StringBuilder(src.length()).append(src, 0, spanLength);
                return normalizeSecondAndAppend(sb, src.subSequence(spanLength, src.length())).toString();
            }
        }
        return normalize(src, new StringBuilder(src.length())).toString();
    }

    /**
     * Writes the normalized form of the source string to the destination string
     * (replacing its contents) and returns the destination string.
     * The source and destination strings must be different objects.
     * @param src source string
     * @param dest destination string; its contents is replaced with normalized src
     * @return dest
     * @stable ICU 4.4
     */
    public abstract StringBuilder normalize(CharSequence src, StringBuilder dest);

    /**
     * Writes the normalized form of the source string to the destination Appendable
     * and returns the destination Appendable.
     * The source and destination strings must be different objects.
     *
     * <p>Any {@link java.io.IOException} is wrapped into a {@link com.ibm.icu.util.ICUUncheckedIOException}.
     *
     * @param src source string
     * @param dest destination Appendable; gets normalized src appended
     * @return dest
     * @stable ICU 4.6
     */
    public abstract Appendable normalize(CharSequence src, Appendable dest);

    /**
     * Appends the normalized form of the second string to the first string
     * (merging them at the boundary) and returns the first string.
     * The result is normalized if the first string was normalized.
     * The first and second strings must be different objects.
     * @param first string, should be normalized
     * @param second string, will be normalized
     * @return first
     * @stable ICU 4.4
     */
    public abstract StringBuilder normalizeSecondAndAppend(
            StringBuilder first, CharSequence second);

    /**
     * Appends the second string to the first string
     * (merging them at the boundary) and returns the first string.
     * The result is normalized if both the strings were normalized.
     * The first and second strings must be different objects.
     * @param first string, should be normalized
     * @param second string, should be normalized
     * @return first
     * @stable ICU 4.4
     */
    public abstract StringBuilder append(StringBuilder first, CharSequence second);

    /**
     * Gets the decomposition mapping of c.
     * Roughly equivalent to normalizing the String form of c
     * on a DECOMPOSE Normalizer2 instance, but much faster, and except that this function
     * returns null if c does not have a decomposition mapping in this instance's data.
     * This function is independent of the mode of the Normalizer2.
     * @param c code point
     * @return c's decomposition mapping, if any; otherwise null
     * @stable ICU 4.6
     */
    public abstract String getDecomposition(int c);

    /**
     * Gets the combining class of c.
     * The default implementation returns 0
     * but all standard implementations return the Unicode Canonical_Combining_Class value.
     * @param c code point
     * @return c's combining class
     * @stable ICU 49
     */
    public int getCombiningClass(int c) { return 0; }

    /**
     * Tests if the string is normalized.
     * Internally, in cases where the quickCheck() method would return "maybe"
     * (which is only possible for the two COMPOSE modes) this method
     * resolves to "yes" or "no" to provide a definitive result,
     * at the cost of doing more work in those cases.
     * @param s input string
     * @return true if s is normalized
     * @stable ICU 4.4
     */
    public abstract boolean isNormalized(CharSequence s);

    /**
     * Returns the end of the normalized substring of the input string.
     * In other words, with <code>end=spanQuickCheckYes(s);</code>
     * the substring <code>s.subSequence(0, end)</code>
     * will pass the quick check with a "yes" result.
     * <p>
     * The returned end index is usually one or more characters before the
     * "no" or "maybe" character: The end index is at a normalization boundary.
     * (See the class documentation for more about normalization boundaries.)
     * <p>
     * When the goal is a normalized string and most input strings are expected
     * to be normalized already, then call this method,
     * and if it returns a prefix shorter than the input string,
     * copy that prefix and use normalizeSecondAndAppend() for the remainder.
     * @param s input string
     * @return "yes" span end index
     * @stable ICU 4.4
     */
    public abstract int spanQuickCheckYes(CharSequence s);

    /**
     * Tests if the character always has a normalization boundary before it,
     * regardless of context.
     * If true, then the character does not normalization-interact with
     * preceding characters.
     * In other words, a string containing this character can be normalized
     * by processing portions before this character and starting from this
     * character independently.
     * This is used for iterative normalization. See the class documentation for details.
     * @param c character to test
     * @return true if c has a normalization boundary before it
     * @stable ICU 4.4
     */
    public abstract boolean hasBoundaryBefore(int c);

    /**
     * Sole constructor.  (For invocation by subclass constructors,
     * typically implicit.)
     * @internal
     * deprecated This API is ICU internal only.
     */
    protected Normalizer2() {
    }
}
