/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

package javax.security.auth.kerberos;

import java.util.Arrays;
import java.util.Objects;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;

/**
 * This class encapsulates an EncryptionKey used in Kerberos.<p>
 *
 * An EncryptionKey is defined in Section 4.2.9 of the Kerberos Protocol
 * Specification (<a href=http://www.ietf.org/rfc/rfc4120.txt>RFC 4120</a>) as:
 * <pre>
 *     EncryptionKey   ::= SEQUENCE {
 *             keytype         [0] Int32 -- actually encryption type --,
 *             keyvalue        [1] OCTET STRING
 *     }
 * </pre>
 * The key material of an {@code EncryptionKey} is defined as the value
 * of the {@code keyValue} above.
 *
 * @since 9
 */
public final class EncryptionKey implements SecretKey {

    private static final long serialVersionUID = 9L;

   /**
    * {@code KeyImpl} is serialized by writing out the ASN.1 encoded bytes
    * of the encryption key.
    *
    * @serial
    */
    private final KeyImpl key;

    private transient boolean destroyed = false;

    /**
     * Constructs an {@code EncryptionKey} from the given bytes and
     * the key type.
     * <p>
     * The contents of the byte array are copied; subsequent modification of
     * the byte array does not affect the newly created key.
     *
     * @param keyBytes the key material for the key
     * @param keyType the key type for the key as defined by the
     *                Kerberos protocol specification.
     * @throws NullPointerException if keyBytes is null
     */
    public EncryptionKey(byte[] keyBytes, int keyType) {
        key = new KeyImpl(Objects.requireNonNull(keyBytes), keyType);
    }

    /**
     * Returns the key type for this key.
     *
     * @return the key type.
     * @throws IllegalStateException if the key is destroyed
     */
    public int getKeyType() {
        // KeyImpl already checked if destroyed
        return key.getKeyType();
    }

    /*
     * Methods from java.security.Key
     */

    /**
     * Returns the standard algorithm name for this key. The algorithm names
     * are the encryption type string defined on the IANA
     * <a href="https://www.iana.org/assignments/kerberos-parameters/kerberos-parameters.xhtml#kerberos-parameters-1">Kerberos Encryption Type Numbers</a>
     * page.
     * <p>
     * This method can return the following value not defined on the IANA page:
     * <ol>
     *     <li>none: for etype equal to 0</li>
     *     <li>unknown: for etype greater than 0 but unsupported by
     *         the implementation</li>
     *     <li>private: for etype smaller than 0</li>
     * </ol>
     *
     * @return the name of the algorithm associated with this key.
     * @throws IllegalStateException if the key is destroyed
     */
    @Override
    public String getAlgorithm() {
        // KeyImpl already checked if destroyed
        return key.getAlgorithm();
    }

    /**
     * Returns the name of the encoding format for this key.
     *
     * @return the String "RAW"
     * @throws IllegalStateException if the key is destroyed
     */
    @Override
    public String getFormat() {
        // KeyImpl already checked if destroyed
        return key.getFormat();
    }

    /**
     * Returns the key material of this key.
     *
     * @return a newly allocated byte array that contains the key material
     * @throws IllegalStateException if the key is destroyed
     */
    @Override
    public byte[] getEncoded() {
        // KeyImpl already checked if destroyed
        return key.getEncoded();
    }

    /**
     * Destroys this key by clearing out the key material of this key.
     *
     * @throws DestroyFailedException if some error occurs while destorying
     * this key.
     */
    @Override
    public void destroy() throws DestroyFailedException {
        if (!destroyed) {
            key.destroy();
            destroyed = true;
        }
    }


    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Returns an informative textual representation of this {@code EncryptionKey}.
     *
     * @return an informative textual representation of this {@code EncryptionKey}.
     */
    @Override
    public String toString() {
        if (destroyed) {
            return "Destroyed EncryptionKey";
        }
        return "key "  + key.toString();
    }

    /**
     * Returns a hash code for this {@code EncryptionKey}.
     *
     * @return a hash code for this {@code EncryptionKey}.
     */
    @Override
    public int hashCode() {
        int result = 17;
        if (isDestroyed()) {
            return result;
        }
        result = 37 * result + Arrays.hashCode(getEncoded());
        return 37 * result + getKeyType();
    }

    /**
     * Compares the specified object with this key for equality.
     * Returns true if the given object is also an
     * {@code EncryptionKey} and the two
     * {@code EncryptionKey} instances are equivalent. More formally two
     * {@code EncryptionKey} instances are equal if they have equal key types
     * and key material.
     * A destroyed {@code EncryptionKey} object is only equal to itself.
     *
     * @param other the object to compare to
     * @return true if the specified object is equal to this
     * {@code EncryptionKey}, false otherwise.
     */
    @Override
    public boolean equals(Object other) {

        if (other == this)
            return true;

        if (! (other instanceof EncryptionKey)) {
            return false;
        }

        EncryptionKey otherKey = ((EncryptionKey) other);
        if (isDestroyed() || otherKey.isDestroyed()) {
            return false;
        }

        return getKeyType() == otherKey.getKeyType()
                && Arrays.equals(getEncoded(), otherKey.getEncoded());
    }
}
