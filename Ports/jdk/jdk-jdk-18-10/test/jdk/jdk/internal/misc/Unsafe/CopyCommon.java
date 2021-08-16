/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import jdk.internal.misc.Unsafe;

/**
 * Helper class to support testing of Unsafe.copyMemory and Unsafe.copySwapMemory
 */
public class CopyCommon {
    private static final boolean DEBUG = Boolean.getBoolean("CopyCommon.DEBUG");

    public static final long KB = 1024;
    public static final long MB = KB * 1024;
    public static final long GB = MB * 1024;

    static final int SMALL_COPY_SIZE = 32;
    static final int BASE_ALIGNMENT = 16;

    protected static final Unsafe UNSAFE = Unsafe.getUnsafe();

    static long alignDown(long value, long alignment) {
        return value & ~(alignment - 1);
    }

    static long alignUp(long value, long alignment) {
        return (value + alignment - 1) & ~(alignment - 1);
    }

    static boolean isAligned(long value, long alignment) {
        return value == alignDown(value, alignment);
    }

    CopyCommon() {
    }

    /**
     * Generate verification data for a given offset
     *
     * The verification data is used to verify that the correct bytes
     * have indeed been copied and byte swapped.
     *
     * The data is generated based on the offset (in bytes) into the
     * source buffer. For a native buffer the offset is relative to
     * the base pointer. For a heap array it is relative to the
     * address of the first array element.
     *
     * This method will return the result of doing an elementSize byte
     * read starting at offset (in bytes).
     *
     * @param offset offset into buffer
     * @param elemSize size (in bytes) of the element
     *
     * @return the verification data, only the least significant
     * elemSize*8 bits are set, zero extended
     */
    private long getVerificationDataForOffset(long offset, long elemSize) {
        byte[] bytes = new byte[(int)elemSize];

        for (long i = 0; i < elemSize; i++) {
            bytes[(int)i] = (byte)(offset + i);
        }

        long o = UNSAFE.arrayBaseOffset(byte[].class);

        switch ((int)elemSize) {
        case 1: return Byte.toUnsignedLong(UNSAFE.getByte(bytes, o));
        case 2: return Short.toUnsignedLong(UNSAFE.getShortUnaligned(bytes, o));
        case 4: return Integer.toUnsignedLong(UNSAFE.getIntUnaligned(bytes, o));
        case 8: return UNSAFE.getLongUnaligned(bytes, o);
        default: throw new IllegalArgumentException("Invalid element size: " + elemSize);
        }
    }

    /**
     * Verify byte swapped data
     *
     * @param ptr the data to verify
     * @param srcOffset the srcOffset (in bytes) from which the copy started,
     *        used as key to regenerate the verification data
     * @param dstOffset the offset (in bytes) in the array at which to start
     *        the verification, relative to the first element in the array
     * @param size size (in bytes) of data to to verify
     * @param elemSize size (in bytes) of the individual array elements
     *
     * @throws RuntimeException if an error is found
     */
    private void verifySwappedData(GenericPointer ptr, long srcOffset, long dstOffset, long size, long elemSize) {
        for (long offset = 0; offset < size; offset += elemSize) {
            long expectedUnswapped = getVerificationDataForOffset(srcOffset + offset, elemSize);
            long expected = byteSwap(expectedUnswapped, elemSize);

            long actual = getArrayElem(ptr, dstOffset + offset, elemSize);

            if (expected != actual) {
                throw new RuntimeException("srcOffset: 0x" + Long.toHexString(srcOffset) +
                                           " dstOffset: 0x" + Long.toHexString(dstOffset) +
                                           " size: 0x" + Long.toHexString(size) +
                                           " offset: 0x" + Long.toHexString(offset) +
                                           " expectedUnswapped: 0x" + Long.toHexString(expectedUnswapped) +
                                           " expected: 0x" + Long.toHexString(expected) +
                                           " != actual: 0x" + Long.toHexString(actual));
            }
        }
    }

