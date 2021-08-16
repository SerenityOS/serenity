/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.security.interfaces.*;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.MGF1ParameterSpec;

import javax.crypto.*;
import javax.crypto.spec.PSource;
import javax.crypto.spec.OAEPParameterSpec;

import sun.security.rsa.*;
import sun.security.jca.Providers;
import sun.security.internal.spec.TlsRsaPremasterSecretParameterSpec;
import sun.security.util.KeyUtil;

/**
 * RSA cipher implementation. Supports RSA en/decryption and signing/verifying
 * using both PKCS#1 v1.5 and OAEP (v2.2) paddings and without padding (raw RSA).
 * Note that raw RSA is supported mostly for completeness and should only be
 * used in rare cases.
 *
 * Objects should be instantiated by calling Cipher.getInstance() using the
 * following algorithm names:
 *  . "RSA/ECB/PKCS1Padding" (or "RSA") for PKCS#1 v1.5 padding.
 *  . "RSA/ECB/OAEPwith<hash>andMGF1Padding" (or "RSA/ECB/OAEPPadding") for
 *    PKCS#1 v2.2 padding.
 *  . "RSA/ECB/NoPadding" for rsa RSA.
 *
 * We only do one RSA operation per doFinal() call. If the application passes
 * more data via calls to update() or doFinal(), we throw an
 * IllegalBlockSizeException when doFinal() is called (see JCE API spec).
 * Bulk encryption using RSA does not make sense and is not standardized.
 *
 * Note: RSA keys should be at least 512 bits long
 *
 * @since   1.5
 * @author  Andreas Sterbenz
 */
public final class RSACipher extends CipherSpi {

    // constant for an empty byte array
    private static final byte[] B0 = new byte[0];

    // mode constant for public key encryption
    private static final int MODE_ENCRYPT = 1;
    // mode constant for private key decryption
    private static final int MODE_DECRYPT = 2;
    // mode constant for private key encryption (signing)
    private static final int MODE_SIGN    = 3;
    // mode constant for public key decryption (verifying)
    private static final int MODE_VERIFY  = 4;

    // constant for raw RSA
    private static final String PAD_NONE  = "NoPadding";
    // constant for PKCS#1 v1.5 RSA
    private static final String PAD_PKCS1 = "PKCS1Padding";
    // constant for PKCS#2 v2.2 OAEP with MGF1
    private static final String PAD_OAEP_MGF1  = "OAEP";

    // current mode, one of MODE_* above. Set when init() is called
    private int mode;

    // active padding type, one of PAD_* above. Set by setPadding()
    private String paddingType;

    // padding object
    private RSAPadding padding;

    // cipher parameter for OAEP padding and TLS RSA premaster secret
    private AlgorithmParameterSpec spec = null;

    // buffer for the data
    private byte[] buffer;
    // offset into the buffer (number of bytes buffered)
    private int bufOfs;

    // size of the output
    private int outputSize;

    // the public key, if we were initialized using a public key
    private RSAPublicKey publicKey;
    // the private key, if we were initialized using a private key
    private RSAPrivateKey privateKey;

    // hash algorithm for OAEP
    private String oaepHashAlgorithm = "SHA-1";

    // the source of randomness
    private SecureRandom random;

    public RSACipher() {
        paddingType = PAD_PKCS1;
    }

    // modes do not make sense for RSA, but allow ECB
    // see JCE spec
    protected void engineSetMode(String mode) throws NoSuchAlgorithmException {
        if (mode.equalsIgnoreCase("ECB") == false) {
            throw new NoSuchAlgorithmException("Unsupported mode " + mode);
        }
    }

