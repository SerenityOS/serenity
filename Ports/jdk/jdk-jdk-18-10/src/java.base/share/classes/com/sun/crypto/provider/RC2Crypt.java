/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.crypto.provider;

import java.security.InvalidKeyException;

/**
 * Implementation of the RC2(tm) algorithm as described in RFC 2268.
 *
 * RC2 is a 16-bit based algorithm and not particularly fast on 32/64 bit
 * architectures. Also, note that although the JVM has a 16-bit integer
 * type (short), all expressions are evaluated either in 32 or 64 bit
 * (int or long). Expression such as "s1 = s2 + s3" are implemented by
 * first promoting s2 and s3 to int, performing an int addition, and
 * then demoting the result back to short to store in s1. To avoid this
 * fairly slow process, we use the int type throughout and manually insert
 * "& 0xffff" where necessary.
 *
 * @since   1.5
 * @author  Andreas Sterbenz
 */
final class RC2Crypt extends SymmetricCipher {

    // PITABLE from the RFC, used in key setup
    private static final int[] PI_TABLE = new int[] {
        0xd9, 0x78, 0xf9, 0xc4, 0x19, 0xdd, 0xb5, 0xed,
        0x28, 0xe9, 0xfd, 0x79, 0x4a, 0xa0, 0xd8, 0x9d,
        0xc6, 0x7e, 0x37, 0x83, 0x2b, 0x76, 0x53, 0x8e,
        0x62, 0x4c, 0x64, 0x88, 0x44, 0x8b, 0xfb, 0xa2,
        0x17, 0x9a, 0x59, 0xf5, 0x87, 0xb3, 0x4f, 0x13,
        0x61, 0x45, 0x6d, 0x8d, 0x09, 0x81, 0x7d, 0x32,
        0xbd, 0x8f, 0x40, 0xeb, 0x86, 0xb7, 0x7b, 0x0b,
        0xf0, 0x95, 0x21, 0x22, 0x5c, 0x6b, 0x4e, 0x82,
        0x54, 0xd6, 0x65, 0x93, 0xce, 0x60, 0xb2, 0x1c,
        0x73, 0x56, 0xc0, 0x14, 0xa7, 0x8c, 0xf1, 0xdc,
        0x12, 0x75, 0xca, 0x1f, 0x3b, 0xbe, 0xe4, 0xd1,
        0x42, 0x3d, 0xd4, 0x30, 0xa3, 0x3c, 0xb6, 0x26,
        0x6f, 0xbf, 0x0e, 0xda, 0x46, 0x69, 0x07, 0x57,
        0x27, 0xf2, 0x1d, 0x9b, 0xbc, 0x94, 0x43, 0x03,
        0xf8, 0x11, 0xc7, 0xf6, 0x90, 0xef, 0x3e, 0xe7,
        0x06, 0xc3, 0xd5, 0x2f, 0xc8, 0x66, 0x1e, 0xd7,
        0x08, 0xe8, 0xea, 0xde, 0x80, 0x52, 0xee, 0xf7,
        0x84, 0xaa, 0x72, 0xac, 0x35, 0x4d, 0x6a, 0x2a,
        0x96, 0x1a, 0xd2, 0x71, 0x5a, 0x15, 0x49, 0x74,
        0x4b, 0x9f, 0xd0, 0x5e, 0x04, 0x18, 0xa4, 0xec,
        0xc2, 0xe0, 0x41, 0x6e, 0x0f, 0x51, 0xcb, 0xcc,
        0x24, 0x91, 0xaf, 0x50, 0xa1, 0xf4, 0x70, 0x39,
        0x99, 0x7c, 0x3a, 0x85, 0x23, 0xb8, 0xb4, 0x7a,
        0xfc, 0x02, 0x36, 0x5b, 0x25, 0x55, 0x97, 0x31,
        0x2d, 0x5d, 0xfa, 0x98, 0xe3, 0x8a, 0x92, 0xae,
        0x05, 0xdf, 0x29, 0x10, 0x67, 0x6c, 0xba, 0xc9,
        0xd3, 0x00, 0xe6, 0xcf, 0xe1, 0x9e, 0xa8, 0x2c,
        0x63, 0x16, 0x01, 0x3f, 0x58, 0xe2, 0x89, 0xa9,
        0x0d, 0x38, 0x34, 0x1b, 0xab, 0x33, 0xff, 0xb0,
        0xbb, 0x48, 0x0c, 0x5f, 0xb9, 0xb1, 0xcd, 0x2e,
        0xc5, 0xf3, 0xdb, 0x47, 0xe5, 0xa5, 0x9c, 0x77,
        0x0a, 0xa6, 0x20, 0x68, 0xfe, 0x7f, 0xc1, 0xad,
    };

