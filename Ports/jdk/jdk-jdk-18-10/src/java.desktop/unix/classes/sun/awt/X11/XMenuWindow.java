/*
 * Copyright (c) 2002, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.image.BufferedImage;
import java.awt.geom.Point2D;

import java.util.Vector;
import sun.util.logging.PlatformLogger;

public class XMenuWindow extends XBaseMenuWindow {

    /************************************************
     *
     * Data members
     *
     ************************************************/

    private static PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.XMenuWindow");

    /*
     * Primary members
     */
    private XMenuPeer menuPeer;

    /*
     * dimension constants
     */
    private static final int WINDOW_SPACING_LEFT = 2;
    private static final int WINDOW_SPACING_RIGHT = 2;
    private static final int WINDOW_SPACING_TOP = 2;
    private static final int WINDOW_SPACING_BOTTOM = 2;
    private static final int WINDOW_ITEM_INDENT = 15;
    private static final int WINDOW_ITEM_MARGIN_LEFT = 2;
    private static final int WINDOW_ITEM_MARGIN_RIGHT = 2;
    private static final int WINDOW_ITEM_MARGIN_TOP = 2;
    private static final int WINDOW_ITEM_MARGIN_BOTTOM = 2;
    private static final int WINDOW_SHORTCUT_SPACING = 10;

    /*
     * Checkmark
     */
    private static final int CHECKMARK_SIZE = 128;
    private static final int[] CHECKMARK_X = new int[] {1, 25,56,124,124,85, 64};  // X-coords
    private static final int[] CHECKMARK_Y = new int[] {59,35,67,  0, 12,66,123};  // Y-coords

    /************************************************
     *
     * Mapping data
     *
     ************************************************/

    static class MappingData extends XBaseMenuWindow.MappingData {
        /**
         * Rectangle for the caption
         * Necessary to fix 6267144: PIT: Popup menu label is not shown, XToolkit
         */
        private Rectangle captionRect;

        /**
         * Desired size of menu window
         */
        private Dimension desiredSize;

        /**
         * Width of largest checkmark
         * At the same time the left origin
         * of all item's text
         */
        private int leftMarkWidth;

        /**
         * Left origin of all shortcut labels
         */
        private int shortcutOrigin;

        /**
         * The origin of right mark
         * (submenu's arrow)
         */
        private int rightMarkOrigin;

        MappingData(XMenuItemPeer[] items, Rectangle captionRect, Dimension desiredSize, int leftMarkWidth, int shortcutOrigin, int rightMarkOrigin) {
            super(items);
            this.captionRect = captionRect;
            this.desiredSize = desiredSize;
            this.leftMarkWidth = leftMarkWidth;
            this.shortcutOrigin = shortcutOrigin;
            this.rightMarkOrigin = rightMarkOrigin;
        }

        /**
         * Constructs MappingData without items
         * This constructor should be used in case of errors
         */
        MappingData() {
            this.desiredSize = new Dimension(0, 0);
            this.leftMarkWidth = 0;
            this.shortcutOrigin = 0;
            this.rightMarkOrigin = 0;
        }

        public Rectangle getCaptionRect() {
            return this.captionRect;
        }

        public Dimension getDesiredSize() {
            return this.desiredSize;
        }

        public int getShortcutOrigin() {
            return this.shortcutOrigin;
        }

        public int getLeftMarkWidth() {
            return this.leftMarkWidth;
        }

        public int getRightMarkOrigin() {
            return this.rightMarkOrigin;
        }

    }


    /************************************************
     *
     * Construction
     *
     ************************************************/

    /**
     * Constructs XMenuWindow for specified XMenuPeer
     * null for XPopupMenuWindow
     */
    XMenuWindow(XMenuPeer menuPeer) {
        if (menuPeer != null) {
            this.menuPeer = menuPeer;
            this.target = menuPeer.getContainer().target;
            // Get menus from the target.
            Vector<MenuItem> targetItemVector = null;
            targetItemVector = getMenuTargetItems();
            reloadItems(targetItemVector);
        }
    }

    /************************************************
     *
     * Initialization
     *
     ************************************************/
    /*
     * Overriden initialization
     */
    void postInit(XCreateWindowParams params) {
        super.postInit(params);
        //Fixed 6267182: PIT: Menu is not visible after
        //showing and disposing a file dialog, XToolkit
        //toFront() is called on every show
    }

    /************************************************
     *
     * Implementation of abstract methods
     *
     ************************************************/

    /**
     * @see XBaseMenuWindow#getParentMenuWindow()
     */
    protected XBaseMenuWindow getParentMenuWindow() {
        return (menuPeer != null) ? menuPeer.getContainer() : null;
    }

    /**
     * @see XBaseMenuWindow#map()
     */
    protected MappingData map() {
        //TODO:Implement popup-menu caption mapping and painting and tear-off
        int itemCnt;
        if (!isCreated()) {
            MappingData mappingData = new MappingData(new XMenuItemPeer[0], new Rectangle(0, 0, 0, 0), new Dimension(0, 0), 0, 0, 0);
            return mappingData;
        }
        XMenuItemPeer[] itemVector = copyItems();
        itemCnt = itemVector.length;
        //We need maximum width of components before calculating item's bounds
        Dimension captionSize = getCaptionSize();
        int maxWidth = (captionSize != null) ? captionSize.width : 0;
        int maxLeftIndent = 0;
        int maxRightIndent = 0;
        int maxShortcutWidth = 0;
        XMenuItemPeer.TextMetrics[] itemMetrics = new XMenuItemPeer.TextMetrics[itemCnt];
        for (int i = 0; i < itemCnt; i++) {
            XMenuItemPeer item = itemVector[i];
            itemMetrics[i] = itemVector[i].getTextMetrics();
            Dimension dim = itemMetrics[i].getTextDimension();
            if (dim != null) {
                if (itemVector[i] instanceof XCheckboxMenuItemPeer) {
                    maxLeftIndent = Math.max(maxLeftIndent, dim.height);
                } else if (itemVector[i] instanceof XMenuPeer) {
                    maxRightIndent = Math.max(maxRightIndent, dim.height);
                }
                maxWidth = Math.max(maxWidth, dim.width);
                maxShortcutWidth = Math.max(maxShortcutWidth, itemMetrics[i].getShortcutWidth());
            }
        }
        //Calculate bounds
        int nextOffset = WINDOW_SPACING_TOP;
        int shortcutOrigin = WINDOW_SPACING_LEFT + WINDOW_ITEM_MARGIN_LEFT + maxLeftIndent + maxWidth;
        if (maxShortcutWidth > 0) {
            shortcutOrigin = shortcutOrigin + WINDOW_SHORTCUT_SPACING;
        }
        int rightMarkOrigin = shortcutOrigin + maxShortcutWidth;
        int itemWidth = rightMarkOrigin + maxRightIndent + WINDOW_ITEM_MARGIN_RIGHT;
        int width = WINDOW_SPACING_LEFT + itemWidth + WINDOW_SPACING_RIGHT;
        //Caption rectangle
        Rectangle captionRect = null;
        if (captionSize != null) {
            captionRect = new Rectangle(WINDOW_SPACING_LEFT, nextOffset, itemWidth, captionSize.height);
            nextOffset += captionSize.height;
        } else {
            captionRect = new Rectangle(WINDOW_SPACING_LEFT, nextOffset, maxWidth, 0);
        }
        //Item rectangles
        for (int i = 0; i < itemCnt; i++) {
            XMenuItemPeer item = itemVector[i];
            XMenuItemPeer.TextMetrics metrics = itemMetrics[i];
            Dimension dim = metrics.getTextDimension();
            if (dim != null) {
                int itemHeight = WINDOW_ITEM_MARGIN_TOP + dim.height + WINDOW_ITEM_MARGIN_BOTTOM;
                Rectangle bounds = new Rectangle(WINDOW_SPACING_LEFT, nextOffset, itemWidth, itemHeight);
                int y = (itemHeight + dim.height) / 2  - metrics.getTextBaseline();
                Point textOrigin = new Point(WINDOW_SPACING_LEFT + WINDOW_ITEM_MARGIN_LEFT + maxLeftIndent, nextOffset + y);
                nextOffset += itemHeight;
                item.map(bounds, textOrigin);
            } else {
                //Text metrics could not be determined because of errors
                //Map item with empty rectangle
                Rectangle bounds = new Rectangle(WINDOW_SPACING_LEFT, nextOffset, 0, 0);
                Point textOrigin = new Point(WINDOW_SPACING_LEFT + WINDOW_ITEM_MARGIN_LEFT + maxLeftIndent, nextOffset);
                item.map(bounds, textOrigin);
            }
        }
        int height = nextOffset + WINDOW_SPACING_BOTTOM;
        MappingData mappingData = new MappingData(itemVector, captionRect, new Dimension(width, height), maxLeftIndent, shortcutOrigin, rightMarkOrigin);
        return mappingData;
    }

    /**
     * @see XBaseMenuWindow#getSubmenuBounds
     */
    protected Rectangle getSubmenuBounds(Rectangle itemBounds, Dimension windowSize) {
        Rectangle globalBounds = toGlobal(itemBounds);
        Rectangle screenBounds = getCurrentGraphicsConfiguration().getBounds();
        Rectangle res;
        res = fitWindowRight(globalBounds, windowSize, screenBounds);
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
        res = fitWindowLeft(globalBounds, windowSize, screenBounds);
        if (res != null) {
            return res;
        }
        return fitWindowToScreen(windowSize, screenBounds);
   }

    /**
     * It's likely that size of items was changed
     * invoke resizing of window on eventHandlerThread
     */
    protected void updateSize() {
        resetMapping();
        if (isShowing()) {
            XToolkit.executeOnEventHandlerThread(target, new Runnable() {
                    public void run() {
                        Dimension dim = getDesiredSize();
                        reshape(x, y, dim.width, dim.height);
                    }
                });
        }
    }

    /************************************************
     *
     * Overridable caption-painting functions
     * Necessary to fix 6267144: PIT: Popup menu label is not shown, XToolkit
     *
     ************************************************/

    /**
     * Returns size of menu window's caption or null
     * if window has no caption.
     * Can be overriden for popup menus and tear-off menus
     */
    protected Dimension getCaptionSize() {
        return null;
    }

    /**
     * Paints menu window's caption.
     * Can be overriden for popup menus and tear-off menus.
     * Default implementation does nothing
     */
    protected void paintCaption(Graphics g, Rectangle rect) {
    }

    /************************************************
     *
     * General-purpose utility functions
     *
     ************************************************/

    /**
     * Returns corresponding menu peer
     */
    XMenuPeer getMenuPeer() {
        return menuPeer;
    }

    /**
     * Reads vector of items from target
     * This function is overriden in XPopupMenuPeer
     */
    Vector<MenuItem> getMenuTargetItems() {
        return menuPeer.getTargetItems();
    }

    /**
     * Returns desired size calculated while mapping
     */
    Dimension getDesiredSize() {
        MappingData mappingData = (MappingData)getMappingData();
        return mappingData.getDesiredSize();
    }

    /**
     * Checks if menu window is created
     */
    boolean isCreated() {
        return getWindow() != 0;
    }

    /**
     * Performs delayed creation of menu window if necessary
     */
    boolean ensureCreated() {
        if (!isCreated()) {
            XCreateWindowParams params = getDelayedParams();
            params.remove(DELAYED);
            params.add(OVERRIDE_REDIRECT, Boolean.TRUE);
            params.add(XWindow.TARGET, target);
            init(params);
        }
        return true;
    }

    /**
     * Init window if it's not inited yet
     * and show it at specified coordinates
     * @param bounds bounding rectangle of window
     * in global coordinates
     */
    void show(Rectangle bounds) {
        if (!isCreated()) {
            return;
        }
        if (log.isLoggable(PlatformLogger.Level.FINER)) {
            log.finer("showing menu window + " + getWindow() + " at " + bounds);
        }
        XToolkit.awtLock();
        try {
            reshape(bounds.x, bounds.y, bounds.width, bounds.height);
            xSetVisible(true);
            //Fixed 6267182: PIT: Menu is not visible after
            //showing and disposing a file dialog, XToolkit
            toFront();
            selectItem(getFirstSelectableItem(), false);
        } finally {
            XToolkit.awtUnlock();
        }
    }

    /**
     * Hides menu window
     */
    void hide() {
        selectItem(null, false);
        xSetVisible(false);
    }

    /************************************************
     *
     * Painting
     *
     ************************************************/

    /**
     * Paints menu window
     */
    @Override
    public void paintPeer(Graphics g) {
        resetColors();
        int width = getWidth();
        int height = getHeight();

        flush();
        //Fill background of rectangle
        g.setColor(getBackgroundColor());
        g.fillRect(1, 1, width - 2, height - 2);
        draw3DRect(g, 0, 0, width, height, true);

        //Mapping data
        MappingData mappingData = (MappingData)getMappingData();

        //Paint caption
        paintCaption(g, mappingData.getCaptionRect());

        //Paint menus
        XMenuItemPeer[] itemVector = mappingData.getItems();
        Dimension windowSize =  mappingData.getDesiredSize();
        XMenuItemPeer selectedItem = getSelectedItem();
        for (int i = 0; i < itemVector.length; i++) {
            XMenuItemPeer item = itemVector[i];
            XMenuItemPeer.TextMetrics metrics = item.getTextMetrics();
            Rectangle bounds = item.getBounds();
            if (item.isSeparator()) {
                draw3DRect(g, bounds.x, bounds.y + bounds.height / 2,  bounds.width, 2, false);
            } else {
                //paint item
                g.setFont(item.getTargetFont());
                Point textOrigin = item.getTextOrigin();
                Dimension textDim = metrics.getTextDimension();
                if (item == selectedItem) {
                    g.setColor(getSelectedColor());
                    g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                    draw3DRect(g, bounds.x, bounds.y, bounds.width, bounds.height, false);
                }
                g.setColor(item.isTargetItemEnabled() ? getForegroundColor() : getDisabledColor());
                g.drawString(item.getTargetLabel(), textOrigin.x, textOrigin.y);
                String shortcutText = item.getShortcutText();
                if (shortcutText != null) {
                    g.drawString(shortcutText, mappingData.getShortcutOrigin(), textOrigin.y);
                }
                if (item instanceof XMenuPeer) {
                    //calculate arrow coordinates
                    int markWidth = textDim.height * 4 / 5;
                    int markHeight = textDim.height * 4 / 5;
                    int markX = bounds.x + bounds.width - markWidth - WINDOW_SPACING_RIGHT - WINDOW_ITEM_MARGIN_RIGHT;
                    int markY = bounds.y + (bounds.height - markHeight) / 2;
                    //draw arrow
                    g.setColor(item.isTargetItemEnabled() ? getDarkShadowColor() : getDisabledColor());
                    g.drawLine(markX, markY + markHeight, markX + markWidth, markY + markHeight / 2);
                    g.setColor(item.isTargetItemEnabled() ? getLightShadowColor() : getDisabledColor());
                    g.drawLine(markX, markY, markX + markWidth, markY + markHeight / 2);
                    g.drawLine(markX, markY, markX, markY + markHeight);
                } else if (item instanceof XCheckboxMenuItemPeer) {
                    //calculate checkmark coordinates
                    int markWidth = textDim.height * 4 / 5;
                    int markHeight = textDim.height * 4 / 5;
                    int markX = WINDOW_SPACING_LEFT + WINDOW_ITEM_MARGIN_LEFT;
                    int markY = bounds.y + (bounds.height - markHeight) / 2;
                    boolean checkState = ((XCheckboxMenuItemPeer)item).getTargetState();
                    //draw checkmark
                    if (checkState) {
                        g.setColor(getSelectedColor());
                        g.fillRect(markX, markY, markWidth, markHeight);
                        draw3DRect(g, markX, markY, markWidth, markHeight, false);
                        int[] px = new int[CHECKMARK_X.length];
                        int[] py = new int[CHECKMARK_X.length];
                        for (int j = 0; j < CHECKMARK_X.length; j++) {
                            px[j] = markX + CHECKMARK_X[j] * markWidth / CHECKMARK_SIZE;
                            py[j] = markY + CHECKMARK_Y[j] * markHeight / CHECKMARK_SIZE;
                        }
                        g.setColor(item.isTargetItemEnabled() ? getForegroundColor() : getDisabledColor());
                        g.fillPolygon(px, py, CHECKMARK_X.length);
                    } else {
                        g.setColor(getBackgroundColor());
                        g.fillRect(markX, markY, markWidth, markHeight);
                        draw3DRect(g, markX, markY, markWidth, markHeight, true);
                    }
                }
            }
        }
        flush();
    }

}