    /**
     * Initialize an array with verification friendly data
     *
     * @param ptr pointer to the data to initialize
     * @param size size (in bytes) of the data
     * @param elemSize size (in bytes) of the individual elements
     */
    private void initVerificationData(GenericPointer ptr, long size, long elemSize) {
        for (long offset = 0; offset < size; offset++) {
            byte data = (byte)getVerificationDataForOffset(offset, 1);

            if (ptr.isOnHeap()) {
                UNSAFE.putByte(ptr.getObject(), ptr.getOffset() + offset, data);
            } else {
                UNSAFE.putByte(ptr.getOffset() + offset, data);
            }
        }
    }

    /**
     * Allocate a primitive array
     *
     * @param size size (in bytes) of all the array elements (elemSize * length)
     * @param elemSize the size of the array elements
     *
     * @return a newly allocated primitive array
     */
    Object allocArray(long size, long elemSize) {
        int length = (int)(size / elemSize);

        switch ((int)elemSize) {
        case 1: return new byte[length];
        case 2: return new short[length];
        case 4: return new int[length];
        case 8: return new long[length];
        default:
            throw new IllegalArgumentException("Invalid element size: " + elemSize);
        }
    }

    /**
     * Get the value of a primitive array entry
     *
     * @param ptr pointer to the data
     * @param offset offset (in bytes) of the array element, relative to the first element in the array
     *
     * @return the array element, as an unsigned long
     */
    private long getArrayElem(GenericPointer ptr, long offset, long elemSize) {
        if (ptr.isOnHeap()) {
            Object o = ptr.getObject();
            int index = (int)(offset / elemSize);

            if (o instanceof short[]) {
                short[] arr = (short[])o;
                return Short.toUnsignedLong(arr[index]);
            } else if (o instanceof int[]) {
                int[] arr = (int[])o;
                return Integer.toUnsignedLong(arr[index]);
            } else if (o instanceof long[]) {
                long[] arr = (long[])o;
                return arr[index];
            } else {
                throw new IllegalArgumentException("Invalid object type: " + o.getClass().getName());
            }
        } else {
            long addr = ptr.getOffset() + offset;

            switch ((int)elemSize) {
            case 1: return Byte.toUnsignedLong(UNSAFE.getByte(addr));
            case 2: return Short.toUnsignedLong(UNSAFE.getShortUnaligned(null, addr));
            case 4: return Integer.toUnsignedLong(UNSAFE.getIntUnaligned(null, addr));
            case 8: return UNSAFE.getLongUnaligned(null, addr);
            default: throw new IllegalArgumentException("Invalid element size: " + elemSize);
            }
        }
    }

    private void putValue(long addr, long elemSize, long value) {
        switch ((int)elemSize) {
        case 1: UNSAFE.putByte(addr, (byte)value); break;
        case 2: UNSAFE.putShortUnaligned(null, addr, (short)value); break;
        case 4: UNSAFE.putIntUnaligned(null, addr, (int)value); break;
        case 8: UNSAFE.putLongUnaligned(null, addr, value); break;
        default: throw new IllegalArgumentException("Invalid element size: " + elemSize);
        }
    }

    /**
     * Get the size of the elements for an array
     *
     * @param o a primitive heap array
     *
     * @return the size (in bytes) of the individual array elements
     */
    private long getArrayElemSize(Object o) {
        if (o instanceof short[]) {
            return 2;
        } else if (o instanceof int[]) {
            return 4;
        } else if (o instanceof long[]) {
            return 8;
        } else {
            throw new IllegalArgumentException("Invalid object type: " + o.getClass().getName());
        }
    }

    /**
     * Byte swap a value
     *
     * @param value the value to swap, only the bytes*8 least significant bits are used
     * @param size size (in bytes) of the value
     *
     * @return the byte swapped value in the bytes*8 least significant bits
     */
    private long byteSwap(long value, long size) {
        switch ((int)size) {
        case 2: return Short.toUnsignedLong(Short.reverseBytes((short)value));
        case 4: return Integer.toUnsignedLong(Integer.reverseBytes((int)value));
        case 8: return Long.reverseBytes(value);
        default: throw new IllegalArgumentException("Invalid element size: " + size);
        }
    }

