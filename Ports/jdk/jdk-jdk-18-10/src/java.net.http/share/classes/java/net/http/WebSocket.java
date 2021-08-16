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

package java.net.http;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A WebSocket Client.
 *
 * <p> {@code WebSocket} instances are created through {@link WebSocket.Builder}.
 *
 * <p> WebSocket has an input and an output side. These sides are independent
 * from each other. A side can either be open or closed. Once closed, the side
 * remains closed. WebSocket messages are sent through a {@code WebSocket} and
 * received through a {@code WebSocket.Listener} associated with it. Messages
 * can be sent until the WebSocket's output is closed, and received until the
 * WebSocket's input is closed.
 *
 * <p> A send method is any of the {@code sendText}, {@code sendBinary},
 * {@code sendPing}, {@code sendPong} and {@code sendClose} methods of
 * {@code WebSocket}. A send method initiates a send operation and returns a
 * {@code CompletableFuture} which completes once the operation has completed.
 * If the {@code CompletableFuture} completes normally the operation is
 * considered succeeded. If the {@code CompletableFuture} completes
 * exceptionally, the operation is considered failed. An operation that has been
 * initiated but not yet completed is considered pending.
 *
 * <p> A receive method is any of the {@code onText}, {@code onBinary},
 * {@code onPing}, {@code onPong} and {@code onClose} methods of
 * {@code Listener}. WebSocket initiates a receive operation by invoking a
 * receive method on the listener. The listener then must return a
 * {@code CompletionStage} which completes once the operation has completed.
 *
 * <p> To control receiving of messages, a WebSocket maintains an
 * <a id="counter">internal counter</a>. This counter's value is a number of
 * times the WebSocket has yet to invoke a receive method. While this counter is
 * zero the WebSocket does not invoke receive methods. The counter is
 * incremented by {@code n} when {@code request(n)} is called. The counter is
 * decremented by one when the WebSocket invokes a receive method.
 * {@code onOpen} and {@code onError} are not receive methods. WebSocket invokes
 * {@code onOpen} prior to any other methods on the listener. WebSocket invokes
 * {@code onOpen} at most once. WebSocket may invoke {@code onError} at any
 * given time. If the WebSocket invokes {@code onError} or {@code onClose}, then
 * no further listener's methods will be invoked, no matter the value of the
 * counter. For a newly built WebSocket the counter is zero.
 *
 * <p> Unless otherwise stated, {@code null} arguments will cause methods
 * of {@code WebSocket} to throw {@code NullPointerException}, similarly,
 * {@code WebSocket} will not pass {@code null} arguments to methods of
 * {@code Listener}. The state of a WebSocket is not changed by the invocations
 * that throw or return a {@code CompletableFuture} that completes with one of
 * the {@code NullPointerException}, {@code IllegalArgumentException},
 * {@code IllegalStateException} exceptions.
 *
 * <p> {@code WebSocket} handles received Ping and Close messages automatically
 * (as per the WebSocket Protocol) by replying with Pong and Close messages. If
 * the listener receives Ping or Close messages, no mandatory actions from the
 * listener are required.
 *
 * @apiNote The relationship between a WebSocket and the associated Listener is
 * analogous to that of a Subscription and the associated Subscriber of type
 * {@link java.util.concurrent.Flow}.
 *
 * @since 11
 */
public interface WebSocket {

    /**
     * The WebSocket Close message status code (<code>{@value}</code>),
     * indicating normal closure, meaning that the purpose for which the
     * connection was established has been fulfilled.
     *
     * @see #sendClose(int, String)
     * @see Listener#onClose(WebSocket, int, String)
     */
    int NORMAL_CLOSURE = 1000;

