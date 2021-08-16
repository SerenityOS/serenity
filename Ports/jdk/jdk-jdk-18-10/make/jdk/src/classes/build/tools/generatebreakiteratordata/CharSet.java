/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright IBM Corp. 1996 - 2002 - All Rights Reserved
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 */

package build.tools.generatebreakiteratordata;

import java.util.Arrays;
import java.util.Hashtable;

/**
 * An object representing a set of characters.  (This is a "set" in the
 * mathematical sense: an unduplicated list of characters on which set
 * operations such as union and intersection can be performed.)  The
 * set information is stored in compressed, optimized form: The object
 * contains an integer array with an even number of characters.  Each
 * pair of characters represents a range of characters contained in the set
 * (a pair of the same character represents a single character).  The
 * characters are sorted in increasing order.
 */
class CharSet {
    /**
     * The structure containing the set information.  The characters
     * in this array are organized into pairs, each pair representing
     * a range of characters contained in the set
     */
    private int[] chars;

    //==========================================================================
    // parseString() and associated routines
    //==========================================================================
    /**
     * A cache which is used to speed up parseString() whenever it is
     * used to parse a description that has been parsed before
     */
    private static Hashtable<String, CharSet> expressionCache = null;

    /**
     * Builds a CharSet based on a textual description.  For the syntax of
     * the description, see the documentation of RuleBasedBreakIterator.
     * @see java.text.RuleBasedBreakIterator
     */
    public static CharSet parseString(String s) {
        CharSet result = null;

        // if "s" is in the expression cache, pull the result out
        // of the expresison cache
        if (expressionCache != null) {
            result = expressionCache.get(s);
        }

        // otherwise, use doParseString() to actually parse the string,
        // and then add a corresponding entry to the expression cache
        if (result == null) {
            result = doParseString(s);
            if (expressionCache == null) {
                expressionCache = new Hashtable<>();
            }
            expressionCache.put(s, result);
        }
        result = (CharSet)(result.clone());
        return result;
    }

