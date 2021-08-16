/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @run testng/othervm TestSegmentAllocators
 */

import jdk.incubator.foreign.*;

import org.testng.annotations.*;

import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.testng.Assert.*;

public class TestSegmentAllocators {

    final static int ELEMS = 128;
    final static Class<?> ADDRESS_CARRIER = MemoryLayouts.ADDRESS.bitSize() == 64 ? long.class : int.class;

    @Test(dataProvider = "nativeScopes")
    public <Z> void testAllocation(Z value, AllocationFactory allocationFactory, ValueLayout layout, AllocationFunction<Z> allocationFunction, Function<MemoryLayout, VarHandle> handleFactory) {
        ValueLayout[] layouts = {
                layout,
                layout.withBitAlignment(layout.bitAlignment() * 2),
                layout.withBitAlignment(layout.bitAlignment() * 4),
                layout.withBitAlignment(layout.bitAlignment() * 8)
        };
        for (ValueLayout alignedLayout : layouts) {
            List<MemorySegment> addressList = new ArrayList<>();
            int elems = ELEMS / ((int)alignedLayout.byteAlignment() / (int)layout.byteAlignment());
            ResourceScope[] scopes = {
                    ResourceScope.newConfinedScope(),
                    ResourceScope.newSharedScope()
            };
            for (ResourceScope scope : scopes) {
                try (scope) {
                    SegmentAllocator allocator = allocationFactory.allocator(alignedLayout.byteSize() * ELEMS, scope);
                    for (int i = 0; i < elems; i++) {
                        MemorySegment address = allocationFunction.allocate(allocator, alignedLayout, value);
                        assertEquals(address.byteSize(), alignedLayout.byteSize());
                        addressList.add(address);
                        VarHandle handle = handleFactory.apply(alignedLayout);
                        assertEquals(value, handle.get(address));
                    }
                    boolean isBound = allocationFactory.isBound();
                    try {
                        allocationFunction.allocate(allocator, alignedLayout, value); //too much, should fail if bound
                        assertFalse(isBound);
                    } catch (OutOfMemoryError ex) {
                        //failure is expected if bound
                        assertTrue(isBound);
                    }
                }
                // addresses should be invalid now
                for (MemorySegment address : addressList) {
                    assertFalse(address.scope().isAlive());
                }
            }
        }
    }

    static final int SIZE_256M = 1024 * 1024 * 256;

