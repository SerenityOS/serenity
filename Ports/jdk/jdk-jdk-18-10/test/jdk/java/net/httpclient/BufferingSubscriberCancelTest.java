/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.IntSupplier;
import java.util.stream.IntStream;
import java.net.http.HttpResponse.BodySubscriber;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.Long.MAX_VALUE;
import static java.lang.Long.MIN_VALUE;
import static java.lang.System.out;
import static java.nio.ByteBuffer.wrap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.net.http.HttpResponse.BodySubscribers.buffering;
import static org.testng.Assert.*;

/*
 * @test
 * @summary Direct test for HttpResponse.BodySubscriber.buffering() cancellation
 * @run testng/othervm BufferingSubscriberCancelTest
 */

public class BufferingSubscriberCancelTest {

    @DataProvider(name = "bufferSizes")
    public Object[][] bufferSizes() {
        return new Object[][]{
            // bufferSize should be irrelevant
            {1}, {100}, {511}, {512}, {513}, {1024}, {2047}, {2048}
        };
    }

    @Test(dataProvider = "bufferSizes")
    public void cancelWithoutAnyItemsPublished(int bufferSize) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        SubmissionPublisher<List<ByteBuffer>> publisher =
                new SubmissionPublisher<>(executor, 1);

        CountDownLatch gate = new CountDownLatch(1);  // single onSubscribe
        ExposingSubscriber exposingSubscriber = new ExposingSubscriber(gate);
        BodySubscriber subscriber = buffering(exposingSubscriber, bufferSize);
        publisher.subscribe(subscriber);
        gate.await(30, SECONDS);
        assertEqualsWithRetry(publisher::getNumberOfSubscribers, 1);
        exposingSubscriber.subscription.cancel();
        assertEqualsWithRetry(publisher::getNumberOfSubscribers, 0);

        // further cancels/requests should be a no-op
        Subscription s = exposingSubscriber.subscription;
        s.cancel(); s.request(1);
        s.cancel(); s.request(100); s.cancel();
        s.cancel(); s.request(MAX_VALUE); s.cancel(); s.cancel();
        s.cancel(); s.cancel(); s.cancel(); s.cancel();
        s.request(MAX_VALUE); s.request(MAX_VALUE); s.request(MAX_VALUE);
        s.request(-1); s.request(-100); s.request(MIN_VALUE);
        assertEqualsWithRetry(publisher::getNumberOfSubscribers, 0);
        executor.shutdown();
    }

    @DataProvider(name = "sizeAndItems")
    public Object[][] sizeAndItems() {
        return new Object[][] {
            // bufferSize and item bytes must be equal to count onNext calls
            // bufferSize        items
            { 1,   List.of(wrap(new byte[] { 1 }))                             },
            { 2,   List.of(wrap(new byte[] { 1, 2 }))                          },
            { 3,   List.of(wrap(new byte[] { 1, 2, 3}))                        },
            { 4,   List.of(wrap(new byte[] { 1, 2 , 3, 4}))                    },
            { 5,   List.of(wrap(new byte[] { 1, 2 , 3, 4, 5}))                 },
            { 6,   List.of(wrap(new byte[] { 1, 2 , 3, 4, 5, 6}))              },
            { 7,   List.of(wrap(new byte[] { 1, 2 , 3, 4, 5, 6, 7}))           },
            { 8,   List.of(wrap(new byte[] { 1, 2 , 3, 4, 5, 6, 7, 8}))        },
            { 9,   List.of(wrap(new byte[] { 1, 2 , 3, 4, 5, 6, 7, 8, 9}))     },
            { 10,  List.of(wrap(new byte[] { 1, 2 , 3, 4, 5, 6, 7, 8, 9, 10})) },
        };
    }

    @Test(dataProvider = "sizeAndItems")
    public void cancelWithItemsPublished(int bufferSize, List<ByteBuffer> items)
        throws Exception
    {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        SubmissionPublisher<List<ByteBuffer>> publisher =
                new SubmissionPublisher<>(executor, 24);

        final int ITERATION_TIMES = 10;
        // onSubscribe + onNext ITERATION_TIMES
        CountDownLatch gate = new CountDownLatch(1 + ITERATION_TIMES);
        ExposingSubscriber exposingSubscriber = new ExposingSubscriber(gate);
        BodySubscriber subscriber = buffering(exposingSubscriber, bufferSize);
        publisher.subscribe(subscriber);

        assertEqualsWithRetry(publisher::getNumberOfSubscribers, 1);
        IntStream.range(0, ITERATION_TIMES).forEach(x -> publisher.submit(items));
        gate.await(30, SECONDS);
        exposingSubscriber.subscription.cancel();
        IntStream.range(0, ITERATION_TIMES+1).forEach(x -> publisher.submit(items));

        assertEqualsWithRetry(publisher::getNumberOfSubscribers, 0);
        assertEquals(exposingSubscriber.onNextInvocations, ITERATION_TIMES);
        executor.shutdown();
    }

    // same as above but with more racy conditions, do not wait on the gate
    @Test(dataProvider = "sizeAndItems")
    public void cancelWithItemsPublishedNoWait(int bufferSize, List<ByteBuffer> items)
        throws Exception
    {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        SubmissionPublisher<List<ByteBuffer>> publisher =
                new SubmissionPublisher<>(executor, 24);

        final int ITERATION_TIMES = 10;
        // any callback will so, since onSub is guaranteed to be before onNext
        CountDownLatch gate = new CountDownLatch(1);
        ExposingSubscriber exposingSubscriber = new ExposingSubscriber(gate);
        BodySubscriber subscriber = buffering(exposingSubscriber, bufferSize);
        publisher.subscribe(subscriber);

        IntStream.range(0, ITERATION_TIMES).forEach(x -> publisher.submit(items));
        gate.await(30, SECONDS);
        exposingSubscriber.subscription.cancel();
        IntStream.range(0, ITERATION_TIMES+1).forEach(x -> publisher.submit(items));

        int onNextInvocations = exposingSubscriber.onNextInvocations;
        assertTrue(onNextInvocations <= ITERATION_TIMES,
                   "Expected <= " + ITERATION_TIMES + ", got " + onNextInvocations);
        executor.shutdown();
    }

    static class ExposingSubscriber implements BodySubscriber<Void> {
        final CountDownLatch gate;
        volatile Subscription subscription;
        volatile int onNextInvocations;

        ExposingSubscriber(CountDownLatch gate) {
            this.gate = gate;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            //out.println("onSubscribe " + subscription);
            this.subscription = subscription;
            gate.countDown();
            subscription.request(MAX_VALUE); // forever
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            //out.println("onNext " + item);
            onNextInvocations++;
            gate.countDown();
        }

        @Override
        public void onError(Throwable throwable) {
            out.println("onError " + throwable);
        }

        @Override
        public void onComplete() {
            out.println("onComplete ");
        }

        @Override
        public CompletionStage<Void> getBody() {
            throw new UnsupportedOperationException("getBody is unsupported");
        }
    }

    // There is a race between cancellation and subscriber callbacks, the
    // following mechanism retries a number of times to allow for this race. The
    // only requirement is that the expected result is actually observed.

    static final int TEST_RECHECK_TIMES = 30;

    static void assertEqualsWithRetry(IntSupplier actualSupplier, int expected)
        throws Exception
    {
        int actual = expected + 1; // anything other than expected
        for (int i=0; i< TEST_RECHECK_TIMES; i++) {
            actual = actualSupplier.getAsInt();
            if (actual == expected)
                return;
            Thread.sleep(100);
        }
        assertEquals(actual, expected); // will fail with the usual testng message
    }
}
