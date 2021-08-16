/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * Copyright (c) 2008-2012 Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package test.java.time;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test system clock.
 */
@Test
public class TestClock_System {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final Clock systemUTC = Clock.systemUTC();

    public void test_withZone_same() {
        Clock test = Clock.system(PARIS);
        Clock changed = test.withZone(PARIS);
        assertSame(test, changed);
    }

    //-----------------------------------------------------------------------
    public void test_toString() {
        Clock test = Clock.system(PARIS);
        assertEquals(test.toString(), "SystemClock[Europe/Paris]");
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="sampleSystemUTC")
    Object[][] provider_sampleSystemUTC() {
        return new Object[][] {
            {"Clock.systemUTC()#1",  Clock.systemUTC()},
            {"Clock.systemUTC()#2",  Clock.systemUTC()},
            {"Clock.system(ZoneOffset.UTC)#1",  Clock.system(ZoneOffset.UTC)},
            {"Clock.system(ZoneOffset.UTC)#2",  Clock.system(ZoneOffset.UTC)}
        };
    }

    // Test for 8073394
    @Test(dataProvider="sampleSystemUTC")
    public void test_systemUTC(String s, Clock clock) {
        if (clock != systemUTC) {
            throw new RuntimeException("Unexpected clock instance for " + s + ": "
                + "\n\texpected: " + toString(systemUTC)
                + "\n\tactual:   " + toString(clock));
        }
    }

    private static String toString(Clock c) {
        return c == null ? null :
               c + " " + c.getClass().getName() + "@" + System.identityHashCode(c);
    }

    //-----------------------------------------------------------------------

    private static String formatTime(String prefix, Instant time) {
        return prefix + ": " + time + " - seconds: "
                        + time.getEpochSecond() + ", nanos: "
                        + time.getNano();
    }

