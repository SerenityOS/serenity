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
import jdk.jfr.Registered;
import jdk.jfr.consumer.RecordingStream;

/**
 * @test
 * @summary Test that it is possible to register new metadata in a chunk
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.streaming.TestEventRegistration
 */
public class TestEventRegistration {
    @Registered(false)
    static class StreamEvent1 extends Event {
    }

    @Registered(false)
    static class StreamEvent2 extends Event {
    }

    public static void main(String... args) throws Exception {

        CountDownLatch s1Latch = new CountDownLatch(1);
        CountDownLatch s2Latch = new CountDownLatch(1);
        try (RecordingStream es = new RecordingStream()) {
            es.onEvent(StreamEvent1.class.getName(), e -> {
                s1Latch.countDown();
            });
            es.onEvent(StreamEvent2.class.getName(), e -> {
                s2Latch.countDown();
            });
            es.startAsync();
            System.out.println("Registering " + StreamEvent1.class.getName());
            FlightRecorder.register(StreamEvent1.class);
            StreamEvent1 s1 = new StreamEvent1();
            s1.commit();
            System.out.println(StreamEvent1.class.getName() + " commited");
            System.out.println("Awaiting latch for " + StreamEvent1.class.getName());
            s1Latch.await();
            System.out.println();
            System.out.println("Registering " + StreamEvent2.class.getName());
            FlightRecorder.register(StreamEvent2.class);
            StreamEvent2 s2 = new StreamEvent2();
            s2.commit();
            System.out.println(StreamEvent2.class.getName() + " commited");
            System.out.println("Awaiting latch for " + StreamEvent2.class.getName());
            s2Latch.await();
        }
    }
}
