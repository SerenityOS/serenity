/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.net.http.common.FlowTube;
import jdk.internal.net.http.common.SSLTube;
import jdk.internal.net.http.common.Utils;
import org.testng.annotations.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicLong;

public class AbstractSSLTubeTest extends AbstractRandomTest {

    public static final long COUNTER = 600;
    public static final int LONGS_PER_BUF = 800;
    public static final long TOTAL_LONGS = COUNTER * LONGS_PER_BUF;
    public static final ByteBuffer SENTINEL = ByteBuffer.allocate(0);
    // This is a hack to work around an issue with SubmissionPublisher.
    // SubmissionPublisher will call onComplete immediately without forwarding
    // remaining pending data if SubmissionPublisher.close() is called when
    // there is no demand. In other words, it doesn't wait for the subscriber
    // to pull all the data before calling onComplete.
    // We use a CountDownLatch to figure out when it is safe to call close().
    // This may cause the test to hang if data are buffered.
    protected final CountDownLatch allBytesReceived = new CountDownLatch(1);


    protected static ByteBuffer getBuffer(long startingAt) {
        ByteBuffer buf = ByteBuffer.allocate(LONGS_PER_BUF * 8);
        for (int j = 0; j < LONGS_PER_BUF; j++) {
            buf.putLong(startingAt++);
        }
        buf.flip();
        return buf;
    }

    protected void run(FlowTube server,
                       ExecutorService sslExecutor,
                       CountDownLatch allBytesReceived) throws IOException {
        FlowTube client = new SSLTube(createSSLEngine(true),
                                      sslExecutor,
                                      server);
        SubmissionPublisher<List<ByteBuffer>> p =
                new SubmissionPublisher<>(ForkJoinPool.commonPool(),
                                          Integer.MAX_VALUE);
        FlowTube.TubePublisher begin = p::subscribe;
        CompletableFuture<Void> completion = new CompletableFuture<>();
        EndSubscriber end = new EndSubscriber(TOTAL_LONGS, completion, allBytesReceived);
        client.connectFlows(begin, end);
        /* End of wiring */

        long count = 0;
        System.out.printf("Submitting %d buffer arrays\n", COUNTER);
        System.out.printf("LoopCount should be %d\n", TOTAL_LONGS);
        for (long i = 0; i < COUNTER; i++) {
            ByteBuffer b = getBuffer(count);
            count += LONGS_PER_BUF;
            p.submit(List.of(b));
        }
        System.out.println("Finished submission. Waiting for loopback");
        completion.whenComplete((r,t) -> allBytesReceived.countDown());
        try {
            allBytesReceived.await();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        p.close();
        System.out.println("All bytes received: calling publisher.close()");
        try {
            completion.join();
            System.out.println("OK");
        } finally {
            sslExecutor.shutdownNow();
        }
    }

    protected static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {

        }
    }

    /**
     * The final subscriber which receives the decrypted looped-back data. Just
     * needs to compare the data with what was sent. The given CF is either
     * completed exceptionally with an error or normally on success.
     */
    protected static class EndSubscriber implements FlowTube.TubeSubscriber {

        private static final int REQUEST_WINDOW = 13;
        private static final int SIZEOF_LONG = 8;

        private final long nbytes;
        private final AtomicLong counter = new AtomicLong();
        private final CompletableFuture<?> completion;
        private final CountDownLatch allBytesReceived;
        private volatile Flow.Subscription subscription;
        private long unfulfilled;
        private final ByteBuffer carry; // used if buffers don't break at long boundaries.


        EndSubscriber(long nbytes, CompletableFuture<?> completion,
                      CountDownLatch allBytesReceived) {
            this.nbytes = nbytes;
            this.completion = completion;
            this.allBytesReceived = allBytesReceived;
            this.carry = ByteBuffer.allocate(SIZEOF_LONG);
            carry.position(carry.limit());
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            unfulfilled = REQUEST_WINDOW;
            System.out.println("EndSubscriber request " + REQUEST_WINDOW);
            subscription.request(REQUEST_WINDOW);
        }

        public static String info(List<ByteBuffer> i) {
            StringBuilder sb = new StringBuilder();
            sb.append("size: ").append(Integer.toString(i.size()));
            int x = 0;
            for (ByteBuffer b : i)
                x += b.remaining();
            sb.append(" bytes: ").append(x);
            return sb.toString();
        }

