/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.generatecharacter;

import java.text.*;
import java.util.*;

public class Utility {
    static byte peekByte(String s, int index) {
        char c = s.charAt(index/2);
        return ((index&1)==0)?(byte)(c>>8):(byte)c;
    }

    static short peekShort(String s, int index) {
        return (short)s.charAt(index);
    }

    static int peekInt(String s, int index) {
        index *= 2;
        return (((int)s.charAt(index)) << 16) | s.charAt(index+1);
    }

    static void poke(String s, int index, byte value) {
        int mask = 0xFF00;
        int ivalue = value;
        if ((index&1)==0) {
            ivalue <<= 8;
            mask = 0x00FF;
        }
        index /= 2;
        if (index == s.length()) {
            s = s + (char)ivalue;
        }
        else if (index == 0) {
            s = (char)(ivalue|(s.charAt(0)&mask)) + s.substring(1);
        }
        else {
            s = s.substring(0, index) + (char)(ivalue|(s.charAt(index)&mask))
                + s.substring(index+1);
        }
    }

    static void poke(String s, int index, short value) {
        if (index == s.length()) {
            s = s + (char)value;
        }
        else if (index == 0) {
            s = (char)value + s.substring(1);
        }
        else {
            s = s.substring(0, index) + (char)value + s.substring(index+1);
        }
    }

    static void poke(String s, int index, int value) {
        index *= 2;
        char hi = (char)(value >> 16);
        if (index == s.length()) {
            s = s + hi + (char)value;
        }
        else if (index == 0) {
            s = hi + (char)value + s.substring(2);
        }
        else {
            s = s.substring(0, index) + hi + (char)value + s.substring(index+2);
        }
    }

    /**
     * The ESCAPE character is used during run-length encoding.  It signals
     * a run of identical chars.
     */
    static final char ESCAPE = '\uA5A5';

    /**
     * The ESCAPE_BYTE character is used during run-length encoding.  It signals
     * a run of identical bytes.
     */
    static final byte ESCAPE_BYTE = (byte)0xA5;

    /**
     * Construct a string representing a short array.  Use run-length encoding.
     * A character represents itself, unless it is the ESCAPE character.  Then
     * the following notations are possible:
     *   ESCAPE ESCAPE   ESCAPE literal
     *   ESCAPE n c      n instances of character c
     * Since an encoded run occupies 3 characters, we only encode runs of 4 or
     * more characters.  Thus we have n > 0 and n != ESCAPE and n <= 0xFFFF.
     * If we encounter a run where n == ESCAPE, we represent this as:
     *   c ESCAPE n-1 c
     * The ESCAPE value is chosen so as not to collide with commonly
     * seen values.
     */
    static final String arrayToRLEString(short[] a) {
        StringBuffer buffer = new StringBuffer();
        // for (int i=0; i<a.length; ++i) buffer.append((char) a[i]);
        buffer.append((char) (a.length >> 16));
        buffer.append((char) a.length);
        short runValue = a[0];
        int runLength = 1;
        for (int i=1; i<a.length; ++i) {
            short s = a[i];
            if (s == runValue && runLength < 0xFFFF) ++runLength;
            else {
                encodeRun(buffer, runValue, runLength);
                runValue = s;
                runLength = 1;
            }
        }
        encodeRun(buffer, runValue, runLength);
        return buffer.toString();
    }

    /**
     * Construct a string representing a byte array.  Use run-length encoding.
     * Two bytes are packed into a single char, with a single extra zero byte at
     * the end if needed.  A byte represents itself, unless it is the
     * ESCAPE_BYTE.  Then the following notations are possible:
     *   ESCAPE_BYTE ESCAPE_BYTE   ESCAPE_BYTE literal
     *   ESCAPE_BYTE n b           n instances of byte b
     * Since an encoded run occupies 3 bytes, we only encode runs of 4 or
     * more bytes.  Thus we have n > 0 and n != ESCAPE_BYTE and n <= 0xFF.
     * If we encounter a run where n == ESCAPE_BYTE, we represent this as:
     *   b ESCAPE_BYTE n-1 b
     * The ESCAPE_BYTE value is chosen so as not to collide with commonly
     * seen values.
     */
    static final String arrayToRLEString(byte[] a) {
        StringBuffer buffer = new StringBuffer();
        buffer.append((char) (a.length >> 16));
        buffer.append((char) a.length);
        byte runValue = a[0];
        int runLength = 1;
        byte[] state = new byte[2];
        for (int i=1; i<a.length; ++i) {
            byte b = a[i];
            if (b == runValue && runLength < 0xFF) ++runLength;
            else {
                encodeRun(buffer, runValue, runLength, state);
                runValue = b;
                runLength = 1;
            }
        }
        encodeRun(buffer, runValue, runLength, state);

        // We must save the final byte, if there is one, by padding
        // an extra zero.
        if (state[0] != 0) appendEncodedByte(buffer, (byte)0, state);

        return buffer.toString();
    }

