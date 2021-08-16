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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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
 * @run main/othervm jdk.jfr.event.io.TestFileChannelEvents
 */
public class TestFileChannelEvents {
    public static void main(String[] args) throws Throwable {
        File tmp = Utils.createTempFile("TestFileChannelEvents", ".tmp").toFile();
        try (Recording recording = new Recording()) {
            List<IOEvent> expectedEvents = new ArrayList<>();
            try (RandomAccessFile rf = new RandomAccessFile(tmp, "rw"); FileChannel ch = rf.getChannel();) {
                recording.enable(IOEvent.EVENT_FILE_FORCE).withThreshold(Duration.ofMillis(0));
                recording.enable(IOEvent.EVENT_FILE_READ).withThreshold(Duration.ofMillis(0));
                recording.enable(IOEvent.EVENT_FILE_WRITE).withThreshold(Duration.ofMillis(0));
                recording.start();

                ByteBuffer bufA = ByteBuffer.allocateDirect(10);
                ByteBuffer bufB = ByteBuffer.allocateDirect(20);
                bufA.put("1234567890".getBytes());
                bufB.put("1234567890".getBytes());

                // test write(ByteBuffer)
                bufA.flip();
                long size = ch.write(bufA);
                expectedEvents.add(IOEvent.createFileWriteEvent(size, tmp));

                // test write(ByteBuffer, long)
                bufA.flip();
                size = ch.write(bufA, bufA.capacity() / 2);
                expectedEvents.add(IOEvent.createFileWriteEvent(size, tmp));

                // test write(ByteBuffer[])
                bufA.flip();
                bufA.limit(5);
                bufB.flip();
                bufB.limit(5);
                size = ch.write(new ByteBuffer[] { bufA, bufB });
                expectedEvents.add(IOEvent.createFileWriteEvent(size, tmp));

                // test force(boolean)
                ch.force(true);
                expectedEvents.add(IOEvent.createFileForceEvent(tmp));

                // reset file
                ch.position(0);

                // test read(ByteBuffer)
                bufA.clear();
                size = ch.read(bufA);
                expectedEvents.add(IOEvent.createFileReadEvent(size, tmp));

                // test read(ByteBuffer, long)
                bufA.clear();
                size = ch.read(bufA, bufA.capacity() / 2);
                expectedEvents.add(IOEvent.createFileReadEvent(size, tmp));

                // test read(ByteBuffer[])
                bufA.clear();
                bufA.limit(5);
                bufB.clear();
                bufB.limit(5);
                size = ch.read(new ByteBuffer[] { bufA, bufB });
                expectedEvents.add(IOEvent.createFileReadEvent(size, tmp));

                // Read at EOF. Should get size -1 in event.
                ch.position(ch.size());
                bufA.clear();
                size = ch.read(bufA);
                assertEquals(size, -1L, "Expected size -1 when read at EOF");
                expectedEvents.add(IOEvent.createFileReadEvent(size, tmp));

                ch.close();
                recording.stop();
                List<RecordedEvent> events = Events.fromRecording(recording);
                IOHelper.verifyEqualsInOrder(events, expectedEvents);
            }
        }
    }
}
