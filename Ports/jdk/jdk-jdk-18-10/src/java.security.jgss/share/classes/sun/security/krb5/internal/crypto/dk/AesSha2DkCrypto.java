/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 */

package sun.security.krb5.internal.crypto.dk;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import sun.security.krb5.KrbCryptoException;
import sun.security.krb5.Confounder;
import sun.security.krb5.internal.crypto.KeyUsage;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class provides the implementation of AES Encryption with
 * HMAC-SHA2 for Kerberos 5
 * https://tools.ietf.org/html/rfc8009
 *
 * Algorithm profile described in [KCRYPTO]:
 * +--------------------------------------------------------------------+
 * |               protocol key format          128- or 256-bit string  |
 * |                                                                    |
 * |            string-to-key function          PBKDF2+DK with variable |
 * |                                          iteration count (see      |
 * |                                          above)                    |
 * |                                                                    |
 * |  default string-to-key parameters          00 00 80 00             |
 * |                                                                    |
 * |        key-generation seed length          key size                |
 * |                                                                    |
 * |            random-to-key function          identity function       |
 * |                                                                    |
 * |                  hash function, H          SHA-256 / SHA-384       |
 * |                                                                    |
 * |               HMAC output size, h          16/24 octets            |
 * |                                                                    |
 * |             message block size, m          1 octet                 |
 * |                                                                    |
 * |  encryption/decryption functions,          AES in CBC-CTS mode     |
 * |  E and D                                 (cipher block size 16     |
 * |                                          octets), with next to     |
 * |                                          last block as CBC-style   |
 * |                                          ivec                      |
 * +--------------------------------------------------------------------+
 *
 * Supports aes128-cts-hmac-sha256-128 and aes256-cts-hmac-sha384-192
 */

public class AesSha2DkCrypto extends DkCrypto {

    private static final boolean debug = false;

