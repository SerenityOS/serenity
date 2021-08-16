/*
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
 *
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5.internal.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import javax.crypto.spec.IvParameterSpec;
import sun.security.krb5.KrbCryptoException;
import java.util.Arrays;
import sun.security.action.GetPropertyAction;

public final class Des {

    // RFC 3961 demands that UTF-8 encoding be used in DES's
    // string-to-key function. For historical reasons, some
    // implementations use a locale-specific encoding. Even
    // so, when the client and server use different locales,
    // they must agree on a common value, normally the one
    // used when the password is set/reset.
    //
    // The following system property is provided to perform the
    // string-to-key encoding. When set, the specified charset
    // name is used. Otherwise, the system default charset.

    private static final String CHARSET = GetPropertyAction
            .privilegedGetProperty("sun.security.krb5.msinterop.des.s2kcharset");

    private static final long[] bad_keys = {
        0x0101010101010101L, 0xfefefefefefefefeL,
        0x1f1f1f1f1f1f1f1fL, 0xe0e0e0e0e0e0e0e0L,
        0x01fe01fe01fe01feL, 0xfe01fe01fe01fe01L,
        0x1fe01fe00ef10ef1L, 0xe01fe01ff10ef10eL,
        0x01e001e001f101f1L, 0xe001e001f101f101L,
        0x1ffe1ffe0efe0efeL, 0xfe1ffe1ffe0efe0eL,
        0x011f011f010e010eL, 0x1f011f010e010e01L,
        0xe0fee0fef1fef1feL, 0xfee0fee0fef1fef1L
    };

    private static final byte[] good_parity = {
        1,       1,   2,   2,   4,   4,   7,   7,
        8,   8,   11,  11,  13,  13,  14,  14,
        16,  16,  19,  19,  21,  21,  22,  22,
        25,  25,  26,  26,  28,  28,  31,  31,
        32,  32,  35,  35,  37,  37,  38,  38,
        41,  41,  42,  42,  44,  44,  47,  47,
        49,  49,  50,  50,  52,  52,  55,  55,
        56,  56,  59,  59,  61,  61,  62,  62,
        64,  64,  67,  67,  69,  69,  70,  70,
        73,  73,  74,  74,  76,  76,  79,  79,
        81,  81,  82,  82,  84,  84,  87,  87,
        88,  88,  91,  91,  93,  93,  94,  94,
        97,  97,  98,  98,  100, 100, 103, 103,
        104, 104, 107, 107, 109, 109, 110, 110,
        112, 112, 115, 115, 117, 117, 118, 118,
        121, 121, 122, 122, 124, 124, 127, 127,
        (byte)128, (byte)128, (byte)131, (byte)131,
        (byte)133, (byte)133, (byte)134, (byte)134,
        (byte)137, (byte)137, (byte)138, (byte)138,
        (byte)140, (byte)140, (byte)143, (byte)143,
        (byte)145, (byte)145, (byte)146, (byte)146,
        (byte)148, (byte)148, (byte)151, (byte)151,
        (byte)152, (byte)152, (byte)155, (byte)155,
        (byte)157, (byte)157, (byte)158, (byte)158,
        (byte)161, (byte)161, (byte)162, (byte)162,
        (byte)164, (byte)164, (byte)167, (byte)167,
        (byte)168, (byte)168, (byte)171, (byte)171,
        (byte)173, (byte)173, (byte)174, (byte)174,
        (byte)176, (byte)176, (byte)179, (byte)179,
        (byte)181, (byte)181, (byte)182, (byte)182,
        (byte)185, (byte)185, (byte)186, (byte)186,
        (byte)188, (byte)188, (byte)191, (byte)191,
        (byte)193, (byte)193, (byte)194, (byte)194,
        (byte)196, (byte)196, (byte)199, (byte)199,
        (byte)200, (byte)200, (byte)203, (byte)203,
        (byte)205, (byte)205, (byte)206, (byte)206,
        (byte)208, (byte)208, (byte)211, (byte)211,
        (byte)213, (byte)213, (byte)214, (byte)214,
        (byte)217, (byte)217, (byte)218, (byte)218,
        (byte)220, (byte)220, (byte)223, (byte)223,
        (byte)224, (byte)224, (byte)227, (byte)227,
        (byte)229, (byte)229, (byte)230, (byte)230,
        (byte)233, (byte)233, (byte)234, (byte)234,
        (byte)236, (byte)236, (byte)239, (byte)239,
        (byte)241, (byte)241, (byte)242, (byte)242,
        (byte)244, (byte)244, (byte)247, (byte)247,
        (byte)248, (byte)248, (byte)251, (byte)251,
        (byte)253, (byte)253, (byte)254, (byte)254
    };

    public static final byte[] set_parity(byte[] key) {
        for (int i=0; i < 8; i++) {
            key[i] = good_parity[key[i] & 0xff];
        }
        return key;
    }

    public static final long set_parity(long key) {
        return octet2long(set_parity(long2octet(key)));
    }

    public static final boolean bad_key(long key) {
        for (int i = 0; i < bad_keys.length; i++) {
            if (bad_keys[i] == key) {
                return true;
            }
        }
        return false;
    }

    public static final boolean bad_key(byte[] key) {
        return bad_key(octet2long(key));
    }

    public static long octet2long(byte[] input) {
        return octet2long(input, 0);
    }

    public static long octet2long(byte[] input, int offset) {   //convert a 8-byte to a long
        long result = 0;
        for (int i = 0; i < 8; i++) {
            if (i + offset < input.length) {
                result |= (((long)input[i + offset]) & 0xffL) << ((7 - i) * 8);
            }
        }
        return result;
    }

    public static byte[] long2octet(long input) {
        byte[] output = new byte[8];
        for (int i = 0; i < 8; i++) {
            output[i] = (byte)((input >>> ((7 - i) * 8)) & 0xffL);
        }
        return output;
    }

    public static void long2octet(long input, byte[] output) {
        long2octet(input, output, 0);
    }

    public static void long2octet(long input, byte[] output, int offset) {
        for (int i = 0; i < 8; i++) {
            if (i + offset < output.length) {
                output[i + offset] =
                    (byte)((input >>> ((7 - i) * 8)) & 0xffL);
            }
        }
    }

    /**
     * Creates a DES cipher in Electronic Codebook mode, with no padding.
     * @param input plain text.
     * @param output the buffer for the result.
     * @param key DES the key to encrypt the text.
     * @param ivec initialization vector.
     *
     * @created by Yanni Zhang, Dec 6 99.
     */
    public static void cbc_encrypt (
                                    byte[] input,
                                    byte[] output,
                                    byte[] key,
                                    byte[] ivec,
                                    boolean encrypt) throws KrbCryptoException {

        Cipher cipher = null;

        try {
            cipher = Cipher.getInstance("DES/CBC/NoPadding");
        } catch (GeneralSecurityException e) {
            KrbCryptoException ke = new KrbCryptoException("JCE provider may not be installed. "
                                                           + e.getMessage());
            ke.initCause(e);
            throw ke;
        }
        IvParameterSpec params = new IvParameterSpec(ivec);
        SecretKeySpec skSpec = new SecretKeySpec(key, "DES");
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
            //                  SecretKey sk = skf.generateSecret(skSpec);
            SecretKey sk = (SecretKey) skSpec;
            if (encrypt)
                cipher.init(Cipher.ENCRYPT_MODE, sk, params);
            else
                cipher.init(Cipher.DECRYPT_MODE, sk, params);
            byte[] result;
            result = cipher.doFinal(input);
            System.arraycopy(result, 0, output, 0, result.length);
        } catch (GeneralSecurityException e) {
            KrbCryptoException ke = new KrbCryptoException(e.getMessage());
            ke.initCause(e);
            throw ke;
        }
    }

    /**
     * Generates DES key from the password.
     * @param passwdChars a char[] used to create the key.
     * @return DES key.
     *
     * @modified by Yanni Zhang, Dec 6, 99
     */
    public static long char_to_key(char[] passwdChars) throws KrbCryptoException {
        long key = 0;
        long octet, octet1, octet2 = 0;
        byte[] cbytes = null;

        // Convert password to byte array
        try {
            if (CHARSET == null) {
                cbytes = (new String(passwdChars)).getBytes();
            } else {
                cbytes = (new String(passwdChars)).getBytes(CHARSET);
            }
        } catch (Exception e) {
            // clear-up sensitive information
            if (cbytes != null) {
                Arrays.fill(cbytes, 0, cbytes.length, (byte) 0);
            }
            KrbCryptoException ce =
                new KrbCryptoException("Unable to convert passwd, " + e);
            ce.initCause(e);
            throw ce;
        }

        // pad data
        byte[] passwdBytes = pad(cbytes);

        byte[] newkey = new byte[8];
        int length = (passwdBytes.length / 8) + (passwdBytes.length % 8  == 0 ? 0 : 1);
        for (int i = 0; i < length; i++) {
            octet = octet2long(passwdBytes, i * 8) & 0x7f7f7f7f7f7f7f7fL;
            if (i % 2 == 1) {
                octet1 = 0;
                for (int j = 0; j < 64; j++) {
                    octet1 |= ((octet & (1L << j)) >>> j) << (63 - j);
                }
                octet = octet1 >>> 1;
            }
            key ^= (octet << 1);
        }
        key = set_parity(key);
        if (bad_key(key)) {
            byte [] temp = long2octet(key);
            temp[7] ^= 0xf0;
            key = octet2long(temp);
        }

        newkey = des_cksum(long2octet(key), passwdBytes, long2octet(key));
        key = octet2long(set_parity(newkey));
        if (bad_key(key)) {
            byte [] temp = long2octet(key);
            temp[7] ^= 0xf0;
            key = octet2long(temp);
        }

        // clear-up sensitive information
        if (cbytes != null) {
            Arrays.fill(cbytes, 0, cbytes.length, (byte) 0);
        }
        if (passwdBytes != null) {
            Arrays.fill(passwdBytes, 0, passwdBytes.length, (byte) 0);
        }

        return key;
    }

    /**
     * Encrypts the message blocks using DES CBC and output the
     * final block of 8-byte ciphertext.
     * @param ivec Initialization vector.
     * @param msg Input message as an byte array.
     * @param key DES key to encrypt the message.
     * @return the last block of ciphertext.
     *
     * @created by Yanni Zhang, Dec 6, 99.
     */
    public static byte[] des_cksum(byte[] ivec, byte[] msg, byte[] key) throws KrbCryptoException {
        Cipher cipher = null;

        byte[] result = new byte[8];
        try{
            cipher = Cipher.getInstance("DES/CBC/NoPadding");
        } catch (Exception e) {
            KrbCryptoException ke = new KrbCryptoException("JCE provider may not be installed. "
                                                           + e.getMessage());
            ke.initCause(e);
            throw ke;
        }
        IvParameterSpec params = new IvParameterSpec(ivec);
        SecretKeySpec skSpec = new SecretKeySpec(key, "DES");
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
            // SecretKey sk = skf.generateSecret(skSpec);
            SecretKey sk = (SecretKey) skSpec;
            cipher.init(Cipher.ENCRYPT_MODE, sk, params);
            for (int i = 0; i < msg.length / 8; i++) {
                result = cipher.doFinal(msg, i * 8, 8);
                cipher.init(Cipher.ENCRYPT_MODE, sk, (new IvParameterSpec(result)));
            }
        }
        catch (GeneralSecurityException e) {
            KrbCryptoException ke = new KrbCryptoException(e.getMessage());
            ke.initCause(e);
            throw ke;
        }
        return result;
    }

    /**
     * Pads the data so that its length is a multiple of 8 bytes.
     * @param data the raw data.
     * @return the data being padded.
     *
     * @created by Yanni Zhang, Dec 6 99. //Kerberos does not use PKCS5 padding.
     */
    static byte[] pad(byte[] data) {
        int len;
        if (data.length < 8) len = data.length;
        else len = data.length % 8;
        if (len == 0) return data;
        else {
            byte[] padding = new byte[ 8 - len + data.length];
            for (int i = padding.length - 1; i > data.length - 1; i--) {
                padding[i] = 0;
            }
            System.arraycopy(data, 0, padding, 0, data.length);
            return padding;
        }
    }

    // Caller is responsible for clearing password
    public static byte[] string_to_key_bytes(char[] passwdChars)
        throws KrbCryptoException {
        return long2octet(char_to_key(passwdChars));
    }
}
