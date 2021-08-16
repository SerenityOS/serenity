/*
 *  Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
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

package jdk.incubator.foreign;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

import jdk.internal.foreign.AbstractMemorySegmentImpl;
import jdk.internal.foreign.HeapMemorySegmentImpl;
import jdk.internal.foreign.MappedMemorySegmentImpl;
import jdk.internal.foreign.ResourceScopeImpl;
import jdk.internal.foreign.NativeMemorySegmentImpl;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Spliterator;
import java.util.stream.Stream;

/**
 * A memory segment models a contiguous region of memory. A memory segment is associated with both spatial
 * and temporal bounds (e.g. a {@link ResourceScope}). Spatial bounds ensure that memory access operations on a memory segment cannot affect a memory location
 * which falls <em>outside</em> the boundaries of the memory segment being accessed. Temporal bounds ensure that memory access
 * operations on a segment cannot occur after the resource scope associated with a memory segment has been closed (see {@link ResourceScope#close()}).
 * <p>
 * All implementations of this interface must be <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>;
 * programmers should treat instances that are {@linkplain Object#equals(Object) equal} as interchangeable and should not
 * use instances for synchronization, or unpredictable behavior may occur. For example, in a future release,
 * synchronization may fail. The {@code equals} method should be used for comparisons.
 * <p>
 * Non-platform classes should not implement {@linkplain MemorySegment} directly.
 *
 * <p> Unless otherwise specified, passing a {@code null} argument, or an array argument containing one or more {@code null}
 * elements to a method in this class causes a {@link NullPointerException NullPointerException} to be thrown. </p>
 *
 * <h2>Constructing memory segments</h2>
 *
 * There are multiple ways to obtain a memory segment. First, memory segments backed by off-heap memory can
 * be allocated using one of the many factory methods provided (see {@link MemorySegment#allocateNative(MemoryLayout, ResourceScope)},
 * {@link MemorySegment#allocateNative(long, ResourceScope)} and {@link MemorySegment#allocateNative(long, long, ResourceScope)}). Memory segments obtained
 * in this way are called <em>native memory segments</em>.
 * <p>
 * It is also possible to obtain a memory segment backed by an existing heap-allocated Java array,
 * using one of the provided factory methods (e.g. {@link MemorySegment#ofArray(int[])}). Memory segments obtained
 * in this way are called <em>array memory segments</em>.
 * <p>
 * It is possible to obtain a memory segment backed by an existing Java byte buffer (see {@link ByteBuffer}),
 * using the factory method {@link MemorySegment#ofByteBuffer(ByteBuffer)}.
 * Memory segments obtained in this way are called <em>buffer memory segments</em>. Note that buffer memory segments might
 * be backed by native memory (as in the case of native memory segments) or heap memory (as in the case of array memory segments),
 * depending on the characteristics of the byte buffer instance the segment is associated with. For instance, a buffer memory
 * segment obtained from a byte buffer created with the {@link ByteBuffer#allocateDirect(int)} method will be backed
 * by native memory.
 *
 * <h2>Mapping memory segments from files</h2>
 *
 * It is also possible to obtain a native memory segment backed by a memory-mapped file using the factory method
 * {@link MemorySegment#mapFile(Path, long, long, FileChannel.MapMode, ResourceScope)}. Such native memory segments are
 * called <em>mapped memory segments</em>; mapped memory segments are associated with an underlying file descriptor.
 * <p>
 * Contents of mapped memory segments can be {@linkplain #force() persisted} and {@linkplain #load() loaded} to and from the underlying file;
 * these capabilities are suitable replacements for some of the functionality in the {@link java.nio.MappedByteBuffer} class.
 * Note that, while it is possible to map a segment into a byte buffer (see {@link MemorySegment#asByteBuffer()}),
 * and then call e.g. {@link java.nio.MappedByteBuffer#force()} that way, this can only be done when the source segment
 * is small enough, due to the size limitation inherent to the ByteBuffer API.
 * <p>
 * Clients requiring sophisticated, low-level control over mapped memory segments, should consider writing
 * custom mapped memory segment factories; using {@link CLinker}, e.g. on Linux, it is possible to call {@code mmap}
 * with the desired parameters; the returned address can be easily wrapped into a memory segment, using
 * {@link MemoryAddress#ofLong(long)} and {@link MemoryAddress#asSegment(long, Runnable, ResourceScope)}.
 *
 * <h2>Lifecycle and confinement</h2>
 *
 * Memory segments are associated with a resource scope (see {@link ResourceScope}), which can be accessed using
 * the {@link #scope()} method. As for all resources associated with a resource scope, a segment cannot be
 * accessed after its corresponding scope has been closed. For instance, the following code will result in an
 * exception:
 * <blockquote><pre>{@code
MemorySegment segment = null;
try (ResourceScope scope = ResourceScope.newConfinedScope()) {
    segment = MemorySegment.allocateNative(8, 1, scope);
}
MemoryAccess.getLong(segment); // already closed!
 * }</pre></blockquote>
 * Additionally, access to a memory segment is subject to the thread-confinement checks enforced by the owning scope; that is,
 * if the segment is associated with a shared scope, it can be accessed by multiple threads; if it is associated with a confined
 * scope, it can only be accessed by the thread which owns the scope.
 * <p>
 * Heap and buffer segments are always associated with a <em>global</em>, shared scope. This scope cannot be closed,
 * and can be considered as <em>always alive</em>.
 *
 * <h2>Memory segment views</h2>
 *
 * Memory segments support <em>views</em>. For instance, it is possible to create an <em>immutable</em> view of a memory segment, as follows:
 * <blockquote><pre>{@code
MemorySegment segment = ...
MemorySegment roSegment = segment.asReadOnly();
 * }</pre></blockquote>
 * It is also possible to create views whose spatial bounds are stricter than the ones of the original segment
 * (see {@link MemorySegment#asSlice(long, long)}).
 * <p>
 * Temporal bounds of the original segment are inherited by the views; that is, when the scope associated with a segment
 * is closed, all the views associated with that segment will also be rendered inaccessible.
 * <p>
 * To allow for interoperability with existing code, a byte buffer view can be obtained from a memory segment
 * (see {@link #asByteBuffer()}). This can be useful, for instance, for those clients that want to keep using the
 * {@link ByteBuffer} API, but need to operate on large memory segments. Byte buffers obtained in such a way support
 * the same spatial and temporal access restrictions associated with the memory segment from which they originated.
 *
 * <h2>Stream support</h2>
 *
 * A client might obtain a {@link Stream} from a segment, which can then be used to slice the segment (according to a given
 * element layout) and even allow multiple threads to work in parallel on disjoint segment slices
 * (to do this, the segment has to be associated with a shared scope). The following code can be used to sum all int
 * values in a memory segment in parallel:
 *
 * <blockquote><pre>{@code
try (ResourceScope scope = ResourceScope.newSharedScope()) {
    SequenceLayout SEQUENCE_LAYOUT = MemoryLayout.sequenceLayout(1024, MemoryLayouts.JAVA_INT);
    MemorySegment segment = MemorySegment.allocateNative(SEQUENCE_LAYOUT, scope);
    VarHandle VH_int = SEQUENCE_LAYOUT.elementLayout().varHandle(int.class);
    int sum = segment.elements(MemoryLayouts.JAVA_INT).parallel()
                           .mapToInt(s -> (int)VH_int.get(s.address()))
                           .sum();
}
 * }</pre></blockquote>
 *
 * @implSpec
 * Implementations of this interface are immutable, thread-safe and <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>.
 */
