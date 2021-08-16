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

/**
 * @test
 * @bug 6857566
 * @summary DirectByteBuffer garbage creation can outpace reclamation
 *
 * @run main/othervm -XX:MaxDirectMemorySize=128m -XX:-ExplicitGCInvokesConcurrent DirectBufferAllocTest
 */

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DirectBufferAllocTest {
    // defaults
    static final int RUN_TIME_SECONDS = 5;
    static final int MIN_THREADS = 4;
    static final int MAX_THREADS = 64;
    static final int CAPACITY = 1024 * 1024; // bytes

    /**
     * This test spawns multiple threads that constantly allocate direct
     * {@link ByteBuffer}s in a loop, trying to provoke {@link OutOfMemoryError}.<p>
     * When run without command-line arguments, it runs as a regression test
     * for at most 5 seconds.<p>
     * Command line arguments:
     * <pre>
     * -r run-time-seconds <i>(duration of successful test - default 5 s)</i>
     * -t threads <i>(default is 2 * # of CPUs, at least 4 but no more than 64)</i>
     * -c capacity <i>(of direct buffers in bytes - default is 1MB)</i>
     * -p print-alloc-time-batch-size <i>(every "batch size" iterations,
     *                                 average time per allocation is printed)</i>
     * </pre>
     * Use something like the following to run a 10 minute stress test and
     * print allocation times as it goes:
     * <pre>
     * java -XX:MaxDirectMemorySize=128m DirectBufferAllocTest -r 600 -t 32 -p 5000
     * </pre>
     */
    public static void main(String[] args) throws Exception {
        int runTimeSeconds = RUN_TIME_SECONDS;
        int threads = Math.max(
            Math.min(
                Runtime.getRuntime().availableProcessors() * 2,
                MAX_THREADS
            ),
            MIN_THREADS
        );
        int capacity = CAPACITY;
        int printBatchSize = 0;

        // override with command line arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-r":
                    runTimeSeconds = Integer.parseInt(args[++i]);
                    break;
                case "-t":
                    threads = Integer.parseInt(args[++i]);
                    break;
                case "-c":
                    capacity = Integer.parseInt(args[++i]);
                    break;
                case "-p":
                    printBatchSize = Integer.parseInt(args[++i]);
                    break;
                default:
                    System.err.println(
                        "Usage: java" +
                        " [-XX:MaxDirectMemorySize=XXXm]" +
                        " DirectBufferAllocTest" +
                        " [-r run-time-seconds]" +
                        " [-t threads]" +
                        " [-c capacity-of-direct-buffers]" +
                        " [-p print-alloc-time-batch-size]"
                    );
                    System.exit(-1);
            }
        }

        System.out.printf(
            "Allocating direct ByteBuffers with capacity %d bytes, using %d threads for %d seconds...\n",
            capacity, threads, runTimeSeconds
        );

        ExecutorService executor = Executors.newFixedThreadPool(threads);

        int pbs = printBatchSize;
        int cap = capacity;

        List<Future<Void>> futures =
            IntStream.range(0, threads)
                     .mapToObj(
                         i -> (Callable<Void>) () -> {
                             long t0 = System.nanoTime();
                             loop:
                             while (true) {
                                 for (int n = 0; pbs == 0 || n < pbs; n++) {
                                     if (Thread.interrupted()) {
                                         break loop;
                                     }
                                     ByteBuffer.allocateDirect(cap);
                                 }
                                 long t1 = System.nanoTime();
                                 if (pbs > 0) {
                                     System.out.printf(
                                         "Thread %2d: %5.2f ms/allocation\n",
                                         i, ((double) (t1 - t0) / (1_000_000d * pbs))
                                     );
                                 }
                                 t0 = t1;
                             }
                             return null;
                         }
                     )
                     .map(executor::submit)
                     .collect(Collectors.toList());

        for (int i = 0; i < runTimeSeconds; i++) {
            if (futures.stream().anyMatch(Future::isDone)) {
                break;
            }
            Thread.sleep(1000L);
        }

        Exception exception = null;
        for (Future<Void> future : futures) {
            if (future.isDone()) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    if (exception == null) {
                        exception = new RuntimeException("Errors encountered!");
                    }
                    exception.addSuppressed(e.getCause());
                }
            } else {
                future.cancel(true);
            }
        }

        executor.shutdown();

        if (exception != null) {
            throw exception;
        } else {
            System.out.printf("No errors after %d seconds.\n", runTimeSeconds);
        }
    }
}
