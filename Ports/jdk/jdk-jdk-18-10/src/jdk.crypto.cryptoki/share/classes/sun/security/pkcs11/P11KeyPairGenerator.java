/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.pkcs11;

import java.math.BigInteger;

import java.security.*;
import java.security.spec.*;

import javax.crypto.spec.DHParameterSpec;

import sun.security.provider.ParameterCache;
import static sun.security.util.SecurityProviderConstants.*;

import static sun.security.pkcs11.TemplateManager.*;
import sun.security.pkcs11.wrapper.*;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;


import sun.security.rsa.RSAKeyFactory;

/**
 * KeyPairGenerator implementation class. This class currently supports
 * RSA, DSA, DH, and EC.
 *
 * Note that for DSA and DH we rely on the Sun and SunJCE providers to
 * obtain the parameters from.
 *
 * @author  Andreas Sterbenz
 * @since   1.5
 */
final class P11KeyPairGenerator extends KeyPairGeneratorSpi {

    // token instance
    private final Token token;

    // algorithm name
    private final String algorithm;

    // mechanism id
    private final long mechanism;

    // selected or default key size, always valid
    private int keySize;

    // parameters specified via init, if any
    private AlgorithmParameterSpec params;

    // for RSA, selected or default value of public exponent, always valid
    private BigInteger rsaPublicExponent = RSAKeyGenParameterSpec.F4;

    // the supported keysize range of the native PKCS11 library
    // if mechanism info is unavailable, 0/Integer.MAX_VALUE is used
    private final int minKeySize;
    private final int maxKeySize;

    // SecureRandom instance, if specified in init
    private SecureRandom random;

    P11KeyPairGenerator(Token token, String algorithm, long mechanism)
            throws PKCS11Exception {
        super();
        int minKeyLen = 0;
        int maxKeyLen = Integer.MAX_VALUE;
        try {
            CK_MECHANISM_INFO mechInfo = token.getMechanismInfo(mechanism);
            if (mechInfo != null) {
                minKeyLen = mechInfo.iMinKeySize;
                maxKeyLen = mechInfo.iMaxKeySize;
            }
        } catch (PKCS11Exception p11e) {
            // Should never happen
            throw new ProviderException
                        ("Unexpected error while getting mechanism info", p11e);
        }
        // set default key sizes and apply our own algorithm-specific limits
        // override lower limit to disallow unsecure keys being generated
        // override upper limit to deter DOS attack
        if (algorithm.equals("EC")) {
            keySize = DEF_EC_KEY_SIZE;
            if (minKeyLen < 112) {
                minKeyLen = 112;
            }
            if (maxKeyLen > 2048) {
                maxKeyLen = 2048;
            }
        } else {
            if (algorithm.equals("DSA")) {
                keySize = DEF_DSA_KEY_SIZE;
            } else if (algorithm.equals("RSA")) {
                keySize = DEF_RSA_KEY_SIZE;
                if (maxKeyLen > 64 * 1024) {
                    maxKeyLen = 64 * 1024;
                }
            } else {
                keySize = DEF_DH_KEY_SIZE;
            }
            if (minKeyLen < 512) {
                minKeyLen = 512;
            }
        }

        // auto-adjust default keysize in case it's out-of-range
        if (keySize < minKeyLen) {
            keySize = minKeyLen;
        }
        if (keySize > maxKeyLen) {
            keySize = maxKeyLen;
        }
        this.token = token;
        this.algorithm = algorithm;
        this.mechanism = mechanism;
        this.minKeySize = minKeyLen;
        this.maxKeySize = maxKeyLen;
        initialize(keySize, null);
    }

    // see JCA spec
    @Override
    public void initialize(int keySize, SecureRandom random) {
        token.ensureValid();
        try {
            checkKeySize(keySize, null);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidParameterException(e.getMessage());
        }
        this.params = null;
        if (algorithm.equals("EC")) {
            params = P11ECKeyFactory.getECParameterSpec(keySize);
            if (params == null) {
                throw new InvalidParameterException(
                    "No EC parameters available for key size "
                    + keySize + " bits");
            }
        }
        this.keySize = keySize;
        this.random = random;
    }

