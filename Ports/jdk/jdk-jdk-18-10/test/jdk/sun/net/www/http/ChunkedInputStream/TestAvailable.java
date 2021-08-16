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

/*
 * @test
 * @bug 6446990
 * @modules jdk.httpserver
 * @library /test/lib
 * @run main/othervm TestAvailable
 * @summary HttpURLConnection#available() reads more and more data into memory
 */

import java.net.*;
import java.util.*;
import java.io.*;
import com.sun.net.httpserver.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import jdk.test.lib.net.URIBuilder;

public class TestAvailable
{
    com.sun.net.httpserver.HttpServer httpServer;
    ExecutorService executorService;

    public static void main(String[] args)
    {
        new TestAvailable();
    }

    public TestAvailable()
    {
        try {
            startHttpServer();
            doClient();
        } catch (IOException ioe) {
            System.err.println(ioe);
        }
    }

    void doClient() {
        try {
            InetSocketAddress address = httpServer.getAddress();

            URL url = URIBuilder.newBuilder()
                      .scheme("http")
                      .host(address.getAddress())
                      .port(address.getPort())
                      .path("/testAvailable/")
                      .toURLUnchecked();

            HttpURLConnection uc = (HttpURLConnection)url.openConnection();

            uc.setDoOutput(true);
            uc.setRequestMethod("POST");
            uc.setChunkedStreamingMode(0);
            OutputStream os = uc.getOutputStream();
            for (int i=0; i< (128 * 1024); i++)
                os.write('X');
            os.close();

            InputStream is = uc.getInputStream();
            int avail = 0;
            while (avail == 0) {
                try { Thread.sleep(2000); } catch (Exception e) {}
                avail = is.available();
            }

            try { Thread.sleep(2000); } catch (Exception e) {}
            int nextAvail =  is.available();

            is.close();

            if (nextAvail > avail) {
                throw new RuntimeException
                        ("Failed: calling available multiple times should not return more data");
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            httpServer.stop(1);
            executorService.shutdown();
        }


    }

     /**
     * Http Server
     */
    public void startHttpServer() throws IOException {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        InetSocketAddress sockaddr = new InetSocketAddress(loopback, 0);
        httpServer = com.sun.net.httpserver.HttpServer.create(sockaddr, 0);

        // create HttpServer context
        HttpContext ctx = httpServer.createContext("/testAvailable/", new MyHandler());

        executorService = Executors.newCachedThreadPool();
        httpServer.setExecutor(executorService);
        httpServer.start();
    }

    class MyHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            InputStream is = t.getRequestBody();
            byte[] ba = new byte[1024];
            while (is.read(ba) != -1);
            is.close();

            t.sendResponseHeaders(200, 0);

            OutputStream os = t.getResponseBody();
            for (int i=0; i< (128 * 1024); i++)
                os.write('X');
            os.close();

            t.close();
        }
    }
}
