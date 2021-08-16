/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.InetAddress;
import javax.net.ssl.*;
import java.net.URISyntaxException;
import java.net.http.HttpHeaders;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import jdk.internal.net.http.common.HttpHeadersBuilder;
import jdk.internal.net.http.frame.*;
import jdk.internal.net.http.hpack.Decoder;
import jdk.internal.net.http.hpack.DecodingCallback;
import jdk.internal.net.http.hpack.Encoder;
import sun.net.www.http.ChunkedInputStream;
import sun.net.www.http.HttpClient;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static jdk.internal.net.http.frame.SettingsFrame.HEADER_TABLE_SIZE;

/**
 * Represents one HTTP2 connection, either plaintext upgraded from HTTP/1.1
 * or HTTPS opened using "h2" ALPN.
 */
public class Http2TestServerConnection {
    final Http2TestServer server;
    @SuppressWarnings({"rawtypes","unchecked"})
    final Map<Integer, Queue> streams; // input q per stream
    final Map<Integer, BodyOutputStream> outStreams; // output q per stream
    final HashSet<Integer> pushStreams;
    final Queue<Http2Frame> outputQ;
    volatile int nextstream;
    final Socket socket;
    final Http2TestExchangeSupplier exchangeSupplier;
    final InputStream is;
    final OutputStream os;
    volatile Encoder hpackOut;
    volatile Decoder hpackIn;
    volatile SettingsFrame clientSettings;
    final SettingsFrame serverSettings;
    final ExecutorService exec;
    final boolean secure;
    final Properties properties;
    volatile boolean stopping;
    volatile int nextPushStreamId = 2;
    ConcurrentLinkedQueue<PingRequest> pings = new ConcurrentLinkedQueue<>();

    final static ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
    final static byte[] EMPTY_BARRAY = new byte[0];
    final Random random;

    final static byte[] clientPreface = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes();

    static class Sentinel extends Http2Frame {
        Sentinel() { super(-1,-1);}
    }

    static final Sentinel sentinel = new Sentinel();

    class PingRequest {
        final byte[] pingData;
        final long pingStamp;
        final CompletableFuture<Long> response;

        PingRequest() {
            pingData = new byte[8];
            random.nextBytes(pingData);
            pingStamp = System.currentTimeMillis();
            response = new CompletableFuture<>();
        }

        PingFrame frame() {
            return new PingFrame(0, pingData);
        }

        CompletableFuture<Long> response() {
            return response;
        }

        void success() {
            response.complete(System.currentTimeMillis() - pingStamp);
        }

        void fail(Throwable t) {
            response.completeExceptionally(t);
        }
    }

