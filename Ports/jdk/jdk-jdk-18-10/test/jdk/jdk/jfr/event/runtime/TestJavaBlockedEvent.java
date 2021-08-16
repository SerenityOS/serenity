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

import static jdk.test.lib.Asserts.assertFalse;
import static jdk.test.lib.Asserts.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.management.ThreadMXBeanTool;
import jdk.test.lib.thread.TestThread;
import jdk.test.lib.thread.XRun;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 *
 * @run main/othervm jdk.jfr.event.runtime.TestJavaBlockedEvent
 */
public class TestJavaBlockedEvent {
    private static final String EVENT_NAME = EventNames.JavaMonitorEnter;
    private static final long THRESHOLD_MILLIS = 1;

    static class Lock {
    }

    public static void main(String[] args) throws Throwable {
        Recording recording = new Recording();

        recording.enable(EVENT_NAME).withThreshold(Duration.ofMillis(THRESHOLD_MILLIS));
        recording.start();
        final Lock lock = new Lock();
        final CountDownLatch blockerInLock = new CountDownLatch(1);
        final CountDownLatch blockingFinished = new CountDownLatch(1);
        final CountDownLatch blockedFinished = new CountDownLatch(1);
        TestThread blockerThread = new TestThread(new XRun() {
            @Override
            public void xrun() throws Throwable {
                synchronized (lock) {
                    blockerInLock.countDown();
                    blockingFinished.await();
                }
                blockedFinished.await();
            }
        });

        TestThread blockedThread = new TestThread(new XRun() {
            @Override
            public void xrun() throws Throwable {
                blockerInLock.await();
                synchronized (lock) {
                    blockedFinished.countDown();
                }
            }
        });
        blockerThread.start();
        blockedThread.start();

        blockerInLock.await();
        ThreadMXBeanTool.waitUntilBlockingOnObject(blockedThread, Thread.State.BLOCKED, lock);
        Thread.sleep(2 * THRESHOLD_MILLIS);
        blockingFinished.countDown();
        blockedFinished.await();

        blockedThread.join();
        blockerThread.join();
        recording.stop();

        List<RecordedEvent> events = Events.fromRecording(recording);
        boolean isAnyFound = false;
        for (RecordedEvent event : events) {
            System.out.println("Event:" + event);
            if (event.getThread().getJavaThreadId() == blockedThread.getId()) {
                if (isMyLock(Events.assertField(event, "monitorClass.name").getValue())) {
                    Events.assertEventThread(event, "previousOwner", blockerThread);
                    Events.assertField(event, "address").above(0L);
                    assertFalse(isAnyFound, "Found multiple events");
                    isAnyFound = true;
                }
            }
        }
        assertTrue(isAnyFound, "No blocking event from " + blockedThread.getName());
    }

    private static boolean isMyLock(String className) {
        return Lock.class.getName().replace('.', '/').equals(className);
    }
}
