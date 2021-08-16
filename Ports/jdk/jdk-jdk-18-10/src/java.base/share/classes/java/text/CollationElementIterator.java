/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright Taligent, Inc. 1996, 1997 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996-1998 - All Rights Reserved
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.text;

import java.lang.Character;
import java.util.Vector;
import sun.text.CollatorUtilities;
import jdk.internal.icu.text.NormalizerBase;

/**
 * The {@code CollationElementIterator} class is used as an iterator
 * to walk through each character of an international string. Use the iterator
 * to return the ordering priority of the positioned character. The ordering
 * priority of a character, which we refer to as a key, defines how a character
 * is collated in the given collation object.
 *
 * <p>
 * For example, consider the following in Spanish:
 * <blockquote>
 * <pre>
 * "ca" &rarr; the first key is key('c') and second key is key('a').
 * "cha" &rarr; the first key is key('ch') and second key is key('a').
 * </pre>
 * </blockquote>
 * And in German,
 * <blockquote>
 * <pre>
 * "\u00e4b" &rarr; the first key is key('a'), the second key is key('e'), and
 * the third key is key('b').
 * </pre>
 * </blockquote>
 * The key of a character is an integer composed of primary order(short),
 * secondary order(byte), and tertiary order(byte). Java strictly defines
 * the size and signedness of its primitive data types. Therefore, the static
 * functions {@code primaryOrder}, {@code secondaryOrder}, and
 * {@code tertiaryOrder} return {@code int}, {@code short},
 * and {@code short} respectively to ensure the correctness of the key
 * value.
 *
 * <p>
 * Example of the iterator usage,
 * <blockquote>
 * <pre>
 *
 *  String testString = "This is a test";
 *  Collator col = Collator.getInstance();
 *  if (col instanceof RuleBasedCollator) {
 *      RuleBasedCollator ruleBasedCollator = (RuleBasedCollator)col;
 *      CollationElementIterator collationElementIterator = ruleBasedCollator.getCollationElementIterator(testString);
 *      int primaryOrder = CollationElementIterator.primaryOrder(collationElementIterator.next());
 *          :
 *  }
 * </pre>
 * </blockquote>
 *
 * <p>
 * {@code CollationElementIterator.next} returns the collation order
 * of the next character. A collation order consists of primary order,
 * secondary order and tertiary order. The data type of the collation
 * order is <strong>int</strong>. The first 16 bits of a collation order
 * is its primary order; the next 8 bits is the secondary order and the
 * last 8 bits is the tertiary order.
 *
 * <p><b>Note:</b> {@code CollationElementIterator} is a part of
 * {@code RuleBasedCollator} implementation. It is only usable
 * with {@code RuleBasedCollator} instances.
 *
 * @see                Collator
 * @see                RuleBasedCollator
 * @author             Helena Shih, Laura Werner, Richard Gillam
 * @since 1.1
 */
public final class CollationElementIterator
{
    /**
     * Null order which indicates the end of string is reached by the
     * cursor.
     */
    public static final int NULLORDER = 0xffffffff;

    /**
     * CollationElementIterator constructor.  This takes the source string and
     * the collation object.  The cursor will walk thru the source string based
     * on the predefined collation rules.  If the source string is empty,
     * NULLORDER will be returned on the calls to next().
     * @param sourceText the source string.
     * @param owner the collation object.
     */
    CollationElementIterator(String sourceText, RuleBasedCollator owner) {
        this.owner = owner;
        ordering = owner.getTables();
        if (!sourceText.isEmpty()) {
            NormalizerBase.Mode mode =
                CollatorUtilities.toNormalizerMode(owner.getDecomposition());
            text = new NormalizerBase(sourceText, mode);
        }
    }

