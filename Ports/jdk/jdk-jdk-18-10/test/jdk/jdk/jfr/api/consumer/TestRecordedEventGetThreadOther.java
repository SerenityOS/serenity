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

import java.nio.file.Path;
import java.util.List;

import jdk.jfr.Event;
import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;

/**
 * @test
 * @summary Tests that the RecordedEvent.getThread() returns th expected info
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.TestRecordedEventGetThreadOther
 */
public class TestRecordedEventGetThreadOther {

    private static final String MY_THREAD_NAME = "MY_THREAD_NAME";

    static class TestEvent extends Event {
    }

    static class PostingThread extends Thread {
        private final Path dumpFilePath;
        PostingThread(Path dumpFilePath) {
            this.dumpFilePath = dumpFilePath;
        }

        @Override
        public void run() {
            try {
                System.out.println("Starting thread...");
                try (Recording r = new Recording()) {
                    r.start();
                    TestEvent t = new TestEvent();
                    t.commit();
                    r.stop();
                    r.dump(dumpFilePath);
                    System.out.println("events dumped to the file " + dumpFilePath);
                }
            } catch (Throwable t) {
                t.printStackTrace();
                Asserts.fail();
            }
        }
    }

    public static void main(String[] args) throws Exception  {
        Path dumpFilePath = Utils.createTempFile("event-thread", ".jfr");

        PostingThread thread = new PostingThread(dumpFilePath);
        thread.setName(MY_THREAD_NAME);
        thread.start();
        thread.join();

        List<RecordedEvent> events = RecordingFile.readAllEvents(dumpFilePath);
        Asserts.assertEquals(events.size(), 1);

        RecordedEvent event = events.get(0);
        RecordedThread recordedThread = event.getThread();

        Asserts.assertNotNull(recordedThread);
        Asserts.assertEquals(recordedThread.getJavaName(), MY_THREAD_NAME);
        Asserts.assertEquals(recordedThread.getJavaThreadId(), thread.getId());
        Asserts.assertNotNull(recordedThread.getId());
        Asserts.assertEquals(recordedThread.getOSName(), MY_THREAD_NAME);
    }
}
