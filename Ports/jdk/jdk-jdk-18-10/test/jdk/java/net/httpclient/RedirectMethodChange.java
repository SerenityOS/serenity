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

/*
 * @test
 * @summary Method change during redirection
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 *          jdk.httpserver
 * @library /test/lib http2/server
 * @build Http2TestServer
 * @build jdk.test.lib.net.SimpleSSLContext
 * @run testng/othervm RedirectMethodChange
 */

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.testng.Assert.assertEquals;

public class RedirectMethodChange implements HttpServerAdapters {

    SSLContext sslContext;
    HttpClient client;

    HttpTestServer httpTestServer;        // HTTP/1.1    [ 4 servers ]
    HttpTestServer httpsTestServer;       // HTTPS/1.1
    HttpTestServer http2TestServer;       // HTTP/2 ( h2c )
    HttpTestServer https2TestServer;      // HTTP/2 ( h2  )
    String httpURI;
    String httpsURI;
    String http2URI;
    String https2URI;

    static final String RESPONSE = "Hello world";
    static final String POST_BODY = "This is the POST body 123909090909090";

    static HttpRequest.BodyPublisher getRequestBodyFor(String method) {
        switch (method) {
            case "GET":
            case "DELETE":
            case "HEAD":
                return BodyPublishers.noBody();
            case "POST":
            case "PUT":
                return BodyPublishers.ofString(POST_BODY);
            default:
                throw new AssertionError("Unknown method:" + method);
        }
    }

    @DataProvider(name = "variants")
    public Object[][] variants() {
        return new Object[][] {
                { httpURI, "GET",  301, "GET"  },
                { httpURI, "GET",  302, "GET"  },
                { httpURI, "GET",  303, "GET"  },
                { httpURI, "GET",  307, "GET"  },
                { httpURI, "GET",  308, "GET"  },
                { httpURI, "POST", 301, "GET"  },
                { httpURI, "POST", 302, "GET"  },
                { httpURI, "POST", 303, "GET"  },
                { httpURI, "POST", 307, "POST" },
                { httpURI, "POST", 308, "POST" },
                { httpURI, "PUT",  301, "PUT"  },
                { httpURI, "PUT",  302, "PUT"  },
                { httpURI, "PUT",  303, "GET"  },
                { httpURI, "PUT",  307, "PUT"  },
                { httpURI, "PUT",  308, "PUT"  },

                { httpsURI, "GET",  301, "GET"  },
                { httpsURI, "GET",  302, "GET"  },
                { httpsURI, "GET",  303, "GET"  },
                { httpsURI, "GET",  307, "GET"  },
                { httpsURI, "GET",  308, "GET"  },
                { httpsURI, "POST", 301, "GET"  },
                { httpsURI, "POST", 302, "GET"  },
                { httpsURI, "POST", 303, "GET"  },
                { httpsURI, "POST", 307, "POST" },
                { httpsURI, "POST", 308, "POST" },
                { httpsURI, "PUT",  301, "PUT"  },
                { httpsURI, "PUT",  302, "PUT"  },
                { httpsURI, "PUT",  303, "GET"  },
                { httpsURI, "PUT",  307, "PUT"  },
                { httpsURI, "PUT",  308, "PUT"  },

                { http2URI, "GET",  301, "GET"  },
                { http2URI, "GET",  302, "GET"  },
                { http2URI, "GET",  303, "GET"  },
                { http2URI, "GET",  307, "GET"  },
                { http2URI, "GET",  308, "GET"  },
                { http2URI, "POST", 301, "GET"  },
                { http2URI, "POST", 302, "GET"  },
                { http2URI, "POST", 303, "GET"  },
                { http2URI, "POST", 307, "POST" },
                { http2URI, "POST", 308, "POST" },
                { http2URI, "PUT",  301, "PUT"  },
                { http2URI, "PUT",  302, "PUT"  },
                { http2URI, "PUT",  303, "GET"  },
                { http2URI, "PUT",  307, "PUT"  },
                { http2URI, "PUT",  308, "PUT"  },

                { https2URI, "GET",  301, "GET"  },
                { https2URI, "GET",  302, "GET"  },
                { https2URI, "GET",  303, "GET"  },
                { https2URI, "GET",  307, "GET"  },
                { https2URI, "GET",  308, "GET"  },
                { https2URI, "POST", 301, "GET"  },
                { https2URI, "POST", 302, "GET"  },
                { https2URI, "POST", 303, "GET"  },
                { https2URI, "POST", 307, "POST" },
                { https2URI, "POST", 308, "POST" },
                { https2URI, "PUT",  301, "PUT"  },
                { https2URI, "PUT",  302, "PUT"  },
                { https2URI, "PUT",  303, "GET"  },
                { https2URI, "PUT",  307, "PUT"  },
                { https2URI, "PUT",  308, "PUT"  },
        };
    }

    @Test(dataProvider = "variants")
    public void test(String uriString,
                     String method,
                     int redirectCode,
                     String expectedMethod)
        throws Exception
    {
        HttpRequest req = HttpRequest.newBuilder(URI.create(uriString))
                .method(method, getRequestBodyFor(method))
                .header("X-Redirect-Code", Integer.toString(redirectCode))
                .header("X-Expect-Method", expectedMethod)
                .build();
        HttpResponse<String> resp = client.send(req, BodyHandlers.ofString());

        System.out.println("Response: " + resp + ", body: " + resp.body());
        assertEquals(resp.statusCode(), 200);
        assertEquals(resp.body(), RESPONSE);
    }

