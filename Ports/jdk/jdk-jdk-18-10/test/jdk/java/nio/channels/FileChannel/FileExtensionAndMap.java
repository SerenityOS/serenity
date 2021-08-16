/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @ignore This test has huge disk space requirements
 * @bug 8168628
 * @summary Test extending files to very large sizes without hitting a SIGBUS
 * @requires (os.family == "linux")
 * @run main/othervm/timeout=600 -Xms4g -Xmx4g FileExtensionAndMap
 * @run main/othervm/timeout=600 -Xms4g -Xmx4g FileExtensionAndMap true
 */

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

public class FileExtensionAndMap {

    private static final ExecutorService CACHED_EXECUTORSERVICE =
        Executors.newCachedThreadPool();

    private static final String TMPDIR = System.getProperty("test.dir", ".");

    private static boolean useRaf = false;

    public static void main(String args[]) throws Exception {
        if (args.length > 2) {
            throw new IllegalArgumentException
                ("Arguments: [true|false [targetFolder]]");
        }

        String defaultFolder = TMPDIR + File.separator + "target";
        if (args.length > 0) {
            useRaf = Boolean.valueOf(args[0]);
            if (args.length > 1) {
                defaultFolder = args[1];
            }
        }
        final String targetFolder = defaultFolder;
        Path p = Paths.get(targetFolder);
        boolean targetExists = Files.exists(p);
        if (!targetExists) {
            Files.createDirectory(p);
        }

        System.out.printf("Using RandomAccessFile: %s; target folder: %s%n",
            useRaf, targetFolder);

        ForkJoinPool fjPool = new ForkJoinPool(3);
        fjPool.submit(() -> {
            IntStream.range(0, 20).parallel().forEach((index) -> {
                String fileName = "testBigFile_" + index + ".dat";
                Path source = null;
                Path target = null;
                try {
                    source = Paths.get(TMPDIR, fileName);
                    testCreateBigFile(source);
                    target = Paths.get(targetFolder, fileName);
                    testFileCopy(source, target);
                } catch (Throwable th) {
                    System.err.println("Error copying file with fileName: "
                        + fileName + " : " + th.getMessage());
                    th.printStackTrace(System.err);
                } finally {
                    try {
                        if (source != null) {
                            Files.deleteIfExists(source);
                        }
                    } catch (Throwable ignored) {
                    }
                    try {
                        if (target != null) {
                            Files.deleteIfExists(target);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            });
        }).join();

        if (!targetExists) {
            Files.delete(p);
        }
    }

    private static void testFileCopy(Path source, Path target)
        throws IOException {
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Finished copying file with fileName: "
                + source.getFileName());
    }

    private static void testCreateBigFile(Path segmentFile)
        throws IOException {
        final Semaphore concurrencySemaphore = new Semaphore(5);
        long fileSize = 3L * 1024L * 1024L * 1024L;
        int blockSize = 10 * 1024 * 1024;
        int loopCount = (int) Math.floorDiv(fileSize, blockSize);

        String fileName = segmentFile.getFileName().toString();
        if (useRaf) {
            try (RandomAccessFile raf
                = new RandomAccessFile(segmentFile.toFile(), "rw")) {
                raf.setLength(fileSize);
                try (FileChannel fc = raf.getChannel()) {
                    for (int i = 0; i < loopCount; i++) {
                        final long startPosition = 1L * blockSize * i;
                        concurrencySemaphore.acquireUninterruptibly();
                        CACHED_EXECUTORSERVICE.submit(() -> {
                            writeTemplateData(fileName, fc, startPosition,
                                    blockSize, concurrencySemaphore);
                        });
                    }
                } finally {
                    concurrencySemaphore.acquireUninterruptibly(5);
                }
            }
        } else {
            Path file = Files.createFile(segmentFile);
            try (FileChannel fc = FileChannel.open(file,
                StandardOpenOption.READ, StandardOpenOption.WRITE)) {
                for (int i = 0; i < loopCount; i++) {
                    final long startPosition = 1L * blockSize * i;
                    concurrencySemaphore.acquireUninterruptibly();
                    CACHED_EXECUTORSERVICE.submit(() -> {
                        writeTemplateData(fileName, fc, startPosition,
                                blockSize, concurrencySemaphore);
                    });
                }
            }
        }
    }

    private static void writeTemplateData(String fileName,
        FileChannel fc, long startPosition, int blockSize,
        Semaphore concurrencySemaphore) {
        try {
            byte[] EMPTY_RECORD = new byte[blockSize / 256];

            MappedByteBuffer mappedByteBuffer = fc.map(MapMode.READ_WRITE,
                startPosition, blockSize);
            IntStream.range(0, 256).forEach((recordIndex) -> {
                try {
                    mappedByteBuffer.position((int) (recordIndex *
                        EMPTY_RECORD.length));
                    mappedByteBuffer.put(EMPTY_RECORD, 0, EMPTY_RECORD.length);
                } catch (Throwable th) {
                    System.err.println
                        ("Error in FileExtensionAndMap.writeTemplateData empty record for fileName: "
                        + fileName + ", startPosition: " + startPosition + ", recordIndex: "
                        + recordIndex + " : " + th.getMessage());
                    th.printStackTrace(System.err);
                }
            });

            mappedByteBuffer.force();
        } catch (Throwable th) {
            if (!(th instanceof ClosedChannelException)) {
                System.err.println
                    ("Error in FileExtensionAndMap.writeTemplateData empty record for fileName: "
                    + fileName + ", startPosition: " + startPosition + " : "
                    + th.getMessage());
                th.printStackTrace(System.err);
            }
        } finally {
            concurrencySemaphore.release();
        }
    }
}
