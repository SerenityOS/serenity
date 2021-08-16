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

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.Test;
import static java.time.Duration.*;
import static org.testng.Assert.*;

/*
 * @test
 * @summary HttpClient[.Builder] API and behaviour checks
 * @library /test/lib
 * @build jdk.test.lib.net.SimpleSSLContext
 * @run testng HttpClientBuilderTest
 */

public class HttpClientBuilderTest {

    static final Class<NullPointerException> NPE = NullPointerException.class;
    static final Class<IllegalArgumentException> IAE = IllegalArgumentException.class;

    @Test
    public void testDefaults() throws Exception {
        List<HttpClient> clients = List.of(HttpClient.newHttpClient(),
                                           HttpClient.newBuilder().build());

        for (HttpClient client : clients) {
            // Empty optionals and defaults
            assertFalse(client.authenticator().isPresent());
            assertFalse(client.cookieHandler().isPresent());
            assertFalse(client.connectTimeout().isPresent());
            assertFalse(client.executor().isPresent());
            assertFalse(client.proxy().isPresent());
            assertTrue(client.sslParameters() != null);
            assertTrue(client.followRedirects().equals(HttpClient.Redirect.NEVER));
            assertTrue(client.sslContext() == SSLContext.getDefault());
            assertTrue(client.version().equals(HttpClient.Version.HTTP_2));
        }
    }

    @Test
    public void testNull() throws Exception {
        HttpClient.Builder builder = HttpClient.newBuilder();
        assertThrows(NPE, () -> builder.authenticator(null));
        assertThrows(NPE, () -> builder.cookieHandler(null));
        assertThrows(NPE, () -> builder.connectTimeout(null));
        assertThrows(NPE, () -> builder.executor(null));
        assertThrows(NPE, () -> builder.proxy(null));
        assertThrows(NPE, () -> builder.sslParameters(null));
        assertThrows(NPE, () -> builder.followRedirects(null));
        assertThrows(NPE, () -> builder.sslContext(null));
        assertThrows(NPE, () -> builder.version(null));
    }

    static class TestAuthenticator extends Authenticator { }

    @Test
    public void testAuthenticator() {
        HttpClient.Builder builder = HttpClient.newBuilder();
        Authenticator a = new TestAuthenticator();
        builder.authenticator(a);
        assertTrue(builder.build().authenticator().get() == a);
        Authenticator b = new TestAuthenticator();
        builder.authenticator(b);
        assertTrue(builder.build().authenticator().get() == b);
        assertThrows(NPE, () -> builder.authenticator(null));
        Authenticator c = new TestAuthenticator();
        builder.authenticator(c);
        assertTrue(builder.build().authenticator().get() == c);
    }

    @Test
    public void testCookieHandler() {
        HttpClient.Builder builder = HttpClient.newBuilder();
        CookieHandler a = new CookieManager();
        builder.cookieHandler(a);
        assertTrue(builder.build().cookieHandler().get() == a);
        CookieHandler b = new CookieManager();
        builder.cookieHandler(b);
        assertTrue(builder.build().cookieHandler().get() == b);
        assertThrows(NPE, () -> builder.cookieHandler(null));
        CookieManager c = new CookieManager();
        builder.cookieHandler(c);
        assertTrue(builder.build().cookieHandler().get() == c);
    }

    @Test
    public void testConnectTimeout() {
        HttpClient.Builder builder = HttpClient.newBuilder();
        Duration a = Duration.ofSeconds(5);
        builder.connectTimeout(a);
        assertTrue(builder.build().connectTimeout().get() == a);
        Duration b = Duration.ofMinutes(1);
        builder.connectTimeout(b);
        assertTrue(builder.build().connectTimeout().get() == b);
        assertThrows(NPE, () -> builder.cookieHandler(null));
        Duration c = Duration.ofHours(100);
        builder.connectTimeout(c);
        assertTrue(builder.build().connectTimeout().get() == c);

        assertThrows(IAE, () -> builder.connectTimeout(ZERO));
        assertThrows(IAE, () -> builder.connectTimeout(ofSeconds(0)));
        assertThrows(IAE, () -> builder.connectTimeout(ofSeconds(-1)));
        assertThrows(IAE, () -> builder.connectTimeout(ofNanos(-100)));
    }

