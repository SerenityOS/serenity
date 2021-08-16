/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MenuBar;
import java.awt.MenuComponent;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.dnd.peer.DragSourceContextPeer;
import java.awt.peer.FramePeer;

/**
 * The class provides basic functionality for a lightweight frame
 * implementation. A subclass is expected to provide painting to an
 * offscreen image and access to it. Thus it can be used for lightweight
 * embedding.
 *
 * @author Artem Ananiev
 * @author Anton Tarasov
 */
@SuppressWarnings("serial")
public abstract class LightweightFrame extends Frame {

    /**
     * Constructs a new, initially invisible {@code LightweightFrame}
     * instance.
     */
    public LightweightFrame() {
        setUndecorated(true);
        setResizable(true);
        setEnabled(true);
    }

    /**
     * Blocks introspection of a parent window by this child.
     *
     * @return null
     */
    @Override public final Container getParent() { return null; }

    @Override public Graphics getGraphics() { return null; }

    @Override public final boolean isResizable() { return true; }

    // Block modification of any frame attributes, since they aren't
    // applicable for a lightweight frame.

    @Override public final void setTitle(String title) {}
    @Override public final void setIconImage(Image image) {}
    @Override public final void setIconImages(java.util.List<? extends Image> icons) {}
    @Override public final void setMenuBar(MenuBar mb) {}
    @Override public final void setResizable(boolean resizable) {}
    @Override public final void remove(MenuComponent m) {}
    @Override public final void toFront() {}
    @Override public final void toBack() {}

    @SuppressWarnings("deprecation")
    @Override public void addNotify() {
        synchronized (getTreeLock()) {
            if (!isDisplayable()) {
                SunToolkit stk = (SunToolkit)Toolkit.getDefaultToolkit();
                try {
                    setPeer(stk.createLightweightFrame(this));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            super.addNotify();
        }
    }

    private void setPeer(final FramePeer p) {
        AWTAccessor.getComponentAccessor().setPeer(this, p);
    }

    /**
     * Requests the peer to emulate activation or deactivation of the
     * frame. Peers should override this method if they are to implement
     * this functionality.
     *
     * @param activate if {@code true}, activates the frame;
     *                 otherwise, deactivates the frame
     */
    public void emulateActivation(boolean activate) {
        final FramePeer peer = AWTAccessor.getComponentAccessor().getPeer(this);
        peer.emulateActivation(activate);
    }

    /**
     * Delegates the focus grab action to the client (embedding) application.
     * The method is called by the AWT grab machinery.
     *
     * @see SunToolkit#grab(java.awt.Window)
     */
    public abstract void grabFocus();

    /**
     * Delegates the focus ungrab action to the client (embedding) application.
     * The method is called by the AWT grab machinery.
     *
     * @see SunToolkit#ungrab(java.awt.Window)
     */
    public abstract void ungrabFocus();

    /**
     * Returns the scale factor of this frame. The default value is 1.
     *
     * @return the scale factor
     * @see #notifyDisplayChanged(int)
     * @Depricated replaced by {@link #getScaleFactorX()} and
     * {@link #getScaleFactorY}
     */
    @Deprecated(since = "9")
    public abstract int getScaleFactor();

    /**
     * Returns the scale factor of this frame along x coordinate. The default
     * value is 1.
     *
     * @return the x coordinate scale factor
     * @see #notifyDisplayChanged(double, double)
     * @since 9
     */
    public abstract double getScaleFactorX();

    /**
     * Returns the scale factor of this frame along y coordinate. The default
     * value is 1.
     *
     * @return the y coordinate scale factor
     * @see #notifyDisplayChanged(double, double)
     * @since 9
     */
    public abstract double getScaleFactorY();

    /**
     * Called when display of the hosted frame is changed.
     *
     * @param scaleFactor the scale factor
     * @Depricated replaced by {@link #notifyDisplayChanged(double, double)}
     */
    @Deprecated(since = "9")
    public abstract void notifyDisplayChanged(int scaleFactor);

    /**
     * Called when display of the hosted frame is changed.
     *
     * @param scaleFactorX the scale factor
     * @param scaleFactorY the scale factor
     * @since 9
     */
    public abstract void notifyDisplayChanged(double scaleFactorX,
                                              double scaleFactorY);

    /**
     * Host window absolute bounds.
     */
    private int hostX, hostY, hostW, hostH;

    /**
     * Returns the absolute bounds of the host (embedding) window.
     *
     * @return the host window bounds
     */
    public Rectangle getHostBounds() {
        if (hostX == 0 && hostY == 0 && hostW == 0 && hostH == 0) {
            // The client app is probably unaware of the setHostBounds.
            // A safe fall-back:
            return getBounds();
        }
        return new Rectangle(hostX, hostY, hostW, hostH);
    }

    /**
     * Sets the absolute bounds of the host (embedding) window.
     */
    public void setHostBounds(int x, int y, int w, int h) {
        hostX = x;
        hostY = y;
        hostW = w;
        hostH = h;
    }

    /**
     * Create a drag gesture recognizer for the lightweight frame.
     */
    public abstract <T extends DragGestureRecognizer> T createDragGestureRecognizer(
            Class<T> abstractRecognizerClass,
            DragSource ds, Component c, int srcActions,
            DragGestureListener dgl);

    /**
     * Create a drag source context peer for the lightweight frame.
     */
    public abstract DragSourceContextPeer createDragSourceContextPeer(DragGestureEvent dge) throws InvalidDnDOperationException;

    /**
     * Adds a drop target to the lightweight frame.
     */
    public abstract void addDropTarget(DropTarget dt);

    /**
     * Removes a drop target from the lightweight frame.
     */
    public abstract void removeDropTarget(DropTarget dt);

}
