/*
 * Copyright (c) 2005, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6229618
 * @summary Ensure that the correct crypto permission is granted
 * even when the transformation algorithm is lowercase or mixed
 * case.
 * @author Valerie Peng
 */

import java.io.*;
import java.util.*;

import java.security.*;
import java.security.spec.*;

import javax.crypto.*;
import javax.crypto.spec.*;

public class LowercasePermCheck {

    private static String[] ALGOS = {
        "des", "desede", "rsa"
    };

    public static void main(String[] args) throws Exception {
        Provider p = Security.getProvider("SunJCE");
        System.out.println("Testing provider " + p.getName() + "...");
        if (Cipher.getMaxAllowedKeyLength("DES") == Integer.MAX_VALUE) {
            // skip this test for unlimited jurisdiction policy files
            System.out.println("Skip this test due to unlimited version");
            return;
        }
        boolean isFailed = false;
        for (int i = 0; i < ALGOS.length; i++) {
            String algo = ALGOS[i];
            Cipher c = Cipher.getInstance(algo, p);
            int keyLen1 = Cipher.getMaxAllowedKeyLength(algo);
            int keyLen2 = Cipher.getMaxAllowedKeyLength(algo.toUpperCase());

            if (keyLen1 != keyLen2) {
                System.out.println("ERROR: Wrong keysize limit for " + algo);
                System.out.println("Configured: " + keyLen2);
                System.out.println("Actual: " + keyLen1);
                isFailed = true;
            }
            System.out.println(algo + ": max " + keyLen1 + "-bit keys");
        }
        if (isFailed) {
            throw new Exception("Test Failed!");
        } else {
            System.out.println("Test Passed!");
        }
    }
}
