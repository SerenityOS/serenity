/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http;

import jdk.internal.net.http.common.Demand;
import jdk.internal.net.http.common.FlowTube;
import jdk.internal.net.http.common.SSLTube;
import jdk.internal.net.http.common.SequentialScheduler;
import jdk.internal.net.http.common.Utils;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Test
public class SSLEchoTubeTest extends AbstractSSLTubeTest {

    @Test
    public void runWithEchoServer() throws IOException {
        ExecutorService sslExecutor = Executors.newCachedThreadPool();

        /* Start of wiring */
        /* Emulates an echo server */
        FlowTube server = crossOverEchoServer(sslExecutor);

        run(server, sslExecutor, allBytesReceived);
    }

    /**
     * Creates a cross-over FlowTube than can be plugged into a client-side
     * SSLTube (in place of the SSLLoopbackSubscriber).
     * Note that the only method that can be called on the return tube
     * is connectFlows(). Calling any other method will trigger an
     * InternalError.
     * @param sslExecutor an executor
     * @return a cross-over FlowTube connected to an EchoTube.
     * @throws IOException
     */
    private FlowTube crossOverEchoServer(Executor sslExecutor) throws IOException {
        LateBindingTube crossOver = new LateBindingTube();
        FlowTube server = new SSLTube(createSSLEngine(false),
                                      sslExecutor,
                                      crossOver);
        EchoTube echo = new EchoTube(6);
        server.connectFlows(FlowTube.asTubePublisher(echo), FlowTube.asTubeSubscriber(echo));

        return new CrossOverTube(crossOver);
    }

    /**
     * A cross-over FlowTube that makes it possible to reverse the direction
     * of flows. The typical usage is to connect an two opposite SSLTube,
     * one encrypting, one decrypting, to e.g. an EchoTube, with the help
     * of a LateBindingTube:
     * {@code
     * client app => SSLTube => CrossOverTube <= LateBindingTube <= SSLTube <= EchoTube
     * }
     * <p>
     * Note that the only method that can be called on the CrossOverTube is
     * connectFlows(). Calling any other method will cause an InternalError to
     * be thrown.
     * Also connectFlows() can be called only once.
     */
    private static final class CrossOverTube implements FlowTube {
        final LateBindingTube tube;
        CrossOverTube(LateBindingTube tube) {
            this.tube = tube;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
            throw newInternalError();
        }

        @Override
        public void connectFlows(TubePublisher writePublisher, TubeSubscriber readSubscriber) {
            tube.start(writePublisher, readSubscriber);
        }

        @Override
        public boolean isFinished() {
            return tube.isFinished();
        }

        Error newInternalError() {
            InternalError error = new InternalError();
            error.printStackTrace(System.out);
            return error;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            throw newInternalError();
        }

        @Override
        public void onError(Throwable throwable) {
            throw newInternalError();
        }

        @Override
        public void onComplete() {
            throw newInternalError();
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            throw newInternalError();
        }
    }

    /**
     * A late binding tube that makes it possible to create an
     * SSLTube before the right-hand-side tube has been created.
     * The typical usage is to make it possible to connect two
     * opposite SSLTube (one encrypting, one decrypting) through a
     * CrossOverTube:
     * {@code
     * client app => SSLTube => CrossOverTube <= LateBindingTube <= SSLTube <= EchoTube
     * }
     * <p>
     * Note that this class only supports a single call to start(): it cannot be
     * subscribed more than once from its left-hand-side (the cross over tube side).
     */
    private static class LateBindingTube implements FlowTube {

        final CompletableFuture<Flow.Publisher<List<ByteBuffer>>> futurePublisher
                = new CompletableFuture<>();
        final ConcurrentLinkedQueue<Consumer<Flow.Subscriber<? super List<ByteBuffer>>>> queue
                = new ConcurrentLinkedQueue<>();
        AtomicReference<Flow.Subscriber<? super List<ByteBuffer>>> subscriberRef = new AtomicReference<>();
        SequentialScheduler scheduler = SequentialScheduler.lockingScheduler(this::loop);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        private volatile boolean finished;
        private volatile boolean completed;


        public void start(Flow.Publisher<List<ByteBuffer>> publisher,
                          Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
            subscriberRef.set(subscriber);
            futurePublisher.complete(publisher);
            scheduler.runOrSchedule();
        }

        @Override
        public boolean isFinished() {
            return finished;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
            futurePublisher.thenAccept((p) -> p.subscribe(subscriber));
            scheduler.runOrSchedule();
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            queue.add((s) -> s.onSubscribe(subscription));
            scheduler.runOrSchedule();
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            queue.add((s) -> s.onNext(item));
            scheduler.runOrSchedule();
        }

        @Override
        public void onError(Throwable throwable) {
            System.out.println("LateBindingTube onError");
            throwable.printStackTrace(System.out);
            queue.add((s) -> {
                errorRef.compareAndSet(null, throwable);
                try {
                    System.out.println("LateBindingTube subscriber onError: " + throwable);
                    s.onError(errorRef.get());
                } finally {
                    finished = true;
                    System.out.println("LateBindingTube finished");
                }
            });
            scheduler.runOrSchedule();
        }

        @Override
        public void onComplete() {
            System.out.println("LateBindingTube completing");
            queue.add((s) -> {
                completed = true;
                try {
                    System.out.println("LateBindingTube complete subscriber");
                    s.onComplete();
                } finally {
                    finished = true;
                    System.out.println("LateBindingTube finished");
                }
            });
            scheduler.runOrSchedule();
        }

