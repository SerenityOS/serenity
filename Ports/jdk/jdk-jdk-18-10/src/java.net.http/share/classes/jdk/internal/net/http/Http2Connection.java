/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.internal.net.http;

import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Flow;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import jdk.internal.net.http.HttpConnection.HttpPublisher;
import jdk.internal.net.http.common.FlowTube;
import jdk.internal.net.http.common.FlowTube.TubeSubscriber;
import jdk.internal.net.http.common.HttpHeadersBuilder;
import jdk.internal.net.http.common.Log;
import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.MinimalFuture;
import jdk.internal.net.http.common.SequentialScheduler;
import jdk.internal.net.http.common.Utils;
import jdk.internal.net.http.frame.ContinuationFrame;
import jdk.internal.net.http.frame.DataFrame;
import jdk.internal.net.http.frame.ErrorFrame;
import jdk.internal.net.http.frame.FramesDecoder;
import jdk.internal.net.http.frame.FramesEncoder;
import jdk.internal.net.http.frame.GoAwayFrame;
import jdk.internal.net.http.frame.HeaderFrame;
import jdk.internal.net.http.frame.HeadersFrame;
import jdk.internal.net.http.frame.Http2Frame;
import jdk.internal.net.http.frame.MalformedFrame;
import jdk.internal.net.http.frame.OutgoingHeaders;
import jdk.internal.net.http.frame.PingFrame;
import jdk.internal.net.http.frame.PushPromiseFrame;
import jdk.internal.net.http.frame.ResetFrame;
import jdk.internal.net.http.frame.SettingsFrame;
import jdk.internal.net.http.frame.WindowUpdateFrame;
import jdk.internal.net.http.hpack.Encoder;
import jdk.internal.net.http.hpack.Decoder;
import jdk.internal.net.http.hpack.DecodingCallback;
import static java.nio.charset.StandardCharsets.UTF_8;
import static jdk.internal.net.http.frame.SettingsFrame.*;

/**
 * An Http2Connection. Encapsulates the socket(channel) and any SSLEngine used
 * over it. Contains an HttpConnection which hides the SocketChannel SSL stuff.
 *
 * Http2Connections belong to a Http2ClientImpl, (one of) which belongs
 * to a HttpClientImpl.
 *
 * Creation cases:
 * 1) upgraded HTTP/1.1 plain tcp connection
 * 2) prior knowledge directly created plain tcp connection
 * 3) directly created HTTP/2 SSL connection which uses ALPN.
 *
 * Sending is done by writing directly to underlying HttpConnection object which
 * is operating in async mode. No flow control applies on output at this level
 * and all writes are just executed as puts to an output Q belonging to HttpConnection
 * Flow control is implemented by HTTP/2 protocol itself.
 *
 * Hpack header compression
 * and outgoing stream creation is also done here, because these operations
 * must be synchronized at the socket level. Stream objects send frames simply
 * by placing them on the connection's output Queue. sendFrame() is called
 * from a higher level (Stream) thread.
 *
 * asyncReceive(ByteBuffer) is always called from the selector thread. It assembles
 * incoming Http2Frames, and directs them to the appropriate Stream.incoming()
 * or handles them directly itself. This thread performs hpack decompression
 * and incoming stream creation (Server push). Incoming frames destined for a
 * stream are provided by calling Stream.incoming().
 */
class Http2Connection  {

    final Logger debug = Utils.getDebugLogger(this::dbgString, Utils.DEBUG);
    final static Logger DEBUG_LOGGER =
            Utils.getDebugLogger("Http2Connection"::toString, Utils.DEBUG);
    private final Logger debugHpack =
            Utils.getHpackLogger(this::dbgString, Utils.DEBUG_HPACK);
    static final ByteBuffer EMPTY_TRIGGER = ByteBuffer.allocate(0);

    static private final int MAX_CLIENT_STREAM_ID = Integer.MAX_VALUE; // 2147483647
    static private final int MAX_SERVER_STREAM_ID = Integer.MAX_VALUE - 1; // 2147483646

    /**
     * Flag set when no more streams to be opened on this connection.
     * Two cases where it is used.
     *
     * 1. Two connections to the same server were opened concurrently, in which
     *    case one of them will be put in the cache, and the second will expire
     *    when all its opened streams (which usually should be a single client
     *    stream + possibly some additional push-promise server streams) complete.
     * 2. A cached connection reaches its maximum number of streams (~ 2^31-1)
     *    either server / or client allocated, in which case it will be taken
     *    out of the cache - allowing a new connection to replace it. It will
     *    expire when all its still open streams (which could be many) eventually
     *    complete.
     */
    private boolean finalStream;

    /*
     * ByteBuffer pooling strategy for HTTP/2 protocol.
     *
     * In general there are 4 points where ByteBuffers are used:
     *  - incoming/outgoing frames from/to ByteBuffers plus incoming/outgoing
     *    encrypted data in case of SSL connection.
     *
     * 1. Outgoing frames encoded to ByteBuffers.
     *
     *  Outgoing ByteBuffers are created with required size and frequently
     *  small (except DataFrames, etc). At this place no pools at all. All
     *  outgoing buffers should eventually be collected by GC.
     *
     * 2. Incoming ByteBuffers (decoded to frames).
     *
     *  Here, total elimination of BB pool is not a good idea.
     *  We don't know how many bytes we will receive through network.
     *
     *  A possible future improvement ( currently not implemented ):
     *  Allocate buffers of reasonable size. The following life of the BB:
     *   - If all frames decoded from the BB are other than DataFrame and
     *     HeaderFrame (and HeaderFrame subclasses) BB is returned to pool,
     *   - If a DataFrame is decoded from the BB. In that case DataFrame refers
     *     to sub-buffer obtained by slice(). Such a BB is never returned to the
     *     pool and will eventually be GC'ed.
     *   - If a HeadersFrame is decoded from the BB. Then header decoding is
     *     performed inside processFrame method and the buffer could be release
     *     back to pool.
     *
     * 3. SSL encrypted buffers ( received ).
     *
     *  The current implementation recycles encrypted buffers read from the
     *  channel. The pool of buffers has a maximum size of 3, SocketTube.MAX_BUFFERS,
     *  direct buffers which are shared by all connections on a given client.
     *  The pool is used by all SSL connections - whether HTTP/1.1 or HTTP/2,
     *  but only for SSL encrypted buffers that circulate between the SocketTube
     *  Publisher and the SSLFlowDelegate Reader. Limiting the pool to this
     *  particular segment allows the use of direct buffers, thus avoiding any
     *  additional copy in the NIO socket channel implementation. See
     *  HttpClientImpl.SSLDirectBufferSupplier, SocketTube.SSLDirectBufferSource,
     *  and SSLTube.recycler.
     */


