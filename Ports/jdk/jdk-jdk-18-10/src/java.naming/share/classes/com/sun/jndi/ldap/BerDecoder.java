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
  * A BER decoder. Contains methods to parse a BER buffer.
  *
  * @author Jagane Sundar
  * @author Vincent Ryan
  */
public final class BerDecoder extends Ber {

    private int origOffset;  // The start point in buf to decode

    /**
     * Creates a BER decoder that reads bytes from the specified buffer.
     */
    public BerDecoder(byte buf[], int offset, int bufsize) {

        this.buf = buf;         // shared buffer, be careful to use this class
        this.bufsize = bufsize;
        this.origOffset = offset;

        reset();
    }

    /**
     * Resets this decode to start parsing from the initial offset
     * (ie., same state as after calling the constructor).
     */
    public void reset() {
        offset = origOffset;
    }

    /**
      * Returns the current parse position.
      * It points to the byte that will be parsed next.
      * Useful for parsing sequences.
      */
    public int getParsePosition() {
        return offset;
    }

    /**
      * Parses a possibly variable length field.
      */
    public int parseLength() throws DecodeException {

        int lengthbyte = parseByte();

        if ((lengthbyte & 0x80) == 0x80) {

            lengthbyte &= 0x7f;

            if (lengthbyte == 0) {
                throw new DecodeException(
                    "Indefinite length not supported");
            }

            if (lengthbyte > 4) {
                throw new DecodeException("encoding too long");
            }

            if (bufsize - offset < lengthbyte) {
                throw new DecodeException("Insufficient data");
            }

            int retval = 0;

            for( int i = 0; i < lengthbyte; i++) {
                retval = (retval << 8) + (buf[offset++] & 0xff);
            }
            if (retval < 0) {
              throw new DecodeException("Invalid length bytes");
            }
            return retval;
        } else {
            return lengthbyte;
        }
    }

    /**
     * Parses the next sequence in this BER buffer.
     * @param rlen An array for returning size of the sequence in bytes. If null,
     *          the size is not returned.
     * @return The sequence's tag.
     */
    public int parseSeq(int rlen[]) throws DecodeException {

        int seq = parseByte();
        int len = parseLength();
        if (rlen != null) {
            rlen[0] = len;
        }
        return seq;
    }

    /**
     * Used to skip bytes. Usually used when trying to recover from parse error.
     * Don't need to be public right now?
     * @param i The number of bytes to skip
     */
    void seek(int i) throws DecodeException {
        if (offset + i > bufsize || offset + i < 0) {
            throw new DecodeException("array index out of bounds");
        }
        offset += i;
    }

    /**
     * Parses the next byte in this BER buffer.
     * @return The byte parsed.
     */
    public int parseByte() throws DecodeException {
        if (bufsize - offset < 1) {
            throw new DecodeException("Insufficient data");
        }
        return buf[offset++] & 0xff;
    }


    /**
     * Returns the next byte in this BER buffer without consuming it.
     * @return The next byte.
     */
    public int peekByte() throws DecodeException {
        if (bufsize - offset < 1) {
            throw new DecodeException("Insufficient data");
        }
        return buf[offset] & 0xff;
    }

    /**
     * Parses an ASN_BOOLEAN tagged integer from this BER buffer.
     * @return true if the tagged integer is 0; false otherwise.
     */
    public boolean parseBoolean() throws DecodeException {
        return ((parseIntWithTag(ASN_BOOLEAN) == 0x00) ? false : true);
    }

    /**
     * Parses an ASN_ENUMERATED tagged integer from this BER buffer.
     * @return The tag of enumeration.
     */
    public int parseEnumeration() throws DecodeException {
        return parseIntWithTag(ASN_ENUMERATED);
    }

    /**
     * Parses an ASN_INTEGER tagged integer from this BER buffer.
     * @return The value of the integer.
     */
    public int parseInt() throws DecodeException {
        return parseIntWithTag(ASN_INTEGER);
    }