    // -- Infrastructure

    @BeforeTest
    public void setup() throws Exception {
        sslContext = new SimpleSSLContext().get();
        if (sslContext == null)
            throw new AssertionError("Unexpected null sslContext");

        client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .sslContext(sslContext)
                .build();

        InetSocketAddress sa = new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);

        httpTestServer = HttpTestServer.of(HttpServer.create(sa, 0));
        String targetURI = "http://" + httpTestServer.serverAuthority() + "/http1/redirect/rmt";
        RedirMethodChgeHandler handler = new RedirMethodChgeHandler(targetURI);
        httpTestServer.addHandler(handler, "/http1/");
        httpURI = "http://" + httpTestServer.serverAuthority() + "/http1/test/rmt";

        HttpsServer httpsServer = HttpsServer.create(sa, 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        httpsTestServer = HttpTestServer.of(httpsServer);
        targetURI = "https://" + httpsTestServer.serverAuthority() + "/https1/redirect/rmt";
        handler = new RedirMethodChgeHandler(targetURI);
        httpsTestServer.addHandler(handler,"/https1/");
        httpsURI = "https://" + httpsTestServer.serverAuthority() + "/https1/test/rmt";

        http2TestServer = HttpTestServer.of(new Http2TestServer("localhost", false, 0));
        targetURI = "http://" + http2TestServer.serverAuthority() + "/http2/redirect/rmt";
        handler = new RedirMethodChgeHandler(targetURI);
        http2TestServer.addHandler(handler, "/http2/");
        http2URI = "http://" + http2TestServer.serverAuthority() + "/http2/test/rmt";

        https2TestServer = HttpTestServer.of(new Http2TestServer("localhost", true, sslContext));
        targetURI = "https://" + https2TestServer.serverAuthority() + "/https2/redirect/rmt";
        handler = new RedirMethodChgeHandler(targetURI);
        https2TestServer.addHandler(handler, "/https2/");
        https2URI = "https://" + https2TestServer.serverAuthority() + "/https2/test/rmt";

        httpTestServer.start();
        httpsTestServer.start();
        http2TestServer.start();
        https2TestServer.start();
    }

    @AfterTest
    public void teardown() throws Exception {
        httpTestServer.stop();
        httpsTestServer.stop();
        http2TestServer.stop();
        https2TestServer.stop();
    }

    /**
     * Stateful handler.
     *
     * Request to "<protocol>/test/rmt" is first, with the following checked
     * headers:
     *   X-Redirect-Code: nnn    <the redirect code to send back>
     *   X-Expect-Method: the method that the client should use for the next request
     *
     * The following request should be to "<protocol>/redirect/rmt" and should
     * use the method indicated previously. If all ok, return a 200 response.
     * Otherwise 50X error.
     */
    static class RedirMethodChgeHandler implements HttpTestHandler {

        boolean inTest;
        String expectedMethod;

        final String targetURL;
        RedirMethodChgeHandler(String targetURL) {
            this.targetURL = targetURL;
        }

        boolean readAndCheckBody(HttpTestExchange e) throws IOException {
            String method = e.getRequestMethod();
            String requestBody;
            try (InputStream is = e.getRequestBody()) {
                requestBody = new String(is.readAllBytes(), US_ASCII);
            }
            if ((method.equals("POST") || method.equals("PUT"))
                    && !requestBody.equals(POST_BODY)) {
                Throwable ex = new RuntimeException("Unexpected request body for "
                        + method + ": [" + requestBody +"]");
                ex.printStackTrace();
                e.sendResponseHeaders(503, 0);
                return false;
            }
            return true;
        }

        @Override
        public synchronized void handle(HttpTestExchange he) throws IOException {
            boolean newtest = he.getRequestURI().getPath().endsWith("/test/rmt");
            if ((newtest && inTest) || (!newtest && !inTest)) {
                Throwable ex = new RuntimeException("Unexpected newtest:" + newtest
                        + ", inTest:" + inTest +  ", for " + he.getRequestURI());
                ex.printStackTrace();
                he.sendResponseHeaders(500, 0);
                return;
            }

            if (newtest) {
                HttpTestRequestHeaders hdrs = he.getRequestHeaders();
                String value = hdrs.firstValue("X-Redirect-Code").get();
                int redirectCode = Integer.parseInt(value);
                expectedMethod = hdrs.firstValue("X-Expect-Method").get();
                if (!readAndCheckBody(he))
                    return;
                HttpTestResponseHeaders headersbuilder = he.getResponseHeaders();
                headersbuilder.addHeader("Location", targetURL);
                he.sendResponseHeaders(redirectCode, 0);
                inTest = true;
            } else {
                // should be the redirect
                if (!he.getRequestURI().getPath().endsWith("/redirect/rmt")) {
                    Throwable ex = new RuntimeException("Unexpected redirected request, got:"
                            + he.getRequestURI());
                    ex.printStackTrace();
                    he.sendResponseHeaders(501, 0);
                } else if (!he.getRequestMethod().equals(expectedMethod)) {
                    Throwable ex = new RuntimeException("Expected: " + expectedMethod
                            + " Got: " + he.getRequestMethod());
                    ex.printStackTrace();
                    he.sendResponseHeaders(504, 0);
                } else {
                    if (!readAndCheckBody(he))
                        return;
                    he.sendResponseHeaders(200, RESPONSE.length());
                    try (OutputStream os = he.getResponseBody()) {
                        os.write(RESPONSE.getBytes(US_ASCII));
                    }
                }
                inTest = false;
            }
        }
    }
}
