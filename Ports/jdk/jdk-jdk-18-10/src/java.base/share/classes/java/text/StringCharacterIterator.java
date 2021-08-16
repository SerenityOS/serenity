/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.text;

/**
 * {@code StringCharacterIterator} implements the
 * {@code CharacterIterator} protocol for a {@code String}.
 * The {@code StringCharacterIterator} class iterates over the
 * entire {@code String}.
 *
 * @see CharacterIterator
 * @since 1.1
 */

public final class StringCharacterIterator implements CharacterIterator
{
    private String text;
    private int begin;
    private int end;
    // invariant: begin <= pos <= end
    private int pos;

    /**
     * Constructs an iterator with an initial index of 0.
     *
     * @param text the {@code String} to be iterated over
     */
    public StringCharacterIterator(String text)
    {
        this(text, 0);
    }

    /**
     * Constructs an iterator with the specified initial index.
     *
     * @param  text   The String to be iterated over
     * @param  pos    Initial iterator position
     */
    public StringCharacterIterator(String text, int pos)
    {
    this(text, 0, text.length(), pos);
    }

    /**
     * Constructs an iterator over the given range of the given string, with the
     * index set at the specified position.
     *
     * @param  text   The String to be iterated over
     * @param  begin  Index of the first character
     * @param  end    Index of the character following the last character
     * @param  pos    Initial iterator position
     */
    public StringCharacterIterator(String text, int begin, int end, int pos) {
        if (text == null)
            throw new NullPointerException();
        this.text = text;

        if (begin < 0 || begin > end || end > text.length())
            throw new IllegalArgumentException("Invalid substring range");

        if (pos < begin || pos > end)
            throw new IllegalArgumentException("Invalid position");

        this.begin = begin;
        this.end = end;
        this.pos = pos;
    }

    /**
     * Reset this iterator to point to a new string.  This package-visible
     * method is used by other java.text classes that want to avoid allocating
     * new StringCharacterIterator objects every time their setText method
     * is called.
     *
     * @param  text   The String to be iterated over
     * @since 1.2
     */
    public void setText(String text) {
        if (text == null)
            throw new NullPointerException();
        this.text = text;
        this.begin = 0;
        this.end = text.length();
        this.pos = 0;
    }

    /**
     * Implements CharacterIterator.first() for String.
     * @see CharacterIterator#first
     */
    public char first()
    {
        pos = begin;
        return current();
    }

    /**
     * Implements CharacterIterator.last() for String.
     * @see CharacterIterator#last
     */
    public char last()
    {
        if (end != begin) {
            pos = end - 1;
        } else {
            pos = end;
        }
        return current();
     }

    /**
     * Implements CharacterIterator.setIndex() for String.
     * @see CharacterIterator#setIndex
     */
    public char setIndex(int p)
    {
    if (p < begin || p > end)
            throw new IllegalArgumentException("Invalid index");
        pos = p;
        return current();
    }

    /**
     * Implements CharacterIterator.current() for String.
     * @see CharacterIterator#current
     */
    public char current()
    {
        if (pos >= begin && pos < end) {
            return text.charAt(pos);
        }
        else {
            return DONE;
        }
    }

    /**
     * Implements CharacterIterator.next() for String.
     * @see CharacterIterator#next
     */
    public char next()
    {
        if (pos < end - 1) {
            pos++;
            return text.charAt(pos);
        }
        else {
            pos = end;
            return DONE;
        }
    }

    /**
     * Implements CharacterIterator.previous() for String.
     * @see CharacterIterator#previous
     */
    public char previous()
    {
        if (pos > begin) {
            pos--;
            return text.charAt(pos);
        }
        else {
            return DONE;
        }
    }

    /**
     * Implements CharacterIterator.getBeginIndex() for String.
     * @see CharacterIterator#getBeginIndex
     */
    public int getBeginIndex()
    {
        return begin;
    }

    /**
     * Implements CharacterIterator.getEndIndex() for String.
     * @see CharacterIterator#getEndIndex
     */
    public int getEndIndex()
    {
        return end;
    }

    /**
     * Implements CharacterIterator.getIndex() for String.
     * @see CharacterIterator#getIndex
     */
    public int getIndex()
    {
        return pos;
    }

    /**
     * Compares the equality of two StringCharacterIterator objects.
     * @param obj the StringCharacterIterator object to be compared with.
     * @return true if the given obj is the same as this
     * StringCharacterIterator object; false otherwise.
     */
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!(obj instanceof StringCharacterIterator that))
            return false;

        if (hashCode() != that.hashCode())
            return false;
        if (!text.equals(that.text))
            return false;
        if (pos != that.pos || begin != that.begin || end != that.end)
            return false;
        return true;
    }

    /**
     * Computes a hashcode for this iterator.
     * @return A hash code
     */
    public int hashCode()
    {
        return text.hashCode() ^ pos ^ begin ^ end;
    }

    /**
     * Creates a copy of this iterator.
     * @return A copy of this
     */
    public Object clone()
    {
        try {
            StringCharacterIterator other
            = (StringCharacterIterator) super.clone();
            return other;
        }
        catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

}