    /**
     * CollationElementIterator constructor.  This takes the source string and
     * the collation object.  The cursor will walk thru the source string based
     * on the predefined collation rules.  If the source string is empty,
     * NULLORDER will be returned on the calls to next().
     * @param sourceText the source string.
     * @param owner the collation object.
     */
    CollationElementIterator(CharacterIterator sourceText, RuleBasedCollator owner) {
        this.owner = owner;
        ordering = owner.getTables();
        NormalizerBase.Mode mode =
            CollatorUtilities.toNormalizerMode(owner.getDecomposition());
        text = new NormalizerBase(sourceText, mode);
    }

    /**
     * Resets the cursor to the beginning of the string.  The next call
     * to next() will return the first collation element in the string.
     */
    public void reset()
    {
        if (text != null) {
            text.reset();
            NormalizerBase.Mode mode =
                CollatorUtilities.toNormalizerMode(owner.getDecomposition());
            text.setMode(mode);
        }
        buffer = null;
        expIndex = 0;
        swapOrder = 0;
    }

    /**
     * Get the next collation element in the string.  <p>This iterator iterates
     * over a sequence of collation elements that were built from the string.
     * Because there isn't necessarily a one-to-one mapping from characters to
     * collation elements, this doesn't mean the same thing as "return the
     * collation element [or ordering priority] of the next character in the
     * string".</p>
     * <p>This function returns the collation element that the iterator is currently
     * pointing to and then updates the internal pointer to point to the next element.
     * previous() updates the pointer first and then returns the element.  This
     * means that when you change direction while iterating (i.e., call next() and
     * then call previous(), or call previous() and then call next()), you'll get
     * back the same element twice.</p>
     *
     * @return the next collation element
     */
    public int next()
    {
        if (text == null) {
            return NULLORDER;
        }
        NormalizerBase.Mode textMode = text.getMode();
        // convert the owner's mode to something the Normalizer understands
        NormalizerBase.Mode ownerMode =
            CollatorUtilities.toNormalizerMode(owner.getDecomposition());
        if (textMode != ownerMode) {
            text.setMode(ownerMode);
        }

        // if buffer contains any decomposed char values
        // return their strength orders before continuing in
        // the Normalizer's CharacterIterator.
        if (buffer != null) {
            if (expIndex < buffer.length) {
                return strengthOrder(buffer[expIndex++]);
            } else {
                buffer = null;
                expIndex = 0;
            }
        } else if (swapOrder != 0) {
            if (Character.isSupplementaryCodePoint(swapOrder)) {
                char[] chars = Character.toChars(swapOrder);
                swapOrder = chars[1];
                return chars[0] << 16;
            }
            int order = swapOrder << 16;
            swapOrder = 0;
            return order;
        }
        int ch  = text.next();

        // are we at the end of Normalizer's text?
        if (ch == NormalizerBase.DONE) {
            return NULLORDER;
        }

        int value = ordering.getUnicodeOrder(ch);
        if (value == RuleBasedCollator.UNMAPPED) {
            swapOrder = ch;
            return UNMAPPEDCHARVALUE;
        }
        else if (value >= RuleBasedCollator.CONTRACTCHARINDEX) {
            value = nextContractChar(ch);
        }
        if (value >= RuleBasedCollator.EXPANDCHARINDEX) {
            buffer = ordering.getExpandValueList(value);
            expIndex = 0;
            value = buffer[expIndex++];
        }

        if (ordering.isSEAsianSwapping()) {
            int consonant;
            if (isThaiPreVowel(ch)) {
                consonant = text.next();
                if (isThaiBaseConsonant(consonant)) {
                    buffer = makeReorderedBuffer(consonant, value, buffer, true);
                    value = buffer[0];
                    expIndex = 1;
                } else if (consonant != NormalizerBase.DONE) {
                    text.previous();
                }
            }
            if (isLaoPreVowel(ch)) {
                consonant = text.next();
                if (isLaoBaseConsonant(consonant)) {
                    buffer = makeReorderedBuffer(consonant, value, buffer, true);
                    value = buffer[0];
                    expIndex = 1;
                } else if (consonant != NormalizerBase.DONE) {
                    text.previous();
                }
            }
        }

        return strengthOrder(value);
    }

