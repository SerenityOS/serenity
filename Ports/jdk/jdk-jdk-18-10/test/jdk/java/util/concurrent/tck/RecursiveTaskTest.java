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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeoutException;

import junit.framework.Test;
import junit.framework.TestSuite;

public class RecursiveTaskTest extends JSR166TestCase {

    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(RecursiveTaskTest.class);
    }

    private static ForkJoinPool mainPool() {
        return new ForkJoinPool();
    }

    private static ForkJoinPool singletonPool() {
        return new ForkJoinPool(1);
    }

    private static ForkJoinPool asyncSingletonPool() {
        return new ForkJoinPool(1,
                                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                                null, true);
    }

    private <T> T testInvokeOnPool(ForkJoinPool pool, RecursiveTask<T> a) {
        try (PoolCleaner cleaner = cleaner(pool)) {
            checkNotDone(a);

            T result = pool.invoke(a);

            checkCompletedNormally(a, result);
            return result;
        }
    }

    void checkNotDone(RecursiveTask<?> a) {
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

    <T> void checkCompletedNormally(RecursiveTask<T> a, T expectedValue) {
        assertTrue(a.isDone());
        assertFalse(a.isCancelled());
        assertTrue(a.isCompletedNormally());
        assertFalse(a.isCompletedAbnormally());
        assertNull(a.getException());
        assertSame(expectedValue, a.getRawResult());
        assertSame(expectedValue, a.join());
        assertFalse(a.cancel(false));
        assertFalse(a.cancel(true));

        T v1 = null, v2 = null;
        try {
            v1 = a.get();
            v2 = a.get(randomTimeout(), randomTimeUnit());
        } catch (Throwable fail) { threadUnexpectedException(fail); }
        assertSame(expectedValue, v1);
        assertSame(expectedValue, v2);
    }

    /**
     * Waits for the task to complete, and checks that when it does,
     * it will have an Integer result equals to the given int.
     */
    void checkCompletesNormally(RecursiveTask<Integer> a, int expectedValue) {
        Integer r = a.join();
        assertEquals(expectedValue, (int) r);
        checkCompletedNormally(a, r);
    }

    /**
     * Like checkCompletesNormally, but verifies that the task has
     * already completed.
     */
    void checkCompletedNormally(RecursiveTask<Integer> a, int expectedValue) {
        Integer r = a.getRawResult();
        assertEquals(expectedValue, (int) r);
        checkCompletedNormally(a, r);
    }

    void checkCancelled(RecursiveTask<?> a) {
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

    void checkCompletedAbnormally(RecursiveTask<?> a, Throwable t) {
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
            assertSame(t.getClass(), expected.getClass());
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
    }

    /** An invalid return value for Fib. */
    static final Integer NoResult = Integer.valueOf(-17);

    /** A simple recursive task for testing. */
    final class FibTask extends CheckedRecursiveTask<Integer> {
        final int number;
        FibTask(int n) { number = n; }
        public Integer realCompute() {
            int n = number;
            if (n <= 1)
                return n;
            FibTask f1 = new FibTask(n - 1);
            f1.fork();
            return new FibTask(n - 2).compute() + f1.join();
        }

        public void publicSetRawResult(Integer result) {
            setRawResult(result);
        }
    }

    /** A recursive action failing in base case. */
    final class FailingFibTask extends RecursiveTask<Integer> {
        final int number;
        int result;
        FailingFibTask(int n) { number = n; }
        public Integer compute() {
            int n = number;
            if (n <= 1)
                throw new FJException();
            FailingFibTask f1 = new FailingFibTask(n - 1);
            f1.fork();
            return new FibTask(n - 2).compute() + f1.join();
        }
    }

    /**
     * invoke returns value when task completes normally.
     * isCompletedAbnormally and isCancelled return false for normally
     * completed tasks. getRawResult of a completed non-null task
     * returns value;
     */
    public void testInvoke() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask f = new FibTask(8);
                Integer r = f.invoke();
                assertEquals(21, (int) r);
                checkCompletedNormally(f, r);
                return r;
            }};
        assertEquals(21, (int) testInvokeOnPool(mainPool(), a));
    }

    /**
     * quietlyInvoke task returns when task completes normally.
     * isCompletedAbnormally and isCancelled return false for normally
     * completed tasks
     */
    public void testQuietlyInvoke() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask f = new FibTask(8);
                f.quietlyInvoke();
                checkCompletedNormally(f, 21);
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * join of a forked task returns when task completes
     */
    public void testForkJoin() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask f = new FibTask(8);
                assertSame(f, f.fork());
                Integer r = f.join();
                assertEquals(21, (int) r);
                checkCompletedNormally(f, r);
                return r;
            }};
        assertEquals(21, (int) testInvokeOnPool(mainPool(), a));
    }

    /**
     * get of a forked task returns when task completes
     */
    public void testForkGet() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() throws Exception {
                FibTask f = new FibTask(8);
                assertSame(f, f.fork());
                Integer r = f.get();
                assertEquals(21, (int) r);
                checkCompletedNormally(f, r);
                return r;
            }};
        assertEquals(21, (int) testInvokeOnPool(mainPool(), a));
    }

    /**
     * timed get of a forked task returns when task completes
     */
    public void testForkTimedGet() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() throws Exception {
                FibTask f = new FibTask(8);
                assertSame(f, f.fork());
                Integer r = f.get(LONG_DELAY_MS, MILLISECONDS);
                assertEquals(21, (int) r);
                checkCompletedNormally(f, r);
                return r;
            }};
        assertEquals(21, (int) testInvokeOnPool(mainPool(), a));
    }

    /**
     * quietlyJoin of a forked task returns when task completes
     */
    public void testForkQuietlyJoin() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask f = new FibTask(8);
                assertSame(f, f.fork());
                f.quietlyJoin();
                Integer r = f.getRawResult();
                assertEquals(21, (int) r);
                checkCompletedNormally(f, r);
                return r;
            }};
        assertEquals(21, (int) testInvokeOnPool(mainPool(), a));
    }

    /**
     * helpQuiesce returns when tasks are complete.
     * getQueuedTaskCount returns 0 when quiescent
     */
    public void testForkHelpQuiesce() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask f = new FibTask(8);
                assertSame(f, f.fork());
                helpQuiesce();
                while (!f.isDone()) // wait out race
                    ;
                assertEquals(0, getQueuedTaskCount());
                checkCompletedNormally(f, 21);
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * invoke task throws exception when task completes abnormally
     */
    public void testAbnormalInvoke() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FailingFibTask f = new FailingFibTask(8);
                try {
                    f.invoke();
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(f, success);
                }
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * quietlyInvoke task returns when task completes abnormally
     */
    public void testAbnormalQuietlyInvoke() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FailingFibTask f = new FailingFibTask(8);
                f.quietlyInvoke();
                assertTrue(f.getException() instanceof FJException);
                checkCompletedAbnormally(f, f.getException());
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * join of a forked task throws exception when task completes abnormally
     */
    public void testAbnormalForkJoin() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FailingFibTask f = new FailingFibTask(8);
                assertSame(f, f.fork());
                try {
                    f.join();
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(f, success);
                }
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * get of a forked task throws exception when task completes abnormally
     */
    public void testAbnormalForkGet() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() throws Exception {
                FailingFibTask f = new FailingFibTask(8);
                assertSame(f, f.fork());
                try {
                    f.get();
                    shouldThrow();
                } catch (ExecutionException success) {
                    Throwable cause = success.getCause();
                    assertTrue(cause instanceof FJException);
                    checkCompletedAbnormally(f, cause);
                }
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * timed get of a forked task throws exception when task completes abnormally
     */
    public void testAbnormalForkTimedGet() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() throws Exception {
                FailingFibTask f = new FailingFibTask(8);
                assertSame(f, f.fork());
                try {
                    f.get(LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (ExecutionException success) {
                    Throwable cause = success.getCause();
                    assertTrue(cause instanceof FJException);
                    checkCompletedAbnormally(f, cause);
                }
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * quietlyJoin of a forked task returns when task completes abnormally
     */
    public void testAbnormalForkQuietlyJoin() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FailingFibTask f = new FailingFibTask(8);
                assertSame(f, f.fork());
                f.quietlyJoin();
                assertTrue(f.getException() instanceof FJException);
                checkCompletedAbnormally(f, f.getException());
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * invoke task throws exception when task cancelled
     */
    public void testCancelledInvoke() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask f = new FibTask(8);
                assertTrue(f.cancel(true));
                try {
                    f.invoke();
                    shouldThrow();
                } catch (CancellationException success) {
                    checkCancelled(f);
                }
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * join of a forked task throws exception when task cancelled
     */
    public void testCancelledForkJoin() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask f = new FibTask(8);
                assertTrue(f.cancel(true));
                assertSame(f, f.fork());
                try {
                    f.join();
                    shouldThrow();
                } catch (CancellationException success) {
                    checkCancelled(f);
                }
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * get of a forked task throws exception when task cancelled
     */
    public void testCancelledForkGet() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() throws Exception {
                FibTask f = new FibTask(8);
                assertTrue(f.cancel(true));
                assertSame(f, f.fork());
                try {
                    f.get();
                    shouldThrow();
                } catch (CancellationException success) {
                    checkCancelled(f);
                }
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * timed get of a forked task throws exception when task cancelled
     */
    public void testCancelledForkTimedGet() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() throws Exception {
                FibTask f = new FibTask(8);
                assertTrue(f.cancel(true));
                assertSame(f, f.fork());
                try {
                    f.get(LONG_DELAY_MS, MILLISECONDS);
                    shouldThrow();
                } catch (CancellationException success) {
                    checkCancelled(f);
                }
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * quietlyJoin of a forked task returns when task cancelled
     */
    public void testCancelledForkQuietlyJoin() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask f = new FibTask(8);
                assertTrue(f.cancel(true));
                assertSame(f, f.fork());
                f.quietlyJoin();
                checkCancelled(f);
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * getPool of executing task returns its pool
     */
    public void testGetPool() {
        final ForkJoinPool mainPool = mainPool();
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                assertSame(mainPool, getPool());
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool, a));
    }

    /**
     * getPool of non-FJ task returns null
     */
    public void testGetPool2() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                assertNull(getPool());
                return NoResult;
            }};
        assertSame(NoResult, a.invoke());
    }

    /**
     * inForkJoinPool of executing task returns true
     */
    public void testInForkJoinPool() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                assertTrue(inForkJoinPool());
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * inForkJoinPool of non-FJ task returns false
     */
    public void testInForkJoinPool2() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                assertFalse(inForkJoinPool());
                return NoResult;
            }};
        assertSame(NoResult, a.invoke());
    }

    /**
     * The value set by setRawResult is returned by getRawResult
     */
    public void testSetRawResult() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                setRawResult(NoResult);
                assertSame(NoResult, getRawResult());
                return NoResult;
            }
        };
        assertSame(NoResult, a.invoke());
    }

    /**
     * A reinitialized normally completed task may be re-invoked
     */
    public void testReinitialize() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask f = new FibTask(8);
                checkNotDone(f);

                for (int i = 0; i < 3; i++) {
                    Integer r = f.invoke();
                    assertEquals(21, (int) r);
                    checkCompletedNormally(f, r);
                    f.reinitialize();
                    f.publicSetRawResult(null);
                    checkNotDone(f);
                }
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * A reinitialized abnormally completed task may be re-invoked
     */
    public void testReinitializeAbnormal() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FailingFibTask f = new FailingFibTask(8);
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
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * invoke task throws exception after invoking completeExceptionally
     */
    public void testCompleteExceptionally() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask f = new FibTask(8);
                f.completeExceptionally(new FJException());
                try {
                    f.invoke();
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(f, success);
                }
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * invoke task suppresses execution invoking complete
     */
    public void testComplete() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask f = new FibTask(8);
                f.complete(NoResult);
                Integer r = f.invoke();
                assertSame(NoResult, r);
                checkCompletedNormally(f, NoResult);
                return r;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * invokeAll(t1, t2) invokes all task arguments
     */
    public void testInvokeAll2() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask f = new FibTask(8);
                FibTask g = new FibTask(9);
                invokeAll(f, g);
                checkCompletedNormally(f, 21);
                checkCompletedNormally(g, 34);
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * invokeAll(tasks) with 1 argument invokes task
     */
    public void testInvokeAll1() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask f = new FibTask(8);
                invokeAll(f);
                checkCompletedNormally(f, 21);
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * invokeAll(tasks) with > 2 argument invokes tasks
     */
    public void testInvokeAll3() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask f = new FibTask(8);
                FibTask g = new FibTask(9);
                FibTask h = new FibTask(7);
                invokeAll(f, g, h);
                assertTrue(f.isDone());
                assertTrue(g.isDone());
                assertTrue(h.isDone());
                checkCompletedNormally(f, 21);
                checkCompletedNormally(g, 34);
                checkCompletedNormally(h, 13);
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * invokeAll(collection) invokes all tasks in the collection
     */
    public void testInvokeAllCollection() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask f = new FibTask(8);
                FibTask g = new FibTask(9);
                FibTask h = new FibTask(7);
                HashSet<ForkJoinTask<?>> set = new HashSet<>();
                set.add(f);
                set.add(g);
                set.add(h);
                invokeAll(set);
                assertTrue(f.isDone());
                assertTrue(g.isDone());
                assertTrue(h.isDone());
                checkCompletedNormally(f, 21);
                checkCompletedNormally(g, 34);
                checkCompletedNormally(h, 13);
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * invokeAll(tasks) with any null task throws NPE
     */
    public void testInvokeAllNPE() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask f = new FibTask(8);
                FibTask g = new FibTask(9);
                FibTask h = null;
                try {
                    invokeAll(f, g, h);
                    shouldThrow();
                } catch (NullPointerException success) {}
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * invokeAll(t1, t2) throw exception if any task does
     */
    public void testAbnormalInvokeAll2() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask f = new FibTask(8);
                FailingFibTask g = new FailingFibTask(9);
                try {
                    invokeAll(f, g);
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(g, success);
                }
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * invokeAll(tasks) with 1 argument throws exception if task does
     */
    public void testAbnormalInvokeAll1() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FailingFibTask g = new FailingFibTask(9);
                try {
                    invokeAll(g);
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(g, success);
                }
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * invokeAll(tasks) with > 2 argument throws exception if any task does
     */
    public void testAbnormalInvokeAll3() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask f = new FibTask(8);
                FailingFibTask g = new FailingFibTask(9);
                FibTask h = new FibTask(7);
                try {
                    invokeAll(f, g, h);
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(g, success);
                }
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * invokeAll(collection) throws exception if any task does
     */
    public void testAbnormalInvokeAllCollection() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FailingFibTask f = new FailingFibTask(8);
                FibTask g = new FibTask(9);
                FibTask h = new FibTask(7);
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
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(mainPool(), a));
    }

    /**
     * tryUnfork returns true for most recent unexecuted task,
     * and suppresses execution
     */
    public void testTryUnfork() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask g = new FibTask(9);
                assertSame(g, g.fork());
                FibTask f = new FibTask(8);
                assertSame(f, f.fork());
                assertTrue(f.tryUnfork());
                helpQuiesce();
                checkNotDone(f);
                checkCompletedNormally(g, 34);
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(singletonPool(), a));
    }

    /**
     * getSurplusQueuedTaskCount returns > 0 when
     * there are more tasks than threads
     */
    public void testGetSurplusQueuedTaskCount() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask h = new FibTask(7);
                assertSame(h, h.fork());
                FibTask g = new FibTask(9);
                assertSame(g, g.fork());
                FibTask f = new FibTask(8);
                assertSame(f, f.fork());
                assertTrue(getSurplusQueuedTaskCount() > 0);
                helpQuiesce();
                assertEquals(0, getSurplusQueuedTaskCount());
                checkCompletedNormally(f, 21);
                checkCompletedNormally(g, 34);
                checkCompletedNormally(h, 13);
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(singletonPool(), a));
    }

    /**
     * peekNextLocalTask returns most recent unexecuted task.
     */
    public void testPeekNextLocalTask() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask g = new FibTask(9);
                assertSame(g, g.fork());
                FibTask f = new FibTask(8);
                assertSame(f, f.fork());
                assertSame(f, peekNextLocalTask());
                checkCompletesNormally(f, 21);
                helpQuiesce();
                checkCompletedNormally(g, 34);
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(singletonPool(), a));
    }

    /**
     * pollNextLocalTask returns most recent unexecuted task
     * without executing it
     */
    public void testPollNextLocalTask() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask g = new FibTask(9);
                assertSame(g, g.fork());
                FibTask f = new FibTask(8);
                assertSame(f, f.fork());
                assertSame(f, pollNextLocalTask());
                helpQuiesce();
                checkNotDone(f);
                checkCompletedNormally(g, 34);
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(singletonPool(), a));
    }

    /**
     * pollTask returns an unexecuted task without executing it
     */
    public void testPollTask() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask g = new FibTask(9);
                assertSame(g, g.fork());
                FibTask f = new FibTask(8);
                assertSame(f, f.fork());
                assertSame(f, pollTask());
                helpQuiesce();
                checkNotDone(f);
                checkCompletedNormally(g, 34);
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(singletonPool(), a));
    }

    /**
     * peekNextLocalTask returns least recent unexecuted task in async mode
     */
    public void testPeekNextLocalTaskAsync() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask g = new FibTask(9);
                assertSame(g, g.fork());
                FibTask f = new FibTask(8);
                assertSame(f, f.fork());
                assertSame(g, peekNextLocalTask());
                assertEquals(21, (int) f.join());
                helpQuiesce();
                checkCompletedNormally(f, 21);
                checkCompletedNormally(g, 34);
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(asyncSingletonPool(), a));
    }

    /**
     * pollNextLocalTask returns least recent unexecuted task without
     * executing it, in async mode
     */
    public void testPollNextLocalTaskAsync() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask g = new FibTask(9);
                assertSame(g, g.fork());
                FibTask f = new FibTask(8);
                assertSame(f, f.fork());
                assertSame(g, pollNextLocalTask());
                helpQuiesce();
                checkCompletedNormally(f, 21);
                checkNotDone(g);
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(asyncSingletonPool(), a));
    }

    /**
     * pollTask returns an unexecuted task without executing it, in
     * async mode
     */
    public void testPollTaskAsync() {
        RecursiveTask<Integer> a = new CheckedRecursiveTask<>() {
            public Integer realCompute() {
                FibTask g = new FibTask(9);
                assertSame(g, g.fork());
                FibTask f = new FibTask(8);
                assertSame(f, f.fork());
                assertSame(g, pollTask());
                helpQuiesce();
                checkCompletedNormally(f, 21);
                checkNotDone(g);
                return NoResult;
            }};
        assertSame(NoResult, testInvokeOnPool(asyncSingletonPool(), a));
    }

}
