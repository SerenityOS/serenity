/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * This performance tests runs AES/GCM encryption and decryption using byte[]
 * as input and output buffers for single and multi-part testing.
 *
 * This test rotates the IV and creates a new GCMParameterSpec for each encrypt
 * benchmark operation
 */

public class AESGCMBench extends CryptoBase {

    @Param({"128"})
    private int keyLength;

    @Param({"1024", "1500", "4096", "16384"})
    private int dataSize;

    byte[] encryptedData;
    byte[] in, out;
    private Cipher encryptCipher;
    private Cipher decryptCipher;
    SecretKeySpec ks;
    GCMParameterSpec gcm_spec;
    byte[] iv;

    private static final int IV_BUFFER_SIZE = 32;
    private static final int IV_MODULO = IV_BUFFER_SIZE - 16;
    int iv_index = 0;
    int updateLen = 0;

    private int next_iv_index() {
        int r = iv_index;
        iv_index = (iv_index + 1) % IV_MODULO;
        return r;
    }

    @Setup
    public void setup() throws Exception {
        setupProvider();

        // Setup key material
        byte[] keystring = fillSecureRandom(new byte[keyLength / 8]);
        ks = new SecretKeySpec(keystring, "AES");
        iv = fillSecureRandom(new byte[IV_BUFFER_SIZE]);
        gcm_spec = new GCMParameterSpec(96, iv, next_iv_index(), 16);

        // Setup Cipher classes
        encryptCipher = makeCipher(prov, "AES/GCM/NoPadding");
        encryptCipher.init(Cipher.ENCRYPT_MODE, ks, gcm_spec);
        decryptCipher = makeCipher(prov, "AES/GCM/NoPadding");
        decryptCipher.init(Cipher.DECRYPT_MODE, ks,
            encryptCipher.getParameters().
                getParameterSpec(GCMParameterSpec.class));

        // Setup input/output buffers
        in = fillRandom(new byte[dataSize]);
        encryptedData = new byte[encryptCipher.getOutputSize(in.length)];
        out = new byte[encryptedData.length];
        encryptCipher.doFinal(in, 0, in.length, encryptedData, 0);
        updateLen = in.length / 2;

    }

    @Benchmark
    public void encrypt() throws Exception {
        gcm_spec = new GCMParameterSpec(96, iv, next_iv_index(), 16);
        encryptCipher.init(Cipher.ENCRYPT_MODE, ks, gcm_spec);
        encryptCipher.doFinal(in, 0, in.length, out, 0);
    }

    @Benchmark
    public void encryptMultiPart() throws Exception {
        gcm_spec = new GCMParameterSpec(96, iv, next_iv_index(), 16);
        encryptCipher.init(Cipher.ENCRYPT_MODE, ks, gcm_spec);
        int outOfs = encryptCipher.update(in, 0, updateLen, out, 0);
        encryptCipher.doFinal(in, updateLen, in.length - updateLen,
            out, outOfs);
    }

    @Benchmark
    public void decrypt() throws Exception {
        decryptCipher.init(Cipher.DECRYPT_MODE, ks,
            encryptCipher.getParameters().
                getParameterSpec(GCMParameterSpec.class));
        decryptCipher.doFinal(encryptedData, 0, encryptedData.length, out, 0);
    }

    @Benchmark
    public void decryptMultiPart() throws Exception {
        decryptCipher.init(Cipher.DECRYPT_MODE, ks,
            encryptCipher.getParameters().
                getParameterSpec(GCMParameterSpec.class));
        decryptCipher.update(encryptedData, 0, updateLen, out, 0);
        decryptCipher.doFinal(encryptedData, updateLen,
            encryptedData.length - updateLen, out, 0);
    }
}
