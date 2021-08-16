/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AlgorithmParameters;
import java.security.Key;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;

/*
 * @test
 * @bug 8048596
 * @summary Check if a key wrapper works properly with GCM mode
 */
public class KeyWrapper {

    static final String AES = "AES";
    static final String TRANSFORMATION = "AES/GCM/NoPadding";
    static final String PROVIDER = "SunJCE";
    static final int KEY_LENGTH = 128;

    public static void main(String argv[]) throws Exception {
        doTest(PROVIDER, TRANSFORMATION);
    }

    private static void doTest(String provider, String algo) throws Exception {
        SecretKey key;
        SecretKey keyToWrap;

        // init a secret Key
        KeyGenerator kg = KeyGenerator.getInstance(AES, PROVIDER);
        kg.init(KEY_LENGTH);
        key = kg.generateKey();
        keyToWrap = kg.generateKey();

        // initialization
        Cipher cipher = Cipher.getInstance(algo, provider);
        cipher.init(Cipher.WRAP_MODE, key);
        AlgorithmParameters params = cipher.getParameters();

        // wrap the key
        byte[] keyWrapper = cipher.wrap(keyToWrap);
        try {
            // check if we can't wrap it again with the same key/IV
            keyWrapper = cipher.wrap(keyToWrap);
            throw new RuntimeException(
                    "FAILED: expected IllegalStateException hasn't "
                            + "been thrown ");
        } catch (IllegalStateException ise) {
            System.out.println(ise.getMessage());
            System.out.println("Expected exception");
        }

        // unwrap the key
        cipher.init(Cipher.UNWRAP_MODE, key, params);
        cipher.unwrap(keyWrapper, algo, Cipher.SECRET_KEY);

        // check if we can unwrap second time
        Key unwrapKey = cipher.unwrap(keyWrapper, algo, Cipher.SECRET_KEY);

        if (!Arrays.equals(keyToWrap.getEncoded(), unwrapKey.getEncoded())) {
            throw new RuntimeException(
                    "FAILED: original and unwrapped keys are not equal");
        }
    }
}
