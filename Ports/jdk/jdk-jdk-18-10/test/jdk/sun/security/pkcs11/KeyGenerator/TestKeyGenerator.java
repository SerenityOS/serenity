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

/*
 * @test
 * @bug 4917233 6461727 6490213 6720456 8242332
 * @summary test the KeyGenerator
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TestKeyGenerator
 * @run main/othervm -Djava.security.manager=allow TestKeyGenerator sm
 */

import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.ProviderException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

enum TestResult {
    PASS,
    FAIL,
    TBD
}

public class TestKeyGenerator extends PKCS11Test {

    public static void main(String[] args) throws Exception {
        main(new TestKeyGenerator(), args);
    }

    private TestResult test(String algorithm, int keyLen, Provider p,
                      TestResult expected)
        throws Exception {
        TestResult actual = TestResult.TBD;
        System.out.println("Testing " + algorithm + ", " + keyLen + " bits...");
        KeyGenerator kg;
        try {
            kg = KeyGenerator.getInstance(algorithm, p);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Not supported, skipping: " + e);
            return TestResult.PASS;
        }
        try {
            kg.init(keyLen);
            actual = TestResult.PASS;
        } catch (InvalidParameterException ipe) {
            actual = TestResult.FAIL;
        }
        if (actual == TestResult.PASS) {
            try {
                SecretKey key = kg.generateKey();
                if (expected == TestResult.FAIL) {
                    throw new Exception("Generated " + key +
                        " using invalid key length");
                }
            } catch (ProviderException e) {
                e.printStackTrace();
                throw (Exception) (new Exception
                    ("key generation failed using valid length").initCause(e));
            }
        }
        if (expected != TestResult.TBD && expected != actual) {
            throw new Exception("Expected to " + expected + ", but " +
                actual);
        }
        return actual;
    }

    @Override
    public void main(Provider p) throws Exception {
        test("DES", 0, p, TestResult.FAIL);
        test("DES", 56, p, TestResult.PASS); // ensure JCE-Compatibility
        test("DES", 64, p, TestResult.PASS);
        test("DES", 128, p, TestResult.FAIL);

        test("DESede", 0, p, TestResult.FAIL);
        // Special handling since not all PKCS11 providers support
        // 2-key DESede, e.g. SunPKCS11-Solaris.
        TestResult temp = test("DESede", 112, p, TestResult.TBD);
        test("DESede", 128, p, temp);
        test("DESede", 168, p, TestResult.PASS);
        test("DESede", 192, p, TestResult.PASS);
        test("DESede", 64, p, TestResult.FAIL);
        test("DESede", 256, p, TestResult.FAIL);

        // Different PKCS11 impls have different ranges
        // of supported key sizes for variable-key-length
        // algorithms.
        // NSS>     Blowfish: n/a,         RC4: 8-2048 bits
        // However, we explicitly disallowed key sizes less
        // than 40-bits.

        test("Blowfish", 0, p, TestResult.FAIL);
        test("Blowfish", 24, p, TestResult.FAIL);
        test("Blowfish", 32, p, TestResult.FAIL);
        test("Blowfish", 40, p, TestResult.PASS);
        test("Blowfish", 128, p, TestResult.PASS);
        test("Blowfish", 136, p, TestResult.TBD);
        test("Blowfish", 448, p, TestResult.TBD);
        test("Blowfish", 456, p, TestResult.FAIL);

        test("ARCFOUR", 0, p, TestResult.FAIL);
        test("ARCFOUR", 32, p, TestResult.FAIL);
        test("ARCFOUR", 40, p, TestResult.PASS);
        test("ARCFOUR", 128, p, TestResult.PASS);

        String[] HMAC_ALGS = {
            "HmacSHA1", "HmacSHA224", "HmacSHA256", "HmacSHA384", "HmacSHA512",
            "HmacSHA512/224", "HmacSHA512/256", "HmacSHA3-224", "HmacSHA3-256",
            "HmacSHA3-384", "HmacSHA3-512",
        };

        for (String hmacAlg : HMAC_ALGS) {
            test(hmacAlg, 0, p, TestResult.FAIL);
            test(hmacAlg, 128, p, TestResult.PASS);
            test(hmacAlg, 224, p, TestResult.PASS);
        }

        if (p.getName().equals("SunPKCS11-NSS")) {
            test("ARCFOUR", 1024, p, TestResult.PASS);
            test("ARCFOUR", 2048, p, TestResult.PASS);
            test("ARCFOUR", 2056, p, TestResult.FAIL);
        }
    }
}
