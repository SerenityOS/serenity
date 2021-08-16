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

package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * A channel that can write bytes from a sequence of buffers.
 *
 * <p> A <i>gathering</i> write operation writes, in a single invocation, a
 * sequence of bytes from one or more of a given sequence of buffers.
 * Gathering writes are often useful when implementing network protocols or
 * file formats that, for example, group data into segments consisting of one
 * or more fixed-length headers followed by a variable-length body.  Similar
 * <i>scattering</i> read operations are defined in the {@link
 * ScatteringByteChannel} interface.  </p>
 *
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */

public interface GatheringByteChannel
    extends WritableByteChannel
{

    /**
     * Writes a sequence of bytes to this channel from a subsequence of the
     * given buffers.
     *
     * <p> An attempt is made to write up to <i>r</i> bytes to this channel,
     * where <i>r</i> is the total number of bytes remaining in the specified
     * subsequence of the given buffer array, that is,
     *
     * <blockquote><pre>
     * srcs[offset].remaining()
     *     + srcs[offset+1].remaining()
     *     + ... + srcs[offset+length-1].remaining()</pre></blockquote>
     *
     * at the moment that this method is invoked.
     *
     * <p> Suppose that a byte sequence of length <i>n</i> is written, where
     * {@code 0}&nbsp;{@code <=}&nbsp;<i>n</i>&nbsp;{@code <=}&nbsp;<i>r</i>.
     * Up to the first {@code srcs[offset].remaining()} bytes of this sequence
     * are written from buffer {@code srcs[offset]}, up to the next
     * {@code srcs[offset+1].remaining()} bytes are written from buffer
     * {@code srcs[offset+1]}, and so forth, until the entire byte sequence is
     * written.  As many bytes as possible are written from each buffer, hence
     * the final position of each updated buffer, except the last updated
     * buffer, is guaranteed to be equal to that buffer's limit.
     *
     * <p> Unless otherwise specified, a write operation will return only after
     * writing all of the <i>r</i> requested bytes.  Some types of channels,
     * depending upon their state, may write only some of the bytes or possibly
     * none at all.  A socket channel in non-blocking mode, for example, cannot
     * write any more bytes than are free in the socket's output buffer.
     *
     * <p> This method may be invoked at any time.  If another thread has
     * already initiated a write operation upon this channel, however, then an
     * invocation of this method will block until the first operation is
     * complete. </p>
     *
     * @param  srcs
     *         The buffers from which bytes are to be retrieved
     *
     * @param  offset
     *         The offset within the buffer array of the first buffer from
     *         which bytes are to be retrieved; must be non-negative and no
     *         larger than {@code srcs.length}
     *
     * @param  length
     *         The maximum number of buffers to be accessed; must be
     *         non-negative and no larger than
     *         {@code srcs.length}&nbsp;-&nbsp;{@code offset}
     *
     * @return  The number of bytes written, possibly zero
     *
     * @throws  IndexOutOfBoundsException
     *          If the preconditions on the {@code offset} and {@code length}
     *          parameters do not hold
     *
     * @throws  NonWritableChannelException
     *          If this channel was not opened for writing
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  AsynchronousCloseException
     *          If another thread closes this channel
     *          while the write operation is in progress
     *
     * @throws  ClosedByInterruptException
     *          If another thread interrupts the current thread
     *          while the write operation is in progress, thereby
     *          closing the channel and setting the current thread's
     *          interrupt status
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public long write(ByteBuffer[] srcs, int offset, int length)
        throws IOException;


    /**
     * Writes a sequence of bytes to this channel from the given buffers.
     *
     * <p> An invocation of this method of the form {@code c.write(srcs)}
     * behaves in exactly the same manner as the invocation
     *
     * <blockquote><pre>
     * c.write(srcs, 0, srcs.length);</pre></blockquote>
     *
     * @param  srcs
     *         The buffers from which bytes are to be retrieved
     *
     * @return  The number of bytes written, possibly zero
     *
     * @throws  NonWritableChannelException
     *          If this channel was not opened for writing
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  AsynchronousCloseException
     *          If another thread closes this channel
     *          while the write operation is in progress
     *
     * @throws  ClosedByInterruptException
     *          If another thread interrupts the current thread
     *          while the write operation is in progress, thereby
     *          closing the channel and setting the current thread's
     *          interrupt status
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public long write(ByteBuffer[] srcs) throws IOException;

}