    public void test_ClockResolution() {
        Clock highestUTC = Clock.systemUTC();

        Instant start = Instant.ofEpochMilli(System.currentTimeMillis());

        try {
            // smoke test
            Instant system1 = Instant.ofEpochMilli(System.currentTimeMillis());
            Instant system2 = Instant.ofEpochMilli(System.currentTimeMillis());
            Instant highest1 = highestUTC.instant();
            Instant highest2 = highestUTC.instant();
            System.out.println(formatTime("\nsystemUTC #1            ", system1));
            System.out.println(formatTime("systemUTC #2            ", system2));
            System.out.println(formatTime("highestResolutionUTC #1 ", highest1));
            System.out.println(formatTime("highestResolutionUTC #2 ", highest2));

            if (system2.isBefore(system1)) {
                System.err.println("system2 is before system1!");
                System.err.println(formatTime("\n\tsystem1", system1));
                System.err.println(formatTime("\n\tsystem2", system2));
                throw new RuntimeException("system2 is before system1!"
                        + formatTime("\n\tsystem1", system1)
                        + formatTime("\n\tsystem2", system2));
            }
            if (highest2.isBefore(highest1)) {
                System.err.println("highest2 is before highest1!");
                System.err.println(formatTime("\n\thighest1", system1));
                System.err.println(formatTime("\n\tsystem2", highest2));
                throw new RuntimeException("highest2 is before system1!"
                        + formatTime("\n\thighest1", system1)
                        + formatTime("\n\tsystem2", highest2));
            }

            // better test - but depends on implementation details.
            // we're not rounding - so highest1 should be greater or equal to
            // system1
            system1 = Instant.ofEpochMilli(System.currentTimeMillis());
            highest1 = highestUTC.instant();

            System.out.println(formatTime("\nsystemUTC            ", system1));
            System.out.println(formatTime("highestResolutionUTC ", highest1));

            if (highest1.isBefore(system1)) {
                System.err.println("highest1 is before system1!");
                System.err.println(formatTime("\n\tsystem1", system1));
                System.err.println(formatTime("\n\thighest1", highest1));
                throw new RuntimeException("highest1 is before system1!"
                        + formatTime("\n\tsystem1", system1)
                        + formatTime("\n\thighest1", highest1));
            }

            int countBetterThanMillisPrecision = 0;
            int countBetterThanMicrosPrecision = 0;
            // let's preheat the system a bit:
            int lastNanos = 0;
            for (int i = 0; i < 1000 ; i++) {
                system1 = Instant.ofEpochMilli(System.currentTimeMillis());
                final int sysnan = system1.getNano();
                int nanos;
                do {
                    highest1 = highestUTC.instant();
                    nanos = highest1.getNano();
                } while (nanos == lastNanos); // Repeat to get a different value
                lastNanos = nanos;

                if ((nanos % 1000000) > 0) {
                    countBetterThanMillisPrecision++; // we have microseconds
                }
                if ((nanos % 1000) > 0) {
                    countBetterThanMicrosPrecision++; // we have nanoseconds
                }
                if ((sysnan % 1000000) > 0) {
                    throw new RuntimeException("Expected only millisecconds "
                            + "precision for systemUTC, found "
                            + (sysnan % 1000000) + " remainder.");
                }
            }
            System.out.println("\nNumber of time stamps which had better than"
                               + " millisecond precision: "
                               + countBetterThanMillisPrecision + "/" + 1000);
            System.out.println("\nNumber of time stamps which had better than"
                               + " microsecond precision: "
                               + countBetterThanMicrosPrecision + "/" + 1000);
            System.out.println(formatTime("\nsystemUTC            ", system1));
            System.out.println(formatTime("highestResolutionUTC ", highest1));
            if (countBetterThanMillisPrecision == 0) {
                System.err.println("Something is strange: no microsecond "
                                   + "precision with highestResolutionUTC?");
                throw new RuntimeException("Micro second precision not reached");
            }

            // check again
            if (highest1.isBefore(system1)) {
                System.err.println("highest1 is before system1!");
                System.err.println(formatTime("\n\tsystem1", system1));
                System.err.println(formatTime("\n\thighest1", highest1));
                throw new RuntimeException("highest1 is before system1!"
                        + formatTime("\n\tsystem1", system1)
                        + formatTime("\n\thighest1", highest1));
            }

            // leap of faith: ensure that highest1 is from within 10 secs of
            //   system1
            if (highest1.toEpochMilli() != system1.toEpochMilli()) {
                long delta = highest1.getEpochSecond() - system1.getEpochSecond();
                if (delta > 10) {
                    throw new RuntimeException("Unexpected long delay between two clocks ("
                            + delta + " seconds)"
                            + formatTime("\n\t system1", system1)
                            + formatTime("\n\t highest1", highest1));

                }
            } else {
                System.out.println("You won the lottery: the two dates are within 1 millisecond!\n");
            }

        } finally {
            Instant stop = Instant.ofEpochMilli(System.currentTimeMillis());
            if (start.isAfter(stop)) {
                // This should not happen - but can (un)probably be observed
                // when switching to summer time, or if another application
                // is switching the system date...
                System.err.println("Cannot test - date was setback: "
                        + formatTime("\n\tstarted at", start)
                        + formatTime("\n\tstopped at", stop) + "\n");
                return; // will prevent exceptions from being propagated.
            }
        }
    }

    static final long MAX_OFFSET = 0x0100000000L;
    static final long MIN_OFFSET = -MAX_OFFSET;

    // A helper class to test that SystemClock correctly recomputes
    // its offset.
    static class SystemClockOffset {

        static final int MILLIS_IN_SECOND = 1000;
        static final int NANOS_IN_MILLI = 1000_000;
        static final int NANOS_IN_MICRO = 1000;
        static final int NANOS_IN_SECOND = 1000_000_000;

        static final boolean verbose = true;
        static final Clock systemUTC = Clock.systemUTC();
        static final Field offsetField;

