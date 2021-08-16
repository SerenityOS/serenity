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
 * @bug 4900891
 * @summary Test the EncryptedPrivateKeyInfo.getKeySpec(...)
 * methods with scenarios where the decrypted bytes are not
 * encoded correctly per PKCS#8 standard.
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

public class GetKeySpecInvalidEncoding {
    private static final String cipherAlg = "PBEWithMD5AndDES";
    private static final char[] passwd = { 'p','a','s','s', 'w', 'd' };
    private static AlgorithmParameters GOOD_PARAMS;

    static {
        try {
            PBEParameterSpec goodParamSpec =
                new PBEParameterSpec(new byte[8], 6);
            GOOD_PARAMS = AlgorithmParameters.getInstance
                (cipherAlg, "SunJCE");
            GOOD_PARAMS.init(goodParamSpec);
        } catch (Exception ex) {
            // should never happen
            GOOD_PARAMS = null;
        }
    }

    private static String encryptedPKCS8 = "5C:BC:13:5D:40:2F:02:28:94:3C:A9:F7:98:A6:58:DC:F9:12:B7:CB:0F:40:DD:66:AB:58:6B:23:2F:8A:5A:81:9D:55:2A:EB:3F:AA:6A:CE:AE:23:8A:96:12:21:5A:09:BF:59:65:3F:B8:48:59:69:C6:D0:9C:48:B6:78:C3:C6:B4:24:F9:BE:10:00:D5:F3:52:88:53:CD:07:CA:88:93:15:3F:BA:19:4A:E9:5D:C7:44:46:49:F6:19:83:86:E0:25:51:9E:83:6D:AE:F2:14:9C:BF:02:7B:8C:64:B4:5F:F1:7B:28:2F:39:55:32:A4:F5:41:85:9E:77:F2:07:09:CD:97:90:5A:04:81:23:30";

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
        byte[] encryptedData = parse(encryptedPKCS8);

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
        try {
            pkcs8Spec = epki.getKeySpec(cipher);
            throw new Exception("getKeySpec(Cipher): should throw IKSE");
        } catch (InvalidKeySpecException ikse) {
            // expected
        }
        // TEST#2 getKeySpec(Key)
        System.out.println("Testing getKeySpec(Key)...");
        try {
            pkcs8Spec = epki.getKeySpec(cipherKey);
            throw new Exception("getKeySpec(Key): should throw IKE");
        } catch (InvalidKeyException ike) {
            // expected
        }

        // TEST#3 getKeySpec(Key, String)
        System.out.println("Testing getKeySpec(Key, String)...");
        try {
            pkcs8Spec = epki.getKeySpec(cipherKey, p.getName());
            throw new Exception("getKeySpec(Key, String): should throw IKE");
        } catch (InvalidKeyException ike) {
            // expected
        }

        // TEST#4 getKeySpec(Key, Provider)
        System.out.println("Testing getKeySpec(Key, Provider)...");
        try {
            pkcs8Spec = epki.getKeySpec(cipherKey, p);
            throw new Exception("getKeySpec(Key, Provider): should throw IKE");
        } catch (InvalidKeyException ike) {
            // expected
        }
        System.out.println("All Tests Passed");
    }
}
