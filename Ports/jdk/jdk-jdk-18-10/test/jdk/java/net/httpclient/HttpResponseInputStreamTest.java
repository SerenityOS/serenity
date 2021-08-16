/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.io.UncheckedIOException;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodySubscriber;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/*
 * @test
 * @bug 8197564 8228970
 * @summary Simple smoke test for BodySubscriber.asInputStream();
 * @run testng/othervm HttpResponseInputStreamTest
 * @author daniel fuchs
 */
public class HttpResponseInputStreamTest {
    static final Class<NullPointerException> NPE = NullPointerException.class;
    static final Class<IndexOutOfBoundsException> OOB = IndexOutOfBoundsException.class;

    static class TestException extends IOException {}

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        testOnError();
    }

    /**
     * Tests that a client will be unblocked and will throw an IOException
     * if an error occurs while the client is waiting for more data.
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    public static void testOnError() throws InterruptedException, ExecutionException {
        CountDownLatch latch = new CountDownLatch(1);
        BodySubscriber<InputStream> isb = BodySubscribers.ofInputStream();
        ErrorTestSubscription s = new ErrorTestSubscription(isb);
        CompletionStage<Throwable> cs =
                isb.getBody().thenApplyAsync((is) -> s.accept(latch, is));
        latch.await();
        isb.onSubscribe(s);
        s.t.join();
        Throwable result = cs.toCompletableFuture().get();
        Throwable t = result;
        if (!(t instanceof IOException)) {
            throw new RuntimeException("Failed to receive excpected IOException", result);
        } else {
            System.out.println("Got expected exception: " + t);
        }
        while (t != null) {
            if (t instanceof TestException) break;
            t = t.getCause();
        }
        if (t instanceof TestException) {
            System.out.println("Got expected cause: " + t);
        } else {
            throw new RuntimeException("Failed to receive excpected TestException", result);
        }
    }

    static class ErrorTestSubscription implements Flow.Subscription {
        final BodySubscriber<InputStream> isb;
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    // Give time to
                    System.out.println("waiting...");
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }
                System.out.println("Calling onError...");
                isb.onError(new TestException());
            }
        };

        ErrorTestSubscription(BodySubscriber<InputStream> isb) {
            this.isb = isb;
        }

        int requested = 0;

        @Override
        public void request(long n) {
            System.out.println("Got request: " + n);
            if (requested == 0 && n > 0) {
                //isb.onNext(List.of(java.nio.ByteBuffer.wrap(new byte[] {0x01})));
                requested += n;
                t.start();
            }
        }

        @Override
        public void cancel() {
        }

        public Throwable accept(CountDownLatch latch, InputStream is) {
            System.out.println("got " + is);
            try {
                latch.countDown();
                System.out.println("reading all bytes");
                is.readAllBytes();
                System.out.println("all bytes read");
            } catch (IOException e) {
                return e;
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    return e;
                }
            }
            return is == null ? new NullPointerException() : null;
        }
    }

    static InputStream close(InputStream is) {
        try {
            is.close();
        } catch (IOException io) {
            throw new CompletionException(io);
        }
        return is;
    }

    @Test
    public static void testCloseAndSubscribe()
            throws InterruptedException, ExecutionException
    {
        BodySubscriber<InputStream> isb = BodySubscribers.ofInputStream();
        TestCancelOnCloseSubscription s = new TestCancelOnCloseSubscription();
        InputStream is = isb.getBody()
                .thenApply(HttpResponseInputStreamTest::close)
                .toCompletableFuture()
                .get();
        isb.onSubscribe(s);
        System.out.println(s);
        if (!s.cancelled.get()) {
            throw new RuntimeException("subscription not cancelled");
        }
        if (s.request.get() > 0) {
            throw new RuntimeException("subscription has demand");
        }
    }

    static byte[] readAllBytes(InputStream is) {
        try {
            return is.readAllBytes();
        } catch (IOException io) {
            io.printStackTrace();
            throw new CompletionException(io);
        }
    }

    @Test
    public static void testReadParameters() throws InterruptedException, ExecutionException, IOException {
        BodySubscriber<InputStream> isb = BodySubscribers.ofInputStream();
        InputStream is = isb.getBody().toCompletableFuture().get();

        Throwable ex;

        // len == 0
        assertEquals(is.read(new byte[16], 0, 0), 0);
        assertEquals(is.read(new byte[16], 16, 0), 0);

        // index == -1
        ex = expectThrows(OOB, () -> is.read(new byte[16], -1, 10));
        System.out.println("OutOfBoundsException thrown as expected: " + ex);

        // large offset
        ex = expectThrows(OOB, () -> is.read(new byte[16], 17, 10));
        System.out.println("OutOfBoundsException thrown as expected: " + ex);

        ex = expectThrows(OOB, () -> is.read(new byte[16], 10, 10));
        System.out.println("OutOfBoundsException thrown as expected: " + ex);

        // null value
        ex = expectThrows(NPE, () -> is.read(null, 0, 10));
        System.out.println("NullPointerException thrown as expected: " + ex);
    }

    @Test
    public static void testSubscribeAndClose()
            throws InterruptedException, ExecutionException
    {
        BodySubscriber<InputStream> isb = BodySubscribers.ofInputStream();
        TestCancelOnCloseSubscription s = new TestCancelOnCloseSubscription();
        InputStream is = isb.getBody().toCompletableFuture().get();
        isb.onSubscribe(s);
        if (s.cancelled.get()) {
            throw new RuntimeException("subscription cancelled");
        }
        CompletableFuture<String> cf = CompletableFuture.supplyAsync(
                () -> HttpResponseInputStreamTest.readAllBytes(is))
                .thenApply(String::new);
        while (s.request.get() == 0) {
            Thread.sleep(100);
        }
        isb.onNext(List.of(ByteBuffer.wrap("coucou".getBytes())));
        close(is);
        System.out.println(s);
        if (!s.cancelled.get()) {
            throw new RuntimeException("subscription not cancelled");
        }
        if (s.request.get() == 0) {
            throw new RuntimeException("subscription has no demand");
        }
        try {
            System.out.println("read " + cf.get() + "!");
            throw new RuntimeException("expected IOException not raised");
        } catch (ExecutionException | CompletionException x) {
            if (x.getCause() instanceof IOException) {
                System.out.println("Got expected IOException: " + x.getCause());
            } else {
                throw x;
            }
        }
    }

    static class TestCancelOnCloseSubscription implements Flow.Subscription {
        final AtomicLong request = new AtomicLong();
        final AtomicBoolean cancelled = new AtomicBoolean();

        @Override
        public void request(long n) {
            request.addAndGet(n);
        }

        @Override
        public void cancel() {
            cancelled.set(true);
        }

        @Override
        public String toString() {
            return String.format("%s(request=%d, cancelled=%s)",
                    this.getClass().getSimpleName(),
                    request.get(),
                    cancelled.get());
        }
    }
}
