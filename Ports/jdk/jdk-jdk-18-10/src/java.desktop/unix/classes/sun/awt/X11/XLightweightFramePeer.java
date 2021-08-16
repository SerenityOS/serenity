/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.X11;

import java.awt.Graphics;
import java.awt.dnd.DropTarget;

import sun.awt.LightweightFrame;
import sun.awt.OverrideNativeWindowHandle;
import sun.swing.JLightweightFrame;
import sun.swing.SwingAccessor;

public class XLightweightFramePeer extends XFramePeer implements OverrideNativeWindowHandle {

    XLightweightFramePeer(LightweightFrame target) {
        super(target);
    }

    private LightweightFrame getLwTarget() {
        return (LightweightFrame)target;
    }

    @Override
    public Graphics getGraphics() {
        return getLwTarget().getGraphics();
    }

    @Override
    public void xSetVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    protected void requestXFocus(long time, boolean timeProvided) {
        // not sending native focus events to the proxy
    }

    @Override
    public void setGrab(boolean grab) {
        if (grab) {
            getLwTarget().grabFocus();
        } else {
            getLwTarget().ungrabFocus();
        }
    }

    @Override
    public void updateCursorImmediately() {
        SwingAccessor.getJLightweightFrameAccessor().updateCursor((JLightweightFrame)getLwTarget());
    }

    @Override
    public void addDropTarget(DropTarget dt) {
        getLwTarget().addDropTarget(dt);
    }

    @Override
    public void removeDropTarget(DropTarget dt) {
        getLwTarget().removeDropTarget(dt);
    }

    private volatile long overriddenWindowHandle = 0L;

    @Override
    public void overrideWindowHandle(final long handle) {
        overriddenWindowHandle = handle;
    }

    public long getOverriddenWindowHandle() {
        return overriddenWindowHandle;
    }
}
