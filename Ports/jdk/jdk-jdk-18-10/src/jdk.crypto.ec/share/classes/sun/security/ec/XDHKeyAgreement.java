/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Key;
import java.security.SecureRandom;
import java.security.ProviderException;
import java.security.interfaces.XECPrivateKey;
import java.security.interfaces.XECPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.NamedParameterSpec;
import javax.crypto.KeyAgreementSpi;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;
import java.util.function.Function;

public class XDHKeyAgreement extends KeyAgreementSpi {

    private byte[] privateKey;
    private byte[] secret;
    private XECOperations ops;
    private XECParameters lockedParams = null;

    XDHKeyAgreement() {
        // do nothing
    }

    XDHKeyAgreement(AlgorithmParameterSpec paramSpec) {
        lockedParams = XECParameters.get(ProviderException::new, paramSpec);
    }

    @Override
    protected void engineInit(Key key, SecureRandom random)
            throws InvalidKeyException {

        initImpl(key);
    }

    @Override
    protected void engineInit(Key key, final AlgorithmParameterSpec params,
                              SecureRandom random) throws InvalidKeyException,
        InvalidAlgorithmParameterException {

        initImpl(key);

        // the private key parameters must match params, if present
        if (params != null) {
            XECParameters xecParams = XECParameters.get(
                InvalidAlgorithmParameterException::new, params);
            if (!xecParams.oidEquals(this.ops.getParameters())) {
                throw new InvalidKeyException(
                    "Incorrect private key parameters"
                );
            }
        }
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

    private void initImpl(Key key) throws InvalidKeyException {

        if (!(key instanceof XECPrivateKey)) {
            throw new InvalidKeyException
            ("Unsupported key type");
        }
        XECPrivateKey privateKey = (XECPrivateKey) key;
        XECParameters xecParams = XECParameters.get(
            InvalidKeyException::new, privateKey.getParams());
        checkLockedParams(InvalidKeyException::new, xecParams);

        this.ops = new XECOperations(xecParams);
        this.privateKey = privateKey.getScalar().orElseThrow(
            () -> new InvalidKeyException("No private key value")
        );
        secret = null;
    }

    @Override
    protected Key engineDoPhase(Key key, boolean lastPhase)
            throws InvalidKeyException, IllegalStateException {

        if (this.privateKey == null) {
            throw new IllegalStateException("Not initialized");
        }
        if (this.secret != null) {
            throw new IllegalStateException("Phase already executed");
        }
        if (!lastPhase) {
            throw new IllegalStateException
                ("Only two party agreement supported, lastPhase must be true");
        }
        if (!(key instanceof XECPublicKey)) {
            throw new InvalidKeyException
                ("Unsupported key type");
        }

        XECPublicKey publicKey = (XECPublicKey) key;

        // Ensure public key parameters are compatible with private key
        XECParameters xecParams = XECParameters.get(InvalidKeyException::new,
            publicKey.getParams());
        if (!ops.getParameters().oidEquals(xecParams)) {
            throw new InvalidKeyException(
            "Public key parameters are not compatible with private key.");
        }

        // The privateKey may be modified to a value that is equivalent for
        // the purposes of this algorithm.
        byte[] computedSecret = ops.encodedPointMultiply(
            this.privateKey,
            publicKey.getU());

        // test for contributory behavior
        if (allZero(computedSecret)) {
            throw new InvalidKeyException("Point has small order");
        }

        this.secret = computedSecret;

        return null;
    }

    /*
     * Constant-time check for an all-zero array
     */
    private boolean allZero(byte[] arr) {
        byte orValue = (byte) 0;
        for (int i = 0; i < arr.length; i++) {
            orValue |= arr[i];
        }

        return orValue == (byte) 0;
    }

    @Override
    protected byte[] engineGenerateSecret() throws IllegalStateException {
        if (secret == null) {
            throw new IllegalStateException("Not initialized correctly");
        }

        byte[] result = secret;
        secret = null;
        return result;
    }

    @Override
    protected int engineGenerateSecret(byte[] sharedSecret, int offset)
        throws IllegalStateException, ShortBufferException {

        if (secret == null) {
            throw new IllegalStateException("Not initialized correctly");
        }
        int secretLen = this.secret.length;
        if (secretLen > sharedSecret.length - offset) {
            throw new ShortBufferException("Need " + secretLen
                + " bytes, only " + (sharedSecret.length - offset)
                + " available");
        }

        System.arraycopy(this.secret, 0, sharedSecret, offset, secretLen);
        secret = null;
        return secretLen;
    }

    @Override
    protected SecretKey engineGenerateSecret(String algorithm)
            throws IllegalStateException, NoSuchAlgorithmException,
            InvalidKeyException {

        if (algorithm == null) {
            throw new NoSuchAlgorithmException("Algorithm must not be null");
        }

        if (!(algorithm.equals("TlsPremasterSecret"))) {
            throw new NoSuchAlgorithmException(
                    "Only supported for algorithm TlsPremasterSecret");
        }
        return new SecretKeySpec(engineGenerateSecret(), algorithm);
    }

    static class X25519 extends XDHKeyAgreement {

        public X25519() {
            super(NamedParameterSpec.X25519);
        }
    }

    static class X448 extends XDHKeyAgreement {

        public X448() {
            super(NamedParameterSpec.X448);
        }
    }
}
