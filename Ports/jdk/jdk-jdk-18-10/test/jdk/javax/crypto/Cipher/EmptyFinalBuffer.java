/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6946830
 * @summary Test the Cipher.doFinal() with 0-length buffer
 * @key randomness
 */

import java.util.*;
import java.nio.*;

import java.security.*;

import javax.crypto.*;
import javax.crypto.spec.*;

public class EmptyFinalBuffer {

    private static final String[] ALGOS = {
        "AES/ECB/PKCS5Padding", "AES/CBC/PKCS5Padding"
    };

    public static void main(String[] args) throws Exception {

        Provider[] provs = Security.getProviders();

        SecretKey key = new SecretKeySpec(new byte[16], "AES");

        boolean testFailed = false;
        for (Provider p : provs) {
            System.out.println("Testing: " + p.getName());
            for (String algo : ALGOS) {
                System.out.print("Algo: " + algo);
                Cipher c;
                try {
                    c = Cipher.getInstance(algo, p);
                } catch (NoSuchAlgorithmException nsae) {
                    // skip
                    System.out.println("=> No Support");
                    continue;
                }
                c.init(Cipher.ENCRYPT_MODE, key);
                AlgorithmParameters params = c.getParameters();
                c.init(Cipher.DECRYPT_MODE, key, params);
                try {
                    byte[] out = c.doFinal(new byte[0]);
                    System.out.println("=> Accepted w/ " +
                        (out == null? "null" : (out.length + "-byte")) +
                        " output");
                } catch (Exception e) {
                    testFailed = true;
                    System.out.println("=> Rejected w/ Exception");
                    e.printStackTrace();
                }
            }
        }
        if (testFailed) {
            throw new Exception("One or more tests failed");
        } else {
            System.out.println("All tests passed");
        }
    }
}