    /**
     * Encode a run, possibly a degenerate run (of < 4 values).
     * @param length The length of the run; must be > 0 && <= 0xFFFF.
     */
    private static final void encodeRun(StringBuffer buffer, short value, int length) {
        if (length < 4) {
            for (int j=0; j<length; ++j) {
                if (value == (int) ESCAPE) buffer.append(ESCAPE);
                buffer.append((char) value);
            }
        }
        else {
            if (length == (int) ESCAPE) {
                if (value == (int) ESCAPE) buffer.append(ESCAPE);
                buffer.append((char) value);
                --length;
            }
            buffer.append(ESCAPE);
            buffer.append((char) length);
            buffer.append((char) value); // Don't need to escape this value
        }
    }

    /**
     * Encode a run, possibly a degenerate run (of < 4 values).
     * @param length The length of the run; must be > 0 && <= 0xFF.
     */
    private static final void encodeRun(StringBuffer buffer, byte value, int length,
                                        byte[] state) {
        if (length < 4) {
            for (int j=0; j<length; ++j) {
                if (value == ESCAPE_BYTE) appendEncodedByte(buffer, ESCAPE_BYTE, state);
                appendEncodedByte(buffer, value, state);
            }
        }
        else {
            if (length == ESCAPE_BYTE) {
                if (value == ESCAPE_BYTE) appendEncodedByte(buffer, ESCAPE_BYTE, state);
                appendEncodedByte(buffer, value, state);
                --length;
            }
            appendEncodedByte(buffer, ESCAPE_BYTE, state);
            appendEncodedByte(buffer, (byte)length, state);
            appendEncodedByte(buffer, value, state); // Don't need to escape this value
        }
    }

    /**
     * Append a byte to the given StringBuffer, packing two bytes into each
     * character.  The state parameter maintains intermediary data between
     * calls.
     * @param state A two-element array, with state[0] == 0 if this is the
     * first byte of a pair, or state[0] != 0 if this is the second byte
     * of a pair, in which case state[1] is the first byte.
     */
    private static final void appendEncodedByte(StringBuffer buffer, byte value,
                                                byte[] state) {
        if (state[0] != 0) {
            char c = (char) ((state[1] << 8) | (((int) value) & 0xFF));
            buffer.append(c);
            state[0] = 0;
        }
        else {
            state[0] = 1;
            state[1] = value;
        }
    }

    /**
     * Construct an array of shorts from a run-length encoded string.
     */
    static final short[] RLEStringToShortArray(String s) {
        int length = (((int) s.charAt(0)) << 16) | ((int) s.charAt(1));
        short[] array = new short[length];
        int ai = 0;
        for (int i=2; i<s.length(); ++i) {
            char c = s.charAt(i);
            if (c == ESCAPE) {
                c = s.charAt(++i);
                if (c == ESCAPE) array[ai++] = (short) c;
                else {
                    int runLength = (int) c;
                    short runValue = (short) s.charAt(++i);
                    for (int j=0; j<runLength; ++j) array[ai++] = runValue;
                }
            }
            else {
                array[ai++] = (short) c;
            }
        }

        if (ai != length)
            throw new InternalError("Bad run-length encoded short array");

        return array;
    }

