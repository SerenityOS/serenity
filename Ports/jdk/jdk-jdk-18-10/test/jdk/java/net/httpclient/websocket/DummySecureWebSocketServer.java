/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketOption;
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
 * Dummy WebSocket Server, which supports TLS.
 * By default the dummy webserver uses a plain TCP connection,
 * but it can use a TLS connection if secure() is called before
 * open(). It will use the default SSL context.
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
public class DummySecureWebSocketServer implements Closeable {

    /**
     * Emulates some of the SocketChannel APIs over a Socket
     * instance.
     */
    public static class WebSocketChannel implements AutoCloseable {
        interface Reader {
            int read(ByteBuffer buf) throws IOException;
        }
        interface Writer {
            void write(ByteBuffer buf) throws IOException;
        }
        interface Config {
            <T> void setOption(SocketOption<T> option, T value) throws IOException;
        }
        interface Closer {
            void close() throws IOException;
        }
        final AutoCloseable channel;
        final Reader reader;
        final Writer writer;
        final Config config;
        final Closer closer;
        WebSocketChannel(AutoCloseable channel, Reader reader, Writer writer, Config config, Closer closer) {
            this.channel = channel;
            this.reader = reader;
            this.writer = writer;
            this.config = config;
            this.closer = closer;
        }
        public void close() throws IOException {
            closer.close();
        }
        public String toString() {
            return channel.toString();
        }
        public int read(ByteBuffer bb) throws IOException {
            return reader.read(bb);
        }
        public void write(ByteBuffer bb) throws IOException {
            writer.write(bb);
        }
        public <T> void setOption(SocketOption<T> option, T value) throws IOException {
            config.setOption(option, value);
        }
        public static WebSocketChannel of(Socket s) {
            Reader reader = (bb) -> DummySecureWebSocketServer.read(s.getInputStream(), bb);
            Writer writer = (bb) -> DummySecureWebSocketServer.write(s.getOutputStream(), bb);
            return new WebSocketChannel(s, reader, writer, s::setOption, s::close);
        }
    }

    /**
     * Emulates some of the ServerSocketChannel APIs over a ServerSocket
     * instance.
     */
    public static class WebServerSocketChannel implements AutoCloseable {
        interface Accepter {
            WebSocketChannel accept() throws IOException;
        }
        interface Binder {
            void bind(SocketAddress address) throws IOException;
        }
        interface Config {
            <T> void setOption(SocketOption<T> option, T value) throws IOException;
        }
        interface Closer {
            void close() throws IOException;
        }
        interface Addressable {
            SocketAddress getLocalAddress() throws IOException;
        }
        final AutoCloseable server;
        final Accepter accepter;
        final Binder binder;
        final Addressable address;
        final Config config;
        final Closer closer;
        WebServerSocketChannel(AutoCloseable server,
                               Accepter accepter,
                               Binder binder,
                               Addressable address,
                               Config config,
                               Closer closer) {
            this.server = server;
            this.accepter = accepter;
            this.binder = binder;
            this.address = address;
            this.config = config;
            this.closer = closer;
        }
        public void close() throws IOException {
            closer.close();
        }
        public String toString() {
            return server.toString();
        }
        public WebSocketChannel accept() throws IOException {
            return accepter.accept();
        }
        public void bind(SocketAddress address) throws IOException {
            binder.bind(address);
        }
        public <T> void setOption(SocketOption<T> option, T value) throws IOException {
            config.setOption(option, value);
        }
        public SocketAddress getLocalAddress()  throws IOException {
            return address.getLocalAddress();
        }
        public static WebServerSocketChannel of(ServerSocket ss) {
            Accepter a = () -> WebSocketChannel.of(ss.accept());
            return new WebServerSocketChannel(ss, a, ss::bind, ss::getLocalSocketAddress, ss::setOption, ss::close);
        }
    }

    // Creates a secure WebServerSocketChannel
    static WebServerSocketChannel openWSS() throws IOException {
       return WebServerSocketChannel.of(SSLServerSocketFactory.getDefault().createServerSocket());
    }

    // Creates a plain WebServerSocketChannel
    static WebServerSocketChannel openWS() throws IOException {
        return WebServerSocketChannel.of(ServerSocketFactory.getDefault().createServerSocket());
    }


