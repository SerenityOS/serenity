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
 * @bug 6472250
 * @modules jdk.httpserver
 * @run main/othervm StreamingOutputStream
 * @summary HttpURLConnection.getOutputStream streaming mode bug when called multiple times
 */

import java.net.*;
import java.io.*;
import com.sun.net.httpserver.*;

/*
 * Calling HttpURLConnection.getOutputStream multiple times when streaming
 * should not create a new OutputStream each time.
 */

public class StreamingOutputStream
{
    com.sun.net.httpserver.HttpServer httpServer;

    public static void main(String[] args) {
        new StreamingOutputStream();
    }

    public StreamingOutputStream() {
         try {
            startHttpServer();
            doClient();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    void doClient() {
        try {
            InetSocketAddress address = httpServer.getAddress();

            URL url = new URL("http://" + address.getHostName() + ":" + address.getPort() + "/test/");
            HttpURLConnection uc = (HttpURLConnection)url.openConnection(Proxy.NO_PROXY);

            uc.setDoOutput(true);
            uc.setFixedLengthStreamingMode(1);
            OutputStream os1 = uc.getOutputStream();
            OutputStream os2 = uc.getOutputStream();

            if (os1 != os2)
                throw new RuntimeException("Failed: OutputStreams should reference the same object");

            os2.write('b');
            os2.close();

            int resp = uc.getResponseCode();
            if (resp != 200)
                throw new RuntimeException("Failed: Server should return 200 OK");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            httpServer.stop(1);
        }
    }
    /**
     * Http Server
     */
    void startHttpServer() throws IOException {
        InetAddress address = InetAddress.getLocalHost();
        if (!InetAddress.getByName(address.getHostName()).equals(address)) {
            // if this happens then we should possibly change the client
            // side to use the address literal in its URL instead of
            // the host name.
            throw new IOException(address.getHostName()
                                  + " resolves to "
                                  + InetAddress.getByName(address.getHostName())
                                  + " not to "
                                  + address + ": check host configuration.");
        }
        httpServer = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(address, 0), 0);
        HttpContext ctx = httpServer.createContext("/test/", new MyHandler());
        httpServer.start();
    }

    class MyHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            InputStream is = t.getRequestBody();
            while(is.read() != -1);
            is.close();

            t.sendResponseHeaders(200, -1);
            t.close();
        }
    }
}
