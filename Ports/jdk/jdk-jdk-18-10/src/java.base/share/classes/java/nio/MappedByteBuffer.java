/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.nio;

import java.io.FileDescriptor;
import java.io.UncheckedIOException;
import java.lang.ref.Reference;
import java.util.Objects;

import jdk.internal.access.foreign.MemorySegmentProxy;
import jdk.internal.access.foreign.UnmapperProxy;
import jdk.internal.misc.ScopedMemoryAccess;
import jdk.internal.misc.Unsafe;


/**
 * A direct byte buffer whose content is a memory-mapped region of a file.
 *
 * <p> Mapped byte buffers are created via the {@link
 * java.nio.channels.FileChannel#map FileChannel.map} method.  This class
 * extends the {@link ByteBuffer} class with operations that are specific to
 * memory-mapped file regions.
 *
 * <p> A mapped byte buffer and the file mapping that it represents remain
 * valid until the buffer itself is garbage-collected.
 *
 * <p> The content of a mapped byte buffer can change at any time, for example
 * if the content of the corresponding region of the mapped file is changed by
 * this program or another.  Whether or not such changes occur, and when they
 * occur, is operating-system dependent and therefore unspecified.
 *
 * <a id="inaccess"></a><p> All or part of a mapped byte buffer may become
 * inaccessible at any time, for example if the mapped file is truncated.  An
 * attempt to access an inaccessible region of a mapped byte buffer will not
 * change the buffer's content and will cause an unspecified exception to be
 * thrown either at the time of the access or at some later time.  It is
 * therefore strongly recommended that appropriate precautions be taken to
 * avoid the manipulation of a mapped file by this program, or by a
 * concurrently running program, except to read or write the file's content.
 *
 * <p> Mapped byte buffers otherwise behave no differently than ordinary direct
 * byte buffers. </p>
 *
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */

