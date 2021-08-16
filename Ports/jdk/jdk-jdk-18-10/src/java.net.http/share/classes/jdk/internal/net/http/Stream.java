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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscriber;
import jdk.internal.net.http.common.*;
import jdk.internal.net.http.frame.*;
import jdk.internal.net.http.hpack.DecodingCallback;

/**
 * Http/2 Stream handling.
 *
 * REQUESTS
 *
 * sendHeadersOnly() -- assembles HEADERS frame and puts on connection outbound Q
 *
 * sendRequest() -- sendHeadersOnly() + sendBody()
 *
 * sendBodyAsync() -- calls sendBody() in an executor thread.
 *
 * sendHeadersAsync() -- calls sendHeadersOnly() which does not block
 *
 * sendRequestAsync() -- calls sendRequest() in an executor thread
 *
 * RESPONSES
 *
 * Multiple responses can be received per request. Responses are queued up on
 * a LinkedList of CF<HttpResponse> and the first one on the list is completed
 * with the next response
 *
 * getResponseAsync() -- queries list of response CFs and returns first one
 *               if one exists. Otherwise, creates one and adds it to list
 *               and returns it. Completion is achieved through the
 *               incoming() upcall from connection reader thread.
 *
 * getResponse() -- calls getResponseAsync() and waits for CF to complete
 *
 * responseBodyAsync() -- calls responseBody() in an executor thread.
 *
 * incoming() -- entry point called from connection reader thread. Frames are
 *               either handled immediately without blocking or for data frames
 *               placed on the stream's inputQ which is consumed by the stream's
 *               reader thread.
 *
 * PushedStream sub class
 * ======================
 * Sending side methods are not used because the request comes from a PUSH_PROMISE
 * frame sent by the server. When a PUSH_PROMISE is received the PushedStream
 * is created. PushedStream does not use responseCF list as there can be only
 * one response. The CF is created when the object created and when the response
 * HEADERS frame is received the object is completed.
 */
class Stream<T> extends ExchangeImpl<T> {

    final Logger debug = Utils.getDebugLogger(this::dbgString, Utils.DEBUG);

    final ConcurrentLinkedQueue<Http2Frame> inputQ = new ConcurrentLinkedQueue<>();
    final SequentialScheduler sched =
            SequentialScheduler.lockingScheduler(this::schedule);
    final SubscriptionBase userSubscription =
            new SubscriptionBase(sched, this::cancel, this::onSubscriptionError);

    /**
     * This stream's identifier. Assigned lazily by the HTTP2Connection before
     * the stream's first frame is sent.
     */
    protected volatile int streamid;

    long requestContentLen;

    final Http2Connection connection;
    final HttpRequestImpl request;
    final HeadersConsumer rspHeadersConsumer;
    final HttpHeadersBuilder responseHeadersBuilder;
    final HttpHeaders requestPseudoHeaders;
    volatile HttpResponse.BodySubscriber<T> responseSubscriber;
    final HttpRequest.BodyPublisher requestPublisher;
    volatile RequestSubscriber requestSubscriber;
    volatile int responseCode;
    volatile Response response;
    // The exception with which this stream was canceled.
    private final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    final CompletableFuture<Void> requestBodyCF = new MinimalFuture<>();
    volatile CompletableFuture<T> responseBodyCF;
    volatile HttpResponse.BodySubscriber<T> pendingResponseSubscriber;
    volatile boolean stopRequested;

    /** True if END_STREAM has been seen in a frame received on this stream. */
    private volatile boolean remotelyClosed;
    private volatile boolean closed;
    private volatile boolean endStreamSent;
    // Indicates the first reason that was invoked when sending a ResetFrame
    // to the server. A streamState of 0 indicates that no reset was sent.
    // (see markStream(int code)
    private volatile int streamState; // assigned using STREAM_STATE varhandle.
    private volatile boolean deRegistered; // assigned using DEREGISTERED varhandle.

    // state flags
    private boolean requestSent, responseReceived;

    // send lock: prevent sending DataFrames after reset occurred.
    private final Object sendLock = new Object();

    /**
     * A reference to this Stream's connection Send Window controller. The
     * stream MUST acquire the appropriate amount of Send Window before
     * sending any data. Will be null for PushStreams, as they cannot send data.
     */
    private final WindowController windowController;
    private final WindowUpdateSender windowUpdater;

    @Override
    HttpConnection connection() {
        return connection.connection;
    }

    /**
     * Invoked either from incoming() -> {receiveDataFrame() or receiveResetFrame() }
     * of after user subscription window has re-opened, from SubscriptionBase.request()
     */
    private void schedule() {
        boolean onCompleteCalled = false;
        HttpResponse.BodySubscriber<T> subscriber = responseSubscriber;
        try {
            if (subscriber == null) {
                subscriber = responseSubscriber = pendingResponseSubscriber;
                if (subscriber == null) {
                    // can't process anything yet
                    return;
                } else {
                    if (debug.on()) debug.log("subscribing user subscriber");
                    subscriber.onSubscribe(userSubscription);
                }
            }
            while (!inputQ.isEmpty()) {
                Http2Frame frame = inputQ.peek();
                if (frame instanceof ResetFrame) {
                    inputQ.remove();
                    handleReset((ResetFrame)frame, subscriber);
                    return;
                }
                DataFrame df = (DataFrame)frame;
                boolean finished = df.getFlag(DataFrame.END_STREAM);

                List<ByteBuffer> buffers = df.getData();
                List<ByteBuffer> dsts = Collections.unmodifiableList(buffers);
                int size = Utils.remaining(dsts, Integer.MAX_VALUE);
                if (size == 0 && finished) {
                    inputQ.remove();
                    connection.ensureWindowUpdated(df); // must update connection window
                    Log.logTrace("responseSubscriber.onComplete");
                    if (debug.on()) debug.log("incoming: onComplete");
                    sched.stop();
                    connection.decrementStreamsCount(streamid);
                    subscriber.onComplete();
                    onCompleteCalled = true;
                    setEndStreamReceived();
                    return;
                } else if (userSubscription.tryDecrement()) {
                    inputQ.remove();
                    Log.logTrace("responseSubscriber.onNext {0}", size);
                    if (debug.on()) debug.log("incoming: onNext(%d)", size);
                    try {
                        subscriber.onNext(dsts);
                    } catch (Throwable t) {
                        connection.dropDataFrame(df); // must update connection window
                        throw t;
                    }
                    if (consumed(df)) {
                        Log.logTrace("responseSubscriber.onComplete");
                        if (debug.on()) debug.log("incoming: onComplete");
                        sched.stop();
                        connection.decrementStreamsCount(streamid);
                        subscriber.onComplete();
                        onCompleteCalled = true;
                        setEndStreamReceived();
                        return;
                    }
                } else {
                    if (stopRequested) break;
                    return;
                }
            }
        } catch (Throwable throwable) {
            errorRef.compareAndSet(null, throwable);
        } finally {
            if (sched.isStopped()) drainInputQueue();
        }

        Throwable t = errorRef.get();
        if (t != null) {
            sched.stop();
            try {
                if (!onCompleteCalled) {
                    if (debug.on())
                        debug.log("calling subscriber.onError: %s", (Object) t);
                    subscriber.onError(t);
                } else {
                    if (debug.on())
                        debug.log("already completed: dropping error %s", (Object) t);
                }
            } catch (Throwable x) {
                Log.logError("Subscriber::onError threw exception: {0}", (Object) t);
            } finally {
                cancelImpl(t);
                drainInputQueue();
            }
        }
    }

