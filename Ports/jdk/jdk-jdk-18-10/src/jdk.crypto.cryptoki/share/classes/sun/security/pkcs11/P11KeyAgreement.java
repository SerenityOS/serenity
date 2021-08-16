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

import java.math.BigInteger;

import java.security.*;
import java.security.spec.*;

import javax.crypto.*;
import javax.crypto.interfaces.*;
import javax.crypto.spec.*;

import static sun.security.pkcs11.TemplateManager.*;
import sun.security.pkcs11.wrapper.*;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;
import sun.security.util.KeyUtil;

/**
 * KeyAgreement implementation class. This class currently supports
 * DH.
 *
 * @author  Andreas Sterbenz
 * @since   1.5
 */
final class P11KeyAgreement extends KeyAgreementSpi {

    // token instance
    private final Token token;

    // algorithm name
    private final String algorithm;

    // mechanism id
    private final long mechanism;

    // private key, if initialized
    private P11Key privateKey;

    // other sides public value ("y"), if doPhase() already called
    private BigInteger publicValue;

    // length of the secret to be derived
    private int secretLen;

    // KeyAgreement from SunJCE as fallback for > 2 party agreement
    private KeyAgreement multiPartyAgreement;

    private static class AllowKDF {

        private static final boolean VALUE = getValue();

        @SuppressWarnings("removal")
        private static boolean getValue() {
            return AccessController.doPrivileged(
                (PrivilegedAction<Boolean>)
                () -> Boolean.getBoolean("jdk.crypto.KeyAgreement.legacyKDF"));
        }
    }

    P11KeyAgreement(Token token, String algorithm, long mechanism) {
        super();
        this.token = token;
        this.algorithm = algorithm;
        this.mechanism = mechanism;
    }

    // see JCE spec
    protected void engineInit(Key key, SecureRandom random)
            throws InvalidKeyException {
        if (key instanceof PrivateKey == false) {
            throw new InvalidKeyException
                        ("Key must be instance of PrivateKey");
        }
        privateKey = P11KeyFactory.convertKey(token, key, algorithm);
        publicValue = null;
        multiPartyAgreement = null;
    }

    // see JCE spec
    protected void engineInit(Key key, AlgorithmParameterSpec params,
            SecureRandom random) throws InvalidKeyException,
            InvalidAlgorithmParameterException {
        if (params != null) {
            throw new InvalidAlgorithmParameterException
                        ("Parameters not supported");
        }
        engineInit(key, random);
    }

    // see JCE spec
    protected Key engineDoPhase(Key key, boolean lastPhase)
            throws InvalidKeyException, IllegalStateException {
        if (privateKey == null) {
            throw new IllegalStateException("Not initialized");
        }
        if (publicValue != null) {
            throw new IllegalStateException("Phase already executed");
        }
        // PKCS#11 only allows key agreement between 2 parties
        // JCE allows >= 2 parties. To support that case (for compatibility
        // and to pass JCK), fall back to SunJCE in this case.
        // NOTE that we initialize using the P11Key, which will fail if it
        // is sensitive/unextractable. However, this is not an issue in the
        // compatibility configuration, which is all we are targeting here.
        if ((multiPartyAgreement != null) || (lastPhase == false)) {
            if (multiPartyAgreement == null) {
                try {
                    multiPartyAgreement = KeyAgreement.getInstance
                        ("DH", P11Util.getSunJceProvider());
                    multiPartyAgreement.init(privateKey);
                } catch (NoSuchAlgorithmException e) {
                    throw new InvalidKeyException
                        ("Could not initialize multi party agreement", e);
                }
            }
            return multiPartyAgreement.doPhase(key, lastPhase);
        }
        if ((key instanceof PublicKey == false)
                || (key.getAlgorithm().equals(algorithm) == false)) {
            throw new InvalidKeyException
                ("Key must be a PublicKey with algorithm DH");
        }
        BigInteger p, g, y;
        if (key instanceof DHPublicKey) {
            DHPublicKey dhKey = (DHPublicKey)key;

            // validate the Diffie-Hellman public key
            KeyUtil.validate(dhKey);

            y = dhKey.getY();
            DHParameterSpec params = dhKey.getParams();
            p = params.getP();
            g = params.getG();
        } else {
            // normally, DH PublicKeys will always implement DHPublicKey
            // just in case not, attempt conversion
            P11DHKeyFactory kf = new P11DHKeyFactory(token, "DH");
            try {
                DHPublicKeySpec spec = kf.engineGetKeySpec(
                        key, DHPublicKeySpec.class);

                // validate the Diffie-Hellman public key
                KeyUtil.validate(spec);

                y = spec.getY();
                p = spec.getP();
                g = spec.getG();
            } catch (InvalidKeySpecException e) {
                throw new InvalidKeyException("Could not obtain key values", e);
            }
        }
        // if parameters of private key are accessible, verify that
        // they match parameters of public key
        // XXX p and g should always be readable, even if the key is sensitive
        if (privateKey instanceof DHPrivateKey) {
            DHPrivateKey dhKey = (DHPrivateKey)privateKey;
            DHParameterSpec params = dhKey.getParams();
            if ((p.equals(params.getP()) == false)
                                || (g.equals(params.getG()) == false)) {
                throw new InvalidKeyException
                ("PublicKey DH parameters must match PrivateKey DH parameters");
            }
        }
        publicValue = y;
        // length of the secret is length of key
        secretLen = (p.bitLength() + 7) >> 3;
        return null;
    }

