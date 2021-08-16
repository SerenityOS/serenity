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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Utils;
import jdk.test.lib.jfr.Events;

/**
 * @test TestFileStreamEvents
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.event.io.TestFileStreamEvents
 */

public class TestFileStreamEvents {
    public static void main(String[] args) throws Throwable {
        File tmp = Utils.createTempFile("TestFileStreamEvents", ".tmp").toFile();
        try (Recording recording = new Recording()) {
            List<IOEvent> expectedEvents = new ArrayList<>();
            try(FileOutputStream fos = new FileOutputStream(tmp); FileInputStream fis = new FileInputStream(tmp);) {
                recording.enable(IOEvent.EVENT_FILE_READ).withThreshold(Duration.ofMillis(0));
                recording.enable(IOEvent.EVENT_FILE_WRITE).withThreshold(Duration.ofMillis(0));
                recording.start();

                int writeByte = 47;
                byte[] writeBuf = {11, 12, 13, 14};

                // Write
                fos.write(writeByte);
                expectedEvents.add(IOEvent.createFileWriteEvent(1, tmp));
                fos.write(writeBuf);
                expectedEvents.add(IOEvent.createFileWriteEvent(writeBuf.length, tmp));
                fos.write(writeBuf, 0, 2);
                expectedEvents.add(IOEvent.createFileWriteEvent(2, tmp));

                // Read
                int readByte = fis.read();
                assertEquals(readByte, writeByte, "Wrong byte read");
                expectedEvents.add(IOEvent.createFileReadEvent(1, tmp));

                byte[] readBuf = new byte[writeBuf.length];
                long size = fis.read(readBuf);
                assertEquals(size, (long)writeBuf.length, "Wrong size when reading byte[]");
                expectedEvents.add(IOEvent.createFileReadEvent(size, tmp));

                size = fis.read(readBuf, 0, 2);
                assertEquals(size, 2L, "Wrong size when reading 2 bytes");
                expectedEvents.add(IOEvent.createFileReadEvent(size, tmp));

                // We are at EOF. Read more and verify we get size -1.
                size = fis.read(readBuf);
                assertEquals(size, -1L, "Size should be -1 at EOF");
                expectedEvents.add(IOEvent.createFileReadEvent(size, tmp));

                recording.stop();
                List<RecordedEvent> events = Events.fromRecording(recording);
                IOHelper.verifyEqualsInOrder(events, expectedEvents);
            }
        }
    }
}
