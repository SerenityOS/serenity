/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Objects;
import jdk.internal.misc.VM;

/**
 * @test
 * @bug 8068730
 * @summary tests that VM.getgetNanoTimeAdjustment() works as expected.
 * @modules java.base/jdk.internal.misc
 * @run main GetNanoTimeAdjustment
 * @author danielfuchs
 */
public class GetNanoTimeAdjustment {

    static final int MILLIS_IN_SECOND = 1000;
    static final int NANOS_IN_MILLI = 1000_000;
    static final int NANOS_IN_MICRO = 1000;
    static final int NANOS_IN_SECOND = 1000_000_000;

    static final boolean verbose = true;

    static final class TestAssertException extends RuntimeException {
        TestAssertException(String msg) { super(msg); }
    }

    private static void assertEquals(long expected, long received, String msg) {
        if (expected != received) {
            throw new TestAssertException("Unexpected result for " + msg
                    + ".\n\texpected: " + expected
                    +  "\n\tactual:   " + received);
        } else if (verbose) {
            System.out.println("Got expected " + msg + ": " + received);
        }
    }

    private static void assertEquals(Object expected, Object received, String msg) {
        if (!Objects.equals(expected, received)) {
            throw new TestAssertException("Unexpected result for " + msg
                    + ".\n\texpected: " + expected
                    +  "\n\tactual:   " + received);
        } else if (verbose) {
            System.out.println("Got expected " + msg + ": " + received);
        }
    }

    static final long MAX_OFFSET = 0x0100000000L;
    static final long MIN_OFFSET = -MAX_OFFSET;
    static enum Answer {
        YES,   // isOffLimit = YES:   we must get -1
        NO,    // isOffLimit = NO:    we must not not get -1
        MAYBE  // isOffLimit = MAYBE: we might get -1 or a valid adjustment.
    };
    static long distance(long one, long two) {
        return one > two ? Math.subtractExact(one, two)
                : Math.subtractExact(two, one);
    }


