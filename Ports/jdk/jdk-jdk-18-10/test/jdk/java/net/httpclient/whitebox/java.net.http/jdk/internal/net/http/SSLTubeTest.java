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

import jdk.internal.net.http.common.FlowTube;
import jdk.internal.net.http.common.SSLFlowDelegate;
import jdk.internal.net.http.common.Utils;
import org.testng.annotations.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicInteger;

@Test
public class SSLTubeTest extends AbstractSSLTubeTest {

    @Test
    public void runWithSSLLoopackServer() throws IOException {
        ExecutorService sslExecutor = Executors.newCachedThreadPool();

        /* Start of wiring */
        /* Emulates an echo server */
        SSLLoopbackSubscriber server =
                new SSLLoopbackSubscriber((new SimpleSSLContext()).get(),
                        sslExecutor,
                        allBytesReceived);
        server.start();

        run(server, sslExecutor, allBytesReceived);
    }

    /**
     * This is a copy of the SSLLoopbackSubscriber used in FlowTest
     */
    private static class SSLLoopbackSubscriber implements FlowTube {
        private final BlockingQueue<ByteBuffer> buffer;
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
            SSLServerSocket serv = (SSLServerSocket) fac.createServerSocket();
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
            System.out.println("Loopback Publisher exception");
            t.printStackTrace(System.out);
        }

        private final AtomicInteger readCount = new AtomicInteger();

        // reads off the SSLSocket the data from the "server"
        private void clientReader() {
            try {
                InputStream is = clientSock.getInputStream();
                final int bufsize = randomRange(512, 16 * 1024);
                System.out.println("clientReader: bufsize = " + bufsize);
                while (true) {
                    byte[] buf = new byte[bufsize];
                    int n = is.read(buf);
                    if (n == -1) {
                        System.out.println("clientReader close: read "
                                + readCount.get() + " bytes");
                        System.out.println("clientReader: waiting signal to close publisher");
                        allBytesReceived.await();
                        System.out.println("clientReader: closing publisher");
                        publisher.close();
                        sleep(2000);
                        Utils.close(is, clientSock);
                        return;
                    }
                    ByteBuffer bb = ByteBuffer.wrap(buf, 0, n);
                    readCount.addAndGet(n);
                    publisher.submit(List.of(bb));
                }
            } catch (Throwable e) {
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
                    if (buf == SENTINEL) {
                        // finished
                        //Utils.sleep(2000);
                        System.out.println("clientWriter close: " + nbytes + " written");
                        clientSock.shutdownOutput();
                        System.out.println("clientWriter close return");
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
                final int bufsize = randomRange(512, 16 * 1024);
                System.out.println("serverLoopback: bufsize = " + bufsize);
                byte[] bb = new byte[bufsize];
                while (true) {
                    int n = is.read(bb);
                    if (n == -1) {
                        sleep(2000);
                        is.close();
                        os.close();
                        serverSock.close();
                        return;
                    }
                    os.write(bb, 0, n);
                    os.flush();
                    loopCount.addAndGet(n);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }


        /**
         * This needs to be called before the chain is subscribed. It can't be
         * supplied in the constructor.
         */
        public void setReturnSubscriber(Flow.Subscriber<List<ByteBuffer>> returnSubscriber) {
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
                buffer.put(SENTINEL);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Utils.close(clientSock);
            }
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super List<ByteBuffer>> subscriber) {
            publisher.subscribe(subscriber);
        }
    }

}
