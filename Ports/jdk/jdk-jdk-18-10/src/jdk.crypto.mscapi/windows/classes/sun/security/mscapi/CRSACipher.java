/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.mscapi;

import java.math.BigInteger;
import java.security.*;
import java.security.Key;
import java.security.interfaces.*;
import java.security.spec.*;

import javax.crypto.*;
import javax.crypto.spec.*;

import sun.security.rsa.RSAKeyFactory;
import sun.security.internal.spec.TlsRsaPremasterSecretParameterSpec;
import sun.security.util.KeyUtil;

/**
 * Cipher implementation using the Microsoft Crypto API.
 * Supports RSA en/decryption and signing/verifying using PKCS#1 v1.5 padding.
 *
 * Objects should be instantiated by calling Cipher.getInstance() using the
 * following algorithm name:
 *
 *  . "RSA/ECB/PKCS1Padding" (or "RSA") for PKCS#1 padding. The mode (blocktype)
 *    is selected based on the en/decryption mode and public/private key used.
 *
 * We only do one RSA operation per doFinal() call. If the application passes
 * more data via calls to update() or doFinal(), we throw an
 * IllegalBlockSizeException when doFinal() is called (see JCE API spec).
 * Bulk encryption using RSA does not make sense and is not standardized.
 *
 * Note: RSA keys should be at least 512 bits long
 *
 * @since   1.6
 * @author  Andreas Sterbenz
 * @author  Vincent Ryan
 */
public final class CRSACipher extends CipherSpi {

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

    // constant for PKCS#1 v1.5 RSA
    private static final String PAD_PKCS1 = "PKCS1Padding";
    private static final int PAD_PKCS1_LENGTH = 11;

    // current mode, one of MODE_* above. Set when init() is called
    private int mode;

    // active padding type, one of PAD_* above. Set by setPadding()
    private String paddingType;
    private int paddingLength = 0;

    // buffer for the data
    private byte[] buffer;
    // offset into the buffer (number of bytes buffered)
    private int bufOfs;

    // size of the output (the length of the key).
    private int outputSize;

    // the public key, if we were initialized using a public key
    private CKey publicKey;

    // the private key, if we were initialized using a private key
    private CKey privateKey;

    // cipher parameter for TLS RSA premaster secret
    private AlgorithmParameterSpec spec = null;

    // the source of randomness
    private SecureRandom random;

