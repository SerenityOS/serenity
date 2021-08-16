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
import static jdk.test.lib.Asserts.assertNotEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Utils;
import jdk.test.lib.jfr.Events;

/**
 * @test
 * @summary Test with FlightRecorder enabled but with the events disabled.
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.event.io.TestDisabledEvents
 */

// Verify that IO operations are correct and that no events are generated.
public class TestDisabledEvents {

    private static final int writeInt = 'A';
    private static final byte[] writeBuf = { 'B', 'C', 'D' };

    public static void main(String[] args) throws Throwable {
        File tmp = Utils.createTempFile("TestDisabledEvents", ".tmp").toFile();
        try (Recording recording = new Recording()) {
            recording.disable(IOEvent.EVENT_FILE_READ);
            recording.disable(IOEvent.EVENT_FILE_WRITE);
            recording.start();

            useRandomAccessFile(tmp);
            useFileStreams(tmp);
            useFileChannel(tmp);

            recording.stop();
            for (RecordedEvent event : Events.fromRecording(recording)) {
                final String eventName = event.getEventType().getName();
                System.out.println("Got eventName:" + eventName);
                assertNotEquals(eventName, IOEvent.EVENT_FILE_READ, "Got disabled read event");
                assertNotEquals(eventName, IOEvent.EVENT_FILE_WRITE, "Got disabled write event");
            }
        }
    }

    private static void useRandomAccessFile(File tmp) throws Throwable {
        tmp.delete();
        try (RandomAccessFile ras = new RandomAccessFile(tmp, "rw")) {
            ras.write(writeInt);
            ras.write(writeBuf);
            ras.seek(0);
            int readInt = ras.read();
            assertEquals(readInt, writeInt, "Wrong readInt");
            byte[] readBuf = new byte[writeBuf.length];
            int readSize = ras.read(readBuf);
            assertEquals(readSize, writeBuf.length, "Wrong readSize");
            // Try to read more which should generate EOF.
            readInt = ras.read();
            assertEquals(readInt, -1, "Wrong readInt after EOF");
        }
    }

    private static void useFileStreams(File tmp) throws Throwable {
        tmp.delete();
        try (FileOutputStream fos = new FileOutputStream(tmp)) {
            fos.write(writeInt);
            fos.write(writeBuf);
        }

        try (FileInputStream fis = new FileInputStream(tmp)) {
            int readInt = fis.read();
            assertEquals(readInt, writeInt, "Wrong readInt");
            byte[] readBuf = new byte[writeBuf.length];
            int readSize = fis.read(readBuf);
            assertEquals(readSize, writeBuf.length, "Wrong readSize");
            // Try to read more which should generate EOF.
            readInt = fis.read();
            assertEquals(readInt, -1, "Wrong readInt after EOF");
        }
    }

    private static void useFileChannel(File tmp) throws Throwable {
        tmp.delete();
        try (RandomAccessFile rf = new RandomAccessFile(tmp, "rw");
                FileChannel ch = rf.getChannel()) {
            final String bufContent = "0123456789";
            final int bufSize = bufContent.length();
            ByteBuffer writeBuf = ByteBuffer.allocateDirect(bufSize);
            writeBuf.put(bufContent.getBytes());

            writeBuf.flip();
            int writeSize = ch.write(writeBuf);
            assertEquals(writeSize, bufSize, "Wrong writeSize for FileChannel");

            ch.position(0);
            ByteBuffer readBuf = ByteBuffer.allocateDirect(bufSize);
            int readSize = ch.read(readBuf);
            assertEquals(readSize, bufSize, "Wrong readSize full for FileChannel");
            assertEquals(0, writeBuf.compareTo(readBuf), "Unexpected readBuf content");
        }
    }
}
