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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Test;
import junit.framework.TestSuite;

public class ThreadLocalRandomTest extends JSR166TestCase {

    public static void main(String[] args) {
        main(suite(), args);
    }
    public static Test suite() {
        return new TestSuite(ThreadLocalRandomTest.class);
    }

    /*
     * Testing coverage notes:
     *
     * We don't test randomness properties, but only that repeated
     * calls, up to NCALLS tries, produce at least one different
     * result.  For bounded versions, we sample various intervals
     * across multiples of primes.
     */

    // max numbers of calls to detect getting stuck on one value
    static final int NCALLS = 10000;

    // max sampled int bound
    static final int MAX_INT_BOUND = (1 << 28);

    // max sampled long bound
    static final long MAX_LONG_BOUND = (1L << 42);

    // Number of replications for other checks
    static final int REPS = 20;

    /**
     * setSeed throws UnsupportedOperationException
     */
    public void testSetSeed() {
        try {
            ThreadLocalRandom.current().setSeed(17);
            shouldThrow();
        } catch (UnsupportedOperationException success) {}
    }

    /**
     * Repeated calls to next (only accessible via reflection) produce
     * at least two distinct results, and repeated calls produce all
     * possible values.
     */
    public void testNext() throws ReflectiveOperationException {
        // Inhibit "An illegal reflective access operation has occurred"
        if (!testImplementationDetails) return;

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        final java.lang.reflect.Method m;
        try {
            m = ThreadLocalRandom.class.getDeclaredMethod("next", int.class);
            m.setAccessible(true);
        } catch (SecurityException acceptable) {
            // Security manager may deny access
            return;
        } catch (Exception ex) {
            // jdk9 module system may deny access
            if (ex.getClass().getSimpleName()
                .equals("InaccessibleObjectException"))
                return;
            throw ex;
        }

        int i;
        {
            int val = new java.util.Random().nextInt(4);
            for (i = 0; i < NCALLS; i++) {
                int q = (int) m.invoke(rnd, new Object[] { 2 });
                if (val == q) break;
            }
            assertTrue(i < NCALLS);
        }

        {
            int r = (int) m.invoke(rnd, new Object[] { 3 });
            for (i = 0; i < NCALLS; i++) {
                int q = (int) m.invoke(rnd, new Object[] { 3 });
                assertTrue(q < (1<<3));
                if (r != q) break;
            }
            assertTrue(i < NCALLS);
        }
    }

    /**
     * Repeated calls to nextInt produce at least two distinct results
     */
    public void testNextInt() {
        int f = ThreadLocalRandom.current().nextInt();
        int i = 0;
        while (i < NCALLS && ThreadLocalRandom.current().nextInt() == f)
            ++i;
        assertTrue(i < NCALLS);
    }

    /**
     * Repeated calls to nextLong produce at least two distinct results
     */
    public void testNextLong() {
        long f = ThreadLocalRandom.current().nextLong();
        int i = 0;
        while (i < NCALLS && ThreadLocalRandom.current().nextLong() == f)
            ++i;
        assertTrue(i < NCALLS);
    }

    /**
     * Repeated calls to nextBoolean produce at least two distinct results
     */
    public void testNextBoolean() {
        boolean f = ThreadLocalRandom.current().nextBoolean();
        int i = 0;
        while (i < NCALLS && ThreadLocalRandom.current().nextBoolean() == f)
            ++i;
        assertTrue(i < NCALLS);
    }

    /**
     * Repeated calls to nextFloat produce at least two distinct results
     */
    public void testNextFloat() {
        float f = ThreadLocalRandom.current().nextFloat();
        int i = 0;
        while (i < NCALLS && ThreadLocalRandom.current().nextFloat() == f)
            ++i;
        assertTrue(i < NCALLS);
    }

    /**
     * Repeated calls to nextDouble produce at least two distinct results
     */
    public void testNextDouble() {
        double f = ThreadLocalRandom.current().nextDouble();
        int i = 0;
        while (i < NCALLS && ThreadLocalRandom.current().nextDouble() == f)
            ++i;
        assertTrue(i < NCALLS);
    }

