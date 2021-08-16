/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.math.BigInteger;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHGenParameterSpec;

import sun.security.provider.ParameterCache;
import static sun.security.util.SecurityProviderConstants.DEF_DH_KEY_SIZE;

/**
 * This class represents the key pair generator for Diffie-Hellman key pairs.
 *
 * <p>This key pair generator may be initialized in two different ways:
 *
 * <ul>
 * <li>By providing the size in bits of the prime modulus -
 * This will be used to create a prime modulus and base generator, which will
 * then be used to create the Diffie-Hellman key pair.
 * <li>By providing a prime modulus and base generator
 * </ul>
 *
 * @author Jan Luehe
 *
 *
 * @see java.security.KeyPairGenerator
 */
public final class DHKeyPairGenerator extends KeyPairGeneratorSpi {

    // parameters to use or null if not specified
    private DHParameterSpec params;

    // The size in bits of the prime modulus
    private int pSize;

    // The size in bits of the random exponent (private value)
    private int lSize;

    // The source of randomness
    private SecureRandom random;

    public DHKeyPairGenerator() {
        super();
        initialize(DEF_DH_KEY_SIZE, null);
    }

    private static void checkKeySize(int keysize)
            throws InvalidParameterException {

        if ((keysize < 512) || (keysize > 8192) || ((keysize & 0x3F) != 0)) {
            throw new InvalidParameterException(
                    "DH key size must be multiple of 64, and can only range " +
                    "from 512 to 8192 (inclusive). " +
                    "The specific key size " + keysize + " is not supported");
        }
    }

    /**
     * Initializes this key pair generator for a certain keysize and source of
     * randomness.
     * The keysize is specified as the size in bits of the prime modulus.
     *
     * @param keysize the keysize (size of prime modulus) in bits
     * @param random the source of randomness
     */
    public void initialize(int keysize, SecureRandom random) {
        checkKeySize(keysize);

        // Use the built-in parameters (ranging from 512 to 8192)
        // when available.
        this.params = ParameterCache.getCachedDHParameterSpec(keysize);

        // Due to performance issue, only support DH parameters generation
        // up to 1024 bits.
        if ((this.params == null) && (keysize > 1024)) {
            throw new InvalidParameterException(
                "Unsupported " + keysize + "-bit DH parameter generation");
        }

        this.pSize = keysize;
        this.lSize = 0;
        this.random = random;
    }

    /**
     * Initializes this key pair generator for the specified parameter
     * set and source of randomness.
     *
     * <p>The given parameter set contains the prime modulus, the base
     * generator, and optionally the requested size in bits of the random
     * exponent (private value).
     *
     * @param algParams the parameter set used to generate the key pair
     * @param random the source of randomness
     *
     * @exception InvalidAlgorithmParameterException if the given parameters
     * are inappropriate for this key pair generator
     */
    public void initialize(AlgorithmParameterSpec algParams,
            SecureRandom random) throws InvalidAlgorithmParameterException {
        if (!(algParams instanceof DHParameterSpec)){
            throw new InvalidAlgorithmParameterException
                ("Inappropriate parameter type");
        }

        params = (DHParameterSpec)algParams;
        pSize = params.getP().bitLength();
        try {
            checkKeySize(pSize);
        } catch (InvalidParameterException ipe) {
            throw new InvalidAlgorithmParameterException(ipe.getMessage());
        }

        // exponent size is optional, could be 0
        lSize = params.getL();

        // Require exponentSize < primeSize
        if ((lSize != 0) && (lSize > pSize)) {
            throw new InvalidAlgorithmParameterException
                ("Exponent size must not be larger than modulus size");
        }
        this.random = random;
    }

    /**
     * Generates a key pair.
     *
     * @return the new key pair
     */
    public KeyPair generateKeyPair() {
        if (random == null) {
            random = SunJCE.getRandom();
        }

        if (params == null) {
            try {
                params = ParameterCache.getDHParameterSpec(pSize, random);
            } catch (GeneralSecurityException e) {
                // should never happen
                throw new ProviderException(e);
            }
        }

        BigInteger p = params.getP();
        BigInteger g = params.getG();

        if (lSize <= 0) {
            lSize = pSize >> 1;
            // use an exponent size of (pSize / 2) but at least 384 bits
            if (lSize < 384) {
                lSize = 384;
            }
        }

        BigInteger x;
        BigInteger pMinus2 = p.subtract(BigInteger.TWO);

        //
        // PKCS#3 section 7.1 "Private-value generation"
        // Repeat if either of the followings does not hold:
        //     0 < x < p-1
        //     2^(lSize-1) <= x < 2^(lSize)
        //
        do {
            // generate random x up to 2^lSize bits long
            x = new BigInteger(lSize, random);
        } while ((x.compareTo(BigInteger.ONE) < 0) ||
            ((x.compareTo(pMinus2) > 0)) || (x.bitLength() != lSize));

        // calculate public value y
        BigInteger y = g.modPow(x, p);

        DHPublicKey pubKey = new DHPublicKey(y, p, g, lSize);
        DHPrivateKey privKey = new DHPrivateKey(x, p, g, lSize);
        return new KeyPair(pubKey, privKey);
    }
}