    // must only be called from the scheduler schedule() loop.
    // ensure that all received data frames are accounted for
    // in the connection window flow control if the scheduler
    // is stopped before all the data is consumed.
    private void drainInputQueue() {
        Http2Frame frame;
        while ((frame = inputQ.poll()) != null) {
            if (frame instanceof DataFrame) {
                connection.dropDataFrame((DataFrame)frame);
            }
        }
    }

    @Override
    void nullBody(HttpResponse<T> resp, Throwable t) {
        if (debug.on()) debug.log("nullBody: streamid=%d", streamid);
        // We should have an END_STREAM data frame waiting in the inputQ.
        // We need a subscriber to force the scheduler to process it.
        pendingResponseSubscriber = HttpResponse.BodySubscribers.replacing(null);
        sched.runOrSchedule();
    }

    // Callback invoked after the Response BodySubscriber has consumed the
    // buffers contained in a DataFrame.
    // Returns true if END_STREAM is reached, false otherwise.
    private boolean consumed(DataFrame df) {
        // RFC 7540 6.1:
        // The entire DATA frame payload is included in flow control,
        // including the Pad Length and Padding fields if present
        int len = df.payloadLength();
        boolean endStream = df.getFlag(DataFrame.END_STREAM);
        if (len == 0) return endStream;

        connection.windowUpdater.update(len);

        if (!endStream) {
            // Don't send window update on a stream which is
            // closed or half closed.
            windowUpdater.update(len);
        }

        // true: end of stream; false: more data coming
        return endStream;
    }

    boolean deRegister() {
        return DEREGISTERED.compareAndSet(this, false, true);
    }

