/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright (C) 2009-2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */

package jdk.internal.icu.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * This is the interface and common implementation of a Unicode Trie2.
 * It is a kind of compressed table that maps from Unicode code points (0..0x10ffff)
 * to 16- or 32-bit integer values.  It works best when there are ranges of
 * characters with the same value, which is generally the case with Unicode
 * character properties.
 *
 * This is the second common version of a Unicode trie (hence the name Trie2).
 *
 */
abstract class Trie2 implements Iterable<Trie2.Range> {

    /**
     * Create a Trie2 from its serialized form.  Inverse of utrie2_serialize().
     *
     * Reads from the current position and leaves the buffer after the end of the trie.
     *
     * The serialized format is identical between ICU4C and ICU4J, so this function
     * will work with serialized Trie2s from either.
     *
     * The actual type of the returned Trie2 will be either Trie2_16 or Trie2_32, depending
     * on the width of the data.
     *
     * To obtain the width of the Trie2, check the actual class type of the returned Trie2.
     * Or use the createFromSerialized() function of Trie2_16 or Trie2_32, which will
     * return only Tries of their specific type/size.
     *
     * The serialized Trie2 on the stream may be in either little or big endian byte order.
     * This allows using serialized Tries from ICU4C without needing to consider the
     * byte order of the system that created them.
     *
     * @param bytes a byte buffer to the serialized form of a UTrie2.
     * @return An unserialized Trie2, ready for use.
     * @throws IllegalArgumentException if the stream does not contain a serialized Trie2.
     * @throws IOException if a read error occurs in the buffer.
     *
     */
    public static Trie2 createFromSerialized(ByteBuffer bytes) throws IOException {
         //    From ICU4C utrie2_impl.h
         //    * Trie2 data structure in serialized form:
         //     *
         //     * UTrie2Header header;
         //     * uint16_t index[header.index2Length];
         //     * uint16_t data[header.shiftedDataLength<<2];  -- or uint32_t data[...]
         //     * @internal
         //     */
         //    typedef struct UTrie2Header {
         //        /** "Tri2" in big-endian US-ASCII (0x54726932) */
         //        uint32_t signature;

         //       /**
         //         * options bit field:
         //         * 15.. 4   reserved (0)
         //         *  3.. 0   UTrie2ValueBits valueBits
         //         */
         //        uint16_t options;
         //
         //        /** UTRIE2_INDEX_1_OFFSET..UTRIE2_MAX_INDEX_LENGTH */
         //        uint16_t indexLength;
         //
         //        /** (UTRIE2_DATA_START_OFFSET..UTRIE2_MAX_DATA_LENGTH)>>UTRIE2_INDEX_SHIFT */
         //        uint16_t shiftedDataLength;
         //
         //        /** Null index and data blocks, not shifted. */
         //        uint16_t index2NullOffset, dataNullOffset;
         //
         //        /**
         //         * First code point of the single-value range ending with U+10ffff,
         //         * rounded up and then shifted right by UTRIE2_SHIFT_1.
         //         */
         //        uint16_t shiftedHighStart;
         //    } UTrie2Header;

        ByteOrder outerByteOrder = bytes.order();
        try {
            UTrie2Header header = new UTrie2Header();

            /* check the signature */
            header.signature = bytes.getInt();
            switch (header.signature) {
            case 0x54726932:
                // The buffer is already set to the trie data byte order.
                break;
            case 0x32697254:
                // Temporarily reverse the byte order.
                boolean isBigEndian = outerByteOrder == ByteOrder.BIG_ENDIAN;
                bytes.order(isBigEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
                header.signature = 0x54726932;
                break;
            default:
                throw new IllegalArgumentException("Buffer does not contain a serialized UTrie2");
            }

            header.options = bytes.getChar();
            header.indexLength = bytes.getChar();
            header.shiftedDataLength = bytes.getChar();
            header.index2NullOffset = bytes.getChar();
            header.dataNullOffset   = bytes.getChar();
            header.shiftedHighStart = bytes.getChar();

            if ((header.options & UTRIE2_OPTIONS_VALUE_BITS_MASK) != 0) {
                throw new IllegalArgumentException("UTrie2 serialized format error.");
            }

            Trie2 This;
            This = new Trie2_16();
            This.header = header;

            /* get the length values and offsets */
            This.indexLength      = header.indexLength;
            This.dataLength       = header.shiftedDataLength << UTRIE2_INDEX_SHIFT;
            This.index2NullOffset = header.index2NullOffset;
            This.dataNullOffset   = header.dataNullOffset;
            This.highStart        = header.shiftedHighStart << UTRIE2_SHIFT_1;
            This.highValueIndex   = This.dataLength - UTRIE2_DATA_GRANULARITY;
            This.highValueIndex += This.indexLength;

            // Allocate the Trie2 index array. If the data width is 16 bits, the array also
            // includes the space for the data.

            int indexArraySize = This.indexLength;
            indexArraySize += This.dataLength;
            This.index = new char[indexArraySize];

            /* Read in the index */
            int i;
            for (i=0; i<This.indexLength; i++) {
                This.index[i] = bytes.getChar();
            }

            /* Read in the data. 16 bit data goes in the same array as the index.
             * 32 bit data goes in its own separate data array.
             */
            This.data16 = This.indexLength;
            for (i=0; i<This.dataLength; i++) {
                This.index[This.data16 + i] = bytes.getChar();
            }

            This.data32 = null;
            This.initialValue = This.index[This.dataNullOffset];
            This.errorValue   = This.index[This.data16+UTRIE2_BAD_UTF8_DATA_OFFSET];

            return This;
        } finally {
            bytes.order(outerByteOrder);
        }
    }

    /**
     * Get the value for a code point as stored in the Trie2.
     *
     * @param codePoint the code point
     * @return the value
     */
    public abstract int get(int codePoint);

    /**
     * Get the trie value for a UTF-16 code unit.
     *
     * A Trie2 stores two distinct values for input in the lead surrogate
     * range, one for lead surrogates, which is the value that will be
     * returned by this function, and a second value that is returned
     * by Trie2.get().
     *
     * For code units outside of the lead surrogate range, this function
     * returns the same result as Trie2.get().
     *
     * This function, together with the alternate value for lead surrogates,
     * makes possible very efficient processing of UTF-16 strings without
     * first converting surrogate pairs to their corresponding 32 bit code point
     * values.
     *
     * At build-time, enumerate the contents of the Trie2 to see if there
     * is non-trivial (non-initialValue) data for any of the supplementary
     * code points associated with a lead surrogate.
     * If so, then set a special (application-specific) value for the
     * lead surrogate code _unit_, with Trie2Writable.setForLeadSurrogateCodeUnit().
     *
     * At runtime, use Trie2.getFromU16SingleLead(). If there is non-trivial
     * data and the code unit is a lead surrogate, then check if a trail surrogate
     * follows. If so, assemble the supplementary code point and look up its value
     * with Trie2.get(); otherwise reset the lead
     * surrogate's value or do a code point lookup for it.
     *
     * If there is only trivial data for lead and trail surrogates, then processing
     * can often skip them. For example, in normalization or case mapping
     * all characters that do not have any mappings are simply copied as is.
     *
     * @param c the code point or lead surrogate value.
     * @return the value
     */
    public abstract int getFromU16SingleLead(char c);

    /**
     * When iterating over the contents of a Trie2, Elements of this type are produced.
     * The iterator will return one item for each contiguous range of codepoints  having the same value.
     *
     * When iterating, the same Trie2EnumRange object will be reused and returned for each range.
     * If you need to retain complete iteration results, clone each returned Trie2EnumRange,
     * or save the range in some other way, before advancing to the next iteration step.
     */
    public static class Range {
        public int     startCodePoint;
        public int     endCodePoint;     // Inclusive.
        public int     value;
        public boolean leadSurrogate;

        public boolean equals(Object other) {
            if (other == null || !(other.getClass().equals(getClass()))) {
                return false;
            }
            Range tother = (Range)other;
            return this.startCodePoint == tother.startCodePoint &&
                   this.endCodePoint   == tother.endCodePoint   &&
                   this.value          == tother.value          &&
                   this.leadSurrogate  == tother.leadSurrogate;
        }

        public int hashCode() {
            int h = initHash();
            h = hashUChar32(h, startCodePoint);
            h = hashUChar32(h, endCodePoint);
            h = hashInt(h, value);
            h = hashByte(h, leadSurrogate? 1: 0);
            return h;
        }
    }

    /**
     *  Create an iterator over the value ranges in this Trie2.
     *  Values from the Trie2 are not remapped or filtered, but are returned as they
     *  are stored in the Trie2.
     *
     * @return an Iterator
     */
    public Iterator<Range> iterator() {
        return iterator(defaultValueMapper);
    }

    private static ValueMapper defaultValueMapper = new ValueMapper() {
        public int map(int in) {
            return in;
        }
    };

    /**
     * Create an iterator over the value ranges from this Trie2.
     * Values from the Trie2 are passed through a caller-supplied remapping function,
     * and it is the remapped values that determine the ranges that
     * will be produced by the iterator.
     *
     *
     * @param mapper provides a function to remap values obtained from the Trie2.
     * @return an Iterator
     */
    public Iterator<Range> iterator(ValueMapper mapper) {
        return new Trie2Iterator(mapper);
    }

    /**
     * When iterating over the contents of a Trie2, an instance of TrieValueMapper may
     * be used to remap the values from the Trie2.  The remapped values will be used
     * both in determining the ranges of codepoints and as the value to be returned
     * for each range.
     *
     * Example of use, with an anonymous subclass of TrieValueMapper:
     *
     *
     * ValueMapper m = new ValueMapper() {
     *    int map(int in) {return in & 0x1f;};
     * }
     * for (Iterator<Trie2EnumRange> iter = trie.iterator(m); i.hasNext(); ) {
     *     Trie2EnumRange r = i.next();
     *     ...  // Do something with the range r.
     * }
     *
     */
    public interface ValueMapper {
        public int  map(int originalVal);
    }

    //--------------------------------------------------------------------------------
    //
    // Below this point are internal implementation items.  No further public API.
    //
    //--------------------------------------------------------------------------------

     /**
      * Trie2 data structure in serialized form:
      *
      * UTrie2Header header;
      * uint16_t index[header.index2Length];
      * uint16_t data[header.shiftedDataLength<<2];  -- or uint32_t data[...]
      *
      * For Java, this is read from the stream into an instance of UTrie2Header.
      * (The C version just places a struct over the raw serialized data.)
      *
      * @internal
      */
    static class UTrie2Header {
        /** "Tri2" in big-endian US-ASCII (0x54726932) */
        int signature;

        /**
         * options bit field (uint16_t):
         * 15.. 4   reserved (0)
         *  3.. 0   UTrie2ValueBits valueBits
         */
        int  options;

        /** UTRIE2_INDEX_1_OFFSET..UTRIE2_MAX_INDEX_LENGTH  (uint16_t) */
        int  indexLength;

        /** (UTRIE2_DATA_START_OFFSET..UTRIE2_MAX_DATA_LENGTH)>>UTRIE2_INDEX_SHIFT  (uint16_t) */
        int  shiftedDataLength;

        /** Null index and data blocks, not shifted.  (uint16_t) */
        int  index2NullOffset, dataNullOffset;

        /**
         * First code point of the single-value range ending with U+10ffff,
         * rounded up and then shifted right by UTRIE2_SHIFT_1.  (uint16_t)
         */
        int shiftedHighStart;
    }

    //
    //  Data members of UTrie2.
    //
    UTrie2Header  header;
    char          index[];           // Index array.  Includes data for 16 bit Tries.
    int           data16;            // Offset to data portion of the index array, if 16 bit data.
                                     //    zero if 32 bit data.
    int           data32[];          // NULL if 16b data is used via index

    int           indexLength;
    int           dataLength;
    int           index2NullOffset;  // 0xffff if there is no dedicated index-2 null block
    int           initialValue;

    /** Value returned for out-of-range code points and illegal UTF-8. */
    int           errorValue;

    /* Start of the last range which ends at U+10ffff, and its value. */
    int           highStart;
    int           highValueIndex;

    int           dataNullOffset;

    /**
     * Trie2 constants, defining shift widths, index array lengths, etc.
     *
     * These are needed for the runtime macros but users can treat these as
     * implementation details and skip to the actual public API further below.
     */

    static final int UTRIE2_OPTIONS_VALUE_BITS_MASK=0x000f;


    /** Shift size for getting the index-1 table offset. */
    static final int UTRIE2_SHIFT_1=6+5;

    /** Shift size for getting the index-2 table offset. */
    static final int UTRIE2_SHIFT_2=5;

    /**
     * Difference between the two shift sizes,
     * for getting an index-1 offset from an index-2 offset. 6=11-5
     */
    static final int UTRIE2_SHIFT_1_2=UTRIE2_SHIFT_1-UTRIE2_SHIFT_2;

    /**
     * Number of index-1 entries for the BMP. 32=0x20
     * This part of the index-1 table is omitted from the serialized form.
     */
    static final int UTRIE2_OMITTED_BMP_INDEX_1_LENGTH=0x10000>>UTRIE2_SHIFT_1;

    /** Number of entries in an index-2 block. 64=0x40 */
    static final int UTRIE2_INDEX_2_BLOCK_LENGTH=1<<UTRIE2_SHIFT_1_2;

    /** Mask for getting the lower bits for the in-index-2-block offset. */
    static final int UTRIE2_INDEX_2_MASK=UTRIE2_INDEX_2_BLOCK_LENGTH-1;

    /** Number of entries in a data block. 32=0x20 */
    static final int UTRIE2_DATA_BLOCK_LENGTH=1<<UTRIE2_SHIFT_2;

    /** Mask for getting the lower bits for the in-data-block offset. */
    static final int UTRIE2_DATA_MASK=UTRIE2_DATA_BLOCK_LENGTH-1;

    /**
     * Shift size for shifting left the index array values.
     * Increases possible data size with 16-bit index values at the cost
     * of compactability.
     * This requires data blocks to be aligned by UTRIE2_DATA_GRANULARITY.
     */
    static final int UTRIE2_INDEX_SHIFT=2;

    /** The alignment size of a data block. Also the granularity for compaction. */
    static final int UTRIE2_DATA_GRANULARITY=1<<UTRIE2_INDEX_SHIFT;

    /**
     * The part of the index-2 table for U+D800..U+DBFF stores values for
     * lead surrogate code _units_ not code _points_.
     * Values for lead surrogate code _points_ are indexed with this portion of the table.
     * Length=32=0x20=0x400>>UTRIE2_SHIFT_2. (There are 1024=0x400 lead surrogates.)
     */
    static final int UTRIE2_LSCP_INDEX_2_OFFSET=0x10000>>UTRIE2_SHIFT_2;
    static final int UTRIE2_LSCP_INDEX_2_LENGTH=0x400>>UTRIE2_SHIFT_2;

    /** Count the lengths of both BMP pieces. 2080=0x820 */
    static final int UTRIE2_INDEX_2_BMP_LENGTH=UTRIE2_LSCP_INDEX_2_OFFSET+UTRIE2_LSCP_INDEX_2_LENGTH;

    /**
     * The 2-byte UTF-8 version of the index-2 table follows at offset 2080=0x820.
     * Length 32=0x20 for lead bytes C0..DF, regardless of UTRIE2_SHIFT_2.
     */
    static final int UTRIE2_UTF8_2B_INDEX_2_OFFSET=UTRIE2_INDEX_2_BMP_LENGTH;
    static final int UTRIE2_UTF8_2B_INDEX_2_LENGTH=0x800>>6;  /* U+0800 is the first code point after 2-byte UTF-8 */

    /**
     * The index-1 table, only used for supplementary code points, at offset 2112=0x840.
     * Variable length, for code points up to highStart, where the last single-value range starts.
     * Maximum length 512=0x200=0x100000>>UTRIE2_SHIFT_1.
     * (For 0x100000 supplementary code points U+10000..U+10ffff.)
     *
     * The part of the index-2 table for supplementary code points starts
     * after this index-1 table.
     *
     * Both the index-1 table and the following part of the index-2 table
     * are omitted completely if there is only BMP data.
     */
    static final int UTRIE2_INDEX_1_OFFSET=UTRIE2_UTF8_2B_INDEX_2_OFFSET+UTRIE2_UTF8_2B_INDEX_2_LENGTH;

    /**
     * The illegal-UTF-8 data block follows the ASCII block, at offset 128=0x80.
     * Used with linear access for single bytes 0..0xbf for simple error handling.
     * Length 64=0x40, not UTRIE2_DATA_BLOCK_LENGTH.
     */
    static final int UTRIE2_BAD_UTF8_DATA_OFFSET=0x80;

    /**
     * Implementation class for an iterator over a Trie2.
     *
     *   Iteration over a Trie2 first returns all of the ranges that are indexed by code points,
     *   then returns the special alternate values for the lead surrogates
     *
     * @internal
     */
    class Trie2Iterator implements Iterator<Range> {

        // The normal constructor that configures the iterator to cover the complete
        //   contents of the Trie2
        Trie2Iterator(ValueMapper vm) {
            mapper    = vm;
            nextStart = 0;
            limitCP   = 0x110000;
            doLeadSurrogates = true;
        }

        /**
         *  The main next() function for Trie2 iterators
         *
         */
        public Range next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            if (nextStart >= limitCP) {
                // Switch over from iterating normal code point values to
                //   doing the alternate lead-surrogate values.
                doingCodePoints = false;
                nextStart = 0xd800;
            }
            int   endOfRange = 0;
            int   val = 0;
            int   mappedVal = 0;

            if (doingCodePoints) {
                // Iteration over code point values.
                val = get(nextStart);
                mappedVal = mapper.map(val);
                endOfRange = rangeEnd(nextStart, limitCP, val);
                // Loop once for each range in the Trie2 with the same raw (unmapped) value.
                // Loop continues so long as the mapped values are the same.
                for (;;) {
                    if (endOfRange >= limitCP-1) {
                        break;
                    }
                    val = get(endOfRange+1);
                    if (mapper.map(val) != mappedVal) {
                        break;
                    }
                    endOfRange = rangeEnd(endOfRange+1, limitCP, val);
                }
            } else {
                // Iteration over the alternate lead surrogate values.
                val = getFromU16SingleLead((char)nextStart);
                mappedVal = mapper.map(val);
                endOfRange = rangeEndLS((char)nextStart);
                // Loop once for each range in the Trie2 with the same raw (unmapped) value.
                // Loop continues so long as the mapped values are the same.
                for (;;) {
                    if (endOfRange >= 0xdbff) {
                        break;
                    }
                    val = getFromU16SingleLead((char)(endOfRange+1));
                    if (mapper.map(val) != mappedVal) {
                        break;
                    }
                    endOfRange = rangeEndLS((char)(endOfRange+1));
                }
            }
            returnValue.startCodePoint = nextStart;
            returnValue.endCodePoint   = endOfRange;
            returnValue.value          = mappedVal;
            returnValue.leadSurrogate  = !doingCodePoints;
            nextStart                  = endOfRange+1;
            return returnValue;
        }

        /**
         *
         */
        public boolean hasNext() {
            return doingCodePoints && (doLeadSurrogates || nextStart < limitCP) || nextStart < 0xdc00;
        }

        private int rangeEndLS(char startingLS) {
            if (startingLS >= 0xdbff) {
                return 0xdbff;
            }

            int c;
            int val = getFromU16SingleLead(startingLS);
            for (c = startingLS+1; c <= 0x0dbff; c++) {
                if (getFromU16SingleLead((char)c) != val) {
                    break;
                }
            }
            return c-1;
        }

        //
        //   Iteration State Variables
        //
        private ValueMapper    mapper;
        private Range          returnValue = new Range();
        // The starting code point for the next range to be returned.
        private int            nextStart;
        // The upper limit for the last normal range to be returned.  Normally 0x110000, but
        //   may be lower when iterating over the code points for a single lead surrogate.
        private int            limitCP;

        // True while iterating over the Trie2 values for code points.
        // False while iterating over the alternate values for lead surrogates.
        private boolean        doingCodePoints = true;

        // True if the iterator should iterate the special values for lead surrogates in
        //   addition to the normal values for code points.
        private boolean        doLeadSurrogates = true;
    }

