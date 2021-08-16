/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.peer.*;
import java.awt.event.*;

import java.util.Vector;
import sun.awt.AWTAccessor;
import sun.util.logging.PlatformLogger;

public class XPopupMenuPeer extends XMenuWindow implements PopupMenuPeer {

    /************************************************
     *
     * Data members
     *
     ************************************************/
    private static PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.XBaseMenuWindow");

    /*
     * Primary members
     */
    private XComponentPeer componentPeer;
    private PopupMenu popupMenuTarget;

    /*
     * If mouse button is clicked on item showing submenu
     * we have to hide its submenu.
     * This member saves the submenu under cursor
     * Only if it's showing
     */
    private XMenuPeer showingMousePressedSubmenu = null;

    /*
     * Painting constants
     */
    private static final int CAPTION_MARGIN_TOP = 4;
    private static final int CAPTION_SEPARATOR_HEIGHT = 6;

    /************************************************
     *
     * Construction
     *
     ************************************************/
    XPopupMenuPeer(PopupMenu target) {
        super(null);
        this.popupMenuTarget = target;
    }

    /************************************************
     *
     * Implementation of interface methods
     *
     ************************************************/
    /*
     * From MenuComponentPeer
     */
    @Override
    public void setFont(Font f) {
        resetMapping();
        setItemsFont(f);
        postPaintEvent();
    }

    /*
     * From MenuItemPeer
     */
    @Override
    public void setLabel(String label) {
        resetMapping();
        postPaintEvent();
    }


    @Override
    public void setEnabled(boolean enabled) {
        postPaintEvent();
    }

    /*
     * From PopupMenuPeer
     */
    @Override
    @SuppressWarnings("deprecation")
    public void show(Event e) {
        target = (Component)e.target;
        // Get menus from the target.
        Vector<MenuItem> targetItemVector = getMenuTargetItems();
        if (targetItemVector != null) {
            reloadItems(targetItemVector);
            //Fix for 6287092: JCK15a: api/java_awt/interactive/event/EventTests.html#EventTest0015 fails, mustang
            Point tl = target.getLocationOnScreen();
            Point pt = new Point(tl.x + e.x, tl.y + e.y);
            //Fixed 6266513: Incorrect key handling in XAWT popup menu
            //No item should be selected when showing popup menu
            if (!ensureCreated()) {
                return;
            }
            Dimension dim = getDesiredSize();
            //Fix for 6267162: PIT: Popup Menu gets hidden below the screen when opened
            //near the periphery of the screen, XToolkit
            Rectangle bounds = getWindowBounds(pt, dim);
            reshape(bounds);
            xSetVisible(true);
            toFront();
            selectItem(null, false);
            grabInput();
        }
    }

    /************************************************
     *
     * Access to target's fields
     *
     ************************************************/

    //Fix for 6267144: PIT: Popup menu label is not shown, XToolkit
    Font getTargetFont() {
        if (popupMenuTarget == null) {
            return XWindow.getDefaultFont();
        }
        return AWTAccessor.getMenuComponentAccessor()
                   .getFont_NoClientCode(popupMenuTarget);
    }

    //Fix for 6267144: PIT: Popup menu label is not shown, XToolkit
    String getTargetLabel() {
        if (target == null) {
            return "";
        }
        return AWTAccessor.getMenuItemAccessor().getLabel(popupMenuTarget);
    }

    //Fix for 6184485: Popup menu is not disabled on XToolkit even when calling setEnabled (false)
    boolean isTargetEnabled() {
        if (popupMenuTarget == null) {
            return false;
        }
        return AWTAccessor.getMenuItemAccessor().isEnabled(popupMenuTarget);
    }

    @Override
    Vector<MenuItem> getMenuTargetItems() {
        if (popupMenuTarget == null) {
            return null;
        }
        return AWTAccessor.getMenuAccessor().getItems(popupMenuTarget);
    }

    /************************************************
     *
     * Utility functions
     *
     ************************************************/

    //Fix for 6267162: PIT: Popup Menu gets hidden below the screen when opened
    //near the periphery of the screen, XToolkit

