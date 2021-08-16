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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import static java.net.http.HttpClient.Version.HTTP_2;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
* @test
* @bug 8252304
* @summary HttpRequest.newBuilder(HttpRequest) API and behaviour checks
* @run testng/othervm HttpRequestNewBuilderTest
*/
public class HttpRequestNewBuilderTest {
    static final Class<NullPointerException> NPE = NullPointerException.class;
    static final Class<IllegalArgumentException> IAE = IllegalArgumentException.class;

    record NamedAssertion(String name, BiConsumer<HttpRequest, HttpRequest> test) { }

    List<NamedAssertion> REQUEST_ASSERTIONS = List.of(
            new NamedAssertion("uri", (r1, r2) -> assertEquals(r1.uri(), r2.uri())),
            new NamedAssertion("timeout", (r1, r2) -> assertEquals(r1.timeout(), r2.timeout())),
            new NamedAssertion("version", (r1, r2) -> assertEquals(r1.version(), r2.version())),
            new NamedAssertion("headers", (r1, r2) -> assertEquals(r1.headers(), r2.headers())),
            new NamedAssertion("expectContinue", (r1, r2) -> assertEquals(r1.expectContinue(), r2.expectContinue())),
            new NamedAssertion("method", (r1, r2) -> {
                assertEquals(r1.method(), r2.method());
                assertBodyPublisherEqual(r1, r2);
            })
    );

    @DataProvider(name = "testRequests")
    public Object[][] variants() {
        return new Object[][]{
                { HttpRequest.newBuilder(URI.create("https://uri-1/")).build() },
                { HttpRequest.newBuilder(URI.create("https://version-1/")).version(HTTP_1_1).build() },
                { HttpRequest.newBuilder(URI.create("https://version-2/")).version(HTTP_2).build() },
                { HttpRequest.newBuilder(URI.create("https://timeout-1/")).timeout(Duration.ofSeconds(30)).build() },
                { HttpRequest.newBuilder(URI.create("https://header-1/")).header("testName1", "testValue1").build() },
                { HttpRequest.newBuilder(URI.create("https://header-2/"))
                        .headers("testName1", "testValue1", "a", "1", "b", "2", "c", "3", "d", "4").build() },
                { HttpRequest.newBuilder(URI.create("https://header-3/"))
                        .headers("testName1", "testValue1", "a", "1", "b", "2", "c", "3", "d", "4", "testName2", "testValue2").build() },
                { HttpRequest.newBuilder(URI.create("https://header-4/"))
                        .headers("a", "1", "b", "2", "c", "3", "d", "4", "testName1", "testValue1").build() },
                { HttpRequest.newBuilder(URI.create("https://header-5/"))
                        .headers( "a", "1", "b", "2", "testName1", "testValue1", "testName2", "testValue2", "c", "3", "d", "4").build() },
                { HttpRequest.newBuilder(URI.create("https://header-6/"))
                        .headers("testName1", "testValue1")
                        .headers("testName1", "v")
                        .headers("testName1", "w")
                        .headers("testName1", "x")
                        .headers("testName1", "y")
                        .headers("testName1", "z").build() },
                { HttpRequest.newBuilder(URI.create("https://header-7/"))
                        .headers("testName1", "testValue1")
                        .headers("testName1", "v")
                        .headers("testName1", "w")
                        .headers("testName1", "x")
                        .headers("testName1", "y")
                        .headers("testName1", "z")
                        .headers("testName1", "testValue2").build() },
                { HttpRequest.newBuilder(URI.create("https://header-8/"))
                        .headers("testName1", "v")
                        .headers("testName1", "w")
                        .headers("testName1", "x")
                        .headers("testName1", "y")
                        .headers("testName1", "z")
                        .headers("testName1", "testValue1").build() },
                { HttpRequest.newBuilder(URI.create("https://header-9/"))
                        .headers("testName1", "v")
                        .headers("testName1", "w")
                        .headers("testName1", "testValue1")
                        .headers("testName1", "testValue2")
                        .headers("testName1", "x")
                        .headers("testName1", "y")
                        .headers("testName1", "z").build() },
                // dedicated method
                { HttpRequest.newBuilder(URI.create("https://method-1/")).GET().build() },
                { HttpRequest.newBuilder(URI.create("https://method-2/")).DELETE().build() },
                { HttpRequest.newBuilder(URI.create("https://method-3/")).POST(HttpRequest.BodyPublishers.ofString("testData")).build() },
                { HttpRequest.newBuilder(URI.create("https://method-4/")).PUT(HttpRequest.BodyPublishers.ofString("testData")).build() },
                // method w/body
                { HttpRequest.newBuilder(URI.create("https://method-5/")).method("GET", HttpRequest.BodyPublishers.ofString("testData")).build() },
                { HttpRequest.newBuilder(URI.create("https://method-6/")).method("DELETE", HttpRequest.BodyPublishers.ofString("testData")).build() },
                { HttpRequest.newBuilder(URI.create("https://method-7/")).method("POST", HttpRequest.BodyPublishers.ofString("testData")).build() },
                { HttpRequest.newBuilder(URI.create("https://method-8/")).method("PUT", HttpRequest.BodyPublishers.ofString("testData")).build() },
                // method w/o body
                { HttpRequest.newBuilder(URI.create("https://method-9/")).method("GET", HttpRequest.BodyPublishers.noBody()).build() },
                { HttpRequest.newBuilder(URI.create("https://method-10/")).method("DELETE", HttpRequest.BodyPublishers.noBody()).build() },
                { HttpRequest.newBuilder(URI.create("https://method-11/")).method("POST", HttpRequest.BodyPublishers.noBody()).build() },
                { HttpRequest.newBuilder(URI.create("https://method-12/")).method("PUT", HttpRequest.BodyPublishers.noBody()).build() },
                // user defined methods w/ & w/o body
                { HttpRequest.newBuilder(URI.create("https://method-13/")).method("TEST", HttpRequest.BodyPublishers.noBody()).build() },
                { HttpRequest.newBuilder(URI.create("https://method-14/")).method("TEST", HttpRequest.BodyPublishers.ofString("testData")).build() },

                { HttpRequest.newBuilder(URI.create("https://all-fields-1/")).GET().expectContinue(true).version(HTTP_2)
                        .timeout(Duration.ofSeconds(1)).header("testName1", "testValue1").build() },
        };
    }

