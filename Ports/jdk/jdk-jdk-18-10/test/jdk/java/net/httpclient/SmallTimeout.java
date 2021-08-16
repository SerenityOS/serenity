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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import static java.lang.System.out;

/**
 * @test
 * @bug 8178147
 * @modules java.net.http/jdk.internal.net.http.common
 * @summary Ensures that small timeouts do not cause hangs due to race conditions
 * @run main/othervm -Djdk.internal.httpclient.debug=true SmallTimeout
 */

// To enable logging use. Not enabled by default as it changes the dynamics
// of the test.
// @run main/othervm -Djdk.httpclient.HttpClient.log=all,frames:all SmallTimeout

public class SmallTimeout {

    static int[] TIMEOUTS = {2, 1, 3, 2, 100, 1};

    // A queue for placing timed out requests so that their order can be checked.
    static LinkedBlockingQueue<HttpResult> queue = new LinkedBlockingQueue<>();

    static final class HttpResult {
         final HttpRequest request;
         final Throwable   failed;
         HttpResult(HttpRequest request, Throwable   failed) {
             this.request = request;
             this.failed = failed;
         }

         static HttpResult of(HttpRequest request) {
             return new HttpResult(request, null);
         }

         static HttpResult of(HttpRequest request, Throwable t) {
             return new HttpResult(request, t);
         }

    }

    static volatile boolean error;

    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        ReferenceTracker.INSTANCE.track(client);

        Throwable failed = null;
        try (ServerSocket ss = new ServerSocket()) {
            ss.setReuseAddress(false);
            ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            int port = ss.getLocalPort();
            URI u = new URI("http://localhost:" + port + "/");

            HttpRequest[] requests = new HttpRequest[TIMEOUTS.length];

            out.println("--- TESTING Async");
            for (int i = 0; i < TIMEOUTS.length; i++) {
                final int n = i;
                URI uri = new URI(u.toString() + "/r" + n);
                requests[i] = HttpRequest.newBuilder(uri)
                                         .timeout(Duration.ofMillis(TIMEOUTS[i]))
                                         .GET()
                                         .build();

                final HttpRequest req = requests[i];
                CompletableFuture<HttpResponse<Object>> response = client
                    .sendAsync(req, BodyHandlers.replacing(null))
                    .whenComplete((HttpResponse<Object> r, Throwable t) -> {
                        Throwable cause = null;
                        if (r != null) {
                            out.println("Unexpected response for r" + n + ": " + r);
                            cause = new RuntimeException("Unexpected response for r" + n);
                            error = true;
                        }
                        if (t != null) {
                            if (!(t.getCause() instanceof HttpTimeoutException)) {
                                out.println("Wrong exception type for r" + n + ":" + t.toString());
                                Throwable c = t.getCause() == null ? t : t.getCause();
                                c.printStackTrace();
                                cause = c;
                                error = true;
                            } else {
                                out.println("Caught expected timeout for r" + n +": " + t.getCause());
                            }
                        }
                        if (t == null && r == null) {
                            out.println("Both response and throwable are null for r" + n + "!");
                            cause = new RuntimeException("Both response and throwable are null for r"
                                    + n + "!");
                            error = true;
                        }
                        queue.add(HttpResult.of(req,cause));
                    });
            }
            System.out.println("All requests submitted. Waiting ...");

            checkReturn(requests);

            if (error)
                throw new RuntimeException("Failed. Check output");

            // Repeat blocking in separate threads. Use queue to wait.
            out.println("--- TESTING Sync");
            System.err.println("================= TESTING Sync =====================");

            // For running blocking response tasks
            ExecutorService executor = Executors.newCachedThreadPool();

            for (int i = 0; i < TIMEOUTS.length; i++) {
                final int n = i;
                URI uri = new URI(u.toString()+"/sync/r" + n);
                requests[i] = HttpRequest.newBuilder(uri)
                                         .timeout(Duration.ofMillis(TIMEOUTS[i]))
                                         .GET()
                                         .build();

                final HttpRequest req = requests[i];
                executor.execute(() -> {
                    Throwable cause = null;
                    try {
                        HttpResponse<?> r = client.send(req, BodyHandlers.replacing(null));
                        out.println("Unexpected success for r" + n +": " + r);
                    } catch (HttpTimeoutException e) {
                        out.println("Caught expected timeout for r" + n +": " + e);
                    } catch (Throwable ee) {
                        Throwable c = ee.getCause() == null ? ee : ee.getCause();
                        out.println("Unexpected exception for r" + n + ": " + c);
                        c.printStackTrace();
                        cause = c;
                        error = true;
                    } finally {
                        queue.offer(HttpResult.of(req, cause));
                    }
                });
            }
            System.out.println("All requests submitted. Waiting ...");

            checkReturn(requests);

            executor.shutdownNow();

            if (error)
                throw new RuntimeException("Failed. Check output");

        } catch (Throwable t) {
            failed = t;
            throw t;
        } finally {
            try {
                Thread.sleep(100);
            } catch (InterruptedException t) {
                // ignore;
            }
            AssertionError trackFailed = ReferenceTracker.INSTANCE.check(500);
            if (trackFailed != null) {
                if (failed != null) {
                    failed.addSuppressed(trackFailed);
                    if (failed instanceof Exception) throw (Exception) failed;
                    if (failed instanceof Error) throw (Exception) failed;
                }
                throw trackFailed;
            }
        }
    }

    static void checkReturn(HttpRequest[] requests) throws InterruptedException {
        // wait for exceptions and check order
        boolean ok = true;
        for (int j = 0; j < TIMEOUTS.length; j++) {
            HttpResult res = queue.take();
            HttpRequest req = res.request;
            out.println("Got request from queue " + req + ", order: " + getRequest(req, requests)
                         + (res.failed == null ? "" : " failed: " + res.failed));
            ok = ok && res.failed == null;
        }
        out.println("Return " + (ok ? "ok" : "nok"));
    }

    /** Returns the index of the request in the array. */
    static String getRequest(HttpRequest req, HttpRequest[] requests) {
        for (int i=0; i<requests.length; i++) {
            if (req == requests[i]) {
                return "r" + i;
            }
        }
        throw new AssertionError("Unknown request: " + req);
    }
}
