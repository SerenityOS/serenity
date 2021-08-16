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

import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import javax.crypto.*;

/**
 * Implementation of the ARCFOUR cipher, an algorithm apparently compatible
 * with RSA Security's RC4(tm) cipher. The description of this algorithm was
 * taken from Bruce Schneier's book Applied Cryptography, 2nd ed.,
 * section 17.1.
 *
 * We support keys from 40 to 1024 bits. ARCFOUR would allow for keys shorter
 * than 40 bits, but that is too insecure for us to permit.
 *
 * Note that we subclass CipherSpi directly and do not use the CipherCore
 * framework. That was designed to simplify implementation of block ciphers
 * and does not offer any advantages for stream ciphers such as ARCFOUR.
 *
 * @since   1.5
 * @author  Andreas Sterbenz
 */
public final class ARCFOURCipher extends CipherSpi {

    // state array S, 256 entries. The entries are 8-bit, but we use an int[]
    // because int arithmetic is much faster than in Java than bytes.
    private final int[] S;

    // state indices i and j. Called is and js to avoid collision with
    // local variables. 'is' is set to -1 after a call to doFinal()
    private int is, js;

    // the bytes of the last key used (if any)
    // we need this to re-initialize after a call to doFinal()
    private byte[] lastKey;

    // called by the JCE framework
    public ARCFOURCipher() {
        S = new int[256];
    }

    // core key setup code. initializes S, is, and js
    // assumes key is non-null and between 40 and 1024 bit
    private void init(byte[] key) {
        // initialize S[i] to i
        for (int i = 0; i < 256; i++) {
            S[i] = i;
        }

        // we avoid expanding key to 256 bytes and instead keep a separate
        // counter ki = i mod key.length.
        for (int i = 0, j = 0, ki = 0; i < 256; i++) {
            int Si = S[i];
            j = (j + Si + key[ki]) & 0xff;
            S[i] = S[j];
            S[j] = Si;
            ki++;
            if (ki == key.length) {
                ki = 0;
            }
        }

        // set indices to 0
        is = 0;
        js = 0;
    }

    // core crypt code. OFB style, so works for both encryption and decryption
    private void crypt(byte[] in, int inOfs, int inLen, byte[] out,
            int outOfs) {
        if (is < 0) {
            // doFinal() was called, need to reset the cipher to initial state
            init(lastKey);
        }
        while (inLen-- > 0) {
            is = (is + 1) & 0xff;
            int Si = S[is];
            js = (js + Si) & 0xff;
            int Sj = S[js];
            S[is] = Sj;
            S[js] = Si;
            out[outOfs++] = (byte)(in[inOfs++] ^ S[(Si + Sj) & 0xff]);
        }
    }

    // Modes do not make sense with stream ciphers, but allow ECB
    // see JCE spec.
    protected void engineSetMode(String mode) throws NoSuchAlgorithmException {
        if (mode.equalsIgnoreCase("ECB") == false) {
            throw new NoSuchAlgorithmException("Unsupported mode " + mode);
        }
    }

    // Padding does not make sense with stream ciphers, but allow NoPadding
    // see JCE spec.
    protected void engineSetPadding(String padding)
            throws NoSuchPaddingException {
        if (padding.equalsIgnoreCase("NoPadding") == false) {
            throw new NoSuchPaddingException("Padding must be NoPadding");
        }
    }

    // Return 0 to indicate stream cipher
    // see JCE spec.
    protected int engineGetBlockSize() {
        return 0;
    }

    // output length is always the same as input length
    // see JCE spec
    protected int engineGetOutputSize(int inputLen) {
        return inputLen;
    }

    // no IV, return null
    // see JCE spec
    protected byte[] engineGetIV() {
        return null;
    }

    // no parameters
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
    protected void engineInit(int opmode, Key key,
            AlgorithmParameterSpec params, SecureRandom random)
            throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (params != null) {
            throw new InvalidAlgorithmParameterException
                ("Parameters not supported");
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

    // init method. Check key, then call init(byte[]).
    private void init(int opmode, Key key) throws InvalidKeyException {

        // Cipher.init() already checks opmode to be:
        // ENCRYPT_MODE/DECRYPT_MODE/WRAP_MODE/UNWRAP_MODE

        if (lastKey != null) {
            Arrays.fill(lastKey, (byte)0);
        }

        lastKey = getEncodedKey(key);
        init(lastKey);
    }

    // return the encoding of key if key is a valid ARCFOUR key.
    // otherwise, throw an InvalidKeyException
    private static byte[] getEncodedKey(Key key) throws InvalidKeyException {
        String keyAlg = key.getAlgorithm();
        if (!keyAlg.equals("RC4") && !keyAlg.equals("ARCFOUR")) {
            throw new InvalidKeyException("Not an ARCFOUR key: " + keyAlg);
        }
        if ("RAW".equals(key.getFormat()) == false) {
            throw new InvalidKeyException("Key encoding format must be RAW");
        }
        byte[] encodedKey = key.getEncoded();
        if ((encodedKey.length < 5) || (encodedKey.length > 128)) {
            Arrays.fill(encodedKey, (byte)0);
            throw new InvalidKeyException
                ("Key length must be between 40 and 1024 bit");
        }
        return encodedKey;
    }

    // see JCE spec
    protected byte[] engineUpdate(byte[] in, int inOfs, int inLen) {
        byte[] out = new byte[inLen];
        crypt(in, inOfs, inLen, out, 0);
        return out;
    }

    // see JCE spec
    protected int engineUpdate(byte[] in, int inOfs, int inLen,
            byte[] out, int outOfs) throws ShortBufferException {
        if (out.length - outOfs < inLen) {
            throw new ShortBufferException("Output buffer too small");
        }
        crypt(in, inOfs, inLen, out, outOfs);
        return inLen;
    }

    // see JCE spec
    protected byte[] engineDoFinal(byte[] in, int inOfs, int inLen) {
        byte[] out = engineUpdate(in, inOfs, inLen);
        is = -1;
        return out;
    }

    // see JCE spec
    protected int engineDoFinal(byte[] in, int inOfs, int inLen,
            byte[] out, int outOfs) throws ShortBufferException {
        int outLen = engineUpdate(in, inOfs, inLen, out, outOfs);
        is = -1;
        return outLen;
    }

    // see JCE spec
    protected byte[] engineWrap(Key key) throws IllegalBlockSizeException,
            InvalidKeyException {
        byte[] encoded = key.getEncoded();
        if ((encoded == null) || (encoded.length == 0)) {
            throw new InvalidKeyException("Could not obtain encoded key");
        }
        try {
            return engineDoFinal(encoded, 0, encoded.length);
        } finally {
            Arrays.fill(encoded, (byte)0);
        }
    }

    // see JCE spec
    protected Key engineUnwrap(byte[] wrappedKey, String algorithm,
            int type) throws InvalidKeyException, NoSuchAlgorithmException {
        byte[] encoded = null;
        try {
            encoded = engineDoFinal(wrappedKey, 0, wrappedKey.length);
            return ConstructKeys.constructKey(encoded, algorithm, type);
        } finally {
            if (encoded != null) {
                Arrays.fill(encoded, (byte) 0);
            }
        }
    }

    // see JCE spec
    protected int engineGetKeySize(Key key) throws InvalidKeyException {
        byte[] encodedKey = getEncodedKey(key);
        Arrays.fill(encodedKey, (byte)0);
        return Math.multiplyExact(encodedKey.length, 8);
    }

}