    // A small class that allows to control frames with respect to the state of
    // the connection preface. Any data received before the connection
    // preface is sent will be buffered.
    private final class FramesController {
        volatile boolean prefaceSent;
        volatile List<ByteBuffer> pending;

        boolean processReceivedData(FramesDecoder decoder, ByteBuffer buf)
                throws IOException
        {
            // if preface is not sent, buffers data in the pending list
            if (!prefaceSent) {
                if (debug.on())
                    debug.log("Preface not sent: buffering %d", buf.remaining());
                synchronized (this) {
                    if (!prefaceSent) {
                        if (pending == null) pending = new ArrayList<>();
                        pending.add(buf);
                        if (debug.on())
                            debug.log("there are now %d bytes buffered waiting for preface to be sent"
                                    + Utils.remaining(pending)
                            );
                        return false;
                    }
                }
            }

            // Preface is sent. Checks for pending data and flush it.
            // We rely on this method being called from within the Http2TubeSubscriber
            // scheduler, so we know that no other thread could execute this method
            // concurrently while we're here.
            // This ensures that later incoming buffers will not
            // be processed before we have flushed the pending queue.
            // No additional synchronization is therefore necessary here.
            List<ByteBuffer> pending = this.pending;
            this.pending = null;
            if (pending != null) {
                // flush pending data
                if (debug.on()) debug.log(() -> "Processing buffered data: "
                      + Utils.remaining(pending));
                for (ByteBuffer b : pending) {
                    decoder.decode(b);
                }
            }
            // push the received buffer to the frames decoder.
            if (buf != EMPTY_TRIGGER) {
                if (debug.on()) debug.log("Processing %d", buf.remaining());
                decoder.decode(buf);
            }
            return true;
        }

        // Mark that the connection preface is sent
        void markPrefaceSent() {
            assert !prefaceSent;
            synchronized (this) {
                prefaceSent = true;
            }
        }
    }

    volatile boolean closed;

    //-------------------------------------
    final HttpConnection connection;
    private final Http2ClientImpl client2;
    private final ConcurrentMap<Integer,Stream<?>> streams = new ConcurrentHashMap<>();
    private int nextstreamid;
    private int nextPushStream = 2;
    // actual stream ids are not allocated until the Headers frame is ready
    // to be sent. The following two fields are updated as soon as a stream
    // is created and assigned to a connection. They are checked before
    // assigning a stream to a connection.
    private int lastReservedClientStreamid = 1;
    private int lastReservedServerStreamid = 0;
    private int numReservedClientStreams = 0; // count of current streams
    private int numReservedServerStreams = 0; // count of current streams
    private final Encoder hpackOut;
    private final Decoder hpackIn;
    final SettingsFrame clientSettings;
    private volatile SettingsFrame serverSettings;
    private final String key; // for HttpClientImpl.connections map
    private final FramesDecoder framesDecoder;
    private final FramesEncoder framesEncoder = new FramesEncoder();

    /**
     * Send Window controller for both connection and stream windows.
     * Each of this connection's Streams MUST use this controller.
     */
    private final WindowController windowController = new WindowController();
    private final FramesController framesController = new FramesController();
    private final Http2TubeSubscriber subscriber;
    final ConnectionWindowUpdateSender windowUpdater;
    private volatile Throwable cause;
    private volatile Supplier<ByteBuffer> initial;

    static final int DEFAULT_FRAME_SIZE = 16 * 1024;


    // TODO: need list of control frames from other threads
    // that need to be sent

    private Http2Connection(HttpConnection connection,
                            Http2ClientImpl client2,
                            int nextstreamid,
                            String key) {
        this.connection = connection;
        this.client2 = client2;
        this.subscriber = new Http2TubeSubscriber(client2.client());
        this.nextstreamid = nextstreamid;
        this.key = key;
        this.clientSettings = this.client2.getClientSettings();
        this.framesDecoder = new FramesDecoder(this::processFrame,
                clientSettings.getParameter(SettingsFrame.MAX_FRAME_SIZE));
        // serverSettings will be updated by server
        this.serverSettings = SettingsFrame.defaultRFCSettings();
        this.hpackOut = new Encoder(serverSettings.getParameter(HEADER_TABLE_SIZE));
        this.hpackIn = new Decoder(clientSettings.getParameter(HEADER_TABLE_SIZE));
        if (debugHpack.on()) {
            debugHpack.log("For the record:" + super.toString());
            debugHpack.log("Decoder created: %s", hpackIn);
            debugHpack.log("Encoder created: %s", hpackOut);
        }
        this.windowUpdater = new ConnectionWindowUpdateSender(this,
                client2.getConnectionWindowSize(clientSettings));
    }

    /**
     * Case 1) Create from upgraded HTTP/1.1 connection.
     * Is ready to use. Can't be SSL. exchange is the Exchange
     * that initiated the connection, whose response will be delivered
     * on a Stream.
     */
    private Http2Connection(HttpConnection connection,
                    Http2ClientImpl client2,
                    Exchange<?> exchange,
                    Supplier<ByteBuffer> initial)
        throws IOException, InterruptedException
    {
        this(connection,
                client2,
                3, // stream 1 is registered during the upgrade
                keyFor(connection));
        reserveStream(true);
        Log.logTrace("Connection send window size {0} ", windowController.connectionWindowSize());

        Stream<?> initialStream = createStream(exchange);
        boolean opened = initialStream.registerStream(1, true);
        if (debug.on() && !opened) {
            debug.log("Initial stream was cancelled - but connection is maintained: " +
                    "reset frame will need to be sent later");
        }
        windowController.registerStream(1, getInitialSendWindowSize());
        initialStream.requestSent();
        // Upgrading:
        //    set callbacks before sending preface - makes sure anything that
        //    might be sent by the server will come our way.
        this.initial = initial;
        connectFlows(connection);
        sendConnectionPreface();
        if (!opened) {
            debug.log("ensure reset frame is sent to cancel initial stream");
            initialStream.sendCancelStreamFrame();
        }

    }

