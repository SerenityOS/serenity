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

import java.util.Arrays;
import java.util.Locale;

import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.BadPaddingException;

/**
 * This class represents the symmetric algorithms in its various modes
 * (<code>ECB</code>, <code>CFB</code>, <code>OFB</code>, <code>CBC</code>,
 * <code>PCBC</code>, <code>CTR</code>, and <code>CTS</code>) and
 * padding schemes (<code>PKCS5Padding</code>, <code>NoPadding</code>,
 * <code>ISO10126Padding</code>).
 *
 * @author Gigi Ankeny
 * @author Jan Luehe
 * @see ElectronicCodeBook
 * @see CipherFeedback
 * @see OutputFeedback
 * @see CipherBlockChaining
 * @see PCBC
 * @see CounterMode
 * @see CipherTextStealing
 */

final class CipherCore {

    /*
     * internal buffer
     */
    private byte[] buffer = null;

    /*
     * block size of cipher in bytes
     */
    private int blockSize = 0;

    /*
     * unit size (number of input bytes that can be processed at a time)
     */
    private int unitBytes = 0;

    /*
     * index of the content size left in the buffer
     */
    private int buffered = 0;

    /*
     * minimum number of bytes in the buffer required for
     * FeedbackCipher.encryptFinal()/decryptFinal() call.
     * update() must buffer this many bytes before starting
     * to encrypt/decrypt data.
     * currently, only the following cases have non-zero values:
     * 1) CTS mode - due to its special handling on the last two blocks
     * (the last one may be incomplete).
     */
    private int minBytes = 0;

    /*
     * number of bytes needed to make the total input length a multiple
     * of the blocksize (this is used in feedback mode, when the number of
     * input bytes that are processed at a time is different from the block
     * size)
     */
    private int diffBlocksize = 0;

    /*
     * padding class
     */
    private Padding padding = null;

    /*
     * internal cipher engine
     */
    private FeedbackCipher cipher = null;

    /*
     * the cipher mode
     */
    private int cipherMode = ECB_MODE;

    /*
     * are we encrypting or decrypting?
     */
    private boolean decrypting = false;

    /*
     * Block Mode constants
     */
    private static final int ECB_MODE = 0;
    private static final int CBC_MODE = 1;
    private static final int CFB_MODE = 2;
    private static final int OFB_MODE = 3;
    private static final int PCBC_MODE = 4;
    private static final int CTR_MODE = 5;
    private static final int CTS_MODE = 6;

    /**
     * Creates an instance of CipherCore with default ECB mode and
     * PKCS5Padding.
     */
    CipherCore(SymmetricCipher impl, int blkSize) {
        blockSize = blkSize;
        unitBytes = blkSize;
        diffBlocksize = blkSize;

        /*
         * The buffer should be usable for all cipher mode and padding
         * schemes. Thus, it has to be at least (blockSize+1) for CTS.
         * In decryption mode, it also hold the possible padding block.
         */
        buffer = new byte[blockSize*2];

        // set mode and padding
        cipher = new ElectronicCodeBook(impl);
        padding = new PKCS5Padding(blockSize);
    }

    /**
     * Sets the mode of this cipher.
     *
     * @param mode the cipher mode
     *
     * @exception NoSuchAlgorithmException if the requested cipher mode does
     * not exist for this cipher
     */
    void setMode(String mode) throws NoSuchAlgorithmException {
        if (mode == null)
            throw new NoSuchAlgorithmException("null mode");

        String modeUpperCase = mode.toUpperCase(Locale.ENGLISH);

        if (modeUpperCase.equals("ECB")) {
            return;
        }

        SymmetricCipher rawImpl = cipher.getEmbeddedCipher();
        if (modeUpperCase.equals("CBC")) {
            cipherMode = CBC_MODE;
            cipher = new CipherBlockChaining(rawImpl);
        } else if (modeUpperCase.equals("CTS")) {
            cipherMode = CTS_MODE;
            cipher = new CipherTextStealing(rawImpl);
            minBytes = blockSize+1;
            padding = null;
        } else if (modeUpperCase.equals("CTR")) {
            cipherMode = CTR_MODE;
            cipher = new CounterMode(rawImpl);
            unitBytes = 1;
            padding = null;
        } else if (modeUpperCase.startsWith("CFB")) {
            cipherMode = CFB_MODE;
            unitBytes = getNumOfUnit(mode, "CFB".length(), blockSize);
            cipher = new CipherFeedback(rawImpl, unitBytes);
        } else if (modeUpperCase.startsWith("OFB")) {
            cipherMode = OFB_MODE;
            unitBytes = getNumOfUnit(mode, "OFB".length(), blockSize);
            cipher = new OutputFeedback(rawImpl, unitBytes);
        } else if (modeUpperCase.equals("PCBC")) {
            cipherMode = PCBC_MODE;
            cipher = new PCBC(rawImpl);
        }
        else {
            throw new NoSuchAlgorithmException("Cipher mode: " + mode
                                               + " not found");
        }
    }

