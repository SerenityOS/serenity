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
 * @bug 4769350 8017779 8191169
 * @modules jdk.httpserver
 * @run main/othervm B4769350 server
 * @run main/othervm B4769350 proxy
 * @run main/othervm -Djava.net.preferIPv6Addresses=true B4769350 server
 * @run main/othervm -Djava.net.preferIPv6Addresses=true B4769350 proxy
 * @summary proxy authentication username and password caching only works in serial case
 * Run in othervm since the test sets system properties that are read by the
 * networking stack and cached when the HTTP handler is invoked, and previous
 * tests may already have invoked the HTTP handler.
 */

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class B4769350 {

    static int count = 0;
    static boolean error = false;

    static void read (InputStream is) throws IOException {
        while (is.read() != -1) {
            //System.out.write (c);
        }
    }

     static class Client extends Thread {
        String authority, path;
        boolean allowerror;

        Client (String authority, String path, boolean allowerror) {
            super("Thread-" + path);
            this.authority = authority;
            this.path = path;
            this.allowerror = allowerror;
        }

        @Override
        public void run () {
            try {
                URI u = new URI ("http", authority, path, null, null);
                URL url = u.toURL();
                URLConnection urlc = url.openConnection();
                try (InputStream is = urlc.getInputStream()) {
                    read (is);
                }
            } catch (URISyntaxException  e) {
                System.out.println (e);
                error = true;
            } catch (IOException e) {
                if (!allowerror) {
                    System.out.println (Thread.currentThread().getName()
                            + " " + e);
                    e.printStackTrace();
                    error = true;
                }
            }
        }
    }

    class Server implements AutoCloseable {
        HttpServer server;
        Executor executor;

        public String getAddress() {
            return server.getAddress().getHostName();
        }

        public void startServer() {
            InetAddress loopback = InetAddress.getLoopbackAddress();
            InetSocketAddress addr = new InetSocketAddress(loopback, 0);

            try {
                server = HttpServer.create(addr, 0);
            } catch (IOException ioe) {
                throw new RuntimeException("Server could not be created");
            }
            executor = Executors.newFixedThreadPool(10);
            server.setExecutor(executor);
            server.createContext("/test/realm1/t1a",
                    new AuthenticationHandlerT1a() );
            server.createContext("/test/realm2/t1b",
                    new AuthenticationHandlerT1b());
            server.createContext("/test/realm1/t1c",
                    new AuthenticationHandlerT1c());
            server.createContext("/test/realm2/t1d",
                    new AuthenticationHandlerT1d());
            server.createContext("/test/realm3/t2a",
                    new AuthenticationHandlerT2a());
            server.createContext("/test/realm3/t2b",
                    new AuthenticationHandlerT2b());
            server.createContext("/test/realm4/t3a",
                    new AuthenticationHandlerT3a());
            server.createContext("/test/realm4/t3b",
                    new AuthenticationHandlerT3bc());
            server.createContext("/test/realm4/t3c",
                    new AuthenticationHandlerT3bc());
            t1Cond1 = new CyclicBarrier(3);
            server.start();
        }

        public int getPort() {
            return server.getAddress().getPort();
        }

        @Override
        public void close() {
            if (executor != null)
                ((ExecutorService)executor).shutdownNow();
            if (server != null)
                server.stop(0);
        }

        /* T1 tests the client by sending 4 requests to 2 different realms
         * in parallel. The client should recognise two pairs of dependent requests
         * and execute the first of each pair in parallel. When they both succeed
         * the second requests should be executed without calling the authenticator.
         * The test succeeds if the authenticator was only called twice.
         */
        class AuthenticationHandlerT1a implements HttpHandler
        {
            volatile int count = -1;

            @Override
            public void handle(HttpExchange exchange) throws IOException {
                count++;
                try {
                    switch(count) {
                        case 0:
                            AuthenticationHandler.errorReply(exchange,
                                    "Basic realm=\"realm1\"");
                            break;
                        case 1:
                            t1Cond1.await();
                            AuthenticationHandler.okReply(exchange);
                            break;
                        default:
                            System.out.println ("Unexpected request");
                    }
                } catch (InterruptedException |
                                 BrokenBarrierException e)
                        {
                            throw new RuntimeException(e);
                        }
            }
        }

        class AuthenticationHandlerT1b implements HttpHandler
        {
            volatile int count = -1;

            @Override
            public void handle(HttpExchange exchange) throws IOException {
                count++;
                try {
                    switch(count) {
                        case 0:
                            AuthenticationHandler.errorReply(exchange,
                                    "Basic realm=\"realm2\"");
                            break;
                        case 1:
                            t1Cond1.await();
                            AuthenticationHandler.okReply(exchange);
                            break;
                        default:
                            System.out.println ("Unexpected request");
                    }
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        class AuthenticationHandlerT1c implements HttpHandler
        {
            volatile int count = -1;

            @Override
            public void handle(HttpExchange exchange) throws IOException {
                count++;
                switch(count) {
                    case 0:
                        AuthenticationHandler.errorReply(exchange,
                                "Basic realm=\"realm1\"");
                        break;
                    case 1:
                        AuthenticationHandler.okReply(exchange);
                        break;
                    default:
                        System.out.println ("Unexpected request");
                }
            }
        }

        class AuthenticationHandlerT1d implements HttpHandler
        {
            volatile int count = -1;

            @Override
            public void handle(HttpExchange exchange) throws IOException {
                count++;
                switch(count) {
                    case 0:
                        AuthenticationHandler.errorReply(exchange,
                                "Basic realm=\"realm2\"");
                        break;
                    case 1:
                        AuthenticationHandler.okReply(exchange);
                        break;
                    default:
                        System.out.println ("Unexpected request");
                }
            }
        }

        /* T2 tests to check that if initial authentication fails, the second will
         * succeed, and the authenticator is called twice
         */

        class AuthenticationHandlerT2a implements HttpHandler
        {
            volatile int count = -1;

            @Override
            public void handle(HttpExchange exchange) throws IOException {
                count++;
                if (count == 1) {
                    t2condlatch.countDown();
                }
                AuthenticationHandler.errorReply(exchange,
                        "Basic realm=\"realm3\"");

            }
        }

         class AuthenticationHandlerT2b implements HttpHandler
        {
            volatile int count = -1;

            @Override
            public void handle(HttpExchange exchange) throws IOException {
                count++;
                switch(count) {
                    case 0:
                        AuthenticationHandler.errorReply(exchange,
                                "Basic realm=\"realm3\"");
                        break;
                    case 1:
                        AuthenticationHandler.okReply(exchange);
                        break;
                    default:
                        System.out.println ("Unexpected request");
                }
            }
        }

        /* T3 tests proxy and server authentication. three threads request same
         * resource at same time. Authenticator should be called once for server
         * and once for proxy
         */

        class AuthenticationHandlerT3a implements HttpHandler
        {
            volatile int count = -1;

            @Override
            public void handle(HttpExchange exchange) throws IOException {
                count++;
                switch(count) {
                    case 0:
                        AuthenticationHandler.proxyReply(exchange,
                                "Basic realm=\"proxy\"");
                        break;
                    case 1:
                        t3cond1.countDown();
                        AuthenticationHandler.errorReply(exchange,
                                "Basic realm=\"realm4\"");
                        break;
                    case 2:
                        AuthenticationHandler.okReply(exchange);
                        break;
                    default:
                        System.out.println ("Unexpected request");
                }
            }
        }

        class AuthenticationHandlerT3bc implements HttpHandler
        {
            volatile int count = -1;

            @Override
            public void handle(HttpExchange exchange) throws IOException {
                count++;
                switch(count) {
                    case 0:
                        AuthenticationHandler.proxyReply(exchange,
                                "Basic realm=\"proxy\"");
                        break;
                    case 1:
                        AuthenticationHandler.okReply(exchange);
                        break;
                    default:
                        System.out.println ("Unexpected request");
                }
            }
        }
    }

    static class AuthenticationHandler {
        static void errorReply(HttpExchange exchange, String reply)
                throws IOException
        {
            exchange.getResponseHeaders().add("Connection", "close");
            exchange.getResponseHeaders().add("WWW-Authenticate", reply);
            exchange.sendResponseHeaders(401, 0);
            exchange.close();
        }

        static void proxyReply (HttpExchange exchange, String reply)
                throws IOException
        {
            exchange.getResponseHeaders().add("Proxy-Authenticate", reply);
            exchange.sendResponseHeaders(407, 0);
        }

        static void okReply (HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Connection", "close");
            String response = "Hello .";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
            exchange.close();
        }
    }

    static Server server;
    static MyAuthenticator auth = new MyAuthenticator ();

    static int redirects = 4;

    static Client c1,c2,c3,c4,c5,c6,c7,c8,c9;

    static CountDownLatch t2condlatch;
    static CountDownLatch t3cond1;
    static CyclicBarrier t1Cond1;

    static void doServerTests (String authority, Server server) throws Exception
    {
        System.out.println ("Doing Server tests");
        System.out.println ("T1");
        c1 = new Client (authority, "/test/realm1/t1a", false);
        c2 = new Client (authority, "/test/realm2/t1b", false);
        c3 = new Client (authority, "/test/realm1/t1c", false);
        c4 = new Client (authority, "/test/realm2/t1d", false);
        c1.start(); c2.start();
        t1Cond1.await();
        c3.start(); c4.start();
        c1.join(); c2.join(); c3.join(); c4.join();

        int f = auth.getCount();
        if (f != 2) {
            except ("Authenticator was called "+f+" times. Should be 2",
                    server);
        }
        if (error) {
            except ("error occurred", server);
        }

        auth.resetCount();
        System.out.println ("T2");

        c5 = new Client (authority, "/test/realm3/t2a", true);
        c6 = new Client (authority, "/test/realm3/t2b", false);
        t2condlatch = new CountDownLatch(1);
        c5.start ();
        t2condlatch.await();
        c6.start ();
        c5.join(); c6.join();

        f = auth.getCount();
        if (f != redirects+1) {
            except ("Authenticator was called "+f+" times. Should be: "
                    + redirects+1, server);
        }
        if (error) {
            except ("error occurred", server);
        }
    }

    static void doProxyTests (String authority, Server server) throws Exception
    {
        System.out.println ("Doing Proxy tests");
        c7 = new Client (authority, "/test/realm4/t3a", false);
        c8 = new Client (authority, "/test/realm4/t3b", false);
        c9 = new Client (authority, "/test/realm4/t3c", false);
        t3cond1 = new CountDownLatch(1);
        c7.start ();
        t3cond1.await();
        c8.start ();
        c9.start ();
        c7.join(); c8.join(); c9.join();

        int f = auth.getCount();
        if (f != 2) {
            except ("Authenticator was called "+f+" times. Should be: " + 2,
                    server);
        }
        if (error) {
            except ("error occurred", server);
        }
    }

    public static void main (String[] args) throws Exception {
        new B4769350().runTest(args[0].equals ("proxy"));
    }

    public void runTest(boolean proxy) throws Exception {
        System.setProperty ("http.maxRedirects", Integer.toString (redirects));
        System.setProperty ("http.auth.serializeRequests", "true");
        Authenticator.setDefault (auth);
        try (Server server = new Server()) {
            server.startServer();
            System.out.println ("Server: listening on port: "
                    + server.getPort());
            if (proxy) {
                System.setProperty ("http.proxyHost",
                        InetAddress.getLoopbackAddress().getHostAddress());
                System.setProperty ("http.proxyPort",
                        Integer.toString(server.getPort()));
                doProxyTests ("www.foo.com", server);
            } else {
                ProxySelector.setDefault(ProxySelector.of(null));
                doServerTests (authority(server.getPort()), server);
            }
        }

    }

    static String authority(int port) {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        String hoststr = loopback.getHostAddress();
        if (hoststr.indexOf(':') > -1) {
            hoststr = "[" + hoststr + "]";
        }
        return hoststr + ":" + port;
    }

    public static void except (String s, Server server) {
        server.close();
        throw new RuntimeException (s);
    }

    static class MyAuthenticator extends Authenticator {
        MyAuthenticator () {
            super ();
        }

        volatile int count = 0;

        @Override
        public PasswordAuthentication getPasswordAuthentication () {
            PasswordAuthentication pw;
            pw = new PasswordAuthentication ("user", "pass1".toCharArray());
            count ++;
            return pw;
        }

        public void resetCount () {
            count = 0;
        }

        public int getCount () {
            return count;
        }
    }
}