    /**
     * Verify data which has *not* been byte swapped
     *
     * @param ptr the data to verify
     * @param startOffset the offset (in bytes) at which to start the verification
     * @param size size (in bytes) of the data to verify
     *
     * @throws RuntimeException if an error is found
     */
    private void verifyUnswappedData(GenericPointer ptr, long startOffset, long srcOffset, long size) {
        for (long i = 0; i < size; i++) {
            byte expected = (byte)getVerificationDataForOffset(srcOffset + i, 1);

            byte actual;
            if (ptr.isOnHeap()) {
                actual = UNSAFE.getByte(ptr.getObject(), ptr.getOffset() + startOffset + i);
            } else {
                actual = UNSAFE.getByte(ptr.getOffset() + startOffset + i);
            }

            if (expected != actual) {
                throw new RuntimeException("startOffset: 0x" + Long.toHexString(startOffset) +
                                           " srcOffset: 0x" + Long.toHexString(srcOffset) +
                                           " size: 0x" + Long.toHexString(size) +
                                           " i: 0x" + Long.toHexString(i) +
                                           " expected: 0x" + Long.toHexString(expected) +
                                           " != actual: 0x" + Long.toHexString(actual));
            }
        }
    }


    /**
     * Copy and byte swap data from the source to the destination
     *
     * This method will pre-populate the whole source and destination
     * buffers with verification friendly data. It will then copy data
     * to fill part of the destination buffer with data from the
     * source, optionally byte swapping the copied elements on the
     * fly. Some space (padding) will be left before and after the
     * data in the destination buffer, which should not be
     * touched/overwritten by the copy call.
     *
     * Note: Both source and destination buffers will be overwritten!
     *
     * @param src source buffer to copy from
     * @param srcOffset the offset (in bytes) in the source buffer, relative to
     *        the first array element, at which to start reading data
     * @param dst destination buffer to copy to
     * @param dstOffset the offset (in bytes) in the destination
     *        buffer, relative to the first array element, at which to
     *        start writing data
     * @param bufSize the size (in bytes) of the src and dst arrays
     * @param copyBytes the size (in bytes) of the copy to perform,
     *        must be a multiple of elemSize
     * @param elemSize the size (in bytes) of the elements
     * @param swap true if elements should be byte swapped
     *
     * @throws RuntimeException if an error is found
     */
    void testCopyGeneric(GenericPointer src, long srcOffset,
                         GenericPointer dst, long dstOffset,
                         long bufSize, long copyBytes, long elemSize, boolean swap) {
        if (swap) {
            if (!isAligned(copyBytes, elemSize)) {
                throw new IllegalArgumentException(
                    "copyBytes (" + copyBytes + ") must be a multiple of elemSize (" + elemSize + ")");
            }
            if (src.isOnHeap() && !isAligned(srcOffset, elemSize)) {
                throw new IllegalArgumentException(
                    "srcOffset (" + srcOffset + ") must be a multiple of elemSize (" + elemSize + ")");
            }
            if (dst.isOnHeap() && !isAligned(dstOffset, elemSize)) {
                throw new IllegalArgumentException(
                    "dstOffset (" + dstOffset + ") must be a multiple of elemSize (" + elemSize + ")");
            }
        }

        if (srcOffset + copyBytes > bufSize) {
            throw new IllegalArgumentException(
                "srcOffset (" + srcOffset + ") + copyBytes (" + copyBytes + ") > bufSize (" + bufSize + ")");
        }
        if (dstOffset + copyBytes > bufSize) {
            throw new IllegalArgumentException(
                "dstOffset (" + dstOffset + ") + copyBytes (" + copyBytes + ") > bufSize (" + bufSize + ")");
        }

        // Initialize the whole source buffer with a verification friendly pattern (no 0x00 bytes)
        initVerificationData(src, bufSize, elemSize);
        if (!src.equals(dst)) {
            initVerificationData(dst, bufSize, elemSize);
        }

        if (DEBUG) {
            System.out.println("===before===");
            for (int offset = 0; offset < bufSize; offset += elemSize) {
                long srcValue = getArrayElem(src, offset, elemSize);
                long dstValue = getArrayElem(dst, offset, elemSize);

                System.out.println("offs=0x" + Long.toHexString(Integer.toUnsignedLong(offset)) +
                                 " src=0x" + Long.toHexString(srcValue) +
                                 " dst=0x" + Long.toHexString(dstValue));
            }
        }

        if (swap) {
            // Copy & swap data into the middle of the destination buffer
            UNSAFE.copySwapMemory(src.getObject(),
                                  src.getOffset() + srcOffset,
                                  dst.getObject(),
                                  dst.getOffset() + dstOffset,
                                  copyBytes,
                                  elemSize);
        } else {
            // Copy & swap data into the middle of the destination buffer
            UNSAFE.copyMemory(src.getObject(),
                              src.getOffset() + srcOffset,
                              dst.getObject(),
                              dst.getOffset() + dstOffset,
                              copyBytes);
        }

        if (DEBUG) {
            System.out.println("===after===");
            for (int offset = 0; offset < bufSize; offset += elemSize) {
                long srcValue = getArrayElem(src, offset, elemSize);
                long dstValue = getArrayElem(dst, offset, elemSize);

                System.out.println("offs=0x" + Long.toHexString(Integer.toUnsignedLong(offset)) +
                                 " src=0x" + Long.toHexString(srcValue) +
                                 " dst=0x" + Long.toHexString(dstValue));
            }
        }

        // Verify the the front padding is unchanged
        verifyUnswappedData(dst, 0, 0, dstOffset);

        if (swap) {
            // Verify swapped data
            verifySwappedData(dst, srcOffset, dstOffset, copyBytes, elemSize);
        } else {
            // Verify copied/unswapped data
            verifyUnswappedData(dst, dstOffset, srcOffset, copyBytes);
        }

        // Verify that the back padding is unchanged
        long frontAndCopyBytes = dstOffset + copyBytes;
        long trailingBytes = bufSize - frontAndCopyBytes;
        verifyUnswappedData(dst, frontAndCopyBytes, frontAndCopyBytes, trailingBytes);
    }

