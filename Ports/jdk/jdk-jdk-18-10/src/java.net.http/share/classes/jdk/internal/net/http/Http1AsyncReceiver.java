/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import jdk.internal.net.http.common.Demand;
import jdk.internal.net.http.common.FlowTube.TubeSubscriber;
import jdk.internal.net.http.common.Log;
import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.MinimalFuture;
import jdk.internal.net.http.common.SequentialScheduler;
import jdk.internal.net.http.common.ConnectionExpiredException;
import jdk.internal.net.http.common.Utils;


/**
 * A helper class that will queue up incoming data until the receiving
 * side is ready to handle it.
 */
class Http1AsyncReceiver {

    final Logger debug = Utils.getDebugLogger(this::dbgString, Utils.DEBUG);

    /**
     * A delegate that can asynchronously receive data from an upstream flow,
     * parse, it, then possibly transform it and either store it (response
     * headers) or possibly pass it to a downstream subscriber (response body).
     * Usually, there will be one Http1AsyncDelegate in charge of receiving
     * and parsing headers, and another one in charge of receiving, parsing,
     * and forwarding body. Each will sequentially subscribe with the
     * Http1AsyncReceiver in turn. There may be additional delegates which
     * subscribe to the Http1AsyncReceiver, mainly for the purpose of handling
     * errors while the connection is busy transmitting the request body and the
     * Http1Exchange::readBody method hasn't been called yet, and response
     * delegates haven't subscribed yet.
     */
    static interface Http1AsyncDelegate {
        /**
         * Receives and handles a byte buffer reference.
         * @param ref A byte buffer reference coming from upstream.
         * @return false, if the byte buffer reference should be kept in the queue.
         *         Usually, this means that either the byte buffer reference
         *         was handled and parsing is finished, or that the receiver
         *         didn't handle the byte reference at all.
         *         There may or may not be any remaining data in the
         *         byte buffer, and the byte buffer reference must not have
         *         been cleared.
         *         true, if the byte buffer reference was fully read and
         *         more data can be received.
         */
        public boolean tryAsyncReceive(ByteBuffer ref);

        /**
         * Called when an exception is raised.
         * @param ex The raised Throwable.
         */
        public void onReadError(Throwable ex);

        /**
         * Must be called before any other method on the delegate.
         * The subscription can be either used directly by the delegate
         * to request more data (e.g. if the delegate is a header parser),
         * or can be forwarded to a downstream subscriber (if the delegate
         * is a body parser that wraps a response BodySubscriber).
         * In all cases, it is the responsibility of the delegate to ensure
         * that request(n) and demand.tryDecrement() are called appropriately.
         * No data will be sent to {@code tryAsyncReceive} unless
         * the subscription has some demand.
         *
         * @param s A subscription that allows the delegate to control the
         *          data flow.
         */
        public void onSubscribe(AbstractSubscription s);

        /**
         * Returns the subscription that was passed to {@code onSubscribe}
         * @return the subscription that was passed to {@code onSubscribe}..
         */
        public AbstractSubscription subscription();

        /**
         * Called to make sure resources are released when the
         * when the Http1AsyncReceiver is stopped.
         * @param error The Http1AsyncReceiver pending error ref,
         *              if any.
         */
        public void close(Throwable error);

    }

    /**
     * A simple subclass of AbstractSubscription that ensures the
     * SequentialScheduler will be run when request() is called and demand
     * becomes positive again.
     */
    private static final class Http1AsyncDelegateSubscription
            extends AbstractSubscription
    {
        private final Runnable onCancel;
        private final Consumer<Throwable> onError;
        private final SequentialScheduler scheduler;
        private volatile boolean cancelled;
        Http1AsyncDelegateSubscription(SequentialScheduler scheduler,
                                       Runnable onCancel,
                                       Consumer<Throwable> onError) {
            this.scheduler = scheduler;
            this.onCancel = onCancel;
            this.onError = onError;
        }
        @Override
        public void request(long n) {
            if (cancelled) return;
            try {
                final Demand demand = demand();
                if (demand.increase(n)) {
                    scheduler.runOrSchedule();
                }
            } catch (IllegalArgumentException x) {
                cancelled = true;
                onError.accept(x);
            }
        }
        @Override
        public void cancel() {
            cancelled = true;
            onCancel.run();
        }
    }

