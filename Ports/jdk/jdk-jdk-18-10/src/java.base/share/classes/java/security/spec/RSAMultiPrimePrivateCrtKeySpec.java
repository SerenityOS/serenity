/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Objects;

/**
 * This class specifies an RSA multi-prime private key, as defined in the
 * <a href="https://tools.ietf.org/rfc/rfc8017.txt">PKCS#1 v2.2</a> standard
 * using the Chinese Remainder Theorem (CRT) information values
 * for efficiency.
 *
 * @author Valerie Peng
 *
 *
 * @see java.security.Key
 * @see java.security.KeyFactory
 * @see KeySpec
 * @see PKCS8EncodedKeySpec
 * @see RSAPrivateKeySpec
 * @see RSAPublicKeySpec
 * @see RSAOtherPrimeInfo
 *
 * @since 1.4
 */

public class RSAMultiPrimePrivateCrtKeySpec extends RSAPrivateKeySpec {

    private final BigInteger publicExponent;
    private final BigInteger primeP;
    private final BigInteger primeQ;
    private final BigInteger primeExponentP;
    private final BigInteger primeExponentQ;
    private final BigInteger crtCoefficient;
    private final RSAOtherPrimeInfo[] otherPrimeInfo;

   /**
    * Creates a new {@code RSAMultiPrimePrivateCrtKeySpec}.
    *
    * <p>Note that the contents of {@code otherPrimeInfo}
    * are copied to protect against subsequent modification when
    * constructing this object.
    *
    * @param modulus         the modulus n
    * @param publicExponent  the public exponent e
    * @param privateExponent the private exponent d
    * @param primeP          the prime factor p of n
    * @param primeQ          the prime factor q of n
    * @param primeExponentP  this is d mod (p-1)
    * @param primeExponentQ  this is d mod (q-1)
    * @param crtCoefficient  the Chinese Remainder Theorem
    *                        coefficient q-1 mod p
    * @param otherPrimeInfo  triplets of the rest of primes, null can be
    *                        specified if there are only two prime factors
    *                        (p and q)
    * @throws NullPointerException     if any of the specified parameters
    *         with the exception of {@code otherPrimeInfo} is null
    * @throws IllegalArgumentException if an empty, i.e. 0-length,
    *         {@code otherPrimeInfo} is specified
    */
    public RSAMultiPrimePrivateCrtKeySpec(BigInteger modulus,
                                BigInteger publicExponent,
                                BigInteger privateExponent,
                                BigInteger primeP,
                                BigInteger primeQ,
                                BigInteger primeExponentP,
                                BigInteger primeExponentQ,
                                BigInteger crtCoefficient,
                                RSAOtherPrimeInfo[] otherPrimeInfo) {
        this(modulus, publicExponent, privateExponent, primeP, primeQ,
             primeExponentP, primeExponentQ, crtCoefficient, otherPrimeInfo,
             null);
    }

   /**
    * Creates a new {@code RSAMultiPrimePrivateCrtKeySpec} with additional
    * key parameters.
    *
    * <p>Note that the contents of {@code otherPrimeInfo}
    * are copied to protect against subsequent modification when
    * constructing this object.
    *
    * @param modulus          the modulus n
    * @param publicExponent   the public exponent e
    * @param privateExponent  the private exponent d
    * @param primeP           the prime factor p of n
    * @param primeQ           the prime factor q of n
    * @param primeExponentP   this is d mod (p-1)
    * @param primeExponentQ   this is d mod (q-1)
    * @param crtCoefficient   the Chinese Remainder Theorem coefficient
    *                         q-1 mod p
    * @param otherPrimeInfo   triplets of the rest of primes, null can be
    *                         specified if there are only two prime factors
    *                         (p and q)
    * @param keyParams        the parameters associated with key
    * @throws NullPointerException     if any of the specified parameters
    *         with the exception of {@code otherPrimeInfo} and {@code keyParams}
    *         is null
    * @throws IllegalArgumentException if an empty, i.e. 0-length,
    *         {@code otherPrimeInfo} is specified
    * @since 11
    */
    public RSAMultiPrimePrivateCrtKeySpec(BigInteger modulus,
                                BigInteger publicExponent,
                                BigInteger privateExponent,
                                BigInteger primeP,
                                BigInteger primeQ,
                                BigInteger primeExponentP,
                                BigInteger primeExponentQ,
                                BigInteger crtCoefficient,
                                RSAOtherPrimeInfo[] otherPrimeInfo,
                                AlgorithmParameterSpec keyParams) {
        super(modulus, privateExponent, keyParams);
        Objects.requireNonNull(modulus,
            "the modulus parameter must be non-null");
        Objects.requireNonNull(privateExponent,
            "the privateExponent parameter must be non-null");
        this.publicExponent = Objects.requireNonNull(publicExponent,
            "the publicExponent parameter must be non-null");
        this.primeP = Objects.requireNonNull(primeP,
            "the primeP parameter must be non-null");
        this.primeQ = Objects.requireNonNull(primeQ,
            "the primeQ parameter must be non-null");
        this.primeExponentP = Objects.requireNonNull(primeExponentP,
            "the primeExponentP parameter must be non-null");
        this.primeExponentQ = Objects.requireNonNull(primeExponentQ,
            "the primeExponentQ parameter must be non-null");
        this.crtCoefficient = Objects.requireNonNull(crtCoefficient,
            "the crtCoefficient parameter must be non-null");

        if (otherPrimeInfo == null)  {
            this.otherPrimeInfo = null;
        } else if (otherPrimeInfo.length == 0) {
            throw new IllegalArgumentException("the otherPrimeInfo " +
                                                "parameter must not be empty");
        } else {
            this.otherPrimeInfo = otherPrimeInfo.clone();
        }
    }

    /**
     * Returns the public exponent.
     *
     * @return the public exponent.
     */
    public BigInteger getPublicExponent() {
        return this.publicExponent;
    }

    /**
     * Returns the primeP.
     *
     * @return the primeP.
     */
    public BigInteger getPrimeP() {
        return this.primeP;
    }

    /**
     * Returns the primeQ.
     *
     * @return the primeQ.
     */
    public BigInteger getPrimeQ() {
        return this.primeQ;
    }

    /**
     * Returns the primeExponentP.
     *
     * @return the primeExponentP.
     */
    public BigInteger getPrimeExponentP() {
        return this.primeExponentP;
    }

    /**
     * Returns the primeExponentQ.
     *
     * @return the primeExponentQ.
     */
    public BigInteger getPrimeExponentQ() {
        return this.primeExponentQ;
    }

    /**
     * Returns the crtCoefficient.
     *
     * @return the crtCoefficient.
     */
    public BigInteger getCrtCoefficient() {
        return this.crtCoefficient;
    }

    /**
     * Returns a copy of the otherPrimeInfo or null if there are
     * only two prime factors (p and q).
     *
     * @return the otherPrimeInfo. Returns a new array each time this method
     *         is called.
     */
    public RSAOtherPrimeInfo[] getOtherPrimeInfo() {
        if (otherPrimeInfo == null) return null;
        return otherPrimeInfo.clone();
    }
}
