/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8004863
 * @modules jdk.httpserver
 * @summary Checks for proper close code in KeepAliveStream
 * @library /test/lib
 * @run main InfiniteLoop
 * @run main/othervm -Djava.net.preferIPv6Addresses=true InfiniteLoop
 */

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.concurrent.Phaser;

import jdk.test.lib.net.URIBuilder;

// Racey test, will not always fail, but if it does then we have a problem.

public class InfiniteLoop {

    public static void main(String[] args) throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        HttpServer server = HttpServer.create(new InetSocketAddress(loopback, 0), 0);
        server.createContext("/test/InfiniteLoop", new RespHandler());
        server.start();
        try {
            InetSocketAddress address = server.getAddress();
            URL url = URIBuilder.newBuilder()
                      .scheme("http")
                      .host(server.getAddress().getAddress())
                      .port(server.getAddress().getPort())
                      .path("/test/InfiniteLoop")
                      .toURL();
            final Phaser phaser = new Phaser(2);
            for (int i=0; i<10; i++) {
                HttpURLConnection uc = (HttpURLConnection)
                    url.openConnection(Proxy.NO_PROXY);
                final InputStream is = uc.getInputStream();
                final Thread thread = new Thread() {
                    public void run() {
                        try {
                            phaser.arriveAndAwaitAdvance();
                            while (is.read() != -1)
                                Thread.sleep(50);
                        } catch (Exception x) { x.printStackTrace(); }
                    }};
                thread.start();
                phaser.arriveAndAwaitAdvance();
                is.close();
                System.out.println("returned from close");
                thread.join();
            }
        } finally {
            server.stop(0);
        }
    }

    static class RespHandler implements HttpHandler {
        static final int RESP_LENGTH = 32 * 1024;
        @Override
        public void handle(HttpExchange t) throws IOException {
            InputStream is  = t.getRequestBody();
            byte[] ba = new byte[8192];
            while(is.read(ba) != -1);

            t.sendResponseHeaders(200, RESP_LENGTH);
            try (OutputStream os = t.getResponseBody()) {
                os.write(new byte[RESP_LENGTH]);
            }
            t.close();
        }
    }
}
