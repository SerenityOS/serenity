/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
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
package sun.text;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.MissingResourceException;
import sun.text.CompactByteArray;
import sun.text.SupplementaryCharacterData;

/**
 * This is the class that represents the list of known words used by
 * DictionaryBasedBreakIterator.  The conceptual data structure used
 * here is a trie: there is a node hanging off the root node for every
 * letter that can start a word.  Each of these nodes has a node hanging
 * off of it for every letter that can be the second letter of a word
 * if this node is the first letter, and so on.  The trie is represented
 * as a two-dimensional array that can be treated as a table of state
 * transitions.  Indexes are used to compress this array, taking
 * advantage of the fact that this array will always be very sparse.
 */
class BreakDictionary {

    //=========================================================================
    // data members
    //=========================================================================

    /**
     * The version of the dictionary that was read in.
     */
    private static int supportedVersion = 1;

    /**
     * Maps from characters to column numbers.  The main use of this is to
     * avoid making room in the array for empty columns.
     */
    private CompactByteArray columnMap = null;
    private SupplementaryCharacterData supplementaryCharColumnMap = null;

    /**
     * The number of actual columns in the table
     */
    private int numCols;

    /**
     * Columns are organized into groups of 32.  This says how many
     * column groups.  (We could calculate this, but we store the
     * value to avoid having to repeatedly calculate it.)
     */
    private int numColGroups;

    /**
     * The actual compressed state table.  Each conceptual row represents
     * a state, and the cells in it contain the row numbers of the states
     * to transition to for each possible letter.  0 is used to indicate
     * an illegal combination of letters (i.e., the error state).  The
     * table is compressed by eliminating all the unpopulated (i.e., zero)
     * cells.  Multiple conceptual rows can then be doubled up in a single
     * physical row by sliding them up and possibly shifting them to one
     * side or the other so the populated cells don't collide.  Indexes
     * are used to identify unpopulated cells and to locate populated cells.
     */
    private short[] table = null;

    /**
     * This index maps logical row numbers to physical row numbers
     */
    private short[] rowIndex = null;

    /**
     * A bitmap is used to tell which cells in the comceptual table are
     * populated.  This array contains all the unique bit combinations
     * in that bitmap.  If the table is more than 32 columns wide,
     * successive entries in this array are used for a single row.
     */
    private int[] rowIndexFlags = null;

    /**
     * This index maps from a logical row number into the bitmap table above.
     * (This keeps us from storing duplicate bitmap combinations.)  Since there
     * are a lot of rows with only one populated cell, instead of wasting space
     * in the bitmap table, we just store a negative number in this index for
     * rows with one populated cell.  The absolute value of that number is
     * the column number of the populated cell.
     */
    private short[] rowIndexFlagsIndex = null;

    /**
     * For each logical row, this index contains a constant that is added to
     * the logical column number to get the physical column number
     */
    private byte[] rowIndexShifts = null;

    //=========================================================================
    // deserialization
    //=========================================================================

    BreakDictionary(String dictionaryName, byte[] dictionaryData) {
        try {
            setupDictionary(dictionaryName, dictionaryData);
        } catch (BufferUnderflowException bue) {
            MissingResourceException e;
            e = new MissingResourceException("Corrupted dictionary data",
                                             dictionaryName, "");
            e.initCause(bue);
            throw e;
        }
    }

