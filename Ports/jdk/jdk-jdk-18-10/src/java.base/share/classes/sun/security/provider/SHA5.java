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
 * This class implements the Secure Hash Algorithm SHA-384 and SHA-512
 * developed by the National Institute of Standards and Technology along
 * with the National Security Agency.
 *
 * The two algorithms are almost identical. This file contains a base
 * class SHA5 and two nested static subclasses as the classes to be used
 * by the JCA framework.
 *
 * <p>It implements java.security.MessageDigestSpi, and can be used
 * through Java Cryptography Architecture (JCA), as a pluggable
 * MessageDigest implementation.
 *
 * @since       1.4.2
 * @author      Valerie Peng
 * @author      Andreas Sterbenz
 */
abstract class SHA5 extends DigestBase {

    private static final int ITERATION = 80;
    // Constants for each round/iteration
    private static final long[] ROUND_CONSTS = {
        0x428A2F98D728AE22L, 0x7137449123EF65CDL, 0xB5C0FBCFEC4D3B2FL,
        0xE9B5DBA58189DBBCL, 0x3956C25BF348B538L, 0x59F111F1B605D019L,
        0x923F82A4AF194F9BL, 0xAB1C5ED5DA6D8118L, 0xD807AA98A3030242L,
        0x12835B0145706FBEL, 0x243185BE4EE4B28CL, 0x550C7DC3D5FFB4E2L,
        0x72BE5D74F27B896FL, 0x80DEB1FE3B1696B1L, 0x9BDC06A725C71235L,
        0xC19BF174CF692694L, 0xE49B69C19EF14AD2L, 0xEFBE4786384F25E3L,
        0x0FC19DC68B8CD5B5L, 0x240CA1CC77AC9C65L, 0x2DE92C6F592B0275L,
        0x4A7484AA6EA6E483L, 0x5CB0A9DCBD41FBD4L, 0x76F988DA831153B5L,
        0x983E5152EE66DFABL, 0xA831C66D2DB43210L, 0xB00327C898FB213FL,
        0xBF597FC7BEEF0EE4L, 0xC6E00BF33DA88FC2L, 0xD5A79147930AA725L,
        0x06CA6351E003826FL, 0x142929670A0E6E70L, 0x27B70A8546D22FFCL,
        0x2E1B21385C26C926L, 0x4D2C6DFC5AC42AEDL, 0x53380D139D95B3DFL,
        0x650A73548BAF63DEL, 0x766A0ABB3C77B2A8L, 0x81C2C92E47EDAEE6L,
        0x92722C851482353BL, 0xA2BFE8A14CF10364L, 0xA81A664BBC423001L,
        0xC24B8B70D0F89791L, 0xC76C51A30654BE30L, 0xD192E819D6EF5218L,
        0xD69906245565A910L, 0xF40E35855771202AL, 0x106AA07032BBD1B8L,
        0x19A4C116B8D2D0C8L, 0x1E376C085141AB53L, 0x2748774CDF8EEB99L,
        0x34B0BCB5E19B48A8L, 0x391C0CB3C5C95A63L, 0x4ED8AA4AE3418ACBL,
        0x5B9CCA4F7763E373L, 0x682E6FF3D6B2B8A3L, 0x748F82EE5DEFB2FCL,
        0x78A5636F43172F60L, 0x84C87814A1F0AB72L, 0x8CC702081A6439ECL,
        0x90BEFFFA23631E28L, 0xA4506CEBDE82BDE9L, 0xBEF9A3F7B2C67915L,
        0xC67178F2E372532BL, 0xCA273ECEEA26619CL, 0xD186B8C721C0C207L,
        0xEADA7DD6CDE0EB1EL, 0xF57D4F7FEE6ED178L, 0x06F067AA72176FBAL,
        0x0A637DC5A2C898A6L, 0x113F9804BEF90DAEL, 0x1B710B35131C471BL,
        0x28DB77F523047D84L, 0x32CAAB7B40C72493L, 0x3C9EBE0A15C9BEBCL,
        0x431D67C49C100D4CL, 0x4CC5D4BECB3E42B6L, 0x597F299CFC657E2AL,
        0x5FCB6FAB3AD6FAECL, 0x6C44198C4A475817L
    };