    /**
     * A builder of {@linkplain WebSocket WebSocket Clients}.
     *
     * <p> Builders are created by invoking
     * {@link HttpClient#newWebSocketBuilder HttpClient.newWebSocketBuilder}.
     * The intermediate (setter-like) methods change the state of the builder
     * and return the same builder they have been invoked on. If an intermediate
     * method is not invoked, an appropriate default value (or behavior) will be
     * assumed. A {@code Builder} is not safe for use by multiple threads
     * without external synchronization.
     *
     * @since 11
     */
    interface Builder {

        /**
         * Adds the given name-value pair to the list of additional HTTP headers
         * sent during the opening handshake.
         *
         * <p> Headers defined in the
         * <a href="https://tools.ietf.org/html/rfc6455#section-11.3">WebSocket
         * Protocol</a> are illegal. If this method is not invoked, no
         * additional HTTP headers will be sent.
         *
         * @param name
         *         the header name
         * @param value
         *         the header value
         *
         * @return this builder
         */
        Builder header(String name, String value);

        /**
         * Sets a timeout for establishing a WebSocket connection.
         *
         * <p> If the connection is not established within the specified
         * duration then building of the {@code WebSocket} will fail with
         * {@link HttpTimeoutException}. If this method is not invoked then the
         * infinite timeout is assumed.
         *
         * @param timeout
         *         the timeout, non-{@linkplain Duration#isNegative() negative},
         *         non-{@linkplain Duration#ZERO ZERO}
         *
         * @return this builder
         */
        Builder connectTimeout(Duration timeout);

        /**
         * Sets a request for the given subprotocols.
         *
         * <p> After the {@code WebSocket} has been built, the actual
         * subprotocol can be queried through
         * {@link WebSocket#getSubprotocol WebSocket.getSubprotocol()}.
         *
         * <p> Subprotocols are specified in the order of preference. The most
         * preferred subprotocol is specified first. If there are any additional
         * subprotocols they are enumerated from the most preferred to the least
         * preferred.
         *
         * <p> Subprotocols not conforming to the syntax of subprotocol
         * identifiers are illegal. If this method is not invoked then no
         * subprotocols will be requested.
         *
         * @param mostPreferred
         *         the most preferred subprotocol
         * @param lesserPreferred
         *         the lesser preferred subprotocols
         *
         * @return this builder
         */
        Builder subprotocols(String mostPreferred, String... lesserPreferred);

        /**
         * Builds a {@link WebSocket} connected to the given {@code URI} and
         * associated with the given {@code Listener}.
         *
         * <p> Returns a {@code CompletableFuture} which will either complete
         * normally with the resulting {@code WebSocket} or complete
         * exceptionally with one of the following errors:
         * <ul>
         * <li> {@link IOException} -
         *          if an I/O error occurs
         * <li> {@link WebSocketHandshakeException} -
         *          if the opening handshake fails
         * <li> {@link HttpTimeoutException} -
         *          if the opening handshake does not complete within
         *          the timeout
         * <li> {@link InterruptedException} -
         *          if the operation is interrupted
         * <li> {@link SecurityException} -
         *          if a security manager has been installed and it denies
         *          {@link java.net.URLPermission access} to {@code uri}.
         *          <a href="HttpClient.html#securitychecks">Security checks</a>
         *          contains more information relating to the security context
         *          in which the listener is invoked.
         * <li> {@link IllegalArgumentException} -
         *          if any of the arguments of this builder's methods are
         *          illegal
         * </ul>
         *
         * @param uri
         *         the WebSocket URI
         * @param listener
         *         the listener
         *
         * @return a {@code CompletableFuture} with the {@code WebSocket}
         */
        CompletableFuture<WebSocket> buildAsync(URI uri, Listener listener);
    }