public sealed interface MemorySegment extends Addressable permits AbstractMemorySegmentImpl {

    /**
     * The base memory address associated with this memory segment.
     * The returned memory address is associated with same resource scope as that associated with this segment.
     * @return The base memory address.
     */
    @Override
    MemoryAddress address();

    /**
     * Returns a spliterator for this memory segment. The returned spliterator reports {@link Spliterator#SIZED},
     * {@link Spliterator#SUBSIZED}, {@link Spliterator#IMMUTABLE}, {@link Spliterator#NONNULL} and {@link Spliterator#ORDERED}
     * characteristics.
     * <p>
     * The returned spliterator splits this segment according to the specified element layout; that is,
     * if the supplied layout has size N, then calling {@link Spliterator#trySplit()} will result in a spliterator serving
     * approximately {@code S/N/2} elements (depending on whether N is even or not), where {@code S} is the size of
     * this segment. As such, splitting is possible as long as {@code S/N >= 2}. The spliterator returns segments that feature the same
     * scope as this given segment.
     * <p>
     * The returned spliterator effectively allows to slice this segment into disjoint sub-segments, which can then
     * be processed in parallel by multiple threads.
     *
     * @param elementLayout the layout to be used for splitting.
     * @return the element spliterator for this segment
     * @throws IllegalArgumentException if the {@code elementLayout} size is zero, or the segment size modulo the
     * {@code elementLayout} size is greater than zero.
     */
    Spliterator<MemorySegment> spliterator(MemoryLayout elementLayout);

    /**
     * Returns a sequential {@code Stream} over disjoint slices (whose size matches that of the specified layout)
     * in this segment. Calling this method is equivalent to the following code:
     * <blockquote><pre>{@code
    StreamSupport.stream(segment.spliterator(elementLayout), false);
     * }</pre></blockquote>
     *
     * @param elementLayout the layout to be used for splitting.
     * @return a sequential {@code Stream} over disjoint slices in this segment.
     * @throws IllegalArgumentException if the {@code elementLayout} size is zero, or the segment size modulo the
     * {@code elementLayout} size is greater than zero.
     */
    Stream<MemorySegment> elements(MemoryLayout elementLayout);

