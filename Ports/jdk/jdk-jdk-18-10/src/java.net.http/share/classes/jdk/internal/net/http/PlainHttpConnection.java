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

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import jdk.internal.net.http.common.FlowTube;
import jdk.internal.net.http.common.Log;
import jdk.internal.net.http.common.MinimalFuture;
import jdk.internal.net.http.common.Utils;

/**
 * Plain raw TCP connection direct to destination.
 * The connection operates in asynchronous non-blocking mode.
 * All reads and writes are done non-blocking.
 */
class PlainHttpConnection extends HttpConnection {

    private final Object reading = new Object();
    protected final SocketChannel chan;
    private final SocketTube tube; // need SocketTube to call signalClosed().
    private final PlainHttpPublisher writePublisher = new PlainHttpPublisher(reading);
    private volatile boolean connected;
    private boolean closed;
    private volatile ConnectTimerEvent connectTimerEvent;  // may be null
    private volatile int unsuccessfulAttempts;

    // Indicates whether a connection attempt has succeeded or should be retried.
    // If the attempt failed, and shouldn't be retried, there will be an exception
    // instead.
    private enum ConnectState { SUCCESS, RETRY }


    /**
     * Returns a ConnectTimerEvent iff there is a connect timeout duration,
     * otherwise null.
     */
    private ConnectTimerEvent newConnectTimer(Exchange<?> exchange,
                                              CompletableFuture<?> cf) {
        Duration duration = exchange.remainingConnectTimeout().orElse(null);
        if (duration != null) {
            ConnectTimerEvent cte = new ConnectTimerEvent(duration, exchange, cf);
            return cte;
        }
        return null;
    }

    final class ConnectTimerEvent extends TimeoutEvent {
        private final CompletableFuture<?> cf;
        private final Exchange<?> exchange;

        ConnectTimerEvent(Duration duration,
                          Exchange<?> exchange,
                          CompletableFuture<?> cf) {
            super(duration);
            this.exchange = exchange;
            this.cf = cf;
        }

        @Override
        public void handle() {
            if (debug.on()) {
                debug.log("HTTP connect timed out");
            }
            ConnectException ce = new ConnectException("HTTP connect timed out");
            exchange.multi.cancel(ce);
            client().theExecutor().execute(() -> cf.completeExceptionally(ce));
        }

        @Override
        public String toString() {
            return "ConnectTimerEvent, " + super.toString();
        }
    }

    final class ConnectEvent extends AsyncEvent {
        private final CompletableFuture<ConnectState> cf;
        private final Exchange<?> exchange;

        ConnectEvent(CompletableFuture<ConnectState> cf, Exchange<?> exchange) {
            this.cf = cf;
            this.exchange = exchange;
        }

        @Override
        public SelectableChannel channel() {
            return chan;
        }

        @Override
        public int interestOps() {
            return SelectionKey.OP_CONNECT;
        }

        @Override
        public void handle() {
            try {
                assert !connected : "Already connected";
                assert !chan.isBlocking() : "Unexpected blocking channel";
                if (debug.on())
                    debug.log("ConnectEvent: finishing connect");
                boolean finished = chan.finishConnect();
                if (debug.on())
                    debug.log("ConnectEvent: connect finished: %s, cancelled: %s, Local addr: %s",
                              finished, exchange.multi.requestCancelled(), chan.getLocalAddress());
                assert finished || exchange.multi.requestCancelled() : "Expected channel to be connected";
                // complete async since the event runs on the SelectorManager thread
                cf.completeAsync(() -> ConnectState.SUCCESS, client().theExecutor());
            } catch (Throwable e) {
                if (canRetryConnect(e)) {
                    unsuccessfulAttempts++;
                    cf.completeAsync(() -> ConnectState.RETRY, client().theExecutor());
                    return;
                }
                Throwable t = Utils.toConnectException(e);
                client().theExecutor().execute( () -> cf.completeExceptionally(t));
                close();
            }
        }

        @Override
        public void abort(IOException ioe) {
            client().theExecutor().execute( () -> cf.completeExceptionally(ioe));
            close();
        }
    }

    @SuppressWarnings("removal")
    @Override
    public CompletableFuture<Void> connectAsync(Exchange<?> exchange) {
        CompletableFuture<ConnectState> cf = new MinimalFuture<>();
        try {
            assert !connected : "Already connected";
            assert !chan.isBlocking() : "Unexpected blocking channel";
            boolean finished;

            if (connectTimerEvent == null) {
                connectTimerEvent = newConnectTimer(exchange, cf);
                if (connectTimerEvent != null) {
                    if (debug.on())
                        debug.log("registering connect timer: " + connectTimerEvent);
                    client().registerTimer(connectTimerEvent);
                }
            }

            PrivilegedExceptionAction<Boolean> pa =
                    () -> chan.connect(Utils.resolveAddress(address));
            try {
                 finished = AccessController.doPrivileged(pa);
            } catch (PrivilegedActionException e) {
               throw e.getCause();
            }
            if (finished) {
                if (debug.on()) debug.log("connect finished without blocking");
                cf.complete(ConnectState.SUCCESS);
            } else {
                if (debug.on()) debug.log("registering connect event");
                client().registerEvent(new ConnectEvent(cf, exchange));
            }
            cf = exchange.checkCancelled(cf, this);
        } catch (Throwable throwable) {
            cf.completeExceptionally(Utils.toConnectException(throwable));
            try {
                close();
            } catch (Exception x) {
                if (debug.on())
                    debug.log("Failed to close channel after unsuccessful connect");
            }
        }
        return cf.handle((r,t) -> checkRetryConnect(r, t,exchange))
                .thenCompose(Function.identity());
    }

