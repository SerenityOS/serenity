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

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAccumulator;

import junit.framework.Test;
import junit.framework.TestSuite;

public class LongAccumulatorTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(LongAccumulatorTest.class);
    }

    /**
     * new instance initialized to supplied identity
     */
    public void testConstructor() {
        for (long identity : new long[] { Long.MIN_VALUE, 0, Long.MAX_VALUE })
            assertEquals(identity,
                         new LongAccumulator(Long::max, identity).get());
    }

    /**
     * accumulate accumulates given value to current, and get returns current value
     */
    public void testAccumulateAndGet() {
        LongAccumulator acc = new LongAccumulator(Long::max, 0L);
        acc.accumulate(2);
        assertEquals(2, acc.get());
        acc.accumulate(-4);
        assertEquals(2, acc.get());
        acc.accumulate(4);
        assertEquals(4, acc.get());
    }

    /**
     * reset() causes subsequent get() to return zero
     */
    public void testReset() {
        LongAccumulator acc = new LongAccumulator(Long::max, 0L);
        acc.accumulate(2);
        assertEquals(2, acc.get());
        acc.reset();
        assertEquals(0, acc.get());
    }

    /**
     * getThenReset() returns current value; subsequent get() returns zero
     */
    public void testGetThenReset() {
        LongAccumulator acc = new LongAccumulator(Long::max, 0L);
        acc.accumulate(2);
        assertEquals(2, acc.get());
        assertEquals(2, acc.getThenReset());
        assertEquals(0, acc.get());
    }

    /**
     * toString returns current value.
     */
    public void testToString() {
        LongAccumulator acc = new LongAccumulator(Long::max, 0L);
        assertEquals("0", acc.toString());
        acc.accumulate(1);
        assertEquals(Long.toString(1), acc.toString());
    }

    /**
     * intValue returns current value.
     */
    public void testIntValue() {
        LongAccumulator acc = new LongAccumulator(Long::max, 0L);
        assertEquals(0, acc.intValue());
        acc.accumulate(1);
        assertEquals(1, acc.intValue());
    }

    /**
     * longValue returns current value.
     */
    public void testLongValue() {
        LongAccumulator acc = new LongAccumulator(Long::max, 0L);
        assertEquals(0, acc.longValue());
        acc.accumulate(1);
        assertEquals(1, acc.longValue());
    }

    /**
     * floatValue returns current value.
     */
    public void testFloatValue() {
        LongAccumulator acc = new LongAccumulator(Long::max, 0L);
        assertEquals(0.0f, acc.floatValue());
        acc.accumulate(1);
        assertEquals(1.0f, acc.floatValue());
    }

    /**
     * doubleValue returns current value.
     */
    public void testDoubleValue() {
        LongAccumulator acc = new LongAccumulator(Long::max, 0L);
        assertEquals(0.0, acc.doubleValue());
        acc.accumulate(1);
        assertEquals(1.0, acc.doubleValue());
    }

    /**
     * accumulates by multiple threads produce correct result
     */
    public void testAccumulateAndGetMT() {
        final LongAccumulator acc
            = new LongAccumulator((x, y) -> x + y, 0L);
        final int nThreads = ThreadLocalRandom.current().nextInt(1, 5);
        final Phaser phaser = new Phaser(nThreads + 1);
        final int incs = expensiveTests ? 1_000_000 : 100_000;
        final long total = nThreads * incs/2L * (incs - 1); // Gauss
        final Runnable task = () -> {
            phaser.arriveAndAwaitAdvance();
            for (int i = 0; i < incs; i++) {
                acc.accumulate((long) i);
                assertTrue(acc.get() <= total);
            }
            phaser.arrive();
        };
        final ExecutorService p = Executors.newCachedThreadPool();
        try (PoolCleaner cleaner = cleaner(p)) {
            for (int i = nThreads; i-->0; )
                p.execute(task);
            phaser.arriveAndAwaitAdvance();
            phaser.arriveAndAwaitAdvance();
            assertEquals(total, acc.get());
        }
    }

}
