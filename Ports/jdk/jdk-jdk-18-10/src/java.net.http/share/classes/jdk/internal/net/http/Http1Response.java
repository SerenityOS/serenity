/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import jdk.internal.net.http.ResponseContent.BodyParser;
import jdk.internal.net.http.ResponseContent.UnknownLengthBodyParser;
import jdk.internal.net.http.ResponseSubscribers.TrustedSubscriber;
import jdk.internal.net.http.common.Log;
import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.MinimalFuture;
import jdk.internal.net.http.common.Utils;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.net.http.HttpResponse.BodySubscribers.discarding;
import static jdk.internal.net.http.common.Utils.wrapWithExtraDetail;
import static jdk.internal.net.http.RedirectFilter.HTTP_NOT_MODIFIED;

/**
 * Handles a HTTP/1.1 response (headers + body).
 * There can be more than one of these per Http exchange.
 */
class Http1Response<T> {

    private volatile ResponseContent content;
    private final HttpRequestImpl request;
    private Response response;
    private final HttpConnection connection;
    private HttpHeaders headers;
    private int responseCode;
    private final Http1Exchange<T> exchange;
    private boolean return2Cache; // return connection to cache when finished
    private final HeadersReader headersReader; // used to read the headers
    private final BodyReader bodyReader; // used to read the body
    private final Http1AsyncReceiver asyncReceiver;
    private volatile EOFException eof;
    private volatile BodyParser bodyParser;
    // max number of bytes of (fixed length) body to ignore on redirect
    private final static int MAX_IGNORE = 1024;

    // Revisit: can we get rid of this?
    static enum State {INITIAL, READING_HEADERS, READING_BODY, DONE}
    private volatile State readProgress = State.INITIAL;

    final Logger debug = Utils.getDebugLogger(this::dbgString, Utils.DEBUG);
    final static AtomicLong responseCount = new AtomicLong();
    final long id = responseCount.incrementAndGet();
    private Http1HeaderParser hd;

    Http1Response(HttpConnection conn,
                  Http1Exchange<T> exchange,
                  Http1AsyncReceiver asyncReceiver) {
        this.readProgress = State.INITIAL;
        this.request = exchange.request();
        this.exchange = exchange;
        this.connection = conn;
        this.asyncReceiver = asyncReceiver;
        headersReader = new HeadersReader(this::advance);
        bodyReader = new BodyReader(this::advance);

        hd = new Http1HeaderParser();
        readProgress = State.READING_HEADERS;
        headersReader.start(hd);
        asyncReceiver.subscribe(headersReader);
    }

    String dbgTag;
    private String dbgString() {
        String dbg = dbgTag;
        if (dbg == null) {
            String cdbg = connection.dbgTag;
            if (cdbg != null) {
                dbgTag = dbg = "Http1Response(id=" + id + ", " + cdbg + ")";
            } else {
                dbg = "Http1Response(id=" + id + ")";
            }
        }
        return dbg;
    }

    // The ClientRefCountTracker is used to track the state
    // of a pending operation. Altough there usually is a single
    // point where the operation starts, it may terminate at
    // different places.
    private final class ClientRefCountTracker {
        final HttpClientImpl client = connection.client();
        // state & 0x01 != 0 => acquire called
        // state & 0x02 != 0 => tryRelease called
        byte state;

        public synchronized void acquire() {
            if (state == 0) {
                // increment the reference count on the HttpClientImpl
                // to prevent the SelectorManager thread from exiting
                // until our operation is complete.
                if (debug.on())
                    debug.log("Operation started: incrementing ref count for %s", client);
                client.reference();
                state = 0x01;
            } else {
                if (debug.on())
                    debug.log("Operation ref count for %s is already %s",
                              client, ((state & 0x2) == 0x2) ? "released." : "incremented!" );
                assert (state & 0x01) == 0 : "reference count already incremented";
            }
        }