    /**
     * Returns the resource scope associated with this memory segment.
     * @return the resource scope associated with this memory segment.
     */
    ResourceScope scope();

    /**
     * The size (in bytes) of this memory segment.
     * @return The size (in bytes) of this memory segment.
     */
    long byteSize();

    /**
     * Obtains a new memory segment view whose base address is the same as the base address of this segment plus a given offset,
     * and whose new size is specified by the given argument.
     *
     * @see #asSlice(long)
     * @see #asSlice(MemoryAddress)
     * @see #asSlice(MemoryAddress, long)
     *
     * @param offset The new segment base offset (relative to the current segment base address), specified in bytes.
     * @param newSize The new segment size, specified in bytes.
     * @return a new memory segment view with updated base/limit addresses.
     * @throws IndexOutOfBoundsException if {@code offset < 0}, {@code offset > byteSize()}, {@code newSize < 0}, or {@code newSize > byteSize() - offset}
     */
    MemorySegment asSlice(long offset, long newSize);

    /**
     * Obtains a new memory segment view whose base address is the given address, and whose new size is specified by the given argument.
     * <p>
     * Equivalent to the following code:
     * <pre>{@code
    asSlice(newBase.segmentOffset(this), newSize);
     * }</pre>
     *
     * @see #asSlice(long)
     * @see #asSlice(MemoryAddress)
     * @see #asSlice(long, long)
     *
     * @param newBase The new segment base address.
     * @param newSize The new segment size, specified in bytes.
     * @return a new memory segment view with updated base/limit addresses.
     * @throws IndexOutOfBoundsException if {@code offset < 0}, {@code offset > byteSize()}, {@code newSize < 0}, or {@code newSize > byteSize() - offset}
     */
    default MemorySegment asSlice(MemoryAddress newBase, long newSize) {
        Objects.requireNonNull(newBase);
        return asSlice(newBase.segmentOffset(this), newSize);
    }

    /**
     * Obtains a new memory segment view whose base address is the same as the base address of this segment plus a given offset,
     * and whose new size is computed by subtracting the specified offset from this segment size.
     * <p>
     * Equivalent to the following code:
     * <pre>{@code
    asSlice(offset, byteSize() - offset);
     * }</pre>
     *
     * @see #asSlice(MemoryAddress)
     * @see #asSlice(MemoryAddress, long)
     * @see #asSlice(long, long)
     *
     * @param offset The new segment base offset (relative to the current segment base address), specified in bytes.
     * @return a new memory segment view with updated base/limit addresses.
     * @throws IndexOutOfBoundsException if {@code offset < 0}, or {@code offset > byteSize()}.
     */
    default MemorySegment asSlice(long offset) {
        return asSlice(offset, byteSize() - offset);
    }

    /**
     * Obtains a new memory segment view whose base address is the given address, and whose new size is computed by subtracting
     * the address offset relative to this segment (see {@link MemoryAddress#segmentOffset(MemorySegment)}) from this segment size.
     * <p>
     * Equivalent to the following code:
     * <pre>{@code
    asSlice(newBase.segmentOffset(this));
     * }</pre>
     *
     * @see #asSlice(long)
     * @see #asSlice(MemoryAddress, long)
     * @see #asSlice(long, long)
     *
     * @param newBase The new segment base offset (relative to the current segment base address), specified in bytes.
     * @return a new memory segment view with updated base/limit addresses.
     * @throws IndexOutOfBoundsException if {@code address.segmentOffset(this) < 0}, or {@code address.segmentOffset(this) > byteSize()}.
     */
    default MemorySegment asSlice(MemoryAddress newBase) {
        Objects.requireNonNull(newBase);
        return asSlice(newBase.segmentOffset(this));
    }

    /**
     * Is this segment read-only?
     * @return {@code true}, if this segment is read-only.
     * @see #asReadOnly()
     */
    boolean isReadOnly();

    /**
     * Obtains a read-only view of this segment. The resulting segment will be identical to this one, but
     * attempts to overwrite the contents of the returned segment will cause runtime exceptions.
     * @return a read-only view of this segment
     * @see #isReadOnly()
     */
    MemorySegment asReadOnly();

    /**
     * Is this a native segment? Returns true if this segment is a native memory segment,
     * created using the {@link #allocateNative(long, ResourceScope)} (and related) factory, or a buffer segment
     * derived from a direct {@link java.nio.ByteBuffer} using the {@link #ofByteBuffer(ByteBuffer)} factory,
     * or if this is a {@linkplain #isMapped() mapped} segment.
     * @return {@code true} if this segment is native segment.
     */
    boolean isNative();

    /**
     * Is this a mapped segment? Returns true if this segment is a mapped memory segment,
     * created using the {@link #mapFile(Path, long, long, FileChannel.MapMode, ResourceScope)} factory, or a buffer segment
     * derived from a {@link java.nio.MappedByteBuffer} using the {@link #ofByteBuffer(ByteBuffer)} factory.
     * @return {@code true} if this segment is a mapped segment.
     */
    boolean isMapped();

