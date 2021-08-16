/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.channels.spi.*;


/**
 * A pair of channels that implements a unidirectional pipe.
 *
 * <p> A pipe consists of a pair of channels: A writable {@link
 * Pipe.SinkChannel sink} channel and a readable {@link Pipe.SourceChannel source}
 * channel.  Once some bytes are written to the sink channel they can be read
 * from the source channel in exactly the order in which they were written.
 *
 * <p> Whether or not a thread writing bytes to a pipe will block until another
 * thread reads those bytes, or some previously-written bytes, from the pipe is
 * system-dependent and therefore unspecified.  Many pipe implementations will
 * buffer up to a certain number of bytes between the sink and source channels,
 * but such buffering should not be assumed.  </p>
 *
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */

public abstract class Pipe {

    /**
     * A channel representing the readable end of a {@link Pipe}.
     *
     * @since 1.4
     */
    public abstract static class SourceChannel
        extends AbstractSelectableChannel
        implements ReadableByteChannel, ScatteringByteChannel
    {
        /**
         * Constructs a new instance of this class.
         *
         * @param  provider
         *         The selector provider
         */
        protected SourceChannel(SelectorProvider provider) {
            super(provider);
        }

        /**
         * Returns an operation set identifying this channel's supported
         * operations.
         *
         * <p> Pipe-source channels only support reading, so this method
         * returns {@link SelectionKey#OP_READ}.  </p>
         *
         * @return  The valid-operation set
         */
        public final int validOps() {
            return SelectionKey.OP_READ;
        }

    }

    /**
     * A channel representing the writable end of a {@link Pipe}.
     *
     * @since 1.4
     */
    public abstract static class SinkChannel
        extends AbstractSelectableChannel
        implements WritableByteChannel, GatheringByteChannel
    {
        /**
         * Initializes a new instance of this class.
         *
         * @param  provider
         *         The selector provider
         */
        protected SinkChannel(SelectorProvider provider) {
            super(provider);
        }

        /**
         * Returns an operation set identifying this channel's supported
         * operations.
         *
         * <p> Pipe-sink channels only support writing, so this method returns
         * {@link SelectionKey#OP_WRITE}.  </p>
         *
         * @return  The valid-operation set
         */
        public final int validOps() {
            return SelectionKey.OP_WRITE;
        }

    }

    /**
     * Initializes a new instance of this class.
     */
    protected Pipe() { }

    /**
     * Returns this pipe's source channel.
     *
     * @return  This pipe's source channel
     */
    public abstract SourceChannel source();

    /**
     * Returns this pipe's sink channel.
     *
     * @return  This pipe's sink channel
     */
    public abstract SinkChannel sink();

    /**
     * Opens a pipe.
     *
     * <p> The new pipe is created by invoking the {@link
     * java.nio.channels.spi.SelectorProvider#openPipe openPipe} method of the
     * system-wide default {@link java.nio.channels.spi.SelectorProvider}
     * object.  </p>
     *
     * @return  A new pipe
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    public static Pipe open() throws IOException {
        return SelectorProvider.provider().openPipe();
    }

}
