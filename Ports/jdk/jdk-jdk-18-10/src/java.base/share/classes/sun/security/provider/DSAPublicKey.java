/*
 * Copyright (c) 1996, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.provider;

import java.util.*;
import java.io.*;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.ProviderException;
import java.security.AlgorithmParameters;
import java.security.spec.DSAParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.security.interfaces.DSAParams;

import sun.security.x509.X509Key;
import sun.security.x509.AlgIdDSA;
import sun.security.util.BitArray;
import sun.security.util.Debug;
import sun.security.util.DerValue;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;

/**
 * An X.509 public key for the Digital Signature Algorithm.
 *
 * @author Benjamin Renaud
 *
 *
 * @see DSAPrivateKey
 * @see AlgIdDSA
 * @see DSA
 */

public class DSAPublicKey extends X509Key
implements java.security.interfaces.DSAPublicKey, Serializable {

    /** use serialVersionUID from JDK 1.1. for interoperability */
    @java.io.Serial
    private static final long serialVersionUID = -2994193307391104133L;

    /* the public key */
    private BigInteger y;

    /*
     * Keep this constructor for backwards compatibility with JDK1.1.
     */
    public DSAPublicKey() {
    }

    /**
     * Make a DSA public key out of a public key and three parameters.
     * The p, q, and g parameters may be null, but if so, parameters will need
     * to be supplied from some other source before this key can be used in
     * cryptographic operations.  PKIX RFC2459bis explicitly allows DSA public
     * keys without parameters, where the parameters are provided in the
     * issuer's DSA public key.
     *
     * @param y the actual key bits
     * @param p DSA parameter p, may be null if all of p, q, and g are null.
     * @param q DSA parameter q, may be null if all of p, q, and g are null.
     * @param g DSA parameter g, may be null if all of p, q, and g are null.
     */
    public DSAPublicKey(BigInteger y, BigInteger p, BigInteger q,
                        BigInteger g)
    throws InvalidKeyException {
        this.y = y;
        algid = new AlgIdDSA(p, q, g);

        try {
            byte[] keyArray = new DerValue(DerValue.tag_Integer,
                               y.toByteArray()).toByteArray();
            setKey(new BitArray(keyArray.length*8, keyArray));
            encode();
        } catch (IOException e) {
            throw new InvalidKeyException("could not DER encode y: " +
                                          e.getMessage());
        }
    }

    /**
     * Make a DSA public key from its DER encoding (X.509).
     */
    public DSAPublicKey(byte[] encoded) throws InvalidKeyException {
        decode(encoded);
    }

    /**
     * Returns the DSA parameters associated with this key, or null if the
     * parameters could not be parsed.
     */
    public DSAParams getParams() {
        try {
            if (algid instanceof DSAParams) {
                return (DSAParams)algid;
            } else {
                DSAParameterSpec paramSpec;
                AlgorithmParameters algParams = algid.getParameters();
                if (algParams == null) {
                    return null;
                }
                paramSpec = algParams.getParameterSpec(DSAParameterSpec.class);
                return (DSAParams)paramSpec;
            }
        } catch (InvalidParameterSpecException e) {
            return null;
        }
    }

    /**
     * Get the raw public value, y, without the parameters.
     *
     * @see getParameters
     */
    public BigInteger getY() {
        return y;
    }

    public String toString() {
        return "Sun DSA Public Key\n    Parameters:" + algid
            + "\n  y:\n" + Debug.toHexString(y) + "\n";
    }

    protected void parseKeyBits() throws InvalidKeyException {
        try {
            DerInputStream in = new DerInputStream(getKey().toByteArray());
            y = in.getBigInteger();
        } catch (IOException e) {
            throw new InvalidKeyException("Invalid key: y value\n" +
                                          e.getMessage());
        }
    }
}
