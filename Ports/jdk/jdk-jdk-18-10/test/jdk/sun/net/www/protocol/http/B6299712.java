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

/*
 * @test
 * @bug 6299712 7150552
 * @modules jdk.httpserver
 * @run main/othervm B6299712
 * @run main/othervm -Djava.net.preferIPv6Addresses=true B6299712
 * @summary  NullPointerException in sun.net.www.protocol.http.HttpURLConnection.followRedirect
 */

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.io.*;
import java.util.*;

/*
 * Test Description:
 *      - main thread is run as a http client
 *      - another thread runs an http server, which redirects calls to "/" to
 *        "/redirect" and returns '200 OK' for the successive call
 *      - a global ResponseCache instance is installed, which returns DeployCacheResponse
 *        for urls that end with "/redirect", i.e. the url redirected to by our simple http server,
 *        and null for other urls.
 *      - the whole result is that the first call will be served by our simple
 *        http server and is redirected to "/redirect". The successive call will be done
 *        automatically by HttpURLConnection, which will be served by DeployCacheResponse.
 *        The NPE will be thrown on the second round if the bug is there.
 */
public class B6299712 {
    static HttpServer server;

    public static void main(String[] args) throws Exception {
        ResponseCache.setDefault(new DeployCacheHandler());
        ProxySelector.setDefault(ProxySelector.of(null)); // no proxy
        startHttpServer();

        makeHttpCall();
    }

    public static void startHttpServer() throws IOException {
        InetAddress address = InetAddress.getLocalHost();
        server = HttpServer.create(new InetSocketAddress(address, 0), 0);
        server.createContext("/", new DefaultHandler());
        server.createContext("/redirect", new RedirectHandler());
        server.start();
    }

    public static void makeHttpCall() throws IOException {
        try {
            System.out.println("http server listen on: "
                    + server.getAddress().getPort());
            URL url = new URL("http",
                               InetAddress.getLocalHost().getHostAddress(),
                               server.getAddress().getPort(), "/");
            HttpURLConnection uc = (HttpURLConnection)url.openConnection();
            if (uc.getResponseCode() != 200)
                throw new RuntimeException("Expected Response Code was 200,"
                        + "received: " + uc.getResponseCode());
            uc.disconnect();
        } finally {
            server.stop(0);
        }
    }

    static class RedirectHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        }

    }

    static class DefaultHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Location", "/redirect");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        }

    }

    static class DeployCacheHandler extends java.net.ResponseCache {

        public synchronized CacheResponse get(final URI uri, String rqstMethod,
                Map<String, List<String>> requestHeaders) throws IOException
        {
            System.out.println("get!!!: " + uri);
            if (!uri.toString().endsWith("redirect")) {
                return null;
            }
            System.out.println("Serving request from cache");
            return new DeployCacheResponse(new EmptyInputStream(),
                                           new HashMap<String, List<String>>());
        }

        public synchronized CacheRequest put(URI uri, URLConnection conn)
            throws IOException
        {
            URL url = uri.toURL();
            return new DeployCacheRequest(url, conn);

        }
    }

    static class DeployCacheRequest extends java.net.CacheRequest {

        private URL _url;
        private URLConnection _conn;

        DeployCacheRequest(URL url, URLConnection conn) {
            _url = url;
            _conn = conn;
        }

        public void abort() {

        }

        public OutputStream getBody() throws IOException {

            return null;
        }
    }

    static class DeployCacheResponse extends java.net.CacheResponse {
        protected InputStream is;
        protected Map<String, List<String>> headers;

        DeployCacheResponse(InputStream is, Map<String, List<String>> headers) {
            this.is = is;
            this.headers = headers;
        }

        public InputStream getBody() throws IOException {
            return is;
        }

        public Map<String, List<String>> getHeaders() throws IOException {
            List<String> val = new ArrayList<>();
            val.add("HTTP/1.1 200 OK");
            headers.put(null, val);
            return headers;
        }
    }

    static class EmptyInputStream extends InputStream {

        public int read() throws IOException {
            return -1;
        }
    }
}
