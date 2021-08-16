/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

import static sun.security.provider.ByteArrayAccess.*;

import jdk.internal.util.Preconditions;
import jdk.internal.vm.annotation.IntrinsicCandidate;

/**
 * The MD5 class is used to compute an MD5 message digest over a given
 * buffer of bytes. It is an implementation of the RSA Data Security Inc
 * MD5 algorithim as described in internet RFC 1321.
 *
 * @author      Chuck McManis
 * @author      Benjamin Renaud
 * @author      Andreas Sterbenz
 */
public final class MD5 extends DigestBase {

    // state of this object
    private int[] state;

    // rotation constants
    private static final int S11 = 7;
    private static final int S12 = 12;
    private static final int S13 = 17;
    private static final int S14 = 22;
    private static final int S21 = 5;
    private static final int S22 = 9;
    private static final int S23 = 14;
    private static final int S24 = 20;
    private static final int S31 = 4;
    private static final int S32 = 11;
    private static final int S33 = 16;
    private static final int S34 = 23;
    private static final int S41 = 6;
    private static final int S42 = 10;
    private static final int S43 = 15;
    private static final int S44 = 21;

    // Standard constructor, creates a new MD5 instance.
    public MD5() {
        super("MD5", 16, 64);
        state = new int[4];
        implReset();
    }

    // clone this object
    public Object clone() throws CloneNotSupportedException {
        MD5 copy = (MD5) super.clone();
        copy.state = copy.state.clone();
        return copy;
    }

    /**
     * Reset the state of this object.
     */
    void implReset() {
        // Load magic initialization constants.
        state[0] = 0x67452301;
        state[1] = 0xefcdab89;
        state[2] = 0x98badcfe;
        state[3] = 0x10325476;
    }

    /**
     * Perform the final computations, any buffered bytes are added
     * to the digest, the count is added to the digest, and the resulting
     * digest is stored.
     */
    void implDigest(byte[] out, int ofs) {
        long bitsProcessed = bytesProcessed << 3;

        int index = (int)bytesProcessed & 0x3f;
        int padLen = (index < 56) ? (56 - index) : (120 - index);
        engineUpdate(padding, 0, padLen);

        i2bLittle4((int)bitsProcessed, buffer, 56);
        i2bLittle4((int)(bitsProcessed >>> 32), buffer, 60);
        implCompress(buffer, 0);

        i2bLittle(state, 0, out, ofs, 16);
    }

    /* **********************************************************
     * The MD5 Functions. The results of this
     * implementation were checked against the RSADSI version.
     * **********************************************************
     */

    private static int FF(int a, int b, int c, int d, int x, int s, int ac) {
        a += ((b & c) | ((~b) & d)) + x + ac;
        return ((a << s) | (a >>> (32 - s))) + b;
    }

    private static int GG(int a, int b, int c, int d, int x, int s, int ac) {
        a += ((b & d) | (c & (~d))) + x + ac;
        return ((a << s) | (a >>> (32 - s))) + b;
    }

    private static int HH(int a, int b, int c, int d, int x, int s, int ac) {
        a += ((b ^ c) ^ d) + x + ac;
        return ((a << s) | (a >>> (32 - s))) + b;
    }

    private static int II(int a, int b, int c, int d, int x, int s, int ac) {
        a += (c ^ (b | (~d))) + x + ac;
        return ((a << s) | (a >>> (32 - s))) + b;
    }

    /**
     * This is where the functions come together as the generic MD5
     * transformation operation. It consumes sixteen
     * bytes from the buffer, beginning at the specified offset.
     */
    void implCompress(byte[] buf, int ofs) {
        implCompressCheck(buf, ofs);
        implCompress0(buf, ofs);
    }

    private void implCompressCheck(byte[] buf, int ofs) {
        Objects.requireNonNull(buf);

        // These checks are sufficient for the case when the method
        // 'implCompressImpl' is replaced with a compiler
        // intrinsic.
        Preconditions.checkFromIndexSize(ofs, 64, buf.length, Preconditions.AIOOBE_FORMATTER);
    }

