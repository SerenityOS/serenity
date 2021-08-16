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

package com.sun.crypto.provider;

import java.util.Arrays;

import java.security.*;
import java.security.spec.AlgorithmParameterSpec;

import static java.nio.charset.StandardCharsets.UTF_8;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

import sun.security.internal.spec.TlsPrfParameterSpec;

/**
 * KeyGenerator implementation for the TLS PRF function.
 * <p>
 * This class duplicates the HMAC functionality (RFC 2104) with
 * performance optimizations (e.g. XOR'ing keys with padding doesn't
 * need to be redone for each HMAC operation).
 *
 * @author  Andreas Sterbenz
 * @since   1.6
 */
abstract class TlsPrfGenerator extends KeyGeneratorSpi {

    // magic constants and utility functions, also used by other files
    // in this package

    private static final byte[] B0 = new byte[0];

    static final byte[] LABEL_MASTER_SECRET = // "master secret"
        { 109, 97, 115, 116, 101, 114, 32, 115, 101, 99, 114, 101, 116 };

    static final byte[] LABEL_EXTENDED_MASTER_SECRET =
                                            // "extended master secret"
        { 101, 120, 116, 101, 110, 100, 101, 100, 32, 109, 97, 115, 116,
          101, 114, 32, 115, 101, 99, 114, 101, 116 };

    static final byte[] LABEL_KEY_EXPANSION = // "key expansion"
        { 107, 101, 121, 32, 101, 120, 112, 97, 110, 115, 105, 111, 110 };

    static final byte[] LABEL_CLIENT_WRITE_KEY = // "client write key"
        { 99, 108, 105, 101, 110, 116, 32, 119, 114, 105, 116, 101, 32,
          107, 101, 121 };

    static final byte[] LABEL_SERVER_WRITE_KEY = // "server write key"
        { 115, 101, 114, 118, 101, 114, 32, 119, 114, 105, 116, 101, 32,
          107, 101, 121 };

    static final byte[] LABEL_IV_BLOCK = // "IV block"
        { 73, 86, 32, 98, 108, 111, 99, 107 };

    /*
     * TLS HMAC "inner" and "outer" padding.  This isn't a function
     * of the digest algorithm.
     */
    private static final byte[] HMAC_ipad64  = genPad((byte)0x36, 64);
    private static final byte[] HMAC_ipad128 = genPad((byte)0x36, 128);
    private static final byte[] HMAC_opad64  = genPad((byte)0x5c, 64);
    private static final byte[] HMAC_opad128 = genPad((byte)0x5c, 128);

    // SSL3 magic mix constants ("A", "BB", "CCC", ...)
    static final byte[][] SSL3_CONST = genConst();

    static byte[] genPad(byte b, int count) {
        byte[] padding = new byte[count];
        Arrays.fill(padding, b);
        return padding;
    }

    static byte[] concat(byte[] b1, byte[] b2) {
        int n1 = b1.length;
        int n2 = b2.length;
        byte[] b = new byte[n1 + n2];
        System.arraycopy(b1, 0, b, 0, n1);
        System.arraycopy(b2, 0, b, n1, n2);
        return b;
    }

    private static byte[][] genConst() {
        int n = 10;
        byte[][] arr = new byte[n][];
        for (int i = 0; i < n; i++) {
            byte[] b = new byte[i + 1];
            Arrays.fill(b, (byte)('A' + i));
            arr[i] = b;
        }
        return arr;
    }

    // PRF implementation

    private static final String MSG = "TlsPrfGenerator must be "
        + "initialized using a TlsPrfParameterSpec";

    @SuppressWarnings("deprecation")
    private TlsPrfParameterSpec spec;

    public TlsPrfGenerator() {
    }

    protected void engineInit(SecureRandom random) {
        throw new InvalidParameterException(MSG);
    }

    @SuppressWarnings("deprecation")
    protected void engineInit(AlgorithmParameterSpec params,
            SecureRandom random) throws InvalidAlgorithmParameterException {
        if (params instanceof TlsPrfParameterSpec == false) {
            throw new InvalidAlgorithmParameterException(MSG);
        }
        this.spec = (TlsPrfParameterSpec)params;
        SecretKey key = spec.getSecret();
        if ((key != null) && ("RAW".equals(key.getFormat()) == false)) {
            throw new InvalidAlgorithmParameterException(
                "Key encoding format must be RAW");
        }
    }

