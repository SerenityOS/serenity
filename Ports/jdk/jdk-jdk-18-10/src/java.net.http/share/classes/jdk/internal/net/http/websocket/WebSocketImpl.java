/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http.websocket;

import jdk.internal.net.http.HttpClientFacade;
import jdk.internal.net.http.common.Demand;
import jdk.internal.net.http.common.Log;
import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.MinimalFuture;
import jdk.internal.net.http.common.SequentialScheduler;
import jdk.internal.net.http.common.Utils;
import jdk.internal.net.http.websocket.OpeningHandshake.Result;

import java.io.IOException;
import java.lang.ref.Reference;
import java.net.ProtocolException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static jdk.internal.net.http.common.MinimalFuture.failedFuture;
import static jdk.internal.net.http.websocket.StatusCodes.CLOSED_ABNORMALLY;
import static jdk.internal.net.http.websocket.StatusCodes.NO_STATUS_CODE;
import static jdk.internal.net.http.websocket.StatusCodes.isLegalToSendFromClient;
import static jdk.internal.net.http.websocket.WebSocketImpl.State.BINARY;
import static jdk.internal.net.http.websocket.WebSocketImpl.State.CLOSE;
import static jdk.internal.net.http.websocket.WebSocketImpl.State.ERROR;
import static jdk.internal.net.http.websocket.WebSocketImpl.State.IDLE;
import static jdk.internal.net.http.websocket.WebSocketImpl.State.OPEN;
import static jdk.internal.net.http.websocket.WebSocketImpl.State.PING;
import static jdk.internal.net.http.websocket.WebSocketImpl.State.PONG;
import static jdk.internal.net.http.websocket.WebSocketImpl.State.TEXT;
import static jdk.internal.net.http.websocket.WebSocketImpl.State.WAITING;

/*
 * A WebSocket client.
 */
public final class WebSocketImpl implements WebSocket {

    private static final Logger debug =
            Utils.getWebSocketLogger("[WebSocket]"::toString, Utils.DEBUG_WS);
    private final AtomicLong sendCounter = new AtomicLong();
    private final AtomicLong receiveCounter = new AtomicLong();

    enum State {
        OPEN,
        IDLE,
        WAITING,
        TEXT,
        BINARY,
        PING,
        PONG,
        CLOSE,
        ERROR
    }

    private final AtomicReference<ByteBuffer> lastAutomaticPong = new AtomicReference<>();
    private final MinimalFuture<WebSocket> DONE = MinimalFuture.completedFuture(this);
    private volatile boolean inputClosed;
    private final AtomicBoolean outputClosed = new AtomicBoolean();

    private final AtomicReference<State> state = new AtomicReference<>(OPEN);

    /* Components of calls to Listener's methods */
    private boolean last;
    private ByteBuffer binaryData;
    private CharSequence text;
    private int statusCode;
    private String reason;
    private final AtomicReference<Throwable> error = new AtomicReference<>();

    private final URI uri;
    private final String subprotocol;
    private final Listener listener;

    private final AtomicBoolean pendingTextOrBinary = new AtomicBoolean();
    private final AtomicBoolean pendingPingOrPong = new AtomicBoolean();
    private final Transport transport;
    private final SequentialScheduler receiveScheduler
            = new SequentialScheduler(new ReceiveTask());
    private final Demand demand = new Demand();
    private final Executor clientExecutor;

    public static CompletableFuture<WebSocket> newInstanceAsync(BuilderImpl b) {
        Function<Result, WebSocket> newWebSocket = r -> {
            WebSocket ws = newInstance(b.getUri(),
                                       b.getClient(),
                                       r.subprotocol,
                                       b.getListener(),
                                       r.transport);
            // Make sure we don't release the builder until this lambda
            // has been executed. The builder has a strong reference to
            // the HttpClientFacade, and we want to keep that live until
            // after the raw channel is created and passed to WebSocketImpl.
            Reference.reachabilityFence(b);
            return ws;
        };
        OpeningHandshake h;
        try {
            h = new OpeningHandshake(b);
        } catch (Throwable e) {
            return failedFuture(e);
        }
        return h.send().thenApply(newWebSocket);
    }