    // set the padding type
    // see JCE spec
    protected void engineSetPadding(String paddingName)
            throws NoSuchPaddingException {
        if (paddingName.equalsIgnoreCase(PAD_NONE)) {
            paddingType = PAD_NONE;
        } else if (paddingName.equalsIgnoreCase(PAD_PKCS1)) {
            paddingType = PAD_PKCS1;
        } else {
            String lowerPadding = paddingName.toLowerCase(Locale.ENGLISH);
            if (lowerPadding.equals("oaeppadding")) {
                paddingType = PAD_OAEP_MGF1;
            } else if (lowerPadding.startsWith("oaepwith") &&
                       lowerPadding.endsWith("andmgf1padding")) {
                paddingType = PAD_OAEP_MGF1;
                // "oaepwith".length() == 8
                // "andmgf1padding".length() == 14
                oaepHashAlgorithm =
                        paddingName.substring(8, paddingName.length() - 14);
                // check if MessageDigest appears to be available
                // avoid getInstance() call here
                if (Providers.getProviderList().getService
                        ("MessageDigest", oaepHashAlgorithm) == null) {
                    throw new NoSuchPaddingException
                        ("MessageDigest not available for " + paddingName);
                }
            } else {
                throw new NoSuchPaddingException
                    ("Padding " + paddingName + " not supported");
            }
        }
    }

    // return 0 as block size, we are not a block cipher
    // see JCE spec
    protected int engineGetBlockSize() {
        return 0;
    }

    // return the output size
    // see JCE spec
    protected int engineGetOutputSize(int inputLen) {
        return outputSize;
    }

    // no iv, return null
    // see JCE spec
    protected byte[] engineGetIV() {
        return null;
    }

    // see JCE spec
    protected AlgorithmParameters engineGetParameters() {
        if (spec != null && spec instanceof OAEPParameterSpec) {
            try {
                AlgorithmParameters params =
                    AlgorithmParameters.getInstance("OAEP",
                        SunJCE.getInstance());
                params.init(spec);
                return params;
            } catch (NoSuchAlgorithmException nsae) {
                // should never happen
                throw new RuntimeException("Cannot find OAEP " +
                    " AlgorithmParameters implementation in SunJCE provider");
            } catch (InvalidParameterSpecException ipse) {
                // should never happen
                throw new RuntimeException("OAEPParameterSpec not supported");
            }
        } else {
            return null;
        }
    }

    // see JCE spec
    protected void engineInit(int opmode, Key key, SecureRandom random)
            throws InvalidKeyException {
        try {
            init(opmode, key, random, null);
        } catch (InvalidAlgorithmParameterException iape) {
            // never thrown when null parameters are used;
            // but re-throw it just in case
            InvalidKeyException ike =
                new InvalidKeyException("Wrong parameters");
            ike.initCause(iape);
            throw ike;
        }
    }

    // see JCE spec
    protected void engineInit(int opmode, Key key,
            AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        init(opmode, key, random, params);
    }

    // see JCE spec
    protected void engineInit(int opmode, Key key,
            AlgorithmParameters params, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (params == null) {
            init(opmode, key, random, null);
        } else {
            try {
                OAEPParameterSpec spec =
                        params.getParameterSpec(OAEPParameterSpec.class);
                init(opmode, key, random, spec);
            } catch (InvalidParameterSpecException ipse) {
                InvalidAlgorithmParameterException iape =
                    new InvalidAlgorithmParameterException("Wrong parameter");
                iape.initCause(ipse);
                throw iape;
            }
        }
    }

