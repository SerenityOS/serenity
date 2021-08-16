/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Tests sending a 128 byte message on a second, to a thread which
 * echo's it back and received by the original thread.
 * Benchmark is performed for "inet" channels over TCP/IP
 * and "unix" domain channels.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class SocketChannelCompare {

    static final int BUFSIZE = 128; // message size sent and received
    private ServerSocketChannel ssc;
    private SocketChannel s1, s2;
    private EchoThread rt;
    private ByteBuffer bb = ByteBuffer.allocate(BUFSIZE);

    private static volatile String tempDir;
    private static final AtomicInteger count = new AtomicInteger(0);
    private volatile Path socket;

    @Param({"inet", "unix"})
    private volatile String family;

    static {
        try {
            Path p = Files.createTempDirectory("readWriteTest");
            tempDir = p.toString();
        } catch (IOException e) {
            tempDir = null;
        }
    }

    private ServerSocketChannel getServerSocketChannel() throws IOException {
        if (family.equals("inet"))
            return getInetServerSocketChannel();
        else if (family.equals("unix"))
            return getUnixServerSocketChannel();
        throw new InternalError();
    }


    private ServerSocketChannel getInetServerSocketChannel() throws IOException {
        InetAddress iaddr = InetAddress.getLoopbackAddress();
        return ServerSocketChannel.open().bind(null);
    }

    private ServerSocketChannel getUnixServerSocketChannel() throws IOException {
        int next = count.incrementAndGet();
        socket = Paths.get(tempDir, Integer.toString(next));
        UnixDomainSocketAddress addr = UnixDomainSocketAddress.of(socket);
        return ServerSocketChannel.open(StandardProtocolFamily.UNIX).bind(addr);
    }

    @Setup(Level.Trial)
    public void beforeRun() throws IOException {
        ssc = getServerSocketChannel();
        s1 = SocketChannel.open(ssc.getLocalAddress());
        s2 = ssc.accept();

        rt = new EchoThread(s2);
        rt.start();
    }

    @TearDown(Level.Trial)
    public void afterRun() throws IOException, InterruptedException {
        s1.close();
        s2.close();
        ssc.close();
        if (family.equals("unix")) {
            Files.delete(socket);
            Files.delete(Path.of(tempDir));
        }
        rt.join();
    }

    @Benchmark
    public void test() throws IOException {
        bb.position(0).limit(BUFSIZE);
        s1.write(bb);
        bb.clear();
        readFully(s1, bb);
    }

    // read until buf is full, or EOF. Always returns number of bytes read

    static int readFully(SocketChannel chan, ByteBuffer buf) throws IOException {
        int n = buf.remaining();
        int count = 0;
        while (n > 0) {
            int c = chan.read(buf);
            if (c == -1)
                return count;
            n -= c;
            count += c;
        }
        return count;
    }

    static class EchoThread extends Thread {
        private SocketChannel sc;

        public EchoThread(SocketChannel s2) {
            this.sc = s2;
        }

        public void run() {
            try {
                ByteBuffer bb = ByteBuffer.allocate(BUFSIZE);
                while (true) {
                    bb.clear();
                    int c = readFully(sc, bb);
                    if (c == 0) {
                        sc.close();
                        return;
                    }
                    bb.flip();
                    sc.write(bb);
                }
            } catch (ClosedChannelException ex) {
                // shutdown time
            } catch (IOException ioex) {
                ioex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(org.openjdk.bench.java.net.SocketChannelCompare.class.getSimpleName())
                .warmupForks(1)
                .warmupIterations(2)
                .measurementIterations(2)
                .forks(2)
                .build();

        new Runner(opt).run();

        opt = new OptionsBuilder()
                .include(org.openjdk.bench.java.net.SocketChannelCompare.class.getSimpleName())
                .warmupForks(1)
                .warmupIterations(2)
                .measurementIterations(2)
                .jvmArgsPrepend("-Djdk.net.useFastTcpLoopback=true")
                .forks(3)
                .build();

        new Runner(opt).run();
    }
}
