/*
 * Copyright (c) 2004, 2009, Oracle and/or its affiliates. All rights reserved.
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

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.spec.KeySpec;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.util.Arrays;

public class Des3DkCrypto extends DkCrypto {

    private static final byte[] ZERO_IV = new byte[] {0, 0, 0, 0, 0, 0, 0, 0};

    public Des3DkCrypto() {
    }

    protected int getKeySeedLength() {
        return 168;   // bits; 3DES key material has 21 bytes
    }

    public byte[] stringToKey(char[] salt) throws GeneralSecurityException {
        byte[] saltUtf8 = null;
        try {
            saltUtf8 = charToUtf8(salt);
            return stringToKey(saltUtf8, null);
        } finally {
            if (saltUtf8 != null) {
                Arrays.fill(saltUtf8, (byte)0);
            }
            // Caller responsible for clearing its own salt
        }
    }

    private byte[] stringToKey(byte[] secretAndSalt, byte[] opaque)
        throws GeneralSecurityException {

        if (opaque != null && opaque.length > 0) {
            throw new RuntimeException("Invalid parameter to stringToKey");
        }

        byte[] tmpKey = randomToKey(nfold(secretAndSalt, getKeySeedLength()));
        return dk(tmpKey, KERBEROS_CONSTANT);
    }

    public byte[] parityFix(byte[] value)
        throws GeneralSecurityException {
        // fix key parity
        setParityBit(value);
        return value;
    }

    /*
     * From RFC 3961.
     *
     * The 168 bits of random key data are converted to a protocol key value
     * as follows.  First, the 168 bits are divided into three groups of 56
     * bits, which are expanded individually into 64 bits as in des3Expand().
     * Result is a 24 byte (192-bit) key.
     */
    protected byte[] randomToKey(byte[] in) {
        if (in.length != 21) {
            throw new IllegalArgumentException("input must be 168 bits");
        }

        byte[] one = keyCorrection(des3Expand(in, 0, 7));
        byte[] two = keyCorrection(des3Expand(in, 7, 14));
        byte[] three = keyCorrection(des3Expand(in, 14, 21));

        byte[] key = new byte[24];
        System.arraycopy(one, 0, key, 0, 8);
        System.arraycopy(two, 0, key, 8, 8);
        System.arraycopy(three, 0, key, 16, 8);

        return key;
    }

    private static byte[] keyCorrection(byte[] key) {
        // check for weak key
        try {
            if (DESKeySpec.isWeak(key, 0)) {
                key[7] = (byte)(key[7] ^ 0xF0);
            }
        } catch (InvalidKeyException ex) {
            // swallow, since it should never happen
        }
        return key;
    }

    /**
     * From RFC 3961.
     *
     * Expands a 7-byte array into an 8-byte array that contains parity bits.
     * The 56 bits are expanded into 64 bits as follows:
     *   1  2  3  4  5  6  7  p
     *   9 10 11 12 13 14 15  p
     *   17 18 19 20 21 22 23  p
     *   25 26 27 28 29 30 31  p
     *   33 34 35 36 37 38 39  p
     *   41 42 43 44 45 46 47  p
     *   49 50 51 52 53 54 55  p
     *   56 48 40 32 24 16  8  p
     *
     * (PI,P2,...,P8) are reserved for parity bits computed on the preceding
     * seven independent bits and set so that the parity of the octet is odd,
     * i.e., there is an odd number of "1" bits in the octet.
     *
     * @param start index of starting byte (inclusive)
     * @param end index of ending byte (exclusive)
     */
    private static byte[] des3Expand(byte[] input, int start, int end) {
        if ((end - start) != 7)
            throw new IllegalArgumentException(
                "Invalid length of DES Key Value:" + start + "," + end);

        byte[] result = new byte[8];
        byte last = 0;
        System.arraycopy(input, start, result, 0, 7);
        byte posn = 0;

        // Fill in last row
        for (int i = start; i < end; i++) {
            byte bit = (byte) (input[i]&0x01);
            if (debug) {
                System.out.println(i + ": " + Integer.toHexString(input[i]) +
                    " bit= " + Integer.toHexString(bit));
            }
            ++posn;
            if (bit != 0) {
                last |= (bit<<posn);
            }
        }

        if (debug) {
            System.out.println("last: " + Integer.toHexString(last));
        }
        result[7] = last;
        setParityBit(result);
        return result;
    }

    /**
     * Sets the parity bit (0th bit) in each byte so that each byte
     * contains an odd number of 1's.
     */
    private static void setParityBit(byte[] key) {
        for (int i = 0; i < key.length; i++) {
            int b = key[i] & 0xfe;
            b |= (Integer.bitCount(b) & 1) ^ 1;
            key[i] = (byte) b;
        }
    }

    protected Cipher getCipher(byte[] key, byte[] ivec, int mode)
        throws GeneralSecurityException {
        // NoSuchAlgorithException
        SecretKeyFactory factory = SecretKeyFactory.getInstance("desede");

        // InvalidKeyException
        KeySpec spec = new DESedeKeySpec(key, 0);

        // InvalidKeySpecException
        SecretKey secretKey = factory.generateSecret(spec);

        // IV
        if (ivec == null) {
            ivec = ZERO_IV;
        }

        // NoSuchAlgorithmException, NoSuchPaddingException
        // NoSuchProviderException
        Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
        IvParameterSpec encIv = new IvParameterSpec(ivec, 0, ivec.length);

        // InvalidKeyException, InvalidAlgorithParameterException
        cipher.init(mode, secretKey, encIv);

        return cipher;
    }

    public int getChecksumLength() {
        return 20;  // bytes
    }

    protected byte[] getHmac(byte[] key, byte[] msg)
        throws GeneralSecurityException {

        SecretKey keyKi = new SecretKeySpec(key, "HmacSHA1");
        Mac m = Mac.getInstance("HmacSHA1");
        m.init(keyKi);
        return m.doFinal(msg);
    }
}
