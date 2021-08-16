/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import static java.lang.String.format;
import static java.lang.System.out;
import static java.net.http.HttpClient.Builder.NO_PROXY;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.net.http.HttpClient.Version.HTTP_2;
import static java.time.Duration.*;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.testng.Assert.fail;

public abstract class AbstractConnectTimeoutHandshake {

    // The number of iterations each testXXXClient performs.
    static final int TIMES = 2;

    Server server;
    URI httpsURI;

    static final Duration NO_DURATION = null;

    static List<List<Duration>> TIMEOUTS = List.of(
                    // connectTimeout   HttpRequest timeout
            Arrays.asList( NO_DURATION,   ofSeconds(1)  ),
            Arrays.asList( NO_DURATION,   ofSeconds(2)  ),
            Arrays.asList( NO_DURATION,   ofMillis(500) ),

            Arrays.asList( ofSeconds(1),  NO_DURATION   ),
            Arrays.asList( ofSeconds(2),  NO_DURATION   ),
            Arrays.asList( ofMillis(500), NO_DURATION   ),

            Arrays.asList( ofSeconds(1),  ofMinutes(1)  ),
            Arrays.asList( ofSeconds(2),  ofMinutes(1)  ),
            Arrays.asList( ofMillis(500), ofMinutes(1)  )
    );

    static final List<String> METHODS = List.of("GET" , "POST");
    static final List<Version> VERSIONS = List.of(HTTP_2, HTTP_1_1);

    @DataProvider(name = "variants")
    public Object[][] variants() {
        List<Object[]> l = new ArrayList<>();
        for (List<Duration> timeouts : TIMEOUTS) {
           Duration connectTimeout = timeouts.get(0);
           Duration requestTimeout = timeouts.get(1);
           for (String method: METHODS) {
            for (Version requestVersion : VERSIONS) {
             l.add(new Object[] {requestVersion, method, connectTimeout, requestTimeout});
        }}}
        return l.stream().toArray(Object[][]::new);
    }

    //@Test(dataProvider = "variants")
    protected void timeoutSync(Version requestVersion,
                              String method,
                              Duration connectTimeout,
                              Duration requestTimeout)
        throws Exception
    {
        out.printf("%n--- timeoutSync requestVersion=%s, method=%s, "
                   + "connectTimeout=%s, requestTimeout=%s ---%n",
                   requestVersion, method, connectTimeout, requestTimeout);
        HttpClient client = newClient(connectTimeout);
        HttpRequest request = newRequest(requestVersion, method, requestTimeout);

        for (int i = 0; i < TIMES; i++) {
            out.printf("iteration %d%n", i);
            long startTime = System.nanoTime();
            try {
                HttpResponse<String> resp = client.send(request, BodyHandlers.ofString());
                printResponse(resp);
                fail("Unexpected response: " + resp);
            } catch (HttpConnectTimeoutException expected) {
                long elapsedTime = NANOSECONDS.toMillis(System.nanoTime() - startTime);
                out.printf("Client: received in %d millis%n", elapsedTime);
                out.printf("Client: caught expected HttpConnectTimeoutException: %s%n", expected);
                checkExceptionOrCause(ConnectException.class, expected);
            }
        }
    }

    //@Test(dataProvider = "variants")
    protected void timeoutAsync(Version requestVersion,
                                String method,
                                Duration connectTimeout,
                                Duration requestTimeout) {
        out.printf("%n--- timeoutAsync requestVersion=%s, method=%s, "
                        + "connectTimeout=%s, requestTimeout=%s ---%n",
                   requestVersion, method, connectTimeout, requestTimeout);
        HttpClient client = newClient(connectTimeout);
        HttpRequest request = newRequest(requestVersion, method, requestTimeout);

        for (int i = 0; i < TIMES; i++) {
            out.printf("iteration %d%n", i);
            long startTime = System.nanoTime();
            CompletableFuture<HttpResponse<String>> cf =
                    client.sendAsync(request, BodyHandlers.ofString());
            try {
                HttpResponse<String> resp = cf.join();
                printResponse(resp);
                fail("Unexpected response: " + resp);
            } catch (CompletionException ce) {
                long elapsedTime = NANOSECONDS.toMillis(System.nanoTime() - startTime);
                out.printf("Client: received in %d millis%n", elapsedTime);
                Throwable expected = ce.getCause();
                if (expected instanceof HttpConnectTimeoutException) {
                    out.printf("Client: caught expected HttpConnectTimeoutException: %s%n", expected);
                    checkExceptionOrCause(ConnectException.class, expected);
                } else {
                    out.printf("Client: caught UNEXPECTED exception: %s%n", expected);
                    throw ce;
                }
            }
        }
    }

