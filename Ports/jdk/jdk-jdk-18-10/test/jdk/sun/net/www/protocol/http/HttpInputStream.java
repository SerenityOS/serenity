/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4937598
 * @library /test/lib
 * @summary http://www.clipstream.com video does not play; read() problem
 */


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

import jdk.test.lib.net.URIBuilder;

public class HttpInputStream {

    private static final int CONTENT_LENGTH = 20;

    static class Server implements AutoCloseable, Runnable {

        final ServerSocket serverSocket;
        static final byte[] requestEnd = new byte[]{'\r', '\n', '\r', '\n'};
        static final int TIMEOUT = 10 * 1000;

        Server() throws IOException {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(
                    InetAddress.getLoopbackAddress(), 0));
        }

        void readOneRequest(InputStream is) throws IOException {
            int requestEndCount = 0, r;
            while ((r = is.read()) != -1) {
                if (r == requestEnd[requestEndCount]) {
                    requestEndCount++;
                    if (requestEndCount == 4) {
                        break;
                    }
                } else {
                    requestEndCount = 0;
                }
            }
        }

        @Override
        public void run() {
            try (Socket s = serverSocket.accept()) {
                s.setSoTimeout(TIMEOUT);
                readOneRequest(s.getInputStream());
                try (OutputStream os =
                             s.getOutputStream()) {
                    os.write("HTTP/1.1 200 OK".getBytes());
                    os.write(("Content-Length: " + CONTENT_LENGTH).getBytes());
                    os.write("\r\n\r\n".getBytes());
                    for (int i = 0; i < CONTENT_LENGTH; i++) {
                        os.write(0xff);
                    }
                    os.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void close() throws IOException {
            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }
        }

        public int getPort() {
            return serverSocket.getLocalPort();
        }
    }


    private static int read(InputStream is) throws IOException {
        int len = 0;
        while (is.read() != -1) {
            len++;
        }
        return len;
    }

    public static void main(String args[]) throws IOException {
        try (Server server = new Server()) {
            (new Thread(server)).start();
            URL url = URIBuilder.newBuilder()
                    .scheme("http")
                    .loopback()
                    .port(server.getPort())
                    .path("/anything")
                    .toURLUnchecked();
            try (InputStream is = url.openConnection().getInputStream()) {
                if (read(is) != CONTENT_LENGTH) {
                    throw new RuntimeException("HttpInputStream.read() failed with 0xff");
                }
            }
        }
    }
}
