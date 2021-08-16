/*
 * Copyright (c) 1996, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.peer.ScrollPanePeer;

import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.ComponentAccessor;
import sun.awt.PeerEvent;

import sun.util.logging.PlatformLogger;

final class WScrollPanePeer extends WPanelPeer implements ScrollPanePeer {

    private static final PlatformLogger log = PlatformLogger.getLogger("sun.awt.windows.WScrollPanePeer");

    int scrollbarWidth;
    int scrollbarHeight;
    int prevx;
    int prevy;

    static {
        initIDs();
    }

    static native void initIDs();
    @Override
    native void create(WComponentPeer parent);
    native int getOffset(int orient);

    WScrollPanePeer(Component target) {
        super(target);
        scrollbarWidth = _getVScrollbarWidth();
        scrollbarHeight = _getHScrollbarHeight();
    }

    @Override
    void initialize() {
        super.initialize();
        setInsets();
        Insets i = getInsets();
        setScrollPosition(-i.left,-i.top);
    }

    @Override
    public void setUnitIncrement(Adjustable adj, int p) {
        // The unitIncrement is grabbed from the target as needed.
    }

    private native void setInsets();

    @Override
    public synchronized native void setScrollPosition(int x, int y);

    @Override
    public int getHScrollbarHeight() {
        return scrollbarHeight;
    }
    private native int _getHScrollbarHeight();

    @Override
    public int getVScrollbarWidth() {
        return scrollbarWidth;
    }
    private native int _getVScrollbarWidth();

    public Point getScrollOffset() {
        int x = getOffset(Adjustable.HORIZONTAL);
        int y = getOffset(Adjustable.VERTICAL);
        return new Point(x, y);
    }

    /**
     * The child component has been resized.  The scrollbars must be
     * updated with the new sizes.  At the native level the sizes of
     * the actual windows may not have changed yet, so the size
     * information from the java-level is passed down and used.
     */
    @Override
    public void childResized(int width, int height) {
        ScrollPane sp = (ScrollPane)target;
        Dimension vs = sp.getSize();
        setSpans(vs.width, vs.height, width, height);
        setInsets();
    }

    synchronized native void setSpans(int viewWidth, int viewHeight,
                                      int childWidth, int childHeight);

    /**
     * Called by ScrollPane's internal observer of the scrollpane's adjustables.
     * This is called whenever a scroll position is changed in one
     * of adjustables, whether it was modified externally or from the
     * native scrollbars themselves.
     */
    @Override
    public void setValue(Adjustable adj, int v) {
        Component c = getScrollChild();
        if (c == null) {
            return;
        }

        Point p = c.getLocation();
        switch(adj.getOrientation()) {
        case Adjustable.VERTICAL:
            setScrollPosition(-(p.x), v);
            break;
        case Adjustable.HORIZONTAL:
            setScrollPosition(v, -(p.y));
            break;
        }
    }

    private Component getScrollChild() {
        ScrollPane sp = (ScrollPane)target;
        Component child = null;
        try {
            child = sp.getComponent(0);
        } catch (ArrayIndexOutOfBoundsException e) {
            // do nothing.  in this case we return null
        }
        return child;
    }

    /*
     * Called from Windows in response to WM_VSCROLL/WM_HSCROLL message
     */
    private void postScrollEvent(int orient, int type,
                                 int pos, boolean isAdjusting)
    {
        Runnable adjustor = new Adjustor(orient, type, pos, isAdjusting);
        WToolkit.executeOnEventHandlerThread(new ScrollEvent(target, adjustor));
    }

    /*
     * Event that executes on the Java dispatch thread to move the
     * scroll bar thumbs and paint the exposed area in one synchronous
     * operation.
     */
    @SuppressWarnings("serial") // JDK-implementation class
    class ScrollEvent extends PeerEvent {
        ScrollEvent(Object source, Runnable runnable) {
            super(source, runnable, 0L);
        }

        @Override
        public PeerEvent coalesceEvents(PeerEvent newEvent) {
            if (log.isLoggable(PlatformLogger.Level.FINEST)) {
                log.finest("ScrollEvent coalesced: " + newEvent);
            }
            if (newEvent instanceof ScrollEvent) {
                return newEvent;
            }
            return null;
        }
    }

    /*
     * Runnable for the ScrollEvent that performs the adjustment.
     */
    class Adjustor implements Runnable {
        int orient;             // selects scrollbar
        int type;               // adjustment type
        int pos;                // new position (only used for absolute)
        boolean isAdjusting;    // isAdjusting status

        Adjustor(int orient, int type, int pos, boolean isAdjusting) {
            this.orient = orient;
            this.type = type;
            this.pos = pos;
            this.isAdjusting = isAdjusting;
        }

        @Override
        public void run() {
            if (getScrollChild() == null) {
                return;
            }
            ScrollPane sp = (ScrollPane)WScrollPanePeer.this.target;
            ScrollPaneAdjustable adj = null;

            // ScrollPaneAdjustable made public in 1.4, but
            // get[HV]Adjustable can't be declared to return
            // ScrollPaneAdjustable because it would break backward
            // compatibility -- hence the cast

            if (orient == Adjustable.VERTICAL) {
                adj = (ScrollPaneAdjustable)sp.getVAdjustable();
            } else if (orient == Adjustable.HORIZONTAL) {
                adj = (ScrollPaneAdjustable)sp.getHAdjustable();
            } else {
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("Assertion failed: unknown orient");
                }
            }

            if (adj == null) {
                return;
            }

            int newpos = adj.getValue();
            switch (type) {
              case AdjustmentEvent.UNIT_DECREMENT:
                  newpos -= adj.getUnitIncrement();
                  break;
              case AdjustmentEvent.UNIT_INCREMENT:
                  newpos += adj.getUnitIncrement();
                  break;
              case AdjustmentEvent.BLOCK_DECREMENT:
                  newpos -= adj.getBlockIncrement();
                  break;
              case AdjustmentEvent.BLOCK_INCREMENT:
                  newpos += adj.getBlockIncrement();
                  break;
              case AdjustmentEvent.TRACK:
                  newpos = this.pos;
                  break;
              default:
                  if (log.isLoggable(PlatformLogger.Level.FINE)) {
                      log.fine("Assertion failed: unknown type");
                  }
                  return;
            }

            // keep scroll position in acceptable range
            newpos = Math.max(adj.getMinimum(), newpos);
            newpos = Math.min(adj.getMaximum(), newpos);

            // set value, this will synchronously fire an AdjustmentEvent
            adj.setValueIsAdjusting(isAdjusting);

            // Fix for 4075484 - consider type information when creating AdjustmentEvent
            // We can't just call adj.setValue() because it creates AdjustmentEvent with type=TRACK
            // Instead, we call private method setTypedValue of ScrollPaneAdjustable.
            AWTAccessor.getScrollPaneAdjustableAccessor().setTypedValue(adj,
                                                                        newpos,
                                                                        type);

            // Paint the exposed area right away.  To do this - find
            // the heavyweight ancestor of the scroll child.
            Component hwAncestor = getScrollChild();
            final ComponentAccessor acc = AWTAccessor.getComponentAccessor();
            while (hwAncestor != null
                   && !(acc.getPeer(hwAncestor) instanceof WComponentPeer))
            {
                hwAncestor = hwAncestor.getParent();
            }
            if (log.isLoggable(PlatformLogger.Level.FINE)) {
                if (hwAncestor == null) {
                    log.fine("Assertion (hwAncestor != null) failed, " +
                             "couldn't find heavyweight ancestor of scroll pane child");
                }
            }
            WComponentPeer hwPeer = acc.getPeer(hwAncestor);
            hwPeer.paintDamagedAreaImmediately();
        }
    }

}
