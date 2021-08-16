/*
 * Copyright (c) 2005, 2008, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.krb5.internal.crypto.dk;

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.util.*;
import sun.security.krb5.EncryptedData;
import sun.security.krb5.KrbCryptoException;
import sun.security.krb5.Confounder;
import sun.security.krb5.internal.crypto.KeyUsage;

/**
 * Support for ArcFour in Kerberos
 * as defined in RFC 4757.
 * http://www.ietf.org/rfc/rfc4757.txt
 *
 * @author Seema Malkani
 */

public class ArcFourCrypto extends DkCrypto {

    private static final boolean debug = false;

    private static final int confounderSize = 8;
    private static final byte[] ZERO_IV = new byte[] {0, 0, 0, 0, 0, 0, 0, 0};
    private static final int hashSize = 16;
    private final int keyLength;

    public ArcFourCrypto(int length) {
        keyLength = length;
    }

    protected int getKeySeedLength() {
        return keyLength;   // bits; RC4 key material
    }

    protected byte[] randomToKey(byte[] in) {
        // simple identity operation
        return in;
    }

    public byte[] stringToKey(char[] passwd)
        throws GeneralSecurityException {
        return stringToKey(passwd, null);
    }

    /*
     * String2Key(Password)
     * K = MD4(UNICODE(password))
     */
    private byte[] stringToKey(char[] secret, byte[] opaque)
        throws GeneralSecurityException {

        if (opaque != null && opaque.length > 0) {
            throw new RuntimeException("Invalid parameter to stringToKey");
        }

        byte[] passwd = null;
        byte[] digest = null;
        try {
            // convert ascii to unicode
            passwd = charToUtf16(secret);

            // provider for MD4
            MessageDigest md = sun.security.provider.MD4.getInstance();
            md.update(passwd);
            digest = md.digest();
        } catch (Exception e) {
            return null;
        } finally {
            if (passwd != null) {
                Arrays.fill(passwd, (byte)0);
            }
        }

        return digest;
    }

    protected Cipher getCipher(byte[] key, byte[] ivec, int mode)
        throws GeneralSecurityException {

        // IV
        if (ivec == null) {
           ivec = ZERO_IV;
        }
        SecretKeySpec secretKey = new SecretKeySpec(key, "ARCFOUR");
        Cipher cipher = Cipher.getInstance("ARCFOUR");
        IvParameterSpec encIv = new IvParameterSpec(ivec, 0, ivec.length);
        cipher.init(mode, secretKey, encIv);
        return cipher;
    }

    public int getChecksumLength() {
        return hashSize;  // bytes
    }

    /**
     * Get the HMAC-MD5
     */
    protected byte[] getHmac(byte[] key, byte[] msg)
        throws GeneralSecurityException {

        SecretKey keyKi = new SecretKeySpec(key, "HmacMD5");
        Mac m = Mac.getInstance("HmacMD5");
        m.init(keyKi);

        // generate hash
        byte[] hash = m.doFinal(msg);
        return hash;
    }

    /**
     * Calculate the checksum
     */
    public byte[] calculateChecksum(byte[] baseKey, int usage, byte[] input,
        int start, int len) throws GeneralSecurityException {

        if (debug) {
            System.out.println("ARCFOUR: calculateChecksum with usage = " +
                                                usage);
        }

        if (!KeyUsage.isValid(usage)) {
            throw new GeneralSecurityException("Invalid key usage number: "
                                                + usage);
        }

        byte[] Ksign = null;
        // Derive signing key from session key
        try {
           byte[] ss = "signaturekey".getBytes();
           // need to append end-of-string 00
           byte[] new_ss = new byte[ss.length+1];
           System.arraycopy(ss, 0, new_ss, 0, ss.length);
           Ksign = getHmac(baseKey, new_ss);
        } catch (Exception e) {
            GeneralSecurityException gse =
                new GeneralSecurityException("Calculate Checkum Failed!");
            gse.initCause(e);
            throw gse;
        }

        // get the salt using key usage
        byte[] salt = getSalt(usage);

        // Generate checksum of message
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            GeneralSecurityException gse =
                new GeneralSecurityException("Calculate Checkum Failed!");
            gse.initCause(e);
            throw gse;
        }
        messageDigest.update(salt);
        messageDigest.update(input, start, len);
        byte[] md5tmp = messageDigest.digest();

