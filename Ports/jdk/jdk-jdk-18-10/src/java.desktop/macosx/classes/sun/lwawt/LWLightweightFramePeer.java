/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.dnd.DropTarget;
import java.awt.event.FocusEvent;

import sun.awt.LightweightFrame;
import sun.awt.OverrideNativeWindowHandle;
import sun.swing.JLightweightFrame;
import sun.swing.SwingAccessor;

public class LWLightweightFramePeer extends LWWindowPeer implements OverrideNativeWindowHandle {

    public LWLightweightFramePeer(LightweightFrame target,
                                  PlatformComponent platformComponent,
                                  PlatformWindow platformWindow)
    {
        super(target, platformComponent, platformWindow, LWWindowPeer.PeerType.LW_FRAME);
    }

    private LightweightFrame getLwTarget() {
        return (LightweightFrame)getTarget();
    }

    @Override
    public Graphics getGraphics() {
        return getLwTarget().getGraphics();
    }

    @Override
    protected void setVisibleImpl(final boolean visible) {
    }

    @Override
    public boolean requestWindowFocus(FocusEvent.Cause cause) {
        if (!focusAllowedFor()) {
            return false;
        }
        if (getPlatformWindow().rejectFocusRequest(cause)) {
            return false;
        }

        Window opposite = LWKeyboardFocusManagerPeer.getInstance().
            getCurrentFocusedWindow();

        changeFocusedWindow(true, opposite);

        return true;
    }

    @Override
    public Point getLocationOnScreen() {
        Rectangle bounds = getBounds();
        return new Point(bounds.x, bounds.y); // todo
    }

    @Override
    public Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }

    @Override
    public void setBounds(int x, int y, int w, int h, int op) {
        setBounds(x, y, w, h, op, true, true);
    }

    @Override
    public void addDropTarget(DropTarget dt) {
        getLwTarget().addDropTarget(dt);
    }

    @Override
    public void removeDropTarget(DropTarget dt) {
        getLwTarget().removeDropTarget(dt);
    }

    @Override
    public void grab() {
        getLwTarget().grabFocus();
    }

    @Override
    public void ungrab() {
        getLwTarget().ungrabFocus();
    }

    @Override
    public void updateCursorImmediately() {
        SwingAccessor.getJLightweightFrameAccessor().updateCursor((JLightweightFrame)getLwTarget());
    }

    // SwingNode
    private volatile long overriddenWindowHandle = 0L;

    @Override
    public void overrideWindowHandle(final long handle) {
        this.overriddenWindowHandle = handle;
    }

    public long getOverriddenWindowHandle() {
        return overriddenWindowHandle;
    }
}