    protected void engineInit(int keysize, SecureRandom random) {
        throw new InvalidParameterException(MSG);
    }

    SecretKey engineGenerateKey0(boolean tls12) {
        if (spec == null) {
            throw new IllegalStateException(
                "TlsPrfGenerator must be initialized");
        }
        SecretKey key = spec.getSecret();
        byte[] secret = (key == null) ? null : key.getEncoded();
        try {
            byte[] labelBytes = spec.getLabel().getBytes(UTF_8);
            int n = spec.getOutputLength();
            byte[] prfBytes = (tls12 ?
                doTLS12PRF(secret, labelBytes, spec.getSeed(), n,
                    spec.getPRFHashAlg(), spec.getPRFHashLength(),
                    spec.getPRFBlockSize()) :
                doTLS10PRF(secret, labelBytes, spec.getSeed(), n));
            try {
                return new SecretKeySpec(prfBytes, "TlsPrf");
            } finally {
                Arrays.fill(prfBytes, (byte)0);
            }
        } catch (GeneralSecurityException e) {
            throw new ProviderException("Could not generate PRF", e);
        } finally {
            if (secret != null) {
                Arrays.fill(secret, (byte) 0);
            }
        }
    }

    static byte[] doTLS12PRF(byte[] secret, byte[] labelBytes,
            byte[] seed, int outputLength,
            String prfHash, int prfHashLength, int prfBlockSize)
            throws NoSuchAlgorithmException, DigestException {
        if (prfHash == null) {
            throw new NoSuchAlgorithmException("Unspecified PRF algorithm");
        }
        MessageDigest prfMD = MessageDigest.getInstance(prfHash);
        return doTLS12PRF(secret, labelBytes, seed, outputLength,
            prfMD, prfHashLength, prfBlockSize);
    }

    static byte[] doTLS12PRF(byte[] secret, byte[] labelBytes,
            byte[] seed, int outputLength,
            MessageDigest mdPRF, int mdPRFLen, int mdPRFBlockSize)
            throws DigestException {

        if (secret == null) {
            secret = B0;
        }

        // If we have a long secret, digest it first.
        if (secret.length > mdPRFBlockSize) {
            secret = mdPRF.digest(secret);
        }

        byte[] output = new byte[outputLength];
        byte [] ipad;
        byte [] opad;

        switch (mdPRFBlockSize) {
        case 64:
            ipad = HMAC_ipad64.clone();
            opad = HMAC_opad64.clone();
            break;
        case 128:
            ipad = HMAC_ipad128.clone();
            opad = HMAC_opad128.clone();
            break;
        default:
            throw new DigestException("Unexpected block size.");
        }

        // P_HASH(Secret, label + seed)
        expand(mdPRF, mdPRFLen, secret, 0, secret.length, labelBytes,
            seed, output, ipad, opad);

        return output;
    }

