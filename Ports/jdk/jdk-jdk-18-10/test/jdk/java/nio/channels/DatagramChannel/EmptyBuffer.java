/*
 * Copyright (c) 2002, 2015, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4669040
 * @summary Test DatagramChannel receive with empty buffer
 * @author Mike McCloskey
 */

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;

public class EmptyBuffer {

    private static final PrintStream log = System.err;

    public static void main(String[] args) throws Exception {
        test();
    }

    private static void test() throws Exception {
        DatagramChannel dc = DatagramChannel.open();
        InetAddress localHost = InetAddress.getLocalHost();
        dc.bind(new InetSocketAddress(localHost, 0));

        Server server = new Server(dc.getLocalAddress());
        Thread serverThread = new Thread(server);
        serverThread.start();

        try {
            InetSocketAddress isa = new InetSocketAddress(localHost, server.port());
            dc.connect(isa);

            ByteBuffer bb = ByteBuffer.allocateDirect(12);
            bb.order(ByteOrder.BIG_ENDIAN);
            bb.putInt(1).putLong(1);
            bb.flip();

            dc.write(bb);
            bb.rewind();
            dc.write(bb);
            bb.rewind();
            dc.write(bb);

            Thread.sleep(2000);

            serverThread.interrupt();
            server.throwException();
        } finally {
            dc.close();
        }
    }

    private static class Server implements Runnable {
        private final DatagramChannel dc;
        private final SocketAddress clientAddress;
        private Exception e = null;

        Server(SocketAddress clientAddress) throws IOException {
            this.dc = DatagramChannel.open().bind(new InetSocketAddress(0));
            this.clientAddress = clientAddress;
        }

        int port() {
            return dc.socket().getLocalPort();
        }

        void throwException() throws Exception {
            if (e != null)
                throw e;
        }

        void showBuffer(String s, ByteBuffer bb) {
            log.println(s);
            bb.rewind();
            for (int i=0; i<bb.limit(); i++) {
                byte element = bb.get();
                log.print(element);
            }
            log.println();
        }

        @Override
        public void run() {
            try {
                ByteBuffer bb = ByteBuffer.allocateDirect(12);
                bb.clear();
                // Only one clear. The buffer will be full after
                // the first receive, but it should still block
                // and receive and discard the next two
                int numberReceived = 0;
                while (!Thread.interrupted()) {
                    SocketAddress sa;
                    try {
                        sa = dc.receive(bb);
                    } catch (ClosedByInterruptException cbie) {
                        // Expected
                        log.println("Took expected exit");
                        // Verify that enough packets were received
                        if (numberReceived != 3)
                            throw new RuntimeException("Failed: Too few datagrams");
                        break;
                    }
                    if (sa != null) {
                        log.println("Client: " + sa);
                        // Check client address so as not to count stray packets
                        if (sa.equals(clientAddress)) {
                            showBuffer("RECV", bb);
                            numberReceived++;
                        }
                        if (numberReceived > 3)
                            throw new RuntimeException("Failed: Too many datagrams");
                        sa = null;
                    }
                }
            } catch (Exception ex) {
                e = ex;
            } finally {
                try { dc.close(); } catch (IOException ignore) { }
            }
        }
    }
}
