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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.net.ssl.*;
import jdk.internal.net.http.common.Utils;
import org.testng.annotations.Test;
import jdk.internal.net.http.common.SSLFlowDelegate;

@Test
public class FlowTest extends AbstractRandomTest {

    private final SubmissionPublisher<List<ByteBuffer>> srcPublisher;
    private final ExecutorService executor;
    private static final long COUNTER = 3000;
    private static final int LONGS_PER_BUF = 800;
    static final long TOTAL_LONGS = COUNTER * LONGS_PER_BUF;
    public static final ByteBuffer SENTINEL = ByteBuffer.allocate(0);
    static volatile String alpn;

    // This is a hack to work around an issue with SubmissionPublisher.
    // SubmissionPublisher will call onComplete immediately without forwarding
    // remaining pending data if SubmissionPublisher.close() is called when
    // there is no demand. In other words, it doesn't wait for the subscriber
    // to pull all the data before calling onComplete.
    // We use a CountDownLatch to figure out when it is safe to call close().
    // This may cause the test to hang if data are buffered.
    final CountDownLatch allBytesReceived = new CountDownLatch(1);

    private final CompletableFuture<Void> completion;

    public FlowTest() throws IOException {
        executor = Executors.newCachedThreadPool();
        srcPublisher = new SubmissionPublisher<>(executor, 20,
                                                 this::handlePublisherException);
        SSLContext ctx = (new SimpleSSLContext()).get();
        SSLEngine engineClient = ctx.createSSLEngine();
        SSLParameters params = ctx.getSupportedSSLParameters();
        params.setApplicationProtocols(new String[]{"proto1", "proto2"}); // server will choose proto2
        engineClient.setSSLParameters(params);
        engineClient.setUseClientMode(true);
        completion = new CompletableFuture<>();
        SSLLoopbackSubscriber looper = new SSLLoopbackSubscriber(ctx, executor, allBytesReceived);
        looper.start();
        EndSubscriber end = new EndSubscriber(TOTAL_LONGS, completion, allBytesReceived);
        SSLFlowDelegate sslClient = new SSLFlowDelegate(engineClient, executor, end, looper);
        // going to measure how long handshake takes
        final long start = System.currentTimeMillis();
        var alpnCF = sslClient.alpn()
                .whenComplete((String s, Throwable t) -> {
            if (t != null)
                t.printStackTrace();
            long endTime = System.currentTimeMillis();
            alpn = s;
            println("ALPN: " + alpn);
            long period = (endTime - start);
            printf("Handshake took %d ms", period);
        });
        Subscriber<List<ByteBuffer>> reader = sslClient.upstreamReader();
        Subscriber<List<ByteBuffer>> writer = sslClient.upstreamWriter();
        looper.setReturnSubscriber(reader);
        // now connect all the pieces
        srcPublisher.subscribe(writer);
        String aa = alpnCF.join();
        println("AAALPN = " + aa);
    }

    private void handlePublisherException(Object o, Throwable t) {
        println("Src Publisher exception");
        t.printStackTrace(System.out);
    }

    private static ByteBuffer getBuffer(long startingAt) {
        ByteBuffer buf = ByteBuffer.allocate(LONGS_PER_BUF * 8);
        for (int j = 0; j < LONGS_PER_BUF; j++) {
            buf.putLong(startingAt++);
        }
        buf.flip();
        return buf;
    }

