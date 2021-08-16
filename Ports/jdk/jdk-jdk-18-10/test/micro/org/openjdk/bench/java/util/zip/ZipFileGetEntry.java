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

package org.openjdk.bench.java.util.zip;

import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Simple benchmark measuring cost of looking up entries in a zip file.
 *
 * Uncached variants create a new String before lookup. Intent is to have
 * every String's hashCode cache be uninitialized, in case the implementation
 * uses it internally.
 *
 * Pre-JDK-8243469:
 * Benchmark                             (size)  Mode  Cnt    Score   Error  Units
 * ZipFileGetEntry.getEntryHit              512  avgt   15  126.264   5.297  ns/op
 * ZipFileGetEntry.getEntryHit             1024  avgt   15  130.823   7.212  ns/op
 * ZipFileGetEntry.getEntryHitUncached      512  avgt   15  152.149   4.978  ns/op
 * ZipFileGetEntry.getEntryHitUncached     1024  avgt   15  151.527   4.054  ns/op
 * ZipFileGetEntry.getEntryMiss             512  avgt   15   73.242   3.218  ns/op
 * ZipFileGetEntry.getEntryMiss            1024  avgt   15   82.115   5.961  ns/op
 * ZipFileGetEntry.getEntryMissUncached     512  avgt   15   94.313   5.533  ns/op
 * ZipFileGetEntry.getEntryMissUncached    1024  avgt   15   99.135   4.422  ns/op
 *
 * JDK-8243469:
 * Benchmark                             (size)  Mode  Cnt    Score   Error  Units
 * ZipFileGetEntry.getEntryHit              512  avgt   15   84.450   5.474  ns/op
 * ZipFileGetEntry.getEntryHit             1024  avgt   15   85.224   3.776  ns/op
 * ZipFileGetEntry.getEntryHitUncached      512  avgt   15  140.448   4.667  ns/op
 * ZipFileGetEntry.getEntryHitUncached     1024  avgt   15  145.046   7.363  ns/op
 * ZipFileGetEntry.getEntryMiss             512  avgt   15   19.093   0.169  ns/op
 * ZipFileGetEntry.getEntryMiss            1024  avgt   15   21.460   0.522  ns/op
 * ZipFileGetEntry.getEntryMissUncached     512  avgt   15   74.982   3.363  ns/op
 * ZipFileGetEntry.getEntryMissUncached    1024  avgt   15   81.856   3.648  ns/op
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
public class ZipFileGetEntry {

    @Param({"512", "1024"})
    private int size;

    public ZipFile          zipFile;
    public String[]         entryNames;
    public String[]         missingEntryNames;
    public StringBuilder[]  entryNameBuilders;
    public StringBuilder[]  missingEntryNameBuilders;

    public int index = 0;

    @Setup(Level.Trial)
    public void beforeRun() throws IOException {
        // Create a test Zip file with the number of entries.
        File tempFile = Files.createTempFile("zip-micro", ".zip").toFile();
        tempFile.deleteOnExit();

        entryNameBuilders = new StringBuilder[size];
        missingEntryNameBuilders = new StringBuilder[size];

        entryNames = new String[size];
        missingEntryNames = new String[size];
        try (FileOutputStream fos = new FileOutputStream(tempFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            Random random = new Random(4711);
            for (int i = 0; i < size; i++) {
                String ename = "directory-" + (random.nextInt(90000) + 10000) + "-" + i + "/";
                zos.putNextEntry(new ZipEntry(ename));

                ename += "entry-"  + (random.nextInt(90000) + 10000)  + "-" + i;
                zos.putNextEntry(new ZipEntry(ename));

                entryNames[i] = ename;
                entryNameBuilders[i] = new StringBuilder(ename);

                missingEntryNames[i] = ename + "-";
                missingEntryNameBuilders[i] = new StringBuilder(missingEntryNames[i]);
            }
        }
        zipFile = new ZipFile(tempFile);
    }

    @Benchmark
    public void getEntryHit() {
        if (index >= size) {
            index = 0;
        }
        zipFile.getEntry(entryNames[index++]);
    }

    @Benchmark
    public void getEntryMiss() {
        if (index >= size) {
            index = 0;
        }
        zipFile.getEntry(missingEntryNames[index++]);
    }

    @Benchmark
    public void getEntryHitUncached() {
        if (index >= size) {
            index = 0;
        }
        zipFile.getEntry(entryNameBuilders[index++].toString());
    }

    @Benchmark
    public void getEntryMissUncached() {
        if (index >= size) {
            index = 0;
        }
        zipFile.getEntry(missingEntryNameBuilders[index++].toString());
    }
}
