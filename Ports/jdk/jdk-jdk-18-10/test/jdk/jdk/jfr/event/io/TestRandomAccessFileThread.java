/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.thread.TestThread;
import jdk.test.lib.thread.XRun;


/**
 * @test
 * @summary Verify the event time stamp and thread name
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm -XX:+UnlockExperimentalVMOptions -XX:-UseFastUnorderedTimeStamps jdk.jfr.event.io.TestRandomAccessFileThread
 */

// TODO: This test should work without -XX:-UseFastUnorderedTimeStamps

// The test uses 2 threads to read and write to a file.
// The number of bytes in each read/write operation is increased by 1.
// By looking at the number of bytes in each event, we know in what order
// the events should arrive. This is used to verify the event time stamps.
public class TestRandomAccessFileThread {
    private static final int OP_COUNT = 100;    // Total number of read/write operations.
    private static volatile int writeCount = 0; // Number of writes executed.

    public static void main(String[] args) throws Throwable {
        File tmp = Utils.createTempFile("TestRandomAccessFileThread", ".tmp").toFile();
        try (Recording recording = new Recording()) {
            recording.enable(IOEvent.EVENT_FILE_READ).withThreshold(Duration.ofMillis(0));
            recording.enable(IOEvent.EVENT_FILE_WRITE).withThreshold(Duration.ofMillis(0));
            recording.start();

            TestThread writerThread = new TestThread(new XRun() {
                @Override
                public void xrun() throws IOException {
                    final byte[] buf = new byte[OP_COUNT];
                    for (int i = 0; i < buf.length; ++i) {
                        buf[i] = (byte)((i + 'a') % 255);
                    }
                    try (RandomAccessFile raf = new RandomAccessFile(tmp, "rwd")) {
                        for(int i = 0; i < OP_COUNT; ++i) {
                            raf.write(buf, 0, i + 1);
                            writeCount++;
                        }
                    }
                }}, "TestWriterThread");

            TestThread readerThread = new TestThread(new XRun() {
            @Override
            public void xrun() throws IOException {
                try (RandomAccessFile raf = new RandomAccessFile(tmp, "r")) {
                    byte[] buf = new byte[OP_COUNT];
                    for(int i = 0; i < OP_COUNT; ++i) {
                        while (writeCount <= i) {
                            // No more data to read. Wait for writer thread.
                            Thread.yield();
                        }
                        int expectedSize = i + 1;
                        int actualSize = raf.read(buf, 0, expectedSize);
                        Asserts.assertEquals(actualSize, expectedSize, "Wrong read size. Probably test error.");
                    }
                }
            }}, "TestReaderThread");

            readerThread.start();
            writerThread.start();
            writerThread.joinAndThrow();
            readerThread.joinAndThrow();
            recording.stop();

            List<RecordedEvent> events = Events.fromRecording(recording);
            events.sort(new EventComparator());

            List<RecordedEvent> readEvents = new ArrayList<>();
            List<RecordedEvent> writeEvents = new ArrayList<>();
            for (RecordedEvent event : events) {
                if (!isOurEvent(event, tmp)) {
                    continue;
                }
                logEventSummary(event);
                if (Events.isEventType(event, IOEvent.EVENT_FILE_READ)) {
                    readEvents.add(event);
                } else {
                    writeEvents.add(event);
                }
            }

            verifyThread(readEvents, readerThread);
            verifyThread(writeEvents, writerThread);
            verifyBytes(readEvents, "bytesRead");
            verifyBytes(writeEvents, "bytesWritten");
            verifyTimes(readEvents);
            verifyTimes(writeEvents);
            verifyReadWriteTimes(readEvents, writeEvents);

            Asserts.assertEquals(readEvents.size(), OP_COUNT, "Wrong number of read events");
            Asserts.assertEquals(writeEvents.size(), OP_COUNT, "Wrong number of write events");
        }
    }

        private static void logEventSummary(RecordedEvent event) {
            boolean isRead = Events.isEventType(event, IOEvent.EVENT_FILE_READ);
            String name = isRead ? "read " : "write";
            String bytesField = isRead ? "bytesRead" : "bytesWritten";
            long bytes = Events.assertField(event, bytesField).getValue();
            long commit = Events.assertField(event, "startTime").getValue();
            Instant start = event.getStartTime();
            Instant end = event.getEndTime();
            System.out.printf("%s: bytes=%d, commit=%d, start=%s, end=%s%n", name, bytes, commit, start, end);
        }

        private static void verifyThread(List<RecordedEvent> events, Thread thread) {
            events.stream().forEach(e -> Events.assertEventThread(e, thread));
        }

        private static void verifyBytes(List<RecordedEvent> events, String fieldName) {
            long expectedBytes = 0;
            for (RecordedEvent event : events) {
                Events.assertField(event, fieldName).equal(++expectedBytes);
            }
        }

        // Verify that all times are increasing
        private static void verifyTimes(List<RecordedEvent> events) {
            RecordedEvent prev = null;
            for (RecordedEvent curr : events) {
                if (prev != null) {
                    try {
                        Asserts.assertGreaterThanOrEqual(curr.getStartTime(), prev.getStartTime(), "Wrong startTime");
                        Asserts.assertGreaterThanOrEqual(curr.getEndTime(), prev.getEndTime(), "Wrong endTime");
                        long commitPrev = Events.assertField(prev, "startTime").getValue();
                        long commitCurr = Events.assertField(curr, "startTime").getValue();
                        Asserts.assertGreaterThanOrEqual(commitCurr, commitPrev, "Wrong commitTime");
                    } catch (Exception e) {
                        System.out.println("Error: " + e.getMessage());
                        System.out.println("Prev Event: " + prev);
                        System.out.println("Curr Event: " + curr);
                        throw e;
                    }
                }
                prev = curr;
            }
        }

        // Verify that all times are increasing
        private static void verifyReadWriteTimes(List<RecordedEvent> readEvents, List<RecordedEvent> writeEvents) {
            List<RecordedEvent> events = new ArrayList<>();
            events.addAll(readEvents);
            events.addAll(writeEvents);
            events.sort(new EventComparator());

            int countRead = 0;
            int countWrite = 0;
            for (RecordedEvent event : events) {
                if (Events.isEventType(event, IOEvent.EVENT_FILE_READ)) {
                    ++countRead;
                } else {
                    ++countWrite;
                }
                // We can not read from the file before it has been written.
                // This check verifies that times of different threads are correct.
                // Since the read and write are from different threads, it is possible that the read
                // is committed before the same write.
                // But read operation may only be 1 step ahead of the write operation.
                Asserts.assertLessThanOrEqual(countRead, countWrite + 1, "read must be after write");
            }
        }

        private static boolean isOurEvent(RecordedEvent event, File file) {
            if (!Events.isEventType(event, IOEvent.EVENT_FILE_READ) &&
                !Events.isEventType(event, IOEvent.EVENT_FILE_WRITE)) {
                return false;
            }
            String path = Events.assertField(event, "path").getValue();
            return file.getPath().equals(path);
        }

        private static class EventComparator implements Comparator<RecordedEvent> {
            @Override
            public int compare(RecordedEvent a, RecordedEvent b) {
                long commitA = Events.assertField(a, "startTime").getValue();
                long commitB = Events.assertField(b, "startTime").getValue();
                return Long.compare(commitA, commitB);
            }
        }

}
