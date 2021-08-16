/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
// (c) 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License

// created: 2018may04 Markus W. Scherer

package jdk.internal.icu.util;

import jdk.internal.icu.impl.ICUBinary;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static jdk.internal.icu.impl.NormalizerImpl.UTF16Plus;

/**
 * Immutable Unicode code point trie.
 * Fast, reasonably compact, map from Unicode code points (U+0000..U+10FFFF) to integer values.
 * For details see http://site.icu-project.org/design/struct/utrie
 *
 * <p>This class is not intended for public subclassing.
 *
 * @see MutableCodePointTrie
 * @stable ICU 63
 */
@SuppressWarnings("deprecation")
public abstract class CodePointTrie extends CodePointMap {
    /**
     * Selectors for the type of a CodePointTrie.
     * Different trade-offs for size vs. speed.
     *
     * <p>Use null for {@link #fromBinary} to accept any type;
     * {@link #getType} will return the actual type.
     *
     * @see MutableCodePointTrie#buildImmutable(CodePointTrie.Type, CodePointTrie.ValueWidth)
     * @see #fromBinary
     * @see #getType
     * @stable ICU 63
     */
    public enum Type {
        /**
         * Fast/simple/larger BMP data structure.
         * The {@link Fast} subclasses have additional functions for lookup for BMP and supplementary code points.
         *
         * @see Fast
         * @stable ICU 63
         */
        FAST,
        /**
         * Small/slower BMP data structure.
         *
         * @see Small
         * @stable ICU 63
         */
        SMALL
    }

    /**
     * Selectors for the number of bits in a CodePointTrie data value.
     *
     * <p>Use null for {@link #fromBinary} to accept any data value width;
     * {@link #getValueWidth} will return the actual data value width.
     *
     * @stable ICU 63
     */
    public enum ValueWidth {
        /**
         * The trie stores 16 bits per data value.
         * It returns them as unsigned values 0..0xffff=65535.
         *
         * @stable ICU 63
         */
        BITS_16,
        /**
         * The trie stores 32 bits per data value.
         *
         * @stable ICU 63
         */
        BITS_32,
        /**
         * The trie stores 8 bits per data value.
         * It returns them as unsigned values 0..0xff=255.
         *
         * @stable ICU 63
         */
        BITS_8
    }

    private CodePointTrie(char[] index, Data data, int highStart,
            int index3NullOffset, int dataNullOffset) {
        this.ascii = new int[ASCII_LIMIT];
        this.index = index;
        this.data = data;
        this.dataLength = data.getDataLength();
        this.highStart = highStart;
        this.index3NullOffset = index3NullOffset;
        this.dataNullOffset = dataNullOffset;

        for (int c = 0; c < ASCII_LIMIT; ++c) {
            ascii[c] = data.getFromIndex(c);
        }

        int nullValueOffset = dataNullOffset;
        if (nullValueOffset >= dataLength) {
            nullValueOffset = dataLength - HIGH_VALUE_NEG_DATA_OFFSET;
        }
        nullValue = data.getFromIndex(nullValueOffset);
    }

