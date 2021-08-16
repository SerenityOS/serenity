/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6361557
 * @run main/othervm B6361557
 * @summary  Lightweight HTTP server quickly runs out of file descriptors on Linux
 */

import com.sun.net.httpserver.*;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;

/**
 * The test simply opens 1,000 separate connections
 * and invokes one http request on each. The client does
 * not close any sockets until after they are closed
 * by the server. This verifies the basic ability
 * of the server to manage a reasonable number of connections
 */
public class B6361557 {

    public static boolean error = false;
    static final int NUM = 1000;

    static class Handler implements HttpHandler {
        int invocation = 1;
        public void handle (HttpExchange t)
            throws IOException
        {
            InputStream is = t.getRequestBody();
            Headers map = t.getRequestHeaders();
            Headers rmap = t.getResponseHeaders();
            while (is.read () != -1) ;
            is.close();
            t.sendResponseHeaders (200, -1);
            t.close();
        }
    }

    final static String request = "GET /test/foo.html HTTP/1.1\r\nContent-length: 0\r\n\r\n";
    final static ByteBuffer requestBuf = ByteBuffer.allocate(64).put(request.getBytes());

    public static void main (String[] args) throws Exception {
        Handler handler = new Handler();
        InetAddress loopback = InetAddress.getLoopbackAddress();
        InetSocketAddress addr = new InetSocketAddress (loopback, 0);
        HttpServer server = HttpServer.create (addr, 0);
        HttpContext ctx = server.createContext ("/test", handler);

        ExecutorService executor = Executors.newCachedThreadPool();
        server.setExecutor (executor);
        server.start ();

        InetSocketAddress destaddr = new InetSocketAddress (
                loopback, server.getAddress().getPort()
        );
        System.out.println ("destaddr " + destaddr);

        Selector selector = Selector.open ();
        int requests = 0;
        int responses = 0;
        while (true) {
            // we need to read responses from time to time: slightly
            // increase the timeout with the amount of pending responses
            // to give a chance to the server to reply.
            int selres = selector.select (requests - responses + 1);
            Set<SelectionKey> selkeys = selector.selectedKeys();
            for (SelectionKey key : selkeys) {
                if (key.isReadable()) {
                    SocketChannel chan = (SocketChannel)key.channel();
                    ByteBuffer buf = (ByteBuffer)key.attachment();
                    try {
                        int x = chan.read(buf);
                        if (x == -1 || responseComplete(buf)) {
                            System.out.print("_");
                            key.attach(null);
                            chan.close();
                            responses++;
                        }
                    } catch (IOException e) {
                        System.out.println(e);
                    }
                }
            }
            if (requests < NUM) {
                System.out.print(".");
                SocketChannel schan = SocketChannel.open(destaddr);
                requestBuf.rewind();
                int c = 0;
                while (requestBuf.remaining() > 0) {
                    c += schan.write(requestBuf);
                }
                schan.configureBlocking(false);
                schan.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(100));
                requests++;
            }
            if (responses == NUM) {
                System.out.println ("Finished clients");
                break;
            }
        }
        server.stop (1);
        selector.close();
        executor.shutdown ();

    }

    /* Look for CR LF CR LF */
    static boolean responseComplete(ByteBuffer buf) {
        int pos = buf.position();
        buf.flip();
        byte[] lookingFor = new byte[] {'\r', '\n', '\r', '\n' };
        int lookingForCount = 0;
        while (buf.hasRemaining()) {
            byte b = buf.get();
            if (b == lookingFor[lookingForCount]) {
                lookingForCount++;
                if (lookingForCount == 4) {
                    return true;
                }
            } else {
                lookingForCount = 0;
            }
        }
        buf.position(pos);
        buf.limit(buf.capacity());
        return false;
    }
}
