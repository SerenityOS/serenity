/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.net;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark socket read/write.
 *
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class SocketReadWrite {

    static final InetAddress address = InetAddress.getLoopbackAddress();
    public static final int TIMEOUT = 10000;

    static class EchoServer implements Runnable {
        // EchoServer is implemented to execute the same amount echo threads as benchmarking threads are running

        final ServerSocket ss;
        final int port;
        final CountDownLatch startedLatch;
        final int size;
        final boolean timeout;
        List<ServerThread> threads = new ArrayList<>();
        volatile boolean isDone = false;

        public EchoServer(CountDownLatch await, int size, boolean timeout) throws IOException {
            this.size = size;
            this.timeout = timeout;
            ss = new ServerSocket(0);
            port = ss.getLocalPort();
            this.startedLatch = await;
        }

        @Override
        public void run() {
            startedLatch.countDown();
            while (!isDone) {
                try {
                    Socket s = ss.accept();
                    s.setTcpNoDelay(true);
                    if (timeout) {
                        s.setSoTimeout(TIMEOUT);
                    }
                    ServerThread st = new ServerThread(s, size);
                    threads.add(st);
                    new Thread(st).start();
                } catch (IOException e) {
                    if (!isDone) {
                        e.printStackTrace();
                    }
                }
            }
        }

        synchronized void close() throws IOException {
            if (!isDone) {
                isDone = true;
                ss.close();
                for (ServerThread st : threads) {
                    st.close();
                }
            }
        }

        static EchoServer instance = null;

        static synchronized EchoServer startServer(int size, boolean timeout) throws IOException {
            if (instance == null) {
                CountDownLatch started = new CountDownLatch(1);
                EchoServer s = new EchoServer(started, size, timeout);
                new Thread(s).start();
                try {
                    started.await(); // wait until server thread started
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                instance = s;
            }
            return instance;
        }

        static class ServerThread implements Runnable {

            final Socket s;
            final InputStream in;
            final OutputStream out;
            final int size;
            volatile boolean isDone = false;

            ServerThread(Socket s, int size) throws IOException {
                this.s = s;
                this.size = size;
                in = s.getInputStream();
                out = s.getOutputStream();
            }

            @Override
            public void run() {
                byte[] a = new byte[size];
                while (!isDone) {
                    try {
                        readN(a, size, this.in);
                        out.write(a);
                    } catch (IOException e) {
                        if (!isDone) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            public void close() throws IOException {
                isDone = true;
                s.close();
            }

        }
    }

    static void readN(byte[] array, int size, InputStream in) throws IOException {
        int nread = 0;
        while (size > 0) {
            int n = in.read(array, nread, size);
            if (n < 0) throw new RuntimeException();
            nread += n;
            size -= n;
        }
    }

    EchoServer server;

    @Param({"1", "1024", "8192", "64000", "128000"})
    public int size;

    @Param({"false", "true"})
    public boolean timeout;

    Socket s;
    InputStream in;
    OutputStream out;
    byte[] array;

    @Setup
    public void setup() throws IOException {
        server = EchoServer.startServer(size, timeout);
        int port = server.port;
        s = new Socket(address, port);
        s.setTcpNoDelay(true);
        if (timeout) {
            s.setSoTimeout(TIMEOUT);
            // 10 seconds times is quite large and never will happen (for microbenchmarking),
            // but it's required since other paths inside SocketImpl are involved
        }
        in = s.getInputStream();
        out = s.getOutputStream();
        array = new byte[size];
        ThreadLocalRandom.current().nextBytes(array);
    }

    @TearDown
    public void tearDown() throws IOException {
        server.close();
        s.close();
    }

    @Benchmark
    public void echo() throws IOException {
        out.write(array);
        readN(array, size, in);
    }
}
