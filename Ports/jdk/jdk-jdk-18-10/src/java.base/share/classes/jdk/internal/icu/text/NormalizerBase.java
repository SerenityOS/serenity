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
 * Copyright (C) 2000-2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package jdk.internal.icu.text;

import jdk.internal.icu.impl.Norm2AllModes;

import java.text.CharacterIterator;
import java.text.Normalizer;

/**
 * Unicode Normalization
 *
 * <h2>Unicode normalization API</h2>
 *
 * <code>normalize</code> transforms Unicode text into an equivalent composed or
 * decomposed form, allowing for easier sorting and searching of text.
 * <code>normalize</code> supports the standard normalization forms described in
 * <a href="http://www.unicode.org/reports/tr15/" target="unicode">
 * Unicode Standard Annex #15 &mdash; Unicode Normalization Forms</a>.
 *
 * Characters with accents or other adornments can be encoded in
 * several different ways in Unicode.  For example, take the character A-acute.
 * In Unicode, this can be encoded as a single character (the
 * "composed" form):
 *
 * <pre>
 *      00C1    LATIN CAPITAL LETTER A WITH ACUTE
 * </pre>
 *
 * or as two separate characters (the "decomposed" form):
 *
 * <pre>
 *      0041    LATIN CAPITAL LETTER A
 *      0301    COMBINING ACUTE ACCENT
 * </pre>
 *
 * To a user of your program, however, both of these sequences should be
 * treated as the same "user-level" character "A with acute accent".  When you
 * are searching or comparing text, you must ensure that these two sequences are
 * treated equivalently.  In addition, you must handle characters with more than
 * one accent.  Sometimes the order of a character's combining accents is
 * significant, while in other cases accent sequences in different orders are
 * really equivalent.
 *
 * Similarly, the string "ffi" can be encoded as three separate letters:
 *
 * <pre>
 *      0066    LATIN SMALL LETTER F
 *      0066    LATIN SMALL LETTER F
 *      0069    LATIN SMALL LETTER I
 * </pre>
 *
 * or as the single character
 *
 * <pre>
 *      FB03    LATIN SMALL LIGATURE FFI
 * </pre>
 *
 * The ffi ligature is not a distinct semantic character, and strictly speaking
 * it shouldn't be in Unicode at all, but it was included for compatibility
 * with existing character sets that already provided it.  The Unicode standard
 * identifies such characters by giving them "compatibility" decompositions
 * into the corresponding semantic characters.  When sorting and searching, you
 * will often want to use these mappings.
 *
 * <code>normalize</code> helps solve these problems by transforming text into
 * the canonical composed and decomposed forms as shown in the first example
 * above. In addition, you can have it perform compatibility decompositions so
 * that you can treat compatibility characters the same as their equivalents.
 * Finally, <code>normalize</code> rearranges accents into the proper canonical
 * order, so that you do not have to worry about accent rearrangement on your
 * own.
 *
 * Form FCD, "Fast C or D", is also designed for collation.
 * It allows to work on strings that are not necessarily normalized
 * with an algorithm (like in collation) that works under "canonical closure",
 * i.e., it treats precomposed characters and their decomposed equivalents the
 * same.
 *
 * It is not a normalization form because it does not provide for uniqueness of
 * representation. Multiple strings may be canonically equivalent (their NFDs
 * are identical) and may all conform to FCD without being identical themselves.
 *
 * The form is defined such that the "raw decomposition", the recursive
 * canonical decomposition of each character, results in a string that is
 * canonically ordered. This means that precomposed characters are allowed for
 * as long as their decompositions do not need canonical reordering.
 *
 * Its advantage for a process like collation is that all NFD and most NFC texts
 * - and many unnormalized texts - already conform to FCD and do not need to be
 * normalized (NFD) for such a process. The FCD quick check will return YES for
 * most strings in practice.
 *
 * normalize(FCD) may be implemented with NFD.
 *
 * For more details on FCD see Unicode Technical Note #5 (Canonical Equivalence in Applications):
 * http://www.unicode.org/notes/tn5/#FCD
 *
 * ICU collation performs either NFD or FCD normalization automatically if
 * normalization is turned on for the collator object. Beyond collation and
 * string search, normalized strings may be useful for string equivalence
 * comparisons, transliteration/transcription, unique representations, etc.
 *
 * The W3C generally recommends to exchange texts in NFC.
 * Note also that most legacy character encodings use only precomposed forms and
 * often do not encode any combining marks by themselves. For conversion to such
 * character encodings the Unicode text needs to be normalized to NFC.
 * For more usage examples, see the Unicode Standard Annex.
 *
 * Note: The Normalizer class also provides API for iterative normalization.
 * While the setIndex() and getIndex() refer to indices in the
 * underlying Unicode input text, the next() and previous() methods
 * iterate through characters in the normalized output.
 * This means that there is not necessarily a one-to-one correspondence
 * between characters returned by next() and previous() and the indices
 * passed to and returned from setIndex() and getIndex().
 * It is for this reason that Normalizer does not implement the CharacterIterator interface.
 *
 * @stable ICU 2.8
 */
