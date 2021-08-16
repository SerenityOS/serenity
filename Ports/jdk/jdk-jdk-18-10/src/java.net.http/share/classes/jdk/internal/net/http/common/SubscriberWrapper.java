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

package jdk.internal.net.http.common;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A wrapper for a Flow.Subscriber. This wrapper delivers data to the wrapped
 * Subscriber which is supplied to the constructor. This class takes care of
 * downstream flow control automatically and upstream flow control automatically
 * by default.
 * <p>
 * Processing is done by implementing the {@link #incoming(List, boolean)} method
 * which supplies buffers from upstream. This method (or any other method)
 * can then call the outgoing() method to deliver processed buffers downstream.
 * <p>
 * Upstream error signals are delivered downstream directly. Cancellation from
 * downstream is also propagated upstream immediately.
 * <p>
 * Each SubscriberWrapper has a {@link java.util.concurrent.CompletableFuture}{@code <Void>}
 * which propagates completion/errors from downstream to upstream. Normal completion
 * can only occur after onComplete() is called, but errors can be propagated upwards
 * at any time.
 */
public abstract class SubscriberWrapper
    implements FlowTube.TubeSubscriber, Closeable, Flow.Processor<List<ByteBuffer>,List<ByteBuffer>>
                // TODO: SSLTube Subscriber will never change? Does this really need to be a TS?
{
    final Logger debug =
            Utils.getDebugLogger(this::dbgString, Utils.DEBUG);

    public enum SchedulingAction { CONTINUE, RETURN, RESCHEDULE }

    volatile Flow.Subscription upstreamSubscription;
    final SubscriptionBase downstreamSubscription;
    volatile boolean upstreamCompleted;
    volatile boolean downstreamCompleted;
    volatile boolean completionAcknowledged;
    private volatile Subscriber<? super List<ByteBuffer>> downstreamSubscriber;
    // processed byte to send to the downstream subscriber.
    private final ConcurrentLinkedQueue<List<ByteBuffer>> outputQ;
    private final CompletableFuture<Void> cf;
    private final SequentialScheduler pushScheduler;
    private final AtomicReference<Throwable> errorRef = new AtomicReference<>();
    final AtomicLong upstreamWindow = new AtomicLong();

    /**
     * Wraps the given downstream subscriber. For each call to {@link
     * #onNext(List<ByteBuffer>) } the given filter function is invoked
     * and the list (if not empty) returned is passed downstream.
     *
     * A {@code CompletableFuture} is supplied which can be used to signal an
     * error from downstream and which terminates the wrapper or which signals
     * completion of downstream activity which can be propagated upstream. Error
     * completion can be signaled at any time, but normal completion must not be
     * signaled before onComplete() is called.
     */
    public SubscriberWrapper()
    {
        this.outputQ = new ConcurrentLinkedQueue<>();
        this.cf = new MinimalFuture<Void>();
        cf.whenComplete((v,t) -> {
            if (t != null)
                errorCommon(t);
        });
        this.pushScheduler =
                SequentialScheduler.lockingScheduler(new DownstreamPusher());
        this.downstreamSubscription = new SubscriptionBase(pushScheduler,
                                                           this::downstreamCompletion);
    }

    @Override
    public final void subscribe(Subscriber<?  super List<ByteBuffer>> downstreamSubscriber) {
        Objects.requireNonNull(downstreamSubscriber);
        this.downstreamSubscriber = downstreamSubscriber;
    }

    /**
     * Wraps the given downstream wrapper in this. For each call to
     * {@link #onNext(List<ByteBuffer>) } the incoming() method is called.
     *
     * The {@code downstreamCF} from the downstream wrapper is linked to this
     * wrappers notifier.
     *
     * @param downstreamWrapper downstream destination
     */
    public SubscriberWrapper(Subscriber<? super List<ByteBuffer>> downstreamWrapper)
    {
        this();
        subscribe(downstreamWrapper);
    }

    /**
     * Delivers data to be processed by this wrapper. Generated data to be sent
     * downstream, must be provided to the {@link #outgoing(List, boolean)}}
     * method.
     *
     * @param buffers a List of ByteBuffers.
     * @param complete if true then no more data will be added to the list
     */
    protected abstract void incoming(List<ByteBuffer> buffers, boolean complete);

    /**
     * This method is called to determine the window size to use at any time. The
     * current window is supplied together with the current downstream queue size.
     * {@code 0} should be returned if no change is
     * required or a positive integer which will be added to the current window.
     * The default implementation maintains a downstream queue size of no greater
     * than 5. The method can be overridden if required.
     *
     * @param currentWindow the current upstream subscription window
     * @param downstreamQsize the current number of buffers waiting to be sent
     *                        downstream
     *
     * @return value to add to currentWindow
     */
    protected long upstreamWindowUpdate(long currentWindow, long downstreamQsize) {
        if (downstreamQsize > 5) {
            return 0;
        }

        if (currentWindow == 0) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Override this if anything needs to be done after the upstream subscriber
     * has subscribed
     */
    protected void onSubscribe() {
    }

    /**
     * Override this if anything needs to be done before checking for error
     * and processing the input queue.
     * @return
     */
    protected SchedulingAction enterScheduling() {
        return SchedulingAction.CONTINUE;
    }

    protected boolean signalScheduling() {
        if (downstreamCompleted || pushScheduler.isStopped()) {
            return false;
        }
        pushScheduler.runOrSchedule();
        return true;
    }

    /**
     * Delivers buffers of data downstream. After incoming()
     * has been called complete == true signifying completion of the upstream
     * subscription, data may continue to be delivered, up to when outgoing() is
     * called complete == true, after which, the downstream subscription is
     * completed.
     *
     * It's an error to call outgoing() with complete = true if incoming() has
     * not previously been called with it.
     */
    public void outgoing(ByteBuffer buffer, boolean complete) {
        Objects.requireNonNull(buffer);
        assert !complete || !buffer.hasRemaining();
        outgoing(List.of(buffer), complete);
    }

    /**
     * Sometime it might be necessary to complete the downstream subscriber
     * before the upstream completes. For instance, when an SSL server
     * sends a notify_close. In that case we should let the outgoing
     * complete before upstream is completed.
     * @return true, may be overridden by subclasses.
     */
    public boolean closing() {
        return false;
    }

    public void outgoing(List<ByteBuffer> buffers, boolean complete) {
        Objects.requireNonNull(buffers);
        if (complete) {
            assert Utils.remaining(buffers) == 0;
            boolean closing = closing();
            if (debug.on())
                debug.log("completionAcknowledged upstreamCompleted:%s,"
                          + " downstreamCompleted:%s, closing:%s",
                          upstreamCompleted, downstreamCompleted, closing);
            if (!upstreamCompleted && !closing) {
                throw new IllegalStateException("upstream not completed");
            }
            completionAcknowledged = true;
        } else {
            if (debug.on())
                debug.log("Adding %d to outputQ queue", Utils.remaining(buffers));
            outputQ.add(buffers);
        }
        if (debug.on())
            debug.log("pushScheduler" +(pushScheduler.isStopped() ? " is stopped!" : " is alive"));
        pushScheduler.runOrSchedule();
    }

    /**
     * Returns a CompletableFuture which completes when this wrapper completes.
     * Normal completion happens with the following steps (in order):
     *   1. onComplete() is called
     *   2. incoming() called with complete = true
     *   3. outgoing() may continue to be called normally
     *   4. outgoing called with complete = true
     *   5. downstream subscriber is called onComplete()
     *
     * If the subscription is canceled or onComplete() is invoked the
     * CompletableFuture completes exceptionally. Exceptional completion
     * also occurs if downstreamCF completes exceptionally.
     */
    public CompletableFuture<Void> completion() {
        return cf;
    }

    /**
     * Invoked whenever it 'may' be possible to push buffers downstream.
     */
    class DownstreamPusher implements Runnable {
        @Override
        public void run() {
            try {
                run1();
            } catch (Throwable t) {
                if (debug.on())
                    debug.log("DownstreamPusher threw: " + t);
                errorCommon(t);
            }
        }

        private void run1() {
            if (downstreamCompleted) {
                if (debug.on())
                    debug.log("DownstreamPusher: downstream is already completed");
                return;
            }
            switch (enterScheduling()) {
                case CONTINUE: break;
                case RESCHEDULE: pushScheduler.runOrSchedule(); return;
                case RETURN: return;
                default:
                    errorRef.compareAndSet(null,
                            new InternalError("unknown scheduling command"));
                    break;
            }
            // If there was an error, send it downstream.
            Throwable error = errorRef.get();
            if (error != null && outputQ.isEmpty()) {
                synchronized(this) {
                    if (downstreamCompleted)
                        return;
                    downstreamCompleted = true;
                }
                if (debug.on())
                    debug.log("DownstreamPusher: forwarding error downstream: " + error);
                pushScheduler.stop();
                outputQ.clear();
                downstreamSubscriber.onError(error);
                cf.completeExceptionally(error);
                return;
            }

            // OK - no error, let's proceed
            if (!outputQ.isEmpty()) {
                if (debug.on())
                    debug.log("DownstreamPusher: queue not empty, downstreamSubscription: %s",
                              downstreamSubscription);
            } else {
                if (debug.on())
                    debug.log("DownstreamPusher: queue empty, downstreamSubscription: %s",
                               downstreamSubscription);
            }

            boolean datasent = false;
            while (!outputQ.isEmpty() && downstreamSubscription.tryDecrement()) {
                List<ByteBuffer> b = outputQ.poll();
                if (debug.on())
                    debug.log("DownstreamPusher: Pushing %d bytes downstream",
                              Utils.remaining(b));
                downstreamSubscriber.onNext(b);
                datasent = true;
            }

            // If we have sent some decrypted data downstream,
            // or if:
            //    - there's nothing more available to send downstream
            //    - and we still have some demand from downstream
            //    - and upstream is not completed yet
            //    - and our demand from upstream has reached 0,
            // then check whether we should request more data from
            // upstream
            if (datasent || outputQ.isEmpty()
                    && !downstreamSubscription.demand.isFulfilled()
                    && !upstreamCompleted
                    && upstreamWindow.get() == 0) {
                upstreamWindowUpdate();
            }
            checkCompletion();
        }
    }

    final int outputQueueSize() {
        return outputQ.size();
    }

    final boolean hasNoOutputData() {
        return outputQ.isEmpty();
    }

    void upstreamWindowUpdate() {
        long downstreamQueueSize = outputQ.size();
        long upstreamWindowSize = upstreamWindow.get();
        long n = upstreamWindowUpdate(upstreamWindowSize, downstreamQueueSize);
        if (debug.on())
            debug.log("upstreamWindowUpdate, "
                      + "downstreamQueueSize:%d, upstreamWindow:%d",
                      downstreamQueueSize, upstreamWindowSize);
        if (n > 0)
            upstreamRequest(n);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        if (upstreamSubscription != null) {
            throw new IllegalStateException("Single shot publisher");
        }
        this.upstreamSubscription = subscription;
        upstreamRequest(initialUpstreamDemand());
        if (debug.on())
            debug.log("calling downstreamSubscriber::onSubscribe on %s",
                      downstreamSubscriber);
        downstreamSubscriber.onSubscribe(downstreamSubscription);
        onSubscribe();
    }

    @Override
    public void onNext(List<ByteBuffer> item) {
        if (debug.on()) debug.log("onNext");
        long prev = upstreamWindow.getAndDecrement();
        if (prev <= 0)
            throw new IllegalStateException("invalid onNext call");
        incomingCaller(item, false);
    }

    private void upstreamRequest(long n) {
        if (debug.on()) debug.log("requesting %d", n);
        upstreamWindow.getAndAdd(n);
        upstreamSubscription.request(n);
    }

    /**
     * Initial demand that should be requested
     * from upstream when we get the upstream subscription
     * from {@link #onSubscribe(Flow.Subscription)}.
     * @return The initial demand to request from upstream.
     */
    protected long initialUpstreamDemand() {
        return 1;
    }

    protected void requestMore() {
        if (upstreamWindow.get() == 0) {
            upstreamRequest(1);
        }
    }

    public long upstreamWindow() {
        return upstreamWindow.get();
    }

    @Override
    public void onError(Throwable throwable) {
        if (debug.on()) debug.log("onError: " + throwable);
        errorCommon(Objects.requireNonNull(throwable));
    }

    protected boolean errorCommon(Throwable throwable) {
        assert throwable != null ||
                (throwable = new AssertionError("null throwable")) != null;
        if (errorRef.compareAndSet(null, throwable)) {
            if (debug.on()) debug.log("error", throwable);
            upstreamCompleted = true;
            pushScheduler.runOrSchedule();
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        errorCommon(new RuntimeException("wrapper closed"));
    }

    public void close(Throwable t) {
        errorCommon(t);
    }

    private void incomingCaller(List<ByteBuffer> l, boolean complete) {
        try {
            incoming(l, complete);
        } catch(Throwable t) {
            errorCommon(t);
        }
    }

    @Override
    public void onComplete() {
        if (debug.on()) debug.log("upstream completed: " + toString());
        upstreamCompleted = true;
        incomingCaller(Utils.EMPTY_BB_LIST, true);
        // pushScheduler will call checkCompletion()
        pushScheduler.runOrSchedule();
    }

    /** Adds the given data to the input queue. */
    public void addData(ByteBuffer l) {
        if (upstreamSubscription == null) {
            throw new IllegalStateException("can't add data before upstream subscriber subscribes");
        }
        incomingCaller(List.of(l), false);
    }

    void checkCompletion() {
        if (downstreamCompleted || !upstreamCompleted) {
            return;
        }
        if (!outputQ.isEmpty()) {
            return;
        }
        if (errorRef.get() != null) {
            pushScheduler.runOrSchedule();
            return;
        }
        if (completionAcknowledged) {
            if (debug.on()) debug.log("calling downstreamSubscriber.onComplete()");
            downstreamSubscriber.onComplete();
            // Fix me subscriber.onComplete.run();
            downstreamCompleted = true;
            cf.complete(null);
        }
    }

    // called from the downstream Subscription.cancel()
    void downstreamCompletion() {
        upstreamSubscription.cancel();
        cf.complete(null);
    }

    public void resetDownstreamDemand() {
        downstreamSubscription.demand.reset();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SubscriberWrapper:")
          .append(" upstreamCompleted: ").append(Boolean.toString(upstreamCompleted))
          .append(" upstreamWindow: ").append(upstreamWindow.toString())
          .append(" downstreamCompleted: ").append(Boolean.toString(downstreamCompleted))
          .append(" completionAcknowledged: ").append(Boolean.toString(completionAcknowledged))
          .append(" outputQ size: ").append(Integer.toString(outputQ.size()))
          //.append(" outputQ: ").append(outputQ.toString())
          .append(" cf: ").append(cf.toString())
          .append(" downstreamSubscription: ").append(downstreamSubscription)
          .append(" downstreamSubscriber: ").append(downstreamSubscriber);

        return sb.toString();
    }

    public String dbgString() {
        return "SubscriberWrapper";
    }
}
