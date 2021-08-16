/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.lang.System.err;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * Dummy WebSocket Server.
 *
 * Performs simpler version of the WebSocket Opening Handshake over HTTP (i.e.
 * no proxying, cookies, etc.) Supports sequential connections, one at a time,
 * i.e. in order for a client to connect to the server the previous client must
 * disconnect first.
 *
 * Expected client request:
 *
 *     GET /chat HTTP/1.1
 *     Host: server.example.com
 *     Upgrade: websocket
 *     Connection: Upgrade
 *     Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
 *     Origin: http://example.com
 *     Sec-WebSocket-Protocol: chat, superchat
 *     Sec-WebSocket-Version: 13
 *
 * This server response:
 *
 *     HTTP/1.1 101 Switching Protocols
 *     Upgrade: websocket
 *     Connection: Upgrade
 *     Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
 *     Sec-WebSocket-Protocol: chat
 */
public class DummyWebSocketServer implements Closeable {

    private final AtomicBoolean started = new AtomicBoolean();
    private final Thread thread;
    private volatile ServerSocketChannel ssc;
    private volatile InetSocketAddress address;
    private ByteBuffer read = ByteBuffer.allocate(16384);
    private final CountDownLatch readReady = new CountDownLatch(1);
    private volatile int receiveBufferSize;

    private static class Credentials {
        private final String name;
        private final String password;
        private Credentials(String name, String password) {
            this.name = name;
            this.password = password;
        }
        public String name() { return name; }
        public String password() { return password; }
    }

    public DummyWebSocketServer() {
        this(defaultMapping(), null, null);
    }

    public DummyWebSocketServer(String username, String password) {
        this(defaultMapping(), username, password);
    }

