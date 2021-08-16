/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4286936 8143100
 * @summary Unit test for server-socket channels
 * @library ..
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;


public class Basic {

    static PrintStream log = System.err;

    static class Server
        extends TestThread
    {
        ServerSocketChannel ssc;
        boolean block;

        Server(ServerSocketChannel ssc, boolean block) {
            super("Server", Basic.log);
            this.ssc = ssc;
            this.block = block;
        }

        void go() throws Exception {
            log.println("Server: Listening "
                        + (block ? "(blocking)" : "(non-blocking)"));
            if (!block)
                ssc.configureBlocking(false);
            log.println("  " + ssc);
            //log.println("  " + ssc.options());
            SocketChannel sc = null;
            for (;;) {
                sc = ssc.accept();
                if (sc != null) {
                    break;
                }
                log.println("Server: Sleeping...");
                Thread.sleep(50);
            }
            log.println("Server: Accepted " + sc);
            ByteBuffer bb = ByteBuffer.allocateDirect(100);
            if (sc.read(bb) != 1)
                throw new Exception("Read failed");
            bb.flip();
            byte b = bb.get();
            log.println("Server: Read " + b + ", writing " + (b + 1));
            bb.clear();
            bb.put((byte)43);
            bb.flip();
            if (sc.write(bb) != 1)
                throw new Exception("Write failed");
            sc.close();
            ssc.close();
            log.println("Server: Finished");
        }

    }

    static class Client
        extends TestThread
    {
        int port;
        boolean dally;

        Client(int port, boolean block) {
            super("Client", Basic.log);
            this.port = port;
            this.dally = !block;
        }

        public void go() throws Exception {
            if (dally)
                Thread.sleep(200);
            InetSocketAddress isa
                = new InetSocketAddress(InetAddress.getLocalHost(), port);
            log.println("Client: Connecting to " + isa);
            SocketChannel sc = SocketChannel.open();
            sc.connect(isa);
            log.println("Client: Connected");
            ByteBuffer bb = ByteBuffer.allocateDirect(512);
            bb.put((byte)42).flip();
            log.println("Client: Writing " + bb.get(0));
            if (sc.write(bb) != 1)
                throw new Exception("Write failed");
            bb.clear();
            if (sc.read(bb) != 1)
                throw new Exception("Read failed");
            bb.flip();
            if (bb.get() != 43)
                throw new Exception("Read " + bb.get(bb.position() - 1));
            log.println("Client: Read " + bb.get(0));
            sc.close();
            log.println("Client: Finished");
        }

    }

    static void test(boolean block) throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.socket().setReuseAddress(true);
        int port = TestUtil.bind(ssc);
        Server server = new Server(ssc, block);
        Client client = new Client(port, block);
        server.start();
        client.start();
        if ((server.finish(0) & client.finish(0)) == 0)
            throw new Exception("Failure");
        log.println();
    }

    public static void main(String[] args) throws Exception {
        log.println();
        test(true);
        test(false);
    }

}
