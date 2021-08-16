/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.net.http.HttpResponse.BodySubscriber;
import jdk.internal.net.http.common.Demand;
import jdk.internal.net.http.common.SequentialScheduler;
import jdk.internal.net.http.ResponseSubscribers.TrustedSubscriber;

/**
 * A buffering BodySubscriber. When subscribed, accumulates ( buffers ) a given
 * amount ( in bytes ) of a publisher's data before pushing it to a downstream
 * subscriber.
 */
public class BufferingSubscriber<T> implements TrustedSubscriber<T>
{
    /** The downstream consumer of the data. */
    private final BodySubscriber<T> downstreamSubscriber;
    /** The amount of data to be accumulate before pushing downstream. */
    private final int bufferSize;

    /** The subscription, created lazily. */
    private volatile Flow.Subscription subscription;
    /** The downstream subscription, created lazily. */
    private volatile DownstreamSubscription downstreamSubscription;

    /** Must be held when accessing the internal buffers. */
    private final Object buffersLock = new Object();
    /** The internal buffers holding the buffered data. */
    private ArrayList<ByteBuffer> internalBuffers;
    /** The actual accumulated remaining bytes in internalBuffers. */
    private int accumulatedBytes;

    /** Holds the Throwable from upstream's onError. */
    private volatile Throwable throwable;

    /** State of the buffering subscriber:
     *  1) [UNSUBSCRIBED] when initially created
     *  2) [ACTIVE] when subscribed and can receive data
     *  3) [ERROR | CANCELLED | COMPLETE] (terminal state)
     */
    static final int UNSUBSCRIBED = 0x01;
    static final int ACTIVE       = 0x02;
    static final int ERROR        = 0x04;
    static final int CANCELLED    = 0x08;
    static final int COMPLETE     = 0x10;

    private volatile int state;

    public BufferingSubscriber(BodySubscriber<T> downstreamSubscriber,
                               int bufferSize) {
        this.downstreamSubscriber = Objects.requireNonNull(downstreamSubscriber);
        this.bufferSize = bufferSize;
        synchronized (buffersLock) {
            internalBuffers = new ArrayList<>();
        }
        state = UNSUBSCRIBED;
    }

    /** Returns the number of bytes remaining in the given buffers. */
    private static final long remaining(List<ByteBuffer> buffers) {
        return buffers.stream().mapToLong(ByteBuffer::remaining).sum();
    }

    @Override
    public boolean needsExecutor() {
        return TrustedSubscriber.needsExecutor(downstreamSubscriber);
    }

    /**
     * Tells whether, or not, there is at least a sufficient number of bytes
     * accumulated in the internal buffers. If the subscriber is COMPLETE, and
     * has some buffered data, then there is always enough ( to pass downstream ).
     */
    private final boolean hasEnoughAccumulatedBytes() {
        assert Thread.holdsLock(buffersLock);
        return accumulatedBytes >= bufferSize
                || (state == COMPLETE && accumulatedBytes > 0);
    }

    /**
     * Returns a new, unmodifiable, List<ByteBuffer> containing exactly the
     * amount of data as required before pushing downstream. The amount of data
     * may be less than required ( bufferSize ), in the case where the subscriber
     * is COMPLETE.
     */
    private List<ByteBuffer> fromInternalBuffers() {
        assert Thread.holdsLock(buffersLock);
        int leftToFill = bufferSize;
        int state = this.state;
        assert (state == ACTIVE || state == CANCELLED)
                ? accumulatedBytes >= leftToFill : true;
        List<ByteBuffer> dsts = new ArrayList<>();

        ListIterator<ByteBuffer> itr = internalBuffers.listIterator();
        while (itr.hasNext()) {
            ByteBuffer b = itr.next();
            if (b.remaining() <= leftToFill) {
                itr.remove();
                if (b.position() != 0)
                    b = b.slice();  // ensure position = 0 when propagated
                dsts.add(b);
                leftToFill -= b.remaining();
                accumulatedBytes -= b.remaining();
                if (leftToFill == 0)
                    break;
            } else {
                int prevLimit = b.limit();
                b.limit(b.position() + leftToFill);
                ByteBuffer slice = b.slice();
                dsts.add(slice);
                b.limit(prevLimit);
                b.position(b.position() + leftToFill);
                accumulatedBytes -= leftToFill;
                leftToFill = 0;
                break;
            }
        }
        assert (state == ACTIVE || state == CANCELLED)
                ? leftToFill == 0 : state == COMPLETE;
        assert (state == ACTIVE || state == CANCELLED)
                ? remaining(dsts) == bufferSize : state == COMPLETE;
        assert accumulatedBytes >= 0;
        assert dsts.stream().noneMatch(b -> b.position() != 0);
        return Collections.unmodifiableList(dsts);
    }

