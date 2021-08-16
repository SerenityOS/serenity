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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import jdk.jfr.Event;
import jdk.jfr.consumer.RecordingStream;

/**
 * @test
 * @summary Tests RecordingStream::start()
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @build jdk.jfr.api.consumer.recordingstream.EventProducer
 * @run main/othervm jdk.jfr.api.consumer.recordingstream.TestStart
 */
public class TestStart {
    static class StartEvent extends Event {
    }
    public static void main(String... args) throws Exception {
        testStart();
        testStartOnEvent();
        testStartTwice();
        testStartClosed();
    }

    private static void testStartTwice() throws Exception {
        log("Entering testStartTwice()");
        CountDownLatch started = new CountDownLatch(1);
        try (RecordingStream rs = new RecordingStream()) {
            EventProducer t = new EventProducer();
            t.start();
            Thread thread = new Thread() {
                public void run() {
                    rs.start();
                }
            };
            thread.start();
            rs.onEvent(e -> {
                if (started.getCount() > 0) {
                    started.countDown();
                }
            });
            started.await();
            t.kill();
            try {
                rs.start();
                throw new AssertionError("Expected IllegalStateException if started twice");
            } catch (IllegalStateException ise) {
                // OK, as expected
            }
        }
        log("Leaving testStartTwice()");
    }

    static void testStart() throws Exception {
        log("Entering testStart()");
        CountDownLatch started = new CountDownLatch(1);
        try (RecordingStream rs = new RecordingStream()) {
            rs.onEvent(e -> {
                started.countDown();
            });
            EventProducer t = new EventProducer();
            t.start();
            Thread thread = new Thread() {
                public void run() {
                    rs.start();
                }
            };
            thread.start();
            started.await();
            t.kill();
        }
        log("Leaving testStart()");
    }

    static void testStartOnEvent() throws Exception {
        log("Entering testStartOnEvent()");
        AtomicBoolean ISE = new AtomicBoolean(false);
        CountDownLatch startedTwice = new CountDownLatch(1);
        try (RecordingStream rs = new RecordingStream()) {
            rs.onEvent(e -> {
                try {
                    rs.start(); // must not deadlock
                } catch (IllegalStateException ise) {
                    if (!ISE.get())  {
                        ISE.set(true);
                        startedTwice.countDown();
                    }
                }
            });
            EventProducer t = new EventProducer();
            t.start();
            Thread thread = new Thread() {
                public void run() {
                    rs.start();
                }
            };
            thread.start();
            startedTwice.await();
            t.kill();
            if (!ISE.get()) {
                throw new AssertionError("Expected IllegalStateException");
            }
        }
        log("Leaving testStartOnEvent()");
    }

    static void testStartClosed() {
        log("Entering testStartClosed()");
        RecordingStream rs = new RecordingStream();
        rs.close();
        try {
            rs.start();
            throw new AssertionError("Expected IllegalStateException");
        } catch (IllegalStateException ise) {
            // OK, as expected.
        }
        log("Leaving testStartClosed()");
    }

    private static void log(String msg) {
        System.out.println(msg);
    }
}
