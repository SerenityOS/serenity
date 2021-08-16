/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * library /test/lib /
 * build jdk.test.lib.net.SimpleSSLContext ProxyServer
 * compile ../../../com/sun/net/httpserver/LogFilter.java
 * compile ../../../com/sun/net/httpserver/EchoHandler.java
 * compile ../../../com/sun/net/httpserver/FileServerHandler.java
 */
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import jdk.test.lib.net.SimpleSSLContext;

public class LightWeightHttpServer {

    static SSLContext ctx;
    static HttpServer httpServer;
    static HttpsServer httpsServer;
    static ExecutorService executor;
    static int port;
    static int httpsport;
    static String httproot;
    static String httpsroot;
    static ProxyServer proxy;
    static int proxyPort;
    static RedirectErrorHandler redirectErrorHandler, redirectErrorHandlerSecure;
    static RedirectHandler redirectHandler, redirectHandlerSecure;
    static DelayHandler delayHandler;
    static final String midSizedFilename = "/files/notsobigfile.txt";
    static final String smallFilename = "/files/smallfile.txt";
    static Path midSizedFile;
    static Path smallFile;
    static String fileroot;

    public static void initServer() throws IOException {

        Logger logger = Logger.getLogger("com.sun.net.httpserver");
        ConsoleHandler ch = new ConsoleHandler();
        logger.setLevel(Level.ALL);
        ch.setLevel(Level.ALL);
        logger.addHandler(ch);

        String root = System.getProperty("test.src", ".") + "/docs";
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        httpServer = HttpServer.create(addr, 0);
        if (httpServer instanceof HttpsServer) {
            throw new RuntimeException("should not be httpsserver");
        }
        httpsServer = HttpsServer.create(addr, 0);
        HttpHandler h = new FileServerHandler(root);

        HttpContext c1 = httpServer.createContext("/files", h);
        HttpContext c2 = httpsServer.createContext("/files", h);
        HttpContext c3 = httpServer.createContext("/echo", new EchoHandler());
        redirectHandler = new RedirectHandler("/redirect");
        redirectHandlerSecure = new RedirectHandler("/redirect");
        HttpContext c4 = httpServer.createContext("/redirect", redirectHandler);
        HttpContext c41 = httpsServer.createContext("/redirect", redirectHandlerSecure);
        HttpContext c5 = httpsServer.createContext("/echo", new EchoHandler());
        HttpContext c6 = httpServer.createContext("/keepalive", new KeepAliveHandler());
        redirectErrorHandler = new RedirectErrorHandler("/redirecterror");
        redirectErrorHandlerSecure = new RedirectErrorHandler("/redirecterror");
        HttpContext c7 = httpServer.createContext("/redirecterror", redirectErrorHandler);
        HttpContext c71 = httpsServer.createContext("/redirecterror", redirectErrorHandlerSecure);
        delayHandler = new DelayHandler();
        HttpContext c8 = httpServer.createContext("/delay", delayHandler);
        HttpContext c81 = httpsServer.createContext("/delay", delayHandler);

        executor = Executors.newCachedThreadPool();
        httpServer.setExecutor(executor);
        httpsServer.setExecutor(executor);
        ctx = new SimpleSSLContext().get();
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(ctx));
        httpServer.start();
        httpsServer.start();

        port = httpServer.getAddress().getPort();
        System.out.println("HTTP server port = " + port);
        httpsport = httpsServer.getAddress().getPort();
        System.out.println("HTTPS server port = " + httpsport);
        httproot = "http://localhost:" + port + "/";
        httpsroot = "https://localhost:" + httpsport + "/";

