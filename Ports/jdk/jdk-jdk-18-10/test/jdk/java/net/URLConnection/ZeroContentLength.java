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
 * @bug 4507412
 * @bug 4506998
 * @summary Check that a 304 "Not-Modified" response from a server
 *          doesn't cause http client to close a keep-alive
 *          connection.
 *          Check that a content-length of 0 results in an
 *          empty input stream.
 * @library /test/lib
 * @run main ZeroContentLength
 * @run main/othervm -Djava.net.preferIPv6Addresses=true ZeroContentLength
 */

import java.net.*;
import java.io.*;
import jdk.test.lib.net.URIBuilder;

public class ZeroContentLength {

    /*
     * Is debugging enabled - start with -d to enable.
     */
    static boolean debug = false;

    static void debug(String msg) {
        if (debug)
            System.out.println(msg);
    }

    /*
     * The response string and content-length that
     * the server should return;
     */
    static String response;
    static int contentLength;

    static synchronized void setResponse(String rsp, int cl) {
        response = rsp;
        contentLength = cl;
    }

    static synchronized String getResponse() {
        return response;
    }

    static synchronized int getContentLength() {
        return contentLength;
    }

    /*
     * Worker thread to service single connection - can service
     * multiple http requests on same connection.
     */
    class Worker extends Thread {
        Socket s;
        int id;

        Worker(Socket s, int id) {
            this.s = s;
            this.id = id;
        }

        final int CR = '\r';
        final int LF = '\n';

        public void run() {
            try {

                s.setSoTimeout(2000);
                int max = 20; // there should only be 20 connections
                InputStream in = new BufferedInputStream(s.getInputStream());

                for (;;) {
                    // read entire request from client, until CR LF CR LF
                    int c, total=0;

                    try {
                        while ((c = in.read()) > 0) {
                            total++;
                            if (c == CR) {
                                if ((c = in.read()) > 0) {
                                    total++;
                                    if (c == LF) {
                                        if ((c = in.read()) > 0) {
                                            total++;
                                            if (c == CR) {
                                                if ((c = in.read()) > 0) {
                                                    total++;
                                                    if (c == LF) {
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                        }
                    } catch (SocketTimeoutException e) {}

                    debug("worker " + id +
                        ": Read request from client " +
                        "(" + total + " bytes).");

                    if (total == 0) {
                        debug("worker: " + id + ": Shutdown");
                        return;
                    }

                    // response to client
                    PrintStream out = new PrintStream(
                                        new BufferedOutputStream(
                                                s.getOutputStream() ));

                    out.print("HTTP/1.1 " + getResponse() + "\r\n");
                    int clen = getContentLength();
                    if (clen >= 0) {
                        out.print("Content-Length: " + clen +
                                    "\r\n");
                    }
                    out.print("\r\n");
                    for (int i=0; i<clen; i++) {
                        out.write( (byte)'.' );
                    }
                    out.flush();

                    debug("worked " + id +
                        ": Sent response to client, length: " + clen);

                    if (--max == 0) {
                        s.close();
                        return;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    s.close();
                } catch (Exception e) { }
            }
        }
    }

    /*
     * Server thread to accept connection and create worker threads
     * to service each connection.
     */
    class Server extends Thread {
        ServerSocket ss;
        int connectionCount;
        boolean shutdown = false;

        Server(ServerSocket ss) {
            this.ss = ss;
        }

        public synchronized int connectionCount() {
            return connectionCount;
        }

        public synchronized void shutdown() {
            shutdown = true;
        }

        public void run() {
            try {
                ss.setSoTimeout(2000);

                for (;;) {
                    Socket s;
                    try {
                        debug("server: Waiting for connections");
                        s = ss.accept();
                    } catch (SocketTimeoutException te) {
                        synchronized (this) {
                            if (shutdown) {
                                debug("server: Shuting down.");
                                return;
                            }
                        }
                        continue;
                    }

                    int id;
                    synchronized (this) {
                        id = connectionCount++;
                    }

                    Worker w = new Worker(s, id);
                    w.start();
                    debug("server: Started worker " + id);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    ss.close();
                } catch (Exception e) { }
            }
        }
    }

    /*
     * Make a single http request and return the content length
     * received. Also do sanity check to ensure that the
     * content-length header matches the total received on
     * the input stream.
     */
    int doRequest(String uri) throws Exception {
        URL url = new URL(uri);
        HttpURLConnection http = (HttpURLConnection)url.openConnection(Proxy.NO_PROXY);

        int cl = http.getContentLength();

        InputStream in = http.getInputStream();
        byte b[] = new byte[100];
        int total = 0;
        int n;
        do {
            n = in.read(b);
            if (n > 0) total += n;
        } while (n > 0);
        in.close();

        if (cl >= 0 && total != cl) {
            System.err.println("content-length header indicated: " + cl);
            System.err.println("Actual received: " + total);
            throw new Exception("Content-length didn't match actual received");
        }

        return total;
    }


    /*
     * Send http requests to "server" and check that they all
     * use the same network connection and that the content
     * length corresponds to the content length expected.
     * stream.
     */
    ZeroContentLength() throws Exception {

        /* start the server */
        ServerSocket ss = new ServerSocket();
        ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        Server svr = new Server(ss);
        svr.start();

        String uri = URIBuilder.newBuilder()
                     .scheme("http")
                     .host(ss.getInetAddress())
                     .port(ss.getLocalPort())
                     .path("/foo.html")
                     .build().toString();

        int expectedTotal = 0;
        int actualTotal = 0;

        System.out.println("**********************************");
        System.out.println("200 OK, content-length:1024 ...");
        setResponse("200 OK", 1024);
        for (int i=0; i<5; i++) {
            actualTotal += doRequest(uri);
            expectedTotal += 1024;
        }

        System.out.println("**********************************");
        System.out.println("200 OK, content-length:0 ...");
        setResponse("200 OK", 0);
        for (int i=0; i<5; i++) {
            actualTotal += doRequest(uri);
        }

        System.out.println("**********************************");
        System.out.println("304 Not-Modified, (no content-length) ...");
        setResponse("304 Not-Modifed", -1);
        for (int i=0; i<5; i++) {
            actualTotal += doRequest(uri);
        }

        System.out.println("**********************************");
        System.out.println("204 No-Content, (no content-length) ...");
        setResponse("204 No-Content", -1);
        for (int i=0; i<5; i++) {
            actualTotal += doRequest(uri);
        }

        // shutdown server - we're done.
        svr.shutdown();

        System.out.println("**********************************");

        if (actualTotal == expectedTotal) {
            System.out.println("Passed: Actual total equal to expected total");
        } else {
            throw new Exception("Actual total != Expected total!!!");
        }

        int cnt = svr.connectionCount();
        if (cnt == 1) {
            System.out.println("Passed: Only 1 connection established");
        } else {
            throw new Exception("Test failed: Number of connections " +
                "established: " + cnt + " - see log for details.");
        }
    }

    public static void main(String args[]) throws Exception {

        if (args.length > 0 && args[0].equals("-d")) {
            debug = true;
        }

        new ZeroContentLength();
    }

}
