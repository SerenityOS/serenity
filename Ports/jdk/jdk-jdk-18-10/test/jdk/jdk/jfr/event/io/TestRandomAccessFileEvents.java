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

import static jdk.test.lib.Asserts.assertEquals;

import java.io.File;
import java.io.RandomAccessFile;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Utils;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.event.io.TestRandomAccessFileEvents
 */
public class TestRandomAccessFileEvents {

    public static void main(String[] args) throws Throwable {
        File tmp = Utils.createTempFile("TestRandomAccessFileEvents", ".tmp").toFile();
        try (Recording recording = new Recording()) {
            List<IOEvent> expectedEvents = new ArrayList<>();

            recording.enable(IOEvent.EVENT_FILE_WRITE).withThreshold(Duration.ofMillis(0));
            recording.enable(IOEvent.EVENT_FILE_READ).withThreshold(Duration.ofMillis(0));
            recording.start();

            RandomAccessFile ras = new RandomAccessFile(tmp, "rw");
            int writeInt = 47;
            byte[] writeBuffer = {10,11,12,13};

            // Write an int and a buffer.
            ras.write(writeInt);
            expectedEvents.add(IOEvent.createFileWriteEvent(1, tmp));
            ras.write(writeBuffer);
            expectedEvents.add(IOEvent.createFileWriteEvent(writeBuffer.length, tmp));

            ras.seek(0);

            // Read int and buffer
            int readInt = ras.read();
            assertEquals(readInt, writeInt, "wrong int read");
            expectedEvents.add(IOEvent.createFileReadEvent(1, tmp));
            byte[] readBuffer = new byte [writeBuffer.length];
            int size = ras.read(readBuffer);
            verifyBufferEquals(readBuffer, writeBuffer);
            expectedEvents.add(IOEvent.createFileReadEvent(readBuffer.length, tmp));

            // Read beyond EOF
            readInt = ras.read();
            assertEquals(-1, readInt, "wrong read after EOF");
            expectedEvents.add(IOEvent.createFileReadEvent(-1, tmp));

            // Seek to beginning and verify we can read after EOF.
            ras.seek(0);
            readInt = ras.read();
            assertEquals(readInt, writeInt, "wrong int read after seek(0)");
            expectedEvents.add(IOEvent.createFileReadEvent(1, tmp));

            // seek beyond EOF and verify we get EOF when reading.
            ras.seek(10);
            readInt = ras.read();
            assertEquals(-1, readInt, "wrong read after seek beyond EOF");
            expectedEvents.add(IOEvent.createFileReadEvent(-1, tmp));

            // Read partial buffer.
            int partialSize = writeBuffer.length - 2;
            ras.seek(ras.length()-partialSize);
            size = ras.read(readBuffer);
            assertEquals(size, partialSize, "Wrong size partial buffer read");
            expectedEvents.add(IOEvent.createFileReadEvent(size, tmp));

            ras.close();
            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            IOHelper.verifyEqualsInOrder(events, expectedEvents);
        }
    }

    private static void verifyBufferEquals(byte[] a, byte[] b) {
        assertEquals(a.length, b.length, "Wrong buffer size");
        for (int i = 0; i < a.length; ++i) {
            assertEquals(a[i], b[i], "Wrong buffer content at pos " + i);
        }
    }
}
