/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8175251
 * @summary ensure that PKCS8-encoded private key with leading 0s
 * can be loaded.
 * @run main TestLeadingZeros
 */

import java.io.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.interfaces.*;
import java.util.*;

public class TestLeadingZeros {

    // The following test vectors are various BER encoded PKCS8 bytes
    static final String[] PKCS8_ENCODINGS  = {
        // first is the original one from PKCS8Test
        "301e020100301206052b0e03020c30090201020201030201040403020101A000",
        // changed original to version w/ 1 leading 0
        "301f02020000301206052b0e03020c30090201020201030201040403020101A000",
        // changed original to P w/ 1 leading 0
        "301f020100301306052b0e03020c300a020200020201030201040403020101A000",
        // changed original to X w/ 2 leading 0s
        "3020020100301206052b0e03020c300902010202010302010404050203000001A000"
    };

    public static void main(String[] argv) throws Exception {
        KeyFactory factory = KeyFactory.getInstance("DSA", "SUN");

        for (String encodings : PKCS8_ENCODINGS) {
            byte[] encodingBytes = hexToBytes(encodings);
            PKCS8EncodedKeySpec encodedKeySpec =
                new PKCS8EncodedKeySpec(encodingBytes);
            DSAPrivateKey privKey2 = (DSAPrivateKey)
                factory.generatePrivate(encodedKeySpec);
            System.out.println("key: " + privKey2);
        }
        System.out.println("Test Passed");
    }

    private static byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new RuntimeException("Input should be even length");
        }
        int size = hex.length() / 2;
        byte[] result = new byte[size];
        for (int i = 0; i < size; i++) {
            int hi = Character.digit(hex.charAt(2 * i), 16);
            int lo = Character.digit(hex.charAt(2 * i + 1), 16);
            if ((hi == -1) || (lo == -1)) {
                throw new RuntimeException("Input should be hexadecimal");
            }
            result[i] = (byte) (16 * hi + lo);
        }
        return result;
    }
}
