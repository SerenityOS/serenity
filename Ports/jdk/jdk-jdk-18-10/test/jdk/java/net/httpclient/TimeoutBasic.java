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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import jdk.test.lib.net.SimpleSSLContext;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import static java.lang.System.out;

/**
 * @test
 * @library /test/lib
 * @build jdk.test.lib.net.SimpleSSLContext
 * @summary Basic tests for response timeouts
 * @run main/othervm TimeoutBasic
 */

public class TimeoutBasic {

    static List<Duration> TIMEOUTS = List.of(Duration.ofSeconds(1),
                                             Duration.ofMillis(100),
                                             Duration.ofNanos(99),
                                             Duration.ofNanos(1));

    static final List<Function<HttpRequest.Builder, HttpRequest.Builder>> METHODS =
            Arrays.asList(HttpRequest.Builder::GET,
                          TimeoutBasic::DELETE,
                          TimeoutBasic::PUT,
                          TimeoutBasic::POST,
                          null);

    static final List<HttpClient.Version> VERSIONS =
            Arrays.asList(HttpClient.Version.HTTP_2, HttpClient.Version.HTTP_1_1, null);

    static final List<String> SCHEMES = List.of("https", "http");

    static {
        try {
            SSLContext.setDefault(new SimpleSSLContext().get());
        } catch (IOException x) {
            throw new ExceptionInInitializerError(x);
        }
    }

    public static void main(String[] args) throws Exception {
        for (Function<HttpRequest.Builder, HttpRequest.Builder> m : METHODS) {
            for (HttpClient.Version version : List.of(HttpClient.Version.HTTP_1_1)) {
                for (HttpClient.Version reqVersion : VERSIONS) {
                    for (String scheme : SCHEMES) {
                        ServerSocketFactory ssf;
                        if (scheme.equalsIgnoreCase("https")) {
                            ssf = SSLServerSocketFactory.getDefault();
                        } else {
                            ssf = ServerSocketFactory.getDefault();
                        }
                        test(version, reqVersion, scheme, m, ssf);
                    }
                }
            }
        }
    }

    static HttpRequest.Builder DELETE(HttpRequest.Builder builder) {
        return builder.DELETE();
    }

    static HttpRequest.Builder PUT(HttpRequest.Builder builder) {
        HttpRequest.BodyPublisher noBody = HttpRequest.BodyPublishers.noBody();
        return builder.PUT(noBody);
    }

    static HttpRequest.Builder POST(HttpRequest.Builder builder) {
        HttpRequest.BodyPublisher noBody = HttpRequest.BodyPublishers.noBody();
        return builder.POST(noBody);
    }

    static HttpRequest newRequest(URI uri,
                                  Duration duration,
                                  HttpClient.Version reqVersion,
                                  Function<HttpRequest.Builder, HttpRequest.Builder> method) {
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(uri)
                .timeout(duration);
        if (method != null) reqBuilder = method.apply(reqBuilder);
        if (reqVersion != null) reqBuilder = reqBuilder.version(reqVersion);
        HttpRequest request = reqBuilder.build();
        if (duration.compareTo(Duration.ofSeconds(1)) >= 0) {
            if (method == null || !request.method().equalsIgnoreCase("get")) {
                out.println("Skipping " + duration + " for " + request.method());
                return null;
            }
        }
        return request;
    }

    public static void test(HttpClient.Version version,
                            HttpClient.Version reqVersion,
                            String scheme,
                            Function<HttpRequest.Builder, HttpRequest.Builder> method,
                            ServerSocketFactory ssf)
            throws Exception
    {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .proxy(HttpClient.Builder.NO_PROXY);
        if (version != null) builder.version(version);
        HttpClient client = builder.build();
        out.printf("%ntest(version=%s, reqVersion=%s, scheme=%s)%n", version, reqVersion, scheme);
        try (ServerSocket ss = ssf.createServerSocket()) {
            ss.setReuseAddress(false);
            ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            int port = ss.getLocalPort();
            URI uri = new URI(scheme +"://localhost:" + port + "/");

            out.println("--- TESTING Async");
            int count = 0;
            for (Duration duration : TIMEOUTS) {
                out.println("  with duration of " + duration);
                HttpRequest request = newRequest(uri, duration, reqVersion, method);
                if (request == null) continue;
                count++;
                try {
                    HttpResponse<?> resp = client.sendAsync(request, BodyHandlers.discarding()).join();
                    out.println("Unexpected response for: " + request);
                    out.println("\t from " + ss.getLocalSocketAddress());
                    out.println("Response is: " + resp);
                    out.println("Headers: " + resp.headers().map());
                    out.println("Body (should be null): " + resp.body());
                    throw new RuntimeException("Unexpected response: " + resp.statusCode());
                } catch (CompletionException e) {
                    if (!(e.getCause() instanceof HttpTimeoutException)) {
                        e.printStackTrace(out);
                        throw new RuntimeException("Unexpected exception: " + e.getCause());
                    } else {
                        out.println("Caught expected timeout: " + e.getCause());
                    }
                }
            }
            assert count >= TIMEOUTS.size() -1;

            out.println("--- TESTING Sync");
            count = 0;
            for (Duration duration : TIMEOUTS) {
                out.println("  with duration of " + duration);
                HttpRequest request = newRequest(uri, duration, reqVersion, method);
                if (request == null) continue;
                count++;
                try {
                    client.send(request, BodyHandlers.discarding());
                } catch (HttpTimeoutException e) {
                    out.println("Caught expected timeout: " + e);
                }
            }
            assert count >= TIMEOUTS.size() -1;

        }
    }
}