    // expanded key, 64 times 16-bit words
    private final int[] expandedKey;

    // effective key bits
    private int effectiveKeyBits;

    RC2Crypt() {
        expandedKey = new int[64];
    }

    int getBlockSize() {
        return 8;
    }

    int getEffectiveKeyBits() {
        return effectiveKeyBits;
    }

    /**
     * Initializes the effective key bit size. This method is a hook to
     * allow RC2Cipher to initialize the effective key size.
     */
    void initEffectiveKeyBits(int effectiveKeyBits) {
        this.effectiveKeyBits = effectiveKeyBits;
    }

    static void checkKey(String algorithm, int keyLength)
            throws InvalidKeyException {
        if (algorithm.equals("RC2") == false) {
            throw new InvalidKeyException("Key algorithm must be RC2");
        }
        if ((keyLength < 5) || (keyLength > 128)) {
            throw new InvalidKeyException
                ("RC2 key length must be between 40 and 1024 bit");
        }
    }

    void init(boolean decrypting, String algorithm, byte[] key)
            throws InvalidKeyException {
        int keyLength = key.length;
        if (effectiveKeyBits == 0) {
            effectiveKeyBits = keyLength << 3;
        }

        checkKey(algorithm, keyLength);

        // key buffer, the L[] byte array from the spec
        byte[] expandedKeyBytes = new byte[128];

        // place key into key buffer
        System.arraycopy(key, 0, expandedKeyBytes, 0, keyLength);

        // first loop
        int t = expandedKeyBytes[keyLength - 1];
        for (int i = keyLength; i < 128; i++) {
            t = PI_TABLE[(t + expandedKeyBytes[i - keyLength]) & 0xff];
            expandedKeyBytes[i] = (byte)t;
        }

        int t8 = (effectiveKeyBits + 7) >> 3;
        int tm = 0xff >> (-effectiveKeyBits & 7);

        // second loop, reduce search space to effective key bits
        t = PI_TABLE[expandedKeyBytes[128 - t8] & tm];
        expandedKeyBytes[128 - t8] = (byte)t;
        for (int i = 127 - t8; i >= 0; i--) {
            t = PI_TABLE[t ^ (expandedKeyBytes[i + t8] & 0xff)];
            expandedKeyBytes[i] = (byte)t;
        }

        // byte to short conversion, little endian (copy into K[])
        for (int i = 0, j = 0; i < 64; i++, j += 2) {
            t =  (expandedKeyBytes[j    ] & 0xff)
              + ((expandedKeyBytes[j + 1] & 0xff) << 8);
            expandedKey[i] = t;
        }
    }

