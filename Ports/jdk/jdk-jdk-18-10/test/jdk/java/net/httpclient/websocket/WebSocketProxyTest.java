/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8217429 8236859
 * @summary WebSocket proxy tunneling tests
 * @library /test/lib
 * @compile SecureSupport.java DummySecureWebSocketServer.java ../ProxyServer.java
 * @build jdk.test.lib.net.SimpleSSLContext WebSocketProxyTest
 * @run testng/othervm
 *         -Djdk.internal.httpclient.debug=true
 *         -Djdk.internal.httpclient.websocket.debug=true
 *         -Djdk.httpclient.HttpClient.log=errors,requests,headers
 *         -Djdk.http.auth.tunneling.disabledSchemes=
 *         WebSocketProxyTest
 */

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jdk.test.lib.net.SimpleSSLContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.net.ssl.SSLContext;

import static java.net.http.HttpClient.newBuilder;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.FileAssert.fail;

public class WebSocketProxyTest {

    // Used to verify a proxy/websocket server requiring Authentication
    private static final String USERNAME = "wally";
    private static final String PASSWORD = "xyz987";

    static {
        try {
            SSLContext.setDefault(new SimpleSSLContext().get());
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    static class WSAuthenticator extends Authenticator {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(USERNAME, PASSWORD.toCharArray());
        }
    }

    static final Function<int[],DummySecureWebSocketServer> SERVER_WITH_CANNED_DATA =
        new Function<>() {
            @Override public DummySecureWebSocketServer apply(int[] data) {
                return SecureSupport.serverWithCannedData(data); }
            @Override public String toString() { return "SERVER_WITH_CANNED_DATA"; }
        };

    static final Function<int[],DummySecureWebSocketServer> SSL_SERVER_WITH_CANNED_DATA =
            new Function<>() {
                @Override public DummySecureWebSocketServer apply(int[] data) {
                    return SecureSupport.serverWithCannedData(data).secure(); }
                @Override public String toString() { return "SSL_SERVER_WITH_CANNED_DATA"; }
            };

    static final Function<int[],DummySecureWebSocketServer> AUTH_SERVER_WITH_CANNED_DATA =
        new Function<>() {
            @Override public DummySecureWebSocketServer apply(int[] data) {
                return SecureSupport.serverWithCannedDataAndAuthentication(USERNAME, PASSWORD, data); }
            @Override public String toString() { return "AUTH_SERVER_WITH_CANNED_DATA"; }
        };

    static final Function<int[],DummySecureWebSocketServer> AUTH_SSL_SVR_WITH_CANNED_DATA =
            new Function<>() {
                @Override public DummySecureWebSocketServer apply(int[] data) {
                    return SecureSupport.serverWithCannedDataAndAuthentication(USERNAME, PASSWORD, data).secure(); }
                @Override public String toString() { return "AUTH_SSL_SVR_WITH_CANNED_DATA"; }
            };

    static final Supplier<ProxyServer> TUNNELING_PROXY_SERVER =
        new Supplier<>() {
            @Override public ProxyServer get() {
                try { return new ProxyServer(0, true);}
                catch(IOException e) { throw new UncheckedIOException(e); } }
            @Override public String toString() { return "TUNNELING_PROXY_SERVER"; }
        };
    static final Supplier<ProxyServer> AUTH_TUNNELING_PROXY_SERVER =
        new Supplier<>() {
            @Override public ProxyServer get() {
                try { return new ProxyServer(0, true, USERNAME, PASSWORD);}
                catch(IOException e) { throw new UncheckedIOException(e); } }
            @Override public String toString() { return "AUTH_TUNNELING_PROXY_SERVER"; }
        };

    @DataProvider(name = "servers")
    public Object[][] servers() {
        return new Object[][] {
            { SERVER_WITH_CANNED_DATA,       TUNNELING_PROXY_SERVER      },
            { SERVER_WITH_CANNED_DATA,       AUTH_TUNNELING_PROXY_SERVER },
            { SSL_SERVER_WITH_CANNED_DATA,   TUNNELING_PROXY_SERVER      },
            { SSL_SERVER_WITH_CANNED_DATA,   AUTH_TUNNELING_PROXY_SERVER },
            { AUTH_SERVER_WITH_CANNED_DATA,  TUNNELING_PROXY_SERVER      },
            { AUTH_SSL_SVR_WITH_CANNED_DATA, TUNNELING_PROXY_SERVER      },
            { AUTH_SERVER_WITH_CANNED_DATA,  AUTH_TUNNELING_PROXY_SERVER },
            { AUTH_SSL_SVR_WITH_CANNED_DATA, AUTH_TUNNELING_PROXY_SERVER },
        };
    }

    record bytes(byte[] bytes) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof bytes other) {
                return Arrays.equals(bytes(), other.bytes());
            }
            return false;
        }
        @Override
        public int hashCode() { return Arrays.hashCode(bytes()); }
        public String toString() {
            return "0x" + HexFormat.of()
                    .withUpperCase()
                    .formatHex(bytes());
        }
    }

    static List<bytes> ofBytes(List<byte[]> bytes) {
        return bytes.stream().map(bytes::new).toList();
    }

    static String diagnose(List<byte[]> a, List<byte[]> b) {
        var actual = ofBytes(a);
        var expected = ofBytes(b);
        var message = actual.equals(expected) ? "match" : "differ";
        return "%s and %s %s".formatted(actual, expected, message);
    }

    @Test(dataProvider = "servers")
    public void simpleAggregatingBinaryMessages
            (Function<int[],DummySecureWebSocketServer> serverSupplier,
             Supplier<ProxyServer> proxyServerSupplier)
        throws IOException
    {
        List<byte[]> expected = List.of("hello", "chegar")
                .stream()
                .map(s -> s.getBytes(StandardCharsets.US_ASCII))
                .collect(Collectors.toList());
        int[] binary = new int[]{
                0x82, 0x05, 0x68, 0x65, 0x6C, 0x6C, 0x6F,       // hello
                0x82, 0x06, 0x63, 0x68, 0x65, 0x67, 0x61, 0x72, // chegar
                0x88, 0x00                                      // <CLOSE>
        };
        CompletableFuture<List<byte[]>> actual = new CompletableFuture<>();

        try (var proxyServer = proxyServerSupplier.get();
             var server = serverSupplier.apply(binary)) {

            InetSocketAddress proxyAddress = new InetSocketAddress(
                    InetAddress.getLoopbackAddress(), proxyServer.getPort());
            server.open();
            System.out.println("Server: " + server.getURI());
            System.out.println("Proxy: " + proxyAddress);

            WebSocket.Listener listener = new WebSocket.Listener() {

                List<byte[]> collectedBytes = new ArrayList<>();
                ByteBuffer buffer = ByteBuffer.allocate(1024);

                @Override
                public CompletionStage<?> onBinary(WebSocket webSocket,
                                                   ByteBuffer message,
                                                   boolean last) {
                    System.out.printf("onBinary(%s, %s)%n", message, last);
                    webSocket.request(1);

                    append(message);
                    if (last) {
                        buffer.flip();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        buffer.clear();
                        processWholeBinary(bytes);
                    }
                    return null;
                }

                private void append(ByteBuffer message) {
                    if (buffer.remaining() < message.remaining()) {
                        assert message.remaining() > 0;
                        int cap = (buffer.capacity() + message.remaining()) * 2;
                        ByteBuffer b = ByteBuffer.allocate(cap);
                        b.put(buffer.flip());
                        buffer = b;
                    }
                    buffer.put(message);
                }

                private void processWholeBinary(byte[] bytes) {
                    String stringBytes = new String(bytes, UTF_8);
                    System.out.println("processWholeBinary: " + stringBytes);
                    collectedBytes.add(bytes);
                }

                @Override
                public CompletionStage<?> onClose(WebSocket webSocket,
                                                  int statusCode,
                                                  String reason) {
                    actual.complete(collectedBytes);
                    return null;
                }

                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    actual.completeExceptionally(error);
                }
            };

            var webSocket = newBuilder()
                    .proxy(ProxySelector.of(proxyAddress))
                    .authenticator(new WSAuthenticator())
                    .build().newWebSocketBuilder()
                    .buildAsync(server.getURI(), listener)
                    .join();

            List<byte[]> a = actual.join();
            assertEquals(ofBytes(a), ofBytes(expected), diagnose(a, expected));
        }
    }

    // -- authentication specific tests

    /*
     * Ensures authentication succeeds when an Authenticator set on client builder.
     */
    @Test
    public void clientAuthenticate() throws IOException  {
        try (var proxyServer = AUTH_TUNNELING_PROXY_SERVER.get();
             var server = new DummySecureWebSocketServer()){
            server.open();
            InetSocketAddress proxyAddress = new InetSocketAddress(
                    InetAddress.getLoopbackAddress(), proxyServer.getPort());

            var webSocket = newBuilder()
                    .proxy(ProxySelector.of(proxyAddress))
                    .authenticator(new WSAuthenticator())
                    .build()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();
        }
    }

    /*
     * Ensures authentication succeeds when an `Authorization` header is explicitly set.
     */
    @Test
    public void explicitAuthenticate() throws IOException  {
        try (var proxyServer = AUTH_TUNNELING_PROXY_SERVER.get();
             var server = new DummySecureWebSocketServer()) {
            server.open();
            InetSocketAddress proxyAddress = new InetSocketAddress(
                    InetAddress.getLoopbackAddress(), proxyServer.getPort());

            String hv = "Basic " + Base64.getEncoder().encodeToString(
                    (USERNAME + ":" + PASSWORD).getBytes(UTF_8));

            var webSocket = newBuilder()
                    .proxy(ProxySelector.of(proxyAddress)).build()
                    .newWebSocketBuilder()
                    .header("Proxy-Authorization", hv)
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();
        }
    }

    /*
     * Ensures authentication succeeds when an `Authorization` header is explicitly set.
     */
    @Test
    public void explicitAuthenticate2() throws IOException  {
        try (var proxyServer = AUTH_TUNNELING_PROXY_SERVER.get();
             var server = new DummySecureWebSocketServer(USERNAME, PASSWORD).secure()) {
            server.open();
            InetSocketAddress proxyAddress = new InetSocketAddress(
                    InetAddress.getLoopbackAddress(), proxyServer.getPort());

            String hv = "Basic " + Base64.getEncoder().encodeToString(
                    (USERNAME + ":" + PASSWORD).getBytes(UTF_8));

            var webSocket = newBuilder()
                    .proxy(ProxySelector.of(proxyAddress)).build()
                    .newWebSocketBuilder()
                    .header("Proxy-Authorization", hv)
                    .header("Authorization", hv)
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();
        }
    }

    /*
     * Ensures authentication does not succeed when no authenticator is present.
     */
    @Test
    public void failNoAuthenticator() throws IOException  {
        try (var proxyServer = AUTH_TUNNELING_PROXY_SERVER.get();
             var server = new DummySecureWebSocketServer(USERNAME, PASSWORD)) {
            server.open();
            InetSocketAddress proxyAddress = new InetSocketAddress(
                    InetAddress.getLoopbackAddress(), proxyServer.getPort());

            CompletableFuture<WebSocket> cf = newBuilder()
                    .proxy(ProxySelector.of(proxyAddress)).build()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { });

            try {
                var webSocket = cf.join();
                fail("Expected exception not thrown");
            } catch (CompletionException expected) {
                WebSocketHandshakeException e = (WebSocketHandshakeException)expected.getCause();
                HttpResponse<?> response = e.getResponse();
                assertEquals(response.statusCode(), 407);
            }
        }
    }

    /*
     * Ensures authentication does not succeed when the authenticator presents
     * unauthorized credentials.
     */
    @Test
    public void failBadCredentials() throws IOException  {
        try (var proxyServer = AUTH_TUNNELING_PROXY_SERVER.get();
             var server = new DummySecureWebSocketServer(USERNAME, PASSWORD)) {
            server.open();
            InetSocketAddress proxyAddress = new InetSocketAddress(
                    InetAddress.getLoopbackAddress(), proxyServer.getPort());

            Authenticator authenticator = new Authenticator() {
                @Override protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication("BAD"+USERNAME, "".toCharArray());
                }
            };

            CompletableFuture<WebSocket> cf = newBuilder()
                    .proxy(ProxySelector.of(proxyAddress))
                    .authenticator(authenticator)
                    .build()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { });

            try {
                var webSocket = cf.join();
                fail("Expected exception not thrown");
            } catch (CompletionException expected) {
                System.out.println("caught expected exception:" + expected);
            }
        }
    }

    @BeforeMethod
    public void breakBetweenTests() {
        System.gc();
        try {Thread.sleep(100); } catch (InterruptedException x) { /* OK */ }
        System.out.println("\n-------\n");
    }
}
