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

package sun.security.pkcs11;

import java.security.*;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.*;

import static sun.security.pkcs11.TemplateManager.*;
import sun.security.pkcs11.wrapper.*;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;

/**
 * KeyGenerator implementation class. This class currently supports
 * DES, DESede, AES, ARCFOUR, Blowfish, Hmac using MD5, SHA, SHA-2 family
 * (SHA-224, SHA-256, SHA-384, SHA-512, SHA-512/224, SHA-512/256), and SHA-3
 * family (SHA3-224, SHA3-256, SHA3-384, SHA3-512) of digests.
 *
 * @author  Andreas Sterbenz
 * @since   1.5
 */
final class P11KeyGenerator extends KeyGeneratorSpi {

    // token instance
    private final Token token;

    // algorithm name
    private final String algorithm;

    // mechanism id
    private long mechanism;

    // raw key size in bits, e.g. 64 for DES. Always valid.
    private int keySize;

    // bits of entropy in the key, e.g. 56 for DES. Always valid.
    private int significantKeySize;

    // keyType (CKK_*), needed for TemplateManager call only.
    private long keyType;

    // for determining if both 112 and 168 bits of DESede key lengths
    // are supported.
    private boolean supportBothKeySizes;

    // for determining if the specified key size is valid
    private final CK_MECHANISM_INFO range;

    // utility method for query the native key sizes and enforcing the
    // java-specific lower limit; returned values are in bits
    private static CK_MECHANISM_INFO getSupportedRange(Token token,
        long mech) throws ProviderException {
        // No need to query for fix-length algorithms
        if (mech == CKM_DES_KEY_GEN || mech == CKM_DES2_KEY_GEN ||
            mech == CKM_DES3_KEY_GEN) {
            return null;
        }

        // Explicitly disallow keys shorter than 40-bits for security
        int lower = 40;
        int upper = Integer.MAX_VALUE;
        try {
            CK_MECHANISM_INFO info = token.getMechanismInfo(mech);
            if (info != null) {
                boolean isBytes = ((mech != CKM_GENERIC_SECRET_KEY_GEN
                        && mech != CKM_RC4_KEY_GEN) || info.iMinKeySize < 8);
                lower = Math.max(lower, (isBytes?
                        Math.multiplyExact(info.iMinKeySize, 8) :
                        info.iMinKeySize));
                // NSS CKM_GENERIC_SECRET_KEY_GEN mech info is not precise;
                // its upper limit is too low and does not match its impl
                if (mech == CKM_GENERIC_SECRET_KEY_GEN &&
                    info.iMaxKeySize <= 32) {
                    // ignore and leave upper limit at MAX_VALUE;
                } else if (info.iMaxKeySize != Integer.MAX_VALUE) {
                    upper = (isBytes?
                            Math.multiplyExact(info.iMaxKeySize, 8) :
                            info.iMaxKeySize);
                }
            }
        } catch (PKCS11Exception p11e) {
            // Should never happen
            throw new ProviderException("Cannot retrieve mechanism info", p11e);
        }
        return new CK_MECHANISM_INFO(lower, upper, 0 /* flags not used */);
    }

    /**
     * Utility method for checking if the specified key size is valid
     * and within the supported range. Return the significant key size
     * upon successful validation.
     * @param keyGenMech the PKCS#11 key generation mechanism.
     * @param keySize the to-be-checked key size for this mechanism.
     * @param token token which provides this mechanism.
     * @return the significant key size (in bits) corresponding to the
     * specified key size.
     * @throws InvalidParameterException if the specified key size is invalid.
     * @throws ProviderException if this mechanism isn't supported by SunPKCS11
     * or underlying native impl.
     */
    // called by P11SecretKeyFactory to check key size
    static int checkKeySize(long keyGenMech, int keySize, Token token)
        throws InvalidAlgorithmParameterException, ProviderException {
        CK_MECHANISM_INFO range = getSupportedRange(token, keyGenMech);
        return checkKeySize(keyGenMech, keySize, range);
    }

