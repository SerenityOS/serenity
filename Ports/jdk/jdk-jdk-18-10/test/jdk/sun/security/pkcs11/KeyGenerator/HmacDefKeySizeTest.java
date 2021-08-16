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

/*
 * @test
 * @bug 8242332
 * @summary Check that PKCS11 Hamc KeyGenerator picks appropriate default size
 * @library /test/lib ..
 * @modules jdk.crypto.cryptoki
 * @run main/othervm HmacDefKeySizeTest
 * @run main/othervm -Djava.security.manager=allow HmacDefKeySizeTest sm
 */

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.util.List;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class HmacDefKeySizeTest extends PKCS11Test {

    /**
     * Request a KeyGenerator object from PKCS11 provider for Hmac algorithm,
     * and generate the SecretKey.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        main(new HmacDefKeySizeTest(), args);
    }

    @Override
    public void main(Provider p) {
        List<String> algorithms = getSupportedAlgorithms("KeyGenerator",
                "Hmac", p);
        boolean success = true;

        for (String alg : algorithms) {
            System.out.println("Testing " + alg);
            try {
                KeyGenerator kg = KeyGenerator.getInstance(alg, p);
                SecretKey k1 = kg.generateKey();
                int keysize = k1.getEncoded().length << 3;
                System.out.println("=> default key size = " + keysize);
                kg.init(keysize);
                SecretKey k2 = kg.generateKey();
                if ((k2.getEncoded().length << 3) != keysize) {
                    success = false;
                    System.out.println("keysize check failed");
                }
            } catch (Exception e) {
                System.out.println("Unexpected exception: " + e);
                e.printStackTrace();
                success = false;
            }
        }

        if (!success) {
            throw new RuntimeException("One or more tests failed");
        }
    }
}
