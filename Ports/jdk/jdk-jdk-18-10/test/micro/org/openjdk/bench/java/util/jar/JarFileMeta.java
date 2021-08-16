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
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Simple benchmark measuring cost of various operations relating to jar
 * meta-inf and manifests, especially costs incurred during opening of the
 * file, and when opening an input stream (which runs
 * JarFile.maybeInstantiateVerifier)
 *
 * Before JDK-8244624:
 * Benchmark                          (size)  Mode  Cnt       Score    Error   Units
 * getManifestFromJarWithManifest       1024  avgt    5     232.437   31.535   us/op
 *   gc.alloc.rate.norm                 1024  avgt    5  206410.627    2.833    B/op
 * getStreamFromJarWithManifest         1024  avgt    5     277.696   32.078   us/op
 *   gc.alloc.rate.norm                 1024  avgt    5  250454.252    2.452    B/op
 * getStreamFromJarWithNoManifest       1024  avgt    5     312.432   58.663   us/op
 *   gc.alloc.rate.norm                 1024  avgt    5  301802.644   13.276    B/op
 * getStreamFromJarWithSignatureFiles   1024  avgt    5     315.752   55.048   us/op
 *   gc.alloc.rate.norm                 1024  avgt    5  305354.934   14.093    B/op
 *
 * With JDK-8244624:
 * Benchmark                          (size)  Mode  Cnt       Score    Error   Units
 * getManifestFromJarWithManifest       1024  avgt    5     215.242   32.085   us/op
 *   gc.alloc.rate.norm                 1024  avgt    5  196609.220   14.788    B/op
 * getStreamFromJarWithManifest         1024  avgt    5     216.435   10.876   us/op
 *   gc.alloc.rate.norm                 1024  avgt    5  187960.147    9.026    B/op
 * getStreamFromJarWithNoManifest       1024  avgt    5     204.256   25.744   us/op
 *   gc.alloc.rate.norm                 1024  avgt    5  186784.347    1.841    B/op
 * getStreamFromJarWithSignatureFiles   1024  avgt    5     247.972   38.574   us/op
 *   gc.alloc.rate.norm                 1024  avgt    5  211577.268   15.109    B/op
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
public class JarFileMeta {

    @Param({"512", "1024"})
    private int size;

    public File jarManifest;
    public File jarNoManifest;
    public File jarManifestSignature;

    public int index = 0;

    @Setup(Level.Trial)
    public void beforeRun() throws IOException {
        jarNoManifest = createJar(false, false);
        jarManifest = createJar(true, false);
        jarManifestSignature = createJar(true, true);
    }

    private File createJar(boolean manifest, boolean signatureFiles) throws IOException {
        // Create a test Zip file with the number of entries.
        File tempFile = Files.createTempFile("jar-micro", ".jar").toFile();
        tempFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(tempFile);
             JarOutputStream zos = manifest
                     ? new JarOutputStream(fos, new Manifest())
                     : new JarOutputStream(fos)) {

            // Always add this
            zos.putNextEntry(new ZipEntry("README"));

            Random random = new Random(4711);
            for (int i = 0; i < size; i++) {
                String ename = "directory-" + (random.nextInt(90000) + 10000) + "-" + i + "/";
                if (random.nextInt(100) > 70) {
                    ename = "META-INF/" + ename;

                }
                zos.putNextEntry(new ZipEntry(ename));

                ename += "entry-"  + (random.nextInt(90000) + 10000)  + "-" + i;
                if (signatureFiles && random.nextInt(100) > 95) {
                    ename += ".DSA";
                }
                zos.putNextEntry(new ZipEntry(ename));
            }
        }
        return tempFile;
    }

    private InputStream openGetStreamAndClose(File file) throws IOException {
        JarFile jf = new JarFile(file);
        InputStream is = jf.getInputStream(jf.getEntry("README"));
        jf.close();
        // we'll never actually read from the closed stream, rather just
        // return it to avoid DCE
        return is;
    }

    @Benchmark
    public InputStream getStreamFromJarWithManifest() throws IOException {
        return openGetStreamAndClose(jarManifest);
    }

    @Benchmark
    public InputStream getStreamFromJarWithNoManifest() throws IOException {
        return openGetStreamAndClose(jarNoManifest);
    }

    @Benchmark
    public InputStream getStreamFromJarWithSignatureFiles() throws IOException {
        return openGetStreamAndClose(jarManifestSignature);
    }

    @Benchmark
    public Manifest getManifestFromJarWithManifest() throws IOException {
        JarFile jf = new JarFile(jarManifest);
        Manifest mf = jf.getManifest();
        jf.close();
        return mf;
    }
}
