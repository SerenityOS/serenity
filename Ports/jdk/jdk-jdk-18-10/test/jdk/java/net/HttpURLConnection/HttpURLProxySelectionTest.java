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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jdk.test.lib.net.URIBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import sun.net.spi.DefaultProxySelector;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

/**
 * @test
 * @bug 6563286 6797318 8177648 8230220
 * @summary Tests that sun.net.www.protocol.http.HttpURLConnection when dealing with
 * sun.net.spi.DefaultProxySelector#select() handles any IllegalArgumentException
 * correctly
 * @library /test/lib
 * @run testng HttpURLProxySelectionTest
 * @modules java.base/sun.net.spi:+open
 */
public class HttpURLProxySelectionTest {

    private static final String WEB_APP_CONTEXT = "/httpurlproxytest";

    private HttpServer server;
    private SimpleHandler handler;
    private ProxySelector previousDefault;
    private CustomProxySelector ourProxySelector = new CustomProxySelector();

    @BeforeTest
    public void beforeTest() throws Exception {
        previousDefault = ProxySelector.getDefault();
        ProxySelector.setDefault(ourProxySelector);
        handler = new SimpleHandler();
        server = createServer(handler);
    }

    @AfterTest
    public void afterTest() {
        try {
            if (server != null) {
                final int delaySeconds = 0;
                server.stop(delaySeconds);
            }
        } finally {
            ProxySelector.setDefault(previousDefault);
        }
    }

    /**
     * - Test initiates a HTTP request to server
     * - Server receives request and sends a 301 redirect to an URI which doesn't have a "host"
     * - Redirect is expected to fail with IOException (caused by IllegalArgumentException from DefaultProxySelector)
     *
     * @throws Exception
     */
    @Test
    public void test() throws Exception {
        final URL targetURL = URIBuilder.newBuilder()
                .scheme("http")
                .host(server.getAddress().getAddress())
                .port(server.getAddress().getPort())
                .path(WEB_APP_CONTEXT)
                .toURL();
        System.out.println("Sending request to " + targetURL);
        final HttpURLConnection conn = (HttpURLConnection) targetURL.openConnection();
        try {
            conn.getResponseCode();
            Assert.fail("Request to " + targetURL + " was expected to fail during redirect");
        } catch (IOException ioe) {
            // expected because of the redirect to an invalid URL, for which a proxy can't be selected

            // make sure the it was indeed a redirect
            Assert.assertTrue(handler.redirectSent, "Server was expected to send a redirect, but didn't");
            Assert.assertTrue(ourProxySelector.selectorUsedForRedirect, "Proxy selector wasn't used for redirect");

            // make sure the IOException was caused by an IllegalArgumentException
            Assert.assertTrue(ioe.getCause() instanceof IllegalArgumentException, "Unexpected cause in the IOException");
        }
    }


    private static HttpServer createServer(final HttpHandler handler) throws IOException {
        final InetSocketAddress serverAddr = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        final int backlog = -1;
        final HttpServer server = HttpServer.create(serverAddr, backlog);
        // setup the handler
        server.createContext(WEB_APP_CONTEXT, handler);
        // start the server
        server.start();
        System.out.println("Server started on " + server.getAddress());
        return server;
    }

    private static class SimpleHandler implements HttpHandler {
        private volatile boolean redirectSent = false;

        @Override
        public void handle(final HttpExchange httpExchange) throws IOException {
            final String redirectURL;
            try {
                redirectURL = new URI("http", "/irrelevant", null).toString();
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
            httpExchange.getResponseHeaders().add("Location", redirectURL);
            final URI requestURI = httpExchange.getRequestURI();
            System.out.println("Handling " + httpExchange.getRequestMethod() + " request "
                    + requestURI + " responding with redirect to " + redirectURL);
            this.redirectSent = true;
            httpExchange.sendResponseHeaders(301, -1);
        }

    }

    private static class CustomProxySelector extends DefaultProxySelector {

        private volatile boolean selectorUsedForRedirect = false;

        @Override
        public List<Proxy> select(final URI uri) {
            if (uri.toString().contains("/irrelevant")) {
                this.selectorUsedForRedirect = true;
            }
            return super.select(uri);
        }
    }
}
