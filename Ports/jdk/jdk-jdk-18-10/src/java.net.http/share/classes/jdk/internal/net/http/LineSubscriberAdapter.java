/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import jdk.internal.net.http.common.Demand;
import java.net.http.HttpResponse.BodySubscriber;
import jdk.internal.net.http.common.MinimalFuture;
import jdk.internal.net.http.common.SequentialScheduler;

/** An adapter between {@code BodySubscriber} and {@code Flow.Subscriber<String>}. */
public final class LineSubscriberAdapter<S extends Subscriber<? super String>,R>
        implements BodySubscriber<R> {
    private final CompletableFuture<R> cf = new MinimalFuture<>();
    private final S subscriber;
    private final Function<? super S, ? extends R> finisher;
    private final Charset charset;
    private final String eol;
    private final AtomicBoolean subscribed = new AtomicBoolean();
    private volatile LineSubscription downstream;

    private LineSubscriberAdapter(S subscriber,
                                  Function<? super S, ? extends R> finisher,
                                  Charset charset,
                                  String eol) {
        if (eol != null && eol.isEmpty())
            throw new IllegalArgumentException("empty line separator");
        this.subscriber = Objects.requireNonNull(subscriber);
        this.finisher = Objects.requireNonNull(finisher);
        this.charset = Objects.requireNonNull(charset);
        this.eol = eol;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        Objects.requireNonNull(subscription);
        if (!subscribed.compareAndSet(false, true)) {
            subscription.cancel();
            return;
        }

        downstream = LineSubscription.create(subscription,
                                             charset,
                                             eol,
                                             subscriber,
                                             cf);
        subscriber.onSubscribe(downstream);
    }

    @Override
    public void onNext(List<ByteBuffer> item) {
        Objects.requireNonNull(item);
        try {
            downstream.submit(item);
        } catch (Throwable t) {
            onError(t);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        Objects.requireNonNull(throwable);
        try {
            downstream.signalError(throwable);
        } finally {
            cf.completeExceptionally(throwable);
        }
    }

    @Override
    public void onComplete() {
        try {
            downstream.signalComplete();
        } finally {
            cf.complete(finisher.apply(subscriber));
        }
    }

    @Override
    public CompletionStage<R> getBody() {
        return cf;
    }

    public static <S extends Subscriber<? super String>, R> LineSubscriberAdapter<S, R>
    create(S subscriber, Function<? super S, ? extends R> finisher, Charset charset, String eol)
    {
        if (eol != null && eol.isEmpty())
            throw new IllegalArgumentException("empty line separator");
        return new LineSubscriberAdapter<>(Objects.requireNonNull(subscriber),
                Objects.requireNonNull(finisher),
                Objects.requireNonNull(charset),
                eol);
    }

    static final class LineSubscription implements Flow.Subscription {
        final Flow.Subscription upstreamSubscription;
        final CharsetDecoder decoder;
        final String newline;
        final Demand downstreamDemand;
        final ConcurrentLinkedDeque<ByteBuffer> queue;
        final SequentialScheduler scheduler;
        final Flow.Subscriber<? super String> upstream;
        final CompletableFuture<?> cf;
        private final AtomicReference<Throwable> errorRef = new AtomicReference<>();
        private final AtomicLong demanded = new AtomicLong();
        private volatile boolean completed;
        private volatile boolean cancelled;

        private final char[] chars = new char[1024];
        private final ByteBuffer leftover = ByteBuffer.wrap(new byte[64]);
        private final CharBuffer buffer = CharBuffer.wrap(chars);
        private final StringBuilder builder = new StringBuilder();
        private String nextLine;

        private LineSubscription(Flow.Subscription s,
                                 CharsetDecoder dec,
                                 String separator,
                                 Flow.Subscriber<? super String> subscriber,
                                 CompletableFuture<?> completion) {
            downstreamDemand = new Demand();
            queue = new ConcurrentLinkedDeque<>();
            upstreamSubscription = Objects.requireNonNull(s);
            decoder = Objects.requireNonNull(dec);
            newline = separator;
            upstream = Objects.requireNonNull(subscriber);
            cf = Objects.requireNonNull(completion);
            scheduler = SequentialScheduler.lockingScheduler(this::loop);
        }

        @Override
        public void request(long n) {
            if (cancelled) return;
            if (downstreamDemand.increase(n)) {
                scheduler.runOrSchedule();
            }
        }

        @Override
        public void cancel() {
            cancelled = true;
            upstreamSubscription.cancel();
        }

        public void submit(List<ByteBuffer> list) {
            queue.addAll(list);
            demanded.decrementAndGet();
            scheduler.runOrSchedule();
        }

        public void signalComplete() {
            completed = true;
            scheduler.runOrSchedule();
        }

        public void signalError(Throwable error) {
            if (errorRef.compareAndSet(null,
                    Objects.requireNonNull(error))) {
                scheduler.runOrSchedule();
            }
        }

        // This method looks at whether some bytes where left over (in leftover)
        // from decoding the previous buffer when the previous buffer was in
        // underflow. If so, it takes bytes one by one from the new buffer 'in'
        // and combines them with the leftover bytes until 'in' is exhausted or a
        // character was produced in 'out', resolving the previous underflow.
        // Returns true if the buffer is still in underflow, false otherwise.
        // However, in both situation some chars might have been produced in 'out'.
        private boolean isUnderFlow(ByteBuffer in, CharBuffer out, boolean endOfInput)
                throws CharacterCodingException {
            int limit = leftover.position();
            if (limit == 0) {
                // no leftover
                return false;
            } else {
                CoderResult res = null;
                while (in.hasRemaining()) {
                    leftover.position(limit);
                    leftover.limit(++limit);
                    leftover.put(in.get());
                    leftover.position(0);
                    res = decoder.decode(leftover, out,
                            endOfInput && !in.hasRemaining());
                    int remaining = leftover.remaining();
                    if (remaining > 0) {
                        assert leftover.position() == 0;
                        leftover.position(remaining);
                    } else {
                        leftover.position(0);
                    }
                    leftover.limit(leftover.capacity());
                    if (res.isUnderflow() && remaining > 0 && in.hasRemaining()) {
                        continue;
                    }
                    if (res.isError()) {
                        res.throwException();
                    }
                    assert !res.isOverflow();
                    return false;
                }
                return !endOfInput;
            }
        }

        // extract characters from start to end and remove them from
        // the StringBuilder
        private static String take(StringBuilder b, int start, int end) {
            assert start == 0;
            String line;
            if (end == start) return "";
            line = b.substring(start, end);
            b.delete(start, end);
            return line;
        }

        // finds end of line, returns -1 if not found, or the position after
        // the line delimiter if found, removing the delimiter in the process.
        private static int endOfLine(StringBuilder b, String eol, boolean endOfInput) {
            int len = b.length();
            if (eol != null) { // delimiter explicitly specified
                int i = b.indexOf(eol);
                if (i >= 0) {
                    // remove the delimiter and returns the position
                    // of the char after it.
                    b.delete(i, i + eol.length());
                    return i;
                }
            } else { // no delimiter specified, behaves as BufferedReader::readLine
                boolean crfound = false;
                for (int i = 0; i < len; i++) {
                    char c = b.charAt(i);
                    if (c == '\n') {
                        // '\n' or '\r\n' found.
                        // remove the delimiter and returns the position
                        // of the char after it.
                        b.delete(crfound ? i - 1 : i, i + 1);
                        return crfound ? i - 1 : i;
                    } else if (crfound) {
                        // previous char was '\r', c != '\n'
                        assert i != 0;
                        // remove the delimiter and returns the position
                        // of the char after it.
                        b.delete(i - 1, i);
                        return i - 1;
                    }
                    crfound = c == '\r';
                }
                if (crfound && endOfInput) {
                    // remove the delimiter and returns the position
                    // of the char after it.
                    b.delete(len - 1, len);
                    return len - 1;
                }
            }
            return endOfInput && len > 0 ? len : -1;
        }

        // Looks at whether the StringBuilder contains a line.
        // Returns null if more character are needed.
        private static String nextLine(StringBuilder b, String eol, boolean endOfInput) {
            int next = endOfLine(b, eol, endOfInput);
            return (next > -1) ? take(b, 0, next) : null;
        }

        // Attempts to read the next line. Returns the next line if
        // the delimiter was found, null otherwise. The delimiters are
        // consumed.
        private String nextLine()
                throws CharacterCodingException {
            assert nextLine == null;
            LINES:
            while (nextLine == null) {
                boolean endOfInput = completed && queue.isEmpty();
                nextLine = nextLine(builder, newline,
                        endOfInput && leftover.position() == 0);
                if (nextLine != null) return nextLine;
                ByteBuffer b;
                BUFFERS:
                while ((b = queue.peek()) != null) {
                    if (!b.hasRemaining()) {
                        queue.poll();
                        continue BUFFERS;
                    }
                    BYTES:
                    while (b.hasRemaining()) {
                        buffer.position(0);
                        buffer.limit(buffer.capacity());
                        boolean endofInput = completed && queue.size() <= 1;
                        if (isUnderFlow(b, buffer, endofInput)) {
                            assert !b.hasRemaining();
                            if (buffer.position() > 0) {
                                buffer.flip();
                                builder.append(buffer);
                            }
                            continue BUFFERS;
                        }
                        CoderResult res = decoder.decode(b, buffer, endofInput);
                        if (res.isError()) res.throwException();
                        if (buffer.position() > 0) {
                            buffer.flip();
                            builder.append(buffer);
                            continue LINES;
                        }
                        if (res.isUnderflow() && b.hasRemaining()) {
                            //System.out.println("underflow: adding " + b.remaining() + " bytes");
                            leftover.put(b);
                            assert !b.hasRemaining();
                            continue BUFFERS;
                        }
                    }
                }

                assert queue.isEmpty();
                if (endOfInput) {
                    // Time to cleanup: there may be some undecoded leftover bytes
                    // We need to flush them out.
                    // The decoder has been configured to replace malformed/unmappable
                    // chars with some replacement, in order to behave like
                    // InputStreamReader.
                    leftover.flip();
                    buffer.position(0);
                    buffer.limit(buffer.capacity());

                    // decode() must be called just before flush, even if there
                    // is nothing to decode. We must do this even if leftover
                    // has no remaining bytes.
                    CoderResult res = decoder.decode(leftover, buffer, endOfInput);
                    if (buffer.position() > 0) {
                        buffer.flip();
                        builder.append(buffer);
                    }
                    if (res.isError()) res.throwException();

                    // Now call decoder.flush()
                    buffer.position(0);
                    buffer.limit(buffer.capacity());
                    res = decoder.flush(buffer);
                    if (buffer.position() > 0) {
                        buffer.flip();
                        builder.append(buffer);
                    }
                    if (res.isError()) res.throwException();

                    // It's possible that we reach here twice - just for the
                    // purpose of checking that no bytes were left over, so
                    // we reset leftover/decoder to make the function reentrant.
                    leftover.position(0);
                    leftover.limit(leftover.capacity());
                    decoder.reset();

                    // if some chars were produced then this call will
                    // return them.
                    return nextLine = nextLine(builder, newline, endOfInput);
                }
                return null;
            }
            return null;
        }

        // The main sequential scheduler loop.
        private void loop() {
            try {
                while (!cancelled) {
                    Throwable error = errorRef.get();
                    if (error != null) {
                        cancelled = true;
                        scheduler.stop();
                        upstream.onError(error);
                        cf.completeExceptionally(error);
                        return;
                    }
                    if (nextLine == null) nextLine = nextLine();
                    if (nextLine == null) {
                        if (completed) {
                            scheduler.stop();
                            if (leftover.position() != 0) {
                                // Underflow: not all bytes could be
                                // decoded, but no more bytes will be coming.
                                // This should not happen as we should already
                                // have got a MalformedInputException, or
                                // replaced the unmappable chars.
                                errorRef.compareAndSet(null,
                                        new IllegalStateException(
                                                "premature end of input ("
                                                        + leftover.position()
                                                        + " undecoded bytes)"));
                                continue;
                            } else {
                                upstream.onComplete();
                            }
                            return;
                        } else if (demanded.get() == 0
                                && !downstreamDemand.isFulfilled()) {
                            long incr = Math.max(1, downstreamDemand.get());
                            demanded.addAndGet(incr);
                            upstreamSubscription.request(incr);
                            continue;
                        } else return;
                    }
                    assert nextLine != null;
                    assert newline != null && !nextLine.endsWith(newline)
                            || !nextLine.endsWith("\n") || !nextLine.endsWith("\r");
                    if (downstreamDemand.tryDecrement()) {
                        String forward = nextLine;
                        nextLine = null;
                        upstream.onNext(forward);
                    } else return; // no demand: come back later
                }
            } catch (Throwable t) {
                try {
                    upstreamSubscription.cancel();
                } finally {
                    signalError(t);
                }
            }
        }

        static LineSubscription create(Flow.Subscription s,
                                       Charset charset,
                                       String lineSeparator,
                                       Flow.Subscriber<? super String> upstream,
                                       CompletableFuture<?> cf) {
            return new LineSubscription(Objects.requireNonNull(s),
                    Objects.requireNonNull(charset).newDecoder()
                            // use the same decoder configuration than
                            // java.io.InputStreamReader
                            .onMalformedInput(CodingErrorAction.REPLACE)
                            .onUnmappableCharacter(CodingErrorAction.REPLACE),
                    lineSeparator,
                    Objects.requireNonNull(upstream),
                    Objects.requireNonNull(cf));
        }
    }
}