    /**
     * Construct an array of bytes from a run-length encoded string.
     */
    static final byte[] RLEStringToByteArray(String s) {
        int length = (((int) s.charAt(0)) << 16) | ((int) s.charAt(1));
        byte[] array = new byte[length];
        boolean nextChar = true;
        char c = 0;
        int node = 0;
        int runLength = 0;
        int i = 2;
        for (int ai=0; ai<length; ) {
            // This part of the loop places the next byte into the local
            // variable 'b' each time through the loop.  It keeps the
            // current character in 'c' and uses the boolean 'nextChar'
            // to see if we've taken both bytes out of 'c' yet.
            byte b;
            if (nextChar) {
                c = s.charAt(i++);
                b = (byte) (c >> 8);
                nextChar = false;
            }
            else {
                b = (byte) (c & 0xFF);
                nextChar = true;
            }

            // This part of the loop is a tiny state machine which handles
            // the parsing of the run-length encoding.  This would be simpler
            // if we could look ahead, but we can't, so we use 'node' to
            // move between three nodes in the state machine.
            switch (node) {
            case 0:
                // Normal idle node
                if (b == ESCAPE_BYTE) {
                    node = 1;
                }
                else {
                    array[ai++] = b;
                }
                break;
            case 1:
                // We have seen one ESCAPE_BYTE; we expect either a second
                // one, or a run length and value.
                if (b == ESCAPE_BYTE) {
                    array[ai++] = ESCAPE_BYTE;
                    node = 0;
                }
                else {
                    runLength = b;
                    // Interpret signed byte as unsigned
                    if (runLength < 0) runLength += 0x100;
                    node = 2;
                }
                break;
            case 2:
                // We have seen an ESCAPE_BYTE and length byte.  We interpret
                // the next byte as the value to be repeated.
                for (int j=0; j<runLength; ++j) array[ai++] = b;
                node = 0;
                break;
            }
        }

        if (node != 0)
            throw new InternalError("Bad run-length encoded byte array");

        if (i != s.length())
            throw new InternalError("Excess data in RLE byte array string");

        return array;
    }

    /**
     * Format a String for representation in a source file.  This includes
     * breaking it into lines escaping characters using octal notation
     * when necessary (control characters and double quotes).
     */
    static final String formatForSource(String s) {
        return formatForSource(s, "        ");
    }

    /**
     * Format a String for representation in a source file.  This includes
     * breaking it into lines escaping characters using octal notation
     * when necessary (control characters and double quotes).
     */
    static final String formatForSource(String s, String indent) {
        StringBuffer buffer = new StringBuffer();
        for (int i=0; i<s.length();) {
            if (i > 0) buffer.append("+\n");
            int limit = buffer.length() + 78; // Leave 2 for trailing <"+>
            buffer.append(indent + '"');
            while (i<s.length() && buffer.length()<limit) {
                char c = s.charAt(i++);
                /* This works too but it's kind of unnecessary; might as
                   well keep things simple.
                if (c == '\\' || c == '"') {
                    // Escape backslash and double-quote.  Don't need to
                    // escape single-quote.
                    buffer.append("\\" + c);
                }
                else if (c >= '\u0020' && c <= '\u007E') {
                    // Printable ASCII ranges from ' ' to '~'
                    buffer.append(c);
                }
                else
                */
                if (c <= '\377') {
                    // Represent control characters
                    // using octal notation; otherwise the string we form
                    // won't compile, since Unicode escape sequences are
                    // processed before tokenization.
                    buffer.append('\\');
                    buffer.append(HEX_DIGIT[(c & 0700) >> 6]); // HEX_DIGIT works for octal
                    buffer.append(HEX_DIGIT[(c & 0070) >> 3]);
                    buffer.append(HEX_DIGIT[(c & 0007)]);
                }
                else {
                    // Handle the rest with Unicode
                    buffer.append("\\u");
                    buffer.append(HEX_DIGIT[(c & 0xF000) >> 12]);
                    buffer.append(HEX_DIGIT[(c & 0x0F00) >> 8]);
                    buffer.append(HEX_DIGIT[(c & 0x00F0) >> 4]);
                    buffer.append(HEX_DIGIT[(c & 0x000F)]);
                }
            }
            buffer.append('"');
        }
        return buffer.toString();
    }

    static final char[] HEX_DIGIT = {'0','1','2','3','4','5','6','7',
                                     '8','9','A','B','C','D','E','F'};
}
