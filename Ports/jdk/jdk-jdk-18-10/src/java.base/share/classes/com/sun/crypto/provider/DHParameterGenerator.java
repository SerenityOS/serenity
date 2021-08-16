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
import java.security.spec.*;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHGenParameterSpec;

import static sun.security.util.SecurityProviderConstants.DEF_DH_KEY_SIZE;

/*
 * This class generates parameters for the Diffie-Hellman algorithm.
 * The parameters are a prime, a base, and optionally the length in bits of
 * the private value.
 *
 * <p>The Diffie-Hellman parameter generation accepts the size in bits of the
 * prime modulus and the size in bits of the random exponent as input.
 *
 * @author Jan Luehe
 *
 *
 * @see java.security.AlgorithmParameters
 * @see java.security.spec.AlgorithmParameterSpec
 * @see DHParameters
 */
public final class DHParameterGenerator extends AlgorithmParameterGeneratorSpi {

    // The size in bits of the prime modulus
    private int primeSize = DEF_DH_KEY_SIZE;

    // The size in bits of the random exponent (private value)
    private int exponentSize = 0;

    // The source of randomness
    private SecureRandom random = null;

    private static void checkKeySize(int keysize)
            throws InvalidParameterException {

        boolean supported = ((keysize == 2048) || (keysize == 3072) ||
            ((keysize >= 512) && (keysize <= 1024) && ((keysize & 0x3F) == 0)));

        if (!supported) {
            throw new InvalidParameterException(
                    "DH key size must be multiple of 64 and range " +
                    "from 512 to 1024 (inclusive), or 2048, 3072. " +
                    "The specific key size " + keysize + " is not supported");
        }
    }

    /**
     * Initializes this parameter generator for a certain keysize
     * and source of randomness.
     * The keysize is specified as the size in bits of the prime modulus.
     *
     * @param keysize the keysize (size of prime modulus) in bits
     * @param random the source of randomness
     */
    @Override
    protected void engineInit(int keysize, SecureRandom random) {
        checkKeySize(keysize);
        this.primeSize = keysize;
        this.random = random;
    }

    /**
     * Initializes this parameter generator with a set of parameter
     * generation values, which specify the size of the prime modulus and
     * the size of the random exponent, both in bits.
     *
     * @param genParamSpec the set of parameter generation values
     * @param random the source of randomness
     *
     * @exception InvalidAlgorithmParameterException if the given parameter
     * generation values are inappropriate for this parameter generator
     */
    @Override
    protected void engineInit(AlgorithmParameterSpec genParamSpec,
            SecureRandom random) throws InvalidAlgorithmParameterException {

        if (!(genParamSpec instanceof DHGenParameterSpec)) {
            throw new InvalidAlgorithmParameterException
                ("Inappropriate parameter type");
        }

        DHGenParameterSpec dhParamSpec = (DHGenParameterSpec)genParamSpec;
        primeSize = dhParamSpec.getPrimeSize();
        exponentSize = dhParamSpec.getExponentSize();
        if ((exponentSize <= 0) || (exponentSize >= primeSize)) {
            throw new InvalidAlgorithmParameterException(
                    "Exponent size (" + exponentSize +
                    ") must be positive and less than modulus size (" +
                    primeSize + ")");
        }
        try {
            checkKeySize(primeSize);
        } catch (InvalidParameterException ipe) {
            throw new InvalidAlgorithmParameterException(ipe.getMessage());
        }

        this.random = random;
    }

    /**
     * Generates the parameters.
     *
     * @return the new AlgorithmParameters object
     */
    @Override
    protected AlgorithmParameters engineGenerateParameters() {

        if (random == null) {
            random = SunJCE.getRandom();
        }

        BigInteger paramP = null;
        BigInteger paramG = null;
        try {
            AlgorithmParameterGenerator dsaParamGen =
                    AlgorithmParameterGenerator.getInstance("DSA");
            dsaParamGen.init(primeSize, random);
            AlgorithmParameters dsaParams = dsaParamGen.generateParameters();
            DSAParameterSpec dsaParamSpec =
                    dsaParams.getParameterSpec(DSAParameterSpec.class);

            DHParameterSpec dhParamSpec;
            if (this.exponentSize > 0) {
                dhParamSpec = new DHParameterSpec(dsaParamSpec.getP(),
                                                  dsaParamSpec.getG(),
                                                  this.exponentSize);
            } else {
                dhParamSpec = new DHParameterSpec(dsaParamSpec.getP(),
                                                  dsaParamSpec.getG());
            }
            AlgorithmParameters algParams =
                    AlgorithmParameters.getInstance("DH", SunJCE.getInstance());
            algParams.init(dhParamSpec);

            return algParams;
        } catch (Exception ex) {
            throw new ProviderException("Unexpected exception", ex);
        }
    }
}
