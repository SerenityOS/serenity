/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8231254
 * @requires os.family == "mac"
 * @summary Check access and basic NIO APIs on APFS for macOS version >= 10.15
 */
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

public class MacVolumesTest {
    private static final String SYSTEM_VOLUME = "/";
    private static final String DATA_VOLUME = "/System/Volumes/Data";
    private static final String FIRMLINKS = "/usr/share/firmlinks";

    private static final void checkSystemVolume() throws IOException {
        System.out.format("--- Checking system volume %s ---%n", SYSTEM_VOLUME);
        Path root = Path.of(SYSTEM_VOLUME);
        if (!Files.getFileStore(root).isReadOnly()) {
            throw new RuntimeException("Root volume is not read-only");
        }

        Path tempDir;
        try {
            tempDir = Files.createTempDirectory(root, "tempDir");
            throw new RuntimeException("Created temporary directory in root");
        } catch (IOException ignore) {
        }

        Path tempFile;
        try {
            tempFile = Files.createTempFile(root, "tempFile", null);
            throw new RuntimeException("Created temporary file in root");
        } catch (IOException ignore) {
        }

        Path path = null;
        Path etc = Path.of(SYSTEM_VOLUME, "etc");
        if (Files.isWritable(etc)) {
            throw new RuntimeException("System path " + etc + " is writable");
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(etc)) {
            Iterator<Path> paths = ds.iterator();
            while (paths.hasNext()) {
                Path p = paths.next();
                if (Files.isReadable(p) && Files.isRegularFile(p)) {
                    path = p;
                    break;
                }
            }
        }
        if (path == null) {
            System.err.println("No root test file found: skipping file test");
            return;
        }
        System.out.format("Using root test file %s%n", path);

        if (Files.isWritable(path)) {
            throw new RuntimeException("Test file " + path + " is writable");
        }

        FileTime creationTime =
            (FileTime)Files.getAttribute(path, "basic:creationTime");
        System.out.format("%s creation time: %s%n", path, creationTime);

        long size = Files.size(path);
        int capacity = (int)Math.min(1024, size);
        ByteBuffer buf = ByteBuffer.allocate(capacity);
        try (SeekableByteChannel sbc = Files.newByteChannel(path)) {
            int n = sbc.read(buf);
            System.out.format("Read %d bytes from %s%n", n, path);
        }
    }

    private static final void checkDataVolume() throws IOException {
        System.out.format("--- Checking data volume %s ---%n", DATA_VOLUME);
        Path data = Path.of(DATA_VOLUME, "private", "tmp");
        if (Files.getFileStore(data).isReadOnly()) {
            throw new RuntimeException("Data volume is read-only");
        }

        Path tempDir = Files.createTempDirectory(data, "tempDir");
        tempDir.toFile().deleteOnExit();
        System.out.format("Temporary directory: %s%n", tempDir);
        if (!Files.isWritable(tempDir)) {
            throw new RuntimeException("Temporary directory is not writable");
        }

        Path tempFile = Files.createTempFile(tempDir, "tempFile", null);
        tempFile.toFile().deleteOnExit();
        System.out.format("Temporary file: %s%n", tempFile);
        if (!Files.isWritable(tempFile)) {
            throw new RuntimeException("Temporary file is not writable");
        }

        byte[] bytes = new byte[42];
        new Random().nextBytes(bytes);
        try (SeekableByteChannel sbc = Files.newByteChannel(tempFile,
            StandardOpenOption.WRITE)) {
            ByteBuffer src = ByteBuffer.wrap(bytes);
            if (sbc.write(src) != bytes.length) {
                throw new RuntimeException("Incorrect number of bytes written");
            }
        }

        try (SeekableByteChannel sbc = Files.newByteChannel(tempFile)) {
            ByteBuffer dst = ByteBuffer.allocate(bytes.length);
            if (sbc.read(dst) != bytes.length) {
                throw new RuntimeException("Incorrect number of bytes read");
            }
            if (!Arrays.equals(dst.array(), bytes)) {
                throw new RuntimeException("Bytes read != bytes written");
            }
        }
    }

    static void checkFirmlinks() throws IOException {
        System.out.format("--- Checking firmlinks %s ---%n", FIRMLINKS);
        Path firmlinks = Path.of(FIRMLINKS);
        if (!Files.exists(firmlinks)) {
            System.err.format("%s does not exist: skipping firmlinks test%n",
                firmlinks);
            return;
        } else if (!Files.isReadable(firmlinks)) {
            throw new RuntimeException(String.format("%s is not readable",
                firmlinks));
        }

        try (BufferedReader br = Files.newBufferedReader(firmlinks)) {
            String line;
            while ((line = br.readLine()) != null) {
                String file = line.split("\\s")[0];
                Path path = Path.of(file);
                if (!Files.exists(path)) {
                    System.err.format("Firmlink %s does not exist: skipping%n",
                        file);
                    continue;
                }
                if (Files.getFileStore(path).isReadOnly()) {
                    String msg = String.format("%s is read-only%n", file);
                    throw new RuntimeException(msg);
                } else {
                    System.out.format("Firmlink %s OK%n", file);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String[] osv = System.getProperty("os.version").split("\\.");
        int major = Integer.valueOf(osv[0]);
        int minor = Integer.valueOf(osv[1]);
        if (major < 10 || (major == 10 && minor < 15)) {
            System.out.format("macOS version %d.%d too old: skipping test%n",
                major, minor);
            return;
        }

        // Check system volume for read-only.
        checkSystemVolume();

        // Check data volume for read-write.
        checkDataVolume();

        // Check firmlinks for read-write.
        checkFirmlinks();
    }
}
