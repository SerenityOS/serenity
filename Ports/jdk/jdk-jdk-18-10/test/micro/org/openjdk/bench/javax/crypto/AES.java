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

import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class AES {

    @Param("10000000")
    private int count;

    private Cipher cipher;
    private byte[] src;

    @Setup
    public void setup() throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(new byte[]{-80, -103, -1, 68, -29, -94, 61, -52, 93, -59, -128, 105, 110, 88, 44, 105}, "AES");
        IvParameterSpec iv = new IvParameterSpec(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});

        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);

        src = new byte[count];
        new Random(1).nextBytes(src);
    }

    @Benchmark
    @Fork(jvmArgsAppend = {"-XX:+UnlockDiagnosticVMOptions", "-XX:-UseAES", "-XX:-UseAESIntrinsics"})
    public byte[] testBaseline() throws Exception {
        return cipher.doFinal(src);
    }

    @Benchmark
    @Fork(jvmArgsAppend = {"-XX:+UnlockDiagnosticVMOptions", "-XX:+UseAES", "-XX:-UseAESIntrinsics"})
    public byte[] testUseAes() throws Exception {
        return cipher.doFinal(src);
    }

    @Benchmark
    @Fork(jvmArgsAppend = {"-XX:+UnlockDiagnosticVMOptions", "-XX:+UseAES", "-XX:+UseAESIntrinsics"})
    public byte[] testUseAesIntrinsics() throws Exception {
        return cipher.doFinal(src);
    }

}
