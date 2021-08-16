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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ExecutorCompletionServiceTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(ExecutorCompletionServiceTest.class);
    }

    /**
     * new ExecutorCompletionService(null) throws NullPointerException
     */
    public void testConstructorNPE() {
        try {
            new ExecutorCompletionService<Item>(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * new ExecutorCompletionService(e, null) throws NullPointerException
     */
    public void testConstructorNPE2() {
        try {
            new ExecutorCompletionService<Item>(cachedThreadPool, null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * ecs.submit(null) throws NullPointerException
     */
    public void testSubmitNullCallable() {
        CompletionService<Item> cs = new ExecutorCompletionService<>(cachedThreadPool);
        try {
            cs.submit((Callable<Item>) null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * ecs.submit(null, val) throws NullPointerException
     */
    public void testSubmitNullRunnable() {
        CompletionService<Boolean> cs = new ExecutorCompletionService<>(cachedThreadPool);
        try {
            cs.submit((Runnable) null, Boolean.TRUE);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * A taken submitted task is completed
     */
    public void testTake() throws Exception {
        CompletionService<String> cs = new ExecutorCompletionService<>(cachedThreadPool);
        cs.submit(new StringTask());
        Future<?> f = cs.take();
        assertTrue(f.isDone());
        assertSame(TEST_STRING, f.get());
    }

    /**
     * Take returns the same future object returned by submit
     */
    public void testTake2() throws InterruptedException {
        CompletionService<String> cs = new ExecutorCompletionService<>(cachedThreadPool);
        Future<?> f1 = cs.submit(new StringTask());
        Future<?> f2 = cs.take();
        assertSame(f1, f2);
    }

    /**
     * poll returns non-null when the returned task is completed
     */
    public void testPoll1() throws Exception {
        CompletionService<String> cs = new ExecutorCompletionService<>(cachedThreadPool);
        assertNull(cs.poll());
        cs.submit(new StringTask());

        long startTime = System.nanoTime();
        Future<?> f;
        while ((f = cs.poll()) == null) {
            if (millisElapsedSince(startTime) > LONG_DELAY_MS)
                fail("timed out");
            Thread.yield();
        }
        assertTrue(f.isDone());
        assertSame(TEST_STRING, f.get());
    }

    /**
     * timed poll returns non-null when the returned task is completed
     */
    public void testPoll2() throws Exception {
        CompletionService<String> cs = new ExecutorCompletionService<>(cachedThreadPool);
        assertNull(cs.poll());
        cs.submit(new StringTask());

        long startTime = System.nanoTime();
        Future<?> f;
        while ((f = cs.poll(timeoutMillis(), MILLISECONDS)) == null) {
            assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
            if (millisElapsedSince(startTime) > LONG_DELAY_MS)
                fail("timed out");
            Thread.yield();
        }
        assertTrue(f.isDone());
        assertSame(TEST_STRING, f.get());
    }

    /**
     * poll returns null before the returned task is completed
     */
    public void testPollReturnsNullBeforeCompletion() throws Exception {
        CompletionService<String> cs = new ExecutorCompletionService<>(cachedThreadPool);
        final CountDownLatch proceed = new CountDownLatch(1);
        cs.submit(new Callable<String>() { public String call() throws Exception {
            await(proceed);
            return TEST_STRING;
        }});
        assertNull(cs.poll());
        assertNull(cs.poll(0L, MILLISECONDS));
        assertNull(cs.poll(Long.MIN_VALUE, MILLISECONDS));
        long startTime = System.nanoTime();
        assertNull(cs.poll(timeoutMillis(), MILLISECONDS));
        assertTrue(millisElapsedSince(startTime) >= timeoutMillis());
        proceed.countDown();
        assertSame(TEST_STRING, cs.take().get());
    }

    /**
     * successful and failed tasks are both returned
     */
    public void testTaskAssortment() throws Exception {
        CompletionService<String> cs = new ExecutorCompletionService<>(cachedThreadPool);
        ArithmeticException ex = new ArithmeticException();
        final int rounds = 2;
        for (int i = rounds; i--> 0; ) {
            cs.submit(new StringTask());
            cs.submit(callableThrowing(ex));
            cs.submit(runnableThrowing(ex), null);
        }
        int normalCompletions = 0;
        int exceptionalCompletions = 0;
        for (int i = 3 * rounds; i--> 0; ) {
            try {
                assertSame(TEST_STRING, cs.take().get());
                normalCompletions++;
            } catch (ExecutionException expected) {
                assertSame(ex, expected.getCause());
                exceptionalCompletions++;
            }
        }
        assertEquals(1 * rounds, normalCompletions);
        assertEquals(2 * rounds, exceptionalCompletions);
        assertNull(cs.poll());
    }

    /**
     * Submitting to underlying AES that overrides newTaskFor(Callable)
     * returns and eventually runs Future returned by newTaskFor.
     */
    public void testNewTaskForCallable() throws InterruptedException {
        final AtomicBoolean done = new AtomicBoolean(false);
        class MyCallableFuture<V> extends FutureTask<V> {
            MyCallableFuture(Callable<V> c) { super(c); }
            @Override protected void done() { done.set(true); }
        }
        final ExecutorService e =
            new ThreadPoolExecutor(1, 1,
                                   30L, TimeUnit.SECONDS,
                                   new ArrayBlockingQueue<Runnable>(1)) {
                protected <T> RunnableFuture<T> newTaskFor(Callable<T> c) {
                    return new MyCallableFuture<T>(c);
                }};
        CompletionService<String> cs = new ExecutorCompletionService<>(e);
        try (PoolCleaner cleaner = cleaner(e)) {
            assertNull(cs.poll());
            Callable<String> c = new StringTask();
            Future<?> f1 = cs.submit(c);
            assertTrue("submit must return MyCallableFuture",
                       f1 instanceof MyCallableFuture);
            Future<?> f2 = cs.take();
            assertSame("submit and take must return same objects", f1, f2);
            assertTrue("completed task must have set done", done.get());
        }
    }

    /**
     * Submitting to underlying AES that overrides newTaskFor(Runnable,T)
     * returns and eventually runs Future returned by newTaskFor.
     */
    public void testNewTaskForRunnable() throws InterruptedException {
        final AtomicBoolean done = new AtomicBoolean(false);
        class MyRunnableFuture<V> extends FutureTask<V> {
            MyRunnableFuture(Runnable t, V r) { super(t, r); }
            @Override protected void done() { done.set(true); }
        }
        final ExecutorService e =
            new ThreadPoolExecutor(1, 1,
                                   30L, TimeUnit.SECONDS,
                                   new ArrayBlockingQueue<Runnable>(1)) {
                protected <T> RunnableFuture<T> newTaskFor(Runnable t, T r) {
                    return new MyRunnableFuture<T>(t, r);
                }};
        CompletionService<String> cs = new ExecutorCompletionService<>(e);
        try (PoolCleaner cleaner = cleaner(e)) {
            assertNull(cs.poll());
            Runnable r = new NoOpRunnable();
            Future<?> f1 = cs.submit(r, null);
            assertTrue("submit must return MyRunnableFuture",
                       f1 instanceof MyRunnableFuture);
            Future<?> f2 = cs.take();
            assertSame("submit and take must return same objects", f1, f2);
            assertTrue("completed task must have set done", done.get());
        }
    }

}
