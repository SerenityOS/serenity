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
 * @bug 4953554
 * @summary Ensure either IllegalAlgorithmParameterException or
 * InvalidKeyException is thrown instead of SecurityException when
 * crypto permssion checks failed.
 * @author Valerie Peng
 * @key randomness
 */

import java.io.*;
import java.util.*;

import java.security.*;
import java.security.spec.*;

import javax.crypto.*;
import javax.crypto.spec.*;

public class AllPermCheck {

    private static String SYM_ALGOS[] = {
        "AES", "Blowfish", "RC2", "ARCFOUR"
    };

    public static void runTest(Cipher c, Key key) throws Exception {
        SecureRandom sr = new SecureRandom();

        for (int i = 0; i < 6; i++) {
            try {
                switch (i) {
                case 0:
                    c.init(Cipher.ENCRYPT_MODE, key);
                    break;
                case 1:
                    c.init(Cipher.ENCRYPT_MODE, key, sr);
                    break;
                case 2:
                    c.init(Cipher.ENCRYPT_MODE, key,
                           (AlgorithmParameters)null);
                    break;
                case 3:
                    c.init(Cipher.ENCRYPT_MODE, key,
                           (AlgorithmParameters)null, sr);
                    break;
                case 4:
                    c.init(Cipher.ENCRYPT_MODE, key,
                           (AlgorithmParameterSpec)null);
                    break;
                case 5:
                    c.init(Cipher.ENCRYPT_MODE, key,
                           (AlgorithmParameterSpec)null, sr);
                    break;
                }
                throw new Exception("...#" + i + " should throw IKE for " +
                                    key.getEncoded().length + "-byte keys");
            } catch (InvalidKeyException ike) {
                System.out.println("...#" + i + " expected IKE thrown");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Provider p = Security.getProvider("SunJCE");
        System.out.println("Testing provider " + p.getName() + "...");
        if (Cipher.getMaxAllowedKeyLength("DES") == Integer.MAX_VALUE) {
            // skip this test for unlimited jurisdiction policy files
            System.out.println("Skip this test due to unlimited version");
            return;
        }
        for (int i = 0; i < SYM_ALGOS.length; i++) {
            String algo = SYM_ALGOS[i];
            Cipher c = Cipher.getInstance(algo, p);
            int keyLength = Cipher.getMaxAllowedKeyLength(algo);
            SecretKey key = new SecretKeySpec(new byte[keyLength/8 + 8], algo);
            System.out.println("Testing " + algo + " Cipher");
            runTest(c, key);
        }
        System.out.println("All tests passed!");
    }
}
