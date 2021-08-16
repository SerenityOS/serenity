/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4856966
 * @summary Basic known-answer-test for all our MessageDigest algorithms
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm DigestKAT
 * @run main/othervm -Djava.security.manager=allow DigestKAT sm
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.Provider;
import java.util.Arrays;

public class DigestKAT extends PKCS11Test {

    private final static char[] hexDigits = "0123456789abcdef".toCharArray();

    public static String toString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 3);
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

    static class DigestTest extends Test {
        private final String alg;
        private final byte[] data;
        private final byte[] digest;
        DigestTest(String alg, byte[] data, byte[] digest) {
            this.alg = alg;
            this.data = data;
            this.digest = digest;
        }
        @Override
        void run(Provider p) throws Exception {
            if (p.getService("MessageDigest", alg) == null) {
                System.out.println("Skipped " + alg);
                return;
            }
            MessageDigest md = MessageDigest.getInstance(alg, p);
            md.update(data);
            byte[] myDigest = md.digest();
            if (Arrays.equals(digest, myDigest) == false) {
                System.out.println("Digest test for " + alg + " failed:");
                if (data.length < 256) {
                    System.out.println("data: " + DigestKAT.toString(data));
                }
                System.out.println("dig:  " + DigestKAT.toString(digest));
                System.out.println("out:  " + DigestKAT.toString(myDigest));
                throw new Exception("Digest test for " + alg + " failed");
            }
        }
    }

    private static byte[] s(String s) {
        try {
            return s.getBytes("UTF8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Test t(String alg, byte[] data, String digest) {
        return new DigestTest(alg, data, parse(digest));
    }

    private final static byte[] ALONG;

    static {
        ALONG = new byte[1024 * 128];
        Arrays.fill(ALONG, (byte)'a');
    }

    private final static Test[] tests = {
        t("MD2", s(""), "8350e5a3e24c153df2275c9f80692773"),
        t("MD2", s("a"), "32ec01ec4a6dac72c0ab96fb34c0b5d1"),
        t("MD2", s("abc"), "da853b0d3f88d99b30283a69e6ded6bb"),
        t("MD2", s("message digest"), "ab4f496bfb2a530b219ff33031fe06b0"),
        t("MD2", s("abcdefghijklmnopqrstuvwxyz"), "4e8ddff3650292ab5a4108c3aa47940b"),
        t("MD2", s("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"), "da33def2a42df13975352846c30338cd"),
        t("MD2", s("12345678901234567890123456789012345678901234567890123456789012345678901234567890"), "d5976f79d83d3a0dc9806c3c66f3efd8"),
        t("MD2", ALONG, "92:e3:15:b4:d5:35:6f:55:45:e0:08:37:dc:8f:6e:e3"),

        t("MD5", s(""), "d41d8cd98f00b204e9800998ecf8427e"),
        t("MD5", s("a"), "0cc175b9c0f1b6a831c399e269772661"),
        t("MD5", s("abc"), "900150983cd24fb0d6963f7d28e17f72"),
        t("MD5", s("message digest"), "f96b697d7cb7938d525a2f31aaf161d0"),
        t("MD5", s("abcdefghijklmnopqrstuvwxyz"), "c3fcd3d76192e4007dfb496cca67e13b"),
        t("MD5", s("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"), "d174ab98d277d9f5a5611c2c9f419d9f"),
        t("MD5", s("12345678901234567890123456789012345678901234567890123456789012345678901234567890"), "57edf4a22be3c955ac49da2e2107b67a"),
        t("MD5", ALONG, "81:61:54:49:a9:8a:aa:ad:8d:c1:79:b3:be:c8:7f:38"),

        t("SHA1", s(""), "da:39:a3:ee:5e:6b:4b:0d:32:55:bf:ef:95:60:18:90:af:d8:07:09"),
        t("SHA1", s("a"), "86:f7:e4:37:fa:a5:a7:fc:e1:5d:1d:dc:b9:ea:ea:ea:37:76:67:b8"),
        t("SHA1", s("abc"), "a9:99:3e:36:47:06:81:6a:ba:3e:25:71:78:50:c2:6c:9c:d0:d8:9d"),
        t("SHA1", s("message digest"), "c1:22:52:ce:da:8b:e8:99:4d:5f:a0:29:0a:47:23:1c:1d:16:aa:e3"),
        t("SHA1", s("abcdefghijklmnopqrstuvwxyz"), "32:d1:0c:7b:8c:f9:65:70:ca:04:ce:37:f2:a1:9d:84:24:0d:3a:89"),
        t("SHA1", s("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"), "76:1c:45:7b:f7:3b:14:d2:7e:9e:92:65:c4:6f:4b:4d:da:11:f9:40"),
        t("SHA1", s("12345678901234567890123456789012345678901234567890123456789012345678901234567890"), "50:ab:f5:70:6a:15:09:90:a0:8b:2c:5e:a4:0f:a0:e5:85:55:47:32"),
        t("SHA1", ALONG, "ce:56:53:59:08:04:ba:a9:36:9f:72:d4:83:ed:9e:ba:72:f0:4d:29"),

        t("SHA-224", s(""), "d1:4a:02:8c:2a:3a:2b:c9:47:61:02:bb:28:82:34:c4:15:a2:b0:1f:82:8e:a6:2a:c5:b3:e4:2f"),
        t("SHA-224", s("abc"), "23:09:7d:22:34:05:d8:22:86:42:a4:77:bd:a2:55:b3:2a:ad:bc:e4:bd:a0:b3:f7:e3:6c:9d:a7"),
        t("SHA-224", s("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"), "75:38:8b:16:51:27:76:cc:5d:ba:5d:a1:fd:89:01:50:b0:c6:45:5c:b4:f5:8b:19:52:52:25:25"),
        t("SHA-224", s("The quick brown fox jumps over the lazy dog"), "73:0e:10:9b:d7:a8:a3:2b:1c:b9:d9:a0:9a:a2:32:5d:24:30:58:7d:db:c0:c3:8b:ad:91:15:25"),
        t("SHA-224", s("The quick brown fox jumps over the lazy dog."), "61:9c:ba:8e:8e:05:82:6e:9b:8c:51:9c:0a:5c:68:f4:fb:65:3e:8a:3d:8a:a0:4b:b2:c8:cd:4c"),

        t("SHA-256", s(""), "e3:b0:c4:42:98:fc:1c:14:9a:fb:f4:c8:99:6f:b9:24:27:ae:41:e4:64:9b:93:4c:a4:95:99:1b:78:52:b8:55"),
        t("SHA-256", s("a"), "ca:97:81:12:ca:1b:bd:ca:fa:c2:31:b3:9a:23:dc:4d:a7:86:ef:f8:14:7c:4e:72:b9:80:77:85:af:ee:48:bb"),
        t("SHA-256", s("abc"), "ba:78:16:bf:8f:01:cf:ea:41:41:40:de:5d:ae:22:23:b0:03:61:a3:96:17:7a:9c:b4:10:ff:61:f2:00:15:ad"),
        t("SHA-256", s("message digest"), "f7:84:6f:55:cf:23:e1:4e:eb:ea:b5:b4:e1:55:0c:ad:5b:50:9e:33:48:fb:c4:ef:a3:a1:41:3d:39:3c:b6:50"),
        t("SHA-256", s("abcdefghijklmnopqrstuvwxyz"), "71:c4:80:df:93:d6:ae:2f:1e:fa:d1:44:7c:66:c9:52:5e:31:62:18:cf:51:fc:8d:9e:d8:32:f2:da:f1:8b:73"),
        t("SHA-256", s("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"), "db:4b:fc:bd:4d:a0:cd:85:a6:0c:3c:37:d3:fb:d8:80:5c:77:f1:5f:c6:b1:fd:fe:61:4e:e0:a7:c8:fd:b4:c0"),
        t("SHA-256", s("12345678901234567890123456789012345678901234567890123456789012345678901234567890"), "f3:71:bc:4a:31:1f:2b:00:9e:ef:95:2d:d8:3c:a8:0e:2b:60:02:6c:8e:93:55:92:d0:f9:c3:08:45:3c:81:3e"),
        t("SHA-256", ALONG, "b4:4f:fb:72:fc:c2:59:67:6b:d8:04:95:fe:f1:b4:4b:80:8c:a8:f1:ff:e1:b1:70:6a:4d:79:11:b0:e3:1f:11"),

        t("SHA-384", s(""), "38:b0:60:a7:51:ac:96:38:4c:d9:32:7e:b1:b1:e3:6a:21:fd:b7:11:14:be:07:43:4c:0c:c7:bf:63:f6:e1:da:27:4e:de:bf:e7:6f:65:fb:d5:1a:d2:f1:48:98:b9:5b"),
        t("SHA-384", s("a"), "54:a5:9b:9f:22:b0:b8:08:80:d8:42:7e:54:8b:7c:23:ab:d8:73:48:6e:1f:03:5d:ce:9c:d6:97:e8:51:75:03:3c:aa:88:e6:d5:7b:c3:5e:fa:e0:b5:af:d3:14:5f:31"),
        t("SHA-384", s("abc"), "cb:00:75:3f:45:a3:5e:8b:b5:a0:3d:69:9a:c6:50:07:27:2c:32:ab:0e:de:d1:63:1a:8b:60:5a:43:ff:5b:ed:80:86:07:2b:a1:e7:cc:23:58:ba:ec:a1:34:c8:25:a7"),
        t("SHA-384", s("message digest"), "47:3e:d3:51:67:ec:1f:5d:8e:55:03:68:a3:db:39:be:54:63:9f:82:88:68:e9:45:4c:23:9f:c8:b5:2e:3c:61:db:d0:d8:b4:de:13:90:c2:56:dc:bb:5d:5f:d9:9c:d5"),
        t("SHA-384", s("abcdefghijklmnopqrstuvwxyz"), "fe:b6:73:49:df:3d:b6:f5:92:48:15:d6:c3:dc:13:3f:09:18:09:21:37:31:fe:5c:7b:5f:49:99:e4:63:47:9f:f2:87:7f:5f:29:36:fa:63:bb:43:78:4b:12:f3:eb:b4"),
        t("SHA-384", s("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"), "17:61:33:6e:3f:7c:bf:e5:1d:eb:13:7f:02:6f:89:e0:1a:44:8e:3b:1f:af:a6:40:39:c1:46:4e:e8:73:2f:11:a5:34:1a:6f:41:e0:c2:02:29:47:36:ed:64:db:1a:84"),
        t("SHA-384", s("12345678901234567890123456789012345678901234567890123456789012345678901234567890"), "b1:29:32:b0:62:7d:1c:06:09:42:f5:44:77:64:15:56:55:bd:4d:a0:c9:af:a6:dd:9b:9e:f5:31:29:af:1b:8f:b0:19:59:96:d2:de:9c:a0:df:9d:82:1f:fe:e6:70:26"),
        t("SHA-384", ALONG, "ba:25:a4:25:1a:6c:69:f5:d7:4a:b2:b1:08:be:35:fb:6c:db:35:df:4f:17:c8:08:e7:cc:a2:cb:de:e7:7a:13:4b:35:90:50:10:bf:b9:26:78:65:cf:79:2e:09:79:1c"),

        t("SHA-512", s(""), "cf:83:e1:35:7e:ef:b8:bd:f1:54:28:50:d6:6d:80:07:d6:20:e4:05:0b:57:15:dc:83:f4:a9:21:d3:6c:e9:ce:47:d0:d1:3c:5d:85:f2:b0:ff:83:18:d2:87:7e:ec:2f:63:b9:31:bd:47:41:7a:81:a5:38:32:7a:f9:27:da:3e"),
        t("SHA-512", s("a"), "1f:40:fc:92:da:24:16:94:75:09:79:ee:6c:f5:82:f2:d5:d7:d2:8e:18:33:5d:e0:5a:bc:54:d0:56:0e:0f:53:02:86:0c:65:2b:f0:8d:56:02:52:aa:5e:74:21:05:46:f3:69:fb:bb:ce:8c:12:cf:c7:95:7b:26:52:fe:9a:75"),
        t("SHA-512", s("abc"), "dd:af:35:a1:93:61:7a:ba:cc:41:73:49:ae:20:41:31:12:e6:fa:4e:89:a9:7e:a2:0a:9e:ee:e6:4b:55:d3:9a:21:92:99:2a:27:4f:c1:a8:36:ba:3c:23:a3:fe:eb:bd:45:4d:44:23:64:3c:e8:0e:2a:9a:c9:4f:a5:4c:a4:9f"),
        t("SHA-512", s("message digest"), "10:7d:bf:38:9d:9e:9f:71:a3:a9:5f:6c:05:5b:92:51:bc:52:68:c2:be:16:d6:c1:34:92:ea:45:b0:19:9f:33:09:e1:64:55:ab:1e:96:11:8e:8a:90:5d:55:97:b7:20:38:dd:b3:72:a8:98:26:04:6d:e6:66:87:bb:42:0e:7c"),
        t("SHA-512", s("abcdefghijklmnopqrstuvwxyz"), "4d:bf:f8:6c:c2:ca:1b:ae:1e:16:46:8a:05:cb:98:81:c9:7f:17:53:bc:e3:61:90:34:89:8f:aa:1a:ab:e4:29:95:5a:1b:f8:ec:48:3d:74:21:fe:3c:16:46:61:3a:59:ed:54:41:fb:0f:32:13:89:f7:7f:48:a8:79:c7:b1:f1"),
        t("SHA-512", s("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"), "1e:07:be:23:c2:6a:86:ea:37:ea:81:0c:8e:c7:80:93:52:51:5a:97:0e:92:53:c2:6f:53:6c:fc:7a:99:96:c4:5c:83:70:58:3e:0a:78:fa:4a:90:04:1d:71:a4:ce:ab:74:23:f1:9c:71:b9:d5:a3:e0:12:49:f0:be:bd:58:94"),
        t("SHA-512", s("12345678901234567890123456789012345678901234567890123456789012345678901234567890"), "72:ec:1e:f1:12:4a:45:b0:47:e8:b7:c7:5a:93:21:95:13:5b:b6:1d:e2:4e:c0:d1:91:40:42:24:6e:0a:ec:3a:23:54:e0:93:d7:6f:30:48:b4:56:76:43:46:90:0c:b1:30:d2:a4:fd:5d:d1:6a:bb:5e:30:bc:b8:50:de:e8:43"),
        t("SHA-512", ALONG, "20:ef:14:68:87:86:cd:1e:1f:ba:e1:0f:6f:83:f8:f0:66:d0:56:3c:94:f6:b4:e5:f4:7e:07:64:60:d0:70:c4:2e:3f:cc:82:4c:a2:ed:35:eb:11:10:7b:ee:70:82:01:99:b6:0c:37:af:12:d0:0f:17:36:b8:56:f4:1f:f7:bf"),

    };

    static void runTests(Test[] tests, Provider p) throws Exception {
        long start = System.currentTimeMillis();
        System.out.println("Testing provider " + p.getName() + "...");
        for (int i = 0; i < tests.length; i++) {
            Test test = tests[i];
            test.run(p);
        }
        System.out.println("All tests passed");
        long stop = System.currentTimeMillis();
        System.out.println("Done (" + (stop - start) + " ms).");
    }

    @Override
    public void main(Provider p) throws Exception{
        runTests(tests, p);
    }

    public static void main(String[] args) throws Exception {
        main(new DigestKAT(), args);
    }

}