    private static int checkKeySize(long keyGenMech, int keySize,
        CK_MECHANISM_INFO range) throws InvalidAlgorithmParameterException {
        int sigKeySize;
        switch ((int)keyGenMech) {
            case (int)CKM_DES_KEY_GEN:
                if ((keySize != 64) && (keySize != 56)) {
                    throw new InvalidAlgorithmParameterException
                            ("DES key length must be 56 bits");
                }
                sigKeySize = 56;
                break;
            case (int)CKM_DES2_KEY_GEN:
            case (int)CKM_DES3_KEY_GEN:
                if ((keySize == 112) || (keySize == 128)) {
                    sigKeySize = 112;
                } else if ((keySize == 168) || (keySize == 192)) {
                    sigKeySize = 168;
                } else {
                    throw new InvalidAlgorithmParameterException
                            ("DESede key length must be 112, or 168 bits");
                }
                break;
            default:
                // Handle all variable-key-length algorithms here
                if (range != null && keySize < range.iMinKeySize
                    || keySize > range.iMaxKeySize) {
                    throw new InvalidAlgorithmParameterException
                        ("Key length must be between " + range.iMinKeySize +
                        " and " + range.iMaxKeySize + " bits");
                }
                if (keyGenMech == CKM_AES_KEY_GEN) {
                    if ((keySize != 128) && (keySize != 192) &&
                        (keySize != 256)) {
                        throw new InvalidAlgorithmParameterException
                            ("AES key length must be 128, 192, or 256 bits");
                    }
                }
                sigKeySize = keySize;
        }
        return sigKeySize;
    }

    // check the supplied keysize (in bits) and adjust it based on the given
    // range
    private static int adjustKeySize(int ks, CK_MECHANISM_INFO mi) {
        // adjust to fit within the supported range
        if (mi != null) {
            if (ks < mi.iMinKeySize) {
                ks = mi.iMinKeySize;
            } else if (ks > mi.iMaxKeySize) {
                ks = mi.iMaxKeySize;
            }
        }
        return ks;
    }

    P11KeyGenerator(Token token, String algorithm, long mechanism)
            throws PKCS11Exception {
        super();
        this.token = token;
        this.algorithm = algorithm;
        this.mechanism = mechanism;

        if (this.mechanism == CKM_DES3_KEY_GEN) {
            /* Given the current lookup order specified in SunPKCS11.java,
               if CKM_DES2_KEY_GEN is used to construct this object, it
               means that CKM_DES3_KEY_GEN is disabled or unsupported.
            */
            supportBothKeySizes =
                (token.provider.config.isEnabled(CKM_DES2_KEY_GEN) &&
                 (token.getMechanismInfo(CKM_DES2_KEY_GEN) != null));
        }
        this.range = getSupportedRange(token, mechanism);
        setDefault();
    }

    // set default keysize and keyType
    private void setDefault() {
        significantKeySize = -1;
        switch ((int)mechanism) {
        case (int)CKM_DES_KEY_GEN:
            keySize = 64;
            keyType = CKK_DES;
            significantKeySize = 56;
            break;
        case (int)CKM_DES2_KEY_GEN:
            keySize = 128;
            keyType = CKK_DES2;
            significantKeySize = 112;
            break;
        case (int)CKM_DES3_KEY_GEN:
            keySize = 192;
            keyType = CKK_DES3;
            significantKeySize = 168;
            break;
        case (int)CKM_AES_KEY_GEN:
            keySize = adjustKeySize(128, range);
            keyType = CKK_AES;
            break;
        case (int)CKM_RC4_KEY_GEN:
            keySize = adjustKeySize(128, range);
            keyType = CKK_RC4;
            break;
        case (int)CKM_BLOWFISH_KEY_GEN:
            keySize = adjustKeySize(128, range);
            keyType = CKK_BLOWFISH;
            break;
        case (int)CKM_CHACHA20_KEY_GEN:
            keySize = 256;
            keyType = CKK_CHACHA20;
            break;
        case (int)CKM_SHA_1_KEY_GEN:
            keySize = adjustKeySize(160, range);
            keyType = CKK_SHA_1_HMAC;
            break;
        case (int)CKM_SHA224_KEY_GEN:
            keySize = adjustKeySize(224, range);
            keyType = CKK_SHA224_HMAC;
            break;
        case (int)CKM_SHA256_KEY_GEN:
            keySize = adjustKeySize(256, range);
            keyType = CKK_SHA256_HMAC;
            break;
        case (int)CKM_SHA384_KEY_GEN:
            keySize = adjustKeySize(384, range);
            keyType = CKK_SHA384_HMAC;
            break;
        case (int)CKM_SHA512_KEY_GEN:
            keySize = adjustKeySize(512, range);
            keyType = CKK_SHA512_HMAC;
            break;
        case (int)CKM_SHA512_224_KEY_GEN:
            keySize = adjustKeySize(224, range);
            keyType = CKK_SHA512_224_HMAC;
            break;
        case (int)CKM_SHA512_256_KEY_GEN:
            keySize = adjustKeySize(256, range);
            keyType = CKK_SHA512_256_HMAC;
            break;
        case (int)CKM_SHA3_224_KEY_GEN:
            keySize = adjustKeySize(224, range);
            keyType = CKK_SHA3_224_HMAC;
            break;
        case (int)CKM_SHA3_256_KEY_GEN:
            keySize = adjustKeySize(256, range);
            keyType = CKK_SHA3_256_HMAC;
            break;
        case (int)CKM_SHA3_384_KEY_GEN:
            keySize = adjustKeySize(384, range);
            keyType = CKK_SHA3_384_HMAC;
            break;
        case (int)CKM_SHA3_512_KEY_GEN:
            keySize = adjustKeySize(512, range);
            keyType = CKK_SHA3_512_HMAC;
            break;
        case (int)CKM_GENERIC_SECRET_KEY_GEN:
            if (algorithm.startsWith("Hmac")) {
                String digest = algorithm.substring(4);
                keySize = adjustKeySize(switch (digest) {
                    case "MD5" -> 512;
                    case "SHA1" -> 160;
                    case "SHA224", "SHA512/224", "SHA3-224" -> 224;
                    case "SHA256", "SHA512/256", "SHA3-256" -> 256;
                    case "SHA384", "SHA3-384" -> 384;
                    case "SHA512", "SHA3-512" -> 512;
                    default -> {
                        throw new ProviderException("Unsupported algorithm " +
                            algorithm);
                    }
                }, range);
            } else {
                throw new ProviderException("Unsupported algorithm " +
                        algorithm);
            }
            keyType = CKK_GENERIC_SECRET;
            break;
        default:
            throw new ProviderException("Unknown mechanism " + mechanism);
        }
        if (significantKeySize == -1) {
            significantKeySize = keySize;
        }
    }

