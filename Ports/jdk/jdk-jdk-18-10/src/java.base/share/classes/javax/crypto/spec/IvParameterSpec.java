/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;

/**
 * This class specifies an <i>initialization vector</i> (IV).
 * Examples which use IVs are ciphers in feedback mode,
 * e.g., DES in CBC mode and RSA ciphers with OAEP encoding
 * operation.
 *
 * @author Jan Luehe
 *
 * @since 1.4
 */
public class IvParameterSpec implements AlgorithmParameterSpec {

    private byte[] iv;

    /**
     * Creates an IvParameterSpec object using the bytes in <code>iv</code>
     * as the IV.
     *
     * @param iv the buffer with the IV. The contents of the
     * buffer are copied to protect against subsequent modification.
     * @throws NullPointerException if <code>iv</code> is <code>null</code>
     */
    public IvParameterSpec(byte[] iv) {
        this(iv, 0, iv.length);
    }

    /**
     * Creates an IvParameterSpec object using the first <code>len</code>
     * bytes in <code>iv</code>, beginning at <code>offset</code>
     * inclusive, as the IV.
     *
     * <p> The bytes that constitute the IV are those between
     * <code>iv[offset]</code> and <code>iv[offset+len-1]</code> inclusive.
     *
     * @param iv the buffer with the IV. The first <code>len</code>
     * bytes of the buffer beginning at <code>offset</code> inclusive
     * are copied to protect against subsequent modification.
     * @param offset the offset in <code>iv</code> where the IV
     * starts.
     * @param len the number of IV bytes.
     * @throws IllegalArgumentException if <code>iv</code> is <code>null</code>
     * or {@code (iv.length - offset < len)}
     * @throws ArrayIndexOutOfBoundsException is thrown if <code>offset</code>
     * or <code>len</code> index bytes outside the <code>iv</code>.
     */
    public IvParameterSpec(byte[] iv, int offset, int len) {
        if (iv == null) {
            throw new IllegalArgumentException("IV missing");
        }
        if (iv.length - offset < len) {
            throw new IllegalArgumentException
                ("IV buffer too short for given offset/length combination");
        }
        if (len < 0) {
            throw new ArrayIndexOutOfBoundsException("len is negative");
        }
        this.iv = new byte[len];
        System.arraycopy(iv, offset, this.iv, 0, len);
    }

    /**
     * Returns the initialization vector (IV).
     *
     * @return the initialization vector (IV). Returns a new array
     * each time this method is called.
     */
    public byte[] getIV() {
        return this.iv.clone();
    }
}
