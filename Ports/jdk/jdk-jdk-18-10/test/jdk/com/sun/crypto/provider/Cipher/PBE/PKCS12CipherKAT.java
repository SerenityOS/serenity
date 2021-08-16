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
 * @bug 4893959
 * @summary basic test for PBEWithSHA1AndDESede and
 * PBEWithSHA1AndRC2_40
 * @author Valerie Peng
 */

import java.io.*;
import java.util.*;

import java.security.*;

import javax.crypto.*;
import javax.crypto.spec.*;

public class PKCS12CipherKAT {

    private final static String INPUT = "12:34:56:78:90:ab:cd:ef:ab:cd:ef:12:34:56:78:90:fe:db:ca:09:87:65";
    private final static String SALT = "7d:60:43:5f:02:e9:e0:ae";
    private final static int ITER_COUNT = 2048;

    private final static char[] hexDigits = "0123456789abcdef".toCharArray();

    public static byte[] parse(String s) {
        try {
            int n = s.length();
            ByteArrayOutputStream out = new ByteArrayOutputStream(n/3);
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
        private final char[] password;
        private final byte[] salt;
        private final int ic;

        CipherTest(String alg, byte[] plaintext, byte[] ciphertext,
                   char[] password, byte[] salt, int ic) {
            this.alg = alg;
            this.plaintext = plaintext;
            this.ciphertext = ciphertext;
            this.password = password;
            this.salt = salt;
            this.ic = ic;
        }

        String hexDump(byte[] b) {
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

        void run(Provider p) throws Exception {
            Cipher cipher = Cipher.getInstance(alg, p);
            PBEKeySpec pbeKeySpec = new PBEKeySpec(password);
            SecretKeyFactory keyFac = SecretKeyFactory.getInstance("PBE", p);
            PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, ic);
            SecretKey key = keyFac.generateSecret(pbeKeySpec);
            cipher.init(Cipher.ENCRYPT_MODE, key, pbeParamSpec);
            byte[] enc = cipher.doFinal(plaintext);
            if (Arrays.equals(ciphertext, enc) == false) {
                System.out.println(
                    "Cipher test encryption for " + alg + " failed:");
                System.out.println("plaintext:  " + hexDump(plaintext));
                System.out.println("ciphertext: " + hexDump(ciphertext));
                System.out.println("encrypted:  " + hexDump(enc));
                System.out.println("password:   " + password);
                System.out.println("salt:       " + hexDump(salt));
                System.out.println("iterationCount: " + ic);
                throw new Exception("encryption test for " + alg + " failed");
            }
            enc = cipher.doFinal(plaintext);
            if (Arrays.equals(ciphertext, enc) == false) {
                throw new Exception("Re-encryption test failed");
            }
            cipher.init(Cipher.DECRYPT_MODE, key, pbeParamSpec);
            byte[] dec = cipher.doFinal(ciphertext);
            if (Arrays.equals(plaintext, dec) == false) {
                System.out.println("plaintext:  " + hexDump(plaintext));
                System.out.println("ciphertext: " + hexDump(ciphertext));
                System.out.println("decrypted:  " + hexDump(dec));
                System.out.println("password:   " + password);
                System.out.println("salt:       " + hexDump(salt));
                System.out.println("iterationCount: " + ic);
                throw new Exception("decryption test for " + alg + " failed");
            }
            System.out.println("passed: " + alg);
        }
    }

    private static Test t(String alg, String plaintext, char[] password,
        String salt, int iterationCount, String ciphertext) {
        return new CipherTest(alg, b(plaintext), b(ciphertext), password,
            b(salt), iterationCount);
    }

    private final static char[] PASSWD = { 'p','a','s','s','w','o','r','d' };
    private final static Test[] tests = {
    t("PBEWithSHA1AndDESede", INPUT, PASSWD, SALT, ITER_COUNT,
      "95:94:49:5a:a2:cf:c9:a5:bb:21:08:23:45:41:46:a3:9c:c5:84:da:b5:04:ae:1a"),
    t("PBEWithSHA1AndRC2_40", INPUT, PASSWD, SALT, ITER_COUNT,
      "ec:32:f4:68:29:29:8b:c8:55:75:cb:ac:a4:01:d9:9c:b3:27:d6:b6:9f:26:98:f1")
    };

    static void runTests(Test[] tests) throws Exception {
        long start = System.currentTimeMillis();
        Provider p = Security.getProvider("SunJCE");
        System.out.println("Testing provider " + p.getName() + "...");
        Cipher.getInstance("PBEWithSHA1AndRC2_40", p);
        Cipher.getInstance("PBEWithSHA1AndDESede", p);
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
