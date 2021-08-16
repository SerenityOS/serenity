/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6725892
 * @library /test/lib
 * @run main/othervm -Dsun.net.httpserver.maxReqTime=2 Test
 * @run main/othervm -Djava.net.preferIPv6Addresses=true -Dsun.net.httpserver.maxReqTime=2 Test
 * @summary
 */

import com.sun.net.httpserver.*;

import java.util.concurrent.*;
import java.util.logging.*;
import java.io.*;
import java.net.*;
import javax.net.ssl.*;

import jdk.test.lib.net.URIBuilder;

public class Test {

    static HttpServer s1;
    static int port;
    static URL url;
    static final String RESPONSE_BODY = "response";
    static boolean failed = false;

    static class Handler implements HttpHandler {

        public void handle(HttpExchange t)
            throws IOException
        {
            InputStream is = t.getRequestBody();
            InetSocketAddress rem = t.getRemoteAddress();
            System.out.println("Request from: " + rem);
            while (is.read () != -1) ;
            is.close();
            String requrl = t.getRequestURI().toString();
            OutputStream os = t.getResponseBody();
            t.sendResponseHeaders(200, RESPONSE_BODY.length());
            os.write(RESPONSE_BODY.getBytes());
            t.close();
        }
    }

    public static void main(String[] args) throws Exception {

        ExecutorService exec = Executors.newCachedThreadPool();

        InetAddress loopback = InetAddress.getLoopbackAddress();
        try {
            InetSocketAddress addr = new InetSocketAddress(loopback, 0);
            s1 = HttpServer.create(addr, 100);
            HttpHandler h = new Handler();
            HttpContext c1 = s1.createContext("/", h);
            s1.setExecutor(exec);
            s1.start();

            port = s1.getAddress().getPort();
            System.out.println("Server on port " + port);
            url = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(port)
                .path("/foo")
                .toURLUnchecked();
            System.out.println("URL: " + url);
            test1();
            test2();
            test3();
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("FAIL");
            throw new RuntimeException();
        } finally {
            s1.stop(0);
            System.out.println("After Shutdown");
            exec.shutdown();
        }
    }

    // open TCP connection without sending anything. Check server closes it.

    static void test1() throws IOException {
        failed = false;
        Socket s = new Socket(InetAddress.getLoopbackAddress(), port);
        InputStream is = s.getInputStream();
        // server should close connection after 2 seconds. We wait up to 10
        s.setSoTimeout(10000);
        try {
            is.read();
        } catch (SocketTimeoutException e) {
            failed = true;
        }
        s.close();
        if (failed) {
            System.out.println("test1: FAIL");
            throw new RuntimeException();
        } else {
            System.out.println("test1: OK");
        }
    }

    // send request and don't read response. Check server closes connection

    static void test2() throws IOException {
        HttpURLConnection urlc = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        urlc.setReadTimeout(20 * 1000);
        InputStream is = urlc.getInputStream();
        // we won't read response and check if it times out
        // on server. If it timesout at client then there is a problem
        try {
            Thread.sleep(10 * 1000);
            while (is.read() != -1) ;
        } catch (InterruptedException e) {
            System.out.println(e);
            System.out.println("test2: FAIL");
            throw new RuntimeException("unexpected error");
        } catch (SocketTimeoutException e1) {
            System.out.println(e1);
            System.out.println("test2: FAIL");
            throw new RuntimeException("client timedout");
        } finally {
            is.close();
        }
        System.out.println("test2: OK");
    }

    // same as test2, but repeated with multiple connections
    // including a number of valid request/responses

    // Worker: a thread opens a connection to the server in one of three modes.
    // NORMAL - sends a request, waits for response, and checks valid response
    // REQUEST - sends a partial request, and blocks, to see if
    //                  server closes the connection.
    // RESPONSE - sends a request, partially reads response and blocks,
    //                  to see if server closes the connection.

    static class Worker extends Thread {
        CountDownLatch latch;
        Mode mode;

        enum Mode {
            REQUEST,    // block during sending of request
            RESPONSE,   // block during reading of response
            NORMAL      // don't block
        };

        Worker (CountDownLatch latch, Mode mode) {
            this.latch = latch;
            this.mode = mode;
        }

        void fail(String msg) {
            System.out.println(msg);
            failed = true;
        }

        public void run() {
            HttpURLConnection urlc;
            InputStream is = null;

            try {
                urlc = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
                urlc.setReadTimeout(20 * 1000);
                urlc.setDoOutput(true);
            } catch (IOException e) {
                fail("Worker: failed to connect to server");
                latch.countDown();
                return;
            }
            try {
                OutputStream os = urlc.getOutputStream();
                os.write("foo".getBytes());
                if (mode == Mode.REQUEST) {
                    Thread.sleep(3000);
                }
                os.close();
                is = urlc.getInputStream();
                if (mode == Mode.RESPONSE) {
                    Thread.sleep(3000);
                }
                if (!checkResponse(is, RESPONSE_BODY)) {
                    fail("Worker: response");
                }
                is.close();
                return;
            } catch (InterruptedException e0) {
                fail("Worker: timedout");
            } catch (SocketTimeoutException e1) {
                fail("Worker: timedout");
            } catch (IOException e2) {
                switch (mode) {
                  case NORMAL:
                    fail("Worker: " + e2.getMessage());
                    break;
                  case RESPONSE:
                    if (is == null) {
                        fail("Worker: " + e2.getMessage());
                        break;
                    }
                  // default: is ok
                }
            } finally {
                latch.countDown();
            }
        }
    }

    static final int NUM = 20;

    static void test3() throws Exception {
        failed = false;
        CountDownLatch l = new CountDownLatch(NUM*3);
        Worker[] workers = new Worker[NUM*3];
        for (int i=0; i<NUM; i++) {
            workers[i*3] = new Worker(l, Worker.Mode.NORMAL);
            workers[i*3+1] = new Worker(l, Worker.Mode.REQUEST);
            workers[i*3+2] = new Worker(l, Worker.Mode.RESPONSE);
            workers[i*3].start();
            workers[i*3+1].start();
            workers[i*3+2].start();
        }
        l.await();
        for (int i=0; i<NUM*3; i++) {
            workers[i].join();
        }
        if (failed) {
            throw new RuntimeException("test3: failed");
        }
        System.out.println("test3: OK");
    }

    static boolean checkResponse(InputStream is, String resp) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[64];
            int c;
            while ((c=is.read(buf)) != -1) {
                bos.write(buf, 0, c);
            }
            bos.close();
            if (!bos.toString().equals(resp)) {
                System.out.println("Wrong response: " + bos.toString());
                return false;
            }
        } catch (IOException e) {
            System.out.println(e);
            return false;
        }
        return true;
    }
}
