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
package org.openjdk.bench.jdk.incubator.foreign;

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 3, jvmArgsAppend = { "--add-modules=jdk.incubator.foreign" })
public class ResourceScopeClose {

    static final int ALLOC_SIZE = 1024;

    public enum StressMode {
        NONE,
        MEMORY,
        THREADS
    }

    @Param({"NONE", "MEMORY", "THREADS"})
    StressMode mode;

    List<byte[]> arrays;
    volatile boolean stop = false;
    List<Thread> threads;

    @Setup
    public void setup() throws Throwable {
        if (mode == StressMode.MEMORY) {
            arrays = new ArrayList<>();
            for (int i = 0; i < 100_000_000; i++) {
                arrays.add(new byte[2]);
            }
        } else if (mode == StressMode.THREADS) {
            threads = new ArrayList<>();
            for (int i = 0 ; i < 4 ; i++) {
                threads.add(new Thread(() -> {
                    while (true) {
                        if (stop) break;
                        // busy wait
                    }
                }));
            }
            threads.forEach(Thread::start);
        }

    }

    @TearDown
    public void tearDown() throws Throwable {
        arrays = null;
        if (threads != null) {
            stop = true;
            threads.forEach(t -> {
                try {
                    t.join();
                } catch (InterruptedException ex) {
                    throw new IllegalStateException(ex);
                }
            });
        }
    }

    @Benchmark
    public MemorySegment confined_close() {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            return MemorySegment.allocateNative(ALLOC_SIZE, 4, scope);
        }
    }

    @Benchmark
    public MemorySegment shared_close() {
        try (ResourceScope scope = ResourceScope.newSharedScope()) {
            return MemorySegment.allocateNative(ALLOC_SIZE, 4, scope);
        }
    }

    @Benchmark
    public MemorySegment implicit_close() {
        return MemorySegment.allocateNative(ALLOC_SIZE, 4, ResourceScope.newImplicitScope());
    }

    @Benchmark
    public MemorySegment implicit_close_systemgc() {
        if (gcCount++ == 0) System.gc(); // GC when we overflow
        return MemorySegment.allocateNative(ALLOC_SIZE, 4, ResourceScope.newImplicitScope());
    }

    // keep
    static byte gcCount = 0;
}
