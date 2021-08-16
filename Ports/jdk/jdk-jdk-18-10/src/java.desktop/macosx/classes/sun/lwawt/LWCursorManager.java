/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.lwawt;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Point;

import java.util.concurrent.atomic.AtomicBoolean;

import sun.awt.AWTAccessor;
import sun.awt.SunToolkit;

public abstract class LWCursorManager {

    /**
     * A flag to indicate if the update is scheduled, so we don't process it
     * twice.
     */
    private final AtomicBoolean updatePending = new AtomicBoolean(false);

    protected LWCursorManager() {
    }

    /**
     * Sets the cursor to correspond the component currently under mouse.
     *
     * This method should not be executed on the toolkit thread as it
     * calls to user code (e.g. Container.findComponentAt).
     */
    public final void updateCursor() {
        updatePending.set(false);
        updateCursorImpl();
    }

    /**
     * Schedules updating the cursor on the corresponding event dispatch
     * thread for the given window.
     *
     * This method is called on the toolkit thread as a result of a
     * native update cursor request (e.g. WM_SETCURSOR on Windows).
     */
    public final void updateCursorLater(final LWWindowPeer window) {
        if (updatePending.compareAndSet(false, true)) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    updateCursor();
                }
            };
            SunToolkit.executeOnEventHandlerThread(window.getTarget(), r);
        }
    }

    private void updateCursorImpl() {
        final Point cursorPos = getCursorPosition();
        final Component c = findComponent(cursorPos);
        final Cursor cursor;
        final Object peer = LWToolkit.targetToPeer(c);
        if (peer instanceof LWComponentPeer) {
            final LWComponentPeer<?, ?> lwpeer = (LWComponentPeer<?, ?>) peer;
            final Point p = lwpeer.getLocationOnScreen();
            cursor = lwpeer.getCursor(new Point(cursorPos.x - p.x,
                                                cursorPos.y - p.y));
        } else {
            cursor = (c != null) ? c.getCursor() : null;
        }
        setCursor(cursor);
    }

    /**
     * Returns the first visible, enabled and showing component under cursor.
     * Returns null for modal blocked windows.
     *
     * @param cursorPos Current cursor position.
     * @return Component or null.
     */
    private static final Component findComponent(final Point cursorPos) {
        final LWComponentPeer<?, ?> peer = LWWindowPeer.getPeerUnderCursor();
        Component c = null;
        if (peer != null && peer.getWindowPeerOrSelf().getBlocker() == null) {
            c = peer.getTarget();
            if (c instanceof Container) {
                final Point p = peer.getLocationOnScreen();
                c = AWTAccessor.getContainerAccessor().findComponentAt(
                    (Container) c, cursorPos.x - p.x, cursorPos.y - p.y, false);

            }
            while (c != null) {
                final Object p = AWTAccessor.getComponentAccessor().getPeer(c);
                if (c.isVisible() && c.isEnabled() && p != null) {
                    break;
                }
                c = c.getParent();
            }
        }
        return c;
    }

    /**
     * Returns the current cursor position.
     */
    // TODO: make it public to reuse for MouseInfo
    protected abstract Point getCursorPosition();

    /**
     * Sets a cursor. The cursor can be null if the mouse is not over a Java
     * window.
     * @param cursor the new {@code Cursor}.
     */
    protected abstract void setCursor(Cursor cursor);
}
