/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.access;

import jdk.internal.access.foreign.MemorySegmentProxy;
import jdk.internal.access.foreign.UnmapperProxy;
import jdk.internal.misc.ScopedMemoryAccess.Scope;
import jdk.internal.misc.VM.BufferPool;

import java.io.FileDescriptor;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public interface JavaNioAccess {

    /**
     * Used by {@code jdk.internal.misc.VM}.
     */
    BufferPool getDirectBufferPool();

    /**
     * Constructs a direct ByteBuffer referring to the block of memory starting
     * at the given memory address and extending {@code cap} bytes.
     * The {@code ob} parameter is an arbitrary object that is attached
     * to the resulting buffer.
     * Used by {@code jdk.internal.foreignMemorySegmentImpl}.
     */
    ByteBuffer newDirectByteBuffer(long addr, int cap, Object obj, MemorySegmentProxy segment);

    /**
     * Constructs a mapped ByteBuffer referring to the block of memory starting
     * at the given memory address and extending {@code cap} bytes.
     * The {@code ob} parameter is an arbitrary object that is attached
     * to the resulting buffer. The {@code sync} and {@code fd} parameters of the mapped
     * buffer are derived from the {@code UnmapperProxy}.
     * Used by {@code jdk.internal.foreignMemorySegmentImpl}.
     */
    ByteBuffer newMappedByteBuffer(UnmapperProxy unmapperProxy, long addr, int cap, Object obj, MemorySegmentProxy segment);

    /**
     * Constructs an heap ByteBuffer with given backing array, offset, capacity and segment.
     * Used by {@code jdk.internal.foreignMemorySegmentImpl}.
     */
    ByteBuffer newHeapByteBuffer(byte[] hb, int offset, int capacity, MemorySegmentProxy segment);

    /**
     * Used by {@code jdk.internal.foreign.Utils}.
     */
    Object getBufferBase(ByteBuffer bb);

    /**
     * Used by {@code jdk.internal.foreign.Utils}.
     */
    long getBufferAddress(ByteBuffer bb);

    /**
     * Used by {@code jdk.internal.foreign.Utils}.
     */
    UnmapperProxy unmapper(ByteBuffer bb);

    /**
     * Used by {@code jdk.internal.foreign.AbstractMemorySegmentImpl} and byte buffer var handle views.
     */
    MemorySegmentProxy bufferSegment(Buffer buffer);

    /**
     * Used by I/O operations to make a buffer's resource scope non-closeable
     * (for the duration of the I/O operation) by acquiring a new resource
     * scope handle. Null is returned if the buffer has no scope, or
     * acquiring is not required to guarantee safety.
     */
    Scope.Handle acquireScope(Buffer buffer, boolean async);

    /**
     * Used by {@code jdk.internal.foreign.MappedMemorySegmentImpl} and byte buffer var handle views.
     */
    void force(FileDescriptor fd, long address, boolean isSync, long offset, long size);

    /**
     * Used by {@code jdk.internal.foreign.MappedMemorySegmentImpl} and byte buffer var handle views.
     */
    void load(long address, boolean isSync, long size);

    /**
     * Used by {@code jdk.internal.foreign.MappedMemorySegmentImpl}.
     */
    void unload(long address, boolean isSync, long size);

    /**
     * Used by {@code jdk.internal.foreign.MappedMemorySegmentImpl} and byte buffer var handle views.
     */
    boolean isLoaded(long address, boolean isSync, long size);

    /**
     * Used by {@code jdk.internal.foreign.NativeMemorySegmentImpl}.
     */
    void reserveMemory(long size, long cap);

    /**
     * Used by {@code jdk.internal.foreign.NativeMemorySegmentImpl}.
     */
    void unreserveMemory(long size, long cap);

    /**
     * Used by {@code jdk.internal.foreign.NativeMemorySegmentImpl}.
     */
    int pageSize();
}
