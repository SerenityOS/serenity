/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.ByteBuffer;
import java.util.concurrent.Future;

/**
 * An asynchronous channel that can read and write bytes.
 *
 * <p> Some channels may not allow more than one read or write to be outstanding
 * at any given time. If a thread invokes a read method before a previous read
 * operation has completed then a {@link ReadPendingException} will be thrown.
 * Similarly, if a write method is invoked before a previous write has completed
 * then {@link WritePendingException} is thrown. Whether or not other kinds of
 * I/O operations may proceed concurrently with a read operation depends upon
 * the type of the channel.
 *
 * <p> Note that {@link java.nio.ByteBuffer ByteBuffers} are not safe for use by
 * multiple concurrent threads. When a read or write operation is initiated then
 * care must be taken to ensure that the buffer is not accessed until the
 * operation completes.
 *
 * @see Channels#newInputStream(AsynchronousByteChannel)
 * @see Channels#newOutputStream(AsynchronousByteChannel)
 *
 * @since 1.7
 */

public interface AsynchronousByteChannel
    extends AsynchronousChannel
{
    /**
     * Reads a sequence of bytes from this channel into the given buffer.
     *
     * <p> This method initiates an asynchronous read operation to read a
     * sequence of bytes from this channel into the given buffer. The {@code
     * handler} parameter is a completion handler that is invoked when the read
     * operation completes (or fails). The result passed to the completion
     * handler is the number of bytes read or {@code -1} if no bytes could be
     * read because the channel has reached end-of-stream.
     *
     * <p> The read operation may read up to <i>r</i> bytes from the channel,
     * where <i>r</i> is the number of bytes remaining in the buffer, that is,
     * {@code dst.remaining()} at the time that the read is attempted. Where
     * <i>r</i> is 0, the read operation completes immediately with a result of
     * {@code 0} without initiating an I/O operation.
     *
     * <p> Suppose that a byte sequence of length <i>n</i> is read, where
     * {@code 0}&nbsp;{@code <}&nbsp;<i>n</i>&nbsp;{@code <=}&nbsp;<i>r</i>.
     * This byte sequence will be transferred into the buffer so that the first
     * byte in the sequence is at index <i>p</i> and the last byte is at index
     * <i>p</i>&nbsp;{@code +}&nbsp;<i>n</i>&nbsp;{@code -}&nbsp;{@code 1},
     * where <i>p</i> is the buffer's position at the moment the read is
     * performed. Upon completion the buffer's position will be equal to
     * <i>p</i>&nbsp;{@code +}&nbsp;<i>n</i>; its limit will not have changed.
     *
     * <p> Buffers are not safe for use by multiple concurrent threads so care
     * should be taken to not access the buffer until the operation has
     * completed.
     *
     * <p> This method may be invoked at any time. Some channel types may not
     * allow more than one read to be outstanding at any given time. If a thread
     * initiates a read operation before a previous read operation has
     * completed then a {@link ReadPendingException} will be thrown.
     *
     * @param   <A>
     *          The type of the attachment
     * @param   dst
     *          The buffer into which bytes are to be transferred
     * @param   attachment
     *          The object to attach to the I/O operation; can be {@code null}
     * @param   handler
     *          The completion handler
     *
     * @throws  IllegalArgumentException
     *          If the buffer is read-only
     * @throws  ReadPendingException
     *          If the channel does not allow more than one read to be outstanding
     *          and a previous read has not completed
     * @throws  ShutdownChannelGroupException
     *          If the channel is associated with a {@link AsynchronousChannelGroup
     *          group} that has terminated
     */
    <A> void read(ByteBuffer dst,
                  A attachment,
                  CompletionHandler<Integer,? super A> handler);

    /**
     * Reads a sequence of bytes from this channel into the given buffer.
     *
     * <p> This method initiates an asynchronous read operation to read a
     * sequence of bytes from this channel into the given buffer. The method
     * behaves in exactly the same manner as the {@link
     * #read(ByteBuffer,Object,CompletionHandler)
     * read(ByteBuffer,Object,CompletionHandler)} method except that instead
     * of specifying a completion handler, this method returns a {@code Future}
     * representing the pending result. The {@code Future}'s {@link Future#get()
     * get} method returns the number of bytes read or {@code -1} if no bytes
     * could be read because the channel has reached end-of-stream.
     *
     * @param   dst
     *          The buffer into which bytes are to be transferred
     *
     * @return  A Future representing the result of the operation
     *
     * @throws  IllegalArgumentException
     *          If the buffer is read-only
     * @throws  ReadPendingException
     *          If the channel does not allow more than one read to be outstanding
     *          and a previous read has not completed
     */
    Future<Integer> read(ByteBuffer dst);

    /**
     * Writes a sequence of bytes to this channel from the given buffer.
     *
     * <p> This method initiates an asynchronous write operation to write a
     * sequence of bytes to this channel from the given buffer. The {@code
     * handler} parameter is a completion handler that is invoked when the write
     * operation completes (or fails). The result passed to the completion
     * handler is the number of bytes written.
     *
     * <p> The write operation may write up to <i>r</i> bytes to the channel,
     * where <i>r</i> is the number of bytes remaining in the buffer, that is,
     * {@code src.remaining()} at the time that the write is attempted. Where
     * <i>r</i> is 0, the write operation completes immediately with a result of
     * {@code 0} without initiating an I/O operation.
     *
     * <p> Suppose that a byte sequence of length <i>n</i> is written, where
     * {@code 0}&nbsp;{@code <}&nbsp;<i>n</i>&nbsp;{@code <=}&nbsp;<i>r</i>.
     * This byte sequence will be transferred from the buffer starting at index
     * <i>p</i>, where <i>p</i> is the buffer's position at the moment the
     * write is performed; the index of the last byte written will be
     * <i>p</i>&nbsp;{@code +}&nbsp;<i>n</i>&nbsp;{@code -}&nbsp;{@code 1}.
     * Upon completion the buffer's position will be equal to
     * <i>p</i>&nbsp;{@code +}&nbsp;<i>n</i>; its limit will not have changed.
     *
     * <p> Buffers are not safe for use by multiple concurrent threads so care
     * should be taken to not access the buffer until the operation has
     * completed.
     *
     * <p> This method may be invoked at any time. Some channel types may not
     * allow more than one write to be outstanding at any given time. If a thread
     * initiates a write operation before a previous write operation has
     * completed then a {@link WritePendingException} will be thrown.
     *
     * @param   <A>
     *          The type of the attachment
     * @param   src
     *          The buffer from which bytes are to be retrieved
     * @param   attachment
     *          The object to attach to the I/O operation; can be {@code null}
     * @param   handler
     *          The completion handler object
     *
     * @throws  WritePendingException
     *          If the channel does not allow more than one write to be outstanding
     *          and a previous write has not completed
     * @throws  ShutdownChannelGroupException
     *          If the channel is associated with a {@link AsynchronousChannelGroup
     *          group} that has terminated
     */
    <A> void write(ByteBuffer src,
                   A attachment,
                   CompletionHandler<Integer,? super A> handler);

    /**
     * Writes a sequence of bytes to this channel from the given buffer.
     *
     * <p> This method initiates an asynchronous write operation to write a
     * sequence of bytes to this channel from the given buffer. The method
     * behaves in exactly the same manner as the {@link
     * #write(ByteBuffer,Object,CompletionHandler)
     * write(ByteBuffer,Object,CompletionHandler)} method except that instead
     * of specifying a completion handler, this method returns a {@code Future}
     * representing the pending result. The {@code Future}'s {@link Future#get()
     * get} method returns the number of bytes written.
     *
     * @param   src
     *          The buffer from which bytes are to be retrieved
     *
     * @return A Future representing the result of the operation
     *
     * @throws  WritePendingException
     *          If the channel does not allow more than one write to be outstanding
     *          and a previous write has not completed
     */
    Future<Integer> write(ByteBuffer src);
}
