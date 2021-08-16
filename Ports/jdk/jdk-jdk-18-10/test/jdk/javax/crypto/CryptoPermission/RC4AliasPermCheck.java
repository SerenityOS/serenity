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
 * @bug 5056438
 * @summary Ensure the crypto permission check on RC4 ciphers
 * do not fail accidentally due to the use of an alias, i.e.
 * "ARCFOUR" vs "RC4".
 * @author Valerie Peng
 */

import java.io.*;
import java.security.*;
import javax.crypto.*;

public class RC4AliasPermCheck {
    /**
     * Testing the crypto permission check using both standard
     * and alias names.
     *
     * @param algo algorithm for key and cipher
     * @param keyLen key length must be positive
     */
    private static void test(String algo, int keyLen) throws Exception {
        Provider p = Security.getProvider("SunJCE");
        System.out.println("=>Testing " + algo + " cipher with "
                           + keyLen + "-bit key");
        KeyGenerator kg = KeyGenerator.getInstance(algo, p);
        kg.init(keyLen);
        SecretKey key = kg.generateKey();
        System.out.println("Generated key with algorithm " +
                           key.getAlgorithm());
        Cipher cipher = Cipher.getInstance(algo, p);
        System.out.println("Requested cipher with algorithm " +
                           algo);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        System.out.println("Initialization succeeded as expected");
    }

    public static void main(String[] argv) throws Exception {
        test("ARCFOUR", 120);
        test("RC4", 120);
        System.out.println("TEST PASSED");
    }
}
