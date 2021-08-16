/*
 * Copyright (c) 2020, Huawei Technologies Co., Ltd. All rights reserved.
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

package org.openjdk.micro.bench.java.util;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Base64;
import java.util.Random;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class Base64Encode {

    private Base64.Encoder encoder;
    private ArrayList<byte[]> unencoded;
    private byte[] encoded;

    private static final int TESTSIZE = 1000;

    @Param({"1", "2", "3", "6", "7", "9", "10", "48", "512", "1000", "20000"})
    private int maxNumBytes;

    @Setup
    public void setup() {
        Random r = new Random(1123);

        int dstLen = ((maxNumBytes + 16) / 3) * 4;

        encoder = Base64.getEncoder();
        unencoded = new ArrayList<byte[]> ();
        encoded = new byte[dstLen];

        for (int i = 0; i < TESTSIZE; i++) {
            int srcLen = 1 + r.nextInt(maxNumBytes);
            byte[] src = new byte[srcLen];
            r.nextBytes(src);
            unencoded.add(src);
        }
    }

    @Benchmark
    @OperationsPerInvocation(TESTSIZE)
    public void testBase64Encode(Blackhole bh) {
        for (byte[] s : unencoded) {
            encoder.encode(s, encoded);
            bh.consume(encoded);
        }
    }
}
