/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.math.BigInteger;

import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;

import static java.math.BigInteger.*;
import sun.security.jca.JCAUtil;
import sun.security.rsa.RSAUtil.KeyType;

import static sun.security.util.SecurityProviderConstants.DEF_RSA_KEY_SIZE;
import static sun.security.util.SecurityProviderConstants.DEF_RSASSA_PSS_KEY_SIZE;

/**
 * RSA keypair generation. Standard algorithm, minimum key length 512 bit.
 * We generate two random primes until we find two where phi is relative
 * prime to the public exponent. Default exponent is 65537. It has only bit 0
 * and bit 4 set, which makes it particularly efficient.
 *
 * @since   1.5
 * @author  Andreas Sterbenz
 */
public abstract class RSAKeyPairGenerator extends KeyPairGeneratorSpi {

    private static final BigInteger SQRT_2048;
    private static final BigInteger SQRT_3072;
    private static final BigInteger SQRT_4096;

    static {
        SQRT_2048 = TWO.pow(2047).sqrt();
        SQRT_3072 = TWO.pow(3071).sqrt();
        SQRT_4096 = TWO.pow(4095).sqrt();
    }

    // public exponent to use
    private BigInteger publicExponent;

    // size of the key to generate, >= RSAKeyFactory.MIN_MODLEN
    private int keySize;

    private final KeyType type;
    private AlgorithmParameterSpec keyParams;

    // PRNG to use
    private SecureRandom random;

    // whether to generate key pairs following the new guidelines from
    // FIPS 186-4 and later
    private boolean useNew;

    RSAKeyPairGenerator(KeyType type, int defKeySize) {
        this.type = type;
        // initialize to default in case the app does not call initialize()
        initialize(defKeySize, null);
    }

    // initialize the generator. See JCA doc
    public void initialize(int keySize, SecureRandom random) {
        try {
            initialize(new RSAKeyGenParameterSpec(keySize,
                    RSAKeyGenParameterSpec.F4), random);
        } catch (InvalidAlgorithmParameterException iape) {
            throw new InvalidParameterException(iape.getMessage());
        }
    }