    // Used when upgrading an HTTP/1.1 connection to HTTP/2 after receiving
    // agreement from the server. Async style but completes immediately, because
    // the connection is already connected.
    static CompletableFuture<Http2Connection> createAsync(HttpConnection connection,
                                                          Http2ClientImpl client2,
                                                          Exchange<?> exchange,
                                                          Supplier<ByteBuffer> initial)
    {
        return MinimalFuture.supply(() -> new Http2Connection(connection, client2, exchange, initial));
    }

    // Requires TLS handshake. So, is really async
    static CompletableFuture<Http2Connection> createAsync(HttpRequestImpl request,
                                                          Http2ClientImpl h2client,
                                                          Exchange<?> exchange) {
        assert request.secure();
        AbstractAsyncSSLConnection connection = (AbstractAsyncSSLConnection)
        HttpConnection.getConnection(request.getAddress(),
                                     h2client.client(),
                                     request,
                                     HttpClient.Version.HTTP_2);

        // Expose the underlying connection to the exchange's aborter so it can
        // be closed if a timeout occurs.
        exchange.connectionAborter.connection(connection);

        return connection.connectAsync(exchange)
                  .thenCompose(unused -> connection.finishConnect())
                  .thenCompose(unused -> checkSSLConfig(connection))
                  .thenCompose(notused-> {
                      CompletableFuture<Http2Connection> cf = new MinimalFuture<>();
                      try {
                          Http2Connection hc = new Http2Connection(request, h2client, connection);
                          cf.complete(hc);
                      } catch (IOException e) {
                          cf.completeExceptionally(e);
                      }
                      return cf; } );
    }

    /**
     * Cases 2) 3)
     *
     * request is request to be sent.
     */
    private Http2Connection(HttpRequestImpl request,
                            Http2ClientImpl h2client,
                            HttpConnection connection)
        throws IOException
    {
        this(connection,
             h2client,
             1,
             keyFor(request.uri(), request.proxy()));

        Log.logTrace("Connection send window size {0} ", windowController.connectionWindowSize());

        // safe to resume async reading now.
        connectFlows(connection);
        sendConnectionPreface();
    }

    private void connectFlows(HttpConnection connection) {
        FlowTube tube =  connection.getConnectionFlow();
        // Connect the flow to our Http2TubeSubscriber:
        tube.connectFlows(connection.publisher(), subscriber);
    }

    final HttpClientImpl client() {
        return client2.client();
    }

    // call these before assigning a request/stream to a connection
    // if false returned then a new Http2Connection is required
    // if true, the stream may be assigned to this connection
    // for server push, if false returned, then the stream should be cancelled
    synchronized boolean reserveStream(boolean clientInitiated) throws IOException {
        if (finalStream) {
            return false;
        }
        if (clientInitiated && (lastReservedClientStreamid + 2) >= MAX_CLIENT_STREAM_ID) {
            setFinalStream();
            client2.deleteConnection(this);
            return false;
        } else if (!clientInitiated && (lastReservedServerStreamid + 2) >= MAX_SERVER_STREAM_ID) {
            setFinalStream();
            client2.deleteConnection(this);
            return false;
        }
        if (clientInitiated)
            lastReservedClientStreamid+=2;
        else
            lastReservedServerStreamid+=2;

        assert numReservedClientStreams >= 0;
        assert numReservedServerStreams >= 0;
        if (clientInitiated &&numReservedClientStreams >= maxConcurrentClientInitiatedStreams()) {
            throw new IOException("too many concurrent streams");
        } else if (clientInitiated) {
            numReservedClientStreams++;
        }
        if (!clientInitiated && numReservedServerStreams >= maxConcurrentServerInitiatedStreams()) {
            return false;
        } else if (!clientInitiated) {
            numReservedServerStreams++;
        }
        return true;
    }

    /**
     * Throws an IOException if h2 was not negotiated
     */
    private static CompletableFuture<?> checkSSLConfig(AbstractAsyncSSLConnection aconn) {
        assert aconn.isSecure();

        Function<String, CompletableFuture<Void>> checkAlpnCF = (alpn) -> {
            CompletableFuture<Void> cf = new MinimalFuture<>();
            SSLEngine engine = aconn.getEngine();
            String engineAlpn = engine.getApplicationProtocol();
            assert Objects.equals(alpn, engineAlpn)
                    : "alpn: %s, engine: %s".formatted(alpn, engineAlpn);

            DEBUG_LOGGER.log("checkSSLConfig: alpn: %s", alpn );

            if (alpn == null || !alpn.equals("h2")) {
                String msg;
                if (alpn == null) {
                    Log.logSSL("ALPN not supported");
                    msg = "ALPN not supported";
                } else {
                    switch (alpn) {
                        case "":
                            Log.logSSL(msg = "No ALPN negotiated");
                            break;
                        case "http/1.1":
                            Log.logSSL( msg = "HTTP/1.1 ALPN returned");
                            break;
                        default:
                            Log.logSSL(msg = "Unexpected ALPN: " + alpn);
                            cf.completeExceptionally(new IOException(msg));
                    }
                }
                cf.completeExceptionally(new ALPNException(msg, aconn));
                return cf;
            }
            cf.complete(null);
            return cf;
        };

        return aconn.getALPN()
                .whenComplete((r,t) -> {
                    if (t != null && t instanceof SSLException) {
                        // something went wrong during the initial handshake
                        // close the connection
                        aconn.close();
                    }
                })
                .thenCompose(checkAlpnCF);
    }

    synchronized boolean finalStream() {
        return finalStream;
    }

    /**
     * Mark this connection so no more streams created on it and it will close when
     * all are complete.
     */
    synchronized void setFinalStream() {
        finalStream = true;
    }

    static String keyFor(HttpConnection connection) {
        boolean isProxy = connection.isProxied(); // tunnel or plain clear connection through proxy
        boolean isSecure = connection.isSecure();
        InetSocketAddress addr = connection.address();
        InetSocketAddress proxyAddr = connection.proxy();
        assert isProxy == (proxyAddr != null);

        return keyString(isSecure, proxyAddr, addr.getHostString(), addr.getPort());
    }

    static String keyFor(URI uri, InetSocketAddress proxy) {
        boolean isSecure = uri.getScheme().equalsIgnoreCase("https");

        String host = uri.getHost();
        int port = uri.getPort();
        return keyString(isSecure, proxy, host, port);
    }


