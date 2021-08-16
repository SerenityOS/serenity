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

package jdk.internal.icu.impl;

import jdk.internal.icu.text.Replaceable;
import jdk.internal.icu.text.ReplaceableString;
import jdk.internal.icu.text.UCharacterIterator;

/**
 * DLF docs must define behavior when Replaceable is mutated underneath
 * the iterator.
 *
 * This and ICUCharacterIterator share some code, maybe they should share
 * an implementation, or the common state and implementation should be
 * moved up into UCharacterIterator.
 *
 * What are first, last, and getBeginIndex doing here?!?!?!
 */
public class ReplaceableUCharacterIterator extends UCharacterIterator {

    // public constructor ------------------------------------------------------

    /**
     * Public constructor
     * @param str text which the iterator will be based on
     */
    public ReplaceableUCharacterIterator(String str){
        if(str==null){
            throw new IllegalArgumentException();
        }
        this.replaceable  = new ReplaceableString(str);
        this.currentIndex = 0;
    }

    /**
     * Public constructor
     * @param buf buffer of text on which the iterator will be based
     */
    public ReplaceableUCharacterIterator(StringBuffer buf){
        if(buf==null){
            throw new IllegalArgumentException();
        }
        this.replaceable  = new ReplaceableString(buf);
        this.currentIndex = 0;
    }

    // public methods ----------------------------------------------------------

    /**
     * Creates a copy of this iterator, does not clone the underlying
     * <code>Replaceable</code>object
     * @return copy of this iterator
     */
    public Object clone(){
        try {
          return super.clone();
        } catch (CloneNotSupportedException e) {
            return null; // never invoked
        }
    }

    /**
     * Returns the current UTF16 character.
     * @return current UTF16 character
     */
    public int current(){
        if (currentIndex < replaceable.length()) {
            return replaceable.charAt(currentIndex);
        }
        return DONE;
    }

    /**
     * Returns the length of the text
     * @return length of the text
     */
    public int getLength(){
        return replaceable.length();
    }

    /**
     * Gets the current currentIndex in text.
     * @return current currentIndex in text.
     */
    public int getIndex(){
        return currentIndex;
    }

    /**
     * Returns next UTF16 character and increments the iterator's currentIndex by 1.
     * If the resulting currentIndex is greater or equal to the text length, the
     * currentIndex is reset to the text length and a value of DONECODEPOINT is
     * returned.
     * @return next UTF16 character in text or DONE if the new currentIndex is off the
     *         end of the text range.
     */
    public int next(){
        if (currentIndex < replaceable.length()) {
            return replaceable.charAt(currentIndex++);
        }
        return DONE;
    }


    /**
     * Returns previous UTF16 character and decrements the iterator's currentIndex by
     * 1.
     * If the resulting currentIndex is less than 0, the currentIndex is reset to 0 and a
     * value of DONECODEPOINT is returned.
     * @return next UTF16 character in text or DONE if the new currentIndex is off the
     *         start of the text range.
     */
    public int previous(){
        if (currentIndex > 0) {
            return replaceable.charAt(--currentIndex);
        }
        return DONE;
    }

    /**
     * Sets the currentIndex to the specified currentIndex in the text and returns that
     * single UTF16 character at currentIndex.
     * This assumes the text is stored as 16-bit code units.
     * @param currentIndex the currentIndex within the text.
     * @exception IllegalArgumentException is thrown if an invalid currentIndex is
     *            supplied. i.e. currentIndex is out of bounds.
     */
    public void setIndex(int currentIndex) {
        if (currentIndex < 0 || currentIndex > replaceable.length()) {
            throw new IllegalArgumentException();
        }
        this.currentIndex = currentIndex;
    }

    public int getText(char[] fillIn, int offset){
        int length = replaceable.length();
        if(offset < 0 || offset + length > fillIn.length){
            throw new IndexOutOfBoundsException(Integer.toString(length));
        }
        replaceable.getChars(0,length,fillIn,offset);
        return length;
    }

    // private data members ----------------------------------------------------

    /**
     * Replaceable object
     */
    private Replaceable replaceable;
    /**
     * Current currentIndex
     */
    private int currentIndex;

}
