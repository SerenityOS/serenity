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

/**
 * @test
 * @bug 6373555
 * @library /test/lib
 * @summary HTTP Server failing to answer client requests
 * @run main B6373555
 * @run main/othervm -Djava.net.preferIPv6Addresses=true B6373555
 */

import java.net.*;
import java.io.*;
import java.util.*;
import com.sun.net.httpserver.*;
import java.util.concurrent.*;
import jdk.test.lib.net.URIBuilder;

public class B6373555 {

    private static int s_received = 0;
    private static int sent = 0;

    private static int received = 0;
    private static int port;

    private static volatile boolean error = false;
    static HttpServer httpServer;
    static ExecutorService pool, execs;
    static int NUM = 1000;

    public static void main(String[] args) throws Exception {
        try {
            if (args.length > 0) {
                NUM = Integer.parseInt (args[0]);
            }
            execs = Executors.newFixedThreadPool(5);
            httpServer = createHttpServer(execs);
            port = httpServer.getAddress().getPort();
            pool = Executors.newFixedThreadPool(10);
            httpServer.start();
            for (int i=0; i < NUM; i++) {
                pool.execute(new Client());
                if (error) {
                    throw new Exception ("error in test");
                }
            }
            System.out.println("Main thread waiting");
            pool.shutdown();
            long latest = System.currentTimeMillis() + 200 * 1000;
            while (System.currentTimeMillis() < latest) {
                if (pool.awaitTermination(2000L, TimeUnit.MILLISECONDS)) {
                    System.out.println("Main thread done!");
                    return;
                }
                if (error) {
                    throw new Exception ("error in test");
                }
            }
            throw new Exception ("error in test: timed out");
        } finally {
            httpServer.stop(0);
            pool.shutdownNow();
            execs.shutdownNow();
        }
    }

    public static class Client implements Runnable {

        byte[] getBuf () {
            byte[] buf = new byte [5200];
            for (int i=0; i< 5200; i++) {
                buf [i] = (byte)i;
            }
            return buf;
        }

        public void run() {
            try {
                Thread.sleep(10);
                byte[] buf = getBuf();
                URL url = URIBuilder.newBuilder()
                    .scheme("http")
                    .loopback()
                    .port(port)
                    .path("/test")
                    .toURLUnchecked();
                System.out.println("URL: " + url);
                HttpURLConnection con = (HttpURLConnection)url.openConnection(Proxy.NO_PROXY);
                con.setDoOutput(true);
                con.setDoInput(true);
                con.setRequestMethod("POST");
                con.setRequestProperty(
                    "Content-Type",
                    "Multipart/Related; type=\"application/xop+xml\"; boundary=\"----=_Part_0_6251267.1128549570165\"; start-info=\"text/xml\"");
                OutputStream out = con.getOutputStream();
                out.write(buf);
                out.close();
                InputStream in = con.getInputStream();
                byte[] newBuf = readFully(in);
                in.close();
                if (buf.length != newBuf.length) {
                    System.out.println("Doesn't match");
                    error = true;
                }
            }
            catch(Exception e) {
                e.printStackTrace();
                System.out.print (".");
                error = true;
            }
        }
    }

    private static byte[] readFully(InputStream istream) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int num = 0;

        if (istream != null) {
            while ((num = istream.read(buf)) != -1) {
                bout.write(buf, 0, num);
            }
        }
        byte[] ret = bout.toByteArray();
        return ret;
    }


    private static HttpServer createHttpServer(ExecutorService execs)
        throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        InetSocketAddress inetAddress = new InetSocketAddress(loopback, 0);
        HttpServer testServer = HttpServer.create(inetAddress, 15);
        testServer.setExecutor(execs);
        HttpContext context = testServer.createContext("/test");
        context.setHandler(new HttpHandler() {
            public void handle(HttpExchange msg) {
                try {
                    String method = msg.getRequestMethod();
                        if (method.equals("POST")) {
                        InputStream is = msg.getRequestBody();
                            byte[] buf = readFully(is);
                            is.close();
                            writePostReply(msg, buf);
                    } else {
                        System.out.println("****** METHOD not handled ***** "+method);
                            System.out.println("Received="+s_received);
                    }
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
                finally {
                    msg.close();
                }
            }
        }
        );
        return testServer;
    }

    private static void writePostReply(HttpExchange msg, byte[] buf)
        throws Exception {
        msg.getResponseHeaders().add("Content-Type", "text/xml");
        msg.sendResponseHeaders(200, buf.length);
        OutputStream out = msg.getResponseBody();
        out.write(buf);
        out.close();
    }

}
