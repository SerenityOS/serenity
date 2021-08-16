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

import com.sun.net.httpserver.*;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.logging.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import javax.net.ssl.*;

public class Server {

    HttpsServer server;
    final ExecutorService exec;
    final int port;

    // certfile: needs to be good or bad, ie. bad contains an otherwise valid
    // cert but whose CN contains a different host. good must be correct

    // assuming the TLS handshake succeeds, the server returns a 200 OK
    // response with a short text string.
    public Server(SSLContext ctx) throws Exception {
        initLogger();
        Configurator cfg = new Configurator(ctx);
        InetSocketAddress addr = new InetSocketAddress(
                InetAddress.getLoopbackAddress(), 0);
        server = HttpsServer.create(addr, 10);
        server.setHttpsConfigurator(cfg);
        server.createContext("/", new MyHandler());
        server.setExecutor((exec = Executors.newCachedThreadPool()));
        port = server.getAddress().getPort();
        System.out.println("Listening on port " + port);
        server.start();
    }

    int getPort() {
        return port;
    }

    void stop() {
        server.stop(1);
        exec.shutdownNow();
    }

    Logger logger;

    void initLogger() {
        logger = Logger.getLogger("com.sun.net.httpserver");
        Handler h = new ConsoleHandler();
        logger.setLevel(Level.ALL);
        h.setLevel(Level.ALL);
        logger.addHandler(h);
    }

    String responseBody = "Greetings from localhost";

    class MyHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange e) throws IOException {
            System.out.println("Server: received " + e.getRequestURI());
            InputStream is = e.getRequestBody();
            byte[] buf = new byte[128];
            while (is.read(buf) != -1);
            is.close();
            e.sendResponseHeaders(200, responseBody.length());
            OutputStream os = e.getResponseBody();
            os.write(responseBody.getBytes("ISO8859_1"));
            os.close();
        }
    }

    class Configurator extends HttpsConfigurator {
        public Configurator(SSLContext ctx) throws Exception {
            super(ctx);
        }

        public void configure(HttpsParameters params) {
            SSLParameters p = getSSLContext().getDefaultSSLParameters();
            for (String cipher : p.getCipherSuites())
                System.out.println("Cipher: " + cipher);
            System.err.println("Params = " + p);
            params.setSSLParameters(p);
        }
    }
}
