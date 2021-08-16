/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

/*
 * @test
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestSharedAccess
 */

import jdk.incubator.foreign.*;
import org.testng.annotations.*;

import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.*;

public class TestSharedAccess {

    static final VarHandle intHandle = MemoryLayouts.JAVA_INT.varHandle(int.class);

    @Test
    public void testShared() throws Throwable {
        SequenceLayout layout = MemoryLayout.sequenceLayout(1024, MemoryLayouts.JAVA_INT);
        try (ResourceScope scope = ResourceScope.newSharedScope()) {
            MemorySegment s = MemorySegment.allocateNative(layout, scope);
            for (int i = 0 ; i < layout.elementCount().getAsLong() ; i++) {
                setInt(s.asSlice(i * 4), 42);
            }
            List<Thread> threads = new ArrayList<>();
            List<Spliterator<MemorySegment>> spliterators = new ArrayList<>();
            spliterators.add(s.spliterator(layout.elementLayout()));
            while (true) {
                boolean progress = false;
                List<Spliterator<MemorySegment>> newSpliterators = new ArrayList<>();
                for (Spliterator<MemorySegment> spliterator : spliterators) {
                    Spliterator<MemorySegment> sub = spliterator.trySplit();
                    if (sub != null) {
                        progress = true;
                        newSpliterators.add(sub);
                    }
                }
                spliterators.addAll(newSpliterators);
                if (!progress) break;
            }

            AtomicInteger accessCount = new AtomicInteger();
            for (Spliterator<MemorySegment> spliterator : spliterators) {
                threads.add(new Thread(() -> {
                    spliterator.tryAdvance(local -> {
                        assertEquals(getInt(local), 42);
                        accessCount.incrementAndGet();
                    });
                }));
            }
            threads.forEach(Thread::start);
            threads.forEach(t -> {
                try {
                    t.join();
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
            });
            assertEquals(accessCount.get(), 1024);
        }
    }

    @Test
    public void testSharedUnsafe() throws Throwable {
        try (ResourceScope scope = ResourceScope.newSharedScope()) {
            MemorySegment s = MemorySegment.allocateNative(4, 1, scope);
            setInt(s, 42);
            assertEquals(getInt(s), 42);
            List<Thread> threads = new ArrayList<>();
            MemorySegment sharedSegment = s.address().asSegment(s.byteSize(), scope);
            for (int i = 0 ; i < 1000 ; i++) {
                threads.add(new Thread(() -> {
                    assertEquals(getInt(sharedSegment), 42);
                }));
            }
            threads.forEach(Thread::start);
            threads.forEach(t -> {
                try {
                    t.join();
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }

    @Test
    public void testOutsideConfinementThread() throws Throwable {
        CountDownLatch a = new CountDownLatch(1);
        CountDownLatch b = new CountDownLatch(1);
        CompletableFuture<?> r;
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemorySegment s1 = MemorySegment.allocateNative(MemoryLayout.sequenceLayout(2, MemoryLayouts.JAVA_INT), scope);
            r = CompletableFuture.runAsync(() -> {
                try {
                    ByteBuffer bb = s1.asByteBuffer();

                    MemorySegment s2 = MemorySegment.ofByteBuffer(bb);
                    a.countDown();

                    try {
                        b.await();
                    } catch (InterruptedException e) {
                    }

                    setInt(s2.asSlice(4), -42);
                    fail();
                } catch (IllegalStateException ex) {
                    assertTrue(ex.getMessage().contains("owning thread"));
                }
            });

            a.await();
            setInt(s1.asSlice(4), 42);
        }

        b.countDown();
        r.get();
    }

    static int getInt(MemorySegment base) {
        return (int)intHandle.getVolatile(base);
    }

    static void setInt(MemorySegment base, int value) {
        intHandle.setVolatile(base, value);
    }
}
