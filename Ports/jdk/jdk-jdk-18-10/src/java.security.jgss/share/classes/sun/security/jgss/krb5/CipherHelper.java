/*
 * Copyright (c) 2004, 2017, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.jgss.krb5;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import org.ietf.jgss.*;

import java.security.MessageDigest;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import sun.security.krb5.*;
import sun.security.krb5.internal.crypto.Aes128Sha2;
import sun.security.krb5.internal.crypto.Aes256Sha2;
import sun.security.krb5.internal.crypto.Des3;
import sun.security.krb5.internal.crypto.Aes128;
import sun.security.krb5.internal.crypto.Aes256;
import sun.security.krb5.internal.crypto.ArcFourHmac;
import sun.security.krb5.internal.crypto.EType;

class CipherHelper {

    // From draft-raeburn-cat-gssapi-krb5-3des-00
    // Key usage values when deriving keys
    private static final int KG_USAGE_SEAL = 22;
    private static final int KG_USAGE_SIGN = 23;
    private static final int KG_USAGE_SEQ = 24;

    private static final int DES_CHECKSUM_SIZE = 8;
    private static final int DES_IV_SIZE = 8;
    private static final int AES_IV_SIZE = 16;

    // ARCFOUR-HMAC
    // Save first 8 octets of HMAC Sgn_Cksum
    private static final int HMAC_CHECKSUM_SIZE = 8;
    // key usage for MIC tokens used by MS
    private static final int KG_USAGE_SIGN_MS = 15;

    // debug flag
    private static final boolean DEBUG = Krb5Util.DEBUG;

    /**
     * A zero initial vector to be used for checksum calculation and for
     * DesCbc application data encryption/decryption.
     */
    private static final byte[] ZERO_IV = new byte[DES_IV_SIZE];
    private static final byte[] ZERO_IV_AES = new byte[AES_IV_SIZE];

    private int etype;
    private int sgnAlg, sealAlg;
    private byte[] keybytes;

    CipherHelper(EncryptionKey key) throws GSSException {
        etype = key.getEType();
        keybytes = key.getBytes();

        switch (etype) {
        case EncryptedData.ETYPE_DES_CBC_CRC:
        case EncryptedData.ETYPE_DES_CBC_MD5:
            sgnAlg = MessageToken.SGN_ALG_DES_MAC_MD5;
            sealAlg = MessageToken.SEAL_ALG_DES;
            break;

        case EncryptedData.ETYPE_DES3_CBC_HMAC_SHA1_KD:
            sgnAlg = MessageToken.SGN_ALG_HMAC_SHA1_DES3_KD;
            sealAlg = MessageToken.SEAL_ALG_DES3_KD;
            break;

        case EncryptedData.ETYPE_ARCFOUR_HMAC:
            sgnAlg = MessageToken.SGN_ALG_HMAC_MD5_ARCFOUR;
            sealAlg = MessageToken.SEAL_ALG_ARCFOUR_HMAC;
            break;

        case EncryptedData.ETYPE_AES128_CTS_HMAC_SHA1_96:
        case EncryptedData.ETYPE_AES256_CTS_HMAC_SHA1_96:
        case EncryptedData.ETYPE_AES128_CTS_HMAC_SHA256_128:
        case EncryptedData.ETYPE_AES256_CTS_HMAC_SHA384_192:
            sgnAlg = -1;
            sealAlg = -1;
            break;

        default:
            throw new GSSException(GSSException.FAILURE, -1,
                "Unsupported encryption type: " + etype);
        }
    }

    int getSgnAlg() {
        return sgnAlg;
    }

    int getSealAlg() {
        return sealAlg;
    }

    // new token format from draft-ietf-krb-wg-gssapi-cfx-07
    // proto is used to determine new GSS token format for "newer" etypes
    int getProto() {
        return EType.isNewer(etype) ? 1 : 0;
    }

    int getEType() {
        return etype;
    }

    boolean isArcFour() {
        boolean flag = false;
        if (etype == EncryptedData.ETYPE_ARCFOUR_HMAC) {
            flag = true;
        }
        return flag;
    }

    @SuppressWarnings("fallthrough")
    byte[] calculateChecksum(int alg, byte[] header, byte[] trailer,
        byte[] data, int start, int len, int tokenId) throws GSSException {

        switch (alg) {
        case MessageToken.SGN_ALG_DES_MAC_MD5:
            /*
             * With this sign algorithm, first an MD5 hash is computed on the
             * application data. The 16 byte hash is then DesCbc encrypted.
             */
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");

                // debug("\t\tdata=[");

                // debug(getHexBytes(checksumDataHeader,
                //                      checksumDataHeader.length) + " ");
                md5.update(header);

                // debug(getHexBytes(data, start, len));
                md5.update(data, start, len);

                if (trailer != null) {
                    // debug(" " +
                    //       getHexBytes(trailer,
                    //                     optionalTrailer.length));
                    md5.update(trailer);
                }
                //          debug("]\n");

                data = md5.digest();
                start = 0;
                len = data.length;
                //          System.out.println("\tMD5 Checksum is [" +
                //                             getHexBytes(data) + "]\n");
                header = null;
                trailer = null;
            } catch (NoSuchAlgorithmException e) {
                GSSException ge = new GSSException(GSSException.FAILURE, -1,
                    "Could not get MD5 Message Digest - " + e.getMessage());
                ge.initCause(e);
                throw ge;
            }
            // fall through to encrypt checksum

        case MessageToken.SGN_ALG_DES_MAC:
            return getDesCbcChecksum(keybytes, header, data, start, len);

        case MessageToken.SGN_ALG_HMAC_SHA1_DES3_KD:
            byte[] buf;
            int offset, total;
            if (header == null && trailer == null) {
                buf = data;
                total = len;
                offset = start;
            } else {
                total = ((header != null ? header.length : 0) + len +
                    (trailer != null ? trailer.length : 0));

                buf = new byte[total];
                int pos = 0;
                if (header != null) {
                    System.arraycopy(header, 0, buf, 0, header.length);
                    pos = header.length;
                }
                System.arraycopy(data, start, buf, pos, len);
                pos += len;
                if (trailer != null) {
                    System.arraycopy(trailer, 0, buf, pos, trailer.length);
                }

                offset = 0;
            }

            try {

                /*
                Krb5Token.debug("\nkeybytes: " +
                    Krb5Token.getHexBytes(keybytes));
                Krb5Token.debug("\nheader: " + (header == null ? "NONE" :
                    Krb5Token.getHexBytes(header)));
                Krb5Token.debug("\ntrailer: " + (trailer == null ? "NONE" :
                    Krb5Token.getHexBytes(trailer)));
                Krb5Token.debug("\ndata: " +
                    Krb5Token.getHexBytes(data, start, len));
                Krb5Token.debug("\nbuf: " + Krb5Token.getHexBytes(buf, offset,
                    total));
                */

                byte[] answer = Des3.calculateChecksum(keybytes,
                    KG_USAGE_SIGN, buf, offset, total);
                // Krb5Token.debug("\nanswer: " +
                //              Krb5Token.getHexBytes(answer));
                return answer;
            } catch (GeneralSecurityException e) {
                GSSException ge = new GSSException(GSSException.FAILURE, -1,
                    "Could not use HMAC-SHA1-DES3-KD signing algorithm - " +
                    e.getMessage());
                ge.initCause(e);
                throw ge;
            }

        case MessageToken.SGN_ALG_HMAC_MD5_ARCFOUR:
            byte[] buffer;
            int off, tot;
            if (header == null && trailer == null) {
                buffer = data;
                tot = len;
                off = start;
            } else {
                tot = ((header != null ? header.length : 0) + len +
                      (trailer != null ? trailer.length : 0));

                buffer = new byte[tot];
                int pos = 0;

                if (header != null) {
                    System.arraycopy(header, 0, buffer, 0, header.length);
                    pos = header.length;
                }
                System.arraycopy(data, start, buffer, pos, len);
                pos += len;
                if (trailer != null) {
                    System.arraycopy(trailer, 0, buffer, pos, trailer.length);
                }

                off = 0;
            }

            try {

                /*
                Krb5Token.debug("\nkeybytes: " +
                    Krb5Token.getHexBytes(keybytes));
                Krb5Token.debug("\nheader: " + (header == null ? "NONE" :
                    Krb5Token.getHexBytes(header)));
                Krb5Token.debug("\ntrailer: " + (trailer == null ? "NONE" :
                    Krb5Token.getHexBytes(trailer)));
                Krb5Token.debug("\ndata: " +
                    Krb5Token.getHexBytes(data, start, len));
                Krb5Token.debug("\nbuffer: " +
                    Krb5Token.getHexBytes(buffer, off, tot));
                */

                // for MIC tokens, key derivation salt is 15
                // NOTE: Required for interoperability. The RC4-HMAC spec
                // defines key_usage of 23, however all Kerberos impl.
                // MS/Solaris/MIT all use key_usage of 15 for MIC tokens
                int key_usage = KG_USAGE_SIGN;
                if (tokenId == Krb5Token.MIC_ID) {
                        key_usage = KG_USAGE_SIGN_MS;
                }
                byte[] answer = ArcFourHmac.calculateChecksum(keybytes,
                    key_usage, buffer, off, tot);
                // Krb5Token.debug("\nanswer: " +
                //      Krb5Token.getHexBytes(answer));

                // Save first 8 octets of HMAC Sgn_Cksum
                byte[] output = new byte[getChecksumLength()];
                System.arraycopy(answer, 0, output, 0, output.length);
                // Krb5Token.debug("\nanswer (trimmed): " +
                //              Krb5Token.getHexBytes(output));
                return output;
            } catch (GeneralSecurityException e) {
                GSSException ge = new GSSException(GSSException.FAILURE, -1,
                    "Could not use HMAC_MD5_ARCFOUR signing algorithm - " +
                    e.getMessage());
                ge.initCause(e);
                throw ge;
            }

        default:
            throw new GSSException(GSSException.FAILURE, -1,
                "Unsupported signing algorithm: " + sgnAlg);
        }
    }

    // calculate Checksum for the new GSS tokens
    byte[] calculateChecksum(byte[] header, byte[] data, int start, int len,
        int key_usage) throws GSSException {

        // total length
        int total = ((header != null ? header.length : 0) + len);

        // get_mic("plaintext-data" | "header")
        byte[] buf = new byte[total];

        // data
        System.arraycopy(data, start, buf, 0, len);

        // token header
        if (header != null) {
            System.arraycopy(header, 0, buf, len, header.length);
        }

        // Krb5Token.debug("\nAES calculate checksum on: " +
        //              Krb5Token.getHexBytes(buf));
        switch (etype) {
        case EncryptedData.ETYPE_AES128_CTS_HMAC_SHA1_96:
            try {
                byte[] answer = Aes128.calculateChecksum(keybytes, key_usage,
                                        buf, 0, total);
                // Krb5Token.debug("\nAES128 checksum: " +
                //                      Krb5Token.getHexBytes(answer));
                return answer;
            } catch (GeneralSecurityException e) {
                GSSException ge = new GSSException(GSSException.FAILURE, -1,
                    "Could not use AES128 signing algorithm - " +
                    e.getMessage());
                ge.initCause(e);
                throw ge;
            }

        case EncryptedData.ETYPE_AES256_CTS_HMAC_SHA1_96:
            try {
                byte[] answer = Aes256.calculateChecksum(keybytes, key_usage,
                                        buf, 0, total);
                // Krb5Token.debug("\nAES256 checksum: " +
                //              Krb5Token.getHexBytes(answer));
                return answer;
            } catch (GeneralSecurityException e) {
                GSSException ge = new GSSException(GSSException.FAILURE, -1,
                    "Could not use AES256 signing algorithm - " +
                    e.getMessage());
                ge.initCause(e);
                throw ge;
            }

        case EncryptedData.ETYPE_AES128_CTS_HMAC_SHA256_128:
            try {
                byte[] answer = Aes128Sha2.calculateChecksum(keybytes, key_usage,
                        buf, 0, total);
                return answer;
            } catch (GeneralSecurityException e) {
                GSSException ge = new GSSException(GSSException.FAILURE, -1,
                        "Could not use AES128 signing algorithm - " +
                                e.getMessage());
                ge.initCause(e);
                throw ge;
            }

        case EncryptedData.ETYPE_AES256_CTS_HMAC_SHA384_192:
            try {
                byte[] answer = Aes256Sha2.calculateChecksum(keybytes, key_usage,
                        buf, 0, total);
                return answer;
            } catch (GeneralSecurityException e) {
                GSSException ge = new GSSException(GSSException.FAILURE, -1,
                        "Could not use AES256 signing algorithm - " +
                                e.getMessage());
                ge.initCause(e);
                throw ge;
            }


        default:
            throw new GSSException(GSSException.FAILURE, -1,
                "Unsupported encryption type: " + etype);
        }
    }

    byte[] encryptSeq(byte[] ivec, byte[] plaintext, int start, int len)
    throws GSSException {

        switch (sgnAlg) {
        case MessageToken.SGN_ALG_DES_MAC_MD5:
        case MessageToken.SGN_ALG_DES_MAC:
            try {
                Cipher des = getInitializedDes(true, keybytes, ivec);
                return des.doFinal(plaintext, start, len);

            } catch (GeneralSecurityException e) {
                GSSException ge = new GSSException(GSSException.FAILURE, -1,
                    "Could not encrypt sequence number using DES - " +
                    e.getMessage());
                ge.initCause(e);
                throw ge;
            }

        case MessageToken.SGN_ALG_HMAC_SHA1_DES3_KD:
            byte[] iv;
            if (ivec.length == DES_IV_SIZE) {
                iv = ivec;
            } else {
                iv = new byte[DES_IV_SIZE];
                System.arraycopy(ivec, 0, iv, 0, DES_IV_SIZE);
            }
            try {
                return Des3.encryptRaw(keybytes, KG_USAGE_SEQ, iv,
                    plaintext, start, len);
            } catch (Exception e) {
                // GeneralSecurityException, KrbCryptoException
                GSSException ge = new GSSException(GSSException.FAILURE, -1,
                    "Could not encrypt sequence number using DES3-KD - " +
                    e.getMessage());
                ge.initCause(e);
                throw ge;
            }

        case MessageToken.SGN_ALG_HMAC_MD5_ARCFOUR:
            // ivec passed is the checksum
            byte[] checksum;
            if (ivec.length == HMAC_CHECKSUM_SIZE) {
                checksum = ivec;
            } else {
                checksum = new byte[HMAC_CHECKSUM_SIZE];
                System.arraycopy(ivec, 0, checksum, 0, HMAC_CHECKSUM_SIZE);
            }

            try {
                return ArcFourHmac.encryptSeq(keybytes, KG_USAGE_SEQ, checksum,
                    plaintext, start, len);
            } catch (Exception e) {
                // GeneralSecurityException, KrbCryptoException
                GSSException ge = new GSSException(GSSException.FAILURE, -1,
                    "Could not encrypt sequence number using RC4-HMAC - " +
                    e.getMessage());
                ge.initCause(e);
                throw ge;
            }

        default:
            throw new GSSException(GSSException.FAILURE, -1,
                "Unsupported signing algorithm: " + sgnAlg);
        }
    }

    byte[] decryptSeq(byte[] ivec, byte[] ciphertext, int start, int len)
        throws GSSException {

        switch (sgnAlg) {
        case MessageToken.SGN_ALG_DES_MAC_MD5:
        case MessageToken.SGN_ALG_DES_MAC:
            try {
                Cipher des = getInitializedDes(false, keybytes, ivec);
                return des.doFinal(ciphertext, start, len);
            } catch (GeneralSecurityException e) {
                GSSException ge = new GSSException(GSSException.FAILURE, -1,
                    "Could not decrypt sequence number using DES - " +
                    e.getMessage());
                ge.initCause(e);
                throw ge;
            }

        case MessageToken.SGN_ALG_HMAC_SHA1_DES3_KD:
            byte[] iv;
            if (ivec.length == DES_IV_SIZE) {
                iv = ivec;
            } else {
                iv = new byte[8];
                System.arraycopy(ivec, 0, iv, 0, DES_IV_SIZE);
            }

            try {
                return Des3.decryptRaw(keybytes, KG_USAGE_SEQ, iv,
                    ciphertext, start, len);
            } catch (Exception e) {
                // GeneralSecurityException, KrbCryptoException
                GSSException ge = new GSSException(GSSException.FAILURE, -1,
                    "Could not decrypt sequence number using DES3-KD - " +
                    e.getMessage());
                ge.initCause(e);
                throw ge;
            }

        case MessageToken.SGN_ALG_HMAC_MD5_ARCFOUR:
            // ivec passed is the checksum
            byte[] checksum;
            if (ivec.length == HMAC_CHECKSUM_SIZE) {
                checksum = ivec;
            } else {
                checksum = new byte[HMAC_CHECKSUM_SIZE];
                System.arraycopy(ivec, 0, checksum, 0, HMAC_CHECKSUM_SIZE);
            }

            try {
                return ArcFourHmac.decryptSeq(keybytes, KG_USAGE_SEQ, checksum,
                    ciphertext, start, len);
            } catch (Exception e) {
                // GeneralSecurityException, KrbCryptoException
                GSSException ge = new GSSException(GSSException.FAILURE, -1,
                    "Could not decrypt sequence number using RC4-HMAC - " +
                    e.getMessage());
                ge.initCause(e);
                throw ge;
            }

        default:
            throw new GSSException(GSSException.FAILURE, -1,
                "Unsupported signing algorithm: " + sgnAlg);
        }
    }

    int getChecksumLength() throws GSSException {
        switch (etype) {
        case EncryptedData.ETYPE_DES_CBC_CRC:
        case EncryptedData.ETYPE_DES_CBC_MD5:
            return DES_CHECKSUM_SIZE;

        case EncryptedData.ETYPE_DES3_CBC_HMAC_SHA1_KD:
            return Des3.getChecksumLength();

        case EncryptedData.ETYPE_AES128_CTS_HMAC_SHA1_96:
            return Aes128.getChecksumLength();
        case EncryptedData.ETYPE_AES256_CTS_HMAC_SHA1_96:
            return Aes256.getChecksumLength();

        case EncryptedData.ETYPE_AES128_CTS_HMAC_SHA256_128:
            return Aes128Sha2.getChecksumLength();
        case EncryptedData.ETYPE_AES256_CTS_HMAC_SHA384_192:
            return Aes256Sha2.getChecksumLength();

        case EncryptedData.ETYPE_ARCFOUR_HMAC:
            // only first 8 octets of HMAC Sgn_Cksum are used
            return HMAC_CHECKSUM_SIZE;

        default:
            throw new GSSException(GSSException.FAILURE, -1,
                "Unsupported encryption type: " + etype);
        }
    }

    void decryptData(WrapToken token, byte[] ciphertext, int cStart, int cLen,
        byte[] plaintext, int pStart) throws GSSException {

        /*
        Krb5Token.debug("decryptData : ciphertext =  " +
                Krb5Token.getHexBytes(ciphertext));
        */

        switch (sealAlg) {
        case MessageToken.SEAL_ALG_DES:
            desCbcDecrypt(token, getDesEncryptionKey(keybytes),
                ciphertext, cStart, cLen, plaintext, pStart);
            break;

        case MessageToken.SEAL_ALG_DES3_KD:
            des3KdDecrypt(token, ciphertext, cStart, cLen, plaintext, pStart);
            break;

        case MessageToken.SEAL_ALG_ARCFOUR_HMAC:
            arcFourDecrypt(token, ciphertext, cStart, cLen, plaintext, pStart);
            break;

        default:
            throw new GSSException(GSSException.FAILURE, -1,
                "Unsupported seal algorithm: " + sealAlg);
        }
    }

    // decrypt data in the new GSS tokens
    void decryptData(WrapToken_v2 token, byte[] ciphertext, int cStart,
                int cLen, byte[] plaintext, int pStart, int key_usage)
        throws GSSException {

        /*
        Krb5Token.debug("decryptData : ciphertext =  " +
                Krb5Token.getHexBytes(ciphertext));
        */

        switch (etype) {
            case EncryptedData.ETYPE_AES128_CTS_HMAC_SHA1_96:
                    aes128Decrypt(token, ciphertext, cStart, cLen,
                                plaintext, pStart, key_usage);
                    break;
            case EncryptedData.ETYPE_AES256_CTS_HMAC_SHA1_96:
                    aes256Decrypt(token, ciphertext, cStart, cLen,
                                plaintext, pStart, key_usage);
                    break;
            case EncryptedData.ETYPE_AES128_CTS_HMAC_SHA256_128:
                    aes128Sha2Decrypt(token, ciphertext, cStart, cLen,
                            plaintext, pStart, key_usage);
                    break;
            case EncryptedData.ETYPE_AES256_CTS_HMAC_SHA384_192:
                    aes256Sha2Decrypt(token, ciphertext, cStart, cLen,
                            plaintext, pStart, key_usage);
                    break;
            default:
                    throw new GSSException(GSSException.FAILURE, -1,
                        "Unsupported etype: " + etype);
            }
    }

    void decryptData(WrapToken token, InputStream cipherStream, int cLen,
        byte[] plaintext, int pStart)
        throws GSSException, IOException {

        switch (sealAlg) {
        case MessageToken.SEAL_ALG_DES:
            desCbcDecrypt(token, getDesEncryptionKey(keybytes),
                cipherStream, cLen, plaintext, pStart);
            break;

        case MessageToken.SEAL_ALG_DES3_KD:

            // Read encrypted data from stream
            byte[] ciphertext = new byte[cLen];
            try {
                Krb5Token.readFully(cipherStream, ciphertext, 0, cLen);
            } catch (IOException e) {
                GSSException ge = new GSSException(
                    GSSException.DEFECTIVE_TOKEN, -1,
                    "Cannot read complete token");
                ge.initCause(e);
                throw ge;
            }

            des3KdDecrypt(token, ciphertext, 0, cLen, plaintext, pStart);
            break;

        case MessageToken.SEAL_ALG_ARCFOUR_HMAC:

            // Read encrypted data from stream
            byte[] ctext = new byte[cLen];
            try {
                Krb5Token.readFully(cipherStream, ctext, 0, cLen);
            } catch (IOException e) {
                GSSException ge = new GSSException(
                    GSSException.DEFECTIVE_TOKEN, -1,
                    "Cannot read complete token");
                ge.initCause(e);
                throw ge;
            }

            arcFourDecrypt(token, ctext, 0, cLen, plaintext, pStart);
            break;

        default:
            throw new GSSException(GSSException.FAILURE, -1,
                "Unsupported seal algorithm: " + sealAlg);
        }
    }

    void decryptData(WrapToken_v2 token, InputStream cipherStream, int cLen,
        byte[] plaintext, int pStart, int key_usage)
        throws GSSException, IOException {

        // Read encrypted data from stream
        byte[] ciphertext = new byte[cLen];
        try {
                Krb5Token.readFully(cipherStream, ciphertext, 0, cLen);
        } catch (IOException e) {
                GSSException ge = new GSSException(
                    GSSException.DEFECTIVE_TOKEN, -1,
                    "Cannot read complete token");
                ge.initCause(e);
                throw ge;
        }
        switch (etype) {
            case EncryptedData.ETYPE_AES128_CTS_HMAC_SHA1_96:
                    aes128Decrypt(token, ciphertext, 0, cLen,
                                plaintext, pStart, key_usage);
                    break;
            case EncryptedData.ETYPE_AES256_CTS_HMAC_SHA1_96:
                    aes256Decrypt(token, ciphertext, 0, cLen,
                                plaintext, pStart, key_usage);
                    break;
            case EncryptedData.ETYPE_AES128_CTS_HMAC_SHA256_128:
                    aes128Sha2Decrypt(token, ciphertext, 0, cLen,
                            plaintext, pStart, key_usage);
                    break;
            case EncryptedData.ETYPE_AES256_CTS_HMAC_SHA384_192:
                    aes256Sha2Decrypt(token, ciphertext, 0, cLen,
                            plaintext, pStart, key_usage);
                    break;
            default:
                    throw new GSSException(GSSException.FAILURE, -1,
                        "Unsupported etype: " + etype);
        }
    }

    void encryptData(WrapToken token, byte[] confounder, byte[] plaintext,
        int start, int len, byte[] padding, OutputStream os)
        throws GSSException, IOException {

        switch (sealAlg) {
        case MessageToken.SEAL_ALG_DES:
            // Encrypt on the fly and write
            Cipher des = getInitializedDes(true, getDesEncryptionKey(keybytes),
                ZERO_IV);
            CipherOutputStream cos = new CipherOutputStream(os, des);
            // debug(getHexBytes(confounder, confounder.length));
            cos.write(confounder);
            // debug(" " + getHexBytes(plaintext, start, len));
            cos.write(plaintext, start, len);
            // debug(" " + getHexBytes(padding, padding.length));
            cos.write(padding);
            break;

        case MessageToken.SEAL_ALG_DES3_KD:
            byte[] ctext = des3KdEncrypt(confounder, plaintext, start, len,
                padding);

            // Write to stream
            os.write(ctext);
            break;

        case MessageToken.SEAL_ALG_ARCFOUR_HMAC:
            byte[] ciphertext = arcFourEncrypt(token, confounder, plaintext,
                start, len, padding);

            // Write to stream
            os.write(ciphertext);
            break;

        default:
            throw new GSSException(GSSException.FAILURE, -1,
                "Unsupported seal algorithm: " + sealAlg);
        }
    }

    /*
     * Encrypt data in the new GSS tokens
     *
     * Wrap Tokens (with confidentiality)
     * { Encrypt(16-byte confounder | plaintext | 16-byte token_header) |
     *           12-byte HMAC }
     * where HMAC is on {16-byte confounder | plaintext | 16-byte token_header}
     * HMAC is not encrypted; it is appended at the end.
     */
    byte[] encryptData(WrapToken_v2 token, byte[] confounder, byte[] tokenHeader,
            byte[] plaintext, int start, int len, int key_usage)
            throws GSSException {

        switch (etype) {
            case EncryptedData.ETYPE_AES128_CTS_HMAC_SHA1_96:
                return aes128Encrypt(confounder, tokenHeader,
                            plaintext, start, len, key_usage);
            case EncryptedData.ETYPE_AES256_CTS_HMAC_SHA1_96:
                return aes256Encrypt(confounder, tokenHeader,
                            plaintext, start, len, key_usage);
            case EncryptedData.ETYPE_AES128_CTS_HMAC_SHA256_128:
                return aes128Sha2Encrypt(confounder, tokenHeader,
                        plaintext, start, len, key_usage);
            case EncryptedData.ETYPE_AES256_CTS_HMAC_SHA384_192:
                return aes256Sha2Encrypt(confounder, tokenHeader,
                        plaintext, start, len, key_usage);
            default:
                throw new GSSException(GSSException.FAILURE, -1,
                    "Unsupported etype: " + etype);
        }
    }

    void encryptData(WrapToken token, byte[] confounder, byte[] plaintext,
        int pStart, int pLen, byte[] padding, byte[] ciphertext, int cStart)
        throws GSSException {

        switch (sealAlg) {
        case MessageToken.SEAL_ALG_DES:
            int pos = cStart;
            // Encrypt and write
            Cipher des = getInitializedDes(true, getDesEncryptionKey(keybytes),
                ZERO_IV);
            try {
                // debug(getHexBytes(confounder, confounder.length));
                pos += des.update(confounder, 0, confounder.length,
                                  ciphertext, pos);
                // debug(" " + getHexBytes(dataBytes, dataOffset, dataLen));
                pos += des.update(plaintext, pStart, pLen,
                                  ciphertext, pos);
                // debug(" " + getHexBytes(padding, padding.length));
                des.update(padding, 0, padding.length,
                           ciphertext, pos);
                des.doFinal();
            } catch (GeneralSecurityException e) {
                GSSException ge = new GSSException(GSSException.FAILURE, -1,
                    "Could not use DES Cipher - " + e.getMessage());
                ge.initCause(e);
                throw ge;
            }
            break;

        case MessageToken.SEAL_ALG_DES3_KD:
            byte[] ctext = des3KdEncrypt(confounder, plaintext, pStart, pLen,
                padding);
            System.arraycopy(ctext, 0, ciphertext, cStart, ctext.length);
            break;

        case MessageToken.SEAL_ALG_ARCFOUR_HMAC:
            byte[] ctext2 = arcFourEncrypt(token, confounder, plaintext, pStart,
                pLen, padding);
            System.arraycopy(ctext2, 0, ciphertext, cStart, ctext2.length);
            break;

        default:
            throw new GSSException(GSSException.FAILURE, -1,
                "Unsupported seal algorithm: " + sealAlg);
        }
    }

    /*
     * Encrypt data in the new GSS tokens
     *
     * Wrap Tokens (with confidentiality)
     * { Encrypt(16-byte confounder | plaintext | 16-byte token_header) |
     *           12-byte HMAC }
     * where HMAC is on {16-byte confounder | plaintext | 16-byte token_header}
     * HMAC is not encrypted; it is appended at the end.
     */
    int encryptData(WrapToken_v2 token, byte[] confounder, byte[] tokenHeader,
        byte[] plaintext, int pStart, int pLen, byte[] ciphertext, int cStart,
        int key_usage) throws GSSException {

        byte[] ctext = null;
        switch (etype) {
            case EncryptedData.ETYPE_AES128_CTS_HMAC_SHA1_96:
                    ctext = aes128Encrypt(confounder, tokenHeader,
                                plaintext, pStart, pLen, key_usage);
                    break;
            case EncryptedData.ETYPE_AES256_CTS_HMAC_SHA1_96:
                    ctext = aes256Encrypt(confounder, tokenHeader,
                                plaintext, pStart, pLen, key_usage);
                    break;
            case EncryptedData.ETYPE_AES128_CTS_HMAC_SHA256_128:
                    ctext = aes128Sha2Encrypt(confounder, tokenHeader,
                            plaintext, pStart, pLen, key_usage);
                    break;
            case EncryptedData.ETYPE_AES256_CTS_HMAC_SHA384_192:
                    ctext = aes256Sha2Encrypt(confounder, tokenHeader,
                            plaintext, pStart, pLen, key_usage);
                    break;
            default:
                    throw new GSSException(GSSException.FAILURE, -1,
                        "Unsupported etype: " + etype);
        }
        System.arraycopy(ctext, 0, ciphertext, cStart, ctext.length);
        return ctext.length;
    }

    // --------------------- DES methods

    /**
     * Computes the DesCbc checksum based on the algorithm published in FIPS
     * Publication 113. This involves applying padding to the data passed
     * in, then performing DesCbc encryption on the data with a zero initial
     * vector, and finally returning the last 8 bytes of the encryption
     * result.
     *
     * @param key the bytes for the DES key
     * @param header a header to process first before the data is.
     * @param data the data to checksum
     * @param offset the offset where the data begins
     * @param len the length of the data
     * @throws GSSException when an error occuse in the encryption
     */
    private byte[] getDesCbcChecksum(byte key[],
                                     byte[] header,
                                     byte[] data, int offset, int len)
        throws GSSException {

        Cipher des = getInitializedDes(true, key, ZERO_IV);

        int blockSize = des.getBlockSize();

        /*
         * Here the data need not be a multiple of the blocksize
         * (8). Encrypt and throw away results for all blocks except for
         * the very last block.
         */

        byte[] finalBlock = new byte[blockSize];

        int numBlocks = len / blockSize;
        int lastBytes = len % blockSize;
        if (lastBytes == 0) {
            // No need for padding. Save last block from application data
            numBlocks -= 1;
            System.arraycopy(data, offset + numBlocks*blockSize,
                             finalBlock, 0, blockSize);
        } else {
            System.arraycopy(data, offset + numBlocks*blockSize,
                             finalBlock, 0, lastBytes);
            // Zero padding automatically done
        }

        try {
            byte[] temp = new byte[Math.max(blockSize,
                (header == null? blockSize : header.length))];

            if (header != null) {
                // header will be null when doing DES-MD5 Checksum
                des.update(header, 0, header.length, temp, 0);
            }

            // Iterate over all but the last block
            for (int i = 0; i < numBlocks; i++) {
                des.update(data, offset, blockSize,
                           temp, 0);
                offset += blockSize;
            }

            // Now process the final block
            byte[] retVal = new byte[blockSize];
            des.update(finalBlock, 0, blockSize, retVal, 0);
            des.doFinal();

            return retVal;
        } catch (GeneralSecurityException e) {
            GSSException ge = new GSSException(GSSException.FAILURE, -1,
                "Could not use DES Cipher - " + e.getMessage());
            ge.initCause(e);
            throw ge;
        }
    }

    /**
     * Obtains an initialized DES cipher.
     *
     * @param encryptMode true if encryption is desired, false is decryption
     * is desired.
     * @param key the bytes for the DES key
     * @param ivBytes the initial vector bytes
     */
    private final Cipher getInitializedDes(boolean encryptMode, byte[] key,
                                          byte[] ivBytes)
        throws  GSSException  {


        try {
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            SecretKey jceKey = (SecretKey) (new SecretKeySpec(key, "DES"));

            Cipher desCipher = Cipher.getInstance("DES/CBC/NoPadding");
            desCipher.init(
                (encryptMode ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE),
                jceKey, iv);
            return desCipher;
        } catch (GeneralSecurityException e) {
            GSSException ge = new GSSException(GSSException.FAILURE, -1,
                e.getMessage());
            ge.initCause(e);
            throw ge;
        }
    }

    /**
     * Helper routine to decrypt fromm a byte array and write the
     * application data straight to an output array with minimal
     * buffer copies. The confounder and the padding are stored
     * separately and not copied into this output array.
     * @param key the DES key to use
     * @param cipherText the encrypted data
     * @param offset the offset for the encrypted data
     * @param len the length of the encrypted data
     * @param dataOutBuf the output buffer where the application data
     * should be writte
     * @param dataOffset the offser where the application data should
     * be written.
     * @throws GSSException is an error occurs while decrypting the
     * data
     */
    private void desCbcDecrypt(WrapToken token, byte[] key, byte[] cipherText,
        int offset, int len, byte[] dataOutBuf, int dataOffset)
         throws GSSException {

        try {

            int temp = 0;

            Cipher des = getInitializedDes(false, key, ZERO_IV);

            /*
             * Remove the counfounder first.
             * CONFOUNDER_SIZE is one DES block ie 8 bytes.
             */
            temp = des.update(cipherText, offset, WrapToken.CONFOUNDER_SIZE,
                              token.confounder);
            // temp should be CONFOUNDER_SIZE
            // debug("\n\ttemp is " + temp + " and CONFOUNDER_SIZE is "
            //  + CONFOUNDER_SIZE);

            offset += WrapToken.CONFOUNDER_SIZE;
            len -= WrapToken.CONFOUNDER_SIZE;

            /*
             * len is a multiple of 8 due to padding.
             * Decrypt all blocks directly into the output buffer except for
             * the very last block. Remove the trailing padding bytes from the
             * very last block and copy that into the output buffer.
             */

            int blockSize = des.getBlockSize();
            int numBlocks = len / blockSize - 1;

            // Iterate over all but the last block
            for (int i = 0; i < numBlocks; i++) {
                temp = des.update(cipherText, offset, blockSize,
                                  dataOutBuf, dataOffset);
                // temp should be blockSize
                // debug("\n\ttemp is " + temp + " and blockSize is "
                //    + blockSize);

                offset += blockSize;
                dataOffset += blockSize;
            }

            // Now process the last block
            byte[] finalBlock = new byte[blockSize];
            des.update(cipherText, offset, blockSize, finalBlock);

            des.doFinal();

            /*
             * There is always at least one padding byte. The padding bytes
             * are all the value of the number of padding bytes.
             */

            int padSize = finalBlock[blockSize - 1];
            if (padSize < 1  || padSize > 8)
                throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1,
                                        "Invalid padding on Wrap Token");
            token.padding = WrapToken.pads[padSize];
            blockSize -= padSize;

            // Copy this last block into the output buffer
            System.arraycopy(finalBlock, 0, dataOutBuf, dataOffset,
                             blockSize);

        } catch (GeneralSecurityException e) {
            GSSException ge = new GSSException(GSSException.FAILURE, -1,
                "Could not use DES cipher - " + e.getMessage());
            ge.initCause(e);
            throw ge;
        }
    }

    /**
     * Helper routine to decrypt from an InputStream and write the
     * application data straight to an output array with minimal
     * buffer copies. The confounder and the padding are stored
     * separately and not copied into this output array.
     * @param key the DES key to use
     * @param is the InputStream from which the cipher text should be
     * read
     * @param len the length of the ciphertext data
     * @param dataOutBuf the output buffer where the application data
     * should be writte
     * @param dataOffset the offser where the application data should
     * be written.
     * @throws GSSException is an error occurs while decrypting the
     * data
     */
    private void desCbcDecrypt(WrapToken token, byte[] key,
        InputStream is, int len, byte[] dataOutBuf, int dataOffset)
        throws GSSException, IOException {

        int temp = 0;

        Cipher des = getInitializedDes(false, key, ZERO_IV);

        WrapTokenInputStream truncatedInputStream =
            new WrapTokenInputStream(is, len);
        CipherInputStream cis = new CipherInputStream(truncatedInputStream,
                                                      des);
        /*
         * Remove the counfounder first.
         * CONFOUNDER_SIZE is one DES block ie 8 bytes.
         */
        temp = cis.read(token.confounder);

        len -= temp;
        // temp should be CONFOUNDER_SIZE
        // debug("Got " + temp + " bytes; CONFOUNDER_SIZE is "
        //     + CONFOUNDER_SIZE + "\n");
        // debug("Confounder is " + getHexBytes(confounder) + "\n");


        /*
         * len is a multiple of 8 due to padding.
         * Decrypt all blocks directly into the output buffer except for
         * the very last block. Remove the trailing padding bytes from the
         * very last block and copy that into the output buffer.
         */

        int blockSize = des.getBlockSize();
        int numBlocks = len / blockSize - 1;

        // Iterate over all but the last block
        for (int i = 0; i < numBlocks; i++) {
            // debug("dataOffset is " + dataOffset + "\n");
            temp = cis.read(dataOutBuf, dataOffset, blockSize);

            // temp should be blockSize
            // debug("Got " + temp + " bytes and blockSize is "
            //    + blockSize + "\n");
            // debug("Bytes are: "
            //    + getHexBytes(dataOutBuf, dataOffset, temp) + "\n");
            dataOffset += blockSize;
        }

        // Now process the last block
        byte[] finalBlock = new byte[blockSize];
        // debug("Will call read on finalBlock" + "\n");
        temp = cis.read(finalBlock);
        // temp should be blockSize
        /*
          debug("Got " + temp + " bytes and blockSize is "
          + blockSize + "\n");
          debug("Bytes are: "
          + getHexBytes(finalBlock, 0, temp) + "\n");
          debug("Will call doFinal" + "\n");
        */
        try {
            des.doFinal();
        } catch (GeneralSecurityException e) {
            GSSException ge = new GSSException(GSSException.FAILURE, -1,
                "Could not use DES cipher - " + e.getMessage());
            ge.initCause(e);
            throw ge;
        }

        /*
         * There is always at least one padding byte. The padding bytes
         * are all the value of the number of padding bytes.
         */

        int padSize = finalBlock[blockSize - 1];
        if (padSize < 1  || padSize > 8)
            throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1,
                                   "Invalid padding on Wrap Token");
        token.padding = WrapToken.pads[padSize];
        blockSize -= padSize;

        // Copy this last block into the output buffer
        System.arraycopy(finalBlock, 0, dataOutBuf, dataOffset,
                         blockSize);
    }

    private static byte[] getDesEncryptionKey(byte[] key)
        throws GSSException {

        /*
         * To meet export control requirements, double check that the
         * key being used is no longer than 64 bits.
         *
         * Note that from a protocol point of view, an
         * algorithm that is not DES will be rejected before this
         * point. Also, a DES key that is not 64 bits will be
         * rejected by a good JCE provider.
         */
        if (key.length > 8)
            throw new GSSException(GSSException.FAILURE, -100,
                                   "Invalid DES Key!");

        byte[] retVal = new byte[key.length];
        for (int i = 0; i < key.length; i++)
            retVal[i] = (byte)(key[i] ^ 0xf0);  // RFC 1964, Section 1.2.2
        return retVal;
    }

    // ---- DES3-KD methods
    private void des3KdDecrypt(WrapToken token, byte[] ciphertext,
        int cStart, int cLen, byte[] plaintext, int pStart)
        throws GSSException {
        byte[] ptext;
        try {
            ptext = Des3.decryptRaw(keybytes, KG_USAGE_SEAL, ZERO_IV,
                ciphertext, cStart, cLen);
        } catch (GeneralSecurityException e) {
            GSSException ge = new GSSException(GSSException.FAILURE, -1,
                "Could not use DES3-KD Cipher - " + e.getMessage());
            ge.initCause(e);
            throw ge;
        }

        /*
        Krb5Token.debug("\ndes3KdDecrypt in: " +
            Krb5Token.getHexBytes(ciphertext, cStart, cLen));
        Krb5Token.debug("\ndes3KdDecrypt plain: " +
            Krb5Token.getHexBytes(ptext));
        */

        // Strip out confounder and padding
        /*
         * There is always at least one padding byte. The padding bytes
         * are all the value of the number of padding bytes.
         */
        int padSize = ptext[ptext.length - 1];
        if (padSize < 1  || padSize > 8)
            throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1,
                "Invalid padding on Wrap Token");

        token.padding = WrapToken.pads[padSize];
        int len = ptext.length - WrapToken.CONFOUNDER_SIZE - padSize;

        System.arraycopy(ptext, WrapToken.CONFOUNDER_SIZE,
            plaintext, pStart, len);

        // Needed to calculate checksum
        System.arraycopy(ptext, 0, token.confounder,
            0, WrapToken.CONFOUNDER_SIZE);
    }

    private byte[] des3KdEncrypt(byte[] confounder, byte[] plaintext,
        int start, int len, byte[] padding) throws GSSException {


        // [confounder | plaintext | padding]
        byte[] all = new byte[confounder.length + len + padding.length];
        System.arraycopy(confounder, 0, all, 0, confounder.length);
        System.arraycopy(plaintext, start, all, confounder.length, len);
        System.arraycopy(padding, 0, all, confounder.length + len,
            padding.length);

        // Krb5Token.debug("\ndes3KdEncrypt:" + Krb5Token.getHexBytes(all));

        // Encrypt
        try {
            byte[] answer = Des3.encryptRaw(keybytes, KG_USAGE_SEAL, ZERO_IV,
                all, 0, all.length);
            // Krb5Token.debug("\ndes3KdEncrypt encrypted:" +
            //  Krb5Token.getHexBytes(answer));
            return answer;
        } catch (Exception e) {
            // GeneralSecurityException, KrbCryptoException
            GSSException ge = new GSSException(GSSException.FAILURE, -1,
                "Could not use DES3-KD Cipher - " + e.getMessage());
            ge.initCause(e);
            throw ge;
        }
    }

    // ---- RC4-HMAC methods
    private void arcFourDecrypt(WrapToken token, byte[] ciphertext,
        int cStart, int cLen, byte[] plaintext, int pStart)
        throws GSSException {

        // obtain Sequence number needed for decryption
        // first decrypt the Sequence Number using checksum
        byte[] seqNum = decryptSeq(token.getChecksum(),
                token.getEncSeqNumber(), 0, 8);

        byte[] ptext;
        try {
            ptext = ArcFourHmac.decryptRaw(keybytes, KG_USAGE_SEAL, ZERO_IV,
                ciphertext, cStart, cLen, seqNum);
        } catch (GeneralSecurityException e) {
            GSSException ge = new GSSException(GSSException.FAILURE, -1,
                "Could not use ArcFour Cipher - " + e.getMessage());
            ge.initCause(e);
            throw ge;
        }

        /*
        Krb5Token.debug("\narcFourDecrypt in: " +
            Krb5Token.getHexBytes(ciphertext, cStart, cLen));
        Krb5Token.debug("\narcFourDecrypt plain: " +
            Krb5Token.getHexBytes(ptext));
        */

        // Strip out confounder and padding
        /*
         * There is always at least one padding byte. The padding bytes
         * are all the value of the number of padding bytes.
         */
        int padSize = ptext[ptext.length - 1];
        if (padSize < 1)
            throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1,
                "Invalid padding on Wrap Token");

        token.padding = WrapToken.pads[padSize];
        int len = ptext.length - WrapToken.CONFOUNDER_SIZE - padSize;

        System.arraycopy(ptext, WrapToken.CONFOUNDER_SIZE,
            plaintext, pStart, len);

        // Krb5Token.debug("\narcFourDecrypt plaintext: " +
        //    Krb5Token.getHexBytes(plaintext));

        // Needed to calculate checksum
        System.arraycopy(ptext, 0, token.confounder,
            0, WrapToken.CONFOUNDER_SIZE);
    }

    private byte[] arcFourEncrypt(WrapToken token, byte[] confounder,
        byte[] plaintext, int start, int len, byte[] padding)
        throws GSSException {

        // [confounder | plaintext | padding]
        byte[] all = new byte[confounder.length + len + padding.length];
        System.arraycopy(confounder, 0, all, 0, confounder.length);
        System.arraycopy(plaintext, start, all, confounder.length, len);
        System.arraycopy(padding, 0, all, confounder.length + len,
            padding.length);

        // get the token Sequence Number required for encryption
        // Note: When using this RC4 based encryption type, the sequence number
        // is always sent in big-endian rather than little-endian order.
        byte[] seqNum = new byte[4];
        WrapToken.writeBigEndian(token.getSequenceNumber(), seqNum);

        // Krb5Token.debug("\narcFourEncrypt:" + Krb5Token.getHexBytes(all));

        // Encrypt
        try {
            byte[] answer = ArcFourHmac.encryptRaw(keybytes, KG_USAGE_SEAL,
                                        seqNum, all, 0, all.length);
            // Krb5Token.debug("\narcFourEncrypt encrypted:" +
            //  Krb5Token.getHexBytes(answer));
            return answer;
        } catch (Exception e) {
            // GeneralSecurityException, KrbCryptoException
            GSSException ge = new GSSException(GSSException.FAILURE, -1,
                "Could not use ArcFour Cipher - " + e.getMessage());
            ge.initCause(e);
            throw ge;
        }
    }

    // ---- AES methods
    private byte[] aes128Encrypt(byte[] confounder, byte[] tokenHeader,
        byte[] plaintext, int start, int len, int key_usage)
        throws GSSException {

        // encrypt { AES-plaintext-data | filler | header }
        // AES-plaintext-data { confounder | plaintext }
        // WrapToken = { tokenHeader |
        //      Encrypt (confounder | plaintext | tokenHeader ) | HMAC }

        byte[] all = new byte[confounder.length + len + tokenHeader.length];
        System.arraycopy(confounder, 0, all, 0, confounder.length);
        System.arraycopy(plaintext, start, all, confounder.length, len);
        System.arraycopy(tokenHeader, 0, all, confounder.length+len,
                                tokenHeader.length);

        // Krb5Token.debug("\naes128Encrypt:" + Krb5Token.getHexBytes(all));
        try {
            byte[] answer = Aes128.encryptRaw(keybytes, key_usage,
                                ZERO_IV_AES,
                                all, 0, all.length);
            // Krb5Token.debug("\naes128Encrypt encrypted:" +
            //                  Krb5Token.getHexBytes(answer));
            return answer;
        } catch (Exception e) {
            // GeneralSecurityException, KrbCryptoException
            GSSException ge = new GSSException(GSSException.FAILURE, -1,
                "Could not use AES128 Cipher - " + e.getMessage());
            ge.initCause(e);
            throw ge;
        }
    }

    private byte[] aes128Sha2Encrypt(byte[] confounder, byte[] tokenHeader,
            byte[] plaintext, int start, int len, int key_usage)
            throws GSSException {

        // encrypt { AES-plaintext-data | filler | header }
        // AES-plaintext-data { confounder | plaintext }
        // WrapToken = { tokenHeader |
        //      Encrypt (confounder | plaintext | tokenHeader ) | HMAC }

        byte[] all = new byte[confounder.length + len + tokenHeader.length];
        System.arraycopy(confounder, 0, all, 0, confounder.length);
        System.arraycopy(plaintext, start, all, confounder.length, len);
        System.arraycopy(tokenHeader, 0, all, confounder.length+len,
                tokenHeader.length);

        // Krb5Token.debug("\naes128Sha2Encrypt:" + Krb5Token.getHexBytes(all));
        try {
            byte[] answer = Aes128Sha2.encryptRaw(keybytes, key_usage,
                    ZERO_IV_AES,
                    all, 0, all.length);
            // Krb5Token.debug("\naes128Sha2Encrypt encrypted:" +
            //                  Krb5Token.getHexBytes(answer));
            return answer;
        } catch (Exception e) {
            // GeneralSecurityException, KrbCryptoException
            GSSException ge = new GSSException(GSSException.FAILURE, -1,
                    "Could not use Aes128Sha2 Cipher - " + e.getMessage());
            ge.initCause(e);
            throw ge;
        }
    }

    private void aes128Decrypt(WrapToken_v2 token, byte[] ciphertext,
        int cStart, int cLen, byte[] plaintext, int pStart, int key_usage)
        throws GSSException {

        byte[] ptext = null;

        try {
            ptext = Aes128.decryptRaw(keybytes, key_usage,
                        ZERO_IV_AES, ciphertext, cStart, cLen);
        } catch (GeneralSecurityException e) {
            GSSException ge = new GSSException(GSSException.FAILURE, -1,
                "Could not use AES128 Cipher - " + e.getMessage());
            ge.initCause(e);
            throw ge;
        }

        /*
        Krb5Token.debug("\naes128Decrypt in: " +
            Krb5Token.getHexBytes(ciphertext, cStart, cLen));
        Krb5Token.debug("\naes128Decrypt plain: " +
            Krb5Token.getHexBytes(ptext));
        Krb5Token.debug("\naes128Decrypt ptext: " +
            Krb5Token.getHexBytes(ptext));
        */

        // Strip out confounder and token header
        int len = ptext.length - WrapToken_v2.CONFOUNDER_SIZE -
                        WrapToken_v2.TOKEN_HEADER_SIZE;
        System.arraycopy(ptext, WrapToken_v2.CONFOUNDER_SIZE,
                                plaintext, pStart, len);

        /*
        Krb5Token.debug("\naes128Decrypt plaintext: " +
            Krb5Token.getHexBytes(plaintext, pStart, len));
        */
    }

    private void aes128Sha2Decrypt(WrapToken_v2 token, byte[] ciphertext,
            int cStart, int cLen, byte[] plaintext, int pStart, int key_usage)
            throws GSSException {

        byte[] ptext = null;

        try {
            ptext = Aes128Sha2.decryptRaw(keybytes, key_usage,
                    ZERO_IV_AES, ciphertext, cStart, cLen);
        } catch (GeneralSecurityException e) {
            GSSException ge = new GSSException(GSSException.FAILURE, -1,
                    "Could not use AES128Sha2 Cipher - " + e.getMessage());
            ge.initCause(e);
            throw ge;
        }

        /*
        Krb5Token.debug("\naes128Sha2Decrypt in: " +
            Krb5Token.getHexBytes(ciphertext, cStart, cLen));
        Krb5Token.debug("\naes128Sha2Decrypt plain: " +
            Krb5Token.getHexBytes(ptext));
        Krb5Token.debug("\naes128Sha2Decrypt ptext: " +
            Krb5Token.getHexBytes(ptext));
        */

        // Strip out confounder and token header
        int len = ptext.length - WrapToken_v2.CONFOUNDER_SIZE -
                WrapToken_v2.TOKEN_HEADER_SIZE;
        System.arraycopy(ptext, WrapToken_v2.CONFOUNDER_SIZE,
                plaintext, pStart, len);

        /*
        Krb5Token.debug("\naes128Sha2Decrypt plaintext: " +
            Krb5Token.getHexBytes(plaintext, pStart, len));
        */
    }

    private byte[] aes256Encrypt(byte[] confounder, byte[] tokenHeader,
        byte[] plaintext, int start, int len, int key_usage)
        throws GSSException {

        // encrypt { AES-plaintext-data | filler | header }
        // AES-plaintext-data { confounder | plaintext }
        // WrapToken = { tokenHeader |
        //       Encrypt (confounder | plaintext | tokenHeader ) | HMAC }

        byte[] all = new byte[confounder.length + len + tokenHeader.length];
        System.arraycopy(confounder, 0, all, 0, confounder.length);
        System.arraycopy(plaintext, start, all, confounder.length, len);
        System.arraycopy(tokenHeader, 0, all, confounder.length+len,
                                tokenHeader.length);

        // Krb5Token.debug("\naes256Encrypt:" + Krb5Token.getHexBytes(all));

        try {
            byte[] answer = Aes256.encryptRaw(keybytes, key_usage,
                                ZERO_IV_AES, all, 0, all.length);
            // Krb5Token.debug("\naes256Encrypt encrypted:" +
            //  Krb5Token.getHexBytes(answer));
            return answer;
        } catch (Exception e) {
            // GeneralSecurityException, KrbCryptoException
            GSSException ge = new GSSException(GSSException.FAILURE, -1,
                "Could not use AES256 Cipher - " + e.getMessage());
            ge.initCause(e);
            throw ge;
        }
    }

    private byte[] aes256Sha2Encrypt(byte[] confounder, byte[] tokenHeader,
            byte[] plaintext, int start, int len, int key_usage)
            throws GSSException {

        // encrypt { AES-plaintext-data | filler | header }
        // AES-plaintext-data { confounder | plaintext }
        // WrapToken = { tokenHeader |
        //       Encrypt (confounder | plaintext | tokenHeader ) | HMAC }

        byte[] all = new byte[confounder.length + len + tokenHeader.length];
        System.arraycopy(confounder, 0, all, 0, confounder.length);
        System.arraycopy(plaintext, start, all, confounder.length, len);
        System.arraycopy(tokenHeader, 0, all, confounder.length+len,
                tokenHeader.length);

        // Krb5Token.debug("\naes256Sha2Encrypt:" + Krb5Token.getHexBytes(all));

        try {
            byte[] answer = Aes256Sha2.encryptRaw(keybytes, key_usage,
                    ZERO_IV_AES, all, 0, all.length);
            // Krb5Token.debug("\naes256Sha2Encrypt encrypted:" +
            //  Krb5Token.getHexBytes(answer));
            return answer;
        } catch (Exception e) {
            // GeneralSecurityException, KrbCryptoException
            GSSException ge = new GSSException(GSSException.FAILURE, -1,
                    "Could not use Aes256Sha2 Cipher - " + e.getMessage());
            ge.initCause(e);
            throw ge;
        }
    }

    private void aes256Decrypt(WrapToken_v2 token, byte[] ciphertext,
        int cStart, int cLen, byte[] plaintext, int pStart, int key_usage)
        throws GSSException {

        byte[] ptext;
        try {
            ptext = Aes256.decryptRaw(keybytes, key_usage,
                        ZERO_IV_AES, ciphertext, cStart, cLen);
        } catch (GeneralSecurityException e) {
            GSSException ge = new GSSException(GSSException.FAILURE, -1,
                "Could not use AES128 Cipher - " + e.getMessage());
            ge.initCause(e);
            throw ge;
        }

        /*
        Krb5Token.debug("\naes256Decrypt in: " +
            Krb5Token.getHexBytes(ciphertext, cStart, cLen));
        Krb5Token.debug("\naes256Decrypt plain: " +
            Krb5Token.getHexBytes(ptext));
        Krb5Token.debug("\naes256Decrypt ptext: " +
            Krb5Token.getHexBytes(ptext));
        */

        // Strip out confounder and token header
        int len = ptext.length - WrapToken_v2.CONFOUNDER_SIZE -
                        WrapToken_v2.TOKEN_HEADER_SIZE;
        System.arraycopy(ptext, WrapToken_v2.CONFOUNDER_SIZE,
                                plaintext, pStart, len);

        /*
        Krb5Token.debug("\naes128Decrypt plaintext: " +
            Krb5Token.getHexBytes(plaintext, pStart, len));
        */

    }

    private void aes256Sha2Decrypt(WrapToken_v2 token, byte[] ciphertext,
            int cStart, int cLen, byte[] plaintext, int pStart, int key_usage)
            throws GSSException {

        byte[] ptext;
        try {
            ptext = Aes256Sha2.decryptRaw(keybytes, key_usage,
                    ZERO_IV_AES, ciphertext, cStart, cLen);
        } catch (GeneralSecurityException e) {
            GSSException ge = new GSSException(GSSException.FAILURE, -1,
                    "Could not use AES256Sha2 Cipher - " + e.getMessage());
            ge.initCause(e);
            throw ge;
        }

        /*
        Krb5Token.debug("\naes256Sha2Decrypt in: " +
            Krb5Token.getHexBytes(ciphertext, cStart, cLen));
        Krb5Token.debug("\naes256Sha2Decrypt plain: " +
            Krb5Token.getHexBytes(ptext));
        Krb5Token.debug("\naes256Sha2Decrypt ptext: " +
            Krb5Token.getHexBytes(ptext));
        */

        // Strip out confounder and token header
        int len = ptext.length - WrapToken_v2.CONFOUNDER_SIZE -
                WrapToken_v2.TOKEN_HEADER_SIZE;
        System.arraycopy(ptext, WrapToken_v2.CONFOUNDER_SIZE,
                plaintext, pStart, len);

        /*
        Krb5Token.debug("\naes256Sha2Decrypt plaintext: " +
            Krb5Token.getHexBytes(plaintext, pStart, len));
        */

    }

    /**
     * This class provides a truncated inputstream needed by WrapToken. The
     * truncated inputstream is passed to CipherInputStream. It prevents
     * the CipherInputStream from treating the bytes of the following token
     * as part fo the ciphertext for this token.
     */
    class WrapTokenInputStream extends InputStream {

        private InputStream is;
        private int length;
        private int remaining;

        private int temp;

        public WrapTokenInputStream(InputStream is, int length) {
            this.is = is;
            this.length = length;
            remaining = length;
        }

        public final int read() throws IOException {
            if (remaining == 0)
                return -1;
            else {
                temp = is.read();
                if (temp != -1)
                    remaining -= temp;
                return temp;
            }
        }

        public final int read(byte[] b) throws IOException {
            if (remaining == 0)
                return -1;
            else {
                temp = Math.min(remaining, b.length);
                temp = is.read(b, 0, temp);
                if (temp != -1)
                    remaining -= temp;
                return temp;
            }
        }

        public final int read(byte[] b,
                              int off,
                              int len) throws IOException {
            if (remaining == 0)
                return -1;
            else {
                temp = Math.min(remaining, len);
                temp = is.read(b, off, temp);
                if (temp != -1)
                    remaining -= temp;
                return temp;
            }
        }

        public final long skip(long n)  throws IOException {
            if (remaining == 0)
                return 0;
            else {
                temp = (int) Math.min(remaining, n);
                temp = (int) is.skip(temp);
                remaining -= temp;
                return temp;
            }
        }

        public final int available() throws IOException {
            return Math.min(remaining, is.available());
        }

        public final void close() throws IOException {
            remaining = 0;
        }
    }
}