        public synchronized void tryRelease() {
            if (state == 0x01) {
                // decrement the reference count on the HttpClientImpl
                // to allow the SelectorManager thread to exit if no
                // other operation is pending and the facade is no
                // longer referenced.
                if (debug.on())
                    debug.log("Operation finished: decrementing ref count for %s", client);
                client.unreference();
            } else if (state == 0) {
                if (debug.on())
                    debug.log("Operation finished: releasing ref count for %s", client);
            } else if ((state & 0x02) == 0x02) {
                if (debug.on())
                    debug.log("ref count for %s already released", client);
            }
            state |= 0x02;
        }
    }

    private volatile boolean firstTimeAround = true;

    public CompletableFuture<Response> readHeadersAsync(Executor executor) {
        if (debug.on())
            debug.log("Reading Headers: (remaining: "
                      + asyncReceiver.remaining() +") "  + readProgress);

        if (firstTimeAround) {
            if (debug.on()) debug.log("First time around");
            firstTimeAround = false;
        } else {
            // with expect continue we will resume reading headers + body.
            asyncReceiver.unsubscribe(bodyReader);
            bodyReader.reset();

            hd = new Http1HeaderParser();
            readProgress = State.READING_HEADERS;
            headersReader.reset();
            headersReader.start(hd);
            asyncReceiver.subscribe(headersReader);
        }

        CompletableFuture<State> cf = headersReader.completion();
        assert cf != null : "parsing not started";
        if (debug.on()) {
            debug.log("headersReader is %s",
                    cf == null ? "not yet started"
                            : cf.isDone() ? "already completed"
                            : "not yet completed");
        }

        Function<State, Response> lambda = (State completed) -> {
                assert completed == State.READING_HEADERS;
                if (debug.on())
                    debug.log("Reading Headers: creating Response object;"
                              + " state is now " + readProgress);
                asyncReceiver.unsubscribe(headersReader);
                responseCode = hd.responseCode();
                headers = hd.headers();

                response = new Response(request,
                                        exchange.getExchange(),
                                        headers,
                                        connection,
                                        responseCode,
                                        HTTP_1_1);

                if (Log.headers()) {
                    StringBuilder sb = new StringBuilder("RESPONSE HEADERS:\n");
                    Log.dumpHeaders(sb, "    ", headers);
                    Log.logHeaders(sb.toString());
                }

                return response;
            };

        if (executor != null) {
            return cf.thenApplyAsync(lambda, executor);
        } else {
            return cf.thenApply(lambda);
        }
    }

    private boolean finished;

    synchronized void completed() {
        finished = true;
    }

    synchronized boolean finished() {
        return finished;
    }

    /**
     * Return known fixed content length or -1 if chunked, or -2 if no content-length
     * information in which case, connection termination delimits the response body
     */
    long fixupContentLen(long clen) {
        if (request.method().equalsIgnoreCase("HEAD") || responseCode == HTTP_NOT_MODIFIED) {
            return 0L;
        }
        if (clen == -1L) {
            if (headers.firstValue("Transfer-encoding").orElse("")
                       .equalsIgnoreCase("chunked")) {
                return -1L;
            }
            if (responseCode == 101) {
                // this is a h2c or websocket upgrade, contentlength must be zero
                return 0L;
            }
            return -2L;
        }
        return clen;
    }

    /**
     * Read up to MAX_IGNORE bytes discarding
     */
    public CompletableFuture<Void> ignoreBody(Executor executor) {
        int clen = (int)headers.firstValueAsLong("Content-Length").orElse(-1);
        if (clen == -1 || clen > MAX_IGNORE) {
            connection.close();
            return MinimalFuture.completedFuture(null); // not treating as error
        } else {
            return readBody(discarding(), !request.isWebSocket(), executor);
        }
    }

    // Used for those response codes that have no body associated
    public void nullBody(HttpResponse<T> resp, Throwable t) {
        if (t != null) connection.close();
        else {
            return2Cache = !request.isWebSocket();
            onFinished();
        }
    }

    static final Flow.Subscription NOP = new Flow.Subscription() {
        @Override
        public void request(long n) { }
        public void cancel() { }
    };