    // test methods
    void assertBodyPublisherEqual(HttpRequest r1, HttpRequest r2) {
        if (r1.bodyPublisher().isPresent()) {
            assertTrue(r2.bodyPublisher().isPresent());
            var bp1 = r1.bodyPublisher().get();
            var bp2 = r2.bodyPublisher().get();

            assertEquals(bp1.getClass(), bp2.getClass());
            assertEquals(bp1.contentLength(), bp2.contentLength());

            final class TestSubscriber implements Flow.Subscriber<ByteBuffer> {
                final BodySubscriber<String> s;
                TestSubscriber(BodySubscriber<String> s) { this.s = s; }
                @Override
                public void onSubscribe(Flow.Subscription subscription) { s.onSubscribe(subscription); }
                @Override
                public void onNext(ByteBuffer item) { s.onNext(List.of(item)); }
                @Override
                public void onError(Throwable throwable) { fail("TestSubscriber failed"); }
                @Override
                public void onComplete() { s.onComplete(); }
            }
            var bs1 = BodySubscribers.ofString(UTF_8);
            bp1.subscribe(new TestSubscriber(bs1));
            var b1 = bs1.getBody().toCompletableFuture().join().getBytes();

            var bs2 = BodySubscribers.ofString(UTF_8);
            bp2.subscribe(new TestSubscriber(bs2));
            var b2 = bs2.getBody().toCompletableFuture().join().getBytes();

            assertEquals(b1, b2);
        } else {
            assertFalse(r2.bodyPublisher().isPresent());
        }
    }

    void assertAllOtherElementsEqual(HttpRequest r1, HttpRequest r2, String... except) {
        var ignoreList = Arrays.asList(except);
        REQUEST_ASSERTIONS.stream()
                .filter(a -> !ignoreList.contains(a.name()))
                .forEach(testCaseAssertion -> testCaseAssertion.test().accept(r1, r2));
    }

    void testBodyPublisher(String methodName, HttpRequest request) {
        // method w/body
        var r = HttpRequest.newBuilder(request, (n, v) -> true)
                .method(methodName, HttpRequest.BodyPublishers.ofString("testData"))
                .build();
        assertEquals(r.method(), methodName);
        assertTrue(r.bodyPublisher().isPresent());
        assertEquals(r.bodyPublisher().get().contentLength(), 8);
        assertAllOtherElementsEqual(r, request, "method");

        // method w/o body
        var noBodyPublisher = HttpRequest.BodyPublishers.noBody();
        var r1 = HttpRequest.newBuilder(request, (n, v) -> true)
                .method(methodName, noBodyPublisher)
                .build();
        assertEquals(r1.method(), methodName);
        assertTrue(r1.bodyPublisher().isPresent());
        assertEquals(r1.bodyPublisher().get(), noBodyPublisher);
        assertAllOtherElementsEqual(r1, request, "method");
    }

