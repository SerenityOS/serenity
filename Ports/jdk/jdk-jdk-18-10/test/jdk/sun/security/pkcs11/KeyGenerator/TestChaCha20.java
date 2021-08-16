/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8255410
 * @modules jdk.crypto.cryptoki
 * @summary Check ChaCha20 key generator.
 * @library /test/lib ..
 * @run main/othervm TestChaCha20
 */
import java.security.Provider;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.ChaCha20ParameterSpec;

public class TestChaCha20 extends PKCS11Test {

    private static final String ALGO = "ChaCha20";

    public static void main(String[] args) throws Exception {
        main(new TestChaCha20(), args);
    }

    @Override
    public void main(Provider p) throws Exception {
        System.out.println("Testing " + p.getName());
        KeyGenerator kg;
        try {
            kg = KeyGenerator.getInstance(ALGO, p);
        } catch (NoSuchAlgorithmException nsae) {
            System.out.println("Skip; no support for " + ALGO);
            return;
        }

        try {
            kg.init(new ChaCha20ParameterSpec(new byte[12], 0));
            throw new RuntimeException(
                    "ChaCha20 key generation should not need any paramSpec");
        } catch (InvalidAlgorithmParameterException e) {
            System.out.println("Expected IAPE: " + e.getMessage());
        }

        for (int keySize : new int[] { 32, 64, 128, 256, 512, 1024 }) {
            try {
                kg.init(keySize);
                if (keySize != 256) {
                    throw new RuntimeException(keySize + " is invalid keysize");
                }
            } catch (InvalidParameterException e) {
                if (keySize == 256) {
                    throw new RuntimeException("IPE thrown for valid keySize");
                } else {
                    System.out.println("Expected IPE thrown for " + keySize);
                }
            }
        }

        //kg.init(256);
        SecretKey key = kg.generateKey();
        byte[] keyValue = key.getEncoded();
        System.out.println("Key: " + HexFormat.of().formatHex(keyValue));
        if (keyValue.length != 32) {
            throw new RuntimeException("The size of generated key must be 256");
        }
    }
}
