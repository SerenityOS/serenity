/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.crypto.provider;

import java.util.Arrays;
import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import static com.sun.crypto.provider.KWUtil.*;

/**
 * This class is the impl class for AES KeyWrap algorithms as defined in
 * <a href=https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-38F.pdf>
 * "Recommendation for Block Cipher Modes of Operation: Methods for Key Wrapping"
 */
abstract class KeyWrapCipher extends CipherSpi {

    // for AESWrap + AES/KW/NoPadding
    public static final class AES_KW_NoPadding extends KeyWrapCipher {
        public AES_KW_NoPadding() {
            super(new AESKeyWrap(), null, -1);
        }
    }

    // for AESWrap_128 + AES_128/KW/NoPadding
    public static final class AES128_KW_NoPadding extends KeyWrapCipher {
        public AES128_KW_NoPadding() {
            super(new AESKeyWrap(), null, 16);
        }
    }

    // for AESWrap_192 + AES_192/KW/NoPadding
    public static final class AES192_KW_NoPadding extends KeyWrapCipher {
        public AES192_KW_NoPadding() {
            super(new AESKeyWrap(), null, 24);
        }
    }

    // for AESWrap_256 + AES_256/KW/NoPadding
    public static final class AES256_KW_NoPadding extends KeyWrapCipher {
        public AES256_KW_NoPadding() {
            super(new AESKeyWrap(), null, 32);
        }
    }

    // for AES/KW/NoPadding
    public static final class AES_KW_PKCS5Padding extends KeyWrapCipher {
        public AES_KW_PKCS5Padding() {
            super(new AESKeyWrap(), new PKCS5Padding(16), -1);
        }
    }

    // for AES_128/KW/NoPadding
    public static final class AES128_KW_PKCS5Padding extends KeyWrapCipher {
        public AES128_KW_PKCS5Padding() {
            super(new AESKeyWrap(), new PKCS5Padding(16), 16);
        }
    }

    // for AES_192/KW/NoPadding
    public static final class AES192_KW_PKCS5Padding extends KeyWrapCipher {
        public AES192_KW_PKCS5Padding() {
            super(new AESKeyWrap(), new PKCS5Padding(16), 24);
        }
    }

    // for AES_256/KW/NoPadding
    public static final class AES256_KW_PKCS5Padding extends KeyWrapCipher {
        public AES256_KW_PKCS5Padding() {
            super(new AESKeyWrap(), new PKCS5Padding(16), 32);
        }
    }

    // for AES/KWP/NoPadding
    public static final class AES_KWP_NoPadding extends KeyWrapCipher {
        public AES_KWP_NoPadding() {
            super(new AESKeyWrapPadded(), null, -1);
        }
    }

    // for AES_128/KWP/NoPadding
    public static final class AES128_KWP_NoPadding extends KeyWrapCipher {
        public AES128_KWP_NoPadding() {
            super(new AESKeyWrapPadded(), null, 16);
        }
    }

    // for AES_192/KWP/NoPadding
    public static final class AES192_KWP_NoPadding extends KeyWrapCipher {
        public AES192_KWP_NoPadding() {
            super(new AESKeyWrapPadded(), null, 24);
        }
    }

    // for AES_256/KWP/NoPadding
    public static final class AES256_KWP_NoPadding extends KeyWrapCipher {
        public AES256_KWP_NoPadding() {
            super(new AESKeyWrapPadded(), null, 32);
        }
    }

