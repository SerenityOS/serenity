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

package sun.security.pkcs;

import java.io.*;
import java.security.Key;
import java.security.KeyRep;
import java.security.PrivateKey;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

import jdk.internal.access.SharedSecrets;
import sun.security.x509.*;
import sun.security.util.*;

/**
 * Holds a PKCS#8 key, for example a private key
 *
 * According to https://tools.ietf.org/html/rfc5958:
 *
 *     OneAsymmetricKey ::= SEQUENCE {
 *        version                   Version,
 *        privateKeyAlgorithm       PrivateKeyAlgorithmIdentifier,
 *        privateKey                PrivateKey,
 *        attributes            [0] Attributes OPTIONAL,
 *        ...,
 *        [[2: publicKey        [1] PublicKey OPTIONAL ]],
 *        ...
 *      }
 *
 * We support this format but do not parse attributes and publicKey now.
 */
public class PKCS8Key implements PrivateKey {

    /** use serialVersionUID from JDK 1.1. for interoperability */
    @java.io.Serial
    private static final long serialVersionUID = -3836890099307167124L;

    /* The algorithm information (name, parameters, etc). */
    protected AlgorithmId algid;

    /* The key bytes, without the algorithm information */
    protected byte[] key;

    /* The encoded for the key. Created on demand by encode(). */
    protected byte[] encodedKey;

    /* The version for this key */
    private static final int V1 = 0;
    private static final int V2 = 1;

    /**
     * Default constructor. Constructors in sub-classes that create a new key
     * from its components require this. These constructors must initialize
     * {@link #algid} and {@link #key}.
     */
    protected PKCS8Key() { }

    /**
     * Another constructor. Constructors in sub-classes that create a new key
     * from an encoded byte array require this. We do not assign this
     * encoding to {@link #encodedKey} directly.
     *
     * This method is also used by {@link #parseKey} to create a raw key.
     */
    protected PKCS8Key(byte[] input) throws InvalidKeyException {
        decode(new ByteArrayInputStream(input));
    }

    private void decode(InputStream is) throws InvalidKeyException {
        DerValue val = null;
        try {
            val = new DerValue(is);
            if (val.tag != DerValue.tag_Sequence) {
                throw new InvalidKeyException("invalid key format");
            }

            int version = val.data.getInteger();
            if (version != V1 && version != V2) {
                throw new InvalidKeyException("unknown version: " + version);
            }
            algid = AlgorithmId.parse (val.data.getDerValue ());
            key = val.data.getOctetString();

            DerValue next;
            if (val.data.available() == 0) {
                return;
            }
            next = val.data.getDerValue();
            if (next.isContextSpecific((byte)0)) {
                if (val.data.available() == 0) {
                    return;
                }
                next = val.data.getDerValue();
            }

            if (next.isContextSpecific((byte)1)) {
                if (version == V1) {
                    throw new InvalidKeyException("publicKey seen in v1");
                }
                if (val.data.available() == 0) {
                    return;
                }
            }
            throw new InvalidKeyException("Extra bytes");
        } catch (IOException e) {
            throw new InvalidKeyException("IOException : " + e.getMessage());
        } finally {
            if (val != null) {
                val.clear();
            }
        }
    }

