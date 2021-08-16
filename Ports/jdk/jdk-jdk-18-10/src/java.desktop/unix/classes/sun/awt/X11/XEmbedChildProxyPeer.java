/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.InvocationEvent;
import java.awt.event.KeyEvent;
import java.awt.event.PaintEvent;
import java.awt.image.ColorModel;
import java.awt.image.VolatileImage;
import java.awt.peer.ComponentPeer;
import java.awt.peer.ContainerPeer;

import sun.java2d.pipe.Region;

public class XEmbedChildProxyPeer implements ComponentPeer, XEventDispatcher{
    XEmbeddingContainer container;
    XEmbedChildProxy proxy;
    long handle;
    XEmbedChildProxyPeer(XEmbedChildProxy proxy) {
        this.container = proxy.getEmbeddingContainer();
        this.handle = proxy.getHandle();
        this.proxy = proxy;
        initDispatching();
    }

    void initDispatching() {
        XToolkit.awtLock();
        try {
            XToolkit.addEventDispatcher(handle, this);
            XlibWrapper.XSelectInput(XToolkit.getDisplay(), handle,
                    XConstants.StructureNotifyMask | XConstants.PropertyChangeMask);
        }
        finally {
            XToolkit.awtUnlock();
        }
        container.notifyChildEmbedded(handle);
    }
    public boolean isObscured() { return false; }
    public boolean canDetermineObscurity() { return false; }
    public void                 setVisible(boolean b) {
        if (!b) {
            XToolkit.awtLock();
            try {
                XlibWrapper.XUnmapWindow(XToolkit.getDisplay(), handle);
            }
            finally {
                XToolkit.awtUnlock();
            }
        } else {
            XToolkit.awtLock();
            try {
                XlibWrapper.XMapWindow(XToolkit.getDisplay(), handle);
            }
            finally {
                XToolkit.awtUnlock();
            }
        }
    }
    public void setEnabled(boolean b) {}
    public void paint(Graphics g) {}
    public void print(Graphics g) {}
    public void setBounds(int x, int y, int width, int height, int op) {
        // Unimplemeneted: Check for min/max hints for non-resizable
        XToolkit.awtLock();
        try {
            XlibWrapper.XMoveResizeWindow(XToolkit.getDisplay(), handle, x, y, width, height);
        }
        finally {
            XToolkit.awtUnlock();
        }
    }
    public void handleEvent(AWTEvent e) {
        switch (e.getID()) {
          case FocusEvent.FOCUS_GAINED:
              XKeyboardFocusManagerPeer.getInstance().setCurrentFocusOwner(proxy);
              container.focusGained(handle);
              break;
          case FocusEvent.FOCUS_LOST:
              XKeyboardFocusManagerPeer.getInstance().setCurrentFocusOwner(null);
              container.focusLost(handle);
              break;
          case KeyEvent.KEY_PRESSED:
          case KeyEvent.KEY_RELEASED:
              if (!((InputEvent)e).isConsumed()) {
                  container.forwardKeyEvent(handle, (KeyEvent)e);
              }
              break;
        }
    }
    public void                coalescePaintEvent(PaintEvent e) {}
    public Point                getLocationOnScreen() {
        XWindowAttributes attr = new XWindowAttributes();
        XToolkit.awtLock();
        try{
            XlibWrapper.XGetWindowAttributes(XToolkit.getDisplay(), handle, attr.pData);
            return new Point(attr.get_x(), attr.get_y());
        } finally {
            XToolkit.awtUnlock();
            attr.dispose();
        }
    }
    public Dimension            getPreferredSize() {
        XToolkit.awtLock();
        long p_hints = XlibWrapper.XAllocSizeHints();
        try {
            XSizeHints hints = new XSizeHints(p_hints);
            XlibWrapper.XGetWMNormalHints(XToolkit.getDisplay(), handle, p_hints, XlibWrapper.larg1);
            Dimension res = new Dimension(hints.get_width(), hints.get_height());
            return res;
        } finally {
            XlibWrapper.XFree(p_hints);
            XToolkit.awtUnlock();
        }
    }
    public Dimension            getMinimumSize() {
        XToolkit.awtLock();
        long p_hints = XlibWrapper.XAllocSizeHints();
        try {
            XSizeHints hints = new XSizeHints(p_hints);
            XlibWrapper.XGetWMNormalHints(XToolkit.getDisplay(), handle, p_hints, XlibWrapper.larg1);
            Dimension res = new Dimension(hints.get_min_width(), hints.get_min_height());
            return res;
        } finally {
            XlibWrapper.XFree(p_hints);
            XToolkit.awtUnlock();
        }
    }
    public ColorModel           getColorModel() { return null; }
    public Toolkit              getToolkit() { return Toolkit.getDefaultToolkit(); }