    // initialize this cipher
    @SuppressWarnings("deprecation")
    private void init(int opmode, Key key, SecureRandom random,
            AlgorithmParameterSpec params)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        boolean encrypt;
        switch (opmode) {
        case Cipher.ENCRYPT_MODE:
        case Cipher.WRAP_MODE:
            encrypt = true;
            break;
        case Cipher.DECRYPT_MODE:
        case Cipher.UNWRAP_MODE:
            encrypt = false;
            break;
        default:
            // should never happen; checked by Cipher.init()
            throw new AssertionError("Unknown mode: " + opmode);
        }
        RSAKey rsaKey = RSAKeyFactory.toRSAKey(key);
        if (rsaKey instanceof RSAPublicKey) {
            mode = encrypt ? MODE_ENCRYPT : MODE_VERIFY;
            publicKey = (RSAPublicKey)rsaKey;
            privateKey = null;
        } else { // must be RSAPrivateKey per check in toRSAKey
            mode = encrypt ? MODE_SIGN : MODE_DECRYPT;
            privateKey = (RSAPrivateKey)rsaKey;
            publicKey = null;
        }
        int n = RSACore.getByteLength(rsaKey.getModulus());
        outputSize = n;
        bufOfs = 0;
        if (paddingType == PAD_NONE) {
            if (params != null) {
                throw new InvalidAlgorithmParameterException
                ("Parameters not supported");
            }
            padding = RSAPadding.getInstance(RSAPadding.PAD_NONE, n, random);
            buffer = new byte[n];
        } else if (paddingType == PAD_PKCS1) {
            if (params != null) {
                if (!(params instanceof TlsRsaPremasterSecretParameterSpec)) {
                    throw new InvalidAlgorithmParameterException(
                            "Parameters not supported");
                }

                spec = params;
                this.random = random;   // for TLS RSA premaster secret
            }
            int blockType = (mode <= MODE_DECRYPT) ? RSAPadding.PAD_BLOCKTYPE_2
                                                   : RSAPadding.PAD_BLOCKTYPE_1;
            padding = RSAPadding.getInstance(blockType, n, random);
            if (encrypt) {
                int k = padding.getMaxDataSize();
                buffer = new byte[k];
            } else {
                buffer = new byte[n];
            }
        } else { // PAD_OAEP_MGF1
            if ((mode == MODE_SIGN) || (mode == MODE_VERIFY)) {
                throw new InvalidKeyException
                        ("OAEP cannot be used to sign or verify signatures");
            }
            if (params != null) {
                if (!(params instanceof OAEPParameterSpec)) {
                    throw new InvalidAlgorithmParameterException
                        ("Wrong Parameters for OAEP Padding");
                }
                spec = params;
            } else {
                spec = new OAEPParameterSpec(oaepHashAlgorithm, "MGF1",
                    MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
            }
            padding = RSAPadding.getInstance(RSAPadding.PAD_OAEP_MGF1, n,
                random, (OAEPParameterSpec)spec);
            if (encrypt) {
                int k = padding.getMaxDataSize();
                buffer = new byte[k];
            } else {
                buffer = new byte[n];
            }
        }
    }

    // internal update method
    private void update(byte[] in, int inOfs, int inLen) {
        if ((inLen == 0) || (in == null)) {
            return;
        }
        if (inLen > (buffer.length - bufOfs)) {
            bufOfs = buffer.length + 1;
            return;
        }
        System.arraycopy(in, inOfs, buffer, bufOfs, inLen);
        bufOfs += inLen;
    }

    // internal doFinal() method. Here we perform the actual RSA operation
    private byte[] doFinal() throws BadPaddingException,
            IllegalBlockSizeException {
        if (bufOfs > buffer.length) {
            throw new IllegalBlockSizeException("Data must not be longer "
                + "than " + buffer.length + " bytes");
        }
        byte[] paddingCopy = null;
        byte[] result = null;
        try {
            switch (mode) {
            case MODE_SIGN:
                paddingCopy = padding.pad(buffer, 0, bufOfs);
                result = RSACore.rsa(paddingCopy, privateKey, true);
                break;
            case MODE_VERIFY:
                byte[] verifyBuffer = RSACore.convert(buffer, 0, bufOfs);
                paddingCopy = RSACore.rsa(verifyBuffer, publicKey);
                result = padding.unpad(paddingCopy);
                break;
            case MODE_ENCRYPT:
                paddingCopy = padding.pad(buffer, 0, bufOfs);
                result = RSACore.rsa(paddingCopy, publicKey);
                break;
            case MODE_DECRYPT:
                byte[] decryptBuffer = RSACore.convert(buffer, 0, bufOfs);
                paddingCopy = RSACore.rsa(decryptBuffer, privateKey, false);
                result = padding.unpad(paddingCopy);
                break;
            default:
                throw new AssertionError("Internal error");
            }
            return result;
        } finally {
            Arrays.fill(buffer, 0, bufOfs, (byte)0);
            bufOfs = 0;
            if (paddingCopy != null             // will not happen
                    && paddingCopy != buffer    // already cleaned
                    && paddingCopy != result) { // DO NOT CLEAN, THIS IS RESULT!
                Arrays.fill(paddingCopy, (byte)0);
            }
        }
    }

