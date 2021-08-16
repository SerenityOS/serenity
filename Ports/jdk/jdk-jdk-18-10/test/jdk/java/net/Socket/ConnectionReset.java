/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @run testng ConnectionReset
 * @summary Test behavior of read and available when a connection is reset
 */

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class ConnectionReset {

    static final int REPEAT_COUNT = 5;

    /**
     * Tests available before read when there are no bytes to read
     */
    public void testAvailableBeforeRead1() throws IOException {
        System.out.println("testAvailableBeforeRead1");
        withResetConnection(null, s -> {
            InputStream in = s.getInputStream();
            for (int i=0; i<REPEAT_COUNT; i++) {
                int bytesAvailable = in.available();
                System.out.format("available => %d%n", bytesAvailable);
                assertTrue(bytesAvailable == 0);
                try {
                    int bytesRead = in.read();
                    if (bytesRead == -1) {
                        System.out.println("read => EOF");
                    } else {
                        System.out.println("read => 1 byte");
                    }
                    assertTrue(false);
                } catch (IOException ioe) {
                    System.out.format("read => %s (expected)%n", ioe);
                }
            }
        });
    }

    /**
     * Tests available before read when there are bytes to read
     */
    public void testAvailableBeforeRead2() throws IOException {
        System.out.println("testAvailableBeforeRead2");
        byte[] data = { 1, 2, 3 };
        withResetConnection(data, s -> {
            InputStream in = s.getInputStream();
            int remaining = data.length;
            for (int i=0; i<REPEAT_COUNT; i++) {
                int bytesAvailable = in.available();
                System.out.format("available => %d%n", bytesAvailable);
                assertTrue(bytesAvailable <= remaining);
                try {
                    int bytesRead = in.read();
                    if (bytesRead == -1) {
                        System.out.println("read => EOF");
                        assertTrue(false);
                    } else {
                        System.out.println("read => 1 byte");
                        assertTrue(remaining > 0);
                        remaining--;
                    }
                } catch (IOException ioe) {
                    System.out.format("read => %s%n", ioe);
                    remaining = 0;
                }
            }
        });
    }

    /**
     * Tests read before available when there are no bytes to read
     */
    public void testReadBeforeAvailable1() throws IOException {
        System.out.println("testReadBeforeAvailable1");
        withResetConnection(null, s -> {
            InputStream in = s.getInputStream();
            for (int i=0; i<REPEAT_COUNT; i++) {
                try {
                    int bytesRead = in.read();
                    if (bytesRead == -1) {
                        System.out.println("read => EOF");
                    } else {
                        System.out.println("read => 1 byte");
                    }
                    assertTrue(false);
                } catch (IOException ioe) {
                    System.out.format("read => %s (expected)%n", ioe);
                }
                int bytesAvailable = in.available();
                System.out.format("available => %d%n", bytesAvailable);
                assertTrue(bytesAvailable == 0);
            }
        });
    }

    /**
     * Tests read before available when there are bytes to read
     */
    public void testReadBeforeAvailable2() throws IOException {
        System.out.println("testReadBeforeAvailable2");
        byte[] data = { 1, 2, 3 };
        withResetConnection(data, s -> {
            InputStream in = s.getInputStream();
            int remaining = data.length;
            for (int i=0; i<REPEAT_COUNT; i++) {
                try {
                    int bytesRead = in.read();
                    if (bytesRead == -1) {
                        System.out.println("read => EOF");
                        assertTrue(false);
                    } else {
                        System.out.println("read => 1 byte");
                        assertTrue(remaining > 0);
                        remaining--;
                    }
                } catch (IOException ioe) {
                    System.out.format("read => %s%n", ioe);
                    remaining = 0;
                }
                int bytesAvailable = in.available();
                System.out.format("available => %d%n", bytesAvailable);
                assertTrue(bytesAvailable <= remaining);
            }
        });
    }

    /**
     * Tests available and read on a socket closed after connection reset
     */
    public void testAfterClose() throws IOException {
        System.out.println("testAfterClose");
        withResetConnection(null, s -> {
            InputStream in = s.getInputStream();
            try {
                in.read();
                assertTrue(false);
            } catch (IOException ioe) {
                // expected
            }
            s.close();
            try {
                int bytesAvailable = in.available();
                System.out.format("available => %d%n", bytesAvailable);
                assertTrue(false);
            } catch (IOException ioe) {
                System.out.format("available => %s (expected)%n", ioe);
            }
            try {
                int n = in.read();
                System.out.format("read => %d%n", n);
                assertTrue(false);
            } catch (IOException ioe) {
                System.out.format("read => %s (expected)%n", ioe);
            }
        });
    }

    interface ThrowingConsumer<T> {
        void accept(T t) throws IOException;
    }

    /**
     * Invokes a consumer with a Socket connected to a peer that has closed the
     * connection with a "connection reset". The peer sends the given data bytes
     * before closing (when data is not null).
     */
    static void withResetConnection(byte[] data, ThrowingConsumer<Socket> consumer)
        throws IOException
    {
        var loopback = InetAddress.getLoopbackAddress();
        try (var listener = new ServerSocket()) {
            listener.bind(new InetSocketAddress(loopback, 0));
            try (var socket = new Socket()) {
                socket.connect(listener.getLocalSocketAddress());
                try (Socket peer = listener.accept()) {
                    if (data != null) {
                        peer.getOutputStream().write(data);
                    }
                    peer.setSoLinger(true, 0);
                }
                consumer.accept(socket);
            }
        }
    }
}
