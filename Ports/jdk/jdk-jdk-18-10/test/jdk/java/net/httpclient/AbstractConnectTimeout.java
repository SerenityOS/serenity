/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.channels.UnresolvedAddressException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionException;
import org.testng.annotations.DataProvider;
import static java.lang.System.out;
import static java.net.http.HttpClient.Builder.NO_PROXY;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.net.http.HttpClient.Version.HTTP_2;
import static java.time.Duration.*;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.testng.Assert.fail;

public abstract class AbstractConnectTimeout {

    static final Duration NO_DURATION = null;

    static List<List<Duration>> TIMEOUTS = List.of(
                    // connectTimeout   HttpRequest timeout
            Arrays.asList( NO_DURATION,   ofSeconds(1)  ),
            Arrays.asList( NO_DURATION,   ofMillis(100) ),
            Arrays.asList( NO_DURATION,   ofNanos(99)   ),
            Arrays.asList( NO_DURATION,   ofNanos(1)    ),

            Arrays.asList( ofSeconds(1),  NO_DURATION   ),
            Arrays.asList( ofMillis(100), NO_DURATION   ),
            Arrays.asList( ofNanos(99),   NO_DURATION   ),
            Arrays.asList( ofNanos(1),    NO_DURATION   ),

            Arrays.asList( ofSeconds(1),  ofMinutes(1)  ),
            Arrays.asList( ofMillis(100), ofMinutes(1)  ),
            Arrays.asList( ofNanos(99),   ofMinutes(1)  ),
            Arrays.asList( ofNanos(1),    ofMinutes(1)  )
    );

    static final List<String> METHODS = List.of("GET", "POST");
    static final List<Version> VERSIONS = List.of(HTTP_2, HTTP_1_1);
    static final List<String> SCHEMES = List.of("https", "http");

    @DataProvider(name = "variants")
    public Object[][] variants() {
        List<Object[]> l = new ArrayList<>();
        for (List<Duration> timeouts : TIMEOUTS) {
           Duration connectTimeout = timeouts.get(0);
           Duration requestTimeout = timeouts.get(1);
           for (String method: METHODS) {
            for (String scheme : SCHEMES) {
             for (Version requestVersion : VERSIONS) {
              l.add(new Object[] {requestVersion, scheme, method, connectTimeout, requestTimeout});
        }}}}
        return l.stream().toArray(Object[][]::new);
    }

    static final ProxySelector EXAMPLE_DOT_COM_PROXY = ProxySelector.of(
            InetSocketAddress.createUnresolved("example.com", 8080));

    //@Test(dataProvider = "variants")
    protected void timeoutNoProxySync(Version requestVersion,
                                      String scheme,
                                      String method,
                                      Duration connectTimeout,
                                      Duration requestTimeout)
        throws Exception
    {
        timeoutSync(requestVersion, scheme, method, connectTimeout, requestTimeout, NO_PROXY);
    }

    //@Test(dataProvider = "variants")
    protected void timeoutWithProxySync(Version requestVersion,
                                        String scheme,
                                        String method,
                                        Duration connectTimeout,
                                        Duration requestTimeout)
        throws Exception
    {
        timeoutSync(requestVersion, scheme, method, connectTimeout, requestTimeout, EXAMPLE_DOT_COM_PROXY);
    }

    private void timeoutSync(Version requestVersion,
                             String scheme,
                             String method,
                             Duration connectTimeout,
                             Duration requestTimeout,
                             ProxySelector proxy)
        throws Exception
    {
        out.printf("%ntimeoutSync(requestVersion=%s, scheme=%s, method=%s,"
                   + " connectTimeout=%s, requestTimeout=%s, proxy=%s)%n",
                   requestVersion, scheme, method, connectTimeout, requestTimeout, proxy);

        HttpClient client = newClient(connectTimeout, proxy);
        HttpRequest request = newRequest(scheme, requestVersion, method, requestTimeout);

        for (int i = 0; i < 2; i++) {
            out.printf("iteration %d%n", i);
            long startTime = System.nanoTime();
            try {
                HttpResponse<?> resp = client.send(request, BodyHandlers.ofString());
                printResponse(resp);
                fail("Unexpected response: " + resp);
            } catch (HttpConnectTimeoutException expected) { // blocking thread-specific exception
                long elapsedTime = NANOSECONDS.toMillis(System.nanoTime() - startTime);
                out.printf("Client: received in %d millis%n", elapsedTime);
                assertExceptionTypeAndCause(expected.getCause());
            } catch (ConnectException e) {
                long elapsedTime = NANOSECONDS.toMillis(System.nanoTime() - startTime);
                out.printf("Client: received in %d millis%n", elapsedTime);
                Throwable t = e.getCause().getCause();  // blocking thread-specific exception
                if (!isAcceptableCause(t)) { // tolerate only NRTHE or UAE
                    e.printStackTrace(out);
                    fail("Unexpected exception:" + e);
                } else {
                    out.printf("Caught ConnectException with "
                            + " cause: %s - skipping%n", t.getCause());
                }
            }
        }
    }

