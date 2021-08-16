/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.security.auth.Destroyable;
import javax.security.auth.DestroyFailedException;
import sun.security.util.HexDumpEncoder;
import sun.security.krb5.Asn1Exception;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.EncryptionKey;
import sun.security.krb5.EncryptedData;
import sun.security.krb5.KrbException;
import sun.security.util.DerValue;

/**
 * This class encapsulates a Kerberos encryption key. It is not associated
 * with a principal and may represent an ephemeral session key.
 *
 * @author Mayank Upadhyay
 * @since 1.4
 *
 * @serial include
 */
class KeyImpl implements SecretKey, Destroyable, Serializable {

    private static final long serialVersionUID = -7889313790214321193L;

    private transient byte[] keyBytes;
    private transient int keyType;
    private transient volatile boolean destroyed = false;


    /**
     * Constructs a KeyImpl from the given bytes.
     *
     * @param keyBytes the raw bytes for the secret key
     * @param keyType the key type for the secret key as defined by the
     * Kerberos protocol specification.
     */
    public KeyImpl(byte[] keyBytes,
                       int keyType) {
        this.keyBytes = keyBytes.clone();
        this.keyType = keyType;
    }

    /**
     * Constructs a KeyImpl from a password.
     *
     * @param principal the principal from which to derive the salt
     * @param password the password that should be used to compute the
     * key.
     * @param algorithm the name for the algorithm that this key wil be
     * used for. This parameter may be null in which case "DES" will be
     * assumed.
     */
    public KeyImpl(KerberosPrincipal principal,
                   char[] password,
                   String algorithm) {

        try {
            PrincipalName princ = new PrincipalName(principal.getName());
            EncryptionKey key;
            if ("none".equalsIgnoreCase(algorithm)) {
                key = EncryptionKey.NULL_KEY;
            } else {
                key = new EncryptionKey(password, princ.getSalt(), algorithm);
            }
            this.keyBytes = key.getBytes();
            this.keyType = key.getEType();
        } catch (KrbException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Returns the keyType for this key as defined in the Kerberos Spec.
     */
    public final int getKeyType() {
        if (destroyed)
            throw new IllegalStateException("This key is no longer valid");
        return keyType;
    }

    /*
     * Methods from java.security.Key
     */

    public final String getAlgorithm() {
        return getAlgorithmName(keyType);
    }

    private String getAlgorithmName(int eType) {
        if (destroyed)
            throw new IllegalStateException("This key is no longer valid");

        switch (eType) {
        case EncryptedData.ETYPE_DES_CBC_CRC:
            return "des-cbc-crc";

        case EncryptedData.ETYPE_DES_CBC_MD5:
            return "des-cbc-md5";

        case EncryptedData.ETYPE_DES3_CBC_HMAC_SHA1_KD:
            return "des3-cbc-sha1-kd";

        case EncryptedData.ETYPE_ARCFOUR_HMAC:
            return "rc4-hmac";

        case EncryptedData.ETYPE_AES128_CTS_HMAC_SHA1_96:
            return "aes128-cts-hmac-sha1-96";

        case EncryptedData.ETYPE_AES256_CTS_HMAC_SHA1_96:
            return "aes256-cts-hmac-sha1-96";

        case EncryptedData.ETYPE_AES128_CTS_HMAC_SHA256_128:
            return "aes128-cts-hmac-sha256-128";

        case EncryptedData.ETYPE_AES256_CTS_HMAC_SHA384_192:
            return "aes256-cts-hmac-sha384-192";

        case EncryptedData.ETYPE_NULL:
            return "none";

        default:
            return eType > 0 ? "unknown" : "private";
        }
    }

    public final String getFormat() {
        if (destroyed)
            throw new IllegalStateException("This key is no longer valid");
        return "RAW";
    }

    public final byte[] getEncoded() {
        if (destroyed)
            throw new IllegalStateException("This key is no longer valid");
        return keyBytes.clone();
    }

    public void destroy() throws DestroyFailedException {
        if (!destroyed) {
            destroyed = true;
            Arrays.fill(keyBytes, (byte) 0);
        }
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Writes the state of this object to the stream.

     * @serialData this {@code KeyImpl} is serialized by
     * writing out the ASN.1 Encoded bytes of the encryption key.
     * The ASN.1 encoding is defined in RFC4120 as follows:
     * EncryptionKey   ::= SEQUENCE {
     *          keytype    [0] Int32 -- actually encryption type --,
     *          keyvalue   [1] OCTET STRING
     *
     * @param  oos the {@code ObjectOutputStream} to which data is written
     * @throws IOException if an I/O error occurs
     * }
     */
    private void writeObject(ObjectOutputStream oos)
                throws IOException {
        if (destroyed) {
           throw new IOException("This key is no longer valid");
        }

        try {
           oos.writeObject((new EncryptionKey(keyType, keyBytes)).asn1Encode());
        } catch (Asn1Exception ae) {
           throw new IOException(ae.getMessage());
        }
    }

    /**
     * Restores the state of this object from the stream.
     *
     * @param  ois the {@code ObjectInputStream} from which data is read
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a serialized class cannot be loaded
     */
    private void readObject(ObjectInputStream ois)
                throws IOException, ClassNotFoundException {
        try {
            EncryptionKey encKey = new EncryptionKey(new
                                     DerValue((byte[])ois.readObject()));
            keyType = encKey.getEType();
            keyBytes = encKey.getBytes();
        } catch (Asn1Exception ae) {
            throw new IOException(ae.getMessage());
        }
    }

    public String toString() {
        HexDumpEncoder hd = new HexDumpEncoder();
        return "EncryptionKey: keyType=" + keyType
                          + " keyBytes (hex dump)="
                          + (keyBytes == null || keyBytes.length == 0 ?
                             " Empty Key" :
                             '\n' + hd.encodeBuffer(keyBytes)
                          + '\n');


    }

    public int hashCode() {
        int result = 17;
        if(isDestroyed()) {
            return result;
        }
        result = 37 * result + Arrays.hashCode(keyBytes);
        return 37 * result + keyType;
    }

    public boolean equals(Object other) {

        if (other == this)
            return true;

        if (! (other instanceof KeyImpl)) {
            return false;
        }

        KeyImpl otherKey = ((KeyImpl) other);
        if (isDestroyed() || otherKey.isDestroyed()) {
            return false;
        }

        if(keyType != otherKey.getKeyType() ||
                !Arrays.equals(keyBytes, otherKey.getEncoded())) {
            return false;
        }

        return true;
    }
}
