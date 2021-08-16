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
 * Measures connection setup times
 */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class SocketChannelConnectionSetup {

    private ServerSocketChannel ssc;
    private SocketChannel s1, s2;

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
    }

    @TearDown(Level.Trial)
    public void afterRun() throws IOException, InterruptedException {
        ssc.close();
        if (family.equals("unix")) {
            Files.delete(socket);
            Files.delete(Path.of(tempDir));
        }
    }

    @Benchmark
    @Measurement(iterations = 5, batchSize=200)
    public void test() throws IOException {
        s1 = SocketChannel.open(ssc.getLocalAddress());
        s2 = ssc.accept();
        s1.close();
        s2.close();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(org.openjdk.bench.java.net.SocketChannelConnectionSetup.class.getSimpleName())
                .warmupForks(1)
                .forks(2)
                .build();

        new Runner(opt).run();

        opt = new OptionsBuilder()
                .include(org.openjdk.bench.java.net.SocketChannelConnectionSetup.class.getSimpleName())
                .jvmArgsPrepend("-Djdk.net.useFastTcpLoopback=true")
                .warmupForks(1)
                .forks(2)
                .build();

        new Runner(opt).run();
    }
}
