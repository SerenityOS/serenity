/*
 * Copyright (c) 1996, 2014, Oracle and/or its affiliates. All rights reserved.
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
package java.util.zip;

import java.nio.ByteBuffer;

/**
 * An interface representing a data checksum.
 *
 * @author David Connelly
 * @since 1.1
 */
public interface Checksum {

    /**
     * Updates the current checksum with the specified byte.
     *
     * @param b the byte to update the checksum with
     */
    public void update(int b);

    /**
     * Updates the current checksum with the specified array of bytes.
     *
     * @implSpec This default implementation is equal to calling
     * {@code update(b, 0, b.length)}.
     *
     * @param b the array of bytes to update the checksum with
     *
     * @throws NullPointerException
     *         if {@code b} is {@code null}
     *
     * @since 9
     */
    default public void update(byte[] b) {
        update(b, 0, b.length);
    }

    /**
     * Updates the current checksum with the specified array of bytes.
     *
     * @param b the byte array to update the checksum with
     * @param off the start offset of the data
     * @param len the number of bytes to use for the update
     */
    public void update(byte[] b, int off, int len);

    /**
     * Updates the current checksum with the bytes from the specified buffer.
     *
     * The checksum is updated with the remaining bytes in the buffer, starting
     * at the buffer's position. Upon return, the buffer's position will be
     * updated to its limit; its limit will not have been changed.
     *
     * @apiNote For best performance with DirectByteBuffer and other ByteBuffer
     * implementations without a backing array implementers of this interface
     * should override this method.
     *
     * @implSpec The default implementation has the following behavior.<br>
     * For ByteBuffers backed by an accessible byte array.
     * <pre>{@code
     * update(buffer.array(),
     *        buffer.position() + buffer.arrayOffset(),
     *        buffer.remaining());
     * }</pre>
     * For ByteBuffers not backed by an accessible byte array.
     * <pre>{@code
     * byte[] b = new byte[Math.min(buffer.remaining(), 4096)];
     * while (buffer.hasRemaining()) {
     *     int length = Math.min(buffer.remaining(), b.length);
     *     buffer.get(b, 0, length);
     *     update(b, 0, length);
     * }
     * }</pre>
     *
     * @param buffer the ByteBuffer to update the checksum with
     *
     * @throws NullPointerException
     *         if {@code buffer} is {@code null}
     *
     * @since 9
     */
    default public void update(ByteBuffer buffer) {
        int pos = buffer.position();
        int limit = buffer.limit();
        assert (pos <= limit);
        int rem = limit - pos;
        if (rem <= 0) {
            return;
        }
        if (buffer.hasArray()) {
            update(buffer.array(), pos + buffer.arrayOffset(), rem);
        } else {
            byte[] b = new byte[Math.min(buffer.remaining(), 4096)];
            while (buffer.hasRemaining()) {
                int length = Math.min(buffer.remaining(), b.length);
                buffer.get(b, 0, length);
                update(b, 0, length);
            }
        }
        buffer.position(limit);
    }

    /**
     * Returns the current checksum value.
     *
     * @return the current checksum value
     */
    public long getValue();

    /**
     * Resets the checksum to its initial value.
     */
    public void reset();
}
