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
package org.openjdk.bench.java.util;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Tests ZipFile.getEntry() on the microbenchmarks.jar zip file
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class ZipFind {

    // Files that exist in the microbenchmarks.jar zip file
    public static final String[] existingFiles = {"org/openjdk/bench/java/util/ZipFind.class",
            "org/openjdk/bench/vm/lang/Throw.class",
            "org/openjdk/bench/java/nio/ByteBuffers.class"};
    public static String[] nonExistingFiles = {"/try/to/findme.not", "needle/in/a/HayStack.class"};

    private ZipFile zip;

    @Setup
    public void prepare() throws IOException, URISyntaxException {
        String zipFile = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        zip = new ZipFile(zipFile);

        // Verify no typos in the filename lists above
        assert zip.getEntry(ZipFind.nonExistingFiles[0]) == null;
        assert zip.getEntry(ZipFind.existingFiles[0]) != null;
        assert zip.getEntry(ZipFind.existingFiles[1]) != null;
        assert zip.getEntry(ZipFind.existingFiles[2]) != null;
    }

    @TearDown
    public void cleanup() throws IOException {
        zip.close();
    }

    @Benchmark
    public ZipEntry testOneNonExisting() throws IOException {
         return zip.getEntry(ZipFind.nonExistingFiles[0]);
    }

    @Benchmark
    public void testTwoNonExisting(Blackhole bh) throws IOException {
        bh.consume(zip.getEntry(nonExistingFiles[0]));
        bh.consume(zip.getEntry(nonExistingFiles[1]));
    }

    @Benchmark
    public void testNonExistingAndExisting(Blackhole bh) throws IOException {
        bh.consume(zip.getEntry(nonExistingFiles[0]));
        bh.consume(zip.getEntry(existingFiles[0]));
    }

    @Benchmark
    public ZipEntry testOneExisting() throws IOException {
        return zip.getEntry(ZipFind.existingFiles[0]);
    }

    @Benchmark
    public void testTwoExisting(Blackhole bh) throws IOException {
        bh.consume(zip.getEntry(existingFiles[0]));
        bh.consume(zip.getEntry(existingFiles[1]));
    }

    @Benchmark
    public void testThreeExisting(Blackhole bh) throws IOException {
        bh.consume(zip.getEntry(existingFiles[0]));
        bh.consume(zip.getEntry(existingFiles[1]));
        bh.consume(zip.getEntry(existingFiles[2]));
    }
}
