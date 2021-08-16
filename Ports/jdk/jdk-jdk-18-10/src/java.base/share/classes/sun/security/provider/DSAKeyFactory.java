/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.provider;

import java.security.Key;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.KeyFactorySpi;
import java.security.InvalidKeyException;
import java.security.interfaces.DSAParams;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.KeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

/**
 * This class implements the DSA key factory of the Sun provider.
 *
 * @author Jan Luehe
 *
 *
 * @since 1.2
 */

public class DSAKeyFactory extends KeyFactorySpi {
    /**
     * Generates a public key object from the provided key specification
     * (key material).
     *
     * @param keySpec the specification (key material) of the public key
     *
     * @return the public key
     *
     * @exception InvalidKeySpecException if the given key specification
     * is inappropriate for this key factory to produce a public key.
     */
    protected PublicKey engineGeneratePublic(KeySpec keySpec)
    throws InvalidKeySpecException {
        try {
            if (keySpec instanceof DSAPublicKeySpec) {
                DSAPublicKeySpec dsaPubKeySpec = (DSAPublicKeySpec)keySpec;
                return new DSAPublicKeyImpl(dsaPubKeySpec.getY(),
                                    dsaPubKeySpec.getP(),
                                    dsaPubKeySpec.getQ(),
                                    dsaPubKeySpec.getG());
            } else if (keySpec instanceof X509EncodedKeySpec) {
                return new DSAPublicKeyImpl
                    (((X509EncodedKeySpec)keySpec).getEncoded());
            } else {
                throw new InvalidKeySpecException
                    ("Inappropriate key specification");
            }
        } catch (InvalidKeyException e) {
            throw new InvalidKeySpecException
                ("Inappropriate key specification: " + e.getMessage());
        }
    }

    /**
     * Generates a private key object from the provided key specification
     * (key material).
     *
     * @param keySpec the specification (key material) of the private key
     *
     * @return the private key
     *
     * @exception InvalidKeySpecException if the given key specification
     * is inappropriate for this key factory to produce a private key.
     */
    protected PrivateKey engineGeneratePrivate(KeySpec keySpec)
            throws InvalidKeySpecException {
        try {
            if (keySpec instanceof DSAPrivateKeySpec) {
                DSAPrivateKeySpec dsaPrivKeySpec = (DSAPrivateKeySpec)keySpec;
                return new DSAPrivateKey(dsaPrivKeySpec.getX(),
                                         dsaPrivKeySpec.getP(),
                                         dsaPrivKeySpec.getQ(),
                                         dsaPrivKeySpec.getG());

            } else if (keySpec instanceof PKCS8EncodedKeySpec) {
                byte[] encoded = ((PKCS8EncodedKeySpec)keySpec).getEncoded();
                try {
                    return new DSAPrivateKey(encoded);
                } finally {
                    Arrays.fill(encoded, (byte) 0);
                }
            } else {
                throw new InvalidKeySpecException
                    ("Inappropriate key specification");
            }
        } catch (InvalidKeyException e) {
            throw new InvalidKeySpecException
                ("Inappropriate key specification: " + e.getMessage());
        }
    }

