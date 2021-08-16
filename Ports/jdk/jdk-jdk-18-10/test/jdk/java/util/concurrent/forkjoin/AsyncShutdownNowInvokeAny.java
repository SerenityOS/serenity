/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @run testng AsyncShutdownNowInvokeAny
 * @summary A variant of AsyncShutdownNow useful for race bug hunting
 */

// TODO: reorganize all of the AsyncShutdown tests

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class AsyncShutdownNowInvokeAny {

    // long running interruptible task
    private static final Callable<Void> SLEEP_FOR_A_DAY = () -> {
        Thread.sleep(86400_000);
        return null;
    };

    private ScheduledExecutorService scheduledExecutor;

    @BeforeClass
    public void setup() {
        scheduledExecutor = Executors.newScheduledThreadPool(1);
    }

    @AfterClass
    public void teardown() {
        scheduledExecutor.shutdown();
    }

    /**
     * Schedule the given executor service to be shutdown abruptly after the given
     * delay, in seconds.
     */
    private void scheduleShutdownNow(ExecutorService executor, int delayInSeconds) {
        scheduledExecutor.schedule(() -> {
            executor.shutdownNow();
            return null;
        }, delayInSeconds, TimeUnit.SECONDS);
    }

    /**
     * Test shutdownNow with thread blocked in invokeAny.
     */
    @Test
    public void testInvokeAny() throws Exception {
        final int reps = 4;
        for (int rep = 1; rep < reps; rep++) {
            ExecutorService pool = new ForkJoinPool(1);
            scheduleShutdownNow(pool, 5);
            try {
                try {
                    // execute long running tasks
                    pool.invokeAny(List.of(SLEEP_FOR_A_DAY, SLEEP_FOR_A_DAY));
                    assertTrue(false);
                } catch (ExecutionException | RejectedExecutionException e) {
                    // expected
                }
            } finally {
                pool.shutdown();
            }
        }
    }
}
