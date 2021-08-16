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

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ThreadLocalRandom8Test extends JSR166TestCase {

    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(ThreadLocalRandom8Test.class);
    }

    // max sampled int bound
    static final int MAX_INT_BOUND = (1 << 26);

    // max sampled long bound
    static final long MAX_LONG_BOUND = (1L << 42);

    // Number of replications for other checks
    static final int REPS =
        Integer.getInteger("ThreadLocalRandom8Test.reps", 4);

    /**
     * Invoking sized ints, long, doubles, with negative sizes throws
     * IllegalArgumentException
     */
    public void testBadStreamSize() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        assertThrows(
            IllegalArgumentException.class,
            () -> r.ints(-1L),
            () -> r.ints(-1L, 2, 3),
            () -> r.longs(-1L),
            () -> r.longs(-1L, -1L, 1L),
            () -> r.doubles(-1L),
            () -> r.doubles(-1L, .5, .6));
    }

    /**
     * Invoking bounded ints, long, doubles, with illegal bounds throws
     * IllegalArgumentException
     */
    public void testBadStreamBounds() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        assertThrows(
            IllegalArgumentException.class,
            () -> r.ints(2, 1),
            () -> r.ints(10, 42, 42),
            () -> r.longs(-1L, -1L),
            () -> r.longs(10, 1L, -2L),
            () -> r.doubles(0.0, 0.0),
            () -> r.doubles(10, .5, .4));
    }

    /**
     * A parallel sized stream of ints generates the given number of values
     */
    public void testIntsCount() {
        LongAdder counter = new LongAdder();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long size = 0;
        for (int reps = 0; reps < REPS; ++reps) {
            counter.reset();
            r.ints(size).parallel().forEach(x -> counter.increment());
            assertEquals(size, counter.sum());
            size += 524959;
        }
    }

    /**
     * A parallel sized stream of longs generates the given number of values
     */
    public void testLongsCount() {
        LongAdder counter = new LongAdder();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long size = 0;
        for (int reps = 0; reps < REPS; ++reps) {
            counter.reset();
            r.longs(size).parallel().forEach(x -> counter.increment());
            assertEquals(size, counter.sum());
            size += 524959;
        }
    }

    /**
     * A parallel sized stream of doubles generates the given number of values
     */
    public void testDoublesCount() {
        LongAdder counter = new LongAdder();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long size = 0;
        for (int reps = 0; reps < REPS; ++reps) {
            counter.reset();
            r.doubles(size).parallel().forEach(x -> counter.increment());
            assertEquals(size, counter.sum());
            size += 524959;
        }
    }

    /**
     * Each of a parallel sized stream of bounded ints is within bounds
     */
    public void testBoundedInts() {
        AtomicInteger fails = new AtomicInteger(0);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long size = 12345L;
        for (int least = -15485867; least < MAX_INT_BOUND; least += 524959) {
            for (int bound = least + 2; bound > least && bound < MAX_INT_BOUND; bound += 67867967) {
                final int lo = least, hi = bound;
                r.ints(size, lo, hi).parallel().forEach(
                    x -> {
                        if (x < lo || x >= hi)
                            fails.getAndIncrement(); });
            }
        }
        assertEquals(0, fails.get());
    }

    /**
     * Each of a parallel sized stream of bounded longs is within bounds
     */
    public void testBoundedLongs() {
        AtomicInteger fails = new AtomicInteger(0);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long size = 123L;
        for (long least = -86028121; least < MAX_LONG_BOUND; least += 1982451653L) {
            for (long bound = least + 2; bound > least && bound < MAX_LONG_BOUND; bound += Math.abs(bound * 7919)) {
                final long lo = least, hi = bound;
                r.longs(size, lo, hi).parallel().forEach(
                    x -> {
                        if (x < lo || x >= hi)
                            fails.getAndIncrement(); });
            }
        }
        assertEquals(0, fails.get());
    }

    /**
     * Each of a parallel sized stream of bounded doubles is within bounds
     */
    public void testBoundedDoubles() {
        AtomicInteger fails = new AtomicInteger(0);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long size = 456;
        for (double least = 0.00011; least < 1.0e20; least *= 9) {
            for (double bound = least * 1.0011; bound < 1.0e20; bound *= 17) {
                final double lo = least, hi = bound;
                r.doubles(size, lo, hi).parallel().forEach(
                    x -> {
                        if (x < lo || x >= hi)
                            fails.getAndIncrement(); });
            }
        }
        assertEquals(0, fails.get());
    }

    /**
     * A parallel unsized stream of ints generates at least 100 values
     */
    public void testUnsizedIntsCount() {
        LongAdder counter = new LongAdder();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long size = 100;
        r.ints().limit(size).parallel().forEach(x -> counter.increment());
        assertEquals(size, counter.sum());
    }

    /**
     * A parallel unsized stream of longs generates at least 100 values
     */
    public void testUnsizedLongsCount() {
        LongAdder counter = new LongAdder();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long size = 100;
        r.longs().limit(size).parallel().forEach(x -> counter.increment());
        assertEquals(size, counter.sum());
    }

    /**
     * A parallel unsized stream of doubles generates at least 100 values
     */
    public void testUnsizedDoublesCount() {
        LongAdder counter = new LongAdder();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long size = 100;
        r.doubles().limit(size).parallel().forEach(x -> counter.increment());
        assertEquals(size, counter.sum());
    }

    /**
     * A sequential unsized stream of ints generates at least 100 values
     */
    public void testUnsizedIntsCountSeq() {
        LongAdder counter = new LongAdder();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long size = 100;
        r.ints().limit(size).forEach(x -> counter.increment());
        assertEquals(size, counter.sum());
    }

    /**
     * A sequential unsized stream of longs generates at least 100 values
     */
    public void testUnsizedLongsCountSeq() {
        LongAdder counter = new LongAdder();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long size = 100;
        r.longs().limit(size).forEach(x -> counter.increment());
        assertEquals(size, counter.sum());
    }

    /**
     * A sequential unsized stream of doubles generates at least 100 values
     */
    public void testUnsizedDoublesCountSeq() {
        LongAdder counter = new LongAdder();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        long size = 100;
        r.doubles().limit(size).forEach(x -> counter.increment());
        assertEquals(size, counter.sum());
    }

    /**
     * A deserialized/reserialized ThreadLocalRandom is always
     * identical to ThreadLocalRandom.current()
     */
    public void testSerialization() {
        assertSame(
            ThreadLocalRandom.current(),
            serialClone(ThreadLocalRandom.current()));
        // In the current implementation, there is exactly one shared instance
        if (testImplementationDetails)
            assertSame(
                ThreadLocalRandom.current(),
                java.util.concurrent.CompletableFuture.supplyAsync(
                    () -> serialClone(ThreadLocalRandom.current())).join());
    }

}