    /**
     * Get the previous collation element in the string.  <p>This iterator iterates
     * over a sequence of collation elements that were built from the string.
     * Because there isn't necessarily a one-to-one mapping from characters to
     * collation elements, this doesn't mean the same thing as "return the
     * collation element [or ordering priority] of the previous character in the
     * string".</p>
     * <p>This function updates the iterator's internal pointer to point to the
     * collation element preceding the one it's currently pointing to and then
     * returns that element, while next() returns the current element and then
     * updates the pointer.  This means that when you change direction while
     * iterating (i.e., call next() and then call previous(), or call previous()
     * and then call next()), you'll get back the same element twice.</p>
     *
     * @return the previous collation element
     * @since 1.2
     */
    public int previous()
    {
        if (text == null) {
            return NULLORDER;
        }
        NormalizerBase.Mode textMode = text.getMode();
        // convert the owner's mode to something the Normalizer understands
        NormalizerBase.Mode ownerMode =
            CollatorUtilities.toNormalizerMode(owner.getDecomposition());
        if (textMode != ownerMode) {
            text.setMode(ownerMode);
        }
        if (buffer != null) {
            if (expIndex > 0) {
                return strengthOrder(buffer[--expIndex]);
            } else {
                buffer = null;
                expIndex = 0;
            }
        } else if (swapOrder != 0) {
            if (Character.isSupplementaryCodePoint(swapOrder)) {
                char[] chars = Character.toChars(swapOrder);
                swapOrder = chars[1];
                return chars[0] << 16;
            }
            int order = swapOrder << 16;
            swapOrder = 0;
            return order;
        }
        int ch = text.previous();
        if (ch == NormalizerBase.DONE) {
            return NULLORDER;
        }

        int value = ordering.getUnicodeOrder(ch);

        if (value == RuleBasedCollator.UNMAPPED) {
            swapOrder = UNMAPPEDCHARVALUE;
            return ch;
        } else if (value >= RuleBasedCollator.CONTRACTCHARINDEX) {
            value = prevContractChar(ch);
        }
        if (value >= RuleBasedCollator.EXPANDCHARINDEX) {
            buffer = ordering.getExpandValueList(value);
            expIndex = buffer.length;
            value = buffer[--expIndex];
        }

        if (ordering.isSEAsianSwapping()) {
            int vowel;
            if (isThaiBaseConsonant(ch)) {
                vowel = text.previous();
                if (isThaiPreVowel(vowel)) {
                    buffer = makeReorderedBuffer(vowel, value, buffer, false);
                    expIndex = buffer.length - 1;
                    value = buffer[expIndex];
                } else {
                    text.next();
                }
            }
            if (isLaoBaseConsonant(ch)) {
                vowel = text.previous();
                if (isLaoPreVowel(vowel)) {
                    buffer = makeReorderedBuffer(vowel, value, buffer, false);
                    expIndex = buffer.length - 1;
                    value = buffer[expIndex];
                } else {
                    text.next();
                }
            }
        }

        return strengthOrder(value);
    }

    /**
     * Return the primary component of a collation element.
     * @param order the collation element
     * @return the element's primary component
     */
    public static final int primaryOrder(int order)
    {
        order &= RBCollationTables.PRIMARYORDERMASK;
        return (order >>> RBCollationTables.PRIMARYORDERSHIFT);
    }
    /**
     * Return the secondary component of a collation element.
     * @param order the collation element
     * @return the element's secondary component
     */
    public static final short secondaryOrder(int order)
    {
        order = order & RBCollationTables.SECONDARYORDERMASK;
        return ((short)(order >> RBCollationTables.SECONDARYORDERSHIFT));
    }
    /**
     * Return the tertiary component of a collation element.
     * @param order the collation element
     * @return the element's tertiary component
     */
    public static final short tertiaryOrder(int order)
    {
        return ((short)(order &= RBCollationTables.TERTIARYORDERMASK));
    }

