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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MessageDigestBench extends CryptoBase {

    public static final int SET_SIZE = 128;

    @Param({"MD5", "SHA", "SHA-256", "SHA-384", "SHA-512"})
    private String algorithm;

    /*
     * Note: dataSize value shouldn't be small unless you want to evaluate MessageDigest.getInstance performance.
     * Small value causes large impact of MessageDigest.getInstance including lock contention in multi-threaded
     * execution.
     */
    @Param({""+1024*1024})
    int dataSize;

    private byte[][] data;
    int index = 0;


    @Setup
    public void setup() {
        setupProvider();
        data = fillRandom(new byte[SET_SIZE][dataSize]);
    }

    @Benchmark
    public byte[] digest() throws NoSuchAlgorithmException {
        MessageDigest md = (prov == null) ? MessageDigest.getInstance(algorithm) : MessageDigest.getInstance(algorithm, prov);
        byte[] d = data[index];
        index = (index +1) % SET_SIZE;
        return md.digest(d);
    }

}
