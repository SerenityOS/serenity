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
 * @bug 4892365
 * @summary Ensure the crypto permission check on cipher algorithms
 * with restricted parameter values are correctly enforced.
 * @author Valerie Peng
 * @key randomness
 */

import java.io.*;
import java.util.*;

import java.security.*;
import java.security.spec.*;

import javax.crypto.*;
import javax.crypto.spec.*;

public class RC2PermCheck {

    public static void main(String[] args) throws Exception {
        Provider p = Security.getProvider("SunJCE");
        System.out.println("Testing provider " + p.getName() + "...");
        if (Cipher.getMaxAllowedKeyLength("DES") == Integer.MAX_VALUE) {
            // skip this test for unlimited jurisdiction policy files
            System.out.println("Skip this test due to unlimited version");
            return;
        }
        // Currently, RC2 is the only algorithm whose parameter values
        // are restricted
        String algo = "RC2";
        Cipher c = Cipher.getInstance(algo + "/CBC/PKCS5Padding", p);
        SecretKeySpec key = new SecretKeySpec(new byte[16], "RC2");
        SecureRandom srand = new SecureRandom();
        int numOfTests = 6;
        boolean result = true;
        // test set#1: init with no parameter supplied
        for (int i = 0; i < numOfTests; i++) {
            try {
                switch (i) {
                case 0:
                    c.init(Cipher.ENCRYPT_MODE, key);
                    break;
                case 1:
                    c.init(Cipher.ENCRYPT_MODE, key, srand);
                    break;
                case 2:
                    c.init(Cipher.ENCRYPT_MODE, key,
                           (AlgorithmParameters) null);
                    break;
                case 3:
                    c.init(Cipher.ENCRYPT_MODE, key,
                           (AlgorithmParameters) null, srand);
                    break;
                case 4:
                    c.init(Cipher.ENCRYPT_MODE, key,
                           (AlgorithmParameterSpec) null);
                    break;
                case 5:
                    c.init(Cipher.ENCRYPT_MODE, key,
                           (AlgorithmParameterSpec) null, srand);
                    break;
                }
            } catch (Exception ex) {
                result = false;
                System.out.println("Test#1." + i + " failed!");
                ex.printStackTrace();
                continue;
            }
        }
        // test set#2: init with parameter within limit
        RC2ParameterSpec paramSpec = new RC2ParameterSpec(128, new byte[8]);
        AlgorithmParameters param = AlgorithmParameters.getInstance(algo, p);
        param.init(paramSpec);
        numOfTests = 4;
        for (int i = 0; i < numOfTests; i++) {
            try {
                switch (i) {
                case 0:
                    c.init(Cipher.ENCRYPT_MODE, key, paramSpec);
                    break;
                case 1:
                    c.init(Cipher.ENCRYPT_MODE, key, paramSpec, srand);
                    break;
                case 2:
                    c.init(Cipher.ENCRYPT_MODE, key, param);
                    break;
                case 3:
                    c.init(Cipher.ENCRYPT_MODE, key, param, srand);
                    break;
                }
            } catch (Exception ex) {
                result = false;
                System.out.println("Test#2." + i + " failed!");
                ex.printStackTrace();
            }
        }
        // test set#3: init with parameter over limit
        paramSpec = new RC2ParameterSpec(256, new byte[8]);
        param = AlgorithmParameters.getInstance(algo);
        param.init(paramSpec);

        for (int i = 0; i < numOfTests; i++) {
            try {
                switch (i) {
                case 0:
                    c.init(Cipher.ENCRYPT_MODE, key, paramSpec);
                    result = false;
                    System.out.println("Test#3." + i + " failed!");
                    break;
                case 1:
                    c.init(Cipher.ENCRYPT_MODE, key, paramSpec, srand);
                    result = false;
                    System.out.println("Test#3." + i + " failed!");
                    break;
                case 2:
                    c.init(Cipher.ENCRYPT_MODE, key, param);
                    result = false;
                    System.out.println("Test#3." + i + " failed!");
                    break;
                case 3:
                    c.init(Cipher.ENCRYPT_MODE, key, param, srand);
                    result = false;
                    System.out.println("Test#3." + i + " failed!");
                    break;
                }
            } catch (InvalidAlgorithmParameterException iape) {
                // expected exception thrown; proceed to next test
                continue;
            }
        }
        if (result) {
            System.out.println("All tests passed!");
        } else {
            throw new Exception("One or more test failed!");
        }
    }
}
