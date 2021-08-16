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

/*
 * @test
 * @summary Test drainTo failing due to c.add throwing
 * @library /test/lib
 */

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import jdk.test.lib.Utils;

@SuppressWarnings({"unchecked", "rawtypes"})
public class DrainToFails {
    static final long LONG_DELAY_MS = Utils.adjustTimeout(10_000);
    final int CAPACITY = 10;
    final int SMALL = 2;

    void test(String[] args) throws Throwable {
        testDelayQueue(new DelayQueue());
        testDelayQueue(new ScheduledThreadPoolExecutor(1).getQueue());

        testUnbounded(new LinkedBlockingQueue());
        testUnbounded(new LinkedBlockingDeque());
        testUnbounded(new PriorityBlockingQueue());

        testBounded(new LinkedBlockingQueue(CAPACITY));
        testBounded(new LinkedBlockingDeque(CAPACITY));
        testBounded(new ArrayBlockingQueue(CAPACITY));
    }

    static class PDelay
        extends FutureTask<Void>
        implements Delayed, RunnableScheduledFuture<Void> {
        int pseudodelay;
        PDelay(int i) {
            super(new Runnable() { public void run() {}}, null);
            pseudodelay = i;
        }
        public int compareTo(PDelay other) {
            return Integer.compare(this.pseudodelay, other.pseudodelay);
        }
        public int compareTo(Delayed y) {
            return compareTo((PDelay)y);
        }
        public boolean equals(Object other) {
            return (other instanceof PDelay) &&
                this.pseudodelay == ((PDelay)other).pseudodelay;
        }
        public long getDelay(TimeUnit ignore) {
            return Integer.MIN_VALUE + pseudodelay;
        }
        public String toString() {
            return String.valueOf(pseudodelay);
        }
        public boolean isPeriodic() { return false; }
    }

    void testDelayQueue(final BlockingQueue q) throws Throwable {
        System.err.println(q.getClass().getSimpleName());
        for (int i = 0; i < CAPACITY; i++)
            q.add(new PDelay(i));
        ArrayBlockingQueue q2 = new ArrayBlockingQueue(SMALL);
        try {
            q.drainTo(q2, SMALL + 3);
            fail("should throw");
        } catch (IllegalStateException success) {
            equal(SMALL, q2.size());
            equal(new PDelay(0), q2.poll());
            equal(new PDelay(1), q2.poll());
            check(q2.isEmpty());
            for (int i = SMALL; i < CAPACITY; i++)
                equal(new PDelay(i), q.poll());
            equal(0, q.size());
        }
    }

    void testUnbounded(final BlockingQueue q) throws Throwable {
        System.err.println(q.getClass().getSimpleName());
        for (int i = 0; i < CAPACITY; i++)
            q.add(i);
        ArrayBlockingQueue q2 = new ArrayBlockingQueue(SMALL);
        try {
            q.drainTo(q2, 7);
            fail("should throw");
        } catch (IllegalStateException success) {
            assertContentsInOrder(q2, 0, 1);
            q2.clear();
            equal(q.size(), CAPACITY - SMALL);
            equal(SMALL, q.peek());
        }

        try {
            q.drainTo(q2);
            fail("should throw");
        } catch (IllegalStateException success) {
            assertContentsInOrder(q2, 2, 3);
            equal(q.size(), CAPACITY - 2 * SMALL);
            for (int i = 2 * SMALL; i < CAPACITY; i++)
                equal(i, q.poll());
            equal(0, q.size());
        }
    }

    void testBounded(final BlockingQueue q) throws Throwable {
        System.err.println(q.getClass().getSimpleName());
        for (int i = 0; i < CAPACITY; i++)
            q.add(i);
        List<Thread> putters = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Thread putter = new Thread(putter(q, 42 + i));
            putters.add(putter);
            putter.setDaemon(true);
            putter.start();
        }
        ArrayBlockingQueue q2 = new ArrayBlockingQueue(SMALL);
        try {
            q.drainTo(q2, 7);
            fail("should throw");
        } catch (IllegalStateException success) {
            while (q.size() < CAPACITY)
                Thread.yield();
            assertContentsInOrder(q2, 0, 1);
            q2.clear();
        }

        try {
            q.drainTo(q2);
            fail("should throw");
        } catch (IllegalStateException success) {
            for (Thread putter : putters) {
                putter.join(LONG_DELAY_MS);
                check(! putter.isAlive());
            }
            assertContentsInOrder(q2, 2, 3);
            for (int i = 2 * SMALL; i < CAPACITY; i++)
                equal(i, q.poll());
            equal(4, q.size());
            check(q.contains(42));
            check(q.contains(43));
            check(q.contains(44));
            check(q.contains(45));
        }
    }

    Runnable putter(BlockingQueue q, int elt) {
        return () -> {
            try { q.put(elt); }
            catch (Throwable t) { unexpected(t); }};
    }

    void assertContentsInOrder(Iterable it, Object... contents) {
        int i = 0;
        for (Object e : it)
            equal(contents[i++], e);
        equal(contents.length, i);
    }

    //--------------------- Infrastructure ---------------------------
    volatile int passed = 0, failed = 0;
    void pass() {passed++;}
    void fail() {failed++; Thread.dumpStack();}
    void fail(String msg) {System.err.println(msg); fail();}
    void unexpected(Throwable t) {failed++; t.printStackTrace();}
    void check(boolean cond) {if (cond) pass(); else fail();}
    void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        new DrainToFails().instanceMain(args);}
    public void instanceMain(String[] args) throws Throwable {
        try {test(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
