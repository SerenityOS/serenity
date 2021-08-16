/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.consumer.streaming;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import jdk.jfr.Enabled;
import jdk.jfr.Event;
import jdk.jfr.FlightRecorder;
import jdk.jfr.consumer.RecordingStream;

/**
 * @test
 * @summary Verifies that it is possible to stream contents from specified event
 *          settings
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 *
 * @run main/othervm jdk.jfr.api.consumer.streaming.TestEnableEvents
 */
public class TestEnableEvents {

    @Enabled(false)
    static class HorseEvent extends Event {
    }

    @Enabled(false)
    static class ElephantEvent extends Event {
    }

    @Enabled(false)
    static class TigerEvent extends Event {
    }

    public static void main(String... args) throws Exception {
        CountDownLatch elephantLatch = new CountDownLatch(1);
        CountDownLatch tigerLatch = new CountDownLatch(1);
        CountDownLatch horseLatch = new CountDownLatch(1);

        FlightRecorder.addPeriodicEvent(ElephantEvent.class, () -> {
            HorseEvent ze = new HorseEvent();
            ze.commit();
        });

        try (RecordingStream s = new RecordingStream()) {
            s.enable(HorseEvent.class.getName()).withPeriod(Duration.ofMillis(50));
            s.enable(TigerEvent.class.getName());
            s.enable(ElephantEvent.class.getName());
            s.onEvent(TigerEvent.class.getName(), e -> {
                System.out.println("Event: " + e.getEventType().getName());
                System.out.println("Found tiger!");
                tigerLatch.countDown();
            });
            s.onEvent(HorseEvent.class.getName(), e -> {
                System.out.println("Found horse!");
                horseLatch.countDown();
            });
            s.onEvent(ElephantEvent.class.getName(), e -> {
                System.out.println("Found elelphant!");
                elephantLatch.countDown();
            });
            s.startAsync();
            TigerEvent te = new TigerEvent();
            te.commit();
            ElephantEvent ee = new ElephantEvent();
            ee.commit();
            elephantLatch.await();
            horseLatch.await();
            tigerLatch.await();
        }

    }

}
