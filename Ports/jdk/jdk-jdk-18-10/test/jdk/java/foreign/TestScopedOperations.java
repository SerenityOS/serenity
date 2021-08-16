/*
 *  Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

/*
 * @test
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestScopedOperations
 */

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class TestScopedOperations {

    static Path tempPath;

    static {
        try {
            File file = File.createTempFile("scopedBuffer", "txt");
            file.deleteOnExit();
            tempPath = file.toPath();
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @Test(dataProvider = "scopedOperations")
    public void testOpAfterClose(String name, ScopedOperation scopedOperation) {
        ResourceScope scope = ResourceScope.newConfinedScope();
        scope.close();
        try {
            scopedOperation.accept(scope);
            fail();
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("closed"));
        }
    }

    @Test(dataProvider = "scopedOperations")
    public void testOpOutsideConfinement(String name, ScopedOperation scopedOperation) {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            AtomicReference<Throwable> failed = new AtomicReference<>();
            Thread t = new Thread(() -> {
                try {
                    scopedOperation.accept(scope);
                } catch (Throwable ex) {
                    failed.set(ex);
                }
            });
            t.start();
            t.join();
            assertNotNull(failed.get());
            assertEquals(failed.get().getClass(), IllegalStateException.class);
            assertTrue(failed.get().getMessage().contains("outside"));
        } catch (InterruptedException ex) {
            throw new AssertionError(ex);
        }
    }

    static List<ScopedOperation> scopedOperations = new ArrayList<>();

    static {
        // scope operations
        ScopedOperation.ofScope(scope -> scope.addCloseAction(() -> {
        }), "ResourceScope::addOnClose");
        ScopedOperation.ofScope(scope -> {
            ResourceScope.Handle handle = scope.acquire();
            scope.release(handle);
        }, "ResourceScope::lock");
        ScopedOperation.ofScope(scope -> MemorySegment.allocateNative(100, scope), "MemorySegment::allocateNative");
        ScopedOperation.ofScope(scope -> {
            try {
                MemorySegment.mapFile(tempPath, 0, 10, FileChannel.MapMode.READ_WRITE, scope);
            } catch (IOException ex) {
                fail();
            }
        }, "MemorySegment::mapFromFile");
        ScopedOperation.ofScope(scope -> CLinker.VaList.make(b -> {}, scope), "VaList::make");
        ScopedOperation.ofScope(scope -> CLinker.VaList.ofAddress(MemoryAddress.ofLong(42), scope), "VaList::make");
        ScopedOperation.ofScope(scope -> CLinker.toCString("Hello", scope), "CLinker::toCString");
        ScopedOperation.ofScope(SegmentAllocator::arenaAllocator, "SegmentAllocator::arenaAllocator");
        // segment operations
        ScopedOperation.ofSegment(MemorySegment::toByteArray, "MemorySegment::toByteArray");
        ScopedOperation.ofSegment(MemorySegment::toCharArray, "MemorySegment::toCharArray");
        ScopedOperation.ofSegment(MemorySegment::toShortArray, "MemorySegment::toShortArray");
        ScopedOperation.ofSegment(MemorySegment::toIntArray, "MemorySegment::toIntArray");
        ScopedOperation.ofSegment(MemorySegment::toFloatArray, "MemorySegment::toFloatArray");
        ScopedOperation.ofSegment(MemorySegment::toLongArray, "MemorySegment::toLongArray");
        ScopedOperation.ofSegment(MemorySegment::toDoubleArray, "MemorySegment::toDoubleArray");
        ScopedOperation.ofSegment(MemorySegment::address, "MemorySegment::address");
        ScopedOperation.ofSegment(s -> MemoryLayout.sequenceLayout(s.byteSize(), MemoryLayouts.JAVA_BYTE), "MemorySegment::spliterator");
        ScopedOperation.ofSegment(s -> s.copyFrom(s), "MemorySegment::copyFrom");
        ScopedOperation.ofSegment(s -> s.mismatch(s), "MemorySegment::mismatch");
        ScopedOperation.ofSegment(s -> s.fill((byte) 0), "MemorySegment::fill");
        // address operations
        ScopedOperation.ofAddress(a -> a.toRawLongValue(), "MemoryAddress::toRawLongValue");
        ScopedOperation.ofAddress(a -> a.asSegment(100, ResourceScope.globalScope()), "MemoryAddress::asSegment");
        // valist operations
        ScopedOperation.ofVaList(CLinker.VaList::address, "VaList::address");
        ScopedOperation.ofVaList(CLinker.VaList::copy, "VaList::copy");
        ScopedOperation.ofVaList(list -> list.vargAsAddress(MemoryLayouts.ADDRESS), "VaList::vargAsAddress");
        ScopedOperation.ofVaList(list -> list.vargAsInt(MemoryLayouts.JAVA_INT), "VaList::vargAsInt");
        ScopedOperation.ofVaList(list -> list.vargAsLong(MemoryLayouts.JAVA_LONG), "VaList::vargAsLong");
        ScopedOperation.ofVaList(list -> list.vargAsDouble(MemoryLayouts.JAVA_DOUBLE), "VaList::vargAsDouble");
        ScopedOperation.ofVaList(CLinker.VaList::skip, "VaList::skip");
        ScopedOperation.ofVaList(list -> list.vargAsSegment(MemoryLayout.structLayout(MemoryLayouts.JAVA_INT), ResourceScope.newImplicitScope()), "VaList::vargAsSegment/1");
        // allocator operations
        ScopedOperation.ofAllocator(a -> a.allocate(1), "NativeAllocator::allocate/size");
        ScopedOperation.ofAllocator(a -> a.allocate(1, 1), "NativeAllocator::allocate/size/align");
        ScopedOperation.ofAllocator(a -> a.allocate(MemoryLayouts.JAVA_BYTE), "NativeAllocator::allocate/layout");
        ScopedOperation.ofAllocator(a -> a.allocate(MemoryLayouts.JAVA_BYTE, (byte) 0), "NativeAllocator::allocate/byte");
        ScopedOperation.ofAllocator(a -> a.allocate(MemoryLayouts.JAVA_CHAR, (char) 0), "NativeAllocator::allocate/char");
        ScopedOperation.ofAllocator(a -> a.allocate(MemoryLayouts.JAVA_SHORT, (short) 0), "NativeAllocator::allocate/short");
        ScopedOperation.ofAllocator(a -> a.allocate(MemoryLayouts.JAVA_INT, 0), "NativeAllocator::allocate/int");
        ScopedOperation.ofAllocator(a -> a.allocate(MemoryLayouts.JAVA_FLOAT, 0f), "NativeAllocator::allocate/float");
        ScopedOperation.ofAllocator(a -> a.allocate(MemoryLayouts.JAVA_LONG, 0L), "NativeAllocator::allocate/long");
        ScopedOperation.ofAllocator(a -> a.allocate(MemoryLayouts.JAVA_DOUBLE, 0d), "NativeAllocator::allocate/double");
        ScopedOperation.ofAllocator(a -> a.allocateArray(MemoryLayouts.JAVA_BYTE, 1L), "NativeAllocator::allocateArray/size");
        ScopedOperation.ofAllocator(a -> a.allocateArray(MemoryLayouts.JAVA_BYTE, new byte[]{0}), "NativeAllocator::allocateArray/byte");
        ScopedOperation.ofAllocator(a -> a.allocateArray(MemoryLayouts.JAVA_CHAR, new char[]{0}), "NativeAllocator::allocateArray/char");
        ScopedOperation.ofAllocator(a -> a.allocateArray(MemoryLayouts.JAVA_SHORT, new short[]{0}), "NativeAllocator::allocateArray/short");
        ScopedOperation.ofAllocator(a -> a.allocateArray(MemoryLayouts.JAVA_INT, new int[]{0}), "NativeAllocator::allocateArray/int");
        ScopedOperation.ofAllocator(a -> a.allocateArray(MemoryLayouts.JAVA_FLOAT, new float[]{0}), "NativeAllocator::allocateArray/float");
        ScopedOperation.ofAllocator(a -> a.allocateArray(MemoryLayouts.JAVA_LONG, new long[]{0}), "NativeAllocator::allocateArray/long");
        ScopedOperation.ofAllocator(a -> a.allocateArray(MemoryLayouts.JAVA_DOUBLE, new double[]{0}), "NativeAllocator::allocateArray/double");
    };

    @DataProvider(name = "scopedOperations")
    static Object[][] scopedOperations() {
        return scopedOperations.stream().map(op -> new Object[] { op.name, op }).toArray(Object[][]::new);
    }

    static class ScopedOperation implements Consumer<ResourceScope> {

        final Consumer<ResourceScope> scopeConsumer;
        final String name;

        private ScopedOperation(Consumer<ResourceScope> scopeConsumer, String name) {
            this.scopeConsumer = scopeConsumer;
            this.name = name;
        }

        @Override
        public void accept(ResourceScope scope) {
            scopeConsumer.accept(scope);
        }

        static void ofScope(Consumer<ResourceScope> scopeConsumer, String name) {
            scopedOperations.add(new ScopedOperation(scopeConsumer::accept, name));
        }

        static void ofVaList(Consumer<CLinker.VaList> vaListConsumer, String name) {
            scopedOperations.add(new ScopedOperation(scope -> {
                CLinker.VaList vaList = CLinker.VaList.make((builder) -> {}, scope);
                vaListConsumer.accept(vaList);
            }, name));
        }

        static void ofSegment(Consumer<MemorySegment> segmentConsumer, String name) {
            for (SegmentFactory segmentFactory : SegmentFactory.values()) {
                scopedOperations.add(new ScopedOperation(scope -> {
                    MemorySegment segment = segmentFactory.segmentFactory.apply(scope);
                    segmentConsumer.accept(segment);
                }, segmentFactory.name() + "/" + name));
            }
        }

        static void ofAddress(Consumer<MemoryAddress> addressConsumer, String name) {
            for (SegmentFactory segmentFactory : SegmentFactory.values()) {
                scopedOperations.add(new ScopedOperation(scope -> {
                    MemoryAddress segment = segmentFactory.segmentFactory.apply(scope).address();
                    addressConsumer.accept(segment);
                }, segmentFactory.name() + "/" + name));
            }
        }

        static void ofAllocator(Consumer<SegmentAllocator> allocatorConsumer, String name) {
            for (AllocatorFactory allocatorFactory : AllocatorFactory.values()) {
                scopedOperations.add(new ScopedOperation(scope -> {
                    SegmentAllocator allocator = allocatorFactory.allocatorFactory.apply(scope);
                    allocatorConsumer.accept(allocator);
                }, allocatorFactory.name() + "/" + name));
            }
        }

        enum SegmentFactory {

            NATIVE(scope -> MemorySegment.allocateNative(10, scope)),
            MAPPED(scope -> {
                try {
                    return MemorySegment.mapFile(Path.of("foo.txt"), 0, 10, FileChannel.MapMode.READ_WRITE, scope);
                } catch (IOException ex) {
                    throw new AssertionError(ex);
                }
            }),
            UNSAFE(scope -> MemoryAddress.NULL.asSegment(10, scope));

            static {
                try {
                    File f = new File("foo.txt");
                    f.createNewFile();
                    f.deleteOnExit();
                } catch (IOException ex) {
                    throw new ExceptionInInitializerError(ex);
                }
            }

            final Function<ResourceScope, MemorySegment> segmentFactory;

            SegmentFactory(Function<ResourceScope, MemorySegment> segmentFactory) {
                this.segmentFactory = segmentFactory;
            }
        }

        enum AllocatorFactory {
            ARENA_BOUNDED(scope -> SegmentAllocator.arenaAllocator(1000, scope)),
            ARENA_UNBOUNDED(SegmentAllocator::arenaAllocator),
            FROM_SEGMENT(scope -> {
                MemorySegment segment = MemorySegment.allocateNative(10, scope);
                return SegmentAllocator.ofSegment(segment);
            }),
            FROM_SCOPE(SegmentAllocator::ofScope);

            final Function<ResourceScope, SegmentAllocator> allocatorFactory;

            AllocatorFactory(Function<ResourceScope, SegmentAllocator> allocatorFactory) {
                this.allocatorFactory = allocatorFactory;
            }
        }
    }
}