    // buffer used by implCompress()
    private long[] W;

    // state of this object
    private long[] state;

    // initial state value. different between SHA-384 and SHA-512
    private final long[] initialHashes;

    /**
     * Creates a new SHA object.
     */
    SHA5(String name, int digestLength, long[] initialHashes) {
        super(name, digestLength, 128);
        this.initialHashes = initialHashes;
        state = new long[8];
        resetHashes();
    }

    final void implReset() {
        resetHashes();
        if (W != null) {
            Arrays.fill(W, 0L);
        }
    }

    private void resetHashes() {
        System.arraycopy(initialHashes, 0, state, 0, state.length);
    }

    final void implDigest(byte[] out, int ofs) {
        long bitsProcessed = bytesProcessed << 3;

        int index = (int)bytesProcessed & 0x7f;
        int padLen = (index < 112) ? (112 - index) : (240 - index);
        engineUpdate(padding, 0, padLen + 8);

        i2bBig4((int)(bitsProcessed >>> 32), buffer, 120);
        i2bBig4((int)bitsProcessed, buffer, 124);
        implCompress(buffer, 0);

        int len = engineGetDigestLength();
        if (len == 28) {
            // Special case for SHA-512/224
            l2bBig(state, 0, out, ofs, 24);
            i2bBig4((int)(state[3] >> 32), out, ofs + 24);
        } else {
            l2bBig(state, 0, out, ofs, len);
        }
    }

    /**
     * logical function ch(x,y,z) as defined in spec:
     * @return (x and y) xor ((complement x) and z)
     * @param x long
     * @param y long
     * @param z long
     */
    private static long lf_ch(long x, long y, long z) {
        return (x & y) ^ ((~x) & z);
    }

    /**
     * logical function maj(x,y,z) as defined in spec:
     * @return (x and y) xor (x and z) xor (y and z)
     * @param x long
     * @param y long
     * @param z long
     */
    private static long lf_maj(long x, long y, long z) {
        return (x & y) ^ (x & z) ^ (y & z);
    }

    /**
     * logical function R(x,s) - right shift
     * @return x right shift for s times
     * @param x long
     * @param s int
     */
    private static long lf_R(long x, int s) {
        return (x >>> s);
    }

    /**
     * logical function S(x,s) - right rotation
     * @return x circular right shift for s times
     * @param x long
     * @param s int
     */
    private static long lf_S(long x, int s) {
        return (x >>> s) | (x << (64 - s));
    }

    /**
     * logical function sigma0(x) - xor of results of right rotations
     * @return S(x,28) xor S(x,34) xor S(x,39)
     * @param x long
     */
    private static long lf_sigma0(long x) {
        return lf_S(x, 28) ^ lf_S(x, 34) ^ lf_S(x, 39);
    }

    /**
     * logical function sigma1(x) - xor of results of right rotations
     * @return S(x,14) xor S(x,18) xor S(x,41)
     * @param x long
     */
    private static long lf_sigma1(long x) {
        return lf_S(x, 14) ^ lf_S(x, 18) ^ lf_S(x, 41);
    }

    /**
     * logical function delta0(x) - xor of results of right shifts/rotations
     * @return long
     * @param x long
     */
    private static long lf_delta0(long x) {
        return lf_S(x, 1) ^ lf_S(x, 8) ^ lf_R(x, 7);
    }

    /**
     * logical function delta1(x) - xor of results of right shifts/rotations
     * @return long
     * @param x long
     */
    private static long lf_delta1(long x) {
        return lf_S(x, 19) ^ lf_S(x, 61) ^ lf_R(x, 6);
    }

    /**
     * Compute the hash for the current block.
     *
     * This is in the same vein as Peter Gutmann's algorithm listed in
     * the back of Applied Cryptography, Compact implementation of
     * "old" NIST Secure Hash Algorithm.
     */
    final void implCompress(byte[] buf, int ofs) {
        implCompressCheck(buf, ofs);
        implCompress0(buf, ofs);
    }

