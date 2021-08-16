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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.thread.TestThread;
import jdk.test.lib.thread.XRun;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @run main/othervm jdk.jfr.event.io.TestSocketEvents
 */
public class TestSocketEvents {

    private static final int writeInt = 'A';
    private static final byte[] writeBuf = { 'B', 'C', 'D', 'E' };

    private List<IOEvent> expectedEvents = new ArrayList<>();

    private synchronized void addExpectedEvent(IOEvent event) {
        expectedEvents.add(event);
    }

    public static void main(String[] args) throws Throwable {
        new TestSocketEvents().test();
    }

    private void test() throws Throwable {
        try (Recording recording = new Recording()) {
            try (ServerSocket ss = new ServerSocket()) {
                recording.enable(IOEvent.EVENT_SOCKET_READ).withThreshold(Duration.ofMillis(0));
                recording.enable(IOEvent.EVENT_SOCKET_WRITE).withThreshold(Duration.ofMillis(0));
                recording.start();

                ss.setReuseAddress(true);
                ss.bind(null);

                TestThread readerThread = new TestThread(new XRun() {
                    @Override
                    public void xrun() throws IOException {
                        byte[] bs = new byte[4];
                        try (Socket s = ss.accept(); InputStream is = s.getInputStream()) {
                            int readInt = is.read();
                            assertEquals(readInt, writeInt, "Wrong readInt");
                            addExpectedEvent(IOEvent.createSocketReadEvent(1, s));

                            int bytesRead = is.read(bs, 0, 3);
                            assertEquals(bytesRead, 3, "Wrong bytesRead partial buffer");
                            addExpectedEvent(IOEvent.createSocketReadEvent(bytesRead, s));

                            bytesRead = is.read(bs);
                            assertEquals(bytesRead, writeBuf.length, "Wrong bytesRead full buffer");
                            addExpectedEvent(IOEvent.createSocketReadEvent(bytesRead, s));

                            // Try to read more, but writer have closed. Should
                            // get EOF.
                            readInt = is.read();
                            assertEquals(readInt, -1, "Wrong readInt at EOF");
                            addExpectedEvent(IOEvent.createSocketReadEvent(-1, s));
                        }
                    }
                });
                readerThread.start();

                try (Socket s = new Socket()) {
                    s.connect(ss.getLocalSocketAddress());
                    try (OutputStream os = s.getOutputStream()) {
                        os.write(writeInt);
                        addExpectedEvent(IOEvent.createSocketWriteEvent(1, s));
                        os.write(writeBuf, 0, 3);
                        addExpectedEvent(IOEvent.createSocketWriteEvent(3, s));
                        os.write(writeBuf);
                        addExpectedEvent(IOEvent.createSocketWriteEvent(writeBuf.length, s));
                    }
                }

                readerThread.joinAndThrow();
                recording.stop();
                List<RecordedEvent> events = Events.fromRecording(recording);
                IOHelper.verifyEquals(events, expectedEvents);
            }
        }
    }
}