    @Test
    public void run() {
        long count = 0;
        printf("Submitting %d buffer arrays", COUNTER);
        printf("LoopCount should be %d", TOTAL_LONGS);
        for (long i = 0; i < COUNTER; i++) {
            ByteBuffer b = getBuffer(count);
            count += LONGS_PER_BUF;
            srcPublisher.submit(List.of(b));
        }
        println("Finished submission. Waiting for loopback");
        // make sure we don't wait for allBytesReceived in case of error.
        var done = completion.whenComplete((r,t) -> {
            try {
                if (t != null) {
                    println("Completion with error: " + t);
                    t.printStackTrace(System.out);
                } else {
                    println("Successful completion");
                }
            } finally {
                println("allBytesReceived.countDown()");
                allBytesReceived.countDown();
            }
        });

        try {
            allBytesReceived.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        println("All bytes received; latch count:"
                + allBytesReceived.getCount());
        srcPublisher.close();
        try {
            done.join();
            if (!alpn.equals("proto2")) {
                throw new RuntimeException("wrong alpn received");
            }
            println("OK");
        } finally {
            executor.shutdownNow();
        }
    }

/*
    public static void main(String[]args) throws Exception {
        FlowTest test = new FlowTest();
        test.run();
    }
*/

    /**
     * This Subscriber simulates an SSL loopback network. The object itself
     * accepts outgoing SSL encrypted data which is looped back via two sockets
     * (one of which is an SSLSocket emulating a server). The method
     * {@link #setReturnSubscriber(java.util.concurrent.Flow.Subscriber) }
     * is used to provide the Subscriber which feeds the incoming side
     * of SSLFlowDelegate. Three threads are used to implement this behavior
     * and a SubmissionPublisher drives the incoming read side.
     * <p>
     * A thread reads from the buffer, writes
     * to the client j.n.Socket which is connected to a SSLSocket operating
     * in server mode. A second thread loops back data read from the SSLSocket back to the
     * client again. A third thread reads the client socket and pushes the data to
     * a SubmissionPublisher that drives the reader side of the SSLFlowDelegate
     */
    static class SSLLoopbackSubscriber implements Subscriber<List<ByteBuffer>> {
        private final BlockingQueue<ByteBuffer> buffer;
        private final SSLServerSocket serv;
        private final Socket clientSock;
        private final SSLSocket serverSock;
        private final Thread thread1, thread2, thread3;
        private volatile Flow.Subscription clientSubscription;
        private final SubmissionPublisher<List<ByteBuffer>> publisher;
        private final CountDownLatch allBytesReceived;

        SSLLoopbackSubscriber(SSLContext ctx,
                              ExecutorService exec,
                              CountDownLatch allBytesReceived) throws IOException {
            SSLServerSocketFactory fac = ctx.getServerSocketFactory();
            serv = (SSLServerSocket) fac.createServerSocket();
            serv.setReuseAddress(false);
            serv.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            SSLParameters params = serv.getSSLParameters();
            params.setApplicationProtocols(new String[]{"proto2"});
            serv.setSSLParameters(params);


            int serverPort = serv.getLocalPort();
            clientSock = new Socket("localhost", serverPort);
            serverSock = (SSLSocket) serv.accept();
            this.buffer = new LinkedBlockingQueue<>();
            this.allBytesReceived = allBytesReceived;
            thread1 = new Thread(this::clientWriter, "clientWriter");
            thread2 = new Thread(this::serverLoopback, "serverLoopback");
            thread3 = new Thread(this::clientReader, "clientReader");
            publisher = new SubmissionPublisher<>(exec, Flow.defaultBufferSize(),
                    this::handlePublisherException);
            SSLFlowDelegate.Monitor.add(this::monitor);
        }

        public void start() {
            thread1.start();
            thread2.start();
            thread3.start();
        }

        private void handlePublisherException(Object o, Throwable t) {
            println("Loopback Publisher exception");
            t.printStackTrace(System.out);
        }

        private final AtomicInteger readCount = new AtomicInteger();

        // reads off the SSLSocket the data from the "server"
        private void clientReader() {
            try {
                InputStream is = clientSock.getInputStream();
                final int bufsize = FlowTest.randomRange(512, 16 * 1024);
                println("clientReader: bufsize = " + bufsize);
                while (true) {
                    byte[] buf = new byte[bufsize];
                    int n = is.read(buf);
                    if (n == -1) {
                        println("clientReader close: read "
                                + readCount.get() + " bytes");
                        println("clientReader: got EOF. "
                                + "Waiting signal to close publisher.");
                        allBytesReceived.await();
                        println("clientReader: closing publisher; latch count: "
                                + allBytesReceived.getCount());
                        publisher.close();
                        sleep(2000);
                        Utils.close(is, clientSock, serv);
                        return;
                    }
                    ByteBuffer bb = ByteBuffer.wrap(buf, 0, n);
                    readCount.addAndGet(n);
                    publisher.submit(List.of(bb));
                }
            } catch (Throwable e) {
                println("clientReader failed: " + e);
                e.printStackTrace();
                Utils.close(clientSock);
            }
        }

        // writes the encrypted data from SSLFLowDelegate to the j.n.Socket
        // which is connected to the SSLSocket emulating a server.
        private void clientWriter() {
            long nbytes = 0;
            try {
                OutputStream os =
                        new BufferedOutputStream(clientSock.getOutputStream());

                while (true) {
                    ByteBuffer buf = buffer.take();
                    if (buf == FlowTest.SENTINEL) {
                        // finished
                        //Utils.sleep(2000);
                        println("clientWriter close: " + nbytes + " written");
                        clientSock.shutdownOutput();
                        println("clientWriter close return");
                        return;
                    }
                    int len = buf.remaining();
                    int written = writeToStream(os, buf);
                    assert len == written;
                    nbytes += len;
                    assert !buf.hasRemaining()
                            : "buffer has " + buf.remaining() + " bytes left";
                    clientSubscription.request(1);
                }
            } catch (Throwable e) {
                println("clientWriter failed: " + e);
                e.printStackTrace();
            }
        }

        private int writeToStream(OutputStream os, ByteBuffer buf) throws IOException {
            byte[] b = buf.array();
            int offset = buf.arrayOffset() + buf.position();
            int n = buf.limit() - buf.position();
            os.write(b, offset, n);
            buf.position(buf.limit());
            os.flush();
            return n;
        }

        private final AtomicInteger loopCount = new AtomicInteger();

        public String monitor() {
            return "serverLoopback: loopcount = " + loopCount.toString()
                    + " clientRead: count = " + readCount.toString();
        }

        // thread2
        private void serverLoopback() {
            try {
                InputStream is = serverSock.getInputStream();
                OutputStream os = serverSock.getOutputStream();
                final int bufsize = FlowTest.randomRange(512, 16 * 1024);
                println("serverLoopback: bufsize = " + bufsize);
                byte[] bb = new byte[bufsize];
                while (true) {
                    int n = is.read(bb);
                    if (n == -1) {
                        sleep(2000);
                        println("Received EOF: closing server socket");
                        Utils.close(is, serverSock, serv);
                        println("Server socket closed");
                        return;
                    }
                    os.write(bb, 0, n);
                    os.flush();
                    loopCount.addAndGet(n);
                }
            } catch (Throwable e) {
                println("serverLoopback failed: " + e);
                e.printStackTrace();
            }
        }


        /**
         * This needs to be called before the chain is subscribed. It can't be
         * supplied in the constructor.
         */
        public void setReturnSubscriber(Subscriber<List<ByteBuffer>> returnSubscriber) {
            publisher.subscribe(returnSubscriber);
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            clientSubscription = subscription;
            clientSubscription.request(5);
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            try {
                for (ByteBuffer b : item)
                    buffer.put(b);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Utils.close(clientSock);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            throwable.printStackTrace();
            Utils.close(clientSock);
        }

        @Override
        public void onComplete() {
            try {
                buffer.put(FlowTest.SENTINEL);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Utils.close(clientSock);
            }
        }
    }

    /**
     * The final subscriber which receives the decrypted looped-back data.
     * Just needs to compare the data with what was sent. The given CF is
     * either completed exceptionally with an error or normally on success.
     */
    static class EndSubscriber implements Subscriber<List<ByteBuffer>> {

        private final long nlongs;

        private final AtomicLong counter;
        private volatile Flow.Subscription subscription;
        private final CompletableFuture<Void> completion;
        private final CountDownLatch allBytesReceived;

        EndSubscriber(long nlongs,
                      CompletableFuture<Void> completion,
                      CountDownLatch allBytesReceived) {
            counter = new AtomicLong(0);
            this.nlongs = nlongs;
            this.completion = completion;
            this.allBytesReceived = allBytesReceived;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(5);
        }

        public static String info(List<ByteBuffer> i) {
            StringBuilder sb = new StringBuilder();
            sb.append("size: ").append(Integer.toString(i.size()));
            int x = 0;
            for (ByteBuffer b : i)
                x += b.remaining();
            sb.append(" bytes: " + Integer.toString(x));
            return sb.toString();
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            long currval = counter.get();
            //if (currval % 500 == 0) {
            //System.out.println("End: " + currval);
            //}

            for (ByteBuffer buf : buffers) {
                while (buf.hasRemaining()) {
                    if (buf.remaining() % 8 != 0) {
                        completion.completeExceptionally(
                                new AssertionError("Unaligned buffer: " + buf.remaining()));
                    }
                    long n = buf.getLong();
                    //if (currval > (FlowTest.TOTAL_LONGS - 50)) {
                    //System.out.println("End: " + currval);
                    //}
                    if (n != currval++) {
                        println("ERROR at " + n + " != " + (currval - 1));
                        completion.completeExceptionally(new RuntimeException("ERROR"));
                        subscription.cancel();
                        return;
                    }
                }
            }

            counter.set(currval);
            subscription.request(1);
            if (currval >= TOTAL_LONGS) {
                println("allBytesReceived.countDown(): currval=" +currval);
                allBytesReceived.countDown();
            }
        }

        @Override
        public void onError(Throwable throwable) {
            completion.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            long n = counter.get();
            if (n != nlongs) {
                printf("Error at end: nlongs=%d n=%d", nlongs, n);
                completion.completeExceptionally(new RuntimeException("ERROR AT END"));
            } else {
                println("DONE OK: counter = " + n);
                completion.complete(null);
            }
        }
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static final long START = System.nanoTime();

    static String now() {
        long now = System.nanoTime() - START;
        long min = now / 1000_000_000L / 60L;
        long sec = (now / 1000_000_000L) % 60L;
        long mil = (now / 1000_000L) % 1000L;
        long nan = (now % 1000_000L);
        StringBuilder str = new StringBuilder();
        if (min != 0) {
            str = str.append(min).append("m ");
        }
        if (sec != 0) {
            str = str.append(sec).append("s ");
        }
        if (mil != 0) {
            str.append(mil).append("ms ");
        }
        if (nan != 0) {
            str.append(nan).append("ns ");
        }
        assert now == min * 60L * 1000_000_000L
                + sec * 1000_000_000L
                + mil * 1000_000L + nan;
        return str.toString().trim();
    }

    static void printf(String fmt, Object... args) {
        println(String.format(fmt, args));
    }

    static void println(String msg) {
        System.out.println("[" + Thread.currentThread() + "] [" + now() + "] " + msg);
    }

}
