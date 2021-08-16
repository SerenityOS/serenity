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

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeoutException;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ForkJoinTask8Test extends JSR166TestCase {

    /*
     * Testing notes: This differs from ForkJoinTaskTest mainly by
     * defining a version of BinaryAsyncAction that uses JDK8 task
     * tags for control state, thereby testing getForkJoinTaskTag,
     * setForkJoinTaskTag, and compareAndSetForkJoinTaskTag across
     * various contexts. Most of the test methods using it are
     * otherwise identical, but omitting retest of those dealing with
     * cancellation, which is not represented in this tag scheme.
     */

    static final short INITIAL_STATE = -1;
    static final short COMPLETE_STATE = 0;
    static final short EXCEPTION_STATE = 1;

    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        return new TestSuite(ForkJoinTask8Test.class);
    }

    // Runs with "mainPool" use > 1 thread. singletonPool tests use 1
    static final int mainPoolSize =
        Math.max(2, Runtime.getRuntime().availableProcessors());

    private static ForkJoinPool mainPool() {
        return new ForkJoinPool(mainPoolSize);
    }

    private static ForkJoinPool singletonPool() {
        return new ForkJoinPool(1);
    }

    private static ForkJoinPool asyncSingletonPool() {
        return new ForkJoinPool(1,
                                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                                null, true);
    }

    // Compute fib naively and efficiently
    final int[] fib;
    {
        int[] fib = new int[10];
        fib[0] = 0;
        fib[1] = 1;
        for (int i = 2; i < fib.length; i++)
            fib[i] = fib[i - 1] + fib[i - 2];
        this.fib = fib;
    }

    private void testInvokeOnPool(ForkJoinPool pool, RecursiveAction a) {
        try (PoolCleaner cleaner = cleaner(pool)) {
            assertFalse(a.isDone());
            assertFalse(a.isCompletedNormally());
            assertFalse(a.isCompletedAbnormally());
            assertFalse(a.isCancelled());
            assertNull(a.getException());
            assertNull(a.getRawResult());

            assertNull(pool.invoke(a));

            assertTrue(a.isDone());
            assertTrue(a.isCompletedNormally());
            assertFalse(a.isCompletedAbnormally());
            assertFalse(a.isCancelled());
            assertNull(a.getException());
            assertNull(a.getRawResult());
        }
    }

    void checkNotDone(ForkJoinTask<?> a) {
        assertFalse(a.isDone());
        assertFalse(a.isCompletedNormally());
        assertFalse(a.isCompletedAbnormally());
        assertFalse(a.isCancelled());
        assertNull(a.getException());
        assertNull(a.getRawResult());
        if (a instanceof BinaryAsyncAction)
            assertEquals(INITIAL_STATE,
                         ((BinaryAsyncAction)a).getForkJoinTaskTag());

        try {
            a.get(randomExpiredTimeout(), randomTimeUnit());
            shouldThrow();
        } catch (TimeoutException success) {
        } catch (Throwable fail) { threadUnexpectedException(fail); }
    }

    <T> void checkCompletedNormally(ForkJoinTask<T> a) {
        checkCompletedNormally(a, null);
    }

    <T> void checkCompletedNormally(ForkJoinTask<T> a, T expectedValue) {
        assertTrue(a.isDone());
        assertFalse(a.isCancelled());
        assertTrue(a.isCompletedNormally());
        assertFalse(a.isCompletedAbnormally());
        assertNull(a.getException());
        assertSame(expectedValue, a.getRawResult());
        if (a instanceof BinaryAsyncAction)
            assertEquals(COMPLETE_STATE,
                         ((BinaryAsyncAction)a).getForkJoinTaskTag());

        {
            Thread.currentThread().interrupt();
            long startTime = System.nanoTime();
            assertSame(expectedValue, a.join());
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
            Thread.interrupted();
        }

        {
            Thread.currentThread().interrupt();
            long startTime = System.nanoTime();
            a.quietlyJoin();        // should be no-op
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
            Thread.interrupted();
        }

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

    void checkCompletedAbnormally(ForkJoinTask<?> a, Throwable t) {
        assertTrue(a.isDone());
        assertFalse(a.isCancelled());
        assertFalse(a.isCompletedNormally());
        assertTrue(a.isCompletedAbnormally());
        assertSame(t.getClass(), a.getException().getClass());
        assertNull(a.getRawResult());
        assertFalse(a.cancel(false));
        assertFalse(a.cancel(true));
        if (a instanceof BinaryAsyncAction)
            assertTrue(((BinaryAsyncAction)a).getForkJoinTaskTag() != INITIAL_STATE);

        try {
            Thread.currentThread().interrupt();
            a.join();
            shouldThrow();
        } catch (Throwable expected) {
            assertSame(t.getClass(), expected.getClass());
        }
        Thread.interrupted();

        {
            long startTime = System.nanoTime();
            a.quietlyJoin();        // should be no-op
            assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS);
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
        FJException() { super(); }
    }

    abstract static class BinaryAsyncAction extends ForkJoinTask<Void> {

        private volatile BinaryAsyncAction parent;

        private volatile BinaryAsyncAction sibling;

        protected BinaryAsyncAction() {
            setForkJoinTaskTag(INITIAL_STATE);
        }

        public final Void getRawResult() { return null; }
        protected final void setRawResult(Void mustBeNull) { }

        public final void linkSubtasks(BinaryAsyncAction x, BinaryAsyncAction y) {
            x.parent = y.parent = this;
            x.sibling = y;
            y.sibling = x;
        }

        protected void onComplete(BinaryAsyncAction x, BinaryAsyncAction y) {
            if (this.getForkJoinTaskTag() != COMPLETE_STATE ||
                x.getForkJoinTaskTag() != COMPLETE_STATE ||
                y.getForkJoinTaskTag() != COMPLETE_STATE) {
                completeThisExceptionally(new FJException());
            }
        }

        protected boolean onException() {
            return true;
        }

        public void linkAndForkSubtasks(BinaryAsyncAction x, BinaryAsyncAction y) {
            linkSubtasks(x, y);
            y.fork();
            x.fork();
        }

        private void completeThis() {
            setForkJoinTaskTag(COMPLETE_STATE);
            super.complete(null);
        }

        private void completeThisExceptionally(Throwable ex) {
            setForkJoinTaskTag(EXCEPTION_STATE);
            super.completeExceptionally(ex);
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            if (super.cancel(mayInterruptIfRunning)) {
                completeExceptionally(new FJException());
                return true;
            }
            return false;
        }

        public final void complete() {
            BinaryAsyncAction a = this;
            for (;;) {
                BinaryAsyncAction s = a.sibling;
                BinaryAsyncAction p = a.parent;
                a.sibling = null;
                a.parent = null;
                a.completeThis();
                if (p == null ||
                    p.compareAndSetForkJoinTaskTag(INITIAL_STATE, COMPLETE_STATE))
                    break;
                try {
                    p.onComplete(a, s);
                } catch (Throwable rex) {
                    p.completeExceptionally(rex);
                    return;
                }
                a = p;
            }
        }

        public final void completeExceptionally(Throwable ex) {
            for (BinaryAsyncAction a = this;;) {
                a.completeThisExceptionally(ex);
                BinaryAsyncAction s = a.sibling;
                if (s != null && !s.isDone())
                    s.completeExceptionally(ex);
                if ((a = a.parent) == null)
                    break;
            }
        }

        public final BinaryAsyncAction getParent() {
            return parent;
        }

        public BinaryAsyncAction getSibling() {
            return sibling;
        }

        public void reinitialize() {
            parent = sibling = null;
            super.reinitialize();
        }

    }

    final class AsyncFib extends BinaryAsyncAction {
        int number;
        int expectedResult;
        public AsyncFib(int number) {
            this.number = number;
            this.expectedResult = fib[number];
        }

        public final boolean exec() {
            try {
                AsyncFib f = this;
                int n = f.number;
                while (n > 1) {
                    AsyncFib p = f;
                    AsyncFib r = new AsyncFib(n - 2);
                    f = new AsyncFib(--n);
                    p.linkSubtasks(r, f);
                    r.fork();
                }
                f.complete();
            }
            catch (Throwable ex) {
                compareAndSetForkJoinTaskTag(INITIAL_STATE, EXCEPTION_STATE);
            }
            if (getForkJoinTaskTag() == EXCEPTION_STATE)
                throw new FJException();
            return false;
        }

        protected void onComplete(BinaryAsyncAction x, BinaryAsyncAction y) {
            number = ((AsyncFib)x).number + ((AsyncFib)y).number;
            super.onComplete(x, y);
        }

        public void checkCompletedNormally() {
            assertEquals(expectedResult, number);
            ForkJoinTask8Test.this.checkCompletedNormally(this);
        }
    }

    static final class FailingAsyncFib extends BinaryAsyncAction {
        int number;
        public FailingAsyncFib(int n) {
            this.number = n;
        }

        public final boolean exec() {
            try {
                FailingAsyncFib f = this;
                int n = f.number;
                while (n > 1) {
                    FailingAsyncFib p = f;
                    FailingAsyncFib r = new FailingAsyncFib(n - 2);
                    f = new FailingAsyncFib(--n);
                    p.linkSubtasks(r, f);
                    r.fork();
                }
                f.complete();
            }
            catch (Throwable ex) {
                compareAndSetForkJoinTaskTag(INITIAL_STATE, EXCEPTION_STATE);
            }
            if (getForkJoinTaskTag() == EXCEPTION_STATE)
                throw new FJException();
            return false;
        }

        protected void onComplete(BinaryAsyncAction x, BinaryAsyncAction y) {
            completeExceptionally(new FJException());
        }
    }

    /**
     * invoke returns when task completes normally.
     * isCompletedAbnormally and isCancelled return false for normally
     * completed tasks; getRawResult returns null.
     */
    public void testInvoke() {
        testInvoke(mainPool());
    }
    public void testInvoke_Singleton() {
        testInvoke(singletonPool());
    }
    public void testInvoke(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                AsyncFib f = new AsyncFib(8);
                assertNull(f.invoke());
                f.checkCompletedNormally();
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * quietlyInvoke task returns when task completes normally.
     * isCompletedAbnormally and isCancelled return false for normally
     * completed tasks
     */
    public void testQuietlyInvoke() {
        testQuietlyInvoke(mainPool());
    }
    public void testQuietlyInvoke_Singleton() {
        testQuietlyInvoke(singletonPool());
    }
    public void testQuietlyInvoke(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                AsyncFib f = new AsyncFib(8);
                f.quietlyInvoke();
                f.checkCompletedNormally();
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * join of a forked task returns when task completes
     */
    public void testForkJoin() {
        testForkJoin(mainPool());
    }
    public void testForkJoin_Singleton() {
        testForkJoin(singletonPool());
    }
    public void testForkJoin(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                AsyncFib f = new AsyncFib(8);
                assertSame(f, f.fork());
                assertNull(f.join());
                f.checkCompletedNormally();
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * get of a forked task returns when task completes
     */
    public void testForkGet() {
        testForkGet(mainPool());
    }
    public void testForkGet_Singleton() {
        testForkGet(singletonPool());
    }
    public void testForkGet(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() throws Exception {
                AsyncFib f = new AsyncFib(8);
                assertSame(f, f.fork());
                assertNull(f.get());
                f.checkCompletedNormally();
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * timed get of a forked task returns when task completes
     */
    public void testForkTimedGet() {
        testForkTimedGet(mainPool());
    }
    public void testForkTimedGet_Singleton() {
        testForkTimedGet(singletonPool());
    }
    public void testForkTimedGet(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() throws Exception {
                AsyncFib f = new AsyncFib(8);
                assertSame(f, f.fork());
                assertNull(f.get(LONG_DELAY_MS, MILLISECONDS));
                f.checkCompletedNormally();
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * timed get with null time unit throws NullPointerException
     */
    public void testForkTimedGetNullTimeUnit() {
        testForkTimedGetNullTimeUnit(mainPool());
    }
    public void testForkTimedGetNullTimeUnit_Singleton() {
        testForkTimedGet(singletonPool());
    }
    public void testForkTimedGetNullTimeUnit(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() throws Exception {
                AsyncFib f = new AsyncFib(8);
                assertSame(f, f.fork());
                try {
                    f.get(randomTimeout(), null);
                    shouldThrow();
                } catch (NullPointerException success) {}
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * quietlyJoin of a forked task returns when task completes
     */
    public void testForkQuietlyJoin() {
        testForkQuietlyJoin(mainPool());
    }
    public void testForkQuietlyJoin_Singleton() {
        testForkQuietlyJoin(singletonPool());
    }
    public void testForkQuietlyJoin(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                AsyncFib f = new AsyncFib(8);
                assertSame(f, f.fork());
                f.quietlyJoin();
                f.checkCompletedNormally();
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * helpQuiesce returns when tasks are complete.
     * getQueuedTaskCount returns 0 when quiescent
     */
    public void testForkHelpQuiesce() {
        testForkHelpQuiesce(mainPool());
    }
    public void testForkHelpQuiesce_Singleton() {
        testForkHelpQuiesce(singletonPool());
    }
    public void testForkHelpQuiesce(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                AsyncFib f = new AsyncFib(8);
                assertSame(f, f.fork());
                helpQuiesce();
                while (!f.isDone()) // wait out race
                    ;
                assertEquals(0, getQueuedTaskCount());
                f.checkCompletedNormally();
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * invoke task throws exception when task completes abnormally
     */
    public void testAbnormalInvoke() {
        testAbnormalInvoke(mainPool());
    }
    public void testAbnormalInvoke_Singleton() {
        testAbnormalInvoke(singletonPool());
    }
    public void testAbnormalInvoke(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FailingAsyncFib f = new FailingAsyncFib(8);
                try {
                    f.invoke();
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(f, success);
                }
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * quietlyInvoke task returns when task completes abnormally
     */
    public void testAbnormalQuietlyInvoke() {
        testAbnormalQuietlyInvoke(mainPool());
    }
    public void testAbnormalQuietlyInvoke_Singleton() {
        testAbnormalQuietlyInvoke(singletonPool());
    }
    public void testAbnormalQuietlyInvoke(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FailingAsyncFib f = new FailingAsyncFib(8);
                f.quietlyInvoke();
                assertTrue(f.getException() instanceof FJException);
                checkCompletedAbnormally(f, f.getException());
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * join of a forked task throws exception when task completes abnormally
     */
    public void testAbnormalForkJoin() {
        testAbnormalForkJoin(mainPool());
    }
    public void testAbnormalForkJoin_Singleton() {
        testAbnormalForkJoin(singletonPool());
    }
    public void testAbnormalForkJoin(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FailingAsyncFib f = new FailingAsyncFib(8);
                assertSame(f, f.fork());
                try {
                    f.join();
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(f, success);
                }
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * get of a forked task throws exception when task completes abnormally
     */
    public void testAbnormalForkGet() {
        testAbnormalForkGet(mainPool());
    }
    public void testAbnormalForkGet_Singleton() {
        testAbnormalForkJoin(singletonPool());
    }
    public void testAbnormalForkGet(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() throws Exception {
                FailingAsyncFib f = new FailingAsyncFib(8);
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
        testInvokeOnPool(pool, a);
    }

    /**
     * timed get of a forked task throws exception when task completes abnormally
     */
    public void testAbnormalForkTimedGet() {
        testAbnormalForkTimedGet(mainPool());
    }
    public void testAbnormalForkTimedGet_Singleton() {
        testAbnormalForkTimedGet(singletonPool());
    }
    public void testAbnormalForkTimedGet(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() throws Exception {
                FailingAsyncFib f = new FailingAsyncFib(8);
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
        testInvokeOnPool(pool, a);
    }

    /**
     * quietlyJoin of a forked task returns when task completes abnormally
     */
    public void testAbnormalForkQuietlyJoin() {
        testAbnormalForkQuietlyJoin(mainPool());
    }
    public void testAbnormalForkQuietlyJoin_Singleton() {
        testAbnormalForkQuietlyJoin(singletonPool());
    }
    public void testAbnormalForkQuietlyJoin(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FailingAsyncFib f = new FailingAsyncFib(8);
                assertSame(f, f.fork());
                f.quietlyJoin();
                assertTrue(f.getException() instanceof FJException);
                checkCompletedAbnormally(f, f.getException());
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * getPool of executing task returns its pool
     */
    public void testGetPool() {
        testGetPool(mainPool());
    }
    public void testGetPool_Singleton() {
        testGetPool(singletonPool());
    }
    public void testGetPool(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                assertSame(pool, getPool());
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * getPool of non-FJ task returns null
     */
    public void testGetPool2() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                assertNull(getPool());
            }};
        assertNull(a.invoke());
    }

    /**
     * inForkJoinPool of executing task returns true
     */
    public void testInForkJoinPool() {
        testInForkJoinPool(mainPool());
    }
    public void testInForkJoinPool_Singleton() {
        testInForkJoinPool(singletonPool());
    }
    public void testInForkJoinPool(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                assertTrue(inForkJoinPool());
            }};
        testInvokeOnPool(pool, a);
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
     * setRawResult(null) succeeds
     */
    public void testSetRawResult() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                setRawResult(null);
                assertNull(getRawResult());
            }};
        assertNull(a.invoke());
    }

    /**
     * invoke task throws exception after invoking completeExceptionally
     */
    public void testCompleteExceptionally() {
        testCompleteExceptionally(mainPool());
    }
    public void testCompleteExceptionally_Singleton() {
        testCompleteExceptionally(singletonPool());
    }
    public void testCompleteExceptionally(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                AsyncFib f = new AsyncFib(8);
                f.completeExceptionally(new FJException());
                try {
                    f.invoke();
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(f, success);
                }
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * invokeAll(tasks) with 1 argument invokes task
     */
    public void testInvokeAll1() {
        testInvokeAll1(mainPool());
    }
    public void testInvokeAll1_Singleton() {
        testInvokeAll1(singletonPool());
    }
    public void testInvokeAll1(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                AsyncFib f = new AsyncFib(8);
                invokeAll(f);
                f.checkCompletedNormally();
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * invokeAll(t1, t2) invokes all task arguments
     */
    public void testInvokeAll2() {
        testInvokeAll2(mainPool());
    }
    public void testInvokeAll2_Singleton() {
        testInvokeAll2(singletonPool());
    }
    public void testInvokeAll2(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                AsyncFib[] tasks = {
                    new AsyncFib(8),
                    new AsyncFib(9),
                };
                invokeAll(tasks[0], tasks[1]);
                for (AsyncFib task : tasks) assertTrue(task.isDone());
                for (AsyncFib task : tasks) task.checkCompletedNormally();
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * invokeAll(tasks) with > 2 argument invokes tasks
     */
    public void testInvokeAll3() {
        testInvokeAll3(mainPool());
    }
    public void testInvokeAll3_Singleton() {
        testInvokeAll3(singletonPool());
    }
    public void testInvokeAll3(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                AsyncFib[] tasks = {
                    new AsyncFib(8),
                    new AsyncFib(9),
                    new AsyncFib(7),
                };
                invokeAll(tasks[0], tasks[1], tasks[2]);
                for (AsyncFib task : tasks) assertTrue(task.isDone());
                for (AsyncFib task : tasks) task.checkCompletedNormally();
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * invokeAll(collection) invokes all tasks in the collection
     */
    public void testInvokeAllCollection() {
        testInvokeAllCollection(mainPool());
    }
    public void testInvokeAllCollection_Singleton() {
        testInvokeAllCollection(singletonPool());
    }
    public void testInvokeAllCollection(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                AsyncFib[] tasks = {
                    new AsyncFib(8),
                    new AsyncFib(9),
                    new AsyncFib(7),
                };
                invokeAll(Arrays.asList(tasks));
                for (AsyncFib task : tasks) assertTrue(task.isDone());
                for (AsyncFib task : tasks) task.checkCompletedNormally();
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * invokeAll(tasks) with any null task throws NullPointerException
     */
    public void testInvokeAllNullTask() {
        testInvokeAllNullTask(mainPool());
    }
    public void testInvokeAllNullTask_Singleton() {
        testInvokeAllNullTask(singletonPool());
    }
    public void testInvokeAllNullTask(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                AsyncFib nul = null;
                assertThrows(
                    NullPointerException.class,
                    () -> invokeAll(nul),
                    () -> invokeAll(nul, nul),
                    () -> invokeAll(new AsyncFib(8), new AsyncFib(9), nul),
                    () -> invokeAll(new AsyncFib(8), nul, new AsyncFib(9)),
                    () -> invokeAll(nul, new AsyncFib(8), new AsyncFib(9)));
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * invokeAll(tasks) with 1 argument throws exception if task does
     */
    public void testAbnormalInvokeAll1() {
        testAbnormalInvokeAll1(mainPool());
    }
    public void testAbnormalInvokeAll1_Singleton() {
        testAbnormalInvokeAll1(singletonPool());
    }
    public void testAbnormalInvokeAll1(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FailingAsyncFib g = new FailingAsyncFib(9);
                try {
                    invokeAll(g);
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(g, success);
                }
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * invokeAll(t1, t2) throw exception if any task does
     */
    public void testAbnormalInvokeAll2() {
        testAbnormalInvokeAll2(mainPool());
    }
    public void testAbnormalInvokeAll2_Singleton() {
        testAbnormalInvokeAll2(singletonPool());
    }
    public void testAbnormalInvokeAll2(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                AsyncFib f = new AsyncFib(8);
                FailingAsyncFib g = new FailingAsyncFib(9);
                ForkJoinTask<?>[] tasks = { f, g };
                shuffle(tasks);
                try {
                    invokeAll(tasks[0], tasks[1]);
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(g, success);
                }
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * invokeAll(tasks) with > 2 argument throws exception if any task does
     */
    public void testAbnormalInvokeAll3() {
        testAbnormalInvokeAll3(mainPool());
    }
    public void testAbnormalInvokeAll3_Singleton() {
        testAbnormalInvokeAll3(singletonPool());
    }
    public void testAbnormalInvokeAll3(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                AsyncFib f = new AsyncFib(8);
                FailingAsyncFib g = new FailingAsyncFib(9);
                AsyncFib h = new AsyncFib(7);
                ForkJoinTask<?>[] tasks = { f, g, h };
                shuffle(tasks);
                try {
                    invokeAll(tasks[0], tasks[1], tasks[2]);
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(g, success);
                }
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * invokeAll(collection) throws exception if any task does
     */
    public void testAbnormalInvokeAllCollection() {
        testAbnormalInvokeAllCollection(mainPool());
    }
    public void testAbnormalInvokeAllCollection_Singleton() {
        testAbnormalInvokeAllCollection(singletonPool());
    }
    public void testAbnormalInvokeAllCollection(ForkJoinPool pool) {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                FailingAsyncFib f = new FailingAsyncFib(8);
                AsyncFib g = new AsyncFib(9);
                AsyncFib h = new AsyncFib(7);
                ForkJoinTask<?>[] tasks = { f, g, h };
                shuffle(tasks);
                try {
                    invokeAll(Arrays.asList(tasks));
                    shouldThrow();
                } catch (FJException success) {
                    checkCompletedAbnormally(f, success);
                }
            }};
        testInvokeOnPool(pool, a);
    }

    /**
     * tryUnfork returns true for most recent unexecuted task,
     * and suppresses execution
     */
    public void testTryUnfork() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                AsyncFib g = new AsyncFib(9);
                assertSame(g, g.fork());
                AsyncFib f = new AsyncFib(8);
                assertSame(f, f.fork());
                assertTrue(f.tryUnfork());
                helpQuiesce();
                checkNotDone(f);
                g.checkCompletedNormally();
            }};
        testInvokeOnPool(singletonPool(), a);
    }

    /**
     * getSurplusQueuedTaskCount returns > 0 when
     * there are more tasks than threads
     */
    public void testGetSurplusQueuedTaskCount() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                AsyncFib h = new AsyncFib(7);
                assertSame(h, h.fork());
                AsyncFib g = new AsyncFib(9);
                assertSame(g, g.fork());
                AsyncFib f = new AsyncFib(8);
                assertSame(f, f.fork());
                assertTrue(getSurplusQueuedTaskCount() > 0);
                helpQuiesce();
                assertEquals(0, getSurplusQueuedTaskCount());
                f.checkCompletedNormally();
                g.checkCompletedNormally();
                h.checkCompletedNormally();
            }};
        testInvokeOnPool(singletonPool(), a);
    }

    /**
     * peekNextLocalTask returns most recent unexecuted task.
     */
    public void testPeekNextLocalTask() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                AsyncFib g = new AsyncFib(9);
                assertSame(g, g.fork());
                AsyncFib f = new AsyncFib(8);
                assertSame(f, f.fork());
                assertSame(f, peekNextLocalTask());
                assertNull(f.join());
                f.checkCompletedNormally();
                helpQuiesce();
                g.checkCompletedNormally();
            }};
        testInvokeOnPool(singletonPool(), a);
    }

    /**
     * pollNextLocalTask returns most recent unexecuted task without
     * executing it
     */
    public void testPollNextLocalTask() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                AsyncFib g = new AsyncFib(9);
                assertSame(g, g.fork());
                AsyncFib f = new AsyncFib(8);
                assertSame(f, f.fork());
                assertSame(f, pollNextLocalTask());
                helpQuiesce();
                checkNotDone(f);
                g.checkCompletedNormally();
            }};
        testInvokeOnPool(singletonPool(), a);
    }

    /**
     * pollTask returns an unexecuted task without executing it
     */
    public void testPollTask() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                AsyncFib g = new AsyncFib(9);
                assertSame(g, g.fork());
                AsyncFib f = new AsyncFib(8);
                assertSame(f, f.fork());
                assertSame(f, pollTask());
                helpQuiesce();
                checkNotDone(f);
                g.checkCompletedNormally();
            }};
        testInvokeOnPool(singletonPool(), a);
    }

    /**
     * peekNextLocalTask returns least recent unexecuted task in async mode
     */
    public void testPeekNextLocalTaskAsync() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                AsyncFib g = new AsyncFib(9);
                assertSame(g, g.fork());
                AsyncFib f = new AsyncFib(8);
                assertSame(f, f.fork());
                assertSame(g, peekNextLocalTask());
                assertNull(f.join());
                helpQuiesce();
                f.checkCompletedNormally();
                g.checkCompletedNormally();
            }};
        testInvokeOnPool(asyncSingletonPool(), a);
    }

    /**
     * pollNextLocalTask returns least recent unexecuted task without
     * executing it, in async mode
     */
    public void testPollNextLocalTaskAsync() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                AsyncFib g = new AsyncFib(9);
                assertSame(g, g.fork());
                AsyncFib f = new AsyncFib(8);
                assertSame(f, f.fork());
                assertSame(g, pollNextLocalTask());
                helpQuiesce();
                f.checkCompletedNormally();
                checkNotDone(g);
            }};
        testInvokeOnPool(asyncSingletonPool(), a);
    }

    /**
     * pollTask returns an unexecuted task without executing it, in
     * async mode
     */
    public void testPollTaskAsync() {
        RecursiveAction a = new CheckedRecursiveAction() {
            protected void realCompute() {
                AsyncFib g = new AsyncFib(9);
                assertSame(g, g.fork());
                AsyncFib f = new AsyncFib(8);
                assertSame(f, f.fork());
                assertSame(g, pollTask());
                helpQuiesce();
                f.checkCompletedNormally();
                checkNotDone(g);
            }};
        testInvokeOnPool(asyncSingletonPool(), a);
    }

    /**
     * ForkJoinTask.quietlyComplete returns when task completes
     * normally without setting a value. The most recent value
     * established by setRawResult(V) (or null by default) is returned
     * from invoke.
     */
    public void testQuietlyComplete() {
        RecursiveAction a = new CheckedRecursiveAction() {
                protected void realCompute() {
                    AsyncFib f = new AsyncFib(8);
                    f.quietlyComplete();
                    assertEquals(8, f.number);
                    assertTrue(f.isDone());
                    assertFalse(f.isCancelled());
                    assertTrue(f.isCompletedNormally());
                    assertFalse(f.isCompletedAbnormally());
                    assertNull(f.getException());
                }};
        testInvokeOnPool(mainPool(), a);
    }

    // jdk9

    /**
     * pollSubmission returns unexecuted submitted task, if present
     */
    public void testPollSubmission() {
        final CountDownLatch done = new CountDownLatch(1);
        final ForkJoinTask<?> a = ForkJoinTask.adapt(awaiter(done));
        final ForkJoinTask<?> b = ForkJoinTask.adapt(awaiter(done));
        final ForkJoinTask<?> c = ForkJoinTask.adapt(awaiter(done));
        final ForkJoinPool p = singletonPool();
        try (PoolCleaner cleaner = cleaner(p, done)) {
            Thread external = new Thread(new CheckedRunnable() {
                public void realRun() {
                    p.execute(a);
                    p.execute(b);
                    p.execute(c);
                }});
            RecursiveAction s = new CheckedRecursiveAction() {
                protected void realCompute() {
                    external.start();
                    try {
                        external.join();
                    } catch (Exception ex) {
                        threadUnexpectedException(ex);
                    }
                    assertTrue(p.hasQueuedSubmissions());
                    assertTrue(Thread.currentThread() instanceof ForkJoinWorkerThread);
                    ForkJoinTask<?> r = ForkJoinTask.pollSubmission();
                    assertTrue(r == a || r == b || r == c);
                    assertFalse(r.isDone());
                }};
            p.invoke(s);
        }
    }

}
