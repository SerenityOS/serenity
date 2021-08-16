/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.provider;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;

public class CtrDrbg extends AbstractDrbg {

    private static final int AES_LIMIT;

    static {
        try {
            AES_LIMIT = Cipher.getMaxAllowedKeyLength("AES");
        } catch (Exception e) {
            // should not happen
            throw new AssertionError("Cannot detect AES", e);
        }
    }

    private Cipher cipher;

    private String cipherAlg;
    private String keyAlg;

    private int ctrLen;
    private int blockLen;
    private int keyLen;
    private int seedLen;

    private byte[] v;
    private byte[] k;

    public CtrDrbg(SecureRandomParameters params) {
        mechName = "CTR_DRBG";
        configure(params);
    }

    private static int alg2strength(String algorithm) {
        switch (algorithm.toUpperCase(Locale.ROOT)) {
            case "AES-128":
                return 128;
            case "AES-192":
                return 192;
            case "AES-256":
                return 256;
            default:
                throw new IllegalArgumentException(algorithm +
                        " not supported in CTR_DBRG");
        }
    }

    @Override
    protected void chooseAlgorithmAndStrength() {
        if (requestedAlgorithm != null) {
            algorithm = requestedAlgorithm.toUpperCase(Locale.ROOT);
            int supportedStrength = alg2strength(algorithm);
            if (requestedInstantiationSecurityStrength >= 0) {
                int tryStrength = getStandardStrength(
                        requestedInstantiationSecurityStrength);
                if (tryStrength > supportedStrength) {
                    throw new IllegalArgumentException(algorithm +
                            " does not support strength " +
                            requestedInstantiationSecurityStrength);
                }
                this.securityStrength = tryStrength;
            } else {
                this.securityStrength = (DEFAULT_STRENGTH > supportedStrength) ?
                        supportedStrength : DEFAULT_STRENGTH;
            }
        } else {
            int tryStrength = (requestedInstantiationSecurityStrength < 0) ?
                    DEFAULT_STRENGTH : requestedInstantiationSecurityStrength;
            tryStrength = getStandardStrength(tryStrength);
            // Default algorithm, use AES-128 if AES-256 is not available.
            // Remember to sync with "securerandom.drbg.config" in java.security
            if (tryStrength <= 128 && AES_LIMIT < 256) {
                algorithm = "AES-128";
            } else if (AES_LIMIT >= 256) {
                algorithm = "AES-256";
            } else {
                throw new IllegalArgumentException("unsupported strength " +
                        requestedInstantiationSecurityStrength);
            }
            this.securityStrength = tryStrength;
        }
        switch (algorithm.toUpperCase(Locale.ROOT)) {
            case "AES-128":
            case "AES-192":
            case "AES-256":
                this.keyAlg = "AES";
                this.cipherAlg = "AES/ECB/NoPadding";
                switch (algorithm) {
                    case "AES-128":
                        this.keyLen = 128 / 8;
                        break;
                    case "AES-192":
                        this.keyLen = 192 / 8;
                        if (AES_LIMIT < 192) {
                            throw new IllegalArgumentException(algorithm +
                                " not available (because policy) in CTR_DBRG");
                        }
                        break;
                    case "AES-256":
                        this.keyLen = 256 / 8;
                        if (AES_LIMIT < 256) {
                            throw new IllegalArgumentException(algorithm +
                                " not available (because policy) in CTR_DBRG");
                        }
                        break;
                    default:
                        throw new IllegalArgumentException(algorithm +
                            " not supported in CTR_DBRG");
                }
                this.blockLen = 128 / 8;
                break;
            default:
                throw new IllegalArgumentException(algorithm +
                        " not supported in CTR_DBRG");
        }
        this.seedLen = this.blockLen + this.keyLen;
        this.ctrLen = this.blockLen;    // TODO
        if (usedf) {
            this.minLength = this.securityStrength / 8;
        } else {
            this.minLength = this.maxLength =
                    this.maxPersonalizationStringLength =
                            this.maxAdditionalInputLength = seedLen;
        }
    }

