/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.channels.*;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.io.Closeable;
import java.io.IOException;
import java.io.FileDescriptor;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import jdk.internal.misc.Unsafe;

/**
 * Windows implementation of AsynchronousChannelGroup encapsulating an I/O
 * completion port.
 */

class Iocp extends AsynchronousChannelGroupImpl {
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long INVALID_HANDLE_VALUE  = -1L;

    // maps completion key to channel
    private final ReadWriteLock keyToChannelLock = new ReentrantReadWriteLock();
    private final Map<Integer,OverlappedChannel> keyToChannel =
        new HashMap<Integer,OverlappedChannel>();
    private int nextCompletionKey;

    // handle to completion port
    private final long port;

    // true if port has been closed
    private boolean closed;

    // the set of "stale" OVERLAPPED structures. These OVERLAPPED structures
    // relate to I/O operations where the completion notification was not
    // received in a timely manner after the channel is closed.
    private final Set<Long> staleIoSet = new HashSet<Long>();

    Iocp(AsynchronousChannelProvider provider, ThreadPool pool)
        throws IOException
    {
        super(provider, pool);
        this.port =
          createIoCompletionPort(INVALID_HANDLE_VALUE, 0, 0, fixedThreadCount());
        this.nextCompletionKey = 1;
    }

    Iocp start() {
        startThreads(new EventHandlerTask());
        return this;
    }

    /*
     * Channels implements this interface support overlapped I/O and can be
     * associated with a completion port.
     */
    static interface OverlappedChannel extends Closeable {
        /**
         * Returns a reference to the pending I/O result.
         */
        <V,A> PendingFuture<V,A> getByOverlapped(long overlapped);
    }

    // release all resources
    void implClose() {
        synchronized (this) {
            if (closed)
                return;
            closed = true;
        }
        close0(port);
        synchronized (staleIoSet) {
            for (Long ov: staleIoSet) {
                unsafe.freeMemory(ov);
            }
            staleIoSet.clear();
        }
    }

    @Override
    boolean isEmpty() {
        keyToChannelLock.writeLock().lock();
        try {
            return keyToChannel.isEmpty();
        } finally {
            keyToChannelLock.writeLock().unlock();
        }
    }

    @Override
    final Object attachForeignChannel(final Channel channel, FileDescriptor fdObj)
        throws IOException
    {
        int key = associate(new OverlappedChannel() {
            public <V,A> PendingFuture<V,A> getByOverlapped(long overlapped) {
                return null;
            }
            public void close() throws IOException {
                channel.close();
            }
        }, 0L);
        return Integer.valueOf(key);
    }

    @Override
    final void detachForeignChannel(Object key) {
        disassociate((Integer)key);
    }

    @Override
    void closeAllChannels() {
        /**
         * On Windows the close operation will close the socket/file handle
         * and then wait until all outstanding I/O operations have aborted.
         * This is necessary as each channel's cache of OVERLAPPED structures
         * can only be freed once all I/O operations have completed. As I/O
         * completion requires a lookup of the keyToChannel then we must close
         * the channels when not holding the write lock.
         */
        final int MAX_BATCH_SIZE = 32;
        OverlappedChannel channels[] = new OverlappedChannel[MAX_BATCH_SIZE];
        int count;
        do {
            // grab a batch of up to 32 channels
            keyToChannelLock.writeLock().lock();
            count = 0;
            try {
                for (Integer key: keyToChannel.keySet()) {
                    channels[count++] = keyToChannel.get(key);
                    if (count >= MAX_BATCH_SIZE)
                        break;
                }
            } finally {
                keyToChannelLock.writeLock().unlock();
            }

            // close them
            for (int i=0; i<count; i++) {
                try {
                    channels[i].close();
                } catch (IOException ignore) { }
            }
        } while (count > 0);
    }

    private void wakeup() {
        try {
            postQueuedCompletionStatus(port, 0);
        } catch (IOException e) {
            // should not happen
            throw new AssertionError(e);
        }
    }

    @Override
    void executeOnHandlerTask(Runnable task) {
        synchronized (this) {
            if (closed)
                throw new RejectedExecutionException();
            offerTask(task);
            wakeup();
        }

    }

    @Override
    void shutdownHandlerTasks() {
        // shutdown all handler threads
        int nThreads = threadCount();
        while (nThreads-- > 0) {
            wakeup();
        }
    }

    /**
     * Associate the given handle with this group
     */
    int associate(OverlappedChannel ch, long handle) throws IOException {
        keyToChannelLock.writeLock().lock();

        // generate a completion key (if not shutdown)
        int key;
        try {
            if (isShutdown())
                throw new ShutdownChannelGroupException();

            // generate unique key
            do {
                key = nextCompletionKey++;
            } while ((key == 0) || keyToChannel.containsKey(key));

            // associate with I/O completion port
            if (handle != 0L) {
                createIoCompletionPort(handle, port, key, 0);
            }

            // setup mapping
            keyToChannel.put(key, ch);
        } finally {
            keyToChannelLock.writeLock().unlock();
        }
        return key;
    }

