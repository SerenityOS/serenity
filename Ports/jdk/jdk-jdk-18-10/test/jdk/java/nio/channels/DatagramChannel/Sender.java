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
 * @bug 4669040 8130394
 * @summary Test DatagramChannel subsequent receives with no datagram ready
 * @author Mike McCloskey
 */

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;

public class Sender {

    static PrintStream log = System.err;
    static volatile SocketAddress clientISA = null;

    public static void main(String[] args) throws Exception {
        test();
    }

    static void test() throws Exception {
        Server server = new Server();
        Client client = new Client(server.port());

        Thread serverThread = new Thread(server);
        serverThread.start();

        Thread clientThread = new Thread(client);
        clientThread.start();

        serverThread.join();
        clientThread.join();

        server.throwException();
        client.throwException();
    }

    public static class Client implements Runnable {
        final int port;
        Exception e = null;

        Client(int port) {
            this.port = port;
        }

        void throwException() throws Exception {
            if (e != null)
                throw e;
        }

        public void run() {
            try {
                DatagramChannel dc = DatagramChannel.open();
                ByteBuffer bb = ByteBuffer.allocateDirect(12);
                bb.order(ByteOrder.BIG_ENDIAN);
                bb.putInt(1).putLong(1);
                bb.flip();
                InetAddress address = InetAddress.getLocalHost();
                InetSocketAddress isa = new InetSocketAddress(address, port);
                dc.connect(isa);
                clientISA = dc.getLocalAddress();
                dc.write(bb);
            } catch (Exception ex) {
                e = ex;
            }
        }
    }

    public static class Server implements Runnable {
        final DatagramChannel dc;
        Exception e = null;

        Server() throws IOException {
            dc = DatagramChannel.open().bind(new InetSocketAddress(0));
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

        public void run() {
            SocketAddress sa = null;


            try {
                ByteBuffer bb = ByteBuffer.allocateDirect(12);
                bb.clear();
                // Get the one valid datagram
                dc.configureBlocking(false);
                while (sa == null) {
                    sa = dc.receive(bb);
                    if (sa != null && clientISA != null && !clientISA.equals(sa)) {
                        log.println("Ignore a possible stray diagram from " + sa);
                        sa = null;
                    }
                }
                showBuffer("Received:", bb);
                sa = null;
                for (int i=0; i<100; i++) {
                    bb.clear();
                    sa = dc.receive(bb);
                    if (sa != null)
                        throw new RuntimeException("Test failed");
                }
                dc.close();
            } catch (Exception ex) {
                e = ex;
            }
        }
    }

}