    // store the specified bytes, e.g. in[inOfs...(inOfs+inLen-1)] into
    // 'dataBuf' starting at 'dataIdx'.
    // NOTE: if 'in' is null, this method will ensure that 'dataBuf' has enough
    // capacity for 'inLen' bytes but will NOT copy bytes from 'in'.
    private void store(byte[] in, int inOfs, int inLen) {
        // In NIST SP 800-38F, KWP input size is limited to be no longer
        // than 2^32 bytes. Otherwise, the length cannot be encoded in 32 bits
        // However, given the current spec requirement that recovered text
        // can only be returned after successful tag verification, we are
        // bound by limiting the data size to the size limit of java byte array,
        // e.g. Integer.MAX_VALUE, since all data are returned by doFinal().
        int remain = Integer.MAX_VALUE - dataIdx;
        if (inLen > remain) {
            throw new ProviderException("SunJCE provider can only take " +
                remain + " more bytes");
        }

        // resize 'dataBuf' to the smallest (n * BLKSIZE) + SEMI_BLKSIZE)
        if (dataBuf == null || dataBuf.length - dataIdx < inLen) {
            int newSize = Math.addExact(dataIdx, inLen);
            int lastBlk = (dataIdx + inLen - SEMI_BLKSIZE) % BLKSIZE;
            if (lastBlk != 0 || padding != null) {
                newSize = Math.addExact(newSize, BLKSIZE - lastBlk);
            }
            byte[] temp = new byte[newSize];
            if (dataBuf != null && dataIdx > 0) {
                System.arraycopy(dataBuf, 0, temp, 0, dataIdx);
            }
            dataBuf = temp;
        }

        if (in != null) {
            System.arraycopy(in, inOfs, dataBuf, dataIdx, inLen);
            dataIdx += inLen;
        }
    }

    // internal cipher object which does the real work.
    // AESKeyWrap for KW, AESKeyWrapPadded for KWP
    private final FeedbackCipher cipher;

    // internal padding object; null if NoPadding
    private final Padding padding;

    // encrypt/wrap or decrypt/unwrap?
    private int opmode = -1; // must be set by init(..)

    /*
     * needed to support oids which associates a fixed key size
     * to the cipher object.
     */
    private final int fixedKeySize; // in bytes, -1 if no restriction

    // internal data buffer for encrypt, decrypt calls
    // must use store() to store data into 'dataBuf' as it will resize if needed
    private byte[] dataBuf;
    private int dataIdx;

    /**
     * Creates an instance of KeyWrap cipher using the specified
     * symmetric cipher whose block size must be 128-bit, and
     * the supported mode and padding scheme.
     */
    public KeyWrapCipher(FeedbackCipher cipher, Padding padding, int keySize) {
        this.cipher = cipher;
        this.padding = padding;
        this.fixedKeySize = keySize;
        this.dataBuf = null;
        this.dataIdx = 0;
    }

    /**
     * Sets the mode of this cipher. Must match the mode specified in
     * the constructor.
     *
     * @param mode the cipher mode
     *
     * @exception NoSuchAlgorithmException if the requested cipher mode
     * does not match the supported mode
     */
    @Override
    protected void engineSetMode(String mode) throws NoSuchAlgorithmException {
        if (mode != null && !cipher.getFeedback().equalsIgnoreCase(mode)) {
            throw new NoSuchAlgorithmException(mode + " cannot be used");
        }
    }

    /**
     * Sets the padding mechanism of this cipher. The specified padding
     * scheme should match what this cipher is configured with.
     *
     * @param padding the padding mechanism
     *
     * @exception NoSuchPaddingException if the requested padding mechanism
     * does not match the supported padding scheme
     */
    @Override
    protected void engineSetPadding(String padding)
            throws NoSuchPaddingException {
        if ((this.padding == null && !"NoPadding".equalsIgnoreCase(padding)) ||
                 this.padding instanceof PKCS5Padding &&
                 !"PKCS5Padding".equalsIgnoreCase(padding)) {
            throw new NoSuchPaddingException("Unsupported padding " + padding);
        }
    }

    /**
     * Returns the block size (in bytes). i.e. 16 bytes.
     *
     * @return the block size (in bytes), i.e. 16 bytes.
     */
    @Override
    protected int engineGetBlockSize() {
        return cipher.getBlockSize();
    }

    /**
     * Returns the length in bytes that an output buffer would need to be
     * given the input length <code>inLen</code> (in bytes).
     *
     * <p>The actual output length of the next <code>update</code> or
     * <code>doFinal</code> call may be smaller than the length returned
     * by this method.
     *
     * @param inLen the input length (in bytes)
     *
     * @return the required output buffer size (in bytes)
     */
    protected int engineGetOutputSize(int inLen) {

        int result;

        if (opmode == Cipher.ENCRYPT_MODE || opmode == Cipher.WRAP_MODE) {
            result = (dataIdx > 0?
                    Math.addExact(inLen, dataIdx - SEMI_BLKSIZE) : inLen);
            // calculate padding length based on plaintext length
            int padLen = 0;
            if (padding != null) {
                padLen = padding.padLength(result);
            } else if (cipher instanceof AESKeyWrapPadded) {
                int n = result % SEMI_BLKSIZE;
                if (n != 0) {
                    padLen = SEMI_BLKSIZE - n;
                }
            }
            // then add the first semiblock and padLen to result
            result = Math.addExact(result, SEMI_BLKSIZE + padLen);
        } else {
            result = inLen - SEMI_BLKSIZE;
            if (dataIdx > 0) {
                result = Math.addExact(result, dataIdx);
            }
        }
        return result;
    }