    static byte[] doTLS10PRF(byte[] secret, byte[] labelBytes,
            byte[] seed, int outputLength) throws NoSuchAlgorithmException,
            DigestException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        MessageDigest sha = MessageDigest.getInstance("SHA1");
        return doTLS10PRF(secret, labelBytes, seed, outputLength, md5, sha);
    }

    static byte[] doTLS10PRF(byte[] secret, byte[] labelBytes,
            byte[] seed, int outputLength, MessageDigest md5,
            MessageDigest sha) throws DigestException {
        /*
         * Split the secret into two halves S1 and S2 of same length.
         * S1 is taken from the first half of the secret, S2 from the
         * second half.
         * Their length is created by rounding up the length of the
         * overall secret divided by two; thus, if the original secret
         * is an odd number of bytes long, the last byte of S1 will be
         * the same as the first byte of S2.
         *
         * Note: Instead of creating S1 and S2, we determine the offset into
         * the overall secret where S2 starts.
         */

        if (secret == null) {
            secret = B0;
        }
        int off = secret.length >> 1;
        int seclen = off + (secret.length & 1);

        byte[] secKey = secret;
        int keyLen = seclen;
        byte[] output = new byte[outputLength];

        // P_MD5(S1, label + seed)
        // If we have a long secret, digest it first.
        if (seclen > 64) {              // 64: block size of HMAC-MD5
            md5.update(secret, 0, seclen);
            secKey = md5.digest();
            md5.reset();
            keyLen = secKey.length;
        }
        expand(md5, 16, secKey, 0, keyLen, labelBytes, seed, output,
            HMAC_ipad64.clone(), HMAC_opad64.clone());

        // P_SHA-1(S2, label + seed)
        // If we have a long secret, digest it first.
        if (seclen > 64) {              // 64: block size of HMAC-SHA1
            sha.update(secret, off, seclen);
            secKey = sha.digest();
            sha.reset();
            keyLen = secKey.length;
            off = 0;
        }
        expand(sha, 20, secKey, off, keyLen, labelBytes, seed, output,
            HMAC_ipad64.clone(), HMAC_opad64.clone());

        return output;
    }

    /*
     * @param digest the MessageDigest to produce the HMAC
     * @param hmacSize the HMAC size
     * @param secret the secret
     * @param secOff the offset into the secret
     * @param secLen the secret length
     * @param label the label
     * @param seed the seed
     * @param output the output array
     */
    private static void expand(MessageDigest digest, int hmacSize,
            byte[] secret, int secOff, int secLen, byte[] label, byte[] seed,
            byte[] output, byte[] pad1, byte[] pad2) throws DigestException {
        /*
         * modify the padding used, by XORing the key into our copy of that
         * padding.  That's to avoid doing that for each HMAC computation.
         */
        for (int i = 0; i < secLen; i++) {
            pad1[i] ^= secret[i + secOff];
            pad2[i] ^= secret[i + secOff];
        }

        byte[] tmp = new byte[hmacSize];
        byte[] aBytes = null;

        /*
         * compute:
         *
         *     P_hash(secret, seed) = HMAC_hash(secret, A(1) + seed) +
         *                            HMAC_hash(secret, A(2) + seed) +
         *                            HMAC_hash(secret, A(3) + seed) + ...
         * A() is defined as:
         *
         *     A(0) = seed
         *     A(i) = HMAC_hash(secret, A(i-1))
         */
        int remaining = output.length;
        int ofs = 0;
        while (remaining > 0) {
            /*
             * compute A() ...
             */
            // inner digest
            digest.update(pad1);
            if (aBytes == null) {
                digest.update(label);
                digest.update(seed);
            } else {
                digest.update(aBytes);
            }
            digest.digest(tmp, 0, hmacSize);

            // outer digest
            digest.update(pad2);
            digest.update(tmp);
            if (aBytes == null) {
                aBytes = new byte[hmacSize];
            }
            digest.digest(aBytes, 0, hmacSize);

            /*
             * compute HMAC_hash() ...
             */
            // inner digest
            digest.update(pad1);
            digest.update(aBytes);
            digest.update(label);
            digest.update(seed);
            digest.digest(tmp, 0, hmacSize);

            // outer digest
            digest.update(pad2);
            digest.update(tmp);
            digest.digest(tmp, 0, hmacSize);

            digest.reset();

            int k = Math.min(hmacSize, remaining);
            for (int i = 0; i < k; i++) {
                output[ofs++] ^= tmp[i];
            }
            remaining -= k;
        }
        Arrays.fill(tmp, (byte)0);
    }

    /**
     * A KeyGenerator implementation that supports TLS 1.2.
     * <p>
     * TLS 1.2 uses a different hash algorithm than 1.0/1.1 for the PRF
     * calculations.  As of 2010, there is no PKCS11-level support for TLS
     * 1.2 PRF calculations, and no known OS's have an internal variant
     * we could use.  Therefore for TLS 1.2, we are updating JSSE to request
     * a different provider algorithm:  "SunTls12Prf".  If we reused the
     * name "SunTlsPrf", the PKCS11 provider would need be updated to
     * fail correctly when presented with the wrong version number
     * (via Provider.Service.supportsParameters()), and add the
     * appropriate supportsParamters() checks into KeyGenerators (not
     * currently there).
     */
    public static class V12 extends TlsPrfGenerator {
        protected SecretKey engineGenerateKey() {
            return engineGenerateKey0(true);
        }
    }

    /**
     * A KeyGenerator implementation that supports TLS 1.0/1.1.
     */
    public static class V10 extends TlsPrfGenerator {
        protected SecretKey engineGenerateKey() {
            return engineGenerateKey0(false);
        }
    }
}
