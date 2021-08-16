/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8153029
 * @summary ChaCha20 key generator test.
 * @library /test/lib
 */

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.util.HexFormat;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.ChaCha20ParameterSpec;

public class ChaCha20KeyGeneratorTest {

    public static void main(String[] args) throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("ChaCha20");

        try {
            generator.init(new ChaCha20ParameterSpec(
                    HexFormat.of().parseHex("100000000000000000000000"), 0));
            throw new RuntimeException(
                    "ChaCha20 key generation should not consume AlgorithmParameterSpec");
        } catch (InvalidAlgorithmParameterException e) {
            System.out.println("Expected InvalidAlgorithmParameterException: "
                    + e.getMessage());
        }

        for (int keySize : new int[] { 32, 64, 128, 512, 1024 }) {
            try {
                generator.init(keySize);
                throw new RuntimeException("The key size for ChaCha20 must be 256");
            } catch (InvalidParameterException e) {
                System.out.println(
                        "Expected InvalidParameterException: " + keySize);
            }
        }

        generator.init(256);
        SecretKey key = generator.generateKey();
        byte[] keyValue = key.getEncoded();
        System.out.println("Key: " + HexFormat.of().formatHex(keyValue));
        if (keyValue.length != 32) {
            throw new RuntimeException("The size of generated key must be 256");
        }
    }
}
