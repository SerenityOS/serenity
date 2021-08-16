/*
 * Copyright (c) 2002, 2007, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * @test
 * @bug 4688768
 * @library ../UTIL
 * @build TestUtil
 * @run main CounterMode
 * @summary Verify that CTR mode works as expected
 * @author Andreas Sterbenz
 */

import java.util.*;

import javax.crypto.*;
import javax.crypto.spec.*;

public class CounterMode {

    private final static byte[] toByteArray(String s) {
        char[] c = s.toCharArray();
        byte[] t = new byte[c.length / 2];
        int n = 0;
        int d1 = -1;
        int d2 = -1;
        for (int i = 0; i < c.length; i++) {
            char ch = c[i];
            if (d1 == -1) {
                d1 = Character.digit(ch, 16);
            } else {
                d2 = Character.digit(ch, 16);
                if (d2 != -1) {
                    t[n++] = (byte)((d1 << 4) | d2);
                    d1 = -1;
                    d2 = -1;
                }
            }
        }
        if (d1 != -1) {
            throw new RuntimeException();
        }
        if (n == t.length) {
            return t;
        }
        byte[] b = new byte[n];
        System.arraycopy(t, 0, b, 0, n);
        return b;
    }

    private final static String[] ALGORITHM = {
        "AES",
        "AES",
        "AES",
        "AES",
        "AES",
        "DES",
        "DES",
        "DESede",
        "DESede",
        "Blowfish",
        "Blowfish",
    };

    private final static byte[][] KEYS = {
        toByteArray("2b7e151628aed2a6abf7158809cf4f3c"),
        toByteArray("8e73b0f7da0e6452c810f32b809079e562f8ead2522c6b7b"),
        toByteArray("603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4"),
        toByteArray("2b7e151628aed2a6abf7158809cf4f3c"),
        toByteArray("2b7e151628aed2a6abf7158809cf4f3c"),
        toByteArray("2b7e151628aed2a6"),
        toByteArray("874d6191b620e326"),
        toByteArray("8e73b0f7da0e6452c810f32b809079e562f8ead2522c6b7b"),
        toByteArray("874d6191b620e3261bef6864990db6ce9806f66b7970fdff"),
        toByteArray("2b7e151628aed2a6abf7158809cf4f3c"),
        toByteArray("874d6191b620e3261bef6864990db6ce"),
    };

    private final static byte[][] IVS = {
        toByteArray("f0f1f2f3f4f5f6f7f8f9fafbfcfdfeff"),
        toByteArray("f0f1f2f3f4f5f6f7f8f9fafbfcfdfeff"),
        toByteArray("f0f1f2f3f4f5f6f7f8f9fafbfcfdfeff"),
        toByteArray("f0f1f2f3f4f5f6f7f8f9fafbfcfdfeff"),
        toByteArray("f0f1f2f3f4f5f6f7f8f9fafbfcfdfeff"),
        toByteArray("fffffffffffffffd"),
        toByteArray("f0f1f2f3f4f5f6f7"),
        toByteArray("f0f1f2f3f4f5f6f7"),
        toByteArray("f0f1f2f3f4f5f6ff"),
        toByteArray("f0f1f2f3f4f5f6f7"),
        toByteArray("f0f1f2f3f4f5f6ff"),
    };

    private final static byte[][] PLAIN = {
        toByteArray("6bc1bee22e409f96e93d7e117393172aae2d8a571e03ac9c9eb76fac45af8e5130c81c46a35ce411e5fbc1191a0a52eff69f2445df4f9b17ad2b417be66c3710"),
        toByteArray("6bc1bee22e409f96e93d7e117393172aae2d8a571e03ac9c9eb76fac45af8e5130c81c46a35ce411e5fbc1191a0a52eff69f2445df4f9b17ad2b417be66c3710"),
        toByteArray("6bc1bee22e409f96e93d7e117393172aae2d8a571e03ac9c9eb76fac45af8e5130c81c46a35ce411e5fbc1191a0a52eff69f2445df4f9b17ad2b417be66c3710"),
        toByteArray("6bc1bee22e409f96e93d7e117393172aae2d8a571e03ac9c9eb76fac45af8e5130c81c46a35ce411e5fbc1191a0a52eff69f2445df4f9b17ad2b417be66c37"),
        toByteArray(""),
        toByteArray("6bc1bee22e409f96e93d7e117393172aae2d8a571e03ac9c9eb76fac45af8e5130c81c46a35ce411e5fbc1191a0a52eff69f2445df4f9b17ad2b417be66c3710"),
        toByteArray("6bc1bee22e409f96"),
        toByteArray("6bc1bee22e409f96e93d7e117393172aae2d8a571e03ac9c9eb76fac45af8e5130c81c46a35ce411e5fbc1191a0a52eff69f2445df4f9b17ad2b417be66c3710"),
        toByteArray("6bc1bee22e409f96e9"),
        toByteArray("6bc1bee22e409f96e93d7e117393172aae2d8a571e03ac9c9eb76fac45af8e5130c81c46a35ce411e5fbc1191a0a52eff69f2445df4f9b17ad2b417be66c3710"),
        toByteArray("6bc1bee22e409f"),
    };