    /**
     * Returns a specification (key material) of the given key object
     * in the requested format.
     *
     * @param key the key
     *
     * @param keySpec the requested format in which the key material shall be
     * returned
     *
     * @return the underlying key specification (key material) in the
     * requested format
     *
     * @exception InvalidKeySpecException if the requested key specification is
     * inappropriate for the given key, or the given key cannot be processed
     * (e.g., the given key has an unrecognized algorithm or format).
     */
    protected <T extends KeySpec>
        T engineGetKeySpec(Key key, Class<T> keySpec)
    throws InvalidKeySpecException {

        DSAParams params;

        try {

            if (key instanceof java.security.interfaces.DSAPublicKey) {

                // Determine valid key specs
                Class<?> dsaPubKeySpec = Class.forName
                    ("java.security.spec.DSAPublicKeySpec");
                Class<?> x509KeySpec = Class.forName
                    ("java.security.spec.X509EncodedKeySpec");

                if (keySpec.isAssignableFrom(dsaPubKeySpec)) {
                    java.security.interfaces.DSAPublicKey dsaPubKey
                        = (java.security.interfaces.DSAPublicKey)key;
                    params = dsaPubKey.getParams();
                    return keySpec.cast(new DSAPublicKeySpec(dsaPubKey.getY(),
                                                             params.getP(),
                                                             params.getQ(),
                                                             params.getG()));

                } else if (keySpec.isAssignableFrom(x509KeySpec)) {
                    return keySpec.cast(new X509EncodedKeySpec(key.getEncoded()));

                } else {
                    throw new InvalidKeySpecException
                        ("Inappropriate key specification");
                }

            } else if (key instanceof java.security.interfaces.DSAPrivateKey) {

                // Determine valid key specs
                Class<?> dsaPrivKeySpec = Class.forName
                    ("java.security.spec.DSAPrivateKeySpec");
                Class<?> pkcs8KeySpec = Class.forName
                    ("java.security.spec.PKCS8EncodedKeySpec");

                if (keySpec.isAssignableFrom(dsaPrivKeySpec)) {
                    java.security.interfaces.DSAPrivateKey dsaPrivKey
                        = (java.security.interfaces.DSAPrivateKey)key;
                    params = dsaPrivKey.getParams();
                    return keySpec.cast(new DSAPrivateKeySpec(dsaPrivKey.getX(),
                                                              params.getP(),
                                                              params.getQ(),
                                                              params.getG()));

                } else if (keySpec.isAssignableFrom(pkcs8KeySpec)) {
                    byte[] encoded = key.getEncoded();
                    try {
                        return keySpec.cast(new PKCS8EncodedKeySpec(encoded));
                    } finally {
                        Arrays.fill(encoded, (byte)0);
                    }
                } else {
                    throw new InvalidKeySpecException
                        ("Inappropriate key specification");
                }

            } else {
                throw new InvalidKeySpecException("Inappropriate key type");
            }

        } catch (ClassNotFoundException e) {
            throw new InvalidKeySpecException
                ("Unsupported key specification: " + e.getMessage());
        }
    }

    /**
     * Translates a key object, whose provider may be unknown or potentially
     * untrusted, into a corresponding key object of this key factory.
     *
     * @param key the key whose provider is unknown or untrusted
     *
     * @return the translated key
     *
     * @exception InvalidKeyException if the given key cannot be processed by
     * this key factory.
     */
    protected Key engineTranslateKey(Key key) throws InvalidKeyException {

        try {

            if (key instanceof java.security.interfaces.DSAPublicKey) {
                // Check if key originates from this factory
                if (key instanceof sun.security.provider.DSAPublicKey) {
                    return key;
                }
                // Convert key to spec
                DSAPublicKeySpec dsaPubKeySpec
                    = engineGetKeySpec(key, DSAPublicKeySpec.class);
                // Create key from spec, and return it
                return engineGeneratePublic(dsaPubKeySpec);

            } else if (key instanceof java.security.interfaces.DSAPrivateKey) {
                // Check if key originates from this factory
                if (key instanceof sun.security.provider.DSAPrivateKey) {
                    return key;
                }
                // Convert key to spec
                DSAPrivateKeySpec dsaPrivKeySpec
                    = engineGetKeySpec(key, DSAPrivateKeySpec.class);
                // Create key from spec, and return it
                return engineGeneratePrivate(dsaPrivKeySpec);

            } else {
                throw new InvalidKeyException("Wrong algorithm type");
            }

        } catch (InvalidKeySpecException e) {
            throw new InvalidKeyException("Cannot translate key: "
                                          + e.getMessage());
        }
    }
}
