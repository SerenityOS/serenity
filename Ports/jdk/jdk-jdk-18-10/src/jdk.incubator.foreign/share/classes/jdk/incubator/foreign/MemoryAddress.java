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

import jdk.internal.foreign.MemoryAddressImpl;
import jdk.internal.ref.CleanerFactory;
import jdk.internal.reflect.CallerSensitive;

import java.lang.ref.Cleaner;

/**
 * A memory address models a reference into a memory location. Memory addresses are typically obtained using the
 * {@link MemorySegment#address()} method, and can refer to either off-heap or on-heap memory. Off-heap memory
 * addresses are referred to as <em>native</em> memory addresses (see {@link #isNative()}). Native memory addresses
 * allow clients to obtain a raw memory address (expressed as a long value) which can then be used e.g. when interacting
 * with native code.
 * <p>
 * Given an address, it is possible to compute its offset relative to a given segment, which can be useful
 * when performing memory dereference operations using a memory access var handle (see {@link MemoryHandles}).
 * <p>
 * A memory address is associated with a {@linkplain ResourceScope resource scope}; the resource scope determines the
 * lifecycle of the memory address, and whether the address can be used from multiple threads. Memory addresses
 * obtained from {@linkplain #ofLong(long) numeric values}, or from native code, are associated with the
 * {@linkplain ResourceScope#globalScope() global resource scope}. Memory addresses obtained from segments
 * are associated with the same scope as the segment from which they have been obtained.
 * <p>
 * All implementations of this interface must be <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>;
 * programmers should treat instances that are {@linkplain #equals(Object) equal} as interchangeable and should not
 * use instances for synchronization, or unpredictable behavior may occur. For example, in a future release,
 * synchronization may fail. The {@code equals} method should be used for comparisons.
 * <p>
 * Non-platform classes should not implement {@linkplain MemoryAddress} directly.
 *
 * <p> Unless otherwise specified, passing a {@code null} argument, or an array argument containing one or more {@code null}
 * elements to a method in this class causes a {@link NullPointerException NullPointerException} to be thrown. </p>
 *
 * @implSpec
 * Implementations of this interface are immutable, thread-safe and <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>.
 */
public sealed interface MemoryAddress extends Addressable permits MemoryAddressImpl {

    @Override
    default MemoryAddress address() {
        return this;
    }

    /**
     * Creates a new memory address with given offset (in bytes), which might be negative, from current one.
     * @param offset specified offset (in bytes), relative to this address, which should be used to create the new address.
     * @return a new memory address with given offset from current one.
     */
    MemoryAddress addOffset(long offset);

    /**
     * Returns the resource scope associated with this memory address.
     * @return the resource scope associated with this memory address.
     */
    ResourceScope scope();

    /**
     * Returns the offset of this memory address into the given segment. More specifically, if both the segment's
     * base address and this address are native addresses, the result is computed as
     * {@code this.toRawLongValue() - segment.address().toRawLongValue()}. Otherwise, if both addresses in the form
     * {@code (B, O1)}, {@code (B, O2)}, where {@code B} is the same base heap object and {@code O1}, {@code O2}
     * are byte offsets (relative to the base object) associated with this address and the segment's base address,
     * the result is computed as {@code O1 - O2}.
     * <p>
     * If the segment's base address and this address are both heap addresses, but with different base objects, the result is undefined
     * and an exception is thrown. Similarly, if the segment's base address is an heap address (resp. off-heap) and
     * this address is an off-heap (resp. heap) address, the result is undefined and an exception is thrown.
     * Otherwise, the result is a byte offset {@code SO}. If this address falls within the
     * spatial bounds of the given segment, then {@code 0 <= SO < segment.byteSize()}; otherwise, {@code SO < 0 || SO > segment.byteSize()}.
     * @return the offset of this memory address into the given segment.
     * @param segment the segment relative to which this address offset should be computed
     * @throws IllegalArgumentException if {@code segment} is not compatible with this address; this can happen, for instance,
     * when {@code segment} models an heap memory region, while this address is a {@linkplain #isNative() native} address.
     */
    long segmentOffset(MemorySegment segment);

