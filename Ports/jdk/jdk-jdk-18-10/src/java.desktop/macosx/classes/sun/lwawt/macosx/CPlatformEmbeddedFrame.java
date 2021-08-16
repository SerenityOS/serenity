/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.lwawt.macosx;

import java.awt.*;
import java.awt.event.FocusEvent;

import sun.awt.CGraphicsDevice;
import sun.java2d.SurfaceData;
import sun.java2d.metal.MTLLayer;
import sun.java2d.opengl.CGLLayer;
import sun.lwawt.macosx.CFLayer;
import sun.lwawt.LWWindowPeer;
import sun.lwawt.PlatformWindow;
import sun.util.logging.PlatformLogger;


/*
 * Provides a lightweight implementation of the EmbeddedFrame.
 */
public class CPlatformEmbeddedFrame implements PlatformWindow {

    private static final PlatformLogger focusLogger = PlatformLogger.getLogger(
            "sun.lwawt.macosx.focus.CPlatformEmbeddedFrame");

    private CFLayer windowLayer;
    private LWWindowPeer peer;
    private CEmbeddedFrame target;

    private volatile int screenX = 0;
    private volatile int screenY = 0;

    @Override // PlatformWindow
    public void initialize(Window target, final LWWindowPeer peer, PlatformWindow owner) {
        this.peer = peer;
        if (CGraphicsDevice.usingMetalPipeline()) {
            this.windowLayer = new MTLLayer(peer);
        } else {
            this.windowLayer = new CGLLayer(peer);
        }
        this.target = (CEmbeddedFrame)target;
    }

    @Override
    public LWWindowPeer getPeer() {
        return peer;
    }

    @Override
    public long getLayerPtr() {
        return windowLayer.getPointer();
    }

    @Override
    public void dispose() {
        windowLayer.dispose();
    }

    @Override
    public void setBounds(int x, int y, int w, int h) {
        // This is a lightweight implementation of the EmbeddedFrame
        // and we simply synthesize a reshape request.
        screenX = x;
        screenY = y;
        peer.notifyReshape(x, y, w, h);
    }

    @Override
    public GraphicsDevice getGraphicsDevice() {
        // REMIND: return the main screen for the initial implementation
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        return ge.getDefaultScreenDevice();
    }

    @Override
    public Point getLocationOnScreen() {
        return new Point(screenX, screenY);
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public SurfaceData getScreenSurface() {
        return windowLayer.getSurfaceData();
    }

    @Override
    public SurfaceData replaceSurfaceData() {
        return windowLayer.replaceSurfaceData();
    }

    @Override
    public void setVisible(boolean visible) {}

    @Override
    public void setTitle(String title) {}

    @Override
    public Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }

    @Override
    public void toFront() {}

    @Override
    public void toBack() {}

    @Override
    public void setMenuBar(MenuBar mb) {}

    @Override
    public void setAlwaysOnTop(boolean value) {}

    @Override
    public void updateFocusableWindowState() {}

    @Override
    public boolean rejectFocusRequest(FocusEvent.Cause cause) {
        // Cross-app activation requests are not allowed.
        if (cause != FocusEvent.Cause.MOUSE_EVENT &&
            !target.isParentWindowActive())
        {
            focusLogger.fine("the embedder is inactive, so the request is rejected");
            return true;
        }
        return false;
    }

    @Override
    public boolean requestWindowFocus() {
        CEmbeddedFrame.updateGlobalFocusedWindow(target);
        target.synthesizeWindowActivation(true);
        return true;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void setResizable(boolean resizable) {}

    @Override
    public void setSizeConstraints(int minW, int minH, int maxW, int maxH) {}

    @Override
    public void updateIconImages() {}

    @Override
    public void setOpacity(float opacity) {}

    @Override
    public void setOpaque(boolean isOpaque) {}

    @Override
    public void enterFullScreenMode() {}

    @Override
    public void exitFullScreenMode() {}

    @Override
    public boolean isFullScreenMode() {
        return false;
    }

    @Override
    public void setWindowState(int windowState) {}

    @Override
    public void setModalBlocked(boolean blocked) {}

    /*
     * The method could not be implemented due to CALayer restrictions.
     * The exeption enforce clients not to use it.
     */
    @Override
    public boolean isUnderMouse() {
        throw new RuntimeException("Not implemented");
    }
}