    @Override
    CompletableFuture<T> readBodyAsync(HttpResponse.BodyHandler<T> handler,
                                       boolean returnConnectionToPool,
                                       Executor executor)
    {
        try {
            Log.logTrace("Reading body on stream {0}", streamid);
            debug.log("Getting BodySubscriber for: " + response);
            BodySubscriber<T> bodySubscriber = handler.apply(new ResponseInfoImpl(response));
            CompletableFuture<T> cf = receiveData(bodySubscriber, executor);

            PushGroup<?> pg = exchange.getPushGroup();
            if (pg != null) {
                // if an error occurs make sure it is recorded in the PushGroup
                cf = cf.whenComplete((t, e) -> pg.pushError(e));
            }
            return cf;
        } catch (Throwable t) {
            // may be thrown by handler.apply
            cancelImpl(t);
            return MinimalFuture.failedFuture(t);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("streamid: ")
                .append(streamid);
        return sb.toString();
    }

    private void receiveDataFrame(DataFrame df) {
        inputQ.add(df);
        sched.runOrSchedule();
    }

    /** Handles a RESET frame. RESET is always handled inline in the queue. */
    private void receiveResetFrame(ResetFrame frame) {
        inputQ.add(frame);
        sched.runOrSchedule();
    }

    /**
     * Records the first reason which was invoked when sending a ResetFrame
     * to the server in the streamState, and return the previous value
     * of the streamState. This is an atomic operation.
     * A possible use of this method would be to send a ResetFrame only
     * if no previous reset frame has been sent.
     * For instance: <pre>{@code
     *  if (markStream(ResetFrame.CANCEL) == 0) {
     *      connection.sendResetFrame(streamId, ResetFrame.CANCEL);
     *  }
     *  }</pre>
     * @param code the reason code as per HTTP/2 protocol
     * @return the previous value of the stream state.
     */
    int  markStream(int code) {
        if (code == 0) return streamState;
        synchronized (sendLock) {
            return (int) STREAM_STATE.compareAndExchange(this, 0, code);
        }
    }

    private void sendDataFrame(DataFrame frame) {
         synchronized (sendLock) {
             // must not send DataFrame after reset.
             if (streamState == 0) {
                connection.sendDataFrame(frame);
             }
        }
    }

    // pushes entire response body into response subscriber
    // blocking when required by local or remote flow control
    CompletableFuture<T> receiveData(BodySubscriber<T> bodySubscriber, Executor executor) {
        // We want to allow the subscriber's getBody() method to block so it
        // can work with InputStreams. So, we offload execution.
        responseBodyCF = ResponseSubscribers.getBodyAsync(executor, bodySubscriber,
                new MinimalFuture<>(), this::cancelImpl);

        if (isCanceled()) {
            Throwable t = getCancelCause();
            responseBodyCF.completeExceptionally(t);
        } else {
            pendingResponseSubscriber = bodySubscriber;
            sched.runOrSchedule(); // in case data waiting already to be processed
        }
        return responseBodyCF;
    }

    @Override
    CompletableFuture<ExchangeImpl<T>> sendBodyAsync() {
        return sendBodyImpl().thenApply( v -> this);
    }

    @SuppressWarnings("unchecked")
    Stream(Http2Connection connection,
           Exchange<T> e,
           WindowController windowController)
    {
        super(e);
        this.connection = connection;
        this.windowController = windowController;
        this.request = e.request();
        this.requestPublisher = request.requestPublisher;  // may be null
        this.responseHeadersBuilder = new HttpHeadersBuilder();
        this.rspHeadersConsumer = new HeadersConsumer();
        this.requestPseudoHeaders = createPseudoHeaders(request);
        this.windowUpdater = new StreamWindowUpdateSender(connection);
    }

    private boolean checkRequestCancelled() {
        if (exchange.multi.requestCancelled()) {
            if (errorRef.get() == null) cancel();
            else sendCancelStreamFrame();
            return true;
        }
        return false;
    }

    /**
     * Entry point from Http2Connection reader thread.
     *
     * Data frames will be removed by response body thread.
     */
    void incoming(Http2Frame frame) throws IOException {
        if (debug.on()) debug.log("incoming: %s", frame);
        var cancelled = checkRequestCancelled() || closed;
        if ((frame instanceof HeaderFrame)) {
            HeaderFrame hframe = (HeaderFrame) frame;
            if (hframe.endHeaders()) {
                Log.logTrace("handling response (streamid={0})", streamid);
                handleResponse();
            }
            if (hframe.getFlag(HeaderFrame.END_STREAM)) {
                if (debug.on()) debug.log("handling END_STREAM: %d", streamid);
                receiveDataFrame(new DataFrame(streamid, DataFrame.END_STREAM, List.of()));
            }
        } else if (frame instanceof DataFrame) {
            if (cancelled) connection.dropDataFrame((DataFrame) frame);
            else receiveDataFrame((DataFrame) frame);
        } else {
            if (!cancelled) otherFrame(frame);
        }
    }

    void otherFrame(Http2Frame frame) throws IOException {
        switch (frame.type()) {
            case WindowUpdateFrame.TYPE ->  incoming_windowUpdate((WindowUpdateFrame) frame);
            case ResetFrame.TYPE        ->  incoming_reset((ResetFrame) frame);
            case PriorityFrame.TYPE     ->  incoming_priority((PriorityFrame) frame);

            default -> throw new IOException("Unexpected frame: " + frame.toString());
        }
    }

    // The Hpack decoder decodes into one of these consumers of name,value pairs

    DecodingCallback rspHeadersConsumer() {
        return rspHeadersConsumer;
    }

    protected void handleResponse() throws IOException {
        HttpHeaders responseHeaders = responseHeadersBuilder.build();
        responseCode = (int)responseHeaders
                .firstValueAsLong(":status")
                .orElseThrow(() -> new IOException("no statuscode in response"));

        response = new Response(
                request, exchange, responseHeaders, connection(),
                responseCode, HttpClient.Version.HTTP_2);

        /* TODO: review if needs to be removed
           the value is not used, but in case `content-length` doesn't parse as
           long, there will be NumberFormatException. If left as is, make sure
           code up the stack handles NFE correctly. */
        responseHeaders.firstValueAsLong("content-length");

        if (Log.headers()) {
            StringBuilder sb = new StringBuilder("RESPONSE HEADERS:\n");
            Log.dumpHeaders(sb, "    ", responseHeaders);
            Log.logHeaders(sb.toString());
        }

        // this will clear the response headers
        rspHeadersConsumer.reset();

        completeResponse(response);
    }

    void incoming_reset(ResetFrame frame) {
        Log.logTrace("Received RST_STREAM on stream {0}", streamid);
        if (endStreamReceived()) {
            Log.logTrace("Ignoring RST_STREAM frame received on remotely closed stream {0}", streamid);
        } else if (closed) {
            Log.logTrace("Ignoring RST_STREAM frame received on closed stream {0}", streamid);
        } else {
            Flow.Subscriber<?> subscriber =
                    responseSubscriber == null ? pendingResponseSubscriber : responseSubscriber;
            if (response == null && subscriber == null) {
                // we haven't receive the headers yet, and won't receive any!
                // handle reset now.
                handleReset(frame, subscriber);
            } else {
                // put it in the input queue in order to read all
                // pending data frames first. Indeed, a server may send
                // RST_STREAM after sending END_STREAM, in which case we should
                // ignore it. However, we won't know if we have received END_STREAM
                // or not until all pending data frames are read.
                receiveResetFrame(frame);
                // RST_STREAM was pushed to the queue. It will be handled by
                // asyncReceive after all pending data frames have been
                // processed.
                Log.logTrace("RST_STREAM pushed in queue for stream {0}", streamid);
            }
        }
    }

    void handleReset(ResetFrame frame, Flow.Subscriber<?> subscriber) {
        Log.logTrace("Handling RST_STREAM on stream {0}", streamid);
        if (!closed) {
            synchronized (this) {
                if (closed) {
                    if (debug.on()) debug.log("Stream already closed: ignoring RESET");
                    return;
                }
                closed = true;
            }
            try {
                int error = frame.getErrorCode();
                IOException e = new IOException("Received RST_STREAM: "
                        + ErrorFrame.stringForCode(error));
                if (errorRef.compareAndSet(null, e)) {
                    if (subscriber != null) {
                        subscriber.onError(e);
                    }
                }
                completeResponseExceptionally(e);
                if (!requestBodyCF.isDone()) {
                    requestBodyCF.completeExceptionally(errorRef.get()); // we may be sending the body..
                }
                if (responseBodyCF != null) {
                    responseBodyCF.completeExceptionally(errorRef.get());
                }
            } finally {
                connection.decrementStreamsCount(streamid);
                connection.closeStream(streamid);
            }
        } else {
            Log.logTrace("Ignoring RST_STREAM frame received on closed stream {0}", streamid);
        }
    }

    void incoming_priority(PriorityFrame frame) {
        // TODO: implement priority
        throw new UnsupportedOperationException("Not implemented");
    }

    private void incoming_windowUpdate(WindowUpdateFrame frame)
        throws IOException
    {
        int amount = frame.getUpdate();
        if (amount <= 0) {
            Log.logTrace("Resetting stream: {0}, Window Update amount: {1}",
                         streamid, amount);
            connection.resetStream(streamid, ResetFrame.FLOW_CONTROL_ERROR);
        } else {
            assert streamid != 0;
            boolean success = windowController.increaseStreamWindow(amount, streamid);
            if (!success) {  // overflow
                connection.resetStream(streamid, ResetFrame.FLOW_CONTROL_ERROR);
            }
        }
    }

    void incoming_pushPromise(HttpRequestImpl pushRequest,
                              PushedStream<T> pushStream)
        throws IOException
    {
        if (Log.requests()) {
            Log.logRequest("PUSH_PROMISE: " + pushRequest.toString());
        }
        PushGroup<T> pushGroup = exchange.getPushGroup();
        if (pushGroup == null || exchange.multi.requestCancelled()) {
            Log.logTrace("Rejecting push promise stream " + streamid);
            connection.resetStream(pushStream.streamid, ResetFrame.REFUSED_STREAM);
            pushStream.close();
            return;
        }

        PushGroup.Acceptor<T> acceptor = null;
        boolean accepted = false;
        try {
            acceptor = pushGroup.acceptPushRequest(pushRequest);
            accepted = acceptor.accepted();
        } catch (Throwable t) {
            if (debug.on())
                debug.log("PushPromiseHandler::applyPushPromise threw exception %s",
                          (Object)t);
        }
        if (!accepted) {
            // cancel / reject
            IOException ex = new IOException("Stream " + streamid + " cancelled by users handler");
            if (Log.trace()) {
                Log.logTrace("No body subscriber for {0}: {1}", pushRequest,
                        ex.getMessage());
            }
            pushStream.cancelImpl(ex);
            return;
        }

        assert accepted && acceptor != null;
        CompletableFuture<HttpResponse<T>> pushResponseCF = acceptor.cf();
        HttpResponse.BodyHandler<T> pushHandler = acceptor.bodyHandler();
        assert pushHandler != null;

        pushStream.requestSent();
        pushStream.setPushHandler(pushHandler);  // TODO: could wrap the handler to throw on acceptPushPromise ?
        // setup housekeeping for when the push is received
        // TODO: deal with ignoring of CF anti-pattern
        CompletableFuture<HttpResponse<T>> cf = pushStream.responseCF();
        cf.whenComplete((HttpResponse<T> resp, Throwable t) -> {
            t = Utils.getCompletionCause(t);
            if (Log.trace()) {
                Log.logTrace("Push completed on stream {0} for {1}{2}",
                             pushStream.streamid, resp,
                             ((t==null) ? "": " with exception " + t));
            }
            if (t != null) {
                pushGroup.pushError(t);
                pushResponseCF.completeExceptionally(t);
            } else {
                pushResponseCF.complete(resp);
            }
            pushGroup.pushCompleted();
        });

    }

    private OutgoingHeaders<Stream<T>> headerFrame(long contentLength) {
        HttpHeadersBuilder h = request.getSystemHeadersBuilder();
        if (contentLength > 0) {
            h.setHeader("content-length", Long.toString(contentLength));
        }
        HttpHeaders sysh = filterHeaders(h.build());
        HttpHeaders userh = filterHeaders(request.getUserHeaders());
        // Filter context restricted from userHeaders
        userh = HttpHeaders.of(userh.map(), Utils.CONTEXT_RESTRICTED(client()));

        final HttpHeaders uh = userh;

        // Filter any headers from systemHeaders that are set in userHeaders
        sysh = HttpHeaders.of(sysh.map(), (k,v) -> uh.firstValue(k).isEmpty());

        OutgoingHeaders<Stream<T>> f = new OutgoingHeaders<>(sysh, userh, this);
        if (contentLength == 0) {
            f.setFlag(HeadersFrame.END_STREAM);
            endStreamSent = true;
        }
        return f;
    }

    private boolean hasProxyAuthorization(HttpHeaders headers) {
        return headers.firstValue("proxy-authorization")
                      .isPresent();
    }

    // Determines whether we need to build a new HttpHeader object.
    //
    // Ideally we should pass the filter to OutgoingHeaders refactor the
    // code that creates the HeaderFrame to honor the filter.
    // We're not there yet - so depending on the filter we need to
    // apply and the content of the header we will try to determine
    //  whether anything might need to be filtered.
    // If nothing needs filtering then we can just use the
    // original headers.
    private boolean needsFiltering(HttpHeaders headers,
                                   BiPredicate<String, String> filter) {
        if (filter == Utils.PROXY_TUNNEL_FILTER || filter == Utils.PROXY_FILTER) {
            // we're either connecting or proxying
            // slight optimization: we only need to filter out
            // disabled schemes, so if there are none just
            // pass through.
            return Utils.proxyHasDisabledSchemes(filter == Utils.PROXY_TUNNEL_FILTER)
                    && hasProxyAuthorization(headers);
        } else {
            // we're talking to a server, either directly or through
            // a tunnel.
            // Slight optimization: we only need to filter out
            // proxy authorization headers, so if there are none just
            // pass through.
            return hasProxyAuthorization(headers);
        }
    }

    private HttpHeaders filterHeaders(HttpHeaders headers) {
        HttpConnection conn = connection();
        BiPredicate<String, String> filter = conn.headerFilter(request);
        if (needsFiltering(headers, filter)) {
            return HttpHeaders.of(headers.map(), filter);
        }
        return headers;
    }

    private static HttpHeaders createPseudoHeaders(HttpRequest request) {
        HttpHeadersBuilder hdrs = new HttpHeadersBuilder();
        String method = request.method();
        hdrs.setHeader(":method", method);
        URI uri = request.uri();
        hdrs.setHeader(":scheme", uri.getScheme());
        // TODO: userinfo deprecated. Needs to be removed
        hdrs.setHeader(":authority", uri.getAuthority());
        // TODO: ensure header names beginning with : not in user headers
        String query = uri.getRawQuery();
        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) {
            if (method.equalsIgnoreCase("OPTIONS")) {
                path = "*";
            } else {
                path = "/";
            }
        }
        if (query != null) {
            path += "?" + query;
        }
        hdrs.setHeader(":path", Utils.encode(path));
        return hdrs.build();
    }