    static class TestExecutor implements Executor {
        public void execute(Runnable r) { }
    }

    @Test
    public void testExecutor() {
        HttpClient.Builder builder = HttpClient.newBuilder();
        TestExecutor a = new TestExecutor();
        builder.executor(a);
        assertTrue(builder.build().executor().get() == a);
        TestExecutor b = new TestExecutor();
        builder.executor(b);
        assertTrue(builder.build().executor().get() == b);
        assertThrows(NPE, () -> builder.executor(null));
        TestExecutor c = new TestExecutor();
        builder.executor(c);
        assertTrue(builder.build().executor().get() == c);
    }

    @Test
    public void testProxySelector() {
        HttpClient.Builder builder = HttpClient.newBuilder();
        ProxySelector a = ProxySelector.of(null);
        builder.proxy(a);
        assertTrue(builder.build().proxy().get() == a);
        ProxySelector b = ProxySelector.of(InetSocketAddress.createUnresolved("foo", 80));
        builder.proxy(b);
        assertTrue(builder.build().proxy().get() == b);
        assertThrows(NPE, () -> builder.proxy(null));
        ProxySelector c = ProxySelector.of(InetSocketAddress.createUnresolved("bar", 80));
        builder.proxy(c);
        assertTrue(builder.build().proxy().get() == c);
    }

    @Test
    public void testSSLParameters() {
        HttpClient.Builder builder = HttpClient.newBuilder();
        SSLParameters a = new SSLParameters();
        a.setCipherSuites(new String[] { "A" });
        builder.sslParameters(a);
        a.setCipherSuites(new String[] { "Z" });
        assertTrue(builder.build().sslParameters() != (a));
        assertTrue(builder.build().sslParameters().getCipherSuites()[0].equals("A"));
        SSLParameters b = new SSLParameters();
        b.setEnableRetransmissions(true);
        builder.sslParameters(b);
        assertTrue(builder.build().sslParameters() != b);
        assertTrue(builder.build().sslParameters().getEnableRetransmissions());
        assertThrows(NPE, () -> builder.sslParameters(null));
        SSLParameters c = new SSLParameters();
        c.setProtocols(new String[] { "C" });
        builder.sslParameters(c);
        c.setProtocols(new String[] { "D" });
        assertTrue(builder.build().sslParameters().getProtocols()[0].equals("C"));
    }

    @Test
    public void testSSLContext() throws Exception {
        HttpClient.Builder builder = HttpClient.newBuilder();
        SSLContext a = (new SimpleSSLContext()).get();
        builder.sslContext(a);
        assertTrue(builder.build().sslContext() == a);
        SSLContext b = (new SimpleSSLContext()).get();
        builder.sslContext(b);
        assertTrue(builder.build().sslContext() == b);
        assertThrows(NPE, () -> builder.sslContext(null));
        SSLContext c = (new SimpleSSLContext()).get();
        builder.sslContext(c);
        assertTrue(builder.build().sslContext() == c);
    }

    @Test
    public void testFollowRedirects() {
        HttpClient.Builder builder = HttpClient.newBuilder();
        builder.followRedirects(Redirect.ALWAYS);
        assertTrue(builder.build().followRedirects() == Redirect.ALWAYS);
        builder.followRedirects(Redirect.NEVER);
        assertTrue(builder.build().followRedirects() == Redirect.NEVER);
        assertThrows(NPE, () -> builder.followRedirects(null));
        builder.followRedirects(Redirect.NORMAL);
        assertTrue(builder.build().followRedirects() == Redirect.NORMAL);
    }

    @Test
    public void testVersion() {
        HttpClient.Builder builder = HttpClient.newBuilder();
        builder.version(Version.HTTP_2);
        assertTrue(builder.build().version() == Version.HTTP_2);
        builder.version(Version.HTTP_1_1);
        assertTrue(builder.build().version() == Version.HTTP_1_1);
        assertThrows(NPE, () -> builder.version(null));
        builder.version(Version.HTTP_2);
        assertTrue(builder.build().version() == Version.HTTP_2);
        builder.version(Version.HTTP_1_1);
        assertTrue(builder.build().version() == Version.HTTP_1_1);
    }

