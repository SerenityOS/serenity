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
package org.openjdk.bench.vm.compiler;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.*;

import java.util.concurrent.TimeUnit;
import java.util.Random;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class VectorShiftAccumulate {
    @Param({"1028"})
    public int count;

    private byte[]  bytesA,  bytesB,  bytesD;
    private short[] shortsA, shortsB, shortsD;
    private char[]  charsA,  charsB,  charsD;
    private int[]   intsA,   intsB,   intsD;
    private long[]  longsA,  longsB,  longsD;

    @Param("0")
    private int seed;
    private Random r = new Random(seed);

    @Setup
    public void init() {
        bytesA  = new byte[count];
        shortsA = new short[count];
        charsA  = new char[count];
        intsA   = new int[count];
        longsA  = new long[count];

        bytesB  = new byte[count];
        shortsB = new short[count];
        charsB  = new char[count];
        intsB   = new int[count];
        longsB  = new long[count];

        bytesD  = new byte[count];
        shortsD = new short[count];
        charsD  = new char[count];
        intsD   = new int[count];
        longsD  = new long[count];

        for (int i = 0; i < count; i++) {
            bytesA[i]  = (byte) r.nextInt();
            shortsA[i] = (short) r.nextInt();
            intsA[i]   = r.nextInt();
            longsA[i]  = r.nextLong();

            bytesB[i]  = (byte) r.nextInt();
            shortsB[i] = (short) r.nextInt();
            intsB[i]   = r.nextInt();
            longsB[i]  = r.nextLong();
        }
    }

    @Benchmark
    public void shiftRightAccumulateByte() {
        for (int i = 0; i < count; i++) {
            bytesD[i] = (byte) (bytesA[i] + (bytesB[i] >> 1));
        }
    }

    @Benchmark
    public void shiftURightAccumulateByte() {
        for (int i = 0; i < count; i++) {
            bytesD[i] = (byte) (bytesA[i] + (((byte) (bytesB[i] >>> 3))));
        }
    }

    @Benchmark
    public void shiftRightAccumulateShort() {
        for (int i = 0; i < count; i++) {
            shortsD[i] = (short) (shortsA[i] + (shortsB[i] >> 5));
        }
    }

    @Benchmark
    public void shiftURightAccumulateChar() {
        for (int i = 0; i < count; i++) {
            charsD[i] = (char) (charsA[i] + (charsB[i] >>> 4));
        }
    }

    @Benchmark
    public void shiftRightAccumulateInt() {
        for (int i = 0; i < count; i++) {
            intsD[i] = intsA[i] + (intsB[i] >> 2);
        }
    }

    @Benchmark
    public void shiftURightAccumulateInt() {
        for (int i = 0; i < count; i++) {
            intsD[i] = (intsB[i] >>> 2) + intsA[i];
        }
    }

    @Benchmark
    public void shiftRightAccumulateLong() {
        for (int i = 0; i < count; i++) {
            longsD[i] = longsA[i] + (longsB[i] >> 5);
        }
    }

    @Benchmark
    public void shiftURightAccumulateLong() {
        for (int i = 0; i < count; i++) {
            longsD[i] = (longsB[i] >>> 2) + longsA[i];
        }
    }
}

