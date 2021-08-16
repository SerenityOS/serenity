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
 * @bug 4890078
 * @summary Basic known-answer-test for RC2 and ARCFOUR
 * @author Andreas Sterbenz
 */

import java.io.*;
import java.util.*;

import java.security.*;

import javax.crypto.*;
import javax.crypto.spec.*;

public class CipherKAT {

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

    static abstract class Test {
        abstract void run(Provider p) throws Exception;
    }

    static class CipherTest extends Test {
        private final String alg;
        private final byte[] plaintext;
        private final byte[] ciphertext;
        private final byte[] key;
        private final byte[] iv;
        CipherTest(String alg, byte[] plaintext, byte[] ciphertext, byte[] key, byte[] iv) {
            this.alg = alg;
            this.plaintext = plaintext;
            this.ciphertext = ciphertext;
            this.key = key;
            this.iv = iv;
        }
        void run(Provider p) throws Exception {
            Cipher cipher = Cipher.getInstance(alg, p);
            SecretKey keySpec = new SecretKeySpec(key, alg.split("/")[0]);
            IvParameterSpec ivSpec;
            if (iv == null) {
                ivSpec = null;
            } else {
                ivSpec = new IvParameterSpec(iv);
            }
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] enc = cipher.doFinal(plaintext);
            if (Arrays.equals(ciphertext, enc) == false) {
                System.out.println("Cipher test encryption for " + alg + " failed:");
                if (plaintext.length < 256) {
                    System.out.println("plaintext:  " + CipherKAT.toString(plaintext));
                    System.out.println("ciphertext: " + CipherKAT.toString(ciphertext));
                    System.out.println("encrypted:  " + CipherKAT.toString(enc));
                }
                System.out.println("key:        " + CipherKAT.toString(key));
                System.out.println("iv:         " + CipherKAT.toString(iv));
                throw new Exception("Cipher test encryption for " + alg + " failed");
            }
            enc = cipher.doFinal(plaintext);
            if (Arrays.equals(ciphertext, enc) == false) {
                throw new Exception("Re-encryption test failed");
            }
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] dec = cipher.doFinal(ciphertext);
            if (Arrays.equals(plaintext, dec) == false) {
                System.out.println("Cipher test decryption for " + alg + " failed:");
                if (plaintext.length < 256) {
                    System.out.println("plaintext:  " + CipherKAT.toString(plaintext));
                    System.out.println("ciphertext: " + CipherKAT.toString(ciphertext));
                    System.out.println("decrypted:  " + CipherKAT.toString(dec));
                }
                System.out.println("key:        " + CipherKAT.toString(key));
                System.out.println("iv:         " + CipherKAT.toString(iv));
                throw new Exception("Cipher test decryption for " + alg + " failed");
            }
            System.out.println("passed: " + alg);
        }
    }

    private static byte[] s(String s) {
        try {
            return s.getBytes("UTF8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Test t(String alg, String plaintext, String ciphertext, String key) {
        return new CipherTest(alg, b(plaintext), b(ciphertext), b(key), null);
    }

    private final static byte[] ALONG;

    static {
        ALONG = new byte[1024 * 128];
        Arrays.fill(ALONG, (byte)'a');
    }

    private final static Test[] tests = {
        t("RC4", "00", "74", "0123456789abcdef"),
        t("RC4", "000102030405060708090a0b0c0d0e0f", "74:95:c0:e4:14:4e:0e:7e:05:42:df:58:3e:82:10:f3", "0123456789abcdef"),
        t("RC4", "000102030405060708090a0b0c0d0e0f", "1b:34:61:29:05:0d:73:db:25:d0:dd:64:06:29:f6:8a", "0123456789"), // 40 bit
        t("RC4", "000102030405060708090a0b0c0d0e0f", "ad:79:9d:98:d6:38:b1:f8:05:96:5c:e6:75:e7:82:24", "0123456789abcdeffedcba9876543210"), // 128 bit
        t("RC4", "74:95:c0:e4:14:4e:0e:7e:05:42:df:58:3e:82:10:f3 1b:34:61:29:05:0d:73:db:25:d0:dd:64:06:29:f6:8a ad:79:9d:98:d6:38:b1:f8:05:96:5c:e6:75:e7:82:24 ad:79:9d:98:d6:38:b1:f8:05:96:5c:e6:75:e7:82:24",
                 "e1:15:95:fc:79:da:5e:a4:e4:2a:a8:ce:cd:7d:1e:f3:f7:ef:19:a5:05:70:9f:f7:59:49:44:d6:fb:5a:b6:54:04:4d:f8:e6:60:a3:96:7d:40:33:09:78:f2:2e:de:25:fe:ad:54:dd:b9:65:97:d6:d4:4c:a8:a6:f2:2c:13:61",
                 "1b:34:61:29:05:0d:73:db:25:d0:dd:64:06:29:f6:8a"),
        t("RC2/ECB/NoPadding", "00:00:00:00:00:00:00:00", "81:07:71:4F:0D:81:88:A7",
                "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00"),
        t("RC2/ECB/NoPadding", "00:00:00:00:00:00:00:00", "39:88:F7:B8:6C:14:D3:F8",
                "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:01"),
        t("RC2/ECB/NoPadding", "FF:FF:FF:FF:FF:FF:FF:FF", "BA:B8:12:73:3E:2E:B7:EE",
                "00:00:00:00:00:00:00:00:00:00:00:00:00:00:00:00"),
        t("RC2/ECB/NoPadding", "00:00:00:00:00:00:00:00", "9C:4B:FE:6D:FE:73:9C:2B",
                "00:01:02:03:04:05:06:07:08:09:0A:0B:0C:0D:0E:0F"),
        t("RC2/ECB/NoPadding", "9C:4B:FE:6D:FE:73:9C:2B", "1d:b2:cb:09:7f:e8:ab:43",
                "39:88:F7:B8:6C:14:D3:F8:BA:B8:12:73:3E:2E:B7:EE"),
        t("RC2/ECB/NoPadding", "BA:B8:12:73:3E:2E:B7:EE", "14:e5:5b:62:ca:f5:16:24",
                "1d:b2:cb:09:7f:e8:ab:43"),
    };

    static void runTests(Test[] tests) throws Exception {
        long start = System.currentTimeMillis();
        Provider p = Security.getProvider("SunJCE");
        System.out.println("Testing provider " + p.getName() + "...");
        Cipher.getInstance("RC2", p);
        Cipher.getInstance("RC4", p);
        Cipher.getInstance("ARCFOUR", p);
        KeyGenerator.getInstance("RC2", p);
        KeyGenerator.getInstance("RC4", p);
        KeyGenerator.getInstance("ARCFOUR", p);
        for (int i = 0; i < tests.length; i++) {
            Test test = tests[i];
            test.run(p);
        }
        System.out.println("All tests passed");
        long stop = System.currentTimeMillis();
        System.out.println("Done (" + (stop - start) + " ms).");
    }

    public static void main(String[] args) throws Exception {
        runTests(tests);
    }

}
