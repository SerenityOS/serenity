/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Locale;

import java.security.*;
import java.security.spec.*;

import javax.crypto.*;
import javax.crypto.spec.*;

import sun.nio.ch.DirectBuffer;
import sun.security.jca.JCAUtil;
import sun.security.pkcs11.wrapper.*;
import static sun.security.pkcs11.wrapper.PKCS11Constants.*;
import static sun.security.pkcs11.wrapper.PKCS11Exception.*;

/**
 * P11 AEAD Cipher implementation class. This class currently supports
 * AES cipher in GCM mode and CHACHA20-POLY1305 cipher.
 *
 * Note that AEAD modes do not use padding, so this class does not have
 * its own padding impl. In addition, some vendors such as NSS may not support
 * multi-part encryption/decryption for AEAD cipher algorithms, thus the
 * current impl uses PKCS#11 C_Encrypt/C_Decrypt calls and buffers data until
 * doFinal is called.
 *
 * @since   13
 */
final class P11AEADCipher extends CipherSpi {

    // supported AEAD algorithms/transformations
    private enum Transformation {
        AES_GCM("AES", "GCM", "NOPADDING", 16, 16),
        CHACHA20_POLY1305("CHACHA20", "NONE", "NOPADDING", 12, 16);

        final String keyAlgo;
        final String mode;
        final String padding;
        final int defIvLen; // in bytes
        final int defTagLen; // in bytes

        Transformation(String keyAlgo, String mode, String padding,
                int defIvLen, int defTagLen) {
            this.keyAlgo = keyAlgo;
            this.mode = mode;
            this.padding = padding;
            this.defIvLen = defIvLen;
            this.defTagLen = defTagLen;
        }
    }

    // token instance
    private final Token token;

    // mechanism id
    private final long mechanism;

    // type of this AEAD cipher, one of Transformation enum above
    private final Transformation type;

    // acceptable key size in bytes, -1 if more than 1 key sizes are accepted
    private final int fixedKeySize;

    // associated session, if any
    private Session session = null;

    // key, if init() was called
    private P11Key p11Key = null;

    // flag indicating whether an operation is initialized
    private boolean initialized = false;

    // falg indicating encrypt or decrypt mode
    private boolean encrypt = true;

    // parameters
    private byte[] iv = null;
    private int tagLen = -1;
    private SecureRandom random = JCAUtil.getSecureRandom();

    // dataBuffer is cleared upon doFinal calls
    private ByteArrayOutputStream dataBuffer = new ByteArrayOutputStream();
    // aadBuffer is cleared upon successful init calls
    private ByteArrayOutputStream aadBuffer = new ByteArrayOutputStream();
    private boolean updateCalled = false;

    private boolean requireReinit = false;
    private P11Key lastEncKey = null;
    private byte[] lastEncIv = null;

    P11AEADCipher(Token token, String algorithm, long mechanism)
            throws PKCS11Exception, NoSuchAlgorithmException {
        super();
        this.token = token;
        this.mechanism = mechanism;

        String[] algoParts = algorithm.split("/");
        if (algoParts[0].startsWith("AES")) {
            // for AES_GCM, need 3 parts
            if (algoParts.length != 3) {
                throw new AssertionError("Invalid Transformation format: " +
                        algorithm);
            }
            int index = algoParts[0].indexOf('_');
            if (index != -1) {
                // should be well-formed since we specify what we support
                fixedKeySize = Integer.parseInt(algoParts[0].substring(index+1)) >> 3;
            } else {
                fixedKeySize = -1;
            }
            this.type = Transformation.AES_GCM;
            engineSetMode(algoParts[1]);
            try {
                engineSetPadding(algoParts[2]);
            } catch (NoSuchPaddingException e) {
                throw new NoSuchAlgorithmException();
            }
        } else if (algoParts[0].equals("ChaCha20-Poly1305")) {
            fixedKeySize = 32;
            this.type = Transformation.CHACHA20_POLY1305;
            if (algoParts.length > 3) {
                throw new AssertionError(
                        "Invalid Transformation format: " + algorithm);
            } else {
                if (algoParts.length > 1) {
                    engineSetMode(algoParts[1]);
                }
                try {
                    if (algoParts.length > 2) {
                        engineSetPadding(algoParts[2]);
                    }
                } catch (NoSuchPaddingException e) {
                    throw new NoSuchAlgorithmException();
                }
            }
        } else {
            throw new AssertionError("Unsupported transformation " + algorithm);
        }
    }