        // Check whether we need bytes from the next buffer to read
        // the next long. If yes, drains the current buffer into the
        // carry and returns true. If no and the current buffer
        // or the carry have enough bytes to read a long, return
        // false.
        private boolean requiresMoreBytes(ByteBuffer buf) {
            // First see if the carry contains some left over bytes
            // from the previous buffer
            if (carry.hasRemaining()) {
                // If so fills up the carry, if we can
                while (carry.hasRemaining() && buf.hasRemaining()) {
                    carry.put(buf.get());
                }
                if (!carry.hasRemaining()) {
                    // The carry is full: we can use it.
                    carry.flip();
                    return false;
                } else {
                    // There was not enough bytes to fill the carry,
                    // continue with next buffer.
                    assert !buf.hasRemaining();
                    return true;
                }
            } else if (buf.remaining() < SIZEOF_LONG) {
                // The carry is empty and the current buffer doesn't
                // have enough bytes: drains it into the carry.
                carry.clear();
                carry.put(buf);
                assert carry.hasRemaining();
                assert !buf.hasRemaining();
                // We still need more bytes from the next buffer.
                return true;
            }
            // We have enough bytes to read a long. No need
            // to read from next buffer.
            assert buf.remaining() >= SIZEOF_LONG;
            return false;
        }

        private long readNextLong(ByteBuffer buf) {
            // either the carry is ready to use (it must have 8 bytes to read)
            // or it must be used up and at the limit.
            assert !carry.hasRemaining() || carry.remaining() == SIZEOF_LONG;
            // either we have a long in the carry, or we have enough bytes in the buffer
            assert carry.remaining() == SIZEOF_LONG || buf.remaining() >= SIZEOF_LONG;

            ByteBuffer source = carry.hasRemaining() ? carry : buf;
            return source.getLong();
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            if (--unfulfilled == (REQUEST_WINDOW / 2)) {
                long req = REQUEST_WINDOW - unfulfilled;
                System.out.println("EndSubscriber request " + req);
                unfulfilled = REQUEST_WINDOW;
                subscription.request(req);
            }

            long currval = counter.get();
            if (currval % 500 == 0) {
                System.out.println("EndSubscriber: " + currval);
            }
            System.out.println("EndSubscriber onNext " + Utils.remaining(buffers));

            for (ByteBuffer buf : buffers) {
                while (buf.hasRemaining()) {
                    // first check if we have enough bytes to
                    // read a long. If not, place the bytes in
                    // the carry and continue with next buffer.
                    if (requiresMoreBytes(buf)) continue;

                    // either we have a long in the carry, or we have
                    // enough bytes in the buffer to read a long.
                    long n = readNextLong(buf);

                    assert !carry.hasRemaining();

                    if (currval > (TOTAL_LONGS - 50)) {
                        System.out.println("End: " + currval);
                    }
                    if (n != currval++) {
                        System.out.println("ERROR at " + n + " != " + (currval - 1));
                        completion.completeExceptionally(new RuntimeException("ERROR"));
                        subscription.cancel();
                        return;
                    }
                }
            }

            counter.set(currval);
            if (currval >= TOTAL_LONGS) {
                allBytesReceived.countDown();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            System.out.println("EndSubscriber onError " + throwable);
            completion.completeExceptionally(throwable);
            allBytesReceived.countDown();
        }

        @Override
        public void onComplete() {
            long n = counter.get();
            if (n != nbytes) {
                System.out.printf("nbytes=%d n=%d\n", nbytes, n);
                completion.completeExceptionally(new RuntimeException("ERROR AT END"));
            } else {
                System.out.println("DONE OK");
                completion.complete(null);
            }
            allBytesReceived.countDown();
        }

        @Override
        public String toString() {
            return "EndSubscriber";
        }
    }

    protected static SSLEngine createSSLEngine(boolean client) throws IOException {
        SSLContext context = (new SimpleSSLContext()).get();
        SSLEngine engine = context.createSSLEngine();
        SSLParameters params = context.getSupportedSSLParameters();
        if (client) {
            params.setApplicationProtocols(new String[]{"proto1", "proto2"}); // server will choose proto2
        } else {
            params.setApplicationProtocols(new String[]{"proto2"}); // server will choose proto2
        }
        engine.setSSLParameters(params);
        engine.setUseClientMode(client);
        return engine;
    }
}
