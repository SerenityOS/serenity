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

import jdk.incubator.foreign.MemorySegment;
import jdk.internal.access.JavaNioAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.ForceInline;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Implementation for heap memory segments. An heap memory segment is composed by an offset and
 * a base object (typically an array). To enhance performances, the access to the base object needs to feature
 * sharp type information, as well as sharp null-check information. For this reason, many concrete subclasses
 * of {@link HeapMemorySegmentImpl} are defined (e.g. {@link OfFloat}, so that each subclass can override the
 * {@link HeapMemorySegmentImpl#base()} method so that it returns an array of the correct (sharp) type.
 */
public abstract class HeapMemorySegmentImpl<H> extends AbstractMemorySegmentImpl {

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final int BYTE_ARR_BASE = UNSAFE.arrayBaseOffset(byte[].class);

    final long offset;
    final H base;

    @ForceInline
    HeapMemorySegmentImpl(long offset, H base, long length, int mask) {
        super(length, mask, ResourceScopeImpl.GLOBAL);
        this.offset = offset;
        this.base = base;
    }

    @Override
    abstract H base();

    @Override
    long min() {
        return offset;
    }

    @Override
    abstract HeapMemorySegmentImpl<H> dup(long offset, long size, int mask, ResourceScopeImpl scope);

    @Override
    ByteBuffer makeByteBuffer() {
        if (!(base() instanceof byte[])) {
            throw new UnsupportedOperationException("Not an address to an heap-allocated byte array");
        }
        JavaNioAccess nioAccess = SharedSecrets.getJavaNioAccess();
        return nioAccess.newHeapByteBuffer((byte[]) base(), (int)min() - BYTE_ARR_BASE, (int) byteSize(), null);
    }

    // factories

    public static class OfByte extends HeapMemorySegmentImpl<byte[]> {

        OfByte(long offset, byte[] base, long length, int mask) {
            super(offset, base, length, mask);
        }

        @Override
        OfByte dup(long offset, long size, int mask, ResourceScopeImpl scope) {
            return new OfByte(this.offset + offset, base, size, mask);
        }

        @Override
        byte[] base() {
            return Objects.requireNonNull(base);
        }

        public static MemorySegment fromArray(byte[] arr) {
            Objects.requireNonNull(arr);
            long byteSize = (long)arr.length * Unsafe.ARRAY_BYTE_INDEX_SCALE;
            return new OfByte(Unsafe.ARRAY_BYTE_BASE_OFFSET, arr, byteSize, defaultAccessModes(byteSize));
        }
    }

    public static class OfChar extends HeapMemorySegmentImpl<char[]> {

        OfChar(long offset, char[] base, long length, int mask) {
            super(offset, base, length, mask);
        }

        @Override
        OfChar dup(long offset, long size, int mask, ResourceScopeImpl scope) {
            return new OfChar(this.offset + offset, base, size, mask);
        }

        @Override
        char[] base() {
            return Objects.requireNonNull(base);
        }

        public static MemorySegment fromArray(char[] arr) {
            Objects.requireNonNull(arr);
            long byteSize = (long)arr.length * Unsafe.ARRAY_CHAR_INDEX_SCALE;
            return new OfChar(Unsafe.ARRAY_CHAR_BASE_OFFSET, arr, byteSize, defaultAccessModes(byteSize));
        }
    }

    public static class OfShort extends HeapMemorySegmentImpl<short[]> {

        OfShort(long offset, short[] base, long length, int mask) {
            super(offset, base, length, mask);
        }

        @Override
        OfShort dup(long offset, long size, int mask, ResourceScopeImpl scope) {
            return new OfShort(this.offset + offset, base, size, mask);
        }

        @Override
        short[] base() {
            return Objects.requireNonNull(base);
        }

        public static MemorySegment fromArray(short[] arr) {
            Objects.requireNonNull(arr);
            long byteSize = (long)arr.length * Unsafe.ARRAY_SHORT_INDEX_SCALE;
            return new OfShort(Unsafe.ARRAY_SHORT_BASE_OFFSET, arr, byteSize, defaultAccessModes(byteSize));
        }
    }

    public static class OfInt extends HeapMemorySegmentImpl<int[]> {

        OfInt(long offset, int[] base, long length, int mask) {
            super(offset, base, length, mask);
        }

        @Override
        OfInt dup(long offset, long size, int mask, ResourceScopeImpl scope) {
            return new OfInt(this.offset + offset, base, size, mask);
        }

        @Override
        int[] base() {
            return Objects.requireNonNull(base);
        }

        public static MemorySegment fromArray(int[] arr) {
            Objects.requireNonNull(arr);
            long byteSize = (long)arr.length * Unsafe.ARRAY_INT_INDEX_SCALE;
            return new OfInt(Unsafe.ARRAY_INT_BASE_OFFSET, arr, byteSize, defaultAccessModes(byteSize));
        }
    }

    public static class OfLong extends HeapMemorySegmentImpl<long[]> {

        OfLong(long offset, long[] base, long length, int mask) {
            super(offset, base, length, mask);
        }

        @Override
        OfLong dup(long offset, long size, int mask, ResourceScopeImpl scope) {
            return new OfLong(this.offset + offset, base, size, mask);
        }

        @Override
        long[] base() {
            return Objects.requireNonNull(base);
        }

        public static MemorySegment fromArray(long[] arr) {
            Objects.requireNonNull(arr);
            long byteSize = (long)arr.length * Unsafe.ARRAY_LONG_INDEX_SCALE;
            return new OfLong(Unsafe.ARRAY_LONG_BASE_OFFSET, arr, byteSize, defaultAccessModes(byteSize));
        }
    }

    public static class OfFloat extends HeapMemorySegmentImpl<float[]> {

        OfFloat(long offset, float[] base, long length, int mask) {
            super(offset, base, length, mask);
        }

        @Override
        OfFloat dup(long offset, long size, int mask, ResourceScopeImpl scope) {
            return new OfFloat(this.offset + offset, base, size, mask);
        }

        @Override
        float[] base() {
            return Objects.requireNonNull(base);
        }

        public static MemorySegment fromArray(float[] arr) {
            Objects.requireNonNull(arr);
            long byteSize = (long)arr.length * Unsafe.ARRAY_FLOAT_INDEX_SCALE;
            return new OfFloat(Unsafe.ARRAY_FLOAT_BASE_OFFSET, arr, byteSize, defaultAccessModes(byteSize));
        }
    }

    public static class OfDouble extends HeapMemorySegmentImpl<double[]> {

        OfDouble(long offset, double[] base, long length, int mask) {
            super(offset, base, length, mask);
        }

        @Override
        OfDouble dup(long offset, long size, int mask, ResourceScopeImpl scope) {
            return new OfDouble(this.offset + offset, base, size, mask);
        }

        @Override
        double[] base() {
            return Objects.requireNonNull(base);
        }

        public static MemorySegment fromArray(double[] arr) {
            Objects.requireNonNull(arr);
            long byteSize = (long)arr.length * Unsafe.ARRAY_DOUBLE_INDEX_SCALE;
            return new OfDouble(Unsafe.ARRAY_DOUBLE_BASE_OFFSET, arr, byteSize, defaultAccessModes(byteSize));
        }
    }

}
