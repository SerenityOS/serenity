/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8137121 8137230
 * @summary (fc) Infinite loop FileChannel.truncate
 * @library /test/lib
 * @build jdk.test.lib.Utils
 * @run main/othervm/timeout=300 LoopingTruncate
 */

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.*;
import java.util.concurrent.TimeUnit;
import static jdk.test.lib.Utils.adjustTimeout;

public class LoopingTruncate {

    // (int)FATEFUL_SIZE == -3 == IOStatus.INTERRUPTED
    static long FATEFUL_SIZE = 0x1FFFFFFFDL;

    // At least 20 seconds
    static long TIMEOUT = adjustTimeout(20_000);

    public static void main(String[] args) throws Throwable {
        Path path = Files.createTempFile("LoopingTruncate.tmp", null);
        try (FileChannel fc = FileChannel.open(path, CREATE, WRITE)) {
            fc.position(FATEFUL_SIZE + 1L);
            System.out.println("  Writing large file...");
            long t0 = System.nanoTime();
            fc.write(ByteBuffer.wrap(new byte[] {0}));
            long t1 = System.nanoTime();
            System.out.printf("  Wrote large file in %d ns (%d ms) %n",
                t1 - t0, TimeUnit.NANOSECONDS.toMillis(t1 - t0));

            Thread th = new Thread(() -> {
                try {
                    System.out.println("  Truncating large file...");
                    long t2 = System.nanoTime();
                    fc.truncate(FATEFUL_SIZE);
                    long t3 = System.nanoTime();
                    System.out.printf("  Truncated large file in %d ns (%d ms) %n",
                        t3 - t2, TimeUnit.NANOSECONDS.toMillis(t3 - t2));
                } catch (ClosedByInterruptException ignore) {
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }});
            th.start();
            th.join(TIMEOUT);

            if (th.isAlive()) {
                System.err.println("=== Stack trace of the guilty thread:");
                for (StackTraceElement el : th.getStackTrace()) {
                    System.err.println("\t" + el);
                }
                System.err.println("===");

                th.interrupt();
                th.join();
                throw new RuntimeException("Failed to complete on time");
            }
        } finally {
            Files.deleteIfExists(path);
        }

        System.out.println("Test succeeded.");
        System.out.flush();
    }
}
