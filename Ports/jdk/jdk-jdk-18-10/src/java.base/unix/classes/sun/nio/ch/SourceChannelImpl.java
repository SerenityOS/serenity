/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.SelectorProvider;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

class SourceChannelImpl
    extends Pipe.SourceChannel
    implements SelChImpl
{
    // Used to make native read and write calls
    private static final NativeDispatcher nd = new FileDispatcherImpl();

    // The file descriptor associated with this channel
    private final FileDescriptor fd;
    private final int fdVal;

    // Lock held by current reading thread
    private final ReentrantLock readLock = new ReentrantLock();

    // Lock held by any thread that modifies the state fields declared below
    // DO NOT invoke a blocking I/O operation while holding this lock!
    private final Object stateLock = new Object();

    // -- The following fields are protected by stateLock

    // Channel state
    private static final int ST_INUSE = 0;
    private static final int ST_CLOSING = 1;
    private static final int ST_CLOSED = 2;
    private int state;

    // ID of native thread doing read, for signalling
    private long thread;

    // -- End of fields protected by stateLock


    public FileDescriptor getFD() {
        return fd;
    }

    public int getFDVal() {
        return fdVal;
    }

    SourceChannelImpl(SelectorProvider sp, FileDescriptor fd) {
        super(sp);
        this.fd = fd;
        this.fdVal = IOUtil.fdVal(fd);
    }

    /**
     * Closes the read end of the pipe if there are no read operation in
     * progress and the channel is not registered with a Selector.
     */
    private boolean tryClose() throws IOException {
        assert Thread.holdsLock(stateLock) && state == ST_CLOSING;
        if (thread == 0 && !isRegistered()) {
            state = ST_CLOSED;
            nd.close(fd);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Invokes tryClose to attempt to close the read end of the pipe.
     *
     * This method is used for deferred closing by I/O and Selector operations.
     */
    private void tryFinishClose() {
        try {
            tryClose();
        } catch (IOException ignore) { }
    }

    /**
     * Closes this channel when configured in blocking mode.
     *
     * If there is a read operation in progress then the read-end of the pipe
     * is pre-closed and the reader is signalled, in which case the final close
     * is deferred until the reader aborts.
     */
    private void implCloseBlockingMode() throws IOException {
        synchronized (stateLock) {
            assert state < ST_CLOSING;
            state = ST_CLOSING;
            if (!tryClose()) {
                long th = thread;
                if (th != 0) {
                    nd.preClose(fd);
                    NativeThread.signal(th);
                }
            }
        }
    }

    /**
     * Closes this channel when configured in non-blocking mode.
     *
     * If the channel is registered with a Selector then the close is deferred
     * until the channel is flushed from all Selectors.
     */
    private void implCloseNonBlockingMode() throws IOException {
        synchronized (stateLock) {
            assert state < ST_CLOSING;
            state = ST_CLOSING;
        }
        // wait for any read operation to complete before trying to close
        readLock.lock();
        readLock.unlock();
        synchronized (stateLock) {
            if (state == ST_CLOSING) {
                tryClose();
            }
        }
    }

    /**
     * Invoked by implCloseChannel to close the channel.
     */
    @Override
    protected void implCloseSelectableChannel() throws IOException {
        assert !isOpen();
        if (isBlocking()) {
            implCloseBlockingMode();
        } else {
            implCloseNonBlockingMode();
        }
    }
    @Override
    public void kill() {
        synchronized (stateLock) {
            assert !isOpen();
            if (state == ST_CLOSING) {
                tryFinishClose();
            }
        }
    }

    @Override
    protected void implConfigureBlocking(boolean block) throws IOException {
        readLock.lock();
        try {
            synchronized (stateLock) {
                if (!isOpen())
                    throw new ClosedChannelException();
                IOUtil.configureBlocking(fd, block);
            }
        } finally {
            readLock.unlock();
        }
    }

    public boolean translateReadyOps(int ops, int initialOps, SelectionKeyImpl ski) {
        int intOps = ski.nioInterestOps();
        int oldOps = ski.nioReadyOps();
        int newOps = initialOps;

        if ((ops & Net.POLLNVAL) != 0)
            throw new Error("POLLNVAL detected");

        if ((ops & (Net.POLLERR | Net.POLLHUP)) != 0) {
            newOps = intOps;
            ski.nioReadyOps(newOps);
            return (newOps & ~oldOps) != 0;
        }

        if (((ops & Net.POLLIN) != 0) &&
            ((intOps & SelectionKey.OP_READ) != 0))
            newOps |= SelectionKey.OP_READ;

        ski.nioReadyOps(newOps);
        return (newOps & ~oldOps) != 0;
    }

    public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl ski) {
        return translateReadyOps(ops, ski.nioReadyOps(), ski);
    }

    public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl ski) {
        return translateReadyOps(ops, 0, ski);
    }

    public int translateInterestOps(int ops) {
        int newOps = 0;
        if (ops == SelectionKey.OP_READ)
            newOps |= Net.POLLIN;
        return newOps;
    }

    /**
     * Marks the beginning of a read operation that might block.
     *
     * @throws ClosedChannelException if the channel is closed
     * @throws NotYetConnectedException if the channel is not yet connected
     */
    private void beginRead(boolean blocking) throws ClosedChannelException {
        if (blocking) {
            // set hook for Thread.interrupt
            begin();
        }
        synchronized (stateLock) {
            if (!isOpen())
                throw new ClosedChannelException();
            if (blocking)
                thread = NativeThread.current();
        }
    }

    /**
     * Marks the end of a read operation that may have blocked.
     *
     * @throws AsynchronousCloseException if the channel was closed due to this
     * thread being interrupted on a blocking read operation.
     */
    private void endRead(boolean blocking, boolean completed)
        throws AsynchronousCloseException
    {
        if (blocking) {
            synchronized (stateLock) {
                thread = 0;
                if (state == ST_CLOSING) {
                    tryFinishClose();
                }
            }
            // remove hook for Thread.interrupt
            end(completed);
        }
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        Objects.requireNonNull(dst);

        readLock.lock();
        try {
            boolean blocking = isBlocking();
            int n = 0;
            try {
                beginRead(blocking);
                n = IOUtil.read(fd, dst, -1, nd);
                if (blocking) {
                    while (IOStatus.okayToRetry(n) && isOpen()) {
                        park(Net.POLLIN);
                        n = IOUtil.read(fd, dst, -1, nd);
                    }
                }
            } finally {
                endRead(blocking, n > 0);
                assert IOStatus.check(n);
            }
            return IOStatus.normalize(n);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, dsts.length);

        readLock.lock();
        try {
            boolean blocking = isBlocking();
            long n = 0;
            try {
                beginRead(blocking);
                n = IOUtil.read(fd, dsts, offset, length, nd);
                if (blocking) {
                    while (IOStatus.okayToRetry(n) && isOpen()) {
                        park(Net.POLLIN);
                        n = IOUtil.read(fd, dsts, offset, length, nd);
                    }
                }
            } finally {
                endRead(blocking, n > 0);
                assert IOStatus.check(n);
            }
            return IOStatus.normalize(n);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public long read(ByteBuffer[] dsts) throws IOException {
        return read(dsts, 0, dsts.length);
    }
}
