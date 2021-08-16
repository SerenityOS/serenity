/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ec.ed;

//import java.security.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGeneratorSpi;
import java.security.NoSuchAlgorithmException;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.EdECPoint;
import java.security.spec.NamedParameterSpec;
import java.util.Arrays;

import sun.security.jca.JCAUtil;
import sun.security.util.SecurityProviderConstants;

/**
 * Key pair generator for the EdDSA signature algorithm.
 */
public class EdDSAKeyPairGenerator extends KeyPairGeneratorSpi {

    private SecureRandom random = null;
    private EdDSAOperations ops = null;
    private EdDSAParameters lockedParams = null;

    public EdDSAKeyPairGenerator() {
        initialize(SecurityProviderConstants.DEF_ED_KEY_SIZE, null);
    }

    private EdDSAKeyPairGenerator(NamedParameterSpec paramSpec) {
        tryInitialize(paramSpec);
        lockedParams = ops.getParameters();
    }

    private void tryInitialize(NamedParameterSpec paramSpec) {
        try {
            initialize(paramSpec, null);
        } catch (InvalidAlgorithmParameterException ex) {
            String name = paramSpec.getName();
            throw new ProviderException(name + " not supported");
        }
    }

    @Override
    public void initialize(int keySize, SecureRandom random) {

        EdDSAParameters params = EdDSAParameters.getBySize(
            InvalidParameterException::new, keySize);

        initializeImpl(params, random);
    }

    @Override
    public void initialize(AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidAlgorithmParameterException {

        EdDSAParameters edParams = EdDSAParameters.get(
            InvalidAlgorithmParameterException::new, params);

        try {
            initializeImpl(edParams, random);
        } catch (InvalidParameterException e) {
            throw new InvalidAlgorithmParameterException(e);
        }
    }

    private void initializeImpl(EdDSAParameters params, SecureRandom random) {

        if (lockedParams != null && lockedParams != params) {
            throw new InvalidParameterException("Parameters must be " +
                lockedParams.getName());
        }

        try {
            this.ops = new EdDSAOperations(params);
        } catch (NoSuchAlgorithmException ex) {
            throw new ProviderException(ex);
        }
        this.random = random == null ? JCAUtil.getSecureRandom() : random;
    }


    @Override
    public KeyPair generateKeyPair() {

        byte[] privateKey = ops.generatePrivate(random);
        EdECPoint publicKey = ops.computePublic(privateKey);

        try {
            return new KeyPair(
                new EdDSAPublicKeyImpl(ops.getParameters(), publicKey),
                new EdDSAPrivateKeyImpl(ops.getParameters(), privateKey)
            );
        } catch (InvalidKeyException ex) {
            throw new ProviderException(ex);
        } finally {
            Arrays.fill(privateKey, (byte)0);
        }
    }

    public static class Ed25519 extends EdDSAKeyPairGenerator {

        public Ed25519() {
            super(NamedParameterSpec.ED25519);
        }
    }

    public static class Ed448 extends EdDSAKeyPairGenerator {

        public Ed448() {
            super(NamedParameterSpec.ED448);
        }
    }
}
