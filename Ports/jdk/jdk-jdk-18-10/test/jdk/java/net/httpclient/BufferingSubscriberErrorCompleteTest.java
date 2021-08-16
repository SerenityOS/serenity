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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.Phaser;
import java.util.concurrent.SubmissionPublisher;
import java.util.stream.IntStream;
import java.net.http.HttpResponse.BodySubscriber;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.Long.MAX_VALUE;
import static java.lang.Long.MIN_VALUE;
import static java.nio.ByteBuffer.wrap;
import static java.net.http.HttpResponse.BodySubscribers.buffering;
import static org.testng.Assert.*;

/*
 * @test
 * @summary Test for HttpResponse.BodySubscriber.buffering() onError/onComplete
 * @run testng/othervm BufferingSubscriberErrorCompleteTest
 */

public class BufferingSubscriberErrorCompleteTest {

    @DataProvider(name = "illegalDemand")
    public Object[][] illegalDemand() {
        return new Object[][]{
            {0L}, {-1L}, {-5L}, {-100L}, {-101L}, {-100_001L}, {MIN_VALUE}
        };
    }

    @Test(dataProvider = "illegalDemand")
    public void illegalRequest(long demand) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        SubmissionPublisher<List<ByteBuffer>> publisher =
                new SubmissionPublisher<>(executor, 1);

        Phaser gate = new Phaser(2);  // single onSubscribe and onError
        ExposingSubscriber exposingSubscriber = new ExposingSubscriber(gate);
        BodySubscriber subscriber = buffering(exposingSubscriber, 1);
        publisher.subscribe(subscriber);
        gate.arriveAndAwaitAdvance();

        Subscription s = exposingSubscriber.subscription;
        int previous = exposingSubscriber.onErrorInvocations;
        s.request(demand);
        gate.arriveAndAwaitAdvance();

        assertEquals(previous + 1, exposingSubscriber.onErrorInvocations);
        assertTrue(exposingSubscriber.throwable instanceof IllegalArgumentException,
                "Expected IAE, got:" + exposingSubscriber.throwable);

        furtherCancelsRequestsShouldBeNoOp(s);
        assertEquals(exposingSubscriber.onErrorInvocations, 1);
        executor.shutdown();
    }


    @DataProvider(name = "bufferAndItemSizes")
    public Object[][] bufferAndItemSizes() {
        List<Object[]> values = new ArrayList<>();

        for (int bufferSize : new int[] { 1, 5, 10, 100, 1000 })
            for (int items : new int[]  { 0, 1, 2, 5, 9, 10, 11, 15, 29, 99 })
                values.add(new Object[] { bufferSize, items });

        return values.stream().toArray(Object[][]::new);
    }

    @Test(dataProvider = "bufferAndItemSizes")
    public void onErrorFromPublisher(int bufferSize,
                                     int numberOfItems)
        throws Exception
    {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        SubmissionPublisher<List<ByteBuffer>> publisher =
                new SubmissionPublisher<>(executor, 1);

        // onSubscribe + onError + this thread
        Phaser gate = new Phaser(3);
        ExposingSubscriber exposingSubscriber = new ExposingSubscriber(gate);
        BodySubscriber subscriber = buffering(exposingSubscriber, bufferSize);
        publisher.subscribe(subscriber);

        List<ByteBuffer> item = List.of(wrap(new byte[] { 1 }));
        IntStream.range(0, numberOfItems).forEach(x -> publisher.submit(item));
        Throwable t = new Throwable("a message from me to me");
        publisher.closeExceptionally(t);

        gate.arriveAndAwaitAdvance();

        Subscription s = exposingSubscriber.subscription;

        assertEquals(exposingSubscriber.onErrorInvocations, 1);
        assertEquals(exposingSubscriber.onCompleteInvocations, 0);
        assertEquals(exposingSubscriber.throwable, t);
        assertEquals(exposingSubscriber.throwable.getMessage(),
                     "a message from me to me");

        furtherCancelsRequestsShouldBeNoOp(s);
        assertEquals(exposingSubscriber.onErrorInvocations, 1);
        assertEquals(exposingSubscriber.onCompleteInvocations, 0);
        executor.shutdown();
    }

    @Test(dataProvider = "bufferAndItemSizes")
    public void onCompleteFromPublisher(int bufferSize,
                                        int numberOfItems)
        throws Exception
    {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        SubmissionPublisher<List<ByteBuffer>> publisher =
                new SubmissionPublisher<>(executor, 1);

        // onSubscribe + onComplete + this thread
        Phaser gate = new Phaser(3);
        ExposingSubscriber exposingSubscriber = new ExposingSubscriber(gate);
        BodySubscriber subscriber = buffering(exposingSubscriber, bufferSize);
        publisher.subscribe(subscriber);

        List<ByteBuffer> item = List.of(wrap(new byte[] { 1 }));
        IntStream.range(0, numberOfItems).forEach(x -> publisher.submit(item));
        publisher.close();

        gate.arriveAndAwaitAdvance();

        Subscription s = exposingSubscriber.subscription;

        assertEquals(exposingSubscriber.onErrorInvocations, 0);
        assertEquals(exposingSubscriber.onCompleteInvocations, 1);
        assertEquals(exposingSubscriber.throwable, null);

        furtherCancelsRequestsShouldBeNoOp(s);
        assertEquals(exposingSubscriber.onErrorInvocations, 0);
        assertEquals(exposingSubscriber.onCompleteInvocations, 1);
        assertEquals(exposingSubscriber.throwable, null);
        executor.shutdown();
    }

    static class ExposingSubscriber implements BodySubscriber<Void> {
        final Phaser gate;
        volatile Subscription subscription;
        volatile int onNextInvocations;
        volatile int onErrorInvocations;
        volatile int onCompleteInvocations;
        volatile Throwable throwable;

        ExposingSubscriber(Phaser gate) {
            this.gate = gate;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            //out.println("onSubscribe " + subscription);
            this.subscription = subscription;
            subscription.request(MAX_VALUE);
            gate.arrive();
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            //out.println("onNext " + item);
            onNextInvocations++;
        }

        @Override
        public void onError(Throwable throwable) {
            //out.println("onError " + throwable);
            this.throwable = throwable;
            onErrorInvocations++;
            gate.arrive();
        }

        @Override
        public void onComplete() {
            //out.println("onComplete ");
            onCompleteInvocations++;
            gate.arrive();
        }

        @Override
        public CompletionStage<Void> getBody() {
            throw new UnsupportedOperationException("getBody is unsupported");
        }
    }

    static void furtherCancelsRequestsShouldBeNoOp(Subscription s) {
        s.cancel(); s.request(1);
        s.cancel(); s.request(100); s.cancel();
        s.cancel(); s.request(MAX_VALUE); s.cancel(); s.cancel();
        s.cancel(); s.cancel(); s.cancel(); s.cancel();
        s.request(MAX_VALUE); s.request(MAX_VALUE); s.request(MAX_VALUE);
        s.request(-1); s.request(-100); s.request(MIN_VALUE);
    }
}
