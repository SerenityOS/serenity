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
 * @bug 4898468 6994008
 * @summary basic test for RSA cipher
 * @author Andreas Sterbenz
 * @library /test/lib ..
 * @key randomness
 * @modules jdk.crypto.cryptoki
 * @run main/othervm TestRSACipher
 * @run main/othervm -Djava.security.manager=allow TestRSACipher sm
 */

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Random;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

public class TestRSACipher extends PKCS11Test {

    private static final String[] RSA_ALGOS =
        { "RSA/ECB/PKCS1Padding", "RSA" };

    @Override
    public void main(Provider p) throws Exception {
        try {
            Cipher.getInstance(RSA_ALGOS[0], p);
        } catch (GeneralSecurityException e) {
            System.out.println("Not supported by provider, skipping");
            return;
        }
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", p);
        kpg.initialize(1024);
        KeyPair kp = kpg.generateKeyPair();
        PublicKey publicKey = kp.getPublic();
        PrivateKey privateKey = kp.getPrivate();
        Random random = new Random();
        byte[] b, e, d;
        b = new byte[16];
        random.nextBytes(b);

        for (String rsaAlgo: RSA_ALGOS) {
            Cipher c1 = Cipher.getInstance(rsaAlgo, p);
            Cipher c2 = Cipher.getInstance(rsaAlgo, "SunJCE");

            c1.init(Cipher.ENCRYPT_MODE, publicKey);
            e = c1.doFinal(b);
            c1.init(Cipher.DECRYPT_MODE, privateKey);
            d = c1.doFinal(e);
            match(b, d);
            c2.init(Cipher.DECRYPT_MODE, privateKey);
            d = c2.doFinal(e);
            match(b, d);

            // invalid data
            c1.init(Cipher.DECRYPT_MODE, publicKey);
            try {
                d = c1.doFinal(e);
                throw new Exception("completed call");
            } catch (BadPaddingException ee) {
                ee.printStackTrace();
            }

            c1.init(Cipher.ENCRYPT_MODE, privateKey);
            e = c1.doFinal(b);
            c1.init(Cipher.DECRYPT_MODE, publicKey);
            d = c1.doFinal(e);
            match(b, d);
            c2.init(Cipher.DECRYPT_MODE, publicKey);
            d = c2.doFinal(e);
            match(b, d);

            // reinit tests
            c1.init(Cipher.ENCRYPT_MODE, privateKey);
            c1.init(Cipher.ENCRYPT_MODE, privateKey);
            e = c1.doFinal(b);
            e = c1.doFinal(b);
            c1.update(b);
            c1.update(b);
            c1.init(Cipher.ENCRYPT_MODE, privateKey);
            e = c1.doFinal();
            e = c1.doFinal();
            c1.update(b);
            e = c1.doFinal();

            c1.update(new byte[256]);
            try {
                e = c1.doFinal();
                throw new Exception("completed call");
            } catch (IllegalBlockSizeException ee) {
                System.out.println(ee);
            }
        }
    }

    private static void match(byte[] b1, byte[] b2) throws Exception {
        if (Arrays.equals(b1, b2) == false) {
            System.out.println(toString(b1));
            System.out.println(toString(b2));
            throw new Exception("mismatch");
        }
    }

    public static void main(String[] args) throws Exception {
        main(new TestRSACipher(), args);
    }

}