    /**
     * Calculates placement of popup menu window
     * given origin in global coordinates and
     * size of menu window. Returns suggested
     * rectangle for menu window in global coordinates
     * @param origin the origin point specified in show()
     * function converted to global coordinates
     * @param windowSize the desired size of menu's window
     */
    protected Rectangle getWindowBounds(Point origin, Dimension windowSize) {
        Rectangle globalBounds = new Rectangle(origin.x, origin.y, 0, 0);
        Rectangle screenBounds = getCurrentGraphicsConfiguration().getBounds();
        Rectangle res;
        res = fitWindowRight(globalBounds, windowSize, screenBounds);
        if (res != null) {
            return res;
        }
        res = fitWindowLeft(globalBounds, windowSize, screenBounds);
        if (res != null) {
            return res;
        }
        res = fitWindowBelow(globalBounds, windowSize, screenBounds);
        if (res != null) {
            return res;
        }
        res = fitWindowAbove(globalBounds, windowSize, screenBounds);
        if (res != null) {
            return res;
        }
        return fitWindowToScreen(windowSize, screenBounds);
   }

    /************************************************
     *
     * Overriden XMenuWindow caption-painting functions
     * Necessary to fix 6267144: PIT: Popup menu label is not shown, XToolkit
     *
     ************************************************/
    /**
     * Returns height of menu window's caption.
     * Can be overriden for popup menus and tear-off menus
     */
    @Override
    protected Dimension getCaptionSize() {
        String s = getTargetLabel();
        if (s.isEmpty()) {
            return null;
        }
        Graphics g = getGraphics();
        if (g == null) {
            return null;
        }
        try {
            g.setFont(getTargetFont());
            FontMetrics fm = g.getFontMetrics();
            String str = getTargetLabel();
            int width = fm.stringWidth(str);
            int height = CAPTION_MARGIN_TOP + fm.getHeight() + CAPTION_SEPARATOR_HEIGHT;
            Dimension textDimension = new Dimension(width, height);
            return textDimension;
        } finally {
            g.dispose();
        }
    }

    /**
     * Paints menu window's caption.
     * Can be overriden for popup menus and tear-off menus.
     * Default implementation does nothing
     */
    @Override
    protected void paintCaption(Graphics g, Rectangle rect) {
        String s = getTargetLabel();
        if (s.isEmpty()) {
            return;
        }
        g.setFont(getTargetFont());
        FontMetrics fm = g.getFontMetrics();
        String str = getTargetLabel();
        int width = fm.stringWidth(str);
        int textx = rect.x + (rect.width - width) / 2;
        int texty = rect.y + CAPTION_MARGIN_TOP + fm.getAscent();
        int sepy = rect.y + rect.height - CAPTION_SEPARATOR_HEIGHT / 2;
        g.setColor(isTargetEnabled() ? getForegroundColor() : getDisabledColor());
        g.drawString(s, textx, texty);
        draw3DRect(g, rect.x, sepy,  rect.width, 2, false);
    }

    /************************************************
     *
     * Overriden XBaseMenuWindow functions
     *
     ************************************************/
    @Override
    protected void doDispose() {
        super.doDispose();
        XToolkit.targetDisposedPeer(popupMenuTarget, this);
    }

    @Override
    protected void handleEvent(AWTEvent event) {
        switch(event.getID()) {
        case MouseEvent.MOUSE_PRESSED:
        case MouseEvent.MOUSE_RELEASED:
        case MouseEvent.MOUSE_CLICKED:
        case MouseEvent.MOUSE_MOVED:
        case MouseEvent.MOUSE_ENTERED:
        case MouseEvent.MOUSE_EXITED:
        case MouseEvent.MOUSE_DRAGGED:
            doHandleJavaMouseEvent((MouseEvent)event);
            break;
        case KeyEvent.KEY_PRESSED:
        case KeyEvent.KEY_RELEASED:
            doHandleJavaKeyEvent((KeyEvent)event);
            break;
        default:
            super.handleEvent(event);
            break;
        }
    }

    /************************************************
     *
     * Overriden XWindow general-purpose functions
     *
     ************************************************/
    @Override
    void ungrabInputImpl() {
        hide();
    }

    /************************************************
     *
     * Overriden XWindow keyboard processing
     *
     ************************************************/

    /*
     * In previous version keys were handled in handleKeyPress.
     * Now we override this function do disable F10 explicit
     * processing. All processing is done using KeyEvent.
     */
    @Override
    public void handleKeyPress(XEvent xev) {
        XKeyEvent xkey = xev.get_xkey();
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine(xkey.toString());
        }
        if (isEventDisabled(xev)) {
            return;
        }
        final Component currentSource = getEventSource();
        handleKeyPress(xkey);
    }

}