    /**
     * Returns the initialization vector (IV) in a new buffer.
     *
     * @return the user-specified iv, or null if the underlying algorithm does
     * not use an IV, or if the IV has not yet been set.
     */
    @Override
    protected byte[] engineGetIV() {
        byte[] iv = cipher.getIV();
        return (iv == null? null : iv.clone());
    }

    // actual impl for various engineInit(...) methods
    private void implInit(int opmode, Key key, byte[] iv, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        byte[] keyBytes = key.getEncoded();
        if (keyBytes == null) {
            throw new InvalidKeyException("Null key");
        }
        this.opmode = opmode;
        boolean decrypting = (opmode == Cipher.DECRYPT_MODE ||
                opmode == Cipher.UNWRAP_MODE);
        try {
            cipher.init(decrypting, key.getAlgorithm(), keyBytes, iv);
            dataBuf = null;
            dataIdx = 0;
        } finally {
            Arrays.fill(keyBytes, (byte) 0);
        }
    }

    /**
     * Initializes this cipher with a key and a source of randomness.
     *
     * @param opmode the operation mode of this cipher.
     * @param key the secret key.
     * @param random the source of randomness.
     *
     * @exception InvalidKeyException if the given key is inappropriate for
     * initializing this cipher.
     */
    @Override
    protected void engineInit(int opmode, Key key, SecureRandom random)
        throws InvalidKeyException {
        try {
            implInit(opmode, key, (byte[])null, random);
        } catch (InvalidAlgorithmParameterException iae) {
            // should never happen
            throw new AssertionError(iae);
        }
    }

    /**
     * Initializes this cipher with a key, a set of algorithm parameters,
     * and a source of randomness.
     *
     * @param opmode the operation mode of this cipher.
     * @param key the secret key.
     * @param params the algorithm parameters; if not null, must be of type
     * IvParameterSpec
     * @param random the source of randomness.
     *
     * @exception InvalidKeyException if the given key is inappropriate for
     * initializing this cipher
     * @exception InvalidAlgorithmParameterException if the given algorithm
     * parameters is invalid.
     */
    @Override
    protected void engineInit(int opmode, Key key,
            AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (params != null && !(params instanceof IvParameterSpec)) {
            throw new InvalidAlgorithmParameterException(
                "Only IvParameterSpec is accepted");
        }
        byte[] iv = (params == null? null : ((IvParameterSpec)params).getIV());
        implInit(opmode, key, iv, random);
    }

    /**
     * Initializes this cipher with a key, a set of algorithm parameters,
     * and a source of randomness.
     *
     * @param opmode the operation mode of this cipher.
     * @param key the secret key.
     * @param params the algorithm parameters; if not null, must be able to
     * be converted to IvParameterSpec.
     * @param random the source of randomness.
     *
     * @exception InvalidKeyException if the given key is inappropriate.
     * @exception InvalidAlgorithmParameterException if the given algorithm
     * parameters is invalid.
     */
    @Override
    protected void engineInit(int opmode, Key key, AlgorithmParameters params,
            SecureRandom random) throws InvalidKeyException,
            InvalidAlgorithmParameterException {
        byte[] iv = null;
        if (params != null) {
            try {
                AlgorithmParameterSpec spec =
                        params.getParameterSpec(IvParameterSpec.class);
                iv = ((IvParameterSpec)spec).getIV();
            } catch (InvalidParameterSpecException ispe) {
                throw new InvalidAlgorithmParameterException(
                    "Only IvParameterSpec is accepted");
            }
        }
        try {
            implInit(opmode, key, iv, random);
        } catch (IllegalArgumentException iae) {
            throw new InvalidAlgorithmParameterException(iae.getMessage());
        }
    }

