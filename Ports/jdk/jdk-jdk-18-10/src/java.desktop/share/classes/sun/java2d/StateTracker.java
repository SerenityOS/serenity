/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d;

/**
 * This interface is used to track changes to the complex data of an
 * object that implements the StateTrackable interface.
 * <p>
 * The usage pattern for code accessing the trackable data is as follows:
 * <pre>
 *     StateTrackable trackedobject;
 *     MyInfo cacheddata;
 *     StateTracker cachetracker;
 *     public synchronized MyInfo getInfoAbout(StateTrackable obj) {
 *         if (trackedobject != obj || !cachetracker.isCurrent()) {
 *             // Note: Always call getStateTracker() before
 *             // caching any data about the objct...
 *             cachetracker = obj.getStateTracker();
 *             cacheddata = calculateInfoFrom(obj);
 *             trackedobject = obj;
 *         }
 *         return cacheddata;
 *     }
 * </pre>
 * Note that the sample code above works correctly regardless of the
 * {@link StateTrackable.State State} of the complex data of the object,
 * but it may be inefficient to store precalculated information about
 * an object whose current {@link StateTrackable.State State} is
 * {@link StateTrackable.State#UNTRACKABLE UNTRACKABLE}
 * and it is unnecessary to perform the {@link #isCurrent} test for
 * data whose current {@link StateTrackable.State State} is
 * {@link StateTrackable.State#IMMUTABLE IMMUTABLE}.
 * Optimizations to the sample code for either or both of those terminal
 * States may be of benefit for some use cases, but is left out of the
 * example to reduce its complexity.
 *
 * @see StateTrackable.State
 * @since 1.7
 */
public interface StateTracker {
    /**
     * An implementation of the StateTracker interface which
     * always returns true.
     * This implementation is useful for objects whose current
     * {@link StateTrackable.State State} is
     * {@link StateTrackable.State#IMMUTABLE IMMUTABLE}.
     * @since 1.7
     */
    public StateTracker ALWAYS_CURRENT = new StateTracker() {
        public boolean isCurrent() {
            return true;
        }
    };

    /**
     * An implementation of the StateTracker interface which
     * always returns false.
     * This implementation is useful for objects whose current
     * {@link StateTrackable.State State} is
     * {@link StateTrackable.State#UNTRACKABLE UNTRACKABLE}.
     * This implementation may also be useful for some objects
     * whose current {@link StateTrackable.State State} is
     * {@link StateTrackable.State#DYNAMIC DYNAMIC}.
     * @since 1.7
     */
    public StateTracker NEVER_CURRENT = new StateTracker() {
        public boolean isCurrent() {
            return false;
        }
    };

    /**
     * Returns true iff the contents of the complex data of the
     * associated StateTrackable object have not changed since
     * the time that this StateTracker was returned from its
     * getStateTracker() method.
     * @see StateTrackable
     * @since 1.7
     */
    public boolean isCurrent();
}
