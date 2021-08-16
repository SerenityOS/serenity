/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

import sun.awt.AWTAccessor;

public class XMenuItemPeer implements MenuItemPeer {

    /************************************************
     *
     * Data members
     *
     ************************************************/

    /*
     * Primary members
     */

    /**
     * Window that this item belongs to.
     */
    private XBaseMenuWindow container;

    /**
     * Target MenuItem. Note that 'target' member
     * in XWindow is required for dispatching events.
     * This member is only used for accessing its fields
     * and firing ActionEvent & ItemEvent
     */
    private MenuItem target;

    /*
     * Mapping to window
     */

    /**
     * Rectangle occupied by menu item in container's
     * coordinates. Filled by map(...) function from
     * XBaseMenuWindow.map()
     */
    private Rectangle bounds;

    /**
     * Point in container's coordinate system used as
     * origin by drawText.
     */
    private Point textOrigin;

    /*
     * Size constants
     */
    private static final int SEPARATOR_WIDTH = 20;
    private static final int SEPARATOR_HEIGHT = 5;

    /************************************************
     *
     * Text Metrics
     *
     ************************************************/

    /**
     * Text metrics are filled in calcTextMetrics function
     * and reset in resetTextMetrics function. Text metrics
     * contain calculated dimensions of various components of
     * menu item.
     */
    private TextMetrics textMetrics;

    static class TextMetrics implements Cloneable {
        /*
         * Calculated text size members
         */
        private Dimension textDimension;
        private int shortcutWidth;
        private int textBaseline;