    /**
     * The receiving interface of {@code WebSocket}.
     *
     * <p> A {@code WebSocket} invokes methods of the associated listener
     * passing itself as an argument. These methods are invoked in a thread-safe
     * manner, such that the next invocation may start only after the previous
     * one has finished.
     *
     * <p> When data has been received, the {@code WebSocket} invokes a receive
     * method. Methods {@code onText}, {@code onBinary}, {@code onPing} and
     * {@code onPong} must return a {@code CompletionStage} that completes once
     * the message has been received by the listener. If a listener's method
     * returns {@code null} rather than a {@code CompletionStage},
     * {@code WebSocket} will behave as if the listener returned a
     * {@code CompletionStage} that is already completed normally.
     *
     * <p> An {@code IOException} raised in {@code WebSocket} will result in an
     * invocation of {@code onError} with that exception (if the input is not
     * closed). Unless otherwise stated if the listener's method throws an
     * exception or a {@code CompletionStage} returned from a method completes
     * exceptionally, the WebSocket will invoke {@code onError} with this
     * exception.
     *
     * @apiNote The strict sequential order of invocations from
     * {@code WebSocket} to {@code Listener} means, in particular, that the
     * {@code Listener}'s methods are treated as non-reentrant. This means that
     * {@code Listener} implementations do not need to be concerned with
     * possible recursion or the order in which they invoke
     * {@code WebSocket.request} in relation to their processing logic.
     *
     * <p> Careful attention may be required if a listener is associated
     * with more than a single {@code WebSocket}. In this case invocations
     * related to different instances of {@code WebSocket} may not be ordered
     * and may even happen concurrently.
     *
     * <p> {@code CompletionStage}s returned from the receive methods have
     * nothing to do with the
     * <a href="WebSocket.html#counter">counter of invocations</a>.
     * Namely, a {@code CompletionStage} does not have to be completed in order
     * to receive more invocations of the listener's methods.
     * Here is an example of a listener that requests invocations, one at a
     * time, until a complete message has been accumulated, then processes
     * the result, and completes the {@code CompletionStage}:
     * <pre>{@code     WebSocket.Listener listener = new WebSocket.Listener() {
     *
     *        List<CharSequence> parts = new ArrayList<>();
     *        CompletableFuture<?> accumulatedMessage = new CompletableFuture<>();
     *
     *        public CompletionStage<?> onText(WebSocket webSocket,
     *                                         CharSequence message,
     *                                         boolean last) {
     *            parts.add(message);
     *            webSocket.request(1);
     *            if (last) {
     *                processWholeText(parts);
     *                parts = new ArrayList<>();
     *                accumulatedMessage.complete(null);
     *                CompletionStage<?> cf = accumulatedMessage;
     *                accumulatedMessage = new CompletableFuture<>();
     *                return cf;
     *            }
     *            return accumulatedMessage;
     *        }
     *    ...
     *    } } </pre>
     *
     * @since 11
     */
    interface Listener {

        /**
         * A {@code WebSocket} has been connected.
         *
         * <p> This is the initial invocation and it is made once. It is
         * typically used to make a request for more invocations.
         *
         * @implSpec The default implementation is equivalent to:
         * <pre>{@code     webSocket.request(1); }</pre>
         *
         * @param webSocket
         *         the WebSocket that has been connected
         */
        default void onOpen(WebSocket webSocket) { webSocket.request(1); }

        /**
         * A textual data has been received.
         *
         * <p> Return a {@code CompletionStage} which will be used by the
         * {@code WebSocket} as an indication it may reclaim the
         * {@code CharSequence}. Do not access the {@code CharSequence} after
         * this {@code CompletionStage} has completed.
         *
         * @implSpec The default implementation is equivalent to:
         * <pre>{@code     webSocket.request(1);
         *    return null; }</pre>
         *
         * @implNote The {@code data} is always a legal UTF-16 sequence.
         *
         * @param webSocket
         *         the WebSocket on which the data has been received
         * @param data
         *         the data
         * @param last
         *         whether this invocation completes the message
         *
         * @return a {@code CompletionStage} which completes when the
         * {@code CharSequence} may be reclaimed; or {@code null} if it may be
         * reclaimed immediately
         */
        default CompletionStage<?> onText(WebSocket webSocket,
                                          CharSequence data,
                                          boolean last) {
            webSocket.request(1);
            return null;
        }