    @Test
    public void testNull() {
        var r = HttpRequest.newBuilder(URI.create("https://foobar/")).build();
        assertThrows(NPE, () -> HttpRequest.newBuilder(r, null));
        assertThrows(NPE, () -> HttpRequest.newBuilder(null, (n, v) -> true));
        assertThrows(NPE, () -> HttpRequest.newBuilder(null, null));
    }

    @Test(dataProvider = "testRequests")
    void testBuilder(HttpRequest request) {
        var r = HttpRequest.newBuilder(request, (n, v) -> true).build();
        assertEquals(r, request);
        assertAllOtherElementsEqual(r, request);
    }

    @Test(dataProvider = "testRequests")
    public void testURI(HttpRequest request) {
        URI newURI = URI.create("http://www.newURI.com/");
        var r = HttpRequest.newBuilder(request, (n, v) -> true).uri(newURI).build();

        assertEquals(r.uri(), newURI);
        assertAllOtherElementsEqual(r, request, "uri");
    }

    @Test(dataProvider = "testRequests")
    public void testTimeout(HttpRequest request) {
        var r = HttpRequest.newBuilder(request, (n, v) -> true).timeout(Duration.ofSeconds(2)).build();

        assertEquals(r.timeout().get().getSeconds(), 2);
        assertAllOtherElementsEqual(r, request, "timeout");
    }

    @Test(dataProvider = "testRequests")
    public void testVersion(HttpRequest request) {
        var r = HttpRequest.newBuilder(request, (n, v) -> true).version(HTTP_1_1).build();

        assertEquals(r.version().get(), HTTP_1_1);
        assertAllOtherElementsEqual(r, request, "version");
    }

    @Test(dataProvider = "testRequests")
    public void testGET(HttpRequest request) {
        var r = HttpRequest.newBuilder(request, (n, v) -> true)
                .GET()
                .build();
        assertEquals(r.method(), "GET");
        assertTrue(r.bodyPublisher().isEmpty());
        assertAllOtherElementsEqual(r, request, "method");

        testBodyPublisher("GET", request);
    }

    @Test(dataProvider = "testRequests")
    public void testDELETE(HttpRequest request) {
        var r = HttpRequest.newBuilder(request, (n, v) -> true)
                .DELETE()
                .build();
        assertEquals(r.method(), "DELETE");
        assertTrue(r.bodyPublisher().isEmpty());
        assertAllOtherElementsEqual(r, request, "method");

        testBodyPublisher("DELETE", request);
    }

    @Test(dataProvider = "testRequests")
    public void testPOST(HttpRequest request) {
        var r = HttpRequest.newBuilder(request, (n, v) -> true)
                .POST(HttpRequest.BodyPublishers.ofString("testData"))
                .build();
        assertEquals(r.method(), "POST");
        assertTrue(r.bodyPublisher().isPresent());
        assertEquals(r.bodyPublisher().get().contentLength(), 8);
        assertAllOtherElementsEqual(r, request, "method");

        testBodyPublisher("POST", request);
    }

    @Test(dataProvider = "testRequests")
    public void testPUT(HttpRequest request) {
        var r = HttpRequest.newBuilder(request, (n, v) -> true)
                .PUT(HttpRequest.BodyPublishers.ofString("testData"))
                .build();
        assertEquals(r.method(), "PUT");
        assertTrue(r.bodyPublisher().isPresent());
        assertEquals(r.bodyPublisher().get().contentLength(), 8);
        assertAllOtherElementsEqual(r, request, "method");

        testBodyPublisher("PUT", request);
    }

    @Test(dataProvider = "testRequests")
    public void testUserDefinedMethod(HttpRequest request) {
        testBodyPublisher("TEST", request);
    }

    @Test(dataProvider = "testRequests")
    public void testAddHeader(HttpRequest request) {
        BiPredicate<String, String> filter = (n, v) -> true;

        var r = HttpRequest.newBuilder(request, filter).headers("newName", "newValue").build();
        assertEquals(r.headers().firstValue("newName").get(), "newValue");
        assertEquals(r.headers().allValues("newName").size(), 1);
        assertAllOtherElementsEqual(r, request, "headers");
    }

    @Test(dataProvider = "testRequests")
    public void testRemoveHeader(HttpRequest request) {
        if(!request.headers().map().isEmpty()) {
            assertTrue(request.headers().map().containsKey("testName1"));
        }
        BiPredicate<String, String> filter = (n, v) -> !n.equalsIgnoreCase("testName1");

        var r = HttpRequest.newBuilder(request, filter).build();
        assertFalse(r.headers().map().containsKey("testName1"));
        assertEquals(r.headers().map(), HttpHeaders.of(request.headers().map(), filter).map());
    }

