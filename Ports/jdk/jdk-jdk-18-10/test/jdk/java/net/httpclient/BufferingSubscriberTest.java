/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.BiConsumer;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import jdk.test.lib.RandomFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.Long.MAX_VALUE;
import static java.lang.Long.min;
import static java.lang.System.out;
import static java.util.concurrent.CompletableFuture.delayedExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.testng.Assert.*;

/*
 * @test
 * @bug 8184285
 * @summary Direct test for HttpResponse.BodySubscriber.buffering() API
 * @key randomness
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run testng/othervm -Djdk.internal.httpclient.debug=true BufferingSubscriberTest
 */

public class BufferingSubscriberTest {

    // If we compute that a test will take less that 10s
    // we judge it acceptable
    static final long LOWER_THRESHOLD = 10_000; // 10 sec.
    // If we compute that a test will take more than 20 sec
    // we judge it problematic: we will try to adjust the
    // buffer sizes, and if we can't we will print a warning
    static final long UPPER_THRESHOLD = 20_000; // 20 sec.

    static final Random random = RandomFactory.getRandom();
    static final long start = System.nanoTime();
    static final String START = "start";
    static final String END   = "end  ";
    static long elapsed() { return (System.nanoTime() - start)/1000_000;}
    static void printStamp(String what, String fmt, Object... args) {
        long elapsed = elapsed();
        long sec = elapsed/1000;
        long ms  = elapsed % 1000;
        String time = sec > 0 ? sec + "sec " : "";
        time = time + ms + "ms";
        out.println(what + "\t ["+time+"]\t "+ String.format(fmt,args));
    }
    @DataProvider(name = "negatives")
    public Object[][] negatives() {
        return new Object[][] {  { 0 }, { -1 }, { -1000 } };
    }

    @Test(dataProvider = "negatives", expectedExceptions = IllegalArgumentException.class)
    public void subscriberThrowsIAE(int bufferSize) {
        printStamp(START, "subscriberThrowsIAE(%d)", bufferSize);
        try {
            BodySubscriber<?> bp = BodySubscribers.ofByteArray();
            BodySubscribers.buffering(bp, bufferSize);
        } finally {
            printStamp(END, "subscriberThrowsIAE(%d)", bufferSize);
        }
    }

    @Test(dataProvider = "negatives", expectedExceptions = IllegalArgumentException.class)
    public void handlerThrowsIAE(int bufferSize) {
        printStamp(START, "handlerThrowsIAE(%d)", bufferSize);
        try {
            BodyHandler<?> bp = BodyHandlers.ofByteArray();
            BodyHandlers.buffering(bp, bufferSize);
        } finally {
            printStamp(END, "handlerThrowsIAE(%d)", bufferSize);
        }
    }

    // ---

    @DataProvider(name = "config")
    public Object[][] config() {
        return new Object[][] {
            // iterations delayMillis numBuffers bufferSize maxBufferSize minBufferSize
            { 1,              0,          1,         1,         2,            1   },
            { 1,              5,          1,         100,       2,            1   },
            { 1,              0,          1,         10,        1000,         1   },
            { 1,              10,         1,         10,        1000,         1   },
            { 1,              0,          1,         1000,      1000,         10  },
            { 1,              0,          10,        1000,      1000,         50  },
            { 1,              0,          1000,      10 ,       1000,         50  },
            { 1,              100,        1,         1000 * 4,  1000,         50  },
            { 100,            0,          1000,      1,         2,            1   },
            { 3,              0,          4,         5006,      1000,         50  },
            { 20,             0,          100,       4888,      1000,         100 },
            { 16,             10,         1000,      50 ,       1000,         100 },
            { 16,             10,         1000,      50 ,       657,          657 },
        };
    }