    @Test
    public void testBigAllocationInUnboundedScope() {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            SegmentAllocator allocator = SegmentAllocator.arenaAllocator(scope);
            for (int i = 8 ; i < SIZE_256M ; i *= 8) {
                MemorySegment address = allocator.allocate(i, i);
                //check size
                assertEquals(address.byteSize(), i);
                //check alignment
                assertEquals(address.address().toRawLongValue() % i, 0);
            }
        }
    }

    @Test(expectedExceptions = OutOfMemoryError.class)
    public void testTooBigForBoundedArena() {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            SegmentAllocator allocator = SegmentAllocator.arenaAllocator(10, scope);
            allocator.allocate(12);
        }
    }

    @Test
    public void testBiggerThanBlockForBoundedArena() {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            SegmentAllocator allocator = SegmentAllocator.arenaAllocator(4 * 1024 * 2, scope);
            allocator.allocate(4 * 1024 + 1); // should be ok
        }
    }

    @Test(dataProvider = "arrayScopes")
    public <Z> void testArray(AllocationFactory allocationFactory, ValueLayout layout, AllocationFunction<Object> allocationFunction, ToArrayHelper<Z> arrayHelper) {
        Z arr = arrayHelper.array();
        ResourceScope[] scopes = {
                ResourceScope.newConfinedScope(),
                ResourceScope.newSharedScope()
        };
        for (ResourceScope scope : scopes) {
            try (scope) {
                SegmentAllocator allocator = allocationFactory.allocator(100, scope);
                MemorySegment address = allocationFunction.allocate(allocator, layout, arr);
                Z found = arrayHelper.toArray(address, layout);
                assertEquals(found, arr);
            }
        }
    }

    @DataProvider(name = "nativeScopes")
    static Object[][] nativeScopes() {
        return new Object[][] {
                { (byte)42, AllocationFactory.BOUNDED, MemoryLayouts.BITS_8_BE,
                        (AllocationFunction<Byte>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(byte.class) },
                { (short)42, AllocationFactory.BOUNDED, MemoryLayouts.BITS_16_BE,
                        (AllocationFunction<Short>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(short.class) },
                { (char)42, AllocationFactory.BOUNDED, MemoryLayouts.BITS_16_BE,
                        (AllocationFunction<Character>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(char.class) },
                { 42, AllocationFactory.BOUNDED,
                        MemoryLayouts.BITS_32_BE,
                        (AllocationFunction<Integer>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(int.class) },
                { 42f, AllocationFactory.BOUNDED, MemoryLayouts.BITS_32_BE,
                        (AllocationFunction<Float>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(float.class) },
                { 42L, AllocationFactory.BOUNDED, MemoryLayouts.BITS_64_BE,
                        (AllocationFunction<Long>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(long.class) },
                { 42d, AllocationFactory.BOUNDED, MemoryLayouts.BITS_64_BE,
                        (AllocationFunction<Double>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(double.class) },
                { MemoryAddress.ofLong(42), AllocationFactory.BOUNDED, MemoryLayouts.ADDRESS.withOrder(ByteOrder.BIG_ENDIAN),
                        (AllocationFunction<MemoryAddress>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> MemoryHandles.asAddressVarHandle(l.varHandle(ADDRESS_CARRIER)) },

                { (byte)42, AllocationFactory.BOUNDED, MemoryLayouts.BITS_8_LE,
                        (AllocationFunction<Byte>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(byte.class) },
                { (short)42, AllocationFactory.BOUNDED, MemoryLayouts.BITS_16_LE,
                        (AllocationFunction<Short>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(short.class) },
                { (char)42, AllocationFactory.BOUNDED, MemoryLayouts.BITS_16_LE,
                        (AllocationFunction<Character>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(char.class) },
                { 42, AllocationFactory.BOUNDED,
                        MemoryLayouts.BITS_32_LE,
                        (AllocationFunction<Integer>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(int.class) },
                { 42f, AllocationFactory.BOUNDED, MemoryLayouts.BITS_32_LE,
                        (AllocationFunction<Float>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(float.class) },
                { 42L, AllocationFactory.BOUNDED, MemoryLayouts.BITS_64_LE,
                        (AllocationFunction<Long>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(long.class) },
                { 42d, AllocationFactory.BOUNDED, MemoryLayouts.BITS_64_LE,
                        (AllocationFunction<Double>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(double.class) },
                { MemoryAddress.ofLong(42), AllocationFactory.BOUNDED, MemoryLayouts.ADDRESS.withOrder(ByteOrder.LITTLE_ENDIAN),
                        (AllocationFunction<MemoryAddress>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> MemoryHandles.asAddressVarHandle(l.varHandle(ADDRESS_CARRIER)) },

                { (byte)42, AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_8_BE,
                        (AllocationFunction<Byte>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(byte.class) },
                { (short)42, AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_16_BE,
                        (AllocationFunction<Short>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(short.class) },
                { (char)42, AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_16_BE,
                        (AllocationFunction<Character>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(char.class) },
                { 42, AllocationFactory.UNBOUNDED,
                        MemoryLayouts.BITS_32_BE,
                        (AllocationFunction<Integer>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(int.class) },
                { 42f, AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_32_BE,
                        (AllocationFunction<Float>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(float.class) },
                { 42L, AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_64_BE,
                        (AllocationFunction<Long>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(long.class) },
                { 42d, AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_64_BE,
                        (AllocationFunction<Double>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(double.class) },
                { MemoryAddress.ofLong(42), AllocationFactory.UNBOUNDED, MemoryLayouts.ADDRESS.withOrder(ByteOrder.BIG_ENDIAN),
                        (AllocationFunction<MemoryAddress>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> MemoryHandles.asAddressVarHandle(l.varHandle(ADDRESS_CARRIER)) },

                { (byte)42, AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_8_LE,
                        (AllocationFunction<Byte>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(byte.class) },
                { (short)42, AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_16_LE,
                        (AllocationFunction<Short>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(short.class) },
                { (char)42, AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_16_LE,
                        (AllocationFunction<Character>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(char.class) },
                { 42, AllocationFactory.UNBOUNDED,
                        MemoryLayouts.BITS_32_LE,
                        (AllocationFunction<Integer>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(int.class) },
                { 42f, AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_32_LE,
                        (AllocationFunction<Float>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(float.class) },
                { 42L, AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_64_LE,
                        (AllocationFunction<Long>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(long.class) },
                { 42d, AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_64_LE,
                        (AllocationFunction<Double>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> l.varHandle(double.class) },
                { MemoryAddress.ofLong(42), AllocationFactory.UNBOUNDED, MemoryLayouts.ADDRESS.withOrder(ByteOrder.LITTLE_ENDIAN),
                        (AllocationFunction<MemoryAddress>) SegmentAllocator::allocate,
                        (Function<MemoryLayout, VarHandle>)l -> MemoryHandles.asAddressVarHandle(l.varHandle(ADDRESS_CARRIER)) },
        };
    }

    @DataProvider(name = "arrayScopes")
    static Object[][] arrayScopes() {
        return new Object[][] {
                { AllocationFactory.BOUNDED, MemoryLayouts.BITS_8_LE,
                        (AllocationFunction<byte[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toByteArray },
                { AllocationFactory.BOUNDED, MemoryLayouts.BITS_16_LE,
                        (AllocationFunction<short[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toShortArray },
                { AllocationFactory.BOUNDED,
                        MemoryLayouts.BITS_32_LE,
                        (AllocationFunction<int[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toIntArray },
                { AllocationFactory.BOUNDED, MemoryLayouts.BITS_32_LE,
                        (AllocationFunction<float[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toFloatArray },
                { AllocationFactory.BOUNDED, MemoryLayouts.BITS_64_LE,
                        (AllocationFunction<long[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toLongArray },
                { AllocationFactory.BOUNDED, MemoryLayouts.BITS_64_LE,
                        (AllocationFunction<double[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toDoubleArray },
                { AllocationFactory.BOUNDED, MemoryLayouts.ADDRESS.withOrder(ByteOrder.LITTLE_ENDIAN),
                        (AllocationFunction<MemoryAddress[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toAddressArray },


                { AllocationFactory.BOUNDED, MemoryLayouts.BITS_8_BE,
                        (AllocationFunction<byte[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toByteArray },
                { AllocationFactory.BOUNDED, MemoryLayouts.BITS_16_BE,
                        (AllocationFunction<short[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toShortArray },
                { AllocationFactory.BOUNDED,
                        MemoryLayouts.BITS_32_BE,
                        (AllocationFunction<int[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toIntArray },
                { AllocationFactory.BOUNDED, MemoryLayouts.BITS_32_BE,
                        (AllocationFunction<float[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toFloatArray },
                { AllocationFactory.BOUNDED, MemoryLayouts.BITS_64_BE,
                        (AllocationFunction<long[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toLongArray },
                { AllocationFactory.BOUNDED, MemoryLayouts.BITS_64_BE,
                        (AllocationFunction<double[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toDoubleArray },
                { AllocationFactory.BOUNDED, MemoryLayouts.ADDRESS.withOrder(ByteOrder.BIG_ENDIAN),
                        (AllocationFunction<MemoryAddress[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toAddressArray },

                { AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_8_LE,
                        (AllocationFunction<byte[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toByteArray },
                { AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_16_LE,
                        (AllocationFunction<short[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toShortArray },
                { AllocationFactory.UNBOUNDED,
                        MemoryLayouts.BITS_32_LE,
                        (AllocationFunction<int[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toIntArray },
                { AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_32_LE,
                        (AllocationFunction<float[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toFloatArray },
                { AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_64_LE,
                        (AllocationFunction<long[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toLongArray },
                { AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_64_LE,
                        (AllocationFunction<double[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toDoubleArray },
                { AllocationFactory.UNBOUNDED, MemoryLayouts.ADDRESS.withOrder(ByteOrder.LITTLE_ENDIAN),
                        (AllocationFunction<MemoryAddress[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toAddressArray },


                { AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_8_BE,
                        (AllocationFunction<byte[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toByteArray },
                { AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_16_BE,
                        (AllocationFunction<short[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toShortArray },
                { AllocationFactory.UNBOUNDED,
                        MemoryLayouts.BITS_32_BE,
                        (AllocationFunction<int[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toIntArray },
                { AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_32_BE,
                        (AllocationFunction<float[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toFloatArray },
                { AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_64_BE,
                        (AllocationFunction<long[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toLongArray },
                { AllocationFactory.UNBOUNDED, MemoryLayouts.BITS_64_BE,
                        (AllocationFunction<double[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toDoubleArray },
                { AllocationFactory.UNBOUNDED, MemoryLayouts.ADDRESS.withOrder(ByteOrder.BIG_ENDIAN),
                        (AllocationFunction<MemoryAddress[]>) SegmentAllocator::allocateArray,
                        ToArrayHelper.toAddressArray },
        };
    }

    interface AllocationFunction<X> {
        MemorySegment allocate(SegmentAllocator allocator, ValueLayout layout, X value);
    }

    static class AllocationFactory {
        private final boolean isBound;
        private final BiFunction<Long, ResourceScope, SegmentAllocator> factory;

        private AllocationFactory(boolean isBound, BiFunction<Long, ResourceScope, SegmentAllocator> factory) {
            this.isBound = isBound;
            this.factory = factory;
        }

        SegmentAllocator allocator(long size, ResourceScope scope) {
            return factory.apply(size, scope);
        }

        public boolean isBound() {
            return isBound;
        }

        static AllocationFactory BOUNDED = new AllocationFactory(true, SegmentAllocator::arenaAllocator);
        static AllocationFactory UNBOUNDED = new AllocationFactory(false, (size, scope) -> SegmentAllocator.arenaAllocator(scope));
    }

    interface ToArrayHelper<T> {
        T array();
        T toArray(MemorySegment segment, ValueLayout layout);

        ToArrayHelper<byte[]> toByteArray = new ToArrayHelper<>() {
            @Override
            public byte[] array() {
                return new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
            }

            @Override
            public byte[] toArray(MemorySegment segment, ValueLayout layout) {
                ByteBuffer buffer = segment.asByteBuffer().order(layout.order());
                byte[] found = new byte[buffer.limit()];
                buffer.get(found);
                return found;
            }
        };

        ToArrayHelper<short[]> toShortArray = new ToArrayHelper<>() {
            @Override
            public short[] array() {
                return new short[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
            }

            @Override
            public short[] toArray(MemorySegment segment, ValueLayout layout) {
                ShortBuffer buffer = segment.asByteBuffer().order(layout.order()).asShortBuffer();
                short[] found = new short[buffer.limit()];
                buffer.get(found);
                return found;
            }
        };

        ToArrayHelper<int[]> toIntArray = new ToArrayHelper<>() {
            @Override
            public int[] array() {
                return new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
            }

            @Override
            public int[] toArray(MemorySegment segment, ValueLayout layout) {
                IntBuffer buffer = segment.asByteBuffer().order(layout.order()).asIntBuffer();
                int[] found = new int[buffer.limit()];
                buffer.get(found);
                return found;
            }
        };

        ToArrayHelper<float[]> toFloatArray = new ToArrayHelper<>() {
            @Override
            public float[] array() {
                return new float[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
            }

            @Override
            public float[] toArray(MemorySegment segment, ValueLayout layout) {
                FloatBuffer buffer = segment.asByteBuffer().order(layout.order()).asFloatBuffer();
                float[] found = new float[buffer.limit()];
                buffer.get(found);
                return found;
            }
        };

        ToArrayHelper<long[]> toLongArray = new ToArrayHelper<>() {
            @Override
            public long[] array() {
                return new long[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
            }

            @Override
            public long[] toArray(MemorySegment segment, ValueLayout layout) {
                LongBuffer buffer = segment.asByteBuffer().order(layout.order()).asLongBuffer();
                long[] found = new long[buffer.limit()];
                buffer.get(found);
                return found;
            }
        };

        ToArrayHelper<double[]> toDoubleArray = new ToArrayHelper<>() {
            @Override
            public double[] array() {
                return new double[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
            }

            @Override
            public double[] toArray(MemorySegment segment, ValueLayout layout) {
                DoubleBuffer buffer = segment.asByteBuffer().order(layout.order()).asDoubleBuffer();
                double[] found = new double[buffer.limit()];
                buffer.get(found);
                return found;
            }
        };

        ToArrayHelper<MemoryAddress[]> toAddressArray = new ToArrayHelper<>() {
            @Override
            public MemoryAddress[] array() {
                return switch ((int)MemoryLayouts.ADDRESS.byteSize()) {
                    case 4 -> wrap(toIntArray.array());
                    case 8 -> wrap(toLongArray.array());
                    default -> throw new IllegalStateException("Cannot get here");
                };
            }

            @Override
            public MemoryAddress[] toArray(MemorySegment segment, ValueLayout layout) {
                return switch ((int)layout.byteSize()) {
                    case 4 -> wrap(toIntArray.toArray(segment, layout));
                    case 8 -> wrap(toLongArray.toArray(segment, layout));
                    default -> throw new IllegalStateException("Cannot get here");
                };
            }

            private MemoryAddress[] wrap(int[] ints) {
                return IntStream.of(ints).mapToObj(MemoryAddress::ofLong).toArray(MemoryAddress[]::new);
            }

            private MemoryAddress[] wrap(long[] ints) {
                return LongStream.of(ints).mapToObj(MemoryAddress::ofLong).toArray(MemoryAddress[]::new);
            }
        };
    }
}