    /**
     * The Http1AsyncReceiver ensures that all calls to
     * the subscriber, including onSubscribe, occur sequentially.
     * There could however be some race conditions that could happen
     * in case of unexpected errors thrown at unexpected places, which
     * may cause onError to be called multiple times.
     * The Http1BodySubscriber will ensure that the user subscriber
     * is actually completed only once - and only after it is
     * subscribed.
     * @param <U> The type of response.
     */
    final static class Http1BodySubscriber<U> implements TrustedSubscriber<U> {
        final HttpResponse.BodySubscriber<U> userSubscriber;
        final AtomicBoolean completed = new AtomicBoolean();
        volatile Throwable withError;
        volatile boolean subscribed;
        Http1BodySubscriber(HttpResponse.BodySubscriber<U> userSubscriber) {
            this.userSubscriber = userSubscriber;
        }

        @Override
        public boolean needsExecutor() {
            return TrustedSubscriber.needsExecutor(userSubscriber);
        }

        // propagate the error to the user subscriber, even if not
        // subscribed yet.
        private void propagateError(Throwable t) {
            assert t != null;
            try {
                // if unsubscribed at this point, it will not
                // get subscribed later - so do it now and
                // propagate the error
                if (subscribed == false) {
                    subscribed = true;
                    userSubscriber.onSubscribe(NOP);
                }
            } finally  {
                // if onError throws then there is nothing to do
                // here: let the caller deal with it by logging
                // and closing the connection.
                userSubscriber.onError(t);
            }
        }

        // complete the subscriber, either normally or exceptionally
        // ensure that the subscriber is completed only once.
        private void complete(Throwable t) {
            if (completed.compareAndSet(false, true)) {
                t  = withError = Utils.getCompletionCause(t);
                if (t == null) {
                    assert subscribed;
                    try {
                        userSubscriber.onComplete();
                    } catch (Throwable x) {
                        // Simply propagate the error by calling
                        // onError on the user subscriber, and let the
                        // connection be reused since we should have received
                        // and parsed all the bytes when we reach here.
                        // If onError throws in turn, then we will simply
                        // let that new exception flow up to the caller
                        // and let it deal with it.
                        // (i.e: log and close the connection)
                        // Note that rethrowing here could introduce a
                        // race that might cause the next send() operation to
                        // fail as the connection has already been put back
                        // into the cache when we reach here.
                        propagateError(t = withError = Utils.getCompletionCause(x));
                    }
                } else {
                    propagateError(t);
                }
            }
        }

        @Override
        public CompletionStage<U> getBody() {
            return userSubscriber.getBody();
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (!subscribed) {
                subscribed = true;
                userSubscriber.onSubscribe(subscription);
            } else {
                // could be already subscribed and completed
                // if an unexpected error occurred before the actual
                // subscription - though that's not supposed
                // happen.
                assert completed.get();
            }
        }
        @Override
        public void onNext(List<ByteBuffer> item) {
            assert !completed.get();
            userSubscriber.onNext(item);
        }
        @Override
        public void onError(Throwable throwable) {
            complete(throwable);
        }
        @Override
        public void onComplete() {
            complete(null);
        }
    }