        static {
            try {
                offsetField = Class.forName("java.time.Clock").getDeclaredField("offset");
                offsetField.setAccessible(true);
            } catch (ClassNotFoundException | NoSuchFieldException ex) {
                throw new ExceptionInInitializerError(ex);
            }
        }

        static enum Answer {

            YES, // isOffLimit = YES:   we must get -1
            NO, // isOffLimit = NO:    we must not not get -1
            MAYBE  // isOffLimit = MAYBE: we might get -1 or a valid adjustment.
        };

        static long distance(long one, long two) {
            return one > two ? Math.subtractExact(one, two)
                    : Math.subtractExact(two, one);
        }

        static Answer isOffLimits(long before, long after, long offset) {
            long relativeDistanceBefore = distance(before, offset);
            long relativeDistanceAfter = distance(after, offset);
            if (relativeDistanceBefore >= MAX_OFFSET && relativeDistanceAfter >= MAX_OFFSET) {
                return Answer.YES;
            }
            if (relativeDistanceBefore < MAX_OFFSET && relativeDistanceAfter < MAX_OFFSET) {
                if (relativeDistanceBefore == 0 || relativeDistanceAfter == 0) {
                    return Answer.MAYBE; // unlucky case where
                }
                return Answer.NO;
            }
            return Answer.MAYBE;
        }

        static void testWithOffset(String name, long offset)
                throws IllegalAccessException {
            testWithOffset(name, offset, systemUTC);
        }

        static void testWithOffset(String name, long offset, Clock clock)
                throws IllegalAccessException {
            offsetField.set(null, offset);
            long beforeMillis = System.currentTimeMillis();
            final Instant instant = clock.instant();
            long afterMillis = System.currentTimeMillis();
            long actualOffset = offsetField.getLong(null);
            long instantMillis = instant.getEpochSecond() * MILLIS_IN_SECOND
                    + instant.getNano() / NANOS_IN_MILLI;
            if (instantMillis < beforeMillis || instantMillis > afterMillis) {
                throw new RuntimeException(name
                        + ": Invalid instant: " + instant
                        + " (~" + instantMillis + "ms)"
                        + " when time in millis is in ["
                        + beforeMillis + ", " + afterMillis
                        + "] and offset in seconds is " + offset);
            }
            Answer isOffLimits = isOffLimits(beforeMillis / MILLIS_IN_SECOND,
                    afterMillis / MILLIS_IN_SECOND, offset);
            switch (isOffLimits) {
                case YES:
                    if (actualOffset == offset) {
                        throw new RuntimeException(name
                                + ": offset was offlimit but was not recomputed "
                                + " when time in millis is in ["
                                + beforeMillis + ", " + afterMillis
                                + "] and offset in seconds was " + offset);
                    }
                    break;
                case NO:
                    if (actualOffset != offset) {
                        throw new RuntimeException(name
                                + ": offset was not offlimit but was recomputed.");
                    }
                    break;
                default:
                    break;
            }
            if (distance(actualOffset, instant.getEpochSecond()) >= MAX_OFFSET) {
                throw new RuntimeException(name + ": Actual offset is too far off:"
                        + " offset=" + actualOffset
                        + "instant.seconds=" + instant.getEpochSecond());
            }
            long adjustment = (instant.getEpochSecond() - actualOffset) * NANOS_IN_SECOND
                    + instant.getNano();
            validateAdjustment(name, actualOffset, beforeMillis, afterMillis, adjustment);
        }

