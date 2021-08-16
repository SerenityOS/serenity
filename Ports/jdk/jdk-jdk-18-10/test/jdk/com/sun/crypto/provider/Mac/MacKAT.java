/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4846410 6313661 4963723 8172680
 * @summary Basic known-answer-test for Hmac and SslMac algorithms
 * @author Andreas Sterbenz
 */

import java.io.*;
import java.util.*;

import java.security.*;

import javax.crypto.*;
import javax.crypto.spec.*;

public class MacKAT {

    private final static char[] hexDigits = "0123456789abcdef".toCharArray();

    private final static char SEP = ':';

    static boolean testFailed = false;

    public static String toString(byte[] b) {
        if (b == null) {
            return "(null)";
        }
        StringBuffer sb = new StringBuffer(b.length * 3);
        for (int i = 0; i < b.length; i++) {
            int k = b[i] & 0xff;
            if (i != 0) {
                sb.append(SEP);
            }
            sb.append(hexDigits[k >>> 4]);
            sb.append(hexDigits[k & 0xf]);
        }
        return sb.toString();
    }

    public static byte[] parse(String s) {
        try {
            int n = s.length();
            ByteArrayOutputStream out;
            if (s.indexOf(SEP) == -1) {
                out = new ByteArrayOutputStream(n / 2);
            } else {
                out = new ByteArrayOutputStream(n / 3);
            }

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

    static class MacTest extends Test {
        private final String alg;
        private final byte[] input;
        private final byte[] macvalue;
        private final byte[] key;
        MacTest(String alg, byte[] input, byte[] macvalue, byte[] key) {
            this.alg = alg;
            this.input = input;
            this.macvalue = macvalue;
            this.key = key;
        }
        void run(Provider p) {
            Mac mac = null;
            try {
                mac = Mac.getInstance(alg, p);
            } catch (NoSuchAlgorithmException nsae) {
                System.out.println("Skip test for " + alg +
                    " due to lack of support");
                return;
            }
            // ensure there is matching KeyGenerator support
            if (!alg.startsWith("SslMac")) {
                try {
                    KeyGenerator.getInstance(alg, p);
                } catch (NoSuchAlgorithmException nsae) {
                    System.out.println("Test Failed due to " + nsae);
                    MacKAT.testFailed = true;
                    return;
                }
            }
            byte[] macv = null;
            try {
                SecretKey keySpec = new SecretKeySpec(key, alg);
                mac.init(keySpec);
                mac.update(input);
                macv = mac.doFinal();
            } catch (Exception e) {
                System.out.println("Unexpected ex when testing " + alg + ": ");
                e.printStackTrace();
                MacKAT.testFailed = true;
                return;
            }

            if (Arrays.equals(macvalue, macv) == false) {
                System.out.println("Mac test for " + alg + " failed:");
                if (input.length < 256) {
                    System.out.println("input:       " + toString(input));
                }
                System.out.println("key:        " + toString(key));
                System.out.println("macvalue:   " + toString(macvalue));
                System.out.println("calculated: " + toString(macv));
                MacKAT.testFailed = true;
            } else {
                System.out.println("passed: " + alg);
            }
        }
        private static String toString(byte[] b) {
            return MacKAT.toString(b);
        }
    }

    private static byte[] s(String s) {
        try {
            return s.getBytes("UTF8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Test t(String alg, String input, String macvalue, String key) {
        return new MacTest(alg, b(input), b(macvalue), b(key));
    }
    private static Test t(String alg, String input, String macvalue, byte[] key) {
        return new MacTest(alg, b(input), b(macvalue), key);
    }
    private static Test t(String alg, byte[] input, String macvalue, String key) {
        return new MacTest(alg, input, b(macvalue), b(key));
    }

    private static Test t(String alg, byte[] input, String macvalue, byte[] key) {
        return new MacTest(alg, input, b(macvalue), key);
    }
    private final static byte[] ALONG, BLONG, DDDATA_50, CDDATA_50;
    private final static byte[] STR_HI_THERE, STR_WHAT_DO_YA_WANT;
    private final static byte[] STR_TEST_USING1, STR_TEST_USING2;
    private final static byte[] STR_NIST1, STR_NIST2, STR_NIST3;
    private final static byte[] BKEY, BKEY_20, CKEY_20;
    private final static byte[] AAKEY_20, AAKEY_131, AAKEY_147, INCKEY_25;
    private final static byte[] NISTKEY_172;

    static {
        ALONG = new byte[1024 * 128];
        Arrays.fill(ALONG, (byte)'a');
        BLONG = new byte[1024 * 128];
        Random random = new Random(12345678);
        random.nextBytes(BLONG);
        DDDATA_50 = new byte[50];
        Arrays.fill(DDDATA_50, (byte) 0xdd);
        CDDATA_50 = new byte[50];
        Arrays.fill(CDDATA_50, (byte) 0xcd);

        STR_HI_THERE = s("Hi There");
        STR_WHAT_DO_YA_WANT = s("what do ya want for nothing?");
        STR_TEST_USING1 = s("Test Using Larger Than Block-Size Key - Hash Key First");
        STR_TEST_USING2 = s("This is a test using a larger than block-size key and a larger than block-size data. The key needs to be hashed before being used by the HMAC algorithm.");

        STR_NIST1 = s("Sample message for keylen<blocklen");
        STR_NIST2 = s("Sample message for keylen=blocklen");
        STR_NIST3 = s("Sample message for keylen>blocklen");

        BKEY = new byte[128];
        random.nextBytes(BKEY);
        BKEY_20 = new byte[20];
        Arrays.fill(BKEY_20, (byte) 0x0b);
        CKEY_20 = new byte[20];
        Arrays.fill(CKEY_20, (byte) 0x0c);
        AAKEY_20 = new byte[20];
        Arrays.fill(AAKEY_20, (byte) 0xaa);
        AAKEY_131 = new byte[131];
        Arrays.fill(AAKEY_131, (byte) 0xaa);
        AAKEY_147 = new byte[147];
        Arrays.fill(AAKEY_147, (byte) 0xaa);
        INCKEY_25 = new byte[25];
        for (int i = 0; i < INCKEY_25.length; i++) {
            INCKEY_25[i] = (byte)(i+1);
        }
        NISTKEY_172 = new byte[172];
        for (int i = 0; i < NISTKEY_172.length; i++) {
            NISTKEY_172[i] = (byte)i;
        }

    }

    private final static Test[] tests = {
        t("SslMacMD5", ALONG, "f4:ad:01:71:51:f6:89:56:72:a3:32:bf:d9:2a:f2:a5",
                "1b:34:61:29:05:0d:73:db:25:d0:dd:64:06:29:f6:8a"),
        t("SslMacMD5", BLONG, "34:1c:ad:a0:95:57:32:f8:8e:80:8f:ee:b2:d8:23:e5",
                "76:00:4a:72:98:9b:65:ec:2e:f1:43:c4:65:4a:13:71"),
        t("SslMacSHA1", ALONG, "11:c1:71:2e:61:be:4b:cf:bc:6d:e2:4c:58:ae:27:30:0b:24:a4:87",
                "23:ae:dd:61:87:6c:7a:45:47:2f:2c:8f:ea:64:99:3e:27:5f:97:a5"),
        t("SslMacSHA1", BLONG, "84:af:57:0a:af:ef:16:93:90:50:da:88:f8:ad:1a:c5:66:6c:94:d0",
                "9b:bb:e2:aa:9b:28:1c:95:0e:ea:30:21:98:a5:7e:31:9e:bf:5f:51"),

        t("HmacMD5", ALONG, "76:00:4a:72:98:9b:65:ec:2e:f1:43:c4:65:4a:13:71",
                "1b:34:61:29:05:0d:73:db:25:d0:dd:64:06:29:f6:8a"),
        t("HmacMD5", BLONG, "6c:22:79:bb:34:9e:da:f4:f5:cf:df:0c:62:3d:59:e0",
                "76:00:4a:72:98:9b:65:ec:2e:f1:43:c4:65:4a:13:71"),
        t("HmacMD5", BLONG, "e6:ad:00:c9:49:6b:98:fe:53:a2:b9:2d:7d:41:a2:03",
                BKEY),
        t("HmacSHA1", ALONG, "9e:b3:6e:35:fa:fb:17:2e:2b:f3:b0:4a:9d:38:83:c4:5f:6d:d9:00",
                "1b:34:61:29:05:0d:73:db:25:d0:dd:64:06:29:f6:8a"),
        t("HmacSHA1", BLONG, "80:2d:5b:ea:08:df:a4:1f:e5:3e:1c:fa:fc:ad:dd:31:da:15:60:2c",
                "76:00:4a:72:98:9b:65:ec:2e:f1:43:c4:65:4a:13:71"),
        t("HmacSHA1", BLONG, "a2:fa:2a:85:18:0e:94:b2:a5:e2:17:8b:2a:29:7a:95:cd:e8:aa:82",
                BKEY),

        t("HmacSHA256", ALONG, "3f:6d:08:df:0c:90:b0:e9:ed:13:4a:2e:c3:48:1d:3d:3e:61:2e:f1:30:c2:63:c4:58:57:03:c2:cb:87:15:07",
                "1b:34:61:29:05:0d:73:db:25:d0:dd:64:06:29:f6:8a"),
        t("HmacSHA256", BLONG, "e2:4e:a3:b9:0b:b8:99:e4:71:cf:ca:9f:f8:4e:f0:34:8b:19:9f:33:4b:1a:b7:13:f7:c8:57:92:e3:03:74:78",
                BKEY),
        t("HmacSHA384", ALONG, "d0:f0:d4:54:1c:0a:6d:81:ed:15:20:d7:0c:96:06:61:a0:ff:c9:ff:91:e9:a0:cd:e2:45:64:9d:93:4c:a9:fa:89:ae:c0:90:e6:0b:a1:a0:56:80:57:3b:ed:4b:b0:71",
                "1b:34:61:29:05:0d:73:db:25:d0:dd:64:06:29:f6:8a"),
        t("HmacSHA384", BLONG, "75:c4:ca:c7:f7:58:9d:d3:23:b1:1b:5c:93:2d:ec:7a:03:dc:8c:eb:8d:fe:79:46:4f:30:e7:99:62:de:44:e2:38:95:0e:79:91:78:2f:a4:05:0a:f0:17:10:38:a1:8e",
                BKEY),
        t("HmacSHA512", ALONG, "41:ea:4c:e5:31:3f:7c:18:0e:5e:95:a9:25:0a:10:58:e6:40:53:88:82:4f:5a:da:6f:29:de:04:7b:8e:d7:ed:7c:4d:b8:2a:48:2d:17:2a:2d:59:bb:81:9c:bf:33:40:04:77:44:fb:45:25:1f:fd:b9:29:f4:a6:69:a3:43:6f",
                "1b:34:61:29:05:0d:73:db:25:d0:dd:64:06:29:f6:8a"),
        t("HmacSHA512", BLONG, "fb:cf:4b:c6:d5:49:5a:5b:0b:d9:2a:32:f5:fa:68:d2:68:a4:0f:ae:53:fc:49:12:e6:1d:53:cf:b2:cb:c5:c5:f2:2d:86:bd:14:61:30:c3:a6:6f:44:1f:77:9b:aa:a1:22:48:a9:dd:d0:45:86:d1:a1:82:53:13:c4:03:06:a3",
                BKEY),
        // Test vectors From RFC4231
        // Test Case 1
        t("HmacSHA224", STR_HI_THERE, "896fb1128abbdf196832107cd49df33f47b4b1169912ba4f53684b22", BKEY_20),
        t("HmacSHA256", STR_HI_THERE, "b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7", BKEY_20),
        t("HmacSHA384", STR_HI_THERE, "afd03944d84895626b0825f4ab46907f15f9dadbe4101ec682aa034c7cebc59cfaea9ea9076ede7f4af152e8b2fa9cb6", BKEY_20),
        t("HmacSHA512", STR_HI_THERE, "87aa7cdea5ef619d4ff0b4241a1d6cb02379f4e2ce4ec2787ad0b30545e17cdedaa833b7d6b8a702038b274eaea3f4e4be9d914eeb61f1702e696c203a126854", BKEY_20),
        // Test Case 2
        t("HmacSHA224", STR_WHAT_DO_YA_WANT, "a30e01098bc6dbbf45690f3a7e9e6d0f8bbea2a39e6148008fd05e44", s("Jefe")),
        t("HmacSHA256", STR_WHAT_DO_YA_WANT, "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843", s("Jefe")),
        t("HmacSHA384", STR_WHAT_DO_YA_WANT, "af45d2e376484031617f78d2b58a6b1b9c7ef464f5a01b47e42ec3736322445e8e2240ca5e69e2c78b3239ecfab21649", s("Jefe")),
        t("HmacSHA512", STR_WHAT_DO_YA_WANT, "164b7a7bfcf819e2e395fbe73b56e0a387bd64222e831fd610270cd7ea2505549758bf75c05a994a6d034f65f8f0e6fdcaeab1a34d4a6b4b636e070a38bce737", s("Jefe")),
        // Test Case 3
        t("HmacSHA224", DDDATA_50, "7fb3cb3588c6c1f6ffa9694d7d6ad2649365b0c1f65d69d1ec8333ea", AAKEY_20),
        t("HmacSHA256", DDDATA_50, "773ea91e36800e46854db8ebd09181a72959098b3ef8c122d9635514ced565fe", AAKEY_20),
        t("HmacSHA384", DDDATA_50, "88062608d3e6ad8a0aa2ace014c8a86f0aa635d947ac9febe83ef4e55966144b2a5ab39dc13814b94e3ab6e101a34f27", AAKEY_20),
        t("HmacSHA512", DDDATA_50, "fa73b0089d56a284efb0f0756c890be9b1b5dbdd8ee81a3655f83e33b2279d39bf3e848279a722c806b485a47e67c807b946a337bee8942674278859e13292fb", AAKEY_20),
        // Test Case 4
        t("HmacSHA224", CDDATA_50, "6c11506874013cac6a2abc1bb382627cec6a90d86efc012de7afec5a", INCKEY_25),
        t("HmacSHA256", CDDATA_50, "82558a389a443c0ea4cc819899f2083a85f0faa3e578f8077a2e3ff46729665b", INCKEY_25),
        t("HmacSHA384", CDDATA_50, "3e8a69b7783c25851933ab6290af6ca77a9981480850009cc5577c6e1f573b4e6801dd23c4a7d679ccf8a386c674cffb", INCKEY_25),
        t("HmacSHA512", CDDATA_50, "b0ba465637458c6990e5a8c5f61d4af7e576d97ff94b872de76f8050361ee3dba91ca5c11aa25eb4d679275cc5788063a5f19741120c4f2de2adebeb10a298dd", INCKEY_25),
        // skip Test Case 5 which truncates all output to 128 bits
        // Test Case 6
        t("HmacSHA224", STR_TEST_USING1, "95e9a0db962095adaebe9b2d6f0dbce2d499f112f2d2b7273fa6870e", AAKEY_131),
        t("HmacSHA256", STR_TEST_USING1, "60e431591ee0b67f0d8a26aacbf5b77f8e0bc6213728c5140546040f0ee37f54", AAKEY_131),
        t("HmacSHA384", STR_TEST_USING1, "4ece084485813e9088d2c63a041bc5b44f9ef1012a2b588f3cd11f05033ac4c60c2ef6ab4030fe8296248df163f44952", AAKEY_131),
        t("HmacSHA512", STR_TEST_USING1, "80b24263c7c1a3ebb71493c1dd7be8b49b46d1f41b4aeec1121b013783f8f3526b56d037e05f2598bd0fd2215d6a1e5295e64f73f63f0aec8b915a985d786598", AAKEY_131),
        // Test Case 7
        t("HmacSHA224", STR_TEST_USING2, "3a854166ac5d9f023f54d517d0b39dbd946770db9c2b95c9f6f565d1", AAKEY_131),
        t("HmacSHA256", STR_TEST_USING2, "9b09ffa71b942fcb27635fbcd5b0e944bfdc63644f0713938a7f51535c3a35e2", AAKEY_131),
        t("HmacSHA384", STR_TEST_USING2, "6617178e941f020d351e2f254e8fd32c602420feb0b8fb9adccebb82461e99c5a678cc31e799176d3860e6110c46523e", AAKEY_131),
        t("HmacSHA512", STR_TEST_USING2, "e37b6a775dc87dbaa4dfa9f96e5e3ffddebd71f8867289865df5a32d20cdc944b6022cac3c4982b10d5eeb55c3e4de15134676fb6de0446065c97440fa8c6a58", AAKEY_131),
        // NIST Example Values
        // https://csrc.nist.gov/projects/cryptographic-standards-and-guidelines/example-values#aMsgAuth
        t("HmacSHA3-224", STR_NIST1, "332cfd59347fdb8e576e77260be4aba2d6dc53117b3bfb52c6d18c04", Arrays.copyOf(NISTKEY_172, 28)),
        t("HmacSHA3-224", STR_NIST2, "d8b733bcf66c644a12323d564e24dcf3fc75f231f3b67968359100c7", Arrays.copyOf(NISTKEY_172, 144)),
        t("HmacSHA3-224", STR_NIST3, "078695eecc227c636ad31d063a15dd05a7e819a66ec6d8de1e193e59", NISTKEY_172),

        t("HmacSHA3-256", STR_NIST1, "4fe8e202c4f058e8dddc23d8c34e467343e23555e24fc2f025d598f558f67205", Arrays.copyOf(NISTKEY_172, 32)),
        t("HmacSHA3-256", STR_NIST2, "68b94e2e538a9be4103bebb5aa016d47961d4d1aa906061313b557f8af2c3faa", Arrays.copyOf(NISTKEY_172, 136)),
        t("HmacSHA3-256", STR_NIST3, "9bcf2c238e235c3ce88404e813bd2f3a97185ac6f238c63d6229a00b07974258", Arrays.copyOf(NISTKEY_172, 168)),

        t("HmacSHA3-384", STR_NIST1, "d588a3c51f3f2d906e8298c1199aa8ff6296218127f6b38a90b6afe2c5617725bc99987f79b22a557b6520db710b7f42", Arrays.copyOf(NISTKEY_172, 48)),
        t("HmacSHA3-384", STR_NIST2, "a27d24b592e8c8cbf6d4ce6fc5bf62d8fc98bf2d486640d9eb8099e24047837f5f3bffbe92dcce90b4ed5b1e7e44fa90", Arrays.copyOf(NISTKEY_172, 104)),
        t("HmacSHA3-384", STR_NIST3, "e5ae4c739f455279368ebf36d4f5354c95aa184c899d3870e460ebc288ef1f9470053f73f7c6da2a71bcaec38ce7d6ac", Arrays.copyOf(NISTKEY_172, 152)),

        t("HmacSHA3-512", STR_NIST1, "4efd629d6c71bf86162658f29943b1c308ce27cdfa6db0d9c3ce81763f9cbce5f7ebe9868031db1a8f8eb7b6b95e5c5e3f657a8996c86a2f6527e307f0213196", Arrays.copyOf(NISTKEY_172, 64)),
        t("HmacSHA3-512", STR_NIST2, "544e257ea2a3e5ea19a590e6a24b724ce6327757723fe2751b75bf007d80f6b360744bf1b7a88ea585f9765b47911976d3191cf83c039f5ffab0d29cc9d9b6da", Arrays.copyOf(NISTKEY_172, 72)),
        t("HmacSHA3-512", STR_NIST3, "5f464f5e5b7848e3885e49b2c385f0694985d0e38966242dc4a5fe3fea4b37d46b65ceced5dcf59438dd840bab22269f0ba7febdb9fcf74602a35666b2a32915", Arrays.copyOf(NISTKEY_172, 136)),
    };

    static void runTests(Test[] tests) throws Exception {
        long start = System.currentTimeMillis();
        Provider p = Security.getProvider("SunJCE");
        System.out.println("Testing provider " + p.getName() + "...");
        for (int i = 0; i < tests.length; i++) {
            Test test = tests[i];
            test.run(p);
        }
        long stop = System.currentTimeMillis();
        System.out.println("Done (" + (stop - start) + " ms).");

        if (!testFailed) {
            System.out.println("All tests passed");
        } else {
            throw new RuntimeException("One or more tests failed");
        }
    }

    public static void main(String[] args) throws Exception {
        runTests(tests);
    }
}