    private final ConcurrentLinkedDeque<ByteBuffer> queue
            = new ConcurrentLinkedDeque<>();
    private final SequentialScheduler scheduler =
            SequentialScheduler.lockingScheduler(this::flush);
    final MinimalFuture<Void> whenFinished;
    private final Executor executor;
    private final Http1TubeSubscriber subscriber = new Http1TubeSubscriber();
    private final AtomicReference<Http1AsyncDelegate> pendingDelegateRef;
    private final AtomicLong received = new AtomicLong();
    final AtomicBoolean canRequestMore = new AtomicBoolean();

    private volatile Throwable error;
    private volatile Http1AsyncDelegate delegate;
    // This reference is only used to prevent early GC of the exchange.
    private volatile Http1Exchange<?>  owner;
    // Only used for checking whether we run on the selector manager thread.
    private final HttpClientImpl client;
    private boolean retry;
    private volatile boolean stopRequested;

    public Http1AsyncReceiver(Executor executor, Http1Exchange<?> owner) {
        this.pendingDelegateRef = new AtomicReference<>();
        this.executor = executor;
        this.whenFinished = new MinimalFuture<>();
        this.owner = owner;
        this.client = owner.client;
    }

    // This is the main loop called by the SequentialScheduler.
    // It attempts to empty the queue until the scheduler is stopped,
    // or the delegate is unregistered, or the delegate is unable to
    // process the data (because it's not ready or already done), which
    // it signals by returning 'true';
    private void flush() {
        ByteBuffer buf;
        try {
            // we should not be running in the selector here,
            // except if the custom Executor supplied to the client is
            // something like (r) -> r.run();
           assert !client.isSelectorThread()
                   || !(client.theExecutor().delegate() instanceof ExecutorService) :
                    "Http1AsyncReceiver::flush should not run in the selector: "
                    + Thread.currentThread().getName();

            // First check whether we have a pending delegate that has
            // just subscribed, and if so, create a Subscription for it
            // and call onSubscribe.
            handlePendingDelegate();

            // Then start emptying the queue, if possible.
            while ((buf = queue.peek()) != null && !stopRequested) {
                Http1AsyncDelegate delegate = this.delegate;
                if (debug.on())
                    debug.log("Got %s bytes for delegate %s",
                              buf.remaining(), delegate);
                if (!hasDemand(delegate)) {
                    // The scheduler will be invoked again later when the demand
                    // becomes positive.
                    return;
                }

                assert delegate != null;
                if (debug.on())
                    debug.log("Forwarding %s bytes to delegate %s",
                              buf.remaining(), delegate);
                // The delegate has demand: feed it the next buffer.
                if (!delegate.tryAsyncReceive(buf)) {
                    final long remaining = buf.remaining();
                    if (debug.on()) debug.log(() -> {
                        // If the scheduler is stopped, the queue may already
                        // be empty and the reference may already be released.
                        String remstr = scheduler.isStopped() ? "" :
                                " remaining in ref: "
                                + remaining;
                        remstr +=  remstr
                                + " total remaining: " + remaining();
                        return "Delegate done: " + remaining;
                    });
                    canRequestMore.set(false);
                    // The last buffer parsed may have remaining unparsed bytes.
                    // Don't take it out of the queue.
                    return; // done.
                }

                // removed parsed buffer from queue, and continue with next
                // if available
                ByteBuffer parsed = queue.remove();
                canRequestMore.set(queue.isEmpty() && !stopRequested);
                assert parsed == buf;
            }

            // queue is empty: let's see if we should request more
            checkRequestMore();

        } catch (Throwable t) {
            Throwable x = error;
            if (x == null) error = t; // will be handled in the finally block
            if (debug.on()) debug.log("Unexpected error caught in flush()", t);
        } finally {
            // Handles any pending error.
            // The most recently subscribed delegate will get the error.
            checkForErrors();
        }
    }

