/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * This class acts as the base class for AES KeyWrap algorithms as defined
 * in <a href=https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-38F.pdf>
 * "Recommendation for Block Cipher Modes of Operation: Methods for Key Wrapping"
 */
class KWUtil {

    static final int BLKSIZE = 16;
    static final int SEMI_BLKSIZE = BLKSIZE >> 1;
    static final int MIN_INPUTLEN = BLKSIZE + SEMI_BLKSIZE;

    /*
     * The wrapping function W as defined in section 6.1 of NIST SP 800-38F as
     * well as sec 2.2.1 of RFC 3394.
     * @param firstSemiblk the first semi block value to overwrite the input
     *         with
     * @param in input bytes
     * @param inLen length of the to-be-processed bytes
     * @param cipher the initialized cipher object used
     * @return the processed output length, i.e. same as {@code inLen}.
     */
    static final int W(byte[] icvIn, byte[] in, int inLen,
            SymmetricCipher cipher) {
        assert((inLen >= MIN_INPUTLEN) && ((inLen % SEMI_BLKSIZE) == 0)) :
                ("Invalid data length for W: " + inLen);
        assert(icvIn.length == SEMI_BLKSIZE) : "Invalid ICV buffer size";

        // overwrite the first block of in with the icv semiblock
        System.arraycopy(icvIn, 0, in, 0, SEMI_BLKSIZE);

        int n = inLen / SEMI_BLKSIZE - 1;

        byte[] buffer = new byte[BLKSIZE];
        byte[] out = in; // in-place
        for (int j = 0; j < 6; j++) {
            for (int i = 1; i <= n; i++) {
                int T = i + j*n;
                System.arraycopy(out, 0, buffer, 0, SEMI_BLKSIZE);
                System.arraycopy(out, i << 3, buffer, SEMI_BLKSIZE, 8);
                cipher.encryptBlock(buffer, 0, buffer, 0);
                for (int k = 1; T != 0; k++) {
                    byte v = (byte) T;
                    buffer[SEMI_BLKSIZE - k] ^= v;
                    T >>>= SEMI_BLKSIZE;
                }
                System.arraycopy(buffer, 0, out, 0, SEMI_BLKSIZE);
                System.arraycopy(buffer, SEMI_BLKSIZE, out, i << 3,
                        SEMI_BLKSIZE);
            }
        }
        // for W, output length is same as input length
        return inLen;
    }

    /*
     * The unwrapping function W^-1 as defined in section 6.1 of NIST SP
     * 800-38F as well as sec 2.2.2 of RFC 3394.
     * - separated out the initial value from the remaining recovered data
     * - no output buffer argument since we cannot write out the recovered
     *   data until the initial value and padding bytes are verified.
     * @param in input bytes, i.e. the to-be-processed data
     * @param inLen length of the to-be-processed bytes
     * @param ivOut buffer for holding the recovered ICV semiblock
     * @param cipher the initialized cipher object used
     * @return the recovered data length, i.e. {@code (inLen - icvOut.length)}
     */
    static final int W_INV(byte[] in, int inLen, byte[] icvOut,
            SymmetricCipher cipher) {

        assert((inLen >= MIN_INPUTLEN) && ((inLen % SEMI_BLKSIZE) == 0)) :
                ("Invalid data length for W_INV: " + inLen);
        assert(icvOut.length == SEMI_BLKSIZE) : "Invalid ICV buffer size";

        byte[] buffer = new byte[BLKSIZE];
        System.arraycopy(in, 0, buffer, 0, SEMI_BLKSIZE);
        System.arraycopy(in, SEMI_BLKSIZE, in, 0, inLen - SEMI_BLKSIZE);
        int n = (inLen - SEMI_BLKSIZE) / SEMI_BLKSIZE;

        for (int j = 5; j >= 0; j--) {
            for (int i = n; i > 0; i--) {
                int T = i + n*j;
                int idx = (i-1) << 3;
                System.arraycopy(in, idx, buffer, SEMI_BLKSIZE, SEMI_BLKSIZE);
                for (int k = 1; T != 0; k++) {
                    byte v = (byte) T;
                    buffer[SEMI_BLKSIZE - k] ^= v;
                    T >>>= SEMI_BLKSIZE;
                }
                cipher.decryptBlock(buffer, 0, buffer, 0);
                System.arraycopy(buffer, SEMI_BLKSIZE, in, idx, SEMI_BLKSIZE);
            }
        }
        System.arraycopy(buffer, 0, icvOut, 0, SEMI_BLKSIZE);
        Arrays.fill(buffer, (byte)0);
        return inLen - SEMI_BLKSIZE;
    }
}