    @Test(dataProvider = "testRequests")
    public void testRemoveSingleHeaderValue(HttpRequest request) {
        if(!request.headers().map().isEmpty()) {
            assertTrue(request.headers().allValues("testName1").contains("testValue1"));
        }
        BiPredicate<String, String> filter = (n, v) ->
                !(n.equalsIgnoreCase("testName1") && v.equals("testValue1"));

        var r = HttpRequest.newBuilder(request, filter).build();
        assertFalse(r.headers().map().containsValue("testValue1"));
        assertEquals(r.headers().map(), HttpHeaders.of(request.headers().map(), filter).map());
    }

    @Test(dataProvider = "testRequests")
    public void testRemoveMultipleHeaders(HttpRequest request) {
        BiPredicate<String, String> isTestName1Value1 = (n ,v) ->
                n.equalsIgnoreCase("testName1") && v.equals("testValue1");
        BiPredicate<String, String> isTestName2Value2 = (n ,v) ->
                n.equalsIgnoreCase("testName2") && v.equals("testValue2");
        var filter = (isTestName1Value1.or(isTestName2Value2)).negate();

        var r = HttpRequest.newBuilder(request, filter).build();
        assertEquals(r.headers().map(), HttpHeaders.of(request.headers().map(), filter).map());

        BiPredicate<String, String> filter1 = (n, v) ->
                !(n.equalsIgnoreCase("testName1") && (v.equals("testValue1") || v.equals("testValue2")));

        var r1 = HttpRequest.newBuilder(request, filter1).build();
        assertEquals(r1.headers().map(), HttpHeaders.of(request.headers().map(), filter1).map());
    }

    @Test(dataProvider = "testRequests")
    public void testRemoveAllHeaders(HttpRequest request) {
        if (!request.headers().map().isEmpty()) {
            BiPredicate<String, String> filter = (n, v) -> false;

            var r = HttpRequest.newBuilder(request, filter).build();
            assertTrue(r.headers().map().isEmpty());
            assertEquals(r.headers().map(), HttpHeaders.of(request.headers().map(), filter).map());
        }
    }

    @Test(dataProvider = "testRequests")
    public void testRetainAllHeaders(HttpRequest request) {
        if (!request.headers().map().isEmpty()) {
            BiPredicate<String, String> filter = (n, v) -> true;

            var r = HttpRequest.newBuilder(request, filter).build();
            assertFalse(r.headers().map().isEmpty());
            assertEquals(r.headers().map(), HttpHeaders.of(request.headers().map(), filter).map());
        }
    }

    @Test
    public void testHeaderExample() {
        var request = HttpRequest.newBuilder(URI.create("https://example/"))
                .header("Foo-Bar", "baz").build();

        BiPredicate<String, String> filter = (n, v) -> !n.equalsIgnoreCase("Foo-Bar");
        var r = HttpRequest.newBuilder(request, filter).build();
        assertFalse(r.headers().map().containsKey("Foo-Bar"));
        assertEquals(r.headers().map(), HttpHeaders.of(request.headers().map(), filter).map());
    }

    @Test
    public void testInvalidMethod() throws URISyntaxException {
        URI testURI = new URI("http://www.foo.com/");
        var r = new HttpRequest() {
            @Override
            public Optional<BodyPublisher> bodyPublisher() { return Optional.empty(); }
            @Override
            public String method() { return "CONNECT"; }
            @Override
            public Optional<Duration> timeout() { return Optional.empty(); }
            @Override
            public boolean expectContinue() { return false; }
            @Override
            public URI uri() { return testURI; }
            @Override
            public Optional<Version> version() { return Optional.empty(); }
            @Override
            public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (n, v) -> true); }
        };
        assertThrows(IAE, () -> HttpRequest.newBuilder(r, (n, v) -> true).build());
    }

    @Test
    public void testInvalidURIScheme() throws URISyntaxException {
        URI badURI = new URI("ftp://foo.com/somefile");
        var r = new HttpRequest() {
            @Override
            public Optional<BodyPublisher> bodyPublisher() { return Optional.empty(); }
            @Override
            public String method() { return "GET"; }
            @Override
            public Optional<Duration> timeout() { return Optional.empty(); }
            @Override
            public boolean expectContinue() { return false; }
            @Override
            public URI uri() { return badURI; }
            @Override
            public Optional<Version> version() { return Optional.empty(); }
            @Override
            public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (n, v) -> true); }
        };
        assertThrows(IAE, () -> HttpRequest.newBuilder(r, (n, v) -> true).build());
    }
}