    private void implCompressCheck(byte[] buf, int ofs) {
        Objects.requireNonNull(buf);

        // Checks similar to those performed by the method 'b2lBig128'
        // are sufficient for the case when the method 'implCompress0' is
        // replaced with a compiler intrinsic.
        Preconditions.checkFromIndexSize(ofs, 128, buf.length, Preconditions.AIOOBE_FORMATTER);
    }

    // The method 'implCompressImpl' seems not to use its parameters.
    // The method can, however, be replaced with a compiler intrinsic
    // that operates directly on the array 'buf' (starting from
    // offset 'ofs') and not on array 'W', therefore 'buf' and 'ofs'
    // must be passed as parameter to the method.
    @IntrinsicCandidate
    private final void implCompress0(byte[] buf, int ofs) {
        if (W == null) {
            W = new long[80];
        }
        b2lBig128(buf, ofs, W);
        // The first 16 longs are from the byte stream, compute the rest of
        // the W[]'s
        for (int t = 16; t < ITERATION; t++) {
            W[t] = lf_delta1(W[t-2]) + W[t-7] + lf_delta0(W[t-15])
                   + W[t-16];
        }

        long a = state[0];
        long b = state[1];
        long c = state[2];
        long d = state[3];
        long e = state[4];
        long f = state[5];
        long g = state[6];
        long h = state[7];

        for (int i = 0; i < ITERATION; i++) {
            long T1 = h + lf_sigma1(e) + lf_ch(e,f,g) + ROUND_CONSTS[i] + W[i];
            long T2 = lf_sigma0(a) + lf_maj(a,b,c);
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
        SHA5 copy = (SHA5) super.clone();
        copy.state = copy.state.clone();
        copy.W = null;
        return copy;
    }

    /**
     * SHA-512 implementation class.
     */
    public static final class SHA512 extends SHA5 {

        private static final long[] INITIAL_HASHES = {
            0x6a09e667f3bcc908L, 0xbb67ae8584caa73bL,
            0x3c6ef372fe94f82bL, 0xa54ff53a5f1d36f1L,
            0x510e527fade682d1L, 0x9b05688c2b3e6c1fL,
            0x1f83d9abfb41bd6bL, 0x5be0cd19137e2179L
        };

        public SHA512() {
            super("SHA-512", 64, INITIAL_HASHES);
        }
    }

    /**
     * SHA-384 implementation class.
     */
    public static final class SHA384 extends SHA5 {

        private static final long[] INITIAL_HASHES = {
            0xcbbb9d5dc1059ed8L, 0x629a292a367cd507L,
            0x9159015a3070dd17L, 0x152fecd8f70e5939L,
            0x67332667ffc00b31L, 0x8eb44a8768581511L,
            0xdb0c2e0d64f98fa7L, 0x47b5481dbefa4fa4L
        };

        public SHA384() {
            super("SHA-384", 48, INITIAL_HASHES);
        }
    }
    public static final class SHA512_224 extends SHA5 {

        private static final long[] INITIAL_HASHES = {
                0x8C3D37C819544DA2L, 0x73E1996689DCD4D6L,
                0x1DFAB7AE32FF9C82L, 0x679DD514582F9FCFL,
                0x0F6D2B697BD44DA8L, 0x77E36F7304C48942L,
                0x3F9D85A86A1D36C8L, 0x1112E6AD91D692A1L
        };

        public SHA512_224() {
            super("SHA-512/224", 28, INITIAL_HASHES);
        }
    }

    public static final class SHA512_256 extends SHA5 {

        private static final long[] INITIAL_HASHES = {
                0x22312194FC2BF72CL, 0x9F555FA3C84C64C2L,
                0x2393B86B6F53B151L, 0x963877195940EABDL,
                0x96283EE2A88EFFE3L, 0xBE5E1E2553863992L,
                0x2B0199FC2C85B8AAL, 0x0EB72DDC81C52CA2L
        };

        public SHA512_256() {
            super("SHA-512/256", 32, INITIAL_HASHES);
        }
    }
}