    static Answer isOffLimits(long before, long after, long offset) {
        long relativeDistanceBefore = distance(before, offset);
        long relativeDistanceAfter  = distance(after, offset);
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

    static void testWithOffset(String name, long offset) {
        System.out.println("Testing with offset: " + name);
        long beforeMillis = System.currentTimeMillis();
        long adjustment = VM.getNanoTimeAdjustment(offset);
        long afterMillis = System.currentTimeMillis();

        if (offset >= beforeMillis/MILLIS_IN_SECOND
                && offset <= afterMillis/MILLIS_IN_SECOND) {
            if (adjustment == -1) {
                // it's possible that we have fallen in the unlucky case
                // where -1 was the genuine result. let's go backward a bit.
                offset = offset - 10;
                beforeMillis = System.currentTimeMillis();
                adjustment = VM.getNanoTimeAdjustment(offset);
                afterMillis = System.currentTimeMillis();
                if (adjustment == -1) {
                    throw new RuntimeException(name + ": VM says " + offset
                            + " secs is too far off, "
                            + " when time in seconds is in ["
                            + beforeMillis/MILLIS_IN_SECOND + ", "
                            + afterMillis/MILLIS_IN_SECOND
                            + "]");
                }
            }
        }

        Answer isOffLimit = isOffLimits(beforeMillis/MILLIS_IN_SECOND,
                afterMillis/MILLIS_IN_SECOND, offset);
        switch (isOffLimit) {
            case YES:
                if (adjustment != -1) {
                    throw new RuntimeException(name
                        + ": VM should have returned -1 for "
                        + offset
                        + " when time in seconds is in ["
                        + beforeMillis/MILLIS_IN_SECOND + ", "
                        + afterMillis/MILLIS_IN_SECOND + "]");
                }
                System.out.println("Got expected exception value: " + adjustment);
                break;
            case NO:
                if (adjustment == -1) {
                    throw new RuntimeException(name
                            + "VM says "  + offset
                            + " secs is too far off, "
                            + " when time in seconds is in ["
                            + beforeMillis/MILLIS_IN_SECOND + ", "
                            + afterMillis/MILLIS_IN_SECOND
                            + "]");
                }
                break;
            case MAYBE:
                System.out.println("Adjustment: " + adjustment);
                System.out.println("Can't assert for -1 with offset "
                        + offset + "(" + name + ")"
                        + " when time in seconds is in ["
                        + beforeMillis/MILLIS_IN_SECOND + ", "
                        + afterMillis/MILLIS_IN_SECOND
                        + "]");
                // not conclusive
        }

        if (isOffLimit == Answer.NO || adjustment != -1) {
            System.out.println("Validating adjustment: " + adjustment);
            long expectedMax = distance(offset, beforeMillis/MILLIS_IN_SECOND)
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

    static void regular() {
        System.out.println("*** Testing regular cases ***");
        final long start = System.currentTimeMillis();
        long offset = start/1000;
        long adjustment = VM.getNanoTimeAdjustment(offset);
        if (start != offset*1000) {
            if (adjustment == -1) {
                throw new RuntimeException("VM says " + offset
                        + " secs is too far off, but time millis is "
                        + System.currentTimeMillis());
            }
        }
        if (adjustment == -1) {
            offset = System.currentTimeMillis()/1000 - 1024;
            adjustment = VM.getNanoTimeAdjustment(offset);
            if (adjustment == -1) {
                throw new RuntimeException("VM says " + offset
                        + " secs is too far off, but time millis is "
                        + System.currentTimeMillis());
            }
        }
        if (adjustment > (start/1000 - offset + 20)*NANOS_IN_SECOND) {
            throw new RuntimeException("Excessive adjustment: " + adjustment);
        }
        testWithOffset("System.currentTimeMillis()/1000",
                System.currentTimeMillis()/1000);
        testWithOffset("System.currentTimeMillis()/1000 - 1024",
                System.currentTimeMillis()/1000 - 1024);
        testWithOffset("System.currentTimeMillis()/1000 + 1024",
                System.currentTimeMillis()/1000 + 1024);
    }

    static void testLimits() {
        System.out.println("*** Testing limits ***");
        testWithOffset("System.currentTimeMillis()/1000 - MAX_OFFSET + 1",
                System.currentTimeMillis()/1000 - MAX_OFFSET + 1);
        testWithOffset("System.currentTimeMillis()/1000 + MAX_OFFSET - 1",
                System.currentTimeMillis()/1000 + MAX_OFFSET - 1);
        testWithOffset("System.currentTimeMillis()/1000 - MAX_OFFSET",
                System.currentTimeMillis()/1000 - MAX_OFFSET);
        testWithOffset("System.currentTimeMillis()/1000 + MAX_OFFSET",
                System.currentTimeMillis()/1000 + MAX_OFFSET);
        testWithOffset("System.currentTimeMillis()/1000 - MAX_OFFSET - 1024",
                System.currentTimeMillis()/1000 - MAX_OFFSET - 1024);
        testWithOffset("System.currentTimeMillis()/1000 + MAX_OFFSET + 1024",
                System.currentTimeMillis()/1000 + MAX_OFFSET + 1024);
        testWithOffset("0", 0);
        testWithOffset("-1", -1);
        testWithOffset("Integer.MAX_VALUE + System.currentTimeMillis()/1000",
                ((long)Integer.MAX_VALUE) + System.currentTimeMillis()/1000);
        testWithOffset("System.currentTimeMillis()/1000 - Integer.MIN_VALUE",
                System.currentTimeMillis()/1000 - Integer.MIN_VALUE);
        testWithOffset("Long.MAX_VALUE", Long.MAX_VALUE);
        testWithOffset("System.currentTimeMillis()/1000 - Long.MIN_VALUE",
                (Long.MIN_VALUE + System.currentTimeMillis()/1000)*-1);
    }

    public static void main(String[] args) throws Exception {
        regular();
        testLimits();
    }

}
