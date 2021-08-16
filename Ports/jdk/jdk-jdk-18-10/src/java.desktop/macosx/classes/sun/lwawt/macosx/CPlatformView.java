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

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import sun.awt.CGraphicsEnvironment;
import sun.awt.CGraphicsDevice;
import sun.java2d.metal.MTLLayer;
import sun.lwawt.LWWindowPeer;

import sun.java2d.SurfaceData;
import sun.java2d.opengl.CGLLayer;
import sun.lwawt.macosx.CFLayer;

public class CPlatformView extends CFRetainedResource {
    private native long nativeCreateView(int x, int y, int width, int height, long windowLayerPtr);
    private static native void nativeSetAutoResizable(long awtView, boolean toResize);
    private static native int nativeGetNSViewDisplayID(long awtView);
    private static native Rectangle2D nativeGetLocationOnScreen(long awtView);
    private static native boolean nativeIsViewUnderMouse(long ptr);

    private LWWindowPeer peer;
    private SurfaceData surfaceData;
    private CFLayer windowLayer;
    private CPlatformResponder responder;

    public CPlatformView() {
        super(0, true);
    }

    public void initialize(LWWindowPeer peer, CPlatformResponder responder) {
        initializeBase(peer, responder);

        this.windowLayer = CGraphicsDevice.usingMetalPipeline()? createMTLLayer() : createCGLayer();
        setPtr(nativeCreateView(0, 0, 0, 0, getWindowLayerPtr()));
    }

    public CGLLayer createCGLayer() {
        return new CGLLayer(peer);
    }

    public MTLLayer createMTLLayer() {
        return new MTLLayer(peer);
    }


    protected void initializeBase(LWWindowPeer peer, CPlatformResponder responder) {
        this.peer = peer;
        this.responder = responder;
    }

    public long getAWTView() {
        return ptr;
    }

    /*
     * All coordinates passed to the method should be based on the origin being in the bottom-left corner (standard
     * Cocoa coordinates).
     */
    public void setBounds(int x, int y, int width, int height) {
        execute(ptr->CWrapper.NSView.setFrame(ptr, x, y, width, height));
    }

    // REMIND: CGLSurfaceData expects top-level's size
    public Rectangle getBounds() {
        return peer.getBounds();
    }

    public void setToolTip(String msg) {
        execute(ptr -> CWrapper.NSView.setToolTip(ptr, msg));
    }

    // ----------------------------------------------------------------------
    // PAINTING METHODS
    // ----------------------------------------------------------------------
    public SurfaceData replaceSurfaceData() {
        surfaceData = windowLayer.replaceSurfaceData();
        return surfaceData;
    }

    public SurfaceData getSurfaceData() {
        return surfaceData;
    }

    @Override
    public void dispose() {
        windowLayer.dispose();
        super.dispose();
    }

    public long getWindowLayerPtr() {
        return windowLayer.getPointer();
    }

    public void setAutoResizable(boolean toResize) {
        execute(ptr -> nativeSetAutoResizable(ptr, toResize));
    }

    public boolean isUnderMouse() {
        AtomicBoolean ref = new AtomicBoolean();
        execute(ptr -> {
            ref.set(nativeIsViewUnderMouse(ptr));
        });
        return ref.get();
    }

    public GraphicsDevice getGraphicsDevice() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        CGraphicsEnvironment cge = (CGraphicsEnvironment)ge;
        AtomicInteger ref = new AtomicInteger();
        execute(ptr -> {
            ref.set(nativeGetNSViewDisplayID(ptr));
        });
        GraphicsDevice gd = cge.getScreenDevice(ref.get());
        if (gd == null) {
            // this could possibly happen during device removal
            // use the default screen device in this case
            gd = ge.getDefaultScreenDevice();
        }
        return gd;
    }

    public Point getLocationOnScreen() {
        AtomicReference<Rectangle> ref = new AtomicReference<>();
        execute(ptr -> {
            ref.set(nativeGetLocationOnScreen(ptr).getBounds());
        });
        Rectangle r = ref.get();
        if (r != null) {
            return new Point(r.x, r.y);
        }
        return new Point(0, 0);
    }

    // ----------------------------------------------------------------------
    // NATIVE CALLBACKS
    // ----------------------------------------------------------------------

    /*
     * The callback is called only in the embedded case when the view is
     * automatically resized by the superview.
     * In normal mode this method is never called.
     */
    private void deliverResize(int x, int y, int w, int h) {
        peer.notifyReshape(x, y, w, h);
    }


    private void deliverMouseEvent(final NSEvent event) {
        int x = event.getX();
        int y = getBounds().height - event.getY();
        int absX = event.getAbsX();
        int absY = event.getAbsY();

        if (event.getType() == CocoaConstants.NSScrollWheel) {
            responder.handleScrollEvent(x, y, absX, absY, event.getModifierFlags(),
                                        event.getScrollDeltaX(), event.getScrollDeltaY(),
                                        event.getScrollPhase());
        } else {
            responder.handleMouseEvent(event.getType(), event.getModifierFlags(), event.getButtonNumber(),
                                       event.getClickCount(), x, y,
                                       absX, absY);
        }
    }

    private void deliverKeyEvent(NSEvent event) {
        responder.handleKeyEvent(event.getType(), event.getModifierFlags(), event.getCharacters(),
                                 event.getCharactersIgnoringModifiers(), event.getKeyCode(), true, false);
    }

    /**
     * Called by the native delegate in layer backed view mode or in the simple
     * NSView mode. See NSView.drawRect().
     */
    private void deliverWindowDidExposeEvent() {
        peer.notifyExpose(peer.getSize());
    }
}