    /**
     * Fills a value into this memory segment.
     * <p>
     * More specifically, the given value is filled into each address of this
     * segment. Equivalent to (but likely more efficient than) the following code:
     *
     * <pre>{@code
byteHandle = MemoryLayout.ofSequence(MemoryLayouts.JAVA_BYTE)
         .varHandle(byte.class, MemoryLayout.PathElement.sequenceElement());
for (long l = 0; l < segment.byteSize(); l++) {
     byteHandle.set(segment.address(), l, value);
}
     * }</pre>
     *
     * without any regard or guarantees on the ordering of particular memory
     * elements being set.
     * <p>
     * Fill can be useful to initialize or reset the memory of a segment.
     *
     * @param value the value to fill into this segment
     * @return this memory segment
     * @throws IllegalStateException if the scope associated with this segment has been closed, or if access occurs from
     * a thread other than the thread owning that scope,
     * @throws UnsupportedOperationException if this segment is read-only (see {@link #isReadOnly()}).
     */
    MemorySegment fill(byte value);

    /**
     * Performs a bulk copy from given source segment to this segment. More specifically, the bytes at
     * offset {@code 0} through {@code src.byteSize() - 1} in the source segment are copied into this segment
     * at offset {@code 0} through {@code src.byteSize() - 1}.
     * If the source segment overlaps with this segment, then the copying is performed as if the bytes at
     * offset {@code 0} through {@code src.byteSize() - 1} in the source segment were first copied into a
     * temporary segment with size {@code bytes}, and then the contents of the temporary segment were copied into
     * this segment at offset {@code 0} through {@code src.byteSize() - 1}.
     * <p>
     * The result of a bulk copy is unspecified if, in the uncommon case, the source segment and this segment
     * do not overlap, but refer to overlapping regions of the same backing storage using different addresses.
     * For example, this may occur if the same file is {@linkplain MemorySegment#mapFile mapped} to two segments.
     *
     * @param src the source segment.
     * @throws IndexOutOfBoundsException if {@code src.byteSize() > this.byteSize()}.
     * @throws IllegalStateException if either the scope associated with the source segment or the scope associated
     * with this segment have been already closed, or if access occurs from a thread other than the thread owning either
     * scopes.
     * @throws UnsupportedOperationException if this segment is read-only (see {@link #isReadOnly()}).
     */
    void copyFrom(MemorySegment src);

    /**
     * Finds and returns the offset, in bytes, of the first mismatch between
     * this segment and a given other segment. The offset is relative to the
     * {@linkplain #address() base address} of each segment and will be in the
     * range of 0 (inclusive) up to the {@linkplain #byteSize() size} (in bytes) of
     * the smaller memory segment (exclusive).
     * <p>
     * If the two segments share a common prefix then the returned offset is
     * the length of the common prefix and it follows that there is a mismatch
     * between the two segments at that offset within the respective segments.
     * If one segment is a proper prefix of the other then the returned offset is
     * the smaller of the segment sizes, and it follows that the offset is only
     * valid for the larger segment. Otherwise, there is no mismatch and {@code
     * -1} is returned.
     *
     * @param other the segment to be tested for a mismatch with this segment
     * @return the relative offset, in bytes, of the first mismatch between this
     * and the given other segment, otherwise -1 if no mismatch
     * @throws IllegalStateException if either the scope associated with this segment or the scope associated
     * with the {@code other} segment have been already closed, or if access occurs from a thread other than the thread
     * owning either scopes.
     */
    long mismatch(MemorySegment other);

    /**
     * Tells whether or not the contents of this mapped segment is resident in physical
     * memory.
     *
     * <p> A return value of {@code true} implies that it is highly likely
     * that all of the data in this segment is resident in physical memory and
     * may therefore be accessed without incurring any virtual-memory page
     * faults or I/O operations.  A return value of {@code false} does not
     * necessarily imply that this segment's content is not resident in physical
     * memory.
     *
     * <p> The returned value is a hint, rather than a guarantee, because the
     * underlying operating system may have paged out some of this segment's data
     * by the time that an invocation of this method returns.  </p>
     *
     * @return  {@code true} if it is likely that the contents of this segment
     *          is resident in physical memory
     *
     * @throws IllegalStateException if the scope associated with this segment has been closed, or if access occurs from
     * a thread other than the thread owning that scope.
     * @throws UnsupportedOperationException if this segment is not a mapped memory segment, e.g. if
     * {@code isMapped() == false}.
     */
    boolean isLoaded();

    /**
     * Loads the contents of this mapped segment into physical memory.
     *
     * <p> This method makes a best effort to ensure that, when it returns,
     * this contents of this segment is resident in physical memory.  Invoking this
     * method may cause some number of page faults and I/O operations to
     * occur. </p>
     *
     * @throws IllegalStateException if the scope associated with this segment has been closed, or if access occurs from
     * a thread other than the thread owning that scope.
     * @throws UnsupportedOperationException if this segment is not a mapped memory segment, e.g. if
     * {@code isMapped() == false}.
     */
    void load();

