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

import java.util.concurrent.CountDownLatch;

import jdk.jfr.Event;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Period;
import jdk.jfr.consumer.RecordingStream;

/**
 * @test
 * @summary Verifies that it is possible to stream contents of ongoing
 *          recordings
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm -Xlog:jfr+system+streaming=trace
 *      jdk.jfr.api.consumer.streaming.TestStartSingleChunk
 */
public class TestStartSingleChunk {

    @Period("500 ms")
    static class ElkEvent extends Event {
    }

    static class FrogEvent extends Event {
    }

    static class LionEvent extends Event {
    }

    public static void main(String... args) throws Exception {
        CountDownLatch frogLatch = new CountDownLatch(1);
        CountDownLatch lionLatch = new CountDownLatch(1);
        CountDownLatch elkLatch = new CountDownLatch(3);

        FlightRecorder.addPeriodicEvent(ElkEvent.class, () -> {
            ElkEvent ee = new ElkEvent();
            ee.commit();
        });
        try (RecordingStream s = new RecordingStream()) {
            s.onEvent(ElkEvent.class.getName(), e -> {
                System.out.println("Found elk!");
                elkLatch.countDown();
            });
            s.onEvent(LionEvent.class.getName(), e -> {
                System.out.println("Found lion!");
                lionLatch.countDown();
            });
            s.onEvent(FrogEvent.class.getName(), e -> {
                System.out.println("Found frog!");
                frogLatch.countDown();
            });
            s.startAsync();
            FrogEvent fe = new FrogEvent();
            fe.commit();

            LionEvent le = new LionEvent();
            le.commit();

            frogLatch.await();
            lionLatch.await();
            elkLatch.await();
        }
    }
}
