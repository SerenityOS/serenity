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
import java.security.interfaces.*;
import java.security.spec.*;

import static sun.security.pkcs11.TemplateManager.*;
import sun.security.pkcs11.wrapper.*;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;

/**
 * DSA KeyFactory implementation.
 *
 * @author  Andreas Sterbenz
 * @since   1.5
 */
final class P11DSAKeyFactory extends P11KeyFactory {

    P11DSAKeyFactory(Token token, String algorithm) {
        super(token, algorithm);
    }

    PublicKey implTranslatePublicKey(PublicKey key) throws InvalidKeyException {
        try {
            if (key instanceof DSAPublicKey) {
                DSAPublicKey dsaKey = (DSAPublicKey)key;
                DSAParams params = dsaKey.getParams();
                return generatePublic(
                    dsaKey.getY(),
                    params.getP(),
                    params.getQ(),
                    params.getG()
                );
            } else if ("X.509".equals(key.getFormat())) {
                // let Sun provider parse for us, then recurse
                byte[] encoded = key.getEncoded();
                key = new sun.security.provider.DSAPublicKey(encoded);
                return implTranslatePublicKey(key);
            } else {
                throw new InvalidKeyException("PublicKey must be instance "
                        + "of DSAPublicKey or have X.509 encoding");
            }
        } catch (PKCS11Exception e) {
            throw new InvalidKeyException("Could not create DSA public key", e);
        }
    }

    PrivateKey implTranslatePrivateKey(PrivateKey key)
            throws InvalidKeyException {
        try {
            if (key instanceof DSAPrivateKey) {
                DSAPrivateKey dsaKey = (DSAPrivateKey)key;
                DSAParams params = dsaKey.getParams();
                return generatePrivate(
                    dsaKey.getX(),
                    params.getP(),
                    params.getQ(),
                    params.getG()
                );
            } else if ("PKCS#8".equals(key.getFormat())) {
                // let Sun provider parse for us, then recurse
                byte[] encoded = key.getEncoded();
                key = new sun.security.provider.DSAPrivateKey(encoded);
                return implTranslatePrivateKey(key);
            } else {
                throw new InvalidKeyException("PrivateKey must be instance "
                        + "of DSAPrivateKey or have PKCS#8 encoding");
            }
        } catch (PKCS11Exception e) {
            throw new InvalidKeyException("Could not create DSA private key", e);
        }
    }

    // see JCA spec
    protected PublicKey engineGeneratePublic(KeySpec keySpec)
            throws InvalidKeySpecException {
        token.ensureValid();
        if (keySpec instanceof X509EncodedKeySpec) {
            try {
                byte[] encoded = ((X509EncodedKeySpec)keySpec).getEncoded();
                PublicKey key = new sun.security.provider.DSAPublicKey(encoded);
                return implTranslatePublicKey(key);
            } catch (InvalidKeyException e) {
                throw new InvalidKeySpecException
                        ("Could not create DSA public key", e);
            }
        }
        if (keySpec instanceof DSAPublicKeySpec == false) {
            throw new InvalidKeySpecException("Only DSAPublicKeySpec and "
                + "X509EncodedKeySpec supported for DSA public keys");
        }
        try {
            DSAPublicKeySpec ds = (DSAPublicKeySpec)keySpec;
            return generatePublic(
                ds.getY(),
                ds.getP(),
                ds.getQ(),
                ds.getG()
            );
        } catch (PKCS11Exception e) {
            throw new InvalidKeySpecException
                ("Could not create DSA public key", e);
        }
    }

    // see JCA spec
    protected PrivateKey engineGeneratePrivate(KeySpec keySpec)
            throws InvalidKeySpecException {
        token.ensureValid();
        if (keySpec instanceof PKCS8EncodedKeySpec) {
            try {
                byte[] encoded = ((PKCS8EncodedKeySpec)keySpec).getEncoded();
                PrivateKey key = new sun.security.provider.DSAPrivateKey(encoded);
                return implTranslatePrivateKey(key);
            } catch (GeneralSecurityException e) {
                throw new InvalidKeySpecException
                        ("Could not create DSA private key", e);
            }
        }
        if (keySpec instanceof DSAPrivateKeySpec == false) {
            throw new InvalidKeySpecException("Only DSAPrivateKeySpec and "
                + "PKCS8EncodedKeySpec supported for DSA private keys");
        }
        try {
            DSAPrivateKeySpec ds = (DSAPrivateKeySpec)keySpec;
            return generatePrivate(
                ds.getX(),
                ds.getP(),
                ds.getQ(),
                ds.getG()
            );
        } catch (PKCS11Exception e) {
            throw new InvalidKeySpecException
                ("Could not create DSA private key", e);
        }
    }