    /* Exposed for testing purposes */
    static WebSocketImpl newInstance(URI uri,
                                     HttpClient client,
                                     String subprotocol,
                                     Listener listener,
                                     TransportFactory transport) {
        WebSocketImpl ws = new WebSocketImpl(uri, client, subprotocol, listener, transport);
        // This initialisation is outside of the constructor for the sake of
        // safe publication of WebSocketImpl.this
        ws.signalOpen();
        return ws;
    }

    private WebSocketImpl(URI uri,
                          HttpClient client,
                          String subprotocol,
                          Listener listener,
                          TransportFactory transportFactory) {
        this.uri = requireNonNull(uri);
        this.clientExecutor = ((HttpClientFacade)client).theExecutor();
        this.subprotocol = requireNonNull(subprotocol);
        this.listener = requireNonNull(listener);
        // Why 6? 1 sendPing/sendPong + 1 sendText/sendBinary + 1 Close +
        // 2 automatic Ping replies + 1 automatic Close = 6 messages
        // Why 2 automatic Pong replies? One is being sent, but the byte buffer
        // has been set to null, another just has been added.
        this.transport = transportFactory.createTransport(new MessageQueue(6),
                new SignallingMessageConsumer());
    }

    // FIXME: add to action handling of errors -> signalError()

    @Override
    public CompletableFuture<WebSocket> sendText(CharSequence message,
                                                 boolean last) {
        Objects.requireNonNull(message);
        long id = 0;
        if (debug.on()) {
            id = sendCounter.incrementAndGet();
            debug.log("enter send text %s payload length=%s last=%s",
                      id, message.length(), last);
        }
        CompletableFuture<WebSocket> result;
        if (!setPendingTextOrBinary()) {
            result = failedFuture(new IllegalStateException("Send pending"));
        } else {
            result = transport.sendText(message, last, this,
                                        (r, e) -> clearPendingTextOrBinary());
        }
        if (debug.on()) {
            debug.log("exit send text %s returned %s", id, result);
        }

        return replaceNull(result);
    }

    @Override
    public CompletableFuture<WebSocket> sendBinary(ByteBuffer message,
                                                   boolean last) {
        Objects.requireNonNull(message);
        long id = 0;
        if (debug.on()) {
            id = sendCounter.incrementAndGet();
            debug.log("enter send binary %s payload=%s last=%s",
                      id, message, last);
        }
        CompletableFuture<WebSocket> result;
        if (!setPendingTextOrBinary()) {
            result = failedFuture(new IllegalStateException("Send pending"));
        } else {
            result = transport.sendBinary(message, last, this,
                                          (r, e) -> clearPendingTextOrBinary());
        }
        if (debug.on()) {
            debug.log("exit send binary %s returned %s", id, result);
        }
        return replaceNull(result);
    }

    private void clearPendingTextOrBinary() {
        pendingTextOrBinary.set(false);
    }

    private boolean setPendingTextOrBinary() {
        return pendingTextOrBinary.compareAndSet(false, true);
    }

    private CompletableFuture<WebSocket> replaceNull(
            CompletableFuture<WebSocket> cf)
    {
        if (cf == null) {
            return DONE;
        } else {
            return cf;
        }
    }

    @Override
    public CompletableFuture<WebSocket> sendPing(ByteBuffer message) {
        Objects.requireNonNull(message);
        long id = 0;
        if (debug.on()) {
            id = sendCounter.incrementAndGet();
            debug.log("enter send ping %s payload=%s", id, message);
        }
        CompletableFuture<WebSocket> result;
        if (!setPendingPingOrPong()) {
            result = failedFuture(new IllegalStateException("Send pending"));
        } else {
            result = transport.sendPing(message, this,
                                        (r, e) -> clearPendingPingOrPong());
        }
        if (debug.on()) {
            debug.log("exit send ping %s returned %s", id, result);
        }
        return replaceNull(result);
    }

