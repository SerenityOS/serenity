/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.ldap;

import java.io.UnsupportedEncodingException;

/**
  * A BER encoder.
  *
  * @author Jagane Sundar
  * @author Scott Seligman
  * @author Vincent Ryan
  */
public final class BerEncoder extends Ber {

    private int curSeqIndex;
    private int seqOffset[];
    private static final int INITIAL_SEQUENCES = 16;
    private static final int DEFAULT_BUFSIZE = 1024;

    // When buf is full, expand its size by the following factor.
    private static final int BUF_GROWTH_FACTOR = 8;

    /**
     * Creates a BER buffer for encoding.
     */
    public BerEncoder() {
        this(DEFAULT_BUFSIZE);
    }

    /**
     * Creates a BER buffer of a specified size for encoding.
     * Specify the initial bufsize.  Buffer will be expanded as needed.
     * @param bufsize The number of bytes for the buffer.
     */
    public BerEncoder(int bufsize) {
        buf = new byte[bufsize];
        this.bufsize = bufsize;
        offset = 0;

        seqOffset = new int[INITIAL_SEQUENCES];
        curSeqIndex = 0;
    }

    /**
     * Resets encoder to state when newly constructed.  Zeros out
     * internal data structures.
     */
    public void reset() {
        while (offset > 0) {
            buf[--offset] = 0;
        }
        while (curSeqIndex > 0) {
            seqOffset[--curSeqIndex] = 0;
        }
    }

// ------------------ Accessor methods ------------

    /**
     * Gets the number of encoded bytes in this BER buffer.
     */
    public int getDataLen() {
        return offset;
    }

    /**
     * Gets the buffer that contains the BER encoding. Throws an
     * exception if unmatched beginSeq() and endSeq() pairs were
     * encountered. Not entire buffer contains encoded bytes.
     * Use getDataLen() to determine number of encoded bytes.
     * Use getBuffer(true) to get rid of excess bytes in array.
     * @throws IllegalStateException If buffer contains unbalanced sequence.
     */
    public byte[] getBuf() {
        if (curSeqIndex != 0) {
            throw new IllegalStateException("BER encode error: Unbalanced SEQUENCEs.");
        }
        return buf;     // shared buffer, be careful to use this method.
    }

    /**
     * Gets the buffer that contains the BER encoding, trimming unused bytes.
     *
     * @throws IllegalStateException If buffer contains unbalanced sequence.
     */
    public byte[] getTrimmedBuf() {
        int len = getDataLen();
        byte[] trimBuf = new byte[len];

        System.arraycopy(getBuf(), 0, trimBuf, 0, len);
        return trimBuf;
    }

// -------------- encoding methods -------------

    /**
     * Begin encoding a sequence with a tag.
     */
    public void beginSeq(int tag) {

        // Double the size of the SEQUENCE array if it overflows
        if (curSeqIndex >= seqOffset.length) {
            int[] seqOffsetTmp = new int[seqOffset.length * 2];

            for (int i = 0; i < seqOffset.length; i++) {
                seqOffsetTmp[i] = seqOffset[i];
            }
            seqOffset = seqOffsetTmp;
        }

        encodeByte(tag);
        seqOffset[curSeqIndex] = offset;

        // Save space for sequence length.
        // %%% Currently we save enough space for sequences up to 64k.
        //     For larger sequences we'll need to shift the data to the right
        //     in endSeq().  If we could instead pad the length field with
        //     zeros, it would be a big win.
        ensureFreeBytes(3);
        offset += 3;

        curSeqIndex++;
    }

