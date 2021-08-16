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
 * @run testng AsyncShutdownNow
 * @summary Test invoking shutdownNow with threads blocked in Future.get,
 *          invokeAll, and invokeAny
 */

// TODO: this test is far too slow

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class AsyncShutdownNow {

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
     * The executors to test.
     */
    @DataProvider(name = "executors")
    public Object[][] executors() {
        return new Object[][] {
                { new ForkJoinPool() },
                { new ForkJoinPool(1) },
        };
    }

    /**
     * Test shutdownNow with running task and thread blocked in Future::get.
     */
    @Test(dataProvider = "executors")
    public void testFutureGet(ExecutorService executor) throws Exception {
        System.out.format("testFutureGet: %s%n", executor);
        scheduleShutdownNow(executor, 5);
        try {
            // submit long running task, the task should be cancelled
            Future<?> future = executor.submit(SLEEP_FOR_A_DAY);
            try {
                future.get();
                assertTrue(false);
            } catch (ExecutionException | RejectedExecutionException e) {
                // expected
            }
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Test shutdownNow with running task and thread blocked in a timed Future::get.
     */
    @Test(dataProvider = "executors")
    public void testTimedFutureGet(ExecutorService executor) throws Exception {
        System.out.format("testTimedFutureGet: %s%n", executor);
        scheduleShutdownNow(executor, 5);
        try {
            // submit long running task, the task should be cancelled
            Future<?> future = executor.submit(SLEEP_FOR_A_DAY);
            try {
                future.get(1, TimeUnit.HOURS);
                assertTrue(false);
            } catch (ExecutionException | RejectedExecutionException e) {
                // expected
            }
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Test shutdownNow with thread blocked in invokeAll.
     */
    @Test(dataProvider = "executors")
    public void testInvokeAll(ExecutorService executor) throws Exception {
        System.out.format("testInvokeAll: %s%n", executor);
        scheduleShutdownNow(executor, 5);
        try {
            // execute long running tasks
            List<Future<Void>> futures = executor.invokeAll(List.of(SLEEP_FOR_A_DAY, SLEEP_FOR_A_DAY));
            for (Future<Void> f : futures) {
                assertTrue(f.isDone());
                try {
                    Object result = f.get();
                    assertTrue(false);
                } catch (ExecutionException | CancellationException e) {
                    // expected
                }
            }
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Test shutdownNow with thread blocked in invokeAny.
     */
    @Test(dataProvider = "executors")
    public void testInvokeAny(ExecutorService executor) throws Exception {
        System.out.format("testInvokeAny: %s%n", executor);
        scheduleShutdownNow(executor, 5);
        try {
            try {
                // execute long running tasks
                executor.invokeAny(List.of(SLEEP_FOR_A_DAY, SLEEP_FOR_A_DAY));
                assertTrue(false);
            } catch (ExecutionException | RejectedExecutionException e) {
                // expected
            }
        } finally {
            executor.shutdown();
        }
    }
}
