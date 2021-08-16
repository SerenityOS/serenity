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
 ******************************************************************************
 * Copyright (C) 1996-2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 ******************************************************************************
 */

package jdk.internal.icu.impl;

import jdk.internal.icu.text.UTF16;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * Trie implementation which stores data in char, 16 bits.
 * @author synwee
 * @see com.ibm.icu.impl.Trie
 * @since release 2.1, Jan 01 2002
 */

 // note that i need to handle the block calculations later, since chartrie
 // in icu4c uses the same index array.
public class CharTrie extends Trie
{
    // public constructors ---------------------------------------------

    /**
     * <p>Creates a new Trie with the settings for the trie data.</p>
     * <p>Unserialize the 32-bit-aligned input stream and use the data for the
     * trie.</p>
     * @param inputStream file input stream to a ICU data file, containing
     *                    the trie
     * @param dataManipulate object which provides methods to parse the char
     *                        data
     * @throws IOException thrown when data reading fails
     * @draft 2.1
     */
    public CharTrie(InputStream inputStream,
                    DataManipulate dataManipulate) throws IOException
    {
        super(inputStream, dataManipulate);

        if (!isCharTrie()) {
            throw new IllegalArgumentException(
                               "Data given does not belong to a char trie.");
        }
    }

    // public methods --------------------------------------------------

    /**
     * Gets the value associated with the codepoint.
     * If no value is associated with the codepoint, a default value will be
     * returned.
     * @param ch codepoint
     * @return offset to data
     */
    public final char getCodePointValue(int ch)
    {
        int offset;

        // fastpath for U+0000..U+D7FF
        if(0 <= ch && ch < UTF16.LEAD_SURROGATE_MIN_VALUE) {
            // copy of getRawOffset()
            offset = (m_index_[ch >> INDEX_STAGE_1_SHIFT_] << INDEX_STAGE_2_SHIFT_)
                    + (ch & INDEX_STAGE_3_MASK_);
            return m_data_[offset];
        }

        // handle U+D800..U+10FFFF
        offset = getCodePointOffset(ch);

        // return -1 if there is an error, in this case we return the default
        // value: m_initialValue_
        return (offset >= 0) ? m_data_[offset] : m_initialValue_;
    }

    /**
     * Gets the value to the data which this lead surrogate character points
     * to.
     * Returned data may contain folding offset information for the next
     * trailing surrogate character.
     * This method does not guarantee correct results for trail surrogates.
     * @param ch lead surrogate character
     * @return data value
     */
    public final char getLeadValue(char ch)
    {
       return m_data_[getLeadOffset(ch)];
    }

    // protected methods -----------------------------------------------

    /**
     * <p>Parses the input stream and stores its trie content into a index and
     * data array</p>
     * @param inputStream data input stream containing trie data
     * @exception IOException thrown when data reading fails
     */
    protected final void unserialize(InputStream inputStream)
                                                throws IOException
    {
        DataInputStream input = new DataInputStream(inputStream);
        int indexDataLength = m_dataOffset_ + m_dataLength_;
        m_index_ = new char[indexDataLength];
        for (int i = 0; i < indexDataLength; i ++) {
            m_index_[i] = input.readChar();
        }
        m_data_           = m_index_;
        m_initialValue_   = m_data_[m_dataOffset_];
    }

    /**
     * Gets the offset to the data which the surrogate pair points to.
     * @param lead lead surrogate
     * @param trail trailing surrogate
     * @return offset to data
     * @draft 2.1
     */
    protected final int getSurrogateOffset(char lead, char trail)
    {
        if (m_dataManipulate_ == null) {
            throw new NullPointerException(
                             "The field DataManipulate in this Trie is null");
        }

        // get fold position for the next trail surrogate
        int offset = m_dataManipulate_.getFoldingOffset(getLeadValue(lead));

        // get the real data from the folded lead/trail units
        if (offset > 0) {
            return getRawOffset(offset, (char)(trail & SURROGATE_MASK_));
        }

        // return -1 if there is an error, in this case we return the default
        // value: m_initialValue_
        return -1;
    }

    // private data members --------------------------------------------

    /**
     * Default value
     */
    private char m_initialValue_;
    /**
     * Array of char data
     */
    private char m_data_[];
}
