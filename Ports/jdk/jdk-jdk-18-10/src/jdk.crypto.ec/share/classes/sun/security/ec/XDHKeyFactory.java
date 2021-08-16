/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.security.KeyFactorySpi;
import java.security.Key;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.InvalidKeyException;
import java.security.ProviderException;
import java.security.interfaces.XECKey;
import java.security.interfaces.XECPrivateKey;
import java.security.interfaces.XECPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.NamedParameterSpec;
import java.security.spec.KeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.XECPublicKeySpec;
import java.security.spec.XECPrivateKeySpec;
import java.util.Arrays;
import java.util.function.Function;

public class XDHKeyFactory extends KeyFactorySpi {

    private XECParameters lockedParams = null;

    XDHKeyFactory() {
        // do nothing
    }

    protected XDHKeyFactory(AlgorithmParameterSpec paramSpec) {
        lockedParams = XECParameters.get(ProviderException::new, paramSpec);
    }

    @Override
    protected Key engineTranslateKey(Key key) throws InvalidKeyException {

        if (key == null) {
            throw new InvalidKeyException("Key must not be null");
        }

        if (key instanceof XECKey) {
            XECKey xecKey = (XECKey) key;
            XECParameters params = XECParameters.get(InvalidKeyException::new,
                xecKey.getParams());
            checkLockedParams(InvalidKeyException::new, params);

            if (xecKey instanceof XECPublicKey) {
                XECPublicKey publicKey = (XECPublicKey) xecKey;
                return new XDHPublicKeyImpl(params, publicKey.getU());
            } else if (xecKey instanceof XECPrivateKey) {
                XECPrivateKey privateKey = (XECPrivateKey) xecKey;
                byte[] scalar = privateKey.getScalar().orElseThrow(
                    () -> new InvalidKeyException("No private key data"));
                return new XDHPrivateKeyImpl(params, scalar);
            } else {
                throw new InvalidKeyException("Unsupported XECKey subclass");
            }
        } else if (key instanceof PublicKey &&
                   key.getFormat().equals("X.509")) {
            XDHPublicKeyImpl result = new XDHPublicKeyImpl(key.getEncoded());
            checkLockedParams(InvalidKeyException::new, result.getParams());
            return result;
        } else if (key instanceof PrivateKey &&
                   key.getFormat().equals("PKCS#8")) {
            byte[] encoded = key.getEncoded();
            try {
                XDHPrivateKeyImpl result = new XDHPrivateKeyImpl(encoded);
                checkLockedParams(InvalidKeyException::new, result.getParams());
                return result;
            } finally {
                Arrays.fill(encoded, (byte)0);
            }
        } else {
            throw new InvalidKeyException("Unsupported key type or format");
        }
    }

    private
    <T extends Throwable>
    void checkLockedParams(Function<String, T> exception,
                           AlgorithmParameterSpec spec) throws T {

        XECParameters params = XECParameters.get(exception, spec);
        checkLockedParams(exception, params);
    }

    private
    <T extends Throwable>
    void checkLockedParams(Function<String, T> exception,
                           XECParameters params) throws T {

        if (lockedParams != null && lockedParams != params) {
            throw exception.apply("Parameters must be " +
                lockedParams.getName());
        }
    }

    @Override
    protected PublicKey engineGeneratePublic(KeySpec keySpec)
        throws InvalidKeySpecException {

        try {
             return generatePublicImpl(keySpec);
        } catch (InvalidKeyException ex) {
            throw new InvalidKeySpecException(ex);
        }
    }

    @Override
    protected PrivateKey engineGeneratePrivate(KeySpec keySpec)
        throws InvalidKeySpecException {

        try {
            return generatePrivateImpl(keySpec);
        } catch (InvalidKeyException ex) {
            throw new InvalidKeySpecException(ex);
        }
    }


    private PublicKey generatePublicImpl(KeySpec keySpec)
        throws InvalidKeyException, InvalidKeySpecException {

        if (keySpec instanceof X509EncodedKeySpec) {
            X509EncodedKeySpec x509Spec = (X509EncodedKeySpec) keySpec;
            XDHPublicKeyImpl result =
                new XDHPublicKeyImpl(x509Spec.getEncoded());
            checkLockedParams(InvalidKeySpecException::new,
                result.getParams());
            return result;
        } else if (keySpec instanceof XECPublicKeySpec) {
            XECPublicKeySpec publicKeySpec = (XECPublicKeySpec) keySpec;
            XECParameters params = XECParameters.get(
                InvalidKeySpecException::new, publicKeySpec.getParams());
            checkLockedParams(InvalidKeySpecException::new, params);
            return new XDHPublicKeyImpl(params, publicKeySpec.getU());
        } else {
            throw new InvalidKeySpecException(
                "Only X509EncodedKeySpec and XECPublicKeySpec are supported");
        }
    }

