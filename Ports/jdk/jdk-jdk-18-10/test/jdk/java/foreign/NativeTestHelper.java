/*
 *  Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.
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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;

public class NativeTestHelper {

    static CLinker.TypeKind kind(MemoryLayout layout) {
        return (CLinker.TypeKind)layout.attribute(CLinker.TypeKind.ATTR_NAME).orElseThrow(
                () -> new IllegalStateException("Unexpected value layout: could not determine ABI class"));
    }

    public static boolean isIntegral(MemoryLayout layout) {
        return kind(layout).isIntegral();
    }

    public static boolean isPointer(MemoryLayout layout) {
        return kind(layout).isPointer();
    }

    public static class NativeScope implements SegmentAllocator, AutoCloseable {
        final ResourceScope resourceScope;
        final ResourceScope.Handle scopeHandle;
        final SegmentAllocator allocator;

        long allocatedBytes = 0;

        public NativeScope() {
            this.resourceScope = ResourceScope.newConfinedScope();
            this.scopeHandle = resourceScope.acquire();
            this.allocator = SegmentAllocator.arenaAllocator(resourceScope);
        }

        @Override
        public MemorySegment allocate(long bytesSize, long bytesAlignment) {
            allocatedBytes += bytesSize;
            return allocator.allocate(bytesSize, bytesAlignment);
        }

        public ResourceScope scope() {
            return resourceScope;
        }

        public long allocatedBytes() {
            return allocatedBytes;
        }

        @Override
        public void close() {
            resourceScope.release(scopeHandle);
            resourceScope.close();
        }
    }
}
