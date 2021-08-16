/*
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 * Other contributors include Andrew Wright, Jeffrey Hayes,
 * Pat Fisher, Mike Judd.
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AbstractExecutorServiceTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(AbstractExecutorServiceTest.class);
    }

    /**
     * A no-frills implementation of AbstractExecutorService, designed
     * to test the submit methods only.
     */
    static class DirectExecutorService extends AbstractExecutorService {
        public void execute(Runnable r) { r.run(); }
        public void shutdown() { shutdown = true; }
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return Collections.emptyList();
        }
        public boolean isShutdown() { return shutdown; }
        public boolean isTerminated() { return isShutdown(); }
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return isShutdown();
        }
        private volatile boolean shutdown = false;
    }

    /**
     * execute(runnable) runs it to completion
     */
    public void testExecuteRunnable() throws Exception {
        ExecutorService e = new DirectExecutorService();
        final AtomicBoolean done = new AtomicBoolean(false);
        Future<?> future = e.submit(new CheckedRunnable() {
            public void realRun() {
                done.set(true);
            }});
        assertNull(future.get());
        assertNull(future.get(0, MILLISECONDS));
        assertTrue(done.get());
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
    }

    /**
     * Completed submit(callable) returns result
     */
    public void testSubmitCallable() throws Exception {
        ExecutorService e = new DirectExecutorService();
        Future<String> future = e.submit(new StringTask());
        String result = future.get();
        assertSame(TEST_STRING, result);
    }

    /**
     * Completed submit(runnable) returns successfully
     */
    public void testSubmitRunnable() throws Exception {
        ExecutorService e = new DirectExecutorService();
        Future<?> future = e.submit(new NoOpRunnable());
        future.get();
        assertTrue(future.isDone());
    }

    /**
     * Completed submit(runnable, result) returns result
     */
    public void testSubmitRunnable2() throws Exception {
        ExecutorService e = new DirectExecutorService();
        Future<String> future = e.submit(new NoOpRunnable(), TEST_STRING);
        String result = future.get();
        assertSame(TEST_STRING, result);
    }

    /**
     * A submitted privileged action runs to completion
     */
    public void testSubmitPrivilegedAction() throws Exception {
        Runnable r = new CheckedRunnable() {
            public void realRun() throws Exception {
                ExecutorService e = new DirectExecutorService();
                Future<?> future = e.submit(Executors.callable(new PrivilegedAction<Object>() {
                    public Object run() {
                        return TEST_STRING;
                    }}));

                assertSame(TEST_STRING, future.get());
            }};

        runWithPermissions(r,
                           new RuntimePermission("getClassLoader"),
                           new RuntimePermission("setContextClassLoader"),
                           new RuntimePermission("modifyThread"));
    }

    /**
     * A submitted privileged exception action runs to completion
     */
    public void testSubmitPrivilegedExceptionAction() throws Exception {
        Runnable r = new CheckedRunnable() {
            public void realRun() throws Exception {
                ExecutorService e = new DirectExecutorService();
                Future<?> future = e.submit(Executors.callable(new PrivilegedExceptionAction<Object>() {
                    public Object run() {
                        return TEST_STRING;
                    }}));

                assertSame(TEST_STRING, future.get());
            }};

        runWithPermissions(r);
    }

    /**
     * A submitted failed privileged exception action reports exception
     */
    public void testSubmitFailedPrivilegedExceptionAction() throws Exception {
        Runnable r = new CheckedRunnable() {
            public void realRun() throws Exception {
                ExecutorService e = new DirectExecutorService();
                Future<?> future = e.submit(Executors.callable(new PrivilegedExceptionAction<Object>() {
                    public Object run() throws Exception {
                        throw new IndexOutOfBoundsException();
                    }}));

                try {
                    future.get();
                    shouldThrow();
                } catch (ExecutionException success) {
                    assertTrue(success.getCause() instanceof IndexOutOfBoundsException);
                }}};

        runWithPermissions(r);
    }

    /**
     * Submitting null tasks throws NullPointerException
     */
    public void testNullTaskSubmission() {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            assertNullTaskSubmissionThrowsNullPointerException(e);
        }
    }

    /**
     * submit(callable).get() throws InterruptedException if interrupted
     */
    public void testInterruptedSubmit() throws InterruptedException {
        final CountDownLatch submitted    = new CountDownLatch(1);
        final CountDownLatch quittingTime = new CountDownLatch(1);
        final Callable<Void> awaiter = new CheckedCallable<>() {
            public Void realCall() throws InterruptedException {
                assertTrue(quittingTime.await(2*LONG_DELAY_MS, MILLISECONDS));
                return null;
            }};
        final ExecutorService p
            = new ThreadPoolExecutor(1,1,60, TimeUnit.SECONDS,
                                     new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p, quittingTime)) {
            Thread t = newStartedThread(new CheckedInterruptedRunnable() {
                public void realRun() throws Exception {
                    Future<Void> future = p.submit(awaiter);
                    submitted.countDown();
                    future.get();
                }});

            await(submitted);
            t.interrupt();
            awaitTermination(t);
        }
    }

    /**
     * get of submit(callable) throws ExecutionException if callable
     * throws exception
     */
    public void testSubmitEE() throws InterruptedException {
        final ThreadPoolExecutor p =
            new ThreadPoolExecutor(1, 1,
                                   60, TimeUnit.SECONDS,
                                   new ArrayBlockingQueue<Runnable>(10));
        try (PoolCleaner cleaner = cleaner(p)) {
            Callable<Object> c = new Callable<>() {
                public Object call() { throw new ArithmeticException(); }};
            try {
                p.submit(c).get();
                shouldThrow();
            } catch (ExecutionException success) {
                assertTrue(success.getCause() instanceof ArithmeticException);
            }
        }
    }

    /**
     * invokeAny(null) throws NPE
     */
    public void testInvokeAny1() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAny(null);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * invokeAny(empty collection) throws IllegalArgumentException
     */
    public void testInvokeAny2() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        final Collection<Callable<String>> emptyCollection
            = Collections.emptyList();
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAny(emptyCollection);
                shouldThrow();
            } catch (IllegalArgumentException success) {}
        }
    }

    /**
     * invokeAny(c) throws NPE if c has null elements
     */
    public void testInvokeAny3() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<Long>> l = new ArrayList<>();
            l.add(new Callable<Long>() {
                      public Long call() { throw new ArithmeticException(); }});
            l.add(null);
            try {
                e.invokeAny(l);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * invokeAny(c) throws ExecutionException if no task in c completes
     */
    public void testInvokeAny4() throws InterruptedException {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new NPETask());
            try {
                e.invokeAny(l);
                shouldThrow();
            } catch (ExecutionException success) {
                assertTrue(success.getCause() instanceof NullPointerException);
            }
        }
    }

    /**
     * invokeAny(c) returns result of some task in c if at least one completes
     */
    public void testInvokeAny5() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            l.add(new StringTask());
            String result = e.invokeAny(l);
            assertSame(TEST_STRING, result);
        }
    }

    /**
     * invokeAll(null) throws NPE
     */
    public void testInvokeAll1() throws InterruptedException {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAll(null);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * invokeAll(empty collection) returns empty list
     */
    public void testInvokeAll2() throws InterruptedException {
        final ExecutorService e = new DirectExecutorService();
        final Collection<Callable<String>> emptyCollection
            = Collections.emptyList();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Future<String>> r = e.invokeAll(emptyCollection);
            assertTrue(r.isEmpty());
        }
    }

    /**
     * invokeAll(c) throws NPE if c has null elements
     */
    public void testInvokeAll3() throws InterruptedException {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            l.add(null);
            try {
                e.invokeAll(l);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * get of returned element of invokeAll(c) throws exception on failed task
     */
    public void testInvokeAll4() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new NPETask());
            List<Future<String>> futures = e.invokeAll(l);
            assertEquals(1, futures.size());
            try {
                futures.get(0).get();
                shouldThrow();
            } catch (ExecutionException success) {
                assertTrue(success.getCause() instanceof NullPointerException);
            }
        }
    }

    /**
     * invokeAll(c) returns results of all completed tasks in c
     */
    public void testInvokeAll5() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            l.add(new StringTask());
            List<Future<String>> futures = e.invokeAll(l);
            assertEquals(2, futures.size());
            for (Future<String> future : futures)
                assertSame(TEST_STRING, future.get());
        }
    }

    /**
     * timed invokeAny(null) throws NPE
     */
    public void testTimedInvokeAny1() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAny(null, randomTimeout(), randomTimeUnit());
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * timed invokeAny(null time unit) throws NullPointerException
     */
    public void testTimedInvokeAnyNullTimeUnit() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            try {
                e.invokeAny(l, randomTimeout(), null);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * timed invokeAny(empty collection) throws IllegalArgumentException
     */
    public void testTimedInvokeAny2() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        final Collection<Callable<String>> emptyCollection
            = Collections.emptyList();
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAny(emptyCollection, randomTimeout(), randomTimeUnit());
                shouldThrow();
            } catch (IllegalArgumentException success) {}
        }
    }

    /**
     * timed invokeAny(c) throws NPE if c has null elements
     */
    public void testTimedInvokeAny3() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<Long>> l = new ArrayList<>();
            l.add(new Callable<Long>() {
                      public Long call() { throw new ArithmeticException(); }});
            l.add(null);
            try {
                e.invokeAny(l, randomTimeout(), randomTimeUnit());
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * timed invokeAny(c) throws ExecutionException if no task completes
     */
    public void testTimedInvokeAny4() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            long startTime = System.nanoTime();
            List<Callable<String>> l = new ArrayList<>();
            l.add(new NPETask());
            try {
                e.invokeAny(l, LONG_DELAY_MS, MILLISECONDS);
                shouldThrow();
            } catch (ExecutionException success) {
                assertTrue(success.getCause() instanceof NullPointerException);
            }
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
        }
    }

    /**
     * timed invokeAny(c) returns result of some task in c
     */
    public void testTimedInvokeAny5() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            long startTime = System.nanoTime();
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            l.add(new StringTask());
            String result = e.invokeAny(l, LONG_DELAY_MS, MILLISECONDS);
            assertSame(TEST_STRING, result);
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
        }
    }

    /**
     * timed invokeAll(null) throws NullPointerException
     */
    public void testTimedInvokeAll1() throws InterruptedException {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            try {
                e.invokeAll(null, randomTimeout(), randomTimeUnit());
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * timed invokeAll(null time unit) throws NPE
     */
    public void testTimedInvokeAllNullTimeUnit() throws InterruptedException {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            try {
                e.invokeAll(l, randomTimeout(), null);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * timed invokeAll(empty collection) returns empty list
     */
    public void testTimedInvokeAll2() throws InterruptedException {
        final ExecutorService e = new DirectExecutorService();
        final Collection<Callable<String>> emptyCollection
            = Collections.emptyList();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Future<String>> r =
                e.invokeAll(emptyCollection, randomTimeout(), randomTimeUnit());
            assertTrue(r.isEmpty());
        }
    }

    /**
     * timed invokeAll(c) throws NullPointerException if c has null elements
     */
    public void testTimedInvokeAll3() throws InterruptedException {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            l.add(null);
            try {
                e.invokeAll(l, randomTimeout(), randomTimeUnit());
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * get of returned element of invokeAll(c) throws exception on failed task
     */
    public void testTimedInvokeAll4() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new NPETask());
            List<Future<String>> futures =
                e.invokeAll(l, LONG_DELAY_MS, MILLISECONDS);
            assertEquals(1, futures.size());
            try {
                futures.get(0).get();
                shouldThrow();
            } catch (ExecutionException success) {
                assertTrue(success.getCause() instanceof NullPointerException);
            }
        }
    }

    /**
     * timed invokeAll(c) returns results of all completed tasks in c
     */
    public void testTimedInvokeAll5() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            List<Callable<String>> l = new ArrayList<>();
            l.add(new StringTask());
            l.add(new StringTask());
            List<Future<String>> futures =
                e.invokeAll(l, LONG_DELAY_MS, MILLISECONDS);
            assertEquals(2, futures.size());
            for (Future<String> future : futures)
                assertSame(TEST_STRING, future.get());
        }
    }

    /**
     * timed invokeAll cancels tasks not completed by timeout
     */
    public void testTimedInvokeAll6() throws Exception {
        final ExecutorService e = new DirectExecutorService();
        try (PoolCleaner cleaner = cleaner(e)) {
            for (long timeout = timeoutMillis();;) {
                List<Callable<String>> tasks = new ArrayList<>();
                tasks.add(new StringTask("0"));
                tasks.add(Executors.callable(possiblyInterruptedRunnable(timeout),
                                             TEST_STRING));
                tasks.add(new StringTask("2"));
                long startTime = System.nanoTime();
                List<Future<String>> futures =
                    e.invokeAll(tasks, timeout, MILLISECONDS);
                assertEquals(tasks.size(), futures.size());
                assertTrue(millisElapsedSince(startTime) >= timeout);
                for (Future<?> future : futures)
                    assertTrue(future.isDone());
                try {
                    assertEquals("0", futures.get(0).get());
                    assertEquals(TEST_STRING, futures.get(1).get());
                } catch (CancellationException retryWithLongerTimeout) {
                    // unusual delay before starting second task
                    timeout *= 2;
                    if (timeout >= LONG_DELAY_MS / 2)
                        fail("expected exactly one task to be cancelled");
                    continue;
                }
                assertTrue(futures.get(2).isCancelled());
                break;
            }
        }
    }

}