    /**
     * See CipherSpi.engineUpdate(...) - buffers data internally as
     * only single part operation is supported.
     *
     * @param in the input buffer.
     * @param inOffset the offset in <code>in</code> where the input
     * starts.
     * @param inLen the input length.
     *
     * @return null.
     */
    @Override
    protected byte[] engineUpdate(byte[] in, int inOffset, int inLen) {
        if (opmode != Cipher.ENCRYPT_MODE && opmode != Cipher.DECRYPT_MODE) {
            throw new IllegalStateException
                    ("Cipher not initialized for update");
        }
        implUpdate(in, inOffset, inLen);
        return null;
    }

    /**
     * See CipherSpi.engineUpdate(...) - buffers data internally as
     * only single part operation is supported.
     *
     * @param in the input buffer.
     * @param inOffset the offset in <code>in</code> where the input
     * starts.
     * @param inLen the input length.
     * @param out the buffer for the result.
     * @param outOffset the offset in <code>out</code> where the result
     * is stored.
     *
     * @return n/a.
     *
     * @exception IllegalStateException upon invocation of this method.
     */
    @Override
    protected int engineUpdate(byte[] in, int inOffset, int inLen,
            byte[] out, int outOffset) throws ShortBufferException {
        if (opmode != Cipher.ENCRYPT_MODE && opmode != Cipher.DECRYPT_MODE) {
            throw new IllegalStateException
                    ("Cipher not initialized for update");
        }
        implUpdate(in, inOffset, inLen);
        return 0;
    }

    // actual impl for various engineUpdate(...) methods
    private void implUpdate(byte[] in, int inOfs, int inLen) {
        if (inLen <= 0) return;

        if (opmode == Cipher.ENCRYPT_MODE && dataIdx == 0) {
            // the first semiblock is for iv, store data after it
            dataIdx = SEMI_BLKSIZE;
        }
        store(in, inOfs, inLen);
    }

    /**
     * See CipherSpi.engineDoFinal(...)
     *
     * @param input the input buffer
     * @param inputOffset the offset in <code>in</code> where the input
     * starts
     * @param inputLen the input length.
     *
     * @return n/a.
     *
     * @exception IllegalStateException upon invocation of this method.
     */
    @Override
    protected byte[] engineDoFinal(byte[] in, int inOfs, int inLen)
            throws IllegalBlockSizeException, BadPaddingException {

        int estOutLen = engineGetOutputSize(inLen);
        byte[] out = new byte[estOutLen];
        try {
            int outLen = engineDoFinal(in, inOfs, inLen, out, 0);

            if (outLen < estOutLen) {
                try {
                    return Arrays.copyOf(out, outLen);
                } finally {
                    Arrays.fill(out, (byte)0);
                }
            } else {
                return out;
            }
        } catch (ShortBufferException sbe) {
            // should never happen
            throw new AssertionError(sbe);
        }
    }

    /**
     * See CipherSpi.doFinal(...)
     *
     * @param in the input buffer.
     * @param inOffset the offset in <code>in</code> where the input
     * starts.
     * @param inLen the input length.
     * @param out the buffer for the result.
     * @param outOffset the ofset in <code>out</code> where the result
     * is stored.
     *
     * @return n/a.
     *
     * @exception IllegalStateException upon invocation of this method.
     */
    protected int engineDoFinal(byte[] in, int inOfs, int inLen,
            byte[] out, int outOfs) throws IllegalBlockSizeException,
            ShortBufferException, BadPaddingException {

        if (opmode != Cipher.ENCRYPT_MODE && opmode != Cipher.DECRYPT_MODE) {
            throw new IllegalStateException
                    ("Cipher not initialized for doFinal");
        }

        int estOutLen = engineGetOutputSize(inLen);
        if (out.length - outOfs < estOutLen) {
            throw new ShortBufferException("Need at least " + estOutLen);
        }

        try {
            // cannot write out the result for decryption due to verification
            // requirement
            if (outOfs == 0 && opmode == Cipher.ENCRYPT_MODE) {
                return implDoFinal(in, inOfs, inLen, out);
            } else {
                // use 'dataBuf' as output buffer and then copy into 'out'
                // make sure 'dataBuf' is large enough
                store(null, 0, inLen);
                int outLen = implDoFinal(in, inOfs, inLen, dataBuf);
                if (outLen > estOutLen) {
                    throw new AssertionError
                            ("Actual output length exceeds estimated length");
                }
                System.arraycopy(dataBuf, 0, out, outOfs, outLen);
                return outLen;
            }
        } finally {
            if (dataBuf != null) {
                Arrays.fill(dataBuf, (byte)0);
            }
            dataBuf = null;
            dataIdx = 0;
        }
    }

