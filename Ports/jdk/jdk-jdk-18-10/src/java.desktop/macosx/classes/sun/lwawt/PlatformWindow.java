/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.event.FocusEvent;

import sun.java2d.SurfaceData;

// TODO Is it worth to generify this interface, like that:
//
// public interface PlatformWindow<WindowType extends Window>
//
// ?

public interface PlatformWindow {

    /*
     * Delegate initialization (create native window and all the
     * related resources).
     */
    public void initialize(Window target, LWWindowPeer peer, PlatformWindow owner);

    /*
     * Delegate shutdown (dispose native window and all the
     * related resources).
     */
    public void dispose();

    /*
     * Shows or hides the window.
     */
    public void setVisible(boolean visible);

    /*
     * Sets the window title
     */
    public void setTitle(String title);

    /*
     * Sets the window bounds. Called when user changes window bounds
     * with setSize/setLocation/setBounds/reshape methods.
     */
    public void setBounds(int x, int y, int w, int h);

    /*
     * Sets the maximized bounds.
     */
    public default void setMaximizedBounds(int x, int y, int w, int h){}

    /*
     * Returns the graphics device where the window is.
     */
    public GraphicsDevice getGraphicsDevice();

    /*
     * Returns the location of the window.
     */
    public Point getLocationOnScreen();

    /*
     * Returns the window insets.
     */
    public Insets getInsets();

    /*
     * Returns the metrics for a given font.
     */
    public FontMetrics getFontMetrics(Font f);

    /*
     * Get the SurfaceData for the window.
     */
    public SurfaceData getScreenSurface();

    /*
     * Revalidates the window's current SurfaceData and returns
     * the newly created one.
     */
    public SurfaceData replaceSurfaceData();

    public void setModalBlocked(boolean blocked);

    public void toFront();

    public void toBack();

    public void setMenuBar(MenuBar mb);

    public void setAlwaysOnTop(boolean value);

    public void updateFocusableWindowState();

    public boolean rejectFocusRequest(FocusEvent.Cause cause);

    public boolean requestWindowFocus();

    /*
     * Returns true only when called on a frame/dialog when it's natively focused.
     */
    public boolean isActive();

    public void setResizable(boolean resizable);

    /**
     * Applies the minimum and maximum size to the platform window.
     */
    public void setSizeConstraints(int minW, int minH, int maxW, int maxH);

    /*
     * Installs the images for particular window.
     */
    public void updateIconImages();

    public void setOpacity(float opacity);

    public void setOpaque(boolean isOpaque);

    public void enterFullScreenMode();

    public void exitFullScreenMode();

    public boolean isFullScreenMode();

    public void setWindowState(int windowState);

    public long getLayerPtr();

    public LWWindowPeer getPeer();

    public boolean isUnderMouse();
}
