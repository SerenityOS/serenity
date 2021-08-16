/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.math.BigInteger;

import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.security.interfaces.*;
import java.util.Arrays;

import sun.security.util.*;
import sun.security.pkcs.PKCS8Key;

import sun.security.rsa.RSAUtil.KeyType;

/**
 * RSA private key implementation for "RSA", "RSASSA-PSS" algorithms in non-CRT
 * form (modulus, private exponent only). For CRT private keys, see
 * RSAPrivateCrtKeyImpl. We need separate classes to ensure correct behavior
 * in instanceof checks, etc.
 *
 * Note: RSA keys must be at least 512 bits long
 *
 * @see RSAPrivateCrtKeyImpl
 * @see RSAKeyFactory
 *
 * @since   1.5
 * @author  Andreas Sterbenz
 */
public final class RSAPrivateKeyImpl extends PKCS8Key implements RSAPrivateKey {

    @java.io.Serial
    private static final long serialVersionUID = -33106691987952810L;

    private final BigInteger n;         // modulus
    private final BigInteger d;         // private exponent

    private transient final KeyType type;

    // optional parameters associated with this RSA key
    // specified in the encoding of its AlgorithmId.
    // must be null for "RSA" keys.
    private transient final AlgorithmParameterSpec keyParams;

    /**
     * Construct a key from its components. Used by the
     * RSAKeyFactory and the RSAKeyPairGenerator.
     */
    RSAPrivateKeyImpl(KeyType type, AlgorithmParameterSpec keyParams,
            BigInteger n, BigInteger d) throws InvalidKeyException {

        RSAKeyFactory.checkRSAProviderKeyLengths(n.bitLength(), null);

        this.n = n;
        this.d = d;

        try {
            // validate and generate the algid encoding
            algid = RSAUtil.createAlgorithmId(type, keyParams);
        } catch (ProviderException pe) {
            throw new InvalidKeyException(pe);
        }

        this.type = type;
        this.keyParams = keyParams;

        try {
            // generate the key encoding
            byte[] nbytes = n.toByteArray();
            byte[] dbytes = d.toByteArray();
            DerOutputStream out = new DerOutputStream(
                    nbytes.length + dbytes.length + 50);
                    // Enough for 7 zeroes (21) and 2 tag+length(4)
            out.putInteger(0); // version must be 0
            out.putInteger(nbytes);
            Arrays.fill(nbytes, (byte)0);
            out.putInteger(0);
            out.putInteger(dbytes);
            Arrays.fill(dbytes, (byte)0);
            out.putInteger(0);
            out.putInteger(0);
            out.putInteger(0);
            out.putInteger(0);
            out.putInteger(0);
            DerValue val = DerValue.wrap(DerValue.tag_Sequence, out);
            key = val.toByteArray();
            val.clear();
        } catch (IOException exc) {
            // should never occur
            throw new InvalidKeyException(exc);
        }
    }

    // see JCA doc
    @Override
    public String getAlgorithm() {
        return type.keyAlgo;
    }

    // see JCA doc
    @Override
    public BigInteger getModulus() {
        return n;
    }

    // see JCA doc
    @Override
    public BigInteger getPrivateExponent() {
        return d;
    }

    // see JCA doc
    @Override
    public AlgorithmParameterSpec getParams() {
        return keyParams;
    }

    // return a string representation of this key for debugging
    @Override
    public String toString() {
        return "Sun " + type.keyAlgo + " private key, " + n.bitLength()
               + " bits" + "\n  params: " + keyParams + "\n  modulus: " + n
               + "\n  private exponent: " + d;
    }
}