    private String describe() {
        Http1Exchange<?> exchange = owner;
        if (exchange != null) {
            return String.valueOf(exchange.request());
        }
        return "<uri unavailable>";
    }

    /**
     * Must be called from within the scheduler main loop.
     * Handles any pending errors by calling delegate.onReadError().
     * If the error can be forwarded to the delegate, stops the scheduler.
     */
    private void checkForErrors() {
        // Handles any pending error.
        // The most recently subscribed delegate will get the error.
        // If the delegate is null, the error will be handled by the next
        // delegate that subscribes.
        // If the queue is not empty, wait until it is empty before
        // handling the error.
        Http1AsyncDelegate delegate = pendingDelegateRef.get();
        if (delegate == null) delegate = this.delegate;
        Throwable x = error;
        if (delegate != null && x != null && (stopRequested || queue.isEmpty())) {
            // forward error only after emptying the queue.
            final Object captured = delegate;
            if (debug.on())
                debug.log(() -> "flushing " + x + "\n\t delegate: " + captured
                          + "\t\t queue.isEmpty: " + queue.isEmpty());
            scheduler.stop();
            delegate.onReadError(x);
            whenFinished.completeExceptionally(x);
            if (Log.channel()) {
                Log.logChannel("HTTP/1 read subscriber stopped for: {0}", describe());
            }
            if (stopRequested) {
                // This is the special case where the subscriber
                // has requested an illegal number of items.
                // In this case, the error doesn't come from
                // upstream, but from downstream, and we need to
                // close the upstream connection.
                Http1Exchange<?> exchg = owner;
                stop();
                if (exchg != null) exchg.connection().close();
            }
        }
    }

    /**
     * Must be called from within the scheduler main loop.
     * Figure out whether more data should be requested from the
     * Http1TubeSubscriber.
     */
    private void checkRequestMore() {
        Http1AsyncDelegate delegate = this.delegate;
        boolean more = this.canRequestMore.get();
        boolean hasDemand = hasDemand(delegate);
        if (debug.on())
            debug.log("checkRequestMore: " + "canRequestMore=" + more
                      + ", hasDemand=" + hasDemand
                      + (delegate == null ? ", delegate=null" : ""));
        if (hasDemand) {
            subscriber.requestMore();
        }
    }

    /**
     * Must be called from within the scheduler main loop.
     * Return true if the delegate is not null and has some demand.
     * @param delegate The Http1AsyncDelegate delegate
     * @return true if the delegate is not null and has some demand
     */
    private boolean hasDemand(Http1AsyncDelegate delegate) {
        if (delegate == null) return false;
        AbstractSubscription subscription = delegate.subscription();
        long demand = subscription.demand().get();
        if (debug.on())
            debug.log("downstream subscription demand is %s", demand);
        return demand > 0;
    }

