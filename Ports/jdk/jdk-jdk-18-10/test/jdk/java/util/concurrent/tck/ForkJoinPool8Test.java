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
 */

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.HashSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountedCompleter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeoutException;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ForkJoinPool8Test extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        return new TestSuite(ForkJoinPool8Test.class);
    }

    /**
     * Common pool exists and has expected parallelism.
     */
    public void testCommonPoolParallelism() {
        assertEquals(ForkJoinPool.getCommonPoolParallelism(),
                     ForkJoinPool.commonPool().getParallelism());
    }

    /**
     * Common pool cannot be shut down
     */
    public void testCommonPoolShutDown() {
        assertFalse(ForkJoinPool.commonPool().isShutdown());
        assertFalse(ForkJoinPool.commonPool().isTerminating());
        assertFalse(ForkJoinPool.commonPool().isTerminated());
        ForkJoinPool.commonPool().shutdown();
        assertFalse(ForkJoinPool.commonPool().isShutdown());
        assertFalse(ForkJoinPool.commonPool().isTerminating());
        assertFalse(ForkJoinPool.commonPool().isTerminated());
        ForkJoinPool.commonPool().shutdownNow();
        assertFalse(ForkJoinPool.commonPool().isShutdown());
        assertFalse(ForkJoinPool.commonPool().isTerminating());
        assertFalse(ForkJoinPool.commonPool().isTerminated());
    }

    /*
     * All of the following test methods are adaptations of those for
     * RecursiveAction and CountedCompleter, but with all actions
     * executed in the common pool, generally implicitly via
     * checkInvoke.
     */

    private void checkInvoke(ForkJoinTask<?> a) {
        checkNotDone(a);
        assertNull(a.invoke());
        checkCompletedNormally(a);
    }

    void checkNotDone(ForkJoinTask<?> a) {
        assertFalse(a.isDone());
        assertFalse(a.isCompletedNormally());
        assertFalse(a.isCompletedAbnormally());
        assertFalse(a.isCancelled());
        assertNull(a.getException());
        assertNull(a.getRawResult());

        if (! ForkJoinTask.inForkJoinPool()) {
            Thread.currentThread().interrupt();
            try {
                a.get();
                shouldThrow();
            } catch (InterruptedException success) {
            } catch (Throwable fail) { threadUnexpectedException(fail); }

            Thread.currentThread().interrupt();
            try {
                a.get(randomTimeout(), randomTimeUnit());
                shouldThrow();
            } catch (InterruptedException success) {
            } catch (Throwable fail) { threadUnexpectedException(fail); }
        }

        try {
            a.get(randomExpiredTimeout(), randomTimeUnit());
            shouldThrow();
        } catch (TimeoutException success) {
        } catch (Throwable fail) { threadUnexpectedException(fail); }
    }

    void checkCompletedNormally(ForkJoinTask<?> a) {
        assertTrue(a.isDone());
        assertFalse(a.isCancelled());
        assertTrue(a.isCompletedNormally());
        assertFalse(a.isCompletedAbnormally());
        assertNull(a.getException());
        assertNull(a.getRawResult());
        assertNull(a.join());
        assertFalse(a.cancel(false));
        assertFalse(a.cancel(true));

        Object v1 = null, v2 = null;
        try {
            v1 = a.get();
            v2 = a.get(randomTimeout(), randomTimeUnit());
        } catch (Throwable fail) { threadUnexpectedException(fail); }
        assertNull(v1);
        assertNull(v2);
    }

    void checkCancelled(ForkJoinTask<?> a) {
        assertTrue(a.isDone());
        assertTrue(a.isCancelled());
        assertFalse(a.isCompletedNormally());
        assertTrue(a.isCompletedAbnormally());
        assertTrue(a.getException() instanceof CancellationException);
        assertNull(a.getRawResult());

        try {
            a.join();
            shouldThrow();
        } catch (CancellationException success) {
        } catch (Throwable fail) { threadUnexpectedException(fail); }

        try {
            a.get();
            shouldThrow();
        } catch (CancellationException success) {
        } catch (Throwable fail) { threadUnexpectedException(fail); }

        try {
            a.get(randomTimeout(), randomTimeUnit());
            shouldThrow();
        } catch (CancellationException success) {
        } catch (Throwable fail) { threadUnexpectedException(fail); }
    }

    void checkCompletedAbnormally(ForkJoinTask<?> a, Throwable t) {
        assertTrue(a.isDone());
        assertFalse(a.isCancelled());
        assertFalse(a.isCompletedNormally());
        assertTrue(a.isCompletedAbnormally());
        assertSame(t.getClass(), a.getException().getClass());
        assertNull(a.getRawResult());
        assertFalse(a.cancel(false));
        assertFalse(a.cancel(true));

        try {
            a.join();
            shouldThrow();
        } catch (Throwable expected) {
            assertSame(expected.getClass(), t.getClass());
        }

        try {
            a.get();
            shouldThrow();
        } catch (ExecutionException success) {
            assertSame(t.getClass(), success.getCause().getClass());
        } catch (Throwable fail) { threadUnexpectedException(fail); }

        try {
            a.get(randomTimeout(), randomTimeUnit());
            shouldThrow();
        } catch (ExecutionException success) {
            assertSame(t.getClass(), success.getCause().getClass());
        } catch (Throwable fail) { threadUnexpectedException(fail); }
    }

    public static final class FJException extends RuntimeException {
        public FJException() { super(); }
        public FJException(Throwable cause) { super(cause); }
    }

    /** A simple recursive action for testing. */
    final class FibAction extends CheckedRecursiveAction {
        final int number;
        int result;
        FibAction(int n) { number = n; }
        protected void realCompute() {
            int n = number;
            if (n <= 1)
                result = n;
            else {
                FibAction f1 = new FibAction(n - 1);
                FibAction f2 = new FibAction(n - 2);
                invokeAll(f1, f2);
                result = f1.result + f2.result;
            }
        }
    }

    /** A recursive action failing in base case. */
    static final class FailingFibAction extends RecursiveAction {
        final int number;
        int result;
        FailingFibAction(int n) { number = n; }
        public void compute() {
            int n = number;
            if (n <= 1)
                throw new FJException();
            else {
                FailingFibAction f1 = new FailingFibAction(n - 1);
                FailingFibAction f2 = new FailingFibAction(n - 2);
                invokeAll(f1, f2);
                result = f1.result + f2.result;
            }
        }
    }

    /**
     * invoke returns when task completes normally.
     * isCompletedAbnormally and isCancelled return false for normally
     * completed tasks. getRawResult of a RecursiveAction returns null;
     */
    public void testInvoke() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FibAction f = new FibAction(8);
                assertNull(f.invoke());
                assertEquals(21, f.result);
                checkCompletedNormally(f);
            }};
        checkInvoke(a);
    }

    /**
     * quietlyInvoke task returns when task completes normally.
     * isCompletedAbnormally and isCancelled return false for normally
     * completed tasks
     */
    public void testQuietlyInvoke() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FibAction f = new FibAction(8);
                f.quietlyInvoke();
                assertEquals(21, f.result);
                checkCompletedNormally(f);
            }};
        checkInvoke(a);
    }

    /**
     * join of a forked task returns when task completes
     */
    public void testForkJoin() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FibAction f = new FibAction(8);
                assertSame(f, f.fork());
                assertNull(f.join());
                assertEquals(21, f.result);
                checkCompletedNormally(f);
            }};
        checkInvoke(a);
    }

    /**
     * join/quietlyJoin of a forked task succeeds in the presence of interrupts
     */
    public void testJoinIgnoresInterrupts() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FibAction f = new FibAction(8);
                final Thread currentThread = Thread.currentThread();

                // test join()
                assertSame(f, f.fork());
                currentThread.interrupt();
                assertNull(f.join());
                Thread.interrupted();
                assertEquals(21, f.result);
                checkCompletedNormally(f);

                f = new FibAction(8);
                f.cancel(true);
                assertSame(f, f.fork());
                currentThread.interrupt();
                try {
                    f.join();
                    shouldThrow();
                } catch (CancellationException success) {
                    Thread.interrupted();
                    checkCancelled(f);
                }

                f = new FibAction(8);
                f.completeExceptionally(new FJException());
                assertSame(f, f.fork());
                currentThread.interrupt();
                try {
                    f.join();
                    shouldThrow();
                } catch (FJException success) {
                    Thread.interrupted();
                    checkCompletedAbnormally(f, success);
                }

                // test quietlyJoin()
                f = new FibAction(8);
                assertSame(f, f.fork());
                currentThread.interrupt();
                f.quietlyJoin();
                Thread.interrupted();
                assertEquals(21, f.result);
                checkCompletedNormally(f);

                f = new FibAction(8);
                f.cancel(true);
                assertSame(f, f.fork());
                currentThread.interrupt();
                f.quietlyJoin();
                Thread.interrupted();
                checkCancelled(f);

                f = new FibAction(8);
                f.completeExceptionally(new FJException());
                assertSame(f, f.fork());
                currentThread.interrupt();
                f.quietlyJoin();
                Thread.interrupted();
                checkCompletedAbnormally(f, f.getException());
            }};
        checkInvoke(a);
        a.reinitialize();
        checkInvoke(a);
    }

    /**
     * get of a forked task returns when task completes
     */
    public void testForkGet() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() throws Exception {
                FibAction f = new FibAction(8);
                assertSame(f, f.fork());
                assertNull(f.get());
                assertEquals(21, f.result);
                checkCompletedNormally(f);
            }};
        checkInvoke(a);
    }

    /**
     * timed get of a forked task returns when task completes
     */
    public void testForkTimedGet() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() throws Exception {
                FibAction f = new FibAction(8);
                assertSame(f, f.fork());
                assertNull(f.get(LONG_DELAY_MS, MILLISECONDS));
                assertEquals(21, f.result);
                checkCompletedNormally(f);
            }};
        checkInvoke(a);
    }

    /**
     * timed get with null time unit throws NPE
     */
    public void testForkTimedGetNPE() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() throws Exception {
                FibAction f = new FibAction(8);
                assertSame(f, f.fork());
                try {
                    f.get(randomTimeout(), null);
                    shouldThrow();
                } catch (NullPointerException success) {}
            }};
        checkInvoke(a);
    }

    /**
     * quietlyJoin of a forked task returns when task completes
     */
    public void testForkQuietlyJoin() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FibAction f = new FibAction(8);
                assertSame(f, f.fork());
                f.quietlyJoin();
                assertEquals(21, f.result);
                checkCompletedNormally(f);
            }};
        checkInvoke(a);
    }

    /**
     * invoke task throws exception when task completes abnormally
     */
    public void testAbnormalInvoke() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FailingFibAction f = new FailingFibAction(8);
                try {
                    f.invoke();
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(f, success);
                }
            }};
        checkInvoke(a);
    }

    /**
     * quietlyInvoke task returns when task completes abnormally
     */
    public void testAbnormalQuietlyInvoke() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FailingFibAction f = new FailingFibAction(8);
                f.quietlyInvoke();
                assertTrue(f.getException() instanceof FJException);
                checkCompletedAbnormally(f, f.getException());
            }};
        checkInvoke(a);
    }

    /**
     * join of a forked task throws exception when task completes abnormally
     */
    public void testAbnormalForkJoin() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FailingFibAction f = new FailingFibAction(8);
                assertSame(f, f.fork());
                try {
                    f.join();
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(f, success);
                }
            }};
        checkInvoke(a);
    }

    /**
     * get of a forked task throws exception when task completes abnormally
     */
    public void testAbnormalForkGet() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() throws Exception {
                FailingFibAction f = new FailingFibAction(8);
                assertSame(f, f.fork());
                try {
                    f.get();
                    shouldThrow();
                } catch (ExecutionException success) {
                    Throwable cause = success.getCause();
                    assertTrue(cause instanceof FJException);
                    checkCompletedAbnormally(f, cause);
                }
            }};
        checkInvoke(a);
    }

    /**
     * timed get of a forked task throws exception when task completes abnormally
     */
    public void testAbnormalForkTimedGet() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() throws Exception {
                FailingFibAction f = new FailingFibAction(8);
                assertSame(f, f.fork());
                try {
                    f.get(LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (ExecutionException success) {
                    Throwable cause = success.getCause();
                    assertTrue(cause instanceof FJException);
                    checkCompletedAbnormally(f, cause);
                }
            }};
        checkInvoke(a);
    }

    /**
     * quietlyJoin of a forked task returns when task completes abnormally
     */
    public void testAbnormalForkQuietlyJoin() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FailingFibAction f = new FailingFibAction(8);
                assertSame(f, f.fork());
                f.quietlyJoin();
                assertTrue(f.getException() instanceof FJException);
                checkCompletedAbnormally(f, f.getException());
            }};
        checkInvoke(a);
    }

    /**
     * invoke task throws exception when task cancelled
     */
    public void testCancelledInvoke() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FibAction f = new FibAction(8);
                assertTrue(f.cancel(true));
                try {
                    f.invoke();
                    shouldThrow();
                } catch (CancellationException success) {
                    checkCancelled(f);
                }
            }};
        checkInvoke(a);
    }

    /**
     * join of a forked task throws exception when task cancelled
     */
    public void testCancelledForkJoin() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FibAction f = new FibAction(8);
                assertTrue(f.cancel(true));
                assertSame(f, f.fork());
                try {
                    f.join();
                    shouldThrow();
                } catch (CancellationException success) {
                    checkCancelled(f);
                }
            }};
        checkInvoke(a);
    }

    /**
     * get of a forked task throws exception when task cancelled
     */
    public void testCancelledForkGet() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() throws Exception {
                FibAction f = new FibAction(8);
                assertTrue(f.cancel(true));
                assertSame(f, f.fork());
                try {
                    f.get();
                    shouldThrow();
                } catch (CancellationException success) {
                    checkCancelled(f);
                }
            }};
        checkInvoke(a);
    }

    /**
     * timed get of a forked task throws exception when task cancelled
     */
    public void testCancelledForkTimedGet() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() throws Exception {
                FibAction f = new FibAction(8);
                assertTrue(f.cancel(true));
                assertSame(f, f.fork());
                try {
                    f.get(LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (CancellationException success) {
                    checkCancelled(f);
                }
            }};
        checkInvoke(a);
    }

    /**
     * quietlyJoin of a forked task returns when task cancelled
     */
    public void testCancelledForkQuietlyJoin() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FibAction f = new FibAction(8);
                assertTrue(f.cancel(true));
                assertSame(f, f.fork());
                f.quietlyJoin();
                checkCancelled(f);
            }};
        checkInvoke(a);
    }

    /**
     * inForkJoinPool of non-FJ task returns false
     */
    public void testInForkJoinPool2() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                assertFalse(inForkJoinPool());
            }};
        assertNull(a.invoke());
    }

    /**
     * A reinitialized normally completed task may be re-invoked
     */
    public void testReinitialize() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FibAction f = new FibAction(8);
                checkNotDone(f);

                for (int i = 0; i < 3; i++) {
                    assertNull(f.invoke());
                    assertEquals(21, f.result);
                    checkCompletedNormally(f);
                    f.reinitialize();
                    checkNotDone(f);
                }
            }};
        checkInvoke(a);
    }

    /**
     * A reinitialized abnormally completed task may be re-invoked
     */
    public void testReinitializeAbnormal() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FailingFibAction f = new FailingFibAction(8);
                checkNotDone(f);

                for (int i = 0; i < 3; i++) {
                    try {
                        f.invoke();
                        shouldThrow();
                    } catch (FJException success) {
                        checkCompletedAbnormally(f, success);
                    }
                    f.reinitialize();
                    checkNotDone(f);
                }
            }};
        checkInvoke(a);
    }

    /**
     * invoke task throws exception after invoking completeExceptionally
     */
    public void testCompleteExceptionally() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FibAction f = new FibAction(8);
                f.completeExceptionally(new FJException());
                try {
                    f.invoke();
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(f, success);
                }
            }};
        checkInvoke(a);
    }

    /**
     * invoke task suppresses execution invoking complete
     */
    public void testComplete() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FibAction f = new FibAction(8);
                f.complete(null);
                assertNull(f.invoke());
                assertEquals(0, f.result);
                checkCompletedNormally(f);
            }};
        checkInvoke(a);
    }

    /**
     * invokeAll(t1, t2) invokes all task arguments
     */
    public void testInvokeAll2() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FibAction f = new FibAction(8);
                FibAction g = new FibAction(9);
                invokeAll(f, g);
                checkCompletedNormally(f);
                assertEquals(21, f.result);
                checkCompletedNormally(g);
                assertEquals(34, g.result);
            }};
        checkInvoke(a);
    }

    /**
     * invokeAll(tasks) with 1 argument invokes task
     */
    public void testInvokeAll1() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FibAction f = new FibAction(8);
                invokeAll(f);
                checkCompletedNormally(f);
                assertEquals(21, f.result);
            }};
        checkInvoke(a);
    }

    /**
     * invokeAll(tasks) with > 2 argument invokes tasks
     */
    public void testInvokeAll3() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FibAction f = new FibAction(8);
                FibAction g = new FibAction(9);
                FibAction h = new FibAction(7);
                invokeAll(f, g, h);
                assertTrue(f.isDone());
                assertTrue(g.isDone());
                assertTrue(h.isDone());
                checkCompletedNormally(f);
                assertEquals(21, f.result);
                checkCompletedNormally(g);
                assertEquals(34, g.result);
                checkCompletedNormally(g);
                assertEquals(13, h.result);
            }};
        checkInvoke(a);
    }

    /**
     * invokeAll(collection) invokes all tasks in the collection
     */
    public void testInvokeAllCollection() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FibAction f = new FibAction(8);
                FibAction g = new FibAction(9);
                FibAction h = new FibAction(7);
                HashSet<ForkJoinTask<?>> set = new HashSet<>();
                set.add(f);
                set.add(g);
                set.add(h);
                invokeAll(set);
                assertTrue(f.isDone());
                assertTrue(g.isDone());
                assertTrue(h.isDone());
                checkCompletedNormally(f);
                assertEquals(21, f.result);
                checkCompletedNormally(g);
                assertEquals(34, g.result);
                checkCompletedNormally(g);
                assertEquals(13, h.result);
            }};
        checkInvoke(a);
    }

    /**
     * invokeAll(tasks) with any null task throws NPE
     */
    public void testInvokeAllNPE() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FibAction f = new FibAction(8);
                FibAction g = new FibAction(9);
                FibAction h = null;
                try {
                    invokeAll(f, g, h);
                    shouldThrow();
                } catch (NullPointerException success) {}
            }};
        checkInvoke(a);
    }

    /**
     * invokeAll(t1, t2) throw exception if any task does
     */
    public void testAbnormalInvokeAll2() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FibAction f = new FibAction(8);
                FailingFibAction g = new FailingFibAction(9);
                try {
                    invokeAll(f, g);
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(g, success);
                }
            }};
        checkInvoke(a);
    }

    /**
     * invokeAll(tasks) with 1 argument throws exception if task does
     */
    public void testAbnormalInvokeAll1() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FailingFibAction g = new FailingFibAction(9);
                try {
                    invokeAll(g);
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(g, success);
                }
            }};
        checkInvoke(a);
    }

    /**
     * invokeAll(tasks) with > 2 argument throws exception if any task does
     */
    public void testAbnormalInvokeAll3() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FibAction f = new FibAction(8);
                FailingFibAction g = new FailingFibAction(9);
                FibAction h = new FibAction(7);
                try {
                    invokeAll(f, g, h);
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(g, success);
                }
            }};
        checkInvoke(a);
    }

    /**
     * invokeAll(collection) throws exception if any task does
     */
    public void testAbnormalInvokeAllCollection() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FailingFibAction f = new FailingFibAction(8);
                FibAction g = new FibAction(9);
                FibAction h = new FibAction(7);
                HashSet<ForkJoinTask<?>> set = new HashSet<>();
                set.add(f);
                set.add(g);
                set.add(h);
                try {
                    invokeAll(set);
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(f, success);
                }
            }};
        checkInvoke(a);
    }

    // CountedCompleter versions

    abstract static class CCF extends CountedCompleter<Void> {
        int number;
        int rnumber;

        public CCF(CountedCompleter<?> parent, int n) {
            super(parent, 1);
            this.number = n;
        }

        public final void compute() {
            CountedCompleter<?> p;
            CCF f = this;
            int n = number;
            while (n >= 2) {
                new RCCF(f, n - 2).fork();
                f = new LCCF(f, --n);
            }
            f.number = n;
            f.onCompletion(f);
            if ((p = f.getCompleter()) != null)
                p.tryComplete();
            else
                f.quietlyComplete();
        }
    }

    static final class LCCF extends CCF {
        public LCCF(CountedCompleter<?> parent, int n) {
            super(parent, n);
        }
        public final void onCompletion(CountedCompleter<?> caller) {
            CCF p = (CCF)getCompleter();
            int n = number + rnumber;
            if (p != null)
                p.number = n;
            else
                number = n;
        }
    }
    static final class RCCF extends CCF {
        public RCCF(CountedCompleter parent, int n) {
            super(parent, n);
        }
        public final void onCompletion(CountedCompleter<?> caller) {
            CCF p = (CCF)getCompleter();
            int n = number + rnumber;
            if (p != null)
                p.rnumber = n;
            else
                number = n;
        }
    }

    /** Version of CCF with forced failure in left completions. */
    abstract static class FailingCCF extends CountedCompleter<Void> {
        int number;
        int rnumber;

        public FailingCCF(CountedCompleter<?> parent, int n) {
            super(parent, 1);
            this.number = n;
        }

        public final void compute() {
            CountedCompleter<?> p;
            FailingCCF f = this;
            int n = number;
            while (n >= 2) {
                new RFCCF(f, n - 2).fork();
                f = new LFCCF(f, --n);
            }
            f.number = n;
            f.onCompletion(f);
            if ((p = f.getCompleter()) != null)
                p.tryComplete();
            else
                f.quietlyComplete();
        }
    }

    static final class LFCCF extends FailingCCF {
        public LFCCF(CountedCompleter<?> parent, int n) {
            super(parent, n);
        }
        public final void onCompletion(CountedCompleter<?> caller) {
            FailingCCF p = (FailingCCF)getCompleter();
            int n = number + rnumber;
            if (p != null)
                p.number = n;
            else
                number = n;
        }
    }
    static final class RFCCF extends FailingCCF {
        public RFCCF(CountedCompleter<?> parent, int n) {
            super(parent, n);
        }
        public final void onCompletion(CountedCompleter<?> caller) {
            completeExceptionally(new FJException());
        }
    }

    /**
     * invoke returns when task completes normally.
     * isCompletedAbnormally and isCancelled return false for normally
     * completed tasks; getRawResult returns null.
     */
    public void testInvokeCC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                CCF f = new LCCF(null, 8);
                assertNull(f.invoke());
                assertEquals(21, f.number);
                checkCompletedNormally(f);
            }};
        checkInvoke(a);
    }

    /**
     * quietlyInvoke task returns when task completes normally.
     * isCompletedAbnormally and isCancelled return false for normally
     * completed tasks
     */
    public void testQuietlyInvokeCC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                CCF f = new LCCF(null, 8);
                f.quietlyInvoke();
                assertEquals(21, f.number);
                checkCompletedNormally(f);
            }};
        checkInvoke(a);
    }

    /**
     * join of a forked task returns when task completes
     */
    public void testForkJoinCC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                CCF f = new LCCF(null, 8);
                assertSame(f, f.fork());
                assertNull(f.join());
                assertEquals(21, f.number);
                checkCompletedNormally(f);
            }};
        checkInvoke(a);
    }

    /**
     * get of a forked task returns when task completes
     */
    public void testForkGetCC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() throws Exception {
                CCF f = new LCCF(null, 8);
                assertSame(f, f.fork());
                assertNull(f.get());
                assertEquals(21, f.number);
                checkCompletedNormally(f);
            }};
        checkInvoke(a);
    }

    /**
     * timed get of a forked task returns when task completes
     */
    public void testForkTimedGetCC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() throws Exception {
                CCF f = new LCCF(null, 8);
                assertSame(f, f.fork());
                assertNull(f.get(LONG_DELAY_MS, MILLISECONDS));
                assertEquals(21, f.number);
                checkCompletedNormally(f);
            }};
        checkInvoke(a);
    }

    /**
     * timed get with null time unit throws NPE
     */
    public void testForkTimedGetNPECC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() throws Exception {
                CCF f = new LCCF(null, 8);
                assertSame(f, f.fork());
                try {
                    f.get(randomTimeout(), null);
                    shouldThrow();
                } catch (NullPointerException success) {}
            }};
        checkInvoke(a);
    }

    /**
     * quietlyJoin of a forked task returns when task completes
     */
    public void testForkQuietlyJoinCC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                CCF f = new LCCF(null, 8);
                assertSame(f, f.fork());
                f.quietlyJoin();
                assertEquals(21, f.number);
                checkCompletedNormally(f);
            }};
        checkInvoke(a);
    }

    /**
     * invoke task throws exception when task completes abnormally
     */
    public void testAbnormalInvokeCC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FailingCCF f = new LFCCF(null, 8);
                try {
                    f.invoke();
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(f, success);
                }
            }};
        checkInvoke(a);
    }

    /**
     * quietlyInvoke task returns when task completes abnormally
     */
    public void testAbnormalQuietlyInvokeCC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FailingCCF f = new LFCCF(null, 8);
                f.quietlyInvoke();
                assertTrue(f.getException() instanceof FJException);
                checkCompletedAbnormally(f, f.getException());
            }};
        checkInvoke(a);
    }

    /**
     * join of a forked task throws exception when task completes abnormally
     */
    public void testAbnormalForkJoinCC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FailingCCF f = new LFCCF(null, 8);
                assertSame(f, f.fork());
                try {
                    f.join();
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(f, success);
                }
            }};
        checkInvoke(a);
    }

    /**
     * get of a forked task throws exception when task completes abnormally
     */
    public void testAbnormalForkGetCC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() throws Exception {
                FailingCCF f = new LFCCF(null, 8);
                assertSame(f, f.fork());
                try {
                    f.get();
                    shouldThrow();
                } catch (ExecutionException success) {
                    Throwable cause = success.getCause();
                    assertTrue(cause instanceof FJException);
                    checkCompletedAbnormally(f, cause);
                }
            }};
        checkInvoke(a);
    }

    /**
     * timed get of a forked task throws exception when task completes abnormally
     */
    public void testAbnormalForkTimedGetCC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() throws Exception {
                FailingCCF f = new LFCCF(null, 8);
                assertSame(f, f.fork());
                try {
                    f.get(LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (ExecutionException success) {
                    Throwable cause = success.getCause();
                    assertTrue(cause instanceof FJException);
                    checkCompletedAbnormally(f, cause);
                }
            }};
        checkInvoke(a);
    }

    /**
     * quietlyJoin of a forked task returns when task completes abnormally
     */
    public void testAbnormalForkQuietlyJoinCC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FailingCCF f = new LFCCF(null, 8);
                assertSame(f, f.fork());
                f.quietlyJoin();
                assertTrue(f.getException() instanceof FJException);
                checkCompletedAbnormally(f, f.getException());
            }};
        checkInvoke(a);
    }

    /**
     * invoke task throws exception when task cancelled
     */
    public void testCancelledInvokeCC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                CCF f = new LCCF(null, 8);
                assertTrue(f.cancel(true));
                try {
                    f.invoke();
                    shouldThrow();
                } catch (CancellationException success) {
                    checkCancelled(f);
                }
            }};
        checkInvoke(a);
    }

    /**
     * join of a forked task throws exception when task cancelled
     */
    public void testCancelledForkJoinCC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                CCF f = new LCCF(null, 8);
                assertTrue(f.cancel(true));
                assertSame(f, f.fork());
                try {
                    f.join();
                    shouldThrow();
                } catch (CancellationException success) {
                    checkCancelled(f);
                }
            }};
        checkInvoke(a);
    }

    /**
     * get of a forked task throws exception when task cancelled
     */
    public void testCancelledForkGetCC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() throws Exception {
                CCF f = new LCCF(null, 8);
                assertTrue(f.cancel(true));
                assertSame(f, f.fork());
                try {
                    f.get();
                    shouldThrow();
                } catch (CancellationException success) {
                    checkCancelled(f);
                }
            }};
        checkInvoke(a);
    }

    /**
     * timed get of a forked task throws exception when task cancelled
     */
    public void testCancelledForkTimedGetCC() throws Exception {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() throws Exception {
                CCF f = new LCCF(null, 8);
                assertTrue(f.cancel(true));
                assertSame(f, f.fork());
                try {
                    f.get(LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (CancellationException success) {
                    checkCancelled(f);
                }
            }};
        checkInvoke(a);
    }

    /**
     * quietlyJoin of a forked task returns when task cancelled
     */
    public void testCancelledForkQuietlyJoinCC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                CCF f = new LCCF(null, 8);
                assertTrue(f.cancel(true));
                assertSame(f, f.fork());
                f.quietlyJoin();
                checkCancelled(f);
            }};
        checkInvoke(a);
    }

    /**
     * getPool of non-FJ task returns null
     */
    public void testGetPool2CC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                assertNull(getPool());
            }};
        assertNull(a.invoke());
    }

    /**
     * inForkJoinPool of non-FJ task returns false
     */
    public void testInForkJoinPool2CC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                assertFalse(inForkJoinPool());
            }};
        assertNull(a.invoke());
    }

    /**
     * setRawResult(null) succeeds
     */
    public void testSetRawResultCC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                setRawResult(null);
                assertNull(getRawResult());
            }};
        assertNull(a.invoke());
    }

    /**
     * invoke task throws exception after invoking completeExceptionally
     */
    public void testCompleteExceptionally2CC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                CCF f = new LCCF(null, 8);
                f.completeExceptionally(new FJException());
                try {
                    f.invoke();
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(f, success);
                }
            }};
        checkInvoke(a);
    }

    /**
     * invokeAll(t1, t2) invokes all task arguments
     */
    public void testInvokeAll2CC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                CCF f = new LCCF(null, 8);
                CCF g = new LCCF(null, 9);
                invokeAll(f, g);
                assertEquals(21, f.number);
                assertEquals(34, g.number);
                checkCompletedNormally(f);
                checkCompletedNormally(g);
            }};
        checkInvoke(a);
    }

    /**
     * invokeAll(tasks) with 1 argument invokes task
     */
    public void testInvokeAll1CC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                CCF f = new LCCF(null, 8);
                invokeAll(f);
                checkCompletedNormally(f);
                assertEquals(21, f.number);
            }};
        checkInvoke(a);
    }

    /**
     * invokeAll(tasks) with > 2 argument invokes tasks
     */
    public void testInvokeAll3CC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                CCF f = new LCCF(null, 8);
                CCF g = new LCCF(null, 9);
                CCF h = new LCCF(null, 7);
                invokeAll(f, g, h);
                assertEquals(21, f.number);
                assertEquals(34, g.number);
                assertEquals(13, h.number);
                checkCompletedNormally(f);
                checkCompletedNormally(g);
                checkCompletedNormally(h);
            }};
        checkInvoke(a);
    }

    /**
     * invokeAll(collection) invokes all tasks in the collection
     */
    public void testInvokeAllCollectionCC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                CCF f = new LCCF(null, 8);
                CCF g = new LCCF(null, 9);
                CCF h = new LCCF(null, 7);
                HashSet<ForkJoinTask<?>> set = new HashSet<>();
                set.add(f);
                set.add(g);
                set.add(h);
                invokeAll(set);
                assertEquals(21, f.number);
                assertEquals(34, g.number);
                assertEquals(13, h.number);
                checkCompletedNormally(f);
                checkCompletedNormally(g);
                checkCompletedNormally(h);
            }};
        checkInvoke(a);
    }

    /**
     * invokeAll(tasks) with any null task throws NPE
     */
    public void testInvokeAllNPECC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                CCF f = new LCCF(null, 8);
                CCF g = new LCCF(null, 9);
                CCF h = null;
                try {
                    invokeAll(f, g, h);
                    shouldThrow();
                } catch (NullPointerException success) {}
            }};
        checkInvoke(a);
    }

    /**
     * invokeAll(t1, t2) throw exception if any task does
     */
    public void testAbnormalInvokeAll2CC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                CCF f = new LCCF(null, 8);
                FailingCCF g = new LFCCF(null, 9);
                try {
                    invokeAll(f, g);
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(g, success);
                }
            }};
        checkInvoke(a);
    }

    /**
     * invokeAll(tasks) with 1 argument throws exception if task does
     */
    public void testAbnormalInvokeAll1CC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FailingCCF g = new LFCCF(null, 9);
                try {
                    invokeAll(g);
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(g, success);
                }
            }};
        checkInvoke(a);
    }

    /**
     * invokeAll(tasks) with > 2 argument throws exception if any task does
     */
    public void testAbnormalInvokeAll3CC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                CCF f = new LCCF(null, 8);
                FailingCCF g = new LFCCF(null, 9);
                CCF h = new LCCF(null, 7);
                try {
                    invokeAll(f, g, h);
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(g, success);
                }
            }};
        checkInvoke(a);
    }

    /**
     * invokeAll(collection) throws exception if any task does
     */
    public void testAbnormalInvokeAllCollectionCC() {
        CheckedRecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FailingCCF f = new LFCCF(null, 8);
                CCF g = new LCCF(null, 9);
                CCF h = new LCCF(null, 7);
                HashSet<ForkJoinTask<?>> set = new HashSet<>();
                set.add(f);
                set.add(g);
                set.add(h);
                try {
                    invokeAll(set);
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(f, success);
                }
            }};
        checkInvoke(a);
    }

    /**
     * awaitQuiescence by a worker is equivalent in effect to
     * ForkJoinTask.helpQuiesce()
     */
    public void testAwaitQuiescence1() throws Exception {
        final ForkJoinPool p = new ForkJoinPool();
        try (PoolCleaner cleaner = cleaner(p)) {
            final long startTime = System.nanoTime();
            assertTrue(p.isQuiescent());
            CheckedRecursiveAction a = new CheckedRecursiveAction() {
                protected void realCompute() {
                    FibAction f = new FibAction(8);
                    assertSame(f, f.fork());
                    assertSame(p, ForkJoinTask.getPool());
                    boolean quiescent = p.awaitQuiescence(LONG_DELAY_MS, MILLISECONDS);
                    assertTrue(quiescent);
                    assertFalse(p.isQuiescent());
                    while (!f.isDone()) {
                        assertFalse(p.getAsyncMode());
                        assertFalse(p.isShutdown());
                        assertFalse(p.isTerminating());
                        assertFalse(p.isTerminated());
                        Thread.yield();
                    }
                    assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
                    assertFalse(p.isQuiescent());
                    assertEquals(0, ForkJoinTask.getQueuedTaskCount());
                    assertEquals(21, f.result);
                }};
            p.execute(a);
            while (!a.isDone() || !p.isQuiescent()) {
                assertFalse(p.getAsyncMode());
                assertFalse(p.isShutdown());
                assertFalse(p.isTerminating());
                assertFalse(p.isTerminated());
                Thread.yield();
            }
            assertEquals(0, p.getQueuedTaskCount());
            assertFalse(p.getAsyncMode());
            assertEquals(0, p.getQueuedSubmissionCount());
            assertFalse(p.hasQueuedSubmissions());
            while (p.getActiveThreadCount() != 0
                   && millisElapsedSince(startTime) < LONG_DELAY_MS)
                Thread.yield();
            assertFalse(p.isShutdown());
            assertFalse(p.isTerminating());
            assertFalse(p.isTerminated());
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
        }
    }

    /**
     * awaitQuiescence returns when pool isQuiescent() or the indicated
     * timeout elapsed
     */
    public void testAwaitQuiescence2() throws Exception {
        /*
         * """It is possible to disable or limit the use of threads in the
         * common pool by setting the parallelism property to zero. However
         * doing so may cause unjoined tasks to never be executed."""
         */
        if ("0".equals(System.getProperty(
             "java.util.concurrent.ForkJoinPool.common.parallelism")))
            return;
        final ForkJoinPool p = new ForkJoinPool();
        try (PoolCleaner cleaner = cleaner(p)) {
            assertTrue(p.isQuiescent());
            final long startTime = System.nanoTime();
            CheckedRecursiveAction a = new CheckedRecursiveAction() {
                protected void realCompute() {
                    FibAction f = new FibAction(8);
                    assertSame(f, f.fork());
                    while (!f.isDone()
                           && millisElapsedSince(startTime) < LONG_DELAY_MS) {
                        assertFalse(p.getAsyncMode());
                        assertFalse(p.isShutdown());
                        assertFalse(p.isTerminating());
                        assertFalse(p.isTerminated());
                        Thread.yield();
                    }
                    assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
                    assertEquals(0, ForkJoinTask.getQueuedTaskCount());
                    assertEquals(21, f.result);
                }};
            p.execute(a);
            assertTrue(p.awaitQuiescence(LONG_DELAY_MS, MILLISECONDS));
            assertTrue(p.isQuiescent());
            assertTrue(a.isDone());
            assertEquals(0, p.getQueuedTaskCount());
            assertFalse(p.getAsyncMode());
            assertEquals(0, p.getQueuedSubmissionCount());
            assertFalse(p.hasQueuedSubmissions());
            while (p.getActiveThreadCount() != 0
                   && millisElapsedSince(startTime) < LONG_DELAY_MS)
                Thread.yield();
            assertFalse(p.isShutdown());
            assertFalse(p.isTerminating());
            assertFalse(p.isTerminated());
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
        }
    }

}
