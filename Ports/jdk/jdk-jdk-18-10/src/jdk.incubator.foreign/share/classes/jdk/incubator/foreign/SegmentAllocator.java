/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.incubator.foreign;

import jdk.internal.foreign.ArenaAllocator;
import jdk.internal.foreign.AbstractMemorySegmentImpl;
import jdk.internal.foreign.ResourceScopeImpl;
import jdk.internal.foreign.Utils;

import java.lang.invoke.VarHandle;
import java.lang.reflect.Array;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * This interface models a memory allocator. Clients implementing this interface
 * must implement the {@link #allocate(long, long)} method. This interface defines several default methods
 * which can be useful to create segments from several kinds of Java values such as primitives and arrays.
 * This interface can be seen as a thin wrapper around the basic capabilities for creating native segments
 * (e.g. {@link MemorySegment#allocateNative(long, long, ResourceScope)}); since {@link SegmentAllocator} is a <em>functional interface</em>,
 * clients can easily obtain a native allocator by using either a lambda expression or a method reference.
 * <p>
 * This interface provides a factory, namely {@link SegmentAllocator#ofScope(ResourceScope)} which can be used to obtain
 * a <em>scoped</em> allocator, that is, an allocator which creates segment bound by a given scope. This can be useful
 * when working inside a <em>try-with-resources</em> construct:
 *
 * <blockquote><pre>{@code
try (ResourceScope scope = ResourceScope.newConfinedScope()) {
   SegmentAllocator allocator = SegmentAllocator.ofScope(scope);
   ...
}
 * }</pre></blockquote>
 *
 * In addition, this interface also defines factories for commonly used allocators; for instance {@link #arenaAllocator(ResourceScope)}
 * and {@link #arenaAllocator(long, ResourceScope)} are arena-style native allocators. Finally {@link #ofSegment(MemorySegment)}
 * returns an allocator which wraps a segment (either on-heap or off-heap) and recycles its content upon each new allocation request.
 */
@FunctionalInterface
public interface SegmentAllocator {

    /**
     * Allocate a block of memory with given layout and initialize it with given byte value.
     * @implSpec the default implementation for this method calls {@code this.allocate(layout)}.
     * @param layout the layout of the block of memory to be allocated.
     * @param value the value to be set on the newly allocated memory block.
     * @return a segment for the newly allocated memory block.
     * @throws IllegalArgumentException if {@code layout.byteSize()} does not conform to the size of a byte value.
     */
    default MemorySegment allocate(ValueLayout layout, byte value) {
        Objects.requireNonNull(layout);
        VarHandle handle = layout.varHandle(byte.class);
        MemorySegment addr = allocate(layout);
        handle.set(addr, value);
        return addr;
    }

    /**
     * Allocate a block of memory with given layout and initialize it with given char value.
     * @implSpec the default implementation for this method calls {@code this.allocate(layout)}.
     * @param layout the layout of the block of memory to be allocated.
     * @param value the value to be set on the newly allocated memory block.
     * @return a segment for the newly allocated memory block.
     * @throws IllegalArgumentException if {@code layout.byteSize()} does not conform to the size of a char value.
     */
    default MemorySegment allocate(ValueLayout layout, char value) {
        Objects.requireNonNull(layout);
        VarHandle handle = layout.varHandle(char.class);
        MemorySegment addr = allocate(layout);
        handle.set(addr, value);
        return addr;
    }

    /**
     * Allocate a block of memory with given layout and initialize it with given short value.
     * @implSpec the default implementation for this method calls {@code this.allocate(layout)}.
     * @param layout the layout of the block of memory to be allocated.
     * @param value the value to be set on the newly allocated memory block.
     * @return a segment for the newly allocated memory block.
     * @throws IllegalArgumentException if {@code layout.byteSize()} does not conform to the size of a short value.
     */
    default MemorySegment allocate(ValueLayout layout, short value) {
        Objects.requireNonNull(layout);
        VarHandle handle = layout.varHandle(short.class);
        MemorySegment addr = allocate(layout);
        handle.set(addr, value);
        return addr;
    }

    /**
     * Allocate a block of memory with given layout and initialize it with given int value.
     * @implSpec the default implementation for this method calls {@code this.allocate(layout)}.
     * @param layout the layout of the block of memory to be allocated.
     * @param value the value to be set on the newly allocated memory block.
     * @return a segment for the newly allocated memory block.
     * @throws IllegalArgumentException if {@code layout.byteSize()} does not conform to the size of a int value.
     */
    default MemorySegment allocate(ValueLayout layout, int value) {
        Objects.requireNonNull(layout);
        VarHandle handle = layout.varHandle(int.class);
        MemorySegment addr = allocate(layout);
        handle.set(addr, value);
        return addr;
    }

    /**
     * Allocate a block of memory with given layout and initialize it with given float value.
     * @implSpec the default implementation for this method calls {@code this.allocate(layout)}.
     * @param layout the layout of the block of memory to be allocated.
     * @param value the value to be set on the newly allocated memory block.
     * @return a segment for the newly allocated memory block.
     * @throws IllegalArgumentException if {@code layout.byteSize()} does not conform to the size of a float value.
     */
    default MemorySegment allocate(ValueLayout layout, float value) {
        Objects.requireNonNull(layout);
        VarHandle handle = layout.varHandle(float.class);
        MemorySegment addr = allocate(layout);
        handle.set(addr, value);
        return addr;
    }

    /**
     * Allocate a block of memory with given layout and initialize it with given long value.
     * @implSpec the default implementation for this method calls {@code this.allocate(layout)}.
     * @param layout the layout of the block of memory to be allocated.
     * @param value the value to be set on the newly allocated memory block.
     * @return a segment for the newly allocated memory block.
     * @throws IllegalArgumentException if {@code layout.byteSize()} does not conform to the size of a long value.
     */
    default MemorySegment allocate(ValueLayout layout, long value) {
        Objects.requireNonNull(layout);
        VarHandle handle = layout.varHandle(long.class);
        MemorySegment addr = allocate(layout);
        handle.set(addr, value);
        return addr;
    }

    /**
     * Allocate a block of memory with given layout and initialize it with given double value.
     * @implSpec the default implementation for this method calls {@code this.allocate(layout)}.
     * @param layout the layout of the block of memory to be allocated.
     * @param value the value to be set on the newly allocated memory block.
     * @return a segment for the newly allocated memory block.
     * @throws IllegalArgumentException if {@code layout.byteSize()} does not conform to the size of a double value.
     */
    default MemorySegment allocate(ValueLayout layout, double value) {
        Objects.requireNonNull(layout);
        VarHandle handle = layout.varHandle(double.class);
        MemorySegment addr = allocate(layout);
        handle.set(addr, value);
        return addr;
    }

    /**
     * Allocate a block of memory with given layout and initialize it with given address value
     * (expressed as an {@link Addressable} instance).
     * The address value might be narrowed according to the platform address size (see {@link MemoryLayouts#ADDRESS}).
     * @implSpec the default implementation for this method calls {@code this.allocate(layout)}.
     * @param layout the layout of the block of memory to be allocated.
     * @param value the value to be set on the newly allocated memory block.
     * @return a segment for the newly allocated memory block.
     * @throws IllegalArgumentException if {@code layout.byteSize() != MemoryLayouts.ADDRESS.byteSize()}.
     */
    default MemorySegment allocate(ValueLayout layout, Addressable value) {
        Objects.requireNonNull(value);
        Objects.requireNonNull(layout);
        if (MemoryLayouts.ADDRESS.byteSize() != layout.byteSize()) {
            throw new IllegalArgumentException("Layout size mismatch - " + layout.byteSize() + " != " + MemoryLayouts.ADDRESS.byteSize());
        }
        return switch ((int)layout.byteSize()) {
            case 4 -> allocate(layout, (int)value.address().toRawLongValue());
            case 8 -> allocate(layout, value.address().toRawLongValue());
            default -> throw new UnsupportedOperationException("Unsupported pointer size"); // should not get here
        };
    }

    /**
     * Allocate a block of memory with given layout and initialize it with given byte array.
     * @implSpec the default implementation for this method calls {@code this.allocateArray(layout, array.length)}.
     * @param elementLayout the element layout of the array to be allocated.
     * @param array the array to be copied on the newly allocated memory block.
     * @return a segment for the newly allocated memory block.
     * @throws IllegalArgumentException if {@code elementLayout.byteSize()} does not conform to the size of a byte value.
     */
    default MemorySegment allocateArray(ValueLayout elementLayout, byte[] array) {
        return copyArrayWithSwapIfNeeded(array, elementLayout, MemorySegment::ofArray);
    }

    /**
     * Allocate a block of memory with given layout and initialize it with given short array.
     * @implSpec the default implementation for this method calls {@code this.allocateArray(layout, array.length)}.
     * @param elementLayout the element layout of the array to be allocated.
     * @param array the array to be copied on the newly allocated memory block.
     * @return a segment for the newly allocated memory block.
     * @throws IllegalArgumentException if {@code elementLayout.byteSize()} does not conform to the size of a short value.
     */
    default MemorySegment allocateArray(ValueLayout elementLayout, short[] array) {
        return copyArrayWithSwapIfNeeded(array, elementLayout, MemorySegment::ofArray);
    }

    /**
     * Allocate a block of memory with given layout and initialize it with given char array.
     * @implSpec the default implementation for this method calls {@code this.allocateArray(layout, array.length)}.
     * @param elementLayout the element layout of the array to be allocated.
     * @param array the array to be copied on the newly allocated memory block.
     * @return a segment for the newly allocated memory block.
     * @throws IllegalArgumentException if {@code elementLayout.byteSize()} does not conform to the size of a char value.
     */
    default MemorySegment allocateArray(ValueLayout elementLayout, char[] array) {
        return copyArrayWithSwapIfNeeded(array, elementLayout, MemorySegment::ofArray);
    }

    /**
     * Allocate a block of memory with given layout and initialize it with given int array.
     * @implSpec the default implementation for this method calls {@code this.allocateArray(layout, array.length)}.
     * @param elementLayout the element layout of the array to be allocated.
     * @param array the array to be copied on the newly allocated memory block.
     * @return a segment for the newly allocated memory block.
     * @throws IllegalArgumentException if {@code elementLayout.byteSize()} does not conform to the size of a int value.
     */
    default MemorySegment allocateArray(ValueLayout elementLayout, int[] array) {
        return copyArrayWithSwapIfNeeded(array, elementLayout, MemorySegment::ofArray);
    }

    /**
     * Allocate a block of memory with given layout and initialize it with given float array.
     * @implSpec the default implementation for this method calls {@code this.allocateArray(layout, array.length)}.
     * @param elementLayout the element layout of the array to be allocated.
     * @param array the array to be copied on the newly allocated memory block.
     * @return a segment for the newly allocated memory block.
     * @throws IllegalArgumentException if {@code elementLayout.byteSize()} does not conform to the size of a float value.
     */
    default MemorySegment allocateArray(ValueLayout elementLayout, float[] array) {
        return copyArrayWithSwapIfNeeded(array, elementLayout, MemorySegment::ofArray);
    }

    /**
     * Allocate a block of memory with given layout and initialize it with given long array.
     * @implSpec the default implementation for this method calls {@code this.allocateArray(layout, array.length)}.
     * @param elementLayout the element layout of the array to be allocated.
     * @param array the array to be copied on the newly allocated memory block.
     * @return a segment for the newly allocated memory block.
     * @throws IllegalArgumentException if {@code elementLayout.byteSize()} does not conform to the size of a long value.
     */
    default MemorySegment allocateArray(ValueLayout elementLayout, long[] array) {
        return copyArrayWithSwapIfNeeded(array, elementLayout, MemorySegment::ofArray);
    }

    /**
     * Allocate a block of memory with given layout and initialize it with given double array.
     * @implSpec the default implementation for this method calls {@code this.allocateArray(layout, array.length)}.
     * @param elementLayout the element layout of the array to be allocated.
     * @param array the array to be copied on the newly allocated memory block.
     * @return a segment for the newly allocated memory block.
     * @throws IllegalArgumentException if {@code elementLayout.byteSize()} does not conform to the size of a double value.
     */
    default MemorySegment allocateArray(ValueLayout elementLayout, double[] array) {
        return copyArrayWithSwapIfNeeded(array, elementLayout, MemorySegment::ofArray);
    }

    /**
     * Allocate a block of memory with given layout and initialize it with given address array.
     * The address value of each array element might be narrowed according to the platform address size (see {@link MemoryLayouts#ADDRESS}).
     * @implSpec the default implementation for this method calls {@code this.allocateArray(layout, array.length)}.
     * @param elementLayout the element layout of the array to be allocated.
     * @param array the array to be copied on the newly allocated memory block.
     * @return a segment for the newly allocated memory block.
     * @throws IllegalArgumentException if {@code layout.byteSize() != MemoryLayouts.ADDRESS.byteSize()}.
     */
    default MemorySegment allocateArray(ValueLayout elementLayout, Addressable[] array) {
        Objects.requireNonNull(elementLayout);
        Objects.requireNonNull(array);
        Stream.of(array).forEach(Objects::requireNonNull);
        if (MemoryLayouts.ADDRESS.byteSize() != elementLayout.byteSize()) {
            throw new IllegalArgumentException("Layout size mismatch - " + elementLayout.byteSize() + " != " + MemoryLayouts.ADDRESS.byteSize());
        }
        return switch ((int)elementLayout.byteSize()) {
            case 4 -> copyArrayWithSwapIfNeeded(Stream.of(array)
                            .mapToInt(a -> (int)a.address().toRawLongValue()).toArray(),
                    elementLayout, MemorySegment::ofArray);
            case 8 -> copyArrayWithSwapIfNeeded(Stream.of(array)
                            .mapToLong(a -> a.address().toRawLongValue()).toArray(),
                    elementLayout, MemorySegment::ofArray);
            default -> throw new UnsupportedOperationException("Unsupported pointer size"); // should not get here
        };
    }

    private <Z> MemorySegment copyArrayWithSwapIfNeeded(Z array, ValueLayout elementLayout,
                                                        Function<Z, MemorySegment> heapSegmentFactory) {
        Objects.requireNonNull(array);
        Objects.requireNonNull(elementLayout);
        Utils.checkPrimitiveCarrierCompat(array.getClass().componentType(), elementLayout);
        MemorySegment addr = allocate(MemoryLayout.sequenceLayout(Array.getLength(array), elementLayout));
        if (elementLayout.byteSize() == 1 || (elementLayout.order() == ByteOrder.nativeOrder())) {
            addr.copyFrom(heapSegmentFactory.apply(array));
        } else {
            ((AbstractMemorySegmentImpl)addr).copyFromSwap(heapSegmentFactory.apply(array), elementLayout.byteSize());
        }
        return addr;
    }

    /**
     * Allocate a block of memory  with given layout.
     * @implSpec the default implementation for this method calls {@code this.allocate(layout.byteSize(), layout.byteAlignment())}.
     * @param layout the layout of the block of memory to be allocated.
     * @return a segment for the newly allocated memory block.
     */
    default MemorySegment allocate(MemoryLayout layout) {
        Objects.requireNonNull(layout);
        return allocate(layout.byteSize(), layout.byteAlignment());
    }

    /**
     * Allocate a block of memory corresponding to an array with given element layout and size.
     * @implSpec the default implementation for this method calls {@code this.allocate(MemoryLayout.sequenceLayout(count, elementLayout))}.
     * @param elementLayout the array element layout.
     * @param count the array element count.
     * @return a segment for the newly allocated memory block.
     */
    default MemorySegment allocateArray(MemoryLayout elementLayout, long count) {
        Objects.requireNonNull(elementLayout);
        return allocate(MemoryLayout.sequenceLayout(count, elementLayout));
    }

    /**
     * Allocate a block of memory with given size, with default alignment (1-byte aligned).
     * @implSpec the default implementation for this method calls {@code this.allocate(bytesSize, 1)}.
     * @param bytesSize the size (in bytes) of the block of memory to be allocated.
     * @return a segment for the newly allocated memory block.
     */
    default MemorySegment allocate(long bytesSize) {
        return allocate(bytesSize, 1);
    }

    /**
     * Allocate a block of memory  with given size and alignment constraint.
     * @param bytesSize the size (in bytes) of the block of memory to be allocated.
     * @param bytesAlignment the alignment (in bytes) of the block of memory to be allocated.
     * @return a segment for the newly allocated memory block.
     */
    MemorySegment allocate(long bytesSize, long bytesAlignment);

    /**
     * Returns a native arena-based allocator which allocates a single memory segment, of given size (using malloc),
     * and then responds to allocation request by returning different slices of that same segment
     * (until no further allocation is possible).
     * This can be useful when clients want to perform multiple allocation requests while avoiding the cost associated
     * with allocating a new off-heap memory region upon each allocation request.
     * <p>
     * An allocator associated with a <em>shared</em> resource scope is thread-safe and allocation requests may be
     * performed concurrently; conversely, if the arena allocator is associated with a <em>confined</em> resource scope,
     * allocation requests can only occur from the thread owning the allocator's resource scope.
     * <p>
     * The returned allocator might throw an {@link OutOfMemoryError} if an incoming allocation request exceeds
     * the allocator capacity.
     *
     * @param size the size (in bytes) of the allocation arena.
     * @param scope the scope associated with the segments returned by this allocator.
     * @return a new bounded arena-based allocator
     * @throws IllegalArgumentException if {@code size <= 0}.
     * @throws IllegalStateException if {@code scope} has been already closed, or if access occurs from a thread other
     * than the thread owning {@code scope}.
     */
    static SegmentAllocator arenaAllocator(long size, ResourceScope scope) {
        Objects.requireNonNull(scope);
        return scope.ownerThread() == null ?
                new ArenaAllocator.BoundedSharedArenaAllocator(scope, size) :
                new ArenaAllocator.BoundedArenaAllocator(scope, size);
    }

    /**
     * Returns a native unbounded arena-based allocator.
     * <p>
     * The returned allocator allocates a memory segment {@code S} of a certain fixed size (using malloc) and then
     * responds to allocation requests in one of the following ways:
     * <ul>
     *     <li>if the size of the allocation requests is smaller than the size of {@code S}, and {@code S} has a <em>free</em>
     *     slice {@code S'} which fits that allocation request, return that {@code S'}.
     *     <li>if the size of the allocation requests is smaller than the size of {@code S}, and {@code S} has no <em>free</em>
     *     slices which fits that allocation request, allocate a new segment {@code S'} (using malloc), which has same size as {@code S}
     *     and set {@code S = S'}; the allocator then tries to respond to the same allocation request again.
     *     <li>if the size of the allocation requests is bigger than the size of {@code S}, allocate a new segment {@code S'}
     *     (using malloc), which has a sufficient size to satisfy the allocation request, and return {@code S'}.
     * </ul>
     * <p>
     * This segment allocator can be useful when clients want to perform multiple allocation requests while avoiding the
     * cost associated with allocating a new off-heap memory region upon each allocation request.
     * <p>
     * An allocator associated with a <em>shared</em> resource scope is thread-safe and allocation requests may be
     * performed concurrently; conversely, if the arena allocator is associated with a <em>confined</em> resource scope,
     * allocation requests can only occur from the thread owning the allocator's resource scope.
     * <p>
     * The returned allocator might throw an {@link OutOfMemoryError} if an incoming allocation request exceeds
     * the system capacity.
     *
     * @param scope the scope associated with the segments returned by this allocator.
     * @return a new unbounded arena-based allocator
     * @throws IllegalStateException if {@code scope} has been already closed, or if access occurs from a thread other
     * than the thread owning {@code scope}.
     */
    static SegmentAllocator arenaAllocator(ResourceScope scope) {
        Objects.requireNonNull(scope);
        return scope.ownerThread() == null ?
                new ArenaAllocator.UnboundedSharedArenaAllocator(scope) :
                new ArenaAllocator.UnboundedArenaAllocator(scope);
    }

    /**
     * Returns a segment allocator which responds to allocation requests by recycling a single segment; that is,
     * each new allocation request will return a new slice starting at the segment offset {@code 0} (alignment
     * constraints are ignored by this allocator). This can be useful to limit allocation requests in case a client
     * knows that they have fully processed the contents of the allocated segment before the subsequent allocation request
     * takes place.
     * <p>
     * While the allocator returned by this method is <em>thread-safe</em>, concurrent access on the same recycling
     * allocator might cause a thread to overwrite contents written to the underlying segment by a different thread.
     *
     * @param segment the memory segment to be recycled by the returned allocator.
     * @return an allocator which recycles an existing segment upon each new allocation request.
     */
    static SegmentAllocator ofSegment(MemorySegment segment) {
        Objects.requireNonNull(segment);
        return (size, align) -> segment.asSlice(0, size);
    }

    /**
     * Returns a native allocator which responds to allocation requests by allocating new segments
     * bound by the given resource scope, using the {@link MemorySegment#allocateNative(long, long, ResourceScope)}
     * factory. This code is equivalent (but likely more efficient) to the following:
     * <blockquote><pre>{@code
    Resource scope = ...
    SegmentAllocator scoped = (size, align) -> MemorySegment.allocateNative(size, align, scope);
     * }</pre></blockquote>
     *
     * @param scope the resource scope associated with the segments created by the returned allocator.
     * @return an allocator which allocates new memory segment bound by the provided resource scope.
     */
    static SegmentAllocator ofScope(ResourceScope scope) {
        Objects.requireNonNull(scope);
        return (ResourceScopeImpl)scope;
    }
}
