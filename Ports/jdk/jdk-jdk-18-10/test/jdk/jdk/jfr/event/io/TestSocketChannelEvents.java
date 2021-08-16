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
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
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
 * @run main/othervm jdk.jfr.event.io.TestSocketChannelEvents
 */
public class TestSocketChannelEvents {
    private static final int bufSizeA = 10;
    private static final int bufSizeB = 20;

    private List<IOEvent> expectedEvents = new ArrayList<>();

    private synchronized void addExpectedEvent(IOEvent event) {
        expectedEvents.add(event);
    }

    public static void main(String[] args) throws Throwable {
        new TestSocketChannelEvents().test();
    }

    public void test() throws Throwable {
        try (Recording recording = new Recording()) {
            try (ServerSocketChannel ss = ServerSocketChannel.open()) {
                recording.enable(IOEvent.EVENT_SOCKET_READ).withThreshold(Duration.ofMillis(0));
                recording.enable(IOEvent.EVENT_SOCKET_WRITE).withThreshold(Duration.ofMillis(0));
                recording.start();

                ss.socket().setReuseAddress(true);
                ss.socket().bind(null);

                TestThread readerThread = new TestThread(new XRun() {
                    @Override
                    public void xrun() throws IOException {
                        ByteBuffer bufA = ByteBuffer.allocate(bufSizeA);
                        ByteBuffer bufB = ByteBuffer.allocate(bufSizeB);
                        try (SocketChannel sc = ss.accept()) {
                            int readSize = sc.read(bufA);
                            assertEquals(readSize, bufSizeA, "Wrong readSize bufA");
                            addExpectedEvent(IOEvent.createSocketReadEvent(bufSizeA, sc.socket()));

                            bufA.clear();
                            bufA.limit(1);
                            readSize = (int) sc.read(new ByteBuffer[] { bufA, bufB });
                            assertEquals(readSize, 1 + bufSizeB, "Wrong readSize 1+bufB");
                            addExpectedEvent(IOEvent.createSocketReadEvent(readSize, sc.socket()));

                            // We try to read, but client have closed. Should
                            // get EOF.
                            bufA.clear();
                            bufA.limit(1);
                            readSize = sc.read(bufA);
                            assertEquals(readSize, -1, "Wrong readSize at EOF");
                            addExpectedEvent(IOEvent.createSocketReadEvent(-1, sc.socket()));
                        }
                    }
                });
                readerThread.start();

                try (SocketChannel sc = SocketChannel.open(ss.socket().getLocalSocketAddress())) {
                    ByteBuffer bufA = ByteBuffer.allocateDirect(bufSizeA);
                    ByteBuffer bufB = ByteBuffer.allocateDirect(bufSizeB);
                    for (int i = 0; i < bufSizeA; ++i) {
                        bufA.put((byte) ('a' + (i % 20)));
                    }
                    for (int i = 0; i < bufSizeB; ++i) {
                        bufB.put((byte) ('A' + (i % 20)));
                    }
                    bufA.flip();
                    bufB.flip();

                    sc.write(bufA);
                    addExpectedEvent(IOEvent.createSocketWriteEvent(bufSizeA, sc.socket()));

                    bufA.clear();
                    bufA.limit(1);
                    int bytesWritten = (int) sc.write(new ByteBuffer[] { bufA, bufB });
                    assertEquals(bytesWritten, 1 + bufSizeB, "Wrong bytesWritten 1+bufB");
                    addExpectedEvent(IOEvent.createSocketWriteEvent(bytesWritten, sc.socket()));
                }

                readerThread.joinAndThrow();
                recording.stop();
                List<RecordedEvent> events = Events.fromRecording(recording);
                IOHelper.verifyEquals(events, expectedEvents);
            }
        }
    }
}
