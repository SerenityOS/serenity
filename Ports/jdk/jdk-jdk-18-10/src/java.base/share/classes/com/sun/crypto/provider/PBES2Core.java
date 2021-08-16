/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * These algorithms implement PBE with HmacSHA1/HmacSHA2-family and AES-CBC.
 * Padding is done as described in PKCS #5.
 *
 * @author Jan Luehe
 *
 *
 * @see javax.crypto.Cipher
 */
abstract class PBES2Core extends CipherSpi {

    private static final int DEFAULT_SALT_LENGTH = 20;
    private static final int DEFAULT_COUNT = 4096;

    // the encapsulated cipher
    private final CipherCore cipher;
    private final int keyLength; // in bits
    private final int blkSize; // in bits
    private final PBKDF2Core kdf;
    private final String pbeAlgo;
    private final String cipherAlgo;
    private int iCount = DEFAULT_COUNT;
    private byte[] salt = null;
    private IvParameterSpec ivSpec = null;

    /**
     * Creates an instance of PBE Scheme 2 according to the selected
     * password-based key derivation function and encryption scheme.
     */
    PBES2Core(String kdfAlgo, String cipherAlgo, int keySize)
        throws NoSuchAlgorithmException, NoSuchPaddingException {

        this.cipherAlgo = cipherAlgo;
        keyLength = keySize * 8;
        pbeAlgo = "PBEWith" + kdfAlgo + "And" + cipherAlgo + "_" + keyLength;

        if (cipherAlgo.equals("AES")) {
            blkSize = AESConstants.AES_BLOCK_SIZE;
            cipher = new CipherCore(new AESCrypt(), blkSize);

            switch(kdfAlgo) {
            case "HmacSHA1":
                kdf = new PBKDF2Core.HmacSHA1();
                break;
            case "HmacSHA224":
                kdf = new PBKDF2Core.HmacSHA224();
                break;
            case "HmacSHA256":
                kdf = new PBKDF2Core.HmacSHA256();
                break;
            case "HmacSHA384":
                kdf = new PBKDF2Core.HmacSHA384();
                break;
            case "HmacSHA512":
                kdf = new PBKDF2Core.HmacSHA512();
                break;
            default:
                throw new NoSuchAlgorithmException(
                    "No Cipher implementation for " + kdfAlgo);
            }
        } else {
            throw new NoSuchAlgorithmException("No Cipher implementation for " +
                                               pbeAlgo);
        }
        cipher.setMode("CBC");
        cipher.setPadding("PKCS5Padding");
    }

    protected void engineSetMode(String mode) throws NoSuchAlgorithmException {
        if ((mode != null) && (!mode.equalsIgnoreCase("CBC"))) {
            throw new NoSuchAlgorithmException("Invalid cipher mode: " + mode);
        }
    }

    protected void engineSetPadding(String paddingScheme)
        throws NoSuchPaddingException {
        if ((paddingScheme != null) &&
            (!paddingScheme.equalsIgnoreCase("PKCS5Padding"))) {
            throw new NoSuchPaddingException("Invalid padding scheme: " +
                                             paddingScheme);
        }
    }

    protected int engineGetBlockSize() {
        return blkSize;
    }

    protected int engineGetOutputSize(int inputLen) {
        return cipher.getOutputSize(inputLen);
    }

    protected byte[] engineGetIV() {
        return cipher.getIV();
    }