        static void validateAdjustment(String name, long offset, long beforeMillis,
                long afterMillis, long adjustment) {
            System.out.println("Validating adjustment: " + adjustment);
            long expectedMax = distance(offset, beforeMillis / MILLIS_IN_SECOND)
                    * NANOS_IN_SECOND
                    + (beforeMillis % MILLIS_IN_SECOND) * NANOS_IN_MILLI
                    + (afterMillis - beforeMillis + 1) * NANOS_IN_MILLI;
            long absoluteAdjustment = distance(0, adjustment);
            if (absoluteAdjustment > expectedMax) {
                long adjSec = absoluteAdjustment / NANOS_IN_SECOND;
                long adjMil = (absoluteAdjustment % NANOS_IN_SECOND) / NANOS_IN_MILLI;
                long adjMic = (absoluteAdjustment % NANOS_IN_MILLI) / NANOS_IN_MICRO;
                long adjNan = (absoluteAdjustment % NANOS_IN_MICRO);
                long expSec = expectedMax / NANOS_IN_SECOND;
                long expMil = (expectedMax % NANOS_IN_SECOND) / NANOS_IN_MILLI;
                long expMic = (expectedMax % NANOS_IN_MILLI) / NANOS_IN_MICRO;
                long expNan = (expectedMax % NANOS_IN_MICRO);
                System.err.println("Excessive adjustment: " + adjSec + "s, "
                        + adjMil + "ms, " + adjMic + "mics, " + adjNan + "ns");
                System.err.println("Epected max: " + expSec + "s, "
                        + expMil + "ms, " + expMic + "mics, " + expNan + "ns");

                throw new RuntimeException(name
                        + ": Excessive adjustment: " + adjustment
                        + " when time in millis is in ["
                        + beforeMillis + ", " + afterMillis
                        + "] and offset in seconds is " + offset);
            }
        }
    }

    public void test_OffsetRegular() throws IllegalAccessException {
        System.out.println("*** Testing regular cases ***");
        SystemClockOffset.testWithOffset("System.currentTimeMillis()/1000",
                System.currentTimeMillis()/1000);
        SystemClockOffset.testWithOffset("System.currentTimeMillis()/1000 - 1024",
                System.currentTimeMillis()/1000 - 1024);
        SystemClockOffset.testWithOffset("System.currentTimeMillis()/1000 + 1024",
                System.currentTimeMillis()/1000 + 1024);
    }

    public void test_OffsetLimits() throws IllegalAccessException {
        System.out.println("*** Testing limits ***");
        SystemClockOffset.testWithOffset("System.currentTimeMillis()/1000 - MAX_OFFSET + 1",
                System.currentTimeMillis()/1000 - MAX_OFFSET + 1);
        SystemClockOffset.testWithOffset("System.currentTimeMillis()/1000 + MAX_OFFSET - 1",
                System.currentTimeMillis()/1000 + MAX_OFFSET - 1);
        SystemClockOffset.testWithOffset("System.currentTimeMillis()/1000 - MAX_OFFSET",
                System.currentTimeMillis()/1000 - MAX_OFFSET);
        SystemClockOffset.testWithOffset("System.currentTimeMillis()/1000 + MAX_OFFSET",
                System.currentTimeMillis()/1000 + MAX_OFFSET);
        SystemClockOffset.testWithOffset("System.currentTimeMillis()/1000 - MAX_OFFSET - 1024",
                System.currentTimeMillis()/1000 - MAX_OFFSET - 1024);
        SystemClockOffset.testWithOffset("System.currentTimeMillis()/1000 + MAX_OFFSET + 1024",
                System.currentTimeMillis()/1000 + MAX_OFFSET + 1024);
        SystemClockOffset.testWithOffset("0", 0);
        SystemClockOffset.testWithOffset("-1", -1);
        SystemClockOffset.testWithOffset("Integer.MAX_VALUE + System.currentTimeMillis()/1000",
                ((long)Integer.MAX_VALUE) + System.currentTimeMillis()/1000);
        SystemClockOffset.testWithOffset("System.currentTimeMillis()/1000 - Integer.MIN_VALUE",
                System.currentTimeMillis()/1000 - Integer.MIN_VALUE);
        SystemClockOffset.testWithOffset("Long.MAX_VALUE", Long.MAX_VALUE);
        SystemClockOffset.testWithOffset("System.currentTimeMillis()/1000 - Long.MIN_VALUE",
                (Long.MIN_VALUE + System.currentTimeMillis()/1000)*-1);
    }
}
