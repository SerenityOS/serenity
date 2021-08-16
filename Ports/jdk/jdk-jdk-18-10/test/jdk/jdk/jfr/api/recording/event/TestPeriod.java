/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.recording.event;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import jdk.jfr.Event;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Test periodic events.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.event.TestPeriod
 */
public class TestPeriod {

    static class Pulse extends Event {
    }

    public static void main(String[] args) throws Throwable {

        CountDownLatch latch = new CountDownLatch(3);
        FlightRecorder.addPeriodicEvent(Pulse.class, () -> {
           Pulse event = new Pulse();
           event.commit();
           latch.countDown();
        });

        try (Recording r = new Recording()) {
            r.enable(Pulse.class).withPeriod(Duration.ofMillis(500));
            r.start();
            latch.await();
            r.stop();
            List<RecordedEvent> events = Events.fromRecording(r);
            if (events.size() < 3) {
                System.out.println(events);
                throw new Exception("Expected at least 3 events");
            }
        }

    }

}
