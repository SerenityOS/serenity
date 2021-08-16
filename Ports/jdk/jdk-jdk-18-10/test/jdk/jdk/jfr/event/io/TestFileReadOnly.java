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

import static jdk.test.lib.Asserts.fail;

import java.io.File;
import java.io.IOException;
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
 * @run main/othervm jdk.jfr.event.io.TestFileReadOnly
 */
public class TestFileReadOnly {

    public static void main(String[] args) throws Throwable {
        File tmp = Utils.createTempFile("TestFileReadOnly", ".tmp").toFile();
        try (Recording recording = new Recording()) {
            List<IOEvent> expectedEvents = new ArrayList<>();

            recording.enable(IOEvent.EVENT_FILE_READ).withThreshold(Duration.ofMillis(0));
            recording.enable(IOEvent.EVENT_FILE_WRITE).withThreshold(Duration.ofMillis(0));
            recording.start();

            final byte[] buf = { 1, 2, 3 };

            // Create the file.
            try (RandomAccessFile f = new RandomAccessFile(tmp, "rw")) {
                f.write(buf);
                expectedEvents.add(IOEvent.createFileWriteEvent(buf.length, tmp));
            }

            // Reopen the file as ReadOnly and try to write to it.
            // Should generate an event with bytesWritten = -1.
            try (RandomAccessFile f = new RandomAccessFile(tmp, "r")) {
                try {
                    f.write(buf);
                    fail("No exception for ReadOnly File");
                } catch (IOException e) {
                    // Expected exception
                    expectedEvents.add(IOEvent.createFileWriteEvent(-1, tmp));
                }
            }

            // Try to write to read-only FileChannel.
            try (RandomAccessFile f = new RandomAccessFile(tmp, "r"); FileChannel ch = f.getChannel()) {
                ByteBuffer writeBuf = ByteBuffer.allocateDirect(buf.length);
                writeBuf.put(buf);
                writeBuf.flip();
                ch.position(0);
                try {
                    ch.write(writeBuf);
                    fail("No exception for ReadOnly FileChannel");
                } catch (java.nio.channels.NonWritableChannelException e) {
                    // Expected exception
                    expectedEvents.add(IOEvent.createFileWriteEvent(-1, tmp));
                }
            }

            recording.stop();
            List<RecordedEvent> events = Events.fromRecording(recording);
            IOHelper.verifyEqualsInOrder(events, expectedEvents);
        }
    }
}
