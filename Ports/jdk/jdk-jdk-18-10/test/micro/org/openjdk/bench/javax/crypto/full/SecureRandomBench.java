/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;

public class SecureRandomBench extends CryptoBase {

    @Param({"NativePRNG", "SHA1PRNG"})
    private String algorithm;

    @Param({"64"})
    int dataSize;

    @Param({"true", "false"})
    // if shared - use the single SecureRandom instance for all threads
    // otherwise - each thread uses its own SecureRandom instance
    boolean shared;

    private byte[] bytes;
    private SecureRandom rnd;

    private static SecureRandom sharedRnd;

    private static synchronized SecureRandom getSharedInstance(String algorithm, Provider prov) throws NoSuchAlgorithmException {
        if (sharedRnd == null) {
            sharedRnd = (prov == null) ? SecureRandom.getInstance(algorithm) : SecureRandom.getInstance(algorithm, prov);
        }
        return sharedRnd;
    }

    @Setup
    public void setup() throws NoSuchAlgorithmException {
        setupProvider();
        bytes = new byte[dataSize];
        if (shared) {
            rnd = getSharedInstance(algorithm, prov);
        } else {
            rnd = (prov == null) ? SecureRandom.getInstance(algorithm) : SecureRandom.getInstance(algorithm, prov);
        }
    }

    @Benchmark
    public byte[] nextBytes() {
        rnd.nextBytes(bytes);
        return bytes;
    }

}
