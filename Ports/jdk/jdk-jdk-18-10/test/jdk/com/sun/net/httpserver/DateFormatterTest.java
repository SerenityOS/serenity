/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdk.test.lib.net.URIBuilder;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @test
 * @bug 8245307
 * @summary Test for DateFormatter in ExchangeImpl
 * @modules java.net.http
 * @library /test/lib
 * @build DateFormatterTest
 * @run testng/othervm DateFormatterTest
 */
public class DateFormatterTest {

    private HttpServer server;

    static URI httpURI;
    static final Integer ITERATIONS = 10;
    static String format;
    static Pattern pattern;

    @BeforeTest
    public void setUp() throws IOException, URISyntaxException {
        String days = "(Mon|Tue|Wed|Thu|Fri|Sat|Sun)(,)";
        String dayNo = "(\\s\\d\\d\\s)";
        String month = "(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)";
        String hour = "(\\s\\d\\d\\d\\d\\s\\d\\d)(:)(\\d\\d)(:)(\\d\\d\\s)";
        String zone = "(GMT)";
        format = days + dayNo + month + hour + zone;
        pattern = Pattern.compile(format);
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 10);
        server.createContext("/server", new DateFormatHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        httpURI = URIBuilder.newBuilder()
                            .host(server.getAddress().getAddress())
                            .port(server.getAddress().getPort())
                            .scheme("http")
                            .path("/server")
                            .build();
        server.start();
    }

    @AfterTest
    public void cleanUp() {
        server.stop(1);
    }

    @Test
    public void testDateFormat() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                                      .build();
        HttpRequest request = HttpRequest.newBuilder(httpURI)
                                         .GET()
                                         .build();
        for (int i = 0; i < ITERATIONS; i++) {
            HttpResponse<String> response = client.send(request, ofString());
            String date = response.headers().firstValue("Date").orElse("null");
            if (date.equals("null"))
                fail("Date not present");
            Matcher matcher = pattern.matcher(date);
            assertTrue(matcher.matches());
        }
    }

    public static class DateFormatHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (InputStream is = exchange.getRequestBody();
                OutputStream os = exchange.getResponseBody()) {
                byte[] bytes = is.readAllBytes();
                exchange.sendResponseHeaders(200, bytes.length);
                os.write(bytes);
            }
        }
    }
}
