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

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jdk.jfr.api.consumer.recordingstream.TestUtils.TestError;
import jdk.jfr.api.consumer.recordingstream.TestUtils.TestException;
import jdk.jfr.api.consumer.security.TestStreamingRemote.TestEvent;
import jdk.jfr.consumer.RecordingStream;

/**
 * @test
 * @summary Tests RecordingStream::onError(...) when using RecordingStream:start
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.api.consumer.recordingstream.TestOnErrorSync
 */
public class TestOnErrorSync {
    public static void main(String... args) throws Exception {
        testDefaultError();
        testCustomError();
        testDefaultException();
        testCustomException();
        testOnFlushSanity();
        testOnCloseSanity();
    }

    private static void testDefaultError() throws Exception {
        TestError error = new TestError();
        AtomicBoolean closed = new AtomicBoolean();
        Timer t = newEventEmitter();
        try (RecordingStream r = new RecordingStream()) {
            r.onEvent(e -> {
                throw error; // closes stream
            });
            r.onClose(() -> {
                closed.set(true);
            });
            try {
                r.start();
                throw new Exception("Expected TestError to be thrown");
            } catch (TestError te) {
                // as expected
            }
            if (!closed.get()) {
                throw new Exception("Expected stream to be closed");
            }
        } finally {
            t.cancel();
        }
    }

    private static void testCustomError() throws Exception {
        TestError error = new TestError();
        AtomicBoolean onError = new AtomicBoolean();
        AtomicBoolean closed = new AtomicBoolean();
        Timer t = newEventEmitter();
        try (RecordingStream r = new RecordingStream()) {
            r.onEvent(e -> {
                throw error; // closes stream
            });
            r.onError(e -> {
                onError.set(true);
            });
            r.onClose(() -> {
                closed.set(true);
            });
            try {
                r.start();
                throw new Exception("Expected TestError to be thrown");
            } catch (TestError terror) {
                // as expected
            }
            if (onError.get()) {
                throw new Exception("Expected onError(...) NOT to be invoked");
            }
            if (!closed.get()) {
                throw new Exception("Expected stream to be closed");
            }
        } finally {
            t.cancel();
        }
    }

    private static void testDefaultException() throws Exception {
        TestException exception = new TestException();
        AtomicInteger counter = new AtomicInteger();
        AtomicBoolean closed = new AtomicBoolean();
        Timer t = newEventEmitter();
        try (RecordingStream r = new RecordingStream()) {
            r.onEvent(e -> {
                if (counter.incrementAndGet() == 2) {
                    // Only close if we get a second event after an exception
                    r.close();
                    return;
                }
                TestUtils.throwUnchecked(exception);
            });
            r.onClose(() -> {
                closed.set(true);
            });
            try {
                r.start();
            } catch (Exception e) {
                throw new Exception("Unexpected exception thrown from start()", e);
            }
            if (!exception.isPrinted()) {
                throw new Exception("Expected stack trace from Exception to be printed");
            }
            if (!closed.get()) {
                throw new Exception("Expected stream to be closed");
            }
        } finally {
            t.cancel();
        }
    }

    private static void testCustomException() throws Exception {
        TestException exception = new TestException();
        AtomicInteger counter = new AtomicInteger();
        AtomicBoolean onError = new AtomicBoolean();
        AtomicBoolean closed = new AtomicBoolean();
        AtomicBoolean received = new AtomicBoolean();
        Timer t = newEventEmitter();
        try (RecordingStream r = new RecordingStream()) {
            r.onEvent(e -> {
                if (counter.incrementAndGet() == 2) {
                    // Only close if we get a second event after an exception
                    r.close();
                    return;
                }
                TestUtils.throwUnchecked(exception);
            });
            r.onError(e -> {
                received.set(e == exception);
                onError.set(true);
            });
            r.onClose(() -> {
                closed.set(true);
            });
            try {
                r.start();
            } catch (Exception e) {
                throw new Exception("Unexpected exception thrown from start()", e);
            }
            if (!received.get()) {
                throw new Exception("Did not receive expected exception in onError(...)");
            }
            if (exception.isPrinted()) {
                throw new Exception("Expected stack trace from Exception NOT to be printed");
            }
            if (!onError.get()) {
                throw new Exception("Expected OnError(...) to be invoked");
            }
            if (!closed.get()) {
                throw new Exception("Expected stream to be closed");
            }
        } finally {
            t.cancel();
        }
    }

    private static void testOnFlushSanity() throws Exception {
        TestException exception = new TestException();
        AtomicBoolean received = new AtomicBoolean();
        try (RecordingStream r = new RecordingStream()) {
            r.onFlush(() -> {
                TestUtils.throwUnchecked(exception);
            });
            r.onError(t -> {
                received.set(t == exception);
                r.close();
            });
            r.start();
            if (!received.get()) {
                throw new Exception("Expected exception in OnFlush to propagate to onError");
            }
        }
    }

    private static void testOnCloseSanity() throws Exception {
        TestException exception = new TestException();
        AtomicBoolean received = new AtomicBoolean();
        try (RecordingStream r = new RecordingStream()) {
            r.onFlush(() -> {
                r.close(); // will trigger onClose
            });
            r.onClose(() -> {
                TestUtils.throwUnchecked(exception); // will trigger onError
            });
            r.onError(t -> {
                received.set(t == exception);
            });
            r.start();
            if (!received.get()) {
                throw new Exception("Expected exception in OnFlush to propagate to onError");
            }
        }
    }

    private static Timer newEventEmitter() {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                TestEvent event = new TestEvent();
                event.commit();
            }
        };
        timer.schedule(task, 0, 100);
        return timer;
    }
}