    public <U> CompletableFuture<U> readBody(HttpResponse.BodySubscriber<U> p,
                                         boolean return2Cache,
                                         Executor executor) {
        if (debug.on()) {
            debug.log("readBody: return2Cache: " + return2Cache);
            if (request.isWebSocket() && return2Cache && connection != null) {
                debug.log("websocket connection will be returned to cache: "
                        + connection.getClass() + "/" + connection );
            }
        }
        assert !return2Cache || !request.isWebSocket();
        this.return2Cache = return2Cache;
        final Http1BodySubscriber<U> subscriber = new Http1BodySubscriber<>(p);

        final CompletableFuture<U> cf = new MinimalFuture<>();

        long clen0 = headers.firstValueAsLong("Content-Length").orElse(-1L);
        final long clen = fixupContentLen(clen0);

        // expect-continue reads headers and body twice.
        // if we reach here, we must reset the headersReader state.
        asyncReceiver.unsubscribe(headersReader);
        headersReader.reset();
        ClientRefCountTracker refCountTracker = new ClientRefCountTracker();

        // We need to keep hold on the client facade until the
        // tracker has been incremented.
        connection.client().reference();
        executor.execute(() -> {
            try {
                content = new ResponseContent(
                        connection, clen, headers, subscriber,
                        this::onFinished
                );
                if (cf.isCompletedExceptionally()) {
                    // if an error occurs during subscription
                    connection.close();
                    return;
                }
                // increment the reference count on the HttpClientImpl
                // to prevent the SelectorManager thread from exiting until
                // the body is fully read.
                refCountTracker.acquire();
                bodyParser = content.getBodyParser(
                    (t) -> {
                        try {
                            if (t != null) {
                                try {
                                    subscriber.onError(t);
                                } finally {
                                    cf.completeExceptionally(t);
                                }
                            }
                        } finally {
                            bodyReader.onComplete(t);
                            if (t != null) {
                                connection.close();
                            }
                        }
                    });
                bodyReader.start(bodyParser);
                CompletableFuture<State> bodyReaderCF = bodyReader.completion();
                asyncReceiver.subscribe(bodyReader);
                assert bodyReaderCF != null : "parsing not started";
                // Make sure to keep a reference to asyncReceiver from
                // within this
                CompletableFuture<?> trailingOp = bodyReaderCF.whenComplete((s,t) ->  {
                    t = Utils.getCompletionCause(t);
                    try {
                        if (t == null) {
                            if (debug.on()) debug.log("Finished reading body: " + s);
                            assert s == State.READING_BODY;
                        }
                        if (t != null) {
                            subscriber.onError(t);
                            cf.completeExceptionally(t);
                        }
                    } catch (Throwable x) {
                        // not supposed to happen
                        asyncReceiver.onReadError(x);
                    } finally {
                        // we're done: release the ref count for
                        // the current operation.
                        refCountTracker.tryRelease();
                    }
                });
                connection.addTrailingOperation(trailingOp);
            } catch (Throwable t) {
               if (debug.on()) debug.log("Failed reading body: " + t);
                try {
                    subscriber.onError(t);
                    cf.completeExceptionally(t);
                } finally {
                    asyncReceiver.onReadError(t);
                }
            } finally {
                connection.client().unreference();
            }
        });

        ResponseSubscribers.getBodyAsync(executor, p, cf, (t) -> {
            cf.completeExceptionally(t);
            asyncReceiver.setRetryOnError(false);
            asyncReceiver.onReadError(t);
        });

        return cf.whenComplete((s,t) -> {
            if (t != null) {
                // If an exception occurred, release the
                // ref count for the current operation, as
                // it may never be triggered otherwise
                // (BodySubscriber ofInputStream)
                // If there was no exception then the
                // ref count will be/have been released when
                // the last byte of the response is/was received
                refCountTracker.tryRelease();
            }
        });
    }


    private void onFinished() {
        asyncReceiver.clear();
        if (return2Cache) {
            Log.logTrace("Attempting to return connection to the pool: {0}", connection);
            // TODO: need to do something here?
            // connection.setAsyncCallbacks(null, null, null);

            // don't return the connection to the cache if EOF happened.
            if (debug.on())
                debug.log(connection.getConnectionFlow() + ": return to HTTP/1.1 pool");
            connection.closeOrReturnToCache(eof == null ? headers : null);
        }
    }

    HttpHeaders responseHeaders() {
        return headers;
    }

    int responseCode() {
        return responseCode;
    }

// ================ Support for plugging into Http1Receiver   =================
// ============================================================================

