/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.font;

import java.text.CharacterIterator;

class CharArrayIterator implements CharacterIterator {

    private char[] chars;
    private int pos;
    private int begin;

    CharArrayIterator(char[] chars) {

        reset(chars, 0);
    }

    CharArrayIterator(char[] chars, int begin) {

        reset(chars, begin);
    }

    /**
     * Sets the position to getBeginIndex() and returns the character at that
     * position.
     * @return the first character in the text, or DONE if the text is empty
     * @see #getBeginIndex
     */
    public char first() {

        pos = 0;
        return current();
    }

    /**
     * Sets the position to getEndIndex()-1 (getEndIndex() if the text is empty)
     * and returns the character at that position.
     * @return the last character in the text, or DONE if the text is empty
     * @see #getEndIndex
     */
    public char last() {

        if (chars.length > 0) {
            pos = chars.length-1;
        }
        else {
            pos = 0;
        }
        return current();
    }

    /**
     * Gets the character at the current position (as returned by getIndex()).
     * @return the character at the current position or DONE if the current
     * position is off the end of the text.
     * @see #getIndex
     */
    public char current() {

        if (pos >= 0 && pos < chars.length) {
            return chars[pos];
        }
        else {
            return DONE;
        }
    }

    /**
     * Increments the iterator's index by one and returns the character
     * at the new index.  If the resulting index is greater or equal
     * to getEndIndex(), the current index is reset to getEndIndex() and
     * a value of DONE is returned.
     * @return the character at the new position or DONE if the new
     * position is off the end of the text range.
     */
    public char next() {

        if (pos < chars.length-1) {
            pos++;
            return chars[pos];
        }
        else {
            pos = chars.length;
            return DONE;
        }
    }

    /**
     * Decrements the iterator's index by one and returns the character
     * at the new index. If the current index is getBeginIndex(), the index
     * remains at getBeginIndex() and a value of DONE is returned.
     * @return the character at the new position or DONE if the current
     * position is equal to getBeginIndex().
     */
    public char previous() {

        if (pos > 0) {
            pos--;
            return chars[pos];
        }
        else {
            pos = 0;
            return DONE;
        }
    }

    /**
     * Sets the position to the specified position in the text and returns that
     * character.
     * @param position the position within the text.  Valid values range from
     * getBeginIndex() to getEndIndex().  An IllegalArgumentException is thrown
     * if an invalid value is supplied.
     * @return the character at the specified position or DONE if the specified position is equal to getEndIndex()
     */
    public char setIndex(int position) {

        position -= begin;
        if (position < 0 || position > chars.length) {
            throw new IllegalArgumentException("Invalid index");
        }
        pos = position;
        return current();
    }

    /**
     * Returns the start index of the text.
     * @return the index at which the text begins.
     */
    public int getBeginIndex() {
        return begin;
    }

    /**
     * Returns the end index of the text.  This index is the index of the first
     * character following the end of the text.
     * @return the index after the last character in the text
     */
    public int getEndIndex() {
        return begin+chars.length;
    }

    /**
     * Returns the current index.
     * @return the current index.
     */
    public int getIndex() {
        return begin+pos;
    }

    /**
     * Create a copy of this iterator
     * @return A copy of this
     */
    public Object clone() {
        CharArrayIterator c = new CharArrayIterator(chars, begin);
        c.pos = this.pos;
        return c;
    }

    void reset(char[] chars) {
        reset(chars, 0);
    }

    void reset(char[] chars, int begin) {

        this.chars = chars;
        this.begin = begin;
        pos = 0;
    }
}