    protected AlgorithmParameters engineGetParameters() {
        AlgorithmParameters params = null;
        if (salt == null) {
            // generate random salt and use default iteration count
            salt = new byte[DEFAULT_SALT_LENGTH];
            SunJCE.getRandom().nextBytes(salt);
            iCount = DEFAULT_COUNT;
        }
        if (ivSpec == null) {
            // generate random IV
            byte[] ivBytes = new byte[blkSize];
            SunJCE.getRandom().nextBytes(ivBytes);
            ivSpec = new IvParameterSpec(ivBytes);
        }
        PBEParameterSpec pbeSpec = new PBEParameterSpec(salt, iCount, ivSpec);
        try {
            params = AlgorithmParameters.getInstance(pbeAlgo,
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

    protected void engineInit(int opmode, Key key, SecureRandom random)
        throws InvalidKeyException {
        try {
            engineInit(opmode, key, (AlgorithmParameterSpec) null, random);
        } catch (InvalidAlgorithmParameterException ie) {
            InvalidKeyException ike =
                new InvalidKeyException("requires PBE parameters");
            ike.initCause(ie);
            throw ike;
        }
    }

    protected void engineInit(int opmode, Key key,
                              AlgorithmParameterSpec params,
                              SecureRandom random)
        throws InvalidKeyException, InvalidAlgorithmParameterException {

        if (key == null) {
            throw new InvalidKeyException("Null key");
        }

        byte[] passwdBytes = key.getEncoded();
        char[] passwdChars = null;
        PBEKeySpec pbeSpec;
        try {
            if ((passwdBytes == null) ||
                    !(key.getAlgorithm().regionMatches(true, 0, "PBE", 0, 3))) {
                throw new InvalidKeyException("Missing password");
            }

            // TBD: consolidate the salt, ic and IV parameter checks below

            // Extract salt and iteration count from the key, if present
            if (key instanceof javax.crypto.interfaces.PBEKey) {
                salt = ((javax.crypto.interfaces.PBEKey)key).getSalt();
                if (salt != null && salt.length < 8) {
                    throw new InvalidAlgorithmParameterException(
                            "Salt must be at least 8 bytes long");
                }
                iCount = ((javax.crypto.interfaces.PBEKey)key).getIterationCount();
                if (iCount == 0) {
                    iCount = DEFAULT_COUNT;
                } else if (iCount < 0) {
                    throw new InvalidAlgorithmParameterException(
                            "Iteration count must be a positive number");
                }
            }

            // Extract salt, iteration count and IV from the params, if present
            if (params == null) {
                if (salt == null) {
                    // generate random salt and use default iteration count
                    salt = new byte[DEFAULT_SALT_LENGTH];
                    random.nextBytes(salt);
                    iCount = DEFAULT_COUNT;
                }
                if ((opmode == Cipher.ENCRYPT_MODE) ||
                        (opmode == Cipher.WRAP_MODE)) {
                    // generate random IV
                    byte[] ivBytes = new byte[blkSize];
                    random.nextBytes(ivBytes);
                    ivSpec = new IvParameterSpec(ivBytes);
                }
            } else {
                if (!(params instanceof PBEParameterSpec)) {
                    throw new InvalidAlgorithmParameterException
                            ("Wrong parameter type: PBE expected");
                }
                // salt and iteration count from the params take precedence
                byte[] specSalt = ((PBEParameterSpec) params).getSalt();
                if (specSalt != null && specSalt.length < 8) {
                    throw new InvalidAlgorithmParameterException(
                            "Salt must be at least 8 bytes long");
                }
                salt = specSalt;
                int specICount = ((PBEParameterSpec) params).getIterationCount();
                if (specICount == 0) {
                    specICount = DEFAULT_COUNT;
                } else if (specICount < 0) {
                    throw new InvalidAlgorithmParameterException(
                            "Iteration count must be a positive number");
                }
                iCount = specICount;

                AlgorithmParameterSpec specParams =
                        ((PBEParameterSpec) params).getParameterSpec();
                if (specParams != null) {
                    if (specParams instanceof IvParameterSpec) {
                        ivSpec = (IvParameterSpec)specParams;
                    } else {
                        throw new InvalidAlgorithmParameterException(
                                "Wrong parameter type: IV expected");
                    }
                } else if ((opmode == Cipher.ENCRYPT_MODE) ||
                        (opmode == Cipher.WRAP_MODE)) {
                    // generate random IV
                    byte[] ivBytes = new byte[blkSize];
                    random.nextBytes(ivBytes);
                    ivSpec = new IvParameterSpec(ivBytes);
                } else {
                    throw new InvalidAlgorithmParameterException(
                            "Missing parameter type: IV expected");
                }
            }

            passwdChars = new char[passwdBytes.length];
            for (int i = 0; i < passwdChars.length; i++)
                passwdChars[i] = (char) (passwdBytes[i] & 0x7f);

            pbeSpec = new PBEKeySpec(passwdChars, salt, iCount, keyLength);
            // password char[] was cloned in PBEKeySpec constructor,
            // so we can zero it out here
        } finally {
            if (passwdChars != null) Arrays.fill(passwdChars, '\0');
            if (passwdBytes != null) Arrays.fill(passwdBytes, (byte)0x00);
        }

        PBKDF2KeyImpl s;

        try {
            s = (PBKDF2KeyImpl)kdf.engineGenerateSecret(pbeSpec);
        } catch (InvalidKeySpecException ikse) {
            InvalidKeyException ike =
                new InvalidKeyException("Cannot construct PBE key");
            ike.initCause(ikse);
            throw ike;
        } finally {
            pbeSpec.clearPassword();
        }
        byte[] derivedKey = s.getEncoded();
        s.clearPassword();
        SecretKeySpec cipherKey = new SecretKeySpec(derivedKey, cipherAlgo);

        // initialize the underlying cipher
        cipher.init(opmode, cipherKey, ivSpec, random);
    }

    protected void engineInit(int opmode, Key key, AlgorithmParameters params,
                              SecureRandom random)
        throws InvalidKeyException, InvalidAlgorithmParameterException {
        AlgorithmParameterSpec pbeSpec = null;
        if (params != null) {
            try {
                pbeSpec = params.getParameterSpec(PBEParameterSpec.class);
            } catch (InvalidParameterSpecException ipse) {
                throw new InvalidAlgorithmParameterException(
                    "Wrong parameter type: PBE expected");
            }
        }
        engineInit(opmode, key, pbeSpec, random);
    }

    protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
        return cipher.update(input, inputOffset, inputLen);
    }

    protected int engineUpdate(byte[] input, int inputOffset, int inputLen,
                               byte[] output, int outputOffset)
        throws ShortBufferException {
        return cipher.update(input, inputOffset, inputLen,
                             output, outputOffset);
    }

    protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen)
        throws IllegalBlockSizeException, BadPaddingException {
        return cipher.doFinal(input, inputOffset, inputLen);
    }

    protected int engineDoFinal(byte[] input, int inputOffset, int inputLen,
                                byte[] output, int outputOffset)
        throws ShortBufferException, IllegalBlockSizeException,
               BadPaddingException {
        return cipher.doFinal(input, inputOffset, inputLen,
                              output, outputOffset);
    }

    protected int engineGetKeySize(Key key) throws InvalidKeyException {
        return keyLength;
    }

    protected byte[] engineWrap(Key key)
        throws IllegalBlockSizeException, InvalidKeyException {
        return cipher.wrap(key);
    }

    protected Key engineUnwrap(byte[] wrappedKey, String wrappedKeyAlgorithm,
                               int wrappedKeyType)
        throws InvalidKeyException, NoSuchAlgorithmException {
        byte[] encodedKey;
        return cipher.unwrap(wrappedKey, wrappedKeyAlgorithm,
                             wrappedKeyType);
    }

    public static final class HmacSHA1AndAES_128 extends PBES2Core {
        public HmacSHA1AndAES_128()
            throws NoSuchAlgorithmException, NoSuchPaddingException {
            super("HmacSHA1", "AES", 16);
        }
    }

    public static final class HmacSHA224AndAES_128 extends PBES2Core {
        public HmacSHA224AndAES_128()
            throws NoSuchAlgorithmException, NoSuchPaddingException {
            super("HmacSHA224", "AES", 16);
        }
    }

    public static final class HmacSHA256AndAES_128 extends PBES2Core {
        public HmacSHA256AndAES_128()
            throws NoSuchAlgorithmException, NoSuchPaddingException {
            super("HmacSHA256", "AES", 16);
        }
    }

    public static final class HmacSHA384AndAES_128 extends PBES2Core {
        public HmacSHA384AndAES_128()
            throws NoSuchAlgorithmException, NoSuchPaddingException {
            super("HmacSHA384", "AES", 16);
        }
    }

    public static final class HmacSHA512AndAES_128 extends PBES2Core {
        public HmacSHA512AndAES_128()
            throws NoSuchAlgorithmException, NoSuchPaddingException {
            super("HmacSHA512", "AES", 16);
        }
    }

    public static final class HmacSHA1AndAES_256 extends PBES2Core {
        public HmacSHA1AndAES_256()
            throws NoSuchAlgorithmException, NoSuchPaddingException {
            super("HmacSHA1", "AES", 32);
        }
    }

    public static final class HmacSHA224AndAES_256 extends PBES2Core {
        public HmacSHA224AndAES_256()
            throws NoSuchAlgorithmException, NoSuchPaddingException {
            super("HmacSHA224", "AES", 32);
        }
    }

    public static final class HmacSHA256AndAES_256 extends PBES2Core {
        public HmacSHA256AndAES_256()
            throws NoSuchAlgorithmException, NoSuchPaddingException {
            super("HmacSHA256", "AES", 32);
        }
    }

    public static final class HmacSHA384AndAES_256 extends PBES2Core {
        public HmacSHA384AndAES_256()
            throws NoSuchAlgorithmException, NoSuchPaddingException {
            super("HmacSHA384", "AES", 32);
        }
    }

    public static final class HmacSHA512AndAES_256 extends PBES2Core {
        public HmacSHA512AndAES_256()
            throws NoSuchAlgorithmException, NoSuchPaddingException {
            super("HmacSHA512", "AES", 32);
        }
    }
}