    Http2TestServerConnection(Http2TestServer server,
                              Socket socket,
                              Http2TestExchangeSupplier exchangeSupplier,
                              Properties properties)
        throws IOException
    {
        System.err.println("TestServer: New connection from " + socket);

        if (socket instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket)socket;
            handshake(server.serverName(), sslSocket);
            if (!server.supportsHTTP11 && !"h2".equals(sslSocket.getApplicationProtocol())) {
                throw new IOException("Unexpected ALPN: [" + sslSocket.getApplicationProtocol() + "]");
            }
        }
        this.server = server;
        this.exchangeSupplier = exchangeSupplier;
        this.streams = Collections.synchronizedMap(new HashMap<>());
        this.outStreams = Collections.synchronizedMap(new HashMap<>());
        this.outputQ = new Queue<>(sentinel);
        this.random = new Random();
        this.socket = socket;
        this.properties = properties;
        this.socket.setTcpNoDelay(true);
        this.serverSettings = getServerSettingProperties();
        this.exec = server.exec;
        this.secure = server.secure;
        this.pushStreams = new HashSet<>();
        is = new BufferedInputStream(socket.getInputStream());
        os = new BufferedOutputStream(socket.getOutputStream());
    }

    static final String propPrefix = "http2server.settings.";

    static final String[][] propIDs = {
        {"header_table_size", Integer.toString(SettingsFrame.HEADER_TABLE_SIZE)},
        {"enable_push", Integer.toString(SettingsFrame.ENABLE_PUSH)},
        {"max_concurrent_streams", Integer.toString(SettingsFrame.MAX_CONCURRENT_STREAMS)},
        {"initial_window_size", Integer.toString(SettingsFrame.INITIAL_WINDOW_SIZE)},
        {"max_frame_size", Integer.toString(SettingsFrame.MAX_FRAME_SIZE)},
        {"max_header_list_size", Integer.toString(SettingsFrame.MAX_HEADER_LIST_SIZE)}
    };

    private SettingsFrame getServerSettingProperties() {
        SettingsFrame s = SettingsFrame.defaultRFCSettings();
        if (properties == null)
            return s;
        for (int i=0; i<propIDs.length; i++) {
            String key = propIDs[i][0];
            String numS = propIDs[i][1];
            String prop = properties.getProperty(propPrefix + key);
            if (prop != null) {
                try {
                    System.err.println("TestServer: setting " + key + " property to: " +
                        prop);
                    int num = Integer.parseInt(numS);
                    System.err.println("TestServer: num = " + num);
                    s.setParameter(num, Integer.parseInt(prop));
                } catch (NumberFormatException e) {/* ignore errors */}
            }
        }
        return s;
    }

    /**
     * Sends a PING frame on this connection, and completes the returned
     * CF when the PING ack is received. The CF is given
     * an integer, whose value is the number of milliseconds
     * between PING and ACK.
     */
    CompletableFuture<Long> sendPing() {
        PingRequest ping = null;
        try {
            ping = new PingRequest();
            pings.add(ping);
            outputQ.put(ping.frame());
        } catch (Throwable t) {
            ping.fail(t);
        }
        return ping.response();
    }

    void goAway(int error) throws IOException {
        int laststream = nextstream >= 3 ? nextstream - 2 : 1;

        GoAwayFrame go = new GoAwayFrame(laststream, error);
        outputQ.put(go);
    }

    /**
     * Returns the first PingRequest from Queue
     */
    private PingRequest getNextRequest() {
        return pings.poll();
    }

    /**
     * Handles incoming Ping, which could be an ack
     * or a client originated Ping
     */
    void handlePing(PingFrame ping) throws IOException {
        if (ping.streamid() != 0) {
            System.err.println("Invalid ping received");
            close(ErrorFrame.PROTOCOL_ERROR);
            return;
        }
        if (ping.getFlag(PingFrame.ACK)) {
            // did we send a Ping?
            PingRequest request = getNextRequest();
            if (request == null) {
                System.err.println("Invalid ping ACK received");
                close(ErrorFrame.PROTOCOL_ERROR);
                return;
            } else if (!Arrays.equals(request.pingData, ping.getData())) {
                request.fail(new RuntimeException("Wrong ping data in ACK"));
            } else {
                request.success();
            }
        } else {
            // client originated PING. Just send it back with ACK set
            ping.setFlag(PingFrame.ACK);
            outputQ.put(ping);
        }
    }

    private static boolean compareIPAddrs(InetAddress addr1, String host) {
        try {
            InetAddress addr2 = InetAddress.getByName(host);
            return addr1.equals(addr2);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void handshake(String name, SSLSocket sock) throws IOException {
        if (name == null) {
            sock.getSession(); // awaits handshake completion
            return;
        } else if (name.equals("localhost")) {
            name = "localhost";
        }
        final String fname = name;
        final InetAddress addr1 = InetAddress.getByName(name);
        SSLParameters params = sock.getSSLParameters();
        SNIMatcher matcher = new SNIMatcher(StandardConstants.SNI_HOST_NAME) {
            public boolean matches (SNIServerName n) {
                String host = ((SNIHostName)n).getAsciiName();
                if (host.equals("localhost"))
                    host = "localhost";
                boolean cmp = host.equalsIgnoreCase(fname);
                if (cmp)
                    return true;
                return compareIPAddrs(addr1, host);
            }
        };
        List<SNIMatcher> list = List.of(matcher);
        params.setSNIMatchers(list);
        sock.setSSLParameters(params);
        sock.getSession(); // blocks until handshake done
    }

    void closeIncoming() {
        close(-1);
    }

    void close(int error) {
        if (stopping)
            return;
        stopping = true;
        System.err.printf("Server connection to %s stopping. %d streams\n",
            socket.getRemoteSocketAddress().toString(), streams.size());
        streams.forEach((i, q) -> {
            q.orderlyClose();
        });
        try {
            if (error != -1)
                goAway(error);
            outputQ.orderlyClose();
            socket.close();
        } catch (Exception e) {
        }
    }

    private void readPreface() throws IOException {
        int len = clientPreface.length;
        byte[] bytes = new byte[len];
        int n = is.readNBytes(bytes, 0, len);
        if (Arrays.compare(clientPreface, bytes) != 0) {
            System.err.printf("Invalid preface: read %d/%d bytes%n", n, len);
            throw new IOException("Invalid preface: " +
                    new String(bytes, 0, len, ISO_8859_1));
        }
    }

    Http1InitialRequest doUpgrade(Http1InitialRequest upgrade) throws IOException {
        String h2c = getHeader(upgrade.headers, "Upgrade");
        if (h2c == null || !h2c.equals("h2c")) {
            System.err.println("Server:HEADERS: " + upgrade);
            throw new IOException("Bad upgrade 1 " + h2c);
        }

        sendHttp1Response(101, "Switching Protocols", "Connection", "Upgrade",
                "Upgrade", "h2c");

        sendSettingsFrame();
        readPreface();

        String clientSettingsString = getHeader(upgrade.headers, "HTTP2-Settings");
        clientSettings = getSettingsFromString(clientSettingsString);

        return upgrade;
    }

    /**
     * Decodes the given, Client, settings payload provided in base64 HTTP1
     * header value.
     */
    private SettingsFrame getSettingsFromString(String s) throws IOException {
        Base64.Decoder decoder = Base64.getUrlDecoder();
        byte[] payload = decoder.decode(s);
        ByteBuffer bb1 = ByteBuffer.wrap(payload);
        // simulate header of Settings Frame
        ByteBuffer bb0 = ByteBuffer.wrap(
                new byte[] {0, 0, (byte)payload.length, 4, 0, 0, 0, 0, 0});
        List<Http2Frame> frames = new ArrayList<>();
        FramesDecoder reader = new FramesDecoder(frames::add);
        reader.decode(bb0);
        reader.decode(bb1);
        if (frames.size()!=1)
            throw new IOException("Expected 1 frame got "+frames.size()) ;
        Http2Frame frame = frames.get(0);
        if (!(frame instanceof SettingsFrame))
            throw new IOException("Expected SettingsFrame");
        return (SettingsFrame)frame;
    }

    public int getMaxFrameSize() {
        return clientSettings.getParameter(SettingsFrame.MAX_FRAME_SIZE);
    }

    /** Sends a pre-canned HTTP/1.1 response. */
    private void standardHTTP11Response(Http1InitialRequest request)
        throws IOException
    {
        String upgradeHeader = getHeader(request.headers, "Upgrade");
        if (upgradeHeader != null) {
            throw new IOException("Unexpected Upgrade header:" + upgradeHeader);
        }

        sendHttp1Response(200, "OK",
                          "Connection", "close",
                          "Content-Length", "0",
                          "X-Magic", "HTTP/1.1 request received by HTTP/2 server",
                          "X-Received-Body", new String(request.body, UTF_8));
    }

    void run() throws Exception {
        Http1InitialRequest upgrade = null;
        if (!secure) {
            Http1InitialRequest request = readHttp1Request();
            String h2c = getHeader(request.headers, "Upgrade");
            if (h2c == null || !h2c.equals("h2c")) {
                if (server.supportsHTTP11) {
                    standardHTTP11Response(request);
                    socket.close();
                    return;
                } else {
                    System.err.println("Server:HEADERS: " + upgrade);
                    throw new IOException("Bad upgrade 1 " + h2c);
                }
            }
            upgrade = doUpgrade(request);
        } else { // secure
            SSLSocket sslSocket = (SSLSocket)socket;
            if (sslSocket.getApplicationProtocol().equals("h2")) {
                readPreface();
                sendSettingsFrame(true);
                clientSettings = (SettingsFrame) readFrame();
                if (clientSettings.getFlag(SettingsFrame.ACK)) {
                    // we received the ack to our frame first
                    clientSettings = (SettingsFrame) readFrame();
                }
                nextstream = 1;
            } else if (sslSocket.getApplicationProtocol().equals("http/1.1") ||
                       sslSocket.getApplicationProtocol().equals("")) {
                standardHTTP11Response(readHttp1Request());
                socket.shutdownOutput();
                socket.close();
                return;
            } else {
                throw new IOException("Unexpected ALPN:" + sslSocket.getApplicationProtocol());
            }
        }

        // Uncomment if needed, but very noisy
        //System.out.println("ServerSettings: " + serverSettings);
        //System.out.println("ClientSettings: " + clientSettings);

        hpackOut = new Encoder(serverSettings.getParameter(HEADER_TABLE_SIZE));
        hpackIn = new Decoder(clientSettings.getParameter(HEADER_TABLE_SIZE));

        if (!secure) {
            createPrimordialStream(upgrade);
            nextstream = 3;
        }

        (new ConnectionThread("readLoop", this::readLoop)).start();
        (new ConnectionThread("writeLoop", this::writeLoop)).start();
    }

    class ConnectionThread extends Thread {
        final Runnable r;
        ConnectionThread(String name, Runnable r) {
            setName(name);
            setDaemon(true);
            this.r = r;
        }

        public void run() {
            r.run();
        }
    }

    private void writeFrame(Http2Frame frame) throws IOException {
        List<ByteBuffer> bufs = new FramesEncoder().encodeFrame(frame);
        //System.err.println("TestServer: Writing frame " + frame.toString());
        int c = 0;
        for (ByteBuffer buf : bufs) {
            byte[] ba = buf.array();
            int start = buf.arrayOffset() + buf.position();
            c += buf.remaining();
            os.write(ba, start, buf.remaining());

//            System.out.println("writing byte at a time");
//            while (buf.hasRemaining()) {
//                byte b = buf.get();
//                os.write(b);
//                os.flush();
//                try {
//                    Thread.sleep(1);
//                } catch(InterruptedException e) {
//                    UncheckedIOException uie = new UncheckedIOException(new IOException(""));
//                    uie.addSuppressed(e);
//                    throw uie;
//                }
//            }
        }
        os.flush();
        //System.err.printf("TestServer: wrote %d bytes\n", c);
    }

    private void handleCommonFrame(Http2Frame f) throws IOException {
        if (f instanceof SettingsFrame) {
            SettingsFrame sf = (SettingsFrame) f;
            if (sf.getFlag(SettingsFrame.ACK)) // ignore
            {
                return;
            }
            // otherwise acknowledge it
            clientSettings = sf;
            SettingsFrame frame = new SettingsFrame();
            frame.setFlag(SettingsFrame.ACK);
            frame.streamid(0);
            outputQ.put(frame);
            return;
        } else if (f instanceof GoAwayFrame) {
            System.err.println("Closing: "+ f.toString());
            close(ErrorFrame.NO_ERROR);
        } else if (f instanceof PingFrame) {
            handlePing((PingFrame)f);
        } else
            throw new UnsupportedOperationException("Not supported yet: " + f.toString());
    }

    void sendWindowUpdates(int len, int streamid) throws IOException {
        if (len == 0)
            return;
        WindowUpdateFrame wup = new WindowUpdateFrame(streamid, len);
        outputQ.put(wup);
        wup = new WindowUpdateFrame(0 , len);
        outputQ.put(wup);
    }

    HttpHeaders decodeHeaders(List<HeaderFrame> frames) throws IOException {
        HttpHeadersBuilder headersBuilder = createNewHeadersBuilder();

        DecodingCallback cb = (name, value) -> {
            headersBuilder.addHeader(name.toString(), value.toString());
        };

        for (HeaderFrame frame : frames) {
            List<ByteBuffer> buffers = frame.getHeaderBlock();
            for (ByteBuffer buffer : buffers) {
                hpackIn.decode(buffer, false, cb);
            }
        }
        hpackIn.decode(EMPTY_BUFFER, true, cb);
        return headersBuilder.build();
    }

    String getRequestLine(String request) {
        int eol = request.indexOf(CRLF);
        return request.substring(0, eol);
    }

    String getHeaders(String request) {
        int start = request.indexOf(CRLF);
        int end = request.indexOf(CRLFCRLF);
        if (start == -1 || end == -1) {
            throw new RuntimeException("Malformed request");
        }
        return request.substring(start,end);
    }

    static void addHeaders(String headersString, HttpHeadersBuilder headersBuilder) {
        String[] hh = headersString.split(CRLF);
        for (String header : hh) {
            int colon = header.indexOf(':');
            if (colon == -1)
                continue;
            String name = header.substring(0, colon);
            String value = header.substring(colon+1);
            while (value.startsWith(" "))
                value = value.substring(1);
            headersBuilder.addHeader(name, value);
        }
    }

    // First stream (1) comes from a plaintext HTTP/1.1 request
    @SuppressWarnings({"rawtypes","unchecked"})
    void createPrimordialStream(Http1InitialRequest request) throws IOException {
        HttpHeadersBuilder headersBuilder = createNewHeadersBuilder();
        String requestLine = getRequestLine(request.headers);
        String[] tokens = requestLine.split(" ");
        if (!tokens[2].equals("HTTP/1.1")) {
            throw new IOException("bad request line");
        }
        URI uri;
        try {
            uri = new URI(tokens[1]);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        String host = getHeader(request.headers, "Host");
        if (host == null) {
            throw new IOException("missing Host");
        }

        headersBuilder.setHeader(":method", tokens[0]);
        headersBuilder.setHeader(":scheme", "http"); // always in this case
        headersBuilder.setHeader(":authority", host);
        String path = uri.getRawPath();
        if (uri.getRawQuery() != null)
            path = path + "?" + uri.getRawQuery();
        headersBuilder.setHeader(":path", path);

        Queue q = new Queue(sentinel);
        byte[] body = getRequestBody(request);
        addHeaders(getHeaders(request.headers), headersBuilder);
        headersBuilder.setHeader("Content-length", Integer.toString(body.length));
        HttpHeaders headers = headersBuilder.build();

        addRequestBodyToQueue(body, q);
        streams.put(1, q);
        exec.submit(() -> {
            handleRequest(headers, q, 1, true /*complete request has been read*/);
        });
    }

    // all other streams created here
    @SuppressWarnings({"rawtypes","unchecked"})
    void createStream(HeaderFrame frame) throws IOException {
        List<HeaderFrame> frames = new LinkedList<>();
        frames.add(frame);
        int streamid = frame.streamid();
        if (streamid != nextstream) {
            throw new IOException("unexpected stream id");
        }
        nextstream += 2;

        boolean endStream = false;
        if (frame.getFlag(HeaderFrame.END_STREAM)) {
            endStream = true;
        }

        while (!frame.getFlag(HeaderFrame.END_HEADERS)) {
            Http2Frame f = readFrame();
            if (!(f instanceof HeaderFrame)) {
                handleCommonFrame(f); // should only be error frames
            } else {
                frame = (HeaderFrame) f;
                if (frame.getFlag(HeaderFrame.END_STREAM)) {
                    endStream = true;
                }
                frames.add(frame);
            }
        }
        boolean endStreamReceived = endStream;
        HttpHeaders headers = decodeHeaders(frames);

        // Strict to assert Client correctness. Not all servers are as strict,
        // but some are known to be.
        Optional<?> disallowedHeader = headers.firstValue("Upgrade");
        if (disallowedHeader.isPresent()) {
            throw new IOException("Unexpected Upgrade in headers:" + headers);
        }
        disallowedHeader = headers.firstValue("HTTP2-Settings");
        if (disallowedHeader.isPresent())
            throw new IOException("Unexpected HTTP2-Settings in headers:" + headers);


        Queue q = new Queue(sentinel);
        streams.put(streamid, q);
        exec.submit(() -> {
            handleRequest(headers, q, streamid, endStreamReceived);
        });
    }

    // runs in own thread. Handles request from start to finish. Incoming frames
    // for this stream/request delivered on Q

    @SuppressWarnings({"rawtypes","unchecked"})
    void handleRequest(HttpHeaders headers,
                       Queue queue,
                       int streamid,
                       boolean endStreamReceived)
    {
        String method = headers.firstValue(":method").orElse("");
        //System.out.println("method = " + method);
        String path = headers.firstValue(":path").orElse("");
        //System.out.println("path = " + path);
        String scheme = headers.firstValue(":scheme").orElse("");
        //System.out.println("scheme = " + scheme);
        String authority = headers.firstValue(":authority").orElse("");
        //System.out.println("authority = " + authority);
        System.err.printf("TestServer: %s %s\n", method, path);
        int winsize = clientSettings.getParameter(
                SettingsFrame.INITIAL_WINDOW_SIZE);
        //System.err.println ("Stream window size = " + winsize);

        final InputStream bis;
        if (endStreamReceived && queue.size() == 0) {
            System.err.println("Server: got END_STREAM for stream " + streamid);
            bis = NullInputStream.INSTANCE;
        } else {
            System.err.println("Server: creating input stream for stream " + streamid);
            bis = new BodyInputStream(queue, streamid, this);
        }
        try (bis;
             BodyOutputStream bos = new BodyOutputStream(streamid, winsize, this))
        {
            outStreams.put(streamid, bos);
            String us = scheme + "://" + authority + path;
            URI uri = new URI(us);
            boolean pushAllowed = clientSettings.getParameter(SettingsFrame.ENABLE_PUSH) == 1;
            HttpHeadersBuilder rspheadersBuilder = createNewHeadersBuilder();
            Http2TestExchange exchange = exchangeSupplier.get(streamid, method,
                    headers, rspheadersBuilder, uri, bis, getSSLSession(),
                    bos, this, pushAllowed);

            // give to user
            Http2Handler handler = server.getHandlerFor(uri.getPath());
            try {
                handler.handle(exchange);
            } catch (IOException closed) {
                if (bos.closed) {
                    Queue q = streams.get(streamid);
                    if (q != null && (q.isClosed() || q.isClosing())) {
                        System.err.println("TestServer: Stream " + streamid + " closed: " + closed);
                        return;
                    }
                }
                throw closed;
            }

            // everything happens in the exchange from here. Hopefully will
            // return though.
        } catch (Throwable e) {
            System.err.println("TestServer: handleRequest exception: " + e);
            e.printStackTrace();
            close(-1);
        }
    }

    protected HttpHeadersBuilder createNewHeadersBuilder() {
        return new HttpHeadersBuilder();
    }

    private SSLSession getSSLSession() {
        if (! (socket instanceof SSLSocket))
            return null;
        SSLSocket ssl = (SSLSocket)socket;
        return ssl.getSession();
    }
    // Runs in own thread

    @SuppressWarnings({"rawtypes","unchecked"})
    void readLoop() {
        try {
            while (!stopping) {
                Http2Frame frame = readFrameImpl();
                if (frame == null) {
                    closeIncoming();
                    return;
                }
                //System.err.printf("TestServer: received frame %s\n", frame);
                int stream = frame.streamid();
                int next = nextstream;
                int nextPush = nextPushStreamId;
                if (stream == 0) {
                    if (frame.type() == WindowUpdateFrame.TYPE) {
                        WindowUpdateFrame wup = (WindowUpdateFrame) frame;
                        updateConnectionWindow(wup.getUpdate());
                    } else {
                        // other common frame types
                        handleCommonFrame(frame);
                    }
                } else {
                    Queue q = streams.get(stream);
                    if (frame.type() == HeadersFrame.TYPE) {
                        if (q != null) {
                            System.err.println("HEADERS frame for existing stream! Error.");
                            // TODO: close connection
                            continue;
                        } else {
                            createStream((HeadersFrame) frame);
                        }
                    } else {
                        if (q == null && !pushStreams.contains(stream)) {
                            System.err.printf("Non Headers frame received with"+
                                    " non existing stream (%d) ", frame.streamid());
                            System.err.println(frame);
                            continue;
                        }
                        if (frame.type() == WindowUpdateFrame.TYPE) {
                            WindowUpdateFrame wup = (WindowUpdateFrame) frame;
                            synchronized (updaters) {
                                Consumer<Integer> r = updaters.get(stream);
                                r.accept(wup.getUpdate());
                            }
                        } else if (frame.type() == ResetFrame.TYPE) {
                            // do orderly close on input q
                            // and close the output q immediately
                            // This should mean depending on what the
                            // handler is doing: either an EOF on read
                            // or an IOException if writing the response.
                            if (q != null) {
                                q.orderlyClose();
                                BodyOutputStream oq = outStreams.get(stream);
                                if (oq != null)
                                    oq.closeInternal();
                            } else if (pushStreams.contains(stream)) {
                                // we could interrupt the pushStream's output
                                // but the continuation, even after a reset
                                // should be handle gracefully by the client
                                // anyway.
                            } else if (isClientStreamId(stream) && stream < next) {
                                // We may receive a reset on a client stream that has already
                                // been closed. Just ignore it.
                                System.err.println("TestServer: received ResetFrame on closed stream: " + stream);
                                System.err.println(frame);
                            } else if (isServerStreamId(stream) && stream < nextPush) {
                                // We may receive a reset on a push stream that has already
                                // been closed. Just ignore it.
                                System.err.println("TestServer: received ResetFrame on closed push stream: " + stream);
                                System.err.println(frame);
                            } else {
                                System.err.println("TestServer: Unexpected frame on: " + stream);
                                System.err.println(frame);
                                throw new IOException("Unexpected frame");
                            }
                        } else {
                            q.put(frame);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            if (!stopping) {
                System.err.println("Http server reader thread shutdown");
                e.printStackTrace();
            }
            close(ErrorFrame.PROTOCOL_ERROR);
        }
    }

    static boolean isClientStreamId(int streamid) {
        return (streamid & 0x01) == 0x01;
    }

    static boolean isServerStreamId(int streamid) {
        return (streamid & 0x01) == 0x00;
    }

    /** Encodes an group of headers, without any ordering guarantees. */
    List<ByteBuffer> encodeHeaders(HttpHeaders headers) {
        List<ByteBuffer> buffers = new LinkedList<>();

        ByteBuffer buf = getBuffer();
        boolean encoded;
        for (Map.Entry<String, List<String>> entry : headers.map().entrySet()) {
            List<String> values = entry.getValue();
            String key = entry.getKey().toLowerCase();
            for (String value : values) {
                do {
                    hpackOut.header(key, value);
                    encoded = hpackOut.encode(buf);
                    if (!encoded) {
                        buf.flip();
                        buffers.add(buf);
                        buf = getBuffer();
                    }
                } while (!encoded);
            }
        }
        buf.flip();
        buffers.add(buf);
        return buffers;
    }

    /** Encodes an ordered list of headers. */
    List<ByteBuffer> encodeHeadersOrdered(List<Map.Entry<String,String>> headers) {
        List<ByteBuffer> buffers = new LinkedList<>();

        ByteBuffer buf = getBuffer();
        boolean encoded;
        for (Map.Entry<String, String> entry : headers) {
            String value = entry.getValue();
            String key = entry.getKey().toLowerCase();
            do {
                hpackOut.header(key, value);
                encoded = hpackOut.encode(buf);
                if (!encoded) {
                    buf.flip();
                    buffers.add(buf);
                    buf = getBuffer();
                }
            } while (!encoded);
        }
        buf.flip();
        buffers.add(buf);
        return buffers;
    }

    static void closeIgnore(Closeable c) {
        try {
            c.close();
        } catch (IOException e) {}
    }

    // Runs in own thread
    void writeLoop() {
        try {
            while (!stopping) {
                Http2Frame frame;
                try {
                    frame = outputQ.take();
                    if (stopping)
                        break;
                } catch(IOException x) {
                    if (stopping && x.getCause() instanceof InterruptedException) {
                        break;
                    } else throw x;
                }
                if (frame instanceof ResponseHeaders) {
                    ResponseHeaders rh = (ResponseHeaders)frame;
                    HeadersFrame hf = new HeadersFrame(rh.streamid(), rh.getFlags(), encodeHeaders(rh.headers));
                    writeFrame(hf);
                } else if (frame instanceof OutgoingPushPromise) {
                    handlePush((OutgoingPushPromise)frame);
                } else
                    writeFrame(frame);
            }
            System.err.println("TestServer: Connection writer stopping");
        } catch (Throwable e) {
            e.printStackTrace();
            /*close();
            if (!stopping) {
                e.printStackTrace();
                System.err.println("TestServer: writeLoop exception: " + e);
            }*/
        }
    }

    private void handlePush(OutgoingPushPromise op) throws IOException {
        int promisedStreamid = nextPushStreamId;
        PushPromiseFrame pp = new PushPromiseFrame(op.parentStream,
                                                   HeaderFrame.END_HEADERS,
                                                   promisedStreamid,
                                                   encodeHeaders(op.headers),
                                                   0);
        pushStreams.add(promisedStreamid);
        nextPushStreamId += 2;
        pp.streamid(op.parentStream);
        writeFrame(pp);
        final InputStream ii = op.is;
        final BodyOutputStream oo = new BodyOutputStream(
                promisedStreamid,
                clientSettings.getParameter(
                        SettingsFrame.INITIAL_WINDOW_SIZE), this);
        outStreams.put(promisedStreamid, oo);
        oo.goodToGo();
        exec.submit(() -> {
            try {
                ResponseHeaders oh = getPushResponse(promisedStreamid);
                outputQ.put(oh);
                ii.transferTo(oo);
            } catch (Throwable ex) {
                System.err.printf("TestServer: pushing response error: %s\n",
                        ex.toString());
            } finally {
                closeIgnore(ii);
                closeIgnore(oo);
            }
        });

    }

    // returns a minimal response with status 200
    // that is the response to the push promise just sent
    private ResponseHeaders getPushResponse(int streamid) {
        HttpHeadersBuilder hb = createNewHeadersBuilder();
        hb.addHeader(":status", "200");
        ResponseHeaders oh = new ResponseHeaders(hb.build());
        oh.streamid(streamid);
        oh.setFlag(HeaderFrame.END_HEADERS);
        return oh;
    }

    private ByteBuffer getBuffer() {
        return ByteBuffer.allocate(8 * 1024);
    }

    private Http2Frame readFrame() throws IOException {
        Http2Frame f = readFrameImpl();
        if (f == null)
            throw new IOException("connection closed");
        return f;
    }

    // does not throw an exception for EOF
    private Http2Frame readFrameImpl() throws IOException {
        try {
            byte[] buf = new byte[9];
            int ret;
            ret=is.readNBytes(buf, 0, 9);
            if (ret == 0) {
                return null;
            } else if (ret != 9) {
                throw new IOException("readFrame: connection closed");
            }
            int len = 0;
            for (int i = 0; i < 3; i++) {
                int n = buf[i] & 0xff;
                //System.err.println("n = " + n);
                len = (len << 8) + n;
            }
            byte[] rest = new byte[len];
            int n = is.readNBytes(rest, 0, len);
            if (n != len)
                throw new IOException("Error reading frame");
            List<Http2Frame> frames = new ArrayList<>();
            FramesDecoder reader = new FramesDecoder(frames::add);
            reader.decode(ByteBuffer.wrap(buf));
            reader.decode(ByteBuffer.wrap(rest));
            if (frames.size()!=1)
                throw new IOException("Expected 1 frame got "+frames.size()) ;

            return frames.get(0);
        } catch (IOException ee) {
            if (stopping)
                return null;
            throw ee;
        }
    }

    void sendSettingsFrame() throws IOException {
        sendSettingsFrame(false);
    }

    void sendSettingsFrame(boolean now) throws IOException {
        if (now) {
            writeFrame(serverSettings);
        } else {
            outputQ.put(serverSettings);
        }
    }

    String readUntil(String end) throws IOException {
        int number = end.length();
        int found = 0;
        StringBuilder sb = new StringBuilder();
        while (found < number) {
            char expected = end.charAt(found);
            int c = is.read();
            if (c == -1) {
                throw new IOException("Connection closed");
            }
            char c0 = (char) c;
            sb.append(c0);
            if (c0 != expected) {
                found = 0;
                continue;
            }
            found++;
        }
        return sb.toString();
    }

    private int getContentLength(String headers) {
        return getIntHeader(headers, "Content-length");
    }

    private int getIntHeader(String headers, String name) {
        String val = getHeader(headers, name);
        if (val == null) {
            return -1;
        }
        return Integer.parseInt(val);
    }

    private String getHeader(String headers, String name) {
        String headers1 = headers.toLowerCase(); // not efficient
        name = CRLF + name.toLowerCase();
        int start = headers1.indexOf(name);
        if (start == -1) {
            return null;
        }
        start += 2;
        int end = headers1.indexOf(CRLF, start);
        String line = headers.substring(start, end);
        start = line.indexOf(':');
        if (start == -1) {
            return null;
        }
        return line.substring(start + 1).trim();
    }

    final static String CRLF = "\r\n";
    final static String CRLFCRLF = "\r\n\r\n";

    static class Http1InitialRequest {
        final String headers;
        final byte[] body;
        Http1InitialRequest(String headers, byte[] body) {
            this.headers = headers;
            this.body = body.clone();
        }
    }

    Http1InitialRequest readHttp1Request() throws IOException {
        String headers = readUntil(CRLF + CRLF);
        int clen = getContentLength(headers);
        String te = getHeader(headers, "Transfer-encoding");
        byte[] buf = new byte[0];
        try {
            if (clen >= 0) {
                // HTTP/1.1 fixed length content ( may be 0 ), read it
                buf = new byte[clen];
                is.readNBytes(buf, 0, clen);
            } else if ("chunked".equalsIgnoreCase(te)) {
                //  HTTP/1.1 chunked data, read it
                buf = readChunkedInputStream(is);
            }
            return new Http1InitialRequest(headers, buf);
        } catch (IOException e) {
            System.err.println("TestServer: headers read: [ " + headers + " ]");
            throw e;
        }
    }

    // This is a quick hack to get a chunked input stream reader.
    private static byte[] readChunkedInputStream(InputStream is) throws IOException {
        ChunkedInputStream cis = new ChunkedInputStream(is, new HttpClient() {}, null);
        return cis.readAllBytes();
    }

    void sendHttp1Response(int code, String msg, String... headers) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ")
                .append(code)
                .append(' ')
                .append(msg)
                .append(CRLF);
        int numheaders = headers.length;
        for (int i = 0; i < numheaders; i += 2) {
            sb.append(headers[i])
                    .append(": ")
                    .append(headers[i + 1])
                    .append(CRLF);
        }
        sb.append(CRLF);
        String s = sb.toString();
        os.write(s.getBytes("US-ASCII"));
        os.flush();
    }

    private void unexpectedFrame(Http2Frame frame) {
        System.err.println("OOPS. Unexpected");
        assert false;
    }

    final static ByteBuffer[] bbarray = new ByteBuffer[0];

    // wrapper around a BlockingQueue that throws an exception when it's closed
    // Each stream has one of these

    byte[] getRequestBody(Http1InitialRequest request) {
        return request.body;
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    void addRequestBodyToQueue(byte[] body, Queue q) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(body);
        DataFrame df = new DataFrame(1, DataFrame.END_STREAM, buf);
        // only used for primordial stream
        q.put(df);
    }

    // window updates done in main reader thread because they may
    // be used to unblock BodyOutputStreams waiting for WUPs

    HashMap<Integer,Consumer<Integer>> updaters = new HashMap<>();

    void registerStreamWindowUpdater(int streamid, Consumer<Integer> r) {
        synchronized(updaters) {
            updaters.put(streamid, r);
        }
    }

    int sendWindow = 64 * 1024 - 1; // connection level send window

    /**
     * BodyOutputStreams call this to get the connection window first.
     *
     * @param amount
     */
    synchronized void obtainConnectionWindow(int amount) throws InterruptedException {
        while (amount > 0) {
            int n = Math.min(amount, sendWindow);
            amount -= n;
            sendWindow -= n;
            if (amount > 0)
                wait();
        }
    }

    synchronized void updateConnectionWindow(int amount) {
        sendWindow += amount;
        notifyAll();
    }

    // simplified output headers class. really just a type safe container
    // for the hashmap.

    static class ResponseHeaders extends Http2Frame {
        HttpHeaders headers;

        ResponseHeaders(HttpHeaders headers) {
            super(0, 0);
            this.headers = headers;
        }

    }

    static class NullInputStream extends InputStream {
        static final NullInputStream INSTANCE = new NullInputStream();
        private NullInputStream() {}
        public int read()      { return -1; }
        public int available() { return 0;  }
    }
}