    /**
     * This function is used by parseString() to actually parse the string
     */
    private static CharSet doParseString(String s) {
        CharSet result = new CharSet();
        int p = 0;

        boolean haveDash = false;
        boolean haveTilde = false;
        boolean wIsReal = false;
        int w = 0x0000;

        // for each character in the description...
        while (p < s.length()) {
            int c = s.codePointAt(p);

            // if it's an opening bracket...
            if (c == '[') {
                // flush the single-character cache
                if (wIsReal) {
                    result.internalUnion(new CharSet(w));
                }

                // locate the matching closing bracket
                int bracketLevel = 1;
                int q = p + 1;
                while (bracketLevel != 0) {
                    // if no matching bracket by end of string then...
                    if (q >= s.length()) {
                        throw new IllegalArgumentException("Parse error at position " + p + " in " + s);
                    }
                    int ch = s.codePointAt(q);
                    switch (ch) {
                    case '\\': // need to step over next character
                        ch = s.codePointAt(++q);
                        break;
                    case '[':
                        ++bracketLevel;
                        break;
                    case ']':
                        --bracketLevel;
                        break;
                    }
                    q += Character.charCount(ch);
                }
                --q;

                // call parseString() recursively to parse the text inside
                // the brackets, then either add or subtract the result from
                // our running result depending on whether or not the []
                // expresison was preceded by a ^
                if (!haveTilde) {
                    result.internalUnion(CharSet.parseString(s.substring(p + 1, q)));
                }
                else {
                    result.internalDifference(CharSet.parseString(s.substring(p + 1, q)));
                }
                haveTilde = false;
                haveDash = false;
                wIsReal = false;
                p = q + 1;
            }

            // if the character is a colon...
            else if (c == ':') {
                // flush the single-character cache
                if (wIsReal) {
                    result.internalUnion(new CharSet(w));
                }

                // locate the matching colon (and throw an error if there
                // isn't one)
                int q = s.indexOf(':', p + 1);
                if (q == -1) {
                    throw new IllegalArgumentException("Parse error at position " + p + " in " + s);
                }

                // use charSetForCategory() to parse the text in the colons,
                // and either add or substract the result from our running
                // result depending on whether the :: expression was
                // preceded by a ^
                if (!haveTilde) {
                    result.internalUnion(charSetForCategory(s.substring(p + 1, q)));
                }
                else {
                    result.internalDifference(charSetForCategory(s.substring(p + 1, q)));
                }

                // reset everything and advance to the next character
                haveTilde = false;
                haveDash = false;
                wIsReal = false;
                p = q + 1;
            }

            // if the character is a dash, set an appropriate flag
            else if (c == '-') {
                if (wIsReal) {
                    haveDash = true;
                }
                ++p;
            }

            // if the character is a caret, flush the single-character
            // cache and set an appropriate flag.  If the set is empty
            // (i.e., if the expression begins with ^), invert the set
            // (i.e., set it to include everything).  The idea here is
            // that a set that includes nothing but ^ expressions
            // means "everything but these things".
            else if (c == '^') {
                if (wIsReal) {
                    result.internalUnion(new CharSet(w));
                    wIsReal = false;
                }
                haveTilde = true;
                ++p;
                if (result.empty()) {
                    result.internalComplement();
                }
            }

            // throw an exception on an illegal character
            else if (c >= ' ' && c < '\u007f' && !Character.isLetter((char)c)
                     && !Character.isDigit((char)c) && c != '\\') {
                throw new IllegalArgumentException("Parse error at position " + p + " in " + s);
            }

            // otherwise, we end up here...
            else {
                // on a backslash, advance to the next character
                if (c == '\\') {
                    ++p;
                }

                // if the preceding character was a dash, this character
                // defines the end of a range.  Add or subtract that range
                // from the running result depending on whether or not it
                // was preceded by a ^
                if (haveDash) {
                    if (s.codePointAt(p) < w) {
                        throw new IllegalArgumentException("U+" +
                            Integer.toHexString(s.codePointAt(p))
                            + " is less than U+" + Integer.toHexString(w) + ".  Dash expressions "
                            + "can't have their endpoints in reverse order.");
                    }

                    int ch = s.codePointAt(p);
                    if (!haveTilde) {
                        result.internalUnion(new CharSet(w, ch));
                    }
                    else {
                        result.internalDifference(new CharSet(w, ch));
                    }
                    p += Character.charCount(ch);
                    haveDash = false;
                    haveTilde = false;
                    wIsReal = false;
                }

                // if the preceding character was a caret, remove this character
                // from the running result
                else if (haveTilde) {
                    w = s.codePointAt(p);
                    result.internalDifference(new CharSet(w));
                    p += Character.charCount(w);
                    haveTilde = false;
                    wIsReal = false;
                }

                // otherwise, flush the single-character cache and then
                // put this character into the cache
                else if (wIsReal) {
                    result.internalUnion(new CharSet(w));
                    w = s.codePointAt(p);
                    p += Character.charCount(w);
                    wIsReal = true;
                } else {
                    w = s.codePointAt(p);
                    p += Character.charCount(w);
                    wIsReal = true;
                }
            }
        }

        // finally, flush the single-character cache one last time
        if (wIsReal) {
            result.internalUnion(new CharSet(w));
        }

        return result;
    }

    /**
     * Creates a CharSet containing all the characters in a particular
     * Unicode category.  The text is either a two-character code from
     * the Unicode database or a single character that begins one or more
     * two-character codes.
     */
    private static CharSet charSetForCategory(String category) {
        // throw an exception if we have anything other than one or two
        // characters inside the colons
        if (category.length() == 0 || category.length() >= 3) {
            throw new IllegalArgumentException("Invalid character category: " + category);
        }

        // if we have two characters, search the category map for that code
        // and either construct and return a CharSet from the data in the
        // category map or throw an exception
        if (category.length() == 2) {
            for (int i = 0; i < CharacterCategory.categoryNames.length; i++) {
                if (CharacterCategory.categoryNames[i].equals(category)) {
                    return new CharSet(CharacterCategory.getCategoryMap(i));
                }
            }
            throw new IllegalArgumentException("Invalid character category: " + category);
        }

        // if we have one character, search the category map for codes beginning
        // with that letter, and union together all of the matching sets that
        // we find (or throw an exception if there are no matches)
        else if (category.length() == 1) {
            CharSet result = new CharSet();
            for (int i = 0; i < CharacterCategory.categoryNames.length; i++) {
                if (CharacterCategory.categoryNames[i].startsWith(category)) {
                    result = result.union(new CharSet(CharacterCategory.getCategoryMap(i)));
                }
            }
            if (result.empty()) {
                throw new IllegalArgumentException("Invalid character category: " + category);
            }
            else {
                return result;
            }
        }
        return new CharSet(); // should never get here, but to make the compiler happy...
    }