    /**
      * Terminate a BER sequence.
      */
    public void endSeq() throws EncodeException {
        curSeqIndex--;
        if (curSeqIndex < 0) {
            throw new IllegalStateException("BER encode error: Unbalanced SEQUENCEs.");
        }

        int start = seqOffset[curSeqIndex] + 3; // index beyond length field
        int len = offset - start;

        if (len <= 0x7f) {
            shiftSeqData(start, len, -2);
            buf[seqOffset[curSeqIndex]] = (byte) len;
        } else if (len <= 0xff) {
            shiftSeqData(start, len, -1);
            buf[seqOffset[curSeqIndex]] = (byte) 0x81;
            buf[seqOffset[curSeqIndex] + 1] = (byte) len;
        } else if (len <= 0xffff) {
            buf[seqOffset[curSeqIndex]] = (byte) 0x82;
            buf[seqOffset[curSeqIndex] + 1] = (byte) (len >> 8);
            buf[seqOffset[curSeqIndex] + 2] = (byte) len;
        } else if (len <= 0xffffff) {
            shiftSeqData(start, len, 1);
            buf[seqOffset[curSeqIndex]] = (byte) 0x83;
            buf[seqOffset[curSeqIndex] + 1] = (byte) (len >> 16);
            buf[seqOffset[curSeqIndex] + 2] = (byte) (len >> 8);
            buf[seqOffset[curSeqIndex] + 3] = (byte) len;
        } else {
            throw new EncodeException("SEQUENCE too long");
        }
    }

    /**
     * Shifts contents of buf in the range [start,start+len) a specified amount.
     * Positive shift value means shift to the right.
     */
    private void shiftSeqData(int start, int len, int shift) {
        if (shift > 0) {
            ensureFreeBytes(shift);
        }
        System.arraycopy(buf, start, buf, start + shift, len);
        offset += shift;
    }

    /**
     * Encode a single byte.
     */
    public void encodeByte(int b) {
        ensureFreeBytes(1);
        buf[offset++] = (byte) b;
    }

/*
    private void deleteByte() {
        offset--;
    }
*/


    /*
     * Encodes an int.
     *<blockquote><pre>
     * BER integer ::= 0x02 berlength byte {byte}*
     *</pre></blockquote>
     */
    public void encodeInt(int i) {
        encodeInt(i, 0x02);
    }

    /**
     * Encodes an int and a tag.
     *<blockquote><pre>
     * BER integer w tag ::= tag berlength byte {byte}*
     *</pre></blockquote>
     */
    public void encodeInt(int i, int tag) {
        int mask = 0xff800000;
        int intsize = 4;

        while( (((i & mask) == 0) || ((i & mask) == mask)) && (intsize > 1) ) {
            intsize--;
            i <<= 8;
        }

        encodeInt(i, tag, intsize);
    }

    //
    // encodes an int using numbytes for the actual encoding.
    //
    private void encodeInt(int i, int tag, int intsize) {

        //
        // integer ::= 0x02 asnlength byte {byte}*
        //

        if (intsize > 4) {
            throw new IllegalArgumentException("BER encode error: INTEGER too long.");
        }

        ensureFreeBytes(2 + intsize);

        buf[offset++] = (byte) tag;
        buf[offset++] = (byte) intsize;

        int mask = 0xff000000;

        while (intsize-- > 0) {
            buf[offset++] = (byte) ((i & mask) >> 24);
            i <<= 8;
        }
    }

    /**
     * Encodes a boolean.
     *<blockquote><pre>
     * BER boolean ::= 0x01 0x01 {0xff|0x00}
     *</pre></blockquote>
     */
    public void encodeBoolean(boolean b) {
        encodeBoolean(b, ASN_BOOLEAN);
    }


    /**
     * Encodes a boolean and a tag
     *<blockquote><pre>
     * BER boolean w TAG ::= tag 0x01 {0xff|0x00}
     *</pre></blockquote>
     */
    public void encodeBoolean(boolean b, int tag) {
        ensureFreeBytes(3);

        buf[offset++] = (byte) tag;
        buf[offset++] = 0x01;
        buf[offset++] = b ? (byte) 0xff : (byte) 0x00;
    }

