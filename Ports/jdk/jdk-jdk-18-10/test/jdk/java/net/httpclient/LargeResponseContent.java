/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import jdk.test.lib.net.URIBuilder;

/**
 * @test
 * @bug 8212926
 * @library /test/lib
 * @summary Basic tests for response timeouts
 * @run main/othervm LargeResponseContent
 */

public class LargeResponseContent {
    final ServerSocket server;
    final int port;

    public LargeResponseContent() throws Exception {
        server = new ServerSocket(0, 10, InetAddress.getLoopbackAddress());
        Thread serverThread = new Thread(this::handleConnection);
        serverThread.setDaemon(false);
        port = server.getLocalPort();
        serverThread.start();
    }

    void runClient() throws IOException, InterruptedException {
        URI uri = URIBuilder.newBuilder()
            .scheme("http")
            .loopback()
            .port(port)
            .path("/foo")
            .buildUnchecked();
        System.out.println("URI: " + uri);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .build();
        HttpResponse<Long> response = client.send(request, new ClientHandler());
        System.out.println("Response code = " + response.statusCode());
        long blen = response.body();
        if (blen != CONTENT_LEN)
            throw new RuntimeException("wrong content length");
    }

    public static void main(String[] args) throws Exception {
        System.out.println ("CONTENT_LEN = " + CONTENT_LEN);
        System.out.println ("CLEN_STR = " + CLEN_STR);
        LargeResponseContent test = new LargeResponseContent();
        test.runClient();
    }

    static class ClientHandler implements HttpResponse.BodyHandler<Long> {

        @Override
        public HttpResponse.BodySubscriber<Long> apply(HttpResponse.ResponseInfo responseInfo) {
            HttpHeaders headers = responseInfo.headers();
            headers.firstValue("content-length");
            long  clen = headers.firstValueAsLong("content-length").orElse(-1);
            if (clen != CONTENT_LEN)
                return new Subscriber(new RuntimeException("Wrong content length received"));
            return new Subscriber(null);
        }
    }

    static class Subscriber implements HttpResponse.BodySubscriber<Long> {
        final CompletableFuture<Long> cf = new CompletableFuture<>();
        volatile Flow.Subscription subscription;
        volatile long counter = 0;

        Subscriber(Throwable t) {
            if (t != null)
                cf.completeExceptionally(t);
        }

        @Override
        public CompletionStage<Long> getBody() {
            return cf;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            long v = 0;
            for (ByteBuffer b : item)
                v+= b.remaining();
            counter += v;
        }

        @Override
        public void onError(Throwable throwable) {
            throwable.printStackTrace();
        }

        @Override
        public void onComplete() {
            cf.complete(counter);
        }
    }

    static final long CONTENT_LEN = Integer.MAX_VALUE + 1000L;
    static final String CLEN_STR = Long.valueOf(CONTENT_LEN).toString();

    static String RESPONSE = "HTTP/1.1 200 OK\r\n" +
            "Content-length: " + CLEN_STR + "\r\n" +
            "\r\n";


    void readHeaders(InputStream is) throws IOException {
        String s = "";
        byte[] buf = new byte[128];
        while (!s.endsWith("\r\n\r\n")) {
            int c = is.read(buf);
            String f = new String(buf, 0, c, StandardCharsets.ISO_8859_1);
            s = s + f;
        }
    }

    public void handleConnection() {
        long remaining = CONTENT_LEN;
        try {
            Socket socket = server.accept();
            InputStream is = socket.getInputStream();
            readHeaders(is); // read first byte
            OutputStream os = socket.getOutputStream();
            os.write(RESPONSE.getBytes());
            byte[] buf = new byte[64 * 1024];
            while (remaining > 0) {
                int amount = (int)Math.min(remaining, buf.length);
                os.write(buf, 0, amount);
                remaining -= amount;
            }
            System.out.println("Server: finished writing");
            os.close();

        } catch (IOException e) {
            long sent = CONTENT_LEN - remaining;
            System.out.println("Sent " + sent);
            e.printStackTrace();
        }
    }
}