    /**
     * This call, used by the constructors, instantiates the digest.
     */
    @Override
    protected void initEngine() {
        try {
            /*
             * Use the local SunJCE implementation to avoid native
             * performance overhead.
             */
            cipher = Cipher.getInstance(cipherAlg, "SunJCE");
        } catch (NoSuchProviderException | NoSuchAlgorithmException
                | NoSuchPaddingException e) {
            // Fallback to any available.
            try {
                cipher = Cipher.getInstance(cipherAlg);
            } catch (NoSuchAlgorithmException | NoSuchPaddingException exc) {
                throw new InternalError(
                    "internal error: " + cipherAlg + " not available.", exc);
            }
        }
    }

    private void status() {
        if (debug != null) {
            debug.println(this, "Key = " + HexFormat.of().formatHex(k));
            debug.println(this, "V   = " + HexFormat.of().formatHex(v));
            debug.println(this, "reseed counter = " + reseedCounter);
        }
    }

    // 800-90Ar1 10.2.1.2. CTR_DRBG_Update
    private void update(byte[] input) {
        if (input.length != seedLen) {
            // Should not happen
            throw new IllegalArgumentException("input length not seedLen: "
                    + input.length);
        }
        try {

            int m = (seedLen + blockLen - 1) / blockLen;
            byte[] temp = new byte[m * blockLen];

            // Step 1. temp = Null.

            // Step 2. Loop
            for (int i = 0; i < m; i++) {
                // Step 2.1. Increment
                addOne(v, ctrLen);
                // Step 2.2. Block_Encrypt
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(k, keyAlg));
                // Step 2.3. Encrypt into right position, no need to cat
                cipher.doFinal(v, 0, blockLen, temp, i * blockLen);
            }

            // Step 3. Truncate
            temp = Arrays.copyOf(temp, seedLen);

            // Step 4: Add
            for (int i = 0; i < seedLen; i++) {
                temp[i] ^= input[i];
            }

            // Step 5: leftmost
            k = Arrays.copyOf(temp, keyLen);

            // Step 6: rightmost
            v = Arrays.copyOfRange(temp, seedLen - blockLen, seedLen);

            // Step 7. Return
        } catch (GeneralSecurityException e) {
            throw new InternalError(e);
        }
    }

    @Override
    protected void instantiateAlgorithm(byte[] ei) {
        if (debug != null) {
            debug.println(this, "instantiate");
        }
        byte[] more;
        if (usedf) {
            // 800-90Ar1 10.2.1.3.2 Step 1-2. cat bytes
            if (personalizationString == null) {
                more = nonce;
            } else {
                if (nonce.length + personalizationString.length < 0) {
                    // Length must be represented as a 32 bit integer in df()
                    throw new IllegalArgumentException(
                            "nonce plus personalization string is too long");
                }
                more = Arrays.copyOf(
                        nonce, nonce.length + personalizationString.length);
                System.arraycopy(personalizationString, 0, more, nonce.length,
                        personalizationString.length);
            }
        } else {
            // 800-90Ar1 10.2.1.3.1
            // Step 1-2, no need to expand personalizationString, we only XOR
            // with shorter length
            more = personalizationString;
        }
        reseedAlgorithm(ei, more);
    }

    /**
     * Block_cipher_df in 10.3.2
     *
     * @param input the input string
     * @return the output block (always of seedLen)
     */
    private byte[] df(byte[] input) {
        // 800-90Ar1 10.3.2
        // 2. L = len (input_string)/8
        int l = input.length;
        // 3. N = number_of_bits_to_return/8
        int n = seedLen;
        // 4. S = L || N || input_string || 0x80
        byte[] ln = new byte[8];
        ln[0] = (byte)(l >> 24);
        ln[1] = (byte)(l >> 16);
        ln[2] = (byte)(l >> 8);
        ln[3] = (byte)(l);
        ln[4] = (byte)(n >> 24);
        ln[5] = (byte)(n >> 16);
        ln[6] = (byte)(n >> 8);
        ln[7] = (byte)(n);

        // 5. Zero padding of S
        // Not necessary, see bcc

        // 8. K = leftmost (0x00010203...1D1E1F, keylen).
        byte[] k = new byte[keyLen];
        for (int i = 0; i < k.length; i++) {
            k[i] = (byte)i;
        }

        // 6. temp = the Null String
        byte[] temp = new byte[seedLen];

        // 7. i = 0
        for (int i = 0; i * blockLen < temp.length; i++) {
            // 9.1 IV = i || 0^(outlen - len (i)). outLen is blockLen
            byte[] iv = new byte[blockLen];
            iv[0] = (byte)(i >> 24);
            iv[1] = (byte)(i >> 16);
            iv[2] = (byte)(i >> 8);
            iv[3] = (byte)(i);

            int tailLen = temp.length - blockLen*i;
            if (tailLen > blockLen) {
                tailLen = blockLen;
            }
            // 9.2 temp = temp || BCC (K, (IV || S)).
            System.arraycopy(bcc(k, iv, ln, input, new byte[]{(byte)0x80}),
                    0, temp, blockLen*i, tailLen);
        }

        // 10. K = leftmost(temp, keylen)
        k = Arrays.copyOf(temp, keyLen);

        // 11. x = select(temp, keylen+1, keylen+outlen)
        byte[] x = Arrays.copyOfRange(temp, keyLen, temp.length);

        // 12. temp = the Null string
        // No need to clean up, temp will be overwritten

        for (int i = 0; i * blockLen < seedLen; i++) {
            try {
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(k, keyAlg));
                int tailLen = temp.length - blockLen*i;
                // 14. requested_bits = leftmost(temp, nuumber_of_bits_to_return)
                if (tailLen > blockLen) {
                    tailLen = blockLen;
                }
                x = cipher.doFinal(x);
                System.arraycopy(x, 0, temp, blockLen * i, tailLen);
            } catch (GeneralSecurityException e) {
                throw new InternalError(e);
            }
        }

        // 15. Return
        return temp;
    }

    /**
     * Block_Encrypt in 10.3.3
     *
     * @param k the key
     * @param data after concatenated, the data to be operated upon. This is
     *             a series of byte[], each with an arbitrary length. Note
     *             that the full length is not necessarily a multiple of
     *             outlen. XOR with zero is no-op.
     * @return the result
     */
    private byte[] bcc(byte[] k, byte[]... data) {
        byte[] chain = new byte[blockLen];
        int n1 = 0; // index in data
        int n2 = 0; // index in data[n1]
        // pack blockLen of bytes into chain from data[][], again and again
        while (n1 < data.length) {
            int j;
            out: for (j = 0; j < blockLen; j++) {
                while (n2 >= data[n1].length) {
                    n1++;
                    if (n1 >= data.length) {
                        break out;
                    }
                    n2 = 0;
                }
                chain[j] ^= data[n1][n2];
                n2++;
            }
            if (j == 0) { // all data happens to be consumed in the last loop
                break;
            }
            try {
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(k, keyAlg));
                chain = cipher.doFinal(chain);
            } catch (GeneralSecurityException e) {
                throw new InternalError(e);
            }
        }
        return chain;
    }

    @Override
    protected synchronized void reseedAlgorithm(
            byte[] ei,
            byte[] additionalInput) {
        if (usedf) {
            // 800-90Ar1 10.2.1.3.2 Instantiate.
            // 800-90Ar1 10.2.1.4.2 Reseed.

            // Step 1: cat bytes
            if (additionalInput != null) {
                if (ei.length + additionalInput.length < 0) {
                    // Length must be represented as a 32 bit integer in df()
                    throw new IllegalArgumentException(
                            "entropy plus additional input is too long");
                }
                byte[] temp = Arrays.copyOf(
                        ei, ei.length + additionalInput.length);
                System.arraycopy(additionalInput, 0, temp, ei.length,
                        additionalInput.length);
                ei = temp;
            }
            // Step 2. df (seed_material, seedlen).
            ei = df(ei);
        } else {
            // 800-90Ar1 10.2.1.3.1 Instantiate
            // 800-90Ar1 10.2.1.4.1 Reseed
            // Step 1-2. Needless
            // Step 3. seed_material = entropy_input XOR more
            if (additionalInput != null) {
                // additionalInput.length <= seedLen
                for (int i = 0; i < additionalInput.length; i++) {
                    ei[i] ^= additionalInput[i];
                }
            }
        }

        if (v == null) {
            // 800-90Ar1 10.2.1.3.2 Instantiate. Step 3-4
            // 800-90Ar1 10.2.1.3.1 Instantiate. Step 4-5
            k = new byte[keyLen];
            v = new byte[blockLen];
        }
        //status();

        // 800-90Ar1 10.2.1.3.1 Instantiate. Step 6
        // 800-90Ar1 10.2.1.3.2 Instantiate. Step 5
        // 800-90Ar1 10.2.1.4.1 Reseed. Step 4
        // 800-90Ar1 10.2.1.4.2 Reseed. Step 3
        update(ei);
        // 800-90Ar1 10.2.1.3.1 Instantiate. Step 7
        // 800-90Ar1 10.2.1.3.2 Instantiate. Step 6
        // 800-90Ar1 10.2.1.4.1 Reseed. Step 5
        // 800-90Ar1 10.2.1.4.2 Reseed. Step 4
        reseedCounter = 1;
        //status();

        // Whatever step. Return
    }

    /**
     * Add one to data, only touch the last len bytes.
     */
    private static void addOne(byte[] data, int len) {
        for (int i = 0; i < len; i++) {
            data[data.length - 1 - i]++;
            if (data[data.length - 1 - i] != 0) {
                break;
            }
        }
    }

    @Override
    public synchronized void generateAlgorithm(
            byte[] result, byte[] additionalInput) {

        if (debug != null) {
            debug.println(this, "generateAlgorithm");
        }

        // 800-90Ar1 10.2.1.5.1 Generate
        // 800-90Ar1 10.2.1.5.2 Generate

        // Step 1: Check reseed_counter. Will not fail. Already checked in
        // AbstractDrbg#engineNextBytes.

        if (additionalInput != null) {
            if (usedf) {
                // 10.2.1.5.2 Step 2.1
                additionalInput = df(additionalInput);
            } else {
                // 10.2.1.5.1 Step 2.1-2.2
                additionalInput = Arrays.copyOf(additionalInput, seedLen);
            }
            // 10.2.1.5.1 Step 2.3
            // 10.2.1.5.2 Step 2.2
            update(additionalInput);
        } else {
            // 10.2.1.5.1 Step 2 Else
            // 10.2.1.5.2 Step 2 Else
            additionalInput = new byte[seedLen];
        }

        // Step 3. temp = Null
        int pos = 0;
        int len = result.length;

        // Step 4. Loop
        while (len > 0) {
            // Step 4.1. Increment
            addOne(v, ctrLen);
            try {
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(k, keyAlg));
                // Step 4.2. Encrypt
                // Step 4.3 and 5. Cat bytes and leftmost
                if (len > blockLen) {
                    cipher.doFinal(v, 0, blockLen, result, pos);
                } else {
                    byte[] out = cipher.doFinal(v);
                    System.arraycopy(out, 0, result, pos, len);
                    Arrays.fill(out, (byte)0);
                }
            } catch (GeneralSecurityException e) {
                throw new InternalError(e);
            }
            len -= blockLen;
            if (len <= 0) {
                // shortcut, so that pos needn't be updated
                break;
            }
            pos += blockLen;
        }

        // Step 6. Update
        update(additionalInput);

        // Step 7. reseed_counter++
        reseedCounter++;

        //status();

        // Step 8. Return
    }

    @Override
    public String toString() {
        return super.toString() + ","
                + (usedf ? "use_df" : "no_df");
    }
}