    @Test(dataProvider = "config")
    public void test(int iterations,
                     int delayMillis,
                     int numBuffers,
                     int bufferSize,
                     int maxBufferSize,
                     int minbufferSize) {
        for (long perRequestAmount : new long[] { 1L, MAX_VALUE })
            test(iterations,
                 delayMillis,
                 numBuffers,
                 bufferSize,
                 maxBufferSize,
                 minbufferSize,
                 perRequestAmount);
    }

    volatile boolean onNextThrew;

    BiConsumer<Flow.Subscriber<?>, ? super Throwable> onNextThrowHandler =
            (sub, ex) -> {
                onNextThrew = true;
                System.out.println("onNext threw " + ex);
                ex.printStackTrace();
    };

    public void test(int iterations,
                     int delayMillis,
                     int numBuffers,
                     int bufferSize,
                     int maxBufferSize,
                     int minBufferSize,
                     long requestAmount) {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            out.printf("Iterations %d\n", iterations);
            for (int i=0; i<iterations; i++ ) {
                printStamp(START, "Iteration %d", i);
                try {
                    SubmissionPublisher<List<ByteBuffer>> publisher =
                            new SubmissionPublisher<>(executor,
                                                      1, // lock-step with the publisher, for now
                                                      onNextThrowHandler);
                    CompletableFuture<?> cf = sink(publisher,
                            delayMillis,
                            numBuffers * bufferSize,
                            requestAmount,
                            maxBufferSize,
                            minBufferSize);
                    source(publisher, numBuffers, bufferSize);
                    publisher.close();
                    cf.join();
                } finally {
                    printStamp(END, "Iteration %d\n", i);
                }
            }

            assertFalse(onNextThrew, "Unexpected onNextThrew, check output");

            out.println("OK");
        } finally {
            executor.shutdown();
        }
    }

    static long accumulatedDataSize(List<ByteBuffer> bufs) {
        return bufs.stream().mapToLong(ByteBuffer::remaining).sum();
    }

    /** Returns a new BB with its contents set to monotonically increasing
     * values, staring at the given start index and wrapping every 100. */
    static ByteBuffer allocateBuffer(int size, int startIdx) {
        ByteBuffer b = ByteBuffer.allocate(size);
        for (int i=0; i<size; i++)
            b.put((byte)((startIdx + i) % 100));
        b.position(0);
        return b;
    }

    static class TestSubscriber implements BodySubscriber<Integer> {
        final int delayMillis;
        final int bufferSize;
        final int expectedTotalSize;
        final long requestAmount;
        final CompletableFuture<Integer> completion;
        final Executor delayedExecutor;
        volatile Flow.Subscription subscription;

        TestSubscriber(int bufferSize,
                       int delayMillis,
                       int expectedTotalSize,
                       long requestAmount) {
            this.bufferSize = bufferSize;
            this.completion = new CompletableFuture<>();
            this.delayMillis = delayMillis;
            this.delayedExecutor = delayedExecutor(delayMillis, MILLISECONDS);
            this.expectedTotalSize = expectedTotalSize;
            this.requestAmount = requestAmount;
        }

        /**
         * Example of a factory method which would decorate a buffering
         * subscriber to create a new subscriber dependent on buffering capability.
         * <p>
         * The integer type parameter simulates the body just by counting the
         * number of bytes in the body.
         */
        static BodySubscriber<Integer> createSubscriber(int bufferSize,
                                                        int delay,
                                                        int expectedTotalSize,
                                                        long requestAmount) {
            TestSubscriber s = new TestSubscriber(bufferSize,
                                                delay,
                                                expectedTotalSize,
                                                requestAmount);
            return BodySubscribers.buffering(s, bufferSize);
        }

        private void requestMore() {
            subscription.request(requestAmount);
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            assertNull(this.subscription);
            this.subscription = subscription;
            if (delayMillis > 0)
                delayedExecutor.execute(this::requestMore);
            else
                requestMore();
        }

        volatile int wrongSizes;
        volatile int totalBytesReceived;
        volatile int onNextInvocations;
        volatile long lastSeenSize = -1;
        volatile boolean noMoreOnNext; // false
        volatile int index; // 0
        volatile long count;

        @Override
        public void onNext(List<ByteBuffer> items) {
            try {
                long sz = accumulatedDataSize(items);
                boolean printStamp = delayMillis > 0
                        && requestAmount < Long.MAX_VALUE
                        && count % 20 == 0;
                if (printStamp) {
                    printStamp("stamp", "count=%d sz=%d accumulated=%d",
                            count, sz, (totalBytesReceived + sz));
                }
                count++;
                onNextInvocations++;
                assertNotEquals(sz, 0L, "Unexpected empty buffers");
                items.stream().forEach(b -> assertEquals(b.position(), 0));
                assertFalse(noMoreOnNext);

                if (sz != bufferSize) {
                    String msg = sz + ", should be less than bufferSize, " + bufferSize;
                    assertTrue(sz < bufferSize, msg);
                    assertTrue(lastSeenSize == -1 || lastSeenSize == bufferSize);
                    noMoreOnNext = true;
                    wrongSizes++;
                    printStamp("onNext",
                            "Possibly received last buffer: sz=%d, accumulated=%d, total=%d",
                            sz, totalBytesReceived, totalBytesReceived + sz);
                } else {
                    assertEquals(sz, bufferSize, "Expected to receive exactly bufferSize");
                }
                lastSeenSize = sz;

                // Ensure expected contents
                for (ByteBuffer b : items) {
                    while (b.hasRemaining()) {
                        assertEquals(b.get(), (byte) (index % 100));
                        index++;
                    }
                }

                totalBytesReceived += sz;
                assertEquals(totalBytesReceived, index);
                if (delayMillis > 0 && ((expectedTotalSize - totalBytesReceived) > bufferSize))
                    delayedExecutor.execute(this::requestMore);
                else
                    requestMore();
            } catch (Throwable t) {
                completion.completeExceptionally(t);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            completion.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            if (wrongSizes > 1) { // allow just the final item to be smaller
                String msg = "Wrong sizes. Expected no more than 1. [" + this + "]";
                completion.completeExceptionally(new Throwable(msg));
            }
            if (totalBytesReceived != expectedTotalSize) {
                String msg = "Wrong number of bytes. [" + this + "]";
                completion.completeExceptionally(new Throwable(msg));
            } else {
                completion.complete(totalBytesReceived);
            }
        }

        @Override
        public CompletionStage<Integer> getBody() {
            return completion;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.toString());
            sb.append(", bufferSize=").append(bufferSize);
            sb.append(", onNextInvocations=").append(onNextInvocations);
            sb.append(", totalBytesReceived=").append(totalBytesReceived);
            sb.append(", expectedTotalSize=").append(expectedTotalSize);
            sb.append(", requestAmount=").append(requestAmount);
            sb.append(", lastSeenSize=").append(lastSeenSize);
            sb.append(", wrongSizes=").append(wrongSizes);
            sb.append(", index=").append(index);
            return sb.toString();
        }
    }

    /**
     * Publishes data, through the given publisher, using the main thread.
     *
     * Note: The executor supplied when creating the SubmissionPublisher provides
     * the threads for executing the Subscribers.
     *
     * @param publisher the publisher
     * @param numBuffers the number of buffers to send ( before splitting in two )
     * @param bufferSize the total size of the data to send ( before splitting in two )
     */
    static void source(SubmissionPublisher<List<ByteBuffer>> publisher,
                       int numBuffers,
                       int bufferSize) {
        printStamp("source","Publishing %d buffers of size %d each", numBuffers, bufferSize);
        int index = 0;
        for (int i=0; i<numBuffers; i++) {
            int chunkSize = random.nextInt(bufferSize);
            ByteBuffer buf1 = allocateBuffer(chunkSize, index);
            index += chunkSize;
            ByteBuffer buf2 = allocateBuffer(bufferSize - chunkSize, index);
            index += bufferSize - chunkSize;
            publisher.submit(List.of(buf1, buf2));
        }
        printStamp("source", "complete");
    }

    /**
     * Creates and subscribes Subscribers that receive data from the given
     * publisher.
     *
     * @param publisher the publisher
     * @param delayMillis time, in milliseconds, to delay the Subscription
     *                    requesting more bytes ( for simulating slow consumption )
     * @param expectedTotalSize the total number of bytes expected to be received
     *                          by the subscribers
     * @return a CompletableFuture which completes when the subscription is complete
     */
    static CompletableFuture<?> sink(SubmissionPublisher<List<ByteBuffer>> publisher,
                                     int delayMillis,
                                     int expectedTotalSize,
                                     long requestAmount,
                                     int maxBufferSize,
                                     int minBufferSize) {
        int bufferSize = chooseBufferSize(maxBufferSize,
                                          minBufferSize,
                                          delayMillis,
                                          expectedTotalSize,
                                          requestAmount);
        assert bufferSize > 0;
        assert bufferSize >= minBufferSize;
        assert bufferSize <= maxBufferSize;
        BodySubscriber<Integer> sub = TestSubscriber.createSubscriber(bufferSize,
                                                                    delayMillis,
                                                                    expectedTotalSize,
                                                                    requestAmount);
        publisher.subscribe(sub);
        printStamp("sink","Subscriber reads data with buffer size: %d", bufferSize);
        out.printf("Subscription delay is %d msec\n", delayMillis);
        long delay = (((long)delayMillis * expectedTotalSize) / bufferSize) / requestAmount;
        out.printf("Minimum total delay is %d sec %d ms\n", delay / 1000, delay % 1000);
        out.printf("Request amount is %d items\n", requestAmount);
        return sub.getBody().toCompletableFuture();
    }

    static int chooseBufferSize(int maxBufferSize,
                                int minBufferSize,
                                int delaysMillis,
                                int expectedTotalSize,
                                long requestAmount) {
        assert minBufferSize > 0 && maxBufferSize > 0 && requestAmount > 0;
        int bufferSize = maxBufferSize == minBufferSize ? maxBufferSize :
                (random.nextInt(maxBufferSize - minBufferSize)
                    + minBufferSize);
        if (requestAmount == Long.MAX_VALUE) return bufferSize;
        long minDelay = (((long)delaysMillis * expectedTotalSize) / maxBufferSize)
                / requestAmount;
        long maxDelay = (((long)delaysMillis * expectedTotalSize) / minBufferSize)
                / requestAmount;
        // if the maximum delay is < 10s just take a random number between min and max.
        if (maxDelay <= LOWER_THRESHOLD) {
            return bufferSize;
        }
        // if minimum delay is greater than 20s then print a warning and use max buffer.
        if (minDelay >= UPPER_THRESHOLD) {
            System.out.println("Warning: minimum delay is "
                    + minDelay/1000 + "sec " + minDelay%1000 + "ms");
            System.err.println("Warning: minimum delay is "
                    + minDelay/1000 + "sec " + minDelay%1000 + "ms");
            return maxBufferSize;
        }
        // maxDelay could be anything, but minDelay is below the UPPER_THRESHOLD
        // try to pick up a buffer size that keeps the delay below the
        // UPPER_THRESHOLD
        while (minBufferSize < maxBufferSize) {
            bufferSize = random.nextInt(maxBufferSize - minBufferSize)
                    + minBufferSize;
            long delay = (((long)delaysMillis * expectedTotalSize) / bufferSize)
                    / requestAmount;
            if (delay < UPPER_THRESHOLD) return bufferSize;
            minBufferSize++;
        }
        return minBufferSize;
    }

    // ---

    /* Main entry point for standalone testing of the main functional test. */
    public static void main(String... args) {
        BufferingSubscriberTest t = new BufferingSubscriberTest();
        for (Object[] objs : t.config())
            t.test((int)objs[0], (int)objs[1], (int)objs[2], (int)objs[3], (int)objs[4], (int)objs[5]);
    }
}
