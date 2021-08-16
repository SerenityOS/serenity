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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.net.http.HttpClient;
import javax.net.ssl.SSLContext;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;

public abstract class AbstractNoBody {

    SSLContext sslContext;
    HttpServer httpTestServer;         // HTTP/1.1    [ 4 servers ]
    HttpsServer httpsTestServer;       // HTTPS/1.1
    Http2TestServer http2TestServer;   // HTTP/2 ( h2c )
    Http2TestServer https2TestServer;  // HTTP/2 ( h2  )
    String httpURI_fixed;
    String httpURI_chunk;
    String httpsURI_fixed;
    String httpsURI_chunk;
    String http2URI_fixed;
    String http2URI_chunk;
    String https2URI_fixed;
    String https2URI_chunk;

    static final String SIMPLE_STRING = "Hello world. Goodbye world";
    static final int ITERATION_COUNT = 3;
    // a shared executor helps reduce the amount of threads created by the test
    static final Executor executor = Executors.newFixedThreadPool(ITERATION_COUNT * 2);
    static final ExecutorService serverExecutor = Executors.newFixedThreadPool(ITERATION_COUNT * 4);

    @DataProvider(name = "variants")
    public Object[][] variants() {
        return new Object[][]{
                { httpURI_fixed,    false },
                { httpURI_chunk,    false },
                { httpsURI_fixed,   false },
                { httpsURI_chunk,   false },
                { http2URI_fixed,   false },
                { http2URI_chunk,   false },
                { https2URI_fixed,  false,},
                { https2URI_chunk,  false },

                { httpURI_fixed,    true },
                { httpURI_chunk,    true },
                { httpsURI_fixed,   true },
                { httpsURI_chunk,   true },
                { http2URI_fixed,   true },
                { http2URI_chunk,   true },
                { https2URI_fixed,  true,},
                { https2URI_chunk,  true },
        };
    }

    HttpClient newHttpClient() {
        return HttpClient.newBuilder()
                .executor(executor)
                .sslContext(sslContext)
                .build();
    }

    static String serverAuthority(HttpServer server) {
        return InetAddress.getLoopbackAddress().getHostName() + ":"
                + server.getAddress().getPort();
    }

    @BeforeTest
    public void setup() throws Exception {
        printStamp(START, "setup");
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");

        // HTTP/1.1
        HttpHandler h1_fixedLengthNoBodyHandler = new HTTP1_FixedLengthNoBodyHandler();
        HttpHandler h1_chunkNoBodyHandler = new HTTP1_ChunkedNoBodyHandler();
        InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        httpTestServer = HttpServer.create(sa, 0);
        httpTestServer.setExecutor(serverExecutor);
        httpTestServer.createContext("/http1/noBodyFixed", h1_fixedLengthNoBodyHandler);
        httpTestServer.createContext("/http1/noBodyChunk", h1_chunkNoBodyHandler);
        httpURI_fixed = "http://" + serverAuthority(httpTestServer) + "/http1/noBodyFixed";
        httpURI_chunk = "http://" + serverAuthority(httpTestServer) + "/http1/noBodyChunk";

        httpsTestServer = HttpsServer.create(sa, 0);
        httpsTestServer.setExecutor(serverExecutor);
        httpsTestServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsTestServer.createContext("/https1/noBodyFixed", h1_fixedLengthNoBodyHandler);
        httpsTestServer.createContext("/https1/noBodyChunk", h1_chunkNoBodyHandler);
        httpsURI_fixed = "https://" + serverAuthority(httpsTestServer) + "/https1/noBodyFixed";
        httpsURI_chunk = "https://" + serverAuthority(httpsTestServer) + "/https1/noBodyChunk";

        // HTTP/2
        Http2Handler h2_fixedLengthNoBodyHandler = new HTTP2_FixedLengthNoBodyHandler();
        Http2Handler h2_chunkedNoBodyHandler = new HTTP2_ChunkedNoBodyHandler();

        http2TestServer = new Http2TestServer("localhost", false, 0, serverExecutor, null);
        http2TestServer.addHandler(h2_fixedLengthNoBodyHandler, "/http2/noBodyFixed");
        http2TestServer.addHandler(h2_chunkedNoBodyHandler, "/http2/noBodyChunk");
        http2URI_fixed = "http://" + http2TestServer.serverAuthority() + "/http2/noBodyFixed";
        http2URI_chunk = "http://" + http2TestServer.serverAuthority() + "/http2/noBodyChunk";

        https2TestServer = new Http2TestServer("localhost", true, 0, serverExecutor, sslContext);
        https2TestServer.addHandler(h2_fixedLengthNoBodyHandler, "/https2/noBodyFixed");
        https2TestServer.addHandler(h2_chunkedNoBodyHandler, "/https2/noBodyChunk");
        https2URI_fixed = "https://" + https2TestServer.serverAuthority() + "/https2/noBodyFixed";
        https2URI_chunk = "https://" + https2TestServer.serverAuthority() + "/https2/noBodyChunk";

        httpTestServer.start();
        httpsTestServer.start();
        http2TestServer.start();
        https2TestServer.start();
        printStamp(END,"setup");
    }

    @AfterTest
    public void teardown() throws Exception {
        printStamp(START, "teardown");
        httpTestServer.stop(0);
        httpsTestServer.stop(0);
        http2TestServer.stop();
        https2TestServer.stop();
        printStamp(END, "teardown");
    }

    static final long start = System.nanoTime();
    static final String START = "start";
    static final String END   = "end  ";
    static long elapsed() { return (System.nanoTime() - start)/1000_000;}
    void printStamp(String what, String fmt, Object... args) {
        long elapsed = elapsed();
        long sec = elapsed/1000;
        long ms  = elapsed % 1000;
        String time = sec > 0 ? sec + "sec " : "";
        time = time + ms + "ms";
        System.out.printf("%s: %s \t [%s]\t %s%n",
                getClass().getSimpleName(), what, time, String.format(fmt,args));
    }


    static class HTTP1_FixedLengthNoBodyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            //out.println("NoBodyHandler received request to " + t.getRequestURI());
            try (InputStream is = t.getRequestBody()) {
                is.readAllBytes();
            }
            t.sendResponseHeaders(200, -1); // no body
        }
    }

    static class HTTP1_ChunkedNoBodyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            //out.println("NoBodyHandler received request to " + t.getRequestURI());
            try (InputStream is = t.getRequestBody()) {
                is.readAllBytes();
            }
            t.sendResponseHeaders(200, 0); // chunked
            t.getResponseBody().close();  // write nothing
        }
    }

    static class HTTP2_FixedLengthNoBodyHandler implements Http2Handler {
        @Override
        public void handle(Http2TestExchange t) throws IOException {
            //out.println("NoBodyHandler received request to " + t.getRequestURI());
            try (InputStream is = t.getRequestBody()) {
                is.readAllBytes();
            }
            t.sendResponseHeaders(200, 0);
        }
    }

    static class HTTP2_ChunkedNoBodyHandler implements Http2Handler {
        @Override
        public void handle(Http2TestExchange t) throws IOException {
            //out.println("NoBodyHandler received request to " + t.getRequestURI());
            try (InputStream is = t.getRequestBody()) {
                is.readAllBytes();
            }
            t.sendResponseHeaders(200, -1);
            t.getResponseBody().close();  // write nothing
        }
    }
}
