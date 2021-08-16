/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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


/**
 * Determine length of this Standard UTF-8 in Modified UTF-8.
 *    Validation is done of the basic UTF encoding rules, returns
 *    length (no change) when errors are detected in the UTF encoding.
 *
 *    Note: Accepts Modified UTF-8 also, no verification on the
 *          correctness of Standard UTF-8 is done. e,g, 0xC080 input is ok.
 */
int
modifiedUtf8LengthOfUtf8(char* string, int length) {
    int new_length;
    int i;

    new_length = 0;
    /*
     * if length < 0 or new_length becomes < 0 => string is too big
     * (handled as error after the cycle).
     */
    for ( i = 0 ; i < length && new_length >= 0 ; i++ ) {
        unsigned byte;

        byte = (unsigned char)string[i];
        if ( (byte & 0x80) == 0 ) { /* 1byte encoding */
            new_length++;
            if ( byte == 0 ) {
                new_length++; /* We gain one byte in length on NULL bytes */
            }
        } else if ( (byte & 0xE0) == 0xC0 ) { /* 2byte encoding */
            /* Check encoding of following bytes */
            if ( (i+1) >= length || (string[i+1] & 0xC0) != 0x80 ) {
                break; /* Error condition */
            }
            i++; /* Skip next byte */
            new_length += 2;
        } else if ( (byte & 0xF0) == 0xE0 ) { /* 3byte encoding */
            /* Check encoding of following bytes */
            if ( (i+2) >= length || (string[i+1] & 0xC0) != 0x80
                                 || (string[i+2] & 0xC0) != 0x80 ) {
                break; /* Error condition */
            }
            i += 2; /* Skip next two bytes */
            new_length += 3;
        } else if ( (byte & 0xF8) == 0xF0 ) { /* 4byte encoding */
            /* Check encoding of following bytes */
            if ( (i+3) >= length || (string[i+1] & 0xC0) != 0x80
                                 || (string[i+2] & 0xC0) != 0x80
                                 || (string[i+3] & 0xC0) != 0x80 ) {
                break; /* Error condition */
            }
            i += 3; /* Skip next 3 bytes */
            new_length += 6; /* 4byte encoding turns into 2 3byte ones */
        } else {
            break; /* Error condition */
        }
    }
    if ( i != length ) {
        /* Error in finding new length, return old length so no conversion */
        /* FIXUP: ERROR_MESSAGE? */
        return length;
    }
    return new_length;
}

/*
 * Convert Standard UTF-8 to Modified UTF-8.
 *    Assumes the UTF-8 encoding was validated by modifiedLength() above.
 *
 *    Note: Accepts Modified UTF-8 also, no verification on the
 *          correctness of Standard UTF-8 is done. e,g, 0xC080 input is ok.
 */
void
convertUtf8ToModifiedUtf8(char *string, int length, char *new_string, int new_length)
{
    int i;
    int j;

    j = 0;
    for ( i = 0 ; i < length ; i++ ) {
        unsigned byte1;

        byte1 = (unsigned char)string[i];

        /* NULL bytes and bytes starting with 11110xxx are special */
        if ( (byte1 & 0x80) == 0 ) { /* 1byte encoding */
            if ( byte1 == 0 ) {
                /* Bits out: 11000000 10000000 */
                new_string[j++] = (char)0xC0;
                new_string[j++] = (char)0x80;
            } else {
                /* Single byte */
                new_string[j++] = byte1;
            }
        } else if ( (byte1 & 0xE0) == 0xC0 ) { /* 2byte encoding */
            new_string[j++] = byte1;
            new_string[j++] = string[++i];
        } else if ( (byte1 & 0xF0) == 0xE0 ) { /* 3byte encoding */
            new_string[j++] = byte1;
            new_string[j++] = string[++i];
            new_string[j++] = string[++i];
        } else if ( (byte1 & 0xF8) == 0xF0 ) { /* 4byte encoding */
            /* Beginning of 4byte encoding, turn into 2 3byte encodings */
            unsigned byte2, byte3, byte4, u21;

            /* Bits in: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx */
            byte2 = (unsigned char)string[++i];
            byte3 = (unsigned char)string[++i];
            byte4 = (unsigned char)string[++i];
            /* Reconstruct full 21bit value */
            u21  = (byte1 & 0x07) << 18;
            u21 += (byte2 & 0x3F) << 12;
            u21 += (byte3 & 0x3F) << 6;
            u21 += (byte4 & 0x3F);
            /* Bits out: 11101101 1010xxxx 10xxxxxx */
            new_string[j++] = (char)0xED;
            new_string[j++] = 0xA0 + (((u21 >> 16) - 1) & 0x0F);
            new_string[j++] = 0x80 + ((u21 >> 10) & 0x3F);
            /* Bits out: 11101101 1011xxxx 10xxxxxx */
            new_string[j++] = (char)0xED;
            new_string[j++] = 0xB0 + ((u21 >>  6) & 0x0F);
            new_string[j++] = byte4;
        }
    }
    new_string[j] = 0;
}