    // Callback: Error receiver: Consumer of Throwable.
    void onReadError(Throwable t) {
        Log.logError(t);
        Receiver<?> receiver = receiver(readProgress);
        if (t instanceof EOFException) {
            debug.log(Level.DEBUG, "onReadError: received EOF");
            eof = (EOFException) t;
        }
        CompletableFuture<?> cf = receiver == null ? null : receiver.completion();
        debug.log(Level.DEBUG, () -> "onReadError: cf is "
                + (cf == null  ? "null"
                : (cf.isDone() ? "already completed"
                               : "not yet completed")));
        if (cf != null) {
            cf.completeExceptionally(t);
        } else {
            debug.log(Level.DEBUG, "onReadError", t);
        }
        debug.log(Level.DEBUG, () -> "closing connection: cause is " + t);
        connection.close();
    }

    // ========================================================================

    private State advance(State previous) {
        assert readProgress == previous;
        switch(previous) {
            case READING_HEADERS:
                asyncReceiver.unsubscribe(headersReader);
                return readProgress = State.READING_BODY;
            case READING_BODY:
                asyncReceiver.unsubscribe(bodyReader);
                return readProgress = State.DONE;
            default:
                throw new InternalError("can't advance from " + previous);
        }
    }

    Receiver<?> receiver(State state) {
        return switch (state) {
            case READING_HEADERS    -> headersReader;
            case READING_BODY       -> bodyReader;

            default -> null;
        };

    }

    static abstract class Receiver<T>
            implements Http1AsyncReceiver.Http1AsyncDelegate {
        abstract void start(T parser);
        abstract CompletableFuture<State> completion();
        // accepts a buffer from upstream.
        // this should be implemented as a simple call to
        // accept(ref, parser, cf)
        public abstract boolean tryAsyncReceive(ByteBuffer buffer);
        public abstract void onReadError(Throwable t);
        // handle a byte buffer received from upstream.
        // this method should set the value of Http1Response.buffer
        // to ref.get() before beginning parsing.
        abstract void handle(ByteBuffer buf, T parser,
                             CompletableFuture<State> cf);
        // resets this objects state so that it can be reused later on
        // typically puts the reference to parser and completion to null
        abstract void reset();

        // accepts a byte buffer received from upstream
        // returns true if the buffer is fully parsed and more data can
        // be accepted, false otherwise.
        final boolean accept(ByteBuffer buf, T parser,
                CompletableFuture<State> cf) {
            if (cf == null || parser == null || cf.isDone()) return false;
            handle(buf, parser, cf);
            return !cf.isDone();
        }
        public abstract void onSubscribe(AbstractSubscription s);
        public abstract AbstractSubscription subscription();

    }

    // Invoked with each new ByteBuffer when reading headers...
    final class HeadersReader extends Receiver<Http1HeaderParser> {
        final Consumer<State> onComplete;
        volatile Http1HeaderParser parser;
        volatile CompletableFuture<State> cf;
        volatile long count; // bytes parsed (for debug)
        volatile AbstractSubscription subscription;

        HeadersReader(Consumer<State> onComplete) {
            this.onComplete = onComplete;
        }

        @Override
        public AbstractSubscription subscription() {
            return subscription;
        }

        @Override
        public void onSubscribe(AbstractSubscription s) {
            this.subscription = s;
            s.request(1);
        }

        @Override
        void reset() {
            cf = null;
            parser = null;
            count = 0;
            subscription = null;
        }

        // Revisit: do we need to support restarting?
        @Override
        final void start(Http1HeaderParser hp) {
            count = 0;
            cf = new MinimalFuture<>();
            parser = hp;
        }

        @Override
        CompletableFuture<State> completion() {
            return cf;
        }

        @Override
        public final boolean tryAsyncReceive(ByteBuffer ref) {
            boolean hasDemand = subscription.demand().tryDecrement();
            assert hasDemand;
            boolean needsMore = accept(ref, parser, cf);
            if (needsMore) subscription.request(1);
            return needsMore;
        }

        @Override
        public final void onReadError(Throwable t) {
            t = wrapWithExtraDetail(t, parser::currentStateMessage);
            Http1Response.this.onReadError(t);
        }