    /**
     * Construct PKCS#8 subject public key from a DER encoding.  If a
     * security provider supports the key algorithm with a specific class,
     * a PrivateKey from the provider is returned.  Otherwise, a raw
     * PKCS8Key object is returned.
     *
     * <P>This mechanism guarantees that keys (and algorithms) may be
     * freely manipulated and transferred, without risk of losing
     * information.  Also, when a key (or algorithm) needs some special
     * handling, that specific need can be accommodated.
     *
     * @param encoded the DER-encoded SubjectPublicKeyInfo value
     * @exception IOException on data format errors
     */
    public static PrivateKey parseKey(byte[] encoded) throws IOException {
        try {
            PKCS8Key rawKey = new PKCS8Key(encoded);
            byte[] internal = rawKey.getEncodedInternal();
            PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(internal);
            PrivateKey result = null;
            try {
                result = KeyFactory.getInstance(rawKey.algid.getName())
                        .generatePrivate(pkcs8KeySpec);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                // Ignore and return raw key
                result = rawKey;
            } finally {
                if (result != rawKey) {
                    rawKey.clear();
                }
                SharedSecrets.getJavaSecuritySpecAccess()
                        .clearEncodedKeySpec(pkcs8KeySpec);
            }
            return result;
        } catch (InvalidKeyException e) {
            throw new IOException("corrupt private key", e);
        }
    }

    /**
     * Returns the algorithm to be used with this key.
     */
    public String getAlgorithm() {
        return algid.getName();
    }

    /**
     * Returns the algorithm ID to be used with this key.
     */
    public AlgorithmId  getAlgorithmId () {
        return algid;
    }

    /**
     * Returns the DER-encoded form of the key as a byte array,
     * or {@code null} if an encoding error occurs.
     */
    public byte[] getEncoded() {
        byte[] b = getEncodedInternal();
        return (b == null) ? null : b.clone();
    }

    /**
     * Returns the format for this key: "PKCS#8"
     */
    public String getFormat() {
        return "PKCS#8";
    }

    /**
     * DER-encodes this key as a byte array stored inside this object
     * and return it.
     *
     * @return the encoding, or null if there is an I/O error.
     */
    private synchronized byte[] getEncodedInternal() {
        if (encodedKey == null) {
            try {
                DerOutputStream tmp = new DerOutputStream();
                tmp.putInteger(V1);
                algid.encode(tmp);
                tmp.putOctetString(key);
                DerValue out = DerValue.wrap(DerValue.tag_Sequence, tmp);
                encodedKey = out.toByteArray();
                out.clear();
            } catch (IOException e) {
                // encodedKey is still null
            }
        }
        return encodedKey;
    }

    @java.io.Serial
    protected Object writeReplace() throws java.io.ObjectStreamException {
        return new KeyRep(KeyRep.Type.PRIVATE,
                getAlgorithm(),
                getFormat(),
                getEncodedInternal());
    }

    /**
     * We used to serialize a PKCS8Key as itself (instead of a KeyRep).
     */
    @java.io.Serial
    private void readObject(ObjectInputStream stream) throws IOException {
        try {
            decode(stream);
        } catch (InvalidKeyException e) {
            throw new IOException("deserialized key is invalid: " +
                                  e.getMessage());
        }
    }

    /**
     * Compares two private keys. This returns false if the object with which
     * to compare is not of type <code>Key</code>.
     * Otherwise, the encoding of this key object is compared with the
     * encoding of the given key object.
     *
     * @param object the object with which to compare
     * @return {@code true} if this key has the same encoding as the
     *          object argument; {@code false} otherwise.
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof PKCS8Key) {
            // time-constant comparison
            return MessageDigest.isEqual(
                    getEncodedInternal(),
                    ((PKCS8Key)object).getEncodedInternal());
        } else if (object instanceof Key) {
            // time-constant comparison
            byte[] otherEncoded = ((Key)object).getEncoded();
            try {
                return MessageDigest.isEqual(
                        getEncodedInternal(),
                        otherEncoded);
            } finally {
                if (otherEncoded != null) {
                    Arrays.fill(otherEncoded, (byte) 0);
                }
            }
        }
        return false;
    }

    /**
     * Calculates a hash code value for this object. Objects
     * which are equal will also have the same hashcode.
     */
    public int hashCode() {
        return Arrays.hashCode(getEncodedInternal());
    }

    public void clear() {
        if (encodedKey != null) {
            Arrays.fill(encodedKey, (byte)0);
        }
        Arrays.fill(key, (byte)0);
    }
}
