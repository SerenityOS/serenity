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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.stream.Stream;

/*
 * S for Support.
 *
 * Auxiliary methods for tests that check conformance with reactive streams
 * specification.
 *
 * Short name is for the sake of convenience calling this class' static methods.
 * It could've been called Support or TckSupport, but then we would need to
 * place this class in its own package so as to use "import static".
 */
public class S {

    private static final Random RANDOM = new SecureRandom();

    private S() { }

    public static List<ByteBuffer> listOfBuffersFromBufferOfNBytes(int nBytes) {
        return scatterBuffer(bufferOfNRandomBytes(nBytes));
    }

    /*
     * Spreads the remaining contents of the given byte buffer across a number
     * of buffers put into a list.
     */
    public static List<ByteBuffer> scatterBuffer(ByteBuffer src) {
        List<ByteBuffer> buffers = new ArrayList<>();
        while (src.hasRemaining()) {
            // We do not allow empty buffers ~~~~~~~~~~~~~~~~v
            int capacity = RANDOM.nextInt(src.remaining()) + 1;
            ByteBuffer b = ByteBuffer.allocate(capacity);
            for (int i = 0; i < capacity; i++) {
                b.put(src.get());
            }
            b.flip();
            buffers.add(b);
        }
        return List.copyOf(buffers);
    }

    public static ByteBuffer bufferOfNRandomBytes(int capacity) {
        return ByteBuffer.wrap(arrayOfNRandomBytes(capacity));
    }

    public static byte[] arrayOfNRandomBytes(int nBytes) {
        byte[] contents = new byte[nBytes];
        RANDOM.nextBytes(contents);
        return contents;
    }

    public static InputStream inputStreamOfNReads(long n) {
        return new NReadsInputStream(n);
    }

    /*
     * Convenience method for testing publishers.
     */
    public static byte[] arrayOfNRandomBytes(long nBytes) {
        return arrayOfNRandomBytes((int) nBytes);
    }

    public static ByteBuffer bufferOfNRandomASCIIBytes(int capacity) {
        String alphaNumeric = "abcdefghijklmnopqrstuvwxyz1234567890";
        StringBuilder builder = new StringBuilder(capacity);
        for (int i = 0; i < capacity; i++) {
            int idx = RANDOM.nextInt(alphaNumeric.length());
            builder.append(alphaNumeric.charAt(idx));
        }
        return ByteBuffer.wrap(builder.toString().getBytes(
                StandardCharsets.US_ASCII));
    }

    /*
     * Returns a simple non-compliant Subscriber.
     *
     * This Subscriber is useful for testing our adaptors and wrappers, to make
     * sure they do not delegate RS compliance to the underlying (and foreign to
     * java.net.http codebase) Subscribers, but rather comply themselves.
     *
     * Here's an example:
     *
     *     public void onSubscribe(Subscription s) {
     *         delegate.onSubscribe(s);
     *     }
     *
     * The snippet above cannot be considered a good implementation of a
     * Subscriber if `delegate` is an unknown Subscriber. In this case the
     * implementation should independently check all the rules from the RS spec
     * related to subscribers.
     */
    public static <T> Subscriber<T> nonCompliantSubscriber() {
        return new Subscriber<>() {

            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(T item) { }

            @Override
            public void onError(Throwable throwable) { }

            @Override
            public void onComplete() { }
        };
    }

    public static int randomIntUpTo(int bound) {
        return RANDOM.nextInt(bound);
    }

    /*
     * Signals an error to its subscribers immediately after subscription.
     */
    public static <T> Publisher<T> newErroredPublisher() {
        return subscriber -> {
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) { }

                @Override
                public void cancel() { }
            });
            subscriber.onError(new IOException());
        };
    }

    /*
     * Publishes the elements obtained from the stream and signals completion.
     * Can be cancelled, but cannot signal an error.
     *
     * This trivial ad-hoc implementation of Publisher was created so as to
     * publish lists of byte buffers. We can publish ByteBuffer, but we can't
     * seem to publish List<ByteBuffer> since there's no readily available
     * publisher of those, nor there's a simple adaptor.
     */
    public static <T> Publisher<T> publisherOfStream(Stream<? extends T> stream)
    {
        if (stream == null) {
            throw new NullPointerException();
        }
        return new Publisher<T>() {
            @Override
            public void subscribe(Subscriber<? super T> subscriber) {
                if (subscriber == null) {
                    throw new NullPointerException();
                }
                Subscription subscription = new Subscription() {

                    boolean inOnNext; // recursion control
                    volatile boolean cancelled;
                    long demand;
                    final Iterator<? extends T> supply = stream.iterator();

                    @Override
                    public void request(long n) {
                        demand = demand + n < 0 ? Long.MAX_VALUE : demand + n;
                        if (inOnNext) {
                            return;
                        }
                        if (cancelled)
                            return;
                        if (n <= 0) {
                            cancelled = true;
                            subscriber.onError(new IllegalArgumentException(
                                    "non-positive subscription request"));
                            return;
                        }
                        while (supply.hasNext() && demand > 0 && !cancelled) {
                            demand--;
                            inOnNext = true;
                            try {
                                T item = supply.next();
                                subscriber.onNext(item);
                            } finally {
                                inOnNext = false;
                            }
                        }
                        if (!supply.hasNext()) {
                            cancelled = true;
                            subscriber.onComplete();
                        }
                    }

                    @Override
                    public void cancel() {
                        cancelled = true;
                    }
                };
                subscriber.onSubscribe(subscription);
            }
        };
    }

    static final class NReadsInputStream extends InputStream {

        private static final int EOF = -1;
        private long readsLeft;

        NReadsInputStream(long n) {
            if (n < 0) {
                throw new IllegalArgumentException(String.valueOf(n));
            }
            this.readsLeft = n;
        }

        @Override
        public int read() {
            if (readsLeft == 0L) {
                return EOF;
            }
            readsLeft--;
            return S.randomIntUpTo(256);
        }

        @Override
        public int read(byte[] b, int off, int len) {
            Objects.checkFromIndexSize(off, len, b.length);
            // Must return 0 if len == 0,
            // even if there are no more reads left
            if (len == 0) {
                return 0;
            }
            if (readsLeft == 0L) {
                return EOF;
            }
            readsLeft--;
            // At least one byte MUST be read, but we can read
            // less than `len` bytes
            int r = RANDOM.nextInt(len) + 1;
            for (int i = 0; i < r; i++) {
                b[i] = (byte) randomIntUpTo(256);
            }
            return r;
        }
    }
}