    // Compute the key for an HttpConnection in the Http2ClientImpl pool:
    // The key string follows one of the three forms below:
    //    {C,S}:H:host:port
    //    C:P:proxy-host:proxy-port
    //    S:T:H:host:port;P:proxy-host:proxy-port
    // C indicates clear text connection "http"
    // S indicates secure "https"
    // H indicates host (direct) connection
    // P indicates proxy
    // T indicates a tunnel connection through a proxy
    //
    // The first form indicates a direct connection to a server:
    //   - direct clear connection to an HTTP host:
    //     e.g.: "C:H:foo.com:80"
    //   - direct secure connection to an HTTPS host:
    //     e.g.: "S:H:foo.com:443"
    // The second form indicates a clear connection to an HTTP/1.1 proxy:
    //     e.g.: "C:P:myproxy:8080"
    // The third form indicates a secure tunnel connection to an HTTPS
    // host through an HTTP/1.1 proxy:
    //     e.g: "S:T:H:foo.com:80;P:myproxy:8080"
    static String keyString(boolean secure, InetSocketAddress proxy, String host, int port) {
        if (secure && port == -1)
            port = 443;
        else if (!secure && port == -1)
            port = 80;
        var key = (secure ? "S:" : "C:");
        if (proxy != null && !secure) {
            // clear connection through proxy
            key = key + "P:" + proxy.getHostString() + ":" + proxy.getPort();
        } else if (proxy == null) {
            // direct connection to host
            key = key + "H:" + host + ":" + port;
        } else {
            // tunnel connection through proxy
            key = key + "T:H:" + host + ":" + port + ";P:" + proxy.getHostString() + ":" + proxy.getPort();
        }
        return  key;
    }

    String key() {
        return this.key;
    }

    boolean offerConnection() {
        return client2.offerConnection(this);
    }

    private HttpPublisher publisher() {
        return connection.publisher();
    }

    private void decodeHeaders(HeaderFrame frame, DecodingCallback decoder)
            throws IOException
    {
        if (debugHpack.on()) debugHpack.log("decodeHeaders(%s)", decoder);

        boolean endOfHeaders = frame.getFlag(HeaderFrame.END_HEADERS);

        List<ByteBuffer> buffers = frame.getHeaderBlock();
        int len = buffers.size();
        for (int i = 0; i < len; i++) {
            ByteBuffer b = buffers.get(i);
            hpackIn.decode(b, endOfHeaders && (i == len - 1), decoder);
        }
    }

    final int getInitialSendWindowSize() {
        return serverSettings.getParameter(INITIAL_WINDOW_SIZE);
    }

    final int maxConcurrentClientInitiatedStreams() {
        return serverSettings.getParameter(MAX_CONCURRENT_STREAMS);
    }

    final int maxConcurrentServerInitiatedStreams() {
        return clientSettings.getParameter(MAX_CONCURRENT_STREAMS);
    }

    void close() {
        Log.logTrace("Closing HTTP/2 connection: to {0}", connection.address());
        GoAwayFrame f = new GoAwayFrame(0,
                                        ErrorFrame.NO_ERROR,
                                        "Requested by user".getBytes(UTF_8));
        // TODO: set last stream. For now zero ok.
        sendFrame(f);
    }

    long count;
    final void asyncReceive(ByteBuffer buffer) {
        // We don't need to read anything and
        // we don't want to send anything back to the server
        // until the connection preface has been sent.
        // Therefore we're going to wait if needed before reading
        // (and thus replying) to anything.
        // Starting to reply to something (e.g send an ACK to a
        // SettingsFrame sent by the server) before the connection
        // preface is fully sent might result in the server
        // sending a GOAWAY frame with 'invalid_preface'.
        //
        // Note: asyncReceive is only called from the Http2TubeSubscriber
        //       sequential scheduler.
        try {
            Supplier<ByteBuffer> bs = initial;
            // ensure that we always handle the initial buffer first,
            // if any.
            if (bs != null) {
                initial = null;
                ByteBuffer b = bs.get();
                if (b.hasRemaining()) {
                    long c = ++count;
                    if (debug.on())
                        debug.log(() -> "H2 Receiving Initial(" + c +"): " + b.remaining());
                    framesController.processReceivedData(framesDecoder, b);
                }
            }
            ByteBuffer b = buffer;
            // the Http2TubeSubscriber scheduler ensures that the order of incoming
            // buffers is preserved.
            if (b == EMPTY_TRIGGER) {
                if (debug.on()) debug.log("H2 Received EMPTY_TRIGGER");
                boolean prefaceSent = framesController.prefaceSent;
                assert prefaceSent;
                // call framesController.processReceivedData to potentially
                // trigger the processing of all the data buffered there.
                framesController.processReceivedData(framesDecoder, buffer);
                if (debug.on()) debug.log("H2 processed buffered data");
            } else {
                long c = ++count;
                if (debug.on())
                    debug.log("H2 Receiving(%d): %d", c, b.remaining());
                framesController.processReceivedData(framesDecoder, buffer);
                if (debug.on()) debug.log("H2 processed(%d)", c);
            }
        } catch (Throwable e) {
            String msg = Utils.stackTrace(e);
            Log.logTrace(msg);
            shutdown(e);
        }
    }

    Throwable getRecordedCause() {
        return cause;
    }

    void shutdown(Throwable t) {
        if (debug.on()) debug.log(() -> "Shutting down h2c (closed="+closed+"): " + t);
        if (closed == true) return;
        synchronized (this) {
            if (closed == true) return;
            closed = true;
        }
        if (Log.errors()) {
            if (!(t instanceof EOFException) || isActive()) {
                Log.logError(t);
            } else if (t != null) {
                Log.logError("Shutting down connection: {0}", t.getMessage());
            }
        }
        Throwable initialCause = this.cause;
        if (initialCause == null) this.cause = t;
        client2.deleteConnection(this);
        for (Stream<?> s : streams.values()) {
            try {
                s.connectionClosing(t);
            } catch (Throwable e) {
                Log.logError("Failed to close stream {0}: {1}", s.streamid, e);
            }
        }
        connection.close();
    }

    /**
     * Streams initiated by a client MUST use odd-numbered stream
     * identifiers; those initiated by the server MUST use even-numbered
     * stream identifiers.
     */
    private static final boolean isServerInitiatedStream(int streamid) {
        return (streamid & 0x1) == 0;
    }

