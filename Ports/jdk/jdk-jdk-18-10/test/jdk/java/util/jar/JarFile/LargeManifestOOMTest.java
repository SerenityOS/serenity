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

import jdk.test.lib.util.JarUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;

/**
 * @test
 * @bug 8242882
 * @summary Verify that opening a jar file with a large manifest throws an OutOfMemoryError
 * and not a NegativeArraySizeException
 * @library /test/lib
 * @run testng LargeManifestOOMTest
 */
public class LargeManifestOOMTest {
    // file will be created with size greater than Integer.MAX_VALUE
    private static final long MANIFEST_FILE_SIZE = Integer.MAX_VALUE + 1024L;

    /**
     * Creates a jar which has a large manifest file and then uses the {@link JarFile} to
     * {@link JarFile#getManifest() load the manifest}. The call to the {@link JarFile#getManifest()}
     * is then expected to throw a {@link OutOfMemoryError}
     */
    @Test
    public void testOutOfMemoryError() throws Exception {
        final Path jarSourceRoot = Paths.get("jar-source");
        createLargeManifest(jarSourceRoot.resolve("META-INF"));
        final Path jarFilePath = Paths.get("oom-test.jar");
        JarUtils.createJarFile(jarFilePath.toAbsolutePath(), jarSourceRoot);
        final JarFile jar = new JarFile(jarFilePath.toFile());
        Assert.assertThrows(OutOfMemoryError.class, () -> jar.getManifest());
    }

    /**
     * Creates a {@code MANIFEST.MF}, whose content is {@link #MANIFEST_FILE_SIZE} in size,
     * in the {@code parentDir}
     *
     * @param parentDir The directory in which the MANIFEST.MF file will be created
     */
    private static void createLargeManifest(final Path parentDir) throws IOException {
        Files.createDirectories(parentDir.toAbsolutePath());
        final Path manifestFile = parentDir.resolve("MANIFEST.MF");
        try (final RandomAccessFile largeManifest = new RandomAccessFile(manifestFile.toFile(), "rw")) {
            largeManifest.writeUTF("Manifest-Version: 1.0\n");
            largeManifest.writeUTF("OOM-Test: a\n");
            largeManifest.setLength(MANIFEST_FILE_SIZE);
        }
        System.out.println("Size of file " + manifestFile + " is " + manifestFile.toFile().length());
    }
}