    private PublicKey generatePublic(BigInteger y, BigInteger p, BigInteger q,
            BigInteger g) throws PKCS11Exception {
        CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
            new CK_ATTRIBUTE(CKA_CLASS, CKO_PUBLIC_KEY),
            new CK_ATTRIBUTE(CKA_KEY_TYPE, CKK_DSA),
            new CK_ATTRIBUTE(CKA_VALUE, y),
            new CK_ATTRIBUTE(CKA_PRIME, p),
            new CK_ATTRIBUTE(CKA_SUBPRIME, q),
            new CK_ATTRIBUTE(CKA_BASE, g),
        };
        attributes = token.getAttributes
                (O_IMPORT, CKO_PUBLIC_KEY, CKK_DSA, attributes);
        Session session = null;
        try {
            session = token.getObjSession();
            long keyID = token.p11.C_CreateObject(session.id(), attributes);
            return P11Key.publicKey
                (session, keyID, "DSA", p.bitLength(), attributes);
        } finally {
            token.releaseSession(session);
        }
    }

    private PrivateKey generatePrivate(BigInteger x, BigInteger p,
            BigInteger q, BigInteger g) throws PKCS11Exception {
        CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
            new CK_ATTRIBUTE(CKA_CLASS, CKO_PRIVATE_KEY),
            new CK_ATTRIBUTE(CKA_KEY_TYPE, CKK_DSA),
            new CK_ATTRIBUTE(CKA_VALUE, x),
            new CK_ATTRIBUTE(CKA_PRIME, p),
            new CK_ATTRIBUTE(CKA_SUBPRIME, q),
            new CK_ATTRIBUTE(CKA_BASE, g),
        };
        attributes = token.getAttributes
                (O_IMPORT, CKO_PRIVATE_KEY, CKK_DSA, attributes);
        Session session = null;
        try {
            session = token.getObjSession();
            long keyID = token.p11.C_CreateObject(session.id(), attributes);
            return P11Key.privateKey
                (session, keyID, "DSA", p.bitLength(), attributes);
        } finally {
            token.releaseSession(session);
        }
    }

    <T extends KeySpec> T implGetPublicKeySpec(P11Key key, Class<T> keySpec,
            Session[] session) throws PKCS11Exception, InvalidKeySpecException {
        if (keySpec.isAssignableFrom(DSAPublicKeySpec.class)) {
            session[0] = token.getObjSession();
            CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_VALUE),
                new CK_ATTRIBUTE(CKA_PRIME),
                new CK_ATTRIBUTE(CKA_SUBPRIME),
                new CK_ATTRIBUTE(CKA_BASE),
            };
            long keyID = key.getKeyID();
            try {
                token.p11.C_GetAttributeValue(session[0].id(), keyID, attributes);
            } finally {
                key.releaseKeyID();
            }
            KeySpec spec = new DSAPublicKeySpec(
                attributes[0].getBigInteger(),
                attributes[1].getBigInteger(),
                attributes[2].getBigInteger(),
                attributes[3].getBigInteger()
            );
            return keySpec.cast(spec);
        } else { // X.509 handled in superclass
            throw new InvalidKeySpecException("Only DSAPublicKeySpec and "
                + "X509EncodedKeySpec supported for DSA public keys");
        }
    }

    <T extends KeySpec> T implGetPrivateKeySpec(P11Key key, Class<T> keySpec,
            Session[] session) throws PKCS11Exception, InvalidKeySpecException {
        if (keySpec.isAssignableFrom(DSAPrivateKeySpec.class)) {
            session[0] = token.getObjSession();
            CK_ATTRIBUTE[] attributes = new CK_ATTRIBUTE[] {
                new CK_ATTRIBUTE(CKA_VALUE),
                new CK_ATTRIBUTE(CKA_PRIME),
                new CK_ATTRIBUTE(CKA_SUBPRIME),
                new CK_ATTRIBUTE(CKA_BASE),
            };
            long keyID = key.getKeyID();
            try {
                token.p11.C_GetAttributeValue(session[0].id(), keyID, attributes);
            } finally {
                key.releaseKeyID();
            }
            KeySpec spec = new DSAPrivateKeySpec(
                attributes[0].getBigInteger(),
                attributes[1].getBigInteger(),
                attributes[2].getBigInteger(),
                attributes[3].getBigInteger()
            );
            return keySpec.cast(spec);
        } else { // PKCS#8 handled in superclass
            throw new InvalidKeySpecException("Only DSAPrivateKeySpec "
                + "and PKCS8EncodedKeySpec supported for DSA private keys");
        }
    }

    KeyFactory implGetSoftwareFactory() throws GeneralSecurityException {
        return KeyFactory.getInstance("DSA", P11Util.getSunProvider());
    }

}