    /**
     * Handles stream 0 (common) frames that apply to whole connection and passes
     * other stream specific frames to that Stream object.
     *
     * Invokes Stream.incoming() which is expected to process frame without
     * blocking.
     */
    void processFrame(Http2Frame frame) throws IOException {
        Log.logFrames(frame, "IN");
        int streamid = frame.streamid();
        if (frame instanceof MalformedFrame) {
            Log.logError(((MalformedFrame) frame).getMessage());
            if (streamid == 0) {
                framesDecoder.close("Malformed frame on stream 0");
                protocolError(((MalformedFrame) frame).getErrorCode(),
                        ((MalformedFrame) frame).getMessage());
            } else {
                if (debug.on())
                    debug.log(() -> "Reset stream: " + ((MalformedFrame) frame).getMessage());
                resetStream(streamid, ((MalformedFrame) frame).getErrorCode());
            }
            return;
        }
        if (streamid == 0) {
            handleConnectionFrame(frame);
        } else {
            if (frame instanceof SettingsFrame) {
                // The stream identifier for a SETTINGS frame MUST be zero
                framesDecoder.close(
                        "The stream identifier for a SETTINGS frame MUST be zero");
                protocolError(GoAwayFrame.PROTOCOL_ERROR);
                return;
            }

            Stream<?> stream = getStream(streamid);
            if (stream == null) {
                // Should never receive a frame with unknown stream id

                if (frame instanceof HeaderFrame) {
                    // always decode the headers as they may affect
                    // connection-level HPACK decoding state
                    DecodingCallback decoder = new ValidatingHeadersConsumer();
                    try {
                        decodeHeaders((HeaderFrame) frame, decoder);
                    } catch (UncheckedIOException e) {
                        protocolError(ResetFrame.PROTOCOL_ERROR, e.getMessage());
                        return;
                    }
                }

                if (!(frame instanceof ResetFrame)) {
                    if (frame instanceof DataFrame) {
                        dropDataFrame((DataFrame)frame);
                    }
                    if (isServerInitiatedStream(streamid)) {
                        if (streamid < nextPushStream) {
                            // trailing data on a cancelled push promise stream,
                            // reset will already have been sent, ignore
                            Log.logTrace("Ignoring cancelled push promise frame " + frame);
                        } else {
                            resetStream(streamid, ResetFrame.PROTOCOL_ERROR);
                        }
                    } else if (streamid >= nextstreamid) {
                        // otherwise the stream has already been reset/closed
                        resetStream(streamid, ResetFrame.PROTOCOL_ERROR);
                    }
                }
                return;
            }
            if (frame instanceof PushPromiseFrame) {
                PushPromiseFrame pp = (PushPromiseFrame)frame;
                try {
                    handlePushPromise(stream, pp);
                } catch (UncheckedIOException e) {
                    protocolError(ResetFrame.PROTOCOL_ERROR, e.getMessage());
                    return;
                }
            } else if (frame instanceof HeaderFrame) {
                // decode headers (or continuation)
                try {
                    decodeHeaders((HeaderFrame) frame, stream.rspHeadersConsumer());
                } catch (UncheckedIOException e) {
                    debug.log("Error decoding headers: " + e.getMessage(), e);
                    protocolError(ResetFrame.PROTOCOL_ERROR, e.getMessage());
                    return;
                }
                stream.incoming(frame);
            } else {
                stream.incoming(frame);
            }
        }
    }

    final void dropDataFrame(DataFrame df) {
        if (closed) return;
        if (debug.on()) {
            debug.log("Dropping data frame for stream %d (%d payload bytes)",
                    df.streamid(), df.payloadLength());
        }
        ensureWindowUpdated(df);
    }

    final void ensureWindowUpdated(DataFrame df) {
        try {
            if (closed) return;
            int length = df.payloadLength();
            if (length > 0) {
                windowUpdater.update(length);
            }
        } catch(Throwable t) {
            Log.logError("Unexpected exception while updating window: {0}", (Object)t);
        }
    }

    private <T> void handlePushPromise(Stream<T> parent, PushPromiseFrame pp)
        throws IOException
    {
        // always decode the headers as they may affect connection-level HPACK
        // decoding state
        HeaderDecoder decoder = new HeaderDecoder();
        decodeHeaders(pp, decoder);

        HttpRequestImpl parentReq = parent.request;
        int promisedStreamid = pp.getPromisedStream();
        if (promisedStreamid != nextPushStream) {
            resetStream(promisedStreamid, ResetFrame.PROTOCOL_ERROR);
            return;
        } else if (!reserveStream(false)) {
            resetStream(promisedStreamid, ResetFrame.REFUSED_STREAM);
            return;
        } else {
            nextPushStream += 2;
        }

        HttpHeaders headers = decoder.headers();
        HttpRequestImpl pushReq = HttpRequestImpl.createPushRequest(parentReq, headers);
        Exchange<T> pushExch = new Exchange<>(pushReq, parent.exchange.multi);
        Stream.PushedStream<T> pushStream = createPushStream(parent, pushExch);
        pushExch.exchImpl = pushStream;
        pushStream.registerStream(promisedStreamid, true);
        parent.incoming_pushPromise(pushReq, pushStream);
    }

    private void handleConnectionFrame(Http2Frame frame)
        throws IOException
    {
        switch (frame.type()) {
            case SettingsFrame.TYPE     -> handleSettings((SettingsFrame) frame);
            case PingFrame.TYPE         -> handlePing((PingFrame) frame);
            case GoAwayFrame.TYPE       -> handleGoAway((GoAwayFrame) frame);
            case WindowUpdateFrame.TYPE -> handleWindowUpdate((WindowUpdateFrame) frame);

            default -> protocolError(ErrorFrame.PROTOCOL_ERROR);
        }
    }

    void resetStream(int streamid, int code) {
        try {
            if (connection.channel().isOpen()) {
                // no need to try & send a reset frame if the
                // connection channel is already closed.
                Log.logError(
                        "Resetting stream {0,number,integer} with error code {1,number,integer}",
                        streamid, code);
                markStream(streamid, code);
                ResetFrame frame = new ResetFrame(streamid, code);
                sendFrame(frame);
            } else if (debug.on()) {
                debug.log("Channel already closed, no need to reset stream %d",
                          streamid);
            }
        } finally {
            decrementStreamsCount(streamid);
            closeStream(streamid);
        }
    }

    private void markStream(int streamid, int code) {
        Stream<?> s = streams.get(streamid);
        if (s != null) s.markStream(code);
    }

