/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.event;

import java.time.Duration;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.test.lib.Asserts;

/**
 * @test
 * @summary Test enable/disable event and verify recording has expected events.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm -Xlog:jfr+event+setting=trace jdk.jfr.api.event.TestShouldCommit
 */

public class TestShouldCommit {

    public static void main(String[] args) throws Exception {
        Recording rA = new Recording();

        verifyShouldCommitFalse(); // No active recordings

        rA.start();
        rA.enable(MyEvent.class).withoutThreshold(); // recA=all
        verifyShouldCommitTrue();

        setThreshold(rA, 100); // recA=100
        verifyThreshold(100);

        setThreshold(rA, 200); // recA=200
        verifyThreshold(200);

        Recording rB = new Recording();
        verifyThreshold(200);  // recA=200, recB=not started

        rB.start();
        verifyThreshold(200);  // recA=200, recB=not specified, settings from recA is used.

        setThreshold(rB, 100); // recA=200, recB=100
        verifyThreshold(100);

        setThreshold(rB, 300); // recA=200, recB=300
        verifyThreshold(200);

        rA.disable(MyEvent.class); // recA=disabled, recB=300

        verifyThreshold(300);

        rB.disable(MyEvent.class); // recA=disabled, recB=disabled
        verifyShouldCommitFalse();

        setThreshold(rA, 200); // recA=200, recB=disabled
        verifyThreshold(200);

        rB.enable(MyEvent.class).withoutThreshold(); // recA=200, recB=all
        verifyShouldCommitTrue();

        setThreshold(rB, 100); // recA=200, recB=100
        verifyThreshold(100);

        rB.stop(); // recA=200, recB=stopped
        verifyThreshold(200);

        rA.stop(); // recA=stopped, recB=stopped
        verifyShouldCommitFalse();

        rA.close();
        rB.close();

        verifyShouldCommitFalse();
    }

    private static void setThreshold(Recording r, long millis) {
        r.enable(MyEvent.class).withThreshold(Duration.ofMillis(millis));
    }

    private static void verifyThreshold(long threshold) throws Exception {
        // Create 2 events, with different sleep time between begin() and end()
        // First event ends just before threshold, the other just after.
        verifyThreshold(threshold-5, threshold);
        verifyThreshold(threshold+5, threshold);
    }

    private static void verifyThreshold(long sleepMs, long thresholdMs) throws Exception {
        MyEvent event = new MyEvent();

        long beforeStartNanos = System.nanoTime();
        event.begin();
        long afterStartNanos = System.nanoTime();

        Thread.sleep(sleepMs);

        long beforeStopNanos = System.nanoTime();
        event.end();
        long afterStopNanos = System.nanoTime();

        boolean actualShouldCommit = event.shouldCommit();

        final long safetyMarginNanos = 2000000; // Allow an error of 2 ms. May have to be tuned.

        //Duration of event has been at least minDurationMicros
        long minDurationMicros = (beforeStopNanos - afterStartNanos - safetyMarginNanos) / 1000;
        //Duration of event has been at most maxDurationMicros
        long maxDurationMicros = (afterStopNanos - beforeStartNanos + safetyMarginNanos) / 1000;
        Asserts.assertLessThanOrEqual(minDurationMicros, maxDurationMicros, "Wrong min/max duration. Test error.");

        long thresholdMicros = thresholdMs * 1000;
        Boolean shouldCommit = null;
        if (minDurationMicros > thresholdMicros) {
            shouldCommit = new Boolean(true);  // shouldCommit() must be true
        } else if (maxDurationMicros < thresholdMicros) {
            shouldCommit = new Boolean(false); // shouldCommit() must be false
        } else {
            // Too close to call. No checks are done since we are not sure of expected shouldCommit().
        }

        System.out.printf(
            "threshold=%d, duration=[%d-%d], shouldCommit()=%b, expected=%s%n",
            thresholdMicros, minDurationMicros, maxDurationMicros, actualShouldCommit,
            (shouldCommit!=null ? shouldCommit : "too close to call"));

        try {
            if (shouldCommit != null) {
                Asserts.assertEquals(shouldCommit.booleanValue(), actualShouldCommit, "Wrong shouldCommit()");
            }
        } catch (Exception e) {
            System.out.println("Unexpected value of shouldCommit(). Searching for active threshold...");
            searchThreshold(thresholdMs, 2000+thresholdMs);
            throw e;
        }
    }

    // Sleeps until shouldCommit() is true, or give up. Used for logging.
    private static void searchThreshold(long expectedMs, long maxMs) throws Exception {
        long start = System.nanoTime();
        long stop = start + maxMs * 1000000;

        MyEvent event = new MyEvent();
        event.begin();
        event.end();

        while (!event.shouldCommit() && System.nanoTime() < stop) {
            Thread.sleep(1);
            event.end();
        }
        long durationMicros = (System.nanoTime() - start) / 1000;
        long expectedMicros = expectedMs * 1000;
        System.out.printf("shouldCommit()=%b after %,d ms, expected %,d%n", event.shouldCommit(), durationMicros, expectedMicros);
    }

    private static void verifyShouldCommitFalse() {
        MyEvent event = new MyEvent();
        event.begin();
        event.end();
        Asserts.assertFalse(event.shouldCommit(), "shouldCommit() expected false");
    }

    private static void verifyShouldCommitTrue() {
        MyEvent event = new MyEvent();
        event.begin();
        event.end();
        Asserts.assertTrue(event.shouldCommit(), "shouldCommit() expected true");
    }

    private static class MyEvent extends Event {
    }

}
