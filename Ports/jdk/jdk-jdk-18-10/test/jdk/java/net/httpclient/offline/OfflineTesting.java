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
 * @summary Demonstrates how to achieve testing without network connections
 * @build DelegatingHttpClient FixedHttpResponse FixedResponseHttpClient
 * @run testng/othervm OfflineTesting
 */

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import org.testng.annotations.Test;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class OfflineTesting {

    private static HttpClient getClient() {
        // be sure to return the appropriate client when testing
        //return HttpClient.newHttpClient();
        return FixedResponseHttpClient.createClientFrom(
                HttpClient.newBuilder(),
                200,
                headersOf("Server", "nginx",
                          "Content-Type", "text/html"),
                "A response message");
    }

    @Test
    public void testResponseAsString() {
        HttpClient client = getClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://openjdk.java.net/"))
                .build();

        client.sendAsync(request, BodyHandlers.ofString())
                .thenAccept(response -> {
                    System.out.println("response: " + response);
                    assertEquals(response.statusCode(), 200);
                    assertTrue(response.headers().firstValue("Server").isPresent());
                    assertEquals(response.body(), "A response message"); } )
                .join();
    }

    @Test
    public void testResponseAsByteArray() {
        HttpClient client = getClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://openjdk.java.net/"))
                .build();

        client.sendAsync(request, BodyHandlers.ofByteArray())
                .thenAccept(response -> {
                    System.out.println("response: " + response);
                    assertEquals(response.statusCode(), 200);
                    assertTrue(response.headers().firstValue("Content-Type").isPresent());
                    assertEquals(response.body(), "A response message".getBytes(UTF_8)); } )
                .join();
    }

    @Test
    public void testFileNotFound() {
        //HttpClient client = HttpClient.newHttpClient();
        HttpClient client = FixedResponseHttpClient.createClientFrom(
                HttpClient.newBuilder(),
                404,
                headersOf("Connection",  "keep-alive",
                          "Content-Length", "162",
                          "Content-Type", "text/html",
                          "Date", "Mon, 15 Jan 2018 15:01:16 GMT",
                          "Server", "nginx"),
                "<html>\n" +
                "<head><title>404 Not Found</title></head>\n" +
                "<body bgcolor=\"white\">\n" +
                "<center><h1>404 Not Found</h1></center>\n" +
                "<hr><center>nginx</center>\n" +
                "</body>\n" +
                "</html>");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://openjdk.java.net/notFound"))
                .build();

        client.sendAsync(request, BodyHandlers.ofString())
                .thenAccept(response -> {
                    assertEquals(response.statusCode(), 404);
                    response.headers().firstValue("Content-Type")
                            .ifPresentOrElse(type -> assertEquals(type, "text/html"),
                                             () -> fail("Content-Type not present"));
                    assertTrue(response.body().contains("404 Not Found")); } )
                .join();
    }

    @Test
    public void testEcho() {
        HttpClient client = FixedResponseHttpClient.createEchoClient(
                HttpClient.newBuilder(),
                200,
                headersOf("Connection",  "keep-alive"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://openjdk.java.net/echo"))
                .POST(BodyPublishers.ofString("Hello World"))
                .build();

        client.sendAsync(request, BodyHandlers.ofString())
                .thenAccept(response -> {
                    System.out.println("response: " + response);
                    assertEquals(response.statusCode(), 200);
                    assertEquals(response.body(), "Hello World"); } )
                .join();
    }

    @Test
    public void testEchoBlocking() throws IOException, InterruptedException {
        HttpClient client = FixedResponseHttpClient.createEchoClient(
                HttpClient.newBuilder(),
                200,
                headersOf("Connection",  "keep-alive"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://openjdk.java.net/echo"))
                .POST(BodyPublishers.ofString("Hello chegar!!"))
                .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        System.out.println("response: " + response);
        assertEquals(response.statusCode(), 200);
        assertEquals(response.body(), "Hello chegar!!");
    }

    // ---

    public static IllegalArgumentException newIAE(String message, Object... args) {
        return new IllegalArgumentException(format(message, args));
    }

    static final BiPredicate<String,String> ACCEPT_ALL = (x, y) -> true;

    static HttpHeaders headersOf(String... params) {
        Map<String,List<String>> map = new HashMap<>();
        requireNonNull(params);
        if (params.length == 0 || params.length % 2 != 0) {
            throw newIAE("wrong number, %d, of parameters", params.length);
        }
        for (int i = 0; i < params.length; i += 2) {
            String name  = params[i];
            String value = params[i + 1];
            map.put(name, List.of(value));
        }
        return HttpHeaders.of(map, ACCEPT_ALL);
    }
}