    // actual impl for various engineDoFinal(...) methods.
    // prepare 'out' buffer with the buffered bytes in 'dataBuf',
    // and the to-be-processed bytes in 'in', then perform single-part
    // encryption/decrytion over 'out' buffer
    private int implDoFinal(byte[] in, int inOfs, int inLen, byte[] out)
        throws IllegalBlockSizeException, BadPaddingException,
            ShortBufferException {

        int len = (out == dataBuf? dataIdx : 0);

        // copy over the buffered bytes if out != dataBuf
        if (out != dataBuf && dataIdx > 0) {
            System.arraycopy(dataBuf, 0, out, 0, dataIdx);
            len = dataIdx;
        }

        if (opmode == Cipher.ENCRYPT_MODE && len == 0) {
            len = SEMI_BLKSIZE; // reserve space for the ICV if encryption
        }

        if (inLen > 0) {
            System.arraycopy(in, inOfs, out, len, inLen);
            len += inLen;
        }

        try {
            return (opmode == Cipher.ENCRYPT_MODE ?
                    helperEncrypt(out, len) : helperDecrypt(out, len));
        } finally {
            if (dataBuf != null && dataBuf != out) {
                Arrays.fill(dataBuf, (byte)0);
            }
        }
    }

    // helper routine for in-place encryption.
    // 'inBuf' = semiblock | plain text | extra bytes if padding is used
    // 'inLen' = semiblock length + plain text length
    private int helperEncrypt(byte[] inBuf, int inLen)
            throws IllegalBlockSizeException, ShortBufferException {

        // pad data if padding is used
        if (padding != null) {
            int paddingLen = padding.padLength(inLen - SEMI_BLKSIZE);

            if (inLen + paddingLen > inBuf.length) {
                throw new AssertionError("encrypt buffer too small");
            }

            try {
                padding.padWithLen(inBuf, inLen, paddingLen);
                inLen += paddingLen;
            } catch (ShortBufferException sbe) {
                // should never happen
                throw new AssertionError(sbe);
            }
        }
        return cipher.encryptFinal(inBuf, 0, inLen, null, 0);
    }

    // helper routine for in-place decryption.
    // 'inBuf' = cipher text
    // 'inLen' = cipher text length
    private int helperDecrypt(byte[] inBuf, int inLen)
            throws IllegalBlockSizeException, BadPaddingException,
            ShortBufferException {

        int outLen = cipher.decryptFinal(inBuf, 0, inLen, null, 0);
        // unpad data if padding is used
        if (padding != null) {
            int padIdx = padding.unpad(inBuf, 0, outLen);
            if (padIdx <= 0) {
                throw new BadPaddingException("Bad Padding: " + padIdx);
            }
            outLen = padIdx;
        }
        return outLen;
    }

    /**
     * Returns the parameters used with this cipher.
     *
     * @return AlgorithmParameters object containing IV, or null if this cipher
     * does not use any parameters.
     */
    @Override
    protected AlgorithmParameters engineGetParameters() {
        AlgorithmParameters params = null;

        byte[] iv = cipher.getIV();
        if (iv == null) {
            iv = (cipher instanceof AESKeyWrap?
                    AESKeyWrap.ICV1 : AESKeyWrapPadded.ICV2);
        }
        try {
            params = AlgorithmParameters.getInstance("AES");
            params.init(new IvParameterSpec(iv));
        } catch (NoSuchAlgorithmException | InvalidParameterSpecException e) {
            // should never happen
            throw new AssertionError();
        }
        return params;
    }