    HttpHeaders getRequestPseudoHeaders() {
        return requestPseudoHeaders;
    }

    /** Sets endStreamReceived. Should be called only once. */
    void setEndStreamReceived() {
        if (debug.on()) debug.log("setEndStreamReceived: streamid=%d", streamid);
        assert remotelyClosed == false: "Unexpected endStream already set";
        remotelyClosed = true;
        responseReceived();
    }

    /** Tells whether, or not, the END_STREAM Flag has been seen in any frame
     *  received on this stream. */
    private boolean endStreamReceived() {
        return remotelyClosed;
    }

    @Override
    CompletableFuture<ExchangeImpl<T>> sendHeadersAsync() {
        if (debug.on()) debug.log("sendHeadersOnly()");
        if (Log.requests() && request != null) {
            Log.logRequest(request.toString());
        }
        if (requestPublisher != null) {
            requestContentLen = requestPublisher.contentLength();
        } else {
            requestContentLen = 0;
        }

        // At this point the stream doesn't have a streamid yet.
        // It will be allocated if we send the request headers.
        Throwable t = errorRef.get();
        if (t != null) {
            if (debug.on()) debug.log("stream already cancelled, headers not sent: %s", (Object)t);
            return MinimalFuture.failedFuture(t);
        }

        // sending the headers will cause the allocation of the stream id
        OutgoingHeaders<Stream<T>> f = headerFrame(requestContentLen);
        connection.sendFrame(f);
        CompletableFuture<ExchangeImpl<T>> cf = new MinimalFuture<>();
        cf.complete(this);  // #### good enough for now
        return cf;
    }

