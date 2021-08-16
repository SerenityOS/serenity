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

import java.security.*;
import java.security.spec.*;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * This class implements the CMS DESede KeyWrap algorithm as defined
 * in <a href=http://www.w3.org/TR/xmlenc-core/#sec-Alg-SymmetricKeyWrap></a>
 * "XML Encryption Syntax and Processing" section 5.6.2
 * "CMS Triple DES Key Wrap".
 * Note: only <code>CBC</code> mode and <code>NoPadding</code> padding
 * scheme can be used for this algorithm.
 *
 * @author Valerie Peng
 *
 *
 * @see DESedeCipher
 */
public final class DESedeWrapCipher extends CipherSpi {

    private static final byte[] IV2 = {
        (byte) 0x4a, (byte) 0xdd, (byte) 0xa2, (byte) 0x2c,
        (byte) 0x79, (byte) 0xe8, (byte) 0x21, (byte) 0x05
    };

    private static final int CHECKSUM_LEN = 8;
    private static final int IV_LEN = 8;

    /*
     * internal cipher object which does the real work.
     */
    private FeedbackCipher cipher;

    /*
     * iv for (re-)initializing the internal cipher object.
     */
    private byte[] iv = null;

    /*
     * key for re-initializing the internal cipher object.
     */
    private Key cipherKey = null;

    /*
     * are we encrypting or decrypting?
     */
    private boolean decrypting = false;

    /**
     * Creates an instance of CMS DESede KeyWrap cipher with default
     * mode, i.e. "CBC" and padding scheme, i.e. "NoPadding".
     */
    public DESedeWrapCipher() {
        cipher = new CipherBlockChaining(new DESedeCrypt());
    }

    /**
     * Sets the mode of this cipher. Only "CBC" mode is accepted for this
     * cipher.
     *
     * @param mode the cipher mode.
     *
     * @exception NoSuchAlgorithmException if the requested cipher mode
     * is not "CBC".
     */
    protected void engineSetMode(String mode)
        throws NoSuchAlgorithmException {
        if (!mode.equalsIgnoreCase("CBC")) {
            throw new NoSuchAlgorithmException(mode + " cannot be used");
        }
    }

    /**
     * Sets the padding mechanism of this cipher. Only "NoPadding" schmem
     * is accepted for this cipher.
     *
     * @param padding the padding mechanism.
     *
     * @exception NoSuchPaddingException if the requested padding mechanism
     * is not "NoPadding".
     */
    protected void engineSetPadding(String padding)
        throws NoSuchPaddingException {
        if (!padding.equalsIgnoreCase("NoPadding")) {
            throw new NoSuchPaddingException(padding + " cannot be used");
        }
    }

    /**
     * Returns the block size (in bytes), i.e. 8 bytes.
     *
     * @return the block size (in bytes), i.e. 8 bytes.
     */
    protected int engineGetBlockSize() {
        return DESConstants.DES_BLOCK_SIZE;
    }

    /**
     * Returns the length in bytes that an output buffer would need to be
     * given the input length <code>inputLen</code> (in bytes).
     *
     * <p>The actual output length of the next <code>update</code> or
     * <code>doFinal</code> call may be smaller than the length returned
     * by this method.
     *
     * @param inputLen the input length (in bytes).
     *
     * @return the required output buffer size (in bytes).
     */
    protected int engineGetOutputSize(int inputLen) {
        // can only return an upper-limit if not initialized yet.
        int result = 0;
        if (decrypting) {
            result = inputLen - 16; // CHECKSUM_LEN + IV_LEN;
        } else {
            result = Math.addExact(inputLen, 16);
        }
        return (result < 0? 0:result);
    }

    /**
     * Returns the initialization vector (IV) in a new buffer.
     *
     * @return the initialization vector, or null if the underlying
     * algorithm does not use an IV, or if the IV has not yet
     * been set.
     */
    protected byte[] engineGetIV() {
        return (iv == null) ? null : iv.clone();
    }

    /**
     * Initializes this cipher with a key and a source of randomness.
     *
     * <p>The cipher only supports the following two operation modes:
     * {@code Cipher.WRAP_MODE}, and {@code Cipher.UNWRAP_MODE}.
     * <p>For modes other than the above two, UnsupportedOperationException
     * will be thrown.
     * <p>If this cipher requires an initialization vector (IV), it will get
     * it from <code>random</code>.
     *
     * @param opmode the operation mode of this cipher. Only
     * <code>WRAP_MODE</code> or <code>UNWRAP_MODE</code>) are accepted.
     * @param key the secret key.
     * @param random the source of randomness.
     *
     * @exception InvalidKeyException if the given key is inappropriate
     * or if parameters are required but not supplied.
     */
    protected void engineInit(int opmode, Key key, SecureRandom random)
        throws InvalidKeyException {
        try {
            engineInit(opmode, key, (AlgorithmParameterSpec) null, random);
        } catch (InvalidAlgorithmParameterException iape) {
            // should never happen
            InvalidKeyException ike =
                new InvalidKeyException("Parameters required");
            ike.initCause(iape);
            throw ike;
        }
    }