    //@Test(dataProvider = "variants")
    protected void timeoutNoProxyAsync(Version requestVersion,
                                       String scheme,
                                       String method,
                                       Duration connectTimeout,
                                       Duration requestTimeout) {
        timeoutAsync(requestVersion, scheme, method, connectTimeout, requestTimeout, NO_PROXY);
    }

    //@Test(dataProvider = "variants")
    protected void timeoutWithProxyAsync(Version requestVersion,
                                         String scheme,
                                         String method,
                                         Duration connectTimeout,
                                         Duration requestTimeout) {
        timeoutAsync(requestVersion, scheme, method, connectTimeout, requestTimeout, EXAMPLE_DOT_COM_PROXY);
    }

    private void timeoutAsync(Version requestVersion,
                              String scheme,
                              String method,
                              Duration connectTimeout,
                              Duration requestTimeout,
                              ProxySelector proxy) {
        out.printf("%ntimeoutAsync(requestVersion=%s, scheme=%s, method=%s, "
                   + "connectTimeout=%s, requestTimeout=%s, proxy=%s)%n",
                   requestVersion, scheme, method, connectTimeout, requestTimeout, proxy);

        HttpClient client = newClient(connectTimeout, proxy);
        HttpRequest request = newRequest(scheme, requestVersion, method, requestTimeout);
        for (int i = 0; i < 2; i++) {
            out.printf("iteration %d%n", i);
            long startTime = System.nanoTime();
            try {
                HttpResponse<?> resp = client.sendAsync(request, BodyHandlers.ofString()).join();
                printResponse(resp);
                fail("Unexpected response: " + resp);
            } catch (CompletionException e) {
                long elapsedTime = NANOSECONDS.toMillis(System.nanoTime() - startTime);
                out.printf("Client: received in %d millis%n", elapsedTime);
                Throwable t = e.getCause();
                if (t instanceof ConnectException && isAcceptableCause(t.getCause())) {
                    // tolerate only NRTHE and UAE
                    out.printf("Caught ConnectException with "
                            + "cause: %s - skipping%n", t.getCause());
                } else {
                    assertExceptionTypeAndCause(t);
                }
            }
        }
    }

    static boolean isAcceptableCause(Throwable cause) {
        if (cause instanceof NoRouteToHostException) return true;
        if (cause instanceof UnresolvedAddressException) return true;
        return false;
    }

    static HttpClient newClient(Duration connectTimeout, ProxySelector proxy) {
        HttpClient.Builder builder = HttpClient.newBuilder().proxy(proxy);
        if (connectTimeout != NO_DURATION)
            builder.connectTimeout(connectTimeout);
        return builder.build();
    }

    static HttpRequest newRequest(String scheme,
                                  Version reqVersion,
                                  String method,
                                  Duration requestTimeout) {
        // Resolvable address. Most tested environments just ignore the TCP SYN,
        // or occasionally return ICMP no route to host
        URI uri = URI.create(scheme +"://example.com:81/");
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(uri);
        reqBuilder = reqBuilder.version(reqVersion);
        switch (method) {
            case "GET"   : reqBuilder.GET();                         break;
            case "POST"  : reqBuilder.POST(BodyPublishers.noBody()); break;
            default: throw new AssertionError("Unknown method:" + method);
        }
        if (requestTimeout != NO_DURATION)
            reqBuilder.timeout(requestTimeout);
        return reqBuilder.build();
    }

    static void assertExceptionTypeAndCause(Throwable t) {
        if (!(t instanceof HttpConnectTimeoutException)) {
            t.printStackTrace(out);
            fail("Expected HttpConnectTimeoutException, got:" + t);
        }
        Throwable connEx = t.getCause();
        if (!(connEx instanceof ConnectException)) {
            t.printStackTrace(out);
            fail("Expected ConnectException cause in:" + connEx);
        }
        out.printf("Caught expected HttpConnectTimeoutException with ConnectException"
                + " cause: %n%s%n%s%n", t, connEx);
        final String EXPECTED_MESSAGE = "HTTP connect timed out"; // impl dependent
        if (!connEx.getMessage().equals(EXPECTED_MESSAGE))
            fail("Expected: \"" + EXPECTED_MESSAGE + "\", got: \"" + connEx.getMessage() + "\"");

    }

    static void printResponse(HttpResponse<?> response) {
        out.println("Unexpected response: " + response);
        out.println("Headers: " + response.headers());
        out.println("Body: " + response.body());
    }
}
