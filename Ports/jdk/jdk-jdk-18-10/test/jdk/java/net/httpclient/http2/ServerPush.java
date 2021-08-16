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

/*
 * @test
 * @bug 8087112 8159814
 * @library /test/lib server
 * @build jdk.test.lib.net.SimpleSSLContext
 * @modules java.base/sun.net.www.http
 *          java.net.http/jdk.internal.net.http.common
 *          java.net.http/jdk.internal.net.http.frame
 *          java.net.http/jdk.internal.net.http.hpack
 * @run testng/othervm
 *      -Djdk.httpclient.HttpClient.log=errors,requests,responses
 *      ServerPush
 */

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscribers;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.*;

public class ServerPush {

    static final int LOOPS = 13;
    static final int FILE_SIZE = 512 * 1024 + 343;

    static Path tempFile;

    Http2TestServer server;
    URI uri;

    @BeforeTest
    public void setup() throws Exception {
        tempFile = TestUtil.getAFile(FILE_SIZE);
        server = new Http2TestServer(false, 0);
        server.addHandler(new PushHandler(tempFile, LOOPS), "/");
        System.out.println("Using temp file:" + tempFile);

        System.err.println("Server listening on port " + server.getAddress().getPort());
        server.start();
        int port = server.getAddress().getPort();
        uri = new URI("http://localhost:" + port + "/foo/a/b/c");
    }

    @AfterTest
    public void teardown() {
        server.stop();
    }

    // Test 1 - custom written push promise handler, everything as a String
    @Test
    public void testTypeString() throws Exception {
        String tempFileAsString = new String(Files.readAllBytes(tempFile), UTF_8);
        ConcurrentMap<HttpRequest, CompletableFuture<HttpResponse<String>>>
                resultMap = new ConcurrentHashMap<>();

        PushPromiseHandler<String> pph = (initial, pushRequest, acceptor) -> {
            BodyHandler<String> s = BodyHandlers.ofString(UTF_8);
            CompletableFuture<HttpResponse<String>> cf = acceptor.apply(s);
            resultMap.put(pushRequest, cf);
        };

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        CompletableFuture<HttpResponse<String>> cf =
                client.sendAsync(request, BodyHandlers.ofString(UTF_8), pph);
        cf.join();
        resultMap.put(request, cf);
        System.err.println("results.size: " + resultMap.size());
        for (HttpRequest r : resultMap.keySet()) {
            HttpResponse<String> response = resultMap.get(r).join();
            assertEquals(response.statusCode(), 200);
            assertEquals(response.body(), tempFileAsString);
        }
        assertEquals(resultMap.size(), LOOPS + 1);
    }

    // Test 2 - of(...) populating the given Map, everything as a String
    @Test
    public void testTypeStringOfMap() throws Exception {
        String tempFileAsString = new String(Files.readAllBytes(tempFile), UTF_8);
        ConcurrentMap<HttpRequest, CompletableFuture<HttpResponse<String>>>
                resultMap = new ConcurrentHashMap<>();

        PushPromiseHandler<String> pph =
                PushPromiseHandler.of(pushPromise -> BodyHandlers.ofString(UTF_8),
                                      resultMap);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        CompletableFuture<HttpResponse<String>> cf =
                client.sendAsync(request, BodyHandlers.ofString(UTF_8), pph);
        cf.join();
        resultMap.put(request, cf);
        System.err.println("results.size: " + resultMap.size());
        for (HttpRequest r : resultMap.keySet()) {
            HttpResponse<String> response = resultMap.get(r).join();
            assertEquals(response.statusCode(), 200);
            assertEquals(response.body(), tempFileAsString);
        }
        assertEquals(resultMap.size(), LOOPS + 1);
    }

    // --- Path ---