    /**
     * Returns the key size of the given key object in number of bits.
     *
     * @param key the key object.
     *
     * @return the "effective" key size of the given key object.
     *
     * @exception InvalidKeyException if <code>key</code> is invalid.
     */
    protected int engineGetKeySize(Key key) throws InvalidKeyException {
        byte[] encoded = key.getEncoded();
        if (encoded == null)  {
            throw new InvalidKeyException("Cannot decide key length");
        }

        // only need length
        Arrays.fill(encoded, (byte) 0);
        int keyLen = encoded.length;
        if (!key.getAlgorithm().equalsIgnoreCase("AES") ||
            !AESCrypt.isKeySizeValid(keyLen) ||
            (fixedKeySize != -1 && fixedKeySize != keyLen)) {
            throw new InvalidKeyException("Invalid key length: " +
                    keyLen + " bytes");
        }
        return Math.multiplyExact(keyLen, 8);
    }

    /**
     * Wrap a key.
     *
     * @param key the key to be wrapped.
     *
     * @return the wrapped key.
     *
     * @exception IllegalBlockSizeException if this cipher is a block
     * cipher, no padding has been requested, and the length of the
     * encoding of the key to be wrapped is not a
     * multiple of the block size.
     *
     * @exception InvalidKeyException if it is impossible or unsafe to
     * wrap the key with this cipher (e.g., a hardware protected key is
     * being passed to a software only cipher).
     */
    @Override
    protected byte[] engineWrap(Key key)
            throws IllegalBlockSizeException, InvalidKeyException {

        if (opmode != Cipher.WRAP_MODE) {
            throw new IllegalStateException("Cipher not initialized for wrap");
        }
        byte[] encoded = key.getEncoded();
        if ((encoded == null) || (encoded.length == 0)) {
            throw new InvalidKeyException("Cannot get an encoding of " +
                                          "the key to be wrapped");
        }
        // output size is known, allocate output buffer
        byte[] out = new byte[engineGetOutputSize(encoded.length)];

        // reserve the first semiblock and do not write data
        int len = SEMI_BLKSIZE;
        System.arraycopy(encoded, 0, out, len, encoded.length);
        len += encoded.length;

        // discard key data
        Arrays.fill(encoded, (byte) 0);

        try {
            int outLen = helperEncrypt(out, len);
            if (outLen != out.length) {
                throw new AssertionError("Wrong output buffer size");
            }
            return out;
        } catch (ShortBufferException sbe) {
            // should never happen
            throw new AssertionError();
        }
    }

    /**
     * Unwrap a previously wrapped key.
     *
     * @param wrappedKey the key to be unwrapped.
     *
     * @param wrappedKeyAlgorithm the algorithm the wrapped key is for.
     *
     * @param wrappedKeyType the type of the wrapped key.
     * This is one of <code>Cipher.SECRET_KEY</code>,
     * <code>Cipher.PRIVATE_KEY</code>, or <code>Cipher.PUBLIC_KEY</code>.
     *
     * @return the unwrapped key.
     *
     * @exception NoSuchAlgorithmException if no installed providers
     * can create keys of type <code>wrappedKeyType</code> for the
     * <code>wrappedKeyAlgorithm</code>.
     *
     * @exception InvalidKeyException if <code>wrappedKey</code> does not
     * represent a wrapped key of type <code>wrappedKeyType</code> for
     * the <code>wrappedKeyAlgorithm</code>.
     */
    @Override
    protected Key engineUnwrap(byte[] wrappedKey, String wrappedKeyAlgorithm,
            int wrappedKeyType) throws InvalidKeyException,
            NoSuchAlgorithmException {

        if (opmode != Cipher.UNWRAP_MODE) {
            throw new IllegalStateException
                    ("Cipher not initialized for unwrap");
        }

        byte[] buf = wrappedKey.clone();
        try {
            int outLen = helperDecrypt(buf, buf.length);
            return ConstructKeys.constructKey(buf, 0, outLen,
                    wrappedKeyAlgorithm, wrappedKeyType);
        } catch (ShortBufferException sbe) {
            // should never happen
            throw new AssertionError();
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new InvalidKeyException(e);
        } finally {
            Arrays.fill(buf, (byte) 0);
        }
    }
}
