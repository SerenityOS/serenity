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

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TimeUnitTest extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        return new TestSuite(TimeUnitTest.class);
    }

    void testConversion(TimeUnit x, TimeUnit y, long n, long expected) {
        assertEquals(expected, x.convert(n, y));
        switch (x) {
        case NANOSECONDS:  assertEquals(expected, y.toNanos(n));   break;
        case MICROSECONDS: assertEquals(expected, y.toMicros(n));  break;
        case MILLISECONDS: assertEquals(expected, y.toMillis(n));  break;
        case SECONDS:      assertEquals(expected, y.toSeconds(n)); break;
        case MINUTES:      assertEquals(expected, y.toMinutes(n)); break;
        case HOURS:        assertEquals(expected, y.toHours(n));   break;
        case DAYS:         assertEquals(expected, y.toDays(n));    break;
        default: throw new AssertionError();
        }

        if (n > 0) testConversion(x, y, -n, -expected);
    }

    void testConversion(TimeUnit x, TimeUnit y) {
        long ratio = x.toNanos(1)/y.toNanos(1);
        assertTrue(ratio > 0);
        long[] ns = { 0, 1, 2, Long.MAX_VALUE/ratio, Long.MIN_VALUE/ratio };
        for (long n : ns) {
            testConversion(y, x, n, n * ratio);
            long[] ks = { n * ratio, n * ratio + 1, n * ratio - 1 };
            for (long k : ks) {
                testConversion(x, y, k, k / ratio);
            }
        }
    }

    /**
     * Conversion methods correctly convert sample values
     */
    public void testConversions() {
        // Sanity check
        assertEquals(1, NANOSECONDS.toNanos(1));
        assertEquals(1000L * NANOSECONDS.toNanos(1), MICROSECONDS.toNanos(1));
        assertEquals(1000L * MICROSECONDS.toNanos(1), MILLISECONDS.toNanos(1));
        assertEquals(1000L * MILLISECONDS.toNanos(1), SECONDS.toNanos(1));
        assertEquals(60L * SECONDS.toNanos(1), MINUTES.toNanos(1));
        assertEquals(60L * MINUTES.toNanos(1), HOURS.toNanos(1));
        assertEquals(24L * HOURS.toNanos(1), DAYS.toNanos(1));

        for (TimeUnit x : TimeUnit.values()) {
            assertEquals(x.toNanos(1), NANOSECONDS.convert(1, x));
        }

        for (TimeUnit x : TimeUnit.values())
            for (TimeUnit y : TimeUnit.values())
                if (x.toNanos(1) >= y.toNanos(1))
                    testConversion(x, y);
    }

    /**
     * convert saturates positive too-large values to Long.MAX_VALUE
     * and negative to LONG.MIN_VALUE
     */
    public void testConvertSaturate() {
        assertEquals(Long.MAX_VALUE,
                     NANOSECONDS.convert(Long.MAX_VALUE / 2, SECONDS));
        assertEquals(Long.MIN_VALUE,
                     NANOSECONDS.convert(-Long.MAX_VALUE / 4, SECONDS));
        assertEquals(Long.MAX_VALUE,
                     NANOSECONDS.convert(Long.MAX_VALUE / 2, MINUTES));
        assertEquals(Long.MIN_VALUE,
                     NANOSECONDS.convert(-Long.MAX_VALUE / 4, MINUTES));
        assertEquals(Long.MAX_VALUE,
                     NANOSECONDS.convert(Long.MAX_VALUE / 2, HOURS));
        assertEquals(Long.MIN_VALUE,
                     NANOSECONDS.convert(-Long.MAX_VALUE / 4, HOURS));
        assertEquals(Long.MAX_VALUE,
                     NANOSECONDS.convert(Long.MAX_VALUE / 2, DAYS));
        assertEquals(Long.MIN_VALUE,
                     NANOSECONDS.convert(-Long.MAX_VALUE / 4, DAYS));

        for (TimeUnit x : TimeUnit.values())
            for (TimeUnit y : TimeUnit.values()) {
                long ratio = x.toNanos(1) / y.toNanos(1);
                if (ratio >= 1) {
                    assertEquals(ratio, y.convert(1, x));
                    assertEquals(1, x.convert(ratio, y));
                    long max = Long.MAX_VALUE/ratio;
                    assertEquals(max * ratio, y.convert(max, x));
                    assertEquals(-max * ratio, y.convert(-max, x));
                    assertEquals(max, x.convert(max * ratio, y));
                    assertEquals(-max, x.convert(-max * ratio, y));
                    if (max < Long.MAX_VALUE) {
                        assertEquals(Long.MAX_VALUE, y.convert(max + 1, x));
                        assertEquals(Long.MIN_VALUE, y.convert(-max - 1, x));
                        assertEquals(Long.MIN_VALUE, y.convert(Long.MIN_VALUE + 1, x));
                    }
                    assertEquals(Long.MAX_VALUE, y.convert(Long.MAX_VALUE, x));
                    assertEquals(Long.MIN_VALUE, y.convert(Long.MIN_VALUE, x));
                }
            }
    }

    /**
     * toNanos saturates positive too-large values to Long.MAX_VALUE
     * and negative to LONG.MIN_VALUE
     */
    public void testToNanosSaturate() {
        assertEquals(Long.MAX_VALUE,
                     MILLISECONDS.toNanos(Long.MAX_VALUE / 2));
        assertEquals(Long.MIN_VALUE,
                     MILLISECONDS.toNanos(-Long.MAX_VALUE / 3));

        for (TimeUnit x : TimeUnit.values()) {
            long ratio = x.toNanos(1) / NANOSECONDS.toNanos(1);
            if (ratio >= 1) {
                long max = Long.MAX_VALUE/ratio;
                for (long z : new long[] {0, 1, -1, max, -max})
                    assertEquals(z * ratio, x.toNanos(z));
                if (max < Long.MAX_VALUE) {
                    assertEquals(Long.MAX_VALUE, x.toNanos(max + 1));
                    assertEquals(Long.MIN_VALUE, x.toNanos(-max - 1));
                    assertEquals(Long.MIN_VALUE, x.toNanos(Long.MIN_VALUE + 1));
                }
                assertEquals(Long.MAX_VALUE, x.toNanos(Long.MAX_VALUE));
                assertEquals(Long.MIN_VALUE, x.toNanos(Long.MIN_VALUE));
                if (max < Integer.MAX_VALUE) {
                    assertEquals(Long.MAX_VALUE, x.toNanos(Integer.MAX_VALUE));
                    assertEquals(Long.MIN_VALUE, x.toNanos(Integer.MIN_VALUE));
                }
            }
        }
    }

    /**
     * toMicros saturates positive too-large values to Long.MAX_VALUE
     * and negative to LONG.MIN_VALUE
     */
    public void testToMicrosSaturate() {
        for (TimeUnit x : TimeUnit.values()) {
            long ratio = x.toNanos(1) / MICROSECONDS.toNanos(1);
            if (ratio >= 1) {
                long max = Long.MAX_VALUE/ratio;
                for (long z : new long[] {0, 1, -1, max, -max})
                    assertEquals(z * ratio, x.toMicros(z));
                if (max < Long.MAX_VALUE) {
                    assertEquals(Long.MAX_VALUE, x.toMicros(max + 1));
                    assertEquals(Long.MIN_VALUE, x.toMicros(-max - 1));
                    assertEquals(Long.MIN_VALUE, x.toMicros(Long.MIN_VALUE + 1));
                }
                assertEquals(Long.MAX_VALUE, x.toMicros(Long.MAX_VALUE));
                assertEquals(Long.MIN_VALUE, x.toMicros(Long.MIN_VALUE));
                if (max < Integer.MAX_VALUE) {
                    assertEquals(Long.MAX_VALUE, x.toMicros(Integer.MAX_VALUE));
                    assertEquals(Long.MIN_VALUE, x.toMicros(Integer.MIN_VALUE));
                }
            }
        }
    }

    /**
     * toMillis saturates positive too-large values to Long.MAX_VALUE
     * and negative to LONG.MIN_VALUE
     */
    public void testToMillisSaturate() {
        for (TimeUnit x : TimeUnit.values()) {
            long ratio = x.toNanos(1) / MILLISECONDS.toNanos(1);
            if (ratio >= 1) {
                long max = Long.MAX_VALUE/ratio;
                for (long z : new long[] {0, 1, -1, max, -max})
                    assertEquals(z * ratio, x.toMillis(z));
                if (max < Long.MAX_VALUE) {
                    assertEquals(Long.MAX_VALUE, x.toMillis(max + 1));
                    assertEquals(Long.MIN_VALUE, x.toMillis(-max - 1));
                    assertEquals(Long.MIN_VALUE, x.toMillis(Long.MIN_VALUE + 1));
                }
                assertEquals(Long.MAX_VALUE, x.toMillis(Long.MAX_VALUE));
                assertEquals(Long.MIN_VALUE, x.toMillis(Long.MIN_VALUE));
                if (max < Integer.MAX_VALUE) {
                    assertEquals(Long.MAX_VALUE, x.toMillis(Integer.MAX_VALUE));
                    assertEquals(Long.MIN_VALUE, x.toMillis(Integer.MIN_VALUE));
                }
            }
        }
    }

    /**
     * toSeconds saturates positive too-large values to Long.MAX_VALUE
     * and negative to LONG.MIN_VALUE
     */
    public void testToSecondsSaturate() {
        for (TimeUnit x : TimeUnit.values()) {
            long ratio = x.toNanos(1) / SECONDS.toNanos(1);
            if (ratio >= 1) {
                long max = Long.MAX_VALUE/ratio;
                for (long z : new long[] {0, 1, -1, max, -max})
                    assertEquals(z * ratio, x.toSeconds(z));
                if (max < Long.MAX_VALUE) {
                    assertEquals(Long.MAX_VALUE, x.toSeconds(max + 1));
                    assertEquals(Long.MIN_VALUE, x.toSeconds(-max - 1));
                    assertEquals(Long.MIN_VALUE, x.toSeconds(Long.MIN_VALUE + 1));
                }
                assertEquals(Long.MAX_VALUE, x.toSeconds(Long.MAX_VALUE));
                assertEquals(Long.MIN_VALUE, x.toSeconds(Long.MIN_VALUE));
                if (max < Integer.MAX_VALUE) {
                    assertEquals(Long.MAX_VALUE, x.toSeconds(Integer.MAX_VALUE));
                    assertEquals(Long.MIN_VALUE, x.toSeconds(Integer.MIN_VALUE));
                }
            }
        }
    }

    /**
     * toMinutes saturates positive too-large values to Long.MAX_VALUE
     * and negative to LONG.MIN_VALUE
     */
    public void testToMinutesSaturate() {
        for (TimeUnit x : TimeUnit.values()) {
            long ratio = x.toNanos(1) / MINUTES.toNanos(1);
            if (ratio > 1) {
                long max = Long.MAX_VALUE/ratio;
                for (long z : new long[] {0, 1, -1, max, -max})
                    assertEquals(z * ratio, x.toMinutes(z));
                assertEquals(Long.MAX_VALUE, x.toMinutes(max + 1));
                assertEquals(Long.MIN_VALUE, x.toMinutes(-max - 1));
                assertEquals(Long.MAX_VALUE, x.toMinutes(Long.MAX_VALUE));
                assertEquals(Long.MIN_VALUE, x.toMinutes(Long.MIN_VALUE));
                assertEquals(Long.MIN_VALUE, x.toMinutes(Long.MIN_VALUE + 1));
            }
        }
    }

    /**
     * toHours saturates positive too-large values to Long.MAX_VALUE
     * and negative to LONG.MIN_VALUE
     */
    public void testToHoursSaturate() {
        for (TimeUnit x : TimeUnit.values()) {
            long ratio = x.toNanos(1) / HOURS.toNanos(1);
            if (ratio >= 1) {
                long max = Long.MAX_VALUE/ratio;
                for (long z : new long[] {0, 1, -1, max, -max})
                    assertEquals(z * ratio, x.toHours(z));
                if (max < Long.MAX_VALUE) {
                    assertEquals(Long.MAX_VALUE, x.toHours(max + 1));
                    assertEquals(Long.MIN_VALUE, x.toHours(-max - 1));
                    assertEquals(Long.MIN_VALUE, x.toHours(Long.MIN_VALUE + 1));
                }
                assertEquals(Long.MAX_VALUE, x.toHours(Long.MAX_VALUE));
                assertEquals(Long.MIN_VALUE, x.toHours(Long.MIN_VALUE));
            }
        }
    }

    /**
     * toString returns name of unit
     */
    public void testToString() {
        assertEquals("NANOSECONDS", NANOSECONDS.toString());
        assertEquals("MICROSECONDS", MICROSECONDS.toString());
        assertEquals("MILLISECONDS", MILLISECONDS.toString());
        assertEquals("SECONDS", SECONDS.toString());
        assertEquals("MINUTES", MINUTES.toString());
        assertEquals("HOURS", HOURS.toString());
        assertEquals("DAYS", DAYS.toString());
    }

    /**
     * name returns name of unit
     */
    public void testName() {
        for (TimeUnit x : TimeUnit.values())
            assertEquals(x.toString(), x.name());
    }

    /**
     * Timed wait without holding lock throws
     * IllegalMonitorStateException
     */
    public void testTimedWait_IllegalMonitorException() {
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                Object o = new Object();
                try {
                    MILLISECONDS.timedWait(o, LONGER_DELAY_MS);
                    threadShouldThrow();
                } catch (IllegalMonitorStateException success) {}
            }});

        awaitTermination(t);
    }

    /**
     * timedWait throws InterruptedException when interrupted
     */
    public void testTimedWait_Interruptible() {
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                Object o = new Object();

                Thread.currentThread().interrupt();
                try {
                    synchronized (o) {
                        MILLISECONDS.timedWait(o, LONGER_DELAY_MS);
                    }
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    synchronized (o) {
                        MILLISECONDS.timedWait(o, LONGER_DELAY_MS);
                    }
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(pleaseInterrupt);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.TIMED_WAITING);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * timedJoin throws InterruptedException when interrupted
     */
    public void testTimedJoin_Interruptible() {
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        final Thread s = newStartedThread(new CheckedInterruptedRunnable() {
            public void realRun() throws InterruptedException {
                Thread.sleep(LONGER_DELAY_MS);
            }});
        final Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                Thread.currentThread().interrupt();
                try {
                    MILLISECONDS.timedJoin(s, LONGER_DELAY_MS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    MILLISECONDS.timedJoin(s, LONGER_DELAY_MS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(pleaseInterrupt);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.TIMED_WAITING);
        t.interrupt();
        awaitTermination(t);
        s.interrupt();
        awaitTermination(s);
    }

    /**
     * timeUnit.sleep throws InterruptedException when interrupted
     */
    public void testTimedSleep_Interruptible() {
        final CountDownLatch pleaseInterrupt = new CountDownLatch(1);
        Thread t = newStartedThread(new CheckedRunnable() {
            public void realRun() throws InterruptedException {
                Thread.currentThread().interrupt();
                try {
                    MILLISECONDS.sleep(LONGER_DELAY_MS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());

                pleaseInterrupt.countDown();
                try {
                    MILLISECONDS.sleep(LONGER_DELAY_MS);
                    shouldThrow();
                } catch (InterruptedException success) {}
                assertFalse(Thread.interrupted());
            }});

        await(pleaseInterrupt);
        if (randomBoolean()) assertThreadBlocks(t, Thread.State.TIMED_WAITING);
        t.interrupt();
        awaitTermination(t);
    }

    /**
     * timeUnit.sleep(x) for x <= 0 does not sleep at all.
     */
    public void testTimedSleep_nonPositive() throws InterruptedException {
        boolean interrupt = randomBoolean();
        if (interrupt) Thread.currentThread().interrupt();
        randomTimeUnit().sleep(0L);
        randomTimeUnit().sleep(-1L);
        randomTimeUnit().sleep(Long.MIN_VALUE);
        if (interrupt) assertTrue(Thread.interrupted());
    }

    /**
     * a deserialized/reserialized unit is the same instance
     */
    public void testSerialization() throws Exception {
        for (TimeUnit x : TimeUnit.values())
            assertSame(x, serialClone(x));
    }

}
