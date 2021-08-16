/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;

/**
 * This class encapsulates a long term secret key for a Kerberos
 * principal.<p>
 *
 * A {@code KerberosKey} object includes an EncryptionKey, a
 * {@link KerberosPrincipal} as its owner, and the version number
 * of the key.<p>
 *
 * An EncryptionKey is defined in Section 4.2.9 of the Kerberos Protocol
 * Specification (<a href=http://www.ietf.org/rfc/rfc4120.txt>RFC 4120</a>) as:
 * <pre>
 *     EncryptionKey   ::= SEQUENCE {
 *             keytype         [0] Int32 -- actually encryption type --,
 *             keyvalue        [1] OCTET STRING
 *     }
 * </pre>
 * The key material of a {@code KerberosKey} is defined as the value
 * of the {@code keyValue} above.<p>
 *
 * All Kerberos JAAS login modules that obtain a principal's password and
 * generate the secret key from it should use this class.
 * Sometimes, such as when authenticating a server in
 * the absence of user-to-user authentication, the login module will store
 * an instance of this class in the private credential set of a
 * {@link javax.security.auth.Subject Subject} during the commit phase of the
 * authentication process.<p>
 *
 * A Kerberos service using a keytab to read secret keys should use
 * the {@link KeyTab} class, where latest keys can be read when needed.<p>
 *
 * It might be necessary for the application to be granted a
 * {@link javax.security.auth.PrivateCredentialPermission
 * PrivateCredentialPermission} if it needs to access the {@code KerberosKey}
 * instance from a Subject. This permission is not needed when the
 * application depends on the default JGSS Kerberos mechanism to access the
 * {@code KerberosKey}. In that case, however, the application will need an
 * appropriate
 * {@link javax.security.auth.kerberos.ServicePermission ServicePermission}.<p>
 *
 * When creating a {@code KerberosKey} using the
 * {@link #KerberosKey(KerberosPrincipal, char[], String)} constructor,
 * an implementation may accept non-IANA algorithm names (For example,
 * "ArcFourMac" for "rc4-hmac"), but the {@link #getAlgorithm} method
 * must always return the IANA algorithm name.
 *
 * @implNote Old algorithm names used before JDK 9 are supported in the
 * {@link #KerberosKey(KerberosPrincipal, char[], String)} constructor in this
 * implementation for compatibility reasons, which are "DES" (and null) for
 * "des-cbc-md5", "DESede" for "des3-cbc-sha1-kd", "ArcFourHmac" for "rc4-hmac",
 * "AES128" for "aes128-cts-hmac-sha1-96", and "AES256" for
 * "aes256-cts-hmac-sha1-96".
 *
 * @author Mayank Upadhyay
 * @since 1.4
 */
public class KerberosKey implements SecretKey {

    private static final long serialVersionUID = -4625402278148246993L;

    /**
     * The principal that this secret key belongs to.
     *
     * @serial
     */
    private KerberosPrincipal principal;

    /**
     * the version number of this secret key
     *
     * @serial
     */
    private final int versionNum;

    /**
     * {@code KeyImpl} is serialized by writing out the ASN.1 encoded bytes
     * of the encryption key.
     *
     * @serial
     */
    private KeyImpl key;

    private transient boolean destroyed = false;

    /**
     * Constructs a {@code KerberosKey} from the given bytes when the key type
     * and key version number are known. This can be used when reading the
     * secret key information from a Kerberos "keytab".
     *
     * @param principal the principal that this secret key belongs to
     * @param keyBytes the key material for the secret key
     * @param keyType the key type for the secret key as defined by the
     * Kerberos protocol specification.
     * @param versionNum the version number of this secret key
     */
    public KerberosKey(KerberosPrincipal principal,
                       byte[] keyBytes,
                       int keyType,
                       int versionNum) {
        this.principal = principal;
        this.versionNum = versionNum;
        key = new KeyImpl(keyBytes, keyType);
    }