    /**
     * Unloads the contents of this mapped segment from physical memory.
     *
     * <p> This method makes a best effort to ensure that the contents of this segment are
     * are no longer resident in physical memory. Accessing this segment's contents
     * after invoking this method may cause some number of page faults and I/O operations to
     * occur (as this segment's contents might need to be paged back in). </p>
     *
     * @throws IllegalStateException if the scope associated with this segment has been closed, or if access occurs from
     * a thread other than the thread owning that scope.
     * @throws UnsupportedOperationException if this segment is not a mapped memory segment, e.g. if
     * {@code isMapped() == false}.
     */
    void unload();

    /**
     * Forces any changes made to the contents of this mapped segment to be written to the
     * storage device described by the mapped segment's file descriptor.
     *
     * <p> If the file descriptor associated with this mapped segment resides on a local storage
     * device then when this method returns it is guaranteed that all changes
     * made to this segment since it was created, or since this method was last
     * invoked, will have been written to that device.
     *
     * <p> If the file descriptor associated with this mapped segment does not reside on a local device then
     * no such guarantee is made.
     *
     * <p> If this segment was not mapped in read/write mode ({@link
     * java.nio.channels.FileChannel.MapMode#READ_WRITE}) then
     * invoking this method may have no effect. In particular, the
     * method has no effect for segments mapped in read-only or private
     * mapping modes. This method may or may not have an effect for
     * implementation-specific mapping modes.
     * </p>
     *
     * @throws IllegalStateException if the scope associated with this segment has been closed, or if access occurs from
     * a thread other than the thread owning that scope.
     * @throws UnsupportedOperationException if this segment is not a mapped memory segment, e.g. if
     * {@code isMapped() == false}.
     * @throws UncheckedIOException if there is an I/O error writing the contents of this segment to the associated storage device
     */
    void force();

    /**
     * Wraps this segment in a {@link ByteBuffer}. Some of the properties of the returned buffer are linked to
     * the properties of this segment. For instance, if this segment is <em>immutable</em>
     * (e.g. the segment is a read-only segment, see {@link #isReadOnly()}), then the resulting buffer is <em>read-only</em>
     * (see {@link ByteBuffer#isReadOnly()}. Additionally, if this is a native memory segment, the resulting buffer is
     * <em>direct</em> (see {@link ByteBuffer#isDirect()}).
     * <p>
     * The returned buffer's position (see {@link ByteBuffer#position()} is initially set to zero, while
     * the returned buffer's capacity and limit (see {@link ByteBuffer#capacity()} and {@link ByteBuffer#limit()}, respectively)
     * are set to this segment' size (see {@link MemorySegment#byteSize()}). For this reason, a byte buffer cannot be
     * returned if this segment' size is greater than {@link Integer#MAX_VALUE}.
     * <p>
     * The life-cycle of the returned buffer will be tied to that of this segment. That is, accessing the returned buffer
     * after the scope associated with this segment has been closed (see {@link ResourceScope#close()}, will throw an {@link IllegalStateException}.
     * <p>
     * If this segment is associated with a confined scope, calling read/write I/O operations on the resulting buffer
     * might result in an unspecified exception being thrown. Examples of such problematic operations are
     * {@link java.nio.channels.AsynchronousSocketChannel#read(ByteBuffer)} and
     * {@link java.nio.channels.AsynchronousSocketChannel#write(ByteBuffer)}.
     * <p>
     * Finally, the resulting buffer's byte order is {@link java.nio.ByteOrder#BIG_ENDIAN}; this can be changed using
     * {@link ByteBuffer#order(java.nio.ByteOrder)}.
     *
     * @return a {@link ByteBuffer} view of this memory segment.
     * @throws UnsupportedOperationException if this segment cannot be mapped onto a {@link ByteBuffer} instance,
     * e.g. because it models an heap-based segment that is not based on a {@code byte[]}), or if its size is greater
     * than {@link Integer#MAX_VALUE}.
     */
    ByteBuffer asByteBuffer();

    /**
     * Copy the contents of this memory segment into a fresh byte array.
     * @return a fresh byte array copy of this memory segment.
     * @throws IllegalStateException if the scope associated with this segment has been closed, or if access occurs from
     * a thread other than the thread owning that scope, or if this segment's contents cannot be copied into a {@link byte[]} instance,
     * e.g. its size is greater than {@link Integer#MAX_VALUE}.
     */
    byte[] toByteArray();

    /**
     * Copy the contents of this memory segment into a fresh short array.
     * @return a fresh short array copy of this memory segment.
     * @throws IllegalStateException if the scope associated with this segment has been closed, or if access occurs from
     * a thread other than the thread owning that scope, or if this segment's contents cannot be copied into a {@link short[]} instance,
     * e.g. because {@code byteSize() % 2 != 0}, or {@code byteSize() / 2 > Integer#MAX_VALUE}
     */
    short[] toShortArray();

