/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4361492
 * @summary HTTPUrlConnection does not receive binary data correctly
 * @library /test/lib
 * @run main/timeout=20 ResendPostBody
 */

import java.io.*;
import java.net.*;

import jdk.test.lib.net.URIBuilder;

/*
 * This test does the following:
 * 1. client opens HTTP connection to server
 * 2. client sends POST with a body containing "ZZZ"
 * 3. server waits for POST and closes connection without replying
 * 4. client should re-open the connection and re-send the POST
 * 5. <bug>The client forgets to re-send the body with the POST
 *    The server hangs waiting for the body</bug>
 *
 *    <bugfixed>The client sends the body. The server reads it and the
 *     test terminates normally </bugfixed>
 */

public class ResendPostBody {

    static class Server extends Thread {

        private InputStream in;
        private OutputStream out;
        private Socket sock;
        private StringBuffer response;
        private ServerSocket server;

        Server(ServerSocket s) throws IOException {
            server = s;
        }

        void waitFor(String s) throws IOException {
            byte[] w = s.getBytes();
            for (int c = 0; c < w.length; c++) {
                byte expected = w[c];
                int b = in.read();
                if (b == -1) {
                    acceptConn();
                }
                if ((byte) b != expected) {
                    c = 0;
                }
            }
        }

        private boolean done = false;

        public synchronized boolean finished() {
            return done;
        }

        public synchronized void setFinished(boolean b) throws IOException {
            done = b;
            this.closeConn();
            server.close();
        }

        void acceptConn() throws IOException {
            sock = server.accept();
            in = sock.getInputStream();
            out = sock.getOutputStream();
        }

        void closeConn() throws IOException {
            in.close();
            out.close();
            sock.close();
        }

        public void run() {
            try {
                response = new StringBuffer(1024);
                acceptConn();
                waitFor("POST");
                waitFor("ZZZ");
                Thread.sleep(500);
                sock.close();
                acceptConn();
                waitFor("POST");
                waitFor("ZZZ");
                response.append("HTTP/1.1 200 OK\r\n");
                response.append("Server: Microsoft-IIS/5.0");
                response.append("Date: Wed, 26 Jul 2000 14:17:04 GMT\r\n\r\n");
                out.write(response.toString().getBytes());
                out.flush();
                while (!finished()) {
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                if (!done) {
                    System.err.println("Server Exception: " + e);
                }
            } finally {
                try {
                    closeConn();
                } catch (IOException ioe) {
                    if (!done) {
                        ioe.printStackTrace();
                    }
                }
            }
        }
    }

    private ServerSocket ss;
    private Server server;

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("-i")) {
            System.out.println("Press return when ready");
            System.in.read();
            System.out.println("Done");
        }
        ResendPostBody t = new ResendPostBody();
        t.execute();
    }

    public void execute() throws Exception {
        byte b[] = "X=ABCDEFGHZZZ".getBytes();

        ss = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
        server = new Server(ss);
        server.start();

        /* Get the URL */
        URL url = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(ss.getLocalPort())
                .path("/test")
                .toURL();
        HttpURLConnection conURL = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);

        conURL.setDoOutput(true);
        conURL.setDoInput(true);
        conURL.setAllowUserInteraction(false);
        conURL.setUseCaches(false);
        conURL.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conURL.setRequestProperty("Content-Length", "" + b.length);
        conURL.setRequestProperty("Connection", "Close");

        /* POST some data */
        DataOutputStream OutStream = new DataOutputStream(conURL.getOutputStream());
        OutStream.write(b, 0, b.length);
        OutStream.flush();
        OutStream.close();

        /* Read the response */
        int resp = conURL.getResponseCode();

        server.setFinished(true);    // Set finished and close ServerSocket
        server.join();            // Join server thread

        if (resp != 200)
            throw new RuntimeException("Response code was not 200: " + resp);
    }
}