    static final Path dir = Paths.get(".", "serverPush");
    static BodyHandler<Path> requestToPath(HttpRequest req) {
        URI u = req.uri();
        Path path = Paths.get(dir.toString(), u.getPath());
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException ee) {
            throw new UncheckedIOException(ee);
        }
        return BodyHandlers.ofFile(path);
    }

    // Test 3 - custom written push promise handler, everything as a Path
    @Test
    public void testTypePath() throws Exception {
        String tempFileAsString = new String(Files.readAllBytes(tempFile), UTF_8);
        ConcurrentMap<HttpRequest, CompletableFuture<HttpResponse<Path>>> resultsMap
                = new ConcurrentHashMap<>();

        PushPromiseHandler<Path> pushPromiseHandler = (initial, pushRequest, acceptor) -> {
            BodyHandler<Path> pp = requestToPath(pushRequest);
            CompletableFuture<HttpResponse<Path>> cf = acceptor.apply(pp);
            resultsMap.put(pushRequest, cf);
        };

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        CompletableFuture<HttpResponse<Path>> cf =
                client.sendAsync(request, requestToPath(request), pushPromiseHandler);
        cf.join();
        resultsMap.put(request, cf);

        for (HttpRequest r : resultsMap.keySet()) {
            HttpResponse<Path> response = resultsMap.get(r).join();
            assertEquals(response.statusCode(), 200);
            String fileAsString = new String(Files.readAllBytes(response.body()), UTF_8);
            assertEquals(fileAsString, tempFileAsString);
        }
        assertEquals(resultsMap.size(),  LOOPS + 1);
    }

    // Test 4 - of(...) populating the given Map, everything as a Path
    @Test
    public void testTypePathOfMap() throws Exception {
        String tempFileAsString = new String(Files.readAllBytes(tempFile), UTF_8);
        ConcurrentMap<HttpRequest, CompletableFuture<HttpResponse<Path>>> resultsMap
                = new ConcurrentHashMap<>();

        PushPromiseHandler<Path> pushPromiseHandler =
                PushPromiseHandler.of(pushRequest -> requestToPath(pushRequest),
                        resultsMap);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        CompletableFuture<HttpResponse<Path>> cf =
                client.sendAsync(request, requestToPath(request), pushPromiseHandler);
        cf.join();
        resultsMap.put(request, cf);

        for (HttpRequest r : resultsMap.keySet()) {
            HttpResponse<Path> response = resultsMap.get(r).join();
            assertEquals(response.statusCode(), 200);
            String fileAsString = new String(Files.readAllBytes(response.body()), UTF_8);
            assertEquals(fileAsString, tempFileAsString);
        }
        assertEquals(resultsMap.size(),  LOOPS + 1);
    }

    // ---  Consumer<byte[]> ---

    static class ByteArrayConsumer implements Consumer<Optional<byte[]>> {
        volatile List<byte[]> listByteArrays = new ArrayList<>();
        volatile byte[] accumulatedBytes;

        public byte[] getAccumulatedBytes() { return accumulatedBytes; }

        @Override
        public void accept(Optional<byte[]> optionalBytes) {
            assert accumulatedBytes == null;
            if (!optionalBytes.isPresent()) {
                int size = listByteArrays.stream().mapToInt(ba -> ba.length).sum();
                ByteBuffer bb = ByteBuffer.allocate(size);
                listByteArrays.stream().forEach(ba -> bb.put(ba));
                accumulatedBytes = bb.array();
            } else {
                listByteArrays.add(optionalBytes.get());
            }
        }
    }

    // Test 5 - custom written handler, everything as a consumer of optional byte[]
    @Test
    public void testTypeByteArrayConsumer() throws Exception {
        String tempFileAsString = new String(Files.readAllBytes(tempFile), UTF_8);
        ConcurrentMap<HttpRequest, CompletableFuture<HttpResponse<Void>>> resultsMap
                = new ConcurrentHashMap<>();
        Map<HttpRequest,ByteArrayConsumer> byteArrayConsumerMap
                = new ConcurrentHashMap<>();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        ByteArrayConsumer bac = new ByteArrayConsumer();
        byteArrayConsumerMap.put(request, bac);

        PushPromiseHandler<Void> pushPromiseHandler = (initial, pushRequest, acceptor) -> {
            CompletableFuture<HttpResponse<Void>> cf = acceptor.apply(
                    (info) -> {
                        ByteArrayConsumer bc = new ByteArrayConsumer();
                        byteArrayConsumerMap.put(pushRequest, bc);
                        return BodySubscribers.ofByteArrayConsumer(bc); } );
            resultsMap.put(pushRequest, cf);
        };

        CompletableFuture<HttpResponse<Void>> cf =
                client.sendAsync(request, BodyHandlers.ofByteArrayConsumer(bac), pushPromiseHandler);
        cf.join();
        resultsMap.put(request, cf);

        for (HttpRequest r : resultsMap.keySet()) {
            HttpResponse<Void> response = resultsMap.get(r).join();
            assertEquals(response.statusCode(), 200);
            byte[] ba = byteArrayConsumerMap.get(r).getAccumulatedBytes();
            String result = new String(ba, UTF_8);
            assertEquals(result, tempFileAsString);
        }
        assertEquals(resultsMap.size(), LOOPS + 1);
    }

    // Test 6 - of(...) populating the given Map, everything as a consumer of optional byte[]
    @Test
    public void testTypeByteArrayConsumerOfMap() throws Exception {
        String tempFileAsString = new String(Files.readAllBytes(tempFile), UTF_8);
        ConcurrentMap<HttpRequest, CompletableFuture<HttpResponse<Void>>> resultsMap
                = new ConcurrentHashMap<>();
        Map<HttpRequest,ByteArrayConsumer> byteArrayConsumerMap
                = new ConcurrentHashMap<>();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        ByteArrayConsumer bac = new ByteArrayConsumer();
        byteArrayConsumerMap.put(request, bac);

        PushPromiseHandler<Void> pushPromiseHandler =
                PushPromiseHandler.of(
                        pushRequest -> {
                            ByteArrayConsumer bc = new ByteArrayConsumer();
                            byteArrayConsumerMap.put(pushRequest, bc);
                            return BodyHandlers.ofByteArrayConsumer(bc);
                        },
                        resultsMap);

        CompletableFuture<HttpResponse<Void>> cf =
                client.sendAsync(request, BodyHandlers.ofByteArrayConsumer(bac), pushPromiseHandler);
        cf.join();
        resultsMap.put(request, cf);

        for (HttpRequest r : resultsMap.keySet()) {
            HttpResponse<Void> response = resultsMap.get(r).join();
            assertEquals(response.statusCode(), 200);
            byte[] ba = byteArrayConsumerMap.get(r).getAccumulatedBytes();
            String result = new String(ba, UTF_8);
            assertEquals(result, tempFileAsString);
        }
        assertEquals(resultsMap.size(), LOOPS + 1);
    }
}