    /**
     * Must be called from within the scheduler main loop.
     * Handles pending delegate subscription.
     * Return true if there was some pending delegate subscription and a new
     * delegate was subscribed, false otherwise.
     *
     * @return true if there was some pending delegate subscription and a new
     *         delegate was subscribed, false otherwise.
     */
    private boolean handlePendingDelegate() {
        Http1AsyncDelegate pending = pendingDelegateRef.get();
        if (pending != null && pendingDelegateRef.compareAndSet(pending, null)) {
            Http1AsyncDelegate delegate = this.delegate;
            if (delegate != null) unsubscribe(delegate);
            Consumer<Throwable> onSubscriptionError = (x) -> {
                setRetryOnError(false);
                stopRequested = true;
                onReadError(x);
            };
            Runnable cancel = () -> {
                if (debug.on())
                    debug.log("Downstream subscription cancelled by %s", pending);
                // The connection should be closed, as some data may
                // be left over in the stream.
                try {
                    setRetryOnError(false);
                    pending.close(null);
                    onReadError(new IOException("subscription cancelled"));
                    unsubscribe(pending);
                } finally {
                    Http1Exchange<?> exchg = owner;
                    stop();
                    if (exchg != null) exchg.connection().close();
                }
            };
            // The subscription created by a delegate is only loosely
            // coupled with the upstream subscription. This is partly because
            // the header/body parser work with a flow of ByteBuffer, whereas
            // we have a flow List<ByteBuffer> upstream.
            Http1AsyncDelegateSubscription subscription =
                    new Http1AsyncDelegateSubscription(scheduler, cancel, onSubscriptionError);
            try {
                pending.onSubscribe(subscription);
            } finally {
                this.delegate = delegate = pending;
            }
            final Object captured = delegate;
            if (debug.on())
                debug.log("delegate is now " + captured
                          + ", demand=" + subscription.demand().get()
                          + ", canRequestMore=" + canRequestMore.get()
                          + ", queue.isEmpty=" + queue.isEmpty());
            return true;
        }
        return false;
    }

    synchronized void setRetryOnError(boolean retry) {
        this.retry = retry;
    }

    void clear() {
        if (debug.on()) debug.log("cleared");
        this.pendingDelegateRef.set(null);
        this.delegate = null;
        this.owner = null;
    }

    void subscribe(Http1AsyncDelegate delegate) {
        synchronized(this) {
            pendingDelegateRef.set(delegate);
        }
        if (queue.isEmpty()) {
            canRequestMore.set(true);
        }
        if (debug.on())
            debug.log("Subscribed pending " + delegate + " queue.isEmpty: "
                      + queue.isEmpty());
        // Everything may have been received already. Make sure
        // we parse it.
        if (client.isSelectorThread()) {
            scheduler.runOrSchedule(executor);
        } else {
            scheduler.runOrSchedule();
        }
    }

    // Used for debugging only!
    long remaining() {
        return Utils.remaining(queue.toArray(Utils.EMPTY_BB_ARRAY));
    }

    void unsubscribe(Http1AsyncDelegate delegate) {
        synchronized(this) {
            if (this.delegate == delegate) {
                if (debug.on()) debug.log("Unsubscribed %s", delegate);
                this.delegate = null;
            }
        }
    }

    // Callback: Consumer of ByteBuffer
    private void asyncReceive(ByteBuffer buf) {
        if (debug.on())
            debug.log("Putting %s bytes into the queue", buf.remaining());
        received.addAndGet(buf.remaining());
        queue.offer(buf);

        // This callback is called from within the selector thread.
        // Use an executor here to avoid doing the heavy lifting in the
        // selector.
        scheduler.runOrSchedule(executor);
    }

    // Callback: Consumer of Throwable
    void onReadError(Throwable ex) {
        Http1AsyncDelegate delegate;
        Throwable recorded;
        if (debug.on()) debug.log("onError: %s", (Object) ex);
        synchronized (this) {
            delegate = this.delegate;
            recorded = error;
            if (recorded == null) {
                // retry is set to true by HttpExchange when the connection is
                // already connected, which means it's been retrieved from
                // the pool.
                if (retry && (ex instanceof IOException)) {
                    // could be either EOFException, or
                    // IOException("connection reset by peer), or
                    // SSLHandshakeException resulting from the server having
                    // closed the SSL session.
                    if (received.get() == 0) {
                        // If we receive such an exception before having
                        // received any byte, then in this case, we will
                        // throw ConnectionExpiredException
                        // to try & force a retry of the request.
                        retry = false;
                        ex = new ConnectionExpiredException(ex);
                    }
                }
                error = ex;
            }
        }

        final Throwable t = (recorded == null ? ex : recorded);
        if (debug.on())
            debug.log("recorded " + t + "\n\t delegate: " + delegate
                      + "\t\t queue.isEmpty: " + queue.isEmpty(), ex);
        if (Log.errors()) {
            Log.logError("HTTP/1 read subscriber recorded error: {0} - {1}", describe(), t);
        }
        if (queue.isEmpty() || pendingDelegateRef.get() != null || stopRequested) {
            // This callback is called from within the selector thread.
            // Use an executor here to avoid doing the heavy lifting in the
            // selector.
            if (Log.errors()) {
                Log.logError("HTTP/1 propagating recorded error: {0} - {1}", describe(), t);
            }
            scheduler.runOrSchedule(executor);
        }
    }

