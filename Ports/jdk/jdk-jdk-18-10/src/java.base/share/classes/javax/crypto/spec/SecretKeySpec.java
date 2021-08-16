/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.access.JavaxCryptoSpecAccess;
import jdk.internal.access.SharedSecrets;

import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Locale;
import javax.crypto.SecretKey;

/**
 * This class specifies a secret key in a provider-independent fashion.
 *
 * <p>It can be used to construct a <code>SecretKey</code> from a byte array,
 * without having to go through a (provider-based)
 * <code>SecretKeyFactory</code>.
 *
 * <p>This class is only useful for raw secret keys that can be represented as
 * a byte array and have no key parameters associated with them, e.g., DES or
 * Triple DES keys.
 *
 * @author Jan Luehe
 *
 * @see javax.crypto.SecretKey
 * @see javax.crypto.SecretKeyFactory
 * @since 1.4
 */
public class SecretKeySpec implements KeySpec, SecretKey {

    @java.io.Serial
    private static final long serialVersionUID = 6577238317307289933L;

    /**
     * The secret key.
     *
     * @serial
     */
    private byte[] key;

    /**
     * The name of the algorithm associated with this key.
     *
     * @serial
     */
    private String algorithm;

    static {
        SharedSecrets.setJavaxCryptoSpecAccess(
                new JavaxCryptoSpecAccess() {
                    @Override
                    public void clearSecretKeySpec(SecretKeySpec keySpec) {
                        keySpec.clear();
                    }
                });
    }

    /**
     * Constructs a secret key from the given byte array.
     *
     * <p>This constructor does not check if the given bytes indeed specify a
     * secret key of the specified algorithm. For example, if the algorithm is
     * DES, this constructor does not check if <code>key</code> is 8 bytes
     * long, and also does not check for weak or semi-weak keys.
     * In order for those checks to be performed, an algorithm-specific
     * <i>key specification</i> class (in this case:
     * {@link DESKeySpec DESKeySpec})
     * should be used.
     *
     * @param key the key material of the secret key. The contents of
     * the array are copied to protect against subsequent modification.
     * @param algorithm the name of the secret-key algorithm to be associated
     * with the given key material.
     * See the <a href="{@docRoot}/../specs/security/standard-names.html">
     * Java Security Standard Algorithm Names</a> document
     * for information about standard algorithm names.
     * @exception IllegalArgumentException if <code>algorithm</code>
     * is null or <code>key</code> is null or empty.
     */
    public SecretKeySpec(byte[] key, String algorithm) {
        if (key == null || algorithm == null) {
            throw new IllegalArgumentException("Missing argument");
        }
        if (key.length == 0) {
            throw new IllegalArgumentException("Empty key");
        }
        this.key = key.clone();
        this.algorithm = algorithm;
    }

    /**
     * Constructs a secret key from the given byte array, using the first
     * <code>len</code> bytes of <code>key</code>, starting at
     * <code>offset</code> inclusive.
     *
     * <p> The bytes that constitute the secret key are
     * those between <code>key[offset]</code> and
     * <code>key[offset+len-1]</code> inclusive.
     *
     * <p>This constructor does not check if the given bytes indeed specify a
     * secret key of the specified algorithm. For example, if the algorithm is
     * DES, this constructor does not check if <code>key</code> is 8 bytes
     * long, and also does not check for weak or semi-weak keys.
     * In order for those checks to be performed, an algorithm-specific key
     * specification class (in this case:
     * {@link DESKeySpec DESKeySpec})
     * must be used.
     *
     * @param key the key material of the secret key. The first
     * <code>len</code> bytes of the array beginning at
     * <code>offset</code> inclusive are copied to protect
     * against subsequent modification.
     * @param offset the offset in <code>key</code> where the key material
     * starts.
     * @param len the length of the key material.
     * @param algorithm the name of the secret-key algorithm to be associated
     * with the given key material.
     * See the <a href="{@docRoot}/../specs/security/standard-names.html">
     * Java Security Standard Algorithm Names</a> document
     * for information about standard algorithm names.
     * @exception IllegalArgumentException if <code>algorithm</code>
     * is null or <code>key</code> is null, empty, or too short,
     * i.e. {@code key.length-offset<len}.
     * @exception ArrayIndexOutOfBoundsException is thrown if
     * <code>offset</code> or <code>len</code> index bytes outside the
     * <code>key</code>.
     */
    public SecretKeySpec(byte[] key, int offset, int len, String algorithm) {
        if (key == null || algorithm == null) {
            throw new IllegalArgumentException("Missing argument");
        }
        if (key.length == 0) {
            throw new IllegalArgumentException("Empty key");
        }
        if (key.length-offset < len) {
            throw new IllegalArgumentException
                ("Invalid offset/length combination");
        }
        if (len < 0) {
            throw new ArrayIndexOutOfBoundsException("len is negative");
        }
        this.key = new byte[len];
        System.arraycopy(key, offset, this.key, 0, len);
        this.algorithm = algorithm;
    }

    /**
     * Returns the name of the algorithm associated with this secret key.
     *
     * @return the secret key algorithm.
     */
    public String getAlgorithm() {
        return this.algorithm;
    }

    /**
     * Returns the name of the encoding format for this secret key.
     *
     * @return the string "RAW".
     */
    public String getFormat() {
        return "RAW";
    }

    /**
     * Returns the key material of this secret key.
     *
     * @return the key material. Returns a new array
     * each time this method is called.
     */
    public byte[] getEncoded() {
        return this.key.clone();
    }

    /**
     * Calculates a hash code value for the object.
     * Objects that are equal will also have the same hashcode.
     */
    public int hashCode() {
        int retval = 0;
        for (int i = 1; i < this.key.length; i++) {
            retval += this.key[i] * i;
        }
        if (this.algorithm.equalsIgnoreCase("TripleDES"))
            return (retval ^= "desede".hashCode());
        else
            return (retval ^=
                    this.algorithm.toLowerCase(Locale.ENGLISH).hashCode());
    }

    /**
     * Tests for equality between the specified object and this
     * object. Two SecretKeySpec objects are considered equal if
     * they are both SecretKey instances which have the
     * same case-insensitive algorithm name and key encoding.
     *
     * @param obj the object to test for equality with this object.
     *
     * @return true if the objects are considered equal, false if
     * <code>obj</code> is null or otherwise.
     */
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (!(obj instanceof SecretKey))
            return false;

        String thatAlg = ((SecretKey)obj).getAlgorithm();
        if (!(thatAlg.equalsIgnoreCase(this.algorithm))) {
            if ((!(thatAlg.equalsIgnoreCase("DESede"))
                 || !(this.algorithm.equalsIgnoreCase("TripleDES")))
                && (!(thatAlg.equalsIgnoreCase("TripleDES"))
                    || !(this.algorithm.equalsIgnoreCase("DESede"))))
            return false;
        }

        byte[] thatKey = ((SecretKey)obj).getEncoded();
        try {
            return MessageDigest.isEqual(this.key, thatKey);
        } finally {
            if (thatKey != null) {
                Arrays.fill(thatKey, (byte)0);
            }
        }
    }

    /**
     * Clear the key bytes inside.
     */
    void clear() {
        Arrays.fill(key, (byte)0);
    }
}