    // see JCE spec
    protected void engineInit(SecureRandom random) {
        token.ensureValid();
        setDefault();
    }

    // see JCE spec
    protected void engineInit(AlgorithmParameterSpec params,
            SecureRandom random) throws InvalidAlgorithmParameterException {
        throw new InvalidAlgorithmParameterException
                ("AlgorithmParameterSpec not supported");
    }

    // see JCE spec
    protected void engineInit(int keySize, SecureRandom random) {
        token.ensureValid();
        int newSignificantKeySize;
        try {
            newSignificantKeySize = checkKeySize(mechanism, keySize, range);
        } catch (InvalidAlgorithmParameterException iape) {
            throw (InvalidParameterException)
                    (new InvalidParameterException().initCause(iape));
        }
        if ((mechanism == CKM_DES2_KEY_GEN) ||
            (mechanism == CKM_DES3_KEY_GEN))  {
            long newMechanism = (newSignificantKeySize == 112 ?
                CKM_DES2_KEY_GEN : CKM_DES3_KEY_GEN);
            if (mechanism != newMechanism) {
                if (supportBothKeySizes) {
                    mechanism = newMechanism;
                    // Adjust keyType to reflect the mechanism change
                    keyType = (mechanism == CKM_DES2_KEY_GEN ?
                        CKK_DES2 : CKK_DES3);
                } else {
                    throw new InvalidParameterException
                            ("Only " + significantKeySize +
                             "-bit DESede is supported");
                }
            }
        }
        this.keySize = keySize;
        this.significantKeySize = newSignificantKeySize;
    }

    // see JCE spec
    protected SecretKey engineGenerateKey() {
        Session session = null;
        try {
            session = token.getObjSession();
            CK_ATTRIBUTE[] attributes;

            switch ((int)mechanism) {
            case (int)CKM_DES_KEY_GEN:
            case (int)CKM_DES2_KEY_GEN:
            case (int)CKM_DES3_KEY_GEN:
                // fixed length, do not specify CKA_VALUE_LEN
                attributes = new CK_ATTRIBUTE[] {
                    new CK_ATTRIBUTE(CKA_CLASS, CKO_SECRET_KEY),
                };
                break;
            default:
                attributes = new CK_ATTRIBUTE[] {
                    new CK_ATTRIBUTE(CKA_CLASS, CKO_SECRET_KEY),
                    new CK_ATTRIBUTE(CKA_VALUE_LEN, keySize >> 3),
                };
                break;
            }
            attributes = token.getAttributes
                (O_GENERATE, CKO_SECRET_KEY, keyType, attributes);
            long keyID = token.p11.C_GenerateKey
                (session.id(), new CK_MECHANISM(mechanism), attributes);
            return P11Key.secretKey
                (session, keyID, algorithm, significantKeySize, attributes);
        } catch (PKCS11Exception e) {
            throw new ProviderException("Could not generate key", e);
        } finally {
            token.releaseSession(session);
        }
    }
}
