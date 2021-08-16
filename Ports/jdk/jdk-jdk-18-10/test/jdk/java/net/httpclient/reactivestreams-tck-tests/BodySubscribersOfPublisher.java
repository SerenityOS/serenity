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

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowSubscriberBlackboxVerification;

import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

/* See TckDriver.java for more information */
public class BodySubscribersOfPublisher
        extends FlowSubscriberBlackboxVerification<List<ByteBuffer>> {

    public BodySubscribersOfPublisher() {
        super(new TestEnvironment(450L));
    }

    /* The reason for overriding this method is that BodySubscribers.ofPublisher
       is somewhat tricky. It is not an independent Subscriber, but rather
       an adaptor from Subscriber to Publisher. Until the Subscriber that
       subscribed to that resulting Publisher requests anything, nothing
       happens. */
    @Override
    public void triggerFlowRequest(
            Subscriber<? super List<ByteBuffer>> subscriber)
    {
        BodySubscriber<Publisher<List<ByteBuffer>>> sub =
                (BodySubscriber<Publisher<List<ByteBuffer>>>) subscriber;
        CompletionStage<Publisher<List<ByteBuffer>>> body = sub.getBody();
        Publisher<List<ByteBuffer>> pub = body.toCompletableFuture().join();
        pub.subscribe(new Subscriber<>() {

            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Integer.MAX_VALUE);
            }

            @Override public void onNext(List<ByteBuffer> item) { }
            @Override public void onError(Throwable throwable) { }
            @Override public void onComplete() { }
        });
    }

    @Override
    public Subscriber<List<ByteBuffer>> createFlowSubscriber() {
        return BodySubscribers.ofPublisher();
    }

    @Override
    public List<ByteBuffer> createElement(int element) {
        return S.listOfBuffersFromBufferOfNBytes(element % 17);
    }
}