        proxy = new ProxyServer(0, false);
        proxyPort = proxy.getPort();
        System.out.println("Proxy port = " + proxyPort);
    }

    public static void stop() throws IOException {
        if (httpServer != null) {
            httpServer.stop(0);
        }
        if (httpsServer != null) {
            httpsServer.stop(0);
        }
        if (proxy != null) {
            proxy.close();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    static class RedirectErrorHandler implements HttpHandler {

        String root;
        volatile int count = 1;

        RedirectErrorHandler(String root) {
            this.root = root;
        }

        synchronized int count() {
            return count;
        }

        synchronized void increment() {
            count++;
        }

        @Override
        public synchronized void handle(HttpExchange t)
                throws IOException {
            byte[] buf = new byte[2048];
            try (InputStream is = t.getRequestBody()) {
                while (is.read(buf) != -1) ;
            }

            Headers map = t.getResponseHeaders();
            String redirect = root + "/foo/" + Integer.toString(count);
            increment();
            map.add("Location", redirect);
            t.sendResponseHeaders(301, -1);
            t.close();
        }
    }

    static class RedirectHandler implements HttpHandler {

        String root;
        volatile int count = 0;

        RedirectHandler(String root) {
            this.root = root;
        }

        @Override
        public synchronized void handle(HttpExchange t)
                throws IOException {
            byte[] buf = new byte[2048];
            try (InputStream is = t.getRequestBody()) {
                while (is.read(buf) != -1) ;
            }

            Headers map = t.getResponseHeaders();

            if (count++ < 1) {
                map.add("Location", root + "/foo/" + count);
            } else {
                map.add("Location", SmokeTest.midSizedFilename);
            }
            t.sendResponseHeaders(301, -1);
            t.close();
        }

        int count() {
            return count;
        }

        void reset() {
            count = 0;
        }
    }

    static class KeepAliveHandler implements HttpHandler {

        volatile int counter = 0;
        HashSet<Integer> portSet = new HashSet<>();
        volatile int[] ports = new int[4];

        void sleep(int n) {
            try {
                Thread.sleep(n);
            } catch (InterruptedException e) {
            }
        }

        @Override
        public synchronized void handle(HttpExchange t)
                throws IOException {
            int remotePort = t.getRemoteAddress().getPort();
            String result = "OK";

            int n = counter++;
            /// First test
            if (n < 4) {
                ports[n] = remotePort;
            }
            if (n == 3) {
                // check all values in ports[] are the same
                if (ports[0] != ports[1] || ports[2] != ports[3]
                        || ports[0] != ports[2]) {
                    result = "Error " + Integer.toString(n);
                    System.out.println(result);
                }
            }
            // Second test
            if (n >= 4 && n < 8) {
                // delay to ensure ports are different
                sleep(500);
                ports[n - 4] = remotePort;
            }
            if (n == 7) {
                // should be all different
                if (ports[0] == ports[1] || ports[2] == ports[3]
                        || ports[0] == ports[2]) {
                    result = "Error " + Integer.toString(n);
                    System.out.println(result);
                    System.out.printf("Ports: %d, %d, %d, %d\n",
                                      ports[0], ports[1], ports[2], ports[3]);
                }
                // setup for third test
                for (int i = 0; i < 4; i++) {
                    portSet.add(ports[i]);
                }
            }
            // Third test
            if (n > 7) {
                // just check that port is one of the ones in portSet
                if (!portSet.contains(remotePort)) {
                    System.out.println("UNEXPECTED REMOTE PORT " + remotePort);
                    result = "Error " + Integer.toString(n);
                    System.out.println(result);
                }
            }
            byte[] buf = new byte[2048];

            try (InputStream is = t.getRequestBody()) {
                while (is.read(buf) != -1) ;
            }
            t.sendResponseHeaders(200, result.length());
            OutputStream o = t.getResponseBody();
            o.write(result.getBytes("US-ASCII"));
            t.close();
        }
    }

    static class DelayHandler implements HttpHandler {

        CyclicBarrier bar1 = new CyclicBarrier(2);
        CyclicBarrier bar2 = new CyclicBarrier(2);
        CyclicBarrier bar3 = new CyclicBarrier(2);

        CyclicBarrier barrier1() {
            return bar1;
        }

        CyclicBarrier barrier2() {
            return bar2;
        }

        @Override
        public synchronized void handle(HttpExchange he) throws IOException {
            try(InputStream is = he.getRequestBody()) {
                is.readAllBytes();
                bar1.await();
                bar2.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new IOException(e);
            }
            he.sendResponseHeaders(200, -1); // will probably fail
            he.close();
        }
    }
}