    /**
     * On some platforms, a ConnectEvent may be raised and a ConnectionException
     * may occur with the message "Connection timed out: no further information"
     * before our actual connection timeout has expired. In this case, this
     * method will be called with a {@code connect} state of {@code ConnectState.RETRY)}
     * and we will retry once again.
     * @param connect indicates whether the connection was successful or should be retried
     * @param failed the failure if the connection failed
     * @param exchange the exchange
     * @return a completable future that will take care of retrying the connection if needed.
     */
    private CompletableFuture<Void> checkRetryConnect(ConnectState connect, Throwable failed, Exchange<?> exchange) {
        // first check if the connection failed
        if (failed != null) return MinimalFuture.failedFuture(failed);
        // then check if the connection should be retried
        if (connect == ConnectState.RETRY) {
            int attempts = unsuccessfulAttempts;
            assert attempts <= 1;
            if (debug.on())
                debug.log("Retrying connect after %d attempts", attempts);
            return connectAsync(exchange);
        }
        // Otherwise, the connection was successful;
        assert connect == ConnectState.SUCCESS;
        return MinimalFuture.completedFuture(null);
    }

    private boolean canRetryConnect(Throwable e) {
        if (!MultiExchange.RETRY_CONNECT) return false;
        if (!(e instanceof ConnectException)) return false;
        if (unsuccessfulAttempts > 0) return false;
        ConnectTimerEvent timer = connectTimerEvent;
        if (timer == null) return true;
        return timer.deadline().isAfter(Instant.now());
    }

    @Override
    public CompletableFuture<Void> finishConnect() {
        assert connected == false;
        if (debug.on()) debug.log("finishConnect, setting connected=true");
        connected = true;
        if (connectTimerEvent != null)
            client().cancelTimer(connectTimerEvent);
        return MinimalFuture.completedFuture(null);
    }

    @Override
    SocketChannel channel() {
        return chan;
    }

    @Override
    final FlowTube getConnectionFlow() {
        return tube;
    }

    PlainHttpConnection(InetSocketAddress addr, HttpClientImpl client) {
        super(addr, client);
        try {
            this.chan = SocketChannel.open();
            chan.configureBlocking(false);
            if (debug.on()) {
                int bufsize = getSoReceiveBufferSize();
                debug.log("Initial receive buffer size is: %d", bufsize);
                bufsize = getSoSendBufferSize();
                debug.log("Initial send buffer size is: %d", bufsize);
            }
            if (trySetReceiveBufferSize(client.getReceiveBufferSize())) {
                if (debug.on()) {
                    int bufsize = getSoReceiveBufferSize();
                    debug.log("Receive buffer size configured: %d", bufsize);
                }
            }
            if (trySetSendBufferSize(client.getSendBufferSize())) {
                if (debug.on()) {
                    int bufsize = getSoSendBufferSize();
                    debug.log("Send buffer size configured: %d", bufsize);
                }
            }
            chan.setOption(StandardSocketOptions.TCP_NODELAY, true);
            // wrap the channel in a Tube for async reading and writing
            tube = new SocketTube(client(), chan, Utils::getBuffer);
        } catch (IOException e) {
            throw new InternalError(e);
        }
    }

    private int getSoReceiveBufferSize() {
        try {
            return chan.getOption(StandardSocketOptions.SO_RCVBUF);
        } catch (IOException x) {
            if (debug.on())
                debug.log("Failed to get initial receive buffer size on %s", chan);
        }
        return 0;
    }

    private int getSoSendBufferSize() {
        try {
            return chan.getOption(StandardSocketOptions.SO_SNDBUF);
        } catch (IOException x) {
            if (debug.on())
                debug.log("Failed to get initial receive buffer size on %s", chan);
        }
        return 0;
    }

    private boolean trySetReceiveBufferSize(int bufsize) {
        try {
            if (bufsize > 0) {
                chan.setOption(StandardSocketOptions.SO_RCVBUF, bufsize);
                return true;
            }
        } catch (IOException x) {
            if (debug.on())
                debug.log("Failed to set receive buffer size to %d on %s",
                          bufsize, chan);
        }
        return false;
    }

    private boolean trySetSendBufferSize(int bufsize) {
        try {
            if (bufsize > 0) {
                chan.setOption(StandardSocketOptions.SO_SNDBUF, bufsize);
                return true;
            }
        } catch (IOException x) {
            if (debug.on())
                debug.log("Failed to set send buffer size to %d on %s",
                        bufsize, chan);
        }
        return false;
    }

    @Override
    HttpPublisher publisher() { return writePublisher; }


    @Override
    public String toString() {
        return "PlainHttpConnection: " + super.toString();
    }

    /**
     * Closes this connection
     */
    @Override
    public void close() {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
        }
        try {
            Log.logTrace("Closing: " + toString());
            if (debug.on())
                debug.log("Closing channel: " + client().debugInterestOps(chan));
            if (connectTimerEvent != null)
                client().cancelTimer(connectTimerEvent);
            chan.close();
            tube.signalClosed();
        } catch (IOException e) {
            Log.logTrace("Closing resulted in " + e);
        }
    }


    @Override
    ConnectionPool.CacheKey cacheKey() {
        return new ConnectionPool.CacheKey(address, null);
    }

    @Override
    synchronized boolean connected() {
        return connected;
    }


    @Override
    boolean isSecure() {
        return false;
    }

    @Override
    boolean isProxied() {
        return false;
    }

    @Override
    InetSocketAddress proxy() {
        return null;
    }
}