    // reduce count of streams by 1 if stream still exists
    synchronized void decrementStreamsCount(int streamid) {
        Stream<?> s = streams.get(streamid);
        if (s == null || !s.deRegister())
            return;
        if (streamid % 2 == 1) {
            numReservedClientStreams--;
            assert numReservedClientStreams >= 0 :
                    "negative client stream count for stream=" + streamid;
        } else {
            numReservedServerStreams--;
            assert numReservedServerStreams >= 0 :
                    "negative server stream count for stream=" + streamid;
        }
    }

    void closeStream(int streamid) {
        if (debug.on()) debug.log("Closed stream %d", streamid);
        boolean isClient = (streamid % 2) == 1;
        Stream<?> s = streams.remove(streamid);
        if (s != null) {
            // decrement the reference count on the HttpClientImpl
            // to allow the SelectorManager thread to exit if no
            // other operation is pending and the facade is no
            // longer referenced.
            client().streamUnreference();
        }
        // ## Remove s != null. It is a hack for delayed cancellation,reset
        if (s != null && !(s instanceof Stream.PushedStream)) {
            // Since PushStreams have no request body, then they have no
            // corresponding entry in the window controller.
            windowController.removeStream(streamid);
        }
        if (finalStream() && streams.isEmpty()) {
            // should be only 1 stream, but there might be more if server push
            close();
        }
    }

    /**
     * Increments this connection's send Window by the amount in the given frame.
     */
    private void handleWindowUpdate(WindowUpdateFrame f)
        throws IOException
    {
        int amount = f.getUpdate();
        if (amount <= 0) {
            // ## temporarily disable to workaround a bug in Jetty where it
            // ## sends Window updates with a 0 update value.
            //protocolError(ErrorFrame.PROTOCOL_ERROR);
        } else {
            boolean success = windowController.increaseConnectionWindow(amount);
            if (!success) {
                protocolError(ErrorFrame.FLOW_CONTROL_ERROR);  // overflow
            }
        }
    }

    private void protocolError(int errorCode)
        throws IOException
    {
        protocolError(errorCode, null);
    }

    private void protocolError(int errorCode, String msg)
        throws IOException
    {
        GoAwayFrame frame = new GoAwayFrame(0, errorCode);
        sendFrame(frame);
        shutdown(new IOException("protocol error" + (msg == null?"":(": " + msg))));
    }

    private void handleSettings(SettingsFrame frame)
        throws IOException
    {
        assert frame.streamid() == 0;
        if (!frame.getFlag(SettingsFrame.ACK)) {
            int newWindowSize = frame.getParameter(INITIAL_WINDOW_SIZE);
            if (newWindowSize != -1) {
                int oldWindowSize = serverSettings.getParameter(INITIAL_WINDOW_SIZE);
                int diff = newWindowSize - oldWindowSize;
                if (diff != 0) {
                    windowController.adjustActiveStreams(diff);
                }
            }

            serverSettings.update(frame);
            sendFrame(new SettingsFrame(SettingsFrame.ACK));
        }
    }

    private void handlePing(PingFrame frame)
        throws IOException
    {
        frame.setFlag(PingFrame.ACK);
        sendUnorderedFrame(frame);
    }

    private void handleGoAway(GoAwayFrame frame)
        throws IOException
    {
        shutdown(new IOException(
                        String.valueOf(connection.channel().getLocalAddress())
                        +": GOAWAY received"));
    }

    /**
     * Max frame size we are allowed to send
     */
    public int getMaxSendFrameSize() {
        int param = serverSettings.getParameter(MAX_FRAME_SIZE);
        if (param == -1) {
            param = DEFAULT_FRAME_SIZE;
        }
        return param;
    }

    /**
     * Max frame size we will receive
     */
    public int getMaxReceiveFrameSize() {
        return clientSettings.getParameter(MAX_FRAME_SIZE);
    }

    private static final String CLIENT_PREFACE = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n";

    private static final byte[] PREFACE_BYTES =
        CLIENT_PREFACE.getBytes(StandardCharsets.ISO_8859_1);

    /**
     * Sends Connection preface and Settings frame with current preferred
     * values
     */
    private void sendConnectionPreface() throws IOException {
        Log.logTrace("{0}: start sending connection preface to {1}",
                     connection.channel().getLocalAddress(),
                     connection.address());
        SettingsFrame sf = new SettingsFrame(clientSettings);
        ByteBuffer buf = framesEncoder.encodeConnectionPreface(PREFACE_BYTES, sf);
        Log.logFrames(sf, "OUT");
        // send preface bytes and SettingsFrame together
        HttpPublisher publisher = publisher();
        publisher.enqueueUnordered(List.of(buf));
        publisher.signalEnqueued();
        // mark preface sent.
        framesController.markPrefaceSent();
        Log.logTrace("PREFACE_BYTES sent");
        Log.logTrace("Settings Frame sent");

        // send a Window update for the receive buffer we are using
        // minus the initial 64 K -1 specified in protocol:
        // RFC 7540, Section 6.9.2:
        // "[...] the connection flow-control window is set to the default
        // initial window size until a WINDOW_UPDATE frame is received."
        //
        // Note that the default initial window size, not to be confused
        // with the initial window size, is defined by RFC 7540 as
        // 64K -1.
        final int len = windowUpdater.initialWindowSize - DEFAULT_INITIAL_WINDOW_SIZE;
        if (len != 0) {
            if (Log.channel()) {
                Log.logChannel("Sending initial connection window update frame: {0} ({1} - {2})",
                        len, windowUpdater.initialWindowSize, DEFAULT_INITIAL_WINDOW_SIZE);
            }
            windowUpdater.sendWindowUpdate(len);
        }
        // there will be an ACK to the windows update - which should
        // cause any pending data stored before the preface was sent to be
        // flushed (see PrefaceController).
        Log.logTrace("finished sending connection preface");
        if (debug.on())
            debug.log("Triggering processing of buffered data"
                      + " after sending connection preface");
        subscriber.onNext(List.of(EMPTY_TRIGGER));
    }

    /**
     * Returns an existing Stream with given id, or null if doesn't exist
     */
    @SuppressWarnings("unchecked")
    <T> Stream<T> getStream(int streamid) {
        return (Stream<T>)streams.get(streamid);
    }

    /**
     * Creates Stream with given id.
     */
    final <T> Stream<T> createStream(Exchange<T> exchange) {
        Stream<T> stream = new Stream<>(this, exchange, windowController);
        return stream;
    }