public abstract class MappedByteBuffer
    extends ByteBuffer
{

    // This is a little bit backwards: By rights MappedByteBuffer should be a
    // subclass of DirectByteBuffer, but to keep the spec clear and simple, and
    // for optimization purposes, it's easier to do it the other way around.
    // This works because DirectByteBuffer is a package-private class.

    // For mapped buffers, a FileDescriptor that may be used for mapping
    // operations if valid; null if the buffer is not mapped.
    private final FileDescriptor fd;

    // A flag true if this buffer is mapped against non-volatile
    // memory using one of the extended FileChannel.MapMode modes,
    // MapMode.READ_ONLY_SYNC or MapMode.READ_WRITE_SYNC and false if
    // it is mapped using any of the other modes. This flag only
    // determines the behavior of force operations.
    private final boolean isSync;

    static final ScopedMemoryAccess SCOPED_MEMORY_ACCESS = ScopedMemoryAccess.getScopedMemoryAccess();

    // This should only be invoked by the DirectByteBuffer constructors
    //
    MappedByteBuffer(int mark, int pos, int lim, int cap, // package-private
                     FileDescriptor fd, boolean isSync, MemorySegmentProxy segment) {
        super(mark, pos, lim, cap, segment);
        this.fd = fd;
        this.isSync = isSync;
    }

    MappedByteBuffer(int mark, int pos, int lim, int cap, // package-private
                     boolean isSync, MemorySegmentProxy segment) {
        super(mark, pos, lim, cap, segment);
        this.fd = null;
        this.isSync = isSync;
    }

    MappedByteBuffer(int mark, int pos, int lim, int cap, MemorySegmentProxy segment) { // package-private
        super(mark, pos, lim, cap, segment);
        this.fd = null;
        this.isSync = false;
    }

    UnmapperProxy unmapper() {
        return fd != null ?
                new UnmapperProxy() {
                    @Override
                    public long address() {
                        return address;
                    }

                    @Override
                    public FileDescriptor fileDescriptor() {
                        return fd;
                    }

                    @Override
                    public boolean isSync() {
                        return isSync;
                    }

                    @Override
                    public void unmap() {
                        Unsafe.getUnsafe().invokeCleaner(MappedByteBuffer.this);
                    }
                } : null;
    }

    /**
     * Tells whether this buffer was mapped against a non-volatile
     * memory device by passing one of the sync map modes {@link
     * jdk.nio.mapmode.ExtendedMapMode#READ_ONLY_SYNC
     * ExtendedMapModeMapMode#READ_ONLY_SYNC} or {@link
     * jdk.nio.mapmode.ExtendedMapMode#READ_ONLY_SYNC
     * ExtendedMapMode#READ_WRITE_SYNC} in the call to {@link
     * java.nio.channels.FileChannel#map FileChannel.map} or was
     * mapped by passing one of the other map modes.
     *
     * @return true if the file was mapped using one of the sync map
     * modes, otherwise false.
     */
    final boolean isSync() { // package-private
        return isSync;
    }

    /**
     * Returns the {@code FileDescriptor} associated with this
     * {@code MappedByteBuffer}.
     *
     * @return the buffer's file descriptor; may be {@code null}
     */
    final FileDescriptor fileDescriptor() { // package-private
        return fd;
    }

    /**
     * Tells whether or not this buffer's content is resident in physical
     * memory.
     *
     * <p> A return value of {@code true} implies that it is highly likely
     * that all of the data in this buffer is resident in physical memory and
     * may therefore be accessed without incurring any virtual-memory page
     * faults or I/O operations.  A return value of {@code false} does not
     * necessarily imply that the buffer's content is not resident in physical
     * memory.
     *
     * <p> The returned value is a hint, rather than a guarantee, because the
     * underlying operating system may have paged out some of the buffer's data
     * by the time that an invocation of this method returns.  </p>
     *
     * @return  {@code true} if it is likely that this buffer's content
     *          is resident in physical memory
     */
    public final boolean isLoaded() {
        if (fd == null) {
            return true;
        }
        return SCOPED_MEMORY_ACCESS.isLoaded(scope(), address, isSync, capacity());
    }

    /**
     * Loads this buffer's content into physical memory.
     *
     * <p> This method makes a best effort to ensure that, when it returns,
     * this buffer's content is resident in physical memory.  Invoking this
     * method may cause some number of page faults and I/O operations to
     * occur. </p>
     *
     * @return  This buffer
     */
    public final MappedByteBuffer load() {
        if (fd == null) {
            return this;
        }
        try {
            SCOPED_MEMORY_ACCESS.load(scope(), address, isSync, capacity());
        } finally {
            Reference.reachabilityFence(this);
        }
        return this;
    }

    /**
     * Forces any changes made to this buffer's content to be written to the
     * storage device containing the mapped file.  The region starts at index
     * zero in this buffer and is {@code capacity()} bytes.  An invocation of
     * this method behaves in exactly the same way as the invocation
     * {@link force(int,int) force(0,capacity())}.
     *
     * <p> If the file mapped into this buffer resides on a local storage
     * device then when this method returns it is guaranteed that all changes
     * made to the buffer since it was created, or since this method was last
     * invoked, will have been written to that device.
     *
     * <p> If the file does not reside on a local device then no such guarantee
     * is made.
     *
     * <p> If this buffer was not mapped in read/write mode ({@link
     * java.nio.channels.FileChannel.MapMode#READ_WRITE}) then
     * invoking this method may have no effect. In particular, the
     * method has no effect for buffers mapped in read-only or private
     * mapping modes. This method may or may not have an effect for
     * implementation-specific mapping modes. </p>
     *
     * @throws UncheckedIOException
     *         If an I/O error occurs writing the buffer's content to the
     *         storage device containing the mapped file
     *
     * @return  This buffer
     */
    public final MappedByteBuffer force() {
        if (fd == null) {
            return this;
        }
        int capacity = capacity();
        if (isSync || ((address != 0) && (capacity != 0))) {
            return force(0, capacity);
        }
        return this;
    }

    /**
     * Forces any changes made to a region of this buffer's content to
     * be written to the storage device containing the mapped
     * file. The region starts at the given {@code index} in this
     * buffer and is {@code length} bytes.
     *
     * <p> If the file mapped into this buffer resides on a local
     * storage device then when this method returns it is guaranteed
     * that all changes made to the selected region buffer since it
     * was created, or since this method was last invoked, will have
     * been written to that device. The force operation is free to
     * write bytes that lie outside the specified region, for example
     * to ensure that data blocks of some device-specific granularity
     * are transferred in their entirety.
     *
     * <p> If the file does not reside on a local device then no such
     * guarantee is made.
     *
     * <p> If this buffer was not mapped in read/write mode ({@link
     * java.nio.channels.FileChannel.MapMode#READ_WRITE}) then
     * invoking this method may have no effect. In particular, the
     * method has no effect for buffers mapped in read-only or private
     * mapping modes. This method may or may not have an effect for
     * implementation-specific mapping modes. </p>
     *
     * @param  index
     *         The index of the first byte in the buffer region that is
     *         to be written back to storage; must be non-negative
     *         and less than {@code capacity()}
     *
     * @param  length
     *         The length of the region in bytes; must be non-negative
     *         and no larger than {@code capacity() - index}
     *
     * @throws IndexOutOfBoundsException
     *         if the preconditions on the index and length do not
     *         hold.
     *
     * @throws UncheckedIOException
     *         If an I/O error occurs writing the buffer's content to the
     *         storage device containing the mapped file
     *
     * @return  This buffer
     *
     * @since 13
     */
    public final MappedByteBuffer force(int index, int length) {
        if (fd == null) {
            return this;
        }
        int capacity = capacity();
        if ((address != 0) && (capacity != 0)) {
            // check inputs
            Objects.checkFromIndexSize(index, length, capacity);
            SCOPED_MEMORY_ACCESS.force(scope(), fd, address, isSync, index, length);
        }
        return this;
    }

    // -- Covariant return type overrides

    /**
     * {@inheritDoc}
     */
    @Override
    public final MappedByteBuffer position(int newPosition) {
        super.position(newPosition);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final MappedByteBuffer limit(int newLimit) {
        super.limit(newLimit);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final MappedByteBuffer mark() {
        super.mark();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final MappedByteBuffer reset() {
        super.reset();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final MappedByteBuffer clear() {
        super.clear();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final MappedByteBuffer flip() {
        super.flip();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final MappedByteBuffer rewind() {
        super.rewind();
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * <p> Reading bytes into physical memory by invoking {@code load()} on the
     * returned buffer, or writing bytes to the storage device by invoking
     * {@code force()} on the returned buffer, will only act on the sub-range
     * of this buffer that the returned buffer represents, namely
     * {@code [position(),limit())}.
     */
    @Override
    public abstract MappedByteBuffer slice();

    /**
     * {@inheritDoc}
     *
     * <p> Reading bytes into physical memory by invoking {@code load()} on the
     * returned buffer, or writing bytes to the storage device by invoking
     * {@code force()} on the returned buffer, will only act on the sub-range
     * of this buffer that the returned buffer represents, namely
     * {@code [index,index+length)}, where {@code index} and {@code length} are
     * assumed to satisfy the preconditions.
     */
    @Override
    public abstract MappedByteBuffer slice(int index, int length);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract MappedByteBuffer duplicate();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract MappedByteBuffer compact();
}
