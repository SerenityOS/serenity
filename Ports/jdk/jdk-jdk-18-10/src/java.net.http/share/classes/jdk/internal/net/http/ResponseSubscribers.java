/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.net.http.HttpResponse.BodySubscriber;
import jdk.internal.net.http.common.Log;
import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.MinimalFuture;
import jdk.internal.net.http.common.Utils;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ResponseSubscribers {

    /**
     * This interface is used by our BodySubscriber implementations to
     * declare whether calling getBody() inline is safe, or whether
     * it needs to be called asynchronously in an executor thread.
     * Calling getBody() inline is usually safe except when it
     * might block - which can be the case if the BodySubscriber
     * is provided by custom code, or if it uses a finisher that
     * might be called and might block before the last bit is
     * received (for instance, if a mapping subscriber is used with
     * a mapper function that maps an InputStream to a GZIPInputStream,
     * as the the constructor of GZIPInputStream calls read()).
     * @param <T> The response type.
     */
    public interface TrustedSubscriber<T> extends BodySubscriber<T> {
        /**
         * Returns true if getBody() should be called asynchronously.
         * @implSpec The default implementation of this method returns
         *           false.
         * @return true if getBody() should be called asynchronously.
         */
        default boolean needsExecutor() { return false;}

        /**
         * Returns true if calling {@code bs::getBody} might block
         * and requires an executor.
         *
         * @implNote
         * In particular this method returns
         * true if {@code bs} is not a {@code TrustedSubscriber}.
         * If it is a {@code TrustedSubscriber}, it returns
         * {@code ((TrustedSubscriber) bs).needsExecutor()}.
         *
         * @param bs A BodySubscriber.
         * @return true if calling {@code bs::getBody} requires using
         *         an executor.
         */
        static boolean needsExecutor(BodySubscriber<?> bs) {
            if (bs instanceof TrustedSubscriber) {
                return ((TrustedSubscriber) bs).needsExecutor();
            } else return true;
        }
    }

    public static class ConsumerSubscriber implements TrustedSubscriber<Void> {
        private final Consumer<Optional<byte[]>> consumer;
        private Flow.Subscription subscription;
        private final CompletableFuture<Void> result = new MinimalFuture<>();
        private final AtomicBoolean subscribed = new AtomicBoolean();

        public ConsumerSubscriber(Consumer<Optional<byte[]>> consumer) {
            this.consumer = Objects.requireNonNull(consumer);
        }

        @Override
        public CompletionStage<Void> getBody() {
            return result;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            Objects.requireNonNull(subscription);
            if (!subscribed.compareAndSet(false, true)) {
                subscription.cancel();
            } else {
                this.subscription = subscription;
                subscription.request(1);
            }
        }

        @Override
        public void onNext(List<ByteBuffer> items) {
            Objects.requireNonNull(items);
            for (ByteBuffer item : items) {
                byte[] buf = new byte[item.remaining()];
                item.get(buf);
                consumer.accept(Optional.of(buf));
            }
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            Objects.requireNonNull(throwable);
            result.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            consumer.accept(Optional.empty());
            result.complete(null);
        }

    }

    /**
     * A Subscriber that writes the flow of data to a given file.
     *
     * Privileged actions are performed within a limited doPrivileged that only
     * asserts the specific, write, file permissions that were checked during
     * the construction of this PathSubscriber.
     */
    public static class PathSubscriber implements TrustedSubscriber<Path> {

        private static final FilePermission[] EMPTY_FILE_PERMISSIONS = new FilePermission[0];

        private final Path file;
        private final OpenOption[] options;
        @SuppressWarnings("removal")
        private final AccessControlContext acc;
        private final FilePermission[] filePermissions;
        private final boolean isDefaultFS;
        private final CompletableFuture<Path> result = new MinimalFuture<>();

        private final AtomicBoolean subscribed = new AtomicBoolean();
        private volatile Flow.Subscription subscription;
        private volatile FileChannel out;

        private static final String pathForSecurityCheck(Path path) {
            return path.toFile().getPath();
        }

        /**
         * Factory for creating PathSubscriber.
         *
         * Permission checks are performed here before construction of the
         * PathSubscriber. Permission checking and construction are deliberately
         * and tightly co-located.
         */
        public static PathSubscriber create(Path file,
                                            List<OpenOption> options) {
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            FilePermission filePermission = null;
            if (sm != null) {
                try {
                    String fn = pathForSecurityCheck(file);
                    FilePermission writePermission = new FilePermission(fn, "write");
                    sm.checkPermission(writePermission);
                    filePermission = writePermission;
                } catch (UnsupportedOperationException ignored) {
                    // path not associated with the default file system provider
                }
            }

            assert filePermission == null || filePermission.getActions().equals("write");
            @SuppressWarnings("removal")
            AccessControlContext acc = sm != null ? AccessController.getContext() : null;
            return new PathSubscriber(file, options, acc, filePermission);
        }

        // pp so handler implementations in the same package can construct
        /*package-private*/ PathSubscriber(Path file,
                                           List<OpenOption> options,
                                           @SuppressWarnings("removal") AccessControlContext acc,
                                           FilePermission... filePermissions) {
            this.file = file;
            this.options = options.stream().toArray(OpenOption[]::new);
            this.acc = acc;
            this.filePermissions = filePermissions == null || filePermissions[0] == null
                            ? EMPTY_FILE_PERMISSIONS : filePermissions;
            this.isDefaultFS = isDefaultFS(file);
        }

        private static boolean isDefaultFS(Path file) {
            try {
                file.toFile();
                return true;
            } catch (UnsupportedOperationException uoe) {
                return false;
            }
        }

        @SuppressWarnings("removal")
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            Objects.requireNonNull(subscription);
            if (!subscribed.compareAndSet(false, true)) {
                subscription.cancel();
                return;
            }

            this.subscription = subscription;
            if (acc == null) {
                try {
                    out = FileChannel.open(file, options);
                } catch (IOException ioe) {
                    result.completeExceptionally(ioe);
                    subscription.cancel();
                    return;
                }
            } else {
                try {
                    PrivilegedExceptionAction<FileChannel> pa =
                            () -> FileChannel.open(file, options);
                    out = isDefaultFS
                            ? AccessController.doPrivileged(pa, acc, filePermissions)
                            : AccessController.doPrivileged(pa, acc);
                } catch (PrivilegedActionException pae) {
                    Throwable t = pae.getCause() != null ? pae.getCause() : pae;
                    result.completeExceptionally(t);
                    subscription.cancel();
                    return;
                } catch (Exception e) {
                    result.completeExceptionally(e);
                    subscription.cancel();
                    return;
                }
            }
            subscription.request(1);
        }

        @Override
        public void onNext(List<ByteBuffer> items) {
            try {
                out.write(items.toArray(Utils.EMPTY_BB_ARRAY));
            } catch (IOException ex) {
                close();
                subscription.cancel();
                result.completeExceptionally(ex);
            }
            subscription.request(1);
        }

        @Override
        public void onError(Throwable e) {
            result.completeExceptionally(e);
            close();
        }

        @Override
        public void onComplete() {
            close();
            result.complete(file);
        }

        @Override
        public CompletionStage<Path> getBody() {
            return result;
        }

        @SuppressWarnings("removal")
        private void close() {
            if (acc == null) {
                Utils.close(out);
            } else {
                PrivilegedAction<Void> pa = () -> {
                    Utils.close(out);
                    return null;
                };
                if (isDefaultFS) {
                    AccessController.doPrivileged(pa, acc, filePermissions);
                } else {
                    AccessController.doPrivileged(pa, acc);
                }
            }
        }
    }

    public static class ByteArraySubscriber<T> implements TrustedSubscriber<T> {
        private final Function<byte[], T> finisher;
        private final CompletableFuture<T> result = new MinimalFuture<>();
        private final List<ByteBuffer> received = new ArrayList<>();

        private volatile Flow.Subscription subscription;

        public ByteArraySubscriber(Function<byte[],T> finisher) {
            this.finisher = finisher;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (this.subscription != null) {
                subscription.cancel();
                return;
            }
            this.subscription = subscription;
            // We can handle whatever you've got
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(List<ByteBuffer> items) {
            // incoming buffers are allocated by http client internally,
            // and won't be used anywhere except this place.
            // So it's free simply to store them for further processing.
            assert Utils.hasRemaining(items);
            received.addAll(items);
        }

        @Override
        public void onError(Throwable throwable) {
            received.clear();
            result.completeExceptionally(throwable);
        }

        static private byte[] join(List<ByteBuffer> bytes) {
            int size = Utils.remaining(bytes, Integer.MAX_VALUE);
            byte[] res = new byte[size];
            int from = 0;
            for (ByteBuffer b : bytes) {
                int l = b.remaining();
                b.get(res, from, l);
                from += l;
            }
            return res;
        }

        @Override
        public void onComplete() {
            try {
                result.complete(finisher.apply(join(received)));
                received.clear();
            } catch (IllegalArgumentException e) {
                result.completeExceptionally(e);
            }
        }

        @Override
        public CompletionStage<T> getBody() {
            return result;
        }
    }

    /**
     * An InputStream built on top of the Flow API.
     */
    public static class HttpResponseInputStream extends InputStream
        implements TrustedSubscriber<InputStream>
    {
        final static int MAX_BUFFERS_IN_QUEUE = 1;  // lock-step with the producer

        // An immutable ByteBuffer sentinel to mark that the last byte was received.
        private static final ByteBuffer LAST_BUFFER = ByteBuffer.wrap(new byte[0]);
        private static final List<ByteBuffer> LAST_LIST = List.of(LAST_BUFFER);
        private static final Logger debug =
                Utils.getDebugLogger("HttpResponseInputStream"::toString, Utils.DEBUG);

        // A queue of yet unprocessed ByteBuffers received from the flow API.
        private final BlockingQueue<List<ByteBuffer>> buffers;
        private volatile Flow.Subscription subscription;
        private volatile boolean closed;
        private volatile Throwable failed;
        private volatile Iterator<ByteBuffer> currentListItr;
        private volatile ByteBuffer currentBuffer;
        private final AtomicBoolean subscribed = new AtomicBoolean();

        public HttpResponseInputStream() {
            this(MAX_BUFFERS_IN_QUEUE);
        }

        HttpResponseInputStream(int maxBuffers) {
            int capacity = (maxBuffers <= 0 ? MAX_BUFFERS_IN_QUEUE : maxBuffers);
            // 1 additional slot needed for LAST_LIST added by onComplete
            this.buffers = new ArrayBlockingQueue<>(capacity + 1);
        }

        @Override
        public CompletionStage<InputStream> getBody() {
            // Returns the stream immediately, before the
            // response body is received.
            // This makes it possible for sendAsync().get().body()
            // to complete before the response body is received.
            return CompletableFuture.completedStage(this);
        }

        // Returns the current byte buffer to read from.
        // If the current buffer has no remaining data, this method will take the
        // next buffer from the buffers queue, possibly blocking until
        // a new buffer is made available through the Flow API, or the
        // end of the flow has been reached.
        private ByteBuffer current() throws IOException {
            while (currentBuffer == null || !currentBuffer.hasRemaining()) {
                // Check whether the stream is closed or exhausted
                if (closed || failed != null) {
                    throw new IOException("closed", failed);
                }
                if (currentBuffer == LAST_BUFFER) break;

                try {
                    if (currentListItr == null || !currentListItr.hasNext()) {
                        // Take a new list of buffers from the queue, blocking
                        // if none is available yet...

                        if (debug.on()) debug.log("Taking list of Buffers");
                        List<ByteBuffer> lb = buffers.take();
                        currentListItr = lb.iterator();
                        if (debug.on()) debug.log("List of Buffers Taken");

                        // Check whether an exception was encountered upstream
                        if (closed || failed != null)
                            throw new IOException("closed", failed);

                        // Check whether we're done.
                        if (lb == LAST_LIST) {
                            currentListItr = null;
                            currentBuffer = LAST_BUFFER;
                            break;
                        }

                        // Request another upstream item ( list of buffers )
                        Flow.Subscription s = subscription;
                        if (s != null) {
                            if (debug.on()) debug.log("Increased demand by 1");
                            s.request(1);
                        }
                        assert currentListItr != null;
                        if (lb.isEmpty()) continue;
                    }
                    assert currentListItr != null;
                    assert currentListItr.hasNext();
                    if (debug.on()) debug.log("Next Buffer");
                    currentBuffer = currentListItr.next();
                } catch (InterruptedException ex) {
                    // continue
                }
            }
            assert currentBuffer == LAST_BUFFER || currentBuffer.hasRemaining();
            return currentBuffer;
        }

        @Override
        public int read(byte[] bytes, int off, int len) throws IOException {
            Objects.checkFromIndexSize(off, len, bytes.length);
            if (len == 0) {
                return 0;
            }
            // get the buffer to read from, possibly blocking if
            // none is available
            ByteBuffer buffer;
            if ((buffer = current()) == LAST_BUFFER) return -1;

            // don't attempt to read more than what is available
            // in the current buffer.
            int read = Math.min(buffer.remaining(), len);
            assert read > 0 && read <= buffer.remaining();

            // buffer.get() will do the boundary check for us.
            buffer.get(bytes, off, read);
            return read;
        }

        @Override
        public int read() throws IOException {
            ByteBuffer buffer;
            if ((buffer = current()) == LAST_BUFFER) return -1;
            return buffer.get() & 0xFF;
        }

        @Override
        public int available() throws IOException {
            // best effort: returns the number of remaining bytes in
            // the current buffer if any, or 1 if the current buffer
            // is null or empty but the queue or current buffer list
            // are not empty. Returns 0 otherwise.
            if (closed) return 0;
            int available = 0;
            ByteBuffer current = currentBuffer;
            if (current == LAST_BUFFER) return 0;
            if (current != null) available = current.remaining();
            if (available != 0) return available;
            Iterator<?> iterator = currentListItr;
            if (iterator != null && iterator.hasNext()) return 1;
            if (buffers.isEmpty()) return 0;
            return 1;
        }

        @Override
        public void onSubscribe(Flow.Subscription s) {
            Objects.requireNonNull(s);
            try {
                if (!subscribed.compareAndSet(false, true)) {
                    s.cancel();
                } else {
                    // check whether the stream is already closed.
                    // if so, we should cancel the subscription
                    // immediately.
                    boolean closed;
                    synchronized (this) {
                        closed = this.closed;
                        if (!closed) {
                            this.subscription = s;
                        }
                    }
                    if (closed) {
                        s.cancel();
                        return;
                    }
                    assert buffers.remainingCapacity() > 1; // should contain at least 2
                    if (debug.on())
                        debug.log("onSubscribe: requesting "
                                  + Math.max(1, buffers.remainingCapacity() - 1));
                    s.request(Math.max(1, buffers.remainingCapacity() - 1));
                }
            } catch (Throwable t) {
                failed = t;
                try {
                    close();
                } catch (IOException x) {
                    // OK
                } finally {
                    onError(t);
                }
            }
        }

        @Override
        public void onNext(List<ByteBuffer> t) {
            Objects.requireNonNull(t);
            try {
                if (debug.on()) debug.log("next item received");
                if (!buffers.offer(t)) {
                    throw new IllegalStateException("queue is full");
                }
                if (debug.on()) debug.log("item offered");
            } catch (Throwable ex) {
                failed = ex;
                try {
                    close();
                } catch (IOException ex1) {
                    // OK
                } finally {
                    onError(ex);
                }
            }
        }

        @Override
        public void onError(Throwable thrwbl) {
            subscription = null;
            failed = Objects.requireNonNull(thrwbl);
            // The client process that reads the input stream might
            // be blocked in queue.take().
            // Tries to offer LAST_LIST to the queue. If the queue is
            // full we don't care if we can't insert this buffer, as
            // the client can't be blocked in queue.take() in that case.
            // Adding LAST_LIST to the queue is harmless, as the client
            // should find failed != null before handling LAST_LIST.
            buffers.offer(LAST_LIST);
        }

        @Override
        public void onComplete() {
            subscription = null;
            onNext(LAST_LIST);
        }

        @Override
        public void close() throws IOException {
            Flow.Subscription s;
            synchronized (this) {
                if (closed) return;
                closed = true;
                s = subscription;
                subscription = null;
            }
            // s will be null if already completed
            try {
                if (s != null) {
                    s.cancel();
                }
            } finally {
                buffers.offer(LAST_LIST);
                super.close();
            }
        }

    }

    public static BodySubscriber<Stream<String>> createLineStream() {
        return createLineStream(UTF_8);
    }

    public static BodySubscriber<Stream<String>> createLineStream(Charset charset) {
        Objects.requireNonNull(charset);
        BodySubscriber<InputStream> s = new HttpResponseInputStream();
        // Creates a MappingSubscriber with a trusted finisher that is
        // trusted not to block.
        return new MappingSubscriber<InputStream,Stream<String>>(s,
            (InputStream stream) -> {
                return new BufferedReader(new InputStreamReader(stream, charset))
                            .lines().onClose(() -> Utils.close(stream));
            }, true);
    }

    /**
     * Currently this consumes all of the data and ignores it
     */
    public static class NullSubscriber<T> implements TrustedSubscriber<T> {

        private final CompletableFuture<T> cf = new MinimalFuture<>();
        private final Optional<T> result;
        private final AtomicBoolean subscribed = new AtomicBoolean();

        public NullSubscriber(Optional<T> result) {
            this.result = result;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            Objects.requireNonNull(subscription);
            if (!subscribed.compareAndSet(false, true)) {
                subscription.cancel();
            } else {
                subscription.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(List<ByteBuffer> items) {
            Objects.requireNonNull(items);
        }

        @Override
        public void onError(Throwable throwable) {
            Objects.requireNonNull(throwable);
            cf.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            if (result.isPresent()) {
                cf.complete(result.get());
            } else {
                cf.complete(null);
            }
        }

        @Override
        public CompletionStage<T> getBody() {
            return cf;
        }
    }

    /** An adapter between {@code BodySubscriber} and {@code Flow.Subscriber}. */
    public static final class SubscriberAdapter<S extends Subscriber<? super List<ByteBuffer>>,R>
        implements TrustedSubscriber<R>
    {
        private final CompletableFuture<R> cf = new MinimalFuture<>();
        private final S subscriber;
        private final Function<? super S,? extends R> finisher;
        private volatile Subscription subscription;

        // The finisher isn't called until all bytes have been received,
        // and so shouldn't need an executor. No need to override
        // TrustedSubscriber::needsExecutor
        public SubscriberAdapter(S subscriber, Function<? super S,? extends R> finisher) {
            this.subscriber = Objects.requireNonNull(subscriber);
            this.finisher = Objects.requireNonNull(finisher);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            Objects.requireNonNull(subscription);
            if (this.subscription != null) {
                subscription.cancel();
            } else {
                this.subscription = subscription;
                subscriber.onSubscribe(subscription);
            }
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            Objects.requireNonNull(item);
            try {
                subscriber.onNext(item);
            } catch (Throwable throwable) {
                subscription.cancel();
                onError(throwable);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            Objects.requireNonNull(throwable);
            try {
                subscriber.onError(throwable);
            } finally {
                cf.completeExceptionally(throwable);
            }
        }

        @Override
        public void onComplete() {
            try {
                subscriber.onComplete();
            } finally {
                try {
                    cf.complete(finisher.apply(subscriber));
                } catch (Throwable throwable) {
                    cf.completeExceptionally(throwable);
                }
            }
        }

        @Override
        public CompletionStage<R> getBody() {
            return cf;
        }
    }

    /**
     * A body subscriber which receives input from an upstream subscriber
     * and maps that subscriber's body type to a new type. The upstream subscriber
     * delegates all flow operations directly to this object. The
     * {@link CompletionStage} returned by {@link #getBody()}} takes the output
     * of the upstream {@code getBody()} and applies the mapper function to
     * obtain the new {@code CompletionStage} type.
     *
     * @param <T> the upstream body type
     * @param <U> this subscriber's body type
     */
    public static class MappingSubscriber<T,U> implements TrustedSubscriber<U> {
        private final BodySubscriber<T> upstream;
        private final Function<? super T,? extends U> mapper;
        private final boolean trusted;

        public MappingSubscriber(BodySubscriber<T> upstream,
                                 Function<? super T,? extends U> mapper) {
            this(upstream, mapper, false);
        }

        // creates a MappingSubscriber with a mapper that is trusted
        // to not block when called.
        MappingSubscriber(BodySubscriber<T> upstream,
                          Function<? super T,? extends U> mapper,
                          boolean trusted) {
            this.upstream = Objects.requireNonNull(upstream);
            this.mapper = Objects.requireNonNull(mapper);
            this.trusted = trusted;
        }

        // There is no way to know whether a custom mapper function
        // might block or not - so we should return true unless the
        // mapper is implemented and trusted by our own code not to
        // block.
        @Override
        public boolean needsExecutor() {
            return !trusted || TrustedSubscriber.needsExecutor(upstream);
        }

        // If upstream.getBody() is already completed (case of InputStream),
        // then calling upstream.getBody().thenApply(mapper) might block
        // if the mapper blocks. We should probably add a variant of
        // MappingSubscriber that calls thenApplyAsync instead, but this
        // needs a new public API point. See needsExecutor() above.
        @Override
        public CompletionStage<U> getBody() {
            return upstream.getBody().thenApply(mapper);
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            upstream.onSubscribe(subscription);
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            upstream.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            upstream.onError(throwable);
        }

        @Override
        public void onComplete() {
            upstream.onComplete();
        }
    }

    // A BodySubscriber that returns a Publisher<List<ByteBuffer>>
    static class PublishingBodySubscriber
            implements TrustedSubscriber<Flow.Publisher<List<ByteBuffer>>> {
        private final MinimalFuture<Flow.Subscription>
                subscriptionCF = new MinimalFuture<>();
        private final MinimalFuture<SubscriberRef>
                subscribedCF = new MinimalFuture<>();
        private AtomicReference<SubscriberRef>
                subscriberRef = new AtomicReference<>();
        private final CompletionStage<Flow.Publisher<List<ByteBuffer>>> body =
                subscriptionCF.thenCompose(
                        (s) -> MinimalFuture.completedFuture(this::subscribe));

        // We use the completionCF to ensure that only one of
        // onError or onComplete is ever called.
        private final MinimalFuture<Void> completionCF;
        private PublishingBodySubscriber() {
            completionCF = new MinimalFuture<>();
            completionCF.whenComplete(
                    (r,t) -> subscribedCF.thenAccept( s -> complete(s, t)));
        }

        // An object that holds a reference to a Flow.Subscriber.
        // The reference is cleared when the subscriber is completed - either
        // normally or exceptionally, or when the subscription is cancelled.
        static final class SubscriberRef {
            volatile Flow.Subscriber<? super List<ByteBuffer>> ref;
            SubscriberRef(Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
                ref = subscriber;
            }
            Flow.Subscriber<? super List<ByteBuffer>> get() {
                return ref;
            }
            Flow.Subscriber<? super List<ByteBuffer>> clear() {
                Flow.Subscriber<? super List<ByteBuffer>> res = ref;
                ref = null;
                return res;
            }
        }

        // A subscription that wraps an upstream subscription and
        // holds a reference to a subscriber. The subscriber reference
        // is cleared when the subscription is cancelled
        final static class SubscriptionRef implements Flow.Subscription {
            final Flow.Subscription subscription;
            final SubscriberRef subscriberRef;
            SubscriptionRef(Flow.Subscription subscription,
                            SubscriberRef subscriberRef) {
                this.subscription = subscription;
                this.subscriberRef = subscriberRef;
            }
            @Override
            public void request(long n) {
                if (subscriberRef.get() != null) {
                    subscription.request(n);
                }
            }
            @Override
            public void cancel() {
                subscription.cancel();
                subscriberRef.clear();
            }

            void subscribe() {
                Subscriber<?> subscriber = subscriberRef.get();
                if (subscriber != null) {
                    subscriber.onSubscribe(this);
                }
            }

            @Override
            public String toString() {
                return "SubscriptionRef/"
                        + subscription.getClass().getName()
                        + "@"
                        + System.identityHashCode(subscription);
            }
        }

        // This is a callback for the subscribedCF.
        // Do not call directly!
        private void complete(SubscriberRef ref, Throwable t) {
            assert ref != null;
            Subscriber<?> s = ref.clear();
            // maybe null if subscription was cancelled
            if (s == null) return;
            if (t == null) {
                try {
                    s.onComplete();
                } catch (Throwable x) {
                    s.onError(x);
                }
            } else {
                s.onError(t);
            }
        }

        private void signalError(Throwable err) {
            if (err == null) {
                err = new NullPointerException("null throwable");
            }
            completionCF.completeExceptionally(err);
        }

        private void signalComplete() {
            completionCF.complete(null);
        }

        private void subscribe(Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
            Objects.requireNonNull(subscriber, "subscriber must not be null");
            SubscriberRef ref = new SubscriberRef(subscriber);
            if (subscriberRef.compareAndSet(null, ref)) {
                subscriptionCF.thenAccept((s) -> {
                    SubscriptionRef subscription = new SubscriptionRef(s,ref);
                    try {
                        subscription.subscribe();
                        subscribedCF.complete(ref);
                    } catch (Throwable t) {
                        if (Log.errors()) {
                            Log.logError("Failed to call onSubscribe: " +
                                    "cancelling subscription: " + t);
                            Log.logError(t);
                        }
                        subscription.cancel();
                    }
                });
            } else {
                subscriber.onSubscribe(new Flow.Subscription() {
                    @Override public void request(long n) { }
                    @Override public void cancel() { }
                });
                subscriber.onError(new IllegalStateException(
                        "This publisher has already one subscriber"));
            }
        }

        private final AtomicBoolean subscribed = new AtomicBoolean();

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            Objects.requireNonNull(subscription);
            if (!subscribed.compareAndSet(false, true)) {
                subscription.cancel();
            } else {
                subscriptionCF.complete(subscription);
            }
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            Objects.requireNonNull(item);
            try {
                // cannot be called before onSubscribe()
                assert subscriptionCF.isDone();
                SubscriberRef ref = subscriberRef.get();
                // cannot be called before subscriber calls request(1)
                assert ref != null;
                Flow.Subscriber<? super List<ByteBuffer>>
                        subscriber = ref.get();
                if (subscriber != null) {
                    // may be null if subscription was cancelled.
                    subscriber.onNext(item);
                }
            } catch (Throwable err) {
                signalError(err);
                subscriptionCF.thenAccept(s -> s.cancel());
            }
        }

        @Override
        public void onError(Throwable throwable) {
            // cannot be called before onSubscribe();
            assert suppress(subscriptionCF.isDone(),
                    "onError called before onSubscribe",
                    throwable);
            // onError can be called before request(1), and therefore can
            // be called before subscriberRef is set.
            signalError(throwable);
            Objects.requireNonNull(throwable);
        }

        @Override
        public void onComplete() {
            // cannot be called before onSubscribe()
            if (!subscriptionCF.isDone()) {
                signalError(new InternalError(
                        "onComplete called before onSubscribed"));
            } else {
                // onComplete can be called before request(1),
                // and therefore can be called before subscriberRef
                // is set.
                signalComplete();
            }
        }

        @Override
        public CompletionStage<Flow.Publisher<List<ByteBuffer>>> getBody() {
            return body;
        }

        private boolean suppress(boolean condition,
                                 String assertion,
                                 Throwable carrier) {
            if (!condition) {
                if (carrier != null) {
                    carrier.addSuppressed(new AssertionError(assertion));
                } else if (Log.errors()) {
                    Log.logError(new AssertionError(assertion));
                }
            }
            return true;
        }

    }

    public static BodySubscriber<Flow.Publisher<List<ByteBuffer>>>
    createPublisher() {
        return new PublishingBodySubscriber();
    }


    /**
     * Tries to determine whether bs::getBody must be invoked asynchronously,
     * and if so, uses the provided executor to do it.
     * If the executor is a {@link HttpClientImpl.DelegatingExecutor},
     * uses the executor's delegate.
     * @param e    The executor to use if an executor is required.
     * @param bs   The BodySubscriber (trusted or not)
     * @param <T>  The type of the response.
     * @return A completion stage that completes when the completion
     *         stage returned by bs::getBody completes. This may, or
     *         may not, be the same completion stage.
     */
    public static <T> CompletionStage<T> getBodyAsync(Executor e, BodySubscriber<T> bs) {
        if (TrustedSubscriber.needsExecutor(bs)) {
            // getBody must be called in the executor
            return getBodyAsync(e, bs, new MinimalFuture<>());
        } else {
            // No executor needed
            return bs.getBody();
        }
    }

    /**
     * Invokes bs::getBody using the provided executor.
     * If invoking bs::getBody requires an executor, and the given executor
     * is a {@link HttpClientImpl.DelegatingExecutor}, then the executor's
     * delegate is used. If an error occurs anywhere then the given {code cf}
     * is completed exceptionally (this method does not throw).
     * @param e   The executor that should be used to call bs::getBody
     * @param bs  The BodySubscriber
     * @param cf  A completable future that this function will set up
     *            to complete when the completion stage returned by
     *            bs::getBody completes.
     *            In case of any error while trying to set up the
     *            completion chain, {@code cf} will be completed
     *            exceptionally with that error.
     * @param <T> The response type.
     * @return The provided {@code cf}.
     */
    public static <T> CompletableFuture<T> getBodyAsync(Executor e,
                                                      BodySubscriber<T> bs,
                                                      CompletableFuture<T> cf) {
        return getBodyAsync(e, bs, cf, cf::completeExceptionally);
    }

    /**
     * Invokes bs::getBody using the provided executor.
     * If invoking bs::getBody requires an executor, and the given executor
     * is a {@link HttpClientImpl.DelegatingExecutor}, then the executor's
     * delegate is used.
     * The provided {@code cf} is completed with the result (exceptional
     * or not) of the completion stage returned by bs::getBody.
     * If an error occurs when trying to set up the
     * completion chain, the provided {@code errorHandler} is invoked,
     * but {@code cf} is not necessarily affected.
     * This method does not throw.
     * @param e   The executor that should be used to call bs::getBody
     * @param bs  The BodySubscriber
     * @param cf  A completable future that this function will set up
     *            to complete when the completion stage returned by
     *            bs::getBody completes.
     *            In case of any error while trying to set up the
     *            completion chain, {@code cf} will be completed
     *            exceptionally with that error.
     * @param errorHandler The handler to invoke if an error is raised
     *                     while trying to set up the completion chain.
     * @param <T> The response type.
     * @return The provide {@code cf}. If the {@code errorHandler} is
     * invoked, it is the responsibility of the {@code errorHandler} to
     * complete the {@code cf}, if needed.
     */
    public static <T> CompletableFuture<T> getBodyAsync(Executor e,
                                                      BodySubscriber<T> bs,
                                                      CompletableFuture<T> cf,
                                                      Consumer<Throwable> errorHandler) {
        assert errorHandler != null;
        try {
            assert e != null;
            assert cf != null;

            if (TrustedSubscriber.needsExecutor(bs)) {
                e = (e instanceof HttpClientImpl.DelegatingExecutor)
                        ? ((HttpClientImpl.DelegatingExecutor) e).delegate() : e;
            }

            e.execute(() -> {
                try {
                    bs.getBody().whenComplete((r, t) -> {
                        if (t != null) {
                            cf.completeExceptionally(t);
                        } else {
                            cf.complete(r);
                        }
                    });
                } catch (Throwable t) {
                    errorHandler.accept(t);
                }
            });
            return cf;

        } catch (Throwable t) {
            errorHandler.accept(t);
        }
        return cf;
    }
}
