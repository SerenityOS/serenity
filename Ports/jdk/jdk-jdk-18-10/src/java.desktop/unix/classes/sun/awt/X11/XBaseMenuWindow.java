/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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

import sun.awt.*;

import java.awt.peer.ComponentPeer;
import java.util.ArrayList;
import java.util.Vector;
import sun.util.logging.PlatformLogger;
import sun.java2d.SurfaceData;

/**
 * The abstract class XBaseMenuWindow is the superclass
 * of all menu windows.
 */
public abstract class XBaseMenuWindow extends XWindow {

    /************************************************
     *
     * Data members
     *
     ************************************************/

    private static PlatformLogger log = PlatformLogger.getLogger("sun.awt.X11.XBaseMenuWindow");

    /*
     * Colors are calculated using MotifColorUtilities class
     * from backgroundColor and are contained in these vars.
     */
    private Color backgroundColor;
    private Color foregroundColor;
    private Color lightShadowColor;
    private Color darkShadowColor;
    private Color selectedColor;
    private Color disabledColor;

    /**
     * Array of items.
     */
    private ArrayList<XMenuItemPeer> items;

    /**
     * Index of selected item in array of items
     */
    private int selectedIndex = -1;

    /**
     * Specifies currently showing submenu.
     */
    private XMenuPeer showingSubmenu = null;

    /**
     * Static synchronizational object.
     * Following operations should be synchronized
     * using this object:
     * 1. Access to items vector
     * 2. Access to selection
     * 3. Access to showing menu window member
     *
     * This is lowest level lock,
     * no other locks should be taken when
     * thread own this lock.
     */
    private static Object menuTreeLock = new Object();

    /************************************************
     *
     * Event processing
     *
     ************************************************/

    /**
     * If mouse button is clicked on item showing submenu
     * we have to hide its submenu.
     * And if mouse button is pressed on such item and
     * dragged to another, getShowingSubmenu() is changed.
     * So this member saves the item that the user
     * presses mouse button on _only_ if it's showing submenu.
     */
    private XMenuPeer showingMousePressedSubmenu = null;

    /**
     * If the PopupMenu is invoked as a result of right button click
     * first mouse event after grabInput would be MouseReleased.
     * We need to check if the user has moved mouse after input grab.
     * If yes - hide the PopupMenu. If no - do nothing
     */
    protected Point grabInputPoint = null;
    protected boolean hasPointerMoved = false;

    private AppContext disposeAppContext;

    /************************************************
     *
     * Mapping data
     *
     ************************************************/

    /**
     * Mapping data that is filled in getMappedItems function
     * and reset in resetSize function. It contains array of
     * items in order that they appear on screen and may contain
     * additional data defined by descendants.
     */
    private MappingData mappingData;

    static class MappingData implements Cloneable {

        /**
         * Array of item in order that they appear on screen
         */
        private XMenuItemPeer[] items;

        /**
         * Constructs MappingData object with list
         * of menu items
         */
        MappingData(XMenuItemPeer[] items) {
            this.items = items;
        }

