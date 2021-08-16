/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import static java.util.concurrent.Flow.Publisher;
import static java.util.concurrent.Flow.Subscriber;
import static java.util.concurrent.Flow.Subscription;

/**
 * A {@link Flow.Publisher} that asynchronously issues submitted
 * (non-null) items to current subscribers until it is closed.  Each
 * current subscriber receives newly submitted items in the same order
 * unless drops or exceptions are encountered.  Using a
 * SubmissionPublisher allows item generators to act as compliant <a
 * href="http://www.reactive-streams.org/"> reactive-streams</a>
 * Publishers relying on drop handling and/or blocking for flow
 * control.
 *
 * <p>A SubmissionPublisher uses the {@link Executor} supplied in its
 * constructor for delivery to subscribers. The best choice of
 * Executor depends on expected usage. If the generator(s) of
 * submitted items run in separate threads, and the number of
 * subscribers can be estimated, consider using a {@link
 * Executors#newFixedThreadPool}. Otherwise consider using the
 * default, normally the {@link ForkJoinPool#commonPool}.
 *
 * <p>Buffering allows producers and consumers to transiently operate
 * at different rates.  Each subscriber uses an independent buffer.
 * Buffers are created upon first use and expanded as needed up to the
 * given maximum. (The enforced capacity may be rounded up to the
 * nearest power of two and/or bounded by the largest value supported
 * by this implementation.)  Invocations of {@link
 * Flow.Subscription#request(long) request} do not directly result in
 * buffer expansion, but risk saturation if unfilled requests exceed
 * the maximum capacity.  The default value of {@link
 * Flow#defaultBufferSize()} may provide a useful starting point for
 * choosing a capacity based on expected rates, resources, and usages.
 *
 * <p>A single SubmissionPublisher may be shared among multiple
 * sources. Actions in a source thread prior to publishing an item or
 * issuing a signal <a href="package-summary.html#MemoryVisibility">
 * <i>happen-before</i></a> actions subsequent to the corresponding
 * access by each subscriber. But reported estimates of lag and demand
 * are designed for use in monitoring, not for synchronization
 * control, and may reflect stale or inaccurate views of progress.
 *
 * <p>Publication methods support different policies about what to do
 * when buffers are saturated. Method {@link #submit(Object) submit}
 * blocks until resources are available. This is simplest, but least
 * responsive.  The {@code offer} methods may drop items (either
 * immediately or with bounded timeout), but provide an opportunity to
 * interpose a handler and then retry.
 *
 * <p>If any Subscriber method throws an exception, its subscription
 * is cancelled.  If a handler is supplied as a constructor argument,
 * it is invoked before cancellation upon an exception in method
 * {@link Flow.Subscriber#onNext onNext}, but exceptions in methods
 * {@link Flow.Subscriber#onSubscribe onSubscribe},
 * {@link Flow.Subscriber#onError(Throwable) onError} and
 * {@link Flow.Subscriber#onComplete() onComplete} are not recorded or
 * handled before cancellation.  If the supplied Executor throws
 * {@link RejectedExecutionException} (or any other RuntimeException
 * or Error) when attempting to execute a task, or a drop handler
 * throws an exception when processing a dropped item, then the
 * exception is rethrown. In these cases, not all subscribers will
 * have been issued the published item. It is usually good practice to
 * {@link #closeExceptionally closeExceptionally} in these cases.
 *
 * <p>Method {@link #consume(Consumer)} simplifies support for a
 * common case in which the only action of a subscriber is to request
 * and process all items using a supplied function.
 *
 * <p>This class may also serve as a convenient base for subclasses
 * that generate items, and use the methods in this class to publish
 * them.  For example here is a class that periodically publishes the
 * items generated from a supplier. (In practice you might add methods
 * to independently start and stop generation, to share Executors
 * among publishers, and so on, or use a SubmissionPublisher as a
 * component rather than a superclass.)
 *
 * <pre> {@code
 * class PeriodicPublisher<T> extends SubmissionPublisher<T> {
 *   final ScheduledFuture<?> periodicTask;
 *   final ScheduledExecutorService scheduler;
 *   PeriodicPublisher(Executor executor, int maxBufferCapacity,
 *                     Supplier<? extends T> supplier,
 *                     long period, TimeUnit unit) {
 *     super(executor, maxBufferCapacity);
 *     scheduler = new ScheduledThreadPoolExecutor(1);
 *     periodicTask = scheduler.scheduleAtFixedRate(
 *       () -> submit(supplier.get()), 0, period, unit);
 *   }
 *   public void close() {
 *     periodicTask.cancel(false);
 *     scheduler.shutdown();
 *     super.close();
 *   }
 * }}</pre>
 *
 * <p>Here is an example of a {@link Flow.Processor} implementation.
 * It uses single-step requests to its publisher for simplicity of
 * illustration. A more adaptive version could monitor flow using the
 * lag estimate returned from {@code submit}, along with other utility
 * methods.
 *
 * <pre> {@code
 * class TransformProcessor<S,T> extends SubmissionPublisher<T>
 *   implements Flow.Processor<S,T> {
 *   final Function<? super S, ? extends T> function;
 *   Flow.Subscription subscription;
 *   TransformProcessor(Executor executor, int maxBufferCapacity,
 *                      Function<? super S, ? extends T> function) {
 *     super(executor, maxBufferCapacity);
 *     this.function = function;
 *   }
 *   public void onSubscribe(Flow.Subscription subscription) {
 *     (this.subscription = subscription).request(1);
 *   }
 *   public void onNext(S item) {
 *     subscription.request(1);
 *     submit(function.apply(item));
 *   }
 *   public void onError(Throwable ex) { closeExceptionally(ex); }
 *   public void onComplete() { close(); }
 * }}</pre>
 *
 * @param <T> the published item type
 * @author Doug Lea
 * @since 9
 */