    @Override
    void released() {
        if (streamid > 0) {
            if (debug.on()) debug.log("Released stream %d", streamid);
            // remove this stream from the Http2Connection map.
            connection.decrementStreamsCount(streamid);
            connection.closeStream(streamid);
        } else {
            if (debug.on()) debug.log("Can't release stream %d", streamid);
        }
    }

    @Override
    void completed() {
        // There should be nothing to do here: the stream should have
        // been already closed (or will be closed shortly after).
    }

    boolean registerStream(int id, boolean registerIfCancelled) {
        boolean cancelled = closed || exchange.multi.requestCancelled();
        if (!cancelled || registerIfCancelled) {
            this.streamid = id;
            connection.putStream(this, streamid);
            if (debug.on()) {
                debug.log("Stream %d registered (cancelled: %b, registerIfCancelled: %b)",
                        streamid, cancelled, registerIfCancelled);
            }
        }
        return !cancelled;
    }

    void signalWindowUpdate() {
        RequestSubscriber subscriber = requestSubscriber;
        assert subscriber != null;
        if (debug.on()) debug.log("Signalling window update");
        subscriber.sendScheduler.runOrSchedule();
    }

    static final ByteBuffer COMPLETED = ByteBuffer.allocate(0);
    class RequestSubscriber implements Flow.Subscriber<ByteBuffer> {
        // can be < 0 if the actual length is not known.
        private final long contentLength;
        private volatile long remainingContentLength;
        private volatile Subscription subscription;

        // Holds the outgoing data. There will be at most 2 outgoing ByteBuffers.
        //  1) The data that was published by the request body Publisher, and
        //  2) the COMPLETED sentinel, since onComplete can be invoked without demand.
        final ConcurrentLinkedDeque<ByteBuffer> outgoing = new ConcurrentLinkedDeque<>();

        private final AtomicReference<Throwable> errorRef = new AtomicReference<>();
        // A scheduler used to honor window updates. Writing must be paused
        // when the window is exhausted, and resumed when the window acquires
        // some space. The sendScheduler makes it possible to implement this
        // behaviour in an asynchronous non-blocking way.
        // See RequestSubscriber::trySend below.
        final SequentialScheduler sendScheduler;

        RequestSubscriber(long contentLen) {
            this.contentLength = contentLen;
            this.remainingContentLength = contentLen;
            this.sendScheduler =
                    SequentialScheduler.lockingScheduler(this::trySend);
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (this.subscription != null) {
                throw new IllegalStateException("already subscribed");
            }
            this.subscription = subscription;
            if (debug.on())
                debug.log("RequestSubscriber: onSubscribe, request 1");
            subscription.request(1);
        }

        @Override
        public void onNext(ByteBuffer item) {
            if (debug.on())
                debug.log("RequestSubscriber: onNext(%d)", item.remaining());
            int size = outgoing.size();
            assert size == 0 : "non-zero size: " + size;
            onNextImpl(item);
        }

        private void onNextImpl(ByteBuffer item) {
            // Got some more request body bytes to send.
            if (requestBodyCF.isDone()) {
                // stream already cancelled, probably in timeout
                sendScheduler.stop();
                subscription.cancel();
                return;
            }
            outgoing.add(item);
            sendScheduler.runOrSchedule();
        }

        @Override
        public void onError(Throwable throwable) {
            if (debug.on())
                debug.log(() -> "RequestSubscriber: onError: " + throwable);
            // ensure that errors are handled within the flow.
            if (errorRef.compareAndSet(null, throwable)) {
                sendScheduler.runOrSchedule();
            }
        }

        @Override
        public void onComplete() {
            if (debug.on()) debug.log("RequestSubscriber: onComplete");
            int size = outgoing.size();
            assert size == 0 || size == 1 : "non-zero or one size: " + size;
            // last byte of request body has been obtained.
            // ensure that everything is completed within the flow.
            onNextImpl(COMPLETED);
        }