    /** Subscription that is passed to the downstream subscriber. */
    private class DownstreamSubscription implements Flow.Subscription {
        private final AtomicBoolean cancelled = new AtomicBoolean(); // false
        private final Demand demand = new Demand();
        private volatile boolean illegalArg;

        @Override
        public void request(long n) {
            if (cancelled.get() || illegalArg) {
                return;
            }
            if (n <= 0L) {
                // pass the "bad" value upstream so the Publisher can deal with
                // it appropriately, i.e. invoke onError
                illegalArg = true;
                subscription.request(n);
                return;
            }

            demand.increase(n);

            pushDemanded();
        }

        private final SequentialScheduler pushDemandedScheduler =
                new SequentialScheduler(new PushDemandedTask());

        void pushDemanded() {
            if (cancelled.get())
                return;
            pushDemandedScheduler.runOrSchedule();
        }

        class PushDemandedTask extends SequentialScheduler.CompleteRestartableTask {
            @Override
            public void run() {
                try {
                    Throwable t = throwable;
                    if (t != null) {
                        pushDemandedScheduler.stop(); // stop the demand scheduler
                        downstreamSubscriber.onError(t);
                        return;
                    }

                    while (true) {
                        List<ByteBuffer> item;
                        synchronized (buffersLock) {
                            if (cancelled.get())
                                return;
                            if (!hasEnoughAccumulatedBytes())
                                break;
                            if (!demand.tryDecrement())
                                break;
                            item = fromInternalBuffers();
                        }
                        assert item != null;

                        downstreamSubscriber.onNext(item);
                    }
                    if (cancelled.get())
                        return;

                    // complete only if all data consumed
                    boolean complete;
                    synchronized (buffersLock) {
                        complete = state == COMPLETE && internalBuffers.isEmpty();
                    }
                    if (complete) {
                        assert internalBuffers.isEmpty();
                        pushDemandedScheduler.stop(); // stop the demand scheduler
                        downstreamSubscriber.onComplete();
                        return;
                    }
                } catch (Throwable t) {
                    cancel();  // cancel if there is any error
                    throw t;
                }

                boolean requestMore = false;
                synchronized (buffersLock) {
                    if (!hasEnoughAccumulatedBytes() && !demand.isFulfilled()) {
                        // request more upstream data
                        requestMore = true;
                    }
                }
                if (requestMore)
                    subscription.request(1);
            }
        }

        @Override
        public void cancel() {
            if (cancelled.compareAndExchange(false, true))
                return;  // already cancelled

            state = CANCELLED;  // set CANCELLED state of upstream subscriber
            subscription.cancel();  // cancel upstream subscription
            pushDemandedScheduler.stop(); // stop the demand scheduler
        }
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        Objects.requireNonNull(subscription);
        if (this.subscription != null) {
            subscription.cancel();
            return;
        }

        int s = this.state;
        assert s == UNSUBSCRIBED;
        state = ACTIVE;
        this.subscription = subscription;
        downstreamSubscription = new DownstreamSubscription();
        downstreamSubscriber.onSubscribe(downstreamSubscription);
    }

    @Override
    public void onNext(List<ByteBuffer> item) {
        Objects.requireNonNull(item);

        int s = state;
        if (s == CANCELLED)
            return;

        if (s != ACTIVE)
            throw new InternalError("onNext on inactive subscriber");

        synchronized (buffersLock) {
            internalBuffers.addAll(item);
            accumulatedBytes += remaining(item);
        }

        downstreamSubscription.pushDemanded();
    }

    @Override
    public void onError(Throwable incomingThrowable) {
        Objects.requireNonNull(incomingThrowable);
        int s = state;
        assert s == ACTIVE : "Expected ACTIVE, got:" + s;
        state = ERROR;
        Throwable t = this.throwable;
        assert t == null : "Expected null, got:" + t;
        this.throwable = incomingThrowable;
        downstreamSubscription.pushDemanded();
    }

    @Override
    public void onComplete() {
        int s = state;
        assert s == ACTIVE : "Expected ACTIVE, got:" + s;
        state = COMPLETE;
        downstreamSubscription.pushDemanded();
    }

    @Override
    public CompletionStage<T> getBody() {
        return downstreamSubscriber.getBody();
    }
}