    /**
     * Constructs a {@code KerberosKey} from a principal's password using the
     * specified algorithm name. The algorithm name (case insensitive) should
     * be provided as the encryption type string defined on the IANA
     * <a href="https://www.iana.org/assignments/kerberos-parameters/kerberos-parameters.xhtml#kerberos-parameters-1">Kerberos Encryption Type Numbers</a>
     * page. The version number of the key generated will be 0.
     *
     * @param principal the principal that this password belongs to
     * @param password the password that should be used to compute the key
     * @param algorithm the name for the algorithm that this key will be
     * used for
     * @throws IllegalArgumentException if the name of the
     * algorithm passed is unsupported.
     */
    public KerberosKey(KerberosPrincipal principal,
                       char[] password,
                       String algorithm) {

        this.principal = principal;
        this.versionNum = 0;
        // Pass principal in for salt
        key = new KeyImpl(principal, password, algorithm);
    }

    /**
     * Returns the principal that this key belongs to.
     *
     * @return the principal this key belongs to.
     * @throws IllegalStateException if the key is destroyed
     */
    public final KerberosPrincipal getPrincipal() {
        if (destroyed) {
            throw new IllegalStateException("This key is no longer valid");
        }
        return principal;
    }

    /**
     * Returns the key version number.
     *
     * @return the key version number.
     * @throws IllegalStateException if the key is destroyed
     */
    public final int getVersionNumber() {
        if (destroyed) {
            throw new IllegalStateException("This key is no longer valid");
        }
        return versionNum;
    }

    /**
     * Returns the key type for this long-term key.
     *
     * @return the key type.
     * @throws IllegalStateException if the key is destroyed
     */
    public final int getKeyType() {
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
    public final String getAlgorithm() {
        // KeyImpl already checked if destroyed
        return key.getAlgorithm();
    }

    /**
     * Returns the name of the encoding format for this secret key.
     *
     * @return the String "RAW"
     * @throws IllegalStateException if the key is destroyed
     */
    public final String getFormat() {
        // KeyImpl already checked if destroyed
        return key.getFormat();
    }

    /**
     * Returns the key material of this secret key.
     *
     * @return the key material
     * @throws IllegalStateException if the key is destroyed
     */
    public final byte[] getEncoded() {
        // KeyImpl already checked if destroyed
        return key.getEncoded();
    }

    /**
     * Destroys this key by clearing out the key material of this secret key.
     *
     * @throws DestroyFailedException if some error occurs while destorying
     * this key.
     */
    public void destroy() throws DestroyFailedException {
        if (!destroyed) {
            key.destroy();
            principal = null;
            destroyed = true;
        }
    }


    /** Determines if this key has been destroyed.*/
    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Returns an informative textual representation of this {@code KerberosKey}.
     *
     * @return an informative textual representation of this {@code KerberosKey}.
     */
    public String toString() {
        if (destroyed) {
            return "Destroyed KerberosKey";
        }
        return "Kerberos Principal " + principal +
                "Key Version " + versionNum +
                "key "  + key.toString();
    }

    /**
     * Returns a hash code for this {@code KerberosKey}.
     *
     * @return a hash code for this {@code KerberosKey}.
     * @since 1.6
     */
    public int hashCode() {
        int result = 17;
        if (isDestroyed()) {
            return result;
        }
        result = 37 * result + Arrays.hashCode(getEncoded());
        result = 37 * result + getKeyType();
        if (principal != null) {
            result = 37 * result + principal.hashCode();
        }
        return result * 37 + versionNum;
    }

    /**
     * Compares the specified object with this {@code KerberosKey} for
     * equality. Returns true if the given object is also a
     * {@code KerberosKey} and the two
     * {@code KerberosKey} instances are equivalent.
     * A destroyed {@code KerberosKey} object is only equal to itself.
     *
     * @param other the object to compare to
     * @return true if the specified object is equal to this {@code KerberosKey},
     * false otherwise.
     * @since 1.6
     */
    public boolean equals(Object other) {

        if (other == this) {
            return true;
        }

        if (! (other instanceof KerberosKey)) {
            return false;
        }

        KerberosKey otherKey = ((KerberosKey) other);
        if (isDestroyed() || otherKey.isDestroyed()) {
            return false;
        }

        if (versionNum != otherKey.getVersionNumber() ||
                getKeyType() != otherKey.getKeyType() ||
                !Arrays.equals(getEncoded(), otherKey.getEncoded())) {
            return false;
        }

        if (principal == null) {
            if (otherKey.getPrincipal() != null) {
                return false;
            }
        } else {
            if (!principal.equals(otherKey.getPrincipal())) {
                return false;
            }
        }

        return true;
    }
}
