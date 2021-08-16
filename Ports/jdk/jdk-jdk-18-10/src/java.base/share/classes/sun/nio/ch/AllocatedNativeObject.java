/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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

/*
 */

package sun.nio.ch;                                     // Formerly in sun.misc


// ## In the fullness of time, this class will be eliminated

class AllocatedNativeObject                             // package-private
    extends NativeObject
{

    /**
     * Allocates a memory area of at least {@code size} bytes outside of the
     * Java heap and creates a native object for that area.
     *
     * @param  size
     *         Number of bytes to allocate
     *
     * @param  pageAligned
     *         If {@code true} then the area will be aligned on a hardware
     *         page boundary
     *
     * @throws OutOfMemoryError
     *         If the request cannot be satisfied
     */
    AllocatedNativeObject(int size, boolean pageAligned) {
        super(size, pageAligned);
    }

    /**
     * Frees the native memory area associated with this object.
     */
    synchronized void free() {
        if (allocationAddress != 0) {
            unsafe.freeMemory(allocationAddress);
            allocationAddress = 0;
        }
    }

}
