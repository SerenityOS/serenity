/*
 * Copyright (c) 2008, 2011, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.fs;

import java.nio.file.*;
import java.util.concurrent.*;
import java.io.IOException;

/**
 * Base implementation class for watch services.
 */

abstract class AbstractWatchService implements WatchService {

    // signaled keys waiting to be dequeued
    private final LinkedBlockingDeque<WatchKey> pendingKeys =
        new LinkedBlockingDeque<WatchKey>();

    // special key to indicate that watch service is closed
    private final WatchKey CLOSE_KEY =
        new AbstractWatchKey(null, null) {
            @Override
            public boolean isValid() {
                return true;
            }

            @Override
            public void cancel() {
            }
        };

    // used when closing watch service
    private volatile boolean closed;
    private final Object closeLock = new Object();

    protected AbstractWatchService() {
    }

    /**
     * Register the given object with this watch service
     */
    abstract WatchKey register(Path path,
                               WatchEvent.Kind<?>[] events,
                               WatchEvent.Modifier... modifers)
        throws IOException;

    // used by AbstractWatchKey to enqueue key
    final void enqueueKey(WatchKey key) {
        pendingKeys.offer(key);
    }

    /**
     * Throws ClosedWatchServiceException if watch service is closed
     */
    private void checkOpen() {
        if (closed)
            throw new ClosedWatchServiceException();
    }

    /**
     * Checks the key isn't the special CLOSE_KEY used to unblock threads when
     * the watch service is closed.
     */
    private void checkKey(WatchKey key) {
        if (key == CLOSE_KEY) {
            // re-queue in case there are other threads blocked in take/poll
            enqueueKey(key);
        }
        checkOpen();
    }

    @Override
    public final WatchKey poll() {
        checkOpen();
        WatchKey key = pendingKeys.poll();
        checkKey(key);
        return key;
    }

    @Override
    public final WatchKey poll(long timeout, TimeUnit unit)
        throws InterruptedException
    {
        checkOpen();
        WatchKey key = pendingKeys.poll(timeout, unit);
        checkKey(key);
        return key;
    }

    @Override
    public final WatchKey take()
        throws InterruptedException
    {
        checkOpen();
        WatchKey key = pendingKeys.take();
        checkKey(key);
        return key;
    }

    /**
     * Tells whether or not this watch service is open.
     */
    final boolean isOpen() {
        return !closed;
    }

    /**
     * Retrieves the object upon which the close method synchronizes.
     */
    final Object closeLock() {
        return closeLock;
    }

    /**
     * Closes this watch service. This method is invoked by the close
     * method to perform the actual work of closing the watch service.
     */
    abstract void implClose() throws IOException;

    @Override
    public final void close()
        throws IOException
    {
        synchronized (closeLock) {
            // nothing to do if already closed
            if (closed)
                return;
            closed = true;

            implClose();

            // clear pending keys and queue special key to ensure that any
            // threads blocked in take/poll wakeup
            pendingKeys.clear();
            pendingKeys.offer(CLOSE_KEY);
        }
    }
}
