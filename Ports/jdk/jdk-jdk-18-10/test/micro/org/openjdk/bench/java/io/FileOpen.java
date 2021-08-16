/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Tests the overheads of creating File objects, and converting such objects to Paths.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(time=2, iterations=5)
@Measurement(time=3, iterations=5)
@Fork(value=2, jvmArgs="-Xmx1g")
public class FileOpen {

    private String normalFile = "/test/dir/file/name.txt";
    private String root = "/";
    private String trailingSlash = "/test/dir/file/name.txt/";
    private String notNormalizedFile = "/test/dir/file//name.txt";

    public File tmp;

    @Setup
    public void setup() throws IOException {
        tmp = new File("FileOpen.tmp");
        tmp.createNewFile();
        tmp.deleteOnExit();
    }

    @Benchmark
    public void mix(Blackhole bh) {
        bh.consume(new File(normalFile));
        bh.consume(new File(root));
        bh.consume(new File(trailingSlash));
        bh.consume(new File(notNormalizedFile));
    }

    @Benchmark
    public File normalized() {
        return new File(normalFile);
    }

    @Benchmark
    public File root() {
        return new File(root);
    }

    @Benchmark
    public File trailingSlash() {
        return new File(trailingSlash);
    }

    @Benchmark
    public File notNormalized() {
        return new File(notNormalizedFile);
    }

    @Benchmark
    public boolean booleanAttributes() {
        return tmp.exists()
                && tmp.isHidden()
                && tmp.isDirectory()
                && tmp.isFile();
    }

    @Benchmark
    public void mixToPath(Blackhole bh)  {
        bh.consume(new File(normalFile).toPath());
        bh.consume(new File(root).toPath());
        bh.consume(new File(trailingSlash).toPath());
        bh.consume(new File(notNormalizedFile).toPath());
    }

    @Benchmark
    public Path normalizedToPath() {
        return new File(normalFile).toPath();
    }

    @Benchmark
    public Path rootToPath() {
        return new File(root).toPath();
    }

    @Benchmark
    public Path trailingSlashToPath() {
        return new File(trailingSlash).toPath();
    }

    @Benchmark
    public Path notNormalizedToPath() {
        return new File(notNormalizedFile).toPath();
    }
}
