/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;
import java.util.Objects;
import java.math.BigInteger;
import java.security.KeyRep;
import java.security.PrivateKey;
import java.security.InvalidKeyException;
import java.security.ProviderException;
import javax.crypto.spec.DHParameterSpec;
import sun.security.util.*;

/**
 * A private key in PKCS#8 format for the Diffie-Hellman key agreement
 * algorithm.
 *
 * @author Jan Luehe
 *
 *
 * @see DHPublicKey
 * @see java.security.KeyAgreement
 */
final class DHPrivateKey implements PrivateKey,
        javax.crypto.interfaces.DHPrivateKey, Serializable {

    @java.io.Serial
    static final long serialVersionUID = 7565477590005668886L;

    // only supported version of PKCS#8 PrivateKeyInfo
    private static final BigInteger PKCS8_VERSION = BigInteger.ZERO;

    // the private key
    private BigInteger x;

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

    /**
     * Make a DH private key out of a private value <code>x</code>, a prime
     * modulus <code>p</code>, and a base generator <code>g</code>.
     *
     * @param x the private value
     * @param p the prime modulus
     * @param g the base generator
     *
     * @throws ProviderException if the key cannot be encoded
     */
    DHPrivateKey(BigInteger x, BigInteger p, BigInteger g)
        throws InvalidKeyException {
        this(x, p, g, 0);
    }

    /**
     * Make a DH private key out of a private value <code>x</code>, a prime
     * modulus <code>p</code>, a base generator <code>g</code>, and a
     * private-value length <code>l</code>.
     *
     * @param x the private value
     * @param p the prime modulus
     * @param g the base generator
     * @param l the private-value length
     *
     * @throws ProviderException if the key cannot be encoded
     */
    DHPrivateKey(BigInteger x, BigInteger p, BigInteger g, int l) {
        this.x = x;
        this.p = p;
        this.g = g;
        this.l = l;
        try {
            byte[] xbytes = x.toByteArray();
            DerValue val = new DerValue(DerValue.tag_Integer, xbytes);
            this.key = val.toByteArray();
            val.clear();
            Arrays.fill(xbytes, (byte)0);
            encode();
        } catch (IOException e) {
            throw new ProviderException("Cannot produce ASN.1 encoding", e);
        }
    }

    /**
     * Make a DH private key from its DER encoding (PKCS #8).
     *
     * @param encodedKey the encoded key
     *
     * @throws InvalidKeyException if the encoded key does not represent
     * a Diffie-Hellman private key
     */
    DHPrivateKey(byte[] encodedKey) throws InvalidKeyException {
        DerValue val = null;
        try {
            val = new DerValue(encodedKey);
            if (val.tag != DerValue.tag_Sequence) {
                throw new InvalidKeyException ("Key not a SEQUENCE");
            }

            //
            // version
            //
            BigInteger parsedVersion = val.data.getBigInteger();
            if (!parsedVersion.equals(PKCS8_VERSION)) {
                throw new IOException("version mismatch: (supported: " +
                                      PKCS8_VERSION + ", parsed: " +
                                      parsedVersion);
            }

            //
            // privateKeyAlgorithm
            //
            DerValue algid = val.data.getDerValue();
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
            // parse the parameters
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

            //
            // privateKey
            //
            this.key = val.data.getOctetString();
            parseKeyBits();

            this.encodedKey = encodedKey.clone();
        } catch (IOException | NumberFormatException e) {
            throw new InvalidKeyException("Error parsing key encoding", e);
        } finally {
            if (val != null) {
                val.clear();
            }
        }
    }

    /**
     * Returns the encoding format of this key: "PKCS#8"
     */
    public String getFormat() {
        return "PKCS#8";
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
        encode();
        return encodedKey.clone();
    }

    /**
     * Generate the encodedKey field if it has not been calculated.
     * Could generate null.
     */
    private void encode() {
        if (this.encodedKey == null) {
            try {
                DerOutputStream tmp = new DerOutputStream();

                //
                // version
                //
                tmp.putInteger(PKCS8_VERSION);

                //
                // privateKeyAlgorithm
                //
                DerOutputStream algid = new DerOutputStream();

                // store OID
                algid.putOID(DHPublicKey.DH_OID);
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
                // wrap algid into SEQUENCE
                tmp.write(DerValue.tag_Sequence, algid);

                // privateKey
                tmp.putOctetString(this.key);

                // make it a SEQUENCE
                DerValue val = DerValue.wrap(DerValue.tag_Sequence, tmp);
                this.encodedKey = val.toByteArray();
                val.clear();
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }
    }

    /**
     * Returns the private value, <code>x</code>.
     *
     * @return the private value, <code>x</code>
     */
    public BigInteger getX() {
        return this.x;
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

    private void parseKeyBits() throws InvalidKeyException {
        try {
            DerInputStream in = new DerInputStream(this.key);
            this.x = in.getBigInteger();
        } catch (IOException e) {
            InvalidKeyException ike = new InvalidKeyException(
                "Error parsing key encoding: " + e.getMessage());
            ike.initCause(e);
            throw ike;
        }
    }

    /**
     * Calculates a hash code value for the object.
     * Objects that are equal will also have the same hashcode.
     */
    public int hashCode() {
        return Objects.hash(x, p, g);
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (!(obj instanceof javax.crypto.interfaces.DHPrivateKey)) {
            return false;
        }
        javax.crypto.interfaces.DHPrivateKey other =
                (javax.crypto.interfaces.DHPrivateKey) obj;
        DHParameterSpec otherParams = other.getParams();
        return ((this.x.compareTo(other.getX()) == 0) &&
                (this.p.compareTo(otherParams.getP()) == 0) &&
                (this.g.compareTo(otherParams.getG()) == 0));
    }

    /**
     * Replace the DH private key to be serialized.
     *
     * @return the standard KeyRep object to be serialized
     *
     * @throws java.io.ObjectStreamException if a new object representing
     * this DH private key could not be created
     */
    @java.io.Serial
    private Object writeReplace() throws java.io.ObjectStreamException {
        encode();
        return new KeyRep(KeyRep.Type.PRIVATE,
                getAlgorithm(),
                getFormat(),
                encodedKey);
    }
}