    /**
      * Parses an integer that's preceded by a tag.
      *<blockquote><pre>
      * BER integer ::= tag length byte {byte}*
      *</pre></blockquote>
      */
    private int parseIntWithTag(int tag) throws DecodeException {
        if (parseByte() != tag) {
            // Ber could have been reset;
            String s;
            if (offset > 0) {
                s = Integer.toString(buf[offset - 1] & 0xff);
            } else {
                s = "Empty tag";
            }
            throw new DecodeException("Encountered ASN.1 tag " +
                s + " (expected tag " + Integer.toString(tag) + ")");
        }

        int len = parseLength();

        if (len > 4) {
            throw new DecodeException("INTEGER too long");
        } else if (len > bufsize - offset) {
            throw new DecodeException("Insufficient data");
        }

        byte fb = buf[offset++];
        int value = 0;

        value = fb & 0x7F;
        for( int i = 1 /* first byte already read */ ; i < len; i++) {
            value <<= 8;
            value |= (buf[offset++] & 0xff);
        }

        if ((fb & 0x80) == 0x80) {
            value = -value;
        }

        return value;
    }

    /**
      * Parses a string.
      */
    public String parseString(boolean decodeUTF8) throws DecodeException {
        return parseStringWithTag(ASN_SIMPLE_STRING, decodeUTF8, null);
    }

    /**
      * Parses a string of a given tag from this BER buffer.
      *<blockquote><pre>
      *BER simple string ::= tag length {byte}*
      *</pre></blockquote>
      * @param rlen An array for holding the relative parsed offset; if null
      *  offset not set.
      * @param decodeUTF8 If true, use UTF-8 when decoding the string; otherwise
      * use ISO-Latin-1 (8859_1). Use true for LDAPv3; false for LDAPv2.
      * @param tag The tag that precedes the string.
      * @return The non-null parsed string.
      */
    public String parseStringWithTag(int tag, boolean decodeUTF8, int rlen[])
        throws DecodeException {

        int st;
        int origOffset = offset;

        if ((st = parseByte()) != tag) {
            throw new DecodeException("Encountered ASN.1 tag " +
                Integer.toString((byte)st) + " (expected tag " + tag + ")");
        }

        int len = parseLength();

        if (len > bufsize - offset) {
            throw new DecodeException("Insufficient data");
        }

        String retstr;
        if (len == 0) {
            retstr = "";
        } else {
            byte[] buf2 = new byte[len];

            System.arraycopy(buf, offset, buf2, 0, len);
            if (decodeUTF8) {
                try {
                    retstr = new String(buf2, "UTF8");
                } catch (UnsupportedEncodingException e) {
                    throw new DecodeException("UTF8 not available on platform");
                }
            } else {
                try {
                    retstr = new String(buf2, "8859_1");
                } catch (UnsupportedEncodingException e) {
                    throw new DecodeException("8859_1 not available on platform");
                }
            }
            offset += len;
        }

        if (rlen != null) {
            rlen[0] = offset - origOffset;
        }

        return retstr;
    }

    /**
     * Parses an octet string of a given type(tag) from this BER buffer.
     * <blockquote><pre>
     * BER Binary Data of type "tag" ::= tag length {byte}*
     *</pre></blockquote>
     *
     * @param tag The tag to look for.
     * @param rlen An array for returning the relative parsed position. If null,
     *          the relative parsed position is not returned.
     * @return A non-null array containing the octet string.
     * @throws DecodeException If the next byte in the BER buffer is not
     * {@code tag}, or if length specified in the BER buffer exceeds the
     * number of bytes left in the buffer.
     */
    public byte[] parseOctetString(int tag, int rlen[]) throws DecodeException {

        int origOffset = offset;
        int st;
        if ((st = parseByte()) != tag) {

            throw new DecodeException("Encountered ASN.1 tag " +
                Integer.toString(st) +
                " (expected tag " + Integer.toString(tag) + ")");
        }

        int len = parseLength();

        if (len > bufsize - offset) {
            throw new DecodeException("Insufficient data");
        }

        byte retarr[] = new byte[len];
        if (len > 0) {
            System.arraycopy(buf, offset, retarr, 0, len);
            offset += len;
        }

        if (rlen != null) {
            rlen[0] = offset - origOffset;
        }

        return retarr;
    }

    /**
     * Returns the number of unparsed bytes in this BER buffer.
     */
    public int bytesLeft() {
        return bufsize - offset;
    }
}
