/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * This class represents password-based encryption as defined by the PKCS #5
 * standard.
 *
 * @author Jan Luehe
 *
 *
 * @see javax.crypto.Cipher
 */
final class PBES1Core {

    // the encapsulated DES cipher
    private CipherCore cipher;
    private MessageDigest md;
    private int blkSize;
    private String algo = null;
    private byte[] salt = null;
    private int iCount = 10;

    /**
     * Creates an instance of PBE Cipher using the specified CipherSpi
     * instance.
     *
     */
    PBES1Core(String cipherAlg) throws NoSuchAlgorithmException,
        NoSuchPaddingException {
        algo = cipherAlg;
        if (algo.equals("DES")) {
            cipher = new CipherCore(new DESCrypt(),
                                    DESConstants.DES_BLOCK_SIZE);
        } else if (algo.equals("DESede")) {

            cipher = new CipherCore(new DESedeCrypt(),
                                    DESConstants.DES_BLOCK_SIZE);
        } else {
            throw new NoSuchAlgorithmException("No Cipher implementation " +
                                               "for PBEWithMD5And" + algo);
        }
        cipher.setMode("CBC");
        cipher.setPadding("PKCS5Padding");
        // get instance of MD5
        md = MessageDigest.getInstance("MD5");
    }

    /**
     * Sets the mode of this cipher. This algorithm can only be run in CBC
     * mode.
     *
     * @param mode the cipher mode
     *
     * @exception NoSuchAlgorithmException if the requested cipher mode is
     * invalid
     */
    void setMode(String mode) throws NoSuchAlgorithmException {
        cipher.setMode(mode);
    }

    /**
     * Sets the padding mechanism of this cipher. This algorithm only uses
     * PKCS #5 padding.
     *
     * @param paddingScheme the padding mechanism
     *
     * @exception NoSuchPaddingException if the requested padding mechanism
     * is invalid
     */
    void setPadding(String paddingScheme) throws NoSuchPaddingException {
        cipher.setPadding(paddingScheme);
    }

    /**
     * Returns the block size (in bytes).
     *
     * @return the block size (in bytes)
     */
    int getBlockSize() {
        return DESConstants.DES_BLOCK_SIZE;
    }

    /**
     * Returns the length in bytes that an output buffer would need to be in
     * order to hold the result of the next <code>update</code> or
     * <code>doFinal</code> operation, given the input length
     * <code>inputLen</code> (in bytes).
     *
     * <p>This call takes into account any unprocessed (buffered) data from a
     * previous <code>update</code> call, and padding.
     *
     * <p>The actual output length of the next <code>update</code> or
     * <code>doFinal</code> call may be smaller than the length returned by
     * this method.
     *
     * @param inputLen the input length (in bytes)
     *
     * @return the required output buffer size (in bytes)
     *
     */
    int getOutputSize(int inputLen) {
        return cipher.getOutputSize(inputLen);
    }

    /**
     * Returns the initialization vector (IV) in a new buffer.
     *
     * <p> This is useful in the case where a random IV has been created
     * (see <a href = "#init">init</a>),
     * or in the context of password-based encryption or
     * decryption, where the IV is derived from a user-supplied password.
     *
     * @return the initialization vector in a new buffer, or null if the
     * underlying algorithm does not use an IV, or if the IV has not yet
     * been set.
     */
    byte[] getIV() {
        return cipher.getIV();
    }

    /**
     * Returns the parameters used with this cipher.
     *
     * <p>The returned parameters may be the same that were used to initialize
     * this cipher, or may contain the default set of parameters or a set of
     * randomly generated parameters used by the underlying cipher
     * implementation (provided that the underlying cipher implementation
     * uses a default set of parameters or creates new parameters if it needs
     * parameters but was not initialized with any).
     *
     * @return the parameters used with this cipher, or null if this cipher
     * does not use any parameters.
     */
    AlgorithmParameters getParameters() {
        AlgorithmParameters params = null;
        if (salt == null) {
            salt = new byte[8];
            SunJCE.getRandom().nextBytes(salt);
        }
        PBEParameterSpec pbeSpec = new PBEParameterSpec(salt, iCount);
        try {
            params = AlgorithmParameters.getInstance("PBEWithMD5And" +
                (algo.equalsIgnoreCase("DES")? "DES":"TripleDES"),
                SunJCE.getInstance());
            params.init(pbeSpec);
        } catch (NoSuchAlgorithmException nsae) {
            // should never happen
            throw new RuntimeException("SunJCE called, but not configured");
        } catch (InvalidParameterSpecException ipse) {
            // should never happen
            throw new RuntimeException("PBEParameterSpec not supported");
        }
        return params;
    }

