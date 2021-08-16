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

package gc.stress;

/*
 * @test TestStressUncommit
 * @key stress
 * @summary Stress uncommitting by allocating and releasing memory
 * @requires vm.gc.G1
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @run driver/timeout=1300 gc.stress.TestStressG1Uncommit
 */

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.sun.management.ThreadMXBean;

import jdk.test.lib.Asserts;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class TestStressG1Uncommit {
    public static void main(String[] args) throws Exception {
        ArrayList<String> options = new ArrayList<>();
        Collections.addAll(options,
            "-Xlog:gc,gc+heap+region=debug",
            "-XX:+UseG1GC",
            StressUncommit.class.getName()
        );
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(options);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
        output.shouldMatch("Uncommit regions");
        output.outputTo(System.out);
    }
}

class StressUncommit {
    private static final long M = 1024 * 1024;
    private static final long G = 1024 * M;
    private static final Instant StartTime = Instant.now();

    private static final ThreadMXBean threadBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private static ConcurrentLinkedQueue<Object> globalKeepAlive;

    public static void main(String args[]) throws InterruptedException {
        // Leave 20% head room to try to avoid Full GCs.
        long allocationSize = (long) (Runtime.getRuntime().maxMemory() * 0.8);

        // Figure out suitable number of workers (~1 per gig).
        int gigsOfAllocation = (int) Math.ceil((double) allocationSize / G);
        int numWorkers = Math.min(gigsOfAllocation, Runtime.getRuntime().availableProcessors());
        long workerAllocation = allocationSize / numWorkers;

        log("Using " + numWorkers + " workers, each allocating: ~" + (workerAllocation / M) + "M");
        ExecutorService workers = Executors.newFixedThreadPool(numWorkers);
        try {
            int iteration = 1;
            // Run for 60 seconds.
            while (uptime() < 60) {
                log("Interation: " + iteration++);
                globalKeepAlive = new ConcurrentLinkedQueue<>();
                // Submit work to executor.
                CountDownLatch workersRunning = new CountDownLatch(numWorkers);
                for (int j = 0; j < numWorkers; j++) {
                    // Submit worker task.
                    workers.submit(() -> {
                    allocateToLimit(workerAllocation);
                    workersRunning.countDown();
                    });
                }

                // Wait for tasks to complete.
                workersRunning.await();

                // Clear the reference holding all task allocations alive.
                globalKeepAlive = null;

                // Do a GC that should shrink the heap.
                long committedBefore = memoryBean.getHeapMemoryUsage().getCommitted();
                System.gc();
                long committedAfter = memoryBean.getHeapMemoryUsage().getCommitted();
                Asserts.assertLessThan(committedAfter, committedBefore);
            }
        } finally {
            workers.shutdown();
            workers.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private static void allocateToLimit(long limit) {
        var localKeepAlive = new LinkedList<byte[]>();

        long currentAllocation = threadBean.getCurrentThreadAllocatedBytes();
        long allocationLimit = currentAllocation + limit;

        while (currentAllocation < allocationLimit) {
            // Check roughly every megabyte.
            for (long j = 0 ; j < 1000; j++) {
            localKeepAlive.add(new byte[1024]);
            }
            currentAllocation = threadBean.getCurrentThreadAllocatedBytes();
        }

        // Add to globalKeepAlive for realease by main thread.
        globalKeepAlive.add(localKeepAlive);
    }

    private static long uptime() {
        return Duration.between(StartTime, Instant.now()).getSeconds();
    }

    private static void log(String text) {
        System.out.println(uptime() + "s: " + text);
    }
}