    /**
     * Returns a copy of CharSet's expression cache and sets CharSet's
     * expression cache to empty.
     */
    public static Hashtable<String, CharSet> releaseExpressionCache() {
        Hashtable<String, CharSet> result = expressionCache;
        expressionCache = null;
        return result;
    }

    //==========================================================================
    // CharSet manipulation
    //==========================================================================
    /**
     * Creates an empty CharSet.
     */
    public CharSet() {
        chars = new int[0];
    }

    /**
     * Creates a CharSet containing a single character.
     * @param c The character to put into the CharSet
     */
    public CharSet(int c) {
        chars = new int[2];
        chars[0] = c;
        chars[1] = c;
    }

    /**
     * Creates a CharSet containing a range of characters.
     * @param lo The lowest-numbered character to include in the range
     * @param hi The highest-numbered character to include in the range
     */
    public CharSet(int lo, int hi) {
        chars = new int[2];
        if (lo <= hi) {
            chars[0] = lo;
            chars[1] = hi;
        }
        else {
            chars[0] = hi;
            chars[1] = lo;
        }
    }

    /**
     * Creates a CharSet, initializing it from the internal storage
     * of another CharSet (this function performs no error checking
     * on "chars", so if it's malformed, undefined behavior will result)
     */
    private CharSet(int[] chars) {
        this.chars = chars;
    }

    /**
     * Returns a CharSet representing the union of two CharSets.
     */
    public CharSet union(CharSet that) {
        return new CharSet(doUnion(that.chars));
    }

    /**
     * Adds the characters in "that" to this CharSet
     */
    private void internalUnion(CharSet that) {
        chars = doUnion(that.chars);
    }

    /**
     * The actual implementation of the union functions
     */
    private int[] doUnion(int[] c2) {
        int[] result = new int[chars.length+c2.length];

        int i = 0;
        int j = 0;
        int index = 0;

        // consider all the characters in both strings
        while (i < chars.length && j < c2.length) {
            int ub;

            // the first character in the result is the lower of the
            // starting characters of the two strings, and "ub" gets
            // set to the upper bound of that range
            if (chars[i] < c2[j]) {
                result[index++] = chars[i];
                ub = chars[++i];
            }
            else {
                result[index++] = c2[j];
                ub = c2[++j];
            }

            // for as long as one of our two pointers is pointing to a range's
            // end point, or i is pointing to a character that is less than
            // "ub" plus one (the "plus one" stitches touching ranges together)...
            while (i % 2 == 1 ||
                   j % 2 == 1 ||
                   (i < chars.length && chars[i] <= ub + 1)) {

                // advance i to the first character that is greater than
                // "ub" plus one
                while (i < chars.length && chars[i] <= ub + 1) {
                    ++i;
                }

                // if i points to the endpoint of a range, update "ub"
                // to that character, or if i points to the start of
                // a range and the endpoint of the preceding range is
                // greater than "ub", update "up" to _that_ character
                if (i % 2 == 1) {
                    ub = chars[i];
                }
                else if (i > 0 && chars[i - 1] > ub) {
                    ub = chars[i - 1];
                }

                // now advance j to the first character that is greater
                // that "ub" plus one
                while (j < c2.length && c2[j] <= ub + 1) {
                    ++j;
                }

                // if j points to the endpoint of a range, update "ub"
                // to that character, or if j points to the start of
                // a range and the endpoint of the preceding range is
                // greater than "ub", update "up" to _that_ character
                if (j % 2 == 1) {
                    ub = c2[j];
                }
                else if (j > 0 && c2[j - 1] > ub) {
                    ub = c2[j - 1];
                }
            }
            // when we finally fall out of this loop, we will have stitched
            // together a series of ranges that overlap or touch, i and j
            // will both point to starting points of ranges, and "ub" will
            // be the endpoint of the range we're working on.  Write "ub"
            // to the result
            result[index++] = ub;

        // loop back around to create the next range in the result
        }

        // we fall out to here when we've exhausted all the characters in
        // one of the operands.  We can append all of the remaining characters
        // in the other operand without doing any extra work.
        if (i < chars.length) {
            for (int k = i; k < chars.length; k++) {
                result[index++] = chars[k];
            }
        }
        if (j < c2.length) {
            for (int k = j; k < c2.length; k++) {
                result[index++] = c2[k];
            }
        }

        if (result.length > index) {
            int[] tmpbuf = new int[index];
            System.arraycopy(result, 0, tmpbuf, 0, index);
            return tmpbuf;
        }

        return result;
    }

