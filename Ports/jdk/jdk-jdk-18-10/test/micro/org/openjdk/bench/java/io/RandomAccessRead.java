/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

/**
 * Tests the overheads of I/O API.
 * This test is known to depend heavily on disk subsystem performance.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class RandomAccessRead {

    @Param("1000000")
    private int fileSize;

    @Param("8192")
    private int buffer;

    private RandomAccessFile raf;
    private long offset;
    private int deltaIndex;
    private int[] deltas;
    private File f;
    private byte[] buf;

    @Setup(Level.Trial)
    public void beforeRun() throws IOException {
        f = File.createTempFile("RandomAccessBench", ".bin");
        try (FileOutputStream fos = new FileOutputStream(f)) {
            for (int i = 0; i < fileSize; i++) {
                fos.write((byte) i);
            }
        }
        deltas = new int[]{1, 2, 3, 5, 7, 11, 13, 17, 19, 23};
        buf = new byte[buffer];
    }

    @TearDown(Level.Trial)
    public void afterRun() throws IOException {
        f.delete();
    }

    @Setup(Level.Iteration)
    public void beforeIteration() throws IOException {
        raf = new RandomAccessFile(f, "rw");
        offset = 0;
        deltaIndex = 0;
    }

    @TearDown(Level.Iteration)
    public void afterIteration() throws IOException {
        raf.close();
    }

    @Benchmark
    public int testBuffer() throws IOException {
        offset = offset + deltas[deltaIndex];
        if (offset >= fileSize) {
            offset = 0;
        }
        deltaIndex++;
        if (deltaIndex >= deltas.length) {
            deltaIndex = 0;
        }
        raf.seek(offset);
        return raf.read(buf);
    }

    @Benchmark
    public int test() throws IOException {
        offset = offset + deltas[deltaIndex];
        if (offset >= fileSize) {
            offset = 0;
        }
        deltaIndex++;
        if (deltaIndex >= deltas.length) {
            deltaIndex = 0;
        }
        raf.seek(offset);
        return raf.read();
    }
}
