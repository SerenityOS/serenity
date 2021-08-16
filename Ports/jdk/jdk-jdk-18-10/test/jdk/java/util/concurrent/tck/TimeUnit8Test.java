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

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TimeUnit8Test extends JSR166TestCase {
    public static void main(String[] args) {
        main(suite(), args);
    }

    public static Test suite() {
        return new TestSuite(TimeUnit8Test.class);
    }

    /**
     * tests for toChronoUnit.
     */
    public void testToChronoUnit() throws Exception {
        assertSame(ChronoUnit.NANOS,   NANOSECONDS.toChronoUnit());
        assertSame(ChronoUnit.MICROS,  MICROSECONDS.toChronoUnit());
        assertSame(ChronoUnit.MILLIS,  MILLISECONDS.toChronoUnit());
        assertSame(ChronoUnit.SECONDS, SECONDS.toChronoUnit());
        assertSame(ChronoUnit.MINUTES, MINUTES.toChronoUnit());
        assertSame(ChronoUnit.HOURS,   HOURS.toChronoUnit());
        assertSame(ChronoUnit.DAYS,    DAYS.toChronoUnit());

        // Every TimeUnit has a defined ChronoUnit equivalent
        for (TimeUnit x : TimeUnit.values())
            assertSame(x, TimeUnit.of(x.toChronoUnit()));
    }

    /**
     * tests for TimeUnit.of(ChronoUnit).
     */
    public void testTimeUnitOf() throws Exception {
        assertSame(NANOSECONDS,  TimeUnit.of(ChronoUnit.NANOS));
        assertSame(MICROSECONDS, TimeUnit.of(ChronoUnit.MICROS));
        assertSame(MILLISECONDS, TimeUnit.of(ChronoUnit.MILLIS));
        assertSame(SECONDS,      TimeUnit.of(ChronoUnit.SECONDS));
        assertSame(MINUTES,      TimeUnit.of(ChronoUnit.MINUTES));
        assertSame(HOURS,        TimeUnit.of(ChronoUnit.HOURS));
        assertSame(DAYS,         TimeUnit.of(ChronoUnit.DAYS));

        assertThrows(NullPointerException.class,
                     () -> TimeUnit.of((ChronoUnit)null));

        // ChronoUnits either round trip to their TimeUnit
        // equivalents, or throw IllegalArgumentException.
        for (ChronoUnit cu : ChronoUnit.values()) {
            final TimeUnit tu;
            try {
                tu = TimeUnit.of(cu);
            } catch (IllegalArgumentException acceptable) {
                continue;
            }
            assertSame(cu, tu.toChronoUnit());
        }
    }

    /**
     * convert(Duration) roundtrips with Duration.ofXXXX and Duration.of(long, ChronoUnit)
     */
    public void testConvertDuration_roundtripDurationOf() {
        long n = ThreadLocalRandom.current().nextLong();

        assertEquals(n, NANOSECONDS.convert(Duration.ofNanos(n)));
        assertEquals(n, NANOSECONDS.convert(Duration.of(n, ChronoUnit.NANOS)));
        assertEquals(n, MILLISECONDS.convert(Duration.ofMillis(n)));
        assertEquals(n, MILLISECONDS.convert(Duration.of(n, ChronoUnit.MILLIS)));
        assertEquals(n, SECONDS.convert(Duration.ofSeconds(n)));
        assertEquals(n, SECONDS.convert(Duration.of(n, ChronoUnit.SECONDS)));
        n /= 60;
        assertEquals(n, MINUTES.convert(Duration.ofMinutes(n)));
        assertEquals(n, MINUTES.convert(Duration.of(n, ChronoUnit.MINUTES)));
        n /= 60;
        assertEquals(n, HOURS.convert(Duration.ofHours(n)));
        assertEquals(n, HOURS.convert(Duration.of(n, ChronoUnit.HOURS)));
        n /= 24;
        assertEquals(n, DAYS.convert(Duration.ofDays(n)));
        assertEquals(n, DAYS.convert(Duration.of(n, ChronoUnit.DAYS)));
    }

    /**
     * convert(Duration.ofNanos(n)) agrees with convert(n, NANOSECONDS)
     */
    public void testConvertDuration_roundtripDurationOfNanos() {
        // Test values near unit transitions and near overflow.
        LongStream.concat(
                Arrays.stream(TimeUnit.values()).mapToLong(u -> u.toNanos(1)),
                LongStream.of(Long.MAX_VALUE, Long.MIN_VALUE))
            .flatMap(n -> LongStream.of(n, n + 1, n - 1))
            .flatMap(n -> LongStream.of(n, n + 1_000_000_000, n - 1_000_000_000))
            .flatMap(n -> LongStream.of(n, -n))
            // .peek(System.err::println)
            .forEach(n -> Arrays.stream(TimeUnit.values()).forEach(
                u -> assertEquals(u.convert(n, NANOSECONDS),
                                  u.convert(Duration.ofNanos(n)))));
    }

    /**
     * convert(Duration) doesn't misbehave near Long.MAX_VALUE and Long.MIN_VALUE.
     */
    public void testConvertDuration_nearOverflow() {
        ChronoUnit NANOS = ChronoUnit.NANOS;
        Duration maxDuration = Duration.ofSeconds(Long.MAX_VALUE, 999_999_999);
        Duration minDuration = Duration.ofSeconds(Long.MIN_VALUE, 0);

        for (TimeUnit u : TimeUnit.values()) {
            ChronoUnit cu = u.toChronoUnit();
            long r;
            if (u.toNanos(1) > SECONDS.toNanos(1)) {
                r = u.toNanos(1) / SECONDS.toNanos(1);

                assertThrows(ArithmeticException.class,
                             () -> Duration.of(Long.MAX_VALUE, cu),
                             () -> Duration.of(Long.MIN_VALUE, cu));
            } else {
                r = 1;

                Duration max = Duration.of(Long.MAX_VALUE, cu);
                Duration min = Duration.of(Long.MIN_VALUE, cu);
                assertEquals(Long.MAX_VALUE, u.convert(max));
                assertEquals(Long.MAX_VALUE - 1, u.convert(max.minus(1, NANOS)));
                assertEquals(Long.MAX_VALUE - 1, u.convert(max.minus(1, cu)));
                assertEquals(Long.MIN_VALUE, u.convert(min));
                assertEquals(Long.MIN_VALUE + 1, u.convert(min.plus(1, NANOS)));
                assertEquals(Long.MIN_VALUE + 1, u.convert(min.plus(1, cu)));
                assertEquals(Long.MAX_VALUE, u.convert(max.plus(1, NANOS)));
                if (u != SECONDS) {
                    assertEquals(Long.MAX_VALUE, u.convert(max.plus(1, cu)));
                    assertEquals(Long.MIN_VALUE, u.convert(min.minus(1, NANOS)));
                    assertEquals(Long.MIN_VALUE, u.convert(min.minus(1, cu)));
                }
            }

            assertEquals(Long.MAX_VALUE / r, u.convert(maxDuration));
            assertEquals(Long.MIN_VALUE / r, u.convert(minDuration));
        }
    }

}