    // see JCE spec
    protected byte[] engineGenerateSecret() throws IllegalStateException {
        if (multiPartyAgreement != null) {
            byte[] val = multiPartyAgreement.generateSecret();
            multiPartyAgreement = null;
            return val;
        }
        if ((privateKey == null) || (publicValue == null)) {
            throw new IllegalStateException("Not initialized correctly");
        }
        Session session = null;
        long privKeyID = privateKey.getKeyID();
        try {
            session = token.getOpSession();
            CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_CLASS, CKO_SECRET_KEY),
                new CK_ATTRIBUTE(CKA_KEY_TYPE, CKK_GENERIC_SECRET),
            };
            attributes = token.getAttributes
                (O_GENERATE, CKO_SECRET_KEY, CKK_GENERIC_SECRET, attributes);
            long keyID = token.p11.C_DeriveKey(session.id(),
                    new CK_MECHANISM(mechanism, publicValue), privKeyID,
                    attributes);

            attributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_VALUE)
            };
            token.p11.C_GetAttributeValue(session.id(), keyID, attributes);
            byte[] secret = attributes[0].getByteArray();
            token.p11.C_DestroyObject(session.id(), keyID);
            // Some vendors, e.g. NSS, trim off the leading 0x00 byte(s) from
            // the generated secret. Thus, we need to check the secret length
            // and trim/pad it so the returned value has the same length as
            // the modulus size
            if (secret.length == secretLen) {
                return secret;
            } else {
                if (secret.length > secretLen) {
                    // Shouldn't happen; but check just in case
                    throw new ProviderException("generated secret is out-of-range");
                }
                byte[] newSecret = new byte[secretLen];
                System.arraycopy(secret, 0, newSecret, secretLen - secret.length,
                    secret.length);
                return newSecret;
            }
        } catch (PKCS11Exception e) {
            throw new ProviderException("Could not derive key", e);
        } finally {
            privateKey.releaseKeyID();
            publicValue = null;
            token.releaseSession(session);
        }
    }

    // see JCE spec
    protected int engineGenerateSecret(byte[] sharedSecret, int
            offset) throws IllegalStateException, ShortBufferException {
        if (multiPartyAgreement != null) {
            int n = multiPartyAgreement.generateSecret(sharedSecret, offset);
            multiPartyAgreement = null;
            return n;
        }
        if (offset + secretLen > sharedSecret.length) {
            throw new ShortBufferException("Need " + secretLen
                + " bytes, only " + (sharedSecret.length - offset) + " available");
        }
        byte[] secret = engineGenerateSecret();
        System.arraycopy(secret, 0, sharedSecret, offset, secret.length);
        return secret.length;
    }

    // see JCE spec
    protected SecretKey engineGenerateSecret(String algorithm)
            throws IllegalStateException, NoSuchAlgorithmException,
            InvalidKeyException {
        if (multiPartyAgreement != null) {
            SecretKey key = multiPartyAgreement.generateSecret(algorithm);
            multiPartyAgreement = null;
            return key;
        }
        if (algorithm == null) {
            throw new NoSuchAlgorithmException("Algorithm must not be null");
        }

        if (algorithm.equals("TlsPremasterSecret")) {
            // For now, only perform native derivation for TlsPremasterSecret
            // as that is required for FIPS compliance.
            // For other algorithms, there are unresolved issues regarding
            // how this should work in JCE plus a Solaris truncation bug.
            // (bug not yet filed).
            return nativeGenerateSecret(algorithm);
        }

        if (!algorithm.equalsIgnoreCase("TlsPremasterSecret") &&
            !AllowKDF.VALUE) {

            throw new NoSuchAlgorithmException("Unsupported secret key "
                                               + "algorithm: " + algorithm);
        }

        byte[] secret = engineGenerateSecret();
        // Maintain compatibility for SunJCE:
        // verify secret length is sensible for algorithm / truncate
        // return generated key itself if possible
        int keyLen;
        if (algorithm.equalsIgnoreCase("DES")) {
            keyLen = 8;
        } else if (algorithm.equalsIgnoreCase("DESede")) {
            keyLen = 24;
        } else if (algorithm.equalsIgnoreCase("Blowfish")) {
            keyLen = Math.min(56, secret.length);
        } else if (algorithm.equalsIgnoreCase("TlsPremasterSecret")) {
            keyLen = secret.length;
        } else {
            throw new NoSuchAlgorithmException
                ("Unknown algorithm " + algorithm);
        }
        if (secret.length < keyLen) {
            throw new InvalidKeyException("Secret too short");
        }
        if (algorithm.equalsIgnoreCase("DES") ||
            algorithm.equalsIgnoreCase("DESede")) {
                for (int i = 0; i < keyLen; i+=8) {
                    P11SecretKeyFactory.fixDESParity(secret, i);
                }
        }
        return new SecretKeySpec(secret, 0, keyLen, algorithm);
    }

    private SecretKey nativeGenerateSecret(String algorithm)
            throws IllegalStateException, NoSuchAlgorithmException,
            InvalidKeyException {
        if ((privateKey == null) || (publicValue == null)) {
            throw new IllegalStateException("Not initialized correctly");
        }
        long keyType = CKK_GENERIC_SECRET;
        Session session = null;
        long privKeyID = privateKey.getKeyID();
        try {
            session = token.getObjSession();
            CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_CLASS, CKO_SECRET_KEY),
                new CK_ATTRIBUTE(CKA_KEY_TYPE, keyType),
            };
            attributes = token.getAttributes
                (O_GENERATE, CKO_SECRET_KEY, keyType, attributes);
            long keyID = token.p11.C_DeriveKey(session.id(),
                    new CK_MECHANISM(mechanism, publicValue), privKeyID,
                    attributes);
            CK_ATTRIBUTE[] lenAttributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_VALUE_LEN),
            };
            token.p11.C_GetAttributeValue(session.id(), keyID, lenAttributes);
            int keyLen = (int)lenAttributes[0].getLong();
            SecretKey key = P11Key.secretKey
                        (session, keyID, algorithm, keyLen << 3, attributes);
            if ("RAW".equals(key.getFormat())) {
                // Workaround for Solaris bug 6318543.
                // Strip leading zeroes ourselves if possible (key not sensitive).
                // This should be removed once the Solaris fix is available
                // as here we always retrieve the CKA_VALUE even for tokens
                // that do not have that bug.
                byte[] keyBytes = key.getEncoded();
                byte[] newBytes = KeyUtil.trimZeroes(keyBytes);
                if (keyBytes != newBytes) {
                    key = new SecretKeySpec(newBytes, algorithm);
                }
            }
            return key;
        } catch (PKCS11Exception e) {
            throw new InvalidKeyException("Could not derive key", e);
        } finally {
            privateKey.releaseKeyID();
            publicValue = null;
            token.releaseSession(session);
        }
    }

}