    /**
     * Initializes this cipher with a key, a set of algorithm parameters,
     * and a source of randomness.
     *
     * <p>The cipher only supports the following two operation modes:
     * {@code Cipher.WRAP_MODE}, and {@code Cipher.UNWRAP_MODE}.
     * <p>For modes other than the above two, UnsupportedOperationException
     * will be thrown.
     * <p>If this cipher requires an initialization vector (IV), it will get
     * it from <code>random</code>.
     *
     * @param opmode the operation mode of this cipher. Only
     * <code>WRAP_MODE</code> or <code>UNWRAP_MODE</code>) are accepted.
     * @param key the secret key.
     * @param params the algorithm parameters.
     * @param random the source of randomness.
     *
     * @exception InvalidKeyException if the given key is inappropriate.
     * @exception InvalidAlgorithmParameterException if the given algorithm
     * parameters are inappropriate for this cipher.
     */
    protected void engineInit(int opmode, Key key,
                              AlgorithmParameterSpec params,
                              SecureRandom random)
        throws InvalidKeyException, InvalidAlgorithmParameterException {
        byte[] currIv = null;
        if (opmode == Cipher.WRAP_MODE) {
            decrypting = false;
            if (params == null) {
                iv = new byte[IV_LEN];
                if (random == null) {
                    random = SunJCE.getRandom();
                }
                random.nextBytes(iv);
            }
            else if (params instanceof IvParameterSpec) {
                iv = ((IvParameterSpec) params).getIV();
            } else {
                throw new InvalidAlgorithmParameterException
                    ("Wrong parameter type: IV expected");
            }
            currIv = iv;
        } else if (opmode == Cipher.UNWRAP_MODE) {
            if (params != null) {
                throw new InvalidAlgorithmParameterException
                    ("No parameter accepted for unwrapping keys");
            }
            iv = null;
            decrypting = true;
            currIv = IV2;
        } else {
            throw new UnsupportedOperationException("This cipher can " +
                "only be used for key wrapping and unwrapping");
        }
        byte[] encoded = key.getEncoded();
        try {
            cipher.init(decrypting, key.getAlgorithm(), encoded, currIv);
        } finally {
            if (encoded != null) {
                Arrays.fill(encoded, (byte) 0);
            }
        }
        cipherKey = key;
    }

    /**
     * Initializes this cipher with a key, a set of algorithm parameters,
     * and a source of randomness.
     *
     * <p>The cipher only supports the following two operation modes:
     * {@code Cipher.WRAP_MODE}, and {@code Cipher.UNWRAP_MODE}.
     * <p>For modes other than the above two, UnsupportedOperationException
     * will be thrown.
     * <p>If this cipher requires an initialization vector (IV), it will get
     * it from <code>random</code>.
     *
     * @param opmode the operation mode of this cipher. Only
     * <code>WRAP_MODE</code> or <code>UNWRAP_MODE</code>) are accepted.
     * @param key the secret key.
     * @param params the algorithm parameters.
     * @param random the source of randomness.
     *
     * @exception InvalidKeyException if the given key is inappropriate.
     * @exception InvalidAlgorithmParameterException if the given algorithm
     * parameters are inappropriate for this cipher.
     */
    protected void engineInit(int opmode, Key key,
                              AlgorithmParameters params,
                              SecureRandom random)
        throws InvalidKeyException, InvalidAlgorithmParameterException {
        IvParameterSpec ivSpec = null;
        if (params != null) {
            try {
                DESedeParameters paramsEng = new DESedeParameters();
                paramsEng.engineInit(params.getEncoded());
                ivSpec = paramsEng.engineGetParameterSpec(IvParameterSpec.class);
            } catch (Exception ex) {
                InvalidAlgorithmParameterException iape =
                    new InvalidAlgorithmParameterException
                        ("Wrong parameter type: IV expected");
                iape.initCause(ex);
                throw iape;
            }
        }
        engineInit(opmode, key, ivSpec, random);
    }

