/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.crypto.provider;

import java.io.*;
import java.util.Objects;
import java.math.BigInteger;
import java.security.KeyRep;
import java.security.InvalidKeyException;
import java.security.ProviderException;
import java.security.PublicKey;
import javax.crypto.spec.DHParameterSpec;
import sun.security.util.*;


/**
 * A public key in X.509 format for the Diffie-Hellman key agreement algorithm.
 *
 * @author Jan Luehe
 *
 *
 * @see DHPrivateKey
 * @see javax.crypto.KeyAgreement
 */
final class DHPublicKey implements PublicKey,
javax.crypto.interfaces.DHPublicKey, Serializable {

    @java.io.Serial
    static final long serialVersionUID = 7647557958927458271L;

    // the public key
    private BigInteger y;

    // the key bytes, without the algorithm information
    private byte[] key;

    // the encoded key
    private byte[] encodedKey;

    // the prime modulus
    private BigInteger p;

    // the base generator
    private BigInteger g;

    // the private-value length (optional)
    private int l;

    // Note: this OID is used by DHPrivateKey as well.
    static ObjectIdentifier DH_OID =
            ObjectIdentifier.of(KnownOIDs.DiffieHellman);

    /**
     * Make a DH public key out of a public value <code>y</code>, a prime
     * modulus <code>p</code>, and a base generator <code>g</code>.
     *
     * @param y the public value
     * @param p the prime modulus
     * @param g the base generator
     *
     * @exception InvalidKeyException if the key cannot be encoded
     */
    DHPublicKey(BigInteger y, BigInteger p, BigInteger g)
        throws InvalidKeyException {
        this(y, p, g, 0);
    }

    /**
     * Make a DH public key out of a public value <code>y</code>, a prime
     * modulus <code>p</code>, a base generator <code>g</code>, and a
     * private-value length <code>l</code>.
     *
     * @param y the public value
     * @param p the prime modulus
     * @param g the base generator
     * @param l the private-value length
     *
     * @exception ProviderException if the key cannot be encoded
     */
    DHPublicKey(BigInteger y, BigInteger p, BigInteger g, int l) {
        this.y = y;
        this.p = p;
        this.g = g;
        this.l = l;
        try {
            this.key = new DerValue(DerValue.tag_Integer,
                                    this.y.toByteArray()).toByteArray();
            this.encodedKey = getEncoded();
        } catch (IOException e) {
            throw new ProviderException("Cannot produce ASN.1 encoding", e);
        }
    }

    /**
     * Make a DH public key from its DER encoding (X.509).
     *
     * @param encodedKey the encoded key
     *
     * @exception InvalidKeyException if the encoded key does not represent
     * a Diffie-Hellman public key
     */
    DHPublicKey(byte[] encodedKey) throws InvalidKeyException {
        InputStream inStream = new ByteArrayInputStream(encodedKey);
        try {
            DerValue derKeyVal = new DerValue(inStream);
            if (derKeyVal.tag != DerValue.tag_Sequence) {
                throw new InvalidKeyException ("Invalid key format");
            }

            /*
             * Parse the algorithm identifier
             */
            DerValue algid = derKeyVal.data.getDerValue();
            if (algid.tag != DerValue.tag_Sequence) {
                throw new InvalidKeyException("AlgId is not a SEQUENCE");
            }
            DerInputStream derInStream = algid.toDerInputStream();
            ObjectIdentifier oid = derInStream.getOID();
            if (oid == null) {
                throw new InvalidKeyException("Null OID");
            }
            if (derInStream.available() == 0) {
                throw new InvalidKeyException("Parameters missing");
            }

            /*
             * Parse the parameters
             */
            DerValue params = derInStream.getDerValue();
            if (params.tag == DerValue.tag_Null) {
                throw new InvalidKeyException("Null parameters");
            }
            if (params.tag != DerValue.tag_Sequence) {
                throw new InvalidKeyException("Parameters not a SEQUENCE");
            }
            params.data.reset();
            this.p = params.data.getBigInteger();
            this.g = params.data.getBigInteger();
            // Private-value length is OPTIONAL
            if (params.data.available() != 0) {
                this.l = params.data.getInteger();
            }
            if (params.data.available() != 0) {
                throw new InvalidKeyException("Extra parameter data");
            }

            /*
             * Parse the key
             */
            this.key = derKeyVal.data.getBitString();
            parseKeyBits();
            if (derKeyVal.data.available() != 0) {
                throw new InvalidKeyException("Excess key data");
            }

            this.encodedKey = encodedKey.clone();
        } catch (IOException | NumberFormatException e) {
            throw new InvalidKeyException("Error parsing key encoding", e);
        }
    }

    /**
     * Returns the encoding format of this key: "X.509"
     */
    public String getFormat() {
        return "X.509";
    }

    /**
     * Returns the name of the algorithm associated with this key: "DH"
     */
    public String getAlgorithm() {
        return "DH";
    }

    /**
     * Get the encoding of the key.
     */
    public synchronized byte[] getEncoded() {
        if (this.encodedKey == null) {
            try {
                DerOutputStream algid = new DerOutputStream();

                // store oid in algid
                algid.putOID(DH_OID);

                // encode parameters
                DerOutputStream params = new DerOutputStream();
                params.putInteger(this.p);
                params.putInteger(this.g);
                if (this.l != 0) {
                    params.putInteger(this.l);
                }
                // wrap parameters into SEQUENCE
                DerValue paramSequence = new DerValue(DerValue.tag_Sequence,
                                                      params.toByteArray());
                // store parameter SEQUENCE in algid
                algid.putDerValue(paramSequence);

                // wrap algid into SEQUENCE, and store it in key encoding
                DerOutputStream tmpDerKey = new DerOutputStream();
                tmpDerKey.write(DerValue.tag_Sequence, algid);

                // store key data
                tmpDerKey.putBitString(this.key);

                // wrap algid and key into SEQUENCE
                DerOutputStream derKey = new DerOutputStream();
                derKey.write(DerValue.tag_Sequence, tmpDerKey);
                this.encodedKey = derKey.toByteArray();
            } catch (IOException e) {
                return null;
            }
        }
        return this.encodedKey.clone();
    }

    /**
     * Returns the public value, <code>y</code>.
     *
     * @return the public value, <code>y</code>
     */
    public BigInteger getY() {
        return this.y;
    }

    /**
     * Returns the key parameters.
     *
     * @return the key parameters
     */
    public DHParameterSpec getParams() {
        if (this.l != 0) {
            return new DHParameterSpec(this.p, this.g, this.l);
        } else {
            return new DHParameterSpec(this.p, this.g);
        }
    }

    public String toString() {
        String LINE_SEP = System.lineSeparator();

        StringBuilder sb
            = new StringBuilder("SunJCE Diffie-Hellman Public Key:"
                               + LINE_SEP + "y:" + LINE_SEP
                               + Debug.toHexString(this.y)
                               + LINE_SEP + "p:" + LINE_SEP
                               + Debug.toHexString(this.p)
                               + LINE_SEP + "g:" + LINE_SEP
                               + Debug.toHexString(this.g));
        if (this.l != 0)
            sb.append(LINE_SEP + "l:" + LINE_SEP + "    " + this.l);
        return sb.toString();
    }

    private void parseKeyBits() throws InvalidKeyException {
        try {
            DerInputStream in = new DerInputStream(this.key);
            this.y = in.getBigInteger();
        } catch (IOException e) {
            throw new InvalidKeyException(
                "Error parsing key encoding: " + e.toString());
        }
    }

    /**
     * Calculates a hash code value for the object.
     * Objects that are equal will also have the same hashcode.
     */
    public int hashCode() {
        return Objects.hash(y, p, g);
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (!(obj instanceof javax.crypto.interfaces.DHPublicKey)) {
            return false;
        }

        javax.crypto.interfaces.DHPublicKey other =
            (javax.crypto.interfaces.DHPublicKey) obj;
        DHParameterSpec otherParams = other.getParams();
        return ((this.y.compareTo(other.getY()) == 0) &&
                (this.p.compareTo(otherParams.getP()) == 0) &&
                (this.g.compareTo(otherParams.getG()) == 0));
    }

    /**
     * Replace the DH public key to be serialized.
     *
     * @return the standard KeyRep object to be serialized
     *
     * @throws java.io.ObjectStreamException if a new object representing
     * this DH public key could not be created
     */
    @java.io.Serial
    private Object writeReplace() throws java.io.ObjectStreamException {
        return new KeyRep(KeyRep.Type.PUBLIC,
                        getAlgorithm(),
                        getFormat(),
                        getEncoded());
    }
}
