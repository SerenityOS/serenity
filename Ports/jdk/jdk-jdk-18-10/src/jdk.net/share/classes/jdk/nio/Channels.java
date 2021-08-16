/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.nio;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Objects;

import sun.nio.ch.IOUtil;
import sun.nio.ch.Net;
import sun.nio.ch.SelChImpl;
import sun.nio.ch.SelectionKeyImpl;
import sun.nio.ch.SelectorProviderImpl;

/**
 * Defines static methods to create {@link java.nio.channels.Channel channels}.
 *
 * <p> Unless otherwise specified, passing a {@code null} argument to any of the
 * methods defined here will cause a {@code NullPointerException} to be thrown.
 *
 * @since 11
 */

public final class Channels {
    private Channels() { }

    /**
     * An object used to coordinate the closing of a selectable channel created
     * by {@link Channels#readWriteSelectableChannel readWriteSelectableChannel}.
     *
     * @since 11
     */
    public interface SelectableChannelCloser {

        /**
         * Closes a selectable channel.
         *
         * <p> This method is invoked by the channel's close method in order to
         * perform the actual work of closing the channel. This method is only
         * invoked if the channel has not yet been closed, and it is never
         * invoked more than once by the channel's close implementation.
         *
         * <p> An implementation of this method must arrange for any other
         * thread that is blocked in an I/O operation upon the channel to return
         * immediately, either by throwing an exception or by returning normally.
         * If the channel is {@link SelectableChannel#isRegistered registered}
         * with one or more {@link java.nio.channels.Selector Selector}s then
         * the file descriptor should not be released until the {@link
         * #implReleaseChannel implReleaseChannel} method is invoked. </p>
         *
         * @param  sc
         *         The selectable channel
         *
         * @throws IOException
         *         If an I/O error occurs while closing the file descriptor
         *
         * @see java.nio.channels.spi.AbstractInterruptibleChannel#implCloseChannel
         */
        void implCloseChannel(SelectableChannel sc) throws IOException;

        /**
         * Release the file descriptor and any resources for a selectable
         * channel that closed while registered with one or more {@link
         * java.nio.channels.Selector Selector}s.
         *
         * <p> This method is for cases where a channel is closed when
         * {@link java.nio.channels.SelectableChannel#isRegistered registered}
         * with one or more {@code Selector}s. A channel may remain registered
         * for some time after it is closed. This method is invoked when the
         * channel is eventually deregistered from the last {@code Selector}
         * that it was registered with. It is invoked at most once.
         *
         * @apiNote This method is invoked while synchronized on the selector
         * and its selected-key set. Great care must be taken to avoid deadlocks
         * with other threads that also synchronize on these objects.
         *
         * @param  sc
         *         The closed selectable channel
         *
         * @throws IOException
         *         If an I/O error occurs
         *
         * @see java.nio.channels.spi.AbstractSelector#deregister
         */
        void implReleaseChannel(SelectableChannel sc) throws IOException;
    }

    /**
     * Creates a selectable channel to a file descriptor that supports an
     * {@link SelectableChannel#validOps() operation-set} of
     * {@link SelectionKey#OP_READ OP_READ} and
     * {@link SelectionKey#OP_WRITE OP_WRITE}. The selectable channel will be
     * created by the default {@link SelectorProvider}.
     *
     * <p> The given file descriptor is a socket or resource that can be
     * multiplexed by a {@link java.nio.channels.Selector} for read and write
     * readiness. Great care is required to coordinate direct use of the file
     * descriptor with the use of the selectable channel. In particular,
     * changing the blocking mode or closing the file descriptor without careful
     * coordination will result in unspecified and unsafe side effects. The
     * given {@link SelectableChannelCloser SelectableChannelCloser} is invoked to
     * close the file descriptor and to coordinate the closing when the channel
     * is registered with a {@code Selector}. </p>
     *
     * <p> If there is a security manager set then its
     * {@link SecurityManager#checkRead(FileDescriptor) checkRead} and
     * {@link SecurityManager#checkWrite(FileDescriptor) checkWrite} methods
     * are invoked to check that the caller has permission to both read from and
     * write to the file descriptor. </p>
     *
     * @implNote This method throws {@code UnsupportedOperationException} if
     * the default {@code SelectorProvider} is not the JDK built-in implementation.
     *
     * @param  fd
     *         The file descriptor
     * @param  closer
     *         The object to close the channel
     *
     * @return The selectable channel
     *
     * @throws IllegalArgumentException
     *         If the file descriptor is not {@link FileDescriptor#valid() valid}
     * @throws SecurityException
     *         If denied by the security manager
     */
    public static SelectableChannel readWriteSelectableChannel(FileDescriptor fd,
                                                               SelectableChannelCloser closer) {
        Objects.requireNonNull(closer);
        if (!fd.valid())
            throw new IllegalArgumentException("file descriptor is not valid");

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkRead(fd);
            sm.checkWrite(fd);
        }

        SelectorProvider provider = SelectorProvider.provider();
        if (!(provider instanceof SelectorProviderImpl))
            throw new UnsupportedOperationException("custom SelectorProvider");

        return new ReadWriteChannelImpl((SelectorProviderImpl)provider, fd, closer);
    }

    private static final class ReadWriteChannelImpl
        extends AbstractSelectableChannel implements SelChImpl
    {
        private final FileDescriptor fd;
        private final int fdVal;
        private final SelectableChannelCloser closer;

        ReadWriteChannelImpl(SelectorProviderImpl provider,
                             FileDescriptor fd,
                             SelectableChannelCloser closer) {
            super(provider);
            this.fd = fd;
            this.fdVal = IOUtil.fdVal(fd);
            this.closer = closer;
        }

        @Override
        public FileDescriptor getFD() {
            return fd;
        }

        @Override
        public int getFDVal() {
            return fdVal;
        }

        @Override
        public int validOps() {
            return (SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        }

        private boolean translateReadyOps(int ops,
                                          int initialOps,
                                          SelectionKeyImpl ski) {
            int intOps = ski.nioInterestOps();
            int oldOps = ski.nioReadyOps();
            int newOps = initialOps;

            if ((ops & (Net.POLLERR | Net.POLLHUP)) != 0) {
                newOps = intOps;
                ski.nioReadyOps(newOps);
                return (newOps & ~oldOps) != 0;
            }

            if (((ops & Net.POLLIN) != 0) &&
                    ((intOps & SelectionKey.OP_READ) != 0))
                newOps |= SelectionKey.OP_READ;

            if (((ops & Net.POLLOUT) != 0) &&
                    ((intOps & SelectionKey.OP_WRITE) != 0))
                newOps |= SelectionKey.OP_WRITE;

            ski.nioReadyOps(newOps);
            return (newOps & ~oldOps) != 0;
        }

        @Override
        public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl ski) {
            return translateReadyOps(ops, ski.nioReadyOps(), ski);
        }

        @Override
        public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl ski) {
            return translateReadyOps(ops, 0, ski);
        }

        @Override
        public int translateInterestOps(int ops) {
            int newOps = 0;
            if ((ops & SelectionKey.OP_READ) != 0)
                newOps |= Net.POLLIN;
            if ((ops & SelectionKey.OP_WRITE) != 0)
                newOps |= Net.POLLOUT;
            return newOps;
        }

        @Override
        protected void implConfigureBlocking(boolean block) throws IOException {
            IOUtil.configureBlocking(fd, block);
        }

        @Override
        protected void implCloseSelectableChannel() throws IOException {
            closer.implCloseChannel(this);
        }

        @Override
        public void kill() throws IOException {
            closer.implReleaseChannel(this);
        }
    }
}
