/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import static java.lang.System.err;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @test
 * @bug 8151441
 * @summary Request body of incorrect (larger or smaller) sizes than that
 *          reported by the body publisher
 * @run main/othervm ShortRequestBody
 */

public class ShortRequestBody {

    static final Path testSrc = Paths.get(System.getProperty("test.src", "."));

    // Some body types ( sources ) for testing.
    static final String STRING_BODY = "Hello world";
    static final byte[] BYTE_ARRAY_BODY = new byte[] {
        (byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE };
    static final Path FILE_BODY = testSrc.resolve("docs").resolve("files").resolve("foo.txt");

    // Body lengths and offsets ( amount to be wrong by ), to make coordination
    // between client and server easier.
    static final int[] BODY_LENGTHS = new int[] { STRING_BODY.length(),
                                                  BYTE_ARRAY_BODY.length,
                                                  fileSize(FILE_BODY) };
    static final int[] BODY_OFFSETS = new int[] { 0, +1, -1, +2, -2, +3, -3 };
    static final String MARKER = "ShortRequestBody";

    // A delegating Body Publisher. Subtypes will have a concrete body type.
    static abstract class AbstractDelegateRequestBody
            implements HttpRequest.BodyPublisher {

        final HttpRequest.BodyPublisher delegate;
        final long contentLength;

        AbstractDelegateRequestBody(HttpRequest.BodyPublisher delegate,
                                    long contentLength) {
            this.delegate = delegate;
            this.contentLength = contentLength;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
            delegate.subscribe(subscriber);
        }

        @Override
        public long contentLength() { return contentLength; /* may be wrong! */ }
    }

    // Request body Publishers that may generate a different number of actual
    // bytes to that of what is reported through their {@code contentLength}.

    static class StringRequestBody extends AbstractDelegateRequestBody {
        StringRequestBody(String body, int additionalLength) {
            super(HttpRequest.BodyPublishers.ofString(body),
                  body.getBytes(UTF_8).length + additionalLength);
        }
    }

    static class ByteArrayRequestBody extends AbstractDelegateRequestBody {
        ByteArrayRequestBody(byte[] body, int additionalLength) {
            super(BodyPublishers.ofByteArray(body),
                  body.length + additionalLength);
        }
    }

    static class FileRequestBody extends AbstractDelegateRequestBody {
        FileRequestBody(Path path, int additionalLength) throws IOException {
            super(BodyPublishers.ofFile(path),
                  Files.size(path) + additionalLength);
        }
    }

    // ---

    public static void main(String[] args) throws Exception {
        HttpClient sharedClient = HttpClient.newHttpClient();
        List<Supplier<HttpClient>> clientSuppliers = new ArrayList<>();
        clientSuppliers.add(() -> HttpClient.newHttpClient());
        clientSuppliers.add(() -> sharedClient);

        try (Server server = new Server()) {
            for (Supplier<HttpClient> cs : clientSuppliers) {
                err.println("\n---- next supplier ----\n");
                URI uri = new URI("http://localhost:" + server.getPort() + "/" + MARKER);

                // sanity ( 6 requests to keep client and server offsets easy to workout )
                success(cs, uri, new StringRequestBody(STRING_BODY, 0));
                success(cs, uri, new ByteArrayRequestBody(BYTE_ARRAY_BODY, 0));
                success(cs, uri, new FileRequestBody(FILE_BODY, 0));
                success(cs, uri, new StringRequestBody(STRING_BODY, 0));
                success(cs, uri, new ByteArrayRequestBody(BYTE_ARRAY_BODY, 0));
                success(cs, uri, new FileRequestBody(FILE_BODY, 0));

                for (int i = 1; i < BODY_OFFSETS.length; i++) {
                    failureBlocking(cs, uri, new StringRequestBody(STRING_BODY, BODY_OFFSETS[i]));
                    failureBlocking(cs, uri, new ByteArrayRequestBody(BYTE_ARRAY_BODY, BODY_OFFSETS[i]));
                    failureBlocking(cs, uri, new FileRequestBody(FILE_BODY, BODY_OFFSETS[i]));

                    failureNonBlocking(cs, uri, new StringRequestBody(STRING_BODY, BODY_OFFSETS[i]));
                    failureNonBlocking(cs, uri, new ByteArrayRequestBody(BYTE_ARRAY_BODY, BODY_OFFSETS[i]));
                    failureNonBlocking(cs, uri, new FileRequestBody(FILE_BODY, BODY_OFFSETS[i]));
                }
            }
        }
    }

    static void success(Supplier<HttpClient> clientSupplier,
                        URI uri,
                        HttpRequest.BodyPublisher publisher)
        throws Exception
    {
        CompletableFuture<HttpResponse<Void>> cf;
        HttpRequest request = HttpRequest.newBuilder(uri)
                                         .POST(publisher)
                                         .build();
        cf = clientSupplier.get().sendAsync(request, BodyHandlers.discarding());

        HttpResponse<Void> resp = cf.get(30, TimeUnit.SECONDS);
        err.println("Response code: " + resp.statusCode());
        check(resp.statusCode() == 200, null,
                "Expected 200, got ", resp.statusCode());
    }

    static void failureNonBlocking(Supplier<HttpClient> clientSupplier,
                                   URI uri,
                                   HttpRequest.BodyPublisher publisher)
        throws Exception
    {
        CompletableFuture<HttpResponse<Void>> cf;
        HttpRequest request = HttpRequest.newBuilder(uri)
                                         .POST(publisher)
                                         .build();
        cf = clientSupplier.get().sendAsync(request, BodyHandlers.discarding());

        try {
            HttpResponse<Void> r = cf.get(30, TimeUnit.SECONDS);
            throw new RuntimeException("Unexpected response: " + r.statusCode());
        } catch (TimeoutException x) {
            throw new RuntimeException("Unexpected timeout", x);
        } catch (ExecutionException expected) {
            err.println("Caught expected: " + expected);
            Throwable t = expected.getCause();
            check(t instanceof IOException, t,
                  "Expected cause IOException, but got: ", t);
            String msg = t.getMessage();
            check(msg.contains("Too many") || msg.contains("Too few"),
                    t, "Expected Too many|Too few, got: ", t);
        }
    }

    static void failureBlocking(Supplier<HttpClient> clientSupplier,
                                URI uri,
                                HttpRequest.BodyPublisher publisher)
        throws Exception
    {
        HttpRequest request = HttpRequest.newBuilder(uri)
                                         .POST(publisher)
                                         .build();
        try {
            HttpResponse<Void> r = clientSupplier.get()
                    .send(request, BodyHandlers.discarding());
            throw new RuntimeException("Unexpected response: " + r.statusCode());
        } catch (HttpTimeoutException x) {
            throw new RuntimeException("Unexpected timeout", x);
        } catch (IOException expected) {
            err.println("Caught expected: " + expected);
            String msg = expected.getMessage();
            check(msg.contains("Too many") || msg.contains("Too few"),
                    expected,"Expected Too many|Too few, got: ", expected);
        }
    }

    static class Server extends Thread implements AutoCloseable {

        static String RESPONSE = "HTTP/1.1 200 OK\r\n" +
                                 "Connection: close\r\n"+
                                 "Content-length: 0\r\n\r\n";

        private final ServerSocket ss;
        private volatile boolean closed;

        Server() throws IOException {
            super("Test-Server");
            ss = new ServerSocket();
            ss.setReuseAddress(false);
            ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            this.start();
        }

        int getPort() { return ss.getLocalPort(); }

        @Override
        public void run() {
            int count = 0;
            int offset = 0;

            while (!closed) {
                err.println("Server: waiting for connection");
                try (Socket s = ss.accept()) {
                    err.println("Server: got connection");
                    InputStream is = s.getInputStream();
                    try {
                        String headers = readRequestHeaders(is);
                        if (headers == null) continue;
                    } catch (SocketException ex) {
                        err.println("Ignoring unexpected exception while reading headers: " + ex);
                        ex.printStackTrace(err);
                        // proceed in order to update count etc..., even though
                        // we know that read() will fail;
                    }
                    byte[] ba = new byte[1024];

                    int length = BODY_LENGTHS[count % 3];
                    length += BODY_OFFSETS[offset];
                    err.println("Server: count=" + count + ", offset=" + offset);
                    err.println("Server: expecting " +length+ " bytes");
                    int read = 0;
                    try {
                        read = is.readNBytes(ba, 0, length);
                        err.println("Server: actually read " + read + " bytes");
                    } finally {
                        // Update the counts before replying, to prevent the
                        // client-side racing reset with this thread.
                        count++;
                        if (count % 6 == 0) // 6 is the number of failure requests per offset
                            offset++;
                        if (count % 42 == 0) {
                            count = 0;  // reset, for second iteration
                            offset = 0;
                        }
                    }
                    if (read < length) {
                        // no need to reply, client has already closed
                        // ensure closed
                        if (is.read() != -1)
                            new AssertionError("Unexpected read: " + read);
                    } else {
                        OutputStream os = s.getOutputStream();
                        err.println("Server: writing "
                                + RESPONSE.getBytes(US_ASCII).length + " bytes");
                        os.write(RESPONSE.getBytes(US_ASCII));
                    }
                } catch (Throwable e) {
                    if (!closed) {
                        err.println("Unexpected: " + e);
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void close() {
            if (closed)
                return;
            closed = true;
            try {
                ss.close();
            } catch (IOException e) {
                throw new UncheckedIOException("Unexpected", e);
            }
        }
    }

    static final byte[] requestEnd = new byte[] {'\r', '\n', '\r', '\n' };

    // Read until the end of a HTTP request headers
    static String readRequestHeaders(InputStream is) throws IOException {
        int requestEndCount = 0, r, eol = -1;
        StringBuilder headers = new StringBuilder();
        while ((r = is.read()) != -1) {
            if (r == '\r' && eol < 0) {
                eol = headers.length();
            }
            headers.append((char) r);
            if (r == requestEnd[requestEndCount]) {
                requestEndCount++;
                if (requestEndCount == 4) {
                    break;
                }
            } else {
                requestEndCount = 0;
            }
        }

        if (eol <= 0) return null;
        String requestLine = headers.toString().substring(0, eol);
        if (!requestLine.contains(MARKER)) return null;
        return headers.toString();
    }

    static int fileSize(Path p) {
        try { return (int) Files.size(p); }
        catch (IOException x) { throw new UncheckedIOException(x); }
    }

    static boolean check(boolean cond, Throwable t, Object... failedArgs) {
        if (cond)
            return true;
        // We are going to fail...
        StringBuilder sb = new StringBuilder();
        for (Object o : failedArgs)
                sb.append(o);
        throw new RuntimeException(sb.toString(), t);
    }
}