    private static int getNumOfUnit(String mode, int offset, int blockSize)
        throws NoSuchAlgorithmException {
        int result = blockSize; // use blockSize as default value
        if (mode.length() > offset) {
            int numInt;
            try {
                Integer num = Integer.valueOf(mode.substring(offset));
                numInt = num.intValue();
                result = numInt >> 3;
            } catch (NumberFormatException e) {
                throw new NoSuchAlgorithmException
                    ("Algorithm mode: " + mode + " not implemented");
            }
            if ((numInt % 8 != 0) || (result > blockSize)) {
                throw new NoSuchAlgorithmException
                    ("Invalid algorithm mode: " + mode);
            }
        }
        return result;
    }

    /**
     * Sets the padding mechanism of this cipher.
     *
     * @param paddingScheme the padding mechanism
     *
     * @exception NoSuchPaddingException if the requested padding mechanism
     * does not exist
     */
    void setPadding(String paddingScheme)
        throws NoSuchPaddingException
    {
        if (paddingScheme == null) {
            throw new NoSuchPaddingException("null padding");
        }
        if (paddingScheme.equalsIgnoreCase("NoPadding")) {
            padding = null;
        } else if (paddingScheme.equalsIgnoreCase("ISO10126Padding")) {
            padding = new ISO10126Padding(blockSize);
        } else if (paddingScheme.equalsIgnoreCase("PKCS5Padding")) {
            padding = new PKCS5Padding(blockSize);
        } else {
            throw new NoSuchPaddingException("Padding: " + paddingScheme
                                             + " not implemented");
        }
        if ((padding != null) &&
            ((cipherMode == CTR_MODE) || (cipherMode == CTS_MODE))) {
            padding = null;
            String modeStr = null;
            switch (cipherMode) {
            case CTR_MODE:
                modeStr = "CTR";
                break;
            case CTS_MODE:
                modeStr = "CTS";
                break;
            default:
                // should never happen
            }
            if (modeStr != null) {
                throw new NoSuchPaddingException
                    (modeStr + " mode must be used with NoPadding");
            }
        }
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
     */
    int getOutputSize(int inputLen) {
        // estimate based on the maximum
        return getOutputSizeByOperation(inputLen, true);
    }

    private int getOutputSizeByOperation(int inputLen, boolean isDoFinal) {
        int totalLen = buffered;
        totalLen = Math.addExact(totalLen, inputLen);
        if (padding != null && !decrypting) {
            if (unitBytes != blockSize) {
                if (totalLen < diffBlocksize) {
                    totalLen = diffBlocksize;
                } else {
                    int residue = (totalLen - diffBlocksize) % blockSize;
                    totalLen = Math.addExact(totalLen, (blockSize - residue));
                }
            } else {
                totalLen = Math.addExact(totalLen, padding.padLength(totalLen));
            }
        }
        return totalLen;
    }

    /**
     * Returns the initialization vector (IV) in a new buffer.
     *
     * <p>This is useful in the case where a random IV has been created
     * (see <a href = "#init">init</a>),
     * or in the context of password-based encryption or
     * decryption, where the IV is derived from a user-provided password.
     *
     * @return the initialization vector in a new buffer, or null if the
     * underlying algorithm does not use an IV, or if the IV has not yet
     * been set.
     */
    byte[] getIV() {
        byte[] iv = cipher.getIV();
        return (iv == null) ? null : iv.clone();
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
    AlgorithmParameters getParameters(String algName) {
        if (cipherMode == ECB_MODE) {
            return null;
        }
        AlgorithmParameters params = null;
        AlgorithmParameterSpec spec;
        byte[] iv = getIV();
        if (iv == null) {
            iv = new byte[blockSize];
            SunJCE.getRandom().nextBytes(iv);
        }
        if (algName.equals("RC2")) {
            RC2Crypt rawImpl = (RC2Crypt) cipher.getEmbeddedCipher();
            spec = new RC2ParameterSpec
                (rawImpl.getEffectiveKeyBits(), iv);
        } else {
            spec = new IvParameterSpec(iv);
        }
        try {
            params = AlgorithmParameters.getInstance(algName,
                    SunJCE.getInstance());
            params.init(spec);
        } catch (NoSuchAlgorithmException nsae) {
            // should never happen
            throw new RuntimeException("Cannot find " + algName +
                " AlgorithmParameters implementation in SunJCE provider");
        } catch (InvalidParameterSpecException ipse) {
            // should never happen
            throw new RuntimeException(spec.getClass() + " not supported");
        }
        return params;
    }

    /**
     * Initializes this cipher with a key and a source of randomness.
     *
     * <p>The cipher is initialized for one of the following four operations:
     * encryption, decryption, key wrapping or key unwrapping, depending on
     * the value of <code>opmode</code>.
     *
     * <p>If this cipher requires an initialization vector (IV), it will get
     * it from <code>random</code>.
     * This behaviour should only be used in encryption or key wrapping
     * mode, however.
     * When initializing a cipher that requires an IV for decryption or
     * key unwrapping, the IV
     * (same IV that was used for encryption or key wrapping) must be provided
     * explicitly as a
     * parameter, in order to get the correct result.
     *
     * <p>This method also cleans existing buffer and other related state
     * information.
     *
     * @param opmode the operation mode of this cipher (this is one of
     * the following:
     * <code>ENCRYPT_MODE</code>, <code>DECRYPT_MODE</code>,
     * <code>WRAP_MODE</code> or <code>UNWRAP_MODE</code>)
     * @param key the secret key
     * @param random the source of randomness
     *
     * @exception InvalidKeyException if the given key is inappropriate for
     * initializing this cipher
     */
    void init(int opmode, Key key, SecureRandom random)
            throws InvalidKeyException {
        try {
            init(opmode, key, (AlgorithmParameterSpec)null, random);
        } catch (InvalidAlgorithmParameterException e) {
            throw new InvalidKeyException(e.getMessage());
        }
    }

    /**
     * Initializes this cipher with a key, a set of
     * algorithm parameters, and a source of randomness.
     *
     * <p>The cipher is initialized for one of the following four operations:
     * encryption, decryption, key wrapping or key unwrapping, depending on
     * the value of <code>opmode</code>.
     *
     * <p>If this cipher (including its underlying feedback or padding scheme)
     * requires any random bytes, it will get them from <code>random</code>.
     *
     * @param opmode the operation mode of this cipher (this is one of
     * the following:
     * <code>ENCRYPT_MODE</code>, <code>DECRYPT_MODE</code>,
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
        decrypting = (opmode == Cipher.DECRYPT_MODE)
                  || (opmode == Cipher.UNWRAP_MODE);

        byte[] keyBytes = getKeyBytes(key);
        byte[] ivBytes = null;
        try {
            if (params != null) {
                if (params instanceof IvParameterSpec) {
                    ivBytes = ((IvParameterSpec) params).getIV();
                    if ((ivBytes == null) || (ivBytes.length != blockSize)) {
                        throw new InvalidAlgorithmParameterException
                                ("Wrong IV length: must be " + blockSize +
                                        " bytes long");
                    }
                } else if (params instanceof RC2ParameterSpec) {
                    ivBytes = ((RC2ParameterSpec) params).getIV();
                    if ((ivBytes != null) && (ivBytes.length != blockSize)) {
                        throw new InvalidAlgorithmParameterException
                                ("Wrong IV length: must be " + blockSize +
                                        " bytes long");
                    }
                } else {
                    throw new InvalidAlgorithmParameterException
                            ("Unsupported parameter: " + params);
                }
            }
            if (cipherMode == ECB_MODE) {
                if (ivBytes != null) {
                    throw new InvalidAlgorithmParameterException
                            ("ECB mode cannot use IV");
                }
            } else if (ivBytes == null) {
                if (decrypting) {
                    throw new InvalidAlgorithmParameterException("Parameters "
                            + "missing");
                }

                if (random == null) {
                    random = SunJCE.getRandom();
                }

                ivBytes = new byte[blockSize];
                random.nextBytes(ivBytes);
            }

            buffered = 0;
            diffBlocksize = blockSize;

            String algorithm = key.getAlgorithm();
            cipher.init(decrypting, algorithm, keyBytes, ivBytes);
        } finally {
            Arrays.fill(keyBytes, (byte)0);
        }
    }

    void init(int opmode, Key key, AlgorithmParameters params,
              SecureRandom random)
        throws InvalidKeyException, InvalidAlgorithmParameterException {
        AlgorithmParameterSpec spec = null;
        String paramType = null;
        if (params != null) {
            try {
                // NOTE: RC2 parameters are always handled through
                // init(..., AlgorithmParameterSpec,...) method, so
                // we can assume IvParameterSpec type here.
                paramType = "IV";
                spec = params.getParameterSpec(IvParameterSpec.class);
            } catch (InvalidParameterSpecException ipse) {
                throw new InvalidAlgorithmParameterException
                    ("Wrong parameter type: " + paramType + " expected");
            }
        }
        init(opmode, key, spec, random);
    }

    /**
     * Return the key bytes of the specified key. Throw an InvalidKeyException
     * if the key is not usable.
     */
    static byte[] getKeyBytes(Key key) throws InvalidKeyException {
        if (key == null) {
            throw new InvalidKeyException("No key given");
        }
        // note: key.getFormat() may return null
        if (!"RAW".equalsIgnoreCase(key.getFormat())) {
            throw new InvalidKeyException("Wrong format: RAW bytes needed");
        }
        byte[] keyBytes = key.getEncoded();
        if (keyBytes == null) {
            throw new InvalidKeyException("RAW key bytes missing");
        }
        return keyBytes;
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
     * @exception IllegalStateException if this cipher is in a wrong state
     * (e.g., has not been initialized)
     */
    byte[] update(byte[] input, int inputOffset, int inputLen) {

        byte[] output = null;
        try {
            output = new byte[getOutputSizeByOperation(inputLen, false)];
            int len = update(input, inputOffset, inputLen, output,
                             0);
            if (len == output.length) {
                return output;
            } else {
                byte[] copy = Arrays.copyOf(output, len);
                if (decrypting) {
                    // Zero out internal buffer which is no longer required
                    Arrays.fill(output, (byte) 0x00);
                }
                return copy;
            }
        } catch (ShortBufferException e) {
            // should never happen
            throw new ProviderException("Unexpected exception", e);
        }
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
    int update(byte[] input, int inputOffset, int inputLen, byte[] output,
               int outputOffset) throws ShortBufferException {

        // figure out how much can be sent to crypto function
        int len = Math.addExact(buffered, inputLen);
        len -= minBytes;
        if (padding != null && decrypting) {
            // do not include the padding bytes when decrypting
            len -= blockSize;
        }
        // do not count the trailing bytes which do not make up a unit
        len = (len > 0 ? (len - (len % unitBytes)) : 0);

        // check output buffer capacity
        if (output == null || (output.length - outputOffset) < len) {
            throw new ShortBufferException("Output buffer must be "
                                           + "(at least) " + len
                                           + " bytes long");
        }

        int outLen = 0;
        if (len != 0) { // there is some work to do
            if ((input == output)
                 && (outputOffset - inputOffset < inputLen)
                 && (inputOffset - outputOffset < buffer.length)) {
                // copy 'input' out to avoid its content being
                // overwritten prematurely.
                input = Arrays.copyOfRange(input, inputOffset,
                    Math.addExact(inputOffset, inputLen));
                inputOffset = 0;
            }
            if (len <= buffered) {
                // all to-be-processed data are from 'buffer'
                if (decrypting) {
                    outLen = cipher.decrypt(buffer, 0, len, output, outputOffset);
                } else {
                    outLen = cipher.encrypt(buffer, 0, len, output, outputOffset);
                }
                buffered -= len;
                if (buffered != 0) {
                    System.arraycopy(buffer, len, buffer, 0, buffered);
                }
            } else { // len > buffered
                int inputConsumed = len - buffered;
                int temp;
                if (buffered > 0) {
                    int bufferCapacity = buffer.length - buffered;
                    if (bufferCapacity != 0) {
                        temp = Math.min(bufferCapacity, inputConsumed);
                        if (unitBytes != blockSize) {
                            temp -= (Math.addExact(buffered, temp) % unitBytes);
                        }
                        System.arraycopy(input, inputOffset, buffer, buffered, temp);
                        inputOffset = Math.addExact(inputOffset, temp);
                        inputConsumed -= temp;
                        inputLen -= temp;
                        buffered = Math.addExact(buffered, temp);
                    }
                    // process 'buffer'. When finished we can null out 'buffer'
                    // Only necessary to null out if buffer holds data for encryption
                    if (decrypting) {
                         outLen = cipher.decrypt(buffer, 0, buffered, output, outputOffset);
                    } else {
                         outLen = cipher.encrypt(buffer, 0, buffered, output, outputOffset);
                         //encrypt mode. Zero out internal (input) buffer
                         Arrays.fill(buffer, (byte) 0x00);
                    }
                    outputOffset = Math.addExact(outputOffset, outLen);
                    buffered = 0;
                }
                if (inputConsumed > 0) { // still has input to process
                    if (decrypting) {
                        outLen += cipher.decrypt(input, inputOffset, inputConsumed,
                            output, outputOffset);
                    } else {
                        outLen += cipher.encrypt(input, inputOffset, inputConsumed,
                            output, outputOffset);
                    }
                    inputOffset += inputConsumed;
                    inputLen -= inputConsumed;
                }
            }
            // Let's keep track of how many bytes are needed to make
            // the total input length a multiple of blocksize when
            // padding is applied
            if (unitBytes != blockSize) {
                if (len < diffBlocksize) {
                    diffBlocksize -= len;
                } else {
                    diffBlocksize = blockSize -
                        ((len - diffBlocksize) % blockSize);
                }
            }
        }
        // Store remaining input into 'buffer' again
        if (inputLen > 0) {
            System.arraycopy(input, inputOffset, buffer, buffered,
                             inputLen);
            buffered = Math.addExact(buffered, inputLen);
        }
        return outLen;
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
     * @exception BadPaddingException if this cipher is in decryption mode,
     * and (un)padding has been requested, but the decrypted data is not
     * bounded by the appropriate padding bytes
     */
    byte[] doFinal(byte[] input, int inputOffset, int inputLen)
        throws IllegalBlockSizeException, BadPaddingException {
        try {
            byte[] output = new byte[getOutputSizeByOperation(inputLen, true)];
            byte[] finalBuf = prepareInputBuffer(input, inputOffset,
                    inputLen, output, 0);
            int finalOffset = (finalBuf == input) ? inputOffset : 0;
            int finalBufLen = (finalBuf == input) ? inputLen : finalBuf.length;

            int outLen = fillOutputBuffer(finalBuf, finalOffset, output, 0,
                    finalBufLen, input);

            endDoFinal();
            if (outLen < output.length) {
                byte[] copy = Arrays.copyOf(output, outLen);
                if (decrypting) {
                    // Zero out internal (output) array
                    Arrays.fill(output, (byte) 0x00);
                }
                return copy;
            } else {
                return output;
            }
        } catch (ShortBufferException e) {
            // never thrown
            throw new ProviderException("Unexpected exception", e);
        }
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
     * @exception BadPaddingException if this cipher is in decryption mode,
     * and (un)padding has been requested, but the decrypted data is not
     * bounded by the appropriate padding bytes
     */
    int doFinal(byte[] input, int inputOffset, int inputLen, byte[] output,
                int outputOffset)
        throws IllegalBlockSizeException, ShortBufferException,
               BadPaddingException {

        int estOutSize = getOutputSizeByOperation(inputLen, true);
        int outputCapacity = checkOutputCapacity(output, outputOffset,
                estOutSize);
        int offset = outputOffset;
        byte[] finalBuf = prepareInputBuffer(input, inputOffset,
                inputLen, output, outputOffset);
        byte[] internalOutput = null; // for decrypting only

        int finalOffset = (finalBuf == input) ? inputOffset : 0;
        int finalBufLen = (finalBuf == input) ? inputLen : finalBuf.length;

        if (decrypting) {
            // if the size of specified output buffer is less than
            // the length of the cipher text, then the current
            // content of cipher has to be preserved in order for
            // users to retry the call with a larger buffer in the
            // case of ShortBufferException.
            if (outputCapacity < estOutSize) {
                cipher.save();
            }
            // create temporary output buffer if the estimated size is larger
            // than the user-provided buffer.
            internalOutput = new byte[estOutSize];
            offset = 0;
        }

        byte[] outBuffer = (internalOutput != null) ? internalOutput : output;
        int outLen = fillOutputBuffer(finalBuf, finalOffset, outBuffer,
                offset, finalBufLen, input);

        if (decrypting) {

            if (outputCapacity < outLen) {
                // restore so users can retry with a larger buffer
                cipher.restore();
                throw new ShortBufferException("Output buffer too short: "
                    + (outputCapacity) + " bytes given, " + outLen
                    + " bytes needed");
            }
            // copy the result into user-supplied output buffer
            if (internalOutput != null) {
                System.arraycopy(internalOutput, 0, output, outputOffset,
                    outLen);
                // decrypt mode. Zero out output data that's not required
                Arrays.fill(internalOutput, (byte) 0x00);
            }
        }
        endDoFinal();
        return outLen;
    }

    private void endDoFinal() {
        buffered = 0;
        diffBlocksize = blockSize;
        if (cipherMode != ECB_MODE) {
            cipher.reset();
        }
    }

    private int unpad(int outLen, int off, byte[] outWithPadding)
            throws BadPaddingException {
        int padStart = padding.unpad(outWithPadding, off, outLen);
        if (padStart < 0) {
            throw new BadPaddingException("Given final block not " +
            "properly padded. Such issues can arise if a bad key " +
            "is used during decryption.");
        }
        return padStart - off;
    }

    private byte[] prepareInputBuffer(byte[] input, int inputOffset,
                      int inputLen, byte[] output, int outputOffset)
                      throws IllegalBlockSizeException, ShortBufferException {
        // calculate total input length
        int len = Math.addExact(buffered, inputLen);
        // calculate padding length
        int totalLen = len;
        int paddingLen = 0;
        // will the total input length be a multiple of blockSize?
        if (unitBytes != blockSize) {
            if (totalLen < diffBlocksize) {
                paddingLen = diffBlocksize - totalLen;
            } else {
                paddingLen = blockSize -
                    ((totalLen - diffBlocksize) % blockSize);
            }
        } else if (padding != null) {
            paddingLen = padding.padLength(totalLen);
        }

        if (decrypting && (padding != null) &&
            (paddingLen > 0) && (paddingLen != blockSize)) {
            throw new IllegalBlockSizeException
                ("Input length must be multiple of " + blockSize +
                 " when decrypting with padded cipher");
        }

        /*
         * prepare the final input, assemble a new buffer if any
         * of the following is true:
         *  - 'input' and 'output' are the same buffer
         *  - there are internally buffered bytes
         *  - doing encryption and padding is needed
         */
        if ((buffered != 0) || (!decrypting && padding != null) ||
            ((input == output)
              && (outputOffset - inputOffset < inputLen)
              && (inputOffset - outputOffset < buffer.length))) {
            byte[] finalBuf;
            if (decrypting || padding == null) {
                paddingLen = 0;
            }
            finalBuf = new byte[Math.addExact(len, paddingLen)];
            if (buffered != 0) {
                System.arraycopy(buffer, 0, finalBuf, 0, buffered);
                if (!decrypting) {
                    // done with input buffer. We should zero out the
                    // data if we're in encrypt mode.
                    Arrays.fill(buffer, (byte) 0x00);
                }
            }
            if (inputLen != 0) {
                System.arraycopy(input, inputOffset, finalBuf,
                        buffered, inputLen);
            }
            if (paddingLen != 0) {
                padding.padWithLen(finalBuf, Math.addExact(buffered, inputLen), paddingLen);
            }
            return finalBuf;
        }
        return input;
    }

    private int fillOutputBuffer(byte[] finalBuf, int finalOffset,
        byte[] output, int outOfs, int finalBufLen, byte[] input)
        throws ShortBufferException, BadPaddingException,
        IllegalBlockSizeException {

        int len;
        try {
            len = finalNoPadding(finalBuf, finalOffset, output,
                outOfs, finalBufLen);
            if (decrypting && padding != null) {
                len = unpad(len, outOfs, output);
            }
            return len;
        } finally {
            if (!decrypting && finalBuf != input) {
                // done with internal finalBuf array. Copied to output
                Arrays.fill(finalBuf, (byte) 0x00);
            }
        }
    }


    private int checkOutputCapacity(byte[] output, int outputOffset,
                            int estOutSize) throws ShortBufferException {
        // check output buffer capacity.
        // if we are decrypting with padding applied, we can perform this
        // check only after we have determined how many padding bytes there
        // are.
        int outputCapacity = output.length - outputOffset;
        int minOutSize = decrypting ? (estOutSize - blockSize) : estOutSize;
        if ((output == null) || (outputCapacity < minOutSize)) {
            throw new ShortBufferException("Output buffer must be "
                + "(at least) " + minOutSize + " bytes long");
        }
        return outputCapacity;
    }

    private int finalNoPadding(byte[] in, int inOfs, byte[] out, int outOfs,
                               int len)
        throws IllegalBlockSizeException, ShortBufferException {

        if (in == null || len == 0) {
            return 0;
        }
        if ((cipherMode != CFB_MODE) && (cipherMode != OFB_MODE) &&
            ((len % unitBytes) != 0) && (cipherMode != CTS_MODE)) {
                if (padding != null) {
                    throw new IllegalBlockSizeException
                        ("Input length (with padding) not multiple of " +
                         unitBytes + " bytes");
                } else {
                    throw new IllegalBlockSizeException
                        ("Input length not multiple of " + unitBytes
                         + " bytes");
                }
        }
        int outLen;
        if (decrypting) {
            outLen = cipher.decryptFinal(in, inOfs, len, out, outOfs);
        } else {
            outLen = cipher.encryptFinal(in, inOfs, len, out, outOfs);
        }
        return outLen;
    }

    // Note: Wrap() and Unwrap() are the same in
    // each of SunJCE CipherSpi implementation classes.
    // They are duplicated due to export control requirements:
    // All CipherSpi implementation must be final.
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

        try {
            byte[] encodedKey = key.getEncoded();
            if ((encodedKey == null) || (encodedKey.length == 0)) {
                throw new InvalidKeyException("Cannot get an encoding of " +
                                              "the key to be wrapped");
            }
            try {
                result = doFinal(encodedKey, 0, encodedKey.length);
            } finally {
                Arrays.fill(encodedKey, (byte)0);
            }
        } catch (BadPaddingException e) {
            // Should never happen
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
    Key unwrap(byte[] wrappedKey, String wrappedKeyAlgorithm,
               int wrappedKeyType)
        throws InvalidKeyException, NoSuchAlgorithmException {
        byte[] encodedKey;
        try {
            encodedKey = doFinal(wrappedKey, 0, wrappedKey.length);
        } catch (BadPaddingException ePadding) {
            throw new InvalidKeyException("The wrapped key is not padded " +
                                          "correctly");
        } catch (IllegalBlockSizeException eBlockSize) {
            throw new InvalidKeyException("The wrapped key does not have " +
                                          "the correct length");
        }
        try {
            return ConstructKeys.constructKey(encodedKey, wrappedKeyAlgorithm,
                    wrappedKeyType);
        } finally {
            Arrays.fill(encodedKey, (byte)0);
        }
    }
}