    private final static byte[][] CIPHER = {
        toByteArray("874d6191b620e3261bef6864990db6ce9806f66b7970fdff8617187bb9fffdff5ae4df3edbd5d35e5b4f09020db03eab1e031dda2fbe03d1792170a0f3009cee"),
        toByteArray("1abc932417521ca24f2b0459fe7e6e0b090339ec0aa6faefd5ccc2c6f4ce8e941e36b26bd1ebc670d1bd1d665620abf74f78a7f6d29809585a97daec58c6b050"),
        toByteArray("601ec313775789a5b7a7f504bbf3d228f443e3ca4d62b59aca84e990cacaf5c52b0930daa23de94ce87017ba2d84988ddfc9c58db67aada613c2dd08457941a6"),
        toByteArray("874d6191b620e3261bef6864990db6ce9806f66b7970fdff8617187bb9fffdff5ae4df3edbd5d35e5b4f09020db03eab1e031dda2fbe03d1792170a0f3009c"),
        toByteArray(""),
        toByteArray("79:62:2c:42:59:8e:9a:1a:e3:da:3d:7c:88:4f:0a:d2:eb:1a:e2:27:b0:d7:16:f6:6d:8d:50:fe:8b:9f:87:a7:ba:83:65:48:5b:87:12:07:d0:3f:3d:f0:b2:c8:98:e9:bb:a0:ad:35:98:e8:4f:d8:ef:91:85:2c:55:c0:b0:b9"),
        toByteArray("79:5e:90:a6:35:9d:1e:b2"),
        toByteArray("0a:ce:b5:1e:20:74:2e:29:17:28:55:d6:c9:28:62:c3:43:a5:da:30:90:9f:8f:46:60:3d:b6:5c:b6:bc:21:52:06:2f:6f:b4:14:0d:ef:23:e2:99:fd:4b:dc:65:ee:6a:50:76:5b:86:43:65:56:ee:1a:7a:25:07:43:b4:f0:88"),
        toByteArray("12:06:8f:ae:bb:47:a7:68:8b"),
        toByteArray("ac:92:83:dd:79:f3:c2:36:0c:da:06:bd:ed:69:ca:47:42:2e:68:11:5e:6b:63:56:3e:87:b0:be:98:17:42:7d:8b:4a:5b:be:af:0c:2b:f0:1c:1c:62:60:b9:2c:69:04:ca:c9:23:d2:08:9d:8d:57:7f:61:f7:e6:79:83:ae:89"),
        toByteArray("65:ff:ee:6b:64:9e:a0"),
    };

    private static String toString(byte[] b) {
        StringBuffer sb = new StringBuffer(b.length * 3);
        for (int i = 0; i < b.length; i++) {
            int k = b[i] & 0xff;
            if (i != 0) {
                sb.append(':');
            }
            sb.append(Character.forDigit(k >> 4, 16));
            sb.append(Character.forDigit(k & 0xf, 16));
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < KEYS.length; i++) {
            try {
                String alg = ALGORITHM[i];
                int MAX_KEYSIZE = Cipher.getMaxAllowedKeyLength(alg);
                if (KEYS[i].length*8 > MAX_KEYSIZE) {
                    // skip tests using keys whose length exceeds
                    // what's configured in jce jurisdiction policy files.
                    continue;
                }
                System.out.println("Running test " + i +  " (" + alg + ")");
                Cipher cipher = Cipher.getInstance(alg + "/CTR/NoPadding", "SunJCE");
                SecretKeySpec key = new SecretKeySpec(KEYS[i], alg);
                IvParameterSpec iv = new IvParameterSpec(IVS[i]);
                byte[] plainText = PLAIN[i];
                byte[] cipherText = CIPHER[i];
                cipher.init(Cipher.ENCRYPT_MODE, key, iv);
                byte[] enc = cipher.doFinal(plainText);
                if (Arrays.equals(cipherText, enc) == false) {
                    System.out.println("plain:  " + toString(PLAIN[i]));
                    System.out.println("cipher: " + toString(CIPHER[i]));
                    System.out.println("actual: " + toString(enc));
                    throw new RuntimeException("Encryption failure for test " + i);
                }
                cipher.init(Cipher.DECRYPT_MODE, key, iv);
                byte[] dec = cipher.doFinal(cipherText);
                if (Arrays.equals(plainText, dec) == false) {
                    throw new RuntimeException("Decryption failure for test " + i);
                }
            } catch (SecurityException ex) {
                TestUtil.handleSE(ex);
            }
        }
        System.out.println("Done");
    }
}
