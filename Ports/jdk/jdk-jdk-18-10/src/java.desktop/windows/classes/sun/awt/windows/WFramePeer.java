/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.windows;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.MenuBar;
import java.awt.Rectangle;
import java.awt.peer.FramePeer;
import java.security.AccessController;

import sun.awt.AWTAccessor;
import sun.awt.im.InputMethodManager;
import sun.security.action.GetPropertyAction;

import static sun.java2d.SunGraphicsEnvironment.getGCDeviceBounds;
import static sun.java2d.SunGraphicsEnvironment.toDeviceSpaceAbs;
import static sun.java2d.SunGraphicsEnvironment.toUserSpace;

class WFramePeer extends WWindowPeer implements FramePeer {

    static {
        initIDs();
    }

    // initialize JNI field and method IDs
    private static native void initIDs();

    // FramePeer implementation
    @Override
    public native void setState(int state);
    @Override
    public native int getState();

    // sync target and peer
    public void setExtendedState(int state) {
        AWTAccessor.getFrameAccessor().setExtendedState((Frame)target, state);
    }
    public int getExtendedState() {
        return AWTAccessor.getFrameAccessor().getExtendedState((Frame)target);
    }

    // Convenience methods to save us from trouble of extracting
    // Rectangle fields in native code.
    private native void setMaximizedBounds(int x, int y, int w, int h);
    private native void clearMaximizedBounds();

    @SuppressWarnings("removal")
    private static final boolean keepOnMinimize = "true".equals(
        AccessController.doPrivileged(
            new GetPropertyAction(
            "sun.awt.keepWorkingSetOnMinimize")));

    @Override
    public final void setMaximizedBounds(Rectangle b) {
        if (b == null) {
            clearMaximizedBounds();
        } else {
            b = adjustMaximizedBounds(b);
            setMaximizedBounds(b.x, b.y, b.width, b.height);
        }
    }

    /**
     * The incoming bounds describe the maximized size and position of the
     * window in the virtual coordinate system. But the window manager expects
     * that the bounds are based on the size of the primary monitor and
     * position is based on the actual window monitor, even if the window
     * ultimately maximizes onto a secondary monitor. And the window manager
     * adjusts these values to compensate for differences between the primary
     * monitor and the monitor that displays the window.
     * <p>
     * The method translates the incoming bounds to the values acceptable
     * by the window manager. For more details, please refer to 6699851.
     */
    private Rectangle adjustMaximizedBounds(Rectangle bounds) {
        // All calculations should be done in the device space
        bounds = toDeviceSpaceAbs(bounds);
        GraphicsConfiguration gc = getGraphicsConfiguration();
        Rectangle currentDevBounds = getGCDeviceBounds(gc);
        // Prepare data for WM_GETMINMAXINFO message.
        // ptMaxPosition should be in coordinate system of the current monitor,
        // not the main monitor, or monitor on which we maximize the window.
        bounds.x -= currentDevBounds.x;
        bounds.y -= currentDevBounds.y;
        // ptMaxSize will be used as-is if the size is smaller than the main
        // monitor. If the size is larger than the main monitor then the
        // window manager adjusts the size, like this:
        // result = bounds.w + (current.w - main.w); =>> wrong size
        // We can try to compensate for this adjustment like this:
        // result = bounds.w - (current.w - main.w);
        // but this can result to the size smaller than the main screen, so no
        // adjustment will be done by the window manager =>> wrong size.
        // So we skip compensation here and cut the adjustment on
        // WM_WINDOWPOSCHANGING event.
        // Note that the result does not depend on the monitor on which we
        // maximize the window.
        return bounds;
    }

    @Override
    public boolean updateGraphicsData(GraphicsConfiguration gc) {
        boolean result = super.updateGraphicsData(gc);
        Rectangle bounds = AWTAccessor.getFrameAccessor().
                               getMaximizedBounds((Frame)target);
        if (bounds != null) {
            setMaximizedBounds(bounds);
        }
        return result;
    }

    @Override
    boolean isTargetUndecorated() {
        return ((Frame)target).isUndecorated();
    }

    @Override
    public void reshape(int x, int y, int width, int height) {
        if (((Frame)target).isUndecorated()) {
            super.reshape(x, y, width, height);
        } else {
            reshapeFrame(x, y, width, height);
        }
    }

    @Override
    public final Dimension getMinimumSize() {
        GraphicsConfiguration gc = getGraphicsConfiguration();
        Dimension d = new Dimension();
        if (!((Frame)target).isUndecorated()) {
            d.setSize(toUserSpace(gc, getSysMinWidth(), getSysMinHeight()));
        }
        if (((Frame) target).getMenuBar() != null) {
            d.height += toUserSpace(gc, 0, getSysMenuHeight()).height;
        }
        return d;
    }

    // Note: Because this method calls resize(), which may be overridden
    // by client code, this method must not be executed on the toolkit
    // thread.
    @Override
    public void setMenuBar(MenuBar mb) {
        WMenuBarPeer mbPeer = (WMenuBarPeer) WToolkit.targetToPeer(mb);
        if (mbPeer != null) {
            if (mbPeer.framePeer != this) {
                mb.removeNotify();
                mb.addNotify();
                mbPeer = (WMenuBarPeer) WToolkit.targetToPeer(mb);
                if (mbPeer != null && mbPeer.framePeer != this) {
                    throw new IllegalStateException("Wrong parent peer");
                }
            }
            if (mbPeer != null) {
                addChildPeer(mbPeer);
            }
        }
        setMenuBar0(mbPeer);
        updateInsets(insets_);
    }

    // Note: Because this method calls resize(), which may be overridden
    // by client code, this method must not be executed on the toolkit
    // thread.
    private native void setMenuBar0(WMenuBarPeer mbPeer);

    // Toolkit & peer internals

    WFramePeer(Frame target) {
        super(target);

        InputMethodManager imm = InputMethodManager.getInstance();
        String menuString = imm.getTriggerMenuString();
        if (menuString != null)
        {
          pSetIMMOption(menuString);
        }
    }

    native void createAwtFrame(WComponentPeer parent);
    @Override
    void create(WComponentPeer parent) {
        preCreate(parent);
        createAwtFrame(parent);
    }

    @Override
    void initialize() {
        super.initialize();

        Frame target = (Frame)this.target;

        if (target.getTitle() != null) {
            setTitle(target.getTitle());
        }
        setResizable(target.isResizable());
        setState(target.getExtendedState());
    }

    private static native int getSysMenuHeight();

    native void pSetIMMOption(String option);
    void notifyIMMOptionChange(){
      InputMethodManager.getInstance().notifyChangeRequest((Component)target);
    }

    @Override
    public void setBoundsPrivate(int x, int y, int width, int height) {
        setBounds(x, y, width, height, SET_BOUNDS);
    }
    @Override
    public Rectangle getBoundsPrivate() {
        return getBounds();
    }

    // TODO: implement it in peers. WLightweightFramePeer may implement lw version.
    @Override
    public void emulateActivation(boolean activate) {
        synthesizeWmActivate(activate);
    }

    private native void synthesizeWmActivate(boolean activate);
}
