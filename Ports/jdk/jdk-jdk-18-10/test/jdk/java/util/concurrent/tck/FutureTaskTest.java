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
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Test;
import junit.framework.TestSuite;

public class FutureTaskTest extends JSR166TestCase {

    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(FutureTaskTest.class);
    }

    void checkIsDone(Future<?> f) {
        assertTrue(f.isDone());
        assertFalse(f.cancel(false));
        assertFalse(f.cancel(true));
        if (f instanceof PublicFutureTask) {
            PublicFutureTask pf = (PublicFutureTask) f;
            assertEquals(1, pf.doneCount());
            assertFalse(pf.runAndReset());
            assertEquals(1, pf.doneCount());
            Object r = null; Object exInfo = null;
            try {
                r = f.get();
            } catch (CancellationException t) {
                exInfo = CancellationException.class;
            } catch (ExecutionException t) {
                exInfo = t.getCause();
            } catch (Throwable t) {
                threadUnexpectedException(t);
            }

            // Check that run and runAndReset have no effect.
            int savedRunCount = pf.runCount();
            pf.run();
            pf.runAndReset();
            assertEquals(savedRunCount, pf.runCount());
            Object r2 = null;
            try {
                r2 = f.get();
            } catch (CancellationException t) {
                assertSame(exInfo, CancellationException.class);
            } catch (ExecutionException t) {
                assertSame(exInfo, t.getCause());
            } catch (Throwable t) {
                threadUnexpectedException(t);
            }
            if (exInfo == null)
                assertSame(r, r2);
            assertTrue(f.isDone());
        }
    }

    void checkNotDone(Future<?> f) {
        assertFalse(f.isDone());
        assertFalse(f.isCancelled());
        if (f instanceof PublicFutureTask) {
            PublicFutureTask pf = (PublicFutureTask) f;
            assertEquals(0, pf.doneCount());
            assertEquals(0, pf.setCount());
            assertEquals(0, pf.setExceptionCount());
        }
    }

    void checkIsRunning(Future<?> f) {
        checkNotDone(f);
        if (f instanceof FutureTask) {
            FutureTask<?> ft = (FutureTask<?>) f;
            // Check that run methods do nothing
            ft.run();
            if (f instanceof PublicFutureTask) {
                PublicFutureTask pf = (PublicFutureTask) f;
                int savedRunCount = pf.runCount();
                pf.run();
                assertFalse(pf.runAndReset());
                assertEquals(savedRunCount, pf.runCount());
            }
            checkNotDone(f);
        }
    }

    <T> void checkCompletedNormally(Future<T> f, T expectedValue) {
        checkIsDone(f);
        assertFalse(f.isCancelled());

        T v1 = null, v2 = null;
        try {
            v1 = f.get();
            v2 = f.get(randomTimeout(), randomTimeUnit());
        } catch (Throwable fail) { threadUnexpectedException(fail); }
        assertSame(expectedValue, v1);
        assertSame(expectedValue, v2);
    }

    void checkCancelled(Future<?> f) {
        checkIsDone(f);
        assertTrue(f.isCancelled());

        try {
            f.get();
            shouldThrow();
        } catch (CancellationException success) {
        } catch (Throwable fail) { threadUnexpectedException(fail); }

        try {
            f.get(randomTimeout(), randomTimeUnit());
            shouldThrow();
        } catch (CancellationException success) {
        } catch (Throwable fail) { threadUnexpectedException(fail); }
    }

    void tryToConfuseDoneTask(PublicFutureTask pf) {
        pf.set(new Object());
        pf.setException(new Error());
        for (boolean mayInterruptIfRunning : new boolean[] { true, false }) {
            pf.cancel(mayInterruptIfRunning);
        }
    }

    void checkCompletedAbnormally(Future<?> f, Throwable t) {
        checkIsDone(f);
        assertFalse(f.isCancelled());

        try {
            f.get();
            shouldThrow();
        } catch (ExecutionException success) {
            assertSame(t, success.getCause());
        } catch (Throwable fail) { threadUnexpectedException(fail); }

        try {
            f.get(randomTimeout(), randomTimeUnit());
            shouldThrow();
        } catch (ExecutionException success) {
            assertSame(t, success.getCause());
        } catch (Throwable fail) { threadUnexpectedException(fail); }
    }

    /**
     * Subclass to expose protected methods
     */
    static class PublicFutureTask extends FutureTask<Object> {
        private final AtomicInteger runCount;
        private final AtomicInteger doneCount = new AtomicInteger(0);
        private final AtomicInteger runAndResetCount = new AtomicInteger(0);
        private final AtomicInteger setCount = new AtomicInteger(0);
        private final AtomicInteger setExceptionCount = new AtomicInteger(0);
        public int runCount() { return runCount.get(); }
        public int doneCount() { return doneCount.get(); }
        public int runAndResetCount() { return runAndResetCount.get(); }
        public int setCount() { return setCount.get(); }
        public int setExceptionCount() { return setExceptionCount.get(); }

        PublicFutureTask(Runnable runnable) {
            this(runnable, seven);
        }
        PublicFutureTask(Runnable runnable, Object result) {
            this(runnable, result, new AtomicInteger(0));
        }
        private PublicFutureTask(final Runnable runnable, Object result,
                                 final AtomicInteger runCount) {
            super(new Runnable() {
                public void run() {
                    runCount.getAndIncrement();
                    runnable.run();
                }}, result);
            this.runCount = runCount;
        }
        PublicFutureTask(Callable<?> callable) {
            this(callable, new AtomicInteger(0));
        }
        private PublicFutureTask(final Callable<?> callable,
                                 final AtomicInteger runCount) {
            super(new Callable<Object>() {
                public Object call() throws Exception {
                    runCount.getAndIncrement();
                    return callable.call();
                }});
            this.runCount = runCount;
        }
        @Override public void done() {
            assertTrue(isDone());
            doneCount.incrementAndGet();
            super.done();
        }
        @Override public boolean runAndReset() {
            runAndResetCount.incrementAndGet();
            return super.runAndReset();
        }
        @Override public void set(Object x) {
            setCount.incrementAndGet();
            super.set(x);
        }
        @Override public void setException(Throwable t) {
            setExceptionCount.incrementAndGet();
            super.setException(t);
        }
    }

    class Counter extends CheckedRunnable {
        final AtomicInteger count = new AtomicInteger(0);
        public int get() { return count.get(); }
        public void realRun() {
            count.getAndIncrement();
        }
    }

    /**
     * creating a future with a null callable throws NullPointerException
     */
    public void testConstructor() {
        try {
            new FutureTask<Void>(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * creating a future with null runnable throws NullPointerException
     */
    public void testConstructor2() {
        try {
            new FutureTask<Boolean>(null, Boolean.TRUE);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * isDone is true when a task completes
     */
    public void testIsDone() {
        PublicFutureTask task = new PublicFutureTask(new NoOpCallable());
        assertFalse(task.isDone());
        task.run();
        assertTrue(task.isDone());
        checkCompletedNormally(task, Boolean.TRUE);
        assertEquals(1, task.runCount());
    }

    /**
     * runAndReset of a non-cancelled task succeeds
     */
    public void testRunAndReset() {
        PublicFutureTask task = new PublicFutureTask(new NoOpCallable());
        for (int i = 0; i < 3; i++) {
            assertTrue(task.runAndReset());
            checkNotDone(task);
            assertEquals(i + 1, task.runCount());
            assertEquals(i + 1, task.runAndResetCount());
            assertEquals(0, task.setCount());
            assertEquals(0, task.setExceptionCount());
        }
    }

    /**
     * runAndReset after cancellation fails
     */
    public void testRunAndResetAfterCancel() {
        for (boolean mayInterruptIfRunning : new boolean[] { true, false }) {
            PublicFutureTask task = new PublicFutureTask(new NoOpCallable());
            assertTrue(task.cancel(mayInterruptIfRunning));
            for (int i = 0; i < 3; i++) {
                assertFalse(task.runAndReset());
                assertEquals(0, task.runCount());
                assertEquals(i + 1, task.runAndResetCount());
                assertEquals(0, task.setCount());
                assertEquals(0, task.setExceptionCount());
            }
            tryToConfuseDoneTask(task);
            checkCancelled(task);
        }
    }

    /**
     * setting value causes get to return it
     */
    public void testSet() throws Exception {
        PublicFutureTask task = new PublicFutureTask(new NoOpCallable());
        task.set(one);
        for (int i = 0; i < 3; i++) {
            assertSame(one, task.get());
            assertSame(one, task.get(LONG_DELAY_MS, MILLISECONDS));
            assertEquals(1, task.setCount());
        }
        tryToConfuseDoneTask(task);
        checkCompletedNormally(task, one);
        assertEquals(0, task.runCount());
    }

    /**
     * setException causes get to throw ExecutionException
     */
    public void testSetException_get() throws Exception {
        Exception nse = new NoSuchElementException();
        PublicFutureTask task = new PublicFutureTask(new NoOpCallable());
        task.setException(nse);

        try {
            task.get();
            shouldThrow();
        } catch (ExecutionException success) {
            assertSame(nse, success.getCause());
            checkCompletedAbnormally(task, nse);
        }

        try {
            task.get(LONG_DELAY_MS, MILLISECONDS);
            shouldThrow();
        } catch (ExecutionException success) {
            assertSame(nse, success.getCause());
            checkCompletedAbnormally(task, nse);
        }

        assertEquals(1, task.setExceptionCount());
        assertEquals(0, task.setCount());
        tryToConfuseDoneTask(task);
        checkCompletedAbnormally(task, nse);
        assertEquals(0, task.runCount());
    }

    /**
     * cancel(false) before run succeeds
     */
    public void testCancelBeforeRun() {
        PublicFutureTask task = new PublicFutureTask(new NoOpCallable());
        assertTrue(task.cancel(false));
        task.run();
        assertEquals(0, task.runCount());
        assertEquals(0, task.setCount());
        assertEquals(0, task.setExceptionCount());
        assertTrue(task.isCancelled());
        assertTrue(task.isDone());
        tryToConfuseDoneTask(task);
        assertEquals(0, task.runCount());
        checkCancelled(task);
    }

    /**
     * cancel(true) before run succeeds
     */
    public void testCancelBeforeRun2() {
        PublicFutureTask task = new PublicFutureTask(new NoOpCallable());
        assertTrue(task.cancel(true));
        task.run();
        assertEquals(0, task.runCount());
        assertEquals(0, task.setCount());
        assertEquals(0, task.setExceptionCount());
        assertTrue(task.isCancelled());
        assertTrue(task.isDone());
        tryToConfuseDoneTask(task);
        assertEquals(0, task.runCount());
        checkCancelled(task);
    }

    /**
     * cancel(false) of a completed task fails
     */
    public void testCancelAfterRun() {
        PublicFutureTask task = new PublicFutureTask(new NoOpCallable());
        task.run();
        assertFalse(task.cancel(false));
        assertEquals(1, task.runCount());
        assertEquals(1, task.setCount());
        assertEquals(0, task.setExceptionCount());
        tryToConfuseDoneTask(task);
        checkCompletedNormally(task, Boolean.TRUE);
        assertEquals(1, task.runCount());
    }

    /**
     * cancel(true) of a completed task fails
     */
    public void testCancelAfterRun2() {
        PublicFutureTask task = new PublicFutureTask(new NoOpCallable());
        task.run();
        assertFalse(task.cancel(true));
        assertEquals(1, task.runCount());
        assertEquals(1, task.setCount());
        assertEquals(0, task.setExceptionCount());
        tryToConfuseDoneTask(task);
        checkCompletedNormally(task, Boolean.TRUE);
        assertEquals(1, task.runCount());
    }

    /**
     * cancel(true) interrupts a running task that subsequently succeeds
     */
    public void testCancelInterrupt() {
        final CountDownLatch pleaseCancel = new CountDownLatch(1);
        final PublicFutureTask task =
            new PublicFutureTask(new CheckedRunnable() {
                public void realRun() {
                    pleaseCancel.countDown();
                    try {
                        delay(LONG_DELAY_MS);
                        shouldThrow();
                    } catch (InterruptedException success) {}
                    assertFalse(Thread.interrupted());
                }});

        Thread t = newStartedThread(task);
        await(pleaseCancel);
        assertTrue(task.cancel(true));
        assertTrue(task.isCancelled());
        assertTrue(task.isDone());
        awaitTermination(t);
        assertEquals(1, task.runCount());
        assertEquals(1, task.setCount());
        assertEquals(0, task.setExceptionCount());
        tryToConfuseDoneTask(task);
        checkCancelled(task);
    }

    /**
     * cancel(true) tries to interrupt a running task, but
     * Thread.interrupt throws (simulating a restrictive security
     * manager)
     */
    public void testCancelInterrupt_ThrowsSecurityException() {
        final CountDownLatch pleaseCancel = new CountDownLatch(1);
        final CountDownLatch cancelled = new CountDownLatch(1);
        final PublicFutureTask task =
            new PublicFutureTask(new CheckedRunnable() {
                public void realRun() {
                    pleaseCancel.countDown();
                    await(cancelled);
                    assertFalse(Thread.interrupted());
                }});

        final Thread t = new Thread(task) {
            // Simulate a restrictive security manager.
            @Override public void interrupt() {
                throw new SecurityException();
            }};
        t.setDaemon(true);
        t.start();

        await(pleaseCancel);
        try {
            task.cancel(true);
            shouldThrow();
        } catch (SecurityException success) {}

        // We failed to deliver the interrupt, but the world retains
        // its sanity, as if we had done task.cancel(false)
        assertTrue(task.isCancelled());
        assertTrue(task.isDone());
        assertEquals(1, task.runCount());
        assertEquals(1, task.doneCount());
        assertEquals(0, task.setCount());
        assertEquals(0, task.setExceptionCount());
        cancelled.countDown();
        awaitTermination(t);
        assertEquals(1, task.setCount());
        assertEquals(0, task.setExceptionCount());
        tryToConfuseDoneTask(task);
        checkCancelled(task);
    }

    /**
     * cancel(true) interrupts a running task that subsequently throws
     */
    public void testCancelInterrupt_taskFails() {
        final CountDownLatch pleaseCancel = new CountDownLatch(1);
        final PublicFutureTask task =
            new PublicFutureTask(new Runnable() {
                public void run() {
                    pleaseCancel.countDown();
                    try {
                        delay(LONG_DELAY_MS);
                        threadShouldThrow();
                    } catch (InterruptedException success) {
                    } catch (Throwable t) { threadUnexpectedException(t); }
                    throw new RuntimeException();
                }});

        Thread t = newStartedThread(task);
        await(pleaseCancel);
        assertTrue(task.cancel(true));
        assertTrue(task.isCancelled());
        awaitTermination(t);
        assertEquals(1, task.runCount());
        assertEquals(0, task.setCount());
        assertEquals(1, task.setExceptionCount());
        tryToConfuseDoneTask(task);
        checkCancelled(task);
    }

    /**
     * cancel(false) does not interrupt a running task
     */
    public void testCancelNoInterrupt() {
        final CountDownLatch pleaseCancel = new CountDownLatch(1);
        final CountDownLatch cancelled = new CountDownLatch(1);
        final PublicFutureTask task =
            new PublicFutureTask(new CheckedCallable<Boolean>() {
                public Boolean realCall() {
                    pleaseCancel.countDown();
                    await(cancelled);
                    assertFalse(Thread.interrupted());
                    return Boolean.TRUE;
                }});

        Thread t = newStartedThread(task);
        await(pleaseCancel);
        assertTrue(task.cancel(false));
        assertTrue(task.isCancelled());
        cancelled.countDown();
        awaitTermination(t);
        assertEquals(1, task.runCount());
        assertEquals(1, task.setCount());
        assertEquals(0, task.setExceptionCount());
        tryToConfuseDoneTask(task);
        checkCancelled(task);
    }

    /**
     * run in one thread causes get in another thread to retrieve value
     */
    public void testGetRun() {
        final CountDownLatch pleaseRun = new CountDownLatch(2);

        final PublicFutureTask task =
            new PublicFutureTask(new CheckedCallable<Object>() {
                public Object realCall() {
                    return two;
                }});

        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws Exception {
                pleaseRun.countDown();
                assertSame(two, task.get());
            }});

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws Exception {
                pleaseRun.countDown();
                assertSame(two, task.get(2*LONG_DELAY_MS, MILLISECONDS));
            }});

        await(pleaseRun);
        checkNotDone(task);
        assertTrue(t1.isAlive());
        assertTrue(t2.isAlive());
        task.run();
        checkCompletedNormally(task, two);
        assertEquals(1, task.runCount());
        assertEquals(1, task.setCount());
        assertEquals(0, task.setExceptionCount());
        awaitTermination(t1);
        awaitTermination(t2);
        tryToConfuseDoneTask(task);
        checkCompletedNormally(task, two);
    }

    /**
     * set in one thread causes get in another thread to retrieve value
     */
    public void testGetSet() {
        final CountDownLatch pleaseSet = new CountDownLatch(2);

        final PublicFutureTask task =
            new PublicFutureTask(new CheckedCallable<Object>() {
                public Object realCall() throws InterruptedException {
                    return two;
                }});

        Thread t1 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws Exception {
                pleaseSet.countDown();
                assertSame(two, task.get());
            }});

        Thread t2 = newStartedThread(new CheckedRunnable() {
            public void realRun() throws Exception {
                pleaseSet.countDown();
                assertSame(two, task.get(2*LONG_DELAY_MS, MILLISECONDS));
            }});

        await(pleaseSet);
        checkNotDone(task);
        assertTrue(t1.isAlive());
        assertTrue(t2.isAlive());
        task.set(two);
        assertEquals(0, task.runCount());
        assertEquals(1, task.setCount());
        assertEquals(0, task.setExceptionCount());
        tryToConfuseDoneTask(task);
        checkCompletedNormally(task, two);
        awaitTermination(t1);
        awaitTermination(t2);
    }

    /**
     * Cancelling a task causes timed get in another thread to throw
     * CancellationException
     */
    public void testTimedGet_Cancellation() {
        testTimedGet_Cancellation(false);
    }
    public void testTimedGet_Cancellation_interrupt() {
        testTimedGet_Cancellation(true);
    }
    public void testTimedGet_Cancellation(final boolean mayInterruptIfRunning) {
        final CountDownLatch pleaseCancel = new CountDownLatch(3);
        final CountDownLatch cancelled = new CountDownLatch(1);
        final Callable<Object> callable = new CheckedCallable<>() {
            public Object realCall() throws InterruptedException {
                pleaseCancel.countDown();
                if (mayInterruptIfRunning) {
                    try {
                        delay(2*LONG_DELAY_MS);
                    } catch (InterruptedException success) {}
                } else {
                    await(cancelled);
                }
                return two;
            }};
        final PublicFutureTask task = new PublicFutureTask(callable);

        Thread t1 = new ThreadShouldThrow(CancellationException.class) {
                public void realRun() throws Exception {
                    pleaseCancel.countDown();
                    task.get();
                }};
        Thread t2 = new ThreadShouldThrow(CancellationException.class) {
                public void realRun() throws Exception {
                    pleaseCancel.countDown();
                    task.get(2*LONG_DELAY_MS, MILLISECONDS);
                }};
        t1.start();
        t2.start();
        Thread t3 = newStartedThread(task);
        await(pleaseCancel);
        checkIsRunning(task);
        task.cancel(mayInterruptIfRunning);
        checkCancelled(task);
        awaitTermination(t1);
        awaitTermination(t2);
        cancelled.countDown();
        awaitTermination(t3);
        assertEquals(1, task.runCount());
        assertEquals(1, task.setCount());
        assertEquals(0, task.setExceptionCount());
        tryToConfuseDoneTask(task);
        checkCancelled(task);
    }

    /**
     * A runtime exception in task causes get to throw ExecutionException
     */
    public void testGet_ExecutionException() throws InterruptedException {
        final ArithmeticException e = new ArithmeticException();
        final PublicFutureTask task = new PublicFutureTask(new Callable<Object>() {
            public Object call() {
                throw e;
            }});

        task.run();
        assertEquals(1, task.runCount());
        assertEquals(0, task.setCount());
        assertEquals(1, task.setExceptionCount());
        try {
            task.get();
            shouldThrow();
        } catch (ExecutionException success) {
            assertSame(e, success.getCause());
            tryToConfuseDoneTask(task);
            checkCompletedAbnormally(task, success.getCause());
        }
    }

    /**
     * A runtime exception in task causes timed get to throw ExecutionException
     */
    public void testTimedGet_ExecutionException2() throws Exception {
        final ArithmeticException e = new ArithmeticException();
        final PublicFutureTask task = new PublicFutureTask(new Callable<Object>() {
            public Object call() {
                throw e;
            }});

        task.run();
        try {
            task.get(LONG_DELAY_MS, MILLISECONDS);
            shouldThrow();
        } catch (ExecutionException success) {
            assertSame(e, success.getCause());
            tryToConfuseDoneTask(task);
            checkCompletedAbnormally(task, success.getCause());
        }
    }

    /**
     * get is interruptible
     */
    public void testGet_Interruptible() {
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        final FutureTask<Object> task = new FutureTask<>(new NoOpCallable());
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws Exception {
                Thread.currentThread().interrupt();
                try {
                    task.get();
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    task.get();
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(pleaseInterrupt);
        t.interrupt();
        awaitTermination(t);
        checkNotDone(task);
    }

    /**
     * timed get is interruptible
     */
    public void testTimedGet_Interruptible() {
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        final FutureTask<Object> task = new FutureTask<>(new NoOpCallable());
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws Exception {
                Thread.currentThread().interrupt();
                try {
                    task.get(randomTimeout(), randomTimeUnit());
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    task.get(LONGER_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(pleaseInterrupt);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.TIMED_WAITING);
        t.interrupt();
        awaitTermination(t);
        checkNotDone(task);
    }

    /**
     * A timed out timed get throws TimeoutException
     */
    public void testGet_TimeoutException() throws Exception {
        FutureTask<Object> task = new FutureTask<>(new NoOpCallable());
        long startTime = System.nanoTime();
        try {
            task.get(timeoutMillis(), MILLISECONDS);
            shouldThrow();
        } catch (TimeoutException success) {
            assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
        }
    }

    /**
     * timed get with null TimeUnit throws NullPointerException
     */
    public void testGet_NullTimeUnit() throws Exception {
        FutureTask<Object> task = new FutureTask<>(new NoOpCallable());
        long[] timeouts = { Long.MIN_VALUE, 0L, Long.MAX_VALUE };

        for (long timeout : timeouts) {
            try {
                task.get(timeout, null);
                shouldThrow();
            } catch (NullPointerException success) {}
        }

        task.run();

        for (long timeout : timeouts) {
            try {
                task.get(timeout, null);
                shouldThrow();
            } catch (NullPointerException success) {}
        }
    }

    /**
     * timed get with most negative timeout works correctly (i.e. no
     * underflow bug)
     */
    public void testGet_NegativeInfinityTimeout() throws Exception {
        final ExecutorService pool = Executors.newFixedThreadPool(10);
        final Runnable nop = new Runnable() { public void run() {}};
        final FutureTask<Void> task = new FutureTask<>(nop, null);
        final List<Future<?>> futures = new ArrayList<>();
        Runnable r = new Runnable() { public void run() {
            for (long timeout : new long[] { 0L, -1L, Long.MIN_VALUE }) {
                try {
                    task.get(timeout, NANOSECONDS);
                    shouldThrow();
                } catch (TimeoutException success) {
                } catch (Throwable fail) {threadUnexpectedException(fail);}}}};
        for (int i = 0; i < 10; i++)
            futures.add(pool.submit(r));
        try {
            joinPool(pool);
            for (Future<?> future : futures)
                checkCompletedNormally(future, null);
        } finally {
            task.run();         // last resort to help terminate
        }
    }

    /**
     * toString indicates current completion state
     */
    public void testToString_incomplete() {
        FutureTask<String> f = new FutureTask<>(() -> "");
        assertTrue(f.toString().matches(".*\\[.*Not completed.*\\]"));
        if (testImplementationDetails)
            assertTrue(f.toString().startsWith(
                               identityString(f) + "[Not completed, task ="));
    }

    public void testToString_normal() {
        FutureTask<String> f = new FutureTask<>(() -> "");
        f.run();
        assertTrue(f.toString().matches(".*\\[.*Completed normally.*\\]"));
        if (testImplementationDetails)
            assertEquals(identityString(f) + "[Completed normally]",
                         f.toString());
    }

    public void testToString_exception() {
        FutureTask<String> f = new FutureTask<>(
                () -> { throw new ArithmeticException(); });
        f.run();
        assertTrue(f.toString().matches(".*\\[.*Completed exceptionally.*\\]"));
        if (testImplementationDetails)
            assertTrue(f.toString().startsWith(
                               identityString(f) + "[Completed exceptionally: "));
    }

    public void testToString_cancelled() {
        for (boolean mayInterruptIfRunning : new boolean[] { true, false }) {
            FutureTask<String> f = new FutureTask<>(() -> "");
            assertTrue(f.cancel(mayInterruptIfRunning));
            assertTrue(f.toString().matches(".*\\[.*Cancelled.*\\]"));
            if (testImplementationDetails)
                assertEquals(identityString(f) + "[Cancelled]",
                             f.toString());
        }
    }

}
