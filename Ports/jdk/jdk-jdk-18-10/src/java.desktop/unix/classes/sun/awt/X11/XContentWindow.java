/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Insets;

import java.awt.event.ComponentEvent;

import sun.util.logging.PlatformLogger;

import sun.awt.AWTAccessor;

/**
 * This class implements window which serves as content window for decorated frames.
 * Its purpose to provide correct events dispatching for the complex
 * constructs such as decorated frames.
 *
 * It should always be located at (- left inset, - top inset) in the associated
 * decorated window.  So coordinates in it would be the same as java coordinates.
 */
public final class XContentWindow extends XWindow {
    private static PlatformLogger insLog = PlatformLogger.getLogger("sun.awt.X11.insets.XContentWindow");

    static XContentWindow createContent(XDecoratedPeer parentFrame) {
        final WindowDimensions dims = parentFrame.getDimensions();
        Rectangle rec = dims.getBounds();
        // Fix for  - set the location of the content window to the (-left inset, -top inset)
        Insets ins = dims.getInsets();
        if (ins != null) {
            rec.x = -ins.left;
            rec.y = -ins.top;
        } else {
            rec.x = 0;
            rec.y = 0;
        }
        final XContentWindow cw = new XContentWindow(parentFrame, rec);
        cw.xSetVisible(true);
        return cw;
    }

    private final XDecoratedPeer parentFrame;

    // A list of expose events that come when the parentFrame is iconified
    private final java.util.List<SavedExposeEvent> iconifiedExposeEvents =
            new java.util.ArrayList<SavedExposeEvent>();

    private XContentWindow(XDecoratedPeer parentFrame, Rectangle bounds) {
        super((Component)parentFrame.getTarget(), parentFrame.getShell(), bounds);
        this.parentFrame = parentFrame;
    }

    void preInit(XCreateWindowParams params) {
        super.preInit(params);
        params.putIfNull(BIT_GRAVITY, Integer.valueOf(XConstants.NorthWestGravity));
        Long eventMask = (Long)params.get(EVENT_MASK);
        if (eventMask != null) {
            eventMask = eventMask & ~(XConstants.StructureNotifyMask);
            params.put(EVENT_MASK, eventMask);
        }
    }

    protected String getWMName() {
        return "Content window";
    }
    protected boolean isEventDisabled(XEvent e) {
        switch (e.get_type()) {
          // Override parentFrame to receive MouseEnter/Exit
          case XConstants.EnterNotify:
          case XConstants.LeaveNotify:
              return false;
          // We handle ConfigureNotify specifically in XDecoratedPeer
          case XConstants.ConfigureNotify:
              return true;
          // We don't want SHOWN/HIDDEN on content window since it will duplicate XDecoratedPeer
          case XConstants.MapNotify:
          case XConstants.UnmapNotify:
              return true;
          default:
              return super.isEventDisabled(e) || parentFrame.isEventDisabled(e);
        }
    }

    // Coordinates are that of the shell
    void setContentBounds(WindowDimensions dims) {
        XToolkit.awtLock();
        try {
            // Bounds of content window are of the same size as bounds of Java window and with
            // location as -(insets)
            Rectangle newBounds = dims.getBounds();
            Insets in = dims.getInsets();
            if (in != null) {
                newBounds.setLocation(-in.left, -in.top);
            }
            if (insLog.isLoggable(PlatformLogger.Level.FINE)) {
                insLog.fine("Setting content bounds {0}, old bounds {1}",
                            newBounds, getBounds());
            }
            // Fix for 5023533:
            // Change in the size of the content window means, well, change of the size
            // Change in the location of the content window means change in insets
            boolean needHandleResize = !(newBounds.equals(getBounds()));
            boolean needPaint = width <= 0 || height <= 0;
            reshape(newBounds);
            if (needHandleResize) {
                insLog.fine("Sending RESIZED");
                handleResize(newBounds);
            }
            if (needPaint) {
                postPaintEvent(target, 0, 0, newBounds.width, newBounds.height);
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    @Override
    public void handleExposeEvent(XEvent xev) {
        if(parentFrame.isTargetUndecorated() &&
           XWM.getWMID() != XWM.UNITY_COMPIZ_WM &&
                width <= 0 && height <= 0) {
            // WM didn't send initial ConfigureNotify, so set the bounds here
            setContentBounds(parentFrame.getDimensions());
        }
        if (width <= 0 || height <= 0) {
            return;
        }
        super.handleExposeEvent(xev);
    }

    // NOTE: This method may be called by privileged threads.
    //       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
    public void handleResize(Rectangle bounds) {
        AWTAccessor.getComponentAccessor().setSize(target, bounds.width, bounds.height);
        postEvent(new ComponentEvent(target, ComponentEvent.COMPONENT_RESIZED));
    }


    public void postPaintEvent(Component target, int x, int y, int w, int h) {
        // TODO: ?
        // get rid of 'istanceof' by subclassing:
        // XContentWindow -> XFrameContentWindow

        // Expose event(s) that result from deiconification
        // come before a deicinofication notification.
        // We reorder these events by saving all expose events
        // that come when the frame is iconified. Then we
        // actually handle saved expose events on deiconification.

        if (parentFrame instanceof XFramePeer &&
                (((XFramePeer)parentFrame).getState() & java.awt.Frame.ICONIFIED) != 0) {
            // Save expose events if the frame is iconified
            // in order to handle them on deiconification.
            iconifiedExposeEvents.add(new SavedExposeEvent(target, x, y, w, h));
        } else {
            // Normal case: [it is not a frame or] the frame is not iconified.
            super.postPaintEvent(target, x, y, w, h);
        }
    }

    void purgeIconifiedExposeEvents() {
        for (SavedExposeEvent evt : iconifiedExposeEvents) {
            super.postPaintEvent(evt.target, evt.x, evt.y, evt.w, evt.h);
        }
        iconifiedExposeEvents.clear();
    }

    private static class SavedExposeEvent {
        Component target;
        int x, y, w, h;
        SavedExposeEvent(Component target, int x, int y, int w, int h) {
            this.target = target;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    public String toString() {
        return getClass().getName() + "[" + getBounds() + "]";
    }
}
