/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 7133367
 * @modules jdk.httpserver
 * @library /test/lib
 * @summary ResponseCache.put should not be called when setUseCaches(false)
 */


import java.net.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jdk.test.lib.net.URIBuilder;

public class NoCache
{
    public static void main(String[] args) throws IOException {
        ResponseCache.setDefault(new ThrowingCache());

        HttpServer server = startHttpServer();
        try {
            URL url = URIBuilder.newBuilder()
                    .scheme("http")
                    .host(server.getAddress().getAddress())
                    .port(server.getAddress().getPort())
                    .path("/NoCache/")
                    .toURLUnchecked();
            URLConnection uc = url.openConnection(Proxy.NO_PROXY);
            uc.setUseCaches(false);
            uc.getInputStream().close();
        } finally {
            server.stop(0);
            // clear the system-wide cache handler, samevm/agentvm mode
            ResponseCache.setDefault(null);
        }
    }

    static class ThrowingCache extends ResponseCache {
        @Override
        public CacheResponse get(URI uri, String rqstMethod,
                                 Map<String,List<String>> rqstHeaders) {
            throw new RuntimeException("ResponseCache.get should not be called");
        }

        @Override
        public CacheRequest put(URI uri, URLConnection conn) {
            throw new RuntimeException("ResponseCache.put should not be called");
        }
    }

    // HTTP Server
    static HttpServer startHttpServer() throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLocalHost(), 0), 0);
        httpServer.createContext("/NoCache/", new SimpleHandler());
        httpServer.start();
        return httpServer;
    }

    static class SimpleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.sendResponseHeaders(200, -1);
            t.close();
        }
    }
}