        /**
         * Constructs MappingData without items
         * This constructor should be used in case of errors
         */
        MappingData() {
            this.items = new XMenuItemPeer[0];
        }

        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException ex) {
                throw new InternalError(ex);
            }
        }

        public XMenuItemPeer[] getItems() {
            return this.items;
        }
    }

    /************************************************
     *
     * Construction
     *
     ************************************************/
    XBaseMenuWindow() {
        super(new XCreateWindowParams(new Object[] {
            DELAYED, Boolean.TRUE}));

        disposeAppContext = AppContext.getAppContext();
    }

    /************************************************
     *
     * Abstract methods
     *
     ************************************************/

    /**
     * Returns parent menu window (not the X-hierarchy parent window)
     */
    protected abstract XBaseMenuWindow getParentMenuWindow();

    /**
     * Performs mapping of items in window.
     * This function creates and fills specific
     * descendant of MappingData
     * and sets mapping coordinates of items
     * This function should return default menu data
     * if errors occur
     */
    protected abstract MappingData map();

    /**
     * Calculates placement of submenu window
     * given bounds of item with submenu and
     * size of submenu window. Returns suggested
     * rectangle for submenu window in global coordinates
     * @param itemBounds the bounding rectangle of item
     * in local coordinates
     * @param windowSize the desired size of submenu's window
     */
    protected abstract Rectangle getSubmenuBounds(Rectangle itemBounds, Dimension windowSize);


    /**
     * This function is to be called if it's likely that size
     * of items was changed. It can be called from any thread
     * in any locked state, so it should not take locks
     */
    protected abstract void updateSize();

    /************************************************
     *
     * Initialization
     *
     ************************************************/

    /**
     * Overrides XBaseWindow.instantPreInit
     */
    void instantPreInit(XCreateWindowParams params) {
        super.instantPreInit(params);
        items = new ArrayList<>();
    }

    /************************************************
     *
     * General-purpose functions
     *
     ************************************************/

    /**
     * Returns static lock used for menus
     */
    static Object getMenuTreeLock() {
        return menuTreeLock;
    }

    /**
     * This function is called to clear all saved
     * size data.
     */
    protected void resetMapping() {
        mappingData = null;
    }

    /**
     * Invokes repaint procedure on eventHandlerThread
     */
    void postPaintEvent() {
        if (isShowing()) {
            PaintEvent pe = new PaintEvent(target, PaintEvent.PAINT,
                                           new Rectangle(0, 0, width, height));
            postEvent(pe);
        }
    }

    /************************************************
     *
     * Utility functions for manipulating items
     *
     ************************************************/

    /**
     * Thread-safely returns item at specified index
     * @param index the position of the item to be returned.
     */
    XMenuItemPeer getItem(int index) {
        if (index >= 0) {
            synchronized(getMenuTreeLock()) {
                if (items.size() > index) {
                    return items.get(index);
                }
            }
        }
        return null;
    }

    /**
     * Thread-safely creates a copy of the items vector
     */
    XMenuItemPeer[] copyItems() {
        synchronized(getMenuTreeLock()) {
            return items.toArray(new XMenuItemPeer[] {});
        }
    }


    /**
     * Thread-safely returns selected item
     */
    XMenuItemPeer getSelectedItem() {
        synchronized(getMenuTreeLock()) {
            if (selectedIndex >= 0) {
                if (items.size() > selectedIndex) {
                    return items.get(selectedIndex);
                }
            }
            return null;
        }
    }

    /**
     * Returns showing submenu, if any
     */
    XMenuPeer getShowingSubmenu() {
        synchronized(getMenuTreeLock()) {
            return showingSubmenu;
        }
    }

    /**
     * Adds item to end of items vector.
     * Note that this function does not perform
     * check for adding duplicate items
     * @param item item to add
     */
    public void addItem(MenuItem item) {
        XMenuItemPeer mp = AWTAccessor.getMenuComponentAccessor().getPeer(item);
        if (mp != null) {
            mp.setContainer(this);
            synchronized(getMenuTreeLock()) {
                items.add(mp);
            }
        } else {
            if (log.isLoggable(PlatformLogger.Level.FINE)) {
                log.fine("WARNING: Attempt to add menu item without a peer");
            }
        }
        updateSize();
    }

    /**
     * Removes item at the specified index from items vector.
     * @param index the position of the item to be removed
     */
    public void delItem(int index) {
        synchronized(getMenuTreeLock()) {
            if (selectedIndex == index) {
                selectItem(null, false);
            } else if (selectedIndex > index) {
                selectedIndex--;
            }
            if (index < items.size()) {
                items.remove(index);
            } else {
                if (log.isLoggable(PlatformLogger.Level.FINE)) {
                    log.fine("WARNING: Attempt to remove non-existing menu item, index : " + index + ", item count : " + items.size());
                }
            }
        }
        updateSize();
    }

    /**
     * Clears items vector and loads specified vector
     * @param items vector to be loaded
     */
    public void reloadItems(Vector<? extends MenuItem> items) {
        synchronized(getMenuTreeLock()) {
            this.items.clear();
            MenuItem[] itemArray = items.toArray(new MenuItem[] {});
            int itemCnt = itemArray.length;
            for(int i = 0; i < itemCnt; i++) {
                addItem(itemArray[i]);
            }
        }
    }

    /**
     * Select specified item and shows/hides submenus if necessary
     * We can not select by index, so we need to select by ref.
     * @param item the item to be selected, null to clear selection
     * @param showWindowIfMenu if the item is XMenuPeer then its
     * window is shown/hidden according to this param.
     */
    void selectItem(XMenuItemPeer item, boolean showWindowIfMenu) {
        synchronized(getMenuTreeLock()) {
            XMenuPeer showingSubmenu = getShowingSubmenu();
            int newSelectedIndex = (item != null) ? items.indexOf(item) : -1;
            if (this.selectedIndex != newSelectedIndex) {
                if (log.isLoggable(PlatformLogger.Level.FINEST)) {
                    log.finest("Selected index changed, was : " + this.selectedIndex + ", new : " + newSelectedIndex);
                }
                this.selectedIndex = newSelectedIndex;
                postPaintEvent();
            }
            final XMenuPeer submenuToShow = (showWindowIfMenu && (item instanceof XMenuPeer)) ? (XMenuPeer)item : null;
            if (submenuToShow != showingSubmenu) {
                XToolkit.executeOnEventHandlerThread(target, new Runnable() {
                        public void run() {
                            doShowSubmenu(submenuToShow);
                        }
                    });
            }
        }
    }

    /**
     * Performs hiding of currently showing submenu
     * and showing of submenuToShow.
     * This function should be executed on eventHandlerThread
     * @param submenuToShow submenu to be shown or null
     * to hide currently showing submenu
     */
    private void doShowSubmenu(XMenuPeer submenuToShow) {
        XMenuWindow menuWindowToShow = (submenuToShow != null) ? submenuToShow.getMenuWindow() : null;
        Dimension dim = null;
        Rectangle bounds = null;
        //ensureCreated can invoke XWindowPeer.init() ->
        //XWindowPeer.initGraphicsConfiguration() ->
        //Window.getGraphicsConfiguration()
        //that tries to obtain Component.AWTTreeLock.
        //So it should be called outside awtLock()
        if (menuWindowToShow != null) {
            menuWindowToShow.ensureCreated();
        }
        XToolkit.awtLock();
        try {
            synchronized(getMenuTreeLock()) {
                if (showingSubmenu != submenuToShow) {
                    if (log.isLoggable(PlatformLogger.Level.FINEST)) {
                        log.finest("Changing showing submenu");
                    }
                    if (showingSubmenu != null) {
                        XMenuWindow showingSubmenuWindow = showingSubmenu.getMenuWindow();
                        if (showingSubmenuWindow != null) {
                            showingSubmenuWindow.hide();
                        }
                    }
                    if (submenuToShow != null) {
                        dim = menuWindowToShow.getDesiredSize();
                        bounds = menuWindowToShow.getParentMenuWindow().getSubmenuBounds(submenuToShow.getBounds(), dim);
                        menuWindowToShow.show(bounds);
                    }
                    showingSubmenu = submenuToShow;
                }
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    final void setItemsFont( Font font ) {
        XMenuItemPeer[] items = copyItems();
        int itemCnt = items.length;
        for (int i = 0; i < itemCnt; i++) {
            items[i].setFont(font);
        }
    }

    /************************************************
     *
     * Utility functions for manipulating mapped items
     *
     ************************************************/

    /**
     * Returns array of mapped items, null if error
     * This function has to be not synchronized
     * and we have to guarantee that we return
     * some MappingData to user. It's OK if
     * this.mappingData is replaced meanwhile
     */
    MappingData getMappingData() {
        MappingData mappingData = this.mappingData;
        if (mappingData == null) {
            mappingData = map();
            this.mappingData = mappingData;
        }
        return (MappingData)mappingData.clone();
    }

    /**
     * returns item thats mapped coordinates contain
     * specified point, null of none.
     * @param pt the point in this window's coordinate system
     */
    XMenuItemPeer getItemFromPoint(Point pt) {
        XMenuItemPeer[] items = getMappingData().getItems();
        int cnt = items.length;
        for (int i = 0; i < cnt; i++) {
            if (items[i].getBounds().contains(pt)) {
                return items[i];
            }
        }
        return null;
    }

    /**
     * Returns first item after currently selected
     * item that can be selected according to mapping array.
     * (no separators and no disabled items).
     * Currently selected item if it's only selectable,
     * null if no item can be selected
     */
    XMenuItemPeer getNextSelectableItem() {
        XMenuItemPeer[] mappedItems = getMappingData().getItems();
        XMenuItemPeer selectedItem = getSelectedItem();
        int cnt = mappedItems.length;
        //Find index of selected item
        int selIdx = -1;
        for (int i = 0; i < cnt; i++) {
            if (mappedItems[i] == selectedItem) {
                selIdx = i;
                break;
            }
        }
        int idx = (selIdx == cnt - 1) ? 0 : selIdx + 1;
        //cycle through mappedItems to find selectable item
        //beginning from the next item and moving to the
        //beginning of array when end is reached.
        //Cycle is finished on selected item itself
        for (int i = 0; i < cnt; i++) {
            XMenuItemPeer item = mappedItems[idx];
            if (!item.isSeparator() && item.isTargetItemEnabled()) {
                return item;
            }
            idx++;
            if (idx >= cnt) {
                idx = 0;
            }
        }
        //return null if no selectable item was found
        return null;
    }

    /**
     * Returns first item before currently selected
     * see getNextSelectableItem() for comments
     */
    XMenuItemPeer getPrevSelectableItem() {
        XMenuItemPeer[] mappedItems = getMappingData().getItems();
        XMenuItemPeer selectedItem = getSelectedItem();
        int cnt = mappedItems.length;
        //Find index of selected item
        int selIdx = -1;
        for (int i = 0; i < cnt; i++) {
            if (mappedItems[i] == selectedItem) {
                selIdx = i;
                break;
            }
        }
        int idx = (selIdx <= 0) ? cnt - 1 : selIdx - 1;
        //cycle through mappedItems to find selectable item
        for (int i = 0; i < cnt; i++) {
            XMenuItemPeer item = mappedItems[idx];
            if (!item.isSeparator() && item.isTargetItemEnabled()) {
                return item;
            }
            idx--;
            if (idx < 0) {
                idx = cnt - 1;
            }
        }
        //return null if no selectable item was found
        return null;
    }

    /**
     * Returns first selectable item
     * This function is intended for clearing selection
     */
    XMenuItemPeer getFirstSelectableItem() {
        XMenuItemPeer[] mappedItems = getMappingData().getItems();
        int cnt = mappedItems.length;
        for (int i = 0; i < cnt; i++) {
            XMenuItemPeer item = mappedItems[i];
            if (!item.isSeparator() && item.isTargetItemEnabled()) {
                return item;
            }
        }

        return null;
    }

    /************************************************
     *
     * Utility functions for manipulating
     * hierarchy of windows
     *
     ************************************************/

    /**
     * returns leaf menu window or
     * this if no children are showing
     */
    XBaseMenuWindow getShowingLeaf() {
        synchronized(getMenuTreeLock()) {
            XBaseMenuWindow leaf = this;
            XMenuPeer leafchild = leaf.getShowingSubmenu();
            while (leafchild != null) {
                leaf = leafchild.getMenuWindow();
                leafchild = leaf.getShowingSubmenu();
            }
            return leaf;
        }
    }

    /**
     * returns root menu window
     * or this if this window is topmost
     */
    XBaseMenuWindow getRootMenuWindow() {
        synchronized(getMenuTreeLock()) {
            XBaseMenuWindow t = this;
            XBaseMenuWindow tparent = t.getParentMenuWindow();
            while (tparent != null) {
                t = tparent;
                tparent = t.getParentMenuWindow();
            }
            return t;
        }
    }

    /**
     * Returns window that contains pt.
     * search is started from leaf window
     * to return first window in Z-order
     * @param pt point in global coordinates
     */
    XBaseMenuWindow getMenuWindowFromPoint(Point pt) {
        synchronized(getMenuTreeLock()) {
            XBaseMenuWindow t = getShowingLeaf();
            while (t != null) {
                Rectangle r = new Rectangle(t.toGlobal(new Point(0, 0)), t.getSize());
                if (r.contains(pt)) {
                    return t;
                }
                t = t.getParentMenuWindow();
            }
            return null;
        }
    }

    /************************************************
     *
     * Primitives for getSubmenuBounds
     *
     * These functions are invoked from getSubmenuBounds
     * implementations in different order. They check if window
     * of size windowSize fits to the specified edge of
     * rectangle itemBounds on the screen of screenSize.
     * Return rectangle that occupies the window if it fits or null.
     *
     ************************************************/

    GraphicsConfiguration getCurrentGraphicsConfiguration() {
        Component hw = SunToolkit.getHeavyweightComponent(target);
        XWindow peer = AWTAccessor.getComponentAccessor().getPeer(hw);
        if (peer != null && peer.graphicsConfig != null) {
            return peer.graphicsConfig;
        }
        return graphicsConfig;
    }

    /**
     * Checks if window fits below specified item
     * returns rectangle that the window fits to or null.
     * @param itemBounds rectangle of item in global coordinates
     * @param windowSize size of submenu window to fit
     * @param screenBounds size of screen
     */
    Rectangle fitWindowBelow(Rectangle itemBounds, Dimension windowSize, Rectangle screenBounds) {
        int width = windowSize.width;
        int height = windowSize.height;
        //Fix for 6267162: PIT: Popup Menu gets hidden below the screen when opened
        //near the periphery of the screen, XToolkit
        //Window should be moved if it's outside top-left screen bounds
        int x = (itemBounds.x > screenBounds.x) ? itemBounds.x : screenBounds.x;
        int y = (itemBounds.y + itemBounds.height > screenBounds.y) ? itemBounds.y + itemBounds.height : screenBounds.y;
        if (y + height <= screenBounds.y + screenBounds.height) {
            //move it to the left if needed
            if (width > screenBounds.width) {
                width = screenBounds.width;
            }
            if (x + width > screenBounds.x + screenBounds.width) {
                x = screenBounds.x + screenBounds.width - width;
            }
            return new Rectangle(x, y, width, height);
        } else {
            return null;
        }
    }

    /**
     * Checks if window fits above specified item
     * returns rectangle that the window fits to or null.
     * @param itemBounds rectangle of item in global coordinates
     * @param windowSize size of submenu window to fit
     * @param screenBounds size of screen
     */
    Rectangle fitWindowAbove(Rectangle itemBounds, Dimension windowSize, Rectangle screenBounds) {
        int width = windowSize.width;
        int height = windowSize.height;
        //Fix for 6267162: PIT: Popup Menu gets hidden below the screen when opened
        //near the periphery of the screen, XToolkit
        //Window should be moved if it's outside bottom-left screen bounds
        int x = (itemBounds.x > screenBounds.x) ? itemBounds.x : screenBounds.x;
        int y = (itemBounds.y > screenBounds.y + screenBounds.height) ? screenBounds.y + screenBounds.height - height : itemBounds.y - height;
        if (y >= screenBounds.y) {
            //move it to the left if needed
            if (width > screenBounds.width) {
                width = screenBounds.width;
            }
            if (x + width > screenBounds.x + screenBounds.width) {
                x = screenBounds.x + screenBounds.width - width;
            }
            return new Rectangle(x, y, width, height);
        } else {
            return null;
        }
    }

    /**
     * Checks if window fits to the right specified item
     * returns rectangle that the window fits to or null.
     * @param itemBounds rectangle of item in global coordinates
     * @param windowSize size of submenu window to fit
     * @param screenBounds size of screen
     */
    Rectangle fitWindowRight(Rectangle itemBounds, Dimension windowSize, Rectangle screenBounds) {
        int width = windowSize.width;
        int height = windowSize.height;
        //Fix for 6267162: PIT: Popup Menu gets hidden below the screen when opened
        //near the periphery of the screen, XToolkit
        //Window should be moved if it's outside top-left screen bounds
        int x = (itemBounds.x + itemBounds.width > screenBounds.x) ? itemBounds.x + itemBounds.width : screenBounds.x;
        int y = (itemBounds.y > screenBounds.y) ? itemBounds.y : screenBounds.y;
        if (x + width <= screenBounds.x + screenBounds.width) {
            //move it to the top if needed
            if (height > screenBounds.height) {
                height = screenBounds.height;
            }
            if (y + height > screenBounds.y + screenBounds.height) {
                y = screenBounds.y + screenBounds.height - height;
            }
            return new Rectangle(x, y, width, height);
        } else {
            return null;
        }
    }

    /**
     * Checks if window fits to the left specified item
     * returns rectangle that the window fits to or null.
     * @param itemBounds rectangle of item in global coordinates
     * @param windowSize size of submenu window to fit
     * @param screenBounds size of screen
     */
    Rectangle fitWindowLeft(Rectangle itemBounds, Dimension windowSize, Rectangle screenBounds) {
        int width = windowSize.width;
        int height = windowSize.height;
        //Fix for 6267162: PIT: Popup Menu gets hidden below the screen when opened
        //near the periphery of the screen, XToolkit
        //Window should be moved if it's outside top-right screen bounds
        int x = (itemBounds.x < screenBounds.x + screenBounds.width) ? itemBounds.x - width : screenBounds.x + screenBounds.width - width;
        int y = (itemBounds.y > screenBounds.y) ? itemBounds.y : screenBounds.y;
        if (x >= screenBounds.x) {
            //move it to the top if needed
            if (height > screenBounds.height) {
                height = screenBounds.height;
            }
            if (y + height > screenBounds.y + screenBounds.height) {
                y = screenBounds.y + screenBounds.height - height;
            }
            return new Rectangle(x, y, width, height);
        } else {
            return null;
        }
    }

    /**
     * The last thing we can do with the window
     * to fit it on screen - move it to the
     * top-left edge and cut by screen dimensions
     * @param windowSize size of submenu window to fit
     * @param screenBounds size of screen
     */
    Rectangle fitWindowToScreen(Dimension windowSize, Rectangle screenBounds) {
        int width = (windowSize.width < screenBounds.width) ? windowSize.width : screenBounds.width;
        int height = (windowSize.height < screenBounds.height) ? windowSize.height : screenBounds.height;
        return new Rectangle(screenBounds.x, screenBounds.y, width, height);
    }


    /************************************************
     *
     * Utility functions for manipulating colors
     *
     ************************************************/

    /**
     * This function is called before every painting.
     * TODO:It would be better to add PropertyChangeListener
     * to target component
     * TODO:It would be better to access background color
     * not invoking user-overridable function
     */
    void resetColors() {
        replaceColors((target == null) ? SystemColor.window : target.getBackground());
    }

    /**
     * Calculates colors of various elements given
     * background color. Uses MotifColorUtilities
     * @param backgroundColor the color of menu window's
     * background.
     */
    void replaceColors(Color backgroundColor) {
        if (backgroundColor != this.backgroundColor) {
            this.backgroundColor = backgroundColor;

            int red = backgroundColor.getRed();
            int green = backgroundColor.getGreen();
            int blue = backgroundColor.getBlue();

            foregroundColor = new Color(MotifColorUtilities.calculateForegroundFromBackground(red,green,blue));
            lightShadowColor = new Color(MotifColorUtilities.calculateTopShadowFromBackground(red,green,blue));
            darkShadowColor = new Color(MotifColorUtilities.calculateBottomShadowFromBackground(red,green,blue));
            selectedColor = new Color(MotifColorUtilities.calculateSelectFromBackground(red,green,blue));
            disabledColor = (backgroundColor.equals(Color.BLACK)) ? foregroundColor.darker() : backgroundColor.darker();
        }
    }

    Color getBackgroundColor() {
        return backgroundColor;
    }

    Color getForegroundColor() {
        return foregroundColor;
    }

    Color getLightShadowColor() {
        return lightShadowColor;
    }

    Color getDarkShadowColor() {
        return darkShadowColor;
    }

    Color getSelectedColor() {
        return selectedColor;
    }

    Color getDisabledColor() {
        return disabledColor;
    }

    /************************************************
     *
     * Painting utility functions
     *
     ************************************************/

    /**
     * Draws raised or sunken rectangle on specified graphics
     * @param g the graphics on which to draw
     * @param x the coordinate of left edge in coordinates of graphics
     * @param y the coordinate of top edge in coordinates of graphics
     * @param width the width of rectangle
     * @param height the height of rectangle
     * @param raised true to draw raised rectangle, false to draw sunken
     */
    void draw3DRect(Graphics g, int x, int y, int width, int height, boolean raised) {
        if ((width <= 0) || (height <= 0)) {
            return;
        }
        Color c = g.getColor();
        g.setColor(raised ? getLightShadowColor() : getDarkShadowColor());
        g.drawLine(x, y, x, y + height - 1);
        g.drawLine(x + 1, y, x + width - 1, y);
        g.setColor(raised ? getDarkShadowColor() : getLightShadowColor());
        g.drawLine(x + 1, y + height - 1, x + width - 1, y + height - 1);
        g.drawLine(x + width - 1, y + 1, x + width - 1, y + height - 1);
        g.setColor(c);
    }

    /************************************************
     *
     * Overriden utility functions of XWindow
     *
     ************************************************/

    /**
     * Filters X events
     */
     protected boolean isEventDisabled(XEvent e) {
        switch (e.get_type()) {
          case XConstants.Expose :
          case XConstants.GraphicsExpose :
          case XConstants.ButtonPress:
          case XConstants.ButtonRelease:
          case XConstants.MotionNotify:
          case XConstants.KeyPress:
          case XConstants.KeyRelease:
          case XConstants.DestroyNotify:
              return super.isEventDisabled(e);
          default:
              return true;
        }
    }

    /**
     * Invokes disposal procedure on eventHandlerThread
     */
    public void dispose() {
        setDisposed(true);

        SunToolkit.invokeLaterOnAppContext(disposeAppContext, new Runnable()  {
            public void run() {
                doDispose();
            }
        });
    }

    /**
     * Performs disposal of menu window.
     * Should be called only on eventHandlerThread
     */
    protected void doDispose() {
        xSetVisible(false);
        SurfaceData oldData = surfaceData;
        surfaceData = null;
        if (oldData != null) {
            oldData.invalidate();
        }
        destroy();
    }

    /**
     * Invokes event processing on eventHandlerThread
     * This function needs to be overriden since
     * XBaseMenuWindow has no corresponding component
     * so events can not be processed using standart means
     */
    void postEvent(final AWTEvent event) {
        InvocationEvent ev = new InvocationEvent(event.getSource(), new Runnable() {
            public void run() {
                handleEvent(event);
            }
        });
        super.postEvent(ev);
    }

    /**
     * The implementation of base window performs processing
     * of paint events only. This behaviour is changed in
     * descendants.
     */
    protected void handleEvent(AWTEvent event) {
        switch(event.getID()) {
        case PaintEvent.PAINT:
            doHandleJavaPaintEvent((PaintEvent)event);
            break;
        }
    }

    /**
     * Save location of pointer for further use
     * then invoke superclass
     */
    public boolean grabInput() {
        int rootX;
        int rootY;
        boolean res;
        XToolkit.awtLock();
        try {
            long root = XlibWrapper.RootWindow(XToolkit.getDisplay(),
                    getScreenNumber());
            res = XlibWrapper.XQueryPointer(XToolkit.getDisplay(), root,
                                            XlibWrapper.larg1, //root
                                            XlibWrapper.larg2, //child
                                            XlibWrapper.larg3, //root_x
                                            XlibWrapper.larg4, //root_y
                                            XlibWrapper.larg5, //child_x
                                            XlibWrapper.larg6, //child_y
                                            XlibWrapper.larg7);//mask
            rootX = Native.getInt(XlibWrapper.larg3);
            rootY = Native.getInt(XlibWrapper.larg4);
            res &= super.grabInput();
        } finally {
            XToolkit.awtUnlock();
        }
        if (res) {
            //Mouse pointer is on the same display
            this.grabInputPoint = new Point(rootX, rootY);
            this.hasPointerMoved = false;
        } else {
            this.grabInputPoint = null;
            this.hasPointerMoved = true;
        }
        return res;
    }
    /************************************************
     *
     * Overridable event processing functions
     *
     ************************************************/

    /**
     * Performs repainting
     */
    void doHandleJavaPaintEvent(PaintEvent event) {
        Rectangle rect = event.getUpdateRect();
        repaint(rect.x, rect.y, rect.width, rect.height);
    }

    /************************************************
     *
     * User input handling utility functions
     *
     ************************************************/

    /**
     * Performs handling of java mouse event
     * Note that this function should be invoked
     * only from root of menu window's hierarchy
     * that grabs input focus
     */
    void doHandleJavaMouseEvent( MouseEvent mouseEvent ) {
        if (!XToolkit.isLeftMouseButton(mouseEvent) && !XToolkit.isRightMouseButton(mouseEvent)) {
            return;
        }
        //Window that owns input
        XBaseWindow grabWindow = XAwtState.getGrabWindow();
        //Point of mouse event in global coordinates
        Point ptGlobal = mouseEvent.getLocationOnScreen();
        if (!hasPointerMoved) {
            //Fix for 6301307: NullPointerException while dispatching mouse events, XToolkit
            if (grabInputPoint == null ||
                (Math.abs(ptGlobal.x - grabInputPoint.x) > getMouseMovementSmudge()) ||
                (Math.abs(ptGlobal.y - grabInputPoint.y) > getMouseMovementSmudge())) {
                hasPointerMoved = true;
            }
        }
        //Z-order first descendant of current menu window
        //hierarchy that contain mouse point
        XBaseMenuWindow wnd = getMenuWindowFromPoint(ptGlobal);
        //Item in wnd that contains mouse point, if any
        XMenuItemPeer item = (wnd != null) ? wnd.getItemFromPoint(wnd.toLocal(ptGlobal)) : null;
        //Currently showing leaf window
        XBaseMenuWindow cwnd = getShowingLeaf();
        switch (mouseEvent.getID()) {
          case MouseEvent.MOUSE_PRESSED:
              //This line is to get rid of possible problems
              //That may occur if mouse events are lost
              showingMousePressedSubmenu = null;
              if ((grabWindow == this) && (wnd == null)) {
                  //Menus grab input and the user
                  //presses mouse button outside
                  ungrabInput();
              } else {
                  //Menus grab input OR mouse is pressed on menu window
                  grabInput();
                  if (item != null && !item.isSeparator() && item.isTargetItemEnabled()) {
                      //Button is pressed on enabled item
                      if (wnd.getShowingSubmenu() == item) {
                          //Button is pressed on item that shows
                          //submenu. We have to hide its submenu
                          //if user clicks on it
                          showingMousePressedSubmenu = (XMenuPeer)item;
                      }
                      wnd.selectItem(item, true);
                  } else {
                      //Button is pressed on disabled item or empty space
                      if (wnd != null) {
                          wnd.selectItem(null, false);
                      }
                  }
              }
              break;
          case MouseEvent.MOUSE_RELEASED:
              //Note that if item is not null, wnd has to be not null
              if (item != null && !item.isSeparator() && item.isTargetItemEnabled()) {
                  if  (item instanceof XMenuPeer) {
                      if (showingMousePressedSubmenu == item) {
                          //User clicks on item that shows submenu.
                          //Hide the submenu
                          if (wnd instanceof XMenuBarPeer) {
                              ungrabInput();
                          } else {
                              wnd.selectItem(item, false);
                          }
                      }
                  } else {
                      //Invoke action event
                      @SuppressWarnings("deprecation")
                      final int modifiers = mouseEvent.getModifiers();
                      item.action(mouseEvent.getWhen(), modifiers);
                      ungrabInput();
                  }
              } else {
                  //Mouse is released outside menu items
                  if (hasPointerMoved || (wnd instanceof XMenuBarPeer)) {
                      ungrabInput();
                  }
              }
              showingMousePressedSubmenu = null;
              break;
          case MouseEvent.MOUSE_DRAGGED:
              if (wnd != null) {
                  //Mouse is dragged over menu window
                  //Move selection to item under cursor
                  if (item != null && !item.isSeparator() && item.isTargetItemEnabled()) {
                      if (grabWindow == this){
                          wnd.selectItem(item, true);
                      }
                  } else {
                      wnd.selectItem(null, false);
                  }
              } else {
                  //Mouse is dragged outside menu windows
                  //clear selection in leaf to reflect it
                  if (cwnd != null) {
                      cwnd.selectItem(null, false);
                  }
              }
              break;
        }
    }

    /**
     * Performs handling of java keyboard event
     * Note that this function should be invoked
     * only from root of menu window's hierarchy
     * that grabs input focus
     */
    void doHandleJavaKeyEvent(KeyEvent event) {
        if (log.isLoggable(PlatformLogger.Level.FINER)) {
            log.finer(event.toString());
        }
        if (event.getID() != KeyEvent.KEY_PRESSED) {
            return;
        }
        final int keyCode = event.getKeyCode();
        XBaseMenuWindow cwnd = getShowingLeaf();
        XMenuItemPeer citem = cwnd.getSelectedItem();
        switch(keyCode) {
          case KeyEvent.VK_UP:
          case KeyEvent.VK_KP_UP:
              if (!(cwnd instanceof XMenuBarPeer)) {
                  //If active window is not menu bar,
                  //move selection up
                  cwnd.selectItem(cwnd.getPrevSelectableItem(), false);
              }
              break;
          case KeyEvent.VK_DOWN:
          case KeyEvent.VK_KP_DOWN:
              if (cwnd instanceof XMenuBarPeer) {
                  //If active window is menu bar show current submenu
                  selectItem(getSelectedItem(), true);
              } else {
                  //move selection down
                  cwnd.selectItem(cwnd.getNextSelectableItem(), false);
              }
              break;
          case KeyEvent.VK_LEFT:
          case KeyEvent.VK_KP_LEFT:
              if (cwnd instanceof XMenuBarPeer) {
                  //leaf window is menu bar
                  //select previous item
                  selectItem(getPrevSelectableItem(), false);
              } else if (cwnd.getParentMenuWindow() instanceof XMenuBarPeer) {
                  //leaf window is direct child of menu bar
                  //select previous item of menu bar
                  //and show its submenu
                  selectItem(getPrevSelectableItem(), true);
              } else {
                  //hide leaf moving focus to its parent
                  //(equvivalent of pressing ESC)
                  XBaseMenuWindow pwnd = cwnd.getParentMenuWindow();
                  //Fix for 6272952: PIT: Pressing LEFT ARROW on a popup menu throws NullPointerException, XToolkit
                  if (pwnd != null) {
                      pwnd.selectItem(pwnd.getSelectedItem(), false);
                  }
              }
              break;
          case KeyEvent.VK_RIGHT:
          case KeyEvent.VK_KP_RIGHT:
              if (cwnd instanceof XMenuBarPeer) {
                  //leaf window is menu bar
                  //select next item
                  selectItem(getNextSelectableItem(), false);
              } else if (citem instanceof XMenuPeer) {
                  //current item is menu, show its window
                  //(equivalent of ENTER)
                  cwnd.selectItem(citem, true);
              } else if (this instanceof XMenuBarPeer) {
                  //if this is menu bar (not popup menu)
                  //and the user presses RIGHT on item (not submenu)
                  //select next top-level menu
                  selectItem(getNextSelectableItem(), true);
              }
              break;
          case KeyEvent.VK_SPACE:
          case KeyEvent.VK_ENTER:
              //If the current item has submenu show it
              //Perform action otherwise
              if (citem instanceof XMenuPeer) {
                  cwnd.selectItem(citem, true);
              } else if (citem != null) {
                  @SuppressWarnings("deprecation")
                  final int modifiers = event.getModifiers();
                  citem.action(event.getWhen(), modifiers);
                  ungrabInput();
              }
              break;
          case KeyEvent.VK_ESCAPE:
              //If current window is menu bar or its child - close it
              //If current window is popup menu - close it
              //go one level up otherwise

              //Fixed 6266513: Incorrect key handling in XAWT popup menu
              //Popup menu should be closed on 'ESC'
              if ((cwnd instanceof XMenuBarPeer) || (cwnd.getParentMenuWindow() instanceof XMenuBarPeer)) {
                  ungrabInput();
              } else if (cwnd instanceof XPopupMenuPeer) {
                  ungrabInput();
              } else {
                  XBaseMenuWindow pwnd = cwnd.getParentMenuWindow();
                  pwnd.selectItem(pwnd.getSelectedItem(), false);
              }
              break;
          case KeyEvent.VK_F10:
              //Fixed 6266513: Incorrect key handling in XAWT popup menu
              //All menus should be closed on 'F10'
              ungrabInput();
              break;
          default:
              break;
        }
    }

} //class XBaseMenuWindow