    /**
     * Returns the intersection of two CharSets.
     */
    public CharSet intersection(CharSet that) {
        return new CharSet(doIntersection(that.chars));
    }

    /**
     * Removes from this CharSet any characters that aren't also in "that"
     */
    private void internalIntersection(CharSet that) {
        chars = doIntersection(that.chars);
    }

    /**
     * The internal implementation of the two intersection functions
     */
    private int[] doIntersection(int[] c2) {
        int[] result = new int[chars.length+c2.length];

        int i = 0;
        int j = 0;
        int oldI;
        int oldJ;
        int index = 0;

        // iterate until we've exhausted one of the operands
        while (i < chars.length && j < c2.length) {

            // advance j until it points to a character that is larger than
            // the one i points to.  If this is the beginning of a one-
            // character range, advance j to point to the end
            if (i < chars.length && i % 2 == 0) {
                while (j < c2.length && c2[j] < chars[i]) {
                    ++j;
                }
                if (j < c2.length && j % 2 == 0 && c2[j] == chars[i]) {
                    ++j;
                }
            }

            // if j points to the endpoint of a range, save the current
            // value of i, then advance i until it reaches a character
            // which is larger than the character pointed at
            // by j.  All of the characters we've advanced over (except
            // the one currently pointed to by i) are added to the result
            oldI = i;
            while (j % 2 == 1 && i < chars.length && chars[i] <= c2[j]) {
                ++i;
            }
            for (int k = oldI; k < i; k++) {
                result[index++] = chars[k];
            }

            // if i points to the endpoint of a range, save the current
            // value of j, then advance j until it reaches a character
            // which is larger than the character pointed at
            // by i.  All of the characters we've advanced over (except
            // the one currently pointed to by i) are added to the result
            oldJ = j;
            while (i % 2 == 1 && j < c2.length && c2[j] <= chars[i]) {
                ++j;
            }
            for (int k = oldJ; k < j; k++) {
                result[index++] = c2[k];
            }

            // advance i until it points to a character larger than j
            // If it points at the beginning of a one-character range,
            // advance it to the end of that range
            if (j < c2.length && j % 2 == 0) {
                while (i < chars.length && chars[i] < c2[j]) {
                    ++i;
                }
                if (i < chars.length && i % 2 == 0 && c2[j] == chars[i]) {
                    ++i;
                }
            }
        }

        if (result.length > index) {
            int[] tmpbuf = new int[index];
            System.arraycopy(result, 0, tmpbuf, 0, index);
            return tmpbuf;
        }

        return result;
    }

    /**
     * Returns a CharSet containing all the characters in "this" that
     * aren't also in "that"
     */
    public CharSet difference(CharSet that) {
        return new CharSet(doIntersection(that.doComplement()));
    }

    /**
     * Removes from "this" all the characters that are also in "that"
     */
    private void internalDifference(CharSet that) {
        chars = doIntersection(that.doComplement());
    }

    /**
     * Returns a CharSet containing all the characters which are not
     * in "this"
     */
    public CharSet complement() {
        return new CharSet(doComplement());
    }

    /**
     * Complements "this".  All the characters it contains are removed,
     * and all the characters it doesn't contain are added.
     */
    private void internalComplement() {
        chars = doComplement();
    }

