/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @build jdk.jfr.event.runtime.LatchedThread
 * @run main/othervm jdk.jfr.event.runtime.TestThreadEndEvent
 */
public class TestThreadEndEvent {
    private final static String EVENT_NAME_THREAD_END = EventNames.ThreadEnd;

    public static void main(String[] args) throws Throwable {
        try (Recording recording = new Recording()) {
            recording.enable(EVENT_NAME_THREAD_END);

            LatchedThread beforeThread = new LatchedThread("Before Thread");
            beforeThread.start();
            beforeThread.awaitStarted();
            recording.start();

            // End an already running thread
            beforeThread.stopAndJoin();

            // End a thread that is started during recording
            LatchedThread duringThread = new LatchedThread("During Thread");
            duringThread.start();
            duringThread.stopAndJoin();

            // Start a thread and end it after the recording has stopped
            LatchedThread afterThread = new LatchedThread("After Thread");
            afterThread.start();
            afterThread.awaitStarted();

            recording.stop();
            afterThread.stopAndJoin();

            List<RecordedEvent> events = Events.fromRecording(recording);
            assertEvent(events, beforeThread);
            assertEvent(events, duringThread);
            Asserts.assertNull(findEventByThreadName(events, afterThread.getName()));
        }
    }

    private static void assertEvent(List<RecordedEvent> events, LatchedThread thread) throws Exception {
        RecordedEvent event = findEventByThreadName(events, thread.getName());
        System.out.println(event);
        RecordedThread t = event.getThread();
        Asserts.assertEquals(event.getThread("thread").getJavaName(), thread.getName());
        Asserts.assertEquals(t.getThreadGroup().getName(), LatchedThread.THREAD_GROUP.getName());
    }

    private static RecordedEvent findEventByThreadName(List<RecordedEvent> events, String name) {
        for (RecordedEvent e : events) {
            if (e.getThread().getJavaName().equals(name)) {
                return e;
            }
        }
        return null;
    }
}
