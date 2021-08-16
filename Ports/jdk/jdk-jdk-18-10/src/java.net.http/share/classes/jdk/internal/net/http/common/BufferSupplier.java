/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.net.http.common;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 *  This interface allows to recycle buffers used for SSL decryption.
 *  Buffers that are used for reading SSL encrypted data are typically
 *  very short lived, as it is necessary to aggregate their content
 *  before calling SSLEngine::unwrap.
 *  Because both reading and copying happen in the SelectorManager
 *  thread, then it makes it possible to pool these buffers and
 *  recycle them for the next socket read, instead of simply
 *  letting them be GC'ed. That also makes it possible to use
 *  direct byte buffers, and avoid another layer of copying by
 *  the SocketChannel implementation.
 *
 *  The HttpClientImpl has an implementation of this interface
 *  that allows to reuse the same 3 direct buffers for reading
 *  off SSL encrypted data from the socket.
 *  The BufferSupplier::get method is called by SocketTube
 *  (see SocketTube.SSLDirectBufferSource) and BufferSupplier::recycle
 *  is called by SSLFlowDelegate.Reader.
 **/
public interface BufferSupplier extends Supplier<ByteBuffer> {
    /**
     * Returns a buffer to read encrypted data off the socket.
     * @return a buffer to read encrypted data off the socket.
     */
    ByteBuffer get();

    /**
     * Returns a buffer to the pool.
     *
     * @param buffer This must be a buffer previously obtained
     *               by calling BufferSupplier::get. The caller must
     *               not touch the buffer after returning it to
     *               the pool.
     */
    void recycle(ByteBuffer buffer);
}

