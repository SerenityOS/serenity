/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @modules java.net.http
 *          jdk.httpserver
 * @run main/othervm MultiAuthTest
 * @summary Basic Authentication test with multiple clients issuing
 *          multiple requests. Includes password changes
 *          on server and client side.
 */

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static java.nio.charset.StandardCharsets.US_ASCII;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class MultiAuthTest {

    static volatile boolean ok;
    static final String RESPONSE = "Hello world";
    static final String POST_BODY = "This is the POST body " + UUID.randomUUID();

    static HttpServer createServer(ExecutorService e, BasicAuthenticator sa) throws Exception {
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        HttpServer server = HttpServer.create(addr, 10);
        Handler h = new Handler();
        HttpContext serverContext = server.createContext("/test", h);
        serverContext.setAuthenticator(sa);
        server.setExecutor(e);
        server.start();
        return server;
    }

    public interface HttpRequestBuilderFactory extends Function<URI, HttpRequest.Builder> {

        default HttpRequest.Builder request(URI uri) {
            return this.apply(uri);
        }
    }


    public static void main(String[] args) throws Exception {
        ExecutorService e = Executors.newCachedThreadPool();
        ServerAuth sa = new ServerAuth("foo realm");
        HttpServer server = createServer(e, sa);
        int port = server.getAddress().getPort();
        System.out.println("Server port = " + port);

        ClientAuth ca = new ClientAuth();
        HttpClient client1 = HttpClient.newBuilder()
                                       .authenticator(ca)
                                       .build();
        HttpClient client2 = HttpClient.newBuilder()
                                       .authenticator(ca)
                                       .build();
        HttpClient client3 = HttpClient.newHttpClient();

        try {
            URI uri = new URI("http://localhost:" + port + "/test/foo");
            System.out.println("URI: " + uri);

            System.out.println("\nTesting with client #1, Authenticator #1");
            test(client1, ca, uri, 1, null);
            System.out.println("Testing again with client #1, Authenticator #1");
            test(client1, ca, uri, 1, null);
            System.out.println("Testing with client #2, Authenticator #1");
            test(client2, ca, uri, 2, null);

            System.out.println("Testing with default client"
                               + " (HttpClient.newHttpClient()), no authenticator");
            test(HttpClient.newHttpClient(), ca, uri, 2, IOException.class);

            System.out.println("\nSetting default authenticator\n");
            java.net.Authenticator.setDefault(ca);

            System.out.println("Testing default client"
                               + " (HttpClient.newHttpClient()), no authenticator");
            test(HttpClient.newHttpClient(), ca, uri, 3, IOException.class);

            System.out.println("Testing with client #4, no authenticator");
            test(client3, ca, uri, 4, IOException.class);

            String oldpwd = sa.passwd;
            sa.passwd = "changed";
            System.out.println("\nChanged server password\n");

            sa.passwd = "changed";
            System.out.println("\nChanged server password\n");

            System.out.println("Testing with client #1, Authenticator #1"
                                + " (count=" + ca.count.get() +")");
            test(client1, ca, uri, 7, IOException.class);
            System.out.println("Testing again with client #1, Authenticator #1"
                                + " (count=" + ca.count.get() +")");
            test(client1, ca, uri, 10, IOException.class);
            System.out.println("Testing with client #2, Authenticator #1"
                                + " (count=" + ca.count.get() +")");
            test(client2, ca, uri, 14, IOException.class);

            System.out.println("\nRestored server password"
                                + " (count=" + ca.count.get() +")\n");
            sa.passwd = oldpwd;

            int count = ca.count.get(); // depends on retry limit...
            System.out.println("Testing with client #1, Authenticator #1");
            test(client1, ca, uri, count+1, null);
            System.out.println("Testing again with client #1, Authenticator #1");
            test(client1, ca, uri, count+1, null);
            System.out.println("Testing with client #2, Authenticator #1");
            test(client2, ca, uri, count+2, null);

            sa.passwd = ca.passwd = "changed#2";
            System.out.println("\nChanged password on both sides\n");

            System.out.println("Testing with client #1, Authenticator #1");
            test(client1, ca, uri, count+3, null);
            System.out.println("Testing again with client #1, Authenticator #1");
            test(client1, ca, uri, count+3, null);
            System.out.println("Testing with client #2, Authenticator #1");
            test(client2, ca, uri, count+4, null);
        } finally {
            server.stop(0);
            e.shutdownNow();
        }
        System.out.println("OK");
    }

    static void test(HttpClient client,
                     ClientAuth ca,
                     URI uri,
                     int expectCount,
                     Class<? extends Exception> expectFailure)
        throws IOException, InterruptedException
    {
        HttpRequest req = HttpRequest.newBuilder(uri).GET().build();

        HttpResponse resp;
        try {
            resp = client.send(req, BodyHandlers.ofString());
            ok = resp.statusCode() == 200 &&
                resp.body().equals(RESPONSE);
            if (resp.statusCode() == 401 || resp.statusCode() == 407) {
                throw new IOException(String.valueOf(resp));
            }
            if (expectFailure != null) {
                throw new RuntimeException("Expected " + expectFailure.getName()
                         +" not raised");
            }
        } catch (IOException io) {
            if (expectFailure != null) {
                if (expectFailure.isInstance(io)) {
                    System.out.println("Got expected exception: " + io);
                    return;
                }
            }
            throw io;
        }

        if (!ok || ca.count.get() != expectCount)
            throw new RuntimeException("Test failed: ok=" + ok
                 + " count=" + ca.count.get() + " (expected=" + expectCount+")");

        // repeat same request, should succeed but no additional authenticator calls
        resp = client.send(req, BodyHandlers.ofString());
        ok = resp.statusCode() == 200 &&
                resp.body().equals(RESPONSE);

        if (!ok || ca.count.get() != expectCount)
            throw new RuntimeException("Test failed: ok=" + ok
                 + " count=" + ca.count.get() + " (expected=" + expectCount+")");

        // try a POST
        req = HttpRequest.newBuilder(uri)
                         .POST(BodyPublishers.ofString(POST_BODY))
                         .build();
        resp = client.send(req, BodyHandlers.ofString());
        ok = resp.statusCode() == 200;

        if (!ok || ca.count.get() != expectCount)
            throw new RuntimeException("Test failed");

    }

    static class ServerAuth extends BasicAuthenticator {

        volatile String passwd = "passwd";

        ServerAuth(String realm) {
            super(realm);
        }

        @Override
        public boolean checkCredentials(String username, String password) {
            if (!"user".equals(username) || !passwd.equals(password)) {
                return false;
            }
            return true;
        }

    }

    static class ClientAuth extends java.net.Authenticator {
        final AtomicInteger count = new AtomicInteger();
        volatile String passwd = "passwd";

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            count.incrementAndGet();
            return new PasswordAuthentication("user", passwd.toCharArray());
        }
    }

   static class Handler implements HttpHandler {
        static volatile boolean ok;

        @Override
        public void handle(HttpExchange he) throws IOException {
            String method = he.getRequestMethod();
            InputStream is = he.getRequestBody();
            if (method.equalsIgnoreCase("POST")) {
                String requestBody = new String(is.readAllBytes(), US_ASCII);
                if (!requestBody.equals(POST_BODY)) {
                    he.sendResponseHeaders(500, -1);
                    ok = false;
                } else {
                    he.sendResponseHeaders(200, -1);
                    ok = true;
                }
            } else { // GET
                he.sendResponseHeaders(200, RESPONSE.length());
                OutputStream os = he.getResponseBody();
                os.write(RESPONSE.getBytes(US_ASCII));
                os.close();
                ok = true;
            }
        }

   }
}
