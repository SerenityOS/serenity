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
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import static java.lang.System.out;

/**
 * @test
 * @summary Ensures that timeouts of multiple requests are handled in correct order
 * @run main/othervm TimeoutOrdering
 */

// To enable logging use
// @run main/othervm -Djdk.httpclient.HttpClient.log=all,frames:all TimeoutOrdering

public class TimeoutOrdering {

    // The assumption is that 5 secs is sufficiently large enough, without being
    // too large, to ensure the correct receive order of HttpTimeoutExceptions.
    static int[] TIMEOUTS = {10, 5, 15, 10, 10, 5};

    // A queue for placing timed out requests so that their order can be checked.
    static LinkedBlockingQueue<HttpRequest> queue = new LinkedBlockingQueue<>();

    static volatile boolean error;

    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        try (ServerSocket ss = new ServerSocket()) {
            ss.setReuseAddress(false);
            ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            int port = ss.getLocalPort();
            URI uri = new URI("http://localhost:" + port + "/");

            HttpRequest[] requests = new HttpRequest[TIMEOUTS.length];

            out.println("--- TESTING Async");
            for (int i = 0; i < TIMEOUTS.length; i++) {
                requests[i] = HttpRequest.newBuilder(uri)
                                         .timeout(Duration.ofSeconds(TIMEOUTS[i]))
                                         .GET()
                                         .build();

                final HttpRequest req = requests[i];
                final int j = i;
                CompletableFuture<HttpResponse<Object>> response = client
                    .sendAsync(req, BodyHandlers.replacing(null))
                    .whenComplete((HttpResponse<Object> r, Throwable t) -> {
                        if (r != null) {
                            out.println("Unexpected response for r" + j + ": " + r);
                            error = true;
                        }
                        if (t != null) {
                            if (!(t.getCause() instanceof HttpTimeoutException)) {
                                out.println("Wrong exception type for r" + j + ": " + t.toString());
                                Throwable c = t.getCause() == null ? t : t.getCause();
                                c.printStackTrace();
                                error = true;
                            } else {
                                out.println("Caught expected timeout for r" + j + ": " + t.getCause());
                            }
                        }
                        queue.add(req);
                    });
            }
            System.out.println("All requests submitted. Waiting ...");

            checkReturnOrder(requests);

            if (error)
                throw new RuntimeException("Failed. Check output");

            // Repeat blocking in separate threads. Use queue to wait.
            out.println("--- TESTING Sync");

            // For running blocking response tasks
            ExecutorService executor = Executors.newCachedThreadPool();

            for (int i = 0; i < TIMEOUTS.length; i++) {
                requests[i] = HttpRequest.newBuilder(uri)
                                         .timeout(Duration.ofSeconds(TIMEOUTS[i]))
                                         .GET()
                                         .build();

                final HttpRequest req = requests[i];
                final int j = i;
                executor.execute(() -> {
                    try {
                        HttpResponse<?> r = client.send(req, BodyHandlers.replacing(null));
                        out.println("Unexpected response for r" + j + ": " + r);
                        error = true;
                    } catch (HttpTimeoutException e) {
                        out.println("Caught expected timeout for r" + j +": " + e);
                    } catch (IOException | InterruptedException ee) {
                        Throwable c = ee.getCause() == null ? ee : ee.getCause();
                        out.println("Wrong exception type for r" + j + ": " + c.toString());
                        c.printStackTrace();
                        error = true;
                    } finally {
                        queue.offer(req);
                    }
                });
            }
            System.out.println("All requests submitted. Waiting ...");

            checkReturnOrder(requests);

            executor.shutdownNow();

            if (error)
                throw new RuntimeException("Failed. Check output");

        }
    }

    static void checkReturnOrder(HttpRequest[] requests) throws InterruptedException {
        // wait for exceptions and check order
        for (int j = 0; j < TIMEOUTS.length; j++) {
            HttpRequest req = queue.take();
            out.println("Got request from queue " + req + ", order: " + getRequest(req, requests));
            switch (j) {
                case 0:
                case 1:  // Expect shortest timeouts, 5sec, first.
                    if (!(req == requests[1] || req == requests[5])) {
                        String s = "Expected r1 or r5. Got: " + getRequest(req, requests);
                        throw new RuntimeException(s);
                    }
                    break;
                case 2:
                case 3:
                case 4: // Expect medium timeouts, 10sec, next.
                    if (!(req == requests[0] || req == requests[3] || req == requests[4])) {
                        String s = "Expected r1, r4 or r5. Got: " + getRequest(req, requests);
                        throw new RuntimeException(s);
                    }
                    break;
                case 5:  // Expect largest timeout, 15sec, last.
                    if (req != requests[2]) {
                        String s= "Expected r3. Got: " + getRequest(req, requests);
                        throw new RuntimeException(s);
                    }
                    break;
                default:
                    throw new AssertionError("Unknown index: " + j);
            }
        }
        out.println("Return order ok");
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
