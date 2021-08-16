/*
 * Copyright (c) 2006, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.math.BigInteger;

import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;

import static sun.security.pkcs11.TemplateManager.*;
import sun.security.pkcs11.wrapper.*;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;

import sun.security.util.DerValue;
import sun.security.util.ECUtil;

/**
 * EC KeyFactory implementation.
 *
 * @author  Andreas Sterbenz
 * @since   1.6
 */
final class P11ECKeyFactory extends P11KeyFactory {
    private static Provider sunECprovider;

    private static Provider getSunECProvider() {
        if (sunECprovider == null) {
            sunECprovider = Security.getProvider("SunEC");
            if (sunECprovider == null) {
                throw new RuntimeException("Cannot load SunEC provider");
            }
        }

        return sunECprovider;
    }

    P11ECKeyFactory(Token token, String algorithm) {
        super(token, algorithm);
    }

    static ECParameterSpec getECParameterSpec(String name) {
        return ECUtil.getECParameterSpec(getSunECProvider(), name);
    }

    static ECParameterSpec getECParameterSpec(int keySize) {
        return ECUtil.getECParameterSpec(getSunECProvider(), keySize);
    }

    // Check that spec is a known supported curve and convert it to our
    // ECParameterSpec subclass. If not possible, return null.
    static ECParameterSpec getECParameterSpec(ECParameterSpec spec) {
        return ECUtil.getECParameterSpec(getSunECProvider(), spec);
    }

    static ECParameterSpec decodeParameters(byte[] params) throws IOException {
        return ECUtil.getECParameterSpec(getSunECProvider(), params);
    }

    static byte[] encodeParameters(ECParameterSpec params) {
        return ECUtil.encodeECParameterSpec(getSunECProvider(), params);
    }

    static ECPoint decodePoint(byte[] encoded, EllipticCurve curve) throws IOException {
        return ECUtil.decodePoint(encoded, curve);
    }

    // Used by ECDH KeyAgreement
    static byte[] getEncodedPublicValue(PublicKey key) throws InvalidKeyException {
        if (key instanceof ECPublicKey) {
            ECPublicKey ecKey = (ECPublicKey)key;
            ECPoint w = ecKey.getW();
            ECParameterSpec params = ecKey.getParams();
            return ECUtil.encodePoint(w, params.getCurve());
        } else {
            // should never occur
            throw new InvalidKeyException
                ("Key class not yet supported: " + key.getClass().getName());
        }
    }

    PublicKey implTranslatePublicKey(PublicKey key) throws InvalidKeyException {
        try {
            if (key instanceof ECPublicKey) {
                ECPublicKey ecKey = (ECPublicKey)key;
                return generatePublic(
                    ecKey.getW(),
                    ecKey.getParams()
                );
            } else if ("X.509".equals(key.getFormat())) {
                // let Sun provider parse for us, then recurse
                byte[] encoded = key.getEncoded();

                try {
                    key = ECUtil.decodeX509ECPublicKey(encoded);
                } catch (InvalidKeySpecException ikse) {
                    throw new InvalidKeyException(ikse);
                }

                return implTranslatePublicKey(key);
            } else {
                throw new InvalidKeyException("PublicKey must be instance "
                        + "of ECPublicKey or have X.509 encoding");
            }
        } catch (PKCS11Exception e) {
            throw new InvalidKeyException("Could not create EC public key", e);
        }
    }

    PrivateKey implTranslatePrivateKey(PrivateKey key)
            throws InvalidKeyException {
        try {
            if (key instanceof ECPrivateKey) {
                ECPrivateKey ecKey = (ECPrivateKey)key;
                return generatePrivate(
                    ecKey.getS(),
                    ecKey.getParams()
                );
            } else if ("PKCS#8".equals(key.getFormat())) {
                // let Sun provider parse for us, then recurse
                byte[] encoded = key.getEncoded();

                try {
                    key = ECUtil.decodePKCS8ECPrivateKey(encoded);
                } catch (InvalidKeySpecException ikse) {
                    throw new InvalidKeyException(ikse);
                }

                return implTranslatePrivateKey(key);
            } else {
                throw new InvalidKeyException("PrivateKey must be instance "
                        + "of ECPrivateKey or have PKCS#8 encoding");
            }
        } catch (PKCS11Exception e) {
            throw new InvalidKeyException("Could not create EC private key", e);
        }
    }

