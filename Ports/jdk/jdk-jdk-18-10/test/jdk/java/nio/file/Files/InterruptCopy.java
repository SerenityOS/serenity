/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4313887 6993267
 * @summary Unit test for Sun-specific ExtendedCopyOption.INTERRUPTIBLE option
 * @modules jdk.unsupported
 * @library ..
 */

import java.nio.file.*;
import java.io.*;
import java.util.concurrent.*;
import com.sun.nio.file.ExtendedCopyOption;

public class InterruptCopy {

    private static final long FILE_SIZE_TO_COPY = 512L * 1024L * 1024L;
    private static final int DELAY_IN_MS = 500;
    private static final int DURATION_MAX_IN_MS = 5000;

    public static void main(String[] args) throws Exception {
        Path dir = TestUtil.createTemporaryDirectory();
        try {
            FileStore store = Files.getFileStore(dir);
            System.out.format("Checking space (%s)\n", store);
            long usableSpace = store.getUsableSpace();
            if (usableSpace < 2*FILE_SIZE_TO_COPY) {
                System.out.println("Insufficient disk space to run test.");
                return;
            }
            doTest(dir);
        } finally {
            TestUtil.removeAll(dir);
        }
    }

    static void doTest(Path dir) throws Exception {
        final Path source = dir.resolve("foo");
        final Path target = dir.resolve("bar");

        // create source file (don't create it as sparse file because we
        // require the copy to take a long time)
        System.out.println("Creating source file...");
        byte[] buf = new byte[32*1024];
        long total = 0;
        try (OutputStream out = Files.newOutputStream(source)) {
            do {
                out.write(buf);
                total += buf.length;
            } while (total < FILE_SIZE_TO_COPY);
        }
        System.out.println("Source file created.");

        ScheduledExecutorService pool =
            Executors.newSingleThreadScheduledExecutor();
        try {
            // copy source to target in main thread, interrupting it after a delay
            final Thread me = Thread.currentThread();
            Future<?> wakeup = pool.schedule(new Runnable() {
                public void run() {
                    me.interrupt();
                }}, DELAY_IN_MS, TimeUnit.MILLISECONDS);
            System.out.println("Copying file...");
            try {
                long start = System.currentTimeMillis();
                Files.copy(source, target, ExtendedCopyOption.INTERRUPTIBLE);
                long duration = System.currentTimeMillis() - start;
                if (duration > DURATION_MAX_IN_MS)
                    throw new RuntimeException("Copy was not interrupted");
            } catch (IOException e) {
                boolean interrupted = Thread.interrupted();
                if (!interrupted)
                    throw new RuntimeException("Interrupt status was not set");
                System.out.println("Copy failed (this is expected)");
            }
            try {
                wakeup.get();
            } catch (InterruptedException ignore) { }
            Thread.interrupted();

            // copy source to target via task in thread pool, interrupting it after
            // a delay using cancel(true)
            Future<Void> result = pool.submit(new Callable<Void>() {
                public Void call() throws IOException {
                    System.out.println("Copying file...");
                    Files.copy(source, target, ExtendedCopyOption.INTERRUPTIBLE,
                        StandardCopyOption.REPLACE_EXISTING);
                    return null;
                }
            });
            Thread.sleep(DELAY_IN_MS);
            boolean cancelled = result.cancel(true);
            if (!cancelled)
                result.get();
            System.out.println("Copy cancelled.");
        } finally {
            pool.shutdown();
        }
    }
}