    private static final int BLOCK_SIZE = 16;
    private static final int DEFAULT_ITERATION_COUNT = 32768;
    private static final byte[] ZERO_IV = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0,
                                                       0, 0, 0, 0, 0, 0, 0, 0 };

    private static final byte[] ETYPE_NAME_128 =
            "aes128-cts-hmac-sha256-128".getBytes();
    private static final byte[] ETYPE_NAME_256 =
            "aes256-cts-hmac-sha384-192".getBytes();

    private final int hashSize;
    private final int keyLength;

    public AesSha2DkCrypto(int length) {
        keyLength = length;
        hashSize = (length == 128?128:192)/8;
    }

    protected int getKeySeedLength() {
        return keyLength;   // bits; AES key material
    }

    public byte[] stringToKey(char[] password, String salt, byte[] s2kparams)
        throws GeneralSecurityException {

        byte[] saltUtf8 = null;
        try {
            saltUtf8 = salt.getBytes(UTF_8);
            return stringToKey(password, saltUtf8, s2kparams);
        } catch (Exception e) {
            return null;
        } finally {
            if (saltUtf8 != null) {
                Arrays.fill(saltUtf8, (byte)0);
            }
        }
    }

    // https://tools.ietf.org/html/rfc8009#section-4
    private byte[] stringToKey(char[] secret, byte[] salt, byte[] params)
        throws GeneralSecurityException {

        int iter_count = DEFAULT_ITERATION_COUNT;
        if (params != null) {
            if (params.length != 4) {
                throw new RuntimeException("Invalid parameter to stringToKey");
            }
            iter_count = readBigEndian(params, 0, 4);
        }

        byte[] saltp = new byte[26 + 1 + salt.length];
        if (keyLength == 128) {
            System.arraycopy(ETYPE_NAME_128, 0, saltp, 0, 26);
        } else {
            System.arraycopy(ETYPE_NAME_256, 0, saltp, 0, 26);
        }
        System.arraycopy(salt, 0, saltp, 27, salt.length);
        byte[] tmpKey = randomToKey(PBKDF2(secret, saltp, iter_count,
                                        getKeySeedLength()));
        byte[] result = dk(tmpKey, KERBEROS_CONSTANT);
        return result;
    }

    protected byte[] randomToKey(byte[] in) {
        // simple identity operation
        return in;
    }

    /*
     * https://tools.ietf.org/html/rfc8009#section-3 defines
     * a new key derivation function:
     *
     * KDF-HMAC-SHA2(key, label, k) = k-truncate(K1)
     * K1 = HMAC-SHA-256(key, 0x00000001 | label | 0x00 | k) or
     * K1 = HMAC-SHA-384(key, 0x00000001 | label | 0x00 | k)
     *
     * where label is constant below.
     */
    protected byte[] dr(byte[] key, byte[] constant)
            throws GeneralSecurityException {
        byte[] result;
        byte[] input = new byte[constant.length + 9];
        // 0x00000001 at the beginning
        input[3] = 1;
        // label follows
        System.arraycopy(constant, 0, input, 4, constant.length);
        SecretKeySpec tkey = new SecretKeySpec(key, "HMAC");
        Mac mac = Mac.getInstance(
                keyLength == 128? "HmacSHA256": "HmacSHA384");
        mac.init(tkey);

        int k;
        if (keyLength == 128) {
            // key length for enc and hmac both 128
            k = 128;
        } else {
            byte last = constant[constant.length-1];
            if (last == (byte)0x99 || last == (byte)0x55) {
                // 192 for hmac
                k = 192;
            } else {
                // 256 for enc
                k = 256;
            }
        }
        // 0x00 and k at the end
        input[input.length - 1] = (byte)(k);
        input[input.length - 2] = (byte)(k / 256);

        result = mac.doFinal(input);
        return Arrays.copyOf(result, k / 8);
    }

    protected Cipher getCipher(byte[] key, byte[] ivec, int mode)
        throws GeneralSecurityException {

        // IV
        if (ivec == null) {
           ivec = ZERO_IV;
        }
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        IvParameterSpec encIv = new IvParameterSpec(ivec, 0, ivec.length);
        cipher.init(mode, secretKey, encIv);
        return cipher;
    }

    // get an instance of the AES Cipher in CTS mode
    public int getChecksumLength() {
        return hashSize;  // bytes
    }

    /**
     * Get the truncated HMAC
     */
    protected byte[] getHmac(byte[] key, byte[] msg)
        throws GeneralSecurityException {

        SecretKey keyKi = new SecretKeySpec(key, "HMAC");
        Mac m = Mac.getInstance(keyLength == 128 ? "HmacSHA256" : "HmacSHA384");
        m.init(keyKi);

        // generate hash
        byte[] hash = m.doFinal(msg);

        // truncate hash
        byte[] output = new byte[hashSize];
        System.arraycopy(hash, 0, output, 0, hashSize);
        return output;
    }

    private byte[] deriveKey(byte[] baseKey, int usage, byte type)
            throws GeneralSecurityException {
        byte[] constant = new byte[5];
        constant[0] = (byte) ((usage>>24)&0xff);
        constant[1] = (byte) ((usage>>16)&0xff);
        constant[2] = (byte) ((usage>>8)&0xff);
        constant[3] = (byte) (usage&0xff);
        constant[4] = type;
        return dk(baseKey, constant);
    }

    /**
     * Calculate the checksum
     */
    public byte[] calculateChecksum(byte[] baseKey, int usage, byte[] input,
        int start, int len) throws GeneralSecurityException {

        if (!KeyUsage.isValid(usage)) {
            throw new GeneralSecurityException("Invalid key usage number: "
                                                + usage);
        }

        byte[] Kc = deriveKey(baseKey, usage, (byte) 0x99);  // Checksum key
        if (debug) {
            System.err.println("usage: " + usage);
            traceOutput("input", input, start, Math.min(len, 32));
            traceOutput("baseKey", baseKey, 0, baseKey.length);
            traceOutput("Kc", Kc, 0, Kc.length);
        }

        try {
            // Generate checksum
            // H1 = HMAC(Kc, input)
            byte[] hmac = getHmac(Kc, input);
            if (debug) {
                traceOutput("hmac", hmac, 0, hmac.length);
            }
            if (hmac.length == getChecksumLength()) {
                return hmac;
            } else if (hmac.length > getChecksumLength()) {
                byte[] buf = new byte[getChecksumLength()];
                System.arraycopy(hmac, 0, buf, 0, buf.length);
                return buf;
            } else {
                throw new GeneralSecurityException("checksum size too short: " +
                        hmac.length + "; expecting : " + getChecksumLength());
            }
        } finally {
            Arrays.fill(Kc, 0, Kc.length, (byte)0);
        }
    }

    /**
     * Performs encryption using derived key; adds confounder.
     */
    public byte[] encrypt(byte[] baseKey, int usage,
        byte[] ivec, byte[] new_ivec, byte[] plaintext, int start, int len)
        throws GeneralSecurityException, KrbCryptoException {

        if (!KeyUsage.isValid(usage)) {
            throw new GeneralSecurityException("Invalid key usage number: "
                                                 + usage);
        }
        byte[] output = encryptCTS(baseKey, usage, ivec, new_ivec, plaintext,
                                        start, len, true);
        return output;
    }

    /**
     * Performs encryption using derived key; does not add confounder.
     */
    public byte[] encryptRaw(byte[] baseKey, int usage,
        byte[] ivec, byte[] plaintext, int start, int len)
        throws GeneralSecurityException, KrbCryptoException {

        if (!KeyUsage.isValid(usage)) {
            throw new GeneralSecurityException("Invalid key usage number: "
                                                + usage);
        }
        byte[] output = encryptCTS(baseKey, usage, ivec, null, plaintext,
                                        start, len, false);
        return output;
    }

    /**
     * @param baseKey key from which keys are to be derived using usage
     * @param ciphertext  E(Ke, conf | plaintext | padding, ivec) | H1[1..h]
     */
    public byte[] decrypt(byte[] baseKey, int usage, byte[] ivec,
        byte[] ciphertext, int start, int len) throws GeneralSecurityException {

        if (!KeyUsage.isValid(usage)) {
            throw new GeneralSecurityException("Invalid key usage number: "
                                                + usage);
        }
        byte[] output = decryptCTS(baseKey, usage, ivec, ciphertext,
                                        start, len, true);
        return output;
    }

    /**
     * Decrypts data using specified key and initial vector.
     * @param baseKey encryption key to use
     * @param ciphertext  encrypted data to be decrypted
     * @param usage ignored
     */
    public byte[] decryptRaw(byte[] baseKey, int usage, byte[] ivec,
        byte[] ciphertext, int start, int len)
        throws GeneralSecurityException {

        if (!KeyUsage.isValid(usage)) {
            throw new GeneralSecurityException("Invalid key usage number: "
                                                + usage);
        }
        byte[] output = decryptCTS(baseKey, usage, ivec, ciphertext,
                                        start, len, false);
        return output;
    }

    /**
     * Encrypt AES in CBC-CTS mode using derived keys.
     */
    private byte[] encryptCTS(byte[] baseKey, int usage, byte[] ivec,
        byte[] new_ivec, byte[] plaintext, int start, int len,
        boolean confounder_exists)
        throws GeneralSecurityException, KrbCryptoException {

        byte[] Ke = null;
        byte[] Ki = null;

        if (debug) {
            System.err.println("usage: " + usage);
            if (ivec != null) {
                traceOutput("old_state.ivec", ivec, 0, ivec.length);
            }
            traceOutput("plaintext", plaintext, start, Math.min(len, 32));
            traceOutput("baseKey", baseKey, 0, baseKey.length);
        }

        try {
            Ke = deriveKey(baseKey, usage, (byte) 0xaa);  // Encryption key

            byte[] toBeEncrypted = null;
            if (confounder_exists) {
                byte[] confounder = Confounder.bytes(BLOCK_SIZE);
                toBeEncrypted = new byte[confounder.length + len];
                System.arraycopy(confounder, 0, toBeEncrypted,
                                        0, confounder.length);
                System.arraycopy(plaintext, start, toBeEncrypted,
                                        confounder.length, len);
            } else {
                toBeEncrypted = new byte[len];
                System.arraycopy(plaintext, start, toBeEncrypted, 0, len);
            }

            // encryptedData + HMAC
            byte[] output = new byte[toBeEncrypted.length + hashSize];

            // AES in JCE
            Cipher cipher = Cipher.getInstance("AES/CTS/NoPadding");
            SecretKeySpec secretKey = new SecretKeySpec(Ke, "AES");
            IvParameterSpec encIv = new IvParameterSpec(ivec, 0, ivec.length);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, encIv);
            cipher.doFinal(toBeEncrypted, 0, toBeEncrypted.length, output);

            Ki = deriveKey(baseKey, usage, (byte) 0x55);
            if (debug) {
                traceOutput("Ki", Ki, 0, Ke.length);
            }

            // Generate checksum
            // H = HMAC(Ki, IV | C)
            byte[] msg = Arrays.copyOf(ivec, ivec.length + toBeEncrypted.length);
            System.arraycopy(output, 0, msg, ivec.length, toBeEncrypted.length);
            byte[] hmac = getHmac(Ki, msg);

            // encryptedData + HMAC
            System.arraycopy(hmac, 0, output, toBeEncrypted.length,
                                hmac.length);
            return output;
        } finally {
            if (Ke != null) {
                Arrays.fill(Ke, 0, Ke.length, (byte) 0);
            }
            if (Ki != null) {
                Arrays.fill(Ki, 0, Ki.length, (byte) 0);
            }
        }
    }

    /**
     * Decrypt AES in CBC-CTS mode using derived keys.
     */
    private byte[] decryptCTS(byte[] baseKey, int usage, byte[] ivec,
        byte[] ciphertext, int start, int len, boolean confounder_exists)
        throws GeneralSecurityException {

        byte[] Ke = null;
        byte[] Ki = null;

        try {
            Ke = deriveKey(baseKey, usage, (byte) 0xaa);  // Encryption key

            if (debug) {
                System.err.println("usage: " + usage);
                if (ivec != null) {
                    traceOutput("old_state.ivec", ivec, 0, ivec.length);
                }
                traceOutput("ciphertext", ciphertext, start, Math.min(len, 32));
                traceOutput("baseKey", baseKey, 0, baseKey.length);
                traceOutput("Ke", Ke, 0, Ke.length);
            }

            // Decrypt [confounder | plaintext ] (without checksum)

            // AES in JCE
            Cipher cipher = Cipher.getInstance("AES/CTS/NoPadding");
            SecretKeySpec secretKey = new SecretKeySpec(Ke, "AES");
            IvParameterSpec encIv = new IvParameterSpec(ivec, 0, ivec.length);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, encIv);
            byte[] plaintext = cipher.doFinal(ciphertext, start, len-hashSize);

            if (debug) {
                traceOutput("AES PlainText", plaintext, 0,
                                Math.min(plaintext.length, 32));
            }

            Ki = deriveKey(baseKey, usage, (byte) 0x55);  // Integrity key
            if (debug) {
                traceOutput("Ki", Ki, 0, Ke.length);
            }

            // Verify checksum
            // H = HMAC(Ki, IV | C)
            byte[] msg = Arrays.copyOf(ivec, ivec.length + len-hashSize);
            System.arraycopy(ciphertext, start, msg, ivec.length, len-hashSize);
            byte[] calculatedHmac = getHmac(Ki, msg);
            int hmacOffset = start + len - hashSize;
            if (debug) {
                traceOutput("calculated Hmac", calculatedHmac,
                                0, calculatedHmac.length);
                traceOutput("message Hmac", ciphertext, hmacOffset, hashSize);
            }
            boolean cksumFailed = false;
            if (calculatedHmac.length >= hashSize) {
                for (int i = 0; i < hashSize; i++) {
                    if (calculatedHmac[i] != ciphertext[hmacOffset+i]) {
                        cksumFailed = true;
                        if (debug) {
                            System.err.println("Checksum failed !");
                        }
                        break;
                    }
                }
            }
            if (cksumFailed) {
                throw new GeneralSecurityException("Checksum failed");
            }

            if (confounder_exists) {
                // Get rid of confounder
                // [ confounder | plaintext ]
                byte[] output = new byte[plaintext.length - BLOCK_SIZE];
                System.arraycopy(plaintext, BLOCK_SIZE, output,
                                        0, output.length);
                return output;
            } else {
                return plaintext;
            }
        } finally {
            if (Ke != null) {
                Arrays.fill(Ke, 0, Ke.length, (byte) 0);
            }
            if (Ki != null) {
                Arrays.fill(Ki, 0, Ki.length, (byte) 0);
            }
        }
    }

    /*
     * Invoke the PKCS#5 PBKDF2 algorithm
     */
    private static byte[] PBKDF2(char[] secret, byte[] salt,
        int count, int keyLength) throws GeneralSecurityException {

        PBEKeySpec keySpec = new PBEKeySpec(secret, salt, count, keyLength);
        SecretKeyFactory skf =
                SecretKeyFactory.getInstance(keyLength == 128 ?
                        "PBKDF2WithHmacSHA256" : "PBKDF2WithHmacSHA384");
        SecretKey key = skf.generateSecret(keySpec);
        byte[] result = key.getEncoded();

        return result;
    }

    public static final int readBigEndian(byte[] data, int pos, int size) {
        int retVal = 0;
        int shifter = (size-1)*8;
        while (size > 0) {
            retVal += (data[pos] & 0xff) << shifter;
            shifter -= 8;
            pos++;
            size--;
        }
        return retVal;
    }

}
