/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text;

import java.text.CharacterIterator;

/**
 * A segment of a character array representing a fragment
 * of text.  It should be treated as immutable even though
 * the array is directly accessible.  This gives fast access
 * to fragments of text without the overhead of copying
 * around characters.  This is effectively an unprotected
 * String.
 * <p>
 * The Segment implements the java.text.CharacterIterator
 * interface to support use with the i18n support without
 * copying text into a string.
 *
 * @author  Timothy Prinzing
 */
public class Segment implements Cloneable, CharacterIterator, CharSequence {

    /**
     * This is the array containing the text of
     * interest.  This array should never be modified;
     * it is available only for efficiency.
     */
    public char[] array;

    /**
     * This is the offset into the array that
     * the desired text begins.
     */
    public int offset;

    /**
     * This is the number of array elements that
     * make up the text of interest.
     */
    public int count;

    /**
     * Whether the array is a copy of data or not.
     */
    boolean copy;

    private boolean partialReturn;

    /**
     * Creates a new segment.
     */
    public Segment() {
        this(null, 0, 0);
    }

    /**
     * Creates a new segment referring to an existing array.
     *
     * @param array the array to refer to
     * @param offset the offset into the array
     * @param count the number of characters
     */
    public Segment(char[] array, int offset, int count) {
        this.array = array;
        this.offset = offset;
        this.count = count;
        partialReturn = false;
    }

    /**
     * Flag to indicate that partial returns are valid.  If the flag is true,
     * an implementation of the interface method Document.getText(position,length,Segment)
     * should return as much text as possible without making a copy.  The default
     * state of the flag is false which will cause Document.getText(position,length,Segment)
     * to provide the same return behavior it always had, which may or may not
     * make a copy of the text depending upon the request.
     *
     * @param p whether or not partial returns are valid.
     * @since 1.4
     */
    public void setPartialReturn(boolean p) {
        partialReturn = p;
    }

    /**
     * Flag to indicate that partial returns are valid.
     *
     * @return whether or not partial returns are valid.
     * @since 1.4
     */
    public boolean isPartialReturn() {
        return partialReturn;
    }

    /**
     * Converts a segment into a String.
     *
     * @return the string
     */
    public String toString() {
        if (array != null) {
            return new String(array, offset, count);
        }
        return "";
    }

    // --- CharacterIterator methods -------------------------------------

    /**
     * Sets the position to getBeginIndex() and returns the character at that
     * position.
     * @return the first character in the text, or DONE if the text is empty
     * @see #getBeginIndex
     * @since 1.3
     */
    public char first() {
        pos = offset;
        if (count != 0) {
            return array[pos];
        }
        return DONE;
    }

    /**
     * Sets the position to getEndIndex()-1 (getEndIndex() if the text is empty)
     * and returns the character at that position.
     * @return the last character in the text, or DONE if the text is empty
     * @see #getEndIndex
     * @since 1.3
     */
    public char last() {
        pos = offset + count;
        if (count != 0) {
            pos -= 1;
            return array[pos];
        }
        return DONE;
    }

    /**
     * Gets the character at the current position (as returned by getIndex()).
     * @return the character at the current position or DONE if the current
     * position is off the end of the text.
     * @see #getIndex
     * @since 1.3
     */
    public char current() {
        if (count != 0 && pos < offset + count) {
            return array[pos];
        }
        return DONE;
    }

    /**
     * Increments the iterator's index by one and returns the character
     * at the new index.  If the resulting index is greater or equal
     * to getEndIndex(), the current index is reset to getEndIndex() and
     * a value of DONE is returned.
     * @return the character at the new position or DONE if the new
     * position is off the end of the text range.
     * @since 1.3
     */
    public char next() {
        pos += 1;
        int end = offset + count;
        if (pos >= end) {
            pos = end;
            return DONE;
        }
        return current();
    }

    /**
     * Decrements the iterator's index by one and returns the character
     * at the new index. If the current index is getBeginIndex(), the index
     * remains at getBeginIndex() and a value of DONE is returned.
     * @return the character at the new position or DONE if the current
     * position is equal to getBeginIndex().
     * @since 1.3
     */
    public char previous() {
        if (pos == offset) {
            return DONE;
        }
        pos -= 1;
        return current();
    }

    /**
     * Sets the position to the specified position in the text and returns that
     * character.
     * @param position the position within the text.  Valid values range from
     * getBeginIndex() to getEndIndex().  An IllegalArgumentException is thrown
     * if an invalid value is supplied.
     * @return the character at the specified position or DONE if the specified position is equal to getEndIndex()
     * @since 1.3
     */
    public char setIndex(int position) {
        int end = offset + count;
        if ((position < offset) || (position > end)) {
            throw new IllegalArgumentException("bad position: " + position);
        }
        pos = position;
        if ((pos != end) && (count != 0)) {
            return array[pos];
        }
        return DONE;
    }

    /**
     * Returns the start index of the text.
     * @return the index at which the text begins.
     * @since 1.3
     */
    public int getBeginIndex() {
        return offset;
    }

    /**
     * Returns the end index of the text.  This index is the index of the first
     * character following the end of the text.
     * @return the index after the last character in the text
     * @since 1.3
     */
    public int getEndIndex() {
        return offset + count;
    }

    /**
     * Returns the current index.
     * @return the current index.
     * @since 1.3
     */
    public int getIndex() {
        return pos;
    }

    // --- CharSequence methods -------------------------------------

    /**
     * {@inheritDoc}
     * @since 1.6
     */
    public char charAt(int index) {
        if (index < 0
            || index >= count) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return array[offset + index];
    }

    /**
     * {@inheritDoc}
     * @since 1.6
     */
    public int length() {
        return count;
    }

    /**
     * {@inheritDoc}
     * @since 1.6
     */
    public CharSequence subSequence(int start, int end) {
        if (start < 0) {
            throw new StringIndexOutOfBoundsException(start);
        }
        if (end > count) {
            throw new StringIndexOutOfBoundsException(end);
        }
        if (start > end) {
            throw new StringIndexOutOfBoundsException(end - start);
        }
        Segment segment = new Segment();
        segment.array = this.array;
        segment.offset = this.offset + start;
        segment.count = end - start;
        return segment;
    }

    /**
     * Creates a shallow copy.
     *
     * @return the copy
     */
    public Object clone() {
        Object o;
        try {
            o = super.clone();
        } catch (CloneNotSupportedException cnse) {
            o = null;
        }
        return o;
    }

    private int pos;


}