    static int read(InputStream str, ByteBuffer buffer) throws IOException {
        int len = Math.min(buffer.remaining(), 1024);
        if (len <= 0) return 0;
        byte[] bytes = new byte[len];
        int res = 0;
        if (buffer.hasRemaining()) {
            len = Math.min(len, buffer.remaining());
            int n = str.read(bytes, 0, len);
            if (n > 0) {
                buffer.put(bytes, 0, n);
                res += n;
            } else if (res > 0) {
                return res;
            } else {
                return n;
            }
        }
        return res;
    }

    static void write(OutputStream str, ByteBuffer buffer) throws IOException {
        int len = Math.min(buffer.remaining(), 1024);
        if (len <= 0) return;
        byte[] bytes = new byte[len];
        int res = 0;
        int pos = buffer.position();
        while (buffer.hasRemaining()) {
            len = Math.min(len, buffer.remaining());
            buffer.get(bytes, 0, len);
            str.write(bytes, 0, len);
        }
    }

    private final AtomicBoolean started = new AtomicBoolean();
    private final Thread thread;
    private volatile WebServerSocketChannel ss;
    private volatile InetSocketAddress address;
    private volatile boolean secure;
    private ByteBuffer read = ByteBuffer.allocate(16384);
    private final CountDownLatch readReady = new CountDownLatch(1);
    private volatile boolean done;

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

    public DummySecureWebSocketServer() {
        this(defaultMapping(), null, null);
    }

    public DummySecureWebSocketServer(String username, String password) {
        this(defaultMapping(), username, password);
    }

    public DummySecureWebSocketServer(BiFunction<List<String>,Credentials,List<String>> mapping,
                                String username,
                                String password) {
        requireNonNull(mapping);
        Credentials credentials = username != null ?
                new Credentials(username, password) : null;

        thread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted() && !done) {
                    err.println("Accepting next connection at: " + ss);
                    WebSocketChannel channel = ss.accept();
                    err.println("Accepted: " + channel);
                    try {
                        channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
                        while (!done) {
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
                        if (!done) {
                            err.println("Error in connection: " + channel + ", " + e);
                        }
                    } finally {
                        err.println("Closed: " + channel);
                        close(channel);
                        readReady.countDown();
                    }
                }
            } catch (ClosedByInterruptException ignored) {
            } catch (Throwable e) {
                if (!done) {
                    e.printStackTrace(err);
                }
            } finally {
                done = true;
                close(ss);
                err.println("Stopped at: " + getURI());
            }
        });
        thread.setName("DummySecureWebSocketServer");
        thread.setDaemon(false);
    }

    // must be called before open()
    public DummySecureWebSocketServer secure() {
        secure = true;
        return this;
    }

    protected void read(WebSocketChannel ch) throws IOException {
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

    protected void write(WebSocketChannel ch) throws IOException { }

    protected final void serve(WebSocketChannel channel)
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
            while (!done) {
                try {
                    reader.join(500);
                } catch (InterruptedException x) {
                    if (done) {
                        close(channel);
                        break;
                    }
                }
            }
        } finally {
            reader.interrupt();
            try {
                while (!done) {
                    try {
                        writer.join(500);
                    } catch (InterruptedException x) {
                        if (done) break;
                    }
                }
            } finally {
                writer.interrupt();
            }
        }
    }

    public ByteBuffer read() throws InterruptedException {
        readReady.await();
        return read.duplicate().asReadOnlyBuffer().flip();
    }

    public void open() throws IOException {
        err.println("Starting");
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Already started");
        }
        ss = secure ? openWSS() : openWS();
        try {
            ss.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            address = (InetSocketAddress) ss.getLocalAddress();
            thread.start();
        } catch (IOException e) {
            done = true;
            close(ss);
            throw e;
        }
        err.println("Started at: " + getURI());
    }

    @Override
    public void close() {
        err.println("Stopping: " + getURI());
        done = true;
        thread.interrupt();
        close(ss);
    }

    URI getURI() {
        if (!started.get()) {
            throw new IllegalStateException("Not yet started");
        }
        if (!secure) {
            return URI.create("ws://localhost:" + address.getPort());
        } else {
            return URI.create("wss://localhost:" + address.getPort());
        }
    }

    private boolean readRequest(WebSocketChannel channel, StringBuilder request)
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

    private void writeResponse(WebSocketChannel channel, List<String> response)
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