    /**
     * Test various configurations of copying and optionally swapping data
     *
     * @param src the source buffer to copy from
     * @param dst the destination buffer to copy to
     * @param size size (in bytes) of the buffers
     * @param elemSize size (in bytes) of the individual elements
     *
     * @throws RuntimeException if an error is found
     */
    public void testBufferPair(GenericPointer src, GenericPointer dst, long size, long elemSize, boolean swap) {
        // offset in source from which to start reading data
        for (long srcOffset = 0; srcOffset < size; srcOffset += (src.isOnHeap() ? elemSize : 1)) {

            // offset in destination at which to start writing data
            for (int dstOffset = 0; dstOffset < size; dstOffset += (dst.isOnHeap() ? elemSize : 1)) {

                // number of bytes to copy
                long maxCopyBytes = Math.min(size - srcOffset, size - dstOffset);
                for (long copyBytes = 0; copyBytes < maxCopyBytes; copyBytes += elemSize) {
                    try {
                        testCopyGeneric(src, srcOffset, dst, dstOffset, size, copyBytes, elemSize, swap);
                    } catch (RuntimeException e) {
                        // Wrap the exception in another exception to catch the relevant configuration data
                        throw new RuntimeException("testBufferPair: " +
                                                   "src=" + src +
                                                   " dst=" + dst +
                                                   " elemSize=0x" + Long.toHexString(elemSize) +
                                                   " copyBytes=0x" + Long.toHexString(copyBytes) +
                                                   " srcOffset=0x" + Long.toHexString(srcOffset) +
                                                   " dstOffset=0x" + Long.toHexString(dstOffset) +
                                                   " swap=" + swap,
                                                   e);
                    }
                }
            }
        }
    }

    /**
     * Test copying between various permutations of buffers
     *
     * @param buffers buffers to permute (src x dst)
     * @param size size (in bytes) of buffers
     * @param elemSize size (in bytes) of individual elements
     *
     * @throws RuntimeException if an error is found
     */
    public void testPermuteBuffers(GenericPointer[] buffers, long size, long elemSize, boolean swap) {
        System.out.println("testPermuteBuffers(buffers, " + size + ", " + elemSize + ", " + swap + ")");
        for (int srcIndex = 0; srcIndex < buffers.length; srcIndex++) {
            for (int dstIndex = 0; dstIndex < buffers.length; dstIndex++) {
                testBufferPair(buffers[srcIndex], buffers[dstIndex], size, elemSize, swap);
            }
        }
    }