    /**
     *  Get the comparison order in the desired strength.  Ignore the other
     *  differences.
     *  @param order The order value
     */
    final int strengthOrder(int order)
    {
        int s = owner.getStrength();
        if (s == Collator.PRIMARY)
        {
            order &= RBCollationTables.PRIMARYDIFFERENCEONLY;
        } else if (s == Collator.SECONDARY)
        {
            order &= RBCollationTables.SECONDARYDIFFERENCEONLY;
        }
        return order;
    }

    /**
     * Sets the iterator to point to the collation element corresponding to
     * the specified character (the parameter is a CHARACTER offset in the
     * original string, not an offset into its corresponding sequence of
     * collation elements).  The value returned by the next call to next()
     * will be the collation element corresponding to the specified position
     * in the text.  If that position is in the middle of a contracting
     * character sequence, the result of the next call to next() is the
     * collation element for that sequence.  This means that getOffset()
     * is not guaranteed to return the same value as was passed to a preceding
     * call to setOffset().
     *
     * @param newOffset The new character offset into the original text.
     * @since 1.2
     */
    @SuppressWarnings("deprecation") // getBeginIndex, getEndIndex and setIndex are deprecated
    public void setOffset(int newOffset)
    {
        if (text != null) {
            if (newOffset < text.getBeginIndex()
                || newOffset >= text.getEndIndex()) {
                    text.setIndexOnly(newOffset);
            } else {
                int c = text.setIndex(newOffset);

                // if the desired character isn't used in a contracting character
                // sequence, bypass all the backing-up logic-- we're sitting on
                // the right character already
                if (ordering.usedInContractSeq(c)) {
                    // walk backwards through the string until we see a character
                    // that DOESN'T participate in a contracting character sequence
                    while (ordering.usedInContractSeq(c)) {
                        c = text.previous();
                    }
                    // now walk forward using this object's next() method until
                    // we pass the starting point and set our current position
                    // to the beginning of the last "character" before or at
                    // our starting position
                    int last = text.getIndex();
                    while (text.getIndex() <= newOffset) {
                        last = text.getIndex();
                        next();
                    }
                    text.setIndexOnly(last);
                    // we don't need this, since last is the last index
                    // that is the starting of the contraction which encompass
                    // newOffset
                    // text.previous();
                }
            }
        }
        buffer = null;
        expIndex = 0;
        swapOrder = 0;
    }

    /**
     * Returns the character offset in the original text corresponding to the next
     * collation element.  (That is, getOffset() returns the position in the text
     * corresponding to the collation element that will be returned by the next
     * call to next().)  This value will always be the index of the FIRST character
     * corresponding to the collation element (a contracting character sequence is
     * when two or more characters all correspond to the same collation element).
     * This means if you do setOffset(x) followed immediately by getOffset(), getOffset()
     * won't necessarily return x.
     *
     * @return The character offset in the original text corresponding to the collation
     * element that will be returned by the next call to next().
     * @since 1.2
     */
    public int getOffset()
    {
        return (text != null) ? text.getIndex() : 0;
    }


    /**
     * Return the maximum length of any expansion sequences that end
     * with the specified comparison order.
     * @param order a collation order returned by previous or next.
     * @return the maximum length of any expansion sequences ending
     *         with the specified order.
     * @since 1.2
     */
    public int getMaxExpansion(int order)
    {
        return ordering.getMaxExpansion(order);
    }

    /**
     * Set a new string over which to iterate.
     *
     * @param source  the new source text
     * @since 1.2
     */
    public void setText(String source)
    {
        buffer = null;
        swapOrder = 0;
        expIndex = 0;
        NormalizerBase.Mode mode =
            CollatorUtilities.toNormalizerMode(owner.getDecomposition());
        if (text == null) {
            text = new NormalizerBase(source, mode);
        } else {
            text.setMode(mode);
            text.setText(source);
        }
    }