    /**
     * Find the last character in a contiguous range of characters with the
     * same Trie2 value as the input character.
     *
     * @param c  The character to begin with.
     * @return   The last contiguous character with the same value.
     */
    int rangeEnd(int start, int limitp, int val) {
        int c;
        int limit = Math.min(highStart, limitp);

        for (c = start+1; c < limit; c++) {
            if (get(c) != val) {
                break;
            }
        }
        if (c >= highStart) {
            c = limitp;
        }
        return c - 1;
    }


    //
    //  Hashing implementation functions.  FNV hash.  Respected public domain algorithm.
    //
    private static int initHash() {
        return 0x811c9DC5;  // unsigned 2166136261
    }

    private static int hashByte(int h, int b) {
        h = h * 16777619;
        h = h ^ b;
        return h;
    }

    private static int hashUChar32(int h, int c) {
        h = Trie2.hashByte(h, c & 255);
        h = Trie2.hashByte(h, (c>>8) & 255);
        h = Trie2.hashByte(h, c>>16);
        return h;
    }

    private static int hashInt(int h, int i) {
        h = Trie2.hashByte(h, i & 255);
        h = Trie2.hashByte(h, (i>>8) & 255);
        h = Trie2.hashByte(h, (i>>16) & 255);
        h = Trie2.hashByte(h, (i>>24) & 255);
        return h;
    }

}
