/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package sun.security.rsa;

import java.security.*;

/**
 * This class implements the MGF1 mask generation function defined in PKCS#1
 * v2.2 B.2.1 (https://tools.ietf.org/html/rfc8017#appendix-B.2.1). A mask
 * generation function takes an octet string of variable length and a
 * desired output length as input and outputs an octet string of the
 * desired length. MGF1 is a mask generation function based on a hash
 * function, i.e. message digest algorithm.
 *
 * @since   11
 */
public final class MGF1 {

    private final MessageDigest md;

    /**
     * Construct an instance of MGF1 based on the specified digest algorithm.
     */
    MGF1(String mdAlgo) throws NoSuchAlgorithmException {
        this.md = MessageDigest.getInstance(mdAlgo);
    }

    /**
     * Using the specified seed bytes, generate the mask, xor the mask
     * with the specified output buffer and store the result into the
     * output buffer (essentially replaced in place).
     *
     * @param seed the buffer holding the seed bytes
     * @param seedOfs the index of the seed bytes
     * @param seedLen the length of the seed bytes to be used by MGF1
     * @param maskLen the intended length of the generated mask
     * @param out the output buffer holding the mask
     * @param outOfs the index of the output buffer for the mask
     */
    void generateAndXor(byte[] seed, int seedOfs, int seedLen, int maskLen,
            byte[] out, int outOfs) throws RuntimeException {
        byte[] C = new byte[4]; // 32 bit counter
        byte[] digest = new byte[md.getDigestLength()];
        while (maskLen > 0) {
            md.update(seed, seedOfs, seedLen);
            md.update(C);
            try {
                md.digest(digest, 0, digest.length);
            } catch (DigestException e) {
                // should never happen
                throw new RuntimeException(e.toString());
            }
            for (int i = 0; (i < digest.length) && (maskLen > 0); maskLen--) {
                out[outOfs++] ^= digest[i++];
            }
            if (maskLen > 0) {
                // increment counter
                for (int i = C.length - 1; (++C[i] == 0) && (i > 0); i--) {
                    // empty
                }
            }
        }
    }

    /**
     * Returns the name of this MGF1 instance, i.e. "MGF1" followed by the
     * digest algorithm it based on.
     */
    String getName() {
        return "MGF1" + md.getAlgorithm();
    }
}