    public DummyWebSocketServer(BiFunction<List<String>,Credentials,List<String>> mapping,
                                String username,
                                String password) {
        requireNonNull(mapping);
        Credentials credentials = username != null ?
                new Credentials(username, password) : null;

        thread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    err.println("Accepting next connection at: " + ssc);
                    SocketChannel channel = ssc.accept();
                    err.println("Accepted: " + channel);
                    try {
                        channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
                        channel.configureBlocking(true);
                        while (true) {
                            StringBuilder request = new StringBuilder();
                            if (!readRequest(channel, request)) {
                                throw new IOException("Bad request:[" + request + "]");
                            }
                            List<String> strings = asList(request.toString().split("\r\n"));
                            List<String> response = mapping.apply(strings, credentials);
                            writeResponse(channel, response);

                            if (response.get(0).startsWith("HTTP/1.1 401")) {
                                err.println("Sent 401 Authentication response " + channel);
                                continue;
                            } else {
                                serve(channel);
                                break;
                            }
                        }
                    } catch (IOException e) {
                        err.println("Error in connection: " + channel + ", " + e);
                    } finally {
                        err.println("Closed: " + channel);
                        closeChannel(channel);
                        readReady.countDown();
                    }
                }
            } catch (ClosedByInterruptException ignored) {
            } catch (Exception e) {
                e.printStackTrace(err);
            } finally {
                close(ssc);
                err.println("Stopped at: " + getURI());
            }
        });
        thread.setName("DummyWebSocketServer");
        thread.setDaemon(false);
    }

    protected void read(SocketChannel ch) throws IOException {
        // Read until the thread is interrupted or an error occurred
        // or the input is shutdown
        ByteBuffer b = ByteBuffer.allocate(65536);
        while (ch.read(b) != -1) {
            b.flip();
            if (read.remaining() < b.remaining()) {
                int required = read.capacity() - read.remaining() + b.remaining();
                int log2required = 32 - Integer.numberOfLeadingZeros(required - 1);
                ByteBuffer newBuffer = ByteBuffer.allocate(1 << log2required);
                newBuffer.put(read.flip());
                read = newBuffer;
            }
            read.put(b);
            b.clear();
        }
    }

    protected void closeChannel(SocketChannel channel) {
        close(channel);
    }

    protected void write(SocketChannel ch) throws IOException { }

    protected final void serve(SocketChannel channel)
            throws InterruptedException
    {
        Thread reader = new Thread(() -> {
            try {
                read(channel);
            } catch (IOException ignored) { }
        });
        Thread writer = new Thread(() -> {
            try {
                write(channel);
            } catch (IOException ignored) { }
        });
        reader.start();
        writer.start();
        try {
            reader.join();
        } finally {
            reader.interrupt();
            try {
                writer.join();
            } finally {
                writer.interrupt();
            }
        }
    }

    public ByteBuffer read() throws InterruptedException {
        readReady.await();
        return read.duplicate().asReadOnlyBuffer().flip();
    }

    public void setReceiveBufferSize(int bufsize) {
        assert ssc == null : "Must configure before calling open()";
        this.receiveBufferSize = bufsize;
    }

    public void open() throws IOException {
        err.println("Starting");
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Already started");
        }
        ssc = ServerSocketChannel.open();
        try {
            ssc.configureBlocking(true);
            var bufsize = receiveBufferSize;
            if (bufsize > 0) {
                err.printf("Configuring receive buffer size to %d%n", bufsize);
                try {
                    ssc.setOption(StandardSocketOptions.SO_RCVBUF, bufsize);
                } catch (IOException x) {
                    err.printf("Failed to configure receive buffer size to %d%n", bufsize);
                }
            }
            ssc.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            address = (InetSocketAddress) ssc.getLocalAddress();
            thread.start();
        } catch (IOException e) {
            close(ssc);
            throw e;
        }
        err.println("Started at: " + getURI());
    }

    @Override
    public void close() {
        err.println("Stopping: " + getURI());
        thread.interrupt();
        close(ssc);
    }

    URI getURI() {
        if (!started.get()) {
            throw new IllegalStateException("Not yet started");
        }
        return URI.create("ws://localhost:" + address.getPort());
    }

    private boolean readRequest(SocketChannel channel, StringBuilder request)
            throws IOException
    {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        while (channel.read(buffer) != -1) {
            // read the complete HTTP request headers, there should be no body
            CharBuffer decoded;
            buffer.flip();
            try {
                decoded = ISO_8859_1.newDecoder().decode(buffer);
            } catch (CharacterCodingException e) {
                throw new UncheckedIOException(e);
            }
            request.append(decoded);
            if (Pattern.compile("\r\n\r\n").matcher(request).find())
                return true;
            buffer.clear();
        }
        return false;
    }

    private void writeResponse(SocketChannel channel, List<String> response)
            throws IOException
    {
        String s = response.stream().collect(Collectors.joining("\r\n"))
                + "\r\n\r\n";
        ByteBuffer encoded;
        try {
            encoded = ISO_8859_1.newEncoder().encode(CharBuffer.wrap(s));
        } catch (CharacterCodingException e) {
            throw new UncheckedIOException(e);
        }
        while (encoded.hasRemaining()) {
            channel.write(encoded);
        }
    }

    private static BiFunction<List<String>,Credentials,List<String>> defaultMapping() {
        return (request, credentials) -> {
            List<String> response = new LinkedList<>();
            Iterator<String> iterator = request.iterator();
            if (!iterator.hasNext()) {
                throw new IllegalStateException("The request is empty");
            }
            String statusLine = iterator.next();
            if (!(statusLine.startsWith("GET /") && statusLine.endsWith(" HTTP/1.1"))) {
                throw new IllegalStateException
                        ("Unexpected status line: " + request.get(0));
            }
            response.add("HTTP/1.1 101 Switching Protocols");
            Map<String, List<String>> requestHeaders = new HashMap<>();
            while (iterator.hasNext()) {
                String header = iterator.next();
                String[] split = header.split(": ");
                if (split.length != 2) {
                    throw new IllegalStateException
                            ("Unexpected header: " + header
                                     + ", split=" + Arrays.toString(split));
                }
                requestHeaders.computeIfAbsent(split[0], k -> new ArrayList<>()).add(split[1]);

            }
            if (requestHeaders.containsKey("Sec-WebSocket-Protocol")) {
                throw new IllegalStateException("Subprotocols are not expected");
            }
            if (requestHeaders.containsKey("Sec-WebSocket-Extensions")) {
                throw new IllegalStateException("Extensions are not expected");
            }
            expectHeader(requestHeaders, "Connection", "Upgrade");
            response.add("Connection: Upgrade");
            expectHeader(requestHeaders, "Upgrade", "websocket");
            response.add("Upgrade: websocket");
            expectHeader(requestHeaders, "Sec-WebSocket-Version", "13");
            List<String> key = requestHeaders.get("Sec-WebSocket-Key");
            if (key == null || key.isEmpty()) {
                throw new IllegalStateException("Sec-WebSocket-Key is missing");
            }
            if (key.size() != 1) {
                throw new IllegalStateException("Sec-WebSocket-Key has too many values : " + key);
            }
            MessageDigest sha1 = null;
            try {
                sha1 = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                throw new InternalError(e);
            }
            String x = key.get(0) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            sha1.update(x.getBytes(ISO_8859_1));
            String v = Base64.getEncoder().encodeToString(sha1.digest());
            response.add("Sec-WebSocket-Accept: " + v);

            // check authorization credentials, if required by the server
            if (credentials != null && !authorized(credentials, requestHeaders)) {
                response.clear();
                response.add("HTTP/1.1 401 Unauthorized");
                response.add("Content-Length: 0");
                response.add("WWW-Authenticate: Basic realm=\"dummy server realm\"");
            }

            return response;
        };
    }

    // Checks credentials in the request against those allowable by the server.
    private static boolean authorized(Credentials credentials,
                                      Map<String,List<String>> requestHeaders) {
        List<String> authorization = requestHeaders.get("Authorization");
        if (authorization == null)
            return false;

        if (authorization.size() != 1) {
            throw new IllegalStateException("Authorization unexpected count:" + authorization);
        }
        String header = authorization.get(0);
        if (!header.startsWith("Basic "))
            throw new IllegalStateException("Authorization not Basic: " + header);

        header = header.substring("Basic ".length());
        String values = new String(Base64.getDecoder().decode(header), UTF_8);
        int sep = values.indexOf(':');
        if (sep < 1) {
            throw new IllegalStateException("Authorization not colon: " +  values);
        }
        String name = values.substring(0, sep);
        String password = values.substring(sep + 1);

        if (name.equals(credentials.name()) && password.equals(credentials.password()))
            return true;

        return false;
    }

    protected static String expectHeader(Map<String, List<String>> headers,
                                         String name,
                                         String value) {
        List<String> v = headers.get(name);
        if (v == null) {
            throw new IllegalStateException(
                    format("Expected '%s' header, not present in %s",
                           name, headers));
        }
        if (!v.contains(value)) {
            throw new IllegalStateException(
                    format("Expected '%s: %s', actual: '%s: %s'",
                           name, value, name, v)
            );
        }
        return value;
    }

    private static void close(AutoCloseable... acs) {
        for (AutoCloseable ac : acs) {
            try {
                ac.close();
            } catch (Exception ignored) { }
        }
    }
}
