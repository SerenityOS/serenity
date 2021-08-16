/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import sun.security.ec.point.AffinePoint;

import java.io.ByteArrayOutputStream;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.SignatureSpi;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.EdECPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.EdDSAParameterSpec;
import java.security.spec.NamedParameterSpec;
import java.util.function.Function;

public class EdDSASignature extends SignatureSpi {

    private interface MessageAccumulator {
        void add(byte b);
        void add(byte[] data, int off, int len);
        byte[] getMessage();
    }

    private static class DigestAccumulator implements MessageAccumulator {
        private final EdDSAParameters.Digester digester;

        DigestAccumulator(EdDSAParameters.Digester digester) {
            this.digester = digester;
        }

        @Override
        public void add(byte b) {
            digester.update(b);
        }
        @Override
        public void add(byte[] data, int off, int len) {
            digester.update(data, off, len);
        }
        @Override
        public byte[] getMessage() {
            return digester.digest();
        }
    }

    private static class MemoryAccumulator implements MessageAccumulator {
        ByteArrayOutputStream message = new ByteArrayOutputStream();

        @Override
        public void add(byte b) {
            message.write(b);
        }
        @Override
        public void add(byte[] data, int off, int len) {
            message.write(data, off, len);
        }
        @Override
        public byte[] getMessage() {
            return message.toByteArray();
        }
    }

    private byte[] privateKey;
    private AffinePoint publicKeyPoint;
    private byte[] publicKeyBytes;
    private EdDSAOperations ops;
    private EdDSAParameters lockedParams = null;
    private MessageAccumulator message = null;
    private EdDSAParameterSpec sigParams = new EdDSAParameterSpec(false);

    public EdDSASignature() {
        // do nothing
    }

    EdDSASignature(NamedParameterSpec paramSpec) {
        lockedParams = EdDSAParameters.get(ProviderException::new, paramSpec);
    }

    @Override
    protected void engineInitVerify(PublicKey publicKey)
        throws InvalidKeyException {

        if (!(publicKey instanceof EdECPublicKey)) {
            throw new InvalidKeyException("Unsupported key type");
        }
        EdECPublicKey edKey = (EdECPublicKey) publicKey;
        EdDSAParameters params = EdDSAParameters.get(
            InvalidKeyException::new, edKey.getParams());

        initImpl(params);
        this.privateKey = null;
        this.publicKeyPoint = ops.decodeAffinePoint(InvalidKeyException::new,
            edKey.getPoint());
        EdDSAPublicKeyImpl pubKeyImpl = new EdDSAPublicKeyImpl(params,
            edKey.getPoint());
        this.publicKeyBytes = pubKeyImpl.getEncodedPoint();
    }

    @Override
    protected void engineInitSign(PrivateKey privateKey)
        throws InvalidKeyException {
        engineInitSign(privateKey, null);
    }

    @Override
    protected void engineInitSign(PrivateKey privateKey, SecureRandom random)
        throws InvalidKeyException {

        if (!(privateKey instanceof EdECPrivateKey)) {
            throw new InvalidKeyException("Unsupported key type");
        }
        EdECPrivateKey edKey = (EdECPrivateKey) privateKey;

        initImpl(edKey.getParams());
        this.privateKey = edKey.getBytes().orElseThrow(
        () -> new InvalidKeyException("No private key value"));
        this.publicKeyPoint = null;
        this.publicKeyBytes = null;
    }

    private
    <T extends Throwable>
    void checkLockedParams(Function<String, T> exception,
                           EdDSAParameters params) throws T {
        if (lockedParams != null && lockedParams != params) {
            throw exception.apply("Parameters must be " +
            lockedParams.getName());
        }
    }

    private void ensureMessageInit() throws SignatureException {
        if (message == null) {
            initMessage();
        }
    }

    private void initMessage() throws SignatureException {
        if (this.ops == null) {
            throw new SignatureException("not initialized");
        }
        EdDSAParameters params = ops.getParameters();

        if (sigParams.isPrehash()) {
            this.message = new DigestAccumulator(params.createDigester(64));
        } else {
            this.message = new MemoryAccumulator();
        }
    }

    @Override
    protected void engineUpdate(byte b) throws SignatureException {
        ensureMessageInit();
        this.message.add(b);
    }

    @Override
    protected void engineUpdate(byte[] b, int off, int len)
        throws SignatureException {

        ensureMessageInit();
        this.message.add(b, off, len);
    }

    @Override
    protected byte[] engineSign() throws SignatureException {
        if (privateKey == null) {
            throw new SignatureException("Missing private key");
        }
        ensureMessageInit();
        byte[] result = ops.sign(this.sigParams, this.privateKey,
            message.getMessage());
        message = null;
        return result;
    }

    @Override
    protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
        if (publicKeyBytes == null) {
            throw new SignatureException("Missing publicKey");
        }
        if (message == null) {
            return false;
        }
        boolean result = ops.verify(this.sigParams, this.publicKeyPoint,
            this.publicKeyBytes, message.getMessage(), sigBytes);
        message = null;
        return result;
    }

    private void initImpl(EdDSAParameters params) throws InvalidKeyException {
        checkLockedParams(InvalidKeyException::new, params);

        try {
            this.ops = new EdDSAOperations(params);
        } catch (NoSuchAlgorithmException ex) {
            throw new ProviderException(ex);
        }
        // message is (re)set to null
        // it will be initialized on first update
        this.message = null;
    }

    private void initImpl(NamedParameterSpec paramSpec)
        throws InvalidKeyException {

        EdDSAParameters params = EdDSAParameters.get(
            InvalidKeyException::new, paramSpec);
        initImpl(params);
    }

    @Deprecated
    @Override
    protected Object engineGetParameter(String param)
    throws InvalidParameterException {
        throw new UnsupportedOperationException("getParameter() not supported");
    }

    @Deprecated
    @Override
    protected void engineSetParameter(String param, Object value)
        throws InvalidParameterException {

        throw new UnsupportedOperationException("setParameter() not supported");
    }

    @Override
    protected void engineSetParameter(AlgorithmParameterSpec params)
        throws InvalidAlgorithmParameterException {

        // by convention, ignore null parameters
        if (params == null) {
            return;
        }

        if (params instanceof EdDSAParameterSpec) {
            if (message != null) {
                // sign/verify in progress
                throw new InvalidParameterException("Cannot change signature " +
                    "parameters during operation");
            }
            EdDSAParameterSpec edDsaParams = (EdDSAParameterSpec) params;
            checkContextLength(edDsaParams);

            this.sigParams = edDsaParams;
        } else {
            throw new InvalidAlgorithmParameterException(
                "Only EdDSAParameterSpec supported");
        }
    }

    private static void checkContextLength(EdDSAParameterSpec edDsaParams)
        throws InvalidAlgorithmParameterException {

        if (edDsaParams.getContext().isPresent()) {
            byte[] context = edDsaParams.getContext().get();
            if (context.length > 255) {
                throw new InvalidAlgorithmParameterException(
                "Context is longer than 255 bytes");
            }
        }
    }

    // There is no RFC-defined ASN.1 for prehash and context (RFC 8410)
    @Override
    protected AlgorithmParameters engineGetParameters() {
        return null;
    }

    public static class Ed25519 extends EdDSASignature {

        public Ed25519() {
            super(NamedParameterSpec.ED25519);
        }
    }

    public static class Ed448 extends EdDSASignature {

        public Ed448() {
            super(NamedParameterSpec.ED448);
        }
    }
}