    /**
     * Set a new string over which to iterate.
     *
     * @param source  the new source text.
     * @since 1.2
     */
    public void setText(CharacterIterator source)
    {
        buffer = null;
        swapOrder = 0;
        expIndex = 0;
        NormalizerBase.Mode mode =
            CollatorUtilities.toNormalizerMode(owner.getDecomposition());
        if (text == null) {
            text = new NormalizerBase(source, mode);
        } else {
            text.setMode(mode);
            text.setText(source);
        }
    }

    //============================================================
    // privates
    //============================================================

    /**
     * Determine if a character is a Thai vowel (which sorts after
     * its base consonant).
     */
    private static final boolean isThaiPreVowel(int ch) {
        return (ch >= 0x0e40) && (ch <= 0x0e44);
    }

    /**
     * Determine if a character is a Thai base consonant
     */
    private static final boolean isThaiBaseConsonant(int ch) {
        return (ch >= 0x0e01) && (ch <= 0x0e2e);
    }

    /**
     * Determine if a character is a Lao vowel (which sorts after
     * its base consonant).
     */
    private static final boolean isLaoPreVowel(int ch) {
        return (ch >= 0x0ec0) && (ch <= 0x0ec4);
    }

    /**
     * Determine if a character is a Lao base consonant
     */
    private static final boolean isLaoBaseConsonant(int ch) {
        return (ch >= 0x0e81) && (ch <= 0x0eae);
    }

    /**
     * This method produces a buffer which contains the collation
     * elements for the two characters, with colFirst's values preceding
     * another character's.  Presumably, the other character precedes colFirst
     * in logical order (otherwise you wouldn't need this method would you?).
     * The assumption is that the other char's value(s) have already been
     * computed.  If this char has a single element it is passed to this
     * method as lastValue, and lastExpansion is null.  If it has an
     * expansion it is passed in lastExpansion, and colLastValue is ignored.
     */
    private int[] makeReorderedBuffer(int colFirst,
                                      int lastValue,
                                      int[] lastExpansion,
                                      boolean forward) {

        int[] result;

        int firstValue = ordering.getUnicodeOrder(colFirst);
        if (firstValue >= RuleBasedCollator.CONTRACTCHARINDEX) {
            firstValue = forward? nextContractChar(colFirst) : prevContractChar(colFirst);
        }

        int[] firstExpansion = null;
        if (firstValue >= RuleBasedCollator.EXPANDCHARINDEX) {
            firstExpansion = ordering.getExpandValueList(firstValue);
        }

        if (!forward) {
            int temp1 = firstValue;
            firstValue = lastValue;
            lastValue = temp1;
            int[] temp2 = firstExpansion;
            firstExpansion = lastExpansion;
            lastExpansion = temp2;
        }

        if (firstExpansion == null && lastExpansion == null) {
            result = new int [2];
            result[0] = firstValue;
            result[1] = lastValue;
        }
        else {
            int firstLength = firstExpansion==null? 1 : firstExpansion.length;
            int lastLength = lastExpansion==null? 1 : lastExpansion.length;
            result = new int[firstLength + lastLength];

            if (firstExpansion == null) {
                result[0] = firstValue;
            }
            else {
                System.arraycopy(firstExpansion, 0, result, 0, firstLength);
            }

            if (lastExpansion == null) {
                result[firstLength] = lastValue;
            }
            else {
                System.arraycopy(lastExpansion, 0, result, firstLength, lastLength);
            }
        }

        return result;
    }

    /**
     *  Check if a comparison order is ignorable.
     *  @return true if a character is ignorable, false otherwise.
     */
    static final boolean isIgnorable(int order)
    {
        return ((primaryOrder(order) == 0) ? true : false);
    }

