/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.provider;

import java.util.Arrays;
import java.util.Objects;

import jdk.internal.util.Preconditions;
import jdk.internal.vm.annotation.IntrinsicCandidate;
import static sun.security.provider.ByteArrayAccess.*;

/**
 * This class implements the Secure Hash Algorithm SHA-256 developed by
 * the National Institute of Standards and Technology along with the
 * National Security Agency.
 *
 * <p>It implements java.security.MessageDigestSpi, and can be used
 * through Java Cryptography Architecture (JCA), as a pluggable
 * MessageDigest implementation.
 *
 * @since       1.4.2
 * @author      Valerie Peng
 * @author      Andreas Sterbenz
 */
abstract class SHA2 extends DigestBase {

    private static final int ITERATION = 64;
    // Constants for each round
    private static final int[] ROUND_CONSTS = {
        0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
        0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
        0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
        0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
        0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
        0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
        0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
        0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
        0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
        0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
        0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
        0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };

    // buffer used by implCompress()
    private int[] W;

    // state of this object
    private int[] state;

    // initial state value. different between SHA-224 and SHA-256
    private final int[] initialHashes;

    /**
     * Creates a new SHA object.
     */
    SHA2(String name, int digestLength, int[] initialHashes) {
        super(name, digestLength, 64);
        this.initialHashes = initialHashes;
        state = new int[8];
        resetHashes();
    }

    /**
     * Resets the buffers and hash value to start a new hash.
     */
    void implReset() {
        resetHashes();
        if (W != null) {
            Arrays.fill(W, 0);
        }
    }

    private void resetHashes() {
        System.arraycopy(initialHashes, 0, state, 0, state.length);
    }

    void implDigest(byte[] out, int ofs) {
        long bitsProcessed = bytesProcessed << 3;

        int index = (int)bytesProcessed & 0x3f;
        int padLen = (index < 56) ? (56 - index) : (120 - index);
        engineUpdate(padding, 0, padLen);

        i2bBig4((int)(bitsProcessed >>> 32), buffer, 56);
        i2bBig4((int)bitsProcessed, buffer, 60);
        implCompress(buffer, 0);

        i2bBig(state, 0, out, ofs, engineGetDigestLength());
    }

    /**
     * Process the current block to update the state variable state.
     */
    void implCompress(byte[] buf, int ofs) {
        implCompressCheck(buf, ofs);
        implCompress0(buf, ofs);
    }

    private void implCompressCheck(byte[] buf, int ofs) {
        Objects.requireNonNull(buf);

        // Checks similar to those performed by the method 'b2iBig64'
        // are sufficient for the case when the method 'implCompress0' is
        // replaced with a compiler intrinsic.
        Preconditions.checkFromIndexSize(ofs, 64, buf.length, Preconditions.AIOOBE_FORMATTER);
    }

    // The method 'implCompressImpl' seems not to use its parameters.
    // The method can, however, be replaced with a compiler intrinsic
    // that operates directly on the array 'buf' (starting from
    // offset 'ofs') and not on array 'W', therefore 'buf' and 'ofs'
    // must be passed as parameter to the method.
    @IntrinsicCandidate
    private void implCompress0(byte[] buf, int ofs) {
        if (W == null) {
            W = new int[64];
        }
        b2iBig64(buf, ofs, W);
        // The first 16 ints are from the byte stream, compute the rest of
        // the W[]'s
        for (int t = 16; t < ITERATION; t++) {
            int W_t2 = W[t - 2];
            int W_t15 = W[t - 15];

            // S(x,s) is right rotation of x by s positions:
            //   S(x,s) = (x >>> s) | (x << (32 - s))
            // R(x,s) is right shift of x by s positions:
            //   R(x,s) = (x >>> s)

            // delta0(x) = S(x, 7) ^ S(x, 18) ^ R(x, 3)
            int delta0_W_t15 =
                    ((W_t15 >>>  7) | (W_t15 << 25)) ^
                    ((W_t15 >>> 18) | (W_t15 << 14)) ^
                     (W_t15 >>>  3);

            // delta1(x) = S(x, 17) ^ S(x, 19) ^ R(x, 10)
            int delta1_W_t2 =
                    ((W_t2 >>> 17) | (W_t2 << 15)) ^
                    ((W_t2 >>> 19) | (W_t2 << 13)) ^
                     (W_t2 >>> 10);

            W[t] = delta0_W_t15 + delta1_W_t2 + W[t-7] + W[t-16];
        }

        int a = state[0];
        int b = state[1];
        int c = state[2];
        int d = state[3];
        int e = state[4];
        int f = state[5];
        int g = state[6];
        int h = state[7];

        for (int i = 0; i < ITERATION; i++) {
            // S(x,s) is right rotation of x by s positions:
            //   S(x,s) = (x >>> s) | (x << (32 - s))

            // sigma0(x) = S(x,2) xor S(x,13) xor S(x,22)
            int sigma0_a =
                    ((a >>>  2) | (a << 30)) ^
                    ((a >>> 13) | (a << 19)) ^
                    ((a >>> 22) | (a << 10));

            // sigma1(x) = S(x,6) xor S(x,11) xor S(x,25)
            int sigma1_e =
                    ((e >>>  6) | (e << 26)) ^
                    ((e >>> 11) | (e << 21)) ^
                    ((e >>> 25) | (e <<  7));

            // ch(x,y,z) = (x and y) xor ((complement x) and z)
            int ch_efg = (e & f) ^ ((~e) & g);

            // maj(x,y,z) = (x and y) xor (x and z) xor (y and z)
            int maj_abc = (a & b) ^ (a & c) ^ (b & c);

            int T1 = h + sigma1_e + ch_efg + ROUND_CONSTS[i] + W[i];
            int T2 = sigma0_a + maj_abc;
            h = g;
            g = f;
            f = e;
            e = d + T1;
            d = c;
            c = b;
            b = a;
            a = T1 + T2;
        }

        state[0] += a;
        state[1] += b;
        state[2] += c;
        state[3] += d;
        state[4] += e;
        state[5] += f;
        state[6] += g;
        state[7] += h;
    }

    public Object clone() throws CloneNotSupportedException {
        SHA2 copy = (SHA2) super.clone();
        copy.state = copy.state.clone();
        copy.W = null;
        return copy;
    }

    /**
     * SHA-224 implementation class.
     */
    public static final class SHA224 extends SHA2 {
        private static final int[] INITIAL_HASHES = {
            0xc1059ed8, 0x367cd507, 0x3070dd17, 0xf70e5939,
            0xffc00b31, 0x68581511, 0x64f98fa7, 0xbefa4fa4
        };

        public SHA224() {
            super("SHA-224", 28, INITIAL_HASHES);
        }
    }

    /**
     * SHA-256 implementation class.
     */
    public static final class SHA256 extends SHA2 {
        private static final int[] INITIAL_HASHES = {
            0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
            0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19
        };

        public SHA256() {
            super("SHA-256", 32, INITIAL_HASHES);
        }
    }
}
