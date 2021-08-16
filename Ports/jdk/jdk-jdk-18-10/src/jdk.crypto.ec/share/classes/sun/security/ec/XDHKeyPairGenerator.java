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

import java.math.BigInteger;
import java.security.KeyPairGeneratorSpi;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.NamedParameterSpec;
import java.util.Arrays;

import sun.security.jca.JCAUtil;

/**
 * Key pair generator for the XDH key agreement algorithm.
 */
public class XDHKeyPairGenerator extends KeyPairGeneratorSpi {

    private static final NamedParameterSpec DEFAULT_PARAM_SPEC
        = NamedParameterSpec.X25519;

    private SecureRandom random = null;
    private XECOperations ops = null;
    private XECParameters lockedParams = null;

    XDHKeyPairGenerator() {
        tryInitialize(DEFAULT_PARAM_SPEC);
    }

    private XDHKeyPairGenerator(NamedParameterSpec paramSpec) {
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

        XECParameters params = XECParameters.getBySize(
            InvalidParameterException::new, keySize);

        initializeImpl(params, random);
    }

    @Override
    public void initialize(AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidAlgorithmParameterException {

        XECParameters xecParams = XECParameters.get(
            InvalidAlgorithmParameterException::new, params);

        initializeImpl(xecParams, random);
    }

    private void initializeImpl(XECParameters params, SecureRandom random) {

        if (lockedParams != null && lockedParams != params) {
            throw new InvalidParameterException("Parameters must be " +
                lockedParams.getName());
        }

        this.ops = new XECOperations(params);
        this.random = random == null ? JCAUtil.getSecureRandom() : random;
    }


    @Override
    public KeyPair generateKeyPair() {

        byte[] privateKey = ops.generatePrivate(random);
        // computePublic may modify the private key, so clone it first
        byte[] cloned = privateKey.clone();
        BigInteger publicKey = ops.computePublic(cloned);
        Arrays.fill(cloned, (byte)0);

        try {
            return new KeyPair(
                new XDHPublicKeyImpl(ops.getParameters(), publicKey),
                new XDHPrivateKeyImpl(ops.getParameters(), privateKey)
            );
        } catch (InvalidKeyException ex) {
            throw new ProviderException(ex);
        } finally {
            Arrays.fill(privateKey, (byte)0);
        }
    }

    static class X25519 extends XDHKeyPairGenerator {

        public X25519() {
            super(NamedParameterSpec.X25519);
        }
    }

    static class X448 extends XDHKeyPairGenerator {

        public X448() {
            super(NamedParameterSpec.X448);
        }
    }
}