        // Generate checksum
        byte[] hmac = getHmac(Ksign, md5tmp);
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
    }

    /**
     * Performs encryption of Sequence Number using derived key.
     */
    public byte[] encryptSeq(byte[] baseKey, int usage,
        byte[] checksum, byte[] plaintext, int start, int len)
        throws GeneralSecurityException, KrbCryptoException {

        if (!KeyUsage.isValid(usage)) {
            throw new GeneralSecurityException("Invalid key usage number: "
                                                + usage);
        }
        // derive encryption for sequence number
        byte[] salt = new byte[4];
        byte[] kSeq = getHmac(baseKey, salt);

        // derive new encryption key salted with sequence number
        kSeq = getHmac(kSeq, checksum);

        Cipher cipher = Cipher.getInstance("ARCFOUR");
        SecretKeySpec secretKey = new SecretKeySpec(kSeq, "ARCFOUR");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] output = cipher.doFinal(plaintext, start, len);

        return output;
    }

    /**
     * Performs decryption of Sequence Number using derived key.
     */
    public byte[] decryptSeq(byte[] baseKey, int usage,
        byte[] checksum, byte[] ciphertext, int start, int len)
        throws GeneralSecurityException, KrbCryptoException {

        if (!KeyUsage.isValid(usage)) {
            throw new GeneralSecurityException("Invalid key usage number: "
                                                + usage);
        }

        // derive decryption for sequence number
        byte[] salt = new byte[4];
        byte[] kSeq = getHmac(baseKey, salt);

        // derive new encryption key salted with sequence number
        kSeq = getHmac(kSeq, checksum);

        Cipher cipher = Cipher.getInstance("ARCFOUR");
        SecretKeySpec secretKey = new SecretKeySpec(kSeq, "ARCFOUR");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] output = cipher.doFinal(ciphertext, start, len);

        return output;
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

        if (debug) {
            System.out.println("ArcFour: ENCRYPT with key usage = " + usage);
        }

        // get the confounder
        byte[] confounder = Confounder.bytes(confounderSize);

        // add confounder to the plaintext for encryption
        int plainSize = roundup(confounder.length + len, 1);
        byte[] toBeEncrypted = new byte[plainSize];
        System.arraycopy(confounder, 0, toBeEncrypted, 0, confounder.length);
        System.arraycopy(plaintext, start, toBeEncrypted,
                                confounder.length, len);

        /* begin the encryption, compute K1 */
        byte[] k1 = new byte[baseKey.length];
        System.arraycopy(baseKey, 0, k1, 0, baseKey.length);

        // get the salt using key usage
        byte[] salt = getSalt(usage);

        // compute K2 using K1
        byte[] k2 = getHmac(k1, salt);

        // generate checksum using K2
        byte[] checksum = getHmac(k2, toBeEncrypted);

        // compute K3 using K2 and checksum
        byte[] k3 = getHmac(k2, checksum);

        Cipher cipher = Cipher.getInstance("ARCFOUR");
        SecretKeySpec secretKey = new SecretKeySpec(k3, "ARCFOUR");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] output = cipher.doFinal(toBeEncrypted, 0, toBeEncrypted.length);

        // encryptedData + HMAC
        byte[] result = new byte[hashSize + output.length];
        System.arraycopy(checksum, 0, result, 0, hashSize);
        System.arraycopy(output, 0, result, hashSize, output.length);

        return result;
    }

    /**
     * Performs encryption using derived key; does not add confounder.
     */
    public byte[] encryptRaw(byte[] baseKey, int usage,
        byte[] seqNum, byte[] plaintext, int start, int len)
        throws GeneralSecurityException, KrbCryptoException {

        if (!KeyUsage.isValid(usage)) {
            throw new GeneralSecurityException("Invalid key usage number: "
                                                + usage);
        }

        if (debug) {
            System.out.println("\nARCFOUR: encryptRaw with usage = " + usage);
        }

        // Derive encryption key for data
        //   Key derivation salt = 0
        byte[] klocal = new byte[baseKey.length];
        for (int i = 0; i <= 15; i++) {
            klocal[i] = (byte) (baseKey[i] ^ 0xF0);
        }
        byte[] salt = new byte[4];
        byte[] kcrypt = getHmac(klocal, salt);

        // Note: When using this RC4 based encryption type, the sequence number
        // is always sent in big-endian rather than little-endian order.

        // new encryption key salted with sequence number
        kcrypt = getHmac(kcrypt, seqNum);

        Cipher cipher = Cipher.getInstance("ARCFOUR");
        SecretKeySpec secretKey = new SecretKeySpec(kcrypt, "ARCFOUR");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] output = cipher.doFinal(plaintext, start, len);

        return output;
    }

    /**
     * @param baseKey key from which keys are to be derived using usage
     * @param ciphertext  E(Ke, conf | plaintext | padding, ivec) | H1[1..h]
     */
    public byte[] decrypt(byte[] baseKey, int usage, byte[] ivec,
        byte[] ciphertext, int start, int len)
        throws GeneralSecurityException {

        if (!KeyUsage.isValid(usage)) {
            throw new GeneralSecurityException("Invalid key usage number: "
                                                + usage);
        }
        if (debug) {
            System.out.println("\nARCFOUR: DECRYPT using key usage = " + usage);
        }

        // compute K1
        byte[] k1 = new byte[baseKey.length];
        System.arraycopy(baseKey, 0, k1, 0, baseKey.length);

        // get the salt using key usage
        byte[] salt = getSalt(usage);

        // compute K2 using K1
        byte[] k2 = getHmac(k1, salt);

        // compute K3 using K2 and checksum
        byte[] checksum = new byte[hashSize];
        System.arraycopy(ciphertext, start, checksum, 0, hashSize);
        byte[] k3 = getHmac(k2, checksum);

        // Decrypt [confounder | plaintext ] (without checksum)
        Cipher cipher = Cipher.getInstance("ARCFOUR");
        SecretKeySpec secretKey = new SecretKeySpec(k3, "ARCFOUR");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] plaintext = cipher.doFinal(ciphertext, start+hashSize,
                                                len-hashSize);

        // Verify checksum
        byte[] calculatedHmac = getHmac(k2, plaintext);
        if (debug) {
            traceOutput("calculated Hmac", calculatedHmac, 0,
                                calculatedHmac.length);
            traceOutput("message Hmac", ciphertext, 0,
                                hashSize);
        }
        boolean cksumFailed = false;
        if (calculatedHmac.length >= hashSize) {
            for (int i = 0; i < hashSize; i++) {
                if (calculatedHmac[i] != ciphertext[i]) {
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

        // Get rid of confounder
        // [ confounder | plaintext ]
        byte[] output = new byte[plaintext.length - confounderSize];
        System.arraycopy(plaintext, confounderSize, output, 0, output.length);

        return output;
    }

    /**
     * Decrypts data using specified key and initial vector.
     * @param baseKey encryption key to use
     * @param ciphertext  encrypted data to be decrypted
     * @param usage ignored
     */
    public byte[] decryptRaw(byte[] baseKey, int usage, byte[] ivec,
        byte[] ciphertext, int start, int len, byte[] seqNum)
        throws GeneralSecurityException {

        if (!KeyUsage.isValid(usage)) {
            throw new GeneralSecurityException("Invalid key usage number: "
                                                + usage);
        }
        if (debug) {
            System.out.println("\nARCFOUR: decryptRaw with usage = " + usage);
        }

        // Derive encryption key for data
        //   Key derivation salt = 0
        byte[] klocal = new byte[baseKey.length];
        for (int i = 0; i <= 15; i++) {
            klocal[i] = (byte) (baseKey[i] ^ 0xF0);
        }
        byte[] salt = new byte[4];
        byte[] kcrypt = getHmac(klocal, salt);

        // need only first 4 bytes of sequence number
        byte[] sequenceNum = new byte[4];
        System.arraycopy(seqNum, 0, sequenceNum, 0, sequenceNum.length);

        // new encryption key salted with sequence number
        kcrypt = getHmac(kcrypt, sequenceNum);

        Cipher cipher = Cipher.getInstance("ARCFOUR");
        SecretKeySpec secretKey = new SecretKeySpec(kcrypt, "ARCFOUR");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] output = cipher.doFinal(ciphertext, start, len);

        return output;
    }

    // get the salt using key usage
    private byte[] getSalt(int usage) {
        int ms_usage = arcfour_translate_usage(usage);
        byte[] salt = new byte[4];
        salt[0] = (byte)(ms_usage & 0xff);
        salt[1] = (byte)((ms_usage >> 8) & 0xff);
        salt[2] = (byte)((ms_usage >> 16) & 0xff);
        salt[3] = (byte)((ms_usage >> 24) & 0xff);
        return salt;
    }

    // Key usage translation for MS
    private int arcfour_translate_usage(int usage) {
        switch (usage) {
            case 3: return 8;
            case 9: return 8;
            case 23: return 13;
            default: return usage;
        }
    }

}