    /**
     * This operation is not supported by this cipher.
     * Since it's impossible to initialize this cipher given the
     * current Cipher.engineInit(...) implementation,
     * IllegalStateException will always be thrown upon invocation.
     *
     * @param in the input buffer.
     * @param inOffset the offset in <code>in</code> where the input
     * starts.
     * @param inLen the input length.
     *
     * @return n/a.
     *
     * @exception IllegalStateException upon invocation of this method.
     */
    protected byte[] engineUpdate(byte[] in, int inOffset, int inLen) {
        throw new IllegalStateException("Cipher has not been initialized");
    }

    /**
     * This operation is not supported by this cipher.
     * Since it's impossible to initialize this cipher given the
     * current Cipher.engineInit(...) implementation,
     * IllegalStateException will always be thrown upon invocation.
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
    protected int engineUpdate(byte[] in, int inOffset, int inLen,
                               byte[] out, int outOffset)
        throws ShortBufferException {
        throw new IllegalStateException("Cipher has not been initialized");
    }

    /**
     * This operation is not supported by this cipher.
     * Since it's impossible to initialize this cipher given the
     * current Cipher.engineInit(...) implementation,
     * IllegalStateException will always be thrown upon invocation.
     *
     * @param in the input buffer.
     * @param inOffset the offset in <code>in</code> where the input
     * starts.
     * @param inLen the input length.
     *
     * @return the new buffer with the result.
     *
     * @exception IllegalStateException upon invocation of this method.
     */
    protected byte[] engineDoFinal(byte[] in, int inOffset, int inLen)
        throws IllegalBlockSizeException, BadPaddingException {
        throw new IllegalStateException("Cipher has not been initialized");
    }

    /**
     * This operation is not supported by this cipher.
     * Since it's impossible to initialize this cipher given the
     * current Cipher.engineInit(...) implementation,
     * IllegalStateException will always be thrown upon invocation.
     *
     * @param input the input buffer.
     * @param inputOffset the offset in {@code input} where the input
     * starts.
     * @param inputLen the input length.
     * @param output the buffer for the result.
     * @param outputOffset the ofset in {@code output} where the result
     * is stored.
     *
     * @return the number of bytes stored in {@code out}.
     *
     * @exception IllegalStateException upon invocation of this method.
     */
    protected int engineDoFinal(byte[] input, int inputOffset, int inputLen,
                                byte[] output, int outputOffset)
        throws IllegalBlockSizeException, ShortBufferException,
               BadPaddingException {
        throw new IllegalStateException("Cipher has not been initialized");
    }

    /**
     * Returns the parameters used with this cipher.
     * Note that null maybe returned if this cipher does not use any
     * parameters or when it has not be set, e.g. initialized with
     * UNWRAP_MODE but wrapped key data has not been given.
     *
     * @return the parameters used with this cipher; can be null.
     */
    protected AlgorithmParameters engineGetParameters() {
        AlgorithmParameters params = null;
        if (iv != null) {
            String algo = cipherKey.getAlgorithm();
            try {
                params = AlgorithmParameters.getInstance(algo,
                    SunJCE.getInstance());
                params.init(new IvParameterSpec(iv));
            } catch (NoSuchAlgorithmException nsae) {
                // should never happen
                throw new RuntimeException("Cannot find " + algo +
                    " AlgorithmParameters implementation in SunJCE provider");
            } catch (InvalidParameterSpecException ipse) {
                // should never happen
                throw new RuntimeException("IvParameterSpec not supported");
            }
        }
        return params;
    }

