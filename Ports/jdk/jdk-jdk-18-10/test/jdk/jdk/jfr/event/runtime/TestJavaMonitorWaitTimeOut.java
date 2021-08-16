/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.event.runtime.TestJavaMonitorWaitTimeOut
 */
public class TestJavaMonitorWaitTimeOut {

    static final class Lock {

    }

    private static final String THREAD_NAME = "Notifier";

    static class Notifierhread extends Thread {
        private final Object lock;

        Notifierhread(Object lock) {
            super(THREAD_NAME);
            this.lock = lock;
        }

        public void run() {
            synchronized (lock) {
                lock.notify();
            }
        }
    }

    public static void main(String[] args) throws Throwable {
        try (Recording recording = new Recording()) {
            recording.enable(EventNames.JavaMonitorWait).withoutThreshold().withoutStackTrace();
            recording.start();
            Lock lock = new Lock();

            synchronized (lock) {
                // event without notifier, should be null
                lock.wait(10);
            }
            Notifierhread s = new Notifierhread(lock);

            synchronized (lock) {
                s.start();
                // event with a notifier
                lock.wait(1_000_000);
                s.join();
            }

            synchronized (lock) {
                // event without a notifier, should be null
                lock.wait(11);
            }
            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            for (RecordedEvent e : events) {
                if (isWaitEvent(e)) {
                    System.out.println(e);
                }
            }
            assertTimeOutEvent(events, 10, null);
            assertTimeOutEvent(events, 1_000_000, THREAD_NAME);
            assertTimeOutEvent(events, 11, null);
        }
    }

    private static boolean isWaitEvent(RecordedEvent event) {
        RecordedClass t = event.getValue("monitorClass");
        return t != null && t.getName().equals(Lock.class.getName());
    }

    private static void assertTimeOutEvent(List<RecordedEvent> events, long timeout, String expectedThreadName) {
        for (RecordedEvent e : events) {
            if (isWaitEvent(e)) {
                Long l = e.getValue("timeout");
                if (l == timeout) {
                    RecordedThread notifier = e.getValue("notifier");
                    String threadName = null;
                    if (notifier != null) {
                        threadName = notifier.getJavaName();
                    }
                    Asserts.assertEquals(threadName, expectedThreadName, "Invalid thread");
                    return;
                }
            }
        }
        Asserts.fail("Could not find event with monitorClass" + Lock.class.getName());
    }
}
