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
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SealedObject;

/*
 * @test
 * @bug 8048596
 * @summary Check if the seal/unseal feature works properly in AEAD/GCM mode.
 */
public class SealedObjectTest {

    private static final String AES = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String PROVIDER = "SunJCE";
    private static final int KEY_LENGTH = 128;

    public static void main(String[] args) throws Exception {
        doTest();
    }

    /*
     * Run the test:
     * - init a cipher with AES/GCM/NoPadding transformation
     * - seal an object
     * - check if we can't seal it again with the same key/IV
     * - unseal the object using different methods of SealedObject class
     * - check if the original and sealed objects are equal
     */
    static void doTest() throws Exception {
        // init a secret Key
        KeyGenerator kg = KeyGenerator.getInstance(AES, PROVIDER);
        kg.init(KEY_LENGTH);
        SecretKey key = kg.generateKey();

        // initialization
        Cipher cipher = Cipher.getInstance(TRANSFORMATION, PROVIDER);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        AlgorithmParameters params = cipher.getParameters();

        // seal an object
        SealedObject so = new SealedObject(key, cipher);
        try {
            // check if we can't seal it again with the same key/IV
            so = new SealedObject(key, cipher);
            throw new RuntimeException(
                    "FAILED: expected IllegalStateException hasn't "
                            + "been thrown");
        } catch (IllegalStateException ise) {
            System.out.println("Expected exception when seal it again with"
                    + " the same key/IV: " + ise);
        }

        // unseal the object using getObject(Cipher) and compare
        cipher.init(Cipher.DECRYPT_MODE, key, params);
        SecretKey unsealedKey = (SecretKey) so.getObject(cipher);
        assertKeysSame(unsealedKey, key, "SealedObject.getObject(Cipher)");

        // unseal the object using getObject(Key) and compare
        unsealedKey = (SecretKey) so.getObject(key);
        assertKeysSame(unsealedKey, key, "SealedObject.getObject(Key)");

        // unseal the object using getObject(Key, String) and compare
        unsealedKey = (SecretKey) so.getObject(key, PROVIDER);

        assertKeysSame(unsealedKey, key,
                "SealedObject.getObject(Key, String)");
    }

    /**
     * Compare two SecretKey objects.
     *
     * @param key1 first key
     * @param key2 second key
     * @param meth method that was used for unsealing the SecretKey object
     * @return true if key1 and key2 are the same, false otherwise.
     */
    static void assertKeysSame(SecretKey key1, SecretKey key2, String meth) {
        if (!Arrays.equals(key1.getEncoded(), key2.getEncoded())) {
            throw new RuntimeException(
                    "FAILED: original and unsealed objects aren't the same for "
                            + meth);
        }
    }
}