    /**
     * Test copying of a specific element size
     *
     * @param size size (in bytes) of buffers to allocate
     * @param elemSize size (in bytes) of individual elements
     *
     * @throws RuntimeException if an error is found
     */
    private void testElemSize(long size, long elemSize, boolean swap) {
        long buf1Raw = 0;
        long buf2Raw = 0;

        try {
            buf1Raw = UNSAFE.allocateMemory(size + BASE_ALIGNMENT);
            long buf1 = alignUp(buf1Raw, BASE_ALIGNMENT);

            buf2Raw = UNSAFE.allocateMemory(size + BASE_ALIGNMENT);
            long buf2 = alignUp(buf2Raw, BASE_ALIGNMENT);

            GenericPointer[] buffers = {
                new GenericPointer(buf1),
                new GenericPointer(buf2),
                new GenericPointer(allocArray(size, elemSize)),
                new GenericPointer(allocArray(size, elemSize))
            };

            testPermuteBuffers(buffers, size, elemSize, swap);
        } finally {
            if (buf1Raw != 0) {
                UNSAFE.freeMemory(buf1Raw);
            }
            if (buf2Raw != 0) {
                UNSAFE.freeMemory(buf2Raw);
            }
        }
    }

    /**
     * Verify that small copies work
     */
    void testSmallCopy(boolean swap) {
        int smallBufSize = SMALL_COPY_SIZE;
        int minElemSize = swap ? 2 : 1;
        int maxElemSize = swap ? 8 : 1;

        // Test various element types and heap/native combinations
        for (long elemSize = minElemSize; elemSize <= maxElemSize; elemSize <<= 1) {
            testElemSize(smallBufSize, elemSize, swap);
        }
    }


    /**
     * Verify that large copies work
     */
    void testLargeCopy(boolean swap) {
        long size = 2 * GB + 8;
        long bufRaw = 0;

        // Check that a large native copy succeeds
        try {
            try {
                bufRaw = UNSAFE.allocateMemory(size + BASE_ALIGNMENT);
            } catch (OutOfMemoryError e) {
                // Accept failure, skip test
                return;
            }

            long buf = alignUp(bufRaw, BASE_ALIGNMENT);

            if (swap) {
                UNSAFE.copySwapMemory(null, buf, null, buf, size, 8);
            } else {
                UNSAFE.copyMemory(null, buf, null, buf, size);
            }
        } catch (Exception e) {
            throw new RuntimeException("copy of large buffer failed (swap=" + swap + ")");
        } finally {
            if (bufRaw != 0) {
                UNSAFE.freeMemory(bufRaw);
            }
        }
    }

    /**
     * Helper class to represent a "pointer" - either a heap array or
     * a pointer to a native buffer.
     *
     * In the case of a native pointer, the Object is null and the offset is
     * the absolute address of the native buffer.
     *
     * In the case of a heap object, the Object is a primitive array, and
     * the offset will be set to the base offset to the first element, meaning
     * the object and the offset together form a double-register pointer.
     */
    static class GenericPointer {
        private final Object o;
        private final long offset;

        private GenericPointer(Object o, long offset) {
            this.o = o;
            this.offset = offset;
        }

        public String toString() {
            return "GenericPointer(o={" + o + "}, offset=0x" + Long.toHexString(offset) + ")";
        }

        public boolean equals(Object other) {
            if (!(other instanceof GenericPointer)) {
                return false;
            }

            GenericPointer otherp = (GenericPointer)other;

            return o == otherp.o && offset == otherp.offset;
        }

        GenericPointer(Object o) {
            this(o, UNSAFE.arrayBaseOffset(o.getClass()));
        }

        GenericPointer(long offset) {
            this(null, offset);
        }

        public boolean isOnHeap() {
            return o != null;
        }

        public Object getObject() {
            return o;
        }

        public long getOffset() {
            return offset;
        }
    }
}
