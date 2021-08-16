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
package jdk.internal.foreign;

import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;

import jdk.incubator.foreign.ResourceScope;
import java.util.Objects;

/**
 * This class provides an immutable implementation for the {@code MemoryAddress} interface. This class contains information
 * about the segment this address is associated with, as well as an offset into such segment.
 */
public final class MemoryAddressImpl implements MemoryAddress {

    private final AbstractMemorySegmentImpl segment;
    private final long offset;

    public MemoryAddressImpl(AbstractMemorySegmentImpl segment, long offset) {
        this.segment = segment;
        this.offset = offset;
    }

    Object base() {
        return segment != null ? segment.base() : null;
    }

    long offset() {
        return segment != null ?
                segment.min() + offset : offset;
    }

    // MemoryAddress methods

    @Override
    public ResourceScope scope() {
        return segment != null ?
                segment.scope() : ResourceScope.globalScope();
    }

    @Override
    public MemoryAddress addOffset(long offset) {
        return new MemoryAddressImpl(segment, this.offset + offset);
    }

    @Override
    public long segmentOffset(MemorySegment segment) {
        Objects.requireNonNull(segment);
        AbstractMemorySegmentImpl segmentImpl = (AbstractMemorySegmentImpl)segment;
        if (segmentImpl.base() != base()) {
            throw new IllegalArgumentException("Incompatible segment: " + segment);
        }
        return offset() - segmentImpl.min();
    }

    @Override
    public boolean isNative() {
        return base() == null;
    }

    @Override
    public long toRawLongValue() {
        if (segment != null) {
            if (segment.base() != null) {
                throw new UnsupportedOperationException("Not a native address");
            }
            segment.checkValidState();
        }
        return offset();
    }

    // Object methods

    @Override
    public int hashCode() {
        return Objects.hash(base(), offset());
    }

    @Override
    public boolean equals(Object that) {
        if (that instanceof MemoryAddressImpl) {
            MemoryAddressImpl addr = (MemoryAddressImpl)that;
            return Objects.equals(base(), addr.base()) &&
                    offset() == addr.offset();
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "MemoryAddress{ base: " + base() + " offset=0x" + Long.toHexString(offset()) + " }";
    }

    @Override
    @CallerSensitive
    public final MemorySegment asSegment(long bytesSize, ResourceScope scope) {
        Reflection.ensureNativeAccess(Reflection.getCallerClass());
        return asSegment(bytesSize, null, scope);
    }

    @Override
    @CallerSensitive
    public final MemorySegment asSegment(long bytesSize, Runnable cleanupAction, ResourceScope scope) {
        Reflection.ensureNativeAccess(Reflection.getCallerClass());
        Objects.requireNonNull(scope);
        if (bytesSize <= 0) {
            throw new IllegalArgumentException("Invalid size : " + bytesSize);
        }
        return NativeMemorySegmentImpl.makeNativeSegmentUnchecked(this, bytesSize,
                cleanupAction,
                (ResourceScopeImpl) scope);
    }

    public static MemorySegment ofLongUnchecked(long value) {
        return ofLongUnchecked(value, Long.MAX_VALUE);
    }

    public static MemorySegment ofLongUnchecked(long value, long byteSize, ResourceScopeImpl resourceScope) {
        return NativeMemorySegmentImpl.makeNativeSegmentUnchecked(MemoryAddress.ofLong(value), byteSize, null, resourceScope);
    }

    public static MemorySegment ofLongUnchecked(long value, long byteSize) {
        return NativeMemorySegmentImpl.makeNativeSegmentUnchecked(MemoryAddress.ofLong(value), byteSize, null, ResourceScopeImpl.GLOBAL);
    }
}
