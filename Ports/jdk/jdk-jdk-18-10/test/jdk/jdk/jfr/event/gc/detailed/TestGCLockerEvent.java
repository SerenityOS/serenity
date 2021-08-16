/*
 * Copyright (c) 2021, Alibaba Group Holding Limited. All Rights Reserved.
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
 *
 */

/**
 * @test TestGCLockerEvent
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xmx32m -Xms32m -Xmn12m -XX:+UseG1GC jdk.jfr.event.gc.detailed.TestGCLockerEvent
 */

package jdk.jfr.event.gc.detailed;

import static jdk.test.lib.Asserts.assertTrue;

import java.util.concurrent.CountDownLatch;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

import sun.hotspot.WhiteBox;

public class TestGCLockerEvent {

    private static final String EVENT_NAME = EventNames.GCLocker;

    private static final int CRITICAL_THREAD_COUNT = 4;

    private static final CountDownLatch LOCK_COUNT_SIGNAL = new CountDownLatch(CRITICAL_THREAD_COUNT);

    private static final CountDownLatch UNLOCK_SIGNAL = new CountDownLatch(1);

    private static final CountDownLatch UNLOCK_COUNT_SIGNAL = new CountDownLatch(CRITICAL_THREAD_COUNT);

    private static final String CRITICAL_THREAD_NAME_PREFIX = "Critical Thread ";

    private static final int STALL_THREAD_COUNT = 8;

    private static final CountDownLatch STALL_COUNT_SIGNAL = new CountDownLatch(STALL_THREAD_COUNT);

    private static final int LOOP = 32;

    private static final int M = 1024 * 1024;

    public static void main(String[] args) throws Exception {
        var recording = new Recording();
        recording.enable(EVENT_NAME);
        recording.start();

        startCriticalThreads();
        LOCK_COUNT_SIGNAL.await();
        startStallThreads();
        STALL_COUNT_SIGNAL.await();

        // Wait threads to be stalled
        Thread.sleep(1500);

        UNLOCK_SIGNAL.countDown();
        UNLOCK_COUNT_SIGNAL.await();
        recording.stop();

        // Verify recording
        var all = Events.fromRecording(recording);
        Events.hasEvents(all);
        var event = all.get(0);

        assertTrue(Events.isEventType(event, EVENT_NAME));
        Events.assertField(event, "lockCount").equal(CRITICAL_THREAD_COUNT);
        Events.assertField(event, "stallCount").atLeast(STALL_THREAD_COUNT);
        assertTrue(event.getThread().getJavaName().startsWith(CRITICAL_THREAD_NAME_PREFIX));

        recording.close();
    }

    private static void startCriticalThreads() {
        for (var i = 0; i < CRITICAL_THREAD_COUNT; i++) {
            new Thread(() -> {
                try {
                    WhiteBox.getWhiteBox().lockCritical();
                    LOCK_COUNT_SIGNAL.countDown();

                    UNLOCK_SIGNAL.await();
                    WhiteBox.getWhiteBox().unlockCritical();
                    UNLOCK_COUNT_SIGNAL.countDown();
                } catch (InterruptedException ex) {
                }
            }, CRITICAL_THREAD_NAME_PREFIX + i).start();
        }
    }

    private static void startStallThreads() {
        var ts = new Thread[STALL_THREAD_COUNT];
        for (var i = 0; i < STALL_THREAD_COUNT; i++) {
            ts[i] = new Thread(() -> {
                STALL_COUNT_SIGNAL.countDown();
                for (int j = 0; j < LOOP; j++) {
                    byte[] bytes = new byte[M];
                }
            });
        }
        for (Thread t : ts) {
            t.start();
        }
    }
}