    <T> Stream.PushedStream<T> createPushStream(Stream<T> parent, Exchange<T> pushEx) {
        PushGroup<T> pg = parent.exchange.getPushGroup();
        return new Stream.PushedStream<>(pg, this, pushEx);
    }

    <T> void putStream(Stream<T> stream, int streamid) {
        // increment the reference count on the HttpClientImpl
        // to prevent the SelectorManager thread from exiting until
        // the stream is closed.
        client().streamReference();
        streams.put(streamid, stream);
    }

    /**
     * Encode the headers into a List<ByteBuffer> and then create HEADERS
     * and CONTINUATION frames from the list and return the List<Http2Frame>.
     */
    private List<HeaderFrame> encodeHeaders(OutgoingHeaders<Stream<?>> frame) {
        // max value of frame size is clamped by default frame size to avoid OOM
        int bufferSize = Math.min(Math.max(getMaxSendFrameSize(), 1024), DEFAULT_FRAME_SIZE);
        List<ByteBuffer> buffers = encodeHeadersImpl(
                bufferSize,
                frame.getAttachment().getRequestPseudoHeaders(),
                frame.getUserHeaders(),
                frame.getSystemHeaders());

        List<HeaderFrame> frames = new ArrayList<>(buffers.size());
        Iterator<ByteBuffer> bufIterator = buffers.iterator();
        HeaderFrame oframe = new HeadersFrame(frame.streamid(), frame.getFlags(), bufIterator.next());
        frames.add(oframe);
        while(bufIterator.hasNext()) {
            oframe = new ContinuationFrame(frame.streamid(), bufIterator.next());
            frames.add(oframe);
        }
        oframe.setFlag(HeaderFrame.END_HEADERS);
        return frames;
    }

    // Dedicated cache for headers encoding ByteBuffer.
    // There can be no concurrent access to this  buffer as all access to this buffer
    // and its content happen within a single critical code block section protected
    // by the sendLock. / (see sendFrame())
    // private final ByteBufferPool headerEncodingPool = new ByteBufferPool();

    private ByteBuffer getHeaderBuffer(int size) {
        ByteBuffer buf = ByteBuffer.allocate(size);
        buf.limit(size);
        return buf;
    }

    /*
     * Encodes all the headers from the given HttpHeaders into the given List
     * of buffers.
     *
     * From https://tools.ietf.org/html/rfc7540#section-8.1.2 :
     *
     *     ...Just as in HTTP/1.x, header field names are strings of ASCII
     *     characters that are compared in a case-insensitive fashion.  However,
     *     header field names MUST be converted to lowercase prior to their
     *     encoding in HTTP/2...
     */
    private List<ByteBuffer> encodeHeadersImpl(int bufferSize, HttpHeaders... headers) {
        ByteBuffer buffer = getHeaderBuffer(bufferSize);
        List<ByteBuffer> buffers = new ArrayList<>();
        for(HttpHeaders header : headers) {
            for (Map.Entry<String, List<String>> e : header.map().entrySet()) {
                String lKey = e.getKey().toLowerCase(Locale.US);
                List<String> values = e.getValue();
                for (String value : values) {
                    hpackOut.header(lKey, value);
                    while (!hpackOut.encode(buffer)) {
                        buffer.flip();
                        buffers.add(buffer);
                        buffer =  getHeaderBuffer(bufferSize);
                    }
                }
            }
        }
        buffer.flip();
        buffers.add(buffer);
        return buffers;
    }


    private List<ByteBuffer> encodeHeaders(OutgoingHeaders<Stream<?>> oh, Stream<?> stream) {
        oh.streamid(stream.streamid);
        if (Log.headers()) {
            StringBuilder sb = new StringBuilder("HEADERS FRAME (stream=");
            sb.append(stream.streamid).append(")\n");
            Log.dumpHeaders(sb, "    ", oh.getAttachment().getRequestPseudoHeaders());
            Log.dumpHeaders(sb, "    ", oh.getSystemHeaders());
            Log.dumpHeaders(sb, "    ", oh.getUserHeaders());
            Log.logHeaders(sb.toString());
        }
        List<HeaderFrame> frames = encodeHeaders(oh);
        return encodeFrames(frames);
    }

    private List<ByteBuffer> encodeFrames(List<HeaderFrame> frames) {
        if (Log.frames()) {
            frames.forEach(f -> Log.logFrames(f, "OUT"));
        }
        return framesEncoder.encodeFrames(frames);
    }

    private Stream<?> registerNewStream(OutgoingHeaders<Stream<?>> oh) {
        Stream<?> stream = oh.getAttachment();
        assert stream.streamid == 0;
        int streamid = nextstreamid;
        if (stream.registerStream(streamid, false)) {
            // set outgoing window here. This allows thread sending
            // body to proceed.
            nextstreamid += 2;
            windowController.registerStream(streamid, getInitialSendWindowSize());
            return stream;
        } else {
            stream.cancelImpl(new IOException("Request cancelled"));
            if (finalStream() && streams.isEmpty()) {
                close();
            }
            return null;
        }
    }

    private final Object sendlock = new Object();

    void sendFrame(Http2Frame frame) {
        try {
            HttpPublisher publisher = publisher();
            synchronized (sendlock) {
                if (frame instanceof OutgoingHeaders) {
                    @SuppressWarnings("unchecked")
                    OutgoingHeaders<Stream<?>> oh = (OutgoingHeaders<Stream<?>>) frame;
                    Stream<?> stream = registerNewStream(oh);
                    // provide protection from inserting unordered frames between Headers and Continuation
                    if (stream != null) {
                        publisher.enqueue(encodeHeaders(oh, stream));
                    }
                } else {
                    publisher.enqueue(encodeFrame(frame));
                }
            }
            publisher.signalEnqueued();
        } catch (IOException e) {
            if (!closed) {
                Log.logError(e);
                shutdown(e);
            }
        }
    }

    private List<ByteBuffer> encodeFrame(Http2Frame frame) {
        Log.logFrames(frame, "OUT");
        return framesEncoder.encodeFrame(frame);
    }

    void sendDataFrame(DataFrame frame) {
        try {
            HttpPublisher publisher = publisher();
            publisher.enqueue(encodeFrame(frame));
            publisher.signalEnqueued();
        } catch (IOException e) {
            if (!closed) {
                Log.logError(e);
                shutdown(e);
            }
        }
    }

