/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.api.consumer;

import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.SimpleEvent;

/**
 * @test
 * @summary Tests that the RecordedEvent.getThread() returns th expected info
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.TestRecordedEventGetThread
 */
public class TestRecordedEventGetThread {

    private static final String MY_THREAD_NAME = "MY_THREAD_NAME";

    public static void main(String[] args) throws Throwable {
        Thread currentThread = Thread.currentThread();
        currentThread.setName(MY_THREAD_NAME);

        try (Recording r = new Recording()) {
            r.start();
            SimpleEvent t = new SimpleEvent();
            t.commit();
            r.stop();

            List<RecordedEvent> events = Events.fromRecording(r);
            Events.hasEvents(events);
            RecordedEvent event = events.get(0);
            RecordedThread recordedThread = event.getThread();
            Asserts.assertNotNull(recordedThread);

            Asserts.assertEquals(recordedThread.getJavaName(), MY_THREAD_NAME);
            Asserts.assertEquals(recordedThread.getJavaThreadId(), currentThread.getId());
            Asserts.assertNotNull(recordedThread.getOSThreadId());
            Asserts.assertNotNull(recordedThread.getId());
            Asserts.assertEquals(recordedThread.getOSName(), MY_THREAD_NAME);
        }
    }
}
