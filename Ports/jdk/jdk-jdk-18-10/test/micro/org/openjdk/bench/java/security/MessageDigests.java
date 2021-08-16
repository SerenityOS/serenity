/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.security;

import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
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
 * Tests various digester algorithms. Sets Fork parameters as these tests are
 * rather allocation intensive. Reduced number of forks and iterations as
 * benchmarks are stable.
 */
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(jvmArgsAppend = {"-Xms1024m", "-Xmx1024m", "-Xmn768m", "-XX:+UseParallelGC"}, value = 5)
public class MessageDigests {

    @Param({"64", "1024", "16384"})
    private int length;

    @Param({"md2", "md5", "SHA-1", "SHA-224", "SHA-256", "SHA-384", "SHA-512", "SHA3-224", "SHA3-256", "SHA3-384", "SHA3-512"})
    private String digesterName;

    @Param({"DEFAULT", "SUN"})
    protected String provider;

    private byte[] inputBytes;
    private MessageDigest digester;

    @Setup
    public void setup() throws NoSuchAlgorithmException, DigestException, NoSuchProviderException {
        inputBytes = new byte[length];
        new Random(1234567890).nextBytes(inputBytes);
        if ("DEFAULT".equals(provider)) {
            digester = MessageDigest.getInstance(digesterName);
        } else {
            digester = MessageDigest.getInstance(digesterName, provider);
        }
    }

    @Benchmark
    public byte[] digest() throws DigestException {
        return digester.digest(inputBytes);
    }

    @Benchmark
    public byte[] getAndDigest() throws DigestException, NoSuchAlgorithmException, NoSuchProviderException {
        MessageDigest md;
        if ("DEFAULT".equals(provider)) {
            md = MessageDigest.getInstance(digesterName);
        } else {
            md = MessageDigest.getInstance(digesterName, provider);
        }
        return md.digest(inputBytes);
    }
}