    @Override
    protected void engineSetMode(String mode) throws NoSuchAlgorithmException {
        if (!mode.toUpperCase(Locale.ENGLISH).equals(type.mode)) {
            throw new NoSuchAlgorithmException("Unsupported mode " + mode);
        }
    }

    // see JCE spec
    @Override
    protected void engineSetPadding(String padding)
            throws NoSuchPaddingException {
        if (!padding.toUpperCase(Locale.ENGLISH).equals(type.padding)) {
            throw new NoSuchPaddingException("Unsupported padding " + padding);
        }
    }

    // see JCE spec
    @Override
    protected int engineGetBlockSize() {
        return switch (type) {
            case AES_GCM -> 16;
            case CHACHA20_POLY1305 -> 0;
            default -> throw new AssertionError("Unsupported type " + type);
        };
    }

    // see JCE spec
    @Override
    protected int engineGetOutputSize(int inputLen) {
        return doFinalLength(inputLen);
    }

    // see JCE spec
    @Override
    protected byte[] engineGetIV() {
        return (iv == null) ? null : iv.clone();
    }

    // see JCE spec
    protected AlgorithmParameters engineGetParameters() {
        String apAlgo;
        AlgorithmParameterSpec spec = null;
        switch (type) {
            case AES_GCM:
                apAlgo = "GCM";
                if (encrypt && iv == null && tagLen == -1) {
                    iv = new byte[type.defIvLen];
                    tagLen = type.defTagLen;
                    random.nextBytes(iv);
                }
                if (iv != null) {
                    spec = new GCMParameterSpec(tagLen << 3, iv);
                }
            break;
            case CHACHA20_POLY1305:
                if (encrypt && iv == null) {
                    iv = new byte[type.defIvLen];
                    random.nextBytes(iv);
                }
                apAlgo = "ChaCha20-Poly1305";
                if (iv != null) {
                    spec = new IvParameterSpec(iv);
                }
            break;
            default:
                throw new AssertionError("Unsupported type " + type);
        }
        if (spec != null) {
            try {
                AlgorithmParameters params =
                    AlgorithmParameters.getInstance(apAlgo);
                params.init(spec);
                return params;
            } catch (GeneralSecurityException e) {
                // NoSuchAlgorithmException, NoSuchProviderException
                // InvalidParameterSpecException
                throw new ProviderException("Could not encode parameters", e);
            }
        }
        return null;
    }

    // see JCE spec
    protected void engineInit(int opmode, Key key, SecureRandom sr)
            throws InvalidKeyException {
        if (opmode == Cipher.DECRYPT_MODE) {
            throw new InvalidKeyException("Parameters required for decryption");
        }
        updateCalled = false;
        try {
            implInit(opmode, key, null, -1, sr);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException("init() failed", e);
        }
    }

