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
import org.reactivestreams.tck.flow.FlowPublisherVerification;

import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow.Publisher;
import java.util.stream.Stream;

/* See TckDriver.java for more information */
public class BodySubscribersOfPublisherPublisher
        extends FlowPublisherVerification<List<ByteBuffer>> {

    public BodySubscribersOfPublisherPublisher() {
        super(new TestEnvironment(450L));
    }

    @Override
    public Publisher<List<ByteBuffer>> createFlowPublisher(long nElements) {
        BodySubscriber<Publisher<List<ByteBuffer>>> sub =
                BodySubscribers.ofPublisher();
        Stream<List<ByteBuffer>> buffers =
                Stream.generate(() -> S.listOfBuffersFromBufferOfNBytes(1024))
                      .limit(nElements);
        Publisher<List<ByteBuffer>> pub = S.publisherOfStream(buffers);
        pub.subscribe(sub);
        return sub.getBody().toCompletableFuture().join();
    }

    @Override
    public Publisher<List<ByteBuffer>> createFailedFlowPublisher() {
        BodySubscriber<Publisher<List<ByteBuffer>>> sub =
                BodySubscribers.ofPublisher();
        Publisher<List<ByteBuffer>> pub = S.newErroredPublisher();
        pub.subscribe(sub);
        return sub.getBody().toCompletableFuture().join();
    }

    @Override
    public long maxElementsFromPublisher() {
        return 21;
    }
}
