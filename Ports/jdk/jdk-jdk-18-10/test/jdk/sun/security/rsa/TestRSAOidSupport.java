/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8242897
 * @summary Ensure that RSA key factory can parse X.509 encodings containing
 * non-standard RSA oid as in older JDK releases before JDK-8146293
 * @run main TestRSAOidSupport
 */

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;

public class TestRSAOidSupport {

    // SubjectKeyInfo DER encoding w/ Algorithm id 1.3.14.3.2.15
    // which can be used to generate RSA Public Key before PSS
    // support is added
    private static String DER_BYTES =
             "3058300906052b0e03020f0500034b003048024100d7157c65e8f22557d8" +
             "a857122cfe85bddfaba3064c21b345e2a7cdd8a6751e519ab861c5109fb8" +
             "8cce45d161b9817bc0eccdc30fda69e62cc577775f2c1d66bd0203010001";

    // utility method for converting hex string to byte array
    static byte[] toByteArray(String s) {
        byte[] bytes = new byte[s.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            int v = Integer.parseInt(s.substring(index, index + 2), 16);
            bytes[i] = (byte) v;
        }
        return bytes;
    }

    public static void main(String[] args) throws Exception {
        X509EncodedKeySpec x509Spec = new X509EncodedKeySpec
                (toByteArray(DER_BYTES));
        String keyAlgo = "RSA";
        KeyFactory kf = KeyFactory.getInstance(keyAlgo, "SunRsaSign");
        RSAPublicKey rsaKey = (RSAPublicKey) kf.generatePublic(x509Spec);

        if (rsaKey.getAlgorithm() != keyAlgo) {
            throw new RuntimeException("Key algo should be " + keyAlgo +
                    ", but got " + rsaKey.getAlgorithm());
        }
        kf = KeyFactory.getInstance("RSASSA-PSS", "SunRsaSign");
        try {
            kf.generatePublic(x509Spec);
            throw new RuntimeException("Should throw IKSE");
        } catch (InvalidKeySpecException ikse) {
            System.out.println("Expected IKSE exception thrown");
        }
    }
}

