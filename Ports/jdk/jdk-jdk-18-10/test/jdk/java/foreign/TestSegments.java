/*
 *  Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @run testng/othervm -Xmx4G -XX:MaxDirectMemorySize=1M TestSegments
 */

import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;
import java.util.function.LongFunction;
import java.util.function.Supplier;

import static org.testng.Assert.*;

public class TestSegments {

    @Test(dataProvider = "badSizeAndAlignments", expectedExceptions = IllegalArgumentException.class)
    public void testBadAllocateAlign(long size, long align) {
        MemorySegment.allocateNative(size, align, ResourceScope.newImplicitScope());
    }

    @Test(dataProvider = "badLayouts", expectedExceptions = UnsupportedOperationException.class)
    public void testBadAllocateLayout(MemoryLayout layout) {
        MemorySegment.allocateNative(layout, ResourceScope.newImplicitScope());
    }

    @Test(expectedExceptions = { OutOfMemoryError.class,
                                 IllegalArgumentException.class })
    public void testAllocateTooBig() {
        MemorySegment.allocateNative(Long.MAX_VALUE, ResourceScope.newImplicitScope());
    }

    @Test(expectedExceptions = OutOfMemoryError.class)
    public void testNativeAllocationTooBig() {
        MemorySegment segment = MemorySegment.allocateNative(1024 * 1024 * 8 * 2, ResourceScope.newImplicitScope()); // 2M
    }