    /*
     * Direct call of the method bypasses synchronization on "sendlock" and
     * allowed only of control frames: WindowUpdateFrame, PingFrame and etc.
     * prohibited for such frames as DataFrame, HeadersFrame, ContinuationFrame.
     */
    void sendUnorderedFrame(Http2Frame frame) {
        try {
            HttpPublisher publisher = publisher();
            publisher.enqueueUnordered(encodeFrame(frame));
            publisher.signalEnqueued();
        } catch (IOException e) {
            if (!closed) {
                Log.logError(e);
                shutdown(e);
            }
        }
    }

    /**
     * A simple tube subscriber for reading from the connection flow.
     */
    final class Http2TubeSubscriber implements TubeSubscriber {
        private volatile Flow.Subscription subscription;
        private volatile boolean completed;
        private volatile boolean dropped;
        private volatile Throwable error;
        private final ConcurrentLinkedQueue<ByteBuffer> queue
                = new ConcurrentLinkedQueue<>();
        private final SequentialScheduler scheduler =
                SequentialScheduler.lockingScheduler(this::processQueue);
        private final HttpClientImpl client;

        Http2TubeSubscriber(HttpClientImpl client) {
            this.client = Objects.requireNonNull(client);
        }

        final void processQueue() {
            try {
                while (!queue.isEmpty() && !scheduler.isStopped()) {
                    ByteBuffer buffer = queue.poll();
                    if (debug.on())
                        debug.log("sending %d to Http2Connection.asyncReceive",
                                  buffer.remaining());
                    asyncReceive(buffer);
                }
            } catch (Throwable t) {
                Throwable x = error;
                if (x == null) error = t;
            } finally {
                Throwable x = error;
                if (x != null) {
                    if (debug.on()) debug.log("Stopping scheduler", x);
                    scheduler.stop();
                    Http2Connection.this.shutdown(x);
                }
            }
        }

        private final void runOrSchedule() {
            if (client.isSelectorThread()) {
                scheduler.runOrSchedule(client.theExecutor());
            } else scheduler.runOrSchedule();
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            // supports being called multiple time.
            // doesn't cancel the previous subscription, since that is
            // most probably the same as the new subscription.
            assert this.subscription == null || dropped == false;
            this.subscription = subscription;
            dropped = false;
            // TODO FIXME: request(1) should be done by the delegate.
            if (!completed) {
                if (debug.on())
                    debug.log("onSubscribe: requesting Long.MAX_VALUE for reading");
                subscription.request(Long.MAX_VALUE);
            } else {
                if (debug.on()) debug.log("onSubscribe: already completed");
            }
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            if (debug.on()) debug.log(() -> "onNext: got " + Utils.remaining(item)
                    + " bytes in " + item.size() + " buffers");
            queue.addAll(item);
            runOrSchedule();
        }

        @Override
        public void onError(Throwable throwable) {
            if (debug.on()) debug.log(() -> "onError: " + throwable);
            error = throwable;
            completed = true;
            runOrSchedule();
        }

        @Override
        public void onComplete() {
            String msg = isActive()
                    ? "EOF reached while reading"
                    : "Idle connection closed by HTTP/2 peer";
            if (debug.on()) debug.log(msg);
            error = new EOFException(msg);
            completed = true;
            runOrSchedule();
        }

        @Override
        public void dropSubscription() {
            if (debug.on()) debug.log("dropSubscription");
            // we could probably set subscription to null here...
            // then we might not need the 'dropped' boolean?
            dropped = true;
        }
    }

    synchronized boolean isActive() {
        return numReservedClientStreams > 0 || numReservedServerStreams > 0;
    }

    @Override
    public final String toString() {
        return dbgString();
    }

    final String dbgString() {
        return "Http2Connection("
                    + connection.getConnectionFlow() + ")";
    }

    static class HeaderDecoder extends ValidatingHeadersConsumer {

        HttpHeadersBuilder headersBuilder;

        HeaderDecoder() {
            this.headersBuilder = new HttpHeadersBuilder();
        }

        @Override
        public void onDecoded(CharSequence name, CharSequence value) {
            String n = name.toString();
            String v = value.toString();
            super.onDecoded(n, v);
            headersBuilder.addHeader(n, v);
        }

        HttpHeaders headers() {
            return headersBuilder.build();
        }
    }

    /*
     * Checks RFC 7540 rules (relaxed) compliance regarding pseudo-headers.
     */
    static class ValidatingHeadersConsumer implements DecodingCallback {

        private static final Set<String> PSEUDO_HEADERS =
                Set.of(":authority", ":method", ":path", ":scheme", ":status");

        /** Used to check that if there are pseudo-headers, they go first */
        private boolean pseudoHeadersEnded;

        /**
         * Called when END_HEADERS was received. This consumer may be invoked
         * again after reset() is called, but for a whole new set of headers.
         */
        void reset() {
            pseudoHeadersEnded = false;
        }

        @Override
        public void onDecoded(CharSequence name, CharSequence value)
                throws UncheckedIOException
        {
            String n = name.toString();
            if (n.startsWith(":")) {
                if (pseudoHeadersEnded) {
                    throw newException("Unexpected pseudo-header '%s'", n);
                } else if (!PSEUDO_HEADERS.contains(n)) {
                    throw newException("Unknown pseudo-header '%s'", n);
                }
            } else {
                pseudoHeadersEnded = true;
                if (!Utils.isValidName(n)) {
                    throw newException("Bad header name '%s'", n);
                }
            }
            String v = value.toString();
            if (!Utils.isValidValue(v)) {
                throw newException("Bad header value '%s'", v);
            }
        }

        private UncheckedIOException newException(String message, String header)
        {
            return new UncheckedIOException(
                    new IOException(String.format(message, header)));
        }
    }

    static final class ConnectionWindowUpdateSender extends WindowUpdateSender {

        final int initialWindowSize;
        public ConnectionWindowUpdateSender(Http2Connection connection,
                                            int initialWindowSize) {
            super(connection, initialWindowSize);
            this.initialWindowSize = initialWindowSize;
        }

        @Override
        int getStreamId() {
            return 0;
        }
    }

    /**
     * Thrown when https handshake negotiates http/1.1 alpn instead of h2
     */
    static final class ALPNException extends IOException {
        private static final long serialVersionUID = 0L;
        final transient AbstractAsyncSSLConnection connection;

        ALPNException(String msg, AbstractAsyncSSLConnection connection) {
            super(msg);
            this.connection = connection;
        }

        AbstractAsyncSSLConnection getConnection() {
            return connection;
        }
    }
}
