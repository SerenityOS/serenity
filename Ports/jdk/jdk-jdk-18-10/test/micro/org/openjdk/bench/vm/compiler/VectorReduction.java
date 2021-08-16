/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
public abstract class VectorReduction {
    @Param({"512"})
    public int COUNT;

    private int[] intsA;
    private int[] intsB;
    private int[] intsC;
    private int[] intsD;
    private int resI;
    private long[] longsA;
    private long[] longsB;
    private long[] longsC;
    private long[] longsD;
    private long resL;

    @Param("0")
    private int seed;
    private Random r = new Random(seed);

    @Setup
    public void init() {
        intsA = new int[COUNT];
        intsB = new int[COUNT];
        intsC = new int[COUNT];
        intsD = new int[COUNT];
        longsA = new long[COUNT];
        longsB = new long[COUNT];
        longsC = new long[COUNT];
        longsD = new long[COUNT];

        for (int i = 0; i < COUNT; i++) {
            intsA[i] = r.nextInt();
            intsB[i] = r.nextInt();
            intsC[i] = r.nextInt();
            longsA[i] = r.nextLong();
            longsB[i] = r.nextLong();
            longsC[i] = r.nextLong();
        }
    }

    @Benchmark
    public void andRedI() {
        for (int i = 0; i < COUNT; i++) {
            intsD[i] = (intsA[i] * intsB[i]) + (intsA[i] * intsC[i]) + (intsB[i] * intsC[i]);
            resI &= intsD[i];
        }
    }

    @Benchmark
    public void orRedI() {
        for (int i = 0; i < COUNT; i++) {
            intsD[i] = (intsA[i] * intsB[i]) + (intsA[i] * intsC[i]) + (intsB[i] * intsC[i]);
            resI |= intsD[i];
        }
    }

    @Benchmark
    public void xorRedI() {
        for (int i = 0; i < COUNT; i++) {
            intsD[i] = (intsA[i] * intsB[i]) + (intsA[i] * intsC[i]) + (intsB[i] * intsC[i]);
            resI ^= intsD[i];
        }
    }

    @Benchmark
    public void andRedL() {
        for (int i = 0; i < COUNT; i++) {
            longsD[i] = (longsA[i] + longsB[i]) + (longsA[i] + longsC[i]) + (longsB[i] + longsC[i]);
            resL &= longsD[i];
        }
    }

    @Benchmark
    public void orRedL() {
        for (int i = 0; i < COUNT; i++) {
            longsD[i] = (longsA[i] + longsB[i]) + (longsA[i] + longsC[i]) + (longsB[i] + longsC[i]);
            resL |= longsD[i];
        }
    }

    @Benchmark
    public void xorRedL() {
        for (int i = 0; i < COUNT; i++) {
            longsD[i] = (longsA[i] + longsB[i]) + (longsA[i] + longsC[i]) + (longsB[i] + longsC[i]);
            resL ^= longsD[i];
        }
    }

    @Fork(value = 1, jvmArgsPrepend = {
        "-XX:+UseSuperWord"
    })
    public static class WithSuperword extends VectorReduction {

    }

    @Fork(value = 1, jvmArgsPrepend = {
        "-XX:-UseSuperWord"
    })
    public static class NoSuperword extends VectorReduction {
    }

}