    // The method 'implCompress0 seems not to use its parameters.
    // The method can, however, be replaced with a compiler intrinsic
    // that operates directly on the array 'buf' (starting from
    // offset 'ofs') and not on array 'x', therefore 'buf' and 'ofs'
    // must be passed as parameter to the method.
    @IntrinsicCandidate
    void implCompress0(byte[] buf, int ofs) {
        int a = state[0];
        int b = state[1];
        int c = state[2];
        int d = state[3];

        int x0 = (int) LE.INT_ARRAY.get(buf, ofs);
        int x1 = (int) LE.INT_ARRAY.get(buf, ofs + 4);
        int x2 = (int) LE.INT_ARRAY.get(buf, ofs + 8);
        int x3 = (int) LE.INT_ARRAY.get(buf, ofs + 12);
        int x4 = (int) LE.INT_ARRAY.get(buf, ofs + 16);
        int x5 = (int) LE.INT_ARRAY.get(buf, ofs + 20);
        int x6 = (int) LE.INT_ARRAY.get(buf, ofs + 24);
        int x7 = (int) LE.INT_ARRAY.get(buf, ofs + 28);
        int x8 = (int) LE.INT_ARRAY.get(buf, ofs + 32);
        int x9 = (int) LE.INT_ARRAY.get(buf, ofs + 36);
        int x10 = (int) LE.INT_ARRAY.get(buf, ofs + 40);
        int x11 = (int) LE.INT_ARRAY.get(buf, ofs + 44);
        int x12 = (int) LE.INT_ARRAY.get(buf, ofs + 48);
        int x13 = (int) LE.INT_ARRAY.get(buf, ofs + 52);
        int x14 = (int) LE.INT_ARRAY.get(buf, ofs + 56);
        int x15 = (int) LE.INT_ARRAY.get(buf, ofs + 60);

        /* Round 1 */
        a = FF ( a, b, c, d, x0,  S11, 0xd76aa478); /* 1 */
        d = FF ( d, a, b, c, x1,  S12, 0xe8c7b756); /* 2 */
        c = FF ( c, d, a, b, x2,  S13, 0x242070db); /* 3 */
        b = FF ( b, c, d, a, x3,  S14, 0xc1bdceee); /* 4 */
        a = FF ( a, b, c, d, x4,  S11, 0xf57c0faf); /* 5 */
        d = FF ( d, a, b, c, x5,  S12, 0x4787c62a); /* 6 */
        c = FF ( c, d, a, b, x6,  S13, 0xa8304613); /* 7 */
        b = FF ( b, c, d, a, x7,  S14, 0xfd469501); /* 8 */
        a = FF ( a, b, c, d, x8,  S11, 0x698098d8); /* 9 */
        d = FF ( d, a, b, c, x9,  S12, 0x8b44f7af); /* 10 */
        c = FF ( c, d, a, b, x10, S13, 0xffff5bb1); /* 11 */
        b = FF ( b, c, d, a, x11, S14, 0x895cd7be); /* 12 */
        a = FF ( a, b, c, d, x12, S11, 0x6b901122); /* 13 */
        d = FF ( d, a, b, c, x13, S12, 0xfd987193); /* 14 */
        c = FF ( c, d, a, b, x14, S13, 0xa679438e); /* 15 */
        b = FF ( b, c, d, a, x15, S14, 0x49b40821); /* 16 */

        /* Round 2 */
        a = GG ( a, b, c, d, x1,  S21, 0xf61e2562); /* 17 */
        d = GG ( d, a, b, c, x6,  S22, 0xc040b340); /* 18 */
        c = GG ( c, d, a, b, x11, S23, 0x265e5a51); /* 19 */
        b = GG ( b, c, d, a, x0,  S24, 0xe9b6c7aa); /* 20 */
        a = GG ( a, b, c, d, x5,  S21, 0xd62f105d); /* 21 */
        d = GG ( d, a, b, c, x10, S22,  0x2441453); /* 22 */
        c = GG ( c, d, a, b, x15, S23, 0xd8a1e681); /* 23 */
        b = GG ( b, c, d, a, x4,  S24, 0xe7d3fbc8); /* 24 */
        a = GG ( a, b, c, d, x9,  S21, 0x21e1cde6); /* 25 */
        d = GG ( d, a, b, c, x14, S22, 0xc33707d6); /* 26 */
        c = GG ( c, d, a, b, x3,  S23, 0xf4d50d87); /* 27 */
        b = GG ( b, c, d, a, x8,  S24, 0x455a14ed); /* 28 */
        a = GG ( a, b, c, d, x13, S21, 0xa9e3e905); /* 29 */
        d = GG ( d, a, b, c, x2,  S22, 0xfcefa3f8); /* 30 */
        c = GG ( c, d, a, b, x7,  S23, 0x676f02d9); /* 31 */
        b = GG ( b, c, d, a, x12, S24, 0x8d2a4c8a); /* 32 */

        /* Round 3 */
        a = HH ( a, b, c, d, x5,  S31, 0xfffa3942); /* 33 */
        d = HH ( d, a, b, c, x8,  S32, 0x8771f681); /* 34 */
        c = HH ( c, d, a, b, x11, S33, 0x6d9d6122); /* 35 */
        b = HH ( b, c, d, a, x14, S34, 0xfde5380c); /* 36 */
        a = HH ( a, b, c, d, x1,  S31, 0xa4beea44); /* 37 */
        d = HH ( d, a, b, c, x4,  S32, 0x4bdecfa9); /* 38 */
        c = HH ( c, d, a, b, x7,  S33, 0xf6bb4b60); /* 39 */
        b = HH ( b, c, d, a, x10, S34, 0xbebfbc70); /* 40 */
        a = HH ( a, b, c, d, x13, S31, 0x289b7ec6); /* 41 */
        d = HH ( d, a, b, c, x0,  S32, 0xeaa127fa); /* 42 */
        c = HH ( c, d, a, b, x3,  S33, 0xd4ef3085); /* 43 */
        b = HH ( b, c, d, a, x6,  S34,  0x4881d05); /* 44 */
        a = HH ( a, b, c, d, x9,  S31, 0xd9d4d039); /* 45 */
        d = HH ( d, a, b, c, x12, S32, 0xe6db99e5); /* 46 */
        c = HH ( c, d, a, b, x15, S33, 0x1fa27cf8); /* 47 */
        b = HH ( b, c, d, a, x2,  S34, 0xc4ac5665); /* 48 */

        /* Round 4 */
        a = II ( a, b, c, d, x0,  S41, 0xf4292244); /* 49 */
        d = II ( d, a, b, c, x7,  S42, 0x432aff97); /* 50 */
        c = II ( c, d, a, b, x14, S43, 0xab9423a7); /* 51 */
        b = II ( b, c, d, a, x5,  S44, 0xfc93a039); /* 52 */
        a = II ( a, b, c, d, x12, S41, 0x655b59c3); /* 53 */
        d = II ( d, a, b, c, x3,  S42, 0x8f0ccc92); /* 54 */
        c = II ( c, d, a, b, x10, S43, 0xffeff47d); /* 55 */
        b = II ( b, c, d, a, x1,  S44, 0x85845dd1); /* 56 */
        a = II ( a, b, c, d, x8,  S41, 0x6fa87e4f); /* 57 */
        d = II ( d, a, b, c, x15, S42, 0xfe2ce6e0); /* 58 */
        c = II ( c, d, a, b, x6,  S43, 0xa3014314); /* 59 */
        b = II ( b, c, d, a, x13, S44, 0x4e0811a1); /* 60 */
        a = II ( a, b, c, d, x4,  S41, 0xf7537e82); /* 61 */
        d = II ( d, a, b, c, x11, S42, 0xbd3af235); /* 62 */
        c = II ( c, d, a, b, x2,  S43, 0x2ad7d2bb); /* 63 */
        b = II ( b, c, d, a, x9,  S44, 0xeb86d391); /* 64 */

        state[0] += a;
        state[1] += b;
        state[2] += c;
        state[3] += d;
    }

}
