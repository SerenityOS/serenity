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
import java.util.concurrent.atomic.AtomicInteger;

import jdk.jfr.api.consumer.recordingstream.TestUtils.TestError;
import jdk.jfr.api.consumer.recordingstream.TestUtils.TestException;
import jdk.jfr.api.consumer.security.TestStreamingRemote.TestEvent;
import jdk.jfr.consumer.RecordingStream;

/**
 * @test
 * @summary Tests RecordingStream::onError(...) when using
 *          RecordingStream:startAsync
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.api.consumer.recordingstream.TestOnErrorAsync
 */
public class TestOnErrorAsync {
    public static void main(String... args) throws Exception {
        testDefaultError();
        testCustomError();
        testDefaultException();
        testCustomException();
        testOnFlushSanity();
        testOnCloseSanity();
    }

    private static void testDefaultError() throws Exception {
        AtomicBoolean closed = new AtomicBoolean();
        CountDownLatch receivedError = new CountDownLatch(1);
        try (RecordingStream r = new RecordingStream()) {
            r.onEvent(e -> {
                TestError error = new TestError();
                TestUtils.installUncaughtException(receivedError, error);
                throw error; // closes stream
            });
            r.onClose(() -> {
                closed.set(true);
            });
            r.startAsync();
            TestEvent e = new TestEvent();
            e.commit();
            r.awaitTermination();
            receivedError.await();
            if (!closed.get()) {
                throw new Exception("Expected stream to be closed");
            }
        }
    }

    private static void testCustomError() throws Exception {
        AtomicBoolean onError = new AtomicBoolean();
        AtomicBoolean closed = new AtomicBoolean();
        CountDownLatch receivedError = new CountDownLatch(1);
        try (RecordingStream r = new RecordingStream()) {
            r.onEvent(e -> {
                TestError error = new TestError();
                TestUtils.installUncaughtException(receivedError, error);
                throw error; // closes stream
            });
            r.onError(e -> {
                onError.set(true);
            });
            r.onClose(() -> {
                closed.set(true);
            });
            r.startAsync();
            TestEvent e = new TestEvent();
            e.commit();
            r.awaitTermination();
            receivedError.await();
            if (onError.get()) {
                throw new Exception("onError handler should not be invoked on java.lang.Error.");
            }
            if (!closed.get()) {
                throw new Exception("Expected stream to be closed");
            }
        }
    }

    private static void testDefaultException() throws Exception {
        TestException exception = new TestException();
        AtomicBoolean closed = new AtomicBoolean();
        AtomicInteger counter = new AtomicInteger();
        try (RecordingStream r = new RecordingStream()) {
            r.onEvent(e -> {
                if (counter.incrementAndGet() == 2) {
                    r.close();
                    return;
                }
                TestUtils.throwUnchecked(exception);
            });
            r.onClose(() -> {
                closed.set(true);
            });
            r.startAsync();
            TestEvent e1 = new TestEvent();
            e1.commit();
            TestEvent e2 = new TestEvent();
            e2.commit();
            r.awaitTermination();
            if (!exception.isPrinted()) {
                throw new Exception("Expected stack trace from Exception to be printed");
            }
            if (!closed.get()) {
                throw new Exception("Expected stream to be closed");
            }
        }
    }

    private static void testCustomException() throws Exception {
        TestException exception = new TestException();
        AtomicBoolean closed = new AtomicBoolean();
        AtomicBoolean received = new AtomicBoolean();
        try (RecordingStream r = new RecordingStream()) {
            r.onEvent(e -> {
                TestUtils.throwUnchecked(exception);
            });
            r.onError(t -> {
                received.set(t == exception);
                r.close();
            });
            r.onClose(() -> {
                closed.set(true);
            });
            r.startAsync();
            TestEvent event = new TestEvent();
            event.commit();
            r.awaitTermination();
            if (!received.get()) {
                throw new Exception("Did not receive expected exception in onError(...)");
            }
            if (exception.isPrinted()) {
                throw new Exception("Expected stack trace from Exception NOT to be printed");
            }
            if (!closed.get()) {
                throw new Exception("Expected stream to be closed");
            }
        }
    }

    private static void testOnFlushSanity() throws Exception {
        TestException exception = new TestException();
        CountDownLatch received = new CountDownLatch(1);
        try (RecordingStream r = new RecordingStream()) {
            r.onFlush(() -> {
                TestUtils.throwUnchecked(exception);
            });
            r.onError(t -> {
                if (t == exception) {
                    received.countDown();
                }
            });
            r.startAsync();
            received.await();
       }
    }

    private static void testOnCloseSanity() throws Exception {
        TestException exception = new TestException();
        CountDownLatch received = new CountDownLatch(1);
        try (RecordingStream r = new RecordingStream()) {
            r.onFlush(() -> {
                r.close(); // will trigger onClose
            });
            r.onClose(() -> {
                TestUtils.throwUnchecked(exception); // will trigger onError
            });
            r.onError(t -> {
                if (t == exception) {
                    received.countDown();
                }
            });
            r.startAsync();
            received.await();
        }
    }
}