    // see JCE spec
    protected void engineInit(int opmode, Key key,
            AlgorithmParameterSpec params, SecureRandom sr)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (opmode == Cipher.DECRYPT_MODE && params == null) {
            throw new InvalidAlgorithmParameterException
                    ("Parameters required for decryption");
        }
        updateCalled = false;
        byte[] ivValue = null;
        int tagLen = -1;
        switch (type) {
            case AES_GCM:
                if (params != null) {
                    if (!(params instanceof GCMParameterSpec)) {
                        throw new InvalidAlgorithmParameterException
                                ("Only GCMParameterSpec is supported");
                    }
                    ivValue = ((GCMParameterSpec) params).getIV();
                    tagLen = ((GCMParameterSpec) params).getTLen() >> 3;
                }
            break;
            case CHACHA20_POLY1305:
                if (params != null) {
                    if (!(params instanceof IvParameterSpec)) {
                        throw new InvalidAlgorithmParameterException
                                ("Only IvParameterSpec is supported");
                    }
                    ivValue = ((IvParameterSpec) params).getIV();
                    tagLen = type.defTagLen;
                }
            break;
            default:
                throw new AssertionError("Unsupported type " + type);
        };
        implInit(opmode, key, ivValue, tagLen, sr);
    }

    // see JCE spec
    protected void engineInit(int opmode, Key key, AlgorithmParameters params,
            SecureRandom sr)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (opmode == Cipher.DECRYPT_MODE && params == null) {
            throw new InvalidAlgorithmParameterException
                    ("Parameters required for decryption");
        }
        updateCalled = false;
        try {
            AlgorithmParameterSpec paramSpec = null;
            if (params != null) {
                switch (type) {
                    case AES_GCM:
                        paramSpec =
                            params.getParameterSpec(GCMParameterSpec.class);
                        break;
                    case CHACHA20_POLY1305:
                        paramSpec =
                            params.getParameterSpec(IvParameterSpec.class);
                        break;
                    default:
                        throw new AssertionError("Unsupported type " + type);
                }
            }
            engineInit(opmode, key, paramSpec, sr);
        } catch (InvalidParameterSpecException ex) {
            throw new InvalidAlgorithmParameterException(ex);
        }
    }

    // actual init() implementation
    private void implInit(int opmode, Key key, byte[] iv, int tagLen,
        SecureRandom sr)
        throws InvalidKeyException, InvalidAlgorithmParameterException {
        reset(true);
        if (fixedKeySize != -1 &&
                ((key instanceof P11Key) ? ((P11Key) key).length() >> 3 :
                            key.getEncoded().length) != fixedKeySize) {
            throw new InvalidKeyException("Key size is invalid");
        }
        P11Key newKey = P11SecretKeyFactory.convertKey(token, key,
                type.keyAlgo);
        switch (opmode) {
            case Cipher.ENCRYPT_MODE:
                encrypt = true;
                requireReinit = Arrays.equals(iv, lastEncIv) &&
                        (newKey == lastEncKey);
                if (requireReinit) {
                    throw new InvalidAlgorithmParameterException(
                            "Cannot reuse the same key and iv pair");
                }
                break;
            case Cipher.DECRYPT_MODE:
                encrypt = false;
                requireReinit = false;
                break;
            case Cipher.WRAP_MODE:
            case Cipher.UNWRAP_MODE:
                throw new UnsupportedOperationException
                        ("Unsupported mode: " + opmode);
            default:
                // should never happen; checked by Cipher.init()
                throw new AssertionError("Unknown mode: " + opmode);
        }

        // decryption without parameters is checked in all engineInit() calls
        if (sr != null) {
            this.random = sr;
        }

        if (iv == null && tagLen == -1) {
            // generate default values
            switch (type) {
                case AES_GCM:
                    iv = new byte[type.defIvLen];
                    this.random.nextBytes(iv);
                    tagLen = type.defTagLen;
                    break;
                case CHACHA20_POLY1305:
                    iv = new byte[type.defIvLen];
                    this.random.nextBytes(iv);
                    tagLen = type.defTagLen;
                    break;
                default:
                    throw new AssertionError("Unsupported type " + type);
            }
        }
        this.iv = iv;
        this.tagLen = tagLen;
        this.p11Key = newKey;
        try {
            initialize();
        } catch (PKCS11Exception e) {
            if (e.getErrorCode() == CKR_MECHANISM_PARAM_INVALID) {
                throw new InvalidAlgorithmParameterException("Bad params", e);
            }
            throw new InvalidKeyException("Could not initialize cipher", e);
        }
    }

    private void cancelOperation() {
        // cancel operation by finishing it; avoid killSession as some
        // hardware vendors may require re-login
        int bufLen = doFinalLength(0);
        byte[] buffer = new byte[bufLen];
        byte[] in = dataBuffer.toByteArray();
        int inLen = in.length;
        try {
            if (encrypt) {
                token.p11.C_Encrypt(session.id(), 0, in, 0, inLen,
                        0, buffer, 0, bufLen);
            } else {
                token.p11.C_Decrypt(session.id(), 0, in, 0, inLen,
                        0, buffer, 0, bufLen);
            }
        } catch (PKCS11Exception e) {
            if (e.getErrorCode() == CKR_OPERATION_NOT_INITIALIZED) {
                // Cancel Operation may be invoked after an error on a PKCS#11
                // call. If the operation inside the token was already cancelled,
                // do not fail here. This is part of a defensive mechanism for
                // PKCS#11 libraries that do not strictly follow the standard.
                return;
            }
            if (encrypt) {
                throw new ProviderException("Cancel failed", e);
            }
            // ignore failure for decryption
        }
    }

    private void ensureInitialized() throws PKCS11Exception {
        if (initialized && aadBuffer.size() > 0) {
            // need to cancel first to avoid CKR_OPERATION_ACTIVE
            reset(true);
        }
        if (!initialized) {
            initialize();
        }
    }

    private void initialize() throws PKCS11Exception {
        if (p11Key == null) {
            throw new ProviderException(
                    "Operation cannot be performed without"
                    + " calling engineInit first");
        }
        if (requireReinit) {
            throw new IllegalStateException
                ("Must use either different key or iv");
        }

        token.ensureValid();

        byte[] aad = (aadBuffer.size() > 0? aadBuffer.toByteArray() : null);

        long p11KeyID = p11Key.getKeyID();
        try {
            CK_MECHANISM mechWithParams;
            switch (type) {
                case AES_GCM:
                    mechWithParams = new CK_MECHANISM(mechanism,
                        new CK_GCM_PARAMS(tagLen << 3, iv, aad));
                    break;
                case CHACHA20_POLY1305:
                    mechWithParams = new CK_MECHANISM(mechanism,
                        new CK_SALSA20_CHACHA20_POLY1305_PARAMS(iv, aad));
                    break;
                default:
                    throw new AssertionError("Unsupported type: " + type);
            }
            if (session == null) {
                session = token.getOpSession();
            }
            if (encrypt) {
                token.p11.C_EncryptInit(session.id(), mechWithParams,
                    p11KeyID);
            } else {
                token.p11.C_DecryptInit(session.id(), mechWithParams,
                    p11KeyID);
            }
        } catch (PKCS11Exception e) {
            p11Key.releaseKeyID();
            session = token.releaseSession(session);
            throw e;
        } finally {
            dataBuffer.reset();
            aadBuffer.reset();
        }
        initialized = true;
    }

    // if doFinal(inLen) is called, how big does the output buffer have to be?
    private int doFinalLength(int inLen) {
        if (inLen < 0) {
            throw new ProviderException("Invalid negative input length");
        }

        int result = inLen + dataBuffer.size();
        if (encrypt) {
            result += tagLen;
        } else {
            // In earlier NSS versions, AES_GCM would report
            // CKR_BUFFER_TOO_SMALL error if minus tagLen
            if (type == Transformation.CHACHA20_POLY1305) {
                result -= tagLen;
            }
        }
        return (result > 0? result : 0);
    }

    // reset the states to the pre-initialized values
    private void reset(boolean doCancel) {
        if (!initialized) {
            return;
        }
        initialized = false;

        try {
            if (session == null) {
                return;
            }

            if (doCancel && token.explicitCancel) {
                cancelOperation();
            }
        } finally {
            p11Key.releaseKeyID();
            session = token.releaseSession(session);
            dataBuffer.reset();
        }
    }

    // see JCE spec
    protected byte[] engineUpdate(byte[] in, int inOfs, int inLen) {
        updateCalled = true;
        int n = implUpdate(in, inOfs, inLen);
        return new byte[0];
    }

    // see JCE spec
    protected int engineUpdate(byte[] in, int inOfs, int inLen, byte[] out,
            int outOfs) throws ShortBufferException {
        updateCalled = true;
        implUpdate(in, inOfs, inLen);
        return 0;
    }

    // see JCE spec
    @Override
    protected int engineUpdate(ByteBuffer inBuffer, ByteBuffer outBuffer)
            throws ShortBufferException {
        updateCalled = true;
        implUpdate(inBuffer);
        return 0;
    }

    // see JCE spec
    @Override
    protected synchronized void engineUpdateAAD(byte[] src, int srcOfs, int srcLen)
            throws IllegalStateException {
        if ((src == null) || (srcOfs < 0) || (srcOfs + srcLen > src.length)) {
            throw new IllegalArgumentException("Invalid AAD");
        }
        if (requireReinit) {
            throw new IllegalStateException
                ("Must use either different key or iv for encryption");
        }
        if (p11Key == null) {
            throw new IllegalStateException("Need to initialize Cipher first");
        }
        if (updateCalled) {
            throw new IllegalStateException
                ("Update has been called; no more AAD data");
        }
        aadBuffer.write(src, srcOfs, srcLen);
    }

    // see JCE spec
    @Override
    protected void engineUpdateAAD(ByteBuffer src)
            throws IllegalStateException {
        if (src == null) {
            throw new IllegalArgumentException("Invalid AAD");
        }
        byte[] srcBytes = new byte[src.remaining()];
        src.get(srcBytes);
        engineUpdateAAD(srcBytes, 0, srcBytes.length);
    }

    // see JCE spec
    protected byte[] engineDoFinal(byte[] in, int inOfs, int inLen)
            throws IllegalBlockSizeException, BadPaddingException {
        int minOutLen = doFinalLength(inLen);
        try {
            byte[] out = new byte[minOutLen];
            int n = engineDoFinal(in, inOfs, inLen, out, 0);
            return P11Util.convert(out, 0, n);
        } catch (ShortBufferException e) {
            // convert since the output length is calculated by doFinalLength()
            throw new ProviderException(e);
        } finally {
            updateCalled = false;
        }
    }
    // see JCE spec
    protected int engineDoFinal(byte[] in, int inOfs, int inLen, byte[] out,
            int outOfs) throws ShortBufferException, IllegalBlockSizeException,
            BadPaddingException {
        try {
            return implDoFinal(in, inOfs, inLen, out, outOfs, out.length - outOfs);
        } finally {
            updateCalled = false;
        }
    }

    // see JCE spec
    @Override
    protected int engineDoFinal(ByteBuffer inBuffer, ByteBuffer outBuffer)
            throws ShortBufferException, IllegalBlockSizeException,
            BadPaddingException {
        try {
            return implDoFinal(inBuffer, outBuffer);
        } finally {
            updateCalled = false;
        }
    }

    private int implUpdate(byte[] in, int inOfs, int inLen) {
        if (inLen > 0) {
            updateCalled = true;
            try {
                ensureInitialized();
            } catch (PKCS11Exception e) {
                //e.printStackTrace();
                reset(false);
                throw new ProviderException("update() failed", e);
            }
            dataBuffer.write(in, inOfs, inLen);
        }
        // always 0 as NSS only supports single-part encryption/decryption
        return 0;
    }

    private int implUpdate(ByteBuffer inBuf) {
        int inLen = inBuf.remaining();
        if (inLen > 0) {
            try {
                ensureInitialized();
            } catch (PKCS11Exception e) {
                reset(false);
                throw new ProviderException("update() failed", e);
            }
            byte[] data = new byte[inLen];
            inBuf.get(data);
            dataBuffer.write(data, 0, data.length);
        }
        // always 0 as NSS only supports single-part encryption/decryption
        return 0;
    }

    private int implDoFinal(byte[] in, int inOfs, int inLen,
            byte[] out, int outOfs, int outLen)
            throws ShortBufferException, IllegalBlockSizeException,
            BadPaddingException {
        int requiredOutLen = doFinalLength(inLen);
        if (outLen < requiredOutLen) {
            throw new ShortBufferException();
        }

        boolean doCancel = true;
        try {
            ensureInitialized();
            if (dataBuffer.size() > 0) {
                if (in != null && inOfs > 0 && inLen > 0 &&
                    inOfs < (in.length - inLen)) {
                    dataBuffer.write(in, inOfs, inLen);
                }
                in = dataBuffer.toByteArray();
                inOfs = 0;
                inLen = in.length;
            }
            int k = 0;
            if (encrypt) {
                k = token.p11.C_Encrypt(session.id(), 0, in, inOfs, inLen,
                        0, out, outOfs, outLen);
                doCancel = false;
            } else {
                // Special handling to match SunJCE provider behavior
                if (inLen == 0) {
                    return 0;
                }
                k = token.p11.C_Decrypt(session.id(), 0, in, inOfs, inLen,
                        0, out, outOfs, outLen);
                doCancel = false;
            }
            return k;
        } catch (PKCS11Exception e) {
            // As per the PKCS#11 standard, C_Encrypt and C_Decrypt may only
            // keep the operation active on CKR_BUFFER_TOO_SMALL errors or
            // successful calls to determine the output length. However,
            // these cases are not expected here because the output length
            // is checked in the OpenJDK side before making the PKCS#11 call.
            // Thus, doCancel can safely be 'false'.
            doCancel = false;
            handleException(e);
            throw new ProviderException("doFinal() failed", e);
        } finally {
            if (encrypt) {
                lastEncKey = this.p11Key;
                lastEncIv = this.iv;
                requireReinit = true;
            }
            reset(doCancel);
        }
    }

    private int implDoFinal(ByteBuffer inBuffer, ByteBuffer outBuffer)
            throws ShortBufferException, IllegalBlockSizeException,
            BadPaddingException {
        int outLen = outBuffer.remaining();
        int inLen = inBuffer.remaining();

        int requiredOutLen = doFinalLength(inLen);
        if (outLen < requiredOutLen) {
            throw new ShortBufferException();
        }

        boolean doCancel = true;
        try {
            ensureInitialized();

            long inAddr = 0;
            byte[] in = null;
            int inOfs = 0;
            if (dataBuffer.size() > 0) {
                if (inLen > 0) {
                    byte[] temp = new byte[inLen];
                    inBuffer.get(temp);
                    dataBuffer.write(temp, 0, temp.length);
                }
                in = dataBuffer.toByteArray();
                inOfs = 0;
                inLen = in.length;
            } else {
                if (inBuffer instanceof DirectBuffer) {
                    inAddr = ((DirectBuffer) inBuffer).address();
                    inOfs = inBuffer.position();
                } else {
                    if (inBuffer.hasArray()) {
                        in = inBuffer.array();
                        inOfs = inBuffer.position() + inBuffer.arrayOffset();
                    } else {
                        in = new byte[inLen];
                        inBuffer.get(in);
                    }
                }
            }
            long outAddr = 0;
            byte[] outArray = null;
            int outOfs = 0;
            if (outBuffer instanceof DirectBuffer) {
                outAddr = ((DirectBuffer) outBuffer).address();
                outOfs = outBuffer.position();
            } else {
                if (outBuffer.hasArray()) {
                    outArray = outBuffer.array();
                    outOfs = outBuffer.position() + outBuffer.arrayOffset();
                } else {
                    outArray = new byte[outLen];
                }
            }

            int k = 0;
            if (encrypt) {
                k = token.p11.C_Encrypt(session.id(), inAddr, in, inOfs, inLen,
                        outAddr, outArray, outOfs, outLen);
                doCancel = false;
            } else {
                // Special handling to match SunJCE provider behavior
                if (inLen == 0) {
                    return 0;
                }
                k = token.p11.C_Decrypt(session.id(), inAddr, in, inOfs, inLen,
                        outAddr, outArray, outOfs, outLen);
                doCancel = false;
            }
            inBuffer.position(inBuffer.limit());
            outBuffer.position(outBuffer.position() + k);
            return k;
        } catch (PKCS11Exception e) {
            // As per the PKCS#11 standard, C_Encrypt and C_Decrypt may only
            // keep the operation active on CKR_BUFFER_TOO_SMALL errors or
            // successful calls to determine the output length. However,
            // these cases are not expected here because the output length
            // is checked in the OpenJDK side before making the PKCS#11 call.
            // Thus, doCancel can safely be 'false'.
            doCancel = false;
            handleException(e);
            throw new ProviderException("doFinal() failed", e);
        } finally {
            if (encrypt) {
                lastEncKey = this.p11Key;
                lastEncIv = this.iv;
                requireReinit = true;
            }
            reset(doCancel);
        }
    }

    private void handleException(PKCS11Exception e)
            throws ShortBufferException, IllegalBlockSizeException,
            BadPaddingException {
        long errorCode = e.getErrorCode();
        if (errorCode == CKR_BUFFER_TOO_SMALL) {
            throw (ShortBufferException)
                    (new ShortBufferException().initCause(e));
        } else if (errorCode == CKR_DATA_LEN_RANGE ||
                   errorCode == CKR_ENCRYPTED_DATA_LEN_RANGE) {
            throw (IllegalBlockSizeException)
                    (new IllegalBlockSizeException(e.toString()).initCause(e));
        } else if (errorCode == CKR_ENCRYPTED_DATA_INVALID ||
                // Solaris-specific
                errorCode == CKR_GENERAL_ERROR) {
            throw (AEADBadTagException)
                    (new AEADBadTagException(e.toString()).initCause(e));
        }
    }

    // see JCE spec
    protected byte[] engineWrap(Key key) throws IllegalBlockSizeException,
            InvalidKeyException {
        // XXX key wrapping
        throw new UnsupportedOperationException("engineWrap()");
    }

    // see JCE spec
    protected Key engineUnwrap(byte[] wrappedKey, String wrappedKeyAlgorithm,
            int wrappedKeyType)
            throws InvalidKeyException, NoSuchAlgorithmException {
        // XXX key unwrapping
        throw new UnsupportedOperationException("engineUnwrap()");
    }

    // see JCE spec
    @Override
    protected int engineGetKeySize(Key key) throws InvalidKeyException {
        int n = P11SecretKeyFactory.convertKey
                (token, key, type.keyAlgo).length();
        return n;
    }
}