    private void setupDictionary(String dictionaryName, byte[] dictionaryData) {
        ByteBuffer bb = ByteBuffer.wrap(dictionaryData);

        // check version
        int version = bb.getInt();
        if (version != supportedVersion) {
            throw new MissingResourceException("Dictionary version(" + version + ") is unsupported",
                                               dictionaryName, "");
        }

        // Check data size
        int len = bb.getInt();
        if (bb.position() + len != bb.limit()) {
            throw new MissingResourceException("Dictionary size is wrong: " + bb.limit(),
                                               dictionaryName, "");
        }

        // read in the column map for BMP characteres (this is serialized in
        // its internal form: an index array followed by a data array)
        len = bb.getInt();
        short[] temp = new short[len];
        for (int i = 0; i < len; i++) {
            temp[i] = bb.getShort();
        }
        len = bb.getInt();
        byte[] temp2 = new byte[len];
        bb.get(temp2);
        columnMap = new CompactByteArray(temp, temp2);

        // read in numCols and numColGroups
        numCols = bb.getInt();
        numColGroups = bb.getInt();

        // read in the row-number index
        len = bb.getInt();
        rowIndex = new short[len];
        for (int i = 0; i < len; i++) {
            rowIndex[i] = bb.getShort();
        }

        // load in the populated-cells bitmap: index first, then bitmap list
        len = bb.getInt();
        rowIndexFlagsIndex = new short[len];
        for (int i = 0; i < len; i++) {
            rowIndexFlagsIndex[i] = bb.getShort();
        }
        len = bb.getInt();
        rowIndexFlags = new int[len];
        for (int i = 0; i < len; i++) {
            rowIndexFlags[i] = bb.getInt();
        }

        // load in the row-shift index
        len = bb.getInt();
        rowIndexShifts = new byte[len];
        bb.get(rowIndexShifts);

        // load in the actual state table
        len = bb.getInt();
        table = new short[len];
        for (int i = 0; i < len; i++) {
            table[i] = bb.getShort();
        }

        // finally, prepare the column map for supplementary characters
        len = bb.getInt();
        int[] temp3 = new int[len];
        for (int i = 0; i < len; i++) {
            temp3[i] = bb.getInt();
        }
        assert bb.position() == bb.limit();

        supplementaryCharColumnMap = new SupplementaryCharacterData(temp3);
    }

    //=========================================================================
    // access to the words
    //=========================================================================

    /**
     * Uses the column map to map the character to a column number, then
     * passes the row and column number to getNextState()
     * @param row The current state
     * @param ch The character whose column we're interested in
     * @return The new state to transition to
     */
    public final short getNextStateFromCharacter(int row, int ch) {
        int col;
        if (ch < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            col = columnMap.elementAt((char)ch);
        } else {
            col = supplementaryCharColumnMap.getValue(ch);
        }
        return getNextState(row, col);
    }

    /**
     * Returns the value in the cell with the specified (logical) row and
     * column numbers.  In DictionaryBasedBreakIterator, the row number is
     * a state number, the column number is an input, and the return value
     * is the row number of the new state to transition to.  (0 is the
     * "error" state, and -1 is the "end of word" state in a dictionary)
     * @param row The row number of the current state
     * @param col The column number of the input character (0 means "not a
     * dictionary character")
     * @return The row number of the new state to transition to
     */
    public final short getNextState(int row, int col) {
        if (cellIsPopulated(row, col)) {
            // we map from logical to physical row number by looking up the
            // mapping in rowIndex; we map from logical column number to
            // physical column number by looking up a shift value for this
            // logical row and offsetting the logical column number by
            // the shift amount.  Then we can use internalAt() to actually
            // get the value out of the table.
            return internalAt(rowIndex[row], col + rowIndexShifts[row]);
        }
        else {
            return 0;
        }
    }

    /**
     * Given (logical) row and column numbers, returns true if the
     * cell in that position is populated
     */
    private boolean cellIsPopulated(int row, int col) {
        // look up the entry in the bitmap index for the specified row.
        // If it's a negative number, it's the column number of the only
        // populated cell in the row
        if (rowIndexFlagsIndex[row] < 0) {
            return col == -rowIndexFlagsIndex[row];
        }

        // if it's a positive number, it's the offset of an entry in the bitmap
        // list.  If the table is more than 32 columns wide, the bitmap is stored
        // successive entries in the bitmap list, so we have to divide the column
        // number by 32 and offset the number we got out of the index by the result.
        // Once we have the appropriate piece of the bitmap, test the appropriate
        // bit and return the result.
        else {
            int flags = rowIndexFlags[rowIndexFlagsIndex[row] + (col >> 5)];
            return (flags & (1 << (col & 0x1f))) != 0;
        }
    }

    /**
     * Implementation of getNextState() when we know the specified cell is
     * populated.
     * @param row The PHYSICAL row number of the cell
     * @param col The PHYSICAL column number of the cell
     * @return The value stored in the cell
     */
    private short internalAt(int row, int col) {
        // the table is a one-dimensional array, so this just does the math necessary
        // to treat it as a two-dimensional array (we don't just use a two-dimensional
        // array because two-dimensional arrays are inefficient in Java)
        return table[row * numCols + col];
    }
}
