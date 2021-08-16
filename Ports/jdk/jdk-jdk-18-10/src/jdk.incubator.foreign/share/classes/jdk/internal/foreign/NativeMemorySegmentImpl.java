/*
 *  Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.foreign;

import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;
import jdk.internal.misc.Unsafe;
import jdk.internal.misc.VM;
import jdk.internal.vm.annotation.ForceInline;
import sun.security.action.GetBooleanAction;

import java.nio.ByteBuffer;

/**
 * Implementation for native memory segments. A native memory segment is essentially a wrapper around
 * a native long address.
 */
public class NativeMemorySegmentImpl extends AbstractMemorySegmentImpl {

    public static final MemorySegment EVERYTHING = makeNativeSegmentUnchecked(MemoryAddress.NULL, Long.MAX_VALUE, null, ResourceScopeImpl.GLOBAL);

    private static final Unsafe unsafe = Unsafe.getUnsafe();

    public static final SegmentAllocator IMPLICIT_ALLOCATOR = (size, align) -> MemorySegment.allocateNative(size, align, ResourceScope.newImplicitScope());

    // The maximum alignment supported by malloc - typically 16 on
    // 64-bit platforms and 8 on 32-bit platforms.
    private static final long MAX_MALLOC_ALIGN = Unsafe.ADDRESS_SIZE == 4 ? 8 : 16;

    private static final boolean skipZeroMemory = GetBooleanAction.privilegedGetProperty("jdk.internal.foreign.skipZeroMemory");

    final long min;

    @ForceInline
    NativeMemorySegmentImpl(long min, long length, int mask, ResourceScopeImpl scope) {
        super(length, mask, scope);
        this.min = min;
    }

    @Override
    NativeMemorySegmentImpl dup(long offset, long size, int mask, ResourceScopeImpl scope) {
        return new NativeMemorySegmentImpl(min + offset, size, mask, scope);
    }

    @Override
    ByteBuffer makeByteBuffer() {
        return nioAccess.newDirectByteBuffer(min(), (int) this.length, null,
                scope == ResourceScopeImpl.GLOBAL ? null : this);
    }

    @Override
    public boolean isNative() {
        return true;
    }

    @Override
    long min() {
        return min;
    }

    @Override
    Object base() {
        return null;
    }

    // factories

    public static MemorySegment makeNativeSegment(long bytesSize, long alignmentBytes, ResourceScopeImpl scope) {
        scope.checkValidStateSlow();
        if (VM.isDirectMemoryPageAligned()) {
            alignmentBytes = Math.max(alignmentBytes, nioAccess.pageSize());
        }
        long alignedSize = alignmentBytes > MAX_MALLOC_ALIGN ?
                bytesSize + (alignmentBytes - 1) :
                bytesSize;

        nioAccess.reserveMemory(alignedSize, bytesSize);

        long buf = unsafe.allocateMemory(alignedSize);
        if (!skipZeroMemory) {
            unsafe.setMemory(buf, alignedSize, (byte)0);
        }
        long alignedBuf = Utils.alignUp(buf, alignmentBytes);
        AbstractMemorySegmentImpl segment = new NativeMemorySegmentImpl(buf, alignedSize,
                defaultAccessModes(alignedSize), scope);
        scope.addOrCleanupIfFail(new ResourceScopeImpl.ResourceList.ResourceCleanup() {
            @Override
            public void cleanup() {
                unsafe.freeMemory(buf);
                nioAccess.unreserveMemory(alignedSize, bytesSize);
            }
        });
        if (alignedSize != bytesSize) {
            long delta = alignedBuf - buf;
            segment = segment.asSlice(delta, bytesSize);
        }
        return segment;
    }

    public static MemorySegment makeNativeSegmentUnchecked(MemoryAddress min, long bytesSize, Runnable cleanupAction, ResourceScopeImpl scope) {
        scope.checkValidStateSlow();
        AbstractMemorySegmentImpl segment = new NativeMemorySegmentImpl(min.toRawLongValue(), bytesSize, defaultAccessModes(bytesSize), scope);
        if (cleanupAction != null) {
            scope.addCloseAction(cleanupAction);
        }
        return segment;
    }
}
