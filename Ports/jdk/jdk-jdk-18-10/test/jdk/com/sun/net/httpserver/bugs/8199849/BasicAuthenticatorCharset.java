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

/**
 * @test
 * @bug 8199849
 * @library /test/lib
 * @run main/othervm/timeout=6000 BasicAuthenticatorCharset
 * @summary Check for correct use of character sets with BasicAuthenticator() authentication
 */

import com.sun.net.httpserver.*;
import jdk.test.lib.net.URIBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * Test authentication
 */

public class BasicAuthenticatorCharset {

    public static volatile int failCount = 0;

    static final String TEST_USER = "test";
    static final String UNICODE_PW = "Selam D\u00fcnya. Ho\u015f\u00e7akal D\u00fcnya";

    static Handler testHandler;
    static HttpServer testHttpServer;
    static java.net.Authenticator clientAuth;
    static HttpClient client;

    static class Handler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            InputStream is = t.getRequestBody();
            while (is.read() != -1) ;
            is.close();
            t.sendResponseHeaders(200, -1);
            t.close();
        }
    }

    static class ClientAuthenticator extends java.net.Authenticator {
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(TEST_USER, UNICODE_PW.toCharArray());
        }
    }

    static void setAuthenticationPW(String path, String realm, String testPW, Charset charset) {
        HttpContext ctx = testHttpServer.createContext(path, testHandler);
        BasicAuthenticator auth;
        if (charset != null) {
            auth = new BasicAuthenticator(realm, charset) {
                public boolean checkCredentials(String username, String pw) {
                    return username.equals(TEST_USER) && pw.equals(testPW);
                }
            };
        } else {
            auth = new BasicAuthenticator(realm) {
                public boolean checkCredentials(String username, String pw) {
                    return username.equals(TEST_USER) && pw.equals(testPW);
                }
            };
        }
        ctx.setAuthenticator(auth);
    }

    static void connectAndAuth(String path, int expectedStatus) throws Exception {
        // path is prepended with /old or /new for old and new http client
        URL oldurl = URIBuilder.newBuilder()
                .scheme("http")
                .loopback()
                .port(testHttpServer.getAddress().getPort())
                .path("/old" + path)
                .toURL();

        URI newuri = URIBuilder.newBuilder()
            .scheme("http")
            .loopback()
            .port(testHttpServer.getAddress().getPort())
            .path("/new" + path)
            .build();

        // check old client

        HttpURLConnection testConnection = (HttpURLConnection) oldurl.openConnection(Proxy.NO_PROXY);

        // Check for successful authentication
        int status = testConnection.getResponseCode();
        if (status != 401) {
            InputStream is = testConnection.getInputStream();
            while (is.read() != -1) ;
            is.close();
        }
        if (status != expectedStatus) {
            System.err.println("Error (old): " + path);
            failCount++;
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(newuri)
            .GET()
            .build();

        status = -1;
        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            status = response.statusCode();
        } catch (IOException e) {
            System.out.println("NEW: " + e);
            status = 401; // limitation in new API.
        }
        if (status != expectedStatus) {
            System.err.println("Error (new): " + path);
            failCount++;
        }
    }

    public static void main(String[] args) throws Exception {
        clientAuth = new ClientAuthenticator();
        client = HttpClient.newBuilder()
            .authenticator(clientAuth)
            .build();

        String defaultCharset = System.getProperty("file.encoding");
        boolean isUTF8 = defaultCharset.equalsIgnoreCase("UTF-8");
        testHandler = new Handler();
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
        testHttpServer = HttpServer.create(addr, 0);

        // Set the passing credentials OLD client
        setAuthenticationPW("/old/test1/", "passingCharset@test.realm", UNICODE_PW, UTF_8);
        setAuthenticationPW("/old/test2/", "failingCharset@test.realm", UNICODE_PW, ISO_8859_1);
        setAuthenticationPW("/old/test3/", "defaultCharset@test.realm", UNICODE_PW, null);

        // Set the passing credentials NEW client
        setAuthenticationPW("/new/test1/", "passingCharset@test.realm", UNICODE_PW, UTF_8);
        setAuthenticationPW("/new/test2/", "failingCharset@test.realm", UNICODE_PW, ISO_8859_1);
        setAuthenticationPW("/new/test3/", "defaultCharset@test.realm", UNICODE_PW, null);

        ExecutorService executor = Executors.newCachedThreadPool();
        testHttpServer.setExecutor(executor);
        testHttpServer.start();
        java.net.Authenticator.setDefault(clientAuth);

        connectAndAuth("/test1/passingCharset.html", 200);
        connectAndAuth("/test2/failingCharset.html", 401);
        if (isUTF8) {
            connectAndAuth("/test3/defaultCharset.html", 200);
        }

        testHttpServer.stop(2);
        executor.shutdown();

        // should fail once with UNICODE_PW and unsupporting character set
        if (failCount > 0)
            throw new RuntimeException("Fail count : " + failCount);
    }
}
