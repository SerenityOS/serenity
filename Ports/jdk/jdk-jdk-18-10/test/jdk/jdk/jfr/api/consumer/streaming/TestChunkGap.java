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

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.consumer.EventStream;

/**
 * @test
 * @summary Tests that a stream can gracefully handle chunk being removed in the
 *          middle
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.streaming.TestChunkGap
 */
public class TestChunkGap {

    static class StartEvent extends Event {
    }

    static class TestGapEvent extends Event {
    }

    static class EndEvent extends Event {
    }

    private final static AtomicInteger count = new AtomicInteger(0);

    public static void main(String... args) throws Exception {

        CountDownLatch gap = new CountDownLatch(1);
        CountDownLatch receivedEvent = new CountDownLatch(1);

        try (EventStream s = EventStream.openRepository()) {
            try (Recording r1 = new Recording()) {
                s.setStartTime(Instant.EPOCH);
                s.onEvent(e -> {
                    System.out.println(e);
                    receivedEvent.countDown();
                    try {
                        gap.await();
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    count.incrementAndGet();
                    if (e.getEventType().getName().equals(EndEvent.class.getName())) {
                        s.close();
                    }
                });
                s.startAsync();
                r1.start();
                StartEvent event1 = new StartEvent();
                event1.commit();
                receivedEvent.await();
                r1.stop();

                // create chunk that is removed
                try (Recording r2 = new Recording()) {
                    r2.enable(TestGapEvent.class);
                    r2.start();
                    TestGapEvent event2 = new TestGapEvent();
                    event2.commit();
                    r2.stop();
                }
                gap.countDown();
                try (Recording r3 = new Recording()) {
                    r3.enable(EndEvent.class);
                    r3.start();
                    EndEvent event3 = new EndEvent();
                    event3.commit();
                    r3.stop();

                    s.awaitTermination();
                    if (count.get() != 2) {
                        throw new AssertionError("Expected 2 event, but got " + count);
                    }
                }
            }
        }
    }

}
