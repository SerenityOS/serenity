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
 * @bug 5054016
 * @library /test/lib
 * @summary get the failure immediately when writing individual chunks over socket fail
 * @run main CheckError
 * @run main/othervm -Djava.net.preferIPv6Addresses=true CheckError
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import static java.lang.System.out;

import jdk.test.lib.net.URIBuilder;

public class CheckError {

    static int BUFFER_SIZE = 8192; // 8k
    static int TOTAL_BYTES = 1 * 1024 * 1024; // 1M

    public static void main(String[] args) throws Exception {

        HTTPServer server = new HTTPServer();
        server.start();
        int port = server.getPort();
        out.println("Server listening on " + port);


        URL url = URIBuilder.newBuilder()
                  .scheme("http")
                  .host(server.getAddress())
                  .port(port)
                  .toURL();

        HttpURLConnection conn = (HttpURLConnection)url.openConnection(Proxy.NO_PROXY);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setChunkedStreamingMode(1024);

        out.println("sending " + TOTAL_BYTES + " bytes");

        int byteAtOnce;
        int sendingBytes = TOTAL_BYTES;
        byte[] buffer = getBuffer(BUFFER_SIZE);
        try (OutputStream toServer = conn.getOutputStream()) {
            while (sendingBytes > 0) {
                if (sendingBytes > BUFFER_SIZE) {
                    byteAtOnce = BUFFER_SIZE;
                } else {
                    byteAtOnce = sendingBytes;
                }
                toServer.write(buffer, 0, byteAtOnce);
                sendingBytes -= byteAtOnce;
                out.print((TOTAL_BYTES - sendingBytes) + " was sent. ");
                toServer.flush();
                // gives the server thread time to read, and eventually close;
                Thread.sleep(500);
            }
        } catch (IOException expected) {
            // Expected IOException due to server.close()
            out.println("PASSED. Caught expected: " + expected);
            return;
        }

        // Expected IOException not received. FAIL
        throw new RuntimeException("Failed: Expected IOException not received");
    }

    static byte[] getBuffer(int size) {
        byte[] buffer = new byte[size];
        for (int i = 0; i < size; i++)
            buffer[i] = (byte)i;
        return buffer;
    }

    static class HTTPServer extends Thread {

        final ServerSocket serverSocket;

        HTTPServer() throws IOException {
            InetAddress loopback = InetAddress.getLoopbackAddress();
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(loopback, 0));
        }

        int getPort() {
            return serverSocket.getLocalPort();
        }

        InetAddress getAddress() {
            return serverSocket.getInetAddress();
        }

        public void run() {
            try (Socket client = serverSocket.accept()) {

                InputStream in = client.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                do {
                    line = reader.readLine();
                    out.println("Server: " + line);
                } while (line != null && line.length() > 0);

                System.out.println("Server: receiving some data");
                // just read some data, then close the connection
                in.read(new byte[1024]);

                in.close();
                client.close();
                out.println("Server closed socket");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
