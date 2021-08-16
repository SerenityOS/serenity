
/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package jdk.test.lib.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * A simple HTTP Server.
 **/
public class SimpleHttpServer {
    private final HttpServer httpServer;
    private ExecutorService executor;
    private String address;
    private final String context;
    private final String docRoot;
    private final InetSocketAddress inetSocketAddress;

    public SimpleHttpServer(final InetSocketAddress inetSocketAddress, final String context, final String docRoot)
            throws IOException {
        this.inetSocketAddress = inetSocketAddress;
        this.context = context;
        this.docRoot = docRoot;
        httpServer = HttpServer.create();
    }

    public void start() throws IOException, URISyntaxException {
        MyHttpHandler handler = new MyHttpHandler(docRoot);
        httpServer.bind(inetSocketAddress, 0);
        httpServer.createContext(context, handler);
        executor = Executors.newCachedThreadPool();
        httpServer.setExecutor(executor);
        httpServer.start();
        address = "http:" + URIBuilder.newBuilder().host(httpServer.getAddress().getAddress()).
                port(httpServer.getAddress().getPort()).build().toString();
    }

    public void stop() {
        httpServer.stop(2);
        executor.shutdown();
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return httpServer.getAddress().getPort();
    }

    class MyHttpHandler implements HttpHandler {
        private final URI rootUri;

        MyHttpHandler(final String docroot) {
            rootUri = Path.of(docroot).toUri().normalize();
        }

        public void handle(final HttpExchange t) throws IOException {
            try (InputStream is = t.getRequestBody()) {
                is.readAllBytes();
                Headers rMap = t.getResponseHeaders();
                try (OutputStream os = t.getResponseBody()) {
                    URI uri = t.getRequestURI();
                    String path = uri.getRawPath();
                    assert path.isEmpty() || path.startsWith("/");
                    Path fPath;
                    try {
                        uri = URI.create("file://" + rootUri.getRawPath() + path).normalize();
                        fPath = Path.of(uri);
                    } catch (IllegalArgumentException | FileSystemNotFoundException | SecurityException ex) {
                        ex.printStackTrace();
                        notfound(t, path);
                        return;
                    }
                    byte[] bytes = Files.readAllBytes(fPath);
                    String method = t.getRequestMethod();
                    if (method.equals("HEAD")) {
                        rMap.set("Content-Length", Long.toString(bytes.length));
                        t.sendResponseHeaders(200, -1);
                        t.close();
                    } else if (!method.equals("GET")) {
                        t.sendResponseHeaders(405, -1);
                        t.close();
                        return;
                    }
                    if (path.endsWith(".html") || path.endsWith(".htm")) {
                        rMap.set("Content-Type", "text/html");
                    } else {
                        rMap.set("Content-Type", "text/plain");
                    }
                    t.sendResponseHeaders(200, bytes.length);
                    os.write(bytes);
                }
            }
        }
        void moved(final HttpExchange t) throws IOException {
            Headers req = t.getRequestHeaders();
            Headers map = t.getResponseHeaders();
            URI uri = t.getRequestURI();
            String host = req.getFirst("Host");
            String location = "http://" + host + uri.getPath() + "/";
            map.set("Content-Type", "text/html");
            map.set("Location", location);
            t.sendResponseHeaders(301, -1);
            t.close();
        }
        void notfound(final HttpExchange t, final String p) throws IOException {
            t.getResponseHeaders().set("Content-Type", "text/html");
            t.sendResponseHeaders(404, 0);
            try (OutputStream os = t.getResponseBody()) {
                String s = "<h2>File not found</h2>";
                s = s + p + "<p>";
                os.write(s.getBytes());
            }
            t.close();
        }
    }
}
