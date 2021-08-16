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
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicInteger;
import junit.framework.Test;
import junit.framework.TestSuite;

import static java.util.concurrent.Flow.Subscriber;
import static java.util.concurrent.Flow.Subscription;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class SubmissionPublisherTest extends JSR166TestCase {

    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(SubmissionPublisherTest.class);
    }

    final Executor basicExecutor = basicPublisher().getExecutor();

    static SubmissionPublisher<Integer> basicPublisher() {
        return new SubmissionPublisher<Integer>();
    }

    static class SPException extends RuntimeException {}

    class TestSubscriber implements Subscriber<Integer> {
        volatile Subscription sn;
        int last;  // Requires that onNexts are in numeric order
        volatile int nexts;
        volatile int errors;
        volatile int completes;
        volatile boolean throwOnCall = false;
        volatile boolean request = true;
        volatile Throwable lastError;

        public synchronized void onSubscribe(Subscription s) {
            threadAssertTrue(sn == null);
            sn = s;
            notifyAll();
            if (throwOnCall)
                throw new SPException();
            if (request)
                sn.request(1L);
        }
        public synchronized void onNext(Integer t) {
            ++nexts;
            notifyAll();
            int current = t.intValue();
            threadAssertTrue(current >= last);
            last = current;
            if (request)
                sn.request(1L);
            if (throwOnCall)
                throw new SPException();
        }
        public synchronized void onError(Throwable t) {
            threadAssertTrue(completes == 0);
            threadAssertTrue(errors == 0);
            lastError = t;
            ++errors;
            notifyAll();
        }
        public synchronized void onComplete() {
            threadAssertTrue(completes == 0);
            ++completes;
            notifyAll();
        }

        synchronized void awaitSubscribe() {
            while (sn == null) {
                try {
                    wait();
                } catch (Exception ex) {
                    threadUnexpectedException(ex);
                    break;
                }
            }
        }
        synchronized void awaitNext(int n) {
            while (nexts < n) {
                try {
                    wait();
                } catch (Exception ex) {
                    threadUnexpectedException(ex);
                    break;
                }
            }
        }
        synchronized void awaitComplete() {
            while (completes == 0 && errors == 0) {
                try {
                    wait();
                } catch (Exception ex) {
                    threadUnexpectedException(ex);
                    break;
                }
            }
        }
        synchronized void awaitError() {
            while (errors == 0) {
                try {
                    wait();
                } catch (Exception ex) {
                    threadUnexpectedException(ex);
                    break;
                }
            }
        }

    }

    /**
     * A new SubmissionPublisher has no subscribers, a non-null
     * executor, a power-of-two capacity, is not closed, and reports
     * zero demand and lag
     */
    void checkInitialState(SubmissionPublisher<?> p) {
        assertFalse(p.hasSubscribers());
        assertEquals(0, p.getNumberOfSubscribers());
        assertTrue(p.getSubscribers().isEmpty());
        assertFalse(p.isClosed());
        assertNull(p.getClosedException());
        int n = p.getMaxBufferCapacity();
        assertTrue((n & (n - 1)) == 0); // power of two
        assertNotNull(p.getExecutor());
        assertEquals(0, p.estimateMinimumDemand());
        assertEquals(0, p.estimateMaximumLag());
    }

    /**
     * A default-constructed SubmissionPublisher has no subscribers,
     * is not closed, has default buffer size, and uses the
     * defaultExecutor
     */
    public void testConstructor1() {
        SubmissionPublisher<Integer> p = new SubmissionPublisher<>();
        checkInitialState(p);
        assertEquals(p.getMaxBufferCapacity(), Flow.defaultBufferSize());
        Executor e = p.getExecutor(), c = ForkJoinPool.commonPool();
        if (ForkJoinPool.getCommonPoolParallelism() > 1)
            assertSame(e, c);
        else
            assertNotSame(e, c);
    }

    /**
     * A new SubmissionPublisher has no subscribers, is not closed,
     * has the given buffer size, and uses the given executor
     */
    public void testConstructor2() {
        Executor e = Executors.newFixedThreadPool(1);
        SubmissionPublisher<Integer> p = new SubmissionPublisher<>(e, 8);
        checkInitialState(p);
        assertSame(p.getExecutor(), e);
        assertEquals(8, p.getMaxBufferCapacity());
    }

    /**
     * A null Executor argument to SubmissionPublisher constructor
     * throws NullPointerException
     */
    public void testConstructor3() {
        try {
            new SubmissionPublisher<Integer>(null, 8);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * A negative capacity argument to SubmissionPublisher constructor
     * throws IllegalArgumentException
     */
    public void testConstructor4() {
        Executor e = Executors.newFixedThreadPool(1);
        try {
            new SubmissionPublisher<Integer>(e, -1);
            shouldThrow();
        } catch (IllegalArgumentException success) {}
    }

    /**
     * A closed publisher reports isClosed with no closedException and
     * throws IllegalStateException upon attempted submission; a
     * subsequent close or closeExceptionally has no additional
     * effect.
     */
    public void testClose() {
        SubmissionPublisher<Integer> p = basicPublisher();
        checkInitialState(p);
        p.close();
        assertTrue(p.isClosed());
        assertNull(p.getClosedException());
        try {
            p.submit(1);
            shouldThrow();
        } catch (IllegalStateException success) {}
        Throwable ex = new SPException();
        p.closeExceptionally(ex);
        assertTrue(p.isClosed());
        assertNull(p.getClosedException());
    }

    /**
     * A publisher closedExceptionally reports isClosed with the
     * closedException and throws IllegalStateException upon attempted
     * submission; a subsequent close or closeExceptionally has no
     * additional effect.
     */
    public void testCloseExceptionally() {
        SubmissionPublisher<Integer> p = basicPublisher();
        checkInitialState(p);
        Throwable ex = new SPException();
        p.closeExceptionally(ex);
        assertTrue(p.isClosed());
        assertSame(p.getClosedException(), ex);
        try {
            p.submit(1);
            shouldThrow();
        } catch (IllegalStateException success) {}
        p.close();
        assertTrue(p.isClosed());
        assertSame(p.getClosedException(), ex);
    }

    /**
     * Upon subscription, the subscriber's onSubscribe is called, no
     * other Subscriber methods are invoked, the publisher
     * hasSubscribers, isSubscribed is true, and existing
     * subscriptions are unaffected.
     */
    public void testSubscribe1() {
        TestSubscriber s = new TestSubscriber();
        SubmissionPublisher<Integer> p = basicPublisher();
        p.subscribe(s);
        assertTrue(p.hasSubscribers());
        assertEquals(1, p.getNumberOfSubscribers());
        assertTrue(p.getSubscribers().contains(s));
        assertTrue(p.isSubscribed(s));
        s.awaitSubscribe();
        assertNotNull(s.sn);
        assertEquals(0, s.nexts);
        assertEquals(0, s.errors);
        assertEquals(0, s.completes);
        TestSubscriber s2 = new TestSubscriber();
        p.subscribe(s2);
        assertTrue(p.hasSubscribers());
        assertEquals(2, p.getNumberOfSubscribers());
        assertTrue(p.getSubscribers().contains(s));
        assertTrue(p.getSubscribers().contains(s2));
        assertTrue(p.isSubscribed(s));
        assertTrue(p.isSubscribed(s2));
        s2.awaitSubscribe();
        assertNotNull(s2.sn);
        assertEquals(0, s2.nexts);
        assertEquals(0, s2.errors);
        assertEquals(0, s2.completes);
        p.close();
    }

    /**
     * If closed, upon subscription, the subscriber's onComplete
     * method is invoked
     */
    public void testSubscribe2() {
        TestSubscriber s = new TestSubscriber();
        SubmissionPublisher<Integer> p = basicPublisher();
        p.close();
        p.subscribe(s);
        s.awaitComplete();
        assertEquals(0, s.nexts);
        assertEquals(0, s.errors);
        assertEquals(1, s.completes, 1);
    }

    /**
     * If closedExceptionally, upon subscription, the subscriber's
     * onError method is invoked
     */
    public void testSubscribe3() {
        TestSubscriber s = new TestSubscriber();
        SubmissionPublisher<Integer> p = basicPublisher();
        Throwable ex = new SPException();
        p.closeExceptionally(ex);
        assertTrue(p.isClosed());
        assertSame(p.getClosedException(), ex);
        p.subscribe(s);
        s.awaitError();
        assertEquals(0, s.nexts);
        assertEquals(1, s.errors);
    }

    /**
     * Upon attempted resubscription, the subscriber's onError is
     * called and the subscription is cancelled.
     */
    public void testSubscribe4() {
        TestSubscriber s = new TestSubscriber();
        SubmissionPublisher<Integer> p = basicPublisher();
        p.subscribe(s);
        assertTrue(p.hasSubscribers());
        assertEquals(1, p.getNumberOfSubscribers());
        assertTrue(p.getSubscribers().contains(s));
        assertTrue(p.isSubscribed(s));
        s.awaitSubscribe();
        assertNotNull(s.sn);
        assertEquals(0, s.nexts);
        assertEquals(0, s.errors);
        assertEquals(0, s.completes);
        p.subscribe(s);
        s.awaitError();
        assertEquals(0, s.nexts);
        assertEquals(1, s.errors);
        assertFalse(p.isSubscribed(s));
    }

    /**
     * An exception thrown in onSubscribe causes onError
     */
    public void testSubscribe5() {
        TestSubscriber s = new TestSubscriber();
        SubmissionPublisher<Integer> p = basicPublisher();
        s.throwOnCall = true;
        p.subscribe(s);
        s.awaitError();
        assertEquals(0, s.nexts);
        assertEquals(1, s.errors);
        assertEquals(0, s.completes);
    }

    /**
     * subscribe(null) throws NPE
     */
    public void testSubscribe6() {
        SubmissionPublisher<Integer> p = basicPublisher();
        try {
            p.subscribe(null);
            shouldThrow();
        } catch (NullPointerException success) {}
        checkInitialState(p);
    }

    /**
     * Closing a publisher causes onComplete to subscribers
     */
    public void testCloseCompletes() {
        SubmissionPublisher<Integer> p = basicPublisher();
        TestSubscriber s1 = new TestSubscriber();
        TestSubscriber s2 = new TestSubscriber();
        p.subscribe(s1);
        p.subscribe(s2);
        p.submit(1);
        p.close();
        assertTrue(p.isClosed());
        assertNull(p.getClosedException());
        s1.awaitComplete();
        assertEquals(1, s1.nexts);
        assertEquals(1, s1.completes);
        s2.awaitComplete();
        assertEquals(1, s2.nexts);
        assertEquals(1, s2.completes);
    }

    /**
     * Closing a publisher exceptionally causes onError to subscribers
     * after they are subscribed
     */
    public void testCloseExceptionallyError() {
        SubmissionPublisher<Integer> p = basicPublisher();
        TestSubscriber s1 = new TestSubscriber();
        TestSubscriber s2 = new TestSubscriber();
        p.subscribe(s1);
        p.subscribe(s2);
        p.submit(1);
        p.closeExceptionally(new SPException());
        assertTrue(p.isClosed());
        s1.awaitSubscribe();
        s1.awaitError();
        assertTrue(s1.nexts <= 1);
        assertEquals(1, s1.errors);
        s2.awaitSubscribe();
        s2.awaitError();
        assertTrue(s2.nexts <= 1);
        assertEquals(1, s2.errors);
    }

    /**
     * Cancelling a subscription eventually causes no more onNexts to be issued
     */
    public void testCancel() {
        SubmissionPublisher<Integer> p =
            new SubmissionPublisher<>(basicExecutor, 4); // must be < 20
        TestSubscriber s1 = new TestSubscriber();
        TestSubscriber s2 = new TestSubscriber();
        p.subscribe(s1);
        p.subscribe(s2);
        s1.awaitSubscribe();
        p.submit(1);
        s1.sn.cancel();
        for (int i = 2; i <= 20; ++i)
            p.submit(i);
        p.close();
        s2.awaitComplete();
        assertEquals(20, s2.nexts);
        assertEquals(1, s2.completes);
        assertTrue(s1.nexts < 20);
        assertFalse(p.isSubscribed(s1));
    }

    /**
     * Throwing an exception in onNext causes onError
     */
    public void testThrowOnNext() {
        SubmissionPublisher<Integer> p = basicPublisher();
        TestSubscriber s1 = new TestSubscriber();
        TestSubscriber s2 = new TestSubscriber();
        p.subscribe(s1);
        p.subscribe(s2);
        s1.awaitSubscribe();
        p.submit(1);
        s1.throwOnCall = true;
        p.submit(2);
        p.close();
        s2.awaitComplete();
        assertEquals(2, s2.nexts);
        s1.awaitComplete();
        assertEquals(1, s1.errors);
    }

    /**
     * If a handler is supplied in constructor, it is invoked when
     * subscriber throws an exception in onNext
     */
    public void testThrowOnNextHandler() {
        AtomicInteger calls = new AtomicInteger();
        SubmissionPublisher<Integer> p = new SubmissionPublisher<>(
            basicExecutor, 8, (s, e) -> calls.getAndIncrement());
        TestSubscriber s1 = new TestSubscriber();
        TestSubscriber s2 = new TestSubscriber();
        p.subscribe(s1);
        p.subscribe(s2);
        s1.awaitSubscribe();
        p.submit(1);
        s1.throwOnCall = true;
        p.submit(2);
        p.close();
        s2.awaitComplete();
        assertEquals(2, s2.nexts);
        assertEquals(1, s2.completes);
        s1.awaitError();
        assertEquals(1, s1.errors);
        assertEquals(1, calls.get());
    }

    /**
     * onNext items are issued in the same order to each subscriber
     */
    public void testOrder() {
        SubmissionPublisher<Integer> p = basicPublisher();
        TestSubscriber s1 = new TestSubscriber();
        TestSubscriber s2 = new TestSubscriber();
        p.subscribe(s1);
        p.subscribe(s2);
        for (int i = 1; i <= 20; ++i)
            p.submit(i);
        p.close();
        s2.awaitComplete();
        s1.awaitComplete();
        assertEquals(20, s2.nexts);
        assertEquals(1, s2.completes);
        assertEquals(20, s1.nexts);
        assertEquals(1, s1.completes);
    }

    /**
     * onNext is issued only if requested
     */
    public void testRequest1() {
        SubmissionPublisher<Integer> p = basicPublisher();
        TestSubscriber s1 = new TestSubscriber();
        s1.request = false;
        p.subscribe(s1);
        s1.awaitSubscribe();
        assertEquals(0, p.estimateMinimumDemand());
        TestSubscriber s2 = new TestSubscriber();
        p.subscribe(s2);
        p.submit(1);
        p.submit(2);
        s2.awaitNext(1);
        assertEquals(0, s1.nexts);
        s1.sn.request(3);
        p.submit(3);
        p.close();
        s2.awaitComplete();
        assertEquals(3, s2.nexts);
        assertEquals(1, s2.completes);
        s1.awaitComplete();
        assertTrue(s1.nexts > 0);
        assertEquals(1, s1.completes);
    }

    /**
     * onNext is not issued when requests become zero
     */
    public void testRequest2() {
        SubmissionPublisher<Integer> p = basicPublisher();
        TestSubscriber s1 = new TestSubscriber();
        TestSubscriber s2 = new TestSubscriber();
        p.subscribe(s1);
        p.subscribe(s2);
        s2.awaitSubscribe();
        s1.awaitSubscribe();
        s1.request = false;
        p.submit(1);
        p.submit(2);
        p.close();
        s2.awaitComplete();
        assertEquals(2, s2.nexts);
        assertEquals(1, s2.completes);
        s1.awaitNext(1);
        assertEquals(1, s1.nexts);
    }

    /**
     * Non-positive request causes error
     */
    public void testRequest3() {
        SubmissionPublisher<Integer> p = basicPublisher();
        TestSubscriber s1 = new TestSubscriber();
        TestSubscriber s2 = new TestSubscriber();
        TestSubscriber s3 = new TestSubscriber();
        p.subscribe(s1);
        p.subscribe(s2);
        p.subscribe(s3);
        s3.awaitSubscribe();
        s2.awaitSubscribe();
        s1.awaitSubscribe();
        s1.sn.request(-1L);
        s3.sn.request(0L);
        p.submit(1);
        p.submit(2);
        p.close();
        s2.awaitComplete();
        assertEquals(2, s2.nexts);
        assertEquals(1, s2.completes);
        s1.awaitError();
        assertEquals(1, s1.errors);
        assertTrue(s1.lastError instanceof IllegalArgumentException);
        s3.awaitError();
        assertEquals(1, s3.errors);
        assertTrue(s3.lastError instanceof IllegalArgumentException);
    }

    /**
     * estimateMinimumDemand reports 0 until request, nonzero after
     * request
     */
    public void testEstimateMinimumDemand() {
        TestSubscriber s = new TestSubscriber();
        SubmissionPublisher<Integer> p = basicPublisher();
        s.request = false;
        p.subscribe(s);
        s.awaitSubscribe();
        assertEquals(0, p.estimateMinimumDemand());
        s.sn.request(1);
        assertEquals(1, p.estimateMinimumDemand());
    }

    /**
     * submit to a publisher with no subscribers returns lag 0
     */
    public void testEmptySubmit() {
        SubmissionPublisher<Integer> p = basicPublisher();
        assertEquals(0, p.submit(1));
    }

    /**
     * submit(null) throws NPE
     */
    public void testNullSubmit() {
        SubmissionPublisher<Integer> p = basicPublisher();
        try {
            p.submit(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * submit returns number of lagged items, compatible with result
     * of estimateMaximumLag.
     */
    public void testLaggedSubmit() {
        SubmissionPublisher<Integer> p = basicPublisher();
        TestSubscriber s1 = new TestSubscriber();
        s1.request = false;
        TestSubscriber s2 = new TestSubscriber();
        s2.request = false;
        p.subscribe(s1);
        p.subscribe(s2);
        s2.awaitSubscribe();
        s1.awaitSubscribe();
        assertEquals(1, p.submit(1));
        assertTrue(p.estimateMaximumLag() >= 1);
        assertTrue(p.submit(2) >= 2);
        assertTrue(p.estimateMaximumLag() >= 2);
        s1.sn.request(4);
        assertTrue(p.submit(3) >= 3);
        assertTrue(p.estimateMaximumLag() >= 3);
        s2.sn.request(4);
        p.submit(4);
        p.close();
        s2.awaitComplete();
        assertEquals(4, s2.nexts);
        s1.awaitComplete();
        assertEquals(4, s2.nexts);
    }

    /**
     * submit eventually issues requested items when buffer capacity is 1
     */
    public void testCap1Submit() {
        SubmissionPublisher<Integer> p
            = new SubmissionPublisher<>(basicExecutor, 1);
        TestSubscriber s1 = new TestSubscriber();
        TestSubscriber s2 = new TestSubscriber();
        p.subscribe(s1);
        p.subscribe(s2);
        for (int i = 1; i <= 20; ++i) {
            assertTrue(p.submit(i) >= 0);
        }
        p.close();
        s2.awaitComplete();
        s1.awaitComplete();
        assertEquals(20, s2.nexts);
        assertEquals(1, s2.completes);
        assertEquals(20, s1.nexts);
        assertEquals(1, s1.completes);
    }

    static boolean noopHandle(AtomicInteger count) {
        count.getAndIncrement();
        return false;
    }

    static boolean reqHandle(AtomicInteger count, Subscriber<?> s) {
        count.getAndIncrement();
        ((TestSubscriber)s).sn.request(Long.MAX_VALUE);
        return true;
    }

    /**
     * offer to a publisher with no subscribers returns lag 0
     */
    public void testEmptyOffer() {
        SubmissionPublisher<Integer> p = basicPublisher();
        assertEquals(0, p.offer(1, null));
    }

    /**
     * offer(null) throws NPE
     */
    public void testNullOffer() {
        SubmissionPublisher<Integer> p = basicPublisher();
        try {
            p.offer(null, null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * offer returns number of lagged items if not saturated
     */
    public void testLaggedOffer() {
        SubmissionPublisher<Integer> p = basicPublisher();
        TestSubscriber s1 = new TestSubscriber();
        s1.request = false;
        TestSubscriber s2 = new TestSubscriber();
        s2.request = false;
        p.subscribe(s1);
        p.subscribe(s2);
        s2.awaitSubscribe();
        s1.awaitSubscribe();
        assertTrue(p.offer(1, null) >= 1);
        assertTrue(p.offer(2, null) >= 2);
        s1.sn.request(4);
        assertTrue(p.offer(3, null) >= 3);
        s2.sn.request(4);
        p.offer(4, null);
        p.close();
        s2.awaitComplete();
        assertEquals(4, s2.nexts);
        s1.awaitComplete();
        assertEquals(4, s2.nexts);
    }

    /**
     * offer reports drops if saturated
     */
    public void testDroppedOffer() {
        SubmissionPublisher<Integer> p
            = new SubmissionPublisher<>(basicExecutor, 4);
        TestSubscriber s1 = new TestSubscriber();
        s1.request = false;
        TestSubscriber s2 = new TestSubscriber();
        s2.request = false;
        p.subscribe(s1);
        p.subscribe(s2);
        s2.awaitSubscribe();
        s1.awaitSubscribe();
        for (int i = 1; i <= 4; ++i)
            assertTrue(p.offer(i, null) >= 0);
        p.offer(5, null);
        assertTrue(p.offer(6, null) < 0);
        s1.sn.request(64);
        assertTrue(p.offer(7, null) < 0);
        s2.sn.request(64);
        p.close();
        s2.awaitComplete();
        assertTrue(s2.nexts >= 4);
        s1.awaitComplete();
        assertTrue(s1.nexts >= 4);
    }

    /**
     * offer invokes drop handler if saturated
     */
    public void testHandledDroppedOffer() {
        AtomicInteger calls = new AtomicInteger();
        SubmissionPublisher<Integer> p
            = new SubmissionPublisher<>(basicExecutor, 4);
        TestSubscriber s1 = new TestSubscriber();
        s1.request = false;
        TestSubscriber s2 = new TestSubscriber();
        s2.request = false;
        p.subscribe(s1);
        p.subscribe(s2);
        s2.awaitSubscribe();
        s1.awaitSubscribe();
        for (int i = 1; i <= 4; ++i)
            assertTrue(p.offer(i, (s, x) -> noopHandle(calls)) >= 0);
        p.offer(4, (s, x) -> noopHandle(calls));
        assertTrue(p.offer(6, (s, x) -> noopHandle(calls)) < 0);
        s1.sn.request(64);
        assertTrue(p.offer(7, (s, x) -> noopHandle(calls)) < 0);
        s2.sn.request(64);
        p.close();
        s2.awaitComplete();
        s1.awaitComplete();
        assertTrue(calls.get() >= 4);
    }

    /**
     * offer succeeds if drop handler forces request
     */
    public void testRecoveredHandledDroppedOffer() {
        AtomicInteger calls = new AtomicInteger();
        SubmissionPublisher<Integer> p
            = new SubmissionPublisher<>(basicExecutor, 4);
        TestSubscriber s1 = new TestSubscriber();
        s1.request = false;
        TestSubscriber s2 = new TestSubscriber();
        s2.request = false;
        p.subscribe(s1);
        p.subscribe(s2);
        s2.awaitSubscribe();
        s1.awaitSubscribe();
        int n = 0;
        for (int i = 1; i <= 8; ++i) {
            int d = p.offer(i, (s, x) -> reqHandle(calls, s));
            n = n + 2 + (d < 0 ? d : 0);
        }
        p.close();
        s2.awaitComplete();
        s1.awaitComplete();
        assertEquals(n, s1.nexts + s2.nexts);
        assertTrue(calls.get() >= 2);
    }

    /**
     * Timed offer to a publisher with no subscribers returns lag 0
     */
    public void testEmptyTimedOffer() {
        SubmissionPublisher<Integer> p = basicPublisher();
        long startTime = System.nanoTime();
        assertEquals(0, p.offer(1, LONG_DELAY_MS, MILLISECONDS, null));
        assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS / 2);
    }

    /**
     * Timed offer with null item or TimeUnit throws NPE
     */
    public void testNullTimedOffer() {
        SubmissionPublisher<Integer> p = basicPublisher();
        long startTime = System.nanoTime();
        try {
            p.offer(null, LONG_DELAY_MS, MILLISECONDS, null);
            shouldThrow();
        } catch (NullPointerException success) {}
        try {
            p.offer(1, LONG_DELAY_MS, null, null);
            shouldThrow();
        } catch (NullPointerException success) {}
        assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS / 2);
    }

    /**
     * Timed offer returns number of lagged items if not saturated
     */
    public void testLaggedTimedOffer() {
        SubmissionPublisher<Integer> p = basicPublisher();
        TestSubscriber s1 = new TestSubscriber();
        s1.request = false;
        TestSubscriber s2 = new TestSubscriber();
        s2.request = false;
        p.subscribe(s1);
        p.subscribe(s2);
        s2.awaitSubscribe();
        s1.awaitSubscribe();
        long startTime = System.nanoTime();
        assertTrue(p.offer(1, LONG_DELAY_MS, MILLISECONDS, null) >= 1);
        assertTrue(p.offer(2, LONG_DELAY_MS, MILLISECONDS, null) >= 2);
        s1.sn.request(4);
        assertTrue(p.offer(3, LONG_DELAY_MS, MILLISECONDS, null) >= 3);
        s2.sn.request(4);
        p.offer(4, LONG_DELAY_MS, MILLISECONDS, null);
        p.close();
        s2.awaitComplete();
        assertEquals(4, s2.nexts);
        s1.awaitComplete();
        assertEquals(4, s2.nexts);
        assertTrue(millisElapsedSince(startTime) < LONG_DELAY_MS / 2);
    }

    /**
     * Timed offer reports drops if saturated
     */
    public void testDroppedTimedOffer() {
        SubmissionPublisher<Integer> p
            = new SubmissionPublisher<>(basicExecutor, 4);
        TestSubscriber s1 = new TestSubscriber();
        s1.request = false;
        TestSubscriber s2 = new TestSubscriber();
        s2.request = false;
        p.subscribe(s1);
        p.subscribe(s2);
        s2.awaitSubscribe();
        s1.awaitSubscribe();
        long delay = timeoutMillis();
        for (int i = 1; i <= 4; ++i)
            assertTrue(p.offer(i, delay, MILLISECONDS, null) >= 0);
        long startTime = System.nanoTime();
        assertTrue(p.offer(5, delay, MILLISECONDS, null) < 0);
        s1.sn.request(64);
        assertTrue(p.offer(6, delay, MILLISECONDS, null) < 0);
        // 2 * delay should elapse but check only 1 * delay to allow timer slop
        assertTrue(millisElapsedSince(startTime) >= delay);
        s2.sn.request(64);
        p.close();
        s2.awaitComplete();
        assertTrue(s2.nexts >= 2);
        s1.awaitComplete();
        assertTrue(s1.nexts >= 2);
    }

    /**
     * Timed offer invokes drop handler if saturated
     */
    public void testHandledDroppedTimedOffer() {
        AtomicInteger calls = new AtomicInteger();
        SubmissionPublisher<Integer> p
            = new SubmissionPublisher<>(basicExecutor, 4);
        TestSubscriber s1 = new TestSubscriber();
        s1.request = false;
        TestSubscriber s2 = new TestSubscriber();
        s2.request = false;
        p.subscribe(s1);
        p.subscribe(s2);
        s2.awaitSubscribe();
        s1.awaitSubscribe();
        long delay = timeoutMillis();
        for (int i = 1; i <= 4; ++i)
            assertTrue(p.offer(i, delay, MILLISECONDS, (s, x) -> noopHandle(calls)) >= 0);
        long startTime = System.nanoTime();
        assertTrue(p.offer(5, delay, MILLISECONDS, (s, x) -> noopHandle(calls)) < 0);
        s1.sn.request(64);
        assertTrue(p.offer(6, delay, MILLISECONDS, (s, x) -> noopHandle(calls)) < 0);
        assertTrue(millisElapsedSince(startTime) >= delay);
        s2.sn.request(64);
        p.close();
        s2.awaitComplete();
        s1.awaitComplete();
        assertTrue(calls.get() >= 2);
    }

    /**
     * Timed offer succeeds if drop handler forces request
     */
    public void testRecoveredHandledDroppedTimedOffer() {
        AtomicInteger calls = new AtomicInteger();
        SubmissionPublisher<Integer> p
            = new SubmissionPublisher<>(basicExecutor, 4);
        TestSubscriber s1 = new TestSubscriber();
        s1.request = false;
        TestSubscriber s2 = new TestSubscriber();
        s2.request = false;
        p.subscribe(s1);
        p.subscribe(s2);
        s2.awaitSubscribe();
        s1.awaitSubscribe();
        int n = 0;
        long delay = timeoutMillis();
        long startTime = System.nanoTime();
        for (int i = 1; i <= 6; ++i) {
            int d = p.offer(i, delay, MILLISECONDS, (s, x) -> reqHandle(calls, s));
            n = n + 2 + (d < 0 ? d : 0);
        }
        assertTrue(millisElapsedSince(startTime) >= delay);
        p.close();
        s2.awaitComplete();
        s1.awaitComplete();
        assertEquals(n, s1.nexts + s2.nexts);
        assertTrue(calls.get() >= 2);
    }

    /**
     * consume returns a CompletableFuture that is done when
     * publisher completes
     */
    public void testConsume() {
        AtomicInteger sum = new AtomicInteger();
        SubmissionPublisher<Integer> p = basicPublisher();
        CompletableFuture<Void> f =
            p.consume((Integer x) -> sum.getAndAdd(x.intValue()));
        int n = 20;
        for (int i = 1; i <= n; ++i)
            p.submit(i);
        p.close();
        f.join();
        assertEquals((n * (n + 1)) / 2, sum.get());
    }

    /**
     * consume(null) throws NPE
     */
    public void testConsumeNPE() {
        SubmissionPublisher<Integer> p = basicPublisher();
        try {
            CompletableFuture<Void> unused = p.consume(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

    /**
     * consume eventually stops processing published items if cancelled
     */
    public void testCancelledConsume() {
        AtomicInteger count = new AtomicInteger();
        SubmissionPublisher<Integer> p = basicPublisher();
        CompletableFuture<Void> f = p.consume(x -> count.getAndIncrement());
        f.cancel(true);
        int n = 1000000; // arbitrary limit
        for (int i = 1; i <= n; ++i)
            p.submit(i);
        assertTrue(count.get() < n);
    }

    /**
     * Tests scenario for
     * JDK-8187947: A race condition in SubmissionPublisher
     * cvs update -D '2017-11-25' src/main/java/util/concurrent/SubmissionPublisher.java && ant -Djsr166.expensiveTests=true -Djsr166.tckTestClass=SubmissionPublisherTest -Djsr166.methodFilter=testMissedSignal tck; cvs update -A src/main/java/util/concurrent/SubmissionPublisher.java
     */
    public void testMissedSignal_8187947() throws Exception {
        if (!atLeastJava9()) return; // backport to jdk8 too hard
        final int N =
            ((ForkJoinPool.getCommonPoolParallelism() < 2) // JDK-8212899
             ? (1 << 5)
             : (1 << 10))
            * (expensiveTests ? (1 << 10) : 1);
        final CountDownLatch finished = new CountDownLatch(1);
        final SubmissionPublisher<Boolean> pub = new SubmissionPublisher<>();
        class Sub implements Subscriber<Boolean> {
            int received;
            public void onSubscribe(Subscription s) {
                s.request(N);
            }
            public void onNext(Boolean item) {
                if (++received == N)
                    finished.countDown();
                else
                    CompletableFuture.runAsync(() -> pub.submit(Boolean.TRUE));
            }
            public void onError(Throwable t) { throw new AssertionError(t); }
            public void onComplete() {}
        }
        pub.subscribe(new Sub());
        checkTimedGet(
            CompletableFuture.runAsync(() -> pub.submit(Boolean.TRUE)),
            null);
        await(finished);
    }
}