// Original filename in ICU4J: Normalizer.java
public final class NormalizerBase implements Cloneable {

    // The input text and our position in it
    private UCharacterIterator  text;
    private Normalizer2         norm2;
    private Mode                mode;
    private int                 options;

    // The normalization buffer is the result of normalization
    // of the source in [currentIndex..nextIndex] .
    private int                 currentIndex;
    private int                 nextIndex;

    // A buffer for holding intermediate results
    private StringBuilder       buffer;
    private int                 bufferPos;

    // Helper classes to defer loading of normalization data.
    private static final class ModeImpl {
        private ModeImpl(Normalizer2 n2) {
            normalizer2 = n2;
        }
        private final Normalizer2 normalizer2;
    }

    private static final class NFDModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(Normalizer2.getNFDInstance());
    }

    private static final class NFKDModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(Normalizer2.getNFKDInstance());
    }

    private static final class NFCModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(Normalizer2.getNFCInstance());
    }

    private static final class NFKCModeImpl {
        private static final ModeImpl INSTANCE = new ModeImpl(Normalizer2.getNFKCInstance());
    }

    private static final class Unicode32 {
        private static final UnicodeSet INSTANCE = new UnicodeSet("[:age=3.2:]").freeze();
    }

    private static final class NFD32ModeImpl {
        private static final ModeImpl INSTANCE =
            new ModeImpl(new FilteredNormalizer2(Normalizer2.getNFDInstance(),
                                                 Unicode32.INSTANCE));
    }

    private static final class NFKD32ModeImpl {
        private static final ModeImpl INSTANCE =
            new ModeImpl(new FilteredNormalizer2(Normalizer2.getNFKDInstance(),
                                                 Unicode32.INSTANCE));
    }

    private static final class NFC32ModeImpl {
        private static final ModeImpl INSTANCE =
            new ModeImpl(new FilteredNormalizer2(Normalizer2.getNFCInstance(),
                                                 Unicode32.INSTANCE));
    }

    private static final class NFKC32ModeImpl {
        private static final ModeImpl INSTANCE =
            new ModeImpl(new FilteredNormalizer2(Normalizer2.getNFKCInstance(),
                                                 Unicode32.INSTANCE));
    }

    /**
     * Options bit set value to select Unicode 3.2 normalization
     * (except NormalizationCorrections).
     * At most one Unicode version can be selected at a time.
     * @stable ICU 2.6
     */
    public static final int UNICODE_3_2=0x20;

    public static final int UNICODE_3_2_0_ORIGINAL=UNICODE_3_2;

    /*
     * Default option for the latest Unicode normalization. This option is
     * provided mainly for testing.
     * The value zero means that normalization is done with the fixes for
     *   - Corrigendum 4 (Five CJK Canonical Mapping Errors)
     *   - Corrigendum 5 (Normalization Idempotency)
     */
    public static final int UNICODE_LATEST = 0x00;

    /**
     * Constant indicating that the end of the iteration has been reached.
     * This is guaranteed to have the same value as {@link UCharacterIterator#DONE}.
     * @stable ICU 2.8
     */
    public static final int DONE = UCharacterIterator.DONE;

    /**
     * Constants for normalization modes.
     * <p>
     * The Mode class is not intended for public subclassing.
     * Only the Mode constants provided by the Normalizer class should be used,
     * and any fields or methods should not be called or overridden by users.
     * @stable ICU 2.8
     */
    public abstract static class Mode {

        /**
         * Sole constructor
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        protected Mode() {
        }

        /**
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        protected abstract Normalizer2 getNormalizer2(int options);
    }

    private static Mode toMode(Normalizer.Form form) {
        switch (form) {
        case NFC :
            return NFC;
        case NFD :
            return NFD;
        case NFKC :
            return NFKC;
        case NFKD :
            return NFKD;
        }

        throw new IllegalArgumentException("Unexpected normalization form: " +
                                           form);
    }

    private static final class NONEMode extends Mode {
        protected Normalizer2 getNormalizer2(int options) { return Norm2AllModes.NOOP_NORMALIZER2; }
    }

    private static final class NFDMode extends Mode {
        protected Normalizer2 getNormalizer2(int options) {
            return (options&UNICODE_3_2) != 0 ?
                    NFD32ModeImpl.INSTANCE.normalizer2 :
                    NFDModeImpl.INSTANCE.normalizer2;
        }
    }

    private static final class NFKDMode extends Mode {
        protected Normalizer2 getNormalizer2(int options) {
            return (options&UNICODE_3_2) != 0 ?
                    NFKD32ModeImpl.INSTANCE.normalizer2 :
                    NFKDModeImpl.INSTANCE.normalizer2;
        }
    }

    private static final class NFCMode extends Mode {
        protected Normalizer2 getNormalizer2(int options) {
            return (options&UNICODE_3_2) != 0 ?
                    NFC32ModeImpl.INSTANCE.normalizer2 :
                    NFCModeImpl.INSTANCE.normalizer2;
        }
    }

    private static final class NFKCMode extends Mode {
        protected Normalizer2 getNormalizer2(int options) {
            return (options&UNICODE_3_2) != 0 ?
                    NFKC32ModeImpl.INSTANCE.normalizer2 :
                    NFKCModeImpl.INSTANCE.normalizer2;
        }
    }

    /**
     * No decomposition/composition.
     * @stable ICU 2.8
     */
    public static final Mode NONE = new NONEMode();

    /**
     * Canonical decomposition.
     * @stable ICU 2.8
     */
    public static final Mode NFD = new NFDMode();

    /**
     * Compatibility decomposition.
     * @stable ICU 2.8
     */
    public static final Mode NFKD = new NFKDMode();

    /**
     * Canonical decomposition followed by canonical composition.
     * @stable ICU 2.8
     */
    public static final Mode NFC = new NFCMode();

    public static final Mode NFKC =new NFKCMode();

    //-------------------------------------------------------------------------
    // Iterator constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new {@code NormalizerBase} object for iterating over the
     * normalized form of a given string.
     * <p>
     * The {@code options} parameter specifies which optional
     * {@code NormalizerBase} features are to be enabled for this object.
     * <p>
     * @param str  The string to be normalized.  The normalization
     *              will start at the beginning of the string.
     *
     * @param mode The normalization mode.
     *
     * @param opt Any optional features to be enabled.
     *            Currently the only available option is {@link #UNICODE_3_2}.
     *            If you want the default behavior corresponding to one of the
     *            standard Unicode Normalization Forms, use 0 for this argument.
     * @stable ICU 2.6
     */
    public NormalizerBase(String str, Mode mode, int opt) {
        this.text = UCharacterIterator.getInstance(str);
        this.mode = mode;
        this.options=opt;
        norm2 = mode.getNormalizer2(opt);
        buffer = new StringBuilder();
    }

    public NormalizerBase(String str, Mode mode) {
       this(str, mode, 0);
    }


    /**
     * Creates a new {@code NormalizerBase} object for iterating over the
     * normalized form of the given text.
     * <p>
     * @param iter  The input text to be normalized.  The normalization
     *              will start at the beginning of the string.
     *
     * @param mode  The normalization mode.
     *
     * @param opt Any optional features to be enabled.
     *            Currently the only available option is {@link #UNICODE_3_2}.
     *            If you want the default behavior corresponding to one of the
     *            standard Unicode Normalization Forms, use 0 for this argument.
     * @stable ICU 2.6
     */
    public NormalizerBase(CharacterIterator iter, Mode mode, int opt) {
        this.text = UCharacterIterator.getInstance((CharacterIterator)iter.clone());
        this.mode = mode;
        this.options = opt;
        norm2 = mode.getNormalizer2(opt);
        buffer = new StringBuilder();
    }

    public NormalizerBase(CharacterIterator iter, Mode mode) {
       this(iter, mode, 0);
    }

    /**
     * Clones this {@code NormalizerBase} object.  All properties of this
     * object are duplicated in the new object, including the cloning of any
     * {@link CharacterIterator} that was passed in to the constructor
     * or to {@link #setText(CharacterIterator) setText}.
     * However, the text storage underlying
     * the {@code CharacterIterator} is not duplicated unless the
     * iterator's {@code clone} method does so.
     * @stable ICU 2.8
     */
    public Object clone() {
        try {
            NormalizerBase copy = (NormalizerBase) super.clone();
            copy.text = (UCharacterIterator) text.clone();
            copy.mode = mode;
            copy.options = options;
            copy.norm2 = norm2;
            copy.buffer = new StringBuilder(buffer);
            copy.bufferPos = bufferPos;
            copy.currentIndex = currentIndex;
            copy.nextIndex = nextIndex;
            return copy;
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString(), e);
        }
    }

    /**
     * Normalizes a {@code String} using the given normalization operation.
     * <p>
     * The {@code options} parameter specifies which optional
     * {@code NormalizerBase} features are to be enabled for this operation.
     * Currently the only available option is {@link #UNICODE_3_2}.
     * If you want the default behavior corresponding to one of the standard
     * Unicode Normalization Forms, use 0 for this argument.
     * <p>
     * @param str       the input string to be normalized.
     * @param mode      the normalization mode
     * @param options   the optional features to be enabled.
     * @return String   the normalized string
     * @stable ICU 2.6
     */
    public static String normalize(String str, Mode mode, int options) {
        return mode.getNormalizer2(options).normalize(str);
    }

    public static String normalize(String str, Normalizer.Form form) {
        return NormalizerBase.normalize(str, toMode(form), UNICODE_LATEST);
    }

    public static String normalize(String str, Normalizer.Form form, int options) {
        return NormalizerBase.normalize(str, toMode(form), options);
    }

    /**
     * Test if a string is in a given normalization form.
     * This is semantically equivalent to source.equals(normalize(source, mode)).
     *
     * Unlike quickCheck(), this function returns a definitive result,
     * never a "maybe".
     * For NFD, NFKD, and FCD, both functions work exactly the same.
     * For NFC and NFKC where quickCheck may return "maybe", this function will
     * perform further tests to arrive at a true/false result.
     * @param str       the input string to be checked to see if it is
     *                   normalized
     * @param mode      the normalization mode
     * @param options   Options for use with exclusion set and tailored Normalization
     *                  The only option that is currently recognized is UNICODE_3_2
     * @see #isNormalized
     * @stable ICU 2.6
     */
    public static boolean isNormalized(String str, Mode mode, int options) {
        return mode.getNormalizer2(options).isNormalized(str);
    }

    public static boolean isNormalized(String str, Normalizer.Form form) {
        return NormalizerBase.isNormalized(str, toMode(form), UNICODE_LATEST);
    }

    public static boolean isNormalized(String str, Normalizer.Form form, int options) {
        return NormalizerBase.isNormalized(str, toMode(form), options);
    }

    //-------------------------------------------------------------------------
    // Iteration API
    //-------------------------------------------------------------------------

    /**
     * Return the current character in the normalized text.
     * @return The codepoint as an int
     * @stable ICU 2.8
     */
    public int current() {
        if(bufferPos<buffer.length() || nextNormalize()) {
            return buffer.codePointAt(bufferPos);
        } else {
            return DONE;
        }
    }

    /**
     * Return the next character in the normalized text and advance
     * the iteration position by one.  If the end
     * of the text has already been reached, {@link #DONE} is returned.
     * @return The codepoint as an int
     * @stable ICU 2.8
     */
    public int next() {
        if(bufferPos<buffer.length() ||  nextNormalize()) {
            int c=buffer.codePointAt(bufferPos);
            bufferPos+=Character.charCount(c);
            return c;
        } else {
            return DONE;
        }
    }

    /**
     * Return the previous character in the normalized text and decrement
     * the iteration position by one.  If the beginning
     * of the text has already been reached, {@link #DONE} is returned.
     * @return The codepoint as an int
     * @stable ICU 2.8
     */
    public int previous() {
        if(bufferPos>0 || previousNormalize()) {
            int c=buffer.codePointBefore(bufferPos);
            bufferPos-=Character.charCount(c);
            return c;
        } else {
            return DONE;
        }
    }

    /**
     * Reset the index to the beginning of the text.
     * This is equivalent to setIndexOnly(startIndex)).
     * @stable ICU 2.8
     */
    public void reset() {
        text.setIndex(0);
        currentIndex=nextIndex=0;
        clearBuffer();
    }

    /**
     * Set the iteration position in the input text that is being normalized,
     * without any immediate normalization.
     * After setIndexOnly(), getIndex() will return the same index that is
     * specified here.
     *
     * @param index the desired index in the input text.
     * @stable ICU 2.8
     */
    public void setIndexOnly(int index) {
        text.setIndex(index);  // validates index
        currentIndex=nextIndex=index;
        clearBuffer();
    }

    /**
     * Set the iteration position in the input text that is being normalized
     * and return the first normalized character at that position.
     * <p>
     * <b>Note:</b> This method sets the position in the <em>input</em> text,
     * while {@link #next} and {@link #previous} iterate through characters
     * in the normalized <em>output</em>.  This means that there is not
     * necessarily a one-to-one correspondence between characters returned
     * by {@code next} and {@code previous} and the indices passed to and
     * returned from {@code setIndex} and {@link #getIndex}.
     * <p>
     * @param index the desired index in the input text.
     *
     * @return   the first normalized character that is the result of iterating
     *            forward starting at the given index.
     *
     * @throws IllegalArgumentException if the given index is less than
     *          {@link #getBeginIndex} or greater than {@link #getEndIndex}.
     * deprecated ICU 3.2
     * @obsolete ICU 3.2
     */
     public int setIndex(int index) {
         setIndexOnly(index);
         return current();
     }

    /**
     * Retrieve the index of the start of the input text. This is the begin
     * index of the {@code CharacterIterator} or the start (i.e. 0) of the
     * {@code String} over which this {@code NormalizerBase} is iterating
     * @deprecated ICU 2.2. Use startIndex() instead.
     * @return The codepoint as an int
     * @see #startIndex
     */
    @Deprecated
    public int getBeginIndex() {
        return 0;
    }

    /**
     * Retrieve the index of the end of the input text.  This is the end index
     * of the {@code CharacterIterator} or the length of the {@code String}
     * over which this {@code NormalizerBase} is iterating
     * @deprecated ICU 2.2. Use endIndex() instead.
     * @return The codepoint as an int
     * @see #endIndex
     */
    @Deprecated
    public int getEndIndex() {
        return endIndex();
    }

    /**
     * Retrieve the current iteration position in the input text that is
     * being normalized.  This method is useful in applications such as
     * searching, where you need to be able to determine the position in
     * the input text that corresponds to a given normalized output character.
     * <p>
     * <b>Note:</b> This method sets the position in the <em>input</em>, while
     * {@link #next} and {@link #previous} iterate through characters in the
     * <em>output</em>.  This means that there is not necessarily a one-to-one
     * correspondence between characters returned by {@code next} and
     * {@code previous} and the indices passed to and returned from
     * {@code setIndex} and {@link #getIndex}.
     * @return The current iteration position
     * @stable ICU 2.8
     */
    public int getIndex() {
        if(bufferPos<buffer.length()) {
            return currentIndex;
        } else {
            return nextIndex;
        }
    }

    /**
     * Retrieve the index of the end of the input text.  This is the end index
     * of the {@code CharacterIterator} or the length of the {@code String}
     * over which this {@code NormalizerBase} is iterating
     * @return The current iteration position
     * @stable ICU 2.8
     */
    public int endIndex() {
        return text.getLength();
    }

    //-------------------------------------------------------------------------
    // Iterator attributes
    //-------------------------------------------------------------------------
    /**
     * Set the normalization mode for this object.
     * <p>
     * <b>Note:</b>If the normalization mode is changed while iterating
     * over a string, calls to {@link #next} and {@link #previous} may
     * return previously buffers characters in the old normalization mode
     * until the iteration is able to re-sync at the next base character.
     * It is safest to call {@link #setText setText()}, {@link #first},
     * {@link #last}, etc. after calling {@code setMode}.
     * <p>
     * @param newMode the new mode for this {@code NormalizerBase}.
     * The supported modes are:
     * <ul>
     *  <li>{@link #NFC}    - Unicode canonical decompositiion
     *                        followed by canonical composition.
     *  <li>{@link #NFKC}   - Unicode compatibility decompositiion
     *                        follwed by canonical composition.
     *  <li>{@link #NFD}    - Unicode canonical decomposition
     *  <li>{@link #NFKD}   - Unicode compatibility decomposition.
     *  <li>{@link #NONE}   - Do nothing but return characters
     *                        from the underlying input text.
     * </ul>
     *
     * @see #getMode
     * @stable ICU 2.8
     */
    public void setMode(Mode newMode) {
        mode = newMode;
        norm2 = mode.getNormalizer2(options);
    }

    /**
     * Return the basic operation performed by this {@code NormalizerBase}
     *
     * @see #setMode
     * @stable ICU 2.8
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Set the input text over which this {@code NormalizerBase} will iterate.
     * The iteration position is set to the beginning of the input text.
     * @param newText   The new string to be normalized.
     * @stable ICU 2.8
     */
    public void setText(String newText) {
        UCharacterIterator newIter = UCharacterIterator.getInstance(newText);
        if (newIter == null) {
            throw new IllegalStateException("Could not create a new UCharacterIterator");
        }
        text = newIter;
        reset();
    }

    /**
     * Set the input text over which this {@code NormalizerBase} will iterate.
     * The iteration position is set to the beginning of the input text.
     * @param newText   The new string to be normalized.
     * @stable ICU 2.8
     */
    public void setText(CharacterIterator newText) {
        UCharacterIterator newIter = UCharacterIterator.getInstance(newText);
        if (newIter == null) {
            throw new IllegalStateException("Could not create a new UCharacterIterator");
        }
        text = newIter;
        currentIndex=nextIndex=0;
        clearBuffer();
    }

    private void clearBuffer() {
        buffer.setLength(0);
        bufferPos=0;
    }

    private boolean nextNormalize() {
        clearBuffer();
        currentIndex=nextIndex;
        text.setIndex(nextIndex);
        // Skip at least one character so we make progress.
        int c=text.nextCodePoint();
        if(c<0) {
            return false;
        }
        StringBuilder segment=new StringBuilder().appendCodePoint(c);
        while((c=text.nextCodePoint())>=0) {
            if(norm2.hasBoundaryBefore(c)) {
                text.moveCodePointIndex(-1);
                break;
            }
            segment.appendCodePoint(c);
        }
        nextIndex=text.getIndex();
        norm2.normalize(segment, buffer);
        return buffer.length()!=0;
    }

    private boolean previousNormalize() {
        clearBuffer();
        nextIndex=currentIndex;
        text.setIndex(currentIndex);
        StringBuilder segment=new StringBuilder();
        int c;
        while((c=text.previousCodePoint())>=0) {
            if(c<=0xffff) {
                segment.insert(0, (char)c);
            } else {
                segment.insert(0, Character.toChars(c));
            }
            if(norm2.hasBoundaryBefore(c)) {
                break;
            }
        }
        currentIndex=text.getIndex();
        norm2.normalize(segment, buffer);
        bufferPos=buffer.length();
        return buffer.length()!=0;
    }

}