    /**
     * Copy the contents of this memory segment into a fresh char array.
     * @return a fresh char array copy of this memory segment.
     * @throws IllegalStateException if the scope associated with this segment has been closed, or if access occurs from
     * a thread other than the thread owning that scope, or if this segment's contents cannot be copied into a {@link char[]} instance,
     * e.g. because {@code byteSize() % 2 != 0}, or {@code byteSize() / 2 > Integer#MAX_VALUE}.
     */
    char[] toCharArray();

    /**
     * Copy the contents of this memory segment into a fresh int array.
     * @return a fresh int array copy of this memory segment.
     * @throws IllegalStateException if the scope associated with this segment has been closed, or if access occurs from
     * a thread other than the thread owning that scope, or if this segment's contents cannot be copied into a {@link int[]} instance,
     * e.g. because {@code byteSize() % 4 != 0}, or {@code byteSize() / 4 > Integer#MAX_VALUE}.
     */
    int[] toIntArray();

    /**
     * Copy the contents of this memory segment into a fresh float array.
     * @return a fresh float array copy of this memory segment.
     * @throws IllegalStateException if the scope associated with this segment has been closed, or if access occurs from
     * a thread other than the thread owning that scope, or if this segment's contents cannot be copied into a {@link float[]} instance,
     * e.g. because {@code byteSize() % 4 != 0}, or {@code byteSize() / 4 > Integer#MAX_VALUE}.
     */
    float[] toFloatArray();

    /**
     * Copy the contents of this memory segment into a fresh long array.
     * @return a fresh long array copy of this memory segment.
     * @throws IllegalStateException if the scope associated with this segment has been closed, or if access occurs from
     * a thread other than the thread owning that scope, or if this segment's contents cannot be copied into a {@link long[]} instance,
     * e.g. because {@code byteSize() % 8 != 0}, or {@code byteSize() / 8 > Integer#MAX_VALUE}.
     */
    long[] toLongArray();

    /**
     * Copy the contents of this memory segment into a fresh double array.
     * @return a fresh double array copy of this memory segment.
     * @throws IllegalStateException if the scope associated with this segment has been closed, or if access occurs from
     * a thread other than the thread owning that scope, or if this segment's contents cannot be copied into a {@link double[]} instance,
     * e.g. because {@code byteSize() % 8 != 0}, or {@code byteSize() / 8 > Integer#MAX_VALUE}.
     */
    double[] toDoubleArray();

    /**
     * Creates a new confined buffer memory segment that models the memory associated with the given byte
     * buffer. The segment starts relative to the buffer's position (inclusive)
     * and ends relative to the buffer's limit (exclusive).
     * <p>
     * If the buffer is {@link ByteBuffer#isReadOnly() read-only}, the resulting segment will also be
     * {@link ByteBuffer#isReadOnly() read-only}. The scope associated with this segment can either be the
     * {@linkplain ResourceScope#globalScope() global} resource scope, in case the buffer has been created independently,
     * or to some other (possibly closeable) resource scope, in case the buffer has been obtained using {@link #asByteBuffer()}.
     * <p>
     * The resulting memory segment keeps a reference to the backing buffer, keeping it <em>reachable</em>.
     *
     * @param bb the byte buffer backing the buffer memory segment.
     * @return a new buffer memory segment.
     */
    static MemorySegment ofByteBuffer(ByteBuffer bb) {
        return AbstractMemorySegmentImpl.ofBuffer(bb);
    }

    /**
     * Creates a new confined array memory segment that models the memory associated with a given heap-allocated byte array.
     * The returned segment's resource scope is set to the {@linkplain ResourceScope#globalScope() global} resource scope.
     *
     * @param arr the primitive array backing the array memory segment.
     * @return a new array memory segment.
     */
    static MemorySegment ofArray(byte[] arr) {
        return HeapMemorySegmentImpl.OfByte.fromArray(arr);
    }

    /**
     * Creates a new confined array memory segment that models the memory associated with a given heap-allocated char array.
     * The returned segment's resource scope is set to the {@linkplain ResourceScope#globalScope() global} resource scope.
     *
     * @param arr the primitive array backing the array memory segment.
     * @return a new array memory segment.
     */
    static MemorySegment ofArray(char[] arr) {
        return HeapMemorySegmentImpl.OfChar.fromArray(arr);
    }

    /**
     * Creates a new confined array memory segment that models the memory associated with a given heap-allocated short array.
     * The returned segment's resource scope is set to the {@linkplain ResourceScope#globalScope() global} resource scope.
     *
     * @param arr the primitive array backing the array memory segment.
     * @return a new array memory segment.
     */
    static MemorySegment ofArray(short[] arr) {
        return HeapMemorySegmentImpl.OfShort.fromArray(arr);
    }

