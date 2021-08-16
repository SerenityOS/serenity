/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6994008
 * @summary basic test for RSA/ECB/NoPadding cipher
 * @author Valerie Peng
 * @library /test/lib ..
 * @key randomness
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TestRawRSACipher
 * @run main/othervm -Djava.security.manager=allow TestRawRSACipher sm
 */

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.util.Arrays;
import java.util.Random;
import javax.crypto.Cipher;

public class TestRawRSACipher extends PKCS11Test {

    @Override
    public void main(Provider p) throws Exception {
        try {
            Cipher.getInstance("RSA/ECB/NoPadding", p);
        } catch (GeneralSecurityException e) {
            System.out.println("Not supported by provider, skipping");
            return;
        }

        final int KEY_LEN = 1024;
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", p);
        kpGen.initialize(KEY_LEN);
        KeyPair kp = kpGen.generateKeyPair();
        Random random = new Random();
        byte[] plainText, cipherText, recoveredText;
        plainText = new byte[KEY_LEN/8];
        random.nextBytes(plainText);
        plainText[0] = 0; // to ensure that it's less than modulus

        Cipher c1 = Cipher.getInstance("RSA/ECB/NoPadding", p);
        Cipher c2 = Cipher.getInstance("RSA/ECB/NoPadding", "SunJCE");

        c1.init(Cipher.ENCRYPT_MODE, kp.getPublic());
        c2.init(Cipher.DECRYPT_MODE, kp.getPrivate());

        cipherText = c1.doFinal(plainText);
        recoveredText = c2.doFinal(cipherText);
        if (!Arrays.equals(plainText, recoveredText)) {
            throw new RuntimeException("E/D Test against SunJCE Failed!");
        }

        c2.init(Cipher.ENCRYPT_MODE, kp.getPublic());
        c1.init(Cipher.DECRYPT_MODE, kp.getPrivate());
        cipherText = c2.doFinal(plainText);
        recoveredText = c1.doFinal(cipherText);
        if (!Arrays.equals(plainText, recoveredText)) {
            throw new RuntimeException("D/E Test against SunJCE Failed!");
        }

        System.out.println("Test Passed");
    }

    public static void main(String[] args) throws Exception {
        main(new TestRawRSACipher(), args);
    }
}
