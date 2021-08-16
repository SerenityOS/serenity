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

package java.nio.file;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * A watch service that <em>watches</em> registered objects for changes and
 * events. For example a file manager may use a watch service to monitor a
 * directory for changes so that it can update its display of the list of files
 * when files are created or deleted.
 *
 * <p> A {@link Watchable} object is registered with a watch service by invoking
 * its {@link Watchable#register register} method, returning a {@link WatchKey}
 * to represent the registration. When an event for an object is detected the
 * key is <em>signalled</em>, and if not currently signalled, it is queued to
 * the watch service so that it can be retrieved by consumers that invoke the
 * {@link #poll() poll} or {@link #take() take} methods to retrieve keys
 * and process events. Once the events have been processed the consumer
 * invokes the key's {@link WatchKey#reset reset} method to reset the key which
 * allows the key to be signalled and re-queued with further events.
 *
 * <p> Registration with a watch service is cancelled by invoking the key's
 * {@link WatchKey#cancel cancel} method. A key that is queued at the time that
 * it is cancelled remains in the queue until it is retrieved. Depending on the
 * object, a key may be cancelled automatically. For example, suppose a
 * directory is watched and the watch service detects that it has been deleted
 * or its file system is no longer accessible. When a key is cancelled in this
 * manner it is signalled and queued, if not currently signalled. To ensure
 * that the consumer is notified the return value from the {@code reset}
 * method indicates if the key is valid.
 *
 * <p> A watch service is safe for use by multiple concurrent consumers. To
 * ensure that only one consumer processes the events for a particular object at
 * any time then care should be taken to ensure that the key's {@code reset}
 * method is only invoked after its events have been processed. The {@link
 * #close close} method may be invoked at any time to close the service causing
 * any threads waiting to retrieve keys, to throw {@code
 * ClosedWatchServiceException}.
 *
 * <p> File systems may report events faster than they can be retrieved or
 * processed and an implementation may impose an unspecified limit on the number
 * of events that it may accumulate. Where an implementation <em>knowingly</em>
 * discards events then it arranges for the key's {@link WatchKey#pollEvents
 * pollEvents} method to return an element with an event type of {@link
 * StandardWatchEventKinds#OVERFLOW OVERFLOW}. This event can be used by the
 * consumer as a trigger to re-examine the state of the object.
 *
 * <p> When an event is reported to indicate that a file in a watched directory
 * has been modified then there is no guarantee that the program (or programs)
 * that have modified the file have completed. Care should be taken to coordinate
 * access with other programs that may be updating the file.
 * The {@link java.nio.channels.FileChannel FileChannel} class defines methods
 * to lock regions of a file against access by other programs.
 *
 * <h2>Platform dependencies</h2>
 *
 * <p> The implementation that observes events from the file system is intended
 * to map directly on to the native file event notification facility where
 * available, or to use a primitive mechanism, such as polling, when a native
 * facility is not available. Consequently, many of the details on how events
 * are detected, their timeliness, and whether their ordering is preserved are
 * highly implementation specific. For example, when a file in a watched
 * directory is modified then it may result in a single {@link
 * StandardWatchEventKinds#ENTRY_MODIFY ENTRY_MODIFY} event in some
 * implementations but several events in other implementations. Short-lived
 * files (meaning files that are deleted very quickly after they are created)
 * may not be detected by primitive implementations that periodically poll the
 * file system to detect changes.
 *
 * <p> If a watched file is not located on a local storage device then it is
 * implementation specific if changes to the file can be detected. In particular,
 * it is not required that changes to files carried out on remote systems be
 * detected.
 *
 * @since 1.7
 *
 * @see FileSystem#newWatchService
 */

public interface WatchService
    extends Closeable
{

    /**
     * Closes this watch service.
     *
     * <p> If a thread is currently blocked in the {@link #take take} or {@link
     * #poll(long,TimeUnit) poll} methods waiting for a key to be queued then
     * it immediately receives a {@link ClosedWatchServiceException}. Any
     * valid keys associated with this watch service are {@link WatchKey#isValid
     * invalidated}.
     *
     * <p> After a watch service is closed, any further attempt to invoke
     * operations upon it will throw {@link ClosedWatchServiceException}.
     * If this watch service is already closed then invoking this method
     * has no effect.
     *
     * @throws  IOException
     *          if an I/O error occurs
     */
    @Override
    void close() throws IOException;

    /**
     * Retrieves and removes the next watch key, or {@code null} if none are
     * present.
     *
     * @return  the next watch key, or {@code null}
     *
     * @throws  ClosedWatchServiceException
     *          if this watch service is closed
     */
    WatchKey poll();

    /**
     * Retrieves and removes the next watch key, waiting if necessary up to the
     * specified wait time if none are yet present.
     *
     * @param   timeout
     *          how to wait before giving up, in units of unit
     * @param   unit
     *          a {@code TimeUnit} determining how to interpret the timeout
     *          parameter
     *
     * @return  the next watch key, or {@code null}
     *
     * @throws  ClosedWatchServiceException
     *          if this watch service is closed, or it is closed while waiting
     *          for the next key
     * @throws  InterruptedException
     *          if interrupted while waiting
     */
    WatchKey poll(long timeout, TimeUnit unit)
        throws InterruptedException;

    /**
     * Retrieves and removes next watch key, waiting if none are yet present.
     *
     * @return  the next watch key
     *
     * @throws  ClosedWatchServiceException
     *          if this watch service is closed, or it is closed while waiting
     *          for the next key
     * @throws  InterruptedException
     *          if interrupted while waiting
     */
    WatchKey take() throws InterruptedException;
}
