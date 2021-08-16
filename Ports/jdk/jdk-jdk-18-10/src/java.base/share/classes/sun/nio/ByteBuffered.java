/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio;

import java.nio.ByteBuffer;
import java.io.IOException;

/**
 * This is an interface to adapt existing APIs to use {@link java.nio.ByteBuffer
 * ByteBuffers} as the underlying data format.  Only the initial producer and
 * final consumer have to be changed.
 *
 * <p>
 * For example, the Zip/Jar code supports {@link java.io.InputStream InputStreams}.
 * To make the Zip code use {@link java.nio.MappedByteBuffer MappedByteBuffers} as
 * the underlying data structure, it can create a class of InputStream that wraps
 * the ByteBuffer, and implements the ByteBuffered interface. A co-operating class
 * several layers away can ask the InputStream if it is an instance of ByteBuffered,
 * then call the {@link #getByteBuffer()} method.
 */
public interface ByteBuffered {

    /**
     * Returns the {@code ByteBuffer} behind this object, if this particular
     * instance has one. An implementation of {@code getByteBuffer()} is allowed
     * to return {@code null} for any reason.
     *
     * @return  The {@code ByteBuffer}, if this particular instance has one,
     *          or {@code null} otherwise.
     *
     * @throws  IOException
     *          If the ByteBuffer is no longer valid.
     *
     * @since  1.5
     */
    public ByteBuffer getByteBuffer() throws IOException;
}
