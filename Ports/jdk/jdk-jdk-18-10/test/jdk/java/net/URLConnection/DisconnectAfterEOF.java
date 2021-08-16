/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4774503
 * @library /test/lib
 * @summary Calling HttpURLConnection's disconnect method after the
 *          response has been received causes havoc with persistent
 *          connections.
 */
import java.net.*;
import java.io.*;
import java.util.*;
import jdk.test.lib.net.URIBuilder;
import static java.net.Proxy.NO_PROXY;

public class DisconnectAfterEOF {

    /*
     * Worker thread to service single connection - can service
     * multiple http requests on same connection.
     */
    static class Worker extends Thread {
        Socket s;

        Worker(Socket s) {
            this.s = s;
        }

        public void run() {
            try {
                InputStream in = s.getInputStream();
                PrintStream out = new PrintStream(
                                        new BufferedOutputStream(
                                                s.getOutputStream() ));
                byte b[] = new byte[1024];
                int n = -1;
                int cl = -1;
                int remaining = -1;
                StringBuffer sb = new StringBuffer();
                boolean close = false;

                boolean inBody = false;
                for (;;) {
                    boolean sendResponse = false;

                    try {
                        n = in.read(b);
                    } catch (IOException ioe) {
                        n = -1;
                    }
                    if (n <= 0) {
                        if (inBody) {
                            System.err.println("ERROR: Client closed before before " +
                                "entire request received.");
                        }
                        return;
                    }

                    // reading entity-body
                    if (inBody) {
                        if (n > remaining) {
                            System.err.println("Receiving more than expected!!!");
                            return;
                        }
                        remaining -= n;

                        if (remaining == 0) {
                            sendResponse = true;
                            n = 0;
                        } else {
                            continue;
                        }
                    }

                    // reading headers
                    for (int i=0; i<n; i++) {
                        char c = (char)b[i];

                        if (c != '\n') {
                            sb.append(c);
                            continue;
                        }


                        // Got end-of-line
                        int len = sb.length();
                        if (len > 0) {
                            if (sb.charAt(len-1) != '\r') {
                                System.err.println("Unexpected CR in header!!");
                                return;
                            }
                        }
                        sb.setLength(len-1);

                        // empty line
                        if (sb.length() == 0) {
                            if (cl < 0) {
                                System.err.println("Content-Length not found!!!");
                                return;
                            }

                            // the surplus is body data
                            int dataRead = n - (i+1);
                            remaining = cl - dataRead;
                            if (remaining > 0) {
                                inBody = true;
                                break;
                            } else {
                                // entire body has been read
                                sendResponse = true;
                            }
                        } else {
                            // non-empty line - check for Content-Length
                            String line = sb.toString().toLowerCase();
                            if (line.startsWith("content-length")) {
                                StringTokenizer st = new StringTokenizer(line, ":");
                                st.nextToken();
                                cl = Integer.parseInt(st.nextToken().trim());
                            }
                            if (line.startsWith("connection")) {
                                StringTokenizer st = new StringTokenizer(line, ":");
                                st.nextToken();
                                if (st.nextToken().trim().equals("close")) {
                                    close =true;
                                }
                            }
                        }
                        sb = new StringBuffer();
                    }


                   if (sendResponse) {
                        // send a large response
                        int rspLen = 32000;

                        out.print("HTTP/1.1 200 OK\r\n");
                        out.print("Content-Length: " + rspLen + "\r\n");
                        out.print("\r\n");

                        if (rspLen > 0)
                            out.write(new byte[rspLen]);

                        out.flush();

                        if (close)
                            return;

                        sendResponse = false;
                        inBody = false;
                        cl = -1;
                   }
                }

            } catch (IOException ioe) {
            } finally {
                try {
                    s.close();
                } catch (Exception e) { }
                System.out.println("+ Worker thread shutdown.");
            }
        }
    }

    /*
     * Server thread to accept connection and create worker threads
     * to service each connection.
     */
    static class Server extends Thread {
        ServerSocket ss;

        Server(ServerSocket ss) {
            this.ss = ss;
        }

        public void run() {
            try {
                for (;;) {
                    Socket s = ss.accept();
                    Worker w = new Worker(s);
                    w.start();
                }

            } catch (IOException ioe) {
            }

            System.out.println("+ Server shutdown.");
        }

        public void shutdown() {
            try {
                ss.close();
            } catch (IOException ioe) { }
        }
    }

    static URLConnection doRequest(String uri) throws IOException {
        URLConnection uc = (new URL(uri)).openConnection(NO_PROXY);
        uc.setDoOutput(true);
        OutputStream out = uc.getOutputStream();
        out.write(new byte[16000]);

        // force the request to be sent
        uc.getInputStream();
        return uc;
    }

    static URLConnection doResponse(URLConnection uc) throws IOException {
        int cl = ((HttpURLConnection)uc).getContentLength();
        byte b[] = new byte[4096];
        int n;
        do {
            n = uc.getInputStream().read(b);
            if (n > 0) cl -= n;
        } while (n > 0);
        if (cl != 0) {
            throw new RuntimeException("ERROR: content-length mismatch");
        }
        return uc;
    }

    public static void main(String args[]) throws Exception {
        // start server
        InetAddress loopback = InetAddress.getLoopbackAddress();
        ServerSocket ss = new ServerSocket();
        ss.bind(new InetSocketAddress(loopback, 0));
        Server svr = new Server(ss);
        svr.start();

        String uri = URIBuilder.newBuilder()
                     .scheme("http")
                     .loopback()
                     .port(ss.getLocalPort())
                     .path("/foo.html")
                     .build().toString();

        /*
         * The following is the test scenario we create here :-
         *
         * 1. We do a http request/response and read the response
         *    to EOF. As it's a persistent connection the idle
         *    connection should go into the keep-alive cache for a
         *    few seconds.
         *
         * 2. We start a second request but don't read the response.
         *    As the request is to the same server we can assume it
         *    (for our implementation anyway) that it will use the
         *    same TCP connection.
         *
         * 3. We "disconnect" the first HttpURLConnection. This
         *    should be no-op because the connection is in use
         *    but another request. However with 1.3.1 and 1.4/1.4.1
         *    this causes the TCP connection for the second request
         *    to be closed.
         *
         */
        URLConnection uc1 = doRequest(uri);
        doResponse(uc1);

        Thread.sleep(2000);

        URLConnection uc2 = doRequest(uri);

        ((HttpURLConnection)uc1).disconnect();

        IOException ioe = null;
        try {
            doResponse(uc2);
        } catch (IOException x) {
            ioe = x;
        }

        ((HttpURLConnection)uc2).disconnect();

        /*
         * Shutdown server as we are done. Worker threads created
         * by the server will shutdown automatically when the
         * client connection closes.
         */
        svr.shutdown();

        if (ioe != null) {
            throw ioe;
        }
    }
}
