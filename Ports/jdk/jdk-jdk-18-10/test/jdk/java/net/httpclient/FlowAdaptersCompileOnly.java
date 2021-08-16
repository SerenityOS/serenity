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

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.function.Function;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;

/*
 * @test
 * @summary Basic test for Flow adapters with generic type parameters
 * @compile FlowAdaptersCompileOnly.java
 */

public class FlowAdaptersCompileOnly {

    static void makesSureDifferentGenericSignaturesCompile() {
        BodyPublishers.fromPublisher(new BBPublisher());
        BodyPublishers.fromPublisher(new MBBPublisher());

        BodyHandlers.fromSubscriber(new ListSubscriber());
        BodyHandlers.fromSubscriber(new CollectionSubscriber());
        BodyHandlers.fromSubscriber(new IterableSubscriber());
        BodyHandlers.fromSubscriber(new ObjectSubscriber());

        BodySubscribers.fromSubscriber(new ListSubscriber());
        BodySubscribers.fromSubscriber(new CollectionSubscriber());
        BodySubscribers.fromSubscriber(new IterableSubscriber());
        BodySubscribers.fromSubscriber(new ObjectSubscriber());

        BodyPublishers.fromPublisher(new BBPublisher(), 1);
        BodyPublishers.fromPublisher(new MBBPublisher(), 1);

        BodyHandlers.fromSubscriber(new ListSubscriber(), Function.identity());
        BodyHandlers.fromSubscriber(new CollectionSubscriber(), Function.identity());
        BodyHandlers.fromSubscriber(new IterableSubscriber(), Function.identity());
        BodyHandlers.fromSubscriber(new ObjectSubscriber(), Function.identity());

        BodySubscribers.fromSubscriber(new ListSubscriber(), Function.identity());
        BodySubscribers.fromSubscriber(new CollectionSubscriber(), Function.identity());
        BodySubscribers.fromSubscriber(new IterableSubscriber(), Function.identity());
        BodySubscribers.fromSubscriber(new ObjectSubscriber(), Function.identity());
    }