    @Override
    public CompletableFuture<WebSocket> sendPong(ByteBuffer message) {
        Objects.requireNonNull(message);
        long id = 0;
        if (debug.on()) {
            id = sendCounter.incrementAndGet();
            debug.log("enter send pong %s payload=%s", id, message);
        }
        CompletableFuture<WebSocket> result;
        if (!setPendingPingOrPong()) {
            result = failedFuture(new IllegalStateException("Send pending"));
        } else {
            result =  transport.sendPong(message, this,
                                         (r, e) -> clearPendingPingOrPong());
        }
        if (debug.on()) {
            debug.log("exit send pong %s returned %s", id, result);
        }
        return replaceNull(result);
    }

    private boolean setPendingPingOrPong() {
        return pendingPingOrPong.compareAndSet(false, true);
    }

    private void clearPendingPingOrPong() {
        pendingPingOrPong.set(false);
    }

    @Override
    public CompletableFuture<WebSocket> sendClose(int statusCode,
                                                  String reason) {
        Objects.requireNonNull(reason);
        long id = 0;
        if (debug.on()) {
            id = sendCounter.incrementAndGet();
            debug.log("enter send close %s statusCode=%s reason.length=%s",
                      id, statusCode, reason.length());
        }
        CompletableFuture<WebSocket> result;
        // Close message is the only type of message whose validity is checked
        // in the corresponding send method. This is made in order to close the
        // output in place. Otherwise the number of Close messages in queue
        // would not be bounded.
        if (!isLegalToSendFromClient(statusCode)) {
            result = failedFuture(new IllegalArgumentException("statusCode"));
        } else if (!isLegalReason(reason)) {
            result = failedFuture(new IllegalArgumentException("reason"));
        } else if (!outputClosed.compareAndSet(false, true)){
            result = failedFuture(new IOException("Output closed"));
        } else {
            result = sendClose0(statusCode, reason);
        }
        if (debug.on()) {
            debug.log("exit send close %s returned %s", id, result);
        }
        return replaceNull(result);
    }

    private static boolean isLegalReason(String reason) {
        if (reason.length() > 123) { // quick check
            return false;
        }
        CharsetEncoder encoder = StandardCharsets.UTF_8.newEncoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer bytes;
        try {
            bytes = encoder.encode(CharBuffer.wrap(reason));
        } catch (CharacterCodingException ignored) {
            return false;
        }
        return bytes.remaining() <= 123;
    }

    /*
     * The implementation uses this method internally to send Close messages
     * with codes that are not allowed to be sent through the API.
     */
    private CompletableFuture<WebSocket> sendClose0(int statusCode,
                                                    String reason) {
        return transport.sendClose(statusCode, reason, this,
                                   (r, e) -> processCloseError(e));
    }

    private void processCloseError(Throwable e) {
        if (e == null) {
            debug.log("send close completed successfully");
        } else {
            debug.log("send close completed with error", e);
        }
        outputClosed.set(true);
        try {
            transport.closeOutput();
        } catch (IOException ignored) { }
    }

    @Override
    public void request(long n) {
        if (debug.on()) {
            debug.log("request %s", n);
        }
        if (demand.increase(n)) {
            receiveScheduler.runOrSchedule(clientExecutor);
        }
    }

    @Override
    public String getSubprotocol() {
        return subprotocol;
    }

    @Override
    public boolean isOutputClosed() {
        return outputClosed.get();
    }

    @Override
    public boolean isInputClosed() {
        return inputClosed;
    }

    @Override
    public void abort() {
        if (debug.on()) {
            debug.log("abort");
        }
        inputClosed = true;
        outputClosed.set(true);
        receiveScheduler.stop();
        close();
    }

    @Override
    public String toString() {
        return super.toString()
                + "[uri=" + uri
                + (!subprotocol.isEmpty() ? ", subprotocol=" + subprotocol : "")
                + "]";
    }

    /*
     * The assumptions about order is as follows:
     *
     *     - state is never changed more than twice inside the `run` method:
     *       x --(1)--> IDLE --(2)--> y (otherwise we're losing events, or
     *       overwriting parts of messages creating a mess since there's no
     *       queueing)
     *     - OPEN is always the first state
     *     - no messages are requested/delivered before onOpen is called (this
     *       is implemented by making WebSocket instance accessible first in
     *       onOpen)
     *     - after the state has been observed as CLOSE/ERROR, the scheduler
     *       is stopped
     */
    private class ReceiveTask extends SequentialScheduler.CompleteRestartableTask {