    void stop() {
        if (debug.on()) debug.log("stopping");
        if (Log.channel() && !scheduler.isStopped()) {
            Log.logChannel("HTTP/1 read subscriber stopped for {0}", describe());
        }
        scheduler.stop();
        // make sure ref count is handled properly by
        // closing the delegate.
        Http1AsyncDelegate previous = delegate;
        if (previous != null) previous.close(error);
        delegate = null;
        owner  = null;
        whenFinished.complete(null);
    }

    /**
     * Returns the TubeSubscriber for reading from the connection flow.
     * @return the TubeSubscriber for reading from the connection flow.
     */
    TubeSubscriber subscriber() {
        return subscriber;
    }

    /**
     * A simple tube subscriber for reading from the connection flow.
     */
    final class Http1TubeSubscriber implements TubeSubscriber {
        volatile Flow.Subscription subscription;
        volatile boolean completed;
        volatile boolean dropped;

        public void onSubscribe(Flow.Subscription subscription) {
            // supports being called multiple time.
            // doesn't cancel the previous subscription, since that is
            // most probably the same as the new subscription.
            if (debug.on()) debug.log("Received onSubscribed from upstream");
            if (Log.channel()) {
                Log.logChannel("HTTP/1 read subscriber got subscription from {0}", describe());
            }
            assert this.subscription == null || dropped == false;
            this.subscription = subscription;
            dropped = false;
            canRequestMore.set(true);
            if (delegate != null) {
                scheduler.runOrSchedule(executor);
            } else {
                if (debug.on()) debug.log("onSubscribe: read delegate not present yet");
            }
        }

        void requestMore() {
            Flow.Subscription s = subscription;
            if (s == null) return;
            if (canRequestMore.compareAndSet(true, false)) {
                if (!completed && !dropped) {
                    if (debug.on())
                        debug.log("Http1TubeSubscriber: requesting one more from upstream");
                    s.request(1);
                    return;
                }
            }
            if (debug.on())
                debug.log("Http1TubeSubscriber: no need to request more");
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            canRequestMore.set(item.isEmpty());
            for (ByteBuffer buffer : item) {
                asyncReceive(buffer);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            onReadError(throwable);
            completed = true;
        }

        @Override
        public void onComplete() {
            onReadError(new EOFException("EOF reached while reading"));
            completed = true;
        }

        public void dropSubscription() {
            if (debug.on()) debug.log("Http1TubeSubscriber: dropSubscription");
            // we could probably set subscription to null here...
            // then we might not need the 'dropped' boolean?
            dropped = true;
        }

    }

