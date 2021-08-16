/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.security.spec;

import java.math.BigInteger;
import java.security.spec.AlgorithmParameterSpec;

/**
 * This class specifies the set of parameters used to generate an RSA
 * key pair.
 *
 * @author Jan Luehe
 *
 * @see java.security.KeyPairGenerator#initialize(java.security.spec.AlgorithmParameterSpec)
 *
 * @since 1.3
 */

public class RSAKeyGenParameterSpec implements AlgorithmParameterSpec {

    private int keysize;
    private BigInteger publicExponent;
    private AlgorithmParameterSpec keyParams;

    /**
     * The public-exponent value F0 = 3.
     */
    public static final BigInteger F0 = BigInteger.valueOf(3);

    /**
     * The public exponent-value F4 = 65537.
     */
    public static final BigInteger F4 = BigInteger.valueOf(65537);

    /**
     * Constructs a new {@code RSAKeyGenParameterSpec} object from the
     * given keysize, public-exponent value, and null key parameters.
     *
     * @param keysize the modulus size (specified in number of bits)
     * @param publicExponent the public exponent
     */
    public RSAKeyGenParameterSpec(int keysize, BigInteger publicExponent) {
        this(keysize, publicExponent, null);
    }

    /**
     * Constructs a new {@code RSAKeyGenParameterSpec} object from the
     * given keysize, public-exponent value, and key parameters.
     *
     * @param keysize the modulus size (specified in number of bits)
     * @param publicExponent the public exponent
     * @param keyParams the key parameters, may be null
     * @since 11
     */
    public RSAKeyGenParameterSpec(int keysize, BigInteger publicExponent,
            AlgorithmParameterSpec keyParams) {
        this.keysize = keysize;
        this.publicExponent = publicExponent;
        this.keyParams = keyParams;
    }

    /**
     * Returns the keysize.
     *
     * @return the keysize.
     */
    public int getKeysize() {
        return keysize;
    }

    /**
     * Returns the public-exponent value.
     *
     * @return the public-exponent value.
     */
    public BigInteger getPublicExponent() {
        return publicExponent;
    }

    /**
     * Returns the parameters to be associated with key.
     *
     * @return the associated parameters, may be null if
     *         not present
     * @since 11
     */
    public AlgorithmParameterSpec getKeyParams() {
        return keyParams;
    }
}
