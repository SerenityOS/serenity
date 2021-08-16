/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @library /test/lib
 * @modules java.base/sun.nio.ch
 *          jdk.incubator.foreign/jdk.internal.foreign
 * @key randomness
 * @run testng/othervm TestSocketChannels
 */

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;
import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.testng.annotations.*;
import static org.testng.Assert.*;

/**
 * Tests consisting of buffer views with synchronous NIO network channels.
 */
public class TestSocketChannels extends AbstractChannelsTest {

    static final Class<IllegalStateException> ISE = IllegalStateException.class;

    @Test(dataProvider = "closeableScopes")
    public void testBasicIOWithClosedSegment(Supplier<ResourceScope> scopeSupplier)
        throws Exception
    {
        try (var channel = SocketChannel.open();
             var server = ServerSocketChannel.open();
             var connectedChannel = connectChannels(server, channel)) {
            ResourceScope scope = scopeSupplier.get();
            ByteBuffer bb = segmentBufferOfSize(scope, 16);
            scope.close();
            assertMessage(expectThrows(ISE, () -> channel.read(bb)),                           "Already closed");
            assertMessage(expectThrows(ISE, () -> channel.read(new ByteBuffer[] {bb})),        "Already closed");
            assertMessage(expectThrows(ISE, () -> channel.read(new ByteBuffer[] {bb}, 0, 1)),  "Already closed");
            assertMessage(expectThrows(ISE, () -> channel.write(bb)),                          "Already closed");
            assertMessage(expectThrows(ISE, () -> channel.write(new ByteBuffer[] {bb})),       "Already closed");
            assertMessage(expectThrows(ISE, () -> channel.write(new ByteBuffer[] {bb}, 0 ,1)), "Already closed");
        }
    }

    @Test(dataProvider = "closeableScopes")
    public void testScatterGatherWithClosedSegment(Supplier<ResourceScope> scopeSupplier)
        throws Exception
    {
        try (var channel = SocketChannel.open();
             var server = ServerSocketChannel.open();
             var connectedChannel = connectChannels(server, channel)) {
            ResourceScope scope = scopeSupplier.get();
            ByteBuffer[] buffers = segmentBuffersOfSize(8, scope, 16);
            scope.close();
            assertMessage(expectThrows(ISE, () -> channel.write(buffers)),       "Already closed");
            assertMessage(expectThrows(ISE, () -> channel.read(buffers)),        "Already closed");
            assertMessage(expectThrows(ISE, () -> channel.write(buffers, 0 ,8)), "Already closed");
            assertMessage(expectThrows(ISE, () -> channel.read(buffers, 0, 8)),  "Already closed");
        }
    }