    /**
     * Creates a new confined array memory segment that models the memory associated with a given heap-allocated int array.
     * The returned segment's resource scope is set to the {@linkplain ResourceScope#globalScope() global} resource scope.
     *
     * @param arr the primitive array backing the array memory segment.
     * @return a new array memory segment.
     */
    static MemorySegment ofArray(int[] arr) {
        return HeapMemorySegmentImpl.OfInt.fromArray(arr);
    }

    /**
     * Creates a new confined array memory segment that models the memory associated with a given heap-allocated float array.
     * The returned segment's resource scope is set to the {@linkplain ResourceScope#globalScope() global} resource scope.
     *
     * @param arr the primitive array backing the array memory segment.
     * @return a new array memory segment.
     */
    static MemorySegment ofArray(float[] arr) {
        return HeapMemorySegmentImpl.OfFloat.fromArray(arr);
    }

    /**
     * Creates a new confined array memory segment that models the memory associated with a given heap-allocated long array.
     * The returned segment's resource scope is set to the {@linkplain ResourceScope#globalScope() global} resource scope.
     *
     * @param arr the primitive array backing the array memory segment.
     * @return a new array memory segment.
     */
    static MemorySegment ofArray(long[] arr) {
        return HeapMemorySegmentImpl.OfLong.fromArray(arr);
    }

    /**
     * Creates a new confined array memory segment that models the memory associated with a given heap-allocated double array.
     * The returned segment's resource scope is set to the {@linkplain ResourceScope#globalScope() global} resource scope.
     *
     * @param arr the primitive array backing the array memory segment.
     * @return a new array memory segment.
     */
    static MemorySegment ofArray(double[] arr) {
        return HeapMemorySegmentImpl.OfDouble.fromArray(arr);
    }

    /**
     * Creates a new confined native memory segment that models a newly allocated block of off-heap memory with given layout
     * and resource scope. A client is responsible make sure that the resource scope associated with the returned segment is closed
     * when the segment is no longer in use. Failure to do so will result in off-heap memory leaks.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    allocateNative(layout.bytesSize(), layout.bytesAlignment(), scope);
     * }</pre></blockquote>
     * <p>
     * The block of off-heap memory associated with the returned native memory segment is initialized to zero.
     *
     * @param layout the layout of the off-heap memory block backing the native memory segment.
     * @param scope the segment scope.
     * @return a new native memory segment.
     * @throws IllegalArgumentException if the specified layout has illegal size or alignment constraint.
     * @throws IllegalStateException if {@code scope} has been already closed, or if access occurs from a thread other
     * than the thread owning {@code scope}.
     */
    static MemorySegment allocateNative(MemoryLayout layout, ResourceScope scope) {
        Objects.requireNonNull(scope);
        Objects.requireNonNull(layout);
        return allocateNative(layout.byteSize(), layout.byteAlignment(), scope);
    }

    /**
     * Creates a new confined native memory segment that models a newly allocated block of off-heap memory with given size (in bytes)
     * and resource scope. A client is responsible make sure that the resource scope associated with the returned segment is closed
     * when the segment is no longer in use. Failure to do so will result in off-heap memory leaks.
     * <p>
     * This is equivalent to the following code:
     * <blockquote><pre>{@code
    allocateNative(bytesSize, 1, scope);
     * }</pre></blockquote>
     * <p>
     * The block of off-heap memory associated with the returned native memory segment is initialized to zero.
     *
     * @param bytesSize the size (in bytes) of the off-heap memory block backing the native memory segment.
     * @param scope the segment scope.
     * @return a new native memory segment.
     * @throws IllegalArgumentException if {@code bytesSize <= 0}.
     * @throws IllegalStateException if {@code scope} has been already closed, or if access occurs from a thread other
     * than the thread owning {@code scope}.
     */
    static MemorySegment allocateNative(long bytesSize, ResourceScope scope) {
        return allocateNative(bytesSize, 1, scope);
    }

    /**
     * Creates a new confined native memory segment that models a newly allocated block of off-heap memory with given size
     * (in bytes), alignment constraint (in bytes) and resource scope. A client is responsible make sure that the resource
     * scope associated with the returned segment is closed when the segment is no longer in use.
     * Failure to do so will result in off-heap memory leaks.
     * <p>
     * The block of off-heap memory associated with the returned native memory segment is initialized to zero.
     *
     * @param bytesSize the size (in bytes) of the off-heap memory block backing the native memory segment.
     * @param alignmentBytes the alignment constraint (in bytes) of the off-heap memory block backing the native memory segment.
     * @param scope the segment scope.
     * @return a new native memory segment.
     * @throws IllegalArgumentException if {@code bytesSize <= 0}, {@code alignmentBytes <= 0}, or if {@code alignmentBytes}
     * is not a power of 2.
     * @throws IllegalStateException if {@code scope} has been already closed, or if access occurs from a thread other
     * than the thread owning {@code scope}.
     */
    static MemorySegment allocateNative(long bytesSize, long alignmentBytes, ResourceScope scope) {
        Objects.requireNonNull(scope);
        if (bytesSize <= 0) {
            throw new IllegalArgumentException("Invalid allocation size : " + bytesSize);
        }

        if (alignmentBytes <= 0 ||
                ((alignmentBytes & (alignmentBytes - 1)) != 0L)) {
            throw new IllegalArgumentException("Invalid alignment constraint : " + alignmentBytes);
        }

        return NativeMemorySegmentImpl.makeNativeSegment(bytesSize, alignmentBytes, (ResourceScopeImpl) scope);
    }

