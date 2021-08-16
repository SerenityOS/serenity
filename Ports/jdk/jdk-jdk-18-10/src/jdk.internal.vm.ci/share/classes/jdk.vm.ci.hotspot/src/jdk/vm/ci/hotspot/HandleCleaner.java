/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.hotspot;

import static jdk.vm.ci.hotspot.UnsafeAccess.UNSAFE;

/**
 * This class manages a set of {@code jobject} and {@code jmetadata} handles whose lifetimes are
 * dependent on associated {@link IndirectHotSpotObjectConstantImpl} and
 * {@link MetaspaceHandleObject} wrapper objects respectively.
 *
 * The general theory of operation is that all wrappers are created by calling into the VM which
 * calls back out to actually create the wrapper instance. During the call the VM keeps the object
 * or metadata reference alive through the use of handles. Once the call completes the wrapper
 * object is registered here and will be scanned during metadata scanning. The weakness of the
 * reference to the wrapper object allows the handles to be reclaimed when they are no longer used.
 */
final class HandleCleaner extends Cleaner {

    /**
     * A {@code jmetadata} or {@code jobject} handle.
     */
    private final long handle;

    /**
     * Specifies if {@link #handle} is a {@code jobject} or {@code jmetadata}.
     */
    private final boolean isJObject;

    private HandleCleaner(Object wrapper, long handle, boolean isJObject) {
        super(wrapper);
        this.handle = handle;
        this.isJObject = isJObject;
    }

    /**
     * Releases the resource associated with {@code this.handle}.
     */
    @Override
    void doCleanup() {
        if (isJObject) {
            // The sentinel value used to denote a free handle is
            // an object on the HotSpot heap so we call into the
            // VM to set the target of an object handle to this value.
            CompilerToVM.compilerToVM().deleteGlobalHandle(handle);
        } else {
            // Setting the target of a jmetadata handle to 0 enables
            // the handle to be reused. See MetadataHandles in
            // metadataHandles.hpp for more info.
            long value = UNSAFE.getLong(null, handle);
            UNSAFE.compareAndSetLong(null, handle, value, 0);
        }
    }

    /**
     * Registers a cleaner for {@code handle}. The cleaner will release the handle some time after
     * {@code wrapper} is detected as unreachable by the garbage collector.
     */
    @SuppressWarnings("unused")
    static void create(Object wrapper, long handle) {
        assert wrapper instanceof IndirectHotSpotObjectConstantImpl || wrapper instanceof MetaspaceHandleObject;
        new HandleCleaner(wrapper, handle, wrapper instanceof IndirectHotSpotObjectConstantImpl);
    }
}