        // Attempts to send the data, if any.
        // Handles errors and completion state.
        // Pause writing if the send window is exhausted, resume it if the
        // send window has some bytes that can be acquired.
        void trySend() {
            try {
                // handle errors raised by onError;
                Throwable t = errorRef.get();
                if (t != null) {
                    sendScheduler.stop();
                    if (requestBodyCF.isDone()) return;
                    subscription.cancel();
                    requestBodyCF.completeExceptionally(t);
                    cancelImpl(t);
                    return;
                }
                int state = streamState;

                do {
                    // handle COMPLETED;
                    ByteBuffer item = outgoing.peekFirst();
                    if (item == null) return;
                    else if (item == COMPLETED) {
                        sendScheduler.stop();
                        complete();
                        return;
                    }

                    // handle bytes to send downstream
                    while (item.hasRemaining() && state == 0) {
                        if (debug.on()) debug.log("trySend: %d", item.remaining());
                        DataFrame df = getDataFrame(item);
                        if (df == null) {
                            if (debug.on())
                                debug.log("trySend: can't send yet: %d", item.remaining());
                            return; // the send window is exhausted: come back later
                        }

                        if (contentLength > 0) {
                            remainingContentLength -= df.getDataLength();
                            if (remainingContentLength < 0) {
                                String msg = connection().getConnectionFlow()
                                        + " stream=" + streamid + " "
                                        + "[" + Thread.currentThread().getName() + "] "
                                        + "Too many bytes in request body. Expected: "
                                        + contentLength + ", got: "
                                        + (contentLength - remainingContentLength);
                                assert streamid > 0;
                                connection.resetStream(streamid, ResetFrame.PROTOCOL_ERROR);
                                throw new IOException(msg);
                            } else if (remainingContentLength == 0) {
                                assert !endStreamSent : "internal error, send data after END_STREAM flag";
                                df.setFlag(DataFrame.END_STREAM);
                                endStreamSent = true;
                            }
                        } else {
                            assert !endStreamSent : "internal error, send data after END_STREAM flag";
                        }
                        if ((state = streamState) != 0) {
                            if (debug.on()) debug.log("trySend: cancelled: %s", String.valueOf(t));
                            break;
                        }
                        if (debug.on())
                            debug.log("trySend: sending: %d", df.getDataLength());
                        sendDataFrame(df);
                    }
                    if (state != 0) break;
                    assert !item.hasRemaining();
                    ByteBuffer b = outgoing.removeFirst();
                    assert b == item;
                } while (outgoing.peekFirst() != null);

                if (state != 0) {
                    t = errorRef.get();
                    if (t == null) t = new IOException(ResetFrame.stringForCode(streamState));
                    throw t;
                }

                if (debug.on()) debug.log("trySend: request 1");
                subscription.request(1);
            } catch (Throwable ex) {
                if (debug.on()) debug.log("trySend: ", ex);
                sendScheduler.stop();
                subscription.cancel();
                requestBodyCF.completeExceptionally(ex);
                // need to cancel the stream to 1. tell the server
                // we don't want to receive any more data and
                // 2. ensure that the operation ref count will be
                // decremented on the HttpClient.
                cancelImpl(ex);
            }
        }

        private void complete() throws IOException {
            long remaining = remainingContentLength;
            long written = contentLength - remaining;
            if (remaining > 0) {
                connection.resetStream(streamid, ResetFrame.PROTOCOL_ERROR);
                // let trySend() handle the exception
                throw new IOException(connection().getConnectionFlow()
                                     + " stream=" + streamid + " "
                                     + "[" + Thread.currentThread().getName() +"] "
                                     + "Too few bytes returned by the publisher ("
                                              + written + "/"
                                              + contentLength + ")");
            }
            if (!endStreamSent) {
                endStreamSent = true;
                connection.sendDataFrame(getEmptyEndStreamDataFrame());
            }
            requestBodyCF.complete(null);
        }
    }

    /**
     * Send a RESET frame to tell server to stop sending data on this stream
     */
    @Override
    public CompletableFuture<Void> ignoreBody() {
        try {
            connection.resetStream(streamid, ResetFrame.STREAM_CLOSED);
            return MinimalFuture.completedFuture(null);
        } catch (Throwable e) {
            Log.logTrace("Error resetting stream {0}", e.toString());
            return MinimalFuture.failedFuture(e);
        }
    }

    DataFrame getDataFrame(ByteBuffer buffer) {
        int requestAmount = Math.min(connection.getMaxSendFrameSize(), buffer.remaining());
        // blocks waiting for stream send window, if exhausted
        int actualAmount = windowController.tryAcquire(requestAmount, streamid, this);
        if (actualAmount <= 0) return null;
        ByteBuffer outBuf = Utils.sliceWithLimitedCapacity(buffer,  actualAmount);
        DataFrame df = new DataFrame(streamid, 0 , outBuf);
        return df;
    }

    private DataFrame getEmptyEndStreamDataFrame()  {
        return new DataFrame(streamid, DataFrame.END_STREAM, List.of());
    }

    /**
     * A List of responses relating to this stream. Normally there is only
     * one response, but intermediate responses like 100 are allowed
     * and must be passed up to higher level before continuing. Deals with races
     * such as if responses are returned before the CFs get created by
     * getResponseAsync()
     */

    final List<CompletableFuture<Response>> response_cfs = new ArrayList<>(5);

    @Override
    CompletableFuture<Response> getResponseAsync(Executor executor) {
        CompletableFuture<Response> cf;
        // The code below deals with race condition that can be caused when
        // completeResponse() is being called before getResponseAsync()
        synchronized (response_cfs) {
            if (!response_cfs.isEmpty()) {
                // This CompletableFuture was created by completeResponse().
                // it will be already completed.
                cf = response_cfs.remove(0);
                // if we find a cf here it should be already completed.
                // finding a non completed cf should not happen. just assert it.
                assert cf.isDone() : "Removing uncompleted response: could cause code to hang!";
            } else {
                // getResponseAsync() is called first. Create a CompletableFuture
                // that will be completed by completeResponse() when
                // completeResponse() is called.
                cf = new MinimalFuture<>();
                response_cfs.add(cf);
            }
        }
        if (executor != null && !cf.isDone()) {
            // protect from executing later chain of CompletableFuture operations from SelectorManager thread
            cf = cf.thenApplyAsync(r -> r, executor);
        }
        Log.logTrace("Response future (stream={0}) is: {1}", streamid, cf);
        PushGroup<?> pg = exchange.getPushGroup();
        if (pg != null) {
            // if an error occurs make sure it is recorded in the PushGroup
            cf = cf.whenComplete((t,e) -> pg.pushError(Utils.getCompletionCause(e)));
        }
        return cf;
    }

    /**
     * Completes the first uncompleted CF on list, and removes it. If there is no
     * uncompleted CF then creates one (completes it) and adds to list
     */
    void completeResponse(Response resp) {
        synchronized (response_cfs) {
            CompletableFuture<Response> cf;
            int cfs_len = response_cfs.size();
            for (int i=0; i<cfs_len; i++) {
                cf = response_cfs.get(i);
                if (!cf.isDone()) {
                    Log.logTrace("Completing response (streamid={0}): {1}",
                                 streamid, cf);
                    if (debug.on())
                        debug.log("Completing responseCF(%d) with response headers", i);
                    response_cfs.remove(cf);
                    cf.complete(resp);
                    return;
                } // else we found the previous response: just leave it alone.
            }
            cf = MinimalFuture.completedFuture(resp);
            Log.logTrace("Created completed future (streamid={0}): {1}",
                         streamid, cf);
            if (debug.on())
                debug.log("Adding completed responseCF(0) with response headers");
            response_cfs.add(cf);
        }
    }