    /**
     * Get the ordering priority of the next contracting character in the
     * string.
     * @param ch the starting character of a contracting character token
     * @return the next contracting character's ordering.  Returns NULLORDER
     * if the end of string is reached.
     */
    private int nextContractChar(int ch)
    {
        // First get the ordering of this single character,
        // which is always the first element in the list
        Vector<EntryPair> list = ordering.getContractValues(ch);
        EntryPair pair = list.firstElement();
        int order = pair.value;

        // find out the length of the longest contracting character sequence in the list.
        // There's logic in the builder code to make sure the longest sequence is always
        // the last.
        pair = list.lastElement();
        int maxLength = pair.entryName.length();

        // (the Normalizer is cloned here so that the seeking we do in the next loop
        // won't affect our real position in the text)
        NormalizerBase tempText = (NormalizerBase)text.clone();

        // extract the next maxLength characters in the string (we have to do this using the
        // Normalizer to ensure that our offsets correspond to those the rest of the
        // iterator is using) and store it in "fragment".
        tempText.previous();
        key.setLength(0);
        int c = tempText.next();
        while (maxLength > 0 && c != NormalizerBase.DONE) {
            if (Character.isSupplementaryCodePoint(c)) {
                key.append(Character.toChars(c));
                maxLength -= 2;
            } else {
                key.append((char)c);
                --maxLength;
            }
            c = tempText.next();
        }
        String fragment = key.toString();
        // now that we have that fragment, iterate through this list looking for the
        // longest sequence that matches the characters in the actual text.  (maxLength
        // is used here to keep track of the length of the longest sequence)
        // Upon exit from this loop, maxLength will contain the length of the matching
        // sequence and order will contain the collation-element value corresponding
        // to this sequence
        maxLength = 1;
        for (int i = list.size() - 1; i > 0; i--) {
            pair = list.elementAt(i);
            if (!pair.fwd)
                continue;

            if (fragment.startsWith(pair.entryName) && pair.entryName.length()
                    > maxLength) {
                maxLength = pair.entryName.length();
                order = pair.value;
            }
        }

        // seek our current iteration position to the end of the matching sequence
        // and return the appropriate collation-element value (if there was no matching
        // sequence, we're already seeked to the right position and order already contains
        // the correct collation-element value for the single character)
        while (maxLength > 1) {
            c = text.next();
            maxLength -= Character.charCount(c);
        }
        return order;
    }

    /**
     * Get the ordering priority of the previous contracting character in the
     * string.
     * @param ch the starting character of a contracting character token
     * @return the next contracting character's ordering.  Returns NULLORDER
     * if the end of string is reached.
     */
    private int prevContractChar(int ch)
    {
        // This function is identical to nextContractChar(), except that we've
        // switched things so that the next() and previous() calls on the Normalizer
        // are switched and so that we skip entry pairs with the fwd flag turned on
        // rather than off.  Notice that we still use append() and startsWith() when
        // working on the fragment.  This is because the entry pairs that are used
        // in reverse iteration have their names reversed already.
        Vector<EntryPair> list = ordering.getContractValues(ch);
        EntryPair pair = list.firstElement();
        int order = pair.value;

        pair = list.lastElement();
        int maxLength = pair.entryName.length();

        NormalizerBase tempText = (NormalizerBase)text.clone();

        tempText.next();
        key.setLength(0);
        int c = tempText.previous();
        while (maxLength > 0 && c != NormalizerBase.DONE) {
            if (Character.isSupplementaryCodePoint(c)) {
                key.append(Character.toChars(c));
                maxLength -= 2;
            } else {
                key.append((char)c);
                --maxLength;
            }
            c = tempText.previous();
        }
        String fragment = key.toString();

        maxLength = 1;
        for (int i = list.size() - 1; i > 0; i--) {
            pair = list.elementAt(i);
            if (pair.fwd)
                continue;

            if (fragment.startsWith(pair.entryName) && pair.entryName.length()
                    > maxLength) {
                maxLength = pair.entryName.length();
                order = pair.value;
            }
        }

        while (maxLength > 1) {
            c = text.previous();
            maxLength -= Character.charCount(c);
        }
        return order;
    }

    static final int UNMAPPEDCHARVALUE = 0x7FFF0000;

    private NormalizerBase text = null;
    private int[] buffer = null;
    private int expIndex = 0;
    private StringBuffer key = new StringBuffer(5);
    private int swapOrder = 0;
    private RBCollationTables ordering;
    private RuleBasedCollator owner;
}