    public Graphics             getGraphics() { return null; }
    public FontMetrics          getFontMetrics(Font font) { return null; }
    public void         dispose() {
        container.detachChild(handle);
    }
    public void         setForeground(Color c) {}
    public void         setBackground(Color c) {}
    public void         setFont(Font f) {}
    public void                 updateCursorImmediately() {}

    void postEvent(AWTEvent event) {
        XToolkit.postEvent(XToolkit.targetToAppContext(proxy), event);
    }

    boolean simulateMotifRequestFocus(Component lightweightChild, boolean temporary,
                                      boolean focusedWindowChangeAllowed, long time)
    {
        if (lightweightChild == null) {
            lightweightChild = (Component)proxy;
        }
        Component currentOwner = XKeyboardFocusManagerPeer.getInstance().getCurrentFocusOwner();
        if (currentOwner != null && !currentOwner.isDisplayable()) {
            currentOwner = null;
        }
        FocusEvent  fg = new FocusEvent(lightweightChild, FocusEvent.FOCUS_GAINED, false, currentOwner );
        FocusEvent fl = null;
        if (currentOwner != null) {
            fl = new FocusEvent(currentOwner, FocusEvent.FOCUS_LOST, false, lightweightChild);
        }

        // TODO: do we need to wrap in sequenced?
        if (fl != null) {
            postEvent(XComponentPeer.wrapInSequenced(fl));
        }
        postEvent(XComponentPeer.wrapInSequenced(fg));
        // End of Motif compatibility code
        return true;
    }

    public boolean requestFocus(Component lightweightChild,
                                boolean temporary,
                                boolean focusedWindowChangeAllowed,
                                long time,
                                FocusEvent.Cause cause)
    {
        int result = XKeyboardFocusManagerPeer
            .shouldNativelyFocusHeavyweight(proxy, lightweightChild,
                                            temporary, false, time, cause);

        switch (result) {
          case XKeyboardFocusManagerPeer.SNFH_FAILURE:
              return false;
          case XKeyboardFocusManagerPeer.SNFH_SUCCESS_PROCEED:
              // Currently we just generate focus events like we deal with lightweight instead of calling
              // XSetInputFocus on native window

              /**
               * The problems with requests in non-focused window arise because shouldNativelyFocusHeavyweight
               * checks that native window is focused while appropriate WINDOW_GAINED_FOCUS has not yet
               * been processed - it is in EventQueue. Thus, SNFH allows native request and stores request record
               * in requests list - and it breaks our requests sequence as first record on WGF should be the last focus
               * owner which had focus before WLF. So, we should not add request record for such requests
               * but store this component in mostRecent - and return true as before for compatibility.
               */
              Container parent = proxy.getParent();
              // Search for parent window
              while (parent != null && !(parent instanceof Window)) {
                  parent = parent.getParent();
              }
              if (parent != null) {
                  Window parentWindow = (Window)parent;
                  // and check that it is focused
                  if (!parentWindow.isFocused() &&
                      XKeyboardFocusManagerPeer.getInstance().getCurrentFocusedWindow() == parentWindow) {
                      // if it is not - skip requesting focus on Solaris
                      // but return true for compatibility.
                      return true;
                  }
              }

              // NOTE: We simulate heavyweight behavior of Motif - component receives focus right
              // after request, not after event. Normally, we should better listen for event
              // by listeners.

              // TODO: consider replacing with XKeyboardFocusManagerPeer.deliverFocus
              return simulateMotifRequestFocus(lightweightChild, temporary, focusedWindowChangeAllowed, time);
              // Motif compatibility code
          case XKeyboardFocusManagerPeer.SNFH_SUCCESS_HANDLED:
              // Either lightweight or excessive requiest - all events are generated.
              return true;
        }
        return false;
    }
    public boolean              isFocusable() {
        return true;
    }

