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

package sun.security.ec;

import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.util.Arrays;

/**
 * KeyFactory for EC keys. Keys must be instances of PublicKey or PrivateKey
 * and getAlgorithm() must return "EC". For such keys, it supports conversion
 * between the following:
 *
 * For public keys:
 *  . PublicKey with an X.509 encoding
 *  . ECPublicKey
 *  . ECPublicKeySpec
 *  . X509EncodedKeySpec
 *
 * For private keys:
 *  . PrivateKey with a PKCS#8 encoding
 *  . ECPrivateKey
 *  . ECPrivateKeySpec
 *  . PKCS8EncodedKeySpec
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 */
public final class ECKeyFactory extends KeyFactorySpi {

    // Used by translateKey()
    private static KeyFactory instance;

    private static KeyFactory getInstance() {
        if (instance == null) {
            try {
                instance = KeyFactory.getInstance("EC", "SunEC");
            } catch (NoSuchProviderException e) {
                throw new RuntimeException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        return instance;
    }

    public ECKeyFactory() {
        // empty
    }

    /**
     * Static method to convert Key into a useable instance of
     * ECPublicKey or ECPrivateKey. Check the key and convert it
     * to a Sun key if necessary. If the key is not an EC key
     * or cannot be used, throw an InvalidKeyException.
     *
     * The difference between this method and engineTranslateKey() is that
     * we do not convert keys of other providers that are already an
     * instance of ECPublicKey or ECPrivateKey.
     *
     * To be used by future Java ECDSA and ECDH implementations.
     */
    public static ECKey toECKey(Key key) throws InvalidKeyException {
        if (key instanceof ECKey) {
            ECKey ecKey = (ECKey)key;
            checkKey(ecKey);
            return ecKey;
        } else {
            /*
             * We don't call the engineTranslateKey method directly
             * because KeyFactory.translateKey adds code to loop through
             * all key factories.
             */
            return (ECKey)getInstance().translateKey(key);
        }
    }

    /**
     * Check that the given EC key is valid.
     */
    private static void checkKey(ECKey key) throws InvalidKeyException {
        // check for subinterfaces, omit additional checks for our keys
        if (key instanceof ECPublicKey) {
            if (key instanceof ECPublicKeyImpl) {
                return;
            }
        } else if (key instanceof ECPrivateKey) {
            if (key instanceof ECPrivateKeyImpl) {
                return;
            }
        } else {
            throw new InvalidKeyException("Neither a public nor a private key");
        }
        // ECKey does not extend Key, so we need to do a cast
        String keyAlg = ((Key)key).getAlgorithm();
        if (keyAlg.equals("EC") == false) {
            throw new InvalidKeyException("Not an EC key: " + keyAlg);
        }
        // XXX further sanity checks about whether this key uses supported
        // fields, point formats, etc. would go here
    }

    /**
     * Translate an EC key into a Sun EC key. If conversion is
     * not possible, throw an InvalidKeyException.
     * See also JCA doc.
     */
    protected Key engineTranslateKey(Key key) throws InvalidKeyException {
        if (key == null) {
            throw new InvalidKeyException("Key must not be null");
        }
        String keyAlg = key.getAlgorithm();
        if (keyAlg.equals("EC") == false) {
            throw new InvalidKeyException("Not an EC key: " + keyAlg);
        }
        if (key instanceof PublicKey) {
            return implTranslatePublicKey((PublicKey)key);
        } else if (key instanceof PrivateKey) {
            return implTranslatePrivateKey((PrivateKey)key);
        } else {
            throw new InvalidKeyException("Neither a public nor a private key");
        }
    }

    // see JCA doc
    protected PublicKey engineGeneratePublic(KeySpec keySpec)
            throws InvalidKeySpecException {
        try {
            return implGeneratePublic(keySpec);
        } catch (InvalidKeySpecException e) {
            throw e;
        } catch (GeneralSecurityException e) {
            throw new InvalidKeySpecException(e);
        }
    }

    // see JCA doc
    protected PrivateKey engineGeneratePrivate(KeySpec keySpec)
            throws InvalidKeySpecException {
        try {
            return implGeneratePrivate(keySpec);
        } catch (InvalidKeySpecException e) {
            throw e;
        } catch (GeneralSecurityException e) {
            throw new InvalidKeySpecException(e);
        }
    }

    // internal implementation of translateKey() for public keys. See JCA doc
    private PublicKey implTranslatePublicKey(PublicKey key)
            throws InvalidKeyException {
        if (key instanceof ECPublicKey) {
            if (key instanceof ECPublicKeyImpl) {
                return key;
            }
            ECPublicKey ecKey = (ECPublicKey)key;
            return new ECPublicKeyImpl(
                ecKey.getW(),
                ecKey.getParams()
            );
        } else if ("X.509".equals(key.getFormat())) {
            byte[] encoded = key.getEncoded();
            return new ECPublicKeyImpl(encoded);
        } else {
            throw new InvalidKeyException("Public keys must be instance "
                + "of ECPublicKey or have X.509 encoding");
        }
    }

    // internal implementation of translateKey() for private keys. See JCA doc
    private PrivateKey implTranslatePrivateKey(PrivateKey key)
            throws InvalidKeyException {
        if (key instanceof ECPrivateKey) {
            if (key instanceof ECPrivateKeyImpl) {
                return key;
            }
            ECPrivateKey ecKey = (ECPrivateKey)key;
            return new ECPrivateKeyImpl(
                ecKey.getS(),
                ecKey.getParams()
            );
        } else if ("PKCS#8".equals(key.getFormat())) {
            byte[] encoded = key.getEncoded();
            try {
                return new ECPrivateKeyImpl(encoded);
            } finally {
                Arrays.fill(encoded, (byte)0);
            }
        } else {
            throw new InvalidKeyException("Private keys must be instance "
                + "of ECPrivateKey or have PKCS#8 encoding");
        }
    }

    // internal implementation of generatePublic. See JCA doc
    private PublicKey implGeneratePublic(KeySpec keySpec)
            throws GeneralSecurityException {
        if (keySpec instanceof X509EncodedKeySpec) {
            X509EncodedKeySpec x509Spec = (X509EncodedKeySpec)keySpec;
            return new ECPublicKeyImpl(x509Spec.getEncoded());
        } else if (keySpec instanceof ECPublicKeySpec) {
            ECPublicKeySpec ecSpec = (ECPublicKeySpec)keySpec;
            return new ECPublicKeyImpl(
                ecSpec.getW(),
                ecSpec.getParams()
            );
        } else {
            throw new InvalidKeySpecException("Only ECPublicKeySpec "
                + "and X509EncodedKeySpec supported for EC public keys");
        }
    }

    // internal implementation of generatePrivate. See JCA doc
    private PrivateKey implGeneratePrivate(KeySpec keySpec)
            throws GeneralSecurityException {
        if (keySpec instanceof PKCS8EncodedKeySpec) {
            PKCS8EncodedKeySpec pkcsSpec = (PKCS8EncodedKeySpec)keySpec;
            byte[] encoded = pkcsSpec.getEncoded();
            try {
                return new ECPrivateKeyImpl(encoded);
            } finally {
                Arrays.fill(encoded, (byte) 0);
            }
        } else if (keySpec instanceof ECPrivateKeySpec) {
            ECPrivateKeySpec ecSpec = (ECPrivateKeySpec)keySpec;
            return new ECPrivateKeyImpl(ecSpec.getS(), ecSpec.getParams());
        } else {
            throw new InvalidKeySpecException("Only ECPrivateKeySpec "
                + "and PKCS8EncodedKeySpec supported for EC private keys");
        }
    }

    protected <T extends KeySpec> T engineGetKeySpec(Key key, Class<T> keySpec)
            throws InvalidKeySpecException {
        try {
            // convert key to one of our keys
            // this also verifies that the key is a valid EC key and ensures
            // that the encoding is X.509/PKCS#8 for public/private keys
            key = engineTranslateKey(key);
        } catch (InvalidKeyException e) {
            throw new InvalidKeySpecException(e);
        }
        if (key instanceof ECPublicKey) {
            ECPublicKey ecKey = (ECPublicKey)key;
            if (keySpec.isAssignableFrom(ECPublicKeySpec.class)) {
                return keySpec.cast(new ECPublicKeySpec(
                    ecKey.getW(),
                    ecKey.getParams()
                ));
            } else if (keySpec.isAssignableFrom(X509EncodedKeySpec.class)) {
                return keySpec.cast(new X509EncodedKeySpec(key.getEncoded()));
            } else {
                throw new InvalidKeySpecException
                        ("KeySpec must be ECPublicKeySpec or "
                        + "X509EncodedKeySpec for EC public keys");
            }
        } else if (key instanceof ECPrivateKey) {
            if (keySpec.isAssignableFrom(PKCS8EncodedKeySpec.class)) {
                byte[] encoded = key.getEncoded();
                try {
                    return keySpec.cast(new PKCS8EncodedKeySpec(encoded));
                } finally {
                    Arrays.fill(encoded, (byte)0);
                }
            } else if (keySpec.isAssignableFrom(ECPrivateKeySpec.class)) {
                ECPrivateKey ecKey = (ECPrivateKey)key;
                return keySpec.cast(new ECPrivateKeySpec(
                    ecKey.getS(),
                    ecKey.getParams()
                ));
            } else {
                throw new InvalidKeySpecException
                        ("KeySpec must be ECPrivateKeySpec or "
                        + "PKCS8EncodedKeySpec for EC private keys");
            }
        } else {
            // should not occur, caught in engineTranslateKey()
            throw new InvalidKeySpecException("Neither public nor private key");
        }
    }
}
