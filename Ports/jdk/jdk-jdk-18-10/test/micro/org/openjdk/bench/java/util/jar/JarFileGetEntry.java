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

package org.openjdk.bench.java.util.jar;

import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * Simple benchmark measuring cost of looking up entries in a jar file.
 *
 * Before JDK-8193066
 * Benchmark                             (size)  Mode  Cnt   Score    Error   Units
 * JarFileGetEntry.getEntryHit            1024  avgt   10  102.554    3.371   ns/op
 *   gc.alloc.rate.norm                   1024  avgt   10  144.036    0.004    B/op
 * JarFileGetEntry.getEntryHitUncached    1024  avgt   10  141.307    7.454   ns/op
 *   gc.alloc.rate.norm                   1024  avgt   10  200.040    0.004    B/op
 * JarFileGetEntry.getEntryMiss           1024  avgt   10   26.489    1.737   ns/op
 *   gc.alloc.rate.norm                   1024  avgt   10   16.001    0.001    B/op
 * JarFileGetEntry.getEntryMissUncached   1024  avgt   10   74.189    3.320   ns/op
 *   gc.alloc.rate.norm                   1024  avgt   10   72.194    0.001    B/op
 *
 * After JDK-8193066
 * Benchmark                            (size)  Mode  Cnt    Score    Error   Units
 * JarFileGetEntry.getEntryHit            1024  avgt   10   98.075    3.718   ns/op
 *   gc.alloc.rate.norm                   1024  avgt   10  128.034    0.007    B/op
 * JarFileGetEntry.getEntryHitUncached    1024  avgt   10  132.998    5.937   ns/op
 *   gc.alloc.rate.norm                   1024  avgt   10  184.039    0.009    B/op
 * JarFileGetEntry.getEntryMiss           1024  avgt   10   24.043    0.930   ns/op
 *   gc.alloc.rate.norm                   1024  avgt   10    0.001    0.001    B/op
 * JarFileGetEntry.getEntryMissUncached   1024  avgt   10   65.840    3.296   ns/op
 *   gc.alloc.rate.norm                   1024  avgt   10   56.192    0.003    B/op
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
public class JarFileGetEntry {

    @Param({"512", "1024"})
    private int size;

    public JarFile jarFile;
    public String[]         entryNames;
    public String[]         missingEntryNames;
    public StringBuilder[]  entryNameBuilders;
    public StringBuilder[]  missingEntryNameBuilders;

    public int index = 0;

    @Setup(Level.Trial)
    public void beforeRun() throws IOException {
        // Create a test Zip file with the number of entries.
        File tempFile = Files.createTempFile("jar-mr-micro", ".jar").toFile();
        tempFile.deleteOnExit();

        entryNameBuilders = new StringBuilder[size];
        missingEntryNameBuilders = new StringBuilder[size];

        entryNames = new String[size];
        missingEntryNames = new String[size];

        try (FileOutputStream fos = new FileOutputStream(tempFile);
             JarOutputStream jos = new JarOutputStream(fos)) {

            Random random = new Random(4711);
            for (int i = 0; i < size; i++) {
                String ename = "entry-" + (random.nextInt(90000) + 10000) + "-" + i;
                jos.putNextEntry(new ZipEntry(ename));

                entryNames[i] = ename;
                entryNameBuilders[i] = new StringBuilder(ename);

                missingEntryNames[i] = ename + "-";
                missingEntryNameBuilders[i] = new StringBuilder(missingEntryNames[i]);
            }
        }

        jarFile = new JarFile(tempFile);
    }

    @Benchmark
    public void getEntryHit() {
        if (index >= size) {
            index = 0;
        }
        jarFile.getEntry(entryNames[index++]);
    }

    @Benchmark
    public void getEntryMiss() {
        if (index >= size) {
            index = 0;
        }
        jarFile.getEntry(missingEntryNames[index++]);
    }

    @Benchmark
    public void getEntryHitUncached() {
        if (index >= size) {
            index = 0;
        }
        jarFile.getEntry(entryNameBuilders[index++].toString());
    }

    @Benchmark
    public void getEntryMissUncached() {
        if (index >= size) {
            index = 0;
        }
        jarFile.getEntry(missingEntryNameBuilders[index++].toString());
    }
}
