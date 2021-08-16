/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.security.interfaces.ECPublicKey;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.*;

import static sun.security.pkcs11.TemplateManager.*;
import sun.security.pkcs11.wrapper.*;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;

/**
 * KeyAgreement implementation for ECDH.
 *
 * @author  Andreas Sterbenz
 * @since   1.6
 */
final class P11ECDHKeyAgreement extends KeyAgreementSpi {

    // token instance
    private final Token token;

    // algorithm name
    private final String algorithm;

    // mechanism id
    private final long mechanism;

    // private key, if initialized
    private P11Key privateKey;

    // encoded public point, non-null between doPhase() and generateSecret() only
    private byte[] publicValue;

    // length of the secret to be derived
    private int secretLen;

    P11ECDHKeyAgreement(Token token, String algorithm, long mechanism) {
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
        privateKey = P11KeyFactory.convertKey(token, key, "EC");
        publicValue = null;
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
        if (lastPhase == false) {
            throw new IllegalStateException
                ("Only two party agreement supported, lastPhase must be true");
        }
        if (key instanceof ECPublicKey == false) {
            throw new InvalidKeyException
                ("Key must be a PublicKey with algorithm EC");
        }
        ECPublicKey ecKey = (ECPublicKey)key;
        int keyLenBits = ecKey.getParams().getCurve().getField().getFieldSize();
        secretLen = (keyLenBits + 7) >> 3;
        publicValue = P11ECKeyFactory.getEncodedPublicValue(ecKey);
        return null;
    }

    // see JCE spec
    protected byte[] engineGenerateSecret() throws IllegalStateException {
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
            CK_ECDH1_DERIVE_PARAMS ckParams =
                    new CK_ECDH1_DERIVE_PARAMS(CKD_NULL, null, publicValue);
            attributes = token.getAttributes
                (O_GENERATE, CKO_SECRET_KEY, CKK_GENERIC_SECRET, attributes);
            long keyID = token.p11.C_DeriveKey(session.id(),
                    new CK_MECHANISM(mechanism, ckParams), privKeyID,
                    attributes);
            attributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_VALUE)
            };
            token.p11.C_GetAttributeValue(session.id(), keyID, attributes);
            byte[] secret = attributes[0].getByteArray();
            token.p11.C_DestroyObject(session.id(), keyID);
            return secret;
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
        if (algorithm == null) {
            throw new NoSuchAlgorithmException("Algorithm must not be null");
        }
        if (algorithm.equals("TlsPremasterSecret") == false) {
            throw new NoSuchAlgorithmException
                ("Only supported for algorithm TlsPremasterSecret");
        }
        return nativeGenerateSecret(algorithm);
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
            CK_ECDH1_DERIVE_PARAMS ckParams =
                    new CK_ECDH1_DERIVE_PARAMS(CKD_NULL, null, publicValue);
            attributes = token.getAttributes
                (O_GENERATE, CKO_SECRET_KEY, keyType, attributes);
            long keyID = token.p11.C_DeriveKey(session.id(),
                    new CK_MECHANISM(mechanism, ckParams), privKeyID,
                    attributes);
            CK_ATTRIBUTE[] lenAttributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_VALUE_LEN),
            };
            token.p11.C_GetAttributeValue(session.id(), keyID, lenAttributes);
            int keyLen = (int)lenAttributes[0].getLong();
            SecretKey key = P11Key.secretKey
                        (session, keyID, algorithm, keyLen << 3, attributes);
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