    /**
     * Creates a new mapped memory segment that models a memory-mapped region of a file from a given path.
     * <p>
     * If the specified mapping mode is {@linkplain FileChannel.MapMode#READ_ONLY READ_ONLY}, the resulting segment
     * will be read-only (see {@link #isReadOnly()}).
     * <p>
     * The content of a mapped memory segment can change at any time, for example
     * if the content of the corresponding region of the mapped file is changed by
     * this (or another) program.  Whether or not such changes occur, and when they
     * occur, is operating-system dependent and therefore unspecified.
     * <p>
     * All or part of a mapped memory segment may become
     * inaccessible at any time, for example if the backing mapped file is truncated.  An
     * attempt to access an inaccessible region of a mapped memory segment will not
     * change the segment's content and will cause an unspecified exception to be
     * thrown either at the time of the access or at some later time.  It is
     * therefore strongly recommended that appropriate precautions be taken to
     * avoid the manipulation of a mapped file by this (or another) program, except to read or write
     * the file's content.
     *
     * @implNote When obtaining a mapped segment from a newly created file, the initialization state of the contents of the block
     * of mapped memory associated with the returned mapped memory segment is unspecified and should not be relied upon.
     *
     * @param path the path to the file to memory map.
     * @param bytesOffset the offset (expressed in bytes) within the file at which the mapped segment is to start.
     * @param bytesSize the size (in bytes) of the mapped memory backing the memory segment.
     * @param mapMode a file mapping mode, see {@link FileChannel#map(FileChannel.MapMode, long, long)}; the chosen mapping mode
     *                might affect the behavior of the returned memory mapped segment (see {@link #force()}).
     * @param scope the segment scope.
     * @return a new confined mapped memory segment.
     * @throws IllegalArgumentException if {@code bytesOffset < 0}, {@code bytesSize < 0}, or if {@code path} is not associated
     * with the default file system.
     * @throws IllegalStateException if {@code scope} has been already closed, or if access occurs from a thread other
     * than the thread owning {@code scope}.
     * @throws UnsupportedOperationException if an unsupported map mode is specified.
     * @throws IOException if the specified path does not point to an existing file, or if some other I/O error occurs.
     * @throws  SecurityException If a security manager is installed and it denies an unspecified permission required by the implementation.
     * In the case of the default provider, the {@link SecurityManager#checkRead(String)} method is invoked to check
     * read access if the file is opened for reading. The {@link SecurityManager#checkWrite(String)} method is invoked to check
     * write access if the file is opened for writing.
     */
    static MemorySegment mapFile(Path path, long bytesOffset, long bytesSize, FileChannel.MapMode mapMode, ResourceScope scope) throws IOException {
        Objects.requireNonNull(scope);
        return MappedMemorySegmentImpl.makeMappedSegment(path, bytesOffset, bytesSize, mapMode, (ResourceScopeImpl) scope);
    }

    /**
     * Returns a native memory segment whose base address is {@link MemoryAddress#NULL} and whose size is {@link Long#MAX_VALUE}.
     * This method can be very useful when dereferencing memory addresses obtained when interacting with native libraries.
     * The returned segment is associated with the <em>global</em> resource scope (see {@link ResourceScope#globalScope()}).
     * Equivalent to (but likely more efficient than) the following code:
     * <pre>{@code
    MemoryAddress.NULL.asSegment(Long.MAX_VALUE)
     * }</pre>
     * <p>
     * This method is <a href="package-summary.html#restricted"><em>restricted</em></a>.
     * Restricted methods are unsafe, and, if used incorrectly, their use might crash
     * the JVM or, worse, silently result in memory corruption. Thus, clients should refrain from depending on
     * restricted methods, and use safe and supported functionalities, where possible.
     *
     * @return a memory segment whose base address is {@link MemoryAddress#NULL} and whose size is {@link Long#MAX_VALUE}.
     * @throws IllegalCallerException if access to this method occurs from a module {@code M} and the command line option
     * {@code --enable-native-access} is either absent, or does not mention the module name {@code M}, or
     * {@code ALL-UNNAMED} in case {@code M} is an unnamed module.
     */
    @CallerSensitive
    static MemorySegment globalNativeSegment() {
        Reflection.ensureNativeAccess(Reflection.getCallerClass());
        return NativeMemorySegmentImpl.EVERYTHING;
    }
}