        // Transport only asked here and nowhere else because we must make sure
        // onOpen is invoked first and no messages become pending before onOpen
        // finishes

        @Override
        public void run() {
            if (debug.on()) {
                debug.log("enter receive task");
            }
            loop:
            while (!receiveScheduler.isStopped()) {
                State s = state.get();
                if (debug.on()) {
                    debug.log("receive state: %s", s);
                }
                try {
                    switch (s) {
                        case OPEN:
                            processOpen();
                            tryChangeState(OPEN, IDLE);
                            break;
                        case TEXT:
                            processText();
                            tryChangeState(TEXT, IDLE);
                            break;
                        case BINARY:
                            processBinary();
                            tryChangeState(BINARY, IDLE);
                            break;
                        case PING:
                            processPing();
                            tryChangeState(PING, IDLE);
                            break;
                        case PONG:
                            processPong();
                            tryChangeState(PONG, IDLE);
                            break;
                        case CLOSE:
                            processClose();
                            break loop;
                        case ERROR:
                            processError();
                            break loop;
                        case IDLE:
                            if (demand.tryDecrement()
                                    && tryChangeState(IDLE, WAITING)) {
                                transport.request(1);
                            }
                            break loop;
                        case WAITING:
                            // For debugging spurious signalling: when there was
                            // a signal, but apparently nothing has changed
                            break loop;
                        default:
                            throw new InternalError(String.valueOf(s));
                    }
                } catch (Throwable t) {
                    signalError(t);
                }
            }
            if (debug.on()) {
                debug.log("exit receive task");
            }
        }

        private void processError() throws IOException {
            if (debug.on()) {
                debug.log("processError");
            }
            transport.closeInput();
            receiveScheduler.stop();
            Throwable err = error.get();
            if (err instanceof FailWebSocketException) {
                int code1 = ((FailWebSocketException) err).getStatusCode();
                err = new ProtocolException().initCause(err);
                if (debug.on()) {
                    debug.log("failing %s with error=%s statusCode=%s",
                              WebSocketImpl.this, err, code1);
                }
                sendCloseSilently(code1);
            }
            long id = 0;
            if (debug.on()) {
                id = receiveCounter.incrementAndGet();
                debug.log("enter onError %s error=%s", id, err);
            }
            try {
                listener.onError(WebSocketImpl.this, err);
            } finally {
                if (debug.on()) {
                    debug.log("exit onError %s", id);
                }
            }
        }

        private void processClose() throws IOException {
            debug.log("processClose");
            transport.closeInput();
            receiveScheduler.stop();
            CompletionStage<?> cs = null; // when the listener is ready to close
            long id = 0;
            if (debug.on()) {
                id = receiveCounter.incrementAndGet();
                debug.log("enter onClose %s statusCode=%s reason.length=%s",
                          id, statusCode, reason.length());
            }
            try {
                cs = listener.onClose(WebSocketImpl.this, statusCode, reason);
            } finally {
                debug.log("exit onClose %s returned %s", id, cs);
            }
            if (cs == null) {
                cs = DONE;
            }
            int code;
            if (statusCode == NO_STATUS_CODE || statusCode == CLOSED_ABNORMALLY) {
                code = NORMAL_CLOSURE;
                debug.log("using statusCode %s instead of %s",
                          statusCode, code);

            } else {
                code = statusCode;
            }
            cs.whenComplete((r, e) -> {
                if (debug.on()) {
                    debug.log("CompletionStage returned by onClose completed result=%s error=%s",
                              r, e);
                }
                sendCloseSilently(code);
            });
        }

        private void processPong() {
            long id = 0;
            if (debug.on()) {
                id = receiveCounter.incrementAndGet();
                debug.log("enter onPong %s payload=%s",
                          id, binaryData);
            }
            CompletionStage<?> cs = null;
            try {
                cs = listener.onPong(WebSocketImpl.this, binaryData);
            } finally {
                if (debug.on()) {
                    debug.log("exit onPong %s returned %s", id, cs);
                }
            }
        }