    // second initialize method. See JCA doc.
    public void initialize(AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidAlgorithmParameterException {
        if (params instanceof RSAKeyGenParameterSpec == false) {
            throw new InvalidAlgorithmParameterException
                ("Params must be instance of RSAKeyGenParameterSpec");
        }

        RSAKeyGenParameterSpec rsaSpec = (RSAKeyGenParameterSpec)params;
        int tmpKeySize = rsaSpec.getKeysize();
        BigInteger tmpPubExp = rsaSpec.getPublicExponent();
        AlgorithmParameterSpec tmpParams = rsaSpec.getKeyParams();

        // use the new approach for even key sizes >= 2048 AND when the
        // public exponent is within FIPS valid range
        boolean useNew = (tmpKeySize >= 2048 && ((tmpKeySize & 1) == 0));

        if (tmpPubExp == null) {
            tmpPubExp = RSAKeyGenParameterSpec.F4;
        } else {
            if (!tmpPubExp.testBit(0)) {
                throw new InvalidAlgorithmParameterException
                    ("Public exponent must be an odd number");
            }
            // current impl checks that  F0 <= e < 2^keysize
            // vs FIPS 186-4 checks that F4 <= e < 2^256
            // for backward compatibility, we keep the same checks
            BigInteger minValue = RSAKeyGenParameterSpec.F0;
            int maxBitLength = tmpKeySize;
            if (tmpPubExp.compareTo(RSAKeyGenParameterSpec.F0) < 0) {
                throw new InvalidAlgorithmParameterException
                        ("Public exponent must be " + minValue + " or larger");
            }
            if (tmpPubExp.bitLength() > maxBitLength) {
                throw new InvalidAlgorithmParameterException
                        ("Public exponent must be no longer than " +
                        maxBitLength + " bits");
            }
            useNew &= ((tmpPubExp.compareTo(RSAKeyGenParameterSpec.F4) >= 0) &&
                    (tmpPubExp.bitLength() < 256));
        }

        // do not allow unreasonably large key sizes, probably user error
        try {
            RSAKeyFactory.checkKeyLengths(tmpKeySize, tmpPubExp, 512,
                    64 * 1024);
        } catch (InvalidKeyException e) {
            throw new InvalidAlgorithmParameterException(
                "Invalid key sizes", e);
        }

        try {
            this.keyParams = RSAUtil.checkParamsAgainstType(type, tmpParams);
        } catch (ProviderException e) {
            throw new InvalidAlgorithmParameterException(
                "Invalid key parameters", e);
        }

        this.keySize = tmpKeySize;
        this.publicExponent = tmpPubExp;
        this.random = (random == null? JCAUtil.getSecureRandom() : random);
        this.useNew = useNew;
    }

    // FIPS 186-4 B.3.3 / FIPS 186-5 A.1.3
    // Generation of Random Primes that are Probably Prime
    public KeyPair generateKeyPair() {
        BigInteger e = publicExponent;
        BigInteger minValue = (useNew? getSqrt(keySize) : ZERO);
        int lp = (keySize + 1) >> 1;;
        int lq = keySize - lp;
        int pqDiffSize = lp - 100;

        while (true) {
            BigInteger p = null;
            BigInteger q = null;

            int i = 0;
            while (i++ < 10*lp) {
                BigInteger tmpP = BigInteger.probablePrime(lp, random);
                if ((!useNew || tmpP.compareTo(minValue) == 1) &&
                        isRelativePrime(e, tmpP.subtract(ONE))) {
                    p = tmpP;
                    break;
                }
            }
            if (p == null) {
                throw new ProviderException("Cannot find prime P");
            }

            i = 0;

            while (i++ < 20*lq) {
                BigInteger tmpQ = BigInteger.probablePrime(lq, random);

                if ((!useNew || tmpQ.compareTo(minValue) == 1) &&
                        (p.subtract(tmpQ).abs().compareTo
                                (TWO.pow(pqDiffSize)) == 1) &&
                        isRelativePrime(e, tmpQ.subtract(ONE))) {
                    q = tmpQ;
                    break;
                }
            }
            if (q == null) {
                throw new ProviderException("Cannot find prime Q");
            }

            BigInteger n = p.multiply(q);
            if (n.bitLength() != keySize) {
                // regenerate P, Q if n is not the right length; should
                // never happen for the new case but check it anyway
                continue;
            }

            KeyPair kp = createKeyPair(type, keyParams, n, e, p, q);
            // done, return the generated keypair;
            if (kp != null) return kp;
        }
    }

    private static BigInteger getSqrt(int keySize) {
        BigInteger sqrt = null;
        switch (keySize) {
            case 2048:
                sqrt = SQRT_2048;
                break;
            case 3072:
                sqrt = SQRT_3072;
                break;
            case 4096:
                sqrt = SQRT_4096;
                break;
            default:
                sqrt = TWO.pow(keySize-1).sqrt();
        }
        return sqrt;
    }

    private static boolean isRelativePrime(BigInteger e, BigInteger bi) {
        // optimize for common known public exponent prime values
        if (e.compareTo(RSAKeyGenParameterSpec.F4) == 0 ||
                e.compareTo(RSAKeyGenParameterSpec.F0) == 0) {
            return !bi.mod(e).equals(ZERO);
        } else {
            return e.gcd(bi).equals(ONE);
        }
    }

    private static KeyPair createKeyPair(KeyType type,
            AlgorithmParameterSpec keyParams,
            BigInteger n, BigInteger e, BigInteger p, BigInteger q) {
        // phi = (p - 1) * (q - 1) must be relative prime to e
        // otherwise RSA just won't work ;-)
        BigInteger p1 = p.subtract(ONE);
        BigInteger q1 = q.subtract(ONE);
        BigInteger phi = p1.multiply(q1);

        BigInteger gcd = p1.gcd(q1);
        BigInteger lcm = (gcd.equals(ONE)?  phi : phi.divide(gcd));

        BigInteger d = e.modInverse(lcm);

        if (d.compareTo(TWO.pow(p.bitLength())) != 1) {
            return null;
        }

        // 1st prime exponent pe = d mod (p - 1)
        BigInteger pe = d.mod(p1);
        // 2nd prime exponent qe = d mod (q - 1)
        BigInteger qe = d.mod(q1);
        // crt coefficient coeff is the inverse of q mod p
        BigInteger coeff = q.modInverse(p);

        try {
            PublicKey publicKey = new RSAPublicKeyImpl(type, keyParams, n, e);
            PrivateKey privateKey = new RSAPrivateCrtKeyImpl(
                type, keyParams, n, e, d, p, q, pe, qe, coeff);
            return new KeyPair(publicKey, privateKey);
        } catch (InvalidKeyException exc) {
            // invalid key exception only thrown for keys < 512 bit,
            // will not happen here
            throw new RuntimeException(exc);
        }
    }

    public static final class Legacy extends RSAKeyPairGenerator {
        public Legacy() {
            super(KeyType.RSA, DEF_RSA_KEY_SIZE);
        }
    }

    public static final class PSS extends RSAKeyPairGenerator {
        public PSS() {
            super(KeyType.PSS, DEF_RSASSA_PSS_KEY_SIZE);
        }
    }
}