    @Test(dataProvider = "allScopes")
    public void testBasicIO(Supplier<ResourceScope> scopeSupplier)
        throws Exception
    {
        ResourceScope scope;
        try (var sc1 = SocketChannel.open();
             var ssc = ServerSocketChannel.open();
             var sc2 = connectChannels(ssc, sc1);
             var scp = closeableScopeOrNull(scope = scopeSupplier.get())) {
            MemorySegment segment1 = MemorySegment.allocateNative(10, 1, scope);
            MemorySegment segment2 = MemorySegment.allocateNative(10, 1, scope);
            for (int i = 0; i < 10; i++) {
                MemoryAccess.setByteAtOffset(segment1, i, (byte) i);
            }
            ByteBuffer bb1 = segment1.asByteBuffer();
            ByteBuffer bb2 = segment2.asByteBuffer();
            assertEquals(sc1.write(bb1), 10);
            assertEquals(sc2.read(bb2), 10);
            assertEquals(bb2.flip(), ByteBuffer.wrap(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}));
        }
    }

    @Test
    public void testBasicHeapIOWithGlobalScope() throws Exception {
        try (var sc1 = SocketChannel.open();
             var ssc = ServerSocketChannel.open();
             var sc2 = connectChannels(ssc, sc1)) {
            var segment1 = MemorySegment.ofArray(new byte[10]);
            var segment2 = MemorySegment.ofArray(new byte[10]);
            for (int i = 0; i < 10; i++) {
                MemoryAccess.setByteAtOffset(segment1, i, (byte) i);
            }
            ByteBuffer bb1 = segment1.asByteBuffer();
            ByteBuffer bb2 = segment2.asByteBuffer();
            assertEquals(sc1.write(bb1), 10);
            assertEquals(sc2.read(bb2), 10);
            assertEquals(bb2.flip(), ByteBuffer.wrap(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}));
        }
    }

    @Test(dataProvider = "confinedScopes")
    public void testIOOnConfinedFromAnotherThread(Supplier<ResourceScope> scopeSupplier)
        throws Exception
    {
        try (var channel = SocketChannel.open();
             var server = ServerSocketChannel.open();
             var connected = connectChannels(server, channel);
             var scope = scopeSupplier.get()) {
            var segment = MemorySegment.allocateNative(10, 1, scope);
            ByteBuffer bb = segment.asByteBuffer();
            List<ThrowingRunnable> ioOps = List.of(
                    () -> channel.write(bb),
                    () -> channel.read(bb),
                    () -> channel.write(new ByteBuffer[] {bb}),
                    () -> channel.read(new ByteBuffer[] {bb}),
                    () -> channel.write(new ByteBuffer[] {bb}, 0, 1),
                    () -> channel.read(new ByteBuffer[] {bb}, 0, 1)
            );
            for (var ioOp : ioOps) {
                AtomicReference<Exception> exception = new AtomicReference<>();
                Runnable task = () -> exception.set(expectThrows(ISE, ioOp));
                var t = new Thread(task);
                t.start();
                t.join();
                assertMessage(exception.get(), "Attempted access outside owning thread");
            }
        }
    }

    @Test(dataProvider = "allScopes")
    public void testScatterGatherIO(Supplier<ResourceScope> scopeSupplier)
        throws Exception
    {
        ResourceScope scope;
        try (var sc1 = SocketChannel.open();
             var ssc = ServerSocketChannel.open();
             var sc2 = connectChannels(ssc, sc1);
             var scp = closeableScopeOrNull(scope = scopeSupplier.get())) {
            var writeBuffers = mixedBuffersOfSize(32, scope, 64);
            var readBuffers = mixedBuffersOfSize(32, scope, 64);
            long expectedCount = remaining(writeBuffers);
            assertEquals(writeNBytes(sc1, writeBuffers, 0, 32, expectedCount), expectedCount);
            assertEquals(readNBytes(sc2, readBuffers, 0, 32, expectedCount), expectedCount);
            assertEquals(flip(readBuffers), clear(writeBuffers));
        }
    }

    @Test(dataProvider = "closeableScopes")
    public void testBasicIOWithDifferentScopes(Supplier<ResourceScope> scopeSupplier)
         throws Exception
    {
        try (var sc1 = SocketChannel.open();
             var ssc = ServerSocketChannel.open();
             var sc2 = connectChannels(ssc, sc1);
             var scope1 = scopeSupplier.get();
             var scope2 = scopeSupplier.get()) {
            var writeBuffers = Stream.of(mixedBuffersOfSize(16, scope1, 64), mixedBuffersOfSize(16, scope2, 64))
                                     .flatMap(Arrays::stream)
                                     .toArray(ByteBuffer[]::new);
            var readBuffers = Stream.of(mixedBuffersOfSize(16, scope1, 64), mixedBuffersOfSize(16, scope2, 64))
                                    .flatMap(Arrays::stream)
                                    .toArray(ByteBuffer[]::new);

            long expectedCount = remaining(writeBuffers);
            assertEquals(writeNBytes(sc1, writeBuffers, 0, 32, expectedCount), expectedCount);
            assertEquals(readNBytes(sc2, readBuffers, 0, 32, expectedCount), expectedCount);
            assertEquals(flip(readBuffers), clear(writeBuffers));
        }
    }

    static SocketChannel connectChannels(ServerSocketChannel ssc, SocketChannel sc)
        throws Exception
    {
        ssc.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
        sc.connect(ssc.getLocalAddress());
        return ssc.accept();
    }

    static long writeNBytes(SocketChannel channel,
                            ByteBuffer[] buffers, int offset, int len,
                            long bytes)
        throws Exception
    {
        long total = 0L;
        do {
            long n = channel.write(buffers, offset, len);
            assertTrue(n > 0, "got:" + n);
            total += n;
        } while (total < bytes);
        return total;
    }

    static long readNBytes(SocketChannel channel,
                           ByteBuffer[] buffers, int offset, int len,
                           long bytes)
        throws Exception
    {
        long total = 0L;
        do {
            long n = channel.read(buffers, offset, len);
            assertTrue(n > 0, "got:" + n);
            total += n;
        } while (total < bytes);
        return total;
    }
}