    // methods to update state and remove stream when finished

    synchronized void requestSent() {
        requestSent = true;
        if (responseReceived) {
            if (debug.on()) debug.log("requestSent: streamid=%d", streamid);
            close();
        } else {
            if (debug.on()) {
                debug.log("requestSent: streamid=%d but response not received", streamid);
            }
        }
    }

    synchronized void responseReceived() {
        responseReceived = true;
        if (requestSent) {
            if (debug.on()) debug.log("responseReceived: streamid=%d", streamid);
            close();
        } else {
            if (debug.on()) {
                debug.log("responseReceived: streamid=%d but request not sent", streamid);
            }
        }
    }

    /**
     * same as above but for errors
     */
    void completeResponseExceptionally(Throwable t) {
        synchronized (response_cfs) {
            // use index to avoid ConcurrentModificationException
            // caused by removing the CF from within the loop.
            for (int i = 0; i < response_cfs.size(); i++) {
                CompletableFuture<Response> cf = response_cfs.get(i);
                if (!cf.isDone()) {
                    response_cfs.remove(i);
                    cf.completeExceptionally(t);
                    return;
                }
            }
            response_cfs.add(MinimalFuture.failedFuture(t));
        }
    }

    CompletableFuture<Void> sendBodyImpl() {
        requestBodyCF.whenComplete((v, t) -> requestSent());
        try {
            if (requestPublisher != null) {
                final RequestSubscriber subscriber = new RequestSubscriber(requestContentLen);
                requestPublisher.subscribe(requestSubscriber = subscriber);
            } else {
                // there is no request body, therefore the request is complete,
                // END_STREAM has already sent with outgoing headers
                requestBodyCF.complete(null);
            }
        } catch (Throwable t) {
            cancelImpl(t);
            requestBodyCF.completeExceptionally(t);
        }
        return requestBodyCF;
    }

    @Override
    void cancel() {
        if ((streamid == 0)) {
            cancel(new IOException("Stream cancelled before streamid assigned"));
        } else {
            cancel(new IOException("Stream " + streamid + " cancelled"));
        }
    }

    void onSubscriptionError(Throwable t) {
        errorRef.compareAndSet(null, t);
        if (debug.on()) debug.log("Got subscription error: %s", (Object)t);
        // This is the special case where the subscriber
        // has requested an illegal number of items.
        // In this case, the error doesn't come from
        // upstream, but from downstream, and we need to
        // handle the error without waiting for the inputQ
        // to be exhausted.
        stopRequested = true;
        sched.runOrSchedule();
    }

    @Override
    void cancel(IOException cause) {
        cancelImpl(cause);
    }

    void connectionClosing(Throwable cause) {
        Flow.Subscriber<?> subscriber =
                responseSubscriber == null ? pendingResponseSubscriber : responseSubscriber;
        errorRef.compareAndSet(null, cause);
        if (subscriber != null && !sched.isStopped() && !inputQ.isEmpty()) {
            sched.runOrSchedule();
        } else cancelImpl(cause);
    }

    // This method sends a RST_STREAM frame
    void cancelImpl(Throwable e) {
        errorRef.compareAndSet(null, e);
        if (debug.on()) {
            if (streamid == 0) debug.log("cancelling stream: %s", (Object)e);
            else debug.log("cancelling stream %d: %s", streamid, e);
        }
        if (Log.trace()) {
            if (streamid == 0) Log.logTrace("cancelling stream: {0}\n", e);
            else Log.logTrace("cancelling stream {0}: {1}\n", streamid, e);
        }
        boolean closing;
        if (closing = !closed) { // assigning closing to !closed
            synchronized (this) {
                if (closing = !closed) { // assigning closing to !closed
                    closed=true;
                }
            }
        }
        if (closing) { // true if the stream has not been closed yet
            if (responseSubscriber != null || pendingResponseSubscriber != null)
                sched.runOrSchedule();
        }
        completeResponseExceptionally(e);
        if (!requestBodyCF.isDone()) {
            requestBodyCF.completeExceptionally(errorRef.get()); // we may be sending the body..
        }
        if (responseBodyCF != null) {
            responseBodyCF.completeExceptionally(errorRef.get());
        }
        try {
            // will send a RST_STREAM frame
            if (streamid != 0 && streamState == 0) {
                e = Utils.getCompletionCause(e);
                if (e instanceof EOFException) {
                    // read EOF: no need to try & send reset
                    connection.decrementStreamsCount(streamid);
                    connection.closeStream(streamid);
                } else {
                    // no use to send CANCEL if already closed.
                    sendCancelStreamFrame();
                }
            }
        } catch (Throwable ex) {
            Log.logError(ex);
        }
    }

    void sendCancelStreamFrame() {
        // do not reset a stream until it has a streamid.
        if (streamid > 0 && markStream(ResetFrame.CANCEL) == 0) {
            connection.resetStream(streamid, ResetFrame.CANCEL);
        }
        close();
    }

    // This method doesn't send any frame
    void close() {
        if (closed) return;
        synchronized(this) {
            if (closed) return;
            closed = true;
        }
        if (debug.on()) debug.log("close stream %d", streamid);
        Log.logTrace("Closing stream {0}", streamid);
        connection.closeStream(streamid);
        Log.logTrace("Stream {0} closed", streamid);
    }

    static class PushedStream<T> extends Stream<T> {
        final PushGroup<T> pushGroup;
        // push streams need the response CF allocated up front as it is
        // given directly to user via the multi handler callback function.
        final CompletableFuture<Response> pushCF;
        CompletableFuture<HttpResponse<T>> responseCF;
        final HttpRequestImpl pushReq;
        HttpResponse.BodyHandler<T> pushHandler;

        PushedStream(PushGroup<T> pushGroup,
                     Http2Connection connection,
                     Exchange<T> pushReq) {
            // ## no request body possible, null window controller
            super(connection, pushReq, null);
            this.pushGroup = pushGroup;
            this.pushReq = pushReq.request();
            this.pushCF = new MinimalFuture<>();
            this.responseCF = new MinimalFuture<>();

        }

