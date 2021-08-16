/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Collectors;
import static java.net.Proxy.NO_PROXY;

/**
 * @test
 * @bug 8230526
 * @summary Verifies that PlainProxyConnections are cached and reused properly. We do this by
 *          verifying that the remote address of the HTTP exchange (on the fake proxy server)
 *          is always the same InetSocketAddress.
 * @modules jdk.httpserver
 * @run main/othervm PlainProxyConnectionTest
 * @author danielfuchs
 */
public class PlainProxyConnectionTest {

    static final String RESPONSE = "<html><body><p>Hello World!</body></html>";
    static final String PATH = "/foo/";
    static final ConcurrentLinkedQueue<InetSocketAddress> connections = new ConcurrentLinkedQueue<>();

    // For convenience the server is used both as a plain server and as a plain proxy.
    // When used as a proxy, it serves the request itself instead of forwarding it
    // to the requested server.
    static HttpServer createHttpsServer() throws IOException, NoSuchAlgorithmException {
        HttpServer server = com.sun.net.httpserver.HttpServer.create();
        HttpContext context = server.createContext(PATH);
        context.setHandler(new HttpHandler() {
            @Override
            public void handle(HttpExchange he) throws IOException {
                connections.add(he.getRemoteAddress());
                he.getResponseHeaders().add("encoding", "UTF-8");
                byte[] bytes = RESPONSE.getBytes(StandardCharsets.UTF_8);
                he.sendResponseHeaders(200, bytes.length > 0 ? bytes.length : -1);
                he.getResponseBody().write(bytes);
                he.close();
            }
        });

        InetSocketAddress addr = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        server.bind(addr, 0);
        return server;
    }

    public static void main(String[] args)
            throws IOException,
            URISyntaxException,
            NoSuchAlgorithmException,
            InterruptedException
    {
        HttpServer server = createHttpsServer();
        server.start();
        try {
            test(server, HttpClient.Version.HTTP_1_1);
            test(server, HttpClient.Version.HTTP_2);
        } finally {
            server.stop(0);
            System.out.println("Server stopped");
        }
    }

    /**
     * A Proxy Selector that wraps a ProxySelector.of(), and counts the number
     * of times its select method has been invoked. This can be used to ensure
     * that the Proxy Selector is invoked only once per HttpClient.sendXXX
     * invocation.
     */
    static class CountingProxySelector extends ProxySelector {
        private final ProxySelector proxySelector;
        private volatile int count; // 0
        private CountingProxySelector(InetSocketAddress proxyAddress) {
            proxySelector = ProxySelector.of(proxyAddress);
        }

        public static CountingProxySelector of(InetSocketAddress proxyAddress) {
            return new CountingProxySelector(proxyAddress);
        }

        int count() { return count; }

        @Override
        public List<Proxy> select(URI uri) {
            count++;
            return proxySelector.select(uri);
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            proxySelector.connectFailed(uri, sa, ioe);
        }
    }

    // The sanity test sends request to the server, and through the proxy,
    // using the legacy HttpURLConnection to verify that server and proxy
    // work as expected.
    private static void performSanityTest(HttpServer server, URI uri, URI proxiedURI)
        throws IOException {
        connections.clear();
        System.out.println("Verifying communication with server");
        try (InputStream is = uri.toURL().openConnection(NO_PROXY).getInputStream()) {
            String resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println(resp);
            if (!RESPONSE.equals(resp)) {
                throw new AssertionError("Unexpected response from server");
            }
        }
        System.out.println("Communication with server OK");
        int count = connections.size();
        if (count != 1) {
            System.err.println("Unexpected connection count: " + count);
            System.err.println("Connections: " + connections);
            throw new AssertionError("Expected only one connection: " + connections);
        }
        try {
            System.out.println("Pretending the server is a proxy...");
            Proxy p = new Proxy(Proxy.Type.HTTP,
                    InetSocketAddress.createUnresolved(
                            server.getAddress().getAddress().getHostAddress(),
                            server.getAddress().getPort()));
            System.out.println("Verifying communication with proxy");
            HttpURLConnection conn = (HttpURLConnection) proxiedURI.toURL().openConnection(p);
            try (InputStream is = conn.getInputStream()) {
                String resp = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                System.out.println(resp);
                if (!RESPONSE.equals(resp)) {
                    throw new AssertionError("Unexpected response from proxy");
                }
            }
            count = connections.size();
            if (count != 2) {
                System.err.println("Unexpected connection count: " + count);
                System.err.println("Connections: " + connections);
                throw new AssertionError("Expected two connection: " + connections);
            }
            System.out.println("Communication with proxy OK");
        } finally {
            connections.clear();
        }
    }

