/*
 * Copyright (c) 2021, Alibaba Group Holding Limited. All Rights Reserved.
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
 *
 */
package org.openjdk.bench.vm.compiler;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Test for x % 2^n (n is constant)
 */
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class ModPowerOf2 {
    @Benchmark
    public int testPositivePowerOf2() {
        int sum = 0;
        for (int i = 0; i < 1000; i++) {
            sum += i % 1;
            sum += i % 2;
            sum += i % 4;
            sum += i % 8;
            sum += i % 16;
            sum += i % 32;
            sum += i % 64;
            sum += i % 128;
            sum += i % 256;
            sum += i % 512;
            sum += i % 1024;
            sum += i % 2048;
            sum += i % 4096;
            sum += i % 8192;
            sum += i % 16384;
            sum += i % 32768;
            sum += i % 65536;
        }
        return sum;
    }

    @Benchmark
    public int testNegativePowerOf2() {
        int sum = 0;
        for (int i = 0; i < 1000; i++) {
            sum += i % -1;
            sum += i % -2;
            sum += i % -4;
            sum += i % -8;
            sum += i % -16;
            sum += i % -32;
            sum += i % -64;
            sum += i % -128;
            sum += i % -256;
            sum += i % -512;
            sum += i % -1024;
            sum += i % -2048;
            sum += i % -4096;
            sum += i % -8192;
            sum += i % -16384;
            sum += i % -32768;
            sum += i % -65536;
        }
        return sum;
    }

    @Benchmark
    public int testMixedPowerOf2() {
        int sum = 0;
        for (int i = 0; i < 1000; i++) {
            sum += i % -1;
            sum += i % 1;
            sum += i % -2;
            sum += i % 2;
            sum += i % -4;
            sum += i % 4;
            sum += i % -8;
            sum += i % 8;
            sum += i % -16;
            sum += i % 16;
            sum += i % -32;
            sum += i % 32;
            sum += i % -64;
            sum += i % 64;
            sum += i % -128;
            sum += i % 128;
            sum += i % -256;
            sum += i % 256;
            sum += i % -512;
            sum += i % 512;
            sum += i % -1024;
            sum += i % 1024;
            sum += i % -2048;
            sum += i % 2048;
            sum += i % -4096;
            sum += i % 4096;
            sum += i % -8192;
            sum += i % 8192;
            sum += i % -16384;
            sum += i % 16384;
            sum += i % -32768;
            sum += i % 32768;
            sum += i % -65536;
            sum += i % 65536;
        }
        return sum;
    }
}
