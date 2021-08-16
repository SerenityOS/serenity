/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.nio.mapmode;

import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

/**
 * JDK-specific map modes.
 *
 * @since 14
 * @see java.nio.channels.FileChannel#map
 */
public class ExtendedMapMode {
    private ExtendedMapMode() { }

    /**
     * File mapping mode for a read-only mapping of a file backed by
     * non-volatile RAM.
     *
     * <p> The {@linkplain FileChannel#map map} method throws
     * {@linkplain UnsupportedOperationException} when this map mode
     * is used on an implementation that does not support it.
     *
     * @implNote On Linux, the {@code MAP_SYNC} and {@code
     * MAP_SHARED_VALIDATE} flags are specified to {@code mmap} when
     * mapping the file into memory.
     */
    public static final MapMode READ_ONLY_SYNC = jdk.internal.misc.ExtendedMapMode.READ_ONLY_SYNC;

    /**
     * File mapping mode for a read-write mapping of a file backed by
     * non-volatile RAM. {@linkplain MappedByteBuffer#force force}
     * operations on a buffer created with this mode will be performed
     * using cache line writeback rather than proceeding via a file
     * device flush.
     *
     * <p> The {@linkplain FileChannel#map map} method throws
     * {@linkplain UnsupportedOperationException} when this map mode
     * is used on an implementation that does not support it.
     *
     * @implNote On Linux, the {@code MAP_SYNC} and {@code
     * MAP_SHARED_VALIDATE} flags are specified to {@code mmap} when
     * mapping the file into memory.
     */
    public static final MapMode READ_WRITE_SYNC = jdk.internal.misc.ExtendedMapMode.READ_WRITE_SYNC;

}