        /**
         * A binary data has been received.
         *
         * <p> This data is located in bytes from the buffer's position to its
         * limit.
         *
         * <p> Return a {@code CompletionStage} which will be used by the
         * {@code WebSocket} as an indication it may reclaim the
         * {@code ByteBuffer}. Do not access the {@code ByteBuffer} after
         * this {@code CompletionStage} has completed.
         *
         * @implSpec The default implementation is equivalent to:
         * <pre>{@code     webSocket.request(1);
         *    return null; }</pre>
         *
         * @param webSocket
         *         the WebSocket on which the data has been received
         * @param data
         *         the data
         * @param last
         *         whether this invocation completes the message
         *
         * @return a {@code CompletionStage} which completes when the
         * {@code ByteBuffer} may be reclaimed; or {@code null} if it may be
         * reclaimed immediately
         */
        default CompletionStage<?> onBinary(WebSocket webSocket,
                                            ByteBuffer data,
                                            boolean last) {
            webSocket.request(1);
            return null;
        }

        /**
         * A Ping message has been received.
         *
         * <p> As guaranteed by the WebSocket Protocol, the message consists of
         * not more than {@code 125} bytes. These bytes are located from the
         * buffer's position to its limit.
         *
         * <p> Given that the WebSocket implementation will automatically send a
         * reciprocal pong when a ping is received, it is rarely required to
         * send a pong message explicitly when a ping is received.
         *
         * <p> Return a {@code CompletionStage} which will be used by the
         * {@code WebSocket} as a signal it may reclaim the
         * {@code ByteBuffer}. Do not access the {@code ByteBuffer} after
         * this {@code CompletionStage} has completed.
         *
         * @implSpec The default implementation is equivalent to:
         * <pre>{@code     webSocket.request(1);
         *    return null; }</pre>
         *
         * @param webSocket
         *         the WebSocket on which the message has been received
         * @param message
         *         the message
         *
         * @return a {@code CompletionStage} which completes when the
         * {@code ByteBuffer} may be reclaimed; or {@code null} if it may be
         * reclaimed immediately
         */
        default CompletionStage<?> onPing(WebSocket webSocket,
                                          ByteBuffer message) {
            webSocket.request(1);
            return null;
        }

        /**
         * A Pong message has been received.
         *
         * <p> As guaranteed by the WebSocket Protocol, the message consists of
         * not more than {@code 125} bytes. These bytes are located from the
         * buffer's position to its limit.
         *
         * <p> Return a {@code CompletionStage} which will be used by the
         * {@code WebSocket} as a signal it may reclaim the
         * {@code ByteBuffer}. Do not access the {@code ByteBuffer} after
         * this {@code CompletionStage} has completed.
         *
         * @implSpec The default implementation is equivalent to:
         * <pre>{@code     webSocket.request(1);
         *    return null; }</pre>
         *
         * @param webSocket
         *         the WebSocket on which the message has been received
         * @param message
         *         the message
         *
         * @return a {@code CompletionStage} which completes when the
         * {@code ByteBuffer} may be reclaimed; or {@code null} if it may be
         * reclaimed immediately
         */
        default CompletionStage<?> onPong(WebSocket webSocket,
                                          ByteBuffer message) {
            webSocket.request(1);
            return null;
        }