    /**
     * Repeated calls to nextGaussian produce at least two distinct results
     */
    public void testNextGaussian() {
        double f = ThreadLocalRandom.current().nextGaussian();
        int i = 0;
        while (i < NCALLS && ThreadLocalRandom.current().nextGaussian() == f)
            ++i;
        assertTrue(i < NCALLS);
    }

    /**
     * nextInt(non-positive) throws IllegalArgumentException
     */
    public void testNextIntBoundNonPositive() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int bound : new int[] { 0, -17, Integer.MIN_VALUE }) {
            try {
                rnd.nextInt(bound);
                shouldThrow();
            } catch (IllegalArgumentException success) {}
        }
    }

    /**
     * nextInt(least >= bound) throws IllegalArgumentException
     */
    public void testNextIntBadBounds() {
        int[][] badBoundss = {
            { 17, 2 },
            { -42, -42 },
            { Integer.MAX_VALUE, Integer.MIN_VALUE },
        };
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int[] badBounds : badBoundss) {
            try {
                rnd.nextInt(badBounds[0], badBounds[1]);
                shouldThrow();
            } catch (IllegalArgumentException success) {}
        }
    }

    /**
     * nextInt(bound) returns 0 <= value < bound;
     * repeated calls produce at least two distinct results
     */
    public void testNextIntBounded() {
        // sample bound space across prime number increments
        for (int bound = 2; bound < MAX_INT_BOUND; bound += 524959) {
            int f = ThreadLocalRandom.current().nextInt(bound);
            assertTrue(0 <= f && f < bound);
            int i = 0;
            int j;
            while (i < NCALLS &&
                   (j = ThreadLocalRandom.current().nextInt(bound)) == f) {
                assertTrue(0 <= j && j < bound);
                ++i;
            }
            assertTrue(i < NCALLS);
        }
    }

    /**
     * nextInt(least, bound) returns least <= value < bound;
     * repeated calls produce at least two distinct results
     */
    public void testNextIntBounded2() {
        for (int least = -15485863; least < MAX_INT_BOUND; least += 524959) {
            for (int bound = least + 2; bound > least && bound < MAX_INT_BOUND; bound += 49979687) {
                int f = ThreadLocalRandom.current().nextInt(least, bound);
                assertTrue(least <= f && f < bound);
                int i = 0;
                int j;
                while (i < NCALLS &&
                       (j = ThreadLocalRandom.current().nextInt(least, bound)) == f) {
                    assertTrue(least <= j && j < bound);
                    ++i;
                }
                assertTrue(i < NCALLS);
            }
        }
    }

    /**
     * nextLong(non-positive) throws IllegalArgumentException
     */
    public void testNextLongBoundNonPositive() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (long bound : new long[] { 0L, -17L, Long.MIN_VALUE }) {
            try {
                rnd.nextLong(bound);
                shouldThrow();
            } catch (IllegalArgumentException success) {}
        }
    }

    /**
     * nextLong(least >= bound) throws IllegalArgumentException
     */
    public void testNextLongBadBounds() {
        long[][] badBoundss = {
            { 17L, 2L },
            { -42L, -42L },
            { Long.MAX_VALUE, Long.MIN_VALUE },
        };
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (long[] badBounds : badBoundss) {
            try {
                rnd.nextLong(badBounds[0], badBounds[1]);
                shouldThrow();
            } catch (IllegalArgumentException success) {}
        }
    }

    /**
     * nextLong(bound) returns 0 <= value < bound;
     * repeated calls produce at least two distinct results
     */
    public void testNextLongBounded() {
        for (long bound = 2; bound < MAX_LONG_BOUND; bound += 15485863) {
            long f = ThreadLocalRandom.current().nextLong(bound);
            assertTrue(0 <= f && f < bound);
            int i = 0;
            long j;
            while (i < NCALLS &&
                   (j = ThreadLocalRandom.current().nextLong(bound)) == f) {
                assertTrue(0 <= j && j < bound);
                ++i;
            }
            assertTrue(i < NCALLS);
        }
    }

    /**
     * nextLong(least, bound) returns least <= value < bound;
     * repeated calls produce at least two distinct results
     */
    public void testNextLongBounded2() {
        for (long least = -86028121; least < MAX_LONG_BOUND; least += 982451653L) {
            for (long bound = least + 2; bound > least && bound < MAX_LONG_BOUND; bound += Math.abs(bound * 7919)) {
                long f = ThreadLocalRandom.current().nextLong(least, bound);
                assertTrue(least <= f && f < bound);
                int i = 0;
                long j;
                while (i < NCALLS &&
                       (j = ThreadLocalRandom.current().nextLong(least, bound)) == f) {
                    assertTrue(least <= j && j < bound);
                    ++i;
                }
                assertTrue(i < NCALLS);
            }
        }
    }

    /**
     * nextDouble(non-positive) throws IllegalArgumentException
     */
    public void testNextDoubleBoundNonPositive() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double[] badBounds = {
            0.0d,
            -17.0d,
            -Double.MIN_VALUE,
            Double.NEGATIVE_INFINITY,
            Double.NaN,
        };
        for (double bound : badBounds) {
            try {
                rnd.nextDouble(bound);
                shouldThrow();
            } catch (IllegalArgumentException success) {}
        }
    }

    /**
     * nextDouble(least, bound) returns least <= value < bound;
     * repeated calls produce at least two distinct results
     */
    public void testNextDoubleBounded2() {
        for (double least = 0.0001; least < 1.0e20; least *= 8) {
            for (double bound = least * 1.001; bound < 1.0e20; bound *= 16) {
                double f = ThreadLocalRandom.current().nextDouble(least, bound);
                assertTrue(least <= f && f < bound);
                int i = 0;
                double j;
                while (i < NCALLS &&
                       (j = ThreadLocalRandom.current().nextDouble(least, bound)) == f) {
                    assertTrue(least <= j && j < bound);
                    ++i;
                }
                assertTrue(i < NCALLS);
            }
        }
    }

    /**
     * Different threads produce different pseudo-random sequences
     */
    public void testDifferentSequences() {
        // Don't use main thread's ThreadLocalRandom - it is likely to
        // be polluted by previous tests.
        final AtomicReference<ThreadLocalRandom> threadLocalRandom =
            new AtomicReference<>();
        final AtomicLong rand = new AtomicLong();

        Runnable getRandomState = new CheckedRunnable() {
            public void realRun() {
                ThreadLocalRandom current = ThreadLocalRandom.current();
                assertSame(current, ThreadLocalRandom.current());
                rand.set(current.nextLong());
                threadLocalRandom.set(current);
            }};

        awaitTermination(newStartedThread(getRandomState));
        long firstRand = rand.get();
        ThreadLocalRandom firstThreadLocalRandom = threadLocalRandom.get();
        assertNotNull(firstThreadLocalRandom);

        for (int i = 0; i < NCALLS; i++) {
            awaitTermination(newStartedThread(getRandomState));
            if (testImplementationDetails)
                // ThreadLocalRandom has been a singleton since jdk8.
                assertSame(firstThreadLocalRandom, threadLocalRandom.get());
            if (firstRand != rand.get())
                return;
        }
        fail("all threads generate the same pseudo-random sequence");
    }

    /**
     * Repeated calls to nextBytes produce at least values of different signs for every byte
     */
    public void testNextBytes() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int n = rnd.nextInt(1, 20);
        byte[] bytes = new byte[n];
        outer:
        for (int i = 0; i < n; i++) {
            for (int tries = NCALLS; tries-->0; ) {
                byte before = bytes[i];
                rnd.nextBytes(bytes);
                byte after = bytes[i];
                if (after * before < 0)
                    continue outer;
            }
            fail("not enough variation in random bytes");
        }
    }

    /**
     * Filling an empty array with random bytes succeeds without effect.
     */
    public void testNextBytes_emptyArray() {
        ThreadLocalRandom.current().nextBytes(new byte[0]);
    }

    public void testNextBytes_nullArray() {
        try {
            ThreadLocalRandom.current().nextBytes(null);
            shouldThrow();
        } catch (NullPointerException success) {}
    }

}