    @Test
    public void testNativeSegmentIsZeroed() {
        VarHandle byteHandle = MemoryLayout.sequenceLayout(MemoryLayouts.JAVA_BYTE)
                .varHandle(byte.class, MemoryLayout.PathElement.sequenceElement());
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemorySegment segment = MemorySegment.allocateNative(1000, 1, scope);
            for (long i = 0 ; i < segment.byteSize() ; i++) {
                assertEquals(0, (byte)byteHandle.get(segment, i));
            }
        }
    }

    @Test
    public void testSlices() {
        VarHandle byteHandle = MemoryLayout.sequenceLayout(MemoryLayouts.JAVA_BYTE)
                .varHandle(byte.class, MemoryLayout.PathElement.sequenceElement());
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemorySegment segment = MemorySegment.allocateNative(10, 1, scope);
            //init
            for (byte i = 0 ; i < segment.byteSize() ; i++) {
                byteHandle.set(segment, (long)i, i);
            }
            for (int offset = 0 ; offset < 10 ; offset++) {
                MemorySegment slice = segment.asSlice(offset);
                for (long i = offset ; i < 10 ; i++) {
                    assertEquals(
                            byteHandle.get(segment, i),
                            byteHandle.get(slice, i - offset)
                    );
                }
            }
        }
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void testSmallSegmentMax() {
        long offset = (long)Integer.MAX_VALUE + (long)Integer.MAX_VALUE + 2L + 6L; // overflows to 6 when casted to int
        MemorySegment memorySegment = MemorySegment.allocateNative(10, ResourceScope.newImplicitScope());
        MemoryAccess.getIntAtOffset(memorySegment, offset);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void testSmallSegmentMin() {
        long offset = ((long)Integer.MIN_VALUE * 2L) + 6L; // underflows to 6 when casted to int
        MemorySegment memorySegment = MemorySegment.allocateNative(10, ResourceScope.newImplicitScope());
        MemoryAccess.getIntAtOffset(memorySegment, offset);
    }

    @Test(dataProvider = "segmentFactories")
    public void testAccessModesOfFactories(Supplier<MemorySegment> memorySegmentSupplier) {
        MemorySegment segment = memorySegmentSupplier.get();
        assertFalse(segment.isReadOnly());
        tryClose(segment);
    }

    static void tryClose(MemorySegment segment) {
        if (!segment.scope().isImplicit()) {
            segment.scope().close();
        }
    }

    @DataProvider(name = "segmentFactories")
    public Object[][] segmentFactories() {
        List<Supplier<MemorySegment>> l = List.of(
                () -> MemorySegment.ofArray(new byte[] { 0x00, 0x01, 0x02, 0x03 }),
                () -> MemorySegment.ofArray(new char[] {'a', 'b', 'c', 'd' }),
                () -> MemorySegment.ofArray(new double[] { 1d, 2d, 3d, 4d} ),
                () -> MemorySegment.ofArray(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }),
                () -> MemorySegment.ofArray(new int[] { 1, 2, 3, 4 }),
                () -> MemorySegment.ofArray(new long[] { 1l, 2l, 3l, 4l } ),
                () -> MemorySegment.ofArray(new short[] { 1, 2, 3, 4 } ),
                () -> MemorySegment.allocateNative(4, ResourceScope.newImplicitScope()),
                () -> MemorySegment.allocateNative(4, 8, ResourceScope.newImplicitScope()),
                () -> MemorySegment.allocateNative(MemoryLayout.valueLayout(32, ByteOrder.nativeOrder()), ResourceScope.newImplicitScope()),
                () -> MemorySegment.allocateNative(4, ResourceScope.newConfinedScope()),
                () -> MemorySegment.allocateNative(4, 8, ResourceScope.newConfinedScope()),
                () -> MemorySegment.allocateNative(MemoryLayout.valueLayout(32, ByteOrder.nativeOrder()), ResourceScope.newConfinedScope())

        );
        return l.stream().map(s -> new Object[] { s }).toArray(Object[][]::new);
    }

    @Test(dataProvider = "segmentFactories")
    public void testFill(Supplier<MemorySegment> memorySegmentSupplier) {
        VarHandle byteHandle = MemoryLayout.sequenceLayout(MemoryLayouts.JAVA_BYTE)
                .varHandle(byte.class, MemoryLayout.PathElement.sequenceElement());

        for (byte value : new byte[] {(byte) 0xFF, (byte) 0x00, (byte) 0x45}) {
            MemorySegment segment = memorySegmentSupplier.get();
            segment.fill(value);
            for (long l = 0; l < segment.byteSize(); l++) {
                assertEquals((byte) byteHandle.get(segment, l), value);
            }

            // fill a slice
            var sliceSegment = segment.asSlice(1, segment.byteSize() - 2).fill((byte) ~value);
            for (long l = 0; l < sliceSegment.byteSize(); l++) {
                assertEquals((byte) byteHandle.get(sliceSegment, l), ~value);
            }
            // assert enclosing slice
            assertEquals((byte) byteHandle.get(segment, 0L), value);
            for (long l = 1; l < segment.byteSize() - 2; l++) {
                assertEquals((byte) byteHandle.get(segment, l), (byte) ~value);
            }
            assertEquals((byte) byteHandle.get(segment, segment.byteSize() - 1L), value);
            tryClose(segment);
        }
    }

    @Test(dataProvider = "segmentFactories")
    public void testFillClosed(Supplier<MemorySegment> memorySegmentSupplier) {
        MemorySegment segment = memorySegmentSupplier.get();
        tryClose(segment);
        if (!segment.scope().isAlive()) {
            try {
                segment.fill((byte) 0xFF);
                fail();
            } catch (IllegalStateException ex) {
                assertTrue(true);
            }
        }
    }

    @Test(dataProvider = "segmentFactories")
    public void testNativeSegments(Supplier<MemorySegment> memorySegmentSupplier) throws Exception {
        MemorySegment segment = memorySegmentSupplier.get();
        try {
            segment.address().toRawLongValue();
            assertTrue(segment.isNative());
            assertTrue(segment.address().isNative());
        } catch (UnsupportedOperationException exception) {
            assertFalse(segment.isNative());
            assertFalse(segment.address().isNative());
        }
        tryClose(segment);
    }

    @Test(dataProvider = "segmentFactories", expectedExceptions = UnsupportedOperationException.class)
    public void testFillIllegalAccessMode(Supplier<MemorySegment> memorySegmentSupplier) {
        MemorySegment segment = memorySegmentSupplier.get();
        segment.asReadOnly().fill((byte) 0xFF);
        tryClose(segment);
    }

    @Test(dataProvider = "segmentFactories")
    public void testFillThread(Supplier<MemorySegment> memorySegmentSupplier) throws Exception {
        MemorySegment segment = memorySegmentSupplier.get();
        AtomicReference<RuntimeException> exception = new AtomicReference<>();
        Runnable action = () -> {
            try {
                segment.fill((byte) 0xBA);
            } catch (RuntimeException e) {
                exception.set(e);
            }
        };
        Thread thread = new Thread(action);
        thread.start();
        thread.join();

        if (segment.scope().ownerThread() != null) {
            RuntimeException e = exception.get();
            if (!(e instanceof IllegalStateException)) {
                throw e;
            }
        } else {
            assertNull(exception.get());
        }
        tryClose(segment);
    }

    @Test
    public void testFillEmpty() {
        MemorySegment.ofArray(new byte[] { }).fill((byte) 0xFF);
        MemorySegment.ofArray(new byte[2]).asSlice(0, 0).fill((byte) 0xFF);
        MemorySegment.ofByteBuffer(ByteBuffer.allocateDirect(0)).fill((byte) 0xFF);
    }

    @Test(dataProvider = "heapFactories")
    public void testBigHeapSegments(IntFunction<MemorySegment> heapSegmentFactory, int factor) {
        int bigSize = (Integer.MAX_VALUE / factor) + 1;
        MemorySegment segment = heapSegmentFactory.apply(bigSize);
        assertTrue(segment.byteSize() > 0);
    }

    @DataProvider(name = "badSizeAndAlignments")
    public Object[][] sizesAndAlignments() {
        return new Object[][] {
                { -1, 8 },
                { 1, 15 },
                { 1, -15 }
        };
    }

    @DataProvider(name = "badLayouts")
    public Object[][] layouts() {
        SizedLayoutFactory[] layoutFactories = SizedLayoutFactory.values();
        Object[][] values = new Object[layoutFactories.length * 2][2];
        for (int i = 0; i < layoutFactories.length ; i++) {
            values[i * 2] = new Object[] { MemoryLayout.structLayout(layoutFactories[i].make(7), MemoryLayout.paddingLayout(9)) }; // good size, bad align
            values[(i * 2) + 1] = new Object[] { layoutFactories[i].make(15).withBitAlignment(16) }; // bad size, good align
        }
        return values;
    }

    enum SizedLayoutFactory {
        VALUE_BE(size -> MemoryLayout.valueLayout(size, ByteOrder.BIG_ENDIAN)),
        VALUE_LE(size -> MemoryLayout.valueLayout(size, ByteOrder.LITTLE_ENDIAN)),
        PADDING(MemoryLayout::paddingLayout);

        private final LongFunction<MemoryLayout> factory;

        SizedLayoutFactory(LongFunction<MemoryLayout> factory) {
            this.factory = factory;
        }

        MemoryLayout make(long size) {
            return factory.apply(size);
        }
    }

    @DataProvider(name = "heapFactories")
    public Object[][] heapFactories() {
        return new Object[][] {
                { (IntFunction<MemorySegment>) size -> MemorySegment.ofArray(new char[size]), 2 },
                { (IntFunction<MemorySegment>) size -> MemorySegment.ofArray(new short[size]), 2 },
                { (IntFunction<MemorySegment>) size -> MemorySegment.ofArray(new int[size]), 4 },
                { (IntFunction<MemorySegment>) size -> MemorySegment.ofArray(new float[size]), 4 },
                { (IntFunction<MemorySegment>) size -> MemorySegment.ofArray(new long[size]), 8 },
                { (IntFunction<MemorySegment>) size -> MemorySegment.ofArray(new double[size]), 8 }
        };
    }
}
