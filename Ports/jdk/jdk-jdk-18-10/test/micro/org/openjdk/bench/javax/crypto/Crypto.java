/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.javax.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Tests various encryption algorithms with the JCE framework. Sets Fork
 * parameters as these tests are rather allocation intensive. Reduced numbers of
 * forks and iteration as benchmarks are stable.
 */
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(jvmArgsAppend = {"-Xms1024m", "-Xmx1024m", "-Xmn768m", "-XX:+UseParallelGC"}, value = 5)
public class Crypto {

    @Param({"64", "1024", "16384"})
    private int length;

    @Param({"AES", "Blowfish", "DES", "DESede"})
    private String cipherName;

    private SecretKeySpec secretKey;
    private Cipher encryptCipher;
    private Cipher decryptCipher;
    private byte[] plainBytes;
    private byte[] encryptedBytes;

    @Setup
    public void setupSubclass() throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        // Setup ciphers for encrypt/decrypt
        byte[] encodedKey = KeyGenerator.getInstance(cipherName).generateKey().getEncoded();
        secretKey = new SecretKeySpec(encodedKey, cipherName);

        encryptCipher = Cipher.getInstance(cipherName);
        encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey);

        decryptCipher = Cipher.getInstance(cipherName);
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKey);

        // Generate data to encrypt/decrypt
        plainBytes = new byte[length];
        new Random(1234567890).nextBytes(plainBytes);
        encryptedBytes = encryptCipher.doFinal(plainBytes);
    }

    /**
     * Encrypt byte array
     *
     * @return encrypted byte array
     * @throws javax.crypto.IllegalBlockSizeException
     * @throws javax.crypto.BadPaddingException
     */
    @Benchmark
    public byte[] encrypt() throws IllegalBlockSizeException, BadPaddingException {
        return encryptCipher.doFinal(plainBytes);
    }

    /**
     * Decrypt byte array
     *
     * @return decrypted byte array
     * @throws javax.crypto.IllegalBlockSizeException
     * @throws javax.crypto.BadPaddingException
     */
    @Benchmark
    public byte[] decrypt() throws IllegalBlockSizeException, BadPaddingException {
        return decryptCipher.doFinal(encryptedBytes);
    }
}
