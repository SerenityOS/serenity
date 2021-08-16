/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6687725
 * @summary Test internal PKCS5Padding impl with various error conditions.
 * @author Valerie Peng
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TestPKCS5PaddingError
 * @run main/othervm -Djava.security.manager=allow TestPKCS5PaddingError sm
 */

import java.security.AlgorithmParameters;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class TestPKCS5PaddingError extends PKCS11Test {
    private static class CI { // class for holding Cipher Information
        String transformation;
        String keyAlgo;

        CI(String transformation, String keyAlgo) {
            this.transformation = transformation;
            this.keyAlgo = keyAlgo;
        }
    }

    private static final CI[] TEST_LIST = {
        // algorithms which use the native padding impl
        new CI("DES/CBC/PKCS5Padding", "DES"),
        new CI("DESede/CBC/PKCS5Padding", "DESede"),
        new CI("AES/CBC/PKCS5Padding", "AES"),
        // algorithms which use SunPKCS11's own padding impl
        new CI("DES/ECB/PKCS5Padding", "DES"),
        new CI("DESede/ECB/PKCS5Padding", "DESede"),
        new CI("AES/ECB/PKCS5Padding", "AES"),
    };

    private static StringBuffer debugBuf = new StringBuffer();

    @Override
    public void main(Provider p) throws Exception {
        try {
            byte[] plainText = new byte[200];

            for (int i = 0; i < TEST_LIST.length; i++) {
                CI currTest = TEST_LIST[i];
                System.out.println("===" + currTest.transformation + "===");
                try {
                    KeyGenerator kg =
                            KeyGenerator.getInstance(currTest.keyAlgo, p);
                    SecretKey key = kg.generateKey();
                    Cipher c1 = Cipher.getInstance(currTest.transformation,
                                                   "SunJCE");
                    c1.init(Cipher.ENCRYPT_MODE, key);
                    byte[] cipherText = c1.doFinal(plainText);
                    AlgorithmParameters params = c1.getParameters();
                    Cipher c2 = Cipher.getInstance(currTest.transformation, p);
                    c2.init(Cipher.DECRYPT_MODE, key, params);

                    // 1st test: wrong output length
                    // NOTE: Skip NSS since it reports CKR_DEVICE_ERROR when
                    // the data passed to its EncryptUpdate/DecryptUpdate is
                    // not multiple of blocks
                    if (!p.getName().equals("SunPKCS11-NSS")) {
                        try {
                            System.out.println("Testing with wrong cipherText length");
                            c2.doFinal(cipherText, 0, cipherText.length - 2);
                        } catch (IllegalBlockSizeException ibe) {
                            // expected
                        } catch (Exception ex) {
                            System.out.println("Error: Unexpected Ex " + ex);
                            ex.printStackTrace();
                        }
                    }
                    // 2nd test: wrong padding value
                    try {
                        System.out.println("Testing with wrong padding bytes");
                        cipherText[cipherText.length - 1]++;
                        c2.doFinal(cipherText);
                    } catch (BadPaddingException bpe) {
                        // expected
                    } catch (Exception ex) {
                        System.out.println("Error: Unexpected Ex " + ex);
                        ex.printStackTrace();
                    }
                    System.out.println("DONE");
                } catch (NoSuchAlgorithmException nsae) {
                    System.out.println("Skipping unsupported algorithm: " +
                            nsae);
                }
            }
        } catch (Exception ex) {
            // print out debug info when exception is encountered
            if (debugBuf != null) {
                System.out.println(debugBuf.toString());
                debugBuf = new StringBuffer();
            }
            throw ex;
        }
    }

    public static void main(String[] args) throws Exception {
        main(new TestPKCS5PaddingError(), args);
    }
}
