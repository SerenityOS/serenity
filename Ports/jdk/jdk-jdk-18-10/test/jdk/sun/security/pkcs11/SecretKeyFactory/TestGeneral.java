/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8255410
 * @summary test the general SecretKeyFactory functionality
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TestGeneral
 */

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class TestGeneral extends PKCS11Test {

    private enum TestResult {
        PASS,
        FAIL,
        TBD // unused for now
    }

    public static void main(String[] args) throws Exception {
        main(new TestGeneral(), args);
    }

    private void test(String algorithm, SecretKey key, Provider p,
            TestResult expected) throws RuntimeException {
        System.out.println("Testing " + algorithm + " SKF from " + p.getName());
        SecretKeyFactory skf;
        try {
            skf = SecretKeyFactory.getInstance(algorithm, p);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Not supported, skipping: " + e);
            return;
        }
        try {
            SecretKey key2 = skf.translateKey(key);
            if (expected == TestResult.FAIL) {
                throw new RuntimeException("translateKey() should FAIL");
            }
            System.out.println("=> translated key");
            if (!key2.getAlgorithm().equalsIgnoreCase(algorithm)) {
                throw new RuntimeException("translated key algorithm mismatch");
            }
            System.out.println("=> checked key algorithm");

            // proceed to check encodings if format match
            if (key2.getFormat().equalsIgnoreCase(key.getFormat())) {
                if (key2.getEncoded() != null &&
                        !Arrays.equals(key.getEncoded(), key2.getEncoded())) {
                    throw new RuntimeException(
                            "translated key encoding mismatch");
                }
                System.out.println("=> checked key format and encoding");
            }
        } catch (Exception e) {
            if (expected == TestResult.PASS) {
                e.printStackTrace();
                throw new RuntimeException("translateKey() should pass", e);
            }
        }
    }

    @Override
    public void main(Provider p) throws Exception {

        byte[] rawBytes = new byte[32];
        new SecureRandom().nextBytes(rawBytes);

        SecretKey aes_128Key = new SecretKeySpec(rawBytes, 0, 16, "AES");
        SecretKey aes_256Key = new SecretKeySpec(rawBytes, 0, 32, "AES");
        SecretKey bf_128Key = new SecretKeySpec(rawBytes, 0, 16, "Blowfish");
        SecretKey cc20Key = new SecretKeySpec(rawBytes, 0, 32, "ChaCha20");

        // fixed key length
        test("AES", aes_128Key, p, TestResult.PASS);
        test("AES", aes_256Key, p, TestResult.PASS);
        test("AES", cc20Key, p, TestResult.FAIL);

        test("ChaCha20", aes_128Key, p, TestResult.FAIL);
        test("ChaCha20", aes_256Key, p, TestResult.FAIL);
        test("ChaCha20", cc20Key, p, TestResult.PASS);

        // variable key length
        // Different PKCS11 impls may have different ranges
        // of supported key sizes for variable-key-length
        // algorithms.
        test("Blowfish", aes_128Key, p, TestResult.FAIL);
        test("Blowfish", cc20Key, p, TestResult.FAIL);
        test("Blowfish", bf_128Key, p, TestResult.PASS);
    }
}
