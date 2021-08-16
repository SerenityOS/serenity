/*
 * Copyright (c) 2002, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.event.*;
import java.awt.peer.*;
import sun.util.logging.PlatformLogger;

class XScrollbarPeer extends XComponentPeer implements ScrollbarPeer, XScrollbarClient {
    private static final PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.XScrollbarPeer");

    private static final int DEFAULT_LENGTH = 50;
    private static final int DEFAULT_WIDTH_SOLARIS = 19;
    private static final int DEFAULT_WIDTH_LINUX;

    XScrollbar tsb;

    static {
        DEFAULT_WIDTH_LINUX = XToolkit.getUIDefaults().getInt("ScrollBar.defaultWidth");
    }

    @SuppressWarnings("deprecation")
    public void preInit(XCreateWindowParams params) {
        super.preInit(params);
        Scrollbar target = (Scrollbar) this.target;
        if (target.getOrientation() == Scrollbar.VERTICAL) {
            tsb = new XVerticalScrollbar(this);
        } else {
            tsb = new XHorizontalScrollbar(this);
        }
        int min = target.getMinimum();
        int max = target.getMaximum();
        int vis = target.getVisibleAmount();
        int val = target.getValue();
        int line = target.getLineIncrement();
        int page = target.getPageIncrement();
        tsb.setValues(val, vis, min, max, line, page);
    }

    /**
     * Create a scrollbar.
     */
    XScrollbarPeer(Scrollbar target) {
        super(target);
        this.target = target;
        xSetVisible(true);
    }

    /**
     * Returns default size of scrollbar on the platform
     * Currently uses hardcoded values
     */
    private int getDefaultDimension() {
        if (System.getProperty("os.name").equals("Linux")) {
            return DEFAULT_WIDTH_LINUX;
        } else {
            return DEFAULT_WIDTH_SOLARIS;
        }
    }

    /**
     * Compute the minimum size for the scrollbar.
     */
    public Dimension getMinimumSize() {
        Scrollbar sb = (Scrollbar)target;
        return (sb.getOrientation() == Scrollbar.VERTICAL)
            ? new Dimension(getDefaultDimension(), DEFAULT_LENGTH)
                : new Dimension(DEFAULT_LENGTH, getDefaultDimension());
    }
    /**
     * Paint the scrollbar.
     */
    @Override
    void paintPeer(final Graphics g) {
        final Color[] colors = getGUIcolors();
        g.setColor(colors[BACKGROUND_COLOR]);
        tsb.paint(g, colors, true);
        // paint the whole scrollbar
    }

    public void repaintScrollbarRequest(XScrollbar sb) {
     repaint();
    }

    /**
     * The value has changed.
     */
    public void notifyValue(XScrollbar obj, int type, int value, boolean isAdjusting) {
        Scrollbar sb = (Scrollbar)target;
        sb.setValue(value);
        postEvent( new AdjustmentEvent(sb, AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED, type, value, isAdjusting));
    }

    /**
     *
     * @see java.awt.event.MouseEvent
     * MouseEvent.MOUSE_CLICKED
     * MouseEvent.MOUSE_PRESSED
     * MouseEvent.MOUSE_RELEASED
     * MouseEvent.MOUSE_MOVED
     * MouseEvent.MOUSE_ENTERED
     * MouseEvent.MOUSE_EXITED
     * MouseEvent.MOUSE_DRAGGED
     */
    @SuppressWarnings("deprecation")
    public void handleJavaMouseEvent( MouseEvent mouseEvent ) {
        super.handleJavaMouseEvent(mouseEvent);

        int x = mouseEvent.getX();
        int y = mouseEvent.getY();
        int modifiers = mouseEvent.getModifiers();
        int id = mouseEvent.getID();


        if ((mouseEvent.getModifiers() & InputEvent.BUTTON1_MASK) == 0) {
            return;
        }

        switch (mouseEvent.getID()) {
          case MouseEvent.MOUSE_PRESSED:
              target.requestFocus();
              tsb.handleMouseEvent(id, modifiers,x,y);
              break;

          case MouseEvent.MOUSE_RELEASED:
              tsb.handleMouseEvent(id, modifiers,x,y);
              break;

          case MouseEvent.MOUSE_DRAGGED:
              tsb.handleMouseEvent(id, modifiers,x,y);
              break;
        }
    }

    public void handleJavaKeyEvent(KeyEvent event) {
        super.handleJavaKeyEvent(event);
        if (log.isLoggable(PlatformLogger.Level.FINER)) {
            log.finer("KeyEvent on scrollbar: " + event);
        }
        if (!(event.isConsumed()) && event.getID() == KeyEvent.KEY_RELEASED) {
            switch(event.getKeyCode()) {
            case KeyEvent.VK_UP:
                log.finer("Scrolling up");
                tsb.notifyValue(tsb.getValue() - tsb.getUnitIncrement());
                break;
            case KeyEvent.VK_DOWN:
                log.finer("Scrolling down");
                tsb.notifyValue(tsb.getValue() + tsb.getUnitIncrement());
                break;
            case KeyEvent.VK_LEFT:
                log.finer("Scrolling up");
                tsb.notifyValue(tsb.getValue() - tsb.getUnitIncrement());
                break;
            case KeyEvent.VK_RIGHT:
                log.finer("Scrolling down");
                tsb.notifyValue(tsb.getValue() + tsb.getUnitIncrement());
                break;
            case KeyEvent.VK_PAGE_UP:
                log.finer("Scrolling page up");
                tsb.notifyValue(tsb.getValue() - tsb.getBlockIncrement());
                break;
            case KeyEvent.VK_PAGE_DOWN:
                log.finer("Scrolling page down");
                tsb.notifyValue(tsb.getValue() + tsb.getBlockIncrement());
                break;
            case KeyEvent.VK_HOME:
                log.finer("Scrolling to home");
                tsb.notifyValue(0);
                break;
            case KeyEvent.VK_END:
                log.finer("Scrolling to end");
                tsb.notifyValue(tsb.getMaximum());
                break;
            }
        }
    }

    public void setValue(int value) {
        tsb.setValue(value);
        repaint();
    }

    public void setValues(int value, int visible, int minimum, int maximum) {

        tsb.setValues(value, visible, minimum, maximum);
        repaint();
    }

    public void setLineIncrement(int l) {
        tsb.setUnitIncrement(l);
    }

    public void setPageIncrement(int p) {
        tsb.setBlockIncrement(p);
    }

    public void layout() {
        super.layout();
        tsb.setSize(width, height);
    }
}
