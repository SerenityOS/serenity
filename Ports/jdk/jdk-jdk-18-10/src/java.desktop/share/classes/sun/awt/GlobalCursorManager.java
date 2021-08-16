/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.InvocationEvent;

/**
 * A stateless class which responds to native mouse moves, Component resizes,
 * Component moves, showing and hiding of Components, minimizing and
 * maximizing of top level Windows, addition and removal of Components,
 * and calls to setCursor().
 */
public abstract class GlobalCursorManager {

    class NativeUpdater implements Runnable {
        boolean pending = false;

        public void run() {
            boolean shouldUpdate = false;
            synchronized (this) {
                if (pending) {
                    pending = false;
                    shouldUpdate = true;
                }
            }
            if (shouldUpdate) {
                _updateCursor(false);
            }
        }

        public void postIfNotPending(Component heavy, InvocationEvent in) {
            boolean shouldPost = false;
            synchronized (this) {
                if (!pending) {
                    pending = shouldPost = true;
                }
            }
            if (shouldPost) {
                SunToolkit.postEvent(SunToolkit.targetToAppContext(heavy), in);
            }
        }
    }

    /**
     * Use a singleton NativeUpdater for better performance. We cannot use
     * a singleton InvocationEvent because we want each event to have a fresh
     * timestamp.
     */
    private final NativeUpdater nativeUpdater = new NativeUpdater();

    /**
     * The last time the cursor was updated, in milliseconds.
     */
    private long lastUpdateMillis;

    /**
     * Locking object for synchronizing access to lastUpdateMillis. The VM
     * does not guarantee atomicity of longs.
     */
    private final Object lastUpdateLock = new Object();

    /**
     * Should be called for any activity at the Java level which may affect
     * the global cursor, except for Java MOUSE_MOVED events.
     */
    public void updateCursorImmediately() {
        synchronized (nativeUpdater) {
            nativeUpdater.pending = false;
        }
        _updateCursor(false);
    }

    /**
     * Should be called in response to Java MOUSE_MOVED events. The update
     * will be discarded if the InputEvent is outdated.
     *
     * @param   e the InputEvent which triggered the cursor update.
     */
    public void updateCursorImmediately(InputEvent e) {
        boolean shouldUpdate;
        synchronized (lastUpdateLock) {
            shouldUpdate = (e.getWhen() >= lastUpdateMillis);
        }
        if (shouldUpdate) {
            _updateCursor(true);
        }
    }

    /**
     * Should be called in response to a native mouse enter or native mouse
     * button released message. Should not be called during a mouse drag.
     */
    public void updateCursorLater(Component heavy) {
        nativeUpdater.postIfNotPending(heavy, new InvocationEvent
            (Toolkit.getDefaultToolkit(), nativeUpdater));
    }

    protected GlobalCursorManager() { }

    /**
     * Set the global cursor to the specified cursor. The component over
     * which the Cursor current resides is provided as a convenience. Not
     * all platforms may require the Component.
     */
    protected abstract void setCursor(Component comp, Cursor cursor,
                                      boolean useCache);
    /**
     * Returns the global cursor position, in screen coordinates.
     */
    protected abstract void getCursorPos(Point p);

    protected abstract Point getLocationOnScreen(Component com);

    /**
     * Returns the most specific, visible, heavyweight Component
     * under the cursor. This method should return null iff the cursor is
     * not over any Java Window.
     *
     * @param   useCache If true, the implementation is free to use caching
     * mechanisms because the Z-order, visibility, and enabled state of the
     * Components has not changed. If false, the implementation should not
     * make these assumptions.
     */
    protected abstract Component findHeavyweightUnderCursor(boolean useCache);

    /**
     * Updates the global cursor. We apply a three-step scheme to cursor
     * updates:<p>
     *
     * (1) InputEvent updates which are outdated are discarded by
     * {@code updateCursorImmediately(InputEvent)}.<p>
     *
     * (2) If 'useCache' is true, the native code is free to use a cached
     * value to determine the most specific, visible, enabled heavyweight
     * because this update is occurring in response to a mouse move. If
     * 'useCache' is false, the native code must perform a new search given
     * the current mouse coordinates.
     *
     * (3) Once we have determined the most specific, visible, enabled
     * heavyweight, we use findComponentAt to find the most specific, visible,
     * enabled Component.
     */
    private void _updateCursor(boolean useCache) {

        synchronized (lastUpdateLock) {
            lastUpdateMillis = System.currentTimeMillis();
        }

        Point queryPos = null, p = null;
        Component comp;

        try {
            comp = findHeavyweightUnderCursor(useCache);
            if (comp == null) {
                updateCursorOutOfJava();
                return;
            }

            if (comp instanceof Window) {
                p = AWTAccessor.getComponentAccessor().getLocation(comp);
            } else if (comp instanceof Container) {
                p = getLocationOnScreen(comp);
            }
            if (p != null) {
                queryPos = new Point();
                getCursorPos(queryPos);
                Component c = AWTAccessor.getContainerAccessor().
                        findComponentAt((Container) comp,
                        queryPos.x - p.x, queryPos.y - p.y, false);

                // If findComponentAt returns null, then something bad has
                // happened. For example, the heavyweight Component may
                // have been hidden or disabled by another thread. In that
                // case, we'll just use the originial heavyweight.
                if (c != null) {
                    comp = c;
                }
            }

            setCursor(comp, AWTAccessor.getComponentAccessor().getCursor(comp), useCache);

        } catch (IllegalComponentStateException e) {
            // Shouldn't happen, but if it does, abort.
        }
    }

    protected void updateCursorOutOfJava() {
        // Cursor is not over a Java Window. Do nothing...usually
        // But we need to update it in case of grab on X.
    }
}
