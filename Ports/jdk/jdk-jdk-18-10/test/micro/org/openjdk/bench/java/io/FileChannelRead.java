/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

/**
 * Tests the overheads of I/O API.
 * This test is known to depend heavily on disk subsystem performance.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class FileChannelRead {

    @Param("1000000")
    private int fileSize;

    private File f;
    private FileChannel fc;
    private ByteBuffer bb;

    @Setup(Level.Trial)
    public void beforeRun() throws IOException {
        f = File.createTempFile("FileChannelReadBench", ".bin");
        try (FileOutputStream fos = new FileOutputStream(f)) {
            for (int i = 0; i < fileSize; i++) {
                fos.write((byte) i);
            }
        }
        bb = ByteBuffer.allocate(1);
    }

    @TearDown(Level.Trial)
    public void afterRun() throws IOException {
        f.delete();
    }

    @Setup(Level.Iteration)
    public void beforeIteration() throws IOException {
        fc = FileChannel.open(f.toPath(), StandardOpenOption.READ);
    }

    @TearDown(Level.Iteration)
    public void afterIteration() throws IOException {
        fc.close();
    }

    @Benchmark
    public void test() throws IOException {
        int ret = fc.read(bb);
        bb.flip();
        if (ret == -1) {
            // start over
            fc.position(0);
        }
    }

}
