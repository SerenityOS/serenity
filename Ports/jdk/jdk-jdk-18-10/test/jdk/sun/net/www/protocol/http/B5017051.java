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
 * @bug 5017051 6360774
 * @modules jdk.httpserver
 * @library /test/lib
 * @run main/othervm B5017051
 * @run main/othervm -Djava.net.preferIPv6Addresses=true B5017051
 * @summary Tests CR 5017051 & 6360774
 */

import java.net.*;
import java.util.*;
import java.io.*;
import com.sun.net.httpserver.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import jdk.test.lib.net.URIBuilder;

/*
 * Part 1:
 *  First request sent to the http server will not have an "Authorization" header set and
 *  the server will respond with a 401, but not until it has set a cookie in the response
 *  headers. The subsequent request ( comes from HttpURLConnection's authentication retry )
 *  will have the appropriate Authorization header and the servers context handler will be
 *  invoked. The test passes only if the client (HttpURLConnection) has sent the cookie
 *  in its second request that had been set via the first response from the server.
 *
 * Part 2:
 *  Preload the CookieManager with a cookie. Make a http request that requires authentication
 *  The cookie will be sent in the first request (without the Authorization header), the
 *  server will respond with a 401 (from MyBasicAuthFilter) and the client will add the
 *  appropriate Authorization header. This tests ensures that there is only one Cookie header
 *  in the request that actually makes it to the Http servers context handler.
 */

public class B5017051
{
    HttpServer httpServer;
    ExecutorService executorService;

    public static void main(String[] args) throws Exception {
        new B5017051();
    }

    public B5017051() throws Exception {
        startHttpServer();
        doClient();
    }

    void doClient() throws Exception {
        java.net.Authenticator.setDefault(new MyAuthenticator());
        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        ProxySelector.setDefault(ProxySelector.of(null));

        try {
            InetSocketAddress address = httpServer.getAddress();

            // Part 1
            URL url = URIBuilder.newBuilder()
                .scheme("http")
                .host(address.getAddress())
                .port(address.getPort())
                .path("/test/")
                .toURL();
            HttpURLConnection uc = (HttpURLConnection)url.openConnection();
            int resp = uc.getResponseCode();
            if (resp != 200)
                throw new RuntimeException("Failed: Part 1, Response code is not 200: " + resp);

            System.out.println("Response code from Part 1 = 200 OK");

            // Part 2
            URL url2 = URIBuilder.newBuilder()
                .scheme("http")
                .host(address.getAddress())
                .port(address.getPort())
                .path("/test2/")
                .toURL();

            // can use the global CookieHandler used for the first test as the URL's are different
            CookieHandler ch = CookieHandler.getDefault();
            Map<String,List<String>> header = new HashMap<String,List<String>>();
            List<String> values = new LinkedList<String>();
            values.add("Test2Cookie=\"TEST2\"; path=\"/test2/\"");
            header.put("Set-Cookie2", values);

            // preload the CookieHandler with a cookie for our URL
            // so that it will be sent during the first request
            ch.put(url2.toURI(), header);

            uc = (HttpURLConnection)url2.openConnection();
            resp = uc.getResponseCode();
            if (resp != 200)
                throw new RuntimeException("Failed: Part 2, Response code is not 200: " + resp);

            System.out.println("Response code from Part 2 = 200 OK");

        } finally {
            httpServer.stop(1);
            executorService.shutdown();
        }
    }

    /**
     * Http Server
     */
    public void startHttpServer() throws IOException {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        httpServer = HttpServer.create(new InetSocketAddress(loopback, 0), 0);

        // create HttpServer context for Part 1.
        HttpContext ctx = httpServer.createContext("/test/", new MyHandler());
        ctx.setAuthenticator( new MyBasicAuthenticator("foo"));
        // CookieFilter needs to be executed before Authenticator.
        ctx.getFilters().add(0, new CookieFilter());

        // create HttpServer context for Part 2.
        HttpContext ctx2 = httpServer.createContext("/test2/", new MyHandler2());
        ctx2.setAuthenticator( new MyBasicAuthenticator("foobar"));

        executorService = Executors.newCachedThreadPool();
        httpServer.setExecutor(executorService);
        httpServer.start();
    }

    class MyHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            InputStream is = t.getRequestBody();
            Headers reqHeaders = t.getRequestHeaders();
            Headers resHeaders = t.getResponseHeaders();
            while (is.read () != -1) ;
            is.close();

            if (!reqHeaders.containsKey("Authorization"))
                t.sendResponseHeaders(400, -1);

            List<String> cookies = reqHeaders.get("Cookie");
            if (cookies != null) {
                for (String str : cookies) {
                    if (str.equals("Customer=WILE_E_COYOTE"))
                        t.sendResponseHeaders(200, -1);
                }
            }
            t.sendResponseHeaders(400, -1);
        }
    }

    class MyHandler2 implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            InputStream is = t.getRequestBody();
            Headers reqHeaders = t.getRequestHeaders();
            Headers resHeaders = t.getResponseHeaders();
            while (is.read () != -1) ;
            is.close();

            if (!reqHeaders.containsKey("Authorization"))
                t.sendResponseHeaders(400, -1);

            List<String> cookies = reqHeaders.get("Cookie");

            // there should only be one Cookie header
            if (cookies != null && (cookies.size() == 1)) {
                t.sendResponseHeaders(200, -1);
            }
            t.sendResponseHeaders(400, -1);
        }
    }

    class MyAuthenticator extends java.net.Authenticator {
        public PasswordAuthentication getPasswordAuthentication () {
            return new PasswordAuthentication("tester", "passwd".toCharArray());
        }
    }

    class MyBasicAuthenticator extends BasicAuthenticator
    {
        public MyBasicAuthenticator(String realm) {
            super(realm);
        }

        public boolean checkCredentials (String username, String password) {
            return username.equals("tester") && password.equals("passwd");
        }
    }

    class CookieFilter extends Filter
    {
        public void doFilter(HttpExchange t, Chain chain) throws IOException
        {
            Headers resHeaders = t.getResponseHeaders();
            Headers reqHeaders = t.getRequestHeaders();

            if (!reqHeaders.containsKey("Authorization"))
                resHeaders.set("Set-Cookie2", "Customer=\"WILE_E_COYOTE\"; path=\"/test/\"");

            chain.doFilter(t);
        }

        public void destroy(HttpContext c) { }

        public void init(HttpContext c) { }

        public String description() {
            return new String("Filter for setting a cookie for requests without an \"Authorization\" header.");
        }
    }
}
