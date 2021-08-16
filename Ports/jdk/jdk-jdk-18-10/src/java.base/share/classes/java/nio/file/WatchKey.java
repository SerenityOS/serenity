/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

/**
 * A token representing the registration of a {@link Watchable watchable} object
 * with a {@link WatchService}.
 *
 * <p> A watch key is created when a watchable object is registered with a watch
 * service. The key remains {@link #isValid valid} until:
 * <ol>
 *   <li> It is cancelled, explicitly, by invoking its {@link #cancel cancel}
 *     method, or</li>
 *   <li> Cancelled implicitly, because the object is no longer accessible,
 *     or </li>
 *   <li> By {@link WatchService#close closing} the watch service. </li>
 * </ol>
 *
 * <p> A watch key has a state. When initially created the key is said to be
 * <em>ready</em>. When an event is detected then the key is <em>signalled</em>
 * and queued so that it can be retrieved by invoking the watch service's {@link
 * WatchService#poll() poll} or {@link WatchService#take() take} methods. Once
 * signalled, a key remains in this state until its {@link #reset reset} method
 * is invoked to return the key to the ready state. Events detected while the
 * key is in the signalled state are queued but do not cause the key to be
 * re-queued for retrieval from the watch service. Events are retrieved by
 * invoking the key's {@link #pollEvents pollEvents} method. This method
 * retrieves and removes all events accumulated for the object. When initially
 * created, a watch key has no pending events. Typically events are retrieved
 * when the key is in the signalled state leading to the following idiom:
 *
 * <pre>
 *     for (;;) {
 *         // retrieve key
 *         WatchKey key = watcher.take();
 *
 *         // process events
 *         for (WatchEvent&lt;?&gt; event: key.pollEvents()) {
 *             :
 *         }
 *
 *         // reset the key
 *         boolean valid = key.reset();
 *         if (!valid) {
 *             // object no longer registered
 *         }
 *     }
 * </pre>
 *
 * <p> Watch keys are safe for use by multiple concurrent threads. Where there
 * are several threads retrieving signalled keys from a watch service then care
 * should be taken to ensure that the {@code reset} method is only invoked after
 * the events for the object have been processed. This ensures that one thread
 * is processing the events for an object at any time.
 *
 * @since 1.7
 */

public interface WatchKey {

    /**
     * Tells whether or not this watch key is valid.
     *
     * <p> A watch key is valid upon creation and remains until it is cancelled,
     * or its watch service is closed.
     *
     * @return  {@code true} if, and only if, this watch key is valid
     */
    boolean isValid();

    /**
     * Retrieves and removes all pending events for this watch key, returning
     * a {@code List} of the events that were retrieved.
     *
     * <p> Note that this method does not wait if there are no events pending.
     *
     * @return  the list of the events retrieved; may be empty
     */
    List<WatchEvent<?>> pollEvents();

    /**
     * Resets this watch key.
     *
     * <p> If this watch key has been cancelled or this watch key is already in
     * the ready state then invoking this method has no effect. Otherwise
     * if there are pending events for the object then this watch key is
     * immediately re-queued to the watch service. If there are no pending
     * events then the watch key is put into the ready state and will remain in
     * that state until an event is detected or the watch key is cancelled.
     *
     * @return  {@code true} if the watch key is valid and has been reset, and
     *          {@code false} if the watch key could not be reset because it is
     *          no longer {@link #isValid valid}
     */
    boolean reset();

    /**
     * Cancels the registration with the watch service. Upon return the watch key
     * will be invalid. If the watch key is enqueued, waiting to be retrieved
     * from the watch service, then it will remain in the queue until it is
     * removed. Pending events, if any, remain pending and may be retrieved by
     * invoking the {@link #pollEvents pollEvents} method after the key is
     * cancelled.
     *
     * <p> If this watch key has already been cancelled then invoking this
     * method has no effect.  Once cancelled, a watch key remains forever invalid.
     */
    void cancel();

    /**
     * Returns the object for which this watch key was created. This method will
     * continue to return the object even after the key is cancelled.
     *
     * <p> As the {@code WatchService} is intended to map directly on to the
     * native file event notification facility (where available) then many of
     * details on how registered objects are watched is highly implementation
     * specific. When watching a directory for changes for example, and the
     * directory is moved or renamed in the file system, there is no guarantee
     * that the watch key will be cancelled and so the object returned by this
     * method may no longer be a valid path to the directory.
     *
     * @return the object for which this watch key was created
     */
    Watchable watchable();
}