        TextMetrics(Dimension textDimension, int shortcutWidth, int textBaseline) {
            this.textDimension = textDimension;
            this.shortcutWidth = shortcutWidth;
            this.textBaseline = textBaseline;
        }

        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException ex) {
                throw new InternalError(ex);
            }
        }

        Dimension getTextDimension() {
            return this.textDimension;
        }

        int getShortcutWidth() {
            return this.shortcutWidth;
        }

        int getTextBaseline() {
            return this.textBaseline;
        }
    }

    /************************************************
     *
     * Construction
     *
     ************************************************/
    XMenuItemPeer(MenuItem target) {
        this.target = target;
    }

    /************************************************
     *
     * Implementaion of interface methods
     *
     ************************************************/

    /*
     * From MenuComponentPeer
     */
    public void dispose() {
        //Empty function
    }

    public void setFont(Font font) {
        resetTextMetrics();
        repaintIfShowing();
    }
    /*
     * From MenuItemPeer
     */
    public void setLabel(String label) {
        resetTextMetrics();
        repaintIfShowing();
    }

    public void setEnabled(boolean enabled) {
        repaintIfShowing();
    }

    /************************************************
     *
     * Access to target's fields
     *
     ************************************************/

    MenuItem getTarget() {
        return this.target;
    }

    Font getTargetFont() {
        if (target == null) {
            return XWindow.getDefaultFont();
        }
        return AWTAccessor.getMenuComponentAccessor().getFont_NoClientCode(target);
    }

    String getTargetLabel() {
        if (target == null) {
            return "";
        }
        String label = AWTAccessor.getMenuItemAccessor().getLabel(target);
        return (label == null) ? "" : label;
    }

    boolean isTargetEnabled() {
        if (target == null) {
            return false;
        }
        return AWTAccessor.getMenuItemAccessor().isEnabled(target);
    }

    /**
     * Returns true if item and all its parents are enabled
     * This function is used to fix
     * 6184485: Popup menu is not disabled on XToolkit even when calling setEnabled (false)
     */
    boolean isTargetItemEnabled() {
        if (target == null) {
            return false;
        }
        return AWTAccessor.getMenuItemAccessor().isItemEnabled(target);
    }

    String getTargetActionCommand() {
        if (target == null) {
            return "";
        }
        return AWTAccessor.getMenuItemAccessor().getActionCommandImpl(target);
    }

    MenuShortcut getTargetShortcut() {
        if (target == null) {
            return null;
        }
        return AWTAccessor.getMenuItemAccessor().getShortcut(target);
    }

    String getShortcutText() {
        //Fix for 6180413: shortcuts should not be displayed for any of the menuitems in a popup menu
        if (container == null) {
            return null;
        }
        if (container.getRootMenuWindow() instanceof XPopupMenuPeer) {
            return null;
        }
        MenuShortcut sc = getTargetShortcut();
        //TODO:This can potentially call user code
        return (sc == null) ? null : sc.toString();
    }


    /************************************************
     *
     * Basic manipulations
     *
     ************************************************/

    /**
     * This function is called when filling item vectors
     * in XMenuWindow & XMenuBar. We need it because peers
     * are created earlier than windows.
     * @param container the window that this item belongs to.
     */
    void setContainer(XBaseMenuWindow container) {
        synchronized(XBaseMenuWindow.getMenuTreeLock()) {
            this.container = container;
        }
    }

    /**
     * returns the window that this item belongs to
     */
    XBaseMenuWindow getContainer() {
        return this.container;
    }

    /************************************************
     *
     * Overridable behaviour
     *
     ************************************************/

    /**
     * This function should be overriden simply to
     * return false in inherited classes.
     */
    boolean isSeparator() {
        boolean r = (getTargetLabel().equals("-"));
        return r;
    }

    /************************************************
     *
     * Utility functions
     *
     ************************************************/

    /**
     * Returns true if container exists and is showing
     */
    boolean isContainerShowing() {
        if (container == null) {
            return false;
        }
        return container.isShowing();
    }

    /**
     * Repaints item if it is showing
     */
    void repaintIfShowing() {
        if (isContainerShowing()) {
            container.postPaintEvent();
        }
    }

    /**
     * This function is invoked when the user clicks
     * on menu item.
     * @param when the timestamp of action event
     */
    void action(long when, int modifiers) {
        if (!isSeparator() && isTargetItemEnabled()) {
            XWindow.postEventStatic(new ActionEvent(target, ActionEvent.ACTION_PERFORMED,
                                                    getTargetActionCommand(), when,
                                                    modifiers));
        }
    }
    /************************************************
     *
     * Text metrics
     *
     ************************************************/

    /**
     * Returns text metrics of menu item.
     * This function does not use any locks
     * and is guaranteed to return some value
     * (possibly actual, possibly expired)
     */
    TextMetrics getTextMetrics() {
        TextMetrics textMetrics = this.textMetrics;
        if (textMetrics == null) {
            textMetrics = calcTextMetrics();
            this.textMetrics = textMetrics;
        }
        return textMetrics;
    }

    /**
     * Returns dimensions of item's label.
     * This function does not use any locks
     * Returns actual or expired  value
     * or null if error occurs
     */
    /*Dimension getTextDimension() {
        TextMetrics textMetrics = this.textMetrics;
        if (textMetrics == null) {
            textMetrics = calcTextMetrics();
            this.textMetrics = textMetrics;
        }
        return (textMetrics != null) ? textMetrics.textDimension : null;
        }*/

    /**
     * Returns width of item's shortcut label,
     * 0 if item has no shortcut.
     * The height of shortcut can be deternimed
     * from text dimensions.
     * This function does not use any locks
     * and is guaranteed to return some value
     * (possibly actual, possibly expired)
     */
    /*int getShortcutWidth() {
        TextMetrics textMetrics = this.textMetrics;
        if (textMetrics == null) {
            textMetrics = calcTextMetrics();
            this.textMetrics = textMetrics;
        }
        return (textMetrics != null) ? textMetrics.shortcutWidth : 0;
    }

    int getTextBaseline() {
        TextMetrics textMetrics = this.textMetrics;
        if (textMetrics == null) {
            textMetrics = calcTextMetrics();
            this.textMetrics = textMetrics;
        }
        return (textMetrics != null) ? textMetrics.textBaseline : 0;
        }*/

    TextMetrics calcTextMetrics() {
        if (container == null) {
            return null;
        }
        if (isSeparator()) {
            return new TextMetrics(new Dimension(SEPARATOR_WIDTH, SEPARATOR_HEIGHT), 0, 0);
        }
        Graphics g = container.getGraphics();
        if (g == null) {
            return null;
        }
        try {
            g.setFont(getTargetFont());
            FontMetrics fm = g.getFontMetrics();
            String str = getTargetLabel();
            int width = fm.stringWidth(str);
            int height = fm.getHeight();
            Dimension textDimension = new Dimension(width, height);
            int textBaseline = fm.getHeight() - fm.getAscent();
            String sc = getShortcutText();
            int shortcutWidth = (sc == null) ? 0 : fm.stringWidth(sc);
            return new TextMetrics(textDimension, shortcutWidth, textBaseline);
        } finally {
            g.dispose();
        }
    }

    void resetTextMetrics() {
        textMetrics = null;
        if (container != null) {
            container.updateSize();
        }
    }

    /************************************************
     *
     * Mapping utility functions
     *
     ************************************************/

    /**
     * Sets mapping of item to window.
     * @param bounds bounds of item in container's coordinates
     * @param textOrigin point for drawString in container's coordinates
     * @see XBaseMenuWindow#map()
     */
    void map(Rectangle bounds, Point textOrigin) {
        this.bounds = bounds;
        this.textOrigin = textOrigin;
    }

    /**
     * returns bounds of item that were previously set by map() function
     */
    Rectangle getBounds() {
        return bounds;
    }

    /**
     * returns origin of item's text that was previously set by map() function
     */
    Point getTextOrigin() {
        return textOrigin;
    }

}
