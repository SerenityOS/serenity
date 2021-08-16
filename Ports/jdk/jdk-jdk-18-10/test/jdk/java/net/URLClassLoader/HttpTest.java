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
 * @bug 4636331
 * @library /test/lib
 * @summary Check that URLClassLoader doesn't create excessive http
 *          connections
 */
import java.net.*;
import java.io.*;
import java.util.*;

import jdk.test.lib.net.URIBuilder;

public class HttpTest {

    /*
     * Simple http server to service http requests. Auto shutdown
     * if "idle" (no requests) for 10 seconds. Forks worker thread
     * to service persistent connections. Work threads shutdown if
     * "idle" for 5 seconds.
     */
    static class HttpServer implements Runnable {

        private static HttpServer svr = null;
        private static Counters cnts = null;
        private static ServerSocket ss;

        private static Object counterLock = new Object();
        private static int getCount = 0;
        private static int headCount = 0;

        class Worker extends Thread {
            Socket s;
            Worker(Socket s) {
                this.s = s;
            }

            public void run() {
                InputStream in = null;
                try {
                    in = s.getInputStream();
                    for (;;) {

                        // read entire request from client
                        byte b[] = new byte[1024];
                        int n, total=0;

                        // max 5 seconds to wait for new request
                        s.setSoTimeout(5000);
                        try {
                            do {
                                n = in.read(b, total, b.length-total);
                                // max 0.5 seconds between each segment
                                // of request.
                                s.setSoTimeout(500);
                                if (n > 0) total += n;
                            } while (n > 0);
                        } catch (SocketTimeoutException e) { }

                        if (total == 0) {
                            s.close();
                            return;
                        }

                        boolean getRequest = false;
                        if (b[0] == 'G' && b[1] == 'E' && b[2] == 'T')
                            getRequest = true;

                        synchronized (counterLock) {
                            if (getRequest)
                                getCount++;
                            else
                                headCount++;
                        }

                        // response to client
                        PrintStream out = new PrintStream(
                                new BufferedOutputStream(
                                        s.getOutputStream() ));
                        out.print("HTTP/1.1 200 OK\r\n");

                        out.print("Content-Length: 75000\r\n");
                        out.print("\r\n");
                        if (getRequest) {
                            for (int i=0; i<75*1000; i++) {
                                out.write( (byte)'.' );
                            }
                        }
                        out.flush();

                    } // for

                } catch (Exception e) {
                    unexpected(e);
                } finally {
                    if (in != null) { try {in.close(); } catch(IOException e) {unexpected(e);} }
                }
            }
        }

        HttpServer() throws Exception {
            ss = new ServerSocket();
            ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        }

        public void run() {
            try {
                // shutdown if no request in 10 seconds.
                ss.setSoTimeout(10000);
                for (;;) {
                    Socket s = ss.accept();
                    (new Worker(s)).start();
                }
            } catch (Exception e) {
            }
        }

        void unexpected(Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }

        public static HttpServer create() throws Exception {
            if (svr != null)
                return svr;
            cnts = new Counters();
            svr = new HttpServer();
            (new Thread(svr)).start();
            return svr;
        }

        public static void shutdown() throws Exception {
            if (svr != null) {
                ss.close();
                svr = null;
            }
        }

        public int port() {
            return ss.getLocalPort();
        }

        public static class Counters {
            public void reset() {
                synchronized (counterLock) {
                    getCount = 0;
                    headCount = 0;
                }
            }

            public int getCount() {
                synchronized (counterLock) {
                    return getCount;
                }
            }

            public int headCount() {
                synchronized (counterLock) {
                    return headCount;
                }
            }

            public String toString() {
                synchronized (counterLock) {
                    return "GET count: " + getCount + "; " +
                       "HEAD count: " + headCount;
                }
            }
        }

        public Counters counters() {
            return cnts;
        }

    }

    public static void main(String args[]) throws Exception {
        boolean failed = false;

        // create http server
        HttpServer svr = HttpServer.create();

        // create class loader
        URL urls[] = {
                URIBuilder.newBuilder().scheme("http").loopback().port(svr.port())
                        .path("/dir1/").toURL(),
                URIBuilder.newBuilder().scheme("http").loopback().port(svr.port())
                        .path("/dir2/").toURL(),
        };
        URLClassLoader cl = new URLClassLoader(urls);

        // Test 1 - check that getResource does single HEAD request
        svr.counters().reset();
        URL url = cl.getResource("foo.gif");
        System.out.println(svr.counters());

        if (svr.counters().getCount() > 0 ||
            svr.counters().headCount() > 1) {
            failed = true;
        }

        // Test 2 - check that getResourceAsStream does at most
        //          one GET request
        svr.counters().reset();
        InputStream in = cl.getResourceAsStream("foo2.gif");
        in.close();
        System.out.println(svr.counters());
        if (svr.counters().getCount() > 1) {
            failed = true;
        }

        // Test 3 - check that getResources only does HEAD requests
        svr.counters().reset();
        Enumeration e = cl.getResources("foos.gif");
        try {
            for (;;) {
                e.nextElement();
            }
        } catch (NoSuchElementException exc) { }
        System.out.println(svr.counters());
        if (svr.counters().getCount() > 1) {
            failed = true;
        }

        // shutdown http server
        svr.shutdown();

        if (failed) {
            throw new Exception("Excessive http connections established - Test failed");
        }
    }

}