        private void loop() {
            if (finished) {
                scheduler.stop();
                return;
            }
            Flow.Subscriber<? super List<ByteBuffer>> subscriber = subscriberRef.get();
            if (subscriber == null) return;
            try {
                Consumer<Flow.Subscriber<? super List<ByteBuffer>>> s;
                while ((s = queue.poll()) != null) {
                    s.accept(subscriber);
                }
            } catch (Throwable t) {
                if (errorRef.compareAndSet(null, t)) {
                    onError(t);
                }
            }
        }
    }

    /**
     * An echo tube that just echoes back whatever bytes it receives.
     * This cannot be plugged to the right-hand-side of an SSLTube
     * since handshake data cannot be simply echoed back, and
     * application data most likely also need to be decrypted and
     * re-encrypted.
     */
    private static final class EchoTube implements FlowTube {

        private final static Object EOF = new Object();
        private final Executor executor = Executors.newSingleThreadExecutor();

        private final Queue<Object> queue = new ConcurrentLinkedQueue<>();
        private final int maxQueueSize;
        private final SequentialScheduler processingScheduler =
                new SequentialScheduler(createProcessingTask());

        /* Writing into this tube */
        private volatile long requested;
        private Flow.Subscription subscription;

        /* Reading from this tube */
        private final Demand demand = new Demand();
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private Flow.Subscriber<? super List<ByteBuffer>> subscriber;

        private EchoTube(int maxBufferSize) {
            if (maxBufferSize < 1)
                throw new IllegalArgumentException();
            this.maxQueueSize = maxBufferSize;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
            this.subscriber = subscriber;
            System.out.println("EchoTube got subscriber: " + subscriber);
            this.subscriber.onSubscribe(new InternalSubscription());
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            System.out.println("EchoTube request: " + maxQueueSize);
            (this.subscription = subscription).request(requested = maxQueueSize);
        }

        private void requestMore() {
            Flow.Subscription s = subscription;
            if (s == null || cancelled.get()) return;
            long unfulfilled = queue.size() + --requested;
            if (unfulfilled <= maxQueueSize/2) {
                long req = maxQueueSize - unfulfilled;
                requested += req;
                s.request(req);
                System.out.printf("EchoTube request: %s [requested:%s, queue:%s, unfulfilled:%s]%n",
                        req, requested-req, queue.size(), unfulfilled );
            }
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            System.out.printf("EchoTube add %s [requested:%s, queue:%s]%n",
                    Utils.remaining(item), requested, queue.size());
            queue.add(item);
            processingScheduler.runOrSchedule(executor);
        }

        @Override
        public void onError(Throwable throwable) {
            System.out.println("EchoTube add " + throwable);
            queue.add(throwable);
            processingScheduler.runOrSchedule(executor);
        }

        @Override
        public void onComplete() {
            System.out.println("EchoTube add EOF");
            queue.add(EOF);
            processingScheduler.runOrSchedule(executor);
        }

        @Override
        public boolean isFinished() {
            return cancelled.get();
        }

        private class InternalSubscription implements Flow.Subscription {

            @Override
            public void request(long n) {
                System.out.println("EchoTube got request: " + n);
                if (n <= 0) {
                    throw new InternalError();
                }
                if (demand.increase(n)) {
                    processingScheduler.runOrSchedule(executor);
                }
            }

            @Override
            public void cancel() {
                queue.add(EOF);
            }
        }

        @Override
        public String toString() {
            return "EchoTube";
        }

        int transmitted = 0;
        private SequentialScheduler.RestartableTask createProcessingTask() {
            return new SequentialScheduler.CompleteRestartableTask() {

                @Override
                protected void run() {
                    try {
                        while (!cancelled.get()) {
                            Object item = queue.peek();
                            if (item == null) {
                                System.out.printf("EchoTube: queue empty, requested=%s, demand=%s, transmitted=%s%n",
                                        requested, demand.get(), transmitted);
                                requestMore();
                                return;
                            }
                            try {
                                System.out.printf("EchoTube processing item, requested=%s, demand=%s, transmitted=%s%n",
                                        requested, demand.get(), transmitted);
                                if (item instanceof List) {
                                    if (!demand.tryDecrement()) {
                                        System.out.println("EchoTube no demand");
                                        return;
                                    }
                                    @SuppressWarnings("unchecked")
                                    List<ByteBuffer> bytes = (List<ByteBuffer>) item;
                                    Object removed = queue.remove();
                                    assert removed == item;
                                    System.out.println("EchoTube processing "
                                            + Utils.remaining(bytes));
                                    transmitted++;
                                    subscriber.onNext(bytes);
                                    requestMore();
                                } else if (item instanceof Throwable) {
                                    cancelled.set(true);
                                    Object removed = queue.remove();
                                    assert removed == item;
                                    System.out.println("EchoTube processing " + item);
                                    subscriber.onError((Throwable) item);
                                } else if (item == EOF) {
                                    cancelled.set(true);
                                    Object removed = queue.remove();
                                    assert removed == item;
                                    System.out.println("EchoTube processing EOF");
                                    subscriber.onComplete();
                                } else {
                                    throw new InternalError(String.valueOf(item));
                                }
                            } finally {
                            }
                        }
                    } catch(Throwable t) {
                        t.printStackTrace();
                        throw t;
                    }
                }
            };
        }
    }
 }