    @Test
    static void testPriority() throws Exception {
        HttpClient.Builder builder = HttpClient.newBuilder();
        assertThrows(IAE, () -> builder.priority(-1));
        assertThrows(IAE, () -> builder.priority(0));
        assertThrows(IAE, () -> builder.priority(257));
        assertThrows(IAE, () -> builder.priority(500));

        builder.priority(1);
        builder.build();
        builder.priority(256);
        builder.build();
    }

    // ---

    static final URI uri = URI.create("http://foo.com/");

    @Test
    static void testHttpClientSendArgs() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(uri).build();

        assertThrows(NPE, () -> client.send(null, BodyHandlers.discarding()));
        assertThrows(NPE, () -> client.send(request, null));
        assertThrows(NPE, () -> client.send(null, null));

        assertThrows(NPE, () -> client.sendAsync(null, BodyHandlers.discarding()));
        assertThrows(NPE, () -> client.sendAsync(request, null));
        assertThrows(NPE, () -> client.sendAsync(null, null));

        assertThrows(NPE, () -> client.sendAsync(null, BodyHandlers.discarding(), null));
        assertThrows(NPE, () -> client.sendAsync(request, null, null));
        assertThrows(NPE, () -> client.sendAsync(null, null, null));

        // CONNECT is disallowed in the implementation, since it is used for
        // tunneling, and is handled separately for security checks.
        HttpRequest connectRequest = new HttpConnectRequest();
        assertThrows(IAE, () -> client.send(connectRequest, BodyHandlers.discarding()));
        assertThrows(IAE, () -> client.sendAsync(connectRequest, BodyHandlers.discarding()));
        assertThrows(IAE, () -> client.sendAsync(connectRequest, BodyHandlers.discarding(), null));
    }

    static class HttpConnectRequest extends HttpRequest {
        @Override public Optional<BodyPublisher> bodyPublisher() { return Optional.empty(); }
        @Override public String method() { return "CONNECT"; }
        @Override public Optional<Duration> timeout() { return Optional.empty(); }
        @Override public boolean expectContinue() { return false; }
        @Override public URI uri() { return URI.create("http://foo.com/"); }
        @Override public Optional<Version> version() { return Optional.empty(); }
        @Override public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (x, y) -> true); }
    }

    // ---

    static final Class<UnsupportedOperationException> UOE =
            UnsupportedOperationException.class;

    @Test
    static void testUnsupportedWebSocket() throws Exception {
        //  @implSpec The default implementation of this method throws
        // {@code UnsupportedOperationException}.
        assertThrows(UOE, () -> (new MockHttpClient()).newWebSocketBuilder());
    }

    static class MockHttpClient extends HttpClient {
        @Override public Optional<CookieHandler> cookieHandler() { return null; }
        @Override public Optional<Duration> connectTimeout() { return null; }
        @Override public Redirect followRedirects() { return null; }
        @Override public Optional<ProxySelector> proxy() { return null; }
        @Override public SSLContext sslContext() { return null; }
        @Override public SSLParameters sslParameters() { return null; }
        @Override public Optional<Authenticator> authenticator() { return null; }
        @Override public Version version() { return null; }
        @Override public Optional<Executor> executor() { return null; }
        @Override public <T> HttpResponse<T>
        send(HttpRequest request, BodyHandler<T> responseBodyHandler)
                throws IOException, InterruptedException {
            return null;
        }
        @Override public <T> CompletableFuture<HttpResponse<T>>
        sendAsync(HttpRequest request, BodyHandler<T> responseBodyHandler) {
            return null;
        }
        @Override
        public <T> CompletableFuture<HttpResponse<T>>
        sendAsync(HttpRequest x, BodyHandler<T> y, PushPromiseHandler<T> z) {
            return null;
        }
    }

    /* ---- standalone entry point ---- */

    public static void main(String[] args) throws Exception {
        HttpClientBuilderTest test = new HttpClientBuilderTest();
        for (Method m : HttpClientBuilderTest.class.getDeclaredMethods()) {
            if (m.isAnnotationPresent(Test.class)) {
                try {
                    m.invoke(test);
                    System.out.printf("test %s: success%n", m.getName());
                } catch (Throwable t ) {
                    System.out.printf("test %s: failed%n", m.getName());
                    t.printStackTrace();
                }
            }
        }
    }
}