    // see JCA spec
    @Override
    public void initialize(AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidAlgorithmParameterException {
        token.ensureValid();
        int tmpKeySize;
        if (algorithm.equals("DH")) {
            if (params instanceof DHParameterSpec == false) {
                throw new InvalidAlgorithmParameterException
                        ("DHParameterSpec required for Diffie-Hellman");
            }
            DHParameterSpec dhParams = (DHParameterSpec) params;
            tmpKeySize = dhParams.getP().bitLength();
            checkKeySize(tmpKeySize, dhParams);
            // XXX sanity check params
        } else if (algorithm.equals("RSA")) {
            if (params instanceof RSAKeyGenParameterSpec == false) {
                throw new InvalidAlgorithmParameterException
                        ("RSAKeyGenParameterSpec required for RSA");
            }
            RSAKeyGenParameterSpec rsaParams =
                (RSAKeyGenParameterSpec) params;
            tmpKeySize = rsaParams.getKeysize();
            checkKeySize(tmpKeySize, rsaParams);
            // override the supplied params to null
            params = null;
            this.rsaPublicExponent = rsaParams.getPublicExponent();
            // XXX sanity check params
        } else if (algorithm.equals("DSA")) {
            if (params instanceof DSAParameterSpec == false) {
                throw new InvalidAlgorithmParameterException
                        ("DSAParameterSpec required for DSA");
            }
            DSAParameterSpec dsaParams = (DSAParameterSpec) params;
            tmpKeySize = dsaParams.getP().bitLength();
            checkKeySize(tmpKeySize, dsaParams);
            // XXX sanity check params
        } else if (algorithm.equals("EC")) {
            ECParameterSpec ecParams;
            if (params instanceof ECParameterSpec) {
                ecParams = P11ECKeyFactory.getECParameterSpec(
                    (ECParameterSpec)params);
                if (ecParams == null) {
                    throw new InvalidAlgorithmParameterException
                        ("Unsupported curve: " + params);
                }
            } else if (params instanceof ECGenParameterSpec) {
                String name = ((ECGenParameterSpec) params).getName();
                ecParams = P11ECKeyFactory.getECParameterSpec(name);
                if (ecParams == null) {
                    throw new InvalidAlgorithmParameterException
                        ("Unknown curve name: " + name);
                }
                // override the supplied params with the derived one
                params = ecParams;
            } else {
                throw new InvalidAlgorithmParameterException
                    ("ECParameterSpec or ECGenParameterSpec required for EC");
            }
            tmpKeySize = ecParams.getCurve().getField().getFieldSize();
            checkKeySize(tmpKeySize, ecParams);
        } else {
            throw new ProviderException("Unknown algorithm: " + algorithm);
        }
        this.keySize = tmpKeySize;
        this.params = params;
        this.random = random;
    }

    private void checkKeySize(int keySize, AlgorithmParameterSpec params)
        throws InvalidAlgorithmParameterException {
        if (keySize <= 0) {
            throw new InvalidAlgorithmParameterException
                    ("key size must be positive, got " + keySize);
        }
        // check native range first
        if (keySize < minKeySize) {
            throw new InvalidAlgorithmParameterException(algorithm +
                " key must be at least " + minKeySize + " bits. " +
                "The specific key size " + keySize + " is not supported");
        }
        if (keySize > maxKeySize) {
            throw new InvalidAlgorithmParameterException(algorithm +
                " key must be at most " + maxKeySize + " bits. " +
                "The specific key size " + keySize + " is not supported");
        }

        // check our own algorithm-specific limits also
        if (algorithm.equals("EC")) {
            if (keySize < 112) {
                throw new InvalidAlgorithmParameterException(
                    "EC key size must be at least 112 bit. " +
                    "The specific key size " + keySize + " is not supported");
            }
            if (keySize > 2048) {
                // sanity check, nobody really wants keys this large
                throw new InvalidAlgorithmParameterException(
                    "EC key size must be at most 2048 bit. " +
                    "The specific key size " + keySize + " is not supported");
            }
        } else {
            // RSA, DH, DSA
            if (keySize < 512) {
                throw new InvalidAlgorithmParameterException(algorithm +
                    " key size must be at least 512 bit. " +
                    "The specific key size " + keySize + " is not supported");
            }
            if (algorithm.equals("RSA")) {
                BigInteger tmpExponent = rsaPublicExponent;
                if (params != null) {
                    tmpExponent =
                        ((RSAKeyGenParameterSpec)params).getPublicExponent();
                }
                try {
                    RSAKeyFactory.checkKeyLengths(keySize, tmpExponent,
                        minKeySize, maxKeySize);
                } catch (InvalidKeyException e) {
                    throw new InvalidAlgorithmParameterException(e);
                }
            } else if (algorithm.equals("DH")) {
                if (params != null) {   // initialized with specified parameters
                    // sanity check, nobody really wants keys this large
                    if (keySize > 64 * 1024) {
                        throw new InvalidAlgorithmParameterException(
                            "DH key size must be at most 65536 bit. " +
                            "The specific key size " +
                            keySize + " is not supported");
                    }
                } else {        // default parameters will be used.
                    // Range is based on the values in
                    // sun.security.provider.ParameterCache class.
                    if ((keySize > 8192) || (keySize < 512) ||
                            ((keySize & 0x3f) != 0)) {
                        throw new InvalidAlgorithmParameterException(
                            "DH key size must be multiple of 64, and can " +
                            "only range from 512 to 8192 (inclusive). " +
                            "The specific key size " +
                            keySize + " is not supported");
                    }

                    DHParameterSpec cache =
                            ParameterCache.getCachedDHParameterSpec(keySize);
                    // Except 2048 and 3072, not yet support generation of
                    // parameters bigger than 1024 bits.
                    if ((cache == null) && (keySize > 1024)) {
                        throw new InvalidAlgorithmParameterException(
                                "Unsupported " + keySize +
                                "-bit DH parameter generation");
                    }
                }
            } else {
                // this restriction is in the spec for DSA
                if ((keySize != 3072) && (keySize != 2048) &&
                        ((keySize > 1024) || ((keySize & 0x3f) != 0))) {
                    throw new InvalidAlgorithmParameterException(
                        "DSA key must be multiples of 64 if less than " +
                        "1024 bits, or 2048, 3072 bits. " +
                        "The specific key size " +
                        keySize + " is not supported");
                }
            }
        }
    }

    // see JCA spec
    @Override
    public KeyPair generateKeyPair() {
        token.ensureValid();
        CK_ATTRIBUTE[] publicKeyTemplate;
        CK_ATTRIBUTE[] privateKeyTemplate;
        long keyType;
        if (algorithm.equals("RSA")) {
            keyType = CKK_RSA;
            publicKeyTemplate = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_MODULUS_BITS, keySize),
                new CK_ATTRIBUTE(CKA_PUBLIC_EXPONENT, rsaPublicExponent),
            };
            privateKeyTemplate = new CK_ATTRIBUTE[] {
                // empty
            };
        } else if (algorithm.equals("DSA")) {
            keyType = CKK_DSA;
            DSAParameterSpec dsaParams;
            if (params == null) {
                try {
                    dsaParams = ParameterCache.getDSAParameterSpec
                                                    (keySize, random);
                } catch (GeneralSecurityException e) {
                    throw new ProviderException
                            ("Could not generate DSA parameters", e);
                }
            } else {
                dsaParams = (DSAParameterSpec)params;
            }
            publicKeyTemplate = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_PRIME, dsaParams.getP()),
                new CK_ATTRIBUTE(CKA_SUBPRIME, dsaParams.getQ()),
                new CK_ATTRIBUTE(CKA_BASE, dsaParams.getG()),
            };
            privateKeyTemplate = new CK_ATTRIBUTE[] {
                // empty
            };
        } else if (algorithm.equals("DH")) {
            keyType = CKK_DH;
            DHParameterSpec dhParams;
            int privateBits;
            if (params == null) {
                try {
                    dhParams = ParameterCache.getDHParameterSpec
                                                    (keySize, random);
                } catch (GeneralSecurityException e) {
                    throw new ProviderException
                            ("Could not generate DH parameters", e);
                }
                privateBits = 0;
            } else {
                dhParams = (DHParameterSpec)params;
                privateBits = dhParams.getL();
            }
            if (privateBits <= 0) {
                // XXX find better defaults
                privateBits = (keySize >= 1024) ? 768 : 512;
            }
            publicKeyTemplate = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_PRIME, dhParams.getP()),
                new CK_ATTRIBUTE(CKA_BASE, dhParams.getG())
            };
            privateKeyTemplate = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_VALUE_BITS, privateBits),
            };
        } else if (algorithm.equals("EC")) {
            keyType = CKK_EC;
            byte[] encodedParams =
                    P11ECKeyFactory.encodeParameters((ECParameterSpec)params);
            publicKeyTemplate = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_EC_PARAMS, encodedParams),
            };
            privateKeyTemplate = new CK_ATTRIBUTE[] {
                // empty
            };
        } else {
            throw new ProviderException("Unknown algorithm: " + algorithm);
        }
        Session session = null;
        try {
            session = token.getObjSession();
            publicKeyTemplate = token.getAttributes
                (O_GENERATE, CKO_PUBLIC_KEY, keyType, publicKeyTemplate);
            privateKeyTemplate = token.getAttributes
                (O_GENERATE, CKO_PRIVATE_KEY, keyType, privateKeyTemplate);
            long[] keyIDs = token.p11.C_GenerateKeyPair
                (session.id(), new CK_MECHANISM(mechanism),
                publicKeyTemplate, privateKeyTemplate);
            PublicKey publicKey = P11Key.publicKey
                (session, keyIDs[0], algorithm, keySize, publicKeyTemplate);
            PrivateKey privateKey = P11Key.privateKey
                (session, keyIDs[1], algorithm, keySize, privateKeyTemplate);
            return new KeyPair(publicKey, privateKey);
        } catch (PKCS11Exception e) {
            throw new ProviderException(e);
        } finally {
            token.releaseSession(session);
        }
    }
}
