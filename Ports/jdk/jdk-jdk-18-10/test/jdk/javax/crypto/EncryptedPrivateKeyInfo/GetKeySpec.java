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
 * @bug 4508341
 * @summary Test the EncryptedPrivateKeyInfo.getKeySpec(...) methods.
 * @author Valerie Peng
 */
import java.util.*;
import java.nio.*;
import java.io.*;
import java.security.*;
import java.util.Arrays;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class GetKeySpec {
    private static final String cipherAlg = "PBEWithMD5AndDES";
    private static final char[] passwd = { 'p','a','s','s','w','d' };
    private static AlgorithmParameters GOOD_PARAMS;

    static {
        try {
            PBEParameterSpec goodParamSpec =
                new PBEParameterSpec(new byte[8], 1024);
            GOOD_PARAMS = AlgorithmParameters.getInstance
                (cipherAlg, "SunJCE");
            GOOD_PARAMS.init(goodParamSpec);
        } catch (Exception ex) {
            // should never happen
            GOOD_PARAMS = null;
        }
    }

    private static String pkcs8Encoded = "30:82:01:53:02:01:00:30:0D:06:09:2A:86:48:86:F7:0D:01:01:01:05:00:04:82:01:3D:30:82:01:39:02:01:00:02:40:6E:A4:13:65:97:A2:C2:47:5E:F2:23:6B:94:D8:D7:25:13:BB:A4:AE:8A:AA:A7:27:A4:9A:04:DC:15:F7:9B:E4:39:18:99:9E:27:EA:92:BB:D0:0E:F3:26:F4:95:89:33:02:65:6D:84:69:2C:CE:B7:FA:68:8E:FE:8D:63:44:6B:02:03:01:00:01:02:40:59:6E:1C:13:98:FE:C1:04:89:75:35:36:27:29:22:B5:E0:7E:62:BD:86:6E:2C:10:7A:16:D8:68:C1:04:D4:A7:10:41:F7:B9:B4:84:05:03:A5:C0:28:73:24:A7:24:F1:1B:C3:4F:BF:05:20:D0:D9:00:08:7F:C3:29:64:1B:29:02:21:00:C4:63:4D:0C:32:51:44:AE:DD:90:A9:B7:B6:C2:6B:11:BE:D2:07:E7:B5:C2:4A:9F:4D:0F:2F:30:5F:E6:1C:6D:02:21:00:90:39:A4:2D:93:0B:08:AF:2F:6F:18:CC:1A:EF:B6:E6:01:E7:21:3A:7F:45:C7:3F:39:12:B8:CC:DF:44:2D:37:02:21:00:B3:9B:61:9E:B2:F2:12:4F:9E:C1:2C:06:A1:B5:A3:38:62:7D:31:CF:9F:32:67:0E:D3:E9:FC:2D:50:B7:61:ED:02:20:5B:FD:77:FB:5D:A3:97:09:6E:1E:D5:59:32:01:1D:CE:7C:FE:38:12:80:A5:38:1D:DA:40:57:C0:CC:D3:46:67:02:20:52:EC:61:05:0D:EC:8A:ED:F7:1E:95:67:D0:7C:8B:D9:AA:A5:33:B8:26:26:2E:8F:D7:A7:18:16:2A:83:63:5C";
    private static String encryptedPKCS8 = "AE:20:81:4F:4D:38:73:C0:51:70:42:DA:C2:EF:61:49:07:E9:B5:D5:55:6D:D1:50:54:B2:0B:41:3E:2F:B6:00:BC:30:89:7B:32:A5:5F:B6:86:92:9E:06:6E:E2:40:8E:3E:E8:0B:CA:97:DB:3E:72:3E:03:22:34:35:EA:5F:B0:71:B2:07:BC:0D:97:94:0A:E6:12:9B:60:7A:77:D4:6C:99:60:2E:68:D6:55:BE:83:B8:A9:0F:19:8A:BE:91:30:D0:FE:52:94:5A:4C:D7:24:07:B3:61:EB:B5:4A:C6:6F:96:8A:C0:20:E9:73:40:FA:A2:56:04:F2:43:35:90:EA:35:C9:8C:08:9D:0B:BC:37:F0:01:D5:DF:BE:E4:4A:57:E0:13:0C:D5:F0:E8:5C:3B:B3:CD:7E:B5:E8:A5:84:63:F6:DA:3E:F2:CF:53:1F:A2:86:44:61:DD:AF:C1:78:70:3A:E6:06:41:77:6C:5B:8D:FA:C4:39:D7:4D:2F:87:D8:31:F4:B6:2B:94:D9:87:17:0E:C8:E3:FA:54:C8:B2:44:56:E0:37:5F:4C:5D:B2:21:DD:15:9E:94:63:89:CF:07:8C:79:F8:65:B2:22:45:D5:F0:2A:70:19:61:16:1D:52:5E:0C:35:3B:20:88:17:7E:FD:05:CC:08:09:2F:05:61:F7:A8:F5:EA:DE:77:DE:5D:55:4E:A0:36:A1:13:FF:2D:57:E8:4E:06:CE:C9:C1:B1:AE:C6:52:A6:EB:35:4C:81:91:DE:71:BA:34:DA:8A:99:1A:47:2E:66:52:AF:E3:2A:E4:0A:27:7F:72:C4:90:7E:8D:8F:64:8D:21:7E:00:DC:1C:62:0F:CC:96:80:C7:E5:5B:70:48:A5:E7:34:27:1A:7C:48:A7:9E:8B:2B:A6:E2";

    private static byte[] parse(String s) {
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

    public static void main(String[] argv) throws Exception {
        if (GOOD_PARAMS == null) {
            throw new Exception("Static parameter generation failed");
        }
        byte[] encodedKey = parse(pkcs8Encoded);
        byte[] encryptedData = parse(encryptedPKCS8);
        boolean result = true;

        Provider p = Security.getProvider("SunJCE");

        // generate encrypted data and EncryptedPrivateKeyInfo object
        EncryptedPrivateKeyInfo epki =
            new EncryptedPrivateKeyInfo(GOOD_PARAMS, encryptedData);

        PKCS8EncodedKeySpec pkcs8Spec;
        // TEST#1 getKeySpec(Cipher)
        System.out.println("Testing getKeySpec(Cipher)...");
        // Prepare Cipher for decryption
        PBEKeySpec pbeKeySpec = new PBEKeySpec(passwd);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(cipherAlg, p);
        SecretKey cipherKey = skf.generateSecret(pbeKeySpec);
        Cipher cipher = Cipher.getInstance(cipherAlg, p);
        cipher.init(Cipher.DECRYPT_MODE, cipherKey, GOOD_PARAMS);
        pkcs8Spec = epki.getKeySpec(cipher);
        if (Arrays.equals(pkcs8Spec.getEncoded(), encodedKey)) {
            System.out.println("passed");
        } else {
            result = false;
        }

        // TEST#2 getKeySpec(Key)
        System.out.println("Testing getKeySpec(Key)...");
        pkcs8Spec = epki.getKeySpec(cipherKey);
        if (Arrays.equals(pkcs8Spec.getEncoded(), encodedKey)) {
            System.out.println("passed");
        } else {
            result = false;
        }

        // TEST#3 getKeySpec(Key, String)
        System.out.println("Testing getKeySpec(Key, String)...");
        pkcs8Spec = epki.getKeySpec(cipherKey, p.getName());
        if (Arrays.equals(pkcs8Spec.getEncoded(), encodedKey)) {
            System.out.println("passed");
        } else {
            result = false;
        }

        // TEST#4 getKeySpec(Key, Provider)
        System.out.println("Testing getKeySpec(Key, Provider)...");
        pkcs8Spec = epki.getKeySpec(cipherKey, p);
        if (Arrays.equals(pkcs8Spec.getEncoded(), encodedKey)) {
            System.out.println("passed");
        } else {
            result = false;
        }

        if (result) {
            System.out.println("All Tests Passed");
        } else {
            throw new Exception("One or More Test Failed");
        }
    }
}