    /**
     * Encrypt a single block. Note that in a few places we omit a "& 0xffff"
     * and allow variables to become larger than 16 bit. This still works
     * because there is never a 32 bit overflow.
     */
    void encryptBlock(byte[] in, int inOfs, byte[] out, int outOfs) {
        int R0 =  (in[inOfs    ] & 0xff)
               + ((in[inOfs + 1] & 0xff) << 8);
        int R1 =  (in[inOfs + 2] & 0xff)
               + ((in[inOfs + 3] & 0xff) << 8);
        int R2 =  (in[inOfs + 4] & 0xff)
               + ((in[inOfs + 5] & 0xff) << 8);
        int R3 =  (in[inOfs + 6] & 0xff)
               + ((in[inOfs + 7] & 0xff) << 8);

        // 5 mixing rounds
        for (int i = 0; i < 20; i += 4) {
            R0 = (R0 + expandedKey[i    ] + (R3 & R2) + (~R3 & R1)) & 0xffff;
            R0 = (R0 << 1) | (R0 >>> 15);

            R1 = (R1 + expandedKey[i + 1] + (R0 & R3) + (~R0 & R2)) & 0xffff;
            R1 = (R1 << 2) | (R1 >>> 14);

            R2 = (R2 + expandedKey[i + 2] + (R1 & R0) + (~R1 & R3)) & 0xffff;
            R2 = (R2 << 3) | (R2 >>> 13);

            R3 = (R3 + expandedKey[i + 3] + (R2 & R1) + (~R2 & R0)) & 0xffff;
            R3 = (R3 << 5) | (R3 >>> 11);
        }

        // 1 mashing round
        R0 += expandedKey[R3 & 0x3f];
        R1 += expandedKey[R0 & 0x3f];
        R2 += expandedKey[R1 & 0x3f];
        R3 += expandedKey[R2 & 0x3f];

        // 6 mixing rounds
        for (int i = 20; i < 44; i += 4) {
            R0 = (R0 + expandedKey[i    ] + (R3 & R2) + (~R3 & R1)) & 0xffff;
            R0 = (R0 << 1) | (R0 >>> 15);

            R1 = (R1 + expandedKey[i + 1] + (R0 & R3) + (~R0 & R2)) & 0xffff;
            R1 = (R1 << 2) | (R1 >>> 14);

            R2 = (R2 + expandedKey[i + 2] + (R1 & R0) + (~R1 & R3)) & 0xffff;
            R2 = (R2 << 3) | (R2 >>> 13);

            R3 = (R3 + expandedKey[i + 3] + (R2 & R1) + (~R2 & R0)) & 0xffff;
            R3 = (R3 << 5) | (R3 >>> 11);
        }

        // 1 mashing round
        R0 += expandedKey[R3 & 0x3f];
        R1 += expandedKey[R0 & 0x3f];
        R2 += expandedKey[R1 & 0x3f];
        R3 += expandedKey[R2 & 0x3f];

        // 5 mixing rounds
        for (int i = 44; i < 64; i += 4) {
            R0 = (R0 + expandedKey[i    ] + (R3 & R2) + (~R3 & R1)) & 0xffff;
            R0 = (R0 << 1) | (R0 >>> 15);

            R1 = (R1 + expandedKey[i + 1] + (R0 & R3) + (~R0 & R2)) & 0xffff;
            R1 = (R1 << 2) | (R1 >>> 14);

            R2 = (R2 + expandedKey[i + 2] + (R1 & R0) + (~R1 & R3)) & 0xffff;
            R2 = (R2 << 3) | (R2 >>> 13);

            R3 = (R3 + expandedKey[i + 3] + (R2 & R1) + (~R2 & R0)) & 0xffff;
            R3 = (R3 << 5) | (R3 >>> 11);
        }

        out[outOfs    ] = (byte)R0;
        out[outOfs + 1] = (byte)(R0 >> 8);
        out[outOfs + 2] = (byte)R1;
        out[outOfs + 3] = (byte)(R1 >> 8);
        out[outOfs + 4] = (byte)R2;
        out[outOfs + 5] = (byte)(R2 >> 8);
        out[outOfs + 6] = (byte)R3;
        out[outOfs + 7] = (byte)(R3 >> 8);
    }