public class SubmissionPublisher<T> implements Publisher<T>,
                                               AutoCloseable {
    /*
     * Most mechanics are handled by BufferedSubscription. This class
     * mainly tracks subscribers and ensures sequentiality, by using
     * locks across public methods, to ensure thread-safety in the
     * presence of multiple sources and maintain acquire-release
     * ordering around user operations. However, we also track whether
     * there is only a single source, and if so streamline some buffer
     * operations by avoiding some atomics.
     */

    /** The largest possible power of two array size. */
    static final int BUFFER_CAPACITY_LIMIT = 1 << 30;

    /**
     * Initial buffer capacity used when maxBufferCapacity is
     * greater. Must be a power of two.
     */
    static final int INITIAL_CAPACITY = 32;

    /** Round capacity to power of 2, at most limit. */
    static final int roundCapacity(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n <= 0) ? 1 : // at least 1
            (n >= BUFFER_CAPACITY_LIMIT) ? BUFFER_CAPACITY_LIMIT : n + 1;
    }

    // default Executor setup; nearly the same as CompletableFuture

    /**
     * Default executor -- ForkJoinPool.commonPool() unless it cannot
     * support parallelism.
     */
    private static final Executor ASYNC_POOL =
        (ForkJoinPool.getCommonPoolParallelism() > 1) ?
        ForkJoinPool.commonPool() : new ThreadPerTaskExecutor();

    /** Fallback if ForkJoinPool.commonPool() cannot support parallelism */
    private static final class ThreadPerTaskExecutor implements Executor {
        ThreadPerTaskExecutor() {}      // prevent access constructor creation
        public void execute(Runnable r) { new Thread(r).start(); }
    }

    /**
     * Clients (BufferedSubscriptions) are maintained in a linked list
     * (via their "next" fields). This works well for publish loops.
     * It requires O(n) traversal to check for duplicate subscribers,
     * but we expect that subscribing is much less common than
     * publishing. Unsubscribing occurs only during traversal loops,
     * when BufferedSubscription methods return negative values
     * signifying that they have been closed.  To reduce
     * head-of-line blocking, submit and offer methods first call
     * BufferedSubscription.offer on each subscriber, and place
     * saturated ones in retries list (using nextRetry field), and
     * retry, possibly blocking or dropping.
     */
    BufferedSubscription<T> clients;

    /** Lock for exclusion across multiple sources */
    final ReentrantLock lock;
    /** Run status, updated only within locks */
    volatile boolean closed;
    /** Set true on first call to subscribe, to initialize possible owner */
    boolean subscribed;
    /** The first caller thread to subscribe, or null if thread ever changed */
    Thread owner;
    /** If non-null, the exception in closeExceptionally */
    volatile Throwable closedException;

    // Parameters for constructing BufferedSubscriptions
    final Executor executor;
    final BiConsumer<? super Subscriber<? super T>, ? super Throwable> onNextHandler;
    final int maxBufferCapacity;

    /**
     * Creates a new SubmissionPublisher using the given Executor for
     * async delivery to subscribers, with the given maximum buffer size
     * for each subscriber, and, if non-null, the given handler invoked
     * when any Subscriber throws an exception in method {@link
     * Flow.Subscriber#onNext(Object) onNext}.
     *
     * @param executor the executor to use for async delivery,
     * supporting creation of at least one independent thread
     * @param maxBufferCapacity the maximum capacity for each
     * subscriber's buffer (the enforced capacity may be rounded up to
     * the nearest power of two and/or bounded by the largest value
     * supported by this implementation; method {@link #getMaxBufferCapacity}
     * returns the actual value)
     * @param handler if non-null, procedure to invoke upon exception
     * thrown in method {@code onNext}
     * @throws NullPointerException if executor is null
     * @throws IllegalArgumentException if maxBufferCapacity not
     * positive
     */
    public SubmissionPublisher(Executor executor, int maxBufferCapacity,
                               BiConsumer<? super Subscriber<? super T>, ? super Throwable> handler) {
        if (executor == null)
            throw new NullPointerException();
        if (maxBufferCapacity <= 0)
            throw new IllegalArgumentException("capacity must be positive");
        this.lock = new ReentrantLock();
        this.executor = executor;
        this.onNextHandler = handler;
        this.maxBufferCapacity = roundCapacity(maxBufferCapacity);
    }

    /**
     * Creates a new SubmissionPublisher using the given Executor for
     * async delivery to subscribers, with the given maximum buffer size
     * for each subscriber, and no handler for Subscriber exceptions in
     * method {@link Flow.Subscriber#onNext(Object) onNext}.
     *
     * @param executor the executor to use for async delivery,
     * supporting creation of at least one independent thread
     * @param maxBufferCapacity the maximum capacity for each
     * subscriber's buffer (the enforced capacity may be rounded up to
     * the nearest power of two and/or bounded by the largest value
     * supported by this implementation; method {@link #getMaxBufferCapacity}
     * returns the actual value)
     * @throws NullPointerException if executor is null
     * @throws IllegalArgumentException if maxBufferCapacity not
     * positive
     */
    public SubmissionPublisher(Executor executor, int maxBufferCapacity) {
        this(executor, maxBufferCapacity, null);
    }

    /**
     * Creates a new SubmissionPublisher using the {@link
     * ForkJoinPool#commonPool()} for async delivery to subscribers
     * (unless it does not support a parallelism level of at least two,
     * in which case, a new Thread is created to run each task), with
     * maximum buffer capacity of {@link Flow#defaultBufferSize}, and no
     * handler for Subscriber exceptions in method {@link
     * Flow.Subscriber#onNext(Object) onNext}.
     */
    public SubmissionPublisher() {
        this(ASYNC_POOL, Flow.defaultBufferSize(), null);
    }

    /**
     * Adds the given Subscriber unless already subscribed.  If already
     * subscribed, the Subscriber's {@link
     * Flow.Subscriber#onError(Throwable) onError} method is invoked on
     * the existing subscription with an {@link IllegalStateException}.
     * Otherwise, upon success, the Subscriber's {@link
     * Flow.Subscriber#onSubscribe onSubscribe} method is invoked
     * asynchronously with a new {@link Flow.Subscription}.  If {@link
     * Flow.Subscriber#onSubscribe onSubscribe} throws an exception, the
     * subscription is cancelled. Otherwise, if this SubmissionPublisher
     * was closed exceptionally, then the subscriber's {@link
     * Flow.Subscriber#onError onError} method is invoked with the
     * corresponding exception, or if closed without exception, the
     * subscriber's {@link Flow.Subscriber#onComplete() onComplete}
     * method is invoked.  Subscribers may enable receiving items by
     * invoking the {@link Flow.Subscription#request(long) request}
     * method of the new Subscription, and may unsubscribe by invoking
     * its {@link Flow.Subscription#cancel() cancel} method.
     *
     * @param subscriber the subscriber
     * @throws NullPointerException if subscriber is null
     */
    public void subscribe(Subscriber<? super T> subscriber) {
        if (subscriber == null) throw new NullPointerException();
        ReentrantLock lock = this.lock;
        int max = maxBufferCapacity; // allocate initial array
        Object[] array = new Object[max < INITIAL_CAPACITY ?
                                    max : INITIAL_CAPACITY];
        BufferedSubscription<T> subscription =
            new BufferedSubscription<T>(subscriber, executor, onNextHandler,
                                        array, max);
        lock.lock();
        try {
            if (!subscribed) {
                subscribed = true;
                owner = Thread.currentThread();
            }
            for (BufferedSubscription<T> b = clients, pred = null;;) {
                if (b == null) {
                    Throwable ex;
                    subscription.onSubscribe();
                    if ((ex = closedException) != null)
                        subscription.onError(ex);
                    else if (closed)
                        subscription.onComplete();
                    else if (pred == null)
                        clients = subscription;
                    else
                        pred.next = subscription;
                    break;
                }
                BufferedSubscription<T> next = b.next;
                if (b.isClosed()) {   // remove
                    b.next = null;    // detach
                    if (pred == null)
                        clients = next;
                    else
                        pred.next = next;
                }
                else if (subscriber.equals(b.subscriber)) {
                    b.onError(new IllegalStateException("Duplicate subscribe"));
                    break;
                }
                else
                    pred = b;
                b = next;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Common implementation for all three forms of submit and offer.
     * Acts as submit if nanos == Long.MAX_VALUE, else offer.
     */
    private int doOffer(T item, long nanos,
                        BiPredicate<Subscriber<? super T>, ? super T> onDrop) {
        if (item == null) throw new NullPointerException();
        int lag = 0;
        boolean complete, unowned;
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Thread t = Thread.currentThread(), o;
            BufferedSubscription<T> b = clients;
            if ((unowned = ((o = owner) != t)) && o != null)
                owner = null;                     // disable bias
            if (b == null)
                complete = closed;
            else {
                complete = false;
                boolean cleanMe = false;
                BufferedSubscription<T> retries = null, rtail = null, next;
                do {
                    next = b.next;
                    int stat = b.offer(item, unowned);
                    if (stat == 0) {              // saturated; add to retry list
                        b.nextRetry = null;       // avoid garbage on exceptions
                        if (rtail == null)
                            retries = b;
                        else
                            rtail.nextRetry = b;
                        rtail = b;
                    }
                    else if (stat < 0)            // closed
                        cleanMe = true;           // remove later
                    else if (stat > lag)
                        lag = stat;
                } while ((b = next) != null);

                if (retries != null || cleanMe)
                    lag = retryOffer(item, nanos, onDrop, retries, lag, cleanMe);
            }
        } finally {
            lock.unlock();
        }
        if (complete)
            throw new IllegalStateException("Closed");
        else
            return lag;
    }

    /**
     * Helps, (timed) waits for, and/or drops buffers on list; returns
     * lag or negative drops (for use in offer).
     */
    private int retryOffer(T item, long nanos,
                           BiPredicate<Subscriber<? super T>, ? super T> onDrop,
                           BufferedSubscription<T> retries, int lag,
                           boolean cleanMe) {
        for (BufferedSubscription<T> r = retries; r != null;) {
            BufferedSubscription<T> nextRetry = r.nextRetry;
            r.nextRetry = null;
            if (nanos > 0L)
                r.awaitSpace(nanos);
            int stat = r.retryOffer(item);
            if (stat == 0 && onDrop != null && onDrop.test(r.subscriber, item))
                stat = r.retryOffer(item);
            if (stat == 0)
                lag = (lag >= 0) ? -1 : lag - 1;
            else if (stat < 0)
                cleanMe = true;
            else if (lag >= 0 && stat > lag)
                lag = stat;
            r = nextRetry;
        }
        if (cleanMe)
            cleanAndCount();
        return lag;
    }

    /**
     * Returns current list count after removing closed subscribers.
     * Call only while holding lock.  Used mainly by retryOffer for
     * cleanup.
     */
    private int cleanAndCount() {
        int count = 0;
        BufferedSubscription<T> pred = null, next;
        for (BufferedSubscription<T> b = clients; b != null; b = next) {
            next = b.next;
            if (b.isClosed()) {
                b.next = null;
                if (pred == null)
                    clients = next;
                else
                    pred.next = next;
            }
            else {
                pred = b;
                ++count;
            }
        }
        return count;
    }

    /**
     * Publishes the given item to each current subscriber by
     * asynchronously invoking its {@link Flow.Subscriber#onNext(Object)
     * onNext} method, blocking uninterruptibly while resources for any
     * subscriber are unavailable. This method returns an estimate of
     * the maximum lag (number of items submitted but not yet consumed)
     * among all current subscribers. This value is at least one
     * (accounting for this submitted item) if there are any
     * subscribers, else zero.
     *
     * <p>If the Executor for this publisher throws a
     * RejectedExecutionException (or any other RuntimeException or
     * Error) when attempting to asynchronously notify subscribers,
     * then this exception is rethrown, in which case not all
     * subscribers will have been issued this item.
     *
     * @param item the (non-null) item to publish
     * @return the estimated maximum lag among subscribers
     * @throws IllegalStateException if closed
     * @throws NullPointerException if item is null
     * @throws RejectedExecutionException if thrown by Executor
     */
    public int submit(T item) {
        return doOffer(item, Long.MAX_VALUE, null);
    }

    /**
     * Publishes the given item, if possible, to each current subscriber
     * by asynchronously invoking its {@link
     * Flow.Subscriber#onNext(Object) onNext} method. The item may be
     * dropped by one or more subscribers if resource limits are
     * exceeded, in which case the given handler (if non-null) is
     * invoked, and if it returns true, retried once.  Other calls to
     * methods in this class by other threads are blocked while the
     * handler is invoked.  Unless recovery is assured, options are
     * usually limited to logging the error and/or issuing an {@link
     * Flow.Subscriber#onError(Throwable) onError} signal to the
     * subscriber.
     *
     * <p>This method returns a status indicator: If negative, it
     * represents the (negative) number of drops (failed attempts to
     * issue the item to a subscriber). Otherwise it is an estimate of
     * the maximum lag (number of items submitted but not yet
     * consumed) among all current subscribers. This value is at least
     * one (accounting for this submitted item) if there are any
     * subscribers, else zero.
     *
     * <p>If the Executor for this publisher throws a
     * RejectedExecutionException (or any other RuntimeException or
     * Error) when attempting to asynchronously notify subscribers, or
     * the drop handler throws an exception when processing a dropped
     * item, then this exception is rethrown.
     *
     * @param item the (non-null) item to publish
     * @param onDrop if non-null, the handler invoked upon a drop to a
     * subscriber, with arguments of the subscriber and item; if it
     * returns true, an offer is re-attempted (once)
     * @return if negative, the (negative) number of drops; otherwise
     * an estimate of maximum lag
     * @throws IllegalStateException if closed
     * @throws NullPointerException if item is null
     * @throws RejectedExecutionException if thrown by Executor
     */
    public int offer(T item,
                     BiPredicate<Subscriber<? super T>, ? super T> onDrop) {
        return doOffer(item, 0L, onDrop);
    }

    /**
     * Publishes the given item, if possible, to each current subscriber
     * by asynchronously invoking its {@link
     * Flow.Subscriber#onNext(Object) onNext} method, blocking while
     * resources for any subscription are unavailable, up to the
     * specified timeout or until the caller thread is interrupted, at
     * which point the given handler (if non-null) is invoked, and if it
     * returns true, retried once. (The drop handler may distinguish
     * timeouts from interrupts by checking whether the current thread
     * is interrupted.)  Other calls to methods in this class by other
     * threads are blocked while the handler is invoked.  Unless
     * recovery is assured, options are usually limited to logging the
     * error and/or issuing an {@link Flow.Subscriber#onError(Throwable)
     * onError} signal to the subscriber.
     *
     * <p>This method returns a status indicator: If negative, it
     * represents the (negative) number of drops (failed attempts to
     * issue the item to a subscriber). Otherwise it is an estimate of
     * the maximum lag (number of items submitted but not yet
     * consumed) among all current subscribers. This value is at least
     * one (accounting for this submitted item) if there are any
     * subscribers, else zero.
     *
     * <p>If the Executor for this publisher throws a
     * RejectedExecutionException (or any other RuntimeException or
     * Error) when attempting to asynchronously notify subscribers, or
     * the drop handler throws an exception when processing a dropped
     * item, then this exception is rethrown.
     *
     * @param item the (non-null) item to publish
     * @param timeout how long to wait for resources for any subscriber
     * before giving up, in units of {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     * {@code timeout} parameter
     * @param onDrop if non-null, the handler invoked upon a drop to a
     * subscriber, with arguments of the subscriber and item; if it
     * returns true, an offer is re-attempted (once)
     * @return if negative, the (negative) number of drops; otherwise
     * an estimate of maximum lag
     * @throws IllegalStateException if closed
     * @throws NullPointerException if item is null
     * @throws RejectedExecutionException if thrown by Executor
     */
    public int offer(T item, long timeout, TimeUnit unit,
                     BiPredicate<Subscriber<? super T>, ? super T> onDrop) {
        long nanos = unit.toNanos(timeout);
        // distinguishes from untimed (only wrt interrupt policy)
        if (nanos == Long.MAX_VALUE) --nanos;
        return doOffer(item, nanos, onDrop);
    }

    /**
     * Unless already closed, issues {@link
     * Flow.Subscriber#onComplete() onComplete} signals to current
     * subscribers, and disallows subsequent attempts to publish.
     * Upon return, this method does <em>NOT</em> guarantee that all
     * subscribers have yet completed.
     */
    public void close() {
        ReentrantLock lock = this.lock;
        if (!closed) {
            BufferedSubscription<T> b;
            lock.lock();
            try {
                // no need to re-check closed here
                b = clients;
                clients = null;
                owner = null;
                closed = true;
            } finally {
                lock.unlock();
            }
            while (b != null) {
                BufferedSubscription<T> next = b.next;
                b.next = null;
                b.onComplete();
                b = next;
            }
        }
    }

    /**
     * Unless already closed, issues {@link
     * Flow.Subscriber#onError(Throwable) onError} signals to current
     * subscribers with the given error, and disallows subsequent
     * attempts to publish.  Future subscribers also receive the given
     * error. Upon return, this method does <em>NOT</em> guarantee
     * that all subscribers have yet completed.
     *
     * @param error the {@code onError} argument sent to subscribers
     * @throws NullPointerException if error is null
     */
    public void closeExceptionally(Throwable error) {
        if (error == null)
            throw new NullPointerException();
        ReentrantLock lock = this.lock;
        if (!closed) {
            BufferedSubscription<T> b;
            lock.lock();
            try {
                b = clients;
                if (!closed) {  // don't clobber racing close
                    closedException = error;
                    clients = null;
                    owner = null;
                    closed = true;
                }
            } finally {
                lock.unlock();
            }
            while (b != null) {
                BufferedSubscription<T> next = b.next;
                b.next = null;
                b.onError(error);
                b = next;
            }
        }
    }

    /**
     * Returns true if this publisher is not accepting submissions.
     *
     * @return true if closed
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Returns the exception associated with {@link
     * #closeExceptionally(Throwable) closeExceptionally}, or null if
     * not closed or if closed normally.
     *
     * @return the exception, or null if none
     */
    public Throwable getClosedException() {
        return closedException;
    }

    /**
     * Returns true if this publisher has any subscribers.
     *
     * @return true if this publisher has any subscribers
     */
    public boolean hasSubscribers() {
        boolean nonEmpty = false;
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            for (BufferedSubscription<T> b = clients; b != null;) {
                BufferedSubscription<T> next = b.next;
                if (b.isClosed()) {
                    b.next = null;
                    b = clients = next;
                }
                else {
                    nonEmpty = true;
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
        return nonEmpty;
    }

    /**
     * Returns the number of current subscribers.
     *
     * @return the number of current subscribers
     */
    public int getNumberOfSubscribers() {
        int n;
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            n = cleanAndCount();
        } finally {
            lock.unlock();
        }
        return n;
    }

    /**
     * Returns the Executor used for asynchronous delivery.
     *
     * @return the Executor used for asynchronous delivery
     */
    public Executor getExecutor() {
        return executor;
    }

    /**
     * Returns the maximum per-subscriber buffer capacity.
     *
     * @return the maximum per-subscriber buffer capacity
     */
    public int getMaxBufferCapacity() {
        return maxBufferCapacity;
    }

    /**
     * Returns a list of current subscribers for monitoring and
     * tracking purposes, not for invoking {@link Flow.Subscriber}
     * methods on the subscribers.
     *
     * @return list of current subscribers
     */
    public List<Subscriber<? super T>> getSubscribers() {
        ArrayList<Subscriber<? super T>> subs = new ArrayList<>();
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            BufferedSubscription<T> pred = null, next;
            for (BufferedSubscription<T> b = clients; b != null; b = next) {
                next = b.next;
                if (b.isClosed()) {
                    b.next = null;
                    if (pred == null)
                        clients = next;
                    else
                        pred.next = next;
                }
                else {
                    subs.add(b.subscriber);
                    pred = b;
                }
            }
        } finally {
            lock.unlock();
        }
        return subs;
    }

    /**
     * Returns true if the given Subscriber is currently subscribed.
     *
     * @param subscriber the subscriber
     * @return true if currently subscribed
     * @throws NullPointerException if subscriber is null
     */
    public boolean isSubscribed(Subscriber<? super T> subscriber) {
        if (subscriber == null) throw new NullPointerException();
        boolean subscribed = false;
        ReentrantLock lock = this.lock;
        if (!closed) {
            lock.lock();
            try {
                BufferedSubscription<T> pred = null, next;
                for (BufferedSubscription<T> b = clients; b != null; b = next) {
                    next = b.next;
                    if (b.isClosed()) {
                        b.next = null;
                        if (pred == null)
                            clients = next;
                        else
                            pred.next = next;
                    }
                    else if (subscribed = subscriber.equals(b.subscriber))
                        break;
                    else
                        pred = b;
                }
            } finally {
                lock.unlock();
            }
        }
        return subscribed;
    }

    /**
     * Returns an estimate of the minimum number of items requested
     * (via {@link Flow.Subscription#request(long) request}) but not
     * yet produced, among all current subscribers.
     *
     * @return the estimate, or zero if no subscribers
     */
    public long estimateMinimumDemand() {
        long min = Long.MAX_VALUE;
        boolean nonEmpty = false;
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            BufferedSubscription<T> pred = null, next;
            for (BufferedSubscription<T> b = clients; b != null; b = next) {
                int n; long d;
                next = b.next;
                if ((n = b.estimateLag()) < 0) {
                    b.next = null;
                    if (pred == null)
                        clients = next;
                    else
                        pred.next = next;
                }
                else {
                    if ((d = b.demand - n) < min)
                        min = d;
                    nonEmpty = true;
                    pred = b;
                }
            }
        } finally {
            lock.unlock();
        }
        return nonEmpty ? min : 0;
    }

    /**
     * Returns an estimate of the maximum number of items produced but
     * not yet consumed among all current subscribers.
     *
     * @return the estimate
     */
    public int estimateMaximumLag() {
        int max = 0;
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            BufferedSubscription<T> pred = null, next;
            for (BufferedSubscription<T> b = clients; b != null; b = next) {
                int n;
                next = b.next;
                if ((n = b.estimateLag()) < 0) {
                    b.next = null;
                    if (pred == null)
                        clients = next;
                    else
                        pred.next = next;
                }
                else {
                    if (n > max)
                        max = n;
                    pred = b;
                }
            }
        } finally {
            lock.unlock();
        }
        return max;
    }

    /**
     * Processes all published items using the given Consumer function.
     * Returns a CompletableFuture that is completed normally when this
     * publisher signals {@link Flow.Subscriber#onComplete()
     * onComplete}, or completed exceptionally upon any error, or an
     * exception is thrown by the Consumer, or the returned
     * CompletableFuture is cancelled, in which case no further items
     * are processed.
     *
     * @param consumer the function applied to each onNext item
     * @return a CompletableFuture that is completed normally
     * when the publisher signals onComplete, and exceptionally
     * upon any error or cancellation
     * @throws NullPointerException if consumer is null
     */
    public CompletableFuture<Void> consume(Consumer<? super T> consumer) {
        if (consumer == null)
            throw new NullPointerException();
        CompletableFuture<Void> status = new CompletableFuture<>();
        subscribe(new ConsumerSubscriber<T>(status, consumer));
        return status;
    }

    /** Subscriber for method consume */
    static final class ConsumerSubscriber<T> implements Subscriber<T> {
        final CompletableFuture<Void> status;
        final Consumer<? super T> consumer;
        Subscription subscription;
        ConsumerSubscriber(CompletableFuture<Void> status,
                           Consumer<? super T> consumer) {
            this.status = status; this.consumer = consumer;
        }
        public final void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            status.whenComplete((v, e) -> subscription.cancel());
            if (!status.isDone())
                subscription.request(Long.MAX_VALUE);
        }
        public final void onError(Throwable ex) {
            status.completeExceptionally(ex);
        }
        public final void onComplete() {
            status.complete(null);
        }
        public final void onNext(T item) {
            try {
                consumer.accept(item);
            } catch (Throwable ex) {
                subscription.cancel();
                status.completeExceptionally(ex);
            }
        }
    }

    /**
     * A task for consuming buffer items and signals, created and
     * executed whenever they become available. A task consumes as
     * many items/signals as possible before terminating, at which
     * point another task is created when needed. The dual Runnable
     * and ForkJoinTask declaration saves overhead when executed by
     * ForkJoinPools, without impacting other kinds of Executors.
     */
    @SuppressWarnings("serial")
    static final class ConsumerTask<T> extends ForkJoinTask<Void>
        implements Runnable, CompletableFuture.AsynchronousCompletionTask {
        final BufferedSubscription<T> consumer;
        ConsumerTask(BufferedSubscription<T> consumer) {
            this.consumer = consumer;
        }
        public final Void getRawResult() { return null; }
        public final void setRawResult(Void v) {}
        public final boolean exec() { consumer.consume(); return false; }
        public final void run() { consumer.consume(); }
    }

    /**
     * A resizable array-based ring buffer with integrated control to
     * start a consumer task whenever items are available.  The buffer
     * algorithm is specialized for the case of at most one concurrent
     * producer and consumer, and power of two buffer sizes. It relies
     * primarily on atomic operations (CAS or getAndSet) at the next
     * array slot to put or take an element, at the "tail" and "head"
     * indices written only by the producer and consumer respectively.
     *
     * We ensure internally that there is at most one active consumer
     * task at any given time. The publisher guarantees a single
     * producer via its lock. Sync among producers and consumers
     * relies on volatile fields "ctl", "demand", and "waiting" (along
     * with element access). Other variables are accessed in plain
     * mode, relying on outer ordering and exclusion, and/or enclosing
     * them within other volatile accesses. Some atomic operations are
     * avoided by tracking single threaded ownership by producers (in
     * the style of biased locking).
     *
     * Execution control and protocol state are managed using field
     * "ctl".  Methods to subscribe, close, request, and cancel set
     * ctl bits (mostly using atomic boolean method getAndBitwiseOr),
     * and ensure that a task is running. (The corresponding consumer
     * side actions are in method consume.)  To avoid starting a new
     * task on each action, ctl also includes a keep-alive bit
     * (ACTIVE) that is refreshed if needed on producer actions.
     * (Maintaining agreement about keep-alives requires most atomic
     * updates to be full SC/Volatile strength, which is still much
     * cheaper than using one task per item.)  Error signals
     * additionally null out items and/or fields to reduce termination
     * latency.  The cancel() method is supported by treating as ERROR
     * but suppressing onError signal.
     *
     * Support for blocking also exploits the fact that there is only
     * one possible waiter. ManagedBlocker-compatible control fields
     * are placed in this class itself rather than in wait-nodes.
     * Blocking control relies on the "waiting" and "waiter"
     * fields. Producers set them before trying to block. Signalling
     * unparks and clears fields. If the producer and/or consumer are
     * using a ForkJoinPool, the producer attempts to help run
     * consumer tasks via ForkJoinPool.helpAsyncBlocker before
     * blocking.
     *
     * Usages of this class may encounter any of several forms of
     * memory contention. We try to ameliorate across them without
     * unduly impacting footprints in low-contention usages where it
     * isn't needed. Buffer arrays start out small and grow only as
     * needed.  The class uses @Contended and heuristic field
     * declaration ordering to reduce false-sharing memory contention
     * across instances of BufferedSubscription (as in, multiple
     * subscribers per publisher).  We additionally segregate some
     * fields that would otherwise nearly always encounter cache line
     * contention among producers and consumers. To reduce contention
     * across time (vs space), consumers only periodically update
     * other fields (see method takeItems), at the expense of possibly
     * staler reporting of lags and demand (bounded at 12.5% == 1/8
     * capacity) and possibly more atomic operations.
     *
     * Other forms of imbalance and slowdowns can occur during startup
     * when producer and consumer methods are compiled and/or memory
     * is allocated at different rates.  This is ameliorated by
     * artificially subdividing some consumer methods, including
     * isolation of all subscriber callbacks.  This code also includes
     * typical power-of-two array screening idioms to avoid compilers
     * generating traps, along with the usual SSA-based inline
     * assignment coding style. Also, all methods and fields have
     * default visibility to simplify usage by callers.
     */
    @SuppressWarnings("serial")
    @jdk.internal.vm.annotation.Contended
    static final class BufferedSubscription<T>
        implements Subscription, ForkJoinPool.ManagedBlocker {
        long timeout;                      // Long.MAX_VALUE if untimed wait
        int head;                          // next position to take
        int tail;                          // next position to put
        final int maxCapacity;             // max buffer size
        volatile int ctl;                  // atomic run state flags
        Object[] array;                    // buffer
        final Subscriber<? super T> subscriber;
        final BiConsumer<? super Subscriber<? super T>, ? super Throwable> onNextHandler;
        Executor executor;                 // null on error
        Thread waiter;                     // blocked producer thread
        Throwable pendingError;            // holds until onError issued
        BufferedSubscription<T> next;      // used only by publisher
        BufferedSubscription<T> nextRetry; // used only by publisher

        @jdk.internal.vm.annotation.Contended("c") // segregate
        volatile long demand;              // # unfilled requests
        @jdk.internal.vm.annotation.Contended("c")
        volatile int waiting;              // nonzero if producer blocked

        // ctl bit values
        static final int CLOSED   = 0x01;  // if set, other bits ignored
        static final int ACTIVE   = 0x02;  // keep-alive for consumer task
        static final int REQS     = 0x04;  // (possibly) nonzero demand
        static final int ERROR    = 0x08;  // issues onError when noticed
        static final int COMPLETE = 0x10;  // issues onComplete when done
        static final int RUN      = 0x20;  // task is or will be running
        static final int OPEN     = 0x40;  // true after subscribe

        static final long INTERRUPTED = -1L; // timeout vs interrupt sentinel

        BufferedSubscription(Subscriber<? super T> subscriber,
                             Executor executor,
                             BiConsumer<? super Subscriber<? super T>,
                             ? super Throwable> onNextHandler,
                             Object[] array,
                             int maxBufferCapacity) {
            this.subscriber = subscriber;
            this.executor = executor;
            this.onNextHandler = onNextHandler;
            this.array = array;
            this.maxCapacity = maxBufferCapacity;
        }

        // Wrappers for some VarHandle methods

        final boolean weakCasCtl(int cmp, int val) {
            return CTL.weakCompareAndSet(this, cmp, val);
        }

        final int getAndBitwiseOrCtl(int bits) {
            return (int)CTL.getAndBitwiseOr(this, bits);
        }

        final long subtractDemand(int k) {
            long n = (long)(-k);
            return n + (long)DEMAND.getAndAdd(this, n);
        }

        final boolean casDemand(long cmp, long val) {
            return DEMAND.compareAndSet(this, cmp, val);
        }

        // Utilities used by SubmissionPublisher

        /**
         * Returns true if closed (consumer task may still be running).
         */
        final boolean isClosed() {
            return (ctl & CLOSED) != 0;
        }

        /**
         * Returns estimated number of buffered items, or negative if
         * closed.
         */
        final int estimateLag() {
            int c = ctl, n = tail - head;
            return ((c & CLOSED) != 0) ? -1 : (n < 0) ? 0 : n;
        }

        // Methods for submitting items

        /**
         * Tries to add item and start consumer task if necessary.
         * @return negative if closed, 0 if saturated, else estimated lag
         */
        final int offer(T item, boolean unowned) {
            Object[] a;
            int stat = 0, cap = ((a = array) == null) ? 0 : a.length;
            int t = tail, i = t & (cap - 1), n = t + 1 - head;
            if (cap > 0) {
                boolean added;
                if (n >= cap && cap < maxCapacity) // resize
                    added = growAndOffer(item, a, t);
                else if (n >= cap || unowned)      // need volatile CAS
                    added = QA.compareAndSet(a, i, null, item);
                else {                             // can use release mode
                    QA.setRelease(a, i, item);
                    added = true;
                }
                if (added) {
                    tail = t + 1;
                    stat = n;
                }
            }
            return startOnOffer(stat);
        }

        /**
         * Tries to expand buffer and add item, returning true on
         * success. Currently fails only if out of memory.
         */
        final boolean growAndOffer(T item, Object[] a, int t) {
            int cap = 0, newCap = 0;
            Object[] newArray = null;
            if (a != null && (cap = a.length) > 0 && (newCap = cap << 1) > 0) {
                try {
                    newArray = new Object[newCap];
                } catch (OutOfMemoryError ex) {
                }
            }
            if (newArray == null)
                return false;
            else {                                // take and move items
                int newMask = newCap - 1;
                newArray[t-- & newMask] = item;
                for (int mask = cap - 1, k = mask; k >= 0; --k) {
                    Object x = QA.getAndSet(a, t & mask, null);
                    if (x == null)
                        break;                    // already consumed
                    else
                        newArray[t-- & newMask] = x;
                }
                array = newArray;
                VarHandle.releaseFence();         // release array and slots
                return true;
            }
        }

        /**
         * Version of offer for retries (no resize or bias)
         */
        final int retryOffer(T item) {
            Object[] a;
            int stat = 0, t = tail, h = head, cap;
            if ((a = array) != null && (cap = a.length) > 0 &&
                QA.compareAndSet(a, (cap - 1) & t, null, item))
                stat = (tail = t + 1) - h;
            return startOnOffer(stat);
        }

        /**
         * Tries to start consumer task after offer.
         * @return negative if now closed, else argument
         */
        final int startOnOffer(int stat) {
            int c; // start or keep alive if requests exist and not active
            if (((c = ctl) & (REQS | ACTIVE)) == REQS &&
                ((c = getAndBitwiseOrCtl(RUN | ACTIVE)) & (RUN | CLOSED)) == 0)
                tryStart();
            else if ((c & CLOSED) != 0)
                stat = -1;
            return stat;
        }

        /**
         * Tries to start consumer task. Sets error state on failure.
         */
        final void tryStart() {
            try {
                Executor e;
                ConsumerTask<T> task = new ConsumerTask<T>(this);
                if ((e = executor) != null)   // skip if disabled on error
                    e.execute(task);
            } catch (RuntimeException | Error ex) {
                getAndBitwiseOrCtl(ERROR | CLOSED);
                throw ex;
            }
        }

        // Signals to consumer tasks

        /**
         * Sets the given control bits, starting task if not running or closed.
         * @param bits state bits, assumed to include RUN but not CLOSED
         */
        final void startOnSignal(int bits) {
            if ((ctl & bits) != bits &&
                (getAndBitwiseOrCtl(bits) & (RUN | CLOSED)) == 0)
                tryStart();
        }

        final void onSubscribe() {
            startOnSignal(RUN | ACTIVE);
        }

        final void onComplete() {
            startOnSignal(RUN | ACTIVE | COMPLETE);
        }

        final void onError(Throwable ex) {
            int c; Object[] a;      // to null out buffer on async error
            if (ex != null)
                pendingError = ex;  // races are OK
            if (((c = getAndBitwiseOrCtl(ERROR | RUN | ACTIVE)) & CLOSED) == 0) {
                if ((c & RUN) == 0)
                    tryStart();
                else if ((a = array) != null)
                    Arrays.fill(a, null);
            }
        }

        public final void cancel() {
            onError(null);
        }

        public final void request(long n) {
            if (n > 0L) {
                for (;;) {
                    long p = demand, d = p + n;  // saturate
                    if (casDemand(p, d < p ? Long.MAX_VALUE : d))
                        break;
                }
                startOnSignal(RUN | ACTIVE | REQS);
            }
            else
                onError(new IllegalArgumentException(
                            "non-positive subscription request"));
        }

        // Consumer task actions

        /**
         * Consumer loop, called from ConsumerTask, or indirectly when
         * helping during submit.
         */
        final void consume() {
            Subscriber<? super T> s;
            if ((s = subscriber) != null) {          // hoist checks
                subscribeOnOpen(s);
                long d = demand;
                for (int h = head, t = tail;;) {
                    int c, taken; boolean empty;
                    if (((c = ctl) & ERROR) != 0) {
                        closeOnError(s, null);
                        break;
                    }
                    else if ((taken = takeItems(s, d, h)) > 0) {
                        head = h += taken;
                        d = subtractDemand(taken);
                    }
                    else if ((d = demand) == 0L && (c & REQS) != 0)
                        weakCasCtl(c, c & ~REQS);    // exhausted demand
                    else if (d != 0L && (c & REQS) == 0)
                        weakCasCtl(c, c | REQS);     // new demand
                    else if (t == (t = tail)) {      // stability check
                        if ((empty = (t == h)) && (c & COMPLETE) != 0) {
                            closeOnComplete(s);      // end of stream
                            break;
                        }
                        else if (empty || d == 0L) {
                            int bit = ((c & ACTIVE) != 0) ? ACTIVE : RUN;
                            if (weakCasCtl(c, c & ~bit) && bit == RUN)
                                break;               // un-keep-alive or exit
                        }
                    }
                }
            }
        }

        /**
         * Consumes some items until unavailable or bound or error.
         *
         * @param s subscriber
         * @param d current demand
         * @param h current head
         * @return number taken
         */
        final int takeItems(Subscriber<? super T> s, long d, int h) {
            Object[] a;
            int k = 0, cap;
            if ((a = array) != null && (cap = a.length) > 0) {
                int m = cap - 1, b = (m >>> 3) + 1; // min(1, cap/8)
                int n = (d < (long)b) ? (int)d : b;
                for (; k < n; ++h, ++k) {
                    Object x = QA.getAndSet(a, h & m, null);
                    if (waiting != 0)
                        signalWaiter();
                    if (x == null)
                        break;
                    else if (!consumeNext(s, x))
                        break;
                }
            }
            return k;
        }

        final boolean consumeNext(Subscriber<? super T> s, Object x) {
            try {
                @SuppressWarnings("unchecked") T y = (T) x;
                if (s != null)
                    s.onNext(y);
                return true;
            } catch (Throwable ex) {
                handleOnNext(s, ex);
                return false;
            }
        }

        /**
         * Processes exception in Subscriber.onNext.
         */
        final void handleOnNext(Subscriber<? super T> s, Throwable ex) {
            BiConsumer<? super Subscriber<? super T>, ? super Throwable> h;
            try {
                if ((h = onNextHandler) != null)
                    h.accept(s, ex);
            } catch (Throwable ignore) {
            }
            closeOnError(s, ex);
        }

        /**
         * Issues subscriber.onSubscribe if this is first signal.
         */
        final void subscribeOnOpen(Subscriber<? super T> s) {
            if ((ctl & OPEN) == 0 && (getAndBitwiseOrCtl(OPEN) & OPEN) == 0)
                consumeSubscribe(s);
        }

        final void consumeSubscribe(Subscriber<? super T> s) {
            try {
                if (s != null) // ignore if disabled
                    s.onSubscribe(this);
            } catch (Throwable ex) {
                closeOnError(s, ex);
            }
        }

        /**
         * Issues subscriber.onComplete unless already closed.
         */
        final void closeOnComplete(Subscriber<? super T> s) {
            if ((getAndBitwiseOrCtl(CLOSED) & CLOSED) == 0)
                consumeComplete(s);
        }

        final void consumeComplete(Subscriber<? super T> s) {
            try {
                if (s != null)
                    s.onComplete();
            } catch (Throwable ignore) {
            }
        }

        /**
         * Issues subscriber.onError, and unblocks producer if needed.
         */
        final void closeOnError(Subscriber<? super T> s, Throwable ex) {
            if ((getAndBitwiseOrCtl(ERROR | CLOSED) & CLOSED) == 0) {
                if (ex == null)
                    ex = pendingError;
                pendingError = null;  // detach
                executor = null;      // suppress racing start calls
                signalWaiter();
                consumeError(s, ex);
            }
        }

        final void consumeError(Subscriber<? super T> s, Throwable ex) {
            try {
                if (ex != null && s != null)
                    s.onError(ex);
            } catch (Throwable ignore) {
            }
        }

        // Blocking support

        /**
         * Unblocks waiting producer.
         */
        final void signalWaiter() {
            Thread w;
            waiting = 0;
            if ((w = waiter) != null)
                LockSupport.unpark(w);
        }

        /**
         * Returns true if closed or space available.
         * For ManagedBlocker.
         */
        public final boolean isReleasable() {
            Object[] a; int cap;
            return ((ctl & CLOSED) != 0 ||
                    ((a = array) != null && (cap = a.length) > 0 &&
                     QA.getAcquire(a, (cap - 1) & tail) == null));
        }

        /**
         * Helps or blocks until timeout, closed, or space available.
         */
        final void awaitSpace(long nanos) {
            if (!isReleasable()) {
                ForkJoinPool.helpAsyncBlocker(executor, this);
                if (!isReleasable()) {
                    timeout = nanos;
                    try {
                        ForkJoinPool.managedBlock(this);
                    } catch (InterruptedException ie) {
                        timeout = INTERRUPTED;
                    }
                    if (timeout == INTERRUPTED)
                        Thread.currentThread().interrupt();
                }
            }
        }

        /**
         * Blocks until closed, space available or timeout.
         * For ManagedBlocker.
         */
        public final boolean block() {
            long nanos = timeout;
            boolean timed = (nanos < Long.MAX_VALUE);
            long deadline = timed ? System.nanoTime() + nanos : 0L;
            while (!isReleasable()) {
                if (Thread.interrupted()) {
                    timeout = INTERRUPTED;
                    if (timed)
                        break;
                }
                else if (timed && (nanos = deadline - System.nanoTime()) <= 0L)
                    break;
                else if (waiter == null)
                    waiter = Thread.currentThread();
                else if (waiting == 0)
                    waiting = 1;
                else if (timed)
                    LockSupport.parkNanos(this, nanos);
                else
                    LockSupport.park(this);
            }
            waiter = null;
            waiting = 0;
            return true;
        }

        // VarHandle mechanics
        static final VarHandle CTL;
        static final VarHandle DEMAND;
        static final VarHandle QA;

        static {
            try {
                MethodHandles.Lookup l = MethodHandles.lookup();
                CTL = l.findVarHandle(BufferedSubscription.class, "ctl",
                                      int.class);
                DEMAND = l.findVarHandle(BufferedSubscription.class, "demand",
                                         long.class);
                QA = MethodHandles.arrayElementVarHandle(Object[].class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }

            // Reduce the risk of rare disastrous classloading in first call to
            // LockSupport.park: https://bugs.openjdk.java.net/browse/JDK-8074773
            Class<?> ensureLoaded = LockSupport.class;
        }
    }
}
