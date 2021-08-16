/*
 * Copyright (c) 2005, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6325952
 * @summary DESKey constructor is parity-adjusting the parameters
 * @author Brad R. Wetmore
 */
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.security.spec.KeySpec;
import javax.crypto.*;
import javax.crypto.spec.*;

public class CheckParity {

    static byte [] testKey = {
        (byte)0x00,             // 00000000     even
        (byte)0x01,             // 00000001     odd
        (byte)0x02,             // 00000010     odd
        (byte)0x03,             // 00000011     even
        (byte)0xfc,             // 11111100     even
        (byte)0xfd,             // 11111101     odd
        (byte)0xfe,             // 11111110     odd
        (byte)0xff,             // 11111111     even
    };

    static byte [] expectedKey = {
        (byte)0x01,             // 00000001     odd
        (byte)0x01,             // 00000001     odd
        (byte)0x02,             // 00000010     odd
        (byte)0x02,             // 00000010     odd
        (byte)0xfd,             // 11111101     odd
        (byte)0xfd,             // 11111101     odd
        (byte)0xfe,             // 11111110     odd
        (byte)0xfe,             // 11111110     odd
    };

    static private void check(String alg, byte [] key,
            byte [] expected, KeySpec ks) throws Exception {

        SecretKeyFactory skf = SecretKeyFactory.getInstance(alg, "SunJCE");
        SecretKey sk = skf.generateSecret(ks);

        if (DESKeySpec.isParityAdjusted(key, 0)) {
            throw new Exception("Initial Key is somehow parity adjusted!");
        }

        byte [] encoded = sk.getEncoded();
        if (!Arrays.equals(expected, encoded)) {
            throw new Exception("encoded key is not the expected key");
        }

        if (!DESKeySpec.isParityAdjusted(encoded, 0)) {
            throw new Exception("Generated Key is not parity adjusted");
        }
    }

    static private void checkDESKey() throws Exception {
        check("DES", testKey, expectedKey, new DESKeySpec(testKey));
    }

    static private void checkDESedeKey() throws Exception {

        byte [] key3 = new byte [testKey.length * 3];
        byte [] expectedKey3 = new byte [expectedKey.length * 3];

        for (int i = 0; i < 3; i++) {
            System.arraycopy(testKey, 0, key3,
                i * testKey.length, testKey.length);
            System.arraycopy(expectedKey, 0, expectedKey3,
                i * testKey.length, testKey.length);
        }

        check("DESede", key3, expectedKey3, new DESedeKeySpec(key3));
    }

    public static void main(String[] args) throws Exception {
        checkDESKey();
        checkDESedeKey();
        System.out.println("Test Passed!");
    }
}
