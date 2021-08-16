/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary HttpRequest[.Builder] API and behaviour checks
 * @run testng RequestBuilderTest
 */

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import static java.time.Duration.ofNanos;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static java.time.Duration.ZERO;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.net.http.HttpClient.Version.HTTP_2;
import static java.net.http.HttpRequest.newBuilder;
import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class RequestBuilderTest {

    static final URI uri = URI.create("http://foo.com/");
    static final Class<NullPointerException> NPE = NullPointerException.class;
    static final Class<IllegalArgumentException> IAE = IllegalArgumentException.class;
    static final Class<IllegalStateException> ISE = IllegalStateException.class;
    static final Class<NumberFormatException> NFE = NumberFormatException.class;
    static final Class<UnsupportedOperationException> UOE = UnsupportedOperationException.class;

    @Test
    public void testDefaults() {
        List<HttpRequest.Builder> builders = List.of(newBuilder().uri(uri),
                                                     newBuilder(uri),
                                                     newBuilder().copy().uri(uri),
                                                     newBuilder(uri).copy());
        for (HttpRequest.Builder builder : builders) {
            assertFalse(builder.build().expectContinue());
            assertEquals(builder.build().method(), "GET");
            assertFalse(builder.build().bodyPublisher().isPresent());
            assertFalse(builder.build().version().isPresent());
            assertFalse(builder.build().timeout().isPresent());
            assertTrue(builder.build().headers() != null);
            assertEquals(builder.build().headers().map().size(), 0);
        }
    }

    @Test
    public void testNull() {
        HttpRequest.Builder builder = newBuilder();

        assertThrows(NPE, () -> newBuilder(null).build());
        assertThrows(NPE, () -> newBuilder(uri).uri(null).build());
        assertThrows(NPE, () -> builder.uri(null));
        assertThrows(NPE, () -> builder.version(null));
        assertThrows(NPE, () -> builder.header(null, null));
        assertThrows(NPE, () -> builder.header("name", null));
        assertThrows(NPE, () -> builder.header(null, "value"));
        assertThrows(NPE, () -> builder.headers(null));
        assertThrows(NPE, () -> builder.headers(new String[] { null, null }));
        assertThrows(NPE, () -> builder.headers(new String[] { "name", null }));
        assertThrows(NPE, () -> builder.headers(new String[] { null, "value" }));
        assertThrows(NPE, () -> builder.method(null, null));
        assertThrows(NPE, () -> builder.method("GET", null));
        assertThrows(NPE, () -> builder.method("POST", null));
        assertThrows(NPE, () -> builder.method("PUT", null));
        assertThrows(NPE, () -> builder.method("DELETE", null));
        assertThrows(NPE, () -> builder.setHeader(null, null));
        assertThrows(NPE, () -> builder.setHeader("name", null));
        assertThrows(NPE, () -> builder.setHeader(null, "value"));
        assertThrows(NPE, () -> builder.timeout(null));
        assertThrows(NPE, () -> builder.POST(null));
        assertThrows(NPE, () -> builder.PUT(null));
    }

    @Test
    public void testURI() {
        assertThrows(ISE, () -> newBuilder().build());
        List<URI> uris = List.of(
                URI.create("ws://foo.com"),
                URI.create("wss://foo.com"),
                URI.create("ftp://foo.com"),
                URI.create("mailto:a@b.com"),
                URI.create("scheme:example.com"),
                URI.create("scheme:example.com"),
                URI.create("scheme:example.com/path"),
                URI.create("path"),
                URI.create("/path")
        );
        for (URI u : uris) {
            assertThrows(IAE, () -> newBuilder(u));
            assertThrows(IAE, () -> newBuilder().uri(u));
        }

        assertEquals(newBuilder(uri).build().uri(), uri);
        assertEquals(newBuilder().uri(uri).build().uri(), uri);
        URI https = URI.create("https://foo.com");
        assertEquals(newBuilder(https).build().uri(), https);
        assertEquals(newBuilder().uri(https).build().uri(), https);
    }

    @Test
    public void testMethod() {
        HttpRequest request = newBuilder(uri).build();
        assertEquals(request.method(), "GET");
        assertTrue(!request.bodyPublisher().isPresent());

        request = newBuilder(uri).GET().build();
        assertEquals(request.method(), "GET");
        assertTrue(!request.bodyPublisher().isPresent());

        request = newBuilder(uri).POST(BodyPublishers.ofString("")).GET().build();
        assertEquals(request.method(), "GET");
        assertTrue(!request.bodyPublisher().isPresent());

        request = newBuilder(uri).PUT(BodyPublishers.ofString("")).GET().build();
        assertEquals(request.method(), "GET");
        assertTrue(!request.bodyPublisher().isPresent());

        request = newBuilder(uri).DELETE().GET().build();
        assertEquals(request.method(), "GET");
        assertTrue(!request.bodyPublisher().isPresent());

        request = newBuilder(uri).POST(BodyPublishers.ofString("")).build();
        assertEquals(request.method(), "POST");
        assertTrue(request.bodyPublisher().isPresent());

        request = newBuilder(uri).PUT(BodyPublishers.ofString("")).build();
        assertEquals(request.method(), "PUT");
        assertTrue(request.bodyPublisher().isPresent());

        request = newBuilder(uri).DELETE().build();
        assertEquals(request.method(), "DELETE");
        assertTrue(!request.bodyPublisher().isPresent());

        request = newBuilder(uri).GET().POST(BodyPublishers.ofString("")).build();
        assertEquals(request.method(), "POST");
        assertTrue(request.bodyPublisher().isPresent());

        request = newBuilder(uri).GET().PUT(BodyPublishers.ofString("")).build();
        assertEquals(request.method(), "PUT");
        assertTrue(request.bodyPublisher().isPresent());

        request = newBuilder(uri).GET().DELETE().build();
        assertEquals(request.method(), "DELETE");
        assertTrue(!request.bodyPublisher().isPresent());

        // CONNECT is disallowed in the implementation, since it is used for
        // tunneling, and is handled separately for security checks.
        assertThrows(IAE, () -> newBuilder(uri).method("CONNECT", BodyPublishers.noBody()).build());

        request = newBuilder(uri).method("GET", BodyPublishers.noBody()).build();
        assertEquals(request.method(), "GET");
        assertTrue(request.bodyPublisher().isPresent());

        request = newBuilder(uri).method("POST", BodyPublishers.ofString("")).build();
        assertEquals(request.method(), "POST");
        assertTrue(request.bodyPublisher().isPresent());
    }

    @Test
    public void testHeaders() {
        HttpRequest.Builder builder = newBuilder(uri);

        String[] empty = new String[0];
        assertThrows(IAE, () -> builder.headers(empty).build());
        assertThrows(IAE, () -> builder.headers("1").build());
        assertThrows(IAE, () -> builder.headers("1", "2", "3").build());
        assertThrows(IAE, () -> builder.headers("1", "2", "3", "4", "5").build());
        assertEquals(builder.build().headers().map().size(),0);

        List<HttpRequest> requests = List.of(
                // same header built from different combinations of the API
                newBuilder(uri).header("A", "B").build(),
                newBuilder(uri).headers("A", "B").build(),
                newBuilder(uri).setHeader("A", "B").build(),
                newBuilder(uri).header("A", "F").setHeader("A", "B").build(),
                newBuilder(uri).headers("A", "F").setHeader("A", "B").build()
        );

        for (HttpRequest r : requests) {
            assertEquals(r.headers().map().size(), 1);
            assertTrue(r.headers().firstValue("A").isPresent());
            assertTrue(r.headers().firstValue("a").isPresent());
            assertEquals(r.headers().firstValue("A").get(), "B");
            assertEquals(r.headers().allValues("A"), List.of("B"));
            assertEquals(r.headers().allValues("C").size(), 0);
            assertEquals(r.headers().map().get("A"), List.of("B"));
            assertThrows(NFE, () -> r.headers().firstValueAsLong("A"));
            assertFalse(r.headers().firstValue("C").isPresent());
            // a non-exhaustive list of mutators
            assertThrows(UOE, () -> r.headers().map().put("Z", List.of("Z")));
            assertThrows(UOE, () -> r.headers().map().remove("A"));
            assertThrows(UOE, () -> r.headers().map().remove("A", "B"));
            assertThrows(UOE, () -> r.headers().map().clear());
            assertThrows(UOE, () -> r.headers().allValues("A").remove("B"));
            assertThrows(UOE, () -> r.headers().allValues("A").remove(1));
            assertThrows(UOE, () -> r.headers().allValues("A").clear());
            assertThrows(UOE, () -> r.headers().allValues("A").add("Z"));
            assertThrows(UOE, () -> r.headers().allValues("A").addAll(List.of("Z")));
            assertThrows(UOE, () -> r.headers().allValues("A").add(1, "Z"));
        }

        requests = List.of(
                // same headers built from different combinations of the API
                newBuilder(uri).header("A", "B")
                               .header("C", "D").build(),
                newBuilder(uri).header("A", "B")
                               .headers("C", "D").build(),
                newBuilder(uri).header("A", "B")
                               .setHeader("C", "D").build(),
                newBuilder(uri).headers("A", "B")
                               .headers("C", "D").build(),
                newBuilder(uri).headers("A", "B")
                               .header("C", "D").build(),
                newBuilder(uri).headers("A", "B")
                               .setHeader("C", "D").build(),
                newBuilder(uri).setHeader("A", "B")
                               .setHeader("C", "D").build(),
                newBuilder(uri).setHeader("A", "B")
                               .header("C", "D").build(),
                newBuilder(uri).setHeader("A", "B")
                               .headers("C", "D").build(),
                newBuilder(uri).headers("A", "B", "C", "D").build()
        );

        for (HttpRequest r : requests) {
            assertEquals(r.headers().map().size(), 2);
            assertTrue(r.headers().firstValue("A").isPresent());
            assertEquals(r.headers().firstValue("A").get(), "B");
            assertEquals(r.headers().allValues("A"), List.of("B"));
            assertTrue(r.headers().firstValue("C").isPresent());
            assertEquals(r.headers().firstValue("C").get(), "D");
            assertEquals(r.headers().allValues("C"), List.of("D"));
            assertEquals(r.headers().map().get("C"), List.of("D"));
            assertThrows(NFE, () -> r.headers().firstValueAsLong("C"));
            assertFalse(r.headers().firstValue("E").isPresent());
            // a smaller non-exhaustive list of mutators
            assertThrows(UOE, () -> r.headers().map().put("Z", List.of("Z")));
            assertThrows(UOE, () -> r.headers().map().remove("C"));
            assertThrows(UOE, () -> r.headers().allValues("A").remove("B"));
            assertThrows(UOE, () -> r.headers().allValues("A").clear());
            assertThrows(UOE, () -> r.headers().allValues("C").add("Z"));
        }

        requests = List.of(
                // same multi-value headers built from different combinations of the API
                newBuilder(uri).header("A", "B")
                               .header("A", "C").build(),
                newBuilder(uri).header("A", "B")
                               .headers("A", "C").build(),
                newBuilder(uri).headers("A", "B")
                               .headers("A", "C").build(),
                newBuilder(uri).headers("A", "B")
                               .header("A", "C").build(),
                newBuilder(uri).setHeader("A", "B")
                               .header("A", "C").build(),
                newBuilder(uri).setHeader("A", "B")
                               .headers("A", "C").build(),
                newBuilder(uri).header("A", "D")
                               .setHeader("A", "B")
                               .headers("A", "C").build(),
                newBuilder(uri).headers("A", "B", "A", "C").build()
        );

        for (HttpRequest r : requests) {
            assertEquals(r.headers().map().size(), 1);
            assertTrue(r.headers().firstValue("A").isPresent());
            assertTrue(r.headers().allValues("A").containsAll(List.of("B", "C")));
            assertEquals(r.headers().allValues("C").size(), 0);
            assertEquals(r.headers().map().get("A"), List.of("B", "C"));
            assertThrows(NFE, () -> r.headers().firstValueAsLong("A"));
            assertFalse(r.headers().firstValue("C").isPresent());
            // a non-exhaustive list of mutators
            assertThrows(UOE, () -> r.headers().map().put("Z", List.of("Z")));
            assertThrows(UOE, () -> r.headers().map().remove("A"));
            assertThrows(UOE, () -> r.headers().map().remove("A", "B"));
            assertThrows(UOE, () -> r.headers().map().clear());
            assertThrows(UOE, () -> r.headers().allValues("A").remove("B"));
            assertThrows(UOE, () -> r.headers().allValues("A").remove(1));
            assertThrows(UOE, () -> r.headers().allValues("A").clear());
            assertThrows(UOE, () -> r.headers().allValues("A").add("Z"));
            assertThrows(UOE, () -> r.headers().allValues("A").addAll(List.of("Z")));
            assertThrows(UOE, () -> r.headers().allValues("A").add(1, "Z"));
        }

        // case-insensitivity
        requests = List.of(
                newBuilder(uri)
                        .header("Accept-Encoding", "gzip, deflate").build(),
                newBuilder(uri)
                        .header("accept-encoding", "gzip, deflate").build(),
                newBuilder(uri)
                        .header("AccePt-EncodINg", "gzip, deflate").build(),
                newBuilder(uri)
                        .header("AcCEpt-EncoDIng", "gzip, deflate").build()
        );
        for (HttpRequest r : requests) {
            for (String name : List.of("Accept-Encoding", "accept-encoding",
                                       "aCCept-EnCODing", "accepT-encodinG")) {
                assertTrue(r.headers().firstValue(name).isPresent());
                assertTrue(r.headers().allValues(name).contains("gzip, deflate"));
                assertEquals(r.headers().firstValue(name).get(), "gzip, deflate");
                assertEquals(r.headers().allValues(name).size(), 1);
                assertEquals(r.headers().map().size(), 1);
                assertEquals(r.headers().map().get(name).size(), 1);
                assertEquals(r.headers().map().get(name).get(0), "gzip, deflate");
            }
        }
    }

    // headers that are allowed now, but weren't before
    private static final Set<String> FORMERLY_RESTRICTED = Set.of("referer", "origin",
            "OriGin", "Referer", "Date", "via", "WarnIng");

    @Test
    public void testFormerlyRestricted()  throws URISyntaxException {
        URI uri = new URI("http://localhost:80/test/");
        URI otherURI = new URI("http://www.foo.com/test/");
        for (String header : FORMERLY_RESTRICTED) {
            HttpRequest req = HttpRequest.newBuilder(uri)
                .header(header, otherURI.toString())
                .GET()
                .build();
        }
    }

    private static final Set<String> RESTRICTED = Set.of("connection", "content-length",
            "expect", "host", "upgrade", "Connection", "Content-Length",
            "eXpect", "hosT", "upgradE", "CONNection", "CONTENT-LENGTH",
            "EXPECT", "Host", "Upgrade");

    interface WithHeader {
        HttpRequest.Builder withHeader(HttpRequest.Builder builder, String name, String value);
    }

    @Test
    public void testRestricted()  throws URISyntaxException {
        URI uri = new URI("http://localhost:80/test/");
        Map<String, WithHeader> lambdas = Map.of(
                "Builder::header",    HttpRequest.Builder::header,
                "Builder::headers",   (b, n, v) -> b.headers(n,v),
                "Builder::setHeader", HttpRequest.Builder::setHeader
                );
        for (Map.Entry<String, WithHeader> e : lambdas.entrySet()) {
            System.out.println("Testing restricted headers with " + e.getKey());
            WithHeader f = e.getValue();
            for (String name : RESTRICTED) {
                String value = name + "-value";
                HttpRequest req = f.withHeader(HttpRequest.newBuilder(uri)
                        .GET(), "x-" + name, value).build();
                String v = req.headers().firstValue("x-" + name).orElseThrow(
                        () -> new RuntimeException("header x-" + name + " not set"));
                assertEquals(v, value);
                try {
                    f.withHeader(HttpRequest.newBuilder(uri)
                            .GET(), name, value).build();
                    throw new RuntimeException("Expected IAE not thrown for " + name);
                } catch (IllegalArgumentException x) {
                    System.out.println("Got expected IAE for " + name + ": " + x);
                }
            }
        }
    }


    @Test
    public void testCopy() {
        HttpRequest.Builder builder = newBuilder(uri).expectContinue(true)
                                                     .header("A", "B")
                                                     .POST(BodyPublishers.ofString(""))
                                                     .timeout(ofSeconds(30))
                                                     .version(HTTP_1_1);
        HttpRequest.Builder copy = builder.copy();
        assertTrue(builder != copy);

        // modify the original builder before building from the copy
        builder.GET().timeout(ofSeconds(5)).version(HTTP_2).setHeader("A", "C");

        HttpRequest copyRequest = copy.build();
        assertEquals(copyRequest.uri(), uri);
        assertEquals(copyRequest.expectContinue(), true);
        assertEquals(copyRequest.headers().map().get("A"), List.of("B"));
        assertEquals(copyRequest.method(), "POST");
        assertEquals(copyRequest.bodyPublisher().isPresent(), true);
        assertEquals(copyRequest.timeout().get(), ofSeconds(30));
        assertTrue(copyRequest.version().isPresent());
        assertEquals(copyRequest.version().get(), HTTP_1_1);

        // lazy set URI ( maybe builder as a template )
        copyRequest = newBuilder().copy().uri(uri).build();
        assertEquals(copyRequest.uri(), uri);

        builder = newBuilder().header("C", "D");
        copy = builder.copy();
        copy.uri(uri);
        copyRequest = copy.build();
        assertEquals(copyRequest.uri(), uri);
        assertEquals(copyRequest.headers().firstValue("C").get(), "D");
    }

    @Test
    public void testTimeout() {
        HttpRequest.Builder builder = newBuilder(uri);
        assertThrows(IAE, () -> builder.timeout(ZERO));
        assertThrows(IAE, () -> builder.timeout(ofSeconds(0)));
        assertThrows(IAE, () -> builder.timeout(ofSeconds(-1)));
        assertThrows(IAE, () -> builder.timeout(ofNanos(-100)));
        assertEquals(builder.timeout(ofNanos(15)).build().timeout().get(), ofNanos(15));
        assertEquals(builder.timeout(ofSeconds(50)).build().timeout().get(), ofSeconds(50));
        assertEquals(builder.timeout(ofMinutes(30)).build().timeout().get(), ofMinutes(30));
    }

    @Test
    public void testExpect() {
        HttpRequest.Builder builder = newBuilder(uri);
        assertEquals(builder.build().expectContinue(), false);
        assertEquals(builder.expectContinue(true).build().expectContinue(), true);
        assertEquals(builder.expectContinue(false).build().expectContinue(), false);
        assertEquals(builder.expectContinue(true).build().expectContinue(), true);
    }

    @Test
    public void testEquals() {
        assertNotEquals(newBuilder(URI.create("http://foo.com")),
                        newBuilder(URI.create("http://bar.com")));

        HttpRequest.Builder builder = newBuilder(uri);
        assertEquals(builder.build(), builder.build());
        assertEquals(builder.build(), newBuilder(uri).build());

        builder.POST(BodyPublishers.noBody());
        assertEquals(builder.build(), builder.build());
        assertEquals(builder.build(), newBuilder(uri).POST(BodyPublishers.noBody()).build());
        assertEquals(builder.build(), newBuilder(uri).POST(BodyPublishers.ofString("")).build());
        assertNotEquals(builder.build(), newBuilder(uri).build());
        assertNotEquals(builder.build(), newBuilder(uri).GET().build());
        assertNotEquals(builder.build(), newBuilder(uri).PUT(BodyPublishers.noBody()).build());

        builder = newBuilder(uri).header("x", "y");
        assertEquals(builder.build(), builder.build());
        assertEquals(builder.build(), newBuilder(uri).header("x", "y").build());
        assertNotEquals(builder.build(), newBuilder(uri).header("x", "Z").build());
        assertNotEquals(builder.build(), newBuilder(uri).header("z", "y").build());
    }
}
