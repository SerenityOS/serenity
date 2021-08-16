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

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import static java.net.http.HttpClient.Builder.NO_PROXY;

/**
 * @test
 * @bug 8087112
 * @summary Basic test for headers, uri, and duration
 */
public class HeadersTest {

    static final BiPredicate<String,String> ACCEPT_ALL = (x, y) -> true;

    static final URI TEST_URI = URI.create("http://www.foo.com/");
    static final HttpClient client = HttpClient.newBuilder().proxy(NO_PROXY).build();

    static void bad(String name) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(TEST_URI);
        try {
            builder.header(name, "foo");
            throw new RuntimeException("Expected IAE for header:" + name);
        } catch (IllegalArgumentException expected)  {
            System.out.println("Got expected IAE: " + expected);
        }
        try {
            HttpRequest req = new HttpRequest() {
                @Override public Optional<BodyPublisher> bodyPublisher() {
                    return Optional.of(BodyPublishers.noBody());
                }
                @Override public String method() {
                    return "GET";
                }
                @Override public Optional<Duration> timeout() {
                    return Optional.empty();
                }
                @Override public boolean expectContinue() {
                    return false;
                }
                @Override public URI uri() {
                    return TEST_URI;
                }
                @Override public Optional<HttpClient.Version> version() {
                    return Optional.empty();
                }
                @Override public HttpHeaders headers() {
                    Map<String, List<String>> map = Map.of(name, List.of("foo"));
                    return HttpHeaders.of(map, ACCEPT_ALL);
                }
            };
            client.send(req, HttpResponse.BodyHandlers.ofString());
            throw new RuntimeException("Expected IAE for header:" + name);
        } catch (IllegalArgumentException expected) {
            System.out.println("Got expected IAE: " + expected);
        }
    }

    static void badValue(String value) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(TEST_URI);
        try {
            builder.header("x-bad", value);
            throw new RuntimeException("Expected IAE for header x-bad: "
                    + value.replace("\r", "\\r")
                    .replace("\n", "\\n"));
        } catch (IllegalArgumentException expected)  {
            System.out.println("Got expected IAE: " + expected);
        }
        try {
            HttpRequest req = new HttpRequest() {
                @Override public Optional<BodyPublisher> bodyPublisher() {
                    return Optional.of(BodyPublishers.noBody());
                }
                @Override public String method() {
                    return "GET";
                }
                @Override public Optional<Duration> timeout() {
                    return Optional.empty();
                }
                @Override public boolean expectContinue() {
                    return false;
                }
                @Override public URI uri() {
                    return TEST_URI;
                }
                @Override public Optional<HttpClient.Version> version() {
                    return Optional.empty();
                }
                @Override public HttpHeaders headers() {
                    Map<String, List<String>> map = Map.of("x-bad", List.of(value));
                    return HttpHeaders.of(map, ACCEPT_ALL);
                }
            };
            client.send(req, HttpResponse.BodyHandlers.ofString());
            throw new RuntimeException("Expected IAE for header x-bad:"
                    + value.replace("\r", "\\r")
                    .replace("\n", "\\n"));
        } catch (IllegalArgumentException expected) {
            System.out.println("Got expected IAE: " + expected);
        }
    }

    static void nullName() throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(TEST_URI);
        try {
            builder.header(null, "foo");
            throw new RuntimeException("Expected NPE for null header name");
        } catch (NullPointerException expected)  {
            System.out.println("Got expected NPE: " + expected);
        }
        try {
            HttpRequest req = new HttpRequest() {
                @Override public Optional<BodyPublisher> bodyPublisher() {
                    return Optional.of(BodyPublishers.noBody());
                }
                @Override public String method() {
                    return "GET";
                }
                @Override public Optional<Duration> timeout() {
                    return Optional.empty();
                }
                @Override public boolean expectContinue() {
                    return false;
                }
                @Override public URI uri() {
                    return TEST_URI;
                }
                @Override public Optional<HttpClient.Version> version() {
                    return Optional.empty();
                }
                @Override public HttpHeaders headers() {
                    Map<String, List<String>> map = new HashMap<>();
                    map.put(null, List.of("foo"));
                    return HttpHeaders.of(map, ACCEPT_ALL);
                }
            };
            client.send(req, HttpResponse.BodyHandlers.ofString());
            throw new RuntimeException("Expected NPE for null header name");
        } catch (NullPointerException expected) {
            System.out.println("Got expected NPE: " + expected);
        }
    }

    static void nullValue() throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(TEST_URI);
        try {
            builder.header("x-bar", null);
            throw new RuntimeException("Expected NPE for null header value");
        } catch (NullPointerException expected)  {
            System.out.println("Got expected NPE: " + expected);
        }
        try {
            HttpRequest req = new HttpRequest() {
                @Override public Optional<BodyPublisher> bodyPublisher() {
                    return Optional.of(BodyPublishers.noBody());
                }
                @Override public String method() {
                    return "GET";
                }
                @Override public Optional<Duration> timeout() {
                    return Optional.empty();
                }
                @Override public boolean expectContinue() {
                    return false;
                }
                @Override public URI uri() {
                    return TEST_URI;
                }
                @Override public Optional<HttpClient.Version> version() {
                    return Optional.empty();
                }
                @Override public HttpHeaders headers() {
                    Map<String, List<String>> map = new HashMap<>();
                    map.put("x-bar", null);
                    return HttpHeaders.of(map, ACCEPT_ALL);
                }
            };
            client.send(req, HttpResponse.BodyHandlers.ofString());
            throw new RuntimeException("Expected NPE for null header values");
        } catch (NullPointerException expected) {
            System.out.println("Got expected NPE: " + expected);
        }
        try {
            HttpRequest req = new HttpRequest() {
                @Override public Optional<BodyPublisher> bodyPublisher() {
                    return Optional.of(BodyPublishers.noBody());
                }
                @Override public String method() {
                    return "GET";
                }
                @Override public Optional<Duration> timeout() {
                    return Optional.empty();
                }
                @Override public boolean expectContinue() {
                    return false;
                }
                @Override public URI uri() {
                    return TEST_URI;
                }
                @Override public Optional<HttpClient.Version> version() {
                    return Optional.empty();
                }
                @Override public HttpHeaders headers() {
                    List<String> values = new ArrayList<>();
                    values.add("foo");
                    values.add(null);
                    return HttpHeaders.of(Map.of("x-bar", values), ACCEPT_ALL);
                }
            };
            client.send(req, HttpResponse.BodyHandlers.ofString());
            throw new RuntimeException("Expected NPE for null header value");
        } catch (NullPointerException expected) {
            System.out.println("Got expected NPE: " + expected);
        }
    }

    static void nullHeaders() throws Exception {
        try {
            HttpRequest req = new HttpRequest() {
                @Override public Optional<BodyPublisher> bodyPublisher() {
                    return Optional.of(BodyPublishers.noBody());
                }
                @Override public String method() {
                    return "GET";
                }
                @Override public Optional<Duration> timeout() {
                    return Optional.empty();
                }
                @Override public boolean expectContinue() {
                    return false;
                }
                @Override public URI uri() {
                    return TEST_URI;
                }
                @Override public Optional<HttpClient.Version> version() {
                    return Optional.empty();
                }
                @Override public HttpHeaders headers() {
                    return HttpHeaders.of(null, ACCEPT_ALL);
                }
            };
            client.send(req, HttpResponse.BodyHandlers.ofString());
            throw new RuntimeException("Expected NPE for null header name");
        } catch (NullPointerException expected) {
            System.out.println("Got expected NPE: " + expected);
        }
        try {
            HttpRequest req = new HttpRequest() {
                @Override public Optional<BodyPublisher> bodyPublisher() {
                    return Optional.of(BodyPublishers.noBody());
                }
                @Override public String method() {
                    return "GET";
                }
                @Override public Optional<Duration> timeout() {
                    return Optional.empty();
                }
                @Override public boolean expectContinue() {
                    return false;
                }
                @Override public URI uri() {
                    return TEST_URI;
                }
                @Override public Optional<HttpClient.Version> version() {
                    return Optional.empty();
                }
                @Override public HttpHeaders headers() {
                    return null;
                }
            };
            client.send(req, HttpResponse.BodyHandlers.ofString());
            throw new RuntimeException("Expected NPE for null header name");
        } catch (NullPointerException expected) {
            System.out.println("Got expected NPE: " + expected);
        }
    }

    static void good(String name) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(TEST_URI);
        try {
            builder.header(name, "foo");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unexpected IAE for header:" + name);
        }
    }

    static void goodValue(String value) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(TEST_URI);
        try {
            builder.header("x-good", value);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unexpected IAE for x-good: " + value);
        }
    }

    static void badURI() throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        URI uri = URI.create(TEST_URI.toString().replace("http", "ftp"));
        try {
            builder.uri(uri);
            throw new RuntimeException("Expected IAE for uri: " + uri);
        } catch (IllegalArgumentException expected)  {
            System.out.println("Got expected IAE: " + expected);
        }
        try {
            HttpRequest.newBuilder(uri);
            throw new RuntimeException("Expected IAE for uri: " + uri);
        } catch (IllegalArgumentException expected)  {
            System.out.println("Got expected IAE: " + expected);
        }
        try {
            HttpRequest req = new HttpRequest() {
                @Override public Optional<BodyPublisher> bodyPublisher() {
                    return Optional.of(BodyPublishers.noBody());
                }
                @Override public String method() {
                    return "GET";
                }
                @Override public Optional<Duration> timeout() {
                    return Optional.empty();
                }
                @Override public boolean expectContinue() {
                    return false;
                }
                @Override public URI uri() {
                    return uri;
                }
                @Override public Optional<HttpClient.Version> version() {
                    return Optional.empty();
                }
                @Override public HttpHeaders headers() {
                    Map<String, List<String>> map = Map.of("x-good", List.of("foo"));
                    return HttpHeaders.of(map, ACCEPT_ALL);
                }
            };
            client.send(req, HttpResponse.BodyHandlers.ofString());
            throw new RuntimeException("Expected IAE for uri:" + uri);
        } catch (IllegalArgumentException expected) {
            System.out.println("Got expected IAE: " + expected);
        }
    }

    static void nullURI() throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        try {
            builder.uri(null);
            throw new RuntimeException("Expected NPE for null URI");
        } catch (NullPointerException expected)  {
            System.out.println("Got expected NPE: " + expected);
        }
        try {
            HttpRequest.newBuilder(null);
            throw new RuntimeException("Expected NPE for null uri");
        } catch (NullPointerException expected)  {
            System.out.println("Got expected NPE: " + expected);
        }
        try {
            HttpRequest req = new HttpRequest() {
                @Override public Optional<BodyPublisher> bodyPublisher() {
                    return Optional.of(BodyPublishers.noBody());
                }
                @Override public String method() {
                    return "GET";
                }
                @Override public Optional<Duration> timeout() {
                    return Optional.empty();
                }
                @Override public boolean expectContinue() {
                    return false;
                }
                @Override public URI uri() {
                    return null;
                }
                @Override public Optional<HttpClient.Version> version() {
                    return Optional.empty();
                }
                @Override public HttpHeaders headers() {
                    Map<String, List<String>> map = Map.of("x-good", List.of("foo"));
                    return HttpHeaders.of(map, ACCEPT_ALL);
                }
            };
            client.send(req, HttpResponse.BodyHandlers.ofString());
            throw new RuntimeException("Expected NPE for null uri");
        } catch (NullPointerException expected) {
            System.out.println("Got expected NPE: " + expected);
        }
    }

    static void badTimeout() throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(TEST_URI);
        Duration zero = Duration.ofSeconds(0);
        Duration negative = Duration.ofSeconds(-10);
        for (Duration bad : List.of(zero, negative)) {
            try {
                builder.timeout(zero);
                throw new RuntimeException("Expected IAE for timeout: " + bad);
            } catch (IllegalArgumentException expected) {
                System.out.println("Got expected IAE: " + expected);
            }
            try {
                HttpRequest req = new HttpRequest() {
                    @Override
                    public Optional<BodyPublisher> bodyPublisher() {
                        return Optional.of(BodyPublishers.noBody());
                    }

                    @Override
                    public String method() {
                        return "GET";
                    }

                    @Override
                    public Optional<Duration> timeout() {
                        return Optional.of(bad);
                    }

                    @Override
                    public boolean expectContinue() {
                        return false;
                    }

                    @Override
                    public URI uri() {
                        return TEST_URI;
                    }

                    @Override
                    public Optional<HttpClient.Version> version() {
                        return Optional.empty();
                    }

                    @Override
                    public HttpHeaders headers() {
                        Map<String, List<String>> map = Map.of("x-good", List.of("foo"));
                        return HttpHeaders.of(map, ACCEPT_ALL);
                    }
                };
                client.send(req, HttpResponse.BodyHandlers.ofString());
                throw new RuntimeException("Expected IAE for timeout:" + bad);
            } catch (IllegalArgumentException expected) {
                System.out.println("Got expected IAE: " + expected);
            }
        }
    }

    static void nullTimeout() throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(TEST_URI);
        try {
            builder.timeout(null);
            throw new RuntimeException("Expected NPE for null timeout");
        } catch (NullPointerException expected) {
            System.out.println("Got expected NPE: " + expected);
        }
        try {
            HttpRequest req = new HttpRequest() {
                @Override
                public Optional<BodyPublisher> bodyPublisher() {
                    return Optional.of(BodyPublishers.noBody());
                }

                @Override
                public String method() {
                    return "GET";
                }

                @Override
                public Optional<Duration> timeout() {
                    return null;
                }

                @Override
                public boolean expectContinue() {
                    return false;
                }

                @Override
                public URI uri() {
                    return TEST_URI;
                }

                @Override
                public Optional<HttpClient.Version> version() {
                    return Optional.empty();
                }

                @Override
                public HttpHeaders headers() {
                    Map<String, List<String>> map = Map.of("x-good", List.of("foo"));
                    return HttpHeaders.of(map, ACCEPT_ALL);
                }
            };
            client.send(req, HttpResponse.BodyHandlers.ofString());
            throw new RuntimeException("Expected NPE for null timeout");
        } catch (NullPointerException expected) {
            System.out.println("Got expected NPE: " + expected);
        }
    }

    public static void main(String[] args) throws Exception {
        bad("bad:header");
        good("X-Foo!");
        good("Bar~");
        good("x");
        bad(" ");
        good("Hello#world");
        good("Qwer#ert");
        badValue("blah\r\n blah");
        goodValue("blah blah");
        goodValue("blah  blah");
        goodValue("\"blah\\\"  \\\"blah\"");
        nullName();
        nullValue();
        nullHeaders();
        badURI();
        nullURI();
        badTimeout();
        nullTimeout();
    }
}