    /**
     * Encodes a string.
     *<blockquote><pre>
     * BER string ::= 0x04 strlen byte1 byte2...
     *</pre></blockquote>
     * The string is converted into bytes using UTF-8 or ISO-Latin-1.
     */
    public void encodeString(String str, boolean encodeUTF8)
        throws EncodeException {
        encodeString(str, ASN_OCTET_STR, encodeUTF8);
    }

    /**
     * Encodes a string and a tag.
     *<blockquote><pre>
     * BER string w TAG ::= tag strlen byte1 byte2...
     *</pre></blockquote>
     */
    public void encodeString(String str, int tag, boolean encodeUTF8)
        throws EncodeException {

        encodeByte(tag);

        int i = 0;
        int count;
        byte[] bytes = null;

        if (str == null) {
            count = 0;
        } else if (encodeUTF8) {
            try {
                bytes = str.getBytes("UTF8");
                count = bytes.length;
            } catch (UnsupportedEncodingException e) {
                throw new EncodeException("UTF8 not available on platform");
            }
        } else {
            try {
                bytes = str.getBytes("8859_1");
                count = bytes.length;
            } catch (UnsupportedEncodingException e) {
                throw new EncodeException("8859_1 not available on platform");
            }
        }

        encodeLength(count);

        ensureFreeBytes(count);
        while (i < count) {
            buf[offset++] = bytes[i++];
        }
    }

    /**
     * Encodes a portion of an octet string and a tag.
     */
    public void encodeOctetString(byte tb[], int tag, int tboffset, int length)
        throws EncodeException {

        encodeByte(tag);
        encodeLength(length);

        if (length > 0) {
            ensureFreeBytes(length);
            System.arraycopy(tb, tboffset, buf, offset, length);
            offset += length;
        }
    }

    /**
      * Encodes an octet string and a tag.
      */
    public void encodeOctetString(byte tb[], int tag) throws EncodeException {
        encodeOctetString(tb, tag, 0, tb.length);
    }

    private void encodeLength(int len) throws EncodeException {
        ensureFreeBytes(4);     // worst case

        if (len < 128) {
            buf[offset++] = (byte) len;
        } else if (len <= 0xff) {
            buf[offset++] = (byte) 0x81;
            buf[offset++] = (byte) len;
        } else if (len <= 0xffff) {
            buf[offset++] = (byte) 0x82;
            buf[offset++] = (byte) (len >> 8);
            buf[offset++] = (byte) (len & 0xff);
        } else if (len <= 0xffffff) {
            buf[offset++] = (byte) 0x83;
            buf[offset++] = (byte) (len >> 16);
            buf[offset++] = (byte) (len >> 8);
            buf[offset++] = (byte) (len & 0xff);
        } else {
            throw new EncodeException("string too long");
        }
    }

    /**
     * Encodes an array of strings.
     */
    public void encodeStringArray(String strs[], boolean encodeUTF8)
        throws EncodeException {
        if (strs == null)
            return;
        for (int i = 0; i < strs.length; i++) {
            encodeString(strs[i], encodeUTF8);
        }
    }
/*
    private void encodeNull() {

        //
        // NULL ::= 0x05 0x00
        //
        encodeByte(0x05);
        encodeByte(0x00);
    }
*/

    /**
     * Ensures that there are at least "len" unused bytes in "buf".
     * When more space is needed "buf" is expanded by a factor of
     * BUF_GROWTH_FACTOR, then "len" bytes are added if "buf" still
     * isn't large enough.
     */
    private void ensureFreeBytes(int len) {
        if (bufsize - offset < len) {
            int newsize = bufsize * BUF_GROWTH_FACTOR;
            if (newsize - offset < len) {
                newsize += len;
            }
            byte newbuf[] = new byte[newsize];
            // Only copy bytes in the range [0, offset)
            System.arraycopy(buf, 0, newbuf, 0, offset);

            buf = newbuf;
            bufsize = newsize;
        }
    }
}
