/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Iterator;
import java.util.concurrent.Flow;
import jdk.internal.net.http.common.Demand;
import jdk.internal.net.http.common.SequentialScheduler;

/**
 * A Publisher that publishes items obtained from the given Iterable. Each new
 * subscription gets a new Iterator.
 */
class PullPublisher<T> implements Flow.Publisher<T> {

    // Only one of `iterable` and `throwable` can be non-null. throwable is
    // non-null when an error has been encountered, by the creator of
    // PullPublisher, while subscribing the subscriber, but before subscribe has
    // completed.
    private final Iterable<T> iterable;
    private final Throwable throwable;

    PullPublisher(Iterable<T> iterable, Throwable throwable) {
        this.iterable = iterable;
        this.throwable = throwable;
    }

    PullPublisher(Iterable<T> iterable) {
        this(iterable, null);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        Subscription sub;
        if (throwable != null) {
            assert iterable == null : "non-null iterable: " + iterable;
            sub = new Subscription(subscriber, null, throwable);
        } else {
            assert throwable == null : "non-null exception: " + throwable;
            sub = new Subscription(subscriber, iterable.iterator(), null);
        }
        subscriber.onSubscribe(sub);

        if (throwable != null) {
            sub.pullScheduler.runOrSchedule();
        }
    }

    private class Subscription implements Flow.Subscription {

        private final Flow.Subscriber<? super T> subscriber;
        private final Iterator<T> iter;
        private volatile boolean completed;
        private volatile boolean cancelled;
        private volatile Throwable error;
        final SequentialScheduler pullScheduler = new SequentialScheduler(new PullTask());
        private final Demand demand = new Demand();

        Subscription(Flow.Subscriber<? super T> subscriber,
                     Iterator<T> iter,
                     Throwable throwable) {
            this.subscriber = subscriber;
            this.iter = iter;
            this.error = throwable;
        }

        final class PullTask extends SequentialScheduler.CompleteRestartableTask {
            @Override
            protected void run() {
                if (completed || cancelled) {
                    return;
                }

                Throwable t = error;
                if (t != null) {
                    completed = true;
                    pullScheduler.stop();
                    subscriber.onError(t);
                    return;
                }

                while (demand.tryDecrement() && !cancelled) {
                    T next;
                    try {
                        if (!iter.hasNext()) {
                            break;
                        }
                        next = iter.next();
                    } catch (Throwable t1) {
                        completed = true;
                        pullScheduler.stop();
                        subscriber.onError(t1);
                        return;
                    }
                    subscriber.onNext(next);
                }
                if (!iter.hasNext() && !cancelled) {
                    completed = true;
                    pullScheduler.stop();
                    subscriber.onComplete();
                }
            }
        }

        @Override
        public void request(long n) {
            if (cancelled)
                return;  // no-op

            if (n <= 0) {
                error = new IllegalArgumentException("non-positive subscription request: " + n);
            } else {
                demand.increase(n);
            }
            pullScheduler.runOrSchedule();
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
