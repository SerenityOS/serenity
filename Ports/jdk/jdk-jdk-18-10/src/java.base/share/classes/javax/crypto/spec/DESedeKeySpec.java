/*
 * Copyright (c) 1997, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.security.InvalidKeyException;

/**
 * This class specifies a DES-EDE ("triple-DES") key.
 *
 * @author Jan Luehe
 *
 * @since 1.4
 */
public class DESedeKeySpec implements java.security.spec.KeySpec {

    /**
     * The constant which defines the length of a DESede key in bytes.
     */
    public static final int DES_EDE_KEY_LEN = 24;

    private byte[] key;

    /**
     * Creates a DESedeKeySpec object using the first 24 bytes in
     * <code>key</code> as the key material for the DES-EDE key.
     *
     * <p> The bytes that constitute the DES-EDE key are those between
     * <code>key[0]</code> and <code>key[23]</code> inclusive
     *
     * @param key the buffer with the DES-EDE key material. The first
     * 24 bytes of the buffer are copied to protect against subsequent
     * modification.
     *
     * @exception NullPointerException if <code>key</code> is null.
     * @exception InvalidKeyException if the given key material is shorter
     * than 24 bytes.
     */
    public DESedeKeySpec(byte[] key) throws InvalidKeyException {
        this(key, 0);
    }

    /**
     * Creates a DESedeKeySpec object using the first 24 bytes in
     * <code>key</code>, beginning at <code>offset</code> inclusive,
     * as the key material for the DES-EDE key.
     *
     * <p> The bytes that constitute the DES-EDE key are those between
     * <code>key[offset]</code> and <code>key[offset+23]</code> inclusive.
     *
     * @param key the buffer with the DES-EDE key material. The first
     * 24 bytes of the buffer beginning at <code>offset</code> inclusive
     * are copied to protect against subsequent modification.
     * @param offset the offset in <code>key</code>, where the DES-EDE key
     * material starts.
     *
     * @exception NullPointerException if <code>key</code> is null.
     * @exception InvalidKeyException if the given key material, starting at
     * <code>offset</code> inclusive, is shorter than 24 bytes
     */
    public DESedeKeySpec(byte[] key, int offset) throws InvalidKeyException {
        if (key.length - offset < 24) {
            throw new InvalidKeyException("Wrong key size");
        }
        this.key = new byte[24];
        System.arraycopy(key, offset, this.key, 0, 24);
    }

    /**
     * Returns the DES-EDE key.
     *
     * @return the DES-EDE key. Returns a new array
     * each time this method is called.
     */
    public byte[] getKey() {
        return this.key.clone();
    }

    /**
     * Checks if the given DES-EDE key, starting at <code>offset</code>
     * inclusive, is parity-adjusted.
     *
     * @param key    a byte array which holds the key value
     * @param offset the offset into the byte array
     * @return true if the given DES-EDE key is parity-adjusted, false
     * otherwise
     *
     * @exception NullPointerException if <code>key</code> is null.
     * @exception InvalidKeyException if the given key material, starting at
     * <code>offset</code> inclusive, is shorter than 24 bytes
     */
    public static boolean isParityAdjusted(byte[] key, int offset)
        throws InvalidKeyException {
            if (key.length - offset < 24) {
                throw new InvalidKeyException("Wrong key size");
            }
            if (DESKeySpec.isParityAdjusted(key, offset) == false
                || DESKeySpec.isParityAdjusted(key, offset + 8) == false
                || DESKeySpec.isParityAdjusted(key, offset + 16) == false) {
                return false;
            }
            return true;
    }
}