    /**
     * Disassociate channel from the group.
     */
    void disassociate(int key) {
        boolean checkForShutdown = false;

        keyToChannelLock.writeLock().lock();
        try {
            keyToChannel.remove(key);

            // last key to be removed so check if group is shutdown
            if (keyToChannel.isEmpty())
                checkForShutdown = true;

        } finally {
            keyToChannelLock.writeLock().unlock();
        }

        // continue shutdown
        if (checkForShutdown && isShutdown()) {
            try {
                shutdownNow();
            } catch (IOException ignore) { }
        }
    }

    /**
     * Invoked when a channel associated with this port is closed before
     * notifications for all outstanding I/O operations have been received.
     */
    void makeStale(Long overlapped) {
        synchronized (staleIoSet) {
            staleIoSet.add(overlapped);
        }
    }

    /**
     * Checks if the given OVERLAPPED is stale and if so, releases it.
     */
    private void checkIfStale(long ov) {
        synchronized (staleIoSet) {
            boolean removed = staleIoSet.remove(ov);
            if (removed) {
                unsafe.freeMemory(ov);
            }
        }
    }

    /**
     * The handler for consuming the result of an asynchronous I/O operation.
     */
    static interface ResultHandler {
        /**
         * Invoked if the I/O operation completes successfully.
         */
        public void completed(int bytesTransferred, boolean canInvokeDirect);

        /**
         * Invoked if the I/O operation fails.
         */
        public void failed(int error, IOException ioe);
    }

    // Creates IOException for the given I/O error.
    private static IOException translateErrorToIOException(int error) {
        String msg = getErrorMessage(error);
        if (msg == null)
            msg = "Unknown error: 0x0" + Integer.toHexString(error);
        return new IOException(msg);
    }

    /**
     * Long-running task servicing system-wide or per-file completion port
     */
    private class EventHandlerTask implements Runnable {
        public void run() {
            Invoker.GroupAndInvokeCount myGroupAndInvokeCount =
                Invoker.getGroupAndInvokeCount();
            boolean canInvokeDirect = (myGroupAndInvokeCount != null);
            CompletionStatus ioResult = new CompletionStatus();
            boolean replaceMe = false;

            try {
                for (;;) {
                    // reset invoke count
                    if (myGroupAndInvokeCount != null)
                        myGroupAndInvokeCount.resetInvokeCount();

                    // wait for I/O completion event
                    // An error here is fatal (thread will not be replaced)
                    replaceMe = false;
                    try {
                        getQueuedCompletionStatus(port, ioResult);
                    } catch (IOException x) {
                        // should not happen
                        x.printStackTrace();
                        return;
                    }

                    // handle wakeup to execute task or shutdown
                    if (ioResult.completionKey() == 0 &&
                        ioResult.overlapped() == 0L)
                    {
                        Runnable task = pollTask();
                        if (task == null) {
                            // shutdown request
                            return;
                        }

                        // run task
                        // (if error/exception then replace thread)
                        replaceMe = true;
                        task.run();
                        continue;
                    }

                    // map key to channel
                    OverlappedChannel ch = null;
                    keyToChannelLock.readLock().lock();
                    try {
                        ch = keyToChannel.get(ioResult.completionKey());
                        if (ch == null) {
                            checkIfStale(ioResult.overlapped());
                            continue;
                        }
                    } finally {
                        keyToChannelLock.readLock().unlock();
                    }

                    // lookup I/O request
                    PendingFuture<?,?> result = ch.getByOverlapped(ioResult.overlapped());
                    if (result == null) {
                        // we get here if the OVERLAPPED structure is associated
                        // with an I/O operation on a channel that was closed
                        // but the I/O operation event wasn't read in a timely
                        // manner. Alternatively, it may be related to a
                        // tryLock operation as the OVERLAPPED structures for
                        // these operations are not in the I/O cache.
                        checkIfStale(ioResult.overlapped());
                        continue;
                    }

                    // synchronize on result in case I/O completed immediately
                    // and was handled by initiator
                    synchronized (result) {
                        if (result.isDone()) {
                            continue;
                        }
                        // not handled by initiator
                    }

                    // invoke I/O result handler
                    int error = ioResult.error();
                    ResultHandler rh = (ResultHandler)result.getContext();
                    replaceMe = true; // (if error/exception then replace thread)
                    if (error == 0) {
                        rh.completed(ioResult.bytesTransferred(), canInvokeDirect);
                    } else {
                        rh.failed(error, translateErrorToIOException(error));
                    }
                }
            } finally {
                // last thread to exit when shutdown releases resources
                int remaining = threadExit(this, replaceMe);
                if (remaining == 0 && isShutdown()) {
                    implClose();
                }
            }
        }
    }

    /**
     * Container for data returned by GetQueuedCompletionStatus
     */
    private static class CompletionStatus {
        private int error;
        private int bytesTransferred;
        private int completionKey;
        private long overlapped;

        private CompletionStatus() { }
        int error() { return error; }
        int bytesTransferred() { return bytesTransferred; }
        int completionKey() { return completionKey; }
        long overlapped() { return overlapped; }
    }

    // -- native methods --

    private static native void initIDs();

    private static native long createIoCompletionPort(long handle,
        long existingPort, int completionKey, int concurrency) throws IOException;

    private static native void close0(long handle);

    private static native void getQueuedCompletionStatus(long completionPort,
        CompletionStatus status) throws IOException;

    private static native void postQueuedCompletionStatus(long completionPort,
        int completionKey) throws IOException;

    private static native String getErrorMessage(int error);

    static {
        IOUtil.load();
        initIDs();
    }
}
