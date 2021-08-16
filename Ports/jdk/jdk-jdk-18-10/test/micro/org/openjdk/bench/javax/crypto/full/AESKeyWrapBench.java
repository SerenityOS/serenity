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
package org.openjdk.bench.javax.crypto.full;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import java.util.concurrent.TimeUnit;


import java.security.Key;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidParameterSpecException;


public class AESKeyWrapBench extends CryptoBase {

    @Param({"AES/KW/NoPadding" , "AES/KW/PKCS5Padding", "AES/KWP/NoPadding"})
    private String algorithm;

    @Param({"128"})
    private int keyLength;

    @Param({"16", "24"})
    private int dataSize;

    SecretKeySpec toBeWrappedKey;
    byte[] wrappedKey;
    private Cipher encryptCipher;
    private Cipher decryptCipher;
    SecretKeySpec ks;

    @Setup
    public void setup() throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException {
        setupProvider();

        byte[] keystring = fillSecureRandom(new byte[keyLength / 8]);
        ks = new SecretKeySpec(keystring, "AES");
        encryptCipher = makeCipher(prov, algorithm);
        encryptCipher.init(Cipher.WRAP_MODE, ks);
        decryptCipher = makeCipher(prov, algorithm);
        decryptCipher.init(Cipher.UNWRAP_MODE, ks);
        byte[] data = fillRandom(new byte[dataSize]);
        toBeWrappedKey = new SecretKeySpec(data, "Custom");
        wrappedKey = encryptCipher.wrap(toBeWrappedKey);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] wrap() throws InvalidKeyException, IllegalBlockSizeException {
        return encryptCipher.wrap(toBeWrappedKey);
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public Key unwrap() throws InvalidKeyException, NoSuchAlgorithmException {
        return decryptCipher.unwrap(wrappedKey, "Custom", Cipher.SECRET_KEY);
    }
}
