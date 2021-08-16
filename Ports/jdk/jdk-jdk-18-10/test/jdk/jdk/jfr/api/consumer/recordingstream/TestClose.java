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

package jdk.jfr.api.consumer.recordingstream;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.RecordingStream;

/**
 * @test
 * @summary Tests RecordingStream::close()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.api.consumer.recordingstream.TestClose
 */
public class TestClose {

    private static class CloseEvent extends Event {
    }

    public static void main(String... args) throws Exception {
        testCloseUnstarted();
        testCloseStarted();
        testCloseTwice();
        testCloseStreaming();
        testCloseMySelf();
        testCloseNoEvents();
    }

    private static void testCloseUnstarted() {
        System.out.println("testCloseUnstarted()");

        try (RecordingStream r = new RecordingStream()) {
            r.close();
        }
    }

    private static void testCloseStarted() {
        System.out.println("testCloseStarted()");

        try (RecordingStream r = new RecordingStream()) {
            r.startAsync();
        } // <- Close
    }

    private static void testCloseTwice() {
        System.out.println("Entering testCloseTwice()");

        try (RecordingStream r = new RecordingStream()) {
            r.startAsync();
            r.close();
        } // <- Second close
    }

    private static void testCloseStreaming() throws Exception {
        System.out.println("Entering testCloseStreaming()");

        EventProducer p = new EventProducer();
        p.start();
        CountDownLatch streaming = new CountDownLatch(1);
        try (RecordingStream r = new RecordingStream()) {
            r.onEvent(e -> {
                streaming.countDown();
            });
            r.startAsync();
            streaming.await();
        } // <- Close
        p.kill();
    }

    private static void testCloseMySelf() throws Exception {
        System.out.println("testCloseMySelf()");

        CountDownLatch closed = new CountDownLatch(1);
        try (RecordingStream r = new RecordingStream()) {
            r.onEvent(e -> {
                r.close();  // <- Close
                closed.countDown();
            });
            r.startAsync();
            CloseEvent c = new CloseEvent();
            c.commit();
            closed.await();
        }
    }

    private static void testCloseNoEvents() throws Exception {
        System.out.println("testCloseNoEvents()");

        try (Recording r = new Recording()) {
            r.start();
            CountDownLatch finished = new CountDownLatch(2);
            AtomicReference<Thread> streamingThread = new AtomicReference<>();
            try (EventStream es = EventStream.openRepository()) {
                es.setStartTime(Instant.EPOCH);
                es.onFlush(() -> {
                    streamingThread.set(Thread.currentThread());
                    finished.countDown();
                });
                es.startAsync();
                finished.await();
            } // <- Close should terminate thread
            while (streamingThread.get().isAlive()) {
                Thread.sleep(10);
            }
        }
    }
}