    // see JCA spec
    protected PublicKey engineGeneratePublic(KeySpec keySpec)
            throws InvalidKeySpecException {
        token.ensureValid();
        if (keySpec instanceof X509EncodedKeySpec) {
            try {
                byte[] encoded = ((X509EncodedKeySpec)keySpec).getEncoded();
                PublicKey key = ECUtil.decodeX509ECPublicKey(encoded);
                return implTranslatePublicKey(key);
            } catch (InvalidKeyException e) {
                throw new InvalidKeySpecException
                        ("Could not create EC public key", e);
            }
        }
        if (keySpec instanceof ECPublicKeySpec == false) {
            throw new InvalidKeySpecException("Only ECPublicKeySpec and "
                + "X509EncodedKeySpec supported for EC public keys");
        }
        try {
            ECPublicKeySpec ec = (ECPublicKeySpec)keySpec;
            return generatePublic(
                ec.getW(),
                ec.getParams()
            );
        } catch (PKCS11Exception e) {
            throw new InvalidKeySpecException
                ("Could not create EC public key", e);
        }
    }

    // see JCA spec
    protected PrivateKey engineGeneratePrivate(KeySpec keySpec)
            throws InvalidKeySpecException {
        token.ensureValid();
        if (keySpec instanceof PKCS8EncodedKeySpec) {
            try {
                byte[] encoded = ((PKCS8EncodedKeySpec)keySpec).getEncoded();
                PrivateKey key = ECUtil.decodePKCS8ECPrivateKey(encoded);
                return implTranslatePrivateKey(key);
            } catch (GeneralSecurityException e) {
                throw new InvalidKeySpecException
                        ("Could not create EC private key", e);
            }
        }
        if (keySpec instanceof ECPrivateKeySpec == false) {
            throw new InvalidKeySpecException("Only ECPrivateKeySpec and "
                + "PKCS8EncodedKeySpec supported for EC private keys");
        }
        try {
            ECPrivateKeySpec ec = (ECPrivateKeySpec)keySpec;
            return generatePrivate(
                ec.getS(),
                ec.getParams()
            );
        } catch (PKCS11Exception e) {
            throw new InvalidKeySpecException
                ("Could not create EC private key", e);
        }
    }

