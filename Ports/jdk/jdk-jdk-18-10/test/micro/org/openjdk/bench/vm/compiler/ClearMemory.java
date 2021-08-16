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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Fork;

import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@Fork(jvmArgsPrepend = {"-XX:-EliminateAllocations", "-XX:-DoEscapeAnalysis"})
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class ClearMemory {
    class Payload8 {
        public long f0;
        public long f1;
        public long f2;
        public long f3;
        public long f4;
        public long f5;
        public long f6;
        public long f7;

        public Payload8() {
            this.f0 = 1;
        }
    }

    class Payload7 {
        public long f0;
        public long f1;
        public long f2;
        public long f3;
        public long f4;
        public long f5;
        public long f6;

        public Payload7() {
            this.f0 = 1;
        }
    }

    class Payload6 {
        public long f0;
        public long f1;
        public long f2;
        public long f3;
        public long f4;
        public long f5;

        public Payload6() {
            this.f0 = 1;
        }
    }

    class Payload5 {
        public long f0;
        public long f1;
        public long f2;
        public long f3;
        public long f4;

        public Payload5() {
            this.f0 = 1;
        }
    }

    class Payload4 {
        public long f0;
        public long f1;
        public long f2;
        public long f3;

        public Payload4() {
            this.f0 = 1;
        }
    }

    @Setup
    public void Setup() {
    }

    @Benchmark
    public void testClearMemory1K(Blackhole bh)  {
        Object [] objs = new Object[64];
        bh.consume(objs);
    }
    @Benchmark
    public void testClearMemory2K(Blackhole bh)  {
        Object [] objs = new Object[128];
        bh.consume(objs);
    }
    @Benchmark
    public void testClearMemory4K(Blackhole bh)  {
        Object [] objs = new Object[256];
        bh.consume(objs);
    }
    @Benchmark
    public void testClearMemory8K(Blackhole bh)  {
        Object [] objs = new Object[512];
        bh.consume(objs);
    }
    @Benchmark
    public void testClearMemory16K(Blackhole bh)  {
        Object [] objs = new Object[1024];
        bh.consume(objs);
    }
    @Benchmark
    public void testClearMemory32K(Blackhole bh)  {
        Object [] objs = new Object[2048];
        bh.consume(objs);
    }
    @Benchmark
    public void testClearMemory1M(Blackhole bh)  {
        Object [] objs = new Object[65536];
        bh.consume(objs);
    }
    @Benchmark
    public void testClearMemory8M(Blackhole bh)  {
        Object [] objs = new Object[524288];
        bh.consume(objs);
    }
    @Benchmark
    public void testClearMemory56B(Blackhole bh)  {
        Payload7 obj = new Payload7();
        bh.consume(obj);
    }
    @Benchmark
    public void testClearMemory48B(Blackhole bh)  {
        Payload6 obj = new Payload6();
        bh.consume(obj);
    }
    @Benchmark
    public void testClearMemory40B(Blackhole bh)  {
        Payload5 obj = new Payload5();
        bh.consume(obj);
    }
    @Benchmark
    public void testClearMemory32B(Blackhole bh)  {
        Payload4 obj = new Payload4();
        bh.consume(obj);
    }
    @Benchmark
    public void testClearMemory24B(Blackhole bh)  {
        Payload4 obj = new Payload4();
        bh.consume(obj);
    }
}