    /**
     * The internal implementation function for the complement routines
     */
    private int[] doComplement() {
        // the complement of an empty CharSet is one containing everything
        if (empty()) {
            int[] result = new int[2];
            result[0] = 0x0000;
            result[1] = 0x10FFFF;
            return result;
        }

        int[] result = new int[chars.length+2];

        int i = 0;
        int index = 0;

        // the result begins with \u0000 unless the original CharSet does
        if (chars[0] != 0x0000) {
            result[index++] = 0x0000;
        }

        // walk through the characters in this CharSet.  Append a pair of
        // characters the first of which is one less than the first
        // character we see and the second of which is one plus the second
        // character we see (don't write the first character if it's \u0000,
        // and don't write the second character if it's \uffff.
        while (i < chars.length) {
            if (chars[i] != 0x0000) {
                result[index++] = chars[i] - 1;
            }
            if (chars[i + 1] != 0x10FFFF) {
                result[index++] = chars[i + 1] + 1;
            }
            i += 2;
        }

        // add 0x10ffff to the end of the result, unless it was in
        // the original set
        if (chars[i-1] != 0x10FFFF) {
            result[index++] = 0x10FFFF;
        }

        if (result.length > index) {
            int[] tmpbuf = new int[index];
            System.arraycopy(result, 0, tmpbuf, 0, index);
            return tmpbuf;
        }

        return result;
    }

    /**
     * Returns true if this CharSet contains the specified character
     * @param c The character we're testing for set membership
     */
    public boolean contains(int c) {
        // search for the first range endpoint that is greater than or
        // equal to c
        int i = 1;
        while (i < chars.length && chars[i] < c) {
            i += 2;
        }

        // if we've walked off the end, we don't contain c
        if (i == chars.length) {
            return false;
        }

        // otherwise, we contain c if the beginning of the range is less
        // than or equal to c
        return chars[i - 1] <= c;
    }

    /**
     * Returns true if "that" is another instance of CharSet containing
     * the exact same characters as this one
     */
    public boolean equals(Object that) {
        return (that instanceof CharSet) && Arrays.equals(chars, ((CharSet)that).chars);
    }

    /**
     * Returns the hash code for this set of characters
     */
    public int hashCode() {
       return Arrays.hashCode(chars);
    }

    /**
     * Creates a new CharSet that is equal to this one
     */
    public Object clone() {
        return new CharSet(chars);
    }

    /**
     * Returns true if this CharSet contains no characters
     */
    public boolean empty() {
        return chars.length == 0;
    }

    /**
     * Returns a textual representation of this CharSet.  If the result
     * of calling this function is passed to CharSet.parseString(), it
     * will produce another CharSet that is equal to this one.
     */
    public String toString() {
        StringBuffer result = new StringBuffer();

        // the result begins with an opening bracket
        result.append('[');

        // iterate through the ranges in the CharSet
        for (int i = 0; i < chars.length; i += 2) {
            // for a range with the same beginning and ending point,
            // output that character
            if (chars[i] == chars[i + 1]) {
                result.append("0x");
                result.append(Integer.toHexString(chars[i]));
            }

            // otherwise, output the start and end points of the range
            // separated by a dash
            else {
                result.append("0x");
                result.append(Integer.toHexString(chars[i]));
                result.append("-0x");
                result.append(Integer.toHexString(chars[i + 1]));
            }
        }

        // the result ends with a closing bracket
        result.append(']');
        return result.toString();
    }

    /**
     * Returns an integer array representing the contents of this CharSet
     * in the same form in which they're stored internally: as pairs
     * of characters representing the start and end points of ranges
     */
    public int[] getRanges() {
        return chars;
    }

    /**
     * Returns an Enumeration that will return the ranges of characters
     * contained in this CharSet one at a time
     */
    public Enumeration getChars() {
        return new Enumeration(this);
    }

    //==========================================================================
    // CharSet.Enumeration
    //==========================================================================

    /**
     * An Enumeration that can be used to extract the character ranges
     * from a CharSet one at a time
     */
    public class Enumeration implements java.util.Enumeration<int[]> {
        /**
         * Initializes a CharSet.Enumeration
         */
        Enumeration(CharSet cs) {
            this.chars = cs.chars;
            p = 0;
        }

        /**
         * Returns true if the enumeration hasn't yet returned
         * all the ranges in the CharSet
         */
        public boolean hasMoreElements() {
            return p < chars.length;
        }

        /**
         * Returns the next range in the CarSet
         */
        public int[] nextElement() {
            int[] result = new int[2];
            result[0] = chars[p++];
            result[1] = chars[p++];
            return result;
        }

        int p;
        int[] chars;
    }
}
