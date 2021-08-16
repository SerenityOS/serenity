/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8190793
 * @summary Httpserver does not detect truncated request body
 */

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Send two POST requests to the server which are both trucated
 * and socket closed. Server needs to detect this and throw an IOException
 * in getRequestBody().read(). Two variants for fixed length and chunked.
 */
public class TruncatedRequestBody {
    static volatile boolean error = false;

    static CountDownLatch latch = new CountDownLatch(2);

    static class Handler implements HttpHandler {

        @Override
        public void handle(HttpExchange exch) throws IOException {
            InputStream is = exch.getRequestBody();
            int c, count = 0;
            byte[] buf = new byte[128];
            try {
            while ((c=is.read(buf)) > 0)
                count += c;
            } catch (IOException e) {
                System.out.println("Exception caught");
                latch.countDown();
                throw e;
            }
            // shouldn't get to here
            error = true;
            latch.countDown();
            System.out.println("Read " + count + " bytes");
            is.close();
            exch.sendResponseHeaders(200, -1);
        }

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        Logger logger = Logger.getLogger("com.sun.net.httpserver");
        ConsoleHandler h = new ConsoleHandler();
        h.setLevel(Level.ALL);
        logger.setLevel(Level.ALL);
        logger.addHandler(h);

        InetAddress loopback = InetAddress.getLoopbackAddress();
        InetSocketAddress addr = new InetSocketAddress(loopback, 0);
        HttpServer server = HttpServer.create(addr, 10);
        HttpContext ct = server.createContext("/", new Handler());
        ExecutorService ex = Executors.newCachedThreadPool();
        server.setExecutor(ex);
        server.start();

        int port = server.getAddress().getPort();

        // Test 1: fixed length

        Socket sock = new Socket(loopback, port);
        String s1 = "POST /foo HTTP/1.1\r\nContent-length: 200000\r\n"
                + "\r\nfoo bar99";

        OutputStream os = sock.getOutputStream();
        os.write(s1.getBytes(StandardCharsets.ISO_8859_1));
        Thread.sleep(500);

        sock.close();

        // Test 2: chunked

        String s2 = "POST /foo HTTP/1.1\r\nTransfer-encoding: chunked\r\n\r\n" +
                "100\r\nFoo bar";
        sock = new Socket(loopback, port);
        os = sock.getOutputStream();
        os.write(s2.getBytes(StandardCharsets.ISO_8859_1));
        Thread.sleep(500);
        sock.close();
        latch.await();
        server.stop(0);
        ex.shutdownNow();
        if (error)
            throw new RuntimeException("Test failed");
    }
}