        private void processPing() {
            if (debug.on()) {
                debug.log("processPing");
            }
            // A full copy of this (small) data is made. This way sending a
            // replying Pong could be done in parallel with the listener
            // handling this Ping.
            ByteBuffer slice = binaryData.slice();
            if (!outputClosed.get()) {
                ByteBuffer copy = ByteBuffer.allocate(binaryData.remaining())
                        .put(binaryData)
                        .flip();
                if (!trySwapAutomaticPong(copy)) {
                    // Non-exclusive send;
                    BiConsumer<WebSocketImpl, Throwable> reporter = (r, e) -> {
                        if (e != null) { // TODO: better error handing. What if already closed?
                            signalError(Utils.getCompletionCause(e));
                        }
                    };
                    transport.sendPong(WebSocketImpl.this::clearAutomaticPong,
                                       WebSocketImpl.this,
                                       reporter);
                }
            }
            long id = 0;
            if (debug.on()) {
                id = receiveCounter.incrementAndGet();
                debug.log("enter onPing %s payload=%s", id, slice);
            }
            CompletionStage<?> cs = null;
            try {
                cs = listener.onPing(WebSocketImpl.this, slice);
            } finally {
                if (debug.on()) {
                    debug.log("exit onPing %s returned %s", id, cs);
                }
            }
        }

        private void processBinary() {
            long id = 0;
            if (debug.on()) {
                id = receiveCounter.incrementAndGet();
                debug.log("enter onBinary %s payload=%s last=%s",
                          id, binaryData, last);
            }
            CompletionStage<?> cs = null;
            try {
                cs = listener.onBinary(WebSocketImpl.this, binaryData, last);
            } finally {
                if (debug.on()) {
                    debug.log("exit onBinary %s returned %s", id, cs);
                }
            }
        }

        private void processText() {
            long id = 0;
            if (debug.on()) {
                id = receiveCounter.incrementAndGet();
                debug.log("enter onText %s payload.length=%s last=%s",
                          id, text.length(), last);
            }
            CompletionStage<?> cs = null;
            try {
                cs = listener.onText(WebSocketImpl.this, text, last);
            } finally {
                if (debug.on()) {
                    debug.log("exit onText %s returned %s", id, cs);
                }
            }
        }

        private void processOpen() {
            long id = 0;
            if (debug.on()) {
                id = receiveCounter.incrementAndGet();
                debug.log("enter onOpen %s", id);
            }
            try {
                listener.onOpen(WebSocketImpl.this);
            } finally {
                if (debug.on()) {
                    debug.log("exit onOpen %s", id);
                }
            }
        }
    }

    private void sendCloseSilently(int statusCode) {
        sendClose0(statusCode, "").whenComplete((r, e) -> {
            if (e != null) {
                if (debug.on()) {
                    debug.log("automatic closure completed with error",
                              (Object) e);
                }
            }
        });
    }

    private ByteBuffer clearAutomaticPong() {
        ByteBuffer data;
        do {
            data = lastAutomaticPong.get();
            if (data == null) {
                // This method must never be called unless a message that is
                // using it has been added previously
                throw new InternalError();
            }
        } while (!lastAutomaticPong.compareAndSet(data, null));
        return data;
    }

    // bound pings
    private boolean trySwapAutomaticPong(ByteBuffer copy) {
        ByteBuffer message;
        boolean swapped;
        while (true) {
            message = lastAutomaticPong.get();
            if (message == null) {
                if (!lastAutomaticPong.compareAndSet(null, copy)) {
                    // It's only this method that can change null to ByteBuffer,
                    // and this method is invoked at most by one thread at a
                    // time. Thus no failure in the atomic operation above is
                    // expected.
                    throw new InternalError();
                }
                swapped = false;
                break;
            } else if (lastAutomaticPong.compareAndSet(message, copy)) {
                swapped = true;
                break;
            }
        }
        if (debug.on()) {
            debug.log("swapped automatic pong from %s to %s",
                      message, copy);
        }
        return swapped;
    }