        /**
         * Receives a Close message indicating the WebSocket's input has been
         * closed.
         *
         * <p> This is the last invocation from the specified {@code WebSocket}.
         * By the time this invocation begins the WebSocket's input will have
         * been closed.
         *
         * <p> A Close message consists of a status code and a reason for
         * closing. The status code is an integer from the range
         * {@code 1000 <= code <= 65535}. The {@code reason} is a string which
         * has a UTF-8 representation not longer than {@code 123} bytes.
         *
         * <p> If the WebSocket's output is not already closed, the
         * {@code CompletionStage} returned by this method will be used as an
         * indication that the WebSocket's output may be closed. The WebSocket
         * will close its output at the earliest of completion of the returned
         * {@code CompletionStage} or invoking either of the {@code sendClose}
         * or {@code abort} methods.
         *
         * @apiNote Returning a {@code CompletionStage} that never completes,
         * effectively disables the reciprocating closure of the output.
         *
         * <p> To specify a custom closure code or reason code the
         * {@code sendClose} method may be invoked from inside the
         * {@code onClose} invocation:
         * <pre>{@code     public CompletionStage<?> onClose(WebSocket webSocket,
         *                                      int statusCode,
         *                                      String reason) {
         *        webSocket.sendClose(CUSTOM_STATUS_CODE, CUSTOM_REASON);
         *        return new CompletableFuture<Void>();
         *    } } </pre>
         *
         * @implSpec The default implementation of this method returns
         * {@code null}, indicating that the output should be closed
         * immediately.
         *
         * @param webSocket
         *         the WebSocket on which the message has been received
         * @param statusCode
         *         the status code
         * @param reason
         *         the reason
         *
         * @return a {@code CompletionStage} which completes when the
         * {@code WebSocket} may be closed; or {@code null} if it may be
         * closed immediately
         */
        default CompletionStage<?> onClose(WebSocket webSocket,
                                           int statusCode,
                                           String reason) {
            return null;
        }

        /**
         * An error has occurred.
         *
         * <p> This is the last invocation from the specified WebSocket. By the
         * time this invocation begins both the WebSocket's input and output
         * will have been closed. A WebSocket may invoke this method on the
         * associated listener at any time after it has invoked {@code onOpen},
         * regardless of whether or not any invocations have been requested from
         * the WebSocket.
         *
         * <p> If an exception is thrown from this method, resulting behavior is
         * undefined.
         *
         * @param webSocket
         *         the WebSocket on which the error has occurred
         * @param error
         *         the error
         */
        default void onError(WebSocket webSocket, Throwable error) { }
    }

    /**
     * Sends textual data with characters from the given character sequence.
     *
     * <p> The character sequence must not be modified until the
     * {@code CompletableFuture} returned from this method has completed.
     *
     * <p> A {@code CompletableFuture} returned from this method can
     * complete exceptionally with:
     * <ul>
     * <li> {@link IllegalStateException} -
     *          if there is a pending text or binary send operation
     *          or if the previous binary data does not complete the message
     * <li> {@link IOException} -
     *          if an I/O error occurs, or if the output is closed
     * </ul>
     *
     * @implNote If {@code data} is a malformed UTF-16 sequence, the operation
     * will fail with {@code IOException}.
     *
     * @param data
     *         the data
     * @param last
     *         {@code true} if this invocation completes the message,
     *         {@code false} otherwise
     *
     * @return a {@code CompletableFuture} that completes, with this WebSocket,
     * when the data has been sent
     */
    CompletableFuture<WebSocket> sendText(CharSequence data, boolean last);

