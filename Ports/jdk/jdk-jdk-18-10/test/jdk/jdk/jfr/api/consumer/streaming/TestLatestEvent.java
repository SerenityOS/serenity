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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jdk.jfr.Event;
import jdk.jfr.FlightRecorder;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.RecordingStream;

/**
 * @test
 * @summary Verifies that EventStream::openRepository() read from the latest flush
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.streaming.TestLatestEvent
 */
public class TestLatestEvent {

    @Name("NotLatest")
    static class NotLatestEvent extends Event {

        public int id;
    }

    @Name("Latest")
    static class LatestEvent extends Event {
    }

    @Name("MakeChunks")
    static class MakeChunks extends Event {
    }

    public static void main(String... args) throws Exception {
        CountDownLatch notLatestEvent = new CountDownLatch(6);
        CountDownLatch beginChunks = new CountDownLatch(1);

        try (RecordingStream r = new RecordingStream()) {
            r.onEvent("MakeChunks", event-> {
                System.out.println(event);
                beginChunks.countDown();
            });
            r.onEvent("NotLatest", event -> {
                System.out.println(event);
                notLatestEvent.countDown();
            });
            r.startAsync();
            MakeChunks e = new MakeChunks();
            e.commit();

            System.out.println("Waiting for first chunk");
            beginChunks.await();
            // Create 5 chunks with events in the repository
            for (int i = 0; i < 5; i++) {
                System.out.println("Creating empty chunk");
                try (Recording r1 = new Recording()) {
                    r1.start();
                    NotLatestEvent notLatest = new NotLatestEvent();
                    notLatest.id = i;
                    notLatest.commit();
                    r1.stop();
                }
            }
            System.out.println("All empty chunks created");

            // Create event in new chunk
            NotLatestEvent notLatest = new NotLatestEvent();
            notLatest.id = 5;
            notLatest.commit();

            // This latch ensures thatNotLatest has been
            // flushed and a new valid position has been written
            // to the chunk header
            notLatestEvent.await(80, TimeUnit.SECONDS);
            if (notLatestEvent.getCount() != 0) {
               Recording rec =  FlightRecorder.getFlightRecorder().takeSnapshot();
               Path p = Paths.get("error-not-latest.jfr").toAbsolutePath();
               rec.dump(p);
               System.out.println("Dumping repository as a file for inspection at " + p);
               throw new Exception("Timeout 80 s. Expected 6 event, but got "  + notLatestEvent.getCount());
            }

            try (EventStream s = EventStream.openRepository()) {
                System.out.println("EventStream opened");
                AtomicBoolean foundLatest = new AtomicBoolean();
                s.onEvent(event -> {
                    String name = event.getEventType().getName();
                    System.out.println("Found event " + name);
                    foundLatest.set(name.equals("Latest"));
                });
                s.startAsync();
                // Must loop here as there is no guarantee
                // that the parser thread starts before event
                // is flushed
                while (!foundLatest.get()) {
                    LatestEvent latest = new LatestEvent();
                    latest.commit();
                    System.out.println("Latest event emitted. Waiting 1 s ...");
                    Thread.sleep(1000);
                }
            }
        }
    }
}