    void decryptBlock(byte[] in, int inOfs, byte[] out, int outOfs) {
        int R0 =  (in[inOfs    ] & 0xff)
               + ((in[inOfs + 1] & 0xff) << 8);
        int R1 =  (in[inOfs + 2] & 0xff)
               + ((in[inOfs + 3] & 0xff) << 8);
        int R2 =  (in[inOfs + 4] & 0xff)
               + ((in[inOfs + 5] & 0xff) << 8);
        int R3 =  (in[inOfs + 6] & 0xff)
               + ((in[inOfs + 7] & 0xff) << 8);

        // 5 r-mixing rounds
        for(int i = 64; i > 44; i -= 4) {
            R3 = ((R3 << 11) | (R3 >>> 5)) & 0xffff;
            R3 = (R3 - expandedKey[i - 1] - (R2 & R1) - (~R2 & R0)) & 0xffff;

            R2 = ((R2 << 13) | (R2 >>> 3)) & 0xffff;
            R2 = (R2 - expandedKey[i - 2] - (R1 & R0) - (~R1 & R3)) & 0xffff;

            R1 = ((R1 << 14) | (R1 >>> 2)) & 0xffff;
            R1 = (R1 - expandedKey[i - 3] - (R0 & R3) - (~R0 & R2)) & 0xffff;

            R0 = ((R0 << 15) | (R0 >>> 1)) & 0xffff;
            R0 = (R0 - expandedKey[i - 4] - (R3 & R2) - (~R3 & R1)) & 0xffff;
        }

        // 1 r-mashing round
        R3 = (R3 - expandedKey[R2 & 0x3f]) & 0xffff;
        R2 = (R2 - expandedKey[R1 & 0x3f]) & 0xffff;
        R1 = (R1 - expandedKey[R0 & 0x3f]) & 0xffff;
        R0 = (R0 - expandedKey[R3 & 0x3f]) & 0xffff;

        // 6 r-mixing rounds
        for(int i = 44; i > 20; i -= 4) {
            R3 = ((R3 << 11) | (R3 >>> 5)) & 0xffff;
            R3 = (R3 - expandedKey[i - 1] - (R2 & R1) - (~R2 & R0)) & 0xffff;

            R2 = ((R2 << 13) | (R2 >>> 3)) & 0xffff;
            R2 = (R2 - expandedKey[i - 2] - (R1 & R0) - (~R1 & R3)) & 0xffff;

            R1 = ((R1 << 14) | (R1 >>> 2)) & 0xffff;
            R1 = (R1 - expandedKey[i - 3] - (R0 & R3) - (~R0 & R2)) & 0xffff;

            R0 = ((R0 << 15) | (R0 >>> 1)) & 0xffff;
            R0 = (R0 - expandedKey[i - 4] - (R3 & R2) - (~R3 & R1)) & 0xffff;
        }

        // 1 r-mashing round
        R3 = (R3 - expandedKey[R2 & 0x3f]) & 0xffff;
        R2 = (R2 - expandedKey[R1 & 0x3f]) & 0xffff;
        R1 = (R1 - expandedKey[R0 & 0x3f]) & 0xffff;
        R0 = (R0 - expandedKey[R3 & 0x3f]) & 0xffff;

        // 5 r-mixing rounds
        for(int i = 20; i > 0; i -= 4) {
            R3 = ((R3 << 11) | (R3 >>> 5)) & 0xffff;
            R3 = (R3 - expandedKey[i - 1] - (R2 & R1) - (~R2 & R0)) & 0xffff;

            R2 = ((R2 << 13) | (R2 >>> 3)) & 0xffff;
            R2 = (R2 - expandedKey[i - 2] - (R1 & R0) - (~R1 & R3)) & 0xffff;

            R1 = ((R1 << 14) | (R1 >>> 2)) & 0xffff;
            R1 = (R1 - expandedKey[i - 3] - (R0 & R3) - (~R0 & R2)) & 0xffff;

            R0 = ((R0 << 15) | (R0 >>> 1)) & 0xffff;
            R0 = (R0 - expandedKey[i - 4] - (R3 & R2) - (~R3 & R1)) & 0xffff;
        }

        out[outOfs    ] = (byte)R0;
        out[outOfs + 1] = (byte)(R0 >> 8);
        out[outOfs + 2] = (byte)R1;
        out[outOfs + 3] = (byte)(R1 >> 8);
        out[outOfs + 4] = (byte)R2;
        out[outOfs + 5] = (byte)(R2 >> 8);
        out[outOfs + 6] = (byte)R3;
        out[outOfs + 7] = (byte)(R3 >> 8);
    }

}
