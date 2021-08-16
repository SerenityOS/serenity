/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package org.reactivestreams.example.unicast;

import org.reactivestreams.*;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A synchronous implementation of the {@link Publisher} that can
 * be subscribed to multiple times and each individual subscription
 * will receive range of monotonically increasing integer values on demand.
 */
public final class RangePublisher implements Publisher<Integer> {

    /** The starting value of the range. */
    final int start;

    /** The number of items to emit. */
    final int count;

    /**
     * Constructs a RangePublisher instance with the given start and count values
     * that yields a sequence of [start, start + count).
     * @param start the starting value of the range
     * @param count the number of items to emit
     */
    public RangePublisher(int start, int count) {
        this.start = start;
        this.count = count;
    }

    @Override
    public void subscribe(Subscriber<? super Integer> subscriber) {
        // As per rule 1.11, we have decided to support multiple subscribers
        // in a unicast configuration for this `Publisher` implementation.

        // As per rule 1.09, we need to throw a `java.lang.NullPointerException`
        // if the `Subscriber` is `null`
        if (subscriber == null) throw null;

        // As per 2.13, this method must return normally (i.e. not throw).
        try {
            subscriber.onSubscribe(new RangeSubscription(subscriber, start, start + count));
        } catch (Throwable ex) {
            new IllegalStateException(subscriber + " violated the Reactive Streams rule 2.13 " +
                    "by throwing an exception from onSubscribe.", ex)
                    // When onSubscribe fails this way, we don't know what state the
                    // subscriber is thus calling onError may cause more crashes.
                    .printStackTrace();
        }
    }

    /**
     * A Subscription implementation that holds the current downstream
     * requested amount and responds to the downstream's request() and
     * cancel() calls.
     */
    static final class RangeSubscription
            // We are using this `AtomicLong` to make sure that this `Subscription`
            // doesn't run concurrently with itself, which would violate rule 1.3
            // among others (no concurrent notifications).
            // The atomic transition from 0L to N > 0L will ensure this.
            extends AtomicLong implements Subscription {

        private static final long serialVersionUID = -9000845542177067735L;

        /** The Subscriber we are emitting integer values to. */
        final Subscriber<? super Integer> downstream;

        /** The end index (exclusive). */
        final int end;

        /**
         * The current index and within the [start, start + count) range that
         * will be emitted as downstream.onNext().
         */
        int index;

        /**
         * Indicates the emission should stop.
         */
        volatile boolean cancelled;

        /**
         * Holds onto the IllegalArgumentException (containing the offending stacktrace)
         * indicating there was a non-positive request() call from the downstream.
         */
        volatile Throwable invalidRequest;

        /**
         * Constructs a stateful RangeSubscription that emits signals to the given
         * downstream from an integer range of [start, end).
         * @param downstream the Subscriber receiving the integer values and the completion signal.
         * @param start the first integer value emitted, start of the range
         * @param end the end of the range, exclusive
         */
        RangeSubscription(Subscriber<? super Integer> downstream, int start, int end) {
            this.downstream = downstream;
            this.index = start;
            this.end = end;
        }

        // This method will register inbound demand from our `Subscriber` and
        // validate it against rule 3.9 and rule 3.17
        @Override
        public void request(long n) {
            // Non-positive requests should be honored with IllegalArgumentException
            if (n <= 0L) {
                invalidRequest = new IllegalArgumentException("§3.9: non-positive requests are not allowed!");
                n = 1;
            }
            // Downstream requests are cumulative and may come from any thread
            for (;;) {
                long requested = get();
                long update = requested + n;
                // As governed by rule 3.17, when demand overflows `Long.MAX_VALUE`
                // we treat the signalled demand as "effectively unbounded"
                if (update < 0L) {
                    update = Long.MAX_VALUE;
                }
                // atomically update the current requested amount
                if (compareAndSet(requested, update)) {
                    // if there was no prior request amount, we start the emission loop
                    if (requested == 0L) {
                        emit(update);
                    }
                    break;
                }
            }
        }

        // This handles cancellation requests, and is idempotent, thread-safe and not
        // synchronously performing heavy computations as specified in rule 3.5
        @Override
        public void cancel() {
            // Indicate to the emission loop it should stop.
            cancelled = true;
        }

        void emit(long currentRequested) {
            // Load fields to avoid re-reading them from memory due to volatile accesses in the loop.
            Subscriber<? super Integer> downstream = this.downstream;
            int index = this.index;
            int end = this.end;
            int emitted = 0;

            try {
                for (; ; ) {
                    // Check if there was an invalid request and then report its exception
                    // as mandated by rule 3.9. The stacktrace in it should
                    // help locate the faulty logic in the Subscriber.
                    Throwable invalidRequest = this.invalidRequest;
                    if (invalidRequest != null) {
                        // When we signal onError, the subscription must be considered as cancelled, as per rule 1.6
                        cancelled = true;

                        downstream.onError(invalidRequest);
                        return;
                    }

                    // Loop while the index hasn't reached the end and we haven't
                    // emitted all that's been requested
                    while (index != end && emitted != currentRequested) {
                        // to make sure that we follow rule 1.8, 3.6 and 3.7
                        // We stop if cancellation was requested.
                        if (cancelled) {
                            return;
                        }

                        downstream.onNext(index);

                        // Increment the index for the next possible emission.
                        index++;
                        // Increment the emitted count to prevent overflowing the downstream.
                        emitted++;
                    }

                    // If the index reached the end, we complete the downstream.
                    if (index == end) {
                        // to make sure that we follow rule 1.8, 3.6 and 3.7
                        // Unless cancellation was requested by the last onNext.
                        if (!cancelled) {
                            // We need to consider this `Subscription` as cancelled as per rule 1.6
                            // Note, however, that this state is not observable from the outside
                            // world and since we leave the loop with requested > 0L, any
                            // further request() will never trigger the loop.
                            cancelled = true;

                            downstream.onComplete();
                        }
                        return;
                    }

                    // Did the requested amount change while we were looping?
                    long freshRequested = get();
                    if (freshRequested == currentRequested) {
                        // Save where the loop has left off: the next value to be emitted
                        this.index = index;
                        // Atomically subtract the previously requested (also emitted) amount
                        currentRequested = addAndGet(-currentRequested);
                        // If there was no new request in between get() and addAndGet(), we simply quit
                        // The next 0 to N transition in request() will trigger the next emission loop.
                        if (currentRequested == 0L) {
                            break;
                        }
                        // Looks like there were more async requests, reset the emitted count and continue.
                        emitted = 0;
                    } else {
                        // Yes, avoid the atomic subtraction and resume.
                        // emitted != currentRequest in this case and index
                        // still points to the next value to be emitted
                        currentRequested = freshRequested;
                    }
                }
            } catch (Throwable ex) {
                // We can only get here if `onNext`, `onError` or `onComplete` threw, and they
                // are not allowed to according to 2.13, so we can only cancel and log here.
                // If `onError` throws an exception, this is a spec violation according to rule 1.9,
                // and all we can do is to log it.

                // Make sure that we are cancelled, since we cannot do anything else
                // since the `Subscriber` is faulty.
                cancelled = true;

                // We can't report the failure to onError as the Subscriber is unreliable.
                (new IllegalStateException(downstream + " violated the Reactive Streams rule 2.13 by " +
                        "throwing an exception from onNext, onError or onComplete.", ex))
                        .printStackTrace();
            }
        }
    }
}