    public Image                createImage(int width, int height) { return null; }
    public VolatileImage        createVolatileImage(int width, int height) { return null; }
    public GraphicsConfiguration getGraphicsConfiguration() { return null; }
    public boolean     handlesWheelScrolling() { return true; }
    public void createBuffers(int numBuffers, BufferCapabilities caps)
      throws AWTException { }
    public Image getBackBuffer() { return null; }
    public void flip(int x1, int y1, int x2, int y2, BufferCapabilities.FlipContents flipAction) {  }
    public void destroyBuffers() { }

    /**
     * Used by lightweight implementations to tell a ComponentPeer to layout
     * its sub-elements.  For instance, a lightweight Checkbox needs to layout
     * the box, as well as the text label.
     */
    public void        layout() {}

    Window getTopLevel(Component comp) {
        while (comp != null && !(comp instanceof Window)) {
            comp = comp.getParent();
        }
        return (Window)comp;
    }

    void childResized() {
        XToolkit.postEvent(XToolkit.targetToAppContext(proxy), new ComponentEvent(proxy, ComponentEvent.COMPONENT_RESIZED));
        container.childResized(proxy);
//         XToolkit.postEvent(XToolkit.targetToAppContext(proxy), new InvocationEvent(proxy, new Runnable() {
//                 public void run() {
//                     getTopLevel(proxy).invalidate();
//                     getTopLevel(proxy).pack();
//                 }
//             }));
    }
    void handlePropertyNotify(XEvent xev) {
        XPropertyEvent ev = xev.get_xproperty();
        if (ev.get_atom() == XAtom.XA_WM_NORMAL_HINTS) {
            childResized();
        }
    }
    void handleConfigureNotify(XEvent xev) {
        childResized();
    }
    public void dispatchEvent(XEvent xev) {
        int type = xev.get_type();
        switch (type) {
          case XConstants.PropertyNotify:
              handlePropertyNotify(xev);
              break;
          case XConstants.ConfigureNotify:
              handleConfigureNotify(xev);
              break;
        }
    }

    void requestXEmbedFocus() {
        postEvent(new InvocationEvent(proxy, new Runnable() {
                public void run() {
                    proxy.requestFocusInWindow();
                }
            }));
    }

    public void reparent(ContainerPeer newNativeParent) {
    }
    public boolean isReparentSupported() {
        return false;
    }
    public Rectangle getBounds() {
        XWindowAttributes attrs = new XWindowAttributes();
        XToolkit.awtLock();
        try {
            XlibWrapper.XGetWindowAttributes(XToolkit.getDisplay(), handle, attrs.pData);
            return new Rectangle(attrs.get_x(), attrs.get_y(), attrs.get_width(), attrs.get_height());
        } finally {
            XToolkit.awtUnlock();
            attrs.dispose();
        }
    }
    public void setBoundsOperation(int operation) {
    }

    public void applyShape(Region shape) {
    }

    public void setZOrder(ComponentPeer above) {
    }

    public boolean updateGraphicsData(GraphicsConfiguration gc) {
        return false;
    }
}