    /**
     * Returns the key size of the given key object in number of bits.
     * This cipher always return the same key size as the DESede ciphers.
     *
     * @param key the key object.
     *
     * @return the "effective" key size of the given key object.
     *
     * @exception InvalidKeyException if <code>key</code> is invalid.
     */
    protected int engineGetKeySize(Key key) throws InvalidKeyException {
        byte[] encoded = key.getEncoded();
        Arrays.fill(encoded, (byte)0);
        if (encoded.length != 24) {
            throw new InvalidKeyException("Invalid key length: " +
                encoded.length + " bytes");
        }
        // Return the effective key length
        return 112;
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
    protected byte[] engineWrap(Key key)
        throws IllegalBlockSizeException, InvalidKeyException {
        byte[] keyVal = key.getEncoded();
        if ((keyVal == null) || (keyVal.length == 0)) {
            throw new InvalidKeyException("Cannot get an encoding of " +
                                          "the key to be wrapped");
        }

        byte[] in = new byte[Math.addExact(keyVal.length, CHECKSUM_LEN)];
        byte[] cipherKeyEncoded = cipherKey.getEncoded();
        byte[] out = new byte[Math.addExact(iv.length, in.length)];
        try {
            byte[] cks = getChecksum(keyVal);
            System.arraycopy(keyVal, 0, in, 0, keyVal.length);
            System.arraycopy(cks, 0, in, keyVal.length, CHECKSUM_LEN);

            System.arraycopy(iv, 0, out, 0, iv.length);

            cipher.encrypt(in, 0, in.length, out, iv.length);

            // reverse the array content
            for (int i = 0; i < out.length / 2; i++) {
                byte temp = out[i];
                out[i] = out[out.length - 1 - i];
                out[out.length - 1 - i] = temp;
            }
            try {
                cipher.init(false, cipherKey.getAlgorithm(),
                        cipherKeyEncoded, IV2);
            } catch (InvalidKeyException ike) {
                // should never happen
                throw new RuntimeException("Internal cipher key is corrupted");
            } catch (InvalidAlgorithmParameterException iape) {
                // should never happen
                throw new RuntimeException("Internal cipher IV is invalid");
            }
            byte[] out2 = new byte[out.length];
            cipher.encrypt(out, 0, out.length, out2, 0);

            // restore cipher state to prior to this call
            try {
                cipher.init(decrypting, cipherKey.getAlgorithm(),
                        cipherKeyEncoded, iv);
            } catch (InvalidKeyException ike) {
                // should never happen
                throw new RuntimeException("Internal cipher key is corrupted");
            } catch (InvalidAlgorithmParameterException iape) {
                // should never happen
                throw new RuntimeException("Internal cipher IV is invalid");
            }
            return out2;
        } finally {
            Arrays.fill(keyVal, (byte)0);
            Arrays.fill(in, (byte)0);
            Arrays.fill(out, (byte)0);
            if (cipherKeyEncoded != null) {
                Arrays.fill(cipherKeyEncoded, (byte) 0);
            }
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
    protected Key engineUnwrap(byte[] wrappedKey,
                               String wrappedKeyAlgorithm,
                               int wrappedKeyType)
        throws InvalidKeyException, NoSuchAlgorithmException {
        if (wrappedKey.length == 0) {
            throw new InvalidKeyException("The wrapped key is empty");
        }
        byte[] buffer = new byte[wrappedKey.length];
        cipher.decrypt(wrappedKey, 0, wrappedKey.length, buffer, 0);

        // reverse array content
        for (int i = 0; i < buffer.length/2; i++) {
            byte temp = buffer[i];
            buffer[i] = buffer[buffer.length-1-i];
            buffer[buffer.length-1-i] = temp;
        }
        iv = new byte[IV_LEN];
        System.arraycopy(buffer, 0, iv, 0, iv.length);
        byte[] cipherKeyEncoded = cipherKey.getEncoded();
        byte[] out = null;
        byte[] buffer2 = new byte[buffer.length - iv.length];
        try {
            try {
                cipher.init(true, cipherKey.getAlgorithm(), cipherKeyEncoded,
                        iv);
            } catch (InvalidAlgorithmParameterException iape) {
                throw new InvalidKeyException("IV in wrapped key is invalid");
            }
            cipher.decrypt(buffer, iv.length, buffer2.length,
                    buffer2, 0);
            int keyValLen = buffer2.length - CHECKSUM_LEN;
            byte[] cks = getChecksum(buffer2, 0, keyValLen);
            int offset = keyValLen;
            for (int i = 0; i < CHECKSUM_LEN; i++) {
                if (buffer2[offset + i] != cks[i]) {
                    throw new InvalidKeyException("Checksum comparison failed");
                }
            }
            // restore cipher state to prior to this call
            try {
                cipher.init(decrypting, cipherKey.getAlgorithm(),
                        cipherKeyEncoded, IV2);
            } catch (InvalidAlgorithmParameterException iape) {
                throw new InvalidKeyException("IV in wrapped key is invalid");
            }
            out = new byte[keyValLen];
            System.arraycopy(buffer2, 0, out, 0, keyValLen);
            return ConstructKeys.constructKey(out, wrappedKeyAlgorithm,
                    wrappedKeyType);
        } finally {
            if (out != null) {
                Arrays.fill(out, (byte)0);
            }
            if (cipherKeyEncoded != null) {
                Arrays.fill(cipherKeyEncoded, (byte) 0);
            }
            Arrays.fill(buffer2, (byte)0);
        }
    }

    private static final byte[] getChecksum(byte[] in) {
        return getChecksum(in, 0, in.length);
    }
    private static final byte[] getChecksum(byte[] in, int offset, int len) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("SHA1 message digest not available");
        }
        md.update(in, offset, len);
        byte[] cks = new byte[CHECKSUM_LEN];
        System.arraycopy(md.digest(), 0, cks, 0, cks.length);
        md.reset();
        return cks;
    }
}
