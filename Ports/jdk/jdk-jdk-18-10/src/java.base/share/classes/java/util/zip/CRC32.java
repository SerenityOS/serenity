/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.util.Objects;

import sun.nio.ch.DirectBuffer;
import jdk.internal.util.Preconditions;
import jdk.internal.vm.annotation.IntrinsicCandidate;

/**
 * A class that can be used to compute the CRC-32 of a data stream.
 *
 * <p> Passing a {@code null} argument to a method in this class will cause
 * a {@link NullPointerException} to be thrown.</p>
 *
 * @author      David Connelly
 * @since 1.1
 */
public class CRC32 implements Checksum {
    private int crc;

    /**
     * Creates a new CRC32 object.
     */
    public CRC32() {
    }


    /**
     * Updates the CRC-32 checksum with the specified byte (the low
     * eight bits of the argument b).
     */
    @Override
    public void update(int b) {
        crc = update(crc, b);
    }

    /**
     * Updates the CRC-32 checksum with the specified array of bytes.
     *
     * @throws ArrayIndexOutOfBoundsException
     *         if {@code off} is negative, or {@code len} is negative, or
     *         {@code off+len} is negative or greater than the length of
     *         the array {@code b}.
     */
    @Override
    public void update(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        }
        Preconditions.checkFromIndexSize(len, off, b.length, Preconditions.AIOOBE_FORMATTER);
        crc = updateBytes(crc, b, off, len);
    }

    /**
     * Updates the CRC-32 checksum with the bytes from the specified buffer.
     *
     * The checksum is updated with the remaining bytes in the buffer, starting
     * at the buffer's position. Upon return, the buffer's position will be
     * updated to its limit; its limit will not have been changed.
     *
     * @since 1.8
     */
    @Override
    public void update(ByteBuffer buffer) {
        int pos = buffer.position();
        int limit = buffer.limit();
        assert (pos <= limit);
        int rem = limit - pos;
        if (rem <= 0)
            return;
        if (buffer.isDirect()) {
            try {
                crc = updateByteBuffer(crc, ((DirectBuffer)buffer).address(), pos, rem);
            } finally {
                Reference.reachabilityFence(buffer);
            }
        } else if (buffer.hasArray()) {
            crc = updateBytes(crc, buffer.array(), pos + buffer.arrayOffset(), rem);
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
     * Resets CRC-32 to initial value.
     */
    @Override
    public void reset() {
        crc = 0;
    }

    /**
     * Returns CRC-32 value.
     */
    @Override
    public long getValue() {
        return (long)crc & 0xffffffffL;
    }

    @IntrinsicCandidate
    private static native int update(int crc, int b);

    private static int updateBytes(int crc, byte[] b, int off, int len) {
        updateBytesCheck(b, off, len);
        return updateBytes0(crc, b, off, len);
    }

    @IntrinsicCandidate
    private static native int updateBytes0(int crc, byte[] b, int off, int len);

    private static void updateBytesCheck(byte[] b, int off, int len) {
        if (len <= 0) {
            return;  // not an error because updateBytesImpl won't execute if len <= 0
        }

        Objects.requireNonNull(b);
        Preconditions.checkIndex(off, b.length, Preconditions.AIOOBE_FORMATTER);
        Preconditions.checkIndex(off + len - 1, b.length, Preconditions.AIOOBE_FORMATTER);
    }

    private static int updateByteBuffer(int alder, long addr,
                                        int off, int len) {
        updateByteBufferCheck(addr);
        return updateByteBuffer0(alder, addr, off, len);
    }

    @IntrinsicCandidate
    private static native int updateByteBuffer0(int alder, long addr,
                                                int off, int len);

    private static void updateByteBufferCheck(long addr) {
        // Performs only a null check because bounds checks
        // are not easy to do on raw addresses.
        if (addr == 0L) {
            throw new NullPointerException();
        }
    }

    static {
        ZipUtils.loadLibrary();
    }
}
