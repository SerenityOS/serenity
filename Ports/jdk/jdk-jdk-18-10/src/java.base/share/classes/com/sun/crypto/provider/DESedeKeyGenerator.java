/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

import javax.crypto.KeyGeneratorSpi;
import javax.crypto.SecretKey;
import javax.crypto.spec.DESedeKeySpec;
import java.security.SecureRandom;
import java.security.InvalidParameterException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.spec.AlgorithmParameterSpec;

/**
 * This class generates a Triple DES key.
 *
 * @author Jan Luehe
 *
 */

public final class DESedeKeyGenerator extends KeyGeneratorSpi {

    private SecureRandom random = null;
    private int keysize = 168;

    /**
     * Empty constructor
     */
    public DESedeKeyGenerator() {
    }

    /**
     * Initializes this key generator.
     *
     * @param random the source of randomness for this generator
     */
    protected void engineInit(SecureRandom random) {
        this.random = random;
    }

    /**
     * Initializes this key generator with the specified parameter
     * set and a user-provided source of randomness.
     *
     * @param params the key generation parameters
     * @param random the source of randomness for this key generator
     *
     * @exception InvalidAlgorithmParameterException if <code>params</code> is
     * inappropriate for this key generator
     */
    protected void engineInit(AlgorithmParameterSpec params,
                              SecureRandom random)
        throws InvalidAlgorithmParameterException {
            throw new InvalidAlgorithmParameterException
                ("Triple DES key generation does not take any parameters");
    }

    /**
     * Initializes this key generator for a certain keysize, using the given
     * source of randomness.
     *
     * @param keysize the keysize. This is an algorithm-specific
     * metric specified in number of bits. A keysize with 112 bits of entropy
     * corresponds to a Triple DES key with 2 intermediate keys, and a keysize
     * with 168 bits of entropy corresponds to a Triple DES key with 3
     * intermediate keys.
     * @param random the source of randomness for this key generator
     */
    protected void engineInit(int keysize, SecureRandom random) {
        if ((keysize != 112) && (keysize != 168)) {
            throw new InvalidParameterException("Wrong keysize: must be "
                                                + "equal to 112 or 168");
        }
        this.keysize = keysize;
        this.engineInit(random);
    }

    /**
     * Generates the Triple DES key.
     *
     * @return the new Triple DES key
     */
    protected SecretKey engineGenerateKey() {
        if (this.random == null) {
            this.random = SunJCE.getRandom();
        }

        byte[] rawkey = new byte[DESedeKeySpec.DES_EDE_KEY_LEN];

        if (keysize == 168) {
            // 3 intermediate keys
            this.random.nextBytes(rawkey);

            // Do parity adjustment for each intermediate key
            DESKeyGenerator.setParityBit(rawkey, 0);
            DESKeyGenerator.setParityBit(rawkey, 8);
            DESKeyGenerator.setParityBit(rawkey, 16);
        } else {
            // 2 intermediate keys
            byte[] tmpkey = new byte[16];
            this.random.nextBytes(tmpkey);
            DESKeyGenerator.setParityBit(tmpkey, 0);
            DESKeyGenerator.setParityBit(tmpkey, 8);
            System.arraycopy(tmpkey, 0, rawkey, 0, tmpkey.length);
            // Copy the first 8 bytes into the last
            System.arraycopy(tmpkey, 0, rawkey, 16, 8);
            java.util.Arrays.fill(tmpkey, (byte)0x00);
        }

        DESedeKey desEdeKey = null;
        try {
            desEdeKey = new DESedeKey(rawkey);
        } catch (InvalidKeyException ike) {
            // this never happens
            throw new RuntimeException(ike.getMessage());
        }

        java.util.Arrays.fill(rawkey, (byte)0x00);

        return desEdeKey;
    }
}