    static class BBPublisher implements Flow.Publisher<ByteBuffer> {
        @Override
        public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) { }
    }

    static class MBBPublisher implements Flow.Publisher<MappedByteBuffer> {
        @Override
        public void subscribe(Flow.Subscriber<? super MappedByteBuffer> subscriber) { }
    }

    static class ListSubscriber implements Flow.Subscriber<List<ByteBuffer>> {
        @Override public void onSubscribe(Flow.Subscription subscription) { }
        @Override public void onNext(List<ByteBuffer> item) { }
        @Override public void onError(Throwable throwable) { }
        @Override public void onComplete() { }
    }

    static class CollectionSubscriber implements Flow.Subscriber<Collection<ByteBuffer>> {
        @Override public void onSubscribe(Flow.Subscription subscription) { }
        @Override public void onNext(Collection<ByteBuffer> item) { }
        @Override public void onError(Throwable throwable) { }
        @Override public void onComplete() { }
    }

    static class IterableSubscriber implements Flow.Subscriber<Iterable<ByteBuffer>> {
        @Override public void onSubscribe(Flow.Subscription subscription) { }
        @Override public void onNext(Iterable<ByteBuffer> item) { }
        @Override public void onError(Throwable throwable) { }
        @Override public void onComplete() { }
    }

    static class ObjectSubscriber implements Flow.Subscriber<Object> {
        @Override public void onSubscribe(Flow.Subscription subscription) { }
        @Override public void onNext(Object item) { }
        @Override public void onError(Throwable throwable) { }
        @Override public void onComplete() { }
    }

    // ---

    static final Function<ListSubscriber,Integer> f1 = subscriber -> 1;
    static final Function<ListSubscriber,Number> f2 = subscriber -> 2;
    static final Function<ListSubscriberX,Integer> f3 = subscriber -> 3;
    static final Function<ListSubscriberX,Number> f4 = subscriber -> 4;

    static class ListSubscriberX extends ListSubscriber {
        int getIntegerX() { return 5; }
    }

    static void makesSureDifferentGenericFunctionSignaturesCompile() {
        BodyHandler<Integer> bh01 = BodyHandlers.fromSubscriber(new ListSubscriber(), s -> 6);
        BodyHandler<Number>  bh02 = BodyHandlers.fromSubscriber(new ListSubscriber(), s -> 7);
        BodyHandler<Integer> bh03 = BodyHandlers.fromSubscriber(new ListSubscriber(), f1);
        BodyHandler<Number>  bh04 = BodyHandlers.fromSubscriber(new ListSubscriber(), f1);
        BodyHandler<Number>  bh05 = BodyHandlers.fromSubscriber(new ListSubscriber(), f2);
        BodyHandler<Integer> bh06 = BodyHandlers.fromSubscriber(new ListSubscriberX(), f1);
        BodyHandler<Number>  bh07 = BodyHandlers.fromSubscriber(new ListSubscriberX(), f1);
        BodyHandler<Number>  bh08 = BodyHandlers.fromSubscriber(new ListSubscriberX(), f2);
        BodyHandler<Integer> bh09 = BodyHandlers.fromSubscriber(new ListSubscriberX(), ListSubscriberX::getIntegerX);
        BodyHandler<Number>  bh10 = BodyHandlers.fromSubscriber(new ListSubscriberX(), ListSubscriberX::getIntegerX);
        BodyHandler<Integer> bh11 = BodyHandlers.fromSubscriber(new ListSubscriberX(), f3);
        BodyHandler<Number>  bh12 = BodyHandlers.fromSubscriber(new ListSubscriberX(), f3);
        BodyHandler<Number>  bh13 = BodyHandlers.fromSubscriber(new ListSubscriberX(), f4);

        BodySubscriber<Integer> bs01 = BodySubscribers.fromSubscriber(new ListSubscriber(), s -> 6);
        BodySubscriber<Number>  bs02 = BodySubscribers.fromSubscriber(new ListSubscriber(), s -> 7);
        BodySubscriber<Integer> bs03 = BodySubscribers.fromSubscriber(new ListSubscriber(), f1);
        BodySubscriber<Number>  bs04 = BodySubscribers.fromSubscriber(new ListSubscriber(), f1);
        BodySubscriber<Number>  bs05 = BodySubscribers.fromSubscriber(new ListSubscriber(), f2);
        BodySubscriber<Integer> bs06 = BodySubscribers.fromSubscriber(new ListSubscriberX(), f1);
        BodySubscriber<Number>  bs07 = BodySubscribers.fromSubscriber(new ListSubscriberX(), f1);
        BodySubscriber<Number>  bs08 = BodySubscribers.fromSubscriber(new ListSubscriberX(), f2);
        BodySubscriber<Integer> bs09 = BodySubscribers.fromSubscriber(new ListSubscriberX(), ListSubscriberX::getIntegerX);
        BodySubscriber<Number>  bs10 = BodySubscribers.fromSubscriber(new ListSubscriberX(), ListSubscriberX::getIntegerX);
        BodySubscriber<Integer> bs11 = BodySubscribers.fromSubscriber(new ListSubscriberX(), f3);
        BodySubscriber<Number>  bs12 = BodySubscribers.fromSubscriber(new ListSubscriberX(), f3);
        BodySubscriber<Number>  bs13 = BodySubscribers.fromSubscriber(new ListSubscriberX(), f4);
    }

    // ---

    static class NumberSubscriber implements Flow.Subscriber<List<ByteBuffer>> {
        @Override public void onSubscribe(Flow.Subscription subscription) { }
        @Override public void onNext(List<ByteBuffer> item) { }
        @Override public void onError(Throwable throwable) { }
        @Override public void onComplete() { }
        public Number getNumber() { return null; }
    }

    static class IntegerSubscriber extends NumberSubscriber {
        @Override public void onSubscribe(Flow.Subscription subscription) { }
        @Override public void onNext(List<ByteBuffer> item) { }
        @Override public void onError(Throwable throwable) { }
        @Override public void onComplete() { }
        public Integer getInteger() { return null; }
    }

    static class LongSubscriber extends NumberSubscriber {
        @Override public void onSubscribe(Flow.Subscription subscription) { }
        @Override public void onNext(List<ByteBuffer> item) { }
        @Override public void onError(Throwable throwable) { }
        @Override public void onComplete() { }
        public Long getLong() { return null; }
    }

    static final Function<NumberSubscriber,Number> numMapper = sub -> sub.getNumber();
    static final Function<IntegerSubscriber,Integer> intMapper = sub -> sub.getInteger();
    static final Function<LongSubscriber,Long> longMapper = sub -> sub.getLong();

    public void makesSureDifferentGenericSubscriberSignaturesCompile()
        throws Exception
    {
        HttpClient client = null;
        HttpRequest request = null;
        IntegerSubscriber sub1 = new IntegerSubscriber();

        HttpResponse<Integer> r1 = client.send(request, BodyHandlers.fromSubscriber(sub1, IntegerSubscriber::getInteger));
        HttpResponse<Number>  r2 = client.send(request, BodyHandlers.fromSubscriber(sub1, IntegerSubscriber::getInteger));
        HttpResponse<Number>  r3 = client.send(request, BodyHandlers.fromSubscriber(sub1, NumberSubscriber::getNumber));
        HttpResponse<Integer> r4 = client.send(request, BodyHandlers.fromSubscriber(sub1, intMapper));
        HttpResponse<Number>  r5 = client.send(request, BodyHandlers.fromSubscriber(sub1, intMapper));
        HttpResponse<Number>  r6 = client.send(request, BodyHandlers.fromSubscriber(sub1, numMapper));

        // compiles but makes little sense. Just what you get with any usage of `? super`
        final Function<Object,Number> objectMapper = sub -> 1;
        client.sendAsync(request, BodyHandlers.fromSubscriber(sub1, objectMapper));

        // does not compile, as expected ( uncomment to see )
        //HttpResponse<Number> r7 = client.send(request, BodyHandler.fromSubscriber(sub1, longMapper));
    }
}