    public CRSACipher() {
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
        if (paddingName.equalsIgnoreCase(PAD_PKCS1)) {
            paddingType = PAD_PKCS1;
        } else {
            throw new NoSuchPaddingException
                ("Padding " + paddingName + " not supported");
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

    // no parameters, return null
    // see JCE spec
    protected AlgorithmParameters engineGetParameters() {
        return null;
    }

    // see JCE spec
    protected void engineInit(int opmode, Key key, SecureRandom random)
            throws InvalidKeyException {
        init(opmode, key);
    }

    // see JCE spec
    @SuppressWarnings("deprecation")
    protected void engineInit(int opmode, Key key,
            AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException {

        if (params != null) {
            if (!(params instanceof TlsRsaPremasterSecretParameterSpec)) {
                throw new InvalidAlgorithmParameterException(
                        "Parameters not supported");
            }
            spec = params;
            this.random = random;   // for TLS RSA premaster secret
        }
        init(opmode, key);
    }

    // see JCE spec
    protected void engineInit(int opmode, Key key,
            AlgorithmParameters params, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException {

        if (params != null) {
            throw new InvalidAlgorithmParameterException
                ("Parameters not supported");
        }
        init(opmode, key);
    }

    // initialize this cipher
    private void init(int opmode, Key key) throws InvalidKeyException {

        boolean encrypt;

        switch (opmode) {
        case Cipher.ENCRYPT_MODE:
        case Cipher.WRAP_MODE:
            paddingLength = PAD_PKCS1_LENGTH;
            encrypt = true;
            break;
        case Cipher.DECRYPT_MODE:
        case Cipher.UNWRAP_MODE:
            paddingLength = 0; // reset
            encrypt = false;
            break;
        default:
            // should never happen; checked by Cipher.init()
            throw new AssertionError("Unknown mode: " + opmode);
        }

        if (!(key instanceof CKey)) {
            if (key instanceof java.security.interfaces.RSAPublicKey) {
                java.security.interfaces.RSAPublicKey rsaKey =
                    (java.security.interfaces.RSAPublicKey) key;

                // Convert key to MSCAPI format

                BigInteger modulus = rsaKey.getModulus();
                BigInteger exponent =  rsaKey.getPublicExponent();

                // Check against the local and global values to make sure
                // the sizes are ok.  Round up to the nearest byte.
                RSAKeyFactory.checkKeyLengths(((modulus.bitLength() + 7) & ~7),
                    exponent, -1, CKeyPairGenerator.RSA.KEY_SIZE_MAX);

                byte[] modulusBytes = modulus.toByteArray();
                byte[] exponentBytes = exponent.toByteArray();

                // Adjust key length due to sign bit
                int keyBitLength = (modulusBytes[0] == 0)
                    ? (modulusBytes.length - 1) * 8
                    : modulusBytes.length * 8;

                byte[] keyBlob = CSignature.RSA.generatePublicKeyBlob(
                    keyBitLength, modulusBytes, exponentBytes);

                try {
                    key = CSignature.importPublicKey("RSA", keyBlob, keyBitLength);

                } catch (KeyStoreException e) {
                    throw new InvalidKeyException(e);
                }

            } else {
                throw new InvalidKeyException("Unsupported key type: " + key);
            }
        }

        if (key instanceof PublicKey) {
            mode = encrypt ? MODE_ENCRYPT : MODE_VERIFY;
            publicKey = (CKey)key;
            privateKey = null;
            outputSize = publicKey.length() / 8;
        } else if (key instanceof PrivateKey) {
            mode = encrypt ? MODE_SIGN : MODE_DECRYPT;
            privateKey = (CKey)key;
            publicKey = null;
            outputSize = privateKey.length() / 8;
        } else {
            throw new InvalidKeyException("Unknown key type: " + key);
        }

        bufOfs = 0;
        buffer = new byte[outputSize];
    }

    // internal update method
    private void update(byte[] in, int inOfs, int inLen) {
        if ((inLen == 0) || (in == null)) {
            return;
        }
        if (bufOfs + inLen > (buffer.length - paddingLength)) {
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
                + "than " + (buffer.length - paddingLength)  + " bytes");
        }

        try {
            byte[] data = buffer;
            switch (mode) {
            case MODE_SIGN:
                return encryptDecrypt(data, bufOfs,
                    privateKey.getHCryptKey(), true);

            case MODE_VERIFY:
                return encryptDecrypt(data, bufOfs,
                    publicKey.getHCryptKey(), false);

            case MODE_ENCRYPT:
                return encryptDecrypt(data, bufOfs,
                    publicKey.getHCryptKey(), true);

            case MODE_DECRYPT:
                return encryptDecrypt(data, bufOfs,
                    privateKey.getHCryptKey(), false);

            default:
                throw new AssertionError("Internal error");
            }

        } catch (KeyException e) {
            throw new ProviderException(e);

        } finally {
            bufOfs = 0;
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
        return n;
    }

    // see JCE spec
    protected byte[] engineWrap(Key key) throws InvalidKeyException,
            IllegalBlockSizeException {
        byte[] encoded = key.getEncoded(); // TODO - unextractable key
        if ((encoded == null) || (encoded.length == 0)) {
            throw new InvalidKeyException("Could not obtain encoded key");
        }
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
    }

    // see JCE spec
    @SuppressWarnings("deprecation")
    protected java.security.Key engineUnwrap(byte[] wrappedKey,
            String algorithm,
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

        if (isTlsRsaPremasterSecret) {
            if (!(spec instanceof TlsRsaPremasterSecretParameterSpec)) {
                throw new IllegalStateException(
                        "No TlsRsaPremasterSecretParameterSpec specified");
            }

            // polish the TLS premaster secret
            encoded = KeyUtil.checkTlsPreMasterSecretKey(
                ((TlsRsaPremasterSecretParameterSpec)spec).getClientVersion(),
                ((TlsRsaPremasterSecretParameterSpec)spec).getServerVersion(),
                random, encoded, (failover != null));
        }

        return constructKey(encoded, algorithm, type);
    }

    // see JCE spec
    protected int engineGetKeySize(Key key) throws InvalidKeyException {

        if (key instanceof CKey) {
            return ((CKey) key).length();

        } else if (key instanceof RSAKey) {
            return ((RSAKey) key).getModulus().bitLength();

        } else {
            throw new InvalidKeyException("Unsupported key type: " + key);
        }
    }

    // Construct an X.509 encoded public key.
    private static PublicKey constructPublicKey(byte[] encodedKey,
        String encodedKeyAlgorithm)
            throws InvalidKeyException, NoSuchAlgorithmException {

        try {
            KeyFactory keyFactory = KeyFactory.getInstance(encodedKeyAlgorithm);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);

            return keyFactory.generatePublic(keySpec);

        } catch (NoSuchAlgorithmException nsae) {
            throw new NoSuchAlgorithmException("No installed provider " +
                "supports the " + encodedKeyAlgorithm + " algorithm", nsae);

        } catch (InvalidKeySpecException ike) {
            throw new InvalidKeyException("Cannot construct public key", ike);
        }
    }

    // Construct a PKCS #8 encoded private key.
    private static PrivateKey constructPrivateKey(byte[] encodedKey,
        String encodedKeyAlgorithm)
            throws InvalidKeyException, NoSuchAlgorithmException {

        try {
            KeyFactory keyFactory = KeyFactory.getInstance(encodedKeyAlgorithm);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);

            return keyFactory.generatePrivate(keySpec);

        } catch (NoSuchAlgorithmException nsae) {
            throw new NoSuchAlgorithmException("No installed provider " +
                "supports the " + encodedKeyAlgorithm + " algorithm", nsae);

        } catch (InvalidKeySpecException ike) {
            throw new InvalidKeyException("Cannot construct private key", ike);
        }
    }

    // Construct an encoded secret key.
    private static SecretKey constructSecretKey(byte[] encodedKey,
        String encodedKeyAlgorithm) {

        return new SecretKeySpec(encodedKey, encodedKeyAlgorithm);
    }

    private static Key constructKey(byte[] encodedKey,
            String encodedKeyAlgorithm,
            int keyType) throws InvalidKeyException, NoSuchAlgorithmException {

        switch (keyType) {
            case Cipher.PUBLIC_KEY:
                return constructPublicKey(encodedKey, encodedKeyAlgorithm);
            case Cipher.PRIVATE_KEY:
                return constructPrivateKey(encodedKey, encodedKeyAlgorithm);
            case Cipher.SECRET_KEY:
                return constructSecretKey(encodedKey, encodedKeyAlgorithm);
            default:
                throw new InvalidKeyException("Unknown key type " + keyType);
        }
    }

    /*
     * Encrypt/decrypt a data buffer using Microsoft Crypto API with HCRYPTKEY.
     * It expects and returns ciphertext data in big-endian form.
     */
    private native static byte[] encryptDecrypt(byte[] data, int dataSize,
        long hCryptKey, boolean doEncrypt) throws KeyException;

}