        @Override
        final void handle(ByteBuffer b,
                          Http1HeaderParser parser,
                          CompletableFuture<State> cf) {
            assert cf != null : "parsing not started";
            assert parser != null : "no parser";
            try {
                count += b.remaining();
                if (debug.on())
                    debug.log("Sending " + b.remaining() + "/" + b.capacity()
                              + " bytes to header parser");
                if (parser.parse(b)) {
                    count -= b.remaining();
                    if (debug.on())
                        debug.log("Parsing headers completed. bytes=" + count);
                    onComplete.accept(State.READING_HEADERS);
                    cf.complete(State.READING_HEADERS);
                }
            } catch (Throwable t) {
                if (debug.on())
                    debug.log("Header parser failed to handle buffer: " + t);
                cf.completeExceptionally(t);
            }
        }

        @Override
        public void close(Throwable error) {
            // if there's no error nothing to do: the cf should/will
            // be completed.
            if (error != null) {
                CompletableFuture<State> cf = this.cf;
                if (cf != null) {
                    if (debug.on())
                        debug.log("close: completing header parser CF with " + error);
                    cf.completeExceptionally(error);
                }
            }
        }
    }

    // Invoked with each new ByteBuffer when reading bodies...
    final class BodyReader extends Receiver<BodyParser> {
        final Consumer<State> onComplete;
        volatile BodyParser parser;
        volatile CompletableFuture<State> cf;
        volatile AbstractSubscription subscription;
        BodyReader(Consumer<State> onComplete) {
            this.onComplete = onComplete;
        }

        @Override
        void reset() {
            parser = null;
            cf = null;
            subscription = null;
        }

        // Revisit: do we need to support restarting?
        @Override
        final void start(BodyParser parser) {
            cf = new MinimalFuture<>();
            this.parser = parser;
        }

        @Override
        CompletableFuture<State> completion() {
            return cf;
        }

        @Override
        public final boolean tryAsyncReceive(ByteBuffer b) {
            return accept(b, parser, cf);
        }

        @Override
        public final void onReadError(Throwable t) {
            if (t instanceof EOFException && bodyParser != null &&
                    bodyParser instanceof UnknownLengthBodyParser) {
                ((UnknownLengthBodyParser)bodyParser).complete();
                return;
            }
            t = wrapWithExtraDetail(t, parser::currentStateMessage);
            Http1Response.this.onReadError(t);
        }

        @Override
        public AbstractSubscription subscription() {
            return subscription;
        }

        @Override
        public void onSubscribe(AbstractSubscription s) {
            this.subscription = s;
            try {
                parser.onSubscribe(s);
            } catch (Throwable t) {
                cf.completeExceptionally(t);
                throw t;
            }
        }

        @Override
        final void handle(ByteBuffer b,
                          BodyParser parser,
                          CompletableFuture<State> cf) {
            assert cf != null : "parsing not started";
            assert parser != null : "no parser";
            try {
                if (debug.on())
                    debug.log("Sending " + b.remaining() + "/" + b.capacity()
                              + " bytes to body parser");
                parser.accept(b);
            } catch (Throwable t) {
                if (debug.on())
                    debug.log("Body parser failed to handle buffer: " + t);
                if (!cf.isDone()) {
                    cf.completeExceptionally(t);
                }
            }
        }

        final void onComplete(Throwable closedExceptionally) {
            if (cf.isDone()) return;
            if (closedExceptionally != null) {
                cf.completeExceptionally(closedExceptionally);
            } else {
                onComplete.accept(State.READING_BODY);
                cf.complete(State.READING_BODY);
            }
        }

        @Override
        public final void close(Throwable error) {
            CompletableFuture<State> cf = this.cf;
            if (cf != null && !cf.isDone()) {
                // we want to make sure dependent actions are triggered
                // in order to make sure the client reference count
                // is decremented
                if (error != null) {
                    if (debug.on())
                        debug.log("close: completing body parser CF with " + error);
                    cf.completeExceptionally(error);
                } else {
                    if (debug.on())
                        debug.log("close: completing body parser CF");
                    cf.complete(State.READING_BODY);
                }
            }
        }

        @Override
        public String toString() {
            return super.toString() + "/parser=" + String.valueOf(parser);
        }
    }
}
