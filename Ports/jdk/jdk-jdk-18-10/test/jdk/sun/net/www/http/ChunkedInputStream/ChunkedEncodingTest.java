/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4333920
 * @modules jdk.httpserver
 * @library /test/lib
 * @run main ChunkedEncodingTest
 * @summary ChunkedEncodingTest unit test
 */

import java.io.*;
import java.net.*;
import java.security.*;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import static java.lang.System.out;
import jdk.test.lib.net.URIBuilder;

public class ChunkedEncodingTest{
    private static MessageDigest serverDigest, clientDigest;
    private static volatile byte[] serverMac, clientMac;

    static void client(String u) throws Exception {
        URL url = new URL(u);
        out.println("client opening connection to: " + u);
        URLConnection urlc = url.openConnection();
        DigestInputStream dis =
                new DigestInputStream(urlc.getInputStream(), clientDigest);
        while (dis.read() != -1);
        clientMac = dis.getMessageDigest().digest();
        dis.close();
    }

    public static void test() {
        HttpServer server = null;
        try {
            serverDigest = MessageDigest.getInstance("MD5");
            clientDigest = MessageDigest.getInstance("MD5");
            server = startHttpServer();

            int port = server.getAddress().getPort();
            out.println ("Server listening on port: " + port);
            String url = URIBuilder.newBuilder()
                .scheme("http")
                .host(server.getAddress().getAddress())
                .port(port)
                .path("/chunked/")
                .build().toString();
            client(url);

            if (!MessageDigest.isEqual(clientMac, serverMac)) {
                throw new RuntimeException(
                 "Data received is NOT equal to the data sent");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (server != null)
                server.stop(0);
        }
    }

    public static void main(String[] args) {
        test();
    }

    /**
     * Http Server
     */
    static HttpServer startHttpServer() throws IOException {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(loopback, 0), 0);
        HttpHandler httpHandler = new SimpleHandler();
        httpServer.createContext("/chunked/", httpHandler);
        httpServer.start();
        return httpServer;
    }

    static class SimpleHandler implements HttpHandler {
        static byte[] baMessage;
        final static int CHUNK_SIZE = 8 * 1024;
        final static int MESSAGE_LENGTH = 52 * CHUNK_SIZE;

        static {
            baMessage = new byte[MESSAGE_LENGTH];
            for (int i=0; i<MESSAGE_LENGTH; i++)
                baMessage[i] = (byte)i;
        }

        @Override
        public void handle(HttpExchange t) throws IOException {
            InputStream is = t.getRequestBody();
            while (is.read() != -1);
            is.close();

            t.sendResponseHeaders (200, 0);
            OutputStream os = t.getResponseBody();
            DigestOutputStream dos = new DigestOutputStream(os, serverDigest);

            int offset = 0;
            for (int i=0; i<52; i++) {
                dos.write(baMessage, offset, CHUNK_SIZE);
                offset += CHUNK_SIZE;
            }
            serverMac = serverDigest.digest();
            os.close();
            t.close();
        }
    }
}