    private PublicKey generatePublic(ECPoint point, ECParameterSpec params)
            throws PKCS11Exception {
        byte[] encodedParams =
            ECUtil.encodeECParameterSpec(getSunECProvider(), params);
        byte[] encodedPoint =
            ECUtil.encodePoint(point, params.getCurve());

        // Check whether the X9.63 encoding of an EC point shall be wrapped
        // in an ASN.1 OCTET STRING
        if (!token.config.getUseEcX963Encoding()) {
            try {
                encodedPoint =
                    new DerValue(DerValue.tag_OctetString, encodedPoint)
                        .toByteArray();
            } catch (IOException e) {
                throw new
                    IllegalArgumentException("Could not DER encode point", e);
            }
        }

        CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
            new CK_ATTRIBUTE(CKA_CLASS, CKO_PUBLIC_KEY),
            new CK_ATTRIBUTE(CKA_KEY_TYPE, CKK_EC),
            new CK_ATTRIBUTE(CKA_EC_POINT, encodedPoint),
            new CK_ATTRIBUTE(CKA_EC_PARAMS, encodedParams),
        };
        attributes = token.getAttributes
                (O_IMPORT, CKO_PUBLIC_KEY, CKK_EC, attributes);
        Session session = null;
        try {
            session = token.getObjSession();
            long keyID = token.p11.C_CreateObject(session.id(), attributes);
            return P11Key.publicKey
                (session, keyID, "EC", params.getCurve().getField().getFieldSize(), attributes);
        } finally {
            token.releaseSession(session);
        }
    }

    private PrivateKey generatePrivate(BigInteger s, ECParameterSpec params)
            throws PKCS11Exception {
        byte[] encodedParams =
            ECUtil.encodeECParameterSpec(getSunECProvider(), params);
        CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
            new CK_ATTRIBUTE(CKA_CLASS, CKO_PRIVATE_KEY),
            new CK_ATTRIBUTE(CKA_KEY_TYPE, CKK_EC),
            new CK_ATTRIBUTE(CKA_VALUE, s),
            new CK_ATTRIBUTE(CKA_EC_PARAMS, encodedParams),
        };
        attributes = token.getAttributes
                (O_IMPORT, CKO_PRIVATE_KEY, CKK_EC, attributes);
        Session session = null;
        try {
            session = token.getObjSession();
            long keyID = token.p11.C_CreateObject(session.id(), attributes);
            return P11Key.privateKey
                (session, keyID, "EC", params.getCurve().getField().getFieldSize(), attributes);
        } finally {
            token.releaseSession(session);
        }
    }

    <T extends KeySpec> T implGetPublicKeySpec(P11Key key, Class<T> keySpec,
            Session[] session) throws PKCS11Exception, InvalidKeySpecException {
        if (keySpec.isAssignableFrom(ECPublicKeySpec.class)) {
            session[0] = token.getObjSession();
            CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_EC_POINT),
                new CK_ATTRIBUTE(CKA_EC_PARAMS),
            };
            long keyID = key.getKeyID();
            try {
                token.p11.C_GetAttributeValue(session[0].id(), keyID, attributes);
                ECParameterSpec params = decodeParameters(attributes[1].getByteArray());
                ECPoint point;

                if (!token.config.getUseEcX963Encoding()) {
                    point = decodePoint(new DerValue(attributes[0].getByteArray()).getOctetString(), params.getCurve());
                } else {
                    point = decodePoint(attributes[0].getByteArray(), params.getCurve());
                }
                return keySpec.cast(new ECPublicKeySpec(point, params));
            } catch (IOException e) {
                throw new InvalidKeySpecException("Could not parse key", e);
            } finally {
                key.releaseKeyID();
            }
        } else { // X.509 handled in superclass
            throw new InvalidKeySpecException("Only ECPublicKeySpec and "
                + "X509EncodedKeySpec supported for EC public keys");
        }
    }

    <T extends KeySpec> T implGetPrivateKeySpec(P11Key key, Class<T> keySpec,
            Session[] session) throws PKCS11Exception, InvalidKeySpecException {
        if (keySpec.isAssignableFrom(ECPrivateKeySpec.class)) {
            session[0] = token.getObjSession();
            CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_VALUE),
                new CK_ATTRIBUTE(CKA_EC_PARAMS),
            };
            long keyID = key.getKeyID();
            try {
                token.p11.C_GetAttributeValue(session[0].id(), keyID, attributes);
                ECParameterSpec params = decodeParameters(attributes[1].getByteArray());
                return keySpec.cast(
                    new ECPrivateKeySpec(attributes[0].getBigInteger(), params));
            } catch (IOException e) {
                throw new InvalidKeySpecException("Could not parse key", e);
            } finally {
                key.releaseKeyID();
            }
        } else { // PKCS#8 handled in superclass
            throw new InvalidKeySpecException("Only ECPrivateKeySpec "
                + "and PKCS8EncodedKeySpec supported for EC private keys");
        }
    }

    KeyFactory implGetSoftwareFactory() throws GeneralSecurityException {
        return KeyFactory.getInstance("EC", getSunECProvider());
    }

}