    /**
     * Creates a trie from its binary form,
     * stored in the ByteBuffer starting at the current position.
     * Advances the buffer position to just after the trie data.
     * Inverse of {@link #toBinary(OutputStream)}.
     *
     * <p>The data is copied from the buffer;
     * later modification of the buffer will not affect the trie.
     *
     * @param type selects the trie type; this method throws an exception
     *             if the type does not match the binary data;
     *             use null to accept any type
     * @param valueWidth selects the number of bits in a data value; this method throws an exception
     *                  if the valueWidth does not match the binary data;
     *                  use null to accept any data value width
     * @param bytes a buffer containing the binary data of a CodePointTrie
     * @return the trie
     * @see MutableCodePointTrie#MutableCodePointTrie(int, int)
     * @see MutableCodePointTrie#buildImmutable(CodePointTrie.Type, CodePointTrie.ValueWidth)
     * @see #toBinary(OutputStream)
     * @stable ICU 63
     */
    public static CodePointTrie fromBinary(Type type, ValueWidth valueWidth, ByteBuffer bytes) {
        ByteOrder outerByteOrder = bytes.order();
        try {
            // Enough data for a trie header?
            if (bytes.remaining() < 16 /* sizeof(UCPTrieHeader) */) {
                throw new InternalError("Buffer too short for a CodePointTrie header");
            }

            // struct UCPTrieHeader
            /** "Tri3" in big-endian US-ASCII (0x54726933) */
            int signature = bytes.getInt();

            // Check the signature.
            switch (signature) {
            case 0x54726933:
                // The buffer is already set to the trie data byte order.
                break;
            case 0x33697254:
                // Temporarily reverse the byte order.
                boolean isBigEndian = outerByteOrder == ByteOrder.BIG_ENDIAN;
                bytes.order(isBigEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
                signature = 0x54726933;
                break;
            default:
                throw new InternalError("Buffer does not contain a serialized CodePointTrie");
            }

            // struct UCPTrieHeader continued
            /**
             * Options bit field:
             * Bits 15..12: Data length bits 19..16.
             * Bits 11..8: Data null block offset bits 19..16.
             * Bits 7..6: UCPTrieType
             * Bits 5..3: Reserved (0).
             * Bits 2..0: UCPTrieValueWidth
             */
            int options = bytes.getChar();

            /** Total length of the index tables. */
            int indexLength = bytes.getChar();

            /** Data length bits 15..0. */
            int dataLength = bytes.getChar();

            /** Index-3 null block offset, 0x7fff or 0xffff if none. */
            int index3NullOffset = bytes.getChar();

            /** Data null block offset bits 15..0, 0xfffff if none. */
            int dataNullOffset = bytes.getChar();

            /**
             * First code point of the single-value range ending with U+10ffff,
             * rounded up and then shifted right by SHIFT_2.
             */
            int shiftedHighStart = bytes.getChar();
            // struct UCPTrieHeader end

            int typeInt = (options >> 6) & 3;
            Type actualType;
            switch (typeInt) {
            case 0: actualType = Type.FAST; break;
            case 1: actualType = Type.SMALL; break;
            default:
                throw new InternalError("CodePointTrie data header has an unsupported type");
            }

            int valueWidthInt = options & OPTIONS_VALUE_BITS_MASK;
            ValueWidth actualValueWidth;
            switch (valueWidthInt) {
            case 0: actualValueWidth = ValueWidth.BITS_16; break;
            case 1: actualValueWidth = ValueWidth.BITS_32; break;
            case 2: actualValueWidth = ValueWidth.BITS_8; break;
            default:
                throw new InternalError("CodePointTrie data header has an unsupported value width");
            }

            if ((options & OPTIONS_RESERVED_MASK) != 0) {
                throw new InternalError("CodePointTrie data header has unsupported options");
            }

            if (type == null) {
                type = actualType;
            }
            if (valueWidth == null) {
                valueWidth = actualValueWidth;
            }
            if (type != actualType || valueWidth != actualValueWidth) {
                throw new InternalError("CodePointTrie data header has a different type or value width than required");
            }

            // Get the length values and offsets.
            dataLength |= ((options & OPTIONS_DATA_LENGTH_MASK) << 4);
            dataNullOffset |= ((options & OPTIONS_DATA_NULL_OFFSET_MASK) << 8);

            int highStart = shiftedHighStart << SHIFT_2;

            // Calculate the actual length, minus the header.
            int actualLength = indexLength * 2;
            if (valueWidth == ValueWidth.BITS_16) {
                actualLength += dataLength * 2;
            } else if (valueWidth == ValueWidth.BITS_32) {
                actualLength += dataLength * 4;
            } else {
                actualLength += dataLength;
            }
            if (bytes.remaining() < actualLength) {
                throw new InternalError("Buffer too short for the CodePointTrie data");
            }

            char[] index = ICUBinary.getChars(bytes, indexLength, 0);
            switch (valueWidth) {
            case BITS_16: {
                char[] data16 = ICUBinary.getChars(bytes, dataLength, 0);
                return type == Type.FAST ?
                        new Fast16(index, data16, highStart, index3NullOffset, dataNullOffset) :
                            new Small16(index, data16, highStart, index3NullOffset, dataNullOffset);
            }
            case BITS_32: {
                int[] data32 = ICUBinary.getInts(bytes, dataLength, 0);
                return type == Type.FAST ?
                        new Fast32(index, data32, highStart, index3NullOffset, dataNullOffset) :
                            new Small32(index, data32, highStart, index3NullOffset, dataNullOffset);
            }
            case BITS_8: {
                byte[] data8 = ICUBinary.getBytes(bytes, dataLength, 0);
                return type == Type.FAST ?
                        new Fast8(index, data8, highStart, index3NullOffset, dataNullOffset) :
                            new Small8(index, data8, highStart, index3NullOffset, dataNullOffset);
            }
            default:
                throw new AssertionError("should be unreachable");
            }
        } finally {
            bytes.order(outerByteOrder);
        }
    }

    /**
     * Returns the trie type.
     *
     * @return the trie type
     * @stable ICU 63
     */
    public abstract Type getType();
    /**
     * Returns the number of bits in a trie data value.
     *
     * @return the number of bits in a trie data value
     * @stable ICU 63
     */
    public final ValueWidth getValueWidth() { return data.getValueWidth(); }

    /**
     * {@inheritDoc}
     * @stable ICU 63
     */
    @Override
    public int get(int c) {
        return data.getFromIndex(cpIndex(c));
    }

    /**
     * Returns a trie value for an ASCII code point, without range checking.
     *
     * @param c the input code point; must be U+0000..U+007F
     * @return The ASCII code point's trie value.
     * @stable ICU 63
     */
    public final int asciiGet(int c) {
        return ascii[c];
    }

    private static final int MAX_UNICODE = 0x10ffff;

    private static final int ASCII_LIMIT = 0x80;

    private static final int maybeFilterValue(int value, int trieNullValue, int nullValue,
            ValueFilter filter) {
        if (value == trieNullValue) {
            value = nullValue;
        } else if (filter != null) {
            value = filter.apply(value);
        }
        return value;
    }

    /**
     * {@inheritDoc}
     * @stable ICU 63
     */
    @Override
    public final boolean getRange(int start, ValueFilter filter, Range range) {
        if (start < 0 || MAX_UNICODE < start) {
            return false;
        }
        if (start >= highStart) {
            int di = dataLength - HIGH_VALUE_NEG_DATA_OFFSET;
            int value = data.getFromIndex(di);
            if (filter != null) { value = filter.apply(value); }
            range.set(start, MAX_UNICODE, value);
            return true;
        }

        int nullValue = this.nullValue;
        if (filter != null) { nullValue = filter.apply(nullValue); }
        Type type = getType();

        int prevI3Block = -1;
        int prevBlock = -1;
        int c = start;
        // Initialize to make compiler happy. Real value when haveValue is true.
        int trieValue = 0, value = 0;
        boolean haveValue = false;
        do {
            int i3Block;
            int i3;
            int i3BlockLength;
            int dataBlockLength;
            if (c <= 0xffff && (type == Type.FAST || c <= SMALL_MAX)) {
                i3Block = 0;
                i3 = c >> FAST_SHIFT;
                i3BlockLength = type == Type.FAST ? BMP_INDEX_LENGTH : SMALL_INDEX_LENGTH;
                dataBlockLength = FAST_DATA_BLOCK_LENGTH;
            } else {
                // Use the multi-stage index.
                int i1 = c >> SHIFT_1;
                if (type == Type.FAST) {
                    assert(0xffff < c && c < highStart);
                    i1 += BMP_INDEX_LENGTH - OMITTED_BMP_INDEX_1_LENGTH;
                } else {
                    assert(c < highStart && highStart > SMALL_LIMIT);
                    i1 += SMALL_INDEX_LENGTH;
                }
                i3Block = index[index[i1] + ((c >> SHIFT_2) & INDEX_2_MASK)];
                if (i3Block == prevI3Block && (c - start) >= CP_PER_INDEX_2_ENTRY) {
                    // The index-3 block is the same as the previous one, and filled with value.
                    assert((c & (CP_PER_INDEX_2_ENTRY - 1)) == 0);
                    c += CP_PER_INDEX_2_ENTRY;
                    continue;
                }
                prevI3Block = i3Block;
                if (i3Block == index3NullOffset) {
                    // This is the index-3 null block.
                    if (haveValue) {
                        if (nullValue != value) {
                            range.set(start, c - 1, value);
                            return true;
                        }
                    } else {
                        trieValue = this.nullValue;
                        value = nullValue;
                        haveValue = true;
                    }
                    prevBlock = dataNullOffset;
                    c = (c + CP_PER_INDEX_2_ENTRY) & ~(CP_PER_INDEX_2_ENTRY - 1);
                    continue;
                }
                i3 = (c >> SHIFT_3) & INDEX_3_MASK;
                i3BlockLength = INDEX_3_BLOCK_LENGTH;
                dataBlockLength = SMALL_DATA_BLOCK_LENGTH;
            }
            // Enumerate data blocks for one index-3 block.
            do {
                int block;
                if ((i3Block & 0x8000) == 0) {
                    block = index[i3Block + i3];
                } else {
                    // 18-bit indexes stored in groups of 9 entries per 8 indexes.
                    int group = (i3Block & 0x7fff) + (i3 & ~7) + (i3 >> 3);
                    int gi = i3 & 7;
                    block = (index[group++] << (2 + (2 * gi))) & 0x30000;
                    block |= index[group + gi];
                }
                if (block == prevBlock && (c - start) >= dataBlockLength) {
                    // The block is the same as the previous one, and filled with value.
                    assert((c & (dataBlockLength - 1)) == 0);
                    c += dataBlockLength;
                } else {
                    int dataMask = dataBlockLength - 1;
                    prevBlock = block;
                    if (block == dataNullOffset) {
                        // This is the data null block.
                        if (haveValue) {
                            if (nullValue != value) {
                                range.set(start, c - 1, value);
                                return true;
                            }
                        } else {
                            trieValue = this.nullValue;
                            value = nullValue;
                            haveValue = true;
                        }
                        c = (c + dataBlockLength) & ~dataMask;
                    } else {
                        int di = block + (c & dataMask);
                        int trieValue2 = data.getFromIndex(di);
                        if (haveValue) {
                            if (trieValue2 != trieValue) {
                                if (filter == null ||
                                        maybeFilterValue(trieValue2, this.nullValue, nullValue,
                                                filter) != value) {
                                    range.set(start, c - 1, value);
                                    return true;
                                }
                                trieValue = trieValue2;  // may or may not help
                            }
                        } else {
                            trieValue = trieValue2;
                            value = maybeFilterValue(trieValue2, this.nullValue, nullValue, filter);
                            haveValue = true;
                        }
                        while ((++c & dataMask) != 0) {
                            trieValue2 = data.getFromIndex(++di);
                            if (trieValue2 != trieValue) {
                                if (filter == null ||
                                        maybeFilterValue(trieValue2, this.nullValue, nullValue,
                                                filter) != value) {
                                    range.set(start, c - 1, value);
                                    return true;
                                }
                                trieValue = trieValue2;  // may or may not help
                            }
                        }
                    }
                }
            } while (++i3 < i3BlockLength);
        } while (c < highStart);
        assert(haveValue);
        int di = dataLength - HIGH_VALUE_NEG_DATA_OFFSET;
        int highValue = data.getFromIndex(di);
        if (maybeFilterValue(highValue, this.nullValue, nullValue, filter) != value) {
            --c;
        } else {
            c = MAX_UNICODE;
        }
        range.set(start, c, value);
        return true;
    }

    /**
     * Writes a representation of the trie to the output stream.
     * Inverse of {@link #fromBinary}.
     *
     * @param os the output stream
     * @return the number of bytes written
     * @stable ICU 63
     */
    public final int toBinary(OutputStream os) {
        try {
            DataOutputStream dos = new DataOutputStream(os);

            // Write the UCPTrieHeader
            dos.writeInt(0x54726933);  // signature="Tri3"
            dos.writeChar(  // options
                ((dataLength & 0xf0000) >> 4) |
                ((dataNullOffset & 0xf0000) >> 8) |
                (getType().ordinal() << 6) |
                getValueWidth().ordinal());
            dos.writeChar(index.length);
            dos.writeChar(dataLength);
            dos.writeChar(index3NullOffset);
            dos.writeChar(dataNullOffset);
            dos.writeChar(highStart >> SHIFT_2);  // shiftedHighStart
            int length = 16;  // sizeof(UCPTrieHeader)

            for (char i : index) { dos.writeChar(i); }
            length += index.length * 2;
            length += data.write(dos);
            return length;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** @internal */
    static final int FAST_SHIFT = 6;

    /** Number of entries in a data block for code points below the fast limit. 64=0x40 @internal */
    static final int FAST_DATA_BLOCK_LENGTH = 1 << FAST_SHIFT;

    /** Mask for getting the lower bits for the in-fast-data-block offset. @internal */
    private static final int FAST_DATA_MASK = FAST_DATA_BLOCK_LENGTH - 1;

    /** @internal */
    private static final int SMALL_MAX = 0xfff;

    /**
     * Offset from dataLength (to be subtracted) for fetching the
     * value returned for out-of-range code points and ill-formed UTF-8/16.
     * @internal
     */
    private static final int ERROR_VALUE_NEG_DATA_OFFSET = 1;
    /**
     * Offset from dataLength (to be subtracted) for fetching the
     * value returned for code points highStart..U+10FFFF.
     * @internal
     */
    private static final int HIGH_VALUE_NEG_DATA_OFFSET = 2;

    // ucptrie_impl.h

    /** The length of the BMP index table. 1024=0x400 */
    private static final int BMP_INDEX_LENGTH = 0x10000 >> FAST_SHIFT;

    static final int SMALL_LIMIT = 0x1000;
    private static final int SMALL_INDEX_LENGTH = SMALL_LIMIT >> FAST_SHIFT;

    /** Shift size for getting the index-3 table offset. */
    static final int SHIFT_3 = 4;

    /** Shift size for getting the index-2 table offset. */
    private static final int SHIFT_2 = 5 + SHIFT_3;

    /** Shift size for getting the index-1 table offset. */
    private static final int SHIFT_1 = 5 + SHIFT_2;

    /**
     * Difference between two shift sizes,
     * for getting an index-2 offset from an index-3 offset. 5=9-4
     */
    static final int SHIFT_2_3 = SHIFT_2 - SHIFT_3;

    /**
     * Difference between two shift sizes,
     * for getting an index-1 offset from an index-2 offset. 5=14-9
     */
    static final int SHIFT_1_2 = SHIFT_1 - SHIFT_2;

    /**
     * Number of index-1 entries for the BMP. (4)
     * This part of the index-1 table is omitted from the serialized form.
     */
    private static final int OMITTED_BMP_INDEX_1_LENGTH = 0x10000 >> SHIFT_1;

    /** Number of entries in an index-2 block. 32=0x20 */
    static final int INDEX_2_BLOCK_LENGTH = 1 << SHIFT_1_2;

    /** Mask for getting the lower bits for the in-index-2-block offset. */
    static final int INDEX_2_MASK = INDEX_2_BLOCK_LENGTH - 1;

    /** Number of code points per index-2 table entry. 512=0x200 */
    static final int CP_PER_INDEX_2_ENTRY = 1 << SHIFT_2;

    /** Number of entries in an index-3 block. 32=0x20 */
    static final int INDEX_3_BLOCK_LENGTH = 1 << SHIFT_2_3;

    /** Mask for getting the lower bits for the in-index-3-block offset. */
    private static final int INDEX_3_MASK = INDEX_3_BLOCK_LENGTH - 1;

    /** Number of entries in a small data block. 16=0x10 */
    static final int SMALL_DATA_BLOCK_LENGTH = 1 << SHIFT_3;

    /** Mask for getting the lower bits for the in-small-data-block offset. */
    static final int SMALL_DATA_MASK = SMALL_DATA_BLOCK_LENGTH - 1;

    // ucptrie_impl.h: Constants for use with UCPTrieHeader.options.
    private static final int OPTIONS_DATA_LENGTH_MASK = 0xf000;
    private static final int OPTIONS_DATA_NULL_OFFSET_MASK = 0xf00;
    private static final int OPTIONS_RESERVED_MASK = 0x38;
    private static final int OPTIONS_VALUE_BITS_MASK = 7;
    /**
     * Value for index3NullOffset which indicates that there is no index-3 null block.
     * Bit 15 is unused for this value because this bit is used if the index-3 contains
     * 18-bit indexes.
     */
    static final int NO_INDEX3_NULL_OFFSET = 0x7fff;
    static final int NO_DATA_NULL_OFFSET = 0xfffff;

    private static abstract class Data {
        abstract ValueWidth getValueWidth();
        abstract int getDataLength();
        abstract int getFromIndex(int index);
        abstract int write(DataOutputStream dos) throws IOException;
    }

    private static final class Data16 extends Data {
        char[] array;
        Data16(char[] a) { array = a; }
        @Override ValueWidth getValueWidth() { return ValueWidth.BITS_16; }
        @Override int getDataLength() { return array.length; }
        @Override int getFromIndex(int index) { return array[index]; }
        @Override int write(DataOutputStream dos) throws IOException {
            for (char v : array) { dos.writeChar(v); }
            return array.length * 2;
        }
    }

    private static final class Data32 extends Data {
        int[] array;
        Data32(int[] a) { array = a; }
        @Override ValueWidth getValueWidth() { return ValueWidth.BITS_32; }
        @Override int getDataLength() { return array.length; }
        @Override int getFromIndex(int index) { return array[index]; }
        @Override int write(DataOutputStream dos) throws IOException {
            for (int v : array) { dos.writeInt(v); }
            return array.length * 4;
        }
    }

    private static final class Data8 extends Data {
        byte[] array;
        Data8(byte[] a) { array = a; }
        @Override ValueWidth getValueWidth() { return ValueWidth.BITS_8; }
        @Override int getDataLength() { return array.length; }
        @Override int getFromIndex(int index) { return array[index] & 0xff; }
        @Override int write(DataOutputStream dos) throws IOException {
            for (byte v : array) { dos.writeByte(v); }
            return array.length;
        }
    }

    /** @internal */
    private final int[] ascii;

    /** @internal */
    private final char[] index;

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    protected final Data data;
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    protected final int dataLength;
    /**
     * Start of the last range which ends at U+10FFFF.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    protected final int highStart;

    /**
     * Internal index-3 null block offset.
     * Set to an impossibly high value (e.g., 0xffff) if there is no dedicated index-3 null block.
     * @internal
     */
    private final int index3NullOffset;
    /**
     * Internal data null block offset, not shifted.
     * Set to an impossibly high value (e.g., 0xfffff) if there is no dedicated data null block.
     * @internal
     */
    private final int dataNullOffset;
    /** @internal */
    private final int nullValue;

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    protected final int fastIndex(int c) {
        return index[c >> FAST_SHIFT] + (c & FAST_DATA_MASK);
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    protected final int smallIndex(Type type, int c) {
        // Split into two methods to make this part inline-friendly.
        // In C, this part is a macro.
        if (c >= highStart) {
            return dataLength - HIGH_VALUE_NEG_DATA_OFFSET;
        }
        return internalSmallIndex(type, c);
    }

    private final int internalSmallIndex(Type type, int c) {
        int i1 = c >> SHIFT_1;
        if (type == Type.FAST) {
            assert(0xffff < c && c < highStart);
            i1 += BMP_INDEX_LENGTH - OMITTED_BMP_INDEX_1_LENGTH;
        } else {
            assert(0 <= c && c < highStart && highStart > SMALL_LIMIT);
            i1 += SMALL_INDEX_LENGTH;
        }
        int i3Block = index[index[i1] + ((c >> SHIFT_2) & INDEX_2_MASK)];
        int i3 = (c >> SHIFT_3) & INDEX_3_MASK;
        int dataBlock;
        if ((i3Block & 0x8000) == 0) {
            // 16-bit indexes
            dataBlock = index[i3Block + i3];
        } else {
            // 18-bit indexes stored in groups of 9 entries per 8 indexes.
            i3Block = (i3Block & 0x7fff) + (i3 & ~7) + (i3 >> 3);
            i3 &= 7;
            dataBlock = (index[i3Block++] << (2 + (2 * i3))) & 0x30000;
            dataBlock |= index[i3Block + i3];
        }
        return dataBlock + (c & SMALL_DATA_MASK);
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    protected abstract int cpIndex(int c);

    /**
     * A CodePointTrie with {@link Type#FAST}.
     *
     * @stable ICU 63
     */
    public static abstract class Fast extends CodePointTrie {
        private Fast(char[] index, Data data, int highStart,
                int index3NullOffset, int dataNullOffset) {
            super(index, data, highStart, index3NullOffset, dataNullOffset);
        }

        /**
         * Creates a trie from its binary form.
         * Same as {@link CodePointTrie#fromBinary(Type, ValueWidth, ByteBuffer)}
         * with {@link Type#FAST}.
         *
         * @param valueWidth selects the number of bits in a data value; this method throws an exception
         *                  if the valueWidth does not match the binary data;
         *                  use null to accept any data value width
         * @param bytes a buffer containing the binary data of a CodePointTrie
         * @return the trie
         * @stable ICU 63
         */
        public static Fast fromBinary(ValueWidth valueWidth, ByteBuffer bytes) {
            return (Fast) CodePointTrie.fromBinary(Type.FAST, valueWidth, bytes);
        }

        /**
         * @return {@link Type#FAST}
         * @stable ICU 63
         */
        @Override
        public final Type getType() { return Type.FAST; }

        /**
         * Returns a trie value for a BMP code point (U+0000..U+FFFF), without range checking.
         * Can be used to look up a value for a UTF-16 code unit if other parts of
         * the string processing check for surrogates.
         *
         * @param c the input code point, must be U+0000..U+FFFF
         * @return The BMP code point's trie value.
         * @stable ICU 63
         */
        public abstract int bmpGet(int c);

        /**
         * Returns a trie value for a supplementary code point (U+10000..U+10FFFF),
         * without range checking.
         *
         * @param c the input code point, must be U+10000..U+10FFFF
         * @return The supplementary code point's trie value.
         * @stable ICU 63
         */
        public abstract int suppGet(int c);

        /**
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        @Override
        protected final int cpIndex(int c) {
            if (c >= 0) {
                if (c <= 0xffff) {
                    return fastIndex(c);
                } else if (c <= 0x10ffff) {
                    return smallIndex(Type.FAST, c);
                }
            }
            return dataLength - ERROR_VALUE_NEG_DATA_OFFSET;
        }

        /**
         * {@inheritDoc}
         * @stable ICU 63
         */
        @Override
        public final StringIterator stringIterator(CharSequence s, int sIndex) {
            return new FastStringIterator(s, sIndex);
        }

        private final class FastStringIterator extends StringIterator {
            private FastStringIterator(CharSequence s, int sIndex) {
                super(s, sIndex);
            }

            @Override
            public boolean next() {
                if (sIndex >= s.length()) {
                    return false;
                }
                char lead = s.charAt(sIndex++);
                c = lead;
                int dataIndex;
                if (!Character.isSurrogate(lead)) {
                    dataIndex = fastIndex(c);
                } else {
                    char trail;
                    if (UTF16Plus.isSurrogateLead(lead) && sIndex < s.length() &&
                            Character.isLowSurrogate(trail = s.charAt(sIndex))) {
                        ++sIndex;
                        c = Character.toCodePoint(lead, trail);
                        dataIndex = smallIndex(Type.FAST, c);
                    } else {
                        dataIndex = dataLength - ERROR_VALUE_NEG_DATA_OFFSET;
                    }
                }
                value = data.getFromIndex(dataIndex);
                return true;
            }

            @Override
            public boolean previous() {
                if (sIndex <= 0) {
                    return false;
                }
                char trail = s.charAt(--sIndex);
                c = trail;
                int dataIndex;
                if (!Character.isSurrogate(trail)) {
                    dataIndex = fastIndex(c);
                } else {
                    char lead;
                    if (!UTF16Plus.isSurrogateLead(trail) && sIndex > 0 &&
                            Character.isHighSurrogate(lead = s.charAt(sIndex - 1))) {
                        --sIndex;
                        c = Character.toCodePoint(lead, trail);
                        dataIndex = smallIndex(Type.FAST, c);
                    } else {
                        dataIndex = dataLength - ERROR_VALUE_NEG_DATA_OFFSET;
                    }
                }
                value = data.getFromIndex(dataIndex);
                return true;
            }
        }
    }

    /**
     * A CodePointTrie with {@link Type#SMALL}.
     *
     * @stable ICU 63
     */
    public static abstract class Small extends CodePointTrie {
        private Small(char[] index, Data data, int highStart,
                int index3NullOffset, int dataNullOffset) {
            super(index, data, highStart, index3NullOffset, dataNullOffset);
        }

        /**
         * Creates a trie from its binary form.
         * Same as {@link CodePointTrie#fromBinary(Type, ValueWidth, ByteBuffer)}
         * with {@link Type#SMALL}.
         *
         * @param valueWidth selects the number of bits in a data value; this method throws an exception
         *                  if the valueWidth does not match the binary data;
         *                  use null to accept any data value width
         * @param bytes a buffer containing the binary data of a CodePointTrie
         * @return the trie
         * @stable ICU 63
         */
        public static Small fromBinary(ValueWidth valueWidth, ByteBuffer bytes) {
            return (Small) CodePointTrie.fromBinary(Type.SMALL, valueWidth, bytes);
        }

        /**
         * @return {@link Type#SMALL}
         * @stable ICU 63
         */
        @Override
        public final Type getType() { return Type.SMALL; }

        /**
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        @Override
        protected final int cpIndex(int c) {
            if (c >= 0) {
                if (c <= SMALL_MAX) {
                    return fastIndex(c);
                } else if (c <= 0x10ffff) {
                    return smallIndex(Type.SMALL, c);
                }
            }
            return dataLength - ERROR_VALUE_NEG_DATA_OFFSET;
        }

        /**
         * {@inheritDoc}
         * @stable ICU 63
         */
        @Override
        public final StringIterator stringIterator(CharSequence s, int sIndex) {
            return new SmallStringIterator(s, sIndex);
        }

        private final class SmallStringIterator extends StringIterator {
            private SmallStringIterator(CharSequence s, int sIndex) {
                super(s, sIndex);
            }

            @Override
            public boolean next() {
                if (sIndex >= s.length()) {
                    return false;
                }
                char lead = s.charAt(sIndex++);
                c = lead;
                int dataIndex;
                if (!Character.isSurrogate(lead)) {
                    dataIndex = cpIndex(c);
                } else {
                    char trail;
                    if (UTF16Plus.isSurrogateLead(lead) && sIndex < s.length() &&
                            Character.isLowSurrogate(trail = s.charAt(sIndex))) {
                        ++sIndex;
                        c = Character.toCodePoint(lead, trail);
                        dataIndex = smallIndex(Type.SMALL, c);
                    } else {
                        dataIndex = dataLength - ERROR_VALUE_NEG_DATA_OFFSET;
                    }
                }
                value = data.getFromIndex(dataIndex);
                return true;
            }

            @Override
            public boolean previous() {
                if (sIndex <= 0) {
                    return false;
                }
                char trail = s.charAt(--sIndex);
                c = trail;
                int dataIndex;
                if (!Character.isSurrogate(trail)) {
                    dataIndex = cpIndex(c);
                } else {
                    char lead;
                    if (!UTF16Plus.isSurrogateLead(trail) && sIndex > 0 &&
                            Character.isHighSurrogate(lead = s.charAt(sIndex - 1))) {
                        --sIndex;
                        c = Character.toCodePoint(lead, trail);
                        dataIndex = smallIndex(Type.SMALL, c);
                    } else {
                        dataIndex = dataLength - ERROR_VALUE_NEG_DATA_OFFSET;
                    }
                }
                value = data.getFromIndex(dataIndex);
                return true;
            }
        }
    }

    /**
     * A CodePointTrie with {@link Type#FAST} and {@link ValueWidth#BITS_16}.
     *
     * @stable ICU 63
     */
    public static final class Fast16 extends Fast {
        private final char[] dataArray;

        Fast16(char[] index, char[] data16, int highStart,
                int index3NullOffset, int dataNullOffset) {
            super(index, new Data16(data16), highStart, index3NullOffset, dataNullOffset);
            this.dataArray = data16;
        }

        /**
         * Creates a trie from its binary form.
         * Same as {@link CodePointTrie#fromBinary(Type, ValueWidth, ByteBuffer)}
         * with {@link Type#FAST} and {@link ValueWidth#BITS_16}.
         *
         * @param bytes a buffer containing the binary data of a CodePointTrie
         * @return the trie
         * @stable ICU 63
         */
        public static Fast16 fromBinary(ByteBuffer bytes) {
            return (Fast16) CodePointTrie.fromBinary(Type.FAST, ValueWidth.BITS_16, bytes);
        }

        /**
         * {@inheritDoc}
         * @stable ICU 63
         */
        @Override
        public final int get(int c) {
            return dataArray[cpIndex(c)];
        }

        /**
         * {@inheritDoc}
         * @stable ICU 63
         */
        @Override
        public final int bmpGet(int c) {
            assert 0 <= c && c <= 0xffff;
            return dataArray[fastIndex(c)];
        }

        /**
         * {@inheritDoc}
         * @stable ICU 63
         */
        @Override
        public final int suppGet(int c) {
            assert 0x10000 <= c && c <= 0x10ffff;
            return dataArray[smallIndex(Type.FAST, c)];
        }
    }

    /**
     * A CodePointTrie with {@link Type#FAST} and {@link ValueWidth#BITS_32}.
     *
     * @stable ICU 63
     */
    public static final class Fast32 extends Fast {
        private final int[] dataArray;

        Fast32(char[] index, int[] data32, int highStart,
                int index3NullOffset, int dataNullOffset) {
            super(index, new Data32(data32), highStart, index3NullOffset, dataNullOffset);
            this.dataArray = data32;
        }

        /**
         * Creates a trie from its binary form.
         * Same as {@link CodePointTrie#fromBinary(Type, ValueWidth, ByteBuffer)}
         * with {@link Type#FAST} and {@link ValueWidth#BITS_32}.
         *
         * @param bytes a buffer containing the binary data of a CodePointTrie
         * @return the trie
         * @stable ICU 63
         */
        public static Fast32 fromBinary(ByteBuffer bytes) {
            return (Fast32) CodePointTrie.fromBinary(Type.FAST, ValueWidth.BITS_32, bytes);
        }

        /**
         * {@inheritDoc}
         * @stable ICU 63
         */
        @Override
        public final int get(int c) {
            return dataArray[cpIndex(c)];
        }

        /**
         * {@inheritDoc}
         * @stable ICU 63
         */
        @Override
        public final int bmpGet(int c) {
            assert 0 <= c && c <= 0xffff;
            return dataArray[fastIndex(c)];
        }

        /**
         * {@inheritDoc}
         * @stable ICU 63
         */
        @Override
        public final int suppGet(int c) {
            assert 0x10000 <= c && c <= 0x10ffff;
            return dataArray[smallIndex(Type.FAST, c)];
        }
    }

    /**
     * A CodePointTrie with {@link Type#FAST} and {@link ValueWidth#BITS_8}.
     *
     * @stable ICU 63
     */
    public static final class Fast8 extends Fast {
        private final byte[] dataArray;

        Fast8(char[] index, byte[] data8, int highStart,
                int index3NullOffset, int dataNullOffset) {
            super(index, new Data8(data8), highStart, index3NullOffset, dataNullOffset);
            this.dataArray = data8;
        }

        /**
         * Creates a trie from its binary form.
         * Same as {@link CodePointTrie#fromBinary(Type, ValueWidth, ByteBuffer)}
         * with {@link Type#FAST} and {@link ValueWidth#BITS_8}.
         *
         * @param bytes a buffer containing the binary data of a CodePointTrie
         * @return the trie
         * @stable ICU 63
         */
        public static Fast8 fromBinary(ByteBuffer bytes) {
            return (Fast8) CodePointTrie.fromBinary(Type.FAST, ValueWidth.BITS_8, bytes);
        }

        /**
         * {@inheritDoc}
         * @stable ICU 63
         */
        @Override
        public final int get(int c) {
            return dataArray[cpIndex(c)] & 0xff;
        }

        /**
         * {@inheritDoc}
         * @stable ICU 63
         */
        @Override
        public final int bmpGet(int c) {
            assert 0 <= c && c <= 0xffff;
            return dataArray[fastIndex(c)] & 0xff;
        }

        /**
         * {@inheritDoc}
         * @stable ICU 63
         */
        @Override
        public final int suppGet(int c) {
            assert 0x10000 <= c && c <= 0x10ffff;
            return dataArray[smallIndex(Type.FAST, c)] & 0xff;
        }
    }

    /**
     * A CodePointTrie with {@link Type#SMALL} and {@link ValueWidth#BITS_16}.
     *
     * @stable ICU 63
     */
    public static final class Small16 extends Small {
        Small16(char[] index, char[] data16, int highStart,
                int index3NullOffset, int dataNullOffset) {
            super(index, new Data16(data16), highStart, index3NullOffset, dataNullOffset);
        }

        /**
         * Creates a trie from its binary form.
         * Same as {@link CodePointTrie#fromBinary(Type, ValueWidth, ByteBuffer)}
         * with {@link Type#SMALL} and {@link ValueWidth#BITS_16}.
         *
         * @param bytes a buffer containing the binary data of a CodePointTrie
         * @return the trie
         * @stable ICU 63
         */
        public static Small16 fromBinary(ByteBuffer bytes) {
            return (Small16) CodePointTrie.fromBinary(Type.SMALL, ValueWidth.BITS_16, bytes);
        }
    }

    /**
     * A CodePointTrie with {@link Type#SMALL} and {@link ValueWidth#BITS_32}.
     *
     * @stable ICU 63
     */
    public static final class Small32 extends Small {
        Small32(char[] index, int[] data32, int highStart,
                int index3NullOffset, int dataNullOffset) {
            super(index, new Data32(data32), highStart, index3NullOffset, dataNullOffset);
        }

        /**
         * Creates a trie from its binary form.
         * Same as {@link CodePointTrie#fromBinary(Type, ValueWidth, ByteBuffer)}
         * with {@link Type#SMALL} and {@link ValueWidth#BITS_32}.
         *
         * @param bytes a buffer containing the binary data of a CodePointTrie
         * @return the trie
         * @stable ICU 63
         */
        public static Small32 fromBinary(ByteBuffer bytes) {
            return (Small32) CodePointTrie.fromBinary(Type.SMALL, ValueWidth.BITS_32, bytes);
        }
    }

    /**
     * A CodePointTrie with {@link Type#SMALL} and {@link ValueWidth#BITS_8}.
     *
     * @stable ICU 63
     */
    public static final class Small8 extends Small {
        Small8(char[] index, byte[] data8, int highStart,
                int index3NullOffset, int dataNullOffset) {
            super(index, new Data8(data8), highStart, index3NullOffset, dataNullOffset);
        }

        /**
         * Creates a trie from its binary form.
         * Same as {@link CodePointTrie#fromBinary(Type, ValueWidth, ByteBuffer)}
         * with {@link Type#SMALL} and {@link ValueWidth#BITS_8}.
         *
         * @param bytes a buffer containing the binary data of a CodePointTrie
         * @return the trie
         * @stable ICU 63
         */
        public static Small8 fromBinary(ByteBuffer bytes) {
            return (Small8) CodePointTrie.fromBinary(Type.SMALL, ValueWidth.BITS_8, bytes);
        }
    }
}
