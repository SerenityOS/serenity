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
package org.openjdk.bench.java.util;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.zip.Adler32;
import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 2)
@Warmup(iterations = 2, time = 30, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 60, timeUnit = TimeUnit.SECONDS)

public class TestAdler32 {

    private Adler32 adler32;
    private Random random;
    private byte[] bytes;

    @Param({"64", "128", "256", "512", "1024", "2048", "4096", "8192", "16384", "32768", "65536"})
    private int count;

    public TestAdler32() {
        adler32 = new Adler32();
        random = new Random(2147483648L);
        bytes = new byte[1000000];
        random.nextBytes(bytes);
    }

    @Setup(Level.Iteration)
    public void setupBytes() {
        adler32.reset();
    }

    @Benchmark
    public void testAdler32Update() {
        adler32.update(bytes, 0, count);
    }
}