        CompletableFuture<HttpResponse<T>> responseCF() {
            return responseCF;
        }

        synchronized void setPushHandler(HttpResponse.BodyHandler<T> pushHandler) {
            this.pushHandler = pushHandler;
        }

        synchronized HttpResponse.BodyHandler<T> getPushHandler() {
            // ignored parameters to function can be used as BodyHandler
            return this.pushHandler;
        }

        // Following methods call the super class but in case of
        // error record it in the PushGroup. The error method is called
        // with a null value when no error occurred (is a no-op)
        @Override
        CompletableFuture<ExchangeImpl<T>> sendBodyAsync() {
            return super.sendBodyAsync()
                        .whenComplete((ExchangeImpl<T> v, Throwable t)
                                -> pushGroup.pushError(Utils.getCompletionCause(t)));
        }

        @Override
        CompletableFuture<ExchangeImpl<T>> sendHeadersAsync() {
            return super.sendHeadersAsync()
                        .whenComplete((ExchangeImpl<T> ex, Throwable t)
                                -> pushGroup.pushError(Utils.getCompletionCause(t)));
        }

        @Override
        CompletableFuture<Response> getResponseAsync(Executor executor) {
            CompletableFuture<Response> cf = pushCF.whenComplete(
                    (v, t) -> pushGroup.pushError(Utils.getCompletionCause(t)));
            if(executor!=null && !cf.isDone()) {
                cf  = cf.thenApplyAsync( r -> r, executor);
            }
            return cf;
        }

        @Override
        CompletableFuture<T> readBodyAsync(
                HttpResponse.BodyHandler<T> handler,
                boolean returnConnectionToPool,
                Executor executor)
        {
            return super.readBodyAsync(handler, returnConnectionToPool, executor)
                        .whenComplete((v, t) -> pushGroup.pushError(t));
        }

        @Override
        void completeResponse(Response r) {
            Log.logResponse(r::toString);
            pushCF.complete(r); // not strictly required for push API
            // start reading the body using the obtained BodySubscriber
            CompletableFuture<Void> start = new MinimalFuture<>();
            start.thenCompose( v -> readBodyAsync(getPushHandler(), false, getExchange().executor()))
                .whenComplete((T body, Throwable t) -> {
                    if (t != null) {
                        responseCF.completeExceptionally(t);
                    } else {
                        HttpResponseImpl<T> resp =
                                new HttpResponseImpl<>(r.request, r, null, body, getExchange());
                        responseCF.complete(resp);
                    }
                });
            start.completeAsync(() -> null, getExchange().executor());
        }

        @Override
        void completeResponseExceptionally(Throwable t) {
            pushCF.completeExceptionally(t);
        }

//        @Override
//        synchronized void responseReceived() {
//            super.responseReceived();
//        }

        // create and return the PushResponseImpl
        @Override
        protected void handleResponse() {
            HttpHeaders responseHeaders = responseHeadersBuilder.build();
            responseCode = (int)responseHeaders
                .firstValueAsLong(":status")
                .orElse(-1);

            if (responseCode == -1) {
                completeResponseExceptionally(new IOException("No status code"));
            }

            this.response = new Response(
                pushReq, exchange, responseHeaders, connection(),
                responseCode, HttpClient.Version.HTTP_2);

            /* TODO: review if needs to be removed
               the value is not used, but in case `content-length` doesn't parse
               as long, there will be NumberFormatException. If left as is, make
               sure code up the stack handles NFE correctly. */
            responseHeaders.firstValueAsLong("content-length");

            if (Log.headers()) {
                StringBuilder sb = new StringBuilder("RESPONSE HEADERS");
                sb.append(" (streamid=").append(streamid).append("):\n");
                Log.dumpHeaders(sb, "    ", responseHeaders);
                Log.logHeaders(sb.toString());
            }

            rspHeadersConsumer.reset();

            // different implementations for normal streams and pushed streams
            completeResponse(response);
        }
    }

    final class StreamWindowUpdateSender extends WindowUpdateSender {

        StreamWindowUpdateSender(Http2Connection connection) {
            super(connection);
        }

        @Override
        int getStreamId() {
            return streamid;
        }

        @Override
        String dbgString() {
            String dbg = dbgString;
            if (dbg != null) return dbg;
            if (streamid == 0) {
                return connection.dbgString() + ":WindowUpdateSender(stream: ?)";
            } else {
                dbg = connection.dbgString() + ":WindowUpdateSender(stream: " + streamid + ")";
                return dbgString = dbg;
            }
        }
    }

    /**
     * Returns true if this exchange was canceled.
     * @return true if this exchange was canceled.
     */
    synchronized boolean isCanceled() {
        return errorRef.get() != null;
    }

    /**
     * Returns the cause for which this exchange was canceled, if available.
     * @return the cause for which this exchange was canceled, if available.
     */
    synchronized Throwable getCancelCause() {
        return errorRef.get();
    }

    final String dbgString() {
        return connection.dbgString() + "/Stream("+streamid+")";
    }

    private class HeadersConsumer extends Http2Connection.ValidatingHeadersConsumer {

        void reset() {
            super.reset();
            responseHeadersBuilder.clear();
            debug.log("Response builder cleared, ready to receive new headers.");
        }

        @Override
        public void onDecoded(CharSequence name, CharSequence value)
            throws UncheckedIOException
        {
            String n = name.toString();
            String v = value.toString();
            super.onDecoded(n, v);
            responseHeadersBuilder.addHeader(n, v);
            if (Log.headers() && Log.trace()) {
                Log.logTrace("RECEIVED HEADER (streamid={0}): {1}: {2}",
                             streamid, n, v);
            }
        }
    }

    private static final VarHandle STREAM_STATE;
    private static final VarHandle DEREGISTERED;
    static {
        try {
            STREAM_STATE = MethodHandles.lookup()
                    .findVarHandle(Stream.class, "streamState", int.class);
            DEREGISTERED = MethodHandles.lookup()
                    .findVarHandle(Stream.class, "deRegistered", boolean.class);
        } catch (Exception x) {
            throw new ExceptionInInitializerError(x);
        }
    }
}