    /**
     * Initializes this cipher with a key, a set of
     * algorithm parameters, and a source of randomness.
     * The cipher is initialized for one of the following four operations:
     * encryption, decryption, key wrapping or key unwrapping, depending on
     * the value of <code>opmode</code>.
     *
     * <p>If this cipher (including its underlying feedback or padding scheme)
     * requires any random bytes, it will get them from <code>random</code>.
     *
     * @param opmode the operation mode of this cipher (this is one of
     * the following:
     * <code>ENCRYPT_MODE</code>, <code>DECRYPT_MODE</code>),
     * <code>WRAP_MODE</code> or <code>UNWRAP_MODE</code>)
     * @param key the encryption key
     * @param params the algorithm parameters
     * @param random the source of randomness
     *
     * @exception InvalidKeyException if the given key is inappropriate for
     * initializing this cipher
     * @exception InvalidAlgorithmParameterException if the given algorithm
     * parameters are inappropriate for this cipher
     */
    void init(int opmode, Key key, AlgorithmParameterSpec params,
              SecureRandom random)
        throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (((opmode == Cipher.DECRYPT_MODE) ||
             (opmode == Cipher.UNWRAP_MODE)) && (params == null)) {
            throw new InvalidAlgorithmParameterException("Parameters "
                                                         + "missing");
        }
        if (key == null) {
            throw new InvalidKeyException("Null key");
        }