    public static void test(HttpServer server, HttpClient.Version version)
            throws IOException,
            URISyntaxException,
            InterruptedException {
        connections.clear();
        System.out.println("\n===== Testing with " + version);
        System.out.println("Server is: " + server.getAddress().toString());
        URI uri = new URI("http", null,
                server.getAddress().getAddress().getHostAddress(),
                server.getAddress().getPort(), PATH + "x",
                null, null);
        URI proxiedURI = new URI("http://some.host.that.does.not.exist:4242" + PATH + "x");

        performSanityTest(server, uri, proxiedURI);

        try {
            connections.clear();
            System.out.println("\nReal test begins here.");
            System.out.println("Setting up request with HttpClient for version: "
                    + version.name());
            // This will force the HTTP client to see the server as a proxy,
            // and to (re)use a PlainProxyConnection to send the request
            // to the fake `proxiedURI` at
            // http://some.host.that.does.not.exist:4242/foo/x
            //
            CountingProxySelector ps = CountingProxySelector.of(
                    InetSocketAddress.createUnresolved(
                            server.getAddress().getAddress().getHostAddress(),
                            server.getAddress().getPort()));
            HttpClient client = HttpClient.newBuilder()
                    .version(version)
                    .proxy(ps)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(proxiedURI)
                    .GET()
                    .build();

            System.out.println("Sending request with HttpClient: " + request);
            HttpResponse<String> response
                    = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Got response");
            String resp = response.body();
            System.out.println("Received: " + resp);
            if (!RESPONSE.equals(resp)) {
                throw new AssertionError("Unexpected response");
            }
            if (ps.count() > 1) {
                throw new AssertionError("CountingProxySelector. Expected 1, got " + ps.count());
            }
            int count = connections.size();
            if (count != 1) {
                System.err.println("Unexpected connection count: " + count);
                System.err.println("Connections: " + connections);
                throw new AssertionError("Expected only one connection: " + connections);
            }
            for (int i = 2; i < 5; i++) {
                System.out.println("Sending next request (" + i + ") with HttpClient: " + request);
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Got response");
                resp = response.body();
                System.out.println("Received: " + resp);
                if (!RESPONSE.equals(resp)) {
                    throw new AssertionError("Unexpected response");
                }
                if (ps.count() > i) {
                    throw new AssertionError("CountingProxySelector. Expected "
                            + i + ", got " + ps.count());
                }
                count = connections.size();
                if (count != i) {
                    System.err.println("Unexpected connection count: " + count);
                    System.err.println("Connections: " + connections);
                    throw new AssertionError("Expected " + i + ": " + connections);
                }
            }
            Set<InetSocketAddress> remote = connections.stream().distinct().collect(Collectors.toSet());
            count = remote.size();
            if (count != 1) {
                System.err.println("Unexpected connection count: " + count);
                System.err.println("Connections: " + remote);
                throw new AssertionError("Expected only one connection: " + remote);
            } else {
                System.out.println("PASSED: Proxy received only one connection from: " + remote);
            }
        } finally {
            connections.clear();
        }
    }
}
