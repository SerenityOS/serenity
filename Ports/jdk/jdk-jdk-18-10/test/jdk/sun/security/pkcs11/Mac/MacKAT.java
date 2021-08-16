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
 * @bug 4846410 6313661 4963723
 * @summary Basic known-answer-test for Hmac algorithms
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm MacKAT
 * @run main/othervm -Djava.security.manager=allow MacKAT sm
 */

import java.io.UnsupportedEncodingException;
import java.security.Provider;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class MacKAT extends PKCS11Test {

    private final static byte[] ALONG, BLONG, BKEY, BKEY_20, DDDATA_50,
            AAKEY_20, CDDATA_50, AAKEY_131;

    static {
        ALONG = new byte[1024 * 128];
        Arrays.fill(ALONG, (byte)'a');
        BLONG = new byte[1024 * 128];
        Random random = new Random(12345678);
        random.nextBytes(BLONG);
        BKEY = new byte[128];
        random.nextBytes(BKEY);
        BKEY_20 = new byte[20];
        Arrays.fill(BKEY_20, (byte) 0x0b);
        DDDATA_50 = new byte[50];
        Arrays.fill(DDDATA_50, (byte) 0xdd);
        AAKEY_20 = new byte[20];
        Arrays.fill(AAKEY_20, (byte) 0xaa);
        CDDATA_50 = new byte[50];
        Arrays.fill(CDDATA_50, (byte) 0xcd);
        AAKEY_131 = new byte[131];
        Arrays.fill(AAKEY_131, (byte) 0xaa);
    }

    private final static Test[] tests = {
        newMacTest("SslMacMD5",
                ALONG,
                "f4:ad:01:71:51:f6:89:56:72:a3:32:bf:d9:2a:f2:a5",
                "1b:34:61:29:05:0d:73:db:25:d0:dd:64:06:29:f6:8a"),
        newMacTest("SslMacMD5",
                BLONG,
                "34:1c:ad:a0:95:57:32:f8:8e:80:8f:ee:b2:d8:23:e5",
                "76:00:4a:72:98:9b:65:ec:2e:f1:43:c4:65:4a:13:71"),
        newMacTest("SslMacSHA1",
                ALONG,
                "11:c1:71:2e:61:be:4b:cf:bc:6d:e2:4c:58:ae:27:30:0b:24:a4:87",
                "23:ae:dd:61:87:6c:7a:45:47:2f:2c:8f:ea:64:99:3e:27:5f:97:a5"),
        newMacTest("SslMacSHA1",
                BLONG,
                "84:af:57:0a:af:ef:16:93:90:50:da:88:f8:ad:1a:c5:66:6c:94:d0",
                "9b:bb:e2:aa:9b:28:1c:95:0e:ea:30:21:98:a5:7e:31:9e:bf:5f:51"),
        newMacTest("HmacMD5",
                ALONG,
                "76:00:4a:72:98:9b:65:ec:2e:f1:43:c4:65:4a:13:71",
                "1b:34:61:29:05:0d:73:db:25:d0:dd:64:06:29:f6:8a"),
        newMacTest("HmacMD5",
                BLONG,
                "6c:22:79:bb:34:9e:da:f4:f5:cf:df:0c:62:3d:59:e0",
                "76:00:4a:72:98:9b:65:ec:2e:f1:43:c4:65:4a:13:71"),
        newMacTest("HmacMD5",
                BLONG,
                "e6:ad:00:c9:49:6b:98:fe:53:a2:b9:2d:7d:41:a2:03",
                BKEY),
        newMacTest("HmacSHA1",
                ALONG,
                "9e:b3:6e:35:fa:fb:17:2e:2b:f3:b0:4a:9d:38:83:c4:5f:6d:d9:00",
                "1b:34:61:29:05:0d:73:db:25:d0:dd:64:06:29:f6:8a"),
        newMacTest("HmacSHA1",
                BLONG,
                "80:2d:5b:ea:08:df:a4:1f:e5:3e:1c:fa:fc:ad:dd:31:da:15:60:2c",
                "76:00:4a:72:98:9b:65:ec:2e:f1:43:c4:65:4a:13:71"),
        newMacTest("HmacSHA1",
                BLONG,
                "a2:fa:2a:85:18:0e:94:b2:a5:e2:17:8b:2a:29:7a:95:cd:e8:aa:82",
                BKEY),
        newMacTest("HmacSHA256",
                ALONG,
                "3f:6d:08:df:0c:90:b0:e9:ed:13:4a:2e:c3:48:1d:3d:3e:61:2e:f1:"
                        + "30:c2:63:c4:58:57:03:c2:cb:87:15:07",
                "1b:34:61:29:05:0d:73:db:25:d0:dd:64:06:29:f6:8a"),
        newMacTest("HmacSHA256",
                BLONG,
                "e2:4e:a3:b9:0b:b8:99:e4:71:cf:ca:9f:f8:4e:f0:34:8b:19:9f:33:"
                        + "4b:1a:b7:13:f7:c8:57:92:e3:03:74:78",
                BKEY),
        newMacTest("HmacSHA384",
                ALONG,
                "d0:f0:d4:54:1c:0a:6d:81:ed:15:20:d7:0c:96:06:61:a0:ff:c9:ff:"
                        + "91:e9:a0:cd:e2:45:64:9d:93:4c:a9:fa:89:ae:c0:90:e6:"
                        + "0b:a1:a0:56:80:57:3b:ed:4b:b0:71",
                "1b:34:61:29:05:0d:73:db:25:d0:dd:64:06:29:f6:8a"),
        newMacTest("HmacSHA384",
                BLONG,
                "75:c4:ca:c7:f7:58:9d:d3:23:b1:1b:5c:93:2d:ec:7a:03:dc:8c:eb:"
                        + "8d:fe:79:46:4f:30:e7:99:62:de:44:e2:38:95:0e:79:91:"
                        + "78:2f:a4:05:0a:f0:17:10:38:a1:8e",
                BKEY),
        newMacTest("HmacSHA512",
                ALONG,
                "41:ea:4c:e5:31:3f:7c:18:0e:5e:95:a9:25:0a:10:58:e6:40:53:88:"
                        + "82:4f:5a:da:6f:29:de:04:7b:8e:d7:ed:7c:4d:b8:2a:48:"
                        + "2d:17:2a:2d:59:bb:81:9c:bf:33:40:04:77:44:fb:45:25:"
                        + "1f:fd:b9:29:f4:a6:69:a3:43:6f",
                "1b:34:61:29:05:0d:73:db:25:d0:dd:64:06:29:f6:8a"),
        newMacTest("HmacSHA512",
                BLONG,
                "fb:cf:4b:c6:d5:49:5a:5b:0b:d9:2a:32:f5:fa:68:d2:68:a4:0f:ae:"
                        + "53:fc:49:12:e6:1d:53:cf:b2:cb:c5:c5:f2:2d:86:bd:14:"
                        + "61:30:c3:a6:6f:44:1f:77:9b:aa:a1:22:48:a9:dd:d0:45:"
                        + "86:d1:a1:82:53:13:c4:03:06:a3",
                BKEY),

        // Test vectors From RFC 4231
        newMacTest("HmacSHA224",
                bytes("Hi There"),
                "89:6f:b1:12:8a:bb:df:19:68:32:10:7c:d4:9d:f3:3f:47:b4:b1:16:"
                        + "99:12:ba:4f:53:68:4b:22",
                BKEY_20),
        newMacTest("HmacSHA224",
                bytes("what do ya want for nothing?"),
                "a3:0e:01:09:8b:c6:db:bf:45:69:0f:3a:7e:9e:6d:0f:8b:be:a2:a3:"
                        + "9e:61:48:00:8f:d0:5e:44",
                bytes("Jefe")),
        newMacTest("HmacSHA224",
                DDDATA_50,
                "7f:b3:cb:35:88:c6:c1:f6:ff:a9:69:4d:7d:6a:d2:64:93:65:b0:c1:"
                        + "f6:5d:69:d1:ec:83:33:ea",
                AAKEY_20),
        newMacTest("HmacSHA224",
                CDDATA_50,
                "6c:11:50:68:74:01:3c:ac:6a:2a:bc:1b:b3:82:62:7c:ec:6a:90:d8:"
                        + "6e:fc:01:2d:e7:af:ec:5a",
                "01:02:03:04:05:06:07:08:09:0a:0b:0c:0d:0e:0f:10:11:12:13:14:"
                        + "15:16:17:18:19"),
        newMacTest("HmacSHA224",
                bytes("Test Using Larger Than Block-Size Key - Hash Key First"),
                "95:e9:a0:db:96:20:95:ad:ae:be:9b:2d:6f:0d:bc:e2:d4:99:f1:12:"
                        + "f2:d2:b7:27:3f:a6:87:0e",
                AAKEY_131),
        newMacTest("HmacSHA224",
                bytes("This is a test using a larger than block-size key and "
                        + "a larger than block-size data. The key needs to be "
                        + "hashed before being used by the HMAC algorithm."),
                "3a:85:41:66:ac:5d:9f:02:3f:54:d5:17:d0:b3:9d:bd:94:67:70:db:"
                        + "9c:2b:95:c9:f6:f5:65:d1",
                AAKEY_131),
    };

    public static void main(String[] args) throws Exception {
        main(new MacKAT(), args);
    }

    @Override
    public void main(Provider p) throws Exception {
        long start = System.currentTimeMillis();

        List<String> algorithms = getSupportedAlgorithms("Mac", "", p);
        for (Test test : tests) {
            if(!algorithms.contains(test.getAlg())) {
                continue;
            }
            test.run(p);
        }

        System.out.println("All tests passed");
        long stop = System.currentTimeMillis();
        System.out.println("Done (" + (stop - start) + " ms).");
    }

    private static byte[] bytes(String s) {
        try {
            return s.getBytes("UTF8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static Test newMacTest(String alg, byte[] input, String macvalue,
            String key) {
        return new MacTest(alg, input, parse(macvalue), parse(key));
    }

    private static Test newMacTest(String alg, byte[] input, String macvalue,
            byte[] key) {
        return new MacTest(alg, input, parse(macvalue), key);
    }

    interface Test {
        void run(Provider p) throws Exception;
        String getAlg();
    }

    static class MacTest implements Test {
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

        @Override
        public String getAlg() {
            return alg;
        }

        @Override
        public void run(Provider p) throws Exception {
            Mac mac = Mac.getInstance(alg, p);
            SecretKey keySpec = new SecretKeySpec(key, alg);
            mac.init(keySpec);
            mac.update(input);
            byte[] macv = mac.doFinal();
            if (Arrays.equals(macvalue, macv) == false) {
                System.out.println("Mac test for " + alg + " failed:");
                if (input.length < 256) {
                    System.out.println("input:       "
                            + PKCS11Test.toString(input));
                }
                System.out.println("key:        " + PKCS11Test.toString(key));
                System.out.println("macvalue:   "
                        + PKCS11Test.toString(macvalue));
                System.out.println("calculated: " + PKCS11Test.toString(macv));
                throw new Exception("Mac test for " + alg + " failed");
            }
            System.out.println("passed: " + alg);
        }
    }

}