    static HttpClient newClient(Duration connectTimeout) {
        HttpClient.Builder builder = HttpClient.newBuilder().proxy(NO_PROXY);
        if (connectTimeout != NO_DURATION)
            builder.connectTimeout(connectTimeout);
        return builder.build();
    }

    HttpRequest newRequest(Version reqVersion,
                           String method,
                           Duration requestTimeout) {
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(httpsURI);
        reqBuilder = reqBuilder.version(reqVersion);
        switch (method) {
            case "GET"   : reqBuilder.GET();                         break;
            case "POST"  : reqBuilder.POST(BodyPublishers.noBody()); break;
            default: throw new AssertionError("Unknown method:" + method);
        }
        if (requestTimeout != NO_DURATION)
            reqBuilder.timeout(requestTimeout);
        return reqBuilder.build();
    }

    static void checkExceptionOrCause(Class<? extends Throwable> clazz, Throwable t) {
        final Throwable original = t;
        do {
            if (clazz.isInstance(t)) {
                System.out.println("Found expected exception/cause: " + t);
                return; // found
            }
        } while ((t = t.getCause()) != null);
        original.printStackTrace(System.out);
        throw new RuntimeException("Expected " + clazz + "in " + original);
    }

    static void printResponse(HttpResponse<?> response) {
        out.println("Unexpected response: " + response);
        out.println("Headers: " + response.headers());
        out.println("Body: " + response.body());
    }

    // -- Infrastructure

    static String serverAuthority(Server server) {
        return InetAddress.getLoopbackAddress().getHostName() + ":"
                + server.getPort();
    }

    @BeforeTest
    public void setup() throws Exception {
        server = new Server();
        httpsURI = URI.create("https://" + serverAuthority(server) + "/foo");
        out.println("HTTPS URI: " + httpsURI);
    }

    @AfterTest
    public void teardown() throws Exception {
        server.close();
        out.printf("%n--- teardown ---%n");

        int numClientConnections = variants().length * TIMES;
        int serverCount = server.count;
        out.printf("Client made %d connections.%n", numClientConnections);
        out.printf("Server received %d connections.%n", serverCount);

        // This is usually the case, but not always, do not assert. Remains
        // as an informative message only.
        //if (numClientConnections != serverCount)
        //    fail(format("[numTests: %d] != [serverCount: %d]",
        //                numClientConnections, serverCount));
    }

    /**
     * Emulates a server-side, using plain cleartext Sockets, that just reads
     * initial client hello and does nothing more.
     */
    static class Server extends Thread implements AutoCloseable {
        private final ServerSocket ss;
        private volatile boolean closed;
        volatile int count;

        Server() throws IOException {
            super("Server");
            ss = new ServerSocket();
            ss.setReuseAddress(false);
            ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            this.start();
        }

        int getPort() {
            return ss.getLocalPort();
        }

        @Override
        public void close() {
            if (closed)
                return;
            closed = true;
            try {
                ss.close();
            } catch (IOException e) {
                throw new UncheckedIOException("Unexpected", e);
            }
        }

        @Override
        public void run() {
            while (!closed) {
                try (Socket s = ss.accept()) {
                    count++;
                    out.println("Server: accepted new connection");
                    InputStream is = new BufferedInputStream(s.getInputStream());

                    out.println("Server: starting to read");
                    while (is.read() != -1) ;

                    out.println("Server: closing connection");
                    s.close(); // close without giving any reply
                } catch (IOException e) {
                    if (!closed)
                        out.println("UNEXPECTED " + e);
                }
            }
        }
    }
}