        byte[] derivedKey;
        byte[] passwdBytes = key.getEncoded();
        try {
            if ((passwdBytes == null) ||
                    !(key.getAlgorithm().regionMatches(true, 0, "PBE", 0, 3))) {
                throw new InvalidKeyException("Missing password");
            }

            if (params == null) {
                // create random salt and use default iteration count
                salt = new byte[8];
                random.nextBytes(salt);
            } else {
                if (!(params instanceof PBEParameterSpec)) {
                    throw new InvalidAlgorithmParameterException
                            ("Wrong parameter type: PBE expected");
                }
                salt = ((PBEParameterSpec) params).getSalt();
                // salt must be 8 bytes long (by definition)
                if (salt.length != 8) {
                    throw new InvalidAlgorithmParameterException
                            ("Salt must be 8 bytes long");
                }
                iCount = ((PBEParameterSpec) params).getIterationCount();
                if (iCount <= 0) {
                    throw new InvalidAlgorithmParameterException
                            ("IterationCount must be a positive number");
                }
            }
            derivedKey = deriveCipherKey(passwdBytes);
        } finally {
            if (passwdBytes != null) Arrays.fill(passwdBytes, (byte) 0x00);
        }
        // use all but the last 8 bytes as the key value
        SecretKeySpec cipherKey = new SecretKeySpec(derivedKey, 0,
                                                    derivedKey.length-8, algo);
        // use the last 8 bytes as the IV
        IvParameterSpec ivSpec = new IvParameterSpec(derivedKey,
                                                     derivedKey.length-8,
                                                     8);
        // initialize the underlying cipher
        cipher.init(opmode, cipherKey, ivSpec, random);
    }

    private byte[] deriveCipherKey(byte[] passwdBytes) {

        byte[] result = null;

        if (algo.equals("DES")) {
            // P || S (password concatenated with salt)
            md.update(passwdBytes);
            md.update(salt);
            // digest P || S with iCount iterations
            // first iteration
            byte[] toBeHashed = md.digest(); // this resets the digest
            // remaining (iCount - 1) iterations
            for (int i = 1; i < iCount; ++i) {
                md.update(toBeHashed);
                try {
                    md.digest(toBeHashed, 0, toBeHashed.length);
                } catch (DigestException e) {
                    throw new ProviderException("Internal error", e);
                }
            }
            result = toBeHashed;
        } else if (algo.equals("DESede")) {
            // if the 2 salt halves are the same, invert one of them
            int i;
            for (i=0; i<4; i++) {
                if (salt[i] != salt[i+4])
                    break;
            }
            if (i==4) { // same, invert 1st half
                for (i=0; i<2; i++) {
                    byte tmp = salt[i];
                    salt[i] = salt[3-i];
                    salt[3-i] = tmp;
                }
            }

            // Now digest each half (concatenated with password). For each
            // half, go through the loop as many times as specified by the
            // iteration count parameter (inner for loop).
            // Concatenate the output from each digest round with the
            // password, and use the result as the input to the next digest
            // operation.
            byte[] toBeHashed = null;
            result = new byte[DESedeKeySpec.DES_EDE_KEY_LEN +
                              DESConstants.DES_BLOCK_SIZE];
            for (i = 0; i < 2; i++) {
                // first iteration
                md.update(salt, i * (salt.length / 2), salt.length / 2);
                md.update(passwdBytes);
                toBeHashed = md.digest();
                // remaining (iCount - 1) iterations
                for (int j = 1; j < iCount; ++j) {
                    md.update(toBeHashed);
                    md.update(passwdBytes);
                    try {
                        md.digest(toBeHashed, 0, toBeHashed.length);
                    } catch (DigestException e) {
                        throw new ProviderException("Internal error", e);
                    }
                }
                System.arraycopy(toBeHashed, 0, result, i*16,
                                 toBeHashed.length);
            }
        }
        // clear data used in message
        md.reset();
        return result;
    }

    void init(int opmode, Key key, AlgorithmParameters params,
              SecureRandom random)
        throws InvalidKeyException, InvalidAlgorithmParameterException {
        PBEParameterSpec pbeSpec = null;
        if (params != null) {
            try {
                pbeSpec = params.getParameterSpec(PBEParameterSpec.class);
            } catch (InvalidParameterSpecException ipse) {
                throw new InvalidAlgorithmParameterException("Wrong parameter "
                                                             + "type: PBE "
                                                             + "expected");
            }
        }
        init(opmode, key, pbeSpec, random);
    }

    /**
     * Continues a multiple-part encryption or decryption operation
     * (depending on how this cipher was initialized), processing another data
     * part.
     *
     * <p>The first <code>inputLen</code> bytes in the <code>input</code>
     * buffer, starting at <code>inputOffset</code>, are processed, and the
     * result is stored in a new buffer.
     *
     * @param input the input buffer
     * @param inputOffset the offset in <code>input</code> where the input
     * starts
     * @param inputLen the input length
     *
     * @return the new buffer with the result
     *
     */
    byte[] update(byte[] input, int inputOffset, int inputLen) {
        return cipher.update(input, inputOffset, inputLen);
    }

    /**
     * Continues a multiple-part encryption or decryption operation
     * (depending on how this cipher was initialized), processing another data
     * part.
     *
     * <p>The first <code>inputLen</code> bytes in the <code>input</code>
     * buffer, starting at <code>inputOffset</code>, are processed, and the
     * result is stored in the <code>output</code> buffer, starting at
     * <code>outputOffset</code>.
     *
     * @param input the input buffer
     * @param inputOffset the offset in <code>input</code> where the input
     * starts
     * @param inputLen the input length
     * @param output the buffer for the result
     * @param outputOffset the offset in <code>output</code> where the result
     * is stored
     *
     * @return the number of bytes stored in <code>output</code>
     *
     * @exception ShortBufferException if the given output buffer is too small
     * to hold the result
     */
    int update(byte[] input, int inputOffset, int inputLen,
               byte[] output, int outputOffset)
        throws ShortBufferException {
        return cipher.update(input, inputOffset, inputLen,
                             output, outputOffset);
    }

    /**
     * Encrypts or decrypts data in a single-part operation,
     * or finishes a multiple-part operation.
     * The data is encrypted or decrypted, depending on how this cipher was
     * initialized.
     *
     * <p>The first <code>inputLen</code> bytes in the <code>input</code>
     * buffer, starting at <code>inputOffset</code>, and any input bytes that
     * may have been buffered during a previous <code>update</code> operation,
     * are processed, with padding (if requested) being applied.
     * The result is stored in a new buffer.
     *
     * <p>The cipher is reset to its initial state (uninitialized) after this
     * call.
     *
     * @param input the input buffer
     * @param inputOffset the offset in <code>input</code> where the input
     * starts
     * @param inputLen the input length
     *
     * @return the new buffer with the result
     *
     * @exception IllegalBlockSizeException if this cipher is a block cipher,
     * no padding has been requested (only in encryption mode), and the total
     * input length of the data processed by this cipher is not a multiple of
     * block size
     * @exception BadPaddingException if decrypting and padding is chosen,
     * but the last input data does not have proper padding bytes.
     */
    byte[] doFinal(byte[] input, int inputOffset, int inputLen)
        throws IllegalBlockSizeException, BadPaddingException {
        return cipher.doFinal(input, inputOffset, inputLen);
    }

    /**
     * Encrypts or decrypts data in a single-part operation,
     * or finishes a multiple-part operation.
     * The data is encrypted or decrypted, depending on how this cipher was
     * initialized.
     *
     * <p>The first <code>inputLen</code> bytes in the <code>input</code>
     * buffer, starting at <code>inputOffset</code>, and any input bytes that
     * may have been buffered during a previous <code>update</code> operation,
     * are processed, with padding (if requested) being applied.
     * The result is stored in the <code>output</code> buffer, starting at
     * <code>outputOffset</code>.
     *
     * <p>The cipher is reset to its initial state (uninitialized) after this
     * call.
     *
     * @param input the input buffer
     * @param inputOffset the offset in <code>input</code> where the input
     * starts
     * @param inputLen the input length
     * @param output the buffer for the result
     * @param outputOffset the offset in <code>output</code> where the result
     * is stored
     *
     * @return the number of bytes stored in <code>output</code>
     *
     * @exception IllegalBlockSizeException if this cipher is a block cipher,
     * no padding has been requested (only in encryption mode), and the total
     * input length of the data processed by this cipher is not a multiple of
     * block size
     * @exception ShortBufferException if the given output buffer is too small
     * to hold the result
     * @exception BadPaddingException if decrypting and padding is chosen,
     * but the last input data does not have proper padding bytes.
     */
    int doFinal(byte[] input, int inputOffset, int inputLen,
                byte[] output, int outputOffset)
        throws ShortBufferException, IllegalBlockSizeException,
               BadPaddingException {
        return cipher.doFinal(input, inputOffset, inputLen,
                                    output, outputOffset);
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
    byte[] wrap(Key key)
        throws IllegalBlockSizeException, InvalidKeyException {
        byte[] result = null;
        byte[] encodedKey = null;
        try {
            encodedKey = key.getEncoded();
            if ((encodedKey == null) || (encodedKey.length == 0)) {
                throw new InvalidKeyException("Cannot get an encoding of " +
                                              "the key to be wrapped");
            }

            result = doFinal(encodedKey, 0, encodedKey.length);
        } catch (BadPaddingException e) {
            // Should never happen
        } finally {
            if (encodedKey != null) Arrays.fill(encodedKey, (byte)0x00);
        }

        return result;
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
    Key unwrap(byte[] wrappedKey,
               String wrappedKeyAlgorithm,
               int wrappedKeyType)
        throws InvalidKeyException, NoSuchAlgorithmException {
        try {
            byte[] encodedKey = doFinal(wrappedKey, 0, wrappedKey.length);
            try {
                return ConstructKeys.constructKey(encodedKey, wrappedKeyAlgorithm,
                        wrappedKeyType);
            } finally {
                Arrays.fill(encodedKey, (byte)0);
            }
        } catch (BadPaddingException ePadding) {
            throw new InvalidKeyException("The wrapped key is not padded " +
                                          "correctly");
        } catch (IllegalBlockSizeException eBlockSize) {
            throw new InvalidKeyException("The wrapped key does not have " +
                                          "the correct length");
        }
    }
}