    private PrivateKey generatePrivateImpl(KeySpec keySpec)
        throws InvalidKeyException, InvalidKeySpecException {

        if (keySpec instanceof PKCS8EncodedKeySpec) {
            PKCS8EncodedKeySpec pkcsSpec = (PKCS8EncodedKeySpec) keySpec;
            byte[] encoded = pkcsSpec.getEncoded();
            try {
                XDHPrivateKeyImpl result = new XDHPrivateKeyImpl(encoded);
                checkLockedParams(InvalidKeySpecException::new,
                        result.getParams());
                return result;
            } finally {
                Arrays.fill(encoded, (byte) 0);
            }
        } else if (keySpec instanceof XECPrivateKeySpec) {
            XECPrivateKeySpec privateKeySpec = (XECPrivateKeySpec) keySpec;
            XECParameters params = XECParameters.get(
                InvalidKeySpecException::new, privateKeySpec.getParams());
            checkLockedParams(InvalidKeySpecException::new, params);
            byte[] scalar = privateKeySpec.getScalar();
            try {
                return new XDHPrivateKeyImpl(params, scalar);
            } finally {
                Arrays.fill(scalar, (byte)0);
            }
        } else {
            throw new InvalidKeySpecException(
                "Only PKCS8EncodedKeySpec and XECPrivateKeySpec supported");
        }
    }

    protected <T extends KeySpec> T engineGetKeySpec(Key key, Class<T> keySpec)
            throws InvalidKeySpecException {

        if (key instanceof XECPublicKey) {
            checkLockedParams(InvalidKeySpecException::new,
                ((XECPublicKey) key).getParams());

            if (keySpec.isAssignableFrom(X509EncodedKeySpec.class)) {
                if (!key.getFormat().equals("X.509")) {
                    throw new InvalidKeySpecException("Format is not X.509");
                }
                return keySpec.cast(new X509EncodedKeySpec(key.getEncoded()));
            } else if (keySpec.isAssignableFrom(XECPublicKeySpec.class)) {
                XECPublicKey xecKey = (XECPublicKey) key;
                return keySpec.cast(
                    new XECPublicKeySpec(xecKey.getParams(), xecKey.getU()));
            } else {
                throw new InvalidKeySpecException(
                    "KeySpec must be X509EncodedKeySpec or XECPublicKeySpec");
            }
        } else if (key instanceof XECPrivateKey) {
            checkLockedParams(InvalidKeySpecException::new,
                ((XECPrivateKey) key).getParams());

            if (keySpec.isAssignableFrom(PKCS8EncodedKeySpec.class)) {
                if (!key.getFormat().equals("PKCS#8")) {
                    throw new InvalidKeySpecException("Format is not PKCS#8");
                }
                byte[] encoded = key.getEncoded();
                try {
                    return keySpec.cast(new PKCS8EncodedKeySpec(encoded));
                } finally {
                    Arrays.fill(encoded, (byte)0);
                }
            } else if (keySpec.isAssignableFrom(XECPrivateKeySpec.class)) {
                XECPrivateKey xecKey = (XECPrivateKey) key;
                byte[] scalar = xecKey.getScalar().orElseThrow(
                    () -> new InvalidKeySpecException("No private key value")
                );
                try {
                    return keySpec.cast(
                            new XECPrivateKeySpec(xecKey.getParams(), scalar));
                } finally {
                    Arrays.fill(scalar, (byte)0);
                }
            } else {
                throw new InvalidKeySpecException
                ("KeySpec must be PKCS8EncodedKeySpec or XECPrivateKeySpec");
            }
        } else {
            throw new InvalidKeySpecException("Unsupported key type");
        }
    }

    static class X25519 extends XDHKeyFactory {

        public X25519() {
            super(NamedParameterSpec.X25519);
        }
    }

    static class X448 extends XDHKeyFactory {

        public X448() {
            super(NamedParameterSpec.X448);
        }
    }
}
