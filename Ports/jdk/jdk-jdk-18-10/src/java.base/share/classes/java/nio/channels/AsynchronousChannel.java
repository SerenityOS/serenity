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

import java.io.IOException;
import java.util.concurrent.Future;  // javadoc

/**
 * A channel that supports asynchronous I/O operations. Asynchronous I/O
 * operations will usually take one of two forms:
 *
 * <ol>
 * <li><pre>{@link Future}&lt;V&gt; <em>operation</em>(<em>...</em>)</pre></li>
 * <li><pre>void <em>operation</em>(<em>...</em> A attachment, {@link
 *   CompletionHandler}&lt;V,? super A&gt; handler)</pre></li>
 * </ol>
 *
 * where <i>operation</i> is the name of the I/O operation (read or write for
 * example), <i>V</i> is the result type of the I/O operation, and <i>A</i> is
 * the type of an object attached to the I/O operation to provide context when
 * consuming the result. The attachment is important for cases where a
 * <em>state-less</em> {@code CompletionHandler} is used to consume the result
 * of many I/O operations.
 *
 * <p> In the first form, the methods defined by the {@link Future Future}
 * interface may be used to check if the operation has completed, wait for its
 * completion, and to retrieve the result. In the second form, a {@link
 * CompletionHandler} is invoked to consume the result of the I/O operation when
 * it completes or fails.
 *
 * <p> A channel that implements this interface is <em>asynchronously
 * closeable</em>: If an I/O operation is outstanding on the channel and the
 * channel's {@link #close close} method is invoked, then the I/O operation
 * fails with the exception {@link AsynchronousCloseException}.
 *
 * <p> Asynchronous channels are safe for use by multiple concurrent threads.
 * Some channel implementations may support concurrent reading and writing, but
 * may not allow more than one read and one write operation to be outstanding at
 * any given time.
 *
 * <h2>Cancellation</h2>
 *
 * <p> The {@code Future} interface defines the {@link Future#cancel cancel}
 * method to cancel execution. This causes all threads waiting on the result of
 * the I/O operation to throw {@link java.util.concurrent.CancellationException}.
 * Whether the underlying I/O operation can be cancelled is highly implementation
 * specific and therefore not specified. Where cancellation leaves the channel,
 * or the entity to which it is connected, in an inconsistent state, then the
 * channel is put into an implementation specific <em>error state</em> that
 * prevents further attempts to initiate I/O operations that are <i>similar</i>
 * to the operation that was cancelled. For example, if a read operation is
 * cancelled but the implementation cannot guarantee that bytes have not been
 * read from the channel then it puts the channel into an error state; further
 * attempts to initiate a {@code read} operation cause an unspecified runtime
 * exception to be thrown. Similarly, if a write operation is cancelled but the
 * implementation cannot guarantee that bytes have not been written to the
 * channel then subsequent attempts to initiate a {@code write} will fail with
 * an unspecified runtime exception.
 *
 * <p> Where the {@link Future#cancel cancel} method is invoked with the {@code
 * mayInterruptIfRunning} parameter set to {@code true} then the I/O operation
 * may be interrupted by closing the channel. In that case all threads waiting
 * on the result of the I/O operation throw {@code CancellationException} and
 * any other I/O operations outstanding on the channel complete with the
 * exception {@link AsynchronousCloseException}.
 *
 * <p> Where the {@code cancel} method is invoked to cancel read or write
 * operations then it is recommended that all buffers used in the I/O operations
 * be discarded or care taken to ensure that the buffers are not accessed while
 * the channel remains open.
 *
 *  @since 1.7
 */

public interface AsynchronousChannel
    extends Channel
{
    /**
     * Closes this channel.
     *
     * <p> Any outstanding asynchronous operations upon this channel will
     * complete with the exception {@link AsynchronousCloseException}. After a
     * channel is closed, further attempts to initiate asynchronous I/O
     * operations complete immediately with cause {@link ClosedChannelException}.
     *
     * <p>  This method otherwise behaves exactly as specified by the {@link
     * Channel} interface.
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    @Override
    void close() throws IOException;
}