    /**
     * Sends binary data with bytes from the given buffer.
     *
     * <p> The data is located in bytes from the buffer's position to its limit.
     * Upon normal completion of a {@code CompletableFuture} returned from this
     * method the buffer will have no remaining bytes. The buffer must not be
     * accessed until after that.
     *
     * <p> The {@code CompletableFuture} returned from this method can
     * complete exceptionally with:
     * <ul>
     * <li> {@link IllegalStateException} -
     *          if there is a pending text or binary send operation
     *          or if the previous textual data does not complete the message
     * <li> {@link IOException} -
     *          if an I/O error occurs, or if the output is closed
     * </ul>
     *
     * @param data
     *         the data
     * @param last
     *         {@code true} if this invocation completes the message,
     *         {@code false} otherwise
     *
     * @return a {@code CompletableFuture} that completes, with this WebSocket,
     * when the data has been sent
     */
    CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last);

    /**
     * Sends a Ping message with bytes from the given buffer.
     *
     * <p> The message consists of not more than {@code 125} bytes from the
     * buffer's position to its limit. Upon normal completion of a
     * {@code CompletableFuture} returned from this method the buffer will
     * have no remaining bytes. The buffer must not be accessed until after that.
     *
     * <p> The {@code CompletableFuture} returned from this method can
     * complete exceptionally with:
     * <ul>
     * <li> {@link IllegalStateException} -
     *          if there is a pending ping or pong send operation
     * <li> {@link IllegalArgumentException} -
     *          if the message is too long
     * <li> {@link IOException} -
     *          if an I/O error occurs, or if the output is closed
     * </ul>
     *
     * @param message
     *         the message
     *
     * @return a {@code CompletableFuture} that completes, with this WebSocket,
     * when the Ping message has been sent
     */
    CompletableFuture<WebSocket> sendPing(ByteBuffer message);

    /**
     * Sends a Pong message with bytes from the given buffer.
     *
     * <p> The message consists of not more than {@code 125} bytes from the
     * buffer's position to its limit. Upon normal completion of a
     * {@code CompletableFuture} returned from this method the buffer will have
     * no remaining bytes. The buffer must not be accessed until after that.
     *
     * <p> Given that the WebSocket implementation will automatically send a
     * reciprocal pong when a ping is received, it is rarely required to send a
     * pong message explicitly.
     *
     * <p> The {@code CompletableFuture} returned from this method can
     * complete exceptionally with:
     * <ul>
     * <li> {@link IllegalStateException} -
     *          if there is a pending ping or pong send operation
     * <li> {@link IllegalArgumentException} -
     *          if the message is too long
     * <li> {@link IOException} -
     *          if an I/O error occurs, or if the output is closed
     * </ul>
     *
     * @param message
     *         the message
     *
     * @return a {@code CompletableFuture} that completes, with this WebSocket,
     * when the Pong message has been sent
     */
    CompletableFuture<WebSocket> sendPong(ByteBuffer message);

    /**
     * Initiates an orderly closure of this WebSocket's output by
     * sending a Close message with the given status code and the reason.
     *
     * <p> The {@code statusCode} is an integer from the range
     * {@code 1000 <= code <= 4999}. Status codes {@code 1002}, {@code 1003},
     * {@code 1006}, {@code 1007}, {@code 1009}, {@code 1010}, {@code 1012},
     * {@code 1013} and {@code 1015} are illegal. Behaviour in respect to other
     * status codes is implementation-specific. A legal {@code reason} is a
     * string that has a UTF-8 representation not longer than {@code 123} bytes.
     *
     * <p> A {@code CompletableFuture} returned from this method can
     * complete exceptionally with:
     * <ul>
     * <li> {@link IllegalArgumentException} -
     *           if {@code statusCode} is illegal, or
     *           if {@code reason} is illegal
     * <li> {@link IOException} -
     *           if an I/O error occurs, or if the output is closed
     * </ul>
     *
     * <p> Unless the {@code CompletableFuture} returned from this method
     * completes with {@code IllegalArgumentException}, or the method throws
     * {@code NullPointerException}, the output will be closed.
     *
     * <p> If not already closed, the input remains open until a Close message
     * {@linkplain Listener#onClose(WebSocket, int, String) received}, or
     * {@code abort} is invoked, or an
     * {@linkplain Listener#onError(WebSocket, Throwable) error} occurs.
     *
     * @apiNote Use the provided integer constant {@link #NORMAL_CLOSURE} as a
     * status code and an empty string as a reason in a typical case:
     * <pre>{@code     CompletableFuture<WebSocket> webSocket = ...
     *    webSocket.thenCompose(ws -> ws.sendText("Hello, ", false))
     *             .thenCompose(ws -> ws.sendText("world!", true))
     *             .thenCompose(ws -> ws.sendClose(WebSocket.NORMAL_CLOSURE, ""))
     *             .join(); }</pre>
     *
     * The {@code sendClose} method does not close this WebSocket's input. It
     * merely closes this WebSocket's output by sending a Close message. To
     * enforce closing the input, invoke the {@code abort} method. Here is an
     * example of an application that sends a Close message, and then starts a
     * timer. Once no data has been received within the specified timeout, the
     * timer goes off and the alarm aborts {@code WebSocket}:
     * <pre>{@code     MyAlarm alarm = new MyAlarm(webSocket::abort);
     *    WebSocket.Listener listener = new WebSocket.Listener() {
     *
     *        public CompletionStage<?> onText(WebSocket webSocket,
     *                                         CharSequence data,
     *                                         boolean last) {
     *            alarm.snooze();
     *            ...
     *        }
     *        ...
     *    };
     *    ...
     *    Runnable startTimer = () -> {
     *        MyTimer idleTimer = new MyTimer();
     *        idleTimer.add(alarm, 30, TimeUnit.SECONDS);
     *    };
     *    webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "ok").thenRun(startTimer);
     * } </pre>
     *
     * @param statusCode
     *         the status code
     * @param reason
     *         the reason
     *
     * @return a {@code CompletableFuture} that completes, with this WebSocket,
     * when the Close message has been sent
     */
    CompletableFuture<WebSocket> sendClose(int statusCode, String reason);

    /**
     * Increments the counter of invocations of receive methods.
     *
     * <p> This WebSocket will invoke {@code onText}, {@code onBinary},
     * {@code onPing}, {@code onPong} or {@code onClose} methods on the
     * associated listener (i.e. receive methods) up to {@code n} more times.
     *
     * @apiNote The parameter of this method is the number of invocations being
     * requested from this WebSocket to the associated listener, not the number
     * of messages. Sometimes a message may be delivered to the listener in a
     * single invocation, but not always. For example, Ping, Pong and Close
     * messages are delivered in a single invocation of {@code onPing},
     * {@code onPong} and {@code onClose} methods respectively. However, whether
     * or not Text and Binary messages are delivered in a single invocation of
     * {@code onText} and {@code onBinary} methods depends on the boolean
     * argument ({@code last}) of these methods. If {@code last} is
     * {@code false}, then there is more to a message than has been delivered to
     * the invocation.
     *
     * <p> Here is an example of a listener that requests invocations, one at a
     * time, until a complete message has been accumulated, and then processes
     * the result:
     * <pre>{@code     WebSocket.Listener listener = new WebSocket.Listener() {
     *
     *        StringBuilder text = new StringBuilder();
     *
     *        public CompletionStage<?> onText(WebSocket webSocket,
     *                                         CharSequence message,
     *                                         boolean last) {
     *            text.append(message);
     *            if (last) {
     *                processCompleteTextMessage(text);
     *                text = new StringBuilder();
     *            }
     *            webSocket.request(1);
     *            return null;
     *        }
     *    ...
     *    } } </pre>
     *
     * @param n
     *         the number of invocations
     *
     * @throws IllegalArgumentException
     *         if {@code n <= 0}
     */
    void request(long n);

    /**
     * Returns the subprotocol used by this WebSocket.
     *
     * @return the subprotocol, or an empty string if there's no subprotocol
     */
    String getSubprotocol();

    /**
     * Tells whether this WebSocket's output is closed.
     *
     * <p> If this method returns {@code true}, subsequent invocations will also
     * return {@code true}.
     *
     * @return {@code true} if closed, {@code false} otherwise
     */
    boolean isOutputClosed();

    /**
     * Tells whether this WebSocket's input is closed.
     *
     * <p> If this method returns {@code true}, subsequent invocations will also
     * return {@code true}.
     *
     * @return {@code true} if closed, {@code false} otherwise
     */
    boolean isInputClosed();

    /**
     * Closes this WebSocket's input and output abruptly.
     *
     * <p> When this method returns both the input and the output will have been
     * closed. Any pending send operations will fail with {@code IOException}.
     * Subsequent invocations of {@code abort} will have no effect.
     */
    void abort();
}