    /**
     Returns a new native memory segment with given size and resource scope (replacing the scope already associated
     * with this address), and whose base address is this address. This method can be useful when interacting with custom
     * native memory sources (e.g. custom allocators), where an address to some
     * underlying memory region is typically obtained from native code (often as a plain {@code long} value).
     * The returned segment is not read-only (see {@link MemorySegment#isReadOnly()}), and is associated with the
     * provided resource scope.
     * <p>
     * Clients should ensure that the address and bounds refers to a valid region of memory that is accessible for reading and,
     * if appropriate, writing; an attempt to access an invalid memory location from Java code will either return an arbitrary value,
     * have no visible effect, or cause an unspecified exception to be thrown.
     * <p>
     * This method is equivalent to the following code:
     * <pre>{@code
    asSegment(byteSize, null, scope);
     * }</pre>
     * <p>
     * This method is <a href="package-summary.html#restricted"><em>restricted</em></a>.
     * Restricted methods are unsafe, and, if used incorrectly, their use might crash
     * the JVM or, worse, silently result in memory corruption. Thus, clients should refrain from depending on
     * restricted methods, and use safe and supported functionalities, where possible.
     *
     * @param bytesSize the desired size.
     * @param scope the native segment scope.
     * @return a new native memory segment with given base address, size and scope.
     * @throws IllegalArgumentException if {@code bytesSize <= 0}.
     * @throws IllegalStateException if either the scope associated with this address or the provided scope
     * have been already closed, or if access occurs from a thread other than the thread owning either
     * scopes.
     * @throws UnsupportedOperationException if this address is not a {@linkplain #isNative() native} address.
     * @throws IllegalCallerException if access to this method occurs from a module {@code M} and the command line option
     * {@code --enable-native-access} is either absent, or does not mention the module name {@code M}, or
     * {@code ALL-UNNAMED} in case {@code M} is an unnamed module.
     */
    @CallerSensitive
    MemorySegment asSegment(long bytesSize, ResourceScope scope);

    /**
     * Returns a new native memory segment with given size and resource scope (replacing the scope already associated
     * with this address), and whose base address is this address. This method can be useful when interacting with custom
     * native memory sources (e.g. custom allocators), where an address to some
     * underlying memory region is typically obtained from native code (often as a plain {@code long} value).
     * The returned segment is associated with the provided resource scope.
     * <p>
     * Clients should ensure that the address and bounds refers to a valid region of memory that is accessible for reading and,
     * if appropriate, writing; an attempt to access an invalid memory location from Java code will either return an arbitrary value,
     * have no visible effect, or cause an unspecified exception to be thrown.
     * <p>
     * Calling {@link ResourceScope#close()} on the scope associated with the returned segment will result in calling
     * the provided cleanup action (if any).
     * <p>
     * This method is <a href="package-summary.html#restricted"><em>restricted</em></a>.
     * Restricted methods are unsafe, and, if used incorrectly, their use might crash
     * the JVM or, worse, silently result in memory corruption. Thus, clients should refrain from depending on
     * restricted methods, and use safe and supported functionalities, where possible.
     *
     * @param bytesSize the desired size.
     * @param cleanupAction the cleanup action; can be {@code null}.
     * @param scope the native segment scope.
     * @return a new native memory segment with given base address, size and scope.
     * @throws IllegalArgumentException if {@code bytesSize <= 0}.
     * @throws IllegalStateException if either the scope associated with this address or the provided scope
     * have been already closed, or if access occurs from a thread other than the thread owning either
     * scopes.
     * @throws UnsupportedOperationException if this address is not a {@linkplain #isNative() native} address.
     * @throws IllegalCallerException if access to this method occurs from a module {@code M} and the command line option
     * {@code --enable-native-access} is either absent, or does not mention the module name {@code M}, or
     * {@code ALL-UNNAMED} in case {@code M} is an unnamed module.
     */
    @CallerSensitive
    MemorySegment asSegment(long bytesSize, Runnable cleanupAction, ResourceScope scope);

    /**
     * Is this an off-heap memory address?
     * @return true, if this is an off-heap memory address.
     */
    boolean isNative();

    /**
     * Returns the raw long value associated with this native memory address.
     * @return The raw long value associated with this native memory address.
     * @throws UnsupportedOperationException if this memory address is not a {@linkplain #isNative() native} address.
     * @throws IllegalStateException if the scope associated with this segment has been already closed,
     * or if access occurs from a thread other than the thread owning either segment.
     */
    long toRawLongValue();

    /**
     * Compares the specified object with this address for equality. Returns {@code true} if and only if the specified
     * object is also an address, and it refers to the same memory location as this address.
     *
     * @apiNote two addresses might be considered equal despite their associated resource scopes differ. This
     * can happen, for instance, if the same memory address is used to create memory segments with different
     * scopes (using {@link #asSegment(long, ResourceScope)}), and the base address of the resulting segments is
     * then compared.
     *
     * @param that the object to be compared for equality with this address.
     * @return {@code true} if the specified object is equal to this address.
     */
    @Override
    boolean equals(Object that);

    /**
     * Returns the hash code value for this address.
     * @return the hash code value for this address.
     */
    @Override
    int hashCode();

    /**
     * The native memory address instance modelling the {@code NULL} address, associated
     * with the {@linkplain ResourceScope#globalScope() global} resource scope.
     */
    MemoryAddress NULL = new MemoryAddressImpl(null, 0L);

    /**
     * Obtain a native memory address instance from given long address. The returned address is associated
     * with the {@linkplain ResourceScope#globalScope() global} resource scope.
     * @param value the long address.
     * @return the new memory address instance.
     */
    static MemoryAddress ofLong(long value) {
        return value == 0 ?
                NULL :
                new MemoryAddressImpl(null, value);
    }
}