    // see JCE spec
    protected byte[] engineUpdate(byte[] in, int inOfs, int inLen) {
        update(in, inOfs, inLen);
        return B0;
    }

    // see JCE spec
    protected int engineUpdate(byte[] in, int inOfs, int inLen, byte[] out,
            int outOfs) {
        update(in, inOfs, inLen);
        return 0;
    }

    // see JCE spec
    protected byte[] engineDoFinal(byte[] in, int inOfs, int inLen)
            throws BadPaddingException, IllegalBlockSizeException {
        update(in, inOfs, inLen);
        return doFinal();
    }

    // see JCE spec
    protected int engineDoFinal(byte[] in, int inOfs, int inLen, byte[] out,
            int outOfs) throws ShortBufferException, BadPaddingException,
            IllegalBlockSizeException {
        if (outputSize > out.length - outOfs) {
            throw new ShortBufferException
                ("Need " + outputSize + " bytes for output");
        }
        update(in, inOfs, inLen);
        byte[] result = doFinal();
        int n = result.length;
        System.arraycopy(result, 0, out, outOfs, n);
        Arrays.fill(result, (byte)0);
        return n;
    }

    // see JCE spec
    protected byte[] engineWrap(Key key) throws InvalidKeyException,
            IllegalBlockSizeException {
        byte[] encoded = key.getEncoded();
        if ((encoded == null) || (encoded.length == 0)) {
            throw new InvalidKeyException("Could not obtain encoded key");
        }
        try {
            if (encoded.length > buffer.length) {
                throw new InvalidKeyException("Key is too long for wrapping");
            }
            update(encoded, 0, encoded.length);
            try {
                return doFinal();
            } catch (BadPaddingException e) {
                // should not occur
                throw new InvalidKeyException("Wrapping failed", e);
            }
        } finally {
            Arrays.fill(encoded, (byte)0);
        }
    }

    // see JCE spec
    @SuppressWarnings("deprecation")
    protected Key engineUnwrap(byte[] wrappedKey, String algorithm,
            int type) throws InvalidKeyException, NoSuchAlgorithmException {
        if (wrappedKey.length > buffer.length) {
            throw new InvalidKeyException("Key is too long for unwrapping");
        }

        boolean isTlsRsaPremasterSecret =
                algorithm.equals("TlsRsaPremasterSecret");
        Exception failover = null;
        byte[] encoded = null;

        update(wrappedKey, 0, wrappedKey.length);
        try {
            encoded = doFinal();
        } catch (BadPaddingException e) {
            if (isTlsRsaPremasterSecret) {
                failover = e;
            } else {
                throw new InvalidKeyException("Unwrapping failed", e);
            }
        } catch (IllegalBlockSizeException e) {
            // should not occur, handled with length check above
            throw new InvalidKeyException("Unwrapping failed", e);
        }

        try {
            if (isTlsRsaPremasterSecret) {
                if (!(spec instanceof TlsRsaPremasterSecretParameterSpec)) {
                    throw new IllegalStateException(
                            "No TlsRsaPremasterSecretParameterSpec specified");
                }

                // polish the TLS premaster secret
                encoded = KeyUtil.checkTlsPreMasterSecretKey(
                        ((TlsRsaPremasterSecretParameterSpec) spec).getClientVersion(),
                        ((TlsRsaPremasterSecretParameterSpec) spec).getServerVersion(),
                        random, encoded, (failover != null));
            }

            return ConstructKeys.constructKey(encoded, algorithm, type);
        } finally {
            if (encoded != null) {
                Arrays.fill(encoded, (byte) 0);
            }
        }
    }

    // see JCE spec
    protected int engineGetKeySize(Key key) throws InvalidKeyException {
        RSAKey rsaKey = RSAKeyFactory.toRSAKey(key);
        return rsaKey.getModulus().bitLength();
    }
}
