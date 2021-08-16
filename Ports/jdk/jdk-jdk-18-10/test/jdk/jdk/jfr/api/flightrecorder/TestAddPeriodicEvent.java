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

package jdk.jfr.api.flightrecorder;

import static jdk.test.lib.Asserts.assertFalse;
import static jdk.test.lib.Asserts.assertTrue;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import jdk.jfr.Event;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;

/**
 * @test
 * @summary
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.flightrecorder.TestAddPeriodicEvent
 */
public class TestAddPeriodicEvent {

    private static class MyEvent extends Event {

    }

    CountDownLatch latch = new CountDownLatch(3);

    class MyHook implements Runnable {

        private int eventCounter;
        private long previousTime;

        @Override
        public void run() {
            log("Commiting event " + (++eventCounter));
            if (previousTime == 0) {
                previousTime = System.currentTimeMillis();
            } else {
                long nowTime = System.currentTimeMillis();
                long elapsedTime = nowTime - previousTime;
                previousTime = nowTime;
                log("Elapsed time since the previous event: " + elapsedTime);
            }

            commitEvent();
            latch.countDown();
        }

        private void commitEvent() {
            MyEvent event = new MyEvent();
            event.commit();
        }
    }

    public static void main(String[] args) throws Exception {
        new TestAddPeriodicEvent().doTest(1000, 3);
    }

    private void doTest(long eventDuration, int numOfEvents) throws Exception {
        latch = new CountDownLatch(numOfEvents);
        MyHook hook = new MyHook();

        Recording r = new Recording();
        r.enable(MyEvent.class).withPeriod(Duration.ofMillis(eventDuration));
        r.start();

        FlightRecorder.addPeriodicEvent(MyEvent.class, hook);

        latch.await();

        assertTrue(FlightRecorder.removePeriodicEvent(hook));
        assertFalse(FlightRecorder.removePeriodicEvent(hook));

        r.stop();
        r.close();
    }

    private static void log(String text) {
        System.out.println(text);
    }
}