    // Drains the content of the queue into a single ByteBuffer.
    // The scheduler must be permanently stopped before calling drain().
    ByteBuffer drain(ByteBuffer initial) {
        // Revisit: need to clean that up.
        //
        ByteBuffer b = initial = (initial == null ? Utils.EMPTY_BYTEBUFFER : initial);
        assert scheduler.isStopped();

        if (queue.isEmpty()) return b;

        // sanity check: we shouldn't have queued the same
        // buffer twice.
        ByteBuffer[] qbb = queue.toArray(new ByteBuffer[queue.size()]);

// the assertion looks suspicious, more investigation needed
//
//        assert java.util.stream.Stream.of(qbb)
//                .collect(Collectors.toSet())
//                .size() == qbb.length : debugQBB(qbb);

        // compute the number of bytes in the queue, the number of bytes
        // in the initial buffer
        // TODO: will need revisiting - as it is not guaranteed that all
        // data will fit in single BB!
        int size = Utils.remaining(qbb, Integer.MAX_VALUE);
        int remaining = b.remaining();
        int free = b.capacity() - b.position() - remaining;
        if (debug.on())
            debug.log("Flushing %s bytes from queue into initial buffer "
                      + "(remaining=%s, free=%s)", size, remaining, free);

        // check whether the initial buffer has enough space
        if (size > free) {
            if (debug.on())
                debug.log("Allocating new buffer for initial: %s", (size + remaining));
            // allocates a new buffer and copy initial to it
            b = ByteBuffer.allocate(size + remaining);
            Utils.copy(initial, b);
            assert b.position() == remaining;
            b.flip();
            assert b.position() == 0;
            assert b.limit() == remaining;
            assert b.remaining() == remaining;
        }

        // store position and limit
        int pos = b.position();
        int limit = b.limit();
        assert limit - pos == remaining;
        assert b.capacity() >= remaining + size
                : "capacity: " + b.capacity()
                + ", remaining: " + b.remaining()
                + ", size: " + size;

        // prepare to copy the content of the queue
        b.position(limit);
        b.limit(pos + remaining + size);
        assert b.remaining() >= size :
                "remaining: " + b.remaining() + ", size: " + size;

        // copy the content of the queue
        int count = 0;
        for (int i=0; i<qbb.length; i++) {
            ByteBuffer b2 = qbb[i];
            int r = b2.remaining();
            assert b.remaining() >= r : "need at least " + r + " only "
                    + b.remaining() + " available";
            int copied = Utils.copy(b2, b);
            assert copied == r : "copied="+copied+" available="+r;
            assert b2.remaining() == 0;
            count += copied;
        }
        assert count == size;
        assert b.position() == pos + remaining + size :
                "b.position="+b.position()+" != "+pos+"+"+remaining+"+"+size;

        // reset limit and position
        b.limit(limit+size);
        b.position(pos);

        // we can clear the refs
        queue.clear();
        final ByteBuffer bb = b;
        if (debug.on())
            debug.log("Initial buffer now has " + bb.remaining()
                       + " pos=" + bb.position() + " limit=" + bb.limit());

        return b;
    }

    private String debugQBB(ByteBuffer[] qbb) {
        StringBuilder msg = new StringBuilder();
        List<ByteBuffer> lbb = Arrays.asList(qbb);
        Set<ByteBuffer> sbb = new HashSet<>(Arrays.asList(qbb));

        int uniquebb = sbb.size();
        msg.append("qbb: ").append(lbb.size())
           .append(" (unique: ").append(uniquebb).append("), ")
           .append("duplicates: ");
        String sep = "";
        for (ByteBuffer b : lbb) {
            if (!sbb.remove(b)) {
                msg.append(sep)
                   .append(String.valueOf(b))
                   .append("[remaining=")
                   .append(b.remaining())
                   .append(", position=")
                   .append(b.position())
                   .append(", capacity=")
                   .append(b.capacity())
                   .append("]");
                sep = ", ";
            }
        }
        return msg.toString();
    }

    volatile String dbgTag;
    String dbgString() {
        String tag = dbgTag;
        if (tag == null) {
            String flowTag = null;
            Http1Exchange<?> exchg = owner;
            Object flow = (exchg != null)
                    ? exchg.connection().getConnectionFlow()
                    : null;
            flowTag = tag = flow == null ? null: (String.valueOf(flow));
            if (flowTag != null) {
                dbgTag = tag = "Http1AsyncReceiver("+ flowTag + ")";
            } else {
                tag = "Http1AsyncReceiver(?)";
            }
        }
        return tag;
    }
}
