/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4853306
 * @summary Test RSA Cipher implementation
 * @author Andreas Sterbenz
 * @key randomness
 */

import java.io.*;
import java.util.*;
import java.math.BigInteger;

import java.security.*;
import java.security.spec.*;

import javax.crypto.*;

public class TestRSA {

    private final static char[] hexDigits = "0123456789abcdef".toCharArray();

    public static String toString(byte[] b) {
        if (b == null) {
            return "(null)";
        }
        StringBuffer sb = new StringBuffer(b.length * 3);
        for (int i = 0; i < b.length; i++) {
            int k = b[i] & 0xff;
            if (i != 0) {
                sb.append(':');
            }
            sb.append(hexDigits[k >>> 4]);
            sb.append(hexDigits[k & 0xf]);
        }
        return sb.toString();
    }

    public static byte[] parse(String s) {
        try {
            int n = s.length();
            ByteArrayOutputStream out = new ByteArrayOutputStream(n / 3);
            StringReader r = new StringReader(s);
            while (true) {
                int b1 = nextNibble(r);
                if (b1 < 0) {
                    break;
                }
                int b2 = nextNibble(r);
                if (b2 < 0) {
                    throw new RuntimeException("Invalid string " + s);
                }
                int b = (b1 << 4) | b2;
                out.write(b);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] b(String s) {
        return parse(s);
    }

    private static int nextNibble(StringReader r) throws IOException {
        while (true) {
            int ch = r.read();
            if (ch == -1) {
                return -1;
            } else if ((ch >= '0') && (ch <= '9')) {
                return ch - '0';
            } else if ((ch >= 'a') && (ch <= 'f')) {
                return ch - 'a' + 10;
            } else if ((ch >= 'A') && (ch <= 'F')) {
                return ch - 'A' + 10;
            }
        }
    }

    private final static BigInteger N = new BigInteger
    ("116231208661367609700141079576488663663527180869991078124978203037949869"
    +"312762870627991319537001781149083155962615105864954367253799351549459177"
    +"839995715202060014346744789001273681801687605044315560723525700773069112"
    +"214443196787519930666193675297582113726306864236010438506452172563580739"
    +"994193451997175316921");

    private final static BigInteger E = BigInteger.valueOf(65537);

    private final static BigInteger D = new BigInteger
    ("528278531576995741358027120152717979850387435582102361125581844437708890"
    +"736418759997555187916546691958396015481089485084669078137376029510618510"
    +"203389286674134146181629472813419906337170366867244770096128371742241254"
    +"843638089774095747779777512895029847721754360216404183209801002443859648"
    +"26168432372077852785");

    private final static Random RANDOM = new Random();

    private static Provider p;

    private static void testEncDec(String alg, int len, Key encKey, Key decKey) throws Exception {
        System.out.println("Testing en/decryption using " + alg + "...");
        Cipher c = Cipher.getInstance(alg, p);

        byte[] b = new byte[len];
        RANDOM.nextBytes(b);
        b[0] &= 0x3f;
        b[0] |= 1;

        c.init(Cipher.ENCRYPT_MODE, encKey);
        byte[] enc = c.doFinal(b);

        c.init(Cipher.DECRYPT_MODE, decKey);
        byte[] dec = c.doFinal(enc);

        if (Arrays.equals(b, dec) == false) {
            System.out.println("in:  " + toString(b));
            System.out.println("dec: " + toString(dec));
            throw new Exception("Failure");
        }
    }

    public static void testKat(String alg, int mode, Key key, String in, String out, boolean result) throws Exception {
        System.out.println("Testing known values for " + alg + "...");
        Cipher c = Cipher.getInstance(alg, p);
        c.init(mode, key);
        byte[] r = c.doFinal(parse(in));
        byte[] s = parse(out);
        if (Arrays.equals(r, s) != result) {
            throw new Exception("Unexpected test result");
        }
    }

    private final static String in2  = "0f:7d:6c:20:75:99:a5:bc:c1:53:b0:4e:8d:ef:98:fb:cf:2d:e5:1d:d4:bf:71:56:12:b7:a3:c3:e4:53:1b:07:d3:bb:94:a7:a7:28:75:1e:83:46:c9:80:4e:3f:ac:b2:47:06:9f:1b:68:38:73:b8:69:9e:6b:8b:8b:23:60:31:ae:ea:36:24:6f:85:af:de:a5:2a:88:7d:6a:9f:8a:9f:61:f6:59:3f:a8:ce:91:75:49:e9:34:b8:9f:b6:21:8c";
    private final static String out2 = "7d:84:d1:3a:dc:ac:46:09:3a:0c:e5:4b:85:5d:fa:bb:52:f1:0f:de:d9:87:ef:b3:f7:c8:e3:9a:29:be:e9:b5:51:57:fd:07:5b:3c:1c:1c:56:aa:0c:a6:3f:79:40:16:ee:2c:2c:2e:fe:b8:3e:fd:45:90:1c:e7:87:1d:0a:0a:c5:de:9d:2b:a9:dd:77:d2:89:ba:98:fe:78:5b:a3:91:b4:ac:b5:ae:ce:45:21:f7:74:97:3e:a9:58:59:bc:14:13:02:3f:09:7b:97:90:b3:bd:53:cb:15:c0:6e:36:ea:d4:a3:3e:fc:94:85:a9:66:7f:57:b4:2a:ae:70:2e:fb";

    private final static String in1  = "17:a3:a7:b1:86:29:06:c5:81:33:cd:2f:da:32:7c:0e:26:a8:18:aa:37:9b:dd:4a:b0:b0:a7:1c:14:82:6c:d9:c9:14:9f:55:19:91:02:0d:d9:d7:95:c2:2b:a6:fa:ba:a3:51:00:83:6b:ec:97:27:40:a3:8f:ba:b1:09:15:11:44:33:c6:3c:47:95:50:71:50:5a:f4:aa:00:4e:b9:48:6a:b1:34:e9:d0:c8:b8:92:bf:95:f3:3d:91:66:93:2b";
    private final static String out1 = "28:b7:b4:73:f2:16:11:c0:67:70:96:ee:dc:3e:23:87:9f:30:a7:e5:f0:db:aa:67:33:27:0e:75:79:af:29:f5:88:3d:93:22:14:d2:59:b4:eb:ce:95:7f:24:74:df:f2:aa:4d:e6:65:5a:63:6d:64:30:ef:31:f1:a6:df:17:42:b6:d1:ed:22:1f:b0:96:69:9d:f8:ce:ff:3a:47:96:51:ba:d9:8d:57:39:40:dc:fc:d3:03:92:39:f4:dd:4b:1b:07:8b:33:60:27:2d:5f:c6:cf:17:92:c6:12:69:a3:54:2e:b8:0f:ca:d9:46:0f:da:95:34:d0:84:35:9c:f6:44";

    private final static String rin1  = "09:01:06:53:a7:96:09:63:ef:e1:3f:e9:8d:95:22:d1:0e:1b:87:c1:a2:41:b2:09:97:a3:5e:e0:a4:1d:59:91:21:e4:ca:87:bf:77:4a:7e:a2:22:ff:59:1e:bd:a4:80:aa:93:4a:41:56:95:5b:f4:57:df:fc:52:2f:46:9b:45:d7:03:ae:22:8e:67:9e:6c:b9:95:4f:bd:8e:e8:67:90:5b:fe:de:2f:11:22:2e:9d:30:93:6d:c0:48:00:cb:08:b9:c4:36:e9:03:7c:08:2d:68:42:cb:71:d0:7d:47:22:c1:58:c5:b8:2f:28:3e:98:78:11:6d:71:5b:3b:36:3c";
    private final static String rout1 = "4a:21:64:20:56:5f:27:0c:90:1d:f3:1b:64:8e:16:d3:af:79:ca:c6:65:56:19:77:8f:25:35:70:be:f3:15:b3:e3:d8:8f:04:ec:c3:60:59:d0:9a:66:be:1c:ad:f7:09:46:a9:09:46:12:5f:28:b6:28:b1:53:fb:fe:07:73:b8:8b:f8:83:64:8e:2d:45:ca:1a:fd:85:4a:2c:fa:fc:e6:58:f7:e4:83:68:8c:38:49:2b:f3:5c:c1:2d:24:6a:cd:22:6d:cb:f4:f1:8c:9e:1a:94:a7:4b:6f:d1:b4:b4:ab:56:8b:a3:a9:89:88:c3:5d:a8:47:2a:67:50:32:71:19";

    private final static String rin2  = "1b:49:a6:7a:83:1c:b6:28:47:16:2f:be:6a:d3:28:a6:83:07:4f:50:be:5c:99:26:2a:15:b8:21:a8:cc:8a:45:93:07:ff:32:67:3c:a4:92:d2:cd:43:eb:f5:2e:09:79:c8:32:3a:9d:00:4c:f5:6e:65:b2:ca:9c:c2:d5:35:8e:fe:6c:ba:1a:7b:65:c1:4f:e9:6c:cb:5d:9f:13:5d:5f:be:32:cd:91:ed:8b:d7:d7:e9:d6:5c:cc:11:7b:d9:ff:7a:93:de:e4:81:92:56:0c:52:47:75:56:a8:e0:9a:55:16:0c:43:df:ae:be:a1:6a:9d:5a:be:fc:51:ea:52:0c";
    private final static String rout2 = "65:28:b9:48:8d:68:3f:5e:9a:85:e7:09:78:4c:0c:0e:60:6c:89:43:3c:d3:72:b9:2f:5a:eb:4f:15:77:93:9d:47:05:a6:52:48:72:ee:ce:e8:5a:6d:28:b0:06:5a:a1:93:58:a1:61:3f:9b:42:0d:c1:ec:32:0a:7a:1e:38:45:47:87:52:16:62:c9:44:c6:04:4d:82:64:01:f4:b1:26:dc:7f:61:82:52:7a:f6:6b:ab:22:98:87:93:63:4c:3f:92:c7:5b:cc:e5:2b:15:db:f7:d3:c7:b5:38:6f:15:3b:1e:88:3d:31:0c:b4:f9:6d:66:41:b7:1b:a0:4a:b8:16";

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();

        p = Security.getProvider("SunJCE");
        System.out.println("Testing provider " + p.getName() + "...");

        KeyFactory kf;
        try {
            kf = KeyFactory.getInstance("RSA", p);
        } catch (NoSuchAlgorithmException e) {
            kf = KeyFactory.getInstance("RSA");
        }

        RSAPublicKeySpec pubSpec = new RSAPublicKeySpec(N, E);
        PublicKey publicKey = kf.generatePublic(pubSpec);

        RSAPrivateKeySpec privSpec = new RSAPrivateKeySpec(N, D);
        PrivateKey privateKey = kf.generatePrivate(privSpec);

        // blocktype 2
        testEncDec("RSA/ECB/PKCS1Padding", 96, publicKey, privateKey);
        // blocktype 1
        testEncDec("RSA/ECB/PKCS1Padding", 96, privateKey, publicKey);

        testEncDec("RSA/ECB/NoPadding", 128, publicKey, privateKey);
        testEncDec("RSA/ECB/NoPadding", 128, privateKey, publicKey);

        // expected failure, blocktype 2 random padding bytes are different
        testKat("RSA/ECB/PKCS1Padding", Cipher.ENCRYPT_MODE, publicKey, in2, out2, false);
        testKat("RSA/ECB/PKCS1Padding", Cipher.DECRYPT_MODE, privateKey, out2, in2, true);

        testKat("RSA/ECB/PKCS1Padding", Cipher.ENCRYPT_MODE, privateKey, in1, out1, true);
        testKat("RSA/ECB/PKCS1Padding", Cipher.DECRYPT_MODE, publicKey, out1, in1, true);

        testKat("RSA/ECB/NoPadding", Cipher.ENCRYPT_MODE, publicKey, rin1, rout1, true);
        testKat("RSA/ECB/NoPadding", Cipher.DECRYPT_MODE, privateKey, rout1, rin1, true);

        testKat("RSA/ECB/NoPadding", Cipher.ENCRYPT_MODE, privateKey, rin2, rout2, true);
        testKat("RSA/ECB/NoPadding", Cipher.DECRYPT_MODE, publicKey, rout2, rin2, true);

        System.out.println("Testing error cases...");
        try {
            // decrypt something not PKCS#1 formatted
            testKat("RSA/ECB/PKCS1Padding", Cipher.DECRYPT_MODE, privateKey, rout1, rin1, true);
            throw new Exception("Unexpected success");
        } catch (BadPaddingException e) {
            // ok
        }

        try {
            // decrypt with wrong key
            testKat("RSA/ECB/PKCS1Padding", Cipher.DECRYPT_MODE, privateKey, out1, in1, true);
            throw new Exception("Unexpected success");
        } catch (BadPaddingException e) {
            // ok
        }

        try {
            // encrypt data that is too long
            testKat("RSA/ECB/PKCS1Padding", Cipher.ENCRYPT_MODE, privateKey, out1, in1, true);
            throw new Exception("Unexpected success");
        } catch (IllegalBlockSizeException e) {
            // ok
        }

        long stop = System.currentTimeMillis();
        System.out.println("Done (" + (stop - start) + " ms).");
    }

}