    private void signalOpen() {
        debug.log("signalOpen");
        receiveScheduler.runOrSchedule(clientExecutor);
    }

    private void signalError(Throwable error) {
        if (debug.on()) {
            debug.log("signalError %s", (Object) error);
        }
        inputClosed = true;
        outputClosed.set(true);
        if (!this.error.compareAndSet(null, error) || !trySetState(ERROR)) {
            if (debug.on()) {
                debug.log("signalError", error);
            }
            Log.logError(error);
        } else {
            close();
        }
    }

    private void close() {
        if (debug.on()) {
            debug.log("close");
        }
        Throwable first = null;
        try {
            transport.closeInput();
        } catch (Throwable t1) {
            first = t1;
        } finally {
            Throwable second = null;
            try {
                transport.closeOutput();
            } catch (Throwable t2) {
                second = t2;
            } finally {
                Throwable e = null;
                if (first != null && second != null) {
                    first.addSuppressed(second);
                    e = first;
                } else if (first != null) {
                    e = first;
                } else if (second != null) {
                    e = second;
                }
                if (e != null) {
                    if (debug.on()) {
                        debug.log("exception in close", e);
                    }
                }
            }
        }
    }

    private void signalClose(int statusCode, String reason) {
        // FIXME: make sure no race reason & close are not intermixed
        inputClosed = true;
        this.statusCode = statusCode;
        this.reason = reason;
        boolean managed = trySetState(CLOSE);
        if (debug.on()) {
            debug.log("signalClose statusCode=%s reason.length=%s: %s",
                      statusCode, reason.length(), managed);
        }
        if (managed) {
            try {
                transport.closeInput();
            } catch (Throwable t) {
                if (debug.on()) {
                    debug.log("exception closing input", (Object) t);
                }
            }
        }
    }

    private class SignallingMessageConsumer implements MessageStreamConsumer {

        @Override
        public void onText(CharSequence data, boolean last) {
            transport.acknowledgeReception();
            text = data;
            WebSocketImpl.this.last = last;
            tryChangeState(WAITING, TEXT);
        }

        @Override
        public void onBinary(ByteBuffer data, boolean last) {
            transport.acknowledgeReception();
            binaryData = data;
            WebSocketImpl.this.last = last;
            tryChangeState(WAITING, BINARY);
        }

        @Override
        public void onPing(ByteBuffer data) {
            transport.acknowledgeReception();
            binaryData = data;
            tryChangeState(WAITING, PING);
        }

        @Override
        public void onPong(ByteBuffer data) {
            transport.acknowledgeReception();
            binaryData = data;
            tryChangeState(WAITING, PONG);
        }

        @Override
        public void onClose(int statusCode, CharSequence reason) {
            transport.acknowledgeReception();
            signalClose(statusCode, reason.toString());
        }

        @Override
        public void onComplete() {
            transport.acknowledgeReception();
            signalClose(CLOSED_ABNORMALLY, "");
        }

        @Override
        public void onError(Throwable error) {
            signalError(error);
        }
    }

    private boolean trySetState(State newState) {
        State currentState;
        boolean success = false;
        while (true) {
            currentState = state.get();
            if (currentState == ERROR || currentState == CLOSE) {
                break;
            } else if (state.compareAndSet(currentState, newState)) {
                receiveScheduler.runOrSchedule(clientExecutor);
                success = true;
                break;
            }
        }
        if (debug.on()) {
            debug.log("set state %s (previous %s) %s",
                      newState, currentState, success);
        }
        return success;
    }

    private boolean tryChangeState(State expectedState, State newState) {
        State witness = state.compareAndExchange(expectedState, newState);
        boolean success = false;
        if (witness == expectedState) {
            receiveScheduler.runOrSchedule(clientExecutor);
            success = true;
        } else if (witness != ERROR && witness != CLOSE) {
            // This should be the only reason for inability to change the state
            // from IDLE to WAITING: the state has changed to terminal
            throw new InternalError();
        }
        if (debug.on()) {
            debug.log("change state from %s to %s %s",
                      expectedState, newState, success);
        }
        return success;
    }

    /* Exposed for testing purposes */
    protected Transport transport() {
        return transport;
    }
}
