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
import java.util.concurrent.atomic.AtomicInteger;

import jdk.jfr.Event;
import jdk.jfr.consumer.RecordingStream;

/**
 * @test
 * @summary Verifies that it is possible to filter a stream for an event
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.streaming.TestFiltering
 */
public class TestFiltering {

    static class SnakeEvent extends Event {
        int id;
    }

    static class EelEvent extends Event {
        int id;
    }

    public static void main(String... args) throws Exception {
        CountDownLatch l = new CountDownLatch(1);
        String eventName = SnakeEvent.class.getName();
        AtomicInteger idCounter = new AtomicInteger(-1);
        try (RecordingStream e = new RecordingStream()) {
            e.onEvent(eventName, event -> {
                if (!event.getEventType().getName().equals(eventName)) {
                    throw new InternalError("Unexpected event " + e);
                }
                if (event.getInt("id") != idCounter.incrementAndGet()) {
                    throw new InternalError("Incorrect id");
                }
                if (idCounter.get() == 99) {
                    l.countDown();
                }
            });
            e.startAsync();
            for (int i = 0; i < 100; i++) {
                SnakeEvent se = new SnakeEvent();
                se.id = i;
                se.commit();

                EelEvent ee = new EelEvent();
                ee.id = i;
                ee.commit();
            }
            l.await();
        }
    }
}
