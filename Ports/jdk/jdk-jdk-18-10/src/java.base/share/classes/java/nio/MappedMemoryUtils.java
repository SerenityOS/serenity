/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.misc.Unsafe;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.UncheckedIOException;

/* package */ class MappedMemoryUtils {

    static boolean isLoaded(long address, boolean isSync, long size) {
        // a sync mapped buffer is always loaded
        if (isSync) {
            return true;
        }
        if ((address == 0) || (size == 0))
            return true;
        long offset = mappingOffset(address);
        long length = mappingLength(offset, size);
        return isLoaded0(mappingAddress(address, offset), length, Bits.pageCount(length));
    }

    static void load(long address, boolean isSync, long size) {
        // no need to load a sync mapped buffer
        if (isSync) {
            return;
        }
        if ((address == 0) || (size == 0))
            return;
        long offset = mappingOffset(address);
        long length = mappingLength(offset, size);
        load0(mappingAddress(address, offset), length);

        // Read a byte from each page to bring it into memory. A checksum
        // is computed as we go along to prevent the compiler from otherwise
        // considering the loop as dead code.
        Unsafe unsafe = Unsafe.getUnsafe();
        int ps = Bits.pageSize();
        long count = Bits.pageCount(length);
        long a = mappingAddress(address, offset);
        byte x = 0;
        for (long i=0; i<count; i++) {
            // TODO consider changing to getByteOpaque thus avoiding
            // dead code elimination and the need to calculate a checksum
            x ^= unsafe.getByte(a);
            a += ps;
        }
        if (unused != 0)
            unused = x;
    }

    // not used, but a potential target for a store, see load() for details.
    private static byte unused;

    static void unload(long address, boolean isSync, long size) {
        // no need to load a sync mapped buffer
        if (isSync) {
            return;
        }
        if ((address == 0) || (size == 0))
            return;
        long offset = mappingOffset(address);
        long length = mappingLength(offset, size);
        unload0(mappingAddress(address, offset), length);
    }

    static void force(FileDescriptor fd, long address, boolean isSync, long index, long length) {
        if (isSync) {
            // simply force writeback of associated cache lines
            Unsafe.getUnsafe().writebackMemory(address + index, length);
        } else {
            // force writeback via file descriptor
            long offset = mappingOffset(address, index);
            try {
                force0(fd, mappingAddress(address, offset, index), mappingLength(offset, length));
            } catch (IOException cause) {
                throw new UncheckedIOException(cause);
            }
        }
    }

    // native methods

    private static native boolean isLoaded0(long address, long length, long pageCount);
    private static native void load0(long address, long length);
    private static native void unload0(long address, long length);
    private static native void force0(FileDescriptor fd, long address, long length) throws IOException;

    // utility methods

    // Returns the distance (in bytes) of the buffer start from the
    // largest page aligned address of the mapping less than or equal
    // to the start address.
    private static long mappingOffset(long address) {
        return mappingOffset(address, 0);
    }

    // Returns the distance (in bytes) of the buffer element
    // identified by index from the largest page aligned address of
    // the mapping less than or equal to the element address. Computed
    // each time to avoid storing in every direct buffer.
    private static long mappingOffset(long address, long index) {
        int ps = Bits.pageSize();
        long indexAddress = address + index;
        long baseAddress = alignDown(indexAddress, ps);
        return indexAddress - baseAddress;
    }

    // Given an offset previously obtained from calling
    // mappingOffset() returns the largest page aligned address of the
    // mapping less than or equal to the buffer start address.
    private static long mappingAddress(long address, long mappingOffset) {
        return mappingAddress(address, mappingOffset, 0);
    }

    // Given an offset previously otained from calling
    // mappingOffset(index) returns the largest page aligned address
    // of the mapping less than or equal to the address of the buffer
    // element identified by index.
    private static long mappingAddress(long address, long mappingOffset, long index) {
        long indexAddress = address + index;
        return indexAddress - mappingOffset;
    }

    // given a mappingOffset previously otained from calling
    // mappingOffset(index) return that offset added to the supplied
    // length.
    private static long mappingLength(long mappingOffset, long length) {
        return length + mappingOffset;
    }

    // align address down to page size
    private static long alignDown(long address, int pageSize) {
        // pageSize must be a power of 2
        return address & ~(pageSize - 1);
    }
}
