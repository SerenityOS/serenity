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

package jdk.internal.net.http;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.testng.annotations.Test;
import jdk.internal.net.http.common.SubscriberWrapper;

@Test
public class WrapperTest {
    static final int LO_PRI = 1;
    static final int HI_PRI = 2;
    static final int NUM_HI_PRI = 240;
    static final int BUFSIZE = 1016;
    static final int BUFSIZE_INT = BUFSIZE/4;
    static final int HI_PRI_FREQ = 40;

    static final int TOTAL = 10000;
    //static final int TOTAL = 500;

    final SubmissionPublisher<List<ByteBuffer>> publisher;
    final SubscriberWrapper sub1, sub2, sub3;
    final ExecutorService executor = Executors.newCachedThreadPool();
    volatile int hipricount = 0;

    void errorHandler(Flow.Subscriber<? super List<ByteBuffer>> sub, Throwable t) {
        System.err.printf("Exception from %s : %s\n", sub.toString(), t.toString());
    }

    public WrapperTest() {
        publisher = new SubmissionPublisher<>(executor, 600,
                (a, b) -> {
                    errorHandler(a, b);
                });

        CompletableFuture<Void> notif = new CompletableFuture<>();
        LastSubscriber ls = new LastSubscriber(notif);
        sub1 = new Filter1(ls);
        sub2 = new Filter2(sub1);
        sub3 = new Filter2(sub2);
    }

    public class Filter2 extends SubscriberWrapper {
        Filter2(SubscriberWrapper wrapper) {
            super(wrapper);
        }

        // reverse the order of the bytes in each buffer
        public void incoming(List<ByteBuffer> list, boolean complete) {
            List<ByteBuffer> out = new LinkedList<>();
            for (ByteBuffer inbuf : list) {
                int size = inbuf.remaining();
                ByteBuffer outbuf = ByteBuffer.allocate(size);
                for (int i=size; i>0; i--) {
                    byte b = inbuf.get(i-1);
                    outbuf.put(b);
                }
                outbuf.flip();
                out.add(outbuf);
            }
            if (complete) System.out.println("Filter2.complete");
            outgoing(out, complete);
        }

        protected long windowUpdate(long currval) {
            return currval == 0 ? 1 : 0;
        }
    }

    volatile int filter1Calls = 0; // every third call we insert hi pri data

    ByteBuffer getHiPri(int val) {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putInt(HI_PRI);
        buf.putInt(val);
        buf.flip();
        return buf;
    }

    volatile int hiPriAdded = 0;

    public class Filter1 extends SubscriberWrapper {
        Filter1(Flow.Subscriber<List<ByteBuffer>> downstreamSubscriber)
        {
            super();
            subscribe(downstreamSubscriber);
        }

        // Inserts up to NUM_HI_PRI hi priority buffers into flow
        protected void incoming(List<ByteBuffer> in, boolean complete) {
            if ((++filter1Calls % HI_PRI_FREQ) == 0 && (hiPriAdded++ < NUM_HI_PRI)) {
                sub1.outgoing(getHiPri(hipricount++), false);
            }
            // pass data thru
            if (complete) System.out.println("Filter1.complete");
            outgoing(in, complete);
        }

        protected long windowUpdate(long currval) {
            return currval == 0 ? 1 : 0;
        }
    }

    /**
     * Final subscriber in the chain. Compares the data sent by the original
     * publisher.
     */
    static public class LastSubscriber implements Flow.Subscriber<List<ByteBuffer>> {
        volatile Flow.Subscription subscription;
        volatile int hipriCounter=0;
        volatile int lopriCounter=0;
        final CompletableFuture<Void> cf;

        LastSubscriber(CompletableFuture<Void> cf) {
            this.cf = cf;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(50); // say
        }

        private void error(String...args) {
            StringBuilder sb = new StringBuilder();
            for (String s : args) {
                sb.append(s);
                sb.append(' ');
            }
            String msg = sb.toString();
            System.out.println("Error: " + msg);
            RuntimeException e = new RuntimeException(msg);
            cf.completeExceptionally(e);
            subscription.cancel(); // This is where we need a variant that include exception
        }

        private void check(ByteBuffer buf) {
            int type = buf.getInt();
            if (type == HI_PRI) {
                // check next int is hi pri counter
                int c = buf.getInt();
                if (c != hipriCounter)
                    error("hi pri counter", Integer.toString(c), Integer.toString(hipriCounter));
                hipriCounter++;
            } else {
                while (buf.hasRemaining()) {
                    if (buf.getInt() != lopriCounter)
                        error("lo pri counter", Integer.toString(lopriCounter));
                    lopriCounter++;
                }
            }
        }

        @Override
        public void onNext(List<ByteBuffer> items) {
            for (ByteBuffer item : items)
                check(item);
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            error(throwable.getMessage());
        }

        @Override
        public void onComplete() {
            if (hipriCounter != NUM_HI_PRI)
                error("hi pri at end wrong", Integer.toString(hipriCounter), Integer.toString(NUM_HI_PRI));
            else {
                System.out.println("LastSubscriber.complete");
                cf.complete(null); // success
            }
        }
    }

    List<ByteBuffer> getBuffer(int c) {
        ByteBuffer buf = ByteBuffer.allocate(BUFSIZE+4);
        buf.putInt(LO_PRI);
        for (int i=0; i<BUFSIZE_INT; i++) {
            buf.putInt(c++);
        }
        buf.flip();
        return List.of(buf);
    }

    boolean errorTest = false;

    @Test
    public void run() throws InterruptedException {
        try {
            CompletableFuture<Void> completion = sub3.completion();
            publisher.subscribe(sub3);
            // now submit a load of data
            int counter = 0;
            for (int i = 0; i < TOTAL; i++) {
                List<ByteBuffer> bufs = getBuffer(counter);
                //if (i==2)
                    //bufs.get(0).putInt(41, 1234); // error
                counter += BUFSIZE_INT;
                publisher.submit(bufs);
                //if (i % 1000 == 0)
                    //Thread.sleep(1000);
                //if (i == 99) {
                    //publisher.closeExceptionally(new RuntimeException("Test error"));
                    //errorTest = true;
                    //break;
                //}
            }
            if (!errorTest) {
                publisher.close();
            }
            System.out.println("Publisher completed");
            completion.join();
            System.out.println("Subscribers completed ok");
        } finally {
            executor.shutdownNow();
        }
    }

    static void display(CompletableFuture<?> cf) {
        System.out.print (cf);
        if (!cf.isDone())
            return;
        try {
            cf.join(); // wont block
        } catch (Exception e) {
            System.out.println(" " + e);
        }
    }

/*
    public static void main(String[] args) throws InterruptedException {
        WrapperTest test = new WrapperTest();
        test.run();
    }
*/
}
