/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.runtime;

import static jdk.test.lib.Asserts.assertTrue;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.thread.TestThread;
import jdk.test.lib.thread.XRun;

/**
 * @test TestJavaMonitorInflateEvent
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.event.runtime.TestJavaMonitorInflateEvent
 */
public class TestJavaMonitorInflateEvent {

    private static final String FIELD_KLASS_NAME = "monitorClass.name";
    private static final String FIELD_ADDRESS    = "address";
    private static final String FIELD_CAUSE      = "cause";

    private static final String EVENT_NAME = EventNames.JavaMonitorInflate;
    private static final long WAIT_TIME = 123456;

    static class Lock {
    }

    public static void main(String[] args) throws Exception {
        Recording recording = new Recording();
        recording.enable(EVENT_NAME).withThreshold(Duration.ofMillis(0));
        final Lock lock = new Lock();
        final CountDownLatch latch = new CountDownLatch(1);
        // create a thread that waits
        TestThread waitThread = new TestThread(new XRun() {
            @Override
            public void xrun() throws Throwable {
                synchronized (lock) {
                    latch.countDown();
                    lock.wait(WAIT_TIME);
                }
            }
        });
        try {
            recording.start();
            waitThread.start();
            latch.await();
            synchronized (lock) {
                lock.notifyAll();
            }
        } finally {
            waitThread.join();
            recording.stop();
        }
        final String thisThreadName = Thread.currentThread().getName();
        final String waitThreadName = waitThread.getName();
        final String lockClassName = lock.getClass().getName().replace('.', '/');
        boolean isAnyFound = false;
        try {
            // Find at least one event with the correct monitor class and check the other fields
            for (RecordedEvent event : Events.fromRecording(recording)) {
                assertTrue(EVENT_NAME.equals(event.getEventType().getName()), "mismatched event types?");
                // Check recorded inflation event is associated with the Lock class used in the test
                final String recordedMonitorClassName = Events.assertField(event, FIELD_KLASS_NAME).getValue();
                if (!lockClassName.equals(recordedMonitorClassName)) {
                    continue;
                }
                // Check recorded thread matches one of the threads in the test
                final String recordedThreadName = event.getThread().getJavaName();
                if (!(recordedThreadName.equals(waitThreadName) || recordedThreadName.equals(thisThreadName))) {
                    continue;
                }
                Events.assertField(event, FIELD_ADDRESS).notEqual(0L);
                Events.assertField(event, FIELD_CAUSE).notNull();
                isAnyFound = true;
                break;
            }
            assertTrue(isAnyFound, "Expected an inflation event from test");
        } catch (Throwable e) {
            recording.dump(Paths.get("failed.jfr"));
            throw e;
        } finally {
            recording.close();
        }
    }
}
